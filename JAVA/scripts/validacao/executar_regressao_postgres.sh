#!/usr/bin/env bash
# Regressão serial PostgreSQL 18.6 nativo. O agente deve usar checkpoint,
# fila heavy-run/cgroup e lotes menores conforme AGENTS; este comando é integral.
# Infraestrutura/credenciais explícitas; JPA verifica versão, role e ownership.
set -euo pipefail
sgt_project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
cd "$sgt_project_root"
: "${INTEGRATION_POSTGRES_URL:?Defina INTEGRATION_POSTGRES_URL para um banco sintético local isolado e autorizado.}"
: "${INTEGRATION_POSTGRES_USER:?Defina a role limitada do laboratório autorizado.}"
: "${INTEGRATION_POSTGRES_PASSWORD:?Defina a senha privada da role limitada.}"
if [[ ! "$INTEGRATION_POSTGRES_URL" =~ ^jdbc:postgresql://(127\.0\.0\.1|localhost):[0-9]+/sgt_[A-Za-z0-9_]+(\?.*)?$ ]]; then
  printf '%s\n' 'A regressão exige banco sintético sgt_* no PostgreSQL local isolado explicitamente autorizado.' >&2
  exit 2
fi
export INTEGRATION_POSTGRES_MODE=${INTEGRATION_POSTGRES_MODE:-native}
if [[ "$INTEGRATION_POSTGRES_MODE" != native ]]; then
  printf '%s\n' 'Use INTEGRATION_POSTGRES_MODE=native; containers automáticos não são autorizados.' >&2
  exit 2
fi
export MAVEN_OPTS="${MAVEN_OPTS:--Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m}"
sgt_maven_executable=${SGT_MAVEN_EXECUTABLE:-./mvnw}
if command -v flock >/dev/null 2>&1; then
  sgt_lock_id=$(printf '%s' "$sgt_project_root" | sha256sum | cut -c1-20)
  exec 9>"${TMPDIR:-/tmp}/sgt-regressao-${sgt_lock_id}.lock"
  flock 9
fi
bash "$sgt_maven_executable" -B -ntp -T1 -Pintegration -Dmaven.compiler.useIncrementalCompilation=false \
  -DskipTests=false -DskipITs=false -Dspring.datasource.hikari.maximum-pool-size=8 \
  clean verify org.jacoco:jacoco-maven-plugin:0.8.15:report
