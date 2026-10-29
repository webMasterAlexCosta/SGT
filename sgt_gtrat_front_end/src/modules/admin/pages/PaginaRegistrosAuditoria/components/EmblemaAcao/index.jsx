import React from "react";
import estilos from "./index.module.css";

export const EmblemaAcao = ({ acao }) => {
  const tipo = String(acao || "").toUpperCase();
  const classe = tipo.includes("CREATE") || tipo.includes("POST") ? estilos.criar
    : tipo.includes("UPDATE") || tipo.includes("PUT") ? estilos.editar
      : tipo.includes("DELETE") ? estilos.excluir : estilos.neutro;
  return <span className={`${estilos.emblema} ${classe}`}>{acao}</span>;
};
