import React from "react";
import { Building2, Plus, Search } from "lucide-react";
import { Botao } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

export const CabecalhoAgencias = ({ termoBusca, aoBuscar, aoCriar, podeEscrever = false }) => (
  <header className={estilos.cabecalho}>
    <div className={estilos.titulo}><span><Building2 size={22} /></span><div><h1>Gestão de agências</h1><p>Cadastro e vínculo das agências bancárias.</p></div></div>
    <div className={estilos.controles}>
      <label className={estilos.busca}><Search size={16} /><input type="search" placeholder="Buscar por número ou banco..." value={termoBusca} onChange={(e) => aoBuscar(e.target.value)} /></label>
      {podeEscrever && <Botao onClick={aoCriar} size="sm"><Plus size={16} /> Nova agência</Botao>}
    </div>
  </header>
);
