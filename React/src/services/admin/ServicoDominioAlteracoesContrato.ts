import type * as DTO from '../../contracts/java';
export interface CompositionForm {
  id: number | null; version: number | null;
  lotacaoId: string | number; jornadaId: string | number; cargoId: string | number;
  efetivoPrevisto: number; dataInicioVigencia: string; dataFimVigencia: string;
}
interface PrecedenceInput { eventoPrecedenteId?: unknown; relacaoPrecedencia?: DTO.TipoRelacaoPrecedenciaNegocial | '' | null; ordemDocumental?: unknown }
type AditamentoPoliticaInput = Partial<Pick<DTO.AditamentoContratualRequisicao,
  'novaMatriculaVinculoObrigatoria' | 'novoDiaSemanaReduzido' | 'novasHorasDiaReduzido'
  | 'restaurarPoliticaMatriculaDoTipo' | 'removerConfiguracaoDiaReduzido'>>;
export interface CompositionChangeInput { chaveCliente?: string | null; composicaoId?: string | number | null; lotacaoId?: string | number | null; jornadaId?: string | number | null; cargoId?: string | number | null; quantidadeResultante?: string | number | null; efetivoPrevisto?: string | number | null; variacao?: string | number | null }
const inteiroOuNulo = (valor: (unknown) | (string | number | undefined)) => {
  if (valor === '' || valor == null) return null;
  const numero = Number(valor);
  return Number.isFinite(numero) ? Math.trunc(numero) : null;
};

function textoOuNulo(valor: DTO.TipoRelacaoPrecedenciaNegocial | '' | null | undefined): DTO.TipoRelacaoPrecedenciaNegocial | null;
function textoOuNulo(valor: unknown): string | null;
function textoOuNulo(valor: unknown): string | null {
  const texto = String(valor ?? '').trim();
  return texto || null;
}

