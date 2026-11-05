// Executa a evidência recebida sem editar seu import @sgt nem suas assertions.
// O arquivo original fica fora de test/ para não depender deste alias na suíte comum.
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';
import base from '../vitest.config.js';

export default defineConfig({
  ...base,
  resolve: { alias: { '@sgt': fileURLToPath(new URL('../src/', import.meta.url)) } },
  test: { ...base.test, include: ['auditoria-recebida/semantica-null-historico.test.jsx'] },
});
