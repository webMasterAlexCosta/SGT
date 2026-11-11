import type * as Input from '../../contracts/java-requests';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { extrairLista, requisitar, requisitarDownload } from '../../core/http.ts';
import { gerarChaveIdempotencia } from '../../core/http/idempotencia.ts';

const contratoBase = (contratoId: ResourceIdentifierInput) => { contratoId = exigirIdentificadorRota(contratoId); return `/contratos/${contratoId}`; };
const documentosBase = (contratoId: ResourceIdentifierInput) => { contratoId = exigirIdentificadorRota(contratoId); return `${contratoBase(contratoId)}/documentos`; };

const formDocumento = (metadados: Input.RequestCadastroDocumentoComprobatorioRequisicao | Input.RequestSubstituicaoDocumentoComprobatorioRequisicao, arquivo: (File | null)) => {
  const dados = { ...metadados };
  if (!dados.chaveIdempotencia) dados.chaveIdempotencia = gerarChaveIdempotencia('doc');
  const form = new FormData();
  form.append('metadados', new Blob([JSON.stringify(dados)], { type: 'application/json' }));
  if (arquivo) form.append('arquivo', arquivo);
  return form;
};

export const ServicoExcecoesContrato = Object.freeze({
  listarTransferencias: async (contratoId: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return extrairLista(await requisitar<Array<DTO.TransferenciaGestaoContratoDTO>>({ method: 'GET', url: `${contratoBase(contratoId)}/transferencias-gestao`, ...options })); },
  previaTransferenciaGestao: async (contratoId: ResourceIdentifierInput, dados: Input.RequestTransferenciaGestaoContratoRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return requisitar<DTO.PreviaCicloVidaContratoDTO>({ method: 'POST', url: `${contratoBase(contratoId)}/transferencias-gestao/previa`, data: dados, retries: 0, ...options }); },
  transferirGestao: async (contratoId: ResourceIdentifierInput, dados: Input.RequestTransferenciaGestaoContratoRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return requisitar<DTO.ContratoDTO>({ method: 'POST', url: `${contratoBase(contratoId)}/transferir-gestao`, data: dados, retries: 0, successMessage: 'Transferência excepcional registrada.', ...options }); },
  cancelarTransferenciaGestao: async (contratoId: ResourceIdentifierInput, transferenciaId: ResourceIdentifierInput, dados: Input.RequestCancelamentoTransferenciaGestaoRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); transferenciaId = exigirIdentificadorRota(transferenciaId); return requisitar<DTO.ContratoDTO>({ method: 'POST', url: `${contratoBase(contratoId)}/transferencias-gestao/${transferenciaId}/cancelar`, data: dados, retries: 0, successMessage: 'Transferência futura cancelada com preservação do histórico.', ...options }); },
  corrigirAnulacao: async (contratoId: ResourceIdentifierInput, dados: Input.RequestAcaoCicloVidaContratoDTO, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return requisitar<DTO.ContratoDTO>({ method: 'POST', url: `${contratoBase(contratoId)}/corrigir-anulacao`, data: dados, retries: 0, successMessage: 'Correção de anulação registrada.', ...options }); },

  listarCorrecoesIdentificador: async (contratoId: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return extrairLista(await requisitar<Array<DTO.CorrecaoIdentificadorContratoDTO>>({ method: 'GET', url: `${contratoBase(contratoId)}/correcoes-identificador`, ...options })); },
  corrigirIdentificador: async (contratoId: ResourceIdentifierInput, dados: Input.RequestCorrecaoIdentificadorContratoRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return requisitar<DTO.CorrecaoIdentificadorContratoDTO>({ method: 'POST', url: `${contratoBase(contratoId)}/correcoes-identificador`, data: { ...dados, chaveIdempotencia: dados?.chaveIdempotencia || gerarChaveIdempotencia('corr-id') }, successMessage: 'Identificador contratual corrigido com trilha histórica.', ...options }); },
  pesquisarHistoricoIdentificador: async (numero: (string), ano: (number), options: RequestOptions = {}) =>
    extrairLista(await requisitar<Array<DTO.CorrecaoIdentificadorContratoDTO>>({ method: 'GET', url: '/contratos/historico-identificador', params: { numero, ano }, ...options })),

  listarCorrecoesCnpj: async (contratoId: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return extrairLista(await requisitar<Array<DTO.CorrecaoCnpjContratoDTO>>({ method: 'GET', url: `${contratoBase(contratoId)}/correcoes-cnpj`, ...options })); },
  corrigirCnpj: async (contratoId: ResourceIdentifierInput, dados: Input.RequestCorrecaoCnpjContratoRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return requisitar<DTO.CorrecaoCnpjContratoDTO>({ method: 'POST', url: `${contratoBase(contratoId)}/correcoes-cnpj`, data: dados, retries: 0, successMessage: 'Correção material de CNPJ registrada.', ...options }); },

  listarDocumentos: async (contratoId: ResourceIdentifierInput, finalidade: (string | undefined), options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return extrairLista(await requisitar<Array<DTO.DocumentoComprobatorioDTO>>({ method: 'GET', url: documentosBase(contratoId), params: finalidade ? { finalidade } : undefined, ...options })); },
  buscarDocumento: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, finalidade: (string | undefined), options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); return requisitar<DTO.DocumentoComprobatorioDTO>({ method: 'GET', url: `${documentosBase(contratoId)}/${documentoId}`, params: finalidade ? { finalidade } : undefined, ...options }); },
  listarEventosDocumento: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, finalidade: (string | undefined), options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); return extrairLista(await requisitar<Array<DTO.EventoDocumentoComprobatorioDTO>>({ method: 'GET', url: `${documentosBase(contratoId)}/${documentoId}/eventos`, params: finalidade ? { finalidade } : undefined, ...options })); },
  baixarDocumento: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, finalidade: (string | undefined), options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); return requisitarDownload({ method: 'GET', url: `${documentosBase(contratoId)}/${documentoId}/arquivo`, params: finalidade ? { finalidade } : undefined, ...options }, `documento-${documentoId}`); },
  criarDocumento: async (contratoId: ResourceIdentifierInput, metadados: Input.RequestCadastroDocumentoComprobatorioRequisicao, arquivo: (File | null), options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return requisitar<DTO.DocumentoComprobatorioDTO>({ method: 'POST', url: documentosBase(contratoId), data: formDocumento(metadados, arquivo), successMessage: 'Documento registrado.', ...options }); },
  retificarSei: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, dados: Input.RequestRetificacaoSeiDocumentoRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); return requisitar<DTO.DocumentoComprobatorioDTO>({ method: 'POST', url: `${documentosBase(contratoId)}/${documentoId}/retificar-sei`, data: { ...dados, chaveIdempotencia: dados?.chaveIdempotencia || gerarChaveIdempotencia('sei') }, successMessage: 'Referência SEI retificada em nova versão.', ...options }); },
  substituirDocumento: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, metadados: Input.RequestSubstituicaoDocumentoComprobatorioRequisicao, arquivo: (File | null), options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); return requisitar<DTO.DocumentoComprobatorioDTO>({ method: 'POST', url: `${documentosBase(contratoId)}/${documentoId}/substituir`, data: formDocumento(metadados, arquivo), successMessage: 'Nova versão documental registrada.', ...options }); },
  invalidarDocumento: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, dados: Input.RequestAcaoDocumentoComprobatorioRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); return requisitar<DTO.DocumentoComprobatorioDTO>({ method: 'POST', url: `${documentosBase(contratoId)}/${documentoId}/invalidar`, data: dados, successMessage: 'Documento invalidado sem apagar o histórico.', ...options }); },
  descartarRascunhoDocumento: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); return requisitar<null>({ method: 'DELETE', url: `${documentosBase(contratoId)}/${documentoId}`, successMessage: 'Rascunho documental descartado.', ...options }); },
  listarPermissoesDocumento: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); return extrairLista(await requisitar<Array<DTO.PermissaoDocumentoRestritoDTO>>({ method: 'GET', url: `${documentosBase(contratoId)}/${documentoId}/permissoes`, ...options })); },
  concederPermissaoDocumento: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, dados: Input.RequestConcessaoAcessoDocumentoRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); return requisitar<DTO.PermissaoDocumentoRestritoDTO>({ method: 'POST', url: `${documentosBase(contratoId)}/${documentoId}/permissoes`, data: dados, successMessage: 'Permissão de documento restrito concedida.', ...options }); },
  revogarPermissaoDocumento: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, permissaoId: ResourceIdentifierInput, dados: Input.RequestAcaoDocumentoComprobatorioRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); permissaoId = exigirIdentificadorRota(permissaoId); return requisitar<DTO.PermissaoDocumentoRestritoDTO>({ method: 'POST', url: `${documentosBase(contratoId)}/${documentoId}/permissoes/${permissaoId}/revogar`, data: dados, successMessage: 'Permissão de documento restrito revogada.', ...options }); },
  listarAcessosDocumento: async (contratoId: ResourceIdentifierInput, documentoId: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); documentoId = exigirIdentificadorRota(documentoId); return extrairLista(await requisitar<Array<DTO.AcessoDocumentoRestritoDTO>>({ method: 'GET', url: `${documentosBase(contratoId)}/${documentoId}/acessos`, ...options })); },
});
