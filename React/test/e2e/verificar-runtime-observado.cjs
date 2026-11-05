// Preparation checks with synthetic JSON only; no browser, JVM, MySQL or Redis.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { runtimeObservado } = require('./runtime-observado.cjs');

function syntheticFiles(t, mutate = () => {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'sgt-receipt-unit-'));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  const jar = path.join(root, 'candidate.jar');
  fs.writeFileSync(jar, 'synthetic placeholder, never executable');
  const config = { schema: 'sgt_synthetic', java: { jar, sha256: 'a'.repeat(64), version: '0.0.4-operacional-20261101' } };
  const receipt = {
    observation_id: '11111111-2222-4333-8444-555555555555', checked_utc: '2026-11-02T00:00:00Z',
    mysql: { version: '8.0.45', version_source: 'SELECT VERSION()', binary_version: '8.0.45',
      server_uuid: '11111111-2222-4333-8444-666666666666', schema: config.schema,
      host: '127.0.0.1', port: 13317, current_user: 'sgt_app@127.0.0.1' },
    java: { artifact_version: config.java.version, jar_sha256: config.java.sha256, jar_path: jar,
      runtime_version: '17.0.18', pid: 1234, running: true }
  };
  mutate(receipt, config);
  const environment = { SGT_RUNTIME_RECEIPT: path.join(root, 'receipt.json'), SGT_RUNTIME_CONFIG: path.join(root, 'config.json') };
  fs.writeFileSync(environment.SGT_RUNTIME_RECEIPT, JSON.stringify(receipt));
  fs.writeFileSync(environment.SGT_RUNTIME_CONFIG, JSON.stringify(config));
  return environment;
}

test('refuses absent observed receipt before scenario execution', () => {
  assert.throws(() => runtimeObservado({}), /SGT_RUNTIME_RECEIPT/);
});
test('refuses 8.4 and suffixes instead of exactly observed 8.0.45', t => {
  for (const version of ['8.0.44', '8.0.46', '8.4.11', '8.0.45-other', undefined]) {
    const env = syntheticFiles(t, receipt => { receipt.mysql.version = version; });
    assert.throws(() => runtimeObservado(env), /8\.0\.45/);
  }
});
test('refuses an unobserved version label', t => {
  const env = syntheticFiles(t, receipt => { receipt.mysql.version_source = 'declared configuration'; });
  assert.throws(() => runtimeObservado(env), /SELECT VERSION/);
});
test('refuses a coherent old 8.0.46 receipt rather than accepting the prior campaign', t => {
  const env = syntheticFiles(t, receipt => { receipt.mysql.version = '8.0.46'; receipt.mysql.binary_version = '8.0.46'; });
  assert.throws(() => runtimeObservado(env), /8\.0\.45/);
});
test('refuses a mismatching or suffixed local binary even when SELECT reports 8.0.45', t => {
  for (const version of ['8.0.46', '8.0.45-other']) {
    const env = syntheticFiles(t, receipt => { receipt.mysql.binary_version = version; });
    assert.throws(() => runtimeObservado(env), /8\.0\.45/);
  }
});
test('refuses a different candidate hash or a stopped Java process', t => {
  assert.throws(() => runtimeObservado(syntheticFiles(t, receipt => { receipt.java.jar_sha256 = 'b'.repeat(64); })), /JAR candidato/);
  assert.throws(() => runtimeObservado(syntheticFiles(t, receipt => { receipt.java.running = false; })), /processo Java/);
});
test('preserves observation identity and receipt digest without claiming a live test', t => {
  const result = runtimeObservado(syntheticFiles(t));
  assert.equal(result.mysql.version, '8.0.45');
  assert.equal(result.mysql.versionSource, 'SELECT VERSION()');
  assert.equal(result.mysql.serverUuid, '11111111-2222-4333-8444-666666666666');
  assert.equal(result.mysql.schema, 'sgt_synthetic');
  assert.equal(result.mysql.port, 13317);
  assert.match(result.receiptSha256, /^[a-f0-9]{64}$/);
  assert.equal(result.passed, undefined);
});
