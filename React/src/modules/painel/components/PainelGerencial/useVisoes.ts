import type { PainelVisoesDTO } from '../../../../contracts/painel-visoes';
import { useEffect, useState } from 'react';
import { ServicoPainel } from '../../../../services/painel/ServicoPainel';
import { obterGeracaoSessao, obterSinalSessao, sessaoAtual } from '../../../../core/security/geracaoSessao';
import type { PainelAnaliticoDTO, ParametrosAnalitico } from '../../../../contracts/painel-analitico';

export const hojeCivil = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Sao_Paulo' }).format(new Date());

export function useVisoes(referenciaInicial?: string) {
  const [filtros, setFiltros] = useState<ParametrosAnalitico>({ granularidade: 'DIARIO', referencia: referenciaInicial || hojeCivil(), comparar: true, paginaUnidades: 0 });
  const [revisao, setRevisao] = useState(0);
  const [resultado, setResultado] = useState<{ chave: string; geracao: number; dados: PainelVisoesDTO } | null>(null);
  const [catalogo, setCatalogo] = useState<{ geracao: number; filtros: PainelAnaliticoDTO['filtros'] } | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(true);
  const chave = JSON.stringify(filtros) + revisao;
  useEffect(() => {
    const controle = new AbortController();
    const geracao = obterGeracaoSessao();
    const sinalSessao = obterSinalSessao(geracao);
    const limparSessao = () => { controle.abort(); setResultado(null); setCatalogo(null); setCarregando(false); };
    if (sinalSessao.aborted) { limparSessao(); return; }
    sinalSessao.addEventListener('abort', limparSessao, { once: true });
    setErro(null); setCarregando(true); setResultado(null);
    ServicoPainel.buscarVisoes(filtros, { signal: controle.signal, geracaoSessao: geracao }).then(dados => {
      if (controle.signal.aborted || !sessaoAtual(geracao)) return;
      setResultado({ chave, geracao, dados }); setCatalogo({ geracao, filtros: dados.analitico.filtros });
    }).catch((falha: unknown) => {
      if (controle.signal.aborted || !sessaoAtual(geracao)) return;
      setErro(falha instanceof Error ? falha.message : 'Não foi possível consultar os indicadores. Tente novamente.');
    }).finally(() => { if (!controle.signal.aborted && sessaoAtual(geracao)) setCarregando(false); });
    return () => { controle.abort(); sinalSessao.removeEventListener('abort', limparSessao); };
    // A chave contém todos os parâmetros primitivos e a revisão explícita.
  }, [chave]);
  const dados = resultado?.chave === chave && sessaoAtual(resultado.geracao) ? resultado.dados : null;
  const opcoes = catalogo && sessaoAtual(catalogo.geracao) ? catalogo.filtros : null;
  const alterar = (novos: Partial<ParametrosAnalitico>) => setFiltros(atuais => ({ ...atuais, ...novos, paginaUnidades: novos.paginaUnidades ?? 0 }));
  return { filtros, dados, opcoes, erro, carregando, alterar, recarregar: () => setRevisao(v => v + 1), limpar: () => setFiltros(atuais => ({ ...atuais, contratoId: undefined, prestadorId: undefined, unidadeId: undefined, paginaUnidades: 0 })) };
}
