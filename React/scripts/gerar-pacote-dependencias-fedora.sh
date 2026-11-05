#!/usr/bin/env bash
# Gera SOMENTE dependências para o usuário anexar ao ChatGPT e armazenar na Biblioteca.
# Requer npm install prévio com cache isolado; não altera Java nem gera ZIP com fontes.
set -Eeuo pipefail
IFS=$'\n\t'
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$ROOT"
for command in node npm python3 zip unzip sha256sum; do
  command -v "$command" >/dev/null || { echo "ERRO: comando obrigatório ausente: $command" >&2; exit 2; }
done
node -e "if (process.platform !== 'linux' || process.arch !== 'x64' || parseInt(process.versions.node,10)!==22) { console.error('Necessário Linux x86_64 + Node 22 para este pacote.'); process.exit(2) }"
[[ -f package-lock.json ]] || { echo 'ERRO: não existe package-lock.json. Rode npm install no Fedora antes.' >&2; exit 2; }
[[ -d node_modules ]] || { echo 'ERRO: node_modules ausente.' >&2; exit 2; }
CACHE="${SGT_NPM_CACHE:-$ROOT/.npm-cache-sgt-gtrat}"
[[ -d "$CACHE/_cacache" ]] || { echo "ERRO: cache npm isolado não encontrado em: $CACHE" >&2; exit 2; }
OUT="${SGT_DEPENDENCIAS_OUT:-$ROOT/artefatos_codex}"
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd -P)"
[[ "$OUT" != "$ROOT" ]] || { echo 'ERRO: o diretório de saída não pode ser o projeto.' >&2; exit 2; }
ARQUIVO=SGT_GTRAT_DEPENDENCIAS_COMPLETAS_FEDORA_NODE22.zip
BASE=SGT_GTRAT_DEPENDENCIAS_COMPLETAS_FEDORA_NODE22
TEMP="$(mktemp -d "$OUT/.geracao-dependencias-XXXXXX")"
trap 'rm -rf -- "$TEMP"' EXIT
mkdir -p "$TEMP/logs" "$TEMP/offline-ci" "$TEMP/stage/$BASE/npm-cache"

# Conferir a identidade da instalação e do lockfile antes de gastar CPU em empacotamento.
node --version | tee "$TEMP/logs/node.txt"
npm --version | tee "$TEMP/logs/npm.txt"
node --input-type=module - <<'NODE'
import { readFileSync } from 'node:fs';
const pkg = JSON.parse(readFileSync('package.json','utf8'));
const lock = JSON.parse(readFileSync('package-lock.json','utf8'));
if (!lock.lockfileVersion || !lock.packages || !lock.packages['']) throw Error('Lockfile npm inválido ou incompleto');
for (const kind of ['dependencies','devDependencies']) {
  for (const [name, expected] of Object.entries(pkg[kind]??{})) {
    const installed = JSON.parse(readFileSync(`node_modules/${name}/package.json`,'utf8')).version;
    const locked = lock.packages[`node_modules/${name}`]?.version;
    const root = lock.packages[''][kind]?.[name];
    if (installed!==expected || locked!==expected || root!==expected) {
      throw Error(`${kind} ${name} incorreto: pkg=${expected}, installed=${installed}, lock=${locked}, root=${root}`);
    }
  }
}
for (const [loc,p] of Object.entries(lock.packages)) {
  if (p.resolved && /^https?:\/\//.test(p.resolved)) {
    const u=new URL(p.resolved);
    if(u.username || u.password || u.hostname!=='registry.npmjs.org') {
      throw Error(`Origem privada/não permitida no lockfile em ${loc}: ${u.hostname}`);
    }
  }
}
console.log('LOCKFILE, PINS E ORIGENS PUBLICAS: OK');
NODE

