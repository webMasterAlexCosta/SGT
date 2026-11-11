import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { useDialogoAcessivel } from '../src/core/ui/acessibilidadeDialogo.ts';

function Dialogo({ aberto, nome, aoFechar }) {
  const ref = useDialogoAcessivel({ aberto, aoFechar });
  return aberto ? <section role="dialog" aria-modal="true" aria-label={nome} ref={ref}><button>{nome}</button></section> : null;
}
beforeEach(() => { document.body.style.overflow = 'auto'; });
afterEach(cleanup);

describe('diálogos sobrepostos', () => {
  it('ESC atua somente no diálogo superior', () => {
    const inferior = vi.fn(); const superior = vi.fn();
    render(<><Dialogo aberto nome="Consulta" aoFechar={inferior} /><Dialogo aberto nome="Confirmação" aoFechar={superior} /></>);
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(superior).toHaveBeenCalledTimes(1);
    expect(inferior).not.toHaveBeenCalled();
  });
  it('mantém a rolagem bloqueada quando o diálogo inferior é desmontado', () => {
    const fechar = vi.fn();
    const { rerender } = render(<><Dialogo aberto nome="Consulta" aoFechar={fechar} /><Dialogo aberto nome="Confirmação" aoFechar={fechar} /></>);
    rerender(<><Dialogo aberto={false} nome="Consulta" aoFechar={fechar} /><Dialogo aberto nome="Confirmação" aoFechar={fechar} /></>);
    expect(document.body.style.overflow).toBe('hidden');
    rerender(<><Dialogo aberto={false} nome="Consulta" aoFechar={fechar} /><Dialogo aberto={false} nome="Confirmação" aoFechar={fechar} /></>);
    expect(document.body.style.overflow).toBe('auto');
  });
  it('não fecha o formulário subjacente quando há confirmação de senha em portal', () => {
    const fechar = vi.fn();
    render(<><Dialogo aberto nome="Formulário" aoFechar={fechar} /><section role="dialog" aria-modal="true" aria-label="Senha"><input aria-label="Senha" /></section></>);
    fireEvent.keyDown(screen.getByLabelText('Senha', { selector: 'input' }), { key: 'Escape' });
    expect(fechar).not.toHaveBeenCalled();
  });
});
