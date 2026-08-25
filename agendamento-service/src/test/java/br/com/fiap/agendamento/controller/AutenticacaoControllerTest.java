package br.com.fiap.agendamento.controller;

import br.com.fiap.agendamento.entity.Paciente;
import br.com.fiap.agendamento.entity.Usuario;
import br.com.fiap.agendamento.repository.PacienteRepository;
import br.com.fiap.agendamento.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vai por HTTP de proposito: e a cadeia de filtros que precisa ser exercitada
 * aqui - o /graphql liberado, o Bearer lido, o campo barrado sem token. Um
 * GraphQlTester comum pula tudo isso.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutenticacaoControllerTest {

    private static final String MUTATION_LOGIN = """
            mutation($login: String!, $senha: String!) {
              login(input: {login: $login, senha: $senha}) { token expiraEm nome role }
            }
            """;

    private static final String QUERY_PROTEGIDA = "query { agendamentos { totalElementos } }";

    private static final String SENHA = "123456";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PacienteRepository pacienteRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void preparar() {
        usuarioRepository.deleteAll();

        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .nome("Maria Silva").telefone("91999998888").build());
        usuarioRepository.save(
                Usuario.paraPaciente("maria", passwordEncoder.encode(SENHA), paciente));
    }

    // ------------------------------------------------------------------- login

    @Test
    void loginValidoDevolveTokenNomeERole() throws Exception {
        login("maria", SENHA)
                .andExpect(status().isOk())
                // Tres segmentos separados por ponto: header, payload e assinatura.
                .andExpect(jsonPath("$.data.login.token").value(startsWith("eyJ")))
                .andExpect(jsonPath("$.data.login.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.data.login.role").value("PACIENTE"))
                .andExpect(jsonPath("$.data.login.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void loginComSenhaErradaNaoDevolveToken() throws Exception {
        login("maria", "senha-errada")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.errors[0].message").value("login ou senha invalidos"));
    }

    @Test
    void loginComUsuarioInexistenteRespondeIgualASenhaErrada() throws Exception {
        login("nao-existe", SENHA)
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.errors[0].message").value("login ou senha invalidos"));
    }

    // -------------------------------------------------------------- protegidas

    @Test
    void operacaoSemTokenERecusada() throws Exception {
        executar(QUERY_PROTEGIDA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agendamentos").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("UNAUTHORIZED"));
    }

    @Test
    void operacaoComTokenInvalidoERecusada() throws Exception {
        executar(QUERY_PROTEGIDA, "eyJhbGciOiJIUzI1NiJ9.falsificado.assinatura")
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("UNAUTHORIZED"));
    }

    @Test
    void operacaoComTokenValidoEAtendida() throws Exception {
        executar(QUERY_PROTEGIDA, tokenDe("maria", SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.agendamentos.totalElementos").isNumber());
    }

    @Test
    void mutationProtegidaTambemExigeToken() throws Exception {
        executar("mutation { removerAgendamento(id: \"1\") }", null)
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("UNAUTHORIZED"));
    }

    // ----------------------------------------------------------------- helpers

    private ResultActions login(String login, String senha) throws Exception {
        return executar(MUTATION_LOGIN, Map.of("login", login, "senha", senha), null);
    }

    private String tokenDe(String login, String senha) throws Exception {
        String corpo = login(login, senha).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).at("/data/login/token").asText();
    }

    private ResultActions executar(String documento, String token) throws Exception {
        return executar(documento, Map.of(), token);
    }

    private ResultActions executar(String documento, Map<String, Object> variaveis, String token)
            throws Exception {

        var requisicao = post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("query", documento, "variables", variaveis)));

        if (token != null) {
            requisicao = requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(requisicao);
    }
}
