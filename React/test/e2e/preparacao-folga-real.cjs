// Native synthetic preparation only. This is not an additional original E2E driver.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { baseFrontend } = require('./contexto-fixture.cjs');
const { execute } = require('./ambiente-real.cjs');
const unwrap = value => value && Object.hasOwn(value, 'data') ? value.data : value;
const rows = value => Array.isArray(value) ? value : value?.content || [];
const normalizedCpf = value => String(value || '').replace(/\D/g, '');
const hash = value => crypto.createHash('sha256').update(JSON.stringify(value)).digest('hex');
execute('preparacao-folga-real', async ({ page, context, fixture, account, report, check, screenshot, open, confirmPassword, recordBusiness }) => {
  report.acceptanceScope = 'PREPARATION_ONLY_NOT_ACCEPTANCE';
  const runtime = process.env.SGT_TEST_RUNTIME;
  const business = JSON.parse(fs.readFileSync(path.join(runtime, 'private/e2e-business.json')));
  const reconciliationPath = process.env.SGT_FOLGA_SOURCE_RECONCILIATION
    || path.join(path.dirname(runtime), 'db-browser-after-refused-op01/db-proof.json');
  const reconciliation = JSON.parse(fs.readFileSync(reconciliationPath));
  const employee = reconciliation.observed.employee_projection;
  const data = fixture.pending_vinculo_request.dataInicio;
  check('Pré-condição readonly liga a projeção à Pessoa canônica e comprova frequência ausente', reconciliation.passed && reconciliation.observed.transaction_read_only
    && reconciliation.observed.frequencies.length === 0 && employee.person_id === business.pessoaId && Number.isSafeInteger(employee.id));
  check('Não há fonte desta preparação já registrada para repetir', !business.frequenciaFolgaFonteId);
  const purpose = `SINTETICO-FOLGA-V${business.vinculoId}-${data}: fonte individual da jornada canônica de homologação.`;
  report.conditions = { preparationOnly: true, employeeId: employee.id, personId: business.pessoaId, vinculoId: business.vinculoId,
    date: data, type: 'FOLGA_REMUNERADA', noJornadaClockOrCalendarMutation: true };
  report.externalReads = [];
  const frequencyPosts = [];
  page.on('request', request => {
    const pathname = new URL(request.url()).pathname;
    if (request.method() !== 'GET' && ['/api/frequencias', '/api/frequencias/lote'].includes(pathname)) frequencyPosts.push(pathname);
  });
  const get = async (pathname, params) => {
    const response = await context.request.get(baseFrontend() + pathname, { params });
    report.externalReads.push({ path: pathname, status: response.status() });
    check(`GET independente${pathname} responde200`, response.status() === 200);
    return unwrap(await response.json());
  };
  const filter = { data, lotacaoId: String(fixture.ids.lotacao), jornadaId: String(fixture.ids.jornada), contratoId: String(fixture.pending_vinculo_request.contratoId), page: '0', size: '100' };
  const frequencyAbsent = async () => !rows(await get('/api/frequencias', filter)).some(item => item.vinculoContratualId === business.vinculoId && item.dataReferencia === data);
  await open('/frequencia-ajustes');
  const profile = await get('/api/usuarios/meu-perfil');
  check('Gestor Local atual possui a Gestão do alvo sem alterar privilégios', profile.id === account.id && profile.perfil === 'GESTOR_LOCAL' && profile.gestaoLocal?.id === fixture.ids.gestao_origem);
  check('Frequência ainda ausente antes de configurar sua fonte', await frequencyAbsent());
  const before = rows(await get('/api/frequencias-excecoes', filter));
  check('Nenhuma folga individual anterior será duplicada', !before.some(item => item.empregadoId === employee.id && item.tipo === 'FOLGA_REMUNERADA'));
  const jornadaBefore = hash(await get('/api/jornadas'));
  await page.getByRole('button', { name: 'Novo ajuste', exact: true }).click();
  const modal = page.getByRole('dialog', { name: 'Exceção de frequência', exact: true });
  await modal.waitFor();
  const search = page.waitForResponse(response => response.request().method() === 'GET'
    && new URL(response.url()).pathname === '/api/empregados'
    && normalizedCpf(new URL(response.url()).searchParams.get('search')) === normalizedCpf(fixture.pending_vinculo_request.pessoa.cpf));
  await modal.getByPlaceholder('Digite ao menos 2 caracteres', { exact: true }).fill(fixture.pending_vinculo_request.pessoa.cpf);
  const searchResponse = await search;
  const matches = rows(unwrap(await searchResponse.json()));
  check('Busca real por CPF retorna somente a projeção canônica comprovada', searchResponse.status() === 200 && matches.length === 1
    && matches[0].id === employee.id && normalizedCpf(matches[0].cpf) === normalizedCpf(fixture.pending_vinculo_request.pessoa.cpf));
  const match = matches[0];
  const label = [match.nome || `Empregado ${match.id}`, match.matricula ? `· ${match.matricula}` : ''].filter(Boolean).join(' ');
  const option = modal.getByRole('button', { name: label, exact: true });
  check('A opção exibida identifica unicamente a projeção procurada', await option.count() === 1);
  await option.click();
  await modal.getByLabel('Início', { exact: true }).fill(data);
  await modal.getByLabel('Fim', { exact: true }).fill(data);
  await modal.getByLabel('Tipo', { exact: true }).fill('FOLGA_REMUNERADA');
  await modal.getByLabel('Status opcional', { exact: true }).fill('APROVADO');
  await modal.getByLabel('Observação', { exact: true }).fill(purpose);
  await screenshot('fonte-individual-antes-confirmacao');
  const saved = page.waitForResponse(response => response.request().method() === 'POST' && new URL(response.url()).pathname === '/api/frequencias-excecoes');
  await modal.getByRole('button', { name: 'Salvar', exact: true }).click();
  await confirmPassword();
  const response = await saved;
  const created = unwrap(await response.json());
  if (response.status() === 201 && Number.isSafeInteger(created.id)) recordBusiness({ frequenciaFolgaFonteId: created.id,
    frequenciaFolgaEmpregadoId: employee.id, frequenciaFolgaFonteData: data });
  check('Fonte individual confirmada201 pelo Java com período e identidade exatos', response.status() === 201 && Number.isSafeInteger(created.id)
    && created.empregadoId === employee.id && created.dataInicio === data && created.dataFim === data
    && created.tipo === 'FOLGA_REMUNERADA' && created.status === 'APROVADO' && created.observacao === purpose);
  await modal.waitFor({ state: 'hidden' });
  const after = rows(await get('/api/frequencias-excecoes', filter));
  check('Releitura independente confirma uma única fonte observada', after.filter(item => item.id === created.id && item.empregadoId === employee.id && item.observacao === purpose).length === 1);
  const history = await get(`/api/frequencias-excecoes/${created.id}/eventos`);
  check('Histórico real da fonte contém sua criação', Array.isArray(history) && history.length === 1);
  check('Jornadas mantêm os mesmos dados após criar a exceção individual', hash(await get('/api/jornadas')) === jornadaBefore);
  check('Configurar a fonte não lançou frequência', frequencyPosts.length === 0 && await frequencyAbsent());
  report.source = { id: created.id, employeeId: employee.id, start: data, end: data, type: created.tipo, status: created.status, historyEntries: history.length };
  await screenshot('fonte-individual-confirmada');
}, { accountName: 'gestor_local', reuse: false });
