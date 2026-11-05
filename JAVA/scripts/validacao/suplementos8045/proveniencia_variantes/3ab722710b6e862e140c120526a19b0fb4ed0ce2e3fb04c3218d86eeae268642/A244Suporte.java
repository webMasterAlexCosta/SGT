package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.infra.ConfiguracaoMysqlIntegracao;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.example.siggtrat.servico.PendenciaAdministrativaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * External test utility. No product classes are changed or recompiled.
 * MockMvc transport; real services/security/JPA/MySQL; synthetic auth inherited.
 * Background TaskScheduler is the inherited test double, never an E2E claim.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.data.redis.database=9"})
abstract class A244Suporte extends ContratosHttpBase {
    static final String SCHEMA = "sgt_audit_h02_crit_a2";
    static {
        if (!"1".equals(System.getenv("SGT_CRIT_A_EXECUTE")))
            throw new IllegalStateException("A244 requires the explicit execution flag after coordinator GO");
        var cfg = ConfiguracaoMysqlIntegracao.carregar();
        if (cfg.usaTestcontainers() || cfg.url() == null || !cfg.url().startsWith("jdbc:mysql://"))
            throw new IllegalStateException("A244 requires the dedicated native MySQL configuration");
        var uri = URI.create(cfg.url().substring("jdbc:".length()));
        if (!"h02_crit_a2".equals(cfg.usuario())
                || !"127.0.0.1".equals(uri.getHost()) || uri.getPort() != 33461
                || !("/" + SCHEMA).equals(uri.getPath()))
            throw new IllegalStateException("A244 refuses any database outside its dedicated native identity");
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (!cwd.toString().contains("/tests/a244_followup_v2/materialized/")
                || !Files.isRegularFile(cwd.resolve("H02_A244_MATERIALIZED.json")))
            throw new IllegalStateException("A244 must execute only in the sealed external project");
    }

    @Autowired protected PendenciaAdministrativaServico pendenciasA244;
    @Autowired protected GestaoContratoTemporalServico gestaoTemporalA244;
    @Autowired private Environment environmentA244;

    @BeforeEach void identityAndTransactionA244() throws Exception {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(environmentA244.getProperty("spring.data.redis.database")).isEqualTo("9");
        readTx(() -> {
            Object[] row = (Object[]) em.createNativeQuery("select database(), current_user(), version()").getSingleResult();
            assertThat(row[0].toString()).isEqualTo(SCHEMA);
            assertThat(row[1].toString()).startsWith("h02_crit_a2@");
            assertThat(row[2].toString()).isEqualTo("8.0.46");
            return null;
        });
    }

    protected <T> T tx(Callable<T> work) throws Exception { return isolated(work, false); }
    protected <T> T readTx(Callable<T> work) throws Exception { return isolated(work, true); }
    private <T> T isolated(Callable<T> work, boolean readOnly) throws Exception {
        var template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setReadOnly(readOnly);
        try {
            return template.execute(status -> {
                em.clear();
                try { T value=work.call(); if (!readOnly) em.flush(); return value; }
                catch (Exception failure) { throw new TestWorkFailure(failure); }
                finally { em.clear(); }
            });
        } catch (TestWorkFailure failure) {
            if (failure.getCause() instanceof Exception cause) throw cause;
            throw failure;
        }
    }
    private static final class TestWorkFailure extends RuntimeException {
        TestWorkFailure(Throwable cause) { super(cause); }
    }

