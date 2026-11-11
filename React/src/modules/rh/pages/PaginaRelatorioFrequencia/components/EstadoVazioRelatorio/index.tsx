import React from "react";
import { LoaderCircle, FileSearch } from "lucide-react";
import estilos from "./index.module.css";

export const EstadoVazioRelatorio = ({ carregando }: { carregando: boolean }) => (
  <div className={estilos.estadoVazio}>
    <div className={estilos.circuloIcone}>
      {carregando ? (
        <LoaderCircle size={32} className={estilos.girando} />
      ) : (
        <FileSearch size={32} />
      )}
    </div>
    <h3>{carregando ? "Apurando Registros de Frequência..." : "Pronto para Pesquisar"}</h3>
    <p>
      {carregando
        ? "Processando regras noturnas, escalas e consolidando lançamentos do período selecionado."
        : "Defina o período desejado e clique em Gerar Relatório para consolidar as jornadas."}
    </p>
  </div>
);