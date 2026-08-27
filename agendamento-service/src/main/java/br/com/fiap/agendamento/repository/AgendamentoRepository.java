package br.com.fiap.agendamento.repository;

import br.com.fiap.agendamento.entity.Agendamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface AgendamentoRepository
        extends JpaRepository<Agendamento, Long>, JpaSpecificationExecutor<Agendamento> {

    /**
     * As associacoes sao EAGER: sem o entity graph o Hibernate dispara um select
     * por associacao por linha da pagina (N+1). O graph traz tudo em um join.
     */
    @Override
    @EntityGraph(attributePaths = {"paciente", "profissional", "procedimento", "convenio"})
    Page<Agendamento> findAll(Specification<Agendamento> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"paciente", "profissional", "procedimento", "convenio"})
    Optional<Agendamento> findById(Long id);

    /**
     * Um profissional nao pode ter dois agendamentos ativos no mesmo horario.
     * {@code idIgnorado} exclui o proprio registro da checagem ao reagendar.
     */
    @Query("""
            select count(a) > 0 from Agendamento a
            where a.profissional.id = :profissionalId
              and a.dataHora = :dataHora
              and a.status <> br.com.fiap.agendamento.entity.StatusAgendamento.CANCELADO
              and (:idIgnorado is null or a.id <> :idIgnorado)
            """)
    boolean existeConflitoDeProfissional(@Param("profissionalId") Long profissionalId,
                                         @Param("dataHora") OffsetDateTime dataHora,
                                         @Param("idIgnorado") Long idIgnorado);

    /** Mesma regra pelo lado do paciente: ele nao pode estar em dois lugares. */
    @Query("""
            select count(a) > 0 from Agendamento a
            where a.paciente.id = :pacienteId
              and a.dataHora = :dataHora
              and a.status <> br.com.fiap.agendamento.entity.StatusAgendamento.CANCELADO
              and (:idIgnorado is null or a.id <> :idIgnorado)
            """)
    boolean existeConflitoDePaciente(@Param("pacienteId") Long pacienteId,
                                     @Param("dataHora") OffsetDateTime dataHora,
                                     @Param("idIgnorado") Long idIgnorado);
}
