package br.com.fiap.agendamento.dto.response;

import java.util.UUID;

public record CriarProfissionalResponse(
        UUID id,
        String nome
) {}
