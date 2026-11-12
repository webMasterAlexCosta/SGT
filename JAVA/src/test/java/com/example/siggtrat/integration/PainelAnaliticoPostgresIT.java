package com.example.siggtrat.integration;

import com.example.siggtrat.controlador.PainelAnaliticoControlador;
import com.example.siggtrat.dominio.frequencia.ValidadeOperacionalFrequencia;
import com.example.siggtrat.dto.PainelAnaliticoDTO.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.PainelAnaliticoRepositorio;
import com.example.siggtrat.repositorio.PainelAnaliticoRepositorio.Filtro;
import com.example.siggtrat.repositorio.PainelEfetivoRepositorio;
import com.example.siggtrat.servico.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integração opt-in: JPA e SQL reais, fixture sintética em transação com rollback. */
@Transactional
class PainelAnaliticoPostgresIT extends PostgresIntegrationTestSupport {
    @Autowired EntityManager em;
    @Autowired PainelAnaliticoRepositorio repo;
    @Autowired PainelEfetivoRepositorio efetivo;
    final LocalDate ref=LocalDate.of(2026,11,10);

    @Test void vencimentoUsaVigenciaCanonicaELimiarInclusivoDeTrintaDias() {
        var f=fixture();
        var versao=em.createQuery("select v from VersaoContratoNegocial v where v.contrato.id=:id",VersaoContratoNegocial.class)
                .setParameter("id",f.contrato().getId()).getSingleResult();
        versao.setDataFim(ref.plusDays(30));em.flush();
        var contexto=mock(PainelServico.class);
        when(contexto.contextoAtual()).thenReturn(new PainelServico.PainelCalculoContexto("Gestão Local",f.gestao().getNome(),f.gestao().getSuperintendencia().getId(),f.gestao().getId(),true,true,true,true,true));
        var servico=new PainelAnaliticoServico(repo,efetivo,contexto);
        var d=servico.obter("DIARIO",ref,f.contrato().getId(),null,null,false,0);
        assertEquals(ref.plusDays(30),d.indicadores().contratos().getFirst().vigenciaFim());
        assertNotEquals(f.contrato().getDataFim(),d.indicadores().contratos().getFirst().vigenciaFim());
        assertTrue(d.alertas().stream().anyMatch(a->a.codigo().equals("CONTRATOS_VENCENDO_30_DIAS")));
        versao.setDataFim(ref.plusDays(31));em.flush();
        assertTrue(servico.obter("DIARIO",ref,f.contrato().getId(),null,null,false,0).alertas().stream()
                .noneMatch(a->a.codigo().equals("CONTRATOS_VENCENDO_30_DIAS")));
    }

    @Test void cincoPeriodosHttpAgregamOsRegistrosReaisESemMetricasInventadas() throws Exception {
        var f=fixture();
        for(String dia:List.of("2025-11-10","2026-01-01","2026-07-01","2026-10-01","2026-11-01","2026-11-09","2026-11-10"))
            frequencia(f,LocalDate.parse(dia),dia.equals("2026-11-10")?"FALTA":"PRESENCA");
        em.flush();
        var contexto=mock(PainelServico.class);
        when(contexto.contextoAtual()).thenReturn(new PainelServico.PainelCalculoContexto("Gestão Local",f.gestao().getNome(),f.gestao().getSuperintendencia().getId(),f.gestao().getId(),true,true,true,true,true));
        var mvc=MockMvcBuilders.standaloneSetup(new PainelAnaliticoControlador(new PainelAnaliticoServico(repo,efetivo,contexto))).build();
        String[] tipos={"DIARIO","SEMANAL","MENSAL","TRIMESTRAL","ANUAL"};
        long[] esperados={1,2,3,4,6};
        for(int i=0;i<tipos.length;i++) {
            String texto=mvc.perform(get("/api/painel/analitico").param("referencia",ref.toString()).param("granularidade",tipos[i]))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var json=new ObjectMapper().readTree(texto);
            assertEquals(esperados[i],json.path("resumoPeriodo").path("registros").asLong());
            assertEquals(1,json.path("resumoPeriodo").path("ausencias").asLong());
            assertEquals(tipos[i],json.path("intervalo").path("granularidade").asText());
            var serie=json.path("evolucao");
            assertTrue(serie.isArray() && !serie.isEmpty());
            var ultimo=serie.get(serie.size()-1);
            assertEquals(ref.toString(),ultimo.path("periodo").path("fim").asText());
            assertEquals(esperados[i],ultimo.path("atual").path("registros").asLong());
            LocalDate inicio=LocalDate.parse(json.path("intervalo").path("inicio").asText());
            for(var coluna:json.path("heatmap").path("colunas")) {
                LocalDate minimo=tipos[i].equals("DIARIO")?ref.minusDays(6):inicio;
                assertFalse(LocalDate.parse(coluna.path("inicio").asText()).isBefore(minimo));
                assertFalse(LocalDate.parse(coluna.path("fim").asText()).isAfter(ref));
            }
            var tempestividade=json.path("indicadores").path("tempestividade");
            assertEquals(esperados[i],tempestividade.path("indeterminado").asLong());
            assertEquals(0,tempestividade.path("dentro").asLong());
            long soma=0;for(var unidade:json.path("unidades")) soma+=unidade.path("totais").path("registros").asLong();
            assertEquals(esperados[i],soma);
            assertEquals(100,json.path("metas").get(0).path("meta").asDouble());
        }
        mvc.perform(get("/api/painel/analitico").param("referencia",ref.toString()).param("tipoUnidade","LOTACAO").param("unidadeId",String.valueOf(Long.MAX_VALUE)))
                .andExpect(status().isForbidden());
    }

