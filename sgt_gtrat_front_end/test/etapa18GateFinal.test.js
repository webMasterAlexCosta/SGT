import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile, access } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  AUTORIDADES,
  PERFIS,
  autoridadeDoPerfil,
  podeAcessarPainelGerencial,
  podeAcessarRelatorios,
  podeAcessarRelatoriosAdministrativos,
  podeEmitirRelatoriosAdministrativos,
  podeVisualizarContratos,
  podeVisualizarFrequencia,
  podeGerenciarUsuarios,
  podeAcessarAuditoria,
  podeAcessarPendenciasAdministrativas,
} from '../src/core/security/roles.js';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const existe = async (nome) => access(path.join(raiz, nome)).then(() => true).catch(() => false);
const usuario = (perfil) => ({ perfil, autoridade: autoridadeDoPerfil(perfil) });

test('candidato não inclui arquivos locais de segredo/certificado', async () => {
  for (const nome of ['.env', '.env.development', 'keystore.p12']) {
    assert.equal(await existe(nome), false, `${nome} não pode integrar o candidato`);
  }
  assert.equal(await existe('.env.example'), true);
  const exemplo = await readFile(path.join(raiz, '.env.example'), 'utf8');
  assert.match(exemplo, /^VITE_TEST_GLOBAL_PASSWORD=\s*$/m);
  assert.match(exemplo, /^VITE_TEST_GESTOR_PASSWORD=\s*$/m);
  assert.match(exemplo, /^DEV_HTTPS_PFX_PASSPHRASE=\s*$/m);
});

test('runtime/dependências preservam React 19, Vite e Node 22 sem Tailwind', async () => {
  const pkg = JSON.parse(await readFile(path.join(raiz, 'package.json'), 'utf8'));
  assert.match(pkg.engines.node, /22/);
  assert.match(pkg.dependencies.react, /^19\./);
  assert.ok(pkg.devDependencies.vite);
  const deps = { ...pkg.dependencies, ...pkg.devDependencies };
  assert.equal(Object.keys(deps).some((nome) => /tailwind/i.test(nome)), false);
});

test('seis perfis canônicos permanecem fechados e sem ROLE_*', () => {
  assert.deepEqual(Object.values(PERFIS), [
    'ADMINISTRADOR', 'GESTOR_NACIONAL', 'GESTOR_ESTADUAL', 'GESTOR_LOCAL', 'PRESIDENTE', 'OPERACIONAL',
  ]);
  for (const perfil of Object.values(PERFIS)) {
    assert.equal(autoridadeDoPerfil(perfil), `PERFIL_${perfil}`);
    assert.equal(AUTORIDADES[perfil], `PERFIL_${perfil}`);
  }
});

test('matriz gerencial/operacional crítica permanece coerente', () => {
  const admin = usuario(PERFIS.ADMINISTRADOR);
  const nacional = usuario(PERFIS.GESTOR_NACIONAL);
  const estadual = usuario(PERFIS.GESTOR_ESTADUAL);
  const local = usuario(PERFIS.GESTOR_LOCAL);
  const presidente = usuario(PERFIS.PRESIDENTE);
  const operacional = usuario(PERFIS.OPERACIONAL);

  for (const u of [admin, nacional, estadual, local, presidente]) assert.equal(podeAcessarPainelGerencial(u), true);
  assert.equal(podeAcessarPainelGerencial(operacional), false);

  for (const u of [admin, nacional, estadual, local]) assert.equal(podeAcessarRelatorios(u), true);
  assert.equal(podeAcessarRelatorios(presidente), false);
  assert.equal(podeAcessarRelatorios(operacional), false);

  for (const u of [admin, nacional, estadual, local, presidente]) assert.equal(podeAcessarRelatoriosAdministrativos(u), true);
  assert.equal(podeEmitirRelatoriosAdministrativos(presidente), false);
  assert.equal(podeAcessarRelatoriosAdministrativos(operacional), false);

  for (const u of [admin, nacional, estadual, local, operacional]) assert.equal(podeVisualizarFrequencia(u), true);
  assert.equal(podeVisualizarFrequencia(presidente), false);
  for (const u of [admin, nacional, estadual, local, presidente, operacional]) assert.equal(podeVisualizarContratos(u), true);

  assert.equal(podeGerenciarUsuarios(presidente), false);
  assert.equal(podeGerenciarUsuarios(operacional), false);
  assert.equal(podeAcessarAuditoria(admin), true);
  for (const u of [nacional, estadual, local, presidente, operacional]) assert.equal(podeAcessarAuditoria(u), false);
  for (const u of [admin, nacional, estadual, local]) assert.equal(podeAcessarPendenciasAdministrativas(u), true);
  assert.equal(podeAcessarPendenciasAdministrativas(presidente), false);
  assert.equal(podeAcessarPendenciasAdministrativas(operacional), false);
});
