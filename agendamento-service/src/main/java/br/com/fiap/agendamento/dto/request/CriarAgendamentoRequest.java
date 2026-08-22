package br.com.fiap.agendamento.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CriarAgendamentoRequest(
        @NotNull UUID pacienteId,
        @NotNull UUID profissionalId,
        @NotNull UUID pagamentoId,
        @NotNull @Future LocalDateTime dateTime
) {}
