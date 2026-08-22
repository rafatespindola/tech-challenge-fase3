package br.com.fiap.agendamento.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CriarAgendamentoResponse(
        UUID id,
        UUID pacienteId,
        UUID profissionalId,
        UUID pagamentoId,
        LocalDateTime dateTime
) {}
