import React from "react";
import { vi } from "vitest";
import {
  PaginaVaziaTeste,
  RespostaVaziaTeste,
  UsuarioAdministradorTeste,
  criarPropsComponente,
} from "../../vitest-componentes/fixtures/ambienteComponentes.jsx";

export { PaginaVaziaTeste, RespostaVaziaTeste, UsuarioAdministradorTeste };

export const criarContratoTeste = () => ({
  id: 1,
  numero: "001/2026",
  numeroContrato: "001/2026",
  descricao: "Contrato de teste",
  objeto: "Prestação de serviços de teste",
  ativo: true,
  active: true,
  status: "ATIVO",
  situacao: "ATIVO",
  uf: "RJ",
  dataInicio: "2026-01-01",
  dataFim: "2026-12-31",
  lotacoes: [],
  turnos: [],
});

export const criarGestaoLocalTeste = () => ({
  id: 1,
  nome: "GTRAT RJ",
  descricao: "GTRAT RJ",
  sigla: "GTRAT-RJ",
  uf: "RJ",
  ativo: true,
});

export const criarSuperintendenciaTeste = () => ({
  id: 1,
  nome: "Superintendência RJ",
  descricao: "Superintendência RJ",
  sigla: "SE-RJ",
  uf: "RJ",
  ativo: true,
  gestoesLocais: [criarGestaoLocalTeste()],
});

export const criarTurnoTeste = () => ({
  id: 1,
  nome: "Turno 1A",
  descricao: "Turno 1A",
  label: "Turno 1A",
  ativo: true,
  horaInicio: "08:00",
  horaFim: "17:00",
  intervaloInicio: "12:00",
  intervaloFim: "13:00",
  cargaHoraria: "08:00",
  diasSemana: ["SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA"],
  configuracoes: [],
  regras: [],
});

export const criarLotacaoTeste = () => ({
  id: 1,
  nome: "CTE Teste",
  descricao: "CTE Teste",
  sigla: "CTE",
  uf: "RJ",
  ativo: true,
  contratoId: 1,
  turnos: [criarTurnoTeste()],
  jornadas: [criarTurnoTeste()],
});

export const criarEmpregadoTeste = (id = 1) => ({
  id,
  empregadoId: id,
  nome: `Empregado Teste ${id}`,
  nomeCompleto: `Empregado Teste ${id}`,
  cpf: "00000000000",
  matricula: String(12345000 + id),
  email: `empregado${id}@exemplo.invalid`,
  ativo: true,
  active: true,
  status: "ATIVO",
  statusDescricao: "ATIVO",
  cargo: "Auxiliar Operacional",
  cargoNome: "Auxiliar Operacional",
  lotacao: "CTE Teste",
  lotacaoNome: "CTE Teste",
  lotacaoId: 1,
  jornadaId: 1,
  jornadaDescricao: "Turno 1A",
  turno: "Turno 1A",
  contratos: [criarContratoTeste()],
  telefones: [],
  ocorrencia: null,
  jaSalvo: false,
  obs: "",
  horarioTrabalhoDoDia: "08:00 - 17:00",
  diario: {
    "2026-08-01": { sigla: "P", status: "PRESENTE" },
  },
  totais: {
    presencas: 1,
    parciais: 0,
    faltas: 0,
    atestados: 0,
    descansos: 0,
    rt: 0,
    horasFisicas: "08:00",
    horasNoturnas: "00:00",
    horasPagaveis: "08:00",
    totalPagavelDecimal: 8,
  },
});

export const criarUsuarioTeste = (id = 1) => ({
  ...UsuarioAdministradorTeste,
  id,
  username: `Administrador Teste ${id}`,
  nome: `Administrador Teste ${id}`,
  email: `admin${id}@exemplo.invalid`,
  active: true,
  ativo: true,
  locked: false,
  forceChangePassword: false,
  retroactiveUnlocked: false,
});

export const criarEventoTeste = () => ({
  id: 1,
  titulo: "Feriado de teste",
  descricao: "Feriado de teste",
  data: "2026-08-21",
  dataInicio: "2026-08-21",
  dataFim: "2026-08-21",
  tipo: "FERIADO",
  abrangencia: "NACIONAL",
  ativo: true,
});

