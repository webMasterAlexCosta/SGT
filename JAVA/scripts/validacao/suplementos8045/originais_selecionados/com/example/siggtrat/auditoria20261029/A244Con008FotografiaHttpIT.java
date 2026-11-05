package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Detects historical fields reconstructed from any changed current master. */
class A244Con008FotografiaHttpIT extends A244Suporte {
    @Test void con008_004_allPublishedValuesSurviveIndependentMasterChanges() throws Exception {
        var c=fixture();var today=RelogioSistema.hoje();var start=today.minusDays(20);var end=today.plusDays(40);
        String companyCnpj=cnpj(990000000000L+c.contratoA.getId());
        long supplier=tx(() -> {
            var type=em.find(TipoContratacao.class,c.tipoA.getId());type.setDescricao("Tipo original A244");type.setMatriculaVinculoObrigatoria(true);
            em.find(GestaoLocal.class,c.gestaoA.getId()).setNome("Gestão original A244 "+c.gestaoA.getId());
            var lot=em.find(Lotacao.class,c.lotacaoA.getId());lot.setNome("Lotação original A244");lot.setMunicipio("Município original A244");
            em.find(Jornada.class,c.jornadaA.getId()).setDescricao("Jornada original A244 8h");
            em.find(Cargo.class,c.cargoA.getId()).setNome("Cargo original A244");
            var f=new Fornecedor();f.setCnpj(companyCnpj);f.setRazaoSocial("Empresa original A244");f.setEmail("original-a244@example.test");em.persist(f);em.flush();return f.getId();
        });
        var req=rascunho(c,"A244-FOTO-"+c.contratoA.getId(),10,start,end);
        req.put("descricao","Descrição contratual original A244");req.put("fornecedorId",supplier);
        req.put("cnpjEmpresa",companyCnpj);req.put("razaoSocialEmpresa","Empresa original A244");req.put("nomeFantasiaEmpresa","Fantasia original A244");
        req.put("matriculaVinculoObrigatoria",null);req.put("diaSemanaReduzido","WEDNESDAY");req.put("horasDiaReduzido",6);
        long id=enviar(c.localA,"/api/contratos",req,201,"A244 complete original draft").path("id").asLong();
        long comp=enviar(c.localA,base(id)+"/composicoes",composicao(c,c.cargoA.getId(),10),201,"A244 complete original composition").path("id").asLong();
        var publication=mapa("dataPublicacao",today,"referenciaSei","SEI-SINTETICO-A244-FOTO","justificativa","Fotografia original sintética completa");
        var preview=enviar(c.localA,base(id)+"/previas/publicar",publication,200,"A244 complete publication preview");
        publication.put("tokenPrevia",preview.path("tokenPrevia").asText());publication.put("confirmarImpacto",true);
        enviar(c.localA,base(id)+"/publicar",publication,200,"A244 complete publication commit");
        var versions=consultar(c.localA,base(id)+"/versoes","A244 original stored version");assertThat(versions).hasSize(1);
        var original=versionValues(versions.get(0));
        var historical=versionValues(consultar(c.localA,base(id)+"/posicao-temporal?data="+start,"A244 original reconstructed values").path("versaoContratual"));
        assertThat(original.path("numeroContrato").asText()).isEqualTo(req.get("numeroContrato"));
        assertThat(original.path("anoContrato").asInt()).isEqualTo(start.getYear());
        assertThat(original.path("descricao").asText()).isEqualTo("Descrição contratual original A244");
        assertThat(original.path("tipoContratacaoId").asLong()).isEqualTo(c.tipoA.getId());
        assertThat(original.path("tipoContratacaoDescricao").asText()).isEqualTo("Tipo original A244");
        assertThat(original.path("fornecedorId").asLong()).isEqualTo(supplier);
        assertThat(original.path("cnpjEmpresa").asText()).isEqualTo(companyCnpj);
        assertThat(original.path("razaoSocialEmpresa").asText()).isEqualTo("Empresa original A244");
        assertThat(original.path("nomeFantasiaEmpresa").asText()).isEqualTo("Fantasia original A244");
        assertThat(original.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(original.path("gestaoLocalNome").asText()).isEqualTo("Gestão original A244 "+c.gestaoA.getId());
        assertThat(original.path("dataInicio").asText()).isEqualTo(start.toString());
        assertThat(original.path("dataFim").asText()).isEqualTo(end.toString());
        assertThat(original.path("efetivoPrevisto").asInt()).isEqualTo(10);
        assertThat(original.path("matriculaVinculoObrigatoria").isNull()).isTrue();
        assertThat(original.path("matriculaTipoContratacaoObrigatoria").asBoolean()).isTrue();
        assertThat(original.path("matriculaVinculoObrigatoriaEfetiva").asBoolean()).isTrue();
        assertThat(original.path("diaSemanaReduzido").asText()).isEqualTo("WEDNESDAY");
        assertThat(original.path("horasDiaReduzido").asInt()).isEqualTo(6);
        assertThat(original.path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        assertThat(original.path("id").asLong()).isPositive();assertThat(original.path("eventoOrigemId").asLong()).isPositive();
        assertThat(original.path("numeroVersao").asInt()).isEqualTo(1);
        assertThat(original.path("registradoEm").asText()).isNotBlank();
        var compositionBefore=AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,base(id)+"/composicoes?data="+start,"A244 original complete composition read"));
        assertThat(compositionBefore.dados()).hasSize(1);
        var compOriginal=byId(compositionBefore.dados(),comp);
        assertThat(compOriginal.path("lotacaoNome").asText()).isEqualTo("Lotação original A244");
        assertThat(compOriginal.path("jornadaDescricao").asText()).isEqualTo("Jornada original A244 8h");
        assertThat(compOriginal.path("cargoNome").asText()).isEqualTo("Cargo original A244");
        assertThat(compOriginal.path("lotacaoMunicipio").asText()).isEqualTo("MUNICÍPIO ORIGINAL A244");
        assertThat(compOriginal.path("jornadaSemanal").asText()).isNotBlank();
        Instant knownOriginal=Instant.now();
        List<Object> changes=new ArrayList<>();
        for(String master:List.of("fornecedor","tipo","gestao","lotacao","jornada","cargo")) {
            String control=tx(() -> {
                switch(master) {
                    case "fornecedor" -> {var f=em.find(Fornecedor.class,supplier);f.setRazaoSocial("Empresa atual A244");f.setCnpj(cnpj(980000000000L+c.contratoA.getId()));f.setEmail("atual-a244@example.test");f.setAtivo(false);return f.getRazaoSocial();}
                    case "tipo" -> {var t=em.find(TipoContratacao.class,c.tipoA.getId());t.setDescricao("Tipo atual A244");t.setMatriculaVinculoObrigatoria(false);return t.getDescricao();}
                    case "gestao" -> {var g=em.find(GestaoLocal.class,c.gestaoA.getId());g.setNome("Gestão atual A244 "+g.getId());return g.getNome();}
                    case "lotacao" -> {var l=em.find(Lotacao.class,c.lotacaoA.getId());l.setNome("Lotação atual A244");l.setMunicipio("Município atual A244");return l.getNome();}
                    case "jornada" -> {var j=em.find(Jornada.class,c.jornadaA.getId());j.setDescricao("Jornada atual A244 4h");for(var d:j.getDias())if(Boolean.TRUE.equals(d.getDiaTrabalho())){d.setEntrada1("08:00");d.setSaida1("12:00");d.setEntrada2(null);d.setSaida2(null);}return j.getDescricao();}
                    case "cargo" -> {var x=em.find(Cargo.class,c.cargoA.getId());x.setNome("Cargo atual A244");return x.getNome();}
                    default -> throw new AssertionError(master);
                }
            });
            assertThat(control).contains("atual");
            String independentControl=readTx(() -> {
                switch(master) {
                    case "fornecedor" -> {var f=em.find(Fornecedor.class,supplier);assertThat(f.getCnpj()).isEqualTo(cnpj(980000000000L+c.contratoA.getId()));assertThat(f.getAtivo()).isFalse();assertThat(f.getEmail()).isEqualTo("atual-a244@example.test");return f.getRazaoSocial();}
                    case "tipo" -> {var t=em.find(TipoContratacao.class,c.tipoA.getId());assertThat(t.getMatriculaVinculoObrigatoria()).isFalse();return t.getDescricao();}
                    case "gestao" -> {return em.find(GestaoLocal.class,c.gestaoA.getId()).getNome();}
                    case "lotacao" -> {var l=em.find(Lotacao.class,c.lotacaoA.getId());assertThat(l.getMunicipio()).isEqualTo("MUNICÍPIO ATUAL A244");return l.getNome();}
                    case "jornada" -> {var j=em.find(Jornada.class,c.jornadaA.getId());assertThat(j.getDias().stream().filter(d->Boolean.TRUE.equals(d.getDiaTrabalho())).toList()).isNotEmpty();for(var d:j.getDias())if(Boolean.TRUE.equals(d.getDiaTrabalho())){assertThat(d.getEntrada1()).isEqualTo("08:00");assertThat(d.getSaida1()).isEqualTo("12:00");assertThat(d.getEntrada2()).isNull();assertThat(d.getSaida2()).isNull();}return j.getDescricao();}
                    case "cargo" -> {return em.find(Cargo.class,c.cargoA.getId()).getNome();}
                    default -> throw new AssertionError(master);
                }
            });
            assertThat(independentControl).as("Committed current master was re-read in another transaction").isEqualTo(control);
            var stored=consultar(c.localA,base(id)+"/versoes","A244 immutable original after "+master);
            assertThat(versionValues(byId(stored,original.path("id").asLong()))).as("Every stored field after "+master).isEqualTo(original);
            var reconstructed=versionValues(consultar(c.localA,base(id)+"/posicao-temporal?data="+start,"A244 reconstruction after current "+master).path("versaoContratual"));
            assertThat(reconstructed).as("Every reconstructed version field after "+master).isEqualTo(historical);
            var atKnown=versionValues(consultar(c.localA,base(id)+"/posicao-temporal?data="+start+"&conhecidoAte="+knownOriginal,"A244 original knowledge after "+master).path("versaoContratual"));
            assertThat(atKnown).isEqualTo(historical);
            var compAfter=AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,base(id)+"/composicoes?data="+start,"A244 all composition values after "+master));
            AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(compositionBefore,compAfter);
            changes.add(mapa("master",master,"currentChangedValue",independentControl,"independentCurrentRead",true,"storedAllFieldsEqual",true,"reconstructedAllFieldsEqual",true,"compositionAllFieldsExceptValidatedReadInstantEqual",true));
        }
        record("CON-008:004",mapa("originalVersionAllFields",original,"originalHistoricalVersionAllFields",historical,
                "originalComposition",compositionBefore.dados(),"knownOriginal",knownOriginal,"changes",changes,
                "scope","One fully populated synthetic publication; all returned values compared after six independent master commits. No authentic administrative source claim."));
    }
    private static String cnpj(long seed) {
        String base=String.format("%012d",seed);
        for(int[] weights:new int[][]{{5,4,3,2,9,8,7,6,5,4,3,2},{6,5,4,3,2,9,8,7,6,5,4,3,2}}) {
            int total=0;for(int i=0;i<weights.length;i++)total+=(base.charAt(i)-'0')*weights[i];
            int rest=total%11;base+=rest<2?0:11-rest;
        }
        return base;
    }
}
