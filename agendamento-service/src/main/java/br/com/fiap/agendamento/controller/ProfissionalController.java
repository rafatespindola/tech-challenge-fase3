package br.com.fiap.agendamento.controller;

import br.com.fiap.agendamento.dto.request.CriarProfissionalRequest;
import br.com.fiap.agendamento.dto.response.CriarProfissionalResponse;
import br.com.fiap.agendamento.service.ProfissionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profissionais")
@RequiredArgsConstructor
public class ProfissionalController {

    private final ProfissionalService profissionalService;

    @PostMapping
    public ResponseEntity<CriarProfissionalResponse> criarProfissional(
            @Valid @RequestBody CriarProfissionalRequest request) {
        CriarProfissionalResponse response = profissionalService.criarProfissional(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
