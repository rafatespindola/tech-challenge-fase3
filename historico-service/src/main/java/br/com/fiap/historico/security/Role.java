package br.com.fiap.historico.security;

/**
 * Copia do enum do agendamento-service. Nao ha como compartilhar a classe sem
 * criar um modulo comum entre os servicos, e um modulo comum acoplaria os dois
 * deploys - entao o contrato compartilhado e a string dentro da claim "role",
 * nao o tipo Java.
 *
 * Ordem e nomes precisam bater com os de la: um nome novo no emissor sem o
 * mesmo nome aqui derruba o token no Role.valueOf, e o usuario perde o acesso
 * ao historico sem nenhuma mudanca neste servico.
 */
public enum Role {
    PACIENTE, MEDICO, ENFERMEIRO;

    /** Nome da authority esperado pelo Spring Security (prefixo ROLE_). */
    public String authority() {
        return "ROLE_" + name();
    }
}
