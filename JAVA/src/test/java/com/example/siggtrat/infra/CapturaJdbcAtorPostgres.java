package com.example.siggtrat.infra;

import org.postgresql.PGConnection;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Apenas fixtures: associa a espera física ao SQL e aos binds opacos efetivos. */
public final class CapturaJdbcAtorPostgres implements BeanPostProcessor {
    @Override public Object postProcessAfterInitialization(Object bean,String nome) {
        if(bean instanceof DataSource ds && !(bean instanceof FonteObservada)) return new FonteObservada(ds);
        return bean;
    }

    private static final class FonteObservada extends DelegatingDataSource implements AutoCloseable {
        private final DataSource original;
        FonteObservada(DataSource original){super(original);this.original=original;}
        @Override public Connection getConnection() throws java.sql.SQLException {return observar(original.getConnection());}
        @Override public Connection getConnection(String usuario,String senha) throws java.sql.SQLException {return observar(original.getConnection(usuario,senha));}
        @Override public void close() throws Exception {if(original instanceof AutoCloseable c)c.close();}
    }

    private static Connection observar(Connection original) throws java.sql.SQLException {
        if(ObservadorAtorPostgres.ATOR.get()==null)return original;
        // É informação local do driver: nenhum SELECT, flush ou ajuste de sessão.
        long pid=original.unwrap(PGConnection.class).getBackendPID();
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,metodo,args)->{
            if(metodo.getDeclaringClass()==Object.class)return objeto(proxy,metodo,args,"ConexaoJdbcObservada");
            Object resultado=invocar(original,metodo,args);
            if(resultado instanceof Statement statement && (metodo.getName().startsWith("prepare")||metodo.getName().equals("createStatement"))) {
                String sql=args!=null&&args.length>0&&args[0] instanceof String texto?texto:null;
                return observarStatement(statement,(Connection)proxy,pid,sql);
            }
            return resultado;
        });
    }

    private static Object observarStatement(Statement original,Connection conexao,long pid,String preparado) {
        Map<Integer,String> valores=new HashMap<>();
        List<Map<Integer,String>> lotes=new ArrayList<>();
        Class<?> contrato=original instanceof CallableStatement?CallableStatement.class:
                original instanceof PreparedStatement?PreparedStatement.class:Statement.class;
        return Proxy.newProxyInstance(contrato.getClassLoader(),new Class<?>[]{contrato},(proxy,metodo,args)->{
            String nome=metodo.getName();
            if(metodo.getDeclaringClass()==Object.class)return objeto(proxy,metodo,args,"StatementJdbcObservado");
            if(nome.equals("getConnection"))return conexao;
            if(nome.startsWith("set") && args!=null && args.length>=2 && args[0] instanceof Integer indice) {
                Object resultado=invocar(original,metodo,args);
                String chave=nome.equals("setNull")?null:ComandoJdbcAtor.chave(args[1]);
                if(chave==null)valores.remove(indice);else valores.put(indice,chave);
                return resultado;
            }
            if(nome.equals("clearParameters")){Object r=invocar(original,metodo,args);valores.clear();return r;}
            if(nome.equals("addBatch")){Object r=invocar(original,metodo,args);lotes.add(Map.copyOf(valores));return r;}
            if(nome.equals("clearBatch")){Object r=invocar(original,metodo,args);lotes.clear();return r;}
            if(!nome.startsWith("execute"))return invocar(original,metodo,args);
            var ator=ObservadorAtorPostgres.ATOR.get();
            String sql=preparado!=null?preparado:args!=null&&args.length>0&&args[0] instanceof String texto?texto:null;
            if(ator==null||sql==null)return invocar(original,metodo,args);
            boolean emLote=nome.equals("executeBatch")||nome.equals("executeLargeBatch");
            var conjuntos=emLote?List.copyOf(lotes):List.of(Map.copyOf(valores));
            var comando=new ComandoJdbcAtor(pid,sql,conjuntos);
            var anterior=ator.comando;
            ator.comando=comando;
            try {return invocar(original,metodo,args);}
            finally {ator.comando=anterior;if(emLote)lotes.clear();}
        });
    }

    private static Object objeto(Object proxy,Method metodo,Object[] args,String descricao) {
        return switch(metodo.getName()) {
            case "equals" -> proxy==args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> descricao;
            default -> throw new IllegalStateException("Método Object inesperado na fixture");
        };
    }

    private static Object invocar(Object alvo,Method metodo,Object[] args) throws Throwable {
        try {return metodo.invoke(alvo,args);}
        catch(InvocationTargetException erro){throw erro.getCause();}
    }
}
