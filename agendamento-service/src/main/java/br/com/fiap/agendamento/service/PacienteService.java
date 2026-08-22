package br.com.fiap.agendamento.service;

import br.com.fiap.agendamento.dto.request.CriarPacienteRequest;
import br.com.fiap.agendamento.dto.request.CriarPagamentoRequest;
import br.com.fiap.agendamento.dto.response.CriarPacienteResponse;
import br.com.fiap.agendamento.entity.Paciente;
import br.com.fiap.agendamento.entity.Pagamento;
import br.com.fiap.agendamento.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    public CriarPacienteResponse criarPaciente(CriarPacienteRequest request) {
        Paciente paciente = Paciente.builder()
                .nome(request.nome())
                .telefone(request.telefone())
                .email(request.email())
                .cpf(request.cpf())
                .dataNascimento(request.dataNascimento())
                .build();

        Paciente pacienteSalvo = pacienteRepository.save(paciente);

        return converteParaResponse(pacienteSalvo);
    }

    public CriarPacienteResponse converteParaResponse(Paciente paciente) {
        return new CriarPacienteResponse(paciente.getId(), paciente.getNome(), paciente.getTelefone(), paciente.getEmail(), paciente.getCpf(), paciente.getDataNascimento());
    }

}
