import React from "react";
import {
  Building2,
  CalendarDays,
  Eye,
  FileSearch,
  MapPinned,
  Pencil,
  Trash2,
  UsersRound,
} from "lucide-react";
import {
  formatarDataBrasileira,
  podeDescartarRascunho,
  podeEditarRascunho,
  rotularEstadoNegocial,
  rotularHabilitacao,
  rotularSituacaoTemporal,
} from "../../../../../../services/admin/ServicoDominioContrato.ts";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../../../../../core/formatters/nomenclaturaGestao.ts";
import estilos from "./index.module.css";

const EfetivoContrato = ({ contrato }: { contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView }) => {
  const previsto = Number(contrato.efetivoPrevisto || 0);
  const atual = Number(contrato.efetivoAtual || 0);
  const saldo = Number.isFinite(Number(contrato.saldoEfetivo)) ? Number(contrato.saldoEfetivo) : previsto - atual;
  const percentual = previsto > 0 ? Math.min(100, Math.round((atual / previsto) * 100)) : 0;
  return (
    <div className={estilos.efetivoContrato} title={`${atual} empregado(s) ativo(s) de ${previsto} previsto(s)`}>
      <div className={estilos.linhaEfetivo}><UsersRound size={14}/><strong>{atual}</strong><span>atual</span><span className={estilos.separadorEfetivo}>/</span><strong>{previsto}</strong><span>previsto</span></div>
      <div className={estilos.barraEfetivo}><span style={{ width: `${percentual}%` }}/></div>
      <small className={saldo < 0 ? estilos.saldoExcedido : saldo > 0 ? estilos.saldoPendente : estilos.saldoCompleto}>{saldo > 0 ? `Faltam ${saldo}` : saldo < 0 ? `${Math.abs(saldo)} acima do previsto` : "Efetivo completo"}</small>
    </div>
  );
};

const EixosContrato = ({ contrato }: { contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView }) => (
  <div className={estilos.eixosContrato}>
    <span className={estilos.eixoNegocial} title="Estado negocial">{rotularEstadoNegocial(contrato.estadoNegocial)}</span>
    <span className={estilos.eixoTemporal} title="Situação temporal">{rotularSituacaoTemporal(contrato.situacaoTemporal)}</span>
    <span className={contrato.habilitacaoAdministrativa === "INATIVO" ? estilos.eixoInativo : estilos.eixoAtivo} title="Habilitação administrativa">{rotularHabilitacao(contrato.habilitacaoAdministrativa)}</span>
  </div>
);

const AcoesContrato = ({ contrato, podeGerenciar, aoDetalhar, aoEditar, aoExcluir }: { contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView; podeGerenciar: boolean; aoDetalhar: (contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView) => void; aoEditar: (contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView) => void; aoExcluir: (contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView) => void }) => (
  <div className={estilos.acoesContrato}>
    <button type="button" className={`${estilos.botaoAcao} ${estilos.botaoDetalhar}`} onClick={() => aoDetalhar(contrato)} title="Abrir ciclo de vida" aria-label={`Abrir ciclo de vida do contrato ${contrato.numeroContrato || "sem número"}`}><Eye size={16}/></button>
    {podeGerenciar && podeEditarRascunho(contrato) && <button type="button" className={`${estilos.botaoAcao} ${estilos.botaoEditar}`} onClick={() => aoEditar(contrato)} title="Editar rascunho" aria-label={`Editar rascunho ${contrato.numeroContrato || "sem número"}`}><Pencil size={16}/></button>}
    {podeGerenciar && podeDescartarRascunho(contrato) && <button type="button" className={`${estilos.botaoAcao} ${estilos.botaoExcluir}`} onClick={() => aoExcluir(contrato)} title="Descartar rascunho" aria-label={`Descartar rascunho ${contrato.numeroContrato || "sem número"}`}><Trash2 size={16}/></button>}
  </div>
);

