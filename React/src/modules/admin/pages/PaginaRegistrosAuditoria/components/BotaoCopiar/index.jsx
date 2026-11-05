import React, { useState } from "react";
import { Check, Copy } from "lucide-react";
import { copiarParaAreaTransferencia } from "../../utils";
import estilos from "./index.module.css";

export const BotaoCopiar = ({ texto }) => {
  const [copiado, setCopiado] = useState(false);
  const copiar = async (evento) => {
    evento.stopPropagation();
    try {
      await copiarParaAreaTransferencia(texto);
      setCopiado(true);
      window.setTimeout(() => setCopiado(false), 1600);
    } catch {}
  };
  return <button type="button" onClick={copiar} className={estilos.botao} title="Copiar">{copiado ? <Check size={12} /> : <Copy size={12} />}</button>;
};
