package br.com.fiap.historico.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * Principal que fica no SecurityContext durante a requisicao, montado sempre a
 * partir das claims do token.
 *
 * Diferente do homonimo do agendamento-service, este nao implementa UserDetails
 * e nao carrega hash de senha: aqui nao existe login para conferir credencial,
 * so verificacao de assinatura. O que o token diz e tudo o que se sabe do
 * usuario - este servico nao tem tabela de usuario e nunca consulta o
 * agendamento-service para saber quem e quem.
 *
 * {@code pacienteId} vem junto porque e ele que restringe o que um PACIENTE
 * enxerga, sem precisar de ida ao banco.
 */
public record UsuarioAutenticado(
        Long id,
        String login,
        String nome,
        Role role,
        Long pacienteId,
        Long profissionalId
) {

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }
}
