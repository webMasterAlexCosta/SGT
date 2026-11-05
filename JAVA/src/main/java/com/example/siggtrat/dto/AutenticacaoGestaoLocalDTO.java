package com.example.siggtrat.dto;

/** Opção pública mínima usada na solicitação de acesso. */
public record AutenticacaoGestaoLocalDTO(
        Long id,
        String nome,
        String codigo,
        Long superintendenciaId,
        String uf
) {}
