import { formatarGestaoLocal, formatarGestorEstadual } from "../formatters/nomenclaturaGestao.js";

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

const AUTORIDADE_POR_PERFIL = Object.freeze({
  [PERFIS.ADMINISTRADOR]: AUTORIDADES.ADMINISTRADOR,
  [PERFIS.GESTOR_NACIONAL]: AUTORIDADES.GESTOR_NACIONAL,
  [PERFIS.GESTOR_ESTADUAL]: AUTORIDADES.GESTOR_ESTADUAL,
  [PERFIS.GESTOR_LOCAL]: AUTORIDADES.GESTOR_LOCAL,
  [PERFIS.PRESIDENTE]: AUTORIDADES.PRESIDENTE,
  [PERFIS.OPERACIONAL]: AUTORIDADES.OPERACIONAL,
});

const PERFIL_POR_AUTORIDADE = Object.freeze(
  Object.fromEntries(Object.entries(AUTORIDADE_POR_PERFIL).map(([perfil, autoridade]) => [autoridade, perfil])),
);

const normalizarTexto = (valor) => String(valor || "").trim().toUpperCase();

export const autoridadeDoPerfil = (perfil) => AUTORIDADE_POR_PERFIL[normalizarTexto(perfil)] || null;
export const perfilDaAutoridade = (autoridade) => PERFIL_POR_AUTORIDADE[normalizarTexto(autoridade)] || null;

export const obterPerfilPrincipal = (usuarioOuPerfil) => {
  if (!usuarioOuPerfil) return null;
  if (typeof usuarioOuPerfil === "string") {
    const valor = normalizarTexto(usuarioOuPerfil).split(",")[0]?.trim();
    if (AUTORIDADE_POR_PERFIL[valor]) return valor;
    return PERFIL_POR_AUTORIDADE[valor] || null;
  }
  if (Array.isArray(usuarioOuPerfil)) {
    return obterPerfilPrincipal(usuarioOuPerfil[0]);
  }
  return obterPerfilPrincipal(
    usuarioOuPerfil.perfil
      || usuarioOuPerfil.autoridade
      || usuarioOuPerfil.authority
      || usuarioOuPerfil.authorities?.[0]
      || null,
  );
};

export const obterAutoridade = (usuarioOuPerfil) => {
  if (!usuarioOuPerfil) return null;
  if (typeof usuarioOuPerfil === "string") {
    const valor = normalizarTexto(usuarioOuPerfil).split(",")[0]?.trim();
    if (PERFIL_POR_AUTORIDADE[valor]) return valor;
    return AUTORIDADE_POR_PERFIL[valor] || null;
  }
  if (Array.isArray(usuarioOuPerfil)) return obterAutoridade(usuarioOuPerfil[0]);
  const direta = normalizarTexto(
    usuarioOuPerfil.autoridade
      || usuarioOuPerfil.authority
      || usuarioOuPerfil.authorities?.[0]
      || "",
  );
  if (PERFIL_POR_AUTORIDADE[direta]) return direta;
  return autoridadeDoPerfil(usuarioOuPerfil.perfil) || null;
};

export const possuiAutoridade = (usuario, ...autoridades) => {
  const atual = obterAutoridade(usuario);
  if (!atual) return false;
  return autoridades.some((autoridade) => atual === normalizarTexto(autoridade));
};

export const possuiAcessoGlobal = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
  );

export const eAdministradorTecnico = (usuario) => possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);
export const eGestorNacional = (usuario) => possuiAutoridade(usuario, AUTORIDADES.GESTOR_NACIONAL);
export const ePresidente = (usuario) => possuiAutoridade(usuario, AUTORIDADES.PRESIDENTE);
export const eGestorEstadual = (usuario) => possuiAutoridade(usuario, AUTORIDADES.GESTOR_ESTADUAL);
// Alias interno mantido para compatibilidade nominal entre módulos durante a migração por etapas.
export const eSuperintendencia = eGestorEstadual;
export const eGestaoLocal = (usuario) => possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);
export const eUsuarioOperacional = (usuario) => possuiAutoridade(usuario, AUTORIDADES.OPERACIONAL);

