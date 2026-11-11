// @vitest-environment node
import { execFileSync } from 'node:child_process';
import { describe, expect, it } from 'vitest';

const modulo = new URL('../src/core/formatters/dataHoraNegocio.ts', import.meta.url).href;
const script = `
import { obterDataNoFusoNegocio, interpretarDataHoraNoFusoNegocio } from ${JSON.stringify(modulo)};
const prazos = [
  '2026-11-02T00:00:00', '2026-11-02T03:00:00Z', '2026-11-02T00:00:00-03:00',
  '2018-11-04T01:00:00', '2018-12-01T00:00:00', '2019-01-01T00:00:00.123456',
  '2018-11-04T00:00:00', '2026-02-30T00:00:00', 'valor-invalido'
].map(valor => {
  const instante = interpretarDataHoraNoFusoNegocio(valor);
  return Number.isFinite(instante) ? new Date(instante).toISOString() : null;
});
console.log(JSON.stringify({
  fusoExecutado: Intl.DateTimeFormat().resolvedOptions().timeZone,
  datas: ['2026-11-02T02:59:59.999Z', '2026-11-02T03:00:00.000Z', '2026-11-02T03:00:00.001Z'].map(obterDataNoFusoNegocio),
  prazos
}));
`;

describe('Representação de datas de negócio independente do fuso do processo', () => {
  it.each(['UTC', 'America/Sao_Paulo', 'Asia/Tokyo', 'America/Los_Angeles'])(
    'conserva os mesmos instantes e datas civis com TZ=%s', (fuso) => {
      const resultado = JSON.parse(execFileSync(process.execPath, ['--input-type=module', '-e', script], {
        encoding: 'utf8', env: { ...process.env, TZ: fuso }, timeout: 10000,
      }));
      expect(resultado).toEqual({
        fusoExecutado: fuso,
        datas: ['2026-11-01', '2026-11-02', '2026-11-02'],
        prazos: [
          '2026-11-02T03:00:00.000Z', '2026-11-02T03:00:00.000Z', '2026-11-02T03:00:00.000Z',
          '2018-11-04T03:00:00.000Z', '2018-12-01T02:00:00.000Z', '2019-01-01T02:00:00.123Z',
          null, null, null,
        ],
      });
    },
  );
});
