package br.com.fiap.agendamento.dto.input;

import br.com.fiap.agendamento.entity.StatusAgendamento;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** Atualizacao parcial: campo nulo significa "nao mexer", nao "limpar". */
public record AtualizarAgendamentoInput(

        @Future(message = "nao e possivel reagendar para uma data no passado")
        OffsetDateTime dataHora,

        StatusAgendamento status,

        @Size(max = 500) String observacao
) { }
