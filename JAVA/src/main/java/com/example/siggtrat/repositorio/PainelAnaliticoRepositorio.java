package com.example.siggtrat.repositorio;

import com.example.siggtrat.dto.PainelAnaliticoDTO.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Somente agregados; nunca materializa frequências/pessoas individuais. Sem cache de autorização. */
@Repository
@Transactional(readOnly = true)
public class PainelAnaliticoRepositorio {
    private final NamedParameterJdbcTemplate jdbc;
    private static final String CONTAGENS = """
        COUNT(*) FILTER (WHERE f.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR','FALTA','ATESTADO','SUSPENSAO')) registros,
        COUNT(*) FILTER (WHERE f.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR')) presencas,
        COUNT(*) FILTER (WHERE f.status IN ('FALTA','ATESTADO','SUSPENSAO')) ausencias
        """;
    public record Filtro(Long gestao, Long superintendencia, Long contrato, TipoUnidade tipo, Long unidade, Long prestador) {
        public Filtro(Long gestao, Long superintendencia, Long contrato, TipoUnidade tipo, Long unidade) { this(gestao,superintendencia,contrato,tipo,unidade,null); }
    }
    public List<Opcao> prestadores(List<Long> contratos) {
        if (contratos.isEmpty()) return List.of();
        return jdbc.query("SELECT id_fornecedor id, MIN(COALESCE(NULLIF(nome_fantasia_empresa,''),razao_social_empresa,'Prestador sem nome')) nome FROM contrato WHERE id_contrato IN (:ids) AND id_fornecedor IS NOT NULL GROUP BY id_fornecedor ORDER BY nome,id",
                new MapSqlParameterSource("ids",contratos),(rs,n)->new Opcao(rs.getLong("id"),rs.getString("nome")));
    }
    public PainelAnaliticoRepositorio(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }

    public List<Opcao> unidades(TipoUnidade tipo, Long gestao, Long se) {
        String sql = switch(tipo) {
            case SUPERINTENDENCIA -> "SELECT id_superintendencia id, nome FROM superintendencia_estadual";
            case GESTAO_LOCAL -> "SELECT id_gestao_local id, nome FROM gestao_local WHERE id_superintendencia=:se";
            case LOTACAO -> "SELECT id_lotacao id, nome FROM lotacao WHERE id_gestao_local=:gestao";
        };
        return jdbc.query(sql+" ORDER BY nome, id", new MapSqlParameterSource().addValue("gestao",gestao).addValue("se",se),
                (rs,n)->new Opcao(rs.getLong("id"),rs.getString("nome")));
    }

    public Totais totais(LocalDate inicio, LocalDate fim, Filtro filtro) {
        return jdbc.queryForObject("SELECT "+CONTAGENS+" FROM frequencia f WHERE f.validade_operacional='VALIDA' AND f.data_referencia BETWEEN :inicio AND :fim AND f.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR','FALTA','ATESTADO','SUSPENSAO')"+where(filtro),
                parametros(inicio,fim,filtro),(rs,n)->new Totais(rs.getLong("registros"),rs.getLong("presencas"),rs.getLong("ausencias")));
    }

    public Tempestividade tempestividade(LocalDate inicio, LocalDate fim, Filtro filtro) {
        String sql = "SELECT COUNT(*) FILTER (WHERE f.prazo_lancamento_em IS NOT NULL AND f.registrado_em < f.prazo_lancamento_em) dentro, "
                + "COUNT(*) FILTER (WHERE f.prazo_lancamento_em IS NOT NULL AND f.registrado_em >= f.prazo_lancamento_em) fora, "
                + "COUNT(*) FILTER (WHERE f.prazo_lancamento_em IS NULL) indeterminado, "
                + "COUNT(*) FILTER (WHERE f.revisao_solicitada_status='PENDENTE') revisao "
                + "FROM frequencia f WHERE f.validade_operacional='VALIDA' AND f.data_referencia BETWEEN :inicio AND :fim "
                + "AND f.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR','FALTA','ATESTADO','SUSPENSAO')" + where(filtro);
        return jdbc.queryForObject(sql,parametros(inicio,fim,filtro),(rs,n)->new Tempestividade(rs.getLong("dentro"),rs.getLong("fora"),rs.getLong("indeterminado"),rs.getLong("revisao")));
    }

