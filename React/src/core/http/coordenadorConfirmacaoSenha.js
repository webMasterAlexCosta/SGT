let solicitador = null;
let fila = Promise.resolve();

export const registrarSolicitadorConfirmacaoSenha = (funcao) => {
  if (typeof funcao !== 'function') throw new TypeError('Solicitador de confirmação deve ser função.');
  solicitador = funcao;
  return () => {
    if (solicitador === funcao) solicitador = null;
  };
};

export const criarErroConfirmacaoCancelada = () => {
  const erro = new Error('Confirmação cancelada pelo usuário.');
  Object.assign(erro, { status: 0, code: 'PASSWORD_CONFIRMATION_CANCELED', tipo: 'CANCELED', canceled: true, silencioso: true });
  return erro;
};

export const solicitarConfirmacaoSenha = (contexto) => {
  const executar = async () => {
    if (contexto?.signal?.aborted) throw criarErroConfirmacaoCancelada();
    if (typeof solicitador !== 'function') {
      const erro = new Error('Interface central de confirmação de senha indisponível.');
      Object.assign(erro, { status: 428, code: 'PASSWORD_CONFIRMATION_UI_UNAVAILABLE', tipo: 'PASSWORD_CONFIRMATION_REQUIRED' });
      throw erro;
    }
    return solicitador(contexto);
  };

  const resultado = fila.then(executar, executar);
  fila = resultado.catch(() => undefined);
  return resultado;
};

export const resetarSolicitadorConfirmacaoSenhaParaTestes = () => {
  solicitador = null;
  fila = Promise.resolve();
};
