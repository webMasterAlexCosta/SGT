import { cp, mkdir, readFile, readdir, realpath } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const frontend = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const raiz = resolve(frontend, '..');
const origem = join(frontend, 'dist');
const destino = join(raiz, 'JAVA', 'src', 'main', 'resources', 'static');
const executar = process.argv.includes('--execute');
const dryRun = process.argv.includes('--dry-run');
if (!executar && !dryRun) throw new Error('Informe --dry-run para conferir ou --execute para sincronizar o build aprovado.');
if (executar && dryRun) throw new Error('Escolha somente um modo de sincronização.');
if (await realpath(frontend) !== frontend || await realpath(join(raiz, 'JAVA')) !== join(raiz, 'JAVA')) {
  throw new Error('A sincronização exige os diretórios locais React e JAVA sem redirecionamento por symlink.');
}
const index = await readFile(join(origem, 'index.html'), 'utf8');
if (!index.includes('/assets/')) throw new Error('dist/index.html não contém assets compilados. Execute npm run build.');
const arquivos = await readdir(origem);
if (!arquivos.includes('assets')) throw new Error('dist/assets ausente.');
console.log(JSON.stringify({ modo: executar ? 'execute' : 'dry-run', origem, destino, arquivos }));
if (executar) {
  await mkdir(destino, { recursive: true });
  // Copia somente o build validado; nunca apaga outros recursos Java.
  for (const arquivo of arquivos) await cp(join(origem, arquivo), join(destino, arquivo), { recursive: true, force: true });
}
