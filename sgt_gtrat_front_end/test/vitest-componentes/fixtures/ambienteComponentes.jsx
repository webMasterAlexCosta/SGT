import React from "react";
import { vi } from "vitest";

export const UsuarioAdministradorTeste = {
  id: 1,
  username: "Administrador Teste",
  nome: "Administrador Teste",
  matricula: "12345678",
  email: "admin.teste@exemplo.invalid",
  perfil: "ADMINISTRADOR",
  autoridade: "PERFIL_ADMINISTRADOR",
  ativo: true,
  twoFactorEnabled: false,
  twoFactorVerified: true,
  forceChangePassword: false,
  lotacaoId: 1,
  lotacaoNome: "CTE Teste",
  lotacao: "CTE Teste",
  jornadaId: 1,
  turno: "Turno Teste",
  uf: "RJ",
  vinculosGestao: [],
};

export const PaginaVaziaTeste = {
  content: [],
  dados: [],
  items: [],
  totalElements: 0,
  totalPaginas: 0,
  totalPages: 0,
  number: 0,
  pagina: 0,
  size: 100,
  tamanho: 100,
  first: true,
  primeira: true,
  last: true,
  ultima: true,
};

export const RespostaVaziaTeste = Object.assign([], {
  ...PaginaVaziaTeste,
  authenticated: false,
  user: null,
  session: { remainingSeconds: 1800 },
  contratos: [],
  lotacoes: [],
  turnos: [],
  jornadas: [],
  superintendencias: [],
  gestoesLocais: [],
  usuarios: [],
  empregados: [],
  eventos: [],
  indicadores: {},
  resumo: {},
});

export const IconeTeste = ({ size = 16, ...props }) => (
  <svg aria-hidden="true" width={size} height={size} {...props} />
);

const callbacksTexto = new Set([
  "obterNomeBanco",
  "obterPerfil",
  "descreverAbrangencia",
  "formatarData",
  "obterRotuloEscopo",
  "funcaoRotuloTurno",
]);

const callbacksBooleanos = new Set([
  "podeGerenciarUsuario",
  "validarHabilitacao",
]);

const propsArray = new Set([
  "admissoesMensais",
  "agencias",
  "alertas",
  "atividades",
  "bancos",
  "columns",
  "contratos",
  "contratosFavoritos",
  "contratosSelecionados",
  "diasDoMes",
  "distribuicaoContratos",
  "distribuicaoStatus",
  "empregados",
  "eventos",
  "evolucao",
  "fields",
  "filtrosDisponiveis",
  "gruposBase",
  "gestoesLocais",
  "historico",
  "itens",
  "jornadasDisponiveis",
  "listaDeContratos",
  "listaDeFrequencia",
  "listaDeGestoesLocais",
  "listaDeLotacoesDisponiveis",
  "listaDeTurnosDisponiveis",
  "listaDeTurnosVisiveis",
  "lotacoes",
  "lotacoesDisponiveis",
  "municipiosDisponiveis",
  "notas",
  "options",
  "passos",
  "perfisPermitidos",
  "registros",
  "statusDisponiveis",
  "superintendencias",
  "tarefas",
  "turnos",
  "turnosDisponiveis",
  "turnosProcessadosNaLista",
  "ufsDisponiveis",
  "unidades",
  "usuarios",
]);

