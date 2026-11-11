package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.infra.ObservadorAtorPostgres;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

/** Contraprovas reais do observador, separadas da aceitação dos fluxos HTTP. */
@Transactional(propagation=Propagation.NOT_SUPPORTED)
@Timeout(50)
class ObservacaoAtoresLocksPostgresIT extends ContratosHttpBase {
    @Autowired DataSource dataSource;
    @Autowired ContratoRepositorio contratos;
    @Autowired UsuarioAplicacaoRepositorio usuarios;
    private static final String CONTRATO="ContratoRepositorio.buscarPorIdParaAtualizacao";
    private static final String USUARIO="UsuarioAplicacaoRepositorio.buscarPorUsernameParaAtualizacao";
    private void aguardar(BooleanSupplier condicao) throws Exception {
        long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
        while(System.nanoTime()<fim) { if(condicao.getAsBoolean()) return; Thread.sleep(20); }
        fail("Espera física dos atores/alvos não observada");
    }
    @Test void observaCadeiaRealRaizPrimeiroSegundoSemContarAtorEstranho() throws Exception {
        var c=criarCenarioPersistido(); var jdbc=new JdbcTemplate(dataSource);
        var liberar=new CountDownLatch(1); var raiz=new CompletableFuture<Long>();
        var a=new Ator("primeiro"); var b=new Ator("segundo"); var estranho=new Ator("nao-executado");
        var pool=Executors.newFixedThreadPool(3);
        try {
            var dono=pool.submit(()->emTransacao(()->{
                em.find(Contrato.class,c.contratoA.getId(),LockModeType.PESSIMISTIC_WRITE);
                raiz.complete(((Number)em.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).longValue());
                esperarLiberacao(liberar); return null;
            }));
            long pidRaiz=raiz.get(10,TimeUnit.SECONDS);
            var primeiro=pool.submit(()->a.executar(()->emTransacao(()->{
                em.find(Contrato.class,c.contratoB.getId(),LockModeType.PESSIMISTIC_WRITE);
                return contratos.buscarPorIdParaAtualizacao(c.contratoA.getId()).orElseThrow().getId();
            })));
            aguardar(()->ObservadorAtorPostgres.esperaDiretaNoAlvo(jdbc,pidRaiz,a,CONTRATO,c.contratoA.getId(),"contrato"));
            assertNotNull(a.comandoAtual(),"A prova deve capturar o comando JDBC efetivamente em espera");
            assertTrue(a.comandoAtual().selecaoChave("contrato","id_contrato",c.contratoA.getId()),
                    ()->"SQL preparado="+a.comandoAtual().sql()+" fingerprints="+a.comandoAtual().conjuntos());
            var segundo=pool.submit(()->b.executar(()->emTransacao(()->contratos.buscarPorIdParaAtualizacao(c.contratoB.getId()).orElseThrow().getId())));
            aguardar(()->!EsperaJdbcPostgres.observarAmbos(jdbc,pidRaiz,a,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoA.getId()),b,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoB.getId()),"cadeia A/B").isEmpty());
            assertFalse(EsperaJdbcPostgres.observarAmbos(jdbc,pidRaiz,a,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoA.getId()),b,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoB.getId()),"cadeia A/B").isEmpty());
            assertTrue(EsperaJdbcPostgres.observarAmbos(jdbc,pidRaiz,a,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoA.getId()),b,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoA.getId()),"chave incorreta B").isEmpty());
            assertTrue(EsperaJdbcPostgres.observarAmbos(jdbc,pidRaiz,a,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoA.getId()),estranho,
                    sql->true,"ator estranho").isEmpty());
            assertNotEquals(a.operacaoAtual().pid(),b.operacaoAtual().pid());
            assertTrue(ObservadorAtorPostgres.esperaDiretaNoAlvo(jdbc,a.operacaoAtual().pid(),b,CONTRATO,c.contratoB.getId(),"contrato"));
            assertFalse(ObservadorAtorPostgres.esperaDiretaNoAlvo(jdbc,pidRaiz,b,CONTRATO,c.contratoB.getId(),"contrato"));
            liberar.countDown(); dono.get(10,TimeUnit.SECONDS);
            assertEquals(c.contratoA.getId(),primeiro.get(10,TimeUnit.SECONDS));
            assertEquals(c.contratoB.getId(),segundo.get(10,TimeUnit.SECONDS));
        } finally { liberar.countDown();pool.shutdown();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS)); }
    }
    @Test void lockConcedidoEmUsuarioNaoProvaQueEsperaAtualSejaNesseAlvo() throws Exception {
        var c=criarCenarioPersistido();var jdbc=new JdbcTemplate(dataSource);
        var liberar=new CountDownLatch(1);var raiz=new CompletableFuture<Long>();var ator=new Ator("consulta-outro-alvo");
        var pool=Executors.newFixedThreadPool(2);
        try {
            var dono=pool.submit(()->emTransacao(()->{
                em.find(UsuarioAplicacao.class,c.operacionalA.getId(),LockModeType.PESSIMISTIC_WRITE);
                em.find(Contrato.class,c.contratoA.getId(),LockModeType.PESSIMISTIC_WRITE);
                raiz.complete(((Number)em.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).longValue());
                esperarLiberacao(liberar);return null;
            }));
            long pidRaiz=raiz.get(10,TimeUnit.SECONDS);
            var tentativa=pool.submit(()->ator.executar(()->emTransacao(()->{
                usuarios.findById(c.operacionalA.getId()).orElseThrow(); // MVCC: AccessShare concedido.
                return contratos.buscarPorIdParaAtualizacao(c.contratoA.getId()).orElseThrow().getId();
            })));
            aguardar(()->ObservadorAtorPostgres.esperaDiretaNoAlvo(jdbc,pidRaiz,ator,CONTRATO,c.contratoA.getId(),"contrato"));
            assertNotNull(ator.comandoAtual());
            assertTrue(ator.comandoAtual().selecaoChave("contrato","id_contrato",c.contratoA.getId()),
                    ()->"SQL preparado="+ator.comandoAtual().sql()+" fingerprints="+ator.comandoAtual().conjuntos());
            assertFalse(ator.comandoAtual().selecaoId("usuario_aplicacao",c.operacionalA.getId()));
            assertFalse(EsperaJdbcPostgres.observar(jdbc,pidRaiz,ator,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoA.getId()),"contrato A").isEmpty());
            assertTrue(EsperaJdbcPostgres.observar(jdbc,pidRaiz,ator,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoB.getId()),"contrato B").isEmpty());
            assertTrue(EsperaJdbcPostgres.observar(jdbc,pidRaiz,ator,
                    sql->sql.selecaoId("usuario_aplicacao",c.operacionalA.getId()),"usuario já lido").isEmpty());
            Long predicadoAntigo=jdbc.queryForObject("""
                SELECT count(*) FROM pg_stat_activity a WHERE a.pid=? AND ?=ANY(pg_blocking_pids(a.pid))
                  AND EXISTS(SELECT 1 FROM pg_locks l JOIN pg_class c ON c.oid=l.relation
                             WHERE l.pid=a.pid AND c.relname='usuario_aplicacao')
                """,Long.class,ator.operacaoAtual().pid(),pidRaiz);
            assertEquals(1L,predicadoAntigo,"Predicado antigo aceita relação já lida, embora espera seja em contrato");
            assertFalse(ObservadorAtorPostgres.esperaDiretaNoAlvo(jdbc,pidRaiz,ator,USUARIO,c.operacionalA.getUsername(),"usuario_aplicacao"));
            assertFalse(ObservadorAtorPostgres.esperaDiretaNoAlvo(jdbc,pidRaiz,ator,CONTRATO,c.contratoB.getId(),"contrato"));
            liberar.countDown();dono.get(10,TimeUnit.SECONDS);assertEquals(c.contratoA.getId(),tentativa.get(10,TimeUnit.SECONDS));
        } finally {liberar.countDown();pool.shutdown();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
    }
    @Test void intermediarioDesconhecidoExecutadoNaoPodeComprovarCadeiaDosAtores() throws Exception {
        var c=criarCenarioPersistido();var jdbc=new JdbcTemplate(dataSource);
        var liberar=new CountDownLatch(1);var raiz=new CompletableFuture<Long>();var estranhoPid=new CompletableFuture<Long>();
        var a=new Ator("conhecido-A");var b=new Ator("conhecido-B");var pool=Executors.newFixedThreadPool(4);
        try {
            var dono=pool.submit(()->emTransacao(()->{
                em.find(Contrato.class,c.contratoA.getId(),LockModeType.PESSIMISTIC_WRITE);
                raiz.complete(((Number)em.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).longValue());
                esperarLiberacao(liberar);return null;
            }));
            long pidRaiz=raiz.get(10,TimeUnit.SECONDS);
            // Esta sessão executa de verdade, mas não pertence aos atores autorizados do gate.
            var desconhecido=pool.submit(()->emTransacao(()->{
                em.find(Contrato.class,c.contratoB.getId(),LockModeType.PESSIMISTIC_WRITE);
                estranhoPid.complete(((Number)em.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).longValue());
                return contratos.buscarPorIdParaAtualizacao(c.contratoA.getId()).orElseThrow().getId();
            }));
            long pidEstranho=estranhoPid.get(10,TimeUnit.SECONDS);
            aguardar(()->Boolean.TRUE.equals(jdbc.queryForObject(
                    "select ?=ANY(pg_blocking_pids(cast(? as integer)))",Boolean.class,pidRaiz,pidEstranho)));
            var primeiro=pool.submit(()->a.executar(()->emTransacao(()->
                    contratos.buscarPorIdParaAtualizacao(c.contratoB.getId()).orElseThrow().getId())));
            var segundo=pool.submit(()->b.executar(()->emTransacao(()->
                    contratos.buscarPorIdParaAtualizacao(c.contratoA.getId()).orElseThrow().getId())));
            aguardar(()->!provaFisicaIncluindoDesconhecido(jdbc,pidRaiz,pidEstranho,a,b,
                    c.contratoB.getId(),c.contratoA.getId()).isEmpty());
            var premissa=provaFisicaIncluindoDesconhecido(jdbc,pidRaiz,pidEstranho,a,b,
                    c.contratoB.getId(),c.contratoA.getId());
            assertFalse(premissa.isEmpty(),"Ambos os comandos exatos precisam estar fisicamente bloqueados antes da negativa");
            System.out.println("ATORES_CONTRAPROVA_CAMINHOS "+premissa);
            assertTrue(Boolean.TRUE.equals(jdbc.queryForObject(
                    "select ?=ANY(pg_blocking_pids(cast(? as integer)))",Boolean.class,pidRaiz,pidEstranho)),
                    "A sessão desconhecida realmente espera a raiz, não é apenas um ator não executado.");
            assertTrue(EsperaJdbcPostgres.observarAmbos(jdbc,pidRaiz,a,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoB.getId()),b,
                    sql->sql.selecaoChave("contrato","id_contrato",c.contratoA.getId()),"cadeia com terceiro desconhecido").isEmpty(),
                    "O caminho passa por PID desconhecido e deve ser recusado, mesmo chegando à raiz.");
            liberar.countDown();dono.get(10,TimeUnit.SECONDS);
            assertEquals(c.contratoA.getId(),desconhecido.get(10,TimeUnit.SECONDS));
            assertEquals(c.contratoB.getId(),primeiro.get(10,TimeUnit.SECONDS));
            assertEquals(c.contratoA.getId(),segundo.get(10,TimeUnit.SECONDS));
        } finally {liberar.countDown();pool.shutdown();assertTrue(pool.awaitTermination(20,TimeUnit.SECONDS));}
    }
    private java.util.List<java.util.Map<String,Object>> provaFisicaIncluindoDesconhecido(
            JdbcTemplate jdbc,long raiz,long desconhecido,Ator a,Ator b,long chaveA,long chaveB) {
        var ca=a.comandoAtual();var cb=b.comandoAtual();
        if(ca==null||cb==null||!ca.selecaoChave("contrato","id_contrato",chaveA)
                ||!cb.selecaoChave("contrato","id_contrato",chaveB))return java.util.List.of();
        var linhas=jdbc.queryForList("""
            WITH RECURSIVE caminhos(origem,pid,caminho) AS (
                SELECT a.pid,a.pid,ARRAY[a.pid] FROM pg_stat_activity a
                 WHERE a.datname=current_database() AND a.pid IN (?,?) AND a.wait_event_type='Lock'
                UNION ALL
                SELECT c.origem,p.pid,c.caminho||p.pid FROM caminhos c
                  CROSS JOIN LATERAL unnest(pg_blocking_pids(c.pid)) AS p(pid)
                 WHERE c.pid<>? AND p.pid IN (?,?,?,?) AND NOT p.pid=ANY(c.caminho)
            ) SELECT a.pid AS solicitante,c.caminho::text AS caminho_ate_raiz,
                     a.wait_event_type,a.wait_event,l.locktype AS recurso,l.granted AS concedido
                FROM caminhos c JOIN pg_stat_activity a ON a.pid=c.origem
                JOIN pg_locks l ON l.pid=a.pid AND NOT l.granted WHERE c.pid=?
            """,ca.pid(),cb.pid(),raiz,raiz,desconhecido,ca.pid(),cb.pid(),raiz);
        if(a.comandoAtual()!=ca||b.comandoAtual()!=cb
                ||linhas.stream().map(l->((Number)l.get("solicitante")).longValue()).distinct().count()!=2)
            return java.util.List.of();
        return linhas;
    }

    private static void esperarLiberacao(CountDownLatch liberar) {
        try { if(!liberar.await(25,TimeUnit.SECONDS)) throw new AssertionError("Fixture não foi liberada"); }
        catch(InterruptedException e) {Thread.currentThread().interrupt();throw new IllegalStateException(e);}
    }
}
