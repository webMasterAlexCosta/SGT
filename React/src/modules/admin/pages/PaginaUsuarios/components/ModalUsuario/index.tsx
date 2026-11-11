import React from "react";
import { Plus, X } from "lucide-react";
import { formatarMatricula } from "../../../../../../core/formatters/matricula";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../../../../../core/formatters/nomenclaturaGestao";
import { PERFIS, rotuloPerfil } from "../../../../../../core/security/roles";
import { Botao, CampoEntrada, JanelaModal } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

export const ModalUsuario = ({
  aberto, aoFechar, editando, dados, setDados,
  perfisPermitidos, superintendencias, gestoesLocaisDisponiveis,
  lotacoesDisponiveis, jornadasDisponiveis,
  aoAlterarPerfil, aoAlterarSuperintendencia, aoAlterarGestaoLocal,
  aoAdicionarVinculo, aoRemoverVinculo, aoSalvar,
  propriaConta = false, senhaMinima = 8,
}: { aberto: (boolean); aoFechar: (() => void); editando: (boolean); dados: import("../../../../../../contracts/usuarios").UserForm; setDados: React.Dispatch<React.SetStateAction<import("../../../../../../contracts/usuarios").UserForm>>; perfisPermitidos: (("ADMINISTRADOR" | "GESTOR_NACIONAL" | "GESTOR_ESTADUAL" | "GESTOR_LOCAL" | "PRESIDENTE" | "OPERACIONAL")[]); superintendencias: (import("../../../../../../contracts/java").SuperintendenciaDTO[]); gestoesLocaisDisponiveis: (import("../../../../../../contracts/opcoes-apresentacao").LocalManagementView[]); lotacoesDisponiveis: (import("../../../../../../contracts/opcoes-apresentacao").LocationView[]); jornadasDisponiveis: (import("../../../../../../contracts/opcoes-apresentacao").ShiftSummaryView[]); aoAlterarPerfil: (valor: import("../../../../../../contracts/java").PerfilAcesso) => void; aoAlterarSuperintendencia: (valor: string) => void; aoAlterarGestaoLocal: (valor: string) => void; aoAdicionarVinculo: (() => void); aoRemoverVinculo: (vinculo: import("../../../../../../contracts/usuarios").ManagementLinkForm) => void; aoSalvar: (() => Promise<void>); propriaConta?: (boolean); senhaMinima?: number }) => (
  <JanelaModal isOpen={aberto} onClose={aoFechar} title={editando ? "Editar usuário" : "Novo usuário"}>
    <div className={estilos.formulario}>
      <div className={estilos.duasColunas}>
        <CampoEntrada label="Usuário" value={dados.username} onChange={(e) => setDados((atual) => ({ ...atual, username: e.target.value }))} disabled={editando} />
        <CampoEntrada label="Matrícula" value={dados.matricula} onChange={(e) => setDados((atual) => ({ ...atual, matricula: formatarMatricula(e.target.value) }))} inputMode="numeric" maxLength={11} placeholder="8.961.474-5" />
      </div>
      <CampoEntrada label="E-mail institucional" type="email" value={dados.email} onChange={(e) => setDados((atual) => ({ ...atual, email: e.target.value }))} placeholder="nome.sobrenome@correios.com.br" ajuda="Obrigatório utilizar o domínio @correios.com.br." />
      {!editando && <CampoEntrada label="Senha inicial" type="password" value={dados.password} onChange={(e) => setDados((atual) => ({ ...atual, password: e.target.value }))} autoComplete="new-password" minLength={senhaMinima} maxLength={72} ajuda={`Mínimo de ${senhaMinima} caracteres no ambiente Java de referência; o servidor valida a política final.`} />}

      <div className={estilos.duasColunas}>
        <label className={estilos.campo}><span>Perfil</span><select value={dados.perfil} onChange={(e) => { const perfil = perfisPermitidos.find((opcao) => opcao === e.target.value); if (perfil) aoAlterarPerfil(perfil); }} disabled={propriaConta}>{perfisPermitidos.map((perfil) => <option key={perfil} value={perfil}>{rotuloPerfil(perfil)}</option>)}</select>{propriaConta && <small>O Java não permite alterar o próprio perfil de acesso por esta tela.</small>}</label>
        <label className={estilos.campo}>
          <span>Status</span>
          <select
            value={String(dados.active)}
            disabled={Boolean(dados.id)}
            onChange={(e) => setDados((atual) => ({ ...atual, active: e.target.value === "true" }))}
          >
            <option value="true">Ativo</option>
            <option value="false">Inativo</option>
          </select>
          {dados.id && <small>Use a ação da lista para ativar ou desativar e aplicar as regras de segurança.</small>}
        </label>
      </div>

      {([PERFIS.GESTOR_ESTADUAL, PERFIS.GESTOR_LOCAL, PERFIS.OPERACIONAL] as readonly string[]).includes(dados.perfil) && (
        <section className={estilos.bloco}>
          <div><strong>Vínculo organizacional</strong><p>A Gestão Local determina automaticamente o gestor estadual correspondente.</p></div>
          <div className={estilos.duasColunas}>
            <label className={estilos.campo}><span>Gestor estadual</span><select value={dados.superintendenciaId} onChange={(e) => aoAlterarSuperintendencia(e.target.value)}><option value="">Selecione...</option>{superintendencias.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{formatarGestorEstadual(item, item.uf)}</option>)}</select></label>
            {([PERFIS.GESTOR_LOCAL, PERFIS.OPERACIONAL] as readonly string[]).includes(dados.perfil) && <label className={estilos.campo}><span>Gestão Local</span><select value={dados.gestaoLocalId} onChange={(e) => aoAlterarGestaoLocal(e.target.value)}><option value="">Selecione...</option>{gestoesLocaisDisponiveis.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{formatarGestaoLocal(item)}</option>)}</select></label>}
          </div>
        </section>
      )}

      {dados.perfil === PERFIS.OPERACIONAL && (
        <section className={estilos.bloco}>
          <div><strong>Escopo operacional</strong><p>O usuário lançará frequência somente nas lotações e turnos selecionados.</p></div>
          <div className={estilos.gradeVinculo}>
            <label className={estilos.campo}><span>Lotação da Gestão Local</span><select value={dados.novoLotacaoId} onChange={(e) => setDados((atual) => ({ ...atual, novoLotacaoId: e.target.value, novoJornadaId: "" }))} disabled={!dados.gestaoLocalId}><option value="">Selecione...</option>{lotacoesDisponiveis.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.nome}{item.municipio ? ` — ${item.municipio}` : ""}</option>)}</select></label>
            <label className={estilos.campo}><span>Turno</span><select value={dados.novoJornadaId} onChange={(e) => setDados((atual) => ({ ...atual, novoJornadaId: e.target.value }))} disabled={!dados.novoLotacaoId}><option value="">Selecione...</option>{jornadasDisponiveis.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.descricao || item.nome}</option>)}</select></label>
            <Botao type="button" onClick={aoAdicionarVinculo} disabled={!dados.novoLotacaoId || !dados.novoJornadaId}><Plus size={16} /> Adicionar</Botao>
          </div>
          <div className={estilos.vinculos}>
            {dados.vinculosGestao.length
              ? dados.vinculosGestao.map((item) => <span key={`${item.lotacaoId}:${item.jornadaId}`}>{item.lotacaoNome} · {item.jornadaNome}<button type="button" onClick={() => aoRemoverVinculo(item)} aria-label="Remover vínculo"><X size={14} /></button></span>)
              : <em>Nenhum vínculo definido.</em>}
          </div>
        </section>
      )}

      <div className={estilos.rodape}><Botao variant="secondary" onClick={aoFechar}>Cancelar</Botao><Botao onClick={aoSalvar}>{editando ? "Salvar alterações" : "Criar usuário"}</Botao></div>
    </div>
  </JanelaModal>
);
