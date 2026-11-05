/**
 * Ambiente real sintético, execução SEQUENCIAL após pessoa-vinculo.cjs.
 * Requer SGT_TEST_RUNTIME, frontend/Java/MySQL/Redis preparados e Playwright
 * do harness ambiente-real.cjs. Não cria pessoas, ocorrências ou frequências.
 * Cria dois snapshots administrativos e jobs de exportação pelas telas reais.
 * A referência vem da fixture, sem deslocar a jornada para forçar um lançamento.
 *
 * Fontes: Aplicacao.jsx; RelatorioControlador.java;
 * RelatorioAdministrativoControlador.java; OcorrenciaControlador.java;
 * RelatorioFrequenciaPaginaDTO.java (paginação por colaborador).
 */
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');

const unwrap = body => body && Object.hasOwn(body, 'data') ? body.data : body;
const list = body => Array.isArray(body) ? body : Array.isArray(body?.content) ? body.content : [];
const onlyDigits = value => String(value || '').replace(/\D/g, '');

execute('frequencias-relatorios', async ({ page, fixture, report, check, screenshot, open }) => {
  const runtime = process.env.SGT_TEST_RUNTIME;
  const businessPath = path.join(runtime, 'private/e2e-business.json');
  const business = JSON.parse(fs.readFileSync(businessPath, 'utf8'));
  const request = fixture.pending_vinculo_request;
  const reference = process.env.SGT_E2E_REFERENCE_DATE || request?.dataInicio;
  const output = process.env.SGT_E2E_OUTPUT || path.join(path.dirname(runtime), 'evidence/browser');
  check('Fixture sintética com pessoa e vínculo previamente criados', fixture.synthetic === true && !!business.pessoaId && !!business.vinculoId);
  check('Data de referência explícita proveniente da fixture', /^\d{4}-\d{2}-\d{2}$/.test(reference || ''));
  check('Identidade sintética disponível para localizar ocorrências', !!onlyDigits(request?.pessoa?.cpf));
  report.conditions = {
    dataReferencia: reference,
    pessoaId: business.pessoaId,
    vinculoId: business.vinculoId,
    lotacaoId: fixture.ids.lotacao,
    jornadaId: fixture.ids.jornada,
    conta: 'gestor_local',
  };
  report.downloads = [];
  report.limits = [];
  report.screenshots = [];
  const forbiddenWrites = [];
  page.on('request', req => {
    const pathname = new URL(req.url()).pathname;
    if (!['GET', 'HEAD', 'OPTIONS'].includes(req.method()) && /^\/api\/(frequencias(?:[-/]|$)|ocorrencias(?:\/|$))/.test(pathname)) {
      forbiddenWrites.push({ method: req.method(), path: pathname });
    }
  });

  const api = async (pathname, method, action, matches = () => true, expected = 200) => {
    const [response] = await Promise.all([
      page.waitForResponse(res => {
        const url = new URL(res.url());
        return url.pathname === pathname && res.request().method() === method && matches(url, res);
      }, { timeout: 30000 }),
      action(),
    ]);
    check(`${method} ${pathname} respondeu ${expected}`, response.status() === expected, { status: response.status() });
    return { response, data: unwrap(await response.json()) };
  };
  const shot = async label => report.screenshots.push(await screenshot(label));
  const choose = async (locator, value) => {
    await locator.locator(`option[value="${String(value)}"]`).waitFor({ state: 'attached', timeout: 15000 });
    if (await locator.inputValue() !== String(value)) await locator.selectOption(String(value));
  };
  const saveDownload = async (download, label, format) => {
    check(`Download ${label} sem falha do navegador`, !(await download.failure()));
    const file = `frequencias-relatorios-${label}.${format.toLowerCase()}`;
    const destination = path.join(output, file);
    await download.saveAs(destination);
    const bytes = fs.readFileSync(destination);
    const valid = format === 'PDF'
      ? bytes.subarray(0, 5).toString() === '%PDF-' && bytes.subarray(-1024).includes(Buffer.from('%%EOF'))
      : format === 'XLSX'
        ? bytes[0] === 0x50 && bytes[1] === 0x4b && bytes.includes(Buffer.from('[Content_Types].xml')) && bytes.includes(Buffer.from('xl/workbook.xml'))
        : bytes.length > 0;
    check(`Arquivo ${label} contém ${format} real`, bytes.length > (format === 'BIN' ? 0 : 100) && valid, { bytes: bytes.length });
    report.downloads.push({ file, format, bytes: bytes.length, sha256: crypto.createHash('sha256').update(bytes).digest('hex') });
  };
  const exportReport = async (kind, button, format, label) => {
    const download = page.waitForEvent('download', { timeout: 90000 }).then(value => ({ value }), error => ({ error }));
    const job = await api(`/api/relatorios/${kind}/exportacao/solicitacao`, 'POST', () => button.click(), url => url.searchParams.get('formato') === format, 202);
    check(`Job ${label} identificado pelo Java`, !!job.data?.id);
    const result = await download;
    check(`Job ${label} entregou arquivo ao navegador`, !!result.value, result.error ? { reason: result.error.message } : undefined);
    await saveDownload(result.value, label, format);
  };

  // Leitura da escala: a data de domingo é mantida; nenhum status é selecionado.
  const frequency = await api('/api/empregados/filtrados-frequencia', 'GET', async () => {
    await open('/frequencia');
    const date = page.getByLabel('Data de referência da frequência', { exact: true });
    if (await date.inputValue() !== reference) await date.fill(reference);
    await choose(page.getByRole('combobox', { name: 'Lotação', exact: true }), fixture.ids.lotacao);
    await choose(page.getByRole('combobox', { name: 'Turno', exact: true }), fixture.ids.jornada);
  }, url => url.searchParams.get('dataRef') === reference
    && url.searchParams.get('lotacaoId') === String(fixture.ids.lotacao)
    && url.searchParams.get('turnoId') === String(fixture.ids.jornada)
    && url.searchParams.get('page') === '0');
  const frequencyRows = list(frequency.data);
  check('Grade consulta cinco colaboradores por página', new URL(frequency.response.url()).searchParams.get('size') === '5' && frequencyRows.length <= 5, { recebidos: frequencyRows.length, total: frequency.data?.totalElements });
  await page.locator('[data-tour="freq-table"]').waitFor();
  if (Number(frequency.data?.totalPages) > 1) {
    const pagination = page.getByLabel('Paginação da grade de frequência');
    await api('/api/empregados/filtrados-frequencia', 'GET', () => pagination.getByRole('button', { name: 'Próxima', exact: true }).click(), url => url.searchParams.get('page') === '1');
    await api('/api/empregados/filtrados-frequencia', 'GET', () => pagination.getByRole('button', { name: 'Anterior', exact: true }).click(), url => url.searchParams.get('page') === '0');
  } else {
    report.limits.push('Grade de frequência: a fixture não contém uma segunda página; comprovados pedido size=5 e leitura da primeira página.');
  }
  await shot('frequencia-consulta');

  // Visão consolidada: o bloco Java mínimo é 10; o tamanho visual permanece 5.
  const consolidated = await api('/api/relatorios/terceirizados/consolidado', 'GET', () => open('/relatorios'), url => url.searchParams.get('page') === '0');
  await page.getByRole('heading', { name: 'Visão consolidada de empregados terceirizados' }).waitFor();
  check('Fixture presente no relatório consolidado', Number(consolidated.data?.empregados?.totalElements) > 0);
  const linesPerPage = page.getByRole('combobox', { name: 'Linhas por página', exact: true });
  await linesPerPage.waitFor();
  check('Visão consolidada inicia com cinco linhas visíveis', await linesPerPage.inputValue() === '5' && await page.locator('table tbody tr').count() <= 5, {
    tamanhoBlocoJava: consolidated.data?.empregados?.size,
    recebidosNoBloco: consolidated.data?.empregados?.content?.length,
    total: consolidated.data?.empregados?.totalElements,
  });
  if (Number(consolidated.data.empregados.totalElements) > 5) {
    await page.getByRole('button', { name: 'Próxima página', exact: true }).click();
    await page.getByText(/Página 2 de/).waitFor();
    check('Segunda página visual mantém até cinco linhas', await page.locator('table tbody tr').count() <= 5);
    await page.getByRole('button', { name: 'Página anterior', exact: true }).click();
    await page.getByText(/Página 1 de/).waitFor();
  } else {
    report.limits.push('Visão consolidada: fixture de até cinco registros; navegação de páginas maiores permanece coberta pelos testes comportamentais com limites Java reais.');
  }
  await shot('terceirizados-consulta');
  await exportReport('terceirizados', page.getByRole('button', { name: /^Excel\s*oficial$/i }), 'XLSX', 'terceirizados');
  await exportReport('terceirizados', page.getByRole('button', { name: /^PDF\s*oficial$/i }), 'PDF', 'terceirizados');

  // Relatórios de frequência e absenteísmo consultam a mesma referência sintética.
  await open('/relatorio-mensal');
  await page.getByLabel('Início', { exact: true }).fill(reference);
  await page.getByLabel('Fim', { exact: true }).fill(reference);
  const monthly = await api('/api/relatorios/frequencia-consolidada/paginada', 'GET', () => page.getByRole('button', { name: 'Gerar Relatório', exact: true }).click());
  check('Página de frequência formalmente concluída pelo Java', monthly.data?.completo === true && Array.isArray(monthly.data?.registros));
  if (Number(monthly.data?.totalColaboradores) > 0) {
    const pageSize = page.getByRole('combobox', { name: 'Colaboradores por página', exact: true });
    await pageSize.waitFor();
    check('Frequência mensal usa tamanho visual cinco', await pageSize.inputValue() === '5', { tamanhoBlocoJava: monthly.data.tamanho, colaboradores: monthly.data.totalColaboradores, contextosRecebidos: monthly.data.registros.length });
  } else {
    report.limits.push('Frequência mensal: resposta real sem colaboradores no recorte; não se afirma cobertura de linhas nem exportação desse relatório.');
  }
  await shot('relatorio-frequencia');

  await open('/relatorio-absenteismo');
  await page.getByRole('combobox', { name: 'Mês da competência', exact: true }).selectOption(String(Number(reference.slice(5, 7))));
  await page.getByLabel('Ano da competência', { exact: true }).fill(reference.slice(0, 4));
  const absences = await api('/api/relatorios/absenteismo/paginado', 'GET', () => page.getByRole('button', { name: 'CALCULAR', exact: true }).click());
  check('Absenteísmo retorna o DTO paginado real', Array.isArray(absences.data?.registros));
  if (Number(absences.data?.totalColaboradores) > 0) {
    const pageSize = page.getByRole('combobox', { name: 'Itens por página', exact: true });
    await pageSize.waitFor();
    check('Absenteísmo inicia com cinco registros visíveis', await pageSize.inputValue() === '5' && await page.locator('table tbody tr').count() <= 5, { tamanhoBlocoJava: absences.data.tamanho, total: absences.data.totalColaboradores });
  } else {
    report.limits.push('Absenteísmo: recorte real vazio; nenhuma ausência artificial foi criada.');
  }
  await shot('relatorio-absenteismo');

  // Duas emissões concluídas com filtros iguais precisam de intenções distintas.
  const history = await api('/api/relatorios/administrativos/emissoes', 'GET', () => open('/relatorios-administrativos'));
  const historyRegion = page.getByRole('region', { name: 'Histórico de emissões', exact: true });
  await historyRegion.waitFor();
  check('Histórico administrativo usa cinco itens por página', new URL(history.response.url()).searchParams.get('tamanho') === '5' && await historyRegion.getByRole('combobox', { name: 'Itens por página', exact: true }).inputValue() === '5');
  const filters = page.getByRole('region', { name: 'Filtros administrativos', exact: true });
  await filters.getByLabel('Início', { exact: true }).fill(reference);
  await filters.getByLabel('Fim', { exact: true }).fill(reference);
  await choose(filters.getByRole('listbox', { name: /^Contratos/ }), request.contratoId);
  const preview = await api('/api/relatorios/administrativos/previa', 'POST', () => page.getByRole('button', { name: 'Gerar prévia', exact: true }).click());
  check('Prévia administrativa traz conteúdo e metadados', !!preview.data?.metadados);
  await page.getByRole('heading', { name: 'Prévia atual', exact: true }).waitFor();
  const emissions = [];
  for (let index = 0; index < 2; index += 1) {
    const emission = await api('/api/relatorios/administrativos/emissoes', 'POST', () => page.getByRole('button', { name: 'Emitir versão formal', exact: true }).click(), () => true, 201);
    const payload = emission.response.request().postDataJSON();
    const metadata = emission.data?.metadados;
    check(`Emissão ${index + 1} contém identidade administrativa`, !!metadata?.id && !!payload.chaveIdempotencia);
    await page.getByRole('heading', { name: `Emissão #${metadata.id}`, exact: true }).waitFor();
    emissions.push({ id: metadata.id, versao: metadata.versao, payload });
  }
  check('Duas emissões concluídas usam chaves e IDs distintos', emissions[0].id !== emissions[1].id && emissions[0].payload.chaveIdempotencia !== emissions[1].payload.chaveIdempotencia);
  const withoutKey = ({ chaveIdempotencia, ...filtersPayload }) => filtersPayload;
  check('Emissões comparadas conservam exatamente os mesmos filtros', JSON.stringify(withoutKey(emissions[0].payload)) === JSON.stringify(withoutKey(emissions[1].payload)));
  report.emissions = emissions.map(({ id, versao }) => ({ id, versao }));
  await shot('administrativos-duas-emissoes');

  // Leitura funcional: localiza o empregado projetado pela API, sem presumir ID.
  await open('/ocorrencias');
  const employeeSearch = await api('/api/empregados', 'GET', () => page.getByPlaceholder('Nome ou CPF...', { exact: true }).fill(request.pessoa.cpf), url => url.searchParams.get('search') === request.pessoa.cpf);
  const employee = list(employeeSearch.data).find(item => onlyDigits(item.cpf) === onlyDigits(request.pessoa.cpf));
  check('Ocorrências localiza a identidade sintética pela API vigente', !!employee?.id);
  const employeeButton = page.getByRole('button').filter({ has: page.getByText(employee.nome, { exact: true }) });
  const occurrenceRead = await api('/api/ocorrencias', 'GET', () => employeeButton.click(), url => url.searchParams.get('empregadoId') === String(employee.id));
  await page.getByRole('heading', { name: 'Histórico funcional', exact: true }).waitFor();
  const occurrences = list(occurrenceRead.data);
  check('Histórico funcional consultado sem inventar ocorrências', Array.isArray(occurrenceRead.data) || Array.isArray(occurrenceRead.data?.content), { primeiraPagina: occurrences.length, total: occurrenceRead.data?.totalElements ?? occurrences.length });
  const attachmentButtons = page.getByRole('button', { name: /^Acessar anexo:/ });
  if (await attachmentButtons.count()) {
    const button = attachmentButtons.first();
    if (await button.isEnabled()) {
      await button.click();
      const modal = page.getByRole('dialog', { name: 'Acessar anexo', exact: true });
      await modal.getByLabel(/^Finalidade do acesso/).fill('Verificação funcional do anexo sintético no ambiente E2E autorizado.');
      const download = page.waitForEvent('download', { timeout: 30000 }).then(value => ({ value }), error => ({ error }));
      await modal.getByRole('button', { name: 'Baixar anexo', exact: true }).click();
      const result = await download;
      check('Anexo existente pode ser baixado pelo perfil local', !!result.value, result.error ? { reason: result.error.message } : undefined);
      await saveDownload(result.value, 'anexo-ocorrencia', 'BIN');
    } else {
      report.limits.push('Anexo de ocorrência existente bloqueado ao perfil; nenhum acesso alternativo foi tentado.');
    }
  } else {
    report.limits.push('Fixture sem anexo de ocorrência: comprovada consulta, sem criar registro ou documento apenas para preencher cobertura.');
  }
  await shot('ocorrencias-consulta');
  check('Nenhum lançamento, fechamento ou alteração de frequência/ocorrência foi enviado', forbiddenWrites.length === 0, forbiddenWrites);
}, { accountName: 'gestor_local', reuse: true });
