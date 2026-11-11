import type { PersonDetailsView, LinkCommand, AllocationSubmission } from '../../../../../contracts/pessoas-vinculos';
import type * as DTO from '../../../../../contracts/java';
import type { MutableForm } from '../../../../../contracts/formularios';
import React, { useEffect, useState } from "react";
import { Eye, EyeOff, History, Link2, ShieldAlert, UserRound } from "lucide-react";
import {
  formatarCpfCompleto,
  formatarCpfProtegido,
  formatarDataPessoaVinculo,
  formatarInstantePessoaVinculo,
  prepararAtualizacaoPessoa,
  prepararCorrecaoIdentidade,
  rotularSituacaoVinculo,
} from "../../../../../services/rh/ServicoDominioPessoaVinculo.ts";
import { notificar } from "../../../../../core/events.ts";
import { ModalEstrutural } from "./ModalEstrutural.tsx";
import estilos from "../index.module.css";

const opcionaisIniciais = (pessoa: Partial<DTO.PessoaDTO> = {}) => ({
  rg: pessoa.rg || "", limparRg: false,
  ctpsSerie: pessoa.ctpsSerie || "", limparCtpsSerie: false,
  nitPisPasep: pessoa.nitPisPasep || "", limparNitPisPasep: false,
  dataNascimento: pessoa.dataNascimento || "", limparDataNascimento: false,
  email: pessoa.email || "", limparEmail: false,
  endereco: {
    logradouro: pessoa.endereco?.logradouro || "", numero: pessoa.endereco?.numero || "",
    bairro: pessoa.endereco?.bairro || "", cidade: pessoa.endereco?.cidade || "",
    uf: pessoa.endereco?.uf || "", cep: pessoa.endereco?.cep || "",
  },
  limparEndereco: false,
});

const CORRECAO_INICIAL = Object.freeze({ tipo: "NOME", novoValor: "", motivo: "", evidencia: "", autorizacao: "", impactosEncontrados: "" });

const LinhaDado = ({ rotulo, valor, restrito = false }: { rotulo: string; valor: React.ReactNode; restrito?: boolean }) => (
  <div className={estilos.linhaDado}><dt>{rotulo}{restrito && <span className={estilos.marcaRestrito}>restrito</span>}</dt><dd>{valor || "—"}</dd></div>
);

