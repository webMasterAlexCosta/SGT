export const STATUS_FECHAMENTO_ATIVOS = Object.freeze(["PENDENTE", "PROCESSANDO"]);
export const STATUS_FECHAMENTO_TERMINAIS = Object.freeze(["CONCLUIDO", "ERRO", "CANCELADO"]);
export const TAMANHO_MAXIMO_LOTE_FREQUENCIA = 1000;

export function normalizarTextoDeBusca(valor) {
  return String(valor ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("pt-BR")
    .replace(/[^a-z0-9]+/g, " ")
    .trim();
}

export function apenasDigitosDeBusca(valor) {
  return String(valor ?? "").replace(/\D/g, "");
}

export function criarIndiceBusca(linha = {}) {
  return [
    normalizarTextoDeBusca(`${linha.nome || ""} ${linha.cpf || ""} ${linha.matricula || ""}`),
    apenasDigitosDeBusca(linha.cpf),
    apenasDigitosDeBusca(linha.matricula),
  ].filter(Boolean).join(" ");
}

export function filtrarFrequenciasPorTermo(lista = [], termo = "") {
  const termoNormalizado = normalizarTextoDeBusca(termo);
  if (!termoNormalizado) return lista;
  const partes = termoNormalizado.split(/\s+/).filter(Boolean);
  return lista.filter((linha) => {
    const indice = linha?.indiceBusca || criarIndiceBusca(linha);
    return partes.every((parte) => indice.includes(parte));
  });
}

export function agruparFaltasNoTopo(lista = []) {
  if (!Array.isArray(lista) || lista.length === 0) {
    return { listaOrdenada: [], totalFaltas: 0, totalAtestados: 0 };
  }

  const faltas = [];
  const atestados = [];
  const demais = [];

  for (const linha of lista) {
    const status = String(linha?.status || "").trim().toUpperCase();
    if (status === "FALTA") faltas.push(linha);
    else if (status === "ATESTADO") atestados.push(linha);
    else demais.push(linha);
  }

  return {
    listaOrdenada: faltas.concat(atestados, demais),
    totalFaltas: faltas.length,
    totalAtestados: atestados.length,
  };
}

export function normalizarStatusFrequencia(status) {
  return String(status || "").trim().toUpperCase();
}

export function possuiIdentidadeCanonicaFrequencia(item) {
  return Number.isFinite(Number(item?.vinculoContratualId)) && Number(item?.vinculoContratualId) > 0;
}

export function gerarChaveIdempotenciaFrequencia(prefixo = "freq") {
  const uuid = globalThis?.crypto?.randomUUID?.();
  if (uuid) return `${prefixo}-${uuid}`;
  const aleatorio = Math.random().toString(36).slice(2);
  return `${prefixo}-${Date.now().toString(36)}-${aleatorio}`;
}

export function frequenciaBloqueadaParaOperacional(item = {}) {
  if (!item?.jaSalvo) return false;
  const estadoNegocial = normalizarStatusFrequencia(item.estadoNegocial);
  const validade = normalizarStatusFrequencia(item.validadeOperacional);
  const auditoria = normalizarStatusFrequencia(item.estadoAuditoria);
  return item.bloqueadaEdicaoOperacional === true
    || estadoNegocial === "FECHADA"
    || (validade && validade !== "VALIDA")
    || auditoria === "SUJEITA_REVISAO";
}

export function descreverEstadoOficialFrequencia(item = {}) {
  if (!item?.jaSalvo) {
    return {
      lancamento: "PENDENTE",
      estadoNegocial: null,
      validadeOperacional: null,
      estadoAuditoria: null,
      versaoNegocial: null,
    };
  }
  return {
    lancamento: "LANCADO",
    estadoNegocial: normalizarStatusFrequencia(item.estadoNegocial) || null,
    validadeOperacional: normalizarStatusFrequencia(item.validadeOperacional) || null,
    estadoAuditoria: normalizarStatusFrequencia(item.estadoAuditoria) || null,
    versaoNegocial: item.versaoNegocial ?? null,
  };
}

export function montarPayloadFrequenciaCanonica({
  linha,
  dataReferencia,
  gestorLocal = false,
  justificativaRetificacao = "",
  chaveIdempotencia,
}) {
  if (!linha || !possuiIdentidadeCanonicaFrequencia(linha)) {
    throw new TypeError("vinculoContratualId é obrigatório para gravar frequência; empregado.id não é identidade de escrita.");
  }
  if (!dataReferencia) throw new TypeError("Data de referência obrigatória.");

  const status = normalizarStatusFrequencia(linha.status);
  if (!status) throw new TypeError("Status da frequência obrigatório.");

  const payload = {
    ...(linha.frequenciaId ? { id: Number(linha.frequenciaId) } : {}),
    vinculoContratualId: Number(linha.vinculoContratualId),
    dataReferencia,
    status,
    observacao: linha.obs ? String(linha.obs).trim() || null : null,
    entrada1: linha.entrada1 || null,
    saida1: linha.saida1 || null,
    entrada2: linha.entrada2 || null,
    saida2: linha.saida2 || null,
    horasTrabalhadas: linha.horasTrabalhadas
      ? String(linha.horasTrabalhadas).replace(",", ".")
      : null,
    horarioSaida: linha.horarioSaida || null,
    horasNoturnas: Number(linha.horasNoturnas || 0),
  };

  if (gestorLocal) {
    const justificativa = String(justificativaRetificacao || "").trim();
    if (justificativa.length < 5) {
      throw new TypeError("Gestor Local deve informar justificativa de retificação com ao menos 5 caracteres.");
    }
    const chave = String(chaveIdempotencia || "").trim();
    if (chave.length < 8) {
      throw new TypeError("Gestor Local deve utilizar chave de idempotência com ao menos 8 caracteres.");
    }
    payload.justificativaRetificacao = justificativa;
    payload.chaveIdempotencia = chave;
  }

  return payload;
}

export function dividirEmLotesFrequencia(itens = [], tamanho = TAMANHO_MAXIMO_LOTE_FREQUENCIA) {
  if (!Array.isArray(itens)) throw new TypeError("Itens de frequência devem ser uma lista.");
  const limite = Math.max(1, Math.min(TAMANHO_MAXIMO_LOTE_FREQUENCIA, Number(tamanho) || TAMANHO_MAXIMO_LOTE_FREQUENCIA));
  const lotes = [];
  for (let indice = 0; indice < itens.length; indice += limite) {
    lotes.push(itens.slice(indice, indice + limite));
  }
  return lotes;
}

export async function executarLotesFrequenciaSequencial({
  payloads = [],
  enviarIndividual,
  enviarLote,
  tamanhoLote = TAMANHO_MAXIMO_LOTE_FREQUENCIA,
  aoProgresso,
}) {
  if (!Array.isArray(payloads) || payloads.length === 0) {
    return { salvos: [], totalSalvo: 0, total: 0, lotesConcluidos: 0 };
  }
  if (typeof enviarIndividual !== "function" || typeof enviarLote !== "function") {
    throw new TypeError("Funções de envio individual e em lote são obrigatórias.");
  }

  if (payloads.length === 1) {
    const salvo = await enviarIndividual(payloads[0]);
    aoProgresso?.({ processados: 1, total: 1, loteAtual: 1, totalLotes: 1 });
    return { salvos: [salvo], totalSalvo: 1, total: 1, lotesConcluidos: 1 };
  }

  const lotes = dividirEmLotesFrequencia(payloads, tamanhoLote);
  const salvos = [];
  for (let indice = 0; indice < lotes.length; indice += 1) {
    try {
      const resposta = await enviarLote(lotes[indice]);
      if (Array.isArray(resposta)) salvos.push(...resposta);
      else if (resposta != null) salvos.push(resposta);
      aoProgresso?.({
        processados: salvos.length,
        total: payloads.length,
        loteAtual: indice + 1,
        totalLotes: lotes.length,
      });
    } catch (erro) {
      erro.progressoFrequencia = {
        totalSalvo: salvos.length,
        total: payloads.length,
        lotesConcluidos: indice,
        loteFalhou: indice + 1,
        totalLotes: lotes.length,
      };
      throw erro;
    }
  }
  return { salvos, totalSalvo: salvos.length, total: payloads.length, lotesConcluidos: lotes.length };
}

// Compatibilidade temporária com o fechamento da Etapa 14. A Etapa 13 não usa
// estas funções para gravar a grade diária, mas outros módulos ainda podem
// consultar o histórico de fechamentos enquanto a migração completa não termina.
export function montarAlteracaoFechamento(estrutura, dataReferencia) {
  if (!estrutura?.empregadoId) throw new TypeError("A alteração deve informar empregadoId.");
  return {
    empregado: { id: estrutura.empregadoId },
    dataReferencia,
    status: normalizarStatusFrequencia(estrutura.status),
    observacao: estrutura.obs ? estrutura.obs.trim() || null : null,
    entrada1: estrutura.entrada1 || null,
    saida1: estrutura.saida1 || null,
    entrada2: estrutura.entrada2 || null,
    saida2: estrutura.saida2 || null,
    horasTrabalhadas: estrutura.horasTrabalhadas ? String(estrutura.horasTrabalhadas).replace(",", ".") : null,
    horarioSaida: estrutura.horarioSaida || null,
    horasNoturnas: estrutura.horasNoturnas ?? 0,
  };
}

export function montarPayloadFechamento({ dataReferencia, lotacaoId, jornadaId, contratoIds = [], registrosPendentes = [] }) {
  if (!dataReferencia || !Number(lotacaoId) || !Number(jornadaId)) {
    throw new TypeError("Data, lotação e turno são obrigatórios.");
  }
  return {
    dataReferencia,
    lotacaoId: Number(lotacaoId),
    jornadaId: Number(jornadaId),
    contratoIds: [...new Set(contratoIds.map(Number).filter(Number.isFinite))].sort((a, b) => a - b),
    alteracoes: registrosPendentes.map((registro) => montarAlteracaoFechamento(registro, dataReferencia)),
  };
}

export function fechamentoEstaAtivo(fechamento) {
  return STATUS_FECHAMENTO_ATIVOS.includes(normalizarStatusFrequencia(fechamento?.status));
}

export function fechamentoTerminou(fechamento) {
  return STATUS_FECHAMENTO_TERMINAIS.includes(normalizarStatusFrequencia(fechamento?.status));
}

export function calcularPercentualFechamento(fechamento) {
  const informado = Number(fechamento?.percentual);
  if (Number.isFinite(informado)) return Math.max(0, Math.min(100, Math.round(informado)));
  const total = Number(fechamento?.totalPrevisto || 0);
  const processado = Number(fechamento?.totalProcessado || 0);
  if (total <= 0) return 0;
  return Math.max(0, Math.min(100, Math.floor((processado * 100) / total)));
}

export function calcularEsperaPolling({ tentativas = 0, falhasConsecutivas = 0 } = {}) {
  const base = falhasConsecutivas > 0
    ? 2000 * (2 ** Math.min(falhasConsecutivas - 1, 3))
    : 1000 + Math.min(Math.max(tentativas, 0), 8) * 250;
  return Math.min(15000, Math.max(1000, base));
}
