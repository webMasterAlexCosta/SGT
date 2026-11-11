import React from "react";
import estilos from "../index.module.css";

export const PainelHUDSeguranca = ({
  usuarioSo,
  nomeNavegador,
  tokenAutenticacao,
  ipCliente,
}: { usuarioSo: string; nomeNavegador: string; tokenAutenticacao: string; ipCliente: string }) => {
  return (
    <div className={estilos.painelHudDireito}>
      <div className={estilos.cabecalhoPainel}>SEGURANÇA & REDE</div>

      <div className={estilos.blocoDados}>
        <div className={estilos.rotuloDados}>SISTEMA OPERACIONAL</div>
        <div className={estilos.valorDadosDestaque}>{usuarioSo}</div>
      </div>

      <div className={estilos.blocoDados}>
        <div className={estilos.rotuloDados}>NAVEGADOR CLIENTE</div>
        <div className={estilos.valorDados}>{nomeNavegador}</div>
      </div>

      <div className={estilos.blocoDados}>
        <div className={estilos.rotuloDados}>AUTENTICAÇÃO</div>
        <div className={estilos.valorDados}>{tokenAutenticacao}</div>
      </div>

      <div className={estilos.blocoDados}>
        <div className={estilos.rotuloDados}>ENDEREÇO IPv4</div>
        <div className={estilos.valorDados}>{ipCliente}</div>
      </div>

      <div className={estilos.reticulaMiraPequena}>
        <div className={estilos.miraX}></div>
        <div className={estilos.miraY}></div>
      </div>
    </div>
  );
};
