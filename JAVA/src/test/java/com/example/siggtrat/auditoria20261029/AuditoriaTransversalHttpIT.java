package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoFrequencia;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class AuditoriaTransversalHttpIT extends AuditoriaHttpSuporte {
    @Test void retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas() throws Exception {
        var f=criarCenario();long id=0;List<JsonNode> respostas=new ArrayList<>();
        for(int i=1;i<=25;i++) {
            String status=i%2==1?"FALTA":"PRESENCA";
            var p=Map.of("vinculoContratualId",f.vinculoA.getId(),"dataReferencia",DATA.toString(),"status",status,
                    "justificativaRetificacao","Conferência documentada número "+i,"chaveIdempotencia","cadeia-"+f.vinculoA.getId()+"-"+i);
            var response=dados(executar(f.localA,post("/api/frequencias").contentType(MediaType.APPLICATION_JSON).content(json(p)),200,
                    "AUD-001/002/007 retificação sucessiva versão "+i));
            assertEquals(i,response.path("versaoNegocial").asInt());
            if(i==1)id=response.path("id").asLong();else assertEquals(id,response.path("id").asLong());
            assertEquals(status,response.path("status").asText());respostas.add(response);
        }
        em.flush();em.clear();
        var events=dados(executar(f.localA,get("/api/frequencias/{id}/eventos",id),200,"AUD-007 consulta preserva cadeia completa após MySQL"));
        assertEquals(25,events.size());String anterior=null;Set<Long> ids=new HashSet<>();
        for(int i=0;i<25;i++) {
            var e=events.get(i);assertEquals(i+1,e.path("numeroVersao").asInt());assertTrue(ids.add(e.path("id").asLong()));
            assertEquals(f.localA.getId(),e.path("usuarioId").asLong());assertEquals("Conferência documentada número "+(i+1),e.path("motivo").asText());
            assertEquals(DATA.toString(),e.path("dataReferencia").asText());assertFalse(e.path("registradoEm").asText().startsWith(DATA.toString()));
            assertTrue(e.path("snapshotNovo").asText().contains("status="+(i%2==0?"FALTA":"PRESENCA")));
            if(i>0){assertEquals(anterior,e.path("hashAnterior").asText());assertTrue(e.path("snapshotAnterior").asText().contains("status="+(i%2==0?"PRESENCA":"FALTA")));}
            anterior=e.path("hashEvento").asText();assertEquals(64,anterior.length());
        }
        assertEquals(25L,em.createQuery("select count(e) from EventoFrequencia e where e.frequencia.id=:id",Long.class).setParameter("id",id).getSingleResult());
    }
}
