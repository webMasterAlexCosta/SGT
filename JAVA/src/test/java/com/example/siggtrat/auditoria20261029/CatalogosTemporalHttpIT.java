package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class CatalogosTemporalHttpIT extends AuditoriaHttpSuporte {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    @ParameterizedTest
    @ValueSource(strings={"cargos","lotacoes","jornadas"})
    void programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa(String recurso) throws Exception {
        Cenario c=criarCenario(); Long id=id(c,recurso);
        Map<String,Object> req=cadastro(c,RelogioSistema.hoje().plusDays(5),810000000);
        JsonNode futuro=dados(executar(c.localA,post("/api/vinculos-contratuais").contentType(MediaType.APPLICATION_JSON).content(json(req)),201,"CAT005 programação futura usa catálogo "+recurso));
        executar(c.localA,delete("/api/"+recurso+"/"+id),409,"CAT005 inativação bloqueada enquanto existe programação "+recurso);
        executar(c.localA,post("/api/vinculos-contratuais/"+futuro.path("id").asLong()+"/cancelar-programacao")
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("justificativa","Programação não ocorrerá","evidencia","SEI-1","autorizacao","Ato 1"))),200,"Tratamento explícito da programação");
        executar(c.localA,delete("/api/"+recurso+"/"+id),409,"CAT005 usos vigentes ainda cobrem dias futuros depois de cancelar só a nova programação "+recurso);
        JsonNode preservado=dados(executar(c.localA,get("/api/"+recurso+"/"+id),200,"CAT003 cadastro inativo permanece consultável"));
        assertThat(preservado.path("ativo").asBoolean()).isTrue(); assertThat(preservado.path("id").asLong()).isEqualTo(id);
    }

    @ParameterizedTest
    @ValueSource(strings={"cargos","lotacoes","jornadas"})
    void catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura(String recurso) throws Exception {
        Cenario c=criarCenario(); Long id=id(c,recurso);
        EventoDisponibilidadeCatalogo origem=new EventoDisponibilidadeCatalogo(); origem.setTipoCatalogo(tipo(recurso)); origem.setCatalogoId(id);
        origem.setDataEfetiva(INICIO); origem.setAtivo(true); origem.setMotivo("Disponibilidade original comprovada");
        origem.setResponsavelId(c.localA.getId()); origem.setResponsavelUsername(c.localA.getUsername()); em.persist(origem);
        if(recurso.equals("cargos")) c.cargoA.setAtivo(false);
        else if(recurso.equals("lotacoes")) c.lotacaoA.setAtivo(false); else c.jornadaA.setAtivo(false);
        EventoDisponibilidadeCatalogo limite=new EventoDisponibilidadeCatalogo(); limite.setTipoCatalogo(tipo(recurso)); limite.setCatalogoId(id);
        limite.setDataEfetiva(RelogioSistema.hoje()); limite.setAtivo(false); limite.setMotivo("Inativação posterior à faixa histórica");
        limite.setResponsavelId(c.localA.getId()); limite.setResponsavelUsername(c.localA.getUsername()); em.persist(limite);
        JsonNode retro=dados(executar(c.localA,post("/api/vinculos-contratuais").contentType(MediaType.APPLICATION_JSON)
                .content(json(cadastro(c,DATA,820000000))),201,"CAT007 fato retroativo usa catálogo válido na época "+recurso));
        assertThat(retro.path("dataInicio").asText()).isEqualTo(DATA.toString());
        executar(c.localA,post("/api/vinculos-contratuais").contentType(MediaType.APPLICATION_JSON)
                .content(json(cadastro(c,RelogioSistema.hoje().plusDays(5),830000000))),409,"CAT008 catálogo inativo não aceita programação futura "+recurso);
    }

    @Test
    void motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior() throws Exception {
        Cenario c=criarCenario();
        declararFonteNominalSintetica(c, "CAT007-MOTIVO-HISTORICO");
        MotivoDesligamento m=new MotivoDesligamento(); m.setDescricao("Causa histórica "+c.gestaoA.getId());
        m.setGestaoLocal(c.gestaoA); m.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        org.springframework.test.util.ReflectionTestUtils.setField(m,"criadoEm",Instant.parse("2025-01-01T00:00:00Z")); em.persist(m);
        // A fixture declara fonte ativa anterior à inativação; criadoEm sozinho
        // não autoriza inferir disponibilidade retroativa em produção.
        EventoDisponibilidadeCatalogo origem=new EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO); origem.setCatalogoId(m.getId());
        origem.setDataEfetiva(java.time.LocalDate.of(2025,1,1)); origem.setAtivo(true);
        origem.setRegistradoEm(Instant.parse("2025-01-01T00:00:00Z"));
        origem.setMotivo("Fonte sintética explícita do motivo ativo desde 2025, anterior à inativação"); em.persist(origem);
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), m.getId(), c.gestaoA.getId(), c.localA,
                "Causa histórica " + c.gestaoA.getId(), CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                DATA, DATA, Instant.now(), "SEI-SINTETICO-CAT007-CONTEXTO-MOTIVO");
        executar(c.localA,put("/api/motivos-desligamento/"+m.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("descricao",m.getDescricao(),"categoria",m.getCategoria(),"ativo",false))),200,"CAT007 inativação do motivo atual");
        JsonNode fim=dados(executar(c.localA,post("/api/vinculos-contratuais/"+c.vinculoA.getId()+"/desligar").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("dataFim",DATA,"motivoDesligamentoId",m.getId(),"justificativa","Registro posterior do fato real",
                        "evidencia","Documento histórico","autorizacao","Ato 2"))),200,"CAT007 DES003 motivo válido na época do desligamento"));
        assertThat(fim.path("motivoDesligamentoId").asLong()).isEqualTo(m.getId());
        assertThat(fim.path("dataFim").asText()).isEqualTo(DATA.toString());
    }

    private Map<String,Object> cadastro(Cenario c,java.time.LocalDate inicio,int semente) {
        return new LinkedHashMap<>(Map.of("pessoa",Map.of("nome","Pessoa temporal "+semente,"cpf",cpf(semente+c.pessoaA.getId().intValue())),
                "contratoId",c.contratoA.getId(),"composicaoId",c.composicaoA.getId(),"dataInicio",inicio));
    }
    private Long id(Cenario c,String recurso) { return switch(recurso) { case "cargos"->c.cargoA.getId(); case "lotacoes"->c.lotacaoA.getId(); default->c.jornadaA.getId(); }; }
    private TipoCatalogoTemporal tipo(String recurso) { return switch(recurso) { case "cargos"->TipoCatalogoTemporal.CARGO; case "lotacoes"->TipoCatalogoTemporal.LOTACAO; default->TipoCatalogoTemporal.JORNADA; }; }
}
