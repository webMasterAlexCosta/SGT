import React from "react";
import { Edit, Search, Trash2 } from "lucide-react";
import { Botao, Cartao, CampoEntrada } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

const ROTULOS = { NACIONAL: "Nacional", ESTADUAL: "Estadual", MUNICIPAL: "Municipal", LOTACAO: "Lotação" };

export const PainelEventos = ({ dataInicio, dataFim, aoMudarInicio, aoMudarFim, aoAtualizar, carregando, eventos, formatarData, obterRotuloEscopo, aoEditar, aoExcluir, podeEditar = true }) => (
  <Cartao>
    <div className={estilos.filtros}><div><CampoEntrada label="Data inicial" type="date" value={dataInicio} onChange={(e) => aoMudarInicio(e.target.value)} /><CampoEntrada label="Data final" type="date" value={dataFim} onChange={(e) => aoMudarFim(e.target.value)} /></div><Botao onClick={aoAtualizar} variant="outline"><Search size={16} /> Atualizar período</Botao></div>
    <div className={estilos.rolagem}><table><thead><tr><th>Data</th><th>Tipo</th><th>Abrangência</th><th>Unidade afetada</th><th>Descrição</th><th>Status</th>{podeEditar && <th>Ações</th>}</tr></thead><tbody>{carregando ? <tr><td colSpan={podeEditar ? 7 : 6} className={estilos.vazio}>Carregando eventos...</td></tr> : !eventos.length ? <tr><td colSpan={podeEditar ? 7 : 6} className={estilos.vazio}>Nenhum evento encontrado no período.</td></tr> : eventos.map((evento) => { const descanso = String(evento.tipo || "").toUpperCase() === "DIA_DESCANSO"; const abrangencia = String(evento.abrangencia || "NACIONAL").toUpperCase(); return <tr key={evento.id}><td className={estilos.data}>{formatarData(evento.data)}</td><td><span className={descanso ? estilos.descanso : estilos.feriado}>{descanso ? "Dia de descanso" : "Feriado"}</span></td><td><span className={estilos.abrangencia}>{ROTULOS[abrangencia] || abrangencia}</span></td><td>{obterRotuloEscopo(evento)}</td><td>{evento.descricao || "Sem descrição"}</td><td><span className={evento.ativo !== false ? estilos.ativo : estilos.inativo}>{evento.ativo !== false ? "Ativo" : "Inativo"}</span></td>{podeEditar && <td><div className={estilos.acoes}><Botao size="sm" variant="outline" onClick={() => aoEditar(evento)}><Edit size={15} /> Editar</Botao><Botao size="sm" variant="danger" onClick={() => aoExcluir(evento.id)}><Trash2 size={15} /> Excluir</Botao></div></td>}</tr>; })}</tbody></table></div>
  </Cartao>
);
