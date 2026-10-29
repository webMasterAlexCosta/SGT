package com.example.siggtrat.utilitario;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginacaoUtilsTest {

    @Test
    void limitaTamanhoDaPagina() {
        Pageable pageable = UtilitarioPaginacao.limitar(
                PageRequest.of(2, 50_000), Sort.by("id").ascending());

        assertEquals(2, pageable.getPageNumber());
        assertEquals(UtilitarioPaginacao.TAMANHO_MAXIMO, pageable.getPageSize());
    }

    @Test
    void aplicaOrdenacaoPadrao() {
        Pageable pageable = UtilitarioPaginacao.limitar(
                PageRequest.of(0, 10), Sort.by("dataReferencia").descending());

        assertEquals(Sort.Direction.DESC,
                pageable.getSort().getOrderFor("dataReferencia").getDirection());
    }
}
