'use strict';
// Navegador e API reais. Não intercepta, não substitui e não fabrica respostas.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { baseFrontend } = require('./contexto-fixture.cjs');
process.env.SGT_E2E_MODE ||= 'jar-integrado';
const base = baseFrontend();
const runtime = process.env.SGT_TEST_RUNTIME;
assert.ok(runtime, 'Defina SGT_TEST_RUNTIME.');
const label = process.env.SGT_EVOLUCAO_LABEL;
assert.ok(/^[A-Za-z0-9_-]{1,40}$/.test(label || ''), 'Defina SGT_EVOLUCAO_LABEL com o label preparado.');
const statePath = path.join(runtime, 'private', `evolucao-governanca-real-${label}.json`);
const state = JSON.parse(fs.readFileSync(statePath));
assert.ok(state.synthetic === true && state.ready === true, 'Execute o setup HTTP desta fixture antes do navegador.');
assert.equal(state.baseUrl, base, 'Setup e navegador devem consultar o mesmo servidor.');
assert.ok(!state.browserStarted, 'Use outro label para repetir o cenário completo após iniciada a consulta do cenário.');
const { execute } = require('./ambiente-real.cjs');
const unwrap = value => value && typeof value === 'object' && Object.hasOwn(value, 'data') && Object.hasOwn(value, 'status') ? value.data : value;
const writeState = () => fs.writeFileSync(statePath, JSON.stringify(state, null, 2) + '\n', { mode: 0o600 });
// Localizadores sem depender de classes CSS ou de valores inseridos pelo teste no DOM.
const dd = (root, name) => root.locator('dt').filter({ hasText: new RegExp('^' + name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$') }).locator('xpath=following-sibling::dd[1]');
async function zeroFalse({ page, context, check, open, screenshot, report }) {
  const contractId = state.objects.contract.id;
  report.fixture = { contractId, zeroCompositionId: state.objects.zeroComposition.id };
  report.limits = ['Contrato e composições sintéticos preparados pela API real; este cenário faz somente leitura histórica.'];
  state.browserStarted = new Date().toISOString(); writeState();
  const api = async (route, options = {}) => {
    const response = await context.request.get(base + route, options);
    check(`Leitura canônica real ${route}`, response.status() === 200, { status: response.status() });
    report.http.push({ method: 'GET', path: route, status: response.status(), driver: 'APIRequestContext real' });
    return unwrap(await response.json());
  };
  await open('/historico');
  const query = page.getByRole('form', { name: 'Consulta histórica', exact: true });
  await query.getByLabel('ID do contrato', { exact: true }).fill(String(contractId));
  await query.getByLabel('Data efetiva consultada', { exact: true }).fill(state.dates.contractStart);
  await query.getByLabel('Conhecido até (ISO com fuso)', { exact: true }).fill('');
  await query.getByRole('button', { name: 'Consultar histórico', exact: true }).click();
  const compositions = page.getByRole('table', { name: 'Composições no recorte D/K', exact: true });
  const zeroId = state.objects.zeroComposition.id;
  await compositions.getByText(`Ver registro ${zeroId}`, { exact: true }).click();
  const zeroDetails = compositions.locator('details').filter({ has: page.getByText(`Ver registro ${zeroId}`, { exact: true }) });
  await dd(zeroDetails, 'Efetivo original previsto').getByText('Valor comprovado', { exact: true }).waitFor();
  check('Histórico integrado apresenta zero documentado sem transformá-lo em ausência', /\b0\b/.test(await dd(zeroDetails, 'Efetivo original previsto').innerText()));
  check('Flag falsa derivada de zero continua valor comprovado', /Valor comprovado/.test(await dd(zeroDetails, 'Permite nova alocação').innerText())
    && /\bNão\b/.test(await dd(zeroDetails, 'Permite nova alocação').innerText()));
  const canonicalCompositions = await api(`/api/contratos/${contractId}/composicoes`, { params: { data: state.dates.contractStart } });
  const zero = canonicalCompositions.find(item => item.id === zeroId);
  check('Zero visto no navegador corresponde à composição e origem reais', zero.efetivoPrevisto === 0
    && zero.provaCampos.efetivoPrevisto.estado === 'VALOR_COMPROVADO'
    && zero.provaCampos.efetivoPrevisto.origens.some(origin => origin.composicaoId === zeroId));
  await screenshot('zero-falso-composicao');
}

async function historical({ page, context, check, open, screenshot, report }) {
  const fixture = state.historical;
  const endpoint = `/api/contratos/${fixture.contratoId}`;
  report.fixture = { ...fixture, sha256: state.historicalFixtureSha256, origin: state.historicalOrigin };
  report.cuts = [];
  report.limits = ['Fixture sintética criada pela suíte Spring/JPA/MySQL. A perda documental foi simulada pelo teste Java; as provas foram incorporadas pela API canônica.',
    'Este cenário lê a persistência real via TCP e navegador. Não cria fatos nem concede acesso no cliente.',
    'A restrição exercitada é a minimização do DTO de prova para seu leitor autorizado, sem tentar contornar a rota documental.'];
  await open('/historico');
  const query = page.getByRole('form', { name: 'Consulta histórica', exact: true });
  const photo = () => page.locator('details').filter({ has: page.getByText('Fotografia contratual aplicada ao recorte', { exact: true }) });
  const consult = async (name, k) => {
    await query.getByLabel('ID do contrato', { exact: true }).fill(String(fixture.contratoId));
    await query.getByLabel('Data efetiva consultada', { exact: true }).fill(fixture.dataReferencia);
    await query.getByLabel('Conhecido até (ISO com fuso)', { exact: true }).fill(k);
    const pending = page.waitForResponse(response => {
      const url = new URL(response.url());
      return url.pathname === endpoint + '/posicao-temporal' && url.searchParams.get('conhecidoAte') === k;
    }, { timeout: 25000 });
    const compositionsPending = page.waitForResponse(response => {
      const url = new URL(response.url());
      return url.pathname === endpoint + '/composicoes' && url.searchParams.get('conhecidoAte') === k;
    }, { timeout: 25000 });
    await query.getByRole('button', { name: 'Consultar histórico', exact: true }).click();
    const response = await pending;
    const position = unwrap(await response.json());
    const compositionResponse = await compositionsPending;
    check(`${name}: D/K exatos chegam às duas rotas reais`, response.status() === 200 && compositionResponse.status() === 200
      && position.dataReferencia === fixture.dataReferencia && position.conhecidoAte === k);
    await page.getByRole('heading', { name: 'Posição contratual no recorte', exact: true }).waitFor();
    await photo().waitFor();
    check(`${name}: K conserva todos os dígitos na interface`, await query.getByLabel('Conhecido até (ISO com fuso)', { exact: true }).inputValue() === k);
    report.cuts.push({ name, data: fixture.dataReferencia, conhecidoAte: k, position });
    return position.versaoContratual;
  };
  const before = await consult('Antes da primeira prova', fixture.conhecidoAntes);
  check('Descrição permanece desconhecida no K anterior', before.provaCampos.descricao.estado === 'NAO_COMPROVADO'
    && /Desconhecido \/ não comprovado/.test(await dd(photo(), 'Descrição').innerText()));
  await screenshot('ind08-antes');
  const description = await consult('K exato da descrição', fixture.conhecidoAposDescricao);
  check('K exato incorpora a descrição e sua fonte real', description.provaCampos.descricao.estado === 'VALOR_COMPROVADO'
    && description.provaCampos.descricao.origens.some(origin => origin.provaId === fixture.provaDescricaoId)
    && (await dd(photo(), 'Descrição').innerText()).includes(description.descricao));
  const falseValue = await consult('K da matrícula parcial', fixture.conhecidoAposMatricula);
  check('Falso comprovado não comprova ausência do dia reduzido', falseValue.matriculaTipoContratacaoObrigatoria === false
    && falseValue.provaCampos.matriculaTipoContratacaoObrigatoria.estado === 'VALOR_COMPROVADO'
    && falseValue.provaCampos.diaSemanaReduzido.estado === 'NAO_COMPROVADO'
    && /\bNão\b/.test(await dd(photo(), 'Matrícula exigida pelo tipo').innerText())
    && /Desconhecido \/ não comprovado/.test(await dd(photo(), 'Dia de jornada reduzida').innerText()));
  await screenshot('ind08-mascara-parcial');
  const absent = await consult('K da ausência documentada', fixture.conhecidoAposAusencia);
  check('Ausência documentada surge somente no K da prova própria', absent.diaSemanaReduzido === null
    && absent.provaCampos.diaSemanaReduzido.estado === 'AUSENCIA_COMPROVADA'
    && absent.provaCampos.diaSemanaReduzido.origens.some(origin => origin.provaId === fixture.provaAusenciaId)
    && /Ausência comprovada/.test(await dd(photo(), 'Dia de jornada reduzida').innerText()));
  check('Outro campo desconhecido continua desconhecido e a projeção não ganha ID físico', absent.id === null
    && absent.provaCampos.tipoContratacaoDescricao.estado === 'NAO_COMPROVADO'
    && /Desconhecido \/ não comprovado/.test(await dd(photo(), 'Descrição histórica do tipo').innerText()));
  check('Descrição herdada conserva origem e ISO exato da primeira prova', absent.provaCampos.descricao.origens.some(origin => origin.provaId === fixture.provaDescricaoId)
    && (await dd(photo(), 'Descrição').innerText()).includes(fixture.conhecidoAposDescricao));
  await screenshot('ind08-ausencia-origens');
  const proofsResponse = await context.request.get(base + endpoint + '/provas-historicas', { params: { conhecidoAte: fixture.conhecidoAposAusencia } });
  check('Cadeia de provas é relida da API real', proofsResponse.status() === 200);
  const proofs = unwrap(await proofsResponse.json());
  const proof = proofs.find(item => item.id === fixture.provaAusenciaId);
  const protectedFields = ['referenciaSei', 'motivo', 'autorizacaoAdministrativa', 'documentoComprobatorioId', 'hashFonteArquivo'];
  check('Visibilidade restrita não inventa estado factual nem origem', protectedFields.every(key => proof.provaCampos[key].visibilidade === 'RESTRITA'
    && proof.provaCampos[key].estado === null && proof.provaCampos[key].origens.length === 0 && !Object.hasOwn(proof, key)));
  const table = page.getByRole('table', { name: 'Provas conhecidas até o corte', exact: true });
  await table.getByText(`Ver registro ${fixture.provaAusenciaId}`, { exact: true }).click();
  const details = table.locator('details').filter({ has: page.getByText(`Ver registro ${fixture.provaAusenciaId}`, { exact: true }) });
  await dd(details, 'Referência SEI').getByText('Acesso restrito', { exact: true }).waitFor();
  check('Interface apresenta restrição sem classificá-la como ausência ou desconhecimento', /Acesso restrito/.test(await dd(details, 'Referência SEI').innerText())
    && !/Ausência comprovada|Desconhecido/.test(await dd(details, 'Referência SEI').innerText()));
  const body = await page.locator('body').innerText();
  const sourceMarkers = ['SEI-SINTETICO-IND08-NAO-EXPOR', 'Narrativa sintética restrita da conferência IND08', 'ATO-IND08-RESTRITO'];
  check('Fonte sintética restrita não aparece no DTO ou no DOM', sourceMarkers.every(value => !body.includes(value) && !JSON.stringify(proofs).includes(value)));
  await screenshot('ind08-restricao-revisao');
  const versionsResponse = await context.request.get(base + endpoint + '/versoes');
  check('Acervo histórico é relido pela rota real', versionsResponse.status() === 200);
  const versions = unwrap(await versionsResponse.json());
  const original = [...versions].sort((a, b) => a.numeroVersao - b.numeroVersao)[0];
  await page.getByText('Publicação original e acervo contratual · conhecimento atual', { exact: true }).click();
  const publications = page.getByRole('table', { name: 'Fotografias vinculadas à publicação', exact: true });
  await publications.getByText(`Ver registro ${original.id}`, { exact: true }).click();
  const originalDetails = publications.locator('details').filter({ has: page.getByText(`Ver registro ${original.id}`, { exact: true }) });
  await dd(originalDetails, 'Descrição').getByText('Desconhecido / não comprovado', { exact: true }).waitFor();
  check('Fotografia original não recebe retroativamente o valor de uma prova posterior', original.provaCampos.descricao.estado === 'NAO_COMPROVADO'
    && /Desconhecido \/ não comprovado/.test(await dd(originalDetails, 'Descrição').innerText()));
  await screenshot('ind08-publicacao-preservada');
}

(async () => {
  await execute(`evolucao-governanca-real-${label}-zero-falso`, zeroFalse, { accountName: 'gestor_local', reuse: false });
  if (process.exitCode) return;
  await execute(`evolucao-governanca-real-${label}-ind08`, historical, { accountName: 'ind08_historico', reuse: false });
})().catch(error => { console.error('E2E interrompido:', error.name, error.message); process.exitCode = 1; });
