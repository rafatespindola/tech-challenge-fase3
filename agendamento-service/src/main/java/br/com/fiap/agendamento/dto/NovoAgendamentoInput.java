package br.com.fiap.agendamento.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record NovoAgendamentoInput(

        @NotNull Long pacienteId,
        @NotNull Long profissionalId,
        @NotNull Long procedimentoId,
        @NotNull Long convenioId,

        @NotNull
        @Future(message = "nao e possivel agendar para uma data no passado")
        OffsetDateTime dataHora,

        @Size(max = 500) String observacao
) { }
