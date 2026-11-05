package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.TipoRelacaoPrecedenciaNegocial;
import java.time.LocalDate;
import java.util.List;

public record RemanejamentoContratualRequisicao(
        String identificacao,
        LocalDate dataEfetiva,
        String justificativa,
        String referenciaSei,
        Boolean confirmarImpacto,
        List<RemanejamentoComposicaoRequisicao> itens,
        Long eventoPrecedenteId,
        TipoRelacaoPrecedenciaNegocial relacaoPrecedencia,
        Integer ordemDocumental,
        String tokenPrevia
) {}
