const UM_DIA_MS = 24 * 60 * 60 * 1000;

export const TWO_FACTOR_REMINDER_DAYS = 7;
export const TWO_FACTOR_REMINDER_INTERVAL_MS = TWO_FACTOR_REMINDER_DAYS * UM_DIA_MS;

const normalizarTrechoChave = (valor) =>
  String(valor ?? "")
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, "_")
    .replace(/^_+|_+$/g, "");

export const obterIdentificadorLembrete2FA = (usuario) => {
  const candidato = usuario?.id ?? usuario?.matricula ?? usuario?.username;
  return normalizarTrechoChave(candidato) || "usuario";
};

export const criarChaveLembrete2FA = (appName, usuario) => {
  const app = normalizarTrechoChave(appName) || "app";
  return `${app}_2fa_reminder_${obterIdentificadorLembrete2FA(usuario)}`;
};

export const calcularProximoLembrete2FA = (
  agora = Date.now(),
  intervaloMs = TWO_FACTOR_REMINDER_INTERVAL_MS,
) => Number(agora) + Number(intervaloMs);

export const deveExibirLembrete2FA = ({
  ativo,
  proximoLembreteEm,
  agora = Date.now(),
}) => {
  if (Boolean(ativo)) return false;
  const proximo = Number(proximoLembreteEm);
  if (!Number.isFinite(proximo) || proximo <= 0) return true;
  return Number(agora) >= proximo;
};

export const lerProximoLembrete2FA = (storage, chave) => {
  try {
    return Number(storage?.getItem?.(chave)) || 0;
  } catch {
    return 0;
  }
};

export const salvarProximoLembrete2FA = (storage, chave, instante) => {
  try {
    storage?.setItem?.(chave, String(instante));
    return true;
  } catch {
    return false;
  }
};

export const limparLembrete2FA = (storage, chave) => {
  try {
    storage?.removeItem?.(chave);
  } catch {}
};
