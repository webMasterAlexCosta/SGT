import React, { memo } from "react";
import { ChevronDown, ChevronRight } from "lucide-react";
import estilos from "../index.module.css";

export const SecaoMenu = memo(({ titulo, estaAberto, aoAlternar, children }) => (
  <div className={estilos.secaoMenu}>
    <button
      onClick={aoAlternar}
      className={estilos.botaoSecaoMenu}
      type="button"
      aria-expanded={estaAberto ? "true" : "false"}
    >
      <span>{titulo}</span>
      {estaAberto ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
    </button>
    <div
      className={`${estilos.conteudoSecaoMenu} ${estaAberto ? estilos.conteudoAberto : estilos.conteudoFechado}`}
      aria-hidden={!estaAberto ? true : undefined}
      inert={!estaAberto ? true : undefined}
    >
      {children}
    </div>
  </div>
));

SecaoMenu.displayName = "SecaoMenu";
