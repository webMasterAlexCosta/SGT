'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend, contratoDaFixture } = require('./contexto-fixture.cjs');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
execute('persistencia-integridade-real', async ({ page, context, fixture, open, check, report, screenshot }) => {
  const runtime = process.env.SGT_TEST_RUNTIME;
  const b = JSON.parse(fs.readFileSync(path.join(runtime, 'private/e2e-business.json')));
  const manifest = JSON.parse(fs.readFileSync(process.env.SGT_E2E_MANIFEST));
  check('Mesmo JAR reaberto sobre o corpus sintético durável', fixture.synthetic && manifest.mode === 'jar'
    && manifest.java.jar_sha256 === 'f9d0a761f139094ce1db585a1cc818843482f003c247c6ba03b66ebf3af9f4c6'
    && b.documentoRetificadoE2EConcluido && b.atestado07Completo && b.jobs200CancelReprocessamentoConcluido);
  const base = baseFrontend(), contract = contratoDaFixture(fixture);
  await open('/contratos');
  const get = async (route, params) => {
    const r = await context.request.get(base + route, { params, timeout: 12000 });
    check('GET real após reinício: ' + route, r.status() === 200, { status: r.status() });
    return (await r.json()).data;
  };
  const previous = await get(contract.api + '/documentos/' + b.documentoPublicadoE2EId);
  const current = await get(contract.api + '/documentos/' + b.documentoRetificadoE2EId);
  check('v1 substituída e v2 publicada preservam cadeia, SEI, versão JPA e hashes',
    previous.estado === 'SUBSTITUIDO' && previous.versaoDocumento === 1
    && current.estado === 'PUBLICADO' && current.versaoDocumento === 2
    && current.documentoAnteriorId === previous.id && current.grupoLogicoId === previous.grupoLogicoId
    && current.version === b.documentoRetificadoE2EPrimeiraVersaoJPA
    && current.referenciaSei === b.documentoRetificadoE2EPrimeiroRecibo.referenciaSei
    && [previous, current].every(d => d.sha256Arquivo === b.documentoPublicadoE2EHash));
  const events = await get(contract.api + '/documentos/' + current.id + '/eventos');
  check('Uma única retificação documental continua auditada após reinício', events.filter(e => e.tipoEvento === 'RETIFICACAO_SEI').length === 1 && events.some(e => e.tipoEvento === 'PUBLICACAO'));
  const document = await context.request.get(base + contract.api + '/documentos/' + current.id + '/arquivo');
  const documentBytes = await document.body();
  check('Arquivo real da v2 mantém tamanho e SHA do upload original', document.status() === 200
    && documentBytes.length === b.documentoPublicadoE2ETamanho && sha(documentBytes) === b.documentoPublicadoE2EHash);
  const occurrence = await get('/api/ocorrencias/' + b.atestado07Id, { finalidade: 'Conferência sintética de integridade após reinício.' });
  check('Atestado mantém três dias e o mesmo anexo restrito', occurrence.cancelada === false
    && occurrence.ocorrencia.quantidadeDias === 3 && occurrence.ocorrencia.dataOcorrencia === b.atestado07Dates[0]
    && occurrence.ocorrencia.anexos.some(a => a.id === b.atestado07Anexo && a.classificacaoAcesso === 'PESSOAL_RESTRITO'));
  const attachment = await context.request.get(base + '/api/ocorrencias/anexos/' + b.atestado07Anexo,
    { params: { finalidade: 'Conferência sintética dos bytes após reinício.' } });
  check('Anexo restrito entregue pela API mantém SHA original', attachment.status() === 200 && sha(await attachment.body()) === b.atestado07Sha);
  for (const day of b.atestado07Dates) {
    const data = await get('/api/frequencias', { data: day, contratoId: String(b.jobs200ContratoId),
      lotacaoId: String(fixture.ids.lotacao), jornadaId: String(fixture.ids.jornada), size: 500 });
    const rows = data.content.filter(f => f.vinculoContratualId === b.atestado07Vinculo);
    check('Efeito AT único preservado em ' + day, rows.length === 1 && rows[0].status === 'ATESTADO');
    const integrity = await get('/api/frequencias/' + rows[0].id + '/evidencias-integridade');
    check('Cadeia da frequência continua íntegra em ' + day, integrity.cadeiaIntegra === true);
  }
  for (const [id, attempt, inserted, day] of [[b.jobs200GradeId, 1, 200, b.jobs200GradeData],
    [b.jobs200CancelId, 2, 200 - b.jobs200CancelPrefix, b.jobs200CancelData]]) {
    const job = await get('/api/frequencias/fechamentos/' + id);
    check('Job ' + id + ' preserva ID, tentativa, conclusão e contadores', job.id === id && job.status === 'CONCLUIDO'
      && job.tentativa === attempt && job.dataReferencia === day && job.totalPrevisto === inserted
      && job.totalProcessado === inserted && job.totalInserido === inserted && job.totalAtualizado === 0);
    const frequencies = await get('/api/frequencias', { data: day, contratoId: String(b.jobs200ContratoId),
      lotacaoId: String(fixture.ids.lotacao), jornadaId: String(fixture.ids.jornada), size: 500 });
    check('Job ' + id + ' permanece com 200 frequências únicas sem reexecutar o fechamento', frequencies.content.length === 200
      && new Set(frequencies.content.map(f => f.id)).size === 200
      && new Set(frequencies.content.map(f => f.vinculoContratualId)).size === 200);
  }
  await page.getByRole('button', { name: contract.botaoAbrir, exact: true }).click();
  const detail = page.getByRole('dialog', { name: contract.titulo, exact: true });
  await detail.waitFor();
  check('Modal real do contrato reabre após reinício', await detail.isVisible());
  await screenshot('contrato-reaberto-integridade');
  report.scope = 'SUPPLEMENT_AFTER_FULL_OWN_LAB_AND_SAME_JAR_RESTART_NOT_AN_ORIGINAL_DRIVER_NOT_LAN';
}, { reuse: false });
