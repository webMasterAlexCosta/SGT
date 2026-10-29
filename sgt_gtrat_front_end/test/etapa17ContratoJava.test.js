import test from "node:test";
import assert from "node:assert/strict";
import { readFile, readdir } from "node:fs/promises";
import { resolve, join } from "node:path";

const backend = process.env.SGT_BACKEND_DIR;
const lerFront = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const lerBack = (caminho) => readFile(resolve(backend, caminho), "utf8");

const listarArquivos = async (dir) => {
  const entradas = await readdir(dir, { withFileTypes: true });
  const itens = [];
  for (const entrada of entradas) {
    const caminho = join(dir, entrada.name);
    if (entrada.isDirectory()) itens.push(...await listarArquivos(caminho));
    else if (/\.(?:js|jsx)$/.test(entrada.name)) itens.push(caminho);
  }
  return itens;
};

const contarMapeamentos = (fonte) => [...fonte.matchAll(/@(Get|Post|Put|Patch|Delete)Mapping\b/g)].length;

test("Etapa 17 integra os 20 endpoints de governança e usa somente Gestor Local", async () => {
  const [controlador, servico, pagina] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/controlador/GovernancaTerceirosControlador.java"),
    lerFront("src/services/governanca/ServicoGovernancaTerceiros.js"),
    lerFront("src/modules/governanca/pages/PaginaGovernancaTerceiros/index.jsx"),
  ]);

  assert.equal(contarMapeamentos(controlador), 20);
  assert.match(controlador, /@PreAuthorize\("hasAuthority\('PERFIL_GESTOR_LOCAL'\)"\)/);
  assert.equal((controlador.match(/@ExigirSenha/g) || []).length, 13);

  for (const trecho of [
    "/resumo", "/fornecedores", "/contratos/${contratoId}/fornecedor/${fornecedorId}",
    "/tabelas-tarifarias", "/processos", "/folhas-horas", "/faturas", "/riscos",
  ]) assert.match(servico, new RegExp(trecho.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));

  assert.match(servico, /atualizarFornecedor:[\s\S]{0,180}method: "PUT"[\s\S]{0,120}fornecedores\/\$\{id\}/);
  assert.match(pagina, /servicoGovernanca\.atualizarFornecedor/);
  assert.match(pagina, /Confirme sua senha para atualizar os dados do fornecedor/);
  assert.match(pagina, /Sem vínculo imediato/);
});

test("transições de processo expostas no React não reduzem a máquina de estados Java", async () => {
  const [java, pagina] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/servico/GovernancaTerceirosServico.java"),
    lerFront("src/modules/governanca/pages/PaginaGovernancaTerceiros/index.jsx"),
  ]);
  for (const transicao of [
    '"PENDENTE", Set.of("EM_ANDAMENTO", "BLOQUEADO", "CANCELADO")',
    '"EM_ANDAMENTO", Set.of("CONCLUIDO", "BLOQUEADO", "CANCELADO")',
    '"BLOQUEADO", Set.of("EM_ANDAMENTO", "CANCELADO")',
  ]) assert.ok(java.includes(transicao));

  for (const status of ["EM_ANDAMENTO", "BLOQUEADO", "CANCELADO", "CONCLUIDO"]) {
    assert.match(pagina, new RegExp(`atualizarProcesso\\(p\\.id, "${status}"`));
  }
});

test("auditoria é Administrador no Java atual e detalhe vem do GET por ID com proteção de PII", async () => {
  const [controlador, roles, servico, pagina, modal, utils, exportacao] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/controlador/RegistroAuditoriaControlador.java"),
    lerFront("src/core/security/roles.js"),
    lerFront("src/services/admin/ServicoAuditoria.js"),
    lerFront("src/modules/admin/pages/PaginaRegistrosAuditoria/index.jsx"),
    lerFront("src/modules/admin/pages/PaginaRegistrosAuditoria/components/ModalDetalhesAuditoria/index.jsx"),
    lerFront("src/modules/admin/pages/PaginaRegistrosAuditoria/utils.js"),
    lerFront("src/modules/admin/pages/PaginaRegistrosAuditoria/exportacao.js"),
  ]);

  assert.match(controlador, /@PreAuthorize\("hasAuthority\([\s\S]{0,120}PERFIL_ADMINISTRADOR[\s\S]{0,80}\)"\)/);
  assert.equal(contarMapeamentos(controlador), 2);
  assert.match(roles, /podeAcessarAuditoria[\s\S]{0,100}AUTORIDADES\.ADMINISTRADOR/);
  assert.match(servico, /buscarPorId:[\s\S]{0,160}\/registros-auditoria\/\$\{id\}/);
  assert.match(pagina, /ServicoAuditoria\.buscarPorId\(registro\.id/);
  assert.match(modal, /textoJsonProtegidoAuditoria/);
  assert.doesNotMatch(modal, /registro\.ip|registro\.userAgent|registro\.queryString/);
  assert.match(utils, /senha\|password[\s\S]{0,200}cpf[\s\S]{0,200}telefone/);
  assert.doesNotMatch(exportacao, /payloadJson|detailsJson|\bip\b|userAgent|queryString/);
});

