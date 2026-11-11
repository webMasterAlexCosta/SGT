const fs=require('node:fs'),path=require('node:path');
const {execute}=require('./ambiente-real.cjs');const {baseFrontend}=require('./contexto-fixture.cjs');
const unwrap=x=>x&&Object.hasOwn(x,'data')?x.data:x;
execute('fechamento-grade-transicao-real',async({page,context,fixture,check,report,open,confirmPassword,recordBusiness,screenshot})=>{
 const b=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json'))),base=baseFrontend();
 check('Coorte200 preservada sem job desta nova data',b.jobs200People?.length===200&&!b.jobs200GradeId);
 const date=new Date(fixture.dates.today+'T12:00:00Z');date.setUTCDate(date.getUTCDate()-2);const day=date.toISOString().slice(0,10);
 await open('/frequencia');await page.getByLabel('Data de referência da frequência',{exact:true}).fill(day);
 await page.getByRole('combobox',{name:'Lotação',exact:true}).selectOption(String(fixture.ids.lotacao));await page.getByRole('combobox',{name:'Turno',exact:true}).selectOption(String(fixture.ids.jornada));
 const contracts=unwrap(await(await context.request.get(base+'/api/contratos')).json()).filter(x=>x.ativo===true),index=contracts.findIndex(x=>x.id===b.jobs200ContratoId);
 check('Contrato exclusivo200 identificado peloID',index>=0);
 await page.getByRole('button',{name:'Selecionar contratos',exact:true}).click();const boxes=page.locator('[data-tour="freq-filters"]').getByRole('checkbox');await boxes.first().click();await boxes.nth(index+1).check();await page.getByRole('button',{name:'Selecionar contratos',exact:true}).click();
 const firstPerson='SINTETICO JOBS200 '+(b.jobs200CorpusMarker||'FINAL03')+' 001';
 await page.getByText(firstPerson,{exact:true}).waitFor({timeout:15000});
 await page.getByText('Pendente',{exact:true}).first().waitFor({timeout:15000});
 check('Página começa com cinco linhas ainda não salvas',await page.getByText('Pendente',{exact:true}).count()===5&&await page.getByText('Salvo',{exact:true}).count()===0);
 const panel=page.getByRole('region',{name:'Fechamento do escopo',exact:true}),launch=panel.getByRole('button',{name:'Iniciar fechamento',exact:true});await launch.click({trial:true,timeout:30000});
 await page.getByLabel('Justificativa da retificação',{exact:true}).fill('Conferência coletiva sintética200: verificar reconciliação automática da grade após encerramento.');
 const pending=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/frequencias/fechamentos'&&r.request().method()==='POST');pending.catch(()=>{});
 await launch.click();await confirmPassword();const created=await pending,started=performance.now(),dto=unwrap(await created.json());
 check('UI recebe202 para novo job real',created.status()===202&&dto.id>0);recordBusiness({jobs200GradeId:dto.id,jobs200GradeData:day});
 const sent=created.request().postDataJSON();
 check('Java recebeu exatamente o contrato200, justificativa e nenhuma edição individual',sent?.contratoIds?.length===1
   && sent.contratoIds[0]===b.jobs200ContratoId && sent?.alteracoes?.length===0 && typeof sent?.chaveIdempotencia==='string'
   && sent?.justificativaRetificacao?.includes('sintética200'));
 await page.getByLabel('Senha de Acesso',{exact:true}).waitFor({state:'hidden'});
 const draft='RASCUNHO LOCAL SINTETICO: preservar anotação enquanto o job confirma a identidade oficial.';
 await page.getByTitle('Marcar Folga/Descanso para '+firstPerson,{exact:true}).click();
 const observation=page.getByPlaceholder('Observação (Opcional)...',{exact:true}).first();await observation.fill(draft);
 check('Rascunho editado na linha ainda pendente durante o job',await page.getByText('Pendente',{exact:true}).count()===5&&await observation.inputValue()===draft);
 let final;const observations=[];
 while(performance.now()-started<120000){const r=await context.request.get(base+'/api/frequencias/fechamentos/'+dto.id,{timeout:10000}),job=unwrap(await r.json());check('GET do job real responde200',r.status()===200);observations.push(job);if(['CONCLUIDO','CANCELADO','ERRO'].includes(job.status)){final=job;break;}await page.waitForTimeout(400);}
 report.job={milliseconds:performance.now()-started,observations};
 check('Conclui200 únicos no prazo físico120s',report.job.milliseconds<=120000&&final?.status==='CONCLUIDO'&&final.id===dto.id&&final.totalPrevisto===200&&final.totalProcessado===200&&final.totalInserido===200&&final.totalAtualizado===0&&final.totalPreservado===0);
 await panel.getByText('200 / 200',{exact:true}).waitFor({timeout:15000});
 check('Progresso UI100% pertence ao mesmo job',await panel.getByText('#'+dto.id,{exact:true}).isVisible()&&await panel.getByLabel('Progresso do fechamento: 100%',{exact:true}).isVisible());
 await page.getByText('Salvo',{exact:true}).first().waitFor({timeout:15000});
 check('Sem refresh manual: cinco Salvos, sem Pendentes nem vínculo provisório',await page.getByText('Salvo',{exact:true}).count()===5&&await page.getByText('Pendente',{exact:true}).count()===0&&!(await page.locator('body').innerText()).includes('Associação será conferida ao salvar'));
 check('Recarga conserva rascunho sem gravá-lo automaticamente',await page.getByPlaceholder('Observação (Opcional)...',{exact:true}).first().inputValue()===draft&&!report.http.some(x=>x.method==='POST'&&['/api/frequencias','/api/frequencias/lote'].includes(x.path)));
 recordBusiness({jobs200GradeConcluido:true});report.conditions={contract:b.jobs200ContratoId,date:day,job:dto.id,population:200,noManualRefresh:true,noMock:true,noSQLDML:true};report.scope='AUXILIARY_GRID_TERMINAL_TRANSITION_NOT_NEW_ORIGINAL_DRIVER';
 await screenshot('concluido-grade-reconciliada');
},{accountName:'gestor_local',reuse:false});
