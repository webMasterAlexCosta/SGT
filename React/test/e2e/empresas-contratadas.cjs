const { execute } = require('./ambiente-real.cjs');
const { contratoDaFixture } = require('./contexto-fixture.cjs');

execute('empresas-contratadas', async ({ page, context, fixture, report, check, open, screenshot }) => {
  const requests = [];
  page.on('request', request => {
    const url = new URL(request.url());
    if (url.pathname.startsWith('/api/')) requests.push({ method: request.method(), path: url.pathname });
  });
  await open('/governanca-terceiros');
  check('A URL anterior não mantém o usuário na funcionalidade removida', new URL(page.url()).pathname !== '/governanca-terceiros');
  check('A página antiga não é renderizada', await page.getByRole('heading', { name: 'Governança do ciclo de terceiros', exact: true }).count() === 0);
  check('O item antigo não existe no menu', await page.getByRole('button', { name: 'Governança do ciclo', exact: true }).count() === 0);

  await open('/empresas-contratadas');
  await page.getByRole('heading', { name: 'Empresas contratadas', exact: true }).waitFor();
  await page.getByText('Carregando empresas...', { exact: true }).waitFor({ state: 'hidden' });
  const canonical = ['/api/contratos', '/api/fornecedores'];
  check('Empresa e contratos são consultados pelas APIs canônicas', canonical.every(path => report.http.some(response => response.method === 'GET' && response.path === path && response.status === 200)));
  // O primeiro contrato já foi publicado por contrato-publicacao.cjs. A associação
  // deve continuar disponível para o outro rascunho real observado, sem reabrir o publicado.
  const published = contratoDaFixture(fixture);
  const draft = contratoDaFixture(fixture, 'contrato_futuro');
  const read = async contract => {
    const response = await context.request.get(new URL(contract.api, page.url()).href);
    check(`Contrato observado ${contract.id} consultado no Java real`, response.status() === 200);
    return (await response.json()).data;
  };
  const publishedDto = await read(published);
  const draftDto = await read(draft);
  check('Pré-condições causais: primeiro contrato publicado e segundo ainda em rascunho', publishedDto.estadoNegocial === 'PUBLICADO' && draftDto.estadoNegocial === 'RASCUNHO');
  const association = page.getByRole('combobox', { name: 'Contrato a vincular à empresa', exact: true });
  check('Contrato sintético autorizado continua disponível para associação', await association.locator(`option[value="${draft.id}"]`).count() === 1);
  check('Contrato já publicado não é oferecido para nova associação', await association.locator(`option[value="${published.id}"]`).count() === 0);
  check('Cadastro conserva paginação visual de cinco', await page.getByRole('navigation', { name: 'Paginação de empresas', exact: true }).getByText(/5 por página/).isVisible());
  check('A primeira página não excede cinco empresas', await page.getByRole('table').locator('tbody tr').count() <= 5);
  check('As colunas exclusivas de risco foram retiradas', await page.getByRole('columnheader', { name: /Risco|Monitoramento|Próxima revisão/ }).count() === 0);
  check('Avaliações e processos não estão disponíveis no cadastro', await page.getByRole('button', { name: /Risco contínuo|Registrar avaliação|Criar processo/ }).count() === 0);
  await page.getByRole('searchbox', { name: 'Pesquisar empresas', exact: true }).fill('SINTETICO-SEM-REGISTRO-NESTA-FIXTURE');
  await page.getByText('Nenhum registro encontrado para esta pesquisa.', { exact: true }).waitFor();
  check('Pesquisa sem correspondência apresenta estado vazio explícito', await page.getByText('Nenhum registro encontrado para esta pesquisa.', { exact: true }).isVisible());
  check('Nenhuma API financeira ou da governança removida é consultada', requests.every(request => !/\/(governanca-terceiros|tabelas-tarifarias|folhas-horas|faturas)(?:\/|$)/.test(request.path)));
  check('O cenário cadastral de consulta não envia mutações', requests.filter(request => request.path.startsWith('/api/fornecedores')).every(request => request.method === 'GET'));
  report.limits = ['Dados sintéticos; consulta e navegação somente.', 'O cenário não cria nem altera empresa/contrato.', 'Os fluxos de escrita e 5+1 têm testes React com transporte controlado; não são prova de persistência Java.'];
  await screenshot('cadastro-empresa-sem-modulo-removido');
});
