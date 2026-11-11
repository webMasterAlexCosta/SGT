package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** RST004: corpus único e intervalo quieto comparado integralmente, sem excluir tabelas mutáveis. */
class ReinicioIntegralCorpusJarIT extends GapsBackend15JarSuporte {
    @Test
    @Timeout(value = 240, unit = TimeUnit.SECONDS)
    void paradaGraciosaDoMesmoJarPreservaTodasLinhasCatalogoArquivosEJobDoMesmoCorpus() throws Exception {
        preparar("rst004");
        Map<String, Object> bancoId = identidadeBanco(); Map<String, String> redisId = identidadeRedis();
        LocalDate dia = LocalDate.now(ZONA).minusDays(1);
        while (dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY) dia = dia.minusDays(1);
        final LocalDate data = dia;
        long contrato = 0, documento = 0, jobId = 0, emissaoId = 0, pidAnterior = 0;
        byte[] pdf = pdf("DOCUMENTO SINTETICO RST004 - persistencia integral do mesmo corpus");
        JsonNode jobOriginal = null, emissaoOriginal = null;
        String tarefaVolatil = null;
        Path relatorioPreservado = null;
        boolean provaReinicio = false;
        try {
            iniciar("antes"); pidAnterior = filho.pid();
            try (Sessao gestor = login(c.localA)) {
                contrato = publicar(gestor);
                long sei = gestor.get(base(contrato) + "/documentos", 200).get(0).path("id").asLong();
                JsonNode anexo = gestor.multipart(base(contrato) + "/documentos/" + sei + "/substituir", mapa(
                        "motivo", "Documento legível sintético do ensaio de reinício", "chaveIdempotencia", "rst-pdf-" + UUID.randomUUID()),
                        "documento-rst.pdf", "application/pdf", pdf, true);
                documento = anexo.path("id").asLong();
                assertEquals("PUBLICADO", anexo.path("estado").asText());
                assertEquals(hash(pdf), anexo.path("sha256Arquivo").asText());
                assertArrayEquals(pdf, gestor.pedido("GET", base(contrato) + "/documentos/" + documento + "/arquivo", null, null, 200).body());

                // Job DURÁVEL e seus eventos pertencem a este banco/corpus, não a um IT anterior.
                JsonNode aceito = gestor.json("POST", "/api/frequencias/fechamentos", mapa("dataReferencia", data,
                        "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(), "contratoIds", List.of(c.contratoA.getId()),
                        "alteracoes", List.of(), "justificativaRetificacao", "Fechamento sintético pertinente ao mesmo corpus RST004",
                        "chaveIdempotencia", "rst-job-" + UUID.randomUUID()), 202);
                jobId = aceito.path("id").asLong();
                jobOriginal = aguardar(gestor, "/api/frequencias/fechamentos/" + jobId, "CONCLUIDO", 30);
                assertEquals(1, jobOriginal.path("tentativa").asInt());
                assertEquals(1, jobOriginal.path("totalPrevisto").asLong()); assertEquals(1, jobOriginal.path("totalProcessado").asLong());
                assertEquals(1, jobOriginal.path("totalInserido").asLong());
                assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM frequencia WHERE id_empregado=? AND data_referencia=?", Long.class, c.empregadoA.getId(), data));
                assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM evento_frequencia e JOIN frequencia f ON f.id=e.id_frequencia WHERE f.id_empregado=? AND f.data_referencia=?", Long.class, c.empregadoA.getId(), data));

                emissaoOriginal = gestor.json("POST", "/api/relatorios/administrativos/emissoes", mapa("tipo", "FREQUENCIA_CONSOLIDADA",
                        "formato", "VISUALIZACAO", "inicio", data, "fim", data, "contratoIds", List.of(c.contratoA.getId()),
                        "chaveIdempotencia", "rst-emissao-" + UUID.randomUUID()), 201);
                emissaoId = emissaoOriginal.path("metadados").path("id").asLong(); assertTrue(emissaoId > 0);

                tarefaVolatil = gestor.json("POST", "/api/relatorios/terceirizados/exportacao/solicitacao?contratoId="
                        + c.contratoA.getId() + "&formato=XLSX", null, 202).path("id").asText();
                aguardar(gestor, "/api/relatorios/exportacoes/" + tarefaVolatil, "CONCLUIDO", 25);
                try (var arquivos = Files.walk(pasta.resolve("relatorios"))) {
                    List<Path> finais = arquivos.filter(Files::isRegularFile).toList();
                    assertEquals(1, finais.size(), "Um relatório real concluído ainda não transferido");
                    relatorioPreservado = finais.get(0); assertFalse(relatorioPreservado.toString().endsWith(".part"));
                    assertTrue(Files.size(relatorioPreservado) > 100);
                }
            }

            // Nenhuma chamada autenticada entre este snapshot e a comparação pós-boot.
            Banco antes = esperarQuietude();
            Map<String, String> arquivosAntes = arquivos();
            assertTrue(arquivosAntes.keySet().stream().anyMatch(k -> k.startsWith("uploads/")));
            assertTrue(arquivosAntes.keySet().stream().anyMatch(k -> k.startsWith("relatorios/")));
            assertFalse(antes.linhas().get("fechamento_frequencia").isEmpty());
            assertFalse(antes.linhas().get("evento_frequencia").isEmpty());
            prova("rst004-antes", mapa("jarSha256", JAR_SHA, "pid", pidAnterior, "banco", bancoId, "redis", redisId,
                    "snapshotIntegral", antes, "arquivos", arquivosAntes, "contratoPublicado", contrato, "documentoId", documento,
                    "jobId", jobId, "job", jobOriginal, "emissaoId", emissaoId, "tarefaExportacaoVolatil", tarefaVolatil,
                    "comparacao", "Todas as linhas/colunas e catálogo, sem exclusões; apenas hashes de conteúdos privados."));
            encerrar(); assertFalse(filho.isAlive());
            assertEquals(bancoId, identidadeBanco()); assertEquals(redisId, identidadeRedis());
            assertEquals(arquivosAntes, arquivos());
            iniciar("depois"); // mesmos JAR/porta/identidade/ENV/opções; somente arquivo privado de log difere.
            assertNotEquals(pidAnterior, filho.pid());
            Banco depois = esperarQuietude();
            Map<String, String> arquivosDepois = arquivos();
            prova("rst004-depois", mapa("snapshotIntegral", depois, "arquivos", arquivosDepois,
                    "jarSha256", hashArquivo(jar), "pidAnterior", pidAnterior, "pidNovo", filho.pid(),
                    "banco", identidadeBanco(), "redis", identidadeRedis(), "nenhumLoginAntesDaComparacao", true,
                    "tabelasComparadas", antes.linhas().keySet(), "tabelasExcluidas", List.of()));
            assertEquals(antes.linhas(), depois.linhas(), "Todas as linhas e versões, inclusive sessão e auditoria, devem permanecer iguais no intervalo quieto");
            assertEquals(antes.catalogo(), depois.catalogo(), "Colunas/defaults/índices/FKs/sequências preservados");
            assertEquals(arquivosAntes, arquivosDepois, "Uploads e relatório real preservados byte a byte");
            assertEquals(bancoId, identidadeBanco()); assertEquals(redisId, identidadeRedis());
            assertEquals(JAR_SHA, hashArquivo(jar)); provaReinicio = true;

            // A partir daqui efeitos de login/download são legítimos e documentados, fora do intervalo quieto.
            try (Sessao gestor = login(c.localA)) {
                assertEquals(jobOriginal, gestor.get("/api/frequencias/fechamentos/" + jobId, 200));
                JsonNode releitura = gestor.get("/api/relatorios/administrativos/emissoes/" + emissaoId, 200);
                assertEquals(emissaoOriginal, releitura, "Mesma emissão administrativa, conteúdo e versão");
                assertArrayEquals(pdf, gestor.pedido("GET", base(contrato) + "/documentos/" + documento + "/arquivo", null, null, 200).body());
                // O UUID da exportação assíncrona é metadado volátil por contrato atual; não é o job persistido acima.
                gestor.get("/api/relatorios/exportacoes/" + tarefaVolatil, 404);
                var baixa = gestor.pedido("GET", "/api/relatorios/terceirizados/exportacao?contratoId=" + c.contratoA.getId() + "&formato=XLSX", null, null, 200);
                assertTrue(baixa.body().length > 100); assertEquals((byte) 'P', baixa.body()[0]); assertEquals((byte) 'K', baixa.body()[1]);
                Banco controles = esperarQuietude();
                for (String tabela : List.of("contrato", "documento_comprobatorio", "fechamento_frequencia", "frequencia", "evento_frequencia", "emissao_relatorio_administrativo")) {
                    assertNotNull(antes.linhas().get(tabela), "Tabela do corpus precisa existir: " + tabela);
                    assertEquals(antes.linhas().get(tabela), controles.linhas().get(tabela), "Controle funcional preserva fatos/versões: " + tabela);
                }
                prova("rst004-controles", mapa("jobReleituraId", jobId, "tentativa", 1, "documentoSha256", hash(pdf),
                        "emissaoId", emissaoId, "novaExportacaoBytes", baixa.body().length, "novaExportacaoSha256", hash(baixa.body()),
                        "efeitosLegitimosDepoisDaComparacao", List.of("Login substitui a sessão do mesmo usuário", "Auditoria de login/baixa/exportação", "Transferência nova remove somente seu temporário"),
                        "metadadoVolatil", "UUID da exportação anterior retorna 404; seu arquivo foi comparado integralmente antes desta fase",
                        "limite", "Parada graciosa própria e mesmo banco/Redis/JAR; não comprova restauro de backup nem reboot do host. PIDs físicos do laboratório são vinculados pelo runner, sem leitura de credenciais pelo teste."));
            }
            semErrosOtimistas();
        } finally {
            encerrar();
            // A memória da tarefa antiga se perde no restart. Preserva-se a prova do arquivo e remove-se
            // somente este temporário conhecido do próprio corpus, após o último filho ter encerrado.
            if (relatorioPreservado != null) {
                assertTrue(relatorioPreservado.toAbsolutePath().normalize().startsWith(pasta.resolve("relatorios")));
                String sha = Files.exists(relatorioPreservado) ? hashArquivo(relatorioPreservado) : null;
                boolean removido = Files.deleteIfExists(relatorioPreservado);
                prova("rst004-limpeza-propria", mapa("arquivoPrivado", relatorioPreservado.toString(), "sha256", sha,
                        "removidoAposFilhos", removido, "provaIntegralConcluida", provaReinicio, "filhoVivo", filho != null && filho.isAlive()));
            }
        }
    }

    private JsonNode aguardar(Sessao s, String rota, String status, int segundos) throws Exception {
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(segundos);
        JsonNode ultimo;
        do {
            ultimo = s.get(rota, 200);
            if (status.equals(ultimo.path("status").asText())) return ultimo;
            assertFalse(Set.of("FALHA", "CANCELADO").contains(ultimo.path("status").asText()));
            Thread.sleep(50);
        } while (System.nanoTime() < fim);
        return fail("Tarefa real não atingiu " + status + " no prazo declarado de " + segundos + " s");
    }
}
