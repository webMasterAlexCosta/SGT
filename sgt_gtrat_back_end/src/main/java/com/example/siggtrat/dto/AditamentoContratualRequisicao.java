package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.TipoRelacaoPrecedenciaNegocial;
import java.time.LocalDate;
import java.util.List;

public record AditamentoContratualRequisicao(
        String identificacao,
        LocalDate dataDocumento,
        LocalDate dataAssinatura,
        LocalDate dataEfetiva,
        String justificativa,
        String referenciaSei,
        String novaDescricao,
        Long novoTipoContratacaoId,
        Long novoFornecedorId,
        String novoCnpjEmpresa,
        String novaRazaoSocialEmpresa,
        String novoNomeFantasiaEmpresa,
        LocalDate novaDataInicio,
        LocalDate novaDataFim,
        Integer novoEfetivoTotal,
        Boolean reducaoExcepcional,
        String fundamentoExcecao,
        Boolean confirmarImpacto,
        List<AditamentoComposicaoRequisicao> alteracoesComposicao,
        Long eventoPrecedenteId,
        TipoRelacaoPrecedenciaNegocial relacaoPrecedencia,
        Integer ordemDocumental,
        String tokenPrevia
) {}
