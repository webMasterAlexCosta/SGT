package com.example.siggtrat.repositorio;

import com.example.siggtrat.dto.PainelEfetivoDTO;
import com.example.siggtrat.dto.PosicaoTemporalContratoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico.ComposicaoProjetada;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Read model do efetivo: projeções canônicas de Vínculo/Alocação e composição
 * reconstruída pelos mesmos motores usados nos comandos negociais.
 *
 * Contratos são lidos por cursor em lotes de 100. Pessoas e frequências não são
 * materializadas como entidades: suas contagens são agregadas no MySQL.
 */
@Repository
@Transactional(readOnly = true)
public class PainelEfetivoRepositorio {
    private static final String PRESENTES = "'PRESENCA','PRESENCA_PARCIAL','ATRASO','TR'";
    private static final String AUSENTES = "'FALTA','ATESTADO','SUSPENSAO'";
    private final NamedParameterJdbcTemplate jdbc;
    private final ContratoRepositorio contratos;
    private final MotorTemporalContratoServico motor;
    private final ComposicaoContratualTemporalServico composicoes;
    @PersistenceContext private EntityManager entityManager;

    public PainelEfetivoRepositorio(NamedParameterJdbcTemplate jdbc, ContratoRepositorio contratos,
                                   MotorTemporalContratoServico motor, ComposicaoContratualTemporalServico composicoes) {
        this.jdbc = jdbc;
        this.contratos = contratos;
        this.motor = motor;
        this.composicoes = composicoes;
    }

