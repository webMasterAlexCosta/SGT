import React, { useState, useEffect, useCallback, Suspense, lazy } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { AlertCircle, Zap, Sparkles, Lightbulb } from "lucide-react";

import { useAutenticacao } from "../../context.jsx";
import { redefinirIndicadorRedirecionamento, notificar } from "../../../../core/events.js";
import { ConteinerAvisos } from "../../../../core/ui/Aviso.jsx";
import { CONFIG } from "../../../../core/config.js";
import { rotaInicialPorPerfil } from "../../../../core/security/roles.js";
import { ServicoAutenticacao } from "../../../../services/auth/ServicoAutenticacao.js";
import {
  formatarMatricula,
  matriculaEstaCompleta,
} from "../../../../core/formatters/matricula.js";

import Homologacao from "../../../../core/ui/Homologacao.jsx";
import logoCorreios from "../../../../assets/correios.png";
import estilos from "./index.module.css";

import { TourGuiado } from "./components/TourGuiado.jsx";
import { IconesTecnologia } from "./components/IconesTecnologia.jsx";
import { ModalTeste } from "./components/ModalTeste.jsx";
import { FormularioAcesso } from "./components/FormularioAcesso.jsx";
import { FormularioRecuperacaoSenha } from "./components/FormularioRecuperacaoSenha.jsx";

const FundoAnimado = lazy(() =>
  import("./components/FundoAnimado.jsx").then((module) => ({
    default: module.FundoAnimado,
  })),
);

