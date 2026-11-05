package com.example.siggtrat.integration;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dto.RelatorioFrequenciaConsolidadoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

/** A autorização atual é aplicada à responsabilidade histórica de cada dia. */
@Transactional
@Import({IdentidadeHistoricaServico.class, RelatorioFrequenciaConsolidadoServico.class, AcessoOrganizacionalServico.class,
        GestaoContratoTemporalServico.class,
        EscopoAcessoUsuarioServico.class, ConfiguracaoServico.class, CalculadoraJornadaNoturnaServico.class,
        RelatorioConsultaServico.class, RelatorioTerceirizadosServico.class, MinimizadorDadosPessoaisServico.class})
class RelatorioFrequenciaEscopoHistoricoIT extends MySqlIntegrationTestSupport {
    private static final LocalDate JUNHO = LocalDate.of(2026, 6, 30);
    private static final LocalDate JULHO = LocalDate.of(2026, 7, 1);
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    @Autowired EntityManager em;
    @Autowired RelatorioFrequenciaConsolidadoServico relatorios;
    @Autowired RelatorioConsultaServico consultas;
    @Autowired MotorTemporalContratoServico motorContrato;

    @AfterEach void limparAutenticacao() { SecurityContextHolder.clearContext(); }

    @Test void gestaoDeOrigemConsultaJunhoAposTransferenciaEmJulho() {
        Fixture f = fixture(false, false);
        autenticar(f.gestorA);
        var resultado = assertDoesNotThrow(() -> relatorios.gerar(JUNHO, JUNHO,
                List.of(f.contrato.getId()), null, null));
        assertEquals(Set.of("2026-06-30"), dias(resultado));
        assertEquals(1, resultado.totais().faltas());
    }

    @Test void intervaloMistoNaoEntregaJulhoAGestaoDeOrigem() {
        Fixture f = fixture(false, false);
        autenticar(f.gestorA);
        var resultado = assertDoesNotThrow(() -> relatorios.gerar(JUNHO, JULHO,
                List.of(f.contrato.getId()), null, null));
        assertEquals(Set.of("2026-06-30"), dias(resultado));
        assertEquals(1, resultado.totais().diasLancados());
    }

    @Test void gestaoDeDestinoRecebeSomenteDiasPosterioresATransferencia() {
        Fixture f = fixture(false, false);
        autenticar(f.gestorB);
        var resultado = relatorios.gerar(JUNHO, JULHO, List.of(f.contrato.getId()), null, null);
        assertEquals(Set.of("2026-07-01"), dias(resultado));
        assertEquals(1, resultado.totais().faltas());
    }

    @Test void contratoInativoHojeContinuaNoRelatorioHistoricoSemFiltroDeContrato() {
        Fixture f = fixture(true, false);
        autenticar(f.gestorA);
        var resultado = relatorios.gerar(JUNHO, JUNHO, null, null, null);
        assertEquals(Set.of("2026-06-30"), dias(resultado));
        assertEquals(List.of(f.contrato.getId()), resultado.contratos());
    }

    @Test void flagAtualSemDataPreservaFaixaHistoricaMesmoSemVersoes() {
        Fixture f = fixture(true, false);
        em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id = :id")
                .setParameter("id", f.contrato.getId()).executeUpdate();
        em.flush(); em.clear();
        autenticar(f.gestorA);

        var resultado = relatorios.gerar(JUNHO, JUNHO, null, null, null);
        assertEquals(Set.of("2026-06-30"), dias(resultado));
        assertEquals(1, resultado.totais().faltas());
        assertEquals(List.of(f.contrato.getId()), resultado.contratos());
        assertLegadoIntacto(f, 0);
    }

