// @vitest-environment node
import { readFileSync } from 'node:fs';
import { parseEnv } from 'node:util';
import { describe, expect, it } from 'vitest';
import { criarConfiguracaoFrontend } from '../src/core/configuracaoFrontend.js';

const exemplo = parseEnv(readFileSync(new URL('../.env.example', import.meta.url), 'utf8'));

describe('Configuração reproduzível da suíte sem ambiente privado', () => {
  it('importa a configuração real do aplicativo usando o exemplo sanitizado nos testes', async () => {
    const { CONFIG } = await import('../src/core/config.js');
    expect(CONFIG.API_URL).toBe(exemplo.VITE_API_URL);
    expect(CONFIG.APP_NAME).toBe(exemplo.VITE_APP_NAME);
    expect(CONFIG.HTTP_TIMEOUT).toBe(Number(exemplo.VITE_HTTP_TIMEOUT));
    expect(CONFIG.PAGINATION_DEFAULT_SIZE).toBe(5);
    expect(CONFIG.SHOW_TEST_CREDENTIALS).toBe(false);
    expect(CONFIG.TEST_GLOBAL_PASSWORD).toBe('');
    expect(CONFIG.TEST_GESTOR_PASSWORD).toBe('');
  });

  it('continua recusando produção sem as variáveis obrigatórias', () => {
    expect(() => criarConfiguracaoFrontend({}, { producao: true })).toThrow('VITE_API_URL é obrigatória');
    expect(() => criarConfiguracaoFrontend({ ...exemplo, VITE_HTTP_TIMEOUT: '' }, { producao: true }))
      .toThrow('VITE_HTTP_TIMEOUT é obrigatória');
  });

  it('continua omitindo credenciais de teste em produção mesmo quando solicitadas', () => {
    const configuracao = criarConfiguracaoFrontend({
      ...exemplo,
      VITE_SHOW_TEST_CREDENTIALS: 'true',
      VITE_TEST_GLOBAL_PASSWORD: 'segredo-sintetico-nao-publicavel',
      VITE_TEST_GESTOR_PASSWORD: 'segredo-sintetico-nao-publicavel',
    }, { producao: true });
    expect(configuracao.SHOW_TEST_CREDENTIALS).toBe(false);
    expect(configuracao.TEST_GLOBAL_PASSWORD).toBe('');
    expect(configuracao.TEST_GESTOR_PASSWORD).toBe('');
  });
});
