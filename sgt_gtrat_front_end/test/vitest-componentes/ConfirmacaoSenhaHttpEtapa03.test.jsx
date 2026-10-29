import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ModalConfirmacaoSenha } from '../../src/core/security/ModalConfirmacaoSenha.jsx';

describe('Etapa 03 - modal semântico de confirmação de senha', () => {
  it('exibe ação, entidade e consequência e limpa a senha após tentativa inválida', async () => {
    const erro = Object.assign(new Error('Senha de confirmação inválida.'), {
      status: 403,
      code: 'INVALID_CONFIRMATION_PASSWORD',
      senhaConfirmacaoInvalida: true,
    });
    const confirmar = vi.fn().mockRejectedValue(erro);

    render(
      <ModalConfirmacaoSenha
        aberto
        aoFechar={() => {}}
        aoConfirmar={confirmar}
        titulo="Confirmar alteração"
        mensagem="O servidor exige nova confirmação."
        acao="Inativar usuário"
        entidade="Usuário 42"
        consequencia="O acesso será bloqueado imediatamente."
      />,
    );

    expect(screen.getByText(/Inativar usuário/)).toBeInTheDocument();
    expect(screen.getByText(/Usuário 42/)).toBeInTheDocument();
    expect(screen.getByText(/acesso será bloqueado imediatamente/i)).toBeInTheDocument();

    const input = screen.getByLabelText(/Senha de Acesso/i);
    fireEvent.change(input, { target: { value: 'senha-incorreta' } });
    fireEvent.click(screen.getByRole('button', { name: /^Confirmar$/i }));

    await waitFor(() => expect(confirmar).toHaveBeenCalledWith('senha-incorreta'));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Senha de confirmação inválida.'));
    expect(input).toHaveValue('');
  });
});
