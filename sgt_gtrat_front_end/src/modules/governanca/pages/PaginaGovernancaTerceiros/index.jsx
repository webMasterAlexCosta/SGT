import React, { useCallback, useEffect, useMemo, useState } from "react";
import { ServicoEmpregado } from "../../../../services/rh/ServicoEmpregado";
import { ServicoGovernancaTerceiros as servicoGovernanca } from "../../../../services/governanca/ServicoGovernancaTerceiros";
import { notificar } from "../../../../core/events";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha";
import estilos from "./index.module.css";

const hoje = new Date().toISOString().slice(0, 10);
const inicioMes = `${hoje.slice(0, 8)}01`;
const dinheiro = (valor) => Number(valor || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
const duracao = (minutos) => `${Math.floor(Number(minutos || 0) / 60)}h${String(Number(minutos || 0) % 60).padStart(2, "0")}`;
const opcoesSeguras = (senhaConfirmacao) => ({ senhaConfirmacao });

const decimalPtBr = (valor) => {
  const texto = String(valor ?? "").trim();
  if (!texto) return Number.NaN;
  const normalizado = texto.includes(",") ? texto.replace(/\./g, "").replace(",", ".") : texto;
  return Number(normalizado);
};

const TabelaVazia = ({ colunas, texto }) => (
  <tr><td className={estilos.vazio} colSpan={colunas}>{texto}</td></tr>
);

export const PaginaGovernancaTerceiros = () => {
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
  const [aba, setAba] = useState("folhasHoras");
  const [carregando, setCarregando] = useState(true);
  const [resumo, setResumo] = useState({});
  const [contratos, setContratos] = useState([]);
  const [cargos, setCargos] = useState([]);
  const [fornecedores, setFornecedores] = useState([]);
  const [processos, setProcessos] = useState([]);
  const [folhasHoras, setFolhasHoras] = useState([]);
  const [faturas, setFaturas] = useState([]);
  const [riscos, setRiscos] = useState([]);
  const [tabelasTarifarias, setTabelasTarifarias] = useState([]);
  const [form, setForm] = useState({
    inicio: inicioMes,
    fim: hoje,
    tipo: "ONBOARDING",
    prazo: hoje,
    adicionalNoturnoPercent: 20,
    proximaAvaliacao: hoje,
  });

  const carregar = useCallback(async () => {
    setCarregando(true);
    const silencioso = { silentError: true };
    const chamadas = [
      ["resumo", servicoGovernanca.resumo(silencioso)],
      ["auxiliares", ServicoEmpregado.buscarDadosAuxiliares(silencioso)],
      ["fornecedores", servicoGovernanca.fornecedores(silencioso)],
      ["processos", servicoGovernanca.processos(silencioso)],
      ["folhasHoras", servicoGovernanca.folhasHoras(silencioso)],
      ["faturas", servicoGovernanca.faturas(silencioso)],
      ["riscos", servicoGovernanca.riscos(silencioso)],
    ];

    try {
      const resultados = await Promise.allSettled(chamadas.map(([, promessa]) => promessa));
      const falhas = [];

      resultados.forEach((resultado, indice) => {
        const nome = chamadas[indice][0];
        if (resultado.status === "rejected") {
          falhas.push(resultado.reason);
          return;
        }

        const valor = resultado.value;
        if (nome === "resumo") setResumo(valor || {});
        if (nome === "auxiliares") {
          setContratos((valor?.contratos || []).filter((contrato) => contrato.ativo !== false));
          setCargos(valor?.cargos || []);
        }
        if (nome === "fornecedores") setFornecedores(Array.isArray(valor) ? valor : []);
        if (nome === "processos") setProcessos(Array.isArray(valor) ? valor : []);
        if (nome === "folhasHoras") setFolhasHoras(Array.isArray(valor) ? valor : []);
        if (nome === "faturas") setFaturas(Array.isArray(valor) ? valor : []);
        if (nome === "riscos") setRiscos(Array.isArray(valor) ? valor : []);
      });

      if (falhas.length > 0) {
        notificar("error", falhas[0]?.message || "Parte dos dados de governança não pôde ser carregada.");
      }
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  const mudar = (campo) => (evento) => setForm((atual) => ({ ...atual, [campo]: evento.target.value }));

  const carregarTabelasTarifarias = useCallback(async (contratoId, { atualizarFormulario = true } = {}) => {
    if (atualizarFormulario) setForm((atual) => ({ ...atual, contratoId }));
    if (!contratoId) {
      setTabelasTarifarias([]);
      return;
    }
    try {
      const lista = await servicoGovernanca.tabelasTarifarias(contratoId, { silentError: true });
      setTabelasTarifarias(Array.isArray(lista) ? lista : []);
    } catch (erro) {
      setTabelasTarifarias([]);
      notificar("error", erro?.message || "Não foi possível carregar as tabelas tarifárias do contrato.");
    }
  }, []);

  const executarProtegido = useCallback(async ({ titulo, mensagem, operacao, sucesso, depois }) => {
    await confirmarComSenha({
      titulo,
      mensagem,
      acao: async (senhaConfirmacao) => {
        await operacao(senhaConfirmacao);
        notificar("success", sucesso);
        await carregar();
        if (depois) await depois();
        return true;
      },
    });
  }, [carregar, confirmarComSenha]);

  const criarFornecedor = async (evento) => {
    evento.preventDefault();
    await executarProtegido({
      titulo: "Cadastrar fornecedor",
      mensagem: "Informe sua senha para cadastrar o fornecedor e, se houver contrato selecionado, vinculá-lo.",
      operacao: async (senha) => {
        const opcoes = opcoesSeguras(senha);
        const novo = await servicoGovernanca.criarFornecedor({
          razaoSocial: form.razaoSocial,
          cnpj: form.cnpj,
          email: form.email,
        }, opcoes);
        if (form.contratoFornecedorId && novo?.id) {
          await servicoGovernanca.vincularFornecedor(form.contratoFornecedorId, novo.id, opcoes);
        }
      },
      sucesso: "Fornecedor cadastrado. O vínculo contratual é aplicado somente quando um contrato em rascunho é selecionado.",
      depois: () => setForm((atual) => ({ ...atual, razaoSocial: "", cnpj: "", email: "" })),
    });
  };

  const editarFornecedor = (fornecedor) => {
    const razaoSocial = window.prompt("Razão social", fornecedor.razaoSocial || "");
    if (razaoSocial === null) return;
    const cnpj = window.prompt("CNPJ", fornecedor.cnpj || "");
    if (cnpj === null) return;
    const email = window.prompt("E-mail", fornecedor.email || "");
    if (email === null) return;
    if (!razaoSocial.trim() || !cnpj.trim()) {
      notificar("warning", "Razão social e CNPJ são obrigatórios.");
      return;
    }
    return executarProtegido({
      titulo: "Atualizar fornecedor",
      mensagem: "Confirme sua senha para atualizar os dados do fornecedor.",
      operacao: (senha) => servicoGovernanca.atualizarFornecedor(fornecedor.id, {
        razaoSocial: razaoSocial.trim(),
        cnpj: cnpj.trim(),
        email: email.trim() || null,
        ativo: fornecedor.ativo !== false,
      }, opcoesSeguras(senha)),
      sucesso: "Fornecedor atualizado.",
    });
  };

  const alternarFornecedorAtivo = (fornecedor) => executarProtegido({
    titulo: fornecedor.ativo === false ? "Reativar fornecedor" : "Inativar fornecedor",
    mensagem: `Confirme sua senha para ${fornecedor.ativo === false ? "reativar" : "inativar"} este fornecedor.`,
    operacao: (senha) => servicoGovernanca.atualizarFornecedor(fornecedor.id, {
      razaoSocial: fornecedor.razaoSocial,
      cnpj: fornecedor.cnpj,
      email: fornecedor.email || null,
      ativo: fornecedor.ativo === false,
    }, opcoesSeguras(senha)),
    sucesso: fornecedor.ativo === false ? "Fornecedor reativado." : "Fornecedor inativado.",
  });

  const rejeitarFolhaHoras = (folhaHoras) => {
    const motivo = window.prompt("Motivo da rejeição");
    if (motivo === null) return;
    if (!motivo.trim()) return notificar("warning", "Informe o motivo da rejeição.");
    return executarProtegido({
      titulo: "Rejeitar folha de horas",
      mensagem: "Confirme sua senha para rejeitar a folha de horas submetida.",
      operacao: (senha) => servicoGovernanca.rejeitarFolhaHoras(folhaHoras.id, motivo.trim(), opcoesSeguras(senha)),
      sucesso: "Folha de horas rejeitada.",
    });
  };

  const faturarFolhaHoras = (folhaHoras) => {
    const informado = window.prompt("Valor das glosas", "0");
    if (informado === null) return;
    const glosas = decimalPtBr(informado);
    if (!Number.isFinite(glosas) || glosas < 0) return notificar("warning", "Informe um valor de glosa válido.");
    return executarProtegido({
      titulo: "Gerar fatura",
      mensagem: "Confirme sua senha para gerar a fatura desta folha de horas aprovada.",
      operacao: (senha) => servicoGovernanca.gerarFatura(folhaHoras.id, glosas, opcoesSeguras(senha)),
      sucesso: "Fatura criada.",
    });
  };

  const emitirFatura = (fatura) => {
    const numeroDocumento = window.prompt("Número do documento fiscal", fatura.numeroDocumento || "");
    if (numeroDocumento === null) return;
    if (!numeroDocumento.trim()) return notificar("warning", "Informe o número do documento fiscal.");
    return executarProtegido({
      titulo: "Emitir fatura",
      mensagem: "Confirme sua senha para marcar a fatura como emitida.",
      operacao: (senha) => servicoGovernanca.atualizarFatura(fatura.id, "EMITIDA", numeroDocumento.trim(), opcoesSeguras(senha)),
      sucesso: "Fatura emitida.",
    });
  };

  const seletorContrato = useMemo(() => (
    <select aria-label="Contrato da folha de horas" value={form.contratoId || ""} onChange={(evento) => carregarTabelasTarifarias(evento.target.value)} required>
      <option value="">Contrato</option>
      {contratos.map((contrato) => (
        <option key={contrato.id} value={contrato.id}>{contrato.numeroContrato || contrato.descricao}</option>
      ))}
    </select>
  ), [carregarTabelasTarifarias, contratos, form.contratoId]);

  return (
    <main className={estilos.pagina}>
      <ModalConfirmacao />

      <header className={estilos.cabecalho}>
        <h1>Governança do ciclo de terceiros</h1>
        <p>Do ingresso ao faturamento, com dupla verificacao, tabela tarifaria e risco contínuo.</p>
      </header>

      <section className={estilos.kpis} aria-busy={carregando}>
        {[
          ["Fornecedores", resumo.fornecedores],
          ["Processos abertos", resumo.processosAbertos],
          ["Folhas de horas pendentes", resumo.folhasHorasPendentes],
          ["Faturas abertas", resumo.faturasAbertas],
          ["Riscos vencidos", resumo.riscosVencidos],
        ].map(([rotulo, valor]) => (
          <div className={estilos.kpi} key={rotulo}><span>{rotulo}</span><strong>{Number(valor || 0)}</strong></div>
        ))}
      </section>

      <nav className={estilos.abas} aria-label="Etapas da governança de terceiros">
        {[
          ["folhasHoras", "Folhas de horas"], ["faturamento", "Faturamento"], ["processos", "Admissão/Desligamento"],
          ["tabelasTarifarias", "Tabelas tarifárias"], ["fornecedores", "Fornecedores"], ["riscos", "Risco contínuo"],
        ].map(([id, label]) => (
          <button type="button" key={id} className={aba === id ? estilos.ativa : ""} onClick={() => setAba(id)}>{label}</button>
        ))}
      </nav>

      {aba === "folhasHoras" && (
        <section className={estilos.painel}>
          <h2>Apuração e aprovação de folhas de horas</h2>
          <form className={estilos.form} onSubmit={(evento) => {
            evento.preventDefault();
            executarProtegido({
              titulo: "Gerar folha de horas",
              mensagem: "Confirme sua senha para recalcular o período pela frequência oficial.",
              operacao: (senha) => servicoGovernanca.gerarFolhaHoras({ contratoId: form.contratoId, inicio: form.inicio, fim: form.fim }, opcoesSeguras(senha)),
              sucesso: "Folha de horas recalculada pela frequência oficial.",
            });
          }}>
            {seletorContrato}
            <input type="date" aria-label="Data inicial da folha de horas" value={form.inicio} onChange={mudar("inicio")} required />
            <input type="date" aria-label="Data final da folha de horas" value={form.fim} onChange={mudar("fim")} required />
            <button type="submit">Gerar da frequência oficial</button>
          </form>
          <table className={estilos.tabela}>
            <thead><tr><th>Contrato/período</th><th>Versão</th><th>Horas</th><th>Valor</th><th>Status</th><th>Ações</th></tr></thead>
            <tbody>
              {folhasHoras.length === 0 && <TabelaVazia colunas={6} texto={carregando ? "Carregando folhas de horas..." : "Nenhuma folha de horas encontrada."} />}
              {folhasHoras.map((t) => (
                <tr key={t.id}>
                  <td>{t.contrato}<br />{t.inicio} a {t.fim}</td>
                  <td>{t.calculoVersao}</td>
                  <td>{duracao(t.minutosPagaveis)}<br /><small>físicas {duracao(t.minutosFisicos)} · noturnas {duracao(t.minutosNoturnos)}</small></td>
                  <td>{dinheiro(t.valorApurado)}</td>
                  <td>{t.status}<br /><small>{t.submetidoPor && `por ${t.submetidoPor}`}</small>{t.motivoRejeicao && <><br /><small>{t.motivoRejeicao}</small></>}</td>
                  <td><div className={estilos.acoes}>
                    {["RASCUNHO", "REJEITADO"].includes(t.status) && <button type="button" onClick={() => executarProtegido({ titulo: "Submeter folha de horas", mensagem: "Confirme sua senha para enviar a folha de horas para aprovação.", operacao: (senha) => servicoGovernanca.submeterFolhaHoras(t.id, opcoesSeguras(senha)), sucesso: "Folha de horas submetida." })}>Submeter</button>}
                    {t.status === "SUBMETIDO" && <>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Aprovar folha de horas", mensagem: "Confirme sua senha para aprovar a folha de horas. A segregação de função será validada pelo sistema.", operacao: (senha) => servicoGovernanca.aprovarFolhaHoras(t.id, opcoesSeguras(senha)), sucesso: "Folha de horas aprovada." })}>Aprovar</button>
                      <button type="button" onClick={() => rejeitarFolhaHoras(t)}>Rejeitar</button>
                    </>}
                    {t.status === "APROVADO" && <button type="button" onClick={() => faturarFolhaHoras(t)}>Faturar</button>}
                  </div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {aba === "faturamento" && (
        <section className={estilos.painel}>
          <h2>Faturamento</h2>
          <table className={estilos.tabela}>
            <thead><tr><th>Contrato</th><th>Folha de horas</th><th>Bruto</th><th>Glosas</th><th>Líquido</th><th>Status</th><th>Ações</th></tr></thead>
            <tbody>
              {faturas.length === 0 && <TabelaVazia colunas={7} texto={carregando ? "Carregando faturas..." : "Nenhuma fatura encontrada."} />}
              {faturas.map((f) => (
                <tr key={f.id}>
                  <td>{f.contrato}</td><td>#{f.folhaHorasId}</td><td>{dinheiro(f.valorBruto)}</td><td>{dinheiro(f.glosas)}</td><td>{dinheiro(f.valorLiquido)}</td><td>{f.status}</td>
                  <td><div className={estilos.acoes}>
                    {f.status === "RASCUNHO" && <>
                      <button type="button" onClick={() => emitirFatura(f)}>Emitir</button>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Cancelar fatura", mensagem: "Confirme sua senha para cancelar a fatura.", operacao: (senha) => servicoGovernanca.atualizarFatura(f.id, "CANCELADA", "", opcoesSeguras(senha)), sucesso: "Fatura cancelada." })}>Cancelar</button>
                    </>}
                    {f.status === "EMITIDA" && <>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Confirmar pagamento", mensagem: "Confirme sua senha para marcar a fatura como paga.", operacao: (senha) => servicoGovernanca.atualizarFatura(f.id, "PAGA", f.numeroDocumento || "", opcoesSeguras(senha)), sucesso: "Pagamento confirmado." })}>Confirmar pagamento</button>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Cancelar fatura", mensagem: "Confirme sua senha para cancelar a fatura emitida.", operacao: (senha) => servicoGovernanca.atualizarFatura(f.id, "CANCELADA", f.numeroDocumento || "", opcoesSeguras(senha)), sucesso: "Fatura cancelada." })}>Cancelar</button>
                    </>}
                  </div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {aba === "processos" && (
        <section className={estilos.painel}>
          <h2>Admissão e desligamento</h2>
          <form className={estilos.form} onSubmit={(evento) => {
            evento.preventDefault();
            executarProtegido({
              titulo: "Criar processo",
              mensagem: "Confirme sua senha para criar o processo de admissão ou desligamento.",
              operacao: (senha) => servicoGovernanca.criarProcesso({ empregadoId: form.empregadoId, contratoId: form.contratoId, tipo: form.tipo, prazo: form.prazo, checklistJson: form.checklistJson, observacao: form.observacao }, opcoesSeguras(senha)),
              sucesso: "Processo criado.",
            });
          }}>
            <input type="number" min="1" aria-label="ID do empregado" value={form.empregadoId || ""} onChange={mudar("empregadoId")} placeholder="ID do empregado" required />
            {seletorContrato}
            <select aria-label="Tipo do processo" value={form.tipo} onChange={mudar("tipo")}><option value="ONBOARDING">Admissão</option><option value="OFFBOARDING">Desligamento</option></select>
            <input type="date" aria-label="Prazo do processo" value={form.prazo} onChange={mudar("prazo")} required />
            <input aria-label="Checklist e evidências" value={form.checklistJson || ""} onChange={mudar("checklistJson")} placeholder="Checklist/evidências" />
            <input aria-label="Observação do processo" value={form.observacao || ""} onChange={mudar("observacao")} placeholder="Observação" />
            <button type="submit">Criar processo</button>
          </form>
          <table className={estilos.tabela}>
            <thead><tr><th>Colaborador</th><th>Tipo</th><th>Contrato/prazo</th><th>Status</th><th>Ações</th></tr></thead>
            <tbody>
              {processos.length === 0 && <TabelaVazia colunas={5} texto={carregando ? "Carregando processos..." : "Nenhum processo encontrado."} />}
              {processos.map((p) => (
                <tr key={p.id}>
                  <td>{p.empregado}</td><td>{p.tipo}</td><td>{p.contrato}<br />{p.prazo}</td><td>{p.status}</td>
                  <td><div className={estilos.acoes}>
                    {p.status === "PENDENTE" && <>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Iniciar processo", mensagem: "Confirme sua senha para iniciar este processo.", operacao: (senha) => servicoGovernanca.atualizarProcesso(p.id, "EM_ANDAMENTO", opcoesSeguras(senha)), sucesso: "Processo iniciado." })}>Iniciar</button>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Bloquear processo", mensagem: "Confirme sua senha para bloquear este processo.", operacao: (senha) => servicoGovernanca.atualizarProcesso(p.id, "BLOQUEADO", opcoesSeguras(senha)), sucesso: "Processo bloqueado." })}>Bloquear</button>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Cancelar processo", mensagem: "Confirme sua senha para cancelar este processo.", operacao: (senha) => servicoGovernanca.atualizarProcesso(p.id, "CANCELADO", opcoesSeguras(senha)), sucesso: "Processo cancelado." })}>Cancelar</button>
                    </>}
                    {p.status === "EM_ANDAMENTO" && <>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Concluir processo", mensagem: "Confirme sua senha para concluir este processo.", operacao: (senha) => servicoGovernanca.atualizarProcesso(p.id, "CONCLUIDO", opcoesSeguras(senha)), sucesso: "Processo concluído." })}>Concluir</button>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Bloquear processo", mensagem: "Confirme sua senha para bloquear este processo.", operacao: (senha) => servicoGovernanca.atualizarProcesso(p.id, "BLOQUEADO", opcoesSeguras(senha)), sucesso: "Processo bloqueado." })}>Bloquear</button>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Cancelar processo", mensagem: "Confirme sua senha para cancelar este processo.", operacao: (senha) => servicoGovernanca.atualizarProcesso(p.id, "CANCELADO", opcoesSeguras(senha)), sucesso: "Processo cancelado." })}>Cancelar</button>
                    </>}
                    {p.status === "BLOQUEADO" && <>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Retomar processo", mensagem: "Confirme sua senha para retomar este processo.", operacao: (senha) => servicoGovernanca.atualizarProcesso(p.id, "EM_ANDAMENTO", opcoesSeguras(senha)), sucesso: "Processo retomado." })}>Retomar</button>
                      <button type="button" onClick={() => executarProtegido({ titulo: "Cancelar processo", mensagem: "Confirme sua senha para cancelar este processo.", operacao: (senha) => servicoGovernanca.atualizarProcesso(p.id, "CANCELADO", opcoesSeguras(senha)), sucesso: "Processo cancelado." })}>Cancelar</button>
                    </>}
                  </div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {aba === "tabelasTarifarias" && (
        <section className={estilos.painel}>
          <h2>Tabelas tarifárias</h2>
          <form className={estilos.form} onSubmit={(evento) => {
            evento.preventDefault();
            const contratoAtual = form.contratoId;
            executarProtegido({
              titulo: "Cadastrar tabela tarifária",
              mensagem: "Confirme sua senha para cadastrar a tarifa do contrato.",
              operacao: (senha) => servicoGovernanca.criarTabelaTarifaria({ contratoId: contratoAtual, cargoId: form.cargoId || null, vigenciaInicio: form.vigenciaInicio || hoje, valorHora: form.valorHora, adicionalNoturnoPercent: form.adicionalNoturnoPercent }, opcoesSeguras(senha)),
              sucesso: "Tabela tarifária criada.",
              depois: () => carregarTabelasTarifarias(contratoAtual, { atualizarFormulario: false }),
            });
          }}>
            {seletorContrato}
            <select aria-label="Cargo da tabela tarifária" value={form.cargoId || ""} onChange={mudar("cargoId")}><option value="">Todos os cargos</option>{cargos.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}</select>
            <input type="date" aria-label="Início da vigência tarifária" value={form.vigenciaInicio || hoje} onChange={mudar("vigenciaInicio")} required />
            <input type="number" min="0.0001" step="0.0001" aria-label="Valor por hora" value={form.valorHora || ""} onChange={mudar("valorHora")} placeholder="Valor-hora" required />
            <input type="number" min="0" max="100" step="0.01" aria-label="Adicional noturno percentual" value={form.adicionalNoturnoPercent} onChange={mudar("adicionalNoturnoPercent")} placeholder="Adicional noturno %" />
            <button type="submit">Cadastrar tarifa</button>
          </form>
          <table className={estilos.tabela}>
            <thead><tr><th>Contrato</th><th>Cargo</th><th>Vigência</th><th>Valor-hora</th><th>Noturno</th></tr></thead>
            <tbody>
              {tabelasTarifarias.length === 0 && <TabelaVazia colunas={5} texto={form.contratoId ? "Nenhuma tabela tarifária para o contrato selecionado." : "Selecione um contrato para consultar as tarifas."} />}
              {tabelasTarifarias.map((r) => <tr key={r.id}><td>{r.contrato}</td><td>{r.cargo}</td><td>{r.vigenciaInicio} a {r.vigenciaFim || "aberta"}</td><td>{dinheiro(r.valorHora)}</td><td>{r.adicionalNoturnoPercent}%</td></tr>)}
            </tbody>
          </table>
        </section>
      )}

      {aba === "fornecedores" && (
        <section className={estilos.painel}>
          <h2>Fornecedores</h2>
          <form className={estilos.form} onSubmit={criarFornecedor}>
            <input aria-label="Razão social do fornecedor" value={form.razaoSocial || ""} onChange={mudar("razaoSocial")} placeholder="Razão social" required />
            <input aria-label="CNPJ do fornecedor" value={form.cnpj || ""} onChange={mudar("cnpj")} placeholder="CNPJ" required />
            <input type="email" aria-label="E-mail do fornecedor" value={form.email || ""} onChange={mudar("email")} placeholder="E-mail" />
            <select aria-label="Contrato a vincular ao fornecedor" value={form.contratoFornecedorId || ""} onChange={mudar("contratoFornecedorId")}><option value="">Sem vínculo imediato</option>{contratos.map((c) => <option key={c.id} value={c.id}>{c.numeroContrato || c.descricao}</option>)}</select>
            <button type="submit">Cadastrar e vincular</button>
          </form>
          <table className={estilos.tabela}>
            <thead><tr><th>Fornecedor</th><th>CNPJ / contato</th><th>Risco</th><th>Monitoramento</th><th>Próxima revisão</th><th>Situação</th><th>Ações</th></tr></thead>
            <tbody>
              {fornecedores.length === 0 && <TabelaVazia colunas={7} texto={carregando ? "Carregando fornecedores..." : "Nenhum fornecedor encontrado."} />}
              {fornecedores.map((f) => <tr key={f.id}>
                <td>{f.razaoSocial}</td><td>{f.cnpj}<br /><small>{f.email || "Sem e-mail"}</small></td><td>{f.riscoNivel} ({f.riscoScore})</td><td>{f.monitoramentoStatus}</td><td>{f.proximaAvaliacao || "Não avaliado"}</td><td>{f.ativo === false ? "INATIVO" : "ATIVO"}</td>
                <td><div className={estilos.acoes}><button type="button" onClick={() => editarFornecedor(f)}>Editar</button><button type="button" onClick={() => alternarFornecedorAtivo(f)}>{f.ativo === false ? "Reativar" : "Inativar"}</button></div></td>
              </tr>)}
            </tbody>
          </table>
        </section>
      )}

      {aba === "riscos" && (
        <section className={estilos.painel}>
          <h2>Risco contínuo de fornecedores</h2>
          <form className={estilos.form} onSubmit={(evento) => {
            evento.preventDefault();
            executarProtegido({
              titulo: "Registrar avaliação de risco",
              mensagem: "Confirme sua senha para registrar a avaliação e atualizar o risco do fornecedor.",
              operacao: (senha) => servicoGovernanca.avaliarRisco({ fornecedorId: form.fornecedorId, scoreTrabalhista: form.scoreTrabalhista, scoreFinanceiro: form.scoreFinanceiro, scoreCompliance: form.scoreCompliance, scoreSeguranca: form.scoreSeguranca, proximaAvaliacao: form.proximaAvaliacao, evidencias: form.evidencias }, opcoesSeguras(senha)),
              sucesso: "Risco reavaliado.",
            });
          }}>
            <select aria-label="Fornecedor para avaliação de risco" value={form.fornecedorId || ""} onChange={mudar("fornecedorId")} required><option value="">Fornecedor</option>{fornecedores.map((f) => <option key={f.id} value={f.id}>{f.razaoSocial}</option>)}</select>
            {["scoreTrabalhista", "scoreFinanceiro", "scoreCompliance", "scoreSeguranca"].map((campo) => (
              <input
                key={campo}
                type="number"
                min="0"
                max="100"
                aria-label={campo.replace("score", "Score ")}
                value={form[campo] || ""}
                onChange={mudar(campo)}
                placeholder={campo.replace("score", "Score ")}
                required
              />
            ))}
            <input type="date" aria-label="Próxima avaliação de risco" value={form.proximaAvaliacao} onChange={mudar("proximaAvaliacao")} required />
            <input aria-label="Evidências e plano de ação" value={form.evidencias || ""} onChange={mudar("evidencias")} placeholder="Evidências e plano de ação" />
            <button type="submit">Registrar avaliação</button>
          </form>
          <table className={estilos.tabela}>
            <thead><tr><th>Fornecedor</th><th>Score/nível</th><th>Avaliação</th><th>Próxima</th><th>Responsável</th></tr></thead>
            <tbody>
              {riscos.length === 0 && <TabelaVazia colunas={5} texto={carregando ? "Carregando avaliações..." : "Nenhuma avaliação de risco encontrada."} />}
              {riscos.map((r) => <tr key={r.id}><td>{r.fornecedor}</td><td>{r.scoreTotal} · {r.nivel}</td><td>{r.avaliadoEm}</td><td>{r.proximaAvaliacao}</td><td>{r.avaliadoPor}</td></tr>)}
            </tbody>
          </table>
        </section>
      )}
    </main>
  );
};
