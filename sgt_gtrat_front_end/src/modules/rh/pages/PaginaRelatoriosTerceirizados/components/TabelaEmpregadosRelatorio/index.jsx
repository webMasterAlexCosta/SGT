import React from "react";
import { ChevronLeft, ChevronRight, FileSearch, LoaderCircle } from "lucide-react";
import { formatarData, obterContratosDoEmpregado } from "../../utils";
import estilos from "./index.module.css";

const TAMANHOS_PAGINA = [25, 50, 100, 200, 1000, 2000, 5000];
const TAMANHO_TODOS = "TODOS";

const obterClasseStatus = (status) => {
  const valor = String(status || "").toUpperCase();
  if (valor === "ATIVO") return estilos.statusAtivo;
  if (valor.includes("FER")) return estilos.statusFerias;
  if (valor.includes("AFAST")) return estilos.statusAfastado;
  return estilos.statusInativo;
};

const formatarNumero = (valor) => Number(valor || 0).toLocaleString("pt-BR");

export const TabelaEmpregadosRelatorio = ({
  empregados,
  paginacao,
  carregando,
  tamanhoSelecionado,
  exibindoTodos,
  progressoCargaTotal,
  aoMudarPagina,
  aoMudarTamanho,
}) => {
  if (carregando && !empregados.length) {
    const temProgresso = exibindoTodos && progressoCargaTotal;
    return (
      <div className={estilos.estadoCarregamento}>
        <LoaderCircle size={22} className={estilos.girando} />
        <strong>{exibindoTodos ? "Carregando bloco de até 5.000 registros..." : "Consolidando dados no servidor..."}</strong>
        {temProgresso && progressoCargaTotal.totalRegistros > 0 && (
          <span>{formatarNumero(progressoCargaTotal.registrosCarregados)} de {formatarNumero(progressoCargaTotal.totalRegistros)} carregados</span>
        )}
      </div>
    );
  }

  if (!empregados.length) {
    return <div className={estilos.estadoVazio}><FileSearch size={27} /><strong>Nenhum empregado encontrado</strong><span>Altere os filtros para ampliar o resultado.</span></div>;
  }

  const paginaAtual = Number(paginacao?.number || 0);
  const totalPaginas = Number(paginacao?.totalPages || 0);
  const total = Number(paginacao?.totalElements || 0);
  const primeiro = Boolean(paginacao?.first);
  const ultimo = Boolean(paginacao?.last);
  const valorSelect = exibindoTodos ? TAMANHO_TODOS : String(tamanhoSelecionado || paginacao?.size || 100);

  return (
    <section className={estilos.cartaoTabela} aria-busy={carregando}>
      <header>
        <div>
          <strong>Quadro consolidado</strong>
          <span>{formatarNumero(total)} registro(s) no resultado · {formatarNumero(empregados.length)} {exibindoTodos ? "exibidos" : "nesta página"}</span>
          {carregando && <span className={estilos.atualizando}><LoaderCircle size={12} /> Atualizando</span>}
        </div>
        {exibindoTodos && (
          <small className={estilos.avisoTodos}>Modo “Todos” ativo: a base completa é navegada em blocos de até 5.000, sem carregar os 268 mil registros simultaneamente no navegador.</small>
        )}
      </header>

      <div className={estilos.tabelaResponsiva}>
        <table>
          <thead><tr><th>Empregado</th><th>Situação</th><th>Cargo e lotação</th><th>Turno</th><th>Admissão</th><th>Contrato(s)</th></tr></thead>
          <tbody>
            {empregados.map((empregado) => {
              const contratos = obterContratosDoEmpregado(empregado);
              const contratosVigentes = contratos.filter((contrato) => contrato.vigente !== false);
              const contratosHistoricos = contratos.filter((contrato) => contrato.vigente === false);
              return (
                <tr key={empregado.id}>
                  <td data-label="Empregado"><div className={estilos.identificacao}><strong>{empregado.nome || "Sem nome"}</strong><span>{empregado.matricula || "Sem matrícula"}{empregado.cpf ? ` • ${empregado.cpf}` : ""}</span></div></td>
                  <td data-label="Situação"><span className={`${estilos.status} ${obterClasseStatus(empregado.statusDescricao)}`}>{empregado.statusDescricao || "Sem status"}</span></td>
                  <td data-label="Cargo e lotação"><div className={estilos.textoDuplo}><strong>{empregado.cargoNome || "Sem cargo"}</strong><span>{empregado.lotacaoNome || "Sem lotação"}</span></div></td>
                  <td data-label="Turno">{empregado.jornadaDescricao || "Sem turno"}</td>
                  <td data-label="Admissão">{formatarData(empregado.dataAdmissao)}</td>
                  <td data-label="Contrato(s)">
                    <div className={estilos.listaContratos}>
                      {contratosVigentes.map((contrato) => <span key={contrato.id || contrato.numeroContrato}>{contrato.numeroContrato || contrato.descricao || "Contrato"}</span>)}
                      {!contratosVigentes.length && <span className={estilos.semContrato}>Sem contrato vigente</span>}
                      {contratosHistoricos.map((contrato) => (
                        <span key={`historico-${contrato.id || contrato.numeroContrato}`} className={estilos.contratoInvalido} title="Vínculo histórico/sem vigência na data de referência">
                          Histórico: {contrato.numeroContrato || contrato.descricao || "Contrato"}
                        </span>
                      ))}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <footer className={estilos.rodapePaginacao}>
        <label>
          <span>Linhas por página</span>
          <select value={valorSelect} onChange={(e) => aoMudarTamanho(e.target.value)} disabled={carregando}>
            {TAMANHOS_PAGINA.map((valor) => <option key={valor} value={valor}>{formatarNumero(valor)}</option>)}
            <option value={TAMANHO_TODOS}>Todos</option>
          </select>
        </label>
        <div className={estilos.controlesPagina}>
          <>
            <span>{exibindoTodos ? "Bloco" : "Página"} {totalPaginas ? paginaAtual + 1 : 0} de {formatarNumero(totalPaginas)}</span>
            <button type="button" onClick={() => aoMudarPagina(Math.max(0, paginaAtual - 1))} disabled={carregando || primeiro} aria-label="Página anterior"><ChevronLeft size={16} /></button>
            <button type="button" onClick={() => aoMudarPagina(paginaAtual + 1)} disabled={carregando || ultimo} aria-label="Próxima página"><ChevronRight size={16} /></button>
          </>
        </div>
      </footer>
    </section>
  );
};
