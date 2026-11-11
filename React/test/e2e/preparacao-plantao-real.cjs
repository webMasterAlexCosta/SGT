'use strict';
const fs = require('node:fs');
const path = require('node:path');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend } = require('./contexto-fixture.cjs');
const cpfSynthetic = () => {
  let s = '967640001';
  for (const n of [9, 10]) { let sum = 0; for (let i = 0; i < n; i++) sum += Number(s[i]) * (n + 1 - i); const d = (sum * 10) % 11; s += d === 10 ? '0' : String(d); }
  return s;
};
execute('preparacao-plantao-real', async ({ context, fixture, account, open, check, recordBusiness, report }) => {
  const b = JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json')));
  let n = { ...(b.demo64Night || {}) };
  const save = values => { n = { ...n, ...values }; recordBusiness({ demo64Night: n }); };
  const base = baseFrontend(), start = '2026-11-05', day = '2026-11-06', movement = '2026-11-07', end = '2026-12-31';
  const marker = 'SINTETICO-DEMO64-NOTURNO';
  check('Preparação isolada ainda não encerrada; nenhuma intenção confirmada será repetida', fixture.synthetic && !n.ready);
  await open('/contratos');
  const csrfResponse = await context.request.get(base + '/api/autenticacao/csrf');
  const csrf = (await csrfResponse.json()).data;
  const headers = { Origin: base, [csrf.headerName]: csrf.token, 'X-User-Password': account.password };
  const request = async (method, route, data, expected = 200) => {
    const response = await context.request[method](base + route, { ...(data ? { data } : {}), headers, timeout: 15000 });
    const envelope = await response.json();
    report.http.push({ method: method.toUpperCase(), path: route.split('?')[0], status: response.status(), code: envelope.code || null });
    check(method.toUpperCase() + ' real ' + route.split('?')[0] + ' status' + expected, response.status() === expected,
      { status: response.status(), code: envelope.code || null, message: response.status() === expected ? undefined : envelope.message });
    return envelope.data ?? envelope;
  };
  const get = route => request('get', route);
  const post = (route, data, status = 200) => request('post', route, data, status);
  if (!n.jornadaId) {
    const list = await get('/api/jornadas');
    check('Não há jornada sem recibo a ser recriada', !list.some(j => j.descricao === marker));
    const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
    const j = await post('/api/jornadas', { descricao: marker, gestaoLocalId: fixture.ids.gestao_origem, ativo: true,
      regrasAdicionais: [], dias: days.map(diaSemana => ({ diaSemana, diaTrabalho: true, entrada1: '22:00', saida1: '06:00', entrada2: null, saida2: null })) }, 201);
    save({ jornadaId: j.id });
    check('Jornada própria22–06 com sete dias criada', j.id > 0 && j.gestaoLocalId === fixture.ids.gestao_origem && j.dias.length === 7);
  }
  if (!n.lotacaoId) {
    const list = await get('/api/lotacoes?size=100');
    check('Não há lotação sem recibo a ser recriada', !list.content.some(l => l.nome === marker));
    const source = await get('/api/lotacoes/' + fixture.ids.lotacao);
    const l = await post('/api/lotacoes', { nome: marker, gestaoLocalId: fixture.ids.gestao_origem,
      municipio: source.municipio, ativo: true, turnos: [{ id: n.jornadaId }] }, 201);
    save({ lotacaoId: l.id });
    check('Lotação própria contém somente o novo turno', l.id > 0 && l.gestaoLocalId === fixture.ids.gestao_origem
      && l.turnos.length === 1 && l.turnos[0].id === n.jornadaId);
  }
  for (const [type, id, endpoint, flag] of [['JORNADA', n.jornadaId, '/api/jornadas/', 'jornadaHistory'],
    ['LOTACAO', n.lotacaoId, '/api/lotacoes/', 'lotacaoHistory']]) {
    if (!n[flag]) {
      const origin = '/api/catalogos-temporais/' + type + '/' + id;
      const unknown = await get(origin + '/disponibilidade?data=' + start);
      check('Flag atual não é fabricada como disponibilidade no passado ' + type, unknown.disponibilidadeComprovada === false && unknown.ativo === null);
      const current = await get(endpoint + id);
      const payload = { dataEfetiva: start, ativo: true, versaoEsperada: current.version,
        motivo: 'Fonte documental sintética de disponibilidade do cenário noturno.',
        evidencia: 'Fixture SINTETICA da campanha, sem autenticidade administrativa institucional.',
        autorizacao: 'Preparação autorizada do laboratório isolado desta campanha.',
        chaveIdempotencia: 'DEMO64-DISP-' + type + '-' + id };
      const preview = await post(origin + '/transicoes/previa', payload);
      await post(origin + '/transicoes', { ...payload, confirmarImpacto: true, tokenPrevia: preview.tokenPrevia });
      save({ [flag]: true });
      const knownBefore = await get(origin + '/disponibilidade?data=' + start + '&conhecidoAte=' + encodeURIComponent(unknown.conhecidoAte));
      check('Conhecimento anterior continua desconhecido ' + type, knownBefore.disponibilidadeComprovada === false && knownBefore.ativo === null);
    }
  }
  for (const [type, id] of [['JORNADA', n.jornadaId], ['LOTACAO', n.lotacaoId], ['JORNADA', fixture.ids.jornada],
    ['LOTACAO', fixture.ids.lotacao], ['CARGO', fixture.ids.cargo], ['TIPO_CONTRATACAO', fixture.ids.tipo]]) {
    const d = await get('/api/catalogos-temporais/' + type + '/' + id + '/disponibilidade?data=' + start);
    check('Disponibilidade documentada cobre D sem alterar catálogos antigos ' + type + id,
      d.ativo === true && d.disponibilidadeComprovada === true && d.eventoFonteId > 0 && d.dataEfetivaFonte <= start);
  }
  if (!n.contratoId) {
    const existing = await get('/api/contratos');
    check('Contrato exclusivo não existe sem recibo', !existing.some(c => c.numeroContrato === marker));
    const providers = await get('/api/fornecedores'), provider = providers.find(p => p.id === fixture.ids.fornecedor);
    check('Fornecedor original permanece ativo no cadastro', provider?.ativo === true);
    const contract = await post('/api/contratos', { numeroContrato: marker, anoContrato: 2026,
      descricao: 'SINTETICO: cenário exclusivo de plantão e responsabilidade histórica, sem valor administrativo.',
      dataInicio: start, dataFim: end, gestaoLocalId: fixture.ids.gestao_origem, tipoContratacaoId: fixture.ids.tipo,
      fornecedorId: provider.id, cnpjEmpresa: provider.cnpj, razaoSocialEmpresa: provider.razaoSocial,
      nomeFantasiaEmpresa: marker, efetivoPrevisto: 2, matriculaVinculoObrigatoria: false }, 201);
    save({ contratoId: contract.id });
    check('Novo contrato é rascunho próprio e não altera contratos anteriores', contract.id > 0 && contract.estadoNegocial === 'RASCUNHO'
      && contract.gestaoLocalId === fixture.ids.gestao_origem && contract.fornecedorId === provider.id);
  }
  for (const [key, lotacaoId, jornadaId] of [['nightCompositionId', n.lotacaoId, n.jornadaId], ['dayCompositionId', fixture.ids.lotacao, fixture.ids.jornada]]) {
    if (!n[key]) {
      const composition = await post('/api/contratos/' + n.contratoId + '/composicoes', { lotacaoId, jornadaId,
        cargoId: fixture.ids.cargo, efetivoPrevisto: 1, dataInicioVigencia: start, dataFimVigencia: end }, 201);
      save({ [key]: composition.id });
    }
  }
  if (!n.published) {
    const publication = { dataPublicacao: start, referenciaSei: marker,
      justificativa: 'Publicação sintética para o plantão, com fonte declarada agora e efeito desde05/11.' };
    const preview = await post('/api/contratos/' + n.contratoId + '/previas/publicar', publication);
    const actual = await post('/api/contratos/' + n.contratoId + '/publicar', { ...publication, confirmarImpacto: true, tokenPrevia: preview.tokenPrevia });
    save({ published: true });
    check('Publicação confirma capacidade original2 e composição integral', actual.estadoNegocial === 'PUBLICADO' && actual.efetivoOriginalPublicado === 2);
  }
  if (!n.vinculoId) {
    const cpf = cpfSynthetic();
    const existing = await get('/api/pessoas?busca=' + cpf);
    check('Pessoa sintética própria ainda não existe', existing.totalElements === 0);
    const v = await post('/api/vinculos-contratuais', { pessoa: { nome: marker, cpf }, contratoId: n.contratoId,
      dataInicio: start, dataFim: end, matricula: marker, composicaoId: n.nightCompositionId }, 201);
    save({ vinculoId: v.id, pessoaId: v.pessoaId, cpf });
  }
  const positionPath = '/api/vinculos-contratuais/' + n.vinculoId + '/alocacoes/posicao?data=';
  const before = await get(positionPath + day);
  if (!n.initialAlocacaoId) save({ initialAlocacaoId: before.alocacaoId });
  if (!n.moved) {
    const move = { dataEfetiva: movement, composicaoId: n.dayCompositionId,
      justificativa: 'Movimento sintético no dia seguinte ao início do plantão22–06.',
      evidencia: 'Fixture SINTETICA, sem validade administrativa.', autorizacao: 'Preparação autorizada do laboratório.' };
    const route = '/api/vinculos-contratuais/' + n.vinculoId;
    const preview = await post(route + '/previas/alocacoes', move);
    const moved = await post(route + '/alocacoes', { ...move, confirmarImpacto: true, tokenPrevia: preview.token });
    save({ moved: true, dayAlocacaoId: moved.id });
  }
  const historical = await get(positionPath + day), following = await get(positionPath + movement);
  check('Plantão mantém alocação de início, dia seguinte usa a nova faixa', historical.alocacaoId === n.initialAlocacaoId
    && historical.lotacaoId === n.lotacaoId && historical.jornadaId === n.jornadaId
    && following.alocacaoId === n.dayAlocacaoId && following.lotacaoId === fixture.ids.lotacao && following.jornadaId === fixture.ids.jornada);
  if (!n.frequenciaId) {
    const payload = { vinculoContratualId: n.vinculoId, dataReferencia: day, status: 'PRESENCA', entrada1: '22:00', saida1: '06:00',
      justificativaRetificacao: 'Registro sintético pelo Gestor do plantão com início anterior ao movimento.', chaveIdempotencia: 'DEMO64-PLANTAO-20261106' };
    const f = await post('/api/frequencias', payload);
    save({ frequenciaId: f.id, frequencyPayload: payload, frequencyDTO: f });
  }
  const persisted = await get('/api/frequencias?data=' + day + '&contratoId=' + n.contratoId + '&lotacaoId=' + n.lotacaoId + '&jornadaId=' + n.jornadaId);
  const f = persisted.content.find(item => item.id === n.frequenciaId);
  check('Frequência confirmada22–06 e N30, sem regra de saída no dia seguinte', f?.id > 0 && f.alocacaoVinculoId === n.initialAlocacaoId
    && f.lotacaoId === n.lotacaoId && f.jornadaId === n.jornadaId && f.dataReferencia === day
    && f.prazoEdicao.startsWith('2026-12-06T00:00') && f.bloqueadaEdicaoOperacional === true);
  const events = await get('/api/frequencias/' + n.frequenciaId + '/eventos');
  // Physical minutes are in the immutable event snapshot, not FrequenciaDTO.
  // horasTrabalhadas is a separate payable calculation and is not an8h oracle.
  check('Fonte auditada preserva480min físicos e um único evento', events.length === 1
    && /(?:^|;)minutosFisicos=480(?:;|$)/.test(events[0].snapshotNovo));
  check('Cadeia da fonte sintética é íntegra', (await get('/api/frequencias/' + n.frequenciaId + '/evidencias-integridade')).cadeiaIntegra === true);
  save({ ready: true, start, day, movement, end, marker });
  report.scope = 'PREPARATION_ONLY_REAL_API_NEW_SYNTHETIC_CORPUS_NO_OLD_TURN_OR_LINK_CHANGE_NOT_CHROME_ACCEPTANCE';
  report.corpus = { contract: n.contratoId, link: n.vinculoId, nightShift: n.jornadaId, nightLocation: n.lotacaoId,
    initialAllocation: n.initialAlocacaoId, nextAllocation: n.dayAlocacaoId, frequency: n.frequenciaId, day, movement };
}, { reuse: false });
