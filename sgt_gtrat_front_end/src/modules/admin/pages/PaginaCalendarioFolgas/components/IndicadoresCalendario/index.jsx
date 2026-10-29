import React from "react";
import { CalendarCheck2, CalendarOff, ListChecks, MapPin } from "lucide-react";
import estilos from "./index.module.css";

const itens = [
  ["total", "Eventos no período", ListChecks],
  ["feriados", "Feriados", CalendarCheck2],
  ["locais", "Municipais ou por lotação", MapPin],
  ["ativos", "Eventos ativos", CalendarOff],
];

export const IndicadoresCalendario = ({ indicadores }) => (
  <section className={estilos.grade} aria-label="Resumo do calendário">{itens.map(([chave, rotulo, Icone]) => <article key={chave}><span><Icone size={19} /></span><div><strong>{indicadores[chave]}</strong><small>{rotulo}</small></div></article>)}</section>
);
