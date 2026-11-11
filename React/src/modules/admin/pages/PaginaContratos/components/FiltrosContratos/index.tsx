import React from "react";
import { FilterX, Search, SlidersHorizontal } from "lucide-react";
import estilos from "./index.module.css";

export const FiltrosContratos = ({
  termoBusca,
  filtroStatus,
  filtroUf,
  filtroGestaoLocal = "TODAS",
  filtroVigencia,
  ufsDisponiveis,
  gestoesLocaisDisponiveis = [],
  mostrarFiltroGestaoLocal = true,
  totalEncontrado,
  aoAlterarBusca,
  aoAlterarStatus,
  aoAlterarUf,
  aoAlterarGestaoLocal = (_id: string) => {},
  aoAlterarVigencia,
  aoLimparFiltros,
}: { termoBusca: (string); filtroStatus: (string); filtroUf: (string); filtroGestaoLocal?: (string); filtroVigencia: (string); ufsDisponiveis: (string[]); gestoesLocaisDisponiveis?: { id: string; nome: string; uf: string }[]; mostrarFiltroGestaoLocal?: (boolean); totalEncontrado: (number); aoAlterarBusca: (React.Dispatch<React.SetStateAction<string>>); aoAlterarStatus: (React.Dispatch<React.SetStateAction<string>>); aoAlterarUf: (React.Dispatch<React.SetStateAction<string>>); aoAlterarGestaoLocal?: (valor: string) => void; aoAlterarVigencia: (React.Dispatch<React.SetStateAction<string>>); aoLimparFiltros: (() => void) }) => {
  const haFiltrosAtivos =
    termoBusca || filtroStatus !== "TODOS" || filtroUf !== "TODAS"
    || filtroGestaoLocal !== "TODAS" || filtroVigencia !== "TODAS";

  return (
    <section className={estilos.painelFiltros} aria-label="Filtros de contratos">
      <div className={estilos.cabecalhoFiltros}>
        <div className={estilos.tituloFiltros}>
          <SlidersHorizontal size={17} />
          <span>Filtros e pesquisa</span>
          <span className={estilos.contadorResultado}>{totalEncontrado}</span>
        </div>

        {haFiltrosAtivos && (
          <button type="button" className={estilos.botaoLimpar} onClick={aoLimparFiltros}>
            <FilterX size={15} />
            Limpar filtros
          </button>
        )}
      </div>

      <div className={estilos.gradeFiltros}>
        <label className={`${estilos.grupoCampo} ${estilos.grupoBusca}`}>
          <span>Pesquisar</span>
          <div className={estilos.campoComIcone}>
            <Search size={17} />
            <input
              type="search"
              value={termoBusca}
              onChange={(evento) => aoAlterarBusca(evento.target.value)}
              placeholder="Número/ano, descrição, empresa, Gestão Local ou UF..."
            />
          </div>
        </label>

        <label className={estilos.grupoCampo}>
          <span>Status</span>
          <select value={filtroStatus} onChange={(evento) => aoAlterarStatus(evento.target.value)}>
            <option value="TODOS">Todos os estados</option>
            <option value="RASCUNHOS">Rascunhos</option>
            <option value="PUBLICADO">Publicados</option>
            <option value="CANCELADO">Cancelados</option>
            <option value="ANULADO">Anulados</option>
            <option value="ATIVOS">Administrativamente ativos</option>
            <option value="INATIVOS">Administrativamente inativos</option>
          </select>
        </label>

        <label className={estilos.grupoCampo}>
          <span>Estado (UF)</span>
          <select value={filtroUf} onChange={(evento) => aoAlterarUf(evento.target.value)}>
            <option value="TODAS">Todos os estados</option>
            {ufsDisponiveis.map((uf) => (
              <option key={uf} value={uf}>{uf}</option>
            ))}
          </select>
        </label>

        {mostrarFiltroGestaoLocal && (
          <label className={estilos.grupoCampo}>
            <span>Gestão Local</span>
            <select
              value={filtroGestaoLocal}
              onChange={(evento) => aoAlterarGestaoLocal(evento.target.value)}
            >
              <option value="TODAS">Todas as Gestões Locais</option>
              {gestoesLocaisDisponiveis.map((item) => (
                <option key={item.id} value={(item.id) ?? undefined}>{item.nome}</option>
              ))}
            </select>
          </label>
        )}

        <label className={estilos.grupoCampo}>
          <span>Vigência</span>
          <select value={filtroVigencia} onChange={(evento) => aoAlterarVigencia(evento.target.value)}>
            <option value="TODAS">Todas</option>
            <option value="RASCUNHO">Rascunho</option>
            <option value="FUTURO">Futuro</option>
            <option value="VIGENTE">Vigente</option>
            <option value="ENCERRADO">Encerrado</option>
            <option value="CANCELADO">Cancelado</option>
            <option value="ANULADO">Anulado</option>
            <option value="VENCENDO">Vencendo em até 90 dias</option>
          </select>
        </label>
      </div>
    </section>
  );
};
