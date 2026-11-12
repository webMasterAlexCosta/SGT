import React from 'react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import HomologacaoModal from '../src/core/ui/HomologacaoModal';
import { CONFIG } from '../src/core/config';

const chave = `ocultarHomologacao_${CONFIG.APP_NAME}_Expiracao`;

beforeEach(() => { localStorage.removeItem(chave); });
afterEach(() => { cleanup(); vi.restoreAllMocks(); localStorage.removeItem(chave); });

it('identifica o aviso como diálogo, contém o foco e permite sair pelo teclado', async () => {
  render(<><button>Origem</button><HomologacaoModal /></>);
  const dialogo = screen.getByRole('dialog', { name: 'Ambiente de DESENVOLVIMENTO' });
  const ciente = screen.getByRole('button', { name: 'Ciente, Vamos lá!' });
  const lembrar = screen.getByRole('checkbox', { name: 'Não mostrar por 30 dias' });
  expect(dialogo).toHaveAttribute('aria-modal', 'true');
  await waitFor(() => expect(screen.getByRole('heading', { name: 'Ambiente de DESENVOLVIMENTO' })).toHaveFocus());
  fireEvent.keyDown(document.activeElement!, { key: 'Tab', shiftKey: true });
  expect(ciente).toHaveFocus();
  ciente.focus();
  fireEvent.keyDown(ciente, { key: 'Tab' });
  expect(lembrar).toHaveFocus();
  fireEvent.keyDown(lembrar, { key: 'Tab', shiftKey: true });
  expect(ciente).toHaveFocus();
  fireEvent.keyDown(ciente, { key: 'Escape' });
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

it.each(['conteudo-invalido', '"sem-data"', '{}', 'true', 'null'])('preferência inválida %s não interrompe a aplicação nem oculta o aviso', valor => {
  localStorage.setItem(chave, valor);
  expect(() => render(<HomologacaoModal />)).not.toThrow();
  expect(screen.getByRole('dialog')).toBeVisible();
});

it('exibe novamente quando a preferência expirou', () => {
  localStorage.setItem(chave, JSON.stringify(Date.now() - 1));
  render(<HomologacaoModal />);
  expect(screen.getByRole('dialog')).toBeVisible();
});

it('preserva uma preferência válida ainda vigente', () => {
  localStorage.setItem(chave, JSON.stringify(Date.now() + 60_000));
  render(<HomologacaoModal />);
  expect(screen.queryByText('Ambiente de DESENVOLVIMENTO')).not.toBeInTheDocument();
});

it('abre o aviso mesmo quando o navegador bloqueia a leitura do armazenamento', () => {
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new DOMException('Bloqueado', 'SecurityError'); });
  expect(() => render(<HomologacaoModal />)).not.toThrow();
  expect(screen.getByRole('dialog')).toBeVisible();
});

it('permite fechar mesmo quando não é possível persistir a preferência', () => {
  render(<HomologacaoModal />);
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Sem espaço', 'QuotaExceededError'); });
  fireEvent.click(screen.getByRole('checkbox', { name: 'Não mostrar por 30 dias' }));
  fireEvent.click(screen.getByRole('button', { name: 'Ciente, Vamos lá!' }));
  expect(screen.queryByText('Ambiente de DESENVOLVIMENTO')).not.toBeInTheDocument();
});
