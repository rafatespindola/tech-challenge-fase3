package br.com.fiap.historico.security;

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
import java.util.Optional;

/**
 * Confere os tokens emitidos pelo agendamento-service. So isso: nao existe
 * metodo de emissao aqui, porque um token de dois emissores diferentes seria
 * impossivel de revogar em um lugar so.
 *
 * A verificacao funciona porque {@code jwt.secret} e o mesmo nos dois servicos.
 * O preco disso e que quem valida tambem conseguiria assinar: se este segredo
 * vazar, da para forjar um token de MEDICO. Em producao o certo seria RS256,
 * com o agendamento assinando com a chave privada e este servico validando so
 * com a publica.
 *
 * As claims vem do JwtService do agendamento-service; os nomes abaixo sao o
 * contrato entre os dois e nao podem divergir.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Piso de 256 bits: abaixo disso a jjwt recusa a chave. */
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private static final String CLAIM_ID = "uid";
    private static final String CLAIM_NOME = "nome";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_PACIENTE_ID = "pid";
    private static final String CLAIM_PROFISSIONAL_ID = "prid";

    private final SecretKey chave;

    public JwtService(@Value("${jwt.secret}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "jwt.secret precisa de pelo menos %d caracteres".formatted(TAMANHO_MINIMO_SEGREDO));
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * Devolve vazio para qualquer token que nao sirva - assinatura invalida,
     * expirado, malformado ou com uma role que este servico nao conhece. Quem
     * chama trata todos igual: segue sem autenticacao, e a autorizacao decide o
     * erro.
     */
    public Optional<Authentication> autenticacaoDe(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UsuarioAutenticado usuario = new UsuarioAutenticado(
                    claims.get(CLAIM_ID, Long.class),
                    claims.getSubject(),
                    claims.get(CLAIM_NOME, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    claims.get(CLAIM_PACIENTE_ID, Long.class),
                    claims.get(CLAIM_PROFISSIONAL_ID, Long.class));

            return Optional.of(UsernamePasswordAuthenticationToken.authenticated(
                    usuario, null, usuario.authorities()));

        } catch (JwtException | IllegalArgumentException excecao) {
            // IllegalArgumentException cobre tambem o Role.valueOf de uma role
            // que so exista no emissor: melhor recusar o token do que autenticar
            // alguem sem authority nenhuma.
            log.debug("token recusado: {}", excecao.getMessage());
            return Optional.empty();
        }
    }
}
