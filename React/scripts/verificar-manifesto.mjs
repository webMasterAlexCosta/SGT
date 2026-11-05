#!/usr/bin/env node
// Verificação somente leitura. Aceita SHA256  CAMINHO e SHA256  BYTES  CAMINHO.
import { createHash } from 'node:crypto';
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, isAbsolute, relative, resolve, sep } from 'node:path';

const manifesto = resolve(process.argv[2] || 'MANIFESTO_SHA256_FINAL.txt');
const raiz = resolve(process.argv[3] || dirname(manifesto));
const declarados = new Set();
const ausentes = [], divergentes = [], invalidos = [];
let conferidos = 0;
for (const linha of readFileSync(manifesto, 'utf8').split(/\r?\n/)) {
  if (!linha.trim() || linha.startsWith('#') || linha.startsWith('SHA256')) continue;
  const item = linha.match(/^([0-9a-f]{64})\s+(?:(\d+)\s+)?(.+)$/i);
  if (!item) { invalidos.push(linha); continue; }
  const [, esperado, bytes, nome] = item;
  const caminho = resolve(raiz, nome);
  if (isAbsolute(nome) || caminho === raiz || !caminho.startsWith(raiz + sep) || declarados.has(nome)) {
    invalidos.push(nome); continue;
  }
  declarados.add(nome);
  let dados;
  try { dados = readFileSync(caminho); } catch { ausentes.push(nome); continue; }
  const hash = createHash('sha256').update(dados).digest('hex');
  if (hash !== esperado.toLowerCase() || (bytes && Number(bytes) !== dados.length)) divergentes.push(nome);
  else conferidos += 1;
}
const arquivos = [];
const listar = diretorio => {
  for (const entrada of readdirSync(diretorio, { withFileTypes: true })) {
    const caminho = resolve(diretorio, entrada.name);
    if (entrada.isDirectory()) listar(caminho);
    else if (caminho !== manifesto && statSync(caminho).isFile()) arquivos.push(relative(raiz, caminho).split(sep).join('/'));
  }
};
listar(raiz);
const naoDeclarados = arquivos.filter(nome => !declarados.has(nome));
const resultado = {
  manifesto, raiz, entradas: declarados.size, conferidos,
  ausentes: ausentes.length, divergentes: divergentes.length,
  naoDeclarados: naoDeclarados.length, invalidos: invalidos.length,
  exemplos: { ausentes: ausentes.slice(0, 10), divergentes: divergentes.slice(0, 10), naoDeclarados: naoDeclarados.slice(0, 10), invalidos: invalidos.slice(0, 10) },
  passed: !ausentes.length && !divergentes.length && !naoDeclarados.length && !invalidos.length,
};
console.log(JSON.stringify(resultado, null, 2));
process.exitCode = resultado.passed ? 0 : 1;
