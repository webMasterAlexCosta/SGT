import React, {
  Suspense,
  lazy,
  useCallback,
  useMemo,
  useRef,
  useState,
  useEffect,
} from "react";
import {
  Routes,
  Route,
  useNavigate,
  useLocation,
  Navigate,
} from "react-router-dom";
import { ArrowUp } from "lucide-react";
import { CONFIG } from "./core/config";
import { combinarConfiguracaoEfetiva } from "./core/runtimeConfig.js";
import { ServicoSistema } from "./services/system/ServicoSistema.js";

import Rodape from "./Leiaute/Rodape/index.jsx";
import BarraLateral from "./Leiaute/BarraLateral/index.jsx";
import Cabecalho from "./Leiaute/Cabecalho/index.jsx";

import { ProvedorAutenticacao, useAutenticacao } from "./modules/auth/context";
import { ConteinerAvisos } from "./core/ui/Aviso";
import { ModalConexao } from "./core/ModalConexao";
import { FronteiraErro } from "./core/ui/FronteiraErro";
import { CarregamentoEmLinha } from "./core/ui/Carregamento";
import { LembreteSegundoFator } from "./core/ui/LembreteSegundoFator";
import { usePulsoAtividade } from "./hooks/usePulsoAtividade";
import { useConsultaMidia } from "./hooks/useConsultaMidia.js";
import {
  eAdministradorTecnico,
  podeAcessarAuditoria,
  podeVisualizarFrequencia,
  podeAcessarOcorrencias,
  podeAcessarPendenciasAdministrativas,
  podeAcessarPermissoesExcepcionais,
  podeGerenciarAjustesFrequencia,
  podeAcessarPainelGerencial,
  podeAcessarRelatorios,
  podeAcessarRelatoriosAdministrativos,
  podeEmitirRelatoriosAdministrativos,
  podeAcessarVisaoConsolidadaRelatorios,
  podeGerenciarCadastrosNacionais,
  podeVisualizarCatalogosReferencia,
  podeVisualizarTiposContratacao,
  podeGerenciarTiposContratacao,
  podeGerenciarCargos,
  podeVisualizarEstruturaOrganizacional,
  podeVisualizarLotacoesETurnos,
  podeVisualizarCalendario,
  podeVisualizarEmpregados,
  podeVisualizarPessoasVinculos,
  podeGerenciarEstruturaOrganizacional,
  podeGerenciarLotacoesETurnos,
  podeGerenciarUsuarios,
  podeGerenciarConfiguracoesSistema,
  podeVisualizarContratos,
  podeGerenciarContratos,
  rotaInicialPorPerfil,
} from "./core/security/roles";
import HomologacaoModal from "./core/ui/HomologacaoModal.jsx";
import { PaginaManutencao } from "./core/ui/PaginaManutencao";
import { ProvedorExportacaoRelatorio } from "./core/exportacao/context";
import { ProvedorConfirmacaoSenhaHttp } from "./core/security/ProvedorConfirmacaoSenhaHttp.jsx";
import { AcessoNegado } from "./core/security/AcessoNegado.jsx";

import estilos from "./Aplicacao.module.css";

// Lazy loading das rotas
const TelaLogin = lazy(() =>
  import("./modules/auth/pages/Login/index.jsx").then((m) => ({
    default: m.Login,
  })),
);

const AtualizacaoModulo = lazy(() => import("./modules/AtualizacaoModulo/index.jsx"));

const Painel = lazy(() =>
  import("./modules/painel/pages/index.jsx").then((m) => ({
    default: m.Painel, 
  })),
);

