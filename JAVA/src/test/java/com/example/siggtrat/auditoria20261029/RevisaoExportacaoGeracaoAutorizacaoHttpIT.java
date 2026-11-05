package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Executor, gerador XLSX e MySQL reais; a barreira observa SQL sem substituir domínio ou persistência. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RevisaoExportacaoGeracaoAutorizacaoHttpIT extends ContratosHttpBase {
    private static final Path ARQUIVOS = Path.of("target/auditoria-http/exportacao-autorizacao-geracao").toAbsolutePath();

    @DynamicPropertySource
    static void configuracao(DynamicPropertyRegistry propriedades) {
        propriedades.add("spring.jpa.properties.hibernate.session_factory.statement_inspector", BarreiraSql.class::getName);
        propriedades.add("app.relatorios.temp.diretorio", ARQUIVOS::toString);
    }

    public static class BarreiraSql implements StatementInspector {
        static final AtomicReference<Barreira> proxima = new AtomicReference<>();

        @Override public String inspect(String sql) {
            if (Thread.currentThread().getName().startsWith("relatorio-exportacao-")
                    && sql.toLowerCase(Locale.ROOT).contains(" from frequencia ")) {
                Barreira barreira = proxima.getAndSet(null);
                if (barreira != null) {
                    barreira.entrou().countDown();
                    try {
                        if (!barreira.liberar().await(20, TimeUnit.SECONDS))
                            throw new IllegalStateException("A alteração de autorização não liberou a apuração real.");
                    } catch (InterruptedException erro) {
                        Thread.currentThread().interrupt(); throw new IllegalStateException(erro);
                    }
                }
            }
            return sql;
        }
    }

    record Barreira(CountDownLatch entrou, CountDownLatch liberar) { }

    @ParameterizedTest(name = "alteracaoDuranteGeracao={0}")
    @ValueSource(strings = {"GESTAO", "PERFIL_MINIMIZADO"})
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void mudarERestaurarAutorizacaoDuranteGeracaoNaoPublicaArquivoDePoliticaIncerta(String mudanca) throws Exception {
        var c = criarCenarioPersistido();
        enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA, "status", "FALTA", "justificativaRetificacao", "Fonte da geração concorrente",
                "chaveIdempotencia", "geracao-permissao-" + UUID.randomUUID()), 200, "Fonte real da exportação");
        Files.createDirectories(ARQUIVOS);
        Set<Path> arquivosAntes = arquivos();
        long versaoAntes = emTransacao(() -> em.find(UsuarioAplicacao.class, c.localA.getId()).getVersion());
        Barreira barreira = new Barreira(new CountDownLatch(1), new CountDownLatch(1));
        assertThat(BarreiraSql.proxima.compareAndSet(null, barreira)).isTrue();
        String id;
        try {
            id = solicitar(c).path("id").asText();
            assertThat(barreira.entrou().await(15, TimeUnit.SECONDS))
                    .as("O executor real precisa alcançar a apuração SQL depois de capturar a autorização").isTrue();
            emTransacao(() -> {
                UsuarioAplicacao usuario = em.find(UsuarioAplicacao.class, c.localA.getId());
                if (mudanca.equals("GESTAO")) {
                    GestaoLocal destino = em.find(GestaoLocal.class, c.gestaoB.getId());
                    usuario.setGestaoLocal(destino); usuario.setSuperintendencia(destino.getSuperintendencia());
                } else {
                    usuario.setPerfil(PerfilAcesso.PRESIDENTE);
                    usuario.setGestaoLocal(null); usuario.setSuperintendencia(null);
                }
                return null;
            });
            executar(c.localA, get("/api/relatorios/exportacoes/{id}", id), 403,
                    "Nem o progresso da tarefa mantém a autorização anterior enquanto o usuário está revogado");
            emTransacao(() -> {
                UsuarioAplicacao usuario = em.find(UsuarioAplicacao.class, c.localA.getId());
                GestaoLocal origem = em.find(GestaoLocal.class, c.gestaoA.getId());
                usuario.setPerfil(PerfilAcesso.GESTOR_LOCAL); usuario.setGestaoLocal(origem);
                usuario.setSuperintendencia(origem.getSuperintendencia());
                return null;
            });
            assertThat(emTransacao(() -> em.find(UsuarioAplicacao.class, c.localA.getId()).getVersion()))
                    .as("Duas mudanças confirmadas não desaparecem da revisão de concorrência quando a política é restaurada")
                    .isGreaterThan(versaoAntes);
        } finally {
            BarreiraSql.proxima.compareAndSet(barreira, null);
            barreira.liberar().countDown();
        }
        JsonNode recusada = aguardarFim(c, id);
        assertThat(recusada.path("status").asText()).isEqualTo("FALHA");
        assertThat(recusada.path("podeBaixar").asBoolean()).isFalse();
        assertThat(recusada.path("mensagem").asText()).contains("autorização foi alterada durante a geração");
        executar(c.localA, get("/api/relatorios/exportacoes/{id}/arquivo", id), 409,
                "Não há arquivo liberado quando a autorização mudou durante a apuração");
        assertThat(arquivos()).as("Arquivo parcial ou final de política incerta é descartado antes de publicar FALHA")
                .isEqualTo(arquivosAntes);

        String controle = solicitar(c).path("id").asText();
        assertThat(aguardarFim(c, controle).path("status").asText()).isEqualTo("CONCLUIDO");
        byte[] bytes = executar(c.localA, get("/api/relatorios/exportacoes/{id}/arquivo", controle), 200,
                "Uma nova geração sob autorização estável continua disponível").getResponse().getContentAsByteArray();
        assertThat(bytes.length).isGreaterThan(100);
        assertThat(bytes[0]).isEqualTo((byte) 'P'); assertThat(bytes[1]).isEqualTo((byte) 'K');
        assertThat(consultar(c.localA, "/api/relatorios/exportacoes/" + controle,
                "Só a geração estável foi transferida").path("status").asText()).isEqualTo("BAIXADO");
    }

    private JsonNode solicitar(Cenario c) throws Exception {
        return dados(executar(c.localA, post("/api/relatorios/frequencia-consolidada/exportacao/solicitacao")
                .param("inicio", DATA.toString()).param("fim", DATA.toString())
                .param("contratoIds", c.contratoA.getId().toString()).param("formato", "XLSX"),
                202, "Geração assíncrona real para prova de autorização"));
    }

    private JsonNode aguardarFim(Cenario c, String id) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        JsonNode estado = null;
        do {
            estado = consultar(c.localA, "/api/relatorios/exportacoes/" + id, "Conclusão do executor real");
            if (Set.of("CONCLUIDO", "FALHA").contains(estado.path("status").asText())) return estado;
            Thread.sleep(50);
        } while (System.nanoTime() < limite);
        return fail("Exportação não concluiu em 20s: " + estado);
    }

    private Set<Path> arquivos() throws Exception {
        try (var arquivos = Files.list(ARQUIVOS)) { return arquivos.collect(Collectors.toSet()); }
    }
}
