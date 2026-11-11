'use strict';
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend, contratoDaFixture } = require('./contexto-fixture.cjs');
execute('documentos-cargos-cauda-real', async ({ page, context, fixture, open, check, screenshot, report }) => {
  const base = baseFrontend(), contract = contratoDaFixture(fixture), draft = contratoDaFixture(fixture, 'contrato_futuro');
  await open('/contratos');
  const get = async (route, params) => {
    const response = await context.request.get(base + route, { params });
    check('GET independente200 ' + route, response.status() === 200);
    return (await response.json()).data;
  };
  const documents = await get(contract.api + '/documentos');
  check('Cadeia real tem mais de cinco documentos e IDs distintos', documents.length > 5 && new Set(documents.map(d => d.id)).size === documents.length);
  await page.getByRole('button', { name: contract.botaoAbrir, exact: true }).click();
  const detail = page.getByRole('dialog', { name: contract.titulo, exact: true });
  const navigation = detail.getByRole('navigation', { name: 'Páginas de documentos', exact: true });
  const observed = [], pageCount = Math.ceil(documents.length / 5);
  for (let n = 0; n < pageCount; n++) {
    await navigation.getByText(`Página ${n + 1} de ${pageCount} · ${documents.length} registros`, { exact: true }).waitFor();
    const cards = navigation.locator('xpath=preceding-sibling::*[1]').locator(':scope > article');
    const expected = documents.slice(n * 5, n * 5 + 5);
    check('Página documental tem todas e somente as linhas esperadas ' + n, await cards.count() === expected.length);
    for (let i = 0; i < expected.length; i++) {
      const d = expected[i], card = cards.nth(i);
      check('Cartão corresponde à versão e título do DTO ' + d.id,
        (await card.innerText()).includes(d.titulo) && (await card.innerText()).includes('v' + d.versaoDocumento));
      const pending = page.waitForResponse(r => r.request().method() === 'GET'
        && new URL(r.url()).pathname === contract.api + '/documentos/' + d.id);
      pending.catch(() => {});
      await card.getByRole('button', { name: 'Detalhes', exact: true }).click();
      const response = await pending, actual = (await response.json()).data;
      check('Detalhe real identifica exatamente o ID e cadeia selecionados ' + d.id, response.status() === 200
        && actual.id === d.id && actual.grupoLogicoId === d.grupoLogicoId && actual.versaoDocumento === d.versaoDocumento);
      await detail.getByRole('heading', { name: d.titulo + ' · cadeia documental', exact: true }).waitFor();
      observed.push(actual.id);
    }
    if (n + 1 < pageCount) await navigation.getByRole('button', { name: 'Próxima', exact: true }).click();
  }
  check('Todos os documentos e a cauda foram percorridos sem IDs perdidos ou duplicados', JSON.stringify(observed) === JSON.stringify(documents.map(d => d.id))
    && new Set(observed).size === documents.length);
  await screenshot('documentos-ultima-pagina-com-cadeia');
  await page.keyboard.press('Escape'); await detail.waitFor({ state: 'hidden' });

  await open('/cargos');
  const all = await get('/api/cargos', { size: 100, sort: 'id,desc' });
  check('Catálogo de seis ou mais cargos próprios tem nomes e IDs únicos', all.totalElements >= 6
    && all.content.length === all.totalElements && new Set(all.content.map(c => c.nome)).size === all.totalElements);
  const cargos = [];
  for (let n = 0; n < Math.ceil(all.totalElements / 5); n++) {
    await page.getByText(`${all.totalElements} registro(s) · página ${n + 1} de ${Math.ceil(all.totalElements / 5)}`, { exact: true }).waitFor();
    const realPage = await get('/api/cargos', { page: n, size: 5, sort: 'id,desc' });
    const names = await page.getByRole('table').locator('tbody tr td:first-child').allTextContents();
    check('Página visual e paginação real da API concordam em todos os cargos ' + n,
      JSON.stringify(names) === JSON.stringify(realPage.content.map(c => c.nome)) && realPage.totalElements === all.totalElements);
    cargos.push(...realPage.content.map(c => c.id));
    if (n + 1 < Math.ceil(all.totalElements / 5)) await page.getByRole('button', { name: 'Próxima', exact: true }).click();
  }
  check('Catálogo completo mantém IDs sem perda ou duplicação', JSON.stringify(cargos) === JSON.stringify(all.content.map(c => c.id)) && new Set(cargos).size === all.totalElements);
  const tail = all.content.at(-1), search = page.getByRole('searchbox', { name: 'Buscar em Cargos', exact: true });
  await search.fill(tail.nome);
  check('Filtro explicitamente local encontra cargo da última página', await search.getAttribute('placeholder') === 'Buscar nesta página...'
    && await page.getByRole('table').locator('tbody tr').count() === 1 && (await page.getByRole('table').innerText()).includes(tail.nome));
  check('GET canônico confirma ID da opção tardia', (await get('/api/cargos/' + tail.id)).id === tail.id);
  await screenshot('cargo-cauda-paginada-e-filtrada');

  const before = await get(draft.api);
  await open('/contratos');
  await page.getByRole('button', { name: draft.botaoAbrir, exact: true }).click();
  const draftDetail = page.getByRole('dialog', { name: draft.titulo, exact: true });
  const form = draftDetail.getByRole('heading', { name: 'Adicionar composição ao rascunho', exact: true }).locator('..');
  const cargo = form.getByRole('combobox', { name: /^Cargo/ });
  await cargo.locator(`option[value="${tail.id}"]`).waitFor({ state: 'attached' });
  const options = await cargo.locator('option').evaluateAll(nodes => nodes.filter(n => Number(n.value) > 0).map(n => Number(n.value)));
  check('Seletor de composição oferece todas as opções reais incluindo cargo posterior à primeira página', options.length === all.totalElements
    && all.content.every(c => options.includes(c.id)) && new Set(options).size === all.totalElements);
  await cargo.selectOption(String(tail.id));
  check('Opção tardia é selecionável pelo ID correto', await cargo.inputValue() === String(tail.id));
  await screenshot('cargo-tardio-disponivel-no-formulario');
  await page.keyboard.press('Escape'); await draftDetail.waitFor({ state: 'hidden' });
  const after = await get(draft.api);
  check('Seleção sem confirmação preserva contrato e nenhuma mutação foi enviada', before.version === after.version && before.estadoNegocial === after.estadoNegocial
    && !report.http.some(h => ['POST', 'PUT', 'PATCH', 'DELETE'].includes(h.method) && !h.path.startsWith('/api/autenticacao/')));
  report.scope = 'AUXILIARY_ROP042_DOCUMENTS_AND_LATE_CARGOS_CATALOGUE_AND_COMPOSITION_OPTION_NOT_NEW_ORIGINAL_DRIVER';
  report.population = { documents: documents.length, documentIds: observed, cargos: all.totalElements, cargoIds: cargos, lateOption: tail.id, pageSize: 5 };
}, { reuse: false });
