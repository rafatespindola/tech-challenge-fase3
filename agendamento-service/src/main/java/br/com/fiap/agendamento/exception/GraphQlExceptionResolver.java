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
 * Roda antes do SecurityDataFetcherExceptionResolver que o Boot registra, para
 * que a falha de login traga a mensagem daqui em vez de um "Unauthorized" seco.
 * O que este resolver nao reconhece continua caindo la - inclusive o
 * AccessDeniedException de quem chama sem token.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
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

        // Falha de login responde sempre igual, sem distinguir usuario inexistente
        // de senha errada, para nao servir de sonda de logins validos.
        String mensagem = switch (ex) {
            case OptimisticLockingFailureException ignored ->
                    "O agendamento foi alterado por outra requisicao. Recarregue e tente novamente.";
            case AuthenticationException ignored -> "login ou senha invalidos";
            default -> ex.getMessage();
        };

        return GraphQLError.newError()
                .errorType(tipo)
                .message(mensagem)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
}
