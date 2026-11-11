export const CHAVE_CONTRATOS_FAVORITOS = "sgt.empregados.contratosFavoritos";

const normalizarIds = (ids: unknown[] = []) =>
  ids
    .map((id) => String(id))
    .filter(Boolean)
    .sort((a, b) => a.localeCompare(b, "pt-BR"));

export const lerContratosFavoritos = (armazenamento: (Storage)) => {
  if (!armazenamento?.getItem) return [];

  try {
    const valor = armazenamento.getItem(CHAVE_CONTRATOS_FAVORITOS);
    if (!valor) return [];
    const itens: unknown = JSON.parse(valor);
    return Array.isArray(itens) ? normalizarIds(itens) : [];
  } catch {
    return [];
  }
};

export const salvarContratosFavoritos = (armazenamento: (Storage), ids: (string[])) => {
  const normalizados = normalizarIds(ids);
  if (!armazenamento?.setItem) return normalizados;

  try {
    armazenamento.setItem(CHAVE_CONTRATOS_FAVORITOS, JSON.stringify(normalizados));
  } catch {
    // armazenamento indisponível: a lista continua em memória
  }

  return normalizados;
};
