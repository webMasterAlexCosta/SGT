import type { TokenCsrfDTO } from '../../contracts/java';
import { criarErroSessaoObsoleta, obterGeracaoSessao, obterSinalSessao, sessaoAtual } from '../security/geracaoSessao.ts';

type NormalizedCsrf = { [K in keyof TokenCsrfDTO]: NonNullable<TokenCsrfDTO[K]> };
let tokenAtual: { geracao: number; token: NormalizedCsrf } | null = null;
let promessaBootstrap: { geracao: number; promessa: Promise<NormalizedCsrf> } | null = null;
let buscarToken: ((contexto: { geracaoSessao: number }) => Promise<unknown>) | null = null;
let revisaoCache = 0;
let sinalObservado: AbortSignal | null = null;

const normalizarToken = (valor: unknown): NormalizedCsrf => {
  const raw = valor && typeof valor === 'object' ? valor as Record<string, unknown> : {};
  const nested = raw.data && typeof raw.data === 'object' ? raw.data as Record<string, unknown> : null;
  const dto = nested?.headerName ? nested : raw;
  const headerName = typeof dto?.headerName === 'string' ? dto.headerName.trim() : '';
  const parameterName = typeof dto?.parameterName === 'string' ? dto.parameterName.trim() : '';
  const token = typeof dto?.token === 'string' ? dto.token.trim() : '';

  if (!headerName || !token) {
    throw new Error('Contrato CSRF inválido: headerName/token ausentes.');
  }

  return Object.freeze({ headerName, parameterName, token });
};

export const configurarBuscadorTokenCsrf = (funcao: (contexto: { geracaoSessao: number }) => Promise<unknown>) => {
  if (typeof funcao !== 'function') throw new TypeError('Buscador CSRF deve ser uma função.');
  buscarToken = funcao;
};

export const invalidarTokenCsrf = () => {
  revisaoCache += 1;
  tokenAtual = null;
  promessaBootstrap = null;
};

export const obterTokenCsrfEmMemoria = () => tokenAtual && sessaoAtual(tokenAtual.geracao) ? tokenAtual.token : null;

export const obterTokenCsrf = async ({ forcarRenovacao = false, geracaoSessao = obterGeracaoSessao() } = {}) => {
  if (!sessaoAtual(geracaoSessao)) throw criarErroSessaoObsoleta();
  const sinal = obterSinalSessao(geracaoSessao);
  if (sinalObservado !== sinal) {
    sinalObservado?.removeEventListener('abort', invalidarTokenCsrf);
    sinalObservado = sinal;
    sinal.addEventListener('abort', invalidarTokenCsrf, { once: true });
  }
  if (forcarRenovacao) invalidarTokenCsrf();
  const emMemoria = obterTokenCsrfEmMemoria();
  if (emMemoria) return emMemoria;
  if (promessaBootstrap?.geracao === geracaoSessao) return promessaBootstrap.promessa;
  if (typeof buscarToken !== 'function') throw new Error('Buscador CSRF não configurado.');

  const revisao = revisaoCache;
  const vigente = () => sessaoAtual(geracaoSessao) && revisao === revisaoCache;
  const promessa = Promise.resolve()
    .then(() => {
      if (!vigente()) throw criarErroSessaoObsoleta();
      if (typeof buscarToken !== 'function') throw new Error('Buscador CSRF não configurado.');
      return buscarToken({ geracaoSessao });
    })
    .then((valor: unknown): NormalizedCsrf => {
      if (!vigente()) throw criarErroSessaoObsoleta();
      const token = normalizarToken(valor);
      tokenAtual = { geracao: geracaoSessao, token };
      return token;
    })
    .catch((erro: unknown) => { throw vigente() ? erro : criarErroSessaoObsoleta(); })
    .finally(() => {
      if (promessaBootstrap?.promessa === promessa) promessaBootstrap = null;
    });

  promessaBootstrap = { geracao: geracaoSessao, promessa };
  return promessa;
};

export const resetarCoordenadorCsrfParaTestes = () => {
  invalidarTokenCsrf();
  sinalObservado?.removeEventListener('abort', invalidarTokenCsrf);
  sinalObservado = null;
};
