import React from "react";
import estilos from "../index.module.css";

export const PainelHUDDiagnostico = ({
  statusSistema,
  latencia,
  memoria,
  cargaCpu,
  nomeNavegador,
}) => {
  return (
    <div className={estilos.painelHudEsquerdo}>
      <div className={estilos.cabecalhoPainel}>CLIENTE-DIAGNÓSTICO</div>

      {/* <div className={estilos.blocoDados}>
        <div className={estilos.rotuloDados}>STATUS DA APLICAÇÃO</div>
        <div
          className={estilos.valorDadosDestaque}
          style={{
            color:
              statusSistema.includes("OFFLINE") ||
              statusSistema.includes("FALTA")
                ? "#ff4444"
                : undefined,
          }}
        >
          {statusSistema}
        </div>
      </div> */}

      {/* <div className={estilos.blocoDados}>
        <div className={estilos.rotuloDados}>LATÊNCIA JAVA</div>
        <div className={estilos.valorDados}>{latencia} ms</div>
        <div className={estilos.graficoBarra}>
          <div
            className={estilos.preenchimentoBarra}
            style={{
              width: `${Math.min(latencia / 2, 100)}%`,
              background: latencia > 500 ? "#ff4444" : undefined,
            }}
          ></div>
        </div>
      </div> */}

      <div className={estilos.blocoDados}>
        <div className={estilos.rotuloDados}>
          MEMÓRIA UTILIZADA {nomeNavegador}
        </div>
        <div className={estilos.valorDados}>{memoria} MB</div>
        <div className={estilos.graficoBarra}>
          <div
            className={estilos.preenchimentoBarra}
            style={{ width: `${Math.min(memoria / 2, 100)}%` }}
          ></div>
        </div>
      </div>

      <div className={estilos.blocoDados}>
        <div className={estilos.rotuloDados}>CARGA DE USO (CPU)</div>
        <div className={estilos.valorDados}>{cargaCpu} %</div>
        <div className={estilos.equalizadorCpu}>
          {Array.from({ length: 15 }).map((_, i) => (
            <div
              key={`cpu-segmento-${i + 1}`}
              className={estilos.barraEqualizador}
              style={{
                height: `${i < (cargaCpu / 100) * 15 ? 100 : 20}%`,
                opacity: i < (cargaCpu / 100) * 15 ? 1 : 0.3,
              }}
            ></div>
          ))}
        </div>
      </div>
    </div>
  );
};
