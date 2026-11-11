'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');
const unwrap = value => value && Object.hasOwn(value, 'data') ? value.data : value;
execute('painel-bissexto-real', async ({ page, context, fixture, check, screenshot, open, report }) => {
  const p = fixture.painel_bissexto;
  const settings = await context.newPage();
  try {
    await settings.goto('chrome://settings/appearance');
    const zoom = settings.getByRole('combobox').filter({ has: settings.getByRole('option', { name: '150%', exact: true }) });
    await zoom.selectOption({ label: '150%' });
    check('Zoom150 configurado pela interface nativa do Chrome', await zoom.inputValue() === '1.5');
  } finally { await settings.close(); }
  await open('/painel');
  const cdp = await context.newCDPSession(page);
  const metrics = await cdp.send('Page.getLayoutMetrics');
  const nativeZoom = metrics.cssVisualViewport.zoom;
  const domMetrics = await page.evaluate(() => ({ innerWidth, innerHeight, devicePixelRatio,
    cssZoom: getComputedStyle(document.documentElement).zoom, bodyZoom: getComputedStyle(document.body).zoom }));
  check('Chrome comprova zoom nativo1.5 sem CSS zoom ou pinch', Math.abs(nativeZoom - 1.5) < .001
    && Math.abs(metrics.cssVisualViewport.scale - 1) < .001 && domMetrics.cssZoom === '1' && domMetrics.bodyZoom === '1');
  report.nativeZoom = { zoom: nativeZoom, cssVisualViewport: metrics.cssVisualViewport, dom: domMetrics,
    source: 'chrome://settings/appearance UI + Page.getLayoutMetrics, isolated temporary Chrome profile' };
  const indicator = page.getByRole('region', { name: 'Indicador contratual de efetivo', exact: true });
  await indicator.getByLabel('Data de referência do indicador', { exact: true }).fill(p.referencia);
  await indicator.getByRole('button', { name: 'Quinzenal', exact: true }).click();
  await indicator.getByRole('combobox', { name: 'Lotação', exact: true }).selectOption(String(p.lotacaoId));
  await indicator.getByRole('combobox', { name: 'Turno', exact: true }).selectOption(String(p.jornadaId));
  await indicator.getByRole('combobox', { name: 'Cargo', exact: true }).selectOption(String(p.cargoId));
  const currentResponse = page.waitForResponse(res => {
    const u = new URL(res.url());return u.pathname === '/api/painel/efetivo' && u.searchParams.get('referencia') === p.referencia
      && u.searchParams.get('periodo') === 'QUINZENAL' && u.searchParams.get('compararAnoAnterior') === 'true'
      && ['lotacaoId','jornadaId','cargoId'].every(key => u.searchParams.get(key) === String(p[key]));
  });
  await indicator.getByRole('button', { name: 'Comparar ano anterior', exact: true }).click();
  const response = await currentResponse, dto = unwrap(await response.json());
  check('Mesmo produtor nativo retorna15pontos2024 e14pontos2023', response.status() === 200
    && dto.serieAtual.length === 15 && dto.serieAnoAnterior.length === 14
    && dto.inicio === '2024-02-15' && dto.fim === p.referencia && dto.periodo === 'QUINZENAL');
  check('Todos29valores discriminantes conferem com a massa independente', [...dto.serieAtual,...dto.serieAnoAnterior]
    .every(row => row.efetivoPrevisto === p.esperado[row.data] && row.efetivoAlocado === 0 && row.registrosLancados === 0)
    && dto.resumoAtual.efetivoPrevisto === 229 && dto.resumoAnoAnterior.efetivoPrevisto === 128);
  check('Não há29fevereiro artificial na comparação2023', !dto.serieAnoAnterior.some(row => row.data === '2023-02-29'));
  const comparisonRow = indicator.getByRole('row').filter({ has: page.getByRole('cell', { name: 'Previsto em contrato', exact: true }) });
  await comparisonRow.getByRole('cell', { name: '229', exact: true }).waitFor();
  check('Tabela apresenta229/128 na mesma referência civil', await comparisonRow.getByRole('cell').allTextContents().then(cells => cells.join('|') === 'Previsto em contrato|229|128')
    && await indicator.getByLabel('Data de referência do indicador', { exact: true }).inputValue() === '2024-02-29');
  const output = process.env.SGT_E2E_OUTPUT;
  fs.writeFileSync(path.join(output,'dto-bissexto-real.json'), JSON.stringify(dto,null,2), {mode:0o600});
  fs.writeFileSync(path.join(output,'expected-bissexto.json'), JSON.stringify(p.esperado,null,2), {mode:0o600});
  await screenshot('comparacao-150');
  await indicator.getByRole('heading', {name:'Efetivo operacional e execução contratual',exact:true}).scrollIntoViewIfNeeded();
  await page.screenshot({path:path.join(output,'painel-filtros-150.png')});
  await comparisonRow.scrollIntoViewIfNeeded();
  await page.screenshot({path:path.join(output,'painel-comparacao-150.png')});
  const file = page.waitForEvent('download', {timeout:15000});
  await indicator.getByRole('button', { name: 'XLSX', exact: true }).click();
  const download = await file;check('Writer frontend entrega XLSX real no navegador', !(await download.failure()));
  const filename = path.join(output,'painel-bissexto-real.xlsx');await download.saveAs(filename);
  const bytes = fs.readFileSync(filename);
  check('Planilha baixada contém ZIP real', bytes.subarray(0,2).toString() === 'PK');
  report.downloads = [{file:path.basename(filename),bytes:bytes.length,sha256:crypto.createHash('sha256').update(bytes).digest('hex')}];
  await indicator.getByRole('button', { name: 'Ver leitura operacional', exact: false }).count().then(count => { report.operationalButtonMatches = count; });
  report.limits = ['UI, HTTP e writer reais em Chrome com zoom nativo150; conteúdo XLSX validado separadamente.',
    'Não prova respostas A→B fora de ordem, LAN, todas as jornadas ou homologação global.'];
}, { accountName: 'painel_gestor', reuse: false, persistent: true });
