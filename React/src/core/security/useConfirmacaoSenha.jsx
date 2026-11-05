import React, { useState, useCallback, useRef, useMemo, useEffect } from 'react';
import { ModalConfirmacaoSenha } from './ModalConfirmacaoSenha';
import { useAutenticacao } from '../../modules/auth/context.jsx';
import { obterGeracaoSessao, obterSinalSessao, sessaoAtual } from './geracaoSessao.js';

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
    pedido: null,
  });

  const pedidoRef = useRef(null);
  const montadoRef = useRef(true);

  const finalizarPedido = useCallback((pedido, resultado, atualizarVisual = true) => {
    if (!pedido || pedido.finalizado) return;
    pedido.finalizado = true;
    pedido.signal.removeEventListener('abort', pedido.cancelar);
    pedido.resolve(resultado);
    pedido.acao = null;
    if (pedidoRef.current === pedido) {
      pedidoRef.current = null;
      if (atualizarVisual && montadoRef.current) setEstado(s => ({ ...s, aberto: false, pedido: null }));
    }
  }, []);

  const fechar = useCallback(() => {
    finalizarPedido(pedidoRef.current, false);
  }, [finalizarPedido]);

  useEffect(() => {
    montadoRef.current = true;
    return () => {
      montadoRef.current = false;
      finalizarPedido(pedidoRef.current, false, false);
    };
  }, [finalizarPedido]);

  const confirmarComSenha = useCallback(
    ({ titulo, mensagem, acao, sempreExigirSenha = false }) => {
      if (!montadoRef.current) return Promise.resolve(false);
      finalizarPedido(pedidoRef.current, false);
      return new Promise((resolve) => {
        const somenteConfirmacao = protecaoAvancadaAtiva && !sempreExigirSenha;
        const geracao = obterGeracaoSessao();
        const signal = obterSinalSessao(geracao);
        const pedido = { acao, resolve, geracao, signal, finalizado: false, executando: false };
        pedido.cancelar = () => finalizarPedido(pedido, false);
        pedidoRef.current = pedido;
        signal.addEventListener('abort', pedido.cancelar, { once: true });
        setEstado({
          aberto: true,
          titulo: titulo || 'Confirmação de Segurança',
          mensagem: somenteConfirmacao
            ? MENSAGEM_PROTECAO_AVANCADA
            : mensagem || 'Para prosseguir, informe sua senha de acesso.',
          somenteConfirmacao,
          pedido,
        });
      });
    },
    [finalizarPedido, protecaoAvancadaAtiva],
  );

  const lidarComConfirmacao = useCallback(async (pedido, senhaInformada) => {
    const atual = () => Boolean(pedido) && montadoRef.current && pedidoRef.current === pedido && !pedido?.finalizado && sessaoAtual(pedido.geracao);
    if (!atual() || pedido.executando) return false;
    pedido.executando = true;
    try {
      if (pedido.acao) {
        const resultado = await pedido.acao(senhaInformada);
        if (!atual()) return false;
        if (resultado === false) throw { status: 400, message: 'Operação não confirmada.' };
        if (resultado && typeof resultado === 'object' && typeof resultado.status === 'number' && resultado.status >= 400) throw resultado;
      }
      if (!atual()) return false;
      finalizarPedido(pedido, true);
      return true;
    } catch (erro) {
      if (!atual() || erro?.canceled) {
        finalizarPedido(pedido, false);
        return false;
      }
      throw erro;
    } finally {
      pedido.executando = false;
    }
  }, [finalizarPedido]);

  const ModalConfirmacao = useMemo(() => {
    const Componente = () => (
      <ModalConfirmacaoSenha
        aberto={estado.aberto}
        aoFechar={() => finalizarPedido(estado.pedido, false)}
        aoConfirmar={(senha) => lidarComConfirmacao(estado.pedido, senha)}
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
    estado.pedido,
    estado.aberto,
    estado.titulo,
    estado.mensagem,
    estado.somenteConfirmacao,
    finalizarPedido,
    lidarComConfirmacao,
  ]);

  return {
    confirmarComSenha,
    ModalConfirmacao,
    fechar,
    protecaoAvancadaAtiva,
  };
};