const propsBooleanas = new Set([
  "aberto",
  "acessoGlobal",
  "administrador",
  "ativo",
  "bloqueado",
  "carregando",
  "carregandoOsDados",
  "carregandoSuperintendencias",
  "closeOnBackdrop",
  "closeOnEsc",
  "configuracaoPendente",
  "desabilitado",
  "disabled",
  "eAdministrador",
  "eAdministradorGlobal",
  "eGestorGestaoLocal",
  "eGestorOperacional",
  "eSubItem",
  "edicaoBloqueada",
  "editando",
  "ehNovoRegistro",
  "escopoBloqueado",
  "estaAberto",
  "estaCarregando",
  "estaDesabilitado",
  "estaDesbloqueadoParaEdicao",
  "estaExpandido",
  "estaMarcado",
  "estaSalvando",
  "exibindoTodos",
  "exibirFiltroEstado",
  "existemDados",
  "exportandoCompleto",
  "filtragemBancariaExibida",
  "filtrosAvancadosAbertos",
  "fullWidth",
  "isOpen",
  "minimizado",
  "mobileMenu",
  "ocultarCabecalho",
  "permitirTelaCheia",
  "podeAbrir",
  "podeAbrirLogs",
  "podeGerenciar",
  "podeOperar",
  "possuiGestorEstadual",
  "presidente",
  "primeira",
  "readOnly",
  "ready",
  "required",
  "salvando",
  "segundoFatorPendente",
  "showCloseButton",
  "somenteConfirmacao",
  "tokenValido",
  "ultima",
  "validandoToken",
]);

const propsNumericas = new Set([
  "cargaCpu",
  "duracaoUltimaBuscaMs",
  "id",
  "idEdicaoTurno",
  "latencia",
  "memoria",
  "minDurationMs",
  "pagina",
  "paginaAtual",
  "periodoDias",
  "progresso",
  "progressoCargaTotal",
  "quantidadeAtivos",
  "quantidadeContratos",
  "quantidadeContratosSelecionados",
  "quantidadeExibida",
  "quantidadeFavoritos",
  "quantidadeGrupos",
  "quantidadePagina",
  "quantidadeRegistros",
  "size",
  "tamanho",
  "tamanhoMinimoSenha",
  "tamanhoPagina",
  "tamanhoSelecionado",
  "totalColaboradores",
  "totalDeResultados",
  "totalElementos",
  "totalEncontrado",
  "totalExibido",
  "totalFiltrado",
  "totalGeral",
  "totalPaginas",
  "totalResultados",
  "totalUsuarios",
]);

const propsDatas = new Set([
  "dataInicio",
  "dataFim",
  "dataReferencia",
  "geradoEm",
  "hoje",
  "referencia",
]);

const criarMapa = () => new Map();

const criarUsuario = () => ({ ...UsuarioAdministradorTeste });

const criarContrato = () => ({
  id: 1,
  numero: "001/2026",
  numeroContrato: "001/2026",
  objeto: "Contrato de teste",
  nome: "Contrato de teste",
  descricao: "Contrato de teste",
  ativo: true,
  situacao: "ATIVO",
  status: "ATIVO",
  uf: "RJ",
  dataInicio: "2026-01-01",
  dataFim: "2026-12-31",
  lotacoes: [],
  turnos: [],
});

const criarLotacao = () => ({
  id: 1,
  nome: "CTE Teste",
  label: "CTE Teste",
  sigla: "CTE",
  ativo: true,
  uf: "RJ",
  turnos: [],
  jornadas: [],
});

const criarTurno = () => ({
  id: 1,
  nome: "Turno Teste",
  descricao: "Turno Teste",
  label: "Turno Teste",
  ativo: true,
  regras: [],
  diasSemana: [],
  configuracoes: [],
});

const criarEmpregado = () => ({
  id: 1,
  nome: "Empregado Teste",
  nomeCompleto: "Empregado Teste",
  cpf: "00000000000",
  matricula: "12345678",
  ativo: true,
  status: "ATIVO",
  cargo: "Auxiliar",
  lotacao: "CTE Teste",
  lotacaoId: 1,
  jornadaId: 1,
  turno: "Turno Teste",
  contratos: [],
  telefones: [],
});

const criarRegistroAuditoria = () => ({
  id: 1,
  acao: "ATUALIZACAO",
  entidade: "Teste",
  entidadeId: "1",
  usuario: "Administrador Teste",
  dataHora: "2026-08-21T12:00:00",
  antes: {},
  depois: {},
});

