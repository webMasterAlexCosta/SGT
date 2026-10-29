import React, { useState, useCallback, useRef, useMemo } from 'react';
import { ModalConfirmacaoSenha } from './ModalConfirmacaoSenha';
import { useAutenticacao } from '../../modules/auth/context.jsx';

const MENSAGEM_PROTECAO_AVANCADA =
  'Sua conta está com Proteção Avançada ativa e o segundo fator foi validado nesta sessão. Não é necessário digitar sua senha novamente. Deseja continuar com esta ação?';

export const useConfirmacaoSenha = () => {
  const { user } = useAutenticacao();
  const protecaoAvancadaAtiva = Boolean(
    user?.twoFactorEnabled && user?.twoFactorVerified,
  );

  const [estado, setEstado] = useState({
    aberto: false,
    titulo: 'Confirmação de Segurança',
    mensagem: 'Para prosseguir, informe sua senha de acesso.',
    somenteConfirmacao: false,
  });

  const acaoPendenteRef = useRef(null);
  const resolvedorRef = useRef(null);

  const fechar = useCallback(() => {
    setEstado((s) => ({ ...s, aberto: false }));
    if (resolvedorRef.current) {
      resolvedorRef.current(false);
      resolvedorRef.current = null;
    }
    acaoPendenteRef.current = null;
  }, []);

  const confirmarComSenha = useCallback(
    ({ titulo, mensagem, acao, sempreExigirSenha = false }) => {
      return new Promise((resolve) => {
        const somenteConfirmacao = protecaoAvancadaAtiva && !sempreExigirSenha;
        resolvedorRef.current = resolve;
        acaoPendenteRef.current = acao;
        setEstado({
          aberto: true,
          titulo: titulo || 'Confirmação de Segurança',
          mensagem: somenteConfirmacao
            ? MENSAGEM_PROTECAO_AVANCADA
            : mensagem || 'Para prosseguir, informe sua senha de acesso.',
          somenteConfirmacao,
        });
      });
    },
    [protecaoAvancadaAtiva],
  );

  const lidarComConfirmacao = useCallback(async (senhaInformada) => {
    const acao = acaoPendenteRef.current;
    try {
      if (acao) {
        const resultado = await acao(senhaInformada);

        if (resultado === false) {
          throw { status: 400, message: 'Operação não confirmada.' };
        }
        if (
          resultado &&
          typeof resultado === 'object' &&
          typeof resultado.status === 'number' &&
          resultado.status >= 400
        ) {
          throw resultado;
        }
      }

      if (resolvedorRef.current) resolvedorRef.current(true);
      resolvedorRef.current = null;
      acaoPendenteRef.current = null;

      return true;
    } catch (erro) {
      throw erro;
    }
  }, []);

  const ModalConfirmacao = useMemo(() => {
    const Componente = () => (
      <ModalConfirmacaoSenha
        aberto={estado.aberto}
        aoFechar={fechar}
        aoConfirmar={lidarComConfirmacao}
        titulo={estado.titulo}
        mensagem={estado.mensagem}
        somenteConfirmacao={estado.somenteConfirmacao}
        subtitulo={
          estado.somenteConfirmacao
            ? 'Proteção avançada ativa'
            : 'Assinatura digital requerida'
        }
        rotuloConfirmar={estado.somenteConfirmacao ? 'Continuar' : 'Confirmar'}
      />
    );
    Componente.displayName = 'ModalConfirmacaoWrapper';
    return Componente;
  }, [
    estado.aberto,
    estado.titulo,
    estado.mensagem,
    estado.somenteConfirmacao,
    fechar,
    lidarComConfirmacao,
  ]);

  return {
    confirmarComSenha,
    ModalConfirmacao,
    fechar,
    protecaoAvancadaAtiva,
  };
};
