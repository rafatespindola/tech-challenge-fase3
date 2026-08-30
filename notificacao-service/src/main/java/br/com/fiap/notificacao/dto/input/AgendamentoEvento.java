package br.com.fiap.notificacao.dto.input;

/**
 * O produtor serializa o evento como enum, ou seja, uma string na mensagem.
 * Precisa ser enum (ou String) aqui: record nao se constroi a partir de string.
 */
public enum AgendamentoEvento {
    AGENDAMENTO_CRIADO,
    AGENDAMENTO_EDITADO,
    AGENDAMENTO_EXCLUIDO
}
