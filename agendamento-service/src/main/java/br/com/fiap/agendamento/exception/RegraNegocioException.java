package br.com.fiap.agendamento.exception;

/** Requisicao bem formada, mas que viola uma regra do dominio. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
