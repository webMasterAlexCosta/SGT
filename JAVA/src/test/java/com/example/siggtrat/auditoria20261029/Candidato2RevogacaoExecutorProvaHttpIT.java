package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoContratual;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Hipótese independente C2-PES-03, derivada da leitura do candidato2 congelado.
 * Não usa mocks de autorização ou persistência. A espera no Contrato é observada
 * no PostgreSQL pg_stat_activity/pg_locks antes de desativar/rebaixar o executor por HTTP.
 * A concessão excepcional continua ativa: revogar a identidade/perfil do seu
 * beneficiário deve impedir um novo ato após a revogação administrativa concluir.
 * Não executado pelo revisor; reservado à rodada real serializada pela raiz.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato2RevogacaoExecutorProvaHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void executorRevogadoEnquantoAguardaContratoNaoConfirmaProva(boolean rebaixarPerfil) throws Exception {
        Cenario c = criarCenarioPersistido();
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        String rota = "/api/pessoas/" + c.pessoaA.getId() + "/provas-nome";
        Map<String, Object> req = mapa("vinculoId", c.vinculoA.getId(),
                "nomeComprovado", "Nome documental aguardando autorização atual",
                "dataInicioEfetiva", DATA, "dataFimEfetiva", DATA.plusDays(1),
                "referenciaSei", "SEI-SINTETICO-C2-PES-03-" + UUID.randomUUID(),
                "motivo", "Fonte sintética explícita para testar revogação concorrente do executor",
                "autorizacao", "ATO-SINTETICO-C2-PES-03", "chaveIdempotencia", UUID.randomUUID().toString(),
                "versaoPessoaEsperada", consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(),
                        "Versão anterior à confirmação").path("version").asLong(),
                "versaoVinculoEsperada", consultar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId(),
                        "Versão anterior à confirmação").path("version").asLong());
        JsonNode previa = enviar(c.localA, rota + "/previa", req, 200,
                "Executor inicialmente ativo, Gestor Local e beneficiário da concessão específica");
        req.put("confirmarImpacto", true);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        var pedido = autenticar(post(rota).contentType(MediaType.APPLICATION_JSON).content(json(req)), c.localA);
        Estado antes = estado(c);

        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberarContrato = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, c.contratoA.getId(), LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberarContrato.await(25, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: Contrato não liberado");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            var ator = new Ator("confirmacao-prova-nome");
            Future<MvcResult> tentativa = pool.submit(() -> ator.executar(() -> mvc.perform(pedido).andReturn()));
            var espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource), conexao, ator,
                    sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()),
                    "Confirmação de prova nominal: Contrato da fixture", tentativa);
            var evidenciaEspera = java.nio.file.Path.of("target/auditoria-http/ator-prova-nome-" + rebaixarPerfil + ".json");
            java.nio.file.Files.createDirectories(evidenciaEspera.getParent());
            java.nio.file.Files.writeString(evidenciaEspera,
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                            "contratoId", c.contratoA.getId(), "esperaPostgresObservada", espera)));


            Map<String, Object> revogacao = rebaixarPerfil
                    ? mapa("perfil", "OPERACIONAL", "gestaoLocalId", c.gestaoA.getId(),
                            "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId())
                    : mapa("active", false);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(json(revogacao)), 200,
                    "Admin conclui a revogação enquanto a confirmação ainda aguarda o Contrato");
            emTransacao(() -> {
                UsuarioAplicacao persistido = em.find(UsuarioAplicacao.class, c.localA.getId());
                if (rebaixarPerfil) assertThat(persistido.getPerfil()).isEqualTo(PerfilAcesso.OPERACIONAL);
                else assertThat(persistido.getActive()).isFalse();
                return null;
            });
            assertThat(tentativa.isDone()).as("Confirmação ainda bloqueada após revogação commitada").isFalse();
            liberarContrato.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);

            // Não se exige uma mensagem específica: a origem da recusa pode ser
            // sessão, perfil, custódia ou versão de autorização, sempre sem mutação.
            assertThat(resultado.getResponse().getStatus())
                    .as("Executor %s antes do ato deve ser revalidado após a espera. Resposta: %s",
                            rebaixarPerfil ? "rebaixado" : "desativado", resultado.getResponse().getContentAsString())
                    .isIn(401, 403, 404, 409);
            assertThat(estado(c)).as("Recusa não publica prova, evento, uso da concessão ou incremento de versão")
                    .isEqualTo(antes);
        } finally {
            liberarContrato.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Estado estado(Cenario c) {
        return emTransacao(() -> new Estado(
                em.find(Pessoa.class, c.pessoaA.getId()).getVersion(),
                em.find(VinculoContratual.class, c.vinculoA.getId()).getVersion(),
                em.createQuery("select count(p) from ProvaNomePessoaHistorico p where p.vinculo.id=:v", Long.class)
                        .setParameter("v", c.vinculoA.getId()).getSingleResult(),
                em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                        .setParameter("v", c.vinculoA.getId()).getSingleResult(),
                em.createQuery("select count(u) from UsoPermissaoExcepcional u where u.usuarioId=:u "
                                + "and u.tipoRecurso='PROVA_NOME_HISTORICO'", Long.class)
                        .setParameter("u", c.localA.getId()).getSingleResult()));
    }

    private record Estado(Long pessoaVersion, Long vinculoVersion, long provas, long eventos, long usos) {}
}
