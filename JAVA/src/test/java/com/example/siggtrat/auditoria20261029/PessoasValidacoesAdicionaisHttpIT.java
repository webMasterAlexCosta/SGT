package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class PessoasValidacoesAdicionaisHttpIT extends AuditoriaHttpSuporte {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    @Test
    void correcaoExcepcionalDeIdentidadeExigeDeclaracaoDosImpactosEncontrados() throws Exception {
        Cenario c=criarCenario(); String nome=c.pessoaA.getNome();
        executar(c.localA,post("/api/pessoas/"+c.pessoaA.getId()+"/correcoes-identidade").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("tipo","NOME","novoValor","Nome corrigido","motivo","Erro material","evidencia","SEI-1","autorizacao","Ato 1"))),400,
                "COL006 COL009 correção deve registrar também impactos encontrados");
        assertThat(c.pessoaA.getNome()).isEqualTo(nome);
    }

    @Test
    void previaDeDesligamentoRejeitaMotivoDeOutraGestaoAntesDaConfirmacao() throws Exception {
        Cenario c=criarCenario(); MotivoDesligamento externo=motivo(c.gestaoB); c.vinculoA.setDataFim(DATA);
        executar(c.localA,post("/api/vinculos-contratuais/"+c.vinculoA.getId()+"/previas/retificacao-desligamento")
                .contentType(MediaType.APPLICATION_JSON).content(json(requisicao(externo.getId()))),400,
                "CAT009 prévia valida a causa de término no mesmo escopo do contrato");
    }

    @Test
    void alteracaoDoMotivoDepoisDaPreviaExigeNovaConfirmacao() throws Exception {
        Cenario c=criarCenario(); MotivoDesligamento motivo=motivo(c.gestaoA); c.vinculoA.setDataFim(DATA); c.vinculoA.setMotivoDesligamento(motivo);
        // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(), c.gestaoA.getId(), c.localA,
                "Outra causa documentada antes da prévia", CategoriaTerminoVinculo.OUTRO,
                DATA.plusDays(1), DATA.plusDays(1), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-MOTIVO-PREVIA");
        Map<String,Object> req=requisicao(motivo.getId());
        JsonNode previa=dados(executar(c.localA,post("/api/vinculos-contratuais/"+c.vinculoA.getId()+"/previas/retificacao-desligamento")
                .contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"Prévia inclui motivo de término vigente"));
        motivo.setDescricao("Causa corrigida depois da prévia"); em.flush();
        req.put("tokenPrevia",previa.path("token").asText()); req.put("confirmarImpacto",true);
        executar(c.localA,post("/api/vinculos-contratuais/"+c.vinculoA.getId()+"/retificar-desligamento")
                .contentType(MediaType.APPLICATION_JSON).content(json(req)),409,"CAT004 VIN014 motivo alterado invalida consentimento anterior");
    }

    @Test
    void corpoDoComandoNaoPodeMudarEntrePreviaEConfirmacao() throws Exception {
        Cenario c=criarCenario(); Map<String,Object> req=new LinkedHashMap<>(Map.of("novoInicio",INICIO.plusDays(1),"justificativa","Fato documentado","evidencia","SEI-1","autorizacao","Ato 1"));
        JsonNode previa=dados(executar(c.localA,post("/api/vinculos-contratuais/"+c.vinculoA.getId()+"/previas/retificacao-inicio")
                .contentType(MediaType.APPLICATION_JSON).content(json(req)),200,"Prévia corresponde ao payload informado"));
        req.put("autorizacao","Ato não apresentado na prévia"); req.put("tokenPrevia",previa.path("token").asText()); req.put("confirmarImpacto",true);
        executar(c.localA,post("/api/vinculos-contratuais/"+c.vinculoA.getId()+"/retificar-inicio").contentType(MediaType.APPLICATION_JSON).content(json(req)),409,
                "VIN014 assinatura também protege justificativa evidência e autorização");
    }

    @Test
    void cadastroCanonicoNaoExibeNemPersisteDadosBancarios() throws Exception {
        Cenario c=criarCenario();
        JsonNode atual=dados(executar(c.localA,get("/api/pessoas/"+c.pessoaA.getId()),200,"Cadastro canônico sem dados bancários"));
        assertThat(atual.has("conta")).isFalse();
        var body=mapper.createObjectNode();
        body.putObject("conta").put("numeroConta","999").put("agenciaId",1).put("tipoContaId",1);
        JsonNode salvo=dados(executar(c.localA,put("/api/pessoas/"+c.pessoaA.getId()+"/dados-opcionais")
                .contentType(MediaType.APPLICATION_JSON).content(json(body)),200,"COL004 campo extra não constitui dado da Pessoa"));
        assertThat(salvo.has("conta")).isFalse();
        assertThat(salvo.path("nome")).isEqualTo(atual.path("nome"));
        assertThat(salvo.path("cpf")).isEqualTo(atual.path("cpf"));
    }

    private MotivoDesligamento motivo(GestaoLocal g) {
        MotivoDesligamento m=new MotivoDesligamento(); m.setGestaoLocal(g);m.setDescricao("Motivo "+g.getId());m.setCategoria(CategoriaTerminoVinculo.OUTRO);em.persist(m);
        // A fixture fornece sua própria origem documental sintética desde INICIO.
        EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
        origem.setCatalogoId(m.getId()); origem.setDataEfetiva(INICIO); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética explícita para o motivo da prévia histórica");
        origem.setResponsavelUsername("FIXTURE_PESSOAS_VALIDACOES"); em.persist(origem); return m;
    }
    private Map<String,Object> requisicao(Long motivo) { return new LinkedHashMap<>(Map.of("dataFim",DATA.plusDays(1),"motivoDesligamentoId",motivo,"justificativa","Fato documentado","evidencia","SEI-1","autorizacao","Ato 1")); }
}
