package com.example.siggtrat.dto;
import java.time.Instant;
public record RevisaoFrequenciaDTO(Long frequenciaId, Integer versao, String status, Instant solicitadaEm,
        String motivo, Instant decididaEm, String decisao, boolean podeSolicitar, boolean podeDecidir) {
    public record Comando(@jakarta.validation.constraints.NotNull Integer versao,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min=5,max=1000) String motivo,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min=8,max=120) String chaveIdempotencia,
            String decisao) {}
}
