import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { Login } from '../src/modules/auth/pages/Login/index.tsx';

vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: null, autenticar: vi.fn(), cadastrar: vi.fn(), atualizarPerfil: vi.fn() }) }));
afterEach(() => { cleanup(); localStorage.clear(); });

describe('login após retirada do modo visual', () => {
  it('ignora a preferência visual antiga e conserva os controles de acesso', () => {
    localStorage.setItem('sgt_perf_mode', 'false');
    const { container } = render(<MemoryRouter><Login /></MemoryRouter>);
    expect(screen.getByRole('textbox', { name: 'Matrícula', exact: true })).toBeTruthy();
    expect(screen.getByLabelText('Senha', { exact: true })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Entrar no Sistema', exact: true })).toBeTruthy();
    expect(screen.getByRole('button', { name: /GUIA RÁPIDO/ })).toBeTruthy();
    expect(screen.queryByRole('button', { name: /MODO VISUAL|MODO DESEMPENHO/ })).toBeNull();
    expect(container.querySelector('#fluidoGooey')).toBeNull();
    expect(screen.getByText(/Sistema desenvolvido por Alex Costa/)).toBeTruthy();
    expect(screen.getByText('89618815')).toBeTruthy();
  });
});
