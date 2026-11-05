package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import com.example.siggtrat.dominio.frequencia.ValidadeOperacionalFrequencia;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** Detects a missing contract filter or a pendency pointing at a different fact. */
class A244Con005PendenciasHttpIT extends A244Suporte {
    @Test void con005_002_nonEmptyFactsBelongToTheSelectedContract() throws Exception {
        var c=fixture();var n=published(c,100,50,false);var other=published(c,5,null,false);
        var adi=aditamento("A244-PEND-ADI",n.inicio().plusDays(1));adi.put("novaDescricao","Descrição própria A244");
        var adiSaved=registrarAditamento(c,n.id(),adi);
        var rem=remanejamento(n,"A244-PEND-REM",n.inicio().plusDays(2),10);previaRemanejamento(c,n.id(),rem);
        var remSaved=enviar(c.localA,base(n.id())+"/remanejamentos",rem,201,"A244 own remanejamento");
        var v=link(c,n,n.composicao1(),n.inicio(),711000000);
        var otherV=link(c,other,other.composicao1(),other.inicio(),712000000);
        long pend=makePending(c,v.path("id").asLong()),otherPend=makePending(c,otherV.path("id").asLong());
        var selected=consultar(c.localA,"/api/pendencias?contratoIds="+n.id(),"A244 non-empty selected contract pendencies");
        var control=consultar(c.localA,"/api/pendencias?contratoIds="+other.id(),"A244 same-manager different-contract control");
        assertThat(selected.path("totalElementos").asLong()).isEqualTo(1);
        assertThat(control.path("totalElementos").asLong()).isEqualTo(1);
        var row=byId(selected.path("registros"),pend);var otherRow=byId(control.path("registros"),otherPend);
        assertThat(row.path("contratoId").asLong()).isEqualTo(n.id());
        assertThat(row.path("vinculoId").asLong()).isEqualTo(v.path("id").asLong());
        assertThat(row.path("frequenciaId").asLong()).isEqualTo(row.path("origemId").asLong());
        assertThat(row.path("origemEntidade").asText()).isEqualTo("Frequencia");
        assertThat(otherRow.path("contratoId").asLong()).isEqualTo(other.id());
        assertThat(otherRow.path("id").asLong()).isNotEqualTo(pend);
        var compositions=consultar(c.localA,base(n.id())+"/composicoes","A244 composition ownership");
        assertThat(compositions).hasSize(2);for(var comp:compositions)assertThat(comp.path("contratoId").asLong()).isEqualTo(n.id());
        var adis=consultar(c.localA,base(n.id())+"/aditamentos","A244 ADI ownership");
        var rems=consultar(c.localA,base(n.id())+"/remanejamentos","A244 REM ownership");
        assertThat(adis).hasSize(1);assertThat(rems).hasSize(1);
        assertThat(byId(adis,adiSaved.path("id").asLong()).path("contratoId").asLong()).isEqualTo(n.id());
        assertThat(byId(rems,remSaved.path("id").asLong()).path("contratoId").asLong()).isEqualTo(n.id());
        var events=consultar(c.localA,base(n.id())+"/eventos","A244 exact contract event facts");
        assertThat(events).hasSize(3);
        assertThat(byId(events,adiSaved.path("eventoId").asLong()).path("tipoEvento").asText()).isEqualTo("ADITAMENTO");
        assertThat(byId(events,remSaved.path("eventoId").asLong()).path("tipoEvento").asText()).isEqualTo("REMANEJAMENTO");
        readTx(() -> {
            Frequencia f=em.find(Frequencia.class,row.path("frequenciaId").asLong());
            assertThat(f.getVinculoContratual().getContrato().getId()).isEqualTo(n.id());return null;
        });
        record("CON-005:002",mapa("selected",selected,"otherContractControl",control,"compositions",compositions,
                "aditamentos",adis,"remanejamentos",rems,"eventIds",List.of(adiSaved.path("eventoId").asLong(),remSaved.path("eventoId").asLong())));
    }
    private long makePending(Cenario c,long link) throws Exception {
        var date=RelogioSistema.hoje().with(TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY));
        JsonNode saved=enviar(c.localA,"/api/frequencias",mapa("vinculoContratualId",link,"dataReferencia",date,
                "status","FALTA","justificativaRetificacao","Frequência sintética para pendência A244",
                "chaveIdempotencia","A244-"+UUID.randomUUID()),200,"A244 real frequency preparation");
        return tx(() -> {
            Frequencia f=em.find(Frequencia.class,saved.path("id").asLong());
            f.setEstadoAuditoria(EstadoAuditoriaFrequencia.SUJEITA_REVISAO);
            f.setValidadeOperacional(ValidadeOperacionalFrequencia.SUJEITA_REVISAO);f.setBloqueadaEdicaoOperacional(true);
            // Explicit fixture state: this test judges route ownership, not automatic impact discovery.
            var p=pendenciasA244.registrarRevisaoFrequencia(f,"Fixture sintética A244 de revisão pendente");em.flush();return p.getId();
        });
    }
}
