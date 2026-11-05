import React from "react";
import { Check } from "lucide-react";
import estilos from "./FluxoEtapas.module.css";

export const FluxoEtapas = ({ etapas = [], etapaAtual = 0, ariaLabel = "Etapas do processo" }) => (
  <nav className={estilos.conteiner} aria-label={ariaLabel}>
    <ol className={estilos.lista}>
      {etapas.map((etapa, indice) => {
        const concluida = indice < etapaAtual;
        const atual = indice === etapaAtual;
        const rotulo = typeof etapa === "string" ? etapa : etapa?.rotulo;
        return (
          <li key={`${rotulo}-${indice}`} className={`${estilos.item} ${concluida ? estilos.concluida : ""} ${atual ? estilos.atual : ""}`} aria-current={atual ? "step" : undefined}>
            <span className={estilos.indicador} aria-hidden="true">
              {concluida ? <Check size={14} /> : indice + 1}
            </span>
            <span className={estilos.rotulo}>{rotulo}</span>
          </li>
        );
      })}
    </ol>
  </nav>
);
