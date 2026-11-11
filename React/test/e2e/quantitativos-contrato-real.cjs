'use strict';
const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const {execute}=require('./ambiente-real.cjs');
const {baseFrontend}=require('./contexto-fixture.cjs');
const phase=process.env.SGT_QUANT_PHASE;
if(!['preparar','preparar-retomar','preparar-corrigir','limites','conceder','excecao','remanejar','revogar'].includes(phase))throw new Error('Fase quantitativa explícita obrigatória.');
const admin=['conceder','revogar'].includes(phase);
const hash=value=>crypto.createHash('sha256').update(JSON.stringify(value)).digest('hex');
const civil=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
const plus=(d,n)=>{const v=new Date(d+'T12:00:00Z');v.setUTCDate(v.getUTCDate()+n);return v.toISOString().slice(0,10);};
const unwrap=value=>value?.data??value;
const number='SINTETICO-E2E-QUANT07-100',cargoName='Cargo sintético QUANT07 B';
const cpf=()=>{let digits='931070027';for(const n of [9,10]){let sum=0;for(let i=0;i<n;i++)sum+=Number(digits[i])*(n+1-i);const v=(sum*10)%11;digits+=v===10?'0':String(v);}return digits;};
const reason='Validação sintética do efetivo contratual e das permissões, sem efeito administrativo.';
const foundation='Fonte SEI sintética e autorização específica para redução acima do piso no contrato QUANT07.';
execute('quantitativos-contrato-real-'+phase,async({page,context,fixture,account,open,check,recordBusiness,report,screenshot})=>{
  const file=path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json');const readBusiness=()=>JSON.parse(fs.readFileSync(file));
  let b=readBusiness();const base=baseFrontend();const today=b.quant07Data||civil();
  check('Roteiro restrito ao laboratório e aos atores sintéticos explícitos',fixture.synthetic&&account.id===fixture.accounts[admin?'fixture_admin':'gestor_local'].id&&/^\d{4}-\d{2}-\d{2}$/.test(today));
  await open(admin?'/permissoes-excepcionais':'/contratos');
  const csrf=unwrap(await (await context.request.get(base+'/api/autenticacao/csrf')).json());
  const headers={Origin:base,Accept:'application/json',[csrf.headerName]:csrf.token,'X-User-Password':account.password};
  const http=async(method,route,{data,params,expected}={})=>{
    const r=await context.request.fetch(base+route,{method,headers,data,params,timeout:12000});
    const envelope=await r.json(),dto=unwrap(envelope);
    report.http.push({method,path:route,status:r.status(),code:envelope.code??null,...(r.status()>=400?{message:envelope.message??null}:{}),driver:'API real, sessão aberta pela UI'});
    if(expected!==undefined)check(`${method} ${route} devolve ${expected}`,r.status()===expected,{status:r.status(),code:envelope.code??null,message:envelope.message??null});
    return {dto,status:r.status(),code:envelope.code??null,message:envelope.message??null};
  };
  const get=async(route,params)=>(await http('GET',route,{params,expected:200})).dto;
  const post=async(route,data,expected)=>http('POST',route,{data,expected});
  const index='/api/contratos',perms='/api/permissoes-excepcionais';
  const quantities=async(id,date,known)=>{
    const list=await get(`${index}/${id}/composicoes`,{data:date,...(known?{conhecidoAte:known}:{})});
    return Object.fromEntries(list.map(x=>[x.id,x.efetivoPrevisto]));
  };
  const pair=(q,a,v1,v2)=>q[a[0]]===v1&&q[a[1]]===v2&&Object.keys(q).length===2;
  const verifyPosition=async(date,total,a,v1,v2,known)=>{
    const p=await get(`${index}/${b.quant07Contrato}/posicao-temporal`,{data:date,...(known?{conhecidoAte:known}:{})});
    check(`D/K em ${date} reconstrói total ${total} e os dois efetivos corretos`,p.versaoContratual.efetivoPrevisto===total
      && pair(await quantities(b.quant07Contrato,date,known),a,v1,v2));
  };
  const oldPhotos=async()=>{
    const vs=await get(`${index}/${b.quant07Contrato}/versoes`);
    check('Fotografia original de100 permanece byte a byte no DTO',hash(vs.find(x=>x.id===b.quant07VersaoId))===b.quant07VersaoSha);
    await verifyPosition(b.quant07D3,100,b.quant07Composicoes,60,40,b.quant07KOriginal);
  };
  if(phase.startsWith('preparar')){
    let target,ids,contract,cargo,provider,dataInicio,dataFim;
    if(phase==='preparar'){
    check('Intenção inédita no corpus, sem reaproveitar estado parcial',!b.quant07Contrato&&!b.quant07Cargo);
    const all=await get(index);check('Não existe outro contrato com o marcador reservado',!all.some(x=>x.numeroContrato===number));
    const cargos=await get('/api/cargos',{size:100});check('Cargo B inédito na Gestão',Array.isArray(cargos.content)&&!cargos.content.some(x=>x.nome===cargoName));
    cargo=(await post('/api/cargos',{nome:cargoName,descricao:'Cargo B da composição sintética de 40.',ativo:true},200)).dto;
    check('Segundo cargo ativo e da mesma Gestão',cargo.id>0&&cargo.id!==fixture.ids.cargo&&cargo.gestaoLocalId===fixture.ids.gestao_origem&&cargo.ativo);
    recordBusiness({quant07Cargo:cargo.id,quant07Data:today,quant07D1:plus(today,1),quant07D2:plus(today,2),quant07D3:plus(today,3)});
    provider=(await get('/api/fornecedores')).find(x=>x.id===b.jobs200FornecedorId&&x.ativo);
    const template=await get(`${index}/${fixture.ids.contrato_rascunho}`);check('Empresa e prazo provêm do corpus publicado',provider?.id>0&&template.dataFim>plus(today,3));
    dataInicio=today;dataFim=template.dataFim;
    contract=(await post(index,{numeroContrato:number,anoContrato:Number(today.slice(0,4)),descricao:reason,tipoContratacaoId:fixture.ids.tipo,
      dataInicio,dataFim,efetivoPrevisto:100,cnpjEmpresa:provider.cnpj,razaoSocialEmpresa:provider.razaoSocial,nomeFantasiaEmpresa:provider.nomeFantasia||'QUANT07 Sintético'},201)).dto;
    if(contract.id>0)recordBusiness({quant07Contrato:contract.id,quant07DataInicio:dataInicio,quant07Numero:number});
    check('Novo rascunho100 no escopo, separado da massa200',contract.id>0&&contract.id!==b.jobs200ContratoId&&contract.estadoNegocial==='RASCUNHO'&&contract.efetivoPrevisto===100&&contract.fornecedorId==null);
    target=index+'/'+contract.id;
    const linked=(await http('PUT',target+'/fornecedor/'+provider.id,{expected:200})).dto;
    check('Associação real é da empresa ativa selecionada',linked.contratoId===contract.id&&linked.fornecedor?.id===provider.id);
    recordBusiness({quant07Fornecedor:provider.id});
    ids=[];
    }else if(phase==='preparar-retomar'){
      const prior=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'../browser-jar07-quant-preparar01/browser/quantitativos-contrato-real-preparar.json')));
      target=index+'/'+b.quant07Contrato;
      check('Retomada só após falha registrada no segundo POST, sem repetir os commits',prior.passed===false&&b.quant07Composicoes?.length===1&&!b.quant07Publicado
        && prior.http.filter(x=>x.method==='POST'&&x.path===target+'/composicoes').map(x=>x.status).join(',')==='201,409');
      contract=await get(target);cargo=await get('/api/cargos/'+b.quant07Cargo);
      provider=(await get('/api/fornecedores')).find(x=>x.id===b.quant07Fornecedor&&x.ativo);
      dataInicio=contract.dataInicio;dataFim=contract.dataFim;
      const existing=await get(target+'/composicoes',{data:today});
      check('Rascunho, fornecedor, cargo e primeira composição conferidos por GET independente',contract.numeroContrato===number&&contract.estadoNegocial==='RASCUNHO'
        && contract.fornecedorId===provider?.id&&cargo.nome===cargoName&&existing.length===1&&existing[0].id===b.quant07Composicoes[0]&&existing[0].efetivoPrevisto===60);
      const catalog='/api/catalogos-temporais/CARGO/'+cargo.id+'/disponibilidade';
      const earlier=await http('GET',catalog,{params:{data:dataInicio}}),current=await get(catalog,{data:today});
      check('Cargo novo não tinha fonte de disponibilidade no início antigo e está comprovado hoje',
        (earlier.status===409||earlier.status===200&&(earlier.dto.ativo!==true||earlier.dto.disponibilidadeComprovada!==true))
        && current.ativo===true&&current.disponibilidadeComprovada===true);
      ids=[...b.quant07Composicoes];
    }else{
      target=index+'/'+b.quant07Contrato;
      const prior=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'../browser-jar07-quant-preparar02/browser/quantitativos-contrato-real-preparar-retomar.json')));
      check('Rascunho parcial vem de duas composições gravadas e prévia400, sem publicação',
        !b.quant07Publicado&&b.quant07Composicoes?.length===2&&prior.passed===false&&prior.http.some(x=>x.path===target+'/previas/publicar'
        &&x.status===400&&/nome fantasia/.test(x.message))&&!prior.http.some(x=>x.path===target+'/publicar'&&x.method==='POST'));
      contract=await get(target);cargo=await get('/api/cargos/'+b.quant07Cargo);
      provider=(await get('/api/fornecedores')).find(x=>x.id===b.quant07Fornecedor&&x.ativo);
      ids=[...b.quant07Composicoes];dataInicio=today;dataFim=contract.dataFim;
      const existing=await get(target+'/composicoes',{data:today});
      const a=existing.find(x=>x.id===ids[0]),second=existing.find(x=>x.id===ids[1]);
      check('Confirma os dois mesmos IDs, empresa ativa e datas a alinhar antes da publicação',contract.numeroContrato===number
        && contract.estadoNegocial==='RASCUNHO'&&contract.fornecedorId===provider?.id&&existing.length===2
        && a?.efetivoPrevisto===60&&second?.efetivoPrevisto===40&&a.dataInicioVigencia===contract.dataInicio&&second.dataInicioVigencia===today);
      // A regra de publicação exige que TODAS as composições cubram a vigência integral.
      contract=(await http('PUT',target,{data:{...contract,dataInicio,nomeFantasiaEmpresa:provider.nomeFantasia||'QUANT07 Sintético'},expected:200})).dto;
      check('Rascunho corrigido mantém a empresa e move a vigência para quando os dois cargos já existiam',
        contract.estadoNegocial==='RASCUNHO'&&contract.dataInicio===dataInicio&&contract.fornecedorId===provider.id&&!!contract.nomeFantasiaEmpresa);
      const first=(await http('PUT',target+'/composicoes/'+ids[0],{data:{lotacaoId:a.lotacaoId,jornadaId:a.jornadaId,cargoId:a.cargoId,
        efetivoPrevisto:60,dataInicioVigencia:dataInicio,dataFimVigencia:dataFim},expected:200})).dto;
      check('Somente a vigência da composição já gravada é alinhada, sem recriação',first.id===ids[0]&&first.efetivoPrevisto===60&&first.dataInicioVigencia===dataInicio);
      recordBusiness({quant07DataInicio:dataInicio,quant07RascunhoCorrigido:true});
    }
    const components=[[fixture.ids.cargo,60,dataInicio],[cargo.id,40,today]];
    for(const [cargoId,efetivoPrevisto,inicio] of components.slice(ids.length)){
      const x=(await post(target+'/composicoes',{lotacaoId:fixture.ids.lotacao,jornadaId:fixture.ids.jornada,cargoId,efetivoPrevisto,dataInicioVigencia:inicio,dataFimVigencia:dataFim},201)).dto;
      check('Composição confirma capacidade, cargo e par operacional',x.id>0&&x.cargoId===cargoId&&x.efetivoPrevisto===efetivoPrevisto&&x.lotacaoId===fixture.ids.lotacao&&x.jornadaId===fixture.ids.jornada);
      ids.push(x.id);recordBusiness({quant07Composicoes:[...ids]});
    }
    const request={dataPublicacao:today,referenciaSei:'SGT-TESTE-QUANT07-PUBLICACAO',justificativa:reason};
    const preview=(await post(target+'/previas/publicar',request,200)).dto;
    check('Prévia atual antes de publicar',typeof preview.tokenPrevia==='string'&&preview.tokenPrevia.length>20);
    const published=(await post(target+'/publicar',{...request,tokenPrevia:preview.tokenPrevia,confirmarImpacto:true},200)).dto;
    if(published.estadoNegocial==='PUBLICADO')recordBusiness({quant07Publicado:true});
    check('Publicação mantém base original100',published.estadoNegocial==='PUBLICADO'&&published.efetivoOriginalPublicado===100&&published.fornecedorId===provider.id);
    const vs=await get(target+'/versoes');check('Primeira fotografia publicada única e com100',vs.length===1&&vs[0].efetivoPrevisto===100&&vs[0].id>0);
    recordBusiness({quant07VersaoId:vs[0].id,quant07VersaoSha:hash(vs[0]),quant07KOriginal:vs[0].registradoEm});b=readBusiness();
    await oldPhotos();
    const byCpf=await get('/api/pessoas',{busca:cpf(),size:5});check('Pessoa sintética de controle não existe antes da admissão',byCpf.totalElements===0);
    const hired=(await post('/api/vinculos-contratuais',{pessoa:{nome:'Pessoa de controle QUANT07',cpf:cpf()},contratoId:contract.id,
      dataInicio:today,matricula:'Q07-BASE-001',composicaoId:ids[0]},201)).dto;
    if(hired.id>0)recordBusiness({quant07Pessoa:hired.pessoaId,quant07Vinculo:hired.id});
    check('Há uma alocação real que o remanejamento deve preservar',hired.pessoaId>0&&hired.id>0&&hired.composicaoInicialId===ids[0]&&hired.dataInicio===today);
    const allocationEvents=await get('/api/vinculos-contratuais/'+hired.id+'/alocacoes/eventos');
    check('Evento de alocação inicial existe antes das alterações',allocationEvents.length===1);
    recordBusiness({quant07AlocacaoEventosSha:hash(allocationEvents)});
    report.scope='PREPARATION_ONLY_NOT_QUANTITATIVE_ACCEPTANCE';
  }else{
    check('Contrato100 explícito, distinto da massa anterior, com fotografia anterior selada',b.quant07Publicado===true&&b.quant07Contrato>0&&b.quant07Contrato!==b.jobs200ContratoId&&b.quant07Composicoes?.length===2&&!!b.quant07KOriginal);
    const target=index+'/'+b.quant07Contrato,a=b.quant07Composicoes;
    const add=target+'/aditamentos',move=target+'/remanejamentos';
    const make=(id,date,total,one,two,extra={})=>({identificacao:id,dataDocumento:b.quant07Data,dataAssinatura:b.quant07Data,dataEfetiva:date,justificativa:reason,referenciaSei:'SGT-TESTE-'+id,
      novoEfetivoTotal:total,alteracoesComposicao:[{composicaoId:a[0],quantidadeResultante:one},{composicaoId:a[1],quantidadeResultante:two}],...extra});
    const previewAdd=async request=>{
      const data=(await post(add+'/previa',request,200)).dto;
      check('Prévia do Java prende a base publicada100 e piso75',data.efetivoOriginalPublicado===100&&data.pisoNormalReducao===75&&!!data.tokenPrevia);
      return data;
    };
    const apply=async(request,preview,expected,key)=>{
      const response=await post(add,{...request,tokenPrevia:preview.tokenPrevia,confirmarImpacto:true});
      if(response.status===201&&response.dto?.id>0)recordBusiness({[key]:response.dto.id});
      check('Confirmação do aditamento retorna o status esperado',response.status===expected,{status:response.status,code:response.code,message:response.message});
      return response.dto;
    };
    if(phase==='limites'){
      check('Limites testados uma única vez',!b.quant07Reducao25&&!b.quant07Negativas);
      const ordinary=make('QUANT07-RED25',b.quant07D1,75,45,30);
      const p=await previewAdd(ordinary);check('Redução comum usa25% da base original',p.efetivoAnterior===100&&p.efetivoResultante===75&&!p.excedeLimiteNormal25);
      await apply(ordinary,p,201,'quant07Reducao25');await verifyPosition(b.quant07D1,75,a,45,30);
      const before=await get(add),versions=await get(target+'/versoes');
      const tooMuch=make('QUANT07-RED26-RECUSADA',b.quant07D2,74,44,30);
      const rejected=await previewAdd(tooMuch);check('O Java identifica 26% acima do limite original',rejected.exigeAutorizacaoExcepcional===true&&rejected.efetivoResultante===74);
      await apply(tooMuch,rejected,409,'quant07Inesperado26Comum');
      const noGrant=make('QUANT07-SEM-CONCESSAO',b.quant07D2,74,44,30,{reducaoExcepcional:true,fundamentoExcecao:foundation});
      await apply(noGrant,await previewAdd(noGrant),403,'quant07InesperadoSemConcessao');
      check('409/403 não criam aditamento, nova versão nem reduzem o efetivo',hash(await get(add))===hash(before)&&hash(await get(target+'/versoes'))===hash(versions));
      await verifyPosition(b.quant07D2,75,a,45,30);await oldPhotos();recordBusiness({quant07Negativas:true});
    }else if(phase==='conceder'){
      check('Concessão depende de recusas anteriores e não deve ser duplicada',b.quant07Negativas===true&&!b.quant07Permissao);
      const req={tipo:'REDUCAO_CONTRATUAL_EXCEPCIONAL',usuarioBeneficiarioId:fixture.accounts.gestor_local.id,contratoId:b.quant07Contrato,
        finalidade:'Exclusivamente a validação sintética de redução26 do contrato QUANT07.',justificativa:reason,
        autorizacaoAdministrativa:'SGT-TESTE-QUANT07-ATO-AUTORIZACAO',validoAte:new Date(Date.now()+40*60000).toISOString()};
      const granted=(await post(perms,req,201)).dto;
      if(granted.id>0)recordBusiness({quant07Permissao:granted.id});
      check('Administrador atual limita o ato a um contrato, beneficiário e finalidade',granted.id>0&&granted.tipo===req.tipo&&granted.contratoId===req.contratoId
        && granted.usuarioBeneficiarioId===req.usuarioBeneficiarioId&&granted.gestaoLocalId==null&&granted.ocorrenciaId==null&&granted.ativa===true&&granted.finalidade===req.finalidade);
      check('Concessão não registra uso por antecipação',(await get(`${perms}/${granted.id}/usos`)).length===0);
    }else if(phase==='excecao'){
      check('Usar uma única vez a concessão específica observada',b.quant07Permissao>0&&!b.quant07Reducao26&&!b.quant07Revogada);
      const authorized=make('QUANT07-RED26-AUTORIZADA',b.quant07D2,74,44,30,{reducaoExcepcional:true,fundamentoExcecao:foundation});
      const p=await previewAdd(authorized);check('Exceção calcula74 a partir de75, com base original100',p.efetivoAnterior===75&&p.efetivoResultante===74&&p.exigeAutorizacaoExcepcional);
      const receipt=await apply(authorized,p,201,'quant07Reducao26');
      check('Exceção preserva fundamento e ator da Gestão, em um único ato',receipt.id>0&&receipt.reducaoExcepcional&&receipt.fundamentoExcecao===foundation&&receipt.usuarioResponsavelId===account.id);
      await verifyPosition(b.quant07D2,74,a,44,30);await oldPhotos();
    }else if(phase==='remanejar'){
      check('Após redução26, remanejamento é intenção inédita',b.quant07Reducao26>0&&!b.quant07Remanejamento);
      const basePayload={identificacao:'QUANT07-REM-ZERO',dataEfetiva:b.quant07D3,referenciaSei:'SGT-TESTE-QUANT07-REM-ZERO',justificativa:reason};
      const listedBefore=await get(move);
      const bad={...basePayload,identificacao:'QUANT07-REM-INVALIDO',itens:[{composicaoId:a[0],variacao:-5},{composicaoId:a[1],variacao:4}]};
      const badPreview=(await post(move+'/previa',bad,200)).dto;
      check('Prévia apenas explica o impacto inválido e a soma -1',badPreview.somaVariacoes===-1&&badPreview.totalPreservado===false&&!!badPreview.tokenPrevia);
      const badCommit=await post(move,{...bad,tokenPrevia:badPreview.tokenPrevia,confirmarImpacto:true});
      if(badCommit.status===201&&badCommit.dto?.id>0)recordBusiness({quant07RemanejamentoInesperado:badCommit.dto.id});
      check('Confirmação com soma diferente de zero é recusada400, sem criar ato',badCommit.status===400&&hash(await get(move))===hash(listedBefore));
      const payload={...basePayload,itens:[{composicaoId:a[0],variacao:-5},{composicaoId:a[1],variacao:5}]};
      const p=(await post(move+'/previa',payload,200)).dto;
      check('Prévia soma zero e não muda o efetivo total74',p.somaVariacoes===0&&p.totalPreservado===true&&p.efetivoTotalAntes===74&&p.efetivoTotalDepois===74&&!!p.tokenPrevia);
      const moved=(await post(move,{...payload,tokenPrevia:p.tokenPrevia,confirmarImpacto:true},201)).dto;
      if(moved.id>0)recordBusiness({quant07Remanejamento:moved.id});
      check('Remanejamento persistido com dois itens e efetivo preservado',moved.somaVariacoes===0&&moved.itens.length===2
        && moved.itens.some(x=>x.composicaoId===a[0]&&x.quantidadeResultante===39)&&moved.itens.some(x=>x.composicaoId===a[1]&&x.quantidadeResultante===35));
      const link=`/api/vinculos-contratuais/${b.quant07Vinculo}/alocacoes`;
      const placement=await get(link+'/posicao',{data:b.quant07D3});
      check('Pessoa permanece na composição original, sem nova movimentação',placement.composicaoBaseId===a[0]
        && hash(await get(link+'/eventos'))===b.quant07AlocacaoEventosSha);
      await verifyPosition(b.quant07D3,74,a,39,35);await oldPhotos();
    }else{
      check('Somente concessão do próprio roteiro, já usada, será revogada',b.quant07Permissao>0&&b.quant07Reducao26>0&&b.quant07Remanejamento>0&&!b.quant07Revogada);
      const all=await get(perms),ours=all.find(x=>x.id===b.quant07Permissao);
      check('Escopo confirmado antes de revogar',ours?.ativa&&ours.contratoId===b.quant07Contrato&&ours.usuarioBeneficiarioId===fixture.accounts.gestor_local.id);
      const usage=await get(`${perms}/${ours.id}/usos`);
      check('A redução deixou exatamente um uso auditável',usage.length===1&&usage[0].id>0);
      const r=await post(`${perms}/${ours.id}/revogar`,{motivo:'Conclusão do caso sintético QUANT07; preservação da trilha.'},200);
      if(r.dto.id===ours.id&&!r.dto.ativa)recordBusiness({quant07Revogada:true});
      const now=await get(perms);
      check('Revogação conserva o uso e as demais permissões, sem apagar histórico',r.dto.id===ours.id&&!r.dto.ativa&&hash(await get(`${perms}/${ours.id}/usos`))===hash(usage)
        && hash(now.filter(x=>x.id!==ours.id))===hash(all.filter(x=>x.id!==ours.id)));
    }
  }
  report.conditions={phase,contract:readBusiness().quant07Contrato,civilClock:true,actualBrowserLogin:true,noSQL:true};
  await screenshot(phase);
},{accountName:admin?'fixture_admin':'gestor_local',reuse:false});
