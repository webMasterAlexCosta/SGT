import React from "react";
import estilos from "./Homologacao.module.css";

const Homologacao = () => {
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
