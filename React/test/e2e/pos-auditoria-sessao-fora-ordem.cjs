// Respostas reais do JAR com entrega deliberadamente adiada pelo Playwright.
// Não substitui a prova determinística de transporte não cancelável do Vitest.
const { execute } = require('./ambiente-real.cjs');
execute('pos-auditoria-sessao-fora-ordem', async ({ page, context, fixture, check, open, report }) => {
  const b = fixture.accounts.gestor_destino;
  const loginB = async () => {
    await page.getByRole('button', { name: /Entrar no Sistema/i }).waitFor({ timeout: 20000 });
    await page.getByLabel(/^Matrícula$/i).fill(b.matricula);
    await page.getByLabel(/^Senha$/i).fill(b.password);
    await page.getByRole('button', { name: /Entrar no Sistema/i }).click();
    await page.getByRole('button', { name: 'Sair', exact: true }).waitFor({ timeout: 20000 });
    await page.getByText('CLIENTE-DIAGNÓSTICO', { exact: true }).waitFor({ state: 'hidden', timeout: 15000 });
  };
  const logout = async () => {
    await page.getByRole('button', { name: 'Sair', exact: true }).click();
    await page.getByRole('button', { name: 'Frequência diária', exact: true }).waitFor({ state: 'hidden', timeout: 5000 });
    check('Logout remove imediatamente a navegação protegida', await page.getByRole('button', { name: 'Frequência diária', exact: true }).count() === 0);
    await page.getByRole('button', { name: /Entrar no Sistema/i }).waitFor({ timeout: 20000 });
  };
  report.delayed = [];
  for (const mode of ['sucesso-A-antigo', 'sessao-A-revogada']) {
    await open('/contratos');
    let capture, release, finish;
    const captured = new Promise(resolve => { capture = resolve; });
    const gate = new Promise(resolve => { release = resolve; });
    const finished = new Promise(resolve => { finish = resolve; });
    let intercepted = false;
    const pattern = '**/api/usuarios/meu-perfil';
    const handler = async route => {
      if (intercepted) return route.continue();
      intercepted = true;
      const request = route.request();
      let response;
      try {
        if (mode === 'sucesso-A-antigo') response = await context.request.fetch(request);
        capture();
        await gate;
        if (!response) response = await context.request.fetch(request);
        const status = response.status();
        let delivered = true;
        try { await route.fulfill({ response }); } catch { delivered = false; }
        report.delayed.push({ mode, status, delivered, expected: mode === 'sucesso-A-antigo' ? 200 : 401 });
      } finally { finish(); }
    };
    await page.route(pattern, handler);
    await page.getByRole('button', { name: 'Frequência diária', exact: true }).click();
    await captured;
    await logout();
    await loginB();
    release();
    await finished;
    await page.unroute(pattern, handler);
    const last = report.delayed.at(-1);
    check(`${mode}: a resposta vem do Java com o status esperado`, last.status === last.expected, last);
    check(`${mode}: resposta antiga não substitui a identidade B`, (await page.locator('body').innerText()).includes(b.username)
      && !(await page.locator('body').innerText()).includes(fixture.accounts.gestor_local.username));
    check(`${mode}: sessão B continua autenticada`, await page.getByRole('button', { name: 'Sair', exact: true }).isVisible());
    await logout();
  }
  // O harness grava storageState da conta A; termine novamente nessa identidade.
  await open('/contratos');
  check('Novo login A continua possível após duas trocas completas', (await page.locator('body').innerText()).includes(fixture.accounts.gestor_local.username));
}, { accountName: 'gestor_local', reuse: false });
