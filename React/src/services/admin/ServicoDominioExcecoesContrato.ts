import type * as DTO from '../../contracts/java';
import type { FormDraft } from '../../contracts/formularios';
import type { HttpError } from '../../core/http/types';
export const TIPOS_FATO_DOCUMENTAL: readonly DTO.TipoFatoDocumental[] = Object.freeze(['CONTRATO_ORIGINAL','ADITAMENTO','REMANEJAMENTO','TRANSFERENCIA_GESTAO','CORRECAO_IDENTIFICADOR','CORRECAO_CNPJ','ENCERRAMENTO_ANTECIPADO','ANULACAO','VINCULO','ALOCACAO','OUTRO']);
export const CLASSIFICACOES_DOCUMENTO: readonly DTO.ClassificacaoAcessoDocumento[] = Object.freeze(['NEGOCIAL','PESSOAL_RESTRITO']);
export const EXTENSOES_DOCUMENTO = Object.freeze(['pdf','png','jpg','jpeg','doc','docx','xls','xlsx']);

const numeroOuNulo = (v: unknown) => v === '' || v == null ? null : Number(v);
const texto = (v: unknown) => String(v ?? '').trim();

export const prepararMetadadosDocumento = (form: FormDraft<DTO.CadastroDocumentoComprobatorioRequisicao>) => ({
  tipoFato: form.tipoFato || 'CONTRATO_ORIGINAL',
  fatoId: numeroOuNulo(form.fatoId),
  fatoChave: texto(form.fatoChave) || null,
  titulo: texto(form.titulo),
  descricao: texto(form.descricao) || null,
  referenciaSei: texto(form.referenciaSei) || null,
  classificacaoAcesso: form.classificacaoAcesso || 'NEGOCIAL',
  dataEfetiva: form.dataEfetiva || null,
  chaveIdempotencia: form.chaveIdempotencia || undefined,
});

export const validarDocumentoLocal = (form: FormDraft<DTO.CadastroDocumentoComprobatorioRequisicao>, arquivo: (File | null)) => {
  if (!texto(form?.titulo)) return 'Informe o título do documento.';
  if (!form?.tipoFato) return 'Informe o tipo de fato documental.';
  if (!form?.classificacaoAcesso) return 'Informe a classificação de acesso.';
  if (!arquivo && !texto(form?.referenciaSei)) return 'Informe um arquivo, uma referência SEI ou ambos.';
  if (arquivo?.name) {
    const ext = arquivo.name.split('.').pop()?.toLowerCase();
    if (!EXTENSOES_DOCUMENTO.includes(ext ?? '')) return 'Formato de arquivo não permitido pelo contrato atual.';
  }
  return null;
};

export const mensagemErroDocumento = (erro: Partial<HttpError> & { response?: { status?: number | null } }) => {
  const status = Number(erro?.status || erro?.response?.status || 0);
  if (status === 413) return 'Arquivo excede o limite individual configurado no servidor.';
  if (status === 415) return 'Formato, MIME ou assinatura binária do arquivo não é aceito pelo servidor.';
  if (status === 409) return 'O estado documental mudou ou a operação conflita com a versão atual. Recarregue os dados.';
  return erro?.message || 'Não foi possível concluir a operação documental.';
};

export const prepararCorrecaoIdentificador = (form: FormDraft<DTO.CorrecaoIdentificadorContratoRequisicao>) => ({
  novoNumero: texto(form.novoNumero),
  novoAno: numeroOuNulo(form.novoAno),
  dataEfetiva: form.dataEfetiva || null,
  dataAto: form.dataAto || null,
  motivo: texto(form.motivo),
  referenciaSei: texto(form.referenciaSei),
  autorizacaoAdministrativa: texto(form.autorizacaoAdministrativa),
  eventoPrecedenteId: numeroOuNulo(form.eventoPrecedenteId),
  relacaoPrecedencia: form.relacaoPrecedencia || null,
  ordemDocumental: numeroOuNulo(form.ordemDocumental),
});