export const criarRegistroAuditoriaTeste = () => ({
  id: 1,
  acao: "ATUALIZACAO",
  entidade: "Empregado",
  entidadeId: "1",
  usuario: "Administrador Teste",
  dataHora: "2026-08-21T12:00:00",
  antes: { nome: "Antes" },
  depois: { nome: "Depois" },
});

const itemParaArray = (nome) => {
  const chaveOriginal = String(nome || "");
  const chave = chaveOriginal.toLowerCase();

  if (["headers"].includes(chaveOriginal)) return "Coluna de teste";
  if (["historico"].includes(chaveOriginal)) return "Evento de teste";
  if (["statusDisponiveis"].includes(chaveOriginal)) return "ATIVO";
  if (["ufsDisponiveis"].includes(chaveOriginal)) return "RJ";
  if (["municipiosDisponiveis"].includes(chaveOriginal)) return "Rio de Janeiro";
  if (["perfisPermitidos"].includes(chaveOriginal)) return "ADMINISTRADOR";
  if (["contratosSelecionados", "contratosFavoritos"].includes(chaveOriginal)) return "1";
  if (["notas"].includes(chaveOriginal)) return "Nota metodológica de teste";
  if (["diasDoMes"].includes(chaveOriginal)) return "2026-08-01";
  if (["passos"].includes(chaveOriginal)) return { id: "teste", target: "body", selector: "body", title: "Passo de teste", text: "Orientação de teste" };
  if (["filtrosDisponiveis"].includes(chaveOriginal)) return { id: "1", nome: "Contrato Teste", label: "Contrato Teste", quantidadeColaboradores: 1, favorito: false };
  if (["distribuicaoStatus"].includes(chaveOriginal)) return { nome: "ATIVO", quantidade: 1 };
  if (["distribuicaoContratos"].includes(chaveOriginal)) return { id: 1, nome: "001/2026", quantidade: 1 };
  if (["evolucao"].includes(chaveOriginal)) return { data: "2026-08-21", presencas: 1, faltas: 0, atestados: 0, quantidade: 1 };
  if (["admissoesMensais"].includes(chaveOriginal)) return { mes: "2026-08", quantidade: 1 };
  if (["atividades"].includes(chaveOriginal)) return { id: 1, acao: "ATUALIZACAO", entidade: "Empregado", usuario: "Administrador", dataHora: "2026-08-21T12:00:00" };
  if (["alertas"].includes(chaveOriginal)) return { codigo: "TESTE", titulo: "Alerta teste", mensagem: "Mensagem teste", severidade: "ATENCAO", rota: "painel" };
  if (["columns"].includes(chaveOriginal)) return { key: "nome", label: "Nome" };
  if (["fields"].includes(chaveOriginal)) return { key: "nome", label: "Nome", type: "text", required: false };
  if (["gruposBase"].includes(chaveOriginal)) { const empregado = criarEmpregadoTeste(); return { id: "1", nome: "Grupo Teste", empregados: [empregado], colaboradores: [empregado], itens: [empregado], stats: { total: 1, ativos: 1, inativos: 0 } }; }
  if (chave.includes("contrato")) return criarContratoTeste();
  if (chave.includes("lotacao") || chave.includes("lotação") || chave.includes("unidade")) return criarLotacaoTeste();
  if (chave.includes("turno") || chave.includes("jornada")) return criarTurnoTeste();
  if (chave.includes("empregado") || chave.includes("colaborador") || chave.includes("frequencia")) return criarEmpregadoTeste();
  if (chave.includes("usuario")) return criarUsuarioTeste();
  if (chave.includes("evento")) return criarEventoTeste();
  if (chave.includes("auditoria") || chave.includes("historico") || chave.includes("registro")) return criarRegistroAuditoriaTeste();
  if (chave.includes("gestaoLocal")) return criarGestaoLocalTeste();
  if (chave.includes("superintend")) return criarSuperintendenciaTeste();
  if (chave.includes("banco")) return { id: 1, codigo: "001", nome: "Banco Teste", descricao: "Banco Teste", ativo: true };
  if (chave.includes("agencia")) return { id: 1, codigo: "0001", numero: "0001", nome: "Agência Teste", bancoId: 1, ativo: true };
  if (chave.includes("status")) return { value: "ATIVO", valor: "ATIVO", label: "Ativo", descricao: "Ativo" };
  if (chave.includes("uf")) return "RJ";
  if (chave.includes("option")) return { value: "1", label: "Opção teste" };
  if (chave.includes("dia")) return "2026-08-01";
  if (chave.includes("alerta")) return { id: 1, titulo: "Alerta teste", mensagem: "Alerta teste", nivel: "INFO" };
  if (chave.includes("nota")) return { id: 1, titulo: "Nota teste", texto: "Nota teste" };
  return { id: 1, nome: "Item de teste", descricao: "Item de teste", ativo: true };
};

