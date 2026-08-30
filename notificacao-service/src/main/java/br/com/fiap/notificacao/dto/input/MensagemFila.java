package br.com.fiap.notificacao.dto.input;

public record MensagemFila(
        AgendamentoEvento agendamentoEvento,
        Agendamento agendamento
) { }