export const criarChaveCliente = () => `linha-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;

export const criarLinhaAditamento = (origem: CompositionChangeInput = {}) => ({
  chaveCliente: origem.chaveCliente || criarChaveCliente(),
  composicaoId: origem.composicaoId ?? '',
  lotacaoId: origem.lotacaoId ?? '',
  jornadaId: origem.jornadaId ?? '',
  cargoId: origem.cargoId ?? '',
  quantidadeResultante: origem.quantidadeResultante ?? origem.efetivoPrevisto ?? 0,
});

export const criarLinhaRemanejamento = (origem: CompositionChangeInput = {}) => ({
  chaveCliente: origem.chaveCliente || criarChaveCliente(),
  composicaoId: origem.composicaoId ?? '',
  lotacaoId: origem.lotacaoId ?? '',
  jornadaId: origem.jornadaId ?? '',
  cargoId: origem.cargoId ?? '',
  variacao: origem.variacao ?? 0,
});

export const prepararComposicao = (dados: Partial<CompositionForm> = {}) => ({
  id: dados.id || undefined,
  lotacaoId: inteiroOuNulo(dados.lotacaoId),
  jornadaId: inteiroOuNulo(dados.jornadaId),
  cargoId: inteiroOuNulo(dados.cargoId),
  efetivoPrevisto: Math.max(0, inteiroOuNulo(dados.efetivoPrevisto) ?? 0),
  dataInicioVigencia: textoOuNulo(dados.dataInicioVigencia),
  dataFimVigencia: textoOuNulo(dados.dataFimVigencia),
  ...(dados.version != null ? { version: dados.version } : {}),
});

const prepararPrecedencia = (dados: PrecedenceInput = {}) => ({
  eventoPrecedenteId: inteiroOuNulo(dados.eventoPrecedenteId),
  relacaoPrecedencia: textoOuNulo(dados.relacaoPrecedencia),
  ordemDocumental: inteiroOuNulo(dados.ordemDocumental),
});

export const prepararAditamento = (dados: { identificacao?: unknown; dataDocumento?: unknown; dataAssinatura?: unknown; dataEfetiva?: unknown; justificativa?: unknown; referenciaSei?: unknown; novaDescricao?: unknown; novoTipoContratacaoId?: unknown; novoFornecedorId?: unknown; novoCnpjEmpresa?: unknown; novaRazaoSocialEmpresa?: unknown; novoNomeFantasiaEmpresa?: unknown; novaDataInicio?: unknown; novaDataFim?: unknown; novoEfetivoTotal?: unknown; reducaoExcepcional?: unknown; fundamentoExcecao?: unknown; confirmarImpacto?: unknown; alteracoesComposicao?: CompositionChangeInput[] } & PrecedenceInput & AditamentoPoliticaInput = {}, tokenPrevia: string | null = null) => ({
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
  novaMatriculaVinculoObrigatoria: dados.novaMatriculaVinculoObrigatoria ?? null,
  novoDiaSemanaReduzido: dados.novoDiaSemanaReduzido ?? null,
  novasHorasDiaReduzido: dados.novasHorasDiaReduzido ?? null,
  restaurarPoliticaMatriculaDoTipo: dados.restaurarPoliticaMatriculaDoTipo ?? null,
  removerConfiguracaoDiaReduzido: dados.removerConfiguracaoDiaReduzido ?? null,
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

export const prepararRemanejamento = (dados: { identificacao?: unknown; dataEfetiva?: unknown; justificativa?: unknown; referenciaSei?: unknown; confirmarImpacto?: unknown; itens?: CompositionChangeInput[] } & PrecedenceInput = {}, tokenPrevia: string | null = null) => ({
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

export const somaVariacoesRemanejamento = (itens: CompositionChangeInput[] = []) => itens.reduce((total, item) => total + (Number(item.variacao) || 0), 0);

export const validarRemanejamentoLocal = (dados: { identificacao?: unknown; dataEfetiva?: unknown; justificativa?: unknown; itens?: CompositionChangeInput[] } = {}) => {
  if (!String(dados.identificacao || '').trim()) return 'Informe a identificação do remanejamento.';
  if (!dados.dataEfetiva) return 'Informe a data efetiva do remanejamento.';
  if (!String(dados.justificativa || '').trim()) return 'Informe a justificativa do remanejamento.';
  if ((dados.itens || []).length < 2) return 'O remanejamento exige pelo menos duas composições.';
  if ((dados.itens || []).some((item) => !Number(item.variacao))) return 'Cada item do remanejamento deve possuir variação diferente de zero.';
  if (somaVariacoesRemanejamento(dados.itens) !== 0) return 'A soma das variações deve ser zero para preservar o efetivo total.';
  return null;
};

export const validarAditamentoLocal = (dados: {
  identificacao?: unknown; dataEfetiva?: unknown; novaDataFim?: unknown; dataDocumento?: unknown;
  justificativa?: unknown; reducaoExcepcional?: unknown; fundamentoExcecao?: unknown;
  alterarEmpresa?: boolean; novoFornecedorId?: unknown; novoCnpjEmpresa?: unknown;
  novaRazaoSocialEmpresa?: unknown; novoNomeFantasiaEmpresa?: unknown;
} = {}) => {
  if (!String(dados.identificacao || '').trim()) return 'Informe a identificação do aditamento.';
  if (!dados.dataEfetiva) return 'Informe a data efetiva do aditamento.';
  // A data efetiva deve estar contida na vigência resultante (mesma regra do
  // serviço Java). Datas de input=date já são civis ISO: não converter em UTC.
  if (dados.novaDataFim && dados.novaDataFim < dados.dataEfetiva) return 'A nova data final da vigência não pode ser anterior à data efetiva.';
  if (!dados.dataDocumento) return 'Informe a data do documento do aditamento.';
  if (!String(dados.justificativa || '').trim()) return 'Informe a justificativa do aditamento.';
  if (dados.alterarEmpresa) {
    if (!inteiroOuNulo(dados.novoFornecedorId)) return 'Selecione a nova empresa contratada.';
    if (!textoOuNulo(dados.novoCnpjEmpresa) || !textoOuNulo(dados.novaRazaoSocialEmpresa) || !textoOuNulo(dados.novoNomeFantasiaEmpresa)) {
      return 'Informe o CNPJ, a razão social e o nome fantasia que constam no documento.';
    }
  }
  if (dados.reducaoExcepcional && !String(dados.fundamentoExcecao || '').trim()) return 'Informe o fundamento da redução excepcional.';
  return null;
};

export interface PreviewState<T = { tokenPrevia?: string }> { previa: T | null; assinatura: string | null; contratoId?: string | number | null; contexto?: string }
export const criarEstadoPrevia = <T = { tokenPrevia?: string }>(): PreviewState<T> => ({ previa: null, assinatura: null });
export const invalidarPrevia = <T = { tokenPrevia?: string }>() => criarEstadoPrevia<T>();

export const assinaturaDadosPrevia = (dados: unknown) => JSON.stringify(dados, (_chave: string, valor: unknown) => (
  typeof valor === 'string' ? valor.trim() : valor
));

export const registrarPrevia = <T,>(dados: unknown, previa: T): PreviewState<T> => ({
  previa,
  assinatura: assinaturaDadosPrevia(dados),
});

export const previaContinuaValida = <T extends { tokenPrevia?: string | null }>(estado: PreviewState<T> | null | undefined, dados: unknown): estado is PreviewState<T> & { previa: T } => Boolean(
  estado?.previa?.tokenPrevia && estado.assinatura && estado.assinatura === assinaturaDadosPrevia(dados),
);

export const payloadComTokenDaPrevia = <D, R, T extends { tokenPrevia?: string | null }>(preparador: (dados: D, tokenPrevia: string | null) => R, dados: D, estadoPrevia: PreviewState<T>): R | null => {
  if (!previaContinuaValida(estadoPrevia, dados)) return null;
  return preparador(dados, estadoPrevia.previa.tokenPrevia ?? null);
};

export const ehConflitoDePrevia = (erro: unknown): boolean => {
  if (typeof erro !== 'object' || erro === null) return false;
  const response: unknown = Reflect.get(erro, 'response');
  const status: unknown = Reflect.get(erro, 'status') || (response !== null && typeof response === 'object' ? Reflect.get(response, 'status') : undefined);
  return Number(status) === 409;
};

export type CompositionPayload = ReturnType<typeof prepararComposicao>;
export type AmendmentPayload = ReturnType<typeof prepararAditamento>;
export type ReallocationPayload = ReturnType<typeof prepararRemanejamento>;
export type AmendmentLineDraft = ReturnType<typeof criarLinhaAditamento>;
export type ReallocationLineDraft = ReturnType<typeof criarLinhaRemanejamento>;
