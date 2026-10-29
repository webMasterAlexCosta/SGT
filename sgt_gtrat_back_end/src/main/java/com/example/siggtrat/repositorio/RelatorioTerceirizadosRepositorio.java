package com.example.siggtrat.repositorio;

import com.example.siggtrat.dto.RelatorioTerceirizadosDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

/**
 * Consultas específicas da visão consolidada. Não usa Page<Empregado> nem
 * serializa entidades: com bases grandes isso evita materializar centenas de
 * milhares de registros apenas para calcular cartões e gráficos.
 */
@Repository
public class RelatorioTerceirizadosRepositorio {

    private final NamedParameterJdbcTemplate jdbc;

    public RelatorioTerceirizadosRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public RelatorioTerceirizadosDTO consultar(
            LocalDate referencia,
            String escopo,
            Long superintendenciaId,
            Long gestaoLocalId,
            String busca,
            String status,
            Long contratoId,
            boolean semContrato,
            Long lotacaoId,
            int pagina,
            int tamanho
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("referencia", referencia)
                .addValue("inicioMes", referencia.withDayOfMonth(1))
                .addValue("fimMes", referencia.withDayOfMonth(1).plusMonths(1))
                .addValue("superintendenciaId", superintendenciaId)
                .addValue("gestaoLocalId", gestaoLocalId)
                .addValue("status", status)
                .addValue("contratoId", contratoId)
                .addValue("lotacaoId", lotacaoId)
                .addValue("busca", busca == null || busca.isBlank() ? null : "%" + busca.trim().toLowerCase(Locale.ROOT) + "%");

        String filtro = filtroEmpregados(superintendenciaId, gestaoLocalId, busca, status, contratoId, semContrato, lotacaoId);
        RelatorioTerceirizadosDTO.Indicadores indicadoresEmpregados = indicadoresEmpregados(params, filtro);
        IndicadoresContratuais contrato = indicadoresContratuais(params, superintendenciaId, gestaoLocalId, contratoId);
        RelatorioTerceirizadosDTO.Indicadores indicadores = new RelatorioTerceirizadosDTO.Indicadores(
                indicadoresEmpregados.empregados(),
                indicadoresEmpregados.ativos(),
                indicadoresEmpregados.semContratoVigente(),
                indicadoresEmpregados.admitidosNoMes(),
                contrato.contratosVigentes(),
                contrato.efetivoPrevisto(),
                contrato.efetivoVinculado(),
                contrato.efetivoPrevisto() - contrato.efetivoVinculado()
        );

        List<RelatorioTerceirizadosDTO.Distribuicao> distribuicaoStatus = distribuicaoStatus(params, filtro);
        List<RelatorioTerceirizadosDTO.DistribuicaoContrato> distribuicaoContratos = distribuicaoContratos(params, filtro);
        List<RelatorioTerceirizadosDTO.OpcaoTexto> statusDisponiveis = statusDisponiveis();
        List<RelatorioTerceirizadosDTO.Opcao> lotacoes = lotacoes(superintendenciaId, gestaoLocalId, params);
        List<RelatorioTerceirizadosDTO.Opcao> contratos = contratos(superintendenciaId, gestaoLocalId, params);
        RelatorioTerceirizadosDTO.PaginaEmpregados empregados = empregados(params, filtro, pagina, tamanho, indicadores.empregados());

        return new RelatorioTerceirizadosDTO(
                referencia,
                com.example.siggtrat.utilitario.RelogioSistema.agora(),
                escopo,
                indicadores,
                distribuicaoStatus,
                distribuicaoContratos,
                statusDisponiveis,
                lotacoes,
                contratos,
                empregados
        );
    }



