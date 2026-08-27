package br.com.fiap.agendamento.exception;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Sem isto toda excecao vira INTERNAL_ERROR com a mensagem "INTERNAL_ERROR for <id>",
 * escondendo do cliente o motivo real da falha.
 *
 * AccessDeniedException fica de fora de proposito: o resolver do proprio Boot
 * (SecurityDataFetcherExceptionResolver) sabe distinguir anonimo, que merece
 * UNAUTHORIZED, de autenticado sem a role, que merece FORBIDDEN - distincao que
 * um case chapado aqui perderia.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        ErrorType tipo = switch (ex) {
            case RecursoNaoEncontradoException ignored -> ErrorType.NOT_FOUND;
            case RegraNegocioException ignored -> ErrorType.BAD_REQUEST;
            case ConstraintViolationException ignored -> ErrorType.BAD_REQUEST;
            case OptimisticLockingFailureException ignored -> ErrorType.BAD_REQUEST;
            case AuthenticationException ignored -> ErrorType.UNAUTHORIZED;
            default -> null;
        };
        if (tipo == null) {
            return null; // deixa o tratamento padrao cuidar (INTERNAL_ERROR + log)
        }

        String mensagem = ex instanceof OptimisticLockingFailureException
                ? "O agendamento foi alterado por outra requisicao. Recarregue e tente novamente."
                : ex.getMessage();

        return GraphQLError.newError()
                .errorType(tipo)
                .message(mensagem)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
}
