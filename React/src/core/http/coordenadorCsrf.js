import { criarErroSessaoObsoleta, obterGeracaoSessao, obterSinalSessao, sessaoAtual } from '../security/geracaoSessao.js';

let tokenAtual = null;
let promessaBootstrap = null;
let buscarToken = null;
let revisaoCache = 0;
let sinalObservado = null;

const normalizarToken = (valor) => {
  const dto = valor?.data?.headerName ? valor.data : valor;
  const headerName = typeof dto?.headerName === 'string' ? dto.headerName.trim() : '';
  const parameterName = typeof dto?.parameterName === 'string' ? dto.parameterName.trim() : '';
  const token = typeof dto?.token === 'string' ? dto.token.trim() : '';

  if (!headerName || !token) {
    throw new Error('Contrato CSRF inválido: headerName/token ausentes.');
  }

  return Object.freeze({ headerName, parameterName, token });
};

export const configurarBuscadorTokenCsrf = (funcao) => {
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
      return buscarToken({ geracaoSessao });
    })
    .then((valor) => {
      if (!vigente()) throw criarErroSessaoObsoleta();
      const token = normalizarToken(valor);
      tokenAtual = { geracao: geracaoSessao, token };
      return token;
    })
    .catch((erro) => { throw vigente() ? erro : criarErroSessaoObsoleta(); })
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
