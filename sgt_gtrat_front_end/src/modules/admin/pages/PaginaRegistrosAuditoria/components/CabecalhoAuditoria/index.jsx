import React, { useEffect, useRef, useState } from "react";
import { ChevronDown, Download, File, FileSpreadsheet, FileText, RefreshCcw } from "lucide-react";
import { Botao } from "../../../../../../core/ui/EnvolucroDica";
import { exportarParaCSV, exportarParaExcel, exportarParaPDF } from "../../exportacao";
import estilos from "./index.module.css";

export const CabecalhoAuditoria = ({ registros, carregando, aoAtualizar }) => {
  const [aberto, setAberto] = useState(false);
  const ref = useRef(null);
  useEffect(() => {
    const fechar = (e) => ref.current && !ref.current.contains(e.target) && setAberto(false);
    document.addEventListener("mousedown", fechar);
    return () => document.removeEventListener("mousedown", fechar);
  }, []);
  const exportar = async (acao) => { await acao(registros); setAberto(false); };
  return (
    <header className={estilos.cabecalho}>
      <div className={estilos.titulo}><span><FileText size={22} /></span><div><h1>Logs de auditoria</h1><p>Histórico e rastreabilidade das operações do sistema.</p></div></div>
      <div className={estilos.acoes}>
        <div className={estilos.exportacao} ref={ref}><Botao variant="secondary" onClick={() => setAberto((valor) => !valor)} disabled={!registros?.length}><Download size={16} /> Exportar <ChevronDown size={14} /></Botao>{aberto && <div className={estilos.menu}><button type="button" onClick={() => exportar(exportarParaExcel)}><FileSpreadsheet size={16} /> Excel (.xlsx)</button><button type="button" onClick={() => exportar(exportarParaCSV)}><FileText size={16} /> CSV (.csv)</button><button type="button" onClick={() => exportar(exportarParaPDF)}><File size={16} /> PDF (.pdf)</button></div>}</div>
        <Botao onClick={aoAtualizar} disabled={carregando}><RefreshCcw size={16} className={carregando ? estilos.girando : ""} /> Atualizar</Botao>
      </div>
    </header>
  );
};
