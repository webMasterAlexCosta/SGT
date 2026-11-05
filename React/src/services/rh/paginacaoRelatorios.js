/**
 * Separa a página visual dos blocos mínimos aceitos pelo Java congelado.
 * As opções visuais são alinhadas a um único bloco; nenhuma consulta completa
 * ou exportação passa por este adaptador.
 */
export const prepararPaginaVisual = (params = {}, minimo, maximo) => {
  const tamanho = Math.min(maximo, Math.max(1, Math.trunc(Number(params.size) || 100)));
  const pagina = Math.max(0, Math.trunc(Number(params.page) || 0));
  const tamanhoBloco = Math.max(tamanho, Math.ceil(minimo / tamanho) * tamanho);
  return {
    pagina,
    tamanho,
    params: { ...params, page: Math.floor(pagina * tamanho / tamanhoBloco), size: tamanhoBloco },
  };
};

export const recortarPaginaVisual = (itens, metadados, consulta) => {
  const total = Math.max(0, Number(metadados.total) || 0);
  const totalPaginas = Math.ceil(total / consulta.tamanho);
  const pagina = totalPaginas ? Math.min(consulta.pagina, totalPaginas - 1) : 0;
  const tamanhoBloco = Number(metadados.tamanho) || consulta.params.size;
  const paginaBloco = Number(metadados.pagina) || 0;
  const inicio = Math.max(0, pagina * consulta.tamanho - paginaBloco * tamanhoBloco);
  return {
    itens: itens.slice(inicio, inicio + consulta.tamanho),
    pagina,
    tamanho: consulta.tamanho,
    total,
    totalPaginas,
    primeira: pagina === 0,
    ultima: totalPaginas === 0 || pagina + 1 >= totalPaginas,
  };
};

// O Java pagina colaboradores, mas devolve uma linha por contexto de alocação.
// Agrupar pela identidade preserva todas as linhas, inclusive as não contíguas.
export const agruparContextosPorColaborador = (linhas = []) => {
  const grupos = new Map();
  linhas.forEach((linha, indice) => {
    const id = linha.empregadoId ?? linha.id ?? `linha-${indice}`;
    if (!grupos.has(id)) grupos.set(id, { id, linhas: [] });
    grupos.get(id).linhas.push(linha);
  });
  return [...grupos.values()];
};
