package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Quando conteudoMinimizado é verdadeiro, snapshots, motivo e login são a
 * representação autorizada do evento. Os hashes continuam identificando a
 * prova integral preservada, e não os bytes dessa representação reduzida.
 */
public record EventoFrequenciaDTO(
        Long id,
        Long frequenciaId,
        Integer numeroVersao,
        TipoEventoFrequencia tipoEvento,
        LocalDate dataReferencia,
        String snapshotAnterior,
        String snapshotNovo,
        String motivo,
        Long usuarioId,
        String usuarioUsername,
        Instant registradoEm,
        String hashAnterior,
        String hashEvento,
        boolean conteudoMinimizado
) {
    public EventoFrequenciaDTO(Long id, Long frequenciaId, Integer numeroVersao,
                              TipoEventoFrequencia tipoEvento, LocalDate dataReferencia,
                              String snapshotAnterior, String snapshotNovo, String motivo,
                              Long usuarioId, String usuarioUsername, Instant registradoEm,
                              String hashAnterior, String hashEvento) {
        this(id, frequenciaId, numeroVersao, tipoEvento, dataReferencia, snapshotAnterior, snapshotNovo,
                motivo, usuarioId, usuarioUsername, registradoEm, hashAnterior, hashEvento, false);
    }
}
