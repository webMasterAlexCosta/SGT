package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Types;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** External synthetic supplement; it is not copied into either candidate. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class B230Suporte extends ContratosHttpBase {
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected Environment ambiente;

    @FunctionalInterface protected interface Acao<T> { T executar() throws Exception; }
    protected record Destino(long cargo, long lotacao, long jornada) {}

    @BeforeEach
    void isolamentoB230() {
        assertThat(jdbc.queryForObject("select version()", String.class)).isEqualTo("8.0.46");
        assertThat(jdbc.queryForObject("select current_user()", String.class)).isEqualTo("h02_crit_b2@127.0.0.1");
        assertThat(jdbc.queryForObject("select database()", String.class)).isEqualTo("sgt_audit_h02_crit_b2");
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    /** Actual verification starts a new transaction and clears the JPA context. */
    protected <T> T nova(Acao<T> action) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status -> {
            em.clear();
            try { return action.executar(); }
            catch (RuntimeException e) { throw e; }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    /**
     * Complete values of business tables, read after commit. Session heartbeats
     * and access/security audit may legitimately change for a rejected request;
     * only those two tables are outside this business rollback oracle.
     */
    protected Map<String,List<String>> negocio() {
        return nova(() -> {
            Map<String,List<String>> result = new TreeMap<>();
            List<String> tables = jdbc.queryForList("select table_name from information_schema.tables "
                    + "where table_schema=database() and table_type='BASE TABLE' order by table_name", String.class);
            assertThat(tables).contains("pessoa", "vinculo_contratual", "alocacao_vinculo", "contrato", "frequencia");
            for (String table : tables) {
                if (Set.of("token_ativo", "registro_auditoria").contains(table)) continue;
                assertThat(table).matches("[a-zA-Z0-9_]+");
                List<String> rows = jdbc.query("select * from `" + table + "`", (ResultSetExtractor<List<String>>) rs -> {
                    List<String> values = new ArrayList<>();
                    var metadata = rs.getMetaData();
                    while (rs.next()) {
                        Map<String,Object> row = new LinkedHashMap<>();
                        for (int column = 1; column <= metadata.getColumnCount(); column++) {
                            Object value;
                            int type = metadata.getColumnType(column);
                            if (Set.of(Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB).contains(type)) {
                                byte[] bytes = rs.getBytes(column);
                                value = bytes == null ? null : HexFormat.of().formatHex(bytes);
                            } else value = rs.getString(column);
                            row.put(metadata.getColumnName(column), value);
                        }
                        try { values.add(mapper.writeValueAsString(row)); }
                        catch (Exception e) { throw new java.sql.SQLException(e); }
                    }
                    Collections.sort(values); return values;
                });
                result.put(table, rows);
            }
            return result;
        });
    }

    protected void semMutacao(Map<String,List<String>> before, String predicate) {
        assertThat(negocio()).as(predicate + " — banco relido em outra transação").isEqualTo(before);
    }

    protected Map<String,List<String>> linhaCanonica() {
        var all = negocio(); Map<String,List<String>> result = new TreeMap<>();
        for (String table : List.of("pessoa", "vinculo_contratual", "alocacao_vinculo",
                "vinculo_contratual_evento", "alocacao_vinculo_evento")) {
            assertThat(all).containsKey(table); result.put(table, all.get(table));
        }
        return result;
    }

    protected Cenario cenario() { return nova(this::criarCenario); }
    protected NovoContrato publicadoConfirmado(Cenario c, int total) {
        return nova(() -> publicado(c, total, null, false));
    }
    protected NovoContrato publicadoEm(Cenario c, int total, LocalDate inicio, LocalDate fim,
                                        Destino destino, Integer quantidadeDestino) throws Exception {
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "B230-PUB-" + UUID.randomUUID().toString().substring(0,8),
                total, inicio, fim), 201, "Rascunho sintético com limites explícitos").path("id").asLong();
        int second = quantidadeDestino == null ? 0 : quantidadeDestino;
        long first = enviar(c.localA, base(id) + "/composicoes", composicao(c,c.cargoA.getId(),total-second),
                201, "Composição original do controle").path("id").asLong();
        Long extra = null;
        if (destino != null) extra = enviar(c.localA, base(id) + "/composicoes",
                mapa("lotacaoId", destino.lotacao(), "jornadaId", destino.jornada(), "cargoId", destino.cargo(),
                        "efetivoPrevisto", second), 201, "Composição adicional isolada").path("id").asLong();
        var req = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-SINTETICO-B230",
                "justificativa", "Publicação sintética explícita do intervalo");
        if (inicio.isBefore(RelogioSistema.hoje())) {
            var preview = enviar(c.localA, base(id) + "/previas/publicar", req, 200, "Prévia da publicação retroativa");
            req.put("tokenPrevia", preview.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        }
        enviar(c.localA, base(id) + "/publicar", req, 200, "Publicação do controle em transação própria");
        return new NovoContrato(id, first, extra, inicio, fim);
    }
    protected JsonNode aditamentoConfirmado(Cenario c, long contrato, Map<String,Object> req) throws Exception {
        var preview = enviar(c.localA, base(contrato) + "/aditamentos/previa", req, 200, "Prévia real do aditamento B230");
        req.put("tokenPrevia", preview.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        return enviar(c.localA, base(contrato) + "/aditamentos", req, 201, "Aditamento confirmado em transação própria");
    }
    protected LocalDate segundaNaJanela() {
        LocalDate day = RelogioSistema.hoje().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertThat(ZonedDateTime.now(RelogioSistema.ZONA)).isBefore(day.plusDays(7).atStartOfDay(RelogioSistema.ZONA));
        return day;
    }

    protected Destino novoDestino(Cenario c) {
        return nova(() -> {
            GestaoLocal g = em.getReference(GestaoLocal.class, c.gestaoA.getId());
            String suffix = UUID.randomUUID().toString();
            Cargo cargo = new Cargo("B230 Cargo " + suffix, "1"); cargo.setGestaoLocal(g); cargo.setAtivo(true); em.persist(cargo);
            Jornada jornada = new Jornada(); jornada.setGestaoLocal(g); jornada.setDescricao("B230 Turno " + suffix); jornada.setAtivo(true);
            for (DayOfWeek day : DayOfWeek.values()) {
                JornadaDiaSemana item = new JornadaDiaSemana(); item.setJornada(jornada); item.setDiaSemana(day);
                item.setDiaTrabalho(true); item.setEntrada1("08:00"); item.setSaida1("16:00"); jornada.getDias().add(item);
            }
            em.persist(jornada);
            Lotacao lotacao = new Lotacao("B230 Lotação " + suffix); lotacao.setGestaoLocal(g); lotacao.setAtivo(true);
            lotacao.setMunicipio("MUNICIPIO SINTETICO B230"); lotacao.getTurnos().add(jornada); em.persist(lotacao); em.flush();
            for (var entry : Map.of(TipoCatalogoTemporal.CARGO, cargo.getId(), TipoCatalogoTemporal.JORNADA, jornada.getId(),
                    TipoCatalogoTemporal.LOTACAO, lotacao.getId()).entrySet()) {
                EventoDisponibilidadeCatalogo source = new EventoDisponibilidadeCatalogo();
                source.setTipoCatalogo(entry.getKey()); source.setCatalogoId(entry.getValue()); source.setAtivo(true);
                source.setDataEfetiva(LocalDate.of(2025,1,1)); source.setRegistradoEm(Instant.parse("2025-01-01T00:00:00Z"));
                source.setMotivo("Fonte exclusivamente sintética B230, não acervo administrativo autêntico"); em.persist(source);
            }
            em.flush(); return new Destino(cargo.getId(), lotacao.getId(), jornada.getId());
        });
    }

    protected long composicaoSintetica(Cenario c, Destino d) {
        return nova(() -> composicao(em.getReference(Contrato.class, c.contratoA.getId()),
                em.getReference(Lotacao.class, d.lotacao()), em.getReference(Jornada.class, d.jornada()),
                em.getReference(Cargo.class, d.cargo()), 5).getId());
    }
    protected UsuarioAplicacao operacional(Cenario c, Destino d) {
        return nova(() -> {
            UsuarioAplicacao user = usuario(PerfilAcesso.OPERACIONAL, em.getReference(GestaoLocal.class, c.gestaoA.getId()));
            user.setLotacao(em.getReference(Lotacao.class, d.lotacao())); user.setJornada(em.getReference(Jornada.class, d.jornada()));
            em.flush(); return user;
        });
    }
    protected JsonNode novoVinculo(Cenario c, long contrato, long composicao, LocalDate inicio, LocalDate fim, int seed) throws Exception {
        String cpf = cpf(Math.toIntExact(650000000L + contrato * 100L + seed));
        return enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoa", mapa("nome", "B230 Pessoa " + cpf, "cpf", cpf),
                "contratoId", contrato, "composicaoId", composicao, "dataInicio", inicio, "dataFim", fim),
                201, "Fixture própria B230 criada pela API canônica");
    }
    protected Map<String,Object> justificativa() {
        return mapa("justificativa", "Fato sintético documentado para contraprova B230",
                "evidencia", "SEI-SINTETICO-B230", "autorizacao", "ATO-SINTETICO-B230");
    }
    protected JsonNode retificarInicio(Cenario c, long vinculo, LocalDate inicio) throws Exception {
        var request = justificativa(); request.put("novoInicio", inicio);
        var preview = enviar(c.localA, "/api/vinculos-contratuais/" + vinculo + "/previas/retificacao-inicio", request, 200,
                "Prévia real da retificação B230");
        request.put("tokenPrevia", preview.path("token").asText()); request.put("confirmarImpacto", true);
        return enviar(c.localA, "/api/vinculos-contratuais/" + vinculo + "/retificar-inicio", request, 200,
                "Retificação explícita com prévia válida");
    }
    protected JsonNode alterarAlocacao(Cenario c, long vinculo, long composicao, LocalDate data, Long retificar) throws Exception {
        var request = justificativa(); request.put("dataEfetiva", data); request.put("composicaoId", composicao);
        var preview = enviar(c.localA, "/api/vinculos-contratuais/" + vinculo + "/previas/alocacoes", request, 200,
                "Prévia real da mudança de faixa B230");
        assertThat(preview.path("incompatibilidades")).isEmpty();
        request.put("tokenPrevia", preview.path("token").asText()); request.put("confirmarImpacto", true);
        return enviar(c.localA, "/api/vinculos-contratuais/" + vinculo + "/alocacoes"
                + (retificar == null ? "" : "/" + retificar + "/retificar"), request, 200, "Mudança de faixa confirmada");
    }
    protected JsonNode ciclo(Cenario c, long contrato, String command, LocalDate data) throws Exception {
        var request = mapa("dataEfetiva", data, "motivo", "B230 habilitação administrativa " + command);
        var preview = enviar(c.localA, base(contrato) + "/previas/" + command, request, 200, "Prévia do ciclo B230");
        request.put("tokenPrevia", preview.path("tokenPrevia").asText()); request.put("confirmarImpacto", true);
        return enviar(c.localA, base(contrato) + "/" + command, request, 200, "Ciclo administrativo explícito B230");
    }
    protected JsonNode frequencia(UsuarioAplicacao user, long vinculo, LocalDate data, String status, int expected) throws Exception {
        var request = mapa("vinculoContratualId", vinculo, "dataReferencia", data, "status", status,
                "chaveIdempotencia", "b230-" + UUID.randomUUID());
        if (user.getPerfil() == PerfilAcesso.GESTOR_LOCAL)
            request.put("justificativaRetificacao", "Fato documental sintético B230 para relatório ou revisão");
        return enviar(user, "/api/frequencias", request, expected, "B230 operação discriminante com nova intenção");
    }
    protected JsonNode procurarInicio(Cenario c, long pessoa, LocalDate data) throws Exception {
        return dados(executar(c.localA, get("/api/vinculos-contratuais").param("pessoaId", Long.toString(pessoa))
                .param("inicioDe", data.toString()).param("inicioAte", data.toString()), 200, "Filtro de início canônico B230"));
    }
    protected void registrarB230(String caso, Object evidence) throws Exception {
        Path file = Path.of("target/auditoria-http/b230-complementos.jsonl"); Files.createDirectories(file.getParent());
        Files.writeString(file, json(mapa("caso", caso, "registradoEm", Instant.now(), "evidencia", evidence,
                "camada", "MockMvc/MySQL real; sem transporte TCP; fixture sintética")) + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