export const validarCorrecaoIdentificador = (form: FormDraft<DTO.CorrecaoIdentificadorContratoRequisicao>) => {
  if (!texto(form?.novoNumero) || !form?.novoAno || !form?.dataEfetiva || !form?.dataAto || !texto(form?.motivo) || !texto(form?.referenciaSei) || !texto(form?.autorizacaoAdministrativa)) return 'Preencha número/ano, datas, motivo, SEI e autorização administrativa.';
  return null;
};

export const RELACOES_PRECEDENCIA_CNPJ = Object.freeze(['SUCEDE', 'RETIFICA', 'CANCELA', 'SUBSTITUI', 'MANTEM_COEXISTE']);

export const prepararCorrecaoCnpj = (form: FormDraft<DTO.CorrecaoCnpjContratoRequisicao>) => ({
  cnpjCorrigido: texto(form.cnpjCorrigido).replace(/\D/g, ''),
  dataEfetiva: form.dataEfetiva || null,
  dataAto: form.dataAto || null,
  motivo: texto(form.motivo),
  referenciaSei: texto(form.referenciaSei),
  chaveIdempotencia: texto(form.chaveIdempotencia),
  eventoPrecedenteId: numeroOuNulo(form.eventoPrecedenteId),
  relacaoPrecedencia: form.relacaoPrecedencia || null,
  ordemDocumental: numeroOuNulo(form.ordemDocumental),
});

export const validarCorrecaoCnpj = (form: FormDraft<DTO.CorrecaoCnpjContratoRequisicao>) => {
  if (texto(form?.cnpjCorrigido).replace(/\D/g, '').length !== 14) return 'O CNPJ corrigido deve conter 14 dígitos.';
  if (!form?.dataEfetiva || !form?.dataAto || !texto(form?.motivo) || !texto(form?.referenciaSei)) return 'Informe as datas efetiva e do ato, o motivo e a referência SEI da correção.';
  if (texto(form.motivo).length > 1000 || texto(form.referenciaSei).length > 120) return 'O motivo permite até 1000 caracteres e a referência SEI até 120.';
  if (Boolean(form.eventoPrecedenteId) !== Boolean(form.relacaoPrecedencia)) return 'Informe o evento precedente e a relação de precedência juntos.';
  if (form.eventoPrecedenteId && (!Number.isSafeInteger(Number(form.eventoPrecedenteId)) || Number(form.eventoPrecedenteId) < 1)) return 'O evento precedente deve ser um ID positivo.';
  if (form.relacaoPrecedencia && !RELACOES_PRECEDENCIA_CNPJ.includes(form.relacaoPrecedencia)) return 'Informe uma relação de precedência válida.';
  if (form.ordemDocumental !== '' && form.ordemDocumental != null && (!Number.isInteger(Number(form.ordemDocumental)) || Number(form.ordemDocumental) < -2147483648 || Number(form.ordemDocumental) > 2147483647)) return 'Informe uma ordem documental inteira válida.';
  return null;
};

export const prepararCorrecaoAnulacao = (form: FormDraft<DTO.AcaoCicloVidaContratoDTO>) => ({
  dataEfetiva: form.dataEfetiva || null,
  dataAto: form.dataAto || null,
  motivo: texto(form.motivo),
  referenciaSei: texto(form.referenciaSei),
  eventoPrecedenteId: numeroOuNulo(form.eventoPrecedenteId),
  relacaoPrecedencia: 'CANCELA' as const,
  ordemDocumental: numeroOuNulo(form.ordemDocumental),
  tokenPrevia: texto(form.tokenPrevia) || null,
  confirmarImpacto: form.confirmarImpacto === true,
});