export const podeAcessarPainelGerencial = (usuario) =>
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
export const podeAcessarRelatorios = (usuario) =>
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
export const podeAcessarRelatoriosAdministrativos = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeEmitirRelatoriosAdministrativos = (usuario) =>
  podeAcessarRelatoriosAdministrativos(usuario) && !ePresidente(usuario);

export const podeAcessarRelatorioHorasTrabalhadas = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeVisualizarContratos = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeGerenciarContratos = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeVisualizarFrequencia = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeAcessarFrequenciaDiaria = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL, AUTORIDADES.OPERACIONAL);

export const podeAcessarOcorrencias = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeGerenciarOcorrencias = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeVisualizarEmpregados = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeGerenciarEmpregados = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

// Pessoa e Vínculo são os agregados canônicos a partir da Etapa 11. Os aliases
// antigos permanecem exportados enquanto frequência/relatórios ainda consomem
// a projeção de Empregado, mas não concedem capacidades adicionais.
export const podeVisualizarPessoasVinculos = (usuario) =>
  podeVisualizarEmpregados(usuario) || ePresidente(usuario);

export const podeGerenciarPessoasVinculos = (usuario) =>
  podeGerenciarEmpregados(usuario);

export const podeGerenciarUsuarios = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeGerenciarConfiguracoesSistema = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

export const podeGerenciarCalendario = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeVisualizarCalendario = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeVisualizarLotacoesETurnos = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeGerenciarLotacoesETurnos = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeVisualizarEstruturaOrganizacional = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeGerenciarEstruturaOrganizacional = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

export const podeVisualizarCatalogosReferencia = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
    AUTORIDADES.OPERACIONAL,
  );

export const podeGerenciarCadastrosNacionais = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

export const podeVisualizarTiposContratacao = (usuario) => Boolean(obterAutoridade(usuario));
export const podeGerenciarTiposContratacao = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeGerenciarCargos = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeAcessarPendenciasAdministrativas = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeAcessarPermissoesExcepcionais = (usuario) =>
  possuiAutoridade(
    usuario,
    AUTORIDADES.ADMINISTRADOR,
    AUTORIDADES.GESTOR_NACIONAL,
    AUTORIDADES.PRESIDENTE,
    AUTORIDADES.GESTOR_ESTADUAL,
    AUTORIDADES.GESTOR_LOCAL,
  );

export const podeGerenciarPermissoesExcepcionais = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

// Apesar do matcher HTTP de leitura ser mais amplo, o serviço Java restringe
// o histórico de usos ao Administrador. O menu permanece fail-closed.
export const podeVisualizarUsosPermissaoExcepcional = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

export const podeGerenciarAjustesFrequencia = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.GESTOR_LOCAL);

export const podeAcessarAuditoria = (usuario) =>
  possuiAutoridade(usuario, AUTORIDADES.ADMINISTRADOR);

export const rotaInicialPorPerfil = (usuario) => {
  if (podeAcessarPainelGerencial(usuario)) return "/painel";
  if (podeVisualizarFrequencia(usuario)) return "/frequencia";
  return "/perfil";
};

export const rotuloPerfil = (usuarioOuPerfil) => {
  const perfil = obterPerfilPrincipal(usuarioOuPerfil);
  if (perfil === PERFIS.PRESIDENTE) return "Presidente";
  if (perfil === PERFIS.GESTOR_NACIONAL) return "Gestor nacional";
  if (perfil === PERFIS.ADMINISTRADOR) return "Administrador técnico";
  if (perfil === PERFIS.GESTOR_ESTADUAL) {
    const superintendencia = typeof usuarioOuPerfil === "object"
      ? usuarioOuPerfil?.superintendencia || usuarioOuPerfil?.superintendenciaNome
      : null;
    const uf = typeof usuarioOuPerfil === "object"
      ? usuarioOuPerfil?.uf || usuarioOuPerfil?.superintendencia?.uf
      : "";
    return formatarGestorEstadual(superintendencia, uf);
  }
  if (perfil === PERFIS.GESTOR_LOCAL) {
    const gestaoLocal = typeof usuarioOuPerfil === "object"
      ? usuarioOuPerfil?.gestaoLocal || usuarioOuPerfil?.gestaoLocalNome
      : null;
    return gestaoLocal ? formatarGestaoLocal(gestaoLocal) : "Gestor local";
  }
  if (perfil === PERFIS.OPERACIONAL) return "Usuário operacional";
  return "Perfil não identificado";
};
