const inteiroOuNulo = (valor) => {
  if (valor === '' || valor == null) return null;
  const numero = Number(valor);
  return Number.isFinite(numero) ? Math.trunc(numero) : null;
};

const textoOuNulo = (valor) => {
  const texto = String(valor ?? '').trim();
  return texto || null;
};

export const criarChaveCliente = () => `linha-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;

export const criarLinhaAditamento = (origem = {}) => ({
  chaveCliente: origem.chaveCliente || criarChaveCliente(),
  composicaoId: origem.composicaoId ?? '',
  lotacaoId: origem.lotacaoId ?? '',
  jornadaId: origem.jornadaId ?? '',
  cargoId: origem.cargoId ?? '',
  quantidadeResultante: origem.quantidadeResultante ?? origem.efetivoPrevisto ?? 0,
});

export const criarLinhaRemanejamento = (origem = {}) => ({
  chaveCliente: origem.chaveCliente || criarChaveCliente(),
  composicaoId: origem.composicaoId ?? '',
  lotacaoId: origem.lotacaoId ?? '',
  jornadaId: origem.jornadaId ?? '',
  cargoId: origem.cargoId ?? '',
  variacao: origem.variacao ?? 0,
});

export const prepararComposicao = (dados = {}) => ({
  id: dados.id || undefined,
  lotacaoId: inteiroOuNulo(dados.lotacaoId),
  jornadaId: inteiroOuNulo(dados.jornadaId),
  cargoId: inteiroOuNulo(dados.cargoId),
  efetivoPrevisto: Math.max(0, inteiroOuNulo(dados.efetivoPrevisto) ?? 0),
  dataInicioVigencia: textoOuNulo(dados.dataInicioVigencia),
  dataFimVigencia: textoOuNulo(dados.dataFimVigencia),
  ...(dados.version != null ? { version: dados.version } : {}),
});

const prepararPrecedencia = (dados = {}) => ({
  eventoPrecedenteId: inteiroOuNulo(dados.eventoPrecedenteId),
  relacaoPrecedencia: textoOuNulo(dados.relacaoPrecedencia),
  ordemDocumental: inteiroOuNulo(dados.ordemDocumental),
});

export const prepararAditamento = (dados = {}, tokenPrevia = null) => ({
  identificacao: textoOuNulo(dados.identificacao),
  dataDocumento: textoOuNulo(dados.dataDocumento),
  dataAssinatura: textoOuNulo(dados.dataAssinatura),
  dataEfetiva: textoOuNulo(dados.dataEfetiva),
  justificativa: textoOuNulo(dados.justificativa),
  referenciaSei: textoOuNulo(dados.referenciaSei),
  novaDescricao: textoOuNulo(dados.novaDescricao),
  novoTipoContratacaoId: inteiroOuNulo(dados.novoTipoContratacaoId),
  novoFornecedorId: inteiroOuNulo(dados.novoFornecedorId),
  novoCnpjEmpresa: textoOuNulo(dados.novoCnpjEmpresa)?.replace(/\D/g, '') || null,
  novaRazaoSocialEmpresa: textoOuNulo(dados.novaRazaoSocialEmpresa),
  novoNomeFantasiaEmpresa: textoOuNulo(dados.novoNomeFantasiaEmpresa),
  novaDataInicio: textoOuNulo(dados.novaDataInicio),
  novaDataFim: textoOuNulo(dados.novaDataFim),
  novoEfetivoTotal: inteiroOuNulo(dados.novoEfetivoTotal),
  reducaoExcepcional: Boolean(dados.reducaoExcepcional),
  fundamentoExcecao: textoOuNulo(dados.fundamentoExcecao),
  confirmarImpacto: Boolean(dados.confirmarImpacto),
  alteracoesComposicao: (dados.alteracoesComposicao || []).map((item) => ({
    composicaoId: inteiroOuNulo(item.composicaoId),
    lotacaoId: inteiroOuNulo(item.lotacaoId),
    jornadaId: inteiroOuNulo(item.jornadaId),
    cargoId: inteiroOuNulo(item.cargoId),
    quantidadeResultante: Math.max(0, inteiroOuNulo(item.quantidadeResultante) ?? 0),
  })),
  ...prepararPrecedencia(dados),
  tokenPrevia: textoOuNulo(tokenPrevia),
});

export const prepararRemanejamento = (dados = {}, tokenPrevia = null) => ({
  identificacao: textoOuNulo(dados.identificacao),
  dataEfetiva: textoOuNulo(dados.dataEfetiva),
  justificativa: textoOuNulo(dados.justificativa),
  referenciaSei: textoOuNulo(dados.referenciaSei),
  confirmarImpacto: Boolean(dados.confirmarImpacto),
  itens: (dados.itens || []).map((item) => ({
    composicaoId: inteiroOuNulo(item.composicaoId),
    lotacaoId: inteiroOuNulo(item.lotacaoId),
    jornadaId: inteiroOuNulo(item.jornadaId),
    cargoId: inteiroOuNulo(item.cargoId),
    variacao: inteiroOuNulo(item.variacao) ?? 0,
  })),
  ...prepararPrecedencia(dados),
  tokenPrevia: textoOuNulo(tokenPrevia),
});

export const somaVariacoesRemanejamento = (itens = []) => itens.reduce((total, item) => total + (Number(item.variacao) || 0), 0);

export const validarRemanejamentoLocal = (dados = {}) => {
  if (!String(dados.identificacao || '').trim()) return 'Informe a identificação do remanejamento.';
  if (!dados.dataEfetiva) return 'Informe a data efetiva do remanejamento.';
  if (!String(dados.justificativa || '').trim()) return 'Informe a justificativa do remanejamento.';
  if ((dados.itens || []).length < 2) return 'O remanejamento exige pelo menos duas composições.';
  if ((dados.itens || []).some((item) => !Number(item.variacao))) return 'Cada item do remanejamento deve possuir variação diferente de zero.';
  if (somaVariacoesRemanejamento(dados.itens) !== 0) return 'A soma das variações deve ser zero para preservar o efetivo total.';
  return null;
};

export const validarAditamentoLocal = (dados = {}) => {
  if (!String(dados.identificacao || '').trim()) return 'Informe a identificação do aditamento.';
  if (!dados.dataEfetiva) return 'Informe a data efetiva do aditamento.';
  if (!String(dados.justificativa || '').trim()) return 'Informe a justificativa do aditamento.';
  if (dados.reducaoExcepcional && !String(dados.fundamentoExcecao || '').trim()) return 'Informe o fundamento da redução excepcional.';
  return null;
};

export const criarEstadoPrevia = () => ({ previa: null, assinatura: null });
export const invalidarPrevia = () => criarEstadoPrevia();

export const assinaturaDadosPrevia = (dados) => JSON.stringify(dados, (_chave, valor) => (
  typeof valor === 'string' ? valor.trim() : valor
));

export const registrarPrevia = (dados, previa) => ({
  previa,
  assinatura: assinaturaDadosPrevia(dados),
});

export const previaContinuaValida = (estado, dados) => Boolean(
  estado?.previa?.tokenPrevia
  && estado?.assinatura
  && estado.assinatura === assinaturaDadosPrevia(dados),
);

export const payloadComTokenDaPrevia = (preparador, dados, estadoPrevia) => {
  if (!previaContinuaValida(estadoPrevia, dados)) return null;
  return preparador(dados, estadoPrevia.previa.tokenPrevia);
};

export const ehConflitoDePrevia = (erro) => Number(erro?.status || erro?.response?.status) === 409;
