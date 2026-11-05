import { CAMPOS_FOTOGRAFIA } from './dominioHistorico.js';

/** @typedef {'VALOR_COMPROVADO'|'AUSENCIA_COMPROVADA'|'NAO_COMPROVADO'} EstadoProva */
/** @typedef {'DISPONIVEL'|'RESTRITA'} VisibilidadeProva */
/** @typedef {{tipo:string, provaId?:number, eventoId?:number, versaoId?:number, composicaoId?:number, aditamentoComposicaoId?:number, mapeamentoId?:number, dataEfetiva?:string|null, registradoEm?:string|null}} OrigemProva */
/** @typedef {{estado:EstadoProva|null, visibilidade:VisibilidadeProva, origens:OrigemProva[]}} ProvaCampo */

const objeto = valor => valor !== null && typeof valor === 'object' && !Array.isArray(valor);
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

export const possuiMetadadosProva = dados => objeto(dados) && Object.hasOwn(dados, 'provaCampos');
export const camposMetadadosProva = dados => objeto(dados?.provaCampos) ? Object.keys(dados.provaCampos) : [];

// A proveniência é uma lista fechada de referências. Não propaga PII, documento ou
// outras propriedades recebidas incidentalmente dentro de uma origem.
const lerOrigens = origens => origens.flatMap(origem => {
  if (!objeto(origem) || !TIPOS_ORIGEM.has(origem.tipo)) return [];
  const resultado = { tipo: origem.tipo };
  for (const id of IDS_ORIGEM) {
    if (Number.isSafeInteger(origem[id]) && origem[id] > 0) resultado[id] = origem[id];
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
export const lerProvaCampo = (dados, campo) => {
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
    || !ESTADOS.has(metadado.estado) || !Array.isArray(metadado.origens)) {
    return { tipo: 'INDISPONIVEL', visibilidade: metadado?.visibilidade === 'DISPONIVEL' ? 'DISPONIVEL' : null,
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
  return { tipo: 'VALOR_COMPROVADO', visibilidade: 'DISPONIVEL', rotulo: 'Valor comprovado', valor, origens };
};

export const formatarInstanteFonte = valor => {
  if (typeof valor !== 'string' || !Number.isFinite(Date.parse(valor))) return null;
  return `${new Date(valor).toLocaleString('pt-BR', { timeZone: 'America/Sao_Paulo', hourCycle: 'h23' })} · America/Sao_Paulo`;
};

/** Valor utilizável de uma leitura; não retorna escalar residual ocultado pelo mapa. */
export const valorCampoDisponivel = (dados, campo) => {
  const prova = lerProvaCampo(dados, campo);
  return prova ? prova.tipo === 'VALOR_COMPROVADO' ? prova.valor : undefined : dados?.[campo];
};

// <option> aceita somente texto. A decisão documental é a mesma usada pelo
// renderer rico, antes de nomes, números e quaisquer formatadores de domínio.
export const textoCampoComProva = (dados, campo) => {
  const prova = lerProvaCampo(dados, campo);
  if (!prova) return dados?.[campo] == null || dados[campo] === '' ? 'Não informado pelo servidor' : String(dados[campo]);
  const visibilidade = prova.visibilidade === 'DISPONIVEL' ? ' · visibilidade disponível' : '';
  if (prova.tipo !== 'VALOR_COMPROVADO') return prova.rotulo + visibilidade;
  const valor = typeof prova.valor === 'boolean' ? prova.valor ? 'Sim' : 'Não' : prova.valor === '' ? 'Texto vazio documentado' : String(prova.valor);
  return `${prova.rotulo}: ${valor}${visibilidade}`;
};

export const rotuloComposicaoComProva = item => {
  if (!possuiMetadadosProva(item)) {
    return [item.lotacaoNome, item.jornadaDescricao, item.cargoNome].filter(Boolean).join(' · ') || `Composição ${item.id}`;
  }
  const identidade = Number.isSafeInteger(item.id) && item.id > 0 ? `Composição #${item.id}` : 'Composição projetada sem ID físico';
  return `${identidade} · Lotação: ${textoCampoComProva(item, 'lotacaoNome')} · Jornada: ${textoCampoComProva(item, 'jornadaDescricao')} · Cargo: ${textoCampoComProva(item, 'cargoNome')}`;
};
