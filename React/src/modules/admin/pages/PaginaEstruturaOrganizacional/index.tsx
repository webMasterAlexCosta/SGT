import { observarFalha } from '../../../../core/http/falhaObservada';
import type { StateManagementForm, LocalManagementForm } from '../../../../contracts/organizacao';
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.tsx";
import { notificar } from "../../../../core/events.ts";
import { ServicoOrganizacional } from "../../../../services/admin/ServicoOrganizacional.ts";
import { useAutenticacao } from "../../../auth/context.tsx";
import { eAdministradorTecnico, ePresidente } from "../../../../core/security/roles.ts";
import estilos from "./index.module.css";
import { CabecalhoEstrutura } from "./components/CabecalhoEstrutura/index.tsx";
import { GradeGestoresEstaduais } from "./components/GradeGestoresEstaduais/index.tsx";
import { TabelaGestoesLocais } from "./components/TabelaGestoesLocais/index.tsx";
import { ModaisEstrutura } from "./components/ModaisEstrutura/index.tsx";
import { ModalSenhaTemporaria } from "../../components/ModalSenhaTemporaria/index.tsx";

const estadoInicialSuperintendencia: StateManagementForm = { id: null, nome: "", uf: "", ativo: true, ativoAnterior: true };
const estadoInicialGestaoLocal: LocalManagementForm = { id: null, nome: "", codigo: "", superintendenciaId: "", ativo: true };

