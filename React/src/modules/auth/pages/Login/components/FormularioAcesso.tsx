
import type { AutenticacaoSuperintendenciaDTO, AutenticacaoGestaoLocalDTO } from '../../../../../contracts/java';
import React from "react";
import {
  User,
  Lock,
  Mail,
  CreditCard,
  Building2,
  ArrowRight,
  CheckCircle2,
  ShieldCheck,
} from "lucide-react";
import { InputModerno } from "./InputModerno";
import { BotaoAcao } from "./BotaoAcao";
import { CONFIG } from "../../../../../core/config";
import { formatarGestaoLocal, formatarGestorEstadual } from "../../../../../core/formatters/nomenclaturaGestao.ts";
import estilos from "../index.module.css";

// 🚀 MELHORIA: React.memo() garante que o formulário só vai renderizar de novo 
// se os dados passados para ele de fato mudarem.
export const FormularioAcesso = React.memo(({
  estadoTela,
  credenciais,
  aoMudarCredenciais,
  aoMudarMatricula,
  novaSenha,
  aoMudarNovaSenha,
  carregando,
  bloqueado,
  modoDesempenho,
  superintendencias,
  carregandoSuperintendencias,
  gestoesLocais,
  carregandoGestoesLocais,
  segundoFatorPendente,
  aoReiniciarLogin,
  aoRecuperarSenha,
  aoSubmeter,
}: { estadoTela: string; credenciais: Record<'username' | 'password' | 'confirmPassword' | 'email' | 'matricula' | 'totp' | 'superintendenciaId' | 'gestaoLocalId', string>; aoMudarCredenciais: (campo: "username" | "password" | "confirmPassword" | "email" | "matricula" | "totp" | "superintendenciaId" | "gestaoLocalId", valor: string) => void; aoMudarMatricula: React.ChangeEventHandler<HTMLInputElement>; novaSenha: { password: string; confirm: string }; aoMudarNovaSenha: (campo: "password" | "confirm", valor: string) => void; carregando?: boolean; bloqueado?: boolean; modoDesempenho?: boolean; superintendencias?: AutenticacaoSuperintendenciaDTO[]; carregandoSuperintendencias?: boolean; gestoesLocais?: AutenticacaoGestaoLocalDTO[]; carregandoGestoesLocais?: boolean; segundoFatorPendente?: boolean; aoReiniciarLogin: () => void; aoRecuperarSenha: () => void; aoSubmeter: React.FormEventHandler<HTMLFormElement> }) => {
  return (
    <form
      onSubmit={aoSubmeter}
      className={estilos.formularioAcesso}
      data-tour="login-form"
    >
      {estadoTela === "TROCAR_SENHA" && (
        <div className={estilos.formularioAcesso}>
          <div className={estilos.caixaAlerta}>
            <ShieldCheck className={estilos.iconeAlerta} size={16} />
            <div className={estilos.conteudoAlerta}>
              <strong className={estilos.tituloAlerta}>
                Troca Obrigatória
              </strong>
              <span className={estilos.textoAlerta}>
                Defina uma nova senha para o seu primeiro acesso.
              </span>
            </div>
          </div>
          <InputModerno
            rotulo="Nova Senha"
            type="password"
            icone={Lock}
            placeholder="••••••••"
            value={novaSenha.password}
            onChange={(evento) =>
              aoMudarNovaSenha("password", evento.target.value)
            }
            required
            minLength={6}
            maxLength={72}
            desabilitado={carregando}
            autoComplete="new-password"
          />
          <InputModerno
            rotulo="Confirmar Senha"
            type="password"
            icone={CheckCircle2}
            placeholder="••••••••"
            value={novaSenha.confirm}
            onChange={(evento) =>
              aoMudarNovaSenha("confirm", evento.target.value)
            }
            required
            minLength={6}
            maxLength={72}
            desabilitado={carregando}
            autoComplete="new-password"
          />
        </div>
      )}

      {estadoTela === "LOGIN" && (
        <InputModerno
          rotulo="Matrícula"
          type="text"
          icone={CreditCard}
          placeholder="0.000.000-0"
          value={credenciais.matricula}
          onChange={aoMudarMatricula}
          required
          desabilitado={bloqueado || carregando || segundoFatorPendente}
          autoComplete="username"
          inputMode="numeric"
          maxLength={11}
        />
      )}

      {estadoTela === "REGISTRO" && (
        <>
          <InputModerno
            rotulo="Nome de usuário"
            type="text"
            icone={User}
            placeholder="Ex: joao.silva"
            value={credenciais.username}
            onChange={(evento) =>
              aoMudarCredenciais("username", evento.target.value)
            }
            required
            desabilitado={carregando}
            autoComplete="username"
          />
          <InputModerno
            rotulo="Matrícula"
            type="text"
            icone={CreditCard}
            placeholder="0.000.000-0"
            value={credenciais.matricula}
            onChange={aoMudarMatricula}
            required
            desabilitado={carregando}
            inputMode="numeric"
            maxLength={11}
          />
          <InputModerno
            rotulo="Email Corporativo"
            type="email"
            icone={Mail}
            placeholder={CONFIG.APP_ALLOWED_DOMAIN}
            value={credenciais.email}
            onChange={(evento) =>
              aoMudarCredenciais("email", evento.target.value)
            }
            required
            desabilitado={carregando}
            autoComplete="email"
          />
          <div className={estilos.grupoEntrada}>
            <label className={estilos.rotuloEntrada}>Gestão Estadual / UF</label>
            <div className={estilos.envoltorioEntrada}>
              <div
                className={`${estilos.iconeEntrada} ${carregandoSuperintendencias ? estilos.iconeEntradaDesabilitado : ""}`}
              >
                <Building2 size={16} />
              </div>
              <select
                className={`${estilos.campoEntrada} ${carregandoSuperintendencias ? estilos.campoEntradaDesabilitado : ""}`}
                value={credenciais.superintendenciaId || ""}
                onChange={(evento) =>
                  aoMudarCredenciais("superintendenciaId", evento.target.value)
                }
                required
                disabled={carregando || carregandoSuperintendencias}
                aria-label="Gestão Estadual / UF"
              >
                <option value="">
                  {carregandoSuperintendencias ? "Carregando gestores estaduais..." : "Selecione seu gestor estadual"}
                </option>
                {(superintendencias || []).map((item) => (
                  <option key={item.id} value={(item.id) ?? undefined}>
                    {formatarGestorEstadual(item, item.uf)}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className={estilos.grupoEntrada}>
            <label className={estilos.rotuloEntrada}>Gestão Local</label>
            <div className={estilos.envoltorioEntrada}>
              <div
                className={`${estilos.iconeEntrada} ${carregandoGestoesLocais ? estilos.iconeEntradaDesabilitado : ""}`}
              >
                <Building2 size={16} />
              </div>
              <select
                className={`${estilos.campoEntrada} ${carregandoGestoesLocais ? estilos.campoEntradaDesabilitado : ""}`}
                value={credenciais.gestaoLocalId || ""}
                onChange={(evento) => aoMudarCredenciais("gestaoLocalId", evento.target.value)}
                required
                disabled={carregando || carregandoGestoesLocais || !credenciais.superintendenciaId}
                aria-label="Gestão Local"
              >
                <option value="">
                  {!credenciais.superintendenciaId
                    ? "Selecione primeiro a UF"
                    : carregandoGestoesLocais
                      ? "Carregando Gestões Locais..."
                      : "Selecione sua Gestão Local"}
                </option>
                {(gestoesLocais || []).map((item) => (
                  <option key={item.id} value={(item.id) ?? undefined}>
                    {formatarGestaoLocal(item)}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </>
      )}

      {estadoTela === "LOGIN" && (
        <>
          <InputModerno
            rotulo="Senha"
            type="password"
            icone={Lock}
            placeholder="••••••••"
            value={credenciais.password}
            onChange={(evento) =>
              aoMudarCredenciais("password", evento.target.value)
            }
            required
            maxLength={72}
            desabilitado={bloqueado || carregando || segundoFatorPendente}
            autoComplete="current-password"
          />
          {!segundoFatorPendente && (
            <button
              type="button"
              className={estilos.botaoEsqueciSenha}
              onClick={aoRecuperarSenha}
              disabled={bloqueado || carregando}
            >
              Esqueci minha senha
            </button>
          )}
          {segundoFatorPendente && (
            <>
              <div className={estilos.caixaAlerta} role="status">
                <ShieldCheck className={estilos.iconeAlerta} size={16} />
                <div className={estilos.conteudoAlerta}>
                  <strong className={estilos.tituloAlerta}>Segundo fator</strong>
                  <span className={estilos.textoAlerta}>
                    Digite o código de 6 dígitos do aplicativo autenticador.
                  </span>
                </div>
                <div className={estilos.acoesSegundoFator}>
                  <button
                    type="button"
                    className={estilos.botaoVoltar2FA}
                    onClick={aoReiniciarLogin}
                    disabled={carregando}
                  >
                    Alterar matrícula ou senha
                  </button>
                </div>
              </div>
              <InputModerno
                rotulo="Código do autenticador"
                type="text"
                icone={ShieldCheck}
                placeholder="000000"
                value={credenciais.totp || ""}
                onChange={(evento) =>
                  aoMudarCredenciais("totp", evento.target.value.replace(/\D/g, "").slice(0, 6))
                }
                required
                inputMode="numeric"
                maxLength={6}
                desabilitado={bloqueado || carregando}
                autoComplete="one-time-code"
              />
            </>
          )}
          {!segundoFatorPendente && (
            <div className={estilos.dicaLogin2FA}>
              <ShieldCheck size={14} />
              <span>Se o 2FA estiver ativo, o backend solicitará o código do aplicativo autenticador antes de criar a sessão.</span>
            </div>
          )}
        </>
      )}

      <div className={estilos.containerBotao}>
        <BotaoAcao
          type="submit"
          carregando={carregando}
          desabilitado={bloqueado || (estadoTela === "REGISTRO" && (carregandoSuperintendencias || carregandoGestoesLocais))}
          modoDesempenho={modoDesempenho}
        >
          {estadoTela === "LOGIN"
            ? segundoFatorPendente ? "Validar segundo fator" : "Entrar no Sistema"
            : estadoTela === "TROCAR_SENHA"
              ? "Ativar Conta"
              : "Solicitar"}
          {!carregando && <ArrowRight size={18} />}
        </BotaoAcao>
      </div>
    </form>
  );
});

FormularioAcesso.displayName = "FormularioAcesso";
