import { extrairLista, extrairPagina, requisitar } from "../../core/http.js";
import { limparParametrosPessoaVinculo } from "./ServicoDominioPessoaVinculo.js";
import { limparParametrosAlocacao } from "./ServicoDominioAlocacaoVinculo.js";

const endpoint = "/vinculos-contratuais";
const confirmacao = (acao, entidade, consequencia) => ({ acao, entidade, consequencia });

const comando = (url, data, options, successMessage, descricao) => requisitar({
  method: "POST",
  url,
  ...(data !== undefined ? { data } : {}),
  retries: 0,
  successMessage,
  confirmacaoSenha: descricao,
  ...options,
});


const normalizarComparavel = (valor) => String(valor ?? "")
  .normalize("NFD")
  .replace(/[\u0300-\u036f]/g, "")
  .replace(/\s+/g, " ")
  .trim()
  .toLocaleLowerCase("pt-BR");

const apenasDigitos = (valor) => String(valor ?? "").replace(/\D/g, "");
const valorSemMascara = (valor) => valor != null && !/[\*•]/.test(String(valor));

async function listarTodosVinculosAtivosPorBusca(parametros, options = {}) {
  const tamanho = 100;
  const acumulado = [];
  let pagina = 0;
  let totalPaginas = 1;
  do {
    const resposta = await ServicoVinculoContratual.listar({ ...parametros, page: pagina, size: tamanho }, options);
    const conteudo = Array.isArray(resposta?.content) ? resposta.content : [];
    acumulado.push(...conteudo);
    totalPaginas = Number(resposta?.totalPages ?? (conteudo.length < tamanho ? pagina + 1 : pagina + 2));
    pagina += 1;
  } while (pagina < totalPaginas);
  return acumulado;
}

async function resolverVinculoAtivoParaFrequencia(linha, { data, contratoIds = [] } = {}, options = {}) {
  if (!linha || !data) throw new TypeError("Linha e data são obrigatórias para resolver o vínculo da frequência.");
  if (Number(linha.vinculoContratualId) > 0) return Number(linha.vinculoContratualId);

  const matricula = String(linha.matricula ?? "").trim();
  const cpfDigitos = apenasDigitos(linha.cpf);
  const nome = String(linha.nome ?? "").trim();
  const busca = valorSemMascara(matricula) && matricula ? matricula : (cpfDigitos.length === 11 ? cpfDigitos : nome);
  if (!busca) throw new Error("Não há matrícula, CPF integral ou nome disponível para resolver o vínculo canônico.");

  const contratos = [...new Set((contratoIds || []).map(Number).filter((id) => Number.isFinite(id) && id > 0))];
  const parametros = { ativoEm: data, busca };
  if (contratos.length === 1) parametros.contratoId = contratos[0];

  let candidatos = await listarTodosVinculosAtivosPorBusca(parametros, options);
  if (contratos.length > 1) candidatos = candidatos.filter((v) => contratos.includes(Number(v?.contratoId)));

  if (matricula && valorSemMascara(matricula)) {
    const alvo = normalizarComparavel(matricula);
    const exatos = candidatos.filter((v) => normalizarComparavel(v?.matricula) === alvo);
    if (exatos.length) candidatos = exatos;
  } else if (cpfDigitos.length === 11) {
    const exatos = candidatos.filter((v) => apenasDigitos(v?.pessoaCpf) === cpfDigitos);
    if (exatos.length) candidatos = exatos;
  } else if (nome) {
    const alvo = normalizarComparavel(nome);
    const exatos = candidatos.filter((v) => normalizarComparavel(v?.pessoaNome) === alvo);
    if (exatos.length) candidatos = exatos;
  }

  if (candidatos.length === 1) return Number(candidatos[0].id);
  if (candidatos.length === 0) throw new Error(`Nenhum vínculo contratual ativo foi encontrado em ${data} para ${nome || matricula || "a linha selecionada"}.`);

  const lotacaoId = Number(linha.lotacaoId);
  const jornadaId = Number(linha.turnoId);
  const compativeis = [];
  for (const candidato of candidatos) {
    const posicao = await ServicoVinculoContratual.consultarPosicaoAlocacao(candidato.id, { data }, { ...options, retries: 0 });
    const mesmaLotacao = !Number.isFinite(lotacaoId) || Number(posicao?.lotacaoId) === lotacaoId;
    const mesmaJornada = !Number.isFinite(jornadaId) || Number(posicao?.jornadaId) === jornadaId;
    if (mesmaLotacao && mesmaJornada) compativeis.push(candidato);
  }
  if (compativeis.length === 1) return Number(compativeis[0].id);
  throw new Error(`O Java retornou ${compativeis.length || candidatos.length} vínculos compatíveis para a linha. A gravação foi interrompida para não escolher uma identidade canônica por inferência ambígua.`);
}

