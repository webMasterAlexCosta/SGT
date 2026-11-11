import React, { useState } from "react";
import { Check, Copy, QrCode, Smartphone } from "lucide-react";
import { notificar } from "../../../../../../core/events.ts";
import estilos from "./index.module.css";

export const ConfiguracaoCodigoQr = ({ qrCodeDataUrl, secret, otpauthUri }: { qrCodeDataUrl: (string | null); secret: (string | null); otpauthUri: (string | null) }) => {
  const [copiado, setCopiado] = useState(false);
  const segredoFormatado = String(secret || "").match(/.{1,4}/g)?.join(" ") || "";

  const copiarSegredo = async () => {
    try {
      await navigator.clipboard.writeText(secret || "");
      setCopiado(true);
      window.setTimeout(() => setCopiado(false), 1600);
    } catch {
      notificar("warning", "Não foi possível copiar automaticamente. Selecione a chave manualmente.");
    }
  };

  return (
    <div className={estilos.configuracao}>
      <div className={estilos.painelQr}>
        <div className={estilos.tituloQr}><QrCode size={17} /> Escaneie o QR Code</div>
        <div className={estilos.molduraQr}>
          {qrCodeDataUrl ? (
            <img src={qrCodeDataUrl} alt="QR Code para configurar o autenticador" />
          ) : (
            <span>QR Code indisponível</span>
          )}
        </div>
        <span className={estilos.compatibilidade}>Google Authenticator · Microsoft Authenticator</span>
      </div>

      <div className={estilos.instrucoes}>
        <ol>
          <li><span>1</span><p>Abra o aplicativo autenticador no celular.</p></li>
          <li><span>2</span><p>Escolha adicionar conta e escaneie este QR Code.</p></li>
          <li><span>3</span><p>Informe abaixo o código de 6 dígitos gerado.</p></li>
        </ol>
        <div className={estilos.chaveManual}>
          <div>
            <Smartphone size={15} />
            <span>Não consegue escanear? Use a chave manual</span>
          </div>
          <code>{segredoFormatado}</code>
          <button type="button" onClick={copiarSegredo}>
            {copiado ? <Check size={14} /> : <Copy size={14} />}
            {copiado ? "Copiada" : "Copiar chave"}
          </button>
        </div>
        <details className={estilos.detalhesUri}>
          <summary>Mostrar URI de configuração avançada</summary>
          <code>{otpauthUri}</code>
        </details>
      </div>
    </div>
  );
};
