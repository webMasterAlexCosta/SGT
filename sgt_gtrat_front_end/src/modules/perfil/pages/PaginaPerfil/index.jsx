import React, { useState, useRef, useEffect, useCallback } from "react";
import { SENHA_MINIMA_UI, SENHA_MAXIMA_UI } from "../../../../core/security/politicaSenha.js";
import { useLocation } from "react-router-dom";
import {
  UserCircle,
  Shield,
  Key,
  Mail,
  Building2,
  Camera,
  Loader2,
  Upload,
  X,
  ScanFace,
  BadgeInfo,
  Calendar,
  Clock,
  MapPin,
  ZoomIn,
  Move,
  Globe,
  MonitorSmartphone,
  Timer,
} from "lucide-react";

import { useAutenticacao } from "../../../auth/context";
import { ServicoPerfil } from "../../../../services/perfil/ServicoPerfil";
import { Botao, Cartao, CampoEntrada, JanelaModal } from "../../../../core/ui/EnvolucroDica";
import { ModalSegurancaSegundoFator } from "../../components/ModalSegurancaSegundoFator";
import { notificar } from "../../../../core/events";
import { CONFIG } from "../../../../core/config";
import { rotuloPerfil } from "../../../../core/security/roles";
import estilos from "./index.module.css";

