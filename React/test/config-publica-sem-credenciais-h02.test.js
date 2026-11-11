import { afterEach, expect, test, vi } from 'vitest';
import { ServicoSistema } from '../src/services/system/ServicoSistema.ts';

afterEach(() => vi.unstubAllGlobals());

test('sondagem pública de latência não carrega a sessão autenticada para o filtro JWT', async () => {
  const fetchPublico = vi.fn().mockResolvedValue({ ok: true, status: 200 });
  vi.stubGlobal('fetch', fetchPublico);
  const tempo = await ServicoSistema.medirLatencia('http://127.0.0.1:18463');
  expect(Number.isFinite(tempo)).toBe(true);
  expect(fetchPublico).toHaveBeenCalledTimes(1);
  const [url, opcoes] = fetchPublico.mock.calls[0];
  expect(url).toMatch(/^http:\/\/127\.0\.0\.1:18463\/config\.json\?t=\d+$/);
  expect(opcoes).toMatchObject({ method: 'GET', cache: 'no-store', credentials: 'omit' });
  expect(opcoes.headers).toEqual({ Accept: 'application/json' });
});
