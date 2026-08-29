package br.com.fiap.agendamento.repository;

import br.com.fiap.agendamento.dto.input.FiltroAgendamentoInput;
import br.com.fiap.agendamento.entity.Agendamento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Monta o where do filtro. Specification em vez de um JPQL com
 * ":param is null or ..." porque o Hibernate nao consegue deduzir o tipo de um
 * parametro enum comparado a null, o que quebraria o filtro por status.
 */
public final class AgendamentoSpecs {

    private AgendamentoSpecs() {
    }

    public static Specification<Agendamento> de(FiltroAgendamentoInput filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (filtro.pacienteId() != null) {
                predicados.add(cb.equal(root.get("paciente").get("id"), filtro.pacienteId()));
            }
            if (filtro.profissionalId() != null) {
                predicados.add(cb.equal(root.get("profissional").get("id"), filtro.profissionalId()));
            }
            if (filtro.status() != null) {
                predicados.add(cb.equal(root.get("status"), filtro.status()));
            }
            if (filtro.de() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("dataHora"), filtro.de()));
            }
            if (filtro.ate() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("dataHora"), filtro.ate()));
            }

            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
