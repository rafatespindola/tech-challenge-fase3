package br.com.fiap.agendamento.repository;

import br.com.fiap.agendamento.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> { }

