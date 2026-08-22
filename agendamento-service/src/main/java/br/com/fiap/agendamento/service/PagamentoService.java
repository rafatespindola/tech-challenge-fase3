package br.com.fiap.agendamento.service;

import br.com.fiap.agendamento.dto.request.CriarPagamentoRequest;
import br.com.fiap.agendamento.dto.response.CriarPagamentoResponse;
import br.com.fiap.agendamento.entity.Pagamento;
import br.com.fiap.agendamento.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;

    public CriarPagamentoResponse criarPagamento(CriarPagamentoRequest request) {
        Pagamento pagamento = Pagamento.builder()
                .nome(request.nome())
                .tipoPagamento(request.tipoPagamento())
                .build();

        Pagamento pagamentoSalvo = pagamentoRepository.save(pagamento);

        return converteParaResponse(pagamentoSalvo);
    }

    public CriarPagamentoResponse converteParaResponse(Pagamento pagamento) {
        return new CriarPagamentoResponse(pagamento.getId(), pagamento.getNome(), pagamento.getTipoPagamento());
    }


}
