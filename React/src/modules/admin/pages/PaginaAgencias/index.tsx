import type { FormDraft } from "../../../../contracts/formularios";
import type { AgenciaReferenciaDTO } from "../../../../contracts/java";
import React, { useState, useEffect, useMemo, useCallback } from "react";
import { ServicoAgencia } from "../../../../services/admin/ServicoAgencia.ts";
import { ServicoBanco } from "../../../../services/admin/ServicoBanco.ts";
import { notificar } from "../../../../core/events.ts";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.tsx";
import { CONFIG } from "../../../../core/config.ts";
import estilos from "./index.module.css";
import { CabecalhoAgencias } from "./components/CabecalhoAgencias/index.tsx";
import { TabelaAgencias } from "./components/TabelaAgencias/index.tsx";
import { ModalAgencia } from "./components/ModalAgencia/index.tsx";

export const PaginaAgencias = ({ podeEscrever = false }: { podeEscrever?: boolean }) => {
  const [agencias, setAgencias] = useState<(import("../../../../contracts/catalogos").CatalogDto<"/agencias">[])>([]);
  const [bancos, setBancos] = useState<(import("../../../../contracts/catalogos").CatalogDto<"/bancos">[])>([]);
  const [termoBusca, setTermoBusca] = useState("");
  const [modalAberto, setModalAberto] = useState<{ aberto: boolean; item: import("../../../../contracts/opcoes-apresentacao").AgencyView | null }>({ aberto: false, item: null });

  // CORREÇÃO: O formulário agora usa "bancoId" plano em conformidade com o DTO da API
  const [dadosFormulario, setDadosFormulario] = useState<FormDraft<AgenciaReferenciaDTO> & { bancoId?: string | number }>({
    numero: "",
    bancoId: "",
  });

  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();


  const atualizarListas = useCallback(() => {
    ServicoAgencia
      .listarTodos({ params: { size: CONFIG.PAGINATION_MAX_SIZE } })
      .then((resposta) => {
        const lista = resposta;
        setAgencias([...lista].sort((a, b) => Number(b.id) - Number(a.id)));
      })
      .catch(console.error);

    ServicoBanco
      .listarTodos({ params: { size: CONFIG.PAGINATION_MAX_SIZE } })
      .then((resposta) => {
        const lista = resposta;
        setBancos(lista);
      })
      .catch(console.error);
  }, []);

  useEffect(() => {
    atualizarListas();
  }, [atualizarListas]);

  // CORREÇÃO: Função auxiliar para resolver o nome do banco com base no ID
  const obterNomeBanco = useCallback((bancoId: number | null | undefined) => {
    const banco = bancos.find(b => String(b.id) === String(bancoId));
    return banco ? `${banco.codigo} - ${banco.nome}` : "-";
  }, [bancos]);

  const agenciasFiltradas = useMemo(() => {
    if (!termoBusca) return agencias;
    const termoMin = String(termoBusca || "").toLocaleLowerCase("pt-BR");
    return agencias.filter(
      (agencia) =>
        String(agencia?.numero || "").toLocaleLowerCase("pt-BR").includes(termoMin) ||
        String(obterNomeBanco(agencia?.banco?.id) || "").toLocaleLowerCase("pt-BR").includes(termoMin)
    );
  }, [agencias, termoBusca, obterNomeBanco]);

  const abrirModal = (item: import("../../../../contracts/opcoes-apresentacao").AgencyView | null = null) => {
    if (!podeEscrever) return;
    if (item) {
      setDadosFormulario({
        id: item.id,
        numero: item.numero,
        bancoId: item.banco?.id || item.bancoId || "",
      });
    } else {
      setDadosFormulario({ numero: "", bancoId: "" });
    }
    setModalAberto({ aberto: true, item });
  };

  const salvarAgencia = async (evento: React.FormEvent<HTMLFormElement>) => {
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
          id: dadosFormulario.id ? Number(dadosFormulario.id) : undefined,
          numero: dadosFormulario.numero ?? undefined,
          bancoId: Number(dadosFormulario.bancoId)
        }, { senhaConfirmacao });
        setModalAberto({ aberto: false, item: null });
        atualizarListas();
      },
    });
  };

  const excluirAgencia = async (idAlvo: number | null) => {
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
