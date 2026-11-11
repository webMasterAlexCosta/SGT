package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class ContratosCrudHttpIT extends ContratosHttpBase {
    @Test
    void rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador() throws Exception {
        var c = criarCenario();
        var inicio = RelogioSistema.hoje().plusDays(5);
        var fim = inicio.plusMonths(2);
        String numero = "RASC-" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode vazio = enviar(c.localA, "/api/contratos", mapa(), 201, "GOV-004/CON-008 rascunho incompleto");
        long id = vazio.path("id").asLong();
        assertThat(vazio.path("estadoNegocial").asText()).isEqualTo("RASCUNHO");
        var body = rascunho(c, numero, 10, inicio, fim);
        JsonNode editado = dados(executar(c.localA, put(base(id)).contentType(MediaType.APPLICATION_JSON).content(json(body)),
                200, "CON-002 número/ano do rascunho editáveis"));
        assertThat(editado.path("numeroContrato").asText()).isEqualTo(numero.toUpperCase(java.util.Locale.ROOT));
        assertThat(editado.path("anoContrato").asInt()).isEqualTo(inicio.getYear());
        var detalhe=consultar(c.localA,base(id),"CON-009:001 leitura do próprio contrato completa o CRUD autorizado");
        assertThat(detalhe.path("id").asLong()).isEqualTo(id);
        assertThat(detalhe.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(detalhe.path("numeroContrato").asText()).isEqualTo(numero.toUpperCase(java.util.Locale.ROOT));
        JsonNode comp = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 10), 201,
                "CMP-001 composição explícita no rascunho");
        long compId = comp.path("id").asLong();
        enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 10), 409, "CMP-002 duplicidade bloqueada");
        JsonNode alterada = dados(executar(c.localA, put(base(id) + "/composicoes/" + compId)
                .contentType(MediaType.APPLICATION_JSON).content(json(composicao(c, c.cargoA.getId(), 9))),
                200, "CON-008 composição de preparação editável"));
        assertThat(alterada.path("efetivoPrevisto").asInt()).isEqualTo(9);
        assertThat(consultar(c.localA, base(id) + "/composicoes", "CMP-001 não cria produto cartesiano").size()).isEqualTo(1);
        executar(c.localA, delete(base(id) + "/composicoes/" + compId), 204, "GOV-004 remove composição exclusiva");
        executar(c.localA, delete(base(id)), 204, "GOV-004 descarte de rascunho nunca publicado");
        executar(c.localA, get(base(id)), 404, "GOV-004 descarte persistido");
        JsonNode reutilizado = enviar(c.localA, "/api/contratos", body, 201, "CON-004 identificador liberado pelo descarte");
        assertThat(reutilizado.path("id").asLong()).isNotEqualTo(id);
    }

    @Test
    void publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre() throws Exception {
        var c = criarCenario();
        var inicio = RelogioSistema.hoje().plusDays(2);
        var body = rascunho(c, "PUB-" + UUID.randomUUID().toString().substring(0, 8), 10, inicio, inicio.plusDays(60));
        body.remove("nomeFantasiaEmpresa");
        long id = enviar(c.localA, "/api/contratos", body, 201, "CON-008 rascunho sem empresa completa").path("id").asLong();
        var publicar = mapa("referenciaSei", "SEI-PUB", "dataPublicacao", RelogioSistema.hoje());
        enviar(c.localA, base(id) + "/publicar", publicar, 400, "EMP-001 nome fantasia obrigatório");
        body.put("nomeFantasiaEmpresa", "Original");
        executar(c.localA, put(base(id)).contentType(MediaType.APPLICATION_JSON).content(json(body)), 200, "EMP-001 completa empresa antes da publicação");
        long comp = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 9), 201, "CMP-003 preparação soma divergente").path("id").asLong();
        enviar(c.localA, base(id) + "/publicar", publicar, 400, "CMP-003 publicação bloqueia soma divergente");
        executar(c.localA, put(base(id) + "/composicoes/" + comp).contentType(MediaType.APPLICATION_JSON)
                .content(json(composicao(c, c.cargoA.getId(), 10))), 200, "CMP-003 corrige composição no rascunho");
        enviar(c.localA, base(id) + "/publicar", mapa(), 400, "DOC-001 publicação sem fonte bloqueada");
        JsonNode pub = enviar(c.localA, base(id) + "/publicar", publicar, 200, "CON-006/008/017 contrato completo publicado");
        assertThat(pub.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(pub.path("situacaoTemporal").asText()).isEqualTo("FUTURO");
        assertThat(pub.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        assertThat(pub.path("efetivoOriginalPublicado").asInt()).isEqualTo(10);
        assertThat(pub.path("publicadoPorId").asLong()).isEqualTo(c.localA.getId());
        assertThat(pub.path("publicadoEm").asText()).isNotBlank();
        executar(c.localA, put(base(id)).contentType(MediaType.APPLICATION_JSON).content(json(body)), 409, "CON-003 publicado imutável no fluxo comum");
        executar(c.localA, delete(base(id)), 409, "GOV-003 publicado não é apagado");
        executar(c.localA, delete(base(id) + "/composicoes/" + comp), 409, "GOV-003 composição publicada preservada");
        enviar(c.localA, "/api/contratos", body, 409, "CON-001 unicidade número+ano global");
        var versoes = consultar(c.localA, base(id) + "/versoes", "CON-008 fotografia integral original");
        assertThat(versoes.size()).isEqualTo(1);
        assertThat(versoes.get(0).path("cnpjEmpresa").asText()).isEqualTo("12345678000190");
        assertThat(versoes.get(0).path("efetivoPrevisto").asInt()).isEqualTo(10);
        var eventos = consultar(c.localA, base(id) + "/eventos", "CON-017 data efetiva separada do registro");
        assertThat(eventos.get(0).path("dataEfetiva").asText()).isEqualTo(inicio.toString());
        assertThat(consultar(c.localA, base(id) + "/eventos/precedencias", "linha original sem precedências artificiais").isArray()).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void leituraEEscritaRespeitamTodosOsPerfisEEscopos() throws Exception {
        var c = criarCenarioPersistido();
        for (var leitor : List.of(c.localA, c.estadualA, c.nacional, c.presidente, c.admin, c.operacionalA)) {
            JsonNode detalhe = consultar(leitor, base(c.contratoA.getId()), "CON-009 leitura no escopo " + leitor.getPerfil());
            assertThat(detalhe.path("id").asLong()).isEqualTo(c.contratoA.getId());
            assertThat(detalhe.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
            assertThat(detalhe.has("cpf")).isFalse();
        }
        for (var fora : List.of(c.localB, c.estadualB, c.operacionalB))
            executar(fora, get(base(c.contratoA.getId())), 404, "CON-009 recurso fora do escopo não exposto");
        for (var leitor : List.of(c.estadualA, c.nacional, c.presidente, c.admin, c.operacionalA))
            executar(leitor, post("/api/contratos").contentType(MediaType.APPLICATION_JSON).content("{}"), 403,
                    "PER-001/002/003/004/005 sem escrita negocial " + leitor.getPerfil());
        executar(null, get("/api/contratos"), 401, "consulta contratual exige autenticação");
        JsonNode local = consultar(c.localA, "/api/contratos", "CON-009 listagem local filtrada");
        assertThat(ids(local)).contains(c.contratoA.getId()).doesNotContain(c.contratoB.getId());
        JsonNode estadual = consultar(c.estadualA, "/api/contratos", "CON-009 listagem estadual filtrada");
        assertThat(ids(estadual)).contains(c.contratoA.getId()).doesNotContain(c.contratoB.getId());
        JsonNode nacional = consultar(c.nacional, "/api/contratos", "consulta nacional");
        assertThat(ids(nacional)).contains(c.contratoA.getId(), c.contratoB.getId());
        JsonNode opcoes = consultar(c.localA, "/api/contratos/opcoes", "opções refletem ocupação canônica");
        assertThat(ids(opcoes)).contains(c.contratoA.getId()).doesNotContain(c.contratoB.getId());
        var comps = consultar(c.localA, base(c.contratoA.getId()) + "/composicoes?data=" + DATA, "CMP-001 composição datada");
        assertThat(comps.size()).isEqualTo(1);
        var excedentes = consultar(c.localA, base(c.contratoA.getId()) + "/composicoes/excedentes?data=" + DATA, "CMP-011 monitoramento completo");
        assertThat(excedentes.get(0).path("quantidadeVinculadaAtual").asInt()).isEqualTo(1);
        assertThat(excedentes.get(0).path("efetivoPrevisto").asInt()).isEqualTo(5);
        assertThat(excedentes.get(0).has("justificativas")).isTrue();
    }

    @Test
    void correcaoDeIdentificadorExigePermissaoMantemPesquisaEPosicaoTemporal() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);
        var antes = consultar(c.localA, base(n.id()), "CON-003 identificação antes da correção");
        var r = mapa("novoNumero", "CORR-" + n.id(), "novoAno", 2027, "dataEfetiva", RelogioSistema.hoje(),
                "dataAto", RelogioSistema.hoje(), "motivo", "Erro material comprovado", "referenciaSei", "SEI-CORR",
                "autorizacaoAdministrativa", "ATO-HTTP", "chaveIdempotencia", "corr-" + n.id());
        enviar(c.localA, base(n.id()) + "/correcoes-identificador", r, 403, "CON-003 correção exige permissão específica");
        conceder(c, n.id(), "CORRECAO_IDENTIFICADOR_CONTRATO");
        var corrigida = enviar(c.localA, base(n.id()) + "/correcoes-identificador", r, 201, "CON-003 correção versionada autorizada");
        assertThat(corrigida.path("numeroAnterior").asText()).isEqualTo(antes.path("numeroContrato").asText());
        assertThat(corrigida.path("numeroNovo").asText()).isEqualTo("CORR-" + n.id());
        assertThat(enviar(c.localA, base(n.id()) + "/correcoes-identificador", r, 201, "AUD-005 repetição exata idempotente").path("id").asLong())
                .isEqualTo(corrigida.path("id").asLong());
        assertThat(consultar(c.localA, base(n.id()) + "/correcoes-identificador", "CON-003 histórico preservado").size()).isEqualTo(1);
        assertThat(consultar(c.localA, "/api/contratos/historico-identificador?numero=" + antes.path("numeroContrato").asText()
                + "&ano=" + antes.path("anoContrato").asInt(), "CON-003 identificador anterior pesquisável").size()).isEqualTo(1);
        var pos = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(), "CON-003 identificação atual na posição completa");
        assertThat(pos.path("versaoContratual").path("numeroContrato").asText()).isEqualTo("CORR-" + n.id());
    }

    @Test
    void transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        var req = mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", RelogioSistema.hoje(), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Reorganização administrativa documentada",
                "referenciaSei", "SEI-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId())));
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200, "CON-013/023 transferência excepcional explícita");
        var transferencias = consultar(c.localB, base(n.id()) + "/transferencias-gestao", "CON-013 responsabilidade histórica");
        assertThat(transferencias.size()).isEqualTo(1);
        assertThat(transferencias.get(0).path("gestaoOrigemId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(transferencias.get(0).path("gestaoDestinoId").asLong()).isEqualTo(c.gestaoB.getId());
        var antes = consultar(c.nacional, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje().minusDays(1), "CON-023 contexto anterior preservado");
        assertThat(antes.path("versaoContratual").path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        var depois = consultar(c.localB, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(), "CON-023 contexto posterior mapeado");
        assertThat(depois.path("versaoContratual").path("gestaoLocalId").asLong()).isEqualTo(c.gestaoB.getId());
    }

    private java.util.List<Long> ids(JsonNode array) {
        java.util.List<Long> result = new java.util.ArrayList<>();
        for (JsonNode node : array) result.add(node.path("id").asLong());
        return result;
    }
}
