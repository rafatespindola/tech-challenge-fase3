package br.com.fiap.agendamento.service;

import br.com.fiap.agendamento.dto.AutenticacaoPayload;
import br.com.fiap.agendamento.dto.LoginInput;
import br.com.fiap.agendamento.security.JwtService;
import br.com.fiap.agendamento.security.UsuarioAutenticado;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AutenticacaoService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Delega a conferencia de login e senha ao AuthenticationManager e, se ela
     * passar, troca a autenticacao por um token.
     *
     * A AuthenticationException que sobe daqui vira UNAUTHORIZED no
     * GraphQlExceptionResolver - nao e capturada de proposito.
     */
    public AutenticacaoPayload autenticar(LoginInput input) {
        Authentication autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.login(), input.senha()));

        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacao.getPrincipal();
        JwtService.Token token = jwtService.gerar(usuario);

        return new AutenticacaoPayload(
                token.valor(), token.expiraEm(), usuario.nome(), usuario.role());
    }
}
