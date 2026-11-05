package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Extensão dos contratos RED: leitura enriquecida e cadeia sem limite artificial de versões. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties={"spring.cache.type=redis","spring.sql.init.mode=never","app.cache.redis.http-get.enabled=true"})
class PessoasProvaLeituraTemporalHttpIT extends ContratosHttpBase {
    @Test
    void posicaoPreservaFotografiaOriginalAusenteEExibeProvaTardiaSeparada() throws Exception {
        Cenario c=criarCenarioPersistido();
        conceder(c,c.contratoA.getId(),"INCORPORACAO_PROVA_HISTORICA");
        Instant antes=Instant.now();
        JsonNode p=registrar(c,"Nome comprovado da posição",null);
        var atual=dados(executar(c.localA,get("/api/vinculos-contratuais/pessoas/"+c.pessoaA.getId()+"/posicao")
                .param("data",DATA.toString()),200,"Posição distingue fonte original e prova recebida depois"));
        assertThat(atual.path("nomeFotografia").isNull()).isTrue();
        assertThat(atual.path("fotografiaIdentidadeComprovada").asBoolean()).isFalse();
        assertThat(atual.path("identidadeHistorica").path("nome").asText()).isEqualTo("Nome comprovado da posição");
        assertThat(atual.path("identidadeHistorica").path("fonteId").asLong()).isEqualTo(p.path("id").asLong());
        var antiga=dados(executar(c.localA,get("/api/vinculos-contratuais/pessoas/"+c.pessoaA.getId()+"/posicao")
                .param("data",DATA.toString()).param("conhecidoAte",antes.toString()),200,"Corte antigo conserva ausência"));
        assertThat(antiga.path("identidadeHistorica").path("comprovado").asBoolean()).isFalse();
        assertThat(antiga.path("identidadeHistorica").path("nome").isNull()).isTrue();
    }
    @Test
    void retificacaoDeRetificacaoConservaTresFontesETresCortesDeConhecimento() throws Exception {
        Cenario c=criarCenarioPersistido();
        conceder(c,c.contratoA.getId(),"INCORPORACAO_PROVA_HISTORICA");
        JsonNode a=registrar(c,"Nome da fonte A",null); Instant ka=Instant.now();
        JsonNode b=registrar(c,"Nome da fonte B",a.path("id").asLong()); Instant kb=Instant.now();
        registrar(c,"Nome da fonte C",b.path("id").asLong());
        assertThat(nome(c,ka).path("nome").asText()).isEqualTo("Nome da fonte A");
        assertThat(nome(c,kb).path("nome").asText()).isEqualTo("Nome da fonte B");
        assertThat(nome(c,null).path("nome").asText()).isEqualTo("Nome da fonte C");
        assertThat(consultar(c.localA,rota(c),"Todas as fontes permanecem íntegras")).hasSize(3);
    }
    private String rota(Cenario c){return "/api/pessoas/"+c.pessoaA.getId()+"/provas-nome";}
    private JsonNode nome(Cenario c,Instant k)throws Exception {
        var req=get("/api/pessoas/"+c.pessoaA.getId()+"/nome-historico").param("vinculoId",c.vinculoA.getId().toString()).param("data",DATA.toString());
        if(k!=null)req.param("conhecidoAte",k.toString());
        return dados(executar(c.localA,req,200,"Identidade no corte de conhecimento"));
    }
    private JsonNode registrar(Cenario c,String nome,Long anterior)throws Exception {
        Map<String,Object> r=mapa("vinculoId",c.vinculoA.getId(),"nomeComprovado",nome,
                "dataInicioEfetiva",DATA,"dataFimEfetiva",DATA.plusDays(2),"provaAnteriorId",anterior,
                "referenciaSei","SEI-SINTETICO-LEITURA","motivo","Fonte sintética explícita delimita identidade no intervalo",
                "autorizacao","ATO-SINTETICO-LEITURA","chaveIdempotencia",UUID.randomUUID().toString(),
                "versaoPessoaEsperada",consultar(c.localA,"/api/pessoas/"+c.pessoaA.getId(),"Versão da Pessoa").path("version").asLong(),
                "versaoVinculoEsperada",consultar(c.localA,"/api/vinculos-contratuais/"+c.vinculoA.getId(),"Versão do Vínculo").path("version").asLong());
        JsonNode previa=enviar(c.localA,rota(c)+"/previa",r,200,"Prévia nominal");
        r.put("confirmarImpacto",true);r.put("tokenPrevia",previa.path("tokenPrevia").asText());
        return enviar(c.localA,rota(c),r,201,"Incorporação nominal");
    }
}
