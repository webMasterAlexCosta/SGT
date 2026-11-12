import React, { useState, useEffect, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { observarFalha } from '../../../../core/http/falhaObservada';
import { useAutenticacao } from "../../context.tsx";
import { redefinirIndicadorRedirecionamento, notificar } from "../../../../core/events.ts";
import { CONFIG } from "../../../../core/config.ts";
import { rotaInicialPorPerfil } from "../../../../core/security/roles.ts";
import { ServicoAutenticacao } from "../../../../services/auth/ServicoAutenticacao.ts";
import { formatarMatricula, matriculaEstaCompleta } from "../../../../core/formatters/matricula.ts";

export const useLogin = () => {
  const { autenticar, cadastrar, user, atualizarPerfil } = useAutenticacao();
  const navegar = useNavigate();
  const localizacao: import("react-router-dom").Location<unknown> = useLocation();

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
  const [superintendencias, setSuperintendencias] = useState<(import("../../../../contracts/java").AutenticacaoSuperintendenciaDTO[])>([]);
  const [carregandoSuperintendencias, setCarregandoSuperintendencias] = useState(false);
  const [gestoesLocais, setGestoesLocais] = useState<(import("../../../../contracts/java").AutenticacaoGestaoLocalDTO[])>([]);
  const [carregandoGestoesLocais, setCarregandoGestoesLocais] = useState(false);
  const [segundoFatorPendente, setSegundoFatorPendente] = useState(false);
  const [emailRecuperacao, setEmailRecuperacao] = useState("");
  const [tokenRecuperacaoValido, setTokenRecuperacaoValido] = useState<(true) | (false) | null>(null);
  const [tokenRecuperacao, setTokenRecuperacao] = useState("");
  const [recuperacaoConfirmada, setRecuperacaoConfirmada] = useState(false);
  const [mensagemTokenRecuperacao, setMensagemTokenRecuperacao] = useState("");
  const [validandoTokenRecuperacao, setValidandoTokenRecuperacao] = useState(false);


  // Apresenta a causa de encerramento guardada pelo fluxo de autenticação.
  useEffect(() => {
    redefinirIndicadorRedirecionamento();
    const erroCacheado = sessionStorage.getItem("logout_reason");
    if (erroCacheado) {
      sessionStorage.removeItem("logout_reason");
      try {
        const payload = observarFalha(JSON.parse(erroCacheado) as unknown);
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
      .catch((erroOriginal: unknown) => {
      const erro = observarFalha(erroOriginal);

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
      .catch((erroOriginal: unknown) => {
      const erro = observarFalha(erroOriginal);

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
      .catch((erroOriginal: unknown) => {
      const erro = observarFalha(erroOriginal);

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
    let intervalo: ReturnType<typeof setInterval> | null = null;
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

  const lidarMudancaMatricula = useCallback((evento: React.ChangeEvent<HTMLInputElement>) => {
    if (segundoFatorPendente) setSegundoFatorPendente(false);
    setCredenciais((estadoAnterior) => ({
      ...estadoAnterior,
      matricula: formatarMatricula(evento.target.value),
      totp: segundoFatorPendente ? "" : estadoAnterior.totp,
    }));
  }, [segundoFatorPendente]);

  const lidarMudancaCredenciais = useCallback((campo: keyof typeof credenciais, valor: string) => {
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

  const lidarMudancaNovaSenha = useCallback((campo: keyof typeof novaSenha, valor: string) => {
    setNovaSenha((estadoAnterior) => ({ ...estadoAnterior, [campo]: valor }));
  }, []);

  const lidarSubmissaoPrincipal = async (evento: React.FormEvent<HTMLFormElement>) => {
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
        if (resultado?.canceled) return;
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
    } catch (erroDeRedeOriginal) {
      const erroDeRede = observarFalha(erroDeRedeOriginal);

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
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

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

  const solicitarRecuperacaoSenha = useCallback(async (evento: React.FormEvent<HTMLFormElement>) => {
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
      notificar("success", (typeof resposta === "string" ? resposta : resposta?.message) || "Se a conta estiver ativa, enviaremos as instruções por e-mail.");
      setEmailRecuperacao("");
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

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
        resultado?.message || "Senha temporária enviada para seu e-mail.",
      );
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      setMensagemTokenRecuperacao(erro?.message || "Não foi possível confirmar a recuperação agora.");
      notificar("error", erro?.message || "Não foi possível confirmar a recuperação agora.");
    } finally {
      setCarregando(false);
    }
  }, [carregando, recuperacaoConfirmada, tokenRecuperacao, tokenRecuperacaoValido]);

  const preencherUsuarioTeste = useCallback(({ matricula, senha }: { matricula: string; senha: string }) => {
    setEstadoTela("LOGIN");
    setCredenciais((estadoAnterior) => ({
      ...estadoAnterior,
      matricula: formatarMatricula(matricula),
      password: senha,
    }));
    setMostrarModalTeste(false);
  }, []);

  const formatarTempo = (segundosTotais: (number)) => {
    const min = Math.floor(segundosTotais / 60)
      .toString()
      .padStart(2, "0");
    const seg = (segundosTotais % 60).toString().padStart(2, "0");
    return `${min}:${seg}`;
  };

  return {
    estadoTela,
    credenciais,
    novaSenha,
    carregando,
    bloqueado,
    tempoBloqueio,
    mostrarModalTeste,
    setMostrarModalTeste,
    tourAberto,
    setTourAberto,
    superintendencias,
    carregandoSuperintendencias,
    gestoesLocais,
    carregandoGestoesLocais,
    segundoFatorPendente,
    emailRecuperacao,
    setEmailRecuperacao,
    tokenRecuperacaoValido,
    recuperacaoConfirmada,
    mensagemTokenRecuperacao,
    validandoTokenRecuperacao,
    confirmarRecuperacaoSenha,
    solicitarRecuperacaoSenha,
    voltarAoLogin,
    lidarMudancaCredenciais,
    lidarMudancaMatricula,
    lidarMudancaNovaSenha,
    reiniciarEtapaDeLogin,
    abrirRecuperacaoSenha,
    lidarSubmissaoPrincipal,
    alternarTela,
    preencherUsuarioTeste,
    formatarTempo
  };
};
