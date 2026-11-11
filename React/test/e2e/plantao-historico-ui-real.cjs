'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend } = require('./contexto-fixture.cjs');
const hash = value => crypto.createHash('sha256').update(JSON.stringify(value)).digest('hex');
execute('plantao-historico-ui-real', async ({ page, context, fixture, open, check, report, screenshot, recordBusiness }) => {
  const b = JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json'))), n = b.demo64Night;
  const base = baseFrontend();
  check('Corpus noturno próprio foi preparado sem mudar relógio ou vínculos antigos', fixture.synthetic && n?.ready && n.day === '2026-11-06' && n.movement === '2026-11-07');
  await open('/frequencia');
  const get = async (route, params) => {
    const response = await context.request.get(base + route, { params });
    check('GET real200 ' + route, response.status() === 200);
    return (await response.json()).data;
  };
  const readFrequency = () => get('/api/frequencias', { data: n.day, contratoId: n.contratoId, lotacaoId: n.lotacaoId, jornadaId: n.jornadaId });
  const position = '/api/vinculos-contratuais/' + n.vinculoId + '/alocacoes/posicao';
  const historical = await get(position, { data: n.day }), next = await get(position, { data: n.movement });
  const initial = await readFrequency(), frequency = initial.content.find(f => f.id === n.frequenciaId);
  check('DTO liga o plantão à alocação de início e D+1 à faixa nova', historical.alocacaoId === n.initialAlocacaoId
    && next.alocacaoId === n.dayAlocacaoId && frequency.alocacaoVinculoId === n.initialAlocacaoId
    && historical.jornadaId === n.jornadaId && next.jornadaId === fixture.ids.jornada);
  check('Prazo N30 vem do backend em São Paulo, independente da saída do plantão', frequency.prazoEdicao.startsWith('2026-12-06T00:00')
    && frequency.dataReferencia === n.day && frequency.entrada1 === '22:00' && frequency.saida1 === '06:00'
    && frequency.gestaoLocalId === fixture.ids.gestao_origem && frequency.bloqueadaEdicaoOperacional === true);
  const events = await get('/api/frequencias/' + n.frequenciaId + '/eventos');
  check('Fonte auditada conserva480min físicos e um único evento', events.length === 1
    && /(?:^|;)minutosFisicos=480(?:;|$)/.test(events[0].snapshotNovo));
  const row = () => page.locator('[class*="linhaVirtualBase"]').filter({ has: page.getByTitle('Nome Completo: ' + n.marker, { exact: true }) });
  const select = async (date, lotacao, jornada) => {
    const waiting = page.waitForResponse(r => {
      const url = new URL(r.url());
      return r.request().method() === 'GET' && url.pathname === '/api/frequencias'
        && url.searchParams.get('data') === date && url.searchParams.get('lotacaoId') === String(lotacao)
        && url.searchParams.get('jornadaId') === String(jornada);
    });
    waiting.catch(() => {});
    await page.getByLabel('Data de referência da frequência', { exact: true }).fill(date);
    const lot = page.getByRole('combobox', { name: 'Lotação', exact: true });
    await lot.locator(`option[value="${lotacao}"]`).waitFor({ state: 'attached' });
    await lot.selectOption(String(lotacao));
    const shift = page.getByRole('combobox', { name: 'Turno', exact: true });
    await shift.locator(`option[value="${jornada}"]`).waitFor({ state: 'attached' });
    await shift.selectOption(String(jornada));
    const response = await waiting;
    check('Grade lê a frequência no recorte exato selecionado ' + date, response.status() === 200);
    await page.getByLabel('Buscar colaborador por nome ou CPF', { exact: true }).fill(n.cpf);
    await row().waitFor();
  };
  await select(n.day, n.lotacaoId, n.jornadaId);
  await row().getByText('Salvo', { exact: true }).waitFor();
  const shift = row().getByTitle(/^Jornada de trabalho:/);
  const title = await shift.getAttribute('title');
  check('UI mostra o plantão histórico22–06, não a jornada diurna atual', title.includes(n.marker) && title.includes('22:00') && title.includes('06:00'));
  const counter = row().getByTitle('Prazo informado pelo backend; este contador é apenas informativo', { exact: true });
  await counter.waitFor();
  const counterText = await counter.innerText(), hours = counterText.match(/(\d+)h/i), minutes = counterText.match(/(\d+)m/i);
  const shownMinutes = hours && minutes ? Number(hours[1]) * 60 + Number(minutes[1]) : NaN;
  const expectedMinutes = Math.floor((Date.parse('2026-12-06T03:00:00Z') - Date.now()) / 60000);
  report.counterObservation = { serverDeadline: frequency.prazoEdicao, visibleCounter: counterText,
    shownMinutes, expectedMinutes, allowanceMinutesForRenderClock: 2 };
  check('Contador visual em horas/minutos corresponde ao prazo N30 e supera sete dias', shownMinutes > 7 * 24 * 60
    && shownMinutes <= 30 * 24 * 60 && Math.abs(shownMinutes - expectedMinutes) <= 2, report.counterObservation);
  await screenshot('plantao-na-data-de-inicio-e-prazo-n30');
  await select(n.movement, fixture.ids.lotacao, fixture.ids.jornada);
  await row().getByText('Pendente', { exact: true }).waitFor();
  check('No dia seguinte a UI usa a nova jornada e não copia o registro anterior', !(await row().getByTitle(/^Jornada de trabalho:/).getAttribute('title')).includes(n.marker)
    && await row().getByText('Salvo', { exact: true }).count() === 0);
  const followingFrequency = await get('/api/frequencias', { data: n.movement, contratoId: n.contratoId,
    lotacaoId: fixture.ids.lotacao, jornadaId: fixture.ids.jornada });
  check('Saída06h não duplica frequência nem parcela no dia da movimentação', followingFrequency.totalElements === 0);
  await screenshot('dia-seguinte-nova-alocacao-sem-duplicacao');

  const other = await context.browser().newContext({ locale: 'pt-BR', timezoneId: 'America/Sao_Paulo' });
  try {
    const actor = fixture.accounts.gestor_destino;
    const csrf1 = (await (await other.request.get(base + '/api/autenticacao/csrf')).json()).data;
    const login = await other.request.post(base + '/api/autenticacao/autenticar', {
      data: { matricula: actor.matricula, password: actor.password }, headers: { Origin: base, [csrf1.headerName]: csrf1.token } });
    check('Contraprova usa sessão real independente do Gestor da outra Gestão', login.status() === 200);
    const csrf2 = (await (await other.request.get(base + '/api/autenticacao/csrf')).json()).data;
    const denied = await other.request.post(base + '/api/frequencias', { data: { ...n.frequencyPayload, status: 'FALTA',
      justificativaRetificacao: 'Contraprova sintética da Gestão sem responsabilidade sobre o plantão.', chaveIdempotencia: 'DEMO64-GESTAO-B-SEM-PODER' },
    headers: { Origin: base, [csrf2.headerName]: csrf2.token, 'X-User-Password': actor.password } });
    const envelope = await denied.json();
    if (denied.status() === 200) recordBusiness({ demo64NightUnexpectedWrite: { id: envelope.data?.id, code: envelope.code || null } });
    check('Backend recusa403 a escrita por Gestão alheia ao contexto histórico', denied.status() === 403
      && /gest|escopo|permit|acesso|autoriz/i.test(envelope.message || ''), { status: denied.status(), code: envelope.code });
    report.http.push({ method: 'POST', path: '/api/frequencias', status: denied.status(), code: envelope.code, driver: 'Independent GestorB APIRequestContext real, no shared cookies' });
    const hidden = await other.request.get(base + '/api/frequencias', { params: { data: n.day, contratoId: n.contratoId, lotacaoId: n.lotacaoId, jornadaId: n.jornadaId } });
    const body = await hidden.json();
    check('Leitura da outra Gestão não revela o plantão', [403, 404].includes(hidden.status())
      || (hidden.status() === 200 && body.data.totalElements === 0 && body.data.content.length === 0));
    report.otherManagerRead = { status: hidden.status(), deniedOrEmpty: true };
  } finally { await other.close(); }
  const after = await readFrequency(), afterEvents = await get('/api/frequencias/' + n.frequenciaId + '/eventos');
  check('Tentativa alheia preserva integralmente frequência e eventos do plantão', hash(after.content.find(f => f.id === n.frequenciaId)) === hash(frequency)
    && hash(afterEvents) === hash(events));
  check('Cadeia continua íntegra após a contraprova', (await get('/api/frequencias/' + n.frequenciaId + '/evidencias-integridade')).cadeiaIntegra === true);
  report.scope = 'AUXILIARY_E2E10_REAL_CHROME_NIGHT_HISTORICAL_ALLOCATION_N30_PLUS_REAL_HTTP_OTHER_MANAGER_DENIAL_NOT_ORIGINAL_DRIVER';
  report.conditions = { referenceDate: n.day, movementDate: n.movement, frequencyId: n.frequenciaId, historicalAllocation: n.initialAlocacaoId,
    nextAllocation: n.dayAlocacaoId, nightLocation: n.lotacaoId, nightShift: n.jornadaId, serverDeadline: frequency.prazoEdicao,
    nativeClockNotChanged: true, physicalMidnightNotClaimed: true, noNewOperationalPermissions: true };
}, { reuse: false });
