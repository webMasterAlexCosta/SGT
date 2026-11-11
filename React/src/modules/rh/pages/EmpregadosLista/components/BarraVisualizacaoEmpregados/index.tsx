import React, { memo } from "react";
import {
  Eraser,
  Filter,
  LayoutGrid,
  List,
  Search,
  SlidersHorizontal,
  RefreshCw,
  Table2,
} from "lucide-react";
import { MODOS_VISUALIZACAO } from "../../visualizacao.ts";
import estilos from "./index.module.css";

const OPCOES_VISUALIZACAO = [
  {
    id: MODOS_VISUALIZACAO.TABELA,
    rotulo: "Tabela",
    descricao: "Mais detalhes por coluna",
    Icone: Table2,
  },
  {
    id: MODOS_VISUALIZACAO.LISTA,
    rotulo: "Lista",
    descricao: "Leitura rápida e compacta",
    Icone: List,
  },
  {
    id: MODOS_VISUALIZACAO.GRADE,
    rotulo: "Grade",
    descricao: "Cartões responsivos",
    Icone: LayoutGrid,
  },
];

export const BarraVisualizacaoEmpregados = memo(
  ({
    modoVisualizacao,
    aoMudarModo,
    quantidadeGrupos,
    quantidadePagina,
    termoBusca,
    aoMudarBusca,
    aoAcionarBusca,
    quantidadeContratosSelecionados,
    lotacaoSelecionadaNome,
    turnoSelecionadoNome,
    aoAbrirFiltros,
    aoLimparFiltros,
    filtrosAvancadosAbertos,
    aoAtualizar,
    carregando = false,
  }: { modoVisualizacao: string; aoMudarModo: (modo: string) => void; quantidadeGrupos: number; quantidadePagina: number; termoBusca: string; aoMudarBusca: (valor: string) => void; aoAcionarBusca: (valor: string) => void; quantidadeContratosSelecionados: number; lotacaoSelecionadaNome?: string; turnoSelecionadoNome?: string; aoAbrirFiltros: () => void; aoLimparFiltros: () => void; filtrosAvancadosAbertos?: boolean; aoAtualizar: () => void; carregando?: boolean }) => (
    <section className={estilos.barraVisualizacao} aria-label="Visualização dos colaboradores">
      <div className={estilos.blocoPesquisa}>
        <Search size={16} className={estilos.iconePesquisa} />
        <input
          type="search"
          value={termoBusca}
          onChange={(evento) => aoMudarBusca(evento.target.value)}
          onKeyDown={(evento) => {
            if (evento.key === "Enter") aoAcionarBusca(termoBusca);
          }}
          className={estilos.entradaPesquisa}
          placeholder="Buscar colaborador, CPF ou matrícula..."
        />
      </div>

      <div className={estilos.areaChips}>
        {quantidadeContratosSelecionados > 0 && (
          <span className={estilos.chipResumo}>
            <Filter size={13} /> {quantidadeContratosSelecionados} contrato(s)
          </span>
        )}
        {lotacaoSelecionadaNome && <span className={estilos.chipResumo}>{lotacaoSelecionadaNome}</span>}
        {turnoSelecionadoNome && <span className={estilos.chipResumo}>{turnoSelecionadoNome}</span>}
        <span className={estilos.chipResumoNeutro}>
          {quantidadeGrupos} grupos • {quantidadePagina} colaboradores
        </span>
      </div>

      <div className={estilos.areaAcoes}>
        <button
          type="button"
          className={`${estilos.botaoAcao} ${filtrosAvancadosAbertos ? estilos.botaoAcaoAtivo : ""}`}
          onClick={aoAbrirFiltros}
          title="Abrir filtros avançados"
          aria-haspopup="dialog"
          aria-expanded={filtrosAvancadosAbertos}
        >
          <SlidersHorizontal size={16} />
          <span className={estilos.rotuloBotaoFiltro}>Filtros</span>
        </button>

        <button type="button" className={estilos.botaoAcao} onClick={aoLimparFiltros} title="Limpar filtros">
          <Eraser size={16} />
          <span className={estilos.rotuloBotao}>Limpar</span>
        </button>


        <button
          type="button"
          className={estilos.botaoAcao}
          onClick={aoAtualizar}
          disabled={carregando}
          title="Atualizar resultados"
          aria-label="Atualizar resultados"
        >
          <RefreshCw size={16} className={carregando ? estilos.iconeGirando : ""} />
          <span className={estilos.rotuloBotao}>Atualizar</span>
        </button>
        <div
          className={estilos.seletorVisualizacao}
          role="group"
          aria-label="Modo de exibição"
        >
          {OPCOES_VISUALIZACAO.map(({ id, rotulo, descricao, Icone }) => {
            const selecionado = modoVisualizacao === id;
            return (
              <button
                key={id}
                type="button"
                className={`${estilos.botaoVisualizacao} ${selecionado ? estilos.botaoVisualizacaoAtivo : ""}`}
                onClick={() => aoMudarModo(id)}
                aria-pressed={selecionado}
                title={`${rotulo} — ${descricao}. A troca de visualização é local e não refaz a consulta ao servidor.`}
              >
                <Icone size={17} aria-hidden="true" />
                <span className={estilos.rotuloModo}>{rotulo}</span>
              </button>
            );
          })}
        </div>
      </div>
    </section>
  ),
);

BarraVisualizacaoEmpregados.displayName = "BarraVisualizacaoEmpregados";
