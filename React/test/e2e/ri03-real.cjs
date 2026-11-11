// Respostas verdadeiras do JAR retidas; nunca fabrica sucesso ou payload de API.
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { baseFrontend } = require('./contexto-fixture.cjs');
const { execute } = require('./ambiente-real.cjs');
const phase = process.env.SGT_RI03_PHASE;
if (!['post','recover','put','get'].includes(phase)) throw new Error('Defina SGT_RI03_PHASE post, recover, put ou get.');
const bounded = async (promise, description, milliseconds=20000) => {
  let timer;
  try { return await Promise.race([promise,new Promise((_,reject) => { timer=setTimeout(() => reject(new Error(description)),milliseconds); })]); }
  finally { clearTimeout(timer); }
};

execute('ri03-real-' + phase, async ({ page, context, fixture, account, report, check, open, confirmPassword, recordBusiness, screenshot }) => {
  let b = JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json')));
  const base = baseFrontend();
  const contractId = phase === 'put' ? b.ri03ContratoPutId : b.ri03ContratoPostId;
  check('Dois rascunhos reais distintos preparados', b.ri03ContratoPostId > 0 && b.ri03ContratoPutId > 0 && b.ri03ContratoPostId !== b.ri03ContratoPutId);
  const observe = label => {
    const result = JSON.parse(execFileSync('python3',[process.env.SGT_RI03_DB_OBSERVER,label],{env:process.env,encoding:'utf8',timeout:15000,stdio:['ignore','pipe','pipe']}));
    check(`PostgreSQL real reconciliado: ${label}`, result.state === 'PASS' && result.readonly_limited_ownership);
    return result;
  };
  await open('/empresas-contratadas');
  await page.getByText('Carregando empresas...', { exact:true }).waitFor({state:'hidden'});
  const traffic=[];
  const onRequest = request => {
    const url=new URL(request.url()); if(!url.pathname.startsWith('/api/'))return;
    let tab='context'; try{tab=request.frame().page()===page?'A':'B';}catch{}
    traffic.push({tab,method:request.method(),path:url.pathname,started:Date.now()});
  };
  context.on('request',onRequest);
  const prepareAssociation = async () => {
    await page.getByRole('searchbox',{name:'Pesquisar empresas',exact:true}).fill(b.ri03FornecedorNome);
    const row=page.getByRole('row').filter({has:page.getByRole('cell',{name:b.ri03FornecedorNome,exact:true})});
    await row.waitFor();await row.getByRole('button',{name:'Vincular',exact:true}).click();
    const region=page.getByRole('region',{name:'Vínculo pendente da empresa cadastrada',exact:true});
    // A seção é semântica quando aria-label é usado; não selecionar por classes.
    await region.getByRole('combobox',{name:'Contrato para vincular a empresa cadastrada',exact:true}).selectOption(String(contractId));
    return region;
  };
  const associationPath=()=>`/api/contratos/${contractId}/fornecedor/${b.ri03FornecedorId}`;
  const begin = async () => {
    if(phase==='get') {
      const group=page.getByRole('button',{name:'Cadastros e configurações',exact:true});
      if(await group.getAttribute('aria-expanded')!=='true')await group.click();
      await page.getByRole('button',{name:'Contratos e efetivo',exact:true}).click();
      return;
    } else if(phase==='post') {
      await page.getByLabel('Razão social da empresa',{exact:true}).fill(b.ri03FornecedorNome);
      await page.getByLabel('CNPJ da empresa',{exact:true}).fill(b.ri03FornecedorCnpj);
      await page.getByLabel('E-mail da empresa',{exact:true}).fill('ri03-final02@example.test');
      await page.getByRole('combobox',{name:'Contrato a vincular à empresa',exact:true}).selectOption(String(contractId));
      await page.getByRole('button',{name:'Cadastrar e vincular',exact:true}).click();
    } else {
      const region=await prepareAssociation();
      await region.getByRole('button',{name:'Vincular empresa cadastrada',exact:true}).click();
    }
    await confirmPassword();
  };
  if(phase==='recover') {
    check('Recuperação exige POST protegido e uma empresa real existente', b.ri03PostProtected===true && b.ri03FornecedorId>0 && !b.ri03RecoveryConfirmed);
    observe('recovery-before');
    const pending=page.waitForResponse(r=>new URL(r.url()).pathname===associationPath()&&r.request().method()==='PUT');pending.catch(()=>{});
    await begin();const response=await pending;const dto=(await response.json()).data;
    check('Recuperação positiva envia somente associação sob autoridade válida', response.status()===200 && dto.contratoId===contractId && dto.fornecedor?.id===b.ri03FornecedorId);
    await page.getByLabel('Senha de Acesso',{exact:true}).waitFor({state:'hidden'});
    check('Nenhum novo POST de empresa foi enviado na recuperação', traffic.every(x=>!(x.method==='POST'&&x.path==='/api/fornecedores')));
    const current=await context.request.get(base+`/api/contratos/${contractId}`);const persisted=(await current.json()).data;
    check('GET independente confirma associação legítima',current.status()===200 && persisted.fornecedorId===b.ri03FornecedorId);
    observe('recovered');recordBusiness({ri03RecoveryConfirmed:true});
    report.traffic=traffic;report.scope='RI03_REAL_POSITIVE_RECOVERY_ONLY_EXISTING_SUPPLIER_ASSOCIATION_NOT_AN_ORIGINAL_DRIVER';await screenshot('recuperacao-somente-associacao');return;
  }
  check('Fase protegida não deve repetir intenção já executada', phase==='post' ? !b.ri03FornecedorId : phase==='put' ? b.ri03RecoveryConfirmed===true && !b.ri03PutProtected : b.ri03PutProtected===true && !b.ri03GetProtected);
  if(phase==='post')observe('prepared');
  let capturedResolve,capturedReject,release,finishedResolve;
  const captured=new Promise((resolve,reject)=>{capturedResolve=resolve;capturedReject=reject;});captured.catch(()=>{});
  const gate=new Promise(resolve=>{release=resolve;});const finished=new Promise(resolve=>{finishedResolve=resolve;});
  const target=phase==='post'?'/api/fornecedores':phase==='get'?'/api/contratos':associationPath();const method=phase==='post'?'POST':phase==='get'?'GET':'PUT';
  let intercepted=false,heldMeta,deliveryAttempted=false,delivered=false,deliveryAuthorized=false;
  const pattern='**'+target;
  const handler=async route=>{
    if(intercepted||route.request().method()!==method)return route.continue();
    intercepted=true;
    try {
      const real=await route.fetch();const body=await real.json();
      heldMeta={status:real.status(),data:body.data,committedAt:Date.now()};
      capturedResolve(heldMeta);await gate;
      if(!deliveryAuthorized){await route.abort('aborted').catch(()=>{});return;}
      deliveryAttempted=true;
      try{await route.fulfill({response:real});delivered=true;}catch{}
    }catch(error){capturedReject(error);}
    finally{finishedResolve();}
  };
  await page.route(pattern,handler);
  let tabB;
  const dismiss = async tab => {
    await tab.getByText('CLIENTE-DIAGNÓSTICO',{exact:true}).waitFor({state:'hidden',timeout:15000});
    const welcome=tab.getByRole('button',{name:/Ciente, Vamos lá/i});if(await welcome.isVisible())await welcome.click();
    const reminder=tab.getByRole('button',{name:/Lembrar em 7 dias/i});if(await reminder.isVisible())await reminder.click();
  };
  const login=async (tab,user)=>{
    await tab.getByRole('button',{name:/Entrar no Sistema/i}).waitFor();
    await tab.getByLabel(/^Matrícula$/i).fill(user.matricula);await tab.getByLabel(/^Senha$/i).fill(user.password);
    await tab.getByRole('button',{name:/Entrar no Sistema/i}).click();
    await tab.getByRole('button',{name:'Sair',exact:true}).waitFor({timeout:20000});await dismiss(tab);
  };
  const logout=async tab=>{
    const pending=tab.waitForResponse(r=>new URL(r.url()).pathname==='/api/autenticacao/encerrar-sessao'&&r.request().method()==='POST');pending.catch(()=>{});
    await tab.getByRole('button',{name:'Sair',exact:true}).click();check('Logout explícito da aba ativa confirmado pelo servidor',(await pending).status()===200);
    await tab.getByRole('button',{name:/Entrar no Sistema/i}).waitFor({timeout:20000});
  };
  try {
    await begin();const meta=await bounded(captured,'Mutação real não chegou à barreira em20s.');
    check('Resposta retida foi produzida pelo Java real com sucesso',meta.status===200);
    if(phase==='post') {
      check('POST retido contém uma única identidade real esperada',meta.data.id>0&&meta.data.cnpj===b.ri03FornecedorCnpj&&meta.data.razaoSocial===b.ri03FornecedorNome);
      recordBusiness({ri03FornecedorId:meta.data.id});b={...b,ri03FornecedorId:meta.data.id};
    } else if(phase==='put') check('PUT retido já associou o contrato solicitado',meta.data.contratoId===contractId&&meta.data.fornecedor?.id===b.ri03FornecedorId);
    else check('GET retido contém contratos reais privados da Gestão A',Array.isArray(meta.data)&&meta.data.some(item=>item.id===b.ri03ContratoPostId)&&meta.data.some(item=>item.id===b.ri03ContratoPutId));
    observe(phase+'-held');
    tabB=await context.newPage();tabB.on('pageerror',error=>report.errors.push(error.message));
    await tabB.goto(base+'/contratos',{waitUntil:'networkidle'});await dismiss(tabB);
    check('Duas abas reais compartilham sessão A antes da troca',context.pages().length===2&&(await tabB.locator('body').innerText()).includes(account.username));
    await logout(tabB);await login(tabB,fixture.accounts.gestor_destino);
    const sessionResponse=await context.request.get(base+'/api/autenticacao/sessao');const session=(await sessionResponse.json()).data;
    const bConfirmedAt=Date.now();
    check('Sessão B efetivamente autenticada antes da entrega antiga',sessionResponse.status()===200&&session.authenticated===true&&session.user?.id===fixture.accounts.gestor_destino.id&&(await tabB.locator('body').innerText()).includes(fixture.accounts.gestor_destino.username));
    deliveryAuthorized=true;release();await bounded(finished,'Entrega da resposta verdadeira não terminou em20s.');
    await page.bringToFront();await page.getByRole('button',{name:/Entrar no Sistema/i}).waitFor({timeout:10000});
    await page.waitForTimeout(350);const observationEnd=Date.now();
    const afterB=traffic.filter(x=>x.started>=bConfirmedAt&&x.started<=observationEnd);
    check('Aba A invalidou identidade e formulário da sessão anterior',await page.getByRole('button',{name:'Sair',exact:true}).count()===0&&!(await page.locator('body').innerText()).includes(account.username));
    const bodyB=await tabB.locator('body').innerText();
    check('Dados contratuais de A não aparecem como atuais na aba B',[b.ri03ContratoPostIdNumero,b.ri03ContratoPutIdNumero].every(number=>typeof number==='string'&&number.length>0&&!bodyB.includes(number)));
    check('Nenhum PUT de associação foi iniciado após autenticação B',afterB.every(x=>!(x.method==='PUT'&&/\/contratos\/\d+\/fornecedor\//.test(x.path))));
    check('Nenhum POST de empresa nem leitura negocial antiga foi iniciado pela aba A',afterB.every(x=>x.tab!=='A'||!((x.method==='POST'&&x.path==='/api/fornecedores')||(x.method==='GET'&&/^\/api\/(contratos|fornecedores)(\/|$)/.test(x.path)))));
    check('Sincronização da aba A não revogou por HTTP a sessão B',afterB.every(x=>!(x.method==='POST'&&x.path==='/api/autenticacao/encerrar-sessao')));
    const currentResponse=await context.request.get(base+'/api/autenticacao/sessao');const current=(await currentResponse.json()).data;
    check('Sessão B permanece válida depois da resposta antiga',currentResponse.status()===200&&current.user?.id===fixture.accounts.gestor_destino.id);
    const marker=await tabB.evaluate(()=>localStorage.getItem('sgt_revisao_compartilhada_sessao'));
    check('Marcador de coordenação é opaco e não contém identidade ou credencial',typeof marker==='string'&&/^(?:[a-f0-9-]{36}|\d+-[a-z0-9]+)$/i.test(marker)&&Object.values(fixture.accounts).every(user=>![user.username,user.matricula,user.password].some(value=>value&&marker.includes(value))));
    observe(phase+'-after');recordBusiness(phase==='post'?{ri03PostProtected:true}:phase==='put'?{ri03PutProtected:true}:{ri03GetProtected:true});
    report.barrier={method,target,serverStatus:meta.status,committedAt:meta.committedAt,bAuthenticatedAt:bConfirmedAt,deliveryAttempted,delivered,observationEnd,quietWindowMilliseconds:350,notFakeResponse:true};
    report.traffic=traffic;await screenshot('abaA-invalidada-sem-associacao-tardia');
    await logout(tabB);await login(tabB,account);await tabB.close();tabB=null;
    await open('/contratos');check('Sessão A restaurada por login explícito ao concluir lote',(await page.locator('body').innerText()).includes(account.username));
  } finally {
    release();await bounded(finished,'Limpeza da barreira RI03 não terminou.',5000).catch(()=>{});
    await page.unroute(pattern,handler);if(tabB)await tabB.close();context.off('request',onRequest);
  }
  report.scope='RI03_REAL_TWO_TABS_POST_PUT_OR_GET_NOT_AN_ADDITIONAL_ORIGINAL_DRIVER';
}, {accountName:'gestor_local',reuse:false});
