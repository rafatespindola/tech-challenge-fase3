package br.com.fiap.agendamento.service;

import java.util.Objects;

/**
 * Recorte de dados que a consulta pode enxergar.
 *
 * Existe para o escopo viajar explicito na assinatura do service, em vez de ser
 * lido de um SecurityContext la dentro: quem chama e obrigado pelo compilador a
 * declarar de quem e a visao, e o service continua testavel sem seguranca.
 */
public record EscopoConsulta(Long pacienteId) {

    /** Visao completa: profissionais veem os agendamentos de todos os pacientes. */
    public static EscopoConsulta irrestrito() {
        return new EscopoConsulta(null);
    }

    public static EscopoConsulta doPaciente(Long pacienteId) {
        return new EscopoConsulta(Objects.requireNonNull(pacienteId));
    }

    public boolean restrito() {
        return pacienteId != null;
    }
}
