package com.example.siggtrat.repositorio;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Consulta somente identificadores para paginar relatórios históricos de grande
 * escala. As entidades completas e as frequências são carregadas depois, em
 * lotes pequenos, preservando a regra de negócio existente sem materializar a
 * população inteira na memória da JVM.
 */
@Repository
public class RelatorioFrequenciaEscalaRepositorio {

    public record EscopoOperacional(Long lotacaoId, Long jornadaId) {}

    private final NamedParameterJdbcTemplate jdbc;

    public RelatorioFrequenciaEscalaRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long contarColaboradores(
            Collection<Long> contratoIds,
            ZonedDateTime inicio,
            ZonedDateTime fimExclusivo,
            Long lotacaoId,
            Long jornadaId,
            Long gestaoLocalOperacionalId,
            List<EscopoOperacional> escoposOperacionais
    ) {
        if (contratoIds == null || contratoIds.isEmpty()) return 0L;
        Consulta consulta = montarConsulta(contratoIds, inicio, fimExclusivo, lotacaoId, jornadaId,
                gestaoLocalOperacionalId, escoposOperacionais);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT h.id_empregado) " + consulta.fromWhere(),
                consulta.parametros(), Long.class);
        return total == null ? 0L : total;
    }

    public List<Long> buscarIdsColaboradores(
            Collection<Long> contratoIds,
            ZonedDateTime inicio,
            ZonedDateTime fimExclusivo,
            Long lotacaoId,
            Long jornadaId,
            Long gestaoLocalOperacionalId,
            List<EscopoOperacional> escoposOperacionais,
            long offset,
            int limite
    ) {
        if (contratoIds == null || contratoIds.isEmpty() || limite <= 0) return List.of();
        Consulta consulta = montarConsulta(contratoIds, inicio, fimExclusivo, lotacaoId, jornadaId,
                gestaoLocalOperacionalId, escoposOperacionais);
        MapSqlParameterSource parametros = new MapSqlParameterSource(consulta.parametros().getValues())
                .addValue("offset", Math.max(0L, offset))
                .addValue("limite", limite);
        String sql = """
                SELECT h.id_empregado
                """ + consulta.fromWhere() + """
                 GROUP BY h.id_empregado, e.nome
                 ORDER BY e.nome, h.id_empregado
                 LIMIT :limite OFFSET :offset
                """;
        return jdbc.query(sql, parametros, (rs, linha) -> rs.getLong("id_empregado"));
    }

    /**
     * Lote sequencial para exportação. Usa paginação por chave em vez de
     * OFFSET, evitando revarrer centenas de milhares de linhas a cada lote.
     */
    public List<Long> buscarIdsColaboradoresAposId(
            Collection<Long> contratoIds,
            ZonedDateTime inicio,
            ZonedDateTime fimExclusivo,
            Long lotacaoId,
            Long jornadaId,
            Long gestaoLocalOperacionalId,
            List<EscopoOperacional> escoposOperacionais,
            long ultimoId,
            int limite
    ) {
        if (contratoIds == null || contratoIds.isEmpty() || limite <= 0) return List.of();
        Consulta consulta = montarConsulta(contratoIds, inicio, fimExclusivo, lotacaoId, jornadaId,
                gestaoLocalOperacionalId, escoposOperacionais);
        MapSqlParameterSource parametros = new MapSqlParameterSource(consulta.parametros().getValues())
                .addValue("ultimoId", Math.max(0L, ultimoId))
                .addValue("limite", limite);
        String sql = """
                SELECT h.id_empregado
                """ + consulta.fromWhere() + """
                   AND h.id_empregado > :ultimoId
                 GROUP BY h.id_empregado
                 ORDER BY h.id_empregado
                 LIMIT :limite
                """;
        return jdbc.query(sql, parametros, (rs, linha) -> rs.getLong("id_empregado"));
    }

    private Consulta montarConsulta(
            Collection<Long> contratoIds,
            ZonedDateTime inicio,
            ZonedDateTime fimExclusivo,
            Long lotacaoId,
            Long jornadaId,
            Long gestaoLocalOperacionalId,
            List<EscopoOperacional> escoposOperacionais
    ) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("contratoIds", contratoIds)
                .addValue("inicio", java.sql.Timestamp.from(inicio.toInstant()))
                .addValue("fimExclusivo", java.sql.Timestamp.from(fimExclusivo.toInstant()))
                .addValue("lotacaoId", lotacaoId)
                .addValue("jornadaId", jornadaId)
                .addValue("gestaoLocalOperacionalId", gestaoLocalOperacionalId);

        StringBuilder sql = new StringBuilder("""
                  FROM historico_vinculo_contrato h
                  JOIN empregado e ON e.id_empregado = h.id_empregado
                  JOIN contrato c ON c.id_contrato = h.id_contrato
                 WHERE h.id_contrato IN (:contratoIds)
                   AND h.data_inicio < :fimExclusivo
                   AND (h.data_fim IS NULL OR h.data_fim >= :inicio)
                """);

        if (lotacaoId != null) sql.append(" AND h.id_lotacao = :lotacaoId ");
        if (jornadaId != null) sql.append(" AND h.id_jornada = :jornadaId ");

        if (gestaoLocalOperacionalId != null) {
            sql.append(" AND c.id_gestao_local = :gestaoLocalOperacionalId ");
            List<EscopoOperacional> escopos = escoposOperacionais == null
                    ? List.of() : escoposOperacionais;
            if (escopos.isEmpty()) {
                sql.append(" AND 1 = 0 ");
            } else {
                List<String> condicoes = new ArrayList<>();
                int indice = 0;
                for (EscopoOperacional escopo : escopos) {
                    if (escopo == null) continue;
                    List<String> partes = new ArrayList<>();
                    if (escopo.lotacaoId() != null) {
                        String nome = "escopoLotacao" + indice;
                        parametros.addValue(nome, escopo.lotacaoId());
                        partes.add("h.id_lotacao = :" + nome);
                    }
                    if (escopo.jornadaId() != null) {
                        String nome = "escopoJornada" + indice;
                        parametros.addValue(nome, escopo.jornadaId());
                        partes.add("h.id_jornada = :" + nome);
                    }
                    if (partes.isEmpty()) partes.add("h.id_lotacao IS NOT NULL AND h.id_jornada IS NOT NULL");
                    else {
                        // A regra operacional exige que ambos existam, mesmo quando um
                        // deles é curinga no vínculo de acesso.
                        partes.add("h.id_lotacao IS NOT NULL");
                        partes.add("h.id_jornada IS NOT NULL");
                    }
                    condicoes.add("(" + String.join(" AND ", partes) + ")");
                    indice++;
                }
                if (condicoes.isEmpty()) sql.append(" AND 1 = 0 ");
                else sql.append(" AND (").append(String.join(" OR ", condicoes)).append(") ");
            }
        }
        return new Consulta(sql.toString(), parametros);
    }

    private record Consulta(String fromWhere, MapSqlParameterSource parametros) {}
}
