package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.pendencia.EstadoPendencia;
import com.example.siggtrat.dominio.pendencia.EscopoBloqueioPendencia;
import com.example.siggtrat.dominio.pendencia.TipoPendencia;

import java.time.Instant;
import java.time.LocalDate;

public record PendenciaDTO(
        Long id,
        TipoPendencia tipo,
        EstadoPendencia estado,
        String motivo,
        Instant surgidaEm,
        LocalDate dataEfetiva,
        String operacaoOrigem,
        PerfilAcesso perfilResponsavel,
        EscopoBloqueioPendencia escopoBloqueio,
        Long contratoId,
        String contrato,
        Long pessoaId,
        String pessoa,
        Long vinculoId,
        Long frequenciaId,
        Long gestaoLocalId,
        Long superintendenciaId,
        String origemEntidade,
        Long origemId,
        Instant iniciadaEm,
        String iniciadaPor,
        Instant resolvidaEm,
        String resolvidaPor,
        String observacaoTratamento,
        String acaoSugerida
) { }
