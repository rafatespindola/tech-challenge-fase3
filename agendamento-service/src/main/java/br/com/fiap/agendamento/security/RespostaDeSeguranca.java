package br.com.fiap.agendamento.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Respostas das falhas que acontecem na cadeia de filtros, antes de qualquer
 * controller - portanto fora do alcance do @RestControllerAdvice.
 *
 * Uma classe para os dois papeis porque o corpo e o mesmo, so muda o status:
 * 401 quando nao ha token valido, 403 quando ha mas a role nao basta. O bean
 * unico atende as duas injecoes em SecurityConfig.
 *
 * O ObjectMapper vem injetado, nao instanciado aqui: o do Boot ja traz o
 * JavaTimeModule e o mixin que achata o ProblemDetail no formato RFC 7807.
 */
@Component
public class RespostaDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespostaDeSeguranca(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException excecao) throws IOException {
        escrever(request, response, HttpStatus.UNAUTHORIZED,
                "Autenticacao necessaria: envie o header Authorization: Bearer <token>.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excecao) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN,
                "Sem permissao para acessar este recurso.");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response,
                          HttpStatus status, String detalhe) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(status.getReasonPhrase());
        problema.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
