// Roteador e fronteira reais. Página e painel sintéticos retêm o dado carregado
// na montagem para detectar reutilização indevida do estado entre contextos.
import React, { useState } from 'react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Aplicacao from '../src/Aplicacao.jsx';
const { auth } = vi.hoisted(() => ({ auth: { user: null, geracaoSessao: 1, loading: false, loggingOut: false, encerrarSessao: () => {}, atualizarInformacoesSessao: () => Promise.resolve(), concluirSaida: () => {} } }));
vi.mock('../src/modules/auth/context', () => ({ ProvedorAutenticacao: ({ children }) => children, useAutenticacao: () => auth }));
vi.mock('../src/services/system/ServicoSistema.js', () => ({ ServicoSistema: { carregarRuntimeConfig: () => Promise.resolve({}) } }));
vi.mock('../src/hooks/usePulsoAtividade', () => ({ usePulsoAtividade: () => {} }));
vi.mock('../src/Leiaute/BarraLateral/index.jsx', () => ({ default: () => null }));
vi.mock('../src/Leiaute/Cabecalho/index.jsx', () => ({ default: () => null }));
vi.mock('../src/Leiaute/Rodape/index.jsx', () => ({ default: () => null }));
vi.mock('../src/core/ui/HomologacaoModal.jsx', () => ({ default: () => null }));
vi.mock('../src/core/ui/LembreteSegundoFator', () => ({ LembreteSegundoFator: () => null }));
vi.mock('../src/modules/AtualizacaoModulo/index.jsx', () => ({ default: () => null }));
vi.mock('../src/core/ModalConexao', () => ({ ModalConexao: () => null }));
vi.mock('../src/core/ui/Aviso', () => ({ ConteinerAvisos: () => null }));
vi.mock('../src/core/security/ProvedorConfirmacaoSenhaHttp.jsx', () => ({ ProvedorConfirmacaoSenhaHttp: ({ children }) => children }));
vi.mock('../src/core/exportacao/context', () => ({ ProvedorExportacaoRelatorio: ({ children }) => {
  const [dados] = useState(() => auth.user ? `${auth.user.id}:${auth.user.gestaoLocalId}` : 'sem-sessao');
  return <><output data-testid="tarefas-carregadas">{dados}</output>{children}</>;
} }));
vi.mock('../src/modules/perfil/pages/PaginaPerfil/index.jsx', () => ({ PaginaPerfil: () => {
  const [dados] = useState(() => `${auth.user.id}:${auth.user.gestaoLocalId}`);
  return <output data-testid="dados-carregados">{dados}</output>;
} }));
vi.mock('../src/modules/auth/pages/Login/index.jsx', () => ({ Login: () => <h1>Login sintético</h1> }));
const arvore = () => <MemoryRouter initialEntries={['/perfil']}><Aplicacao /></MemoryRouter>;
beforeEach(() => { HTMLElement.prototype.scrollTo = () => {}; auth.user = { id: 901, username: 'a-sintetico', perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL', gestaoLocalId: 1 }; auth.geracaoSessao = 1; auth.loggingOut = false; });
afterEach(cleanup);
it.each(['identidade', 'escopo'])('descarta os dados da rota e as exportações quando muda %s', async tipo => {
  const view = render(arvore()); expect((await screen.findByTestId('dados-carregados')).textContent).toBe('901:1');
  auth.user = { ...auth.user, id: tipo === 'identidade' ? 902 : 901, gestaoLocalId: 2 }; auth.geracaoSessao += 1;
  view.rerender(arvore());
  expect(screen.getByTestId('dados-carregados').textContent).toBe(`${auth.user.id}:2`);
  expect(screen.getByTestId('tarefas-carregadas').textContent).toBe(`${auth.user.id}:2`);
});
it('logout desmonta os dados de página e o painel global de exportação imediatamente', async () => {
  const view = render(arvore()); await screen.findByTestId('dados-carregados');
  auth.user = null; auth.loggingOut = true; auth.geracaoSessao += 1; view.rerender(arvore());
  await screen.findByText('Login sintético');
  expect(screen.queryByTestId('dados-carregados')).toBeNull(); expect(screen.queryByTestId('tarefas-carregadas')).toBeNull();
});
