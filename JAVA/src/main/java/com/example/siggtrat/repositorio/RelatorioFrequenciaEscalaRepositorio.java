package com.example.siggtrat.repositorio;

import com.example.siggtrat.utilitario.PrecisaoTemporal;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    /** Intervalos já reconstruídos pelo motor contratual, com limites diários inclusivos. */
    public record FaixaResponsabilidade(Long contratoId, Long gestaoLocalId, Long superintendenciaId,
                                         LocalDate inicio, LocalDate fim, String numeroContrato) {}

    public record EscopoConsulta(Long gestaoLocalId, Long superintendenciaId, boolean operacional,
                                  List<EscopoOperacional> pares,
                                  List<FaixaResponsabilidade> faixas, Instant conhecidoAte) {}

    /** A mesma relação seleciona a página e as faixas que formarão seus dias. */
    public record FonteAlocacao(Long empregadoId, Long historicoId, Long vinculoId, Long alocacaoId,
                                Long gestaoLocalId, Long superintendenciaId, LocalDate inicio,
                                LocalDate fim, String numeroContrato) {}

    private static final String IDENTIDADE_PESSOA = """
            (e.id_pessoa_canonica = p.id_pessoa OR (e.id_pessoa_canonica IS NULL
             AND REPLACE(REPLACE(REPLACE(e.cpf, '.', ''), '-', ''), ' ', '') = p.cpf))
            """;
    private static final String SEM_RAIZ_CANONICA = """
            NOT EXISTS (SELECT 1 FROM vinculo_contratual raiz
                JOIN pessoa p ON p.id_pessoa = raiz.id_pessoa WHERE
            """ + IDENTIDADE_PESSOA + ")";

    private final NamedParameterJdbcTemplate jdbc;

    public RelatorioFrequenciaEscalaRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Descobre identidades consultáveis; a autorização das linhas continua sendo temporal. */
    public List<Long> listarContratosCandidatos(Collection<Long> contratoIds, Long gestaoLocalId,
                                               Long superintendenciaId) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("gestaoLocalId", gestaoLocalId).addValue("superintendenciaId", superintendenciaId);
        String filtroIds = "";
        if (contratoIds != null && !contratoIds.isEmpty()) {
            p.addValue("contratoIds", contratoIds);
            filtroIds = " AND c.id_contrato IN (:contratoIds)";
        }
        return jdbc.queryForList("""
                SELECT c.id_contrato FROM contrato c
                LEFT JOIN gestao_local gl ON gl.id_gestao_local = c.id_gestao_local
                WHERE ((:gestaoLocalId IS NULL AND :superintendenciaId IS NULL)
                    OR c.id_gestao_local = :gestaoLocalId OR gl.id_superintendencia = :superintendenciaId
                    OR EXISTS (SELECT 1 FROM contrato_versao_negocial cv
                        JOIN gestao_local gh ON gh.id_gestao_local = cv.gestao_local_id
                        WHERE cv.id_contrato = c.id_contrato
                          AND (cv.gestao_local_id = :gestaoLocalId OR gh.id_superintendencia = :superintendenciaId))
                    OR EXISTS (SELECT 1 FROM historico_vinculo_contrato h
                        WHERE h.id_contrato = c.id_contrato
                          AND (h.gestao_local_snapshot_id = :gestaoLocalId
                            OR h.superintendencia_snapshot_id = :superintendenciaId)))
                """ + filtroIds + " ORDER BY c.id_contrato", p, Long.class);
    }

    public long contarColaboradores(
            Collection<Long> contratoIds,
            ZonedDateTime inicio,
            ZonedDateTime fimExclusivo,
            Long lotacaoId,
            Long jornadaId,
            EscopoConsulta escopo
    ) {
        if (contratoIds == null || contratoIds.isEmpty()) return 0L;
        Consulta consulta = montarConsulta(contratoIds, inicio, fimExclusivo, lotacaoId, jornadaId,
                escopo);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT pop.id_empregado) " + consulta.fromWhere(),
                consulta.parametros(), Long.class);
        return total == null ? 0L : total;
    }

    public List<Long> buscarIdsColaboradores(
            Collection<Long> contratoIds,
            ZonedDateTime inicio,
            ZonedDateTime fimExclusivo,
            Long lotacaoId,
            Long jornadaId,
            EscopoConsulta escopo,
            long offset,
            int limite
    ) {
        if (contratoIds == null || contratoIds.isEmpty() || limite <= 0) return List.of();
        Consulta consulta = montarConsulta(contratoIds, inicio, fimExclusivo, lotacaoId, jornadaId,
                escopo);
        MapSqlParameterSource parametros = new MapSqlParameterSource(consulta.parametros().getValues())
                .addValue("offset", Math.max(0L, offset))
                .addValue("limite", limite);
        String sql = """
                SELECT pop.id_empregado
                """ + consulta.fromWhere() + """
                 GROUP BY pop.id_empregado, pop.nome
                 ORDER BY pop.nome, pop.id_empregado
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
            EscopoConsulta escopo,
            long ultimoId,
            int limite
    ) {
        if (contratoIds == null || contratoIds.isEmpty() || limite <= 0) return List.of();
        Consulta consulta = montarConsulta(contratoIds, inicio, fimExclusivo, lotacaoId, jornadaId,
                escopo);
        MapSqlParameterSource parametros = new MapSqlParameterSource(consulta.parametros().getValues())
                .addValue("ultimoId", Math.max(0L, ultimoId))
                .addValue("limite", limite);
        String sql = """
                SELECT pop.id_empregado
                """ + consulta.fromWhere() + """
                   AND pop.id_empregado > :ultimoId
                 GROUP BY pop.id_empregado
                 ORDER BY pop.id_empregado
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
            EscopoConsulta escopo
    ) {
        Consulta legado = montarConsultaLegada(contratoIds, inicio, fimExclusivo, lotacaoId, jornadaId, escopo);
        MapSqlParameterSource parametros = legado.parametros();
        parametros.addValue("inicioDia", java.sql.Date.valueOf(inicio.toLocalDate()))
                .addValue("fimDia", java.sql.Date.valueOf(fimExclusivo.toLocalDate().minusDays(1)))
                // Instant é persistido em UTC pelo Hibernate. Timestamp sem
                // Calendar seria serializado no fuso da JVM (UTC-3 nos testes).
                .addValue("conhecidoAte", LocalDateTime.ofInstant(
                        PrecisaoTemporal.limiteInclusivoMicros(escopo.conhecidoAte()), ZoneOffset.UTC));
        List<String> faixas = new ArrayList<>();
        for (int i = 0; i < escopo.faixas().size(); i++) {
            FaixaResponsabilidade faixa = escopo.faixas().get(i);
            String prefixo = "can" + i;
            parametros.addValue(prefixo + "Contrato", faixa.contratoId())
                    .addValue(prefixo + "Gestao", faixa.gestaoLocalId())
                    .addValue(prefixo + "Se", faixa.superintendenciaId())
                    .addValue(prefixo + "Inicio", java.sql.Date.valueOf(faixa.inicio()))
                    .addValue(prefixo + "Fim", java.sql.Date.valueOf(faixa.fim()))
                    .addValue(prefixo + "Numero", faixa.numeroContrato());
            faixas.add("SELECT :" + prefixo + "Contrato contrato_id, :" + prefixo + "Gestao gestao_id, :"
                    + prefixo + "Se se_id, :" + prefixo + "Inicio inicio, :" + prefixo + "Fim fim, :"
                    + prefixo + "Numero numero");
        }
        String consultaLegada = """
                SELECT h.id_empregado, e.nome, h.id historico_id, NULL vinculo_id, NULL alocacao_id,
                       h.gestao_local_snapshot_id gestao_id, h.superintendencia_snapshot_id se_id,
                       NULL inicio_canonico, NULL fim_canonico, h.contrato_snapshot_numero numero
                """ + legado.fromWhere() + " AND " + SEM_RAIZ_CANONICA;
        if (faixas.isEmpty()) return new Consulta(" FROM (" + consultaLegada + ") pop WHERE 1 = 1 ", parametros);

        String inicioComum = "GREATEST(v.data_inicio, a.data_inicio, faixa.inicio, :inicioDia)";
        String fimComum = "LEAST(COALESCE(v.data_fim, :fimDia), COALESCE(a.data_fim, :fimDia), faixa.fim, :fimDia)";
        StringBuilder canonico = new StringBuilder("""
                SELECT e.id_empregado, e.nome, NULL historico_id, v.id_vinculo_contratual vinculo_id,
                       a.id_alocacao_vinculo alocacao_id,
                       COALESCE(faixa.gestao_id, l.id_gestao_local) gestao_id,
                       COALESCE(faixa.se_id, gl.id_superintendencia) se_id,
                """).append(inicioComum).append(" inicio_canonico, ").append(fimComum).append(" fim_canonico, faixa.numero numero ")
                .append("""
                  FROM vinculo_contratual v
                  JOIN pessoa p ON p.id_pessoa = v.id_pessoa
                  JOIN empregado e ON
                """).append(IDENTIDADE_PESSOA).append("""
                  JOIN alocacao_vinculo a ON a.id_vinculo_contratual = v.id_vinculo_contratual
                  JOIN lotacao l ON l.id_lotacao = a.id_lotacao
                  JOIN jornada j ON j.id_jornada = a.id_jornada
                  JOIN cargo cg ON cg.id_cargo = a.id_cargo
                  JOIN gestao_local gl ON gl.id_gestao_local = l.id_gestao_local
                  JOIN (
                """).append(String.join(" UNION ALL ", faixas)).append("""
                  ) faixa ON faixa.contrato_id = v.id_contrato
                 WHERE v.id_contrato IN (:contratoIds)
                   AND v.estado_negocial = 'REGISTRADO' AND a.estado_negocial = 'REGISTRADA'
                   AND v.criado_em <= :conhecidoAte AND a.criado_em <= :conhecidoAte
                   AND l.id_gestao_local = COALESCE(faixa.gestao_id, l.id_gestao_local)
                   AND j.id_gestao_local = COALESCE(faixa.gestao_id, l.id_gestao_local)
                   AND cg.id_gestao_local = COALESCE(faixa.gestao_id, l.id_gestao_local)
                   AND
                """).append(inicioComum).append(" <= ").append(fimComum);
        if (escopo.gestaoLocalId() != null)
            canonico.append(" AND COALESCE(faixa.gestao_id, l.id_gestao_local) = :gestaoLocalId ");
        if (escopo.superintendenciaId() != null)
            canonico.append(" AND COALESCE(faixa.se_id, gl.id_superintendencia) = :superintendenciaId ");
        if (lotacaoId != null) canonico.append(" AND a.id_lotacao = :lotacaoId ");
        if (jornadaId != null) canonico.append(" AND a.id_jornada = :jornadaId ");
        if (escopo.operacional()) {
            List<String> pares = new ArrayList<>();
            int i = 0;
            for (EscopoOperacional par : escopo.pares()) {
                if (par == null) continue;
                List<String> partes = new ArrayList<>();
                if (par.lotacaoId() != null) {
                    parametros.addValue("canLotacao" + i, par.lotacaoId());
                    partes.add("a.id_lotacao = :canLotacao" + i);
                }
                if (par.jornadaId() != null) {
                    parametros.addValue("canJornada" + i, par.jornadaId());
                    partes.add("a.id_jornada = :canJornada" + i);
                }
                pares.add(partes.isEmpty() ? "1 = 1" : "(" + String.join(" AND ", partes) + ")");
                i++;
            }
            canonico.append(pares.isEmpty() ? " AND 1 = 0 " : " AND (" + String.join(" OR ", pares) + ") ");
        }
        return new Consulta(" FROM (" + consultaLegada + " UNION ALL " + canonico + ") pop WHERE 1 = 1 ", parametros);
    }

    public List<FonteAlocacao> buscarFontesDoLote(Collection<Long> empregadoIds, Collection<Long> contratoIds,
                                                 ZonedDateTime inicio, ZonedDateTime fimExclusivo,
                                                 Long lotacaoId, Long jornadaId, EscopoConsulta escopo) {
        if (empregadoIds == null || empregadoIds.isEmpty() || contratoIds == null || contratoIds.isEmpty()) return List.of();
        Consulta consulta = montarConsulta(contratoIds, inicio, fimExclusivo, lotacaoId, jornadaId, escopo);
        consulta.parametros().addValue("empregadoIds", empregadoIds);
        return jdbc.query("SELECT DISTINCT pop.* " + consulta.fromWhere()
                        + " AND pop.id_empregado IN (:empregadoIds)", consulta.parametros(), (rs, linha) -> {
            java.sql.Date de = rs.getDate("inicio_canonico"), ate = rs.getDate("fim_canonico");
            return new FonteAlocacao(rs.getLong("id_empregado"), rs.getObject("historico_id", Long.class),
                    rs.getObject("vinculo_id", Long.class), rs.getObject("alocacao_id", Long.class),
                    rs.getObject("gestao_id", Long.class), rs.getObject("se_id", Long.class),
                    de == null ? null : de.toLocalDate(), ate == null ? null : ate.toLocalDate(), rs.getString("numero"));
        });
    }

    private Consulta montarConsultaLegada(
            Collection<Long> contratoIds,
            ZonedDateTime inicio,
            ZonedDateTime fimExclusivo,
            Long lotacaoId,
            Long jornadaId,
            EscopoConsulta escopo
    ) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("contratoIds", contratoIds)
                .addValue("inicio", java.sql.Timestamp.from(inicio.toInstant()))
                .addValue("fimExclusivo", java.sql.Timestamp.from(fimExclusivo.toInstant()))
                .addValue("lotacaoId", lotacaoId)
                .addValue("jornadaId", jornadaId)
                .addValue("gestaoLocalId", escopo.gestaoLocalId())
                .addValue("superintendenciaId", escopo.superintendenciaId());

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

        if (escopo.gestaoLocalId() != null)
            sql.append(" AND h.gestao_local_snapshot_id = :gestaoLocalId ");
        if (escopo.superintendenciaId() != null)
            sql.append(" AND h.superintendencia_snapshot_id = :superintendenciaId ");

        // A restrição antecede COUNT, ORDER BY e LIMIT. Assim a paginação não
        // revela nem conta pessoas de um período pertencente a outra gestão.
        List<String> faixas = new ArrayList<>();
        int indiceFaixa = 0;
        for (FaixaResponsabilidade faixa : escopo.faixas()) {
            String prefixo = "faixa" + indiceFaixa++;
            parametros.addValue(prefixo + "Contrato", faixa.contratoId())
                    .addValue(prefixo + "Inicio", java.sql.Timestamp.from(
                            faixa.inicio().atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant()))
                    .addValue(prefixo + "Fim", java.sql.Timestamp.from(
                            faixa.fim().plusDays(1).atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant()));
            String condicao = "h.id_contrato = :" + prefixo + "Contrato"
                    + " AND h.data_inicio < :" + prefixo + "Fim"
                    + " AND (h.data_fim IS NULL OR h.data_fim >= :" + prefixo + "Inicio)";
            if (faixa.gestaoLocalId() != null) {
                parametros.addValue(prefixo + "Gestao", faixa.gestaoLocalId());
                condicao += " AND h.gestao_local_snapshot_id = :" + prefixo + "Gestao";
            }
            if (faixa.superintendenciaId() != null) {
                parametros.addValue(prefixo + "Se", faixa.superintendenciaId());
                condicao += " AND h.superintendencia_snapshot_id = :" + prefixo + "Se";
            }
            faixas.add("(" + condicao + ")");
        }
        sql.append(faixas.isEmpty() ? " AND 1 = 0 " : " AND (" + String.join(" OR ", faixas) + ") ");

        if (escopo.operacional()) {
            List<EscopoOperacional> escopos = escopo.pares() == null ? List.of() : escopo.pares();
            if (escopos.isEmpty()) {
                sql.append(" AND 1 = 0 ");
            } else {
                List<String> condicoes = new ArrayList<>();
                int indice = 0;
                for (EscopoOperacional par : escopos) {
                    if (par == null) continue;
                    List<String> partes = new ArrayList<>();
                    if (par.lotacaoId() != null) {
                        String nome = "escopoLotacao" + indice;
                        parametros.addValue(nome, par.lotacaoId());
                        partes.add("h.id_lotacao = :" + nome);
                    }
                    if (par.jornadaId() != null) {
                        String nome = "escopoJornada" + indice;
                        parametros.addValue(nome, par.jornadaId());
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
