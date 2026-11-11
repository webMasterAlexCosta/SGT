'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend } = require('./contexto-fixture.cjs');
const phase = process.env.SGT_PENDENCIA_PHASE;
const actors = { fonte: 'gestor_local', lancamentos: 'audit_operacional', retirar: 'gestor_local',
  escopo: 'gestor_destino', contraprova: 'audit_operacional', resolver: 'gestor_local' };
if (!actors[phase]) throw new Error('Selecione uma fase explícita de pendência.');
const hash = v => crypto.createHash('sha256').update(JSON.stringify(v)).digest('hex');
const unwrap = v => v?.data ?? v;
const plus = (d, n) => { const v = new Date(d + 'T12:00:00Z'); v.setUTCDate(v.getUTCDate() + n); return v.toISOString().slice(0, 10); };
const cpf = index => { let s = String(930550000 + index); for (const n of [9,10]) { let sum = 0; for (let i = 0; i < n; i++) sum += Number(s[i]) * (n+1-i); const v = (sum*10)%11; s += v === 10 ? '0' : v; } return s; };
const motive = 'SGT-PEND08: fonte e tratamento sintéticos do recorte de um único dia.';
execute('pendencia-recorte-real-' + phase, async ({ page, context, fixture, account, open, check, report, recordBusiness, screenshot }) => {
  const base = baseFrontend();
  const b = JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json')));
  const subject = b.jobs200People[0], cid = b.jobs200ContratoId;
  const first = plus(b.jobs200DataInicio, 2), other = plus(first, 1);
  const frequency = '/api/frequencias', exceptions = '/api/frequencias-excecoes', pending = '/api/pendencias';
  const common = { contratoId: cid, lotacaoId: fixture.ids.lotacao, jornadaId: fixture.ids.jornada };
  const filter = { inicio: first, fim: other, contratoIds: cid, tipo: 'RETIFICACAO_FREQUENCIA' };
  const old = b.pend08 || {};
  const state = partial => recordBusiness({ pend08: { ...old, ...partial } });
  check('Mesmo vínculo canônico, datas distintas dentro da janela, nenhum fechamento para elas', fixture.synthetic && account.id === fixture.accounts[actors[phase]].id &&
    subject?.vinculoId > 0 && cid > 0 && first < other && other < fixture.dates.today &&
    [first, other].every(d => ![b.jobs200GradeData, b.jobs200CancelData].includes(d)) && subject.vinculoId !== b.atestado07Vinculo);
  await open(['retirar','escopo','resolver'].includes(phase) ? '/pendencias' : (phase === 'fonte' ? '/frequencia-ajustes' : '/frequencia'));
  const headers = { Origin: base, Accept: 'application/json' };
  const request = async (method, route, expected, data, params) => {
    const r = await context.request.fetch(base + route, { method, headers, data, params, timeout: 12000 });
    const body = r.status() === 204 ? null : await r.json();
    report.http.push({ method, path: route, status: r.status(), code: body?.code ?? null });
    check(method + ' ' + route + ' → ' + expected, r.status() === expected, { status: r.status(), code: body?.code, message: r.ok() ? undefined : body?.message });
    return { data: unwrap(body), message: body?.message };
  };
  const get = async (route, params) => (await request('GET', route, 200, undefined, params)).data;
  const csrf = await get('/api/autenticacao/csrf');
  headers[csrf.headerName] = csrf.token; headers['X-User-Password'] = account.password;
  const readDay = async day => (await get(frequency, { ...common, data: day, size: 100 })).content.find(r => r.vinculoContratualId === subject.vinculoId);
  const events = id => get(frequency + '/' + id + '/eventos');
  const list = () => get(pending, filter);
  const item = async () => (await list()).registros.find(r => r.frequenciaId === old.affected);
  const write = (day, status, label, expected, fields = {}) => request('POST', frequency, expected,
    { vinculoContratualId: subject.vinculoId, dataReferencia: day, status, chaveIdempotencia: 'SGT-PEND08-' + label, ...fields });
  const hours = { entrada1: '08:00', saida1: '12:00', entrada2: '13:00', saida2: '17:00' };
  if (phase === 'fonte') {
    check('Nenhum lançamento ou fonte deste cenário existe', !old.source && !await readDay(first) && !await readDay(other));
    const matches = await get('/api/empregados', { search: cpf(subject.index), size: 20 });
    const found = (Array.isArray(matches) ? matches : matches.content).filter(x => String(x.cpf).replace(/\D/g,'') === cpf(subject.index));
    check('Busca canônica identifica uma projeção de empregado', found.length === 1 && found[0].id > 0);
    const { data } = await request('POST', exceptions, 201, { empregadoId: found[0].id, dataInicio: first, dataFim: first,
      tipo: 'FOLGA_REMUNERADA', status: 'APROVADO', observacao: motive });
    if (data.id > 0) state({ source: data.id, employee: found[0].id });
    check('Fonte real ativa para um único dia e empregado', data.id > 0 && data.empregadoId === found[0].id && data.dataInicio === first && data.dataFim === first);
  } else if (phase === 'lancamentos') {
    check('Fonte observada, nenhum lançamento repetido', old.source > 0 && !old.affected && !old.control && !await readDay(first) && !await readDay(other));
    const a = (await write(first, 'FOLGA', 'INICIAL-A', 200)).data;
    if (a.id > 0) state({ affected: a.id });
    const z = (await write(other, 'FALTA', 'INICIAL-B', 200)).data;
    if (z.id > 0) state({ affected: a.id, control: z.id });
    check('Mesmo Operacional cria ambas as frequências válidas e editáveis', a.id > 0 && z.id > 0 && a.id !== z.id
      && [a,z].every(r => r.validadeOperacional === 'VALIDA' && !r.bloqueadaEdicaoOperacional && r.estadoNegocial === 'ABERTA'));
  } else if (phase === 'retirar') {
    check('Somente fonte deste caso pode ser retirada', old.source > 0 && old.affected > 0 && old.control > 0 && !old.removed);
    const beforeOther = await readDay(other), beforeAffected = await readDay(first);
    check('Vínculo e frequências esperados confirmados antes da retirada', beforeAffected?.id === old.affected && beforeOther?.id === old.control
      && beforeAffected.status === 'FOLGA' && beforeOther.status === 'FALTA' && !await item());
    await request('DELETE', exceptions + '/' + old.source, 204, undefined, { justificativa: motive });
    state({ removed: true });
    const after = await readDay(first), p = await item();
    if (p?.id > 0) state({ removed: true, id: p.id });
    check('Revisão e pendência alcançam somente D1 e preservam o registro original', after.id === old.affected && after.status === 'FOLGA'
      && after.estadoAuditoria === 'SUJEITA_REVISAO' && after.bloqueadaEdicaoOperacional && hash(await readDay(other)) === hash(beforeOther)
      && p?.id > 0 && p.dataEfetiva === first && p.estado === 'ABERTA' && p.gestaoLocalId === fixture.ids.gestao_origem);
    await page.getByRole('button', { name: 'Atualizar', exact: true }).click();
    await page.getByText(new RegExp('^#' + p.id + ' ·')).waitFor();
    await screenshot('pendencia-visivel-somente-no-escopo');
  } else if (phase === 'escopo') {
    check('Gestão destino não lista o caso nem pode iniciar tratamento', old.id > 0 && !(await list()).registros.some(x => x.id === old.id));
    await request('POST', pending + '/' + old.id + '/iniciar-tratamento', 404, { observacao: motive });
    check('Tratamento da Gestão alheia não expõe nem altera a pendência', !(await list()).registros.some(x => x.id === old.id));
    state({ scopeRefusal: true });
  } else if (phase === 'contraprova') {
    check('Pendência aberta deste roteiro, sem escrita de controle anterior', old.id > 0 && old.scopeRefusal && !old.positive);
    const affected = await readDay(first), control = await readDay(other), before = await events(old.affected), controlEvents = await events(old.control);
    check('D1 bloqueado e D2 editável antes de tentar', affected.id === old.affected && affected.bloqueadaEdicaoOperacional &&
      control.id === old.control && !control.bloqueadaEdicaoOperacional);
    const blocked = await write(first, 'FALTA', 'RECUSA-D1', 403);
    check('A recusa é por revisão, ainda na janela operacional', /intervenção do Gestor Local/i.test(blocked.message || '')
      && hash(await readDay(first)) === hash(affected) && hash(await events(old.affected)) === hash(before));
    const updated = (await write(other, 'PRESENCA', 'ESCRITA-D2', 200, hours)).data;
    if (updated.id === old.control && updated.status === 'PRESENCA') state({ positive: true });
    const after = await events(old.control);
    check('Mesmo Operacional gravou D2 na mesma raiz, uma versão a mais, sem tocar D1', updated.id === old.control && updated.status === 'PRESENCA'
      && updated.versaoNegocial > control.versaoNegocial && after.length === controlEvents.length + 1 && hash(await readDay(first)) === hash(affected)
      && hash(await events(old.affected)) === hash(before));
  } else {
    check('Somente a pendência do caso, após as contraprovas', old.id > 0 && old.positive && !old.resolved);
    const p = await item(), pApi = pending + '/' + old.id;
    check('Origem, escopo e estado aberto seguem intactos', p.id === old.id && p.estado === 'ABERTA' && p.frequenciaId === old.affected && p.dataEfetiva === first);
    const r = (await request('POST', pApi + '/iniciar-tratamento', 200, { observacao: motive })).data;
    if (r.estado === 'EM_TRATAMENTO') state({ started: true });
    await request('POST', pApi + '/confirmar-resolucao', 409, { observacao: motive });
    check('A pendência segue em tratamento até a causa ser corrigida', (await item()).estado === 'EM_TRATAMENTO');
    const before = await events(old.affected), control = await readDay(other);
    const corrected = (await write(first, 'PRESENCA', 'REGULARIZAR-D1', 200, { ...hours, justificativaRetificacao: motive })).data;
    if (corrected.id === old.affected && corrected.status === 'PRESENCA') state({ started: true, rectified: true });
    check('Correção possui a mesma raiz com nova versão e validade', corrected.id === old.affected && corrected.status === 'PRESENCA' && corrected.validadeOperacional === 'VALIDA');
    const resolved = (await request('POST', pApi + '/confirmar-resolucao', 200, { observacao: motive })).data;
    if (resolved.estado === 'RESOLVIDA') state({ started: true, rectified: true, resolved: true });
    const history = await events(old.affected);
    check('Resolução mantém origem e histórico de D1, sem alterar D2', resolved.id === old.id && resolved.estado === 'RESOLVIDA' && resolved.origemId === p.origemId
      && resolved.frequenciaId === p.frequenciaId && before.every(x => history.some(y => hash(y) === hash(x))) && history.length > before.length
      && hash(await readDay(other)) === hash(control));
    for (const id of [old.affected, old.control]) check('Cadeia íntegra ' + id, (await get(frequency + '/' + id + '/evidencias-integridade')).cadeiaIntegra === true);
  }
  report.scope = { contract: cid, link: subject.vinculoId, first, other, phase, noSQL: true };
}, { accountName: actors[phase], reuse: false });
