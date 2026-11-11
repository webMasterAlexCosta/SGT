import React, { useId } from 'react';
import { createPortal } from 'react-dom';
import { X, Info } from 'lucide-react';
import { useAutenticacao } from '../../modules/auth/context'; 
import { CONFIG } from '../config';
import estilos from './UI.module.css';
import { useDialogoAcessivel } from './acessibilidadeDialogo.ts';

export const useMostrarAjuda = () => {
  let autenticacao: ReturnType<typeof useAutenticacao> | null = null;
  try { autenticacao = useAutenticacao(); } catch (erro) {}
  
  const usuario = autenticacao?.user;
  if (usuario && usuario.createdAt) {
    const dataCriacao = new Date(usuario.createdAt);
    const diferencaDias = (new Date().getTime() - dataCriacao.getTime()) / (1000 * 3600 * 24);
    if (diferencaDias > CONFIG.HELP_TOOLTIP_DAYS) return false; 
  }
  return true; 
};

export const EnvolucroDica = ({ children, tooltip, position = 'top', disabled = false, wrapperClass = "" }: React.PropsWithChildren<{ tooltip?: React.ReactNode; position?: string; disabled?: boolean; wrapperClass?: string }>) => {
  const mostrarAjuda = useMostrarAjuda();
  if (!tooltip || disabled || !mostrarAjuda) return <>{children}</>;

  let classesPosicaoTooltip = "";
  let classesPosicaoSeta = "";
  if (position === 'top') { classesPosicaoTooltip = estilos.posicaoTopo; classesPosicaoSeta = estilos.setaTopo; } 
  else if (position === 'bottom') { classesPosicaoTooltip = estilos.posicaoBaixo; classesPosicaoSeta = estilos.setaBaixo; } 
  else if (position === 'left') { classesPosicaoTooltip = estilos.posicaoEsquerda; classesPosicaoSeta = estilos.setaEsquerda; } 
  else if (position === 'right') { classesPosicaoTooltip = estilos.posicaoDireita; classesPosicaoSeta = estilos.setaDireita; }

  return (
    <div className={wrapperClass || estilos.envoltorioTooltip}>
      {children}
      <div className={`${estilos.caixaTooltip} ${classesPosicaoTooltip}`}>
        {tooltip}
        <div className={`${estilos.setaTooltip} ${classesPosicaoSeta}`} />
      </div>
    </div>
  );
};

export const Botao = ({
  children,
  onClick,
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  carregando = false,
  className = '',
  type = 'button',
  disabled = false,
  title = '',
  tooltip = '',
  tooltipPosition = 'top',
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: string; size?: string; fullWidth?: boolean; carregando?: boolean; tooltip?: string; tooltipPosition?: string }) => {
  const mostrarAjuda = useMostrarAjuda();
  const variantes: Readonly<Record<string, string | undefined>> = {
    primary: estilos.btnPrimario,
    secondary: estilos.btnSecundario,
    outline: estilos.btnContorno,
    success: estilos.btnSucesso,
    danger: estilos.btnPerigo,
    ghost: estilos.btnFantasma,
  };
  const tamanhos: Readonly<Record<string, string | undefined>> = { sm: estilos.botaoPequeno, md: '', lg: estilos.botaoGrande };
  const estaDesabilitado = disabled || carregando;
  const elementoBotao = (
    <button
      {...props}
      type={type}
      onClick={onClick}
      disabled={estaDesabilitado}
      aria-busy={carregando || undefined}
      title={mostrarAjuda ? title : ''}
      className={`${estilos.botaoBase} ${variantes[String(variant)] || estilos.btnPrimario} ${tamanhos[String(size)] || ''} ${fullWidth ? estilos.botaoLarguraTotal : ''} ${estaDesabilitado ? estilos.botaoDesabilitado : ''} ${className}`}
    >
      {carregando && <span className={estilos.indicadorBotao} aria-hidden="true" />}
      {children}
    </button>
  );
  if (tooltip && mostrarAjuda) {
    return <EnvolucroDica tooltip={tooltip} position={tooltipPosition} disabled={estaDesabilitado}>{elementoBotao}</EnvolucroDica>;
  }
  return elementoBotao;
};

