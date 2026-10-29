import { requisitar, extrairLista } from '../../core/http';
import { CONFIG } from '../../core/config';
import { requisitarTodasAsPaginas } from './paginacao';

export function validarOcorrencia(dados = {}) {
  const motivo = String(dados?.motivo || '').trim();
  const data = String(dados?.dataOcorrencia || '').trim();
  const diasInformados = dados?.dias != null && String(dados.dias).trim() !== '';
  const dias = diasInformados ? Number.parseInt(dados.dias, 10) : null;

  if (!motivo) return { valido: false, mensagem: 'Informe o motivo da ocorrência.' };
  if (!data) return { valido: false, mensagem: 'Informe a data da ocorrência.' };
  if (diasInformados && (!Number.isInteger(dias) || dias < 1)) {
    return { valido: false, mensagem: 'A quantidade de dias, quando informada, deve ser maior que 0.' };
  }
  return { valido: true, mensagem: null };
}

export function prepararCargaOcorrencia(empregadoId, dados = {}) {
  if (!empregadoId) throw new TypeError('empregadoId é obrigatório para registrar ocorrência.');
  const formulario = new FormData();
  formulario.append('empregadoId', empregadoId);
  formulario.append('motivo', String(dados?.motivo || '').trim().toUpperCase());
  formulario.append('dataOcorrencia', dados?.dataOcorrencia || '');

  const opcionais = {
    observacao: dados?.observacao,
    tipoRo: dados?.tipoRo,
    numeroSei: dados?.numeroSei,
  };
  Object.entries(opcionais).forEach(([chave, valor]) => {
    if (valor != null && String(valor).trim()) formulario.append(chave, String(valor).trim());
  });
  if (dados?.dias != null && String(dados.dias).trim()) {
    formulario.append('dias', Number.parseInt(dados.dias, 10));
  }
  for (const arquivo of Array.isArray(dados?.arquivos) ? dados.arquivos : []) {
    formulario.append('anexos', arquivo);
  }
  return formulario;
}

export function obterMensagemConfirmacaoOcorrencia(dados = {}) {
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
  buscarEmpregados: async (search, options = {}) => extrairLista(await requisitar({ method: 'GET', url: '/empregados', params: { search }, ...options })),
  listarPorEmpregado: async (empregadoId, options = {}) => requisitarTodasAsPaginas({
    url: '/ocorrencias', params: { empregadoId }, options,
  }),
  buscarPorData: async (dataIso, params = {}, options = {}) => requisitarTodasAsPaginas({
    url: '/ocorrencias', params: { data: dataIso, ...params }, options,
  }),
  criar: async (payload, options = {}) => requisitar({ method: 'POST', url: '/ocorrencias', data: payload, retries: 0, ...options }),
  excluir: async (id, options = {}) => requisitar({ method: 'DELETE', url: `/ocorrencias/${id}`, retries: 0, ...options }),
  obterUrlAnexo: (id) => `${CONFIG.API_URL.replace(/\/api\/?$/, '')}/api/ocorrencias/anexos/${id}`,
};
