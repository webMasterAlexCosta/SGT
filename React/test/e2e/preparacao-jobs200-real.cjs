// Dez admissões sintéticas por execução; somente API real após login UI.
const fs=require('node:fs'),path=require('node:path');
const {execute}=require('./ambiente-real.cjs');const {baseFrontend}=require('./contexto-fixture.cjs');
const first=Number(process.env.SGT_JOBS200_FIRST);if(!Number.isInteger(first)||first<1||first>191||(first-1)%10)throw new Error('Lote exige início1..191 em passos10.');
const read=()=>JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json')));
const unwrap=x=>x&&Object.hasOwn(x,'data')?x.data:x;
const fresh=path.basename(process.env.SGT_TEST_RUNTIME)==='runtime-frontend55-01';
const marker=fresh?'FINAL07':'FINAL03';
const cpf=index=>{let s=String((fresh?930550000:930530000)+index);for(const length of [9,10]){let sum=0;for(let i=0;i<length;i++)sum+=Number(s[i])*(length+1-i);let n=(sum*10)%11;s+=n===10?'0':String(n);}return s;};
execute('preparacao-jobs200-real-'+String(first).padStart(3,'0'),async({page,context,fixture,account,check,report,open,recordBusiness})=>{
 await open('/pessoas');let b=read();check('Contrato exclusivo pronto antes da massa',b.jobs200ContratoPronto===true&&b.jobs200ContratoId!==fixture.ids.contrato_rascunho&&(!fresh||b.jobs200CorpusMarker===marker));
 let people=b.jobs200People||[];check('Lote continua exatamente após o prefixo durável, sem operação incerta',people.length===first-1&&!b.jobs200PendingIndex);
 const base=baseFrontend(),cr=await context.request.get(base+'/api/contratos/'+b.jobs200ContratoId),contract=unwrap(await cr.json());
 check('Capacidade original200 preservada e contrato publicado',cr.status()===200&&contract.estadoNegocial==='PUBLICADO'&&contract.efetivoOriginalPublicado===200&&contract.efetivoPrevisto===200);
 const csrf=unwrap(await(await context.request.get(base+'/api/autenticacao/csrf')).json());
 for(let index=first;index<first+10;index++){
  recordBusiness({jobs200PendingIndex:index});
  const payload={pessoa:{nome:'SINTETICO JOBS200 '+marker+' '+String(index).padStart(3,'0'),cpf:cpf(index)},contratoId:b.jobs200ContratoId,dataInicio:b.jobs200DataInicio,matricula:(fresh?'J2007-':'J200-')+String(index).padStart(3,'0'),composicaoId:b.jobs200ComposicaoId};
  const r=await context.request.post(base+'/api/vinculos-contratuais',{headers:{[csrf.headerName]:csrf.token,'X-User-Password':account.password,Origin:base},data:payload});const envelope=await r.json(),d=unwrap(envelope);
  report.http.push({method:'POST',path:'/api/vinculos-contratuais',status:r.status(),driver:'APIRequestContext real — preparação10porlote'});
  check('Admissão real201 de índice'+index,r.status()===201&&d.id>0&&d.pessoaId>0&&d.contratoId===b.jobs200ContratoId&&d.dataInicio===b.jobs200DataInicio&&d.composicaoInicialId===b.jobs200ComposicaoId,{status:r.status(),code:envelope.code||null});
  people=[...people,{index,vinculoId:d.id,pessoaId:d.pessoaId,matricula:d.matricula}];recordBusiness({jobs200People:people,jobs200PendingIndex:null});
 }
 check('Dez IDs novos únicos e prefixo durável íntegro',people.length===first+9&&new Set(people.map(x=>x.vinculoId)).size===people.length&&new Set(people.map(x=>x.pessoaId)).size===people.length);
 report.scope='PREPARATION_ONLY_NOT_ACCEPTANCE';report.conditions={first,last:first+9,completed:people.length,capacity:200,contract:b.jobs200ContratoId,serial:true,noSQL:true,noRetries:true};
},{accountName:'gestor_local',reuse:true});
