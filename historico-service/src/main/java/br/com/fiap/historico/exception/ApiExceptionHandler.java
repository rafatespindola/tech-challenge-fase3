package br.com.fiap.historico.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail recursoNaoEncontrado(RecursoNaoEncontradoException excecao,
                                              HttpServletRequest requisicao) {
        return problema(HttpStatus.NOT_FOUND, excecao.getMessage(), requisicao);
    }

    /**
     * Enum ou data invalida no query string. Sem este handler o Spring devolveria
     * 500 para um ?evento=QUALQUER_COISA, escondendo que o erro e do cliente.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException excecao,
                                           HttpServletRequest requisicao) {
        String detalhe = "Valor invalido para o parametro '%s'.".formatted(excecao.getName());
        return problema(HttpStatus.BAD_REQUEST, detalhe, requisicao);
    }

    /**
     * Corpo do PATCH fora do contrato - hoje, observacao acima de 500
     * caracteres. Sem este handler a resposta sairia no formato padrao do Boot,
     * diferente do ProblemDetail dos outros erros desta API.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail corpoInvalido(MethodArgumentNotValidException excecao,
                                       HttpServletRequest requisicao) {
        String detalhe = excecao.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> "%s: %s".formatted(erro.getField(), erro.getDefaultMessage()))
                .orElse("Corpo da requisicao invalido.");

        return problema(HttpStatus.BAD_REQUEST, detalhe, requisicao);
    }

    private ProblemDetail problema(HttpStatus status, String detalhe, HttpServletRequest requisicao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(status.getReasonPhrase());
        problema.setInstance(URI.create(requisicao.getRequestURI()));
        return problema;
    }
}
