import React, { memo, useMemo } from "react";
import {
  ChevronsLeft,
  ChevronLeft,
  ChevronRight,
  ChevronsRight,
} from "lucide-react";
import estilos from "./index.module.css";

const criarPaginasVisiveis = (paginaAtual, totalPaginas) => {
  if (totalPaginas <= 7) {
    return Array.from({ length: totalPaginas }, (_, indice) => indice);
  }

  const paginas = new Set([
    0,
    totalPaginas - 1,
    paginaAtual - 1,
    paginaAtual,
    paginaAtual + 1,
  ]);

  return [...paginas]
    .filter((pagina) => pagina >= 0 && pagina < totalPaginas)
    .sort((primeira, segunda) => primeira - segunda);
};

export const PaginacaoEmpregados = memo(
  ({
    paginaAtual,
    totalPaginas,
    tamanhoPagina,
    totalElementos,
    quantidadeExibida,
    carregarPagina,
    carregando = false,
    duracaoUltimaBuscaMs = null,
  }) => {
    const paginasVisiveis = useMemo(
      () => criarPaginasVisiveis(paginaAtual, totalPaginas),
      [paginaAtual, totalPaginas],
    );

    if (totalPaginas <= 1 || totalElementos === 0) return null;

    const primeiroRegistro = paginaAtual * tamanhoPagina + 1;
    const ultimoRegistro = Math.min(
      paginaAtual * tamanhoPagina + quantidadeExibida,
      totalElementos,
    );

    return (
      <nav className={estilos.cartaoPaginacao} aria-label="Paginação de colaboradores">
        <div className={estilos.informacaoPaginacao}>
          Registros{" "}
          <span className={estilos.destaquePaginacao}>{primeiroRegistro}</span>
          {"–"}
          <span className={estilos.destaquePaginacao}>{ultimoRegistro}</span>{" "}
          de <span className={estilos.destaquePaginacao}>{totalElementos}</span>
          <span className={estilos.totalPaginacao}>
            Página {paginaAtual + 1} de {totalPaginas}
          </span>
          {Number.isFinite(duracaoUltimaBuscaMs) && (
            <span className={estilos.tempoConsulta}>
              Carregado em {(duracaoUltimaBuscaMs / 1000).toLocaleString("pt-BR", {
                minimumFractionDigits: 1,
                maximumFractionDigits: 1,
              })} s
            </span>
          )}
        </div>

        <div className={estilos.acoesPaginacao}>
          <button
            type="button"
            onClick={() => carregarPagina(0)}
            disabled={carregando || paginaAtual === 0}
            className={estilos.botaoPaginaIcone}
            aria-label="Primeira página"
            title="Primeira página"
          >
            <ChevronsLeft size={17} />
          </button>

          <button
            type="button"
            onClick={() => carregarPagina(paginaAtual - 1)}
            disabled={carregando || paginaAtual === 0}
            className={estilos.botaoPagina}
          >
            <ChevronLeft size={16} /> Anterior
          </button>

          <div className={estilos.listaPaginas}>
            {paginasVisiveis.map((pagina, indice) => {
              const paginaAnterior = paginasVisiveis[indice - 1];
              const precisaSeparador =
                indice > 0 && paginaAnterior !== undefined && pagina - paginaAnterior > 1;

              return (
                <React.Fragment key={pagina}>
                  {precisaSeparador && (
                    <span className={estilos.separadorPaginas}>…</span>
                  )}
                  <button
                    type="button"
                    onClick={() => carregarPagina(pagina)}
                    disabled={carregando || pagina === paginaAtual}
                    className={`${estilos.botaoNumeroPagina} ${
                      pagina === paginaAtual ? estilos.botaoNumeroPaginaAtivo : ""
                    }`}
                    aria-current={pagina === paginaAtual ? "page" : undefined}
                    aria-label={`Ir para a página ${pagina + 1}`}
                  >
                    {pagina + 1}
                  </button>
                </React.Fragment>
              );
            })}
          </div>

          <button
            type="button"
            onClick={() => carregarPagina(paginaAtual + 1)}
            disabled={carregando || paginaAtual >= totalPaginas - 1}
            className={estilos.botaoPagina}
          >
            Próxima <ChevronRight size={16} />
          </button>

          <button
            type="button"
            onClick={() => carregarPagina(totalPaginas - 1)}
            disabled={carregando || paginaAtual >= totalPaginas - 1}
            className={estilos.botaoPaginaIcone}
            aria-label="Última página"
            title="Última página"
          >
            <ChevronsRight size={17} />
          </button>
        </div>
      </nav>
    );
  },
);

PaginacaoEmpregados.displayName = "PaginacaoEmpregados";
