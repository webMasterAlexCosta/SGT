import React from "react";
import { Plus, Search, Shield } from "lucide-react";
import { eGestaoLocal, eGestorEstadual, eGestorNacional, rotuloPerfil } from "../../../../../../core/security/roles";
import { Botao } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

const obterTexto = (gestor) => {
  if (eGestorNacional(gestor)) return {
    descricao: "O gestor nacional administra exclusivamente os usuários dos gestores estaduais.",
    busca: "Buscar usuário ou gestor estadual...",
  };
  if (eGestorEstadual(gestor)) return {
    descricao: "O gestor estadual administra exclusivamente as Gestões Locais do próprio estado.",
    busca: "Buscar Gestão Local...",
  };
  if (eGestaoLocal(gestor)) return {
    descricao: "A Gestão Local administra os usuários operacionais vinculados à própria operação.",
    busca: "Buscar usuário, lotação ou turno...",
  };
  return {
    descricao: "Perfis e vínculos são limitados ao escopo organizacional do gestor autenticado.",
    busca: "Buscar usuário, perfil ou escopo...",
  };
};

export const CabecalhoUsuarios = ({
  gestorAtual,
  termoBusca,
  aoBuscar,
  aoNovoUsuario,
  totalUsuarios = 0,
  filtroPerfil = "",
  aoFiltrarPerfil,
  filtroStatus = "",
  aoFiltrarStatus,
  perfisPermitidos = [],
}) => {
  const texto = obterTexto(gestorAtual);
  return (
    <header className={estilos.cabecalho}>
      <div className={estilos.titulo}>
        <span><Shield size={23} /></span>
        <div>
          <span className={estilos.sobretitulo}>Segurança e permissões</span>
          <h1>Usuários e acessos</h1>
          <p>{texto.descricao}</p>
        </div>
      </div>
      <div className={estilos.acoes}>
        <div className={estilos.resumo}>
          <span>Exibidos</span>
          <strong>{totalUsuarios}</strong>
        </div>
        <label className={estilos.busca}>
          <Search size={16} />
          <input type="search" placeholder={texto.busca} value={termoBusca} onChange={(evento) => aoBuscar(evento.target.value)} />
        </label>
        <label className={estilos.filtro}>
          <span>Perfil</span>
          <select value={filtroPerfil} onChange={(evento) => aoFiltrarPerfil?.(evento.target.value)}>
            <option value="">Todos</option>
            {perfisPermitidos.map((perfil) => <option key={perfil} value={perfil}>{rotuloPerfil(perfil)}</option>)}
          </select>
        </label>
        <label className={estilos.filtro}>
          <span>Status</span>
          <select value={filtroStatus} onChange={(evento) => aoFiltrarStatus?.(evento.target.value)}>
            <option value="">Todos</option>
            <option value="ATIVO">Ativos</option>
            <option value="INATIVO">Inativos</option>
            <option value="BLOQUEADO">Bloqueados</option>
            <option value="TROCA_PENDENTE">Troca de senha pendente</option>
          </select>
        </label>
        <Botao onClick={aoNovoUsuario}><Plus size={17} /> Novo usuário</Botao>
      </div>
    </header>
  );
};
