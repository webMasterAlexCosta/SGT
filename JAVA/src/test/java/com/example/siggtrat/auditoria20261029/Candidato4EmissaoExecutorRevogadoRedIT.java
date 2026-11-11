package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Revisor transversal C4, hipótese C4-TR-H02. Proposta não executada pelo autor.
 * Manifesto: 13c84fd30b668335228704fd1028ddea11adad6f607458a7b7fa993dcbc64a89.
 * HTTP/PostgreSQL reais, sem mocks de persistência ou autorização e sem DDL manual. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato4EmissaoExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void emissaoRevalidaExecutorAposEsperarPelasFontes(boolean rebaixarPerfil) throws Exception {
        Cenario c = criarCenarioPersistido();
        String rota = "/api/relatorios/administrativos/emissoes";
        Map<String,Object> controle = pedido(c, "controle-" + UUID.randomUUID());
        enviar(c.localA, rota, controle, 201, "Controle positivo: usuário emite no próprio escopo antes da revogação");
        Estado antes = estado(c.localA.getId());
        String chave = "c4-tr-emissao-" + UUID.randomUUID();
        var requisicao = autenticar(post(rota).contentType(APPLICATION_JSON).content(json(pedido(c, chave))), c.localA);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, c.contratoA.getId(), LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(30, TimeUnit.SECONDS)) throw new AssertionError("Limite da fixture: fonte não liberada");
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            var ator = new Ator("emissao-relatorio-administrativo");
            Future<MvcResult> tentativa = pool.submit(() -> ator.executar(() -> mvc.perform(requisicao).andReturn()));
            var espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource), conexao, ator,
                    sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()),
                    "Emissão administrativa: Contrato da fixture", tentativa);
            Map<String,Object> revogacao = rebaixarPerfil
                    ? mapa("perfil", "OPERACIONAL", "gestaoLocalId", c.gestaoA.getId(),
                            "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId())
                    : mapa("active", false);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(APPLICATION_JSON)
                    .content(json(revogacao)), 200, "Admin revoga emissão antes da liberação da fonte");
            emTransacao(() -> {
                UsuarioAplicacao u = em.find(UsuarioAplicacao.class, c.localA.getId());
                if (rebaixarPerfil) assertThat(u.getPerfil()).isEqualTo(PerfilAcesso.OPERACIONAL);
                else assertThat(u.getActive()).isFalse();
                return null;
            });
            assertThat(tentativa.isDone()).isFalse();
            liberar.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(c.localA.getId());
            Path evidencia = Path.of("target/auditoria-http/candidato4-emissao-executor-" + rebaixarPerfil + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "hipotese", "C4-TR-H02", "rebaixarPerfil", rebaixarPerfil,
                    "esperaPostgresObservada", espera, "revogacaoCommitadaAntesDaLiberacao", true,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("Autorização atual governa emissão mesmo após aguardar fontes",
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Sem nova emissão nem superação da prova anterior").isEqualTo(antes));
        } finally { liberar.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS); }
    }

    private Map<String,Object> pedido(Cenario c, String chave) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO", "inicio", DATA,
                "fim", DATA, "contratoIds", List.of(c.contratoA.getId()), "chaveIdempotencia", chave);
    }

    private Estado estado(long usuarioId) {
        return emTransacao(() -> new Estado(
                em.createQuery("select count(e) from EmissaoRelatorioAdministrativo e where e.usuarioId=:u", Long.class)
                        .setParameter("u", usuarioId).getSingleResult(),
                em.createQuery("select count(e) from EmissaoRelatorioAdministrativo e where e.usuarioId=:u and e.superadaEm is not null", Long.class)
                        .setParameter("u", usuarioId).getSingleResult()));
    }

    private record Estado(long emissoes, long superadas) { }
}
