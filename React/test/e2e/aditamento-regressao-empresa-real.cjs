'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend, contratoDaFixture } = require('./contexto-fixture.cjs');
const phase = process.env.SGT_ADITAMENTO_EMPRESA_PHASE;
if (!['retificar', 'retornar', 'inativar'].includes(phase)) throw new Error('Fase de regressão de empresa obrigatória.');
const hash = x => crypto.createHash('sha256').update(JSON.stringify(x)).digest('hex');
const plus = (d, n) => { const t = new Date(d + 'T12:00:00Z'); t.setUTCDate(t.getUTCDate()+n); return t.toISOString().slice(0,10); };
const unwrap = value => value?.data ?? value;

execute('aditamento-regressao-empresa-real-' + phase, async ({ page, context, fixture, account, report, check, open, confirmPassword, recordBusiness, screenshot }) => {
  const runtime = process.env.SGT_TEST_RUNTIME;
  const b = JSON.parse(fs.readFileSync(path.join(runtime,'private/e2e-business.json')));
  const contract = contratoDaFixture(fixture), origin = baseFrontend(), api = contract.api, endpoint = api + '/aditamentos';
  check('Atores e troca anterior pertencem ao corpus sintético', fixture.synthetic && account.id===fixture.accounts.gestor_local.id && b.aditamento08Empresa>0);
  await open('/contratos');
  const get = async (route, params) => {
    const r = await context.request.get(origin+route,{params,timeout:12000});
    check('GET real: '+route,r.status()===200,{status:r.status()}); return unwrap(await r.json());
  };
  const previous = await get(endpoint), versions = await get(api+'/versoes');
  const original = previous.find(x=>x.id===b.aditamento08Empresa);
  const providers = await get('/api/fornecedores');
  const empresaA = providers.find(x=>x.id===fixture.ids.fornecedor), empresaB=providers.find(x=>x.id===b.fornecedorCrudFinal02Id);
  const retificando = phase==='retificar';
  const oldDate = original?.dataEfetiva, date = retificando ? oldDate : plus(oldDate,phase==='retornar'?1:2);
  const marker = 'SGT-TESTE-ADI08-'+phase.toUpperCase();
  const oldK = new Date().toISOString();
  const position = async (d,k) => (await get(api+'/posicao-temporal',{data:d,...(k?{conhecidoAte:k}:{})})).versaoContratual;
  const posBefore = await position(date,oldK);
  check('Empresa anterior e empresa nova permanecem identificadas no laboratório', original?.novoFornecedorId===empresaB?.id && empresaB?.ativo
    && empresaA?.ativo && empresaA.id!==empresaB.id && !previous.some(x=>x.identificacao===marker));
  check('Fases são separadas e nenhum comando é repetido', retificando ? !b.aditamento08EmpresaRetificada
    : b.aditamento08EmpresaRetificada && (phase==='retornar' ? !b.aditamento08EmpresaRetorno : b.aditamento08EmpresaRetorno && !b.aditamento08Inativada));
  const selected = phase==='inativar' ? empresaB : empresaA;
  const view = await get(api);
  if (phase==='retornar') check('A é a empresa de hoje, enquanto B vigora antes do retorno proposto', view.fornecedorId===empresaA.id && posBefore.fornecedorId===empresaB.id);
  if (phase==='inativar') check('Inativação de B usa data posterior ao retorno de A, sem desativar a empresa daquela data', posBefore.fornecedorId===empresaA.id);
  await page.getByRole('button',{name:contract.botaoAbrir,exact:true}).click();
  const detail=page.getByRole('dialog',{name:contract.titulo,exact:true});
  await detail.getByRole('tab',{name:'Aditamentos',exact:true}).click();
  if(retificando){
    const card=detail.locator('article').filter({hasText:original.identificacao+' · v1'});
    check('Histórico identifica unicamente o ato que será retificado',await card.count()===1);
    await card.getByRole('button',{name:'Retificar',exact:true}).click();
  }
  const form=detail.getByRole('heading',{name:retificando?'Retificação de aditamento':'Novo aditamento',exact:true}).locator('..');
  if(!retificando){
    await form.getByLabel(/^Identificação/).fill(marker);
    await form.getByLabel(/^Data efetiva/).fill(date);
    await form.getByLabel('Data do documento',{exact:true}).fill(fixture.dates.today);
    await form.getByLabel('Data da assinatura',{exact:true}).fill(fixture.dates.today);
  }
  await form.getByLabel('Referência SEI',{exact:true}).fill(marker);
  await form.getByLabel(/^Justificativa/).fill('Fonte documental sintética; reteste de integridade empresarial do SGT.');
  const toggle=form.getByRole('checkbox',{name:retificando?/empresa.*retificação/i:'Alterar empresa neste aditamento'});
  await toggle.check();
  await form.getByRole('combobox',{name:'Nova empresa contratada',exact:true}).selectOption(String(selected.id));
  await form.getByLabel('Nome fantasia da nova empresa').fill('Documentação sintética do ensaio de empresa');
  if(retificando){
    await toggle.uncheck();
    check('Desistir da outra empresa mostra os dados documentais originais',await form.getByText(/Dados da empresa preservados do ato original/).isVisible());
  }
  const watch=route=>{const p=page.waitForResponse(r=>new URL(r.url()).pathname===route&&r.request().method()==='POST');p.catch(()=>{});return p;};
  const waiting=watch(endpoint+'/previa');
  await form.getByRole('button',{name:'Calcular prévia',exact:true}).click();
  const previewResponse=await waiting, preview=unwrap(await previewResponse.json()), draft=previewResponse.request().postDataJSON();
  check('Prévia real admite o documento na data efetiva',previewResponse.status()===200&&preview.tokenPrevia&&draft.dataEfetiva===date,{status:previewResponse.status()});
  const fields=['novoFornecedorId','novoCnpjEmpresa','novaRazaoSocialEmpresa','novoNomeFantasiaEmpresa'];
  if(retificando) check('Prévia conserva exatamente os quatro dados do ato anterior',fields.every(k=>draft[k]===original[k]));
  else check('Prévia contém identidade ativa escolhida e nenhum dado da empresa anterior',draft.novoFornecedorId===selected.id&&draft.novoCnpjEmpresa===selected.cnpj&&draft.novaRazaoSocialEmpresa===selected.razaoSocial);
  const confirm=form.getByRole('button',{name:'Confirmar com esta prévia'});
  await form.getByRole('heading',{name:'Prévia calculada no servidor'}).waitFor();
  const commitPath=retificando?endpoint+'/'+original.id+'/retificar':endpoint;
  let changed=false;
  const setActive=async ativo=>{
    const csrf=await get('/api/autenticacao/csrf');
    const r=await context.request.put(origin+'/api/fornecedores/'+empresaB.id,{data:{razaoSocial:empresaB.razaoSocial,cnpj:empresaB.cnpj,email:empresaB.email,ativo},
      headers:{Origin:origin,[csrf.headerName]:csrf.token,'X-User-Password':account.password},timeout:12000});
    const dto=unwrap(await r.json());
    if(r.status()===200&&dto.id===empresaB.id&&dto.ativo===ativo){changed=!ativo;recordBusiness({aditamento08FornecedorTemporariamenteInativo:changed});}
    check(ativo?'Empresa sintética restaurada':'Empresa selecionada inativada após a prévia, pela API real',r.status()===200&&dto.id===empresaB.id&&dto.ativo===ativo);
  };
  try{
    if(phase==='inativar') await setActive(false);
    const pending=watch(commitPath);
    await confirm.click(); await confirmPassword();
    const r=await pending, envelope=await r.json(), result=unwrap(envelope), sent=r.request().postDataJSON();
    check('Confirmação enviou o mesmo ato e token da prévia',sent.tokenPrevia===preview.tokenPrevia&&fields.every(k=>sent[k]===draft[k]));
    if(phase==='inativar'){
      if(r.status()===201&&result.id>0) recordBusiness({aditamento08InativadaInesperadoId:result.id});
      check('Java recusa 409 por inatividade, apesar da prévia ainda visível',r.status()===409&&/inativo/i.test(envelope.message||''),{status:r.status(),code:envelope.code});
      check('Recusa preserva ato, fotografias, posição e formulário',hash(await get(endpoint))===hash(previous)&&hash(await get(api+'/versoes'))===hash(versions)
        && hash(await position(date,oldK))===hash(posBefore)&&await form.getByLabel(/^Identificação/).inputValue()===marker);
    }else{
      if(r.status()===(retificando?200:201)&&result.id>0) recordBusiness({[retificando?'aditamento08EmpresaRetificada':'aditamento08EmpresaRetorno']:result.id});
      check('Java grava exatamente uma nova versão/ato no contrato, com autoria',r.status()===(retificando?200:201)&&result.usuarioResponsavelId===account.id&&result.contratoId===contract.id&&result.dataEfetiva===date,{status:r.status(),code:envelope.code});
      const now=await position(date), historic=await position(date,oldK);
      const after=await get(api+'/versoes');
      check('Fotografias anteriores permanecem e o corte K antigo mantém a empresa',after.length===versions.length+1&&versions.every(x=>hash(after.find(y=>y.id===x.id))===hash(x))&&historic.fornecedorId===posBefore.fornecedorId);
      if(retificando) check('Retificação mantém B, seu CNPJ e nomes; ato original fica retificado',result.versaoInstrumento===2&&fields.every(k=>result[k]===original[k])
        && now.fornecedorId===empresaB.id&&now.cnpjEmpresa===original.novoCnpjEmpresa&&now.razaoSocialEmpresa===original.novaRazaoSocialEmpresa&&now.nomeFantasiaEmpresa===original.novoNomeFantasiaEmpresa
        && (await get(endpoint)).find(x=>x.id===original.id)?.situacao==='RETIFICADO');
      else check('A empresa vigente hoje pode retornar após B na data D',now.fornecedorId===empresaA.id&&now.cnpjEmpresa===empresaA.cnpj&&(await position(plus(date,-1))).fornecedorId===empresaB.id);
    }
  }finally{if(changed)await setActive(true);}
  if(phase==='inativar')recordBusiness({aditamento08Inativada:true});
  report.conditions={phase,contract:contract.id,effective:date,actualBrowserAndJava:true};
  await screenshot(phase);
},{accountName:'gestor_local',reuse:false});