npm run dependencias:verificar 2>&1 | tee "$TEMP/logs/01-verificacao-dependencias.log"
npm run test:preparacao 2>&1 | tee "$TEMP/logs/02-preparacao-local.log"
npm run test:vitest:dependencias 2>&1 | tee "$TEMP/logs/03-vitest-rtl-jsdom.log"
npm run build 2>&1 | tee "$TEMP/logs/04-build-vite.log"

# Offline de verdade, separado do projeto original; npm ci não pode apagar seu node_modules.
cp package.json package-lock.json "$TEMP/offline-ci/"
(
 cd "$TEMP/offline-ci"
 npm ci --offline --no-audit --no-fund --cache "$CACHE"
) 2>&1 | tee "$TEMP/logs/05-npm-ci-offline.log"
printf 'npm ci --offline: APROVADO (instalacao isolada)\n' | tee "$TEMP/logs/06-resumo.txt"

PACK="$TEMP/stage/$BASE"
cp package.json package-lock.json "$PACK/"
cp -a node_modules "$PACK/node_modules"
cp -a "$CACHE/_cacache" "$PACK/npm-cache/_cacache"
mkdir -p "$PACK/comprovacao"
cp "$TEMP/logs/"* "$PACK/comprovacao/"
cat > "$PACK/README_RESTAURACAO.md" <<'TXT'
# SGT/GTRAT — dependências completas Linux/Node 22

Este ZIP foi gerado no Fedora com dependências instaladas e verificadas, inclusive Vitest, React Testing Library, jsdom, coverage-v8 e plugin-basic-ssl. É **somente dependências**, não contém fontes React nem Java.

**Requisitos:** Linux x86_64, Node.js 22.x e npm compatível com o pacote.

## Restaurar no projeto React recebido

1. Extraia este ZIP e localize `SGT_GTRAT_DEPENDENCIAS_COMPLETAS_FEDORA_NODE22/`.
2. Execute `bash restaurar-no-fedora.sh /caminho/SGT_GTRAT_REACT_FEDORA/frontend`.
3. O instalador compara o `package.json` da pasta alvo antes de copiar; não sobrescreve projeto incompatível.
4. O script copia `package-lock.json`, `node_modules` e cache isolado. Execute `npm run dependencias:verificar` e `npm run test:vitest:dependencias` no projeto React.
5. Para prova de restauração offline em pasta de teste separada, use `npm ci --offline --cache "$PWD/.npm-cache-sgt-gtrat"`. Não rode `npm ci` no projeto principal sem checkpoint: ele recria `node_modules`.

A pasta `comprovacao/` registra as execuções no Fedora. `MANIFESTO_SHA256.txt` permite verificar cada arquivo regular; symlinks são descritos com seus destinos. Não há `.env` nem `.npmrc` neste pacote.
TXT
cat > "$PACK/restaurar-no-fedora.sh" <<'TXT'
#!/usr/bin/env bash
set -Eeuo pipefail
BASE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
DEST="${1:-}"
[[ -n "$DEST" && -f "$DEST/package.json" ]] || { echo 'Uso: bash restaurar-no-fedora.sh /caminho/do/frontend' >&2; exit 2; }
DEST="$(cd "$DEST" && pwd -P)"
if ! cmp -s "$BASE/package.json" "$DEST/package.json"; then
  echo 'ERRO: package.json do destino difere. Atualize o React para o pacote do ChatGPT antes de restaurar.' >&2
  exit 2
fi
mkdir -p "$DEST/node_modules" "$DEST/.npm-cache-sgt-gtrat"
cp -a "$BASE/node_modules/." "$DEST/node_modules/"
cp -a "$BASE/npm-cache/_cacache" "$DEST/.npm-cache-sgt-gtrat/"
cp "$BASE/package-lock.json" "$DEST/package-lock.json"
echo 'Dependências copiadas. Execute npm run dependencias:verificar && npm run test:vitest:dependencias.'
TXT
chmod +x "$PACK/restaurar-no-fedora.sh"

