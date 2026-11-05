const { baseFrontend, contratoDaFixture } = require('./contexto-fixture.cjs');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');
const hash = value => crypto.createHash('sha256').update(value).digest('hex');
execute('persistencia-pos-reinicio', async ({ page, context, account, fixture, check, screenshot, open, report }) => {
  const contratoAlvo = contratoDaFixture(fixture);
  const business = JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json')));
  const base = baseFrontend();
  await open('/historico');
  const read = async (route, params) => {
    const response = await context.request.get(base + '/api' + route, { params });
    check(`Consulta após reinício: ${route}`, response.status() === 200);
    return (await response.json()).data;
  };
  const csrf = await read('/autenticacao/csrf');
  const headers = { [csrf.headerName]: csrf.token, 'X-User-Password': account.password, Origin: base };
  const person = await read(`/pessoas/${business.pessoaId}`);
  check('Nome atual persiste separado da prova histórica', person.nome === business.nomeAtualAnterior);
  const params = { vinculoId: business.vinculoId, data: fixture.dates.today };
  const currentName = await read(`/pessoas/${business.pessoaId}/nome-historico`, params);
  const oldName = await read(`/pessoas/${business.pessoaId}/nome-historico`, { ...params, conhecidoAte: business.nomeConhecidoAntes });
  check('Os dois cortes do nome histórico sobrevivem ao reinício', currentName.nome === business.nomeHistoricoComprovado && oldName.nome === business.nomeAtualAnterior && currentName.comprovado && oldName.comprovado);
  const versions = await read(`${contratoAlvo.caminho}/versoes`);
  const original = versions.find(version => version.id === business.contratoVersaoOriginalId);
  check('Fotografia da publicação mantém SHA-256 após reinício', hash(JSON.stringify(original)) === business.contratoVersaoOriginalSha256);
  for (const [label, endpoint, payload, expectedId, idField] of [
    ['nome', business.nomeEndpoint, business.nomeReplay, business.nomeProvaId, 'id'],
    ['contrato', business.contratoEndpoint, business.contratoReplay, business.contratoProvaId, 'id'],
    ['catálogo inicial', business.catalogoEndpoint, business.catalogoReplay, business.catalogoEventoId, 'eventoId'],
    ['catálogo concorrente', business.catalogoEndpoint, business.concorrenciaCatalogoReplay, business.concorrenciaCatalogoEventoId, 'eventoId'],
    ['correção material CNPJ', business.cnpjEndpoint, business.cnpjReplay, business.cnpjCorrecaoId, 'id'],
  ]) {
    check(`Intenção preservada disponível: ${label}`, !!payload && !!expectedId);
    const response = await context.request.post(base + endpoint, { data: payload, headers });
    const receipt = (await response.json()).data;
    check(`Reenvio idempotente após reinício: ${label}`, response.ok() && receipt[idField] === expectedId, { status: response.status(), id: receipt?.[idField] });
    report.http.push({ method: 'POST', path: endpoint, status: response.status(), driver: 'APIRequestContext — replay exato após reinício' });
  }
  const historicalCatalog = await read(`/catalogos-temporais/MOTIVO_DESLIGAMENTO/${fixture.ids.motivo_desligamento}/disponibilidade`, { data: fixture.dates.today, conhecidoAte: business.catalogoConhecidoAntesConcorrencia });
  const actualCatalog = await read(`/catalogos-temporais/MOTIVO_DESLIGAMENTO/${fixture.ids.motivo_desligamento}/disponibilidade`, { data: fixture.dates.today });
  check('Estado anterior e resultado concorrente permanecem distinguíveis', historicalCatalog.ativo === false && actualCatalog.ativo === business.concorrenciaCatalogoAtivo && historicalCatalog.disponibilidadeComprovada && actualCatalog.disponibilidadeComprovada);
  const future = await read(`${contratoAlvo.caminho}/posicao-temporal`, { data: fixture.dates.future });
  const transfers = await read(`${contratoAlvo.caminho}/transferencias-gestao`);
  check('Cancelamento futuro e cadeia de transferências persistem', business.transferenciaFuturaCancelada === true && future.versaoContratual.gestaoLocalId === fixture.ids.gestao_origem && transfers.some(item => item.id === business.transferenciaInicialId) && transfers.some(item => item.id === business.transferenciaRetificadaId));
  const corrected = await read(`${contratoAlvo.caminho}/posicao-temporal`, { data: business.cnpjDataEfetiva });
  check('Correção material mantém o alcance documentado após reinício', corrected.versaoContratual.cnpjEmpresa === business.cnpjCorrigido);
  const document = await context.request.get(base + `${contratoAlvo.api}/documentos/${business.documentoId}/arquivo`);
  if (document.status() === 404) throw new Error('O driver deve usar a rota binária exata do Java; consulte ServicoExcecoesContrato.');
  const bytes = await document.body();
  check('Arquivo publicado preserva bytes após reinício', document.status() === 200 && hash(bytes) === '8d0f050761d337273d63e3abeb522a8610260bab52604c2b45ef2204600aa5fb');
  const query = page.getByRole('form', { name: 'Consulta histórica' });
  await query.getByLabel('ID do contrato', { exact: true }).fill(String(contratoAlvo.id));
  await query.getByLabel('Data efetiva consultada', { exact: true }).fill(fixture.dates.future);
  await query.getByRole('button', { name: 'Consultar histórico', exact: true }).click();
  await page.getByRole('heading', { name: 'Posição contratual no recorte', exact: true }).waitFor();
  await screenshot('historico-reaberto');
});
