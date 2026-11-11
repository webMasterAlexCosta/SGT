'use strict';
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend } = require('./contexto-fixture.cjs');
execute('carga-parcial-api-real', async ({ page, context, open, check, screenshot, report }) => {
  const base = baseFrontend();
  await open('/contratos');
  const read = async path => {
    const r = await context.request.get(base + path);
    check('Controle independente200 ' + path, r.status() === 200);
    return (await r.json()).data;
  };
  const providers = await read('/api/fornecedores'), contracts = await read('/api/contratos');
  check('Controles válidos possuem dados reais', providers.length > 0 && contracts.length > 0);
  let release;
  const gate = new Promise(resolve => { release = resolve; });
  let capture;
  const held = new Promise(resolve => { capture = resolve; });
  const missing = '/api/contratos/9223372036854775807';
  const pattern = url => url.pathname === '/api/contratos';
  let intercepted = false;
  const handler = async route => {
    if (intercepted || route.request().method() !== 'GET') return route.continue();
    intercepted = true; capture(); await gate;
    // Fault injection changes only one request target. The real JAR authorizes
    // and returns its real 404; no mock server, response body or fulfill is used.
    await route.continue({ url: base + missing });
  };
  await page.route(pattern, handler);
  try {
    await page.goto(base + '/empresas-contratadas', { waitUntil: 'domcontentloaded' });
    await held;
    const company = page.getByRole('textbox', { name: 'Razão social da empresa', exact: true });
    const cnpj = page.getByRole('textbox', { name: 'CNPJ da empresa', exact: true });
    await company.fill('SINTETICO CARGA PARCIAL SEM CADASTRO');
    await cnpj.fill(providers[0].cnpj);
    const failure = page.waitForResponse(r => r.request().method() === 'GET'
      && ['/api/contratos', missing].includes(new URL(r.url()).pathname) && r.status() >= 400);
    failure.catch(() => {});release();
    const response = await failure, envelope = await response.json();
    check('Erro parcial404 é produzido pela API real autenticada', response.status() === 404 && envelope.message && envelope.code);
    await page.getByRole('alert').filter({ hasText: envelope.message }).first().waitFor();
    await page.getByText('Carregando empresas...', { exact: true }).waitFor({ state: 'hidden' });
    check('Falha parcial mostra erro útil sem encerrar a sessão', await page.getByRole('button', { name: 'Sair', exact: true }).isVisible());
    check('Dados carregados com sucesso permanecem na tabela', await page.getByRole('table').locator('tbody tr').count() === Math.min(5, providers.length)
      && (await page.getByRole('table').innerText()).includes(providers[0].razaoSocial));
    check('Total do cadastro não vira lista vazia por erro do outro recurso', (await page.getByRole('navigation', { name: 'Paginação de empresas', exact: true }).innerText()).includes(providers.length + ' registro(s)'));
    check('Formulário e contexto permanecem sem falso sucesso', await company.inputValue() === 'SINTETICO CARGA PARCIAL SEM CADASTRO'
      && await cnpj.inputValue() === providers[0].cnpj
      && !(await page.locator('body').innerText()).includes('Empresa cadastrada com sucesso'));
    check('Nenhuma criação ou associação foi enviada', !report.http.some(h => ['POST', 'PUT', 'PATCH', 'DELETE'].includes(h.method)
      && (h.path.startsWith('/api/fornecedores') || h.path.startsWith('/api/contratos'))));
    report.faultInjection = { original: '/api/contratos', actualTarget: missing, method: 'GET', realStatus: response.status(),
      realCode: envelope.code, responseBodyNotMocked: true, oneRequestOnly: true, noProductOrServerChange: true };
    await screenshot('erro-parcial-dados-e-formulario-preservados');
  } finally { release(); await page.unroute(pattern, handler); }
  check('O recurso válido retorna normalmente após a falha localizada', (await read('/api/contratos')).length === contracts.length
    && (await read('/api/fornecedores')).length === providers.length);
  report.scope = 'AUXILIARY_E2E24_REAL_PARTIAL_LOAD_404_FAULT_INJECTION_NO_SYNTHETIC_RESPONSE_NO_DOMAIN_MUTATIONS_NOT_LAN';
}, { reuse: false });
