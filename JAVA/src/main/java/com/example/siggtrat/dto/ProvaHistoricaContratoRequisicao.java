package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Valores documentados explicitamente; nenhum rótulo é inferido do cadastro mestre. */
public record ProvaHistoricaContratoRequisicao(
        Long eventoOrigemId, Long versaoAnteriorId, Long provaAnteriorId,
        Boolean recuperarPublicacaoOriginal, LocalDate dataDocumento,
        String referenciaSei, Long documentoComprobatorioId, String finalidadeAcessoDocumento,
        String motivo, String autorizacaoAdministrativa, Long versaoContratoEsperada,
        String chaveIdempotencia, Boolean configuracaoCompletaComprovada,
        Set<String> camposComprovados, VersaoContratoNegocialDTO fotografia,
        List<Item> composicoes, String tokenPrevia, Boolean confirmarImpacto) {
    public record Item(Long composicaoId, Long mapeamentoId, Long aditamentoComposicaoId, String lotacaoNome,
            String jornadaDescricao, String cargoNome, String lotacaoMunicipio,
            Boolean municipioComprovado, String jornadaSemanal, Boolean jornadaComprovada) {
        public Item(Long composicaoId, Long mapeamentoId, String lotacaoNome,
                String jornadaDescricao, String cargoNome, String lotacaoMunicipio,
                Boolean municipioComprovado, String jornadaSemanal, Boolean jornadaComprovada) {
            this(composicaoId, mapeamentoId, null, lotacaoNome, jornadaDescricao, cargoNome,
                    lotacaoMunicipio, municipioComprovado, jornadaSemanal, jornadaComprovada);
        }
    }
}
