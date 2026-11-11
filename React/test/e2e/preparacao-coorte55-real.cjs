// Preparação de nova cadeia causal por UI/API; não é jornada adicional nem aceite.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {execute}=require('./ambiente-real.cjs');const {baseFrontend}=require('./contexto-fixture.cjs');
const unwrap=x=>x&&Object.hasOwn(x,'data')?x.data:x;
execute('preparacao-coorte55-real',async({page,context,fixture,check,report,open,confirmPassword,recordBusiness,screenshot})=>{
 const runtime=process.env.SGT_TEST_RUNTIME,campaign=path.dirname(runtime),next=path.join(campaign,'runtime-frontend55-01'),base=baseFrontend();
 const business=()=>JSON.parse(fs.readFileSync(path.join(runtime,'private/e2e-business.json')));
 const hash=file=>crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
 check('Nova fixture não existe e nenhuma operação incerta é repetida',!fs.existsSync(path.join(next,'private/e2e-fixture.json'))&&!business().cohort55Pending);
 const current=JSON.parse(fs.readFileSync(process.env.SGT_E2E_MANIFEST));
 check('JAR05 e banco nativo isolado comprovados pelo supervisor',current.mode==='jar'&&current.java.jar_sha256==='ff4b99ce17804775e9e8899d1d6ad5105b7408c15b3e32ac7b829a12b5264704'&&current.database.version_num===180006&&current.database.role_limited&&current.database.database_owned_by_role&&current.database.all_relations_owned);
 const today=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
 const later=new Date(today+'T12:00:00Z');later.setUTCDate(later.getUTCDate()+10);const future=later.toISOString().slice(0,10);
 const get=async endpoint=>{const r=await context.request.get(base+endpoint);check('GET canônico da preparação '+endpoint,r.status()===200);return unwrap(await r.json());};
 await open('/contratos');const providers=await get('/api/fornecedores');check('Catálogo real contém a empresa peloID observado',Array.isArray(providers)&&providers.some(x=>x.id===fixture.ids.fornecedor));
 const provider=providers.find(x=>x.id===fixture.ids.fornecedor),old=await get('/api/contratos/'+fixture.ids.contrato_rascunho);
 const end=old.dataFim;check('Fornecedor existente e vigência suficiente, sem alterar corpus anterior',provider.id===fixture.ids.fornecedor&&end>future&&old.estadoNegocial==='PUBLICADO');
 const wait=(endpoint,method)=>{const p=page.waitForResponse(r=>new URL(r.url()).pathname===endpoint&&r.request().method()===method);p.catch(()=>{});return p;};
 const create=async(kind,number,start)=>{
  const saved=business()[kind+'Id'];if(saved){const actual=await get('/api/contratos/'+saved);check('Retomada apenas lê rascunho já observado',actual.numeroContrato===number&&actual.estadoNegocial==='RASCUNHO');return actual;}
  await open('/contratos');const all=await get('/api/contratos');check('Número novo sem duplicar contrato',!all.some(x=>x.numeroContrato===number));
  await page.getByRole('button',{name:'Novo contrato',exact:true}).click();const form=page.getByRole('dialog',{name:'Novo rascunho',exact:true});
  await form.getByLabel('Número do contrato',{exact:true}).fill(number);await form.getByLabel('Ano',{exact:true}).fill(today.slice(0,4));
  await form.getByRole('combobox',{name:'Tipo de contratação',exact:true}).selectOption(String(fixture.ids.tipo));
  await form.getByLabel('Efetivo total previsto',{exact:true}).fill('5');await form.getByRole('textbox',{name:/^Descrição/}).fill('SINTETICO COORTE55: cadeia nova de publicação, vínculo e histórico; sem valor administrativo.');
  await form.getByLabel('CNPJ da empresa',{exact:true}).fill(provider.cnpj);await form.getByLabel('Razão social',{exact:true}).fill(provider.razaoSocial);await form.getByLabel('Nome fantasia',{exact:true}).fill(provider.nomeFantasia||'SINTETICA COORTE55');
  await form.getByLabel('Data de início',{exact:true}).fill(start);await form.getByLabel('Data de término',{exact:true}).fill(end);
  recordBusiness({cohort55Pending:kind+':CREATE'});const pending=wait('/api/contratos','POST');await form.getByRole('button',{name:'Criar rascunho',exact:true}).click();await confirmPassword();const r=await pending,dto=unwrap(await r.json());
  check('Rascunho201 com escopo/capacidade/datas exatos',r.status()===201&&dto.id>0&&dto.numeroContrato===number&&dto.efetivoPrevisto===5&&dto.gestaoLocalId===fixture.ids.gestao_origem&&dto.dataInicio===start&&dto.estadoNegocial==='RASCUNHO');
  recordBusiness({[kind+'Id']:dto.id,[kind+'Number']:number,cohort55Pending:null});await form.waitFor({state:'hidden'});await page.getByLabel('Senha de Acesso',{exact:true}).waitFor({state:'hidden'});return dto;
 };
 let main=await create('cohort55Main','SINTETICO-E2E-COORTE55-PRINCIPAL',today);
 if(main.fornecedorId!==provider.id){
  check('Associação parte de rascunho ainda sem empresa',main.fornecedorId==null);
  await open('/empresas-contratadas');await page.getByText('Carregando empresas...',{exact:true}).waitFor({state:'hidden'});await page.getByRole('searchbox',{name:'Pesquisar empresas',exact:true}).fill(provider.razaoSocial);
  const row=page.getByRole('row').filter({has:page.getByRole('cell',{name:provider.razaoSocial,exact:true})});await row.getByRole('button',{name:'Vincular',exact:true}).click();const region=page.getByRole('region',{name:'Vínculo pendente da empresa cadastrada',exact:true});
  await region.getByRole('combobox',{name:'Contrato para vincular a empresa cadastrada',exact:true}).selectOption(String(main.id));
  recordBusiness({cohort55Pending:'ASSOCIATE_MAIN'});const pending=wait(`/api/contratos/${main.id}/fornecedor/${provider.id}`,'PUT');await region.getByRole('button',{name:'Vincular empresa cadastrada',exact:true}).click();await confirmPassword();const r=await pending,dto=unwrap(await r.json());
  check('PUT200 associa IDs reais pelaUI',r.status()===200&&dto.contratoId===main.id&&dto.fornecedor?.id===provider.id);recordBusiness({cohort55MainAssociated:true,cohort55Pending:null});await page.getByLabel('Senha de Acesso',{exact:true}).waitFor({state:'hidden'});
 }
 const compose=async(kind,contract,start)=>{
  if(business()[kind+'CompositionId'])return business()[kind+'CompositionId'];
  await open('/contratos');await page.getByRole('searchbox',{name:'Pesquisar',exact:true}).fill(contract.numeroContrato);await page.getByRole('button',{name:'Abrir ciclo de vida do contrato '+contract.numeroContrato,exact:true}).click();
  const detail=page.getByRole('dialog',{name:contract.numeroContrato+'/'+today.slice(0,4),exact:true}),form=detail.locator('form').filter({has:page.getByRole('heading',{name:'Adicionar composição ao rascunho',exact:true})});
  await form.getByLabel(/^Lotação/).selectOption(String(fixture.ids.lotacao));await form.getByLabel(/^Turno\/Jornada/).selectOption(String(fixture.ids.jornada));await form.getByLabel(/^Cargo/).selectOption(String(fixture.ids.cargo));await form.getByLabel(/^Efetivo previsto/).fill('5');
  await form.getByLabel('Início de vigência',{exact:true}).fill(start);await form.getByLabel('Fim de vigência',{exact:true}).fill(end);
  recordBusiness({cohort55Pending:kind+':COMPOSITION'});const pending=wait(`/api/contratos/${contract.id}/composicoes`,'POST');await form.getByRole('button',{name:'Salvar composição',exact:true}).click();await confirmPassword();const r=await pending,dto=unwrap(await r.json());
  check('Composição201 exata pelaUI',r.status()===201&&dto.id>0&&dto.contratoId===contract.id&&dto.efetivoPrevisto===5&&dto.lotacaoId===fixture.ids.lotacao&&dto.jornadaId===fixture.ids.jornada&&dto.cargoId===fixture.ids.cargo&&dto.dataInicioVigencia===start);
  recordBusiness({[kind+'CompositionId']:dto.id,cohort55Pending:null});await page.getByLabel('Senha de Acesso',{exact:true}).waitFor({state:'hidden'});return dto.id;
 };
 const mainComposition=await compose('cohort55Main',main,today),futureContract=await create('cohort55Future','SINTETICO-E2E-COORTE55-FUTURO',future),futureComposition=await compose('cohort55Future',futureContract,future);
 main=await get('/api/contratos/'+main.id);const nextFuture=await get('/api/contratos/'+futureContract.id),compositions=await get(`/api/contratos/${main.id}/composicoes`);
 check('Dois rascunhos e composição preservam origem canônica',main.estadoNegocial==='RASCUNHO'&&main.fornecedorId===provider.id&&nextFuture.estadoNegocial==='RASCUNHO'&&compositions.some(x=>x.id===mainComposition&&x.efetivoPrevisto===5));
 const derived=JSON.parse(JSON.stringify(fixture));derived.status='DERIVED_COHORT55_UI_API_PREPARATION_ONLY';derived.acceptance=false;derived.preparedAt=new Date().toISOString();derived.dates={...derived.dates,today,future};
 derived.ids={...derived.ids,contrato_rascunho:main.id,composicao_contrato_rascunho:mainComposition,contrato_futuro:nextFuture.id,composicao_contrato_futuro:futureComposition};derived.objects={...derived.objects,contrato_rascunho:main,contrato_futuro:nextFuture};
 let cpf='941055001';for(const n of [9,10]){let sum=0;for(let i=0;i<n;i++)sum+=Number(cpf[i])*(n+1-i);const digit=(sum*10)%11;cpf+=digit===10?'0':String(digit);}
 derived.pending_vinculo_request={...derived.pending_vinculo_request,pessoa:{...derived.pending_vinculo_request.pessoa,cpf},contratoId:main.id,composicaoId:mainComposition,dataInicio:today,matricula:'SINT-COORTE55-001'};
 derived.runtime={...derived.runtime,database:current.database.database,jarSha256:current.java.jar_sha256,derivedFromNativeFixtureSHA256:hash(path.join(runtime,'private/e2e-fixture.json')),sharedOwnedStorageRoot:path.join(runtime,'storage')};
 derived.limits=[...(derived.limits||[]),'Coorte55derivada: novas criações UI/API no mesmo banco exclusivo; contas/catálogos/painel/relatório/ocorrência da fixture nativa permanecem reais. Preparação não é aceite; acervo45preservado.'];
 fs.mkdirSync(path.join(next,'private'),{recursive:true,mode:0o700});
 const write=(name,value)=>{const file=path.join(next,'private',name),fd=fs.openSync(file,'wx',0o600);try{fs.writeFileSync(fd,JSON.stringify(value,null,2));fs.fsyncSync(fd);}finally{fs.closeSync(fd);}const dir=fs.openSync(path.dirname(file),'r');try{fs.fsyncSync(dir);}finally{fs.closeSync(dir);}};
 write('e2e-fixture.json',derived);write('e2e-business.json',{});
 const proof={state:'DERIVED_COHORT55_READY_REAL_UI_API_PREPARATION_ONLY',database:current.database.database,base_fixture_sha256:hash(path.join(runtime,'private/e2e-fixture.json')),fixture_sha256:hash(path.join(next,'private/e2e-fixture.json')),creation_jar_sha256:current.java.jar_sha256,source_driver_sha256:hash(__filename),mainContract:main.id,futureContract:nextFuture.id,mainComposition,all_accounts_and_catalogs_reused_unchanged:true,all_mutations_real_UI:true,no_DDL_DML_SQL:true,no_cache_flush:true,shared_owned_storage:path.join(runtime,'storage'),not_acceptance:true};write('derivation-proof.json',proof);
 recordBusiness({cohort55Ready:true,cohort55FixtureSHA256:proof.fixture_sha256});report.scope='PREPARATION_ONLY_NOT_ACCEPTANCE_NOT_AN_ORIGINAL_DRIVER';report.derived={main:main.id,future:nextFuture.id,composition:mainComposition,fixtureSHA256:proof.fixture_sha256,baseFixtureUnmodified:true};await screenshot('nova-coorte-rascunhos');
},{accountName:'gestor_local',reuse:false});
