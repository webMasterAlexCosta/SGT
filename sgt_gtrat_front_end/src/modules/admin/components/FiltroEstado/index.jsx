import React, { useId } from "react";
import { Globe2, MapPinned } from "lucide-react";
import {
  ESTADOS_BRASIL,
  TODOS_OS_ESTADOS,
} from "../../constants/estados.js";
import estilos from "./index.module.css";

export const FiltroEstado = ({
  valor,
  aoMudar,
  contagens = {},
  rotulo = "Filtrar por estado",
  id,
}) => {
  const idGerado = useId();
  const idCampo = id || `filtro-estado-${idGerado.replaceAll(":", "")}`;
  const totalGeral = Number(contagens[TODOS_OS_ESTADOS] || 0);
  const visaoNacional = valor === TODOS_OS_ESTADOS;

  return (
    <div className={estilos.containerFiltro}>
      <label className={estilos.rotulo} htmlFor={idCampo}>
        <MapPinned size={15} /> {rotulo}
      </label>
      <div className={estilos.linhaControle}>
        <div className={estilos.envoltorioSelect}>
          <select
            id={idCampo}
            value={valor}
            onChange={(evento) => aoMudar(evento.target.value)}
            className={estilos.selectEstado}
            aria-label={rotulo}
          >
            <option value={TODOS_OS_ESTADOS}>
              Todos os estados ({totalGeral})
            </option>
            {ESTADOS_BRASIL.map((estado) => (
              <option key={estado.sigla} value={estado.sigla}>
                {estado.sigla} — {estado.nome} ({Number(contagens[estado.sigla] || 0)})
              </option>
            ))}
          </select>
        </div>
        <span
          className={`${estilos.indicadorEscopo} ${visaoNacional ? estilos.indicadorNacional : ""}`}
        >
          {visaoNacional ? <Globe2 size={13} /> : <MapPinned size={13} />}
          {visaoNacional ? "Visão nacional" : `Exibindo ${valor}`}
        </span>
      </div>
    </div>
  );
};
