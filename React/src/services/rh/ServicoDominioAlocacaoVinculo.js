const inteiroOuNulo = (valor) => {
  if (valor === "" || valor == null) return null;
  const numero = Number(valor);
  return Number.isFinite(numero) ? numero : null;
};

const texto = (valor) => String(valor ?? "").trim();

export const limparParametrosAlocacao = (parametros = {}) => Object.fromEntries(
  Object.entries(parametros).filter(([, valor]) => valor !== "" && valor != null),
);

export const criarChaveContextoOpcoesAlocacao = ({ contratoId, data, lotacaoId, jornadaId } = {}) => [
  contratoId ?? "", data ?? "", lotacaoId ?? "", jornadaId ?? "",
].join("|");

export const prepararAlteracaoAlocacao = (dados = {}) => ({
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

export const prepararCancelamentoAlocacao = (dados = {}) => ({
  justificativa: texto(dados.justificativa),
  evidencia: texto(dados.evidencia),
  autorizacao: texto(dados.autorizacao),
  confirmarImpacto: Boolean(dados.confirmarImpacto),
  tokenPrevia: dados.tokenPrevia || null,
});

export const validarAlteracaoAlocacao = (dados = {}) => {
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

export const validarCancelamentoAlocacao = (dados = {}) => {
  if (!texto(dados.justificativa) || !texto(dados.evidencia) || !texto(dados.autorizacao)) {
    return "Justificativa, evidência e autorização são obrigatórias para cancelar a programação.";
  }
  return null;
};

export const rotularEstadoAlocacao = (estado) => ({
  REGISTRADA: "Registrada",
  SUBSTITUIDA: "Substituída",
  CANCELADA: "Cancelada",
}[estado] || estado || "—");

export const rotularSituacaoTemporalAlocacao = (situacao) => ({
  PROGRAMADA: "Programada",
  VIGENTE: "Vigente",
  ENCERRADA: "Encerrada",
  CANCELADA: "Cancelada",
  AUSENTE: "Sem alocação",
}[situacao] || situacao || "—");

export const rotularTipoEventoAlocacao = (tipo) => ({
  CRIACAO: "Criação",
  ALTERACAO: "Alteração",
  RETIFICACAO: "Retificação",
  CANCELAMENTO_PROGRAMACAO: "Cancelamento de programação",
  AJUSTE_LIMITE_VINCULO: "Ajuste de limite",
  TRANSFERENCIA_GESTAO: "Transferência de gestão",
}[tipo] || tipo || "Evento de alocação");

export const rotularRelacaoEventoAlocacao = (relacao) => ({
  ORIGEM: "Origem",
  SUBSTITUI: "Substitui",
  RETIFICA: "Retifica",
  CANCELA: "Cancela",
  AJUSTA: "Ajusta",
}[relacao] || relacao || "—");

export const ehProgramacaoFuturaCancelavel = (alocacao, hoje) => Boolean(
  alocacao?.estadoNegocial === "REGISTRADA" && alocacao?.dataInicio && hoje && hoje < alocacao.dataInicio,
);
