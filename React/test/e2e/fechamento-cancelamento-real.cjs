const fs=require('node:fs'),path=require('node:path');const {spawn}=require('node:child_process');const readline=require('node:readline');
const {execute}=require('./ambiente-real.cjs');const {baseFrontend}=require('./contexto-fixture.cjs');
const unwrap=x=>x&&Object.hasOwn(x,'data')?x.data:x;
execute('fechamento-cancelamento-real',async({page,context,fixture,check,report,open,confirmPassword,recordBusiness,screenshot})=>{
 const b=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json')));const base=baseFrontend();
 const runtime=JSON.parse(fs.readFileSync(process.env.SGT_E2E_MANIFEST));
 check('MesmoJARsemVite e configuração10 para200pessoas',runtime.mode==='jar'&&!runtime.vite_pid&&runtime.resource_profile.closure_chunk_size===10&&b.jobs200People.length===200&&!b.jobs200CancelId);
 report.scope='JOB200_CANCEL_REPROCESS_AUXILIARY_NOT_ORIGINAL_DRIVER';
 const date=new Date(fixture.dates.today+'T12:00:00Z');date.setUTCDate(date.getUTCDate()-3);const day=date.toISOString().slice(0,10);
 await open('/frequencia');await page.getByLabel('Data de referência da frequência',{exact:true}).fill(day);
 await page.getByRole('combobox',{name:'Lotação',exact:true}).selectOption(String(fixture.ids.lotacao));await page.getByRole('combobox',{name:'Turno',exact:true}).selectOption(String(fixture.ids.jornada));
 const list=unwrap(await(await context.request.get(base+'/api/contratos')).json()),active=list.filter(x=>x.ativo===true),index=active.findIndex(x=>x.id===b.jobs200ContratoId);
 await page.getByRole('button',{name:'Selecionar contratos',exact:true}).click();const boxes=page.locator('[data-tour="freq-filters"]').getByRole('checkbox');check('Filtro localiza contrato200peloID',index>=0&&await boxes.count()===active.length+1);
 await boxes.first().click();await boxes.nth(index+1).check();check('Somente contrato200selecionado',await boxes.evaluateAll(nodes=>nodes.filter(n=>n.checked).length)===1&&!await boxes.first().isChecked());await page.getByRole('button',{name:'Selecionar contratos',exact:true}).click();
 const panel=page.getByRole('region',{name:'Fechamento do escopo',exact:true}),launch=panel.getByRole('button',{name:'Iniciar fechamento',exact:true});await launch.click({trial:true,timeout:30000});
 await page.getByLabel('Justificativa da retificação',{exact:true}).fill('Conferência coletiva sintética200: cancelamento com prefixo confirmado e reprocessamento sem duplicação.');
 const waitApi=(endpoint)=>{const p=page.waitForResponse(r=>new URL(r.url()).pathname===endpoint&&r.request().method()==='POST');p.catch(()=>{});return p;};
 const pending=waitApi('/api/frequencias/fechamentos');await launch.click();await confirmPassword();const created=await pending,startedAt=performance.now(),dto=unwrap(await created.json());
 check('UIcria202jobcoletivo novo',created.status()===202&&dto.id>0);recordBusiness({jobs200CancelId:dto.id,jobs200CancelData:day});await page.getByLabel('Senha de Acesso',{exact:true}).waitFor({state:'hidden'});
 const barrier=spawn('python3',[process.env.SGT_JOB_PREFIX_BARRIER],{stdio:['pipe','pipe','inherit']});const messages=[],waiters=[];let ready=false,released=false;
 readline.createInterface({input:barrier.stdout}).on('line',line=>{const value=JSON.parse(line);if(waiters.length)waiters.shift().resolve(value);else messages.push(value);});
 barrier.on('exit',code=>{while(waiters.length)waiters.shift().reject(new Error('Barreira própria encerrou antes da mensagem esperada: '+code));});
 const receive=()=>messages.length?Promise.resolve(messages.shift()):new Promise((resolve,reject)=>waiters.push({resolve,reject}));
 const send=command=>{if(barrier.exitCode!==null)throw new Error('Barreira já encerrada.');barrier.stdin.write(JSON.stringify({command})+'\n');return receive();};
 const terminal=async(id,phase,started)=>{
  const observations=[];let last;
  while(performance.now()-started<120000){const r=await context.request.get(base+'/api/frequencias/fechamentos/'+id,{timeout:10000});last=unwrap(await r.json());check(`GETreal200nafase${phase}`,r.status()===200);observations.push(last);if(['CONCLUIDO','ERRO','CANCELADO'].includes(last.status))break;await page.waitForTimeout(400);}
  report[phase]={milliseconds:performance.now()-started,observations};
  check(`Fase ${phase} alcançou estado terminal dentro do prazo físico120s`,report[phase].milliseconds<=120000&&['CONCLUIDO','ERRO','CANCELADO'].includes(last?.status));return last;
 };
 const fresh=b.jobs200CorpusMarker==='FINAL07';
 const priorFacts=value=>value.previous_facts_preserved && value.previous_current_contract_rows===(fresh?200:600)
   && (!fresh || (value.separate_prior_corpus_preserved&&value.separate_prior_corpus_rows===1000));
 try{
  const held=await receive();ready=true;check('Prefixo confirmado parcial e worker em espera física',held.ready&&held.physical_wait&&held.prefix>0&&held.prefix<200&&held.planned===200);report.physicalPrefix=held;
  const cancelPending=waitApi(`/api/frequencias/fechamentos/${dto.id}/cancelamento`);await panel.getByRole('button',{name:'Solicitar cancelamento',exact:true}).click();const canceled=await cancelPending,ack=unwrap(await canceled.json());
  check('CancelamentoUI202confirmado no mesmojob',canceled.status()===202&&ack.id===dto.id&&ack.cancelamentoSolicitado===true);
  check('Cancelamento persistido com prefixo ainda protegido',(await send('observe-cancel')).cancel_confirmed===true);
  check('Barreira física liberada porrollback',(await send('release')).released===true);released=true;
  const cancelled=await terminal(dto.id,'cancelamento',startedAt);check('TerminalCANCELADO preserva prefixo e tentativa1',cancelled.status==='CANCELADO'&&cancelled.tentativa===1&&cancelled.totalPrevisto===200&&cancelled.totalProcessado>=held.prefix&&cancelled.totalProcessado<200);
  const cancelDb=await send('cancelled');check('SELECT confere prefixo, frequências e eventos anteriores',cancelDb.verified&&cancelDb.rows===cancelled.totalProcessado&&priorFacts(cancelDb)&&cancelDb.prefix_preserved);recordBusiness({jobs200CancelPrefix:cancelDb.rows});
  await panel.getByRole('button',{name:'Reprocessar',exact:true}).waitFor({timeout:15000});await screenshot('cancelado-com-prefixo');
  const retryPending=waitApi(`/api/frequencias/fechamentos/${dto.id}/reprocessamento`);await panel.getByRole('button',{name:'Reprocessar',exact:true}).click();await confirmPassword();const retry=await retryPending,retryStarted=performance.now(),retryDto=unwrap(await retry.json());
  check('ReprocessamentoUI202reutilizaID e incrementa tentativa',retry.status()===202&&retryDto.id===dto.id&&retryDto.tentativa===2);recordBusiness({jobs200ReprocessamentoTentativa:retryDto.tentativa});
  const final=await terminal(dto.id,'reprocessamento',retryStarted),remaining=200-cancelDb.rows;
  check('Tentativa2conclui somenteosfaltantes',final.status==='CONCLUIDO'&&final.id===dto.id&&final.tentativa===2&&final.totalPrevisto===remaining&&final.totalProcessado===remaining&&final.totalInserido===remaining&&final.totalAtualizado===0&&final.totalPreservado===0&&final.percentual===100);
  const finalDb=await send('reprocessed');check('SELECT reconcilia200 únicos, prefixo imutável e os fatos anteriores',finalDb.verified&&finalDb.rows===200&&finalDb.processed===remaining&&priorFacts(finalDb)&&finalDb.prefix_preserved);
  await panel.getByText(`${remaining} / ${remaining}`,{exact:true}).waitFor({timeout:15000});check('DashboardmostramesmoID, tentativa e100%',await panel.getByText('#'+dto.id,{exact:true}).isVisible()&&await panel.getByLabel('Progresso do fechamento: 100%',{exact:true}).isVisible());
  recordBusiness({jobs200CancelReprocessamentoConcluido:true});report.conditions={job:dto.id,date:day,population:200,chunk:10,cancelledPrefix:cancelDb.rows,attempt2:remaining,combined:cancelDb.rows+remaining,deadlinePerPhaseSeconds:120,physicalClock:true,noSQLDML:true,noMock:true};
  report.limits=['Chunk10éconfiguraçãoexistente paraobservarprefixoparcial; população200e120sporfasemantidos. NãoérecuperaçãoemdoisexecutoresnemreinícioPostgreSQL/Valkey/LAN.'];await screenshot('reprocessado-com-prefixo-preservado');
 }finally{
  if(barrier.exitCode===null){
   if(ready&&!released){try{await send('release');}catch{}}
   barrier.stdin.end();if(!ready)barrier.kill('SIGTERM');
   if(barrier.exitCode===null)await new Promise(resolve=>barrier.once('exit',resolve));
  }
 }
},{accountName:'gestor_local',reuse:false});
