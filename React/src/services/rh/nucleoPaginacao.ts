import type { Page } from '../../contracts/shared';
interface PageLoaderOptions<T> { buscarPagina: (pagina: number, tamanho: number, omitirContagem: boolean) => Promise<Pick<Page<T>, 'content'> & Partial<Page<T>>>; tamanhoPagina: number; maximoItens: number; concorrencia?: number; }
export async function carregarTodasAsPaginas<T>({
  buscarPagina,
  tamanhoPagina,
  maximoItens,
  concorrencia = 4,
}: PageLoaderOptions<T>): Promise<T[]> {
  if (typeof buscarPagina !== "function") throw new TypeError("buscarPagina deve ser uma função.");
  if (!Number.isInteger(tamanhoPagina) || tamanhoPagina <= 0) throw new RangeError("tamanhoPagina deve ser positivo.");
  if (!Number.isInteger(maximoItens) || maximoItens <= 0) throw new RangeError("maximoItens deve ser positivo.");
  if (!Number.isInteger(concorrencia) || concorrencia <= 0) throw new RangeError("concorrencia deve ser positiva.");

  const tamanhoSolicitado = Math.min(tamanhoPagina, maximoItens);
  const primeira = await buscarPagina(0, tamanhoSolicitado, false);
  const conteudoInicial = Array.isArray(primeira?.content) ? primeira.content : [];
  const totalElements = Number(primeira?.totalElements ?? conteudoInicial.length);

  if (totalElements > maximoItens) {
    throw new Error(
      `A consulta encontrou ${totalElements.toLocaleString("pt-BR")} registros e ultrapassou o limite seguro de ${maximoItens.toLocaleString("pt-BR")}. Refine os filtros organizacionais.`,
    );
  }

  const itens = [...conteudoInicial];
  const totalPages = Math.max(1, Number(primeira?.totalPages ?? 1));
  if (totalPages <= 1 || conteudoInicial.length === 0) return itens;

  for (let inicio = 1; inicio < totalPages; inicio += concorrencia) {
    const fim = Math.min(totalPages, inicio + concorrencia);
    const requisicoes: Promise<{ pagina: number; dados: Awaited<ReturnType<PageLoaderOptions<T>["buscarPagina"]>> }>[] = [];
    for (let pagina = inicio; pagina < fim; pagina += 1) {
      requisicoes.push(
        Promise.resolve(buscarPagina(pagina, tamanhoSolicitado, true))
          .then((dados) => ({ pagina, dados })),
      );
    }
    const paginas = await Promise.all(requisicoes);
    paginas.sort((a, b) => a.pagina - b.pagina);
    for (const pagina of paginas) {
      if (Array.isArray(pagina.dados?.content)) itens.push(...pagina.dados.content);
    }
  }
  return itens.slice(0, maximoItens);
}