    /**
     * Lê colaboradores para exportação completa por cursor (keyset pagination).
     * Não usa OFFSET crescente, portanto o custo permanece estável mesmo em
     * bases com centenas de milhares de colaboradores.
     */
    public List<RelatorioTerceirizadosDTO.EmpregadoResumo> buscarEmpregadosParaExportacao(
            LocalDate referencia,
            Long superintendenciaId,
            Long gestaoLocalId,
            String busca,
            String status,
            Long contratoId,
            boolean semContrato,
            Long lotacaoId,
            long ultimoId,
            int tamanho
    ) {
        int limite = Math.min(Math.max(tamanho, 100), 5000);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("referencia", referencia)
                .addValue("superintendenciaId", superintendenciaId)
                .addValue("gestaoLocalId", gestaoLocalId)
                .addValue("status", status)
                .addValue("contratoId", contratoId)
                .addValue("lotacaoId", lotacaoId)
                .addValue("ultimoId", Math.max(ultimoId, 0L))
                .addValue("busca", busca == null || busca.isBlank()
                        ? null : "%" + busca.trim().toLowerCase(Locale.ROOT) + "%");

        String filtro = filtroEmpregados(superintendenciaId, gestaoLocalId, busca, status, contratoId, semContrato, lotacaoId);
        String sql = """
                SELECT e.id_empregado AS id,
                       e.nome,
                       e.matricula,
                       e.cpf,
                       s.descricao AS status_descricao,
                       l.id_lotacao AS lotacao_id,
                       l.nome AS lotacao_nome,
                       cg.nome AS cargo_nome,
                       j.descricao AS jornada_descricao,
                       e.data_admissao
                  FROM empregado e
                  LEFT JOIN status_empregado s ON s.id_status = e.id_status
                  LEFT JOIN cargo cg ON cg.id_cargo = e.id_cargo
                  LEFT JOIN lotacao l ON l.id_lotacao = e.id_lotacao
                  LEFT JOIN jornada j ON j.id_jornada = e.id_jornada
                """ + filtro + " AND e.id_empregado > :ultimoId ORDER BY e.id_empregado LIMIT " + limite;

        List<EmpregadoBase> bases = jdbc.query(sql, params, (rs, n) -> new EmpregadoBase(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("matricula"),
                rs.getString("cpf"),
                rs.getString("status_descricao"),
                longOuNulo(rs.getObject("lotacao_id")),
                rs.getString("lotacao_nome"),
                rs.getString("cargo_nome"),
                rs.getString("jornada_descricao"),
                rs.getDate("data_admissao") == null ? null : rs.getDate("data_admissao").toLocalDate()
        ));

        Map<Long, List<RelatorioTerceirizadosDTO.ContratoResumo>> contratosPorEmpregado =
                contratosDaPagina(bases.stream().map(EmpregadoBase::id).toList(), params);
        return bases.stream().map(base -> new RelatorioTerceirizadosDTO.EmpregadoResumo(
                base.id(), base.nome(), base.matricula(), base.cpf(), base.statusDescricao(),
                base.lotacaoId(), base.lotacaoNome(), base.cargoNome(), base.jornadaDescricao(),
                base.dataAdmissao(), contratosPorEmpregado.getOrDefault(base.id(), List.of())
        )).toList();
    }

    private RelatorioTerceirizadosDTO.Indicadores indicadoresEmpregados(
            MapSqlParameterSource params,
            String filtro
    ) {
        String sql = """
                SELECT COUNT(*) AS empregados,
                       COALESCE(SUM(CASE WHEN s.descricao = 'ATIVO' THEN 1 ELSE 0 END), 0) AS ativos,
                       COALESCE(SUM(CASE WHEN NOT EXISTS (
                           SELECT 1
                             FROM empregado_contrato ec_v
                             JOIN contrato c_v ON c_v.id_contrato = ec_v.id_contrato
                            WHERE ec_v.id_empregado = e.id_empregado
                              AND c_v.id_gestao_local = e.id_gestao_local
                              AND COALESCE(c_v.ativo, TRUE) = TRUE
                              AND (c_v.data_inicio IS NULL OR c_v.data_inicio <= :referencia)
                              AND (c_v.data_fim IS NULL OR c_v.data_fim >= :referencia)
                       ) THEN 1 ELSE 0 END), 0) AS sem_contrato,
                       COALESCE(SUM(CASE WHEN e.data_admissao >= :inicioMes AND e.data_admissao < :fimMes
                                         AND e.data_admissao <= :referencia
                                         THEN 1 ELSE 0 END), 0) AS admitidos_mes
                  FROM empregado e
                  LEFT JOIN status_empregado s ON s.id_status = e.id_status
                  LEFT JOIN cargo cg ON cg.id_cargo = e.id_cargo
                  LEFT JOIN lotacao l ON l.id_lotacao = e.id_lotacao
                """ + filtro;
        Map<String, Object> row = jdbc.queryForMap(sql, params);
        return new RelatorioTerceirizadosDTO.Indicadores(
                numero(row, "empregados"),
                numero(row, "ativos"),
                numero(row, "sem_contrato"),
                numero(row, "admitidos_mes"),
                0, 0, 0, 0
        );
    }

