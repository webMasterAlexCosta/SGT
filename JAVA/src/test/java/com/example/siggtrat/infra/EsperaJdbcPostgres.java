package com.example.siggtrat.infra;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/** Prova de fixture: PID conhecido, espera física e bindings do comando em execução. */
public final class EsperaJdbcPostgres {
    private EsperaJdbcPostgres() { }

    public static boolean indiceUnico(JdbcTemplate jdbc,String tabela,String indice,String coluna) {
        return Long.valueOf(1).equals(jdbc.queryForObject("""
            SELECT count(*) FROM pg_index x JOIN pg_class t ON t.oid=x.indrelid
              JOIN pg_namespace n ON n.oid=t.relnamespace
              JOIN pg_class i ON i.oid=x.indexrelid
              JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=x.indkey[0]
             WHERE n.nspname=current_schema() AND t.relname=? AND i.relname=? AND a.attname=?
               AND x.indisunique AND x.indisvalid AND x.indisready AND x.indnkeyatts=1
               AND x.indexprs IS NULL AND x.indpred IS NULL
            """,Long.class,tabela,indice,coluna));
    }

    public static List<String> indicesUnicosDaColuna(JdbcTemplate jdbc,String tabela,String coluna) {
        return jdbc.query("""
            SELECT i.relname FROM pg_index x JOIN pg_class t ON t.oid=x.indrelid
              JOIN pg_namespace n ON n.oid=t.relnamespace JOIN pg_class i ON i.oid=x.indexrelid
              JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=x.indkey[0]
             WHERE n.nspname=current_schema() AND t.relname=? AND a.attname=?
               AND x.indisunique AND x.indisvalid AND x.indisready AND x.indnkeyatts=1
               AND x.indexprs IS NULL AND x.indpred IS NULL
            """,(r,n)->r.getString(1),tabela,coluna);
    }

    /** Inclui a fila transitiva, mas somente entre os dois atores conhecidos e sua raiz. */
    public static List<Map<String,Object>> observarAmbos(JdbcTemplate jdbc,long raiz,
            ObservadorAtorPostgres.Ator a,Predicate<ComandoJdbcAtor> alvoA,
            ObservadorAtorPostgres.Ator b,Predicate<ComandoJdbcAtor> alvoB,String descricao) {
        var ca=a.comandoAtual();var cb=b.comandoAtual();
        if(ca==null||cb==null||ca.pid()==cb.pid()||!alvoA.test(ca)||!alvoB.test(cb))return List.of();
        var linhas=jdbc.queryForList("""
            WITH RECURSIVE caminhos(origem,pid,caminho) AS (
                SELECT a.pid,a.pid,ARRAY[a.pid] FROM pg_stat_activity a
                 WHERE a.datname=current_database() AND a.pid IN (?,?) AND a.wait_event_type='Lock'
                UNION ALL
                SELECT c.origem,p.pid,c.caminho||p.pid FROM caminhos c
                  CROSS JOIN LATERAL unnest(pg_blocking_pids(c.pid)) AS p(pid)
                 WHERE c.pid<>? AND p.pid IN (?,?,?) AND NOT p.pid=ANY(c.caminho)
            ) SELECT a.pid AS solicitante,c.caminho::text AS caminho_ate_raiz,
                     pg_blocking_pids(a.pid)::text AS bloqueadores_diretos,
                     a.wait_event_type,a.wait_event,w.locktype AS recurso_postgres,
                     w.mode AS modo_solicitado,w.granted AS solicitado_concedido,
                     w.relation AS relation_oid,w.page AS pagina,w.tuple AS tupla,
                     w.transactionid::text AS transaction_id
                FROM caminhos c JOIN pg_stat_activity a ON a.pid=c.origem
                JOIN pg_locks w ON w.pid=a.pid AND NOT w.granted WHERE c.pid=?
            """,ca.pid(),cb.pid(),raiz,raiz,ca.pid(),cb.pid(),raiz);
        if(a.comandoAtual()!=ca||b.comandoAtual()!=cb
                ||linhas.stream().map(l->((Number)l.get("solicitante")).longValue()).distinct().count()!=2)return List.of();
        return linhas.stream().map(linha->{
            long pid=((Number)linha.get("solicitante")).longValue();
            var comando=pid==ca.pid()?ca:cb;
            Map<String,Object> prova=new LinkedHashMap<>(linha);
            prova.put("raiz",raiz);prova.put("ator",pid==ca.pid()?a.nome():b.nome());
            prova.put("origem","cadeia pg_blocking_pids restrita aos atores + pg_locks + JDBC");
            prova.put("alvo_fixture",descricao);prova.put("sql_preparado",comando.sql());
            prova.put("bindings_sha256",comando.conjuntos());return prova;
        }).toList();
    }