    @Test void inativacaoDocumentadaPreservaPeriodoAnteriorEExcluiSomenteAlcance() {
        Fixture f = fixture(true, false);
        EventoCicloVidaContrato bloqueio = new EventoCicloVidaContrato();
        bloqueio.setContrato(f.contrato);
        bloqueio.setTipoEvento(TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA);
        bloqueio.setChaveNegocial("HABILITACAO_ADMINISTRATIVA");
        bloqueio.setDataEfetiva(JULHO); bloqueio.setDataAto(JULHO);
        bloqueio.setMotivo("Ato documentado bloqueia a operação a partir de julho");
        bloqueio.setReferenciaSei("SEI-BLOQUEIO-JULHO");
        bloqueio.setUsuarioResponsavelId(f.gestorB.getId());
        bloqueio.setUsuarioResponsavelUsername(f.gestorB.getUsername());
        em.persist(bloqueio); em.flush(); em.clear();

        autenticar(f.gestorA);
        assertEquals(Set.of("2026-06-30"), dias(relatorios.gerar(JUNHO, JUNHO,
                List.of(f.contrato.getId()), null, null)));
        autenticar(f.gestorB);
        assertTrue(dias(relatorios.gerar(JULHO, JULHO,
                List.of(f.contrato.getId()), null, null)).isEmpty());
        assertEquals(2L, em.createQuery("select count(f) from Frequencia f where f.contratoSnapshotId=:id", Long.class)
                .setParameter("id", f.contrato.getId()).getSingleResult(), "Bloqueio de cálculo não apaga as fontes");
        assertEquals(1L, em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:id", Long.class)
                .setParameter("id", f.contrato.getId()).getSingleResult());
    }

    @Test void fotografiaAdministrativaInativaPermaneceBloqueadaMesmoComFlagAtualAtiva() {
        Fixture f = fixture(false, false);
        assertEquals(1, em.createQuery("update VersaoContratoNegocial v set v.habilitacaoAdministrativa=:estado "
                        + "where v.contrato.id=:id and v.numeroVersao=1")
                .setParameter("estado", HabilitacaoAdministrativaContrato.INATIVO)
                .setParameter("id", f.contrato.getId()).executeUpdate());
        em.flush(); em.clear();
        autenticar(f.gestorA);

        assertTrue(dias(relatorios.gerar(JUNHO, JUNHO,
                List.of(f.contrato.getId()), null, null)).isEmpty());
        assertTrue(em.find(Contrato.class, f.contrato.getId()).getAtivo());
        assertEquals(HabilitacaoAdministrativaContrato.INATIVO,
                em.createQuery("select v.habilitacaoAdministrativa from VersaoContratoNegocial v "
                                + "where v.contrato.id=:id and v.numeroVersao=1", HabilitacaoAdministrativaContrato.class)
                        .setParameter("id", f.contrato.getId()).getSingleResult());
    }

    @Test void flagAtualSemDataNaoTransportaPreservacaoHistoricaParaDiaCorrenteOuFuturo() {
        try (MockedStatic<RelogioSistema> relogio = mockStatic(RelogioSistema.class, CALLS_REAL_METHODS)) {
            LocalDate hoje = JULHO.plusDays(1);
            relogio.when(RelogioSistema::hoje).thenReturn(hoje);
            Fixture f = fixture(false, false);
            UsuarioAplicacao nacional = usuario(PerfilAcesso.GESTOR_NACIONAL, f.gestaoB);
            em.flush(); autenticar(nacional);
            assertEquals(Set.of("2026-06-30", "2026-07-01"), dias(relatorios.gerar(JUNHO, JULHO,
                    List.of(f.contrato.getId()), null, null)), "As duas faixas existem antes da flag atual");
            f.contrato.setAtivo(false); em.flush(); em.clear();

            var resultado = relatorios.gerar(JUNHO, hoje.plusDays(1), List.of(f.contrato.getId()), null, null);
            assertEquals(Set.of("2026-06-30", "2026-07-01"), dias(resultado),
                    "A flag conhecida agora não data a inativação no passado nem libera hoje ou amanhã");
            // TMP-004/005 e HIS-005: a flag sem data não é um fato conhecido
            // para reconstruir habilitação. O gate aceita somente ATIVO;
            // desconhecimento continua bloqueando hoje e amanhã, enquanto
            // os fatos históricos acima permanecem consultáveis.
            assertNull(motorContrato.habilitacaoAdministrativaEm(
                    em.find(Contrato.class, f.contrato.getId()), hoje, Instant.now()),
                    "Flag atual false não inventa uma inativação documental");
            assertNull(motorContrato.habilitacaoAdministrativaEm(
                    em.find(Contrato.class, f.contrato.getId()), hoje.plusDays(1), Instant.now()),
                    "O futuro também permanece sem habilitação comprovada");
            assertNull(motorContrato.habilitacaoAdministrativaEm(
                    em.find(Contrato.class, f.contrato.getId()), hoje.plusDays(1),
                    Instant.parse("2026-01-02T00:00:00Z")),
                    "Corte de conhecimento anterior não pode herdar a flag atual");
            assertLegadoIntacto(f, 2);
        }
    }

