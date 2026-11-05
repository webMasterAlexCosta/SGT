// O Java final é congelado. Não copiar arquivos para dentro da árvore do backend.
console.error('BLOQUEADO: backend Java congelado. Rode npm run build e publique o dist em servidor frontend independente.');
process.exitCode = 2;
