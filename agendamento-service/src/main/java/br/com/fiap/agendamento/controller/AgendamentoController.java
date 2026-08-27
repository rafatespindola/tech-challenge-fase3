package br.com.fiap.agendamento.controller;

import br.com.fiap.agendamento.dto.AtualizarAgendamentoInput;
import br.com.fiap.agendamento.dto.FiltroAgendamentoInput;
import br.com.fiap.agendamento.dto.NovoAgendamentoInput;
import br.com.fiap.agendamento.dto.PaginaAgendamento;
import br.com.fiap.agendamento.entity.Agendamento;
import br.com.fiap.agendamento.entity.Role;
import br.com.fiap.agendamento.security.UsuarioAutenticado;
import br.com.fiap.agendamento.service.AgendamentoService;
import br.com.fiap.agendamento.service.EscopoConsulta;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.Optional;

/**
 * Toda operacao aqui chega no mesmo POST /graphql, entao a regra HTTP so
 * consegue exigir "tem token valido". A distincao entre consultar e alterar
 * vive nos @PreAuthorize abaixo, um por metodo para a matriz de permissoes
 * ficar legivel de relance.
 */
@Controller
public class AgendamentoController {

    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Optional<Agendamento> agendamento(@Argument Long id,
                                             @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.buscarPorId(id, escopoDe(usuario));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public PaginaAgendamento agendamentos(@Argument FiltroAgendamentoInput filtro,
                                          @Argument int pagina,
                                          @Argument int tamanho,
                                          @AuthenticationPrincipal UsuarioAutenticado usuario) {
        FiltroAgendamentoInput efetivo = filtro != null ? filtro : FiltroAgendamentoInput.vazio();
        return PaginaAgendamento.de(service.buscar(efetivo, pagina, tamanho, escopoDe(usuario)));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ENFERMEIRO')")
    public Agendamento criarAgendamento(@Argument @Valid NovoAgendamentoInput input) {
        return service.criar(input);
    }

    @MutationMapping
    @PreAuthorize("hasRole('MEDICO')")
    public Agendamento atualizarAgendamento(@Argument Long id,
                                            @Argument @Valid AtualizarAgendamentoInput input) {
        return service.atualizar(id, input);
    }

    @MutationMapping
    @PreAuthorize("hasRole('MEDICO')")
    public Agendamento cancelarAgendamento(@Argument Long id) {
        return service.cancelar(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('MEDICO')")
    public boolean removerAgendamento(@Argument Long id) {
        return service.remover(id);
    }

    /**A
     * Decide pela role, nunca por "tem pacienteId": no dia em que um
     * profissional tambem tiver cadastro de paciente, a segunda leitura
     * restringiria a agenda dele.
     */
    private EscopoConsulta escopoDe(UsuarioAutenticado usuario) {
        if (usuario == null) {
            // O @PreAuthorize acima ja barrou o anonimo; se chegou aqui nulo e
            // porque o principal nao e um UsuarioAutenticado. Falha fechada.
            throw new AccessDeniedException("principal ausente ou de tipo inesperado");
        }
        if (usuario.role() != Role.PACIENTE) {
            return EscopoConsulta.irrestrito();
        }
        if (usuario.pacienteId() == null) {
            // A check constraint ck_usuario_vinculo garante que nao acontece;
            // a guarda existe para nao virar consulta irrestrita se mudar.
            throw new AccessDeniedException("usuario PACIENTE sem paciente vinculado");
        }
        return EscopoConsulta.doPaciente(usuario.pacienteId());
    }
}
