import React from "react";
import { ChevronLeft, ChevronRight, Edit, KeyRound, Lock, Trash, Unlock, UserCheck } from "lucide-react";
import { eGestaoLocal, possuiAcessoGlobal, PERFIS, rotuloPerfil } from "../../../../../../core/security/roles";
import { Cartao } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

const formatarDataHora = (valor) => {
  if (!valor) return null;
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return null;
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(data);
};

export const TabelaUsuarios = ({
  usuarios,
  carregando,
  gestorAtual,
  obterPerfil,
  podeGerenciarUsuario,
  descreverAbrangencia,
  aoAlternarRetroativo,
  aoRedefinirSenha,
  aoEditar,
  aoAlterarAtivacao,
  paginaAtual = 1,
  totalPaginas = 1,
  totalRegistros = 0,
  aoMudarPagina,
}) => (
  <Cartao className={estilos.cartaoTabela}>
    <div className={estilos.envoltorio}>
      <table className={estilos.tabela}>
        <thead>
          <tr><th>Usuário</th><th>Perfil</th><th>Escopo</th><th>Status</th><th className={estilos.direita}>Ações</th></tr>
        </thead>
        <tbody>
          {usuarios.map((usuario) => {
            const global = possuiAcessoGlobal(usuario);
            const operacional = obterPerfil(usuario) === PERFIS.OPERACIONAL;
            const gerenciavel = podeGerenciarUsuario(usuario);
            const propriaConta = String(usuario.id) === String(gestorAtual?.id);
            const podeAlterarAtivacao = usuario.active ? gerenciavel : operacional ? eGestaoLocal(gestorAtual) : gerenciavel;
            const expiraRetroativo = formatarDataHora(usuario.retroactiveUnlockExpireAt || usuario.retroactiveExpireAt);
            const fimBloqueio = formatarDataHora(usuario.lockoutEnd);
            return (
              <tr key={usuario.id}>
                <td><strong className={estilos.nome}>{usuario.username}</strong><span className={estilos.email}>{usuario.email || "—"}</span><span className={estilos.matricula}>Matrícula: {usuario.matricula || "—"}</span></td>
                <td><span className={global ? estilos.perfilAdmin : estilos.perfilUsuario}>{rotuloPerfil(usuario)}</span></td>
                <td><span className={estilos.escopo}>{descreverAbrangencia(usuario)}</span></td>
                <td className={estilos.status}>
                  <span className={usuario.active ? estilos.ativo : estilos.inativo}>{usuario.active ? "Ativo" : operacional ? "Aguardando Gestão Local" : "Inativo"}</span>
                  {usuario.locked && <span className={estilos.inativo} title={fimBloqueio ? `Bloqueado até ${fimBloqueio}` : "Conta bloqueada"}>Bloqueado{fimBloqueio ? ` até ${fimBloqueio}` : ""}</span>}
                  {usuario.forceChangePassword && <span className={estilos.pendente}>Troca pendente</span>}
                  {usuario.retroactiveUnlocked && <span className={estilos.retroativo} title={expiraRetroativo ? `Expira em ${expiraRetroativo}` : "Acesso retroativo temporário"}>Retroativo{expiraRetroativo ? ` até ${expiraRetroativo}` : ""}</span>}
                </td>
                <td className={estilos.direita}>
                  {gerenciavel && operacional && !propriaConta && <button type="button" onClick={() => aoAlternarRetroativo(usuario)} className={usuario.retroactiveUnlocked ? estilos.acaoAlerta : estilos.acaoSucesso} title="Acesso retroativo" aria-label={`${usuario.retroactiveUnlocked ? "Revogar" : "Conceder"} acesso retroativo de ${usuario.username}`}>{usuario.retroactiveUnlocked ? <Lock size={16} /> : <Unlock size={16} />}</button>}
                  {gerenciavel && !propriaConta && <button type="button" onClick={() => aoRedefinirSenha(usuario)} className={estilos.acaoEditar} title="Redefinir senha" aria-label={`Redefinir senha de ${usuario.username}`}><KeyRound size={16} /></button>}
                  {gerenciavel && <button type="button" onClick={() => aoEditar(usuario)} className={estilos.acaoEditar} title="Editar cadastro" aria-label={`Editar cadastro de ${usuario.username}`}><Edit size={16} /></button>}
                  {podeAlterarAtivacao && !propriaConta && (usuario.active
                    ? <button type="button" onClick={() => aoAlterarAtivacao(usuario)} className={estilos.acaoExcluir} title="Desativar e encerrar sessões" aria-label={`Desativar ${usuario.username}`}><Trash size={16} /></button>
                    : <button type="button" onClick={() => aoAlterarAtivacao(usuario)} className={estilos.acaoAlerta} title="Ativar e gerar senha temporária" aria-label={`Ativar ${usuario.username}`}><UserCheck size={16} /></button>)}
                  {!gerenciavel && <span className={estilos.somenteLeitura}>Somente leitura</span>}
                  {gerenciavel && propriaConta && <span className={estilos.somenteLeitura}>Conta atual</span>}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      {carregando && <div className={estilos.mensagem}>Carregando usuários...</div>}
      {!carregando && !usuarios.length && <div className={estilos.mensagem}>Nenhum usuário encontrado no seu escopo.</div>}
      {!carregando && totalRegistros > 0 && (
        <nav className={estilos.paginacao} aria-label="Paginação de usuários">
          <span>{totalRegistros} {totalRegistros === 1 ? "registro" : "registros"} · página {paginaAtual} de {totalPaginas}</span>
          <div>
            <button type="button" onClick={() => aoMudarPagina?.(Math.max(1, paginaAtual - 1))} disabled={paginaAtual <= 1} aria-label="Página anterior"><ChevronLeft size={16} /></button>
            <button type="button" onClick={() => aoMudarPagina?.(Math.min(totalPaginas, paginaAtual + 1))} disabled={paginaAtual >= totalPaginas} aria-label="Próxima página"><ChevronRight size={16} /></button>
          </div>
        </nav>
      )}
    </div>
  </Cartao>
);
