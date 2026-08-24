package br.com.fiap.agendamento.repository;

import br.com.fiap.agendamento.entity.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> { }