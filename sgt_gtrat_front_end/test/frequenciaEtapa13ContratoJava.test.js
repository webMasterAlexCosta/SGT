import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const backend = process.env.SGT_BACKEND_DIR;
const lerFront = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const lerBack = (caminho) => readFile(resolve(backend, caminho), "utf8");

test("Etapa 13 integra os seis endpoints de frequência e a projeção operacional previstos no Java", async () => {
  const [servico, controladorFrequencia, controladorEmpregado] = await Promise.all([
    lerFront("src/services/rh/ServicoFrequencia.js"),
    lerBack("src/main/java/com/example/siggtrat/controlador/FrequenciaControlador.java"),
    lerBack("src/main/java/com/example/siggtrat/controlador/EmpregadoControlador.java"),
  ]);
  for (const rota of ["@GetMapping", '@GetMapping("/mensal")', '@GetMapping("/intervalo")', '@GetMapping("/{id}/eventos")', "@PostMapping", '@PostMapping("/lote")']) {
    assert.match(controladorFrequencia, new RegExp(rota.replace(/[{}()]/g, ".")));
  }
  assert.match(controladorEmpregado, /@GetMapping\("\/filtrados-frequencia"\)/);
  assert.match(controladorEmpregado, /List<Long> contratoIds/);
  assert.match(controladorEmpregado, /String searchTerm/);
  for (const trecho of ["/frequencias", "/frequencias/lote", "/eventos"]) assert.match(servico, new RegExp(trecho.replace("/", "\\/")));
});

test("projeção não expõe vinculoContratualId, então o React o resolve pelos endpoints existentes do Java sem fallback para empregado.id", async () => {
  const [dto, logica, tabela, controladorVinculo, servicoVinculo] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/dto/EmpregadoFrequenciaDTO.java"),
    lerFront("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"),
    lerFront("src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.jsx"),
    lerBack("src/main/java/com/example/siggtrat/controlador/VinculoContratualControlador.java"),
    lerFront("src/services/rh/ServicoVinculoContratual.js"),
  ]);
  assert.doesNotMatch(dto, /vinculoContratualId/);
  assert.match(controladorVinculo, /@RequestParam\(required = false\) LocalDate ativoEm/);
  assert.match(controladorVinculo, /@GetMapping\("\/{id}\/alocacoes\/posicao"\)/);
  assert.match(logica, /resolverVinculoAtivoParaFrequencia/);
  assert.match(servicoVinculo, /ativoEm:\s*data/);
  assert.match(servicoVinculo, /consultarPosicaoAlocacao/);
  assert.match(tabela, /sem usar <b>empregado\.id<\/b>/);
});

test("POST público exige identidade canônica e o payload React nunca envia empregado como identidade", async () => {
  const [servicoJava, dominioReact] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/servico/FrequenciaServico.java"),
    lerFront("src/services/rh/ServicoDominioFrequencia.js"),
  ]);
  assert.match(servicoJava, /vinculoContratualId é obrigatório no contrato público de Frequência/);
  assert.match(servicoJava, /empregado\.id é apenas projeção interna/);
  assert.match(dominioReact, /vinculoContratualId:\s*Number\(linha\.vinculoContratualId\)/);
  const trechoPayload = dominioReact.slice(dominioReact.indexOf("export function montarPayloadFrequenciaCanonica"), dominioReact.indexOf("export function dividirEmLotesFrequencia"));
  assert.doesNotMatch(trechoPayload, /empregado\s*:/);
});

