package com.example.siggtrat.utilitario;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class UtilitarioPaginacao {

    public static final int TAMANHO_PADRAO = 100;
    public static final int TAMANHO_MAXIMO = 1_000;

    private UtilitarioPaginacao() {
    }

    public static Pageable limitar(Pageable pageable, Sort ordenacaoPadrao) {
        int pagina = pageable == null ? 0 : Math.max(0, pageable.getPageNumber());
        int solicitado = pageable == null ? TAMANHO_PADRAO : pageable.getPageSize();
        int tamanho = Math.max(1, Math.min(solicitado, TAMANHO_MAXIMO));
        Sort ordenacao = pageable != null && pageable.getSort().isSorted()
                ? pageable.getSort()
                : ordenacaoPadrao;
        return PageRequest.of(pagina, tamanho, ordenacao == null ? Sort.unsorted() : ordenacao);
    }

    public static Pageable limitarComOrdenacaoFixa(Pageable pageable, Sort ordenacao) {
        int pagina = pageable == null ? 0 : Math.max(0, pageable.getPageNumber());
        int solicitado = pageable == null ? TAMANHO_PADRAO : pageable.getPageSize();
        int tamanho = Math.max(1, Math.min(solicitado, TAMANHO_MAXIMO));
        return PageRequest.of(pagina, tamanho, ordenacao == null ? Sort.unsorted() : ordenacao);
    }

}
