import React from "react";
import estilos from "./Carregamento.module.css";

export const CarregamentoEmLinha = ({ label = "Carregando..." }) => (
  <div className={estilos.envoltorioEmLinha}>
    <div className={estilos.conteudoEmLinha}>
      <div className={estilos.giradorLivre} aria-hidden="true" />
      <span className={estilos.rotuloEmLinha}>{label}</span>
    </div>
  </div>
);

export const CarregamentoTelaInteira = ({
  title = "Carregando Sistema...",
  subtitle = "Aguarde um momento.",
}) => (
  <div className={estilos.envoltorioTelaCheia}>
    <div className={estilos.cartaoTelaCheia}>
      <div className={estilos.cabecalhoTelaCheia}>
        <div className={estilos.giradorTelaCheia} aria-hidden="true" />
        <div className={estilos.textosTelaCheia}>
          <div className={estilos.tituloTelaCheia}>{title}</div>
          <div className={estilos.subtituloTelaCheia}>{subtitle}</div>
        </div>
      </div>

      <div className={estilos.esqueletoCarregamento} aria-hidden="true">
        <div
          className={`${estilos.linhaEsqueleto} ${estilos.linhaEsqueletoLarga}`}
        />
        <div
          className={`${estilos.linhaEsqueleto} ${estilos.linhaEsqueletoMedia}`}
        />
        <div
          className={`${estilos.linhaEsqueleto} ${estilos.linhaEsqueletoCurta}`}
        />
      </div>
    </div>
  </div>
);
