package com.example.siggtrat.dto;

import jakarta.validation.constraints.Size;

public record TratamentoPendenciaRequisicao(
        @Size(max = 1000) String observacao
) { }
