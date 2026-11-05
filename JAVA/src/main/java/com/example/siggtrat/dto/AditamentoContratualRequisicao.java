package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.TipoRelacaoPrecedenciaNegocial;
import java.time.LocalDate;
import java.time.DayOfWeek;
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
        String tokenPrevia,
        Boolean novaMatriculaVinculoObrigatoria,
        DayOfWeek novoDiaSemanaReduzido,
        Integer novasHorasDiaReduzido,
        Boolean restaurarPoliticaMatriculaDoTipo,
        Boolean removerConfiguracaoDiaReduzido
) {
    public AditamentoContratualRequisicao(String identificacao, LocalDate dataDocumento, LocalDate dataAssinatura,
            LocalDate dataEfetiva, String justificativa, String referenciaSei, String novaDescricao,
            Long novoTipoContratacaoId, Long novoFornecedorId, String novoCnpjEmpresa, String novaRazaoSocialEmpresa,
            String novoNomeFantasiaEmpresa, LocalDate novaDataInicio, LocalDate novaDataFim, Integer novoEfetivoTotal,
            Boolean reducaoExcepcional, String fundamentoExcecao, Boolean confirmarImpacto,
            List<AditamentoComposicaoRequisicao> alteracoesComposicao, Long eventoPrecedenteId,
            TipoRelacaoPrecedenciaNegocial relacaoPrecedencia, Integer ordemDocumental, String tokenPrevia) {
        this(identificacao, dataDocumento, dataAssinatura, dataEfetiva, justificativa, referenciaSei, novaDescricao,
                novoTipoContratacaoId, novoFornecedorId, novoCnpjEmpresa, novaRazaoSocialEmpresa, novoNomeFantasiaEmpresa,
                novaDataInicio, novaDataFim, novoEfetivoTotal, reducaoExcepcional, fundamentoExcecao, confirmarImpacto,
                alteracoesComposicao, eventoPrecedenteId, relacaoPrecedencia, ordemDocumental, tokenPrevia,
                null, null, null, null, null);
    }

    public AditamentoContratualRequisicao(String identificacao, LocalDate dataDocumento, LocalDate dataAssinatura,
            LocalDate dataEfetiva, String justificativa, String referenciaSei, String novaDescricao,
            Long novoTipoContratacaoId, Long novoFornecedorId, String novoCnpjEmpresa, String novaRazaoSocialEmpresa,
            String novoNomeFantasiaEmpresa, LocalDate novaDataInicio, LocalDate novaDataFim, Integer novoEfetivoTotal,
            Boolean reducaoExcepcional, String fundamentoExcecao, Boolean confirmarImpacto,
            List<AditamentoComposicaoRequisicao> alteracoesComposicao, Long eventoPrecedenteId,
            TipoRelacaoPrecedenciaNegocial relacaoPrecedencia, Integer ordemDocumental, String tokenPrevia,
            Boolean novaMatriculaVinculoObrigatoria, DayOfWeek novoDiaSemanaReduzido, Integer novasHorasDiaReduzido) {
        this(identificacao, dataDocumento, dataAssinatura, dataEfetiva, justificativa, referenciaSei, novaDescricao,
                novoTipoContratacaoId, novoFornecedorId, novoCnpjEmpresa, novaRazaoSocialEmpresa, novoNomeFantasiaEmpresa,
                novaDataInicio, novaDataFim, novoEfetivoTotal, reducaoExcepcional, fundamentoExcecao, confirmarImpacto,
                alteracoesComposicao, eventoPrecedenteId, relacaoPrecedencia, ordemDocumental, tokenPrevia,
                novaMatriculaVinculoObrigatoria, novoDiaSemanaReduzido, novasHorasDiaReduzido, null, null);
    }
}
