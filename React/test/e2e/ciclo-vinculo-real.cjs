'use strict';
const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const {execute}=require('./ambiente-real.cjs');
const {baseFrontend}=require('./contexto-fixture.cjs');
const phase=process.env.SGT_VINCULO_PHASE;
if(!['programar','cancelar','admitir','desligar','recontratar'].includes(phase))throw new Error('Fase explícita do ciclo de vínculo obrigatória.');
const digest=value=>crypto.createHash('sha256').update(JSON.stringify(value)).digest('hex');
const cpf=()=>{let x='931070013';for(const n of [9,10]){let sum=0;for(let i=0;i<n;i++)sum+=Number(x[i])*(n+1-i);const v=(sum*10)%11;x+=v===10?'0':String(v);}return x;};
const civilNow=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
const shift=(date,n)=>{const d=new Date(date+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+n);return d.toISOString().slice(0,10);};
const unwrap=body=>body?.data??body;
execute('ciclo-vinculo-real-'+phase,async({page,context,fixture,check,open,confirmPassword,screenshot,report,recordBusiness})=>{
  const file=path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json');
  const b=JSON.parse(fs.readFileSync(file));const base=baseFrontend(),url='/api/vinculos-contratuais';
  const original=fixture.pending_vinculo_request,reference=b.vinculoCiclo07Data||civilNow(),future=shift(reference,8),next=shift(reference,1);
  const markers={programado:'J07-CICLO-PROGR',admitido:'J07-CICLO-ATUAL',recontratado:'J07-CICLO-NOVO'};
  check('Fase autorizada apenas no corpus sintético específico e com data civil real',fixture.synthetic&&original.contratoId!==b.jobs200ContratoId
    && fixture.ids.contrato_rascunho===original.contratoId&&reference===civilNow());
  await open('/pessoas');
  const read=async(route,params)=>{
    const r=await context.request.get(base+route,{params,timeout:10000});check(`GET ${route} confirma leitura real`,r.status()===200,{status:r.status()});
    return unwrap(await r.json());
  };
  const wait=route=>{const promise=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname===route,{timeout:25000});promise.catch(()=>{});return promise;};
  const position=(pid,date,known)=>read(url+'/pessoas/'+pid+'/posicao',{data:date,...(known?{conhecidoAte:known}:{})});
  const allocation=(id,date,known)=>read(url+'/'+id+'/alocacoes/posicao',{data:date,...(known?{conhecidoAte:known}:{})});
  const events=id=>read(url+'/'+id+'/eventos');
  const admission=async(kind,pid,date)=>{
    await page.getByRole('button',{name:'Novo vínculo',exact:true}).click();
    const modal=page.getByRole('dialog',{name:'Cadastrar vínculo contratual',exact:true});await modal.waitFor();
    if(!pid){await modal.getByRole('button',{name:'Nova pessoa',exact:true}).click();await modal.getByLabel(/^Nome/).fill(original.pessoa.nome);await modal.getByLabel(/^CPF/).fill(cpf());}
    else{await modal.getByRole('button',{name:'Pessoa existente',exact:true}).click();await modal.getByPlaceholder('ID da pessoa').fill(String(pid));}
    await modal.getByLabel(/^Contrato/).selectOption(String(original.contratoId));await modal.getByLabel(/^Início efetivo/).fill(date);
    await modal.getByLabel(/^Matrícula do vínculo/).fill(markers[kind]);
    const composition=modal.getByLabel(/^Composição vigente/);
    await composition.locator(`option[value="${original.composicaoId}"]:not([disabled])`).waitFor({state:'attached',timeout:15000});
    await composition.selectOption(String(original.composicaoId));
    const pending=wait(url);await modal.getByRole('button',{name:'Cadastrar vínculo',exact:true}).click();await confirmPassword();
    const response=await pending,envelope=await response.json(),dto=unwrap(envelope);
    if(response.status()===201&&dto.id>0&&dto.pessoaId>0) recordBusiness(kind==='programado'
      ?{vinculoCiclo07Pessoa:dto.pessoaId,vinculoCiclo07Programado:dto.id,vinculoCiclo07Data:reference}
      :{['vinculoCiclo07'+(kind==='admitido'?'Ativo':'Recontratado')]:dto.id});
    check('Cadastro via interface, senha e Java201 conserva identidade, composição, matrícula e data',response.status()===201&&dto.id>0&&dto.pessoaId>0
      && (!pid||dto.pessoaId===pid)&&dto.contratoId===original.contratoId&&dto.composicaoInicialId===original.composicaoId&&dto.dataInicio===date&&dto.matricula===markers[kind],
      {status:response.status(),code:envelope.code??null});
    await modal.waitFor({state:'hidden'});return dto;
  };
  const action=async(id,kind)=>{
    const cancelling=kind==='cancelar',label=cancelling?'Cancelar programação':'Registrar desligamento';
    await page.getByRole('tab',{name:/^Vínculos/}).click();
    await page.getByPlaceholder('Nome, CPF ou matrícula').fill(cancelling?markers.programado:markers.admitido);
    await page.getByRole('button',{name:'Aplicar filtros',exact:true}).click();
    const row=page.getByRole('row').filter({has:page.getByText(cancelling?markers.programado:markers.admitido,{exact:true})});
    await row.getByRole('button',{name:'Linha do tempo',exact:true}).click();
    const link=page.getByRole('dialog',{name:'Vínculo '+id,exact:true});await link.getByRole('button',{name:label,exact:true}).click();
    const modal=page.getByRole('dialog',{name:cancelling?'Cancelar programação futura':label,exact:true});
    const reason=cancelling?'Cancelar programação sintética antes do início.':'Desligamento sintético contemporâneo, com o último dia inclusivo.';
    await modal.getByLabel(/^Justificativa/).fill(reason);await modal.getByLabel(/^Evidência/).fill('SGT-TESTE-CICLO07 — evidência sintética.');
    await modal.getByLabel(/^Autorização/).fill('SGT-TESTE-CICLO07 — autorização de teste.');
    if(!cancelling){
      const motivo=await read('/api/motivos-desligamento/'+fixture.ids.motivo_desligamento);
      check('Motivo atual ativo e de desligamento individual no escopo',motivo.ativo&&['DESLIGAMENTO_INDIVIDUAL','OUTRO'].includes(motivo.categoria));
      await modal.getByLabel(/^Último dia do vínculo/).fill(reference);
      await modal.getByLabel(/^Motivo de desligamento/).selectOption(String(fixture.ids.motivo_desligamento));
      await modal.getByRole('checkbox',{name:/Confirmo a precedência:/i}).check();
    }
    const pending=wait(url+'/'+id+'/'+(cancelling?'cancelar-programacao':'desligar'));
    await modal.getByRole('button',{name:'Confirmar ação',exact:true}).click();await confirmPassword();
    const response=await pending,envelope=await response.json(),dto=unwrap(envelope);
    if(response.status()===200&&dto.id===id)recordBusiness(cancelling?{vinculoCiclo07Cancelado:true}:{vinculoCiclo07Desligado:true});
    check('Comando auditável confirmado pela UI e Java200 no mesmo vínculo',response.status()===200&&dto.id===id&&dto.pessoaId===b.vinculoCiclo07Pessoa,{status:response.status(),code:envelope.code??null});
    await modal.waitFor({state:'hidden'});return dto;
  };
  if(phase==='programar'){
    check('Intenção nova não foi preparada em tentativa anterior',!b.vinculoCiclo07Pessoa);
    const matches=await read('/api/pessoas',{busca:cpf(),size:20});check('CPF sintético de teste sem identidade anterior',matches.totalElements===0);
    const dto=await admission('programado',null,future);
    check('Pessoa homônima criada com ID independente e vínculo programado',dto.pessoaId!==b.pessoaId&&dto.pessoaNome===original.pessoa.nome&&dto.situacaoTemporal==='PROGRAMADO');
    const originalPerson=await read('/api/pessoas/'+b.pessoaId),newPerson=await read('/api/pessoas/'+dto.pessoaId);
    check('Identidades homônimas permanecem distintas em GET canônico',originalPerson.id!==newPerson.id&&originalPerson.nome===newPerson.nome&&originalPerson.nome===original.pessoa.nome);
    const creation=await events(dto.id);check('Programação cria exatamente um evento com fotografia imutável',creation.length===1&&creation[0].tipo==='CRIACAO'&&creation[0].matriculaNova===markers.programado&&!!creation[0].hashEvento);
    recordBusiness({vinculoCiclo07ProgramadoCriacao:digest(creation[0])});
  }else if(phase==='cancelar'){
    check('Programação identificada e ainda não cancelada',b.vinculoCiclo07Programado>0&&!b.vinculoCiclo07Cancelado&&!!b.vinculoCiclo07ProgramadoCriacao);
    const dto=await action(b.vinculoCiclo07Programado,'cancelar');
    const chain=await events(dto.id);
    check('Cancelamento não desliga nem apaga origem, encadeando o evento',dto.estadoNegocial==='CANCELADO'&&dto.situacaoTemporal==='CANCELADO'&&dto.motivoDesligamentoId==null
      && chain.length===2&&digest(chain[0])===b.vinculoCiclo07ProgramadoCriacao&&chain[1].tipo==='CANCELAMENTO_PROGRAMACAO'&&chain[1].eventoPrecedenteId===chain[0].id);
    recordBusiness({vinculoCiclo07CanceladoTrilha:digest(chain)});
  }else if(phase==='admitir'){
    check('Admissão nova reutiliza somente a pessoa cuja programação foi cancelada',b.vinculoCiclo07Pessoa>0&&b.vinculoCiclo07Cancelado&&!b.vinculoCiclo07Ativo);
    const dto=await admission('admitido',b.vinculoCiclo07Pessoa,reference),chain=await events(dto.id);
    check('Admissão vigente tem novo vínculo e preserva matrícula cancelada',dto.id!==b.vinculoCiclo07Programado&&dto.situacaoTemporal==='VIGENTE'&&chain.length===1&&chain[0].tipo==='CRIACAO');
    const k=chain[0].registradoEm,photo=await position(dto.pessoaId,reference,k),alloc=await allocation(dto.id,reference,k);
    const fact=photo.vinculos.find(x=>x.vinculoId===dto.id);
    check('Fotografia D/K comprova pessoa, vigência, matrícula e alocação da admissão',fact?.pessoaId===dto.pessoaId&&fact.situacaoTemporal==='VIGENTE'
      && fact.matricula===markers.admitido&&alloc.vinculoId===dto.id&&alloc.composicaoBaseId===original.composicaoId&&alloc.lotacaoId===fixture.ids.lotacao&&alloc.jornadaId===fixture.ids.jornada&&alloc.cargoId===fixture.ids.cargo);
    recordBusiness({vinculoCiclo07ConhecidoAte:k,vinculoCiclo07Foto:digest(photo),vinculoCiclo07AlocacaoFoto:digest(alloc),vinculoCiclo07CriacaoAtiva:digest(chain[0])});
  }else if(phase==='desligar'){
    check('Somente vínculo vigente novo será desligado uma vez',b.vinculoCiclo07Ativo>0&&!b.vinculoCiclo07Desligado&&!!b.vinculoCiclo07Foto);
    const dto=await action(b.vinculoCiclo07Ativo,'desligar'),chain=await events(dto.id);
    check('Desligamento preserva a origem e grava o último dia, motivo e fotografia',dto.dataFim===reference&&dto.motivoDesligamentoId===fixture.ids.motivo_desligamento
      && chain.length===2&&digest(chain[0])===b.vinculoCiclo07CriacaoAtiva&&chain[1].tipo==='DESLIGAMENTO'&&chain[1].fimNovo===reference
      && chain[1].motivoDesligamentoIdFotografia===fixture.ids.motivo_desligamento&&!!chain[1].motivoDesligamentoDescricaoFotografia);
    recordBusiness({vinculoCiclo07DesligadoTrilha:digest(chain)});
  }else{
    check('Recontratação é uma intenção nova após desligamento comprovado',b.vinculoCiclo07Desligado&&!!b.vinculoCiclo07DesligadoTrilha&&!b.vinculoCiclo07Recontratado);
    const dto=await admission('recontratado',b.vinculoCiclo07Pessoa,next);
    const now=await position(dto.pessoaId,next),old=await position(dto.pessoaId,reference,b.vinculoCiclo07ConhecidoAte);
    const newLink=now.vinculos.find(x=>x.vinculoId===dto.id),oldLink=now.vinculos.find(x=>x.vinculoId===b.vinculoCiclo07Ativo);
    check('Pessoa única recontratada com vínculo e matrícula novos no dia consecutivo, sem sobreposição',dto.situacaoTemporal==='PROGRAMADO'&&dto.id!==b.vinculoCiclo07Ativo
      && newLink?.pessoaId===oldLink?.pessoaId&&newLink?.matricula===markers.recontratado&&oldLink?.situacaoTemporal==='ENCERRADO'&&oldLink?.dataFim===reference);
    const oldAllocation=await allocation(b.vinculoCiclo07Ativo,reference,b.vinculoCiclo07ConhecidoAte);
    check('Fotografia D/K de admissão e trilhas de cancelamento/desligamento mantêm os mesmos hashes',digest(old)===b.vinculoCiclo07Foto
      && digest(oldAllocation)===b.vinculoCiclo07AlocacaoFoto&&digest(await events(b.vinculoCiclo07Programado))===b.vinculoCiclo07CanceladoTrilha
      && digest(await events(b.vinculoCiclo07Ativo))===b.vinculoCiclo07DesligadoTrilha);
    recordBusiness({vinculoCiclo07Completo:true});
  }
  report.conditions={phase,contract:original.contratoId,composition:original.composicaoId,civilDate:reference,physicalClock:true};
  await screenshot(phase);
},{accountName:'gestor_local',reuse:false});
