package br.com.fiap.agendamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginInput(

        @NotBlank @Size(max = 60) String login,

        @NotBlank String senha
) { }
