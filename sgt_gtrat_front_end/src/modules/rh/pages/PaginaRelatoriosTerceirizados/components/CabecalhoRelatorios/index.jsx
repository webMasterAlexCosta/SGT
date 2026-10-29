import React from "react";
import { FileBarChart2, LoaderCircle, RefreshCw, Sparkles } from "lucide-react";
import { FaFileExcel } from "react-icons/fa";
import { BiLogoAdobe } from "react-icons/bi";
import estilos from "./index.module.css";

const formatarNumero = (valor) => Number(valor || 0).toLocaleString("pt-BR");

const formatarGeracao = (valor) => {
  if (!valor) return null;
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return null;
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(data);
};

const BotaoExportacao = ({ tipo, aoClicar, desabilitado, carregando }) => {
  const excel = tipo === "excel";
  const texto = excel ? "Excel" : "PDF";
  return (
    <button
      type="button"
      className={`${estilos.botaoExportacao} ${excel ? estilos.botaoExcel : estilos.botaoPdf}`}
      onClick={aoClicar}
      disabled={desabilitado}
      title={`Exportar o resultado filtrado completo em ${texto}, gerado oficialmente pelo servidor`}
    >
      {carregando
        ? <LoaderCircle size={19} className={estilos.girando} aria-hidden="true" />
        : excel
          ? <FaFileExcel size={19} aria-hidden="true" />
          : <BiLogoAdobe size={21} aria-hidden="true" />}
      <span>{texto}</span>
      <small>oficial</small>
    </button>
  );
};

export const CabecalhoRelatorios = ({
  carregando,
  exportandoCompleto,
  quantidadeRegistros,
  escopo,
  referencia,
  geradoEm,
  aoAtualizar,
  aoExportarExcel,
  aoExportarPdf,
}) => {
  const horarioGeracao = formatarGeracao(geradoEm);
  const exportando = Boolean(exportandoCompleto);

  return (
    <header className={estilos.cabecalhoPrincipal}>
      <div className={estilos.identificacaoPagina}>
        <div className={estilos.iconeTitulo}><FileBarChart2 size={25} aria-hidden="true" /></div>
        <div>
          <span className={estilos.sobretitulo}><Sparkles size={13} aria-hidden="true" /> Inteligência operacional</span>
          <h1>Visão consolidada de empregados terceirizados</h1>
          <p>
            Quadro geral do escopo autorizado, com dimensionamento contratual, distribuição dos vínculos e consulta paginada para bases de grande volume.
          </p>
          <div className={estilos.metadados}>
            <span>{formatarNumero(quantidadeRegistros)} colaborador(es) no resultado</span>
            {escopo && <span>Escopo: {escopo}</span>}
            {referencia && <span>Referência: {new Intl.DateTimeFormat("pt-BR").format(new Date(`${referencia}T12:00:00`))}</span>}
            {horarioGeracao && <span>Gerado em {horarioGeracao}</span>}
          </div>
        </div>
      </div>

      <div className={estilos.acoesCabecalho}>
        <button type="button" className={estilos.botaoAtualizar} onClick={aoAtualizar} disabled={carregando || exportando} title="Atualizar a visão consolidada">
          <RefreshCw size={17} className={carregando ? estilos.girando : ""} aria-hidden="true" />
          <span>Atualizar</span>
        </button>

        <div className={estilos.exportacoes} aria-label="Exportar visão consolidada">
          <span className={estilos.rotuloExportacao}>Exportar relatório oficial</span>
          <div className={estilos.gruposExportacao}>
            <div className={estilos.grupoExportacao}>
              <span className={estilos.rotuloGrupo}>Resultado filtrado completo</span>
              <div className={estilos.botoesExportacao}>
                <BotaoExportacao tipo="excel" aoClicar={aoExportarExcel} desabilitado={!quantidadeRegistros || carregando || exportando} carregando={exportandoCompleto === "excel"} />
                <BotaoExportacao tipo="pdf" aoClicar={aoExportarPdf} desabilitado={!quantidadeRegistros || carregando || exportando} carregando={exportandoCompleto === "pdf"} />
              </div>
            </div>
          </div>
          <small className={estilos.ajudaExportacao}>O Java gera o arquivo com todos os registros do filtro atual. Os botões permanecem disponíveis em qualquer página da consulta.</small>
        </div>
      </div>
    </header>
  );
};
