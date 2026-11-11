const fs = require('node:fs');
const path = require('node:path');
const { contratoDaFixture, idObservado } = require('./contexto-fixture.cjs');
const { execute } = require('./ambiente-real.cjs');

const TIPO = 'TRANSFERENCIA_GESTAO_LOCAL';
const REFERENCIA_BASE = 'SINTETICO-E2E-TRANSFERENCIA-20261101';
const FINALIDADE = 'SINTETICO E2E: validar transferência do contrato sintético observado; sem validade administrativa real.';
const JUSTIFICATIVA = 'SINTETICO E2E autorizado: testar concessão temporária, prévia e posterior revogação em ambiente isolado.';
const dados = body => body && Object.hasOwn(body, 'data') ? body.data : body;
const api = (page, caminho) => new URL(`/api${caminho}`, page.url()).href;

async function principal() {
  await execute('admin-autorizacao', async ({ page, context, fixture, account, report, check, screenshot, open, confirmPassword, recordBusiness }) => {
    const contratoAlvo = contratoDaFixture(fixture);
    const REFERENCIA = `${REFERENCIA_BASE}-CONTRATO-${contratoAlvo.id}`;
    const beneficiarioId = idObservado(fixture.accounts.gestor_local?.id, 'gestor_local');
    const gestaoOrigemId = idObservado(fixture.ids.gestao_origem, 'gestao_origem');
    check('Conta administrativa sintética designada', account.id === idObservado(fixture.accounts.fixture_admin?.id, 'fixture_admin') && account.perfil === 'ADMINISTRADOR');
    await open('/permissoes-excepcionais');
    await page.getByRole('heading', { name: 'Permissões excepcionais', exact: true }).waitFor();

    const contratoResponse = await context.request.get(api(page, contratoAlvo.caminho));
    const contrato = dados(await contratoResponse.json());
    check('Pré-condição real: contrato observado permanece na Gestão de origem da fixture', contratoResponse.status() === 200 && contrato.id === contratoAlvo.id && contrato.gestaoLocalId === gestaoOrigemId, { status: contratoResponse.status(), contratoId: contrato.id, gestaoLocalId: contrato.gestaoLocalId, via: 'context.request GET de pré-condição' });

    const listaResponse = await context.request.get(api(page, '/permissoes-excepcionais'));
    check('Administrador consulta concessões reais', listaResponse.status() === 200, { status: listaResponse.status(), via: 'context.request GET de pré-condição' });
    const lista = dados(await listaResponse.json());
    let concessao = lista.find(item => item.tipo === TIPO && item.usuarioBeneficiarioId === beneficiarioId && item.contratoId === contratoAlvo.id && item.ativa && item.autorizacaoAdministrativa === REFERENCIA);

    if (!concessao) {
      await page.getByRole('button', { name: 'Nova permissão', exact: true }).click();
      const formulario = page.getByRole('dialog', { name: 'Conceder permissão excepcional', exact: true });
      await formulario.waitFor();
      // O nome acessível exclui as opções do select associado ao label implícito.
      // A associação nativa e os quatro nomes exatos também são verificados no teste do componente.
      await formulario.getByRole('combobox', { name: 'Tipo', exact: true }).selectOption(TIPO);
      await formulario.getByRole('combobox', { name: 'Beneficiário', exact: true }).selectOption(String(beneficiarioId));
      await formulario.getByRole('combobox', { name: 'Contrato (quando aplicável)', exact: true }).selectOption(String(contratoAlvo.id));
      await formulario.getByRole('combobox', { name: 'Gestão Local (quando aplicável)', exact: true }).selectOption('');
      const validadeLocal = await page.evaluate(() => {
        const instante = new Date(Date.now() + 24 * 60 * 60 * 1000);
        const numero = valor => String(valor).padStart(2, '0');
        return `${instante.getFullYear()}-${numero(instante.getMonth() + 1)}-${numero(instante.getDate())}T${numero(instante.getHours())}:${numero(instante.getMinutes())}`;
      });
      await page.getByLabel('Validade', { exact: true }).fill(validadeLocal);
      await page.getByLabel('Finalidade', { exact: true }).fill(FINALIDADE);
      await page.getByLabel('Justificativa', { exact: true }).fill(JUSTIFICATIVA);
      await page.getByLabel('Autorização administrativa', { exact: true }).fill(REFERENCIA);
      const respostaPromise = page.waitForResponse(response => response.request().method() === 'POST' && new URL(response.url()).pathname === '/api/permissoes-excepcionais', { timeout: 20000 });
      await page.getByRole('button', { name: 'Conceder', exact: true }).click();
      await confirmPassword();
      const resposta = await respostaPromise;
      const corpo = await resposta.json();
      check('Concessão criada pela interface e confirmação de senha: POST real 201', resposta.status() === 201, { status: resposta.status(), code: corpo.code, via: 'UI → HTTP → Java' });
      concessao = dados(corpo);
      report.createdByUi = true;
    } else {
      report.createdByUi = false;
      report.reusedExistingSyntheticGrant = true;
    }

    check('Concessão persistida para usuário e contrato observados com o tipo Java exato', Number.isSafeInteger(concessao.id) && concessao.tipo === TIPO && concessao.usuarioBeneficiarioId === beneficiarioId && concessao.contratoId === contratoAlvo.id && concessao.ativa === true, { permissaoId: concessao.id, tipo: concessao.tipo, usuarioBeneficiarioId: concessao.usuarioBeneficiarioId, contratoId: concessao.contratoId });
    recordBusiness({ permissaoTransferenciaId: concessao.id, permissaoTransferenciaUsuarioId: beneficiarioId, permissaoTransferenciaContratoId: contratoAlvo.id, referenciaPermissaoTransferencia: REFERENCIA, permissaoTransferenciaValidoAte: concessao.validoAte });
    const cartao = page.locator('article').filter({ hasText: REFERENCIA });
    await cartao.waitFor();
    check('Interface apresenta a autorização concedida como ativa', await cartao.getByText('Ativa', { exact: true }).isVisible());
    await page.getByLabel('Senha de Acesso', { exact: true }).waitFor({ state: 'hidden', timeout: 5000 });
    report.screenshots = [await screenshot('concessao-ativa')];

    await open('/contratos');
    await page.getByRole('heading', { name: 'Contratos de terceirização', exact: true }).waitFor();
    check('Administrador navega em contratos em modo de consulta', await page.getByText('Perfil de consulta', { exact: true }).isVisible());
    check('Interface administrativa não oferece criação negocial de contrato', await page.getByRole('button', { name: 'Novo contrato', exact: true }).count() === 0);
    report.screenshots.push(await screenshot('contratos-consulta-admin'));

    const auditoriaPromise = page.waitForResponse(response => response.request().method() === 'GET' && new URL(response.url()).pathname === '/api/registros-auditoria', { timeout: 20000 });
    await open('/logs');
    await page.getByRole('heading', { name: 'Logs de auditoria', exact: true }).waitFor();
    const auditoria = await auditoriaPromise;
    check('Navegação administrativa de auditoria consulta o Java real', auditoria.status() === 200, { status: auditoria.status(), via: 'UI → HTTP → Java' });
    report.screenshots.push(await screenshot('auditoria-admin'));
    report.concessaoMantidaAtiva = true;
  }, { accountName: 'fixture_admin' });

  if (process.exitCode) return;

  await execute('gestor-destino-acesso', async ({ page, context, fixture, account, report, check, screenshot, open }) => {
    const contratoAlvo = contratoDaFixture(fixture);
    const business = JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json')));
    const documentoId = idObservado(business.documentoId, 'documentoId recebido no upload');
    check('Sessão separada do gestor de destino sintético designado', account.id === idObservado(fixture.accounts.gestor_destino?.id, 'gestor_destino') && account.gestaoLocalId === idObservado(fixture.ids.gestao_destino, 'gestao_destino'));
    await open('/contratos');
    await page.getByRole('heading', { name: 'Contratos de terceirização', exact: true }).waitFor();
    const listagem = await context.request.get(api(page, '/contratos'));
    const contratos = dados(await listagem.json());
    const registros = Array.isArray(contratos) ? contratos : contratos?.content || [];
    check('Listagem real no destino não inclui contrato observado da origem', listagem.status() === 200 && registros.every(item => item.id !== contratoAlvo.id), { status: listagem.status(), via: 'context.request GET de controle' });
    check('Interface do destino não expõe o contrato observado da origem', await page.getByRole('button', { name: contratoAlvo.botaoAbrir, exact: true }).count() === 0 && !(await page.locator('body').innerText()).includes(contratoAlvo.numero));
    check('Interface do destino não expõe o título do documento de origem', !(await page.locator('body').innerText()).includes('DOCUMENTO SINTETICO E2E — sem valor administrativo'));
    report.screenshots = [await screenshot('contratos-sem-origem')];

    for (const caminho of [contratoAlvo.caminho, `${contratoAlvo.caminho}/documentos/${documentoId}`]) {
      const resposta = await context.request.get(api(page, caminho));
      const corpo = await resposta.json();
      const status = resposta.status();
      const esperadoPeloCodigo = 404;
      check(`Leitura direta bloqueada no escopo de destino: ${caminho}`, status === 403 || (status === esperadoPeloCodigo && /escopo|não encontrad/i.test(corpo.message || '')), { status, code: corpo.code, via: 'context.request; controle de servidor, não ação da UI', contratoId: contratoAlvo.id, ...(caminho.includes('/documentos/') ? { documentoId } : {}) });
      check(`Resposta de bloqueio não contém dados negociais/documentais: ${caminho}`, !corpo.data && !('cnpjEmpresa' in corpo) && !('nomeOriginalArquivo' in corpo) && !('sha256Arquivo' in corpo));
      report.http.push({ method: 'GET', path: `/api${caminho}`, status, code: corpo.code, via: 'context.request' });
      if (caminho.includes('/documentos/') && status !== 403) report.documentStatusDifference = { requested: 403, observed: status, reason: 'AcessoOrganizacionalServico.exigirAcessoContrato responde 404 antes da validação de custódia histórica quando o contrato atual está fora do escopo.' };
    }
    check('Nenhuma informação de origem apareceu na interface após as verificações de servidor', !(await page.locator('body').innerText()).includes(contratoAlvo.numero));
  }, { accountName: 'gestor_destino' });
}

principal().catch(() => { console.error('Falha ao executar o driver administrativo sintético; consulte o relatório sanitizado.'); process.exitCode = 1; });
