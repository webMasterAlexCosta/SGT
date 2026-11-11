interface CatalogItem {
  id?: string | number | null;
  ativo?: boolean | null;
  disponibilidadeComprovada?: boolean;
  dataReferencia?: string | null;
}
interface CatalogOptions<T extends CatalogItem> {
  rotulo?: (item: T) => string;
  chavesRotulo?: string[];
  idSelecionado?: string | number | null;
}
const texto = (valor: unknown) => String(valor ?? "").trim();

export const obterRotuloCatalogo = (item: CatalogItem = {}, chaves: string[] = ["nome", "descricao", "codigo", "numero"]) => {
  for (const chave of chaves) {
    const valor = texto(item && Reflect.get(item, chave));
    if (valor) return valor;
  }
  return item?.id != null ? `#${item.id}` : "Item sem identificação";
};

export const adaptarOpcaoCatalogo = <T extends CatalogItem>(item: T, opcoes: CatalogOptions<T> = {}) => ({
  id: item?.id ?? null,
  label: opcoes.rotulo ? opcoes.rotulo(item) : obterRotuloCatalogo(item, opcoes.chavesRotulo),
  ativo: item?.ativo !== false,
  historico: item?.ativo === false,
  metadata: { ...item },
});

export const opcoesParaNovaOperacao = <T extends CatalogItem>(itens: T[] = [], { idSelecionado = null, rotulo, chavesRotulo }: CatalogOptions<T> = {}) =>
  (Array.isArray(itens) ? itens : [])
    .filter((item) => item?.ativo !== false || (idSelecionado != null && String(item?.id) === String(idSelecionado)))
    .map((item) => adaptarOpcaoCatalogo(item, { rotulo, chavesRotulo }));

export const opcoesHistoricas = <T extends CatalogItem>(itens: T[] = [], opcoes: CatalogOptions<T> = {}) =>
  (Array.isArray(itens) ? itens : []).map((item) => adaptarOpcaoCatalogo(item, opcoes));

export const descricaoDisponibilidade = (item: CatalogItem | null | undefined) => {
  if (typeof item?.ativo !== 'boolean' || item?.disponibilidadeComprovada === false) {
    return 'Disponibilidade desconhecida — fonte histórica insuficiente';
  }
  if (item.dataReferencia) return item.ativo ? 'Ativo na data consultada' : 'Inativo na data consultada';
  return item.ativo ? 'Ativo atualmente' : 'Inativo hoje — preservado para histórico';
};

export const limparDependencias = <T extends Record<string, unknown>>(estado: T, campoAlterado: string, dependencias: Record<string, string[]> = {}) => {
  const proximos = { ...estado };
  (dependencias[campoAlterado] || []).forEach((campo) => { Reflect.set(proximos, campo, ""); });
  return proximos;
};
