import React from "react";
import {
  AlertTriangle,
  CalendarDays,
  CheckCircle2,
  Filter,
  Moon,
  SlidersHorizontal,
  SortDesc,
  TimerReset,
  Waves,
} from "lucide-react";
import { CONFIG } from "../../../../../../core/config";
import { FiltroEstado } from "../../../../components/FiltroEstado";
import estilos from "./index.module.css";

export const FiltrosAvancados = ({
  totalDeResultados,
  contagensDeFiltro,
  filtroAtivo,
  aoDefinirFiltroAtivo,
  ordenacaoAtual,
  aoDefinirOrdenacao,
  exibirFiltroEstado = false,
  estadoSelecionado,
  aoDefinirEstado,
  contagensPorEstado,
}: { totalDeResultados: number; contagensDeFiltro: { TODAS: number; FIM_DE_SEMANA: number; NOTURNAS: number; ATE_LIMIAR_INF: number; PROX_REF: number; ACIMA_REF: number; }; filtroAtivo: string; aoDefinirFiltroAtivo: React.Dispatch<React.SetStateAction<string>>; ordenacaoAtual: string; aoDefinirOrdenacao: React.Dispatch<React.SetStateAction<string>>; exibirFiltroEstado?: boolean; estadoSelecionado: string; aoDefinirEstado: React.Dispatch<React.SetStateAction<string>>; contagensPorEstado: Record<string, number> }) => {
  const LISTA_DE_FILTROS = [
    {
      id: "TODAS",
      rotulo: "Todas",
      contagem: contagensDeFiltro.TODAS,
      Icone: CheckCircle2,
    },
    {
      id: "FIM_DE_SEMANA",
      rotulo: "Fim de semana",
      contagem: contagensDeFiltro.FIM_DE_SEMANA,
      Icone: CalendarDays,
    },
    {
      id: "NOTURNAS",
      rotulo: "Noturnas",
      contagem: contagensDeFiltro.NOTURNAS,
      Icone: Moon,
    },
    {
      id: "ATE_LIMIAR_INF",
      rotulo: `Até ${CONFIG.JORNADA_LIMIAR_INFERIOR}h`,
      contagem: contagensDeFiltro.ATE_LIMIAR_INF,
      Icone: TimerReset,
    },
    {
      id: "PROX_REF",
      rotulo: `Próx. ${CONFIG.JORNADA_LIMIAR_MEDIO}h`,
      contagem: contagensDeFiltro.PROX_REF,
      Icone: Waves,
    },
    {
      id: "ACIMA_REF",
      rotulo: `Acima ${CONFIG.JORNADA_REFERENCIA_HORAS}h`,
      contagem: contagensDeFiltro.ACIMA_REF,
      Icone: AlertTriangle,
      perigo: true,
    },
  ];

  return (
    <section className={estilos.barraDeFiltros}>
      <div className={estilos.cabecalhoDaBarra}>
        <div className={estilos.infoDeResultados}>
          <span className={estilos.iconeDaInfo}>
            <Filter size={17} />
          </span>
          <div>
            <strong>{totalDeResultados}</strong>
            <span>resultado(s) exibidos</span>
          </div>
        </div>

        <div className={estilos.controlesDoCabecalho}>
          {exibirFiltroEstado && (
            <FiltroEstado
              id="filtro-estado-turnos"
              valor={estadoSelecionado}
              aoMudar={aoDefinirEstado}
              contagens={contagensPorEstado}
              rotulo="Estado dos turnos"
            />
          )}
          <div className={estilos.envoltorioDaOrdenacao}>
            <SortDesc size={16} className={estilos.iconePrimario} />
            <select
              value={ordenacaoAtual}
              onChange={(evento) => aoDefinirOrdenacao(evento.target.value)}
              className={estilos.caixaDeOrdenacao}
              aria-label="Ordenar turnos"
            >
              <option value="NOME_AZ">Ordem A-Z</option>
              <option value="MAIOR_CARGA">Maior carga</option>
              <option value="MENOR_CARGA">Menor carga</option>
            </select>
          </div>
        </div>
      </div>

      <div className={estilos.grupoDeMarcadores}>
        <span className={estilos.rotuloDaLinhaDeFiltros}>
          <SlidersHorizontal size={14} /> Filtros rápidos
        </span>
        {LISTA_DE_FILTROS.map(({ id, rotulo, contagem, Icone, perigo }) => (
          <button
            key={id}
            className={`${estilos.marcadorFiltro} ${filtroAtivo === id ? estilos.marcadorAtivo : ""} ${perigo ? estilos.marcadorPerigo : ""}`}
            onClick={() => aoDefinirFiltroAtivo(id)}
          >
            <Icone size={14} />
            {rotulo}
            <span className={estilos.emblemaContagem}>{contagem}</span>
          </button>
        ))}
      </div>
    </section>
  );
};
