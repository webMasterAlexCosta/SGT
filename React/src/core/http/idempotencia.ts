import type { IdempotencyOption, PreparedIdempotency } from './types';
const uuid = () => {
  try {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
  } catch {}
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
};

export const gerarChaveIdempotencia = (prefixo = 'cmd') => {
  const prefixoSeguro = String(prefixo || 'cmd')
    .trim()
    .replace(/[^a-zA-Z0-9_-]/g, '-')
    .slice(0, 16) || 'cmd';
  return `${prefixoSeguro}-${uuid()}`.slice(0, 80);
};

export const prepararIdempotencia = (idempotencia?: IdempotencyOption): PreparedIdempotency | null => {
  if (!idempotencia) return null;
  if (typeof idempotencia === 'string') {
    const chave = idempotencia.trim();
    return chave ? Object.freeze({ chave, campo: 'chaveIdempotencia' }) : null;
  }
  if (idempotencia === true) {
    return Object.freeze({ chave: gerarChaveIdempotencia(), campo: 'chaveIdempotencia' });
  }
  if (typeof idempotencia === 'object') {
    const chave = String(idempotencia.chave || gerarChaveIdempotencia(idempotencia.prefixo)).trim();
    const campo = String(idempotencia.campo || 'chaveIdempotencia').trim();
    if (!chave || !campo) throw new TypeError('Configuração de idempotência inválida.');
    return Object.freeze({ chave, campo });
  }
  throw new TypeError('Configuração de idempotência inválida.');
};

export const aplicarIdempotenciaAoCorpo = (data: unknown, idempotenciaPreparada: PreparedIdempotency | null): unknown => {
  if (!idempotenciaPreparada) return data;
  if (data == null) return { [idempotenciaPreparada.campo]: idempotenciaPreparada.chave };
  if (typeof FormData !== 'undefined' && data instanceof FormData) {
    if (!data.has(idempotenciaPreparada.campo)) data.append(idempotenciaPreparada.campo, idempotenciaPreparada.chave);
    return data;
  }
  if (Array.isArray(data) || typeof data !== 'object') {
    throw new TypeError('Idempotência por campo exige corpo objeto ou FormData.');
  }
  if (Reflect.get(data, idempotenciaPreparada.campo) && Reflect.get(data, idempotenciaPreparada.campo) !== idempotenciaPreparada.chave) {
    throw new Error(`O campo ${idempotenciaPreparada.campo} já possui outra chave de idempotência.`);
  }
  return { ...data, [idempotenciaPreparada.campo]: idempotenciaPreparada.chave };
};
