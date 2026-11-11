import type { ConsultaVinculosElegiveisFrequencia } from '../../contracts/pessoas-vinculos';
import type * as Input from '../../contracts/java-requests';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { extrairLista, extrairPagina, requisitar } from "../../core/http.ts";
import { limparParametrosPessoaVinculo } from "./ServicoDominioPessoaVinculo.ts";
import { limparParametrosAlocacao } from "./ServicoDominioAlocacaoVinculo.ts";

const endpoint = "/vinculos-contratuais";
const confirmacao = (acao: string, entidade: (string), consequencia: string) => ({ acao, entidade, consequencia });

const comando = <T,>(url: string, data: unknown, options: RequestOptions, successMessage: string, descricao: RequestOptions['confirmacaoSenha']): Promise<T> => requisitar<T>({
  method: "POST",
  url,
  ...(data !== undefined ? { data } : {}),
  retries: 0,
  successMessage,
  confirmacaoSenha: descricao,
  ...options,
});


const idCanonicoValido = (valor: (unknown) | (number | null)) => (typeof valor === "number" || typeof valor === "string")
  && Number.isSafeInteger(Number(valor)) && Number(valor) > 0;

async function resolverVinculoAtivoParaFrequencia(linha: { vinculoContratualId?: unknown; empregadoId?: unknown; lotacaoId?: unknown; turnoId?: unknown }, { data, contratoIds = [], consultarAssociacaoPorEmpregado = false }: { data?: string | null; contratoIds?: (string | number)[]; consultarAssociacaoPorEmpregado?: boolean } = {}, options: RequestOptions = {}) {
  if (!linha || !data) throw new TypeError("Linha e data são obrigatórias para resolver o vínculo da frequência.");
  const informado = linha.vinculoContratualId;
  if (idCanonicoValido(informado)) return Number(informado);

  // A projeção da grade do Java congelado não expõe a associação Empregado →
  // Pessoa/Vínculo. Busca por texto e posição de alocação não comprovam essa
  // associação, mesmo com resultado único. O POST público exige o ID explícito.
  if (!consultarAssociacaoPorEmpregado || !idCanonicoValido(linha.empregadoId)
    || !idCanonicoValido(linha.lotacaoId) || !idCanonicoValido(linha.turnoId)) {
    throw new Error("O servidor não informou a associação canônica desta linha ao vínculo contratual. A gravação está bloqueada até que essa associação seja disponibilizada por uma consulta autorizada.");
  }

  // Este GET é autorizado ao Gestor Local, mas vedado ao Operacional. O próprio
  // Java associa empregadoId + data a vinculoCanonicoId por Pessoa, nunca por
  // nome/CPF/matrícula. O chamador só o habilita para o perfil autorizado.
  const empregadoId = Number(linha.empregadoId);
  const historicos = extrairLista(await requisitar<Array<DTO.HistoricoVinculoContratoDTO>>({
    method: "GET", url: `/empregados/${empregadoId}/historico-contratos`,
    params: { data }, ...options, retries: 0,
  }));
  if (historicos.some((h) => Number(h?.empregadoId) !== empregadoId || h?.dataReferenciaIdentidade !== data)) {
    throw new Error("A associação retornada pelo servidor não corresponde ao colaborador e à data da linha.");
  }
  const contratos = [...new Set(contratoIds.filter(idCanonicoValido).map(Number))];
  const candidatos = [...new Set(historicos
    .filter((h) => idCanonicoValido(h?.vinculoCanonicoId) && (!contratos.length || contratos.includes(Number(h?.contratoId))))
    .map((h) => Number(h.vinculoCanonicoId)))];
  if (!candidatos.length) throw new Error("O histórico autorizado não informou associação canônica ao vínculo para esta linha, data e contrato. Regularize a associação antes de gravar.");

  const compativeis: number[] = [];
  for (const vinculoId of candidatos) {
    const posicao = await ServicoVinculoContratual.consultarPosicaoAlocacao(vinculoId, { data }, { ...options, retries: 0 });
    if (Number(posicao?.vinculoId) !== vinculoId || posicao?.dataReferencia !== data) {
      throw new Error("A posição de alocação retornada não corresponde ao vínculo e à data consultados.");
    }
    if (posicao.situacaoTemporal === "VIGENTE" && idCanonicoValido(posicao.alocacaoId)
      && Number(posicao.lotacaoId) === Number(linha.lotacaoId) && Number(posicao.jornadaId) === Number(linha.turnoId)) {
      compativeis.push(vinculoId);
    }
  }
  if (compativeis.length === 1) return compativeis[0];
  if (!compativeis.length) throw new Error("Nenhuma associação canônica possui alocação vigente na lotação e no turno desta linha.");
  throw new Error("A associação canônica é ambígua: mais de um vínculo foi retornado para a mesma linha, data, lotação e turno. A gravação foi bloqueada.");
}