export const ModalPessoa = ({
  aberto, pessoa, vinculos = [], correcoes = [], carregando, processando, podeGerenciar,
  aoFechar, aoAbrirVinculo, aoNovoVinculo, aoSolicitarRestritos, aoAtualizar, aoCorrigir,
}: { aberto: boolean; pessoa: PersonDetailsView | null; vinculos?: DTO.VinculoContratualDTO[]; correcoes?: DTO.CorrecaoPessoaDTO[]; carregando?: boolean; processando?: boolean; podeGerenciar?: boolean; aoFechar: () => void; aoAbrirVinculo: (vinculo: DTO.VinculoContratualDTO) => void; aoNovoVinculo: (pessoa: PersonDetailsView) => void; aoSolicitarRestritos: (finalidade: string) => Promise<DTO.PessoaDTO>; aoAtualizar: (payload: import("../../../../../contracts/java-requests").RequestAtualizacaoPessoaOpcionalRequisicao) => Promise<boolean | void>; aoCorrigir: (payload: ReturnType<typeof prepararCorrecaoIdentidade>) => Promise<boolean | void> }) => {
  const [finalidade, setFinalidade] = useState("");
  const [restritos, setRestritos] = useState<DTO.PessoaDTO | null>(null);
  const [buscandoRestritos, setBuscandoRestritos] = useState(false);
  const [edicao, setEdicao] = useState(false);
  const [correcaoAberta, setCorrecaoAberta] = useState(false);
  const [opcionais, setOpcionais] = useState(opcionaisIniciais());
  const [correcao, setCorrecao] = useState<Omit<MutableForm<typeof CORRECAO_INICIAL>, "tipo"> & { tipo: DTO.TipoCorrecaoPessoa }>({ ...CORRECAO_INICIAL });

  useEffect(() => {
    if (!aberto) return;
    setFinalidade("");
    setRestritos(null);
    setEdicao(false);
    setCorrecaoAberta(false);
    setOpcionais(opcionaisIniciais(pessoa ?? undefined));
    setCorrecao({ ...CORRECAO_INICIAL });
  }, [aberto, pessoa]);

  const consultarRestritos = async () => {
    if (!finalidade.trim()) { notificar("warning", "Informe a finalidade antes de revelar dados restritos."); return; }
    setBuscandoRestritos(true);
    try { setRestritos(await aoSolicitarRestritos(finalidade.trim())); }
    finally { setBuscandoRestritos(false); }
  };

  const salvarOpcionais = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const payload = prepararAtualizacaoPessoa(opcionais);
    const endereco = payload.endereco;
    let enderecoComando: import("../../../../../contracts/java-requests").RequestEnderecoDTO | null = null;
    if (endereco) {
      const logradouro = endereco.logradouro;
      if (logradouro == null) {
        notificar("warning", "Ao informar endereço, o logradouro é obrigatório.");
        return;
      }
      enderecoComando = { ...endereco, logradouro };
    }
    await aoAtualizar({ ...payload, endereco: enderecoComando });
    setEdicao(false);
  };

  const salvarCorrecao = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (!correcao.novoValor.trim() || !correcao.motivo.trim() || !correcao.evidencia.trim() || !correcao.autorizacao.trim() || !correcao.impactosEncontrados.trim()) {
      notificar("warning", "A correção excepcional exige novo valor, motivo, evidência, autorização e impactos.");
      return;
    }
    await aoCorrigir(prepararCorrecaoIdentidade(correcao));
    setCorrecaoAberta(false);
  };

  const alterarOpcional = (campo: (string), valor: (string) | (boolean)) => setOpcionais((atual) => ({ ...atual, [campo]: valor }));
  const alterarEndereco = (campo: (string), valor: (string)) => setOpcionais((atual) => ({ ...atual, endereco: { ...atual.endereco, [campo]: valor } }));

  return (
    <ModalEstrutural aberto={aberto} titulo={pessoa?.nome || "Detalhes da pessoa"} subtitulo="Identidade canônica e vínculos contratuais independentes" aoFechar={aoFechar} largura="extra">
      {carregando || !pessoa ? <p className={estilos.estadoVazio}>Carregando a pessoa e sua linha do tempo…</p> : (
        <div className={estilos.colunasDetalhe}>
          <div className={estilos.colunaPrincipal}>
            <section className={estilos.secaoDetalhe}>
              <div className={estilos.tituloSecao}><UserRound size={18} /><h3>Identidade protegida</h3></div>
              <dl className={estilos.gradeDados}>
                <LinhaDado rotulo="Pessoa ID" valor={pessoa.id} />
                <LinhaDado rotulo="Nome atual" valor={pessoa.nome} />
                <LinhaDado rotulo="CPF" valor={restritos ? formatarCpfCompleto(restritos.cpf) : formatarCpfProtegido(pessoa.cpf)} restrito />
                <LinhaDado rotulo="Atualizada em" valor={formatarInstantePessoaVinculo(pessoa.atualizadoEm)} />
              </dl>
              <div className={estilos.caixaPrivacidade}>
                <ShieldAlert size={20} />
                <div><strong>Revelação auditada</strong><p>O CPF completo e os dados documentais só são consultados com uma finalidade registrada no servidor.</p></div>
              </div>
              <div className={estilos.linhaAcaoRestrita}>
                <input value={finalidade} onChange={(e) => setFinalidade(e.target.value)} placeholder="Finalidade da consulta" maxLength={240} autoComplete="off" />
                {!restritos ? <button type="button" className={estilos.botaoSecundario} onClick={consultarRestritos} disabled={buscandoRestritos}><Eye size={16} /> {buscandoRestritos ? "Consultando…" : "Revelar"}</button>
                  : <button type="button" className={estilos.botaoSecundario} onClick={() => { setRestritos(null); setFinalidade(""); }}><EyeOff size={16} /> Ocultar</button>}
              </div>
              {restritos && (
                <dl className={estilos.gradeDados}>
                  <LinhaDado rotulo="RG" valor={restritos.rg} restrito />
                  <LinhaDado rotulo="CTPS / série" valor={restritos.ctpsSerie} restrito />
                  <LinhaDado rotulo="NIT / PIS / PASEP" valor={restritos.nitPisPasep} restrito />
                  <LinhaDado rotulo="Nascimento" valor={formatarDataPessoaVinculo(restritos.dataNascimento)} restrito />
                  <LinhaDado rotulo="E-mail" valor={restritos.email} restrito />
                  <LinhaDado rotulo="Endereço" valor={restritos.endereco ? [restritos.endereco.logradouro, restritos.endereco.numero, restritos.endereco.cidade, restritos.endereco.uf].filter(Boolean).join(", ") : "—"} restrito />
                </dl>
              )}
            </section>

            <section className={estilos.secaoDetalhe}>
              <div className={estilos.tituloSecao}><Link2 size={18} /><h3>Vínculos contratuais</h3><span>{vinculos.length}</span></div>
              {vinculos.length === 0 ? <p className={estilos.estadoVazio}>Nenhum vínculo visível no seu escopo.</p> : (
                <div className={estilos.listaCompacta}>{vinculos.map((vinculo) => (
                  <button type="button" key={vinculo.id} className={estilos.itemCompacto} onClick={() => aoAbrirVinculo(vinculo)}>
                    <span><strong>{vinculo.contratoNumero || `Contrato ${vinculo.contratoId}`}{vinculo.contratoAno ? `/${vinculo.contratoAno}` : ""}</strong><small>{vinculo.gestaoLocalNome || "Gestão Local"} · {formatarDataPessoaVinculo(vinculo.dataInicio)} a {formatarDataPessoaVinculo(vinculo.dataFim, "em aberto")}</small></span>
                    <em>{rotularSituacaoVinculo(vinculo.situacaoTemporal)}</em>
                  </button>
                ))}</div>
              )}
              {podeGerenciar && <button type="button" className={estilos.botaoPrimario} onClick={() => aoNovoVinculo(pessoa)}>Novo vínculo para esta pessoa</button>}
            </section>

            <section className={estilos.secaoDetalhe}>
              <div className={estilos.tituloSecao}><History size={18} /><h3>Correções de identidade</h3><span>{correcoes.length}</span></div>
              {correcoes.length === 0 ? <p className={estilos.estadoVazio}>Nenhuma correção excepcional registrada.</p> : correcoes.map((item) => (
                <article key={item.id} className={estilos.evento}><strong>{item.tipo}</strong><span>{formatarInstantePessoaVinculo(item.registradoEm || item.registradoEm)}</span><p>{item.motivo}</p></article>
              ))}
            </section>
          </div>

          {podeGerenciar && (
            <aside className={estilos.colunaAcoes}>
              <button type="button" className={estilos.botaoSecundario} onClick={() => setEdicao((v) => !v)}>Dados opcionais</button>
              <button type="button" className={estilos.botaoPerigoContorno} onClick={() => setCorrecaoAberta((v) => !v)}>Correção excepcional</button>
              {edicao && (
                <form className={estilos.formularioLateral} onSubmit={salvarOpcionais}>
                  <h3>Atualizar dados opcionais</h3>
                  {([['rg', 'RG', 'limparRg'], ['ctpsSerie', 'CTPS / série', 'limparCtpsSerie'], ['nitPisPasep', 'NIT / PIS / PASEP', 'limparNitPisPasep'], ['dataNascimento', 'Nascimento', 'limparDataNascimento'], ['email', 'E-mail', 'limparEmail']] as const).map(([campo, rotulo, campoLimpeza]) => (
                    <label key={campo}><span>{rotulo}</span><input type={campo === "dataNascimento" ? "date" : campo === "email" ? "email" : "text"} value={opcionais[campo]} onChange={(e) => alterarOpcional(campo, e.target.value)} /><small><input type="checkbox" checked={opcionais[campoLimpeza]} onChange={(e) => alterarOpcional(campoLimpeza, e.target.checked)} /> Limpar explicitamente</small></label>
                  ))}
                  <details><summary>Endereço</summary>{Object.entries(opcionais.endereco).map(([campo, valor]) => <label key={campo}><span>{campo}</span><input value={valor} onChange={(e) => alterarEndereco(campo, e.target.value)} /></label>)}<label className={estilos.confirmacao}><input type="checkbox" checked={opcionais.limparEndereco} onChange={(e) => alterarOpcional("limparEndereco", e.target.checked)} /> Limpar endereço</label></details>
                  <button type="submit" className={estilos.botaoPrimario} disabled={processando}>{processando ? "Salvando…" : "Salvar opcionais"}</button>
                </form>
              )}
              {correcaoAberta && (
                <form className={estilos.formularioLateral} onSubmit={salvarCorrecao}>
                  <h3>Correção excepcional</h3>
                  <label><span>Campo canônico</span><select value={correcao.tipo} onChange={(e) => setCorrecao((a) => ({ ...a, tipo: e.target.value === "CPF" ? "CPF" : "NOME" }))}><option value="NOME">Nome</option><option value="CPF">CPF</option></select></label>
                  <label><span>Novo valor *</span><input value={correcao.novoValor} onChange={(e) => setCorrecao((a) => ({ ...a, novoValor: e.target.value }))} autoComplete="off" /></label>
                  {([['motivo', 'Motivo'], ['evidencia', 'Evidência'], ['autorizacao', 'Autorização'], ['impactosEncontrados', 'Impactos encontrados']] as const).map(([campo, rotulo]) => <label key={campo}><span>{rotulo} *</span><textarea value={correcao[campo]} onChange={(e) => setCorrecao((a) => ({ ...a, [campo]: e.target.value }))} /></label>)}
                  <button type="submit" className={estilos.botaoPerigo} disabled={processando}>{processando ? "Registrando…" : "Registrar correção"}</button>
                </form>
              )}
            </aside>
          )}
        </div>
      )}
    </ModalEstrutural>
  );
};
