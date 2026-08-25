package br.com.fiap.agendamento.config;

import br.com.fiap.agendamento.security.JwtAuthenticationFilter;
import br.com.fiap.agendamento.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Em uma API GraphQL toda operacao chega no mesmo POST /graphql, entao a
 * cadeia HTTP nao tem como distinguir login de agendamento: a URL e a mesma.
 * Por isso /graphql e liberado aqui e a exigencia de autenticacao vive no nivel
 * do campo, via @PreAuthorize nos controllers ({@link EnableMethodSecurity}).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        return http
                // Sem cookie de sessao nao ha CSRF a proteger, e o formulario de
                // login padrao so atrapalharia: o login e uma mutation.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requisicoes -> requisicoes
                        .requestMatchers("/graphql").permitAll()
                        .requestMatchers("/graphiql", "/graphiql/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * BCrypt e o mesmo algoritmo dos hashes gravados em data.sql. Trocar aqui
     * invalida aqueles registros.
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
