package br.com.fiap.agendamento.security;

import br.com.fiap.agendamento.entity.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Autentica o teste como um {@link UsuarioAutenticado} de verdade.
 *
 * @WithMockUser nao serve aqui: ele monta um principal do tipo User, e o
 * resolver de @AuthenticationPrincipal devolve null sem reclamar quando o tipo
 * nao casa (errorOnInvalidType e false por padrao) - o sintoma seria um NPE
 * longe da causa.
 *
 * Ids de vinculo em zero significam ausente, ja que anotacao nao aceita null.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@WithSecurityContext(factory = ComoUsuarioSecurityContextFactory.class)
public @interface ComoUsuario {

    Role role();

    long id() default 1L;

    String login() default "usuario-de-teste";

    String nome() default "Usuario de Teste";

    long pacienteId() default 0L;

    long profissionalId() default 0L;
}
