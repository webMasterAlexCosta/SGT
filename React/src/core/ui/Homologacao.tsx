import React from "react";
import { CONFIG } from "../config";
import estilos from "./Homologacao.module.css";

const Homologacao = () => {
  if (CONFIG.BUILD_PRODUCAO) return null;
  return (
    <div className={estilos.emblemaHomologacao}>
      <span className={estilos.envoltorioPontoAnimado}>
        <span className={estilos.pontoAura}></span>
        <span className={estilos.pontoCentral}></span>
      </span>
      <span className={estilos.textoHomologacao}>DESENVOLVIMENTO</span>
    </div>
  );
};

export default Homologacao;
