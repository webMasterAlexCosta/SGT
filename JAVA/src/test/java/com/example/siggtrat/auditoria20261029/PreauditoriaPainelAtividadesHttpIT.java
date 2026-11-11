package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.HistoricoEmpregado;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Provas propostas pela revisão independente do manifesto
 * 3e19c8d47eb566ca6edd6cb6cbda6ff228e4813b291f93af045715a33f5eb1d2.
 * Requer PostgreSQL real. Não depende de descrição injetada diretamente no histórico:
 * todas as narrativas investigadas são produzidas por comandos HTTP reais.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PreauditoriaPainelAtividadesHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @ValueSource(strings = {"GESTOR_ESTADUAL", "GESTOR_NACIONAL"})
    void painelNaoContornaMinimizacaoDoHistoricoGeradoPorEdicaoRealDePessoa(String perfil) throws Exception {
        Cenario c = criarCenarioPersistido();
        String rg = "RG-PREAUDITORIA-" + c.pessoaA.getId();
        String email = "preauditoria-" + c.pessoaA.getId() + "@example.test";
        ObjectNode body = (ObjectNode) consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(),
                "Gestor responsável prepara edição opcional de Pessoa");
        body.put("rg", rg);
        body.put("email", email);
        executar(c.localA, put("/api/pessoas/" + c.pessoaA.getId() + "/dados-opcionais")
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)), 200,
                "Alteração legítima por API gera histórico com valores pessoais anteriores e novos");

        JsonNode historicoLocal = consultar(c.localA,
                "/api/historicos-empregados/empregado/" + c.empregadoA.getId(),
                "Controle: responsável local consulta a narrativa completa");
        assertThat(historicoLocal.toString()).contains(rg, email);
        JsonNode painelLocal = dados(executar(c.localA, get("/api/painel/atividades").param("size", "50"),
                200, "Controle: responsável local vê sua atividade real"));
        assertThat(painelLocal.toString()).contains(rg, email);

        UsuarioAplicacao leitor = perfil.equals("GESTOR_NACIONAL") ? c.nacional : c.estadualA;
        JsonNode historicoLeitor = consultar(leitor,
                "/api/historicos-empregados/empregado/" + c.empregadoA.getId(),
                "PER-006/PER-011: rota histórica aplica minimização de dados pessoais");
        assertThat(historicoLeitor.toString()).doesNotContain(rg, email);
        JsonNode painelLeitor = dados(executar(leitor, get("/api/painel/atividades").param("size", "50"),
                200, "PER-006/PER-011: painel deve obedecer à mesma minimização"));
        assertThat(painelLeitor.toString())
                .as("Atividades não podem oferecer RG/e-mail que a própria rota de histórico oculta")
                .doesNotContain(rg, email);
    }

    @Test
    void painelNaoExpoeNarrativaDeOcorrenciaForaDaResponsabilidadeHistoricaCorrigida() throws Exception {
        Cenario c = criarCenarioPersistido();
        long ocorrencia = dados(executar(c.localA, multipart("/api/ocorrencias")
                        .param("empregadoId", c.empregadoA.getId().toString())
                        .param("motivo", "ATESTADO")
                        .param("dataOcorrencia", DATA.toString()).param("dias", "1")
                        .param("numeroSei", "SEI-PREAUDITORIA-" + UUID.randomUUID())
                        .param("chaveIdempotencia", "preauditoria-" + UUID.randomUUID()),
                201, "Ocorrência legítima gera narrativa vinculada e assinada em A")).path("id").asLong();
        long historico = emTransacao(() -> em.createQuery(
                        "select h.id from HistoricoEmpregado h where h.ocorrencia.id=:id order by h.id desc", Long.class)
                .setParameter("id", ocorrencia).setMaxResults(1).getSingleResult());
        JsonNode original = consultar(c.localA, "/api/historicos-empregados/" + historico,
                "Controle: narrativa original disponível em A antes da correção");
        assertThat(contemAtividade(dados(executar(c.localA, get("/api/painel/atividades").param("size", "50"),
                200, "Controle: painel contém a ocorrência original em A")), historico)).isTrue();

        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", DATA.minusDays(1), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Responsabilidade histórica corrigida por documento na revisão independente",
                "referenciaSei", "SEI-PREAUDITORIA-TRANSFERENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", c.composicaoA.getId(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência documentada muda a responsabilidade do período para B");

        executar(c.localA, get("/api/historicos-empregados/{id}", historico), 403,
                "Controle: rota individual nega a narrativa após correção do escopo");
        JsonNode historicoB = consultar(c.localB, "/api/historicos-empregados/" + historico,
                "Controle: responsável efetivo B recupera a mesma prova original");
        assertEquals(original, historicoB);
        JsonNode painelA = dados(executar(c.localA, get("/api/painel/atividades").param("size", "50"),
                200, "TMP-004/CON-023: painel não deve conceder acesso pelo snapshot territorial superado"));
        JsonNode painelB = dados(executar(c.localB, get("/api/painel/atividades").param("size", "50"),
                200, "HIS-005: atividade deve acompanhar responsabilidade histórica reconstruída"));
        assertThat(contemAtividade(painelA, historico))
                .as("Narrativa negada no histórico não pode continuar acessível pelo painel de A").isFalse();
        assertThat(contemAtividade(painelB, historico))
                .as("Painel de B deve incluir a narrativa de sua responsabilidade efetiva").isTrue();
        emTransacao(() -> {
            HistoricoEmpregado h = em.find(HistoricoEmpregado.class, historico);
            assertEquals(original.path("hashIntegridade").asText(), h.getHashIntegridade());
            assertEquals(c.gestaoA.getId(), h.getGestaoLocalSnapshotId());
            return null;
        });
    }

    private boolean contemAtividade(JsonNode pagina, long id) {
        JsonNode itens = pagina.isArray() ? pagina : pagina.path("content");
        for (JsonNode item : itens) if (item.path("id").asLong() == id) return true;
        return false;
    }
}
