package com.example.siggtrat.integration;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.PainelEfetivoRepositorio;
import com.example.siggtrat.repositorio.PainelRepositorio;
import com.example.siggtrat.servico.PainelServico;
import com.example.siggtrat.controlador.PainelControlador;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Regressões com MySQL real: nenhuma linha de histórico legado é fabricada. */
@Transactional
class PainelEfetivoTemporalIT extends MySqlIntegrationTestSupport {
    @Autowired EntityManager em;
    @Autowired PainelEfetivoRepositorio painel;
    @Autowired PainelRepositorio painelGestao;
    private final LocalDate data = LocalDate.of(2026, 6, 10);

    @Test void contaVinculoCanonicoSemHistoricoLegado() {
        var f = fixture(3); documentarPublicacao(f.contrato, f.gestao); alocar(f); em.flush();
        var ponto = painel.obterSerie(data, data, f.contrato.getId(), null, null, null, f.gestao.getId(), null).get(0);
        assertEquals(3, ponto.efetivoPrevisto());
        assertEquals(1, ponto.efetivoAlocado());
        assertEquals(1, ponto.pendentesLancamento());
        assertEquals(0, ponto.registrosLancados());
    }

    @Test void recorteDimensionadoSomaSomenteComposicaoSelecionadaMesmoSemPessoas() {
        var f = fixture(3);
        Lotacao outra = new Lotacao("Outra lotação"); outra.setGestaoLocal(f.gestao); em.persist(outra);
        compor(f.contrato, outra, f.jornada, f.cargo, 7);
        f.contrato.setEfetivoPrevisto(10);
        documentarPublicacao(f.contrato, f.gestao); em.flush();
        var ponto = painel.obterSerie(data, data, f.contrato.getId(), f.lotacao.getId(), null, null, null, null).get(0);
        assertEquals(3, ponto.efetivoPrevisto(), "O total do contrato não é a previsão da lotação selecionada.");
    }

    @Test void aditamentoAlteraPrevisaoApenasAPartirDaDataEfetiva() {
        var f = fixture(2); documentarPublicacao(f.contrato, f.gestao);
        EventoCicloVidaContrato evento = evento(f.contrato, TipoEventoCicloVidaContrato.ADITAMENTO, data);
        AditamentoContratual a = new AditamentoContratual(); a.setContrato(f.contrato); a.setEvento(evento);
        a.setIdentificacao("ADIT-IT"); a.setDataDocumento(data); a.setDataEfetiva(data); a.setJustificativa("Ampliação de teste");
        AditamentoComposicao item = new AditamentoComposicao(); item.setComposicaoBase(f.composicao);
        item.setLotacao(f.lotacao); item.setJornada(f.jornada); item.setCargo(f.cargo);
        item.setQuantidadeAnterior(2); item.setVariacao(3); item.setQuantidadeResultante(5); a.adicionarAlteracao(item);
        em.persist(a); f.contrato.setEfetivoPrevisto(5); em.flush();
        var serie = painel.obterSerie(data.minusDays(1), data, f.contrato.getId(), null, null, null, null, null);
        assertEquals(List.of(2L, 5L), serie.stream().map(p -> p.efetivoPrevisto()).toList());
    }

    @Test void inativacaoAtualNaoApagaEfetivoAnterior() {
        var f = fixture(3); documentarPublicacao(f.contrato, f.gestao); alocar(f);
        evento(f.contrato, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, data.plusDays(1));
        f.contrato.setAtivo(false); em.flush();
        var ponto = painel.obterSerie(data, data, f.contrato.getId(), null, null, null, null, null).get(0);
        assertEquals(3, ponto.efetivoPrevisto()); assertEquals(1, ponto.efetivoAlocado());
    }