    public List<PainelEfetivoDTO.PontoDiario> obterSerie(LocalDate inicio, LocalDate fim, Long contratoId,
            Long lotacaoId, Long jornadaId, Long cargoId, Long gestaoLocalId, Long superintendenciaId) {
        Filtro filtro = new Filtro(contratoId, lotacaoId, jornadaId, cargoId, gestaoLocalId, superintendenciaId);
        SortedMap<LocalDate, long[]> totais = new TreeMap<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) totais.put(d, new long[5]);
        percorrer(filtro, inicio, fim, (contrato, segmentos) -> {
            Map<LocalDate, long[]> contagens = contagensDiarias(contrato.getId(), inicio, fim, filtro);
            for (Segmento segmento : segmentos) {
                long previsto = segmento.composicoes.stream().mapToLong(ComposicaoProjetada::efetivoPrevisto).sum();
                for (LocalDate d = segmento.inicio; !d.isAfter(segmento.fim); d = d.plusDays(1)) {
                    long[] total = totais.get(d);
                    total[0] += previsto;
                    long[] contagem = contagens.get(d);
                    if (contagem != null) for (int i = 1; i < 5; i++) total[i] += contagem[i - 1];
                }
            }
        });
        return totais.entrySet().stream().map(e -> ponto(e.getKey(), e.getValue())).toList();
    }

    public List<PainelEfetivoDTO.DetalheContrato> obterDetalhamentoContratos(LocalDate data, Long contratoId,
            Long lotacaoId, Long jornadaId, Long cargoId, Long gestaoLocalId, Long superintendenciaId) {
        Filtro filtro = new Filtro(contratoId, lotacaoId, jornadaId, cargoId, gestaoLocalId, superintendenciaId);
        List<PainelEfetivoDTO.DetalheContrato> resultado = new ArrayList<>();
        percorrer(filtro, data, data, (contrato, segmentos) -> {
            if (segmentos.isEmpty()) return;
            Segmento s = segmentos.get(0);
            long previsto = s.composicoes.stream().mapToLong(ComposicaoProjetada::efetivoPrevisto).sum();
            long[] n = contagensDiarias(contrato.getId(), data, data, filtro).getOrDefault(data, new long[4]);
            if (s.composicoes.isEmpty() && Arrays.stream(n).sum() == 0 && filtro.temDimensao()) return;
            resultado.add(new PainelEfetivoDTO.DetalheContrato(contrato.getId(), s.nome, previsto, n[0], n[1], n[2], n[3],
                    Math.max(0, n[0] - n[1]), Math.max(0, previsto - n[0]), Math.max(0, n[0] - previsto),
                    percentual(n[1], n[0]), percentual(n[2], previsto)));
        });
        resultado.sort(Comparator.comparing(PainelEfetivoDTO.DetalheContrato::nome, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(PainelEfetivoDTO.DetalheContrato::id));
        return List.copyOf(resultado);
    }

    public List<PainelEfetivoDTO.LinhaDimensionada> obterQuadroDimensionado(LocalDate data, Long contratoId,
            Long lotacaoId, Long jornadaId, Long cargoId, Long gestaoLocalId, Long superintendenciaId) {
        Filtro filtro = new Filtro(contratoId, lotacaoId, jornadaId, cargoId, gestaoLocalId, superintendenciaId);
        List<PainelEfetivoDTO.LinhaDimensionada> resultado = new ArrayList<>();
        percorrer(filtro, data, data, (contrato, segmentos) -> {
            if (segmentos.isEmpty()) return;
            Segmento s = segmentos.get(0);
            Map<Chave, Linha> linhas = new LinkedHashMap<>();
            for (ComposicaoProjetada c : s.composicoes) {
                Chave chave = new Chave(c.lotacao().getId(), c.jornada().getId(), c.cargo().getId());
                Linha linha = linhas.computeIfAbsent(chave, k -> new Linha(k, c.lotacaoNome(), c.jornadaDescricao(), c.cargoNome()));
                linha.previsto += c.efetivoPrevisto();
            }
            MapSqlParameterSource p = parametros(contrato.getId(), data, data, filtro);
            String sql = """
                SELECT a.id_lotacao, a.id_jornada, a.id_cargo,
                       MIN(l.nome) lotacao, MIN(j.descricao) turno, MIN(cg.nome) cargo,
                       COUNT(DISTINCT v.id_vinculo_contratual) alocados
                  FROM alocacao_vinculo a
                  JOIN vinculo_contratual v ON v.id_vinculo_contratual = a.id_vinculo_contratual
                  JOIN lotacao l ON l.id_lotacao = a.id_lotacao
                  JOIN jornada j ON j.id_jornada = a.id_jornada
                  JOIN cargo cg ON cg.id_cargo = a.id_cargo
                 WHERE v.id_contrato = :contratoId
                   AND v.estado_negocial = 'REGISTRADO' AND a.estado_negocial = 'REGISTRADA'
                   AND v.data_inicio <= :inicio AND (v.data_fim IS NULL OR v.data_fim >= :inicio)
                   AND a.data_inicio <= :inicio AND (a.data_fim IS NULL OR a.data_fim >= :inicio)
                """ + filtroDimensoes("a", false) + " GROUP BY a.id_lotacao, a.id_jornada, a.id_cargo";
            jdbc.query(sql, p, rs -> {
                Chave chave = new Chave(rs.getLong("id_lotacao"), rs.getLong("id_jornada"), rs.getLong("id_cargo"));
                Linha l = linhas.get(chave);
                if (l == null) {
                    l = new Linha(chave, rs.getString("lotacao"), rs.getString("turno"), rs.getString("cargo"));
                    linhas.put(chave, l);
                }
                l.alocados = rs.getLong("alocados");
            });
            String frequencias = """
                SELECT f.lotacao_snapshot_id, f.jornada_snapshot_id, f.cargo_snapshot_id,
                       MIN(f.lotacao_snapshot_nome) lotacao, MIN(f.jornada_snapshot_descricao) turno,
                       MIN(f.cargo_snapshot_nome) cargo, COUNT(*) registros,
                       SUM(CASE WHEN f.status IN (%s) THEN 1 ELSE 0 END) presentes,
                       SUM(CASE WHEN f.status IN (%s) THEN 1 ELSE 0 END) ausentes
                  FROM frequencia f
                 WHERE f.contrato_snapshot_id = :contratoId AND f.data_referencia = :inicio
                   AND f.validade_operacional = 'VALIDA'
                """.formatted(PRESENTES, AUSENTES) + filtroDimensoes("f", true) + escopoFrequencia(filtro)
                    + " GROUP BY f.lotacao_snapshot_id, f.jornada_snapshot_id, f.cargo_snapshot_id";
            jdbc.query(frequencias, p, rs -> {
                Chave chave = new Chave(rs.getObject("lotacao_snapshot_id", Long.class),
                        rs.getObject("jornada_snapshot_id", Long.class), rs.getObject("cargo_snapshot_id", Long.class));
                Linha l = linhas.get(chave);
                if (l == null) {
                    l = new Linha(chave, rs.getString("lotacao"), rs.getString("turno"), rs.getString("cargo"));
                    linhas.put(chave, l);
                }
                l.registros = rs.getLong("registros"); l.presentes = rs.getLong("presentes"); l.ausentes = rs.getLong("ausentes");
            });
            for (Linha l : linhas.values()) resultado.add(new PainelEfetivoDTO.LinhaDimensionada(
                    contrato.getId(), s.nome, l.chave.lotacao, l.lotacao, l.chave.jornada, l.turno, l.chave.cargo, l.cargo,
                    l.alocados, l.registros, l.presentes, l.ausentes, Math.max(0, l.alocados - l.registros),
                    percentual(l.registros, l.alocados), percentual(l.presentes, l.previsto),
                    l.previsto, Math.max(0, l.previsto - l.alocados), Math.max(0, l.alocados - l.previsto)));
        });
        resultado.sort(Comparator.comparing(PainelEfetivoDTO.LinhaDimensionada::contrato, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(PainelEfetivoDTO.LinhaDimensionada::lotacao, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(PainelEfetivoDTO.LinhaDimensionada::turno, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(PainelEfetivoDTO.LinhaDimensionada::cargo, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return List.copyOf(resultado);
    }

    public List<PainelEfetivoDTO.Opcao> listarContratos(Long gestao, Long se, Long lotacao, Long jornada, Long cargo) {
        return listarContratos(gestao, se, lotacao, jornada, cargo, RelogioSistema.hoje(), RelogioSistema.hoje());
    }
    public List<PainelEfetivoDTO.Opcao> listarContratos(Long gestao, Long se, Long lotacao, Long jornada, Long cargo,
                                                     LocalDate inicio, LocalDate fim) {
        return opcoes(new Filtro(null, lotacao, jornada, cargo, gestao, se), "CONTRATO", inicio, fim);
    }
    public List<PainelEfetivoDTO.Opcao> listarLotacoes(Long gestao, Long se, Long contrato, Long jornada, Long cargo) {
        return listarLotacoes(gestao, se, contrato, jornada, cargo, RelogioSistema.hoje(), RelogioSistema.hoje());
    }
    public List<PainelEfetivoDTO.Opcao> listarLotacoes(Long gestao, Long se, Long contrato, Long jornada, Long cargo,
                                                    LocalDate inicio, LocalDate fim) {
        return opcoes(new Filtro(contrato, null, jornada, cargo, gestao, se), "LOTACAO", inicio, fim);
    }
    public List<PainelEfetivoDTO.Opcao> listarTurnos(Long gestao, Long se, Long contrato, Long lotacao, Long cargo) {
        return listarTurnos(gestao, se, contrato, lotacao, cargo, RelogioSistema.hoje(), RelogioSistema.hoje());
    }
    public List<PainelEfetivoDTO.Opcao> listarTurnos(Long gestao, Long se, Long contrato, Long lotacao, Long cargo,
                                                   LocalDate inicio, LocalDate fim) {
        return opcoes(new Filtro(contrato, lotacao, null, cargo, gestao, se), "TURNO", inicio, fim);
    }
    public List<PainelEfetivoDTO.Opcao> listarCargos(Long gestao, Long se, Long contrato, Long lotacao, Long jornada) {
        return listarCargos(gestao, se, contrato, lotacao, jornada, RelogioSistema.hoje(), RelogioSistema.hoje());
    }
    public List<PainelEfetivoDTO.Opcao> listarCargos(Long gestao, Long se, Long contrato, Long lotacao, Long jornada,
                                                   LocalDate inicio, LocalDate fim) {
        return opcoes(new Filtro(contrato, lotacao, jornada, null, gestao, se), "CARGO", inicio, fim);
    }

    private List<PainelEfetivoDTO.Opcao> opcoes(Filtro filtro, String tipo, LocalDate inicio, LocalDate fim) {
        Map<Long, String> resultado = new HashMap<>();
        percorrer(filtro, inicio, fim, (contrato, segmentos) -> {
            for (Segmento s : segmentos) {
                if ("CONTRATO".equals(tipo) && (!filtro.temDimensao() || !s.composicoes.isEmpty()))
                    resultado.put(contrato.getId(), s.nome);
                for (ComposicaoProjetada c : s.composicoes) switch (tipo) {
                    case "LOTACAO" -> resultado.put(c.lotacao().getId(), c.lotacaoNome());
                    case "TURNO" -> resultado.put(c.jornada().getId(), c.jornadaDescricao());
                    case "CARGO" -> resultado.put(c.cargo().getId(), c.cargoNome());
                    default -> { }
                }
            }
        });
        return resultado.entrySet().stream().map(e -> new PainelEfetivoDTO.Opcao(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PainelEfetivoDTO.Opcao::nome, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(PainelEfetivoDTO.Opcao::id)).toList();
    }

    private void percorrer(Filtro filtro, LocalDate inicio, LocalDate fim,
                          BiConsumer<Contrato, List<Segmento>> consumidor) {
        Instant conhecimento = Instant.now();
        Map<Long, Long> superintendencias = new HashMap<>();
        long cursor = 0;
        while (true) {
            MapSqlParameterSource p = parametros(filtro.contrato, inicio, fim, filtro).addValue("cursor", cursor);
            List<Long> ids = jdbc.queryForList("""
                SELECT c.id_contrato FROM contrato c
                  JOIN gestao_local gl ON gl.id_gestao_local = c.id_gestao_local
                 WHERE c.id_contrato > :cursor AND (CAST(:contratoId AS BIGINT) IS NULL OR c.id_contrato = :contratoId)
                   AND ((CAST(:gestaoLocalId AS BIGINT) IS NULL AND CAST(:superintendenciaId AS BIGINT) IS NULL)
                     OR c.id_gestao_local = :gestaoLocalId OR gl.id_superintendencia = :superintendenciaId
                     OR EXISTS (SELECT 1 FROM contrato_versao_negocial cv
                          JOIN gestao_local gh ON gh.id_gestao_local = cv.gestao_local_id
                         WHERE cv.id_contrato = c.id_contrato
                           AND (cv.gestao_local_id = :gestaoLocalId OR gh.id_superintendencia = :superintendenciaId)))
                 ORDER BY c.id_contrato LIMIT 100
                """, p, Long.class);
            if (ids.isEmpty()) break;
            for (Long id : ids) contratos.findById(id).ifPresent(c ->
                    consumidor.accept(c, segmentos(c, inicio, fim, conhecimento, filtro, superintendencias)));
            cursor = ids.get(ids.size() - 1);
            // A limpeza é exclusiva de consultas read-only; jamais descarta escritas
            // pendentes de um comando transacional que reutilize este read model.
            if (entityManager != null && TransactionSynchronizationManager.isCurrentTransactionReadOnly()) entityManager.clear();
            if (ids.size() < 100) break;
        }
    }

    private List<Segmento> segmentos(Contrato c, LocalDate inicio, LocalDate fim, Instant conhecimento,
                                    Filtro filtro, Map<Long, Long> superintendencias) {
        SortedSet<LocalDate> limites = new TreeSet<>();
        limites.add(inicio); limites.add(fim.plusDays(1));
        limites.addAll(composicoes.datasMudancaCapacidade(c, fim));
        for (var evento : motor.eventosValidosOrdenados(c.getId(), fim, conhecimento)) {
            limites.add(evento.getDataEfetiva());
            // Encerramento inclui seu último dia; o estado muda no dia seguinte.
            limites.add(evento.getDataEfetiva().plusDays(1));
        }
        for (var versao : motor.listarVersoes(c.getId())) {
            if (versao.dataEfetiva() != null) limites.add(versao.dataEfetiva());
            if (versao.dataInicio() != null) limites.add(versao.dataInicio());
            if (versao.dataFim() != null) limites.add(versao.dataFim().plusDays(1));
        }
        if (c.getDataInicio() != null) limites.add(c.getDataInicio());
        if (c.getDataFim() != null) limites.add(c.getDataFim().plusDays(1));
        limites.removeIf(d -> d.isBefore(inicio) || d.isAfter(fim.plusDays(1)));
        List<LocalDate> datas = new ArrayList<>(limites);
        List<Segmento> resultado = new ArrayList<>();
        for (int i = 0; i < datas.size() - 1; i++) {
            LocalDate d = datas.get(i);
            PosicaoTemporalContratoDTO pos = motor.reconstruirPosicao(c, d, conhecimento);
            if (pos.estadoNegocial() != EstadoNegocialContrato.PUBLICADO || pos.situacaoTemporal() != SituacaoTemporalContrato.VIGENTE) continue;
            Long gestao = pos.versaoContratual() != null && pos.versaoContratual().gestaoLocalId() != null
                    ? pos.versaoContratual().gestaoLocalId() : c.getGestaoLocal().getId();
            if (filtro.gestao != null && !Objects.equals(filtro.gestao, gestao)) continue;
            if (filtro.se != null) {
                Long se = superintendencias.computeIfAbsent(gestao, id -> jdbc.queryForObject(
                        "SELECT id_superintendencia FROM gestao_local WHERE id_gestao_local = :id",
                        new MapSqlParameterSource("id", id), Long.class));
                if (!Objects.equals(filtro.se, se)) continue;
            }
            List<ComposicaoProjetada> selecionadas = composicoes.projetarEntidades(c, d, conhecimento).stream()
                    .filter(x -> filtro.lotacao == null || Objects.equals(filtro.lotacao, x.lotacao().getId()))
                    .filter(x -> filtro.jornada == null || Objects.equals(filtro.jornada, x.jornada().getId()))
                    .filter(x -> filtro.cargo == null || Objects.equals(filtro.cargo, x.cargo().getId())).toList();
            String nome = pos.versaoContratual() != null ? pos.versaoContratual().numeroContrato() : c.getNumeroContrato();
            resultado.add(new Segmento(d, datas.get(i + 1).minusDays(1), nome, selecionadas));
        }
        return resultado;
    }

    private Map<LocalDate, long[]> contagensDiarias(Long contrato, LocalDate inicio, LocalDate fim, Filtro filtro) {
        Map<LocalDate, long[]> resultado = new HashMap<>();
        MapSqlParameterSource p = parametros(contrato, inicio, fim, filtro);
        String sql = """
            WITH RECURSIVE datas AS (
                SELECT CAST(:inicio AS DATE) data UNION ALL
                SELECT data + 1 FROM datas WHERE data < CAST(:fim AS DATE)
            )
            SELECT d.data, COUNT(DISTINCT v.id_vinculo_contratual) alocados
              FROM datas d JOIN vinculo_contratual v ON v.id_contrato = :contratoId
                AND v.estado_negocial = 'REGISTRADO' AND v.data_inicio <= d.data
                AND (v.data_fim IS NULL OR v.data_fim >= d.data)
              JOIN alocacao_vinculo a ON a.id_vinculo_contratual = v.id_vinculo_contratual
                AND a.estado_negocial = 'REGISTRADA' AND a.data_inicio <= d.data
                AND (a.data_fim IS NULL OR a.data_fim >= d.data)
             WHERE 1=1
            """ + filtroDimensoes("a", false) + " GROUP BY d.data";
        jdbc.query(sql, p, rs -> {
            long[] n = resultado.computeIfAbsent(rs.getDate("data").toLocalDate(), d -> new long[4]);
            n[0] = rs.getLong("alocados");
        });
        String frequencias = """
            SELECT f.data_referencia data, COUNT(*) registros,
                   SUM(CASE WHEN f.status IN (%s) THEN 1 ELSE 0 END) presentes,
                   SUM(CASE WHEN f.status IN (%s) THEN 1 ELSE 0 END) ausentes
              FROM frequencia f
             WHERE f.contrato_snapshot_id = :contratoId AND f.data_referencia BETWEEN :inicio AND :fim
               AND f.validade_operacional = 'VALIDA'
            """.formatted(PRESENTES, AUSENTES) + filtroDimensoes("f", true) + escopoFrequencia(filtro) + " GROUP BY f.data_referencia";
        jdbc.query(frequencias, p, rs -> {
            long[] n = resultado.computeIfAbsent(rs.getDate("data").toLocalDate(), d -> new long[4]);
            n[1] = rs.getLong("registros"); n[2] = rs.getLong("presentes"); n[3] = rs.getLong("ausentes");
        });
        return resultado;
    }

    private String filtroDimensoes(String alias, boolean snapshot) {
        String l = snapshot ? "lotacao_snapshot_id" : "id_lotacao";
        String j = snapshot ? "jornada_snapshot_id" : "id_jornada";
        String c = snapshot ? "cargo_snapshot_id" : "id_cargo";
        return " AND (CAST(:lotacaoId AS BIGINT) IS NULL OR " + alias + "." + l + " = :lotacaoId)"
                + " AND (CAST(:jornadaId AS BIGINT) IS NULL OR " + alias + "." + j + " = :jornadaId)"
                + " AND (CAST(:cargoId AS BIGINT) IS NULL OR " + alias + "." + c + " = :cargoId)";
    }
    private String escopoFrequencia(Filtro f) {
        return (f.gestao != null ? " AND f.gestao_local_snapshot_id = :gestaoLocalId" : "")
                + (f.se != null ? " AND f.superintendencia_snapshot_id = :superintendenciaId" : "");
    }
    private MapSqlParameterSource parametros(Long contrato, LocalDate inicio, LocalDate fim, Filtro f) {
        return new MapSqlParameterSource().addValue("contratoId", contrato)
                .addValue("inicio", Date.valueOf(inicio)).addValue("fim", Date.valueOf(fim))
                .addValue("lotacaoId", f.lotacao).addValue("jornadaId", f.jornada).addValue("cargoId", f.cargo)
                .addValue("gestaoLocalId", f.gestao).addValue("superintendenciaId", f.se);
    }
    private PainelEfetivoDTO.PontoDiario ponto(LocalDate data, long[] n) {
        return new PainelEfetivoDTO.PontoDiario(data, n[0], n[1], n[2], n[3], n[4], Math.max(0, n[2]-n[3]-n[4]),
                Math.max(0,n[1]-n[2]), Math.max(0,n[0]-n[1]), Math.max(0,n[1]-n[0]), percentual(n[2],n[1]), percentual(n[3],n[0]));
    }
    private static double percentual(long valor, long total) { return total <= 0 ? 0 : Math.round(valor * 10000D / total) / 100D; }
    private record Filtro(Long contrato, Long lotacao, Long jornada, Long cargo, Long gestao, Long se) {
        boolean temDimensao() { return lotacao != null || jornada != null || cargo != null; }
    }
    private record Segmento(LocalDate inicio, LocalDate fim, String nome, List<ComposicaoProjetada> composicoes) {}
    private record Chave(Long lotacao, Long jornada, Long cargo) {}
    private static final class Linha {
        final Chave chave; final String lotacao; final String turno; final String cargo;
        long previsto, alocados, registros, presentes, ausentes;
        Linha(Chave chave, String lotacao, String turno, String cargo) {
            this.chave=chave; this.lotacao=lotacao; this.turno=turno; this.cargo=cargo;
        }
    }
}

