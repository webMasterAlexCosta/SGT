import React from "react";
import { ArrowLeft, CheckCircle2, Mail, ShieldCheck } from "lucide-react";
import { InputModerno } from "./InputModerno";
import { BotaoAcao } from "./BotaoAcao";
import estilos from "../index.module.css";

export const FormularioRecuperacaoSenha = React.memo(({
  modo,
  email,
  aoMudarEmail,
  carregando,
  validandoToken,
  tokenValido,
  mensagemToken,
  confirmada,
  aoConfirmarToken,
  aoSubmeterSolicitacao,
  aoVoltarLogin,
}: { modo: 'SOLICITAR' | 'REDEFINIR'; email: string; aoMudarEmail: (valor: string) => void; carregando?: boolean; validandoToken?: boolean; tokenValido?: boolean | null; mensagemToken?: string; confirmada?: boolean; aoConfirmarToken: () => void; aoSubmeterSolicitacao: React.FormEventHandler<HTMLFormElement>; aoVoltarLogin: () => void }) => {
  if (modo === "REDEFINIR") {
    const valido = !validandoToken && tokenValido === true;
    const invalido = !validandoToken && tokenValido === false;
    return (
      <div className={estilos.formularioRecuperacao}>
        <div className={estilos.caixaAlerta} role="status" aria-live="polite">
          {confirmada
            ? <CheckCircle2 className={estilos.iconeAlerta} size={19} />
            : <ShieldCheck className={estilos.iconeAlerta} size={19} />}
          <div className={estilos.conteudoAlerta}>
            <strong className={estilos.tituloAlerta}>
              {validandoToken ? "Validando recuperação" : confirmada ? "Senha temporária enviada" : valido ? "Link válido" : "Link indisponível"}
            </strong>
            <span className={estilos.textoAlerta}>
              {validandoToken
                ? "Validando o link de recuperação com segurança..."
                : mensagemToken || (invalido ? "Este link é inválido, expirou ou já foi utilizado." : "Confirme a operação para continuar.")}
            </span>
          </div>
        </div>

        {valido && !confirmada && (
          <div className={estilos.containerBotao}>
            <BotaoAcao
              type="button"
              carregando={carregando}
              desabilitado={carregando}

              onClick={aoConfirmarToken}
            >
              Confirmar recuperação e enviar senha temporária
            </BotaoAcao>
          </div>
        )}

        {confirmada && (
          <div className={estilos.caixaAlerta}>
            <Mail className={estilos.iconeAlerta} size={18} />
            <div className={estilos.conteudoAlerta}>
              <strong className={estilos.tituloAlerta}>Próximo passo</strong>
              <span className={estilos.textoAlerta}>
                Entre com a senha temporária recebida por e-mail. No primeiro login, o sistema exigirá uma nova senha pessoal.
              </span>
            </div>
          </div>
        )}

        {!validandoToken && (
          <button type="button" className={estilos.botaoVoltarRecuperacao} onClick={aoVoltarLogin} disabled={carregando}>
            <ArrowLeft size={16} /> {confirmada ? "Ir para o login" : "Voltar ao login"}
          </button>
        )}
      </div>
    );
  }

  return (
    <form className={estilos.formularioAcesso} onSubmit={aoSubmeterSolicitacao}>
      <div className={estilos.caixaAlerta}>
        <Mail className={estilos.iconeAlerta} size={18} />
        <div className={estilos.conteudoAlerta}>
          <strong className={estilos.tituloAlerta}>Recuperar acesso</strong>
          <span className={estilos.textoAlerta}>
            Informe seu e-mail corporativo. Se a conta estiver ativa, enviaremos as instruções sem revelar se o endereço possui cadastro.
          </span>
        </div>
      </div>
      <InputModerno
        rotulo="E-mail corporativo"
        type="email"
        icone={Mail}
        placeholder="nome@correios.com.br"
        value={email}
        onChange={(evento) => aoMudarEmail(evento.target.value)}
        required
        maxLength={254}
        desabilitado={carregando}
        autoComplete="email"
      />
      <div className={estilos.containerBotao}>
        <BotaoAcao type="submit" carregando={carregando} desabilitado={carregando}>
          Enviar link de recuperação
        </BotaoAcao>
      </div>
      <button type="button" className={estilos.botaoVoltarRecuperacao} onClick={aoVoltarLogin} disabled={carregando}>
        <ArrowLeft size={16} /> Voltar ao login
      </button>
    </form>
  );
});

FormularioRecuperacaoSenha.displayName = "FormularioRecuperacaoSenha";
