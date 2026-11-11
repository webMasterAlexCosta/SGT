'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend, contratoDaFixture } = require('./contexto-fixture.cjs');

const phase = process.env.SGT_ADITAMENTO_PHASE;
if (!['empresa', 'sem-concessao', 'conceder', 'excecao', 'revogar'].includes(phase)) throw new Error('Selecione uma fase explícita de aditamento.');
const admin = ['conceder', 'revogar'].includes(phase);
const hash = value => crypto.createHash('sha256').update(Buffer.isBuffer(value) ? value : JSON.stringify(value)).digest('hex');
const plus = (d, n) => { const t = new Date(d + 'T12:00:00Z'); t.setUTCDate(t.getUTCDate() + n); return t.toISOString().slice(0, 10); };
const unwrap = value => value?.data ?? value;
const reason = 'Ato sintético de homologação do SGT, sem validade administrativa.';
const foundation = 'Fonte SEI sintética explícita para redução de uma vaga na composição de teste.';

execute('aditamento-ui-real-' + phase, async ({ page, context, fixture, account, report, check, open, confirmPassword, recordBusiness, screenshot }) => {
  const runtime = process.env.SGT_TEST_RUNTIME;
  const b = JSON.parse(fs.readFileSync(path.join(runtime, 'private/e2e-business.json')));
  const base = baseFrontend(), auth = '/api/autenticacao/csrf', permissions = '/api/permissoes-excepcionais';
  const who = fixture.accounts[admin ? 'fixture_admin' : 'gestor_local'];
  check('Ator e corpus são sintéticos e observados', fixture.synthetic && account.id === who.id);
  await open(admin ? '/permissoes-excepcionais' : '/contratos');
  const get = async (route, params) => {
    const r = await context.request.get(base + route, { params });
    check('Consulta autenticada: ' + route, r.status() === 200, { status: r.status() });
    return unwrap(await r.json());
  };
  const post = async (route, data, expected) => {
    const csrf = await get(auth);
    const r = await context.request.post(base + route, { data, headers: { Origin: base, [csrf.headerName]: csrf.token, 'X-User-Password': account.password } });
    const value = await r.json();
    check('Comando administrativo: ' + route, r.status() === expected, { status: r.status(), code: value.code });
    return unwrap(value);
  };
  if (admin) {
    check('Contrato e negativa prévios estão comprovados', b.quant07Publicado && b.quant07Revogada && b.aditamento08SemConcessao && b.quant07Contrato > 0);
    if (phase === 'conceder') {
      check('Não repetir a concessão', !b.aditamento08Permissao);
      const request = { tipo: 'REDUCAO_CONTRATUAL_EXCEPCIONAL', contratoId: b.quant07Contrato, usuarioBeneficiarioId: fixture.accounts.gestor_local.id,
        finalidade: 'Somente a redução sintética no contrato QUANT07 pelo formulário de aditamentos.', justificativa: reason,
        autorizacaoAdministrativa: 'SGT-TESTE-ADITAMENTO08', validoAte: new Date(Date.now() + 25 * 60000).toISOString() };
      const grant = await post(permissions, request, 201);
      if (grant.id > 0) recordBusiness({ aditamento08Permissao: grant.id });
      check('Concessão ativa, um contrato, um beneficiário e nenhum escopo alheio', grant.id > 0 && grant.ativa && grant.contratoId === request.contratoId
        && grant.usuarioBeneficiarioId === request.usuarioBeneficiarioId && grant.gestaoLocalId == null && grant.ocorrenciaId == null && grant.finalidade === request.finalidade);
      check('Nenhum uso antes do comando', (await get(`${permissions}/${grant.id}/usos`)).length === 0);
    } else {
      check('Revogar somente a concessão do roteiro', b.aditamento08Permissao > 0 && !b.aditamento08Revogada);
      const grants = await get(permissions), grant = grants.find(x => x.id === b.aditamento08Permissao);
      check('Escopo confirmado antes da revogação', grant?.ativa && grant.contratoId === b.quant07Contrato && grant.usuarioBeneficiarioId === fixture.accounts.gestor_local.id);
      const uses = await get(`${permissions}/${grant.id}/usos`);
      // Revogar também na limpeza de uma tentativa que falhe antes do commit.
      const revoked = await post(`${permissions}/${grant.id}/revogar`, { motivo: reason }, 200);
      if (revoked.id === grant.id && !revoked.ativa) recordBusiness({ aditamento08Revogada: true });
      check('Revogação não altera outras concessões nem a trilha de uso', revoked.id === grant.id && !revoked.ativa
        && hash(await get(`${permissions}/${grant.id}/usos`)) === hash(uses) && hash((await get(permissions)).filter(x => x.id !== grant.id)) === hash(grants.filter(x => x.id !== grant.id)));
      check('O uso corresponde somente ao ato efetivamente confirmado', uses.length === (b.aditamento08Excecao ? 1 : 0));
    }
    return;
  }

  const empresa = phase === 'empresa';
  const observed = contratoDaFixture(fixture);
  const id = empresa ? observed.id : b.quant07Contrato, api = '/api/contratos/' + id;
  const contract = await get(api), acts = api + '/aditamentos';
  const previous = await get(acts), versions = await get(api + '/versoes');
  const number = empresa ? 'SGT-TESTE-ADI08-EMPRESA' : 'SGT-TESTE-ADI08-EXCECAO';
  const date = empresa ? plus(b.cnpjDataEfetiva, 3) : plus(b.quant07D3, 1);
  const k = new Date().toISOString();
  const position = async (data, conhecidoAte) => (await get(api + '/posicao-temporal', { data, ...(conhecidoAte ? { conhecidoAte } : {}) })).versaoContratual;
  const before = await position(date, k);
  const today = fixture.dates.today;
  check('Ato novo, distinto, em data comprovada dentro da vigência', id > 0 && contract.estadoNegocial === 'PUBLICADO' && today <= date && date < contract.dataFim
    && !previous.some(x => x.identificacao === number) && !(empresa ? b.aditamento08Empresa : b.aditamento08Excecao));
  let provider, emissionApi, emissionHash, documentApi, documentHash;
  if (empresa) {
    provider = (await get('/api/fornecedores')).find(x => x.id === b.fornecedorCrudFinal02Id);
    check('Troca usa fornecedor ativo observado e parte da posição já corrigida', provider?.ativo && provider.id !== before.fornecedorId
      && provider.cnpj === b.fornecedorCrudFinal02Cnpj && before.cnpjEmpresa === b.cnpjCorrigido && b.documentoRetificadoE2EConcluido);
    const old = JSON.parse(fs.readFileSync(path.join(runtime, '../browser-jar06-original17-relatorios/browser/frequencias-relatorios.json')));
    check('Emissão formal anterior é a do roteiro original', old.passed && old.emissions?.length === 2 && fixture.pending_vinculo_request.contratoId === id);
    emissionApi = '/api/relatorios/administrativos/emissoes/' + old.emissions[0].id;
    const emission = await get(emissionApi);
    check('Identidade e versão da emissão correspondem ao recibo anterior', emission.metadados.id === old.emissions[0].id && emission.metadados.versao === old.emissions[0].versao);
    emissionHash = hash(emission);
    documentApi = api + '/documentos/' + b.documentoRetificadoE2EId;
    documentHash = hash(await get(documentApi));
  } else {
    check('Capacidade anterior74 após remanejamento, concessão anterior revogada', before.efetivoPrevisto === 74 && b.quant07Revogada && b.quant07Composicoes?.length === 2);
    check('A fase excepcional depende da concessão explícita', phase === 'excecao' ? b.aditamento08Permissao > 0 && !b.aditamento08Revogada : !b.aditamento08Permissao && !b.aditamento08SemConcessao);
  }

  await page.getByRole('button', { name: `Abrir ciclo de vida do contrato ${contract.numeroContrato}`, exact: true }).click();
  const detail = page.getByRole('dialog', { name: `${contract.numeroContrato}/${contract.anoContrato}`, exact: true });
  await detail.getByRole('tab', { name: 'Aditamentos', exact: true }).click();
  const form = detail.getByRole('heading', { name: 'Novo aditamento', exact: true }).locator('..');
  await form.getByLabel(/^Identificação/).fill(number);
  await form.getByLabel(/^Data efetiva/).fill(date);
  await form.getByLabel('Data do documento', { exact: true }).fill(today);
  await form.getByLabel('Data da assinatura', { exact: true }).fill(today);
  await form.getByLabel('Referência SEI', { exact: true }).fill(number);
  await form.getByLabel(/^Justificativa/).fill(reason);
  const fantasia = 'Ato documental SGT 08';
  if (empresa) {
    const listaEmpresas = page.waitForResponse(r => new URL(r.url()).pathname === '/api/fornecedores' && r.request().method() === 'GET');
    listaEmpresas.catch(() => {});
    await form.getByRole('checkbox', { name: 'Alterar empresa neste aditamento' }).check();
    const lista = await listaEmpresas;
    check('Catálogo real da troca carregou a empresa ativa', lista.status() === 200 && unwrap(await lista.json()).some(x => x.id === provider.id && x.ativo));
    await form.getByRole('combobox', { name: 'Nova empresa contratada', exact: true }).selectOption(String(provider.id));
    check('CNPJ vem do fornecedor escolhido e não é editável', await form.getByLabel('CNPJ da nova empresa').inputValue() === provider.cnpj
      && !await form.getByLabel('CNPJ da nova empresa').isEditable());
    await form.getByLabel('Nome fantasia da nova empresa').fill(fantasia);
  } else {
    await form.getByLabel('Novo efetivo total').fill('73');
    await form.getByRole('checkbox', { name: 'Usar fluxo excepcional para redução acima do limite normal' }).check();
    await form.getByLabel(/^Fundamento da exceção/).fill(foundation);
    for (const [index, q] of [38, 35].entries()) {
      await form.getByRole('button', { name: 'Adicionar linha' }).click();
      await form.locator('select').nth(index).selectOption(String(b.quant07Composicoes[index]));
      await form.getByLabel('Quantidade resultante').nth(index).fill(String(q));
    }
  }
  const watch = suffix => {
    const p = page.waitForResponse(r => new URL(r.url()).pathname === acts + suffix && r.request().method() === 'POST');
    p.catch(() => {}); return p;
  };
  const previewWait = watch('/previa');
  await form.getByRole('button', { name: 'Calcular prévia', exact: true }).click();
  const pr = await previewWait, preview = unwrap(await pr.json()), request = pr.request().postDataJSON();
  check('Prévia HTTP200 vinculada ao formulário', pr.status() === 200 && preview.tokenPrevia && request.dataEfetiva === date, { status: pr.status() });
  await form.getByRole('heading', { name: 'Prévia calculada no servidor' }).waitFor();
  const confirm = form.getByRole('button', { name: 'Confirmar com esta prévia' });
  if (empresa) {
    check('Prévia recebeu a empresa e todos os dados documentais sem alterar o efetivo', request.novoFornecedorId === provider.id
      && request.novoCnpjEmpresa === provider.cnpj && request.novaRazaoSocialEmpresa === provider.razaoSocial && request.novoNomeFantasiaEmpresa === fantasia
      && preview.efetivoAnterior === preview.efetivoResultante);
  } else {
    check('Java exige exceção da base100 e formulário bloqueia sem aceite', preview.efetivoOriginalPublicado === 100 && preview.pisoNormalReducao === 75
      && preview.exigeAutorizacaoExcepcional && preview.efetivoAnterior === 74 && preview.efetivoResultante === 73 && await confirm.isDisabled());
    await form.getByRole('checkbox', { name: 'Confirmo os impactos desta redução excepcional.' }).check();
    check('Aceite conserva a mesma prévia visível e habilita confirmação', await form.getByRole('heading', { name: 'Prévia calculada no servidor' }).isVisible() && await confirm.isEnabled());
  }
  const commitWait = watch('');
  await confirm.click();
  await confirmPassword();
  const commit = await commitWait, response = await commit.json(), sent = commit.request().postDataJSON(), act = unwrap(response);
  if (commit.status() === 201 && act.id > 0) recordBusiness({ [empresa ? 'aditamento08Empresa' : 'aditamento08Excecao']: act.id });
  check('Confirmação envia o mesmo token, com o aceite explícito na exceção', sent.tokenPrevia === preview.tokenPrevia && (empresa || sent.confirmarImpacto === true));
  if (phase === 'sem-concessao') {
    check('Java recusa 403 e formulário preserva a intenção', commit.status() === 403 && await form.getByLabel(/^Identificação/).inputValue() === number, { status: commit.status(), code: response.code });
    check('Recusa não cria ato, fotografia ou mudança de capacidade', hash(await get(acts)) === hash(previous) && hash(await get(api + '/versoes')) === hash(versions)
      && (await position(date)).efetivoPrevisto === 74);
    recordBusiness({ aditamento08SemConcessao: true });
    return;
  }
  check('Java confirma o único ato com autoria e data', commit.status() === 201 && act.contratoId === id && act.identificacao === number && act.usuarioResponsavelId === account.id && act.dataEfetiva === date, { status: commit.status(), code: response.code });
  const updated = await get(api + '/versoes');
  check('Uma nova fotografia; todas as anteriores são idênticas', updated.length === versions.length + 1 && versions.every(v => hash(updated.find(x => x.id === v.id)) === hash(v)));
  check('D com conhecimento anterior ao ato preserva a projeção preexistente', hash(await position(date, k)) === hash(before));
  const current = await position(date);
  if (empresa) {
    const dMinus = await position(plus(date, -1));
    check('D−1 mantém a empresa antiga e D projeta a empresa documentada', dMinus.fornecedorId === before.fornecedorId && dMinus.cnpjEmpresa === before.cnpjEmpresa
      && current.fornecedorId === provider.id && current.cnpjEmpresa === provider.cnpj && current.razaoSocialEmpresa === provider.razaoSocial && current.nomeFantasiaEmpresa === fantasia);
    const file = await context.request.get(base + documentApi + '/arquivo');
    check('Emissão e documento anteriores continuam idênticos, inclusive os bytes do arquivo', hash(await get(emissionApi)) === emissionHash && hash(await get(documentApi)) === documentHash
      && file.status() === 200 && hash(await file.body()) === b.documentoPublicadoE2EHash);
  } else {
    const q = await get(api + '/composicoes', { data: date });
    check('Exceção reduz exatamente uma vaga, sem mover a pessoa ou criar evento de alocação', current.efetivoPrevisto === 73 && act.reducaoExcepcional && act.fundamentoExcecao === foundation
      && q.length === 2 && q.some(x => x.id === b.quant07Composicoes[0] && x.efetivoPrevisto === 38) && q.some(x => x.id === b.quant07Composicoes[1] && x.efetivoPrevisto === 35)
      && hash(await get(`/api/vinculos-contratuais/${b.quant07Vinculo}/alocacoes/eventos`)) === b.quant07AlocacaoEventosSha);
  }
  await detail.getByText(new RegExp(number)).waitFor();
  report.receipt = { contract: id, act: act.id, event: act.eventoId, effective: date };
  await screenshot(phase);
}, { accountName: admin ? 'fixture_admin' : 'gestor_local', reuse: false });
