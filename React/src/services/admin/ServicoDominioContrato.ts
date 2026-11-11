import type { ContratoDTO } from '../../contracts/java';
export type ContractView = Partial<ContratoDTO> & { gestaoLocalUf?: string | null };
const UM_DIA_EM_MS = 24 * 60 * 60 * 1000;

export const UFS_BRASIL = Object.freeze([
  "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
  "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
  "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO",
]);

export const ESTADOS_NEGOCIAIS = Object.freeze({
  RASCUNHO: "RASCUNHO",
  PUBLICADO: "PUBLICADO",
  CANCELADO: "CANCELADO",
  ANULADO: "ANULADO",
});

export const SITUACOES_TEMPORAIS = Object.freeze({
  RASCUNHO: "RASCUNHO",
  FUTURO: "FUTURO",
  VIGENTE: "VIGENTE",
  ENCERRADO: "ENCERRADO",
  CANCELADO: "CANCELADO",
  ANULADO: "ANULADO",
});

export const HABILITACOES_ADMINISTRATIVAS = Object.freeze({ ATIVO: "ATIVO", INATIVO: "INATIVO" });

export const normalizarTexto = (valor: unknown) =>
  String(valor ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();

export const formatarDataBrasileira = (dataIso: string | null | undefined, textoPadrao = "—") => {
  if (!dataIso) return textoPadrao;
  const data = new Date(`${String(dataIso).split("T")[0]}T12:00:00`);
  if (Number.isNaN(data.getTime())) return textoPadrao;
  return new Intl.DateTimeFormat("pt-BR").format(data);
};

export const formatarDataHoraBrasileira = (valor: unknown, textoPadrao = "—") => {
  if (!valor) return textoPadrao;
  const data = valor instanceof Date ? new Date(valor.getTime()) : new Date(typeof valor === "number" ? valor : String(valor));
  if (Number.isNaN(data.getTime())) return textoPadrao;
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "medium" }).format(data);
};

export const obterSituacaoVigencia = (contrato: ContractView | null | undefined, dataReferencia = new Date()) => {
  if (contrato?.situacaoTemporal) {
    return { chave: contrato.situacaoTemporal, rotulo: rotularSituacaoTemporal(contrato.situacaoTemporal), diasRestantes: null };
  }
  if (!contrato?.dataFim) return { chave: "SEM_DATA_FIM", rotulo: "Sem término definido", diasRestantes: null };
  const fim = new Date(`${String(contrato.dataFim).split("T")[0]}T23:59:59`);
  const referencia = new Date(dataReferencia.getTime());
  referencia.setHours(0, 0, 0, 0);
  const diasRestantes = Math.ceil((fim.getTime() - referencia.getTime()) / UM_DIA_EM_MS);
  if (diasRestantes < 0) return { chave: "ENCERRADO", rotulo: "Encerrado", diasRestantes };
  if (diasRestantes <= 30) return { chave: "VENCENDO_30_DIAS", rotulo: diasRestantes === 0 ? "Vence hoje" : `Vence em ${diasRestantes} dia${diasRestantes === 1 ? "" : "s"}`, diasRestantes };
  if (diasRestantes <= 90) return { chave: "VENCENDO_90_DIAS", rotulo: `Vence em ${diasRestantes} dias`, diasRestantes };
  return { chave: "VIGENTE", rotulo: "Vigente", diasRestantes };
};

export const rotularEstadoNegocial = (valor: unknown) => ({
  RASCUNHO: "Rascunho",
  PUBLICADO: "Publicado",
  CANCELADO: "Cancelado",
  ANULADO: "Anulado",
}[String(valor || "").toUpperCase()] || "Não informado");

export const rotularSituacaoTemporal = (valor: unknown) => ({
  RASCUNHO: "Rascunho",
  FUTURO: "Futuro",
  VIGENTE: "Vigente",
  ENCERRADO: "Encerrado",
  CANCELADO: "Cancelado",
  ANULADO: "Anulado",
}[String(valor || "").toUpperCase()] || "Não informada");

export const rotularHabilitacao = (valor: unknown) => String(valor || "").toUpperCase() === "INATIVO" ? "Inativo" : "Ativo";

export const ehRascunho = (contrato: ContractView | null | undefined) => String(contrato?.estadoNegocial || "").toUpperCase() === ESTADOS_NEGOCIAIS.RASCUNHO;
export const ehPublicadoValido = (contrato: ContractView | null | undefined) => String(contrato?.estadoNegocial || "").toUpperCase() === ESTADOS_NEGOCIAIS.PUBLICADO;
export const podeEditarRascunho = ehRascunho;
export const podeDescartarRascunho = ehRascunho;

export const calcularIndicadoresDosContratos = (contratos: ContractView[] = []) => {
  const total = contratos.length;
  const rascunhos = contratos.filter(ehRascunho).length;
  const vigentes = contratos.filter((c) => c.situacaoTemporal === SITUACOES_TEMPORAIS.VIGENTE).length;
  const futuros = contratos.filter((c) => c.situacaoTemporal === SITUACOES_TEMPORAIS.FUTURO).length;
  const inativos = contratos.filter((c) => c.habilitacaoAdministrativa === HABILITACOES_ADMINISTRATIVAS.INATIVO || c.ativo === false).length;
  const encerrados = contratos.filter((c) => c.situacaoTemporal === SITUACOES_TEMPORAIS.ENCERRADO).length;
  const vencendo = contratos.filter((contrato) => ["VENCENDO_30_DIAS", "VENCENDO_90_DIAS"].includes(obterSituacaoVigencia({ ...contrato, situacaoTemporal: null }).chave)).length;
  const vencidos = encerrados;
  const ativos = total - inativos;
  const efetivoPrevisto = contratos.reduce((soma, contrato) => soma + Number(contrato.efetivoPrevisto || 0), 0);
  const efetivoAtual = contratos.reduce((soma, contrato) => soma + Number(contrato.efetivoAtual || 0), 0);
  return { total, ativos, inativos, rascunhos, vigentes, futuros, encerrados, vencendo, vencidos, efetivoPrevisto, efetivoAtual, saldoEfetivo: efetivoPrevisto - efetivoAtual };
};

