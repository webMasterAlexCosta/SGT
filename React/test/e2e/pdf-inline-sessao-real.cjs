'use strict';
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend } = require('./contexto-fixture.cjs');
const unwrap = body => body && Object.hasOwn(body, 'data') ? body.data : body;
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const latch = () => { let resolve; const promise = new Promise(r => { resolve = r; }); return { promise, resolve }; };
async function bounded(promise, label, ms=15000) {
  let timer;
  try { return await Promise.race([promise,new Promise((_, reject) => { timer=setTimeout(() => reject(new Error(label)),ms); })]); }
  finally { clearTimeout(timer); }
}
execute('pdf-inline-sessao-real', async ({ page, context, account, fixture, report, check, open, screenshot }) => {
  const b=fixture.accounts.gestor_destino, base=baseFrontend();
  check('A e B são dois gestores locais reais, ativos e com escopos distintos', account.perfil==='GESTOR_LOCAL' && b.perfil==='GESTOR_LOCAL'
    && account.id!==b.id && account.gestaoLocalId!==b.gestaoLocalId);
  const panel=page.getByRole('heading',{name:'Gestão operacional e contratual',exact:true});
  const reference=page.getByLabel('Data de referência',{exact:true});
  const pdf=page.locator('button[title="Exportar análise executiva do painel em PDF"]');
  const documentMarker=crypto.randomUUID();
  const assetPattern=/\/assets\/vendor-pdf-[A-Za-z0-9_-]+\.js(?:\?.*)?$/;
  const observed=[], downloads=[];
  page.on('request',req=>{ if(assetPattern.test(req.url()))observed.push(new URL(req.url()).pathname); });
  page.on('download',download=>downloads.push(download.suggestedFilename()));
  const identity=async expected=>{
    const response=await context.request.get(base+'/api/autenticacao/sessao');
    const data=unwrap(await response.json());
    check(`Sessão Java real reconhece ${expected===account?'A':'B'}`,response.status()===200&&data?.authenticated===true&&data?.user?.id===expected.id);
  };
  const selectDate=async(iso, civil)=>{
    await panel.waitFor({timeout:20000});await reference.fill(iso);
    await page.getByText(new RegExp(`referência final em ${civil.replaceAll('/','\\/')}`)).waitFor({timeout:45000});
    await pdf.locator('xpath=self::button[not(@disabled)]').waitFor({timeout:45000});
    assert.equal(await reference.inputValue(),iso);
    const value=await panel.locator('..').locator('p').textContent();
    assert.ok(value?.includes(civil),'Cabeçalho de painel confirmado para a referência solicitada.');
    return value.split(' · janela analítica')[0].trim();
  };
  await open('/painel');await identity(account);
  const dateA=fixture.ocorrencia_restrita.inicio, dateB=fixture.relatorio41.data;
  const civil=iso=>iso.split('-').reverse().join('/');
  assert.match(dateA,/^\d{4}-\d{2}-\d{2}$/);assert.match(dateB,/^\d{4}-\d{2}-\d{2}$/);assert.notEqual(dateA,dateB);
  const scopeA=await selectDate(dateA,civil(dateA));
  await page.evaluate(marker=>{ window.__sgtPdfWitnessDocument=marker; },documentMarker);
  check('Documento frio ainda não solicitou o chunk do PDF',observed.length===0 && await page.evaluate(()=>!performance.getEntriesByType('resource')
    .some(entry=>/\/assets\/vendor-pdf-/.test(entry.name))));
  const held=latch(), release=latch(), done=latch();
  let requests=0;
  const handler=async route=>{
    if (++requests!==1) return route.continue();
    let payload;
    try {
      const response=await route.fetch({timeout:10000});
      const url=new URL(route.request().url());const bytes=await response.body();
      const installed=fs.readFileSync(path.join(__dirname,'../../dist/assets',path.basename(url.pathname)));
      payload={path:url.pathname,status:response.status(),bytes:bytes.length,sha256:hash(bytes),sameDist:hash(installed)===hash(bytes)};
      if (payload.status!==200 || !payload.sameDist || bytes.length<=10000) throw new Error('Chunk real do JAR divergiu de dist.');
      held.resolve({payload});await release.promise;
      await route.fulfill({response});done.resolve({payload,delivered:true});
    } catch(error) {
      const result={payload,reason:error.message};held.resolve(result);done.resolve(result);
      await route.abort().catch(()=>{});
    }
  };
  await page.route(assetPattern,handler);
  try {
    await pdf.click();
    const captured=await bounded(held.promise,'Exportação A não atingiu o import dinâmico frio.');
    check('Clique A dispara import frio e retém exclusivamente o JS200 original com SHA de dist',captured.payload?.sameDist && !captured.reason && requests===1 && downloads.length===0,captured.payload);
    const logout=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname==='/api/autenticacao/encerrar-sessao');
    logout.catch(()=>{});await page.getByRole('button',{name:'Sair',exact:true}).click();
    check('Logout A encerra a sessão no servidor',(await logout).status()===200);
    const login=page.getByRole('button',{name:/Entrar no Sistema/i});await login.waitFor({timeout:20000});
    check('Antes de liberar o import A, a UI remove o painel e o escopo A',await panel.count()===0 && !(await page.locator('body').innerText()).includes(scopeA));
    await page.getByLabel(/^Matrícula$/i).fill(b.matricula);await page.getByLabel(/^Senha$/i).fill(b.password);await login.click();
    await page.getByRole('button',{name:'Sair',exact:true}).waitFor({timeout:20000});await identity(b);
    await page.getByText('CLIENTE-DIAGNÓSTICO',{exact:true}).waitFor({state:'hidden',timeout:15000});
    const welcome=page.getByRole('button',{name:/Ciente, Vamos lá/i});if(await welcome.isVisible())await welcome.click();
    const remind=page.getByRole('button',{name:/Lembrar em 7 dias/i});if(await remind.isVisible())await remind.click();
    const scopeB=await selectDate(dateB,civil(dateB));
    check('B apresenta seu escopo distinto no mesmo documento JavaScript',scopeB!==scopeA && scopeB!=='Escopo autenticado'
      && await page.evaluate(marker=>window.__sgtPdfWitnessDocument===marker,documentMarker) && downloads.length===0);
    const received=page.waitForResponse(res=>assetPattern.test(res.url())&&res.status()===200,{timeout:10000});received.catch(()=>{});
    release.resolve();const delivery=await bounded(done.promise,'Liberação do import original não concluiu.');
    check('Chrome recebeu o chunk original após B assumir a sessão',delivery.delivered && hash(await (await received).body())===delivery.payload.sha256);
    // A importação é resolvida e as microtarefas da aplicação executam antes do próximo frame.
    await page.evaluate(async()=>{ await new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))); });
    check('A exportação obsoleta não dispara download, iframe nem reapresenta dados A',downloads.length===0
      && await page.locator('iframe[title^="download-relatorio-"]').count()===0 && !(await page.locator('body').innerText()).includes(scopeA));
    await screenshot('sessao-b-antes-do-controle');
    const filePromise=page.waitForEvent('download',{timeout:15000});filePromise.catch(()=>{});await pdf.click();
    const control=await filePromise;
    check('Clique B usa o mesmo chunk já carregado e baixa exatamente o PDF B',!await control.failure() && downloads.length===1
      && control.suggestedFilename().includes(dateB) && !control.suggestedFilename().includes(dateA) && observed.length===1 && requests===1);
    const output=path.join(process.env.SGT_E2E_OUTPUT,'painel-pdf-inline-controle-b.pdf');await control.saveAs(output);fs.chmodSync(output,0o600);
    const bytes=fs.readFileSync(output);
    const content=execFileSync('/usr/bin/pdftotext',['-layout',output,'-'],{encoding:'utf8',timeout:10000});
    check('PDF real de B contém sua referência e seu escopo, sem os de A',bytes.subarray(0,5).toString()==='%PDF-'
      && bytes.subarray(-1024).includes(Buffer.from('%%EOF')) && content.includes(`Referência: ${civil(dateB)}`) && content.includes(scopeB)
      && !content.includes(`Referência: ${civil(dateA)}`) && !content.includes(scopeA));
    report.import={...delivery.payload,sameDocument:true,actualDeliveryAfterSessionChange:true,actualRequests:requests};
    report.downloads=[{file:path.basename(output),bytes:bytes.length,sha256:hash(bytes),reference:dateB}];
    report.limits=['Arquivo inline via import dinâmico do Painel, sem criação de job de relatório e sem substituir respostas por dados inventados.',
      'Um Chrome desktop local; não demonstra acesso LAN nem aprovação global.'];
  } finally {
    release.resolve();
    if(requests) await bounded(done.promise,'Handler do chunk ainda ativo após liberação.').catch(()=>{});
    await page.unroute(assetPattern,handler);
  }
  // A gravação do harness precisa terminar com a identidade associada ao arquivo de estado A.
  await page.getByRole('button',{name:'Sair',exact:true}).click();
  await page.getByRole('button',{name:/Entrar no Sistema/i}).waitFor({timeout:20000});
  await open('/painel');await identity(account);
},{accountName:'painel_gestor',reuse:false});
