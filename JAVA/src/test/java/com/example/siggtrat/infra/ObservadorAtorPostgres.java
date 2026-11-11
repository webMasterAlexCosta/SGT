package com.example.siggtrat.infra;

import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/** Somente fixtures: associa a operação de um ator ao backend que realmente a executa. */
public final class ObservadorAtorPostgres {
    private ObservadorAtorPostgres() { }
    static final ThreadLocal<Ator> ATOR = new ThreadLocal<>();
    public record Operacao(long pid, String metodo, Set<String> chaves) {
        public boolean corresponde(String metodoEsperado, Object chave) {
            return metodo.equals(metodoEsperado) && chaves.contains(chaveSegura(chave));
        }
    }
    public static final class Ator {
        private final String nome;
        volatile Operacao atual;
        volatile ComandoJdbcAtor comando;
        public ComandoJdbcAtor comandoAtual() { return comando; }
        public Ator(String nome) { this.nome = nome; }
        public Operacao operacaoAtual() { return atual; }
        public String nome() { return nome; }
        public <T> T executar(Callable<T> operacao) throws Exception {
            Ator anterior = ATOR.get();
            if (anterior != null) throw new IllegalStateException("Fixture não admite atores aninhados");
            ATOR.set(this);
            try { return operacao.call(); }
            finally { atual = null; comando = null; ATOR.remove(); }
        }
    }
    static Set<String> chaves(Object[] argumentos) {
        return Arrays.stream(argumentos).filter(a -> a instanceof Number || a instanceof String)
                .map(ObservadorAtorPostgres::chaveSegura).collect(Collectors.toUnmodifiableSet());
    }
    private static String chaveSegura(Object chave) {
        if (chave instanceof Number n) return "numero:" + n;
        if (chave instanceof String s) return "texto-sha256:" + UtilitarioHashSeguranca.sha256Hexadecimal(s);
        return "sem-chave";
    }
    /** Método+argumento são capturados antes do SQL; PID+SQL corrente comprovam a espera física. */
    public static boolean esperaDiretaNoAlvo(JdbcTemplate jdbc, long raiz, Ator ator,
                                            String metodo, Object chave, String tabela) {
        if (!tabela.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Tabela da fixture inválida");
        Operacao op = ator.atual;
        if (op == null || !op.corresponde(metodo, chave)) return false;
        var consultas = jdbc.query("""
            SELECT query FROM pg_stat_activity a
             WHERE a.datname=current_database() AND a.pid=? AND ? = ANY(pg_blocking_pids(a.pid))
            """, (r, n) -> r.getString(1), op.pid(), raiz);
        if (consultas.isEmpty()) return false;
        String sql = consultas.get(0);
        if (sql == null) return false;
        // Hibernate pode emitir FOR UPDATE ou FOR NO KEY UPDATE. Tabela principal,
        // não qualquer JOIN/lock já concedido, e a chave vêm da operação capturada.
        String normalizado = sql.replace('"', ' ').toLowerCase(java.util.Locale.ROOT);
        return normalizado.matches("(?s).*\\bfrom\\s+(?:[a-z0-9_]+\\.)?" + tabela + "\\b.*")
                && normalizado.matches("(?s).*\\bfor\\s+(?:no\\s+key\\s+)?update\\b.*");
    }
}
