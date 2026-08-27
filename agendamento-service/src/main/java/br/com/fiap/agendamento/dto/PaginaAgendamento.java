package br.com.fiap.agendamento.dto;

import br.com.fiap.agendamento.entity.Agendamento;
import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaAgendamento(
        List<Agendamento> conteudo,
        int pagina,
        int tamanho,
        int totalElementos,
        int totalPaginas
) {
    public static PaginaAgendamento de(Page<Agendamento> page) {
        return new PaginaAgendamento(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                (int) page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
