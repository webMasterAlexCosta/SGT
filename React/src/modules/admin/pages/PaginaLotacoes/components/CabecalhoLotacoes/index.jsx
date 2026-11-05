import React from "react";
import { MapPinned, Plus } from "lucide-react";
import estilos from "./index.module.css";

export const CabecalhoLotacoes = ({ aoCriar, totalExibido, estadoSelecionado, podeEscrever = false }) => {
  const valorEstado =
    typeof estadoSelecionado === "string" || typeof estadoSelecionado === "number"
      ? String(estadoSelecionado)
      : estadoSelecionado?.uf || estadoSelecionado?.sigla || estadoSelecionado?.nome || "TODOS";
  const rotuloEstado = valorEstado === "TODOS" || valorEstado === "TODAS" ? "Brasil" : valorEstado;

  return (
    <header className={estilos.cabecalhoPagina}>
      <div className={estilos.blocoTitulo}>
        <div className={estilos.iconeDestaque}>
          <MapPinned size={25} />
        </div>
        <div className={estilos.textosTitulo}>
          <span className={estilos.sobretitulo}>Estrutura operacional</span>
          <h1>Gestão de Lotações</h1>
          <p>Organize unidades, municípios e turnos por estado e Gestão Local.</p>
        </div>
      </div>
      <div className={estilos.blocoAcoes}>
        <div className={estilos.resumoEscopo}>
          <span>{rotuloEstado}</span>
          <strong>{Number.isFinite(Number(totalExibido)) ? Number(totalExibido) : 0} lotação(ões)</strong>
        </div>
        {podeEscrever && <button type="button" onClick={aoCriar} className={estilos.botaoAcaoPrimaria}>
          <Plus size={18} /> Nova lotação
        </button>}
      </div>
    </header>
  );
};
