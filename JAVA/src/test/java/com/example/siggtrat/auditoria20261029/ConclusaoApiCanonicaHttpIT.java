package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.siggtrat.entidade.HistoricoEmpregado;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import jakarta.persistence.LockModeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** O fluxo Pessoa/Vínculo é a única entrada de escrita de identidade/alocação. */
class ConclusaoApiCanonicaHttpIT extends AuditoriaHttpSuporte {
    @Autowired FilaRegistroAuditoria fila;
    @Test
    void rotasDeEscritaCompatibilidadeECrudDescontinuadoNaoSaoMapeadas() {
        Set<String> raizes = Set.of("/api/bancos", "/api/agencias", "/api/contas",
                "/api/tipos-conta", "/api/enderecos", "/api/telefones");
        List<String> obsoletas = new ArrayList<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            for (String path : mapping.getPatternValues()) {
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    boolean cadastroAbandonado = raizes.stream()
                            .anyMatch(raiz -> path.equals(raiz) || path.startsWith(raiz + "/"));
                    boolean escritaEmpregado = (path.equals("/api/empregados")
                            || path.equals("/api/empregados/{id}"))
                            && Set.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE).contains(method);
                    if (cadastroAbandonado || escritaEmpregado) obsoletas.add(method + " " + path);
                }
            }
        });
        assertThat(obsoletas).as("APIs operacionais antigas efetivamente removidas do Spring").isEmpty();
    }

    @Test
    void dadosOpcionaisCanonicosPreservamEnderecoIdentidadeEVinculoEExigemEscopo() throws Exception {
        Cenario c = criarCenario();
        String rota = "/api/pessoas/" + c.pessoaA.getId() + "/dados-opcionais";
        Map<String, Object> comando = Map.of("rg", "RG-CANONICO", "email", "canonica@example.test",
                "endereco", Map.of("logradouro", "Rua Canônica", "numero", "10", "bairro", "Centro",
                        "cidade", "Cidade", "uf", "SP", "cep", "12345678"));
        JsonNode alterada = dados(executar(c.localA, put(rota).contentType(MediaType.APPLICATION_JSON)
                .content(json(comando)), 200, "COL003 COL011 atualização canônica de opcionais"));
        assertThat(alterada.path("id").asLong()).isEqualTo(c.pessoaA.getId());
        assertThat(alterada.path("nome").asText()).isEqualTo(c.pessoaA.getNome());
        assertThat(alterada.path("cpf").asText()).isEqualTo(c.pessoaA.getCpf());
        assertThat(alterada.path("rg").asText()).isEqualTo("RG-CANONICO");
        assertThat(alterada.path("email").asText()).isEqualTo("canonica@example.test");
        assertThat(alterada.path("endereco").path("logradouro").asText()).isEqualTo("RUA CANÔNICA");
        JsonNode vinculo = dados(executar(c.localA, get("/api/vinculos-contratuais/" + c.vinculoA.getId()),
                200, "O cadastro opcional não altera o contexto negocial"));
        assertThat(vinculo.path("pessoaId").asLong()).isEqualTo(c.pessoaA.getId());
        assertThat(vinculo.path("contratoId").asLong()).isEqualTo(c.contratoA.getId());
        executar(c.localB, put(rota).contentType(MediaType.APPLICATION_JSON).content(json(comando)),
                404, "Outra Gestão não altera os dados pessoais");
        executar(c.nacional, put(rota).contentType(MediaType.APPLICATION_JSON).content(json(comando)),
                403, "Consulta nacional não concede escrita");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void atualizacaoCanonicaRegistraAntesDepoisAutorDataEfetivaESomenteAposCommit() throws Exception {
        Cenario c = criarCenarioPersistido();
        String rota = "/api/pessoas/" + c.pessoaA.getId() + "/dados-opcionais";
        String antes = "RG-ANTES-" + c.pessoaA.getId();
        String depois = "RG-DEPOIS-" + c.pessoaA.getId();
        emTransacao(() -> { em.find(Pessoa.class, c.pessoaA.getId()).setRg(antes); return null; });
        String requestId = UUID.randomUUID().toString();
        executar(c.localA, put(rota).header("X-Request-Id", requestId)
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("rg", depois))),
                200, "AUD001 alteração canônica deve possuir prova de antes/depois, autor e datas");
        fila.descarregar();
        List<RegistroAuditoria> registros = emTransacao(() -> em.createQuery(
                        "select a from RegistroAuditoria a where a.requestId=:rid and a.action='PESSOA_ATUALIZACAO_OPCIONAL'",
                        RegistroAuditoria.class).setParameter("rid", requestId).getResultList());
        assertThat(registros).as("A rota canônica deve registrar a ação confirmada").hasSize(1);
        RegistroAuditoria auditoria = registros.get(0);
        assertThat(auditoria.getUserId()).isEqualTo(c.localA.getId());
        assertThat(auditoria.getEntityId()).isEqualTo(c.pessoaA.getId());
        JsonNode detalhe = mapper.readTree(auditoria.getDetailsJson());
        assertThat(detalhe.path("antes").path("rg").asText()).isEqualTo(antes);
        assertThat(detalhe.path("depois").path("rg").asText()).isEqualTo(depois);
        assertThat(detalhe.path("dataEfetiva").asText()).isEqualTo(RelogioSistema.hoje().toString());
        assertThat(detalhe.path("motivo").asText()).isNotBlank();
        List<HistoricoEmpregado> historicos = emTransacao(() -> em.createQuery(
                        "select h from HistoricoEmpregado h where h.empregado.id=:id order by h.id", HistoricoEmpregado.class)
                .setParameter("id", c.empregadoA.getId()).getResultList());
        assertThat(historicos).anySatisfy(h -> {
            assertThat(h.getDescricao()).contains(antes, depois);
            assertThat(h.getHashIntegridade()).startsWith("HMAC-SHA256:");
            assertThat(h.getDataEvento()).isNotNull();
            assertThat(h.getDataEfetiva()).isEqualTo(RelogioSistema.hoje());
        });
        long quantidade = historicos.size();
        String rejeitada = UUID.randomUUID().toString();
        executar(c.localA, put(rota).header("X-Request-Id", rejeitada).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("rg", "NAO-CONFIRMADO", "endereco", Map.of("logradouro", "")))),
                400, "Rollback não confirma dados nem publica prova de atualização bem-sucedida");
        fila.descarregar();
        emTransacao(() -> {
            assertThat(em.find(Pessoa.class, c.pessoaA.getId()).getRg()).isEqualTo(depois);
            assertThat(em.createQuery("select count(h) from HistoricoEmpregado h where h.empregado.id=:id", Long.class)
                    .setParameter("id", c.empregadoA.getId()).getSingleResult()).isEqualTo(quantidade);
            assertThat(em.createQuery("select count(a) from RegistroAuditoria a where a.requestId=:rid and a.action='PESSOA_ATUALIZACAO_OPCIONAL'", Long.class)
                    .setParameter("rid", rejeitada).getSingleResult()).isZero();
            return null;
        });
    }

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void atualizacoesOpcionaisConcorrentesPreservamCampoOmitidoAposEsperaRealNoMysql() throws Exception {
        Cenario c = criarCenarioPersistido();
        executar(c.localA, get("/api/pessoas/" + c.pessoaA.getId()), 200,
                "Preparar sessão antes da disputa transacional");
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        CountDownLatch liberar = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> primeira = pool.submit(() -> emTransacao(() -> {
                Pessoa pessoa = em.find(Pessoa.class, c.pessoaA.getId(), LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(30, TimeUnit.SECONDS)) throw new AssertionError("Não houve espera pela Pessoa bloqueada.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                pessoa.setCtpsSerie("CTPS-CONFIRMADA-PRIMEIRO");
                return null;
            }));
            long bloqueadora = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<?> segunda = pool.submit(() -> executar(c.localA,
                    put("/api/pessoas/" + c.pessoaA.getId() + "/dados-opcionais")
                            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("rg", "RG-SEGUNDA-TRANSACAO"))),
                    200, "COL011 campos omitidos devem preservar a versão confirmada sob concorrência"));
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean aguardou = false;
            while (System.nanoTime() < limite && !segunda.isDone()) {
                long esperas = emTransacao(() -> ((Number) em.createNativeQuery("""
SELECT count(*) FROM pg_stat_activity a WHERE a.datname=current_database() AND :conexao = ANY(pg_blocking_pids(a.pid))
""").setParameter("conexao", bloqueadora).getSingleResult()).longValue());
                if (esperas > 0) { aguardou = true; break; }
                TimeUnit.MILLISECONDS.sleep(20);
            }
            assertThat(aguardou).as("A segunda conexão deve aguardar a primeira no PostgreSQL real").isTrue();
            liberar.countDown();
            primeira.get(20, TimeUnit.SECONDS);
            segunda.get(20, TimeUnit.SECONDS);
            emTransacao(() -> {
                Pessoa pessoa = em.find(Pessoa.class, c.pessoaA.getId());
                assertThat(pessoa.getCtpsSerie()).isEqualTo("CTPS-CONFIRMADA-PRIMEIRO");
                assertThat(pessoa.getRg()).isEqualTo("RG-SEGUNDA-TRANSACAO");
                return null;
            });
        } finally {
            liberar.countDown(); pool.shutdownNow();
        }
    }
}
