package br.com.fiap.historico.controller;

import br.com.fiap.historico.dto.input.EdicaoObservacaoInput;
import br.com.fiap.historico.dto.input.FiltroHistoricoInput;
import br.com.fiap.historico.dto.output.HistoricoOutput;
import br.com.fiap.historico.dto.output.PaginaHistorico;
import br.com.fiap.historico.entity.AgendamentoEvento;
import br.com.fiap.historico.entity.StatusAgendamento;
import br.com.fiap.historico.security.Role;
import br.com.fiap.historico.security.UsuarioAutenticado;
import br.com.fiap.historico.service.EscopoConsulta;
import br.com.fiap.historico.service.HistoricoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * API do historico. As linhas nascem da fila e nenhuma delas pode ser criada ou
 * apagada por HTTP - um historico que aceita escrita livre deixa de ser
 * historico. A unica escrita e o PATCH da observacao, e so para MEDICO.
 *
 * Matriz de permissoes:
 *   PACIENTE   le, restrito ao proprio pacienteId
 *   ENFERMEIRO le tudo
 *   MEDICO     le tudo e edita a observacao
 *
 * O token vem do agendamento-service: este servico nao tem login proprio.
 */
@RestController
@RequestMapping("/historicos")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    /**
     * Listagem paginada. Os filtros sao opcionais e se combinam; {@code de} e
     * {@code ate} recortam pelo instante do registro, em ISO-8601 com offset
     * (2026-09-02T14:30:00-03:00).
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaHistorico listar(
            @RequestParam(required = false) Long agendamentoId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) AgendamentoEvento evento,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        FiltroHistoricoInput filtro = new FiltroHistoricoInput(
                agendamentoId, pacienteId, profissionalId, evento, status, de, ate);

        return PaginaHistorico.de(
                historicoService.buscar(filtro, pagina, tamanho, escopoDe(usuario)));
    }

    /** Um evento especifico, pelo id da linha do historico. */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public HistoricoOutput porId(@PathVariable Long id,
                                 @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return historicoService.buscarPorId(id, escopoDe(usuario));
    }

    /**
     * Linha do tempo completa de um agendamento, do primeiro evento ao ultimo.
     * Continua respondendo depois que o agendamento e excluido na origem.
     */
    @GetMapping("/agendamentos/{agendamentoId}")
    @PreAuthorize("isAuthenticated()")
    public List<HistoricoOutput> porAgendamento(@PathVariable Long agendamentoId,
                                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return historicoService.buscarLinhaDoTempo(agendamentoId, escopoDe(usuario));
    }

    /**
     * Corrige a observacao de uma linha do historico. Exclusivo do MEDICO: o
     * ENFERMEIRO consulta, mas nao altera anotacao clinica.
     *
     * PATCH, e nao PUT, porque o corpo nao substitui o recurso - o resto do
     * retrato nao e alteravel por HTTP nem que se queira.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MEDICO')")
    public HistoricoOutput editarObservacao(@PathVariable Long id,
                                            @RequestBody @Valid EdicaoObservacaoInput input,
                                            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return historicoService.editarObservacao(id, input, autorDe(usuario), escopoDe(usuario));
    }

    /**
     * Decide pela role, nunca por "tem pacienteId": no dia em que um
     * profissional tambem tiver cadastro de paciente, a segunda leitura
     * esconderia dele o historico dos outros.
     */
    private EscopoConsulta escopoDe(UsuarioAutenticado usuario) {
        UsuarioAutenticado autenticado = exigir(usuario);

        if (autenticado.role() != Role.PACIENTE) {
            return EscopoConsulta.irrestrito();
        }
        if (autenticado.pacienteId() == null) {
            // O emissor so gera token de PACIENTE com vinculo; a guarda existe
            // para nao virar consulta irrestrita se aquilo mudar.
            throw new AccessDeniedException("usuario PACIENTE sem paciente vinculado");
        }
        return EscopoConsulta.doPaciente(autenticado.pacienteId());
    }

    /** O login vai para a coluna de autoria: e o identificador estavel do token. */
    private String autorDe(UsuarioAutenticado usuario) {
        return exigir(usuario).login();
    }

    private UsuarioAutenticado exigir(UsuarioAutenticado usuario) {
        if (usuario == null) {
            // O @PreAuthorize acima ja barrou o anonimo; se chegou aqui nulo e
            // porque o principal nao e um UsuarioAutenticado. Falha fechada.
            throw new AccessDeniedException("principal ausente ou de tipo inesperado");
        }
        return usuario;
    }
}
