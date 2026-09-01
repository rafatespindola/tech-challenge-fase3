package br.com.fiap.historico.dto.input;

import br.com.fiap.historico.entity.StatusAgendamento;

import java.time.OffsetDateTime;

/**
 * Contrato de leitura, nao espelho do modelo do produtor. Aqui ele e mais largo
 * que o do notificacao-service: o historico guarda o retrato do agendamento no
 * momento do evento, entao convenio e observacao tambem entram. O converter do
 * Spring AMQP desliga FAIL_ON_UNKNOWN_PROPERTIES, entao o resto do JSON (versao,
 * auditoria, cpf) e descartado sozinho - e campo novo no produtor nao quebra
 * este consumidor.
 */
public record Agendamento(
        Long id,
        OffsetDateTime dataHora,
        StatusAgendamento status,
        String observacao,
        Paciente paciente,
        Profissional profissional,
        Procedimento procedimento,
        Convenio convenio
) {

    // Sem cpf nem dataNascimento: identificar o paciente no historico exige so
    // id e nome, e manter o resto fora do DTO evita replicar o dado sensivel
    // numa segunda base.
    public record Paciente(Long id, String nome, String telefone, String email) { }

    public record Profissional(Long id, String nome, String especialidade) { }

    public record Procedimento(Long id, String nome) { }

    public record Convenio(Long id, String nome) { }

}
