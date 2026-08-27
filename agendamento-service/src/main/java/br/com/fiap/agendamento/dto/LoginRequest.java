package br.com.fiap.agendamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do POST /auth/login. */
public record LoginRequest(

        @NotBlank @Size(max = 60) String login,

        @NotBlank String senha
) { }