const criarDadosFormulario = () => ({
  id: null,
  nome: "Registro Teste",
  descricao: "Registro Teste",
  ativo: true,
  uf: "RJ",
  email: "teste@exemplo.invalid",
  matricula: "12345678",
  cpf: "00000000000",
  senha: "SenhaTeste123!",
  novaSenha: "SenhaTeste123!",
  perfil: "ADMINISTRADOR",
  autoridade: "PERFIL_ADMINISTRADOR",
  gestaoLocalId: "",
  superintendenciaId: "",
  lotacaoId: "",
  jornadaId: "",
  contratoId: "",
  contratos: [],
  turnos: [],
  jornadas: [],
  vinculosGestao: [],
  telefones: [],
  regras: [],
});

const criarPaginacao = () => ({ ...PaginaVaziaTeste });

const criarObjetoMetricas = () => ({
  total: 0,
  ativos: 0,
  inativos: 0,
  taxa: 0,
  percentual: 0,
  valor: 0,
  quantidade: 0,
  dados: [],
  itens: [],
  series: [],
  labels: [],
});

const criarObjetoPadrao = (nomeProp) => {
  if (["usuario", "user", "gestorAtual", "usuarioLogado"].includes(nomeProp)) return criarUsuario();
  if (["contrato"].includes(nomeProp)) return criarContrato();
  if (["contratoSelecionado"].includes(nomeProp)) return "";
  if (["lotacao", "lotacaoSelecionada"].includes(nomeProp)) return criarLotacao();
  if (["turnoData"].includes(nomeProp)) return criarTurno();
  if (["empregado"].includes(nomeProp)) return criarEmpregado();
  if (["registro"].includes(nomeProp)) return criarRegistroAuditoria();
  if (["paginacao"].includes(nomeProp)) return criarPaginacao();
  if (["turnosPorId", "gestoesLocaisPorSuperintendencia"].includes(nomeProp)) return criarMapa();
  if (["cpfsRevelados"].includes(nomeProp)) return {};
  if (["permissoes"].includes(nomeProp)) {
    return {
      painel: true,
      relatorios: true,
      visaoConsolidadaRelatorios: true,
      frequencia: true,
      ocorrencias: true,
      contratos: true,
      governanca: true,
      empregados: true,
      usuarios: true,
      calendario: true,
      lotacoesTurnos: true,
      estruturaOrganizacional: true,
      auditoria: true,
      cadastrosNacionais: true,
      administradorTecnico: true,
    };
  }
  if (["sectionsOpen"].includes(nomeProp)) {
    return { rh: true, relatorios: true, config: true, sub_estrutura: true, sub_banco: true, sub_sistema: true };
  }
  if (["dadosDoFormulario", "dadosFormulario", "dados", "dadosModalParcial", "formGestaoLocal", "formSuperintendencia", "credenciais"].includes(nomeProp)) {
    return criarDadosFormulario();
  }
  if (["indicadores", "estatisticas", "analise", "comparativo", "frequencia", "horizonte", "integridade", "periodo", "pressao", "quadro", "resumoExecutivo", "dadosFinanceirosContrato", "statusDaData", "estadoTela", "analiseDoFormulario"].includes(nomeProp)) {
    return criarObjetoMetricas();
  }
  if (["filtros", "contagens", "contagensDeFiltro", "contagensPorEstado", "filtrosGrupoAtivos"].includes(nomeProp)) return {};
  if (["grupo"].includes(nomeProp)) return { id: "grupo-teste", nome: "Grupo teste", empregados: [], colaboradores: [], itens: [] };
  if (["modal"].includes(nomeProp)) return null;
  if (["modais", "modaisAcoes", "acoes"].includes(nomeProp)) return {};
  if (["credencial"].includes(nomeProp)) return { matricula: "12345678", senhaTemporaria: "SenhaTeste123!", nome: "Usuário Teste" };
  if (["contextoAcesso"].includes(nomeProp)) return { titulo: "Acesso nacional", descricao: "Teste", nacional: true };
  if (["fechamentoFrequencia"].includes(nomeProp)) return { status: "CONCLUIDO", percentual: 100, processados: 0, total: 0 };
  if (["bloqueioInatividadeStatus"].includes(nomeProp)) return { bloqueado: false };
  if (["escoposDoUsuario"].includes(nomeProp)) return [];
  return {};
};

