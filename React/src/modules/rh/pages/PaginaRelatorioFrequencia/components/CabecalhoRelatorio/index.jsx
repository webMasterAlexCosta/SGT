import React from "react";
import { Activity, BarChart3, FileSpreadsheet, FileText } from "lucide-react";
import estilos from "./index.module.css";

export const CabecalhoRelatorio = ({ existemDados, aoExportarExcel, aoExportarPdf }) => (
  <header className={estilos.cartao}>
    <div className={estilos.identidade}>
      <div className={estilos.tituloGrupo}>
        <div className={estilos.iconeTitulo}>
          <Activity size={24} aria-hidden="true" />
        </div>
        <div className={estilos.textosCabecalho}>
          <div className={estilos.selo}>
            <BarChart3 size={13} aria-hidden="true" />
            <span>Apuração Oficial</span>
          </div>
          <h1>Relatório Consolidado de Frequência</h1>
          <p>Visão unificada de frequência diária, lotação, turno, horas físicas, noturnas e saldo apurado.</p>
        </div>
      </div>
    </div>

    <div className={estilos.exportacoes} aria-label="Exportar relatório de frequência">
      <span className={estilos.exportarRotulo}>Opções de Exportação</span>
      <div className={estilos.botoesExportacao}>
        <button
          type="button"
          className={`${estilos.botaoExportacao} ${estilos.botaoExcel}`}
          onClick={aoExportarExcel}
          disabled={!existemDados}
          title="Exportar dados em formato de planilha Excel (.xlsx)"
        >
          <FileSpreadsheet size={17} aria-hidden="true" />
          <span className={estilos.nomeBotao}>Planilha</span>
          <span className={estilos.extensao}>XLSX</span>
        </button>

        <button
          type="button"
          className={`${estilos.botaoExportacao} ${estilos.botaoPdf}`}
          onClick={aoExportarPdf}
          disabled={!existemDados}
          title="Exportar relatório formatado em PDF"
        >
          <FileText size={17} aria-hidden="true" />
          <span className={estilos.nomeBotao}>Documento</span>
          <span className={estilos.extensao}>PDF</span>
        </button>
      </div>
      {!existemDados && (
        <span className={estilos.ajudaExportacao}>Gere a consulta para liberar os arquivos.</span>
      )}
    </div>
  </header>
);