import React, { useEffect, useMemo, useState } from "react";
import { notificar } from "../../../../core/events.js";
import { CONFIG } from "../../../../core/config";
import { possuiAcessoGlobal } from "../../../../core/security/roles";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.jsx";
import { ServicoLotacao } from "../../../../services/admin/ServicoLotacao";
import { ServicoJornada } from "../../../../services/admin/ServicoJornada";
import { ServicoOrganizacional } from "../../../../services/admin/ServicoOrganizacional";
import { useAutenticacao } from "../../../auth/context.jsx";
import {
  contarPorEstado,
  ESTADO_PADRAO_ADMIN,
  filtrarPorEstado,
  normalizarUf,
  TODOS_OS_ESTADOS,
} from "../../constants/estados.js";
import { CabecalhoLotacoes } from "./components/CabecalhoLotacoes";
import { FiltrosLotacoes } from "./components/FiltrosLotacoes";
import { ListaLotacoes } from "./components/ListaLotacoes";
import { ModalLotacao } from "./components/ModalLotacao";
import estilos from "./index.module.css";

const formularioVazio = (gestaoLocalId = "") => ({
  id: null,
  nome: "",
  municipio: "",
  gestaoLocalId: gestaoLocalId ? String(gestaoLocalId) : "",
  ativo: true,
  turnos: [],
});

