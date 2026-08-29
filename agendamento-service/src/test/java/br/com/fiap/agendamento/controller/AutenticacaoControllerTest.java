package br.com.fiap.agendamento.controller;

import br.com.fiap.agendamento.entity.Cargo;
import br.com.fiap.agendamento.entity.Paciente;
import br.com.fiap.agendamento.entity.Profissional;
import br.com.fiap.agendamento.entity.Usuario;
import br.com.fiap.agendamento.repository.PacienteRepository;
import br.com.fiap.agendamento.repository.ProfissionalRepository;
import br.com.fiap.agendamento.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As verdades que so existem sobre HTTP: o 401 do EntryPoint, o corpo
 * problem+json, o header Authorization sendo lido. O GraphQlTester dos outros
 * testes nao passa pela cadeia de filtros, entao nada disso apareceria la.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutenticacaoControllerTest {

    private static final String SENHA = "123456";
    private static final String QUERY_LISTA =
            "{\"query\":\"{ agendamentos { totalElementos } }\"}";

    // Sem broker no ambiente de teste. O mock deixa o PublishService rodar de
    // verdade (routing key incluida) sem abrir conexao AMQP.
    @MockBean RabbitTemplate rabbitTemplate;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;

    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PacienteRepository pacienteRepository;
    @Autowired ProfissionalRepository profissionalRepository;

    @BeforeEach
    void preparar() {
        // usuario antes de paciente/profissional: a FK aponta nessa direcao.
        usuarioRepository.deleteAll();
        pacienteRepository.deleteAll();
        profissionalRepository.deleteAll();

        // O hash sai do PasswordEncoder injetado, e nao de uma constante colada:
        // assim o teste continua valendo se a forca do BCrypt mudar.
        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .nome("Maria Silva").telefone("91999998888").build());
        Profissional profissional = profissionalRepository.save(Profissional.builder()
                .nome("Dra. Helena Prado").cargo(Cargo.MEDICO)
                .especialidade("Cardiologia").registroConselho("CRM-PA 12345").build());

        usuarioRepository.save(Usuario.paraPaciente(
                "paciente", passwordEncoder.encode(SENHA), paciente));
        usuarioRepository.save(Usuario.paraProfissional(
                "medico", passwordEncoder.encode(SENHA), profissional));
    }

    // ------------------------------------------------------------------ login

    @Test
    void loginDevolveTokenComNomeERole() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin("medico", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Dra. Helena Prado"))
                .andExpect(jsonPath("$.role").value("MEDICO"));
    }

    @Test
    void roleDoUsuarioDePacienteVemDoCadastro() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin("paciente", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PACIENTE"))
                .andExpect(jsonPath("$.nome").value("Maria Silva"));
    }

    /**
     * Senha errada e login inexistente respondem identico: qualquer diferenca
     * viraria uma forma de descobrir quais logins existem.
     */
    @Test
    void senhaErradaEUsuarioInexistenteRespondemIgual() throws Exception {
        String comSenhaErrada = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin("medico", "errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Login ou senha invalidos."))
                .andReturn().getResponse().getContentAsString();

        String comLoginInexistente = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin("nao-existe", SENHA)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(comSenhaErrada).isEqualTo(comLoginInexistente);
    }

    @Test
    void corpoSemSenhaERecusadoComOsCampos() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"medico\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Corpo da requisicao invalido."))
                .andExpect(jsonPath("$.campos[0].campo").value("senha"));
    }

    // ------------------------------------------------------------- /graphql

    @Test
    void graphqlSemTokenResponde401() throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QUERY_LISTA))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("Authorization")));
    }

    @Test
    void graphqlComTokenAdulteradoResponde401() throws Exception {
        String token = autenticar("medico");
        // Trocar o ultimo caractere invalida a assinatura sem quebrar o formato.
        // Nao serve acrescentar um caractere: sozinho ele carrega 6 bits, nao
        // fecha um byte, e o decodificador base64url simplesmente o descarta.
        String adulterado = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        mockMvc.perform(post("/graphql")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adulterado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QUERY_LISTA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void graphqlComTokenValidoResponde200() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + autenticar("medico"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QUERY_LISTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agendamentos.totalElementos").exists())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    /**
     * Negacao por role e erro de campo GraphQL, nao status HTTP: a resposta e 200
     * e o cliente tem de olhar errors[].extensions.classification.
     */
    @Test
    void mutationComTokenDePacienteResponde200ComForbidden() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + autenticar("paciente"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { removerAgendamento(id: \\\"1\\\") }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }

    @Test
    void healthDoActuatorSegueAberto() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ apoio

    private String corpoDeLogin(String login, String senha) {
        return "{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha);
    }

    private String autenticar(String login) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeLogin(login, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(corpo).get("token").asText();
    }
}