const criarCallback = (nomeProp) => {
  if (callbacksTexto.has(nomeProp)) return vi.fn(() => "Teste");
  if (callbacksBooleanos.has(nomeProp)) return vi.fn(() => true);
  if (nomeProp === "funcaoObterJanelaDeEdicao") return vi.fn(() => ({ permitido: true, bloqueado: false }));
  if (nomeProp === "carregarPagina") return vi.fn(async () => ({ ...PaginaVaziaTeste }));
  if (nomeProp === "carregarColaboradores") return vi.fn(async () => []);
  return vi.fn();
};

export const criarValorProp = (nomeProp, hint) => {
  if (nomeProp === "children") return <span>Conteúdo de teste</span>;
  if (["icone", "Icone", "iconeDecorativo"].includes(nomeProp)) return IconeTeste;
  if (nomeProp === "logotipoCorreios") return "/logo-teste.png";

  if (
    hint === "function" ||
    /^(ao|on|set|toggle|alternar|abrir|carregar|validar|funcao|obter|formatar|descrever)/.test(nomeProp) ||
    nomeProp === "podeGerenciarUsuario"
  ) return criarCallback(nomeProp);

  // Quando o próprio componente declara um tipo por valor padrão ou uso direto,
  // essa informação tem prioridade sobre heurísticas baseadas apenas no nome da prop.
  if (hint === "array") return [];
  if (hint === "object") return criarObjetoPadrao(nomeProp);
  if (hint === "boolean") return false;
  if (hint === "number") return 0;
  if (hint === "string") return "Teste";
  if (hint === "nullable") return null;

  if (propsArray.has(nomeProp)) return [];
  if (propsBooleanas.has(nomeProp) || /^pode[A-Z]/.test(nomeProp)) return false;
  if (propsNumericas.has(nomeProp) || /^(total|quantidade)/.test(nomeProp)) return 0;
  if (propsDatas.has(nomeProp)) return "2026-08-21";

  if (["id", "idEdicaoTurno"].includes(nomeProp)) return 1;
  if (["pagina", "paginaAtual"].includes(nomeProp)) return 0;
  if (nomeProp === "tamanho") return 100;

  if (
    /^(dados|estado|filtro|form|config|contexto|periodo|quadro|analise|comparativo|integridade|pressao|horizonte|resumo)/.test(nomeProp)
  ) return criarObjetoPadrao(nomeProp);

  if (nomeProp === "statusSelecionado" || nomeProp === "filtroStatus") return "";
  if (nomeProp === "modoVisualizacao" || nomeProp === "modo") return "tabela";
  if (nomeProp === "mode") return "intro";
  if (nomeProp === "variant") return "default";
  if (nomeProp === "type") return "text";
  if (nomeProp === "nivel") return "NACIONAL";
  if (nomeProp === "perfil") return "ADMINISTRADOR";
  if (nomeProp === "statusSistema") return "ONLINE";
  if (nomeProp === "tokenAutenticacao") return "[TOKEN TESTE]";
  if (nomeProp === "ipCliente") return "127.0.0.1";
  if (nomeProp === "nomeNavegador") return "Navegador Teste";
  if (nomeProp === "usuarioSo") return "TESTE\\USUARIO";
  if (nomeProp === "qrCodeDataUrl") return "data:image/png;base64,";
  if (nomeProp === "otpauthUri") return "otpauth://totp/teste";
  if (nomeProp === "secret") return "TESTESECRET";
  if (nomeProp === "expiraEmEpochMs") return Date.now() + 60 * 60 * 1000;

  return "Teste";
};

