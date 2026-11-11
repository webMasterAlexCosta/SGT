import React from "react";
import { CONFIG } from "../../../core/config";
import estilos from "../index.module.css";

const formatarLinhaHistorico = (valor: (string)) => {
  if (valor === null || valor === undefined) return "";
  if (typeof valor === "string" || typeof valor === "number" || typeof valor === "boolean") {
    return String(valor);
  }
  try {
    return JSON.stringify(valor);
  } catch {
    return String(valor);
  }
};

export const TerminalHistorico = ({ historico = [], rotuloAtual = "" }: { historico?: string[]; rotuloAtual?: (string) }) => {
  const linhas = Array.isArray(historico) ? historico : [];

  return (
    <div className={estilos.painelTerminal}>
      <div className={estilos.cabecalhoTerminal}>
        <span>{CONFIG.APP_NAME}</span>
        <span>Sistema Seguro</span>
      </div>
      {linhas.map((logAnterior, indiceLog) => (
        <div key={`${indiceLog}-${formatarLinhaHistorico(logAnterior)}`} className={estilos.historicoLog}>
          &gt; {formatarLinhaHistorico(logAnterior)}
        </div>
      ))}
      <div className={estilos.logAtivo}>&gt; {formatarLinhaHistorico(rotuloAtual)}</div>
    </div>
  );
};
