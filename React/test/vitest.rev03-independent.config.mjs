import { defineConfig } from 'vitest/config';
import base from '../vitest.config.js';

// Cópia byte a byte do probe congelado; nome .probe.jsx exclui execução geral.
export default defineConfig({
  ...base,
  cacheDir: new URL('../node_modules/.cache/rev03-working', import.meta.url).pathname,
  resolve: { alias: { '@review': new URL('../src/', import.meta.url).pathname } },
  test: { ...base.test, include: ['test/probes/rev03-canonical-original.probe.jsx'], maxWorkers: 1, fileParallelism: false },
});
