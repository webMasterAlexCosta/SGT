package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.ProvaHistoricaContrato;
import com.example.siggtrat.entidade.ProvaMotivoDesligamentoHistorico;
import com.example.siggtrat.entidade.ProvaNomePessoaHistorico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Revisão independente C3: hipótese de diferença entre o instante recebido no
 * POST e o instante realmente confirmado no MySQL, com efeito sobre o corte K.
 * HTTP pelo dispatcher Spring/MockMvc real, autenticação/serviços/transações/JPA
 * reais; não é uma prova de transporte TCP. Não há mocks de relógio/banco.
 * Fontes SEI são explicitamente sintéticas e as fixtures são descartáveis.
 * Preparado sem execução pelo revisor. A classificação depende da execução.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato3PrecisaoConhecimentoProvasHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private Environment ambiente;
    @Autowired private MotorTemporalContratoServico motor;

    @BeforeEach
    void exigirInfraestruturaRealESchemaHibernate() throws Exception {
        try (var c = dataSource.getConnection()) {
            assertThat(c.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(c.getMetaData().getDatabaseProductVersion()).isEqualTo("8.0.45");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(ambiente.getProperty("spring.cache.type")).isEqualTo("redis");
        try (var c = redis.getConnection()) { assertThat(c.ping()).isEqualTo("PONG"); }
    }

    @Test
    void nomeReciboRelidoEReenvioDevemRepresentarMesmoInstantePersistidoEExatoCorteK() throws Exception {
        Cenario c = criarCenarioPersistido();
        String rota = "/api/pessoas/" + c.pessoaA.getId() + "/provas-nome";
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        Map<String, Object> req = mapa("vinculoId", c.vinculoA.getId(),
                "nomeComprovado", "Nome documental sintético de precisão temporal",
                "dataInicioEfetiva", DATA, "dataFimEfetiva", DATA.plusDays(2),
                "referenciaSei", "SEI-SINTETICO-C3-PRECISAO-NOME",
                "motivo", "Fonte sintética comprova o nome no intervalo declarado",
                "autorizacao", "ATO-SINTETICO-C3-PRECISAO",
                "versaoPessoaEsperada", consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(), "Versão real da Pessoa").path("version").asLong(),
                "versaoVinculoEsperada", consultar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId(), "Versão real do Vínculo").path("version").asLong(),
                "chaveIdempotencia", UUID.randomUUID().toString());
        JsonNode recibo = confirmar(c, rota, req);
        long id = recibo.path("id").asLong();
        Instant recebido = instante(recibo);
        Instant persistido = emTransacao(() -> {
            em.clear();
            return em.find(ProvaNomePessoaHistorico.class, id).getRegistradoEm();
        });
        JsonNode lista = dados(executar(c.localA, get(rota).param("vinculoId", c.vinculoA.getId().toString()),
                200, "Outra transação HTTP relê a prova nominal confirmada"));
        JsonNode relida = unica(lista, id);
        JsonNode reenvio = enviar(c.localA, rota, req, 201, "Reenvio nominal relê a origem idempotente");
        JsonNode noK = nomeEm(c, recebido);
        JsonNode antesK = nomeEm(c, recebido.minusNanos(1));
        evidenciar("nome", recibo, relida, reenvio, persistido, noK, antesK);
        SoftAssertions.assertSoftly(soft -> {
            conferirInstantes(soft, recebido, persistido, relida, reenvio);
            soft.assertThat(reenvio.path("id").asLong()).isEqualTo(id);
            soft.assertThat(reenvio.path("reenvio").asBoolean()).isTrue();
            soft.assertThat(noK.path("comprovado").asBoolean()).as("K exatamente igual ao recibo já conhece a prova nominal").isTrue();
            soft.assertThat(noK.path("fonteId").asLong()).as("Origem nominal conhecida no próprio instante devolvido").isEqualTo(id);
            soft.assertThat(noK.path("nome").asText()).isEqualTo(req.get("nomeComprovado"));
            soft.assertThat(antesK.path("comprovado").asBoolean()).as("K um nanossegundo antes do instante anunciado não conhece a fonte").isFalse();
            soft.assertThat(antesK.path("nome").isNull()).isTrue();
        });
    }

    @Test
    void motivoReciboRelidoEReenvioDevemRepresentarMesmoInstantePersistidoEExatoCorteK() throws Exception {
        Cenario c = criarCenarioPersistido();
        long motivoId = enviar(c.localA, "/api/motivos-desligamento", mapa(
                "descricao", "Cadastro sintético atual " + UUID.randomUUID(),
                "categoria", "DESLIGAMENTO_INDIVIDUAL", "ativo", true), 201,
                "Motivo atual não é usado como prova de descrição ou categoria antigas").path("id").asLong();
        String rota = "/api/motivos-desligamento/" + motivoId + "/provas-historicas";
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        Map<String, Object> req = mapa("contratoId", c.contratoA.getId(),
                "descricaoComprovada", "Descrição documental sintética da causa",
                "categoriaComprovada", "FIM_VIGENCIA_CONTRATO",
                "dataInicioEfetiva", DATA, "dataFimEfetiva", DATA.plusDays(2),
                "referenciaSei", "SEI-SINTETICO-C3-PRECISAO-MOTIVO",
                "motivo", "Fonte sintética declara descrição e categoria no intervalo",
                "autorizacao", "ATO-SINTETICO-C3-PRECISAO",
                "versaoMotivoEsperada", consultar(c.localA, "/api/motivos-desligamento/" + motivoId, "Versão real do Motivo").path("version").asLong(),
                "versaoContratoEsperada", consultar(c.localA, base(c.contratoA.getId()), "Versão real do Contrato").path("version").asLong(),
                "chaveIdempotencia", UUID.randomUUID().toString());
        JsonNode recibo = confirmar(c, rota, req);
        long id = recibo.path("id").asLong();
        Instant recebido = instante(recibo);
        Instant persistido = emTransacao(() -> {
            em.clear();
            return em.find(ProvaMotivoDesligamentoHistorico.class, id).getRegistradoEm();
        });
        JsonNode lista = dados(executar(c.localA, get(rota).param("contratoId", c.contratoA.getId().toString()),
                200, "Outra transação HTTP relê a prova contextual confirmada"));
        JsonNode relida = unica(lista, id);
        JsonNode reenvio = enviar(c.localA, rota, req, 201, "Reenvio contextual relê a origem idempotente");
        JsonNode noK = motivoEm(c, motivoId, recebido);
        JsonNode antesK = motivoEm(c, motivoId, recebido.minusNanos(1));
        evidenciar("motivo", recibo, relida, reenvio, persistido, noK, antesK);
        SoftAssertions.assertSoftly(soft -> {
            conferirInstantes(soft, recebido, persistido, relida, reenvio);
            soft.assertThat(reenvio.path("id").asLong()).isEqualTo(id);
            soft.assertThat(reenvio.path("reenvio").asBoolean()).isTrue();
            soft.assertThat(noK.path("comprovado").asBoolean()).as("K exatamente igual ao recibo já conhece a prova da causa").isTrue();
            soft.assertThat(noK.path("fonteId").asLong()).isEqualTo(id);
            soft.assertThat(noK.path("descricao").asText()).isEqualTo(req.get("descricaoComprovada"));
            soft.assertThat(noK.path("categoria").asText()).isEqualTo(req.get("categoriaComprovada"));
            soft.assertThat(antesK.path("comprovado").asBoolean()).as("K um nanossegundo antes do instante anunciado não conhece a causa").isFalse();
            soft.assertThat(antesK.path("descricao").isNull()).isTrue();
            soft.assertThat(antesK.path("categoria").isNull()).isTrue();
        });
    }

    @Test
    void controleContratoQueJaNormalizaMicrosDevePreservarReciboRelidoReenvioECorteK() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        Map<String, Object> req = fonteContratualSintetica(c, n);
        String rota = base(n.id()) + "/provas-historicas";
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");
        JsonNode recibo = confirmar(c, rota, req);
        long id = recibo.path("id").asLong();
        Instant recebido = instante(recibo);
        Instant persistido = emTransacao(() -> {
            em.clear();
            return em.find(ProvaHistoricaContrato.class, id).getRegistradoEm();
        });
        JsonNode relida = unica(consultar(c.localA, rota, "Reler prova contratual em outra transação"), id);
        JsonNode reenvio = enviar(c.localA, rota, req, 201, "Controle contratual relê a origem idempotente");
        JsonNode noK = dados(executar(c.localA, get(rota).param("conhecidoAte", recebido.toString()), 200,
                "Consulta contratual usa exatamente K devolvido pelo POST"));
        JsonNode antesK = dados(executar(c.localA, get(rota).param("conhecidoAte", recebido.minusNanos(1).toString()), 200,
                "K contratual um nanossegundo anterior não pode arredondar para incluir uma fonte posterior"));
        evidenciar("contrato_controle", recibo, relida, reenvio, persistido, noK, antesK);
        SoftAssertions.assertSoftly(soft -> {
            conferirInstantes(soft, recebido, persistido, relida, reenvio);
            soft.assertThat(reenvio.path("id").asLong()).isEqualTo(id);
            soft.assertThat(noK.isArray()).isTrue();
            soft.assertThat(noK.size()).as("Controle com instante já normalizado conhece sua própria fonte em K").isEqualTo(1);
            soft.assertThat(noK.path(0).path("id").asLong()).isEqualTo(id);
            soft.assertThat(antesK.isArray()).isTrue();
            soft.assertThat(antesK.size())
                    .as("K arbitrário anterior não inclui a prova, mesmo se Connector/J arredondar o parâmetro temporal")
                    .isZero();
        });
    }

    private JsonNode confirmar(Cenario c, String rota, Map<String, Object> req) throws Exception {
        JsonNode previa = enviar(c.localA, rota + "/previa", req, 200, "Prévia válida da fonte documental sintética");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        return enviar(c.localA, rota, req, 201, "Confirmar prova histórica pelo fluxo HTTP autenticado");
    }

    private JsonNode nomeEm(Cenario c, Instant k) throws Exception {
        return dados(executar(c.localA, get("/api/pessoas/" + c.pessoaA.getId() + "/nome-historico")
                .param("vinculoId", c.vinculoA.getId().toString()).param("data", DATA.toString())
                .param("conhecidoAte", k.toString()), 200, "Corte nominal K literal, sem arredondamento do teste"));
    }

    private JsonNode motivoEm(Cenario c, long motivoId, Instant k) throws Exception {
        return dados(executar(c.localA, get("/api/motivos-desligamento/" + motivoId + "/contexto-historico")
                .param("contratoId", c.contratoA.getId().toString()).param("data", DATA.toString())
                .param("conhecidoAte", k.toString()), 200, "Corte contextual K literal, sem arredondamento do teste"));
    }

    private Instant instante(JsonNode dto) { return Instant.parse(dto.path("registradoEm").asText()); }

    private JsonNode unica(JsonNode lista, long id) {
        assertThat(lista.isArray()).isTrue();
        assertThat(lista.size()).as("Idempotência não pode criar uma segunda prova").isEqualTo(1);
        assertThat(lista.path(0).path("id").asLong()).isEqualTo(id);
        return lista.path(0);
    }

    private void conferirInstantes(SoftAssertions soft, Instant recebido, Instant persistido, JsonNode relida, JsonNode reenvio) {
        // Equality é sobre o instante, não sobre formatação JSON. Não truncar,
        // arredondar ou aceitar tolerância aqui: o recibo deve dizer a verdade.
        soft.assertThat(recebido).as("Recibo inicial deve representar o instante realmente confirmado no MySQL")
                .isEqualTo(persistido);
        soft.assertThat(instante(relida)).as("GET deve representar o instante efetivamente persistido")
                .isEqualTo(persistido);
        soft.assertThat(instante(reenvio)).as("Reenvio idempotente deve representar o instante efetivamente persistido")
                .isEqualTo(persistido);
        soft.assertThat(instante(relida)).as("O instante do recibo não muda na primeira releitura")
                .isEqualTo(recebido);
        soft.assertThat(instante(reenvio)).as("O instante do recibo não muda no reenvio da mesma intenção")
                .isEqualTo(recebido);
    }

    private void evidenciar(String caso, JsonNode recibo, JsonNode relida, JsonNode reenvio, Instant persistido,
                            JsonNode noK, JsonNode antesK) throws Exception {
        Path destino = Path.of("target/auditoria-http/candidato3-precisao-" + caso + ".json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, json(mapa("caso", caso, "provaId", recibo.path("id").asLong(),
                "registradoEm_recibo", recibo.path("registradoEm").asText(),
                "registradoEm_mysql_jpa_nova_transacao", persistido.toString(),
                "registradoEm_get", relida.path("registradoEm").asText(),
                "registradoEm_reenvio", reenvio.path("registradoEm").asText(),
                "consulta_K_exato", noK, "consulta_K_menos_1_nanossegundo", antesK,
                "transporte", "MockMvc/dispatcher Spring real; JPA/MySQL 8.0.45 e Redis reais; não TCP")));
    }

    private Map<String, Object> fonteContratualSintetica(Cenario c, NovoContrato n) {
        return emTransacao(() -> {
            var versao = motor.fotografiaOriginalConhecida(em.find(Contrato.class, n.id()), Instant.now()).orElseThrow();
            @SuppressWarnings("unchecked")
            Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(versao), LinkedHashMap.class);
            for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado")) foto.remove(campo);
            var comp = em.find(ComposicaoContratual.class, n.composicao1());
            var item = mapa("composicaoId", comp.getId(), "lotacaoNome", comp.getLotacaoNomeFotografia(),
                    "jornadaDescricao", comp.getJornadaDescricaoFotografia(), "cargoNome", comp.getCargoNomeFotografia(),
                    "lotacaoMunicipio", comp.getLotacaoMunicipioFotografia(), "municipioComprovado", true,
                    "jornadaSemanal", comp.getJornadaSemanalFotografia(), "jornadaComprovada", true);
            return mapa("eventoOrigemId", versao.getEventoOrigem().getId(), "versaoAnteriorId", versao.getId(),
                    "dataDocumento", RelogioSistema.hoje().minusDays(40), "referenciaSei", "SEI-SINTETICO-C3-CONTROLE-CONTRATO",
                    "motivo", "Fonte integral sintética da própria publicação, usada como controle de precisão",
                    "autorizacaoAdministrativa", "ATO-SINTETICO-C3-PRECISAO", "versaoContratoEsperada", em.find(Contrato.class, n.id()).getVersion(),
                    "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", true,
                    "fotografia", foto, "composicoes", List.of(item));
        });
    }
}
