package br.com.fiap.notificacao.dto.input;

import java.time.OffsetDateTime;

/**
 * Contrato de leitura, nao espelho do modelo do produtor: so os campos que uma
 * notificacao usa. O converter do Spring AMQP desliga FAIL_ON_UNKNOWN_PROPERTIES,
 * entao o resto do JSON (versao, convenio, cpf, auditoria) e descartado sozinho -
 * e campo novo no produtor nao quebra este consumidor.
 */
public record Agendamento(
        Long id,
        OffsetDateTime dataHora,
        StatusAgendamento status,
        Paciente paciente,
        Profissional profissional,
        Procedimento procedimento
) {

    // Sem cpf nem dataNascimento: nao servem para notificar e manter fora do DTO
    // evita que caiam no log deste servico.
    public record Paciente(Long id, String nome, String telefone, String email) { }

    public record Profissional(Long id, String nome, String especialidade) { }

    public record Procedimento(Long id, String nome) { }

}
