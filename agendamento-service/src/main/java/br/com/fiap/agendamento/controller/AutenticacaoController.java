package br.com.fiap.agendamento.controller;

import br.com.fiap.agendamento.dto.output.AutenticacaoResponse;
import br.com.fiap.agendamento.dto.input.LoginRequest;
import br.com.fiap.agendamento.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unico endpoint REST do servico, e a unica rota publica: o resto da API e
 * GraphQL em /graphql e exige o Bearer token emitido aqui.
 */
@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    @PostMapping(path = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AutenticacaoResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return service.autenticar(requisicao);
    }
}
