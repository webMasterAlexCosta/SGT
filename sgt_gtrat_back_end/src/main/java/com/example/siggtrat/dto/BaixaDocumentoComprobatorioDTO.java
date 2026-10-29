package com.example.siggtrat.dto;

import java.nio.file.Path;

public record BaixaDocumentoComprobatorioDTO(
        Path caminho,
        String nomeOriginal,
        String tipoMime,
        long tamanhoBytes
) {}

