import type { RegistroAuditoriaDTO } from '../../../../../../contracts/java';
import type { Page } from '../../../../../../contracts/shared';
import React from "react";
import { Eye } from "lucide-react";
import { CONFIG } from "../../../../../../core/config";
import { Botao, Cartao } from "../../../../../../core/ui/EnvolucroDica";
import { formatarDataHora } from "../../utils";
import { BotaoCopiar } from "../BotaoCopiar";
import { EmblemaAcao } from "../EmblemaAcao";
import estilos from "./index.module.css";

export const TabelaAuditoria = ({
  dados = { content: [], totalElements: 0, totalPages: 0, number: 0 },
  carregando = false,
  pagina = 0,
  setPagina = () => {},
  tamanho = CONFIG.AUDIT_PAGINATION_OPTIONS?.[0] || 20,
  setTamanho = () => {},
  aoSelecionar = () => {},
}: { dados?: Partial<Page<RegistroAuditoriaDTO>>; carregando?: boolean; pagina?: number; setPagina?: React.Dispatch<React.SetStateAction<number>>; tamanho?: number; setTamanho?: (tamanho: number) => void; aoSelecionar?: (registro: RegistroAuditoriaDTO) => void }) => (
  <Cartao title={`Registros encontrados (${dados.totalElements || 0})`}>
    <div className={estilos.rolagem}><table><thead><tr><th>Data / Hora</th><th>Ação</th><th>Usuário</th><th>Entidade</th><th>ID Ref.</th><th>Request ID</th><th /></tr></thead><tbody>
      {carregando && <tr><td colSpan={7} className={estilos.vazio}>Carregando dados...</td></tr>}
      {!carregando && !dados.content?.length && <tr><td colSpan={7} className={estilos.vazio}>Nenhum registro encontrado.</td></tr>}
      {(dados.content || []).map((linha) => <tr key={linha.id}><td className={estilos.data}>{formatarDataHora(linha.createdAt)}</td><td><EmblemaAcao acao={linha.action} /></td><td><strong>{linha.username}</strong></td><td><span className={estilos.entidade}>{linha.entityType}<small>{linha.entityLabel}</small></span></td><td><span className={estilos.identificador}>{linha.entityId || "-"}{linha.entityId && <BotaoCopiar texto={linha.entityId} />}</span></td><td><span className={estilos.identificador}><code>{linha.requestId ? `${linha.requestId.slice(0, 8)}...` : "-"}</code>{linha.requestId && <BotaoCopiar texto={linha.requestId} />}</span></td><td><button type="button" className={estilos.visualizar} onClick={() => aoSelecionar(linha)} title="Ver detalhes" aria-label="Ver detalhes do registro de auditoria"><Eye size={16} aria-hidden="true" /></button></td></tr>)}
    </tbody></table></div>
    <div className={estilos.paginacao}><span>Página <strong>{(dados.number || 0) + 1}</strong> de <strong>{Math.max(1, dados.totalPages || 0)}</strong></span><div><select aria-label="Quantidade de registros de auditoria por página" value={tamanho} onChange={(e) => { setTamanho(Number(e.target.value)); setPagina(0); }}>{CONFIG.AUDIT_PAGINATION_OPTIONS.map((opcao) => <option key={opcao} value={opcao}>{opcao} linhas</option>)}<option value={CONFIG.PAGINATION_MAX_SIZE}>Máximo</option></select><Botao variant="secondary" size="sm" onClick={() => setPagina((valor) => Math.max(0, valor - 1))} disabled={pagina === 0 || carregando}>Anterior</Botao><Botao variant="secondary" size="sm" onClick={() => setPagina((valor) => valor + 1)} disabled={pagina + 1 >= Number(dados.totalPages) || carregando}>Próxima</Botao></div></div>
  </Cartao>
);
