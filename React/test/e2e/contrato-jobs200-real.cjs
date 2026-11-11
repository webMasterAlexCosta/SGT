// Jornada causal de contrato e preparação isolada de capacidade200, sem DDL/JPA direto.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {execute}=require('./ambiente-real.cjs');
const {baseFrontend}=require('./contexto-fixture.cjs');
const business=()=>JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json')));
const unwrap=x=>x&&Object.hasOwn(x,'data')?x.data:x;
execute('contrato-jobs200-real',async({page,context,fixture,check,report,open,confirmPassword,recordBusiness,screenshot})=>{
 const base=baseFrontend(),fresh=path.basename(process.env.SGT_TEST_RUNTIME)==='runtime-frontend55-01';
 const marker=fresh?'FINAL07':'FINAL03',number='SINTETICO-E2E-JOBS200-'+marker;
 const start=new Date(fixture.dates.today+'T12:00:00Z');start.setUTCDate(start.getUTCDate()-(fresh?9:4));const date=start.toISOString().slice(0,10);
 // Autenticar pela própria interface antes das leituras privadas.
 await open('/contratos');
 const originalResponse=await context.request.get(base+'/api/contratos/'+fixture.ids.contrato_rascunho);
 const original=unwrap(await originalResponse.json());check('Contrato original autorizado, preservado e publicado',originalResponse.status()===200&&original.estadoNegocial==='PUBLICADO');
 const providersResponse=await context.request.get(base+'/api/fornecedores');const providers=unwrap(await providersResponse.json());
 const provider=providers.find(x=>x.id===original.fornecedorId);check('Empresa existente ativa observada pela API',providersResponse.status()===200&&provider?.ativo===true);
 let b=business();
 if(!b.jobs200ContratoId){
  const list=unwrap(await(await context.request.get(base+'/api/contratos')).json());check('Intenção exclusiva não existe antes de criar',!list.some(x=>x.numeroContrato===number));
  await page.getByRole('button',{name:'Novo contrato',exact:true}).click();const form=page.getByRole('dialog',{name:'Novo rascunho',exact:true});
  await form.getByLabel('Número do contrato',{exact:true}).fill(number);await form.getByLabel('Ano',{exact:true}).fill(fixture.dates.today.slice(0,4));
  await form.getByRole('combobox',{name:'Tipo de contratação',exact:true}).selectOption(String(fixture.ids.tipo));
  await form.getByLabel('Efetivo total previsto',{exact:true}).fill('200');
  await form.getByRole('textbox',{name:/^Descrição/}).fill('SINTETICO: contrato separado para fechamento200, admissão e persistência; sem valor administrativo.');
  await form.getByLabel('CNPJ da empresa',{exact:true}).fill(provider.cnpj);await form.getByLabel('Razão social',{exact:true}).fill(provider.razaoSocial);
  await form.getByLabel('Nome fantasia',{exact:true}).fill(provider.nomeFantasia||'SINTETICO JOBS200');
  await form.getByLabel('Data de início',{exact:true}).fill(date);await form.getByLabel('Data de término',{exact:true}).fill(original.dataFim);
  const pending=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/contratos'&&r.request().method()==='POST');pending.catch(()=>{});
  await form.getByRole('button',{name:'Criar rascunho',exact:true}).click();await confirmPassword();const r=await pending,d=unwrap(await r.json());
  check('Rascunho real201, capacidade200 e mesma Gestão',r.status()===201&&d.id>0&&d.efetivoPrevisto===200&&d.gestaoLocalId===fixture.ids.gestao_origem&&d.estadoNegocial==='RASCUNHO');
  recordBusiness({jobs200ContratoId:d.id,jobs200ContratoNumero:number,jobs200DataInicio:date,jobs200FornecedorId:provider.id,jobs200CorpusMarker:marker});await form.waitFor({state:'hidden'});b=business();
 }
 const cid=b.jobs200ContratoId;check('Corpus separado preserva identidade original',cid!==original.id&&b.jobs200DataInicio===date&&b.jobs200ContratoNumero===number&&(!fresh||b.jobs200CorpusMarker===marker));
 let actual=unwrap(await(await context.request.get(base+'/api/contratos/'+cid)).json());
 if(!b.jobs200Associado&&actual.fornecedorId===provider.id){
  if(fresh)throw new Error('A associação já existe no corpus novo sem recibo durável; reconciliar a tentativa própria antes de repetir.');
  const prior=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'../browser-jar-contrato-jobs200-01/browser/contrato-jobs200-real.json')));
  check('Retomada preserva PUT200 da tentativa anterior e GET confirma o vínculo real',prior.passed===false&&prior.failure==='Associação UI200 usa empresa existente'&&prior.http.some(x=>x.method==='PUT'&&x.path===`/api/contratos/${cid}/fornecedor/${provider.id}`&&x.status===200)&&actual.estadoNegocial==='RASCUNHO');
  recordBusiness({jobs200Associado:true,jobs200AssociacaoTentativaAnterior:'browser-jar-contrato-jobs200-01'});b=business();
 }
 if(!b.jobs200Associado){
  check('Rascunho ainda sem fornecedor antes da associação',actual.estadoNegocial==='RASCUNHO'&&actual.fornecedorId==null);
  await open('/empresas-contratadas');await page.getByText('Carregando empresas...',{exact:true}).waitFor({state:'hidden'});
  await page.getByRole('searchbox',{name:'Pesquisar empresas',exact:true}).fill(provider.razaoSocial);
  const row=page.getByRole('row').filter({has:page.getByRole('cell',{name:provider.razaoSocial,exact:true})});await row.getByRole('button',{name:'Vincular',exact:true}).click();
  const region=page.getByRole('region',{name:'Vínculo pendente da empresa cadastrada',exact:true});
  await region.getByRole('combobox',{name:'Contrato para vincular a empresa cadastrada',exact:true}).selectOption(String(cid));
  const pending=page.waitForResponse(r=>new URL(r.url()).pathname===`/api/contratos/${cid}/fornecedor/${provider.id}`&&r.request().method()==='PUT');pending.catch(()=>{});
  await region.getByRole('button',{name:'Vincular empresa cadastrada',exact:true}).click();await confirmPassword();const r=await pending,d=unwrap(await r.json());
  check('Associação UI200 usa empresa existente',r.status()===200&&d.contratoId===cid&&d.fornecedor?.id===provider.id);recordBusiness({jobs200Associado:true});
 }
 await open('/contratos');await page.getByRole('searchbox',{name:'Pesquisar',exact:true}).fill(number);
 await page.getByRole('button',{name:'Abrir ciclo de vida do contrato '+number,exact:true}).click();const detail=page.getByRole('dialog',{name:number+'/'+fixture.dates.today.slice(0,4),exact:true});await detail.waitFor();
 b=business();
 if(!b.jobs200ComposicaoId){
  const form=detail.locator('form').filter({has:page.getByRole('heading',{name:'Adicionar composição ao rascunho',exact:true})});
  await form.getByLabel(/^Lotação/).selectOption(String(fixture.ids.lotacao));await form.getByLabel(/^Turno\/Jornada/).selectOption(String(fixture.ids.jornada));await form.getByLabel(/^Cargo/).selectOption(String(fixture.ids.cargo));
  await form.getByLabel(/^Efetivo previsto/).fill('200');await form.getByLabel('Início de vigência',{exact:true}).fill(date);await form.getByLabel('Fim de vigência',{exact:true}).fill(original.dataFim);
  const pending=page.waitForResponse(r=>new URL(r.url()).pathname===`/api/contratos/${cid}/composicoes`&&r.request().method()==='POST');pending.catch(()=>{});
  await form.getByRole('button',{name:'Salvar composição',exact:true}).click();await confirmPassword();const r=await pending,d=unwrap(await r.json());
  check('Composição UI201 com200 e par autorizado',r.status()===201&&d.id>0&&d.efetivoPrevisto===200&&d.lotacaoId===fixture.ids.lotacao&&d.jornadaId===fixture.ids.jornada);
  recordBusiness({jobs200ComposicaoId:d.id});await page.getByLabel('Senha de Acesso',{exact:true}).waitFor({state:'hidden'});
 }
 b=business();
 if(!b.jobs200Publicado){
  await detail.getByRole('button',{name:'Publicar',exact:true}).click();const modal=page.getByRole('dialog',{name:'Publicar contrato',exact:true});
  await modal.getByLabel(/Data da publicação/).fill(date);await modal.getByLabel(/Referência SEI/).fill('SINTETICO-JOBS200-'+marker);await modal.getByLabel(/Justificativa/).fill('Publicação sintética causal para200 vínculos, registrada agora com data efetiva declarada; sem modificar fotografias anteriores.');
  check('Publicação exige prévia antes da senha',await modal.getByRole('button',{name:/Continuar e confirmar senha/}).isDisabled());
  await modal.getByRole('button',{name:'Gerar prévia',exact:true}).click();await modal.getByRole('heading',{name:'Prévia de impactos'}).waitFor();await modal.getByLabel(/Confirmo os impactos/).check();
  const pending=page.waitForResponse(r=>new URL(r.url()).pathname===`/api/contratos/${cid}/publicar`&&r.request().method()==='POST');pending.catch(()=>{});
  await modal.getByRole('button',{name:/Continuar e confirmar senha/}).click();await confirmPassword();const r=await pending,d=unwrap(await r.json());
  check('Publicação UI200 retém base original200 e fornecedor',r.status()===200&&d.estadoNegocial==='PUBLICADO'&&d.efetivoOriginalPublicado===200&&d.fornecedorId===provider.id);
  recordBusiness({jobs200Publicado:true});await modal.waitFor({state:'hidden'});
 }
 actual=unwrap(await(await context.request.get(base+'/api/contratos/'+cid)).json());const versions=unwrap(await(await context.request.get(base+`/api/contratos/${cid}/versoes`)).json());
 check('GET e fotografia originais publicados com200',actual.estadoNegocial==='PUBLICADO'&&actual.efetivoOriginalPublicado===200&&versions.length===1&&versions[0].efetivoPrevisto===200);
 check('UI remove edição simples após publicação',await detail.getByRole('button',{name:'Editar rascunho',exact:true}).count()===0&&await detail.getByRole('heading',{name:'Adicionar composição ao rascunho',exact:true}).count()===0);
 const csrf=unwrap(await(await context.request.get(base+'/api/autenticacao/csrf')).json());const denied=await context.request.put(base+'/api/contratos/'+cid,{headers:{[csrf.headerName]:csrf.token,'X-User-Password':fixture.accounts.gestor_local.password,Origin:base},data:{...actual,descricao:'TENTATIVA SINTETICA RECUSADA'}});
 check('API também recusa PUT simples pós-publicação409',denied.status()===409);report.http.push({method:'PUT',path:'/api/contratos/'+cid,status:denied.status(),driver:'APIRequestContext real — bloqueio negocial'});
 const after=unwrap(await(await context.request.get(base+'/api/contratos/'+cid)).json());check('Recusa não altera descrição ou versão',after.descricao===actual.descricao&&after.version===actual.version);
 const docs=unwrap(await(await context.request.get(base+`/api/contratos/${cid}/documentos`)).json());check('Fonte documental da publicação criada pelo Java',docs.some(d=>d.tipoFato==='CONTRATO_ORIGINAL'&&d.estado==='PUBLICADO'));
 recordBusiness({jobs200ContratoPronto:true,jobs200VersaoOriginalId:versions[0].id,jobs200VersaoOriginalSha256:crypto.createHash('sha256').update(JSON.stringify(versions[0])).digest('hex')});
 report.conditions={contract:cid,composition:business().jobs200ComposicaoId,capacity:200,start:date,recordedNowNotBackdatedKnowledge:true,no_DDL_DML:true,otherCorpusUnchanged:true};
 report.limits=['Contrato completo pela UI, bloqueio adicional por API real; fonte SEI automática sem novo anexo. Massa de pessoas/job200 ainda não executados.'];await screenshot('contrato-publicado-e-bloqueado');
},{accountName:'gestor_local',reuse:false});
