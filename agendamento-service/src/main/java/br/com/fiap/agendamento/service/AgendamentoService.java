package br.com.fiap.agendamento.service;

import br.com.fiap.agendamento.dto.AtualizarAgendamentoInput;
import br.com.fiap.agendamento.dto.FiltroAgendamentoInput;
import br.com.fiap.agendamento.dto.NovoAgendamentoInput;
import br.com.fiap.agendamento.entity.Agendamento;
import br.com.fiap.agendamento.entity.Convenio;
import br.com.fiap.agendamento.entity.Paciente;
import br.com.fiap.agendamento.entity.Procedimento;
import br.com.fiap.agendamento.entity.Profissional;
import br.com.fiap.agendamento.entity.StatusAgendamento;
import br.com.fiap.agendamento.exception.RecursoNaoEncontradoException;
import br.com.fiap.agendamento.exception.RegraNegocioException;
import br.com.fiap.agendamento.repository.AgendamentoRepository;
import br.com.fiap.agendamento.repository.AgendamentoSpecs;
import br.com.fiap.agendamento.repository.ConvenioRepository;
import br.com.fiap.agendamento.repository.PacienteRepository;
import br.com.fiap.agendamento.repository.ProcedimentoRepository;
import br.com.fiap.agendamento.repository.ProfissionalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AgendamentoService {

    /** Teto de itens por pagina, para o cliente nao pedir a tabela inteira. */
    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final AgendamentoRepository agendamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final ConvenioRepository convenioRepository;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              PacienteRepository pacienteRepository,
                              ProfissionalRepository profissionalRepository,
                              ProcedimentoRepository procedimentoRepository,
                              ConvenioRepository convenioRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.profissionalRepository = profissionalRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.convenioRepository = convenioRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Agendamento> buscarPorId(Long id) {
        return agendamentoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Agendamento> buscar(FiltroAgendamentoInput filtro, int pagina, int tamanho) {
        int tamanhoEfetivo = Math.clamp(tamanho, 1, TAMANHO_MAXIMO_PAGINA);
        PageRequest paginacao = PageRequest.of(
                Math.max(pagina, 0), tamanhoEfetivo, Sort.by("dataHora").ascending());
        return agendamentoRepository.findAll(AgendamentoSpecs.de(filtro), paginacao);
    }

    @Transactional
    public Agendamento criar(NovoAgendamentoInput input) {
        Paciente paciente = pacienteRepository.findById(input.pacienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente", input.pacienteId()));
        Profissional profissional = profissionalRepository.findById(input.profissionalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", input.profissionalId()));
        Procedimento procedimento = procedimentoRepository.findById(input.procedimentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Procedimento", input.procedimentoId()));
        Convenio convenio = convenioRepository.findById(input.convenioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convenio", input.convenioId()));

        validarDisponibilidade(paciente.getId(), profissional.getId(), input.dataHora(), null);

        Agendamento agendamento = new Agendamento(
                paciente, profissional, procedimento, convenio, input.dataHora(), input.observacao());
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento atualizar(Long id, AtualizarAgendamentoInput input) {
        Agendamento agendamento = carregar(id);

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new RegraNegocioException(
                    "agendamento %s nao pode ser alterado".formatted(agendamento.getStatus()));
        }

        if (input.dataHora() != null && !input.dataHora().isEqual(agendamento.getDataHora())) {
            validarDisponibilidade(agendamento.getPaciente().getId(),
                    agendamento.getProfissional().getId(), input.dataHora(), id);
        }

        agendamento.alterar(input.dataHora(), input.status(), input.observacao());
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento cancelar(Long id) {
        Agendamento agendamento = carregar(id);

        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new RegraNegocioException("agendamento ja realizado nao pode ser cancelado");
        }

        agendamento.cancelar();
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public boolean remover(Long id) {
        if (!agendamentoRepository.existsById(id)) {
            return false;
        }
        agendamentoRepository.deleteById(id);
        return true;
    }

    private Agendamento carregar(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
    }

    /** {@code idIgnorado} nulo ao criar; ao reagendar, o id do proprio registro. */
    private void validarDisponibilidade(Long pacienteId, Long profissionalId,
                                        OffsetDateTime dataHora, Long idIgnorado) {
        if (agendamentoRepository.existeConflitoDeProfissional(profissionalId, dataHora, idIgnorado)) {
            throw new RegraNegocioException("o profissional ja tem um agendamento nesse horario");
        }
        if (agendamentoRepository.existeConflitoDePaciente(pacienteId, dataHora, idIgnorado)) {
            throw new RegraNegocioException("o paciente ja tem um agendamento nesse horario");
        }
    }
}
