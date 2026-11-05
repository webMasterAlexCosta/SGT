import React from "react";
import { CONFIG } from "../../../core/config";
import estilos from "../index.module.css";

export const ReatorCentral = ({ progresso, logotipoCorreios }) => {
  const raioSvg = 130;
  const circunferenciaSvg = 2 * Math.PI * raioSvg;
  const deslocamentoSvg =
    circunferenciaSvg - (progresso / 100) * circunferenciaSvg;

  return (
    <div className={estilos.reatorCentral}>
      <div className={estilos.cantoMiraSE}></div>
      <div className={estilos.cantoMiraSD}></div>
      <div className={estilos.cantoMiraIE}></div>
      <div className={estilos.cantoMiraID}></div>
      <div className={estilos.rastreadorRadar}></div>
      <div className={estilos.anelTracejado}></div>

      <svg className={estilos.svgProgresso} width="300" height="300">
        <circle
          cx="150"
          cy="150"
          r={raioSvg}
          className={estilos.trilhaProgresso}
        />
        <circle
          cx="150"
          cy="150"
          r={raioSvg}
          className={estilos.barraProgresso}
          style={{
            strokeDasharray: circunferenciaSvg,
            strokeDashoffset: deslocamentoSvg,
          }}
        />
      </svg>

      <div className={estilos.hudCentral}>
        <img
          src={logotipoCorreios}
          alt={CONFIG.APP_COMPANY}
          className={estilos.logotipo}
        />
        <div className={estilos.leituraDados}>
          <span className={estilos.textoValor}>{Math.round(progresso)}</span>
          <span className={estilos.textoUnidade}>%</span>
        </div>
      </div>
    </div>
  );
};