export const Login = () => {
  const { autenticar, cadastrar, user, atualizarPerfil } = useAutenticacao();
  const navegar = useNavigate();
  const localizacao = useLocation();

  const [estadoTela, setEstadoTela] = useState("LOGIN");
  const [credenciais, setCredenciais] = useState({
    username: "",
    password: "",
    confirmPassword: "",
    email: "",
    matricula: "",
    totp: "",
    superintendenciaId: "",
    gestaoLocalId: "",
  });
  const [novaSenha, setNovaSenha] = useState({ password: "", confirm: "" });
  const [carregando, setCarregando] = useState(false);
  const [bloqueado, setBloqueado] = useState(false);
  const [tempoBloqueio, setTempoBloqueio] = useState(0);
  const [mostrarModalTeste, setMostrarModalTeste] = useState(
    CONFIG.SHOW_TEST_CREDENTIALS,
  );
  const [tourAberto, setTourAberto] = useState(false);
  const [superintendencias, setSuperintendencias] = useState([]);
  const [carregandoSuperintendencias, setCarregandoSuperintendencias] = useState(false);
  const [gestoesLocais, setGestoesLocais] = useState([]);
  const [carregandoGestoesLocais, setCarregandoGestoesLocais] = useState(false);
  const [segundoFatorPendente, setSegundoFatorPendente] = useState(false);
  const [emailRecuperacao, setEmailRecuperacao] = useState("");
  const [tokenRecuperacaoValido, setTokenRecuperacaoValido] = useState(null);
  const [tokenRecuperacao, setTokenRecuperacao] = useState("");
  const [recuperacaoConfirmada, setRecuperacaoConfirmada] = useState(false);
  const [mensagemTokenRecuperacao, setMensagemTokenRecuperacao] = useState("");
  const [validandoTokenRecuperacao, setValidandoTokenRecuperacao] = useState(false);

  const chaveStorageDesempenho = `${CONFIG.APP_NAME.toLowerCase()}_perf_mode`;

  const [modoDesempenho, setModoDesempenho] = useState(() => {
    const salvo = localStorage.getItem(chaveStorageDesempenho);
    return salvo !== null ? salvo === "true" : true;
  });

  const alternarModoDesempenho = useCallback(() => {
    setModoDesempenho((estadoAnterior) => {
      const proximoValor = !estadoAnterior;
      localStorage.setItem(chaveStorageDesempenho, String(proximoValor));
      return proximoValor;
    });
  }, [chaveStorageDesempenho]);

  const passosTour = [
    {
      id: "login-form",
      title: "Acesso Seguro",
      text: "Informe sua matrícula e senha. Se for seu primeiro acesso, use a senha padrão informada pela Gestão Local.",
    },
    {
      id: "login-switch",
      title: "Primeiro Acesso?",
      text: "Informe login, matrícula, e-mail e seu gestor estadual. A Gestão Local responsável receberá a solicitação para definir o escopo e ativar o acesso.",
    },
    {
      id: "perf-toggle",
      title: "Botão de Desempenho",
      text: "O Modo Desempenho vem ativo por padrão. Você pode alternar para o Modo Visual caso queira ver as animações de fundo.",
    },
    {
      id: "tech-stack",
      title: "Tecnologia de Ponta",
      text: "Nosso sistema foi construído sob rigorosos padrões corporativos de segurança, garantindo um alto nível de integridade e proteção para os seus dados.",
    },
  ];

  // CORREÇÃO AQUI: Captação dinâmica do erro com atraso de montagem do DOM
  useEffect(() => {
    redefinirIndicadorRedirecionamento();
    const erroCacheado = sessionStorage.getItem("logout_reason");
    if (erroCacheado) {
      sessionStorage.removeItem("logout_reason");
      try {
        const payload = JSON.parse(erroCacheado);
        if (payload?.message) {
          setTimeout(
            () =>
              notificar(
                payload?.code === "SESSION_IDLE_TIMEOUT" ? "warning" : "error",
                payload.message,
              ),
            150,
          );
        } else if (payload?.code === "SESSION_REPLACED") {
          setTimeout(
            () => notificar("error", "Sessão encerrada noutro dispositivo."),
            150,
          );
        }
      } catch {}
    }
  }, []);

  useEffect(() => {
    if (user?.forceChangePassword) {
      setEstadoTela("TROCAR_SENHA");
      setCarregando(false);
    }
  }, [user]);

  useEffect(() => {
    const parametros = new URLSearchParams(localizacao.search);
    const token = parametros.get("token")?.trim() || "";
    if (localizacao.pathname !== "/recuperar-senha" || !token || user?.forceChangePassword) return;

    let ativo = true;
    setEstadoTela("RECUPERAR_REDEFINIR");
    setTokenRecuperacaoValido(null);
    setMensagemTokenRecuperacao("");
    setValidandoTokenRecuperacao(true);

    // Abrir o deep link apenas valida o token. A confirmação/consumo é uma
    // intenção explícita separada, conforme o contrato do backend.
    setTokenRecuperacao(token);
    setRecuperacaoConfirmada(false);
    ServicoAutenticacao.validarTokenRecuperacaoSenha(token)
      .then((resultado) => {
        if (!ativo) return;
        setTokenRecuperacaoValido(true);
        setMensagemTokenRecuperacao(
          resultado?.message || "Link válido. Confirme abaixo para gerar a senha temporária.",
        );
      })
      .catch((erro) => {
        if (!ativo) return;
        setTokenRecuperacaoValido(false);
        setMensagemTokenRecuperacao(erro?.message || "Este link é inválido, expirou ou já foi utilizado.");
      })
      .finally(() => {
        if (ativo) setValidandoTokenRecuperacao(false);
      });

    return () => { ativo = false; };
  }, [localizacao.pathname, localizacao.search, user?.forceChangePassword]);


  useEffect(() => {
    if (estadoTela !== "REGISTRO" || superintendencias.length) return;
    let ativo = true;
    setCarregandoSuperintendencias(true);
    ServicoAutenticacao.listarSuperintendenciasCadastro()
      .then((itens) => {
        if (ativo) setSuperintendencias(Array.isArray(itens) ? itens : []);
      })
      .catch((erro) => {
        if (ativo) notificar("error", erro?.message || "Não foi possível carregar os gestores estaduais.");
      })
      .finally(() => {
        if (ativo) setCarregandoSuperintendencias(false);
      });
    return () => {
      ativo = false;
    };
  }, [estadoTela, superintendencias.length]);

  useEffect(() => {
    if (estadoTela !== "REGISTRO" || !credenciais.superintendenciaId) {
      setGestoesLocais([]);
      setCarregandoGestoesLocais(false);
      return undefined;
    }

    const controlador = new AbortController();
    setCarregandoGestoesLocais(true);
    ServicoAutenticacao.listarGestoesLocaisCadastro(
      Number(credenciais.superintendenciaId),
      { signal: controlador.signal },
    )
      .then((itens) => setGestoesLocais(Array.isArray(itens) ? itens : []))
      .catch((erro) => {
        if (!erro?.canceled) {
          setGestoesLocais([]);
          notificar("error", erro?.message || "Não foi possível carregar as Gestões Locais.");
        }
      })
      .finally(() => {
        if (!controlador.signal.aborted) setCarregandoGestoesLocais(false);
      });
    return () => controlador.abort();
  }, [estadoTela, credenciais.superintendenciaId]);

  useEffect(() => {
    let intervalo = null;
    if (bloqueado && tempoBloqueio > 0) {
      intervalo = setInterval(
        () => setTempoBloqueio((estadoAnterior) => estadoAnterior - 1),
        1000,
      );
    } else {
      setBloqueado(false);
      if (intervalo) clearInterval(intervalo);
    }
    return () => {
      if (intervalo) clearInterval(intervalo);
    };
  }, [bloqueado, tempoBloqueio]);

  const alternarTela = useCallback(() => {
    if (estadoTela === "TROCAR_SENHA") return;
    setEstadoTela((estadoAnterior) =>
      estadoAnterior === "LOGIN" ? "REGISTRO" : "LOGIN",
    );
    setCredenciais({
      username: "",
      password: "",
      confirmPassword: "",
      email: "",
      matricula: "",
      totp: "",
      superintendenciaId: "",
      gestaoLocalId: "",
    });
    setGestoesLocais([]);
  }, [estadoTela]);

  const lidarMudancaMatricula = useCallback((evento) => {
    if (segundoFatorPendente) setSegundoFatorPendente(false);
    setCredenciais((estadoAnterior) => ({
      ...estadoAnterior,
      matricula: formatarMatricula(evento.target.value),
      totp: segundoFatorPendente ? "" : estadoAnterior.totp,
    }));
  }, [segundoFatorPendente]);

  const lidarMudancaCredenciais = useCallback((campo, valor) => {
    if (campo === "password" && segundoFatorPendente) setSegundoFatorPendente(false);
    setCredenciais((estadoAnterior) => ({
      ...estadoAnterior,
      [campo]: valor,
      ...(campo === "superintendenciaId" ? { gestaoLocalId: "" } : {}),
      ...(campo === "password" && segundoFatorPendente ? { totp: "" } : {}),
    }));
    if (campo === "superintendenciaId") setGestoesLocais([]);
  }, [segundoFatorPendente]);

  const reiniciarEtapaDeLogin = useCallback(() => {
    setSegundoFatorPendente(false);
    setCredenciais((estadoAnterior) => ({ ...estadoAnterior, totp: "" }));
  }, []);

  const lidarMudancaNovaSenha = useCallback((campo, valor) => {
    setNovaSenha((estadoAnterior) => ({ ...estadoAnterior, [campo]: valor }));
  }, []);

  const lidarSubmissaoPrincipal = async (evento) => {
    evento.preventDefault();
    if (carregando) return;

    if (estadoTela === "TROCAR_SENHA") {
      await submeterTrocaSenha();
      return;
    }

    setCarregando(true);
    try {
      if (estadoTela === "LOGIN") {
        if (!matriculaEstaCompleta(credenciais.matricula) || !credenciais.password) {
          notificar("error", "Informe uma matrícula válida de 8 dígitos e a senha.");
          setCarregando(false);
          return;
        }
        if (segundoFatorPendente && !/^\d{6}$/.test(credenciais.totp)) {
          notificar("error", "Informe o código de 6 dígitos do aplicativo autenticador.");
          setCarregando(false);
          return;
        }
        const resultado = await autenticar(
          credenciais.matricula,
          credenciais.password,
          credenciais.totp,
        );
        if (resultado?.requiresTwoFactor) {
          setSegundoFatorPendente(true);
          notificar("info", "Informe o código do seu aplicativo autenticador.");
        } else if (resultado?.error) {
          if (resultado.status === 429) {
            const segundosBloqueio = Number(resultado.lockout);
            if (Number.isFinite(segundosBloqueio) && segundosBloqueio > 0) {
              setTempoBloqueio(Math.ceil(segundosBloqueio));
              setBloqueado(true);
            } else {
              setTempoBloqueio(0);
              setBloqueado(false);
              notificar("warning", resultado.message || "Muitas tentativas. Aguarde e tente novamente.");
            }
          } else {
            notificar("error", resultado.message || "Credenciais inválidas.");
          }
        } else {
          setSegundoFatorPendente(false);
        }
      } else if (estadoTela === "REGISTRO") {
        if (!matriculaEstaCompleta(credenciais.matricula)) {
          notificar("error", "Informe uma matrícula válida de 8 dígitos.");
          setCarregando(false);
          return;
        }
        if (!credenciais.superintendenciaId) {
          notificar("error", "Selecione o seu gestor estadual.");
          setCarregando(false);
          return;
        }
        if (!credenciais.gestaoLocalId) {
          notificar("error", "Selecione a sua Gestão Local.");
          setCarregando(false);
          return;
        }
        if (
          !credenciais.email.toLowerCase().endsWith(CONFIG.APP_ALLOWED_DOMAIN)
        ) {
          notificar(
            "error",
            `Utilize apenas o email institucional ${CONFIG.APP_ALLOWED_DOMAIN}.`,
          );
          setCarregando(false);
          return;
        }
        await cadastrar(
          credenciais.username.trim(),
          credenciais.email.trim(),
          credenciais.matricula,
          Number(credenciais.superintendenciaId),
          Number(credenciais.gestaoLocalId),
        );
        notificar("success", "Solicitação enviada à Gestão Local selecionada!");
        setTimeout(() => setEstadoTela("LOGIN"), 2500);
      }
    } catch (erroDeRede) {
      notificar("error", erroDeRede?.message || "Ocorreu um erro na operação.");
    } finally {
      setCarregando(false);
    }
  };

  const submeterTrocaSenha = async () => {
    if (novaSenha.password !== novaSenha.confirm) {
      notificar("error", "As senhas não coincidem.");
      return;
    }
    if (novaSenha.password.length < 6) {
      notificar("error", "A senha deve ter no mínimo 6 caracteres.");
      return;
    }
    if (novaSenha.password.length > 72) {
      notificar("error", "A senha deve ter no máximo 72 caracteres.");
      return;
    }
    if (novaSenha.password === credenciais.password) {
      notificar("error", "A nova senha deve ser diferente da senha temporária atual.");
      return;
    }

    setCarregando(true);
    try {
      await ServicoAutenticacao.alterarSenhaInicial(novaSenha.password);
      notificar("success", "Senha alterada com sucesso! Acesso liberado.");
      const userAtualizado = await atualizarPerfil();
      if (userAtualizado && !userAtualizado.forceChangePassword) {
        navegar(rotaInicialPorPerfil(userAtualizado), { replace: true });
      } else {
        window.location.reload();
      }
    } catch (erro) {
      notificar("error", erro?.message || "Erro ao alterar a senha.");
    } finally {
      setCarregando(false);
    }
  };


  const abrirRecuperacaoSenha = useCallback(() => {
    setEstadoTela("RECUPERAR_SOLICITAR");
    setEmailRecuperacao("");
    setTokenRecuperacaoValido(null);
    setTokenRecuperacao("");
    setRecuperacaoConfirmada(false);
    setMensagemTokenRecuperacao("");
  }, []);

  const voltarAoLogin = useCallback(() => {
    setEstadoTela("LOGIN");
    setEmailRecuperacao("");
    setTokenRecuperacaoValido(null);
    setTokenRecuperacao("");
    setRecuperacaoConfirmada(false);
    setMensagemTokenRecuperacao("");
    navegar("/", { replace: true });
  }, [navegar]);

  const solicitarRecuperacaoSenha = useCallback(async (evento) => {
    evento.preventDefault();
    if (carregando) return;
    const email = emailRecuperacao.trim().toLowerCase();
    if (!email || !email.endsWith(CONFIG.APP_ALLOWED_DOMAIN)) {
      notificar("error", `Informe seu e-mail institucional ${CONFIG.APP_ALLOWED_DOMAIN}.`);
      return;
    }
    setCarregando(true);
    try {
      const resposta = await ServicoAutenticacao.solicitarRecuperacaoSenha(email);
      notificar("success", resposta?.message || resposta || "Se a conta estiver ativa, enviaremos as instruções por e-mail.");
      setEmailRecuperacao("");
    } catch (erro) {
      notificar("error", erro?.message || "Não foi possível solicitar a recuperação agora.");
    } finally {
      setCarregando(false);
    }
  }, [carregando, emailRecuperacao]);

  const confirmarRecuperacaoSenha = useCallback(async () => {
    if (!tokenRecuperacao || tokenRecuperacaoValido !== true || carregando || recuperacaoConfirmada) return;
    setCarregando(true);
    try {
      const resultado = await ServicoAutenticacao.confirmarRecuperacaoSenha(tokenRecuperacao);
      setRecuperacaoConfirmada(true);
      setMensagemTokenRecuperacao(
        resultado?.message || resultado || "Senha temporária enviada para seu e-mail.",
      );
    } catch (erro) {
      setMensagemTokenRecuperacao(erro?.message || "Não foi possível confirmar a recuperação agora.");
      notificar("error", erro?.message || "Não foi possível confirmar a recuperação agora.");
    } finally {
      setCarregando(false);
    }
  }, [carregando, recuperacaoConfirmada, tokenRecuperacao, tokenRecuperacaoValido]);

  const preencherUsuarioTeste = useCallback(({ matricula, senha }) => {
    setEstadoTela("LOGIN");
    setCredenciais((estadoAnterior) => ({
      ...estadoAnterior,
      matricula: formatarMatricula(matricula),
      password: senha,
    }));
    setMostrarModalTeste(false);
  }, []);

  const formatarTempo = (segundosTotais) => {
    const min = Math.floor(segundosTotais / 60)
      .toString()
      .padStart(2, "0");
    const seg = (segundosTotais % 60).toString().padStart(2, "0");
    return `${min}:${seg}`;
  };

  return (
    <div className={estilos.envoltorioGeral}>
      {!modoDesempenho && (
        <Suspense fallback={null}>
          <FundoAnimado />
        </Suspense>
      )}

      <ConteinerAvisos />
      <TourGuiado
        passos={passosTour}
        aberto={tourAberto}
        aoFechar={() => setTourAberto(false)}
      />

      <div className={estilos.containerBotoesTopo}>
        <button
          onClick={alternarModoDesempenho}
          className={`${estilos.botaoTour} ${modoDesempenho ? estilos.temaDesempenhoAtivo : estilos.temaDesempenhoInativo}`}
          data-tour="perf-toggle"
          title={
            modoDesempenho
              ? "Ativar Efeitos Visuais"
              : "Desativar efeitos para melhorar a velocidade"
          }
        >
          {modoDesempenho ? <Zap size={16} /> : <Sparkles size={16} />}
          {modoDesempenho ? "MODO DESEMPENHO" : "MODO VISUAL"}
        </button>

        <button
          onClick={() => setTourAberto(true)}
          className={estilos.botaoTour}
        >
          <Lightbulb size={16} className={estilos.iconeTour} /> GUIA RÁPIDO
        </button>
      </div>


      {mostrarModalTeste && (
        <ModalTeste
          aoFechar={() => setMostrarModalTeste(false)}
          aoPreencherUsuario={preencherUsuarioTeste}
        />
      )}


      <div className={estilos.cartaoPrincipalLogin}>
        <Homologacao />

        {bloqueado && (
          <div className={estilos.sobreposicaoBloqueio}>
            <AlertCircle size={56} className={estilos.iconeBloqueio} />
            <h2 className={estilos.tituloBloqueio}>Acesso Bloqueado</h2>
            <div className={estilos.cronometroBloqueio}>
              {formatarTempo(tempoBloqueio)}
            </div>
          </div>
        )}

        <div className={estilos.secaoCabecalhoLogin}>
          <div className={estilos.envoltorioLogotipo}>
            <img
              src={logoCorreios}
              alt={CONFIG.APP_COMPANY}
              className={estilos.imagemLogotipo}
            />
          </div>
          <h1 className={estilos.tituloAcesso}>
            {estadoTela === "LOGIN"
              ? ` ${CONFIG.APP_NAME || ""}`
              : estadoTela === "REGISTRO"
                ? "Solicitar Acesso"
                : estadoTela === "RECUPERAR_SOLICITAR"
                  ? "Recuperar Senha"
                  : estadoTela === "RECUPERAR_REDEFINIR"
                    ? "Confirmar Recuperação"
                    : "Definir Senha"}
          </h1>
          <p className={estilos.subtituloAcesso}>Acesso ao sistema</p>
        </div>

        {estadoTela === "RECUPERAR_SOLICITAR" || estadoTela === "RECUPERAR_REDEFINIR" ? (
          <FormularioRecuperacaoSenha
            modo={estadoTela === "RECUPERAR_SOLICITAR" ? "SOLICITAR" : "REDEFINIR"}
            email={emailRecuperacao}
            aoMudarEmail={setEmailRecuperacao}
            carregando={carregando}
            validandoToken={validandoTokenRecuperacao}
            tokenValido={tokenRecuperacaoValido}
            mensagemToken={mensagemTokenRecuperacao}
            confirmada={recuperacaoConfirmada}
            aoConfirmarToken={confirmarRecuperacaoSenha}
            modoDesempenho={modoDesempenho}
            aoSubmeterSolicitacao={solicitarRecuperacaoSenha}
            aoVoltarLogin={voltarAoLogin}
          />
        ) : (
          <FormularioAcesso
            estadoTela={estadoTela}
            credenciais={credenciais}
            aoMudarCredenciais={lidarMudancaCredenciais}
            aoMudarMatricula={lidarMudancaMatricula}
            novaSenha={novaSenha}
            aoMudarNovaSenha={lidarMudancaNovaSenha}
            carregando={carregando}
            bloqueado={bloqueado}
            modoDesempenho={modoDesempenho}
            superintendencias={superintendencias}
            carregandoSuperintendencias={carregandoSuperintendencias}
            gestoesLocais={gestoesLocais}
            carregandoGestoesLocais={carregandoGestoesLocais}
            segundoFatorPendente={segundoFatorPendente}
            aoReiniciarLogin={reiniciarEtapaDeLogin}
            aoRecuperarSenha={abrirRecuperacaoSenha}
            aoSubmeter={lidarSubmissaoPrincipal}
          />
        )}

        {["LOGIN", "REGISTRO"].includes(estadoTela) && (
          <div className={estilos.secaoRodapeCard} data-tour="login-switch">
            <p className={estilos.textoRodapeCard}>
              {estadoTela === "LOGIN"
                ? "Ainda não possui acesso?"
                : "Já tem cadastro?"}
            </p>
            <button
              type="button"
              onClick={alternarTela}
              disabled={bloqueado || carregando}
              className={estilos.botaoAlternarCard}
            >
              {estadoTela === "LOGIN"
                ? "Solicitar Acesso →"
                : "← Voltar ao Login"}
            </button>
          </div>
        )}
      </div>

      <div className={estilos.secaoTecnologias} data-tour="tech-stack">
        <div className={estilos.blocoTecnologia}>
          <p className={estilos.rotuloTecnologia}>
            <b>Tecnologias Utilizadas</b>
          </p>
          <IconesTecnologia />
        </div>
        <p className={estilos.textoCreditos}>
          Desenvolvido por {CONFIG.AUTHOR_NAME}{" "}
          <span className={estilos.matriculaCreditos}>
            {CONFIG.AUTHOR_MATRICULA}
          </span>
        </p>
      </div>
    </div>
  );
};