export const ServicoVinculoContratual = Object.freeze({
  resolverVinculoAtivoParaFrequencia,
  listar: async (parametros = {}, options = {}) => extrairPagina(await requisitar({
    method: "GET",
    url: endpoint,
    params: limparParametrosPessoaVinculo(parametros),
    retries: 1,
    ...options,
  })),

  buscarPorId: async (id, options = {}) => requisitar({ method: "GET", url: `${endpoint}/${id}`, ...options }),

  listarEventos: async (id, options = {}) => extrairLista(await requisitar({
    method: "GET", url: `${endpoint}/${id}/eventos`, ...options,
  })),

  consultarPosicaoPessoa: async (pessoaId, { data, conhecidoAte } = {}, options = {}) => requisitar({
    method: "GET",
    url: `${endpoint}/pessoas/${pessoaId}/posicao`,
    params: limparParametrosPessoaVinculo({ data, conhecidoAte }),
    ...options,
  }),

  obterOpcoesAlocacaoInicial: async (contratoId, data, options = {}) => extrairLista(await requisitar({
    method: "GET",
    url: `${endpoint}/contratos/${contratoId}/opcoes-alocacao`,
    params: { data },
    retries: 0,
    ...options,
  })),

  listarAlocacoes: async (id, { mes } = {}, options = {}) => extrairLista(await requisitar({
    method: "GET",
    url: `${endpoint}/${id}/alocacoes`,
    params: limparParametrosAlocacao({ mes }),
    retries: 1,
    ...options,
  })),

  listarEventosAlocacao: async (id, options = {}) => extrairLista(await requisitar({
    method: "GET", url: `${endpoint}/${id}/alocacoes/eventos`, retries: 1, ...options,
  })),

  consultarPosicaoAlocacao: async (id, { data, conhecidoAte } = {}, options = {}) => requisitar({
    method: "GET",
    url: `${endpoint}/${id}/alocacoes/posicao`,
    params: limparParametrosAlocacao({ data, conhecidoAte }),
    retries: 1,
    ...options,
  }),

  obterOpcoesAlocacao: async (contratoId, { data, lotacaoId, jornadaId } = {}, options = {}) => extrairLista(await requisitar({
    method: "GET",
    url: `${endpoint}/contratos/${contratoId}/opcoes-alocacao`,
    params: limparParametrosAlocacao({ data, lotacaoId, jornadaId }),
    retries: 1,
    ...options,
  })),

  gerarPreviaAlocacao: async (id, dados, options = {}) => requisitar({
    method: "POST", url: `${endpoint}/${id}/previas/alocacoes`, data: dados, retries: 0, ...options,
  }),

  alterarAlocacao: async (id, dados, options = {}) => comando(
    `${endpoint}/${id}/alocacoes`, dados, options, "Nova faixa de alocação registrada.",
    confirmacao("Alterar alocação", `Vínculo ${id}`, "A linha temporal será revalidada e frequências/relatórios impactados poderão ser reprocessados."),
  ),

  retificarAlocacao: async (id, alocacaoId, dados, options = {}) => comando(
    `${endpoint}/${id}/alocacoes/${alocacaoId}/retificar`, dados, options, "Faixa de alocação retificada.",
    confirmacao("Retificar alocação", `Alocação ${alocacaoId}`, "A versão anterior permanecerá auditável; a combinação efetiva será revalidada pelo servidor."),
  ),

  cancelarProgramacaoAlocacao: async (id, alocacaoId, dados, options = {}) => comando(
    `${endpoint}/${id}/alocacoes/${alocacaoId}/cancelar-programacao`, dados, options, "Programação futura de alocação cancelada.",
    confirmacao("Cancelar programação de alocação", `Alocação ${alocacaoId}`, "A faixa predecessora será restaurada pelo servidor quando aplicável, preservando a cadeia de eventos."),
  ),

  cadastrar: async (dados, options = {}) => comando(
    endpoint,
    dados,
    options,
    "Pessoa, vínculo e alocação inicial registrados.",
    confirmacao("Cadastrar vínculo contratual", "Pessoa e vínculo", "A operação será transacional e reservará a matrícula e a capacidade informadas."),
  ),

  gerarPreviaRetificacaoInicio: async (id, dados, options = {}) => requisitar({
    method: "POST", url: `${endpoint}/${id}/previas/retificacao-inicio`, data: dados, retries: 0, ...options,
  }),

  retificarInicio: async (id, dados, options = {}) => comando(
    `${endpoint}/${id}/retificar-inicio`, dados, options, "Início do vínculo retificado.",
    confirmacao("Retificar início do vínculo", `Vínculo ${id}`, "A prévia será revalidada e os fatos dependentes poderão ser recalculados."),
  ),

  alterarMatricula: async (id, dados, options = {}) => comando(
    `${endpoint}/${id}/alterar-matricula`, dados, options, "Matrícula do vínculo alterada.",
    confirmacao("Alterar matrícula", `Vínculo ${id}`, "A matrícula anterior permanecerá reservada no histórico deste contrato."),
  ),

  cancelarProgramacao: async (id, dados, options = {}) => comando(
    `${endpoint}/${id}/cancelar-programacao`, dados, options, "Programação futura cancelada.",
    confirmacao("Cancelar vínculo programado", `Vínculo ${id}`, "O vínculo futuro será cancelado sem ser tratado como desligamento realizado."),
  ),

  desligar: async (id, dados, options = {}) => comando(
    `${endpoint}/${id}/desligar`, dados, options, "Desligamento do vínculo registrado.",
    confirmacao("Registrar desligamento", `Vínculo ${id}`, "A data informada será o último dia do vínculo e a trilha histórica será preservada."),
  ),

  gerarPreviaRetificacaoDesligamento: async (id, dados, options = {}) => requisitar({
    method: "POST", url: `${endpoint}/${id}/previas/retificacao-desligamento`, data: dados, retries: 0, ...options,
  }),

  retificarDesligamento: async (id, dados, options = {}) => comando(
    `${endpoint}/${id}/retificar-desligamento`, dados, options, "Desligamento do vínculo retificado.",
    confirmacao("Retificar desligamento", `Vínculo ${id}`, "A prévia será revalidada e o fato anterior continuará auditável."),
  ),

  cancelarDesligamentoProgramado: async (id, dados, options = {}) => comando(
    `${endpoint}/${id}/cancelar-desligamento-programado`, dados, options, "Desligamento programado cancelado.",
    confirmacao("Cancelar desligamento programado", `Vínculo ${id}`, "A programação será cancelada e o evento original continuará no histórico."),
  ),

  tratarFimNatural: async (contratoId, options = {}) => comando(
    `${endpoint}/contratos/${contratoId}/tratar-fim-natural`, undefined, options, "Fim natural dos vínculos tratado.",
    confirmacao("Tratar fim natural", `Contrato ${contratoId}`, "Vínculos ainda abertos serão encerrados na data final do contrato, conforme decisão do servidor."),
  ),
});
