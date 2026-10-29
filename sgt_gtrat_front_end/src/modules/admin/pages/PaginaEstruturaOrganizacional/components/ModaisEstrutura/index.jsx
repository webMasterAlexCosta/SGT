import React from "react";
import { formatarGestorEstadual } from "../../../../../../core/formatters/nomenclaturaGestao";
import { Botao, CampoEntrada } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

const Sobreposicao = ({ children, aoFechar }) => <div className={estilos.sobreposicao} onMouseDown={(e) => e.target === e.currentTarget && aoFechar()}>{children}</div>;

export const ModaisEstrutura = ({
  modal, administrador, formSuperintendencia, setFormSuperintendencia,
  formGestaoLocal, setFormGestaoLocal, superintendencias,
  aoFechar, aoSalvarSuperintendencia, aoSalvarGestaoLocal,
}) => (
  <>
    {modal === "superintendencia" && <Sobreposicao aoFechar={aoFechar}><form className={estilos.modal} onSubmit={aoSalvarSuperintendencia}><h2>Editar gestor estadual</h2><CampoEntrada label="UF" value={formSuperintendencia.uf} maxLength={2} onChange={(e) => setFormSuperintendencia({ ...formSuperintendencia, uf: e.target.value.toUpperCase() })} required disabled={Boolean(formSuperintendencia.id)} /><CampoEntrada label="Identificação" value={formSuperintendencia.uf ? `Gestor estadual/${formSuperintendencia.uf.toUpperCase()}` : "Gestor estadual/UF"} disabled /><label className={estilos.checkbox}><input type="checkbox" checked={formSuperintendencia.ativo} onChange={(e) => setFormSuperintendencia({ ...formSuperintendencia, ativo: e.target.checked })} /> Ativo</label><div className={estilos.rodape}><Botao variant="secondary" type="button" onClick={aoFechar}>Cancelar</Botao><Botao type="submit">Salvar</Botao></div></form></Sobreposicao>}
    {administrador && modal === "gestaoLocal" && <Sobreposicao aoFechar={aoFechar}><form className={estilos.modal} onSubmit={aoSalvarGestaoLocal}><h2>{formGestaoLocal.id ? "Editar Gestão Local" : "Nova Gestão Local"}</h2><CampoEntrada label="Nome" value={formGestaoLocal.nome} onChange={(e) => setFormGestaoLocal({ ...formGestaoLocal, nome: e.target.value })} required /><CampoEntrada label="Código" value={formGestaoLocal.codigo} onChange={(e) => setFormGestaoLocal({ ...formGestaoLocal, codigo: e.target.value.toUpperCase() })} required /><label className={estilos.campo}>Gestor estadual responsável<select value={formGestaoLocal.superintendenciaId} onChange={(e) => setFormGestaoLocal({ ...formGestaoLocal, superintendenciaId: e.target.value })} required disabled={Boolean(formGestaoLocal.id)}><option value="">Selecione...</option>{superintendencias.filter((item) => item?.ativo !== false || String(item?.id) === String(formGestaoLocal.superintendenciaId)).map((item) => <option key={item.id} value={item.id}>{formatarGestorEstadual(item, item.uf)}{item?.ativo === false ? " · inativo (histórico)" : ""}</option>)}</select></label><label className={estilos.checkbox}><input type="checkbox" checked={formGestaoLocal.ativo} onChange={(e) => setFormGestaoLocal({ ...formGestaoLocal, ativo: e.target.checked })} /> Ativo</label><div className={estilos.rodape}><Botao variant="secondary" type="button" onClick={aoFechar}>Cancelar</Botao><Botao type="submit">Salvar</Botao></div></form></Sobreposicao>}
  </>
);