export const ListaContratos = ({ contratos, carregando, podeGerenciar, aoDetalhar, aoEditar, aoExcluir }: { contratos: (import("../../../../../../contracts/opcoes-apresentacao").ContractView[]); carregando: (boolean); podeGerenciar: (boolean); aoDetalhar: (contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView) => void; aoEditar: (contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView) => void; aoExcluir: (contrato: import("../../../../../../contracts/opcoes-apresentacao").ContractView) => void }) => {
  if (carregando) return <section className={estilos.estadoCarregamento} aria-live="polite"><div className={estilos.esqueletoTitulo}/><div className={estilos.esqueletoLinha}/><div className={estilos.esqueletoLinha}/><div className={estilos.esqueletoLinha}/></section>;
  if (!contratos.length) return <section className={estilos.estadoVazio}><div className={estilos.iconeEstadoVazio}><FileSearch size={28}/></div><h2>Nenhum contrato encontrado</h2><p>Revise os filtros ou crie um rascunho dentro do seu escopo organizacional.</p></section>;

  return (
    <section className={estilos.conteinerLista}>
      <div className={estilos.tabelaResponsiva}>
        <table className={estilos.tabelaContratos}>
          <thead><tr><th>Contrato</th><th>Responsável</th><th>Efetivo</th><th>Vigência</th><th>Ciclo de vida</th><th className={estilos.colunaAcoes}>Ações</th></tr></thead>
          <tbody>{contratos.map((contrato) => <tr key={contrato.id}>
            <td><div className={estilos.identificacaoContrato}><strong>{contrato.numeroContrato || "Rascunho sem número"}{contrato.anoContrato ? `/${contrato.anoContrato}` : ""}</strong><span>{contrato.descricao || "Sem descrição"}</span><small>{contrato.tipoContratacaoDescricao || "Tipo de contratação não definido"}</small></div></td>
            <td><div className={estilos.responsavelContrato}><span className={estilos.linhaResponsavel}><Building2 size={14}/><strong>{contrato.gestaoLocalNome ? formatarGestaoLocal(contrato.gestaoLocalNome) : "Gestão Local não vinculada"}</strong></span><span className={estilos.linhaResponsavelSecundaria}><MapPinned size={13}/>{contrato.superintendenciaNome || contrato.uf ? formatarGestorEstadual(contrato.superintendenciaNome, contrato.uf) : "Gestor estadual não informado"}</span></div></td>
            <td><EfetivoContrato contrato={contrato}/></td>
            <td><div className={estilos.blocoVigencia}><span className={estilos.periodoVigencia}><CalendarDays size={14}/>{formatarDataBrasileira(contrato.dataInicio, "não definida")} a {formatarDataBrasileira(contrato.dataFim, "não definida")}</span><small>{contrato.razaoSocialEmpresa || contrato.nomeFantasiaEmpresa || "Empresa não definida"}</small></div></td>
            <td><EixosContrato contrato={contrato}/></td>
            <td className={estilos.colunaAcoes}><AcoesContrato contrato={contrato} podeGerenciar={podeGerenciar} aoDetalhar={aoDetalhar} aoEditar={aoEditar} aoExcluir={aoExcluir}/></td>
          </tr>)}</tbody>
        </table>
      </div>

      <div className={estilos.listaCartoesMoveis}>{contratos.map((contrato) => <article key={contrato.id} className={estilos.cartaoContratoMovel}>
        <div className={estilos.cabecalhoCartaoMovel}><div><strong>{contrato.numeroContrato || "Rascunho sem número"}{contrato.anoContrato ? `/${contrato.anoContrato}` : ""}</strong><span>{contrato.descricao || "Sem descrição"}</span></div></div>
        <EixosContrato contrato={contrato}/><EfetivoContrato contrato={contrato}/>
        <dl className={estilos.detalhesCartaoMovel}><div><dt>Gestão Local</dt><dd>{contrato.gestaoLocalNome ? formatarGestaoLocal(contrato.gestaoLocalNome) : "Não vinculada"}</dd></div><div><dt>Empresa</dt><dd>{contrato.nomeFantasiaEmpresa || contrato.razaoSocialEmpresa || "Não definida"}</dd></div><div><dt>Vigência</dt><dd>{formatarDataBrasileira(contrato.dataInicio, "não definida")} a {formatarDataBrasileira(contrato.dataFim, "não definida")}</dd></div></dl>
        <AcoesContrato contrato={contrato} podeGerenciar={podeGerenciar} aoDetalhar={aoDetalhar} aoEditar={aoEditar} aoExcluir={aoExcluir}/>
      </article>)}</div>
    </section>
  );
};
