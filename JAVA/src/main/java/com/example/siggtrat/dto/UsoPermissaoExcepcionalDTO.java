package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;

import java.time.Instant;

public record UsoPermissaoExcepcionalDTO(
        Long id,
        Long permissaoId,
        TipoPermissaoExcepcional tipo,
        Long usuarioId,
        String usuario,
        String tipoRecurso,
        Long recursoId,
        String finalidade,
        Instant usadoEm
) {}
