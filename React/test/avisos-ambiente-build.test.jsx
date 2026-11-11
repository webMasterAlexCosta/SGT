import React from 'react';
import '@testing-library/jest-dom/vitest';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';

const config = vi.hoisted(() => ({ APP_NAME: 'SGT', HOMOLOGACAO_EXPIRY_DAYS: 30, BUILD_PRODUCAO: false }));
vi.mock('../src/core/config', () => ({ CONFIG: config }));
import Homologacao from '../src/core/ui/Homologacao.tsx';
import HomologacaoModal from '../src/core/ui/HomologacaoModal.tsx';

beforeEach(() => { config.BUILD_PRODUCAO = false; localStorage.clear(); });
afterEach(() => { cleanup(); vi.restoreAllMocks(); });

describe('Avisos de ambiente no artefato de entrega', () => {
  it('não classifica o build de entrega como desenvolvimento', () => {
    config.BUILD_PRODUCAO = true;
    const { container } = render(<Homologacao />);
    expect(container).toBeEmptyDOMElement();
  });

  it('não afirma que alterações em produção são somente testes', () => {
    config.BUILD_PRODUCAO = true;
    render(<HomologacaoModal />);
    expect(screen.queryByText(/não afetam o ambiente oficial de produção/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/quebrar/i)).not.toBeInTheDocument();
  });

  it('nem consulta estado persistido de aviso de desenvolvimento no build de entrega', () => {
    config.BUILD_PRODUCAO = true;
    localStorage.setItem('ocultarHomologacao_SGT_Expiracao', '{estado-invalido');
    const read = vi.spyOn(Storage.prototype, 'getItem');
    expect(() => render(<HomologacaoModal />)).not.toThrow();
    expect(read).not.toHaveBeenCalled();
  });

  it('mantém o aviso e seu fechamento no desenvolvimento', () => {
    render(<><Homologacao /><HomologacaoModal /></>);
    expect(screen.getByText('DESENVOLVIMENTO')).toBeInTheDocument();
    expect(screen.getByText('Ambiente de DESENVOLVIMENTO')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /ciente/i }));
    expect(screen.queryByText('Ambiente de DESENVOLVIMENTO')).not.toBeInTheDocument();
  });
});
