package br.com.fiap.agendamento.dto.response;

import br.com.fiap.agendamento.entity.TIPO_PAGAMENTO;

import java.util.UUID;

public record CriarPagamentoResponse(
        UUID id,
        String nome,
        TIPO_PAGAMENTO tipoPagamento
) {}