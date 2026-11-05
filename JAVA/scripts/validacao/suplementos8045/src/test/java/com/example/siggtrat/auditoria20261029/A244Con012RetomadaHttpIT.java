package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.ZoneId;
import static org.assertj.core.api.Assertions.assertThat;

/** Detects implicit administrative reopening or loss of the original block/event. */
class A244Con012RetomadaHttpIT extends A244Suporte {
    @Test void con012_004_onlyExplicitReactivationResumesAfterRetroactiveExtension() throws Exception {
        var c=fixture();var today=RelogioSistema.hoje();var start=today.minusDays(30);var oldEnd=today.minusDays(5);
        long id=enviar(c.localA,"/api/contratos",rascunho(c,"A244-RETOMA-"+c.contratoA.getId(),5,start,oldEnd),201,"A244 expired original contract").path("id").asLong();
        long comp=enviar(c.localA,base(id)+"/composicoes",composicao(c,c.cargoA.getId(),5),201,"A244 original composition").path("id").asLong();
        var publication=mapa("dataPublicacao",today,"referenciaSei","SEI-SINTETICO-A244-RETOMA","justificativa","Fonte sintética da vigência original");
        var preview=enviar(c.localA,base(id)+"/previas/publicar",publication,200,"A244 original preview");
        publication.put("tokenPrevia",preview.path("tokenPrevia").asText());publication.put("confirmarImpacto",true);
        enviar(c.localA,base(id)+"/publicar",publication,200,"A244 original publication");
        var n=new NovoContrato(id,comp,null,start,oldEnd);
        cause(c,n,CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO,start,oldEnd);
        cycle(c,id,"inativar",mapa("dataEfetiva",start.plusDays(1),"motivo","Bloqueio administrativo sintético A244"));
        var originalEvents=consultar(c.localA,base(id)+"/eventos","A244 preserved original/block events");
        var originalVersions=consultar(c.localA,base(id)+"/versoes","A244 preserved original/block versions");
        var extension=aditamento("A244-PRORROGACAO",oldEnd.minusDays(1));extension.put("novaDataFim",today.plusDays(30));
        registrarAditamento(c,id,extension);
        var noResumption=consultar(c.localA,base(id)+"/posicao-temporal?data="+today,"A244 extension alone cannot resume");
        assertThat(noResumption.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(noResumption.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        var beforeExplicit=consultar(c.localA,base(id)+"/eventos","A244 before explicit resume");
        assertThat(beforeExplicit.findValuesAsText("tipoEvento")).doesNotContain("REATIVACAO_ADMINISTRATIVA");
        Instant knownBefore=Instant.now();var effect=today.minusDays(1);
        cycle(c,id,"reativar",mapa("dataEfetiva",effect,"motivo","Retomada administrativa explícita A244"));
        var current=consultar(c.localA,base(id)+"/posicao-temporal?data="+today,"A244 explicit resume current truth");
        var priorKnowledge=consultar(c.localA,base(id)+"/posicao-temporal?data="+today+"&conhecidoAte="+knownBefore,"A244 prior knowledge remains blocked");
        assertThat(current.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        assertThat(priorKnowledge.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        var events=consultar(c.localA,base(id)+"/eventos","A244 new event preserves predecessors");
        preservesRows(originalEvents,events,"Original publication and blocking event immutable");
        preservesRows(originalVersions,consultar(c.localA,base(id)+"/versoes","A244 old version values intact"),"Original/block versions immutable");
        assertThat(events.size()).isEqualTo(beforeExplicit.size()+1);
        var added=events.iterator();com.fasterxml.jackson.databind.JsonNode resume=null;
        while(added.hasNext()) {var e=added.next();if(e.path("tipoEvento").asText().equals("REATIVACAO_ADMINISTRATIVA"))resume=e;}
        assertThat(resume).isNotNull();
        assertThat(resume.path("dataEfetiva").asText()).isEqualTo(effect.toString());
        assertThat(resume.path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId());
        var recorded=Instant.parse(resume.path("registradoEm").asText());
        assertThat(recorded).isAfter(knownBefore);
        assertThat(recorded.atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate()).isAfter(effect);
        record("CON-012:004",mapa("beforeExplicit",noResumption,"priorKnowledge",priorKnowledge,"current",current,"reactivationEvent",resume,
                "knownBefore",knownBefore,"originalEventsPreserved",true,"originalVersionsPreserved",true));
    }
}
