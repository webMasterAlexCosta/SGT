'use strict';
const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const {execute}=require('./ambiente-real.cjs');
const {baseFrontend}=require('./contexto-fixture.cjs');
const unwrap=x=>x?.data??x;
const sha=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const hash=x=>sha(JSON.stringify(x));
const shift=(date,by)=>{const d=new Date(date+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+by);return d.toISOString().slice(0,10);};
// Identidade de preparação: o nome tem JOBS200 e também termina em 200.
// Busca por palavras pode achar outros colaboradores; o CPF sintético é único.
const syntheticCpf=()=>{let digits='930550200';for(const n of [9,10]){let sum=0;for(let i=0;i<n;i++)sum+=Number(digits[i])*(n+1-i);const v=(sum*10)%11;digits+=v===10?'0':String(v);}return digits;};
const filename='atestado-sintetico-quant07.txt',note='Atestado sintético de três dias, sem dado de saúde real.';
const bytes=Buffer.from('SGT TESTE 07: documento exclusivamente sintético.\nAtestado de homologação multi-dia, sem identidade ou dado de saúde real.\n','utf8');
execute('atestado-multidia-real',async({page,context,fixture,account,open,check,recordBusiness,report,confirmPassword,screenshot})=>{
  const b=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json'))),base=baseFrontend();
  const chosen=b.jobs200People?.find(x=>x.index===200),name='SINTETICO JOBS200 FINAL07 200',contract=b.jobs200ContratoId;
  const dates=[2,3,4].map(n=>shift(b.jobs200DataInicio,n));
  check('Pessoa sintética real do contrato200, datas separadas dos jobs e ocorrência sem reenvio',fixture.synthetic&&chosen?.vinculoId>0&&chosen.pessoaId>0
    && b.jobs200GradeConcluido&&b.jobs200CancelReprocessamentoConcluido&&!b.atestado07Id
    && dates.every(d=>d!==b.jobs200GradeData&&d!==b.jobs200CancelData&&d<fixture.dates.today));
  await open('/ocorrencias');
  const read=async(route,params)=>{
    const r=await context.request.get(base+route,{params,headers:{Accept:'application/json'},timeout:10000});
    check(`Leitura independente ${route} responde200`,r.status()===200);
    return unwrap(await r.json());
  };
  const freq=async day=>{
    const list=await read('/api/frequencias',{data:day,contratoId:String(contract),lotacaoId:String(fixture.ids.lotacao),jornadaId:String(fixture.ids.jornada),size:500});
    const matching=list.content.filter(x=>x.vinculoContratualId===chosen.vinculoId);check('Uma frequência por vínculo e dia',matching.length<=1);
    return matching[0]||null;
  };
  const csrf=await read('/api/autenticacao/csrf');
  const headers={[csrf.headerName]:csrf.token,'X-User-Password':account.password,Origin:base};
  const empty=[];for(const day of dates)empty.push(await freq(day));
  let original;
  if(b.atestado07Fonte){
    const prior=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'../browser-jar07-atestado01/browser/atestado-multidia-real.json')));
    check('Retoma a presença já gravada, sem novo POST nem ocorrência anterior',prior.passed===false&&prior.failure==='Busca real localiza o mesmo empregado do vínculo canônico'
      && !prior.http.some(x=>x.method==='POST'&&x.path==='/api/ocorrencias')&&empty[0]?.id===b.atestado07Fonte&&empty.slice(1).every(x=>x===null));
    const check02=path.join(process.env.SGT_TEST_RUNTIME,'../browser-jar07-atestado02/browser/atestado-multidia-real.json');
    if(fs.existsSync(check02)){
      const prior02=JSON.parse(fs.readFileSync(check02));
      check('Segunda tentativa comprovou o CPF, sem enviar o formulário',!prior02.passed&&prior02.lookup?.count===1
        && prior02.lookup.actualIds[0]===empty[0].empregado.id&&!prior02.http.some(x=>x.method==='POST'&&x.path==='/api/ocorrencias'));
    }
    original=empty[0];
  }else{
    check('Datas próprias sem fatos anteriores a sobrescrever',empty.every(x=>x===null));
    const source=await context.request.post(base+'/api/frequencias',{headers,data:{vinculoContratualId:chosen.vinculoId,dataReferencia:dates[0],
      status:'PRESENCA',entrada1:'08:00',saida1:'12:00',entrada2:'13:00',saida2:'17:00',justificativaRetificacao:'Fonte sintética original que o atestado não pode apagar.',
      chaveIdempotencia:'SGT-TESTE-AT07-FONTE-'+chosen.vinculoId},timeout:12000});
    original=unwrap(await source.json());check('Java aceita a presença de fonte',source.status()===200&&original.id>0);
    recordBusiness({atestado07Fonte:original.id});
  }
  check('Fonte original é presença real da pessoa e vínculo selecionados',original.status==='PRESENCA'&&original.id>0
    && original.vinculoContratualId===chosen.vinculoId&&original.pessoaId===chosen.pessoaId&&original.empregado?.id>0);
  const origin=await read('/api/frequencias/'+original.id+'/eventos');
  check('A trilha da fonte original tem um evento confirmado',origin.length===1&&origin[0].snapshotNovo.includes('status=PRESENCA')&&origin[0].hashEvento.length>10);
  const search=page.getByLabel('Nome ou CPF do colaborador',{exact:true});
  const cpf=syntheticCpf();
  const searched=page.waitForResponse(r=>{const u=new URL(r.url());return r.request().method()==='GET'&&u.pathname==='/api/empregados'&&u.searchParams.get('search')===cpf;});searched.catch(()=>{});
  await search.fill(cpf);const got=await searched, candidates=unwrap(await got.json()).content;
  report.lookup={status:got.status(),count:candidates.length,expectedEmployeeId:original.empregado.id,actualIds:candidates.map(x=>x.id)};
  check('Busca real pelo CPF único localiza somente o empregado do vínculo canônico',got.status()===200&&candidates.length===1&&candidates[0].nome===name&&candidates[0].id===original.empregado.id);
  await page.getByRole('button',{name:new RegExp(name)}).click();
  await page.getByRole('button',{name:'Registrar Ocorrência',exact:true}).click();
  const modal=page.getByRole('dialog',{name:'Nova Ocorrência',exact:true});await modal.getByRole('button',{name:'Atestado',exact:true}).click();
  await modal.getByLabel('Data do Ocorrido *',{exact:true}).fill(dates[0]);
  await modal.getByRole('spinbutton').fill('3');
  await modal.getByLabel('Número SEI (opcional)',{exact:true}).fill('00001.000007/2026-07');
  await modal.getByPlaceholder('Descreva observações...').fill(note);
  await modal.locator('input[type="file"]').setInputFiles({name:filename,mimeType:'text/plain',buffer:bytes});
  await modal.getByText(filename,{exact:true}).waitFor();
  const saved=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname==='/api/ocorrencias',{timeout:25000});saved.catch(()=>{});
  await modal.getByRole('button',{name:'Salvar Registro',exact:true}).click();await confirmPassword();
  const response=await saved,data=unwrap(await response.json());
  if(response.status()===201&&data.id>0)recordBusiness({atestado07Id:data.id,atestado07Anexo:data.anexos?.[0]?.id,atestado07Dates:dates,atestado07Vinculo:chosen.vinculoId,atestado07Sha:sha(bytes)});
  check('Upload real201 associa um ATESTADO de três dias ao empregado, com anexo restrito',response.status()===201&&data.id>0&&data.motivo==='ATESTADO'
    && data.dataOcorrencia===dates[0]&&data.quantidadeDias===3&&data.empregado.id===original.empregado.id&&data.anexos.length===1
    && data.anexos[0].nomeOriginal===filename&&data.anexos[0].classificacaoAcesso==='PESSOAL_RESTRITO');
  await modal.waitFor({state:'hidden'});
  await page.getByText(note,{exact:true}).waitFor();
  check('Histórico do Chrome mostra três dias e o mesmo documento',await page.getByText('3 Dias',{exact:true}).count()===1);
  const detail=await read('/api/ocorrencias/'+data.id,{finalidade:'Conferência sintética da vigência e do anexo.'});
  check('GET histórico comprova mesma ocorrência, intervalo e identidade',detail.ocorrencia.id===data.id&&detail.ocorrencia.quantidadeDias===3
    && detail.ocorrencia.dataOcorrencia===dates[0]&&detail.cancelada===false&&detail.ocorrencia.anexos[0].id===data.anexos[0].id);
  const after=[];for(const d of dates)after.push(await freq(d));
  check('Efeito AT aparece apenas nas três datas do mesmo vínculo',after.every((x,i)=>x?.status==='ATESTADO'&&x?.dataReferencia===dates[i]&&x?.vinculoContratualId===chosen.vinculoId)
    && after[0].id===original.id&&new Set(after.map(x=>x.id)).size===3&&await freq(shift(dates[2],1))===null);
  const chain=await read('/api/frequencias/'+original.id+'/eventos');
  check('Histórico preserva a presença original e acrescenta uma versão ATESTADO',chain.length===2&&hash(chain[0])===hash(origin[0])
    && chain[1].hashAnterior===chain[0].hashEvento&&chain[1].snapshotAnterior.includes('status=PRESENCA')&&chain[1].snapshotNovo.includes('status=ATESTADO'));
  await page.getByRole('button',{name:'Acessar anexo: '+filename,exact:true}).click();
  const downloadModal=page.getByRole('dialog',{name:'Acessar anexo',exact:true});
  await downloadModal.getByLabel(/^Finalidade do acesso/).fill('Conferência sintética do mesmo anexo.');
  const download=page.waitForEvent('download',{timeout:15000});download.catch(()=>{});
  await downloadModal.getByRole('button',{name:'Baixar anexo',exact:true}).click();
  const file=await download,target=path.join(process.env.SGT_E2E_OUTPUT,filename);await file.saveAs(target);fs.chmodSync(target,0o600);
  check('Download UI entrega exatamente o arquivo sintético original',!await file.failure()&&file.suggestedFilename()===filename&&fs.readFileSync(target).equals(bytes));
  report.conditions={contract,vinculo:chosen.vinculoId,occurrence:data.id,attachment:data.anexos[0].id,dates,originalSource:original.id};
  report.downloads=[{file:filename,bytes:bytes.length,sha256:sha(bytes)}];
  report.limits=['Atestado, efeitos, trilha e download no mesmo JAR e laboratório. Prova de revogação do leitor específico é outro cenário.'];
  recordBusiness({atestado07Completo:true});await screenshot('atestados-e-anexo');
},{accountName:'gestor_local',reuse:false});
