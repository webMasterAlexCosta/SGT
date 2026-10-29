package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/**
 * Visão gerencial consolidada. Todas as métricas são calculadas no servidor,
 * sob o escopo autenticado, para evitar totais divergentes entre telas.
 */
public record PainelGestaoDTO(
        LocalDate referencia,
        LocalDateTime geradoEm,
        Escopo escopo,
        ResumoEfetivo efetivo,
        QuadroPessoal quadro,
        AnaliseCadastro analiseCadastro,
        List<AdmissaoMensal> admissoesMensais,
        SaudeContratual contratos,
        CoberturaFrequencia frequencia,
        AnalisePeriodo periodo,
        ComparativoPeriodo comparativoPeriodo,
        PressaoOperacional pressaoOperacional,
        IntegridadeDados integridade,
        HorizonteContratual horizonteContratual,
        List<DistribuicaoStatus> distribuicaoStatus,
        List<SituacaoQuadro> situacaoQuadro,
        List<PadraoSemanal> padraoSemanal,
        List<UnidadeGestao> unidades,
        List<SerieTemporal> evolucao,
        List<AlertaGestao> alertas,
        List<String> notasMetodologicas
) {
    public record Escopo(
            String nivel,
            String nome,
            Long superintendenciaId,
            Long gestaoLocalId,
            boolean permiteDetalhamentoOperacional
    ) {}

    public record ResumoEfetivo(
            long efetivoPrevisto,
            long efetivoVinculado,
            long empregadosAtivosCadastrados,
            long saldoContratual,
            double percentualOcupacao,
            long ativosSemContrato,
            long contratosVigentes
    ) {}

    public record QuadroPessoal(
            long empregadosCadastrados,
            long ativos,
            long inativosOuAfastados,
            long lotacoes,
            long cargos,
            long jornadas,
            double percentualAtivos
    ) {}

    /**
     * Leitura cadastral baseada exclusivamente em dados persistidos do empregado.
     * Não depende de lançamentos de frequência e, por isso, continua informativa
     * mesmo em bases recém-cadastradas.
     */
    public record AnaliseCadastro(
            long admitidosNoPeriodo,
            long admitidosNoMesReferencia,
            long admitidosUltimos12Meses,
            long comDataAdmissao,
            long semDataAdmissao,
            double percentualComDataAdmissao
    ) {}

    public record AdmissaoMensal(
            LocalDate mes,
            long quantidade
    ) {}

    public record SaudeContratual(
            long total,
            long vigentes,
            long vencidosMarcadosAtivos,
            long vencendoEm30Dias,
            long semDataFim,
            long semEfetivoPrevisto,
            long acimaDoEfetivoPrevisto,
            long abaixoDoEfetivoPrevisto
    ) {}

    /**
     * Cobertura baseada apenas nos empregados efetivamente esperados na data:
     * jornada com dia de trabalho, sem folga remunerada aprovada e sem evento de
     * calendário impeditivo aplicável ao empregado.
     */
    public record CoberturaFrequencia(
            LocalDate data,
            long empregadosAtivos,
            long empregadosEsperados,
            @JsonAlias("registrosLancados") long registrosEsperados,
            long pendentes,
            double percentualCobertura,
            long presencas,
            long ausencias,
            long ausenciasJustificadas,
            long faltasNaoJustificadas,
            long repousosTrabalhados,
            long trabalhosForaEscalaSemRt
    ) {}

    public record AnalisePeriodo(
            LocalDate inicio,
            LocalDate fim,
            int dias,
            long registrosOperacionais,
            long presencas,
            long presencasParciais,
            long atrasos,
            long faltasNaoJustificadas,
            long atestados,
            long suspensoes,
            long dsrPerdidos,
            long repousosTrabalhados,
            long empregadosComAusencia,
            long empregadosComAusenciaRecorrente,
            double taxaPresenca,
            double taxaAbsenteismo,
            LocalDateTime ultimaAtualizacao
    ) {}

    /**
     * Compara o período selecionado com uma janela imediatamente anterior de
     * igual duração. Variações de taxa são expressas em pontos percentuais.
     */
    public record ComparativoPeriodo(
            LocalDate inicioAnterior,
            LocalDate fimAnterior,
            long registrosOperacionaisAnterior,
            long ausenciasAnterior,
            long repousosTrabalhadosAnterior,
            double taxaPresencaAnterior,
            double taxaAbsenteismoAnterior,
            long variacaoRegistrosOperacionais,
            long variacaoAusencias,
            long variacaoRepousosTrabalhados,
            double variacaoTaxaPresencaPp,
            double variacaoTaxaAbsenteismoPp
    ) {}

    /**
     * RT representa trabalho em repouso/folga. A relação com ausências é apenas
     * um indicador observacional e não prova substituição individual.
     */
    public record PressaoOperacional(
            long repousosTrabalhados,
            long empregadosComRt,
            long ausenciasNoPeriodo,
            double rtPorCemAtivos,
            double relacaoRtAusencias
    ) {}

    public record IntegridadeDados(
            long totalInconsistencias,
            long empregadosAtivosSemContrato,
            long empregadosComMultiplosContratosVigentes,
            long empregadoContratoGestaoLocalDivergente,
            long empregadoLotacaoGestaoLocalDivergente,
            long empregadoJornadaGestaoLocalDivergente,
            long jornadasComCalendarioIncompleto,
            long empregadosAtivosSemLotacao,
            long empregadosAtivosSemJornada,
            long adicionaisRtSemFrequencia,
            long empregadosSemGestaoLocal,
            long contratosSemGestaoLocal
    ) {}

    public record HorizonteContratual(
            long vencidos,
            long ate30Dias,
            long de31A90Dias,
            long de91A180Dias,
            long acimaDe180Dias,
            long semDataFim
    ) {}

    public record DistribuicaoStatus(
            String status,
            long quantidade,
            double percentual
    ) {}

    public record SituacaoQuadro(
            String status,
            long quantidade,
            double percentual
    ) {}

    public record PadraoSemanal(
            int ordem,
            String dia,
            long registrosOperacionais,
            long ausencias,
            double taxaAbsenteismo
    ) {}

    public record UnidadeGestao(
            Long id,
            String tipo,
            String nome,
            String uf,
            Long efetivoPrevisto,
            long efetivoAtual,
            Long saldo,
            Double percentualOcupacao,
            long contratosVigentes,
            long alertasContratuais,
            long ausenciasNaReferencia,
            long empregadosEsperados,
            @JsonAlias("registrosLancados") long registrosEsperados,
            long pendentesFrequencia,
            double percentualCoberturaEscala
    ) {}

    public record SerieTemporal(
            LocalDate data,
            long registros,
            long presencas,
            long ausencias,
            long repousosTrabalhados
    ) {}

    public record AlertaGestao(
            String codigo,
            String severidade,
            String titulo,
            String mensagem,
            String rota
    ) {}
}
