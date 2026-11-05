package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Verifica a fronteira de consulta técnica sem adulterar a evidência original. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaDadosRestritosNovaAuditoriaHttpIT extends AuditoriaHttpSuporte {
    @Autowired FilaRegistroAuditoria fila;

    @Test
    void credenciaisNaQueryTambemSaoMascaradasAntesDaPersistencia() throws Exception {
        Cenario c = criarCenarioPersistido();
        String requestId = UUID.randomUUID().toString();
        var body = dados(executar(c.localA, get("/api/pessoas/" + c.pessoaA.getId()),
                200, "Preparar comando legítimo para verificar captura da query"));
        executar(c.localA, put("/api/pessoas/" + c.pessoaA.getId() + "/dados-opcionais")
                .queryParam("newPassword", "SentinelaSenhaQueryNaoDevePersistir")
                .queryParam("filtro", "permitido").header("X-Request-Id", requestId)
                .contentType(MediaType.APPLICATION_JSON).content(json(body)),
                200, "Proteção de credenciais também cobre parâmetros desconhecidos na URL");
        fila.descarregar();
        String query = emTransacao(() -> em.createQuery(
                        "select a.queryString from RegistroAuditoria a where a.requestId=:id and a.action='PESSOA_ATUALIZACAO_OPCIONAL'", String.class)
                .setParameter("id", requestId).getSingleResult());
        assertThat(query).isNotNull().contains("newPassword", "filtro=permitido")
                .doesNotContain("SentinelaSenhaQueryNaoDevePersistir");
    }

    @Test
    void registroLegadoSemTipoEComDetalhesAninhadosNaoVazaConteudoPessoal() throws Exception {
        Cenario c = criarCenarioPersistido();
        String sentinela = "DadoPessoalLegadoNaoExposto";
        Long id = emTransacao(() -> {
            RegistroAuditoria a = new RegistroAuditoria();
            a.setAction("REGISTRO_LEGADO"); a.setEntityId(c.pessoaA.getId());
            a.setEntityLabel(sentinela); a.setPayloadJson("{\"email\":\"" + sentinela + "\"}");
            a.setQueryString("cpf=" + c.pessoaA.getCpf());
            a.setDetailsJson("{\"pessoaId\":" + c.pessoaA.getId()
                    + ",\"antes\":{\"email\":\"" + sentinela + "\"},\"depois\":2}");
            em.persist(a); return a.getId();
        });
        var detalhe = dados(executar(c.admin, get("/api/registros-auditoria/" + id),
                200, "Auditoria legada minimiza campos livres sem exigir tipo cadastrado"));
        assertThat(detalhe.toString()).doesNotContain(sentinela, c.pessoaA.getCpf());
        var tecnico = mapper.readTree(detalhe.path("detailsJson").asText());
        assertThat(tecnico.path("pessoaId").asLong()).isEqualTo(c.pessoaA.getId());
        assertThat(tecnico.path("depois").asInt()).isEqualTo(2);
        emTransacao(() -> {
            assertThat(em.find(RegistroAuditoria.class, id).getPayloadJson()).contains(sentinela);
            return null;
        });
    }

    @Test
    void auditoriaTecnicaNaoContornaMinimizacaoDosDadosPessoais() throws Exception {
        Cenario c = criarCenarioPersistido();
        String rg = "RG-RESTRITO-" + c.pessoaA.getId();
        String email = "restrito-" + c.pessoaA.getId() + "@example.test";
        String requestId = UUID.randomUUID().toString();
        ObjectNode body = (ObjectNode) dados(executar(c.localA, get("/api/pessoas/" + c.pessoaA.getId()),
                200, "Preparar atualização opcional no próprio escopo"));
        body.put("rg", rg); body.put("email", email);
        executar(c.localA, put("/api/pessoas/" + c.pessoaA.getId() + "/dados-opcionais")
                .header("X-Request-Id", requestId).contentType(MediaType.APPLICATION_JSON).content(json(body)),
                200, "Atualização real gera auditoria com dados pessoais restritos");
        fila.descarregar();
        RegistroAuditoria original = emTransacao(() -> em.createQuery(
                        "select a from RegistroAuditoria a where a.requestId=:requestId and a.action='PESSOA_ATUALIZACAO_OPCIONAL'", RegistroAuditoria.class)
                .setParameter("requestId", requestId).getSingleResult());
        assertThat(original.getPayloadJson()).contains(rg, email);
        var empregadoMinimizado = dados(executar(c.admin, get("/api/pessoas/" + c.pessoaA.getId()),
                200, "Administrador já recebe cadastro minimizado"));
        assertThat(empregadoMinimizado.toString()).doesNotContain(rg, email);

        var lista = dados(executar(c.admin, get("/api/registros-auditoria").param("requestId", requestId),
                200, "PER-005 consulta técnica não concede acesso pessoal pelo payload auditado"));
        assertThat(lista.path("totalElements").asLong()).isEqualTo(1);
        assertThat(lista.toString()).doesNotContain(rg, email, c.pessoaA.getCpf());
        var detalhe = dados(executar(c.admin, get("/api/registros-auditoria/" + original.getId()),
                200, "PER-005 detalhe técnico mantém a mesma proteção da coleção"));
        assertThat(detalhe.toString()).doesNotContain(rg, email, c.pessoaA.getCpf());
        assertThat(detalhe.path("action").asText()).isEqualTo("PESSOA_ATUALIZACAO_OPCIONAL");
        assertThat(detalhe.path("entityId").asLong()).isEqualTo(c.pessoaA.getId());
        emTransacao(() -> {
            assertThat(em.find(RegistroAuditoria.class, original.getId()).getPayloadJson()).isEqualTo(original.getPayloadJson());
            return null;
        });
    }

    @Test
    void camposDeCredencialConhecidosNaoDependemDeRegexCompletaNoAmbiente() throws Exception {
        Cenario c = criarCenarioPersistido();
        String requestId = UUID.randomUUID().toString();
        ObjectNode body = (ObjectNode) dados(executar(c.localA, get("/api/pessoas/" + c.pessoaA.getId()),
                200, "Preparar requisição JSON legítima com propriedades adicionais ignoradas"));
        body.put("rg", "RG-AUDITORIA-" + c.pessoaA.getId());
        body.put("newPassword", "SentinelaNovaSenhaNaoDevePersistir");
        body.put("currentPassword", "SentinelaSenhaAtualNaoDevePersistir");
        body.putObject("dadosExtras").put("senhaAtual", "SentinelaSenhaAninhadaNaoDevePersistir");
        executar(c.localA, put("/api/pessoas/" + c.pessoaA.getId() + "/dados-opcionais")
                .header("X-Request-Id", requestId).contentType(MediaType.APPLICATION_JSON).content(json(body)),
                200, "Auditoria captura payload sem expor variantes conhecidas de senha");
        fila.descarregar();
        String persistido = emTransacao(() -> em.createQuery(
                        "select a.payloadJson from RegistroAuditoria a where a.requestId=:requestId and a.action='PESSOA_ATUALIZACAO_OPCIONAL'", String.class)
                .setParameter("requestId", requestId).getSingleResult());
        assertThat(persistido).doesNotContain("SentinelaNovaSenhaNaoDevePersistir",
                "SentinelaSenhaAtualNaoDevePersistir", "SentinelaSenhaAninhadaNaoDevePersistir");
        assertThat(mapper.readTree(persistido).path("newPassword").asText()).isEqualTo("***");
    }
}
