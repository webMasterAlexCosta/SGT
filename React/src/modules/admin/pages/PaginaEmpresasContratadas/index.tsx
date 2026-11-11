import { observarFalha } from '../../../../core/http/falhaObservada';
import type { FornecedorDTO } from '../../../../contracts/java';
import type { RequestOptions } from '../../../../core/http/types';
type CompanyOperation = ReturnType<ReturnType<typeof useConsultaVigente>['iniciar']>;
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { ServicoContrato } from "../../../../services/admin/ServicoContrato";
import { ServicoEmpresaContratada } from "../../../../services/admin/ServicoEmpresaContratada";
import { notificar } from "../../../../core/events";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha";
import { useConsultaVigente } from "../../../../core/hooks/useConsultaVigente.ts";
import { obterSinalSessao } from "../../../../core/security/geracaoSessao.ts";
import { podeGerenciarContratos } from "../../../../core/security/roles";
import { useAutenticacao } from "../../../auth/context.tsx";
import estilos from "./index.module.css";

const normalizarPesquisa = (valor: (string) | (string | boolean | null)) => String(valor ?? "").normalize("NFD").replace(/\p{Diacritic}/gu, "").toLocaleLowerCase("pt-BR");

export const PaginaEmpresasContratadas = () => {
  const { user } = useAutenticacao();
  const podeVincular = podeGerenciarContratos(user);
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
  const [pagina, setPagina] = useState(0);
  const [pesquisa, setPesquisa] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [erroConsulta, setErroConsulta] = useState("");
  const [contratos, setContratos] = useState<(import("../../../../contracts/opcoes-apresentacao").ContractView[])>([]);
  const [empresas, setEmpresas] = useState<(FornecedorDTO[])>([]);
  const [form, setForm] = useState<{ razaoSocial?: string; cnpj?: string; email?: string; contratoId?: string }>({});
  const [vinculoPendente, setVinculoPendente] = useState<({ empresa: FornecedorDTO; contratoId: string; mensagem: string; cadastroConfirmado: boolean; tentativaAnterior: boolean; }) | ({ empresa: FornecedorDTO; contratoId: string; cadastroConfirmado: boolean; tentativaAnterior: boolean; mensagem: string; }) | null>(null);
  const [avisoCadastro, setAvisoCadastro] = useState("");
  const consultas = useConsultaVigente();
  const operacoes = useConsultaVigente();
  const registrosFiltrados = useMemo(() => {
    const termo = normalizarPesquisa(pesquisa.trim());
    return termo ? empresas.filter((empresa) => [empresa.razaoSocial, empresa.cnpj, empresa.email, empresa.ativo]
      .some((valor) => normalizarPesquisa(valor).includes(termo))) : empresas;
  }, [empresas, pesquisa]);
  const totalPaginas = Math.max(1, Math.ceil(registrosFiltrados.length / 5));
  const paginaAtual = Math.min(pagina, totalPaginas - 1);
  const itensDaPagina = registrosFiltrados.slice(paginaAtual * 5, (paginaAtual + 1) * 5);

  const carregar = useCallback(async () => {
    const consulta = consultas.iniciar();
    setCarregando(true);
    setErroConsulta("");
    const options = { silentError: true, signal: consulta.signal };
    try {
      const resultados = await Promise.allSettled([
        ServicoEmpresaContratada.listar(options),
        ServicoContrato.listarTodos(options),
      ]);
      if (!consulta.atual()) return;
      if (resultados[0].status === "fulfilled") setEmpresas(resultados[0].value);
      if (resultados[1].status === "fulfilled") {
        const rascunhos = resultados[1].value.filter((contrato) => contrato.ativo !== false && contrato.estadoNegocial === "RASCUNHO");
        setContratos(rascunhos);
        setVinculoPendente((atual) => atual && !rascunhos.some((contrato) => String(contrato.id) === String(atual.contratoId))
          ? { ...atual, contratoId: "" } : atual);
      }
      const falha = resultados.find((resultado) => resultado.status === "rejected");
      if (falha) {
        const mensagem = observarFalha(falha.reason).message || "Parte dos dados cadastrais não pôde ser carregada.";
        setErroConsulta(mensagem);
        notificar("error", mensagem);
      }
    } finally {
      if (consulta.atual()) setCarregando(false);
    }
  }, [consultas]);

  useEffect(() => { carregar(); }, [carregar]);

  useEffect(() => {
    const sinal = obterSinalSessao();
    const limparIntencao = () => {
      setForm({});
      setVinculoPendente(null);
      setAvisoCadastro("");
    };
    sinal.addEventListener("abort", limparIntencao, { once: true });
    return () => sinal.removeEventListener("abort", limparIntencao);
  }, [user]);

  const mudar = (campo: (string)) => (evento: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setForm((atual) => ({ ...atual, [campo]: evento.target.value }));
  const executarProtegido = useCallback(async ({ titulo, mensagem, operacao, sucesso, depois }: { titulo: string; mensagem: string; operacao: (opcoes: RequestOptions, intencao: CompanyOperation) => Promise<FornecedorDTO | { parcial: boolean; mensagem: string } | void>; sucesso: string; depois?: () => void | Promise<void> }) => {
    const intencao = operacoes.iniciar();
    await confirmarComSenha({
      titulo,
      mensagem,
      acao: async (senhaConfirmacao) => {
        if (!intencao.atual()) return false;
        const resultado = await operacao({ senhaConfirmacao, signal: intencao.signal }, intencao);
        if (!intencao.atual()) return false;
        const parcial = resultado != null && typeof resultado === 'object' && 'parcial' in resultado && resultado.parcial;
        notificar(parcial ? "warning" : "success", parcial && resultado != null && typeof resultado === 'object' && 'mensagem' in resultado ? resultado.mensagem : sucesso);
        await carregar();
        if (depois && !parcial && intencao.atual()) await depois();
        return true;
      },
    });
  }, [carregar, confirmarComSenha, operacoes]);

  const associarEmpresaCadastrada = async (empresa: (FornecedorDTO), contratoId: (string), options: (RequestOptions<unknown>), intencao: ({ signal: AbortSignal; atual: () => boolean; }), cadastroConfirmado = false) => {
    try {
      await ServicoEmpresaContratada.vincularAoContrato(contratoId, empresa.id, { ...options, silentError: true });
      if (intencao.atual()) setVinculoPendente(null);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!intencao.atual()) throw erroOriginal;
      const situacaoCadastro = cadastroConfirmado ? "foi cadastrada" : "já consta no cadastro";
      const mensagem = `Empresa "${empresa.razaoSocial}" ${situacaoCadastro}. Não foi possível confirmar o vínculo ao contrato. ${erro?.message || "Tente novamente ou continue sem vínculo."}`;
      setVinculoPendente({ empresa, contratoId, mensagem, cadastroConfirmado, tentativaAnterior: true });
      return { parcial: true, mensagem };
    }
  };

  const criarEmpresa = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (vinculoPendente) return;
    await executarProtegido({
      titulo: "Cadastrar empresa contratada",
      mensagem: "Informe sua senha para cadastrar a empresa e, se houver contrato selecionado, vinculá-la.",
      operacao: async (options, intencao) => {
        let nova: Awaited<ReturnType<typeof ServicoEmpresaContratada.criar>>;
        try {
          nova = await ServicoEmpresaContratada.criar({
            razaoSocial: form.razaoSocial,
            cnpj: form.cnpj,
            email: form.email,
          }, options);
        } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

          if (intencao.atual() && !erro?.canceled && Number(erro?.status ?? 0) === 0) {
            setAvisoCadastro("Não foi possível confirmar o resultado do cadastro. Confira a lista atualizada. Se a empresa desejada estiver no cadastro, use Vincular para escolher um contrato em rascunho e confirmar a associação.");
            void carregar();
          }
          throw erroOriginal;
        }
        if (!intencao.atual()) return;
        // O POST já foi confirmado. A falha do vínculo não desfaz esse cadastro
        // nem autoriza repetir a criação na próxima confirmação de senha.
        setForm({});
        setAvisoCadastro("");
        setEmpresas((atuais) => [nova, ...atuais.filter((empresa) => empresa.id !== nova.id)]);
        setPagina(0);
        if (form.contratoId && nova?.id) return associarEmpresaCadastrada(nova, form.contratoId, options, intencao, true);
      },
      sucesso: "Empresa contratada cadastrada. O vínculo é aplicado somente quando um contrato em rascunho é selecionado.",
    });
  };

  const tentarVinculoNovamente = () => {
    if (!podeVincular || carregando || !vinculoPendente?.contratoId) return;
    const { empresa, contratoId, cadastroConfirmado } = vinculoPendente;
    return executarProtegido({
      titulo: "Vincular empresa já cadastrada",
      mensagem: "Confirme sua senha para associar a empresa existente ao contrato em rascunho. O cadastro não será repetido.",
      operacao: (options, intencao) => associarEmpresaCadastrada(empresa, contratoId, options, intencao, cadastroConfirmado),
      sucesso: "Empresa contratada vinculada ao contrato.",
    });
  };

  const prepararVinculo = (empresa: (FornecedorDTO)) => {
    if (!podeVincular || carregando || vinculoPendente) return;
    setForm({});
    setAvisoCadastro("");
    setVinculoPendente({
      empresa, contratoId: "", cadastroConfirmado: false, tentativaAnterior: false,
      mensagem: `Empresa "${empresa.razaoSocial}" já consta no cadastro. Escolha um contrato em rascunho e confirme sua senha para vinculá-la.`,
    });
    void carregar();
  };

  const editarEmpresa = (empresa: (FornecedorDTO)) => {
    const razaoSocial = window.prompt("Razão social", empresa.razaoSocial || "");
    if (razaoSocial === null) return;
    const cnpj = window.prompt("CNPJ", empresa.cnpj || "");
    if (cnpj === null) return;
    const email = window.prompt("E-mail", empresa.email || "");
    if (email === null) return;
    if (!razaoSocial.trim() || !cnpj.trim()) {
      notificar("warning", "Razão social e CNPJ são obrigatórios.");
      return;
    }
    return executarProtegido({
      titulo: "Atualizar empresa contratada",
      mensagem: "Confirme sua senha para atualizar os dados da empresa.",
      operacao: (options) => ServicoEmpresaContratada.atualizar(empresa.id, {
        razaoSocial: razaoSocial.trim(),
        cnpj: cnpj.trim(),
        email: email.trim() || null,
        ativo: empresa.ativo !== false,
      }, options),
      sucesso: "Empresa contratada atualizada.",
    });
  };

  const alternarEmpresaAtiva = (empresa: (FornecedorDTO)) => executarProtegido({
    titulo: empresa.ativo === false ? "Reativar empresa contratada" : "Inativar empresa contratada",
    mensagem: `Confirme sua senha para ${empresa.ativo === false ? "reativar" : "inativar"} esta empresa.`,
    operacao: (options) => {
      const { razaoSocial, cnpj } = empresa;
      if (razaoSocial == null || cnpj == null) throw new TypeError("Razão social e CNPJ são obrigatórios para atualizar a empresa.");
      return ServicoEmpresaContratada.atualizar(empresa.id, {
        razaoSocial, cnpj,
        email: empresa.email || null,
        ativo: empresa.ativo === false,
      }, options);
    },
    sucesso: empresa.ativo === false ? "Empresa contratada reativada." : "Empresa contratada inativada.",
  });

  const mensagemVazia = carregando ? "Carregando empresas..." : erroConsulta
    ? "Não foi possível concluir a consulta."
    : pesquisa.trim() ? "Nenhum registro encontrado para esta pesquisa." : "Nenhuma empresa contratada encontrada.";

  return (
    <main className={estilos.pagina}>
      <ModalConfirmacao />
      <header className={estilos.cabecalho}>
        <h1>Empresas contratadas</h1>
        <p>Cadastro de empresas e associação a contratos em rascunho.</p>
      </header>

      {erroConsulta && <p role="alert" className={estilos.erro}>{erroConsulta}</p>}

      <label className={estilos.pesquisa}>
        <span>Pesquisar empresas</span>
        <input type="search" value={pesquisa} onChange={(evento) => { setPesquisa(evento.target.value); setPagina(0); }} placeholder="Digite parte do registro" />
      </label>

      <section className={estilos.painel} aria-busy={carregando}>
        <h2>Cadastro de empresa contratada</h2>
        {avisoCadastro && <section className={estilos.vinculoPendente} aria-label="Conferência do cadastro da empresa">
          <p role="alert" className={estilos.erro}>{avisoCadastro}</p>
          <div className={estilos.acoes}><button type="button" disabled={carregando} onClick={carregar}>Atualizar lista</button></div>
        </section>}
        {vinculoPendente && <section className={estilos.vinculoPendente} aria-label="Vínculo pendente da empresa cadastrada">
          <p role={vinculoPendente.tentativaAnterior ? "alert" : undefined} className={vinculoPendente.tentativaAnterior ? estilos.erro : undefined}>{vinculoPendente.mensagem}</p>
          <div className={estilos.form}>
            <select aria-label="Contrato para vincular a empresa cadastrada" value={vinculoPendente.contratoId}
              onChange={(evento) => setVinculoPendente((atual) => atual ? { ...atual, contratoId: evento.target.value } : null)}>
              <option value="">Selecione um contrato em rascunho</option>
              {contratos.map((contrato) => <option key={contrato.id} value={(contrato.id) ?? undefined}>{contrato.numeroContrato || contrato.descricao}</option>)}
            </select>
            <button type="button" disabled={carregando || !vinculoPendente.contratoId} onClick={tentarVinculoNovamente}>{vinculoPendente.tentativaAnterior ? "Tentar vínculo novamente" : "Vincular empresa cadastrada"}</button>
            <button type="button" onClick={() => setVinculoPendente(null)}>Continuar sem vínculo</button>
          </div>
        </section>}
        <form className={estilos.form} onSubmit={criarEmpresa}>
          <input aria-label="Razão social da empresa" value={form.razaoSocial || ""} onChange={mudar("razaoSocial")} placeholder="Razão social" required disabled={Boolean(vinculoPendente)} />
          <input aria-label="CNPJ da empresa" value={form.cnpj || ""} onChange={mudar("cnpj")} placeholder="CNPJ" required disabled={Boolean(vinculoPendente)} />
          <input type="email" aria-label="E-mail da empresa" value={form.email || ""} onChange={mudar("email")} placeholder="E-mail" disabled={Boolean(vinculoPendente)} />
          <select aria-label="Contrato a vincular à empresa" value={form.contratoId || ""} onChange={mudar("contratoId")} disabled={Boolean(vinculoPendente)}>
            <option value="">Sem vínculo imediato</option>
            {contratos.map((contrato) => <option key={contrato.id} value={(contrato.id) ?? undefined}>{contrato.numeroContrato || contrato.descricao}</option>)}
          </select>
          <button type="submit" disabled={Boolean(vinculoPendente)}>Cadastrar e vincular</button>
        </form>
        <table className={estilos.tabela}>
          <thead><tr><th>Empresa contratada</th><th>CNPJ / contato</th><th>Situação</th><th>Ações</th></tr></thead>
          <tbody>
            {itensDaPagina.length === 0 && <tr><td className={estilos.vazio} colSpan={4}>{mensagemVazia}</td></tr>}
            {itensDaPagina.map((empresa) => <tr key={empresa.id}>
              <td>{empresa.razaoSocial}</td>
              <td>{empresa.cnpj}<br /><small>{empresa.email || "Sem e-mail"}</small></td>
              <td>{empresa.ativo === false ? "INATIVO" : "ATIVO"}</td>
              <td><div className={estilos.acoes}>
                {podeVincular && <button type="button" disabled={carregando || Boolean(vinculoPendente)} onClick={() => prepararVinculo(empresa)}>Vincular</button>}
                <button type="button" onClick={() => editarEmpresa(empresa)}>Editar</button>
                <button type="button" onClick={() => alternarEmpresaAtiva(empresa)}>{empresa.ativo === false ? "Reativar" : "Inativar"}</button>
              </div></td>
            </tr>)}
          </tbody>
        </table>
      </section>
      <nav className={estilos.paginacao} aria-label="Paginação de empresas">
        <span aria-live="polite">{registrosFiltrados.length} registro(s) · página {paginaAtual + 1} de {totalPaginas} · 5 por página</span>
        <button type="button" aria-label="Página anterior de empresas" disabled={carregando || paginaAtual === 0} onClick={() => setPagina(paginaAtual - 1)}>Anterior</button>
        <button type="button" aria-label="Próxima página de empresas" disabled={carregando || paginaAtual + 1 >= totalPaginas} onClick={() => setPagina(paginaAtual + 1)}>Próxima</button>
      </nav>
    </main>
  );
};
