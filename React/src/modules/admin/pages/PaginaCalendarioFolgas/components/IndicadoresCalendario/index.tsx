import React from "react";
import { CalendarCheck2, CalendarOff, ListChecks, MapPin } from "lucide-react";
import estilos from "./index.module.css";

const itens: Array<["total" | "feriados" | "locais" | "ativos", string, import("lucide-react").LucideIcon]> = [
  ["total", "Eventos no período", ListChecks],
  ["feriados", "Feriados", CalendarCheck2],
  ["locais", "Municipais ou por lotação", MapPin],
  ["ativos", "Eventos ativos", CalendarOff],
];

export const IndicadoresCalendario = ({ indicadores }: { indicadores: { total: number; ativos: number; feriados: number; locais: number; } }) => (
  <section className={estilos.grade} aria-label="Resumo do calendário">{itens.map(([chave, rotulo, Icone]) => <article key={chave}><span><Icone size={19} /></span><div><strong>{indicadores[chave]}</strong><small>{rotulo}</small></div></article>)}</section>
);
