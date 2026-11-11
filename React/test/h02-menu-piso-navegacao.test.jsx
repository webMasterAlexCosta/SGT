// Regressão DOM local. O atributo nativo inert é o contrato de exclusão de Tab;
// a navegação física por Tab/Shift+Tab continua pertencendo ao Chrome real.
import React, { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { MemoryRouter } from 'react-router-dom';
import { SecaoMenu } from '../src/Leiaute/BarraLateral/components/SecaoMenu.tsx';
import { SecaoSubMenu } from '../src/Leiaute/BarraLateral/components/SecaoSubMenu.tsx';
import BarraLateral from '../src/Leiaute/BarraLateral/index.tsx';

afterEach(cleanup);

function SecaoControlada({ Componente }) {
  const [aberta, setAberta] = useState(false);
  return <Componente titulo="Operação" estaAberto={aberta} aoAlternar={() => setAberta(valor => !valor)}>
    <button type="button">Destino operacional</button>
  </Componente>;
}

function SecoesAninhadas() {
  const [principal, setPrincipal] = useState(false);
  const [sub, setSub] = useState(true);
  return <SecaoMenu titulo="Cadastros" estaAberto={principal} aoAlternar={() => setPrincipal(valor => !valor)}>
    <SecaoSubMenu titulo="Catálogos" estaAberto={sub} aoAlternar={() => setSub(valor => !valor)}>
      <button type="button">Cargo cadastrado</button>
    </SecaoSubMenu>
  </SecaoMenu>;
}

describe('H02: conteúdo recolhido não permanece disponível para foco/navegação', () => {
  it.each([['seção', SecaoMenu], ['subseção', SecaoSubMenu]])(
    '%s retira conteúdo recolhido e o devolve à navegação ao abrir', (_nome, Componente) => {
      render(<SecaoControlada Componente={Componente} />);
      const alternar = screen.getByRole('button', { name: 'Operação', exact: true });
      const destino = screen.getByText('Destino operacional');
      expect(alternar).toHaveAttribute('aria-expanded', 'false');
      expect(screen.queryByRole('button', { name: 'Destino operacional' })).not.toBeInTheDocument();
      expect(destino.closest('[inert], [hidden]')).not.toBeNull();

      alternar.focus();
      fireEvent.click(alternar);
      expect(alternar).toHaveAttribute('aria-expanded', 'true');
      expect(screen.getByRole('button', { name: 'Destino operacional' })).toBe(destino);
      expect(destino.closest('[inert], [hidden], [aria-hidden="true"]')).toBeNull();
      destino.focus();
      expect(destino).toHaveFocus();

      alternar.focus();
      fireEvent.click(alternar);
      expect(alternar).toHaveFocus();
      expect(alternar).toHaveAttribute('aria-expanded', 'false');
      expect(destino.closest('[inert], [hidden]')).not.toBeNull();
      expect(screen.queryByRole('button', { name: 'Destino operacional' })).not.toBeInTheDocument();
    },
  );

  it('fechar a seção bloqueia inclusive uma subseção aberta, sem perder seu estado', () => {
    render(<SecoesAninhadas />);
    const principal = screen.getByRole('button', { name: 'Cadastros', exact: true });
    const destino = screen.getByText('Cargo cadastrado');
    expect(destino.closest('[inert], [hidden]')).not.toBeNull();
    expect(screen.queryByRole('button', { name: 'Cargo cadastrado' })).not.toBeInTheDocument();

    fireEvent.click(principal);
    const sub = screen.getByRole('button', { name: 'Catálogos', exact: true });
    expect(sub).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('button', { name: 'Cargo cadastrado' })).toBe(destino);
    expect(destino.closest('[inert], [hidden], [aria-hidden="true"]')).toBeNull();

    fireEvent.click(sub);
    expect(sub).toHaveAttribute('aria-expanded', 'false');
    expect(principal).toHaveAttribute('aria-expanded', 'true');
    expect(destino.closest('[inert], [hidden]')).not.toBeNull();
    expect(screen.queryByRole('button', { name: 'Cargo cadastrado' })).not.toBeInTheDocument();
  });

  it('menu móvel aberto conserva foco inicial no fechamento e atendimento de Escape', () => {
    const fechar = vi.fn();
    render(<MemoryRouter><BarraLateral modoMobile mobileMenu onCloseMenu={fechar}
      usuario={{ perfil: 'GESTOR_LOCAL' }} permissoes={{ contratos: true }}
      sectionsOpen={{ config: false }} toggleSection={() => {}} onNavigate={() => {}} />
    </MemoryRouter>);
    const fecharMenu = screen.getByRole('button', { name: 'Fechar menu principal' });
    expect(fecharMenu).toHaveFocus();
    fireEvent.keyDown(fecharMenu, { key: 'Escape' });
    expect(fechar).toHaveBeenCalledTimes(1);
  });
});