    @Test void quadroDimensionadoIncluiAlocacaoCanonica() {
        var f = fixture(3); documentarPublicacao(f.contrato, f.gestao); alocar(f); em.flush();
        var quadro = painel.obterQuadroDimensionado(data, f.contrato.getId(), null, null, null, f.gestao.getId(), null);
        assertEquals(1, quadro.size());
        assertEquals(f.lotacao.getId(), quadro.get(0).lotacaoId());
        assertEquals(1, quadro.get(0).efetivoAlocado());
        assertEquals(3, quadro.get(0).efetivoPrevisto());
        assertEquals(2, quadro.get(0).deficitContratual());
        assertEquals(0, quadro.get(0).excedenteContratual());
    }

    @Test void filtrosIncluemContratoEComposicaoSemEmpregadosLegados() {
        var f = fixture(3); documentarPublicacao(f.contrato, f.gestao); em.flush();
        assertTrue(painel.listarContratos(f.gestao.getId(), null, null, null, null).stream()
                .anyMatch(o -> o.id().equals(f.contrato.getId())));
        assertTrue(painel.listarLotacoes(f.gestao.getId(), null, f.contrato.getId(), null, null).stream()
                .anyMatch(o -> o.id().equals(f.lotacao.getId())));
    }

    @Test void escopoUsaGestaoDaFotografiaHistorica() {
        var f = fixture(3); alocar(f);
        GestaoLocal nova = new GestaoLocal(); nova.setNome("Nova gestão"); nova.setCodigo("NOV");
        nova.setSuperintendencia(f.gestao.getSuperintendencia()); em.persist(nova);
        VersaoContratoNegocial antes = versao(f.contrato, f.gestao, 1, f.contrato.getDataInicio());
        em.persist(antes);
        VersaoContratoNegocial depois = versao(f.contrato, nova, 2, data.plusDays(1)); em.persist(depois);
        f.contrato.setGestaoLocal(nova); em.flush();
        var ponto = painel.obterSerie(data, data, f.contrato.getId(), null, null, null, f.gestao.getId(), null).get(0);
        assertEquals(3, ponto.efetivoPrevisto()); assertEquals(1, ponto.efetivoAlocado());
        var fora = painel.obterSerie(data, data, f.contrato.getId(), null, null, null, nova.getId(), null).get(0);
        assertEquals(0, fora.efetivoPrevisto()); assertEquals(0, fora.efetivoAlocado());
    }

    @Test void apiDeFiltrosIncluiContratoEncerradoDentroDoPeriodoSolicitado() throws Exception {
        var f = fixture(3);
        f.contrato.setDataFim(data); f.composicao.setDataFimVigencia(data);
        documentarPublicacao(f.contrato, f.gestao); em.flush();
        var opcoes = filtrosHttp(f.gestao, data.withDayOfMonth(30), "MENSAL", false);
        assertTrue(contem(opcoes, "contratos", f.contrato.getId()));
        assertTrue(contem(opcoes, "lotacoes", f.lotacao.getId()));
        assertTrue(contem(opcoes, "turnos", f.jornada.getId()));
        assertTrue(contem(opcoes, "cargos", f.cargo.getId()));
    }

    @Test void apiDeFiltrosRespeitaGestaoResponsavelNaDataHistorica() throws Exception {
        var f = fixture(3);
        GestaoLocal nova = new GestaoLocal(); nova.setNome("Nova gestão"); nova.setCodigo("NOV");
        nova.setSuperintendencia(f.gestao.getSuperintendencia()); em.persist(nova);
        em.persist(versao(f.contrato, f.gestao, 1, f.contrato.getDataInicio()));
        em.persist(versao(f.contrato, nova, 2, data.plusDays(1)));
        f.contrato.setGestaoLocal(nova); em.flush();
        assertTrue(contem(filtrosHttp(f.gestao, data, "DIARIO", false), "contratos", f.contrato.getId()));
        assertFalse(contem(filtrosHttp(nova, data, "DIARIO", false), "contratos", f.contrato.getId()));
        assertFalse(contem(filtrosHttp(f.gestao, data.plusDays(1), "DIARIO", false), "contratos", f.contrato.getId()));
        assertTrue(contem(filtrosHttp(nova, data.plusDays(1), "DIARIO", false), "contratos", f.contrato.getId()));
    }

