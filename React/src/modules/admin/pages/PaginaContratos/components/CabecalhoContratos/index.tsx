import React from "react";
import { FileText, Plus, RefreshCw } from "lucide-react";
import estilos from "./index.module.css";

export const CabecalhoContratos = ({
  contextoAcesso,
  podeGerenciar,
  carregando,
  aoCriarContrato,
  aoAtualizar,
}: { contextoAcesso: string | { descricao?: string; titulo?: string }; podeGerenciar: boolean; carregando: boolean; aoCriarContrato: () => void; aoAtualizar: () => Promise<void> }) => {
  const textoContexto =
    typeof contextoAcesso === "string" || typeof contextoAcesso === "number"
      ? String(contextoAcesso)
      : contextoAcesso?.descricao || contextoAcesso?.titulo || "";

  return (
    <header className={estilos.cabecalhoPrincipal}>
      <div className={estilos.identificacaoPagina}>
        <div className={estilos.iconeTitulo} aria-hidden="true">
          <FileText size={24} strokeWidth={2.2} />
        </div>
        <div>
          <span className={estilos.sobretitulo}>Gestão contratual</span>
          <h1 className={estilos.titulo}>Contratos de terceirização</h1>
          <p className={estilos.subtitulo}>{textoContexto}</p>
        </div>
      </div>

      <div className={estilos.acoesCabecalho}>
        <button
          type="button"
          className={estilos.botaoSecundario}
          onClick={aoAtualizar}
          disabled={carregando}
        >
          <RefreshCw size={17} className={carregando ? estilos.iconeGirando : ""} />
          Atualizar
        </button>

        {podeGerenciar && (
          <button type="button" className={estilos.botaoPrimario} onClick={aoCriarContrato}>
            <Plus size={18} />
            Novo contrato
          </button>
        )}
      </div>
    </header>
  );
};
