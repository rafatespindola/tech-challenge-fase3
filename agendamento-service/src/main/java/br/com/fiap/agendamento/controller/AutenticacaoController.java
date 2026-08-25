package br.com.fiap.agendamento.controller;

import br.com.fiap.agendamento.dto.AutenticacaoPayload;
import br.com.fiap.agendamento.dto.LoginInput;
import br.com.fiap.agendamento.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class AutenticacaoController {

    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    @MutationMapping
    public AutenticacaoPayload login(@Argument @Valid LoginInput input) {
        return service.autenticar(input);
    }
}
