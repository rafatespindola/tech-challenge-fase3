package br.com.fiap.agendamento.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record CriarPacienteResponse(
        UUID id,
        String nome,
        String telefone,
        String email,
        String cpf,
        LocalDate dataNascimento
) {}