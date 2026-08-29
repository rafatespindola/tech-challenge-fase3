package br.com.fiap.agendamento.entity;

/**
 * O sufixo compoe a routing key junto com lab.rabbitmq.routing-key-prefix.
 * Evento novo entra aqui e ja sai roteavel, sem tocar no properties.
 */
public enum AgendamentoEvento {
    AGENDAMENTO_CRIADO("criado"),
    AGENDAMENTO_EDITADO("editado"),
    AGENDAMENTO_EXCLUIDO("excluido");

    private final String sufixo;

    AgendamentoEvento(String sufixo) {
        this.sufixo = sufixo;
    }

    public String getSufixo() {
        return sufixo;
    }
}
