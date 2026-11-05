package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class B230CatalogosPessoasComplementosHttpIT extends B230Suporte {
    @Test
    void cat001TextoLivreNaoSubstituiTipoIdENaoGravaContrato() throws Exception {
        Cenario c = cenario(); LocalDate today = RelogioSistema.hoje();
        var request = rascunho(c, "B230-TIPO-" + UUID.randomUUID().toString().substring(0,8), 2, today, today.plusDays(60));
        Object id = request.remove("tipoContratacaoId"); request.put("tipo", c.tipoA.getDescricao());
        var before = negocio();
        JsonNode rejected = enviar(c.localA, "/api/contratos", request, 400, "CAT001 texto válido do catálogo sem ID não é um Tipo selecionado");
        assertThat(rejected.path("message").asText()).contains("identificador");
        semMutacao(before, "CAT001 texto livre recusado sem contrato/reserva/versão nova");
        request.remove("tipo"); request.put("tipoContratacaoId", id);
        JsonNode accepted = enviar(c.localA, "/api/contratos", request, 201, "Controle: mesmo corpo com ID real pode criar rascunho");
        assertThat(accepted.path("tipoContratacaoId").asLong()).isEqualTo(c.tipoA.getId());
        nova(() -> { assertThat(em.find(Contrato.class, accepted.path("id").asLong()).getTipoContratacao().getId())
                .isEqualTo(c.tipoA.getId()); return null; });
    }

    @Test
    void cat001GestaoInativaEContaInativaNaoAdministramTipoMesmoComIdProprio() throws Exception {
        Cenario c = cenario();
        var request = mapa("descricao", "B230 Tipo " + UUID.randomUUID(), "gestaoLocalId", c.gestaoA.getId(),
                "ativo", true, "matriculaVinculoObrigatoria", false);
        long id = enviar(c.localA, "/api/tipos-contratacao", request, 201, "Controle positivo do mesmo gestor").path("id").asLong();
        nova(() -> { em.find(GestaoLocal.class, c.gestaoA.getId()).setAtivo(false); return null; });
        var before = negocio();
        JsonNode inactiveManagementPut = dados(executar(c.localA, put("/api/tipos-contratacao/" + id)
                .contentType(MediaType.APPLICATION_JSON).content(json(request)),
                401, "CAT001 Gestão inativa impede PUT próprio no filtro de autenticação"));
        assertThat(inactiveManagementPut.path("code").asText()).isEqualTo("ACCOUNT_INACTIVE");
        assertThat(inactiveManagementPut.path("action").asText()).isEqualTo("LOGOUT");
        JsonNode inactiveManagementPost = enviar(c.localA, "/api/tipos-contratacao", request, 401,
                "CAT001 Gestão inativa impede POST no mesmo filtro antes da validação de duplicidade");
        assertThat(inactiveManagementPost.path("code").asText()).isEqualTo("ACCOUNT_INACTIVE");
        assertThat(inactiveManagementPost.path("action").asText()).isEqualTo("LOGOUT");
        semMutacao(before, "CAT001 recusa por Gestão inativa");
        UsuarioAplicacao inactive = nova(() -> {
            GestaoLocal g = em.find(GestaoLocal.class, c.gestaoA.getId()); g.setAtivo(true);
            UsuarioAplicacao u = usuario(com.example.siggtrat.dominio.acesso.PerfilAcesso.GESTOR_LOCAL, g);
            u.setActive(false); em.flush(); return u;
        });
        before = negocio();
        JsonNode denied = dados(executar(inactive, put("/api/tipos-contratacao/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(json(request)), 401, "CAT001 conta inativa conserva escopo mas perde autenticação"));
        assertThat(denied.path("code").asText()).isEqualTo("ACCOUNT_INACTIVE");
        assertThat(denied.path("action").asText()).isEqualTo("LOGOUT");
        semMutacao(before, "CAT001 conta inativa não altera Tipo");
    }

    @Test
    void cat001TodosOsPerfisNaoLocaisEOutraGestaoSaoRecusados() throws Exception {
        Cenario c = cenario(); var request = mapa("descricao", "B230 Perfil " + UUID.randomUUID(), "ativo", true);
        for (var actor : List.of(c.admin, c.presidente, c.nacional, c.estadualA, c.operacionalA)) {
            var before = negocio();
            enviar(actor, "/api/tipos-contratacao", request, 403, "CAT001 perfil sem poder negocial sobre Tipo");
            executar(actor, put("/api/tipos-contratacao/" + c.tipoA.getId()).contentType(MediaType.APPLICATION_JSON)
                    .content(json(request)), 403, "CAT001 perfil não local não edita Tipo próprio do território");
            semMutacao(before, "CAT001 recusa sem alterar catálogo ou disponibilidade");
        }
        var before = negocio();
        executar(c.localB, put("/api/tipos-contratacao/" + c.tipoA.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json(request)), 404, "CAT001 outro Gestor Local não obtém identidade do catálogo");
        semMutacao(before, "CAT001 isolamento de Gestão");
    }

    @ParameterizedTest(name = "CAT005 {0} é o único uso futuro de {1}")
    @CsvSource({"ADI,cargos", "ADI,lotacoes", "ADI,jornadas", "REM,cargos", "REM,lotacoes", "REM,jornadas"})
    void cat005InstrumentoFuturoIsoladoImpedeInativacaoEAposCancelamentoPermite(String instrument, String resource) throws Exception {
        Cenario c = cenario(); Destino target = novoDestino(c); LocalDate today = RelogioSistema.hoje(), future = today.plusDays(10);
        var draft = enviar(c.localA, "/api/contratos", rascunho(c, "B230-INST-" + UUID.randomUUID().toString().substring(0,8),
                10, today, today.plusDays(80)), 201, "Contrato novo sem vínculos para isolar instrumento");
        long contract = draft.path("id").asLong();
        long original = enviar(c.localA, base(contract) + "/composicoes", composicao(c, c.cargoA.getId(), 10), 201,
                "Toda capacidade-base pertence à combinação original").path("id").asLong();
        Long zero = null;
        if (instrument.equals("REM")) zero = enviar(c.localA, base(contract) + "/composicoes",
                mapa("lotacaoId", target.lotacao(), "jornadaId", target.jornada(), "cargoId", target.cargo(), "efetivoPrevisto", 0),
                201, "REM exige combinação existente, aqui publicada com capacidade zero").path("id").asLong();
        enviar(c.localA, base(contract) + "/publicar", mapa("dataPublicacao", today, "referenciaSei", "SEI-SINTETICO-B230",
                "justificativa", "Publicação sintética para isolar capacidade futura"), 200, "Publicação sem pessoas/alocações");
        long catalogId = switch (resource) { case "cargos" -> target.cargo(); case "lotacoes" -> target.lotacao(); default -> target.jornada(); };
        String field = switch (resource) { case "cargos" -> "cargo"; case "lotacoes" -> "lotacao"; default -> "jornada"; };
        nova(() -> {
            assertThat(em.createQuery("select count(a) from AlocacaoVinculo a where a." + field + ".id=:id", Long.class)
                    .setParameter("id", catalogId).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(x) from ComposicaoContratual x where x." + field + ".id=:id and x.efetivoPrevisto>0", Long.class)
                    .setParameter("id", catalogId).getSingleResult()).isZero(); return null;
        });
        String route; Map<String,Object> change;
        if (instrument.equals("ADI")) {
            route = "aditamentos"; change = aditamento("B230-ADI-" + contract, future);
            change.put("novoEfetivoTotal", 13);
            change.put("alteracoesComposicao", List.of(mapa("lotacaoId", target.lotacao(), "jornadaId", target.jornada(),
                    "cargoId", target.cargo(), "quantidadeResultante", 3)));
        } else {
            route = "remanejamentos"; change = mapa("identificacao", "B230-REM-" + contract, "dataEfetiva", future,
                    "justificativa", "Único uso futuro positivo do destino", "confirmarImpacto", true,
                    "itens", List.of(mapa("composicaoId", original, "variacao", -3), mapa("composicaoId", zero, "variacao", 3)));
        }
        var preview = enviar(c.localA, base(contract) + "/" + route + "/previa", change, 200, "Prévia do único instrumento futuro");
        change.put("tokenPrevia", preview.path("tokenPrevia").asText()); change.put("confirmarImpacto", true);
        long instrumentId = enviar(c.localA, base(contract) + "/" + route, change, 201, "Instrumento futuro confirmado").path("id").asLong();
        JsonNode projected = consultar(c.localA, base(contract) + "/composicoes?data=" + future, "Capacidade só nasce no instrumento");
        assertThat(capacidade(projected, target)).isEqualTo(3);
        var before = negocio();
        executar(c.localA, delete("/api/" + resource + "/" + catalogId), 409, "CAT005 instrumento futuro isolado protege catálogo");
        semMutacao(before, "CAT005 negativa não inativa catálogo nem altera o instrumento");
        enviar(c.localA, base(contract) + "/" + route + "/" + instrumentId + "/cancelar",
                mapa("motivo", "Cancelamento sintético antes do efeito", "referenciaSei", "SEI-CANCEL-B230"), 200,
                "Controle causal: remover efeito apenas pelo cancelamento permitido");
        assertThat(capacidade(consultar(c.localA, base(contract) + "/composicoes?data=" + future,
                "Capacidade posterior ao cancelamento"), target)).isZero();
        executar(c.localA, delete("/api/" + resource + "/" + catalogId), 204,
                "Controle: sem instrumento efetivo, a inativação antes recusada passa");
        JsonNode inactive = consultar(c.localA, "/api/" + resource + "/" + catalogId, "Catálogo conservado inativo");
        assertThat(inactive.path("ativo").asBoolean()).isFalse();
        registrarB230("CAT-005:001", mapa("instrumento", instrument, "recurso", resource, "contrato", contract,
                "catalogoId", catalogId, "instrumentoId", instrumentId, "alocacoesBase", 0, "capacidadeBasePositiva", 0,
                "capacidadeComInstrumento", 3, "capacidadeAposCancelar", 0, "status", List.of(409,204)));
    }

    private int capacidade(JsonNode rows, Destino target) {
        int quantity = 0;
        for (JsonNode item : rows) if (item.path("lotacaoId").asLong() == target.lotacao()
                && item.path("jornadaId").asLong() == target.jornada() && item.path("cargoId").asLong() == target.cargo())
            quantity += item.path("efetivoPrevisto").asInt();
        return quantity;
    }

    @ParameterizedTest
    @ValueSource(strings={"motivo","evidencia","autorizacao","impactosEncontrados"})
    void col006CadaCampoExcepcionalAusenteRecusaSemGravarEControleCompletoPassa(String field) throws Exception {
        Cenario c = cenario(); var complete = mapa("tipo", "NOME", "novoValor", "B230 Nome comprovado " + c.pessoaA.getId(),
                "motivo", "Erro material individual demonstrado", "evidencia", "SEI-SINTETICO-B230",
                "autorizacao", "ATO-SINTETICO-B230", "impactosEncontrados", "Pessoa, projeção e vínculos identificados");
        var invalid = new LinkedHashMap<>(complete); invalid.remove(field);
        var before = negocio();
        enviar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade", invalid, 400,
                "COL006 omissão isolada do campo " + field);
        semMutacao(before, "COL006 campo individual obrigatório");
        JsonNode corrected = enviar(c.localA, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade", complete, 200,
                "Controle positivo: corpo idêntico com o campo restaurado");
        assertThat(corrected.path("nome").asText()).isEqualTo(complete.get("novoValor"));
        nova(() -> {
            var history = em.createQuery("select r from CorrecaoPessoa r where r.pessoa.id=:id", CorrecaoPessoa.class)
                    .setParameter("id", c.pessoaA.getId()).getResultList();
            assertThat(history).hasSize(1); assertThat(history.get(0).getResponsavelId()).isEqualTo(c.localA.getId()); return null;
        });
    }

    @Test
    void col006MatrizDeExecutorRecusaPerfisETerritorioSemCorrecao() throws Exception {
        Cenario c = cenario(); var request = mapa("tipo", "NOME", "novoValor", "B230 Nome sem autorização negocial",
                "motivo", "Erro material individual", "evidencia", "SEI-B230", "autorizacao", "ATO-B230", "impactosEncontrados", "Identidade e projeção");
        for (var actor : List.of(c.admin,c.presidente,c.nacional,c.estadualA,c.operacionalA,c.localB)) {
            var before = negocio();
            enviar(actor, "/api/pessoas/" + c.pessoaA.getId() + "/correcoes-identidade", request,
                    actor == c.localB ? 404 : 403, "COL006 executor não autorizado no contexto da pessoa");
            semMutacao(before, "COL006 negativa de executor/escopo");
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"justificativa","evidencia","autorizacao"})
    void vin014CadaCampoObrigatorioRecusaConfirmacaoMesmoComTokenEControleCompletoPassa(String field) throws Exception {
        Cenario c = cenario(); var complete = justificativa(); complete.put("novoInicio", INICIO.plusDays(1));
        var preview = enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/previas/retificacao-inicio",
                complete, 200, "Prévia válida de todos os dados antes da negativa individual");
        complete.put("tokenPrevia", preview.path("token").asText()); complete.put("confirmarImpacto", true);
        var invalid = new LinkedHashMap<>(complete); invalid.remove(field); var before = negocio();
        enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/retificar-inicio", invalid, 400,
                "VIN014 falta apenas " + field + ", sem confundir obrigatoriedade com token ausente");
        semMutacao(before, "VIN014 campo obrigatório omitido não grava retificação/faixa/evento");
        var accepted = enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/retificar-inicio", complete, 200,
                "Controle positivo com o mesmo token ainda válido e corpo integral");
        assertThat(accepted.path("dataInicio").asText()).isEqualTo(INICIO.plusDays(1).toString());
        nova(() -> { assertThat(em.find(VinculoContratual.class, c.vinculoA.getId()).getDataInicio()).isEqualTo(INICIO.plusDays(1)); return null; });
    }
}