export const obterUfsDisponiveis = (contratos: ContractView[] = [], acessoNacional = false) => {
  if (acessoNacional) return [...UFS_BRASIL];
  return Array.from(new Set(contratos.map((contrato) => contrato.uf || contrato.gestaoLocalUf).filter(Boolean).map((uf) => String(uf).toUpperCase()))).sort((a, b) => a.localeCompare(b, "pt-BR"));
};

export const filtrarContratos = <T extends ContractView>({ contratos = [], termoBusca = "", filtroStatus = "TODOS", filtroUf = "TODAS", filtroGestaoLocal = "TODAS", filtroVigencia = "TODAS" }: { contratos?: T[]; termoBusca?: string; filtroStatus?: string; filtroUf?: string; filtroGestaoLocal?: string; filtroVigencia?: string }) => {
  const termo = normalizarTexto(termoBusca);
  return contratos.filter((contrato) => {
    const correspondeBusca = !termo || [contrato.numeroContrato, contrato.anoContrato, contrato.descricao, contrato.tipoContratacaoDescricao, contrato.razaoSocialEmpresa, contrato.nomeFantasiaEmpresa, contrato.cnpjEmpresa, contrato.gestaoLocalNome, contrato.superintendenciaNome, contrato.uf].some((valor) => normalizarTexto(valor).includes(termo));
    const habilitacao = String(contrato.habilitacaoAdministrativa || (contrato.ativo === false ? "INATIVO" : "ATIVO")).toUpperCase();
    const correspondeStatus = filtroStatus === "TODOS"
      || (filtroStatus === "ATIVOS" && habilitacao === "ATIVO")
      || (filtroStatus === "INATIVOS" && habilitacao === "INATIVO")
      || (filtroStatus === "RASCUNHOS" && ehRascunho(contrato))
      || String(contrato.estadoNegocial || "").toUpperCase() === filtroStatus;
    const ufContrato = String(contrato.uf || contrato.gestaoLocalUf || "").toUpperCase();
    const correspondeUf = filtroUf === "TODAS" || ufContrato === filtroUf;
    const correspondeGestaoLocal = filtroGestaoLocal === "TODAS" || String(contrato.gestaoLocalId || "") === String(filtroGestaoLocal);
    const temporal = String(contrato.situacaoTemporal || obterSituacaoVigencia(contrato).chave).toUpperCase();
    const correspondeVigencia = filtroVigencia === "TODAS"
      || temporal === filtroVigencia
      || (filtroVigencia === "VENCENDO" && ["VENCENDO_90_DIAS", "VENCENDO_30_DIAS"].includes(obterSituacaoVigencia({ ...contrato, situacaoTemporal: null }).chave));
    return correspondeBusca && correspondeStatus && correspondeUf && correspondeGestaoLocal && correspondeVigencia;
  });
};

export const obterAcoesCicloVida = (contrato: ContractView | null | undefined, podeGerenciar: boolean) => {
  if (!podeGerenciar || !contrato) return [];
  if (ehRascunho(contrato)) return ["EDITAR", "PUBLICAR", "DESCARTAR"];
  if (!ehPublicadoValido(contrato)) return [];
  const acoes: string[] = [];
  if (contrato.habilitacaoAdministrativa === "INATIVO" || contrato.ativo === false) acoes.push("REATIVAR");
  else acoes.push("INATIVAR");
  if (contrato.situacaoTemporal === "FUTURO") acoes.push("CANCELAR_FUTURO");
  if (!["ENCERRADO", "CANCELADO", "ANULADO"].includes(contrato.situacaoTemporal ?? '')) acoes.push("ENCERRAR_ANTECIPADAMENTE");
  acoes.push("ANULAR");
  return acoes;
};

export const prepararContratoRascunho = (dados: import('../../contracts/formularios').FormDraft<import('../../contracts/java').ContratoDTO>, gestaoLocalIdForcada: number | string | null = null) => ({
  id: dados?.id ? Number(dados.id) : undefined,
  numeroContrato: String(dados?.numeroContrato || "").trim() || null,
  anoContrato: dados?.anoContrato === "" || dados?.anoContrato == null ? null : Number(dados.anoContrato),
  descricao: String(dados?.descricao || "").trim() || null,
  tipoContratacaoId: dados?.tipoContratacaoId ? Number(dados.tipoContratacaoId) : null,
  fornecedorId: dados?.fornecedorId ? Number(dados.fornecedorId) : null,
  cnpjEmpresa: String(dados?.cnpjEmpresa || "").replace(/\D/g, "") || null,
  razaoSocialEmpresa: String(dados?.razaoSocialEmpresa || "").trim() || null,
  nomeFantasiaEmpresa: String(dados?.nomeFantasiaEmpresa || "").trim() || null,
  dataInicio: dados?.dataInicio || null,
  dataFim: dados?.dataFim || null,
  efetivoPrevisto: Math.max(0, Number(dados?.efetivoPrevisto || 0)),
  gestaoLocalId: gestaoLocalIdForcada ? Number(gestaoLocalIdForcada) : (dados?.gestaoLocalId ? Number(dados.gestaoLocalId) : null),
  matriculaVinculoObrigatoria: Boolean(dados?.matriculaVinculoObrigatoria),
});
