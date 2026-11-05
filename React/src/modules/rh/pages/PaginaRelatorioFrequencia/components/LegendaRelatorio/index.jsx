import React from "react";
import { Info } from "lucide-react";
import { itensLegenda } from "../../utils";
import estilos from "./index.module.css";

export const LegendaRelatorio = () => (
  <section className={estilos.cartao}>
    <div className={estilos.cabecalho}>
      <Info size={15} />
      <h2>Legenda dos Lançamentos</h2>
    </div>
    <div className={estilos.grade}>
      {itensLegenda.map((item) => (
        <div key={item.sigla} className={estilos.item}>
          <span
            className={estilos.tag}
            style={{
              backgroundColor: `#${item.bg.slice(2)}`,
              color: `#${item.text.slice(2)}`,
              borderColor: `rgba(${item.border.join(",")}, 0.6)`,
            }}
          >
            {item.sigla}
          </span>
          <span className={estilos.descricao}>{item.desc}</span>
        </div>
      ))}
    </div>
  </section>
);