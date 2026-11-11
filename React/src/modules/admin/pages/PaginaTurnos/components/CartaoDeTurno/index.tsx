import type { JornadaDTO } from '../../../../../../contracts/java';
import React from "react";
import {
  AlertTriangle,
  CalendarDays,
  CalendarOff,
  CheckCircle2,
  ClipboardList,
  Clock,
  Building2,
  Edit,
  MapPin,
  Moon,
  Timer,
  Trash2,
  TrendingUp,
} from "lucide-react";
import { CONFIG } from "../../../../../../core/config";
import {
  formatarParaHoraDecimal,
  formatarParaHorasEMinutos,
  analisarEscalaCompleta,
  obterInformacoesDoDia,
  ORDEM_LISTAGEM,
} from "../../../../../../services/admin/ServicoDominioJornada.ts";
import estilos from "./index.module.css";

export const CartaoDeTurno = ({ turnoData = {}, aoEditarTurno = () => {}, aoExcluirTurno = () => {}, podeEscrever = false }: { turnoData?: Partial<JornadaDTO> & { analise?: ReturnType<typeof analisarEscalaCompleta> }; aoEditarTurno?: (id: number | null | undefined) => void; aoExcluirTurno?: (id: number | null | undefined) => void; podeEscrever?: boolean }) => {
  const analise =
    turnoData?.analise && Array.isArray(turnoData.analise.dias)
      ? turnoData.analise
      : analisarEscalaCompleta(turnoData);
  const mapaDeDias = new Map(analise.dias.map((dia) => [dia.diaSemana, dia]));
  const cargaCalculada = analise.minutosSemanais / 60;
  const porcentagemReal = Math.round(
    (cargaCalculada / CONFIG.JORNADA_REFERENCIA_HORAS) * 100,
  );
  const porcentagemDaBarra = Math.min(100, Math.max(0, porcentagemReal));

  const classeDeCarga =
    cargaCalculada > CONFIG.JORNADA_REFERENCIA_HORAS
      ? estilos.cargaExcedida
      : cargaCalculada <= CONFIG.JORNADA_LIMIAR_INFERIOR
        ? estilos.cargaReduzida
        : estilos.cargaDentroDoPadrao;

  const textoDoStatus =
    cargaCalculada > CONFIG.JORNADA_REFERENCIA_HORAS
      ? "Acima da ref."
      : "Validado";

  const classeRodape =
    porcentagemReal === 100
      ? estilos.rodapeDaCarga
      : `${estilos.rodapeDaCarga} ${estilos.rodapeAlerta}`;

  return (
    <article className={estilos.cartaoGeral}>
      <header className={estilos.cabecalhoDoCartao}>
        <div className={estilos.linhaDeTitulo}>
          <div className={estilos.identidadeDoTurno}>
            {/* <div className={estilos.iconeDoTurno}>
              <CalendarDays size={20} />
            </div> */}
            <div className={estilos.textosDoTitulo}>
              <h3 title={(turnoData.descricao) ?? undefined}>{turnoData.descricao} {turnoData.ativo === false && <small className={estilos.seloInativo}>Inativo</small>}</h3>
              <div className={estilos.subtituloDoCartao}>
                <span>ID #{turnoData.id}</span>
                <span className={estilos.pontoSeparador}>•</span>
                <span>{analise.quantidadeDiasTrabalho} dias na escala</span>
              </div>
            </div>
          </div>
          {podeEscrever && (
            <div className={estilos.acoesDoTurno}>
              <button
                type="button"
                onClick={() => aoEditarTurno(turnoData.id)}
                className={estilos.botaoDeAcao}
                title="Editar turno"
                aria-label={`Editar ${turnoData.descricao || "turno"}`}
              >
                <Edit size={15} />
              </button>
              {turnoData.ativo !== false && (
                <button
                  type="button"
                  onClick={() => aoExcluirTurno(turnoData.id)}
                  className={`${estilos.botaoDeAcao} ${estilos.botaoDeAcaoPerigo}`}
                  title="Inativar turno"
                  aria-label={`Inativar ${turnoData.descricao || "turno"}`}
                >
                  <Trash2 size={15} />
                </button>
              )}
            </div>
          )}
        </div>
        <div className={estilos.identificacaoOrganizacional}>
          <span className={estilos.seloEstado}>
            <MapPin size={12} /> {turnoData.uf || "UF não informada"}
          </span>
          <span
            className={estilos.nomeGestor}
            title={turnoData.gestaoLocalNome || "Gestão Local não informada"}
          >
            <Building2 size={12} />
            {turnoData.gestaoLocalNome || "Gestão Local não informada"}
          </span>
        </div>
      </header>

      <div className={estilos.corpoDoCartao}>
        <div className={`${estilos.secaoDeProgresso} ${classeDeCarga}`}>
          <div className={estilos.linhaDeRotuloDaCarga}>
            <div className={estilos.infoCargaPrincipal}>
              <span className={estilos.tituloDaCarga}>Carga Semanal</span>
              <strong className={estilos.valorDaCarga}>
                {formatarParaHorasEMinutos(analise.minutosSemanais)}
              </strong>
            </div>
            <div className={estilos.seloDeStatusDaCarga}>
              {cargaCalculada > CONFIG.JORNADA_REFERENCIA_HORAS ? (
                <AlertTriangle size={14} />
              ) : (
                <CheckCircle2 size={14} />
              )}
              {textoDoStatus}
            </div>
          </div>

          <div className={estilos.fundoDaBarraDeCarga} aria-hidden="true">
            <div
              className={estilos.preenchimentoDaBarraDeCarga}
              style={{ width: `${porcentagemDaBarra}%` }}
            />
          </div>

          <div className={classeRodape}>
            <span>
              <TrendingUp size={14} className={estilos.iconePercentual} /> {porcentagemReal}%
              atingido
            </span>
            <span>Ref: {CONFIG.JORNADA_REFERENCIA_HORAS}h</span>
          </div>
        </div>

        <div className={estilos.etiquetasDoCartao}>
          {analise.quantidadeDoisPeriodos > 0 && (
            <span className={estilos.etiquetaPadrao}>
              <Clock size={12} /> {analise.quantidadeDoisPeriodos}x períodos
            </span>
          )}
          {analise.possuiFimDeSemana && (
            <span className={estilos.etiquetaAviso}>
              <ClipboardList size={12} /> Fim de semana
            </span>
          )}
          {analise.atravessaMeiaNoite && (
            <span className={estilos.etiquetaNoturna}>
              <Moon size={12} /> Noturno
            </span>
          )}
        </div>

        <div className={estilos.miniGradeDeDias}>
          {ORDEM_LISTAGEM.map((idDoDia) => {
            const informacaoDoDia = obterInformacoesDoDia(idDoDia);
            const diaAtual = mapaDeDias.get(idDoDia);
            const trabalhaNesseDia = Boolean(diaAtual?.diaTrabalho);

            let descricaoHorario = "Folga";

            if (trabalhaNesseDia) {
              if ((diaAtual?.periodos?.length || 0) > 0) {
                // Mapeia os períodos configurados para texto (ex: "08:00 às 12:00")
                const textoDosPeriodos = diaAtual?.periodos
                  .map((p) => `${p.entrada} às ${p.saida}`)
                  .join(" e ");
                
                descricaoHorario = `${textoDosPeriodos} (${formatarParaHorasEMinutos(diaAtual?.minutos)})`;
              } else {
                // Fallback de segurança
                descricaoHorario = `${formatarParaHorasEMinutos(diaAtual?.minutos)} trabalhadas`;
              }
            }

            return (
              <div
                key={idDoDia}
                className={`${estilos.caixaMiniDoDia} ${trabalhaNesseDia ? estilos.miniDiaAtivo : estilos.miniDiaDeFolga}`}
                title={`${informacaoDoDia.nome}: ${descricaoHorario}`}
              >
                <span>{informacaoDoDia.curto.substring(0, 3)}</span>
                <small>
                  {trabalhaNesseDia
                    ? formatarParaHorasEMinutos(diaAtual?.minutos)
                    : "—"}
                </small>
              </div>
            );
          })}
        </div>
      </div>
    </article>
  );
};