    protected Cenario fixture() throws Exception { return tx(this::criarCenario); }
    protected NovoContrato published(Cenario c, int total, Integer first, boolean future) throws Exception {
        return tx(() -> super.publicado(c,total,first,future));
    }
    @Override protected JsonNode previaAditamento(Cenario c,long id,Map<String,Object> request) throws Exception {
        JsonNode preview=enviar(c.localA,base(id)+"/aditamentos/previa",request,200,"A244 real ADI preview");
        request.put("tokenPrevia",preview.path("tokenPrevia").asText());return preview;
    }
    @Override protected JsonNode registrarAditamento(Cenario c,long id,Map<String,Object> request) throws Exception {
        previaAditamento(c,id,request);
        return enviar(c.localA,base(id)+"/aditamentos",request,201,"A244 real ADI commit");
    }
    @Override protected JsonNode previaRemanejamento(Cenario c,long id,Map<String,Object> request) throws Exception {
        JsonNode preview=enviar(c.localA,base(id)+"/remanejamentos/previa",request,200,"A244 real REM preview");
        request.put("tokenPrevia",preview.path("tokenPrevia").asText());return preview;
    }
    protected JsonNode cycle(Cenario c,long id,String action,Map<String,Object> request) throws Exception {
        JsonNode preview=enviar(c.localA,base(id)+"/previas/"+action,request,200,"A244 lifecycle preview "+action);
        request.put("tokenPrevia",preview.path("tokenPrevia").asText());request.put("confirmarImpacto",true);
        return enviar(c.localA,base(id)+"/"+action,request,200,"A244 lifecycle command "+action);
    }
    protected JsonNode link(Cenario c,NovoContrato n,long composition,LocalDate start,int seed) throws Exception {
        return enviar(c.localA,"/api/vinculos-contratuais",mapa("contratoId",n.id(),"composicaoId",composition,
                "dataInicio",start,"pessoa",mapa("nome","Pessoa sintética A244 "+seed,
                "cpf",cpf(seed+Math.toIntExact(n.id())))),201,"A244 private synthetic link");
    }
    protected JsonNode byId(JsonNode array,long id) {
        assertThat(array.isArray()).isTrue();
        var matches=new ArrayList<JsonNode>();for(var row:array)if(row.path("id").asLong()==id)matches.add(row);
        assertThat(matches).hasSize(1);return matches.get(0);
    }
    protected void preservesRows(JsonNode before,JsonNode after,String label) {
        assertThat(before.isArray()).isTrue();assertThat(after.isArray()).isTrue();
        for(var row:before)assertThat(byId(after,row.path("id").asLong())).as(label).isEqualTo(row);
    }
    protected ObjectNode versionValues(JsonNode node) {
        assertThat(node.isObject()).isTrue();ObjectNode copy=node.deepCopy();
        copy.remove("provaCampos");return copy;
    }
    protected long cause(Cenario c,NovoContrato n,CategoriaTerminoVinculo category,LocalDate first,LocalDate last) throws Exception {
        return tx(() -> {
            String description="Causa sintética A244 "+category;
            var m=new MotivoDesligamento();m.setGestaoLocal(em.find(GestaoLocal.class,c.gestaoA.getId()));
            m.setDescricao(description);m.setCategoria(category);em.persist(m);em.flush();
            var origin=new EventoDisponibilidadeCatalogo();origin.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            origin.setCatalogoId(m.getId());origin.setDataEfetiva(first);origin.setAtivo(true);
            origin.setMotivo("Disponibilidade declarada pela fixture sintética A244");em.persist(origin);em.flush();
            FonteMotivoSintetica.declarar(em,gestaoTemporalA244,n.id(),m.getId(),c.gestaoA.getId(),c.localA,
                    description,category,first,last,Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                    "SEI-SINTETICO-A244-"+n.id()+"-"+category);
            return m.getId();
        });
    }
    protected void record(String criterion,Object observations) throws Exception {
        Path p=Path.of("target/a244-criteria",getClass().getSimpleName()+".json");Files.createDirectories(p.getParent());
        var data=mapa("criterion",criterion,"status","ASSERTIONS_COMPLETED", "observedAt",Instant.now(),
                "layer","MockMvc + real JPA/MySQL; not TCP/Chrome", "schema",SCHEMA,
                "testTransaction","NOT_SUPPORTED; explicit REQUIRES_NEW observations", "observations",observations,
                "automaticCriterionAcceptance",false,"documentAuthenticityClaimed",false);
        Files.writeString(p,mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)+"\n",StandardOpenOption.CREATE_NEW);
    }

    protected record TableImage(List<String> columns,long rows,String sha256) {}
    protected record PhysicalImage(Map<String,TableImage> tables,Map<String,String> files,List<String> exclusions) {}

    /**
     * Every physical row/column is hashed, excluding session refresh and technical
     * request audit. Business event/source/version/pendency tables are not excluded.
     * Values remain in memory; assertion output contains only names/counts/hashes.
     */
    protected PhysicalImage physicalImage() throws Exception {
        Map<String,TableImage> tables=readTx(() -> em.unwrap(Session.class).doReturningWork(connection -> {
            if (!SCHEMA.equals(connection.getCatalog()))throw new SQLException("A244 schema guard");
            Map<String,TableImage> images=new TreeMap<>();List<String> names=new ArrayList<>();
            try(ResultSet rs=connection.getMetaData().getTables(SCHEMA,null,"%",new String[]{"TABLE"})) {
                while(rs.next())names.add(rs.getString("TABLE_NAME"));
            }
            for(String name:names) {
                if(Set.of("token_ativo","registro_auditoria").contains(name))continue;
                if(!name.matches("[A-Za-z0-9_]+"))throw new SQLException("Unrecognized table identifier");
                List<String> columns=new ArrayList<>(),rows=new ArrayList<>();
                try(Statement statement=connection.createStatement();ResultSet rs=statement.executeQuery("select * from `"+name+"`")) {
                    var md=rs.getMetaData();for(int i=1;i<=md.getColumnCount();i++)columns.add(md.getColumnLabel(i)+":"+md.getColumnTypeName(i));
                    while(rs.next()) {
                        List<String> values=new ArrayList<>();
                        for(int i=1;i<=md.getColumnCount();i++) {
                            byte[] value=rs.getBytes(i);
                            values.add(value==null?"NULL":"BYTES:"+HexFormat.of().formatHex(value));
                        }
                        rows.add(String.join("|",values));
                    }
                }
                Collections.sort(rows);images.put(name,new TableImage(columns,rows.size(),digest(String.join("\n",rows).getBytes(StandardCharsets.UTF_8))));
            }
            return images;
        }));
        assertThat(tables.keySet()).contains("contrato","aditamento_contratual","documento_comprobatorio","pendencia_administrativa");
        Map<String,String> files=new TreeMap<>();
        Path root=Path.of(environmentA244.getRequiredProperty("file.upload-dir")).toAbsolutePath().normalize();
        assertThat(root).as("Actual configured storage belongs to this isolated project").startsWith(Path.of("").toAbsolutePath().normalize());
        if(Files.exists(root))try(var stream=Files.walk(root)) {
            for(Path p:stream.filter(Files::isRegularFile).sorted().toList())files.put(root.relativize(p).toString(),digest(Files.readAllBytes(p)));
        }
        return new PhysicalImage(tables,files,List.of("token_ativo: session metadata","registro_auditoria: technical request/attempt audit"));
    }
    protected static String digest(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
