package br.com.fiap.agendamento.dto.request;

import br.com.fiap.agendamento.entity.TIPO_PAGAMENTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarPagamentoRequest(
        @NotBlank String nome,
        @NotNull TIPO_PAGAMENTO tipoPagamento
) {}
