// @vitest-environment node
// Testa somente a escolha de destino feita pelo driver. Não abre navegador,
// não envia HTTP e não substitui nenhum oráculo dos cenários E2E reais.
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import vm from 'node:vm';
import { afterEach, expect, it, vi } from 'vitest';

const require = createRequire(import.meta.url);
const { baseFrontend, contratoDaFixture, idObservado } = require('./e2e/contexto-fixture.cjs');
const fixture = {
  synthetic: true,
  ids: { contrato_rascunho: 37, contrato_futuro: 48, gestao_origem: 182, gestao_destino: 183 },
  objects: {
    contrato_rascunho: { id: 37, numeroContrato: 'SGT-CONTINUIDADE', anoContrato: 2031 },
    contrato_futuro: { id: 48, numeroContrato: 'SGT-FUTURO', anoContrato: 2031 },
  },
  accounts: { fixture_admin: { id: 14, perfil: 'ADMINISTRADOR' }, gestor_local: { id: 15 }, gestor_destino: { id: 16, gestaoLocalId: 183 } },
  dates: { today: '2031-11-02', future: '2031-12-02' },
};

afterEach(() => vi.unstubAllEnvs());

async function observarPrimeiraConsulta(arquivo) {
  const origem = new URL(`./e2e/${arquivo}`, import.meta.url);
  let tarefa;
  let endereco;
  const seletores = [];
  const fimDaObservacao = new Error('Fim da observação do destino, sem HTTP');
  const seletor = new Proxy({}, {
    get: (_obj, nome) => nome === 'then' ? undefined : (...args) => {
      if (['getByRole', 'getByLabel'].includes(nome)) seletores.push({ metodo: nome, args });
      if (nome === 'url') return 'http://127.0.0.1:18080/contratos';
      return seletor;
    },
  });
  const contexto = {
    page: seletor, fixture, account: fixture.accounts.fixture_admin,
    context: { request: { get: async (url) => { endereco = url; throw fimDaObservacao; } } },
    report: { http: [], screenshots: [] },
    check: () => {}, screenshot: async () => {}, open: async () => {},
    confirmPassword: async () => {}, recordBusiness: () => {},
  };
  const sandbox = {
    URL, process: { env: { ...process.env, SGT_FRONTEND_URL: 'http://127.0.0.1:18080' } },
    console, __dirname: new URL('./e2e', import.meta.url).pathname,
    require: (nome) => nome === './ambiente-real.cjs'
      ? { execute: (_nome, callback) => { tarefa = callback(contexto); return tarefa; } }
      : nome.startsWith('./') ? require(new URL(`./e2e/${nome.slice(2)}`, import.meta.url).pathname) : require(nome),
  };
  vi.stubEnv('SGT_FRONTEND_URL', 'http://127.0.0.1:18080');
  vm.runInNewContext(readFileSync(origem, 'utf8'), sandbox, { filename: origem.pathname });
  await tarefa.catch((erro) => { if (erro !== fimDaObservacao) throw erro; });
  return { endereco, seletores };
}

it('a paginação consulta o contrato observado na fixture e o JAR configurado', async () => {
  const { endereco } = await observarPrimeiraConsulta('paginacao-contrato-real.cjs');
  expect(endereco).toBe('http://127.0.0.1:18080/api/contratos/37/versoes');
});

it('a publicação verifica o mesmo contrato e usa número/ano observados na UI', async () => {
  const { endereco, seletores } = await observarPrimeiraConsulta('contrato-publicacao.cjs');
  expect(endereco).toBe('http://127.0.0.1:18080/api/contratos/37');
  const nomes = seletores.map(({ args }) => args[1]?.name).filter((nome) => typeof nome === 'string');
  expect(nomes).toContain('Abrir ciclo de vida do contrato SGT-CONTINUIDADE');
  expect(nomes).toContain('SGT-CONTINUIDADE/2031');
  expect(nomes).not.toContain('SGTE2E001/2026');
});

