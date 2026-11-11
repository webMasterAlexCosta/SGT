import type * as Input from '../../contracts/java-requests';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar, extrairLista } from '../../core/http';
import { CONFIG } from '../../core/config';
import { prepararCargaCalendarioEvento } from './cargaCalendarioEvento';
import { requisitarTodasAsPaginas } from './paginacao';
import { prepararPaginaVisual, recortarPaginaVisual, agruparContextosPorColaborador } from './paginacaoRelatorios.ts';

export { prepararCargaCalendarioEvento } from './cargaCalendarioEvento';

const criarUrlDownload = (caminho: (string), params: QueryParams = {}) => {
  const base = String(CONFIG.API_URL || '/api').replace(/\/$/, '');
  const consulta = new URLSearchParams();
  Object.entries(params).forEach(([chave, valor]) => {
    if (valor === undefined || valor === null || valor === '') return;
    if (Array.isArray(valor)) valor.forEach((item: unknown) => consulta.append(chave, String(item)));
    else consulta.append(chave, String(valor));
  });
  return `${base}${caminho}?${consulta.toString()}`;
};

export const iniciarDownloadNativo = (url: (string)) => {
  const link = document.createElement('a');
  link.href = url;
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  link.remove();
};

interface ReportPage<T> { registros: T[] | null; pagina: number; tamanho: number; totalColaboradores: number; totalPaginas: number; primeira: boolean; ultima: boolean }
const consultarRelatorioPaginado = async <T extends { empregadoId?: number | null }, R extends ReportPage<T>>(url: string, params: QueryParams = {}, options: RequestOptions = {}, agruparContextos = false) => {
  const consulta = prepararPaginaVisual(params, 25, 500);
  const resposta = await requisitar<R>({ method: 'GET', url, params: consulta.params, ...options });
  if (!Array.isArray(resposta?.registros)) return resposta;
  const meta = { pagina: resposta.pagina, tamanho: resposta.tamanho, total: resposta.totalColaboradores };
  const grupos = agruparContextos ? recortarPaginaVisual(agruparContextosPorColaborador(resposta.registros), meta, consulta) : null;
  const visual = grupos ? { ...grupos, itens: grupos.itens.flatMap(grupo => grupo.linhas) } : recortarPaginaVisual(resposta.registros, meta, consulta);
  return {
    ...resposta,
    registros: visual.itens,
    pagina: visual.pagina, tamanho: visual.tamanho,
    totalColaboradores: visual.total, totalPaginas: visual.totalPaginas,
    primeira: visual.primeira, ultima: visual.ultima,
  };
};

