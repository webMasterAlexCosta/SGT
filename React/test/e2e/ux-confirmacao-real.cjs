'use strict';
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend, contratoDaFixture } = require('./contexto-fixture.cjs');
const hash = value => crypto.createHash('sha256').update(JSON.stringify(value)).digest('hex');
execute('ux-confirmacao-real', async ({ page, context, fixture, open, check, report, screenshot }) => {
  const contract = contratoDaFixture(fixture), base = baseFrontend();
  await open('/contratos');
  const get = async route => {
    const r = await context.request.get(base + route);
    check('GET íntegro ' + route, r.status() === 200);
    return (await r.json()).data;
  };
  const eventsBefore = await get(contract.api + '/eventos');
  const versionsBefore = await get(contract.api + '/versoes');
  const trigger = page.getByRole('button', { name: contract.botaoAbrir, exact: true });
  await trigger.click();
  const detail = page.getByRole('dialog', { name: contract.titulo, exact: true });
  const actionTrigger = detail.getByRole('button', { name: 'Inativar', exact: true });
  await actionTrigger.click();
  const action = page.getByRole('dialog', { name: 'Inativar administrativamente', exact: true });
  await action.waitFor();
  const focusedWithin = () => action.evaluate(el => el.contains(document.activeElement));
  check('Abertura transfere foco ao diálogo superior', await focusedWithin());
  for (let i = 0; i < 15; i++) {
    await page.keyboard.press(i % 3 === 0 ? 'Shift+Tab' : 'Tab');
    check('Tab e Shift+Tab permanecem no diálogo superior ' + i, await focusedWithin());
  }
  const reason = action.getByRole('textbox', { name: /Motivo\/justificativa/i });
  await reason.fill('Conferência sintética da confirmação cancelada; nenhum ato deve ser gravado.');
  const beforePreview = page.waitForResponse(r => new URL(r.url()).pathname === contract.api + '/previas/inativar' && r.request().method() === 'POST');
  beforePreview.catch(() => {});
  await action.getByRole('button', { name: 'Gerar prévia', exact: true }).click();
  check('Prévia verdadeira obtida do JAR antes da confirmação', (await beforePreview).status() === 200);
  await action.getByLabel(/Confirmo os impactos/).check();
  const proceed = action.getByRole('button', { name: 'Continuar e confirmar senha', exact: true });
  await proceed.click();
  const password = page.getByLabel('Senha de Acesso', { exact: true });
  await password.waitFor();
  const confirmation = page.getByRole('dialog').filter({ has: password });
  check('Confirmação mantém dois diálogos e foco no superior', await confirmation.count() === 1
    && await confirmation.evaluate(el => el.contains(document.activeElement)) && await action.isVisible());
  check('Intenção original fica bloqueada durante confirmação', await action.getByRole('button', { name: 'Gerar prévia', exact: true }).isDisabled()
    && await action.getByRole('button', { name: 'Cancelar', exact: true }).isDisabled()
    && await reason.isDisabled());
  await confirmation.getByRole('button', { name: 'Cancelar', exact: true }).click();
  await password.waitFor({ state: 'hidden' });
  check('Cancelar senha preserva formulário e permite nova decisão', await reason.inputValue() === 'Conferência sintética da confirmação cancelada; nenhum ato deve ser gravado.'
    && await action.getByRole('button', { name: 'Cancelar', exact: true }).isEnabled());
  await screenshot('confirmacao-cancelada');
  await page.keyboard.press('Escape');
  await action.waitFor({ state: 'hidden' });
  check('ESC fecha somente ação e devolve foco ao gatilho do detalhe', await detail.isVisible() && await actionTrigger.evaluate(el => el === document.activeElement));
  await page.keyboard.press('Escape');
  await detail.waitFor({ state: 'hidden' });
  check('ESC seguinte devolve foco à linha de contrato', await trigger.evaluate(el => el === document.activeElement));
  check('Nenhum comando de inativação foi enviado após cancelamento', !report.http.some(h => h.method === 'POST' && h.path === contract.api + '/inativar'));
  check('Todos os eventos e fotografias permanecem idênticos', hash(await get(contract.api + '/eventos')) === hash(eventsBefore)
    && hash(await get(contract.api + '/versoes')) === hash(versionsBefore));
  report.scope = 'AUXILIARY_E2E18_AND_E2E24_REAL_PREVIEW_CANCEL_FOCUS_AND_NO_COMMIT_NOT_ORIGINAL_DRIVER_NOT_LAN';
}, { reuse: false });
