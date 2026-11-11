// Retoma ativação já confirmada; não gera outro QR/segredo nem repete ativação.
const fs=require('node:fs');const path=require('node:path');const crypto=require('node:crypto');const {execFileSync}=require('node:child_process');
const {baseFrontend}=require('./contexto-fixture.cjs');const {execute}=require('./ambiente-real.cjs');const {totp}=require('./totp-sintetico.cjs');
execute('2fa-retomada-real',async({page,context,account,fixture,report,check,open,screenshot,recordBusiness})=>{
  const runtime=process.env.SGT_TEST_RUNTIME;const campaign=path.dirname(runtime);const base=baseFrontend();
  check('Vetor público do gerador TOTP e conta sintética designada',totp('GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ',59000)==='287082'&&account.id===fixture.accounts.nacional.id&&account.perfil==='GESTOR_NACIONAL');
  const file=path.join(runtime,'private/2fa-nacional-stage51-secret.json');check('Segredo de recuperação tem permissão privada',fs.existsSync(file)&&(fs.statSync(file).mode&0o077)===0);
  const recovery=JSON.parse(fs.readFileSync(file));const secret=recovery.secret;check('Recuperação pertence somente ao ator sintético esperado',recovery.actorId===account.id&&/^[A-Z2-7]+=*$/.test(secret));
  const sourceFile=path.join(campaign,'browser-jar-2fa01/browser/db/enabled.json');const enabledProof=JSON.parse(fs.readFileSync(sourceFile));
  check('Ativação anterior comprovada por SELECT, sem promover jornada interrompida',enabledProof.verified===true&&enabledProof.observed.actor.id===account.id&&enabledProof.observed.actor.enabled===true&&enabledProof.observed.actor.has_secret===true&&enabledProof.password_unchanged===true);
  report.setupResumed={source:'browser-jar-2fa01',enabledProofSha256:crypto.createHash('sha256').update(fs.readFileSync(sourceFile)).digest('hex'),noNewSetupOrActivation:true};
  const observe=phase=>{
    const d=JSON.parse(execFileSync('python3',[process.env.SGT_2FA_DB_OBSERVER,phase],{env:process.env,encoding:'utf8',timeout:15000,stdio:['ignore','pipe','pipe']}));
    check(`SELECT real de segurança: ${phase}`,d.state==='PASS'&&d.password_and_other_users_unchanged&&d.readonly_limited_ownership);
  };
  const waitApi=(method,pathname)=>{const p=page.waitForResponse(r=>r.request().method()===method&&new URL(r.url()).pathname===pathname);p.catch(()=>{});return p;};
  const dismiss=async()=>{
    await page.getByText('CLIENTE-DIAGNÓSTICO',{exact:true}).waitFor({state:'hidden',timeout:15000});
    const welcome=page.getByRole('button',{name:/Ciente, Vamos lá/i});if(await welcome.isVisible())await welcome.click();
    const remind=page.getByRole('button',{name:/Lembrar em 7 dias/i});if(await remind.isVisible())await remind.click();
  };
  const startChallenge=async()=>{
    await page.getByRole('button',{name:/Entrar no Sistema/i}).waitFor();
    await page.getByLabel(/^Matrícula$/i).fill(account.matricula);await page.getByLabel(/^Senha$/i).fill(account.password);
    const pending=waitApi('POST','/api/autenticacao/autenticar');await page.getByRole('button',{name:/Entrar no Sistema/i}).click();
    const r=await pending;const body=(await r.json()).data;
    check('Senha correta sozinha recebe202/desafio obrigatório',r.status()===202&&body.requiresTwoFactor===true&&body.canSkipTwoFactor===false);
    await page.getByLabel('Código do autenticador',{exact:true}).waitFor();
  };
  const fillCode=async locator=>{try{await locator.fill(totp(secret));}catch{throw new Error('Não foi possível preencher o campo TOTP; valor omitido.');}};
  const loginWithCode=async()=>{
    await fillCode(page.getByLabel('Código do autenticador',{exact:true}));const pending=waitApi('POST','/api/autenticacao/autenticar');
    await page.getByRole('button',{name:'Validar segundo fator',exact:true}).click();const response=await pending;
    check('Login com TOTP nativo válido retorna200',response.status()===200);await page.getByRole('button',{name:'Sair',exact:true}).waitFor({timeout:20000});await dismiss();
  };
  const logout=async tab=>{
    const pending=tab.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname==='/api/autenticacao/encerrar-sessao');pending.catch(()=>{});
    await tab.getByRole('button',{name:'Sair',exact:true}).click();check('Logout explícito confirmado no Java',(await pending).status()===200);
    await tab.getByRole('button',{name:/Entrar no Sistema/i}).waitFor({timeout:20000});
  };
  const security=async()=>{
    await page.getByRole('button',{name:'Proteção Avançada (2FA)',exact:true}).click();
    return page.getByRole('dialog',{name:'Proteção Avançada (2FA)',exact:true});
  };
  await page.goto(base+'/login',{waitUntil:'networkidle'});await startChallenge();await loginWithCode();await open('/perfil');observe('enabled');
  let modal=await security();await modal.getByRole('button',{name:'Desativar segundo fator',exact:true}).waitFor();await screenshot('ativacao-retomada-confirmada');
  await modal.getByRole('button',{name:'Fechar',exact:true}).click();await modal.waitFor({state:'hidden'});await logout(page);
  await startChallenge();check('Desafio não emite cookie de autenticação',!(await context.cookies()).some(cookie=>cookie.name==='autenticacao'));observe('challenge');
  const codes=new Set(Array.from({length:9},(_,index)=>totp(secret,Date.now()+(index-4)*30000)));let invalid=0;while(codes.has(String(invalid).padStart(6,'0')))invalid++;
  await page.getByLabel('Código do autenticador',{exact:true}).fill(String(invalid).padStart(6,'0'));
  const rejected=waitApi('POST','/api/autenticacao/autenticar');await page.getByRole('button',{name:'Validar segundo fator',exact:true}).click();
  check('Código inválido recebe401 e não abre interface autenticada',(await rejected).status()===401&&await page.getByRole('button',{name:'Sair',exact:true}).count()===0);
  if(!await page.getByLabel('Código do autenticador',{exact:true}).count())await startChallenge();await loginWithCode();
  const response=await context.request.get(base+'/api/autenticacao/sessao');const session=(await response.json()).data;
  check('Sessão identifica ator e segundo fator realmente verificado',response.status()===200&&session.authenticated===true&&session.user.id===account.id&&session.user.twoFactorVerified===true);observe('verified');
  const cookies=(await context.cookies()).filter(cookie=>['autenticacao','id_dispositivo'].includes(cookie.name));
  check('Cookies HttpOnly/Strict/pathraiz correspondem à origemHTTP do laboratório',cookies.length===2&&cookies.every(cookie=>cookie.httpOnly&&cookie.sameSite==='Strict'&&cookie.path==='/'&&cookie.secure===(new URL(base).protocol==='https:')));
  report.cookieAttributes=cookies.map(({name,httpOnly,sameSite,path,secure})=>({name,httpOnly,sameSite,path,secure}));
  const tabB=await context.newPage();tabB.on('pageerror',error=>report.errors.push(error.message));
  try{
    await tabB.goto(base+'/perfil',{waitUntil:'networkidle'});check('Segunda aba compartilha a sessão2FA real',(await tabB.locator('body').innerText()).includes(account.username));
    await logout(tabB);await page.bringToFront();await page.getByRole('button',{name:/Entrar no Sistema/i}).waitFor({timeout:10000});
    check('Logout entre abas retira identidade e navegação protegidas',await page.getByRole('button',{name:'Sair',exact:true}).count()===0&&!(await page.locator('body').innerText()).includes(account.username));
    const denied=await context.request.get(base+'/api/usuarios/meu-perfil');check('Backend confirma revogação com401',denied.status()===401);observe('logged-out');await screenshot('logout-duas-abas');
  }finally{await tabB.close();}
  await startChallenge();await loginWithCode();await open('/perfil');modal=await security();
  await modal.getByLabel('Senha atual',{exact:true}).fill(account.password);await fillCode(modal.getByLabel('Código do aplicativo autenticador',{exact:true}));
  const disabledPending=waitApi('DELETE','/api/perfil/2fa');await modal.getByRole('button',{name:'Desativar segundo fator',exact:true}).click();
  const disabled=await disabledPending;check('Restauração real por senha+código retorna200',disabled.status()===200&&(await disabled.json()).data.enabled===false);
  const statusResponse=await context.request.get(base+'/api/perfil/2fa/status');const status=(await statusResponse.json()).data;
  check('2FA e pendência voltaram ao estado inicial',statusResponse.status()===200&&status.enabled===false&&status.setupPending===false);observe('restored');
  recordBusiness({twoFactorStage51Restored:true});
  // A revalidação de perfil pode remontar a página. Reabrir explicitamente é a UI real.
  await page.waitForTimeout(250);if(await modal.isVisible()){await page.keyboard.press('Escape');await modal.waitFor({state:'hidden'});}
  modal=await security();await modal.getByRole('button',{name:'Gerar QR Code',exact:true}).waitFor();await screenshot('estado-inicial-restaurado');
  report.scope='SYNTHETIC_2FA_REAL_UI_NATIVE_TOTP_TWO_TABS_SQL_PHASED_WITH_SETUP01_NOT_PHONE_NOT_GLOBAL_ACCEPTANCE';
},{accountName:'nacional',reuse:false});
