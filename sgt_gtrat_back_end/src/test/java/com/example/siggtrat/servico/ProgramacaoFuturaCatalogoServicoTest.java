package com.example.siggtrat.servico;

import com.example.siggtrat.repositorio.AditamentoContratualRepositorio;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.ComposicaoContratualRepositorio;
import com.example.siggtrat.repositorio.RemanejamentoContratualRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramacaoFuturaCatalogoServicoTest {

    @Mock private ComposicaoContratualRepositorio composicoes;
    @Mock private AlocacaoVinculoRepositorio alocacoes;
    @Mock private AditamentoContratualRepositorio aditamentos;
    @Mock private RemanejamentoContratualRepositorio remanejamentos;
    private ProgramacaoFuturaCatalogoServico servico;

    @BeforeEach
    void criarServico() {
        servico = new ProgramacaoFuturaCatalogoServico(composicoes, alocacoes, aditamentos, remanejamentos);
    }

    @Test
    void permiteInativacaoSemProgramacaoFutura() {
        assertDoesNotThrow(() -> servico.exigirSemProgramacaoFutura(
                ProgramacaoFuturaCatalogoServico.TipoCatalogo.LOTACAO, 10L));
    }

    @Test
    void bloqueiaCargoUsadoEmComposicaoProgramada() {
        when(composicoes.contarUsoProgramadoPorCargo(eq(7L), any(LocalDate.class))).thenReturn(1L);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.exigirSemProgramacaoFutura(
                        ProgramacaoFuturaCatalogoServico.TipoCatalogo.CARGO, 7L));

        assertEquals(409, erro.getStatusCode().value());
    }

    @Test
    void bloqueiaJornadaUsadaEmAditamentoFuturo() {
        when(aditamentos.contarUsoFuturoPorJornada(eq(9L), any(LocalDate.class))).thenReturn(1L);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.exigirSemProgramacaoFutura(
                        ProgramacaoFuturaCatalogoServico.TipoCatalogo.JORNADA, 9L));

        assertEquals(409, erro.getStatusCode().value());
    }

    @Test
    void rejeitaIdentificadorAusente() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.exigirSemProgramacaoFutura(
                        ProgramacaoFuturaCatalogoServico.TipoCatalogo.CARGO, null));

        assertEquals(400, erro.getStatusCode().value());
    }
}
