// Conta exclusivamente sintética. Segredo fica somente na memória e no private0600.
const fs=require('node:fs');const path=require('node:path');const crypto=require('node:crypto');const {execFileSync}=require('node:child_process');
const {baseFrontend}=require('./contexto-fixture.cjs');const {execute}=require('./ambiente-real.cjs');
const totp=(secret,time=Date.now())=>{
  let accumulator=0,bits=0;const bytes=[];const alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
  for(const char of secret.toUpperCase().replace(/=+$/,'')){
    const value=alphabet.indexOf(char);if(value<0)throw new Error('Formato Base32 inválido; valor omitido.');
    accumulator=(accumulator<<5)|value;bits+=5;if(bits>=8){bits-=8;bytes.push((accumulator>>>bits)&255);}
  }
  const counter=Buffer.alloc(8);counter.writeBigUInt64BE(BigInt(Math.floor(time/30000)));
  const mac=crypto.createHmac('sha1',Buffer.from(bytes)).update(counter).digest();const offset=mac[19]&15;
  return String((mac.readUInt32BE(offset)&0x7fffffff)%1000000).padStart(6,'0');
};
execute('2fa-real',async({page,context,account,fixture,report,check,open,screenshot,recordBusiness})=>{
  check('Gerador de teste confere vetor público TOTP',totp('GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ',59000)==='287082');
  check('Somente conta nacional sintética designada',account.id===fixture.accounts.nacional.id&&account.perfil==='GESTOR_NACIONAL');
  const base=baseFrontend();const privateFile=path.join(process.env.SGT_TEST_RUNTIME,'private/2fa-nacional-stage51-secret.json');
  check('Sem reaproveitar ou sobrescrever segredo de execução anterior',!fs.existsSync(privateFile));
  const observe=phase=>{
    const d=JSON.parse(execFileSync('python3',[process.env.SGT_2FA_DB_OBSERVER,phase],{env:process.env,encoding:'utf8',timeout:15000,stdio:['ignore','pipe','pipe']}));
    check(`SELECT real de segurança: ${phase}`,d.state==='PASS'&&d.password_and_other_users_unchanged&&d.readonly_limited_ownership);
  };
  const readStatus=async()=>{const r=await context.request.get(base+'/api/perfil/2fa/status');check('Status2FA consultado no Java real',r.status()===200);return(await r.json()).data;};
  const waitApi=(method,pathname)=>{const p=page.waitForResponse(r=>r.request().method()===method&&new URL(r.url()).pathname===pathname);p.catch(()=>{});return p;};
  const dismiss=async()=>{
    await page.getByText('CLIENTE-DIAGNÓSTICO',{exact:true}).waitFor({state:'hidden',timeout:15000});
    const welcome=page.getByRole('button',{name:/Ciente, Vamos lá/i});if(await welcome.isVisible())await welcome.click();
    const remind=page.getByRole('button',{name:/Lembrar em 7 dias/i});if(await remind.isVisible())await remind.click();
  };
  await open('/perfil');const initial=await readStatus();check('Estado inicial sem2FA e sem configuração pendente',initial.enabled===false&&initial.setupPending===false);observe('before');
  const security=async()=>{
    await page.getByRole('button',{name:'Proteção Avançada (2FA)',exact:true}).click();
    return page.getByRole('dialog',{name:'Proteção Avançada (2FA)',exact:true});
  };
  const readAfterProfileRefresh=async()=>{
    // Atualizar o perfil pode remontar o modal; abra novamente pela UI.
    await open('/perfil');
    return security();
  };
  let modal=await security();
  await modal.getByLabel('Senha atual',{exact:true}).fill(account.password);
  const pendingSetup=waitApi('POST','/api/perfil/2fa/configurar');await modal.getByRole('button',{name:'Gerar QR Code',exact:true}).click();
  const setupResponse=await pendingSetup;check('Configuração pelo React e senha chega ao Java200',setupResponse.status()===200);
  const setup=(await setupResponse.json()).data;check('Servidor entrega segredo Base32 e QR reais sem publicá-los na evidência',/^[A-Z2-7]+=*$/.test(setup.secret)&&setup.otpauthUri.startsWith('otpauth://totp/')&&setup.qrCodeDataUrl.startsWith('data:image/'));
  const secret=setup.secret;const descriptor=fs.openSync(privateFile,'wx',0o600);
  try{fs.writeFileSync(descriptor,JSON.stringify({actorId:account.id,secret,scope:'SYNTHETIC_LOCAL_RECOVERY_ONLY_DO_NOT_PACKAGE_OR_SHARE'}));fs.fsyncSync(descriptor);}finally{fs.closeSync(descriptor);}
  const directory=fs.openSync(path.dirname(privateFile),'r');try{fs.fsyncSync(directory);}finally{fs.closeSync(directory);}
  recordBusiness({twoFactorStage51ActorId:account.id,twoFactorStage51RecoveryReference:'private/2fa-nacional-stage51-secret.json',twoFactorStage51Restored:false});
  const fillCode=async locator=>{
    try{await locator.fill(totp(secret));}
    catch{throw new Error('Não foi possível preencher o TOTP; valor omitido.');}
  };
  await fillCode(modal.getByLabel('Código do aplicativo autenticador',{exact:true}));
  const pendingEnable=waitApi('POST','/api/perfil/2fa/ativar');await modal.getByRole('button',{name:'Confirmar e ativar',exact:true}).click();
  const enabledResponse=await pendingEnable;check('Ativação real exige código nativo e retorna200',enabledResponse.status()===200&&(await enabledResponse.json()).data.enabled===true);
  const enabled=await readStatus();check('2FA ativado sem segredo pendente',enabled.enabled===true&&enabled.setupPending===false);observe('enabled');
  modal=await readAfterProfileRefresh();
  await modal.getByRole('button',{name:'Desativar segundo fator',exact:true}).waitFor();await screenshot('ativado-sem-expor-segredo');
  await modal.getByRole('button',{name:'Fechar',exact:true}).click();await modal.waitFor({state:'hidden'});
  const logout=async tab=>{
    const pending=tab.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname==='/api/autenticacao/encerrar-sessao');pending.catch(()=>{});
    await tab.getByRole('button',{name:'Sair',exact:true}).click();check('Logout explícito confirmado no servidor',(await pending).status()===200);
    await tab.getByRole('button',{name:/Entrar no Sistema/i}).waitFor({timeout:20000});
  };
  await logout(page);
  const startChallenge=async()=>{
    await page.getByLabel(/^Matrícula$/i).fill(account.matricula);await page.getByLabel(/^Senha$/i).fill(account.password);
    const pending=waitApi('POST','/api/autenticacao/autenticar');await page.getByRole('button',{name:/Entrar no Sistema/i}).click();
    const r=await pending;const body=(await r.json()).data;
    check('Senha correta sozinha recebe202/desafio obrigatório sem sessão',r.status()===202&&body.requiresTwoFactor===true&&body.canSkipTwoFactor===false);
    await page.getByLabel('Código do autenticador',{exact:true}).waitFor();
  };
  await startChallenge();check('Desafio não emitiu cookie de autenticação',!(await context.cookies()).some(cookie=>cookie.name==='autenticacao'));observe('challenge');
  const codes=new Set(Array.from({length:9},(_,index)=>totp(secret,Date.now()+(index-4)*30000)));let invalid=0;while(codes.has(String(invalid).padStart(6,'0')))invalid++;
  await page.getByLabel('Código do autenticador',{exact:true}).fill(String(invalid).padStart(6,'0'));
  const rejected=waitApi('POST','/api/autenticacao/autenticar');await page.getByRole('button',{name:'Validar segundo fator',exact:true}).click();
  check('Código deliberadamente inválido recebe401 sem sucesso de sessão',(await rejected).status()===401&&await page.getByRole('button',{name:'Sair',exact:true}).count()===0);
  if(!await page.getByLabel('Código do autenticador',{exact:true}).count())await startChallenge();
  const loginWithCode=async()=>{
    await fillCode(page.getByLabel('Código do autenticador',{exact:true}));const pending=waitApi('POST','/api/autenticacao/autenticar');
    await page.getByRole('button',{name:'Validar segundo fator',exact:true}).click();const response=await pending;
    check('Login com TOTP nativo válido retorna200',response.status()===200);
    await page.getByRole('button',{name:'Sair',exact:true}).waitFor({timeout:20000});await dismiss();
  };
  await loginWithCode();
  const sessionResponse=await context.request.get(base+'/api/autenticacao/sessao');const session=(await sessionResponse.json()).data;
  check('Sessão real identifica a conta e verificação do segundo fator',sessionResponse.status()===200&&session.authenticated===true&&session.user.id===account.id&&session.user.twoFactorVerified===true);observe('verified');
  const cookies=(await context.cookies()).filter(cookie=>['autenticacao','id_dispositivo'].includes(cookie.name));
  check('Cookies de sessão e dispositivo sãoHttpOnly/Strict/pathraiz na origemHTTP autorizada',cookies.length===2&&cookies.every(cookie=>cookie.httpOnly&&cookie.sameSite==='Strict'&&cookie.path==='/'&&cookie.secure===(new URL(base).protocol==='https:')));
  report.cookieAttributes=cookies.map(({name,httpOnly,sameSite,path,secure})=>({name,httpOnly,sameSite,path,secure}));
  const tabB=await context.newPage();tabB.on('pageerror',error=>report.errors.push(error.message));
  try{
    await tabB.goto(base+'/perfil',{waitUntil:'networkidle'});check('Segunda aba real compartilha a sessão2FA verificada',(await tabB.locator('body').innerText()).includes(account.username));
    await logout(tabB);await page.bringToFront();await page.getByRole('button',{name:/Entrar no Sistema/i}).waitFor({timeout:10000});
    check('Logout da segunda aba remove a interface protegida da primeira',await page.getByRole('button',{name:'Sair',exact:true}).count()===0&&!(await page.locator('body').innerText()).includes(account.username));
    const protectedResponse=await context.request.get(base+'/api/usuarios/meu-perfil');check('Backend também recusa a sessão encerrada com401',protectedResponse.status()===401);observe('logged-out');
    await screenshot('logout-duas-abas');
  }finally{await tabB.close();}
  await startChallenge();await loginWithCode();await open('/perfil');
  modal=await security();
  await modal.getByLabel('Senha atual',{exact:true}).fill(account.password);await fillCode(modal.getByLabel('Código do aplicativo autenticador',{exact:true}));
  const pendingDisable=waitApi('DELETE','/api/perfil/2fa');await modal.getByRole('button',{name:'Desativar segundo fator',exact:true}).click();
  const disabledResponse=await pendingDisable;check('Restauração pelo fluxo real exige senha+código e retorna200',disabledResponse.status()===200&&(await disabledResponse.json()).data.enabled===false);
  const restored=await readStatus();check('Estado original restaurado sem2FA nem pendência',restored.enabled===false&&restored.setupPending===false);observe('restored');
  recordBusiness({twoFactorStage51Restored:true});report.scope='SYNTHETIC_2FA_REAL_UI_NATIVE_TOTP_TWO_TABS_SQL_NOT_PHYSICAL_PHONE_NOT_GLOBAL_ACCEPTANCE';
  modal=await readAfterProfileRefresh();
  await modal.getByRole('button',{name:'Gerar QR Code',exact:true}).waitFor();await screenshot('estado-original-restaurado');
},{accountName:'nacional',reuse:false});
