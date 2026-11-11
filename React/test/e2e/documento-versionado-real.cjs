// E2E-22: fonte vinculada a um aditamento real já publicado; não promove rascunhos.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { baseFrontend, contratoDaFixture } = require('./contexto-fixture.cjs');
const { execute } = require('./ambiente-real.cjs');
const hash = value => crypto.createHash('sha256').update(value).digest('hex');

execute('documento-versionado-real', async ({ page, context, account, fixture, report, check, open, confirmPassword, recordBusiness, screenshot }) => {
  const business = JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json')));
  check('Aditamento causal existe e retificação desta intenção ainda não foi executada', business.prorrogacaoFinal02Id > 0 && !business.documentoRetificadoE2EId);
  const contract = contratoDaFixture(fixture);
  const base = baseFrontend();
  await open('/contratos');
  const read = async route => {
    const response = await context.request.get(base + route);
    check(`GET documental independente ${route}`, response.status() === 200);
    return (await response.json()).data;
  };
  const acts = await read(contract.api + '/aditamentos');
  check('Fato informado pertence ao contrato e está efetivado', acts.some(item => item.id === business.prorrogacaoFinal02Id && item.contratoId === contract.id && item.dataEfetiva === fixture.dates.today));
  const title = 'DOCUMENTO SINTETICO VERSIONADO FINAL02';
  const documents = await read(contract.api + '/documentos');
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/assets/logo-footer.png'));
  const sourceHash = hash(source);
  await page.getByRole('button', { name: contract.botaoAbrir, exact: true }).click();
  const detail = page.getByRole('dialog', { name: contract.titulo, exact: true });
  let initial;
  if (business.documentoPublicadoE2EId) {
    const previousPath = path.join(path.dirname(process.env.SGT_TEST_RUNTIME), 'browser-jar-doc-versionado01/browser/documento-versionado-real.json');
    const bytes = fs.readFileSync(previousPath);
    const previous = JSON.parse(bytes);
    check('Retomada usa upload201 previamente observado, sem promovê-lo a aceite integral', previous.passed === false && previous.http.some(item => item.method === 'POST' && item.path === contract.api + '/documentos' && item.status === 201) && previous.assertions.some(item => item.description === 'Fonte publicada pelo vínculo tipado real, v1 e arquivo íntegro' && item.passed));
    initial = await read(contract.api + '/documentos/' + business.documentoPublicadoE2EId);
    check('Mesmo documento publicado v1 já persistido é retomado, sem novo POST', initial.id === business.documentoPublicadoE2EId && initial.titulo === title && initial.fatoId === business.prorrogacaoFinal02Id && initial.estado === 'PUBLICADO' && initial.versaoDocumento === 1 && initial.sha256Arquivo === sourceHash && initial.tamanhoBytes === source.length);
    report.creationResumed = { source: 'browser-jar-doc-versionado01', reportSha256: hash(bytes), documentId: initial.id, noDuplicateUpload: true };
  } else {
  check('Não repetir criação após eventual interrupção', documents.every(item => item.titulo !== title));
  const form = detail.locator('form').filter({ has: page.getByRole('heading', { name: 'Registrar documento', exact: true }) });
  await form.getByLabel(/^Tipo de fato/).selectOption('ADITAMENTO');
  await form.getByLabel('ID do fato', { exact: true }).fill(String(business.prorrogacaoFinal02Id));
  await form.getByLabel(/^Título/).fill(title);
  await form.getByLabel(/^Data efetiva/).fill(fixture.dates.today);
  await form.getByLabel(/^Referência SEI/).fill('SINTETICO-E2E-DOCUMENTO-PUBLICADO-V1');
  await form.getByLabel(/^Arquivo opcional/).setInputFiles({ name: 'comprovante-versionado-sintetico.png', mimeType: 'image/png', buffer: source });
  const createPending = page.waitForResponse(r => new URL(r.url()).pathname === contract.api + '/documentos' && r.request().method() === 'POST');
  createPending.catch(() => {});
  await form.getByRole('button', { name: 'Registrar com confirmação', exact: true }).click();
  await confirmPassword();
  const createdResponse = await createPending;
  check('Upload real confirmado201', createdResponse.status() === 201);
  initial = (await createdResponse.json()).data;
  recordBusiness({ documentoPublicadoE2EId: initial.id, documentoPublicadoE2EHash: sourceHash, documentoPublicadoE2ETamanho: source.length });
  check('Fonte publicada pelo vínculo tipado real, v1 e arquivo íntegro', initial.tipoFato === 'ADITAMENTO' && initial.fatoId === business.prorrogacaoFinal02Id && initial.estado === 'PUBLICADO' && initial.versaoDocumento === 1 && initial.sha256Arquivo === sourceHash && initial.tamanhoBytes === source.length);
  }
  await page.getByLabel('Senha de Acesso', { exact: true }).waitFor({ state: 'hidden' });
  const expectedDocuments = documents.length + (business.documentoPublicadoE2EId ? 0 : 1);
  const findDocument = async (locator, expected) => {
    const navigation = detail.getByRole('navigation', { name: 'Páginas de documentos', exact: true });
    if (expected > 5) await navigation.getByText(new RegExp(`${expected} registros$`)).waitFor();
    for (let step = 0; step <= Math.ceil((documents.length + 2) / 5); step++) {
      if (await locator.count()) { await locator.waitFor(); return; }
      check('Documento continua acessível pela paginação real', await navigation.count() === 1 && await navigation.getByRole('button', { name: 'Próxima', exact: true }).isEnabled());
      const text = await navigation.innerText();
      const current = Number(text.match(/Página (\d+) de/)[1]);
      await navigation.getByRole('button', { name: 'Próxima', exact: true }).click();
      await navigation.getByText(new RegExp(`Página ${current + 1} de`)).waitFor();
    }
    throw new Error('Documento não encontrado na cadeia paginada retornada pelo Java.');
  };
  const card = detail.locator('article').filter({ hasText: title });
  await findDocument(card, expectedDocuments);
  const newReference = 'SINTETICO-E2E-DOCUMENTO-RETIFICADO-V2';
  const answers = ['SINTETICO E2E: retificação da referência documental, preservando o anexo e a versão anterior.', newReference];
  let promptCount = 0;
  const onPrompt = async dialog => {
    if (dialog.type() !== 'prompt' || promptCount >= answers.length) return dialog.dismiss();
    await dialog.accept(answers[promptCount++]);
  };
  const endpoint = contract.api + '/documentos/' + initial.id + '/retificar-sei';
  const pending = page.waitForResponse(r => new URL(r.url()).pathname === endpoint && r.request().method() === 'POST');
  pending.catch(() => {});
  page.on('dialog', onPrompt);
  let first, payload;
  try {
    await card.getByRole('button', { name: 'Retificar SEI', exact: true }).click();
    await confirmPassword();
    const response = await pending;
    const envelope = await response.json();
    check('Retificação real com dois prompts e senha retorna200', promptCount === 2 && response.status() === 200, { status: response.status(), code: envelope.code });
    first = envelope.data;
    payload = response.request().postDataJSON();
  } finally { page.off('dialog', onPrompt); }
  recordBusiness({ documentoRetificadoE2EId: first.id, documentoRetificadoE2EPrimeiraVersaoJPA: first.version, documentoRetificadoE2EPrimeiroRecibo: first, documentoRetificadoE2EPayload: payload, documentoRetificadoE2EEndpoint: endpoint });
  check('Primeira resposta já apresenta v2 publicada na mesma cadeia', first.id !== initial.id && first.grupoLogicoId === initial.grupoLogicoId && first.documentoAnteriorId === initial.id && first.versaoDocumento === 2 && first.estado === 'PUBLICADO' && first.referenciaSei === newReference && first.sha256Arquivo === sourceHash);
  const after = await read(contract.api + '/documentos/' + first.id);
  const previous = await read(contract.api + '/documentos/' + initial.id);
  const csrf = await read('/api/autenticacao/csrf');
  const replayResponse = await context.request.post(base + endpoint, { data: payload, headers: { [csrf.headerName]: csrf.token, 'X-User-Password': account.password, Origin: base } });
  const replay = (await replayResponse.json()).data;
  check('Replay exato retorna a mesma versão documental e JPA da primeira resposta', replayResponse.status() === 200 && replay.id === first.id && replay.versaoDocumento === first.versaoDocumento && replay.version === first.version);
  check('GET persistido concorda com a primeira resposta em versão JPA, cadeia e hash', after.id === first.id && after.version === first.version && after.versaoDocumento === 2 && after.documentoAnteriorId === initial.id && after.sha256Arquivo === sourceHash);
  check('Versão anterior preservada como SUBSTITUIDO sem perder anexo ou SEI original', previous.estado === 'SUBSTITUIDO' && previous.versaoDocumento === 1 && previous.referenciaSei === initial.referenciaSei && previous.sha256Arquivo === sourceHash);
  const events = await read(contract.api + '/documentos/' + first.id + '/eventos');
  check('Histórico da cadeia contém publicação e uma única retificação', events.filter(item => item.tipoEvento === 'RETIFICACAO_SEI').length === 1 && events.some(item => item.tipoEvento === 'PUBLICACAO'));
  await page.getByLabel('Senha de Acesso', { exact: true }).waitFor({ state: 'hidden' });
  const currentCard = detail.locator('article').filter({ hasText: title }).filter({ hasText: newReference });
  await findDocument(currentCard, expectedDocuments + 1);
  const downloadPending = page.waitForEvent('download', { timeout: 15000 });
  downloadPending.catch(() => {});
  await currentCard.getByRole('button', { name: 'Baixar', exact: true }).click();
  const download = await downloadPending;
  const destination = path.join(process.env.SGT_E2E_OUTPUT, 'documento-versionado-v2-download.png');
  await download.saveAs(destination);
  const bytes = fs.readFileSync(destination);
  check('Download real da v2 preserva todos os bytes do upload v1', !(await download.failure()) && bytes.length === source.length && hash(bytes) === sourceHash);
  report.receipt = { initialId: initial.id, currentId: first.id, firstJpaVersion: first.version, replayJpaVersion: replay.version, getJpaVersion: after.version, bytes: bytes.length, sha256: sourceHash };
  report.scope = 'E2E22_UI_HTTP_DOWNLOAD_PENDING_READONLY_JPA_RECONCILIATION_NOT_AN_ORIGINAL_DRIVER';
  recordBusiness({ documentoRetificadoE2EConcluido: true });
  await screenshot('v1-preservada-v2-publicada');
});
