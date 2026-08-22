package br.com.fiap.agendamento.controller;

import br.com.fiap.agendamento.dto.request.CriarPagamentoRequest;
import br.com.fiap.agendamento.dto.response.CriarPagamentoResponse;
import br.com.fiap.agendamento.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    public ResponseEntity<CriarPagamentoResponse> criarPagamento(
            @Valid @RequestBody CriarPagamentoRequest request) {
        CriarPagamentoResponse response = pagamentoService.criarPagamento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
