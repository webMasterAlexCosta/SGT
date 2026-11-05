import React from "react";
import { Ban, CircleCheckBig, FilePenLine, Files, Timer, UserCheck, UsersRound } from "lucide-react";
import estilos from "./index.module.css";

const INDICADORES = [
  { chave: "total", rotulo: "Contratos", Icone: Files, classe: "azul" },
  { chave: "rascunhos", rotulo: "Rascunhos", Icone: FilePenLine, classe: "amarelo" },
  { chave: "vigentes", rotulo: "Vigentes", Icone: CircleCheckBig, classe: "verde" },
  { chave: "futuros", rotulo: "Futuros", Icone: Timer, classe: "azul" },
  { chave: "inativos", rotulo: "Inativos adm.", Icone: Ban, classe: "vermelho" },
  { chave: "efetivoAtual", rotulo: "Efetivo atual", Icone: UserCheck, classe: "verde" },
  { chave: "efetivoPrevisto", rotulo: "Efetivo previsto", Icone: UsersRound, classe: "azul" },
];

export const IndicadoresContratos = ({ indicadores }) => (
  <section className={estilos.gradeIndicadores} aria-label="Indicadores do ciclo de vida contratual">
    {INDICADORES.map(({ chave, rotulo, Icone, classe }) => (
      <article key={chave} className={`${estilos.cartaoIndicador} ${estilos[classe]}`}>
        <div className={estilos.iconeIndicador}><Icone size={20} strokeWidth={2.2}/></div>
        <div><strong className={estilos.valorIndicador}>{indicadores?.[chave] ?? 0}</strong><span className={estilos.rotuloIndicador}>{rotulo}</span></div>
      </article>
    ))}
  </section>
);
