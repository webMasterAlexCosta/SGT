import React, { useState } from "react";
import { ChevronDown, ChevronUp, Filter, Search } from "lucide-react";
import { Botao, Cartao, CampoEntrada } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

export const FiltrosAuditoria = ({ filtros, setFiltros, aoBuscar, aoLimpar }: { filtros: { action: string; username: string; entityType: string; entityId: string; requestId: string; startDate: string; endDate: string; }; setFiltros: React.Dispatch<React.SetStateAction<{ action: string; username: string; entityType: string; entityId: string; requestId: string; startDate: string; endDate: string; }>>; aoBuscar: () => void; aoLimpar: () => void }) => {
  const [aberto, setAberto] = useState(false);
  const alterar = (campo: keyof typeof filtros) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setFiltros((atual) => ({ ...atual, [campo]: e.target.value }));
  return (
    <Cartao>
      <button type="button" className={estilos.cabecalho} onClick={() => setAberto((valor) => !valor)}><span><Filter size={16} /> Filtros de pesquisa</span>{aberto ? <ChevronUp size={16} /> : <ChevronDown size={16} />}</button>
      {aberto && <div className={estilos.corpo}>
        <div className={estilos.grade}><CampoEntrada label="Ação" placeholder="Ex.: CREATE" value={filtros.action} onChange={alterar("action")} /><CampoEntrada label="Usuário" placeholder="Login" value={filtros.username} onChange={alterar("username")} /><CampoEntrada label="Entidade" placeholder="Ex.: Empregado" value={filtros.entityType} onChange={alterar("entityType")} /><CampoEntrada label="ID da entidade" placeholder="ID" value={filtros.entityId} onChange={alterar("entityId")} /><CampoEntrada label="Request ID" placeholder="UUID" value={filtros.requestId} onChange={alterar("requestId")} /></div>
        <div className={estilos.rodape}><label>Data inicial<input type="date" value={filtros.startDate} onChange={alterar("startDate")} /></label><label>Data final<input type="date" value={filtros.endDate} onChange={alterar("endDate")} /></label><div><Botao onClick={aoBuscar}><Search size={16} /> Buscar</Botao><Botao variant="secondary" onClick={aoLimpar}>Limpar</Botao></div></div>
      </div>}
    </Cartao>
  );
};
