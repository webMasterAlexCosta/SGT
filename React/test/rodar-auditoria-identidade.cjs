'use strict';

// Executa os quatro oráculos originais sem alterar suas assertions. Somente o
// caminho da fonte é adaptado ao layout desta rodada, que preserva o original.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const Module = require('node:module');

const base = path.resolve(__dirname, '..');
const originalPath = path.join(base, 'auditoria-recebida/resolver_identidade_frequencia.test.cjs');
const sourcePath = path.resolve(process.argv[2] || path.join(base, 'src/services/rh/ServicoVinculoContratual.js'));
const original = fs.readFileSync(originalPath, 'utf8');
const targetLine = "const filename = path.join(base, 'frozen/final/work/react/src/services/rh/ServicoVinculoContratual.js');";
if (!original.includes(targetLine)) throw new Error('Layout inesperado do oráculo original; adaptação recusada.');
console.log('# original_test_sha256=' + crypto.createHash('sha256').update(original).digest('hex'));
console.log('# source=' + path.relative(base, sourcePath));
console.log('# Camada: resolvedor React real, transporte controlado, sem persistência.');
const adapted = original.replace(targetLine, 'const filename = ' + JSON.stringify(sourcePath) + ';');
const runner = new Module(originalPath, module);
runner.filename = originalPath;
runner.paths = Module._nodeModulePaths(path.dirname(originalPath));
runner._compile(adapted, originalPath);
