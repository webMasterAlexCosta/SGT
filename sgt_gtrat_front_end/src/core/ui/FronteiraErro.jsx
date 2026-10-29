import React from "react";
import { Copy, RefreshCw, RotateCcw } from "lucide-react";
import { gerarIdRequisicao } from "../http/requestId.js";
import estilos from "./FronteiraErro.module.css";

const obterRequestId = (erro) =>
  erro?.requestId || erro?.data?.requestId || erro?.response?.data?.requestId || null;

export class FronteiraErro extends React.Component {
  constructor(props) {
    super(props);
    this.state = { temErro: false, erroEncontrado: null, identificadorSuporte: null, copiado: false };
  }

  static getDerivedStateFromError(erro) {
    return {
      temErro: true,
      erroEncontrado: erro,
      identificadorSuporte: obterRequestId(erro) || `ui_${gerarIdRequisicao()}`,
      copiado: false,
    };
  }

  componentDidCatch(erro, info) {
    if (import.meta?.env?.DEV) {
      console.error("FronteiraErro capturou:", erro);
      console.error("Pilha de componentes:", info?.componentStack);
    }
  }

  recarregarPagina = () => window.location.reload();

  tentarNovamente = () => {
    this.setState({ temErro: false, erroEncontrado: null, identificadorSuporte: null, copiado: false });
  };

  copiarIdentificador = async () => {
    const valor = this.state.identificadorSuporte;
    if (!valor) return;
    try {
      await navigator.clipboard?.writeText?.(valor);
      this.setState({ copiado: true });
    } catch {
      this.setState({ copiado: false });
    }
  };

  render() {
    if (!this.state.temErro) return this.props.children;

    return (
      <main className={estilos.containerGeral}>
        <section className={estilos.cartaoErro} aria-labelledby="falha-aplicacao-titulo">
          <div className={estilos.cabecalhoErro}>
            <h1 id="falha-aplicacao-titulo" className={estilos.tituloErro}>Falha recuperável na aplicação</h1>
            <p className={estilos.subtituloErro}>
              A interface encontrou um problema inesperado. Seus dados no servidor não foram alterados por esta tela de erro.
            </p>
          </div>
          <div className={estilos.corpoErro}>
            <div className={estilos.mensagemErro} role="alert">
              Tente abrir a tela novamente. Se o problema continuar, informe o identificador abaixo ao suporte.
            </div>

            {this.state.identificadorSuporte && (
              <div className={estilos.suporte}>
                <span>Identificador de suporte</span>
                <code>{this.state.identificadorSuporte}</code>
                <button type="button" className={estilos.botaoCopiar} onClick={this.copiarIdentificador} aria-label="Copiar identificador de suporte">
                  <Copy size={15} aria-hidden="true" /> {this.state.copiado ? "Copiado" : "Copiar"}
                </button>
              </div>
            )}

            {import.meta?.env?.DEV && this.state.erroEncontrado?.stack && (
              <details className={estilos.detalhesTecnicos}>
                <summary>Detalhes técnicos de desenvolvimento</summary>
                <pre className={estilos.caixaPilhaErro}>{this.state.erroEncontrado.stack}</pre>
              </details>
            )}

            <div className={estilos.grupoBotoes}>
              <button type="button" onClick={this.tentarNovamente} className={`${estilos.botaoAcao} ${estilos.botaoTentarNovamente}`}>
                <RotateCcw size={16} aria-hidden="true" /> Tentar novamente
              </button>
              <button type="button" onClick={this.recarregarPagina} className={`${estilos.botaoAcao} ${estilos.botaoRecarregar}`}>
                <RefreshCw size={16} aria-hidden="true" /> Recarregar aplicação
              </button>
            </div>
          </div>
        </section>
      </main>
    );
  }
}
