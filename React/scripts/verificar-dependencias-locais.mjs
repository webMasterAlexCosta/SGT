import { readFileSync, existsSync } from 'node:fs';
import path from 'node:path';
const pkg=JSON.parse(readFileSync(new URL('../package.json',import.meta.url),'utf8'));
const base=new URL('../node_modules/',import.meta.url);
let faltar=0;
for (const [categoria, deps] of [['runtime',pkg.dependencies],['development',pkg.devDependencies]]) {
 for (const [nome, versao] of Object.entries(deps)) {
  const p=new URL(`${nome}/package.json`,base);
  let verAtual=null;
  try {verAtual=JSON.parse(readFileSync(p,'utf8')).version;}catch{}
  console.log(`${verAtual===versao?'OK':'PENDENTE'} ${categoria} ${nome}: esperado=${versao} disponível=${verAtual??'ausente'}`);
  if (verAtual!==versao)faltar++;
 }
}
if (faltar) {console.log(`Dependências não atendidas: ${faltar}. Completar no Fedora conforme PROMPT_CODEX_FEDORA.md`);process.exitCode=1}