const popularObjeto = (nome, valor) => {
  const chave = String(nome || "").toLowerCase();
  if (chave === "grupo") {
    const empregado = criarEmpregadoTeste();
    return { id: "grupo-teste", nome: "Grupo teste", empregados: [empregado], colaboradores: [empregado], itens: [empregado], stats: { total: 1, ativos: 1, inativos: 0 } };
  }
  if (chave.includes("paginacao")) {
    return { ...PaginaVaziaTeste, content: [criarEmpregadoTeste()], totalElements: 1, totalPaginas: 1, totalPages: 1 };
  }
  if (chave.includes("indicador") || chave.includes("estatistic") || chave.includes("resumo") || chave.includes("analise")) {
    return {
      ...(valor && typeof valor === "object" ? valor : {}),
      total: 1,
      ativos: 1,
      inativos: 0,
      taxa: 1.5,
      percentual: 98.5,
      quantidade: 1,
      valor: 1,
      dados: [criarEmpregadoTeste()],
      itens: [criarEmpregadoTeste()],
      series: [{ name: "Teste", value: 1 }],
      labels: ["Teste"],
    };
  }
  if (chave.includes("dados") || chave.includes("form")) {
    return {
      ...(valor && typeof valor === "object" ? valor : {}),
      id: valor?.id ?? 1,
      nome: valor?.nome || "Registro Teste",
      descricao: valor?.descricao || "Registro Teste",
      ativo: valor?.ativo ?? true,
      uf: valor?.uf || "RJ",
      email: valor?.email || "teste@exemplo.invalid",
      matricula: valor?.matricula || "12345678",
      cpf: valor?.cpf || "00000000000",
      contratos: valor?.contratos || [criarContratoTeste()],
      lotacoes: valor?.lotacoes || [criarLotacaoTeste()],
      turnos: valor?.turnos || [criarTurnoTeste()],
      jornadas: valor?.jornadas || [criarTurnoTeste()],
    };
  }
  return valor;
};

