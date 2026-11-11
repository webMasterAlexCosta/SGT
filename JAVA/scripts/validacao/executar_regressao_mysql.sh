#!/usr/bin/env bash
# Compatibilidade de caminho; o projeto usa exclusivamente PostgreSQL 18.6.
set -euo pipefail
printf '%s\n' 'Caminho legado: use executar_regressao_postgres.sh com o laboratório PostgreSQL isolado autorizado.' >&2
exec bash "$(dirname -- "${BASH_SOURCE[0]}")/executar_regressao_postgres.sh" "$@"
