package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.AplicacaoBackendSigGtrat;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.infra.ConfiguracaoPostgresIntegracao;
import com.example.siggtrat.servico.ProjecaoOperacionalPessoaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.example.siggtrat.utilitario.UtilitarioJwt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Full MVC/security/service/JPA chain. Only background scheduling is suppressed. */
@org.springframework.context.annotation.Import({com.example.siggtrat.infra.GuardaPostgres186.class, com.example.siggtrat.infra.ConfiguracaoObservacaoLocksPostgres.class})
@SpringBootTest(classes = AplicacaoBackendSigGtrat.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"env-file=./src/test/resources/auditoria-endpoints.env", "spring.cache.type=none",
                "spring.jpa.open-in-view=false", "spring.jpa.show-sql=false", "logging.level.root=WARN",
                "app.email.enabled=false", "app.documentos.diretorio=target/auditoria-http/documentos",
                "app.audit.fallback-log=target/auditoria-http/fallback.jsonl"})
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "frequencia.janela-operacional-dias=7")
@Transactional
public abstract class AuditoriaHttpSuporte {
    protected static final String SENHA = "Auditoria@2026";
    protected static final LocalDate DATA = LocalDate.of(2026, 6, 10);
    protected static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    protected static final LocalDate FIM = LocalDate.of(2027, 12, 31);
    private static final AtomicInteger SEQUENCIA = new AtomicInteger(30000000);
    private static final AtomicInteger SEQUENCIA_UF = new AtomicInteger();
    private static boolean sequenciaInicializada;
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected EntityManager em;
    @Autowired protected PasswordEncoder passwordEncoder;
    private String senhaCenarioCodificada;
    @Autowired protected UtilitarioJwt jwt;
    @Autowired protected ProjecaoOperacionalPessoaServico projecao;
    @Autowired protected PlatformTransactionManager transactionManager;
    @MockitoBean protected TaskScheduler taskScheduler;
    @Autowired protected RequestMappingHandlerMapping mappings;
    private final Map<Long, Cookie[]> sessoes = new HashMap<>();
    private String metodoTeste;
    private String casoTeste;

