import { CAMPOS_FOTOGRAFIA } from './dominioHistorico.ts';

export interface ProofOrigin {
  tipo: string;
  provaId?: number; eventoId?: number; versaoId?: number; composicaoId?: number;
  aditamentoComposicaoId?: number; mapeamentoId?: number;
  dataEfetiva?: string; registradoEm?: string;
}
interface ProofBase { visibilidade: 'DISPONIVEL' | 'RESTRITA' | null; rotulo: string; origens: ProofOrigin[] }
export type ProofReading = (ProofBase & { tipo: 'VALOR_COMPROVADO'; valor: string | number | boolean })
  | (ProofBase & { tipo: 'RESTRITA' | 'INDISPONIVEL' | 'NAO_COMPROVADO' | 'AUSENCIA_COMPROVADA' });

const objeto = (valor: unknown): valor is Record<string, unknown> => valor !== null && typeof valor === 'object' && !Array.isArray(valor);
const ESTADOS = new Set(['VALOR_COMPROVADO', 'AUSENCIA_COMPROVADA', 'NAO_COMPROVADO']);
const TIPOS_ORIGEM = new Set(['FOTOGRAFIA_CONTRATUAL', 'PROVA_HISTORICA', 'EVENTO_CONTRATUAL',
  'COMPOSICAO_PUBLICADA', 'ITEM_ADITAMENTO', 'MAPEAMENTO_TRANSFERENCIA']);
const IDS_ORIGEM = ['provaId', 'eventoId', 'versaoId', 'composicaoId', 'aditamentoComposicaoId', 'mapeamentoId'];
const CAMPOS_NEGOCIAIS = new Set([
  ...CAMPOS_FOTOGRAFIA.map(({ nome }) => nome), 'estadoNegocial', 'situacaoTemporal', 'vigenciaInicio', 'vigenciaFim',
  'lotacaoId', 'lotacaoNome', 'jornadaId', 'jornadaDescricao', 'cargoId', 'cargoNome',
  'dataInicioVigencia', 'dataFimVigencia', 'permiteNovaAlocacao', 'lotacaoMunicipio', 'jornadaSemanal',
  'referenciaSei', 'motivo', 'autorizacaoAdministrativa', 'documentoComprobatorioId', 'hashFonteArquivo',
]);

export const possuiMetadadosProva = (dados: unknown): dados is Record<string, unknown> & { provaCampos: unknown } => objeto(dados) && Object.hasOwn(dados, 'provaCampos');
export const camposMetadadosProva = (dados: unknown): string[] => objeto(dados) && objeto(dados.provaCampos) ? Object.keys(dados.provaCampos) : [];

// A proveniência é uma lista fechada de referências. Não propaga PII, documento ou
// outras propriedades recebidas incidentalmente dentro de uma origem.
const lerOrigens = (origens: unknown[]): ProofOrigin[] => origens.flatMap(origem => {
  if (!objeto(origem) || typeof origem.tipo !== 'string' || !TIPOS_ORIGEM.has(origem.tipo)) return [];
  const resultado: ProofOrigin = { tipo: origem.tipo };
  for (const id of IDS_ORIGEM) {
    if (typeof origem[id] === 'number' && Number.isSafeInteger(origem[id]) && Number(origem[id]) > 0) Reflect.set(resultado, id, origem[id]);
  }
  if (typeof origem.dataEfetiva === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(origem.dataEfetiva)) resultado.dataEfetiva = origem.dataEfetiva;
  if (typeof origem.registradoEm === 'string' && /(?:Z|[+-]\d{2}:\d{2})$/i.test(origem.registradoEm)
    && Number.isFinite(Date.parse(origem.registradoEm))) resultado.registradoEm = origem.registradoEm;
  return [resultado];
});

/**
 * Interpreta somente a semântica recebida, sem resolver fontes ou completar valores.
 * null preserva o renderer legado exclusivamente quando não há metadados, ou quando
 * a propriedade é identidade técnica/estrutura fora do mapa de campos de negócio.
 * @param {Record<string, unknown>} dados
 * @param {string} campo
 */
