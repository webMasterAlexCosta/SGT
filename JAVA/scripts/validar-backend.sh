#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

modo=${1:-preflight}

falhar() {
  echo "ERRO: $*" >&2
  exit 1
}

command -v java >/dev/null 2>&1 || falhar "Java não encontrado no PATH. Instale/configure JDK 17 ou 21."
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')
JAVA_MAJOR=$(printf '%s' "$JAVA_VERSION" | awk -F. '{ if ($1 == "1") print $2; else print $1 }')
case "$JAVA_MAJOR" in 17|21) ;; *) falhar "Use JDK 17 ou 21. Encontrado: $JAVA_VERSION" ;; esac
export MAVEN_OPTS="${MAVEN_OPTS:--Xmx512m -XX:MaxMetaspaceSize=256m}"

[ -f pom.xml ] || falhar "pom.xml não encontrado."
[ -f mvnw ] || falhar "mvnw não encontrado."
[ -x mvnw ] || falhar "mvnw sem permissão de execução. Execute: chmod +x mvnw"

if grep "$(printf '\r')" mvnw >/dev/null 2>&1; then
  falhar "mvnw contém CRLF. O script POSIX deve usar LF."
fi

for arquivo in scripts/*.sh; do
  [ -f "$arquivo" ] || continue
  if grep "$(printf '\r')" "$arquivo" >/dev/null 2>&1; then
    falhar "$arquivo contém CRLF. Scripts POSIX devem usar LF."
  fi
done

echo "Preflight OK: Java $JAVA_VERSION, Wrapper e scripts POSIX consistentes."

case "$modo" in
  preflight)
    ;;
  validate)
    ./mvnw -B -ntp validate
    ;;
  test)
    ./mvnw -B -ntp clean test
    ;;
  package)
    ./mvnw -B -ntp clean package
    ;;
  completo)
    bash scripts/validacao/executar_regressao_mysql.sh
    ./mvnw -B -ntp -Pdb-load verify jacoco:report
    ;;
  *)
    echo "Uso: $0 [preflight|validate|test|package|completo]" >&2
    exit 2
    ;;
esac
