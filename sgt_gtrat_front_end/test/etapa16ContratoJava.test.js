import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const backend = process.env.SGT_BACKEND_DIR;
if (!backend) throw new Error("SGT_BACKEND_DIR é obrigatório para validar a Etapa 16 contra o Java definitivo.");

const lerFront = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const lerBack = (caminho) => readFile(resolve(backend, caminho), "utf8");
const CONTROLES = "src/main/java/com/example/siggtrat/controlador";
const SERVICOS = "src/main/java/com/example/siggtrat/servico";

const blocoEntre = (texto, inicio, fim) => {
  const a = texto.indexOf(inicio);
  assert.ok(a >= 0, `Trecho inicial não encontrado: ${inicio}`);
  const b = fim ? texto.indexOf(fim, a + inicio.length) : texto.length;
  assert.ok(b > a, `Trecho final não encontrado: ${fim}`);
  return texto.slice(a, b);
};

test("Etapa 16 inventaria os 22 endpoints dos dois controladores de relatórios", async () => {
  const [operacional, administrativo, servicoAdmin, servicoOperacional] = await Promise.all([
    lerBack(`${CONTROLES}/RelatorioControlador.java`),
    lerBack(`${CONTROLES}/RelatorioAdministrativoControlador.java`),
    lerFront("src/services/rh/ServicoRelatorioAdministrativo.js"),
    lerFront("src/services/rh/ServicoFrequencia.js"),
  ]);
  assert.equal((operacional.match(/@(Get|Post)Mapping/g) || []).length, 16);
  assert.equal((administrativo.match(/@(Get|Post)Mapping/g) || []).length, 6);
  for (const rota of ["/previa", "/emissoes", "/emissoes/conhecimento", "/comparacao-vigente"]) {
    assert.match(servicoAdmin, new RegExp(rota.replaceAll("/", "\\/")));
  }
  for (const rota of ["/relatorios/filtros", "/relatorios/frequencia-consolidada", "/relatorios/absenteismo", "/relatorios/horas-trabalhadas"]) {
    assert.match(servicoOperacional, new RegExp(rota.replaceAll("/", "\\/")));
  }
});

test("relatórios operacionais excluem Presidente enquanto a série administrativa mantém leitura", async () => {
  const [controlador, admin, roles] = await Promise.all([
    lerBack(`${CONTROLES}/RelatorioControlador.java`),
    lerBack(`${CONTROLES}/RelatorioAdministrativoControlador.java`),
    lerFront("src/core/security/roles.js"),
  ]);
  const blocoOperacional = blocoEntre(controlador, '@GetMapping("/frequencia-consolidada")', '@GetMapping("/frequencia-consolidada/paginada")');
  assert.doesNotMatch(blocoOperacional, /PERFIL_PRESIDENTE/);
  assert.match(admin, /PERFIL_PRESIDENTE/);
  const blocoReactOperacional = blocoEntre(roles, "export const podeAcessarRelatorios", "export const podeAcessarVisaoConsolidadaRelatorios");
  assert.doesNotMatch(blocoReactOperacional, /AUTORIDADES\.PRESIDENTE/);
  const blocoReactAdmin = blocoEntre(roles, "export const podeAcessarRelatoriosAdministrativos", "export const podeEmitirRelatoriosAdministrativos");
  assert.match(blocoReactAdmin, /AUTORIDADES\.PRESIDENTE/);
});

test("Presidente não recebe emissão formal administrativa", async () => {
  const [controlador, roles, pagina] = await Promise.all([
    lerBack(`${CONTROLES}/RelatorioAdministrativoControlador.java`),
    lerFront("src/core/security/roles.js"),
    lerFront("src/modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx"),
  ]);
  assert.match(controlador, /@PostMapping\("\/emissoes"\)[\s\S]{0,220}!hasAuthority[\s\S]{0,220}PERFIL_PRESIDENTE/);
  assert.match(roles, /podeEmitirRelatoriosAdministrativos[\s\S]{0,180}!ePresidente\(usuario\)/);
  assert.match(pagina, /disabled=\{carregando \|\| !podeEmitir \|\| presidente\}/);
});

test("emissão formal usa chave de idempotência exigida pelo DTO Java", async () => {
  const [dto, servico, pagina] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/dto/EmissaoRelatorioRequisicao.java"),
    lerFront("src/services/rh/ServicoRelatorioAdministrativo.js"),
    lerFront("src/modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx"),
  ]);
  assert.match(dto, /@NotBlank @Size\(max = 120\) String chaveIdempotencia/);
  assert.match(servico, /chaveIdempotencia/);
  assert.match(servico, /randomUUID/);
  assert.match(pagina, /chaveIdempotencia\.current/);
  assert.match(pagina, /renovarChave/);
});