export const lerProvaCampo = (dados: unknown, campo: string): ProofReading | null => {
  if (!possuiMetadadosProva(dados)) return null;
  const mapa = objeto(dados.provaCampos) ? dados.provaCampos : null;
  const declarada = mapa && Object.hasOwn(mapa, campo);
  // No recibo, a data do ato é metadado da revisão. Sua fotografia aninhada
  // possui o mapa factual próprio; não transportar esse mapa para o recibo.
  const dataDaRevisao = campo === 'dataEfetiva' && Object.hasOwn(dados, 'numeroRevisao') && Object.hasOwn(dados, 'versaoGeradaId');
  if (!declarada && dataDaRevisao) return null;
  if (!declarada && !CAMPOS_NEGOCIAIS.has(campo)) return null;
  const metadado = declarada ? mapa[campo] : null;
  // A restrição precede estado, valor e proveniência, inclusive em resposta adversarial.
  if (objeto(metadado) && metadado.visibilidade === 'RESTRITA') {
    return { tipo: 'RESTRITA', visibilidade: 'RESTRITA', rotulo: 'Acesso restrito', origens: [] };
  }
  if (!objeto(metadado) || metadado.visibilidade !== 'DISPONIVEL'
    || typeof metadado.estado !== 'string' || !ESTADOS.has(metadado.estado) || !Array.isArray(metadado.origens)) {
    return { tipo: 'INDISPONIVEL', visibilidade: objeto(metadado) && metadado.visibilidade === 'DISPONIVEL' ? 'DISPONIVEL' : null,
      rotulo: 'Comprovação indisponível nesta resposta', origens: [] };
  }
  if (metadado.estado === 'NAO_COMPROVADO') return { tipo: 'NAO_COMPROVADO', visibilidade: 'DISPONIVEL', rotulo: 'Desconhecido / não comprovado', origens: [] };
  const origens = lerOrigens(metadado.origens);
  if (metadado.estado === 'AUSENCIA_COMPROVADA') return { tipo: 'AUSENCIA_COMPROVADA', visibilidade: 'DISPONIVEL', rotulo: 'Ausência comprovada', origens };
  const valor = dados[campo];
  if (!Object.hasOwn(dados, campo) || valor == null || !['string', 'number', 'boolean'].includes(typeof valor)
    || (typeof valor === 'number' && !Number.isFinite(valor))) {
    return { tipo: 'INDISPONIVEL', visibilidade: 'DISPONIVEL', rotulo: 'Valor comprovado não disponível nesta resposta', origens: [] };
  }
  return { tipo: 'VALOR_COMPROVADO', visibilidade: 'DISPONIVEL', rotulo: 'Valor comprovado', valor: valor as string | number | boolean, origens };
};

export const formatarInstanteFonte = (valor: unknown) => {
  if (typeof valor !== 'string' || !Number.isFinite(Date.parse(valor))) return null;
  return `${new Date(valor).toLocaleString('pt-BR', { timeZone: 'America/Sao_Paulo', hourCycle: 'h23' })} · America/Sao_Paulo`;
};

/** Valor utilizável de uma leitura; não retorna escalar residual ocultado pelo mapa. */
export function valorCampoDisponivel<T extends object, K extends keyof T>(dados: T, campo: K): T[K] | undefined;
export function valorCampoDisponivel(dados: unknown, campo: string): unknown;
export function valorCampoDisponivel(dados: unknown, campo: string): unknown {
  const prova = lerProvaCampo(dados, campo);
  return prova ? prova.tipo === 'VALOR_COMPROVADO' ? prova.valor : undefined : (objeto(dados) ? dados[campo] : undefined);
}

// <option> aceita somente texto. A decisão documental é a mesma usada pelo
// renderer rico, antes de nomes, números e quaisquer formatadores de domínio.
export const textoCampoComProva = (dados: unknown, campo: string) => {
  const prova = lerProvaCampo(dados, campo);
  if (!prova) return (objeto(dados) ? dados[campo] : undefined) == null || (objeto(dados) ? dados[campo] : undefined) === '' ? 'Não informado pelo servidor' : String(objeto(dados) ? dados[campo] : undefined);
  const visibilidade = prova.visibilidade === 'DISPONIVEL' ? ' · visibilidade disponível' : '';
  if (prova.tipo !== 'VALOR_COMPROVADO') return prova.rotulo + visibilidade;
  const valor = typeof prova.valor === 'boolean' ? prova.valor ? 'Sim' : 'Não' : prova.valor === '' ? 'Texto vazio documentado' : String(prova.valor);
  return `${prova.rotulo}: ${valor}${visibilidade}`;
};

export const rotuloComposicaoComProva = (item: object) => {
  const id: unknown = Reflect.get(item, 'id');
  if (!possuiMetadadosProva(item)) {
    return [Reflect.get(item, 'lotacaoNome'), Reflect.get(item, 'jornadaDescricao'), Reflect.get(item, 'cargoNome')].filter(Boolean).join(' · ') || `Composição ${id}`;
  }
  const identidade = typeof id === 'number' && Number.isSafeInteger(id) && id > 0 ? `Composição #${id}` : 'Composição projetada sem ID físico';
  return `${identidade} · Lotação: ${textoCampoComProva(item, 'lotacaoNome')} · Jornada: ${textoCampoComProva(item, 'jornadaDescricao')} · Cargo: ${textoCampoComProva(item, 'cargoNome')}`;
};
