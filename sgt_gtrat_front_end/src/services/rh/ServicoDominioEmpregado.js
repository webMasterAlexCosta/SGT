export const SEM_CONTRATO = "sem_contrato";

export const normalizarTextoBusca = (valor = "") =>
  String(valor || "").trim().replace(/\s+/g, " ").slice(0, 80);

export const normalizarIdsFiltro = (ids = []) =>
  [
    ...new Set(
      (Array.isArray(ids) ? ids : [])
        .filter((id) => id != null && id !== "")
        .map(String)
        .filter(Boolean),
    ),
  ];

/**
 * Normaliza a consulta da tela de empregados.
 *
 * A função aceita tanto o estado visual (`contratos`) quanto uma consulta já
 * normalizada (`contratoIds`). Isso é intencional: o hook normaliza a consulta
 * no useMemo e a valida novamente antes de chamar o serviço.
 */
export const criarConsultaEmpregados = ({
  pagina = 0,
  tamanho = 50,
  busca = "",
  lotacaoId = "",
  turnoId = "",
  contratos,
  contratoIds,
  incluirSemContrato = false,
} = {}) => {
  const origemContratos = contratos ?? contratoIds ?? [];
  const contratosNormalizados = normalizarIdsFiltro(origemContratos);
  const possuiMarcadorSemContrato = contratosNormalizados.includes(SEM_CONTRATO);
  const idsContratos = contratosNormalizados.filter((id) => id !== SEM_CONTRATO);

  return {
    pagina: Math.max(0, Number(pagina) || 0),
    tamanho: Math.max(0, Number(tamanho) || 0),
    busca: normalizarTextoBusca(busca),
    lotacaoId: lotacaoId ? String(lotacaoId) : "",
    turnoId: turnoId ? String(turnoId) : "",
    contratoIds: idsContratos,
    incluirSemContrato: Boolean(incluirSemContrato || possuiMarcadorSemContrato),
  };
};

export const consultaPossuiFiltro = (consulta = {}) =>
  Boolean(
    normalizarTextoBusca(consulta.busca) ||
      consulta.lotacaoId ||
      consulta.turnoId ||
      (consulta.contratoIds && consulta.contratoIds.length > 0) ||
      consulta.incluirSemContrato,
  );


export const normalizarContratoUnico = (valor) => {
  if (valor == null || valor === "") return "";
  if (Array.isArray(valor)) {
    const primeiro = valor.find((item) => item != null && item !== "");
    return primeiro == null ? "" : String(primeiro?.id ?? primeiro);
  }
  return String(valor?.id ?? valor);
};

export const contratoUnicoParaPayload = (contratoId) => {
  const id = normalizarContratoUnico(contratoId);
  if (!id) return [];
  const idNumerico = Number(id);
  return Number.isSafeInteger(idNumerico) && idNumerico > 0
    ? [{ id: idNumerico }]
    : [];
};

export const validarQuantidadeContratos = (contratos = []) => {
  const ids = new Set(
    (Array.isArray(contratos) ? contratos : [])
      .map((contrato) => contrato?.id ?? contrato)
      .filter((id) => id != null && id !== "")
      .map(String),
  );
  return ids.size <= 1;
};


const texto = (valor, fallback = "-") => {
  const normalizado = String(valor ?? "").trim();
  return normalizado || fallback;
};

export const normalizarItemHistoricoContrato = (item = {}) => {
  const numeroContrato = item.numeroContrato;
  const descricaoContrato = item.descricaoContrato;
  const nomeContrato = item.nomeContrato ?? descricaoContrato;

  return {
    id: item.id,
    empregadoId: item.empregadoId,
    empregadoNome: item.empregadoNome,
    contratoId: item.contratoId,
    numeroContrato: texto(numeroContrato),
    nomeContrato: texto(nomeContrato),
    descricaoContrato: texto(descricaoContrato),
    tipoContrato: texto(item.tipoContrato),
    contratoDataInicio: item.contratoDataInicio ?? null,
    contratoDataFim: item.contratoDataFim ?? null,
    contratoAtivo: item.contratoAtivo ?? false,
    efetivoPrevisto: item.efetivoPrevisto ?? null,
    efetivoAtual: item.efetivoAtual ?? null,
    saldoEfetivo: item.saldoEfetivo ?? null,
    percentualOcupacao: item.percentualOcupacao ?? null,
    diaSemanaReduzido: item.diaSemanaReduzido ?? null,
    horasDiaReduzido: item.horasDiaReduzido ?? null,
    gestaoLocalId: item.gestaoLocalId,
    gestaoLocalNome: texto(item.gestaoLocalNome),
    superintendenciaId: item.superintendenciaId,
    superintendenciaNome: texto(item.superintendenciaNome),
    uf: texto(item.uf),
    dataInicio: item.dataInicio ?? null,
    dataFim: item.dataFim ?? null,
    observacao: texto(item.observacao, "Sem observações"),
    ativo: Boolean(item.ativo ?? !item.dataFim),
  };
};

export const normalizarHistoricoContrato = (resposta) =>
  (Array.isArray(resposta) ? resposta : resposta?.content || []).map(
    normalizarItemHistoricoContrato,
  );
