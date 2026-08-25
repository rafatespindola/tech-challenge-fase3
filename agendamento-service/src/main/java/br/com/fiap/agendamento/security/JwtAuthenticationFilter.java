package br.com.fiap.agendamento.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Le o header Authorization e, se o token for valido, popula o SecurityContext.
 *
 * Nao rejeita nada: requisicao sem token ou com token invalido apenas segue
 * anonima, e quem barra e a autorizacao no nivel do campo GraphQL. Isso e o que
 * permite a mutation de login conviver com as demais no mesmo endpoint.
 *
 * Nao e um bean: registrado a mao em SecurityConfig porque todo bean do tipo
 * Filter seria tambem adicionado a cadeia do servlet pelo Boot, rodando duas vezes.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFIXO_BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.autenticacaoDe(header.substring(PREFIXO_BEARER.length()))
                    .ifPresent(autenticacao ->
                            SecurityContextHolder.getContext().setAuthentication(autenticacao));
        }

        chain.doFilter(request, response);
    }
}
