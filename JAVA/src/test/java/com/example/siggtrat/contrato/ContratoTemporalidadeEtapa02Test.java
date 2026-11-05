package com.example.siggtrat.contrato;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EstadoNegocialContrato;
import com.example.siggtrat.entidade.HabilitacaoAdministrativaContrato;
import com.example.siggtrat.entidade.SituacaoTemporalContrato;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ContratoTemporalidadeEtapa02Test {

    @Test
    void futuroVigenteEEncerradoSaoDerivadosDaDataENaoGravadosComoEstadoNegocial() {
        Contrato contrato = new Contrato();
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setDataInicio(LocalDate.of(2026, 9, 10));
        contrato.setDataFim(LocalDate.of(2026, 12, 31));

        assertEquals(SituacaoTemporalContrato.FUTURO,
                contrato.situacaoTemporalEm(LocalDate.of(2026, 9, 9)));
        assertEquals(SituacaoTemporalContrato.VIGENTE,
                contrato.situacaoTemporalEm(LocalDate.of(2026, 9, 10)));
        assertEquals(SituacaoTemporalContrato.VIGENTE,
                contrato.situacaoTemporalEm(LocalDate.of(2026, 12, 31)));
        assertEquals(SituacaoTemporalContrato.ENCERRADO,
                contrato.situacaoTemporalEm(LocalDate.of(2027, 1, 1)));
    }

    @Test
    void rascunhoCanceladoEAnuladoDependemDeEstadoNegocialExplicito() {
        Contrato contrato = new Contrato();
        contrato.setDataInicio(LocalDate.of(2026, 10, 1));
        contrato.setDataFim(LocalDate.of(2026, 12, 31));

        contrato.setEstadoNegocial(EstadoNegocialContrato.RASCUNHO);
        assertEquals(SituacaoTemporalContrato.RASCUNHO,
                contrato.situacaoTemporalEm(LocalDate.of(2026, 9, 1)));

        contrato.setEstadoNegocial(EstadoNegocialContrato.CANCELADO);
        assertEquals(SituacaoTemporalContrato.CANCELADO,
                contrato.situacaoTemporalEm(LocalDate.of(2026, 11, 1)));

        contrato.setEstadoNegocial(EstadoNegocialContrato.ANULADO);
        assertEquals(SituacaoTemporalContrato.ANULADO,
                contrato.situacaoTemporalEm(LocalDate.of(2026, 11, 1)));
    }

    @Test
    void habilitacaoAdministrativaEhEixoIndependenteDaSituacaoTemporal() {
        Contrato contrato = new Contrato();
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setDataInicio(LocalDate.of(2026, 1, 1));
        contrato.setDataFim(LocalDate.of(2026, 12, 31));
        contrato.setAtivo(false);

        assertEquals(HabilitacaoAdministrativaContrato.INATIVO,
                contrato.getHabilitacaoAdministrativa());
        assertEquals(SituacaoTemporalContrato.VIGENTE,
                contrato.situacaoTemporalEm(LocalDate.of(2026, 9, 8)));
        assertFalse(contrato.podeOperarEm(LocalDate.of(2026, 9, 8)));
    }

    @Test
    void identificacaoTemRestricaoCompostaNumeroEAno() {
        Table table = Contrato.class.getAnnotation(Table.class);
        assertNotNull(table);

        Set<Set<String>> restricoes = Arrays.stream(table.uniqueConstraints())
                .map(UniqueConstraint::columnNames)
                .map(Arrays::stream)
                .map(stream -> stream.collect(Collectors.toSet()))
                .collect(Collectors.toSet());

        assertTrue(restricoes.contains(Set.of("numero_contrato", "ano_contrato")));
    }
}
