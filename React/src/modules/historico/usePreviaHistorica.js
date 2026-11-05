import { useCallback, useEffect, useRef, useState } from 'react';
import { gerarChaveIdempotencia } from '../../core/http/idempotencia.js';
import { obterGeracaoSessao, obterSinalSessao, sessaoAtual } from '../../core/security/geracaoSessao.js';
import { ServicoHistorico } from '../../services/historico/ServicoHistorico.js';
import { PREPARADORES_HISTORICOS } from '../../services/historico/dominioHistorico.js';

const copiar = (valor) => JSON.parse(JSON.stringify(valor));

/** Uma prévia pertence a uma intenção e ao seu alvo, nunca ao formulário futuro. */
export const usePreviaHistorica = ({ tipo, alvo, formulario, permitido, aoSucesso }) => {
  const geracaoSessao = obterGeracaoSessao();
  const assinatura = JSON.stringify([geracaoSessao, tipo, alvo, formulario, permitido]);
  const atualRef = useRef(assinatura);
  atualRef.current = assinatura;
  const anteriorRef = useRef(assinatura);
  const montadoRef = useRef(true);
  const requisicaoRef = useRef(null);
  const intencaoRef = useRef(null);
  const previaRef = useRef(null);
  const incertoRef = useRef(null);
  const [previa, setPrevia] = useState(null);
  const [confirmarImpacto, setConfirmarImpacto] = useState(false);
  const [processando, setProcessando] = useState(null);
  const [erro, setErro] = useState(null);
  const [recibo, setRecibo] = useState(null);
  const [incerto, setIncerto] = useState(false);

  const invalidar = useCallback(() => {
    if (requisicaoRef.current?.tipo === 'previa') {
      requisicaoRef.current.controlador.abort();
      requisicaoRef.current = null;
      setProcessando(null);
    }
    previaRef.current = null;
    intencaoRef.current = null;
    incertoRef.current = null;
    setPrevia(null);
    setConfirmarImpacto(false);
    setIncerto(false);
    setRecibo(null);
    setErro(null);
  }, []);

  useEffect(() => {
    if (anteriorRef.current !== assinatura) {
      anteriorRef.current = assinatura;
      invalidar();
    }
  }, [assinatura, invalidar]);
  useEffect(() => {
    montadoRef.current = true;
    return () => {
      montadoRef.current = false;
      requisicaoRef.current?.controlador?.abort();
    };
  }, []);
  useEffect(() => {
    const sinal = obterSinalSessao(geracaoSessao);
    const encerrar = () => {
      requisicaoRef.current?.controlador.abort();
      requisicaoRef.current = null;
      setProcessando(null);
      invalidar();
    };
    sinal.addEventListener('abort', encerrar, { once: true });
    if (sinal.aborted) encerrar();
    return () => sinal.removeEventListener('abort', encerrar);
  }, [geracaoSessao, invalidar]);

  const gerarPrevia = async () => {
    if (!sessaoAtual(geracaoSessao) || !permitido || requisicaoRef.current || recibo || incertoRef.current) return;
    let intencao;
    try {
      const dados = PREPARADORES_HISTORICOS[tipo](formulario);
      intencao = intencaoRef.current?.assinatura === assinatura ? intencaoRef.current : {
        assinatura, geracaoSessao, tipo, alvo: copiar(alvo),
        dados: { ...dados, chaveIdempotencia: gerarChaveIdempotencia(`historico-${tipo}`) },
      };
    } catch (falha) { setErro(falha.message); return; }
    intencaoRef.current = intencao;
    previaRef.current = null;
    setPrevia(null);
    setConfirmarImpacto(false);
    setErro(null);
    const requisicao = { tipo: 'previa', controlador: new AbortController() };
    requisicaoRef.current = requisicao; // proteção síncrona, inclusive antes do próximo render
    setProcessando('previa');
    try {
      const resposta = await ServicoHistorico.analisar(tipo, intencao.alvo, copiar(intencao.dados), { signal: requisicao.controlador.signal });
      if (!sessaoAtual(intencao.geracaoSessao) || !montadoRef.current || atualRef.current !== intencao.assinatura || requisicaoRef.current !== requisicao || requisicao.controlador.signal.aborted) return;
      if (!resposta?.tokenPrevia || typeof resposta.tokenPrevia !== 'string') throw new Error('O servidor não retornou uma prévia confirmável. Refaça a consulta.');
      previaRef.current = { ...intencao, resposta: copiar(resposta) };
      setPrevia(resposta);
    } catch (falha) {
      if (sessaoAtual(intencao.geracaoSessao) && montadoRef.current && atualRef.current === intencao.assinatura && requisicaoRef.current === requisicao && !requisicao.controlador.signal.aborted && !falha?.canceled) {
        setErro(falha?.message || 'Não foi possível analisar a fonte histórica.');
      }
    } finally {
      if (requisicaoRef.current === requisicao) {
        requisicaoRef.current = null;
        if (montadoRef.current && sessaoAtual(intencao.geracaoSessao)) setProcessando(null);
      }
    }
  };

  const confirmar = async (repetir = false) => {
    if (!sessaoAtual(geracaoSessao) || !permitido || requisicaoRef.current || recibo) return;
    let envio;
    if (repetir) envio = incertoRef.current;
    else {
      const snapshot = previaRef.current;
      if (!snapshot || !confirmarImpacto || incertoRef.current || snapshot.assinatura !== atualRef.current) return;
      envio = { ...snapshot, payload: { ...copiar(snapshot.dados), tokenPrevia: snapshot.resposta.tokenPrevia, confirmarImpacto: true } };
    }
    if (!envio || !sessaoAtual(envio.geracaoSessao) || envio.assinatura !== atualRef.current) return;
    const requisicao = { tipo: 'confirmacao', controlador: new AbortController() };
    requisicaoRef.current = requisicao;
    setProcessando('confirmacao');
    setErro(null);
    try {
      const resposta = await ServicoHistorico.confirmar(envio.tipo, envio.alvo, copiar(envio.payload), { signal: requisicao.controlador.signal });
      if (!sessaoAtual(envio.geracaoSessao) || !montadoRef.current || atualRef.current !== envio.assinatura || requisicaoRef.current !== requisicao || requisicao.controlador.signal.aborted) return;
      if (!resposta || typeof resposta !== 'object') throw new Error('O servidor não retornou o recibo da operação.');
      previaRef.current = null;
      incertoRef.current = null;
      setPrevia(null);
      setConfirmarImpacto(false);
      setIncerto(false);
      setRecibo(resposta);
      aoSucesso?.(resposta);
    } catch (falha) {
      if (!sessaoAtual(envio.geracaoSessao) || !montadoRef.current || atualRef.current !== envio.assinatura || requisicaoRef.current !== requisicao || requisicao.controlador.signal.aborted) return;
      const status = Number(falha?.status || falha?.response?.status || 0);
      const resultadoIncerto = !falha?.canceled && (falha?.resultadoIncerto || status === 0 || status >= 500);
      if (resultadoIncerto) {
        incertoRef.current = envio;
        setIncerto(true);
        setErro(`Resultado incerto. A resposta de confirmação não foi obtida. Consulte a cadeia ou repita a mesma intenção com a chave preservada. ${falha?.message || ''}`.trim());
      } else {
        previaRef.current = null;
        incertoRef.current = null;
        setPrevia(null);
        setConfirmarImpacto(false);
        setIncerto(false);
        setErro(`${falha?.message || 'Não foi possível incorporar a fonte.'} Gere uma nova prévia antes de confirmar.`);
      }
    } finally {
      if (requisicaoRef.current === requisicao) {
        requisicaoRef.current = null;
        if (montadoRef.current && sessaoAtual(envio.geracaoSessao)) setProcessando(null);
      }
    }
  };

  return { previa, confirmarImpacto, setConfirmarImpacto, processando, erro, recibo, incerto, gerarPrevia, confirmar, invalidar };
};
