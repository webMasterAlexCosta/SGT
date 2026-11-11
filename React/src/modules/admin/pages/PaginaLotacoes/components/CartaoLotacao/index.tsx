import React from "react";
import {
  Building2,
  CalendarDays,
  Clock3,
  Edit3,
  MapPin,
  Trash2,
} from "lucide-react";
import {
  formatarGestorEstadual,
  formatarGestaoLocal,
} from "../../../../../../core/formatters/nomenclaturaGestao.ts";
import { resumirHorariosDoTurno } from "../../resumoHorarios.ts";
import estilos from "./index.module.css";

export const CartaoLotacao = ({
  lotacao,
  turnosPorId,
  aoEditar,
  aoExcluir,
  podeEscrever = false,
}: { lotacao: import("../../../../../../contracts/opcoes-apresentacao").LocationView; turnosPorId: Map<string, import("../../../../../../contracts/java").JornadaDTO>; aoEditar: (item: import("../../../../../../contracts/opcoes-apresentacao").LocationView) => void; aoExcluir: (id: number | null) => void; podeEscrever?: (boolean) }) => {
  const turnos = Array.isArray(lotacao.turnos) ? lotacao.turnos : [];

  return (
    <article className={estilos.cartaoLotacao}>
      <header className={estilos.cabecalhoCartao}>
        <div className={estilos.identidade}>
          <span className={estilos.seloEstado}>{lotacao.uf || "UF"}</span>
          <div>
            <h3>{lotacao.nome} {lotacao.ativo === false && <small className={estilos.inativo}>Inativa</small>}</h3>
            <span className={estilos.municipio}>
              <MapPin size={12} /> {lotacao.municipio || "Município não informado"}
            </span>
          </div>
        </div>
        {podeEscrever && <div className={estilos.acoesCartao}>
          <button type="button" onClick={() => aoEditar(lotacao)} title="Editar lotação">
            <Edit3 size={15} />
          </button>
          <button
            type="button"
            onClick={() => aoExcluir(lotacao.id)}
            className={estilos.botaoExcluir}
            title="Inativar lotação"
          >
            <Trash2 size={15} />
          </button>
        </div>}
      </header>

      <div className={estilos.faixaGestao}>
        <Building2 size={14} />
        <div>
          <span>Responsável</span>
          <strong>
            {lotacao.gestaoLocalNome ? formatarGestaoLocal(lotacao.gestaoLocalNome) : "Gestão Local"}
          </strong>
          <small>
            {formatarGestorEstadual(lotacao.superintendenciaNome, lotacao.uf)}
          </small>
        </div>
      </div>

      <div className={estilos.corpoCartao}>
        <div className={estilos.tituloSecao}>
          <span><CalendarDays size={14} /> Turnos vinculados</span>
          <strong>{turnos.length}</strong>
        </div>

        {turnos.length === 0 ? (
          <div className={estilos.semTurnos}>Nenhum turno definido nesta unidade.</div>
        ) : (
          <div className={estilos.listaTurnos}>
            {turnos.map((turnoResumo) => {
              const turno = turnosPorId.get(String(turnoResumo.id)) || turnoResumo;
              return (
                <div key={turnoResumo.id} className={estilos.itemTurno}>
                  <div className={estilos.nomeTurno}>
                    <Clock3 size={13} />
                    <strong>{turnoResumo.descricao || turno.descricao}</strong>
                  </div>
                  <p>{resumirHorariosDoTurno(turno)}</p>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </article>
  );
};
