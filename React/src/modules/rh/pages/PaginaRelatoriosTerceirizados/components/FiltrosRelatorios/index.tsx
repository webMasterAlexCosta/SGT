import React from "react";
import { FilterX, Search, SlidersHorizontal } from "lucide-react";
import estilos from "./index.module.css";

export const FiltrosRelatorios = ({
  termoBusca,
  statusSelecionado,
  contratoSelecionado,
  lotacaoSelecionada,
  statusDisponiveis,
  contratos,
  lotacoes,
  totalFiltrado,
  aoAlterarBusca,
  aoAlterarStatus,
  aoAlterarContrato,
  aoAlterarLotacao,
  aoLimpar,
  carregando,
}: { termoBusca: (string); statusSelecionado: (string); contratoSelecionado: (string); lotacaoSelecionada: (string); statusDisponiveis: ((string | null)[]); contratos: (import("../../../../../../contracts/java").RelatorioTerceirizadosDTOOpcao[]); lotacoes: (import("../../../../../../contracts/java").RelatorioTerceirizadosDTOOpcao[]); totalFiltrado: (number); aoAlterarBusca: (React.Dispatch<React.SetStateAction<string>>); aoAlterarStatus: (valor: string) => void; aoAlterarContrato: (valor: string) => void; aoAlterarLotacao: (valor: string) => void; aoLimpar: (() => void); carregando: (boolean) }) => {
  const possuiFiltro = termoBusca || statusSelecionado !== "TODOS" || contratoSelecionado !== "TODOS" || lotacaoSelecionada !== "TODAS";
  return (
    <section className={estilos.painelFiltros}>
      <div className={estilos.cabecalhoFiltros}>
        <div><SlidersHorizontal size={17} /><strong>Filtros do relatório</strong><span>{Number(totalFiltrado || 0).toLocaleString("pt-BR")}</span></div>
        {possuiFiltro && <button type="button" onClick={aoLimpar}><FilterX size={15} />Limpar</button>}
      </div>
      <div className={estilos.gradeFiltros}>
        <label className={`${estilos.grupoCampo} ${estilos.campoBusca}`}>
          <span>Pesquisar empregado</span>
          <div className={estilos.entradaComIcone}><Search size={16} /><input value={termoBusca} onChange={(e) => aoAlterarBusca(e.target.value)} placeholder="Nome, matrícula, CPF, cargo ou contrato..." aria-busy={carregando} /></div>
        </label>
        <label className={estilos.grupoCampo}><span>Status</span><select value={statusSelecionado} onChange={(e) => aoAlterarStatus(e.target.value)}><option value="TODOS">Todos</option>{statusDisponiveis.map((status) => <option key={status} value={(status) ?? undefined}>{status}</option>)}</select></label>
        <label className={estilos.grupoCampo}><span>Contrato</span><select value={contratoSelecionado} onChange={(e) => aoAlterarContrato(e.target.value)}><option value="TODOS">Todos</option><option value="SEM_CONTRATO">Sem contrato vigente</option>{contratos.map((contrato) => <option key={contrato.id} value={(contrato.id) ?? undefined}>{contrato.nome || `Contrato ${contrato.id}`}</option>)}</select></label>
        <label className={estilos.grupoCampo}><span>Lotação</span><select value={lotacaoSelecionada} onChange={(e) => aoAlterarLotacao(e.target.value)}><option value="TODAS">Todas</option>{lotacoes.map((lotacao) => <option key={lotacao.id} value={(lotacao.id) ?? undefined}>{lotacao.nome}</option>)}</select></label>
      </div>
    </section>
  );
};