export const criarPropsPopuladas = (entrada, { abrirModais = false, carregando = false } = {}) => {
  const base = criarPropsComponente(entrada);
  const props = { ...base };

  for (const nome of entrada.props || []) {
    const valor = props[nome];
    if (Array.isArray(valor)) props[nome] = [itemParaArray(nome)];
    else if (valor && typeof valor === "object" && !(valor instanceof Map) && !React.isValidElement(valor)) {
      props[nome] = popularObjeto(nome, valor);
    }
  }

  if ("carregando" in props) props.carregando = carregando;
  if ("estaCarregando" in props) props.estaCarregando = carregando;
  if ("carregandoOsDados" in props) props.carregandoOsDados = carregando;

  if (abrirModais) {
    if ("aberto" in props) props.aberto = true;
    if ("isOpen" in props) props.isOpen = true;
    if ("estaAberto" in props) props.estaAberto = true;
  }

  if (entrada.localName === "Tabela") {
    props.headers = ["Coluna de teste"];
    props.children = <tr><td>Conteúdo de teste</td></tr>;
  }

  if (entrada.localName === "TabelaRelatorio") {
    props.dados = [criarEmpregadoTeste()];
    props.diasDoMes = ["2026-08-01"];
    props.totalColaboradores = 1;
    props.totalPaginas = 2;
    props.primeira = false;
    props.ultima = false;
  }

  if (entrada.localName === "TabelaFrequencia") {
    props.listaDeFrequencia = [criarEmpregadoTeste()];
    props.statusDaData = { valid: true, podeEditar: true, bloqueado: false };
    props.eGestorOperacional = true;
    props.edicaoBloqueada = false;
    props.funcaoObterJanelaDeEdicao = vi.fn(() => ({ isValid: true, isEndingSoon: false }));
  }

  if (entrada.localName === "TabelaUsuarios") {
    props.usuarios = [criarUsuarioTeste()];
    props.gestorAtual = criarUsuarioTeste(99);
    props.obterPerfil = vi.fn(() => "OPERACIONAL");
    props.podeGerenciarUsuario = vi.fn(() => true);
    props.descreverAbrangencia = vi.fn(() => "Nacional");
  }

  if (entrada.localName === "GrupoEmpregados") {
    const empregado = criarEmpregadoTeste();
    props.grupo = { id: "grupo-teste", nome: "Grupo teste", empregados: [empregado], colaboradores: [empregado], itens: [empregado], stats: { total: 1, ativos: 1, inativos: 0 } };
    props.estaExpandido = true;
    props.acoes = { alternarVisualizacaoCpf: vi.fn() };
    props.modaisAcoes = {
      abrirEdicao: vi.fn(),
      abrirHistoricoContrato: vi.fn(),
      abrirHistoricoAuditoria: vi.fn(),
    };
  }

  if (entrada.localName === "PainelEventos") props.eventos = [criarEventoTeste()];
  if (entrada.localName === "TabelaAuditoria") props.dados = { ...PaginaVaziaTeste, content: [criarRegistroAuditoriaTeste()], totalElements: 1, totalPages: 1, totalPaginas: 1 };
  if (entrada.localName === "ListaContratos") props.contratos = [criarContratoTeste()];
  if (entrada.localName === "PainelExportacoes") {
    props.tarefas = [{
      id: "teste-1",
      relatorio: "Frequência",
      formato: "PDF",
      status: "CONCLUIDO",
      percentual: 100,
      processados: 1,
      totalEstimado: 1,
      podeBaixar: true,
      mensagem: "Concluído",
    }];
  }

  if (entrada.localName === "ModaisEstrutura" && abrirModais) {
    props.modal = { tipo: "superintendencia", aberto: true, editando: null };
  }
  if (entrada.localName === "ModalAgencia" && abrirModais) {
    props.estado = { aberto: true, editando: null };
  }

  return props;
};

export const criarRespostaApiPopulada = () => ({
  ...RespostaVaziaTeste,
  content: [criarEmpregadoTeste()],
  dados: [criarEmpregadoTeste()],
  items: [criarEmpregadoTeste()],
  totalElements: 1,
  totalPages: 1,
  totalPaginas: 1,
  contratos: [criarContratoTeste()],
  lotacoes: [criarLotacaoTeste()],
  turnos: [criarTurnoTeste()],
  jornadas: [criarTurnoTeste()],
  superintendencias: [criarSuperintendenciaTeste()],
  gestoesLocais: [criarGestaoLocalTeste()],
  usuarios: [criarUsuarioTeste()],
  empregados: [criarEmpregadoTeste()],
  eventos: [criarEventoTeste()],
  indicadores: { total: 1, taxa: 1.5 },
  resumo: { total: 1, taxa: 1.5 },
});

export const ehComponenteReact = (valor) => {
  if (typeof valor === "function") return true;
  return Boolean(valor && typeof valor === "object" && valor.$$typeof);
};

export const textoConsole = (argumentos) => argumentos
  .map((item) => item instanceof Error ? `${item.name}: ${item.message}` : String(item))
  .join(" ");

export const ehErroCriticoConsole = (argumentos) => /(?:TypeError|ReferenceError|RangeError|Maximum update depth|Cannot read properties|is not a function|is not defined|Objects are not valid as a React child|Each child in a list should have a unique|Encountered two children with the same key|A component is changing an uncontrolled input|A component is changing a controlled input)/i.test(textoConsole(argumentos));

export const ehModal = (entrada) => /Modal|Dialog|Janela/i.test(entrada.localName) || /\/Modal/i.test(entrada.file);

export const ehPagina = (entrada) => /\/pages\/[^/]+\/index\.jsx$/i.test(entrada.file) || /Pagina[A-Z]/.test(entrada.localName);