    @Test void apiDeFiltrosIncluiDimensoesDoAnoAnteriorQuandoComparacaoEstaAtiva() throws Exception {
        var f = fixture(3);
        f.contrato.setDataInicio(f.contrato.getDataInicio().minusYears(1));
        f.contrato.setDataFim(f.contrato.getDataFim().minusYears(1));
        f.contrato.setPublicadoEm(Instant.parse("2025-01-01T00:00:00Z"));
        f.composicao.setDataInicioVigencia(f.contrato.getDataInicio());
        f.composicao.setDataFimVigencia(f.contrato.getDataFim());
        documentarPublicacao(f.contrato, f.gestao); em.flush();
        assertFalse(contem(filtrosHttp(f.gestao, data, "MENSAL", false), "contratos", f.contrato.getId()));
        var opcoes = filtrosHttp(f.gestao, data, "MENSAL", true);
        assertTrue(contem(opcoes, "contratos", f.contrato.getId()));
        assertTrue(contem(opcoes, "lotacoes", f.lotacao.getId()));
    }

    @Test void resumoGerencialUsaMesmaPrevisaoEAlocacaoDoIndicadorCanonico() {
        var f = fixture(3); documentarPublicacao(f.contrato, f.gestao); alocar(f);
        evento(f.contrato, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, data.plusDays(1));
        f.contrato.setAtivo(false); em.flush();
        var indicador = painel.obterSerie(data, data, null, null, null, null, f.gestao.getId(), null).get(0);
        var resumo = painelGestao.obterResumoEfetivo(data, f.gestao.getId(), null);
        assertEquals(indicador.efetivoPrevisto(), resumo.efetivoPrevisto());
        assertEquals(indicador.efetivoAlocado(), resumo.efetivoVinculado());
        assertEquals(1, resumo.contratosVigentes());
    }

