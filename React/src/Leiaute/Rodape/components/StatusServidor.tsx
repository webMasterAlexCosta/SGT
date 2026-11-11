import React, { useEffect, useState } from "react";
import { CONFIG } from "../../../core/config";
import { ServicoSistema } from "../../../services/system/ServicoSistema";
import estilos from "../index.module.css";

export const StatusServidor = () => {
  const [latencia, definirLatencia] = useState<string | number>("...");

  useEffect(() => {
    let ativo = true;
    const verificarLatencia = async () => {
      try {
        const urlBase = CONFIG.API_URL.replace(/\/api\/?$/, "");
        const tempoResposta = await ServicoSistema.medirLatencia(urlBase);
        if (ativo) definirLatencia(tempoResposta);
      } catch {
        if (ativo) definirLatencia("ERR");
      }
    };

    verificarLatencia();
    const idIntervalo = setInterval(verificarLatencia, CONFIG.LATENCY_CHECK_INTERVAL_MS);
    return () => {
      ativo = false;
      clearInterval(idIntervalo);
    };
  }, []);

  const temErroRede = latencia === "ERR" || latencia === "OFF";
  return (
    <div
      className={`${estilos.statusServidor} ${temErroRede ? estilos.statusServidorErro : estilos.statusServidorOk}`}
      title={temErroRede ? "Servidor indisponível" : "Latência do servidor"}
      role="status"
      aria-live="polite"
    >
      <span className={estilos.textoStatus}>
        Servidor {latencia} {typeof latencia === "number" ? "ms" : ""}
      </span>
      <span className={estilos.pontoStatus} aria-hidden="true" />
    </div>
  );
};
