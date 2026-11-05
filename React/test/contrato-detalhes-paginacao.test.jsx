import React from 'react';
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
vi.mock('../src/core/ui/acessibilidadeDialogo.js', () => ({ useDialogoAcessivel: () => ({ current: null }) }));
vi.mock('../src/modules/admin/pages/PaginaContratos/components/PainelAlteracoesContrato/index.jsx', () => ({ PainelAlteracoesContrato: () => null }));
vi.mock('../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx', () => ({ PainelExcecoesContrato: () => null }));
import { ModalDetalhesContrato } from '../src/modules/admin/pages/PaginaContratos/components/ModalDetalhesContrato/index.jsx';
const six = Array.from({ length: 6 }, (_, i) => i + 1);
const props = {
  aberto: true, contrato: { id: 1, numeroContrato: 'SINTETICO', anoContrato: 2026, estadoNegocial: 'PUBLICADO' },
  eventos: six.map(id => ({ id, tipoEvento: 'PUBLICACAO', motivo: `Ato ${id}`, registradoEm: `2026-11-0${id}T12:00:00Z` })),
  versoes: six.map(id => ({ id, numeroVersao: id, numeroContrato: 'SINTETICO', anoContrato: 2026 })),
  precedencias: six.map(id => ({ id, tipoRelacao: 'SUCEDE', eventoFonteId: id, eventoAlvoId: id + 10 })),
  aoFechar: vi.fn(),
};
afterEach(cleanup);
it.each([
  ['Timeline auditável', 'Páginas da timeline contratual'],
  ['Versões negociais', 'Páginas das versões negociais'],
  ['Precedências', 'Páginas das precedências contratuais'],
])('%s apresenta cinco registros e permite alcançar o sexto', (title, label) => {
  render(<ModalDetalhesContrato {...props} />);
  const section = within(screen.getByRole('heading', { name: new RegExp(title) }).closest('section'));
  expect(section.getAllByRole('listitem')).toHaveLength(5);
  const navigation = section.getByRole('navigation', { name: label });
  fireEvent.click(within(navigation).getByRole('button', { name: 'Próxima' }));
  expect(section.getAllByRole('listitem')).toHaveLength(1);
  expect(within(navigation).getByRole('button', { name: 'Próxima' })).toBeDisabled();
  fireEvent.click(within(navigation).getByRole('button', { name: 'Anterior' }));
  expect(section.getAllByRole('listitem')).toHaveLength(5);
});
it('reinicia a página quando o diálogo passa para outro contrato', () => {
  const { rerender } = render(<ModalDetalhesContrato {...props} />);
  const navigation = screen.getByRole('navigation', { name: 'Páginas das versões negociais' });
  fireEvent.click(within(navigation).getByRole('button', { name: 'Próxima' }));
  rerender(<ModalDetalhesContrato {...props} contrato={{ ...props.contrato, id: 2 }} />);
  const section = within(screen.getByRole('heading', { name: /Versões negociais/ }).closest('section'));
  expect(section.getAllByRole('listitem')).toHaveLength(5);
  expect(within(screen.getByRole('navigation', { name: 'Páginas das versões negociais' })).getByRole('button', { name: 'Anterior' })).toBeDisabled();
});
