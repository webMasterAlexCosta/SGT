import React from "react";
import { Search, SlidersHorizontal, X } from "lucide-react";
import { FiltroEstado } from "../../../../components/FiltroEstado";
import estilos from "./index.module.css";

export const FiltrosLotacoes = ({
  termoBusca,
  aoMudarBusca,
  exibirFiltroEstado,
  estadoSelecionado,
  aoMudarEstado,
  contagensPorEstado,
  totalResultados,
}: { termoBusca: string; aoMudarBusca: React.Dispatch<React.SetStateAction<string>>; exibirFiltroEstado: boolean; estadoSelecionado: string; aoMudarEstado: React.Dispatch<React.SetStateAction<string>>; contagensPorEstado: Record<string, number | undefined>; totalResultados: number }) => (
  <section className={estilos.painelFiltros} aria-label="Filtros de lotações">
    <div className={estilos.tituloPainel}>
      <SlidersHorizontal size={16} />
      <div>
        <strong>Organizar visualização</strong>
        <span>{totalResultados} resultado(s) exibidos</span>
      </div>
    </div>
    <div className={estilos.controles}>
      {exibirFiltroEstado && (
        <FiltroEstado
          id="filtro-estado-lotacoes"
          valor={estadoSelecionado}
          aoMudar={aoMudarEstado}
          contagens={contagensPorEstado}
          rotulo="Estado das lotações"
        />
      )}
      <div className={estilos.grupoBusca}>
        <label htmlFor="busca-lotacoes">Pesquisar no estado selecionado</label>
        <div className={estilos.campoBusca}>
          <Search size={17} />
          <input
            id="busca-lotacoes"
            type="search"
            placeholder="Lotação, município, gestor ou turno..."
            value={termoBusca}
            onChange={(evento) => aoMudarBusca(evento.target.value)}
          />
          {termoBusca && (
            <button type="button" onClick={() => aoMudarBusca("")} aria-label="Limpar pesquisa">
              <X size={15} />
            </button>
          )}
        </div>
      </div>
    </div>
  </section>
);
