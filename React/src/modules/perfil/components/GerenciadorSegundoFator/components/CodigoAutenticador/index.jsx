import React from "react";
import { KeyRound } from "lucide-react";
import estilos from "./index.module.css";

export const CodigoAutenticador = ({ valor, aoMudar, desabilitado = false, id = "codigo-2fa" }) => (
  <div className={estilos.grupoCodigo}>
    <label htmlFor={id}>Código do aplicativo autenticador</label>
    <div className={estilos.campoCodigo}>
      <KeyRound size={18} />
      <input
        id={id}
        type="text"
        inputMode="numeric"
        pattern="[0-9]{6}"
        maxLength={6}
        placeholder="000000"
        value={valor}
        onChange={(evento) => aoMudar(evento.target.value.replace(/\D/g, "").slice(0, 6))}
        disabled={desabilitado}
        autoComplete="one-time-code"
        aria-describedby={`${id}-ajuda`}
      />
    </div>
    <small id={`${id}-ajuda`}>Digite os 6 números exibidos no aplicativo.</small>
  </div>
);
