package br.com.fiap.agendamento.dto;

import br.com.fiap.agendamento.entity.Role;

import java.time.OffsetDateTime;

/**
 * Resposta do login. Nome e role vao junto para o cliente montar a tela sem
 * precisar decodificar o token.
 */
public record AutenticacaoPayload(
        String token,
        OffsetDateTime expiraEm,
        String nome,
        Role role
) { }
