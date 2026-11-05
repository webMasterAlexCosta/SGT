package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.metamodel.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/** External test-only support. No production class or business service is mocked. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"file.upload-dir=target/criteria-c/uploads", "spring.data.redis.database=11"})
abstract class CriteriosCHttpBase extends ContratosHttpBase {
    @Autowired protected PropriedadesArmazenamentoArquivo armazenamentoC;
    @Autowired private Environment ambienteC;
    protected record PessoaPreparada(Cenario c, NovoContrato contrato, long vinculo, long pessoa,
                                    long empregado, LocalDate inicio, LocalDate fato) {}

    @BeforeEach
    void confirmarIsolamentoEfetivo() {
        assertEquals("h02_crit_c", ambienteC.getProperty("spring.datasource.username"));
        String url = ambienteC.getProperty("spring.datasource.url", "");
        String schema = java.net.URI.create(url.substring("jdbc:".length())).getPath();
        assertTrue(Set.of("/h02_crit_c", "/sgt_audit_h02_crit_c").contains(schema), "Schema externo reservado obrigatório");
        assertEquals(11, Objects.requireNonNull(ambienteC.getProperty("spring.data.redis.database", Integer.class)).intValue());
        assertEquals("none", ambienteC.getProperty("spring.cache.type"));
        assertEquals(Path.of("target/criteria-c/uploads").toAbsolutePath().normalize(),
                armazenamentoC.uploadDir().toAbsolutePath().normalize());
    }

    protected NovoContrato contratoConfirmado(Cenario c, boolean futuro) {
        return emTransacao(() -> {
            try { return publicado(c, 10, null, futuro); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    protected PessoaPreparada pessoaPreparada() throws Exception {
        return pessoaPreparada(false);
    }

    protected PessoaPreparada pessoaPreparada(boolean duasComposicoes) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = duasComposicoes ? emTransacao(() -> {
            try { return publicado(c, 10, 5, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        }) : contratoConfirmado(c, false);
        LocalDate inicio = RelogioSistema.hoje().minusDays(20);
        JsonNode v = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa sintética ensaio C " + n.id(),
                        "cpf", cpf(Math.toIntExact(910000000L + n.id()))),
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", inicio),
                201, "PREPARACAO C: vínculo real, contexto e identidade documental explícitos");
        long vinculo = v.path("id").asLong(); assertTrue(vinculo > 0);
        long pessoa = emTransacao(() -> em.find(VinculoContratual.class, vinculo).getPessoa().getId());
        long empregado = emTransacao(() -> em.createQuery(
                "select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", pessoa).getSingleResult());
        return new PessoaPreparada(c, n, vinculo, pessoa, empregado, inicio, RelogioSistema.hoje().minusDays(2));
    }

    protected byte[] bytes(String marcador) {
        return ("PROVA SINTETICA EXCLUSIVA DO ENSAIO C\n" + marcador + "\n").getBytes(StandardCharsets.UTF_8);
    }

    protected MockHttpServletRequestBuilder ocorrencia(PessoaPreparada x, LocalDate data, int dias,
            String chave, String narrativa, String nome, byte[] conteudo, String classificacao) {
        var req = multipart("/api/ocorrencias").file(new MockMultipartFile("anexos", nome, "text/plain", conteudo))
                .param("empregadoId", Long.toString(x.empregado())).param("motivo", "ATESTADO")
                .param("dataOcorrencia", data.toString()).param("dias", Integer.toString(dias))
                .param("observacao", narrativa).param("numeroSei", "SEI-PRIVADO-C-" + x.vinculo())
                .param("classificacoesAnexos", classificacao);
        if (chave != null) req.param("chaveIdempotencia", chave);
        return req;
    }

    /** All scalar persistent attributes and relation IDs; never serialize lazy graphs or credentials. */
    protected Map<String, Object> linha(Object entidade, Class<?> tipo) {
        // Field reflection on a Hibernate proxy sees constructor defaults, not the loaded row.
        // Resolve the managed implementation inside the active read transaction; retain every field.
        Object fonte = org.hibernate.Hibernate.unproxy(entidade);
        assertTrue(tipo.isInstance(fonte), "Snapshot deve ler a implementação persistente da entidade");
        Object identidade = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(fonte);
        assertNotNull(identidade, "Snapshot exige identidade persistida");
        Map<String, Object> r = new TreeMap<>();
        for (Attribute<?, ?> a : em.getMetamodel().entity(tipo).getAttributes()) {
            if (a.isCollection()) continue;
            Object valor;
            try {
                var membro = a.getJavaMember();
                if (membro instanceof Field f) { f.setAccessible(true); valor = f.get(fonte); }
                else if (membro instanceof Method m) { m.setAccessible(true); valor = m.invoke(fonte); }
                else throw new IllegalStateException("Unsupported persistent member " + a.getName());
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            if (valor != null && (a.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE
                    || a.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_ONE))
                valor = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(valor);
            r.put(a.getName(), valor);
        }
        assertEquals(identidade, r.get("id"), "A leitura de campos deve conservar a identidade canônica");
        return r;
    }

    protected <T> List<Map<String, Object>> linhas(Class<T> tipo, String predicado, long id) {
        return em.createQuery("select x from " + tipo.getSimpleName() + " x where " + predicado + " order by x.id", tipo)
                .setParameter("id", id).getResultList().stream().map(x -> linha(x, tipo)).toList();
    }

    protected JsonNode estadoDocumental(long contrato) {
        return emTransacao(() -> mapper.valueToTree(mapa(
                "contrato", linha(em.find(Contrato.class, contrato), Contrato.class),
                "aditamentos", linhas(AditamentoContratual.class, "x.contrato.id=:id", contrato),
                "composicoes", linhas(ComposicaoContratual.class, "x.contrato.id=:id", contrato),
                "versoes", linhas(VersaoContratoNegocial.class, "x.contrato.id=:id", contrato),
                "eventos", linhas(EventoCicloVidaContrato.class, "x.contrato.id=:id", contrato),
                "documentos", linhas(DocumentoComprobatorio.class, "x.contrato.id=:id", contrato),
                "eventosDocumentos", linhas(EventoDocumentoComprobatorio.class, "x.documento.contrato.id=:id", contrato))));
    }

    protected JsonNode estadoOcorrencias(long empregado) {
        return emTransacao(() -> mapper.valueToTree(mapa(
                "ocorrencias", linhas(Ocorrencia.class, "x.empregado.id=:id", empregado),
                "contextos", linhas(ContextoOcorrencia.class, "x.ocorrencia.empregado.id=:id", empregado),
                "anexos", linhas(AnexoOcorrencia.class, "x.ocorrencia.empregado.id=:id", empregado),
                "historicos", linhas(HistoricoEmpregado.class, "x.empregado.id=:id", empregado),
                "frequencias", linhas(Frequencia.class, "x.empregado.id=:id", empregado),
                "eventosFrequencia", linhas(EventoFrequencia.class, "x.frequencia.empregado.id=:id", empregado))));
    }

    protected Map<String, String> arquivos() throws Exception {
        Path raiz = armazenamentoC.uploadDir().toAbsolutePath().normalize();
        Map<String, String> r = new TreeMap<>();
        if (!Files.exists(raiz)) return r;
        try (var s = Files.walk(raiz)) {
            for (Path p : s.filter(Files::isRegularFile).sorted().toList()) {
                assertFalse(Files.isSymbolicLink(p), "Arquivo do ensaio não pode escapar por symlink");
                r.put(raiz.relativize(p).toString(), sha(Files.readAllBytes(p)));
            }
        }
        return r;
    }

    protected byte[] arquivoDocumento(long id) throws Exception {
        String nome = emTransacao(() -> em.find(DocumentoComprobatorio.class, id).getNomeArmazenado());
        assertNotNull(nome); Path raiz = armazenamentoC.uploadDir().toAbsolutePath().normalize();
        Path p = raiz.resolve(nome).normalize(); assertTrue(p.startsWith(raiz));
        assertFalse(Files.isSymbolicLink(p)); return Files.readAllBytes(p);
    }

    protected Map<String, Object> metadadosImutaveisDocumento(long id) {
        return emTransacao(() -> {
            Map<String, Object> r = new TreeMap<>(linha(em.find(DocumentoComprobatorio.class, id), DocumentoComprobatorio.class));
            // Only these state/audit-head fields legitimately change in substitute/invalidate commands.
            for (String k : List.of("estado", "version", "quantidadeEventosAuditoria", "ultimoHashEventoAuditoria")) r.remove(k);
            return r;
        });
    }

    protected static String sha(byte[] valor) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    protected void registrar(String caso, Object dados) throws Exception {
        Path p = Path.of("target/criteria-c/observations", caso + ".json");
        Files.createDirectories(p.getParent());
        Files.writeString(p, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dados), StandardOpenOption.CREATE_NEW);
    }

    protected void semEfeitoOcorrencia(PessoaPreparada x, JsonNode estado, Map<String, String> files) throws Exception {
        assertEquals(estado, estadoOcorrencias(x.empregado()), "Recusa deve preservar todo o estado negocial persistido");
        assertEquals(files, arquivos(), "Recusa deve preservar os arquivos e não deixar órfãos");
    }
}
