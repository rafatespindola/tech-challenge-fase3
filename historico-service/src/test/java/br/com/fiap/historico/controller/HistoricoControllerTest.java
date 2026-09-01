package br.com.fiap.historico.controller;

import br.com.fiap.historico.dto.input.Agendamento;
import br.com.fiap.historico.dto.input.MensagemFila;
import br.com.fiap.historico.entity.AgendamentoEvento;
import br.com.fiap.historico.entity.StatusAgendamento;
import br.com.fiap.historico.listener.AgendamentoListener;
import br.com.fiap.historico.repository.HistoricoRepository;
import br.com.fiap.historico.security.ComoUsuario;
import br.com.fiap.historico.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Percorre o caminho inteiro: a mensagem entra pelo listener, como entraria vinda
 * da fila, e sai pela API REST. Sem broker - o listener e chamado direto, o que
 * testa tudo o que e deste servico e nada do que e do RabbitMQ.
 *
 * O listener nao passa pela cadeia de seguranca de proposito: quem escreve no
 * historico e a fila, e fila nao tem token. A seguranca vale para o HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HistoricoControllerTest {

    private static final OffsetDateTime DATA_HORA =
            OffsetDateTime.of(2026, 9, 2, 14, 30, 0, 0, ZoneOffset.ofHours(-3));

    private static final long PACIENTE_ANA = 1L;
    private static final long PACIENTE_BRUNO = 5L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgendamentoListener listener;

    @Autowired
    private HistoricoRepository historicoRepository;

    @BeforeEach
    void limpar() {
        historicoRepository.deleteAll();
    }

    @Test
    @ComoUsuario(role = Role.ENFERMEIRO, profissionalId = 2L)
    @DisplayName("registra os tres eventos do agendamento e devolve a linha do tempo em ordem")
    void linhaDoTempoCobreCriacaoEdicaoEExclusao() throws Exception {
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_CRIADO, 7L, DATA_HORA,
                StatusAgendamento.AGENDADO), "msg-1");
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_EDITADO, 7L, DATA_HORA.plusHours(2),
                StatusAgendamento.AGENDADO), "msg-2");
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_EXCLUIDO, 7L, DATA_HORA.plusHours(2),
                StatusAgendamento.CANCELADO), "msg-3");

        mockMvc.perform(get("/historicos/agendamentos/{id}", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].evento").value("AGENDAMENTO_CRIADO"))
                .andExpect(jsonPath("$[1].evento").value("AGENDAMENTO_EDITADO"))
                .andExpect(jsonPath("$[2].evento").value("AGENDAMENTO_EXCLUIDO"))
                .andExpect(jsonPath("$[2].status").value("CANCELADO"))
                .andExpect(jsonPath("$[0].paciente.nome").value("Ana Souza"))
                .andExpect(jsonPath("$[0].convenio.nome").value("Unimed"));
    }

    @Test
    @DisplayName("reentrega da mesma mensagem nao duplica a linha")
    void reentregaNaoDuplica() {
        MensagemFila mensagem = mensagem(AgendamentoEvento.AGENDAMENTO_CRIADO, 8L, DATA_HORA,
                StatusAgendamento.AGENDADO);

        listener.receber(mensagem, "msg-repetida");
        listener.receber(mensagem, "msg-repetida");

        assertThat(historicoRepository.count()).isEqualTo(1);
    }

    @Test
    @ComoUsuario(role = Role.MEDICO, profissionalId = 2L)
    @DisplayName("a listagem filtra por paciente e por evento")
    void listagemFiltra() throws Exception {
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_CRIADO, 10L, DATA_HORA,
                StatusAgendamento.AGENDADO), "msg-10");
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_EDITADO, 11L, DATA_HORA,
                StatusAgendamento.AGENDADO), "msg-11");

        mockMvc.perform(get("/historicos").param("evento", "AGENDAMENTO_EDITADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].agendamentoId").value(11));

        mockMvc.perform(get("/historicos").param("pacienteId", String.valueOf(PACIENTE_ANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2));

        mockMvc.perform(get("/historicos").param("pacienteId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @ComoUsuario(role = Role.ENFERMEIRO, profissionalId = 2L)
    @DisplayName("id inexistente responde 404 e evento invalido responde 400")
    void errosDeCliente() throws Exception {
        mockMvc.perform(get("/historicos/{id}", 999999))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/historicos").param("evento", "AGENDAMENTO_INVENTADO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("sem token nenhuma rota do historico responde: 401 na leitura e na edicao")
    void semTokenNaoLeNemEdita() throws Exception {
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_CRIADO, 20L, DATA_HORA,
                StatusAgendamento.AGENDADO), "msg-20");
        Long id = idDaPrimeiraLinha();

        mockMvc.perform(get("/historicos"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/historicos/{id}", id))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/historicos/agendamentos/{id}", 20))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/historicos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"tentativa\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @ComoUsuario(role = Role.MEDICO, login = "dr.carlos", profissionalId = 2L)
    @DisplayName("medico edita a observacao e a resposta mostra quem editou")
    void medicoEditaObservacao() throws Exception {
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_CRIADO, 21L, DATA_HORA,
                StatusAgendamento.AGENDADO), "msg-21");
        Long id = idDaPrimeiraLinha();

        mockMvc.perform(patch("/historicos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"paciente relatou melhora\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacao").value("paciente relatou melhora"))
                .andExpect(jsonPath("$.observacaoEditadaPor").value("dr.carlos"))
                .andExpect(jsonPath("$.observacaoEditadaEm").isNotEmpty())
                // O retrato do evento nao muda junto com a anotacao.
                .andExpect(jsonPath("$.evento").value("AGENDAMENTO_CRIADO"))
                .andExpect(jsonPath("$.status").value("AGENDADO"));

        mockMvc.perform(get("/historicos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacao").value("paciente relatou melhora"));
    }

    @Test
    @ComoUsuario(role = Role.ENFERMEIRO, profissionalId = 3L)
    @DisplayName("enfermeiro le tudo mas nao edita: 403 no PATCH e observacao intacta")
    void enfermeiroLeMasNaoEdita() throws Exception {
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_CRIADO, 22L, DATA_HORA,
                StatusAgendamento.AGENDADO), "msg-22");
        Long id = idDaPrimeiraLinha();

        mockMvc.perform(get("/historicos/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/historicos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"enfermeiro tentando editar\"}"))
                .andExpect(status().isForbidden());

        assertThat(historicoRepository.findById(id).orElseThrow().getObservacao())
                .isEqualTo("primeira consulta");
    }

    @Test
    @ComoUsuario(role = Role.MEDICO, profissionalId = 2L)
    @DisplayName("observacao acima do limite da coluna responde 400, e nao 500 no flush")
    void observacaoLongaEhRecusada() throws Exception {
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_CRIADO, 23L, DATA_HORA,
                StatusAgendamento.AGENDADO), "msg-23");
        Long id = idDaPrimeiraLinha();

        mockMvc.perform(patch("/historicos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"%s\"}".formatted("x".repeat(501))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @ComoUsuario(role = Role.PACIENTE, pacienteId = PACIENTE_ANA)
    @DisplayName("paciente so enxerga o proprio historico, em qualquer uma das rotas de leitura")
    void pacienteVeSoOProprioHistorico() throws Exception {
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_CRIADO, 30L, DATA_HORA,
                StatusAgendamento.AGENDADO, PACIENTE_ANA, "Ana Souza"), "msg-30");
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_CRIADO, 31L, DATA_HORA,
                StatusAgendamento.AGENDADO, PACIENTE_BRUNO, "Bruno Dias"), "msg-31");

        Long idDoBruno = historicoRepository.findAll().stream()
                .filter(linha -> linha.getPacienteId().equals(PACIENTE_BRUNO))
                .findFirst().orElseThrow().getId();

        // Listagem: so a propria linha, mesmo sem filtrar nada.
        mockMvc.perform(get("/historicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].paciente.id").value(PACIENTE_ANA));

        // Pedir explicitamente o id de outro paciente devolve vazio, nao o alheio.
        mockMvc.perform(get("/historicos").param("pacienteId", String.valueOf(PACIENTE_BRUNO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));

        // 404 e nao 403: um 403 confirmaria que a linha existe.
        mockMvc.perform(get("/historicos/{id}", idDoBruno))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/historicos/agendamentos/{id}", 31))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @ComoUsuario(role = Role.PACIENTE, pacienteId = PACIENTE_ANA)
    @DisplayName("paciente nao edita nem o proprio historico")
    void pacienteNaoEdita() throws Exception {
        listener.receber(mensagem(AgendamentoEvento.AGENDAMENTO_CRIADO, 32L, DATA_HORA,
                StatusAgendamento.AGENDADO), "msg-32");
        Long id = idDaPrimeiraLinha();

        mockMvc.perform(patch("/historicos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"paciente tentando editar\"}"))
                .andExpect(status().isForbidden());
    }

    private Long idDaPrimeiraLinha() {
        return historicoRepository.findAll().getFirst().getId();
    }

    private MensagemFila mensagem(AgendamentoEvento evento, Long agendamentoId,
                                  OffsetDateTime dataHora, StatusAgendamento status) {
        return mensagem(evento, agendamentoId, dataHora, status, PACIENTE_ANA, "Ana Souza");
    }

    private MensagemFila mensagem(AgendamentoEvento evento, Long agendamentoId,
                                  OffsetDateTime dataHora, StatusAgendamento status,
                                  Long pacienteId, String pacienteNome) {
        Agendamento agendamento = new Agendamento(
                agendamentoId,
                dataHora,
                status,
                "primeira consulta",
                new Agendamento.Paciente(pacienteId, pacienteNome, "11988887777", "ana@exemplo.com"),
                new Agendamento.Profissional(2L, "Dr. Carlos Lima", "Cardiologia"),
                new Agendamento.Procedimento(3L, "Consulta cardiologica"),
                new Agendamento.Convenio(4L, "Unimed"));

        return new MensagemFila(evento, agendamento);
    }
}