    private IndicadoresContratuais indicadoresContratuais(
            MapSqlParameterSource params,
            Long superintendenciaId,
            Long gestaoLocalId,
            Long contratoId
    ) {
        String scope = gestaoLocalId != null
                ? " AND c.id_gestao_local = :gestaoLocalId "
                : superintendenciaId != null
                    ? " AND c.id_gestao_local IN (SELECT gl.id_gestao_local FROM gestao_local gl WHERE gl.id_superintendencia = :superintendenciaId) "
                    : "";
        String contrato = contratoId == null ? "" : " AND c.id_contrato = :contratoId ";
        String sql = """
                SELECT COUNT(*) AS contratos_vigentes,
                       COALESCE(SUM(base.efetivo_previsto), 0) AS efetivo_previsto,
                       COALESCE(SUM(base.efetivo_vinculado), 0) AS efetivo_vinculado
                  FROM (
                    SELECT c.id_contrato,
                           COALESCE(c.efetivo_previsto, 0) AS efetivo_previsto,
                           COUNT(DISTINCT CASE WHEN s.descricao = 'ATIVO'
                                                AND e.id_gestao_local = c.id_gestao_local
                                                AND (e.data_admissao IS NULL OR e.data_admissao <= :referencia)
                                              THEN e.id_empregado END) AS efetivo_vinculado
                      FROM contrato c
                      LEFT JOIN empregado_contrato ec ON ec.id_contrato = c.id_contrato
                      LEFT JOIN empregado e ON e.id_empregado = ec.id_empregado
                      LEFT JOIN status_empregado s ON s.id_status = e.id_status
                     WHERE COALESCE(c.ativo, TRUE) = TRUE
                       AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                       AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                """ + scope + contrato + """
                     GROUP BY c.id_contrato, c.efetivo_previsto
                  ) base
                """;
        Map<String, Object> row = jdbc.queryForMap(sql, params);
        return new IndicadoresContratuais(
                numero(row, "contratos_vigentes"),
                numero(row, "efetivo_previsto"),
                numero(row, "efetivo_vinculado")
        );
    }

    private List<RelatorioTerceirizadosDTO.Distribuicao> distribuicaoStatus(
            MapSqlParameterSource params,
            String filtro
    ) {
        String sql = """
                SELECT COALESCE(s.descricao, 'SEM STATUS') AS nome,
                       COUNT(*) AS quantidade
                  FROM empregado e
                  LEFT JOIN status_empregado s ON s.id_status = e.id_status
                  LEFT JOIN cargo cg ON cg.id_cargo = e.id_cargo
                  LEFT JOIN lotacao l ON l.id_lotacao = e.id_lotacao
                """ + filtro + """
                 GROUP BY COALESCE(s.descricao, 'SEM STATUS')
                 ORDER BY quantidade DESC, nome
                """;
        return jdbc.query(sql, params, (rs, n) -> new RelatorioTerceirizadosDTO.Distribuicao(
                rs.getString("nome"), rs.getLong("quantidade")
        ));
    }