export const ServicoFrequencia = {
  carregarFiltrosRelatorio: async (options: RequestOptions = {}) => requisitar<DTO.RelatorioFiltrosDTO>({ method: 'GET', url: '/relatorios/filtros', ...options }),
  buscarDoDia: async (dataIso: (string | null), params: QueryParams = {}, options: RequestOptions = {}) => requisitarTodasAsPaginas<DTO.FrequenciaDTO>({
    url: '/frequencias', params: { data: dataIso, ...params }, options, otimizarContagem: true,
  }),
  registrar: async (payload: Input.RequestFrequenciaDTO, options: RequestOptions = {}) => requisitar<DTO.FrequenciaDTO>({ method: 'POST', url: '/frequencias', data: payload, retries: 0, ...options }),
  registrarLote: async (payloads: Input.RequestFrequenciaDTO[], options: RequestOptions = {}) => requisitar<Array<DTO.FrequenciaDTO>>({ method: 'POST', url: '/frequencias/lote', data: payloads, retries: 0, ...options }),
  listarEventos: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<Array<DTO.EventoFrequenciaDTO>>({ method: 'GET', url: `/frequencias/${id}/eventos`, ...options }); },
  criarFechamento: async (payload: Input.RequestRequisicaoCriacaoFechamentoFrequencia, options: RequestOptions = {}) => requisitar<DTO.FechamentoFrequenciaDTO>({
    method: 'POST', url: '/frequencias/fechamentos', data: payload, retries: 0, ...options,
  }),
  buscarFechamento: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.FechamentoFrequenciaDTO>({
    method: 'GET', url: `/frequencias/fechamentos/${id}`, ...options,
  }); },
  buscarFechamentoAtual: async (params: QueryParams, options: RequestOptions = {}) => requisitar<DTO.FechamentoFrequenciaDTO>({
    method: 'GET', url: '/frequencias/fechamentos/atual', params, ...options,
  }),
  cancelarFechamento: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.FechamentoFrequenciaDTO>({
    method: 'POST', url: `/frequencias/fechamentos/${id}/cancelamento`, retries: 0, ...options,
  }); },
  reprocessarFechamento: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.FechamentoFrequenciaDTO>({
    method: 'POST', url: `/frequencias/fechamentos/${id}/reprocessamento`, retries: 0, ...options,
  }); },
  buscarMensal: async (mes: string | number, ano: string | number, params: QueryParams = {}, options: RequestOptions = {}) => requisitarTodasAsPaginas<DTO.FrequenciaDTO>({
    url: '/frequencias/mensal', params: { mes, ano, ...params }, options, otimizarContagem: true,
  }),
  buscarIntervalo: async (inicioISO: string, fimISO: string, params: QueryParams = {}, options: RequestOptions = {}) => requisitarTodasAsPaginas<DTO.FrequenciaDTO>({
    url: '/frequencias/intervalo', params: { inicio: inicioISO, fim: fimISO, ...params }, options, otimizarContagem: true,
  }),

  gerarRelatorioConsolidadoCompleto: async (params: QueryParams, options: RequestOptions = {}) => requisitar<DTO.RelatorioFrequenciaConsolidadoDTO>({ method: 'GET', url: '/relatorios/frequencia-consolidada', params, ...options }),
  gerarRelatorioConsolidado: (params: QueryParams, options: RequestOptions = {}) => consultarRelatorioPaginado<DTO.RelatorioFrequenciaConsolidadoDTOLinha, DTO.RelatorioFrequenciaPaginaDTO>('/relatorios/frequencia-consolidada/paginada', params, options, true),
  gerarRelatorioAbsenteismoCompleto: async (params: QueryParams, options: RequestOptions = {}) => requisitar<Array<DTO.RelatorioAbsenteismoDTO>>({ method: 'GET', url: '/relatorios/absenteismo', params, ...options }),
  gerarRelatorioAbsenteismo: (params: QueryParams, options: RequestOptions = {}) => consultarRelatorioPaginado<DTO.RelatorioAbsenteismoDTO, DTO.RelatorioAbsenteismoPaginaDTO>('/relatorios/absenteismo/paginado', params, options),
  // O resumo nacional percorre a base completa no servidor. Não aplica o timeout
  // HTTP global de 15s; a tela permanece responsiva porque a Promise é tratada
  // em segundo plano e a tabela paginada já foi entregue.
  gerarResumoAbsenteismo: async (params: QueryParams, options: RequestOptions = {}) => requisitar<DTO.RelatorioAbsenteismoResumoDTO>({
    method: 'GET', url: '/relatorios/absenteismo/resumo', params, timeout: 0, ...options,
  }),
  obterUrlExportacaoFrequencia: (params: QueryParams = {}, formato = 'XLSX') => criarUrlDownload('/relatorios/frequencia-consolidada/exportacao', { ...params, formato }),
  obterUrlExportacaoAbsenteismo: (params: QueryParams = {}, formato = 'XLSX') => criarUrlDownload('/relatorios/absenteismo/exportacao', { ...params, formato }),
  gerarRelatorioHorasTrabalhadas: async (params: QueryParams, options: RequestOptions = {}) => requisitar<Array<DTO.RelatorioHorasDTO>>({ method: 'GET', url: '/relatorios/horas-trabalhadas', params, ...options }),
};

