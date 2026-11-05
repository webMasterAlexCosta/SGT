package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.HistoricoEmpregado;
import com.example.siggtrat.servico.HistoricoIntegridadeServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** DIF-02: a narrativa vinculada à ocorrência não oferece acesso lateral por fotografia superada. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class HistoricoOcorrenciaContextoAtualHttpIT extends ContratosHttpBase {
    @Autowired HistoricoIntegridadeServico integridade;

    @Test
    void historicoVinculadoSegueResponsabilidadeDaOcorrenciaSemReescreverProvaOriginal() throws Exception {
        Cenario c = criarCenarioPersistido();
        long ocorrencia = dados(executar(c.localA, multipart("/api/ocorrencias")
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                .param("dataOcorrencia", DATA.toString()).param("dias", "1")
                .param("chaveIdempotencia", "historico-escopo-" + UUID.randomUUID()), 201,
                "Ocorrência gera narrativa auditada no contexto então comprovado")).path("id").asLong();
        long historico = emTransacao(() -> em.createQuery(
                "select h.id from HistoricoEmpregado h where h.empregado.id=:id order by h.id desc", Long.class)
                .setParameter("id", c.empregadoA.getId()).setMaxResults(1).getSingleResult());
        JsonNode antes = consultar(c.localA, "/api/historicos-empregados/" + historico, "Narrativa original acessível em A");
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(
                "dataEfetiva", DATA.minusDays(1), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Responsabilidade histórica corrigida documentalmente", "referenciaSei", "SEI-HISTORICO-FONTE",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa(
                        "composicaoOrigemId", c.composicaoA.getId(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência válida muda responsabilidade efetiva, sem reassinar narrativas anteriores");
        executar(c.localA, get("/api/historicos-empregados/{id}", historico), 403,
                "Histórico vinculado não contorna a mesma proibição de acesso à ocorrência");
        JsonNode depois = consultar(c.localB, "/api/historicos-empregados/" + historico,
                "Destino responsável recupera a narrativa comprovada pelo vínculo explícito");
        assertEquals(antes, depois, "Fotografia, conteúdo e hash originais são preservados");
        assertEquals(ocorrencia, depois.path("ocorrenciaId").asLong());
        assertEquals(3, depois.path("versaoAssinatura").asInt());
        for (String rota : List.of("/api/historicos-empregados", "/api/historicos-empregados/empregado/" + c.empregadoA.getId())) {
            JsonNode origem = dados(executar(c.localA, get(rota).param("size", "200"), 200,
                    "Filtro de escopo precede total e paginação da lista histórica"));
            JsonNode destino = dados(executar(c.localB, get(rota).param("size", "200"), 200,
                    "Lista do destino inclui o registro vinculado à ocorrência"));
            assertFalse(contem(origem, historico)); assertTrue(contem(destino, historico));
        }
        emTransacao(() -> {
            HistoricoEmpregado h = em.find(HistoricoEmpregado.class, historico);
            assertEquals(c.gestaoA.getId(), h.getGestaoLocalSnapshotId());
            assertEquals(antes.path("hashIntegridade").asText(), h.getHashIntegridade()); return null;
        });
        assertTrue(integridade.verificarEmpregado(c.empregadoA.getId()).integro());
    }

    private boolean contem(JsonNode pagina, long id) {
        JsonNode itens = pagina.isArray() ? pagina : pagina.path("content");
        for (JsonNode item : itens) if (item.path("id").asLong() == id) return true;
        return false;
    }
}
