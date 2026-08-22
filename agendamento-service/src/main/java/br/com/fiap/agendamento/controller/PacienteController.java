package br.com.fiap.agendamento.controller;

import br.com.fiap.agendamento.dto.request.CriarPacienteRequest;
import br.com.fiap.agendamento.dto.response.CriarPacienteResponse;
import br.com.fiap.agendamento.service.PacienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @PostMapping
    public ResponseEntity<CriarPacienteResponse> criarPaciente(
            @Valid @RequestBody CriarPacienteRequest request) {
        CriarPacienteResponse response = pacienteService.criarPaciente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
