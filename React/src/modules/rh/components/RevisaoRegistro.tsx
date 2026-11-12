import React, { useEffect, useRef, useState } from 'react';
import type { RevisaoFrequencia } from '../../../contracts/revisao-frequencia';
import { ServicoFrequencia } from '../../../services/rh/ServicoFrequencia';
import { obterGeracaoSessao, sessaoAtual } from '../../../core/security/geracaoSessao';
import estilos from './RevisaoRegistro.module.css';

export function RevisaoRegistro({ id, aoAtualizar }: { id: number; aoAtualizar?: () => void }) {
  const [dados, setDados] = useState<RevisaoFrequencia | null>(null);
  const [motivo, setMotivo] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [revisao, setRevisao] = useState(0);
  const envio = useRef(false);
  const chave = useRef(crypto.randomUUID());
  useEffect(() => {
    const controle = new AbortController(); const geracao = obterGeracaoSessao();
    setDados(null); setErro(null);
    ServicoFrequencia.consultarRevisao(id,{ signal:controle.signal,geracaoSessao:geracao }).then(d => { if(!controle.signal.aborted && sessaoAtual(geracao)) setDados(d); })
      .catch((e:unknown) => { if(!controle.signal.aborted && sessaoAtual(geracao)) setErro(e instanceof Error?e.message:'Não foi possível consultar a revisão.'); });
    return () => controle.abort();
  },[id,revisao]);
  const enviar = async (decisao?: 'CONFERIDA' | 'REJEITADA') => {
    if(!dados || envio.current || motivo.trim().length<5) return;
    envio.current=true;setSalvando(true);setErro(null);const geracao=obterGeracaoSessao();
    try {
      const nova=await ServicoFrequencia.comandarRevisao(id,{ versao:dados.versao,motivo:motivo.trim(),chaveIdempotencia:chave.current,decisao },Boolean(decisao),{geracaoSessao:geracao,successMessage:decisao?'Decisão de revisão registrada.':'Solicitação de revisão registrada.'});
      if(!sessaoAtual(geracao)) return;
      setDados(nova);setMotivo('');chave.current=crypto.randomUUID();aoAtualizar?.();
    } catch(e:unknown) { if(sessaoAtual(geracao)) setErro(e instanceof Error?e.message:'Não foi possível registrar. Confira o estado antes de reenviar.'); }
    finally { envio.current=false;if(sessaoAtual(geracao)) setSalvando(false); }
  };
  return <section className={estilos.revisao} aria-label={`Revisão do registro ${id}`}><h3>Conferência / correção · registro #{id}</h3>
    {erro && <p role="alert">{erro}<button type="button" disabled={salvando} onClick={() => {chave.current=crypto.randomUUID();setRevisao(v=>v+1);}}>Atualizar estado</button></p>}
    {!dados ? <p role="status">Consultando a solicitação…</p> : <><p><strong>Situação:</strong> {({ SEM_SOLICITACAO:'Sem solicitação',PENDENTE:'Aguardando Gestor Local',CONFERIDA:'Conferida pelo Gestor Local',REJEITADA:'Rejeitada pelo Gestor Local' })[dados.status]}</p>
      {dados.motivo && <p><strong>Solicitação:</strong> {dados.motivo}</p>}{dados.decisao && <p><strong>Decisão:</strong> {dados.decisao}</p>}
      {(dados.podeSolicitar || dados.podeDecidir) && <form onSubmit={e => {e.preventDefault();void enviar(dados.podeDecidir?'CONFERIDA':undefined);}}>
        <p className={estilos.nota}>{dados.podeDecidir?'Se houver correção, execute a retificação no módulo Frequência antes de concluir a conferência. A decisão fica na trilha de auditoria.':'Descreva o que precisa ser conferido. O Gestor Local decidirá e executará eventual correção.'}</p>
        <label>{dados.podeDecidir?'Justificativa da decisão':'Motivo da solicitação'}<textarea required minLength={5} maxLength={1000} value={motivo} disabled={salvando} onChange={e => {setMotivo(e.target.value);chave.current=crypto.randomUUID();}} /></label>
        <div><button type="submit" disabled={salvando || motivo.trim().length<5}>{salvando?'Registrando…':dados.podeDecidir?'Concluir conferência':'Solicitar revisão'}</button>{dados.podeDecidir && <button type="button" disabled={salvando || motivo.trim().length<5} onClick={() => void enviar('REJEITADA')}>Rejeitar solicitação</button>}</div>
      </form>}
    </>}
  </section>;
}