export const criarPropsComponente = (entrada) => {
  const props = {};
  for (const nomeProp of entrada.props || []) {
    props[nomeProp] = criarValorProp(nomeProp, entrada.hints?.[nomeProp]);
  }

  // Ajustes por componente para evitar falsos positivos de fixture.
  if (entrada.localName === "CabecalhoGovernanca" || entrada.localName === "ResumoExecutivo") {
    props.dados = {
      referencia: "2026-08-21",
      escopo: { nome: "Escopo de teste" },
      periodo: { inicio: "2026-08-01", fim: "2026-08-21", dias: 21 },
      efetivo: {},
      quadro: {},
      analiseCadastro: {},
      contratos: {},
      frequencia: {},
      integridade: {},
    };
  }
  if (entrada.localName === "GrupoEmpregados") {
    props.acoes = {};
    props.modaisAcoes = {};
  }
  if (entrada.localName === "TabelaFrequencia") {
    props.listaDeFrequencia = [];
    props.statusDaData = { podeEditar: true, bloqueado: false };
    props.eGestorOperacional = true;
  }
  if (entrada.localName === "TabelaRelatorio") {
    props.dados = [];
    props.diasDoMes = [];
    props.totalColaboradores = 0;
  }
  if (entrada.localName === "ListaContratos") props.contratos = [];
  if (entrada.localName === "TabelaUsuarios") props.usuarios = [];
  if (entrada.localName === "TabelaAuditoria") props.dados = { ...PaginaVaziaTeste };
  if (entrada.localName === "IndicadoresCalendario") props.indicadores = { total: 0, nacionais: 0, estaduais: 0, locais: 0 };
  if (entrada.localName === "PainelEventos") props.eventos = [];
  if (entrada.localName === "GradeGestoresEstaduais") props.gestoesLocaisPorSuperintendencia = new Map();
  if (entrada.localName === "CartaoLotacao") props.turnosPorId = new Map();
  if (entrada.localName === "ModaisEstrutura") props.modal = null;
  if (entrada.localName === "ModalAgencia") {
    props.estado = { aberto: false, editando: null };
    props.dados = criarDadosFormulario();
  }
  if (entrada.localName === "ModalEvento") props.aberto = false;
  if (entrada.localName === "ModalAlocacoesVinculo") props.aberto = false;
  if (["ModalContrato", "ModalAcaoContrato", "ModalDetalhesContrato"].includes(entrada.localName)) props.aberto = false;
  if (entrada.localName === "ModalLotacao") props.aberto = false;
  if (entrada.localName === "ModalDeTurno") props.aberto = false;
  if (entrada.localName === "ModalUsuario") props.aberto = false;
  if (entrada.localName === "ModalRedefinirSenha") props.aberto = false;
  if (entrada.localName === "ModalConfirmacao" || entrada.localName === "ModalConfirmacaoSenha") {
    props.isOpen = false;
    props.aberto = false;
  }
  if (entrada.localName === "JanelaModal") props.isOpen = false;
  if (entrada.localName === "ModalCreditosColaboracao") props.isOpen = false;
  if (entrada.localName === "ModalHistoricoAuditoria" || entrada.localName === "ModalHistoricoContrato") props.isOpen = false;
  if (entrada.localName === "ModalSegurancaSegundoFator") props.isOpen = false;
  if (entrada.localName === "TourGuiado") props.aberto = false;
  if (entrada.localName === "PainelExportacoes") props.tarefas = [];
  if (entrada.localName === "CatalogoRelatorios") props.aoNavegar = vi.fn();
  if (entrada.localName === "CadastroGenerico") {
    props.columns = [];
    props.fields = [];
    props.serviceEndpoint = "/teste";
  }
  if (entrada.localName === "Seletor") props.options = [];
  if (entrada.localName === "FronteiraErro") {
    return { children: <span>Conteúdo protegido</span> };
  }
  if (entrada.localName === "Aplicacao") return {};

  return props;
};
