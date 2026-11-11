'use strict';
const fs = require('node:fs');
const path = require('node:path');
const { execute } = require('./ambiente-real.cjs');
const { baseFrontend, contratoDaFixture } = require('./contexto-fixture.cjs');
execute('preparacao-demonstracoes-real', async ({ context, fixture, open, check, report }) => {
  await open('/contratos');
  const inventory = {};
  for (const [name, route] of [['cargos', '/api/cargos?size=100'], ['jornadas', '/api/jornadas'],
    ['lotacoes', '/api/lotacoes'], ['documents', contratoDaFixture(fixture).api + '/documentos']]) {
    const response = await context.request.get(baseFrontend() + route);
    check('Inventário somente leitura200 ' + name, response.status() === 200);
    inventory[name] = (await response.json()).data;
  }
  fs.writeFileSync(path.join(process.env.SGT_E2E_OUTPUT, 'inventory.json'), JSON.stringify(inventory, null, 2), { mode: 0o600 });
  report.scope = 'PREPARATION_ONLY_READONLY_INVENTORY_NOT_ACCEPTANCE';
}, { reuse: false });
