// src/core/events.js
import { CONFIG } from './config';
import { invalidarGeracaoSessao, obterGeracaoSessao, sessaoAtual } from './security/geracaoSessao.js';

export const EVENT_SHOW_TOAST = 'app-show-toast';
export const EVENT_LOGOUT = 'app-logout';

// --- EVENTOS DE CONEXÃO ---
export const EVENT_CONNECTION_LOST = 'app-connection-lost';
export const EVENT_CONNECTION_RESTORED = 'app-connection-restored';

let isRedirectingToLogin = false;

// Evita spam de toasts repetidos (ex.: falha de rede em loop)
const lastToast = {
  type: null,
  message: null,
  ts: 0
};

const deveSuprimirNotificacao = (type, message) => {
  const now = Date.now();
  const same = lastToast.type === type && lastToast.message === message;
  
  // <-- LIDO DO CONFIG (Evita o número mágico de ms de deduplicação)
  const windowMs = CONFIG.TOAST_DEDUPE_WINDOW_MS;
  
  if (same && now - lastToast.ts < windowMs) return true;
  
  lastToast.type = type;
  lastToast.message = message;
  lastToast.ts = now;
  return false;
};

export const notificar = (type, message) => {
  if (isRedirectingToLogin && type === 'error') return;
  if (!message) return;
  if (deveSuprimirNotificacao(type, message)) return;
  
  window.dispatchEvent(new CustomEvent(EVENT_SHOW_TOAST, { detail: { type, message } }));
};

export const dispararSaida = (message, meta = null) => {
  // Também os disparadores locais (ex.: idle) precisam verificar a revisão
  // externa antes de invalidar/adotar uma geração e solicitar revogação.
  if (!sessaoAtual(meta?.geracaoSessao ?? obterGeracaoSessao())) return;
  if (isRedirectingToLogin) return;
  isRedirectingToLogin = true;
  const geracaoSessao = invalidarGeracaoSessao();
  
  // Persiste motivo para exibir no Login após o redirect
  try {
    const payload = {
      message: message || null,
      code: meta?.code || null,
      ts: Date.now()
    };
    sessionStorage.setItem('logout_reason', JSON.stringify(payload));
  } catch {}

  if (message) notificar('error', message);
  window.dispatchEvent(new CustomEvent(EVENT_LOGOUT, { detail: { geracaoSessao } }));
};

export const redefinirIndicadorRedirecionamento = () => {
  isRedirectingToLogin = false;
};

export const obterStatusRedirecionamento = () => isRedirectingToLogin;

// --- FUNÇÕES DISPARADORAS DE REDE ---
export const dispararConexaoPerdida = () => {
  window.dispatchEvent(new CustomEvent(EVENT_CONNECTION_LOST));
};

export const dispararConexaoRestaurada = () => {
  window.dispatchEvent(new CustomEvent(EVENT_CONNECTION_RESTORED));
};