    public String prestador(Long contrato) {
        return jdbc.queryForObject("SELECT COALESCE(NULLIF(nome_fantasia_empresa,''),razao_social_empresa) FROM contrato WHERE id_contrato=:id",
                new MapSqlParameterSource("id",contrato),String.class);
    }

    public List<Totais> serie(List<Faixa> faixas, Filtro filtro) {
        if(faixas.isEmpty()) return List.of();
        var p=parametros(faixas.get(0).inicio(),faixas.get(faixas.size()-1).fim(),filtro);
        String valores=valoresFaixas(faixas,p);
        return jdbc.query("WITH intervalos(idx,inicio,fim) AS (VALUES "+valores+") SELECT i.idx, "+CONTAGENS+
                " FROM intervalos i LEFT JOIN frequencia f ON f.data_referencia BETWEEN i.inicio AND i.fim AND f.validade_operacional='VALIDA' "+where(filtro)+
                " GROUP BY i.idx ORDER BY i.idx",p,(rs,n)->new Totais(rs.getLong("registros"),rs.getLong("presencas"),rs.getLong("ausencias")));
    }

    public long totalUnidades(LocalDate inicio, LocalDate fim, Filtro filtro) {
        Long total=jdbc.queryForObject("SELECT COUNT(DISTINCT "+coluna(filtro.tipo())+") FROM frequencia f WHERE f.validade_operacional='VALIDA' AND f.data_referencia BETWEEN :inicio AND :fim AND f.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR','FALTA','ATESTADO','SUSPENSAO')"+where(filtro),
                parametros(inicio,fim,filtro),Long.class);
        return total==null?0:total;
    }

    /** Paginação estável por identificador; agregados globais não são limitados à página. */
    public List<Unidade> paginaUnidades(LocalDate inicio, LocalDate fim, Filtro filtro, int pagina, int tamanho) {
        String coluna=coluna(filtro.tipo());
        var p=parametros(inicio,fim,filtro).addValue("limite",tamanho).addValue("offset",(long)pagina*tamanho);
        return jdbc.query("SELECT "+coluna+" unidade, "+CONTAGENS+" FROM frequencia f WHERE f.validade_operacional='VALIDA' AND f.data_referencia BETWEEN :inicio AND :fim AND f.status IN ('PRESENCA','PRESENCA_PARCIAL','ATRASO','TR','FALTA','ATESTADO','SUSPENSAO')"+where(filtro)+
                " GROUP BY "+coluna+" ORDER BY unidade LIMIT :limite OFFSET :offset",p,
                (rs,n)->new Unidade(rs.getLong("unidade"),null,new Totais(rs.getLong("registros"),rs.getLong("presencas"),rs.getLong("ausencias"))));
    }

    public Map<Long,List<Totais>> heatmap(List<Faixa> faixas, List<Long> unidades, Filtro filtro) {
        if(faixas.isEmpty()||unidades.isEmpty()) return Map.of();
        var p=parametros(faixas.get(0).inicio(),faixas.get(faixas.size()-1).fim(),filtro).addValue("unidades",unidades);
        String valores=valoresFaixas(faixas,p), coluna=coluna(filtro.tipo());
        Map<Long,List<Totais>> resultado=new LinkedHashMap<>();
        for(Long id:unidades) resultado.put(id,new java.util.ArrayList<>(java.util.Collections.nCopies(faixas.size(),Totais.vazio())));
        jdbc.query("WITH intervalos(idx,inicio,fim) AS (VALUES "+valores+") SELECT "+coluna+" unidade, i.idx, "+CONTAGENS+
                " FROM intervalos i JOIN frequencia f ON f.data_referencia BETWEEN i.inicio AND i.fim WHERE f.validade_operacional='VALIDA' AND "+coluna+" IN (:unidades)"+where(filtro)+
                " GROUP BY "+coluna+", i.idx",p,rs->{
                    resultado.get(rs.getLong("unidade")).set(rs.getInt("idx"),new Totais(rs.getLong("registros"),rs.getLong("presencas"),rs.getLong("ausencias")));
                });
        return resultado;
    }

