package br.com.fiap.agendamento.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CriarPacienteRequest(
        @NotBlank String nome,
        @NotBlank String telefone,
        @Email String email,
        @NotBlank @Size(min = 11, max = 11) String cpf,
        @Past LocalDate dataNascimento
) {}
