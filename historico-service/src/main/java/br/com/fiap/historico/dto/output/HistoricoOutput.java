package br.com.fiap.historico.dto.output;

import br.com.fiap.historico.entity.AgendamentoEvento;
import br.com.fiap.historico.entity.HistoricoAgendamento;
import br.com.fiap.historico.entity.StatusAgendamento;

import java.time.OffsetDateTime;

/**
 * Remonta o retrato aninhado a partir das colunas planas da tabela. A entidade e
 * plana porque isso simplifica indices e filtros; a resposta e aninhada porque e
 * o formato que o cliente ja conhece do agendamento-service.
 *
 * Os dois campos de edicao vem nulos na esmagadora maioria das linhas, e e essa
 * a informacao util: linha sem eles e o retrato original, intocado.
 */
public record HistoricoOutput(
        Long id,
        AgendamentoEvento evento,
        OffsetDateTime registradoEm,
        Long agendamentoId,
        OffsetDateTime dataHora,
        StatusAgendamento status,
        String observacao,
        String observacaoEditadaPor,
        OffsetDateTime observacaoEditadaEm,
        Paciente paciente,
        Profissional profissional,
        Procedimento procedimento,
        Convenio convenio
) {

    public record Paciente(Long id, String nome, String telefone, String email) { }

    public record Profissional(Long id, String nome, String especialidade) { }

    public record Procedimento(Long id, String nome) { }

    public record Convenio(Long id, String nome) { }

    public static HistoricoOutput de(HistoricoAgendamento historico) {
        return new HistoricoOutput(
                historico.getId(),
                historico.getEvento(),
                historico.getRegistradoEm(),
                historico.getAgendamentoId(),
                historico.getDataHora(),
                historico.getStatus(),
                historico.getObservacao(),
                historico.getObservacaoEditadaPor(),
                historico.getObservacaoEditadaEm(),
                new Paciente(historico.getPacienteId(), historico.getPacienteNome(),
                        historico.getPacienteTelefone(), historico.getPacienteEmail()),
                new Profissional(historico.getProfissionalId(), historico.getProfissionalNome(),
                        historico.getProfissionalEspecialidade()),
                new Procedimento(historico.getProcedimentoId(), historico.getProcedimentoNome()),
                historico.getConvenioId() == null
                        ? null
                        : new Convenio(historico.getConvenioId(), historico.getConvenioNome())
        );
    }
}
