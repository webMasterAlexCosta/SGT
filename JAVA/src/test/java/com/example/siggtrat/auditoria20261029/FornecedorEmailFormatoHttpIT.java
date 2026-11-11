package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Fornecedor;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** M01: validação no servidor real, commits e fotografias integrais após recusa. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FornecedorEmailFormatoHttpIT extends ContratosHttpBase {
    private static final String API = "/api/fornecedores";
    @Autowired DataSource fonte;
    @Autowired FilaRegistroAuditoria fila;

    static Stream<String> emailsInvalidos() {
        return Stream.of("sem-arroba", "contato@", "cadastro@" + "a".repeat(60)
                + "." + "b".repeat(60) + "." + "c".repeat(60) + ".test");
    }

    @ParameterizedTest @MethodSource("emailsInvalidos")
    void criacaoRecusaFormatoOuLimiteSemCadastroOuAuditoriaParcial(String email) throws Exception {
        var c = criarCenarioPersistido();
        executar(c.localA, get(API), 200, "Sessão confirmada antes do snapshot");
        conferirRecusa(c, post(API), pedido(cnpj(), email), "criacao");
    }

    @ParameterizedTest @MethodSource("emailsInvalidos")
    void atualizacaoRecusaSemAlterarEmpresaVersaoContratosOuAuditoria(String email) throws Exception {
        var c = criarCenarioPersistido();
        String identidade = cnpj();
        long id = enviar(c.localA, API, pedido(identidade, "anterior@example.test"), 200,
                "Controle válido antes da atualização recusada").path("id").asLong();
        var req = pedido(identidade, email);
        req.put("razaoSocial", "Nome que não pode persistir");
        req.put("ativo", false);
        conferirRecusa(c, put(API + "/" + id), req, "atualizacao");
    }

    private void conferirRecusa(Cenario c, MockHttpServletRequestBuilder comando,
                                Map<String, Object> pedido, String operacao) throws Exception {
        fila.descarregar();
        var antes = snapshot();
        var resposta = mvc.perform(autenticar(comando.contentType(APPLICATION_JSON).content(json(pedido)), c.localA)).andReturn();
        fila.descarregar();
        var depois = snapshot();
        var corpo = mapper.readTree(resposta.getResponse().getContentAsString());
        SnapshotQuantitativos.registrar(mapper, "fornecedor-email", operacao,
                mapa("antes", antes, "depois", depois, "http", SnapshotQuantitativos.http(mapper, resposta)));
        assertAll(
                () -> assertThat(resposta.getResponse().getStatus()).isEqualTo(400),
                () -> assertThat(corpo.path("code").asText()).isEqualTo("INVALID_ARGUMENT"),
                () -> assertThat(corpo.path("message").asText()).containsIgnoringCase("parâmetro").containsIgnoringCase("formatos"),
                () -> assertThat(depois).isEqualTo(antes));
    }

    @Test void emailNormalizadoEOpcionaisPermitemInativarEReativarSemPerderIdentidadeOuHistoria() throws Exception {
        var c = criarCenarioPersistido();
        String identidade = cnpj();
        JsonNode criado = enviar(c.localA, API, pedido(identidade, " cadastro+operacao@example.test "), 200,
                "E-mail válido conserva normalização de espaços");
        long id = criado.path("id").asLong();
        assertThat(emailPersistido(id)).isEqualTo("cadastro+operacao@example.test");
        var inicio = RelogioSistema.hoje().plusDays(2);
        var contratoReq = rascunho(c, "HIST-FORN-" + UUID.randomUUID(), 5, inicio, inicio.plusDays(40));
        contratoReq.put("fornecedorId", id);
        contratoReq.put("cnpjEmpresa", identidade);
        contratoReq.put("razaoSocialEmpresa", "Empresa sintética M01");
        long contrato = enviar(c.localA, "/api/contratos", contratoReq, 201, "Contrato vinculado ao fornecedor real").path("id").asLong();
        enviar(c.localA, base(contrato) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201, "Composição legítima");
        var publicacao = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-M01-HIST",
                "justificativa", "Publicação sintética para preservar história na reativação");
        publicacao.put("tokenPrevia", enviar(c.localA, base(contrato) + "/previas/publicar", publicacao, 200,
                "Prévia legítima da publicação").path("tokenPrevia").asText());
        publicacao.put("confirmarImpacto", true);
        enviar(c.localA, base(contrato) + "/publicar", publicacao, 200, "Fotografia publicada antes da inativação");
        fila.descarregar();
        var inicial = snapshot();
        var req = pedido(identidade, null);
        req.put("ativo", false);
        executar(c.localA, put(API + "/" + id).contentType(APPLICATION_JSON).content(json(req)), 200, "Inativação legítima");
        assertThat(emailPersistido(id)).isNull();
        assertThat(ativoPersistido(id)).isFalse();
        fila.descarregar();
        var inativo = snapshot();
        req.put("email", "   ");
        req.put("ativo", true);
        executar(c.localA, put(API + "/" + id).contentType(APPLICATION_JSON).content(json(req)), 200, "Reativação do mesmo ID");
        assertThat(emailPersistido(id)).isEmpty();
        assertThat(ativoPersistido(id)).isTrue();
        fila.descarregar();
        var reativado = snapshot();
        for (String campo : java.util.List.of("contratos", "fotografias", "eventos")) {
            assertThat(inativo.get(campo)).isEqualTo(inicial.get(campo));
            assertThat(reativado.get(campo)).isEqualTo(inicial.get(campo));
        }
        var auditorias = emTransacao(() -> em.createQuery("select a.action from RegistroAuditoria a "
                + "where a.entityType='Fornecedor' and a.entityId=:id order by a.id", String.class)
                .setParameter("id", id).getResultList());
        assertThat(auditorias).containsExactly("FORNECEDOR_CRIADO", "FORNECEDOR_ATUALIZADO", "FORNECEDOR_ATUALIZADO");
        SnapshotQuantitativos.registrar(mapper, "fornecedor-email", "opcionais-reativacao",
                mapa("inicial", inicial, "inativo", inativo, "reativado", reativado, "acoes", auditorias));
    }

    @Test void respostaDeCadastroDescartadaRecuperaEmpresaSemDuplicarERetomaSomenteAssociacao() throws Exception {
        var c = criarCenarioPersistido();
        String identidade = cnpj();
        var req = pedido(identidade, "cadastro@example.test");
        // O cliente não usa o corpo da primeira resposta; não é alegação de corte TCP/Chrome.
        var descartada = mvc.perform(autenticar(post(API).contentType(APPLICATION_JSON).content(json(req)), c.localA)).andReturn();
        assertThat(descartada.getResponse().getStatus()).isEqualTo(200);
        fila.descarregar();
        var antesRepeticao = snapshot();
        var repetida = executar(c.localA, post(API).contentType(APPLICATION_JSON).content(json(req)), 409,
                "Repetir intenção não cria outra empresa");
        assertThat(mapper.readTree(repetida.getResponse().getContentAsString()).path("code").asText()).isEqualTo("CNPJ_DUPLICADO");
        fila.descarregar();
        assertThat(snapshot()).isEqualTo(antesRepeticao);
        JsonNode catalogo = consultar(c.localA, API, "Recuperação legítima pelo catálogo");
        long id = 0;
        int encontrados = 0;
        for (JsonNode empresa : catalogo) if (empresa.path("cnpj").asText().equals(identidade)) {
            id = empresa.path("id").asLong(); encontrados++;
        }
        assertThat(encontrados).isEqualTo(1);
        var inicio = RelogioSistema.hoje().plusDays(2);
        var contratoReq = rascunho(c, "RECUP-FORN-" + UUID.randomUUID(), 5, inicio, inicio.plusDays(40));
        contratoReq.put("cnpjEmpresa", identidade);
        long contrato = enviar(c.localA, "/api/contratos", contratoReq, 201, "Rascunho próprio da associação").path("id").asLong();
        var associacao = dados(executar(c.localA, put("/api/contratos/" + contrato + "/fornecedor/" + id), 200,
                "Somente associação é retomada com ID recuperado"));
        assertThat(associacao.path("fornecedor").path("id").asLong()).isEqualTo(id);
        assertThat(emTransacao(() -> em.createQuery("select count(f) from Fornecedor f where f.cnpj=:cnpj", Long.class)
                .setParameter("cnpj", identidade).getSingleResult())).isEqualTo(1);
        SnapshotQuantitativos.registrar(mapper, "fornecedor-email", "resposta-descartada",
                mapa("antesRepeticao", antesRepeticao, "final", snapshot(), "fornecedorId", id, "contratoId", contrato,
                        "limite", "Resposta descartada pelo cliente MockMvc; sem corte de transporte ou prova de UI"));
    }

    private String emailPersistido(long id) { return emTransacao(() -> { em.clear(); return em.find(Fornecedor.class, id).getEmail(); }); }
    private boolean ativoPersistido(long id) { return emTransacao(() -> { em.clear(); return em.find(Fornecedor.class, id).getAtivo(); }); }
    private Map<String, Object> pedido(String cnpj, String email) { return mapa("razaoSocial", "Empresa sintética M01", "cnpj", cnpj, "email", email); }
    private static String cnpj() { return String.format(Locale.ROOT, "%014d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000_000L)); }

    private Map<String, String> snapshot() throws Exception {
        Map<String, String> resultado = new LinkedHashMap<>();
        try (Connection c = fonte.getConnection()) {
            assertThat(c.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            c.setReadOnly(true); c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); c.setAutoCommit(false);
            try {
                for (var tabela : Map.of("fornecedores", "fornecedor", "contratos", "contrato",
                        "fotografias", "contrato_versao_negocial", "eventos", "contrato_evento_ciclo_vida",
                        "auditoria", "registro_auditoria").entrySet()) {
                    String filtro = tabela.getKey().equals("auditoria") ? " where entity_type='Fornecedor' or action='CONTRATO_FORNECEDOR_VINCULADO'" : "";
                    // Identificadores/filtro constantes; todas as colunas e linhas, somente SELECT.
                    String sql = "select coalesce(jsonb_agg(to_jsonb(x) order by to_jsonb(x)::text),'[]'::jsonb)::text from " + tabela.getValue() + " x" + filtro;
                    try (var stmt = c.createStatement(); var r = stmt.executeQuery(sql)) {
                        assertThat(r.next()).isTrue(); resultado.put(tabela.getKey(), UtilitarioHashSeguranca.sha256Hexadecimal(r.getString(1)));
                    }
                }
                c.commit();
            } catch (Exception erro) { c.rollback(); throw erro; }
        }
        return Map.copyOf(resultado);
    }
}