    private void assertLegadoIntacto(Fixture f, long quantidadeVersoes) {
        em.flush(); em.clear();
        assertFalse(em.find(Contrato.class, f.contrato.getId()).getAtivo());
        assertEquals(2L, em.createQuery("select count(f) from Frequencia f where f.contratoSnapshotId=:id", Long.class)
                .setParameter("id", f.contrato.getId()).getSingleResult());
        assertEquals(0L, em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:id", Long.class)
                .setParameter("id", f.contrato.getId()).getSingleResult(), "Leitura não inventa ato administrativo");
        assertEquals(quantidadeVersoes, em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:id", Long.class)
                .setParameter("id", f.contrato.getId()).getSingleResult());
        assertEquals(0L, em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:id "
                        + "and v.habilitacaoAdministrativa is not null", Long.class)
                .setParameter("id", f.contrato.getId()).getSingleResult(), "Ausência de fotografia permanece explícita");
    }

    @Test void gestorEstadualDeOrigemMantemAcessoSomenteAoSeuPeriodo() {
        Fixture f = fixture(false, false);
        UsuarioAplicacao estadual = usuario(PerfilAcesso.GESTOR_ESTADUAL, f.gestaoA);
        em.flush(); autenticar(estadual);
        var resultado = assertDoesNotThrow(() -> relatorios.gerar(JUNHO, JULHO,
                List.of(f.contrato.getId()), null, null));
        assertEquals(Set.of("2026-06-30"), dias(resultado));
    }

    @Test void paginacaoNaoContaColaboradorSemDiaNoEscopoSolicitado() {
        Fixture f = fixture(false, false);
        autenticar(f.gestorA);
        var pagina = assertDoesNotThrow(() -> relatorios.gerarPagina(JULHO, JULHO,
                List.of(f.contrato.getId()), null, null, 0, 25));
        assertEquals(0, pagina.totalColaboradores());
        assertTrue(pagina.registros().isEmpty());
    }

    @Test void historicoLegadoAbertoNaoEstendeResponsabilidadeAposTransferencia() {
        Fixture f = fixture(false, true);
        autenticar(f.gestorA);
        var resultado = assertDoesNotThrow(() -> relatorios.gerar(JUNHO, JULHO,
                List.of(f.contrato.getId()), null, null));
        assertEquals(Set.of("2026-06-30"), dias(resultado));
    }

    @Test void contratoSemVersoesEncerradoNaoContaDiasPosterioresMesmoComHistoricoAberto() {
        Fixture f = fixture(false, true);
        em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id = :id")
                .setParameter("id", f.contrato.getId()).executeUpdate();
        f.contrato.setDataFim(JUNHO);
        f.contrato.setPublicadoEm(null);
        // Sem versão original, a coluna materializada não prova o término.
        // O documento sintético fixa a rescisão em JUNHO; o histórico aberto
        // continua intacto e não autoriza estender o contrato até JULHO.
        EventoCicloVidaContrato encerramento = new EventoCicloVidaContrato();
        encerramento.setContrato(f.contrato);
        encerramento.setTipoEvento(TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO);
        encerramento.setDataEfetiva(JUNHO); encerramento.setDataAto(JUNHO);
        encerramento.setChaveNegocial("ENCERRAMENTO_ANTECIPADO");
        encerramento.setMotivo("Fonte sintética comprova o último dia contratual apesar da perda da publicação");
        encerramento.setReferenciaSei("SEI-FONTE-ENCERRAMENTO-LEGADO-JUNHO");
        encerramento.setUsuarioResponsavelId(f.gestorA.getId());
        encerramento.setUsuarioResponsavelUsername(f.gestorA.getUsername());
        em.persist(encerramento); em.flush();
        assertEquals(0L, em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:id", Long.class)
                .setParameter("id", f.contrato.getId()).getSingleResult());
        assertEquals(1L, em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:id", Long.class)
                .setParameter("id", f.contrato.getId()).getSingleResult());
        autenticar(f.gestorA);
        assertEquals(0, relatorios.gerarPagina(JULHO, JULHO,
                List.of(f.contrato.getId()), null, null, 0, 25).totalColaboradores());
        assertEquals(Set.of("2026-06-30"), dias(relatorios.gerar(JUNHO, JUNHO,
                List.of(f.contrato.getId()), null, null)));
    }

    @Test void filtrosSemDataPermitemSelecionarContratoHistoricoHojeInativo() {
        Fixture f = fixture(true, false);
        autenticar(f.gestorA);
        assertEquals(List.of(f.contrato.getId()), consultas.filtros().contratos().stream()
                .map(com.example.siggtrat.dto.ContratoOpcaoDTO::id).toList());
    }

    @Test void filtrosComPeriodoRespeitamTransferenciaEDataUnicaOpcional() {
        Fixture f = fixture(false, false);
        autenticar(f.gestorA);
        assertEquals(List.of(f.contrato.getId()), consultas.filtros(JUNHO, null).contratos().stream()
                .map(com.example.siggtrat.dto.ContratoOpcaoDTO::id).toList());
        assertTrue(consultas.filtros(JULHO, JULHO).contratos().isEmpty());
    }

    @Test void historicoNaoAutorizaGestaoQueNuncaFoiResponsavel() {
        Fixture f = fixture(false, false);
        UsuarioAplicacao terceiro = usuario(PerfilAcesso.GESTOR_LOCAL, gestao("Terceira", "CC"));
        em.flush(); autenticar(terceiro);
        assertTrue(consultas.filtros().contratos().isEmpty());
        var erro = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> relatorios.gerar(JUNHO, JULHO, List.of(f.contrato.getId()), null, null));
        assertEquals(404, erro.getStatusCode().value());
    }

    @Test void leituraEmLotesNaoDescartaAlteracaoDaTransacaoGravavelChamadora() {
        Fixture f = fixture(false, false);
        autenticar(f.gestorA);
        long quantidade = relatorios.processarFrequenciaEmLotes(JUNHO, JUNHO,
                List.of(f.contrato.getId()), null, null, 100,
                linhas -> f.contrato.setDescricao("Descrição alterada no consumidor do lote"));
        assertEquals(1, quantidade);
        em.flush(); em.clear();
        assertEquals("Descrição alterada no consumidor do lote",
                em.find(Contrato.class, f.contrato.getId()).getDescricao());
    }

    private Set<String> dias(RelatorioFrequenciaConsolidadoDTO dto) {
        Set<String> resultado = new TreeSet<>();
        dto.registros().forEach(linha -> resultado.addAll(linha.diario().keySet()));
        return resultado;
    }

    private Fixture fixture(boolean inativo, boolean faixaAntigaAberta) {
        GestaoLocal a = gestao("Origem", "AA"), b = gestao("Destino", "BB");
        assertNotEquals(a.getSuperintendencia().getId(), b.getSuperintendencia().getId(),
                "Origem e destino precisam manter escopos estaduais distintos.");
        Lotacao la = lotacao(a), lb = lotacao(b);
        Jornada ja = jornada(a), jb = jornada(b);
        Cargo ca = cargo(a), cb = cargo(b);
        Contrato c = new Contrato(); c.setNumeroContrato("HIST-" + UUID.randomUUID()); c.setAnoContrato(2026);
        c.setDataInicio(LocalDate.of(2026, 1, 1)); c.setDataFim(LocalDate.of(2026, 12, 31));
        c.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO); c.setPublicadoEm(Instant.parse("2026-01-01T00:00:00Z"));
        c.setGestaoLocal(b); c.setAtivo(!inativo); em.persist(c);
        versao(c, a, 1, c.getDataInicio()); versao(c, b, 2, JULHO);
        String descricaoStatus = inativo ? "INATIVO" : "ATIVO";
        StatusEmpregado status = em.createQuery("select s from StatusEmpregado s where s.descricao = :descricao",
                StatusEmpregado.class).setParameter("descricao", descricaoStatus).getResultStream().findFirst()
                .orElseGet(() -> { StatusEmpregado s = new StatusEmpregado(descricaoStatus); em.persist(s); return s; });
        Empregado e = new Empregado(); e.setNome("Colaborador histórico"); e.setCpf("12345678909");
        e.setGestaoLocal(b); e.setLotacao(lb); e.setJornada(jb); e.setCargo(cb); e.setStatus(status);
        e.setContratos(new ArrayList<>(List.of(c))); em.persist(e);
        historico(e, c, a, la, ja, ca, c.getDataInicio(), faixaAntigaAberta ? null : JUNHO);
        if (!faixaAntigaAberta) historico(e, c, b, lb, jb, cb, JULHO, c.getDataFim());
        frequencia(e, c, a, la, ja, ca, JUNHO);
        frequencia(e, c, b, lb, jb, cb, JULHO);
        UsuarioAplicacao ua = usuario(PerfilAcesso.GESTOR_LOCAL, a), ub = usuario(PerfilAcesso.GESTOR_LOCAL, b);
        em.flush();
        return new Fixture(a, b, c, ua, ub);
    }

    private GestaoLocal gestao(String nome, String uf) {
        SuperintendenciaEstadual se = em.createQuery(
                        "select s from SuperintendenciaEstadual s where upper(s.uf) = :uf", SuperintendenciaEstadual.class)
                .setParameter("uf", uf.toUpperCase(Locale.ROOT)).getResultStream().findFirst()
                .orElseGet(() -> {
                    SuperintendenciaEstadual nova = new SuperintendenciaEstadual(nome, uf);
                    em.persist(nova); return nova;
                });
        GestaoLocal g = new GestaoLocal(); g.setNome(nome); g.setCodigo(UUID.randomUUID().toString().substring(0, 8));
        g.setSuperintendencia(se); em.persist(g); return g;
    }
    private Lotacao lotacao(GestaoLocal g) {
        Lotacao l = new Lotacao("Lotação " + g.getNome()); l.setGestaoLocal(g); em.persist(l); return l;
    }
    private Jornada jornada(GestaoLocal g) {
        Jornada j = new Jornada(); j.setDescricao("Turno " + g.getNome()); j.setGestaoLocal(g); em.persist(j); return j;
    }
    private Cargo cargo(GestaoLocal g) {
        Cargo c = new Cargo("Cargo " + g.getNome(), "1"); c.setGestaoLocal(g); em.persist(c); return c;
    }
    private UsuarioAplicacao usuario(PerfilAcesso perfil, GestaoLocal g) {
        UsuarioAplicacao u = new UsuarioAplicacao(); u.setUsername("historico-" + UUID.randomUUID());
        u.setPassword("hash-sintetico"); u.setEmail(u.getUsername() + "@example.test"); u.setPerfil(perfil);
        u.setGestaoLocal(perfil == PerfilAcesso.GESTOR_ESTADUAL ? null : g); u.setSuperintendencia(g.getSuperintendencia());
        u.setActive(true); em.persist(u); return u;
    }
    private void autenticar(UsuarioAplicacao u) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                u.getUsername(), null, List.of()));
    }
    private void versao(Contrato c, GestaoLocal g, int n, LocalDate efetiva) {
        VersaoContratoNegocial v = new VersaoContratoNegocial(); v.setContrato(c); v.setNumeroVersao(n);
        v.setDataEfetiva(efetiva); v.setDataInicio(c.getDataInicio()); v.setDataFim(c.getDataFim());
        v.setNumeroContrato(c.getNumeroContrato()); v.setAnoContrato(2026);
        v.setGestaoLocalId(g.getId()); v.setGestaoLocalNome(g.getNome()); em.persist(v);
    }
    private void historico(Empregado e, Contrato c, GestaoLocal g, Lotacao l, Jornada j, Cargo cargo,
                           LocalDate inicio, LocalDate fim) {
        HistoricoVinculoContrato h = new HistoricoVinculoContrato(); h.setEmpregado(e); h.setContrato(c);
        h.setLotacao(l); h.setJornada(j); h.setCargo(cargo); h.setDataInicio(inicio.atStartOfDay(ZONA));
        h.setDataFim(fim == null ? null : fim.plusDays(1).atStartOfDay(ZONA).minusSeconds(1));
        h.setGestaoLocalSnapshotId(g.getId()); h.setSuperintendenciaSnapshotId(g.getSuperintendencia().getId());
        h.setJornadaSnapshotMinutosDia("MONDAY:480;TUESDAY:480;WEDNESDAY:480;THURSDAY:480;FRIDAY:480");
        em.persist(h);
    }
    private void frequencia(Empregado e, Contrato c, GestaoLocal g, Lotacao l, Jornada j, Cargo cargo, LocalDate data) {
        Frequencia f = new Frequencia(); f.setEmpregado(e); f.setDataReferencia(data); f.setStatus("FALTA");
        f.setContratoSnapshotId(c.getId()); f.setLotacaoSnapshotId(l.getId()); f.setJornadaSnapshotId(j.getId());
        f.setCargoSnapshotId(cargo.getId()); f.setGestaoLocalSnapshotId(g.getId());
        f.setSuperintendenciaSnapshotId(g.getSuperintendencia().getId()); em.persist(f);
    }
    private record Fixture(GestaoLocal gestaoA, GestaoLocal gestaoB, Contrato contrato,
                           UsuarioAplicacao gestorA, UsuarioAplicacao gestorB) { }
}
