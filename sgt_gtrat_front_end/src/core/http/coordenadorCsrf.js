let tokenAtual = null;
let promessaBootstrap = null;
let buscarToken = null;

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
  tokenAtual = null;
};

export const obterTokenCsrfEmMemoria = () => tokenAtual;

export const obterTokenCsrf = async ({ forcarRenovacao = false } = {}) => {
  if (forcarRenovacao) tokenAtual = null;
  if (tokenAtual) return tokenAtual;
  if (promessaBootstrap) return promessaBootstrap;
  if (typeof buscarToken !== 'function') throw new Error('Buscador CSRF não configurado.');

  promessaBootstrap = Promise.resolve()
    .then(() => buscarToken())
    .then(normalizarToken)
    .then((token) => {
      tokenAtual = token;
      return token;
    })
    .finally(() => {
      promessaBootstrap = null;
    });

  return promessaBootstrap;
};

export const resetarCoordenadorCsrfParaTestes = () => {
  tokenAtual = null;
  promessaBootstrap = null;
};
