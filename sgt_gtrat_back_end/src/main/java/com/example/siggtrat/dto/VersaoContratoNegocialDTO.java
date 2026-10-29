package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;

public record VersaoContratoNegocialDTO(
        Long id,
        Integer numeroVersao,
        Long eventoOrigemId,
        LocalDate dataEfetiva,
        Instant registradoEm,
        String numeroContrato,
        Integer anoContrato,
        String descricao,
        Long tipoContratacaoId,
        String tipoContratacaoDescricao,
        Long fornecedorId,
        String cnpjEmpresa,
        String razaoSocialEmpresa,
        String nomeFantasiaEmpresa,
        LocalDate dataInicio,
        LocalDate dataFim,
        Integer efetivoPrevisto,
        Long gestaoLocalId,
        String gestaoLocalNome
) {}
