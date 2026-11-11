import React from "react";
import { MapPinned, Plus } from "lucide-react";
import { Botao } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

export const CabecalhoEstrutura = ({ presidente, administrador, possuiGestorEstadual, aoNovoGestaoLocal }: { presidente: boolean; administrador: boolean; possuiGestorEstadual: boolean; aoNovoGestaoLocal: () => void }) => (
  <header className={estilos.cabecalho}>
    <div className={estilos.titulo}><span><MapPinned size={23} /></span><div><h1>Estrutura organizacional</h1><p>{presidente ? "Visão hierárquica dos gestores estaduais e Gestões Locais, em modo somente leitura." : "Gestor nacional → gestor estadual/UF → Gestões Locais → operação."}</p></div></div>
    {administrador && <Botao onClick={aoNovoGestaoLocal} disabled={!possuiGestorEstadual}><Plus size={17} /> Nova Gestão Local</Botao>}
  </header>
);