    private List<RelatorioTerceirizadosDTO.DistribuicaoContrato> distribuicaoContratos(
            MapSqlParameterSource params,
            String filtro
    ) {
        String sql = """
                SELECT cv.id_contrato AS id,
                       COALESCE(NULLIF(cv.numero_contrato, ''), NULLIF(cv.descricao, ''), 'Sem contrato vigente') AS nome,
                       COUNT(DISTINCT e.id_empregado) AS quantidade
                  FROM empregado e
                  LEFT JOIN status_empregado s ON s.id_status = e.id_status
                  LEFT JOIN cargo cg ON cg.id_cargo = e.id_cargo
                  LEFT JOIN lotacao l ON l.id_lotacao = e.id_lotacao
                  LEFT JOIN (
                      SELECT ec.id_empregado, c.id_contrato, c.id_gestao_local, c.numero_contrato, c.descricao
                        FROM empregado_contrato ec
                        JOIN contrato c ON c.id_contrato = ec.id_contrato
                       WHERE COALESCE(c.ativo, TRUE) = TRUE
                         AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                         AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                  ) cv ON cv.id_empregado = e.id_empregado AND cv.id_gestao_local = e.id_gestao_local
                """ + filtro + """
                 GROUP BY cv.id_contrato, COALESCE(NULLIF(cv.numero_contrato, ''), NULLIF(cv.descricao, ''), 'Sem contrato vigente')
                 ORDER BY quantidade DESC, nome
                 LIMIT 8
                """;
        return jdbc.query(sql, params, (rs, n) -> {
            Long id = rs.getObject("id") == null ? null : rs.getLong("id");
            return new RelatorioTerceirizadosDTO.DistribuicaoContrato(
                    id,
                    rs.getString("nome"),
                    rs.getLong("quantidade"),
                    id == null
            );
        });
    }

    private List<RelatorioTerceirizadosDTO.OpcaoTexto> statusDisponiveis() {
        String sql = "SELECT descricao FROM status_empregado ORDER BY descricao";
        return jdbc.query(sql, (rs, n) -> new RelatorioTerceirizadosDTO.OpcaoTexto(
                rs.getString("descricao"), rs.getString("descricao")
        ));
    }

    private List<RelatorioTerceirizadosDTO.Opcao> lotacoes(
            Long superintendenciaId, Long gestaoLocalId, MapSqlParameterSource params) {
        String scope = gestaoLocalId != null
                ? " WHERE l.id_gestao_local = :gestaoLocalId "
                : superintendenciaId != null
                    ? " WHERE l.id_gestao_local IN (SELECT gl.id_gestao_local FROM gestao_local gl WHERE gl.id_superintendencia = :superintendenciaId) "
                    : "";
        String sql = "SELECT l.id_lotacao AS id, l.nome FROM lotacao l" + scope + " ORDER BY l.nome";
        return jdbc.query(sql, params, (rs, n) -> new RelatorioTerceirizadosDTO.Opcao(
                rs.getLong("id"), rs.getString("nome")
        ));
    }

    private List<RelatorioTerceirizadosDTO.Opcao> contratos(
            Long superintendenciaId,
            Long gestaoLocalId,
            MapSqlParameterSource params
    ) {
        String scope = gestaoLocalId != null
                ? " AND c.id_gestao_local = :gestaoLocalId "
                : superintendenciaId != null
                    ? " AND c.id_gestao_local IN (SELECT gl.id_gestao_local FROM gestao_local gl WHERE gl.id_superintendencia = :superintendenciaId) "
                    : "";
        String sql = """
                SELECT c.id_contrato AS id,
                       COALESCE(NULLIF(c.numero_contrato, ''), NULLIF(c.descricao, ''), CONCAT('Contrato ', c.id_contrato)) AS nome
                  FROM contrato c
                 WHERE COALESCE(c.ativo, TRUE) = TRUE
                   AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                   AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                """ + scope + " ORDER BY nome";
        return jdbc.query(sql, params, (rs, n) -> new RelatorioTerceirizadosDTO.Opcao(
                rs.getLong("id"), rs.getString("nome")
        ));
    }

