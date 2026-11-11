import React, { useId, useLayoutEffect, useState } from 'react';
import type { AuthenticatedUser } from '../../../../contracts/auth';
import type { OcorrenciaHistoricoDTO } from '../../../../contracts/java';
import { useConsultaVigente } from '../../../../core/hooks/useConsultaVigente';
import { observarFalha } from '../../../../core/http/falhaObservada';
import { mensagemFalha } from '../../../../core/http/mensagemFalha';
import { obterGeracaoSessao } from '../../../../core/security/geracaoSessao';
import { Botao } from '../../../../core/ui/EnvolucroDica';
import { ServicoOcorrencia } from '../../../../services/rh/ServicoOcorrencia';
import { AnexoOcorrencia } from './AnexoOcorrencia';
import estilos from './ConsultaOcorrenciaEspecifica.module.css';

/** Explicit purpose-bound read. Grants never imply any write command here. */
export const ConsultaOcorrenciaEspecifica = ({ usuario }: { usuario: AuthenticatedUser | null }) => {
  const prefixo = useId();
  const [id, setId] = useState('');
  const [finalidade, setFinalidade] = useState('');
  const [detalhe, setDetalhe] = useState<OcorrenciaHistoricoDTO | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState('');
  const [idInvalido, setIdInvalido] = useState(false);
  const sessao = JSON.stringify([usuario?.id, usuario?.perfil, obterGeracaoSessao()]);
  const contexto = JSON.stringify([sessao, id, finalidade]);
  const consultas = useConsultaVigente(contexto);
  useLayoutEffect(() => {
    setDetalhe(null); setCarregando(false); setErro(''); setIdInvalido(false);
    return consultas.invalidar;
  }, [contexto, consultas]);
  useLayoutEffect(() => { setId(''); setFinalidade(''); }, [sessao]);

  const consultar = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (carregando) return;
    const numero = Number(id);
    if (!/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(numero)) {
      setIdInvalido(true); setErro('Informe um ID de ocorrência inteiro positivo válido.'); return;
    }
    if (!finalidade.trim()) { setErro('Informe a finalidade desta consulta.'); return; }
    const consulta = consultas.iniciar();
    setDetalhe(null); setErro(''); setCarregando(true);
    try {
      const resposta = await ServicoOcorrencia.consultarHistorico(numero, finalidade, { signal: consulta.signal, silentError: true });
      if (consulta.atual()) setDetalhe(resposta);
    } catch (falhaOriginal) {
      const falha = observarFalha(falhaOriginal);
      if (consulta.atual() && !falha.canceled) setErro(mensagemFalha(falha, 'Não foi possível consultar esta ocorrência.'));
    } finally { if (consulta.atual()) setCarregando(false); }
  };
  const ocorrencia = detalhe?.ocorrencia;
  return <section className={estilos.consulta} aria-label="Leitura específica de ocorrência">
    <h2>Consultar ocorrência por ID</h2>
    <p>Informe a finalidade do acesso. A leitura exige autorização vigente para este registro.</p>
    <form aria-label="Consultar ocorrência por ID" className={estilos.formulario} onSubmit={consultar}>
      <label htmlFor={`${prefixo}-id`}>ID da ocorrência<input id={`${prefixo}-id`} inputMode="numeric" value={id}
        onChange={evento => setId(evento.target.value)} aria-invalid={idInvalido} aria-describedby={erro ? `${prefixo}-erro` : undefined} /></label>
      <label htmlFor={`${prefixo}-finalidade`}>Finalidade da consulta<input id={`${prefixo}-finalidade`} value={finalidade}
        onChange={evento => setFinalidade(evento.target.value)} maxLength={1000} required /></label>
      <Botao type="submit" disabled={carregando}>{carregando ? 'Consultando…' : 'Consultar ocorrência'}</Botao>
      {erro && <p id={`${prefixo}-erro`} role="alert" className={estilos.erro}>{erro}</p>}
    </form>
    {ocorrencia && <section aria-label="Ocorrência consultada" className={estilos.resultado}>
      <h3>Ocorrência #{ocorrencia.id}</h3>
      <dl>
        <div><dt>Motivo</dt><dd>{ocorrencia.motivo}</dd></div>
        <div><dt>Data inicial</dt><dd><time dateTime={ocorrencia.dataOcorrencia ?? undefined}>{ocorrencia.dataOcorrencia?.split('-').reverse().join('/') ?? 'Não informada'}</time></dd></div>
        <div><dt>Quantidade de dias</dt><dd>{ocorrencia.quantidadeDias ?? 'Não informada'}</dd></div>
        <div><dt>Situação</dt><dd>{detalhe.cancelada ? 'Cancelada' : 'Não cancelada'}</dd></div>
        {ocorrencia.empregado?.nome && <div><dt>Colaborador</dt><dd>{ocorrencia.empregado.nome}</dd></div>}
        {ocorrencia.numeroSei && <div><dt>Referência SEI</dt><dd>{ocorrencia.numeroSei}</dd></div>}
      </dl>
      {ocorrencia.observacao && <p className={estilos.narrativa}>{ocorrencia.observacao}</p>}
      {detalhe.cancelada && detalhe.motivoCancelamento && <p>Motivo do cancelamento: {detalhe.motivoCancelamento}</p>}
      {ocorrencia.anexos?.map(anexo => <AnexoOcorrencia key={anexo.id} anexo={anexo} usuario={usuario} />)}
    </section>}
  </section>;
};
