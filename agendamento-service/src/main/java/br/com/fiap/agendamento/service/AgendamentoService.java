package br.com.fiap.agendamento.service;

import br.com.fiap.agendamento.dto.request.CriarAgendamentoRequest;
import br.com.fiap.agendamento.dto.response.CriarAgendamentoResponse;
import br.com.fiap.agendamento.entity.Agendamento;
import br.com.fiap.agendamento.repository.AgendamentoRepository;
import br.com.fiap.agendamento.repository.PacienteRepository;
import br.com.fiap.agendamento.repository.PagamentoRepository;
import br.com.fiap.agendamento.repository.ProfissionalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final PagamentoRepository pagamentoRepository;

    @Transactional
    public CriarAgendamentoResponse criarAgendamento(CriarAgendamentoRequest request) {
        validarPaciente(request.pacienteId());
        validarProfissional(request.profissionalId());
        validarPagamento(request.pagamentoId());

        Agendamento agendamento = Agendamento.builder()
                .pacienteId(request.pacienteId())
                .profissionalId(request.profissionalId())
                .pagamentoId(request.pagamentoId())
                .dateTime(request.dateTime())
                .build();

        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);
        log.info("Agendamento criado: {}", agendamentoSalvo.getId());

        return converteParaResponse(agendamentoSalvo);
    }

    public CriarAgendamentoResponse converteParaResponse(Agendamento agendamento) {
        return new CriarAgendamentoResponse(
                agendamento.getId(),
                agendamento.getPacienteId(),
                agendamento.getProfissionalId(),
                agendamento.getPagamentoId(),
                agendamento.getDateTime());
    }

    private void validarPaciente(UUID id) {
        if (!pacienteRepository.existsById(id)) {
            throw new EntityNotFoundException("Paciente nao encontrado: " + id);
        }
    }

    private void validarProfissional(UUID id) {
        if (!profissionalRepository.existsById(id)) {
            throw new EntityNotFoundException("Profissional nao encontrado: " + id);
        }
    }

    private void validarPagamento(UUID id) {
        if (!pagamentoRepository.existsById(id)) {
            throw new EntityNotFoundException("Pagamento nao encontrado: " + id);
        }
    }
}
