package br.com.fiap.historico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * A exclusao existe porque este servico nao tem UserDetailsService: sem ela o
 * Boot cria um usuario "user" em memoria com senha aleatoria e a imprime no log
 * a cada start. Aqui isso seria so ruido - e um usuario a mais no
 * AuthenticationManager - ja que a autenticacao vem do token emitido pelo
 * agendamento-service, e nao de credencial conferida aqui.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class HistoricoServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(HistoricoServiceApplication.class, args);
	}

}
