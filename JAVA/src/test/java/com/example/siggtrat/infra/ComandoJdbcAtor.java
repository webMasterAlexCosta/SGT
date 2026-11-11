package com.example.siggtrat.infra;

import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Somente teste: reconhece formas SQL simples geradas pelo ORM; ambiguidades não provam alvo. */
public record ComandoJdbcAtor(long pid, String sql, List<Map<Integer,String>> conjuntos) {
    public ComandoJdbcAtor { conjuntos=conjuntos.stream().map(Map::copyOf).toList(); }
    public ComandoJdbcAtor(long pid,String sql,Map<Integer,String> parametros) {this(pid,sql,List.of(parametros));}
    public static String chave(Object valor) {
        if(valor instanceof Number) return "N:"+UtilitarioHashSeguranca.sha256Hexadecimal(valor.toString());
        if(valor instanceof String) return "S:"+UtilitarioHashSeguranca.sha256Hexadecimal(valor.toString());
        return null;
    }
    private String normalizado() {return sql.replaceAll("(?s)/\\*.*?\\*/", " ").replace("\"", "").replaceAll("\\s+"," ").trim().toLowerCase(Locale.ROOT);}
    private static String nome(String texto) {if(!texto.matches("[a-z][a-z0-9_]*"))throw new IllegalArgumentException("Identificador da fixture inválido");return texto;}
    private boolean parametro(int pos,Object valor) {
        String esperado=chave(valor);
        return esperado!=null && !conjuntos.isEmpty() && conjuntos.stream().allMatch(p->esperado.equals(p.get(pos)));
    }
    public boolean insercao(String tabela,String coluna,Object esperado) {
        String s=normalizado();
        var m=Pattern.compile("^insert into (?:[a-z0-9_]+\\.)?"+nome(tabela)+"\\s*\\(").matcher(s);
        if(!m.find()) return false;
        int fecha=s.indexOf(')',m.end());if(fecha<0)return false;
        var cols=List.of(s.substring(m.end(),fecha).split("\\s*,\\s*"));int alvo=cols.indexOf(nome(coluna));if(alvo<0)return false;
        var valores=Pattern.compile("\\s*values\\s*\\(").matcher(s);valores.region(fecha+1,s.length());if(!valores.lookingAt())return false;
        int fim=fechaParenteses(s,valores.end()-1);if(fim<0||s.substring(fim+1).stripLeading().startsWith(","))return false;
        List<String> termos=separar(s.substring(valores.end(),fim));if(termos.size()!=cols.size())return false;
        if(!termos.get(alvo).matches("\\?(?:::[a-z0-9_.]+)?"))return false;
        int indice=1;for(int i=0;i<alvo;i++)indice+=marcadores(termos.get(i));
        return parametro(indice,esperado);
    }
    public boolean selecaoId(String tabela,Object id) {return selecaoChave(tabela,"id",id);}
    public boolean selecaoChave(String tabela,String colunaId,Object id) {
        String s=normalizado();if(!s.startsWith("select ")||!Pattern.compile("\\bfor\\s+(?:(?:no\\s+key\\s+)?update|(?:key\\s+)?share)\\b").matcher(s).find())return false;
        int de=palavraSuperior(s,"from",0);if(de<0)return false;
        String alias=aliasTabela(s.substring(de+4),tabela);if(alias==null)return false;
        return idNoWhere(s,alias,colunaId,id);
    }
    public boolean atualizacao(String tabela,String coluna,Object valor,Object id) {return atualizacao(tabela,coluna,valor,"id",id);}
    public boolean alteracaoNaChave(String tabela,String colunaId,Object id) {
        String s=normalizado();if(!s.startsWith("update "))return false;
        String alias=aliasTabela(s.substring(7),tabela);
        return alias!=null&&idNoWhere(s,alias,colunaId,id);
    }
    public boolean atualizacao(String tabela,String coluna,Object valor,String colunaId,Object id) {
        String s=normalizado();if(!s.startsWith("update "))return false;
        String alias=aliasTabela(s.substring(7),tabela);if(alias==null||!idNoWhere(s,alias,colunaId,id))return false;
        int set=palavraSuperior(s,"set",0),where=palavraSuperior(s,"where",0);if(set<0||where<set)return false;
        String trecho=s.substring(set+3,where);
        var m=Pattern.compile("(?:^|,)\\s*"+(alias.isEmpty()?"":"(?:"+Pattern.quote(alias)+"\\.)?")+nome(coluna)+"\\s*=\\s*(\\?)\\s*(?=,|$)").matcher(trecho);
        if(!m.find())return false;
        return parametro(marcadores(s.substring(0,set+3+m.start(1)))+1,valor);
    }
    private boolean idNoWhere(String s,String alias,String colunaId,Object id) {
        int where=palavraSuperior(s,"where",0);if(where<0)return false;
        String qualificador=alias.isEmpty()?"":Pattern.quote(alias)+"\\.";
        if(Pattern.compile("\\b(?:or|select|exists|union)\\b").matcher(s.substring(where+5)).find())return false;
        // Hibernate também usa IN (?) ao refrescar uma entidade com FOR SHARE.
        // Somente um marcador é inequívoco; listas e expressões não provam a linha.
        var m=Pattern.compile("(?<![a-z0-9_.])"+qualificador+nome(colunaId)
                +"\\s*(?:=\\s*(\\?)(?![a-z0-9_])|in\\s*\\(\\s*(\\?)\\s*\\))").matcher(s);
        m.region(where+5,s.length());if(!m.find())return false;
        int posicao=m.start(m.group(1)!=null?1:2);if(m.find())return false;
        return parametro(marcadores(s.substring(0,posicao))+1,id);
    }
    /** null significa outra tabela; vazio significa sem alias. */
    private static String aliasTabela(String trecho,String tabela) {
        var m=Pattern.compile("^\\s*(?:[a-z0-9_]+\\.)?"+nome(tabela)+"\\b(?:\\s+(?:as\\s+)?([a-z0-9_]+))?").matcher(trecho);
        if(!m.find())return null;String alias=m.group(1);
        return alias==null||List.of("where","set","left","right","join","inner","outer","order","group","for").contains(alias)?"":alias;
    }
    private static int palavraSuperior(String s,String palavra,int inicio) {
        int nivel=0;boolean literal=false;
        for(int i=inicio;i<s.length();i++) {
            char c=s.charAt(i);
            if(c=='\''){if(literal&&i+1<s.length()&&s.charAt(i+1)=='\''){i++;continue;}literal=!literal;continue;}
            if(literal)continue;if(c=='(')nivel++;if(c==')')nivel--;
            if(nivel==0&&s.startsWith(palavra,i)&&(i==0||!Character.isJavaIdentifierPart(s.charAt(i-1)))
                    &&(i+palavra.length()==s.length()||!Character.isJavaIdentifierPart(s.charAt(i+palavra.length()))))return i;
        }return -1;
    }
    private static int marcadores(String s) {
        int total=0;boolean literal=false;
        for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='\''){if(literal&&i+1<s.length()&&s.charAt(i+1)=='\''){i++;continue;}literal=!literal;}else if(c=='?'&&!literal)total++;}return total;
    }
    private static int fechaParenteses(String s,int abre) {
        int nivel=0;boolean literal=false;
        for(int i=abre;i<s.length();i++){char c=s.charAt(i);if(c=='\''){if(literal&&i+1<s.length()&&s.charAt(i+1)=='\''){i++;continue;}literal=!literal;}if(!literal){if(c=='(')nivel++;if(c==')'&&--nivel==0)return i;}}return -1;
    }
    private static List<String> separar(String s) {
        var partes=new ArrayList<String>();int nivel=0,inicio=0;boolean literal=false;
        for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='\''){if(literal&&i+1<s.length()&&s.charAt(i+1)=='\''){i++;continue;}literal=!literal;}if(literal)continue;if(c=='(')nivel++;if(c==')')nivel--;if(c==','&&nivel==0){partes.add(s.substring(inicio,i).trim());inicio=i+1;}}
        partes.add(s.substring(inicio).trim());return partes;
    }
    @Override public String toString(){return "ComandoJdbcAtor[pid="+pid+", conjuntos="+conjuntos.size()+"]";}
}
