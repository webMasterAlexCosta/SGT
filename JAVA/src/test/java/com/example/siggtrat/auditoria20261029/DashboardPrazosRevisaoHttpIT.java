package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** PostgreSQL real + cadeia MVC/security/service. Janela de lançamento e de edição independentes. */
@TestPropertySource(properties={"frequencia.janela-operacional-dias=7","frequencia.edicao-horas=72"})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class DashboardPrazosRevisaoHttpIT extends ContratosHttpBase {
    @Autowired PoliticaTemporalFrequenciaServico politica;
    private static final ZoneId ZONA=ZoneId.of("America/Sao_Paulo");

    @Test void operacionalSolicitaGestorDecideSemDuplicarEComAuditoria() throws Exception {
        Cenario c=criarCenarioPersistido();declararFonteNominalSintetica(c,"DASHBOARD-REVISAO-PROPOSTA");
        // Mesmo cadastro da jornada real: escopo por vínculos, sem o par legado no usuário.
        emTransacao(() -> {
            var u=em.find(UsuarioAplicacao.class,c.operacionalA.getId());
            var lotacao=em.find(com.example.siggtrat.entidade.Lotacao.class,c.lotacaoA.getId());
            var jornada=em.find(com.example.siggtrat.entidade.Jornada.class,c.jornadaA.getId());
            if(!lotacao.getTurnos().contains(jornada)) lotacao.getTurnos().add(jornada);
            u.setLotacao(null);u.setJornada(null);
            em.persist(new com.example.siggtrat.entidade.VinculoGestaoUsuarioAplicacao(u,lotacao,jornada));
            em.flush();return null;
        });
        LocalDate dia=LocalDate.now(ZONA);
        JsonNode inicial=enviar(c.operacionalA,"/api/frequencias",corpo(c,dia,"FALTA",false),200,"Lançamento operacional");
        long id=inicial.path("id").asLong();String rota="/api/frequencias/"+id+"/revisao";
        var pedido=mapa("versao",inicial.path("versaoNegocial").asInt(),"motivo","Conferir justificativa da ausência","chaveIdempotencia",UUID.randomUUID().toString());
        var resposta=mvc.perform(autenticar(post(rota).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json(pedido)),c.operacionalA)).andReturn();
        if(resposta.getResolvedException()!=null) throw new AssertionError("Revisão com escopo por vínculos falhou",resposta.getResolvedException());
        assertEquals(200,resposta.getResponse().getStatus());
        JsonNode pendente=dados(resposta);
        assertEquals("PENDENTE",pendente.path("status").asText());
        assertEquals("Conferir justificativa da ausência",consultar(c.localA,rota,"Gestor autorizado vê motivo").path("motivo").asText());
        for(var leitor:List.of(c.admin,c.nacional,c.estadualA)) {
            var minimizada=consultar(leitor,rota,"Justificativa restrita segue política de leitura");
            assertTrue(minimizada.path("motivo").isNull());
            assertFalse(minimizada.path("podeSolicitar").asBoolean());
            assertFalse(minimizada.path("podeDecidir").asBoolean());
        }
        assertEquals(pendente,enviar(c.operacionalA,rota,pedido,200,"Reenvio não duplica solicitação"));
        enviar(c.localA,rota,pedido,403,"Gestor não se faz passar por solicitante operacional");
        var decisao=mapa("versao",pendente.path("versao").asInt(),"motivo","Conferência realizada pelo gestor","chaveIdempotencia",UUID.randomUUID().toString(),"decisao","CONFERIDA");
        enviar(c.operacionalA,rota+"/decisao",decisao,403,"Operacional não decide a própria revisão");
        executar(c.operacionalB,get(rota),404,"Solicitação restrita ao escopo");
        JsonNode painel=consultar(c.localA,"/api/painel/analitico?referencia="+dia+"&contratoId="+c.contratoA.getId(),"Revisão no agregado real");
        assertEquals(1,painel.path("indicadores").path("tempestividade").path("emRevisao").asLong());
        JsonNode resolvida=enviar(c.localA,rota+"/decisao",decisao,200,"Gestor conclui conferência");
        assertEquals("CONFERIDA",resolvida.path("status").asText());
        assertFalse(resolvida.path("podeDecidir").asBoolean());
        JsonNode prova=consultar(c.localA,"/api/frequencias/"+id+"/evidencias-integridade","Cadeia da solicitação e decisão");
        assertTrue(prova.path("cadeiaIntegra").asBoolean(),prova.toString());
    }

    @Test void edicaoTerminaExatamenteEmSetentaEDuasHorasDoPrimeiroLancamento() throws Exception {
        Cenario c=criarCenarioPersistido();declararFonteNominalSintetica(c,"DASHBOARD-EDICAO-72H");
        LocalDate dia=LocalDate.now(ZONA);
        JsonNode inicial=enviar(c.operacionalA,"/api/frequencias",corpo(c,dia,"FALTA",false),200,"Primeiro lançamento");
        Instant prazo=LocalDateTime.parse(inicial.path("prazoEdicao").asText()).atZone(ZONA).toInstant();
        try(var tempo=Mockito.mockStatic(Instant.class,Mockito.CALLS_REAL_METHODS)) {
            tempo.when(Instant::now).thenReturn(prazo.minusSeconds(1));
            JsonNode alterada=enviar(c.operacionalA,"/api/frequencias",corpo(c,dia,"PRESENCA",false),200,"Edição antes do limite");
            assertEquals(inicial.path("prazoEdicao"),alterada.path("prazoEdicao"));
            tempo.when(Instant::now).thenReturn(prazo);
            enviar(c.operacionalA,"/api/frequencias",corpo(c,dia,"FALTA",false),403,"Limite de edição exclusivo");
        }
    }

    @Test void retroativoExigeGestorForaDasSeteDatasENaoAceitaFuturo() throws Exception {
        Cenario c=criarCenarioPersistido();declararFonteNominalSintetica(c,"DASHBOARD-RETROATIVO");
        LocalDate hoje=LocalDate.now(ZONA);
        enviar(c.operacionalA,"/api/frequencias",corpo(c,hoje.minusDays(7),"FALTA",false),403,"Sete datas civis encerradas");
        JsonNode tardia=enviar(c.localA,"/api/frequencias",corpo(c,hoje.minusDays(7),"FALTA",true),200,"Exceção gerencial auditada");
        assertTrue(tardia.path("bloqueadaEdicaoOperacional").asBoolean());
        assertEquals("FECHADA",tardia.path("estadoNegocial").asText());
        enviar(c.localA,"/api/frequencias",corpo(c,hoje.plusDays(1),"FALTA",true),400,"Gestor também não lança futuro");
    }

    private Map<String,Object> corpo(Cenario c,LocalDate dia,String status,boolean gestor) {
        var r=mapa("vinculoContratualId",c.vinculoA.getId(),"dataReferencia",dia,"status",status);
        if(gestor) {r.put("justificativaRetificacao","Correção retroativa autorizada no laboratório");r.put("chaveIdempotencia",UUID.randomUUID().toString());}
        return r;
    }
}
