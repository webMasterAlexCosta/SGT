package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.time.DayOfWeek;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Contratos HTTP dos catálogos: payload, persistência, consulta e fronteiras de escopo. */
class CatalogosContratoHttpIT extends AuditoriaHttpSuporte {

    @ParameterizedTest
    @ValueSource(strings = {"cargos", "tipos-contratacao", "motivos-desligamento", "lotacoes", "jornadas"})
    void cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao(String recurso) throws Exception {
        var c = criarCenario();
        String nome = "Catalogo " + UUID.randomUUID().toString().substring(0, 8);
        String campo = Set.of("cargos", "lotacoes").contains(recurso) ? "nome" : "descricao";
        Map<String,Object> body = payload(recurso, c, nome, true);
        int criadoStatus = recurso.equals("cargos") ? 200 : 201;
        if(recurso.equals("cargos")) {
            long antes=em.createQuery("select count(c) from Cargo c where c.gestaoLocal.id=:g",Long.class)
                    .setParameter("g",c.gestaoA.getId()).getSingleResult();
            for(String variante:List.of("OMITIDO","NULO","VAZIO","BRANCO")) {
                Map<String,Object> invalido=new LinkedHashMap<>(body);
                switch(variante) { case "OMITIDO" -> invalido.remove("nome"); case "NULO" -> invalido.put("nome",null);
                    case "VAZIO" -> invalido.put("nome",""); default -> invalido.put("nome","   "); }
                assertThat(invalido.get("ativo")).isEqualTo(body.get("ativo"));
                assertThat(invalido.get("gestaoLocalId")).isEqualTo(body.get("gestaoLocalId"));
                executar(c.localA,post("/api/cargos").contentType(MediaType.APPLICATION_JSON).content(json(invalido)),400,
                        "CAT002 nome isoladamente "+variante+" com demais campos válidos");
                em.flush();em.clear(); // recarga na transação ambiente; não commit
                assertThat(em.createQuery("select count(c) from Cargo c where c.gestaoLocal.id=:g",Long.class)
                        .setParameter("g",c.gestaoA.getId()).getSingleResult()).isEqualTo(antes);
            }
            body.put("nome","  "+nome+"  "); // controle positivo confirma normalização, sem faltar outro campo
        }
        JsonNode criado = dados(executar(c.localA, post("/api/" + recurso).contentType(MediaType.APPLICATION_JSON)
                .content(json(body)), criadoStatus, "CAT-001/002 criação " + recurso + " na Gestão própria"));
        long id = criado.path("id").asLong();
        assertThat(id).isPositive();
        if(recurso.equals("cargos")) {
            assertThat(criado.path("nome").asText()).isEqualTo(nome);
            em.flush();em.clear();
            Cargo persistido=em.find(Cargo.class,id);
            assertThat(persistido.getNome()).isEqualTo(nome);
            assertThat(persistido.getGestaoLocal().getId()).isEqualTo(c.gestaoA.getId());
        }
        assertThat(criado.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(criado.path("ativo").asBoolean()).isTrue();
        if (Set.of("tipos-contratacao", "motivos-desligamento").contains(recurso)) {
            assertThat(criado.path("criadoPorId").asLong()).isEqualTo(c.localA.getId());
            assertThat(criado.path("criadoEm").asText()).isNotBlank();
        }
        JsonNode detalhe = dados(executar(c.localA, get("/api/" + recurso + "/{id}", id), 200,
                "CAT-003 detalhe de " + recurso));
        assertThat(detalhe.path("id").asLong()).isEqualTo(id);
        JsonNode lista = conteudo(dados(executar(c.localA, get("/api/" + recurso), 200,
                "CAT-001/002 listagem de " + recurso + " pelo escopo")));
        assertThat(ids(lista)).contains(id).doesNotContain(idDoOutroEscopo(recurso, c));
        executar(c.localB, get("/api/" + recurso + "/{id}", id), 404,
                "CAT escopo de leitura cruzada bloqueado " + recurso);
        executar(c.localB, put("/api/" + recurso + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json(body)), 404, "CAT escopo de escrita cruzada bloqueado " + recurso);
        executar(c.nacional, put("/api/" + recurso + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json(body)), 403, "PER-003 Nacional não altera " + recurso);

        Map<String,Object> atualizado = payload(recurso, c, nome + " alterado", true);
        JsonNode apos = dados(executar(c.localA, put("/api/" + recurso + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json(atualizado)), 200, "CAT atualização administrativa " + recurso));
        assertThat(apos.path(campo).asText()).isEqualTo(recurso.equals("jornadas")
                ? (nome + " alterado").toUpperCase(Locale.ROOT) : nome + " alterado");

        if (Set.of("cargos", "lotacoes", "jornadas").contains(recurso)) {
            executar(c.localA, delete("/api/" + recurso + "/{id}", id), 204,
                    "CAT-003 DELETE representa inativação " + recurso);
        } else {
            atualizado.put("ativo", false);
            executar(c.localA, put("/api/" + recurso + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                    .content(json(atualizado)), 200, "CAT-003 inativação preserva " + recurso);
        }
        JsonNode inativo = dados(executar(c.localA, get("/api/" + recurso + "/{id}", id), 200,
                "CAT-003 cadastro inativo permanece consultável " + recurso));
        assertThat(inativo.path("ativo").asBoolean()).isFalse();
        atualizado.put("ativo", true);
        JsonNode reativado = dados(executar(c.localA, put("/api/" + recurso + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json(atualizado)), 200, "CAT-006 reativação explícita " + recurso));
        assertThat(reativado.path("id").asLong()).isEqualTo(id);
        assertThat(reativado.path("ativo").asBoolean()).isTrue();
        executar(c.localA, post("/api/" + recurso).contentType(MediaType.APPLICATION_JSON).content("{}"), 400,
                "CAT payload obrigatório inválido " + recurso);
    }

    @Test
    void lotacoesOpcoesERegrasAdicionaisMantemEscopoDaJornada() throws Exception {
        var c = criarCenario();
        JsonNode opcoes = dados(executar(c.localA, get("/api/lotacoes/opcoes"), 200,
                "CAT opções de lotação e turnos no escopo"));
        assertThat(ids(opcoes)).contains(c.lotacaoA.getId()).doesNotContain(c.lotacaoB.getId());
        Map<String,Object> body = new LinkedHashMap<>(Map.of("tipo", "ADICIONAL_TESTE", "inicio", "22:00",
                "fim", "05:00", "percentual", 20, "ativo", true, "ordem", 1));
        JsonNode regra = dados(executar(c.localA, post("/api/jornadas/{id}/regras-adicionais", c.jornadaA.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)), 201, "Jornada criação de regra adicional"));
        long id = regra.path("id").asLong();
        assertThat(regra.path("percentual").decimalValue()).isEqualByComparingTo("20");
        JsonNode regras = dados(executar(c.localA, get("/api/jornadas/{id}/regras-adicionais", c.jornadaA.getId()),
                200, "Jornada listagem de regras adicionais"));
        assertThat(ids(regras)).contains(id);
        executar(c.localB, get("/api/jornadas/{id}/regras-adicionais", c.jornadaA.getId()), 404,
                "Jornada regras fora da Gestão não são visíveis");
        executar(c.localB, post("/api/jornadas/{id}/regras-adicionais", c.jornadaA.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)), 404, "Jornada regra não pode ser criada fora do escopo");
        executar(c.localB, put("/api/regras-adicionais/{id}", id).contentType(MediaType.APPLICATION_JSON).content(json(body)),
                404, "Jornada regra fora do escopo não pode ser alterada");
        executar(c.localB, delete("/api/regras-adicionais/{id}", id), 404,
                "Jornada regra fora do escopo não pode ser excluída");
        body.put("percentual", 25);
        JsonNode alterada = dados(executar(c.localA, put("/api/regras-adicionais/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(json(body)), 200, "Jornada alteração de regra adicional"));
        assertThat(alterada.path("percentual").decimalValue()).isEqualByComparingTo("25");
        executar(c.localA, delete("/api/regras-adicionais/{id}", id), 204, "Jornada exclusão de regra adicional de preparação");
        assertThat(em.find(RegraAdicional.class, id)).isNull();
        body.put("percentual", -1);
        executar(c.localA, post("/api/jornadas/{id}/regras-adicionais", c.jornadaA.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)), 400, "Jornada percentual negativo rejeitado");
    }

    @Test
    void statusPossuiContratoAdministrativoCompleto() throws Exception {
        var c = criarCenario();
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        for (String recurso : List.of("status-empregados")) {
            Map<String,Object> body = new LinkedHashMap<>(Map.of("descricao", "T-" + sufixo));
            JsonNode criado = dados(executar(c.admin, post("/api/" + recurso).contentType(MediaType.APPLICATION_JSON).content(json(body)),
                    200, "Apoio criação " + recurso));
            long id = criado.path("id").asLong();
            assertListAndDetail(c, recurso, id);
            body.put("descricao", "U-" + sufixo);
            JsonNode alterado = dados(executar(c.admin, put("/api/" + recurso + "/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON).content(json(body)), 200, "Apoio atualização " + recurso));
            assertThat(alterado.path("descricao").asText()).isEqualTo(recurso.equals("status-empregados")
                    ? ((String) body.get("descricao")).toUpperCase(Locale.ROOT) : body.get("descricao"));
            executar(c.nacional, delete("/api/" + recurso + "/{id}", id), 403,
                    "Perfil de consulta não exclui " + recurso);
            executar(c.admin, delete("/api/" + recurso + "/{id}", id), 204, "Apoio exclusão " + recurso);
            executar(c.admin, get("/api/" + recurso + "/{id}", id), 404, "Apoio inexistente " + recurso);
            executar(c.admin, post("/api/" + recurso).contentType(MediaType.APPLICATION_JSON).content("{}"), 400,
                    "Apoio payload obrigatório " + recurso);
        }
    }

    @Test
    void endpointsAposentadosNaoExistemParaNenhumPerfil() throws Exception {
        var c = criarCenario();
        for (String recurso : List.of("bancos", "agencias", "tipos-conta", "contas", "enderecos", "telefones")) {
            for (UsuarioAplicacao usuario : List.of(c.admin, c.localA, c.estadualA, c.nacional, c.presidente, c.operacionalA)) {
                executar(usuario, get("/api/" + recurso), 404, "COL-004 endpoint pessoal aposentado GET " + recurso);
                executar(usuario, get("/api/" + recurso + "/{id}", 1), 404, "COL-004 endpoint pessoal aposentado detalhe " + recurso);
                for (HttpMethod metodo : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)) {
                    String caminho = "/api/" + recurso + (metodo == HttpMethod.POST ? "" : "/1");
                    executar(usuario, request(metodo, caminho).contentType(MediaType.APPLICATION_JSON).content("{}"),
                            404, "COL-004 endpoint pessoal aposentado " + metodo + " " + recurso);
                }
            }
        }
    }

    private void assertListAndDetail(Cenario c, String recurso, long id) throws Exception {
        JsonNode lista = conteudo(dados(executar(c.localA, get("/api/" + recurso), 200, "Apoio lista " + recurso)));
        assertThat(ids(lista)).contains(id);
        JsonNode detalhe = dados(executar(c.localA, get("/api/" + recurso + "/{id}", id), 200, "Apoio detalhe " + recurso));
        assertThat(detalhe.path("id").asLong()).isEqualTo(id);
        executar(c.localA, get("/api/" + recurso + "/{id}", Long.MAX_VALUE), 404, "Apoio ID inexistente " + recurso);
    }

    private Map<String,Object> payload(String recurso, Cenario c, String nome, boolean ativo) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put(Set.of("cargos", "lotacoes").contains(recurso) ? "nome" : "descricao", nome);
        body.put("ativo", ativo);
        body.put("gestaoLocalId", c.gestaoA.getId());
        if (recurso.equals("motivos-desligamento")) body.put("categoria", "DESLIGAMENTO_INDIVIDUAL");
        if (recurso.equals("tipos-contratacao")) body.put("matriculaVinculoObrigatoria", false);
        if (recurso.equals("lotacoes")) {
            body.put("municipio", "CURITIBA");
            body.put("turnos", List.of(Map.of("id", c.jornadaA.getId())));
        }
        if (recurso.equals("jornadas")) {
            List<Map<String,Object>> dias = new ArrayList<>();
            for (DayOfWeek dia : DayOfWeek.values()) {
                Map<String,Object> item = new LinkedHashMap<>();
                item.put("diaSemana", dia.name()); item.put("diaTrabalho", dia.getValue() <= 5);
                if (dia.getValue() <= 5) { item.put("entrada1", "08:00"); item.put("saida1", "16:00"); }
                dias.add(item);
            }
            body.put("dias", dias);
        }
        return body;
    }

    private long idDoOutroEscopo(String recurso, Cenario c) {
        return switch (recurso) {
            case "cargos" -> c.cargoB.getId();
            case "tipos-contratacao" -> c.tipoB.getId();
            case "lotacoes" -> c.lotacaoB.getId();
            case "jornadas" -> c.jornadaB.getId();
            default -> Long.MAX_VALUE;
        };
    }
    private JsonNode conteudo(JsonNode resposta) { return resposta.has("content") ? resposta.get("content") : resposta; }
    private List<Long> ids(JsonNode array) { List<Long> ids = new ArrayList<>(); array.forEach(n -> ids.add(n.path("id").asLong())); return ids; }
}
