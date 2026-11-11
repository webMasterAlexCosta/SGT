const { baseFrontend } = require('./contexto-fixture.cjs');
/**
 * E2E sintético real do fluxo canônico separado. Ordem obrigatória:
 * primeiro OP, depois retificação GL, depois recusa real de nova escrita OP.
 * Não repete automaticamente uma fase com escrita. Não altera fonte ou Java.
 * Usa credenciais privadas somente pelo harness; não registra headers/cookies.
 */
const fs = require('node:fs');
const path = require('node:path');
const { execute } = require('./ambiente-real.cjs');
const unwrap = value => value && Object.hasOwn(value, 'data') ? value.data : value;
const rows = value => Array.isArray(value) ? value : Array.isArray(value?.content) ? value.content : [];
const baseUrl = baseFrontend();
const businessPath = path.join(process.env.SGT_TEST_RUNTIME, 'private/e2e-business.json');
const observacoes = {
  op: 'Primeiro lançamento canônico sintético pelo Operacional — vínculo explícito e FOLGA declarada.',
  gl: 'Retificação canônica sintética pelo Gestor Local — conferência do lançamento Operacional.',
  recusado: 'Tentativa sintética Operacional após intervenção gerencial; o Java deve recusar.',
};

async function executarFase(fase) {
  const accountName = fase === 'gl' ? 'gestor_local' : 'audit_operacional';
  const name = `lancamento-vinculo-real-${fase}`;
  await execute(name, async ({ page, context, fixture, report, check, screenshot, open, confirmPassword, recordBusiness }) => {
    const business = JSON.parse(fs.readFileSync(businessPath, 'utf8'));
    const data = fixture.pending_vinculo_request.dataInicio;
    const vinculoId = Number(business.vinculoId), pessoaId = Number(business.pessoaId);
    const contratoId = Number(fixture.pending_vinculo_request.contratoId);
    const lotacaoId = Number(fixture.ids.lotacao), jornadaId = Number(fixture.ids.jornada);
    check('Fixture sintética identifica Pessoa, Vínculo, Contrato, Lotação, Jornada e data', fixture.synthetic === true && [vinculoId, pessoaId, contratoId, lotacaoId, jornadaId].every(Number.isSafeInteger) && /^\d{4}-\d{2}-\d{2}$/.test(data));
    report.conditions = { fase, accountName, dataReferencia: data, pessoaId, vinculoId, contratoId, lotacaoId, jornadaId, statusDeclarado: fase === 'escopo' ? null : 'FOLGA', scopeObservationOnly: fase === 'escopo', applicationSqlUsed: false, networkInterceptionUsed: false };
    report.screenshots = []; report.externalReads = [];
    const posts = [], posicoes = [];
    page.on('request', request => {
      const url = new URL(request.url());
      if (url.pathname === '/api/frequencias' && request.method() === 'POST') posts.push(request.postDataJSON());
      if (/^\/api\/vinculos-contratuais\/(?:pessoas\/\d+\/posicao|\d+\/alocacoes\/posicao)$/.test(url.pathname)) posicoes.push({ path: url.pathname, data: url.searchParams.get('data') });
    });
    const shot = async label => report.screenshots.push(await screenshot(label));
    const requestGet = async (pathname, params) => {
      const response = await context.request.get(baseUrl + pathname, { params });
      report.externalReads.push({ method: 'GET', path: pathname, params, status: response.status() });
      check(`GET independente ${pathname} respondeu200`, response.status() === 200, { status: response.status() });
      return unwrap(await response.json());
    };
    const lerFrequencia = async () => {
      const result = await requestGet('/api/frequencias', { data, lotacaoId: String(lotacaoId), jornadaId: String(jornadaId), page: '0', size: '1000' });
      check('Leitura independente completa a fixture dentro da página recebida', !result?.totalPages || Number(result.totalPages) <= 1);
      const encontrados = rows(result).filter(item => Number(item.vinculoContratualId) === vinculoId && item.dataReferencia === data);
      check('Leitura por identidade canônica não encontra duplicidade', encontrados.length <= 1);
      return encontrados[0] || null;
    };
    const resumo = item => item ? ({ id: item.id, pessoaId: item.pessoaId, vinculoContratualId: item.vinculoContratualId, alocacaoVinculoId: item.alocacaoVinculoId, empregadoId: item.empregado?.id, contratoId: item.contratoId, lotacaoId: item.lotacaoId, jornadaId: item.jornadaId, dataReferencia: item.dataReferencia, status: item.status, observacao: item.observacao, estadoNegocial: item.estadoNegocial, validadeOperacional: item.validadeOperacional, estadoAuditoria: item.estadoAuditoria, bloqueadaEdicaoOperacional: item.bloqueadaEdicaoOperacional, versaoNegocial: item.versaoNegocial }) : null;
    const esperarApi = (pathname, method = 'GET', predicate = () => true) => page.waitForResponse(response => {
      const url = new URL(response.url());
      return url.pathname === pathname && response.request().method() === method && predicate(url, response);
    }, { timeout: 30000 });
    const choose = async (locator, value) => {
      await locator.locator(`option[value="${value}"]`).waitFor({ state: 'attached', timeout: 15000 });
      if (await locator.inputValue() !== String(value)) await locator.selectOption(String(value));
    };

    await open('/frequencia');
    const dataInput = page.getByLabel('Data de referência da frequência', { exact: true });
    if (await dataInput.inputValue() !== data) await dataInput.fill(data);
    await choose(page.getByRole('combobox', { name: 'Lotação', exact: true }), lotacaoId);
    await choose(page.getByRole('combobox', { name: 'Turno', exact: true }), jornadaId);
    if (fase === 'escopo') {
      const perfilAtual = await requestGet('/api/usuarios/meu-perfil');
      check('Java confirma o par legado atual sem vínculos novos ou mudança de perfil', perfilAtual.perfil === 'OPERACIONAL'
        && Array.isArray(perfilAtual.vinculosGestao) && perfilAtual.vinculosGestao.length === 0
        && Number(perfilAtual.lotacaoId) === lotacaoId && Number(perfilAtual.jornadaId) === jornadaId
        && Number(perfilAtual.gestaoLocal?.id) === Number(fixture.ids.gestao_origem));
    }
    const catalogo = await requestGet('/api/contratos');
    const contrato = rows(catalogo).find(item => Number(item.id) === contratoId);
    check('Contrato da fixture existe no catálogo autorizado', !!contrato);
    const rotuloContrato = `${contrato.numeroContrato ? contrato.numeroContrato + ' - ' : ''}${contrato.descricao || 'Contrato ' + contrato.id}`;
    await page.getByRole('button', { name: 'Selecionar contratos', exact: true }).click();
    // O filtro recebido renderiza contratos ativos na ordem exata do catálogo,
    // sem IDs no rótulo. Há nomes duplicados na fixture: o nome não escolhe o ID.
    const ativos = rows(catalogo).filter(item => item.ativo === true);
    const indiceContrato = ativos.findIndex(item => Number(item.id) === contratoId);
    const checkboxes = page.locator('[data-tour="freq-filters"]').getByRole('checkbox');
    check('Filtro renderiza a sequência do catálogo ativo e contém o Contrato ID', indiceContrato >= 0 && await checkboxes.count() === ativos.length + 1);
    const checkboxContrato = checkboxes.nth(indiceContrato + 1); // primeiro checkbox é Marcar/Desmarcar Todos
    check('Checkbox na posição do ID tem o rótulo do catálogo correspondente', (await checkboxContrato.evaluate(element => element.closest('label').innerText)).trim() === rotuloContrato);
    await checkboxContrato.check();
    await page.getByRole('button', { name: 'Selecionar contratos', exact: true }).click();

    const antes = await lerFrequencia(); report.persistenciaAntes = resumo(antes);
    if (fase === 'op' || fase === 'escopo') check('Primeiro lançamento OP não possui frequência prévia no Vínculo/data', antes === null);
    else check('Etapa posterior encontra a frequência canônica já persistida', !!antes && Number(antes.id) === Number(business.frequenciaCanonicaId));
    if (fase === 'gl') check('GL recebe a observação do primeiro lançamento OP', antes.observacao === observacoes.op && antes.bloqueadaEdicaoOperacional === false);
    if (fase === 'recusado') check('Operacional recebe o estado gerencial bloqueado persistido', antes.observacao === observacoes.gl && antes.estadoAuditoria === 'RETIFICADA_GESTOR' && antes.bloqueadaEdicaoOperacional === true);

    const launcher = page.getByRole('button', { name: 'Lançar por vínculo', exact: true });
    const modal = page.getByRole('dialog', { name: 'Lançar por vínculo', exact: true });
    const abrirLista = async () => {
      const pending = esperarApi('/api/vinculos-contratuais/elegiveis-frequencia', 'GET', url => url.searchParams.get('dataRef') === data && url.searchParams.get('page') === '0');
      await launcher.focus(); await launcher.click();
      const response = await pending;
      check('Listagem canônica usada pelo modal respondeu200', response.status() === 200);
      const url = new URL(response.url());
      check('Listagem solicita cinco opções e o Contrato/data escolhidos', url.searchParams.get('size') === '5' && url.searchParams.get('contratoId') === String(contratoId) && url.searchParams.get('dataRef') === data
        && url.searchParams.get('lotacaoId') === String(lotacaoId) && url.searchParams.get('jornadaId') === String(jornadaId),
        { size: url.searchParams.get('size'), contratoId: url.searchParams.get('contratoId'), dataRef: url.searchParams.get('dataRef'), lotacaoId: url.searchParams.get('lotacaoId'), jornadaId: url.searchParams.get('jornadaId') });
      const body = unwrap(await response.json());
      check('Página canônica contém até cinco opções reais', rows(body).length > 0 && rows(body).length <= 5);
      await modal.waitFor(); await modal.getByRole('button', { name: /^Selecionar vínculo / }).first().waitFor();
      check('Nenhum Vínculo é automaticamente selecionado', await modal.getByLabel('Status da frequência', { exact: true }).count() === 0 && posicoes.length === 0 && posts.length === 0);
      return body;
    };
    let pagina = await abrirLista();
    await page.keyboard.press('Escape');
    await modal.waitFor({ state: 'hidden' });
    check('Escape fecha o modal e devolve foco ao botão de origem', await launcher.evaluate(element => element === document.activeElement));
    pagina = await abrirLista();

    let numeroPagina = 0;
    while (!rows(pagina).some(item => Number(item.id) === vinculoId)) {
      check('Vínculo desejado pode ser procurado na próxima página autorizada', numeroPagina + 1 < Number(pagina.totalPages));
      numeroPagina += 1;
      const pending = esperarApi('/api/vinculos-contratuais/elegiveis-frequencia', 'GET', url => Number(url.searchParams.get('page')) === numeroPagina && url.searchParams.get('dataRef') === data);
      await modal.getByRole('button', { name: 'Próximos vínculos', exact: true }).click();
      const response = await pending; check('Página seguinte de Vínculos responde200', response.status() === 200);
      pagina = unwrap(await response.json());
      check('Página seguinte mantém no máximo cinco opções', rows(pagina).length <= 5);
    }
    const opcao = rows(pagina).find(item => Number(item.id) === vinculoId);
    check('Opção selecionável declara Pessoa e Contrato reais da fixture', Number(opcao.pessoaId) === pessoaId && Number(opcao.contratoId) === contratoId);
    check('Elegibilidade declara a data, o escopo e a alocação reais', opcao.dataReferencia === data && Number(opcao.lotacaoId) === lotacaoId
      && Number(opcao.jornadaId) === jornadaId && Number(opcao.gestaoLocalId) === Number(fixture.ids.gestao_origem) && Number(opcao.alocacaoId) > 0);
    if (opcao.pessoaNome) check('Nome da lista é explicitamente cadastral atual', await modal.getByText(`Nome cadastral atual: ${opcao.pessoaNome}`, { exact: true }).isVisible());
    const pessoaPendente = esperarApi(`/api/vinculos-contratuais/pessoas/${pessoaId}/posicao`, 'GET', url => url.searchParams.get('data') === data);
    const alocacaoPendente = esperarApi(`/api/vinculos-contratuais/${vinculoId}/alocacoes/posicao`, 'GET', url => url.searchParams.get('data') === data);
    await modal.getByRole('button', { name: `Selecionar vínculo ${vinculoId}`, exact: true }).click();
    const [pessoaResponse, alocacaoResponse] = await Promise.all([pessoaPendente, alocacaoPendente]);
    check('Ambas as posições usadas pela UI são autorizadas pelo Java', pessoaResponse.status() === 200 && alocacaoResponse.status() === 200);
    const pessoa = unwrap(await pessoaResponse.json()), alocacao = unwrap(await alocacaoResponse.json());
    const entradas = (pessoa.vinculos || []).filter(item => Number(item.vinculoId) === vinculoId);
    check('PessoaPosição confirma exatamente Pessoa/Vínculo/Contrato/D e estados', Number(pessoa.pessoaId) === pessoaId && pessoa.dataReferencia === data && entradas.length === 1 && Number(entradas[0].pessoaId) === pessoaId && Number(entradas[0].contratoId) === contratoId && entradas[0].dataReferencia === data && entradas[0].estadoNegocial === 'REGISTRADO' && entradas[0].situacaoTemporal === 'VIGENTE');
    check('AlocaçãoPosição confirma ID/data/Alocação e par exato de escopo', Number(alocacao.vinculoId) === vinculoId && alocacao.dataReferencia === data && alocacao.situacaoTemporal === 'VIGENTE' && Number(alocacao.alocacaoId) > 0 && Number(alocacao.alocacaoId) === Number(opcao.alocacaoId) && Number(alocacao.lotacaoId) === lotacaoId && Number(alocacao.jornadaId) === jornadaId);
    report.posicoes = { pessoaId: pessoa.pessoaId, vinculoId, contratoId, dataReferencia: data, alocacaoId: alocacao.alocacaoId, lotacaoId: alocacao.lotacaoId, jornadaId: alocacao.jornadaId, conhecidoAtePessoa: pessoa.conhecidoAte, conhecidoAteAlocacao: alocacao.conhecidoAte };
    await modal.getByText(/^Posição validada para/).waitFor();
    check('Status exige declaração explícita do usuário', await modal.getByLabel('Status da frequência', { exact: true }).inputValue() === '');
    if (fase === 'escopo') {
      await shot('escopo-legado-sem-gravacao');
      await page.keyboard.press('Escape');
      await modal.waitFor({ state: 'hidden' });
      check('Consulta de escopo cancela o modal sem enviar frequência', posts.length === 0);
      check('Frequência continua ausente na leitura independente', await lerFrequencia() === null);
      await requestGet('/api/autenticacao/sessao');
      check('Sessão permanece válida e nenhum comando de frequência foi enviado', posts.length === 0 && await page.getByRole('button', { name: 'Sair', exact: true }).isVisible());
      return;
    }
    await modal.getByLabel('Status da frequência', { exact: true }).selectOption('FOLGA');
    await modal.getByLabel('Observação do lançamento', { exact: true }).fill(observacoes[fase]);
    if (fase === 'gl') await modal.getByLabel('Justificativa da retificação', { exact: true }).fill('Retificação sintética autorizada para comprovar a intervenção gerencial no mesmo Vínculo/data.');
    await shot('selecao-canonica');
    const gravacaoPendente = esperarApi('/api/frequencias', 'POST');
    await modal.getByRole('button', { name: 'Confirmar lançamento', exact: true }).click();
    await confirmPassword();
    const response = await gravacaoPendente;
    const payload = response.request().postDataJSON();
    report.post = { path: '/api/frequencias', status: response.status(), payload };
    check('Payload conserva somente a identidade canônica selecionada', Number(payload.vinculoContratualId) === vinculoId && payload.dataReferencia === data && payload.status === 'FOLGA' && !Object.hasOwn(payload, 'empregado') && !Object.hasOwn(payload, 'empregadoId') && !Object.hasOwn(payload, 'pessoaId') && !Object.hasOwn(payload, 'cpf'));
    check('A intenção efetuou exatamente um POST pelo modal', posts.length === 1);
    if (fase === 'gl') check('Retificação GL inclui justificativa e chave de idempotência', String(payload.justificativaRetificacao || '').length >= 5 && String(payload.chaveIdempotencia || '').length >= 8);

    if (fase === 'recusado') {
      const error = await response.json();
      report.rejection = { status: response.status(), code: error.code, message: error.message };
      check('Java recusa a escrita Operacional após intervenção gerencial', response.status() === 403 && /intervenção.*Gestor Local/i.test(error.message || ''), report.rejection);
      const confirmacao = page.getByRole('dialog', { name: 'Confirmar lançamento por vínculo', exact: true });
      await confirmacao.getByRole('alert').waitFor();
      check('Recusa real permanece visível na confirmação', /intervenção.*Gestor Local/i.test(await confirmacao.getByRole('alert').innerText()));
      await shot('recusa-java');
      await confirmacao.getByRole('button', { name: 'Cancelar', exact: true }).click();
      await confirmacao.waitFor({ state: 'hidden' });
      check('Formulário preserva status e anotação após403', await modal.getByLabel('Status da frequência', { exact: true }).inputValue() === 'FOLGA' && await modal.getByLabel('Observação do lançamento', { exact: true }).inputValue() === observacoes.recusado);
      check('Erro persistente esclarece ausência de retry automático', /Dados e chave de operação mantidos.*não houve nova tentativa automática/s.test(await modal.getByRole('alert').innerText()));
      const depois = await lerFrequencia(); report.persistenciaDepois = resumo(depois);
      check('Recusa não altera a frequência gerencial persistida', Number(depois.id) === Number(antes.id) && depois.observacao === observacoes.gl && depois.versaoNegocial === antes.versaoNegocial && depois.bloqueadaEdicaoOperacional === true);
      await requestGet('/api/autenticacao/sessao');
      check('403 de autorização mantém a sessão e não repete o POST', await page.getByRole('button', { name: 'Sair', exact: true }).isVisible() && posts.length === 1);
      await shot('formulario-preservado');
      if (process.env.SGT_LANCAMENTO_VERIFICAR_INTENCAO === '1') {
        const statusCampo = modal.getByLabel('Status da frequência', { exact: true });
        const observacaoCampo = modal.getByLabel('Observação do lançamento', { exact: true });
        check('Após resposta real recusada, campos permanecem fixos na intenção original', await statusCampo.isDisabled() && await observacaoCampo.isDisabled());
        const enviarRecusado = async () => {
          const pending = esperarApi('/api/frequencias', 'POST');
          await modal.getByRole('button', { name: 'Confirmar lançamento', exact: true }).click();
          await confirmPassword();
          const result = await pending;
          check('Tentativa manual continua sujeita ao403 autoritativo do Java', result.status() === 403);
          await confirmacao.getByRole('alert').waitFor();
          await confirmacao.getByRole('button', { name: 'Cancelar', exact: true }).click();
          await confirmacao.waitFor({ state: 'hidden' });
          return result.request().postDataJSON();
        };
        const repetido = await enviarRecusado();
        check('Repetição manual conserva exatamente o comando anterior', JSON.stringify(repetido) === JSON.stringify(payload) && posts.length === 2);
        await modal.getByRole('button', { name: 'Editar como nova operação', exact: true }).click();
        check('Ação explícita de nova operação libera os campos sem enviar HTTP', await statusCampo.isEnabled() && await observacaoCampo.isEnabled() && posts.length === 2);
        const novaObservacao = observacoes.recusado + ' Nova intenção explicitamente declarada.';
        await observacaoCampo.fill(novaObservacao);
        const novo = await enviarRecusado();
        check('Novo conteúdo só é enviado após nova intenção e nova confirmação', novo.observacao === novaObservacao && novo.vinculoContratualId === payload.vinculoContratualId && posts.length === 3);
        const final = await lerFrequencia();
        check('Três recusas preservam integralmente a frequência gerencial e a autenticação', final.versaoNegocial === antes.versaoNegocial && final.observacao === antes.observacao && await page.getByRole('button', { name: 'Sair', exact: true }).isVisible());
        report.intentPolicy = { profile: 'OPERACIONAL', realStatus: 403, identicalReplay: true, explicitNewIntent: true, posts: posts.length, idempotencyKeyRequiredByJavaForThisProfile: false };
        await shot('nova-intencao-explicita');
      }
      return;
    }

    check('Java confirma o lançamento ou retificação comHTTP200', response.status() === 200, { status: response.status() });
    const gravada = unwrap(await response.json());
    recordBusiness({ frequenciaCanonicaId: gravada.id, frequenciaCanonicaData: data, [`frequenciaCanonica${fase === 'op' ? 'Operacional' : 'Gestor'}Confirmada`]: true });
    report.respostaGravacao = resumo(gravada);
    check('Resposta declara IDs canônicos e projeção operacional reais', Number(gravada.id) > 0 && Number(gravada.pessoaId) === pessoaId && Number(gravada.vinculoContratualId) === vinculoId && Number(gravada.alocacaoVinculoId) === Number(alocacao.alocacaoId) && Number(gravada.empregado?.id) > 0);
    await modal.waitFor({ state: 'hidden', timeout: 15000 });
    const depois = await lerFrequencia(); report.persistenciaDepois = resumo(depois);
    check('GET independente comprova persistência da mesma identidade e observação', Number(depois?.id) === Number(gravada.id) && Number(depois.pessoaId) === pessoaId && Number(depois.vinculoContratualId) === vinculoId && depois.dataReferencia === data && depois.status === 'FOLGA' && depois.observacao === observacoes[fase]);
    if (fase === 'op') check('Primeira gravação OP permanece operacional e válida', depois.bloqueadaEdicaoOperacional === false && depois.validadeOperacional === 'VALIDA');
    else check('Retificação GL conserva o ID e registra bloqueio operacional', Number(depois.id) === Number(antes.id) && depois.estadoAuditoria === 'RETIFICADA_GESTOR' && depois.bloqueadaEdicaoOperacional === true && Number(depois.versaoNegocial) > Number(antes.versaoNegocial));
    await shot('persistencia-real');
  }, { accountName, reuse: false });
}

(async () => {
  const solicitada = process.env.SGT_LANCAMENTO_FASE;
  const fases = solicitada ? [solicitada] : ['op', 'gl', 'recusado'];
  if (fases.some(fase => !['op', 'gl', 'recusado', 'escopo'].includes(fase))) throw new Error('Fase E2E desconhecida.');
  for (const fase of fases) {
    await executarFase(fase);
    if (process.exitCode) break;
  }
})().catch(error => { console.error(error.stack || error.message); process.exitCode = 1; });
