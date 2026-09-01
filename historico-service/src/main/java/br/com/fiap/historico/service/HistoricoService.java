package br.com.fiap.historico.service;

import br.com.fiap.historico.dto.input.EdicaoObservacaoInput;
import br.com.fiap.historico.dto.input.FiltroHistoricoInput;
import br.com.fiap.historico.dto.input.MensagemFila;
import br.com.fiap.historico.dto.output.HistoricoOutput;
import br.com.fiap.historico.entity.HistoricoAgendamento;
import br.com.fiap.historico.exception.RecursoNaoEncontradoException;
import br.com.fiap.historico.repository.HistoricoRepository;
import br.com.fiap.historico.repository.HistoricoSpecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistoricoService {

    private static final Logger log = LoggerFactory.getLogger(HistoricoService.class);

    /** Teto de itens por pagina, para o cliente nao pedir a tabela inteira. */
    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final HistoricoRepository historicoRepository;

    public HistoricoService(HistoricoRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    /**
     * Grava o evento. Criacao, edicao e exclusao seguem o mesmo caminho: o
     * historico nao interpreta o evento, so o registra - inclusive o
     * AGENDAMENTO_EXCLUIDO, que e justamente o unico retrato que sobra do
     * agendamento depois que ele some da base de origem.
     */
    @Transactional
    public void registrar(MensagemFila mensagem, String messageId) {
        if (messageId != null && historicoRepository.existsByMessageId(messageId)) {
            log.info("Mensagem {} ja registrada, ignorando reentrega", messageId);
            return;
        }

        HistoricoAgendamento historico = new HistoricoAgendamento(
                messageId, mensagem.agendamentoEvento(), mensagem.agendamento());

        try {
            historicoRepository.save(historico);
        } catch (DataIntegrityViolationException excecao) {
            // Duas entregas concorrentes passam juntas pelo exists acima; quem
            // perde a corrida bate no unique de message_id. Nada a fazer: a
            // linha ja existe, que e o resultado desejado. Relancar so faria a
            // mensagem voltar para a fila e repetir o mesmo choque.
            log.warn("Mensagem {} gravada em paralelo, ignorando duplicata", messageId);
            return;
        }

        log.info("Evento {} do agendamento {} registrado no historico (mensagem {})",
                mensagem.agendamentoEvento(), mensagem.agendamento().id(), messageId);
    }

    /**
     * Corrige a observacao de uma linha. So a observacao muda; o resto do
     * retrato e o que a fila entregou e continua sendo.
     *
     * O escopo tambem vale aqui, ainda que hoje so um MEDICO chegue a este
     * metodo: se um dia a role de escrita mudar, a regra de visibilidade nao
     * fica para tras.
     */
    @Transactional
    public HistoricoOutput editarObservacao(Long id, EdicaoObservacaoInput input,
                                            String editadoPor, EscopoConsulta escopo) {
        HistoricoAgendamento historico = historicoRepository.findById(id)
                .filter(linha -> !escopo.naoAlcanca(linha.getPacienteId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Historico", id));

        historico.editarObservacao(input.observacao(), editadoPor);

        log.info("Observacao do historico {} editada por {}", id, editadoPor);

        // Dentro da transacao o dirty checking grava no commit; nao ha save().
        return HistoricoOutput.de(historico);
    }

    @Transactional(readOnly = true)
    public Page<HistoricoOutput> buscar(FiltroHistoricoInput filtro, int pagina, int tamanho,
                                        EscopoConsulta escopo) {
        int tamanhoEfetivo = Math.clamp(tamanho, 1, TAMANHO_MAXIMO_PAGINA);
        // Mais recente primeiro: quem abre o historico quer o ultimo evento.
        PageRequest paginacao = PageRequest.of(
                Math.max(pagina, 0), tamanhoEfetivo, Sort.by("registradoEm").descending());

        return historicoRepository.findAll(HistoricoSpecs.de(filtro, escopo), paginacao)
                .map(HistoricoOutput::de);
    }

    /**
     * Linha fora do escopo responde 404, e nao 403, de proposito: um 403 aqui
     * confirmaria para o paciente que aquele id existe e e de outra pessoa.
     */
    @Transactional(readOnly = true)
    public HistoricoOutput buscarPorId(Long id, EscopoConsulta escopo) {
        return historicoRepository.findById(id)
                .filter(linha -> !escopo.naoAlcanca(linha.getPacienteId()))
                .map(HistoricoOutput::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Historico", id));
    }

    /**
     * Linha do tempo de um agendamento, do evento mais antigo ao mais novo -
     * ordem inversa da listagem geral, porque aqui a leitura e narrativa.
     * Lista vazia quando o agendamento nunca gerou evento: ausencia de historico
     * nao e erro.
     */
    @Transactional(readOnly = true)
    public List<HistoricoOutput> buscarLinhaDoTempo(Long agendamentoId, EscopoConsulta escopo) {
        FiltroHistoricoInput filtro = new FiltroHistoricoInput(
                agendamentoId, null, null, null, null, null, null);

        return historicoRepository.findAll(
                        HistoricoSpecs.de(filtro, escopo), Sort.by("registradoEm").ascending())
                .stream()
                .map(HistoricoOutput::de)
                .toList();
    }
}
