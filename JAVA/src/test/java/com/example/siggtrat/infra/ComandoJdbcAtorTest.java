package com.example.siggtrat.infra;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ComandoJdbcAtorTest {
    private String k(Object v){return ComandoJdbcAtor.chave(v);}
    @Test void insertVinculaColunaAoParametroCorretoNaoAQualquerValor(){
        var c=new ComandoJdbcAtor(11,"insert into tipo_contratacao (descricao,gestao_local_id,outra_id) values (?,?,?) returning id",Map.of(1,k("ficticio"),2,k(7L),3,k(8L)));
        assertTrue(c.insercao("tipo_contratacao","gestao_local_id",7L));
        assertFalse(c.insercao("tipo_contratacao","gestao_local_id",8L));
        assertFalse(c.insercao("outra_tabela","gestao_local_id",7L));
    }
    @Test void selectExigeTabelaPrimariaEIdDoWhereNaoDoJoinOuOutroFiltro(){
        var c=new ComandoJdbcAtor(12,"select u.id from public.usuario_aplicacao u left join contrato c on c.criado_por_id=u.id where u.id=? and c.id=? for no key update",Map.of(1,k(5L),2,k(9L)));
        assertTrue(c.selecaoId("usuario_aplicacao",5L));assertFalse(c.selecaoId("usuario_aplicacao",9L));assertFalse(c.selecaoId("contrato",9L));
    }
    @Test void updateVinculaNovoCpfEIdDaPessoaCorreta(){
        var c=new ComandoJdbcAtor(13,"update pessoa set cpf=?,nome=?,version=? where id=? and version=?",Map.of(1,k("CPF-FICTICIO"),2,k("nome"),3,k(1L),4,k(42L),5,k(0L)));
        assertTrue(c.atualizacao("pessoa","cpf","CPF-FICTICIO",42L));assertFalse(c.atualizacao("pessoa","cpf","nome",42L));assertFalse(c.atualizacao("pessoa","cpf","CPF-FICTICIO",43L));
    }
    @Test void loteAmbiguoNaoProvaQueOAlvoEsperadoEstejaEmExecucao(){
        var c=new ComandoJdbcAtor(14,"insert into filho (gestao_local_id) values (?)",List.of(Map.of(1,k(7L)),Map.of(1,k(8L))));
        assertFalse(c.insercao("filho","gestao_local_id",7L));
    }
    @Test void descritorNaoSerializaCpfSenhaOuParametroBruto(){
        String segredo="SENHA_FICTICIA_QUE_NAO_SAI";var c=new ComandoJdbcAtor(15,"insert into pessoa(cpf) values (?)",Map.of(1,k(segredo)));
        assertFalse(c.toString().contains(segredo));assertFalse(c.conjuntos().toString().contains(segredo));
    }
    @Test void colunaPrimariaPersonalizadaNaoEConfundidaComIdGenerico(){
        var c=new ComandoJdbcAtor(16,"select c.id_contrato from contrato c where c.id_contrato=? for no key update",Map.of(1,k(42L)));
        assertTrue(c.selecaoChave("contrato","id_contrato",42L));
        assertFalse(c.selecaoId("contrato",42L));
        assertFalse(c.selecaoChave("contrato","id_contrato",43L));
    }
    @Test void selectComShareAindaExigeTabelaEChaveExatas(){
        var c=new ComandoJdbcAtor(17,"select s.id_superintendencia from superintendencia_estadual s where s.id_superintendencia=? for share",Map.of(1,k(12L)));
        assertTrue(c.selecaoChave("superintendencia_estadual","id_superintendencia",12L));
        assertFalse(c.selecaoChave("superintendencia_estadual","id_superintendencia",13L));
        assertFalse(new ComandoJdbcAtor(17,"select s.id_superintendencia from superintendencia_estadual s where s.id_superintendencia=?",Map.of(1,k(12L)))
                .selecaoChave("superintendencia_estadual","id_superintendencia",12L));
    }
    @Test void alteracaoDaChaveNaoAceitaOutroIdOuOutraTabela(){
        var c=new ComandoJdbcAtor(18,"update usuario_aplicacao set active=?,version=? where id=? and version=?",Map.of(2,k(1L),3,k(42L),4,k(0L)));
        assertTrue(c.alteracaoNaChave("usuario_aplicacao","id",42L));
        assertFalse(c.alteracaoNaChave("usuario_aplicacao","id",43L));
        assertFalse(c.alteracaoNaChave("contrato","id",42L));
    }
    @Test void predicadoComOuNaoProvaQualLinhaCausouEspera(){
        var c=new ComandoJdbcAtor(19,"select p.id from pessoa p where p.id=? or p.id=? for update",Map.of(1,k(42L),2,k(43L)));
        assertFalse(c.selecaoId("pessoa",42L));
    }
    @Test void idSemQualificadorEmJoinNaoProvaTabelaDeOrigem(){
        var c=new ComandoJdbcAtor(20,"select u.id from usuario_aplicacao u join pessoa p on p.id=u.id where id=? for update",Map.of(1,k(42L)));
        assertFalse(c.selecaoId("usuario_aplicacao",42L));
    }
    @Test void insertComMultiplasLinhasNaoProvaQualDelasCausouEspera(){
        var c=new ComandoJdbcAtor(21,"insert into filho (id_gestao_local) values (?),(?)",Map.of(1,k(42L),2,k(43L)));
        assertFalse(c.insercao("filho","id_gestao_local",42L));
    }
    @Test void selectComInDeUmaChaveReconheceLockCompartilhadoDoHibernate(){
        var c=new ComandoJdbcAtor(22,"select tbl.id from usuario_aplicacao tbl where tbl.id in (?) for share of tbl",Map.of(1,k(42L)));
        assertTrue(c.selecaoId("usuario_aplicacao",42L));
        assertFalse(c.selecaoId("usuario_aplicacao",43L));
        assertFalse(c.selecaoId("pessoa",42L));
    }
    @Test void inComVariasChavesOuExpressaoNaoProvaQualRegistroCausouEspera(){
        for(String predicado:List.of("u.id in (?,?)","u.id in (?+?)","u.id in (select id from pessoa where id=?)")) {
            var c=new ComandoJdbcAtor(23,"select u.id from usuario_aplicacao u where "+predicado+" for share",Map.of(1,k(42L),2,k(43L)));
            assertFalse(c.selecaoId("usuario_aplicacao",42L));
        }
    }
}
