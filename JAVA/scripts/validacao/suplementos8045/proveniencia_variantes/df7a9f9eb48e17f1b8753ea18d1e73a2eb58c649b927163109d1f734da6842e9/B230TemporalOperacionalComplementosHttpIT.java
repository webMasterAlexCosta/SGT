package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class B230TemporalOperacionalComplementosHttpIT extends B230Suporte {
    @Test
    void tmp017AnulacaoMudaOMesmoDiaSomenteNoCorteDeConhecimentoDoAto() throws Exception {
        Cenario c = cenario(); NovoContrato n = publicadoConfirmado(c, 5);
        JsonNode vinculo = novoVinculo(c,n.id(),n.composicao1(),n.inicio(),null,1);
        nova(() -> { MotivoDesligamento m = new MotivoDesligamento();
            m.setGestaoLocal(em.getReference(GestaoLocal.class,c.gestaoA.getId()));
            m.setDescricao("B230 causa sintética de anulação"); m.setCategoria(CategoriaTerminoVinculo.ANULACAO_CONTRATO);
            em.persist(m); return null; });
        LocalDate alcance = RelogioSistema.hoje().plusDays(4);
        JsonNode origens = consultar(c.localA,"/api/vinculos-contratuais/"+vinculo.path("id").asLong()+"/eventos","Origem anterior ao ato");
        var req = mapa("dataEfetiva",alcance,"dataAto",RelogioSistema.hoje(),"motivo","Ato sintético B230 de alcance futuro","referenciaSei","SEI-SINTETICO-B230-ANU");
        JsonNode preview = enviar(c.localA,base(n.id())+"/previas/anular",req,200,"Prévia do alcance anulatório");
        req.put("tokenPrevia",preview.path("tokenPrevia").asText()); req.put("confirmarImpacto",true);
        enviar(c.localA,base(n.id())+"/anular",req,200,"Anulação explícita do contrato de teste");
        Instant registrado = nova(() -> em.createQuery("select e.registradoEm from EventoCicloVidaContrato e "
                + "where e.contrato.id=:c and e.tipoEvento=:tipo",Instant.class).setParameter("c",n.id())
                .setParameter("tipo",TipoEventoCicloVidaContrato.ANULACAO).getSingleResult());
        Instant anterior = registrado.minusNanos(1);
        var before = negocio();
        JsonNode kAntes = posicao(c,vinculo.path("pessoaId").asLong(),alcance,anterior);
        JsonNode kApos = posicao(c,vinculo.path("pessoaId").asLong(),alcance,registrado);
        JsonNode dAntes = posicao(c,vinculo.path("pessoaId").asLong(),alcance.minusDays(1),registrado);
        assertThat(kAntes.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(kApos.path("situacaoTemporal").asText()).isEqualTo("ANULADO");
        assertThat(dAntes.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        for (JsonNode p : List.of(kAntes,kApos,dAntes)) assertThat(p.path("vinculoId").asLong()).isEqualTo(vinculo.path("id").asLong());
        JsonNode eventos = consultar(c.localA,"/api/vinculos-contratuais/"+vinculo.path("id").asLong()+"/eventos","Origem preservada depois do ato");
        assertThat(eventos.get(0)).isEqualTo(origens.get(0));
        semMutacao(before,"TMP017 consultas D/K não materializam interpretação nova");
        registrarB230("TMP-017:003",mapa("data",alcance,"conhecidoAntes",anterior,"conhecidoApos",registrado,
                "vinculoId",vinculo.path("id").asLong(),"situacoes",List.of("VIGENTE","ANULADO","VIGENTE")));
    }

    private JsonNode posicao(Cenario c,long pessoa,LocalDate data,Instant conhecimento) throws Exception {
        JsonNode p = dados(executar(c.localA,get("/api/vinculos-contratuais/pessoas/"+pessoa+"/posicao")
                .param("data",data.toString()).param("conhecidoAte",conhecimento.toString()),200,"Mesma pessoa por D e K explícitos"));
        assertThat(p.path("vinculos")).hasSize(1); return p.path("vinculos").get(0);
    }

    @Test
    void vin002PesquisaIgnoraDataDaMudancaDeAlocacaoEDataAdmissaoDivergente() throws Exception {
        Cenario c = cenario(); Destino d = novoDestino(c); long comp = composicaoSintetica(c,d);
        LocalDate mudanca = INICIO.plusDays(50), divergente = INICIO.plusDays(90);
        JsonNode faixa = alterarAlocacao(c,c.vinculoA.getId(),comp,mudanca,null);
        assertThat(faixa.path("dataInicio").asText()).isEqualTo(mudanca.toString());
        assertInicioUnico(c,INICIO,c.vinculoA.getId());
        assertThat(procurarInicio(c,c.pessoaA.getId(),mudanca).path("content")).isEmpty();
        // Declared legacy projection counterexample, confined to this fresh fixture.
        // Only Empregado.dataAdmissao diverges; the canonical link never changes.
        nova(() -> { em.find(Empregado.class,c.empregadoA.getId()).setDataAdmissao(divergente); return null; });
        var before = negocio();
        assertInicioUnico(c,INICIO,c.vinculoA.getId());
        for (LocalDate date : List.of(mudanca,divergente)) {
            JsonNode found = procurarInicio(c,c.pessoaA.getId(),date);
            assertThat(found.path("content")).isEmpty(); assertThat(found.path("totalElements").asLong()).isZero();
        }
        nova(() -> {
            assertThat(em.find(VinculoContratual.class,c.vinculoA.getId()).getDataInicio()).isEqualTo(INICIO);
            assertThat(em.find(Empregado.class,c.empregadoA.getId()).getDataAdmissao()).isEqualTo(divergente); return null;
        });
        semMutacao(before,"VIN002 pesquisa não corrige projeção nem inventa outro vínculo");
        registrarB230("VIN-002:002",mapa("inicioCanonico",INICIO,"mudancaAlocacao",mudanca,
                "dataAdmissaoSinteticaDivergente",divergente,"vinculoId",c.vinculoA.getId()));
    }
    private void assertInicioUnico(Cenario c,LocalDate data,long vinculo) throws Exception {
        JsonNode found = procurarInicio(c,c.pessoaA.getId(),data);
        assertThat(found.path("content")).hasSize(1); assertThat(found.path("totalElements").asLong()).isEqualTo(1);
        assertThat(found.path("content").get(0).path("id").asLong()).isEqualTo(vinculo);
    }

    @Test
    void vin008FimNaturalRecusaOperacaoNoDiaSeguinteMasVinculoContinuaAberto() throws Exception {
        Cenario c = cenario(); LocalDate fim = RelogioSistema.hoje().minusDays(1);
        while (fim.getDayOfWeek().getValue()>5) fim=fim.minusDays(1);
        final LocalDate ultimoDia = fim;
        NovoContrato n = publicadoEm(c,5,fim.minusDays(30),fim,null,null);
        JsonNode vinculo = novoVinculo(c,n.id(),n.composicao1(),fim.minusDays(5),null,2);
        long id = vinculo.path("id").asLong();
        nova(() -> { assertThat(em.find(VinculoContratual.class,id).getDataFim()).isNull(); return null; });
        long frequencia = frequencia(c.operacionalA,id,fim,"FALTA",200).path("id").asLong();
        var before = negocio();
        JsonNode refused = frequencia(c.operacionalA,id,fim.plusDays(1),"FALTA",409);
        assertThat(refused.path("message").asText()).containsIgnoringCase("não está operacional");
        semMutacao(before,"VIN008 D+1 não cria frequência, encerramento técnico ou nova faixa");
        nova(() -> {
            assertThat(em.find(VinculoContratual.class,id).getDataFim()).isNull();
            assertThat(em.find(Frequencia.class,frequencia).getDataReferencia()).isEqualTo(ultimoDia);
            assertThat(em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v and f.dataReferencia=:d",Long.class)
                    .setParameter("v",id).setParameter("d",ultimoDia.plusDays(1)).getSingleResult()).isZero(); return null;
        });
        registrarB230("VIN-008:001",mapa("fimContrato",fim,"controleD",200,"recusaDmais1",409,"vinculoContinuaAberto",true));
    }

    @Test
    void vin013ReativacaoRetomaElegiveisMasNaoEncerradoCanceladoOuVinculoSucedido() throws Exception {
        LocalDate reativacao = segundaNaJanela(), base = reativacao.minusDays(4);
        AtomicReference<LocalDate> hoje = new AtomicReference<>(base);
        try (var clock = Mockito.mockStatic(RelogioSistema.class,Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenAnswer(i->hoje.get());
            Cenario c = cenario(); NovoContrato n = publicadoEm(c,10,base.minusDays(20),base.plusDays(90),null,null);
            JsonNode elegivel = novoVinculo(c,n.id(),n.composicao1(),base.minusDays(5),null,10);
            JsonNode encerrado = novoVinculo(c,n.id(),n.composicao1(),base.minusDays(5),base.minusDays(1),11);
            JsonNode cancelado = novoVinculo(c,n.id(),n.composicao1(),base.plusDays(1),null,12);
            enviar(c.localA,"/api/vinculos-contratuais/"+cancelado.path("id").asLong()+"/cancelar-programacao",justificativa(),200,
                    "Cancelamento explícito antes da data programada");
            JsonNode sucedido = novoVinculo(c,n.id(),n.composicao1(),base.minusDays(5),base.plusDays(1),13);
            JsonNode sucessor = enviar(c.localA,"/api/vinculos-contratuais",mapa("pessoaId",sucedido.path("pessoaId").asLong(),
                    "contratoId",n.id(),"composicaoId",n.composicao1(),"dataInicio",base.plusDays(2)),201,
                    "Sucessor legítimo, sem sobreposição, já existe antes da reativação");
            var canonical = linhaCanonica();
            hoje.set(base.plusDays(1)); ciclo(c,n.id(),"inativar",hoje.get());
            assertThat(linhaCanonica()).isEqualTo(canonical);
            hoje.set(reativacao); ciclo(c,n.id(),"reativar",reativacao);
            assertThat(linhaCanonica()).as("A mesma decisão administrativa não recria nem reabre vínculos/faixas").isEqualTo(canonical);
            for (JsonNode active : List.of(elegivel,sucessor))
                assertThat(frequencia(c.operacionalA,active.path("id").asLong(),reativacao,"FALTA",200).path("id").asLong()).isPositive();
            for (JsonNode invalid : List.of(encerrado,cancelado,sucedido)) {
                var before = negocio(); frequencia(c.operacionalA,invalid.path("id").asLong(),reativacao,"FALTA",409);
                semMutacao(before,"VIN013 vínculo inelegível continua sem operação após reativação");
            }
            nova(() -> {
                assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:c",Long.class)
                        .setParameter("c",n.id()).getSingleResult()).isEqualTo(5);
                assertThat(em.find(VinculoContratual.class,cancelado.path("id").asLong()).getEstadoNegocial().name()).isEqualTo("CANCELADO");
                assertThat(em.find(VinculoContratual.class,encerrado.path("id").asLong()).getDataFim()).isEqualTo(base.minusDays(1)); return null;
            });
            registrarB230("VIN-013:002",mapa("dataReativacao",reativacao,"vinculosPersistidos",5,"controles200",2,"inelegiveis409",3,
                    "relogioControlado","Somente RelogioSistema.hoje para datas administrativas; sem TCP"));
        }
    }

    @Test
    void alo008ReativacaoPreservaFaixaVencidaESubstituidaERetomaSomenteUltimaValida() throws Exception {
        Cenario c = cenario(); Destino d1=novoDestino(c),d2=novoDestino(c);
        long comp1=composicaoSintetica(c,d1),comp2=composicaoSintetica(c,d2);
        LocalDate hoje=segundaNaJanela(), mudanca=hoje.minusDays(5);
        JsonNode primeira=alterarAlocacao(c,c.vinculoA.getId(),comp1,mudanca,null);
        JsonNode atual=alterarAlocacao(c,c.vinculoA.getId(),comp2,mudanca,primeira.path("id").asLong());
        long atualId=atual.path("id").asLong();
        nova(() -> {
            assertThat(em.find(AlocacaoVinculo.class,c.alocacaoA.getId()).getDataFim()).isEqualTo(mudanca.minusDays(1));
            assertThat(em.find(AlocacaoVinculo.class,primeira.path("id").asLong()).getEstadoNegocial().name()).isEqualTo("SUBSTITUIDA");
            assertThat(em.find(AlocacaoVinculo.class,atualId).getEstadoNegocial().name()).isEqualTo("REGISTRADA"); return null;
        });
        UsuarioAplicacao destino=operacional(c,d2), substituido=operacional(c,d1);
        var canonical=linhaCanonica(); ciclo(c,c.contratoA.getId(),"inativar",hoje.minusDays(2));
        ciclo(c,c.contratoA.getId(),"reativar",hoje);
        assertThat(linhaCanonica()).isEqualTo(canonical);
        JsonNode pos=consultar(c.localA,"/api/vinculos-contratuais/"+c.vinculoA.getId()+"/alocacoes/posicao?data="+hoje,"Faixa única retomada");
        assertThat(pos.path("alocacaoId").asLong()).isEqualTo(atualId);
        for (var former : List.of(c.operacionalA,substituido)) {
            var before=negocio(); frequencia(former,c.vinculoA.getId(),hoje,"FALTA",403);
            semMutacao(before,"ALO008 escopo de faixa vencida/substituída não retoma escrita");
        }
        JsonNode created=frequencia(destino,c.vinculoA.getId(),hoje,"FALTA",200);
        nova(() -> { assertThat(em.find(Frequencia.class,created.path("id").asLong()).getAlocacaoVinculo().getId()).isEqualTo(atualId); return null; });
    }

    @Test
    void alo008ComposicaoZeradaPorAditamentoImpedeRetomadaSemGravarReativacao() throws Exception {
        Cenario c=cenario(); Destino target=novoDestino(c); LocalDate hoje=segundaNaJanela();
        NovoContrato n=publicadoEm(c,100,hoje.minusDays(30),hoje.plusDays(80),target,25);
        JsonNode v=novoVinculo(c,n.id(),n.composicao2(),hoje.minusDays(10),null,20);
        var change=aditamento("B230-ZERO-"+n.id(),hoje.minusDays(1));
        change.put("novoEfetivoTotal",75);
        change.put("alteracoesComposicao",List.of(mapa("composicaoId",n.composicao2(),"quantidadeResultante",0)));
        aditamentoConfirmado(c,n.id(),change);
        ciclo(c,n.id(),"inativar",hoje.minusDays(2));
        var req=mapa("dataEfetiva",hoje,"motivo","B230 tentativa de retomar combinação sem capacidade");
        JsonNode preview=enviar(c.localA,base(n.id())+"/previas/reativar",req,200,"Prévia antes da validação da retomada");
        req.put("tokenPrevia",preview.path("tokenPrevia").asText()); req.put("confirmarImpacto",true);
        var before=negocio();
        JsonNode rejected=enviar(c.localA,base(n.id())+"/reativar",req,409,"ALO008 composição efetiva zero recusa retomada");
        assertThat(rejected.path("message").asText()).containsIgnoringCase("zero");
        semMutacao(before,"ALO008 recusa não cria evento nem materializa habilitação ativa");
        nova(() -> { assertThat(em.find(Contrato.class,n.id()).getAtivo()).isFalse();
            assertThat(em.find(VinculoContratual.class,v.path("id").asLong()).getDataFim()).isNull(); return null; });
        ciclo(c,c.contratoA.getId(),"inativar",hoje.minusDays(2));
        ciclo(c,c.contratoA.getId(),"reativar",hoje);
        nova(() -> { assertThat(em.find(Contrato.class,c.contratoA.getId()).getAtivo()).isTrue(); return null; });
    }

    @Test
    void fre016NovoOperacionalNaoHerdeEdicaoDaFonteMesmoDentroDosSeteDias() throws Exception {
        Cenario c=cenario(); LocalDate dia=segundaNaJanela(); Destino target=novoDestino(c);
        long comp=composicaoSintetica(c,target); UsuarioAplicacao destino=operacional(c,target);
        long sourceId=frequencia(c.operacionalA,c.vinculoA.getId(),dia,"FALTA",200).path("id").asLong();
        JsonNode oldEvents=consultar(c.localA,"/api/frequencias/"+sourceId+"/eventos","Fonte operacional antes da mudança");
        alterarAlocacao(c,c.vinculoA.getId(),comp,dia,null);
        nova(() -> {
            var source=em.find(Frequencia.class,sourceId);
            assertThat(source.getAlocacaoVinculo().getId()).isEqualTo(c.alocacaoA.getId());
            assertThat(source.getAutorOriginalId()).isEqualTo(c.operacionalA.getId());
            assertThat(source.getBloqueadaEdicaoOperacional()).isTrue();
            assertThat(source.getEstadoAuditoria().name()).isEqualTo("SUJEITA_REVISAO"); return null;
        });
        JsonNode control=novoVinculo(c,c.contratoA.getId(),comp,dia,null,30);
        JsonNode writable=frequencia(destino,control.path("id").asLong(),dia,"FALTA",200);
        assertThat(writable.path("estadoNegocial").asText()).isEqualTo("ABERTA");
        assertThat(ZonedDateTime.now(RelogioSistema.ZONA)).isBefore(dia.plusDays(7).atStartOfDay(RelogioSistema.ZONA));
        var before=negocio(); JsonNode denied=frequencia(destino,c.vinculoA.getId(),dia,"PRESENCA",403);
        assertThat(denied.path("message").asText()).doesNotContain("FECHADA");
        semMutacao(before,"FRE016 outro Operacional não reassocia nem edita a fonte original");
        JsonNode afterEvents=consultar(c.localA,"/api/frequencias/"+sourceId+"/eventos","Evento original continua íntegro");
        assertThat(afterEvents.get(0)).isEqualTo(oldEvents.get(0));
        registrarB230("FRE-016:001",mapa("dataReferencia",dia,"limiteExclusivo",dia.plusDays(7).atStartOfDay(RelogioSistema.ZONA),
                "observadoEm",ZonedDateTime.now(RelogioSistema.ZONA),"fonteId",sourceId,"controleId",writable.path("id").asLong(),
                "controleDestino",200,"edicaoFonteOriginal",403,"relogioReal",true));
    }
}