    private String valoresFaixas(List<Faixa> faixas,MapSqlParameterSource p) {
        List<String> linhas=new java.util.ArrayList<>();
        for(int i=0;i<faixas.size();i++) {
            p.addValue("i"+i,faixas.get(i).inicio()).addValue("f"+i,faixas.get(i).fim());
            linhas.add("("+i+",CAST(:i"+i+" AS DATE),CAST(:f"+i+" AS DATE))");
        }
        return String.join(",",linhas);
    }
    public PaginaRegistros registros(LocalDate inicio, LocalDate fim, Filtro filtro, String metrica, int pagina) {
        String categorias=switch(metrica) {
            case "presencas" -> "'PRESENCA','PRESENCA_PARCIAL','ATRASO','TR'";
            case "ausencias" -> "'FALTA','ATESTADO','SUSPENSAO'";
            case "dentro", "fora", "revisao", "indeterminado", "registros" -> "'PRESENCA','PRESENCA_PARCIAL','ATRASO','TR','FALTA','ATESTADO','SUSPENSAO'";
            default -> throw new IllegalArgumentException("Métrica inválida.");
        };
        var p=parametros(inicio,fim,filtro).addValue("offset",(long)pagina*20);
        String classe=switch(metrica) {
            case "dentro" -> " AND f.prazo_lancamento_em IS NOT NULL AND f.registrado_em < f.prazo_lancamento_em";
            case "fora" -> " AND f.prazo_lancamento_em IS NOT NULL AND f.registrado_em >= f.prazo_lancamento_em";
            case "revisao" -> " AND f.revisao_solicitada_status='PENDENTE'";
            case "indeterminado" -> " AND f.prazo_lancamento_em IS NULL";
            default -> "";
        };
        String origem=" FROM frequencia f WHERE f.validade_operacional='VALIDA' AND f.data_referencia BETWEEN :inicio AND :fim AND f.status IN ("+categorias+")"+where(filtro)+classe;
        Long total=jdbc.queryForObject("SELECT COUNT(*)"+origem,p,Long.class);
        List<Registro> registros=jdbc.query("SELECT f.id,f.data_referencia,f.status,f.versao_negocial,f.contrato_snapshot_id,"+coluna(filtro.tipo())+" unidade"+origem+
                " ORDER BY f.data_referencia DESC,f.id DESC LIMIT 20 OFFSET :offset",p,
                (rs,n)->new Registro(rs.getLong("id"),rs.getDate("data_referencia").toLocalDate(),rs.getString("status"),rs.getInt("versao_negocial"),rs.getObject("contrato_snapshot_id",Long.class),rs.getLong("unidade")));
        return new PaginaRegistros(registros,pagina,total==null?0:total,20);
    }
    private MapSqlParameterSource parametros(LocalDate inicio, LocalDate fim,Filtro f) {
        return new MapSqlParameterSource().addValue("inicio",inicio).addValue("fim",fim)
                .addValue("prestador",f.prestador()).addValue("gestao",f.gestao()).addValue("se",f.superintendencia()).addValue("contrato",f.contrato()).addValue("unidade",f.unidade());
    }
    private String where(Filtro f) {
        return (f.gestao()!=null?" AND f.gestao_local_snapshot_id=:gestao":"")+
                (f.superintendencia()!=null?" AND f.superintendencia_snapshot_id=:se":"")+
                (f.contrato()!=null?" AND f.contrato_snapshot_id=:contrato":"")+
                (f.prestador()!=null?" AND f.contrato_snapshot_id IN (SELECT id_contrato FROM contrato WHERE id_fornecedor=:prestador)":"")+
                (f.unidade()!=null?" AND "+coluna(f.tipo())+"=:unidade":"");
    }
    private String coluna(TipoUnidade tipo) {
        return switch(tipo) {
            case SUPERINTENDENCIA -> "COALESCE(f.superintendencia_snapshot_id,0)";
            case GESTAO_LOCAL -> "COALESCE(f.gestao_local_snapshot_id,0)";
            case LOTACAO -> "COALESCE(f.lotacao_snapshot_id,0)";
        };
    }
}
