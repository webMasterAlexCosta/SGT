import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const backend = process.env.SGT_BACKEND_DIR;
const lerBack = (relativo) => readFile(path.join(backend, relativo), "utf8");

test("painel expõe análise cadastral real baseada em data_admissao", async () => {
  assert.ok(backend, "SGT_BACKEND_DIR é obrigatório para o contrato integrado");
  const [repo, dto, service, page, ferramentas] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/repositorio/PainelRepositorio.java"),
    lerBack("src/main/java/com/example/siggtrat/dto/PainelGestaoDTO.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/PainelServico.java"),
    ler("src/modules/painel/pages/index.jsx"),
    ler("src/modules/painel/components/FerramentasGestao/index.jsx"),
  ]);
  assert.match(repo, /obterAnaliseCadastro/);
  assert.match(repo, /e\.data_admissao BETWEEN :inicio12Meses AND :referencia/);
  assert.match(repo, /obterAdmissoesMensais/);
  assert.match(repo, /obterSituacaoQuadro\(\s*LocalDate referencia/);
  assert.match(repo, /e\.data_admissao IS NULL OR e\.data_admissao <= :referencia/);
  assert.match(dto, /record AnaliseCadastro/);
  assert.match(dto, /record AdmissaoMensal/);
  assert.match(service, /completarAdmissoesMensais/);
  assert.match(page, /<AnaliseCadastral/);
  assert.match(ferramentas, /Movimentação cadastral/);
});

test("indicadores explicam finalidade, cálculo e fonte em modal", async () => {
  const [modal, executivo, ferramentas] = await Promise.all([
    ler("src/modules/painel/components/ModalPainel/index.jsx"),
    ler("src/modules/painel/components/GestaoExecutiva/index.jsx"),
    ler("src/modules/painel/components/FerramentasGestao/index.jsx"),
  ]);
  assert.match(modal, /BotaoAjudaPainel/);
  assert.match(modal, /ModalAjudaIndicador/);
  assert.match(modal, /Para que serve/);
  assert.match(modal, /Como é calculado/);
  assert.match(modal, /Fonte dos dados/);
  assert.match(executivo, /ajuda=\{AJUDA\.frequencia\}/);
  assert.match(ferramentas, /ajuda=\{AJUDA\.cadastro\}/);
});

test("painel diferencia zero real de ausência de base operacional", async () => {
  const [executivo, ferramentas] = await Promise.all([
    ler("src/modules/painel/components/GestaoExecutiva/index.jsx"),
    ler("src/modules/painel/components/FerramentasGestao/index.jsx"),
  ]);
  assert.match(executivo, /Não aplicável/);
  assert.match(executivo, /não converte esse cenário em 100% de cobertura/);
  assert.match(ferramentas, /Sem lançamentos/);
  assert.match(ferramentas, /ausência de registros/);
});

test("forçar atualização invalida cache do painel e evita campos obsoletos", async () => {
  assert.ok(backend, "SGT_BACKEND_DIR é obrigatório para o contrato integrado");
  const [calc, schema, controller] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/servico/PainelCalculoServico.java"),
    lerBack("src/main/java/com/example/siggtrat/constante/EsquemaPainel.java"),
    lerBack("src/main/java/com/example/siggtrat/controlador/PainelCalculoControlador.java"),
  ]);
  assert.match(calc, /if \(forcarAtualizacao\)/);
  assert.match(calc, /cacheInvalidationServico\.invalidarPainel\(\)/);
  assert.match(schema, /VERSION = "4"/);
  assert.doesNotMatch(controller, /InvalidaCacheNegocio/);
});
