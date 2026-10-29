package com.example.siggtrat.repositorio;

import com.example.siggtrat.dto.PainelEfetivoDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class PainelEfetivoRepositorio {

    private static final String STATUS_PRESENTES = "'PRESENCA','PRESENCA_PARCIAL','ATRASO','TR'";
    private static final String STATUS_AUSENTES = "'FALTA','ATESTADO','SUSPENSAO'";

    private final NamedParameterJdbcTemplate jdbc;

    public PainelEfetivoRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Série diária que parte do efetivo previsto nos contratos vigentes, mesmo
     * quando nenhuma frequência foi lançada. A alocação vigente vem do histórico
     * contratual, fonte canônica para consultas por data.
     */
    public List<PainelEfetivoDTO.PontoDiario> obterSerie(
            LocalDate inicio,
            LocalDate fim,
            Long contratoId,
            Long lotacaoId,
            Long jornadaId,
            Long cargoId,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        MapSqlParameterSource parametros = parametrosEscopo(gestaoLocalId, superintendenciaId)
                .addValue("inicio", Date.valueOf(inicio))
                .addValue("fim", Date.valueOf(fim))
                .addValue("contratoId", contratoId)
                .addValue("lotacaoId", lotacaoId)
                .addValue("jornadaId", jornadaId)
                .addValue("cargoId", cargoId);

        String escopoContrato = filtroEscopoContrato("c", "gl", gestaoLocalId, superintendenciaId);
        String escopoHistorico = filtroEscopoHistorico("h", gestaoLocalId, superintendenciaId);
        String escopoFrequencia = filtroEscopoFrequencia("f", gestaoLocalId, superintendenciaId);

        String sql = """
                WITH RECURSIVE datas AS (
                    SELECT CAST(:inicio AS DATE) AS data
                    UNION ALL
                    SELECT DATE_ADD(data, INTERVAL 1 DAY)
                      FROM datas
                     WHERE data < CAST(:fim AS DATE)
                ),
                contratos_previstos AS (
                    SELECT d.data,
                           c.id_contrato,
                           COALESCE(c.efetivo_previsto, 0) AS efetivo_previsto
                      FROM datas d
                      JOIN contrato c
                        ON COALESCE(c.ativo, TRUE) = TRUE
                       AND (c.data_inicio IS NULL OR c.data_inicio <= d.data)
                       AND (c.data_fim IS NULL OR c.data_fim >= d.data)
                      JOIN gestao_local gl ON gl.id_gestao_local = c.id_gestao_local
                     WHERE (:contratoId IS NULL OR c.id_contrato = :contratoId)
                       AND (
                           (:lotacaoId IS NULL AND :jornadaId IS NULL AND :cargoId IS NULL)
                           OR EXISTS (
                               SELECT 1
                                 FROM historico_vinculo_contrato hd
                                WHERE hd.id_contrato = c.id_contrato
                                  AND hd.data_inicio < DATE_ADD(d.data, INTERVAL 1 DAY)
                                  AND (hd.data_fim IS NULL OR hd.data_fim >= d.data)
                                  AND (:lotacaoId IS NULL OR hd.id_lotacao = :lotacaoId)
                                  AND (:jornadaId IS NULL OR hd.id_jornada = :jornadaId)
                                  AND (:cargoId IS NULL OR hd.id_cargo = :cargoId)
                           )
                       )
                """ + escopoContrato + """
                ),
                previsto AS (
                    SELECT data, COALESCE(SUM(efetivo_previsto), 0) AS efetivo_previsto
                      FROM contratos_previstos
                     GROUP BY data
                ),
                alocados AS (
                    SELECT d.data, COUNT(DISTINCT h.id_empregado) AS efetivo_alocado
                      FROM datas d
                      JOIN historico_vinculo_contrato h
                        ON h.data_inicio < DATE_ADD(d.data, INTERVAL 1 DAY)
                       AND (h.data_fim IS NULL OR h.data_fim >= d.data)
                      JOIN contrato c ON c.id_contrato = h.id_contrato
                     WHERE COALESCE(c.ativo, TRUE) = TRUE
                       AND (c.data_inicio IS NULL OR c.data_inicio <= d.data)
                       AND (c.data_fim IS NULL OR c.data_fim >= d.data)
                       AND (:contratoId IS NULL OR h.id_contrato = :contratoId)
                       AND (:lotacaoId IS NULL OR h.id_lotacao = :lotacaoId)
                       AND (:jornadaId IS NULL OR h.id_jornada = :jornadaId)
                       AND (:cargoId IS NULL OR h.id_cargo = :cargoId)
                """ + escopoHistorico + """
                     GROUP BY d.data
                ),
                lancamentos AS (
                    SELECT f.data_referencia AS data,
                           COUNT(*) AS registros_lancados,
                           COALESCE(SUM(CASE WHEN f.status IN (%s) THEN 1 ELSE 0 END), 0) AS presentes,
                           COALESCE(SUM(CASE WHEN f.status IN (%s) THEN 1 ELSE 0 END), 0) AS ausentes
                      FROM frequencia f
                     WHERE f.validade_operacional = 'VALIDA'
                       AND f.data_referencia BETWEEN :inicio AND :fim
                       AND (:contratoId IS NULL OR f.contrato_snapshot_id = :contratoId)
                       AND (:lotacaoId IS NULL OR f.lotacao_snapshot_id = :lotacaoId)
                       AND (:jornadaId IS NULL OR f.jornada_snapshot_id = :jornadaId)
                       AND (:cargoId IS NULL OR f.cargo_snapshot_id = :cargoId)
                """.formatted(STATUS_PRESENTES, STATUS_AUSENTES)
                + escopoFrequencia + """
                     GROUP BY f.data_referencia
                )
                SELECT d.data,
                       COALESCE(p.efetivo_previsto, 0) AS efetivo_previsto,
                       COALESCE(a.efetivo_alocado, 0) AS efetivo_alocado,
                       COALESCE(l.registros_lancados, 0) AS registros_lancados,
                       COALESCE(l.presentes, 0) AS presentes,
                       COALESCE(l.ausentes, 0) AS ausentes
                  FROM datas d
                  LEFT JOIN previsto p ON p.data = d.data
                  LEFT JOIN alocados a ON a.data = d.data
                  LEFT JOIN lancamentos l ON l.data = d.data
                 ORDER BY d.data
                """;

        return jdbc.query(sql, parametros, (rs, linha) -> montarPontoDiario(
                rs.getDate("data").toLocalDate(),
                rs.getLong("efetivo_previsto"),
                rs.getLong("efetivo_alocado"),
                rs.getLong("registros_lancados"),
                rs.getLong("presentes"),
                rs.getLong("ausentes")
        ));
    }

    /**
     * Contratos sempre usam o dimensionamento cadastrado no próprio contrato.
     * A quantidade alocada é apurada separadamente para não confundir vaga em
     * aberto com frequência ainda não lançada.
     */
    public List<PainelEfetivoDTO.DetalheContrato> obterDetalhamentoContratos(
            LocalDate data,
            Long contratoId,
            Long lotacaoId,
            Long jornadaId,
            Long cargoId,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        MapSqlParameterSource parametros = parametrosEscopo(gestaoLocalId, superintendenciaId)
                .addValue("data", Date.valueOf(data))
                .addValue("contratoId", contratoId)
                .addValue("lotacaoId", lotacaoId)
                .addValue("jornadaId", jornadaId)
                .addValue("cargoId", cargoId);

        String sql = """
                SELECT c.id_contrato AS id,
                       c.numero_contrato AS nome,
                       COALESCE(c.efetivo_previsto, 0) AS efetivo_previsto,
                       COALESCE(a.efetivo_alocado, 0) AS efetivo_alocado,
                       COALESCE(f.registros_lancados, 0) AS registros_lancados,
                       COALESCE(f.presentes, 0) AS presentes,
                       COALESCE(f.ausentes, 0) AS ausentes
                  FROM contrato c
                  JOIN gestao_local gl ON gl.id_gestao_local = c.id_gestao_local
                  LEFT JOIN (
                      SELECT h.id_contrato,
                             COUNT(DISTINCT h.id_empregado) AS efetivo_alocado
                        FROM historico_vinculo_contrato h
                       WHERE h.data_inicio < DATE_ADD(:data, INTERVAL 1 DAY)
                         AND (h.data_fim IS NULL OR h.data_fim >= :data)
                         AND (:lotacaoId IS NULL OR h.id_lotacao = :lotacaoId)
                         AND (:jornadaId IS NULL OR h.id_jornada = :jornadaId)
                         AND (:cargoId IS NULL OR h.id_cargo = :cargoId)
                """ + filtroEscopoHistorico("h", gestaoLocalId, superintendenciaId) + """
                       GROUP BY h.id_contrato
                  ) a ON a.id_contrato = c.id_contrato
                  LEFT JOIN (
                      SELECT f.contrato_snapshot_id AS id_contrato,
                             COUNT(*) AS registros_lancados,
                             COALESCE(SUM(CASE WHEN f.status IN (%s) THEN 1 ELSE 0 END), 0) AS presentes,
                             COALESCE(SUM(CASE WHEN f.status IN (%s) THEN 1 ELSE 0 END), 0) AS ausentes
                        FROM frequencia f
                       WHERE f.validade_operacional = 'VALIDA'
                         AND f.data_referencia = :data
                         AND (:lotacaoId IS NULL OR f.lotacao_snapshot_id = :lotacaoId)
                         AND (:jornadaId IS NULL OR f.jornada_snapshot_id = :jornadaId)
                         AND (:cargoId IS NULL OR f.cargo_snapshot_id = :cargoId)
                """.formatted(STATUS_PRESENTES, STATUS_AUSENTES)
                + filtroEscopoFrequencia("f", gestaoLocalId, superintendenciaId) + """
                       GROUP BY f.contrato_snapshot_id
                  ) f ON f.id_contrato = c.id_contrato
                 WHERE COALESCE(c.ativo, TRUE) = TRUE
                   AND (c.data_inicio IS NULL OR c.data_inicio <= :data)
                   AND (c.data_fim IS NULL OR c.data_fim >= :data)
                   AND (:contratoId IS NULL OR c.id_contrato = :contratoId)
                """ + filtroEscopoContrato("c", "gl", gestaoLocalId, superintendenciaId) + """
                 ORDER BY c.numero_contrato
                """;

        return jdbc.query(sql, parametros, (rs, linha) -> montarDetalheContrato(
                rs.getLong("id"), rs.getString("nome"),
                rs.getLong("efetivo_previsto"), rs.getLong("efetivo_alocado"),
                rs.getLong("registros_lancados"), rs.getLong("presentes"), rs.getLong("ausentes")
        ));
    }

    /**
     * Quadro por combinação operacional. Como contrato não possui distribuição
     * prevista por lotação/turno/cargo, a base correta dessas linhas é a alocação
     * vigente no histórico, nunca a quantidade de frequências lançadas.
     */
    public List<PainelEfetivoDTO.LinhaDimensionada> obterQuadroDimensionado(
            LocalDate data,
            Long contratoId,
            Long lotacaoId,
            Long jornadaId,
            Long cargoId,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        MapSqlParameterSource parametros = parametrosEscopo(gestaoLocalId, superintendenciaId)
                .addValue("data", Date.valueOf(data))
                .addValue("contratoId", contratoId)
                .addValue("lotacaoId", lotacaoId)
                .addValue("jornadaId", jornadaId)
                .addValue("cargoId", cargoId);

        String sql = """
                SELECT h.id_contrato AS contrato_id,
                       COALESCE(NULLIF(h.contrato_snapshot_numero, ''), c.numero_contrato, 'Sem contrato') AS contrato,
                       h.id_lotacao AS lotacao_id,
                       COALESCE(NULLIF(h.lotacao_snapshot_nome, ''), l.nome, 'Sem lotação') AS lotacao,
                       h.id_jornada AS jornada_id,
                       COALESCE(NULLIF(h.jornada_snapshot_descricao, ''), j.descricao, 'Sem turno') AS turno,
                       h.id_cargo AS cargo_id,
                       COALESCE(NULLIF(h.cargo_snapshot_nome, ''), cg.nome, 'Sem cargo') AS cargo,
                       COUNT(DISTINCT h.id_empregado) AS efetivo_alocado,
                       COUNT(DISTINCT f.id) AS registros_lancados,
                       COUNT(DISTINCT CASE WHEN f.status IN (%s) THEN f.id END) AS presentes,
                       COUNT(DISTINCT CASE WHEN f.status IN (%s) THEN f.id END) AS ausentes
                  FROM historico_vinculo_contrato h
                  JOIN contrato c ON c.id_contrato = h.id_contrato
                  LEFT JOIN lotacao l ON l.id_lotacao = h.id_lotacao
                  LEFT JOIN jornada j ON j.id_jornada = h.id_jornada
                  LEFT JOIN cargo cg ON cg.id_cargo = h.id_cargo
                  LEFT JOIN frequencia f
                    ON f.id_empregado = h.id_empregado
                   AND f.data_referencia = :data
                   AND f.validade_operacional = 'VALIDA'
                   AND f.contrato_snapshot_id = h.id_contrato
                   AND COALESCE(f.lotacao_snapshot_id, -1) = COALESCE(h.id_lotacao, -1)
                   AND COALESCE(f.jornada_snapshot_id, -1) = COALESCE(h.id_jornada, -1)
                   AND COALESCE(f.cargo_snapshot_id, -1) = COALESCE(h.id_cargo, -1)
                 WHERE h.data_inicio < DATE_ADD(:data, INTERVAL 1 DAY)
                   AND (h.data_fim IS NULL OR h.data_fim >= :data)
                   AND COALESCE(c.ativo, TRUE) = TRUE
                   AND (c.data_inicio IS NULL OR c.data_inicio <= :data)
                   AND (c.data_fim IS NULL OR c.data_fim >= :data)
                   AND (:contratoId IS NULL OR h.id_contrato = :contratoId)
                   AND (:lotacaoId IS NULL OR h.id_lotacao = :lotacaoId)
                   AND (:jornadaId IS NULL OR h.id_jornada = :jornadaId)
                   AND (:cargoId IS NULL OR h.id_cargo = :cargoId)
                """.formatted(STATUS_PRESENTES, STATUS_AUSENTES)
                + filtroEscopoHistorico("h", gestaoLocalId, superintendenciaId) + """
                 GROUP BY h.id_contrato, h.contrato_snapshot_numero, c.numero_contrato,
                          h.id_lotacao, h.lotacao_snapshot_nome, l.nome,
                          h.id_jornada, h.jornada_snapshot_descricao, j.descricao,
                          h.id_cargo, h.cargo_snapshot_nome, cg.nome
                 ORDER BY contrato, lotacao, turno, cargo
                """;

        return jdbc.query(sql, parametros, (rs, linha) -> {
            long alocados = rs.getLong("efetivo_alocado");
            long registros = rs.getLong("registros_lancados");
            long presentes = rs.getLong("presentes");
            long ausentes = rs.getLong("ausentes");
            return new PainelEfetivoDTO.LinhaDimensionada(
                    nullableLong(rs, "contrato_id"), rs.getString("contrato"),
                    nullableLong(rs, "lotacao_id"), rs.getString("lotacao"),
                    nullableLong(rs, "jornada_id"), rs.getString("turno"),
                    nullableLong(rs, "cargo_id"), rs.getString("cargo"),
                    alocados,
                    registros,
                    presentes,
                    ausentes,
                    Math.max(0L, alocados - registros),
                    percentual(registros, alocados),
                    percentual(presentes, alocados)
            );
        });
    }

    public List<PainelEfetivoDTO.Opcao> listarContratos(
            Long gestaoLocalId,
            Long superintendenciaId,
            Long lotacaoId,
            Long jornadaId,
            Long cargoId
    ) {
        MapSqlParameterSource parametros = parametrosEscopo(gestaoLocalId, superintendenciaId)
                .addValue("lotacaoId", lotacaoId)
                .addValue("jornadaId", jornadaId)
                .addValue("cargoId", cargoId);
        String sql = """
                SELECT DISTINCT c.id_contrato AS id, c.numero_contrato AS nome
                  FROM empregado e
                  JOIN status_empregado s ON s.id_status = e.id_status
                  JOIN gestao_local gl ON gl.id_gestao_local = e.id_gestao_local
                  JOIN empregado_contrato ec ON ec.id_empregado = e.id_empregado
                  JOIN contrato c ON c.id_contrato = ec.id_contrato
                 WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO'
                   AND COALESCE(c.ativo, TRUE) = TRUE
                   AND (:lotacaoId IS NULL OR e.id_lotacao = :lotacaoId)
                   AND (:jornadaId IS NULL OR e.id_jornada = :jornadaId)
                   AND (:cargoId IS NULL OR e.id_cargo = :cargoId)
                """ + filtroEscopoEmpregado("e", "gl", gestaoLocalId, superintendenciaId)
                + " ORDER BY c.numero_contrato";
        return consultarOpcoes(sql, parametros);
    }

    public List<PainelEfetivoDTO.Opcao> listarLotacoes(
            Long gestaoLocalId,
            Long superintendenciaId,
            Long contratoId,
            Long jornadaId,
            Long cargoId
    ) {
        MapSqlParameterSource parametros = parametrosEscopo(gestaoLocalId, superintendenciaId)
                .addValue("contratoId", contratoId)
                .addValue("jornadaId", jornadaId)
                .addValue("cargoId", cargoId);
        String sql = """
                SELECT DISTINCT l.id_lotacao AS id, l.nome AS nome
                  FROM empregado e
                  JOIN status_empregado s ON s.id_status = e.id_status
                  JOIN gestao_local gl ON gl.id_gestao_local = e.id_gestao_local
                  JOIN lotacao l ON l.id_lotacao = e.id_lotacao
                  JOIN empregado_contrato ec ON ec.id_empregado = e.id_empregado
                 WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO'
                   AND (:contratoId IS NULL OR ec.id_contrato = :contratoId)
                   AND (:jornadaId IS NULL OR e.id_jornada = :jornadaId)
                   AND (:cargoId IS NULL OR e.id_cargo = :cargoId)
                """ + filtroEscopoEmpregado("e", "gl", gestaoLocalId, superintendenciaId)
                + " ORDER BY l.nome";
        return consultarOpcoes(sql, parametros);
    }

    public List<PainelEfetivoDTO.Opcao> listarTurnos(
            Long gestaoLocalId,
            Long superintendenciaId,
            Long contratoId,
            Long lotacaoId,
            Long cargoId
    ) {
        MapSqlParameterSource parametros = parametrosEscopo(gestaoLocalId, superintendenciaId)
                .addValue("contratoId", contratoId)
                .addValue("lotacaoId", lotacaoId)
                .addValue("cargoId", cargoId);
        String sql = """
                SELECT DISTINCT j.id_jornada AS id, j.descricao AS nome
                  FROM empregado e
                  JOIN status_empregado s ON s.id_status = e.id_status
                  JOIN gestao_local gl ON gl.id_gestao_local = e.id_gestao_local
                  JOIN jornada j ON j.id_jornada = e.id_jornada
                  JOIN empregado_contrato ec ON ec.id_empregado = e.id_empregado
                 WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO'
                   AND (:contratoId IS NULL OR ec.id_contrato = :contratoId)
                   AND (:lotacaoId IS NULL OR e.id_lotacao = :lotacaoId)
                   AND (:cargoId IS NULL OR e.id_cargo = :cargoId)
                """ + filtroEscopoEmpregado("e", "gl", gestaoLocalId, superintendenciaId)
                + " ORDER BY j.descricao";
        return consultarOpcoes(sql, parametros);
    }

    public List<PainelEfetivoDTO.Opcao> listarCargos(
            Long gestaoLocalId,
            Long superintendenciaId,
            Long contratoId,
            Long lotacaoId,
            Long jornadaId
    ) {
        MapSqlParameterSource parametros = parametrosEscopo(gestaoLocalId, superintendenciaId)
                .addValue("contratoId", contratoId)
                .addValue("lotacaoId", lotacaoId)
                .addValue("jornadaId", jornadaId);
        String sql = """
                SELECT DISTINCT c.id_cargo AS id, c.nome AS nome
                  FROM empregado e
                  JOIN status_empregado s ON s.id_status = e.id_status
                  JOIN gestao_local gl ON gl.id_gestao_local = e.id_gestao_local
                  JOIN cargo c ON c.id_cargo = e.id_cargo
                  JOIN empregado_contrato ec ON ec.id_empregado = e.id_empregado
                 WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO'
                   AND (:contratoId IS NULL OR ec.id_contrato = :contratoId)
                   AND (:lotacaoId IS NULL OR e.id_lotacao = :lotacaoId)
                   AND (:jornadaId IS NULL OR e.id_jornada = :jornadaId)
                """ + filtroEscopoEmpregado("e", "gl", gestaoLocalId, superintendenciaId)
                + " ORDER BY c.nome";
        return consultarOpcoes(sql, parametros);
    }

    private PainelEfetivoDTO.PontoDiario montarPontoDiario(
            LocalDate data,
            long previsto,
            long alocado,
            long registros,
            long presentes,
            long ausentes
    ) {
        long outros = Math.max(0L, registros - presentes - ausentes);
        return new PainelEfetivoDTO.PontoDiario(
                data,
                previsto,
                alocado,
                registros,
                presentes,
                ausentes,
                outros,
                Math.max(0L, alocado - registros),
                Math.max(0L, previsto - alocado),
                Math.max(0L, alocado - previsto),
                percentual(registros, alocado),
                percentual(presentes, previsto)
        );
    }

    private PainelEfetivoDTO.DetalheContrato montarDetalheContrato(
            Long id,
            String nome,
            long previsto,
            long alocado,
            long registros,
            long presentes,
            long ausentes
    ) {
        return new PainelEfetivoDTO.DetalheContrato(
                id,
                nome,
                previsto,
                alocado,
                registros,
                presentes,
                ausentes,
                Math.max(0L, alocado - registros),
                Math.max(0L, previsto - alocado),
                Math.max(0L, alocado - previsto),
                percentual(registros, alocado),
                percentual(presentes, previsto)
        );
    }

    private List<PainelEfetivoDTO.Opcao> consultarOpcoes(String sql, MapSqlParameterSource parametros) {
        return jdbc.query(sql, parametros, (rs, linha) ->
                new PainelEfetivoDTO.Opcao(rs.getLong("id"), rs.getString("nome")));
    }

    private MapSqlParameterSource parametrosEscopo(Long gestaoLocalId, Long superintendenciaId) {
        return new MapSqlParameterSource()
                .addValue("gestaoLocalId", gestaoLocalId)
                .addValue("superintendenciaId", superintendenciaId);
    }

    private String filtroEscopoEmpregado(
            String aliasEmpregado,
            String aliasGestaoLocal,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        if (gestaoLocalId != null) {
            return " AND " + aliasEmpregado + ".id_gestao_local = :gestaoLocalId ";
        }
        if (superintendenciaId != null) {
            return " AND " + aliasGestaoLocal + ".id_superintendencia = :superintendenciaId ";
        }
        return " ";
    }

    private String filtroEscopoContrato(
            String aliasContrato,
            String aliasGestaoLocal,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        if (gestaoLocalId != null) {
            return " AND " + aliasContrato + ".id_gestao_local = :gestaoLocalId ";
        }
        if (superintendenciaId != null) {
            return " AND " + aliasGestaoLocal + ".id_superintendencia = :superintendenciaId ";
        }
        return " ";
    }

    private String filtroEscopoHistorico(String alias, Long gestaoLocalId, Long superintendenciaId) {
        if (gestaoLocalId != null) {
            return " AND " + alias + ".gestao_local_snapshot_id = :gestaoLocalId ";
        }
        if (superintendenciaId != null) {
            return " AND " + alias + ".superintendencia_snapshot_id = :superintendenciaId ";
        }
        return " ";
    }

    private String filtroEscopoFrequencia(String alias, Long gestaoLocalId, Long superintendenciaId) {
        if (gestaoLocalId != null) {
            return " AND " + alias + ".gestao_local_snapshot_id = :gestaoLocalId ";
        }
        if (superintendenciaId != null) {
            return " AND " + alias + ".superintendencia_snapshot_id = :superintendenciaId ";
        }
        return " ";
    }

    private Long nullableLong(java.sql.ResultSet rs, String coluna) throws java.sql.SQLException {
        long valor = rs.getLong(coluna);
        return rs.wasNull() ? null : valor;
    }

    private double percentual(long numerador, long denominador) {
        if (denominador <= 0L) return 0D;
        return Math.round((numerador * 10000D) / denominador) / 100D;
    }
}
