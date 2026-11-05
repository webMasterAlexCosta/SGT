package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.relatorio.CausaSuperacaoRelatorio;
import com.example.siggtrat.dominio.relatorio.EstadoEmissaoRelatorio;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** REL-001/002/006: alterações legítimas na janela operacional também alteram a verdade do relatório. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoRelatoriosOperacionaisHttpIT extends ContratosHttpBase {
    private static final String EMISSOES = "/api/relatorios/administrativos/emissoes";

    @Test
    void alteracaoOperacionalDentroDaJanelaSuperaEmissaoEPreservaSnapshot() throws Exception {
        Cenario c = criarCenarioPersistido(); LocalDate dia = diaUtilRecente();
        enviar(c.operacionalA, "/api/frequencias", frequencia(c, dia, "FALTA"), 200,
                "Operacional registra falta na janela ordinária de edição");
        Map<String, Object> filtro = filtro(c, dia);
        JsonNode primeira = enviar(c.localA, EMISSOES, filtro, 201, "Gestor formaliza a primeira emissão");
        long id = primeira.path("metadados").path("id").asLong();
        Prova original = prova(id);
        assertEquals(1, primeira.path("conteudo").path("totais").path("faltas").asInt());

        enviar(c.operacionalA, "/api/frequencias", frequencia(c, dia, "PRESENCA"), 200,
                "Operacional corrige a própria falta dentro dos sete dias permitidos");

        emTransacao(() -> {
            EmissaoRelatorioAdministrativo e = em.find(EmissaoRelatorioAdministrativo.class, id);
            assertEquals(EstadoEmissaoRelatorio.SUPERADA, e.getEstado(),
                    "O estado da emissão precisa acompanhar a mudança válida da fonte, independentemente do perfil autor.");
            assertEquals(CausaSuperacaoRelatorio.RETIFICACAO_FONTE, e.getCausaSuperacao());
            assertNotNull(e.getSuperadaEm());
            assertArrayEquals(original.gzip(), e.getConteudoSnapshotGzip());
            assertEquals(original.hash(), e.getHashConteudo());
            return null;
        });
        JsonNode comparacao = consultar(c.localA, EMISSOES + "/" + id + "/comparacao-vigente",
                "Comparação mantém o fato emitido e mostra a fonte corrigida");
        assertTrue(comparacao.path("alterado").asBoolean());
        assertEquals(1, comparacao.path("conteudoHistorico").path("totais").path("faltas").asInt());
        assertEquals(1, comparacao.path("conteudoVigente").path("totais").path("presencas").asInt());

        filtro.put("chaveIdempotencia", "op-relatorio-retificado-" + UUID.randomUUID());
        JsonNode segunda = enviar(c.localA, EMISSOES, filtro, 201,
                "Nova emissão identifica a correção retroativa operacional já confirmada");
        assertEquals("RETIFICADA", segunda.path("metadados").path("natureza").asText());
        assertEquals(2, segunda.path("metadados").path("versao").asInt());
        assertEquals(id, segunda.path("metadados").path("emissaoAnteriorId").asLong());
        assertEquals(1, segunda.path("conteudo").path("totais").path("presencas").asInt());
        assertArrayEquals(original.gzip(), prova(id).gzip());
        executar(c.localB, get(EMISSOES + "/" + id), 404,
                "A correção não amplia o acesso ao arquivo de outra Gestão Local");
    }

    @Test
    void reenvioOperacionalIdenticoNaoSuperaEmissaoNemDuplicaFato() throws Exception {
        Cenario c = criarCenarioPersistido(); LocalDate dia = diaUtilRecente();
        Map<String, Object> lancamento = frequencia(c, dia, "FALTA");
        long frequenciaId = enviar(c.operacionalA, "/api/frequencias", lancamento, 200,
                "Lançamento original sem chave explícita").path("id").asLong();
        JsonNode primeira = enviar(c.localA, EMISSOES, filtro(c, dia), 201,
                "Emissão vigente antes do reenvio idêntico");
        long id = primeira.path("metadados").path("id").asLong(); Prova original = prova(id);
        int versao = emTransacao(() -> em.find(Frequencia.class, frequenciaId).getVersaoNegocial());
        long eventos = eventos(frequenciaId);

        enviar(c.operacionalA, "/api/frequencias", lancamento, 200,
                "Reenvio sem chave permanece sem novo efeito negocial");

        emTransacao(() -> {
            EmissaoRelatorioAdministrativo e = em.find(EmissaoRelatorioAdministrativo.class, id);
            assertEquals(EstadoEmissaoRelatorio.VIGENTE, e.getEstado());
            assertNull(e.getSuperadaEm()); assertNull(e.getCausaSuperacao());
            assertArrayEquals(original.gzip(), e.getConteudoSnapshotGzip());
            assertEquals(original.hash(), e.getHashConteudo());
            assertEquals(versao, em.find(Frequencia.class, frequenciaId).getVersaoNegocial());
            return null;
        });
        assertEquals(eventos, eventos(frequenciaId));
        assertFalse(consultar(c.localA, EMISSOES + "/" + id + "/comparacao-vigente",
                "Reenvio inócuo não altera a comparação da emissão").path("alterado").asBoolean());
    }

    private Map<String, Object> frequencia(Cenario c, LocalDate dia, String status) {
        Map<String, Object> dados = mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", dia, "status", status);
        if ("PRESENCA".equals(status)) {
            dados.put("entrada1", "08:00"); dados.put("saida1", "12:00");
            dados.put("entrada2", "13:00"); dados.put("saida2", "17:00");
        }
        return dados;
    }

    private Map<String, Object> filtro(Cenario c, LocalDate dia) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                "inicio", dia, "fim", dia, "contratoIds", List.of(c.contratoA.getId()),
                "chaveIdempotencia", "op-relatorio-" + UUID.randomUUID());
    }

    private LocalDate diaUtilRecente() {
        LocalDate dia = RelogioSistema.hoje().minusDays(1);
        while (dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY) dia = dia.minusDays(1);
        return dia;
    }

    private long eventos(long frequenciaId) {
        return emTransacao(() -> em.createQuery("select count(e) from EventoFrequencia e where e.frequencia.id=:id", Long.class)
                .setParameter("id", frequenciaId).getSingleResult());
    }

    private Prova prova(long id) {
        return emTransacao(() -> {
            EmissaoRelatorioAdministrativo e = em.find(EmissaoRelatorioAdministrativo.class, id);
            return new Prova(e.getConteudoSnapshotGzip().clone(), e.getHashConteudo());
        });
    }

    private record Prova(byte[] gzip, String hash) { }
}
