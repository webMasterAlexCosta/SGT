import React from "react";
import { Copy } from "lucide-react";
import { Botao, JanelaModal } from "../../../../../../core/ui/EnvolucroDica";
import { copiarParaAreaTransferencia, formatarDataHora, textoJsonProtegidoAuditoria } from "../../utils";
import { EmblemaAcao } from "../EmblemaAcao";
import estilos from "./index.module.css";

const SecaoJson = ({ titulo, valor, truncado }: { titulo: string; valor: unknown; truncado?: boolean }) => {
  const texto = textoJsonProtegidoAuditoria(valor) || "{ }";
  return <section className={estilos.json}><header><div><h3>{titulo}</h3>{truncado && <span>TRUNCADO</span>}</div><Botao size="xs" variant="ghost" onClick={() => copiarParaAreaTransferencia(texto)}><Copy size={12} /> Copiar</Botao></header><pre>{texto}</pre></section>;
};

export const ModalDetalhesAuditoria = ({ registro, aoFechar }: { registro: (import("../../../../../../contracts/java").RegistroAuditoriaDTO | null); aoFechar: (() => void) }) => (
  <JanelaModal isOpen={Boolean(registro)} onClose={aoFechar} title="Detalhes do log" maxWidth="max-w-4xl">
    {registro && <div className={estilos.conteudo}>
      <div className={estilos.resumo}><div><span>Data</span><strong>{formatarDataHora(registro.createdAt)}</strong></div><div><span>Usuário</span><strong>{registro.username}</strong></div><div><span>Entidade</span><strong>{registro.entityType} #{registro.entityId}</strong></div><div><span>Ação</span><EmblemaAcao acao={registro.action} /></div></div>
      <div className={estilos.resumo}><div><span>Request ID</span><strong>{registro.requestId || "-"}</strong></div><div><span>HTTP</span><strong>{registro.httpMethod || "-"}</strong></div><div><span>Rota</span><strong>{registro.path || "-"}</strong></div><div><span>Origem</span><strong>dados sensíveis protegidos</strong></div></div>
      <SecaoJson titulo="Payload da requisição (protegido)" valor={registro.payloadJson} truncado={(registro.payloadTruncated) ?? undefined} />
      <SecaoJson titulo="Detalhes (audit log, protegido)" valor={registro.detailsJson} />
      <div className={estilos.rodape}><Botao variant="secondary" onClick={aoFechar}>Fechar</Botao></div>
    </div>}
  </JanelaModal>
);
