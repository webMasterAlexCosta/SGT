const { baseFrontend } = require('./contexto-fixture.cjs');
// Mesmo oráculo RED/GREEN: busca usada pela tela deve responder200 e permitir
// selecionar uma identidade retornada pelo servidor, sem substituir autorização.
const { execute } = require('./ambiente-real.cjs');
const unwrap = b => b && Object.hasOwn(b, 'data') ? b.data : b;
const digits = v => String(v || '').replace(/\D/g, '');
execute('pos-auditoria-operacional', async ({ page, context, fixture, check, open, report }) => {
  await open('/ocorrencias');
  await page.getByRole('heading', { name: 'Ocorrências', exact: true }).waitFor();
  check('Operacional abre a rota autorizada', page.url().endsWith('/ocorrencias'));
  const cpf = fixture.pending_vinculo_request.pessoa.cpf;
  const date = page.getByLabel('Data de referência da busca', { exact: true });
  if (await date.count()) await date.fill(fixture.dates.today);
  const scope = page.getByLabel('Lotação e turno', { exact: true });
  if (await scope.count()) await scope.selectOption(`${fixture.ids.lotacao}:${fixture.ids.jornada}`);
  // Esta consulta é somente uma asserção externa de existência; seus dados não
  // são injetados na UI. O Java continua autorizando cada operação.
  const canonical = await context.request.get((baseFrontend()) + '/api/empregados/filtrados-frequencia', {
    params: { dataRef: fixture.dates.today, lotacaoId: String(fixture.ids.lotacao), turnoId: String(fixture.ids.jornada), searchTerm: cpf },
  });
  const available = unwrap(await canonical.json());
  check('Java oferece identidade sintética real no escopo Operacional', canonical.status() === 200 && available.totalElements > 0);
  const pending = page.waitForResponse(res => {
    const u = new URL(res.url());
    return ['/api/empregados', '/api/empregados/filtrados-frequencia'].includes(u.pathname)
      && res.request().method() === 'GET' && (u.searchParams.get('search') === cpf || u.searchParams.get('searchTerm') === cpf);
  });
  await page.getByPlaceholder('Nome ou CPF...', { exact: true }).fill(cpf);
  const response = await pending;
  report.search = { url: new URL(response.url()).pathname, status: response.status() };
  check('Busca usada pela tela é autorizada e responde200', response.status() === 200, report.search);
  const rows = unwrap(await response.json());
  // O Java minimiza CPF para Operacional; a prova usa o ID retornado, sem exigir exposição do CPF.
  const expectedId = (Array.isArray(available) ? available : available.content || [])[0]?.id;
  const employee = (Array.isArray(rows) ? rows : rows.content || []).find(e => e.id === expectedId);
  check('Resultado da UI identifica o empregado sintético consultado', !!employee?.id);
  const history = page.waitForResponse(res => {
    const u = new URL(res.url());
    return u.pathname === '/api/ocorrencias' && u.searchParams.get('empregadoId') === String(employee.id);
  });
  await page.getByRole('button').filter({ has: page.getByText(employee.nome, { exact: true }) }).click();
  const actual = await history;
  check('Seleção consulta histórico funcional permitido pelo Java', actual.status() === 200);
  await page.getByRole('heading', { name: 'Histórico funcional', exact: true }).waitFor();
  check('Busca corrigida não recorre ao endpoint vedado', !report.http.some(r => r.path === '/api/empregados'));
}, { accountName: 'audit_operacional', reuse: false });
