export const gerarIdRequisicao = () => {
  try {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
  } catch {
  }
  return `req_${Date.now()}_${Math.random().toString(16).slice(2)}`;
};
