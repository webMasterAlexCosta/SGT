import React, { useState, useEffect, useMemo, useCallback } from "react";
import { ServicoAgencia } from "../../../../services/admin/ServicoAgencia.js";
import { ServicoBanco } from "../../../../services/admin/ServicoBanco.js";
import { notificar } from "../../../../core/events.js";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.jsx";
import { CONFIG } from "../../../../core/config.js";
import estilos from "./index.module.css";
import { CabecalhoAgencias } from "./components/CabecalhoAgencias/index.jsx";
import { TabelaAgencias } from "./components/TabelaAgencias/index.jsx";
import { ModalAgencia } from "./components/ModalAgencia/index.jsx";

export const PaginaAgencias = ({ podeEscrever = false }) => {
  const [agencias, setAgencias] = useState([]);
  const [bancos, setBancos] = useState([]);
  const [termoBusca, setTermoBusca] = useState("");
  const [modalAberto, setModalAberto] = useState({ aberto: false, item: null });

  // CORREÇÃO: O formulário agora usa "bancoId" plano em conformidade com o DTO da API
  const [dadosFormulario, setDadosFormulario] = useState({
    numero: "",
    bancoId: "",
  });

  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();


  const atualizarListas = useCallback(() => {
    ServicoAgencia
      .listarTodos({ params: { size: CONFIG.PAGINATION_MAX_SIZE } })
      .then((resposta) => {
        const lista = Array.isArray(resposta) ? resposta : resposta?.content || [];
        setAgencias([...lista].sort((a, b) => b.id - a.id));
      })
      .catch(console.error);

    ServicoBanco
      .listarTodos({ params: { size: CONFIG.PAGINATION_MAX_SIZE } })
      .then((resposta) => {
        const lista = Array.isArray(resposta) ? resposta : resposta?.content || [];
        setBancos(lista);
      })
      .catch(console.error);
  }, []);

  useEffect(() => {
    atualizarListas();
  }, [atualizarListas]);

  // CORREÇÃO: Função auxiliar para resolver o nome do banco com base no ID
  const obterNomeBanco = useCallback((bancoId) => {
    const banco = bancos.find(b => String(b.id) === String(bancoId));
    return banco ? `${banco.codigo} - ${banco.nome}` : "-";
  }, [bancos]);

  const agenciasFiltradas = useMemo(() => {
    if (!termoBusca) return agencias;
    const termoMin = String(termoBusca || "").toLocaleLowerCase("pt-BR");
    return agencias.filter(
      (agencia) =>
        String(agencia?.numero || "").toLocaleLowerCase("pt-BR").includes(termoMin) ||
        String(obterNomeBanco(agencia?.bancoId) || "").toLocaleLowerCase("pt-BR").includes(termoMin)
    );
  }, [agencias, termoBusca, obterNomeBanco]);

  const abrirModal = (item = null) => {
    if (!podeEscrever) return;
    if (item) {
      setDadosFormulario({
        id: item.id,
        numero: item.numero,
        bancoId: item.bancoId || "",
      });
    } else {
      setDadosFormulario({ numero: "", bancoId: "" });
    }
    setModalAberto({ aberto: true, item });
  };

  const salvarAgencia = async (evento) => {
    if (!podeEscrever) return;
    evento.preventDefault();

    if (!dadosFormulario.bancoId) {
      return notificar("warning", "Selecione um banco.");
    }

    await confirmarComSenha({
      titulo: modalAberto.item ? "Confirmar alteração" : "Confirmar criação",
      mensagem: "Informe sua senha para salvar a agência.",
      acao: async (senhaConfirmacao) => {
        // Envia os dados perfeitamente mapeados para o DTO
        await ServicoAgencia.salvar({
          id: dadosFormulario.id,
          numero: dadosFormulario.numero,
          bancoId: Number(dadosFormulario.bancoId)
        }, { senhaConfirmacao });
        setModalAberto({ aberto: false, item: null });
        atualizarListas();
      },
    });
  };

  const excluirAgencia = async (idAlvo) => {
    if (!podeEscrever) return;
    await confirmarComSenha({
      titulo: "Excluir agência",
      mensagem: "Esta ação removerá a agência do sistema. Informe sua senha para confirmar.",
      acao: async (senhaConfirmacao) => {
        await ServicoAgencia.excluir(idAlvo, { senhaConfirmacao });
        atualizarListas();
      },
    });
  };

  return (
    <div className={estilos.containerPagina}>
      <ModalConfirmacao />

      <CabecalhoAgencias termoBusca={termoBusca} aoBuscar={setTermoBusca} aoCriar={() => abrirModal(null)} podeEscrever={podeEscrever} />
      <TabelaAgencias agencias={agenciasFiltradas} obterNomeBanco={obterNomeBanco} aoEditar={abrirModal} aoExcluir={excluirAgencia} podeEscrever={podeEscrever} />
      {!podeEscrever && <p className={estilos.avisoLeitura}>Consulta disponível no seu escopo. Criação, alteração e exclusão de agências são restritas ao Administrador.</p>}
      <ModalAgencia estado={modalAberto} dados={dadosFormulario} bancos={bancos} setDados={setDadosFormulario} aoFechar={() => setModalAberto({ aberto: false, item: null })} aoSalvar={salvarAgencia} />
    </div>
  );
};
