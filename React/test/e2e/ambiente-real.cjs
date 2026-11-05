const { baseFrontend } = require('./contexto-fixture.cjs');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const runtime = process.env.SGT_TEST_RUNTIME;
if (!runtime) throw new Error('Defina SGT_TEST_RUNTIME para o ambiente sintético real preparado.');
const root = path.dirname(runtime);
const fixture = JSON.parse(fs.readFileSync(path.join(runtime, 'private/e2e-fixture.json')));
const evidence = process.env.SGT_E2E_OUTPUT || path.join(root, 'evidence/browser');
fs.mkdirSync(evidence, { recursive: true });
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES + '/playwright');

async function execute(name, task, { accountName = 'gestor_local', reuse = true } = {}) {
  const latest = path.join(evidence, `${name}.json`);
  if (fs.existsSync(latest)) fs.copyFileSync(latest, path.join(evidence, `${name}-attempt-${fs.statSync(latest).mtimeMs.toFixed(0)}.json`));
  const report = { name, synthetic: true, started: new Date().toISOString(), frontendUrl: baseFrontend(), deploymentMode: process.env.SGT_E2E_MODE || 'desenvolvimento', frontendManifest: process.env.SGT_E2E_MANIFEST || null, assertions: [], http: [], errors: [] };
  const account = fixture.accounts[accountName];
  const state = path.join(runtime, 'private', `e2e-${accountName}-browser-state.json`);
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_EXECUTABLE || path.join(runtime, 'chromium/chrome-linux64/chrome'), headless: true, args: ['--disable-dev-shm-usage'] });
  let page;
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, locale: 'pt-BR', timezoneId: 'America/Sao_Paulo', ...(reuse && fs.existsSync(state) ? { storageState: state } : {}) });
    page = await context.newPage();
    page.on('pageerror', error => report.errors.push(error.message));
    page.on('response', async response => {
      const url = new URL(response.url());
      if (!url.pathname.startsWith('/api/')) return;
      const item = { method: response.request().method(), path: url.pathname, status: response.status() };
      report.http.push(item);
      if (response.status() >= 400) {
        try { const body = await response.json(); item.code = body.code; item.message = body.message; } catch {}
      }
    });
    const check = (description, condition, detail) => {
      assert.ok(condition, description);
      report.assertions.push({ description, passed: true, ...(detail ? { detail } : {}) });
    };
    const recordBusiness = fields => {
      const file = path.join(runtime, 'private/e2e-business.json');
      const old = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file)) : {};
      fs.writeFileSync(file, JSON.stringify({ ...old, ...fields }, null, 2));
    };
    const screenshot = async label => {
      const file = `${name}-${label}.png`;
      await page.screenshot({ path: path.join(evidence, file), fullPage: true });
      return file;
    };
    const dismissIntro = async () => {
      await page.getByText('CLIENTE-DIAGNÓSTICO', { exact: true }).waitFor({ state: 'hidden', timeout: 15000 });
      const welcome = page.getByRole('button', { name: /Ciente, Vamos lá/i });
      if (await welcome.isVisible()) {
        await page.getByText(/Não mostrar por \d+ dias/i).click();
        await welcome.click();
      }
      const remind = page.getByRole('button', { name: /Lembrar em 7 dias/i });
      if (await remind.isVisible()) await remind.click();
    };
    const open = async route => {
      await page.goto((baseFrontend()) + route, { waitUntil: 'networkidle' });
      if (await page.getByRole('button', { name: /Entrar no Sistema/i }).isVisible()) {
        await page.getByLabel(/^Matrícula$/i).fill(account.matricula);
        await page.getByLabel(/^Senha$/i).fill(account.password);
        await page.getByRole('button', { name: /Entrar no Sistema/i }).click();
        await page.getByRole('button', { name: 'Sair', exact: true }).waitFor({ timeout: 20000 });
        await dismissIntro();
        await page.goto((baseFrontend()) + route, { waitUntil: 'networkidle' });
      }
      await dismissIntro();
    };
    const confirmPassword = async () => {
      const password = page.getByLabel('Senha de Acesso', { exact: true });
      await password.waitFor({ timeout: 10000 });
      await password.fill(account.password);
      const modal = page.getByRole('dialog').filter({ has: password });
      await modal.getByRole('button', { name: /^Confirmar/i }).click();
    };
    await task({ page, context, fixture, account, report, check, screenshot, open, confirmPassword, recordBusiness });
    check('Sem exceção JavaScript não tratada', report.errors.length === 0, report.errors);
    await context.storageState({ path: state });
    report.passed = true;
  } catch (error) {
    report.passed = false; report.failure = error.message; process.exitCode = 1;
    if (page) {
      report.url = page.url();
      report.body = (await page.locator('body').innerText().catch(() => '')).slice(0, 16000);
      await page.screenshot({ path: path.join(evidence, `${name}-failure.png`), fullPage: true }).catch(() => {});
    }
  } finally {
    await browser.close();
    report.finished = new Date().toISOString();
    const content = JSON.stringify(report, null, 2);
    for (const user of Object.values(fixture.accounts)) {
      for (const key of ['password', 'temporary_password']) {
        if (user[key] && content.includes(user[key])) throw new Error('Evidência contém credencial; publicação recusada.');
      }
    }
    fs.writeFileSync(path.join(evidence, `${name}.json`), content);
    console.log(content);
  }
}
module.exports = { execute };
