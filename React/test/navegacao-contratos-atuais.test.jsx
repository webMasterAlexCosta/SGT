import React from 'react';
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import BarraLateral from '../src/Leiaute/BarraLateral/index.jsx';

afterEach(cleanup);
it('oferece os catálogos vigentes sem conduzir a cadastros bancários removidos da API', () => {
  render(<MemoryRouter><BarraLateral usuario={{ perfil: 'GESTOR_LOCAL' }}
    permissoes={{ catalogosReferencia: true, tiposContratacao: true }}
    sectionsOpen={{ config: true, sub_catalogos: true, sub_banco: true }}
    toggleSection={() => {}} onNavigate={() => {}} /></MemoryRouter>);
  expect(screen.getByRole('button', { name: 'Cargos e CBO' })).toBeTruthy();
  expect(screen.queryByRole('button', { name: 'Bancos', exact: true })).toBeNull();
  expect(screen.queryByRole('button', { name: 'Agências', exact: true })).toBeNull();
  expect(screen.queryByRole('button', { name: 'Tipos de conta', exact: true })).toBeNull();
});

it('oferece histórico ao usuário com acesso e navega para a página canônica', () => {
  const navegar = vi.fn();
  render(<MemoryRouter><BarraLateral usuario={{ perfil: 'GESTOR_LOCAL' }} permissoes={{ historico: true }} sectionsOpen={{ config: true }} toggleSection={() => {}} onNavigate={navegar} /></MemoryRouter>);
  fireEvent.click(screen.getByRole('button', { name: 'Histórico e provas' }));
  expect(navegar).toHaveBeenCalledWith('/historico');
});

it('não oferece navegação histórica quando o perfil não possui consulta', () => {
  render(<MemoryRouter><BarraLateral usuario={{ perfil: 'DESCONHECIDO' }} permissoes={{}} sectionsOpen={{ config: true }} toggleSection={() => {}} onNavigate={() => {}} /></MemoryRouter>);
  expect(screen.queryByRole('button', { name: 'Histórico e provas' })).toBeNull();
});