export const CampoEntrada = ({
  label,
  id,
  name,
  value,
  onChange,
  type = "text",
  required,
  className = "",
  placeholder = "",
  disabled = false,
  readOnly = false,
  tooltip = '',
  autoComplete,
  erro = '',
  ajuda = '',
  ...props
}: React.InputHTMLAttributes<HTMLInputElement> & { label?: React.ReactNode; tooltip?: string; erro?: string; ajuda?: string }) => {
  const mostrarAjuda = useMostrarAjuda();
  const identificadorGerado = useId();
  const identificador = id || name || `campo-${identificadorGerado.replace(/:/g, '')}`;
  const identificadorAjuda = `${identificador}-ajuda`;
  const identificadorErro = `${identificador}-erro`;
  let classeCampoEstado = '';
  if (disabled) classeCampoEstado = estilos.campoDesabilitado;
  else if (readOnly) classeCampoEstado = estilos.campoSomenteLeitura;
  if (erro) classeCampoEstado = `${classeCampoEstado} ${estilos.campoComErro}`;
  const valorAutoComplete = autoComplete || (type === 'password' ? 'current-password' : 'off');
  const descritoPor = [ajuda ? identificadorAjuda : '', erro ? identificadorErro : ''].filter(Boolean).join(' ') || undefined;

  return (
    <div className={`${estilos.grupoEntrada} ${className}`}>
      {(label || tooltip) && (
        <div className={estilos.cabecalhoEntrada}>
          {label && <label htmlFor={identificador} className={estilos.rotuloEntrada}>{label} {required && <span className={estilos.asteriscoObrigatorio}>*</span>}</label>}
          {tooltip && mostrarAjuda && (
            <EnvolucroDica tooltip={tooltip} position="top"><Info size={14} className={estilos.iconeAjuda} /></EnvolucroDica>
          )}
        </div>
      )}
      <input
        {...props}
        id={identificador}
        type={type}
        name={name}
        value={value ?? ''}
        onChange={onChange}
        placeholder={placeholder}
        disabled={disabled}
        readOnly={readOnly}
        className={`${estilos.campoEntrada} ${classeCampoEstado}`}
        required={required}
        autoComplete={valorAutoComplete}
        aria-invalid={Boolean(erro)}
        aria-describedby={descritoPor}
      />
      {ajuda && <small id={identificadorAjuda} className={estilos.textoAjudaCampo}>{ajuda}</small>}
      {erro && <small id={identificadorErro} className={estilos.textoErroCampo}>{erro}</small>}
    </div>
  );
};

export const Seletor = ({ label, id, name, value, onChange, options = [], required, disabled = false, tooltip = '', erro = '', ...props }: React.SelectHTMLAttributes<HTMLSelectElement> & { label?: React.ReactNode; options?: { id?: number | string | null; value?: number | string | null; nome?: string | null; descricao?: string | null; numero?: string | null; label?: string | null }[]; tooltip?: string; erro?: string }) => {
  const mostrarAjuda = useMostrarAjuda();
  const identificadorGerado = useId();
  const identificador = id || name || `selecao-${identificadorGerado.replace(/:/g, '')}`;
  return (
    <div className={estilos.grupoEntrada}>
      <div className={estilos.cabecalhoEntrada}>
        <label htmlFor={identificador} className={estilos.rotuloEntrada}>{label} {required && <span className={estilos.asteriscoObrigatorio}>*</span>}</label>
        {tooltip && mostrarAjuda && <EnvolucroDica tooltip={tooltip} position="top"><Info size={14} className={estilos.iconeAjuda} /></EnvolucroDica>}
      </div>
      <div className={estilos.envoltorioSelect}>
        <select
          {...props}
          id={identificador}
          name={name}
          value={value ?? ''}
          onChange={onChange}
          disabled={disabled}
          required={required}
          aria-invalid={Boolean(erro)}
          className={`${estilos.campoEntrada} ${estilos.campoSelect} ${disabled ? estilos.campoDesabilitado : ''} ${erro ? estilos.campoComErro : ''}`}
        >
          <option value="">Selecione...</option>
          {options.map((opcao) => <option key={opcao.id ?? opcao.value ?? undefined} value={opcao.id ?? opcao.value ?? undefined}>{opcao.nome || opcao.descricao || opcao.numero || opcao.label}</option>)}
        </select>
        <div className={estilos.iconeSetaSelect}><svg className={estilos.setaSvg} viewBox="0 0 20 20"><path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z"/></svg></div>
      </div>
      {erro && <small className={estilos.textoErroCampo}>{erro}</small>}
    </div>
  );
};

