const fs=require('node:fs'),path=require('node:path');
const {execute}=require('./ambiente-real.cjs');const {baseFrontend}=require('./contexto-fixture.cjs');
const unwrap=x=>x&&Object.hasOwn(x,'data')?x.data:x;
const controlCase=process.env.SGT_JOBS200_CONTROL_CASE||'c1';if(!['c1','normaljit','jaronly'].includes(controlCase))throw new Error('Caso decontrole não autorizado.');
const prefix=controlCase==='jaronly'?'jobs200StandaloneControle':controlCase==='normaljit'?'jobs200NormalJitControle':'jobs200Controle';
execute('fechamento-jobs200-real',async({page,context,fixture,check,report,open,confirmPassword,recordBusiness,screenshot})=>{
 const b=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json')));const base=baseFrontend();
 check('Massa causal200 preparada sem operação incerta',b.jobs200ContratoPronto&&b.jobs200People?.length===200&&!b.jobs200PendingIndex&&!b[prefix+'Id']);
 const date=new Date(fixture.dates.today+'T12:00:00Z');date.setUTCDate(date.getUTCDate()-(controlCase==='jaronly'?0:controlCase==='normaljit'?4:1));const day=date.toISOString().slice(0,10);
 await open('/frequencia');await page.getByLabel('Data de referência da frequência',{exact:true}).fill(day);
 await page.getByRole('combobox',{name:'Lotação',exact:true}).selectOption(String(fixture.ids.lotacao));await page.getByRole('combobox',{name:'Turno',exact:true}).selectOption(String(fixture.ids.jornada));
 const list=unwrap(await(await context.request.get(base+'/api/contratos')).json()),active=list.filter(x=>x.ativo===true),index=active.findIndex(x=>x.id===b.jobs200ContratoId);
 await page.getByRole('button',{name:'Selecionar contratos',exact:true}).click();const boxes=page.locator('[data-tour="freq-filters"]').getByRole('checkbox');
 check('Filtro identifica contrato200 pelo ID canônico',index>=0&&await boxes.count()===active.length+1);
 // O cabeçalho restaura[]="todos"; os checkboxes individuais começam desmarcados.
 await boxes.first().click();await boxes.nth(index+1).check();
 check('Somente o contrato200 está selecionado explicitamente',!await boxes.first().isChecked()&&await boxes.nth(index+1).isChecked()&&await boxes.evaluateAll(nodes=>nodes.filter(node=>node.checked).length)===1);
 await page.getByRole('button',{name:'Selecionar contratos',exact:true}).click();
 const reason='Conferência coletiva sintética dos200 vínculos do contrato exclusivo; sem alterar o corpus anterior.';
 await page.getByLabel('Justificativa da retificação',{exact:true}).fill(reason);
 const panel=page.getByRole('region',{name:'Fechamento do escopo',exact:true});const launch=panel.getByRole('button',{name:'Iniciar fechamento',exact:true});await launch.waitFor();
 await launch.click({trial:true,timeout:30000});
 check('UI oferece fechamento do recorte passado e justificativa explícita',await launch.isEnabled()&&await page.getByLabel('Justificativa da retificação',{exact:true}).inputValue()===reason);
 const pending=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/frequencias/fechamentos'&&r.request().method()==='POST');pending.catch(()=>{});
 await launch.click();await confirmPassword();const r=await pending,envelope=await r.json(),dto=unwrap(envelope);const payload=r.request().postDataJSON();
 report.creation={status:r.status(),code:envelope.code||null,message:envelope.message||null,payload};
 check('Java aceita202 o fechamento gerencial legítimo solicitado pela UI',r.status()===202&&dto.id>0,{status:r.status(),code:envelope.code||null,message:envelope.message||null});
 recordBusiness({[prefix+'Id']:dto.id,[prefix+'Data']:day});
 check('Comando coletivo preserva justificativa, chave e escopo200',payload.justificativaRetificacao===reason&&typeof payload.chaveIdempotencia==='string'&&payload.chaveIdempotencia.length>=8&&payload.contratoIds.length===1&&payload.contratoIds[0]===b.jobs200ContratoId&&payload.alteracoes.length===0);
 const observations=[];let final;const deadline=Date.now()+120000;
 while(Date.now()<deadline){const read=await context.request.get(base+'/api/frequencias/fechamentos/'+dto.id);const job=unwrap(await read.json());check('Consulta real do job respondeu200',read.status()===200);observations.push(job);
  if(['CONCLUIDO','ERRO','CANCELADO'].includes(job.status)){final=job;break;}await page.waitForTimeout(400);}
 report.progress=observations;check('Job real conclui200/200 sem erro',final?.status==='CONCLUIDO'&&final.totalPrevisto===200&&final.totalProcessado===200&&final.totalInserido===200&&final.totalAtualizado===0&&final.totalPreservado===0&&final.percentual===100);
 await panel.getByText('200 / 200',{exact:true}).waitFor({timeout:15000});check('Dashboard acompanha o mesmo ID e progresso100%',await panel.getByText('#'+dto.id,{exact:true}).isVisible()&&await panel.getByLabel('Progresso do fechamento: 100%',{exact:true}).isVisible());
 recordBusiness({[prefix+'Concluido']:true});report.conditions={contract:b.jobs200ContratoId,people:200,job:dto.id,date:day,controlCase,physicalClock:true,noProductTimerChanged:true};
 report.limits=['Controle200/200; cancelamento com prefixo e reprocessamento constituem lote separado. Não éLAN nem reinício.'];await screenshot('concluido-200-de-200');
},{accountName:'gestor_local',reuse:false});
