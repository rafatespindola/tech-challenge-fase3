package br.com.fiap.agendamento.security;

import br.com.fiap.agendamento.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Teste de unidade: o JwtService nao depende de contexto Spring nem de banco. */
class JwtServiceTest {

    private static final String SEGREDO = "chave-de-teste-do-agendamento-service-com-mais-de-32-caracteres";
    private static final String OUTRO_SEGREDO = "outra-chave-igualmente-longa-para-o-teste-de-assinatura";

    private final JwtService jwtService = new JwtService(SEGREDO, 120);

    @Test
    void tokenDePacienteCarregaOVinculoDeVolta() {
        UsuarioAutenticado paciente = UsuarioAutenticado.deToken(
                3L, "paciente", "Maria Silva", Role.PACIENTE, 1L, null);

        UsuarioAutenticado lido = principalDe(jwtService.gerar(paciente).valor());

        assertThat(lido.id()).isEqualTo(3L);
        assertThat(lido.login()).isEqualTo("paciente");
        assertThat(lido.nome()).isEqualTo("Maria Silva");
        assertThat(lido.role()).isEqualTo(Role.PACIENTE);
        assertThat(lido.pacienteId()).isEqualTo(1L);
        assertThat(lido.profissionalId()).isNull();
        // O hash nunca viaja no token.
        assertThat(lido.senhaHash()).isNull();
    }

    @Test
    void tokenDeProfissionalNaoTrazPacienteId() {
        UsuarioAutenticado medico = UsuarioAutenticado.deToken(
                1L, "medico", "Dra. Helena Prado", Role.MEDICO, null, 7L);

        UsuarioAutenticado lido = principalDe(jwtService.gerar(medico).valor());

        assertThat(lido.pacienteId()).isNull();
        assertThat(lido.profissionalId()).isEqualTo(7L);
        assertThat(lido.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_MEDICO");
    }

    @Test
    void expiracaoVaiNaResposta() {
        JwtService.Token token = jwtService.gerar(usuarioQualquer());

        assertThat(token.expiraEm()).isAfter(java.time.OffsetDateTime.now().plusMinutes(119));
    }

    @Test
    void recusaTokenAdulterado() {
        String token = jwtService.gerar(usuarioQualquer()).valor();
        // Mexer no ultimo caractere invalida a assinatura sem quebrar o formato.
        String adulterado = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThat(jwtService.autenticacaoDe(adulterado)).isEmpty();
    }

    @Test
    void recusaTokenAssinadoComOutraChave() {
        String token = new JwtService(OUTRO_SEGREDO, 120).gerar(usuarioQualquer()).valor();

        assertThat(jwtService.autenticacaoDe(token)).isEmpty();
    }

    @Test
    void recusaTokenExpirado() {
        String expirado = new JwtService(SEGREDO, -1).gerar(usuarioQualquer()).valor();

        assertThat(jwtService.autenticacaoDe(expirado)).isEmpty();
    }

    @Test
    void recusaLixoNoLugarDoToken() {
        assertThat(jwtService.autenticacaoDe("nem-parece-um-jwt")).isEmpty();
        assertThat(jwtService.autenticacaoDe("")).isEmpty();
    }

    @Test
    void naoSobeComSegredoCurto() {
        assertThatThrownBy(() -> new JwtService("curto-demais", 120))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    private UsuarioAutenticado usuarioQualquer() {
        return UsuarioAutenticado.deToken(1L, "medico", "Dra. Helena Prado", Role.MEDICO, null, 7L);
    }

    private UsuarioAutenticado principalDe(String token) {
        Optional<Authentication> autenticacao = jwtService.autenticacaoDe(token);
        assertThat(autenticacao).isPresent();
        return (UsuarioAutenticado) autenticacao.get().getPrincipal();
    }
}
