package br.com.fiap.historico.repository;

import br.com.fiap.historico.entity.HistoricoAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HistoricoRepository
        extends JpaRepository<HistoricoAgendamento, Long>, JpaSpecificationExecutor<HistoricoAgendamento> {

    /**
     * Guarda de idempotencia: a mesma mensagem reentregue nao vira uma segunda
     * linha. E so a primeira barreira - a definitiva e o unique de message_id,
     * que segura tambem duas entregas concorrentes.
     */
    boolean existsByMessageId(String messageId);
}
