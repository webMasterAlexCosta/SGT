import React, { useMemo } from "react";
import { CONFIG } from "../../../core/config";
import estilos from "../index.module.css";

export const RodapeBarraLateral = () => {
  const anoAtual = useMemo(() => new Date().getFullYear(), []);

  return (
    <div className={estilos.rodape}>
      <div className={estilos.onda}></div>
      <div className={estilos.identificacaoVersaoSidebar}>
        <span className={estilos.seloVersaoSidebar}>v{CONFIG.APP_VERSION}</span>
        <span className={estilos.rotuloVersaoSidebar}>versão do sistema</span>
      </div>
      <p className={estilos.textoTopo}>© {anoAtual}</p>
      <div className={estilos.divisorRodape}></div>
      <p className={estilos.textoFundo}>
        desenvolvido por {CONFIG.AUTHOR_NAME}{" "}
        <span className={estilos.matriculaRodape}>
          {CONFIG.AUTHOR_MATRICULA}
        </span>
      </p>
    </div>
  );
};
