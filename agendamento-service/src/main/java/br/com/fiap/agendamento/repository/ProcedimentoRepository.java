package br.com.fiap.agendamento.repository;

import br.com.fiap.agendamento.entity.Procedimento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, Long> { }
