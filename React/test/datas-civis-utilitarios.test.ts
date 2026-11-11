// @vitest-environment node
import { execFileSync } from 'node:child_process';
import { expect, it } from 'vitest';
const formatador = new URL('../src/core/formatters/valorAuditoria.ts', import.meta.url).href;
const jornada = new URL('../src/services/rh/ServicoDominioRelatorioFrequencia.ts', import.meta.url).href;
const negocio = new URL('../src/core/formatters/dataHoraNegocio.ts', import.meta.url).href;
const script = `
import { formatarValorAuditoria } from ${JSON.stringify(formatador)};
import { calcularDiaDaJornada } from ${JSON.stringify(jornada)};
import * as datasNegocio from ${JSON.stringify(negocio)};
console.log(JSON.stringify({
  civil: formatarValorAuditoria('2026-11-06'),
  instante: formatarValorAuditoria('2026-11-06T00:00:00Z'),
  periodos: ['2026-11-11T01:30:00Z', '2024-03-01T01:30:00Z', '2023-03-01T01:30:00Z', '2026-01-01T01:30:00Z']
    .map(instante => datasNegocio.obterPeriodoMensalNoFusoNegocio(Date.parse(instante))),
  periodoInvalido: datasNegocio.obterPeriodoMensalNoFusoNegocio(NaN),
  minutos: calcularDiaDaJornada({ dias: [
    { diaSemana: 'FRIDAY', diaTrabalho: true, entrada1: '08:00', saida1: '12:00' },
    { diaSemana: 'SATURDAY', diaTrabalho: false }
  ] }, '2026-11-06').minutosFisicos
}));
`;
it.each([
  ['UTC', '06/11/2026'],
  ['America/Sao_Paulo', '05/11/2026'],
  ['America/Los_Angeles', '05/11/2026'],
  ['Pacific/Kiritimati', '06/11/2026'],
])('preserva LocalDate e a escala, distinguindo Instant em %s', (TZ, instante) => {
  const resultado = JSON.parse(execFileSync(process.execPath, ['--input-type=module', '-e', script], { encoding: 'utf8', env: { ...process.env, TZ }, timeout: 10000 }));
  expect(resultado).toEqual({ civil: '06/11/2026', instante, minutos: 240, periodos: [
    { start: '2026-11-01', end: '2026-11-30' },
    { start: '2024-02-01', end: '2024-02-29' },
    { start: '2023-02-01', end: '2023-02-28' },
    { start: '2025-12-01', end: '2025-12-31' },
  ], periodoInvalido: null });
});