    public static List<Map<String,Object>> aguardarAmbos(JdbcTemplate jdbc,long raiz,
            ObservadorAtorPostgres.Ator a,Predicate<ComandoJdbcAtor> alvoA,
            ObservadorAtorPostgres.Ator b,Predicate<ComandoJdbcAtor> alvoB,String descricao,
            Future<?> primeira,Future<?> segunda) throws Exception {
        long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
        while(System.nanoTime()<fim) {
            var prova=observarAmbos(jdbc,raiz,a,alvoA,b,alvoB,descricao);
            if(!prova.isEmpty())return prova;
            if(primeira.isDone()||segunda.isDone())throw new AssertionError("Ambos os atores precisam esperar no alvo: "+descricao);
            Thread.sleep(20);
        }
        throw new AssertionError("Duas esperas físicas não comprovadas: "+descricao);
    }

    public static List<Map<String,Object>> observar(JdbcTemplate jdbc, long bloqueador,
            ObservadorAtorPostgres.Ator ator, Predicate<ComandoJdbcAtor> alvo, String descricaoAlvo) {
        var comando=ator.comandoAtual();
        if(comando==null || !alvo.test(comando)) return List.of();
        var linhas=jdbc.queryForList("""
            SELECT a.pid AS solicitante, b.pid AS bloqueador,
                   a.backend_xid::text AS transacao_solicitante,
                   retentor.backend_xid::text AS transacao_bloqueadora,
                   a.wait_event_type, a.wait_event,
                   w.locktype AS recurso_postgres, w.mode AS modo_solicitado,
                   w.granted AS solicitado_concedido, b.mode AS modo_detido,
                   b.granted AS bloqueador_concedido,
                   w.database AS database_oid, w.relation AS relation_oid,
                   w.page AS pagina, w.tuple AS tupla,
                   w.transactionid::text AS transaction_id, w.virtualxid AS virtual_xid,
                   w.classid AS class_oid, w.objid AS object_oid, w.objsubid AS object_subid
              FROM pg_stat_activity a
              JOIN pg_stat_activity retentor ON retentor.pid=?
              JOIN pg_locks w ON w.pid=a.pid AND NOT w.granted
              JOIN pg_locks b ON b.pid=? AND b.granted
                 AND ROW(b.locktype,b.database,b.relation,b.page,b.tuple,b.virtualxid,
                         b.transactionid,b.classid,b.objid,b.objsubid)
                     IS NOT DISTINCT FROM
                     ROW(w.locktype,w.database,w.relation,w.page,w.tuple,w.virtualxid,
                         w.transactionid,w.classid,w.objid,w.objsubid)
             WHERE a.datname=current_database() AND a.pid=?
               AND a.wait_event_type='Lock' AND ?=ANY(pg_blocking_pids(a.pid))
            """,bloqueador,bloqueador,comando.pid(),bloqueador);
        // Não atribuir a amostra a outro comando que tenha começado durante a consulta.
        if(ator.comandoAtual()!=comando) return List.of();
        return linhas.stream().map(linha->{
            Map<String,Object> prova=new LinkedHashMap<>(linha);
            prova.put("origem","pg_stat_activity/pg_locks/pg_blocking_pids + JDBC do ator");
            prova.put("ator",ator.nome());
            prova.put("alvo_fixture",descricaoAlvo);
            prova.put("sql_preparado",comando.sql());
            prova.put("bindings_sha256",comando.conjuntos());
            prova.put("limite","Alvo associado pelos bindings JDBC e pela fixture; pg_locks não fornece a chave da linha.");
            return prova;
        }).toList();
    }

    public static List<Map<String,Object>> aguardar(JdbcTemplate jdbc,long bloqueador,
            ObservadorAtorPostgres.Ator ator,Predicate<ComandoJdbcAtor> alvo,
            String descricaoAlvo,Future<?> tentativa) throws Exception {
        long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
        while(System.nanoTime()<fim) {
            var prova=observar(jdbc,bloqueador,ator,alvo,descricaoAlvo);
            if(!prova.isEmpty()) return prova;
            if(tentativa.isDone()) {
                tentativa.get();
                throw new AssertionError("Comando terminou sem espera física no alvo: "+descricaoAlvo);
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Espera física não comprovada no alvo: "+descricaoAlvo);
    }
}
