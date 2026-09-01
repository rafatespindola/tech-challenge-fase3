package br.com.fiap.historico.dto.input;

import br.com.fiap.historico.entity.AgendamentoEvento;
import br.com.fiap.historico.entity.StatusAgendamento;

import java.time.OffsetDateTime;

/**
 * Filtros da consulta REST. Todos opcionais e combinaveis; nulo significa "nao
 * restringe". {@code de} e {@code ate} recortam por registradoEm - a linha do
 * tempo do historico - e nao pela data da consulta agendada.
 */
public record FiltroHistoricoInput(
        Long agendamentoId,
        Long pacienteId,
        Long profissionalId,
        AgendamentoEvento evento,
        StatusAgendamento status,
        OffsetDateTime de,
        OffsetDateTime ate
) { }
