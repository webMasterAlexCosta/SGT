#!/usr/bin/env bash
# Regressão completa e serial: unitários + integrações MySQL + JAR + JaCoCo.
# A preparação de infraestrutura e a escolha das credenciais ficam no ambiente.
set -euo pipefail
sgt_project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
cd "$sgt_project_root"
: "${INTEGRATION_MYSQL_URL:?Defina INTEGRATION_MYSQL_URL para um schema sintético MySQL local explicitamente autorizado.}"
if [[ ! "$INTEGRATION_MYSQL_URL" =~ ^jdbc:mysql://(127\.0\.0\.1|localhost):[0-9]+/(sgt_impl_[A-Za-z0-9_]+|sgt_audit_[A-Za-z0-9_]+|sgt_correcao_exec|sgt8045_(baseline|candidata|upgrade|http|409_red|regressao))(\?.*)?$ ]]; then
  printf '%s\n' 'A regressão exige um schema sintético local autorizado; sgt8045 aceita somente baseline, candidata, upgrade, http, 409_red ou regressao.' >&2
  exit 2
fi
export MAVEN_OPTS="${MAVEN_OPTS:--Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m}"
sgt_maven_executable=${SGT_MAVEN_EXECUTABLE:-./mvnw}
if command -v flock >/dev/null 2>&1; then
  sgt_lock_id=$(printf '%s' "$sgt_project_root" | sha256sum | cut -c1-20)
  exec 9>"${TMPDIR:-/tmp}/sgt-regressao-${sgt_lock_id}.lock"
  flock 9
fi
"$sgt_maven_executable" -B -ntp -Pintegration -Dmaven.compiler.useIncrementalCompilation=false \
  -DskipTests=false -DskipITs=false -Dspring.datasource.hikari.maximum-pool-size=8 \
  clean verify org.jacoco:jacoco-maven-plugin:0.8.15:report
