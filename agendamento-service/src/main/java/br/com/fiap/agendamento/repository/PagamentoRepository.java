package br.com.fiap.agendamento.repository;

import br.com.fiap.agendamento.entity.Pagamento;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
    Optional<Pagamento> findById(@NonNull UUID id);
}
