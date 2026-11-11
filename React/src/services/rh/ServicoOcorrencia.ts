import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar, requisitarDownload, extrairLista } from '../../core/http';
import { CONFIG } from '../../core/config';
import { requisitarTodasAsPaginas } from './paginacao';

export function validarOcorrencia(dados: { motivo?: unknown; dataOcorrencia?: unknown; dias?: unknown } = {}) {
  const motivo = String(dados?.motivo || '').trim();
  const data = String(dados?.dataOcorrencia || '').trim();
  const diasInformados = dados?.dias != null && String(dados.dias).trim() !== '';
  const dias = diasInformados ? Number.parseInt(String(dados.dias), 10) : null;

  if (!motivo) return { valido: false, mensagem: 'Informe o motivo da ocorrência.' };
  if (!data) return { valido: false, mensagem: 'Informe a data da ocorrência.' };
  if (diasInformados && (!Number.isInteger(dias) || Number(dias) < 1)) {
    return { valido: false, mensagem: 'A quantidade de dias, quando informada, deve ser maior que 0.' };
  }
  return { valido: true, mensagem: null };
}

export function prepararCargaOcorrencia(empregadoId: ResourceIdentifierInput, dados: { motivo?: unknown; dataOcorrencia?: unknown; observacao?: unknown; tipoRo?: unknown; numeroSei?: unknown; dias?: unknown; arquivos?: Blob[] } = {}) {
  if (!empregadoId) throw new TypeError('empregadoId é obrigatório para registrar ocorrência.');
  const formulario = new FormData();
  formulario.append('empregadoId', String(empregadoId));
  formulario.append('motivo', String(dados?.motivo || '').trim().toUpperCase());
  formulario.append('dataOcorrencia', String(dados?.dataOcorrencia || ''));

  const opcionais = {
    observacao: dados?.observacao,
    tipoRo: dados?.tipoRo,
    numeroSei: dados?.numeroSei,
  };
  Object.entries(opcionais).forEach(([chave, valor]) => {
    if (valor != null && String(valor).trim()) formulario.append(chave, String(valor).trim());
  });
  if (dados?.dias != null && String(dados.dias).trim()) {
    formulario.append('dias', String(Number.parseInt(String(dados.dias), 10)));
  }
  for (const arquivo of Array.isArray(dados?.arquivos) ? dados.arquivos : []) {
    formulario.append('anexos', arquivo);
  }
  return formulario;
}

export function obterMensagemConfirmacaoOcorrencia(dados: { motivo?: unknown; dias?: unknown; dataOcorrencia?: unknown } = {}) {
  const motivo = String(dados?.motivo || '').trim().toUpperCase() || 'OCORRÊNCIA';
  const periodo = dados?.dias ? ` por ${dados.dias} dia(s)` : '';
  return `Registrar ${motivo}${periodo} na data ${dados?.dataOcorrencia || 'informada'}? O Java aplicará os efeitos de frequência previstos para este motivo.`;
}

export const ServicoOcorrencia = {
  validar: validarOcorrencia,
  prepararCarga: prepararCargaOcorrencia,
  obterMensagemConfirmacao: obterMensagemConfirmacaoOcorrencia,
  // Compatibilidade temporária: o contrato Java de ocorrência ainda exige empregadoId.
  // Remover esta consulta quando o backend expuser Pessoa/Vínculo como identidade do comando.
  buscarEmpregados: async (search: (string), options: RequestOptions = {}, contextoOperacional?: { dataRef: string; lotacaoId: string | number; turnoId: string | number }) => {
    if (contextoOperacional === undefined) {
      return extrairLista(await requisitar<Page<DTO.EmpregadoDTO>>({ method: 'GET', url: '/empregados', params: { search }, ...options }));
    }
    const { dataRef, lotacaoId, turnoId } = contextoOperacional || {};
    if (!dataRef || !Number.isSafeInteger(Number(lotacaoId)) || Number(lotacaoId) <= 0
      || !Number.isSafeInteger(Number(turnoId)) || Number(turnoId) <= 0) {
      throw new TypeError('Data, lotação e turno são obrigatórios para a busca operacional.');
    }
    // Esta exceção pública é autorizada ao Operacional no Java. A resposta
    // contém o Empregado real exigido por Ocorrências, sem inferir Pessoa/Vínculo.
    return requisitarTodasAsPaginas<DTO.EmpregadoFrequenciaDTO>({
      url: '/empregados/filtrados-frequencia',
      params: { dataRef, lotacaoId: Number(lotacaoId), turnoId: Number(turnoId), searchTerm: String(search || '').trim() },
      options,
    });
  },
  listarPorEmpregado: async (empregadoId: number | string | null, options: RequestOptions = {}) => requisitarTodasAsPaginas<DTO.OcorrenciaResumoDTO>({
    url: '/ocorrencias', params: { empregadoId }, options,
  }),
  buscarPorData: async (dataIso: (string | null), params: QueryParams = {}, options: RequestOptions = {}) => requisitarTodasAsPaginas<DTO.OcorrenciaResumoDTO>({
    url: '/ocorrencias', params: { data: dataIso, ...params }, options,
  }),
  criar: async (payload: (FormData), options: RequestOptions = {}) => requisitar<DTO.OcorrenciaResumoDTO>({ method: 'POST', url: '/ocorrencias', data: payload, retries: 0, ...options }),
  excluir: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<null>({ method: 'DELETE', url: `/ocorrencias/${id}`, retries: 0, ...options }); },
  consultarHistorico: (id: ResourceIdentifierInput, finalidade: string, options: RequestOptions = {}) => requisitar<DTO.OcorrenciaHistoricoDTO>({
    method: 'GET', url: `/ocorrencias/${encodeURIComponent(exigirIdentificadorRota(id))}`,
    params: { finalidade: finalidade.trim() }, ...options,
  }),
  baixarAnexo: async (id: ResourceIdentifierInput, finalidade: (string), options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitarDownload({
    method: 'GET',
    url: `/ocorrencias/anexos/${id}`,
    params: String(finalidade || '').trim() ? { finalidade: String(finalidade).trim() } : undefined,
    ...options,
  }, `anexo-${id}`); },
  obterUrlAnexo: (id: ResourceIdentifierInput, finalidade: string | null | undefined) => {
  id = exigirIdentificadorRota(id);

    const base = `${CONFIG.API_URL.replace(/\/api\/?$/, '')}/api/ocorrencias/anexos/${id}`;
    const motivo = String(finalidade || '').trim();
    return motivo ? `${base}?${new URLSearchParams({ finalidade: motivo })}` : base;
  },
};
