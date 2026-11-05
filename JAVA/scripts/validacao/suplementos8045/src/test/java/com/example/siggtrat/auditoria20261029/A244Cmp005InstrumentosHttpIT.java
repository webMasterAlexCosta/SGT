package com.example.siggtrat.auditoria20261029;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Detects loss/rewriting of the zeroing instrument when a later act restores it. */
class A244Cmp005InstrumentosHttpIT extends A244Suporte {
    @Test void cmp005_004_zeroAndRestorePreserveBothInstrumentContents() throws Exception {
        var c=fixture();var n=published(c,100,25,false);
        var zero=n.inicio().plusDays(2);var restore=zero.plusDays(3);
        var first=aditamento("A244-ZERAR",zero);first.put("novoEfetivoTotal",75);
        first.put("alteracoesComposicao",List.of(mapa("composicaoId",n.composicao1(),"quantidadeResultante",0)));
        var zeroSaved=registrarAditamento(c,n.id(),first);
        var before=consultar(c.localA,base(n.id())+"/aditamentos","A244 zero instrument independently read");assertThat(before).hasSize(1);
        var zeroOriginal=byId(before,zeroSaved.path("id").asLong()).deepCopy();
        var zeroEvents=consultar(c.localA,base(n.id())+"/eventos","A244 zero event before restoration");
        var second=aditamento("A244-RESTAURAR",restore);second.put("novoEfetivoTotal",100);
        second.put("alteracoesComposicao",List.of(mapa("composicaoId",n.composicao1(),"quantidadeResultante",25)));
        var restoreSaved=registrarAditamento(c,n.id(),second);
        var after=consultar(c.localA,base(n.id())+"/aditamentos","A244 both instruments after independent commits");assertThat(after).hasSize(2);
        var firstRead=byId(after,zeroSaved.path("id").asLong());var secondRead=byId(after,restoreSaved.path("id").asLong());
        assertThat(firstRead).as("Entire zero instrument DTO preserved").isEqualTo(zeroOriginal);
        assertThat(secondRead).as("Entire restore instrument DTO re-read").isEqualTo(restoreSaved);
        assertThat(firstRead.path("identificacao").asText()).isEqualTo("A244-ZERAR");
        assertThat(secondRead.path("identificacao").asText()).isEqualTo("A244-RESTAURAR");
        assertThat(firstRead.path("dataEfetiva").asText()).isEqualTo(zero.toString());
        assertThat(secondRead.path("dataEfetiva").asText()).isEqualTo(restore.toString());
        assertThat(firstRead.path("alteracoesComposicao").get(0).path("quantidadeResultante").asInt()).isZero();
        assertThat(secondRead.path("alteracoesComposicao").get(0).path("quantidadeResultante").asInt()).isEqualTo(25);
        for(var instrument:List.of(firstRead,secondRead)) {
            assertThat(instrument.path("contratoId").asLong()).isEqualTo(n.id());
            assertThat(instrument.path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId());
            assertThat(instrument.path("situacao").asText()).isEqualTo("VIGENTE");
            assertThat(instrument.path("registradoEm").asText()).isNotBlank();
        }
        var during=consultar(c.localA,base(n.id())+"/composicoes?data="+zero.plusDays(1),"A244 preserved zero interval");
        var restored=consultar(c.localA,base(n.id())+"/composicoes?data="+restore,"A244 restored later interval");
        assertThat(byId(during,n.composicao1()).path("efetivoPrevisto").asInt()).isZero();
        assertThat(byId(restored,n.composicao1()).path("efetivoPrevisto").asInt()).isEqualTo(25);
        var events=consultar(c.localA,base(n.id())+"/eventos","A244 both events retained");preservesRows(zeroEvents,events,"Zeroing event remains intact");
        assertThat(byId(events,restoreSaved.path("eventoId").asLong()).path("tipoEvento").asText()).isEqualTo("ADITAMENTO");
        record("CMP-005:004",mapa("instruments",after,"duringZero",during,"afterRestore",restored,"originalEventRowsPreserved",true));
    }
}
