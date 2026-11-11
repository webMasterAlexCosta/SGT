// Auxiliar do E2E-01; mantém os vinte nomes originais separados desta prova.
const fs = require('node:fs');
const path = require('node:path');
const { baseFrontend } = require('./contexto-fixture.cjs');
const { execute } = require('./ambiente-real.cjs');
const cnpjDigit = digits => {
  const weights = digits.length === 12 ? [5,4,3,2,9,8,7,6,5,4,3,2] : [6,5,4,3,2,9,8,7,6,5,4,3,2];
  const remainder = [...digits].reduce((sum, n, index) => sum + Number(n) * weights[index], 0) % 11;
  return remainder < 2 ? '0' : String(11 - remainder);
};

execute('fornecedor-crud-real', async ({ page, context, fixture, report, check, open, confirmPassword, recordBusiness, screenshot }) => {
  const business = JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json')));
  check('Intenção CRUD ainda não executada', !business.fornecedorCrudFinal02Id);
  const base = baseFrontend();
  await open('/empresas-contratadas');
  await page.getByText('Carregando empresas...', { exact: true }).waitFor({ state: 'hidden' });
  const read = async () => {
    const response = await context.request.get(base + '/api/fornecedores');
    check('GET canônico independente de fornecedores', response.status() === 200);
    return (await response.json()).data;
  };
  const before = await read();
  const seeds = fixture.objects.fornecedores;
  const prefix = seeds[0].razaoSocial.replace(/-\d{2}$/, '');
  const six = before.filter(item => seeds.some(seed => seed.id === item.id)).sort((a,b) => a.razaoSocial.localeCompare(b.razaoSocial));
  check('Seis fornecedores reais da preparação permanecem distintos', seeds.length === 6 && six.length === 6 && new Set(six.map(item => item.cnpj)).size === 6);
  const search = page.getByRole('searchbox', { name: 'Pesquisar empresas', exact: true });
  await search.fill(prefix);
  const navigation = page.getByRole('navigation', { name: 'Paginação de empresas', exact: true });
  await navigation.getByText(/6 registro\(s\).*página 1 de 2/).waitFor();
  const rows = page.getByRole('table').locator('tbody tr');
  check('Primeira página mostra os cinco DTOs esperados', await rows.count() === 5 && JSON.stringify(await rows.locator('td:first-child').allTextContents()) === JSON.stringify(six.slice(0,5).map(item => item.razaoSocial)));
  await navigation.getByRole('button', { name: 'Próxima página de empresas', exact: true }).click();
  await rows.getByText(six[5].razaoSocial, { exact: true }).waitFor();
  check('Sexto fornecedor real acessível com CNPJ consistente', await rows.count() === 1 && (await rows.innerText()).includes(six[5].cnpj));
  await screenshot('sexto-fornecedor');
  await navigation.getByRole('button', { name: 'Página anterior de empresas', exact: true }).click();
  await navigation.getByText(/página 1 de 2/).waitFor();
  check('Retorno preserva as cinco primeiras linhas', await rows.count() === 5);
  await search.fill('');
  const cohort = String(fixture.ids.contrato_rascunho).padStart(2, '0');
  check('Identificador observado distingue a nova intenção sintética', /^\d{2}$/.test(cohort));
  const name = 'SINTETICA-E2E-CRUD-FINAL06-C' + cohort;
  let digits = '739955' + cohort + '0001'; digits += cnpjDigit(digits); digits += cnpjDigit(digits);
  const formatted = digits.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
  check('CNPJ sintético e intenção não existem antes do POST', before.every(item => item.cnpj !== digits && item.razaoSocial !== name));
  await page.getByLabel('Razão social da empresa', { exact: true }).fill(name);
  await page.getByLabel('CNPJ da empresa', { exact: true }).fill(formatted);
  await page.getByLabel('E-mail da empresa', { exact: true }).fill('crud-final06-c' + cohort + '@example.test');
  await page.getByRole('combobox', { name: 'Contrato a vincular à empresa', exact: true }).selectOption('');
  const createdPending = page.waitForResponse(r => new URL(r.url()).pathname === '/api/fornecedores' && r.request().method() === 'POST');
  createdPending.catch(() => {});
  await page.getByRole('button', { name: 'Cadastrar e vincular', exact: true }).click();
  await confirmPassword();
  const createdResponse = await createdPending;
  check('Criação por UI/senha retorna o HTTP200 definido no controlador', createdResponse.status() === 200);
  const created = (await createdResponse.json()).data;
  recordBusiness({ fornecedorCrudFinal02Id: created.id, fornecedorCrudFinal02Cnpj: digits, fornecedorCrudFinal02NomeInicial: name });
  check('Java normaliza CNPJ recebido com pontuação', createdResponse.request().postDataJSON().cnpj === formatted && created.cnpj === digits && created.razaoSocial === name && created.ativo === true);
  await page.getByLabel('Senha de Acesso', { exact: true }).waitFor({ state: 'hidden' });
  await search.fill(name);
  const row = page.getByRole('row').filter({ has: page.getByRole('cell', { name, exact: true }) });
  await row.waitFor();
  const editedName = name + '-EDITADA';
  const editedEmail = 'crud-final06-c' + cohort + '-editada@example.test';
  const prompts = [editedName, formatted, editedEmail];
  let promptCount = 0;
  const onDialog = async dialog => {
    if (dialog.type() !== 'prompt' || promptCount >= prompts.length) return dialog.dismiss();
    await dialog.accept(prompts[promptCount++]);
  };
  page.on('dialog', onDialog);
  const updatePath = '/api/fornecedores/' + created.id;
  const editedPending = page.waitForResponse(r => new URL(r.url()).pathname === updatePath && r.request().method() === 'PUT');
  editedPending.catch(() => {});
  try {
    await row.getByRole('button', { name: 'Editar', exact: true }).click();
    await confirmPassword();
    const updatedResponse = await editedPending;
    const edited = (await updatedResponse.json()).data;
    check('Três prompts reais e senha atualizam o mesmo fornecedor', promptCount === 3 && updatedResponse.status() === 200 && edited.id === created.id && edited.razaoSocial === editedName && edited.email === editedEmail && edited.cnpj === digits);
  } finally { page.off('dialog', onDialog); }
  await page.getByLabel('Senha de Acesso', { exact: true }).waitFor({ state: 'hidden' });
  await search.fill(editedName);
  const editedRow = page.getByRole('row').filter({ has: page.getByRole('cell', { name: editedName, exact: true }) });
  await editedRow.waitFor();
  for (const [action, active] of [['Inativar',false],['Reativar',true]]) {
    const pending = page.waitForResponse(r => new URL(r.url()).pathname === updatePath && r.request().method() === 'PUT');
    pending.catch(() => {});
    await editedRow.getByRole('button', { name: action, exact: true }).click();
    await confirmPassword();
    const response = await pending;
    const dto = (await response.json()).data;
    check(`${action}: PUT real preserva identidade e retorna estado solicitado`, response.status() === 200 && dto.id === created.id && dto.ativo === active && dto.cnpj === digits && dto.razaoSocial === editedName);
    await page.getByLabel('Senha de Acesso', { exact: true }).waitFor({ state: 'hidden' });
    await editedRow.getByRole('cell', { name: active ? 'ATIVO' : 'INATIVO', exact: true }).waitFor();
    const persisted = (await read()).filter(item => item.id === created.id);
    check(`${action}: tabela e GET independente concordam`, persisted.length === 1 && persisted[0].ativo === active && persisted[0].email === editedEmail);
    await screenshot(action.toLowerCase());
  }
  const after = await read();
  check('CRUD criou somente um fornecedor e preservou os seis preparados', after.length === before.length + 1 && seeds.every(seed => JSON.stringify(after.find(item => item.id === seed.id)) === JSON.stringify(before.find(item => item.id === seed.id))));
  recordBusiness({ fornecedorCrudFinal02NomeFinal: editedName, fornecedorCrudFinal02EmailFinal: editedEmail, fornecedorCrudFinal02Concluido: true });
  report.scope = 'E2E01_REAL_CRUD_AND_5_PLUS_1_NOT_AN_ADDITIONAL_ORIGINAL_DRIVER';
  report.receipt = { id: created.id, normalizedCnpj: digits, finalActive: true, created: 1, updates: 3 };
});