export const ServicoCalendarioEvento = {
  buscarPorData: async (dataISO: (string | null), opcoes: RequestOptions = {}) => { const { params, ...resto } = opcoes; return extrairLista(await requisitar<Array<DTO.CalendarioEventoDTO>>({ method: 'GET', url: '/calendario-eventos', params: { data: dataISO, ...params }, ...resto })); },
  listarPorPeriodo: async (inicioISO: (string), fimISO: (string), opcoes: RequestOptions = {}) => { const { params, ...resto } = opcoes; return extrairLista(await requisitar<Array<DTO.CalendarioEventoDTO>>({ method: 'GET', url: '/calendario-eventos', params: { inicio: inicioISO, fim: fimISO, ...params }, ...resto })); },
  criar: async (dados: ({ data: string; tipo: string; descricao: string | null; abrangencia: string; superintendenciaId: number | null; gestaoLocalId: number | null; municipio: string | null; lotacaoId: number | null; ativo: boolean; }), opcoes: RequestOptions = {}) => requisitar<DTO.CalendarioEventoDTO>({ method: 'POST', url: '/calendario-eventos', data: prepararCargaCalendarioEvento(dados), retries: 0, ...opcoes }),
  atualizar: async (id: ResourceIdentifierInput, dados: ({ data: string; tipo: string; descricao: string | null; abrangencia: string; superintendenciaId: number | null; gestaoLocalId: number | null; municipio: string | null; lotacaoId: number | null; ativo: boolean; }), opcoes: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.CalendarioEventoDTO>({ method: 'PUT', url: `/calendario-eventos/${id}`, data: prepararCargaCalendarioEvento(dados), retries: 0, ...opcoes }); },
  excluir: async (id: ResourceIdentifierInput, opcoes: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<null>({ method: 'DELETE', url: `/calendario-eventos/${id}`, retries: 0, ...opcoes }); },
};

export const ServicoExcecaoFrequencia = {
  buscarPorData: async (dataISO: (string | null) | (string), params: QueryParams = {}, options: RequestOptions = {}) => requisitarTodasAsPaginas<DTO.FrequenciaExcecaoDTO>({
    url: '/frequencias-excecoes', params: { data: dataISO, ...params }, options,
  }),
  criar: async (payload: Input.RequestFrequenciaExcecaoDTO, options: RequestOptions = {}) => requisitar<DTO.FrequenciaExcecaoDTO>({ method: 'POST', url: '/frequencias-excecoes', data: payload, retries: 0, ...options }),
  atualizar: async (id: ResourceIdentifierInput, payload: Input.RequestFrequenciaExcecaoDTO, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.FrequenciaExcecaoDTO>({ method: 'PUT', url: `/frequencias-excecoes/${id}`, data: payload, retries: 0, ...options }); },
  excluir: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<null>({ method: 'DELETE', url: `/frequencias-excecoes/${id}`, retries: 0, ...options }); },
};

export const ServicoAdicionalFrequencia = {
  buscarPorData: async (dataISO: (string | null) | (string), params: QueryParams = {}, options: RequestOptions = {}) => requisitarTodasAsPaginas<DTO.FrequenciaAdicionalDTO>({
    url: '/frequencias-adicionais', params: { data: dataISO, ...params }, options,
  }),
  buscarIntervalo: async (inicioISO: string, fimISO: string, params: QueryParams = {}, options: RequestOptions = {}) => requisitarTodasAsPaginas<DTO.FrequenciaAdicionalDTO>({
    url: '/frequencias-adicionais', params: { inicio: inicioISO, fim: fimISO, ...params }, options,
  }),
  criar: async (payload: Input.RequestFrequenciaAdicionalDTO, options: RequestOptions = {}) => requisitar<DTO.FrequenciaAdicionalDTO>({ method: 'POST', url: '/frequencias-adicionais', data: payload, retries: 0, ...options }),
  atualizar: async (id: ResourceIdentifierInput, payload: Input.RequestFrequenciaAdicionalDTO, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.FrequenciaAdicionalDTO>({ method: 'PUT', url: `/frequencias-adicionais/${id}`, data: payload, retries: 0, ...options }); },
  excluir: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<null>({ method: 'DELETE', url: `/frequencias-adicionais/${id}`, retries: 0, ...options }); },
};
