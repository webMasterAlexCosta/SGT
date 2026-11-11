'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend } = require('./contexto-fixture.cjs');
const unwrap = value => value?.data ?? value;
execute('rotas-retiradas-cache-real', async ({ page, context, fixture, report, check, open, screenshot }) => {
  const base=baseFrontend();
  const business=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json')));
  await open('/empresas-contratadas');
  const noLegacyUi=async()=>{
    const label=/^(Governança de Terceiros|Risco de Terceiros|Tabelas tarifárias|Folhas de horas|Faturas)$/i;
    return await page.getByRole('button',{name:label}).count()===0 && await page.getByRole('link',{name:label}).count()===0
      && await page.locator('a[href*="governanca-terceiros"]').count()===0;
  };
  check('A tela operacional de empresas aparece sem itens de risco/financeiro retirados',await noLegacyUi());
  const providerPath='/api/fornecedores';
  const read=async route=>{
    const response=await context.request.get(base+route,{headers:{Accept:'application/json'},timeout:10000});
    const body=await response.body(),headers=response.headers();
    report.http.push({method:'GET',path:route,status:response.status(),cache:headers['x-sgt-cache']||null});
    return {status:response.status(),cache:headers['x-sgt-cache'],mime:headers['content-type'],body};
  };
  const first=await read(providerPath),warm=await read(providerPath);
  const providers=unwrap(JSON.parse(warm.body));
  check('A mesma conta autorizada obtém200/HIT real em fornecedores e bytes estáveis',first.status===200&&warm.status===200
    && warm.cache==='HIT'&&warm.mime.includes('application/json')&&first.body.equals(warm.body)&&Array.isArray(providers)
    && providers.some(x=>x.id===business.jobs200FornecedorId&&x.ativo===true));
  const prefix='/api/governanca-terceiros';
  const oldRoutes=['','/','/riscos','/resumo','/fornecedores','/processos','/tabelas-tarifarias','/folhas-horas','/faturas'];
  for(const suffix of oldRoutes){
    const route=prefix+suffix;const response=await read(route);
    check(`GET retirado não passa pelo cache nem retorna dados: ${route}`,response.status===404&&response.cache===undefined
      && response.mime.includes('application/json')&&!response.body.includes(Buffer.from('LEGADO SINTETICO')));
  }
  const after=await read(providerPath);
  check('As recusas404 não revogam a sessão nem invalidam o HIT operacional',after.status===200&&after.cache==='HIT'&&after.body.equals(warm.body)
    && await page.getByRole('button',{name:'Sair',exact:true}).isVisible());
  const contract=await read('/api/contratos/'+business.jobs200ContratoId),entity=unwrap(JSON.parse(contract.body));
  check('O mesmo fornecedor permanece associado a contrato operacional publicado',contract.status===200&&entity.id===business.jobs200ContratoId
    && entity.fornecedorId===business.jobs200FornecedorId&&entity.estadoNegocial==='PUBLICADO');
  await screenshot('empresas-operacionais');
  await open('/frequencia');
  check('Frequência permanece acessível sem menu legado',await page.getByLabel('Data de referência da frequência',{exact:true}).isVisible()&&await noLegacyUi());
  check('O roteiro não envia mutações de fornecedores, legado ou contratos',report.http.every(x=>x.method==='GET'||!/^\/api\/(fornecedores|governanca-terceiros|contratos)(\/|$)/.test(x.path)));
  report.cacheWitness={state:'REAL_OPERATIONAL_HIT_AND_NINE_AUTHENTICATED_404',providerCount:providers.length,operationalBodySha256:crypto.createHash('sha256').update(warm.body).digest('hex'),noFlush:true,noCacheInjection:true};
  report.limits=['JAR e Valkey próprios reais; nenhuma chave antiga foi criada ou removida neste roteiro.',
    'A prova de chave legada inerte é separada no backend. Escopo local Chrome; sem LAN ou homologação global.'];
}, { accountName:'gestor_local', reuse:false });
