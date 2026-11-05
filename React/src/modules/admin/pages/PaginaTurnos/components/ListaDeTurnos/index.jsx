import React from 'react';
import { LayoutDashboard } from 'lucide-react';
import { CartaoDeTurno } from '../CartaoDeTurno';
import estilos from './index.module.css';

export const ListaDeTurnos = ({ carregandoOsDados, turnosProcessadosNaLista, aoEditarTurno, aoExcluirTurno, podeEscrever = false }) => {
  if (carregandoOsDados) {
    return (
      <div className={estilos.estadoVazioDaLista}>
        <div className={estilos.iconeGiradorDeCarregamento}></div>
        <h3>Carregando dados da jornada...</h3>
      </div>
    );
  }

  if (turnosProcessadosNaLista.length === 0) {
    return (
      <div className={estilos.estadoVazioDaLista}>
        <div className={estilos.iconeDeListaVazia}><LayoutDashboard size={48} /></div>
        <h3>Nenhum turno encontrado</h3>
        <p>Altere os filtros ou adicione uma nova rotina para visualizar aqui.</p>
      </div>
    );
  }

  return (
    <section className={estilos.conteinerDaGradeDeCartoes}>
      <div className={estilos.gradeDeVisualizacaoDeCartoes}>
        {turnosProcessadosNaLista.map(turnoAtual => (
          <CartaoDeTurno 
            key={turnoAtual.id} 
            turnoData={turnoAtual} 
            aoEditarTurno={aoEditarTurno} 
            aoExcluirTurno={aoExcluirTurno}
            podeEscrever={podeEscrever}
          />
        ))}
      </div>
    </section>
  );
};