import { observarFalha } from '../http/falhaObservada';
import type { PasswordConfirmationContext } from '../http/types';
interface PasswordRequest extends PasswordConfirmationContext { resolve: (valor: unknown) => void; reject: (erro: unknown) => void }
import React, { useCallback, useEffect, useState } from 'react';
import { ModalConfirmacaoSenha } from './ModalConfirmacaoSenha';
import {
  criarErroConfirmacaoCancelada,
  registrarSolicitadorConfirmacaoSenha,
} from '../http/coordenadorConfirmacaoSenha.ts';

export const ProvedorConfirmacaoSenhaHttp = ({ children }: React.PropsWithChildren) => {
  const [solicitacao, setSolicitacao] = useState<PasswordRequest | null>(null);

  const abrirSolicitacao = useCallback((contexto: PasswordConfirmationContext) => new Promise<unknown>((resolve, reject) => {
    const signal = contexto?.signal;
    if (signal?.aborted) { reject(criarErroConfirmacaoCancelada()); return; }
    let encerrada = false;
    const finalizar = (acao: ((value: unknown) => void), valor: unknown) => {
      if (encerrada) return;
      encerrada = true;
      signal?.removeEventListener('abort', cancelar);
      acao(valor);
    };
    const pedido = { ...contexto, resolve: (valor: unknown) => finalizar(resolve, valor), reject: (erro: unknown) => finalizar(reject, erro) };
    const cancelar = () => {
      pedido.reject(criarErroConfirmacaoCancelada());
      setSolicitacao(atual => atual === pedido ? null : atual);
    };
    signal?.addEventListener('abort', cancelar, { once: true });
    setSolicitacao(pedido);
  }), []);

  useEffect(() => {
    const remover = registrarSolicitadorConfirmacaoSenha(abrirSolicitacao);
    return () => {
      remover();
      setSolicitacao((atual) => {
        atual?.reject?.(criarErroConfirmacaoCancelada());
        return null;
      });
    };
  }, [abrirSolicitacao]);

  const fechar = useCallback(() => {
    setSolicitacao((atual) => {
      atual?.reject?.(criarErroConfirmacaoCancelada());
      return null;
    });
  }, []);

  const confirmar = useCallback(async (senha?: string) => {
    const atual = solicitacao;
    if (!atual?.executar || senha === undefined) return false;
    if (atual.signal?.aborted) {
      atual.reject?.(criarErroConfirmacaoCancelada());
      setSolicitacao(pedido => pedido === atual ? null : pedido);
      return true;
    }

    try {
      const resultado = await atual.executar(senha);
      atual.resolve?.(resultado);
      setSolicitacao(pedido => pedido === atual ? null : pedido);
      return true;
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!atual.signal?.aborted && (erro?.senhaConfirmacaoInvalida || erro?.code === 'INVALID_CONFIRMATION_PASSWORD')) {
        throw erroOriginal;
      }
      atual.reject?.(erroOriginal);
      setSolicitacao(pedido => pedido === atual ? null : pedido);
      return true;
    }
  }, [solicitacao]);

  return (
    <>
      {children}
      <ModalConfirmacaoSenha
        aberto={Boolean(solicitacao)}
        aoFechar={fechar}
        aoConfirmar={confirmar}
        titulo={solicitacao?.titulo || 'Confirmação de Segurança'}
        mensagem={solicitacao?.mensagem || 'Informe sua senha para concluir a operação.'}
        acao={solicitacao?.acao}
        entidade={solicitacao?.entidade}
        consequencia={solicitacao?.consequencia}
        subtitulo="Confirmação exigida pelo servidor"
      />
    </>
  );
};