const PaginaPerfil = lazy(() =>
  import("./modules/perfil/pages/PaginaPerfil/index.jsx").then((m) => ({
    default: m.PaginaPerfil,
  })),
);
const PaginaPessoasVinculos = lazy(() =>
  import("./modules/rh/pages/PaginaPessoasVinculos/index.jsx").then((m) => ({
    default: m.PaginaPessoasVinculos,
  })),
);
const PaginaHistorico = lazy(() => import('./modules/historico/PaginaHistorico.jsx'));
const PaginaFrequencia = lazy(() =>
  import("./modules/rh/pages/PaginaFrequencia/index.jsx").then((m) => ({
    default: m.PaginaFrequencia,
  })),
);
const PaginaOcorrencias = lazy(() =>
  import("./modules/rh/pages/PaginaOcorrencias/index.jsx").then((m) => ({
    default: m.PaginaOcorrencias,
  })),
);
const PaginaAjustesFrequencia = lazy(() =>
  import("./modules/rh/pages/PaginaAjustesFrequencia/index.jsx").then((m) => ({ default: m.PaginaAjustesFrequencia })),
);
const PaginaPendenciasAdministrativas = lazy(() =>
  import("./modules/rh/pages/PaginaPendenciasAdministrativas/index.jsx").then((m) => ({ default: m.PaginaPendenciasAdministrativas })),
);
const PaginaRelatorioFrequencia = lazy(() =>
  import("./modules/rh/pages/PaginaRelatorioFrequencia/index.jsx").then((m) => ({
    default: m.PaginaRelatorioFrequencia,
  })),
);
const PaginaRelatorioAbsenteismo = lazy(() =>
  import("./modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx").then((m) => ({
    default: m.PaginaRelatorioAbsenteismo,
  })),
);
const PaginaRelatoriosTerceirizados = lazy(() =>
  import("./modules/rh/pages/PaginaRelatoriosTerceirizados/index.jsx").then((m) => ({
    default: m.PaginaRelatoriosTerceirizados,
  })),
);
const PaginaRelatoriosAdministrativos = lazy(() =>
  import("./modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx").then((m) => ({
    default: m.PaginaRelatoriosAdministrativos,
  })),
);

const CadastroGenerico = lazy(() =>
  import("./modules/admin/components/CadastroGenerico").then((m) => ({
    default: m.CadastroGenerico,
  })),
);
const PaginaUsuarios = lazy(() =>
  import("./modules/admin/pages/PaginaUsuarios").then((m) => ({
    default: m.PaginaUsuarios,
  })),
);
const PaginaConfiguracoesSistema = lazy(() =>
  import("./modules/admin/pages/PaginaConfiguracoesSistema/index.jsx").then((m) => ({
    default: m.PaginaConfiguracoesSistema,
  })),
);
const PaginaContratos = lazy(() =>
  import("./modules/admin/pages/PaginaContratos").then((m) => ({
    default: m.PaginaContratos,
  })),
);
const PaginaEstruturaOrganizacional = lazy(() =>
  import("./modules/admin/pages/PaginaEstruturaOrganizacional/index.jsx").then((m) => ({
    default: m.PaginaEstruturaOrganizacional,
  })),
);
const PaginaLotacoes = lazy(() =>
  import("./modules/admin/pages/PaginaLotacoes").then((m) => ({
    default: m.PaginaLotacoes,
  })),
);
const PaginaTurnos = lazy(() =>
  import("./modules/admin/pages/PaginaTurnos/index.jsx").then((m) => ({
    default: m.PaginaTurnos,
  })),
);

const PaginaRegistrosAuditoria = lazy(() =>
  import("./modules/admin/pages/PaginaRegistrosAuditoria/index.jsx").then((m) => ({
    default: m.PaginaRegistrosAuditoria,
  })),
);
const PaginaPermissoesExcepcionais = lazy(() =>
  import("./modules/admin/pages/PaginaPermissoesExcepcionais/index.jsx").then((m) => ({ default: m.PaginaPermissoesExcepcionais })),
);
const PaginaCalendarioFolgas = lazy(() =>
  import("./modules/admin/pages/PaginaCalendarioFolgas/index.jsx").then((m) => ({
    default: m.PaginaCalendarioFolgas,
  })),
);
const PaginaEmpresasContratadas = lazy(() =>
  import("./modules/admin/pages/PaginaEmpresasContratadas/index.jsx").then((m) => ({
    default: m.PaginaEmpresasContratadas,
  })),
);

const CarregamentoPagina = () => (
  <div className={estilos.fallbackConteiner}>
    <CarregamentoEmLinha label="Carregando interface..." />
  </div>
);

