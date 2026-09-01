package br.com.fiap.historico.entity;

import br.com.fiap.historico.dto.input.Agendamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

/**
 * Retrato de um evento recebido: uma linha por evento, e o retrato em si nunca
 * muda - evento, status, ids, datas e nomes ficam como chegaram. A unica
 * excecao e a {@code observacao}, que um MEDICO pode corrigir depois; quando
 * isso acontece, {@code observacaoEditadaPor} e {@code observacaoEditadaEm}
 * registram a alteracao, para o historico continuar contando a verdade sobre si
 * mesmo. Por isso
 * os dados do paciente, profissional, procedimento e convenio vem copiados em
 * colunas planas em vez de chaves estrangeiras - o historico tem que continuar
 * legivel depois que o agendamento e excluido no agendamento-service, e tem que
 * mostrar o nome que valia na epoca, nao o de hoje.
 */
@Entity
@Table(name = "historico_agendamento",
        uniqueConstraints = @UniqueConstraint(name = "uk_historico_message_id", columnNames = "message_id"),
        indexes = {
                @Index(name = "idx_historico_agendamento", columnList = "agendamento_id, registrado_em"),
                @Index(name = "idx_historico_paciente", columnList = "paciente_id, registrado_em"),
                @Index(name = "idx_historico_profissional", columnList = "profissional_id, registrado_em"),
                @Index(name = "idx_historico_registrado_em", columnList = "registrado_em")
        })
public class HistoricoAgendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Id da mensagem AMQP. Unico para que uma reentrega (nack, restart do
     * consumidor, retry) nao vire uma segunda linha de historico. Nulo e
     * aceito - o MySQL permite varios NULL num unique - para o caso de uma
     * mensagem publicada sem o header.
     */
    @Column(name = "message_id", length = 36)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgendamentoEvento evento;

    /** Quando este servico gravou o evento, nao quando o agendamento ocorre. */
    @Column(name = "registrado_em", nullable = false, updatable = false)
    private OffsetDateTime registradoEm;

    /** Id no agendamento-service. Repete entre linhas: e a chave da linha do tempo. */
    @Column(name = "agendamento_id", nullable = false)
    private Long agendamentoId;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAgendamento status;

    /**
     * Anotacao clinica. Chega copiada do agendamento e e o unico campo que a API
     * deixa alterar - ver {@link #editarObservacao}.
     */
    @Column(length = 500)
    private String observacao;

    /** Login de quem editou a observacao. Nulo enquanto ninguem editou. */
    @Column(name = "observacao_editada_por", length = 60)
    private String observacaoEditadaPor;

    @Column(name = "observacao_editada_em")
    private OffsetDateTime observacaoEditadaEm;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "paciente_nome", nullable = false)
    private String pacienteNome;

    @Column(name = "paciente_telefone", length = 13)
    private String pacienteTelefone;

    @Column(name = "paciente_email")
    private String pacienteEmail;

    @Column(name = "profissional_id", nullable = false)
    private Long profissionalId;

    @Column(name = "profissional_nome", nullable = false)
    private String profissionalNome;

    /** Nulo para enfermeiro, como na origem. */
    @Column(name = "profissional_especialidade", length = 60)
    private String profissionalEspecialidade;

    @Column(name = "procedimento_id", nullable = false)
    private Long procedimentoId;

    @Column(name = "procedimento_nome", nullable = false, length = 120)
    private String procedimentoNome;

    @Column(name = "convenio_id")
    private Long convenioId;

    @Column(name = "convenio_nome", length = 120)
    private String convenioNome;

    protected HistoricoAgendamento() {
    }

    public HistoricoAgendamento(String messageId, AgendamentoEvento evento, Agendamento agendamento) {
        this.messageId = messageId;
        this.evento = evento;
        this.agendamentoId = agendamento.id();
        this.dataHora = agendamento.dataHora();
        this.status = agendamento.status();
        this.observacao = agendamento.observacao();

        Agendamento.Paciente paciente = agendamento.paciente();
        this.pacienteId = paciente.id();
        this.pacienteNome = paciente.nome();
        this.pacienteTelefone = paciente.telefone();
        this.pacienteEmail = paciente.email();

        Agendamento.Profissional profissional = agendamento.profissional();
        this.profissionalId = profissional.id();
        this.profissionalNome = profissional.nome();
        this.profissionalEspecialidade = profissional.especialidade();

        Agendamento.Procedimento procedimento = agendamento.procedimento();
        this.procedimentoId = procedimento.id();
        this.procedimentoNome = procedimento.nome();

        // Convenio nao existe no DTO do notificacao-service e pode faltar numa
        // mensagem antiga; o historico grava o que veio.
        Agendamento.Convenio convenio = agendamento.convenio();
        if (convenio != null) {
            this.convenioId = convenio.id();
            this.convenioNome = convenio.nome();
        }
    }

    /**
     * Troca a observacao e marca a autoria. Nao ha setter solto: quem edita e
     * obrigado pela assinatura a dizer quem esta editando, senao o rastro
     * ficaria opcional na pratica.
     */
    public void editarObservacao(String observacao, String editadoPor) {
        this.observacao = observacao;
        this.observacaoEditadaPor = editadoPor;
        this.observacaoEditadaEm = OffsetDateTime.now();
    }

    @PrePersist
    void aoGravar() {
        this.registradoEm = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getMessageId() { return messageId; }
    public AgendamentoEvento getEvento() { return evento; }
    public OffsetDateTime getRegistradoEm() { return registradoEm; }
    public Long getAgendamentoId() { return agendamentoId; }
    public OffsetDateTime getDataHora() { return dataHora; }
    public StatusAgendamento getStatus() { return status; }
    public String getObservacao() { return observacao; }
    public String getObservacaoEditadaPor() { return observacaoEditadaPor; }
    public OffsetDateTime getObservacaoEditadaEm() { return observacaoEditadaEm; }
    public Long getPacienteId() { return pacienteId; }
    public String getPacienteNome() { return pacienteNome; }
    public String getPacienteTelefone() { return pacienteTelefone; }
    public String getPacienteEmail() { return pacienteEmail; }
    public Long getProfissionalId() { return profissionalId; }
    public String getProfissionalNome() { return profissionalNome; }
    public String getProfissionalEspecialidade() { return profissionalEspecialidade; }
    public Long getProcedimentoId() { return procedimentoId; }
    public String getProcedimentoNome() { return procedimentoNome; }
    public Long getConvenioId() { return convenioId; }
    public String getConvenioNome() { return convenioNome; }
}
