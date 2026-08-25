package br.com.fiap.agendamento.security;

import br.com.fiap.agendamento.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sem contexto Spring: o JwtService so depende de segredo e validade. */
class JwtServiceTest {

    private static final String SEGREDO = "chave-de-teste-do-agendamento-service-com-tamanho-suficiente";

    private final JwtService jwtService = new JwtService(SEGREDO, 60);

    private final UsuarioAutenticado usuario =
            new UsuarioAutenticado(7L, "medico", "Dra. Helena Prado", Role.MEDICO, null);

    @Test
    void tokenGeradoVoltaComOMesmoUsuario() {
        JwtService.Token token = jwtService.gerar(usuario);

        Authentication autenticacao = jwtService.autenticacaoDe(token.valor()).orElseThrow();
        UsuarioAutenticado doToken = (UsuarioAutenticado) autenticacao.getPrincipal();

        assertThat(doToken.id()).isEqualTo(7L);
        assertThat(doToken.login()).isEqualTo("medico");
        assertThat(doToken.nome()).isEqualTo("Dra. Helena Prado");
        assertThat(doToken.role()).isEqualTo(Role.MEDICO);
        assertThat(autenticacao.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_MEDICO");
    }

    @Test
    void expiraEmRespeitaAValidadeConfigurada() {
        JwtService.Token token = jwtService.gerar(usuario);

        assertThat(token.expiraEm()).isCloseTo(
                Instant.now().plusSeconds(3600).atOffset(token.expiraEm().getOffset()),
                org.assertj.core.api.Assertions.within(30, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void recusaTokenAssinadoComOutraChave() {
        String intruso = Jwts.builder()
                .subject("medico")
                .claim("uid", 7L)
                .claim("nome", "Dra. Helena Prado")
                .claim("role", "MEDICO")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(
                        "outra-chave-completamente-diferente-mas-longa".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.autenticacaoDe(intruso)).isEmpty();
    }

    @Test
    void recusaTokenExpirado() {
        JwtService expirado = new JwtService(SEGREDO, -1);

        assertThat(jwtService.autenticacaoDe(expirado.gerar(usuario).valor())).isEmpty();
    }

    @Test
    void recusaTextoQueNaoEToken() {
        assertThat(jwtService.autenticacaoDe("nao-e-um-jwt")).isEqualTo(Optional.empty());
    }

    @Test
    void recusaSegredoCurtoNaInicializacao() {
        assertThatThrownBy(() -> new JwtService("curto-demais", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
