package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.documento.*;

import java.time.LocalDate;

/** Metadados enviados na parte JSON do upload multipart. */
public record CadastroDocumentoComprobatorioRequisicao(
        TipoFatoDocumental tipoFato,
        Long fatoId,
        String fatoChave,
        String titulo,
        String descricao,
        String referenciaSei,
        ClassificacaoAcessoDocumento classificacaoAcesso,
        LocalDate dataEfetiva,
        String chaveIdempotencia
) {}

