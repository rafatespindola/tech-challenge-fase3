package br.com.fiap.agendamento.dto.input;

import br.com.fiap.agendamento.entity.StatusAgendamento;

import java.time.OffsetDateTime;

public record FiltroAgendamentoInput(
        Long pacienteId,
        Long profissionalId,
        StatusAgendamento status,
        OffsetDateTime de,
        OffsetDateTime ate
) {
    /** Filtro vazio, usado quando o cliente omite o argumento. */
    public static FiltroAgendamentoInput vazio() {
        return new FiltroAgendamentoInput(null, null, null, null, null);
    }

    /**
     * Copia com o paciente trocado. Sobrescreve o que o cliente mandou, e nao
     * mescla: e assim que um PACIENTE pedindo a agenda de outro recebe a propria.
     */
    public FiltroAgendamentoInput comPacienteId(Long pacienteId) {
        return new FiltroAgendamentoInput(pacienteId, profissionalId, status, de, ate);
    }
}
