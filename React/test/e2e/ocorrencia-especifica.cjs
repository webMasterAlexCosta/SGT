'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend } = require('./contexto-fixture.cjs');
const unwrap = value => value && Object.hasOwn(value, 'data') ? value.data : value;
const runtime = process.env.SGT_TEST_RUNTIME;
const businessFile = path.join(runtime, 'private/e2e-business.json');
const beneficiary = process.env.SGT_E2E_BENEFICIARY || 'nacional';
if (!['nacional','presidente'].includes(beneficiary)) throw new Error('Beneficiário sintético não autorizado neste roteiro.');
const key = 'ocorrencia_especifica_' + String(process.env.SGT_E2E_MODE).replace(/[^a-z0-9]/gi, '_') + '_' + beneficiary;
const business = () => JSON.parse(fs.readFileSync(businessFile));
const narrativa = 'Narrativa sintética restrita ao alvo, sem PII real';
async function consultar(page, fixture, expected) {
  const alvo = fixture.ocorrencia_restrita;
  const form = page.getByRole('form', { name: 'Consultar ocorrência por ID', exact: true });
  await form.getByLabel('ID da ocorrência', { exact: true }).fill(String(alvo.id));
  await form.getByLabel('Finalidade da consulta', { exact: true }).fill(alvo.grantRequest.finalidade);
  const responsePromise = page.waitForResponse(r => new URL(r.url()).pathname === `/api/ocorrencias/${alvo.id}` && r.request().method() === 'GET');
  await form.getByRole('button', { name: 'Consultar ocorrência', exact: true }).click();
  const response = await responsePromise;
  if (response.status() !== expected) throw new Error(`Leitura específica esperava${expected}; recebeu${response.status()}.`);
  return { form, response, dto: unwrap(await response.json()) };
}
(async () => {
  if(process.env.SGT_E2E_OCCURRENCE_CLEANUP_ONLY==='1') {
    await execute('ocorrencia-especifica-reconciliacao',async({page,context,fixture,check,open,recordBusiness,report})=>{
      const grant=business()[key],base=baseFrontend();
      if(!grant?.id||grant.revogada)throw new Error('Nenhuma concessão própria ativa para reconciliar.');
      await open('/permissoes-excepcionais');
      const list=await context.request.get(base+'/api/permissoes-excepcionais');
      const before=unwrap(await list.json()),actual=before.find(item=>item.id===grant.id);
      check('Reconciliação identifica concessão própria do alvo/beneficiário, sem ID presumido',list.status()===200
        && actual?.ativa===true && actual.ocorrenciaId===fixture.ocorrencia_restrita.id
        && actual.usuarioBeneficiarioId===fixture.accounts[beneficiary].id);
      const csrf=unwrap(await(await context.request.get(base+'/api/autenticacao/csrf')).json());
      const response=await context.request.post(base+`/api/permissoes-excepcionais/${grant.id}/revogar`,{
        headers:{[csrf.headerName]:csrf.token,'X-User-Password':fixture.accounts.fixture_admin.password,Origin:base},
        data:{motivo:'Reconciliação explícita da concessão sintética criada antes de falha de seletor do roteiro.'}});
      check('Administrador revoga somente a concessão do teste interrompido',response.status()===200);
      const after=unwrap(await(await context.request.get(base+'/api/permissoes-excepcionais')).json());
      check('Histórico do alvo preservado e outras concessões intactas',after.some(item=>item.id===grant.id&&!item.ativa)
        && JSON.stringify(before.filter(item=>item.id!==grant.id))===JSON.stringify(after.filter(item=>item.id!==grant.id)));
      recordBusiness({[key]:{...grant,revogada:true,reconciliacao:'browser-hmr-presidente01 seletor ambíguo'}});
      report.conditions={grantId:grant.id,beneficiary,only_revoke_existing:true,no_new_grant:true};
      report.limits=['Reconciliação por APIRequestContext real após login UI; não é aceite integral da ocorrência.'];
    },{accountName:'fixture_admin',reuse:false});
    return;
  }
  await execute('ocorrencia-especifica-antes', async ({ page, fixture, check, screenshot, open }) => {
    await open('/ocorrencias');
    const { form, response } = await consultar(page, fixture, 403);
    await form.getByRole('alert').waitFor();
    check('Sem concessão: recusa403 apresentada pela própria tela', response.status() === 403);
    check('Recusa conserva ID e finalidade', await form.getByLabel('ID da ocorrência').inputValue() === String(fixture.ocorrencia_restrita.id)
      && await form.getByLabel('Finalidade da consulta').inputValue() === fixture.ocorrencia_restrita.grantRequest.finalidade);
    check('403 não encerra sessão nem apresenta narrativa restrita', await page.getByRole('button', { name: 'Sair', exact: true }).isVisible()
      && !(await page.locator('body').innerText()).includes(narrativa));
    await screenshot('recusa-sem-concessao');
  }, { accountName: beneficiary, reuse: false });
  if (process.exitCode) return;
  await execute('ocorrencia-especifica-concessao', async ({ page, fixture, check, screenshot, open, confirmPassword, recordBusiness, report }) => {
    if (business()[key] && !business()[key].revogada) throw new Error('Concessão anterior ativa: reconciliar antes de repetir a mutação.');
    const alvo = fixture.ocorrencia_restrita, req = alvo.grantRequest;
    await open('/permissoes-excepcionais');
    await page.getByRole('button', { name: 'Nova permissão', exact: true }).click();
    const form = page.getByRole('dialog', { name: 'Conceder permissão excepcional', exact: true });
    report.selectorDiagnosis = { scopeExactLabel: await form.getByLabel('Escopo da concessão', { exact: true }).count(),
      scopeExactRole: await form.getByRole('combobox', { name: 'Escopo da concessão', exact: true }).count() };
    check('Seletor de escopo possui nome acessível exato', report.selectorDiagnosis.scopeExactRole === 1);
    await form.getByRole('combobox', { name: 'Escopo da concessão', exact: true }).selectOption('ocorrencia');
    await form.getByRole('combobox', { name: 'Beneficiário', exact: true }).selectOption(String(fixture.accounts[beneficiary].id));
    await form.getByLabel('Ocorrência (ID)', { exact: true }).fill(String(alvo.id));
    await form.getByLabel('Finalidade', { exact: true }).fill(req.finalidade);
    await form.getByLabel('Justificativa', { exact: true }).fill(req.justificativa);
    await form.getByLabel('Autorização administrativa', { exact: true }).fill(req.autorizacaoAdministrativa);
    const validade = await page.evaluate(iso => { const d = new Date(iso), n = x => String(x).padStart(2, '0');
      return `${d.getFullYear()}-${n(d.getMonth()+1)}-${n(d.getDate())}T${n(d.getHours())}:${n(d.getMinutes())}`; }, req.validoAte);
    await form.getByLabel('Validade', { exact: true }).fill(validade);
    const pending = page.waitForResponse(r => new URL(r.url()).pathname === '/api/permissoes-excepcionais' && r.request().method() === 'POST');
    await form.getByRole('button', { name: 'Conceder', exact: true }).click(); await confirmPassword();
    const response = await pending, dto = unwrap(await response.json());
    check('Administrador atual concede por HTTP real201', response.status() === 201 && Number.isSafeInteger(dto.id) && dto.ocorrenciaId === alvo.id);
    const enviado = response.request().postDataJSON();
    check('Comando vincula somente leitura ao alvo e beneficiário ativos', enviado.tipo === 'ACESSO_DADOS_PESSOAIS_RESTRITOS'
      && enviado.ocorrenciaId === alvo.id && enviado.usuarioBeneficiarioId === fixture.accounts[beneficiary].id
      && enviado.contratoId === null && enviado.gestaoLocalId === null);
    recordBusiness({ [key]: { id: dto.id, ocorrenciaId: alvo.id, revogada: false } });
    report.conditions = { occurrence: alvo.id, grant: dto.id, beneficiary: fixture.accounts[beneficiary].id, daysWithHistoricalProof: alvo.dias };
    const card=page.getByRole('article').filter({has:page.locator('small',{hasText:new RegExp('^#'+dto.id+'$')})});
    await card.getByText('Ocorrência · leitura restrita', { exact: true }).waitFor();
    check('Conceder acesso não escreve ocorrência', report.http.every(r => r.method === 'GET' || !/^\/api\/ocorrencias(?:\/|$)/.test(r.path)));
    await screenshot('concessao-especifica');
  }, { accountName: 'fixture_admin', reuse: false });
  if (process.exitCode) return;
  await execute('ocorrencia-especifica-leitura-revogacao', async ({ page, context, fixture, check, screenshot, open, recordBusiness, report }) => {
    const alvo = fixture.ocorrencia_restrita, grant = business()[key];
    check('Concessão observada disponível, sem ID presumido', Number.isSafeInteger(grant?.id) && grant.ocorrenciaId === alvo.id && !grant.revogada);
    await open('/ocorrencias');
    const { response, dto } = await consultar(page, fixture, 200);
    const region = page.getByRole('region', { name: 'Ocorrência consultada', exact: true });
    await region.getByText(narrativa, { exact: true }).waitFor();
    check('Leitura real mantém ocorrência e três dias originais', response.status() === 200 && dto.ocorrencia.id === alvo.id
      && dto.ocorrencia.quantidadeDias === alvo.dias && dto.ocorrencia.observacao === narrativa && dto.cancelada === false);
    check('Leitor específico não oferece escrita', await region.getByRole('button', { name: /Excluir|Alterar/ }).count() === 0);
    if(beneficiary==='presidente') check('Presidente recebe somente leitor, sem busca geral ou cadastro', await page.getByLabel('Nome ou CPF do colaborador',{exact:true}).count()===0 && await page.getByRole('button',{name:/Novo registro|Nova ocorrência/i}).count()===0);
    await region.getByRole('button', { name: 'Acessar anexo: anexo-sintetico.txt', exact: true }).click();
    const modal = page.getByRole('dialog', { name: 'Acessar anexo', exact: true });
    await modal.getByLabel('Finalidade do acesso (obrigatória)', { exact: true }).fill(alvo.grantRequest.finalidade);
    const downloadPromise = page.waitForEvent('download'); await modal.getByRole('button', { name: 'Baixar anexo', exact: true }).click();
    const download = await downloadPromise; check('Anexo entregue sem falha no navegador', !(await download.failure()));
    const filename = path.join(process.env.SGT_E2E_OUTPUT, 'ocorrencia-anexo-real.txt'); await download.saveAs(filename);
    const bytes = fs.readFileSync(filename); const sha = crypto.createHash('sha256').update(bytes).digest('hex');
    check('Download preserva os bytes originais preparados', sha === alvo.anexoSha256 && bytes.length === alvo.anexoBytes);
    const base = baseFrontend();
    const csrf = unwrap(await (await context.request.get(base + '/api/autenticacao/csrf')).json());
    const deniedWrite = await context.request.delete(base + `/api/ocorrencias/${alvo.id}`, { headers: {
      [csrf.headerName]: csrf.token, 'X-User-Password': fixture.accounts[beneficiary].password, Origin: base } });
    check('Concessão de leitura não habilita DELETE', deniedWrite.status() === 403);
    report.http.push({ method: 'DELETE', path: `/api/ocorrencias/${alvo.id}`, status: deniedWrite.status(), driver: 'APIRequestContext real — negativa de escrita' });
    await screenshot('leitura-autorizada');
    const admin = await context.browser().newContext({ storageState: path.join(runtime, 'private/e2e-fixture_admin-browser-state.json') });
    try {
      const token = unwrap(await (await admin.request.get(base + '/api/autenticacao/csrf')).json());
      const revoked = await admin.request.post(base + `/api/permissoes-excepcionais/${grant.id}/revogar`, {
        headers: { [token.headerName]: token.token, 'X-User-Password': fixture.accounts.fixture_admin.password, Origin: base },
        data: { motivo: 'Encerramento da validação sintética de leitura específica' } });
      check('Administrador revoga pela API real, sem mudar a sessão leitora', revoked.status() === 200);
      report.http.push({ method: 'POST', path: `/api/permissoes-excepcionais/${grant.id}/revogar`, status: revoked.status(), driver: 'APIRequestContext real — revogação administrativa' });
    } finally { await admin.close(); }
    recordBusiness({ [key]: { ...grant, revogada: true } });
    const negada = await consultar(page, fixture, 403); await negada.form.getByRole('alert').waitFor();
    check('A mesma sessão recebe403 e limpa a narrativa já exibida', !(await page.locator('body').innerText()).includes(narrativa)
      && await page.getByRole('button', { name: 'Sair', exact: true }).isVisible());
    check('Recusa após revogação preserva a intenção de consulta', await negada.form.getByLabel('ID da ocorrência').inputValue() === String(alvo.id)
      && await negada.form.getByLabel('Finalidade da consulta').inputValue() === alvo.grantRequest.finalidade);
    const deniedFile = await context.request.get(base + `/api/ocorrencias/anexos/${alvo.anexoId}`, { params: { finalidade: alvo.grantRequest.finalidade } });
    check('Download posterior também é403 sem bytes protegidos', deniedFile.status() === 403 && !(await deniedFile.body()).includes(Buffer.from('ANEXO SINTETICO DE OCORRENCIA')));
    report.http.push({ method: 'GET', path: `/api/ocorrencias/anexos/${alvo.anexoId}`, status: deniedFile.status(), driver: 'APIRequestContext real — negativa após revogação' });
    report.limits = ['Concessão e leitura/download via UI; negativas de escrita e revogação pelo APIRequestContext real.',
      'Não prova duas abas, 2FA, LAN, virada física de data nem homologação global.'];
    await screenshot('mesma-sessao-apos-revogacao');
  }, { accountName: beneficiary, reuse: false });
})();
