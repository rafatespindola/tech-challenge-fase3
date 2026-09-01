package br.com.fiap.historico.dto.input;

import br.com.fiap.historico.entity.AgendamentoEvento;

public record MensagemFila(
        AgendamentoEvento agendamentoEvento,
        Agendamento agendamento
) { }
