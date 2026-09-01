package br.com.fiap.historico.config;

import br.com.fiap.historico.security.JwtAuthenticationFilter;
import br.com.fiap.historico.security.JwtService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cadeia bem mais curta que a do agendamento-service, e por um motivo: este
 * servico nao autentica ninguem. Nao ha /auth/login, PasswordEncoder,
 * AuthenticationManager nem UserDetailsService, porque nao existe tabela de
 * usuario aqui - o token chega pronto, assinado la, e o unico trabalho e
 * conferir a assinatura.
 *
 * A regra HTTP separa leitura de escrita: GET exige so token valido (as tres
 * roles leem) e o PATCH exige MEDICO. O @PreAuthorize nos metodos do controller
 * repete a mesma matriz de proposito - se um dia a API ganhar outro caminho de
 * escrita, o metodo continua protegido mesmo que o matcher aqui fique para tras.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   AuthenticationEntryPoint entryPoint,
                                                   AccessDeniedHandler accessDeniedHandler)
            throws Exception {
        return http
                // Sem cookie de sessao nao ha CSRF a proteger, e os formularios de
                // login e logout padrao so atrapalhariam nesta API.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requisicoes -> requisicoes
                        // O Spring Security 6 autoriza tambem o dispatch de erro. Sem
                        // esta linha, um 404 de requisicao sem token e encaminhado para
                        // /error e volta como 401, escondendo o problema real.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/historicos/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/historicos/**").hasRole("MEDICO")
                        .anyRequest().denyAll())
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
