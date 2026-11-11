// Infraestrutura disponível neste frontend; não representa homologação de negócio.
import { readFileSync } from 'node:fs';
import { parseEnv } from 'node:util';
import react from '@vitejs/plugin-react';
import { configDefaults, defineConfig } from 'vitest/config';

// O exemplo contém somente valores públicos e sanitizados. A suíte não depende
// de .env/.env.local e não usa os parâmetros DEV_* para iniciar serviços reais.
// Este carregamento pertence apenas ao Vitest; a configuração de produção
// continua exigindo as variáveis obrigatórias de seu próprio ambiente.
const ambienteTeste = Object.fromEntries(
  Object.entries(parseEnv(readFileSync(new URL('./.env.example', import.meta.url), 'utf8')))
    .filter(([chave]) => chave.startsWith('VITE_')),
);

export default defineConfig({
  envDir: false,
  plugins: [react()],
  // Permite executar os testes adversariais independentes sem reescrevê-los.
  resolve: { alias: { '@sgt': new URL('./src', import.meta.url).pathname, '@review': new URL('./src', import.meta.url).pathname } },
  test: {
    // Mantém o Web Storage do jsdom; Node26 oferece globais nativas conflitantes.
    execArgv: ['--no-experimental-webstorage'],
    env: ambienteTeste,
    environment: 'jsdom',
    globals: false,
    include: ['test/**/*.{test,spec}.{js,jsx,mjs,cjs,ts,tsx,mts,cts}'],
    // Este arquivo usa node:test e deve continuar sendo executado pelo Node.
    exclude: [...configDefaults.exclude, 'test/preparacao-local.test.mjs'],
    maxWorkers: 1,
    fileParallelism: false,
    testTimeout: 15000,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json-summary', 'lcov'],
      reportsDirectory: 'reports/vitest/coverage',
    },
  },
});
