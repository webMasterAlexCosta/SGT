export const TIPOS_FATO_DOCUMENTAL = Object.freeze(['CONTRATO_ORIGINAL','ADITAMENTO','REMANEJAMENTO','TRANSFERENCIA_GESTAO','CORRECAO_IDENTIFICADOR','ENCERRAMENTO_ANTECIPADO','ANULACAO','VINCULO','ALOCACAO','OUTRO']);
export const CLASSIFICACOES_DOCUMENTO = Object.freeze(['NEGOCIAL','PESSOAL_RESTRITO']);
export const EXTENSOES_DOCUMENTO = Object.freeze(['pdf','png','jpg','jpeg','doc','docx','xls','xlsx']);

const numeroOuNulo = (v) => v === '' || v == null ? null : Number(v);
const texto = (v) => String(v ?? '').trim();

export const prepararMetadadosDocumento = (form) => ({
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

export const validarDocumentoLocal = (form, arquivo) => {
  if (!texto(form?.titulo)) return 'Informe o título do documento.';
  if (!form?.tipoFato) return 'Informe o tipo de fato documental.';
  if (!form?.classificacaoAcesso) return 'Informe a classificação de acesso.';
  if (!arquivo && !texto(form?.referenciaSei)) return 'Informe um arquivo, uma referência SEI ou ambos.';
  if (arquivo?.name) {
    const ext = arquivo.name.split('.').pop()?.toLowerCase();
    if (!EXTENSOES_DOCUMENTO.includes(ext)) return 'Formato de arquivo não permitido pelo contrato atual.';
  }
  return null;
};

export const mensagemErroDocumento = (erro) => {
  const status = Number(erro?.status || erro?.response?.status || 0);
  if (status === 413) return 'Arquivo excede o limite individual configurado no servidor.';
  if (status === 415) return 'Formato, MIME ou assinatura binária do arquivo não é aceito pelo servidor.';
  if (status === 409) return 'O estado documental mudou ou a operação conflita com a versão atual. Recarregue os dados.';
  return erro?.message || 'Não foi possível concluir a operação documental.';
};

export const prepararCorrecaoIdentificador = (form) => ({
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

export const validarCorrecaoIdentificador = (form) => {
  if (!texto(form?.novoNumero) || !form?.novoAno || !form?.dataEfetiva || !form?.dataAto || !texto(form?.motivo) || !texto(form?.referenciaSei) || !texto(form?.autorizacaoAdministrativa)) return 'Preencha número/ano, datas, motivo, SEI e autorização administrativa.';
  return null;
};

export const prepararCorrecaoAnulacao = (form) => ({
  dataEfetiva: form.dataEfetiva || null,
  dataAto: form.dataAto || null,
  motivo: texto(form.motivo),
  referenciaSei: texto(form.referenciaSei),
  eventoPrecedenteId: numeroOuNulo(form.eventoPrecedenteId),
  relacaoPrecedencia: 'CANCELA',
  ordemDocumental: numeroOuNulo(form.ordemDocumental),
});

export const prepararTransferenciaGestao = (form) => ({
  dataEfetiva: form.dataEfetiva || null,
  dataAto: form.dataAto || null,
  gestaoLocalDestinoId: numeroOuNulo(form.gestaoLocalDestinoId),
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
});

export const validarTransferenciaGestao = (form) => {
  if (!form?.dataEfetiva || !form?.gestaoLocalDestinoId || !texto(form?.motivo) || !texto(form?.referenciaSei) || !texto(form?.autorizacaoAdministrativa)) return 'Informe data efetiva, Gestão de destino, motivo, SEI e autorização administrativa.';
  if (!(form.mapeamentos || []).length) return 'Mapeie todas as composições operacionais para a Gestão de destino.';
  if ((form.eventoPrecedenteId && !form.relacaoPrecedencia) || (!form.eventoPrecedenteId && form.relacaoPrecedencia)) return 'Evento precedente e relação de precedência devem ser informados juntos.';
  return null;
};

export const podeDescartarDocumento = (doc) => doc?.estado === 'RASCUNHO' && !doc?.referenciaSei;
export const ehDocumentoRestrito = (doc) => doc?.classificacaoAcesso === 'PESSOAL_RESTRITO';
