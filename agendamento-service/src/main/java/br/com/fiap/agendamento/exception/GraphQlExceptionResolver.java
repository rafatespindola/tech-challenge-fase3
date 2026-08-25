package br.com.fiap.agendamento.exception;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * Sem isto toda excecao vira INTERNAL_ERROR com a mensagem "INTERNAL_ERROR for <id>",
 * escondendo do cliente o motivo real da falha.
 */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        ErrorType tipo = switch (ex) {
            case RecursoNaoEncontradoException ignored -> ErrorType.NOT_FOUND;
            case RegraNegocioException ignored -> ErrorType.BAD_REQUEST;
            case ConstraintViolationException ignored -> ErrorType.BAD_REQUEST;
            case OptimisticLockingFailureException ignored -> ErrorType.BAD_REQUEST;
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