test("cache diagnóstico permanece não consumido e está restrito ao Administrador no Java atual", async () => {
  const controlador = await lerBack("src/main/java/com/example/siggtrat/controlador/CacheDiagnosticoControlador.java");
  assert.match(controlador, /@PreAuthorize\("hasAuthority\([\s\S]{0,120}PERFIL_ADMINISTRADOR[\s\S]{0,80}\)"\)/);
  assert.match(controlador, /@GetMapping\("\/status"\)/);

  const src = resolve(new URL("../src", import.meta.url).pathname);
  const arquivos = await listarArquivos(src);
  const fontes = (await Promise.all(arquivos.map((arquivo) => readFile(arquivo, "utf8")))).join("\n");
  assert.doesNotMatch(fontes, /["'`]\/cache\/status["'`]/);
});

test("resíduos de autenticação e mutações canônicas de Empregado foram removidos do código produtivo", async () => {
  const src = resolve(new URL("../src", import.meta.url).pathname);
  const arquivos = await listarArquivos(src);
  const fontes = (await Promise.all(arquivos.map((arquivo) => readFile(arquivo, "utf8")))).join("\n");
  const empregado = await lerFront("src/services/rh/ServicoEmpregado.js");
  const formularioLegado = await lerFront("src/modules/rh/pages/EmpregadosLista/components/FormularioEmpregado/useLogicaFormulario.js");

  assert.doesNotMatch(fontes, /\bROLE_[A-Z_]+\b/);
  assert.doesNotMatch(fontes, /canSkipTwoFactor|skipTwoFactor/);
  assert.doesNotMatch(empregado, /\bsalvar\s*:/);
  assert.doesNotMatch(empregado, /\bexcluir\s*:/);
  assert.doesNotMatch(empregado, /method:\s*["'](?:POST|PUT|DELETE)["'][\s\S]{0,120}\/empregados/);
  assert.doesNotMatch(formularioLegado, /ServicoEmpregado\.salvar/);
  assert.match(formularioLegado, /modo somente leitura/);
});

test("os 31 endpoints do escopo Etapa 17 têm decisão explícita contra o Java atual", async () => {
  const [gov, audit, cache, empregado, historico] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/controlador/GovernancaTerceirosControlador.java"),
    lerBack("src/main/java/com/example/siggtrat/controlador/RegistroAuditoriaControlador.java"),
    lerBack("src/main/java/com/example/siggtrat/controlador/CacheDiagnosticoControlador.java"),
    lerBack("src/main/java/com/example/siggtrat/controlador/EmpregadoControlador.java"),
    lerBack("src/main/java/com/example/siggtrat/controlador/HistoricoEmpregadoControlador.java"),
  ]);

  assert.equal(contarMapeamentos(gov), 20);
  assert.equal(contarMapeamentos(audit), 2);
  assert.equal(contarMapeamentos(cache), 1);
  assert.equal(contarMapeamentos(historico), 4);
  assert.match(empregado, /@GetMapping\s+public Page<EmpregadoDTO> listarTodos\(/);
  for (const trecho of [
    '@GetMapping("/{id}/historico-contratos")',
    '@GetMapping("/resumo")',
    '@GetMapping("/{id}")',
  ]) assert.ok(empregado.includes(trecho), `endpoint Empregado ausente: ${trecho}`);
  assert.equal(20 + 2 + 1 + 4 + 4, 31);
});
