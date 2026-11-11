// Preparação causal do corpus isolado; não é um dos vinte drivers de aceite.
const { contratoDaFixture, idObservado } = require('./contexto-fixture.cjs');
const { execute } = require('./ambiente-real.cjs');

execute('preparacao-fonte-historica-real', async ({ page, context, fixture, account, report, check, open, confirmPassword, recordBusiness, screenshot }) => {
  report.scope = 'PREPARATION_ONLY_NOT_ACCEPTANCE';
  const contract = contratoDaFixture(fixture);
  const beneficiary = idObservado(fixture.accounts.gestor_local?.id, 'gestor_local');
  const admin = idObservado(fixture.accounts.fixture_admin?.id, 'fixture_admin');
  const type = 'INCORPORACAO_PROVA_HISTORICA';
  const reference = `SINTETICO-E2E-FONTE-HISTORICA-CONTRATO-${contract.id}`;
  check('Administrador sintético designado para preparação', account.id === admin && account.perfil === 'ADMINISTRADOR');
  await open('/permissoes-excepcionais');
  await page.getByRole('heading', { name: 'Permissões excepcionais', exact: true }).waitFor();
  const api = route => new URL('/api' + route, page.url()).href;
  const response = await context.request.get(api('/permissoes-excepcionais'));
  check('Lista administrativa real disponível', response.status() === 200);
  const grants = (await response.json()).data;
  const sameScope = grants.filter(item => item.tipo === type && item.usuarioBeneficiarioId === beneficiary && item.contratoId === contract.id && item.ativa);
  check('Não existe concessão alheia ao preparo no mesmo escopo', sameScope.every(item => item.autorizacaoAdministrativa === reference));
  let grant = sameScope[0];
  if (!grant) {
    await page.getByRole('button', { name: 'Nova permissão', exact: true }).click();
    const form = page.getByRole('dialog', { name: 'Conceder permissão excepcional', exact: true });
    await form.getByRole('combobox', { name: 'Tipo', exact: true }).selectOption(type);
    await form.getByRole('combobox', { name: 'Beneficiário', exact: true }).selectOption(String(beneficiary));
    await form.getByRole('combobox', { name: 'Contrato (quando aplicável)', exact: true }).selectOption(String(contract.id));
    await form.getByRole('combobox', { name: 'Gestão Local (quando aplicável)', exact: true }).selectOption('');
    const expiry = await page.evaluate(() => {
      const date = new Date(Date.now() + 24 * 60 * 60 * 1000);
      const p = value => String(value).padStart(2, '0');
      return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}T${p(date.getHours())}:${p(date.getMinutes())}`;
    });
    await form.getByLabel('Validade', { exact: true }).fill(expiry);
    await form.getByLabel('Finalidade', { exact: true }).fill('SINTETICO E2E: incorporar somente fontes históricas do contrato sintético observado nesta campanha.');
    await form.getByLabel('Justificativa', { exact: true }).fill('Preparação autorizada do laboratório isolado para os drivers originais; sem validade administrativa real.');
    await form.getByLabel('Autorização administrativa', { exact: true }).fill(reference);
    const pending = page.waitForResponse(r => new URL(r.url()).pathname === '/api/permissoes-excepcionais' && r.request().method() === 'POST', { timeout: 20000 });
    pending.catch(() => {});
    await form.getByRole('button', { name: 'Conceder', exact: true }).click();
    await confirmPassword();
    const created = await pending;
    const envelope = await created.json();
    check('Administrador concede pelo React com senha e Java201', created.status() === 201, { status: created.status(), code: envelope.code });
    grant = envelope.data;
    report.createdByUi = true;
  }
  check('Concessão específica ativa, temporária, sem escopo global', grant.tipo === type && grant.usuarioBeneficiarioId === beneficiary && grant.contratoId === contract.id && grant.gestaoLocalId == null && grant.ativa === true && Date.parse(grant.validoAte) > Date.now());
  check('Concessão identifica Administrador, finalidade e justificativa', grant.concedidoPorId === admin && !!grant.finalidade && !!grant.justificativa && grant.autorizacaoAdministrativa === reference);
  const persistedResponse = await context.request.get(api('/permissoes-excepcionais'));
  const persisted = (await persistedResponse.json()).data;
  check('GET independente confirma uma única concessão vigente no escopo', persistedResponse.status() === 200 && persisted.filter(item => item.id === grant.id && item.ativa && item.tipo === type).length === 1);
  recordBusiness({ permissaoFonteHistoricaId: grant.id, permissaoFonteHistoricaContratoId: contract.id, permissaoFonteHistoricaUsuarioId: beneficiary, permissaoFonteHistoricaReferencia: reference, permissaoFonteHistoricaValidoAte: grant.validoAte });
  await page.getByLabel('Senha de Acesso', { exact: true }).waitFor({ state: 'hidden' });
  const card = page.locator('article').filter({ hasText: reference });
  await card.getByText('Ativa', { exact: true }).waitFor();
  await screenshot('concessao-preparada');
}, { accountName: 'fixture_admin' });
