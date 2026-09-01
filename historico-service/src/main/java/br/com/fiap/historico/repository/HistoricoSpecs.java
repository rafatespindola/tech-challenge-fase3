package br.com.fiap.historico.repository;

import br.com.fiap.historico.dto.input.FiltroHistoricoInput;
import br.com.fiap.historico.entity.HistoricoAgendamento;
import br.com.fiap.historico.service.EscopoConsulta;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Monta o where do filtro. Specification em vez de um JPQL com
 * ":param is null or ..." porque o Hibernate nao consegue deduzir o tipo de um
 * parametro enum comparado a null, o que quebraria os filtros por evento e
 * status.
 */
public final class HistoricoSpecs {

    private HistoricoSpecs() {
    }

    public static Specification<HistoricoAgendamento> de(FiltroHistoricoInput filtro,
                                                         EscopoConsulta escopo) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            // O escopo entra como mais um predicado, e nao sobrescrevendo o
            // filtro: assim um PACIENTE que peca ?pacienteId=999 nao recebe o
            // historico alheio nem o proprio - recebe vazio, que e a resposta
            // honesta para "os eventos do paciente 999 que voce pode ver".
            if (escopo.restrito()) {
                predicados.add(cb.equal(root.get("pacienteId"), escopo.pacienteId()));
            }

            if (filtro.agendamentoId() != null) {
                predicados.add(cb.equal(root.get("agendamentoId"), filtro.agendamentoId()));
            }
            if (filtro.pacienteId() != null) {
                predicados.add(cb.equal(root.get("pacienteId"), filtro.pacienteId()));
            }
            if (filtro.profissionalId() != null) {
                predicados.add(cb.equal(root.get("profissionalId"), filtro.profissionalId()));
            }
            if (filtro.evento() != null) {
                predicados.add(cb.equal(root.get("evento"), filtro.evento()));
            }
            if (filtro.status() != null) {
                predicados.add(cb.equal(root.get("status"), filtro.status()));
            }
            if (filtro.de() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("registradoEm"), filtro.de()));
            }
            if (filtro.ate() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("registradoEm"), filtro.ate()));
            }

            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
