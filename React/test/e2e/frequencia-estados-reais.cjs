'use strict';
const fs=require('node:fs');const path=require('node:path');const {execute}=require('./ambiente-real.cjs');const {baseFrontend}=require('./contexto-fixture.cjs');
const unwrap=x=>x?.data??x;
const nextDay=(iso,n)=>{const d=new Date(iso+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+n);return d.toISOString().slice(0,10);};
const syntheticCpf=index=>{let s=String(930550000+index);for(const n of [9,10]){let sum=0;for(let i=0;i<n;i++)sum+=Number(s[i])*(n+1-i);const v=(sum*10)%11;s+=v===10?'0':String(v);}return s;};
execute('frequencia-estados-reais',async({page,context,fixture,check,open,confirmPassword,recordBusiness,report,screenshot})=>{
 const b=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json'))),base=baseFrontend();
 const cid=b.jobs200ContratoId,date=nextDay(b.jobs200DataInicio,8),people=b.jobs200People.slice(-5).reverse();
 const codes=['PRESENCA','PRESENCA_PARCIAL','FALTA'];
 check('Data própria fora dos jobs e dos dias do atestado, sem reenvio',fixture.synthetic&&people.length===5&&date<fixture.dates.today
   && ![b.jobs200GradeData,b.jobs200CancelData,...(b.atestado07Dates??[])].includes(date)&&!b.freqEstados07?.verified);
 await open('/frequencia');
 const read=async(route,params)=>{const r=await context.request.get(base+route,{params,timeout:10000});check('GET real '+route,r.status()===200);return unwrap(await r.json());};
 const common={contratoId:String(cid),lotacaoId:String(fixture.ids.lotacao),jornadaId:String(fixture.ids.jornada)};
 const freq=()=>read('/api/frequencias',{...common,data:date,size:100});
 const existing=await freq(),ids=[...(b.freqEstados07?.ids??[])];
 check('O recorte contém apenas o prefixo confirmado, sem regravar nenhum estado',existing.totalElements===ids.length&&ids.length<3
   && ids.every((id,i)=>existing.content.some(x=>x.id===id&&x.status===codes[i]&&x.vinculoContratualId===people[i].vinculoId)));
 await page.getByLabel('Data de referência da frequência',{exact:true}).fill(date);
 await page.getByRole('combobox',{name:'Lotação',exact:true}).selectOption(common.lotacaoId);
 await page.getByRole('combobox',{name:'Turno',exact:true}).selectOption(common.jornadaId);
 const active=(await read('/api/contratos')).filter(x=>x.ativo===true),idx=active.findIndex(x=>x.id===cid);
 await page.getByRole('button',{name:'Selecionar contratos',exact:true}).click();
 const boxes=page.locator('[data-tour="freq-filters"]').getByRole('checkbox');
 check('Recorte fixado no único contrato de200',idx>=0&&await boxes.count()===active.length+1);
 await boxes.first().click();await boxes.nth(idx+1).check();await page.getByRole('button',{name:'Selecionar contratos',exact:true}).click();
 const launch=page.getByRole('button',{name:'Lançar por vínculo',exact:true});
 for(let i=ids.length;i<codes.length;i++){
  const person=people[i],code=codes[i];await launch.click();
  const modal=page.getByRole('dialog',{name:'Lançar por vínculo',exact:true});
  const selection=modal.getByRole('button',{name:'Selecionar vínculo '+person.vinculoId,exact:true});await selection.click();
  await modal.getByText(/Posição validada para/).waitFor();
  await modal.getByLabel('Status da frequência',{exact:true}).selectOption(code);
  await modal.getByLabel('Observação do lançamento',{exact:true}).fill('Homologação sintética de '+code+' sem conversão de estado.');
  if(code==='PRESENCA_PARCIAL'){
   await modal.getByLabel('Horas trabalhadas',{exact:true}).fill('4.5');
   await modal.getByLabel('Horário de saída',{exact:true}).fill('12:30');
  }
  await modal.getByLabel('Justificativa da retificação',{exact:true}).fill('Registro de homologação explícito no recorte autorizado.');
  const pending=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname==='/api/frequencias',{timeout:25000});pending.catch(()=>{});
  await modal.getByRole('button',{name:'Confirmar lançamento',exact:true}).click();await confirmPassword();
  const result=await pending,body=await result.json(),dto=unwrap(body),payload=result.request().postDataJSON();
  if(result.status()===200&&dto.id>0){ids.push(dto.id);recordBusiness({freqEstados07:{date,contract:cid,ids:[...ids]}});}
  check('DTO/payload confirmam estado '+code+' na mesma identidade e data',result.status()===200&&dto.status===code&&dto.pessoaId===person.pessoaId
    && dto.vinculoContratualId===person.vinculoId&&payload.vinculoContratualId===person.vinculoId&&dto.dataReferencia===date,
    {status:result.status(),code:body.code??null});
  await modal.waitFor({state:'hidden'});
 }
 const selected=(await freq()).content;
 check('Somente três frequências persistidas; nenhum FALTA para quem ficou sem registro',selected.length===3&&selected.every(x=>ids.includes(x.id))
   && codes.every((code,i)=>selected.some(x=>x.status===code&&x.vinculoContratualId===people[i].vinculoId))
   && people.slice(3).every(x=>!selected.some(y=>y.vinculoContratualId===x.vinculoId)));
 const indicators=await read('/api/painel/efetivo',{...common,referencia:date,periodo:'DIARIO',compararAnoAnterior:false});
 const v=indicators.resumoAtual;
 check('Indicador real distingue dois presentes, uma falta e 197 sem registro',indicators.fim===date&&v.efetivoPrevisto===200&&v.efetivoAlocado===200
   && v.registrosLancados===3&&v.presentes===2&&v.ausentes===1&&v.pendentesLancamento===197&&v.outrosRegistros===0,
   {expected:[200,3,2,1,197],actual:[v.efetivoAlocado,v.registrosLancados,v.presentes,v.ausentes,v.pendentesLancamento]});
 const titles=['Confirmar Presença Integral para ','Registrar Presença Parcial (Atraso/Saída Antecipada) para ','Registrar Falta Integral para '];
 const name=i=>'SINTETICO JOBS200 FINAL07 '+String(people[i].index).padStart(3,'0');
 const row=i=>page.locator('[class*="linhaVirtualBase"]').filter({has:page.getByTitle('Nome Completo: '+name(i),{exact:true})});
 const search=page.getByLabel('Buscar colaborador por nome ou CPF',{exact:true});
 for(let i=0;i<codes.length;i++){
  await search.fill(syntheticCpf(people[i].index));
  await row(i).getByText('Salvo',{exact:true}).waitFor({timeout:15000});
  const current=row(i).getByTitle(titles[i]+name(i),{exact:true});
  check('A grade real marca o estado correto de '+codes[i],await current.isVisible()&&/botaoAtivo/.test(await current.getAttribute('class')));
 }
 await search.fill(syntheticCpf(people[3].index));
 await row(3).getByText('Pendente',{exact:true}).waitFor();
 check('A grade distingue o colaborador sem registro, sem marcá-lo como salvo ou falta',await row(3).getByText('Salvo',{exact:true}).count()===0
   && !/botaoAtivo/.test(await row(3).getByTitle(titles[2]+name(3),{exact:true}).getAttribute('class')));
 recordBusiness({freqEstados07:{date,contract:cid,ids,verified:true}});report.conditions={date,contract:cid,vinculos:people.map(p=>p.vinculoId),counts:v};
 await screenshot('p-pp-falta-sem-registro');
},{accountName:'gestor_local',reuse:false});
