import React from "react";
import { Building2, CalendarRange, FilePenLine, Save, ShieldCheck, X } from "lucide-react";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../../../../../core/formatters/nomenclaturaGestao.ts";
import { useDialogoAcessivel } from "../../../../../../core/ui/acessibilidadeDialogo.ts";
import estilos from "./index.module.css";

export const ModalContrato = ({
  aberto,
  dadosFormulario,
  gestoesLocais,
  tiposContratacao = [],
  acessoGlobal,
  gestaoLocalDoUsuario,
  salvando,
  aoAlterarCampo,
  aoFechar,
  aoSalvar,
}: { aberto: (boolean); dadosFormulario: import("../../../../../../contracts/contratos").ContractForm; gestoesLocais: (import("../../../../../../contracts/opcoes-apresentacao").LocalManagementView[]); tiposContratacao?: (import("../../../../../../contracts/catalogos").CatalogDto<"/tipos-contratacao">[]); acessoGlobal: (boolean); gestaoLocalDoUsuario: ({ id: number | null; nome: string | null; uf: string | null; superintendenciaNome: string | null; }); salvando: (boolean); aoAlterarCampo: (campo: string, valor: string | number | boolean) => void; aoFechar: (() => void); aoSalvar: React.FormEventHandler<HTMLFormElement> }) => {
  const referenciaModal = useDialogoAcessivel({ aberto, aoFechar, bloquearFechamento: Boolean(salvando) });
  if (!aberto) return null;

  const estaEditando = Boolean(dadosFormulario.id);

  return (
    <div className={estilos.sobreposicaoModal} onMouseDown={(e) => e.target === e.currentTarget && !salvando && aoFechar()} role="presentation">
      <section className={estilos.cartaoModal} role="dialog" aria-modal="true" aria-labelledby="titulo-modal-contrato" tabIndex={-1} ref={referenciaModal}>
        <header className={estilos.cabecalhoModal}>
          <div className={estilos.identificacaoModal}>
            <div className={estilos.iconeModal}><FilePenLine size={21} /></div>
            <div><span>Rascunho contratual</span><h2 id="titulo-modal-contrato">{estaEditando ? "Editar rascunho" : "Novo rascunho"}</h2></div>
          </div>
          <button type="button" className={estilos.botaoFechar} onClick={aoFechar} disabled={salvando} aria-label="Fechar formulário"><X size={20} /></button>
        </header>

        <form className={estilos.formularioContrato} onSubmit={aoSalvar}>
          <div className={estilos.avisoRascunho}>
            <ShieldCheck size={18} />
            <span>Este formulário edita somente <strong>RASCUNHO</strong>. Após a publicação, alterações devem ocorrer por eventos negociais auditáveis.</span>
          </div>

          <div className={estilos.secaoFormulario}>
            <div className={estilos.tituloSecao}><Building2 size={17} /><span>Responsabilidade organizacional</span></div>
            {acessoGlobal ? (
              <label className={`${estilos.grupoCampo} ${estilos.campoLarguraTotal}`}>
                <span>Gestão Local responsável *</span>
                <select value={dadosFormulario.gestaoLocalId} onChange={(e) => aoAlterarCampo("gestaoLocalId", e.target.value)} required>
                  <option value="">Selecione a Gestão Local</option>
                  {gestoesLocais.filter((g) => g.ativo !== false).map((gestaoLocal) => (
                    <option key={gestaoLocal.id} value={(gestaoLocal.id) ?? undefined}>
                      {formatarGestorEstadual(gestaoLocal.superintendencia || gestaoLocal.superintendenciaNome, gestaoLocal.uf)} — {formatarGestaoLocal(gestaoLocal)}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <div className={estilos.gestaoLocalFixa}>
                <span>Gestão Local determinada pela sessão</span>
                <strong>{gestaoLocalDoUsuario.nome ? formatarGestaoLocal(gestaoLocalDoUsuario) : "Gestão Local não vinculada"}</strong>
                <small>{gestaoLocalDoUsuario.uf || gestaoLocalDoUsuario.superintendenciaNome ? formatarGestorEstadual(gestaoLocalDoUsuario.superintendenciaNome, gestaoLocalDoUsuario.uf) : "Gestor estadual não informado"}</small>
              </div>
            )}
          </div>

          <div className={estilos.secaoFormulario}>
            <div className={estilos.tituloSecao}><FilePenLine size={17} /><span>Identificação e empresa</span></div>
            <div className={estilos.gradeFormulario}>
              <label className={estilos.grupoCampo}><span>Número do contrato</span><input value={dadosFormulario.numeroContrato} onChange={(e) => aoAlterarCampo("numeroContrato", e.target.value)} maxLength={80} placeholder="Ex.: 001/2026" /></label>
              <label className={estilos.grupoCampo}><span>Ano</span><input type="number" min="1900" max="9999" value={dadosFormulario.anoContrato} onChange={(e) => aoAlterarCampo("anoContrato", e.target.value)} placeholder="2026" /></label>
              <label className={estilos.grupoCampo}>
                <span>Tipo de contratação</span>
                <select value={dadosFormulario.tipoContratacaoId} onChange={(e) => aoAlterarCampo("tipoContratacaoId", e.target.value)}>
                  <option value="">Selecione</option>
                  {tiposContratacao.filter((t) => t.ativo !== false || String(t.id) === String(dadosFormulario.tipoContratacaoId)).map((tipo) => <option key={tipo.id} value={(tipo.id) ?? undefined}>{tipo.descricao}{tipo.ativo === false ? " (inativo — histórico)" : ""}</option>)}
                </select>
              </label>
              <label className={estilos.grupoCampo}><span>Efetivo total previsto</span><input type="number" min="0" step="1" value={dadosFormulario.efetivoPrevisto ?? 0} onChange={(e) => aoAlterarCampo("efetivoPrevisto", e.target.value)} /></label>
              <label className={`${estilos.grupoCampo} ${estilos.campoLarguraTotal}`}><span>Descrição</span><textarea value={dadosFormulario.descricao} onChange={(e) => aoAlterarCampo("descricao", e.target.value)} rows={3} maxLength={500} placeholder="Objeto do contrato e serviço terceirizado" /><small>{String(dadosFormulario.descricao || "").length}/500 caracteres</small></label>
              <label className={estilos.grupoCampo}><span>CNPJ da empresa</span><input inputMode="numeric" value={dadosFormulario.cnpjEmpresa} onChange={(e) => aoAlterarCampo("cnpjEmpresa", e.target.value)} maxLength={18} placeholder="00.000.000/0000-00" /></label>
              <label className={estilos.grupoCampo}><span>Razão social</span><input value={dadosFormulario.razaoSocialEmpresa} onChange={(e) => aoAlterarCampo("razaoSocialEmpresa", e.target.value)} maxLength={180} /></label>
              <label className={`${estilos.grupoCampo} ${estilos.campoLarguraTotal}`}><span>Nome fantasia</span><input value={dadosFormulario.nomeFantasiaEmpresa} onChange={(e) => aoAlterarCampo("nomeFantasiaEmpresa", e.target.value)} maxLength={180} /></label>
            </div>
          </div>

          <div className={estilos.secaoFormulario}>
            <div className={estilos.tituloSecao}><CalendarRange size={17} /><span>Vigência e vínculo</span></div>
            <div className={estilos.gradeFormulario}>
              <label className={estilos.grupoCampo}><span>Data de início</span><input type="date" value={dadosFormulario.dataInicio} onChange={(e) => aoAlterarCampo("dataInicio", e.target.value)} /></label>
              <label className={estilos.grupoCampo}><span>Data de término</span><input type="date" value={dadosFormulario.dataFim} min={dadosFormulario.dataInicio || undefined} onChange={(e) => aoAlterarCampo("dataFim", e.target.value)} /></label>
              <label className={`${estilos.controleStatus} ${estilos.campoLarguraTotal}`}>
                <input type="checkbox" checked={Boolean(dadosFormulario.matriculaVinculoObrigatoria)} onChange={(e) => aoAlterarCampo("matriculaVinculoObrigatoria", e.target.checked)} />
                <span className={estilos.trilhoStatus}><span /></span>
                <span><strong>Exigir matrícula no vínculo</strong><small>Define se o vínculo contratual do colaborador exige matrícula específica.</small></span>
              </label>
            </div>
          </div>

          <footer className={estilos.rodapeModal}>
            <button type="button" className={estilos.botaoCancelar} onClick={aoFechar} disabled={salvando}>Cancelar</button>
            <button type="submit" className={estilos.botaoSalvar} disabled={salvando}><Save size={17} />{salvando ? "Salvando..." : estaEditando ? "Salvar rascunho" : "Criar rascunho"}</button>
          </footer>
        </form>
      </section>
    </div>
  );
};
