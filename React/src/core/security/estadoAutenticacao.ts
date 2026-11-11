export const ESTADOS_AUTENTICACAO = Object.freeze({
  RESTAURANDO_SESSAO: "RESTAURANDO_SESSAO",
  NAO_AUTENTICADO: "NAO_AUTENTICADO",
  CREDENCIAIS_ACEITAS: "CREDENCIAIS_ACEITAS",
  SEGUNDO_FATOR_REQUERIDO: "SEGUNDO_FATOR_REQUERIDO",
  AUTENTICADO: "AUTENTICADO",
  SENHA_INICIAL_OBRIGATORIA: "SENHA_INICIAL_OBRIGATORIA",
  SESSAO_ENCERRANDO: "SESSAO_ENCERRANDO",
});

export const CODIGOS_SESSAO_ENCERRADA = Object.freeze([
  "SESSION_MISSING",
  "SESSION_INVALID",
  "SESSION_EXPIRED",
  "SESSION_IDLE_TIMEOUT",
  "SESSION_CLAIMS_ERROR",
  "SESSION_REPLACED",
  "SESSION_DEVICE_MISSING",
  "SESSION_DEVICE_MISMATCH",
  "USER_NOT_FOUND",
  "ACCOUNT_INACTIVE",
  "ACCOUNT_LOCKED",
  "UNAUTHORIZED",
]);

const MENSAGENS_SESSAO: Readonly<Record<string, string | undefined>> = Object.freeze({
  SESSION_REPLACED: "Sua sessão foi substituída por um novo acesso.",
  SESSION_EXPIRED: "Sua sessão expirou. Entre novamente.",
  SESSION_IDLE_TIMEOUT: "Sua sessão expirou por inatividade. Entre novamente.",
  SESSION_DEVICE_MISSING: "Não foi possível validar o dispositivo desta sessão.",
  SESSION_DEVICE_MISMATCH: "A sessão não pertence a este dispositivo.",
  ACCOUNT_INACTIVE: "Sua conta não está ativa.",
  ACCOUNT_LOCKED: "Sua conta está temporariamente bloqueada.",
});

export const estadoAutenticadoPorUsuario = (usuario: (import("../../contracts/auth").AuthenticatedUser)) =>
  usuario?.forceChangePassword
    ? ESTADOS_AUTENTICACAO.SENHA_INICIAL_OBRIGATORIA
    : ESTADOS_AUTENTICACAO.AUTENTICADO;

export const mensagemSessaoEncerrada = (codigo: unknown, fallback = "Sessão encerrada. Entre novamente.") =>
  MENSAGENS_SESSAO[String(codigo || "").trim().toUpperCase()] || fallback;

const campoErro = (valor: unknown, campo: string): unknown => valor !== null && typeof valor === 'object' ? Reflect.get(valor, campo) : undefined;
export const detalhesErroSessao = (erro: unknown) => {
  const resposta = campoErro(erro, 'response');
  const data = campoErro(erro, 'data') ?? campoErro(resposta, 'data');
  const status = Number(campoErro(erro, 'status') ?? campoErro(resposta, 'status') ?? 0);
  const codigo = String(campoErro(erro, 'code') || campoErro(data, 'code') || '');
  const texto = campoErro(erro, 'message') || campoErro(data, 'message');
  return { status, codigo, mensagem: typeof texto === 'string' ? texto : undefined, action: campoErro(data, 'action') };
};
// O Java distingue a perda de sessão da recusa de uma autorização.
export const erroEncerraSessao = (erro: unknown) => {
  const { status, codigo, action } = detalhesErroSessao(erro);
  return (status === 401 || status === 403)
    && (action === 'LOGOUT' || CODIGOS_SESSAO_ENCERRADA.includes(codigo.toUpperCase()));
};
