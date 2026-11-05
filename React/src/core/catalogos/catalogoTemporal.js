const texto = (valor) => String(valor ?? "").trim();

export const obterRotuloCatalogo = (item = {}, chaves = ["nome", "descricao", "codigo", "numero"]) => {
  for (const chave of chaves) {
    const valor = texto(item?.[chave]);
    if (valor) return valor;
  }
  return item?.id != null ? `#${item.id}` : "Item sem identificação";
};

export const adaptarOpcaoCatalogo = (item = {}, opcoes = {}) => ({
  id: item?.id ?? null,
  label: opcoes.rotulo ? opcoes.rotulo(item) : obterRotuloCatalogo(item, opcoes.chavesRotulo),
  ativo: item?.ativo !== false,
  historico: item?.ativo === false,
  metadata: { ...item },
});

export const opcoesParaNovaOperacao = (itens = [], { idSelecionado = null, rotulo, chavesRotulo } = {}) =>
  (Array.isArray(itens) ? itens : [])
    .filter((item) => item?.ativo !== false || (idSelecionado != null && String(item?.id) === String(idSelecionado)))
    .map((item) => adaptarOpcaoCatalogo(item, { rotulo, chavesRotulo }));

export const opcoesHistoricas = (itens = [], opcoes = {}) =>
  (Array.isArray(itens) ? itens : []).map((item) => adaptarOpcaoCatalogo(item, opcoes));

export const descricaoDisponibilidade = (item) => {
  if (typeof item?.ativo !== 'boolean' || item?.disponibilidadeComprovada === false) {
    return 'Disponibilidade desconhecida — fonte histórica insuficiente';
  }
  if (item.dataReferencia) return item.ativo ? 'Ativo na data consultada' : 'Inativo na data consultada';
  return item.ativo ? 'Ativo atualmente' : 'Inativo hoje — preservado para histórico';
};

export const limparDependencias = (estado, campoAlterado, dependencias = {}) => {
  const proximos = { ...estado };
  (dependencias[campoAlterado] || []).forEach((campo) => { proximos[campo] = ""; });
  return proximos;
};
