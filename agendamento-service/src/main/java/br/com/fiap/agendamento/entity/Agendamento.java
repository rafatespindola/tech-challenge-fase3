package br.com.fiap.agendamento.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "agendamento",
        indexes = {
                @Index(name = "idx_agendamento_paciente_data", columnList = "paciente_id, data_hora"),
                @Index(name = "idx_agendamento_profissional_data", columnList = "profissional_id, data_hora")
        })
public class Agendamento extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long versao;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_agendamento_paciente"))
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_agendamento_profissional"))
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "procedimento_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_agendamento_procedimento"))
    private Procedimento procedimento;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "convenio_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_agendamento_convenio"))
    private Convenio convenio;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAgendamento status;

    @Column(length = 500)
    private String observacao;

    protected Agendamento() {
    }

    public Agendamento(Paciente paciente, Profissional profissional, Procedimento procedimento,
                       Convenio convenio, OffsetDateTime dataHora, String observacao) {
        this.paciente = paciente;
        this.profissional = profissional;
        this.procedimento = procedimento;
        this.convenio = convenio;
        this.dataHora = dataHora;
        this.observacao = observacao;
        this.status = StatusAgendamento.AGENDADO;
    }

    public void alterar(OffsetDateTime novaDataHora, StatusAgendamento novoStatus, String novaObservacao) {
        if (novaDataHora != null) {
            this.dataHora = novaDataHora;
        }
        if (novoStatus != null) {
            this.status = novoStatus;
        }
        if (novaObservacao != null) {
            this.observacao = novaObservacao;
        }
    }

    public void cancelar() {
        this.status = StatusAgendamento.CANCELADO;
    }

}
