const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const EXPECTED_POSTGRES_VERSION = 180006;
function fileSha256(file) {
  const fd=fs.openSync(file,'r'),buffer=Buffer.alloc(1024*1024),hash=crypto.createHash('sha256');
  try { let size;while((size=fs.readSync(fd,buffer,0,buffer.length,null))>0)hash.update(buffer.subarray(0,size)); }
  finally { fs.closeSync(fd); }
  return hash.digest('hex');
}
function runtimeObservado(environment = process.env) {
  const receiptPath=environment.SGT_RUNTIME_RECEIPT,configPath=environment.SGT_RUNTIME_CONFIG;
  if(!receiptPath||!configPath||!path.isAbsolute(receiptPath)||!path.isAbsolute(configPath))
    throw new Error('Defina SGT_RUNTIME_RECEIPT e SGT_RUNTIME_CONFIG com caminhos absolutos da execução atual.');
  let bytes,receipt,config;
  try { bytes=fs.readFileSync(receiptPath);receipt=JSON.parse(bytes);config=JSON.parse(fs.readFileSync(configPath)); }
  catch { throw new Error('Recibo observado ou configuração sem credenciais ausente/inválida. Observe o supervisor atual após prontidão.'); }
  const db=receipt.database,java=receipt.java;
  const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if(!uuid.test(receipt.observation_id||'')||!Number.isFinite(Date.parse(receipt.checked_utc)))
    throw new Error('O recibo não identifica a observação e seu horário.');
  if(db?.version_num!==EXPECTED_POSTGRES_VERSION||db.version_source!=="current_setting('server_version_num')")
    throw new Error("O cenário exige PostgreSQL18.6 efetivamente observado por current_setting('server_version_num').");
  if(!config.database||!/^sgt_20261109_[A-Za-z0-9_]+$/.test(db.database||'')||db.database!==config.database.name
    ||!/^\d{19,20}$/.test(db.system_identifier||'')||db.system_identifier!==config.database.systemIdentifier
    ||!Number.isInteger(db.port)||db.port<1||db.port>65535||db.port!==config.database.port
    ||!['127.0.0.1','localhost'].includes(db.host)||db.host!==config.database.host
    ||!/^sgt_20261109_[A-Za-z0-9_]+$/.test(db.current_user||'')
    ||!['role_limited','no_memberships','database_owned_by_role','session_identity','all_relations_owned'].every(key=>db[key]===true))
    throw new Error('Identidade observada do banco incompleta, sem isolamento/ownership ou incompatível com a configuração.');
  if(!java||!config.java||java.artifact_version!=='0.0.4-operacional-20261101'||java.artifact_version!==config.java.version
    ||!/^[a-f0-9]{64}$/.test(java.jar_sha256||'')||java.jar_sha256!==String(config.java.sha256).toLowerCase()
    ||typeof config.java.jar!=='string'||!path.isAbsolute(config.java.jar))
    throw new Error('O recibo deve identificar o JAR candidato verificado.');
  try {
    if(java.jar_path!==fs.realpathSync(config.java.jar)||fileSha256(config.java.jar)!==java.jar_sha256)throw new Error();
  } catch { throw new Error('Os bytes do JAR candidato não correspondem ao recibo observado.'); }
  if(!/^(?:openjdk version ")?25(?:\.|$)/.test(java.runtime_version||'')||java.running!==true
    ||receipt.state!=='READY_OWN_REAL_RUNTIME'||!Number.isInteger(java.pid)||java.pid<1)
    throw new Error('O recibo deve identificar o processo Java25 em execução; prontidão não é execução de aceite.');
  return {
    observationId:receipt.observation_id,observedAt:receipt.checked_utc,receiptPath,
    receiptSha256:crypto.createHash('sha256').update(bytes).digest('hex'),
    postgresql:{versionNum:db.version_num,versionSource:db.version_source,systemIdentifier:db.system_identifier,
      database:db.database,host:db.host,port:db.port,currentUser:db.current_user,
      roleLimited:db.role_limited,databaseOwnedByRole:db.database_owned_by_role,allRelationsOwned:db.all_relations_owned},
    java:{version:java.runtime_version,artifactVersion:java.artifact_version,jarSha256:java.jar_sha256,pid:java.pid},
    scope:'Recibo do supervisor e hash atual do JAR. O runner verifica PIDs/cgroup; HTTP e persistência exigem o cenário e snapshots reais.'
  };
}
module.exports={runtimeObservado};