export const PaginaLotacoes = ({ podeEscrever = false }) => {
  const { user: usuario } = useAutenticacao();
  const acessoGlobal = possuiAcessoGlobal(usuario);
  const [lotacoes, setLotacoes] = useState([]);
  const [turnos, setTurnos] = useState([]);
  const [gestoesLocais, setGestoesLocais] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [termoBusca, setTermoBusca] = useState("");
  const [filtroEstado, setFiltroEstado] = useState(() =>
    acessoGlobal ? ESTADO_PADRAO_ADMIN : TODOS_OS_ESTADOS,
  );
  const [modalAberto, setModalAberto] = useState(false);
  const [dadosFormulario, setDadosFormulario] = useState(() =>
    formularioVazio(usuario?.gestaoLocalId),
  );

  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();

  const carregarTodos = async (opcoes = {}) => {
    setCarregando(true);
    try {
      const promessas = [
        ServicoLotacao.listarTodos({ size: CONFIG.PAGINATION_MAX_SIZE }, opcoes),
        ServicoJornada.listar(opcoes),
      ];
      if (acessoGlobal) promessas.push(ServicoOrganizacional.listarGestoesLocais({}, opcoes));
      const [resLotacoes, resTurnos, resGestoesLocais = []] = await Promise.all(promessas);
      setLotacoes(resLotacoes);
      setTurnos(resTurnos);
      setGestoesLocais(resGestoesLocais);
    } catch (erro) {
      if (!erro?.canceled) notificar("error", "Não foi possível carregar as lotações.");
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    const controlador = new AbortController();
    carregarTodos({ signal: controlador.signal });
    return () => controlador.abort();
  }, []);

  useEffect(() => {
    setFiltroEstado((estadoAtual) => {
      if (!acessoGlobal) return TODOS_OS_ESTADOS;
      return estadoAtual === TODOS_OS_ESTADOS ? ESTADO_PADRAO_ADMIN : estadoAtual;
    });
  }, [acessoGlobal]);

  const contagensPorEstado = useMemo(() => contarPorEstado(lotacoes), [lotacoes]);

  const lotacoesDoEstado = useMemo(
    () => filtrarPorEstado(lotacoes, filtroEstado, acessoGlobal),
    [lotacoes, filtroEstado, acessoGlobal],
  );

  const dadosFiltrados = useMemo(() => {
    const termo = termoBusca.trim().toLocaleLowerCase("pt-BR");
    if (!termo) return lotacoesDoEstado;
    return lotacoesDoEstado.filter((lotacao) => {
      const turnosConcatenados = (lotacao.turnos || [])
        .map((turno) => turno?.descricao)
        .join(" ");
      return [
        lotacao.nome,
        lotacao.municipio,
        lotacao.gestaoLocalNome,
        lotacao.superintendenciaNome,
        lotacao.uf,
        turnosConcatenados,
      ].some((valor) => String(valor || "").toLocaleLowerCase("pt-BR").includes(termo));
    });
  }, [lotacoesDoEstado, termoBusca]);

  const gestoesLocaisDoEstado = useMemo(() => {
    const selecionada = String(dadosFormulario.gestaoLocalId || "");
    const porEstado = (!acessoGlobal || filtroEstado === TODOS_OS_ESTADOS)
      ? gestoesLocais
      : gestoesLocais.filter((gestaoLocal) =>
        normalizarUf(gestaoLocal?.uf || gestaoLocal?.superintendencia?.uf) === filtroEstado,
      );
    return porEstado.filter((gestaoLocal) =>
      gestaoLocal?.ativo !== false || String(gestaoLocal?.id) === selecionada,
    );
  }, [gestoesLocais, filtroEstado, acessoGlobal, dadosFormulario.gestaoLocalId]);

  const turnosDoFormulario = useMemo(() => {
    const selecionados = new Set((dadosFormulario.turnos || []).map((turno) => String(turno.id)));
    const noEscopo = !dadosFormulario.gestaoLocalId
      ? (acessoGlobal ? [] : turnos)
      : turnos.filter(
        (turno) => String(turno.gestaoLocalId || "") === String(dadosFormulario.gestaoLocalId),
      );
    return noEscopo.filter((turno) => turno?.ativo !== false || selecionados.has(String(turno?.id)));
  }, [turnos, dadosFormulario.gestaoLocalId, dadosFormulario.turnos, acessoGlobal]);

  const abrirNovo = () => {
    if (!podeEscrever) return;
    setDadosFormulario(formularioVazio(usuario?.gestaoLocalId));
    setModalAberto(true);
  };

  const abrirEdicao = (lotacao) => {
    if (!podeEscrever) return;
    setDadosFormulario({
      id: lotacao.id,
      nome: lotacao.nome || "",
      municipio: lotacao.municipio || "",
      gestaoLocalId: String(lotacao.gestaoLocalId || usuario?.gestaoLocalId || ""),
      ativo: lotacao.ativo !== false,
      turnos: (lotacao.turnos || []).filter(Boolean).map((turno) => ({ id: turno.id })),
    });
    setModalAberto(true);
  };

  const mudarDadoFormulario = (campo, valor) => {
    setDadosFormulario((atual) => ({
      ...atual,
      [campo]: valor,
      ...(campo === "gestaoLocalId" ? { turnos: [] } : {}),
    }));
  };

  const alternarTurno = (idAlvo) => {
    setDadosFormulario((atual) => {
      const marcado = (atual.turnos || []).some(
        (turno) => String(turno.id) === String(idAlvo),
      );
      return {
        ...atual,
        turnos: marcado
          ? atual.turnos.filter((turno) => String(turno.id) !== String(idAlvo))
          : [...(atual.turnos || []), { id: idAlvo }],
      };
    });
  };

  const salvar = async (evento) => {
    evento.preventDefault();
    if (!podeEscrever) return;
    if (!dadosFormulario.nome?.trim()) return notificar("error", "Informe o nome da lotação.");
    if (!dadosFormulario.municipio?.trim()) return notificar("error", "Informe o município da lotação.");
    if (acessoGlobal && !dadosFormulario.gestaoLocalId) {
      return notificar("error", "Selecione a Gestão Local responsável pela lotação.");
    }

    await confirmarComSenha({
      titulo: dadosFormulario.id ? "Confirmar alteração de lotação" : "Confirmar criação de lotação",
      mensagem: "Informe sua senha para continuar.",
      acao: async (senhaConfirmacao) => {
        await ServicoLotacao.salvar({
          id: dadosFormulario.id || undefined,
          nome: dadosFormulario.nome.trim(),
          municipio: dadosFormulario.municipio.trim(),
          gestaoLocalId: dadosFormulario.gestaoLocalId ? Number(dadosFormulario.gestaoLocalId) : null,
          ativo: dadosFormulario.ativo !== false,
          turnos: (dadosFormulario.turnos || []).map((turno) => ({
            id: turno.id,
            descricao: "",
          })),
        }, { senhaConfirmacao });
        setModalAberto(false);
        await carregarTodos();
      },
    });
  };

  const excluir = async (idAlvo) => {
    if (!podeEscrever) return;
    await confirmarComSenha({
      titulo: "Inativar lotação",
      mensagem: "A lotação continuará disponível para histórico. O Java bloqueará a inativação se houver programação futura incompatível.",
      acao: async (senhaConfirmacao) => {
        await ServicoLotacao.excluir(idAlvo, { senhaConfirmacao });
        await carregarTodos();
      },
    });
  };

  return (
    <div className={estilos.containerPagina}>
      <ModalConfirmacao />
      <CabecalhoLotacoes
        aoCriar={abrirNovo}
        podeEscrever={podeEscrever}
        totalExibido={dadosFiltrados.length}
        estadoSelecionado={filtroEstado}
      />
      <FiltrosLotacoes
        termoBusca={termoBusca}
        aoMudarBusca={setTermoBusca}
        exibirFiltroEstado={acessoGlobal}
        estadoSelecionado={filtroEstado}
        aoMudarEstado={setFiltroEstado}
        contagensPorEstado={contagensPorEstado}
        totalResultados={dadosFiltrados.length}
      />
      <ListaLotacoes
        carregando={carregando}
        lotacoes={dadosFiltrados}
        turnos={turnos}
        aoEditar={abrirEdicao}
        aoExcluir={excluir}
        podeEscrever={podeEscrever}
        estadoSelecionado={filtroEstado}
      />
      <ModalLotacao
        aberto={modalAberto}
        aoFechar={() => setModalAberto(false)}
        dados={dadosFormulario}
        aoMudarDados={mudarDadoFormulario}
        aoSalvar={salvar}
        podeEscrever={podeEscrever}
        acessoGlobal={acessoGlobal}
        gestoesLocais={gestoesLocaisDoEstado}
        turnos={turnosDoFormulario}
        aoAlternarTurno={alternarTurno}
      />
    </div>
  );
};
