'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawn } = require('node:child_process');
const readline = require('node:readline');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend } = require('./contexto-fixture.cjs');
const unwrap = value => value && Object.hasOwn(value, 'data') ? value.data : value;
execute('exportacao-cancelamento-real', async ({ page, context, fixture, check, screenshot, open, report }) => {
  const r = fixture.relatorio41;
  await open('/relatorio-mensal');
  await page.getByRole('heading', { name: 'Relatório Consolidado de Frequência', exact: true }).waitFor();
  await page.getByLabel('Início', { exact: true }).fill(r.data);
  await page.getByLabel('Fim', { exact: true }).fill(r.data);
  await page.getByRole('combobox', { name: 'Lotação', exact: true }).selectOption(String(r.lotacaoId));
  await page.getByRole('combobox', { name: 'Turno', exact: true }).selectOption(String(r.jornadaId));
  const queryPromise = page.waitForResponse(res => {
    const u = new URL(res.url());
    return u.pathname === '/api/relatorios/frequencia-consolidada/paginada' && u.searchParams.get('inicio') === r.data
      && u.searchParams.get('fim') === r.data && u.searchParams.get('lotacaoId') === String(r.lotacaoId)
      && u.searchParams.get('jornadaId') === String(r.jornadaId);
  });
  await page.getByRole('button', { name: 'Gerar Relatório', exact: true }).click();
  const query = await queryPromise, dto = unwrap(await query.json());
  check('API real entrega o bloco mínimo25 de41, sem consulta completa', query.status() === 200 && dto.registros.length === 25 && dto.tamanho === 25 && dto.totalColaboradores === 41);
  const table = page.getByRole('table', { name: 'Relatório de frequência por colaborador', exact: true });
  await table.locator(':scope > tbody > tr').nth(4).waitFor();
  check('Interface mantém exatamente cinco colaboradores por página', await table.locator(':scope > tbody > tr').count() === 5);
  const requests = [], downloads = [];
  page.on('request', req => { const u = new URL(req.url()); requests.push({ method: req.method(), path: u.pathname, at: Date.now() }); });
  page.on('download', d => downloads.push(d));
  const helperPath = process.env.SGT_REPORT_READ_BARRIER;
  if (!helperPath) throw new Error('Barreira física do laboratório isolado não configurada.');
  const barrier = spawn('python3', [helperPath, process.env.SGT_E2E_OUTPUT], { stdio: ['pipe','pipe','inherit'] });
  const messages = [], waiters = [];
  readline.createInterface({ input: barrier.stdout }).on('line', line => {
    const message = JSON.parse(line);
    if (waiters.length) waiters.shift().resolve(message);else messages.push(message);
  });
  barrier.on('exit', code => { if (code) while (waiters.length) waiters.shift().reject(new Error(`Barreira física terminou: ${code}`)); });
  const receive = () => messages.length ? Promise.resolve(messages.shift()) : new Promise((resolve,reject) => waiters.push({resolve,reject}));
  const send = async command => { barrier.stdin.write(JSON.stringify({ command })+'\n');return receive(); };
  let released = false;
  try {
  check('Barreira física adquirida na base isolada, sem DDL/DML', (await receive()).ready === true);
  const createPromise = page.waitForResponse(res => new URL(res.url()).pathname === '/api/relatorios/frequencia-consolidada/exportacao/solicitacao'
    && res.request().method() === 'POST' && new URL(res.url()).searchParams.get('formato') === 'XLSX');
  await page.getByRole('button', { name: 'Planilha XLSX', exact: true }).click();
  const created = await createPromise, task = unwrap(await created.json());
  check('Exportação foi criada202 com estado ainda cancelável', created.status() === 202 && task.id && ['NA_FILA','PROCESSANDO'].includes(task.status));
  const observed = await send('observe');
  check('SELECT real da aplicação está fisicamente retido no PostgreSQL', observed.observed === true && observed.waiters.length > 0);
  report.readBarrier = { type: 'PostgreSQL transaction lock, own isolated database; no DDL/DML', waiters: observed.waiters };
  const panel = page.getByRole('complementary', { name: 'Exportação de documento', exact: true });
  const cancelPromise = page.waitForResponse(res => new URL(res.url()).pathname === `/api/relatorios/exportacoes/${task.id}/cancelamento` && res.request().method() === 'POST');
  await panel.getByRole('button', { name: 'Cancelar exportação', exact: true }).click();
  const canceled = await cancelPromise, canceledDto = unwrap(await canceled.json());
  check('Cancelamento explícito confirmado200/CANCELADO', canceled.status() === 200 && canceledDto.id === task.id && canceledDto.status === 'CANCELADO' && !canceledDto.podeBaixar);
  check('Barreira física liberada por rollback', (await send('release')).released === true);released = true;barrier.stdin.end();
  await panel.getByText('Cancelada', { exact: true }).waitFor();
  const terminalAt = Date.now(); await page.waitForTimeout(1800);
  check('Tarefa cancelada não volta a ser consultada ou baixada', requests.filter(x => x.path === `/api/relatorios/exportacoes/${task.id}` && x.at > terminalAt).length === 0
    && downloads.length === 0 && await page.locator('iframe[title^="download-relatorio-"]').count() === 0);
  check('Mutação de cancelamento não foi repetida', requests.filter(x => x.method === 'POST' && x.path === `/api/relatorios/exportacoes/${task.id}/cancelamento`).length === 1);
  const realDenied = await context.request.get(baseFrontend() + `/api/relatorios/exportacoes/${task.id}/arquivo`);
  check('Arquivo cancelado recusado409, sem conteúdo ZIP/PDF', realDenied.status() === 409 && !(await realDenied.body()).subarray(0,2).equals(Buffer.from('PK')));
  report.http.push({ method: 'GET', path: `/api/relatorios/exportacoes/${task.id}/arquivo`, status: realDenied.status(), driver: 'APIRequestContext real — cancelamento' });
  report.conditions = { recordCount: 41, visiblePageCount: 5, taskId: task.id, canceledStatus: canceledDto.status,
    data: r.data, lotacao: r.lotacaoId, jornada: r.jornadaId };
  await screenshot('cancelada-sem-download');
  await panel.getByRole('button', { name: 'Fechar aviso de exportação', exact: true }).click();
  const pdfResponsePromise = page.waitForResponse(res => new URL(res.url()).pathname === '/api/relatorios/frequencia-consolidada/exportacao/solicitacao'
    && res.request().method() === 'POST' && new URL(res.url()).searchParams.get('formato') === 'PDF');
  const filePromise = page.waitForEvent('download', { timeout: 60000 });
  await page.getByRole('button', { name: 'Documento PDF', exact: true }).click();
  const pdfResponse = await pdfResponsePromise, pdfTask = unwrap(await pdfResponse.json());
  check('Controle normal cria outra exportação202', pdfResponse.status() === 202 && pdfTask.id !== task.id);
  const download = await filePromise;check('Controle normal entrega download real sem falha', !(await download.failure()));
  const filename = path.join(process.env.SGT_E2E_OUTPUT, 'relatorio41-controle.pdf');await download.saveAs(filename);
  const bytes = fs.readFileSync(filename);
  check('Controle normal contém PDF íntegro, não erro JSON', bytes.subarray(0,5).toString() === '%PDF-' && bytes.subarray(-1024).includes(Buffer.from('%%EOF')));
  report.downloads = [{file:'relatorio41-controle.pdf',bytes:bytes.length,sha256:crypto.createHash('sha256').update(bytes).digest('hex')}];
  report.limits = ['Cancelamento e controle normal via UI/API reais, corpus41; sem retenção fictícia ou mocks.',
    'Assinatura PDF não prova por si só contagem/legibilidade41em2páginas; validação de conteúdo separada. Não é LAN.'];
  await screenshot('controle-normal');
  } finally {
    if (!released && barrier.exitCode === null) { barrier.stdin.write(JSON.stringify({command:'release'})+'\n');barrier.stdin.end(); }
    if (barrier.exitCode === null) await new Promise(resolve => barrier.once('exit', resolve));
  }
}, { accountName: 'relatorios_gestor', reuse: false });
