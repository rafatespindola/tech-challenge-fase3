package br.com.fiap.historico.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super("%s %s nao encontrado".formatted(recurso, id));
    }
}
