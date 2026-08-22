package br.com.fiap.agendamento.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agendamento")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Agendamento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @NotNull
    @Column(name = "profissional_id", nullable = false)
    private UUID profissionalId;

    @NotNull
    @Column(name = "pagamento_id", nullable = false)
    private UUID pagamentoId;

    @NotNull
    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;
}
