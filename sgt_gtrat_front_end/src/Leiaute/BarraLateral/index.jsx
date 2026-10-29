import React, { useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";
import {
  AlertOctagon,
  AlertTriangle,
  Briefcase,
  Building2,
  CalendarCheck,
  CalendarClock,
  ClipboardList,
  Clock,
  CreditCard,
  FileBarChart,
  KeyRound,
  Landmark,
  LayoutDashboard,
  MapPin,
  ScrollText,
  Settings2,
  ShieldCheck,
  Workflow,
  UserCog,
  Users,
  X,
} from "lucide-react";

import Homologacao from "../../core/ui/Homologacao.jsx";
import logoGestaoLocal from "../../assets/logo-siggtrat.png";
import { CONFIG } from "../../core/config";
import { rotuloPerfil } from "../../core/security/roles";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../core/formatters/nomenclaturaGestao.js";
import { ItemNavegacao } from "./components/ItemNavegacao";
import { SecaoMenu } from "./components/SecaoMenu";
import { SecaoSubMenu } from "./components/SecaoSubMenu";
import { RodapeBarraLateral } from "./components/RodapeBarraLateral";
import estilos from "./index.module.css";

const obterEscopo = (usuario) => {
  const gestaoLocalBruto = usuario?.gestaoLocal?.label || usuario?.gestaoLocal?.nome || usuario?.gestaoLocalNome;
  const gestorEstadualBruto = usuario?.superintendencia?.label || usuario?.superintendencia?.nome || usuario?.superintendenciaNome;
  const lotacao = usuario?.lotacao?.label || usuario?.lotacao?.nome || usuario?.lotacaoNome;
  const gestaoLocal = gestaoLocalBruto ? formatarGestaoLocal(gestaoLocalBruto, usuario?.gestaoLocal?.codigo) : "";
  const gestorEstadual = gestorEstadualBruto || usuario?.uf
    ? formatarGestorEstadual(gestorEstadualBruto, usuario?.uf)
    : "";
  if (lotacao && gestaoLocal) return `${gestaoLocal} · ${lotacao}`;
  return gestaoLocal || gestorEstadual || "Abrangência nacional";
};

const BarraLateral = ({
  mobileMenu,
  modoMobile = false,
  onCloseMenu,
  usuario,
  permissoes,
  onNavigate,
  sectionsOpen,
  toggleSection,
}) => {
  const localizacao = useLocation();
  const referenciaFechar = useRef(null);

  useEffect(() => {
    if (!modoMobile || !mobileMenu) return undefined;
    referenciaFechar.current?.focus?.();
    const fecharComEsc = (evento) => {
      if (evento.key === "Escape") onCloseMenu?.();
    };
    document.addEventListener("keydown", fecharComEsc);
    return () => document.removeEventListener("keydown", fecharComEsc);
  }, [modoMobile, mobileMenu, onCloseMenu]);
  const podeVerPessoas = permissoes.pessoasVinculos ?? permissoes.empregados;
  const possuiGestaoOperacional = podeVerPessoas || permissoes.frequencia || permissoes.ajustesFrequencia || permissoes.ocorrencias || permissoes.pendencias || permissoes.governanca;
  const possuiConfiguracoes = permissoes.contratos || permissoes.calendario || permissoes.lotacoesTurnos
    || permissoes.estruturaOrganizacional || permissoes.usuarios || permissoes.auditoria
    || permissoes.catalogosReferencia || permissoes.tiposContratacao || permissoes.configuracoesSistema
    || permissoes.permissoesExcepcionais;

  return (
    <aside
      id="navegacao-principal"
      className={`${estilos.barraLateral} ${mobileMenu ? estilos.barraAberta : estilos.barraFechada}`}
      aria-label="Menu principal"
      aria-hidden={modoMobile && !mobileMenu ? true : undefined}
      inert={modoMobile && !mobileMenu ? true : undefined}
    >
      <div className={estilos.camadaBrilho} aria-hidden="true">
        <div className={`${estilos.feixeBrilho} ${estilos.efeitoBrilho}`} />
      </div>

      {modoMobile && (
        <button
          ref={referenciaFechar}
          type="button"
          className={estilos.botaoFecharMobile}
          onClick={onCloseMenu}
          aria-label="Fechar menu principal"
        >
          <X size={20} aria-hidden="true" />
        </button>
      )}

      <div className={estilos.secaoLogotipo}>
        <div className={estilos.envoltorioLogotipo}>
          <img src={logoGestaoLocal} alt="SGT-GTRAT" width="36" height="36" className={estilos.imagemLogotipo} />
          <span className={estilos.nomeAplicativo}>{CONFIG.APP_NAME}</span>
        </div>
        <Homologacao />
        <div className={estilos.resumoAcesso}>
          <strong>{rotuloPerfil(usuario)}</strong>
          <span>{obterEscopo(usuario)}</span>
        </div>
      </div>

      <nav className={estilos.menuNavegacao} aria-label="Navegação principal">
        {permissoes.painel && (
          <ItemNavegacao
            icone={LayoutDashboard}
            rotulo="Painel gerencial"
            id="/painel"
            ativo={localizacao.pathname === "/painel"}
            aoNavegar={onNavigate}
          />
        )}

        {possuiGestaoOperacional && (
          <SecaoMenu titulo="Gestão de terceirizados" estaAberto={sectionsOpen.rh} aoAlternar={() => toggleSection("rh")}>
            {podeVerPessoas && (
              <ItemNavegacao icone={Users} rotulo={<><span>Pessoas e vínculos</span><span className="somenteLeitorTela">Cadastro de empregados</span></>} id="/pessoas" ativo={["/pessoas", "/empregados"].includes(localizacao.pathname)} aoNavegar={onNavigate} />
            )}
            {permissoes.frequencia && (
              <ItemNavegacao icone={CalendarCheck} rotulo="Frequência diária" id="/frequencia" ativo={localizacao.pathname === "/frequencia"} aoNavegar={onNavigate} />
            )}
            {permissoes.ajustesFrequencia && (
              <ItemNavegacao icone={CalendarClock} rotulo="Exceções e adicionais" id="/frequencia-ajustes" ativo={localizacao.pathname === "/frequencia-ajustes"} aoNavegar={onNavigate} />
            )}
            {permissoes.ocorrencias && (
              <ItemNavegacao icone={AlertTriangle} rotulo="Ocorrências" id="/ocorrencias" ativo={localizacao.pathname === "/ocorrencias"} aoNavegar={onNavigate} />
            )}
            {permissoes.pendencias && (
              <ItemNavegacao icone={ClipboardList} rotulo="Pendências administrativas" id="/pendencias" ativo={localizacao.pathname === "/pendencias"} aoNavegar={onNavigate} />
            )}
            {permissoes.governanca && (
              <ItemNavegacao icone={Workflow} rotulo="Governança do ciclo" id="/governanca-terceiros" ativo={localizacao.pathname === "/governanca-terceiros"} aoNavegar={onNavigate} />
            )}
          </SecaoMenu>
        )}

        {(permissoes.relatorios || permissoes.relatoriosAdministrativos) && (
          <SecaoMenu titulo="Relatórios gerenciais" estaAberto={sectionsOpen.relatorios} aoAlternar={() => toggleSection("relatorios")}>
            {permissoes.visaoConsolidadaRelatorios && (
              <ItemNavegacao icone={FileBarChart} rotulo="Visão consolidada" id="/relatorios" ativo={localizacao.pathname === "/relatorios"} aoNavegar={onNavigate} />
            )}
            {permissoes.relatorios && <ItemNavegacao icone={FileBarChart} rotulo="Frequência mensal" id="/relatorio-mensal" ativo={localizacao.pathname === "/relatorio-mensal"} aoNavegar={onNavigate} />}
            {permissoes.relatorios && <ItemNavegacao icone={AlertOctagon} rotulo="Absenteísmo" id="/relatorio-absenteismo" ativo={localizacao.pathname === "/relatorio-absenteismo"} aoNavegar={onNavigate} />}
            {permissoes.relatoriosAdministrativos && <ItemNavegacao icone={ScrollText} rotulo="Emissões administrativas" id="/relatorios-administrativos" ativo={localizacao.pathname === "/relatorios-administrativos"} aoNavegar={onNavigate} />}
          </SecaoMenu>
        )}

        {possuiConfiguracoes && (
          <SecaoMenu titulo="Cadastros e configurações" estaAberto={sectionsOpen.config} aoAlternar={() => toggleSection("config")}>
            {permissoes.contratos && (
              <ItemNavegacao icone={ScrollText} rotulo="Contratos e efetivo" id="/contratos" ativo={localizacao.pathname === "/contratos"} aoNavegar={onNavigate} eSubItem />
            )}
            {permissoes.estruturaOrganizacional && (
              <ItemNavegacao icone={Building2} rotulo="Estrutura organizacional" id="/estrutura-organizacional" ativo={localizacao.pathname === "/estrutura-organizacional"} aoNavegar={onNavigate} eSubItem />
            )}
            {permissoes.calendario && (
              <ItemNavegacao icone={CalendarCheck} rotulo="Feriados e folgas" id="/calendario-folgas" ativo={localizacao.pathname === "/calendario-folgas"} aoNavegar={onNavigate} eSubItem />
            )}

            {permissoes.lotacoesTurnos && (
              <SecaoSubMenu titulo="Operação da Gestão Local" estaAberto={sectionsOpen.sub_estrutura} aoAlternar={() => toggleSection("sub_estrutura")}>
                <ItemNavegacao icone={MapPin} rotulo="Lotações" id="/lotacoes" ativo={localizacao.pathname === "/lotacoes"} aoNavegar={onNavigate} eSubItem />
                <ItemNavegacao icone={Clock} rotulo="Turnos de trabalho" id="/turnos" ativo={localizacao.pathname === "/turnos"} aoNavegar={onNavigate} eSubItem />
              </SecaoSubMenu>
            )}

            {(permissoes.catalogosReferencia || permissoes.tiposContratacao) && (
              <>
                <SecaoSubMenu titulo="Catálogos operacionais" estaAberto={sectionsOpen.sub_catalogos} aoAlternar={() => toggleSection("sub_catalogos")}>
                  {permissoes.catalogosReferencia && <ItemNavegacao icone={Briefcase} rotulo="Cargos e CBO" id="/cargos" ativo={localizacao.pathname === "/cargos"} aoNavegar={onNavigate} eSubItem />}
                  {permissoes.tiposContratacao && <ItemNavegacao icone={ScrollText} rotulo="Tipos de contratação" id="/tipos-contratacao" ativo={localizacao.pathname === "/tipos-contratacao"} aoNavegar={onNavigate} eSubItem />}
                  {permissoes.catalogosReferencia && <ItemNavegacao icone={Users} rotulo="Status de empregado" id="/status-empregados" ativo={localizacao.pathname === "/status-empregados"} aoNavegar={onNavigate} eSubItem />}
                </SecaoSubMenu>
                {permissoes.catalogosReferencia && (
                  <SecaoSubMenu titulo="Dados bancários" estaAberto={sectionsOpen.sub_banco} aoAlternar={() => toggleSection("sub_banco")}>
                    <ItemNavegacao icone={Landmark} rotulo="Bancos" id="/bancos" ativo={localizacao.pathname === "/bancos"} aoNavegar={onNavigate} eSubItem />
                    <ItemNavegacao icone={Building2} rotulo="Agências" id="/agencias" ativo={localizacao.pathname === "/agencias"} aoNavegar={onNavigate} eSubItem />
                    <ItemNavegacao icone={CreditCard} rotulo="Tipos de conta" id="/tipos-conta" ativo={localizacao.pathname === "/tipos-conta"} aoNavegar={onNavigate} eSubItem />
                  </SecaoSubMenu>
                )}
              </>
            )}

            {(permissoes.usuarios || permissoes.permissoesExcepcionais || permissoes.configuracoesSistema || permissoes.auditoria) && (
              <SecaoSubMenu titulo="Acessos e auditoria" estaAberto={sectionsOpen.sub_sistema} aoAlternar={() => toggleSection("sub_sistema")}>
                {permissoes.usuarios && (
                  <ItemNavegacao icone={ShieldCheck} rotulo="Usuários e acessos" id="/gestao-usuarios" ativo={localizacao.pathname === "/gestao-usuarios"} aoNavegar={onNavigate} eSubItem />
                )}
                {permissoes.permissoesExcepcionais && (
                  <ItemNavegacao icone={KeyRound} rotulo="Permissões excepcionais" id="/permissoes-excepcionais" ativo={localizacao.pathname === "/permissoes-excepcionais"} aoNavegar={onNavigate} eSubItem />
                )}
                {permissoes.configuracoesSistema && (
                  <ItemNavegacao icone={Settings2} rotulo="Configurações do sistema" id="/configuracoes-sistema" ativo={localizacao.pathname === "/configuracoes-sistema"} aoNavegar={onNavigate} eSubItem />
                )}
                {permissoes.auditoria && (
                  <ItemNavegacao icone={ScrollText} rotulo="Logs de auditoria" id="/logs" ativo={localizacao.pathname === "/logs"} aoNavegar={onNavigate} eSubItem />
                )}
              </SecaoSubMenu>
            )}
          </SecaoMenu>
        )}

        <div className={estilos.linhaDivisoria} />
        <ItemNavegacao icone={UserCog} rotulo="Meu perfil" id="/perfil" ativo={localizacao.pathname === "/perfil"} aoNavegar={onNavigate} />
      </nav>

      <RodapeBarraLateral />
    </aside>
  );
};

export default BarraLateral;