export const ServicoVinculoContratual = Object.freeze({
  resolverVinculoAtivoParaFrequencia,
  listar: async (parametros: QueryParams = {}, options: RequestOptions = {}) => extrairPagina(await requisitar<Page<DTO.VinculoContratualDTO>>({
    method: "GET",
    url: endpoint,
    params: limparParametrosPessoaVinculo(parametros),
    retries: 1,
    ...options,
  })),

  listarElegiveisParaFrequencia: async (parametros: ConsultaVinculosElegiveisFrequencia, options: RequestOptions = {}) => extrairPagina(await requisitar<Page<DTO.VinculoElegivelFrequenciaDTO>>({
    method: "GET",
    url: `${endpoint}/elegiveis-frequencia`,
    params: limparParametrosPessoaVinculo(parametros),
    retries: 0,
    ...options,
  })),

  buscarPorId: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.VinculoContratualDTO>({ method: "GET", url: `${endpoint}/${id}`, ...options }); },

  listarEventos: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return extrairLista(await requisitar<Array<DTO.EventoVinculoContratualDTO>>({
    method: "GET", url: `${endpoint}/${id}/eventos`, ...options,
  })); },

  consultarPosicaoPessoa: async (pessoaId: ResourceIdentifierInput, { data, conhecidoAte }: { data?: string | null; conhecidoAte?: string } = {}, options: RequestOptions = {}) => { pessoaId = exigirIdentificadorRota(pessoaId); return requisitar<DTO.PosicaoPessoaDTO>({
    method: "GET",
    url: `${endpoint}/pessoas/${pessoaId}/posicao`,
    params: limparParametrosPessoaVinculo({ data, conhecidoAte }),
    ...options,
  }); },

  obterOpcoesAlocacaoInicial: async (contratoId: ResourceIdentifierInput, data: (string), options: RequestOptions = {}) => { contratoId = exigirIdentificadorRota(contratoId); return extrairLista(await requisitar<Array<DTO.OpcaoAlocacaoTemporalDTO>>({
    method: "GET",
    url: `${endpoint}/contratos/${contratoId}/opcoes-alocacao`,
    params: { data },
    retries: 0,
    ...options,
  })); },

  listarAlocacoes: async (id: ResourceIdentifierInput, { mes }: { mes?: string } = {}, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return extrairLista(await requisitar<Array<DTO.AlocacaoVinculoDTO>>({
    method: "GET",
    url: `${endpoint}/${id}/alocacoes`,
    params: limparParametrosAlocacao({ mes }),
    retries: 1,
    ...options,
  })); },

  listarEventosAlocacao: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return extrairLista(await requisitar<Array<DTO.EventoAlocacaoVinculoDTO>>({
    method: "GET", url: `${endpoint}/${id}/alocacoes/eventos`, retries: 1, ...options,
  })); },

  consultarPosicaoAlocacao: async (id: ResourceIdentifierInput, { data, conhecidoAte }: { data?: string | null; conhecidoAte?: string } = {}, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PosicaoAlocacaoTemporalDTO>({
    method: "GET",
    url: `${endpoint}/${id}/alocacoes/posicao`,
    params: limparParametrosAlocacao({ data, conhecidoAte }),
    retries: 1,
    ...options,
  }); },

  obterOpcoesAlocacao: async (contratoId: ResourceIdentifierInput, { data, lotacaoId, jornadaId }: { data?: string | null; lotacaoId?: number | string | null; jornadaId?: number | string | null } = {}, options: RequestOptions = {}) => { contratoId = exigirIdentificadorRota(contratoId); return extrairLista(await requisitar<Array<DTO.OpcaoAlocacaoTemporalDTO>>({
    method: "GET",
    url: `${endpoint}/contratos/${contratoId}/opcoes-alocacao`,
    params: limparParametrosAlocacao({ data, lotacaoId, jornadaId }),
    retries: 1,
    ...options,
  })); },

  gerarPreviaAlocacao: async (id: ResourceIdentifierInput, dados: Input.RequestAlteracaoAlocacaoVinculoRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PreviaImpactoAlocacaoDTO>({
    method: "POST", url: `${endpoint}/${id}/previas/alocacoes`, data: dados, retries: 0, ...options,
  }); },

  alterarAlocacao: async (id: ResourceIdentifierInput, dados: Input.RequestAlteracaoAlocacaoVinculoRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return comando<DTO.AlocacaoVinculoDTO>(
    `${endpoint}/${id}/alocacoes`, dados, options, "Nova faixa de alocação registrada.",
    confirmacao("Alterar alocação", `Vínculo ${id}`, "A linha temporal será revalidada e frequências/relatórios impactados poderão ser reprocessados."),
  ); },

  retificarAlocacao: async (id: ResourceIdentifierInput, alocacaoId: ResourceIdentifierInput, dados: Input.RequestAlteracaoAlocacaoVinculoRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); alocacaoId = exigirIdentificadorRota(alocacaoId); return comando<DTO.AlocacaoVinculoDTO>(
    `${endpoint}/${id}/alocacoes/${alocacaoId}/retificar`, dados, options, "Faixa de alocação retificada.",
    confirmacao("Retificar alocação", `Alocação ${alocacaoId}`, "A versão anterior permanecerá auditável; a combinação efetiva será revalidada pelo servidor."),
  ); },

  cancelarProgramacaoAlocacao: async (id: ResourceIdentifierInput, alocacaoId: ResourceIdentifierInput, dados: Input.RequestAcaoVinculoContratualRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); alocacaoId = exigirIdentificadorRota(alocacaoId); return comando<DTO.AlocacaoVinculoDTO>(
    `${endpoint}/${id}/alocacoes/${alocacaoId}/cancelar-programacao`, dados, options, "Programação futura de alocação cancelada.",
    confirmacao("Cancelar programação de alocação", `Alocação ${alocacaoId}`, "A faixa predecessora será restaurada pelo servidor quando aplicável, preservando a cadeia de eventos."),
  ); },

  cadastrar: async (dados: Input.RequestCadastroVinculoContratualRequisicao, options: RequestOptions = {}) => comando<DTO.VinculoContratualDTO>(
    endpoint,
    dados,
    options,
    "Pessoa, vínculo e alocação inicial registrados.",
    confirmacao("Cadastrar vínculo contratual", "Pessoa e vínculo", "A operação será transacional e reservará a matrícula e a capacidade informadas."),
  ),

  gerarPreviaRetificacaoInicio: async (id: ResourceIdentifierInput, dados: Input.RequestRetificacaoInicioVinculoRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PreviaImpactoVinculoDTO>({
    method: "POST", url: `${endpoint}/${id}/previas/retificacao-inicio`, data: dados, retries: 0, ...options,
  }); },

  retificarInicio: async (id: ResourceIdentifierInput, dados: Input.RequestRetificacaoInicioVinculoRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return comando<DTO.VinculoContratualDTO>(
    `${endpoint}/${id}/retificar-inicio`, dados, options, "Início do vínculo retificado.",
    confirmacao("Retificar início do vínculo", `Vínculo ${id}`, "A prévia será revalidada e os fatos dependentes poderão ser recalculados."),
  ); },

  alterarMatricula: async (id: ResourceIdentifierInput, dados: Input.RequestAlteracaoMatriculaVinculoRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return comando<DTO.VinculoContratualDTO>(
    `${endpoint}/${id}/alterar-matricula`, dados, options, "Matrícula do vínculo alterada.",
    confirmacao("Alterar matrícula", `Vínculo ${id}`, "A matrícula anterior permanecerá reservada no histórico deste contrato."),
  ); },

  cancelarProgramacao: async (id: ResourceIdentifierInput, dados: Input.RequestAcaoVinculoContratualRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return comando<DTO.VinculoContratualDTO>(
    `${endpoint}/${id}/cancelar-programacao`, dados, options, "Programação futura cancelada.",
    confirmacao("Cancelar vínculo programado", `Vínculo ${id}`, "O vínculo futuro será cancelado sem ser tratado como desligamento realizado."),
  ); },

  desligar: async (id: ResourceIdentifierInput, dados: Input.RequestDesligamentoVinculoRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return comando<DTO.VinculoContratualDTO>(
    `${endpoint}/${id}/desligar`, dados, options, "Desligamento do vínculo registrado.",
    confirmacao("Registrar desligamento", `Vínculo ${id}`, "A data informada será o último dia do vínculo e a trilha histórica será preservada."),
  ); },

  gerarPreviaRetificacaoDesligamento: async (id: ResourceIdentifierInput, dados: Input.RequestDesligamentoVinculoRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PreviaImpactoVinculoDTO>({
    method: "POST", url: `${endpoint}/${id}/previas/retificacao-desligamento`, data: dados, retries: 0, ...options,
  }); },

  retificarDesligamento: async (id: ResourceIdentifierInput, dados: Input.RequestDesligamentoVinculoRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return comando<DTO.VinculoContratualDTO>(
    `${endpoint}/${id}/retificar-desligamento`, dados, options, "Desligamento do vínculo retificado.",
    confirmacao("Retificar desligamento", `Vínculo ${id}`, "A prévia será revalidada e o fato anterior continuará auditável."),
  ); },

  cancelarDesligamentoProgramado: async (id: ResourceIdentifierInput, dados: Input.RequestAcaoVinculoContratualRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return comando<DTO.VinculoContratualDTO>(
    `${endpoint}/${id}/cancelar-desligamento-programado`, dados, options, "Desligamento programado cancelado.",
    confirmacao("Cancelar desligamento programado", `Vínculo ${id}`, "A programação será cancelada e o evento original continuará no histórico."),
  ); },

  tratarFimNatural: async (contratoId: ResourceIdentifierInput, options: RequestOptions = {}) => { contratoId = exigirIdentificadorRota(contratoId); return comando<Array<DTO.VinculoContratualDTO>>(
    `${endpoint}/contratos/${contratoId}/tratar-fim-natural`, undefined, options, "Fim natural dos vínculos tratado.",
    confirmacao("Tratar fim natural", `Contrato ${contratoId}`, "Vínculos ainda abertos serão encerrados na data final do contrato, conforme decisão do servidor."),
  ); },
});
