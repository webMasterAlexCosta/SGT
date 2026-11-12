package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Contrato independente do painel legado. Null representa indicador indisponível. */
public record PainelAnaliticoDTO(
        LocalDate referencia, Instant geradoEm, String versaoMetodologia,
        PainelGestaoDTO.Escopo escopo, Intervalo intervalo, Selecao selecao, Filtros filtros,
        Totais resumoPeriodo, Totais comparativo, Variacoes variacoes, Retrato referenciaDiaria,
        List<Ponto> evolucao, Heatmap heatmap, List<Unidade> unidades,
        int paginaUnidades, long totalUnidades, int tamanhoPagina,
        List<Meta> metas, List<PainelGestaoDTO.AlertaGestao> alertas,
        List<String> indisponibilidades, List<String> metodologia, Indicadores indicadores) {
    public record Tempestividade(long dentro, long fora, long indeterminado, long emRevisao) {}
    public record BaseCobertura(long vinculados, long esperados, long lancados, long pendentes, long semProva, long comAdmissao) {
        public static BaseCobertura vazia() { return new BaseCobertura(0,0,0,0,0,0); }
    }
    public record PontoPendencias(Faixa periodo, BaseCobertura atual, BaseCobertura anterior) {}
    public record LinhaPendencias(Long unidadeId, String nome, List<BaseCobertura> celulas) {}
    public record DiagnosticoContrato(Long id, String nome, String prestador, long previsto, long vinculado, long esperados, long lancados, long pendentes, LocalDate vigenciaFim) {}
    public record Indicadores(Tempestividade tempestividade, BaseCobertura cobertura, List<PontoPendencias> evolucaoPendencias,
            List<LinhaPendencias> heatmapPendencias, List<DiagnosticoContrato> contratos) {}
    public enum TipoUnidade { SUPERINTENDENCIA, GESTAO_LOCAL, LOTACAO }
    public record Opcao(Long id, String nome) {}
    public record Selecao(Long contratoId, TipoUnidade tipoUnidade, Long unidadeId, boolean comparar, Long prestadorId) {}
    public record Filtros(List<Opcao> contratos, List<Opcao> unidades, TipoUnidade tipoUnidade, List<Opcao> prestadores) {}
    public record Intervalo(String granularidade, LocalDate inicio, LocalDate fim, boolean parcial,
                            LocalDate anteriorInicio, LocalDate anteriorFim) {}
    public record Faixa(LocalDate inicio, LocalDate fim, String rotulo, boolean parcial) {}
    public record Totais(long registros, long presencas, long ausencias) {
        public static Totais vazio() { return new Totais(0,0,0); }
    }
    public record Retrato(long efetivoPrevisto, long efetivoAlocado, long registrosLancados,
                          long pendentesEntreAlocados, Double coberturaAlocados, Double ocupacao, long contratosVigentes) {}
    public record Comparacao(long diferenca, Double variacaoPercentual) {}
    public record Variacoes(Comparacao registros, Comparacao presencas, Comparacao ausencias) {}
    public record Ponto(Faixa periodo, Faixa periodoAnterior, Totais atual, Totais anterior) {}
    public record Unidade(Long id, String nome, Totais totais) {}
    public record LinhaHeatmap(Long unidadeId, String nome, List<Totais> celulas) {}
    public record Heatmap(List<Faixa> colunas, List<LinhaHeatmap> linhas) {}
    public record Meta(String codigo, String nome, Double valor, Double meta, String fonte) {}
    /** Identificação mínima do fato, sem nome, CPF ou identificação do trabalhador. */
    public record Registro(Long id, LocalDate data, String status, Integer versao, Long contratoId, Long unidadeId) {}
    public record PaginaRegistros(List<Registro> registros, int pagina, long total, int tamanho) {}
}
