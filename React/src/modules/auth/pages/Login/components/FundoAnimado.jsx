import React from "react";
import { Mail, Package, Mails, Truck, Mailbox } from "lucide-react";
import estilos from "../index.module.css";

export const FundoAnimado = () => {
  return (
    <>
      <svg
        style={{ position: "absolute", width: 0, height: 0 }}
        aria-hidden="true"
      >
        <defs>
          <filter id="fluidoGooey">
            <feGaussianBlur in="SourceGraphic" stdDeviation="5" result="blur" />
            <feColorMatrix
              in="blur"
              type="matrix"
              values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 19 -9"
              result="highContrastGraphic"
            />
            <feComposite
              in="SourceGraphic"
              in2="highContrastGraphic"
              operator="atop"
            />
          </filter>
        </defs>
      </svg>

      <div className={estilos.fundoAnimadoContainer}>
        <div className={estilos.areaAnimacao}>
          <Mail
            className={`${estilos.formaFlutuante} ${estilos.forma1}`}
            strokeWidth={1.5}
          />
          <Package
            className={`${estilos.formaFlutuante} ${estilos.forma2}`}
            strokeWidth={1.5}
          />
          <Mails
            className={`${estilos.formaFlutuante} ${estilos.forma3}`}
            strokeWidth={1.5}
          />
          <Truck
            className={`${estilos.formaFlutuante} ${estilos.forma4}`}
            strokeWidth={1.5}
          />
          <Mailbox
            className={`${estilos.formaFlutuante} ${estilos.forma5}`}
            strokeWidth={1.5}
          />
          <Mails
            className={`${estilos.formaFlutuante} ${estilos.forma6}`}
            strokeWidth={1.5}
          />
          <Package
            className={`${estilos.formaFlutuante} ${estilos.forma7}`}
            strokeWidth={1.5}
          />
          <Truck
            className={`${estilos.formaFlutuante} ${estilos.forma8}`}
            strokeWidth={1.5}
          />
          <Mail
            className={`${estilos.formaFlutuante} ${estilos.forma9}`}
            strokeWidth={1.5}
          />
        </div>
      </div>
    </>
  );
};
