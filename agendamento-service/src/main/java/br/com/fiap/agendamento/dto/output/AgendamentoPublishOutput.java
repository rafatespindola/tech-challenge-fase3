package br.com.fiap.agendamento.dto.output;

import br.com.fiap.agendamento.entity.Agendamento;
import br.com.fiap.agendamento.entity.AgendamentoEvento;

public record AgendamentoPublishOutput(
        AgendamentoEvento agendamentoEvento,
        Agendamento agendamento
) { }
