package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.AssinaturaImpactoVinculoServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** A assinatura cobre todo o conjunto sem crescer uma String por dependência no resultado. */
class PessoasAssinaturaImpactoVolumeHttpIT extends AuditoriaHttpSuporte {
    @Autowired AssinaturaImpactoVinculoServico assinaturas;

    @Test
    void assinaturaCompactaIncluiAlteracaoNaUltimaPaginaDeFrequencias() throws Exception {
        Cenario c=criarCenario(); em.flush();
        List<String> semFrequencias=assinaturas.partesPorContrato(c.contratoA.getId(),INICIO,FIM);
        Frequencia ultima=null;
        for(int i=0;i<450;i++)ultima=frequencia(c,false,INICIO.plusDays(i),"FALTA");
        em.flush();
        assertThat(em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v",Long.class)
                .setParameter("v",c.vinculoA.getId()).getSingleResult()).isEqualTo(450);
        assertThat(ultima.getId()).isEqualTo(em.createQuery("select max(f.id) from Frequencia f where f.vinculoContratual.id=:v",Long.class)
                .setParameter("v",c.vinculoA.getId()).getSingleResult());
        List<String> comFrequencias=assinaturas.partesPorContrato(c.contratoA.getId(),INICIO,FIM);
        Map<String,Object> req=new LinkedHashMap<>(Map.of("novoInicio",INICIO.plusDays(1),
                "justificativa","Retificação com volume de dependências","evidencia","SEI-VOLUME","autorizacao","ATO-VOLUME"));
        String base="/api/vinculos-contratuais/"+c.vinculoA.getId();
        JsonNode previa=dados(executar(c.localA,post(base+"/previas/retificacao-inicio").contentType(MediaType.APPLICATION_JSON).content(json(req)),
                200,"VIN014 prévia cobre 450 frequências em mais de uma página"));
        ultima.setBloqueadaEdicaoOperacional(true); em.flush();
        List<String> alteradas=assinaturas.partesPorContrato(c.contratoA.getId(),INICIO,FIM);
        assertThat(alteradas).as("A última frequência deve participar do digest, sem truncamento").isNotEqualTo(comFrequencias);
        req.put("tokenPrevia",previa.path("token").asText()); req.put("confirmarImpacto",true);
        executar(c.localA,post(base+"/retificar-inicio").contentType(MediaType.APPLICATION_JSON).content(json(req)),409,
                "VIN014 alteração do último dependente invalida a confirmação");
        assertThat(c.vinculoA.getDataInicio()).isEqualTo(INICIO);
        assertThat(comFrequencias.size()).as("Resultado da assinatura tem tamanho constante por categoria, independentemente das 450 linhas")
                .isEqualTo(semFrequencias.size());
        assertThat(alteradas.size()).isEqualTo(semFrequencias.size());
    }
}
