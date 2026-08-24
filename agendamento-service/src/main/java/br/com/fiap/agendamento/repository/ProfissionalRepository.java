package br.com.fiap.agendamento.repository;

import br.com.fiap.agendamento.entity.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> { }

