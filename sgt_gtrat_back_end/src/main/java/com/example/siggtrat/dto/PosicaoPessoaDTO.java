package com.example.siggtrat.dto;

import java.time.*;
import java.util.List;

public record PosicaoPessoaDTO(
        Long pessoaId,
        String nomeAtual,
        String cpfAtual,
        LocalDate dataReferencia,
        Instant conhecidoAte,
        List<PosicaoVinculoTemporalDTO> vinculos
) {}
