package br.com.fiap.agendamento.security;

import br.com.fiap.agendamento.entity.Role;
import br.com.fiap.agendamento.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal que fica no SecurityContext durante a requisicao.
 *
 * Existe em duas situacoes: montado a partir da entidade no login (com o hash,
 * que o DaoAuthenticationProvider precisa comparar) e montado a partir das
 * claims do token nas requisicoes seguintes (com {@code senhaHash} nulo, porque
 * ai nao ha nada a conferir - a assinatura ja garantiu a procedencia).
 */
public record UsuarioAutenticado(
        Long id,
        String login,
        String nome,
        Role role,
        String senhaHash
) implements UserDetails {

    public static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getLogin(),
                usuario.getNome(), usuario.getRole(), usuario.getSenha());
    }

    /** Sem o hash: usado ao reconstruir o principal a partir do token. */
    public static UsuarioAutenticado deToken(Long id, String login, String nome, Role role) {
        return new UsuarioAutenticado(id, login, nome, role, null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return login;
    }
}
