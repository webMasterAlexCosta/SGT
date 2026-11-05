package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.documento.*;

import java.time.Instant;
import java.time.LocalDate;

public record DocumentoComprobatorioDTO(
        Long id,
        Long contratoId,
        TipoFatoDocumental tipoFato,
        Long fatoId,
        String fatoChave,
        String grupoLogicoId,
        Integer versaoDocumento,
        Long documentoAnteriorId,
        EstadoDocumentoComprobatorio estado,
        ClassificacaoAcessoDocumento classificacaoAcesso,
        TipoFonteComprobatoria tipoFonte,
        String titulo,
        String descricao,
        String referenciaSei,
        String nomeOriginalArquivo,
        String tipoMime,
        Long tamanhoBytes,
        String sha256Arquivo,
        LocalDate dataEfetiva,
        Instant registradoEm,
        Long registradoPorId,
        String registradoPorUsername,
        Instant publicadoEm,
        Long publicadoPorId,
        String publicadoPorUsername,
        boolean versaoAtual,
        boolean downloadDisponivel,
        long version
) {}

