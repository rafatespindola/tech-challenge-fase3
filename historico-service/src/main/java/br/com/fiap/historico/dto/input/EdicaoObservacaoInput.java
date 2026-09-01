package br.com.fiap.historico.dto.input;

import jakarta.validation.constraints.Size;

/**
 * Corpo do PATCH. So a observacao: e o unico campo do historico que e anotacao
 * livre, e nao retrato do que aconteceu no agendamento-service.
 *
 * Nulo e um valor legitimo aqui - significa apagar a observacao - e por isso nao
 * ha @NotNull. O limite de 500 acompanha o da coluna: sem ele o erro so
 * apareceria como violacao de integridade no flush, virando 500.
 */
public record EdicaoObservacaoInput(
        @Size(max = 500, message = "a observacao deve ter no maximo 500 caracteres")
        String observacao
) { }
