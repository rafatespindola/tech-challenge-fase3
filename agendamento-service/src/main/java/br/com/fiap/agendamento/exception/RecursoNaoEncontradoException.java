package br.com.fiap.agendamento.exception;

/** Id informado na requisicao nao corresponde a nenhuma linha. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Long id) {
        super("%s de id %d nao encontrado".formatted(recurso, id));
    }
}