    private static final ConfiguracaoPostgresIntegracao CONFIGURACAO_POSTGRES = ConfiguracaoPostgresIntegracao.carregar();
    @DynamicPropertySource static void banco(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", CONFIGURACAO_POSTGRES::url);
        p.add("spring.datasource.username", CONFIGURACAO_POSTGRES::usuario);
        p.add("spring.datasource.password", CONFIGURACAO_POSTGRES::senha);
        p.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        p.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        p.add("spring.datasource.hikari.maximum-pool-size", () -> "8");
        p.add("spring.datasource.hikari.minimum-idle", () -> "2");
        p.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        p.add("spring.sql.init.mode", () -> "never");
        p.add("spring.jpa.properties.hibernate.hbm2ddl.halt_on_error", () -> "true");
    }
    @AfterEach void limparContexto() { SecurityContextHolder.clearContext(); }
    @BeforeEach void inventariarEndpoints(org.junit.jupiter.api.TestInfo testInfo) throws Exception {
        metodoTeste = testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("");
        casoTeste = testInfo.getDisplayName();
        List<Map<String,Object>> endpoints = new ArrayList<>();
        mappings.getHandlerMethods().forEach((info, handler) -> {
            if (!handler.getBeanType().getPackageName().startsWith("com.example.siggtrat.controlador")) return;
            for (String path : info.getPatternValues()) for (var method : info.getMethodsCondition().getMethods()) {
                Map<String,Object> endpoint = new LinkedHashMap<>();
                endpoint.put("method", method.name()); endpoint.put("path", path);
                endpoint.put("controller", handler.getBeanType().getSimpleName()); endpoint.put("handler", handler.getMethod().getName());
                endpoint.put("return_type", handler.getMethod().getGenericReturnType().getTypeName());
                endpoint.put("security_annotations", java.util.stream.Stream.concat(
                        Arrays.stream(handler.getBeanType().getAnnotations()), Arrays.stream(handler.getMethod().getAnnotations()))
                        .filter(a -> Set.of("PreAuthorize", "PostAuthorize", "ExigirSenha", "RolesAllowed", "Secured")
                                .contains(a.annotationType().getSimpleName())).map(Object::toString).toList());
                List<Map<String,Object>> args = new ArrayList<>();
                for (var p : handler.getMethod().getParameters()) args.add(descreverParametro(p));
                endpoint.put("parameters", args); endpoints.add(endpoint);
            }
        });
        endpoints.sort(Comparator.comparing(x -> x.get("path") + " " + x.get("method")));
        Path path = Path.of("target/auditoria-http/endpoints-runtime.json"); Files.createDirectories(path.getParent());
        Files.writeString(path, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(endpoints));
        Path tipos = path.resolveSibling("contratos-dto-runtime.json");
        { // Regerar dos tipos efetivos: arquivo antigo não comprova o bytecode atual.
            Map<String,Object> schemas = new TreeMap<>();
            mappings.getHandlerMethods().forEach((info, handler) -> {
                if (!handler.getBeanType().getPackageName().startsWith("com.example.siggtrat.controlador")) return;
                descreverTipo(mapper.constructType(handler.getMethod().getGenericReturnType()), schemas);
                for (var parametro : handler.getMethod().getGenericParameterTypes())
                    descreverTipo(mapper.constructType(parametro), schemas);
            });
            Files.writeString(tipos, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schemas));
        }
    }

    /** Metadados Jackson efetivos; existência de campo/constraint não é prova comportamental. */
    private void descreverTipo(com.fasterxml.jackson.databind.JavaType tipo, Map<String,Object> schemas) {
        if (tipo == null) return;
        for (int i = 0; i < tipo.containedTypeCount(); i++) descreverTipo(tipo.containedType(i), schemas);
        Class<?> raw = tipo.getRawClass();
        if (!raw.getPackageName().startsWith("com.example.siggtrat") || schemas.containsKey(tipo.toCanonical())) return;
        Map<String,Object> schema = new LinkedHashMap<>();
        schemas.put(tipo.toCanonical(), schema);
        if (raw.isEnum()) {
            schema.put("enum", Arrays.stream(raw.getEnumConstants()).map(Object::toString).toList());
            return;
        }
        if (!raw.getPackageName().startsWith("com.example.siggtrat.dto")
                && !raw.getPackageName().startsWith("com.example.siggtrat.entidade")) return;
        for (boolean entrada : List.of(true, false)) {
            var bean = entrada ? mapper.getDeserializationConfig().introspect(tipo)
                    : mapper.getSerializationConfig().introspect(tipo);
            List<Map<String,Object>> campos = new ArrayList<>();
            for (var propriedade : bean.findProperties()) {
                if (entrada && !propriedade.couldDeserialize() || !entrada && !propriedade.couldSerialize()) continue;
                Map<String,Object> campo = new LinkedHashMap<>();
                campo.put("name", propriedade.getName()); campo.put("type", propriedade.getPrimaryType().toCanonical());
                campo.put("required_by_jackson", propriedade.isRequired());
                List<String> restricoes = new ArrayList<>();
                var membro = propriedade.getPrimaryMember();
                if (membro != null) membro.annotations().forEach(a -> {
                    if (a.annotationType().getPackageName().startsWith("jakarta.validation")) restricoes.add(a.toString());
                });
                campo.put("validation_annotations", restricoes); campos.add(campo);
                descreverTipo(propriedade.getPrimaryType(), schemas);
            }
            schema.put(entrada ? "desserializacao" : "serializacao", campos);
        }
    }

    private Map<String,Object> descreverParametro(java.lang.reflect.Parameter p) {
        Map<String,Object> resultado = new LinkedHashMap<>();
        resultado.put("name", p.getName());
        resultado.put("type", p.getParameterizedType().getTypeName());
        List<Map<String,Object>> annotations = new ArrayList<>();
        for (var a : p.getAnnotations()) {
            Map<String,Object> detalhe = new LinkedHashMap<>();
            detalhe.put("annotation", a.annotationType().getSimpleName());
            for (var atributo : a.annotationType().getDeclaredMethods()) {
                try {
                    Object valor = atributo.invoke(a);
                    if (valor instanceof Class<?> classe) valor = classe.getName();
                    if (valor instanceof Class<?>[] classes) valor = Arrays.stream(classes).map(Class::getName).toList();
                    detalhe.put(atributo.getName(), valor);
                } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            }
            annotations.add(detalhe);
        }
        resultado.put("annotations", annotations);
        if (p.getType().getPackageName().startsWith("com.example.siggtrat.dto")) {
            var tipo = mapper.constructType(p.getParameterizedType());
            var bean = mapper.getDeserializationConfig().introspect(tipo);
            resultado.put("body_fields", bean.findProperties().stream().map(propriedade -> {
                Map<String,Object> campo = new LinkedHashMap<>();
                campo.put("name", propriedade.getName());
                campo.put("type", propriedade.getPrimaryType().toCanonical());
                var membro = propriedade.getPrimaryMember();
                List<String> restricoes = new ArrayList<>();
                if (membro != null) membro.annotations().forEach(a -> {
                    if (a.annotationType().getPackageName().startsWith("jakarta.validation")) restricoes.add(a.toString());
                });
                campo.put("validation_annotations", restricoes);
                return campo;
            }).toList());
        }
        return resultado;
    }

    protected String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    protected JsonNode dados(MvcResult result) throws Exception {
        JsonNode root = mapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        return root != null && root.has("data") && !root.get("data").isNull() ? root.get("data") : root;
    }
    protected AbstractMockHttpServletRequestBuilder<?> autenticar(AbstractMockHttpServletRequestBuilder<?> request, UsuarioAplicacao usuario) {
        if (usuario == null) return request;
        Cookie[] cookies = sessoes.computeIfAbsent(usuario.getId(), id -> {
            String raw = jwt.gerarToken(id), device = "dispositivo-auditoria-" + id;
            var now = RelogioSistema.agora();
            TokenAtivo session = TokenAtivo.deTokenBruto(raw, usuario.getUsername(), now, now.plusHours(4));
            session.setDeviceHash(UtilitarioHashSeguranca.sha256Hexadecimal(device)); session.setLastSeenAt(now);
            emTransacao(() -> { em.persist(session); em.flush(); return session; });
            return new Cookie[]{new Cookie("autenticacao", raw), new Cookie("id_dispositivo", device),
                    new Cookie("XSRF-TOKEN", "csrf-auditoria-" + id)};
        });
        return request.cookie(cookies).header("X-XSRF-TOKEN", cookies[2].getValue()).header("X-User-Password", SENHA);
    }
    protected MvcResult executar(UsuarioAplicacao usuario, AbstractMockHttpServletRequestBuilder<?> request,
                                int esperado, String cenario) throws Exception {
        if (TransactionSynchronizationManager.isActualTransactionActive()) em.flush();
        MvcResult result = mvc.perform(autenticar(request, usuario)).andReturn();
        if (result.getRequest().isAsyncStarted()) {
            result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(result)).andReturn();
        }
        registrar(result, usuario, esperado, cenario);
        assertEquals(esperado, result.getResponse().getStatus(), cenario + ": " + abreviar(result));
        return result;
    }
    private String abreviar(MvcResult result) {
        try { String s = result.getResponse().getContentAsString(); return s.substring(0, Math.min(500, s.length())); }
        catch (Exception e) { return "resposta não textual"; }
    }
    private void registrar(MvcResult r, UsuarioAplicacao u, int esperado, String cenario) throws Exception {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("audit_round", System.getenv("SGT_AUDITORIA_RODADA"));
        item.put("recorded_at", Instant.now().toString());
        item.put("suite", getClass().getSimpleName()); item.put("cenario", cenario);
        item.put("test_method", metodoTeste); item.put("test_case", casoTeste);
        item.put("method", r.getRequest().getMethod()); item.put("path", r.getRequest().getRequestURI());
        item.put("mapping", r.getRequest().getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        item.put("perfil", u == null ? "ANONIMO" : u.getPerfil().name());
        item.put("gestao", u == null || u.getGestaoLocal() == null ? null : u.getGestaoLocal().getId());
        item.put("expected_status", esperado); item.put("actual_status", r.getResponse().getStatus());
        item.put("status_ok", esperado == r.getResponse().getStatus()); item.put("content_type", r.getResponse().getContentType());
        item.put("response_bytes", r.getResponse().getContentAsByteArray().length);
        Map<String,String> headers = new TreeMap<>();
        for (String nome : List.of("Content-Type", "Content-Disposition", "Cache-Control", "X-Content-Type-Options", "X-Frame-Options", "X-SGT-Cache")) {
            String valor = r.getResponse().getHeader(nome); if (valor != null) headers.put(nome, valor);
        }
        item.put("response_headers", headers);
        item.put("query_fields", new TreeSet<>(r.getRequest().getParameterMap().keySet()));
        item.put("request_content_type", r.getRequest().getContentType());
        try {
            JsonNode entrada = mapper.readTree(r.getRequest().getContentAsByteArray());
            Set<String> fields = new TreeSet<>();
            if (entrada != null && entrada.isObject()) entrada.fieldNames().forEachRemaining(fields::add);
            item.put("request_fields", fields);
            item.put("request_body_redacted", mascararEvidencia(entrada));
        } catch (java.io.IOException | IllegalArgumentException ignored) { /* Não registrar valores nem conteúdo binário. */ }
        try {
            JsonNode response = mapper.readTree(r.getResponse().getContentAsByteArray());
            if (r.getResponse().getContentAsByteArray().length <= 16384)
                item.put("response_body_redacted", mascararEvidencia(response));
            else item.put("response_body_redacted", "Corpo acima de 16 KiB; conferir teste e artefato de exportação.");
            JsonNode data = response != null && response.has("data") ? response.get("data") : response;
            if (data != null && data.isObject() && data.has("content")) data = data.get("content");
            if (data != null && data.isArray() && !data.isEmpty()) data = data.get(0);
            Set<String> fields = new TreeSet<>();
            if (data != null && data.isObject()) data.fieldNames().forEachRemaining(fields::add);
            item.put("response_fields", fields);
        } catch (java.io.IOException ignored) { /* Binary/empty responses are described by media type and bytes. */ }
        Path path = Path.of("target/auditoria-http/requests.jsonl"); Files.createDirectories(path.getParent());
        synchronized (AuditoriaHttpSuporte.class) {
            Files.writeString(path, mapper.writeValueAsString(item) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private JsonNode mascararEvidencia(JsonNode valor) {
        if (valor == null) return null;
        if (valor.isObject()) {
            var copia = mapper.createObjectNode();
            valor.fields().forEachRemaining(e -> {
                if (e.getKey().toLowerCase(Locale.ROOT).matches(".*(password|senha|token|secret|segredo|authorization|cookie|totp|otp).*"))
                    copia.put(e.getKey(), "[MASCARADO_NA_EVIDENCIA]");
                else copia.set(e.getKey(), mascararEvidencia(e.getValue()));
            });
            return copia;
        }
        if (valor.isArray()) {
            var copia = mapper.createArrayNode(); valor.forEach(v -> copia.add(mascararEvidencia(v))); return copia;
        }
        return valor.deepCopy();
    }

    /** Os atores de uma fixture compartilham a senha sintética; o BCrypt real
     * e sua força permanecem os configurados na aplicação. Evita codificações
     * redundantes durante o preparo, sem substituir matches/autenticação. */
    private synchronized String senhaCenarioCodificada() {
        if (senhaCenarioCodificada == null) senhaCenarioCodificada = passwordEncoder.encode(SENHA);
        return senhaCenarioCodificada;
    }

    protected UsuarioAplicacao usuario(PerfilAcesso perfil, GestaoLocal gestao) {
        UsuarioAplicacao u = new UsuarioAplicacao(); int n = SEQUENCIA.incrementAndGet();
        u.setUsername("audit-" + n); u.setMatricula(String.valueOf(n)); u.setEmail("audit-" + n + "@example.test");
        u.setPassword(senhaCenarioCodificada()); u.setPerfil(perfil); u.setActive(true); u.setForceChangePassword(false);
        u.setGestaoLocal(gestao); u.setSuperintendencia(gestao == null ? null : gestao.getSuperintendencia());
        em.persist(u); return u;
    }
    protected <T> T emTransacao(java.util.function.Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }
    /**
     * Fonte sintética declarada somente pelo cenário que a invoca. Não é
     * recuperação de nome legado e não integra criarCenario: casos de ausência
     * documental devem continuar sem evento nominal.
     */
    protected void declararFonteNominalSintetica(Cenario c, String referencia) {
        emTransacao(() -> {
            VinculoContratual v = em.find(VinculoContratual.class, c.vinculoA.getId());
            assertEquals(0L, em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", v.getId()).getSingleResult());
            EventoVinculoContratual origem = new EventoVinculoContratual();
            origem.setVinculo(v);
            origem.setTipoEvento(com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual.CRIACAO);
            origem.setRelacaoPrecedencia(com.example.siggtrat.dominio.vinculo.TipoRelacaoEventoVinculo.ORIGEM);
            origem.setDataEfetiva(INICIO); origem.setRegistradoEm(INICIO.atStartOfDay(ZoneOffset.UTC).toInstant());
            origem.setInicioNovo(v.getDataInicio()); origem.setFimNovo(v.getDataFim());
            origem.setMatriculaNova(v.getMatricula());
            origem.setPessoaNomeFotografia(v.getPessoa().getNome()); origem.setPessoaCpfFotografia(v.getPessoa().getCpf());
            origem.setContratoNumeroFotografia(v.getContrato().getNumeroContrato());
            origem.setContratoAnoFotografia(v.getContrato().getAnoContrato());
            origem.setGestaoLocalIdFotografia(c.gestaoA.getId()); origem.setGestaoLocalNomeFotografia(c.gestaoA.getNome());
            origem.setResponsavelId(c.localA.getId()); origem.setResponsavelUsername(c.localA.getUsername());
            origem.setJustificativa("Fonte sintética explícita da admissão, limites e identidade do cenário " + referencia);
            origem.setEvidencia("SEI-FIXTURE-" + referencia); origem.setAutorizacao("ATO-FIXTURE-" + referencia);
            origem.setHashEvento(UtilitarioHashSeguranca.sha256Hexadecimal("FIXTURE-ORIGEM-NOMINAL|"
                    + referencia + "|" + v.getId() + "|" + INICIO + "|" + v.getDataInicio()
                    + "|" + v.getDataFim() + "|" + v.getMatricula() + "|" + v.getPessoa().getNome()
                    + "|" + v.getPessoa().getCpf()));
            em.persist(origem); em.flush();
            assertEquals(1L, em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", v.getId()).getSingleResult());
            return null;
        });
    }

    protected Cenario criarCenarioPersistido() { return emTransacao(this::criarCenario); }
    protected Cenario criarCenario() {
        synchronized (SEQUENCIA) {
            if (!sequenciaInicializada) {
                int maior = em.createQuery("select u.matricula from UsuarioAplicacao u", String.class).getResultStream()
                        .filter(Objects::nonNull).filter(m -> m.matches("\\d{8}"))
                        .mapToInt(Integer::parseInt).max().orElse(30000000);
                SEQUENCIA.accumulateAndGet(maior, Math::max); sequenciaInicializada = true;
            }
        }
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Cenario f = new Cenario();
        f.seA = new SuperintendenciaEstadual("Estado A " + sufixo, ufSinteticaLivre()); em.persist(f.seA);
        f.seB = new SuperintendenciaEstadual("Estado B " + sufixo, ufSinteticaLivre()); em.persist(f.seB);
        f.gestaoA = gestao("Gestão A " + sufixo, f.seA); f.gestaoB = gestao("Gestão B " + sufixo, f.seB);
        f.jornadaA = jornada(f.gestaoA); f.jornadaB = jornada(f.gestaoB);
        f.lotacaoA = lotacao(f.gestaoA, f.jornadaA); f.lotacaoB = lotacao(f.gestaoB, f.jornadaB);
        f.cargoA = cargo(f.gestaoA); f.cargoB = cargo(f.gestaoB);
        f.tipoA = tipo(f.gestaoA); f.tipoB = tipo(f.gestaoB);
        // Esta fixture fornece explicitamente a fonte sintética de disponibilidade
        // desde 2025. Não se infere a existência antiga pela flag atual do cadastro.
        for (var entrada : java.util.Map.of(
                com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO, java.util.List.of(f.lotacaoA.getId(), f.lotacaoB.getId()),
                com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.JORNADA, java.util.List.of(f.jornadaA.getId(), f.jornadaB.getId()),
                com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.CARGO, java.util.List.of(f.cargoA.getId(), f.cargoB.getId()),
                com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.TIPO_CONTRATACAO, java.util.List.of(f.tipoA.getId(), f.tipoB.getId())).entrySet()) {
            for (Long catalogoId : entrada.getValue()) {
                EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
                origem.setTipoCatalogo(entrada.getKey()); origem.setCatalogoId(catalogoId);
                origem.setDataEfetiva(java.time.LocalDate.of(2025, 1, 1)); origem.setAtivo(true);
                origem.setMotivo("Fonte sintética explícita da fixture de auditoria desde 2025");
                origem.setRegistradoEm(java.time.Instant.parse("2025-01-01T00:00:00Z")); em.persist(origem);
            }
        }
        f.contratoA = contrato(f.gestaoA, f.tipoA, "A-" + sufixo); f.contratoB = contrato(f.gestaoB, f.tipoB, "B-" + sufixo);
        f.composicaoA = composicao(f.contratoA, f.lotacaoA, f.jornadaA, f.cargoA, 5);
        f.composicaoB = composicao(f.contratoB, f.lotacaoB, f.jornadaB, f.cargoB, 5);
        f.pessoaA = pessoa("Pessoa A " + sufixo, cpf(SEQUENCIA.incrementAndGet()));
        f.pessoaB = pessoa("Pessoa B " + sufixo, cpf(SEQUENCIA.incrementAndGet()));
        f.vinculoA = vinculo(f.pessoaA, f.contratoA); f.vinculoB = vinculo(f.pessoaB, f.contratoB);
        f.alocacaoA = alocacao(f.vinculoA, f.composicaoA); f.alocacaoB = alocacao(f.vinculoB, f.composicaoB);
        if (em.createQuery("select count(s) from StatusEmpregado s where upper(s.descricao)='ATIVO'", Long.class).getSingleResult() == 0)
            em.persist(new StatusEmpregado("ATIVO"));
        f.empregadoA = projecao.projetar(f.vinculoA, f.alocacaoA); f.empregadoB = projecao.projetar(f.vinculoB, f.alocacaoB);
        f.localA = usuario(PerfilAcesso.GESTOR_LOCAL, f.gestaoA); f.localB = usuario(PerfilAcesso.GESTOR_LOCAL, f.gestaoB);
        f.estadualA = usuario(PerfilAcesso.GESTOR_ESTADUAL, f.gestaoA); f.estadualA.setGestaoLocal(null);
        f.estadualB = usuario(PerfilAcesso.GESTOR_ESTADUAL, f.gestaoB); f.estadualB.setGestaoLocal(null);
        f.nacional = usuario(PerfilAcesso.GESTOR_NACIONAL, null); f.presidente = usuario(PerfilAcesso.PRESIDENTE, null);
        f.admin = usuario(PerfilAcesso.ADMINISTRADOR, null);
        f.operacionalA = usuario(PerfilAcesso.OPERACIONAL, f.gestaoA); f.operacionalA.setLotacao(f.lotacaoA); f.operacionalA.setJornada(f.jornadaA);
        f.operacionalB = usuario(PerfilAcesso.OPERACIONAL, f.gestaoB); f.operacionalB.setLotacao(f.lotacaoB); f.operacionalB.setJornada(f.jornadaB);
        em.flush(); return f;
    }
    private String ufSinteticaLivre() {
        Set<String> usadas = new HashSet<>(em.createQuery("select s.uf from SuperintendenciaEstadual s", String.class).getResultList());
        usadas.add("YY");
        usadas.addAll(com.example.siggtrat.constante.ConstantesDominio.UFS_BRASIL);
        // Namespace exclusivamente sintético, persistido diretamente nesta
        // fixture. Duas SE distintas por cenário preservam o isolamento real;
        // os testes HTTP das27 UFs oficiais continuam usando o serviço próprio.
        // 57 símbolos ASCII, sem aspas, barras, espaços ou equivalentes por caixa.
        // 57² = 3.249 pares; 3.221 disponíveis após reservar as 27 UFs e YY.
        // A IT de capacidade confere a unicidade na collation real do PostgreSQL.
        String alfabeto = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ!#$%&()+,-.;=@[]^_{}~";
        int base = alfabeto.length();
        int quantidade = base * base;
        for (int tentativas=0;tentativas<quantidade;tentativas++) {
            int n = Math.floorMod(SEQUENCIA_UF.getAndIncrement(),quantidade);
            String uf = String.valueOf(alfabeto.charAt(n / base)) + alfabeto.charAt(n % base);
            if (!usadas.contains(uf)) return uf;
        }
        throw new IllegalStateException("Não há UF sintética disponível no banco descartável.");
    }
    protected GestaoLocal gestao(String nome, SuperintendenciaEstadual se) {
        GestaoLocal g = new GestaoLocal(); g.setNome(nome); g.setCodigo("AUD-" + SEQUENCIA.incrementAndGet()); g.setSuperintendencia(se); em.persist(g); return g;
    }
    protected Jornada jornada(GestaoLocal g) {
        Jornada j = new Jornada(); j.setDescricao("Turno 8h " + g.getNome()); j.setGestaoLocal(g);
        for (DayOfWeek dia : DayOfWeek.values()) {
            JornadaDiaSemana d = new JornadaDiaSemana(); d.setJornada(j); d.setDiaSemana(dia);
            d.setDiaTrabalho(dia.getValue() <= 5);
            if (dia.getValue() <= 5) { d.setEntrada1("08:00"); d.setSaida1("12:00"); d.setEntrada2("13:00"); d.setSaida2("17:00"); }
            j.getDias().add(d);
        }
        em.persist(j); return j;
    }
    protected Lotacao lotacao(GestaoLocal g, Jornada j) {
        Lotacao l = new Lotacao("Lotação " + g.getNome()); l.setGestaoLocal(g); l.setMunicipio("MUNICIPIO " + g.getId());
        l.getTurnos().add(j); em.persist(l); return l;
    }
    protected Cargo cargo(GestaoLocal g) { Cargo c = new Cargo("Cargo " + g.getNome(), "1"); c.setGestaoLocal(g); em.persist(c); return c; }
    protected TipoContratacao tipo(GestaoLocal g) {
        TipoContratacao t = new TipoContratacao(); t.setGestaoLocal(g); t.setDescricao("Tipo " + g.getNome());
        t.setCriadoEm(Instant.parse("2025-01-01T00:00:00Z")); em.persist(t); return t;
    }
    protected Contrato contrato(GestaoLocal g, TipoContratacao tipo, String numero) {
        Contrato c = new Contrato(); c.setGestaoLocal(g); c.setTipoContratacao(tipo); c.setTipo(tipo.getDescricao());
        c.setNumeroContrato(numero); c.setAnoContrato(2026); c.setDescricao("Contrato de auditoria " + numero);
        c.setCnpjEmpresa("11222333000181"); c.setRazaoSocialEmpresa("Empresa Sintética"); c.setNomeFantasiaEmpresa("Sintética");
        c.setDataInicio(INICIO); c.setDataFim(FIM); c.setEfetivoPrevisto(5); c.setEfetivoOriginalPublicado(5);
        c.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO); c.setPublicadoEm(Instant.parse("2026-01-01T03:00:00Z"));
        c.setReferenciaSeiPublicacao("SEI-AUDITORIA"); em.persist(c);
        VersaoContratoNegocial v = new VersaoContratoNegocial(); v.setContrato(c); v.setNumeroVersao(1); v.setDataEfetiva(INICIO);
        v.setNumeroContrato(numero); v.setAnoContrato(2026); v.setDescricao(c.getDescricao()); v.setDataInicio(INICIO); v.setDataFim(FIM);
        v.setGestaoLocalId(g.getId()); v.setGestaoLocalNome(g.getNome()); v.setEfetivoPrevisto(5);
        v.setCnpjEmpresa(c.getCnpjEmpresa()); v.setRazaoSocialEmpresa(c.getRazaoSocialEmpresa()); v.setNomeFantasiaEmpresa(c.getNomeFantasiaEmpresa());
        v.setTipoContratacaoId(tipo.getId()); v.setTipoContratacaoDescricao(tipo.getDescricao());
        v.setMatriculaVinculoObrigatoria(c.getMatriculaVinculoObrigatoria());
        v.setMatriculaTipoContratacaoObrigatoria(Boolean.TRUE.equals(tipo.getMatriculaVinculoObrigatoria()));
        v.setMatriculaVinculoObrigatoriaEfetiva(c.exigeMatriculaNoVinculo());
        v.setDiaSemanaReduzido(c.getDiaSemanaReduzido()); v.setHorasDiaReduzido(c.getHorasDiaReduzido());
        v.setHabilitacaoAdministrativa(c.getHabilitacaoAdministrativa());
        em.persist(v); return c;
    }
    protected ComposicaoContratual composicao(Contrato c, Lotacao l, Jornada j, Cargo cargo, int quantidade) {
        ComposicaoContratual x = new ComposicaoContratual(); x.setContrato(c); x.setLotacao(l); x.setJornada(j); x.setCargo(cargo);
        // Esta fixture declara a fonte sintética integral da publicação. Não é
        // backfill do domínio: testes de acervo sem origem removem a prova
        // explicitamente, e os fluxos reais a capturam na publicação HTTP.
        x.aplicarConfiguracaoDocumentada(l.getMunicipio(), com.example.siggtrat.utilitario.FotografiaJornada.capturar(j));
        x.setEfetivoPrevisto(quantidade); x.setDataInicioVigencia(INICIO); x.setDataFimVigencia(FIM); em.persist(x); return x;
    }
    protected Pessoa pessoa(String nome, String cpf) { Pessoa p = new Pessoa(); p.setNome(nome); p.setCpf(cpf); em.persist(p); return p; }
    protected VinculoContratual vinculo(Pessoa p, Contrato c) {
        VinculoContratual v = new VinculoContratual(); v.setPessoa(p); v.setContrato(c); v.setDataInicio(INICIO); v.setMatricula("M-" + p.getId()); em.persist(v); return v;
    }
    protected AlocacaoVinculo alocacao(VinculoContratual v, ComposicaoContratual c) {
        AlocacaoVinculo a = new AlocacaoVinculo(); a.setVinculo(v); a.setComposicao(c); a.setLotacao(c.getLotacao());
        a.setJornada(c.getJornada()); a.setCargo(c.getCargo()); a.setDataInicio(v.getDataInicio()); a.setDataFim(c.getDataFimVigencia()); em.persist(a); return a;
    }
    protected Frequencia frequencia(Cenario f, boolean ladoB, LocalDate data, String status) {
        VinculoContratual v = ladoB ? f.vinculoB : f.vinculoA; AlocacaoVinculo a = ladoB ? f.alocacaoB : f.alocacaoA;
        Frequencia r = new Frequencia(); r.setEmpregado(ladoB ? f.empregadoB : f.empregadoA); r.setPessoa(v.getPessoa());
        r.setVinculoContratual(v); r.setAlocacaoVinculo(a); r.setDataReferencia(data); r.setStatus(status);
        r.setContratoSnapshotId(v.getContrato().getId()); r.setLotacaoSnapshotId(a.getLotacao().getId());
        r.setJornadaSnapshotId(a.getJornada().getId()); r.setCargoSnapshotId(a.getCargo().getId());
        GestaoLocal g = v.getContrato().getGestaoLocal(); r.setGestaoLocalSnapshotId(g.getId()); r.setSuperintendenciaSnapshotId(g.getSuperintendencia().getId());
        em.persist(r); return r;
    }
    protected static String cpf(int seed) {
        String base = String.format("%09d", seed); int sum = 0;
        for (int i=0;i<9;i++) sum += (base.charAt(i)-'0')*(10-i);
        int d = 11-sum%11; base += d>=10 ? "0" : String.valueOf(d); sum=0;
        for (int i=0;i<10;i++) sum += (base.charAt(i)-'0')*(11-i);
        d=11-sum%11; return base+(d>=10 ? "0" : String.valueOf(d));
    }
    public static class Cenario {
        public SuperintendenciaEstadual seA, seB; public GestaoLocal gestaoA, gestaoB;
        public Lotacao lotacaoA, lotacaoB; public Jornada jornadaA, jornadaB; public Cargo cargoA, cargoB;
        public TipoContratacao tipoA, tipoB; public Contrato contratoA, contratoB; public ComposicaoContratual composicaoA, composicaoB;
        public Pessoa pessoaA, pessoaB; public VinculoContratual vinculoA, vinculoB; public AlocacaoVinculo alocacaoA, alocacaoB;
        public Empregado empregadoA, empregadoB;
        public UsuarioAplicacao localA, localB, estadualA, estadualB, nacional, presidente, admin, operacionalA, operacionalB;
    }
}
