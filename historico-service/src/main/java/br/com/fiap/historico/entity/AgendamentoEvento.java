package br.com.fiap.historico.entity;

/**
 * O produtor serializa o evento como enum, ou seja, uma string na mensagem.
 * Precisa ser enum (ou String) aqui: record nao se constroi a partir de string.
 * O mesmo tipo vale para a coluna: o valor que chega e o valor que fica.
 */
public enum AgendamentoEvento {
    AGENDAMENTO_CRIADO,
    AGENDAMENTO_EDITADO,
    AGENDAMENTO_EXCLUIDO
}
