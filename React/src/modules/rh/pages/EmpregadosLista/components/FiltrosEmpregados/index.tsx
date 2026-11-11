import type { useEmpregadosLista } from '../../useEmpregadosLista';
type EmployeeModel = ReturnType<typeof useEmpregadosLista>;
import React, { memo, useDeferredValue, useEffect, useMemo, useState } from "react";
import {
  FileText,
  Search,
  Star,
  Users,
  AlertCircle,
  X,
} from "lucide-react";
import estilos from "./index.module.css";

const normalizarTexto = (valor = "") =>
  String(valor)
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("pt-BR")
    .trim();

const ABAS = {
  TODOS: "todos",
  FAVORITOS: "favoritos",
  SELECIONADOS: "selecionados",
};

export const FiltrosEmpregados = memo(
  ({
    filtrosDisponiveis,
    filtrosGrupoAtivos,
    alternarFiltro,
    gruposBase,
    contratosFavoritos,
    aoAlternarFavoritoContrato,
  }: { filtrosDisponiveis: EmployeeModel["processamento"]["filtrosDisponiveis"]; filtrosGrupoAtivos: string[]; alternarFiltro: (id: string) => void; gruposBase: EmployeeModel["processamento"]["gruposBase"]; contratosFavoritos: string[]; aoAlternarFavoritoContrato: (id: string) => void }) => {
    const [termoPesquisa, definirTermoPesquisa] = useState("");
    const [abaAtiva, definirAbaAtiva] = useState(ABAS.TODOS);
    const [limiteVisivel, definirLimiteVisivel] = useState(60);
    const termoPesquisaDiferido = useDeferredValue(termoPesquisa);

    const contagemSemContratoNaPagina = useMemo(
      () => gruposBase.find((grupo) => String(grupo.id) === "sem_contrato")?.stats?.total || 0,
      [gruposBase],
    );

    const opcoesComContagem = useMemo(
      () =>
        filtrosDisponiveis.map((filtro) => ({
          ...filtro,
          id: String(filtro.id),
          quantidade: filtro.quantidadeColaboradores == null
            ? contagemSemContratoNaPagina
            : Number(filtro.quantidadeColaboradores || 0),
          favorito: Boolean(filtro.favorito),
        })),
      [filtrosDisponiveis, contagemSemContratoNaPagina],
    );

    const opcoesFiltradas = useMemo(() => {
      const termoNormalizado = normalizarTexto(termoPesquisaDiferido);
      let lista = opcoesComContagem;

      if (abaAtiva === ABAS.FAVORITOS) {
        lista = lista.filter((item) => item.favorito);
      }
      if (abaAtiva === ABAS.SELECIONADOS) {
        const ativos = new Set(filtrosGrupoAtivos.map(String));
        lista = lista.filter((item) => ativos.has(String(item.id)));
      }
      if (!termoNormalizado) return lista;

      return lista.filter((opcao) =>
        String(opcao.indiceBusca || normalizarTexto(`${opcao.titulo} ${opcao.descricao || ""}`))
          .includes(termoNormalizado),
      );
    }, [abaAtiva, opcoesComContagem, filtrosGrupoAtivos, termoPesquisaDiferido]);

    useEffect(() => {
      definirLimiteVisivel(60);
    }, [abaAtiva, termoPesquisaDiferido]);

    const opcoesVisiveis = opcoesFiltradas.slice(0, limiteVisivel);
    const haMaisOpcoes = opcoesFiltradas.length > limiteVisivel;

    const favoritosTotal = contratosFavoritos.length;

    return (
      <aside className={estilos.painelContratos} aria-label="Contratos">
        <div className={estilos.cabecalhoPainel}>
          <div>
            <h3 className={estilos.tituloPainel}>Contratos</h3>
          </div>
          <span className={estilos.emblemaTotal}>{filtrosDisponiveis.length}</span>
        </div>

        <div className={estilos.abasPainel} role="tablist" aria-label="Navegação de contratos">
          <button
            type="button"
            role="tab"
            aria-selected={abaAtiva === ABAS.TODOS}
            className={`${estilos.abaPainel} ${abaAtiva === ABAS.TODOS ? estilos.abaPainelAtiva : ""}`}
            onClick={() => definirAbaAtiva(ABAS.TODOS)}
          >
            Todos
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={abaAtiva === ABAS.FAVORITOS}
            className={`${estilos.abaPainel} ${abaAtiva === ABAS.FAVORITOS ? estilos.abaPainelAtiva : ""}`}
            onClick={() => definirAbaAtiva(ABAS.FAVORITOS)}
          >
            Favoritos {favoritosTotal > 0 ? favoritosTotal : ""}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={abaAtiva === ABAS.SELECIONADOS}
            className={`${estilos.abaPainel} ${abaAtiva === ABAS.SELECIONADOS ? estilos.abaPainelAtiva : ""}`}
            onClick={() => definirAbaAtiva(ABAS.SELECIONADOS)}
          >
            Selecionado {filtrosGrupoAtivos.length > 0 ? filtrosGrupoAtivos.length : ""}
          </button>
        </div>

        <div className={estilos.areaPesquisa}>
          <Search size={16} className={estilos.iconePesquisa} />
          <input
            type="search"
            value={termoPesquisa}
            onChange={(evento) => definirTermoPesquisa(evento.target.value)}
            className={estilos.entradaPesquisa}
            placeholder="Pesquisar contrato..."
          />
          {termoPesquisa && (
            <button
              type="button"
              className={estilos.botaoLimparPesquisa}
              onClick={() => definirTermoPesquisa("")}
              aria-label="Limpar pesquisa"
            >
              <X size={14} />
            </button>
          )}
        </div>

        <div className={estilos.listaContratos} role="list">
          <button
            type="button"
            className={`${estilos.itemContrato} ${filtrosGrupoAtivos.length === 0 ? estilos.itemContratoAtivo : ""}`}
            onClick={() => alternarFiltro("limpar")}
          >
            <span className={estilos.iconeContratoPadrao} aria-hidden="true">
              <FileText size={16} />
            </span>
            <span className={estilos.textosContrato}>
              <strong className={estilos.tituloContrato}>Todos os contratos</strong>
              <span className={estilos.descricaoContrato}>Exibir todos os registros disponíveis</span>
            </span>
          </button>

          {opcoesFiltradas.length === 0 ? (
            <div className={estilos.estadoVazioLista}>Nenhum contrato encontrado.</div>
          ) : (
            opcoesVisiveis.map((filtro) => {
              const estaAtivo = filtrosGrupoAtivos.includes(String(filtro.id));
              const quantidade = filtro.quantidade || 0;
              const tituloOpcao = `${filtro.titulo}${filtro.descricao ? ` — ${filtro.descricao}` : ""}`;

              return (
                <div
                  key={filtro.id}
                  className={`${estilos.itemContrato} ${estaAtivo ? estilos.itemContratoAtivo : ""} ${filtro.isWarning ? estilos.itemContratoAviso : ""}`}
                  role="listitem"
                >
                  <button
                    type="button"
                    className={estilos.botaoSelecionarContrato}
                    onClick={() => alternarFiltro(filtro.id)}
                    aria-pressed={estaAtivo}
                    title={`${tituloOpcao} — ${quantidade.toLocaleString("pt-BR")} colaborador(es) no contrato`}
                  >
                    <span className={`${estilos.iconeContrato} ${filtro.isWarning ? estilos.iconeContratoAviso : ""}`} aria-hidden="true">
                      {filtro.isWarning ? <AlertCircle size={16} /> : <FileText size={16} />}
                    </span>
                    <span className={estilos.textosContrato}>
                      <strong className={estilos.tituloContrato}>{filtro.titulo}</strong>
                      {filtro.descricao && (
                        <span className={estilos.descricaoContrato}>{filtro.descricao}</span>
                      )}
                    </span>
                    <span className={estilos.marcadoresContrato}>
                      <span className={estilos.chipQtd}>
                        <Users size={12} /> {quantidade.toLocaleString("pt-BR")}
                      </span>
                    </span>
                  </button>

                  {!filtro.isWarning && (
                    <button
                      type="button"
                      className={`${estilos.botaoFavorito} ${filtro.favorito ? estilos.botaoFavoritoAtivo : ""}`}
                      onClick={() => aoAlternarFavoritoContrato(filtro.id)}
                      aria-pressed={Boolean(filtro.favorito)}
                      aria-label={filtro.favorito ? `Desfavoritar ${filtro.titulo}` : `Favoritar ${filtro.titulo}`}
                      title={filtro.favorito ? "Remover dos favoritos" : "Salvar nos favoritos"}
                    >
                      <Star size={15} fill={filtro.favorito ? "currentColor" : "none"} />
                    </button>
                  )}
                </div>
              );
            })
          )}

          {haMaisOpcoes && (
            <button
              type="button"
              className={estilos.botaoMostrarMais}
              onClick={() => definirLimiteVisivel((valor) => valor + 60)}
            >
              Mostrar mais {Math.min(60, opcoesFiltradas.length - limiteVisivel)}
            </button>
          )}
        </div>

        {filtrosGrupoAtivos.length > 0 && (
          <button type="button" className={estilos.botaoLimparSelecao} onClick={() => alternarFiltro("limpar")}>
            Limpar seleção
          </button>
        )}
      </aside>
    );
  },
);

FiltrosEmpregados.displayName = "FiltrosEmpregados";
