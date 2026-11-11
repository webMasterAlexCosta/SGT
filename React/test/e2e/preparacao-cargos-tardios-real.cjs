'use strict';
const fs = require('node:fs');
const path = require('node:path');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend } = require('./contexto-fixture.cjs');
execute('preparacao-cargos-tardios-real', async ({ page, context, fixture, open, check, confirmPassword, recordBusiness, report }) => {
  const b = JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json')));
  const ids = [...(b.demo64Cargos || [])], base = baseFrontend();
  await open('/cargos');
  const list = await context.request.get(base + '/api/cargos', { params: { size: 100 } });
  const initial = (await list.json()).data;
  check('Catálogo próprio e prefixo confirmado, sem repetir criação', list.status() === 200 && ids.length <= 4
    && ids.every(id => initial.content.some(c => c.id === id && c.gestaoLocalId === fixture.ids.gestao_origem))
    && initial.content.filter(c => c.nome.startsWith('SINTETICO DEMO64 CARGO ')).every(c => ids.includes(c.id)));
  for (let i = ids.length; i < 4; i++) {
    await page.getByRole('button', { name: 'Novo', exact: true }).click();
    const form = page.getByRole('dialog', { name: 'Novo registro', exact: true });
    await form.getByRole('textbox', { name: /^Nome/ }).fill('SINTETICO DEMO64 CARGO ' + String(i + 1).padStart(2, '0'));
    await form.getByLabel('Descrição', { exact: true }).fill('Preparação sintética de opções além da página visual; sem valor administrativo.');
    const pending = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname === '/api/cargos');
    pending.catch(() => {});
    await form.getByRole('button', { name: 'Salvar', exact: true }).click();
    await confirmPassword();
    const response = await pending, data = (await response.json()).data;
    if (response.status() === 200 && data.id > 0) { ids.push(data.id); recordBusiness({ demo64Cargos: [...ids] }); }
    check('Cargo sintético criado pela UI no escopo atual ' + (i + 1), response.status() === 200 && data.id > 0
      && data.ativo === true && data.gestaoLocalId === fixture.ids.gestao_origem);
    await form.waitFor({ state: 'hidden' });
  }
  const after = (await (await context.request.get(base + '/api/cargos', { params: { size: 100 } })).json()).data;
  check('Existem seis cargos próprios, todos os anteriores preservados', after.totalElements === initial.totalElements + 4 - (b.demo64Cargos || []).length
    && after.totalElements >= 6 && initial.content.every(c => after.content.some(a => a.id === c.id && a.nome === c.nome)));
  report.scope = 'PREPARATION_ONLY_FOUR_SYNTHETIC_CARGOS_UI_NO_ACCEPTANCE_OF_PAGINATION';
}, { reuse: false });
