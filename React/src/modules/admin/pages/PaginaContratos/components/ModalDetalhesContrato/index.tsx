import { ConferenciaDocumentalRegistrada } from '../ConferenciaDocumentalAto/index.tsx';
import React, { useEffect, useMemo, useState } from "react";
import { CalendarClock, Clock3, Eye, FileClock, GitBranch, History, Pencil, Search, ShieldCheck, Trash2, X } from "lucide-react";
import { useDialogoAcessivel } from "../../../../../../core/ui/acessibilidadeDialogo.ts";
import {
  formatarDataBrasileira,
  formatarDataHoraBrasileira,
  obterAcoesCicloVida,
  rotularEstadoNegocial,
  rotularHabilitacao,
  rotularSituacaoTemporal,
} from "../../../../../../services/admin/ServicoDominioContrato.ts";
import { PainelAlteracoesContrato } from "../PainelAlteracoesContrato";
import { PainelExcecoesContrato } from "../PainelExcecoesContrato";
import { possuiMetadadosProva } from '../../../../../../services/historico/provaHistoricaLeitura.ts';
import { CampoComProva } from '../../../../../historico/ResultadosHistoricos.tsx';
import { obterDataNoFusoNegocio } from '../../../../../../core/formatters/dataHoraNegocio.ts';
import estilos from "./index.module.css";

const hoje = () => obterDataNoFusoNegocio() || '';
const formatarValor = (valor: unknown) => valor == null || valor === "" ? "—" : String(valor);

const RotuloEixo = ({ rotulo, valor, classe = "" }: { rotulo: (string); valor: (string) | ("Inativo" | "Ativo"); classe?: string }) => <div className={`${estilos.eixo} ${classe}`}><span>{rotulo}</span><strong>{valor}</strong></div>;

const ListaHistoricaPaginada = ({ children, titulo, elemento: Elemento = 'ul', className }: React.PropsWithChildren<{ titulo: string; elemento?: "ul" | "ol" | "div"; className?: string }>) => {
  const registros = React.Children.toArray(children);
  const [pagina, setPagina] = useState(0);
  const paginas = Math.max(1, Math.ceil(registros.length / 5));
  const atual = Math.min(pagina, paginas - 1);
  useEffect(() => { setPagina((anterior) => Math.min(anterior, paginas - 1)); }, [paginas]);
  return <>
    <Elemento className={className}>{registros.slice(atual * 5, atual * 5 + 5)}</Elemento>
    {paginas > 1 && <nav className={estilos.paginacao} aria-label={titulo}>
      <button type="button" disabled={atual === 0} onClick={() => setPagina(atual - 1)}>Anterior</button>
      <span>Página {atual + 1} de {paginas} · {registros.length} registros · 5 por página</span>
      <button type="button" disabled={atual + 1 === paginas} onClick={() => setPagina(atual + 1)}>Próxima</button>
    </nav>}
  </>;
};

