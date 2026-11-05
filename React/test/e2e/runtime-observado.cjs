const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const EXPECTED_MYSQL_VERSION = '8.0.45';

function runtimeObservado(environment = process.env) {
  const receiptPath = environment.SGT_RUNTIME_RECEIPT;
  const configPath = environment.SGT_RUNTIME_CONFIG;
  if (!receiptPath || !configPath || !path.isAbsolute(receiptPath) || !path.isAbsolute(configPath)) {
    throw new Error('Defina SGT_RUNTIME_RECEIPT e SGT_RUNTIME_CONFIG com caminhos absolutos da execução atual.');
  }
  let bytes, receipt, config;
  try {
    bytes = fs.readFileSync(receiptPath);
    receipt = JSON.parse(bytes);
    config = JSON.parse(fs.readFileSync(configPath));
  } catch {
    throw new Error('Recibo observado ou configuração privada ausente/inválida. Execute manage.py status após a prontidão.');
  }
  const mysql = receipt.mysql;
  const java = receipt.java;
  const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if (!uuid.test(receipt.observation_id || '') || !Number.isFinite(Date.parse(receipt.checked_utc))) {
    throw new Error('O recibo não identifica a observação e seu horário.');
  }
  if (mysql?.version !== EXPECTED_MYSQL_VERSION || mysql.version_source !== 'SELECT VERSION()' ||
      (mysql.binary_version !== null && mysql.binary_version !== EXPECTED_MYSQL_VERSION)) {
    throw new Error(`O cenário exige MySQL ${EXPECTED_MYSQL_VERSION} efetivamente observado por SELECT VERSION().`);
  }
  if (!uuid.test(mysql.server_uuid || '') || !/^[A-Za-z0-9_]{1,64}$/.test(mysql.schema || '') ||
      mysql.schema !== config.schema || !Number.isInteger(mysql.port) || mysql.port < 1 || mysql.port > 65535 ||
      !['127.0.0.1', 'localhost'].includes(mysql.host) || !mysql.current_user ||
      mysql.current_user.split('@')[0].toLowerCase() === 'root') {
    throw new Error('Identidade observada do banco incompleta ou incompatível com a configuração da aplicação.');
  }
  if (!java || !config.java || java.artifact_version !== '0.0.4-operacional-20261101' ||
      java.artifact_version !== config.java.version || !/^[a-f0-9]{64}$/.test(java.jar_sha256 || '') ||
      java.jar_sha256 !== String(config.java.sha256).toLowerCase() ||
      typeof config.java.jar !== 'string' || !path.isAbsolute(config.java.jar) || java.jar_path !== fs.realpathSync(config.java.jar) ||
      !/^17(?:\.|$)/.test(java.runtime_version || '') || java.running !== true || !Number.isInteger(java.pid) || java.pid < 1) {
    throw new Error('O recibo deve identificar o JAR candidato verificado e seu processo Java 17 em execução.');
  }
  return {
    observationId: receipt.observation_id, observedAt: receipt.checked_utc,
    receiptPath, receiptSha256: crypto.createHash('sha256').update(bytes).digest('hex'),
    mysql: { version: mysql.version, versionSource: mysql.version_source, binaryVersion: mysql.binary_version,
      serverUuid: mysql.server_uuid, schema: mysql.schema, host: mysql.host, port: mysql.port, currentUser: mysql.current_user },
    java: { version: java.runtime_version, artifactVersion: java.artifact_version, jarSha256: java.jar_sha256, pid: java.pid },
    scope: 'Identidade observada pelo supervisor; os controles HTTP e a persistência são verificados pelo cenário e pelos snapshots.'
  };
}

module.exports = { runtimeObservado };
