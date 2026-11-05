import { requisitar, extrairLista } from '../../core/http';
import { CONFIG } from '../../core/config';
import { prepararCargaCalendarioEvento } from './cargaCalendarioEvento';
import { requisitarTodasAsPaginas } from './paginacao';
import { prepararPaginaVisual, recortarPaginaVisual, agruparContextosPorColaborador } from './paginacaoRelatorios.js';

export { prepararCargaCalendarioEvento } from './cargaCalendarioEvento';

const criarUrlDownload = (caminho, params = {}) => {
  const base = String(CONFIG.API_URL || '/api').replace(/\/$/, '');
  const consulta = new URLSearchParams();
  Object.entries(params).forEach(([chave, valor]) => {
    if (valor === undefined || valor === null || valor === '') return;
    if (Array.isArray(valor)) valor.forEach((item) => consulta.append(chave, String(item)));
    else consulta.append(chave, String(valor));
  });
  return `${base}${caminho}?${consulta.toString()}`;
};

export const iniciarDownloadNativo = (url) => {
  const link = document.createElement('a');
  link.href = url;
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  link.remove();
};

const consultarRelatorioPaginado = async (url, params = {}, options = {}, agruparContextos = false) => {
  const consulta = prepararPaginaVisual(params, 25, 500);
  const resposta = await requisitar({ method: 'GET', url, params: consulta.params, ...options });
  if (!Array.isArray(resposta?.registros)) return resposta;
  const unidades = agruparContextos ? agruparContextosPorColaborador(resposta.registros) : resposta.registros;
  const visual = recortarPaginaVisual(unidades, {
    pagina: resposta.pagina, tamanho: resposta.tamanho, total: resposta.totalColaboradores,
  }, consulta);
  return {
    ...resposta,
    registros: agruparContextos ? visual.itens.flatMap(grupo => grupo.linhas) : visual.itens,
    pagina: visual.pagina,
    tamanho: visual.tamanho,
    totalColaboradores: visual.total,
    totalPaginas: visual.totalPaginas,
    primeira: visual.primeira,
    ultima: visual.ultima,
  };
};

export const ServicoFrequencia = {
  carregarFiltrosRelatorio: async (options = {}) => requisitar({ method: 'GET', url: '/relatorios/filtros', ...options }),
  buscarDoDia: async (dataIso, params = {}, options = {}) => requisitarTodasAsPaginas({
    url: '/frequencias', params: { data: dataIso, ...params }, options, otimizarContagem: true,
  }),
  registrar: async (payload, options = {}) => requisitar({ method: 'POST', url: '/frequencias', data: payload, retries: 0, ...options }),
  registrarLote: async (payloads, options = {}) => requisitar({ method: 'POST', url: '/frequencias/lote', data: payloads, retries: 0, ...options }),
  listarEventos: async (id, options = {}) => requisitar({ method: 'GET', url: `/frequencias/${id}/eventos`, ...options }),
  criarFechamento: async (payload, options = {}) => requisitar({
    method: 'POST', url: '/frequencias/fechamentos', data: payload, retries: 0, ...options,
  }),
  buscarFechamento: async (id, options = {}) => requisitar({
    method: 'GET', url: `/frequencias/fechamentos/${id}`, ...options,
  }),
  buscarFechamentoAtual: async (params, options = {}) => requisitar({
    method: 'GET', url: '/frequencias/fechamentos/atual', params, ...options,
  }),
  cancelarFechamento: async (id, options = {}) => requisitar({
    method: 'POST', url: `/frequencias/fechamentos/${id}/cancelamento`, retries: 0, ...options,
  }),
  reprocessarFechamento: async (id, options = {}) => requisitar({
    method: 'POST', url: `/frequencias/fechamentos/${id}/reprocessamento`, retries: 0, ...options,
  }),
  buscarMensal: async (mes, ano, params = {}, options = {}) => requisitarTodasAsPaginas({
    url: '/frequencias/mensal', params: { mes, ano, ...params }, options, otimizarContagem: true,
  }),
  buscarIntervalo: async (inicioISO, fimISO, params = {}, options = {}) => requisitarTodasAsPaginas({
    url: '/frequencias/intervalo', params: { inicio: inicioISO, fim: fimISO, ...params }, options, otimizarContagem: true,
  }),

  gerarRelatorioConsolidadoCompleto: async (params, options = {}) => requisitar({ method: 'GET', url: '/relatorios/frequencia-consolidada', params, ...options }),
  gerarRelatorioConsolidado: (params, options = {}) => consultarRelatorioPaginado('/relatorios/frequencia-consolidada/paginada', params, options, true),
  gerarRelatorioAbsenteismoCompleto: async (params, options = {}) => requisitar({ method: 'GET', url: '/relatorios/absenteismo', params, ...options }),
  gerarRelatorioAbsenteismo: (params, options = {}) => consultarRelatorioPaginado('/relatorios/absenteismo/paginado', params, options),
  // O resumo nacional percorre a base completa no servidor. Não aplica o timeout
  // HTTP global de 15s; a tela permanece responsiva porque a Promise é tratada
  // em segundo plano e a tabela paginada já foi entregue.
  gerarResumoAbsenteismo: async (params, options = {}) => requisitar({
    method: 'GET', url: '/relatorios/absenteismo/resumo', params, timeout: 0, ...options,
  }),
  obterUrlExportacaoFrequencia: (params = {}, formato = 'XLSX') => criarUrlDownload('/relatorios/frequencia-consolidada/exportacao', { ...params, formato }),
  obterUrlExportacaoAbsenteismo: (params = {}, formato = 'XLSX') => criarUrlDownload('/relatorios/absenteismo/exportacao', { ...params, formato }),
  gerarRelatorioHorasTrabalhadas: async (params, options = {}) => requisitar({ method: 'GET', url: '/relatorios/horas-trabalhadas', params, ...options }),
};

