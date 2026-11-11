import { observarFalha } from '../http/falhaObservada';
import React, { useState, memo, useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import {
  ShieldCheck,
  Loader2,
  X,
  LockKeyhole,
  AlertTriangle,
} from "lucide-react";
import logoCorreios from "../../assets/logo-footer.png";
import estilos from "./ModalConfirmacaoSenha.module.css";
import { useDialogoAcessivel } from '../ui/acessibilidadeDialogo.ts';

export const ModalConfirmacaoSenha = memo(
  ({
    aberto,
    aoFechar,
    aoConfirmar,
    titulo = "Confirmação de Segurança",
    mensagem = "Para prosseguir, informe sua senha de acesso corporativo.",
    rotuloConfirmar = "Confirmar",
    rotuloCancelar = "Cancelar",
    somenteConfirmacao = false,
    subtitulo = "Assinatura digital requerida",
    acao,
    entidade,
    consequencia,
  }: { aberto: boolean; aoFechar: () => void; aoConfirmar: (senha?: string) => Promise<boolean | void>; titulo?: string; mensagem?: React.ReactNode; rotuloConfirmar?: string; rotuloCancelar?: string; somenteConfirmacao?: boolean; subtitulo?: string; acao?: string; entidade?: string; consequencia?: string }) => {
    const [senha, setSenha] = useState("");
    const [confirmando, setConfirmando] = useState(false);
    const [mensagemErro, setMensagemErro] = useState("");
    const [visivel, setVisivel] = useState(false);
    const temporizadorVisibilidadeRef = useRef<number | null>(null);

    useEffect(() => {
      if (temporizadorVisibilidadeRef.current) {
        window.clearTimeout(temporizadorVisibilidadeRef.current);
        temporizadorVisibilidadeRef.current = null;
      }

      if (aberto) {
        setSenha("");
        setConfirmando(false);
        setMensagemErro("");
        temporizadorVisibilidadeRef.current = window.setTimeout(() => setVisivel(true), 10);
      } else {
        setVisivel(false);
      }

      return () => {
        if (temporizadorVisibilidadeRef.current) {
          window.clearTimeout(temporizadorVisibilidadeRef.current);
        }
      };
    }, [aberto]);

    const lidarComSubmissao = async (evento?: React.FormEvent<HTMLFormElement>) => {
      if (evento) evento.preventDefault();
      if (confirmando) return;

      if (!somenteConfirmacao && !senha.trim()) {
        setMensagemErro("Informe sua senha para prosseguir.");
        return;
      }

      setConfirmando(true);
      setMensagemErro("");

      try {
        const sucesso = await aoConfirmar?.(
          somenteConfirmacao ? undefined : senha,
        );
        if (sucesso === true) {
          lidarComFechamento();
        }
      } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

        setSenha("");
        const msg =
          erro?.message ||
          (typeof erro === "string" ? erro : "") ||
          (erro?.status
            ? `Falha na confirmação (status ${erro.status}).`
            : "Falha na confirmação.");

        setMensagemErro(msg);
      } finally {
        setConfirmando(false);
      }
    };

    const lidarComFechamento = () => {
      if (!confirmando) {
        setVisivel(false);
        temporizadorVisibilidadeRef.current = window.setTimeout(() => {
          aoFechar?.();
        }, 220);
      }
    };

    const referenciaDialogo = useDialogoAcessivel({ aberto: aberto || visivel, aoFechar: lidarComFechamento, bloquearFechamento: confirmando });

    if (!aberto && !visivel) return null;

    return createPortal(
      <div
        ref={referenciaDialogo}
        className={`${estilos.sobreposicao} ${visivel ? estilos.sobreposicaoVisivel : ""}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-confirmacao-senha"
        onMouseDown={(e) => {
          if (e.target === e.currentTarget && !confirmando)
            lidarComFechamento();
        }}
      >
        <div
          className={`${estilos.cartaoModal} ${visivel ? estilos.cartaoVisivel : ""}`}
        >
          {confirmando && <div className={estilos.barraProgresso}></div>}

          <div className={estilos.bannerCorreios}>
            <button
              type="button"
              onClick={lidarComFechamento}
              disabled={confirmando}
              className={estilos.botaoFechar}
              aria-label="Fechar"
            >
              <X size={20} />
            </button>

            <div className={estilos.logoCaixa}>
              <img
                src={logoCorreios}
                alt="Correios"
                className={estilos.logoBanner}
              />
            </div>

            <div className={estilos.textosBanner}>
              <h3 id="titulo-confirmacao-senha">{titulo}</h3>
              <p>{subtitulo}</p>
            </div>
          </div>

          <form onSubmit={lidarComSubmissao} className={estilos.corpoFormulario}>
            <div className={estilos.caixaInformativa}>
              <ShieldCheck size={20} className={estilos.iconeInformativo} />
              <p className={estilos.mensagemDescritiva}>{mensagem}</p>
            </div>

            {(acao || entidade || consequencia) && (
              <div className={estilos.detalhesOperacao} aria-label="Detalhes da operação protegida">
                {acao && <p><strong>Ação:</strong> {acao}</p>}
                {entidade && <p><strong>Entidade:</strong> {entidade}</p>}
                {consequencia && <p><strong>Consequência:</strong> {consequencia}</p>}
              </div>
            )}

            {!somenteConfirmacao && (
              <div className={estilos.grupoEntrada}>
                <label
                  className={estilos.rotuloEntrada}
                  htmlFor="input-senha-confirmacao"
                >
                  <LockKeyhole size={14} /> Senha de Acesso
                </label>
                <div className={estilos.envoltorioEntrada}>
                  <input
                    id="input-senha-confirmacao"
                    data-foco-inicial
                    type="password"
                    value={senha}
                    onChange={(e) => setSenha(e.target.value)}
                    disabled={confirmando}
                    autoFocus
                    placeholder="Digite sua senha..."
                    autoComplete="current-password"
                    maxLength={72}
                    aria-invalid={Boolean(mensagemErro)}
                    aria-describedby={mensagemErro ? "erro-confirmacao-senha" : undefined}
                    className={`${estilos.entradaSenha} ${mensagemErro ? estilos.entradaComErro : ""}`}
                  />
                </div>
              </div>
            )}

            {mensagemErro && (
              <div id="erro-confirmacao-senha" className={estilos.caixaErro} role="alert">
                <AlertTriangle size={16} className={estilos.iconeErro} />
                <span>{mensagemErro}</span>
              </div>
            )}

            <div className={estilos.rodapeAcoes}>
              <button
                type="button"
                onClick={lidarComFechamento}
                disabled={confirmando}
                className={estilos.botaoCancelar}
              >
                {rotuloCancelar}
              </button>

              <button
                type="submit"
                disabled={(!somenteConfirmacao && !senha.trim()) || confirmando}
                className={`${estilos.botaoConfirmar} ${confirmando ? estilos.botaoConfirmando : ""}`}
              >
                {confirmando ? (
                  <>
                    <Loader2 size={18} className={estilos.iconeGirando} />
                    Processando...
                  </>
                ) : (
                  rotuloConfirmar
                )}
              </button>
            </div>
          </form>
        </div>
      </div>,
      document.body,
    );
  },
);

ModalConfirmacaoSenha.displayName = "ModalConfirmacaoSenha";
