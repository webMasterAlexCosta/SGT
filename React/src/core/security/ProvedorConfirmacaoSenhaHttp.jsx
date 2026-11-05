import React, { useCallback, useEffect, useState } from 'react';
import { ModalConfirmacaoSenha } from './ModalConfirmacaoSenha';
import {
  criarErroConfirmacaoCancelada,
  registrarSolicitadorConfirmacaoSenha,
} from '../http/coordenadorConfirmacaoSenha.js';

export const ProvedorConfirmacaoSenhaHttp = ({ children }) => {
  const [solicitacao, setSolicitacao] = useState(null);

  const abrirSolicitacao = useCallback((contexto) => new Promise((resolve, reject) => {
    const signal = contexto?.signal;
    if (signal?.aborted) { reject(criarErroConfirmacaoCancelada()); return; }
    let encerrada = false;
    const finalizar = (acao, valor) => {
      if (encerrada) return;
      encerrada = true;
      signal?.removeEventListener('abort', cancelar);
      acao(valor);
    };
    const pedido = { ...contexto, resolve: valor => finalizar(resolve, valor), reject: erro => finalizar(reject, erro) };
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

  const confirmar = useCallback(async (senha) => {
    const atual = solicitacao;
    if (!atual?.executar) return false;
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
    } catch (erro) {
      if (!atual.signal?.aborted && (erro?.senhaConfirmacaoInvalida || erro?.code === 'INVALID_CONFIRMATION_PASSWORD')) {
        throw erro;
      }
      atual.reject?.(erro);
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
