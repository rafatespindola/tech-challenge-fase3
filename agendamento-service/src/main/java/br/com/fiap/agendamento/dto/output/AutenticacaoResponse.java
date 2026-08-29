package br.com.fiap.agendamento.dto.output;

import br.com.fiap.agendamento.entity.Role;

import java.time.OffsetDateTime;

/**
 * Resposta do login. Nome e role vao junto para o cliente montar a tela sem
 * precisar decodificar o token.
 */
public record AutenticacaoResponse(
        String token,
        String tipo,
        OffsetDateTime expiraEm,
        String nome,
        Role role
) {
    private static final String BEARER = "Bearer";

    public static AutenticacaoResponse bearer(String token, OffsetDateTime expiraEm,
                                              String nome, Role role) {
        return new AutenticacaoResponse(token, BEARER, expiraEm, nome, role);
    }
}
