// Smoke test somente da infraestrutura de testes; não demonstra regras de negócio SGT/GTRAT.
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    include: ['test/dependencias-stack.test.jsx'],
    maxWorkers: 1,
    testTimeout: 15000,
  },
});
