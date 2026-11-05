package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Referências públicas de registros reais; nunca contém a fonte documental restrita. */
public record OrigemProvaCampoDTO(
        Tipo tipo, Long provaId, Long eventoId, Long versaoId, Long composicaoId,
        Long aditamentoComposicaoId, Long mapeamentoId, LocalDate dataEfetiva, Instant registradoEm) {
    public enum Tipo {
        FOTOGRAFIA_CONTRATUAL, PROVA_HISTORICA, EVENTO_CONTRATUAL,
        COMPOSICAO_PUBLICADA, ITEM_ADITAMENTO, MAPEAMENTO_TRANSFERENCIA
    }
}
