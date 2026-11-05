import React from "react";
import { Building2 } from "lucide-react";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../../../../../core/formatters/nomenclaturaGestao";
import { Botao, CampoEntrada, JanelaModal } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

export const ModalEvento = ({
  aberto, editando, dados, setDados, acessoGlobal, gestoesLocais,
  municipiosDisponiveis, lotacoesDisponiveis,
  aoFechar, aoSalvar, aoAlterarAbrangencia, aoAlterarGestaoLocal, aoAlterarLotacao,
}) => (
  <JanelaModal isOpen={aberto} onClose={aoFechar} title={editando ? "Editar evento do calendário" : "Cadastrar evento no calendário"} maxWidth="max-w-2xl">
    <form className={estilos.formulario} onSubmit={(e) => { e.preventDefault(); aoSalvar(); }}>
      <div className={estilos.grade}>
        <CampoEntrada label="Data" type="date" value={dados.data} onChange={(e) => setDados((atual) => ({ ...atual, data: e.target.value }))} required />
        <label className={estilos.campo}>Tipo do evento<input list="tipos-evento-calendario" value={dados.tipo} onChange={(e) => setDados((atual) => ({ ...atual, tipo: e.target.value.toUpperCase() }))} maxLength={40} required /><datalist id="tipos-evento-calendario"><option value="FERIADO" /><option value="DIA_DESCANSO" /></datalist><small>Feriado e dia de descanso são sugestões; outros tipos devolvidos/aceitos pelo Java permanecem disponíveis.</small></label>
      </div>
      <section className={estilos.escopo}>
        <header><Building2 size={18} /><div><strong>Abrangência do evento</strong><span>Defina quais empregados terão a frequência afetada.</span></div></header>
        <div className={estilos.grade}>
          <label className={estilos.campo}>Abrangência<select value={dados.abrangencia} onChange={(e) => aoAlterarAbrangencia(e.target.value)}>{acessoGlobal && <option value="NACIONAL">Nacional</option>}<option value="ESTADUAL">Estadual</option><option value="MUNICIPAL">Municipal</option><option value="LOTACAO">Lotação específica</option></select></label>
          {acessoGlobal && dados.abrangencia !== "NACIONAL" && <label className={estilos.campo}>Gestão Local / estadual<select value={dados.gestaoLocalId} onChange={(e) => aoAlterarGestaoLocal(e.target.value)} required><option value="">Selecione...</option>{gestoesLocais.map((gestaoLocal) => <option key={gestaoLocal.id} value={gestaoLocal.id}>{formatarGestaoLocal(gestaoLocal)} · {formatarGestorEstadual(gestaoLocal.superintendencia || gestaoLocal.superintendenciaNome, gestaoLocal.uf)}</option>)}</select></label>}
          {dados.abrangencia === "MUNICIPAL" && <label className={estilos.campo}>Município afetado<select value={dados.municipio} onChange={(e) => setDados((atual) => ({ ...atual, municipio: e.target.value }))} required><option value="">Selecione um município...</option>{municipiosDisponiveis.map((municipio) => <option key={municipio} value={municipio}>{municipio}</option>)}</select>{!municipiosDisponiveis.length && <small>Cadastre o município em uma lotação antes de criar o feriado municipal.</small>}</label>}
          {dados.abrangencia === "LOTACAO" && <label className={estilos.campo}>Lotação afetada<select value={dados.lotacaoId} onChange={(e) => aoAlterarLotacao(e.target.value)} required><option value="">Selecione...</option>{lotacoesDisponiveis.map((lotacao) => <option key={lotacao.id} value={lotacao.id}>{lotacao.nome}{lotacao.municipio ? ` · ${lotacao.municipio}` : ""}</option>)}</select></label>}
        </div>
      </section>
      <CampoEntrada label="Descrição" value={dados.descricao} onChange={(e) => setDados((atual) => ({ ...atual, descricao: e.target.value }))} placeholder="Ex.: Aniversário do município" />
      <label className={estilos.checkbox}><input type="checkbox" checked={Boolean(dados.ativo)} onChange={(e) => setDados((atual) => ({ ...atual, ativo: e.target.checked }))} /><span><strong>Evento ativo</strong><small>Considerar este evento nos cálculos do escopo definido.</small></span></label>
      <div className={estilos.rodape}><Botao type="button" variant="outline" onClick={aoFechar}>Cancelar</Botao><Botao type="submit">{editando ? "Salvar alterações" : "Cadastrar evento"}</Botao></div>
    </form>
  </JanelaModal>
);
