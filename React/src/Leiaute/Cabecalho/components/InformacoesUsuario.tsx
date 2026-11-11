import React from "react";
import { rotuloPerfil } from "../../../core/security/roles";
import estilos from "../index.module.css";

export const InformacoesUsuario = ({ usuario }: { usuario: import("../../../contracts/auth").AuthenticatedUser | null }) => (
  <div className={estilos.infoUsuario}>
    <p className={estilos.nomeUsuario}>{usuario?.username}</p>
    <p className={estilos.funcaoUsuario}>{rotuloPerfil(usuario)}</p>
  </div>
);
