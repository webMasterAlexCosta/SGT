import React from "react";
import { AlertCircle, Lightbulb } from "lucide-react";
import { ConteinerAvisos } from "../../../../core/ui/Aviso.tsx";
import { CONFIG } from "../../../../core/config.ts";
import Homologacao from "../../../../core/ui/Homologacao.tsx";
import logoCorreios from "../../../../assets/correios.png";
import estilos from "./index.module.css";
import { useLogin } from "./useLogin";
import { TourGuiado } from "./components/TourGuiado.tsx";
import { IconesTecnologia } from "./components/IconesTecnologia.tsx";
import { ModalTeste } from "./components/ModalTeste.tsx";
import { FormularioAcesso } from "./components/FormularioAcesso.tsx";
import { FormularioRecuperacaoSenha } from "./components/FormularioRecuperacaoSenha.tsx";

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
      id: "tech-stack",
      title: "Tecnologia de Ponta",
      text: "Nosso sistema foi construído sob rigorosos padrões corporativos de segurança, garantindo um alto nível de integridade e proteção para os seus dados.",
    },
  ];


export const Login = () => {
  const {
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
  } = useLogin();

  return (
    <div className={estilos.envoltorioGeral}>
      <ConteinerAvisos />
      <TourGuiado
        passos={passosTour}
        aberto={tourAberto}
        aoFechar={() => setTourAberto(false)}
      />

      <div className={estilos.containerBotoesTopo}>
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
          Sistema desenvolvido por {CONFIG.AUTHOR_NAME}{" "}
          <span className={estilos.matriculaCreditos}>
            {CONFIG.AUTHOR_MATRICULA}
          </span>
        </p>
      </div>
    </div>
  );
};
