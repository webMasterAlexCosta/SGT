#!/usr/bin/env bash
# Preparação local; não altera backend Java. Sem sudo; use o Node.js de seu ambiente.
set -Eeuo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
if ! command -v node >/dev/null || ! command -v npm >/dev/null; then
  echo 'ERRO: Node.js e npm necessários. Requisitos: Node 26.13.0 e npm 12.2.2' >&2
  exit 2
fi
printf 'Node: '; node --version
printf 'npm:  '; npm --version
if [[ ! -f .env ]]; then
  cp .env.example .env
  chmod 600 .env
  echo 'Criado .env sanitizado. Configure antes de usar com contas reais.'
fi
npm run test:preparacao
npm run build
cat <<'FIM'
O build local não comprova integração com Java nem substitui testes ausentes.
Para gerar o pacote de dependências com Vitest e RTL:
  leia PROMPT_CODEX_FEDORA.md
  npm install --no-audit --no-fund --cache "$PWD/.npm-cache-sgt-gtrat"
  npm run dependencias:verificar
  npm run test:vitest:dependencias
  npm run dependencias:empacotar:fedora
ATENÇÃO: o Codex deve recuperar/implementar testes antes de executar npm run test.
É proibido sync:java/build:java e gravar na árvore Java congelada.
FIM
