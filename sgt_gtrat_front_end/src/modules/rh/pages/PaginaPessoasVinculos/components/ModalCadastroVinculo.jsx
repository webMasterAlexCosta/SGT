import React, { useEffect, useMemo, useState } from "react";
import { AlertTriangle, RefreshCw, ShieldCheck } from "lucide-react";
import { ServicoVinculoContratual } from "../../../../../services/rh/ServicoVinculoContratual.js";
import {
  descreverOpcaoAlocacao,
  prepararCadastroVinculo,
  validarCadastroVinculo,
} from "../../../../../services/rh/ServicoDominioPessoaVinculo.js";
import { notificar } from "../../../../../core/events.js";
import { ModalEstrutural } from "./ModalEstrutural.jsx";
import estilos from "../index.module.css";

const FORMULARIO_INICIAL = Object.freeze({
  modoPessoa: "EXISTENTE",
  pessoaId: "",
  pessoa: {
    nome: "", cpf: "", rg: "", ctpsSerie: "", nitPisPasep: "", dataNascimento: "", email: "",
    endereco: { logradouro: "", numero: "", bairro: "", cidade: "", uf: "", cep: "" },
  },
  contratoId: "",
  dataInicio: "",
  dataFim: "",
  matricula: "",
  composicaoId: "",
  confirmarInclusaoExcedente: false,
  justificativaExcedente: "",
  evidenciaExcedente: "",
  autorizacaoExcedente: "",
});

const Campo = ({ rotulo, children, obrigatorio = false, dica }) => (
  <label className={estilos.campo}>
    <span>{rotulo}{obrigatorio && <b aria-hidden="true"> *</b>}</span>
    {children}
    {dica && <small>{dica}</small>}
  </label>
);

const rotuloContrato = (contrato) => {
  const numero = contrato.numeroContrato || contrato.numero || contrato.label || `Contrato ${contrato.id}`;
  const ano = contrato.anoContrato || contrato.ano;
  return ano ? `${numero}/${ano}` : numero;
};

