import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { criarConfiguracaoFrontend } from '../src/core/configuracaoFrontend.ts';

const raiz = dirname(dirname(fileURLToPath(import.meta.url)));
const ambiente = Object.fromEntries(readFileSync(join(raiz,'.env.example'),'utf8').split(/\r?\n/)
  .map(l=>l.trim()).filter(l=>l && !l.startsWith('#') && l.includes('='))
  .map(l=>{const p=l.indexOf('=');return [l.slice(0,p),l.slice(p+1)];}));

test('configuração sanitizada passa nas validações e utiliza cinco registros por página',()=>{
  const config=criarConfiguracaoFrontend(ambiente, { producao:true,versao:'1.4.1D' });
  assert.equal(config.PAGINATION_DEFAULT_SIZE,5);
  assert.equal(config.SHOW_TEST_CREDENTIALS,false);
  assert.equal(config.TEST_GLOBAL_PASSWORD,'');
  assert.equal(config.TEST_GESTOR_PASSWORD,'');
  assert.equal(config.API_URL,'/api');
});

test('rejeita paginação incompatível sem relaxar invariantes',()=>{
  assert.throws(()=>criarConfiguracaoFrontend({...ambiente,VITE_PAGINATION_DEFAULT_SIZE:'200'}, {producao:true}),/exceder/);
});

test('pipeline autorizado verifica tipos, build e sincronização Java explícita',()=>{
  const pkg=JSON.parse(readFileSync(join(raiz,'package.json'),'utf8'));
  assert.match(pkg.scripts['typecheck'],/tsc --noEmit/);
  assert.match(pkg.scripts.build,/typecheck.*vite build/);
  assert.match(pkg.scripts['sync:java'],/sincronizar-java.*--execute/);
  assert.match(pkg.scripts['sync:java:check'],/sincronizar-java.*--dry-run/);
  const sync = readFileSync(join(raiz, 'scripts/sincronizar-java.mjs'), 'utf8');
  assert.match(sync, /if \(executar\)/);
  assert.doesNotMatch(sync, /rmSync|rm\(/);
});

test('não há importações de react-icons nos componentes corrigidos',()=>{
  const arquivos=[
    'src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.tsx',
    'src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/CabecalhoRelatorios/index.tsx',
    'src/modules/painel/components/IndicadorEfetivo/index.tsx',
    'src/modules/painel/components/GestaoExecutiva/index.tsx',
  ];
  for (const f of arquivos) {
    assert.ok(existsSync(join(raiz,f)));
    assert.doesNotMatch(readFileSync(join(raiz,f),'utf8'),/react-icons\/(?:fa|bi)/);
  }
});


// IND-10: o pin Vite 8.2.2 do teste recebido está preservado em auditoria-recebida.
// A atualização autorizada mantém as mesmas verificações de versão declarada e instalada.
test('pins do React e Vite refletem os módulos locais efetivamente recebidos', () => {
  const pkg = JSON.parse(readFileSync(join(raiz, 'package.json'), 'utf8'));
  for (const [nome, versao] of Object.entries({
    react: '19.2.5', 'react-dom': '19.2.5', vite: '8.2.4',
    '@vitejs/plugin-react': '6.1.1', 'lucide-react': '1.11.0',
  })) {
    const declarada = pkg.dependencies[nome] ?? pkg.devDependencies[nome];
    const instalada = JSON.parse(readFileSync(join(raiz, 'node_modules', nome, 'package.json'), 'utf8')).version;
    assert.equal(declarada, versao, `Pin incorreto: ${nome}`);
    assert.equal(instalada, versao, `Módulo incompatível: ${nome}`);
  }
});

test('documentação vigente distingue ambiente público, HMR e integração autorizada', () => {
  const pkg = JSON.parse(readFileSync(join(raiz, 'package.json'), 'utf8'));
  assert.match(pkg.scripts['dependencias:empacotar:fedora'], /gerar-pacote-dependencias-fedora/);
  const documento = readFileSync(join(raiz, 'documentacao/FRONTEND_TYPESCRIPT.md'), 'utf8');
  assert.match(documento, /127\.0\.0\.1:5173/);
  assert.match(documento, /127\.0\.0\.1:5000/);
  assert.match(documento, /cookies.*CSRF/);
  assert.match(documento, /sync:java:check/);
});
