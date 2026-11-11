'use strict';
const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const {execFileSync}=require('node:child_process');
const {execute}=require('./ambiente-real.cjs');
const unwrap=value=>value?.data??value;
const sha=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const latch=()=>{let resolve;const promise=new Promise(r=>{resolve=r;});return{promise,resolve};};
async function bounded(promise,message,ms=15000){
  let t;try{return await Promise.race([promise,new Promise((_,reject)=>{t=setTimeout(()=>reject(new Error(message)),ms);})]);}finally{clearTimeout(t);}
}
const verifyWorkbook=String.raw`
import json,sys,zipfile,xml.etree.ElementTree as ET
ns={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
with zipfile.ZipFile(sys.argv[1]) as z:
 assert z.testzip() is None and all(x.file_size<1000000 for x in z.infolist())
 shared=[''.join(e.itertext()) for e in ET.fromstring(z.read('xl/sharedStrings.xml'))] if 'xl/sharedStrings.xml' in z.namelist() else []
 def sheet(file):
  cells={}
  for cell in ET.fromstring(z.read(file)).findall('.//s:c',ns):
   value=cell.find('s:v',ns)
   val=value.text if value is not None else ''
   if cell.get('t')=='inlineStr':val=''.join(cell.itertext())
   elif cell.get('t')=='s':val=shared[int(val)]
   cells[cell.get('r')]=val
  return cells
 summary=sheet('xl/worksheets/sheet1.xml');series=sheet('xl/worksheets/sheet2.xml')
 print(json.dumps({'reference':summary['B3'],'period':summary['D3'],'current':summary['B10'],'previous':summary['C10'],
  'last_date':series['A17'],'last_value':series['B17'],'last_comparison':series['L17'],'CRC':True}))
`;
execute('painel-ordem-real',async({page,context,fixture,report,check,open,screenshot})=>{
  const f=fixture.painel_bissexto;
  const settings=await context.newPage();
  try{
    await settings.goto('chrome://settings/appearance');
    const zoom=settings.getByRole('combobox').filter({has:settings.getByRole('option',{name:'150%',exact:true})});
    await zoom.selectOption({label:'150%'});check('Chrome desktop configurado em zoom nativo 150%',await zoom.inputValue()==='1.5');
  }finally{await settings.close();}
  await open('/painel');
  const cdp=await context.newCDPSession(page),metrics=await cdp.send('Page.getLayoutMetrics');
  check('Métrica real de layout confirma zoom1.5, sem pinch ou CSS zoom',Math.abs(metrics.cssVisualViewport.zoom-1.5)<.001
    && metrics.cssVisualViewport.scale===1 && await page.evaluate(()=>getComputedStyle(document.documentElement).zoom==='1'&&getComputedStyle(document.body).zoom==='1'));
  const indicator=page.getByRole('region',{name:'Indicador contratual de efetivo',exact:true});
  const reference=indicator.getByLabel('Data de referência do indicador',{exact:true});
  const xlsx=indicator.getByRole('button',{name:'XLSX',exact:true}),pdf=indicator.getByRole('button',{name:'PDF',exact:true});
  const busy=indicator.locator('[aria-busy="true"]');
  await reference.fill('2024-02-27');await indicator.getByRole('button',{name:'Quinzenal',exact:true}).click();
  for(const [label,id] of [['Lotação',f.lotacaoId],['Turno',f.jornadaId],['Cargo',f.cargoId]])
    await indicator.getByRole('combobox',{name:label,exact:true}).selectOption(String(id));
  await indicator.getByRole('button',{name:'Comparar ano anterior',exact:true}).click();
  await xlsx.locator('xpath=self::button[not(@disabled)]').waitFor({timeout:15000});
  const cases={A:{date:'2024-02-28',period:'MENSAL',value:228},B:{date:f.referencia,period:'QUINZENAL',value:229}};
  const classify=url=>{
    const u=new URL(url);if(u.pathname!=='/api/painel/efetivo'||u.searchParams.get('compararAnoAnterior')!=='true'
      || !['lotacaoId','jornadaId','cargoId'].every(k=>u.searchParams.get(k)===String(f[k])))return null;
    return Object.keys(cases).find(k=>cases[k].date===u.searchParams.get('referencia')&&cases[k].period===u.searchParams.get('periodo'))??null;
  };
  const gates=Object.fromEntries(Object.keys(cases).map(k=>[k,{held:latch(),release:latch(),done:latch(),requests:0}]));
  const events=[],downloads=[];
  page.on('download',d=>downloads.push(d.suggestedFilename()));
  page.on('requestfailed',request=>{const k=classify(request.url());if(k)events.push({case:k,type:'requestfailed',cause:request.failure()?.errorText});});
  const pattern='**/api/painel/efetivo?*';
  const handler=async route=>{
    const k=classify(route.request().url());if(!k)return route.continue();
    const gate=gates[k];if(++gate.requests!==1)return route.continue();
    let payload;
    try{
      const result=await route.fetch({timeout:10000}),bytes=await result.body(),dto=unwrap(JSON.parse(bytes));
      payload={status:result.status(),sha256:sha(bytes),bytes:bytes.length,fim:dto.fim,periodo:dto.periodo,value:dto.resumoAtual?.efetivoPrevisto,previous:dto.resumoAnoAnterior?.efetivoPrevisto};
      gate.held.resolve(payload);events.push({case:k,type:'upstream-captured'});await gate.release.promise;
      await route.fulfill({response:result});events.push({case:k,type:'released-to-Chrome'});gate.done.resolve({delivered:true,payload});
    }catch(error){
      gate.held.resolve({error:error.message});gate.done.resolve({delivered:false,payload,error:error.message});await route.abort().catch(()=>{});
    }
  };
  await page.route(pattern,handler);
  try{
    await reference.fill(cases.A.date);await indicator.getByRole('button',{name:'Mensal',exact:true}).click();
    const a=await bounded(gates.A.held.promise,'A mensal não chegou ao Java.');
    check('A mensal retornou200 real com o discriminante228, retido antes de B',a.status===200&&a.fim===cases.A.date&&a.periodo===cases.A.period&&a.value===cases.A.value);
    await busy.waitFor();check('Loading A desabilita ambas exportações',await xlsx.isDisabled()&&await pdf.isDisabled()&&downloads.length===0);
    await reference.fill(cases.B.date);await indicator.getByRole('button',{name:'Quinzenal',exact:true}).click();
    const b=await bounded(gates.B.held.promise,'B quinzenal não chegou ao Java.');
    check('B quinzenal retornou200 real com229/128 enquanto A ainda está retido',b.status===200&&b.fim===cases.B.date&&b.periodo===cases.B.period&&b.value===229&&b.previous===128&&a.sha256!==b.sha256);
    await busy.waitFor();check('Loading B mantém exportações desabilitadas sem reutilizar A',await xlsx.isDisabled()&&await pdf.isDisabled()&&downloads.length===0);
    await screenshot('loading-b-150');
    const delivered=page.waitForResponse(r=>classify(r.url())==='B'&&r.status()===200,{timeout:10000});delivered.catch(()=>{});
    gates.B.release.resolve();const bDelivery=await bounded(gates.B.done.promise,'Resposta B não liberada ao Chrome.');
    check('A entrega real B chega primeiro com os mesmos bytes',bDelivery.delivered&&sha(await (await delivered).body())===b.sha256);
    const row=indicator.getByRole('row').filter({has:page.getByRole('cell',{name:'Previsto em contrato',exact:true})});
    const exact=()=>row.getByRole('cell').allTextContents().then(v=>v.join('|')==='Previsto em contrato|229|128');
    await row.getByRole('cell',{name:'229',exact:true}).waitFor();await busy.waitFor({state:'hidden'});
    check('Após B, conteúdo229/128, referência e exportação habilitada',await exact()&&await reference.inputValue()===cases.B.date&&await xlsx.isEnabled());
    gates.A.release.resolve();const late=await bounded(gates.A.done.promise,'Resposta A não encerrou após liberação tardia.');
    await page.evaluate(async()=>{await new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));});
    check('A original liberada tarde ou cancelada pelo transporte não muda B',!!late.payload
      && (late.delivered||events.some(e=>e.case==='A'&&e.type==='requestfailed'&&/ABORT|CANCEL/i.test(e.cause||'')))
      && await exact()&&await reference.inputValue()===cases.B.date&&await xlsx.isEnabled()&&downloads.length===0);
    await screenshot('b-atual-150');
    const downloaded=page.waitForEvent('download',{timeout:15000});downloaded.catch(()=>{});await xlsx.click();const file=await downloaded;
    const output=path.join(process.env.SGT_E2E_OUTPUT,'painel-atual-b.xlsx');await file.saveAs(output);fs.chmodSync(output,0o600);
    const content=JSON.parse(execFileSync('/usr/bin/python3',['-c',verifyWorkbook,output],{encoding:'utf8',timeout:10000}));
    check('Somente o XLSX B foi baixado e o conteúdo real confirma29/02,229/128 e ausência comparativa',!await file.failure()
      && downloads.length===1&&file.suggestedFilename().includes(cases.B.date)&&content.CRC&&content.reference==='29/02/2024'
      && /Quinzenal/i.test(content.period)&&content.current==='229'&&content.previous==='128'&&content.last_date==='29/02/2024'
      && content.last_value==='229'&&content.last_comparison==='—');
    report.witness={scope:'ACTUAL_A_AND_B_HTTP_B_RELEASED_FIRST',zoom:metrics.cssVisualViewport.zoom,A:a,B:b,A_delivered_after_B:late.delivered,events};
    report.downloads=[{file:path.basename(output),sha256:sha(fs.readFileSync(output)),...content}];
    report.limits=['Respostas originais do Java; o harness só determina quando liberá-las. XLSX verificável localmente; não é LAN.'];
  }finally{
    for(const gate of Object.values(gates))gate.release.resolve();
    for(const gate of Object.values(gates))if(gate.requests)await bounded(gate.done.promise,'Retenção ainda ativa.').catch(()=>{});
    await page.unroute(pattern,handler);
  }
},{accountName:'painel_gestor',reuse:false,persistent:true});
