package br.com.fiap.agendamento.service;

import br.com.fiap.agendamento.dto.output.AutenticacaoResponse;
import br.com.fiap.agendamento.dto.input.LoginRequest;
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
     * A AuthenticationException que sobe daqui vira 401 no ApiExceptionHandler -
     * nao e capturada de proposito.
     */
    public AutenticacaoResponse autenticar(LoginRequest requisicao) {
        Authentication autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requisicao.login(), requisicao.senha()));

        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacao.getPrincipal();
        JwtService.Token token = jwtService.gerar(usuario);

        return AutenticacaoResponse.bearer(
                token.valor(), token.expiraEm(), usuario.nome(), usuario.role());
    }
}
