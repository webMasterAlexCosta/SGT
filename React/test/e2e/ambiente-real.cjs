const { baseFrontend } = require('./contexto-fixture.cjs');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const runtime = process.env.SGT_TEST_RUNTIME;
if (!runtime) throw new Error('Defina SGT_TEST_RUNTIME para o ambiente sintético real preparado.');
const root = path.dirname(runtime);
const fixture = JSON.parse(fs.readFileSync(path.join(runtime, 'private/e2e-fixture.json')));
const evidence = process.env.SGT_E2E_OUTPUT || path.join(root, 'evidence/browser');
fs.mkdirSync(evidence, { recursive: true, mode: 0o700 });
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES + '/playwright');

async function execute(name, task, { accountName = 'gestor_local', reuse = true, persistent = false } = {}) {
  const latest = path.join(evidence, `${name}.json`);
  if (fs.existsSync(latest)) fs.copyFileSync(latest, path.join(evidence, `${name}-attempt-${fs.statSync(latest).mtimeMs.toFixed(0)}.json`));
  const report = { name, synthetic: true, started: new Date().toISOString(), frontendUrl: baseFrontend(), deploymentMode: process.env.SGT_E2E_MODE || 'desenvolvimento', frontendManifest: process.env.SGT_E2E_MANIFEST || null, assertions: [], http: [], errors: [] };
  const account = fixture.accounts[accountName];
  const state = path.join(runtime, 'private', `e2e-${accountName}-browser-state.json`);
  const executablePath = process.env.CHROMIUM_EXECUTABLE || path.join(runtime, 'chromium/chrome-linux64/chrome');
  const launchOptions = { executablePath, headless: true, chromiumSandbox: true,
    args: ['--disable-dev-shm-usage', '--js-flags=--max-old-space-size=256', '--renderer-process-limit=2'] };
  const contextOptions = { viewport: { width: 1440, height: 900 }, locale: 'pt-BR', timezoneId: process.env.SGT_E2E_TIMEZONE || 'America/Sao_Paulo' };
  let persistentContext = null;
  if (persistent) {
    assert.equal(reuse, false, 'Persistent native-zoom profile must start fresh.');
    const profile = path.join(runtime, 'private', `chrome-profile-${path.basename(path.dirname(evidence))}-${name}`);
    fs.mkdirSync(profile, {mode:0o700});
    persistentContext = await chromium.launchPersistentContext(profile, {...launchOptions,...contextOptions});
  }
  const browser = persistentContext ? persistentContext.browser() : await chromium.launch(launchOptions);
  report.browser = { engine: 'Google Chrome/Chromium desktop', version: browser.version(), executablePath, chromiumSandbox: true,
    viewport: { width: 1440, height: 900 }, timezone: process.env.SGT_E2E_TIMEZONE || 'America/Sao_Paulo' };
  let page;
  try {
    const context = persistentContext || await browser.newContext({ ...contextOptions, ...(reuse && fs.existsSync(state) ? { storageState: state } : {}) });
    page = context.pages()[0] || await context.newPage();
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
      const temporary = `${file}.tmp-${process.pid}-${Date.now()}`;
      const descriptor = fs.openSync(temporary, 'wx', 0o600);
      try {
        fs.writeFileSync(descriptor, JSON.stringify({ ...old, ...fields }, null, 2));
        fs.fsyncSync(descriptor);
      } finally { fs.closeSync(descriptor); }
      fs.renameSync(temporary, file);
      const directory = fs.openSync(path.dirname(file), 'r');
      try { fs.fsyncSync(directory); } finally { fs.closeSync(directory); }
    };
    const secretMasks = () => [
      page.locator('input[type="password"], input[autocomplete="one-time-code"]'),
      page.getByAltText(/QR|código.*autentica/i),
      page.getByRole('button', { name: /^(Copiar chave|Copiada)$/ }).locator('..'),
      page.locator('details').filter({ has: page.getByText('Mostrar URI de configuração avançada', { exact: true }) }),
    ];
    const screenshot = async label => {
      const file = `${name}-${label}.png`;
      await page.screenshot({ path: path.join(evidence, file), fullPage: true,
        mask: secretMasks() });
      fs.chmodSync(path.join(evidence, file), 0o600);
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
    fs.chmodSync(state, 0o600);
    report.passed = true;
  } catch (error) {
    report.passed = false; report.failure = error.message; process.exitCode = 1;
    if (page) {
      report.url = page.url();
      const secretSetupVisible = (await page.getByAltText(/QR|código.*autentica/i).count().catch(() => 0))
        || (await page.getByRole('button', { name: /^(Copiar chave|Copiada)$/ }).count().catch(() => 0));
      report.body = secretSetupVisible ? '[Configuração de autenticador omitida para preservar segredo/URI.]'
        : (await page.locator('body').innerText().catch(() => '')).slice(0, 16000);
      await page.screenshot({ path: path.join(evidence, `${name}-failure.png`), fullPage: true,
        mask: [page.locator('input[type="password"], input[autocomplete="one-time-code"]'),
          page.getByAltText(/QR|código.*autentica/i),
          page.getByRole('button', { name: /^(Copiar chave|Copiada)$/ }).locator('..'),
          page.locator('details').filter({ has: page.getByText('Mostrar URI de configuração avançada', { exact: true }) })] }).catch(() => {});
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
    fs.writeFileSync(path.join(evidence, `${name}.json`), content, { mode: 0o600 });
    console.log(content);
  }
}
module.exports = { execute };
