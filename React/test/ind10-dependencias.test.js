import { readFileSync } from 'node:fs';
import { expect, it } from 'vitest';
const ler = p => JSON.parse(readFileSync(new URL(p, import.meta.url), 'utf8'));
it('IND-10: fixa patches corrigidos compatíveis e mantém família Vitest alinhada no lockfile', () => {
  const manifest = ler('../package.json');
  const lock = ler('../package-lock.json');
  for (const [nome, versao] of Object.entries({ vite:'8.2.4', vitest:'4.1.12', '@vitest/coverage-v8':'4.1.12' })) {
    expect(manifest.devDependencies[nome],nome).toBe(versao);
    expect(lock.packages[''].devDependencies[nome],nome).toBe(versao);
    expect(lock.packages[`node_modules/${nome}`].version,nome).toBe(versao);
  }
  expect(lock.packages['node_modules/@vitest/mocker'].version).toBe('4.1.12');
  expect(ler('../node_modules/@vitest/mocker/package.json').version).toBe('4.1.12');
});
