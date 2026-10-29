import { extrairLista, requisitar, requisitarDownload } from '../../core/http.js';
import { gerarChaveIdempotencia } from '../../core/http/idempotencia.js';

const contratoBase = (contratoId) => `/contratos/${contratoId}`;
const documentosBase = (contratoId) => `${contratoBase(contratoId)}/documentos`;

const formDocumento = (metadados, arquivo) => {
  const dados = { ...metadados };
  if (!dados.chaveIdempotencia) dados.chaveIdempotencia = gerarChaveIdempotencia('doc');
  const form = new FormData();
  form.append('metadados', new Blob([JSON.stringify(dados)], { type: 'application/json' }));
  if (arquivo) form.append('arquivo', arquivo);
  return form;
};

export const ServicoExcecoesContrato = Object.freeze({
  listarTransferencias: async (contratoId, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${contratoBase(contratoId)}/transferencias-gestao`, ...options })),
  transferirGestao: async (contratoId, dados, options = {}) =>
    requisitar({ method: 'POST', url: `${contratoBase(contratoId)}/transferir-gestao`, data: dados, successMessage: 'Transferência excepcional registrada.', ...options }),
  corrigirAnulacao: async (contratoId, dados, options = {}) =>
    requisitar({ method: 'POST', url: `${contratoBase(contratoId)}/corrigir-anulacao`, data: dados, successMessage: 'Correção de anulação registrada.', ...options }),

  listarCorrecoesIdentificador: async (contratoId, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${contratoBase(contratoId)}/correcoes-identificador`, ...options })),
  corrigirIdentificador: async (contratoId, dados, options = {}) =>
    requisitar({ method: 'POST', url: `${contratoBase(contratoId)}/correcoes-identificador`, data: { ...dados, chaveIdempotencia: dados?.chaveIdempotencia || gerarChaveIdempotencia('corr-id') }, successMessage: 'Identificador contratual corrigido com trilha histórica.', ...options }),
  pesquisarHistoricoIdentificador: async (numero, ano, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: '/contratos/historico-identificador', params: { numero, ano }, ...options })),

  listarDocumentos: async (contratoId, finalidade, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: documentosBase(contratoId), params: finalidade ? { finalidade } : undefined, ...options })),
  buscarDocumento: async (contratoId, documentoId, finalidade, options = {}) =>
    requisitar({ method: 'GET', url: `${documentosBase(contratoId)}/${documentoId}`, params: finalidade ? { finalidade } : undefined, ...options }),
  listarEventosDocumento: async (contratoId, documentoId, finalidade, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${documentosBase(contratoId)}/${documentoId}/eventos`, params: finalidade ? { finalidade } : undefined, ...options })),
  baixarDocumento: async (contratoId, documentoId, finalidade, options = {}) =>
    requisitarDownload({ method: 'GET', url: `${documentosBase(contratoId)}/${documentoId}/arquivo`, params: finalidade ? { finalidade } : undefined, ...options }, `documento-${documentoId}`),
  criarDocumento: async (contratoId, metadados, arquivo, options = {}) =>
    requisitar({ method: 'POST', url: documentosBase(contratoId), data: formDocumento(metadados, arquivo), successMessage: 'Documento registrado.', ...options }),
  retificarSei: async (contratoId, documentoId, dados, options = {}) =>
    requisitar({ method: 'POST', url: `${documentosBase(contratoId)}/${documentoId}/retificar-sei`, data: { ...dados, chaveIdempotencia: dados?.chaveIdempotencia || gerarChaveIdempotencia('sei') }, successMessage: 'Referência SEI retificada em nova versão.', ...options }),
  substituirDocumento: async (contratoId, documentoId, metadados, arquivo, options = {}) =>
    requisitar({ method: 'POST', url: `${documentosBase(contratoId)}/${documentoId}/substituir`, data: formDocumento(metadados, arquivo), successMessage: 'Nova versão documental registrada.', ...options }),
  invalidarDocumento: async (contratoId, documentoId, dados, options = {}) =>
    requisitar({ method: 'POST', url: `${documentosBase(contratoId)}/${documentoId}/invalidar`, data: dados, successMessage: 'Documento invalidado sem apagar o histórico.', ...options }),
  descartarRascunhoDocumento: async (contratoId, documentoId, options = {}) =>
    requisitar({ method: 'DELETE', url: `${documentosBase(contratoId)}/${documentoId}`, successMessage: 'Rascunho documental descartado.', ...options }),
  listarPermissoesDocumento: async (contratoId, documentoId, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${documentosBase(contratoId)}/${documentoId}/permissoes`, ...options })),
  concederPermissaoDocumento: async (contratoId, documentoId, dados, options = {}) =>
    requisitar({ method: 'POST', url: `${documentosBase(contratoId)}/${documentoId}/permissoes`, data: dados, successMessage: 'Permissão de documento restrito concedida.', ...options }),
  revogarPermissaoDocumento: async (contratoId, documentoId, permissaoId, dados, options = {}) =>
    requisitar({ method: 'POST', url: `${documentosBase(contratoId)}/${documentoId}/permissoes/${permissaoId}/revogar`, data: dados, successMessage: 'Permissão de documento restrito revogada.', ...options }),
  listarAcessosDocumento: async (contratoId, documentoId, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${documentosBase(contratoId)}/${documentoId}/acessos`, ...options })),
});
