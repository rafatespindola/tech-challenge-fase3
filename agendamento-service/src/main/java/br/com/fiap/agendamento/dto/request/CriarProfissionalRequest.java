package br.com.fiap.agendamento.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CriarProfissionalRequest(
        @NotBlank String nome
) {}
