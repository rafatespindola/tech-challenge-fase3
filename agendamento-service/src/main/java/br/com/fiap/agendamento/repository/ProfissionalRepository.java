package br.com.fiap.agendamento.repository;

import br.com.fiap.agendamento.entity.Profissional;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfissionalRepository extends JpaRepository<Profissional, UUID> {

    Optional<Profissional> findById(@NonNull UUID id);
}
