// Executar no Fedora após instalar as seis devDependencies ausentes.
// Valida a integração real de Vitest + jsdom + React Testing Library + jest-dom.
import { afterEach, describe, expect, it } from 'vitest';
import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

afterEach(() => cleanup());

describe('infraestrutura de testes React', () => {
  it('renderiza React com jsdom, consulta por papel e executa matcher jest-dom', () => {
    render(React.createElement('button', { type: 'button', 'aria-label': 'Testar dependências' }, 'OK'));
    expect(screen.getByRole('button', { name: 'Testar dependências' })).toBeInTheDocument();
  });
});
