package com.example.siggtrat.repositorio;

import com.example.siggtrat.dto.PainelGestaoDTO;
import com.example.siggtrat.dto.PainelEfetivoDTO;
import com.example.siggtrat.dto.PainelRankingDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class PainelRepositorio {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final PainelEfetivoRepositorio efetivoTemporal;
    private final PainelAnaliticoRepositorio.Filtro recorte;
    private final List<com.example.siggtrat.dto.PainelAnaliticoDTO.Unidade> unidadesPagina;

    /** Instância imutável por consulta; o bean compartilhado nunca guarda filtros de usuário. */
    public PainelRepositorio noRecorte(PainelAnaliticoRepositorio.Filtro filtro,
            List<com.example.siggtrat.dto.PainelAnaliticoDTO.Unidade> unidades) {
        return new PainelRepositorio(jdbcTemplate, namedJdbcTemplate, efetivoTemporal, filtro, List.copyOf(unidades));
    }


    @org.springframework.beans.factory.annotation.Autowired
    public PainelRepositorio(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate,
                             PainelEfetivoRepositorio efetivoTemporal) {
        this(jdbcTemplate, namedJdbcTemplate, efetivoTemporal, null, List.of());
    }

    private PainelRepositorio(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate,
            PainelEfetivoRepositorio efetivoTemporal, PainelAnaliticoRepositorio.Filtro recorte,
            List<com.example.siggtrat.dto.PainelAnaliticoDTO.Unidade> unidadesPagina) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.efetivoTemporal = efetivoTemporal;
        this.recorte = recorte;
        this.unidadesPagina = unidadesPagina;
    }

    public PainelGestaoDTO.ResumoEfetivo obterResumoEfetivo(LocalDate referencia, Long gestaoLocalId, Long superintendenciaId) {
        // Previsão e ocupação têm exatamente a mesma fonte canônica do /efetivo.
        // Os dois campos explicitamente cadastrais continuam descrevendo o cadastro legado.
        List<PainelEfetivoDTO.DetalheContrato> detalhes = efetivoTemporal.obterDetalhamentoContratos(
                referencia, recorte == null ? null : recorte.contrato(), lotacaoRecorte(), null, null,
                gestaoLocalId, superintendenciaId, recorte == null ? null : recorte.prestador());
        long previsto = detalhes.stream().mapToLong(PainelEfetivoDTO.DetalheContrato::efetivoPrevisto).sum();
        long vinculado = detalhes.stream().mapToLong(PainelEfetivoDTO.DetalheContrato::efetivoAlocado).sum();
        String empregadoScope = condicaoEscopo("e.id_gestao_local", gestaoLocalId, superintendenciaId);
        String sql = """
                SELECT
                  (SELECT COUNT(DISTINCT e.id_empregado)
                     FROM empregado e
                     JOIN status_empregado s ON s.id_status = e.id_status
                    WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO'
                      AND (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                      %s) AS ativos_cadastrados,
                  (SELECT COUNT(DISTINCT e.id_empregado)
                     FROM empregado e
                     JOIN status_empregado s ON s.id_status = e.id_status
                    WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO'
                      AND (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                      AND NOT EXISTS (
                          SELECT 1
                            FROM empregado_contrato ec
                            JOIN contrato c ON c.id_contrato = ec.id_contrato
                           WHERE ec.id_empregado = e.id_empregado
                             AND c.id_gestao_local = e.id_gestao_local
                             AND COALESCE(c.ativo, TRUE) = TRUE
                             AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                             AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                      )
                      %s) AS ativos_sem_contrato
                """.formatted(empregadoScope, empregadoScope);
        Map<String, Object> row = namedJdbcTemplate.queryForMap(sql, parametrosBase(referencia, gestaoLocalId, superintendenciaId));
        return new PainelGestaoDTO.ResumoEfetivo(previsto, vinculado,
                valorLong(row, "ativos_cadastrados"), previsto - vinculado,
                previsto > 0 ? arredondarUmaCasa(vinculado * 100.0 / previsto) : 0.0,
                valorLong(row, "ativos_sem_contrato"), detalhes.size());
    }

    public PainelGestaoDTO.SaudeContratual obterSaudeContratual(LocalDate referencia, Long gestaoLocalId, Long superintendenciaId) {
        String scope = condicaoEscopo("c.id_gestao_local", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(referencia, gestaoLocalId, superintendenciaId)
                .addValue("limite30", referencia.plusDays(30));

        String sql = """
                SELECT
                    COUNT(*) AS total,
                    COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                        AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                        AND (c.data_fim IS NULL OR c.data_fim >= :referencia) THEN 1 ELSE 0 END), 0) AS vigentes,
                    COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                        AND c.data_fim IS NOT NULL AND c.data_fim < :referencia THEN 1 ELSE 0 END), 0) AS vencidos_ativos,
                    COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                        AND c.data_fim BETWEEN :referencia AND :limite30 THEN 1 ELSE 0 END), 0) AS vencendo_30,
                    COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                        AND c.data_fim IS NULL THEN 1 ELSE 0 END), 0) AS sem_data_fim,
                    COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                        AND COALESCE(c.efetivo_previsto, 0) <= 0 THEN 1 ELSE 0 END), 0) AS sem_efetivo,
                    COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                        AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                        AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                        AND COALESCE(v.atual, 0) > COALESCE(c.efetivo_previsto, 0)
                        AND COALESCE(c.efetivo_previsto, 0) > 0 THEN 1 ELSE 0 END), 0) AS acima_previsto,
                    COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                        AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                        AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                        AND COALESCE(v.atual, 0) < COALESCE(c.efetivo_previsto, 0)
                        AND COALESCE(c.efetivo_previsto, 0) > 0 THEN 1 ELSE 0 END), 0) AS abaixo_previsto
                FROM contrato c
                LEFT JOIN (
                    SELECT ec.id_contrato, COUNT(DISTINCT e.id_empregado) AS atual
                      FROM empregado_contrato ec
                      JOIN empregado e ON e.id_empregado = ec.id_empregado
                      JOIN contrato c_vinculo ON c_vinculo.id_contrato = ec.id_contrato
                      JOIN status_empregado s ON s.id_status = e.id_status
                     WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO'
                       AND (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                       AND e.id_gestao_local = c_vinculo.id_gestao_local
                       %s
                     GROUP BY ec.id_contrato
                ) v ON v.id_contrato = c.id_contrato
                WHERE 1 = 1
                """.formatted(condicaoEscopo("c_vinculo.id_gestao_local", gestaoLocalId, superintendenciaId)) + scope;

        Map<String, Object> row = namedJdbcTemplate.queryForMap(sql, params);
        return new PainelGestaoDTO.SaudeContratual(
                valorLong(row, "total"),
                valorLong(row, "vigentes"),
                valorLong(row, "vencidos_ativos"),
                valorLong(row, "vencendo_30"),
                valorLong(row, "sem_data_fim"),
                valorLong(row, "sem_efetivo"),
                valorLong(row, "acima_previsto"),
                valorLong(row, "abaixo_previsto")
        );
    }

    public PainelGestaoDTO.QuadroPessoal obterQuadroPessoal(LocalDate referencia, Long gestaoLocalId, Long superintendenciaId) {
        String empregadoScope = condicaoEscopo("e.id_gestao_local", gestaoLocalId, superintendenciaId);
        String lotacaoScope = condicaoEscopo("l.id_gestao_local", gestaoLocalId, superintendenciaId);
        String jornadaScope = condicaoEscopo("j.id_gestao_local", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(referencia, gestaoLocalId, superintendenciaId);
        String limiteAdmissao = " AND (e.data_admissao IS NULL OR e.data_admissao <= :referencia) ";
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM empregado e WHERE 1 = 1 %s %s) AS total,
                  (SELECT COUNT(*) FROM empregado e
                    JOIN status_empregado s ON s.id_status = e.id_status
                   WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO' %s %s) AS ativos,
                  (SELECT COUNT(*) FROM lotacao l WHERE 1 = 1 %s) AS lotacoes,
                  (SELECT COUNT(DISTINCT e.id_cargo) FROM empregado e WHERE e.id_cargo IS NOT NULL %s %s) AS cargos,
                  (SELECT COUNT(*) FROM jornada j WHERE 1 = 1 %s) AS jornadas
                """.formatted(
                empregadoScope, limiteAdmissao,
                empregadoScope, limiteAdmissao,
                lotacaoScope,
                empregadoScope, limiteAdmissao,
                jornadaScope
        );
        Map<String, Object> row = namedJdbcTemplate.queryForMap(sql, params);
        long total = valorLong(row, "total");
        long ativos = valorLong(row, "ativos");
        return new PainelGestaoDTO.QuadroPessoal(
                total,
                ativos,
                Math.max(0, total - ativos),
                valorLong(row, "lotacoes"),
                valorLong(row, "cargos"),
                valorLong(row, "jornadas"),
                total > 0 ? arredondarUmaCasa(ativos * 100.0 / total) : 0.0
        );
    }

    public PainelGestaoDTO.AnaliseCadastro obterAnaliseCadastro(
            LocalDate inicioPeriodo,
            LocalDate referencia,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scope = condicaoEscopo("e.id_gestao_local", gestaoLocalId, superintendenciaId);
        LocalDate inicioMes = referencia.withDayOfMonth(1);
        LocalDate inicio12Meses = inicioMes.minusMonths(11);
        MapSqlParameterSource params = parametrosBase(referencia, gestaoLocalId, superintendenciaId)
                .addValue("inicioPeriodo", inicioPeriodo)
                .addValue("inicioMes", inicioMes)
                .addValue("inicio12Meses", inicio12Meses);
        String sql = """
                SELECT
                  COALESCE(SUM(CASE WHEN e.data_admissao BETWEEN :inicioPeriodo AND :referencia THEN 1 ELSE 0 END), 0) AS admitidos_periodo,
                  COALESCE(SUM(CASE WHEN e.data_admissao BETWEEN :inicioMes AND :referencia THEN 1 ELSE 0 END), 0) AS admitidos_mes,
                  COALESCE(SUM(CASE WHEN e.data_admissao BETWEEN :inicio12Meses AND :referencia THEN 1 ELSE 0 END), 0) AS admitidos_12_meses,
                  COALESCE(SUM(CASE WHEN e.data_admissao IS NOT NULL AND e.data_admissao <= :referencia THEN 1 ELSE 0 END), 0) AS com_data,
                  COALESCE(SUM(CASE WHEN e.data_admissao IS NULL THEN 1 ELSE 0 END), 0) AS sem_data
                  FROM empregado e
                 WHERE (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                """ + scope;
        Map<String, Object> row = namedJdbcTemplate.queryForMap(sql, params);
        long comData = valorLong(row, "com_data");
        long semData = valorLong(row, "sem_data");
        long total = comData + semData;
        return new PainelGestaoDTO.AnaliseCadastro(
                valorLong(row, "admitidos_periodo"),
                valorLong(row, "admitidos_mes"),
                valorLong(row, "admitidos_12_meses"),
                comData,
                semData,
                total > 0 ? arredondarUmaCasa(comData * 100.0 / total) : 0.0
        );
    }

    public List<PainelGestaoDTO.AdmissaoMensal> obterAdmissoesMensais(
            LocalDate referencia,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scope = condicaoEscopo("e.id_gestao_local", gestaoLocalId, superintendenciaId);
        LocalDate inicio = referencia.withDayOfMonth(1).minusMonths(11);
        MapSqlParameterSource params = parametrosBase(referencia, gestaoLocalId, superintendenciaId)
                .addValue("inicioAdmissoes", inicio);
        String sql = """
                SELECT EXTRACT(YEAR FROM e.data_admissao) AS ano,
                       EXTRACT(MONTH FROM e.data_admissao) AS mes,
                       COUNT(*) AS quantidade
                  FROM empregado e
                 WHERE e.data_admissao BETWEEN :inicioAdmissoes AND :referencia
                """ + scope + """
                 GROUP BY EXTRACT(YEAR FROM e.data_admissao), EXTRACT(MONTH FROM e.data_admissao)
                 ORDER BY EXTRACT(YEAR FROM e.data_admissao), EXTRACT(MONTH FROM e.data_admissao)
                """;
        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> new PainelGestaoDTO.AdmissaoMensal(
                LocalDate.of(rs.getInt("ano"), rs.getInt("mes"), 1),
                rs.getLong("quantidade")
        ));
    }

    public PainelGestaoDTO.CoberturaFrequencia obterCoberturaFrequencia(
            LocalDate referencia,
            long empregadosAtivos,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scope = condicaoEscopo("e.id_gestao_local", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(referencia, gestaoLocalId, superintendenciaId)
                .addValue("diaSemana", referencia.getDayOfWeek().name());
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN base.esperado = 1 THEN 1 ELSE 0 END), 0) AS esperados,
                    COALESCE(SUM(CASE WHEN base.esperado = 1 AND base.id_frequencia IS NOT NULL THEN 1 ELSE 0 END), 0) AS registros_lancados,
                    COALESCE(SUM(CASE WHEN base.esperado = 1 AND base.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR') THEN 1 ELSE 0 END), 0) AS presencas,
                    COALESCE(SUM(CASE WHEN base.esperado = 1 AND base.status IN ('FALTA','ATESTADO','SUSPENSAO') THEN 1 ELSE 0 END), 0) AS ausencias,
                    COALESCE(SUM(CASE WHEN base.esperado = 1 AND base.status = 'ATESTADO' THEN 1 ELSE 0 END), 0) AS justificadas,
                    COALESCE(SUM(CASE WHEN base.esperado = 1 AND base.status = 'FALTA' THEN 1 ELSE 0 END), 0) AS faltas,
                    COALESCE(SUM(CASE WHEN base.id_frequencia IS NOT NULL AND (base.status = 'RT' OR base.tem_rt_adicional = 1) THEN 1 ELSE 0 END), 0) AS rts,
                    COALESCE(SUM(CASE WHEN base.esperado = 0
                                             AND base.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO')
                                             AND base.tem_rt_adicional = 0 THEN 1 ELSE 0 END), 0) AS fora_escala
                FROM (
                    SELECT e.id_empregado,
                           f.id AS id_frequencia,
                           COALESCE(f.status, '') AS status,
                           CASE WHEN EXISTS (
                               SELECT 1
                                 FROM frequencia_adicional fa_rt
                                WHERE fa_rt.id_empregado = e.id_empregado
                                  AND fa_rt.data_referencia = :referencia
                                  AND fa_rt.tipo = 'RT'
                                  AND COALESCE(fa_rt.status, 'CALCULADO') <> 'CANCELADO'
                           ) THEN 1 ELSE 0 END AS tem_rt_adicional,
                           CASE WHEN COALESCE(
                                      jds.dia_trabalho,
                                      CASE
                                          WHEN e.id_jornada IS NULL THEN FALSE
                                          WHEN EXISTS (
                                              SELECT 1 FROM jornada_dia_semana jds_any
                                               WHERE jds_any.id_jornada = e.id_jornada
                                          ) THEN FALSE
                                          WHEN :diaSemana IN ('SATURDAY', 'SUNDAY') THEN FALSE
                                          ELSE TRUE
                                      END
                                  ) = TRUE
                              AND NOT EXISTS (
                                  SELECT 1 FROM frequencia_excecao fx
                                   WHERE fx.id_empregado = e.id_empregado
                                     AND :referencia BETWEEN fx.data_inicio AND fx.data_fim
                                     AND fx.status = 'APROVADO'
                                     AND fx.tipo = 'FOLGA_REMUNERADA'
                              )
                              AND NOT EXISTS (
                                  SELECT 1 FROM calendario_evento ce
                                   WHERE ce.data_evento = :referencia
                                     AND COALESCE(ce.ativo, TRUE) = TRUE
                                     AND ce.tipo IN ('FERIADO','DIA_DESCANSO')
                                     AND (
                                         ce.abrangencia = 'NACIONAL'
                                         OR (ce.abrangencia = 'ESTADUAL'
                                             AND (ce.id_superintendencia = se.id_superintendencia
                                                  OR (ce.id_superintendencia IS NULL AND ce.uf = se.uf)))
                                         OR (ce.abrangencia = 'MUNICIPAL'
                                             AND ce.municipio = l.municipio
                                             AND (ce.id_gestao_local IS NULL OR ce.id_gestao_local = e.id_gestao_local))
                                         OR (ce.abrangencia = 'LOTACAO' AND ce.id_lotacao = e.id_lotacao)
                                     )
                              )
                           THEN 1 ELSE 0 END AS esperado
                      FROM empregado e
                      JOIN status_empregado s ON s.id_status = e.id_status
                      JOIN lotacao l ON l.id_lotacao = e.id_lotacao
                      JOIN gestao_local cp ON cp.id_gestao_local = e.id_gestao_local
                      JOIN superintendencia_estadual se ON se.id_superintendencia = cp.id_superintendencia
                      LEFT JOIN jornada_dia_semana jds ON jds.id_jornada = e.id_jornada
                             AND jds.dia_semana = :diaSemana
                      LEFT JOIN frequencia f ON f.id_empregado = e.id_empregado
                             AND f.data_referencia = :referencia
                             AND f.validade_operacional = 'VALIDA'
                     WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO'
                       AND (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                """ + scope + """
                ) base
                """;
        Map<String, Object> row = namedJdbcTemplate.queryForMap(sql, params);
        long esperados = valorLong(row, "esperados");
        long registros = valorLong(row, "registros_lancados");
        long pendentes = Math.max(0, esperados - registros);
        double cobertura = esperados > 0 ? arredondarUmaCasa(registros * 100.0 / esperados) : 100.0;
        return new PainelGestaoDTO.CoberturaFrequencia(
                referencia,
                empregadosAtivos,
                esperados,
                registros,
                pendentes,
                cobertura,
                valorLong(row, "presencas"),
                valorLong(row, "ausencias"),
                valorLong(row, "justificadas"),
                valorLong(row, "faltas"),
                valorLong(row, "rts"),
                valorLong(row, "fora_escala")
        );
    }

    public PainelGestaoDTO.AnalisePeriodo obterAnalisePeriodo(
            LocalDate inicio,
            LocalDate fim,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scope = condicaoEscopoFrequencia("f", gestaoLocalId, superintendenciaId);
        String recurrentScope = condicaoEscopoFrequencia("f2", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(fim, gestaoLocalId, superintendenciaId)
                .addValue("inicio", inicio)
                .addValue("fim", fim);
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN f.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR','FALTA','ATESTADO','SUSPENSAO') THEN 1 ELSE 0 END), 0) AS operacionais,
                    COALESCE(SUM(CASE WHEN f.status IN ('PRESENCA','TR') THEN 1 ELSE 0 END), 0) AS presencas,
                    COALESCE(SUM(CASE WHEN f.status = 'PRESENCA_PARCIAL' THEN 1 ELSE 0 END), 0) AS parciais,
                    COALESCE(SUM(CASE WHEN f.status = 'ATRASO' THEN 1 ELSE 0 END), 0) AS atrasos,
                    COALESCE(SUM(CASE WHEN f.status = 'FALTA' THEN 1 ELSE 0 END), 0) AS faltas,
                    COALESCE(SUM(CASE WHEN f.status = 'ATESTADO' THEN 1 ELSE 0 END), 0) AS atestados,
                    COALESCE(SUM(CASE WHEN f.status = 'SUSPENSAO' THEN 1 ELSE 0 END), 0) AS suspensoes,
                    COALESCE(SUM(CASE WHEN f.status = 'DSR_PERDIDO' THEN 1 ELSE 0 END), 0) AS dsr_perdidos,
                    COALESCE(SUM(CASE WHEN f.status = 'RT'
                                              OR EXISTS (
                                                  SELECT 1 FROM frequencia_adicional fa_rt
                                                   WHERE fa_rt.id_empregado = f.id_empregado
                                                     AND fa_rt.data_referencia = f.data_referencia
                                                     AND fa_rt.tipo = 'RT'
                                                     AND COALESCE(fa_rt.status, 'CALCULADO') <> 'CANCELADO'
                                              ) THEN 1 ELSE 0 END), 0) AS rts,
                    COUNT(DISTINCT CASE WHEN f.status IN ('FALTA','ATESTADO','SUSPENSAO') THEN f.id_empregado END) AS empregados_ausencia,
                    MAX(f.updated_at) AS ultima_atualizacao,
                    (SELECT COUNT(*) FROM (
                        SELECT f2.id_empregado
                          FROM frequencia f2
                          JOIN empregado e2 ON e2.id_empregado = f2.id_empregado
                         WHERE f2.validade_operacional = 'VALIDA'
                           AND f2.data_referencia BETWEEN :inicio AND :fim
                           AND f2.status IN ('FALTA','ATESTADO','SUSPENSAO')
                           %s
                         GROUP BY f2.id_empregado
                        HAVING COUNT(*) >= 3
                    ) recorrentes) AS empregados_recorrentes
                  FROM frequencia f
                  JOIN empregado e ON e.id_empregado = f.id_empregado
                 WHERE f.validade_operacional = 'VALIDA'
                   AND f.data_referencia BETWEEN :inicio AND :fim
                   %s
                """.formatted(recurrentScope, scope);
        Map<String, Object> row = namedJdbcTemplate.queryForMap(sql, params);
        long operacionais = valorLong(row, "operacionais");
        long presencasCompletas = valorLong(row, "presencas");
        long parciais = valorLong(row, "parciais");
        long atrasos = valorLong(row, "atrasos");
        long faltas = valorLong(row, "faltas");
        long atestados = valorLong(row, "atestados");
        long suspensoes = valorLong(row, "suspensoes");
        long presencasGerenciais = presencasCompletas + parciais + atrasos;
        long ausencias = faltas + atestados + suspensoes;
        return new PainelGestaoDTO.AnalisePeriodo(
                inicio,
                fim,
                (int) (java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) + 1),
                operacionais,
                presencasCompletas,
                parciais,
                atrasos,
                faltas,
                atestados,
                suspensoes,
                valorLong(row, "dsr_perdidos"),
                valorLong(row, "rts"),
                valorLong(row, "empregados_ausencia"),
                valorLong(row, "empregados_recorrentes"),
                operacionais > 0 ? arredondarUmaCasa(presencasGerenciais * 100.0 / operacionais) : 0.0,
                operacionais > 0 ? arredondarUmaCasa(ausencias * 100.0 / operacionais) : 0.0,
                valorDataHora(row, "ultima_atualizacao")
        );
    }

    public PainelGestaoDTO.PressaoOperacional obterPressaoOperacional(
            LocalDate inicio,
            LocalDate fim,
            long empregadosAtivos,
            long ausenciasPeriodo,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scope = condicaoEscopoFrequencia("f", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(fim, gestaoLocalId, superintendenciaId)
                .addValue("inicio", inicio)
                .addValue("fim", fim);
        String sql = """
                SELECT COUNT(*) AS rts,
                       COUNT(DISTINCT f.id_empregado) AS empregados_rt
                  FROM frequencia f
                  JOIN empregado e ON e.id_empregado = f.id_empregado
                 WHERE f.validade_operacional = 'VALIDA'
                   AND f.data_referencia BETWEEN :inicio AND :fim
                   AND (
                       f.status = 'RT'
                       OR EXISTS (
                           SELECT 1 FROM frequencia_adicional fa_rt
                            WHERE fa_rt.id_empregado = f.id_empregado
                              AND fa_rt.data_referencia = f.data_referencia
                              AND fa_rt.tipo = 'RT'
                              AND COALESCE(fa_rt.status, 'CALCULADO') <> 'CANCELADO'
                       )
                   )
                """ + scope;
        Map<String, Object> row = namedJdbcTemplate.queryForMap(sql, params);
        long rts = valorLong(row, "rts");
        return new PainelGestaoDTO.PressaoOperacional(
                rts,
                valorLong(row, "empregados_rt"),
                ausenciasPeriodo,
                empregadosAtivos > 0 ? arredondarUmaCasa(rts * 100.0 / empregadosAtivos) : 0.0,
                ausenciasPeriodo > 0 ? arredondarUmaCasa(rts * 100.0 / ausenciasPeriodo) : 0.0
        );
    }

    public PainelGestaoDTO.HorizonteContratual obterHorizonteContratual(
            LocalDate referencia,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scope = condicaoEscopo("c.id_gestao_local", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(referencia, gestaoLocalId, superintendenciaId)
                .addValue("d30", referencia.plusDays(30))
                .addValue("d90", referencia.plusDays(90))
                .addValue("d180", referencia.plusDays(180));
        String sql = """
                SELECT
                  COALESCE(SUM(CASE WHEN c.data_fim < :referencia THEN 1 ELSE 0 END), 0) AS vencidos,
                  COALESCE(SUM(CASE WHEN c.data_fim BETWEEN :referencia AND :d30 THEN 1 ELSE 0 END), 0) AS ate30,
                  COALESCE(SUM(CASE WHEN c.data_fim > :d30 AND c.data_fim <= :d90 THEN 1 ELSE 0 END), 0) AS d31a90,
                  COALESCE(SUM(CASE WHEN c.data_fim > :d90 AND c.data_fim <= :d180 THEN 1 ELSE 0 END), 0) AS d91a180,
                  COALESCE(SUM(CASE WHEN c.data_fim > :d180 THEN 1 ELSE 0 END), 0) AS acima180,
                  COALESCE(SUM(CASE WHEN c.data_fim IS NULL THEN 1 ELSE 0 END), 0) AS sem_data
                  FROM contrato c
                 WHERE COALESCE(c.ativo, TRUE) = TRUE
                """ + scope;
        Map<String, Object> row = namedJdbcTemplate.queryForMap(sql, params);
        return new PainelGestaoDTO.HorizonteContratual(
                valorLong(row, "vencidos"),
                valorLong(row, "ate30"),
                valorLong(row, "d31a90"),
                valorLong(row, "d91a180"),
                valorLong(row, "acima180"),
                valorLong(row, "sem_data")
        );
    }

    public List<PainelGestaoDTO.DistribuicaoStatus> obterDistribuicaoStatus(
            LocalDate inicio,
            LocalDate fim,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scope = condicaoEscopoFrequencia("f", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(fim, gestaoLocalId, superintendenciaId)
                .addValue("inicio", inicio)
                .addValue("fim", fim);
        String sql = """
                SELECT COALESCE(f.status, 'SEM_STATUS') AS status,
                       COUNT(*) AS quantidade
                  FROM frequencia f
                  JOIN empregado e ON e.id_empregado = f.id_empregado
                 WHERE f.validade_operacional = 'VALIDA'
                   AND f.data_referencia BETWEEN :inicio AND :fim
                """ + scope + """
                 GROUP BY COALESCE(f.status, 'SEM_STATUS')
                 ORDER BY quantidade DESC, status
                """;
        List<Map<String, Object>> rows = namedJdbcTemplate.queryForList(sql, params);
        long total = rows.stream().mapToLong(row -> valorLong(row, "quantidade")).sum();
        return rows.stream().map(row -> new PainelGestaoDTO.DistribuicaoStatus(
                String.valueOf(row.get("status")),
                valorLong(row, "quantidade"),
                total > 0 ? arredondarUmaCasa(valorLong(row, "quantidade") * 100.0 / total) : 0.0
        )).toList();
    }

    public List<PainelGestaoDTO.SituacaoQuadro> obterSituacaoQuadro(
            LocalDate referencia,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scope = condicaoEscopo("e.id_gestao_local", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(referencia, gestaoLocalId, superintendenciaId);
        String sql = """
                SELECT UPPER(TRIM(COALESCE(s.descricao, 'SEM_STATUS'))) AS status,
                       COUNT(*) AS quantidade
                  FROM empregado e
                  LEFT JOIN status_empregado s ON s.id_status = e.id_status
                 WHERE (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                """ + scope + """
                 GROUP BY UPPER(TRIM(COALESCE(s.descricao, 'SEM_STATUS')))
                 ORDER BY quantidade DESC, status
                """;
        List<Map<String, Object>> rows = namedJdbcTemplate.queryForList(sql, params);
        long total = rows.stream().mapToLong(row -> valorLong(row, "quantidade")).sum();
        return rows.stream().map(row -> new PainelGestaoDTO.SituacaoQuadro(
                String.valueOf(row.get("status")),
                valorLong(row, "quantidade"),
                total > 0 ? arredondarUmaCasa(valorLong(row, "quantidade") * 100.0 / total) : 0.0
        )).toList();
    }

    public List<PainelGestaoDTO.PadraoSemanal> obterPadraoSemanal(
            LocalDate inicio,
            LocalDate fim,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scope = condicaoEscopoFrequencia("f", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(fim, gestaoLocalId, superintendenciaId)
                .addValue("inicio", inicio)
                .addValue("fim", fim);
        // O rótulo textual do dia é calculado em Java. Mantê-lo como CASE no SELECT,
        // a ordem numérica segue segunda=0 até domingo=6, como o contrato existente.
        String sql = """
                SELECT (EXTRACT(ISODOW FROM f.data_referencia) - 1) AS ordem,
                       COALESCE(SUM(CASE WHEN f.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR','FALTA','ATESTADO','SUSPENSAO') THEN 1 ELSE 0 END), 0) AS operacionais,
                       COALESCE(SUM(CASE WHEN f.status IN ('FALTA','ATESTADO','SUSPENSAO') THEN 1 ELSE 0 END), 0) AS ausencias
                  FROM frequencia f
                  JOIN empregado e ON e.id_empregado = f.id_empregado
                 WHERE f.validade_operacional = 'VALIDA'
                   AND f.data_referencia BETWEEN :inicio AND :fim
                """ + scope + """
                 GROUP BY (EXTRACT(ISODOW FROM f.data_referencia) - 1)
                 ORDER BY (EXTRACT(ISODOW FROM f.data_referencia) - 1)
                """;
        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> {
            long operacionais = rs.getLong("operacionais");
            long ausencias = rs.getLong("ausencias");
            return new PainelGestaoDTO.PadraoSemanal(
                    rs.getInt("ordem"),
                    nomeDiaSemana(rs.getInt("ordem")),
                    operacionais,
                    ausencias,
                    operacionais > 0 ? arredondarUmaCasa(ausencias * 100.0 / operacionais) : 0.0
            );
        });
    }

    public PainelGestaoDTO.IntegridadeDados obterIntegridadeDados(
            LocalDate referencia,
            long ativosSemContrato,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String employeeScope = condicaoEscopo("e.id_gestao_local", gestaoLocalId, superintendenciaId);
        String contractScope = condicaoEscopo("c.id_gestao_local", gestaoLocalId, superintendenciaId);
        String jornadaScope = condicaoEscopo("j_cfg.id_gestao_local", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(referencia, gestaoLocalId, superintendenciaId);

        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM (
                      SELECT e_multi.id_empregado
                        FROM empregado e_multi
                        JOIN status_empregado s_multi ON s_multi.id_status = e_multi.id_status
                        JOIN empregado_contrato ec_multi ON ec_multi.id_empregado = e_multi.id_empregado
                        JOIN contrato c_multi ON c_multi.id_contrato = ec_multi.id_contrato
                       WHERE UPPER(TRIM(COALESCE(s_multi.descricao, ''))) = 'ATIVO'
                         AND c_multi.id_gestao_local = e_multi.id_gestao_local
                         AND COALESCE(c_multi.ativo, TRUE) = TRUE
                         AND (c_multi.data_inicio IS NULL OR c_multi.data_inicio <= :referencia)
                         AND (c_multi.data_fim IS NULL OR c_multi.data_fim >= :referencia)
                         %s
                       GROUP BY e_multi.id_empregado
                      HAVING COUNT(DISTINCT c_multi.id_contrato) > 1
                  ) multi_contratos) AS multiplos_contratos,
                  (SELECT COUNT(DISTINCT e.id_empregado)
                     FROM empregado e
                     JOIN empregado_contrato ec ON ec.id_empregado = e.id_empregado
                     JOIN contrato c ON c.id_contrato = ec.id_contrato
                    WHERE e.id_gestao_local IS NOT NULL AND c.id_gestao_local IS NOT NULL
                      AND e.id_gestao_local <> c.id_gestao_local
                      %s) AS contrato_divergente,
                  (SELECT COUNT(DISTINCT e.id_empregado)
                     FROM empregado e
                     JOIN lotacao l ON l.id_lotacao = e.id_lotacao
                    WHERE e.id_gestao_local IS NOT NULL AND l.id_gestao_local IS NOT NULL
                      AND e.id_gestao_local <> l.id_gestao_local
                      %s) AS lotacao_divergente,
                  (SELECT COUNT(DISTINCT e.id_empregado)
                     FROM empregado e
                     JOIN jornada j ON j.id_jornada = e.id_jornada
                    WHERE e.id_gestao_local IS NOT NULL AND j.id_gestao_local IS NOT NULL
                      AND e.id_gestao_local <> j.id_gestao_local
                      %s) AS jornada_divergente,
                  (SELECT COUNT(*)
                     FROM jornada j_cfg
                    WHERE (SELECT COUNT(*) FROM jornada_dia_semana jds_cfg
                            WHERE jds_cfg.id_jornada = j_cfg.id_jornada) <> 7
                      %s) AS jornadas_incompletas,
                  (SELECT COUNT(DISTINCT e.id_empregado)
                     FROM empregado e
                     JOIN status_empregado s_estrutura ON s_estrutura.id_status = e.id_status
                    WHERE UPPER(TRIM(COALESCE(s_estrutura.descricao, ''))) = 'ATIVO'
                      AND e.id_lotacao IS NULL
                      %s) AS ativos_sem_lotacao,
                  (SELECT COUNT(DISTINCT e.id_empregado)
                     FROM empregado e
                     JOIN status_empregado s_estrutura ON s_estrutura.id_status = e.id_status
                    WHERE UPPER(TRIM(COALESCE(s_estrutura.descricao, ''))) = 'ATIVO'
                      AND e.id_jornada IS NULL
                      %s) AS ativos_sem_jornada,
                  (SELECT COUNT(*)
                     FROM frequencia_adicional fa_rt_orfa
                     JOIN empregado e ON e.id_empregado = fa_rt_orfa.id_empregado
                    WHERE fa_rt_orfa.tipo = 'RT'
                      AND COALESCE(fa_rt_orfa.status, 'CALCULADO') <> 'CANCELADO'
                      AND fa_rt_orfa.id_frequencia IS NULL
                      %s) AS rt_sem_frequencia,
                  (SELECT COUNT(*)
                     FROM empregado e
                    WHERE e.id_gestao_local IS NULL
                      %s) AS empregados_sem_gestao_local,
                  (SELECT COUNT(*)
                     FROM contrato c
                    WHERE c.id_gestao_local IS NULL
                      %s) AS contratos_sem_gestao_local
                """.formatted(
                condicaoEscopo("e_multi.id_gestao_local", gestaoLocalId, superintendenciaId),
                employeeScope,
                employeeScope,
                employeeScope,
                jornadaScope,
                employeeScope,
                employeeScope,
                employeeScope,
                employeeScope,
                contractScope
        );

        Map<String, Object> row = namedJdbcTemplate.queryForMap(sql, params);
        long multiplosContratos = valorLong(row, "multiplos_contratos");
        long contratoDivergente = valorLong(row, "contrato_divergente");
        long lotacaoDivergente = valorLong(row, "lotacao_divergente");
        long jornadaDivergente = valorLong(row, "jornada_divergente");
        long jornadasIncompletas = valorLong(row, "jornadas_incompletas");
        long ativosSemLotacao = valorLong(row, "ativos_sem_lotacao");
        long ativosSemJornada = valorLong(row, "ativos_sem_jornada");
        long rtSemFrequencia = valorLong(row, "rt_sem_frequencia");
        long empregadosSemGestaoLocal = valorLong(row, "empregados_sem_gestao_local");
        long contratosSemGestaoLocal = valorLong(row, "contratos_sem_gestao_local");
        long total = ativosSemContrato + multiplosContratos + contratoDivergente + lotacaoDivergente
                + jornadaDivergente + jornadasIncompletas + ativosSemLotacao + ativosSemJornada
                + rtSemFrequencia + empregadosSemGestaoLocal + contratosSemGestaoLocal;
        return new PainelGestaoDTO.IntegridadeDados(
                total,
                ativosSemContrato,
                multiplosContratos,
                contratoDivergente,
                lotacaoDivergente,
                jornadaDivergente,
                jornadasIncompletas,
                ativosSemLotacao,
                ativosSemJornada,
                rtSemFrequencia,
                empregadosSemGestaoLocal,
                contratosSemGestaoLocal
        );
    }

    public List<PainelGestaoDTO.UnidadeGestao> obterUnidadesGestao(
            LocalDate referencia,
            String nivel,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        if (recorte != null) return unidadesRecortadas(referencia);
        List<PainelGestaoDTO.UnidadeGestao> unidades;
        if ("NACIONAL".equals(nivel)) unidades = unidadesPorSuperintendencia(referencia);
        else if ("SUPERINTENDENCIA".equals(nivel)) unidades = unidadesPorGestaoLocal(referencia, superintendenciaId);
        else unidades = unidadesPorLotacao(referencia, gestaoLocalId);

        Map<Long, CoberturaUnidade> cobertura = coberturaPorUnidade(
                referencia, nivel, gestaoLocalId, superintendenciaId
        );
        return unidades.stream().map(unidade -> comCobertura(unidade, cobertura.get(unidade.id()))).toList();
    }

    private List<PainelGestaoDTO.UnidadeGestao> unidadesPorSuperintendencia(LocalDate referencia) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("referencia", referencia);
        String sql = """
                SELECT se.id_superintendencia AS id, se.nome, se.uf,
                       COALESCE(ct.previsto, 0) AS previsto,
                       COALESCE(emp.atual, 0) AS atual,
                       COALESCE(ct.contratos, 0) AS contratos,
                       COALESCE(ct.alertas, 0) AS alertas,
                       COALESCE(freq.ausencias, 0) AS ausencias
                  FROM superintendencia_estadual se
                  LEFT JOIN (
                      SELECT cp.id_superintendencia,
                             COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                                                   AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                                                   AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                                              THEN COALESCE(c.efetivo_previsto, 0) ELSE 0 END), 0) AS previsto,
                             COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                                                   AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                                                   AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                                              THEN 1 ELSE 0 END), 0) AS contratos,
                             COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                                                   AND (c.data_fim < :referencia OR c.data_fim IS NULL
                                                        OR COALESCE(c.efetivo_previsto, 0) <= 0)
                                              THEN 1 ELSE 0 END), 0) AS alertas
                        FROM gestao_local cp
                        JOIN contrato c ON c.id_gestao_local = cp.id_gestao_local
                       GROUP BY cp.id_superintendencia
                  ) ct ON ct.id_superintendencia = se.id_superintendencia
                  LEFT JOIN (
                      SELECT cp.id_superintendencia,
                             COUNT(DISTINCT e.id_empregado) AS atual
                        FROM empregado e
                        JOIN status_empregado st ON st.id_status = e.id_status AND UPPER(TRIM(COALESCE(st.descricao, ''))) = 'ATIVO'
                        JOIN empregado_contrato ec ON ec.id_empregado = e.id_empregado
                        JOIN contrato c ON c.id_contrato = ec.id_contrato
                        JOIN gestao_local cp ON cp.id_gestao_local = e.id_gestao_local
                       WHERE c.id_gestao_local = e.id_gestao_local
                         AND (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                         AND COALESCE(c.ativo, TRUE) = TRUE
                         AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                         AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                       GROUP BY cp.id_superintendencia
                  ) emp ON emp.id_superintendencia = se.id_superintendencia
                  LEFT JOIN (
                      SELECT COALESCE(f.superintendencia_snapshot_id, cp.id_superintendencia) AS id_superintendencia,
                             COUNT(*) AS ausencias
                        FROM frequencia f
                        JOIN empregado e ON e.id_empregado = f.id_empregado
                        LEFT JOIN gestao_local cp ON cp.id_gestao_local = e.id_gestao_local
                       WHERE f.validade_operacional = 'VALIDA'
                         AND f.data_referencia = :referencia
                         AND f.status IN ('FALTA','ATESTADO','SUSPENSAO')
                       GROUP BY COALESCE(f.superintendencia_snapshot_id, cp.id_superintendencia)
                  ) freq ON freq.id_superintendencia = se.id_superintendencia
                 WHERE COALESCE(se.ativo, TRUE) = TRUE
                 ORDER BY se.uf
                """;
        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> unidadeComContrato(
                rs.getLong("id"), "SE", rs.getString("nome"), rs.getString("uf"),
                rs.getLong("previsto"), rs.getLong("atual"),
                rs.getLong("contratos"), rs.getLong("alertas"), rs.getLong("ausencias")
        ));
    }

    private List<PainelGestaoDTO.UnidadeGestao> unidadesPorGestaoLocal(LocalDate referencia, Long superintendenciaId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("referencia", referencia)
                .addValue("superintendenciaId", superintendenciaId);
        String sql = """
                SELECT cp.id_gestao_local AS id, cp.nome, se.uf,
                       COALESCE(ct.previsto, 0) AS previsto,
                       COALESCE(emp.atual, 0) AS atual,
                       COALESCE(ct.contratos, 0) AS contratos,
                       COALESCE(ct.alertas, 0) AS alertas,
                       COALESCE(freq.ausencias, 0) AS ausencias
                  FROM gestao_local cp
                  JOIN superintendencia_estadual se ON se.id_superintendencia = cp.id_superintendencia
                  LEFT JOIN (
                      SELECT c.id_gestao_local,
                             COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                                                   AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                                                   AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                                              THEN COALESCE(c.efetivo_previsto, 0) ELSE 0 END), 0) AS previsto,
                             COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                                                   AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                                                   AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                                              THEN 1 ELSE 0 END), 0) AS contratos,
                             COALESCE(SUM(CASE WHEN COALESCE(c.ativo, TRUE) = TRUE
                                                   AND (c.data_fim < :referencia OR c.data_fim IS NULL
                                                        OR COALESCE(c.efetivo_previsto, 0) <= 0)
                                              THEN 1 ELSE 0 END), 0) AS alertas
                        FROM contrato c
                       WHERE c.id_gestao_local IN (SELECT cp0.id_gestao_local FROM gestao_local cp0
                                             WHERE cp0.id_superintendencia = :superintendenciaId)
                       GROUP BY c.id_gestao_local
                  ) ct ON ct.id_gestao_local = cp.id_gestao_local
                  LEFT JOIN (
                      SELECT e.id_gestao_local,
                             COUNT(DISTINCT e.id_empregado) AS atual
                        FROM empregado e
                        JOIN status_empregado st ON st.id_status = e.id_status AND UPPER(TRIM(COALESCE(st.descricao, ''))) = 'ATIVO'
                        JOIN empregado_contrato ec ON ec.id_empregado = e.id_empregado
                        JOIN contrato c ON c.id_contrato = ec.id_contrato
                       WHERE e.id_gestao_local IN (SELECT cp0.id_gestao_local FROM gestao_local cp0
                                             WHERE cp0.id_superintendencia = :superintendenciaId)
                         AND (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                         AND c.id_gestao_local = e.id_gestao_local
                         AND COALESCE(c.ativo, TRUE) = TRUE
                         AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                         AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                       GROUP BY e.id_gestao_local
                  ) emp ON emp.id_gestao_local = cp.id_gestao_local
                  LEFT JOIN (
                      SELECT COALESCE(f.gestao_local_snapshot_id, e.id_gestao_local) AS id_gestao_local,
                             COUNT(*) AS ausencias
                        FROM frequencia f
                        JOIN empregado e ON e.id_empregado = f.id_empregado
                       WHERE f.validade_operacional = 'VALIDA'
                         AND f.data_referencia = :referencia
                         AND f.status IN ('FALTA','ATESTADO','SUSPENSAO')
                         AND (f.superintendencia_snapshot_id = :superintendenciaId
                              OR (f.superintendencia_snapshot_id IS NULL
                                  AND e.id_gestao_local IN (SELECT cp0.id_gestao_local FROM gestao_local cp0
                                                     WHERE cp0.id_superintendencia = :superintendenciaId)))
                       GROUP BY COALESCE(f.gestao_local_snapshot_id, e.id_gestao_local)
                  ) freq ON freq.id_gestao_local = cp.id_gestao_local
                 WHERE cp.id_superintendencia = :superintendenciaId
                   AND COALESCE(cp.ativo, TRUE) = TRUE
                 ORDER BY cp.nome
                """;
        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> unidadeComContrato(
                rs.getLong("id"), "GESTAO_LOCAL", rs.getString("nome"), rs.getString("uf"),
                rs.getLong("previsto"), rs.getLong("atual"),
                rs.getLong("contratos"), rs.getLong("alertas"), rs.getLong("ausencias")
        ));
    }

    private List<PainelGestaoDTO.UnidadeGestao> unidadesPorLotacao(LocalDate referencia, Long gestaoLocalId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("referencia", referencia)
                .addValue("gestaoLocalId", gestaoLocalId);
        String sql = """
                SELECT l.id_lotacao AS id, l.nome, se.uf,
                       COALESCE(emp.atual, 0) AS atual,
                       COALESCE(freq.ausencias, 0) AS ausencias
                  FROM lotacao l
                  JOIN gestao_local cp ON cp.id_gestao_local = l.id_gestao_local
                  JOIN superintendencia_estadual se ON se.id_superintendencia = cp.id_superintendencia
                  LEFT JOIN (
                      SELECT e.id_lotacao, COUNT(*) AS atual
                        FROM empregado e
                        JOIN status_empregado st ON st.id_status = e.id_status
                       WHERE e.id_gestao_local = :gestaoLocalId
                         AND UPPER(TRIM(COALESCE(st.descricao, ''))) = 'ATIVO'
                         AND (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                       GROUP BY e.id_lotacao
                  ) emp ON emp.id_lotacao = l.id_lotacao
                  LEFT JOIN (
                      SELECT COALESCE(f.lotacao_snapshot_id, e.id_lotacao) AS id_lotacao,
                             COUNT(*) AS ausencias
                        FROM frequencia f
                        JOIN empregado e ON e.id_empregado = f.id_empregado
                       WHERE f.validade_operacional = 'VALIDA'
                         AND f.data_referencia = :referencia
                         AND f.status IN ('FALTA','ATESTADO','SUSPENSAO')
                         AND (f.gestao_local_snapshot_id = :gestaoLocalId
                              OR (f.gestao_local_snapshot_id IS NULL AND e.id_gestao_local = :gestaoLocalId))
                       GROUP BY COALESCE(f.lotacao_snapshot_id, e.id_lotacao)
                  ) freq ON freq.id_lotacao = l.id_lotacao
                 WHERE l.id_gestao_local = :gestaoLocalId
                 ORDER BY l.nome
                """;
        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> {
            long atual = rs.getLong("atual");
            return new PainelGestaoDTO.UnidadeGestao(
                    rs.getLong("id"), "LOTACAO", rs.getString("nome"), rs.getString("uf"),
                    null, atual, null, null, 0, 0, rs.getLong("ausencias"),
                    0, 0, 0, 100.0
            );
        });
    }

    public List<PainelGestaoDTO.SerieTemporal> obterEvolucao(
            LocalDate referencia,
            int periodoDias,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scope = condicaoEscopoFrequencia("f", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(referencia, gestaoLocalId, superintendenciaId)
                .addValue("inicio", referencia.minusDays(periodoDias - 1L));
        String sql = """
                SELECT f.data_referencia AS data,
                       COUNT(*) AS registros,
                       COALESCE(SUM(CASE WHEN f.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR') THEN 1 ELSE 0 END), 0) AS presencas,
                       COALESCE(SUM(CASE WHEN f.status IN ('FALTA','ATESTADO','SUSPENSAO') THEN 1 ELSE 0 END), 0) AS ausencias,
                       COALESCE(SUM(CASE WHEN f.status = 'RT'
                                                   OR EXISTS (
                                                       SELECT 1 FROM frequencia_adicional fa_rt
                                                        WHERE fa_rt.id_empregado = f.id_empregado
                                                          AND fa_rt.data_referencia = f.data_referencia
                                                          AND fa_rt.tipo = 'RT'
                                                          AND COALESCE(fa_rt.status, 'CALCULADO') <> 'CANCELADO'
                                                   ) THEN 1 ELSE 0 END), 0) AS rts
                  FROM frequencia f
                  JOIN empregado e ON e.id_empregado = f.id_empregado
                 WHERE f.validade_operacional = 'VALIDA'
                   AND f.data_referencia BETWEEN :inicio AND :referencia
                """ + scope + """
                 GROUP BY f.data_referencia
                 ORDER BY f.data_referencia
                """;
        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> new PainelGestaoDTO.SerieTemporal(
                rs.getDate("data").toLocalDate(),
                rs.getLong("registros"),
                rs.getLong("presencas"),
                rs.getLong("ausencias"),
                rs.getLong("rts")
        ));
    }

    private PainelGestaoDTO.UnidadeGestao unidadeComContrato(
            Long id,
            String tipo,
            String nome,
            String uf,
            long previsto,
            long atual,
            long contratos,
            long alertas,
            long ausencias
    ) {
        long saldo = previsto - atual;
        double ocupacao = previsto > 0 ? arredondarUmaCasa(atual * 100.0 / previsto) : 0.0;
        return new PainelGestaoDTO.UnidadeGestao(
                id, tipo, nome, uf, previsto, atual, saldo, ocupacao,
                contratos, alertas, ausencias, 0, 0, 0, 100.0
        );
    }

    private PainelGestaoDTO.UnidadeGestao comCobertura(
            PainelGestaoDTO.UnidadeGestao unidade,
            CoberturaUnidade cobertura
    ) {
        CoberturaUnidade valor = cobertura != null ? cobertura : new CoberturaUnidade(0, 0, 0, 100.0);
        return new PainelGestaoDTO.UnidadeGestao(
                unidade.id(), unidade.tipo(), unidade.nome(), unidade.uf(),
                unidade.efetivoPrevisto(), unidade.efetivoAtual(), unidade.saldo(),
                unidade.percentualOcupacao(), unidade.contratosVigentes(),
                unidade.alertasContratuais(), unidade.ausenciasNaReferencia(),
                valor.empregadosEsperados(), valor.registrosLancados(),
                valor.pendentes(), valor.percentualCobertura()
        );
    }

    private Map<Long, CoberturaUnidade> coberturaPorUnidade(
            LocalDate referencia,
            String nivel,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String unidadeExpression = switch (nivel) {
            case "NACIONAL" -> "se.id_superintendencia";
            case "SUPERINTENDENCIA" -> "e.id_gestao_local";
            default -> "e.id_lotacao";
        };
        String scope = condicaoEscopo("e.id_gestao_local", gestaoLocalId, superintendenciaId);
        MapSqlParameterSource params = parametrosBase(referencia, gestaoLocalId, superintendenciaId)
                .addValue("diaSemana", referencia.getDayOfWeek().name());

        String sql = """
                SELECT base.unidade_id,
                       COALESCE(SUM(CASE WHEN base.esperado = 1 THEN 1 ELSE 0 END), 0) AS esperados,
                       COALESCE(SUM(CASE WHEN base.esperado = 1 AND base.id_frequencia IS NOT NULL THEN 1 ELSE 0 END), 0) AS registros
                  FROM (
                       SELECT %s AS unidade_id,
                              f.id AS id_frequencia,
                              CASE WHEN COALESCE(
                                         jds.dia_trabalho,
                                         CASE
                                             WHEN e.id_jornada IS NULL THEN FALSE
                                             WHEN EXISTS (
                                                 SELECT 1 FROM jornada_dia_semana jds_any
                                                  WHERE jds_any.id_jornada = e.id_jornada
                                             ) THEN FALSE
                                             WHEN :diaSemana IN ('SATURDAY', 'SUNDAY') THEN FALSE
                                             ELSE TRUE
                                         END
                                     ) = TRUE
                                  AND NOT EXISTS (
                                      SELECT 1 FROM frequencia_excecao fx
                                       WHERE fx.id_empregado = e.id_empregado
                                         AND :referencia BETWEEN fx.data_inicio AND fx.data_fim
                                         AND fx.status = 'APROVADO'
                                         AND fx.tipo = 'FOLGA_REMUNERADA'
                                  )
                                  AND NOT EXISTS (
                                      SELECT 1 FROM calendario_evento ce
                                       WHERE ce.data_evento = :referencia
                                         AND COALESCE(ce.ativo, TRUE) = TRUE
                                         AND ce.tipo IN ('FERIADO','DIA_DESCANSO')
                                         AND (
                                             ce.abrangencia = 'NACIONAL'
                                             OR (ce.abrangencia = 'ESTADUAL'
                                                 AND (ce.id_superintendencia = se.id_superintendencia
                                                      OR (ce.id_superintendencia IS NULL AND ce.uf = se.uf)))
                                             OR (ce.abrangencia = 'MUNICIPAL'
                                                 AND ce.municipio = l.municipio
                                                 AND (ce.id_gestao_local IS NULL OR ce.id_gestao_local = e.id_gestao_local))
                                             OR (ce.abrangencia = 'LOTACAO' AND ce.id_lotacao = e.id_lotacao)
                                         )
                                  )
                              THEN 1 ELSE 0 END AS esperado
                         FROM empregado e
                         JOIN status_empregado s ON s.id_status = e.id_status
                         JOIN lotacao l ON l.id_lotacao = e.id_lotacao
                         JOIN gestao_local cp ON cp.id_gestao_local = e.id_gestao_local
                         JOIN superintendencia_estadual se ON se.id_superintendencia = cp.id_superintendencia
                         LEFT JOIN jornada_dia_semana jds ON jds.id_jornada = e.id_jornada
                                AND jds.dia_semana = :diaSemana
                         LEFT JOIN frequencia f ON f.id_empregado = e.id_empregado
                                AND f.data_referencia = :referencia
                             AND f.validade_operacional = 'VALIDA'
                        WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO'
                          AND (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                          %s
                  ) base
                 GROUP BY base.unidade_id
                """.formatted(unidadeExpression, scope);

        List<Map<String, Object>> rows = namedJdbcTemplate.queryForList(sql, params);
        Map<Long, CoberturaUnidade> resultado = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object idValue = row.get("unidade_id");
            if (!(idValue instanceof Number number)) continue;
            long esperados = valorLong(row, "esperados");
            long registros = valorLong(row, "registros");
            long pendentes = Math.max(0, esperados - registros);
            double percentual = esperados > 0 ? arredondarUmaCasa(registros * 100.0 / esperados) : 100.0;
            resultado.put(number.longValue(), new CoberturaUnidade(esperados, registros, pendentes, percentual));
        }
        return resultado;
    }

    private record CoberturaUnidade(
            long empregadosEsperados,
            long registrosLancados,
            long pendentes,
            double percentualCobertura
    ) {}

    private MapSqlParameterSource parametrosBase(LocalDate referencia, Long gestaoLocalId, Long superintendenciaId) {
        return new MapSqlParameterSource()
                .addValue("referencia", referencia)
                .addValue("scopeGestaoLocalId", gestaoLocalId)
                .addValue("scopeSuperintendenciaId", superintendenciaId)
                .addValue("recorteContrato", recorte == null ? null : recorte.contrato())
                .addValue("recortePrestador", recorte == null ? null : recorte.prestador())
                .addValue("recorteLotacao", lotacaoRecorte());
    }

    /** Restringe as frequências pelo retrato organizacional gravado no cálculo. */
    private String condicaoEscopoFrequencia(
            String frequenciaAlias,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String escopo = gestaoLocalId != null ? " AND " + frequenciaAlias + ".gestao_local_snapshot_id = :scopeGestaoLocalId "
                : superintendenciaId != null ? " AND " + frequenciaAlias + ".superintendencia_snapshot_id = :scopeSuperintendenciaId " : "";
        if (recorte == null) return escopo;
        return escopo
                + (recorte.contrato() == null ? "" : " AND " + frequenciaAlias + ".contrato_snapshot_id = :recorteContrato ")
                + (recorte.prestador() == null ? "" : " AND " + frequenciaAlias + ".contrato_snapshot_id IN (SELECT id_contrato FROM contrato WHERE id_fornecedor = :recortePrestador) ")
                + (lotacaoRecorte() == null ? "" : " AND " + frequenciaAlias + ".lotacao_snapshot_id = :recorteLotacao ");
    }

    private String condicaoEscopo(String gestaoLocalColumn, Long gestaoLocalId, Long superintendenciaId) {
        String escopo = gestaoLocalId != null ? " AND " + gestaoLocalColumn + " = :scopeGestaoLocalId "
                : superintendenciaId != null ? " AND " + gestaoLocalColumn + " IN (SELECT cp_scope.id_gestao_local FROM gestao_local cp_scope WHERE cp_scope.id_superintendencia = :scopeSuperintendenciaId) " : "";
        if (recorte == null) return escopo;
        String alias = gestaoLocalColumn.substring(0, gestaoLocalColumn.indexOf('.'));
        return escopo + switch (alias) {
            case "e", "e_multi" -> filtroCadastro(alias);
            case "c", "c_vinculo" -> filtroContrato(alias);
            case "l" -> (lotacaoRecorte() == null ? "" : " AND l.id_lotacao = :recorteLotacao ")
                    + " AND EXISTS (SELECT 1 FROM empregado e_rec WHERE e_rec.id_lotacao = l.id_lotacao " + filtroCadastro("e_rec") + ") ";
            case "j", "j_cfg" -> " AND EXISTS (SELECT 1 FROM empregado e_rec WHERE e_rec.id_jornada = " + alias + ".id_jornada " + filtroCadastro("e_rec") + ") ";
            default -> throw new IllegalArgumentException("Alias de recorte não suportado");
        };
    }

    private Long lotacaoRecorte() {
        return recorte != null && recorte.tipo() == com.example.siggtrat.dto.PainelAnaliticoDTO.TipoUnidade.LOTACAO ? recorte.unidade() : null;
    }

    private String filtroCadastro(String alias) {
        return (lotacaoRecorte() == null ? "" : " AND " + alias + ".id_lotacao = :recorteLotacao ")
                + (recorte.contrato() == null && recorte.prestador() == null ? "" :
                    " AND EXISTS (SELECT 1 FROM empregado_contrato ec_rec JOIN contrato c_rec ON c_rec.id_contrato = ec_rec.id_contrato WHERE ec_rec.id_empregado = "
                    + alias + ".id_empregado " + filtroContrato("c_rec") + ") ");
    }

    private String filtroContrato(String alias) {
        return (recorte.contrato() == null ? "" : " AND " + alias + ".id_contrato = :recorteContrato ")
                + (recorte.prestador() == null ? "" : " AND " + alias + ".id_fornecedor = :recortePrestador ")
                + (lotacaoRecorte() == null ? "" : " AND (EXISTS (SELECT 1 FROM empregado_contrato ec_loc JOIN empregado e_loc ON e_loc.id_empregado=ec_loc.id_empregado WHERE ec_loc.id_contrato="
                    + alias + ".id_contrato AND e_loc.id_lotacao=:recorteLotacao) OR EXISTS (SELECT 1 FROM composicao_contratual comp_rec WHERE comp_rec.id_contrato="
                    + alias + ".id_contrato AND comp_rec.id_lotacao=:recorteLotacao AND comp_rec.data_inicio_vigencia<=:referencia AND (comp_rec.data_fim_vigencia IS NULL OR comp_rec.data_fim_vigencia>=:referencia))) ");
    }

    private List<PainelGestaoDTO.UnidadeGestao> unidadesRecortadas(LocalDate ref) {
        return unidadesPagina.stream().map(u -> {
            Long gestao = recorte.tipo() == com.example.siggtrat.dto.PainelAnaliticoDTO.TipoUnidade.GESTAO_LOCAL ? u.id() : recorte.gestao();
            Long se = recorte.tipo() == com.example.siggtrat.dto.PainelAnaliticoDTO.TipoUnidade.SUPERINTENDENCIA ? u.id() : recorte.superintendencia();
            Long lotacao = recorte.tipo() == com.example.siggtrat.dto.PainelAnaliticoDTO.TipoUnidade.LOTACAO ? u.id() : null;
            var contratos = efetivoTemporal.obterDetalhamentoContratos(ref, recorte.contrato(), lotacao, null, null, gestao, se, recorte.prestador());
            long previsto = contratos.stream().mapToLong(PainelEfetivoDTO.DetalheContrato::efetivoPrevisto).sum();
            long alocado = contratos.stream().mapToLong(PainelEfetivoDTO.DetalheContrato::efetivoAlocado).sum();
            var bases = efetivoTemporal.obterCoberturaAnalitica(ref, ref, recorte.contrato(), lotacao, gestao, se, recorte.tipo(), recorte.prestador());
            long esperados = bases.stream().mapToLong(PainelEfetivoRepositorio.BaseCobertura::esperados).sum();
            long lancados = bases.stream().mapToLong(PainelEfetivoRepositorio.BaseCobertura::lancados).sum();
            long pendentes = bases.stream().mapToLong(PainelEfetivoRepositorio.BaseCobertura::pendentes).sum();
            var diario = noRecorte(new PainelAnaliticoRepositorio.Filtro(gestao, se, recorte.contrato(), recorte.tipo(), u.id(), recorte.prestador()), List.of())
                    .obterAnalisePeriodo(ref, ref, gestao, se);
            return new PainelGestaoDTO.UnidadeGestao(u.id(), recorte.tipo().name(), u.nome(), null,
                    previsto, alocado, previsto-alocado, previsto == 0 ? null : alocado*100D/previsto,
                    contratos.size(), 0, diario.faltasNaoJustificadas() + diario.atestados() + diario.suspensoes(), esperados, lancados, pendentes, esperados == 0 ? 0 : lancados*100D/esperados);
        }).toList();
    }

    private long valorLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private LocalDateTime valorDataHora(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        return null;
    }

    private double arredondarUmaCasa(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String nomeDiaSemana(int ordem) {
        return switch (ordem) {
            case 0 -> "Segunda";
            case 1 -> "Terça";
            case 2 -> "Quarta";
            case 3 -> "Quinta";
            case 4 -> "Sexta";
            case 5 -> "Sábado";
            case 6 -> "Domingo";
            default -> "Dia inválido";
        };
    }

    public List<PainelRankingDTO> obterRankingLotacoes(
            LocalDate inicio,
            LocalDate fim,
            Long contratoId,
            Long gestaoLocalId,
            Long superintendenciaId
    ) {
        String scopeLotacao = condicaoEscopo("l.id_gestao_local", gestaoLocalId, superintendenciaId);
        String scopeEmpregado = condicaoEscopo("e.id_gestao_local", gestaoLocalId, superintendenciaId);
        String scopeFrequencia = condicaoEscopoFrequencia("f", gestaoLocalId, superintendenciaId);

        String filtroContratoAtual = contratoId == null ? "" : """
                AND EXISTS (
                    SELECT 1
                      FROM empregado_contrato ec_r
                     WHERE ec_r.id_empregado = e.id_empregado
                       AND ec_r.id_contrato = :contratoId
                )
                """;
        String filtroContratoHistorico = contratoId == null ? "" : """
                AND f.contrato_snapshot_id = :contratoId
                """;

        MapSqlParameterSource params = parametrosBase(fim, gestaoLocalId, superintendenciaId)
                .addValue("inicio", inicio)
                .addValue("fim", fim)
                .addValue("contratoId", contratoId);

        /*
         * O ranking mede registros operacionais do período, não cobertura da escala.
         * Por isso presenças/ausências são atribuídas à lotação e à jornada gravadas
         * na frequência. Já "escalados" permanece como fotografia do quadro ativo ao
         * fim da referência, deixando os conceitos explicitamente separados.
         */
        String sql = """
                SELECT dim.lotacao_id,
                       dim.nome,
                       dim.jornada_id,
                       dim.turno,
                       COALESCE(at.ativos, 0) AS escalados,
                       COALESCE(op.presencas, 0) AS presencas,
                       COALESCE(op.faltas, 0) AS faltas
                  FROM (
                      SELECT d.lotacao_id,
                             COALESCE(MAX(NULLIF(d.nome, '')), CONCAT('Lotação ', d.lotacao_id)) AS nome,
                             d.jornada_id,
                             COALESCE(MAX(NULLIF(d.turno, '')), CONCAT('Jornada ', d.jornada_id)) AS turno
                        FROM (
                            SELECT l.id_lotacao AS lotacao_id,
                                   l.nome AS nome,
                                   j.id_jornada AS jornada_id,
                                   j.descricao AS turno
                              FROM lotacao l
                              JOIN lotacao_jornada lj ON lj.id_lotacao = l.id_lotacao
                              JOIN jornada j ON j.id_jornada = lj.id_jornada AND j.id_gestao_local = l.id_gestao_local
                             WHERE 1 = 1
                """ + scopeLotacao + """
                            UNION ALL
                            SELECT f.lotacao_snapshot_id AS lotacao_id,
                                   f.lotacao_snapshot_nome AS nome,
                                   f.jornada_snapshot_id AS jornada_id,
                                   f.jornada_snapshot_descricao AS turno
                              FROM frequencia f
                             WHERE f.validade_operacional = 'VALIDA'
                   AND f.data_referencia BETWEEN :inicio AND :fim
                               AND f.lotacao_snapshot_id IS NOT NULL
                               AND f.jornada_snapshot_id IS NOT NULL
                """ + scopeFrequencia + filtroContratoHistorico + """
                        ) d
                       GROUP BY d.lotacao_id, d.jornada_id
                  ) dim
                  LEFT JOIN (
                      SELECT e.id_lotacao AS lotacao_id,
                             e.id_jornada AS jornada_id,
                             COUNT(DISTINCT e.id_empregado) AS ativos
                        FROM empregado e
                        JOIN status_empregado s ON s.id_status = e.id_status
                       WHERE UPPER(TRIM(COALESCE(s.descricao, ''))) = 'ATIVO'
                         AND e.id_lotacao IS NOT NULL
                         AND e.id_jornada IS NOT NULL
                         AND (e.data_admissao IS NULL OR e.data_admissao <= :fim)
                """ + scopeEmpregado + filtroContratoAtual + """
                       GROUP BY e.id_lotacao, e.id_jornada
                  ) at ON at.lotacao_id = dim.lotacao_id AND at.jornada_id = dim.jornada_id
                  LEFT JOIN (
                      SELECT f.lotacao_snapshot_id AS lotacao_id,
                             f.jornada_snapshot_id AS jornada_id,
                             COALESCE(SUM(CASE WHEN f.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR') THEN 1 ELSE 0 END), 0) AS presencas,
                             COALESCE(SUM(CASE WHEN f.status IN ('FALTA','ATESTADO','SUSPENSAO') THEN 1 ELSE 0 END), 0) AS faltas
                        FROM frequencia f
                       WHERE f.validade_operacional = 'VALIDA'
                   AND f.data_referencia BETWEEN :inicio AND :fim
                         AND f.lotacao_snapshot_id IS NOT NULL
                         AND f.jornada_snapshot_id IS NOT NULL
                """ + scopeFrequencia + filtroContratoHistorico + """
                       GROUP BY f.lotacao_snapshot_id, f.jornada_snapshot_id
                  ) op ON op.lotacao_id = dim.lotacao_id AND op.jornada_id = dim.jornada_id
                 ORDER BY dim.nome, dim.turno
                """;

        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> {
            int presencas = rs.getInt("presencas");
            int faltas = rs.getInt("faltas");
            int registrosOperacionais = presencas + faltas;
            int taxa = registrosOperacionais == 0
                    ? 0
                    : (int) Math.round(presencas * 100.0 / registrosOperacionais);
            return new PainelRankingDTO(
                    rs.getLong("lotacao_id"),
                    rs.getString("nome"),
                    rs.getLong("jornada_id"),
                    rs.getString("turno"),
                    rs.getInt("escalados"),
                    presencas,
                    faltas,
                    taxa
            );
        });
    }


}