export const prepararTransferenciaGestao = (form: FormDraft<DTO.TransferenciaGestaoContratoRequisicao>) => ({
  dataEfetiva: form.dataEfetiva || null,
  dataAto: form.dataAto || null,
  gestaoLocalDestinoId: numeroOuNulo(form.gestaoLocalDestinoId),
  tipoContratacaoDestinoId: numeroOuNulo(form.tipoContratacaoDestinoId),
  motivo: texto(form.motivo),
  referenciaSei: texto(form.referenciaSei),
  autorizacaoAdministrativa: texto(form.autorizacaoAdministrativa),
  mapeamentos: (form.mapeamentos || []).map((m) => ({
    composicaoOrigemId: numeroOuNulo(m.composicaoOrigemId),
    lotacaoOrigemId: numeroOuNulo(m.lotacaoOrigemId),
    jornadaOrigemId: numeroOuNulo(m.jornadaOrigemId),
    cargoOrigemId: numeroOuNulo(m.cargoOrigemId),
    lotacaoDestinoId: numeroOuNulo(m.lotacaoDestinoId),
    jornadaDestinoId: numeroOuNulo(m.jornadaDestinoId),
    cargoDestinoId: numeroOuNulo(m.cargoDestinoId),
  })),
  eventoPrecedenteId: numeroOuNulo(form.eventoPrecedenteId),
  relacaoPrecedencia: form.relacaoPrecedencia || null,
  ordemDocumental: numeroOuNulo(form.ordemDocumental),
  tokenPrevia: texto(form.tokenPrevia) || null,
  confirmarImpacto: form.confirmarImpacto === true,
  conferenciaDocumentalConfirmada: form.conferenciaDocumentalConfirmada === true,
  versaoConferenciaDocumental: texto(form.versaoConferenciaDocumental) || null,
});

export const validarTransferenciaGestao = (form: FormDraft<DTO.TransferenciaGestaoContratoRequisicao>) => {
  if (!form?.dataEfetiva || !form?.gestaoLocalDestinoId || !texto(form?.motivo) || !texto(form?.referenciaSei) || !texto(form?.autorizacaoAdministrativa)) return 'Informe data efetiva, Gestão de destino, motivo, SEI e autorização administrativa.';
  const idValido = (valor: unknown) => valor !== '' && valor != null && Number.isSafeInteger(Number(valor)) && Number(valor) > 0;
  if (!idValido(form.gestaoLocalDestinoId)) return 'Informe um ID válido de Gestão Local de destino.';
  if (!idValido(form.tipoContratacaoDestinoId)) return 'Informe o tipo de contratação da Gestão Local de destino.';
  if (!(form.mapeamentos || []).length) return 'Mapeie todas as composições operacionais para a Gestão de destino.';
  const chavesOrigem = new Set<string>();
  for (const mapa of form.mapeamentos || []) {
    if (!mapa || (['lotacaoOrigemId', 'jornadaOrigemId', 'cargoOrigemId', 'lotacaoDestinoId', 'jornadaDestinoId', 'cargoDestinoId'] as const).some((campo) => !idValido(mapa[campo]))) return 'Preencha os IDs de Lotação, Turno e Cargo de origem e destino de cada mapa.';
    if (mapa.composicaoOrigemId !== '' && mapa.composicaoOrigemId != null && !idValido(mapa.composicaoOrigemId)) return 'O ID da composição de origem deve ser positivo quando informado.';
    const chave = [mapa.lotacaoOrigemId, mapa.jornadaOrigemId, mapa.cargoOrigemId].map(Number).join(':');
    if (chavesOrigem.has(chave)) return 'Há mapa de origem duplicado. Cada recorte deve ser mapeado uma única vez.';
    chavesOrigem.add(chave);
  }
  if ((form.eventoPrecedenteId && !form.relacaoPrecedencia) || (!form.eventoPrecedenteId && form.relacaoPrecedencia)) return 'Evento precedente e relação de precedência devem ser informados juntos.';
  if (form.eventoPrecedenteId && !idValido(form.eventoPrecedenteId)) return 'O evento precedente deve ser um ID positivo.';
  if (form.relacaoPrecedencia && !['SUCEDE', 'RETIFICA', 'CANCELA', 'SUBSTITUI', 'MANTEM_COEXISTE'].includes(form.relacaoPrecedencia)) return 'Informe uma relação de precedência válida.';
  return null;
};

export const podeDescartarDocumento = (doc: Partial<DTO.DocumentoComprobatorioDTO> | null | undefined) => doc?.estado === 'RASCUNHO' && !doc?.referenciaSei;
export const ehDocumentoRestrito = (doc: Partial<DTO.DocumentoComprobatorioDTO> | null | undefined) => doc?.classificacaoAcesso === 'PESSOAL_RESTRITO';
