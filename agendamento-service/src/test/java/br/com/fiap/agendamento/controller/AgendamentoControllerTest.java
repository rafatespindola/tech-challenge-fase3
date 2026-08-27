package br.com.fiap.agendamento.controller;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureGraphQlTester
@ComoUsuario(role = Role.MEDICO, login = "medico", profissionalId = 1)
class AgendamentoControllerTest {

    private static final String MUTATION_CRIAR = """
            mutation($pacienteId: ID!, $profissionalId: ID!, $procedimentoId: ID!,
                     $convenioId: ID!, $dataHora: DateTime!, $observacao: String) {
              criarAgendamento(input: {
                pacienteId: $pacienteId,
                profissionalId: $profissionalId,
                procedimentoId: $procedimentoId,
                convenioId: $convenioId,
                dataHora: $dataHora,
                observacao: $observacao
              }) {
                id
                dataHora
                status
                observacao
                versao
                paciente { id nome }
                profissional { id nome cargo }
                procedimento { id nome duracaoMinutos }
                convenio { id nome }
              }
            }
            """;

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

    final OffsetDateTime amanha = OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS);

    @BeforeEach
    void preparar() {
        agendamentoRepository.deleteAll();

        pacienteId = pacienteRepository.save(Paciente.builder()
                .nome("Maria Silva").telefone("91999998888").build()).getId();
        outroPacienteId = pacienteRepository.save(Paciente.builder()
                .nome("Joao Souza").telefone("91988887777").build()).getId();
        profissionalId = profissionalRepository.save(Profissional.builder()
                .nome("Dra. Helena Prado").cargo(Cargo.MEDICO)
                .especialidade("Cardiologia").registroConselho("CRM-PA 12345").build()).getId();
        procedimentoId = procedimentoRepository.save(
                new Procedimento("Consulta " + System.nanoTime(), 30)).getId();
        convenioId = convenioRepository.save(
                new Convenio(null, "Particular " + System.nanoTime())).getId();
    }

    // ------------------------------------------------------------------ create

    @Test
    void criaAgendamentoComStatusAgendado() {
        GraphQlTester.Response resposta = criar(amanha, "Primeira consulta");

        resposta.path("criarAgendamento.status").entity(String.class).isEqualTo("AGENDADO");
        resposta.path("criarAgendamento.observacao").entity(String.class).isEqualTo("Primeira consulta");
        resposta.path("criarAgendamento.paciente.nome").entity(String.class).isEqualTo("Maria Silva");
        resposta.path("criarAgendamento.profissional.cargo").entity(String.class).isEqualTo("MEDICO");
        resposta.path("criarAgendamento.procedimento.duracaoMinutos").entity(Integer.class).isEqualTo(30);
        resposta.path("criarAgendamento.versao").entity(Integer.class).isEqualTo(0);

        assertThat(agendamentoRepository.count()).isEqualTo(1);
    }

    @Test
    void recusaAgendamentoComIdInexistente() {
        criarComVariaveis(variaveis(99999L, profissionalId, amanha, null))
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.NOT_FOUND
                        && erro.getMessage().contains("Paciente"))
                .verify();
    }

    @Test
    void recusaAgendamentoNoPassado() {
        criarComVariaveis(variaveis(pacienteId, profissionalId,
                OffsetDateTime.now().minusDays(1), null))
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.BAD_REQUEST)
                .verify();

        assertThat(agendamentoRepository.count()).isZero();
    }

    @Test
    void recusaDoisAgendamentosDoMesmoProfissionalNoMesmoHorario() {
        criar(amanha, null);

        criarComVariaveis(variaveis(outroPacienteId, profissionalId, amanha, null))
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.BAD_REQUEST
                        && erro.getMessage().contains("profissional"))
                .verify();

        assertThat(agendamentoRepository.count()).isEqualTo(1);
    }

    // -------------------------------------------------------------------- read

    @Test
    void buscaPorIdRetornaOAgendamento() {
        String id = idDoNovo(criar(amanha, "consulta"));

        tester.document("query($id: ID!) { agendamento(id: $id) { id status observacao } }")
                .variable("id", id)
                .execute()
                .path("agendamento.id").entity(String.class).isEqualTo(id)
                .path("agendamento.observacao").entity(String.class).isEqualTo("consulta");
    }

    @Test
    void buscaPorIdRetornaNullQuandoNaoExiste() {
        tester.document("query { agendamento(id: \"99999\") { id } }")
                .execute()
                .path("agendamento").valueIsNull();
    }

    @Test
    void listaFiltrandoPorProfissionalEStatus() {
        criar(amanha, null);
        criar(amanha.plusHours(2), null);

        tester.document("""
                        query($profissionalId: ID) {
                          agendamentos(filtro: {profissionalId: $profissionalId, status: AGENDADO},
                                       pagina: 0, tamanho: 10) {
                            totalElementos
                            totalPaginas
                            pagina
                            tamanho
                            conteudo { id dataHora status }
                          }
                        }
                        """)
                .variable("profissionalId", profissionalId.toString())
                .execute()
                .path("agendamentos.totalElementos").entity(Integer.class).isEqualTo(2)
                .path("agendamentos.totalPaginas").entity(Integer.class).isEqualTo(1)
                .path("agendamentos.conteudo").entityList(Object.class).hasSize(2);
    }

    @Test
    void listaFiltrandoPorIntervaloDeData() {
        criar(amanha, null);
        criar(amanha.plusDays(10), null);

        tester.document("""
                        query($de: DateTime, $ate: DateTime) {
                          agendamentos(filtro: {de: $de, ate: $ate}) { totalElementos }
                        }
                        """)
                .variable("de", amanha.minusHours(1).toString())
                .variable("ate", amanha.plusHours(1).toString())
                .execute()
                .path("agendamentos.totalElementos").entity(Integer.class).isEqualTo(1);
    }

    @Test
    void listaAplicaPaginacao() {
        criar(amanha, null);
        criar(amanha.plusHours(2), null);
        criar(amanha.plusHours(4), null);

        tester.document("""
                        query { agendamentos(pagina: 1, tamanho: 2) {
                          pagina totalElementos totalPaginas conteudo { id }
                        } }
                        """)
                .execute()
                .path("agendamentos.pagina").entity(Integer.class).isEqualTo(1)
                .path("agendamentos.totalElementos").entity(Integer.class).isEqualTo(3)
                .path("agendamentos.totalPaginas").entity(Integer.class).isEqualTo(2)
                .path("agendamentos.conteudo").entityList(Object.class).hasSize(1);
    }

    // ------------------------------------------------------------------ update

    @Test
    void atualizaDataHoraEObservacao() {
        String id = idDoNovo(criar(amanha, "antes"));
        OffsetDateTime novaData = amanha.plusDays(3);

        tester.document("""
                        mutation($id: ID!, $dataHora: DateTime, $observacao: String) {
                          atualizarAgendamento(id: $id, input: {dataHora: $dataHora, observacao: $observacao}) {
                            id status observacao versao
                          }
                        }
                        """)
                .variable("id", id)
                .variable("dataHora", novaData.toString())
                .variable("observacao", "depois")
                .execute()
                .path("atualizarAgendamento.observacao").entity(String.class).isEqualTo("depois")
                .path("atualizarAgendamento.status").entity(String.class).isEqualTo("AGENDADO")
                .path("atualizarAgendamento.versao").entity(Integer.class).isEqualTo(1);

        assertThat(agendamentoRepository.findById(Long.valueOf(id)).orElseThrow().getDataHora())
                .isEqualTo(novaData);
    }

    @Test
    void atualizaApenasOsCamposInformados() {
        String id = idDoNovo(criar(amanha, "observacao original"));

        tester.document("""
                        mutation($id: ID!) {
                          atualizarAgendamento(id: $id, input: {status: REALIZADO}) {
                            status observacao
                          }
                        }
                        """)
                .variable("id", id)
                .execute()
                .path("atualizarAgendamento.status").entity(String.class).isEqualTo("REALIZADO")
                .path("atualizarAgendamento.observacao").entity(String.class).isEqualTo("observacao original");
    }

    @Test
    void naoAtualizaAgendamentoCancelado() {
        String id = idDoNovo(criar(amanha, null));
        cancelar(id);

        tester.document("""
                        mutation($id: ID!) {
                          atualizarAgendamento(id: $id, input: {observacao: "tarde demais"}) { id }
                        }
                        """)
                .variable("id", id)
                .execute()
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.BAD_REQUEST)
                .verify();
    }

    @Test
    void atualizarIdInexistenteRetornaNotFound() {
        tester.document("""
                        mutation { atualizarAgendamento(id: "99999", input: {observacao: "x"}) { id } }
                        """)
                .execute()
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.NOT_FOUND)
                .verify();
    }

    // ------------------------------------------------------------------ delete

    @Test
    void cancelaPreservandoORegistro() {
        String id = idDoNovo(criar(amanha, null));

        cancelar(id).path("cancelarAgendamento.status").entity(String.class).isEqualTo("CANCELADO");

        assertThat(agendamentoRepository.existsById(Long.valueOf(id))).isTrue();
    }

    @Test
    void horarioLiberadoAposCancelamento() {
        cancelar(idDoNovo(criar(amanha, null)));

        criar(amanha, null).path("criarAgendamento.status").entity(String.class).isEqualTo("AGENDADO");
    }

    @Test
    void removeFisicamente() {
        String id = idDoNovo(criar(amanha, null));

        tester.document("mutation($id: ID!) { removerAgendamento(id: $id) }")
                .variable("id", id)
                .execute()
                .path("removerAgendamento").entity(Boolean.class).isEqualTo(true);

        assertThat(agendamentoRepository.existsById(Long.valueOf(id))).isFalse();
    }

    @Test
    void removerRetornaFalseQuandoNaoExiste() {
        tester.document("mutation { removerAgendamento(id: \"99999\") }")
                .execute()
                .path("removerAgendamento").entity(Boolean.class).isEqualTo(false);
    }

    // ----------------------------------------------------------------- helpers

    private GraphQlTester.Response criar(OffsetDateTime dataHora, String observacao) {
        return criarComVariaveis(variaveis(pacienteId, profissionalId, dataHora, observacao));
    }

    private Map<String, Object> variaveis(Long paciente, Long profissional,
                                          OffsetDateTime dataHora, String observacao) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("pacienteId", paciente.toString());
        mapa.put("profissionalId", profissional.toString());
        mapa.put("procedimentoId", procedimentoId.toString());
        mapa.put("convenioId", convenioId.toString());
        mapa.put("dataHora", dataHora.toString());
        mapa.put("observacao", observacao);
        return mapa;
    }

    private GraphQlTester.Response criarComVariaveis(Map<String, Object> variaveis) {
        GraphQlTester.Request<?> request = tester.document(MUTATION_CRIAR);
        for (Map.Entry<String, Object> entrada : variaveis.entrySet()) {
            request = request.variable(entrada.getKey(), entrada.getValue());
        }
        return request.execute();
    }

    private GraphQlTester.Response cancelar(String id) {
        return tester.document("mutation($id: ID!) { cancelarAgendamento(id: $id) { id status } }")
                .variable("id", id)
                .execute();
    }

    private String idDoNovo(GraphQlTester.Response resposta) {
        return resposta.path("criarAgendamento.id").entity(String.class).get();
    }
}
