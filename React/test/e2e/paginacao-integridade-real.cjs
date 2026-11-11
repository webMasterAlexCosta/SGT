'use strict';
const fs = require('node:fs');
const path = require('node:path');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend } = require('./contexto-fixture.cjs');
execute('paginacao-integridade-real', async ({ page, context, open, check, screenshot, report }) => {
  const b = JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json')));
  const base = baseFrontend();
  await open('/empresas-contratadas');
  await page.getByText('Carregando empresas...', { exact: true }).waitFor({ state: 'hidden' });
  const r = await context.request.get(base + '/api/fornecedores');
  const providers = (await r.json()).data;
  check('Universo real possui ao menos seis empresas', r.status() === 200 && providers.length >= 6);
  const navigation = page.getByRole('navigation', { name: 'Paginação de empresas', exact: true });
  const observed = [];
  const digits = text => text.replace(/\D/g, '');
  for (let n = 0; n < Math.ceil(providers.length / 5); n++) {
    await navigation.getByText(new RegExp(`página ${n + 1} de`)).waitFor();
    const contacts = await page.getByRole('table').locator('tbody tr td:nth-child(2)').evaluateAll(cells => cells.map(c => c.childNodes[0].textContent));
    const expected = providers.slice(n * 5, n * 5 + 5).map(p => digits(p.cnpj));
    check('Página visual de empresas preserva exatamente os CNPJ do DTO ' + n, JSON.stringify(contacts.map(digits)) === JSON.stringify(expected));
    observed.push(...contacts.map(digits));
    if (n + 1 < Math.ceil(providers.length / 5)) await navigation.getByRole('button', { name: 'Próxima página de empresas', exact: true }).click();
  }
  check('Nenhuma empresa perdida ou duplicada na cauda', observed.length === providers.length && new Set(observed).size === providers.length);
  const last = providers.at(-1);
  await page.getByRole('searchbox', { name: 'Pesquisar empresas', exact: true }).fill(last.cnpj);
  check('Pesquisa alcança a última empresa fora da primeira página', await page.getByRole('table').locator('tbody tr').count() === 1
    && (await page.getByRole('table').innerText()).includes(last.razaoSocial));
  await screenshot('empresa-da-ultima-pagina');

  await open('/pessoas');
  const params = { contratoId: String(b.jobs200ContratoId), size: 50, sort: 'nome,asc' };
  const expected = [];
  let total = 0, pages = 1;
  for (let n = 0; n < pages; n++) {
    const response = await context.request.get(base + '/api/pessoas', { params: { ...params, page: n } });
    const data = (await response.json()).data;
    check('Página independente da API responde200 ' + n, response.status() === 200);
    total = data.totalElements; pages = data.totalPages; expected.push(...data.content.map(p => p.id));
  }
  check('Universo independente de pessoas supera31 sem IDs duplicados', total >= 31 && total <= 400 && expected.length === total && new Set(expected).size === total);
  await page.getByRole('combobox', { name: /^Contrato/ }).selectOption(String(b.jobs200ContratoId));
  await page.getByRole('button', { name: 'Aplicar filtros', exact: true }).click();
  const peopleNav = page.getByRole('navigation', { name: 'Paginação', exact: true });
  const actual = [];
  for (let n = 0; n < Math.ceil(total / 5); n++) {
    await peopleNav.getByText(`${total} registro(s) · página ${n + 1} de ${Math.ceil(total / 5)}`, { exact: true }).waitFor();
    const ids = await page.getByRole('table').locator('tbody tr td:first-child small').evaluateAll(nodes => nodes.map(el => Number(el.textContent.match(/ID (\d+)/)[1])));
    check('Página de cinco pessoas coincide com a API independente ' + n, JSON.stringify(ids) === JSON.stringify(expected.slice(n * 5, n * 5 + 5)));
    actual.push(...ids);
    if (n + 1 < Math.ceil(total / 5)) await peopleNav.getByRole('button', { name: 'Próxima', exact: true }).click();
  }
  check('Universo completo de pessoas preservado sem perda nem duplicação', JSON.stringify(actual) === JSON.stringify(expected) && new Set(actual).size === total);
  await screenshot('cauda-pessoas-sem-truncamento');
  check('Navegação somente leitura não enviou mutações de domínio', report.http.every(h => h.method === 'GET' || h.path.startsWith('/api/autenticacao/')));
  report.scope = 'AUXILIARY_ROP042_SUPPLIERS_AND_200_PEOPLE_ALL_UI_PAGES_NOT_DOCUMENT_OR_EXPORT_ACCEPTANCE_NOT_LAN';
  report.population = { providers: providers.length, people: total, pageSize: 5 };
}, { reuse: false });
