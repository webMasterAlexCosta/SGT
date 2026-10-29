import React from "react";
import { Clock3, MoonStar, Scale } from "lucide-react";
import estilos from "./index.module.css";

export const ResumoRegraNoturna = () => (
  <aside className={estilos.cartao} aria-label="Regra de apuração da jornada noturna urbana">
    <div className={estilos.item}>
      <div className={estilos.icone}><MoonStar size={17} /></div>
      <div className={estilos.textos}>
        <strong>Período Noturno Urbano</strong>
        <span>Das 22h00 às 05h00</span>
      </div>
    </div>
    <div className={estilos.divisor} />
    <div className={estilos.item}>
      <div className={estilos.icone}><Clock3 size={17} /></div>
      <div className={estilos.textos}>
        <strong>Hora Noturna Reduzida</strong>
        <span>52min 30s equivalem a 1h de jornada</span>
      </div>
    </div>
    <div className={estilos.divisor} />
    <div className={estilos.item}>
      <div className={estilos.icone}><Scale size={17} /></div>
      <div className={estilos.textos}>
        <strong>Equivalência Integral</strong>
        <span>7h físicas = 8h apuradas/pagáveis</span>
      </div>
    </div>
  </aside>
);