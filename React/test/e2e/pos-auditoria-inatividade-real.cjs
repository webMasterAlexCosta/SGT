// Java real com configuração EXTERNA JWT_MAX_INACTIVITY_MINUTES=1.
// Nenhuma alteração de relógio, token ou linha persistida é usada neste teste.
const { execute } = require('./ambiente-real.cjs');
execute('pos-auditoria-inatividade-real', async ({ page, account, report, check, open, screenshot }) => {
  if (process.env.SGT_IDLE_TIMEOUT_MINUTES !== '1') throw new Error('Configure externamente o JAR para 1 minuto e declare SGT_IDLE_TIMEOUT_MINUTES=1.');
  let loginAt, resolveIdle, rejectIdle;
  const idle = new Promise((resolve, reject) => { resolveIdle = resolve; rejectIdle = reject; });
  const timer = setTimeout(() => rejectIdle(new Error('O Java não respondeu SESSION_IDLE_TIMEOUT em 140 segundos reais.')), 140000);
  const idleResponses = [];
  const held = [];
  const sessionPattern = '**/config.json*';
  const holdBackground = route => { held.push(route); };
  page.on('response', async response => {
    const path = new URL(response.url()).pathname;
    if (path === '/api/autenticacao/autenticar' && response.status() === 200) loginAt = Date.now();
    if (!path.startsWith('/api/') || response.status() < 400) return;
    try {
      const body = await response.json();
      if (body.code !== 'SESSION_IDLE_TIMEOUT') return;
      const observed = { path, status: response.status(), code: body.code, message: body.message, receivedAt: Date.now(), millisecondsAfterLogin: Date.now() - loginAt };
      idleResponses.push(observed);
      resolveIdle(observed);
    } catch {}
  });
  try {
    await open('/contratos');
    check('Sessão autenticada visível antes da espera real', await page.getByRole('button', { name: 'Sair', exact: true }).isVisible());
    // A consulta pública de latência também atravessa o filtro JWT. Retê-la
    // permite observar a PRIMEIRA expiração na consulta real de sessão (401),
    // em vez de consumir o token na rota pública e observar SESSION_REPLACED.
    await page.route(sessionPattern, holdBackground);
    report.conditions = { externalJavaIdleMinutes: 1, browserIdleMinutes: 15, realClock: true, tokenOrDatabaseModified: false, synthetic: true, publicLatencyRequestsHeld: true, sessionPollingUnmodified: true };
    const first = await idle;
    report.firstIdleResponse = first;
    report.heldBackgroundRequests = held.length;
    check('Primeira resposta pertinente contém HTTP401 e o código autoritativo do Java', first.status === 401 && first.code === 'SESSION_IDLE_TIMEOUT' && first.path === '/api/autenticacao/sessao');
    check('O prazo decorreu sem acelerar relógios nem alterar persistência', first.millisecondsAfterLogin >= 60000);
    await page.getByRole('button', { name: /Entrar no Sistema/i }).waitFor({ timeout: 5000 });
    report.localInvalidationMilliseconds = Date.now() - first.receivedAt;
    check('A primeira resposta de inatividade encerra a interface protegida', await page.getByRole('button', { name: 'Sair', exact: true }).count() === 0 && await page.getByRole('button', { name: 'Frequência diária', exact: true }).count() === 0);
    check('A identificação anterior foi retirada da tela', !(await page.locator('body').innerText()).includes(account.username));
    check('A indicação de inatividade permanece acessível', /inatividade/i.test(await page.locator('body').innerText()));
    report.screenshot = await screenshot('primeira-resposta-encerrou-interface');
    report.idleResponseCountBeforeNewLogin = idleResponses.length;
    check('Uma única resposta de inatividade foi suficiente', idleResponses.length === 1);
    await page.unroute(sessionPattern, holdBackground);
    for (const route of held) await route.abort().catch(() => {});
    await page.getByLabel(/^Matrícula$/i).fill(account.matricula);
    await page.getByLabel(/^Senha$/i).fill(account.password);
    await page.getByRole('button', { name: /Entrar no Sistema/i }).click();
    await page.getByRole('button', { name: 'Sair', exact: true }).waitFor({ timeout: 20000 });
    check('Novo login explícito permanece funcional depois da expiração', await page.getByRole('button', { name: 'Sair', exact: true }).isVisible());
  } finally {
    clearTimeout(timer);
    await page.unroute(sessionPattern, holdBackground);
    for (const route of held) await route.abort().catch(() => {});
  }
}, { accountName: 'gestor_local', reuse: false });
