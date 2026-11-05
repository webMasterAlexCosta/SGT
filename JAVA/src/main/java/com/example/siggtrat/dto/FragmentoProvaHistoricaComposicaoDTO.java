package com.example.siggtrat.dto;

import java.util.Map;

/** O fragmento só declara propriedades desta revisão, sem herdar valores de outras fontes. */
public record FragmentoProvaHistoricaComposicaoDTO(Long composicaoId, Long mapeamentoId, Long aditamentoComposicaoId,
        String lotacaoNome, String jornadaDescricao, String cargoNome, String lotacaoMunicipio,
        boolean municipioComprovado, String jornadaSemanal, boolean jornadaComprovada,
        Map<String, ProvaCampoHistoricoDTO> provaCampos) { }
