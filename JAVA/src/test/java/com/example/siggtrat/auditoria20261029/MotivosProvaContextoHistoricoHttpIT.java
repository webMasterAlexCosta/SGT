package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * RED de prova tardia da descrição/categoria de Motivo. Disponibilidade é uma
 * trilha separada: provar um texto não ativa o item, nem muda seu cadastro atual.
 * A confirmação é contratual/escopada e as provas são imutáveis por conhecimento.
 * Não executado pelo subagente; execução serial pertence ao coordenador.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class MotivosProvaContextoHistoricoHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private Environment ambiente;

    @BeforeEach
    void infraestruturaReal() throws Exception {
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
    void cadastroAtualNaoProvaDescricaoOuCategoriaDoMotivoNoPassado() throws Exception {
        Preparado p = preparar();
        JsonNode r = resolver(p, DATA, null);
        assertThat(r.path("comprovado").asBoolean()).isFalse();
        assertThat(r.path("descricao").isNull()).isTrue();
        assertThat(r.path("categoria").isNull()).isTrue();
        assertThat(r.path("fonteId").isNull()).isTrue();
        assertThat(listar(p)).isEmpty();
    }

    @Test
    void documentoTardioEnriqueceSomenteIntervaloEContratoSemModificarMestreOuDisponibilidade() throws Exception {
        Preparado p = preparar();
        assertThat(resolver(p, DATA, null).path("descricao").isNull()).isTrue();
        Mestre antes = mestre(p);
        Instant conhecidoAntes = Instant.now();
        conceder(p.c(), p.c().contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        JsonNode prova = registrar(p, requisicao(p, "Descrição comprovada no documento antigo", "FIM_VIGENCIA_CONTRATO", null));
        JsonNode r = resolver(p, DATA, null);
        assertThat(r.path("descricao").asText()).isEqualTo("Descrição comprovada no documento antigo");
        assertThat(r.path("categoria").asText()).isEqualTo("FIM_VIGENCIA_CONTRATO");
        assertThat(r.path("comprovado").asBoolean()).isTrue();
        assertThat(r.path("fonteId").asLong()).isEqualTo(prova.path("id").asLong());
        assertThat(Instant.parse(prova.path("registradoEm").asText())).isAfter(conhecidoAntes);
        assertThat(resolver(p, DATA, conhecidoAntes).path("descricao").isNull()).isTrue();
        assertThat(resolver(p, DATA.minusDays(1), null).path("descricao").isNull()).isTrue();
        assertThat(resolver(p, DATA.plusDays(3), null).path("descricao").isNull()).isTrue();
        assertThat(mestre(p)).isEqualTo(antes);
    }

    @Test
    void retificacaoExplicitaPreservaOriginalEConhecimentoAnterior() throws Exception {
        Preparado p = preparar(); resolver(p, DATA, null);
        conceder(p.c(), p.c().contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        JsonNode original = registrar(p, requisicao(p, "Primeira leitura documental", "DESLIGAMENTO_INDIVIDUAL", null));
        Instant conhecimentoPrimeira = Instant.now();
        enviar(p.c().localA, rota(p) + "/previa", requisicao(p, "Texto conflitante", "OUTRO", null),
                409, "Descrição ou categoria conflitantes não são resolvidas por data de inserção");
        JsonNode retificacao = registrar(p, requisicao(p, "Leitura documental retificada", "OUTRO", original.path("id").asLong()));
        assertThat(retificacao.path("provaAnteriorId").asLong()).isEqualTo(original.path("id").asLong());
        assertThat(resolver(p, DATA, null).path("categoria").asText()).isEqualTo("OUTRO");
        assertThat(resolver(p, DATA, conhecimentoPrimeira).path("categoria").asText()).isEqualTo("DESLIGAMENTO_INDIVIDUAL");
        assertThat(listar(p)).hasSize(2);
        assertThat(listar(p).get(0).path("descricaoComprovada").asText()).isEqualTo("Primeira leitura documental");
    }

    @Test
    void idempotenciaConservaProvaEVersoesEOutraIntencaoDaMesmaChaveFalha() throws Exception {
        Preparado p = preparar(); resolver(p, DATA, null);
        conceder(p.c(), p.c().contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        Map<String, Object> req = requisicao(p, "Texto documental idempotente", "DESLIGAMENTO_INDIVIDUAL", null);
        confirmar(p, req);
        JsonNode primeira = enviar(p.c().localA, rota(p), req, 201, "Primeira incorporação contextual");
        var vs = versoes(p);
        JsonNode repetida = enviar(p.c().localA, rota(p), req, 201, "Reenvio não consome versão ou cria outra origem");
        assertThat(repetida.path("id").asLong()).isEqualTo(primeira.path("id").asLong());
        assertThat(repetida.path("reenvio").asBoolean()).isTrue();
        assertThat(versoes(p)).isEqualTo(vs);
        req.put("categoriaComprovada", "OUTRO");
        enviar(p.c().localA, rota(p), req, 409, "Chave existente não aceita trocar categoria");
        assertThat(listar(p)).hasSize(1);
        assertThat(versoes(p)).isEqualTo(vs);
    }

    @Test
    void contratoConcessaoEFonteSaoObrigatoriosESemVazamentoEntreGestoes() throws Exception {
        Preparado p = preparar(); resolver(p, DATA, null);
        Map<String, Object> req = requisicao(p, "Descrição documentada", "DESLIGAMENTO_INDIVIDUAL", null);
        enviar(p.c().localA, rota(p) + "/previa", req, 403, "Sem concessão específica não incorpora histórico");
        enviar(p.c().nacional, rota(p) + "/previa", req, 403, "Consulta global não autoriza escrita negocial");
        enviar(p.c().localB, rota(p) + "/previa", req, 404, "Gestão alheia não conhece prova contratual privada");
        conceder(p.c(), p.c().contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        req.put("referenciaSei", null);
        enviar(p.c().localA, rota(p) + "/previa", req, 400, "Texto da justificativa não substitui prova documental");
        req.put("referenciaSei", "SEI-SINTETICO-PROVA-MOTIVO"); req.put("categoriaComprovada", null);
        enviar(p.c().localA, rota(p) + "/previa", req, 400, "Não se infere a categoria atual se a fonte não a informa");
        req.put("categoriaComprovada", "DESLIGAMENTO_INDIVIDUAL"); req.put("contratoId", p.c().contratoB.getId());
        enviar(p.c().localA, rota(p) + "/previa", req, 404, "Prova de outro Contrato/Gestão não recebe alcance global");
        assertThat(listar(p)).isEmpty();
    }

    @Test
    void gestorDestinoIncorporaProvaDaOrigemSomenteNoPeriodoHistoricoCorrespondente() throws Exception {
        Preparado p = preparar();
        resolver(p, DATA, null); // RED inicial da rota ausente precede a nova concessão.
        Mestre mestreOriginal = mestre(p);
        Map<String, Object> req = requisicao(p, "Descrição documental da Gestão de origem", "DESLIGAMENTO_INDIVIDUAL", null);
        conceder(p.c(), p.c().contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        conceder(p.c(), p.c().contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        LocalDate transferencia = DATA.plusDays(10);
        enviar(p.c().localA, base(p.c().contratoA.getId()) + "/transferir-gestao", mapa(
                "dataEfetiva", transferencia, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", p.c().gestaoB.getId(), "tipoContratacaoDestinoId", p.c().tipoB.getId(),
                "motivo", "Transferência documental para testar custódia atual da prova histórica",
                "referenciaSei", "SEI-SINTETICO-TRANSFERENCIA-PROVA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", p.c().composicaoA.getId(),
                        "lotacaoDestinoId", p.c().lotacaoB.getId(), "jornadaDestinoId", p.c().jornadaB.getId(),
                        "cargoDestinoId", p.c().cargoB.getId()))), 200,
                "Transferência muda a custódia atual sem reatribuir a Gestão do período anterior");
        enviar(p.c().admin, "/api/permissoes-excepcionais", mapa("tipo", "INCORPORACAO_PROVA_HISTORICA",
                "usuarioBeneficiarioId", p.c().localB.getId(), "contratoId", p.c().contratoA.getId(),
                "finalidade", "Incorporar fonte recebida após transferência de custódia",
                "justificativa", "Documento delimita a causa no período da Gestão de origem",
                "autorizacaoAdministrativa", "ATO-SINTETICO-CUSTODIA", "validoAte", Instant.now().plusSeconds(7200)), 201,
                "Gestor atualmente responsável recebe concessão específica do Contrato");
        req.put("versaoContratoEsperada", consultar(p.c().localB, base(p.c().contratoA.getId()), "Versão do Contrato transferido")
                .path("version").asLong());
        enviar(p.c().localA, rota(p) + "/previa", req, 404,
                "Concessão antiga e propriedade do catálogo não substituem a custódia atual do Contrato");
        JsonNode previa = enviar(p.c().localB, rota(p) + "/previa", req, 200,
                "Custódio atual pode documentar causa pertencente à Gestão comprovada no período anterior");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(p.c().localB, rota(p), req, 201, "Fonte antiga é incorporada sem editar o mestre da Gestão de origem");
        JsonNode antesTransferencia = dados(executar(p.c().localB,
                get("/api/motivos-desligamento/" + p.motivoId() + "/contexto-historico")
                        .param("contratoId", p.c().contratoA.getId().toString()).param("data", DATA.toString()),
                200, "Consulta histórica respeita fonte anterior à transferência"));
        assertThat(antesTransferencia.path("descricao").asText()).isEqualTo("Descrição documental da Gestão de origem");
        Map<String, Object> foraDoPeriodo = new LinkedHashMap<>(req);
        foraDoPeriodo.put("dataInicioEfetiva", transferencia); foraDoPeriodo.put("dataFimEfetiva", transferencia.plusDays(2));
        foraDoPeriodo.put("chaveIdempotencia", UUID.randomUUID().toString());
        foraDoPeriodo.put("versaoMotivoEsperada", emTransacao(() -> em.find(MotivoDesligamento.class, p.motivoId()).getVersion()));
        foraDoPeriodo.put("versaoContratoEsperada", consultar(p.c().localB, base(p.c().contratoA.getId()), "Versão após prova")
                .path("version").asLong());
        enviar(p.c().localB, rota(p) + "/previa", foraDoPeriodo, 409,
                "Motivo da origem não certifica o período em que o Contrato já pertence à Gestão de destino");
        JsonNode depoisTransferencia = dados(executar(p.c().localB,
                get("/api/motivos-desligamento/" + p.motivoId() + "/contexto-historico")
                        .param("contratoId", p.c().contratoA.getId().toString()).param("data", transferencia.toString()),
                200, "Fora do período documental não há valor emprestado da origem"));
        assertThat(depoisTransferencia.path("comprovado").asBoolean()).isFalse();
        assertThat(depoisTransferencia.path("descricao").isNull()).isTrue();
        assertThat(mestre(p)).isEqualTo(mestreOriginal);
        assertThat(dados(executar(p.c().localB, get(rota(p)).param("contratoId", p.c().contratoA.getId().toString()),
                200, "Tentativa incompatível não acrescentou prova"))).hasSize(1);
    }

    private Preparado preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        long id = enviar(c.localA, "/api/motivos-desligamento", mapa("descricao", "Causa atual " + UUID.randomUUID(),
                "categoria", "DESLIGAMENTO_INDIVIDUAL", "ativo", true), 201, "Catálogo atual é distinto da prova histórica")
                .path("id").asLong();
        return new Preparado(c, id);
    }

    private String rota(Preparado p) { return "/api/motivos-desligamento/" + p.motivoId() + "/provas-historicas"; }

    private JsonNode resolver(Preparado p, LocalDate data, Instant conhecidoAte) throws Exception {
        var req = get("/api/motivos-desligamento/" + p.motivoId() + "/contexto-historico")
                .param("contratoId", p.c().contratoA.getId().toString()).param("data", data.toString());
        if (conhecidoAte != null) req.param("conhecidoAte", conhecidoAte.toString());
        return dados(executar(p.c().localA, req, 200, "Contexto documental do Motivo é bitemporal e escopado"));
    }

    private JsonNode listar(Preparado p) throws Exception {
        return dados(executar(p.c().localA, get(rota(p)).param("contratoId", p.c().contratoA.getId().toString()),
                200, "Histórico imutável das provas do Motivo no Contrato"));
    }

    private Map<String, Object> requisicao(Preparado p, String descricao, String categoria, Long anterior) throws Exception {
        var vs = versoes(p);
        return mapa("contratoId", p.c().contratoA.getId(), "dataInicioEfetiva", DATA, "dataFimEfetiva", DATA.plusDays(2),
                "descricaoComprovada", descricao, "categoriaComprovada", categoria,
                "referenciaSei", "SEI-SINTETICO-PROVA-MOTIVO", "documentoComprobatorioId", null,
                "motivo", "Documento explicita descrição, categoria e intervalo da causa",
                "autorizacao", "ATO-SINTETICO-PROVA-MOTIVO", "provaAnteriorId", anterior,
                "versaoMotivoEsperada", vs.motivo(), "versaoContratoEsperada", vs.contrato(),
                "chaveIdempotencia", UUID.randomUUID().toString());
    }

    private void confirmar(Preparado p, Map<String, Object> req) throws Exception {
        JsonNode previa = enviar(p.c().localA, rota(p) + "/previa", req, 200, "Prévia de prova da causa histórica");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("tokenPrevia").asText());
    }

    private JsonNode registrar(Preparado p, Map<String, Object> req) throws Exception {
        confirmar(p, req);
        return enviar(p.c().localA, rota(p), req, 201, "Registro imutável de fonte contextual tardia");
    }

    private Versoes versoes(Preparado p) throws Exception {
        return new Versoes(consultar(p.c().localA, "/api/motivos-desligamento/" + p.motivoId(), "Versão do motivo").path("version").asLong(),
                consultar(p.c().localA, "/api/contratos/" + p.c().contratoA.getId(), "Versão do Contrato").path("version").asLong());
    }

    private Mestre mestre(Preparado p) {
        return emTransacao(() -> {
            MotivoDesligamento m = em.find(MotivoDesligamento.class, p.motivoId());
            return new Mestre(m.getDescricao(), m.getCategoria().name(), m.getAtivo(),
                    em.createQuery("select count(e) from EventoDisponibilidadeCatalogo e where e.catalogoId=:id and e.tipoCatalogo=:tipo", Long.class)
                            .setParameter("id", p.motivoId()).setParameter("tipo", TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO).getSingleResult());
        });
    }

    private record Preparado(Cenario c, long motivoId) {}
    private record Versoes(long motivo, long contrato) {}
    private record Mestre(String descricao, String categoria, Boolean ativo, long eventosDisponibilidade) {}
}