it('normaliza somente a origem explicitamente configurada', () => {
  expect(baseFrontend({ SGT_FRONTEND_URL: 'http://127.0.0.1:19091/' })).toBe('http://127.0.0.1:19091');
  expect(baseFrontend({ SGT_FRONTEND_URL: 'https://sgt-sintetico.example.test' })).toBe('https://sgt-sintetico.example.test');
});

it.each([
  undefined, '', 'nao-e-url', 'file:///tmp/sgt', 'http://usuario:senha@127.0.0.1:18080',
  'http://127.0.0.1:18080/contratos', 'http://127.0.0.1:18080/?x=1', 'http://127.0.0.1:18080/#x',
])('recusa destino ausente ou ambíguo antes de abrir o navegador (caso %#)', (valor) => {
  expect(() => baseFrontend({ SGT_FRONTEND_URL: valor })).toThrow(/SGT_FRONTEND_URL/);
});

it('distingue o contrato publicado e o futuro pelos IDs e dados da fixture', () => {
  expect(contratoDaFixture(fixture)).toEqual({ id: 37, numero: 'SGT-CONTINUIDADE', ano: 2031,
    titulo: 'SGT-CONTINUIDADE/2031', botaoAbrir: 'Abrir ciclo de vida do contrato SGT-CONTINUIDADE',
    caminho: '/contratos/37', api: '/api/contratos/37' });
  expect(contratoDaFixture(fixture, 'contrato_futuro').api).toBe('/api/contratos/48');
});

it.each([
  { ...fixture, synthetic: false },
  { ...fixture, ids: {} },
  { ...fixture, objects: {} },
  { ...fixture, objects: { contrato_rascunho: { id: 38, numeroContrato: 'OUTRO', anoContrato: 2031 } } },
  { ...fixture, objects: { contrato_rascunho: { id: 37, numeroContrato: '', anoContrato: 2031 } } },
])('recusa identidade não observada, incompleta ou contraditória (%#)', (invalida) => {
  expect(() => contratoDaFixture(invalida)).toThrow(/fixture|observad|sintética/);
});

it.each([undefined, null, 0, -1, '37', 37.5, Number.MAX_SAFE_INTEGER + 1])('não supõe um ID quando o observado é inválido: %s', (valor) => {
  expect(() => idObservado(valor, 'documentoId')).toThrow(/documentoId/);
});

function observarConfiguracaoEvolucao(origem) {
  vi.stubEnv('SGT_FRONTEND_URL', origem);
  const arquivo = new URL('./e2e/evolucao-governanca-real.cjs', import.meta.url);
  const limite = new Error('Fim da observação antes da leitura da fixture, sem navegador');
  let leituraSolicitada = false;
  let erro;
  const sandbox = {
    URL, console,
    process: { env: { ...process.env, SGT_TEST_RUNTIME: '/fixture-sintetica', SGT_EVOLUCAO_LABEL: 'configuracao' } },
    require: (nome) => nome === 'node:fs'
      ? { readFileSync: () => { leituraSolicitada = true; throw limite; } }
      : nome.startsWith('./') ? require(new URL(`./e2e/${nome.slice(2)}`, import.meta.url).pathname) : require(nome),
  };
  try { vm.runInNewContext(readFileSync(arquivo, 'utf8'), sandbox, { filename: arquivo.pathname }); }
  catch (falha) { if (falha !== limite) erro = falha; }
  return { leituraSolicitada, erro };
}

it('a evolução histórica aceita a origem explícita de upgrade sem impor a porta 18080', () => {
  const resultado = observarConfiguracaoEvolucao('http://127.0.0.1:18081');
  expect(resultado.erro).toBeUndefined();
  expect(resultado.leituraSolicitada).toBe(true);
});

it('a evolução histórica não cria origem silenciosa quando falta configuração', () => {
  const resultado = observarConfiguracaoEvolucao(undefined);
  expect(resultado.leituraSolicitada).toBe(false);
  expect(resultado.erro?.message).toMatch(/SGT_FRONTEND_URL/);
});
