package br.com.fiap.agendamento.controller;

import br.com.fiap.agendamento.entity.Agendamento;
import br.com.fiap.agendamento.entity.Cargo;
import br.com.fiap.agendamento.entity.Convenio;
import br.com.fiap.agendamento.entity.Paciente;
import br.com.fiap.agendamento.entity.Procedimento;
import br.com.fiap.agendamento.entity.Profissional;
import br.com.fiap.agendamento.entity.Role;
import br.com.fiap.agendamento.repository.AgendamentoRepository;
import br.com.fiap.agendamento.repository.ConvenioRepository;
import br.com.fiap.agendamento.repository.PacienteRepository;
import br.com.fiap.agendamento.repository.ProcedimentoRepository;
import br.com.fiap.agendamento.repository.ProfissionalRepository;
import br.com.fiap.agendamento.security.ComoUsuario;
import br.com.fiap.agendamento.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * O que muda de resposta conforme quem pergunta.
 *
 * Os casos de PACIENTE montam o contexto a mao porque o pacienteId so existe em
 * runtime, e anotacao nao carrega valor gerado. Nao precisa de @AfterEach: o
 * listener do spring-security-test limpa o contexto depois de cada metodo.
 *
 * Note que este tester e um ExecutionGraphQlServiceTester: ele fala direto com o
 * servico GraphQL, sem passar pela cadeia de filtros. Logo, o 401 de "sem token"
 * nao aparece aqui - isso e verdade so sobre HTTP, e esta em
 * AutenticacaoControllerTest.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
class AgendamentoControllerAutorizacaoTest {

    private static final String QUERY_LISTA = """
            query($pacienteId: ID) {
              agendamentos(filtro: {pacienteId: $pacienteId}) {
                totalElementos
                conteudo { id paciente { id nome } }
              }
            }
            """;

    private static final String MUTATION_CRIAR = """
            mutation($pacienteId: ID!, $profissionalId: ID!, $procedimentoId: ID!,
                     $convenioId: ID!, $dataHora: DateTime!) {
              criarAgendamento(input: {
                pacienteId: $pacienteId, profissionalId: $profissionalId,
                procedimentoId: $procedimentoId, convenioId: $convenioId, dataHora: $dataHora
              }) { id }
            }
            """;

    // Sem broker no ambiente de teste. O mock deixa o PublishService rodar de
    // verdade (routing key incluida) sem abrir conexao AMQP.
    @MockBean RabbitTemplate rabbitTemplate;

    @Autowired GraphQlTester tester;

    @Autowired AgendamentoRepository agendamentoRepository;
    @Autowired PacienteRepository pacienteRepository;
    @Autowired ProfissionalRepository profissionalRepository;
    @Autowired ProcedimentoRepository procedimentoRepository;
    @Autowired ConvenioRepository convenioRepository;

    Long pacienteId;
    Long outroPacienteId;
    Long profissionalId;
    Long procedimentoId;
    Long convenioId;
    Long agendamentoProprioId;
    Long agendamentoAlheioId;

    @BeforeEach
    void preparar() {
        agendamentoRepository.deleteAll();

        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .nome("Maria Silva").telefone("91999998888").build());
        Paciente outroPaciente = pacienteRepository.save(Paciente.builder()
                .nome("Joao Souza").telefone("91988887777").build());
        Profissional profissional = profissionalRepository.save(Profissional.builder()
                .nome("Dra. Helena Prado").cargo(Cargo.MEDICO)
                .especialidade("Cardiologia").registroConselho("CRM-PA 12345").build());
        Procedimento procedimento = procedimentoRepository.save(
                new Procedimento("Consulta " + System.nanoTime(), 30));
        Convenio convenio = convenioRepository.save(
                new Convenio(null, "Particular " + System.nanoTime()));

        pacienteId = paciente.getId();
        outroPacienteId = outroPaciente.getId();
        profissionalId = profissional.getId();
        procedimentoId = procedimento.getId();
        convenioId = convenio.getId();

