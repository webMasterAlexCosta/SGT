package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Detects deletion or reassignment of allocation facts when a contract is annulled. */
class A244Con018AnulacaoHttpIT extends A244Suporte {
    @Test void con018_001_linkAndAllocationRemainQueryableAfterAnnulment() throws Exception {
        var c=fixture();var n=published(c,5,null,false);
        var v=link(c,n,n.composicao1(),n.inicio(),713000000);long vid=v.path("id").asLong();
        var scope=RelogioSistema.hoje().plusDays(5);
        cause(c,n,CategoriaTerminoVinculo.ANULACAO_CONTRATO,n.inicio(),scope);
        String url="/api/vinculos-contratuais/"+vid;
        var before=consultar(c.localA,url+"/alocacoes","A244 allocation before annulment");assertThat(before).hasSize(1);
        var original=before.get(0);long aid=original.path("id").asLong();
        var eventsBefore=consultar(c.localA,url+"/alocacoes/eventos","A244 original allocation event");
        assertThat(eventsBefore.size()).isGreaterThan(0);
        cycle(c,n.id(),"anular",mapa("dataEfetiva",scope,"dataAto",RelogioSistema.hoje(),
                "motivo","Invalidade sintética A244 desde alcance declarado","referenciaSei","SEI-SINTETICO-A244-ANULACAO"));
        var afterV=consultar(c.localA,url,"A244 same link remains addressable");
        assertThat(afterV.path("id").asLong()).isEqualTo(vid);
        assertThat(afterV.path("pessoaId")).isEqualTo(v.path("pessoaId"));
        assertThat(afterV.path("contratoId").asLong()).isEqualTo(n.id());
        var after=consultar(c.localA,url+"/alocacoes","A244 same allocation remains addressable");
        assertThat(after).hasSize(1);var preserved=byId(after,aid);
        for(String field:List.of("id","vinculoId","composicaoBaseId","lotacaoId","lotacaoNome","jornadaId","jornadaDescricao","cargoId","cargoNome","dataInicio","criadoEm","criadoPorId","criadoPorUsername"))
            assertThat(preserved.path(field)).as("Preserved allocation field "+field).isEqualTo(original.path(field));
        var history=consultar(c.localA,url+"/alocacoes/eventos","A244 prior allocation events survive");
        preservesRows(eventsBefore,history,"Annulment does not rewrite allocation history");
        var position=consultar(c.localA,url+"/alocacoes/posicao?data="+scope.minusDays(1),"A244 allocation on the preceding valid day");
        assertThat(position.path("alocacaoId").asLong()).isEqualTo(aid);
        assertThat(position.path("vinculoId").asLong()).isEqualTo(vid);
        assertThat(position.path("composicaoBaseId").asLong()).isEqualTo(n.composicao1());
        assertThat(position.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        var invalid=consultar(c.localA,base(n.id())+"/posicao-temporal?data="+scope,"A244 consultability is not operational validity");
        assertThat(invalid.path("estadoNegocial").asText()).isEqualTo("ANULADO");
        record("CON-018:001",mapa("link",afterV,"allocation",preserved,"historicalPosition",position,
                "annulledContractPosition",invalid,"originalAllocationEventsPreserved",true));
    }
}
