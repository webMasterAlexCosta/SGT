import React from "react";
import { ShieldCheck, ShieldOff } from "lucide-react";
import estilos from "./index.module.css";

export const StatusSegundoFator = ({ ativo, configuracaoPendente = false }) => (
  <div className={`${estilos.status} ${ativo ? estilos.ativo : estilos.inativo}`}>
    <span className={estilos.icone}>
      {ativo ? <ShieldCheck size={21} /> : <ShieldOff size={21} />}
    </span>
    <div>
      <strong>{ativo ? "Segundo fator ativo" : "Segundo fator opcional"}</strong>
      <p>
        {ativo
          ? "O código do autenticador será solicitado nos próximos logins."
          : configuracaoPendente
            ? "Existe uma configuração incompleta. Gere um novo QR Code para continuar."
            : "Ative se quiser proteger sua conta com um aplicativo autenticador."}
      </p>
    </div>
    <span className={estilos.selo}>{ativo ? "Protegido" : "Não configurado"}</span>
  </div>
);