    @Test void contextoSeteDiasNaoContaminaKpiDiarioNemContaFolgaComoLancamento() {
        var f=fixture();
        frequencia(f,ref.minusDays(1),"PRESENCA");
        frequencia(f,ref,"FOLGA");
        em.flush();
        var contexto=mock(PainelServico.class);
        when(contexto.contextoAtual()).thenReturn(new PainelServico.PainelCalculoContexto("Gestão Local",f.gestao().getNome(),f.gestao().getSuperintendencia().getId(),f.gestao().getId(),true,true,true,true,true));
        var d=new PainelAnaliticoServico(repo,efetivo,contexto).obter("DIARIO",ref,null,null,null,false,0);
        assertEquals(0,d.resumoPeriodo().registros());
        assertEquals(1,d.totalUnidades());
        assertEquals(0,d.unidades().get(0).totais().registros());
        assertEquals(1,d.heatmap().linhas().get(0).celulas().stream().mapToLong(Totais::registros).sum());
    }

    @Test void snapshotsMantemEscopoAposMudancaDeCadastroEInvalidosNaoContam() {
        var f=fixture();
        frequencia(f,ref,"ATRASO");
        Frequencia invalida=frequencia(f,ref.minusDays(1),"FALTA");
        invalida.setValidadeOperacional(ValidadeOperacionalFrequencia.INVALIDA_COBERTURA);
        var destinoFixture=PainelAnaliticoFixture.criar(em,"Destino sintético dashboard");
        var outra=destinoFixture.gestao();
        f.vinculo().setDataFim(ref);f.alocacao().setDataFim(ref);
        var novoVinculo=new VinculoContratual();novoVinculo.setPessoa(f.pessoa());novoVinculo.setContrato(destinoFixture.contrato());novoVinculo.setDataInicio(ref.plusDays(1));em.persist(novoVinculo);
        var novaAlocacao=new AlocacaoVinculo();novaAlocacao.setVinculo(novoVinculo);novaAlocacao.setLotacao(destinoFixture.lotacao());novaAlocacao.setJornada(destinoFixture.jornada());novaAlocacao.setCargo(destinoFixture.cargo());novaAlocacao.setDataInicio(ref.plusDays(1));em.persist(novaAlocacao);
        f.empregado().setGestaoLocal(outra);f.empregado().setLotacao(destinoFixture.lotacao());f.empregado().setJornada(destinoFixture.jornada());f.empregado().setCargo(destinoFixture.cargo());f.empregado().setContratos(new ArrayList<>(List.of(destinoFixture.contrato())));em.flush();
        var origem=new Filtro(f.gestao().getId(),null,null,TipoUnidade.LOTACAO,null);
        var destino=new Filtro(outra.getId(),null,null,TipoUnidade.LOTACAO,null);
        assertEquals(new Totais(1,1,0),repo.totais(ref.minusDays(1),ref,origem));
        assertEquals(Totais.vazio(),repo.totais(ref.minusDays(1),ref,destino));
        var faixas=List.of(new Faixa(ref.minusDays(1),ref.minusDays(1),"Anterior",false),new Faixa(ref,ref,"Atual",false));
        assertEquals(List.of(Totais.vazio(),new Totais(1,1,0)),repo.serie(faixas,origem));
        assertEquals(repo.serie(faixas,origem),repo.heatmap(faixas,List.of(f.lotacao().getId()),origem).get(f.lotacao().getId()));
    }

    @Test void coberturaConsideraEscalaFolgaEPrimeiroLancamentoSemReclassificarEdicao() {
        var f=fixture();
        f.empregado().setDataAdmissao(ref.plusDays(1));
        var politica=new PoliticaTemporalFrequenciaServico(7,72);
        var r=PainelAnaliticoFixture.frequenciaComPrazo(em,f,ref,"FALTA",ref.plusDays(1).atTime(14,0).atZone(politica.zona()).toInstant());
        em.flush();
        var filtro=new Filtro(f.gestao().getId(),null,null,TipoUnidade.LOTACAO,null);
        assertEquals(new Tempestividade(1,0,0,0),repo.tempestividade(ref,ref,filtro));
        var base=efetivo.obterCoberturaAnalitica(ref,ref,f.contrato().getId(),null,f.gestao().getId(),null,TipoUnidade.LOTACAO);
        assertEquals(1,base.stream().mapToLong(PainelEfetivoRepositorio.BaseCobertura::comAdmissao).sum());
        assertEquals(1,base.stream().mapToLong(PainelEfetivoRepositorio.BaseCobertura::esperados).sum());
        assertEquals(1,base.stream().mapToLong(PainelEfetivoRepositorio.BaseCobertura::lancados).sum());
        assertEquals(0,base.stream().mapToLong(PainelEfetivoRepositorio.BaseCobertura::pendentes).sum());
        var sabado=LocalDate.of(2026,11,7);
        assertEquals(0,efetivo.obterCoberturaAnalitica(sabado,sabado,f.contrato().getId(),null,f.gestao().getId(),null,TipoUnidade.LOTACAO).stream().mapToLong(PainelEfetivoRepositorio.BaseCobertura::esperados).sum());
        r.setStatus("PRESENCA");r.setRevisaoSolicitadaStatus("PENDENTE");em.flush();
        assertEquals(new Tempestividade(1,0,0,1),repo.tempestividade(ref,ref,filtro));
    }

    private Frequencia frequencia(PainelAnaliticoFixture.Dados f,LocalDate dia,String status) {
        return PainelAnaliticoFixture.frequencia(em,f,dia,status);
    }
    private PainelAnaliticoFixture.Dados fixture() { return PainelAnaliticoFixture.criar(em,"Origem sintética dashboard"); }
}
