package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Metadados públicos da cadeia; a fonte restrita conserva seu próprio controle de acesso. */
public record ProvaHistoricaContratoDTO(Long id, Long contratoId, Long eventoOrigemId,
        Long provaAnteriorId, Integer numeroRevisao, Long versaoAnteriorId, Long versaoGeradaId,
        LocalDate dataEfetiva, Instant registradoEm, String hashConteudo,
        boolean configuracaoCompletaComprovada, Set<String> camposComprovados,
        VersaoContratoLeituraDTO fotografia, List<FragmentoProvaHistoricaComposicaoDTO> composicoes,
        Map<String,ProvaCampoHistoricoDTO> provaCampos) { }
