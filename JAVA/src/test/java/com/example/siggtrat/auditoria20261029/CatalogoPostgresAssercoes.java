package com.example.siggtrat.auditoria20261029;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Catálogo físico em leitura: expectativas críticas do domínio e comparação lógica entre boots. */
final class CatalogoPostgresAssercoes {
    private CatalogoPostgresAssercoes() {}

    record Fotografia(List<Map<String, Object>> colunas, List<Map<String, Object>> restricoes,
                      List<Map<String, Object>> indices) {}

    static Fotografia capturar(JdbcTemplate jdbc) {
        return new Fotografia(fixar(jdbc.queryForList("""
                select table_name, column_name, ordinal_position, data_type, udt_name, is_nullable,
                       character_maximum_length, numeric_precision, numeric_scale, datetime_precision, collation_name,
                       column_default, is_identity, identity_generation
                  from information_schema.columns where table_schema='public'
                 order by table_name,column_name
                """)), fixar(jdbc.queryForList("""
                select t.relname as tabela, c.conname as nome, c.contype::text as tipo,
                       array_to_string(array(select a.attname::text
                         from unnest(c.conkey) with ordinality k(numero,ordem)
                         join pg_attribute a on a.attrelid=c.conrelid and a.attnum=k.numero
                         order by k.ordem),',') as colunas,
                       rt.relname as tabela_referenciada,
                       array_to_string(array(select a.attname::text
                         from unnest(c.confkey) with ordinality k(numero,ordem)
                         join pg_attribute a on a.attrelid=c.confrelid and a.attnum=k.numero
                         order by k.ordem),',') as colunas_referenciadas,
                       c.convalidated as validada, c.condeferrable as adiavel,
                       pg_get_constraintdef(c.oid,true) as definicao
                  from pg_constraint c join pg_class t on t.oid=c.conrelid
                  join pg_namespace n on n.oid=t.relnamespace
                  left join pg_class rt on rt.oid=c.confrelid
                 where n.nspname='public' order by t.relname,c.conname
                """)), fixar(jdbc.queryForList("""
                select t.relname as tabela, i.relname as nome, x.indisunique as unico,
                       x.indisprimary as primario, x.indisvalid as valido, x.indisready as pronto,
                       array_to_string(array(select a.attname::text
                         from unnest(x.indkey::smallint[]) with ordinality k(numero,ordem)
                         left join pg_attribute a on a.attrelid=x.indrelid and a.attnum=k.numero
                         where k.ordem<=x.indnkeyatts order by k.ordem),',') as colunas,
                       pg_get_indexdef(i.oid) as definicao
                  from pg_index x join pg_class t on t.oid=x.indrelid
                  join pg_class i on i.oid=x.indexrelid join pg_namespace n on n.oid=t.relnamespace
                 where n.nspname='public' order by t.relname,i.relname
                """)));
    }