test("limites de paginação do React seguem os máximos efetivos do Java", async () => {
  const [javaOperacional, javaTerceiros, javaAdmin, tabelaFrequencia, paginaAbs, paginaAdmin] = await Promise.all([
    lerBack(`${SERVICOS}/RelatorioFrequenciaConsolidadoServico.java`),
    lerBack(`${SERVICOS}/RelatorioTerceirizadosServico.java`),
    lerBack(`${SERVICOS}/RelatorioAdministrativoServico.java`),
    lerFront("src/modules/rh/pages/PaginaRelatorioFrequencia/components/TabelaRelatorio/index.jsx"),
    lerFront("src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx"),
    lerFront("src/modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx"),
  ]);
  assert.match(javaOperacional, /TAMANHO_PAGINA_MAXIMO\s*=\s*500/);
  assert.match(javaTerceiros, /TAMANHO_MAXIMO\s*=\s*5000/);
  assert.match(javaAdmin, /TAMANHO_MAXIMO_PAGINA\s*=\s*200/);
  for (const codigo of [tabelaFrequencia, paginaAbs]) {
    for (const tamanho of [25, 100, 200, 500]) assert.match(codigo, new RegExp(`value="${tamanho}"`));
    assert.doesNotMatch(codigo, /value="1000"|value="2000"|value="5000"/);
  }
  for (const tamanho of [25, 50, 100, 200]) assert.match(paginaAdmin, new RegExp(`value="${tamanho}"`));
});

test("frequência e absenteísmo exibem aviso de revisão fornecido pelo Java", async () => {
  const [dtoPagina, dtoResumo, freq, abs, servicoAbs] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/dto/RelatorioFrequenciaPaginaDTO.java"),
    lerBack("src/main/java/com/example/siggtrat/dto/RelatorioAbsenteismoResumoDTO.java"),
    lerFront("src/modules/rh/pages/PaginaRelatorioFrequencia/index.jsx"),
    lerFront("src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx"),
    lerFront("src/services/rh/ServicoAbsenteismo.js"),
  ]);
  assert.match(dtoPagina, /AvisoRevisaoRelatorioDTO revisao/);
  assert.match(dtoResumo, /AvisoRevisaoRelatorioDTO revisao/);
  assert.match(freq, /resposta\.revisao/);
  assert.match(freq, /SUJEITO A REVISÃO/);
  assert.match(abs, /SUJEITO A REVISÃO/);
  assert.match(servicoAbs, /resumo\?\.revisao/);
});

test("absenteísmo não recalcula indicadores gerais a partir da página parcial", async () => {
  const [servico, pagina] = await Promise.all([
    lerFront("src/services/rh/ServicoAbsenteismo.js"),
    lerFront("src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx"),
  ]);
  const metodo = blocoEntre(servico, "gerarRelatorio: async", "gerarResumo: async");
  assert.doesNotMatch(metodo, /consolidarAbsenteismo/);
  assert.match(metodo, /pagina\?\.registros/);
  assert.match(pagina, /ServicoAbsenteismo\.gerarResumo/);
  assert.match(pagina, /indicadores\.maioresTaxas/);
});

test("exportação assíncrona evita criação duplicada por duplo clique", async () => {
  const contexto = await lerFront("src/core/exportacao/context.jsx");
  assert.match(contexto, /solicitacoesEmAndamento/);
  assert.match(contexto, /chaveSolicitacao/);
  assert.match(contexto, /if \(existente\) return existente/);
  assert.match(contexto, /solicitacoesEmAndamento\.current\.delete\(chaveSolicitacao\)/);
});

test("administrativo não inventa endpoint binário e mantém conhecida-até/comparação", async () => {
  const [controlador, servico, pagina] = await Promise.all([
    lerBack(`${CONTROLES}/RelatorioAdministrativoControlador.java`),
    lerFront("src/services/rh/ServicoRelatorioAdministrativo.js"),
    lerFront("src/modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx"),
  ]);
  assert.doesNotMatch(controlador, /arquivo|download/i);
  assert.match(servico, /emissoes\/conhecimento/);
  assert.match(servico, /comparacao-vigente/);
  assert.match(pagina, /não possui endpoint binário de download/);
});

test("compatibilidade fail-closed documenta limitação atual de Presidente em obter/comparar por ID", async () => {
  const [servicoJava, pagina] = await Promise.all([
    lerBack(`${SERVICOS}/RelatorioAdministrativoServico.java`),
    lerFront("src/modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx"),
  ]);
  assert.match(servicoJava, /void exigirAcesso|exigirAcesso\(/);
  assert.match(pagina, /serviço Java atual bloqueia a abertura por ID para Presidente/);
  assert.match(pagina, /serviço Java atual bloqueia a comparação por ID para Presidente/);
});