    private com.fasterxml.jackson.databind.JsonNode filtrosHttp(GestaoLocal gestao, LocalDate referencia,
                                                               String periodo, boolean comparar) throws Exception {
        var servico = spy(new PainelServico(null, null, null, null, null, painel, null, null, null));
        doReturn(new PainelServico.PainelCalculoContexto("LOCAL", gestao.getNome(), null, gestao.getId(),
                true, true, true, true, true)).when(servico).contextoAtual();
        String json = MockMvcBuilders.standaloneSetup(new PainelControlador(servico)).build()
                .perform(get("/api/painel/efetivo/filtros").param("referencia", referencia.toString())
                        .param("periodo", periodo).param("compararAnoAnterior", String.valueOf(comparar)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(json);
    }

    private boolean contem(com.fasterxml.jackson.databind.JsonNode opcoes, String dimensao, Long id) {
        for (var opcao : opcoes.path(dimensao)) if (opcao.path("id").asLong() == id) return true;
        return false;
    }


    @ParameterizedTest(name = "contrato={0}, lotacao={1}, turno={2}, cargo={3}")
    @MethodSource("combinacoes")
    void dezesseisRecortesContratuaisEDimensionais(boolean filtraContrato, boolean filtraLotacao,
                                                  boolean filtraTurno, boolean filtraCargo) {
        var f = fixture(2);
        Lotacao l2 = new Lotacao("Outra lotação"); l2.setGestaoLocal(f.gestao); em.persist(l2);
        Jornada j2 = new Jornada(); j2.setDescricao("Outro turno"); j2.setGestaoLocal(f.gestao); em.persist(j2);
        Cargo cg2 = new Cargo("Outro cargo", "2"); cg2.setGestaoLocal(f.gestao); em.persist(cg2);
        Contrato c2 = new Contrato(); c2.setGestaoLocal(f.gestao); c2.setNumeroContrato("OUTRO-"+UUID.randomUUID());
        c2.setAnoContrato(2026); c2.setDataInicio(f.contrato.getDataInicio()); c2.setDataFim(f.contrato.getDataFim());
        c2.setPublicadoEm(f.contrato.getPublicadoEm()); c2.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        c2.setEfetivoPrevisto(7); em.persist(c2);
        List<ComposicaoContratual> todas = List.of(f.composicao,
                compor(f.contrato,l2,f.jornada,f.cargo,3), compor(f.contrato,f.lotacao,j2,cg2,5),
                compor(c2,f.lotacao,f.jornada,f.cargo,7));
        // A preparação documental contém as três composições originais do
        // primeiro contrato (2+3+5), e a composição de7 do segundo contrato.
        f.contrato.setEfetivoPrevisto(10);
        documentarPublicacao(f.contrato, f.gestao); documentarPublicacao(c2, f.gestao);
        for (int i=0;i<todas.size();i++) {
            var comp=todas.get(i); Pessoa p=new Pessoa(); p.setNome("Pessoa sintética "+i); p.setCpf("1000000000"+i); em.persist(p);
            VinculoContratual v=new VinculoContratual();v.setPessoa(p);v.setContrato(comp.getContrato());v.setDataInicio(comp.getDataInicioVigencia());em.persist(v);
            AlocacaoVinculo a=new AlocacaoVinculo();a.setVinculo(v);a.setComposicao(comp);a.setLotacao(comp.getLotacao());
            a.setJornada(comp.getJornada());a.setCargo(comp.getCargo());a.setDataInicio(v.getDataInicio());a.setDataFim(comp.getDataFimVigencia());em.persist(a);
        }
        em.flush();
        var selecionadas=todas.stream().filter(c->!filtraContrato||c.getContrato()==f.contrato)
                .filter(c->!filtraLotacao||c.getLotacao()==f.lotacao).filter(c->!filtraTurno||c.getJornada()==f.jornada)
                .filter(c->!filtraCargo||c.getCargo()==f.cargo).toList();
        var ponto=painel.obterSerie(data,data,filtraContrato?f.contrato.getId():null,
                filtraLotacao?f.lotacao.getId():null,filtraTurno?f.jornada.getId():null,filtraCargo?f.cargo.getId():null,
                f.gestao.getId(),null).get(0);
        assertEquals(selecionadas.stream().mapToLong(ComposicaoContratual::getEfetivoPrevisto).sum(),ponto.efetivoPrevisto());
        assertEquals(selecionadas.size(),ponto.efetivoAlocado());
    }

    static Stream<Arguments> combinacoes() {
        return java.util.stream.IntStream.range(0,16).mapToObj(i->Arguments.of((i&8)!=0,(i&4)!=0,(i&2)!=0,(i&1)!=0));
    }

    private Fixture fixture(int capacidade) {
        SuperintendenciaEstadual se = new SuperintendenciaEstadual("Teste", ufSinteticaLivre()); em.persist(se);
        GestaoLocal gestao = new GestaoLocal(); gestao.setNome("Gestão teste"); gestao.setCodigo("IT");
        gestao.setSuperintendencia(se); em.persist(gestao);
        Lotacao lotacao = new Lotacao("Lotação teste"); lotacao.setGestaoLocal(gestao); em.persist(lotacao);
        Jornada jornada = new Jornada(); jornada.setDescricao("Turno teste"); jornada.setGestaoLocal(gestao); em.persist(jornada);
        Cargo cargo = new Cargo("Cargo teste", "1"); cargo.setGestaoLocal(gestao); em.persist(cargo);
        Contrato contrato = new Contrato(); contrato.setGestaoLocal(gestao); contrato.setNumeroContrato("PAINEL-"+UUID.randomUUID());
        contrato.setAnoContrato(2026); contrato.setDataInicio(LocalDate.of(2026,1,1)); contrato.setDataFim(LocalDate.of(2026,12,31));
        contrato.setPublicadoEm(Instant.parse("2026-01-01T00:00:00Z")); contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setEfetivoPrevisto(capacidade); em.persist(contrato);
        return new Fixture(gestao, lotacao, jornada, cargo, contrato, compor(contrato,lotacao,jornada,cargo,capacidade));
    }

    private String ufSinteticaLivre() {
        Set<String> usadas = new HashSet<>(em.createQuery(
                "select s.uf from SuperintendenciaEstadual s", String.class).getResultList());
        usadas.add("YY");
        usadas.addAll(com.example.siggtrat.constante.ConstantesDominio.UFS_BRASIL);
        // Mesmo namespace sintético validado no MySQL por AuditoriaHttpSuporte.
        // Cada cenário conserva sua própria SE/Gestão, mesmo com fixtures de
        // rodadas anteriores persistidas; a transação deste teste é revertida.
        String alfabeto = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ!#$%&()+,-.;=@[]^_{}~";
        for (int i = 0; i < alfabeto.length(); i++) {
            for (int j = 0; j < alfabeto.length(); j++) {
                String uf = String.valueOf(alfabeto.charAt(i)) + alfabeto.charAt(j);
                if (!usadas.contains(uf)) return uf;
            }
        }
        throw new IllegalStateException("Não há UF sintética disponível no banco descartável.");
    }

    private ComposicaoContratual compor(Contrato c, Lotacao l, Jornada j, Cargo cargo, int qtd) {
        ComposicaoContratual comp = new ComposicaoContratual(); comp.setContrato(c); comp.setLotacao(l); comp.setJornada(j);
        comp.setCargo(cargo); comp.setEfetivoPrevisto(qtd); comp.setDataInicioVigencia(c.getDataInicio()); comp.setDataFimVigencia(c.getDataFim());
        em.persist(comp); return comp;
    }
    private void alocar(Fixture f) {
        Pessoa p = new Pessoa(); p.setNome("Pessoa sintética de teste"); p.setCpf("12345678909"); em.persist(p);
        VinculoContratual v = new VinculoContratual(); v.setPessoa(p); v.setContrato(f.contrato); v.setDataInicio(f.contrato.getDataInicio()); em.persist(v);
        AlocacaoVinculo a = new AlocacaoVinculo(); a.setVinculo(v); a.setComposicao(f.composicao); a.setLotacao(f.lotacao);
        a.setJornada(f.jornada); a.setCargo(f.cargo); a.setDataInicio(v.getDataInicio()); a.setDataFim(f.contrato.getDataFim()); em.persist(a);
    }
    private EventoCicloVidaContrato evento(Contrato c, TipoEventoCicloVidaContrato tipo, LocalDate dataEvento) {
        EventoCicloVidaContrato e = new EventoCicloVidaContrato(); e.setContrato(c); e.setTipoEvento(tipo);
        e.setDataEfetiva(dataEvento); e.setMotivo("Evento sintético de regressão"); em.persist(e); return e;
    }
    private void documentarPublicacao(Contrato contrato, GestaoLocal gestao) {
        // Fonte sintética explícita da vigência, responsabilidade e capacidade
        // originais deste cenário. Nenhum HistoricoVinculoContrato é criado.
        // Chamada após a montagem original e antes de mutações negociais.
        VersaoContratoNegocial origem = versao(contrato, gestao, 1, contrato.getDataInicio());
        // A fonte é incorporada nesta preparação; seu instante de conhecimento
        // é o @PrePersist real, separado da vigência efetiva descrita nela.
        em.persist(origem);
    }

    private VersaoContratoNegocial versao(Contrato c, GestaoLocal g, int n, LocalDate efetiva) {
        VersaoContratoNegocial v = new VersaoContratoNegocial(); v.setContrato(c); v.setNumeroVersao(n); v.setDataEfetiva(efetiva);
        v.setDataInicio(c.getDataInicio()); v.setDataFim(c.getDataFim()); v.setNumeroContrato(c.getNumeroContrato());
        v.setAnoContrato(c.getAnoContrato()); v.setHabilitacaoAdministrativa(HabilitacaoAdministrativaContrato.ATIVO);
        v.setGestaoLocalId(g.getId()); v.setGestaoLocalNome(g.getNome()); v.setEfetivoPrevisto(c.getEfetivoPrevisto()); return v;
    }
    private record Fixture(GestaoLocal gestao, Lotacao lotacao, Jornada jornada, Cargo cargo, Contrato contrato, ComposicaoContratual composicao) {}
}