# NENHUM arquivo pessoal ou ambiente de execução; só os diretórios explícitos acima.
if find "$PACK" \( -name '.env' -o -name '.npmrc' -o -name '.git' \) -print | grep -q .; then
  echo 'ERRO: arquivo pessoal de ambiente encontrado no estágio; abortado.' >&2; exit 3
fi
python3 - "$PACK" <<'PY'
from pathlib import Path
from hashlib import sha256
from datetime import datetime, timezone
import json,os,sys,subprocess
p=Path(sys.argv[1]); result=[]
for f in sorted(p.rglob('*')):
    if f.name=='MANIFESTO_SHA256.txt' or f.is_dir():continue
    rel=f.relative_to(p).as_posix()
    if f.is_symlink():
        dest=os.readlink(f)
        if os.path.isabs(dest): raise RuntimeError(f'Link absoluto não portável: {rel} -> {dest}')
        result.append(f'SYMLINK  {dest}  {rel}')
        continue
    h=sha256()
    with open(f,'rb') as fd:
        for buf in iter(lambda:fd.read(1<<20),b''):h.update(buf)
    result.append(f'{h.hexdigest()}  {f.stat().st_size}  {rel}')
(p/'MANIFESTO_SHA256.txt').write_text('# SHA256 / bytes / arquivo (symlinks: destino relativo)\n'+'\n'.join(result)+'\n')
pkg=json.loads((p/'package.json').read_text())
lock=json.loads((p/'package-lock.json').read_text())
info={
 'gerado_em_utc':datetime.now(timezone.utc).isoformat(),
 'sistema':'Fedora Linux x86_64',
 'node':subprocess.check_output(['node','--version'],text=True).strip(),
 'npm':subprocess.check_output(['npm','--version'],text=True).strip(),
 'package_json_sha256':sha256((p/'package.json').read_bytes()).hexdigest(),
 'package_lock_json_sha256':sha256((p/'package-lock.json').read_bytes()).hexdigest(),
 'dependencias':pkg.get('dependencies',{}),
 'dependencias_desenvolvimento':pkg.get('devDependencies',{}),
 'lockfile_version':lock['lockfileVersion'],
 'npm_ci_offline':'aprovado',
 'vitest_react_testing_library_jsdom':'aprovado',
 'vite_build':'aprovado',
 'arquivos_manifestados':len(result),
 'destino_chatgpt_biblioteca':'/Dependencias/Node-e-React/SGT_GTRAT/',
}
(p/'INVENTARIO.json').write_text(json.dumps(info,indent=2,ensure_ascii=False)+'\n')
print('Arquivos no manifesto:',len(result))
PY
# Refazer manifesto incluindo o inventário gerado depois do primeiro manifesto.
python3 - "$PACK" <<'PY'
from pathlib import Path
from hashlib import sha256
import os,sys
p=Path(sys.argv[1]); lines=['# SHA256 / bytes / arquivo (symlinks: destino relativo)']
for f in sorted(p.rglob('*')):
    if f.name=='MANIFESTO_SHA256.txt' or f.is_dir():continue
    rel=f.relative_to(p).as_posix()
    if f.is_symlink():
        lines.append(f'SYMLINK  {os.readlink(f)}  {rel}')
        continue
    h=sha256()
    with f.open('rb') as fd:
        for b in iter(lambda:fd.read(1<<20),b''):h.update(b)
    lines.append(f'{h.hexdigest()}  {f.stat().st_size}  {rel}')
(p/'MANIFESTO_SHA256.txt').write_text('\n'.join(lines)+'\n')
PY
(
 cd "$TEMP/stage"
 zip -q -r -y -X "$OUT/$ARQUIVO" "$BASE"
)
unzip -tqq "$OUT/$ARQUIVO"
echo 'ZIP DE DEPENDÊNCIAS GERADO:'
ls -lh "$OUT/$ARQUIVO"
sha256sum "$OUT/$ARQUIVO"
echo "Anexe o ZIP ao ChatGPT e peça para salvá-lo em /Dependencias/Node-e-React/SGT_GTRAT/."
