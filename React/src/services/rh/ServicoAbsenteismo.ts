import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { CONFIG } from "../../core/config.ts";
import { extrairLista } from "../../core/http.ts";
import { ServicoFrequencia } from "./ServicoFrequencia.ts";
import { normalizarAvisoRevisaoRelatorio } from "./revisaoRelatorio.ts";
import { obterDataNoFusoNegocio } from "../../core/formatters/dataHoraNegocio.ts";

export const INDICADORES_ABSENTEISMO_VAZIOS = Object.freeze({
  faltas: 0,
  atestados: 0,
  dsr: 0,
  totalPessoas: 0,
  taxaGeral: 0,
  diasTrabalhaveis: 0,
  alertasDisrupcao: 0,
  maioresTaxas: [],
});

export function validarFiltrosAbsenteismo({ mes, ano, contratoId, referencia = new Date() }: { mes?: string | number; ano?: string | number; contratoId?: string | number; referencia?: Date } = {}) {
  const dataAtual = obterDataNoFusoNegocio(referencia.getTime());
  const mesAtual = Number(dataAtual?.slice(5, 7));
  const anoAtual = Number(dataAtual?.slice(0, 4));

  if (Number(ano) > anoAtual || (Number(ano) === anoAtual && Number(mes) > mesAtual)) {
    return { valido: false, codigo: "COMPETENCIA_FUTURA", mensagem: "Não é possível gerar relatórios para competências futuras." };
  }
  return { valido: true, codigo: null, mensagem: null };
}

const normalizarItemAbsenteismo = (item: Partial<DTO.RelatorioAbsenteismoDTO> & { id?: number | null; dsr?: number | null } = {}) => {
  const faltas = Number(item?.faltas || 0);
  const atestados = Number(item?.atestados || 0);
  const dsr = Number(item?.dsrPerdido ?? item?.dsr ?? 0);
  const diasTrabalhaveis = Number(item?.diasTrabalhaveis || 0);
  const taxaIndividual = Number(item?.taxaIndividual || 0);
  const disrupcao = Number(item?.disrupcao || 0);

  return {
    id: item?.empregadoId ?? item?.id,
    nome: item?.nome || "-",
    cargo: item?.cargo || "-",
    lotacao: item?.lotacao || "-",
    turno: item?.turno || "-",
    faltas,
    atestados,
    dsr,
    total: faltas + atestados + dsr,
    taxaIndividual,
    disrupcao,
    diasTrabalhaveis,
  };
};

export function consolidarAbsenteismo(resultados: Partial<DTO.RelatorioAbsenteismoDTO>[] = [], limiteDisrupcao = CONFIG.ABSENTEISMO_DISRUPCAO_THRESHOLD) {
  const lista = extrairLista(resultados);
  if (lista.length === 0) {
    return { dados: [], indicadores: { ...INDICADORES_ABSENTEISMO_VAZIOS } };
  }

  let somaFaltas = 0;
  let somaAtestados = 0;
  let somaDsr = 0;
  let totalDiasTrabalhaveis = 0;
  let alertasDisrupcao = 0;

  const dados = lista.map((item) => {
    const normalizado = normalizarItemAbsenteismo(item);
    somaFaltas += normalizado.faltas;
    somaAtestados += normalizado.atestados;
    somaDsr += normalizado.dsr;
    totalDiasTrabalhaveis += normalizado.diasTrabalhaveis;
    if (normalizado.disrupcao >= limiteDisrupcao) alertasDisrupcao += 1;
    return normalizado;
  }).sort((a, b) => {
    if (b.taxaIndividual !== a.taxaIndividual) return b.taxaIndividual - a.taxaIndividual;
    return String(a.nome || "").localeCompare(String(b.nome || ""), "pt-BR");
  });

  const taxaGeral = totalDiasTrabalhaveis > 0
    ? Number((((somaFaltas + somaAtestados) / totalDiasTrabalhaveis) * 100).toFixed(2))
    : 0;

  return {
    dados,
    indicadores: {
      faltas: somaFaltas,
      atestados: somaAtestados,
      dsr: somaDsr,
      totalPessoas: lista.length,
      taxaGeral,
      diasTrabalhaveis: totalDiasTrabalhaveis,
      alertasDisrupcao,
    },
  };
}

export const ServicoAbsenteismo = {
  carregarFiltros: async (options: RequestOptions = {}) => {
    const filtros = await ServicoFrequencia.carregarFiltrosRelatorio(options);
    return {
      contratos: Array.isArray(filtros?.contratos) ? filtros.contratos : [],
      lotacoes: Array.isArray(filtros?.lotacoes) ? filtros.lotacoes : [],
      turnos: Array.isArray(filtros?.turnos) ? filtros.turnos : [],
    };
  },

  validarFiltros: validarFiltrosAbsenteismo,

  gerarRelatorio: async (params: QueryParams, options: RequestOptions = {}) => {
    const pagina = await ServicoFrequencia.gerarRelatorioAbsenteismo(params, options);
    return {
      // A pagina oficial ja vem calculada e ordenada pelo Java. O navegador
      // apenas normaliza os campos para apresentacao; nao recalcula indicadores
      // a partir da pagina parcial.
      dados: extrairLista(pagina?.registros).map(normalizarItemAbsenteismo),
      revisao: normalizarAvisoRevisaoRelatorio(pagina?.revisao),
      paginacao: {
        pagina: Number(pagina?.pagina || 0),
        tamanho: Number(pagina?.tamanho || params?.size || 100),
        totalPaginas: Number(pagina?.totalPaginas || 0),
        totalColaboradores: Number(pagina?.totalColaboradores || 0),
        primeira: Boolean(pagina?.primeira),
        ultima: Boolean(pagina?.ultima),
      },
    };
  },

  gerarResumo: async (params: QueryParams, options: RequestOptions = {}) => {
    const resumo = await ServicoFrequencia.gerarResumoAbsenteismo(params, options);
    return {
      faltas: Number(resumo?.faltas || 0),
      atestados: Number(resumo?.atestados || 0),
      dsr: Number(resumo?.dsrPerdido || 0),
      totalPessoas: Number(resumo?.totalColaboradores || 0),
      taxaGeral: Number(resumo?.taxaGeral || 0),
      diasTrabalhaveis: Number(resumo?.diasTrabalhaveis || 0),
      alertasDisrupcao: Number(resumo?.alertasDisrupcao || 0),
      maioresTaxas: extrairLista(resumo?.maioresTaxas).map(normalizarItemAbsenteismo),
      revisao: normalizarAvisoRevisaoRelatorio(resumo?.revisao),
    };
  },

  obterUrlExportacao: (params: QueryParams = {}, formato = 'XLSX') =>
    ServicoFrequencia.obterUrlExportacaoAbsenteismo(params, formato),
};