    static List<String> exigirRelacoesCriticas(Fotografia f) {
        List<String> verificadas = new ArrayList<>();
        Map<String, String> chaves = new LinkedHashMap<>();
        chaves.put("pessoa", "id_pessoa"); chaves.put("empregado", "id_empregado");
        chaves.put("contrato", "id_contrato"); chaves.put("vinculo_contratual", "id_vinculo_contratual");
        chaves.put("alocacao_vinculo", "id_alocacao_vinculo"); chaves.put("frequencia", "id");
        chaves.put("ocorrencia", "id"); chaves.put("contexto_ocorrencia", "id");
        chaves.put("anexo_ocorrencia", "id"); chaves.put("documento_comprobatorio", "id_documento");
        chaves.put("permissao_excepcional", "id_permissao_excepcional");
        for (var chave : chaves.entrySet()) {
            exigir(f.restricoes(), r -> igual(r, "tabela", chave.getKey()) && igual(r, "tipo", "p")
                            && igual(r, "colunas", chave.getValue()) && Boolean.TRUE.equals(r.get("validada")),
                    "PK " + chave.getKey() + "(" + chave.getValue() + ")", verificadas);
        }
        fk(f, "vinculo_contratual", "id_pessoa", "pessoa", "id_pessoa", verificadas);
        fk(f, "vinculo_contratual", "id_contrato", "contrato", "id_contrato", verificadas);
        fk(f, "alocacao_vinculo", "id_vinculo_contratual", "vinculo_contratual", "id_vinculo_contratual", verificadas);
        fk(f, "alocacao_vinculo", "id_composicao_contratual", "composicao_contratual", "id_composicao_contratual", verificadas);
        fk(f, "alocacao_vinculo", "id_lotacao", "lotacao", "id_lotacao", verificadas);
        fk(f, "alocacao_vinculo", "id_jornada", "jornada", "id_jornada", verificadas);
        fk(f, "alocacao_vinculo", "id_cargo", "cargo", "id_cargo", verificadas);
        fk(f, "frequencia", "id_empregado", "empregado", "id_empregado", verificadas);
        fk(f, "frequencia", "id_pessoa", "pessoa", "id_pessoa", verificadas);
        fk(f, "frequencia", "id_vinculo_contratual", "vinculo_contratual", "id_vinculo_contratual", verificadas);
        fk(f, "frequencia", "id_alocacao_vinculo", "alocacao_vinculo", "id_alocacao_vinculo", verificadas);
        fk(f, "ocorrencia", "id_empregado", "empregado", "id_empregado", verificadas);
        fk(f, "contexto_ocorrencia", "ocorrencia_id", "ocorrencia", "id", verificadas);
        fk(f, "anexo_ocorrencia", "id_ocorrencia", "ocorrencia", "id", verificadas);
        fk(f, "documento_comprobatorio", "id_contrato", "contrato", "id_contrato", verificadas);
        fk(f, "documento_comprobatorio", "id_documento_anterior", "documento_comprobatorio", "id_documento", verificadas);
        fk(f, "permissao_excepcional", "id_ocorrencia", "ocorrencia", "id", verificadas);
        fk(f, "permissao_excepcional", "id_usuario_beneficiario", "usuario_aplicacao", "id", verificadas);
        unico(f, "frequencia", "id_empregado,data_referencia", verificadas);
        unico(f, "frequencia", "id_vinculo_contratual,data_referencia", verificadas);
        unico(f, "contexto_ocorrencia", "ocorrencia_id,data_referencia", verificadas);
        unico(f, "ocorrencia", "id_empregado,chave_idempotencia", verificadas);
        unico(f, "documento_comprobatorio", "chave_idempotencia", verificadas);
        unico(f, "documento_comprobatorio", "grupo_logico_id,versao_documento", verificadas);
        indice(f, "alocacao_vinculo", "idx_alocacao_vinculo_periodo", "id_vinculo_contratual,data_inicio,data_fim", verificadas);
        indice(f, "permissao_excepcional", "idx_permissao_excepcional_ocorrencia", "id_ocorrencia,tipo,ativo", verificadas);
        indice(f, "documento_comprobatorio", "idx_documento_contrato_fato", "id_contrato,tipo_fato,fato_id", verificadas);
        naoNula(f, "frequencia", "id_empregado", verificadas);
        naoNula(f, "frequencia", "data_referencia", verificadas);
        naoNula(f, "vinculo_contratual", "id_pessoa", verificadas);
        naoNula(f, "vinculo_contratual", "id_contrato", verificadas);
        naoNula(f, "contexto_ocorrencia", "ocorrencia_id", verificadas);
        naoNula(f, "contexto_ocorrencia", "data_referencia", verificadas);
        naoNula(f, "documento_comprobatorio", "id_contrato", verificadas);
        naoNula(f, "documento_comprobatorio", "grupo_logico_id", verificadas);
        naoNula(f, "permissao_excepcional", "id_usuario_beneficiario", verificadas);
        return List.copyOf(verificadas);
    }

    private static void fk(Fotografia f, String tabela, String coluna, String alvo, String colunaAlvo, List<String> ok) {
        exigir(f.restricoes(), r -> igual(r, "tabela", tabela) && igual(r, "tipo", "f")
                        && igual(r, "colunas", coluna) && igual(r, "tabela_referenciada", alvo)
                        && igual(r, "colunas_referenciadas", colunaAlvo) && Boolean.TRUE.equals(r.get("validada")),
                "FK " + tabela + "(" + coluna + ")->" + alvo + "(" + colunaAlvo + ")", ok);
    }

    private static void unico(Fotografia f, String tabela, String colunas, List<String> ok) {
        exigir(f.indices(), r -> igual(r, "tabela", tabela) && igual(r, "colunas", colunas)
                        && Boolean.TRUE.equals(r.get("unico")) && Boolean.TRUE.equals(r.get("valido"))
                        && Boolean.TRUE.equals(r.get("pronto")), "UNIQUE " + tabela + "(" + colunas + ")", ok);
    }

    private static void indice(Fotografia f, String tabela, String nome, String colunas, List<String> ok) {
        exigir(f.indices(), r -> igual(r, "tabela", tabela) && igual(r, "nome", nome) && igual(r, "colunas", colunas)
                        && Boolean.TRUE.equals(r.get("valido")) && Boolean.TRUE.equals(r.get("pronto")),
                "INDEX " + nome + "(" + colunas + ")", ok);
    }

    private static void naoNula(Fotografia f, String tabela, String coluna, List<String> ok) {
        exigir(f.colunas(), r -> igual(r, "table_name", tabela) && igual(r, "column_name", coluna)
                && igual(r, "is_nullable", "NO"), "NOT NULL " + tabela + "." + coluna, ok);
    }

    private static boolean igual(Map<String, Object> r, String campo, String esperado) {
        return esperado.equals(r.get(campo));
    }

    private static void exigir(List<Map<String, Object>> linhas, Predicate<Map<String, Object>> criterio,
                               String descricao, List<String> ok) {
        assertTrue(linhas.stream().anyMatch(criterio), "Catálogo PostgreSQL sem garantia esperada: " + descricao);
        ok.add(descricao);
    }

    private static List<Map<String, Object>> fixar(List<Map<String, Object>> linhas) {
        return linhas.stream().map(l -> Collections.unmodifiableMap(new LinkedHashMap<>(l))).toList();
    }
}
