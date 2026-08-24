package br.com.fiap.agendamento.repository;

import br.com.fiap.agendamento.entity.Convenio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConvenioRepository extends JpaRepository<Convenio, Long> { }

