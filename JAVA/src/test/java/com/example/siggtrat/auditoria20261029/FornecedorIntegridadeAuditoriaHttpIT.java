package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Fornecedor;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * EMP-001/003 e AUD-001 no cadastro compartilhado: requisições Spring MVC reais,
 * filtros de segurança, serviços e transações confirmadas no MySQL 8.0.45.
 * MockMvc não prova transporte TCP nem navegador. A fila de auditoria real é
 * descarregada pelo mesmo método de produção; não há mock de serviço ou banco.
 * A comparação com o mestre limita-se à identidade CNPJ explicitamente usada
 * para escrever rascunhos. Nomes documentais e fatos publicados são preservados.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FornecedorIntegridadeAuditoriaHttpIT extends ContratosHttpBase {
    private static final String F = "/api/fornecedores";
    @Autowired DataSource dataSource;
    @Autowired Environment ambiente;
    @Autowired FilaRegistroAuditoria fila;
    private String caso;

    @BeforeEach
    void confirmarServidorReal(TestInfo info) throws Exception {
        caso = info.getTestMethod().orElseThrow().getName();
        try (var conexao = dataSource.getConnection(); var stmt = conexao.createStatement();
             var rs = stmt.executeQuery("select version(), database(), current_user()")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("8.0.45");
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            evidencia("ambiente", mapa("servidor", rs.getString(1), "schema", rs.getString(2),
                    "identidadeEfetiva", rs.getString(3)));
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @Test
    void criacaoDeContratoRecusaFornecedorComCnpjExplicitoDeOutraEmpresa() throws Exception {
        var c = criarCenarioPersistido();
        var a = novaEmpresa(c, "Empresa A do documento");
        var b = novaEmpresa(c, "Empresa B escolhida");
        var req = pedidoContrato(c, "CRIAR-DIVERGENTE", a.path("cnpj").asText());
        req.put("fornecedorId", b.path("id").asLong());
        long antes = quantidadeContratos();
        try {
            enviar(c.localA, "/api/contratos", req, 400,
                    "EMP-001/003: POST não aceita fornecedor B com CNPJ explícito da empresa A");
        } finally {
            evidencia("criacao-divergente", mapa("request", req, "quantidadeAntes", antes,
                    "quantidadeDepois", quantidadeContratos(), "contratosComNumero", contratosComNumero(req)));
        }
        assertThat(quantidadeContratos()).isEqualTo(antes);
        assertThat(contratosComNumero(req)).isEmpty();
    }

    @Test
    void atualizacaoDeRascunhoRecusaCnpjDivergenteEReverteTodosOsCampos() throws Exception {
        var c = criarCenarioPersistido();
        var a = novaEmpresa(c, "Empresa A mantida");
        var b = novaEmpresa(c, "Empresa B incompatível");
        var req = pedidoContrato(c, "EDITAR-DIVERGENTE", a.path("cnpj").asText());
        req.put("fornecedorId", a.path("id").asLong());
        long id = enviar(c.localA, "/api/contratos", req, 201,
                "Controle positivo: rascunho tem identidade coerente").path("id").asLong();
        JsonNode antes = estadoContrato(id);
        req.put("fornecedorId", b.path("id").asLong());
        req.put("descricao", "Este texto também deve ser revertido");
        req.put("razaoSocialEmpresa", "Nome documental explícito da tentativa");
        try {
            executar(c.localA, put(base(id)).contentType(APPLICATION_JSON).content(json(req)), 400,
                    "EMP-003: PUT de rascunho não mistura identidade B com CNPJ A");
        } finally {
            evidencia("atualizacao-divergente", mapa("antes", antes, "depois", estadoContrato(id)));
        }
        assertThat(estadoContrato(id)).as("Rollback inclui FK, campos, versão, eventos e fotografias")
                .isEqualTo(antes);
    }

    @Test
    void vinculoDiretoRecusaCnpjDivergenteSemAlterarRascunhoOuAuditarSucesso() throws Exception {
        var c = criarCenarioPersistido();
        var a = novaEmpresa(c, "Empresa A original");
        var b = novaEmpresa(c, "Empresa B da tentativa");
        var req = pedidoContrato(c, "VINCULO-DIVERGENTE", a.path("cnpj").asText());
        req.put("fornecedorId", a.path("id").asLong());
        long id = enviar(c.localA, "/api/contratos", req, 201,
                "Controle positivo: empresa A documentada e vinculada").path("id").asLong();
        JsonNode antes = estadoContrato(id);
        String requestId = UUID.randomUUID().toString();
        try {
            executar(c.localA, put(base(id) + "/fornecedor/" + b.path("id").asLong())
                            .header("X-Request-Id", requestId), 400,
                    "EMP-003: vínculo direto não pode conservar CNPJ A depois de selecionar B");
        } finally {
            fila.descarregar();
            evidencia("vinculo-divergente", mapa("antes", antes, "depois", estadoContrato(id),
                    "auditoriasDaRequisicao", auditorias(requestId)));
        }
        assertThat(estadoContrato(id)).isEqualTo(antes);
        assertThat(auditorias(requestId)).as("Tentativa rejeitada não representa vínculo confirmado").isEmpty();
    }

    @Test
    void cnpjFormatadoCoerenteAceitaNomesDocumentaisDistintosDoMestre() throws Exception {
        var c = criarCenarioPersistido();
        var fornecedor = novaEmpresa(c, "Razão no cadastro atual");
        String cnpj = fornecedor.path("cnpj").asText();
        var req = pedidoContrato(c, "NOMES-DOCUMENTAIS", formatado(cnpj));
        req.put("fornecedorId", fornecedor.path("id").asLong());
        req.put("razaoSocialEmpresa", "Razão escrita no instrumento");
        req.put("nomeFantasiaEmpresa", "Fantasia exclusivamente documental");
        long id = enviar(c.localA, "/api/contratos", req, 201,
                "EMP-002: coincidência de identidade não exige igualdade dos nomes documentais").path("id").asLong();
        executar(c.localA, put(base(id) + "/fornecedor/" + fornecedor.path("id").asLong()), 200,
                "Vínculo com CNPJ coerente conserva os nomes do instrumento");
        var estado = estadoContrato(id);
        assertThat(estado.path("cnpjEmpresa").asText()).isEqualTo(cnpj);
        assertThat(estado.path("razaoSocialEmpresa").asText()).isEqualTo("Razão escrita no instrumento");
        assertThat(estado.path("nomeFantasiaEmpresa").asText()).isEqualTo("Fantasia exclusivamente documental");
        evidencia("nomes-documentais", estado);
    }

    @Test
    void crudMantemFallbackExistenteSemInventarNomeFantasia() throws Exception {
        var c = criarCenarioPersistido();
        var fornecedor = novaEmpresa(c, "Razão para fallback já existente");
        var req = pedidoContrato(c, "FALLBACK-EXISTENTE", null);
        req.put("fornecedorId", fornecedor.path("id").asLong());
        req.remove("razaoSocialEmpresa");
        req.remove("nomeFantasiaEmpresa");
        long id = enviar(c.localA, "/api/contratos", req, 201,
                "CRUD conserva preenchimento já existente de CNPJ e razão ausentes").path("id").asLong();
        JsonNode estado = estadoContrato(id);
        assertThat(estado.path("cnpjEmpresa").asText()).isEqualTo(fornecedor.path("cnpj").asText());
        assertThat(estado.path("razaoSocialEmpresa").asText()).isEqualTo(fornecedor.path("razaoSocial").asText());
        assertThat(estado.path("nomeFantasiaEmpresa").isNull()).isTrue();
        evidencia("fallback-existente", estado);
    }

    @Test
    void edicaoDoMestreNaoRecomparaNemReescreveIdentidadeDeFatoPublicado() throws Exception {
        var c = criarCenarioPersistido();
        var fornecedor = novaEmpresa(c, "Razão original do mestre");
        long fornecedorId = fornecedor.path("id").asLong();
        var req = pedidoContrato(c, "HISTORIA-EMPRESA", fornecedor.path("cnpj").asText());
        req.put("fornecedorId", fornecedorId);
        req.put("razaoSocialEmpresa", "Razão comprovada no documento");
        req.put("nomeFantasiaEmpresa", "Fantasia comprovada");
        long id = enviar(c.localA, "/api/contratos", req, 201,
                "Rascunho com identidade coerente e nomes documentais próprios").path("id").asLong();
        enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201,
                "Composição necessária à publicação");
        var publicacao = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-EMPRESA-HISTORICA",
                "justificativa", "Publicação sintética com fotografia empresarial explícita");
        var previa = enviar(c.localA, base(id) + "/previas/publicar", publicacao, 200,
                "Prévia confirma documento publicável");
        publicacao.put("tokenPrevia", previa.path("tokenPrevia").asText());
        publicacao.put("confirmarImpacto", true);
        enviar(c.localA, base(id) + "/publicar", publicacao, 200, "GOV-003: publicação cria fato imutável");
        JsonNode antes = estadoContrato(id);
        String cnpjPosterior = cnpjSintetico();
        executar(c.localA, put(F + "/" + fornecedorId).contentType(APPLICATION_JSON).content(json(
                mapa("razaoSocial", "Razão alterada no mestre", "cnpj", cnpjPosterior,
                        "email", "atual@example.test", "ativo", false))), 200,
                "Alteração do cadastro atual não reinterpreta uma fotografia histórica");
        assertThat(estadoFornecedor(fornecedorId).path("cnpj").asText()).isEqualTo(cnpjPosterior);
        assertThat(estadoContrato(id)).isEqualTo(antes);
        var posicao = consultar(c.localA, base(id) + "/posicao-temporal?data=" + req.get("dataInicio"),
                "Fato publicado permanece legível mesmo se o mestre atual mudou");
        var versao = posicao.path("versaoContratual");
        assertThat(versao.path("cnpjEmpresa").asText()).isEqualTo(fornecedor.path("cnpj").asText());
        assertThat(versao.path("razaoSocialEmpresa").asText()).isEqualTo("Razão comprovada no documento");
        executar(c.localA, put(base(id) + "/fornecedor/" + fornecedorId), 409,
                "GOV-003: a nova validação de rascunho não libera alteração direta de publicado");
        assertThat(estadoContrato(id)).isEqualTo(antes);
        evidencia("historico-publicado", mapa("antes", antes, "depois", estadoContrato(id), "posicao", posicao));
    }

    @Test
    void criacaoDeEmpresaAuditaAutorContextoEEstadoPersistidoDepoisDoCommit() throws Exception {
        var c = criarCenarioPersistido();
        String requestId = UUID.randomUUID().toString();
        String cnpj = cnpjSintetico();
        Instant inicio = Instant.now();
        var criado = dados(executar(c.localA, post(F).header("X-Request-Id", requestId)
                        .contentType(APPLICATION_JSON).content(json(mapa("razaoSocial", "  Razão   auditada  ",
                                "cnpj", formatado(cnpj), "email", " criado@example.test "))), 200,
                "AUD-001: cadastro confirmado deve registrar quem criou e o estado normalizado"));
        long id = criado.path("id").asLong();
        JsonNode persistido = estadoFornecedor(id);
        assertThat(persistido.path("cnpj").asText()).isEqualTo(cnpj);
        var auditoria = exigirAuditoria(requestId, "FORNECEDOR_CRIADO");
        conferirContexto(auditoria, c.localA, "Fornecedor", id, "POST", F, inicio);
        JsonNode detalhes = mapper.readTree(auditoria.getDetailsJson());
        assertThat(detalhes.has("antes")).isTrue();
        assertThat(detalhes.path("antes").isNull()).isTrue();
        assertThat(detalhes.path("depois")).isEqualTo(mapper.readTree(json(persistido)));
    }

    @Test
    void atualizacaoDeEmpresaAuditaAntesEDepoisSemAlterarRegistroAnterior() throws Exception {
        var c = criarCenarioPersistido();
        var criado = novaEmpresa(c, "Razão anterior auditável");
        long id = criado.path("id").asLong();
        JsonNode antes = estadoFornecedor(id);
        fila.descarregar();
        var registrosAntes = auditoriasDaEntidade("Fornecedor", id);
        String requestId = UUID.randomUUID().toString();
        Instant inicio = Instant.now();
        executar(c.localA, put(F + "/" + id).header("X-Request-Id", requestId)
                        .contentType(APPLICATION_JSON).content(json(mapa("razaoSocial", " Razão   depois ",
                                "cnpj", cnpjSintetico(), "email", " depois@example.test ", "ativo", false))), 200,
                "AUD-001: atualização deve guardar o estado anterior e o novo, com autoria");
        JsonNode depois = estadoFornecedor(id);
        assertThat(depois).isNotEqualTo(antes);
        var auditoria = exigirAuditoria(requestId, "FORNECEDOR_ATUALIZADO");
        conferirContexto(auditoria, c.localA, "Fornecedor", id, "PUT", F + "/" + id, inicio);
        JsonNode detalhes = mapper.readTree(auditoria.getDetailsJson());
        assertThat(detalhes.path("antes")).isEqualTo(mapper.readTree(json(antes)));
        assertThat(detalhes.path("depois")).isEqualTo(mapper.readTree(json(depois)));
        assertThat(auditoriasDaEntidade("Fornecedor", id)).containsAll(registrosAntes);
    }

    @Test
    void vinculoDeRascunhoAuditaIdentidadesAntesEDepoisComAutoria() throws Exception {
        var c = criarCenarioPersistido();
        var fornecedor = novaEmpresa(c, "Empresa coerente para vínculo auditado");
        long fornecedorId = fornecedor.path("id").asLong();
        var req = pedidoContrato(c, "VINCULO-AUDITORIA", fornecedor.path("cnpj").asText());
        long id = enviar(c.localA, "/api/contratos", req, 201,
                "Rascunho documenta empresa sem FK ainda selecionada").path("id").asLong();
        JsonNode antes = identidadeContrato(id);
        assertThat(antes.path("fornecedorId").isNull()).isTrue();
        String requestId = UUID.randomUUID().toString();
        String path = base(id) + "/fornecedor/" + fornecedorId;
        Instant inicio = Instant.now();
        executar(c.localA, put(path).header("X-Request-Id", requestId), 200,
                "AUD-001: vínculo permitido do rascunho também exige trilha de autoria");
        JsonNode depois = identidadeContrato(id);
        assertThat(depois.path("fornecedorId").asLong()).isEqualTo(fornecedorId);
        var auditoria = exigirAuditoria(requestId, "CONTRATO_FORNECEDOR_VINCULADO");
        conferirContexto(auditoria, c.localA, "Contrato", id, "PUT", path, inicio);
        JsonNode detalhes = mapper.readTree(auditoria.getDetailsJson());
        assertThat(detalhes.path("antes")).isEqualTo(mapper.readTree(json(antes)));
        assertThat(detalhes.path("depois")).isEqualTo(mapper.readTree(json(depois)));
    }

    @Test
    void rollbackRealNaoPublicaAuditoriaDeAtualizacaoQueNaoFoiConfirmada() throws Exception {
        var c = criarCenarioPersistido();
        var criado = novaEmpresa(c, "Empresa de controle do rollback");
        long id = criado.path("id").asLong();
        JsonNode antes = estadoFornecedor(id);
        // A sessão existe antes da transação que será deliberadamente desfeita.
        executar(c.localA, get(F), 200, "Controle positivo de sessão autenticada persistida");
        String requestId = UUID.randomUUID().toString();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            try {
                executar(c.localA, put(F + "/" + id).header("X-Request-Id", requestId)
                                .contentType(APPLICATION_JSON).content(json(mapa("razaoSocial", "Estado provisório",
                                        "cnpj", antes.path("cnpj").asText(), "email", "rollback@example.test"))), 200,
                        "Comando executado na transação real ainda não confirmada");
                em.flush();
                assertThat(em.find(Fornecedor.class, id).getRazaoSocial()).isEqualTo("Estado provisório");
            } catch (Exception erro) {
                throw new IllegalStateException(erro);
            } finally {
                status.setRollbackOnly();
            }
        });
        fila.descarregar();
        assertThat(estadoFornecedor(id)).isEqualTo(antes);
        assertThat(auditorias(requestId)).as("afterCommit não pode anunciar uma mudança revertida").isEmpty();
        evidencia("rollback-sem-evento", mapa("antes", antes, "depois", estadoFornecedor(id),
                "auditoriasDaRequisicao", auditorias(requestId)));
    }

    private JsonNode novaEmpresa(Cenario c, String razao) throws Exception {
        return enviar(c.localA, F, mapa("razaoSocial", razao, "cnpj", cnpjSintetico(),
                "email", "sintetico@example.test"), 200, "Cadastro sintético compartilhado próprio do cenário");
    }

    private Map<String, Object> pedidoContrato(Cenario c, String prefixo, String cnpj) {
        var inicio = RelogioSistema.hoje().plusDays(10);
        var req = rascunho(c, prefixo + "-" + UUID.randomUUID().toString().substring(0, 8),
                5, inicio, inicio.plusDays(60));
        req.put("cnpjEmpresa", cnpj);
        return req;
    }

    private JsonNode estadoFornecedor(long id) {
        return emTransacao(() -> {
            em.clear();
            var f = em.find(Fornecedor.class, id);
            assertThat(f).isNotNull();
            return mapper.valueToTree(mapa("id", f.getId(), "razaoSocial", f.getRazaoSocial(),
                    "cnpj", f.getCnpj(), "email", f.getEmail(), "ativo", f.getAtivo()));
        });
    }

    private JsonNode identidadeContrato(long id) {
        return emTransacao(() -> {
            em.clear();
            return mapper.valueToTree(identidade(em.find(Contrato.class, id)));
        });
    }

    private Map<String, Object> identidade(Contrato c) {
        return mapa("fornecedorId", c.getFornecedor() == null ? null : c.getFornecedor().getId(),
                "cnpjEmpresa", c.getCnpjEmpresa(), "razaoSocialEmpresa", c.getRazaoSocialEmpresa(),
                "nomeFantasiaEmpresa", c.getNomeFantasiaEmpresa());
    }

    private JsonNode estadoContrato(long id) {
        return emTransacao(() -> {
            em.clear();
            var c = em.find(Contrato.class, id);
            var estado = identidade(c);
            estado.putAll(mapa("id", c.getId(), "version", c.getVersion(), "estado", c.getEstadoNegocial(),
                    "numero", c.getNumeroContrato(), "descricao", c.getDescricao(),
                    "gestaoLocalId", c.getGestaoLocal().getId(), "inicio", c.getDataInicio(), "fim", c.getDataFim(),
                    "fotografias", em.createQuery("select v.id,v.numeroVersao,v.fornecedorId,v.cnpjEmpresa,"
                                    + "v.razaoSocialEmpresa,v.nomeFantasiaEmpresa from VersaoContratoNegocial v "
                                    + "where v.contrato.id=:id order by v.id", Object[].class)
                            .setParameter("id", id).getResultList(),
                    "eventos", em.createQuery("select e.id from EventoCicloVidaContrato e where e.contrato.id=:id "
                                    + "order by e.id", Long.class).setParameter("id", id).getResultList()));
            return mapper.valueToTree(estado);
        });
    }

    private long quantidadeContratos() {
        return emTransacao(() -> em.createQuery("select count(c) from Contrato c", Long.class).getSingleResult());
    }

    private List<Long> contratosComNumero(Map<String, Object> req) {
        return emTransacao(() -> em.createQuery("select c.id from Contrato c where c.numeroContrato=:numero", Long.class)
                .setParameter("numero", req.get("numeroContrato").toString().toUpperCase(Locale.ROOT)).getResultList());
    }

    private List<RegistroAuditoria> auditorias(String requestId) {
        return emTransacao(() -> em.createQuery("select a from RegistroAuditoria a where a.requestId=:rid "
                        + "order by a.id", RegistroAuditoria.class).setParameter("rid", requestId).getResultList());
    }

    private List<JsonNode> auditoriasDaEntidade(String tipo, long id) {
        return emTransacao(() -> em.createQuery("select a from RegistroAuditoria a where a.entityType=:tipo "
                        + "and a.entityId=:id order by a.id", RegistroAuditoria.class)
                .setParameter("tipo", tipo).setParameter("id", id).getResultList().stream()
                .map(a -> mapper.<JsonNode>valueToTree(a)).toList());
    }

    private RegistroAuditoria exigirAuditoria(String requestId, String acao) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        List<RegistroAuditoria> registros;
        do {
            fila.descarregar();
            registros = auditorias(requestId);
            if (!registros.isEmpty()) break;
            TimeUnit.MILLISECONDS.sleep(20);
        } while (System.nanoTime() < limite);
        evidencia("auditoria-" + acao, mapa("requestId", requestId, "encontrados", registros));
        assertThat(registros).as("AUD-001: exatamente um evento confirmado para " + acao).hasSize(1);
        assertThat(registros.get(0).getAction()).isEqualTo(acao);
        return registros.get(0);
    }

    private void conferirContexto(RegistroAuditoria a, UsuarioAplicacao usuario, String tipo,
                                  long id, String metodo, String path, Instant inicio) {
        assertThat(a.getUserId()).isEqualTo(usuario.getId());
        assertThat(a.getUsername()).isEqualTo(usuario.getUsername());
        assertThat(a.getEntityType()).isEqualTo(tipo);
        assertThat(a.getEntityId()).isEqualTo(id);
        assertThat(a.getCreatedAt()).isBetween(inicio.minusSeconds(1), Instant.now().plusSeconds(1));
        assertThat(a.getRequestId()).isNotBlank();
        assertThat(a.getHttpMethod()).isEqualTo(metodo);
        assertThat(a.getPath()).isEqualTo(path);
    }

    private void evidencia(String etapa, Object dados) throws Exception {
        Path path = Path.of("target/auditoria-http/fornecedor-integridade-auditoria.jsonl");
        Files.createDirectories(path.getParent());
        Files.writeString(path, mapper.writeValueAsString(mapa("caso", caso, "etapa", etapa,
                "registradoEm", Instant.now(), "dados", dados)) + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String cnpjSintetico() {
        return String.format(Locale.ROOT, "%014d",
                Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000_000L));
    }

    private static String formatado(String cnpj) {
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8)
                + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }
}
