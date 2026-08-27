package br.com.fiap.agendamento.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

/**
 * Erros da parte REST da API. Nao interfere no GraphQL: excecao de data fetcher
 * nunca chega ao HandlerExceptionResolver - o graphql-java a captura antes e
 * manda para o {@link GraphQlExceptionResolver}, e a resposta HTTP e sempre 200.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Credencial invalida no /auth/login cai aqui, e nao no
     * AuthenticationEntryPoint: o advice roda dentro do DispatcherServlet, mais
     * fundo que o ExceptionTranslationFilter, e a resposta nao propaga de volta
     * para a cadeia de filtros.
     *
     * A mensagem e a mesma para senha errada e login inexistente: distinguir os
     * dois entrega a um atacante quais logins existem.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail credencialInvalida(AuthenticationException excecao,
                                            HttpServletRequest requisicao) {
        return problema(HttpStatus.UNAUTHORIZED, "Login ou senha invalidos.", requisicao);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail acessoNegado(AccessDeniedException excecao,
                                       HttpServletRequest requisicao) {
        return problema(HttpStatus.FORBIDDEN, "Sem permissao para acessar este recurso.", requisicao);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail corpoInvalido(MethodArgumentNotValidException excecao,
                                        HttpServletRequest requisicao) {
        List<CampoInvalido> campos = excecao.getBindingResult().getFieldErrors().stream()
                .map(CampoInvalido::de)
                .sorted(Comparator.comparing(CampoInvalido::campo))
                .toList();

        ProblemDetail problema = problema(
                HttpStatus.BAD_REQUEST, "Corpo da requisicao invalido.", requisicao);
        problema.setProperty("campos", campos);
        return problema;
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraDeNegocio(RegraNegocioException excecao,
                                         HttpServletRequest requisicao) {
        return problema(HttpStatus.BAD_REQUEST, excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail recursoNaoEncontrado(RecursoNaoEncontradoException excecao,
                                               HttpServletRequest requisicao) {
        return problema(HttpStatus.NOT_FOUND, excecao.getMessage(), requisicao);
    }

    private ProblemDetail problema(HttpStatus status, String detalhe, HttpServletRequest requisicao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(status.getReasonPhrase());
        problema.setInstance(URI.create(requisicao.getRequestURI()));
        return problema;
    }

    /** Um erro de bean validation, nomeando o campo que o cliente precisa corrigir. */
    public record CampoInvalido(String campo, String mensagem) {

        static CampoInvalido de(FieldError erro) {
            return new CampoInvalido(erro.getField(), erro.getDefaultMessage());
        }
    }
}
