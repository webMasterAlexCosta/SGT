package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.pessoa.TipoCorrecaoPessoa;
import java.time.Instant;

public record CorrecaoPessoaDTO(
        Long id,
        Long pessoaId,
        TipoCorrecaoPessoa tipo,
        String valorAnterior,
        String valorNovo,
        String motivo,
        String evidencia,
        String autorizacao,
        String impactosEncontrados,
        Long responsavelId,
        String responsavelUsername,
        Instant registradoEm
) {}
