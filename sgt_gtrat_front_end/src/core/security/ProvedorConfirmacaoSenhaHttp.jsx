import React, { useCallback, useEffect, useState } from 'react';
import { ModalConfirmacaoSenha } from './ModalConfirmacaoSenha';
import {
  criarErroConfirmacaoCancelada,
  registrarSolicitadorConfirmacaoSenha,
} from '../http/coordenadorConfirmacaoSenha.js';

export const ProvedorConfirmacaoSenhaHttp = ({ children }) => {
  const [solicitacao, setSolicitacao] = useState(null);

  const abrirSolicitacao = useCallback((contexto) => new Promise((resolve, reject) => {
    setSolicitacao({ ...contexto, resolve, reject });
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

    try {
      const resultado = await atual.executar(senha);
      atual.resolve?.(resultado);
      setSolicitacao(null);
      return true;
    } catch (erro) {
      if (erro?.senhaConfirmacaoInvalida || erro?.code === 'INVALID_CONFIRMATION_PASSWORD') {
        throw erro;
      }
      atual.reject?.(erro);
      setSolicitacao(null);
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
