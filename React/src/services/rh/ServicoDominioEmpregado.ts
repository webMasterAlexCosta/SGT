import type { HistoricoVinculoContratoDTO } from '../../contracts/java';
import type { Page } from '../../contracts/shared';
type ContractIdentifier = string | number | { id?: string | number | null } | null | undefined;
const identifierValue = (item: ContractIdentifier) => item !== null && typeof item === 'object' ? item.id : item;
export const SEM_CONTRATO = "sem_contrato";

export const normalizarTextoBusca = (valor: unknown = "") =>
  String(valor || "").trim().replace(/\s+/g, " ").slice(0, 80);

export const normalizarIdsFiltro = (ids: unknown[] = []) =>
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
}: { pagina?: number; tamanho?: number; busca?: string; lotacaoId?: string | number; turnoId?: string | number; contratos?: unknown[]; contratoIds?: unknown[]; incluirSemContrato?: boolean } = {}) => {
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

export const consultaPossuiFiltro = (consulta: { busca?: unknown; lotacaoId?: unknown; turnoId?: unknown; contratoIds?: unknown[]; incluirSemContrato?: unknown } = {}) =>
  Boolean(
    normalizarTextoBusca(consulta.busca) ||
      consulta.lotacaoId ||
      consulta.turnoId ||
      (consulta.contratoIds && consulta.contratoIds.length > 0) ||
      consulta.incluirSemContrato,
  );


export const normalizarContratoUnico = (valor: ContractIdentifier | ContractIdentifier[]) => {
  if (valor == null || valor === "") return "";
  if (Array.isArray(valor)) {
    const primeiro = valor.find((item) => item != null && item !== "");
    return primeiro == null ? "" : String(identifierValue(primeiro) ?? primeiro);
  }
  return String(identifierValue(valor) ?? valor);
};

export const contratoUnicoParaPayload = (contratoId: (string)) => {
  const id = normalizarContratoUnico(contratoId);
  if (!id) return [];
  const idNumerico = Number(id);
  return Number.isSafeInteger(idNumerico) && idNumerico > 0
    ? [{ id: idNumerico }]
    : [];
};

export const validarQuantidadeContratos = (contratos: ContractIdentifier[] = []) => {
  const ids = new Set(
    (Array.isArray(contratos) ? contratos : [])
      .map((contrato) => identifierValue(contrato) ?? contrato)
      .filter((id) => id != null && id !== "")
      .map(String),
  );
  return ids.size <= 1;
};


const texto = (valor: unknown, fallback = "-") => {
  const normalizado = String(valor ?? "").trim();
  return normalizado || fallback;
};

export const normalizarItemHistoricoContrato = (item: Partial<HistoricoVinculoContratoDTO> = {}) => {
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

export const normalizarHistoricoContrato = (resposta: HistoricoVinculoContratoDTO[] | Partial<Page<HistoricoVinculoContratoDTO>> | null | undefined) =>
  (Array.isArray(resposta) ? resposta : resposta?.content || []).map(
    normalizarItemHistoricoContrato,
  );
