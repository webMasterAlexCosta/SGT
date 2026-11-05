import React, { useEffect, useState } from "react";
import { Plus, Save } from "lucide-react";
import { CATEGORIAS_TERMINO } from "../../../../../services/rh/ServicoDominioPessoaVinculo.js";
import { notificar } from "../../../../../core/events.js";
import { ModalEstrutural } from "./ModalEstrutural.jsx";
import estilos from "../index.module.css";

const VAZIO = Object.freeze({ id: "", descricao: "", categoria: "DESLIGAMENTO_INDIVIDUAL", ativo: true, version: 0 });

export const ModalMotivosDesligamento = ({ aberto, motivos = [], processando, aoFechar, aoSalvar }) => {
  const [dados, setDados] = useState({ ...VAZIO });
  useEffect(() => { if (aberto) setDados({ ...VAZIO }); }, [aberto]);

  const editar = (motivo) => setDados({
    id: motivo.id,
    descricao: motivo.descricao || "",
    categoria: motivo.categoria || "DESLIGAMENTO_INDIVIDUAL",
    ativo: motivo.ativo !== false,
    version: motivo.version || 0,
  });

  const enviar = async (evento) => {
    evento.preventDefault();
    if (!dados.descricao.trim() || !dados.categoria) { notificar("warning", "Descrição e categoria são obrigatórias."); return; }
    await aoSalvar({ ...dados, descricao: dados.descricao.trim() });
    setDados({ ...VAZIO });
  };

  return (
    <ModalEstrutural aberto={aberto} titulo="Motivos de desligamento" subtitulo="Catálogo da Gestão Local separado do status temporal do vínculo" aoFechar={aoFechar}>
      <div className={estilos.catalogoMotivos}>
        <div className={estilos.listaMotivos}>
          {motivos.length === 0 ? <p className={estilos.estadoVazio}>Nenhum motivo disponível no seu escopo.</p> : motivos.map((motivo) => (
            <button type="button" key={motivo.id} className={String(dados.id) === String(motivo.id) ? estilos.itemMotivoAtivo : estilos.itemMotivo} onClick={() => editar(motivo)}>
              <span><strong>{motivo.descricao}</strong><small>{CATEGORIAS_TERMINO.find((item) => item.value === motivo.categoria)?.label || motivo.categoria}</small></span>
              <em>{motivo.ativo === false ? "Inativo" : "Ativo"}</em>
            </button>
          ))}
        </div>
        <form className={estilos.formularioCatalogo} onSubmit={enviar}>
          <h3>{dados.id ? "Editar motivo" : "Novo motivo"}</h3>
          <label><span>Descrição *</span><input data-foco-inicial value={dados.descricao} onChange={(e) => setDados((a) => ({ ...a, descricao: e.target.value }))} maxLength={160} /></label>
          <label><span>Categoria *</span><select value={dados.categoria} onChange={(e) => setDados((a) => ({ ...a, categoria: e.target.value }))}>{CATEGORIAS_TERMINO.map((categoria) => <option key={categoria.value} value={categoria.value}>{categoria.label}</option>)}</select></label>
          <label className={estilos.confirmacao}><input type="checkbox" checked={dados.ativo} onChange={(e) => setDados((a) => ({ ...a, ativo: e.target.checked }))} /> Motivo ativo</label>
          <div className={estilos.acoesFormulario}><button type="button" className={estilos.botaoSecundario} onClick={() => setDados({ ...VAZIO })}><Plus size={16} /> Limpar</button><button type="submit" className={estilos.botaoPrimario} disabled={processando}><Save size={16} /> {processando ? "Salvando…" : "Salvar"}</button></div>
        </form>
      </div>
    </ModalEstrutural>
  );
};
