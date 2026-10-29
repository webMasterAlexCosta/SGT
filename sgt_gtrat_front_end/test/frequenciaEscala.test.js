import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const raiz = new URL('../', import.meta.url);
const ler = (caminho) => readFile(new URL(caminho, raiz), 'utf8');
const trechoEnvioCanonico = (logica) => {
  const inicio = logica.indexOf("const enviarPendentes = useCallback");
  const fim = logica.indexOf("const invocarGatilhoDeDespachoAoServidorGlobal", inicio);
  assert.ok(inicio >= 0 && fim > inicio, "bloco canônico enviarPendentes não encontrado");
  return logica.slice(inicio, fim);
};


test('consultas de frequência usam paginação controlada e limite configurável', async () => {
  const [servico, paginacao, config, env] = await Promise.all([
    ler('src/services/rh/ServicoFrequencia.js'),
    ler('src/services/rh/paginacao.js'),
    ler('src/core/config.js'),
    ler('.env.example'),
  ]);
  assert.match(servico, /requisitarTodasAsPaginas/);
  assert.match(paginacao, /TAMANHO_PAGINA\s*=\s*1000/);
  assert.match(paginacao, /MAXIMO_ITENS_GRADE\s*=\s*CONFIG\.FREQUENCIA_MAX_GRID_ITEMS/);
  assert.match(config, /FREQUENCIA_MAX_GRID_ITEMS/);
  assert.match(env, /VITE_FREQUENCIA_MAX_GRID_ITEMS=150000/);
  assert.doesNotMatch(servico + paginacao, /size\s*:\s*50000/);
});

test('salvar alterações usa identidade canônica e POST individual/lote sem retry automático', async () => {
  const [servico, logica, dominio] = await Promise.all([
    ler('src/services/rh/ServicoFrequencia.js'),
    ler('src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js'),
    ler('src/services/rh/ServicoDominioFrequencia.js'),
  ]);
  assert.match(logica, /obterRegistrosPendentes\(\)/);
  assert.match(logica, /montarPayloadFrequenciaCanonica/);
  assert.match(logica, /ServicoFrequencia\.registrar\(/);
  assert.match(logica, /ServicoFrequencia\.registrarLote\(/);
  assert.match(dominio, /vinculoContratualId/);
  assert.match(dominio, /TAMANHO_MAXIMO_LOTE_FREQUENCIA\s*=\s*1000/);
  assert.match(servico, /registrar:[\s\S]{0,180}?retries:\s*0/);
  assert.match(servico, /registrarLote:[\s\S]{0,180}?retries:\s*0/);
  assert.doesNotMatch(trechoEnvioCanonico(logica), /ServicoFrequencia\.criarFechamento/);
});

test('falha parcial de lotes é explicada e não dispara retry cego', async () => {
  const [logica, dominio] = await Promise.all([
    ler('src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js'),
    ler('src/services/rh/ServicoDominioFrequencia.js'),
  ]);
  assert.match(dominio, /progressoFrequencia/);
  assert.match(logica, /Não houve retry automático/);
  assert.match(logica, /rascunhosNaoSalvosRef\.current\.delete/);
  assert.match(logica, /retries:\s*0/);
});

test('falha de consulta não é convertida em frequência vazia', async () => {
  const logica = await ler('src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js');
  assert.doesNotMatch(logica, /ServicoFrequencia\.buscarDoDia\([\s\S]{0,300}?\.catch\(\(\)\s*=>\s*\[\]\)/);
  assert.match(logica, /setErroCargaFrequencia\(mensagem\)/);
});

test('listas grandes usam virtualização obrigatória', async () => {
  const tabela = await ler('src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.jsx');
  assert.match(tabela, /virtualizacaoObrigatoria\s*=\s*listaDeFrequencia\.length\s*>=\s*150/);
  assert.match(tabela, /virtualizacaoObrigatoria\s*\|\|\s*preferenciaVirtualizacao/);
});

test('filtro de múltiplos contratos usa uma única projeção paginada no servidor', async () => {
  const logica = await ler('src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js');
  assert.match(logica, /buscarPaginadosParaFrequencia/);
  assert.match(logica, /contratoIds:\s*contratosSelecionadosPeloUsuario\.length\s*\?\s*contratosSelecionadosPeloUsuario\.map\(Number\)/);
  assert.doesNotMatch(logica, /Promise\.all\(\s*contratosSelecionadosPeloUsuario\.map/);
});

test('calendário preserva data e acrescenta lotação', async () => {
  const servico = await ler('src/services/rh/ServicoFrequencia.js');
  assert.match(servico, /params:\s*\{\s*data:\s*dataISO,\s*\.\.\.params\s*\}/);
  assert.match(servico, /params:\s*\{\s*inicio:\s*inicioISO,\s*fim:\s*fimISO,\s*\.\.\.params\s*\}/);
});

test('faltas e atestados são destacados e agrupados no topo', async () => {
  const [tabela, estilos, dominio] = await Promise.all([
    ler('src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.jsx'),
    ler('src/modules/rh/pages/PaginaFrequencia/index.module.css'),
    ler('src/services/rh/ServicoDominioFrequencia.js'),
  ]);
  assert.match(tabela, /status[^\n]*FALTA|statusLimpo === "FALTA"/);
  assert.match(tabela, /agruparFaltasNoTopo/);
  assert.match(dominio, /faltas\.concat\(atestados,\s*demais\)/);
  assert.match(tabela, /contadorFaltas/);
  assert.match(tabela, /contadorAtestados/);
  assert.match(estilos, /\.linhaFalta/);
  assert.match(estilos, /\.linhaAtestado/);
});

test('busca usa índice local para resposta imediata e searchTerm diferido no servidor', async () => {
  const [logica, dominio] = await Promise.all([
    ler('src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js'),
    ler('src/services/rh/ServicoDominioFrequencia.js'),
  ]);
  assert.match(logica, /useDeferredValue/);
  assert.match(logica, /searchTerm:\s*termoDeBuscaDiferido/);
  assert.match(logica, /indiceBusca:/);
  assert.match(dominio, /partes\.every/);
  assert.match(dominio, /normalize\("NFD"\)/);
});

test('paginação grande busca páginas de frequências com concorrência limitada', async () => {
  const [paginacao, core] = await Promise.all([
    ler('src/services/rh/paginacao.js'),
    ler('src/services/rh/nucleoPaginacao.js'),
  ]);
  assert.match(paginacao, /MAXIMO_REQUISICOES_PAGINAS_SIMULTANEAS\s*=\s*4/);
  assert.match(core, /Promise\.all\(requisicoes\)/);
  assert.match(core, /totalElements > maximoItens/);
});

test('interface bloqueia duplo envio e mostra progresso de lote canônico', async () => {
  const [cabecalho, pagina, tabela] = await Promise.all([
    ler('src/modules/rh/pages/PaginaFrequencia/components/CabecalhoFrequencia.jsx'),
    ler('src/modules/rh/pages/PaginaFrequencia/index.jsx'),
    ler('src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.jsx'),
  ]);
  assert.match(cabecalho, /ENVIANDO/);
  assert.match(cabecalho, /progressoEnvio/);
  assert.match(pagina, /estado\.indicadorSalvamento\s*\|\|\s*!auth\.podeRealizarFrequencias/);
  assert.match(tabela, /edicaoBloqueada/);
});
