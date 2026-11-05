// Adaptador de distribuição: conserva os probes e aponta para o projeto entregue.
import { readFileSync } from 'node:fs';
import { parseEnv } from 'node:util';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

const frontend = new URL('../', import.meta.url);
const probes = new URL('../../../review/probes/', import.meta.url);
const env = Object.fromEntries(
  Object.entries(parseEnv(readFileSync(new URL('.env.example', frontend), 'utf8')))
    .filter(([key]) => key.startsWith('VITE_')),
);

export default defineConfig({
  root: probes.pathname,
  envDir: false,
  cacheDir: new URL('node_modules/.cache/revisao-independente/', frontend).pathname,
  plugins: [react()],
  resolve: { alias: { '@review': new URL('src', frontend).pathname } },
  test: {
    env,
    environment: 'jsdom',
    globals: false,
    include: [
      '*.independent.test.{js,jsx}',
      'canonical/*.independent.test.{js,jsx}',
      'session/*.independent.test.{js,jsx}',
    ],
    maxWorkers: 1,
    fileParallelism: false,
    testTimeout: 10000,
  },
});
