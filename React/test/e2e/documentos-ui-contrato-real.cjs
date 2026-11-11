// Regressão visual/contratual de leitura; não cria nem retifica documentos.
const fs = require('node:fs');
const path = require('node:path');
const { baseFrontend, contratoDaFixture } = require('./contexto-fixture.cjs');
const { execute } = require('./ambiente-real.cjs');

execute('documentos-ui-contrato-real', async ({ page, context, fixture, report, check, open, screenshot }) => {
  report.scope='DOCUMENT_PANEL_UI_REGRESSION_NOT_AN_ORIGINAL_DRIVER';
  const b=JSON.parse(fs.readFileSync(path.join(process.env.SGT_TEST_RUNTIME,'private/e2e-business.json')));
  const contract=contratoDaFixture(fixture);await open('/contratos');
  const response=await context.request.get(baseFrontend()+contract.api+'/documentos');const docs=(await response.json()).data;
  const draft=docs.find(item=>item.id===b.documentoId);const current=docs.find(item=>item.id===b.documentoRetificadoE2EId);
  check('Rascunho e versão publicada reais persistem em estados distintos',response.status()===200&&draft?.estado==='RASCUNHO'&&current?.estado==='PUBLICADO');
  await page.getByRole('button',{name:contract.botaoAbrir,exact:true}).click();
  const detail=page.getByRole('dialog',{name:contract.titulo,exact:true});
  const navigation=detail.getByRole('navigation',{name:'Páginas de documentos',exact:true});
  await navigation.getByText(new RegExp(`${docs.length} registros$`)).waitFor();
  const draftCard=detail.locator('article').filter({hasText:draft.titulo});await draftCard.waitFor();
  const form=detail.locator('form').filter({has:page.getByRole('heading',{name:'Registrar documento',exact:true})});
  const title=form.getByLabel(/^Título/);await title.scrollIntoViewIfNeeded();
  const styles=await title.evaluate(input=>{
    const style=getComputedStyle(input);const root=getComputedStyle(document.documentElement);
    return {borderTopWidth:style.borderTopWidth,borderTopStyle:style.borderTopStyle,padding:style.padding,
      legacyBorder:root.getPropertyValue('--cor-borda').trim(),legacySurface:root.getPropertyValue('--cor-superficie').trim(),
      currentBorder:root.getPropertyValue('--cor-borda-sutil').trim(),currentSurface:root.getPropertyValue('--cor-fundo-elevado').trim()};
  });
  const actions={retificar:await draftCard.getByRole('button',{name:'Retificar SEI',exact:true}).count(),substituir:await draftCard.getByRole('button',{name:'Nova versão/anexo',exact:true}).count()};
  report.uiDiagnostic={draftId:draft.id,currentId:current.id,draftActions:actions,titleComputedStyle:styles,no_document_mutation:true};
  await screenshot('estados-e-controles');
  const currentCard=detail.locator('article').filter({hasText:current.titulo}).filter({hasText:current.referenciaSei});
  for(let step=0;step<Math.ceil(docs.length/5)&&!await currentCard.count();step++){
    check('Fonte publicada permanece acessível pela paginação real',await navigation.getByRole('button',{name:'Próxima',exact:true}).isEnabled());
    const number=Number((await navigation.innerText()).match(/Página (\d+) de/)[1]);
    await navigation.getByRole('button',{name:'Próxima',exact:true}).click();await navigation.getByText(new RegExp(`Página ${number+1} de`)).waitFor();
  }
  await currentCard.waitFor();
  const publishedActions={retificar:await currentCard.getByRole('button',{name:'Retificar SEI',exact:true}).count(),substituir:await currentCard.getByRole('button',{name:'Nova versão/anexo',exact:true}).count()};
  report.uiDiagnostic.publishedActions=publishedActions;
  check('Fonte publicada preserva versionamento, detalhes e download',publishedActions.retificar===1&&publishedActions.substituir===1&&await currentCard.getByRole('button',{name:'Detalhes',exact:true}).isEnabled()&&await currentCard.getByRole('button',{name:'Baixar',exact:true}).isEnabled());
  await currentCard.scrollIntoViewIfNeeded();await screenshot('versao-publicada-com-acoes');
  const issues=[];
  if(actions.retificar!==0||actions.substituir!==0)issues.push('Rascunho oferece comandos que o contrato Java limita a PUBLICADO.');
  if(!(parseFloat(styles.borderTopWidth)>=1)||styles.borderTopStyle==='none')issues.push('Campo documental sem borda visível; verificar tokens CSS vigentes.');
  report.detectedIssues=issues;
  check('Ações documentais e bordas respeitam o contrato vigente',issues.length===0,{issues});
});
