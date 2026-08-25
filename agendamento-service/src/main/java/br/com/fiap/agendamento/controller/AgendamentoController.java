package br.com.fiap.agendamento.controller;

import br.com.fiap.agendamento.dto.AtualizarAgendamentoInput;
import br.com.fiap.agendamento.dto.FiltroAgendamentoInput;
import br.com.fiap.agendamento.dto.NovoAgendamentoInput;
import br.com.fiap.agendamento.dto.PaginaAgendamento;
import br.com.fiap.agendamento.entity.Agendamento;
import br.com.fiap.agendamento.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class AgendamentoController {

    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    @QueryMapping
    public Optional<Agendamento> agendamento(@Argument Long id) {
        return service.buscarPorId(id);
    }

    @QueryMapping
    public PaginaAgendamento agendamentos(@Argument FiltroAgendamentoInput filtro,
                                          @Argument int pagina,
                                          @Argument int tamanho) {
        FiltroAgendamentoInput efetivo = filtro != null ? filtro : FiltroAgendamentoInput.vazio();
        return PaginaAgendamento.de(service.buscar(efetivo, pagina, tamanho));
    }

    @MutationMapping
    public Agendamento criarAgendamento(@Argument @Valid NovoAgendamentoInput input) {
        return service.criar(input);
    }

    @MutationMapping
    public Agendamento atualizarAgendamento(@Argument Long id,
                                            @Argument @Valid AtualizarAgendamentoInput input) {
        return service.atualizar(id, input);
    }

    @MutationMapping
    public Agendamento cancelarAgendamento(@Argument Long id) {
        return service.cancelar(id);
    }

    @MutationMapping
    public boolean removerAgendamento(@Argument Long id) {
        return service.remover(id);
    }
}