        OffsetDateTime amanha = OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS);

        // Gravados direto pelo repositorio: um PACIENTE nao consegue criar pela API,
        // e e justamente isso que os testes abaixo verificam.
        agendamentoProprioId = agendamentoRepository.save(new Agendamento(
                paciente, profissional, procedimento, convenio, amanha, "propria")).getId();
        agendamentoAlheioId = agendamentoRepository.save(new Agendamento(
                outroPaciente, profissional, procedimento, convenio, amanha.plusHours(2), "alheia")).getId();
    }

    // ------------------------------------------------------------- consulta

    @Test
    void pacienteVeApenasOsProprios() {
        autenticarComoPaciente(pacienteId);

        tester.document(QUERY_LISTA)
                .execute()
                .path("agendamentos.totalElementos").entity(Integer.class).isEqualTo(1)
                .path("agendamentos.conteudo[0].paciente.nome")
                .entity(String.class).isEqualTo("Maria Silva");
    }

    @Test
    void pacientePedindoAgendaDeOutroRecebeAPropria() {
        autenticarComoPaciente(pacienteId);

        tester.document(QUERY_LISTA)
                .variable("pacienteId", outroPacienteId.toString())
                .execute()
                .path("agendamentos.totalElementos").entity(Integer.class).isEqualTo(1)
                .path("agendamentos.conteudo[0].paciente.nome")
                .entity(String.class).isEqualTo("Maria Silva");
    }

    @Test
    @ComoUsuario(role = Role.MEDICO, login = "medico", profissionalId = 1)
    void profissionalVeOsAgendamentosDeTodos() {
        tester.document(QUERY_LISTA)
                .execute()
                .path("agendamentos.totalElementos").entity(Integer.class).isEqualTo(2);
    }

    @Test
    void pacienteBuscaOProprioPorId() {
        autenticarComoPaciente(pacienteId);

        buscarPorId(agendamentoProprioId)
                .path("agendamento.observacao").entity(String.class).isEqualTo("propria");
    }

    /**
     * Alheio e inexistente respondem igual, de proposito: um erro diferente para
     * "existe mas nao e seu" deixaria o paciente descobrir quais ids existem.
     */
    @Test
    void pacienteNaoDistingueAgendamentoAlheioDeInexistente() {
        autenticarComoPaciente(pacienteId);

        buscarPorId(agendamentoAlheioId).path("agendamento").valueIsNull();
        buscarPorId(99999L).path("agendamento").valueIsNull();
    }

    // ------------------------------------------------------------- alteracao

    @Test
    void pacienteNaoPodeCriar() {
        autenticarComoPaciente(pacienteId);

        tester.document(MUTATION_CRIAR)
                .variable("pacienteId", pacienteId.toString())
                .variable("profissionalId", profissionalId.toString())
                .variable("procedimentoId", procedimentoId.toString())
                .variable("convenioId", convenioId.toString())
                .variable("dataHora", OffsetDateTime.now().plusDays(5).toString())
                .execute()
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.FORBIDDEN)
                .verify();
    }

    @Test
    void pacienteNaoPodeCancelar() {
        autenticarComoPaciente(pacienteId);

        tester.document("mutation($id: ID!) { cancelarAgendamento(id: $id) { id status } }")
                .variable("id", agendamentoProprioId.toString())
                .execute()
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.FORBIDDEN)
                .verify();
    }

    @Test
    void pacienteNaoPodeRemover() {
        autenticarComoPaciente(pacienteId);

        tester.document("mutation($id: ID!) { removerAgendamento(id: $id) }")
                .variable("id", agendamentoProprioId.toString())
                .execute()
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.FORBIDDEN)
                .verify();
    }

    @Test
    @ComoUsuario(role = Role.MEDICO, login = "medico", profissionalId = 1)
    void medicoPodeCancelar() {
        tester.document("mutation($id: ID!) { cancelarAgendamento(id: $id) { id status } }")
                .variable("id", agendamentoProprioId.toString())
                .execute()
                .path("cancelarAgendamento.status").entity(String.class).isEqualTo("CANCELADO");
    }

    /**
     * Ser profissional nao basta: cada mutation e de uma role so. Cancelar e do
     * medico, e o enfermeiro bate no mesmo FORBIDDEN que o paciente.
     */
    @Test
    @ComoUsuario(role = Role.ENFERMEIRO, login = "enfermeiro", profissionalId = 2)
    void enfermeiroNaoPodeCancelar() {
        tester.document("mutation($id: ID!) { cancelarAgendamento(id: $id) { id status } }")
                .variable("id", agendamentoProprioId.toString())
                .execute()
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.FORBIDDEN)
                .verify();
    }

    /** O lado espelhado: criar e do enfermeiro, e o medico fica de fora. */
    @Test
    @ComoUsuario(role = Role.MEDICO, login = "medico", profissionalId = 1)
    void medicoNaoPodeCriar() {
        tester.document(MUTATION_CRIAR)
                .variable("pacienteId", pacienteId.toString())
                .variable("profissionalId", profissionalId.toString())
                .variable("procedimentoId", procedimentoId.toString())
                .variable("convenioId", convenioId.toString())
                .variable("dataHora", OffsetDateTime.now().plusDays(5).toString())
                .execute()
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.FORBIDDEN)
                .verify();
    }

    @Test
    @ComoUsuario(role = Role.ENFERMEIRO, login = "enfermeiro", profissionalId = 2)
    void enfermeiroPodeCriar() {
        tester.document(MUTATION_CRIAR)
                .variable("pacienteId", pacienteId.toString())
                .variable("profissionalId", profissionalId.toString())
                .variable("procedimentoId", procedimentoId.toString())
                .variable("convenioId", convenioId.toString())
                .variable("dataHora", OffsetDateTime.now().plusDays(5).toString())
                .execute()
                .path("criarAgendamento.id").hasValue();
    }

    // ------------------------------------------------------------- apoio

    private GraphQlTester.Response buscarPorId(Long id) {
        return tester.document("query($id: ID!) { agendamento(id: $id) { id observacao } }")
                .variable("id", id.toString())
                .execute();
    }

    private void autenticarComoPaciente(Long pacienteId) {
        UsuarioAutenticado usuario = UsuarioAutenticado.deToken(
                10L, "paciente", "Maria Silva", Role.PACIENTE, pacienteId, null);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        usuario, null, usuario.getAuthorities()));
    }
}