export const PaginaPerfil = () => {
  const { user, atualizarPerfil, atualizarInformacoesSessao, sessionExpiresAtEpochMs } = useAutenticacao();
  const localizacao = useLocation();

  const [modalSenhaAberto, setModalSenhaAberto] = useState(false);
  const [modalTwoFactorAberto, setModalTwoFactorAberto] = useState(false);
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(Boolean(user?.twoFactorEnabled));

  useEffect(() => {
    const parametros = new URLSearchParams(localizacao.search);
    if (parametros.get("security") === "2fa") {
      setModalTwoFactorAberto(true);
    }
  }, [localizacao.search]);

  useEffect(() => {
    if (!modalSenhaAberto) return undefined;

    const controlador = new AbortController();
    ServicoPerfil.buscarStatusSegundoFator({ signal: controlador.signal })
      .then((status) => setTwoFactorEnabled(Boolean(status?.enabled)))
      .catch(() => {});

    return () => controlador.abort();
  }, [modalSenhaAberto]);
  const [carregando, setCarregando] = useState(false);
  const [dadosSenha, setDadosSenha] = useState({
    current: "",
    newPass: "",
    confirmPass: "",
    token: "",
  });

  const [detalhesSessao, setDetalhesSessao] = useState(null);

  const [enviandoFoto, setEnviandoFoto] = useState(false);
  const referenciaArquivoInput = useRef(null);
  const chaveCacheFotoPerfil = `${CONFIG.APP_NAME.toLowerCase()}_foto_url`;
  const [fotoLocal, setFotoLocal] = useState(
    localStorage.getItem(chaveCacheFotoPerfil) || user?.fotoUrl,
  );
  const [modalVisualizarFotoAberto, setModalVisualizarFotoAberto] =
    useState(false);

  const [modalRecorteAberto, setModalRecorteAberto] = useState(false);
  const [imagemParaRecortar, setImagemParaRecortar] = useState(null);
  const [zoomRecorte, setZoomRecorte] = useState(1);
  const [posicaoRecorte, setPosicaoRecorte] = useState({ x: 0, y: 0 });
  const [estaArrastando, setEstaArrastando] = useState(false);
  const inicioArraste = useRef({ x: 0, y: 0 });
  const referenciaCanvasRecorte = useRef(null);

  const [cameraAberta, setCameraAberta] = useState(false);
  const [estaAlinhado, setEstaAlinhado] = useState(false);
  const [progressoScan, setProgressoScan] = useState(0);
  const [dicaScan, setDicaScan] = useState("Iniciando câmera...");
  const [modelosCarregados, setModelosCarregados] = useState(false);

  const referenciaVideo = useRef(null);
  const referenciaFluxoCamera = useRef(null);
  const referenciaAnimacaoQuadro = useRef(null);

  const lidarComMudancaSenha = useCallback((evento) => {
    const { name } = evento.target;
    const value = name === "token"
      ? evento.target.value.replace(/\D/g, "").slice(0, 6)
      : evento.target.value;
    setDadosSenha((estadoAnterior) => ({
      ...estadoAnterior,
      [name]: value,
    }));
  }, []);

  const urlBase = CONFIG.API_URL.replace(/\/api\/?$/, "");
  const urlFotoCompleta = fotoLocal ? `${urlBase}${fotoLocal}` : null;

  const analisarAgenteUsuario = useCallback((agente) => {
    if (!agente) return "Desconhecido";
    if (agente.includes("Edg")) return "Microsoft Edge";
    if (agente.includes("Chrome")) return "Google Chrome";
    if (agente.includes("Firefox")) return "Mozilla Firefox";
    if (agente.includes("Safari")) return "Apple Safari";
    return "Navegador Web";
  }, []);

  useEffect(() => {
    atualizarInformacoesSessao({ touch: false, skipLoading: true, silentError: true })
      .then((informacoes) => {
        if (informacoes?.session) setDetalhesSessao(informacoes.session);
      })
      .catch(() => {});
  }, [atualizarInformacoesSessao]);

  const lidarComSelecaoArquivo = useCallback((evento) => {
    const arquivo = evento.target.files?.[0];
    if (arquivo) {
      const url = URL.createObjectURL(arquivo);
      setImagemParaRecortar(url);
      setZoomRecorte(1);
      setPosicaoRecorte({ x: 0, y: 0 });
      setModalRecorteAberto(true);
      if (referenciaArquivoInput.current)
        referenciaArquivoInput.current.value = "";
    }
  }, []);

  const desenharPreviaRecorte = useCallback(() => {
    if (!imagemParaRecortar || !referenciaCanvasRecorte.current) return;
    const canvas = referenciaCanvasRecorte.current;
    const contexto = canvas.getContext("2d");
    const imagemMapeada = new Image();
    imagemMapeada.src = imagemParaRecortar;

    imagemMapeada.onload = () => {
      contexto.clearRect(0, 0, canvas.width, canvas.height);
      const escalaBase = Math.max(
        canvas.width / imagemMapeada.width,
        canvas.height / imagemMapeada.height,
      );
      const escalaFinal = escalaBase * zoomRecorte;
      const larguraDesejada = imagemMapeada.width * escalaFinal;
      const alturaDesejada = imagemMapeada.height * escalaFinal;
      const posicaoX = (canvas.width - larguraDesejada) / 2 + posicaoRecorte.x;
      const posicaoY = (canvas.height - alturaDesejada) / 2 + posicaoRecorte.y;
      contexto.drawImage(
        imagemMapeada,
        posicaoX,
        posicaoY,
        larguraDesejada,
        alturaDesejada,
      );
      contexto.fillStyle = "rgba(0, 0, 0, 0.6)";
      contexto.beginPath();
      contexto.rect(0, 0, canvas.width, canvas.height);
      contexto.arc(
        canvas.width / 2,
        canvas.height / 2,
        canvas.width / 2,
        0,
        Math.PI * 2,
        true,
      );
      contexto.fill();
      contexto.strokeStyle = "#facc15";
      contexto.lineWidth = 3;
      contexto.beginPath();
      contexto.arc(
        canvas.width / 2,
        canvas.height / 2,
        canvas.width / 2 - 2,
        0,
        Math.PI * 2,
      );
      contexto.stroke();
    };
  }, [imagemParaRecortar, zoomRecorte, posicaoRecorte]);

  useEffect(() => {
    if (modalRecorteAberto) desenharPreviaRecorte();
  }, [modalRecorteAberto, desenharPreviaRecorte]);

  const lidarComInicioArrasteImagem = (evento) => {
    setEstaArrastando(true);
    inicioArraste.current = {
      x: evento.clientX - posicaoRecorte.x,
      y: evento.clientY - posicaoRecorte.y,
    };
  };

  const lidarComArrasteImagem = (evento) => {
    if (!estaArrastando) return;
    setPosicaoRecorte({
      x: evento.clientX - inicioArraste.current.x,
      y: evento.clientY - inicioArraste.current.y,
    });
  };

  const lidarComFimArrasteImagem = () => setEstaArrastando(false);

  const confirmarRecorteEEnviar = useCallback(() => {
    setEnviandoFoto(true);
    setModalRecorteAberto(false);

    const canvasFinal = document.createElement("canvas");
    canvasFinal.width = 300;
    canvasFinal.height = 300;
    const contextoFinal = canvasFinal.getContext("2d");

    const imagemMapeada = new Image();
    imagemMapeada.src = imagemParaRecortar;
    imagemMapeada.onload = () => {
      const escalaBase = Math.max(
        300 / imagemMapeada.width,
        300 / imagemMapeada.height,
      );
      const escalaFinal = escalaBase * zoomRecorte;
      const larguraDesejada = imagemMapeada.width * escalaFinal;
      const alturaDesejada = imagemMapeada.height * escalaFinal;
      const posicaoX = (300 - larguraDesejada) / 2 + posicaoRecorte.x;
      const posicaoY = (300 - alturaDesejada) / 2 + posicaoRecorte.y;

      contextoFinal.drawImage(
        imagemMapeada,
        posicaoX,
        posicaoY,
        larguraDesejada,
        alturaDesejada,
      );

      canvasFinal.toBlob(
        async (blob) => {
          if (!blob) {
            setEnviandoFoto(false);
            return;
          }
          const arquivoBlob = new File(
            [blob],
            `avatar_recortado_${Date.now()}.jpg`,
            { type: "image/jpeg" },
          );
          try {
            const maximoBytesPermitido =
              CONFIG.UPLOAD_MAX_SIZE_MB * 1024 * 1024;
            if (arquivoBlob.size > maximoBytesPermitido) {
              notificar(
                "error",
                `A imagem deve ter no máximo ${CONFIG.UPLOAD_MAX_SIZE_MB}MB.`,
              );
              setEnviandoFoto(false);
              return;
            }
            const resposta = await ServicoPerfil.enviarAvatar(arquivoBlob);
            if (resposta?.fotoUrl) {
              setFotoLocal(resposta.fotoUrl);
              localStorage.setItem(chaveCacheFotoPerfil, resposta.fotoUrl);
            }
            await atualizarPerfil();
            notificar("success", "Foto de perfil atualizada com sucesso!");
          } catch (erro) {
            notificar("error", "Falha ao atualizar a foto.");
          } finally {
            setEnviandoFoto(false);
          }
        },
        "image/jpeg",
        0.95,
      );
    };
  }, [imagemParaRecortar, zoomRecorte, posicaoRecorte, atualizarPerfil, chaveCacheFotoPerfil]);

  const pararCamera = useCallback(() => {
    if (referenciaAnimacaoQuadro.current)
      cancelAnimationFrame(referenciaAnimacaoQuadro.current);
    if (referenciaFluxoCamera.current) {
      referenciaFluxoCamera.current
        .getTracks()
        .forEach((track) => track.stop());
      referenciaFluxoCamera.current = null;
    }
    if (referenciaVideo.current) referenciaVideo.current.srcObject = null;
    setCameraAberta(false);
    setEstaAlinhado(false);
    setProgressoScan(0);
  }, []);

  useEffect(() => {
    return () => pararCamera();
  }, [pararCamera]);

  const capturarFoto = useCallback(() => {
    if (referenciaAnimacaoQuadro.current)
      cancelAnimationFrame(referenciaAnimacaoQuadro.current);
    if (!referenciaVideo.current) return;
    const canvas = document.createElement("canvas");
    canvas.width = referenciaVideo.current.videoWidth;
    canvas.height = referenciaVideo.current.videoHeight;
    const contexto = canvas.getContext("2d");
    contexto.translate(canvas.width, 0);
    contexto.scale(-1, 1);
    contexto.drawImage(
      referenciaVideo.current,
      0,
      0,
      canvas.width,
      canvas.height,
    );
    const dataUrlDaImagem = canvas.toDataURL("image/jpeg", 1.0);
    setImagemParaRecortar(dataUrlDaImagem);
    setZoomRecorte(1);
    setPosicaoRecorte({ x: 0, y: 0 });
    setModalRecorteAberto(true);
    pararCamera();
  }, [pararCamera]);

  // LAZY LOAD DO FACE-API
  const carregarModelosIA = useCallback(async () => {
    try {
      setDicaScan("A carregar modelos visuais...");
      const faceapi = await import("@vladmandic/face-api");
      await faceapi.nets.tinyFaceDetector.loadFromUri("/models");
      setModelosCarregados(true);
      setDicaScan("Modelos carregados! Enquadre o rosto no círculo.");
    } catch (erro) {
      console.error("Erro na IA:", erro);
      setDicaScan(
        "Falha na IA. Verifique se os modelos estão na pasta public/models.",
      );
    }
  }, []);

  // ===== INICIALIZAÇÃO DA CÂMERA CORRIGIDA =====
  const iniciarCamera = useCallback(async () => {
    try {
      setCameraAberta(true); // Abre o modal primeiro para a UI aparecer
      setDicaScan("Conectando à câmera...");
      
      let fluxoCamera;
      
      try {
        // TENTATIVA 1: Configuração ideal
        fluxoCamera = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: "user",
            width: { ideal: 640 },
            height: { ideal: 480 },
          },
        });
      } catch (erroRestricao) {
        console.warn("Restrições ideais não suportadas, tentando fallback...", erroRestricao);
        // TENTATIVA 2: Fallback genérico para qualquer câmera (resolve o erro)
        fluxoCamera = await navigator.mediaDevices.getUserMedia({ video: true });
      }

      referenciaFluxoCamera.current = fluxoCamera;
      setEstaAlinhado(false);
      setProgressoScan(0);
      setDicaScan("Carregando vídeo...");
      
      // Pequeno timeout para garantir que a tag <video> já foi renderizada no DOM
      setTimeout(() => {
        if (referenciaVideo.current) {
          referenciaVideo.current.srcObject = fluxoCamera;
        }
      }, 150);

      if (!modelosCarregados) {
          carregarModelosIA();
      } else {
          setDicaScan("Enquadre o rosto no círculo");
      }
      
    } catch (erro) {
      console.error("Erro final ao acessar a câmera:", erro);
      setCameraAberta(false); // Fecha o modal
      
      // Tratamento claro para o usuário
      if (erro.name === 'NotFoundError' || erro.message.includes('Requested device not found')) {
          notificar('error', 'Nenhuma câmera detectada no computador. Verifique o hardware.');
      } else if (erro.name === 'NotAllowedError') {
          notificar('error', 'Acesso negado à câmera. Verifique as permissões do navegador.');
      } else if (erro.name === 'NotReadableError') {
          notificar('error', 'Sua câmera já está em uso por outro programa (Zoom, Teams, etc).');
      } else {
          notificar('error', `Falha ao acessar a câmera: ${erro.message || erro.name}`);
      }
    }
  }, [modelosCarregados, carregarModelosIA]);

  useEffect(() => {
    if (!cameraAberta || !referenciaVideo.current || !modelosCarregados) return;
    let alinhadoDesde = 0;
    let faceapiModulo = null;

    const verificarRosto = async () => {
      if (
        !cameraAberta ||
        !referenciaVideo.current ||
        referenciaVideo.current.readyState < 2
      ) {
        referenciaAnimacaoQuadro.current =
          requestAnimationFrame(verificarRosto);
        return;
      }
      try {
        if (!faceapiModulo) {
          faceapiModulo = await import("@vladmandic/face-api");
        }

        const deteccao = await faceapiModulo.detectSingleFace(
          referenciaVideo.current,
          new faceapiModulo.TinyFaceDetectorOptions({
            inputSize: 224,
            scoreThreshold: 0.5,
          }),
        );

        if (deteccao) {
          const rostoDetectado = deteccao.box;
          const larguraVideo = referenciaVideo.current.videoWidth;
          const alturaVideo = referenciaVideo.current.videoHeight;
          const dimensaoMinima = Math.min(larguraVideo, alturaVideo);
          const centroX = larguraVideo / 2;
          const centroY = alturaVideo / 2;
          const rostoX = rostoDetectado.x + rostoDetectado.width / 2;
          const rostoY = rostoDetectado.y + rostoDetectado.height / 2;

          const centralizadoX =
            Math.abs(rostoX - centroX) < dimensaoMinima * 0.15;
          const centralizadoY =
            Math.abs(rostoY - centroY) < dimensaoMinima * 0.15;
          const temTamanhoSuficiente =
            rostoDetectado.height > alturaVideo * 0.35;
          const naoETaoGrande = rostoDetectado.height < alturaVideo * 0.7;

          if (
            centralizadoX &&
            centralizadoY &&
            temTamanhoSuficiente &&
            naoETaoGrande
          ) {
            setEstaAlinhado(true);
            setDicaScan("Perfeito! Segure...");
            if (alinhadoDesde === 0) alinhadoDesde = Date.now();
            const progressoAtual = Math.min(
              ((Date.now() - alinhadoDesde) / 2000) * 100,
              100,
            );
            setProgressoScan(progressoAtual);
            if (progressoAtual >= 100) {
              capturarFoto();
              return;
            }
          } else {
            setEstaAlinhado(false);
            setProgressoScan(0);
            alinhadoDesde = 0;
            if (!temTamanhoSuficiente) setDicaScan("Aproxime-se mais");
            else if (!naoETaoGrande) setDicaScan("Afaste-se um pouco");
            else setDicaScan("Centralize o seu rosto");
          }
        } else {
          setEstaAlinhado(false);
          setProgressoScan(0);
          alinhadoDesde = 0;
          setDicaScan("Nenhum rosto detectado");
        }
      } catch (e) {}
      referenciaAnimacaoQuadro.current = requestAnimationFrame(verificarRosto);
    };

    referenciaAnimacaoQuadro.current = requestAnimationFrame(verificarRosto);
    return () => {
      if (referenciaAnimacaoQuadro.current)
        cancelAnimationFrame(referenciaAnimacaoQuadro.current);
    };
  }, [cameraAberta, modelosCarregados, capturarFoto]);

  const lidarComEnvioSenha = useCallback(
    async (evento) => {
      evento.preventDefault();
      if (!dadosSenha.current) {
        notificar("error", "Informe a senha atual.");
        return;
      }
      if (dadosSenha.newPass.length < SENHA_MINIMA_UI) {
        notificar("error", `A nova senha deve ter no mínimo ${SENHA_MINIMA_UI} caracteres neste ambiente; o servidor valida a política final.`);
        return;
      }
      if (dadosSenha.newPass.length > SENHA_MAXIMA_UI) {
        notificar("error", "A nova senha deve ter no máximo 72 caracteres.");
        return;
      }
      if (dadosSenha.newPass === dadosSenha.current) {
        notificar("error", "A nova senha deve ser diferente da senha atual.");
        return;
      }
      if (dadosSenha.newPass !== dadosSenha.confirmPass) {
        notificar("error", "A nova senha e a confirmação não coincidem.");
        return;
      }
      if (twoFactorEnabled && !/^\d{6}$/.test(dadosSenha.token)) {
        notificar("error", "O código deve conter exatamente 6 dígitos.");
        return;
      }
      setCarregando(true);
      try {
        const resultado = await ServicoPerfil.alterarSenha(
          dadosSenha.current,
          dadosSenha.newPass,
          twoFactorEnabled ? dadosSenha.token : undefined,
        );
        if (resultado) {
          setDadosSenha({
            current: "",
            newPass: "",
            confirmPass: "",
            token: "",
          });
          setModalSenhaAberto(false);
          notificar("success", resultado?.message || "Senha alterada com sucesso.");

          // O backend preserva a sessão após a troca de senha. Revalide as
          // fontes autoritativas sem criar logout artificial no frontend.
          await atualizarPerfil();
          await atualizarInformacoesSessao({ touch: false, silentError: true });
        }
      } catch (e) {
      } finally {
        setCarregando(false);
      }
    },
    [dadosSenha, atualizarPerfil, atualizarInformacoesSessao, twoFactorEnabled],
  );

  if (!user) return null;

  return (
    <div className={estilos.containerPagina}>
      <h1 className={estilos.tituloPagina}>Meu Perfil</h1>

      <div className={estilos.gradeContainer}>
        <div className={estilos.colunaEsquerda}>
          <Cartao className={estilos.conteudoCartaoPerfil}>
            <div className={estilos.envoltorioAvatar}>
              <div
                className={`${estilos.circuloAvatar} ${urlFotoCompleta ? estilos.circuloAvatarClicavel : ""}`}
                onClick={() => {
                  if (urlFotoCompleta) setModalVisualizarFotoAberto(true);
                }}
                title={urlFotoCompleta ? "Clique para ampliar" : ""}
              >
                {enviandoFoto ? (
                  <Loader2 size={40} className={estilos.animarGiro} />
                ) : urlFotoCompleta ? (
                  <img
                    src={urlFotoCompleta}
                    alt="Perfil"
                    className={estilos.imagemAvatar}
                  />
                ) : (
                  <UserCircle size={100} />
                )}
              </div>

              <div className={estilos.botoesAcao}>
                <button
                  onClick={() => referenciaArquivoInput.current?.click()}
                  disabled={enviandoFoto}
                  className={estilos.botaoUpload}
                  title="Fazer Upload de Foto"
                  type="button"
                >
                  <Upload size={16} />
                </button>
                <button
                  onClick={iniciarCamera}
                  disabled={enviandoFoto}
                  className={estilos.botaoCamera}
                  title="Tirar Foto"
                  type="button"
                >
                  <Camera size={16} />
                </button>
              </div>
              <input
                type="file"
                ref={referenciaArquivoInput}
                onChange={lidarComSelecaoArquivo}
                accept="image/png, image/jpeg, image/webp"
                aria-label="Selecionar foto de perfil"
                className={estilos.oculto}
              />
            </div>

            <h2 className={estilos.nomeUsuario}>{user?.username || "—"}</h2>
            <div className={estilos.envoltorioPapeis}>
              <span className={estilos.emblemaPapel}>
                {rotuloPerfil(user)}
              </span>
              {user?.isRetroactiveUnlocked && (
                <span className={estilos.emblemaRetroativo}>
                  Retroativo Liberado
                </span>
              )}
            </div>

            <div className={estilos.envoltorioBotaoSeguranca}>
              <button
                onClick={() => setModalTwoFactorAberto(true)}
                className={`${estilos.botaoSeguranca} ${estilos.botaoProtecaoAvancada}`}
                type="button"
              >
                <Shield size={16} /> Proteção Avançada (2FA)
              </button>
              <button
                onClick={() => setModalSenhaAberto(true)}
                className={estilos.botaoSeguranca}
                type="button"
              >
                <Key size={16} /> Alterar Senha
              </button>
            </div>
          </Cartao>
        </div>

        <div className={estilos.colunaDireita}>
          <div className={estilos.gradeCartoesDireita}>
            <Cartao title="Informações Pessoais" className={estilos.alturaTotal}>
              <div className={estilos.containerDetalhes}>
                <div
                  className={`${estilos.itemDetalhe} ${estilos.itemDetalheComBorda}`}
                >
                  <Mail size={18} className={estilos.iconeDetalhe} />
                  <div className={estilos.grupoTextoDetalhe}>
                    <span className={estilos.rotuloDetalhe}>
                      Email Corporativo
                    </span>
                    <span className={estilos.valorDetalhe}>
                      {user?.email || "Não informado"}
                    </span>
                  </div>
                </div>
                <div
                  className={`${estilos.itemDetalhe} ${estilos.itemDetalheComBorda}`}
                >
                  <BadgeInfo size={18} className={estilos.iconeDetalhe} />
                  <div className={estilos.grupoTextoDetalhe}>
                    <span className={estilos.rotuloDetalhe}>Matrícula</span>
                    <span className={estilos.valorDetalhe}>
                      {user?.matricula || "Não informada"}
                    </span>
                  </div>
                </div>
                <div className={estilos.itemDetalhe}>
                  <Calendar size={18} className={estilos.iconeDetalhe} />
                  <div className={estilos.grupoTextoDetalhe}>
                    <span className={estilos.rotuloDetalhe}>
                      Data de Inclusão
                    </span>
                    <span className={estilos.valorDetalhe}>
                      {user?.createdAt
                        ? new Date(user.createdAt).toLocaleDateString("pt-BR")
                        : "Desconhecido"}
                    </span>
                  </div>
                </div>
              </div>
            </Cartao>

            <Cartao title="Vínculo Funcional" className={estilos.alturaTotal}>
              <div className={estilos.containerDetalhes}>
                <div
                  className={`${estilos.itemDetalhe} ${estilos.itemDetalheComBorda}`}
                >
                  <Building2 size={18} className={estilos.iconeDetalhe} />
                  <div className={estilos.grupoTextoDetalhe}>
                    <span className={estilos.rotuloDetalhe}>
                      Lotação Principal
                    </span>
                    <span className={estilos.valorDetalhe}>
                      {user?.lotacao || "Não definida"}
                    </span>
                  </div>
                </div>
                <div className={estilos.itemDetalhe}>
                  <Clock size={18} className={estilos.iconeDetalhe} />
                  <div className={estilos.grupoTextoDetalhe}>
                    <span className={estilos.rotuloDetalhe}>
                      Turno / Jornada
                    </span>
                    <span className={estilos.valorDetalhe}>
                      {user?.turno || "Não definido"}
                    </span>
                  </div>
                </div>
              </div>

              {user?.vinculosGestao?.length > 0 && (
                <div className={estilos.secaoEscopoGestao}>
                  <span className={estilos.tituloEscopoGestao}>
                    Escopo de Gestão Autorizado
                  </span>
                  <div className={estilos.listaEscopos}>
                    {user.vinculosGestao.map((vinculo, indiceLista) => (
                      <span key={indiceLista} className={estilos.emblemaEscopo}>
                        <MapPin size={10} className={estilos.iconeEscopo} />{" "}
                        {vinculo.lotacaoNome} • {vinculo.jornadaNome}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </Cartao>
          </div>

          <Cartao title="Segurança e Sessão Ativa">
            <div className={estilos.containerDetalhes}>
              <div
                className={`${estilos.itemDetalhe} ${estilos.itemDetalheComBorda}`}
              >
                <Globe size={18} className={estilos.iconeDetalhe} />
                <div className={estilos.grupoTextoDetalhe}>
                  <span className={estilos.rotuloDetalhe}>
                    Endereço IP Localizado
                  </span>
                  <span className={estilos.valorDetalhe}>
                    {detalhesSessao?.loginIp || "Desconhecido"}
                  </span>
                </div>
              </div>
              <div
                className={`${estilos.itemDetalhe} ${estilos.itemDetalheComBorda}`}
              >
                <MonitorSmartphone size={18} className={estilos.iconeDetalhe} />
                <div className={estilos.grupoTextoDetalhe}>
                  <span className={estilos.rotuloDetalhe}>
                    Dispositivo / Navegador
                  </span>
                  <span
                    className={estilos.valorDetalhe}
                    title={detalhesSessao?.userAgent}
                  >
                    {analisarAgenteUsuario(detalhesSessao?.userAgent)}
                  </span>
                </div>
              </div>
              <div className={estilos.itemDetalhe}>
                <Timer size={18} className={estilos.iconeDetalhe} />
                <div className={estilos.grupoTextoDetalhe}>
                  <span className={estilos.rotuloDetalhe}>
                    Expiração da Autenticação
                  </span>
                  <span className={estilos.valorDetalhe}>
                    {sessionExpiresAtEpochMs
                      ? new Date(
                          Number(sessionExpiresAtEpochMs),
                        ).toLocaleTimeString("pt-BR")
                      : "N/A"}
                  </span>
                </div>
              </div>
            </div>
          </Cartao>
        </div>
      </div>

      {modalVisualizarFotoAberto && urlFotoCompleta && (
        <JanelaModal
          isOpen={modalVisualizarFotoAberto}
          onClose={() => setModalVisualizarFotoAberto(false)}
          title="Foto de Perfil"
          maxWidth="max-w-lg"
        >
          <div className={estilos.envoltorioImagemAmpliada}>
            <img
              src={urlFotoCompleta}
              alt="Perfil Ampliado"
              className={estilos.imagemAmpliada}
            />
          </div>
          <div className={estilos.rodapeModalGeral}>
            <Botao
              variant="secondary"
              onClick={() => setModalVisualizarFotoAberto(false)}
            >
              Fechar
            </Botao>
          </div>
        </JanelaModal>
      )}

      {modalRecorteAberto && imagemParaRecortar && (
        <JanelaModal
          isOpen={modalRecorteAberto}
          onClose={() => setModalRecorteAberto(false)}
          title="Ajustar Foto"
          maxWidth="max-w-md"
        >
          <div className={estilos.conteudoModalRecorte}>
            <p className={estilos.textoInstrucaoRecorte}>
              <b>Clique e arraste</b> a imagem para centralizar o seu rosto
              dentro do círculo e utilize a barra para ajustar o zoom.
            </p>
            <div
              className={estilos.areaRecorte}
              onMouseDown={lidarComInicioArrasteImagem}
              onMouseMove={lidarComArrasteImagem}
              onMouseUp={lidarComFimArrasteImagem}
              onMouseLeave={lidarComFimArrasteImagem}
            >
              <canvas
                ref={referenciaCanvasRecorte}
                width={300}
                height={300}
                className={estilos.blocoCanvas}
              />
            </div>
            <div className={estilos.controlesZoom}>
              <Move
                size={16}
                className={`${estilos.iconeDeslizante} ${estilos.iconeDeslizanteCinza}`}
              />
              <input
                type="range"
                min="1"
                max="4"
                step="0.05"
                value={zoomRecorte}
                onChange={(eventoIntervalo) =>
                  setZoomRecorte(parseFloat(eventoIntervalo.target.value))
                }
                className={estilos.deslizanteZoom}
                aria-label="Zoom da foto de perfil"
              />
              <ZoomIn
                size={18}
                className={`${estilos.iconeDeslizante} ${estilos.iconeDeslizanteAzul}`}
              />
            </div>
            <div className={estilos.rodapeModalRecorte}>
              <Botao
                variant="secondary"
                onClick={() => setModalRecorteAberto(false)}
              >
                Cancelar
              </Botao>
              <Botao onClick={confirmarRecorteEEnviar}>Salvar Foto</Botao>
            </div>
          </div>
        </JanelaModal>
      )}

      <ModalSegurancaSegundoFator
        isOpen={modalTwoFactorAberto}
        onClose={() => setModalTwoFactorAberto(false)}
        onStatusChange={async (ativo) => {
          setTwoFactorEnabled(ativo);
          await atualizarPerfil();
          await atualizarInformacoesSessao({ touch: false, silentError: true });
        }}
      />

      {modalSenhaAberto && (
        <JanelaModal
          isOpen={modalSenhaAberto}
          onClose={() => setModalSenhaAberto(false)}
          title="Alterar Senha de Acesso"
          maxWidth="max-w-lg"
        >
          <div className={estilos.secaoTrocarSenha}>
            <div className={estilos.cabecalhoTrocaSenha}>
              <Key size={20} />
              <div>
                <h4>Defina uma nova senha de acesso</h4>
                <p>
                  Informe sua senha atual e escolha uma nova senha. Se o 2FA estiver
                  ativo, confirme também o código do aplicativo autenticador.
                </p>
              </div>
            </div>
            <form
              onSubmit={lidarComEnvioSenha}
              className={estilos.formularioSenha}
            >
              <CampoEntrada
                label="Senha Atual"
                type="password"
                name="current"
                value={dadosSenha.current}
                onChange={lidarComMudancaSenha}
                maxLength={SENHA_MAXIMA_UI}
                required
              />
              <div className={estilos.gradeSenhasNovas}>
                <CampoEntrada
                  label="Nova Senha"
                  type="password"
                  name="newPass"
                  value={dadosSenha.newPass}
                  onChange={lidarComMudancaSenha}
                  minLength={SENHA_MINIMA_UI}
                  maxLength={SENHA_MAXIMA_UI}
                  required
                />
                <CampoEntrada
                  label="Confirmar Nova Senha"
                  type="password"
                  name="confirmPass"
                  value={dadosSenha.confirmPass}
                  onChange={lidarComMudancaSenha}
                  minLength={SENHA_MINIMA_UI}
                  maxLength={SENHA_MAXIMA_UI}
                  required
                />
              </div>
              {twoFactorEnabled && (
                <div className={estilos.caixa2FA}>
                  <label htmlFor="perfil-token-2fa" className={estilos.rotulo2FA}>
                    Validação 2FA (6 dígitos)
                  </label>
                  <input
                    id="perfil-token-2fa"
                    type="text"
                    name="token"
                    inputMode="numeric"
                    pattern="[0-9]{6}"
                    maxLength="6"
                    placeholder="000000"
                    className={estilos.entrada2FA}
                    value={dadosSenha.token}
                    onChange={lidarComMudancaSenha}
                    required
                    autoComplete="off"
                  />
                </div>
              )}
              <div className={estilos.rodapeModalGeral}>
                <Botao
                  variant="secondary"
                  onClick={() => setModalSenhaAberto(false)}
                  type="button"
                >
                  Fechar
                </Botao>
                <Botao type="submit" disabled={carregando}>
                  {carregando ? "Salvando..." : "Confirmar Nova Senha"}
                </Botao>
              </div>
            </form>
          </div>
        </JanelaModal>
      )}

      {cameraAberta && (
        <div className={estilos.sobreposicaoModalCamera}>
          <div className={estilos.conteudoModalCamera}>
            <div className={estilos.cabecalhoModalCamera}>
              <h3 className={estilos.tituloModalCamera}>
                <ScanFace size={20} className={estilos.iconeFace} /> Captura
                Inteligente
              </h3>
              <button
                onClick={pararCamera}
                type="button"
                className={estilos.botaoFecharCamera}
              >
                <X size={18} />
              </button>
            </div>
            <div className={estilos.containerVideo}>
              <video
                ref={referenciaVideo}
                autoPlay
                playsInline
                muted
                className={estilos.videoCamera}
              />
              <div className={estilos.envoltorioAnel}>
                <div
                  className={`${estilos.anelCamera} ${estaAlinhado ? estilos.anelAlinhado : ""}`}
                >
                  {estaAlinhado && modelosCarregados && (
                    <svg className={estilos.svgProgresso} viewBox="0 0 100 100">
                      <circle
                        cx="50"
                        cy="50"
                        r="48"
                        fill="none"
                        stroke="#10b981"
                        strokeWidth="4"
                        strokeDasharray="301.59"
                        strokeDashoffset={
                          301.59 - (progressoScan / 100) * 301.59
                        }
                        className={estilos.circuloProgresso}
                      />
                    </svg>
                  )}
                </div>
              </div>
              <div className={estilos.containerDicaScan}>
                <p
                  className={`${estilos.dicaScan} ${estaAlinhado ? estilos.dicaScanAlinhada : ""}`}
                >
                  {dicaScan}
                </p>
              </div>
            </div>
            <div className={estilos.rodapeModalCamera}>
              <button
                type="button"
                onClick={pararCamera}
                className={estilos.botaoSecundarioModal}
              >
                Cancelar Captura
              </button>
              {!modelosCarregados && dicaScan.includes("Falha") && (
                <button
                  type="button"
                  onClick={capturarFoto}
                  className={estilos.botaoCapturaManual}
                >
                  <Camera size={18} /> Capturar Manualmente
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
