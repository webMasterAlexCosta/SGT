import React, { useId } from "react";
import { AlertTriangle, Inbox, LoaderCircle, RefreshCw } from "lucide-react";
import { Botao } from "./EnvolucroDica.tsx";
import estilos from "./EstruturaPagina.module.css";

export const EstruturaPagina = ({
  titulo,
  subtitulo = "",
  acoes = null,
  filtros = null,
  quantidadeFiltrosAtivos = 0,
  aoLimparFiltros,
  carregando = false,
  erro = null,
  aoTentarNovamente,
  vazio = false,
  tituloVazio = "Nenhum registro encontrado",
  descricaoVazio = "Ajuste os filtros ou tente novamente mais tarde.",
  children,
  className = "",
}: React.PropsWithChildren<{ titulo: React.ReactNode; subtitulo?: React.ReactNode; acoes?: React.ReactNode; filtros?: React.ReactNode; quantidadeFiltrosAtivos?: number; aoLimparFiltros?: () => void; carregando?: boolean; erro?: string | { message?: string } | null; aoTentarNovamente?: () => void; vazio?: boolean; tituloVazio?: string; descricaoVazio?: string; className?: string }>) => {
  const id = useId().replace(/:/g, "");
  const idTitulo = `pagina-${id}-titulo`;
  const mensagemErro = typeof erro === "string" ? erro : erro?.message;

  return (
    <section className={`${estilos.pagina} ${className}`} aria-labelledby={idTitulo}>
      <header className={estilos.cabecalho}>
        <div className={estilos.textosCabecalho}>
          <h1 id={idTitulo} className={estilos.titulo}>{titulo}</h1>
          {subtitulo && <p className={estilos.subtitulo}>{subtitulo}</p>}
        </div>
        {acoes && <div className={estilos.acoes}>{acoes}</div>}
      </header>

      {filtros && (
        <section className={estilos.filtros} aria-label="Filtros da página">
          <div className={estilos.filtrosConteudo}>{filtros}</div>
          {(quantidadeFiltrosAtivos > 0 || aoLimparFiltros) && (
            <div className={estilos.resumoFiltros}>
              <span aria-live="polite">
                {quantidadeFiltrosAtivos === 1
                  ? "1 filtro ativo"
                  : `${quantidadeFiltrosAtivos} filtros ativos`}
              </span>
              {aoLimparFiltros && quantidadeFiltrosAtivos > 0 && (
                <Botao variant="ghost" size="sm" onClick={aoLimparFiltros}>
                  Limpar filtros
                </Botao>
              )}
            </div>
          )}
        </section>
      )}

      <div className={estilos.conteudo} aria-live={carregando ? "polite" : undefined}>
        {carregando ? (
          <EstadoPagina
            icone={LoaderCircle}
            titulo="Carregando dados"
            descricao="Aguarde enquanto as informações são atualizadas."
            girando
          />
        ) : mensagemErro ? (
          <EstadoPagina
            icone={AlertTriangle}
            titulo="Não foi possível carregar os dados"
            descricao={mensagemErro}
            tipo="erro"
            acao={aoTentarNovamente ? (
              <Botao variant="outline" onClick={aoTentarNovamente}>
                <RefreshCw size={16} aria-hidden="true" /> Tentar novamente
              </Botao>
            ) : null}
          />
        ) : vazio ? (
          <EstadoPagina icone={Inbox} titulo={tituloVazio} descricao={descricaoVazio} />
        ) : children}
      </div>
    </section>
  );
};

export const EstadoPagina = ({
  icone: Icone = Inbox,
  titulo,
  descricao = "",
  tipo = "neutro",
  acao = null,
  girando = false,
}: { icone?: import("lucide-react").LucideIcon; titulo: React.ReactNode; descricao?: React.ReactNode; tipo?: string; acao?: React.ReactNode; girando?: boolean }) => (
  <div className={`${estilos.estado} ${tipo === "erro" ? estilos.estadoErro : ""}`} role={tipo === "erro" ? "alert" : "status"}>
    <Icone className={girando ? estilos.iconeGirando : estilos.iconeEstado} size={30} aria-hidden="true" />
    <strong>{titulo}</strong>
    {descricao && <p>{descricao}</p>}
    {acao && <div className={estilos.acaoEstado}>{acao}</div>}
  </div>
);
