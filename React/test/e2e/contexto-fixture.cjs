// Somente configuração dos drivers: identidades observadas, nunca IDs presumidos.
function baseFrontend(env = process.env) {
  if (!env.SGT_FRONTEND_URL) throw new Error('Defina SGT_FRONTEND_URL para o frontend real desta execução.');
  let url;
  try { url = new URL(env.SGT_FRONTEND_URL); } catch { throw new Error('SGT_FRONTEND_URL deve ser uma origem HTTP(S) válida.'); }
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password ||
      url.pathname !== '/' || url.search || url.hash) {
    throw new Error('SGT_FRONTEND_URL deve conter somente a origem HTTP(S), sem credenciais ou caminho.');
  }
  return url.origin;
}

function idObservado(valor, nome) {
  if (!Number.isSafeInteger(valor) || valor <= 0) throw new Error(`Identidade observada ausente ou inválida: ${nome}.`);
  return valor;
}

function contratoDaFixture(fixture, chave = 'contrato_rascunho') {
  if (fixture?.synthetic !== true) throw new Error('O driver exige uma fixture identificada como sintética.');
  const id = idObservado(fixture.ids?.[chave], chave);
  const contrato = fixture.objects?.[chave];
  if (contrato?.id !== id || typeof contrato.numeroContrato !== 'string' || !contrato.numeroContrato.trim() ||
      !Number.isSafeInteger(contrato.anoContrato) || contrato.anoContrato <= 0) {
    throw new Error(`Dados observados do contrato ausentes ou incompatíveis com a fixture: ${chave}.`);
  }
  return {
    id, numero: contrato.numeroContrato, ano: contrato.anoContrato,
    titulo: `${contrato.numeroContrato}/${contrato.anoContrato}`,
    botaoAbrir: `Abrir ciclo de vida do contrato ${contrato.numeroContrato}`,
    caminho: `/contratos/${id}`, api: `/api/contratos/${id}`,
  };
}

module.exports = { baseFrontend, contratoDaFixture, idObservado };