export const ModalCadastroVinculo = ({ aberto, contratos = [], pessoas = [], pessoaInicial = null, salvando, aoFechar, aoSalvar }) => {
  const [dados, setDados] = useState({ ...FORMULARIO_INICIAL, pessoa: { ...FORMULARIO_INICIAL.pessoa, endereco: { ...FORMULARIO_INICIAL.pessoa.endereco } } });
  const [opcoes, setOpcoes] = useState([]);
  const [carregandoOpcoes, setCarregandoOpcoes] = useState(false);

  useEffect(() => {
    if (!aberto) return;
    setDados({
      ...FORMULARIO_INICIAL,
      pessoaId: pessoaInicial?.id ? String(pessoaInicial.id) : "",
      pessoa: { ...FORMULARIO_INICIAL.pessoa, endereco: { ...FORMULARIO_INICIAL.pessoa.endereco } },
    });
    setOpcoes([]);
  }, [aberto, pessoaInicial]);

  useEffect(() => {
    if (!aberto || !dados.contratoId || !dados.dataInicio) {
      setOpcoes([]);
      return undefined;
    }
    const controlador = new AbortController();
    setCarregandoOpcoes(true);
    ServicoVinculoContratual.obterOpcoesAlocacaoInicial(dados.contratoId, dados.dataInicio, { signal: controlador.signal })
      .then(setOpcoes)
      .catch((erro) => { if (!erro?.canceled && erro?.name !== "CanceledError") notificar("error", "Não foi possível consultar a capacidade de alocação."); })
      .finally(() => { if (!controlador.signal.aborted) setCarregandoOpcoes(false); });
    return () => controlador.abort();
  }, [aberto, dados.contratoId, dados.dataInicio]);

  const opcaoSelecionada = useMemo(
    () => opcoes.find((opcao) => String(opcao.composicaoBaseId || opcao.composicaoId) === String(dados.composicaoId)) || null,
    [dados.composicaoId, opcoes],
  );

  const alterar = (campo, valor) => setDados((atual) => ({ ...atual, [campo]: valor }));
  const alterarPessoa = (campo, valor) => setDados((atual) => ({ ...atual, pessoa: { ...atual.pessoa, [campo]: valor } }));
  const alterarEndereco = (campo, valor) => setDados((atual) => ({
    ...atual,
    pessoa: { ...atual.pessoa, endereco: { ...atual.pessoa.endereco, [campo]: valor } },
  }));

  const enviar = (evento) => {
    evento.preventDefault();
    const erro = validarCadastroVinculo(dados, opcaoSelecionada);
    if (erro) { notificar("warning", erro); return; }
    aoSalvar(prepararCadastroVinculo(dados));
  };

  return (
    <ModalEstrutural
      aberto={aberto}
      titulo="Cadastrar vínculo contratual"
      subtitulo="Pessoa, vínculo e alocação inicial serão gravados em uma única transação."
      aoFechar={aoFechar}
      largura="extra"
    >
      <form onSubmit={enviar}>
        <fieldset className={estilos.blocoFormulario}>
          <legend>1. Identidade da pessoa</legend>
          <div className={estilos.segmentado}>
            <button type="button" className={dados.modoPessoa === "EXISTENTE" ? estilos.ativo : ""} onClick={() => alterar("modoPessoa", "EXISTENTE")}>Pessoa existente</button>
            <button type="button" className={dados.modoPessoa === "NOVA" ? estilos.ativo : ""} onClick={() => alterar("modoPessoa", "NOVA")}>Nova pessoa</button>
          </div>
          {dados.modoPessoa === "EXISTENTE" ? (
            <Campo rotulo="Pessoa" obrigatorio dica="Use a busca da aba Pessoas para localizar registros fora desta página.">
              <input data-foco-inicial list="pessoas-visiveis" value={dados.pessoaId} onChange={(e) => alterar("pessoaId", e.target.value)} inputMode="numeric" placeholder="ID da pessoa" />
              <datalist id="pessoas-visiveis">
                {pessoas.map((pessoa, indice) => <option key={`${pessoa.id || "pessoa"}-${indice}`} value={pessoa.id}>{pessoa.nome}</option>)}
              </datalist>
            </Campo>
          ) : (
            <>
              <div className={estilos.gradeFormulario}>
                <Campo rotulo="Nome" obrigatorio><input data-foco-inicial value={dados.pessoa.nome} onChange={(e) => alterarPessoa("nome", e.target.value)} maxLength={160} /></Campo>
                <Campo rotulo="CPF" obrigatorio><input value={dados.pessoa.cpf} onChange={(e) => alterarPessoa("cpf", e.target.value)} inputMode="numeric" maxLength={14} autoComplete="off" /></Campo>
                <Campo rotulo="RG"><input value={dados.pessoa.rg} onChange={(e) => alterarPessoa("rg", e.target.value)} maxLength={40} /></Campo>
                <Campo rotulo="CTPS / série"><input value={dados.pessoa.ctpsSerie} onChange={(e) => alterarPessoa("ctpsSerie", e.target.value)} maxLength={60} /></Campo>
                <Campo rotulo="NIT / PIS / PASEP"><input value={dados.pessoa.nitPisPasep} onChange={(e) => alterarPessoa("nitPisPasep", e.target.value)} maxLength={40} /></Campo>
                <Campo rotulo="Data de nascimento"><input type="date" value={dados.pessoa.dataNascimento} onChange={(e) => alterarPessoa("dataNascimento", e.target.value)} /></Campo>
                <Campo rotulo="E-mail"><input type="email" value={dados.pessoa.email} onChange={(e) => alterarPessoa("email", e.target.value)} maxLength={180} /></Campo>
              </div>
              <details className={estilos.detalhesFormulario}>
                <summary>Endereço opcional</summary>
                <div className={estilos.gradeFormulario}>
                  <Campo rotulo="Logradouro"><input value={dados.pessoa.endereco.logradouro} onChange={(e) => alterarEndereco("logradouro", e.target.value)} /></Campo>
                  <Campo rotulo="Número"><input value={dados.pessoa.endereco.numero} onChange={(e) => alterarEndereco("numero", e.target.value)} /></Campo>
                  <Campo rotulo="Bairro"><input value={dados.pessoa.endereco.bairro} onChange={(e) => alterarEndereco("bairro", e.target.value)} /></Campo>
                  <Campo rotulo="Cidade"><input value={dados.pessoa.endereco.cidade} onChange={(e) => alterarEndereco("cidade", e.target.value)} /></Campo>
                  <Campo rotulo="UF"><input value={dados.pessoa.endereco.uf} onChange={(e) => alterarEndereco("uf", e.target.value.toUpperCase())} maxLength={2} /></Campo>
                  <Campo rotulo="CEP"><input value={dados.pessoa.endereco.cep} onChange={(e) => alterarEndereco("cep", e.target.value)} inputMode="numeric" /></Campo>
                </div>
              </details>
            </>
          )}
        </fieldset>

        <fieldset className={estilos.blocoFormulario}>
          <legend>2. Vínculo e vigência efetiva</legend>
          <div className={estilos.gradeFormulario}>
            <Campo rotulo="Contrato" obrigatorio>
              <select value={dados.contratoId} onChange={(e) => { alterar("contratoId", e.target.value); alterar("composicaoId", ""); }}>
                <option value="">Selecione</option>
                {contratos.map((contrato, indice) => <option key={`${contrato.id || "contrato"}-${indice}`} value={contrato.id}>{rotuloContrato(contrato)}</option>)}
              </select>
            </Campo>
            <Campo rotulo="Início efetivo" obrigatorio><input type="date" value={dados.dataInicio} onChange={(e) => { alterar("dataInicio", e.target.value); alterar("composicaoId", ""); }} /></Campo>
            <Campo rotulo="Fim previsto"><input type="date" value={dados.dataFim} min={dados.dataInicio || undefined} onChange={(e) => alterar("dataFim", e.target.value)} /></Campo>
            <Campo rotulo="Matrícula do vínculo" dica="A obrigatoriedade é validada pelo tipo de contratação no servidor."><input value={dados.matricula} onChange={(e) => alterar("matricula", e.target.value)} maxLength={80} /></Campo>
          </div>
        </fieldset>

        <fieldset className={estilos.blocoFormulario}>
          <legend>3. Alocação inicial e capacidade</legend>
          {carregandoOpcoes ? <p className={estilos.estadoInline}><RefreshCw size={16} className={estilos.girando} /> Consultando a posição temporal no servidor…</p> : (
            <Campo rotulo="Composição vigente" obrigatorio>
              <select value={dados.composicaoId} onChange={(e) => alterar("composicaoId", e.target.value)} disabled={!dados.contratoId || !dados.dataInicio}>
                <option value="">{opcoes.length ? "Selecione" : "Informe contrato e data"}</option>
                {opcoes.map((opcao, indice) => (
                  <option key={`${opcao.composicaoBaseId || opcao.composicaoId || "opcao"}-${indice}`} value={opcao.composicaoBaseId || opcao.composicaoId} disabled={opcao.disponivelParaNovaAlocacao === false}>
                    {descreverOpcaoAlocacao(opcao)}{opcao.disponivelParaNovaAlocacao === false ? " · indisponível" : ""}
                  </option>
                ))}
              </select>
            </Campo>
          )}
          {opcaoSelecionada?.exigeJustificativaExcedente && (
            <div className={estilos.alertaExcedente}>
              <div><AlertTriangle size={20} /><strong>Inclusão acima do efetivo previsto</strong></div>
              <p>O servidor exige aceite explícito e três referências auditáveis. A interface não recalcula a capacidade.</p>
              <label className={estilos.confirmacao}><input type="checkbox" checked={dados.confirmarInclusaoExcedente} onChange={(e) => alterar("confirmarInclusaoExcedente", e.target.checked)} /> Confirmo a inclusão excedente apresentada pelo servidor.</label>
              <div className={estilos.gradeFormulario}>
                <Campo rotulo="Justificativa" obrigatorio><textarea value={dados.justificativaExcedente} onChange={(e) => alterar("justificativaExcedente", e.target.value)} /></Campo>
                <Campo rotulo="Evidência" obrigatorio><textarea value={dados.evidenciaExcedente} onChange={(e) => alterar("evidenciaExcedente", e.target.value)} /></Campo>
                <Campo rotulo="Autorização" obrigatorio><textarea value={dados.autorizacaoExcedente} onChange={(e) => alterar("autorizacaoExcedente", e.target.value)} /></Campo>
              </div>
            </div>
          )}
        </fieldset>

        <footer className={estilos.rodapeModal}>
          <p><ShieldCheck size={16} /> A senha será solicitada no canal seguro; ela não compõe este formulário.</p>
          <div><button type="button" className={estilos.botaoSecundario} onClick={aoFechar} disabled={salvando}>Cancelar</button><button type="submit" className={estilos.botaoPrimario} disabled={salvando}>{salvando ? "Salvando…" : "Cadastrar vínculo"}</button></div>
        </footer>
      </form>
    </ModalEstrutural>
  );
};
