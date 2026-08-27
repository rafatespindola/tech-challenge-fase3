package br.com.fiap.agendamento.config;

import br.com.fiap.agendamento.security.JwtAuthenticationFilter;
import br.com.fiap.agendamento.security.JwtService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Com o login em POST /auth/login, e nao como mutation, a cadeia HTTP volta a
 * conseguir separar rota publica de protegida: /graphql exige token aqui mesmo.
 *
 * Isso nao dispensa o @PreAuthorize nos controllers. A regra HTTP nao consegue
 * expressar "PACIENTE consulta mas nao altera", porque as duas coisas chegam no
 * mesmo POST /graphql; ela cobre apenas o caso "nenhum token valido".
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
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        // O console e um unico index.html que busca os assets no
                        // unpkg.com, entao o path exato basta.
                        .requestMatchers(HttpMethod.GET, "/graphiql").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/graphql").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * BCrypt e o mesmo algoritmo dos hashes gravados em data.sql, na forca 10
     * que e o default desta classe. Trocar aqui invalida aqueles registros.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expoe o AuthenticationManager montado pelo Spring a partir do
     * UserDetailsService e do PasswordEncoder, para o login poder usa-lo.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao)
            throws Exception {
        return configuracao.getAuthenticationManager();
    }
}
