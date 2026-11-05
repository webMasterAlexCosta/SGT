package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/** Fontes são documentos sintéticos explícitos; nenhum dado mestre fornece a lacuna. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvaHistoricaContratoHttpIT extends ContratosHttpBase {
    @Autowired MotorTemporalContratoServico motor;
    @Autowired ComposicaoContratualTemporalServico composicaoTemporal;

    @Test
    void fontePosteriorCompletaPublicacaoSemReescreverFotoOriginalNemConhecimentoAnterior() throws Exception {
        var c=criarCenarioPersistido();
        var n=novo(c);
        var req=fonte(c,n);
        long original=((Number)req.get("versaoAnteriorId")).longValue();
        emTransacao(()->{
            em.createQuery("update VersaoContratoNegocial v set v.descricao=null, v.tipoContratacaoDescricao=null where v.id=:id")
                    .setParameter("id",original).executeUpdate();
            return null;
        });
        conceder(c,n.id(),"INCORPORACAO_PROVA_HISTORICA");
        Instant antes=Instant.now();
        assertThat(consultar(c.localA,base(n.id())+"/posicao-temporal?data="+n.inicio(),"origem parcial não usa mestre")
                .path("versaoContratual").path("descricao").isNull()).isTrue();
        var previa=enviar(c.localA,rota(n)+"/previa",req,200,"prévia da fonte documental tardia");
        req.put("tokenPrevia",previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto",true);
        var prova=enviar(c.localA,rota(n),req,201,"incorporação imutável e autorizada da fonte tardia");
        assertThat(prova.path("id").asLong()).isPositive();
        assertThat(Instant.parse(prova.path("registradoEm").asText())).isAfterOrEqualTo(antes);
        assertThat(prova.path("hashConteudo").asText()).matches("[0-9a-f]{64}");
        assertThat(consultar(c.localA,base(n.id())+"/posicao-temporal?data="+n.inicio(),"verdade atual enriquecida pela fonte")
                .path("versaoContratual").path("descricao").asText()).isEqualTo("Contrato de auditoria HTTP");
        var antigo=consultar(c.localA,base(n.id())+"/posicao-temporal?data="+n.inicio()+"&conhecidoAte="+antes,
                "fonte não aparece antes do conhecimento real");
        assertThat(antigo.path("versaoContratual").path("descricao").isNull()).isTrue();
        emTransacao(()->{
            var originalPersistido=em.find(VersaoContratoNegocial.class,original);
            assertThat(originalPersistido.getDescricao()).isNull();
            assertThat(originalPersistido.getTipoContratacaoDescricao()).isNull();
            assertThat(em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c",Long.class)
                    .setParameter("c",n.id()).getSingleResult()).isEqualTo(1L);
            return null;
        });
        var repetida=enviar(c.localA,rota(n),req,201,"reenvio retorna a mesma prova sem novo evento");
        assertThat(repetida.path("id")).isEqualTo(prova.path("id"));
    }

    @Test
    void semFonteSemPermissaoOuForaDoEscopoNaoIncorporaNada() throws Exception {
        var c=criarCenarioPersistido();var n=novo(c);var req=fonte(c,n);
        enviar(c.localA,rota(n),req,403,"perfil local não substitui autorização específica");
        conceder(c,n.id(),"INCORPORACAO_PROVA_HISTORICA");
        req.put("referenciaSei",null);
        enviar(c.localA,rota(n)+"/previa",req,400,"sem fonte não existe prova");
        req.put("referenciaSei","SEI-FONTE-SINTETICA-ORIGINAL");
        enviar(c.localB,rota(n)+"/previa",req,404,"outra Gestão não enumera fonte nem contrato");
        enviar(c.estadualA,rota(n)+"/previa",req,403,"consulta estadual não escreve negócio");
        assertThat(emTransacao(()->em.createQuery("select count(p) from ProvaHistoricaContrato p where p.contrato.id=:c",Long.class)
                .setParameter("c",n.id()).getSingleResult())).isZero();
    }

    @Test
    void documentoTardioNaoSubstituiValorJaComprovadoNemAceitaPreviaObsoleta() throws Exception {
        var c=criarCenarioPersistido();var n=novo(c);var req=fonte(c,n);
        conceder(c,n.id(),"INCORPORACAO_PROVA_HISTORICA");
        @SuppressWarnings("unchecked") var foto=(Map<String,Object>)req.get("fotografia");
        foto.put("numeroContrato","OUTRO-NUMERO-INVENTADO");
        enviar(c.localA,rota(n)+"/previa",req,409,"prova complementar não é correção material silenciosa");
        req=fonte(c,n);
        var previa=enviar(c.localA,rota(n)+"/previa",req,200,"prévia da prova conhecida");
        req.put("tokenPrevia",previa.path("tokenPrevia").asText());req.put("confirmarImpacto",true);
        emTransacao(()->{var ct=em.find(Contrato.class,n.id());ct.setDescricao(ct.getDescricao()+" [alteração concorrente sintética]");em.flush();return null;});
        enviar(c.localA,rota(n),req,409,"versão alterada depois da prévia impede confirmação");
    }

    @Test
    void fonteTardiaRestauraConfiguracaoSemUsarCatalogosAtuaisEReaplicaAditamento() throws Exception {
        var c=criarCenarioPersistido();var n=novo(c);var req=fonte(c,n);
        var adi=aditamento("POSTERIOR-INDEPENDENTE",n.inicio().plusDays(5));
        adi.put("novaDescricao","Descrição posterior comprovada");
        emTransacao(()->{try{return registrarAditamento(c,n.id(),adi);}catch(Exception ex){throw new IllegalStateException(ex);}});
        req.put("versaoContratoEsperada",emTransacao(()->em.find(Contrato.class,n.id()).getVersion()));
        emTransacao(()->{
            em.createQuery("update VersaoContratoNegocial v set v.descricao=null where v.id=:id")
                    .setParameter("id",((Number)req.get("versaoAnteriorId")).longValue()).executeUpdate();
            em.createQuery("update ComposicaoContratual c set c.jornadaSemanalFotografia=null, c.lotacaoNomeFotografia='' where c.id=:id")
                    .setParameter("id",n.composicao1()).executeUpdate();
            return null;
        });
        conceder(c,n.id(),"INCORPORACAO_PROVA_HISTORICA");
        var previa=enviar(c.localA,rota(n)+"/previa",req,200,"grade e rótulo são fornecidos pelo documento");
        req.put("tokenPrevia",previa.path("tokenPrevia").asText());req.put("confirmarImpacto",true);
        enviar(c.localA,rota(n),req,201,"conhecimento tardio conserva ADI posterior");
        assertThat(consultar(c.localA,base(n.id())+"/posicao-temporal?data="+n.inicio().plusDays(10),"replay do delta posterior")
                .path("versaoContratual").path("descricao").asText()).isEqualTo("Descrição posterior comprovada");
        emTransacao(()->{var original=em.find(ComposicaoContratual.class,n.composicao1());
            assertThat(original.getJornadaSemanalFotografia()).isNull();assertThat(original.getLotacaoNomeFotografia()).isEmpty();return null;});
        emTransacao(()->{
            var projetada=composicaoTemporal.projetarEntidades(em.find(Contrato.class,n.id()),n.inicio(),Instant.now()).get(0);
            assertThat(projetada.jornadaSemanal()).isNotBlank();
            assertThat(projetada.lotacaoNome()).isEqualTo(c.lotacaoA.getNome());return null;
        });
    }

    @Test
    void fonteTardiaDaTransferenciaRestauraTipoEMapaSomenteDepoisDoConhecimento() throws Exception {
        var c=criarCenarioPersistido();var n=novo(c);
        conceder(c,n.id(),"TRANSFERENCIA_GESTAO_LOCAL");
        LocalDate data=RelogioSistema.hoje().minusDays(2);
        var transferencia=mapa("dataEfetiva",data,"dataAto",RelogioSistema.hoje(),
                "gestaoLocalDestinoId",c.gestaoB.getId(),"tipoContratacaoDestinoId",c.tipoB.getId(),
                "motivo","Transferência sintética respaldada por documento original",
                "referenciaSei","SEI-TRANSFERENCIA-SINTETICA","autorizacaoAdministrativa","ATO-HTTP",
                "mapeamentos",List.of(mapa("composicaoOrigemId",n.composicao1(),
                        "lotacaoDestinoId",c.lotacaoB.getId(),"jornadaDestinoId",c.jornadaB.getId(),"cargoDestinoId",c.cargoB.getId())));
        enviar(c.localA,base(n.id())+"/transferir-gestao",transferencia,200,"transferência original documentada");
        var req=emTransacao(()->{
            var t=em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:c",TransferenciaGestaoContrato.class)
                    .setParameter("c",n.id()).getSingleResult();
            var v=em.createQuery("select v from VersaoContratoNegocial v where v.eventoOrigem.id=:e",VersaoContratoNegocial.class)
                    .setParameter("e",t.getEvento().getId()).getSingleResult();
            @SuppressWarnings("unchecked") Map<String,Object> foto=mapper.convertValue(motor.paraVersaoDTO(v),LinkedHashMap.class);
            for(String campo:List.of("id","numeroVersao","eventoOrigemId","registradoEm","tipoContratacaoComprovado"))foto.remove(campo);
            var m=t.getMapeamentos().get(0);
            var item=mapa("mapeamentoId",m.getId(),"lotacaoNome",m.getLotacaoDestinoNomeFotografia(),
                    "jornadaDescricao",m.getJornadaDestinoDescricaoFotografia(),"cargoNome",m.getCargoDestinoNomeFotografia(),
                    "lotacaoMunicipio",m.getLotacaoDestinoMunicipioFotografia(),"municipioComprovado",true,
                    "jornadaSemanal",m.getJornadaDestinoSemanalFotografia(),"jornadaComprovada",true);
            var fonte=mapa("eventoOrigemId",t.getEvento().getId(),"versaoAnteriorId",v.getId(),
                    "dataDocumento",data,"referenciaSei","SEI-FONTE-TRANSFERENCIA-ORIGINAL",
                    "motivo","Incorporação posterior da fonte original da transferência",
                    "autorizacaoAdministrativa","ATO-CONFERENCIA-SINTETICA", "versaoContratoEsperada",t.getContrato().getVersion(),
                    "chaveIdempotencia",UUID.randomUUID().toString(),"configuracaoCompletaComprovada",true,"fotografia",foto,"composicoes",List.of(item));
            em.createQuery("update TransferenciaGestaoContrato t set t.tipoContratacaoDestino=null, t.tipoDestinoDescricaoFotografia=null where t.id=:id")
                    .setParameter("id",t.getId()).executeUpdate();
            em.createQuery("update MapeamentoTransferenciaGestao m set m.jornadaDestinoSemanalFotografia=null, m.cargoDestinoNomeFotografia='' where m.id=:id")
                    .setParameter("id",m.getId()).executeUpdate();
            return fonte;
        });
        enviar(c.admin,"/api/permissoes-excepcionais",mapa("usuarioBeneficiarioId",c.localB.getId(),"contratoId",n.id(),
                "tipo","INCORPORACAO_PROVA_HISTORICA","finalidade","Incorporação de fonte sintética legítima",
                "justificativa","Documento original recuperado no cenário de teste","autorizacaoAdministrativa","ATO-SINTETICO",
                "validoAte",Instant.now().plusSeconds(7200)),201,"concessão específica ao responsável atual");
        Instant antes=Instant.now();
        assertThat(consultar(c.localB,base(n.id())+"/posicao-temporal?data="+data,"destino sem tipo comprovado")
                .path("versaoContratual").path("tipoContratacaoId").isNull()).isTrue();
        var previa=enviar(c.localB,rota(n)+"/previa",req,200,"prévia da prova tipada da transferência");
        req.put("tokenPrevia",previa.path("tokenPrevia").asText());req.put("confirmarImpacto",true);
        enviar(c.localB,rota(n),req,201,"fonte tardia completa o contexto do destino");
        var atual=consultar(c.localB,base(n.id())+"/posicao-temporal?data="+data,"destino agora comprovado");
        assertThat(atual.path("versaoContratual").path("tipoContratacaoId").asLong()).isEqualTo(c.tipoB.getId());
        assertThat(atual.path("versaoContratual").path("tipoContratacaoComprovado").asBoolean()).isTrue();
        emTransacao(()->{
            var projetada=composicaoTemporal.projetarEntidades(em.find(Contrato.class,n.id()),data,Instant.now()).get(0);
            assertThat(projetada.cargoNome()).isEqualTo(c.cargoB.getNome());
            assertThat(projetada.jornadaSemanal()).isNotBlank();return null;
        });
        assertThat(consultar(c.localB,base(n.id())+"/posicao-temporal?data="+data+"&conhecidoAte="+antes,"corte anterior não conhece prova")
                .path("versaoContratual").path("tipoContratacaoId").isNull()).isTrue();
        emTransacao(()->{
            var t=em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:c",TransferenciaGestaoContrato.class)
                    .setParameter("c",n.id()).getSingleResult();
            assertThat(t.getTipoContratacaoDestino()).isNull();
            assertThat(t.getMapeamentos().get(0).getJornadaDestinoSemanalFotografia()).isNull();
            assertThat(t.getMapeamentos().get(0).getCargoDestinoNomeFotografia()).isEmpty();
            return null;
        });
    }

    @Test
    void origemSemQualquerVersaoPodeSerIncorporadaSomenteComDesignacaoAdministrativaExplicita() throws Exception {
        var c=criarCenarioPersistido();var n=novo(c);var req=fonte(c,n);
        emTransacao(()->{em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id=:c")
                .setParameter("c",n.id()).executeUpdate();return null;});
        req.put("versaoAnteriorId",null);
        enviar(c.admin,"/api/permissoes-excepcionais",mapa("usuarioBeneficiarioId",c.localA.getId(),"contratoId",n.id(),
                "gestaoLocalId",c.gestaoA.getId(),"tipo","INCORPORACAO_PROVA_HISTORICA",
                "finalidade","Conferir documento original de contexto sem fotografia",
                "justificativa","Ausência integral de versões simulada em banco descartável",
                "autorizacaoAdministrativa","DESIGNACAO-EXPRESSA-SINTETICA","validoAte",Instant.now().plusSeconds(7200)),
                201,"designação explícita não infere responsabilidade histórica do cadastro atual");
        Instant antes=Instant.now();
        var previa=enviar(c.localA,rota(n)+"/previa",req,200,"origem canônica existe e fonte integral foi recuperada");
        req.put("tokenPrevia",previa.path("tokenPrevia").asText());req.put("confirmarImpacto",true);
        enviar(c.localA,rota(n),req,201,"nova prova não inventa data de conhecimento anterior");
        assertThat(consultar(c.localA,base(n.id())+"/posicao-temporal?data="+n.inicio(),"publicação com fonte tardia completa")
                .path("versaoContratual").path("numeroContrato").isTextual()).isTrue();
        assertThat(consultar(c.nacional,base(n.id())+"/posicao-temporal?data="+n.inicio()+"&conhecidoAte="+antes,
                "ausência histórica anterior permanece explícita").path("versaoContratual").isNull()).isTrue();
    }

    @Test
    void documentoOriginalRecuperadoPodeReconstituirPublicacaoAusenteSemRetroagirConhecimento() throws Exception {
        var c=criarCenarioPersistido();var n=novo(c);var req=fonte(c,n);
        emTransacao(()->{
            em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id=:c").setParameter("c",n.id()).executeUpdate();
            em.createQuery("delete from EventoCicloVidaContrato e where e.contrato.id=:c").setParameter("c",n.id()).executeUpdate();
            return null;
        });
        req.put("versaoAnteriorId",null);req.put("eventoOrigemId",null);req.put("recuperarPublicacaoOriginal",true);
        enviar(c.admin,"/api/permissoes-excepcionais",mapa("usuarioBeneficiarioId",c.localA.getId(),"contratoId",n.id(),
                "gestaoLocalId",c.gestaoA.getId(),"tipo","INCORPORACAO_PROVA_HISTORICA",
                "finalidade","Incorporar documento original sem evento preservado",
                "justificativa","Perda de evento e fotografia simulada em banco descartável",
                "autorizacaoAdministrativa","DESIGNACAO-EXPRESSA-SINTETICA","validoAte",Instant.now().plusSeconds(7200)),
                201,"responsabilidade da conferência é designada expressamente");
        Instant antes=Instant.now();
        var previa=enviar(c.localA,rota(n)+"/previa",req,200,"origem recuperada por documento completo explícito");
        req.put("tokenPrevia",previa.path("tokenPrevia").asText());req.put("confirmarImpacto",true);
        enviar(c.localA,rota(n),req,201,"ato documental novo registra a data real do conhecimento");
        emTransacao(()->{
            var eventos=em.createQuery("select e from EventoCicloVidaContrato e where e.contrato.id=:c",EventoCicloVidaContrato.class)
                    .setParameter("c",n.id()).getResultList();
            assertThat(eventos).hasSize(1);
            assertThat(eventos.get(0).getTipoEvento()).isEqualTo(TipoEventoCicloVidaContrato.PUBLICACAO);
            assertThat(eventos.get(0).getRegistradoEm()).isAfterOrEqualTo(antes);
            assertThat(eventos.get(0).getDataEfetiva()).isEqualTo(n.inicio());
            return null;
        });
        assertThat(consultar(c.nacional,base(n.id())+"/posicao-temporal?data="+n.inicio()+"&conhecidoAte="+antes,
                "prova tardia não cria fotografia no conhecimento anterior").path("versaoContratual").isNull()).isTrue();
        var repetida=enviar(c.localA,rota(n),req,201,"reenvio não cria segunda publicação");
        assertThat(repetida.path("id").asLong()).isPositive();
        assertThat(emTransacao(()->em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c",Long.class)
                .setParameter("c",n.id()).getSingleResult())).isEqualTo(1L);
    }

    @Test
    void confirmacoesConcorrentesDaMesmaFonteEsperamRaizRealERetornamUmaUnicaProva() throws Exception {
        var c=criarCenarioPersistido();var n=novo(c);var req=fonte(c,n);
        conceder(c,n.id(),"INCORPORACAO_PROVA_HISTORICA");
        var previa=enviar(c.localA,rota(n)+"/previa",req,200,"mesma intenção aprovada para reenvio concorrente");
        req.put("tokenPrevia",previa.path("tokenPrevia").asText());req.put("confirmarImpacto",true);
        var pedido1=autenticar(post(rota(n)).contentType(APPLICATION_JSON).content(json(req)),c.localA);
        var pedido2=autenticar(post(rota(n)).contentType(APPLICATION_JSON).content(json(req)),c.localA);
        var pool=Executors.newFixedThreadPool(3);
        var liberar=new CountDownLatch(1);var conexaoBloqueadora=new CompletableFuture<Long>();
        try {
            var bloqueador=pool.submit(()->emTransacao(()->{
                em.find(Contrato.class,n.id(),LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number)em.createNativeQuery("select connection_id()").getSingleResult()).longValue());
                try {if(!liberar.await(20,TimeUnit.SECONDS))throw new AssertionError("Fixture excedeu espera de lock");}
                catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                return null;
            }));
            long conexao=conexaoBloqueadora.get(10,TimeUnit.SECONDS);
            var primeiro=pool.submit(()->mvc.perform(pedido1).andReturn());
            var segundo=pool.submit(()->mvc.perform(pedido2).andReturn());
            long limite=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);boolean esperaram=false;
            while(System.nanoTime()<limite){
                long esperas=emTransacao(()->((Number)em.createNativeQuery("select count(*) from performance_schema.data_lock_waits w "
                        +"join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID where t.PROCESSLIST_ID=:c")
                        .setParameter("c",conexao).getSingleResult()).longValue());
                if(esperas>=2){esperaram=true;break;}
                if(primeiro.isDone()||segundo.isDone())break;
                Thread.sleep(25);
            }
            assertThat(esperaram).as("duas confirmações aguardaram a mesma raiz no MySQL real").isTrue();
            liberar.countDown();bloqueador.get(10,TimeUnit.SECONDS);
            var a=primeiro.get(15,TimeUnit.SECONDS);var b=segundo.get(15,TimeUnit.SECONDS);
            assertThat(a.getResponse().getStatus()).isEqualTo(201);assertThat(b.getResponse().getStatus()).isEqualTo(201);
            assertThat(dados(a).path("id")).isEqualTo(dados(b).path("id"));
        } finally {liberar.countDown();pool.shutdownNow();}
        assertThat(emTransacao(()->em.createQuery("select count(p) from ProvaHistoricaContrato p where p.contrato.id=:c",Long.class)
                .setParameter("c",n.id()).getSingleResult())).isEqualTo(1L);
    }


    @Test
    void fonteParcialNaoCertificaCamposQueDocumentoNaoComprova() throws Exception {
        var c=criarCenarioPersistido();var n=novo(c);var req=fonte(c,n);
        long origem=((Number)req.get("versaoAnteriorId")).longValue();
        emTransacao(()->{em.createQuery("update VersaoContratoNegocial v set v.descricao=null,v.tipoContratacaoDescricao=null where v.id=:v")
                .setParameter("v",origem).executeUpdate();return null;});
        conceder(c,n.id(),"INCORPORACAO_PROVA_HISTORICA");
        req.put("configuracaoCompletaComprovada",false);req.put("camposComprovados",List.of("descricao"));req.put("composicoes",List.of());
        var previa=enviar(c.localA,rota(n)+"/previa",req,200,"fonte parcial declara somente a descrição");
        req.put("tokenPrevia",previa.path("tokenPrevia").asText());req.put("confirmarImpacto",true);
        var prova=enviar(c.localA,rota(n),req,201,"fonte parcial não elimina outras lacunas");
        var atual=consultar(c.localA,base(n.id())+"/posicao-temporal?data="+n.inicio(),"somente campo comprovado enriquecido");
        assertThat(atual.path("versaoContratual").path("descricao").asText()).isEqualTo("Contrato de auditoria HTTP");
        assertThat(atual.path("versaoContratual").path("tipoContratacaoDescricao").isNull()).isTrue();
        assertThat(prova.path("configuracaoCompletaComprovada").asBoolean()).isFalse();
        var lista=consultar(c.nacional,rota(n),"metadados da cadeia não expõem fonte restrita nem justificativas");
        assertThat(lista.size()).isEqualTo(1);assertThat(lista.get(0).has("referenciaSei")).isFalse();
        assertThat(lista.get(0).has("motivo")).isFalse();
        executar(c.localB,get(rota(n)),404,"provas não são enumeráveis por outra Gestão");
    }

    @Test
    void retificacaoDaProvaExigeAntecessorEConservaTodosOsCortesDeConhecimento() throws Exception {
        var c=criarCenarioPersistido();var n=novo(c);var req=fonte(c,n);
        long origem=((Number)req.get("versaoAnteriorId")).longValue();
        emTransacao(()->{em.createQuery("update VersaoContratoNegocial v set v.descricao=null where v.id=:v")
                .setParameter("v",origem).executeUpdate();return null;});
        conceder(c,n.id(),"INCORPORACAO_PROVA_HISTORICA");
        req.put("configuracaoCompletaComprovada",false);req.put("camposComprovados",List.of("descricao"));req.put("composicoes",List.of());
        var previa=enviar(c.localA,rota(n)+"/previa",req,200,"primeira versão da fonte");
        req.put("tokenPrevia",previa.path("tokenPrevia").asText());req.put("confirmarImpacto",true);
        var primeira=enviar(c.localA,rota(n),req,201,"incorpora primeira prova");
        Instant conhecimentoPrimeira=Instant.now();
        req.put("chaveIdempotencia",UUID.randomUUID().toString());
        req.put("versaoContratoEsperada",emTransacao(()->em.find(Contrato.class,n.id()).getVersion()));
        req.put("versaoAnteriorId",primeira.path("versaoGeradaId").asLong());
        @SuppressWarnings("unchecked") var foto=(Map<String,Object>)req.get("fotografia");
        foto.put("descricao","Descrição retificada por documento adicional explícito");
        enviar(c.localA,rota(n)+"/previa",req,409,"nova verdade não vence por ID ou data de registro");
        req.put("provaAnteriorId",primeira.path("id").asLong());
        req.put("referenciaSei","SEI-RETIFICACAO-EXPLICITA-DA-FONTE");
        previa=enviar(c.localA,rota(n)+"/previa",req,200,"retificação declara o antecessor da prova");
        req.put("tokenPrevia",previa.path("tokenPrevia").asText());
        var segunda=enviar(c.localA,rota(n),req,201,"nova revisão imutável");
        assertThat(segunda.path("numeroRevisao").asInt()).isEqualTo(2);
        assertThat(consultar(c.localA,base(n.id())+"/posicao-temporal?data="+n.inicio(),"verdade documental vigente")
                .path("versaoContratual").path("descricao").asText()).isEqualTo(foto.get("descricao"));
        assertThat(consultar(c.localA,base(n.id())+"/posicao-temporal?data="+n.inicio()+"&conhecidoAte="+conhecimentoPrimeira,"verdade conhecida após primeira prova")
                .path("versaoContratual").path("descricao").asText()).isEqualTo("Contrato de auditoria HTTP");
        emTransacao(()->{assertThat(em.find(VersaoContratoNegocial.class,origem).getDescricao()).isNull();
            assertThat(em.find(VersaoContratoNegocial.class,primeira.path("versaoGeradaId").asLong()).getDescricao()).isEqualTo("Contrato de auditoria HTTP");return null;});
        foto.put("descricao","Outra intenção com a mesma chave");
        enviar(c.localA,rota(n),req,409,"chave não admite intenção diferente nem nova revisão implícita");
    }

    @Test
    void gradeMalformadaEOrigemDeOutroContratoSaoRecusadasAtomicamente() throws Exception {
        var c=criarCenarioPersistido();var n=novo(c);var req=fonte(c,n);
        conceder(c,n.id(),"INCORPORACAO_PROVA_HISTORICA");
        @SuppressWarnings("unchecked") var config=(Map<String,Object>)((List<?>)req.get("composicoes")).get(0);
        config.put("jornadaSemanal","MONDAY|talvez|08:00|12:00|13:00|17:00");
        enviar(c.localA,rota(n)+"/previa",req,400,"booleano e semana não podem ser inferidos");
        var outro=novo(c);var fonteOutra=fonte(c,outro);
        req.put("composicoes",fonteOutra.get("composicoes"));
        enviar(c.localA,rota(n)+"/previa",req,404,"origem canônica de outro contrato é recusada");
        assertThat(emTransacao(()->em.createQuery("select count(p) from ProvaHistoricaContrato p where p.contrato.id=:c",Long.class)
                .setParameter("c",n.id()).getSingleResult())).isZero();
    }

    private NovoContrato novo(Cenario c){return emTransacao(()->{try{return publicado(c,10,null,false);}catch(Exception ex){throw new IllegalStateException(ex);}});}
    private String rota(NovoContrato n){return base(n.id())+"/provas-historicas";}
    private Map<String,Object> fonte(Cenario c,NovoContrato n){return emTransacao(()->{
        var versao=motor.fotografiaOriginalConhecida(em.find(Contrato.class,n.id()),Instant.now()).orElseThrow();
        @SuppressWarnings("unchecked") Map<String,Object> foto=mapper.convertValue(motor.paraVersaoDTO(versao),LinkedHashMap.class);
        for(String campo:List.of("id","numeroVersao","eventoOrigemId","registradoEm","tipoContratacaoComprovado"))foto.remove(campo);
        var comp=em.find(ComposicaoContratual.class,n.composicao1());
        var item=mapa("composicaoId",comp.getId(),"lotacaoNome",comp.getLotacaoNomeFotografia(),
                "jornadaDescricao",comp.getJornadaDescricaoFotografia(),"cargoNome",comp.getCargoNomeFotografia(),
                "lotacaoMunicipio",comp.getLotacaoMunicipioFotografia(),"municipioComprovado",true,
                "jornadaSemanal",comp.getJornadaSemanalFotografia(),"jornadaComprovada",true);
        return mapa("eventoOrigemId",versao.getEventoOrigem().getId(),"versaoAnteriorId",versao.getId(),
                "dataDocumento",RelogioSistema.hoje().minusDays(40),"referenciaSei","SEI-FONTE-SINTETICA-ORIGINAL",
                "motivo","Documento original sintético incorporado posteriormente à perda parcial simulada",
                "autorizacaoAdministrativa","ATO-CONFERENCIA-SINTETICA", "versaoContratoEsperada",em.find(Contrato.class,n.id()).getVersion(),
                "chaveIdempotencia",UUID.randomUUID().toString(),"configuracaoCompletaComprovada",true,"fotografia",foto,"composicoes",List.of(item));
    });}
}