export const ModalDetalhesContrato = ({
  aberto,
  contrato,
  eventos = [],
  precedencias = [],
  versoes = [],
  carregando,
  erroCarregamento,
  podeGerenciar,
  posicaoTemporal,
  consultandoTemporal,
  aoConsultarTemporal,
  aoEditar,
  aoAcao,
  aoFechar,
  aoAtualizarContrato,
}: { aberto: (boolean); contrato: (import("../../../../../../contracts/opcoes-apresentacao").ContractView | null); eventos?: (import("../../../../../../contracts/java").EventoCicloVidaContratoDTO[]); precedencias?: (import("../../../../../../contracts/java").RelacaoPrecedenciaEventoContratoDTO[]); versoes?: (import("../../../../../../contracts/java").VersaoContratoLeituraDTO[]); carregando: (boolean); erroCarregamento: ("Não foi possível carregar o ciclo de vida do contrato. Feche e reabra o detalhe para tentar novamente." | null); podeGerenciar: (boolean); posicaoTemporal: (import("../../../../../../contracts/java").PosicaoTemporalContratoLeituraDTO | null); consultandoTemporal: (boolean); aoConsultarTemporal: (parametros: { data: string; conhecidoAte?: string }) => void; aoEditar: (contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView) => void; aoAcao: (acao: string, contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView) => void; aoFechar: (() => void); aoAtualizarContrato: (() => void | Promise<void>) }) => {
  const referenciaModal = useDialogoAcessivel({ aberto, aoFechar, bloquearFechamento: false });
  const [dataReferencia, setDataReferencia] = useState(hoje());
  const [conhecidoAte, setConhecidoAte] = useState("");
  const acoes = useMemo(() => obterAcoesCicloVida(contrato, podeGerenciar), [contrato, podeGerenciar]);

  useEffect(() => {
    if (aberto) { setDataReferencia(hoje()); setConhecidoAte(""); }
  }, [aberto, contrato?.id]);
  if (!aberto) return null;

  const consultar = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const instante = conhecidoAte ? new Date(conhecidoAte).toISOString() : undefined;
    aoConsultarTemporal({ data: dataReferencia, conhecidoAte: instante });
  };

  return (
    <div className={estilos.sobreposicao} onMouseDown={(e) => e.target === e.currentTarget && aoFechar()} role="presentation">
      <section ref={referenciaModal} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby="titulo-detalhe-contrato" className={estilos.modal}>
        <header className={estilos.cabecalho}>
          <div><span>Contrato</span><h2 id="titulo-detalhe-contrato">{contrato?.numeroContrato || "Sem número"}{contrato?.anoContrato ? `/${contrato.anoContrato}` : ""}</h2><small>{contrato?.descricao || "Sem descrição"}</small></div>
          <button type="button" onClick={aoFechar} aria-label="Fechar"><X size={20}/></button>
        </header>

        {carregando || !contrato ? <div className={estilos.carregando}>Carregando ciclo de vida...</div>
          : erroCarregamento ? <div className={estilos.secao} role="alert">{erroCarregamento}</div> : <>
          <div className={estilos.eixos}>
            <RotuloEixo rotulo="Estado negocial" valor={rotularEstadoNegocial(contrato.estadoNegocial)} />
            <RotuloEixo rotulo="Situação temporal" valor={rotularSituacaoTemporal(contrato.situacaoTemporal)} />
            <RotuloEixo rotulo="Habilitação" valor={rotularHabilitacao(contrato.habilitacaoAdministrativa)} />
          </div>

          <section className={estilos.secao}>
            <h3><Eye size={17}/>Resumo canônico</h3>
            <dl className={estilos.gradeDetalhes}>
              <div><dt>Gestão Local</dt><dd>{formatarValor(contrato.gestaoLocalNome)}</dd></div>
              <div><dt>UF</dt><dd>{formatarValor(contrato.uf)}</dd></div>
              <div><dt>Tipo de contratação</dt><dd>{formatarValor(contrato.tipoContratacaoDescricao)}</dd></div>
              <div><dt>Efetivo previsto</dt><dd>{formatarValor(contrato.efetivoPrevisto)}</dd></div>
              <div><dt>Vigência</dt><dd>{formatarDataBrasileira(contrato.dataInicio)} a {formatarDataBrasileira(contrato.dataFim)}</dd></div>
              <div><dt>Publicado em</dt><dd>{formatarDataHoraBrasileira(contrato.publicadoEm)}</dd></div>
              <div><dt>CNPJ</dt><dd>{formatarValor(contrato.cnpjEmpresa)}</dd></div>
              <div><dt>Razão social</dt><dd>{formatarValor(contrato.razaoSocialEmpresa)}</dd></div>
              <div><dt>Nome fantasia</dt><dd>{formatarValor(contrato.nomeFantasiaEmpresa)}</dd></div>
              <div><dt>SEI da publicação</dt><dd>{formatarValor(contrato.referenciaSeiPublicacao)}</dd></div>
            </dl>
          </section>

          <PainelAlteracoesContrato contrato={contrato} podeGerenciar={podeGerenciar} />
          <PainelExcecoesContrato contrato={contrato} eventos={eventos} podeGerenciar={podeGerenciar} aoAtualizarContrato={aoAtualizarContrato} />

          <section className={estilos.secao}>
            <h3><CalendarClock size={17}/>Posição temporal / bitemporal</h3>
            <form className={estilos.formTemporal} onSubmit={consultar}>
              <label><span>Data efetiva *</span><input type="date" value={dataReferencia} onChange={(e)=>setDataReferencia(e.target.value)} required /></label>
              <label><span>Conhecido até (opcional)</span><input type="datetime-local" value={conhecidoAte} onChange={(e)=>setConhecidoAte(e.target.value)} /></label>
              <button type="submit" disabled={consultandoTemporal}><Search size={16}/>{consultandoTemporal ? "Consultando..." : "Reconstruir posição"}</button>
            </form>
            {posicaoTemporal && <div className={estilos.resultadoTemporal}>
              <strong>Verdade reconstruída em {formatarDataBrasileira(posicaoTemporal.dataReferencia)}</strong>
              <dl className={estilos.gradeDetalhes}>
                <div><dt>Estado negocial</dt><dd><CampoComProva dados={posicaoTemporal} campo="estadoNegocial" formatarValor={rotularEstadoNegocial} /></dd></div>
                <div><dt>Situação temporal</dt><dd><CampoComProva dados={posicaoTemporal} campo="situacaoTemporal" formatarValor={rotularSituacaoTemporal} /></dd></div>
                <div><dt>Habilitação</dt><dd><CampoComProva dados={posicaoTemporal} campo="habilitacaoAdministrativa" formatarValor={rotularHabilitacao} /></dd></div>
              </dl>
              <small>Conhecido até: <CampoComProva dados={posicaoTemporal} campo="conhecidoAte" formatarValor={valor => formatarDataHoraBrasileira(valor, "verdade atualmente conhecida")} /></small>
            </div>}
          </section>

          <section className={estilos.secao}>
            <h3><History size={17}/>Timeline auditável <span className={estilos.contador}>{eventos.length}</span></h3>
            {eventos.length === 0 ? <p className={estilos.vazio}>Nenhum evento negocial registrado.</p> : <ListaHistoricaPaginada key={`${contrato.id}-eventos`} elemento="ol" titulo="Páginas da timeline contratual" className={estilos.timeline}>{[...eventos].sort((a,b)=>String(b.registradoEm||"").localeCompare(String(a.registradoEm||""))).map((evento)=><li key={evento.id}><span className={estilos.marcador}/><div><strong>{String(evento.tipoEvento || "EVENTO").replaceAll("_"," ")}</strong><small>Efetiva: {formatarDataBrasileira(evento.dataEfetiva)} · Ato: {formatarDataBrasileira(evento.dataAto)} · Registro: {formatarDataHoraBrasileira(evento.registradoEm)}</small><p>{evento.motivo || "Sem motivo textual"}</p>{evento.referenciaSei && <em>SEI: {evento.referenciaSei}</em>}<ConferenciaDocumentalRegistrada conferencia={evento.conferenciaDocumental} /></div></li>)}</ListaHistoricaPaginada>}
          </section>

          <div className={estilos.gradeHistorico}>
            <section className={estilos.secao}><h3><FileClock size={17}/>Versões negociais <span className={estilos.contador}>{versoes.length}</span></h3>{versoes.length===0?<p className={estilos.vazio}>Nenhuma versão registrada.</p>:<ListaHistoricaPaginada key={`${contrato.id}-versoes`} titulo="Páginas das versões negociais" className={estilos.listaCompacta}>{versoes.map(v=><li key={v.id}><strong>Versão {v.numeroVersao}</strong>{possuiMetadadosProva(v) ? <dl className={estilos.gradeDetalhes}>
              <div><dt>Número</dt><dd><CampoComProva dados={v} campo="numeroContrato" compacto /></dd></div>
              <div><dt>Ano</dt><dd><CampoComProva dados={v} campo="anoContrato" compacto /></dd></div>
              <div><dt>Data efetiva</dt><dd><CampoComProva dados={v} campo="dataEfetiva" compacto /></dd></div>
            </dl> : <span>{v.numeroContrato}/{v.anoContrato} · {formatarDataBrasileira(v.dataEfetiva)}</span>}</li>)}</ListaHistoricaPaginada>}</section>
            <section className={estilos.secao}><h3><GitBranch size={17}/>Precedências <span className={estilos.contador}>{precedencias.length}</span></h3>{precedencias.length===0?<p className={estilos.vazio}>Nenhuma relação de precedência.</p>:<ListaHistoricaPaginada key={`${contrato.id}-precedencias`} titulo="Páginas das precedências contratuais" className={estilos.listaCompacta}>{precedencias.map(p=><li key={p.id}><strong>{p.tipoRelacao}</strong><span>Evento {p.eventoFonteId} → {p.eventoAlvoId}</span></li>)}</ListaHistoricaPaginada>}</section>
          </div>

          {podeGerenciar && <section className={estilos.secao}><h3><ShieldCheck size={17}/>Ações permitidas no estado atual</h3><div className={estilos.acoes}>{acoes.map((acao)=>{
            if(acao==="EDITAR") return <button key={acao} type="button" onClick={()=>aoEditar(contrato)}><Pencil size={16}/>Editar rascunho</button>;
            if(acao==="DESCARTAR") return <button key={acao} type="button" className={estilos.perigosa} onClick={()=>aoAcao("DESCARTAR",contrato)}><Trash2 size={16}/>Descartar rascunho</button>;
            const rotulo={PUBLICAR:"Publicar",INATIVAR:"Inativar",REATIVAR:"Reativar",ENCERRAR_ANTECIPADAMENTE:"Encerrar antecipadamente",CANCELAR_FUTURO:"Cancelar futuro",ANULAR:"Anular"}[acao]||acao;
            return <button key={acao} type="button" onClick={()=>aoAcao(acao,contrato)}><Clock3 size={16}/>{rotulo}</button>;
          })}</div></section>}
        </>}
      </section>
    </div>
  );
};
