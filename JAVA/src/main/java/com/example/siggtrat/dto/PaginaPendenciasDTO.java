package com.example.siggtrat.dto;

import java.util.List;

public record PaginaPendenciasDTO(
        List<PendenciaDTO> registros,
        long totalElementos,
        int totalPaginas,
        int pagina,
        int tamanho,
        boolean primeira,
        boolean ultima
) { }
