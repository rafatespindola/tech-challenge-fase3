package br.com.fiap.historico.service;

import java.util.Objects;

/**
 * Recorte de dados que a consulta pode enxergar.
 *
 * Existe para o escopo viajar explicito na assinatura do service, em vez de ser
 * lido de um SecurityContext la dentro: quem chama e obrigado pelo compilador a
 * declarar de quem e a visao, e o service continua testavel sem seguranca.
 */
public record EscopoConsulta(Long pacienteId) {

    /** Visao completa: MEDICO e ENFERMEIRO leem o historico de todos. */
    public static EscopoConsulta irrestrito() {
        return new EscopoConsulta(null);
    }

    public static EscopoConsulta doPaciente(Long pacienteId) {
        return new EscopoConsulta(Objects.requireNonNull(pacienteId));
    }

    public boolean restrito() {
        return pacienteId != null;
    }

    /** True quando a linha esta fora do que este escopo pode ver. */
    public boolean naoAlcanca(Long pacienteIdDaLinha) {
        return restrito() && !pacienteId.equals(pacienteIdDaLinha);
    }
}
