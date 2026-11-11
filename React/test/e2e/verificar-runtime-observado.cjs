// Synthetic receipt validation only; no browser, JVM, PostgreSQL or Redis acceptance.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const crypto = require('node:crypto');
const { runtimeObservado } = require('./runtime-observado.cjs');
function syntheticFiles(t, mutate = () => {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'sgt-receipt-unit-'));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  const jar = path.join(root, 'candidate.jar');
  fs.writeFileSync(jar, 'synthetic placeholder, never executable');
  const config = { database: { name:'sgt_20261109_synthetic',host:'127.0.0.1',port:13317,systemIdentifier:'7706058335443300188' },
    java: { jar,sha256:crypto.createHash('sha256').update(fs.readFileSync(jar)).digest('hex'),version:'0.0.4-operacional-20261101' } };
  const receipt = {
    state:'READY_OWN_REAL_RUNTIME',observation_id:'11111111-2222-4333-8444-555555555555',checked_utc:new Date().toISOString(),
    database:{version_num:180006,version_source:"current_setting('server_version_num')",database:config.database.name,
      host:'127.0.0.1',port:13317,current_user:'sgt_20261109_app',system_identifier:config.database.systemIdentifier,
      role_limited:true,no_memberships:true,database_owned_by_role:true,session_identity:true,all_relations_owned:true},
    java:{artifact_version:config.java.version,jar_sha256:config.java.sha256,jar_path:jar,
      runtime_version:'openjdk version "25.0.4.1" 2026-08-18',pid:1234,running:true}
  };
  mutate(receipt,config);
  const environment={SGT_RUNTIME_RECEIPT:path.join(root,'receipt.json'),SGT_RUNTIME_CONFIG:path.join(root,'config.json')};
  fs.writeFileSync(environment.SGT_RUNTIME_RECEIPT,JSON.stringify(receipt));
  fs.writeFileSync(environment.SGT_RUNTIME_CONFIG,JSON.stringify(config));
  return environment;
}
test('refuses absent observed receipt before scenario execution',()=>{
  assert.throws(()=>runtimeObservado({}),/SGT_RUNTIME_RECEIPT/);
});
test('refuses versions other than exactly observed PostgreSQL18.6',t=>{
  for(const version of [180005,180007,180000,170006,'180006',undefined])
    assert.throws(()=>runtimeObservado(syntheticFiles(t,r=>{r.database.version_num=version;})),/18\.6/);
});
test('refuses an unobserved version label',t=>{
  assert.throws(()=>runtimeObservado(syntheticFiles(t,r=>{r.database.version_source='declared configuration';})),/current_setting/);
});
test('refuses MySQL historical receipts instead of relabeling them',t=>{
  assert.throws(()=>runtimeObservado(syntheticFiles(t,r=>{r.mysql={version:'8.0.45'};delete r.database;})),/18\.6/);
});
test('refuses wrong database/cluster/privileges/ownership and nonlocal destinations',t=>{
  const mutations=[r=>{r.database.database='shared';},r=>{r.database.system_identifier='7706058335443300199';},
    r=>{r.database.host='192.0.2.1';},r=>{r.database.port=13318;},r=>{r.database.current_user='postgres';},
    ...['role_limited','no_memberships','database_owned_by_role','session_identity','all_relations_owned'].map(key=>r=>{r.database[key]=false;})];
  for(const mutate of mutations)assert.throws(()=>runtimeObservado(syntheticFiles(t,mutate)),/Identidade observada/);
});
test('refuses wrong candidate/hash/bytes and stopped or nonJava25 observations',t=>{
  assert.throws(()=>runtimeObservado(syntheticFiles(t,r=>{r.java.jar_sha256='b'.repeat(64);})),/JAR candidato/);
  assert.throws(()=>runtimeObservado(syntheticFiles(t,(_r,c)=>{fs.appendFileSync(c.java.jar,' changed');})),/JAR candidato/);
  assert.throws(()=>runtimeObservado(syntheticFiles(t,r=>{r.java.running=false;})),/processo Java/);
  assert.throws(()=>runtimeObservado(syntheticFiles(t,r=>{r.state='STOPPED_OWN_RUNTIME';})),/processo Java/);
  for(const version of ['17.0.18','21.0.9','26.0.1','openjdk version "17.0.18"'])
    assert.throws(()=>runtimeObservado(syntheticFiles(t,r=>{r.java.runtime_version=version;})),/processo Java/);
});
test('preserves observation identity and digest without claiming a live test',t=>{
  const result=runtimeObservado(syntheticFiles(t));
  assert.equal(result.postgresql.versionNum,180006);
  assert.equal(result.postgresql.versionSource,"current_setting('server_version_num')");
  assert.equal(result.postgresql.systemIdentifier,'7706058335443300188');
  assert.equal(result.postgresql.database,'sgt_20261109_synthetic');
  assert.equal(result.postgresql.port,13317);
  assert.match(result.receiptSha256,/^[a-f0-9]{64}$/);
  assert.equal(result.mysql,undefined);assert.equal(result.passed,undefined);
});
