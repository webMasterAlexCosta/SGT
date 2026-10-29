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
  "SESSION_CLAIMS_ERROR",
  "SESSION_REPLACED",
  "SESSION_DEVICE_MISSING",
  "SESSION_DEVICE_MISMATCH",
  "USER_NOT_FOUND",
  "ACCOUNT_INACTIVE",
  "ACCOUNT_LOCKED",
]);

const MENSAGENS_SESSAO = Object.freeze({
  SESSION_REPLACED: "Sua sessão foi substituída por um novo acesso.",
  SESSION_EXPIRED: "Sua sessão expirou. Entre novamente.",
  SESSION_DEVICE_MISSING: "Não foi possível validar o dispositivo desta sessão.",
  SESSION_DEVICE_MISMATCH: "A sessão não pertence a este dispositivo.",
  ACCOUNT_INACTIVE: "Sua conta não está ativa.",
  ACCOUNT_LOCKED: "Sua conta está temporariamente bloqueada.",
});

export const estadoAutenticadoPorUsuario = (usuario) =>
  usuario?.forceChangePassword
    ? ESTADOS_AUTENTICACAO.SENHA_INICIAL_OBRIGATORIA
    : ESTADOS_AUTENTICACAO.AUTENTICADO;

export const mensagemSessaoEncerrada = (codigo, fallback = "Sessão encerrada. Entre novamente.") =>
  MENSAGENS_SESSAO[String(codigo || "").trim().toUpperCase()] || fallback;
