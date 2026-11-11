import type * as DTO from '../../contracts/java';
import type { FormDraft } from '../../contracts/formularios';
type AllocationChangeDraft = FormDraft<DTO.AlteracaoAlocacaoVinculoRequisicao> & { exigePrecedenciaUltimoDia?: boolean };
const inteiroOuNulo = (valor: unknown) => {
  if (valor === "" || valor == null) return null;
  const numero = Number(valor);
  return Number.isFinite(numero) ? numero : null;
};

const texto = (valor: unknown) => String(valor ?? "").trim();

export const limparParametrosAlocacao = (parametros = {}) => Object.fromEntries(
  Object.entries(parametros).filter(([, valor]) => valor !== "" && valor != null),
);

export const criarChaveContextoOpcoesAlocacao = ({ contratoId, data, lotacaoId, jornadaId }: { contratoId?: string | number | null; data?: string | null; lotacaoId?: string | number | null; jornadaId?: string | number | null } = {}) => [
  contratoId ?? "", data ?? "", lotacaoId ?? "", jornadaId ?? "",
].join("|");

export const prepararAlteracaoAlocacao = (dados: AllocationChangeDraft = {}) => ({
  dataEfetiva: dados.dataEfetiva || null,
  composicaoId: inteiroOuNulo(dados.composicaoId),
  lotacaoId: inteiroOuNulo(dados.lotacaoId),
  jornadaId: inteiroOuNulo(dados.jornadaId),
  cargoId: inteiroOuNulo(dados.cargoId),
  justificativa: texto(dados.justificativa),
  evidencia: texto(dados.evidencia),
  autorizacao: texto(dados.autorizacao),
  confirmarImpacto: Boolean(dados.confirmarImpacto),
  tokenPrevia: dados.tokenPrevia || null,
  confirmarPrecedenciaUltimoDia: Boolean(dados.confirmarPrecedenciaUltimoDia),
  confirmarInclusaoExcedente: Boolean(dados.confirmarInclusaoExcedente),
});

export const prepararCancelamentoAlocacao = (dados: FormDraft<DTO.AcaoVinculoContratualRequisicao> = {}) => ({
  justificativa: texto(dados.justificativa),
  evidencia: texto(dados.evidencia),
  autorizacao: texto(dados.autorizacao),
  confirmarImpacto: Boolean(dados.confirmarImpacto),
  tokenPrevia: dados.tokenPrevia || null,
});

export const validarAlteracaoAlocacao = (dados: AllocationChangeDraft = {}) => {
  if (!dados.dataEfetiva) return "Informe a data efetiva da nova faixa de alocação.";
  if (!dados.lotacaoId || !dados.jornadaId || !dados.cargoId) return "Selecione Lotação, Turno e Cargo nas opções válidas do servidor.";
  if (!texto(dados.justificativa) || !texto(dados.evidencia) || !texto(dados.autorizacao)) {
    return "Justificativa, evidência e autorização são obrigatórias.";
  }
  if (!dados.tokenPrevia || !dados.confirmarImpacto) return "Gere a prévia atual e confirme os impactos antes de concluir.";
  if (dados.exigePrecedenciaUltimoDia && !dados.confirmarPrecedenciaUltimoDia) {
    return "Confirme a precedência da alocação no último dia do vínculo.";
  }
  return null;
};

export const validarCancelamentoAlocacao = (dados: FormDraft<DTO.AcaoVinculoContratualRequisicao> = {}) => {
  if (!texto(dados.justificativa) || !texto(dados.evidencia) || !texto(dados.autorizacao)) {
    return "Justificativa, evidência e autorização são obrigatórias para cancelar a programação.";
  }
  return null;
};

export const rotularEstadoAlocacao = (estado: (import("../../contracts/java").EstadoNegocialAlocacao | null)) => (({
  REGISTRADA: "Registrada",
  SUBSTITUIDA: "Substituída",
  CANCELADA: "Cancelada",
} as Readonly<Record<string, string | undefined>>)[String(estado)] || estado || "—");

export const rotularSituacaoTemporalAlocacao = (situacao: (import("../../contracts/java").SituacaoTemporalAlocacao | null)) => (({
  PROGRAMADA: "Programada",
  VIGENTE: "Vigente",
  ENCERRADA: "Encerrada",
  CANCELADA: "Cancelada",
  AUSENTE: "Sem alocação",
} as Readonly<Record<string, string | undefined>>)[String(situacao)] || situacao || "—");

export const rotularTipoEventoAlocacao = (tipo: (import("../../contracts/java").TipoEventoAlocacaoVinculo | null)) => (({
  CRIACAO: "Criação",
  ALTERACAO: "Alteração",
  RETIFICACAO: "Retificação",
  CANCELAMENTO_PROGRAMACAO: "Cancelamento de programação",
  AJUSTE_LIMITE_VINCULO: "Ajuste de limite",
  TRANSFERENCIA_GESTAO: "Transferência de gestão",
} as Readonly<Record<string, string | undefined>>)[String(tipo)] || tipo || "Evento de alocação");

export const rotularRelacaoEventoAlocacao = (relacao: (import("../../contracts/java").TipoRelacaoEventoAlocacao | null)) => (({
  ORIGEM: "Origem",
  SUBSTITUI: "Substitui",
  RETIFICA: "Retifica",
  CANCELA: "Cancela",
  AJUSTA: "Ajusta",
} as Readonly<Record<string, string | undefined>>)[String(relacao)] || relacao || "—");

export const ehProgramacaoFuturaCancelavel = (alocacao: (import("../../contracts/java").AlocacaoVinculoDTO), hoje: (string)) => Boolean(
  alocacao?.estadoNegocial === "REGISTRADA" && alocacao?.dataInicio && hoje && hoje < alocacao.dataInicio,
);