export const PaginaEstruturaOrganizacional = () => {
  const { user } = useAutenticacao();
  const presidente = ePresidente(user);
  const administrador = eAdministradorTecnico(user);
  const [superintendencias, setSuperintendencias] = useState<(import("../../../../contracts/java").SuperintendenciaDTO[])>([]);
  const [gestoesLocais, setGestoesLocais] = useState<(import("../../../../contracts/opcoes-apresentacao").LocalManagementView[])>([]);
  const [carregando, setCarregando] = useState(false);
  const [busca, setBusca] = useState("");
  const [modal, setModal] = useState<("superintendencia") | ("gestaoLocal") | null>(null);
  const [formSuperintendencia, setFormSuperintendencia] = useState(estadoInicialSuperintendencia);
  const [formGestaoLocal, setFormGestaoLocal] = useState(estadoInicialGestaoLocal);
  const [credencialTemporaria, setCredencialTemporaria] = useState<import("../../../../contracts/java").AtivacaoUsuarioAplicacaoDTO | null>(null);
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const resultadoSuperintendencias = await ServicoOrganizacional.listarSuperintendencias();
      setSuperintendencias(resultadoSuperintendencias || []);

      try {
        const resultadoGestoesLocais = await ServicoOrganizacional.listarGestoesLocais();
        setGestoesLocais(resultadoGestoesLocais || []);
      } catch (erroGestoesLocaisOriginal) {
      const erroGestoesLocais = observarFalha(erroGestoesLocaisOriginal);

        setGestoesLocais([]);
        if (!erroGestoesLocais?.canceled) {
          notificar("warning", "Estrutura estadual carregada, mas não foi possível carregar as Gestões Locais do seu escopo.");
        }
      }
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      setSuperintendencias([]);
      setGestoesLocais([]);
      if (!erro?.canceled) notificar("error", "Não foi possível carregar a estrutura organizacional.");
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const gestoesLocaisPorSuperintendencia = useMemo(() => {
    const mapa = new Map<string, import("../../../../contracts/opcoes-apresentacao").LocalManagementView[]>();
    gestoesLocais.forEach((gestaoLocal) => {
      const chave = String(gestaoLocal.superintendenciaId || "");
      mapa.set(chave, [...(mapa.get(chave) || []), gestaoLocal]);
    });
    return mapa;
  }, [gestoesLocais]);

  const gestoesLocaisFiltradas = useMemo(() => {
    const termo = busca.trim().toLowerCase();
    if (!termo) return gestoesLocais;
    return gestoesLocais.filter((item) =>
      [item.nome, item.codigo, item.superintendenciaNome, item.uf]
        .some((valor) => String(valor || "").toLowerCase().includes(termo)),
    );
  }, [gestoesLocais, busca]);

  const abrirSuperintendencia = (item: Partial<import("../../../../contracts/java").SuperintendenciaDTO> = estadoInicialSuperintendencia) => {
    if (!administrador) return;
    setFormSuperintendencia({
      ...estadoInicialSuperintendencia,
      ...item, id: item.id ?? null, nome: item.nome || "", uf: item.uf || "",
      ativo: item.ativo !== false,
      ativoAnterior: item.ativo !== false,
    });
    setModal("superintendencia");
  };

  const abrirGestaoLocal = (item: Omit<Partial<import("../../../../contracts/opcoes-apresentacao").LocalManagementView>, "superintendenciaId"> & { superintendenciaId?: string | number | null } = estadoInicialGestaoLocal) => {
    if (!administrador) return;
    setFormGestaoLocal({
      ...estadoInicialGestaoLocal,
      ...item, id: item.id ?? null, nome: item.nome || "", codigo: item.codigo || "", ativo: item.ativo !== false,
      superintendenciaId: item.superintendenciaId
        ? String(item.superintendenciaId)
        : String(superintendencias[0]?.id || ""),
    });
    setModal("gestaoLocal");
  };

  const salvarSuperintendencia = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const uf = formSuperintendencia.uf.trim().toUpperCase();
    if (!/^[A-Z]{2}$/.test(uf)) {
      notificar("error", "Informe uma UF válida com duas letras.");
      return;
    }
    const reativando = Boolean(formSuperintendencia.id)
      && formSuperintendencia.ativoAnterior === false
      && formSuperintendencia.ativo === true;
    const inativando = Boolean(formSuperintendencia.id)
      && formSuperintendencia.ativoAnterior !== false
      && formSuperintendencia.ativo === false;
    await confirmarComSenha({
      titulo: reativando ? "Reativar gestor estadual" : "Salvar gestor estadual",
      mensagem: reativando
        ? `O Gestor estadual/${uf} receberá uma nova senha temporária. Gestões Locais e usuários operacionais permanecerão inativos até serem reativados pelos respectivos gestores.`
        : inativando
          ? `Ao inativar o Gestor estadual/${uf}, todas as Gestões Locais e usuários operacionais vinculados serão inativados e suas senhas anteriores deixarão de funcionar.`
          : `Confirme sua senha para salvar o Gestor estadual/${uf}.`,
      acao: async (senhaConfirmacao) => {
        const { ativoAnterior, ...dadosSuperintendencia } = formSuperintendencia;
        const resposta = reativando
          ? await ServicoOrganizacional.ativarSuperintendencia(
            formSuperintendencia.id,
            { senhaConfirmacao },
          )
          : await ServicoOrganizacional.salvarSuperintendencia({
            ...dadosSuperintendencia,
            nome: `SE/${uf}`,
            uf,
          }, { senhaConfirmacao });
        setModal(null);
        if (reativando && resposta && typeof resposta === 'object' && 'temporaryPassword' in resposta && resposta.temporaryPassword) setCredencialTemporaria(resposta);
        await carregar();
      },
    });
  };

  const salvarGestaoLocal = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (!formGestaoLocal.nome.trim() || !formGestaoLocal.codigo.trim() || !formGestaoLocal.superintendenciaId) {
      notificar("error", "Informe nome, código e gestor estadual da Gestão Local.");
      return;
    }
    await confirmarComSenha({
      titulo: "Salvar Gestão Local",
      mensagem: "Confirme sua senha para salvar os dados da Gestão Local.",
      acao: async (senhaConfirmacao) => {
        await ServicoOrganizacional.salvarGestaoLocal({
          ...formGestaoLocal,
          nome: formGestaoLocal.nome.trim(),
          codigo: formGestaoLocal.codigo.trim().toUpperCase(),
          superintendenciaId: Number(formGestaoLocal.superintendenciaId),
        }, { senhaConfirmacao });
        setModal(null);
        await carregar();
      },
    });
  };

  return (
    <div className={estilos.containerPagina}>
      <ModalConfirmacao />
      <ModalSenhaTemporaria credencial={credencialTemporaria} aoFechar={() => setCredencialTemporaria(null)} />
      <CabecalhoEstrutura presidente={presidente} administrador={administrador} possuiGestorEstadual={superintendencias.length > 0} aoNovoGestaoLocal={() => abrirGestaoLocal()} />
      <GradeGestoresEstaduais itens={superintendencias} gestoesLocaisPorSuperintendencia={gestoesLocaisPorSuperintendencia} presidente={presidente || !administrador} carregando={carregando} aoEditar={administrador ? abrirSuperintendencia : null} />
      <TabelaGestoesLocais itens={gestoesLocaisFiltradas} busca={busca} aoBuscar={setBusca} carregando={carregando} aoEditar={administrador ? abrirGestaoLocal : null} somenteLeitura={!administrador} />
      <ModaisEstrutura modal={modal} administrador={administrador} formSuperintendencia={formSuperintendencia} setFormSuperintendencia={setFormSuperintendencia} formGestaoLocal={formGestaoLocal} setFormGestaoLocal={setFormGestaoLocal} superintendencias={superintendencias} aoFechar={() => setModal(null)} aoSalvarSuperintendencia={salvarSuperintendencia} aoSalvarGestaoLocal={salvarGestaoLocal} />
    </div>
  );
};
