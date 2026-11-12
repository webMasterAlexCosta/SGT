// Executar pelo cua_repl com o Tab documentado e laboratório explicitamente informado.
// A API/CSRF/perfis são verificados separadamente; este cenário não cria mocks nem muda credenciais.
export default async function verificarDashboard(tab, { laboratorio } = {}) {
  if (!laboratorio) throw new Error('Informe o diretório do laboratório isolado autorizado.');
  const fs = await import('node:fs/promises');
  const path = await import('node:path');
  const meta = JSON.parse(await fs.readFile(path.join(laboratorio,'metadata.json'),'utf8'));
  const oraculo = JSON.parse(await fs.readFile(path.join(laboratorio,'smoke-api.json'),'utf8'));
  const exigir = (valor,mensagem) => { if(!valor) throw new Error(mensagem); };
  const url = await tab.url();
  exigir([meta.api_url,meta.vite_url].some(base=>new URL(base).hostname==='127.0.0.1' && url?.startsWith(base+'/')),'A aba deve estar no laboratório autorizado e autenticada.');
  const relatorio = { natureza:'UI real sobre API PostgreSQL; dados sintéticos', periodos:[], verificacoes:[], exportacoes:[] };
  const nomes={DIARIO:'Diário',SEMANAL:'Semanal',MENSAL:'Mensal',TRIMESTRAL:'Trimestral',ANUAL:'Anual'};
  await tab.playwright.getByRole('heading',{name:'Desempenho operacional',exact:true}).waitFor({state:'visible'});
  // O estado inicial e a identidade das ações vêm do DOM da aplicação, nunca de estado React privado.
  await tab.playwright.domSnapshot();
  await tab.playwright.getByLabel('Data de referência',{exact:true}).fill(oraculo.periodos[0].fim);
  for(const p of oraculo.periodos) {
    await tab.playwright.getByRole('button',{name:nomes[p.tipo],exact:true}).click();
    const kpi=tab.playwright.getByRole('button',{name:`Total de registros: ${new Intl.NumberFormat('pt-BR').format(p.registros)}`,exact:true});
    await kpi.waitFor({state:'visible'});
    exigir((await tab.playwright.getByRole('button',{name:/Fora do prazo:/}).innerText()).includes(new Intl.NumberFormat('pt-BR').format(p.tempestividade.fora)), 'Tempestividade diverge da API.');
    relatorio.periodos.push({tipo:p.tipo,registros:p.registros,inicio:p.inicio,fim:p.fim});
  }
  exigir(await tab.playwright.getByRole('heading',{name:'Indicadores de desempenho e metas',exact:true}).isVisible(),'Barras ausentes.');
  exigir(await tab.playwright.getByRole('heading',{name:'Funil de cobertura',exact:true}).isVisible(),'Funil ausente.');
  const total=tab.playwright.getByRole('button',{name:/Total de registros:/});
  await total.click();await tab.playwright.getByRole('dialog').waitFor({state:'visible'});await tab.playwright.domSnapshot();
  exigir(await tab.playwright.getByRole('heading',{name:'Registros de origem',exact:true}).isVisible(),'Origem real indisponível.');
  await tab.playwright.getByRole('button',{name:'Fechar análise detalhada',exact:true}).press('Escape');await tab.playwright.getByRole('dialog').waitFor({state:'hidden'});
  exigir(await total.evaluate(el=>el===document.activeElement),'Foco não retorna à origem.');
  await tab.playwright.getByRole('button',{name:'Mensal',exact:true}).click();
  await tab.playwright.getByRole('button',{name:`Total de registros: ${oraculo.periodos.find(p=>p.tipo==='MENSAL').registros}`,exact:true}).waitFor({state:'visible'});
  const mapa=tab.playwright.getByRole('region',{name:'Mapa de calor'});
  // section nomeada é uma região semântica; a célula detalha pendências, sem simular lista de faltas.
  const celula=mapa.locator('tbody button').first();await celula.click();await tab.playwright.getByRole('dialog').waitFor({state:'visible'});
  exigir((await tab.playwright.getByRole('dialog').innerText()).includes('esperados'),'Célula não explica a base de elegibilidade.');
  await tab.playwright.getByRole('button',{name:'Fechar análise detalhada',exact:true}).press('Escape');await tab.playwright.getByRole('dialog').waitFor({state:'hidden'});
  exigir(await celula.evaluate(el=>el===document.activeElement),'Foco da célula não restituído.');
  const icones=await tab.playwright.locator('footer [aria-label="Tecnologias utilizadas"] img').count();exigir(icones===7,'Footer deve conter sete ícones locais.');
  const overflow=await tab.playwright.evaluate(()=>document.documentElement.scrollWidth>innerWidth);exigir(!overflow,'Overflow horizontal no desktop.');
  relatorio.verificacoes.push('cinco períodos reconciliados com API','KPIs e metas propostas','funil real','origem mínima','heatmap de pendências','ESC e foco','sete ícones locais','sem overflow horizontal');
  const exports=path.join(laboratorio,'exportacoes');await fs.mkdir(exports,{recursive:true,mode:0o700});
  for(const [nome,ext,assinatura] of [['Exportar XLSX','xlsx','PK'],['PDF','pdf','%PDF-']]) {
    const aguardar=tab.playwright.waitForEvent('download',{timeoutMs:30000});await tab.playwright.getByRole('button',{name:nome,exact:true}).click();
    const download=await aguardar;const arquivo=await download.path();exigir(Boolean(arquivo),'Arquivo não disponível.');
    const bytes=await fs.readFile(arquivo);exigir(bytes.subarray(0,assinatura.length).toString()===assinatura,`Assinatura ${ext} incorreta.`);
    const destino=path.join(exports,'dashboard-real.'+ext);await fs.copyFile(arquivo,destino);
    relatorio.exportacoes.push({tipo:ext,bytes:bytes.length});
  }
  await fs.writeFile(path.join(laboratorio,'e2e-dashboard-ui.json'),JSON.stringify(relatorio,null,2));
  return relatorio;
}
