package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** CT007: apenas o relógio é controlado; fluxo HTTP, segurança e persistência permanecem reais. */
class TransferenciaGestaoFuturaAcessoHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void chegadaDaTransferenciaProgramadaTrocaAcessoDaOrigemPeloDestino(boolean consultaOrigem) throws Exception {
        var f = prepararTransferencia();
        var c = f.cenario();
        var usuario = consultaOrigem ? c.localA : c.localB;
        executar(usuario, get(base(f.contrato().id())), consultaOrigem ? 200 : 404,
                "CON-009 data futura não antecipa transferência de acesso");

        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.dataEfetiva());
            assertThat(consultar(c.nacional, base(f.contrato().id()) + "/posicao-temporal?data=" + f.dataEfetiva(),
                    "TMP-004 fotografia confirma que o fato já é efetivo")
                    .path("versaoContratual").path("gestaoLocalId").asLong()).isEqualTo(c.gestaoB.getId());
            executar(usuario, get(base(f.contrato().id())), consultaOrigem ? 404 : 200,
                    "CON-009/TMP-017 acesso segue a gestão efetiva sem exigir nova escrita ou scheduler");
        }
    }

    @Test
    void consultaComumRefleteGestaoESuperintendenciaEfetivasDepoisDaTransferenciaProgramada() throws Exception {
        var f = prepararTransferencia();
        var c = f.cenario();
        assertThat(consultar(c.nacional, base(f.contrato().id()), "CON-023 gestão original antes do efeito")
                .path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());

        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.dataEfetiva());
            var atual = consultar(c.nacional, base(f.contrato().id()), "CON-023/HIS-005 consulta comum do contexto efetivo");
            assertThat(atual.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoB.getId());
            assertThat(atual.path("gestaoLocalNome").asText()).isEqualTo(c.gestaoB.getNome());
            assertThat(atual.path("superintendenciaId").asLong()).isEqualTo(c.seB.getId());
            assertThat(consultar(c.nacional, base(f.contrato().id()) + "/posicao-temporal?data=" + f.dataEfetiva().minusDays(1),
                    "HIS-004 transferência futura não reescreve a responsabilidade histórica")
                    .path("versaoContratual").path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        }
    }

    @Test
    void listasEOpcoesPassamParaEscopoDestinoNaDataEfetivaDaTransferencia() throws Exception {
        var f = prepararTransferencia();
        var c = f.cenario();
        long id = f.contrato().id();
        assertThat(ids(consultar(c.localA, "/api/contratos", "CON-009 lista antes da transferência"))).contains(id);
        assertThat(ids(consultar(c.localB, "/api/contratos", "CON-009 lista de destino antes do efeito"))).doesNotContain(id);

        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.dataEfetiva());
            assertThat(ids(consultar(c.localB, "/api/contratos", "CON-009 listagem inclui contrato sob responsabilidade efetiva")))
                    .contains(id);
            assertThat(ids(consultar(c.localA, "/api/contratos", "CON-009 lista não expõe responsabilidade já transferida")))
                    .doesNotContain(id);
            assertThat(ids(consultar(c.localB, "/api/contratos/opcoes", "CON-009 seletor usa o mesmo escopo temporal")))
                    .contains(id);
            assertThat(ids(consultar(c.localA, "/api/contratos/opcoes", "CON-009 seletor da origem deixa de expor o contrato")))
                    .doesNotContain(id);
        }
    }

    @Test
    void aditamentoPosteriorConhecidoAntesDaTransferenciaNaoReverteResponsabilidade() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);
        LocalDate data = RelogioSistema.hoje().plusDays(5);
        var posterior = aditamento("POSTERIOR-CONHECIDO-ANTES-DA-TRANSFERENCIA", data.plusDays(1));
        posterior.put("novaDescricao", "Descrição posterior preservada com a gestão correta");
        registrarAditamento(c, n.id(), posterior);
        programar(c, n, data);

        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(data.plusDays(2));
            var atual = consultar(c.localB, base(n.id()), "TMP-008/TMP-012 responsabilidade deriva do delta de transferência");
            assertThat(atual.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoB.getId());
            assertThat(atual.path("descricao").asText()).isEqualTo("Descrição posterior preservada com a gestão correta");
            executar(c.localA, get(base(n.id())), 404,
                    "CON-009 fotografia integral antiga do ADI não restaura acesso ao gestor de origem");
        }
    }

    @Test
    void gestorDestinoRegistraAditamentoComCatalogosDaGestaoEfetiva() throws Exception {
        var f = prepararTransferencia();
        var c = f.cenario();
        var n = f.contrato();
        LocalDate data = f.dataEfetiva().plusDays(1);
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(data);
            var req = aditamento("ADI-GESTOR-DESTINO-EFETIVO", data);
            req.put("novoEfetivoTotal", 110);
            req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 110)));
            var previa = enviar(c.localB, base(n.id()) + "/aditamentos/previa", req, 200,
                    "CON-023/ADI-003 catálogos são validados na gestão efetiva do ato");
            assertThat(previa.path("efetivoAnterior").asInt()).isEqualTo(100);
            req.put("tokenPrevia", previa.path("tokenPrevia").asText());
            enviar(c.localB, base(n.id()) + "/aditamentos", req, 201,
                    "CON-009 escrita do gestor atual não depende da atualização materializada da gestão");
            var composicoes = consultar(c.localB, base(n.id()) + "/composicoes?data=" + data,
                    "CMP-001 composição alterada mantém contexto do destino");
            assertThat(composicoes).hasSize(1);
            assertThat(composicoes.get(0).path("lotacaoId").asLong()).isEqualTo(c.lotacaoB.getId());
            assertThat(composicoes.get(0).path("cargoId").asLong()).isEqualTo(c.cargoB.getId());
            assertThat(composicoes.get(0).path("efetivoPrevisto").asInt()).isEqualTo(110);
        }
    }

    private Preparado prepararTransferencia() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);
        LocalDate data = RelogioSistema.hoje().plusDays(5);
        programar(c, n, data);
        return new Preparado(c, n, data);
    }

    private void programar(Cenario c, NovoContrato n, LocalDate data) throws Exception {
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        var req = mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Responsabilidade administrativa futura documentada",
                "referenciaSei", "SEI-TRANSFERENCIA-FUTURA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId())));
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200,
                "CON-013/CON-023 transferência futura publicada com autorização e mapeamento explícitos");
        em.flush();
        em.clear();
    }

    private List<Long> ids(JsonNode itens) {
        List<Long> ids = new ArrayList<>();
        itens.forEach(item -> ids.add(item.path("id").asLong()));
        return ids;
    }

    private record Preparado(Cenario cenario, NovoContrato contrato, LocalDate dataEfetiva) {}
}