    private RelatorioTerceirizadosDTO.PaginaEmpregados empregados(
            MapSqlParameterSource params,
            String filtro,
            int pagina,
            int tamanho,
            long total
    ) {
        long offset = (long) pagina * tamanho;
        String sql = """
                SELECT e.id_empregado AS id,
                       e.nome,
                       e.matricula,
                       e.cpf,
                       s.descricao AS status_descricao,
                       l.id_lotacao AS lotacao_id,
                       l.nome AS lotacao_nome,
                       cg.nome AS cargo_nome,
                       j.descricao AS jornada_descricao,
                       e.data_admissao
                  FROM empregado e
                  LEFT JOIN status_empregado s ON s.id_status = e.id_status
                  LEFT JOIN cargo cg ON cg.id_cargo = e.id_cargo
                  LEFT JOIN lotacao l ON l.id_lotacao = e.id_lotacao
                  LEFT JOIN jornada j ON j.id_jornada = e.id_jornada
                """ + filtro + " ORDER BY e.nome, e.id_empregado LIMIT " + tamanho + " OFFSET " + offset;

        List<EmpregadoBase> bases = jdbc.query(sql, params, (rs, n) -> new EmpregadoBase(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("matricula"),
                rs.getString("cpf"),
                rs.getString("status_descricao"),
                longOuNulo(rs.getObject("lotacao_id")),
                rs.getString("lotacao_nome"),
                rs.getString("cargo_nome"),
                rs.getString("jornada_descricao"),
                rs.getDate("data_admissao") == null ? null : rs.getDate("data_admissao").toLocalDate()
        ));

        Map<Long, List<RelatorioTerceirizadosDTO.ContratoResumo>> contratosPorEmpregado = contratosDaPagina(
                bases.stream().map(EmpregadoBase::id).toList(), params);
        List<RelatorioTerceirizadosDTO.EmpregadoResumo> content = bases.stream().map(base ->
                new RelatorioTerceirizadosDTO.EmpregadoResumo(
                        base.id(), base.nome(), base.matricula(), base.cpf(), base.statusDescricao(),
                        base.lotacaoId(), base.lotacaoNome(), base.cargoNome(), base.jornadaDescricao(),
                        base.dataAdmissao(), contratosPorEmpregado.getOrDefault(base.id(), List.of())
                )).toList();

        int totalPages = total == 0 ? 0 : (int) Math.ceil(total / (double) tamanho);
        return new RelatorioTerceirizadosDTO.PaginaEmpregados(
                content,
                total,
                totalPages,
                pagina,
                tamanho,
                pagina == 0,
                totalPages == 0 || pagina >= totalPages - 1
        );
    }

    private Map<Long, List<RelatorioTerceirizadosDTO.ContratoResumo>> contratosDaPagina(
            List<Long> ids,
            MapSqlParameterSource parametrosBase
    ) {
        if (ids.isEmpty()) return Map.of();
        MapSqlParameterSource params = new MapSqlParameterSource(parametrosBase.getValues()).addValue("ids", ids);
        String sql = """
                SELECT ec.id_empregado,
                       c.id_contrato,
                       c.numero_contrato,
                       c.descricao,
                       CASE WHEN c.id_gestao_local = e.id_gestao_local
                                  AND COALESCE(c.ativo, TRUE) = TRUE
                                  AND (c.data_inicio IS NULL OR c.data_inicio <= :referencia)
                                  AND (c.data_fim IS NULL OR c.data_fim >= :referencia)
                            THEN TRUE ELSE FALSE END AS vigente
                  FROM empregado_contrato ec
                  JOIN empregado e ON e.id_empregado = ec.id_empregado
                  JOIN contrato c ON c.id_contrato = ec.id_contrato
                 WHERE ec.id_empregado IN (:ids)
                 ORDER BY ec.id_empregado, c.numero_contrato, c.id_contrato
                """;
        Map<Long, List<RelatorioTerceirizadosDTO.ContratoResumo>> resultado = new HashMap<>();
        jdbc.query(sql, params, rs -> {
            resultado.computeIfAbsent(rs.getLong("id_empregado"), ignored -> new ArrayList<>())
                    .add(new RelatorioTerceirizadosDTO.ContratoResumo(
                            rs.getLong("id_contrato"),
                            rs.getString("numero_contrato"),
                            rs.getString("descricao"),
                            rs.getBoolean("vigente")
                    ));
        });
        resultado.replaceAll((id, lista) -> List.copyOf(lista));
        return resultado;
    }

