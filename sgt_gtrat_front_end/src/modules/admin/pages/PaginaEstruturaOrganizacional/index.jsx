import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.jsx";
import { notificar } from "../../../../core/events.js";
import { ServicoOrganizacional } from "../../../../services/admin/ServicoOrganizacional.js";
import { useAutenticacao } from "../../../auth/context.jsx";
import { eAdministradorTecnico, ePresidente } from "../../../../core/security/roles.js";
import estilos from "./index.module.css";
import { CabecalhoEstrutura } from "./components/CabecalhoEstrutura/index.jsx";
import { GradeGestoresEstaduais } from "./components/GradeGestoresEstaduais/index.jsx";
import { TabelaGestoesLocais } from "./components/TabelaGestoesLocais/index.jsx";
import { ModaisEstrutura } from "./components/ModaisEstrutura/index.jsx";
import { ModalSenhaTemporaria } from "../../components/ModalSenhaTemporaria/index.jsx";

const estadoInicialSuperintendencia = { id: null, nome: "", uf: "", ativo: true, ativoAnterior: true };
const estadoInicialGestaoLocal = { id: null, nome: "", codigo: "", superintendenciaId: "", ativo: true };

export const PaginaEstruturaOrganizacional = () => {
  const { user } = useAutenticacao();
  const presidente = ePresidente(user);
  const administrador = eAdministradorTecnico(user);
  const [superintendencias, setSuperintendencias] = useState([]);
  const [gestoesLocais, setGestoesLocais] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [busca, setBusca] = useState("");
  const [modal, setModal] = useState(null);
  const [formSuperintendencia, setFormSuperintendencia] = useState(estadoInicialSuperintendencia);
  const [formGestaoLocal, setFormGestaoLocal] = useState(estadoInicialGestaoLocal);
  const [credencialTemporaria, setCredencialTemporaria] = useState(null);
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const resultadoSuperintendencias = await ServicoOrganizacional.listarSuperintendencias();
      setSuperintendencias(resultadoSuperintendencias || []);

      try {
        const resultadoGestoesLocais = await ServicoOrganizacional.listarGestoesLocais();
        setGestoesLocais(resultadoGestoesLocais || []);
      } catch (erroGestoesLocais) {
        setGestoesLocais([]);
        if (!erroGestoesLocais?.canceled) {
          notificar("warning", "Estrutura estadual carregada, mas não foi possível carregar as Gestões Locais do seu escopo.");
        }
      }
    } catch (erro) {
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
    const mapa = new Map();
    gestoesLocais.forEach((gestaoLocal) => {
      const chave = String(gestaoLocal.superintendenciaId || gestaoLocal.superintendencia?.id || "");
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

  const abrirSuperintendencia = (item = estadoInicialSuperintendencia) => {
    if (!administrador) return;
    setFormSuperintendencia({
      ...estadoInicialSuperintendencia,
      ...item,
      ativo: item.ativo !== false,
      ativoAnterior: item.ativo !== false,
    });
    setModal("superintendencia");
  };

  const abrirGestaoLocal = (item = estadoInicialGestaoLocal) => {
    if (!administrador) return;
    setFormGestaoLocal({
      ...estadoInicialGestaoLocal,
      ...item,
      superintendenciaId: item.superintendenciaId
        ? String(item.superintendenciaId)
        : String(superintendencias[0]?.id || ""),
    });
    setModal("gestaoLocal");
  };

  const salvarSuperintendencia = async (evento) => {
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
        if (reativando && resposta?.temporaryPassword) setCredencialTemporaria(resposta);
        await carregar();
      },
    });
  };

  const salvarGestaoLocal = async (evento) => {
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