const LayoutAplicacao = () => {
  const { user, encerrarSessao, atualizarInformacoesSessao } = useAutenticacao();
  usePulsoAtividade(user, atualizarInformacoesSessao);

  const permissoes = useMemo(() => ({
    painel: podeAcessarPainelGerencial(user),
    relatorios: podeAcessarRelatorios(user),
    relatoriosAdministrativos: podeAcessarRelatoriosAdministrativos(user),
    emitirRelatoriosAdministrativos: podeEmitirRelatoriosAdministrativos(user),
    visaoConsolidadaRelatorios: podeAcessarVisaoConsolidadaRelatorios(user),
    frequencia: podeVisualizarFrequencia(user),
    ocorrencias: podeAcessarOcorrencias(user),
    ajustesFrequencia: podeVisualizarFrequencia(user),
    gerenciarAjustesFrequencia: podeGerenciarAjustesFrequencia(user),
    pendencias: podeAcessarPendenciasAdministrativas(user),
    permissoesExcepcionais: podeAcessarPermissoesExcepcionais(user),
    contratos: podeVisualizarContratos(user),
    empresasContratadas: podeGerenciarContratos(user),
    empregados: podeVisualizarEmpregados(user),
    pessoasVinculos: podeVisualizarPessoasVinculos(user),
    historico: podeVisualizarContratos(user) || podeVisualizarPessoasVinculos(user) || podeVisualizarCatalogosReferencia(user),
    usuarios: podeGerenciarUsuarios(user),
    configuracoesSistema: podeGerenciarConfiguracoesSistema(user),
    calendario: podeVisualizarCalendario(user),
    lotacoesTurnos: podeVisualizarLotacoesETurnos(user),
    gerenciarLotacoesTurnos: podeGerenciarLotacoesETurnos(user),
    estruturaOrganizacional: podeVisualizarEstruturaOrganizacional(user),
    gerenciarEstruturaOrganizacional: podeGerenciarEstruturaOrganizacional(user),
    catalogosReferencia: podeVisualizarCatalogosReferencia(user),
    cadastrosNacionais: podeGerenciarCadastrosNacionais(user),
    tiposContratacao: podeVisualizarTiposContratacao(user),
    gerenciarTiposContratacao: podeGerenciarTiposContratacao(user),
    gerenciarCargos: podeGerenciarCargos(user),
    auditoria: podeAcessarAuditoria(user),
    administradorTecnico: eAdministradorTecnico(user),
  }), [user]);

  const navegar = useNavigate();
  const localizacao = useLocation();
  const rotaInicial = useMemo(() => rotaInicialPorPerfil(user), [user]);
  const [menuMobileAberto, setMenuMobileAberto] = useState(false);
  const modoMobile = useConsultaMidia("(max-width: 767px)");
  const [secoesAbertas, setSecoesAbertas] = useState(() => ({
    rh: ["/pessoas", "/empregados", "/frequencia", "/frequencia-ajustes", "/ocorrencias", "/pendencias"].some((rota) =>
      localizacao.pathname.startsWith(rota),
    ),
    relatorios: localizacao.pathname.startsWith("/relatorio"),
    config: [
      "/contratos", "/empresas-contratadas", "/historico", "/estrutura-organizacional", "/calendario-folgas",
      "/cargos", "/lotacoes", "/turnos", "/bancos", "/agencias",
      "/tipos-conta", "/tipos-contratacao", "/status-empregados", "/gestao-usuarios", "/permissoes-excepcionais", "/configuracoes-sistema", "/logs",
    ].some((rota) => localizacao.pathname.startsWith(rota)),
    sub_estrutura: ["/lotacoes", "/turnos"].some((rota) =>
      localizacao.pathname.startsWith(rota),
    ),
    sub_catalogos: ["/cargos", "/tipos-contratacao", "/status-empregados"].some((rota) =>
      localizacao.pathname.startsWith(rota),
    ),
    sub_banco: ["/bancos", "/agencias", "/tipos-conta"].some((rota) =>
      localizacao.pathname.startsWith(rota),
    ),
    sub_sistema: ["/gestao-usuarios", "/permissoes-excepcionais", "/configuracoes-sistema", "/logs"].some((rota) =>
      localizacao.pathname.startsWith(rota),
    ),
  }));

  useEffect(() => {
    if (!modoMobile) setMenuMobileAberto(false);
  }, [modoMobile]);

  useEffect(() => {
    const caminhoAtual = localizacao.pathname;
    setSecoesAbertas((estadoAtual) => ({
      ...estadoAtual,
      rh: estadoAtual.rh || ["/pessoas", "/empregados", "/frequencia", "/frequencia-ajustes", "/ocorrencias", "/pendencias"].some((rota) => caminhoAtual.startsWith(rota)),
      relatorios: estadoAtual.relatorios || caminhoAtual.startsWith("/relatorio"),
      config: estadoAtual.config || [
        "/contratos", "/empresas-contratadas", "/historico", "/estrutura-organizacional", "/calendario-folgas",
        "/cargos", "/lotacoes", "/turnos", "/bancos", "/agencias",
        "/tipos-conta", "/tipos-contratacao", "/status-empregados", "/gestao-usuarios", "/permissoes-excepcionais", "/configuracoes-sistema", "/logs",
      ].some((rota) => caminhoAtual.startsWith(rota)),
      sub_estrutura: estadoAtual.sub_estrutura || ["/cargos", "/lotacoes", "/turnos"].some((rota) => caminhoAtual.startsWith(rota)),
      sub_banco: estadoAtual.sub_banco || ["/bancos", "/agencias", "/tipos-conta"].some((rota) => caminhoAtual.startsWith(rota)),
      sub_sistema: estadoAtual.sub_sistema || ["/gestao-usuarios", "/permissoes-excepcionais", "/logs"].some((rota) => caminhoAtual.startsWith(rota)),
    }));
  }, [localizacao.pathname]);

  const referenciaPrincipal = useRef(null);
  const [mostrarBotaoTopo, setMostrarBotaoTopo] = useState(false);

  useEffect(() => {
    referenciaPrincipal.current?.scrollTo({ top: 0, left: 0, behavior: "auto" });
    setMostrarBotaoTopo(false);
  }, [localizacao.pathname]);

  const alternarSecao = useCallback((secao) => {
    setSecoesAbertas((estadoAnterior) => ({
      ...estadoAnterior,
      [secao]: !estadoAnterior[secao],
    }));
  }, []);

  const lidarComNavegacao = useCallback((caminho) => {
    navegar(caminho);
    setMenuMobileAberto(false);
  }, [navegar]);

  const lidarComRolagem = useCallback((evento) => {
    const topo = evento.target.scrollTop;
    setMostrarBotaoTopo(topo > 300);
  }, []);

  const rolarParaTopo = useCallback(() => {
    referenciaPrincipal.current?.scrollTo({ top: 0, behavior: "smooth" });
  }, []);

  if (!user) return null;

  return (
    <div className={estilos.aplicacaoConteiner}>
      <ConteinerAvisos />
      <HomologacaoModal />

      <BarraLateral
        mobileMenu={menuMobileAberto}
        modoMobile={modoMobile}
        onCloseMenu={() => setMenuMobileAberto(false)}
        usuario={user}
        permissoes={permissoes}
        onNavigate={lidarComNavegacao}
        sectionsOpen={secoesAbertas}
        toggleSection={alternarSecao}
      />

      <div className={estilos.envoltorioPrincipal}>
        <Cabecalho
          user={user}
          aoSair={encerrarSessao}
          onNavigate={lidarComNavegacao}
          setMobileMenu={setMenuMobileAberto}
          menuMobileAberto={menuMobileAberto}
        />

        <main ref={referenciaPrincipal} onScroll={lidarComRolagem} className={estilos.conteudoPrincipal}>
          <div className={estilos.conteinerPagina}>
            <Suspense fallback={<CarregamentoPagina />}>
              <Routes>
                <Route path="/" element={<Navigate to={rotaInicial} replace />} />
                <Route path="/perfil" element={<PaginaPerfil />} />

                <Route path="/painel" element={permissoes.painel ? <Painel onNavigate={lidarComNavegacao} /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/frequencia" element={permissoes.frequencia ? <PaginaFrequencia /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/frequencia-ajustes" element={permissoes.ajustesFrequencia ? <PaginaAjustesFrequencia /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/ocorrencias" element={permissoes.ocorrencias ? <PaginaOcorrencias /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/pendencias" element={permissoes.pendencias ? <PaginaPendenciasAdministrativas /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/contratos" element={permissoes.contratos ? <PaginaContratos /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/historico" element={permissoes.historico ? <PaginaHistorico /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/empresas-contratadas" element={permissoes.empresasContratadas ? <PaginaEmpresasContratadas /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/pessoas" element={permissoes.pessoasVinculos ? <PaginaPessoasVinculos /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/empregados" element={permissoes.pessoasVinculos ? <Navigate to="/pessoas" replace /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/relatorios" element={permissoes.visaoConsolidadaRelatorios ? <PaginaRelatoriosTerceirizados /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/relatorio-mensal" element={permissoes.relatorios ? <PaginaRelatorioFrequencia /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/relatorio-absenteismo" element={permissoes.relatorios ? <PaginaRelatorioAbsenteismo /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/relatorios-administrativos" element={permissoes.relatoriosAdministrativos ? <PaginaRelatoriosAdministrativos podeEmitir={permissoes.emitirRelatoriosAdministrativos} /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/gestao-usuarios" element={permissoes.usuarios ? <PaginaUsuarios /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/configuracoes-sistema" element={permissoes.configuracoesSistema ? <PaginaConfiguracoesSistema /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/calendario-folgas" element={permissoes.calendario ? <PaginaCalendarioFolgas /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/lotacoes" element={permissoes.lotacoesTurnos ? <PaginaLotacoes podeEscrever={permissoes.gerenciarLotacoesTurnos} /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/turnos" element={permissoes.lotacoesTurnos ? <PaginaTurnos podeEscrever={permissoes.gerenciarLotacoesTurnos} /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/estrutura-organizacional" element={permissoes.estruturaOrganizacional ? <PaginaEstruturaOrganizacional /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/logs" element={permissoes.auditoria ? <PaginaRegistrosAuditoria /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/permissoes-excepcionais" element={permissoes.permissoesExcepcionais ? <PaginaPermissoesExcepcionais /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/cargos" element={permissoes.catalogosReferencia ? <CadastroGenerico title="Cargos" descricao="Catálogo da Gestão Local, administrado pelo Gestor Local e preservado para consulta histórica quando inativado." serviceEndpoint="/cargos" podeEscrever={permissoes.gerenciarCargos} paginado suportaAtivo exclusaoEhInativacao columns={[{ label: "Nome", key: "nome" }, { label: "CBO", key: "cbo" }, { label: "Gestão Local", key: "gestaoLocalNome" }, { label: "UF", key: "uf" }, { label: "Situação", key: "ativo", type: "boolean" }]} fields={[{ label: "Nome", key: "nome", required: true }, { label: "CBO", key: "cbo" }, { label: "Descrição", key: "descricao" }, { label: "Ativo", key: "ativo", type: "checkbox", defaultValue: true }]} /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/status-empregados" element={permissoes.catalogosReferencia ? <CadastroGenerico title="Status de empregado" descricao="Catálogo nacional de situações do empregado. Status estruturais protegidos pelo Java não podem ser renomeados ou excluídos." serviceEndpoint="/status-empregados" podeEscrever={permissoes.cadastrosNacionais} columns={[{ label: "Descrição", key: "descricao" }]} fields={[{ label: "Descrição", key: "descricao", required: true }]} /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="/tipos-contratacao" element={permissoes.tiposContratacao ? <CadastroGenerico title="Tipos de contratação" descricao="Catálogo prévio da Gestão Local usado nos vínculos; não é texto livre." serviceEndpoint="/tipos-contratacao" podeEscrever={permissoes.gerenciarTiposContratacao} paginado={false} suportaAtivo permiteExcluir={false} columns={[{ label: "Descrição", key: "descricao" }, { label: "Gestão Local", key: "gestaoLocalNome" }, { label: "UF", key: "uf" }, { label: "Matrícula de vínculo", key: "matriculaVinculoObrigatoria", type: "boolean", trueLabel: "SIM", falseLabel: "NÃO" }, { label: "Situação", key: "ativo", type: "boolean" }]} fields={[{ label: "Descrição", key: "descricao", required: true }, { label: "Exige matrícula de vínculo", key: "matriculaVinculoObrigatoria", type: "checkbox", defaultValue: false }, { label: "Ativo", key: "ativo", type: "checkbox", defaultValue: true }]} /> : <AcessoNegado rotaRetorno={rotaInicial} />} />
                <Route path="*" element={<Navigate to={rotaInicial} replace />} />
              </Routes>
            </Suspense>
          </div>
          <Rodape />
        </main>

        <button
          onClick={rolarParaTopo}
          className={`${estilos.botaoRolarTopo} ${mostrarBotaoTopo ? estilos.botaoRolarTopoVisivel : estilos.botaoRolarTopoOculto}`}
          type="button"
          aria-label="Voltar ao topo"
        >
          <ArrowUp size={24} strokeWidth={3} />
        </button>

        <LembreteSegundoFator user={user} onNavigate={lidarComNavegacao} />

        {menuMobileAberto && (
          <button
            type="button"
            className={estilos.sobreposicaoMenuMobile}
            onClick={() => setMenuMobileAberto(false)}
            aria-label="Fechar menu de navegação"
          />
        )}
      </div>
    </div>
  );
};

const RoteadorPrincipal = () => {
  const { user, loading, loggingOut, geracaoSessao } = useAutenticacao();
  if (loading && !loggingOut) return <CarregamentoPagina />;

  if (loggingOut || user?.forceChangePassword) {
    return (
      <Suspense fallback={<CarregamentoPagina />}>
        <TelaLogin />
      </Suspense>
    );
  }
  return user ? (
    <ProvedorExportacaoRelatorio key={geracaoSessao}>
      <LayoutAplicacao />
    </ProvedorExportacaoRelatorio>
  ) : (
    <Suspense fallback={<CarregamentoPagina />}>
      <TelaLogin />
    </Suspense>
  );
};

const EstruturaAplicacao = () => {
  const { loading, loggingOut, concluirSaida } = useAutenticacao();
  const chaveArmazenamentoIntro = `${CONFIG.APP_NAME.toLowerCase()}_intro_shown`;

  const [introducaoExibida, setIntroducaoExibida] = useState(() => {
    return sessionStorage.getItem(chaveArmazenamentoIntro) === "true";
  });

  const exibirIntroducao = !introducaoExibida;
  const exibirSaida = introducaoExibida && loggingOut;
  const deveExibirSobreposicao = exibirIntroducao || exibirSaida;
  const modoAnimacao = exibirSaida ? "logout" : "intro";

  const lidarComConclusaoEfeito = useCallback(() => {
    if (modoAnimacao === "intro") {
      setIntroducaoExibida(true);
      sessionStorage.setItem(chaveArmazenamentoIntro, "true");
    } else if (modoAnimacao === "logout") {
      sessionStorage.removeItem(chaveArmazenamentoIntro);
      concluirSaida();
    }
  }, [modoAnimacao, concluirSaida, chaveArmazenamentoIntro]);

  return (
    <>
      <ModalConexao />
      {deveExibirSobreposicao && (
        <Suspense fallback={<CarregamentoPagina />}>
          <AtualizacaoModulo
            key={modoAnimacao}
            mode={modoAnimacao}
            ready={modoAnimacao === "intro" ? !loading : true}
            onDone={lidarComConclusaoEfeito}
          />
        </Suspense>
      )}
      <RoteadorPrincipal />
    </>
  );
};

const Aplicacao = () => {
  const [configuracaoEfetiva, definirConfiguracaoEfetiva] = useState(null);

  useEffect(() => {
    let ativo = true;
    const urlBaseBackend = CONFIG.API_URL.replace(/\/api\/?$/, "");

    ServicoSistema.carregarRuntimeConfig(urlBaseBackend)
      .then((configuracaoServidor) => {
        if (!ativo) return;
        definirConfiguracaoEfetiva(
          combinarConfiguracaoEfetiva(CONFIG, configuracaoServidor),
        );
      })
      .catch(() => {
        if (!ativo) return;
        // Fallback visual para indisponibilidade do endpoint público.
        // Regras e autorizações continuam pertencendo ao backend.
        definirConfiguracaoEfetiva(CONFIG);
      });

    return () => {
      ativo = false;
    };
  }, []);

  if (!configuracaoEfetiva) return <CarregamentoPagina />;

  if (configuracaoEfetiva.EM_MANUTENCAO) {
    return (
      <PaginaManutencao
        previsaoRetorno={configuracaoEfetiva.PREVISAO_RETORNO}
      />
    );
  }

  return (
    <FronteiraErro>
      <ProvedorConfirmacaoSenhaHttp>
        <ProvedorAutenticacao>
          <EstruturaAplicacao />
        </ProvedorAutenticacao>
      </ProvedorConfirmacaoSenhaHttp>
    </FronteiraErro>
  );
};

export default Aplicacao;
