package com.example.siggtrat.repositorio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PainelRepositorioSqlConsistenciaTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock NamedParameterJdbcTemplate namedJdbcTemplate;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void padraoSemanalEhCompativelComOnlyFullGroupBy() {
        when(namedJdbcTemplate.query(
                any(String.class),
                any(SqlParameterSource.class),
                any(RowMapper.class)
        )).thenReturn(List.of());

        PainelRepositorio repositorio = new PainelRepositorio(jdbcTemplate, namedJdbcTemplate, org.mockito.Mockito.mock(PainelEfetivoRepositorio.class));
        repositorio.obterPadraoSemanal(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                null,
                null
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(namedJdbcTemplate).query(
                sqlCaptor.capture(),
                any(SqlParameterSource.class),
                any(RowMapper.class)
        );

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("GROUP BY WEEKDAY(f.data_referencia)"));
        assertFalse(sql.contains("CASE WEEKDAY(f.data_referencia)"));
        assertFalse(sql.contains("AS dia"));
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void rankingHistoricoPriorizaSnapshotsDeLotacaoJornadaEContrato() {
        when(namedJdbcTemplate.query(
                any(String.class),
                any(SqlParameterSource.class),
                any(RowMapper.class)
        )).thenReturn(List.of());

        PainelRepositorio repositorio = new PainelRepositorio(jdbcTemplate, namedJdbcTemplate, org.mockito.Mockito.mock(PainelEfetivoRepositorio.class));
        repositorio.obterRankingLotacoes(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                99L,
                10L,
                null
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(namedJdbcTemplate).query(
                sqlCaptor.capture(),
                any(SqlParameterSource.class),
                any(RowMapper.class)
        );

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("SELECT f.lotacao_snapshot_id AS lotacao_id"));
        assertTrue(sql.contains("f.jornada_snapshot_id AS jornada_id"));
        assertTrue(sql.contains("f.lotacao_snapshot_id IS NOT NULL"));
        assertTrue(sql.contains("f.jornada_snapshot_id IS NOT NULL"));
        assertTrue(sql.contains("f.contrato_snapshot_id = :contratoId"));
        assertTrue(sql.contains("f.gestao_local_snapshot_id = :scopeGestaoLocalId"));
    }

}
