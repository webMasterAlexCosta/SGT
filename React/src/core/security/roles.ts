import type { PerfilAcesso } from '../../contracts/java';
import { formatarGestaoLocal, formatarGestorEstadual } from "../formatters/nomenclaturaGestao.ts";

export const PERFIS = Object.freeze({
  ADMINISTRADOR: "ADMINISTRADOR",
  GESTOR_NACIONAL: "GESTOR_NACIONAL",
  GESTOR_ESTADUAL: "GESTOR_ESTADUAL",
  GESTOR_LOCAL: "GESTOR_LOCAL",
  PRESIDENTE: "PRESIDENTE",
  OPERACIONAL: "OPERACIONAL",
});

export const AUTORIDADES = Object.freeze({
  ADMINISTRADOR: "PERFIL_ADMINISTRADOR",
  GESTOR_NACIONAL: "PERFIL_GESTOR_NACIONAL",
  GESTOR_ESTADUAL: "PERFIL_GESTOR_ESTADUAL",
  GESTOR_LOCAL: "PERFIL_GESTOR_LOCAL",
  PRESIDENTE: "PERFIL_PRESIDENTE",
  OPERACIONAL: "PERFIL_OPERACIONAL",
});

const AUTORIDADE_POR_PERFIL: Readonly<Record<PerfilAcesso, string>> = Object.freeze({
  [PERFIS.ADMINISTRADOR]: AUTORIDADES.ADMINISTRADOR,
  [PERFIS.GESTOR_NACIONAL]: AUTORIDADES.GESTOR_NACIONAL,
  [PERFIS.GESTOR_ESTADUAL]: AUTORIDADES.GESTOR_ESTADUAL,
  [PERFIS.GESTOR_LOCAL]: AUTORIDADES.GESTOR_LOCAL,
  [PERFIS.PRESIDENTE]: AUTORIDADES.PRESIDENTE,
  [PERFIS.OPERACIONAL]: AUTORIDADES.OPERACIONAL,
});

const PERFIL_POR_AUTORIDADE = new Map<string, PerfilAcesso>(
  Object.values(PERFIS).map((perfil) => [AUTORIDADE_POR_PERFIL[perfil], perfil]),
);
const normalizarTexto = (valor: unknown): string => String(valor || "").trim().toUpperCase();
const ehPerfil = (valor: string): valor is PerfilAcesso => Object.hasOwn(AUTORIDADE_POR_PERFIL, valor);
const campo = (valor: unknown, nome: string): unknown => valor !== null && typeof valor === 'object' ? Reflect.get(valor, nome) : undefined;
const primeiraAutoridade = (valor: unknown): unknown => {
  const autoridades = campo(valor, 'authorities');
  return Array.isArray(autoridades) ? autoridades[0] : undefined;
};
export const autoridadeDoPerfil = (perfil: unknown): string | null => {
  const valor = normalizarTexto(perfil);
  return ehPerfil(valor) ? AUTORIDADE_POR_PERFIL[valor] : null;
};
export const perfilDaAutoridade = (autoridade: unknown): PerfilAcesso | null => PERFIL_POR_AUTORIDADE.get(normalizarTexto(autoridade)) || null;

export const obterPerfilPrincipal = (usuarioOuPerfil: unknown): PerfilAcesso | null => {
  if (!usuarioOuPerfil) return null;
  if (typeof usuarioOuPerfil === 'string') {
    const valor = normalizarTexto(usuarioOuPerfil).split(',')[0]?.trim() || '';
    return ehPerfil(valor) ? valor : PERFIL_POR_AUTORIDADE.get(valor) || null;
  }
  if (Array.isArray(usuarioOuPerfil)) return obterPerfilPrincipal(usuarioOuPerfil[0]);
  return obterPerfilPrincipal(campo(usuarioOuPerfil, 'perfil') || campo(usuarioOuPerfil, 'autoridade') || campo(usuarioOuPerfil, 'authority') || primeiraAutoridade(usuarioOuPerfil) || null);
};

export const obterAutoridade = (usuarioOuPerfil: unknown): string | null => {
  if (!usuarioOuPerfil) return null;
  if (typeof usuarioOuPerfil === 'string') {
    const valor = normalizarTexto(usuarioOuPerfil).split(',')[0]?.trim() || '';
    if (PERFIL_POR_AUTORIDADE.has(valor)) return valor;
    return ehPerfil(valor) ? AUTORIDADE_POR_PERFIL[valor] : null;
  }
  if (Array.isArray(usuarioOuPerfil)) return obterAutoridade(usuarioOuPerfil[0]);
  const direta = normalizarTexto(campo(usuarioOuPerfil, 'autoridade') || campo(usuarioOuPerfil, 'authority') || primeiraAutoridade(usuarioOuPerfil) || '');
  return PERFIL_POR_AUTORIDADE.has(direta) ? direta : autoridadeDoPerfil(campo(usuarioOuPerfil, 'perfil'));
};

export const possuiAutoridade = (usuario: unknown, ...autoridades: string[]) => {
  const atual = obterAutoridade(usuario);
  if (!atual) return false;
  return autoridades.some((autoridade) => atual === normalizarTexto(autoridade));
};

export const possuiAcessoGlobal = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
  );

