import React from 'react';
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { LinksRodape } from '../src/Leiaute/Rodape/components/LinksRodape';

afterEach(cleanup);

it('identifica conteúdos indisponíveis sem oferecer links vazios', () => {
  render(<LinksRodape aoAbrirCreditos={() => undefined} />);
  for (const nome of ['Suporte Gestão Local', 'Manual do sistema', 'Subsidio Jurídico PDF', 'Política de uso interno', 'Segurança da informação']) {
    expect(screen.queryByRole('link', { name: nome })).not.toBeInTheDocument();
    expect(screen.getByText(nome).parentElement).toHaveTextContent('Indisponível');
  }
  expect(screen.getAllByRole('button')).toHaveLength(1);
});

it('preserva a abertura dos créditos disponíveis', () => {
  const abrir = vi.fn();
  render(<LinksRodape aoAbrirCreditos={abrir} />);
  fireEvent.click(screen.getByRole('button', { name: 'Créditos' }));
  expect(abrir).toHaveBeenCalledOnce();
});
