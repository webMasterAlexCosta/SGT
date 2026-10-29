package com.example.siggtrat.servico;

import com.example.siggtrat.repositorio.AditamentoContratualRepositorio;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.ComposicaoContratualRepositorio;
import com.example.siggtrat.repositorio.RemanejamentoContratualRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/** Impede inativação de catálogo ainda usado por programação operacional futura. */
@Service
@Transactional(readOnly = true)
public class ProgramacaoFuturaCatalogoServico {

    public enum TipoCatalogo {
        CARGO("Cargo"), LOTACAO("Lotação"), JORNADA("Turno");

        private final String rotulo;
        TipoCatalogo(String rotulo) { this.rotulo = rotulo; }
    }

    private final ComposicaoContratualRepositorio composicoes;
    private final AlocacaoVinculoRepositorio alocacoes;
    private final AditamentoContratualRepositorio aditamentos;
    private final RemanejamentoContratualRepositorio remanejamentos;

    public ProgramacaoFuturaCatalogoServico(
            ComposicaoContratualRepositorio composicoes,
            AlocacaoVinculoRepositorio alocacoes,
            AditamentoContratualRepositorio aditamentos,
            RemanejamentoContratualRepositorio remanejamentos) {
        this.composicoes = composicoes;
        this.alocacoes = alocacoes;
        this.aditamentos = aditamentos;
        this.remanejamentos = remanejamentos;
    }

    public void exigirSemProgramacaoFutura(TipoCatalogo tipo, Long catalogoId) {
        if (tipo == null || catalogoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catálogo inválido para inativação.");
        }
        LocalDate referencia = RelogioSistema.hoje().plusDays(1);
        long usos = switch (tipo) {
            case CARGO -> composicoes.contarUsoProgramadoPorCargo(catalogoId, referencia)
                    + alocacoes.contarUsoProgramadoPorCargo(catalogoId, referencia)
                    + aditamentos.contarUsoFuturoPorCargo(catalogoId, referencia)
                    + remanejamentos.contarUsoFuturoPorCargo(catalogoId, referencia);
            case LOTACAO -> composicoes.contarUsoProgramadoPorLotacao(catalogoId, referencia)
                    + alocacoes.contarUsoProgramadoPorLotacao(catalogoId, referencia)
                    + aditamentos.contarUsoFuturoPorLotacao(catalogoId, referencia)
                    + remanejamentos.contarUsoFuturoPorLotacao(catalogoId, referencia);
            case JORNADA -> composicoes.contarUsoProgramadoPorJornada(catalogoId, referencia)
                    + alocacoes.contarUsoProgramadoPorJornada(catalogoId, referencia)
                    + aditamentos.contarUsoFuturoPorJornada(catalogoId, referencia)
                    + remanejamentos.contarUsoFuturoPorJornada(catalogoId, referencia);
        };
        if (usos > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    tipo.rotulo + " possui programação futura. Trate ou cancele os fatos programados antes da inativação.");
        }
    }
}
