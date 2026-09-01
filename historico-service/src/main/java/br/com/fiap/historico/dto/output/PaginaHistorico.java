package br.com.fiap.historico.dto.output;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaHistorico(
        List<HistoricoOutput> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {
    public static PaginaHistorico de(Page<HistoricoOutput> page) {
        return new PaginaHistorico(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
