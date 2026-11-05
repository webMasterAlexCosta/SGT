// Regressão do texto entregue pelo componente real. Somente transporte e o
// retrato da autenticação são controlados; não é E2E, cálculo Java ou documento.
import React from 'react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitarControlado } = vi.hoisted(() => ({ requisitarControlado: vi.fn() }));
vi.mock('../src/core/http.js', async importarOriginal => ({
  ...await importarOriginal(), requisitar: requisitarControlado,
}));
vi.mock('../src/modules/auth/context.jsx', () => ({
  useAutenticacao: () => ({ user: { id: 71, perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 } }),
}));
import { PainelAlteracoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelAlteracoesContrato/index.jsx';

const contrato = { id: 41, numeroContrato: 'H02-SINTETICO', anoContrato: 2026,
  estadoNegocial: 'PUBLICADO', situacaoTemporal: 'VIGENTE', habilitacaoAdministrativa: 'ATIVO' };
// DTO explícito de PreviaAditamentoContratualDTO: base original 100, aumento
// anterior a 140, redução proposta a 75. A UI deve conservar o piso recebido.
const previa = { contratoId: 41, dataEfetiva: '2026-11-02', efetivoAnterior: 140,
  efetivoResultante: 75, efetivoOriginalPublicado: 100, pisoNormalReducao: 75,
  reducao: true, excedeLimiteNormal25: false, exigeAutorizacaoExcepcional: false,
  possuiImpactoOcupacao: false, impactosComposicao: [], alertas: [],
  tokenPrevia: 'PREVIA-SINTETICA-UNITARIA-SEM-VALOR-REAL' };
const leiturasPermitidas = new Set([
  '/contratos/41/composicoes', '/contratos/41/composicoes/excedentes',
  '/contratos/41/aditamentos', '/contratos/41/remanejamentos',
  '/lotacoes', '/jornadas', '/cargos',
]);

beforeEach(() => {
  requisitarControlado.mockReset();
  requisitarControlado.mockImplementation(async pedido => {
    if (pedido.method === 'GET' && leiturasPermitidas.has(pedido.url)) return [];
    if (pedido.method === 'POST' && pedido.url === '/contratos/41/aditamentos/previa') {
      expect(pedido.data).toMatchObject({ identificacao: 'ADI-SINTETICO-H02',
        dataEfetiva: '2026-11-02', novoEfetivoTotal: 75, justificativa: 'Teste local de orientação do piso.' });
      return previa;
    }
    throw new Error(`Requisição fora do escopo deste teste: ${pedido.method} ${pedido.url}`);
  });
});
afterEach(cleanup);

async function abrirAditamentos(podeGerenciar = false) {
  render(<PainelAlteracoesContrato contrato={contrato} podeGerenciar={podeGerenciar} />);
  await waitFor(() => expect(screen.queryByText('Atualizando posição contratual...')).not.toBeInTheDocument());
  fireEvent.click(screen.getByRole('tab', { name: 'Aditamentos', exact: true }));
}

it('orienta piso de 75% do original e limite ordinário de redução de 25%', async () => {
  await abrirAditamentos();
  const painel = screen.getByRole('tabpanel');
  expect(painel).toHaveTextContent(/piso[^.]*75%[^.]*efetivo total originalmente publicado/i);
  expect(painel).toHaveTextContent(/redução[^.]*25%/i);
  expect(painel).not.toHaveTextContent(/piso\s+normal\s+de\s+25%/i);
});

it('a prévia rotula 75% da base original e exibe o piso numérico recebido sem recalcular', async () => {
  await abrirAditamentos(true);
  fireEvent.change(screen.getByLabelText(/Identificação/), { target: { value: 'ADI-SINTETICO-H02' } });
  fireEvent.change(screen.getByLabelText(/Data efetiva/), { target: { value: '2026-11-02' } });
  fireEvent.change(screen.getByLabelText(/Justificativa/), { target: { value: 'Teste local de orientação do piso.' } });
  fireEvent.change(screen.getByLabelText('Novo efetivo total'), { target: { value: '75' } });
  fireEvent.click(screen.getByRole('button', { name: 'Calcular prévia', exact: true }));
  await screen.findByRole('heading', { name: 'Prévia calculada no servidor' });
  const piso = screen.getByText(/^Piso normal/);
  expect(piso).toHaveTextContent(/Piso normal.*75%.*original.*:\s*75$/i);
  expect(screen.getByRole('button', { name: 'Confirmar com esta prévia' })).toBeEnabled();
});