    private String filtroEmpregados(
            Long superintendenciaId,
            Long gestaoLocalId,
            String busca,
            String status,
            Long contratoId,
            boolean semContrato,
            Long lotacaoId
    ) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        if (gestaoLocalId != null) where.append(" AND e.id_gestao_local = :gestaoLocalId ");
        else if (superintendenciaId != null) {
            where.append(" AND e.id_gestao_local IN (SELECT gl.id_gestao_local FROM gestao_local gl WHERE gl.id_superintendencia = :superintendenciaId) ");
        }
        if (status != null && !status.isBlank()) where.append(" AND s.descricao = :status ");
        if (lotacaoId != null) where.append(" AND e.id_lotacao = :lotacaoId ");
        if (contratoId != null) {
            where.append("""
                     AND EXISTS (
                         SELECT 1
                           FROM empregado_contrato ec_f
                           JOIN contrato c_f ON c_f.id_contrato = ec_f.id_contrato
                          WHERE ec_f.id_empregado = e.id_empregado
                            AND c_f.id_contrato = :contratoId
                            AND c_f.id_gestao_local = e.id_gestao_local
                            AND COALESCE(c_f.ativo, TRUE) = TRUE
                            AND (c_f.data_inicio IS NULL OR c_f.data_inicio <= :referencia)
                            AND (c_f.data_fim IS NULL OR c_f.data_fim >= :referencia)
                     )
                    """);
        } else if (semContrato) {
            where.append("""
                     AND NOT EXISTS (
                         SELECT 1
                           FROM empregado_contrato ec_f
                           JOIN contrato c_f ON c_f.id_contrato = ec_f.id_contrato
                          WHERE ec_f.id_empregado = e.id_empregado
                            AND c_f.id_gestao_local = e.id_gestao_local
                            AND COALESCE(c_f.ativo, TRUE) = TRUE
                            AND (c_f.data_inicio IS NULL OR c_f.data_inicio <= :referencia)
                            AND (c_f.data_fim IS NULL OR c_f.data_fim >= :referencia)
                     )
                    """);
        }
        if (busca != null && !busca.isBlank()) {
            where.append("""
                     AND (
                         LOWER(e.nome) LIKE :busca
                         OR LOWER(COALESCE(e.matricula, '')) LIKE :busca
                         OR LOWER(COALESCE(e.cpf, '')) LIKE :busca
                         OR LOWER(COALESCE(cg.nome, '')) LIKE :busca
                         OR LOWER(COALESCE(l.nome, '')) LIKE :busca
                         OR EXISTS (
                             SELECT 1
                               FROM empregado_contrato ec_b
                               JOIN contrato c_b ON c_b.id_contrato = ec_b.id_contrato
                              WHERE ec_b.id_empregado = e.id_empregado
                                AND c_b.id_gestao_local = e.id_gestao_local
                                AND COALESCE(c_b.ativo, TRUE) = TRUE
                                AND (c_b.data_inicio IS NULL OR c_b.data_inicio <= :referencia)
                                AND (c_b.data_fim IS NULL OR c_b.data_fim >= :referencia)
                                AND (LOWER(COALESCE(c_b.numero_contrato, '')) LIKE :busca
                                     OR LOWER(COALESCE(c_b.descricao, '')) LIKE :busca)
                         )
                     )
                    """);
        }
        return where.toString();
    }

    private long numero(Map<String, Object> row, String chave) {
        Object value = row.get(chave);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Long longOuNulo(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private record IndicadoresContratuais(long contratosVigentes, long efetivoPrevisto, long efetivoVinculado) {}

    private record EmpregadoBase(
            Long id,
            String nome,
            String matricula,
            String cpf,
            String statusDescricao,
            Long lotacaoId,
            String lotacaoNome,
            String cargoNome,
            String jornadaDescricao,
            LocalDate dataAdmissao
    ) {}
}
