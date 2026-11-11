package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Autoria /root, revisão independente separada. Hipótese de atomicidade da
 * proibição já existente de duas concessões vigentes do mesmo tipo e escopo.
 * Dois pedidos válidos de administradores distintos concorrem.
 * A barreira PostgreSQL é a referência persistida, não um mock do repositório.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato9ConcessoesMesmoEscopoConcorrentesRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "escopo={0}")
    @ValueSource(strings = {"CONTRATO", "GESTAO"})
    @Timeout(value = 75, unit = TimeUnit.SECONDS)
    void duasConcessoesConcorrentesPreservamUmaUnicaVigenteNoMesmoEscopo(String escopo) throws Exception {
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao outroAdmin = emTransacao(() -> {
            var u = usuario(PerfilAcesso.ADMINISTRADOR, null); em.flush(); return u;
        });
        boolean contratual = escopo.equals("CONTRATO");
        long alvo = contratual ? c.contratoA.getId() : c.gestaoA.getId();
        long controle = contratual ? c.contratoB.getId() : c.gestaoB.getId();
        Class<?> entidade = contratual ? Contrato.class : GestaoLocal.class;
        String tabela = contratual ? "contrato" : "gestao_local";
        String atributo = contratual ? "contrato" : "gestaoLocal";
        Map<String, Object> corpo = corpo(c, contratual, alvo);
        assertThat(enviar(c.admin, "/api/permissoes-excepcionais", corpo(c, contratual, controle), 201,
                "Controle positivo: concessão institucional no outro escopo").path("id").asLong()).isPositive();
        JsonNode controleAntes = estado(c, atributo, controle);
        assertThat(controleAntes.size()).isEqualTo(1);
        assertThat(estado(c, atributo, alvo).size()).isZero();
        var pedidoA = autenticar(post("/api/permissoes-excepcionais")
                .contentType(APPLICATION_JSON).content(json(corpo)), c.admin);
        var pedidoB = autenticar(post("/api/permissoes-excepcionais")
                .contentType(APPLICATION_JSON).content(json(corpo)), outroAdmin);
        var pool = Executors.newFixedThreadPool(3);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        var atorA=new Ator("concessao-A");var atorB=new Ator("concessao-B");
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                assertThat(em.find(entidade, alvo, LockModeType.PESSIMISTIC_WRITE)).isNotNull();
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(40, TimeUnit.SECONDS)) throw new AssertionError("Referência não liberada");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> primeira = pool.submit(() -> atorA.executar(() -> mvc.perform(pedidoA).andReturn()));
            Future<MvcResult> segunda = pool.submit(() -> atorB.executar(() -> mvc.perform(pedidoB).andReturn()));
            var esperas = EsperaJdbcPostgres.aguardarAmbos(new JdbcTemplate(dataSource),conexao,
                    atorA,sql->sql.selecaoChave(tabela,"id_"+tabela,alvo),
                    atorB,sql->sql.selecaoChave(tabela,"id_"+tabela,alvo),tabela+"/"+alvo,primeira,segunda);
            assertThat(primeira.isDone()).isFalse();
            assertThat(segunda.isDone()).isFalse();
            liberar.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            var respostas = List.of(primeira.get(15, TimeUnit.SECONDS).getResponse().getStatus(),
                    segunda.get(15, TimeUnit.SECONDS).getResponse().getStatus());
            JsonNode depois = estado(c, atributo, alvo);
            JsonNode controleDepois = estado(c, atributo, controle);
            Path arquivo = Path.of("target/auditoria-http/candidato9-concessoes-mesmo-escopo-" + escopo + ".json");
            Files.createDirectories(arquivo.getParent());
            Files.writeString(arquivo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "autor", "/root", "escopo", escopo, "audit_round", System.getenv("SGT_AUDITORIA_RODADA"),
                    "recorded_at", Instant.now().toString(), "tabelaBloqueada", tabela, "registroBloqueado", alvo,
                    "administradoresDistintos", List.of(c.admin.getId(), outroAdmin.getId()),
                    "duasEsperasMySqlObservadas", true, "esperas", esperas, "status", respostas,
                    "concessoesNoEscopoAntes", 0, "concessoesNoEscopoDepois", depois,
                    "controleAntes", controleAntes, "controleDepois", controleDepois)));
            executar(c.admin, post("/api/permissoes-excepcionais").contentType(APPLICATION_JSON)
                    .content(json(corpo)), 409, "Repetição sequencial permanece conflito");
            assertAll("Concessão concorrente respeita unicidade vigente por beneficiário/tipo/escopo",
                    () -> assertThat(respostas).containsExactlyInAnyOrder(201, 409),
                    () -> assertThat(depois.size()).isEqualTo(1),
                    () -> assertThat(controleDepois).isEqualTo(controleAntes));
        } finally {
            liberar.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Map<String, Object> corpo(Cenario c, boolean contrato, long id) {
        return mapa("tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "usuarioBeneficiarioId", c.presidente.getId(),
                contrato ? "contratoId" : "gestaoLocalId", id,
                "finalidade", "Controle sintético de unicidade de concessão",
                "justificativa", "Duas solicitações válidas do mesmo ato sintético no mesmo escopo",
                "autorizacaoAdministrativa", "ATO-SINTETICO-CONCORRENCIA-C9",
                "validoAte", Instant.now().plusSeconds(7200));
    }

    private JsonNode estado(Cenario c, String atributo, long id) {
        return emTransacao(() -> {
            em.clear();
            return mapper.valueToTree(em.createQuery("select p.id,p.version,p.tipo,p.usuarioBeneficiario.id,"
                            + "p.ativo,p.concedidoEm,p.validoAte,p.finalidade,p.justificativa,p.autorizacaoAdministrativa "
                            + "from PermissaoExcepcional p where p.usuarioBeneficiario.id=:u and p.tipo=:t and p."
                            + atributo + ".id=:id order by p.id", Object[].class)
                    .setParameter("u", c.presidente.getId())
                    .setParameter("t", TipoPermissaoExcepcional.ACESSO_DADOS_PESSOAIS_RESTRITOS)
                    .setParameter("id", id).getResultList());
        });
    }


}
