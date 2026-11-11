import React from "react";
import { Plus, Search, XCircle, CalendarDays } from "lucide-react";
import estilos from "./index.module.css";

export const CabecalhoHero = ({
  termoDeBusca,
  aoMudarTermoDeBusca,
  aoAbrirNovoTurno,
  resumoExecutivo = {},
  podeEscrever = false,
}: { termoDeBusca: string; aoMudarTermoDeBusca: React.Dispatch<React.SetStateAction<string>>; aoAbrirNovoTurno: () => void; resumoExecutivo?: Partial<{ totalDeTurnos: number; turnosComFimDeSemana: number; turnosNoturnos: number; }>; podeEscrever?: boolean }) => {
  return (
    <header className={estilos.secaoHero}>
      <div className={estilos.linhaSuperior}>
        
        {/* LADO ESQUERDO: Título e Ícone */}
        <div className={estilos.blocoTitulo}>
          <div className={estilos.iconeDestaque}>
            <CalendarDays size={24} />
          </div>
          <div className={estilos.textosTitulo}>
            <span className={estilos.sobretitulo}>Jornadas e escalas</span>
            <h1>Turnos de trabalho</h1>
            <p>Configure horários, carga semanal e distribuição dos dias de trabalho.</p>
          </div>
        </div>

        {/* LADO DIREITO: Métrica, Busca e Botão */}
        <div className={estilos.blocoAcoes}>
          <div className={estilos.cartaoMetrica}>
            <div className={estilos.iconeMetrica}>
              <CalendarDays size={20} />
            </div>
            <div className={estilos.textosMetrica}>
              <span>Turnos Cadastrados</span>
              <strong>{resumoExecutivo.totalDeTurnos ?? 0} turno(s)</strong>
            </div>
          </div>

          <div className={estilos.campoBusca}>
            <Search size={18} className={estilos.iconeBusca} />
            <input
              type="text"
              placeholder="Buscar por nome ou ID..."
              aria-label="Buscar turnos por nome ou ID"
              value={termoDeBusca || ""}
              onChange={(evento) => aoMudarTermoDeBusca(evento.target.value)}
            />
            {termoDeBusca && (
              <button
                type="button"
                onClick={() => aoMudarTermoDeBusca("")}
                className={estilos.botaoLimpar}
                aria-label="Limpar busca de turnos"
                title="Limpar busca"
              >
                <XCircle size={16} />
              </button>
            )}
          </div>

          {podeEscrever && (
            <button
              type="button"
              onClick={aoAbrirNovoTurno}
              className={estilos.botaoNovaEscala}
            >
              <Plus size={18} /> Novo Turno
            </button>
          )}
        </div>
      </div>
    </header>
  );
};