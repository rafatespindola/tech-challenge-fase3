package br.com.fiap.agendamento.entity;

public enum Role {
    PACIENTE, MEDICO, ENFERMEIRO;

    /** Nome da authority esperado pelo Spring Security (prefixo ROLE_). */
    public String authority() {
        return "ROLE_" + name();
    }

    public boolean ehProfissional() {
        return this == MEDICO || this == ENFERMEIRO;
    }
}