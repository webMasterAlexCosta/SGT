#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

modo=${1:-preflight}

falhar() {
  echo "ERRO: $*" >&2
  exit 1
}

command -v java >/dev/null 2>&1 || falhar "Java não encontrado no PATH. Instale/configure JDK 17."
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')
JAVA_MAJOR=$(printf '%s' "$JAVA_VERSION" | awk -F. '{ if ($1 == "1") print $2; else print $1 }')
[ "$JAVA_MAJOR" = "17" ] || falhar "JDK 17 é obrigatório. Encontrado: $JAVA_VERSION"

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
    ./mvnw -B -ntp clean test
    ./mvnw -B -ntp -DskipTests package
    ;;
  *)
    echo "Uso: $0 [preflight|validate|test|package|completo]" >&2
    exit 2
    ;;
esac
