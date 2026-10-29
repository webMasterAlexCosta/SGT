import React from "react";
import { CalendarDays, Plus } from "lucide-react";
import { Botao } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

export const CabecalhoCalendario = ({ identificacaoEscopo, aoCriar, podeEditar = true }) => (
  <header className={estilos.cabecalho}>
    <div className={estilos.identificacao}><span className={estilos.icone}><CalendarDays size={23} /></span><div><div className={estilos.linha}><h1>Feriados e folgas</h1><em>{identificacaoEscopo}</em></div><p>Eventos afetam somente os empregados vinculados à abrangência selecionada.</p></div></div>
    {podeEditar && <Botao onClick={aoCriar}><Plus size={17} /> Cadastrar evento</Botao>}
  </header>
);