// --- CORREÇÃO: Aplica a classe cartaoBase correta e repassa props (data-tour) ---
export const Cartao = ({ title, children, className = '', ...props }: Omit<React.HTMLAttributes<HTMLDivElement>, 'title'> & { title?: React.ReactNode }) => {
  return (
    <div className={`${estilos.cartaoBase} ${className}`} {...props}>
      {title && (
        <div className={estilos.cabecalhoCartao}>
          <h3 className={estilos.tituloCartao}>{title}</h3>
        </div>
      )}
      <div className={estilos.corpoCartao}>{children}</div>
    </div>
  );
};
export const Tabela = ({ children, headers = [], className = '', caption = '', ...props }: React.TableHTMLAttributes<HTMLTableElement> & { headers?: React.ReactNode[]; caption?: string }) => {
  return (
    <div className={estilos.envoltorioTabela}>
      <table className={`${estilos.tabelaBase} ${className}`} {...props}>
        {caption && <caption className={estilos.legendaTabela}>{caption}</caption>}
        {headers.length > 0 && (
          <thead>
            <tr>{headers.map((cabecalho, indice) => <th key={`${cabecalho}-${indice}`} scope="col">{cabecalho}</th>)}</tr>
          </thead>
        )}
        <tbody>{children}</tbody>
      </table>
    </div>
  );
};
export const JanelaModal = ({
  isOpen,
  onClose,
  title,
  description = "",
  children,
  maxWidth = "max-w-4xl",
  closeOnBackdrop = true,
  closeOnEsc = true,
  showCloseButton = true,
  bloquearFechamento = false,
}: React.PropsWithChildren<{ isOpen: boolean; onClose?: () => void; title: React.ReactNode; description?: React.ReactNode; maxWidth?: string; closeOnBackdrop?: boolean; closeOnEsc?: boolean; showCloseButton?: boolean; bloquearFechamento?: boolean }>) => {
  const idBase = useId().replace(/:/g, "");
  const idTitulo = `dialogo-${idBase}-titulo`;
  const idDescricao = description ? `dialogo-${idBase}-descricao` : undefined;
  const referenciaDialogo = useDialogoAcessivel({
    aberto: isOpen,
    aoFechar: onClose,
    bloquearFechamento,
    fecharComEsc: closeOnEsc,
  });

  if (!isOpen) return null;

  const obterClasseLargura = () => {
    switch (maxWidth) {
      case "max-w-4xl":
      case "max-w-2xl": return estilos.larguraMax4xl;
      case "max-w-lg": return estilos.larguraMaxLg;
      case "max-w-md": return estilos.larguraMaxMd;
      case "max-w-sm": return estilos.larguraMaxSm;
      default: return estilos.larguraMax4xl;
    }
  };

  const tentarFecharPeloFundo = (evento: React.MouseEvent<HTMLElement>) => {
    if (!closeOnBackdrop || bloquearFechamento) return;
    if (evento.target === evento.currentTarget) onClose?.();
  };

  return createPortal(
    <div className={estilos.sobreposicaoModal} onMouseDown={tentarFecharPeloFundo}>
      <div
        ref={referenciaDialogo}
        className={`${estilos.caixaModal} ${obterClasseLargura()}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        aria-describedby={idDescricao}
        aria-busy={bloquearFechamento || undefined}
        tabIndex={-1}
      >
        <div className={estilos.cabecalhoModal}>
          <div className={estilos.textosCabecalhoModal}>
            <h2 id={idTitulo} className={estilos.tituloModal}>{title}</h2>
            {description && <p id={idDescricao} className={estilos.descricaoModal}>{description}</p>}
          </div>
          {showCloseButton && (
            <button
              type="button"
              onClick={onClose}
              className={estilos.botaoFecharModal}
              aria-label={`Fechar ${title || "diálogo"}`}
              disabled={bloquearFechamento}
            >
              <X size={24} aria-hidden="true" />
            </button>
          )}
        </div>
        <div className={estilos.corpoModal}>{children}</div>
      </div>
    </div>,
    document.body,
  );
};