export const eAdministradorTecnico = (usuario: unknown) => possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);
export const eGestorNacional = (usuario: unknown) => possuiAutoridade(usuario, AUTORIDADES.GESTOR_NACIONAL);
export const ePresidente = (usuario: unknown) => possuiAutoridade(usuario, AUTORIDADES.PRESIDENTE);
export const eGestorEstadual = (usuario: unknown) => possuiAutoridade(usuario, AUTORIDADES.GESTOR_ESTADUAL);
// Alias interno mantido para compatibilidade nominal entre módulos durante a migração por etapas.
export const eSuperintendencia = eGestorEstadual;
export const eGestaoLocal = (usuario: unknown) => possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);
export const eUsuarioOperacional = (usuario: unknown) => possuiAutoridade(usuario, AUTORIDADES.OPERACIONAL);

export const podeAcessarPainelGerencial = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

// Relatorios operacionais do RelatorioControlador possuem seguranca de metodo
// mais restritiva que o matcher HTTP geral: Presidente nao acessa frequencia
// consolidada, absenteismo, terceirizados nem os jobs/arquivos dessas emissoes.
export const podeAcessarRelatorios = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeAcessarVisaoConsolidadaRelatorios = podeAcessarRelatorios;

// O RelatorioAdministrativoControlador concede leitura/previa aos cinco perfis
// gerenciais. A emissao formal tem um @PreAuthorize adicional que exclui o
// Presidente; por isso leitura e emissao sao capacidades distintas no React.
export const podeAcessarRelatoriosAdministrativos = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeEmitirRelatoriosAdministrativos = (usuario: unknown) =>
  podeAcessarRelatoriosAdministrativos(usuario) && !ePresidente(usuario);

export const podeAcessarRelatorioHorasTrabalhadas = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeVisualizarContratos = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeGerenciarContratos = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeVisualizarFrequencia = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeAcessarFrequenciaDiaria = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL, AUTORIDADES.OPERACIONAL);

export const podeAcessarOcorrencias = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeGerenciarOcorrencias = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeVisualizarEmpregados = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeGerenciarEmpregados = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

// Pessoa e Vínculo são os agregados canônicos a partir da Etapa 11. Os aliases
// antigos permanecem exportados enquanto frequência/relatórios ainda consomem
// a projeção de Empregado, mas não concedem capacidades adicionais.
export const podeVisualizarPessoasVinculos = (usuario: unknown) =>
  podeVisualizarEmpregados(usuario) || ePresidente(usuario);

export const podeGerenciarPessoasVinculos = (usuario: unknown) =>
  podeGerenciarEmpregados(usuario);

export const podeGerenciarUsuarios = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeGerenciarConfiguracoesSistema = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

export const podeGerenciarCalendario = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeVisualizarCalendario = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeVisualizarLotacoesETurnos = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeGerenciarLotacoesETurnos = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeVisualizarEstruturaOrganizacional = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeGerenciarEstruturaOrganizacional = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

export const podeVisualizarCatalogosReferencia = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeGerenciarCadastrosNacionais = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

export const podeVisualizarTiposContratacao = (usuario: unknown) => Boolean(obterAutoridade(usuario));
export const podeGerenciarTiposContratacao = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeGerenciarCargos = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeAcessarPendenciasAdministrativas = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeAcessarPermissoesExcepcionais = (usuario: unknown) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeGerenciarPermissoesExcepcionais = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

// Apesar do matcher HTTP de leitura ser mais amplo, o serviço Java restringe
// o histórico de usos ao Administrador. O menu permanece fail-closed.
export const podeVisualizarUsosPermissaoExcepcional = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

export const podeGerenciarAjustesFrequencia = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeAcessarAuditoria = (usuario: unknown) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

export const rotaInicialPorPerfil = (usuario: unknown) => {
  if (podeAcessarPainelGerencial(usuario)) return "/painel";
  if (podeVisualizarFrequencia(usuario)) return "/frequencia";
  return "/perfil";
};

export const rotuloPerfil = (usuarioOuPerfil: unknown) => {
  const perfil = obterPerfilPrincipal(usuarioOuPerfil);
  if (perfil === PERFIS.PRESIDENTE) return "Presidente";
  if (perfil === PERFIS.GESTOR_NACIONAL) return "Gestor nacional";
  if (perfil === PERFIS.ADMINISTRADOR) return "Administrador técnico";
  if (perfil === PERFIS.GESTOR_ESTADUAL) {
    const superintendencia = typeof usuarioOuPerfil === "object"
      ? campo(usuarioOuPerfil, 'superintendencia') || campo(usuarioOuPerfil, 'superintendenciaNome')
      : null;
    const uf = typeof usuarioOuPerfil === "object"
      ? campo(usuarioOuPerfil, 'uf') || campo(campo(usuarioOuPerfil, 'superintendencia'), 'uf')
      : "";
    return formatarGestorEstadual(superintendencia, uf);
  }
  if (perfil === PERFIS.GESTOR_LOCAL) {
    const gestaoLocal = typeof usuarioOuPerfil === "object"
      ? campo(usuarioOuPerfil, 'gestaoLocal') || campo(usuarioOuPerfil, 'gestaoLocalNome')
      : null;
    return gestaoLocal ? formatarGestaoLocal(gestaoLocal) : "Gestor local";
  }
  if (perfil === PERFIS.OPERACIONAL) return "Usuário operacional";
  return "Perfil não identificado";
};
