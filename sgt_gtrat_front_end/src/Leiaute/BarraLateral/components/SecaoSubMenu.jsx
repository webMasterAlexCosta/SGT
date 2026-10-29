import React, { memo } from "react";
import { ChevronDown, ChevronRight } from "lucide-react";
import estilos from "../index.module.css";

export const SecaoSubMenu = memo(({ titulo, estaAberto, aoAlternar, children }) => (
  <div className={estilos.secaoSubMenu}>
    <button
      onClick={aoAlternar}
      className={estilos.botaoSecaoSubMenu}
      type="button"
      aria-expanded={estaAberto ? "true" : "false"}
    >
      <span>{titulo}</span>
      {estaAberto ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
    </button>
    <div className={`${estilos.conteudoSubMenu} ${estaAberto ? estilos.conteudoAberto : estilos.conteudoFechado}`}>
      <div className={estilos.bordaSubMenu}>{children}</div>
    </div>
  </div>
));

SecaoSubMenu.displayName = "SecaoSubMenu";