package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.pessoa.TipoCorrecaoPessoa;

public record CorrecaoIdentidadePessoaRequisicao(
        TipoCorrecaoPessoa tipo,
        String novoValor,
        String motivo,
        String evidencia,
        String autorizacao,
        String impactosEncontrados
) {}
