export const CHAVE_CONTRATOS_FAVORITOS = "sgt.empregados.contratosFavoritos";

const normalizarIds = (ids = []) =>
  ids
    .map((id) => String(id))
    .filter(Boolean)
    .sort((a, b) => a.localeCompare(b, "pt-BR"));

export const lerContratosFavoritos = (armazenamento) => {
  if (!armazenamento?.getItem) return [];

  try {
    const valor = armazenamento.getItem(CHAVE_CONTRATOS_FAVORITOS);
    if (!valor) return [];
    const itens = JSON.parse(valor);
    return Array.isArray(itens) ? normalizarIds(itens) : [];
  } catch {
    return [];
  }
};

export const salvarContratosFavoritos = (armazenamento, ids) => {
  const normalizados = normalizarIds(ids);
  if (!armazenamento?.setItem) return normalizados;

  try {
    armazenamento.setItem(CHAVE_CONTRATOS_FAVORITOS, JSON.stringify(normalizados));
  } catch {
    // armazenamento indisponível: a lista continua em memória
  }

  return normalizados;
};
