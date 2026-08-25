package br.com.fiap.agendamento.security;

import br.com.fiap.agendamento.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

/**
 * Emite e confere os tokens. Unico ponto do sistema que conhece o formato do
 * JWT: quem chama lida so com {@link UsuarioAutenticado} e {@link Token}.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** HS256 exige chave de 256 bits; abaixo disso a jjwt recusa a assinatura. */
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private static final String CLAIM_ID = "uid";
    private static final String CLAIM_NOME = "nome";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey chave;
    private final Duration validade;

    public JwtService(@Value("${jwt.secret}") String segredo,
                      @Value("${jwt.expiracao-minutos}") long expiracaoMinutos) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "jwt.secret precisa de pelo menos %d caracteres".formatted(TAMANHO_MINIMO_SEGREDO));
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.validade = Duration.ofMinutes(expiracaoMinutos);
    }

    public Token gerar(UsuarioAutenticado usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(validade);

        String valor = Jwts.builder()
                .subject(usuario.login())
                .claim(CLAIM_ID, usuario.id())
                .claim(CLAIM_NOME, usuario.nome())
                .claim(CLAIM_ROLE, usuario.role().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(chave)
                .compact();

        return new Token(valor, expiracao.atOffset(ZoneOffset.UTC));
    }

    /**
     * Devolve vazio para qualquer token que nao sirva - assinatura invalida,
     * expirado ou malformado. Quem chama trata os tres casos igual: segue sem
     * autenticacao, e a autorizacao decide o erro.
     */
    public Optional<Authentication> autenticacaoDe(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UsuarioAutenticado usuario = UsuarioAutenticado.deToken(
                    claims.get(CLAIM_ID, Long.class),
                    claims.getSubject(),
                    claims.get(CLAIM_NOME, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)));

            return Optional.of(UsernamePasswordAuthenticationToken.authenticated(
                    usuario, null, usuario.getAuthorities()));

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("token recusado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** O instante de expiracao vai junto para o cliente saber quando renovar. */
    public record Token(String valor, OffsetDateTime expiraEm) { }
}
