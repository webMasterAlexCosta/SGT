package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class ContratoCatalogoFotografiaHttpIT extends ContratosHttpBase {
    @Test
    void renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, false);
        var dataPublicada=n.inicio().plusDays(1);
        var publicacao=consultar(c.localA,base(n.id())+"/posicao-temporal?data="+dataPublicada,
                "CON008 valores publicados antes das alterações dos mestres").path("versaoContratual").deepCopy();
        var composicao=consultar(c.localA,base(n.id())+"/composicoes?data="+dataPublicada,
                "CON008 configuração e nomes publicados antes das alterações").get(0).deepCopy();
        assertThat(composicao.path("id").asLong()).isEqualTo(n.composicao1());
        assertThat(composicao.path("jornadaSemanal").asText()).contains("17:00");
        assertThat(composicao.path("lotacaoMunicipio").asText()).isEqualTo(c.lotacaoA.getMunicipio());
        var cadastro = mapa("descricao", "Tipo B no documento", "ativo", true, "matriculaVinculoObrigatoria", false,
                "gestaoLocalId", c.gestaoA.getId());
        long tipoId = enviar(c.localA, "/api/tipos-contratacao", cadastro, 201, "CAT-004 tipo de destino explícito").path("id").asLong();
        var req = aditamento("TROCA-TIPO-FOTOGRAFIA", RelogioSistema.hoje()); req.put("novoTipoContratacaoId", tipoId);
        var adi = registrarAditamento(c, n.id(), req);
        assertThat(adi.path("novoTipoContratacaoDescricao").asText()).isEqualTo("Tipo B no documento");
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(),
                "HIS-004 fotografia do tipo no momento do instrumento").path("versaoContratual").path("tipoContratacaoDescricao").asText())
                .isEqualTo("Tipo B no documento");
        Instant corte = Instant.now();
        cadastro.put("descricao", "Tipo C após atualização do catálogo");
        executar(c.localA, put("/api/tipos-contratacao/" + tipoId).contentType(MediaType.APPLICATION_JSON).content(json(cadastro)),
                200, "CAT-004 edição permitida do nome atual do catálogo");
        em.flush(); em.clear();
        var conhecidoAntes = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje()
                + "&conhecidoAte=" + corte, "HIS-004 corte anterior à renomeação preserva a fotografia publicada");
        var conhecidoHoje = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(),
                "CAT-004 fotografia publicada permanece após catálogo ser renomeado");
        var instrumentos = consultar(c.localA, base(n.id()) + "/aditamentos", "HIS-004 instrumento guarda nome presente no ato");
        assertAll(
                () -> assertThat(conhecidoAntes.path("versaoContratual").path("tipoContratacaoDescricao").asText()).isEqualTo("Tipo B no documento"),
                () -> assertThat(conhecidoHoje.path("versaoContratual").path("tipoContratacaoDescricao").asText()).isEqualTo("Tipo B no documento"),
                () -> assertThat(instrumentos.get(0).path("novoTipoContratacaoDescricao").asText()).isEqualTo("Tipo B no documento")
        );
        conferirPublicacaoB10(c,n,dataPublicada,publicacao,composicao,"tipo de destino renomeado");
        // Cada mestre muda separadamente; releitura positiva confirma a mudança
        // atual e a mesma verificação datada enumera TODOS os campos publicados.
        var tipoOriginal=(com.fasterxml.jackson.databind.node.ObjectNode)consultar(c.localA,"/api/tipos-contratacao/"+c.tipoA.getId(),
                "CON008 tipo original atual antes da alteração").deepCopy();
        tipoOriginal.put("descricao","Tipo mestre posterior B10");tipoOriginal.put("matriculaVinculoObrigatoria",true);
        executar(c.localA,put("/api/tipos-contratacao/"+c.tipoA.getId()).contentType(MediaType.APPLICATION_JSON).content(json(tipoOriginal)),200,
                "CON008 alteração atual do tipo original não retifica a publicação");
        assertThat(consultar(c.localA,"/api/tipos-contratacao/"+c.tipoA.getId(),"CON008 tipo mestre de fato mudou")
                .path("matriculaVinculoObrigatoria").asBoolean()).isTrue();
        conferirPublicacaoB10(c,n,dataPublicada,publicacao,composicao,"tipo original/política atuais alterados");
        var lotacao=(com.fasterxml.jackson.databind.node.ObjectNode)consultar(c.localA,"/api/lotacoes/"+c.lotacaoA.getId(),
                "CON008 DTO completo da lotação atual").deepCopy();
        lotacao.put("nome","Lotação mestre posterior B10");lotacao.put("municipio","MUNICIPIO POSTERIOR B10");
        executar(c.localA,put("/api/lotacoes/"+c.lotacaoA.getId()).contentType(MediaType.APPLICATION_JSON).content(json(lotacao)),200,
                "CON008 nome e município atuais são dados do mestre");
        assertThat(consultar(c.localA,"/api/lotacoes/"+c.lotacaoA.getId(),"CON008 município atual de fato mudou")
                .path("municipio").asText()).isEqualTo("MUNICIPIO POSTERIOR B10");
        conferirPublicacaoB10(c,n,dataPublicada,publicacao,composicao,"lotação/município atuais alterados");
        var jornada=(com.fasterxml.jackson.databind.node.ObjectNode)consultar(c.localA,"/api/jornadas/"+c.jornadaA.getId(),
                "CON008 DTO completo da jornada atual").deepCopy();
        jornada.put("descricao","Jornada mestre posterior B10");
        for(var dia:jornada.path("dias")) if(dia.path("diaTrabalho").asBoolean())
            ((com.fasterxml.jackson.databind.node.ObjectNode)dia).put("saida2","15:00");
        executar(c.localA,put("/api/jornadas/"+c.jornadaA.getId()).contentType(MediaType.APPLICATION_JSON).content(json(jornada)),200,
                "CON008 grade atual de seis horas não substitui a grade publicada");
        var diasAtuais=consultar(c.localA,"/api/jornadas/"+c.jornadaA.getId(),"CON008 grade atual de fato mudou").path("dias");
        assertThat(diasAtuais).hasSize(7);
        for(var dia:diasAtuais) if(dia.path("diaTrabalho").asBoolean()) assertThat(dia.path("saida2").asText()).isEqualTo("15:00");
        conferirPublicacaoB10(c,n,dataPublicada,publicacao,composicao,"jornada/grade atuais alteradas");
        var cargo=(com.fasterxml.jackson.databind.node.ObjectNode)consultar(c.localA,"/api/cargos/"+c.cargoA.getId(),
                "CON008 DTO completo do cargo atual").deepCopy();
        cargo.put("nome","Cargo mestre posterior B10");
        executar(c.localA,put("/api/cargos/"+c.cargoA.getId()).contentType(MediaType.APPLICATION_JSON).content(json(cargo)),200,
                "CON008 nome atual do cargo não reescreve a fotografia");
        assertThat(consultar(c.localA,"/api/cargos/"+c.cargoA.getId(),"CON008 cargo atual de fato mudou")
                .path("nome").asText()).isEqualTo("Cargo mestre posterior B10");
        conferirPublicacaoB10(c,n,dataPublicada,publicacao,composicao,"cargo atual alterado");

        if(publicacao.path("fornecedorId").isNumber()) {
            long fornecedorId=publicacao.path("fornecedorId").asLong();
            var fornecedor=em.find(com.example.siggtrat.entidade.Fornecedor.class,fornecedorId);
            assertThat(fornecedor).isNotNull();
            // Mudança JPA sintética do mestre compartilhado, confinada à
            // transação de teste com rollback; não concede permissão de API.
            String razaoAtual="Fornecedor mestre posterior B10 "+n.id();
            assertThat(fornecedor.getRazaoSocial()).isNotEqualTo(razaoAtual);
            fornecedor.setRazaoSocial(razaoAtual);em.flush();em.clear();
            assertThat(em.find(com.example.siggtrat.entidade.Fornecedor.class,fornecedorId).getRazaoSocial())
                    .isEqualTo(razaoAtual);
            conferirPublicacaoB10(c,n,dataPublicada,publicacao,composicao,"razão social atual do fornecedor alterada");
        } else assertThat(publicacao.path("fornecedorId").isNull()).as("Publicação sem mestre de fornecedor usa empresa fotografada").isTrue();

        var criado=enviar(c.localA,"/api/vinculos-contratuais",mapa("pessoa",mapa("nome","Pessoa retroativa fotografada B10 "+n.id(),
                "cpf",cpf(Math.toIntExact(758000000L+n.id()))),"contratoId",n.id(),"composicaoId",n.composicao1(),"dataInicio",dataPublicada),201,
                "CON008 nova alocação retroativa usa configuração publicada apesar dos mestres atuais distintos");
        long vinculoId=criado.path("id").asLong();
        var alocacoes=consultar(c.localA,"/api/vinculos-contratuais/"+vinculoId+"/alocacoes","CON008 alocação retroativa explícita");
        assertThat(alocacoes).hasSize(1);
        em.flush();em.clear();
        var faixa=em.find(com.example.siggtrat.entidade.AlocacaoVinculo.class,alocacoes.get(0).path("id").asLong());
        assertThat(faixa.getVinculo().getId()).isEqualTo(vinculoId);
        assertThat(faixa.getVinculo().getPessoa().getId()).isEqualTo(criado.path("pessoaId").asLong());
        assertThat(faixa.getVinculo().getContrato().getId()).isEqualTo(n.id());
        assertThat(faixa.getComposicao().getId()).isEqualTo(n.composicao1());
        assertThat(faixa.getLotacaoNomeFotografia()).isEqualTo(composicao.path("lotacaoNome").asText());
        assertThat(faixa.getJornadaDescricaoFotografia()).isEqualTo(composicao.path("jornadaDescricao").asText());
        assertThat(faixa.getCargoNomeFotografia()).isEqualTo(composicao.path("cargoNome").asText());
        assertThat(faixa.getLotacaoMunicipioFotografia()).isEqualTo(composicao.path("lotacaoMunicipio").asText());
        assertThat(faixa.getJornadaSemanalFotografia()).isEqualTo(composicao.path("jornadaSemanal").asText());
        conferirPublicacaoB10(c,n,dataPublicada,publicacao,composicao,"nova alocação retroativa preserva as fontes");
    }

    private void conferirPublicacaoB10(Cenario c,NovoContrato n,java.time.LocalDate data,
            com.fasterxml.jackson.databind.JsonNode publicada,com.fasterxml.jackson.databind.JsonNode composicao,String etapa) throws Exception {
        em.flush();em.clear(); // transação ambiente; recarga JPA não significa commit
        var atual=consultar(c.localA,base(n.id())+"/posicao-temporal?data="+data,
                "CON008 reconstrução integral após "+etapa).path("versaoContratual");
        for(String campo:java.util.List.of("id","numeroVersao","eventoOrigemId","dataEfetiva","registradoEm","numeroContrato","anoContrato",
                "descricao","tipoContratacaoId","tipoContratacaoDescricao","fornecedorId","cnpjEmpresa","razaoSocialEmpresa","nomeFantasiaEmpresa",
                "dataInicio","dataFim","efetivoPrevisto","gestaoLocalId","gestaoLocalNome","matriculaVinculoObrigatoria",
                "matriculaTipoContratacaoObrigatoria","matriculaVinculoObrigatoriaEfetiva","diaSemanaReduzido","horasDiaReduzido",
                "habilitacaoAdministrativa","tipoContratacaoComprovado")) {
            assertThat(publicada.has(campo)).as("Campo publicado conhecido ou null explícito: "+campo).isTrue();
            assertThat(atual.path(campo)).as("CON008 "+campo+" após "+etapa).isEqualTo(publicada.path(campo));
        }
        var comp=consultar(c.localA,base(n.id())+"/composicoes?data="+data,"CON008 composição datada após "+etapa).get(0);
        for(String campo:java.util.List.of("id","contratoId","lotacaoId","lotacaoNome","jornadaId","jornadaDescricao","cargoId","cargoNome",
                "efetivoPrevisto","dataInicioVigencia","dataFimVigencia","permiteNovaAlocacao","lotacaoMunicipio","jornadaSemanal")) {
            assertThat(composicao.has(campo)).as("Campo de composição publicado: "+campo).isTrue();
            assertThat(comp.path(campo)).as("CON008 composição "+campo+" após "+etapa).isEqualTo(composicao.path(campo));
        }
        var fonte=em.find(com.example.siggtrat.entidade.ComposicaoContratual.class,n.composicao1());
        assertThat(fonte.getLotacaoMunicipioFotografia()).isEqualTo(composicao.path("lotacaoMunicipio").asText());
        assertThat(fonte.getJornadaSemanalFotografia()).isEqualTo(composicao.path("jornadaSemanal").asText());
    }
}
