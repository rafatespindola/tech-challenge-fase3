package br.com.fiap.agendamento.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class ComoUsuarioSecurityContextFactory
        implements WithSecurityContextFactory<ComoUsuario> {

    @Override
    public SecurityContext createSecurityContext(ComoUsuario anotacao) {
        UsuarioAutenticado usuario = UsuarioAutenticado.deToken(
                anotacao.id(),
                anotacao.login(),
                anotacao.nome(),
                anotacao.role(),
                vinculo(anotacao.pacienteId()),
                vinculo(anotacao.profissionalId()));

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                usuario, null, usuario.getAuthorities()));
        return contexto;
    }

    private static Long vinculo(long valor) {
        return valor > 0 ? valor : null;
    }
}
