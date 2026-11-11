import React from "react";
import { Building2, Edit, Search } from "lucide-react";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../../../../../core/formatters/nomenclaturaGestao";
import estilos from "./index.module.css";

export const TabelaGestoesLocais = ({ itens, busca, aoBuscar, carregando, aoEditar, somenteLeitura = false }: { itens: (import("../../../../../../contracts/opcoes-apresentacao").LocalManagementView[]); busca: (string); aoBuscar: (React.Dispatch<React.SetStateAction<string>>); carregando: (boolean); aoEditar: ((item: import("../../../../../../contracts/opcoes-apresentacao").LocalManagementView) => void) | null; somenteLeitura?: (boolean) }) => (
  <section className={estilos.cartao}>
    <div className={estilos.topo}><div><h2><Building2 size={20} /> Gestões Locais</h2><p>Cada Gestão Local pertence a um único gestor estadual.</p></div><label className={estilos.busca}><Search size={17} /><input value={busca} onChange={(e) => aoBuscar(e.target.value)} placeholder="Buscar nome, código ou gestor estadual..." /></label></div>
    <div className={estilos.rolagem}><table><thead><tr><th>Gestão Local</th><th>Código</th><th>Gestor estadual</th><th>Status</th>{!somenteLeitura && <th>Ações</th>}</tr></thead><tbody>{itens.map((item) => <tr key={item.id}><td><strong>{formatarGestaoLocal(item)}</strong></td><td>{item.codigo}</td><td><span className={estilos.uf}>{item.uf}</span> {formatarGestorEstadual(item.superintendencia || item.superintendenciaNome, item.uf)}</td><td><span className={item.ativo ? estilos.ativo : estilos.inativo}>{item.ativo ? "Ativo" : "Inativo"}</span></td>{!somenteLeitura && <td><button type="button" onClick={() => aoEditar?.(item)} title="Editar"><Edit size={16} /></button></td>}</tr>)}{!carregando && !itens.length && <tr><td colSpan={somenteLeitura ? 4 : 5} className={estilos.vazio}>Nenhuma Gestão Local encontrada.</td></tr>}</tbody></table></div>
  </section>
);