test("limite de lote do React coincide com o limite padrão de 1000 do Java", async () => {
  const [properties, dominioReact] = await Promise.all([
    lerBack("src/main/resources/application.properties"),
    lerFront("src/services/rh/ServicoDominioFrequencia.js"),
  ]);
  assert.match(properties, /frequencia\.max-batch-size=\$\{FREQUENCIA_MAX_BATCH_SIZE:1000\}/);
  assert.match(dominioReact, /TAMANHO_MAXIMO_LOTE_FREQUENCIA\s*=\s*1000/);
  assert.match(dominioReact, /Math\.min\(TAMANHO_MAXIMO_LOTE_FREQUENCIA/);
});

test("retificação gerencial replica apenas pré-condições de formato e deixa a decisão negocial no Java", async () => {
  const [servicoJava, dominioReact, filtros] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/servico/FrequenciaServico.java"),
    lerFront("src/services/rh/ServicoDominioFrequencia.js"),
    lerFront("src/modules/rh/pages/PaginaFrequencia/components/FiltrosFrequencia.jsx"),
  ]);
  assert.match(servicoJava, /justificativa com ao menos 5 caracteres/);
  assert.match(servicoJava, /chave de idempotência com ao menos 8 caracteres/);
  assert.match(dominioReact, /justificativa\.length < 5/);
  assert.match(dominioReact, /const chave = String\(chaveIdempotencia \|\| ""\)\.trim\(\)/);
  assert.match(dominioReact, /chave\.length < 8/);
  assert.match(filtros, /backend exige ao menos 5 caracteres/);
});

test("estados oficiais de validade, auditoria e bloqueio são projetados do Java para a grade", async () => {
  const [dto, dominioReact, logica] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/dto/FrequenciaDTO.java"),
    lerFront("src/services/rh/ServicoDominioFrequencia.js"),
    lerFront("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"),
  ]);
  for (const campo of ["estadoNegocial", "validadeOperacional", "estadoAuditoria", "bloqueadaEdicaoOperacional", "versaoNegocial"]) {
    assert.match(dto, new RegExp(campo));
    assert.match(logica, new RegExp(campo));
  }
  assert.match(dominioReact, /SUJEITA_REVISAO/);
  assert.match(dominioReact, /estadoNegocial === "FECHADA"/);
});

test("janela de edição permanece autoritativa no Java e prazoEdicao é somente informativo no React", async () => {
  const [servicoJava, logica] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/servico/FrequenciaServico.java"),
    lerFront("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"),
  ]);
  assert.match(servicoJava, /politicaTemporal\.abertaParaOperacional/);
  assert.match(servicoJava, /A frequência está FECHADA; alteração posterior exige retificação do Gestor Local/);
  assert.match(logica, /prazoEdicao é somente informativo no React/);
  assert.doesNotMatch(logica, /validarSeEstaNaJanelaDeSeguranca/);
});

test("data de referência é enviada sem deslocamento, preservando a data inicial do turno noturno", async () => {
  const [dto, dominioReact, logica] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/dto/FrequenciaDTO.java"),
    lerFront("src/services/rh/ServicoDominioFrequencia.js"),
    lerFront("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"),
  ]);
  assert.match(dto, /LocalDate dataReferencia/);
  assert.match(dominioReact, /dataReferencia,/);
  assert.match(logica, /dataReferencia:\s*dataReferenciaAtiva/);
  assert.doesNotMatch(dominioReact, /setDate\(|addDays|plusDays/);
});

test("lote Java é transacional por chamada e React não faz retry cego, preservando progresso entre chunks", async () => {
  const [servicoJava, servicoReact, dominioReact] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/servico/FrequenciaServico.java"),
    lerFront("src/services/rh/ServicoFrequencia.js"),
    lerFront("src/services/rh/ServicoDominioFrequencia.js"),
  ]);
  assert.match(servicoJava, /@Service\s*\n@Transactional\s*\npublic class FrequenciaServico/);
  assert.match(servicoJava, /Lote transacional e limitado/);
  assert.match(servicoReact, /registrarLote:[^\n]+retries:\s*0/);
  assert.match(servicoReact, /registrar:[^\n]+retries:\s*0/);
  assert.match(dominioReact, /erro\.progressoFrequencia/);
  assert.doesNotMatch(dominioReact.slice(dominioReact.indexOf("export async function executarLotesFrequenciaSequencial"), dominioReact.indexOf("export function montarAlteracaoFechamento")), /retry|tentativa/i);
});
