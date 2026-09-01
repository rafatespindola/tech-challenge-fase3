package br.com.fiap.historico.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O contrato entre os dois servicos e o formato do token, nao uma classe
 * compartilhada - entao o teste emite aqui um token com exatamente o layout de
 * claims que o JwtService do agendamento-service produz e confere que este
 * servico o aceita. Se o emissor renomear uma claim, e aqui que quebra.
 */
class JwtServiceTest {

    private static final String SEGREDO =
            "chave-de-teste-do-agendamento-service-com-mais-de-32-caracteres";

    private final JwtService jwtService = new JwtService(SEGREDO);

    @Test
    @DisplayName("aceita o token emitido pelo agendamento-service e remonta o usuario")
    void aceitaTokenDoEmissor() {
        String token = token(SEGREDO, "dr.carlos", "MEDICO", 9L, null, 2L,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        Authentication autenticacao = jwtService.autenticacaoDe(token).orElseThrow();
        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacao.getPrincipal();

        assertThat(usuario.id()).isEqualTo(9L);
        assertThat(usuario.login()).isEqualTo("dr.carlos");
        assertThat(usuario.role()).isEqualTo(Role.MEDICO);
        assertThat(usuario.profissionalId()).isEqualTo(2L);
        assertThat(usuario.pacienteId()).isNull();
        assertThat(autenticacao.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_MEDICO");
    }

    @Test
    @DisplayName("recusa token assinado com outro segredo, expirado ou malformado")
    void recusaTokenQueNaoServe() {
        String outroSegredo = token(
                "outro-segredo-de-32-caracteres-no-minimo-mesmo", "ana", "PACIENTE",
                1L, 1L, null, Instant.now().plus(30, ChronoUnit.MINUTES));
        String expirado = token(SEGREDO, "ana", "PACIENTE", 1L, 1L, null,
                Instant.now().minus(1, ChronoUnit.MINUTES));

        assertThat(jwtService.autenticacaoDe(outroSegredo)).isEmpty();
        assertThat(jwtService.autenticacaoDe(expirado)).isEmpty();
        assertThat(jwtService.autenticacaoDe("nem-parece-um-jwt")).isEmpty();
    }

    @Test
    @DisplayName("recusa token com role que este servico nao conhece, em vez de autenticar sem authority")
    void recusaRoleDesconhecida() {
        String token = token(SEGREDO, "novo", "RECEPCIONISTA", 3L, null, null,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        Optional<Authentication> autenticacao = jwtService.autenticacaoDe(token);

        assertThat(autenticacao).isEmpty();
    }

    @Test
    @DisplayName("segredo curto derruba o servico no start, e nao silenciosamente")
    void segredoCurtoFalhaRapido() {
        assertThatThrownBy(() -> new JwtService("curto-demais"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }

    private static String token(String segredo, String login, String role, Long id,
                                Long pacienteId, Long profissionalId, Instant expiracao) {
        SecretKey chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(login)
                .claim("uid", id)
                .claim("nome", "Fulano")
                .claim("role", role)
                .claim("pid", pacienteId)
                .claim("prid", profissionalId)
                .issuedAt(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
                .expiration(Date.from(expiracao))
                .signWith(chave)
                .compact();
    }
}
