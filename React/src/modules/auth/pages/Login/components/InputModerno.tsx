import React, { useId } from "react";
import estilos from "../index.module.css";

export const InputModerno = ({
  rotulo,
  icone: Icone,
  desabilitado,
  id,
  ...propriedadesInput
}: React.InputHTMLAttributes<HTMLInputElement> & { rotulo: string; icone?: import("lucide-react").LucideIcon; desabilitado?: boolean }) => {
  const idGerado = useId();
  const identificador = id || propriedadesInput.name || `login-${idGerado.replace(/:/g, "")}`;

  return (
    <div className={estilos.grupoEntrada}>
      <label htmlFor={identificador} className={estilos.rotuloEntrada}>{rotulo}</label>
      <div className={estilos.envoltorioEntrada}>
        <div
          className={`${estilos.iconeEntrada} ${desabilitado ? estilos.iconeEntradaDesabilitado : ""}`}
          aria-hidden="true"
        >
          {Icone && <Icone size={16} />}
        </div>
        <input
          id={identificador}
          className={`${estilos.campoEntrada} ${desabilitado ? estilos.campoEntradaDesabilitado : ""}`}
          disabled={desabilitado}
          aria-label={!rotulo ? propriedadesInput["aria-label"] : undefined}
          {...propriedadesInput}
        />
      </div>
    </div>
  );
};
