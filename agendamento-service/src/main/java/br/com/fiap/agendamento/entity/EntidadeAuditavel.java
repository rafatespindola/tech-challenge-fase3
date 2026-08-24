package br.com.fiap.agendamento.entity;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.OffsetDateTime;

/**
* @MappedSuperclass não vira tabela: as colunas descem para cada entidade
* que herda. Evita repetir criado_em/atualizado_em em quatro lugares.
*/
@MappedSuperclass
public abstract class EntidadeAuditavel {

        @Column(name = "criado_em", nullable = false, updatable = false)
        private OffsetDateTime criadoEm;

        @Column(name = "atualizado_em", nullable = false)
        private OffsetDateTime atualizadoEm;

        @PrePersist
        void aoCriar() {
            this.criadoEm = OffsetDateTime.now();
            this.atualizadoEm = this.criadoEm;
        }

        @PreUpdate
        void aoAtualizar() {
            this.atualizadoEm = OffsetDateTime.now();
        }

        public OffsetDateTime getCriadoEm() {
            return criadoEm;
        }

        public OffsetDateTime getAtualizadoEm() {
            return atualizadoEm;
        }
}