export const ServicoCalendarioEvento = {
  buscarPorData: async (dataISO, opcoes = {}) => { const { params, ...resto } = opcoes; return extrairLista(await requisitar({ method: 'GET', url: '/calendario-eventos', params: { data: dataISO, ...params }, ...resto })); },
  listarPorPeriodo: async (inicioISO, fimISO, opcoes = {}) => { const { params, ...resto } = opcoes; return extrairLista(await requisitar({ method: 'GET', url: '/calendario-eventos', params: { inicio: inicioISO, fim: fimISO, ...params }, ...resto })); },
  criar: async (dados, opcoes = {}) => requisitar({ method: 'POST', url: '/calendario-eventos', data: prepararCargaCalendarioEvento(dados), retries: 0, ...opcoes }),
  atualizar: async (id, dados, opcoes = {}) => requisitar({ method: 'PUT', url: `/calendario-eventos/${id}`, data: prepararCargaCalendarioEvento(dados), retries: 0, ...opcoes }),
  excluir: async (id, opcoes = {}) => requisitar({ method: 'DELETE', url: `/calendario-eventos/${id}`, retries: 0, ...opcoes }),
};

export const ServicoExcecaoFrequencia = {
  buscarPorData: async (dataISO, params = {}, options = {}) => requisitarTodasAsPaginas({
    url: '/frequencias-excecoes', params: { data: dataISO, ...params }, options,
  }),
  criar: async (payload, options = {}) => requisitar({ method: 'POST', url: '/frequencias-excecoes', data: payload, retries: 0, ...options }),
  atualizar: async (id, payload, options = {}) => requisitar({ method: 'PUT', url: `/frequencias-excecoes/${id}`, data: payload, retries: 0, ...options }),
  excluir: async (id, options = {}) => requisitar({ method: 'DELETE', url: `/frequencias-excecoes/${id}`, retries: 0, ...options }),
};

export const ServicoAdicionalFrequencia = {
  buscarPorData: async (dataISO, params = {}, options = {}) => requisitarTodasAsPaginas({
    url: '/frequencias-adicionais', params: { data: dataISO, ...params }, options,
  }),
  buscarIntervalo: async (inicioISO, fimISO, params = {}, options = {}) => requisitarTodasAsPaginas({
    url: '/frequencias-adicionais', params: { inicio: inicioISO, fim: fimISO, ...params }, options,
  }),
  criar: async (payload, options = {}) => requisitar({ method: 'POST', url: '/frequencias-adicionais', data: payload, retries: 0, ...options }),
  atualizar: async (id, payload, options = {}) => requisitar({ method: 'PUT', url: `/frequencias-adicionais/${id}`, data: payload, retries: 0, ...options }),
  excluir: async (id, options = {}) => requisitar({ method: 'DELETE', url: `/frequencias-adicionais/${id}`, retries: 0, ...options }),
};
