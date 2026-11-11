import React from "react";
import { Layers3 } from "lucide-react";
import { CartaoLotacao } from "../CartaoLotacao";
import estilos from "./index.module.css";

export const ListaLotacoes = ({
  carregando,
  lotacoes,
  turnos,
  aoEditar,
  aoExcluir,
  estadoSelecionado,
  podeEscrever = false,
}: { carregando: (boolean); lotacoes: import("../../../../../../contracts/opcoes-apresentacao").LocationView[]; turnos: (import("../../../../../../contracts/java").JornadaDTO[]); aoEditar: (item: import("../../../../../../contracts/opcoes-apresentacao").LocationView) => void; aoExcluir: (id: number | null) => void; estadoSelecionado: (string); podeEscrever?: (boolean) }) => {
  if (carregando) {
    return (
      <div className={estilos.estadoLista} aria-live="polite">
        <span className={estilos.spinner} />
        <h3>Carregando lotações...</h3>
        <p>Organizando as unidades por estado e Gestão Local.</p>
      </div>
    );
  }

  if (lotacoes.length === 0) {
    return (
      <div className={estilos.estadoLista}>
        <span className={estilos.iconeVazio}><Layers3 size={34} /></span>
        <h3>Nenhuma lotação encontrada</h3>
        <p>
          Não existem resultados para {estadoSelecionado === "TODOS" ? "os filtros atuais" : `o estado ${estadoSelecionado}`}.
        </p>
      </div>
    );
  }

  const turnosPorId = new Map(
    (Array.isArray(turnos) ? turnos : []).map((turno) => [String(turno.id), turno]),
  );

  return (
    <section className={estilos.gradeCartoes} aria-label="Lotações filtradas">
      {lotacoes.map((lotacao) => (
        <CartaoLotacao
          key={lotacao.id}
          lotacao={lotacao}
          turnosPorId={turnosPorId}
          aoEditar={aoEditar}
          aoExcluir={aoExcluir}
          podeEscrever={podeEscrever}
        />
      ))}
    </section>
  );
};
