import React, { useEffect, useState } from 'react';
import type { PaginaRegistrosAnalitico, PainelAnaliticoDTO } from '../../../../contracts/painel-analitico';
import { ServicoPainel } from '../../../../services/painel/ServicoPainel';
import { obterGeracaoSessao, sessaoAtual } from '../../../../core/security/geracaoSessao';
import type { DetalheDesempenho } from './GraficosDesempenho';
import { dataCivil } from './formatos';
import estilos from './index.module.css';
import { RevisaoRegistro } from '../../../rh/components/RevisaoRegistro';

/** Consulta de fatos identificados, sem nomes/CPF, no recorte do ponto clicado. */
export function RegistrosOrigem({ dados, detalhe, aoAtualizar }: { dados: PainelAnaliticoDTO; detalhe: DetalheDesempenho; aoAtualizar?: () => void }) {
  const [revisaoId,setRevisaoId] = useState<number | null>(null);
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState<{ chave: string; geracao: number; pagina: PaginaRegistrosAnalitico } | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const params = {
    referencia: dados.referencia, inicio: detalhe.faixa.inicio, fim: detalhe.faixa.fim,
    prestadorId: dados.selecao.prestadorId ?? undefined, contratoId: dados.selecao.contratoId ?? undefined, tipoUnidade: dados.selecao.tipoUnidade,
    unidadeId: detalhe.unidadeId ?? dados.selecao.unidadeId ?? undefined,
    metrica: detalhe.metrica ?? 'registros', pagina,
  };
  const chave = JSON.stringify(params);
  useEffect(() => {
    const controle = new AbortController();
    const geracao = obterGeracaoSessao();
    setErro(null);
    if (params.unidadeId === 0) { setErro('Fotografia de unidade ausente: o detalhamento não pode ser recortado com segurança.'); return; }
    ServicoPainel.buscarRegistrosAnalitico(params, { signal: controle.signal, geracaoSessao: geracao })
      .then(resposta => { if (!controle.signal.aborted && sessaoAtual(geracao)) setResultado({ chave, geracao, pagina: resposta }); })
      .catch((falha: unknown) => { if (!controle.signal.aborted && sessaoAtual(geracao)) setErro(falha instanceof Error ? falha.message : 'Não foi possível consultar os registros de origem.'); });
    return () => controle.abort();
  }, [chave]);
  const registros = resultado?.chave === chave && sessaoAtual(resultado.geracao) ? resultado.pagina : null;
  return <section aria-label="Registros de origem"><h3>Registros de origem</h3>
    {erro ? <p role="alert">{erro}</p> : registros ? <>
      <div className={estilos.rolagemTabela}><table><thead><tr><th>Registro</th><th>Data</th><th>Situação</th><th>Versão</th><th>Conferência</th></tr></thead><tbody>{registros.registros.map(r => <tr key={r.id}><td>#{r.id}</td><td>{dataCivil(r.data)}</td><td>{r.status}</td><td>{r.versao}</td><td><button type="button" onClick={() => setRevisaoId(r.id)}>Consultar revisão #{r.id}</button></td></tr>)}</tbody></table></div>
      {!registros.total && <p>Sem registros de origem neste intervalo.</p>}
      <nav className={estilos.paginacao} aria-label="Páginas dos registros de origem"><button type="button" disabled={!pagina} onClick={() => setPagina(v => v - 1)}>Registros anteriores</button><span>{registros.total} registro(s) · página {pagina + 1}</span><button type="button" disabled={(pagina + 1) * registros.tamanho >= registros.total} onClick={() => setPagina(v => v + 1)}>Próximos registros</button></nav>
    </> : <p role="status">Consultando registros de origem…</p>}
    {revisaoId !== null && <RevisaoRegistro key={revisaoId} id={revisaoId} aoAtualizar={aoAtualizar} />}
  </section>;
}
