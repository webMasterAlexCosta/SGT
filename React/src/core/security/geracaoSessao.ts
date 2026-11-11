// Fronteira de sessão compartilhada pelo transporte e pelos consumidores.
// Não depende de React, de serviços nem de eventos; pode ser usada isoladamente.
let geracaoAtual = 0;
let controladorAtual = new AbortController();

// Sinal de invalidação, nunca prova de autenticação. Os dois canais contêm
// somente uma revisão aleatória, sem usuário, credencial ou token do servidor.
// O cookie é o canal comum obrigatório, inclusive quando Storage é negado.
// Permitir canais exclusivos distintos em cada aba não garantiria coordenação.
export const CHAVE_TROCA_SESSAO = 'sgt_revisao_compartilhada_sessao';
export const EVENT_TROCA_SESSAO_EXTERNA = 'sgt-troca-sessao-externa';
const INDISPONIVEL = Symbol('canal-indisponivel');
const lerStorage = () => {
  try {
    const storage = globalThis.localStorage;
    return storage ? storage.getItem(CHAVE_TROCA_SESSAO) : INDISPONIVEL;
  }
  catch { return INDISPONIVEL; }
};
const lerCookie = () => {
  try {
    if (!globalThis.document) return INDISPONIVEL;
    const prefixo = `${CHAVE_TROCA_SESSAO}=`;
    const entrada = document.cookie.split(';').map(item => item.trim()).find(item => item.startsWith(prefixo));
    return entrada ? decodeURIComponent(entrada.slice(prefixo.length)) : null;
  } catch { return INDISPONIVEL; }
};
const lerRevisao = () => ({ storage: lerStorage(), cookie: lerCookie() });
let revisaoObservada = lerRevisao();
let coordenacaoBloqueada = false;
let avisoExternoAgendado = false;

export const houveTrocaSessaoExterna = () => {
  const atual = lerRevisao();
  return coordenacaoBloqueada
    || atual.cookie === INDISPONIVEL
    || atual.storage !== revisaoObservada.storage || atual.cookie !== revisaoObservada.cookie;
};

export const anunciarTrocaSessao = () => {
  const revisao = globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  try { globalThis.localStorage?.setItem(CHAVE_TROCA_SESSAO, revisao); } catch {}
  try {
    if (globalThis.document) document.cookie = `${CHAVE_TROCA_SESSAO}=${encodeURIComponent(revisao)}; Path=/; SameSite=Strict${globalThis.location?.protocol === 'https:' ? '; Secure' : ''}`;
  } catch {}
  // Alguns browsers recusam a escrita de cookies sem lançar erro. Conferir os
  // bytes lidos evita assumir coordenação apenas porque setItem/setter voltou.
  const publicada = lerRevisao();
  coordenacaoBloqueada = publicada.cookie !== revisao;
  if (!coordenacaoBloqueada) revisaoObservada = publicada;
  return !coordenacaoBloqueada;
};

const avisarTrocaExterna = () => {
  if (avisoExternoAgendado || !globalThis.window) return;
  avisoExternoAgendado = true;
  // A guarda continua síncrona. A limpeza React ocorre fora de uma eventual
  // renderização que consultou a guarda, sem esperar o próximo focus/storage.
  queueMicrotask(() => {
    avisoExternoAgendado = false;
    if (houveTrocaSessaoExterna()) window.dispatchEvent(new Event(EVENT_TROCA_SESSAO_EXTERNA));
  });
};

export const obterGeracaoSessao = () => geracaoAtual;
export const sessaoAtual = (geracao: number) => {
  if (geracao !== geracaoAtual) return false;
  if (!houveTrocaSessaoExterna()) return true;
  avisarTrocaExterna();
  return false;
};

export const invalidarGeracaoSessao = () => {
  const anterior = controladorAtual;
  geracaoAtual += 1;
  revisaoObservada = lerRevisao();
  controladorAtual = new AbortController();
  // A nova geração já está publicada quando os consumidores recebem o abort.
  anterior.abort();
  return geracaoAtual;
};

export const obterSinalSessao = (geracao = geracaoAtual) => {
  if (sessaoAtual(geracao)) return controladorAtual.signal;
  const encerrado = new AbortController();
  encerrado.abort();
  return encerrado.signal;
};

export const criarErroSessaoObsoleta = () => Object.assign(
  new Error('Requisição cancelada porque o contexto de sessão mudou.'),
  { status: 0, code: 'REQUEST_CANCELED', tipo: 'CANCELED', canceled: true, silencioso: true },
);
