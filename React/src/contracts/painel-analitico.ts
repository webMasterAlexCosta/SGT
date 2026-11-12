/** Contrato JSON de PainelAnaliticoDTO; independente dos DTOs legados. */
export type GranularidadePainel = 'DIARIO' | 'SEMANAL' | 'MENSAL' | 'TRIMESTRAL' | 'ANUAL';
export type TipoUnidadePainel = 'SUPERINTENDENCIA' | 'GESTAO_LOCAL' | 'LOTACAO';
export interface OpcaoAnalitico { id: number; nome: string; }
export interface TotaisAnalitico { registros: number; presencas: number; ausencias: number; }
export interface FaixaAnalitico { inicio: string; fim: string; rotulo: string; parcial: boolean; }
export interface ParametrosAnalitico {
  granularidade: GranularidadePainel; referencia: string; contratoId?: number; prestadorId?: number;
  tipoUnidade?: TipoUnidadePainel; unidadeId?: number; comparar: boolean; paginaUnidades: number;
}
export interface ParametrosRegistrosAnalitico {
  referencia: string; inicio: string; fim: string; contratoId?: number; prestadorId?: number; tipoUnidade: TipoUnidadePainel;
  unidadeId?: number; metrica: MetricaRegistroAnalitico; pagina: number;
}
export interface PaginaRegistrosAnalitico {
  registros: { id: number; data: string; status: string; versao: number; contratoId: number | null; unidadeId: number | null }[];
  pagina: number; total: number; tamanho: number;
}
export interface PainelAnaliticoDTO {
  indicadores: IndicadoresAnaliticos;
  referencia: string; geradoEm: string; versaoMetodologia: string;
  escopo: { nivel: string; nome: string; superintendenciaId: number | null; gestaoLocalId: number | null; permiteDetalhamentoOperacional: boolean };
  intervalo: { granularidade: GranularidadePainel; inicio: string; fim: string; parcial: boolean; anteriorInicio: string | null; anteriorFim: string | null };
  selecao: { contratoId: number | null; prestadorId: number | null; tipoUnidade: TipoUnidadePainel; unidadeId: number | null; comparar: boolean };
  filtros: { prestadores: OpcaoAnalitico[]; contratos: OpcaoAnalitico[]; unidades: OpcaoAnalitico[]; tipoUnidade: TipoUnidadePainel };
  resumoPeriodo: TotaisAnalitico; comparativo: TotaisAnalitico | null;
  variacoes: Record<keyof TotaisAnalitico, { diferenca: number; variacaoPercentual: number | null }> | null;
  referenciaDiaria: { efetivoPrevisto: number; efetivoAlocado: number; registrosLancados: number; pendentesEntreAlocados: number; coberturaAlocados: number | null; ocupacao: number | null; contratosVigentes: number };
  evolucao: { periodo: FaixaAnalitico; periodoAnterior: FaixaAnalitico | null; atual: TotaisAnalitico; anterior: TotaisAnalitico | null }[];
  heatmap: { colunas: FaixaAnalitico[]; linhas: { unidadeId: number; nome: string; celulas: TotaisAnalitico[] }[] };
  unidades: { id: number; nome: string; totais: TotaisAnalitico }[];
  paginaUnidades: number; totalUnidades: number; tamanhoPagina: number;
  metas: { codigo: string; nome: string; valor: number | null; meta: number | null; fonte: string }[];
  alertas: { codigo: string; severidade: string; titulo: string; mensagem: string; rota: string | null }[];
  indisponibilidades: string[]; metodologia: string[];
}

export interface BaseCoberturaAnalitica { vinculados: number; esperados: number; lancados: number; pendentes: number; semProva: number; comAdmissao: number; }
export interface IndicadoresAnaliticos {
  tempestividade: { dentro: number; fora: number; indeterminado: number; emRevisao: number };
  cobertura: BaseCoberturaAnalitica;
  evolucaoPendencias: { periodo: FaixaAnalitico; atual: BaseCoberturaAnalitica; anterior: BaseCoberturaAnalitica | null }[];
  heatmapPendencias: { unidadeId: number; nome: string; celulas: BaseCoberturaAnalitica[] }[];
  contratos: { id: number; nome: string; prestador: string | null; previsto: number; vinculado: number; esperados: number; lancados: number; pendentes: number; vigenciaFim: string | null }[];
}

export type MetricaRegistroAnalitico = keyof TotaisAnalitico | "dentro" | "fora" | "revisao" | "indeterminado";
