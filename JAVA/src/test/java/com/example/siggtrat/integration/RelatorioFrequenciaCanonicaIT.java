package com.example.siggtrat.integration;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dto.RelatorioFrequenciaConsolidadoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** População, paginação e cálculo partem de Pessoa/Vínculo/Alocação, inclusive sem histórico legado. */
@Transactional
@Import({IdentidadeHistoricaServico.class, RelatorioFrequenciaConsolidadoServico.class, AcessoOrganizacionalServico.class,
        EscopoAcessoUsuarioServico.class, ConfiguracaoServico.class, CalculadoraJornadaNoturnaServico.class,
        ProjecaoOperacionalPessoaServico.class, MotorTemporalVinculoServico.class, MotorTemporalAlocacaoServico.class,
        MinimizadorDadosPessoaisServico.class, GestaoContratoTemporalServico.class})
class RelatorioFrequenciaCanonicaIT extends PostgresIntegrationTestSupport {
    private static final LocalDate JUNHO = LocalDate.of(2026, 6, 30);
    private static final LocalDate JULHO = LocalDate.of(2026, 7, 1);
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    @Autowired EntityManager em;
    @Autowired RelatorioFrequenciaConsolidadoServico relatorios;
    @Autowired ProjecaoOperacionalPessoaServico projecao;
    @Autowired com.example.siggtrat.repositorio.RelatorioFrequenciaEscalaRepositorio escala;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @org.junit.jupiter.params.ParameterizedTest(name = "{0}: eventoK{1}ns, conhecidoK{2}ns, visivel={3}")
    @org.junit.jupiter.params.provider.CsvSource({
            "VINCULO,-1000,0,true", "VINCULO,0,0,true", "VINCULO,1000,0,false",
            "ALOCACAO,-1000,0,true", "ALOCACAO,0,0,true", "ALOCACAO,1000,0,false",
            "VINCULO,-1000,999,true", "VINCULO,0,999,true", "VINCULO,1000,999,false",
            "ALOCACAO,-1000,999,true", "ALOCACAO,0,999,true", "ALOCACAO,1000,999,false"})
    void fronteiraConhecimentoMicrosNaoDependeDoFusoDaSessao(String origem, long nanosEvento,
                                                            long nanosLimite, boolean visivel) {
        assertEquals("America/Sao_Paulo", jdbc.queryForObject("show TimeZone", String.class));
        Instant k = Instant.parse("2026-06-30T12:00:00.123456Z");
        Instant evento = k.plusNanos(nanosEvento), anterior = k.minusSeconds(3600);
        Fixture f = fixture(false, origem.equals("VINCULO") ? evento : anterior,
                origem.equals("ALOCACAO") ? evento : anterior);
        em.flush(); em.clear();
        var faixa = new com.example.siggtrat.repositorio.RelatorioFrequenciaEscalaRepositorio.FaixaResponsabilidade(
                f.contrato.getId(), f.gestaoA.getId(), f.gestaoA.getSuperintendencia().getId(), JUNHO, JUNHO,
                f.contrato.getNumeroContrato());
        var escopo = new com.example.siggtrat.repositorio.RelatorioFrequenciaEscalaRepositorio.EscopoConsulta(
                f.gestaoA.getId(), null, false, List.of(), List.of(faixa), k.plusNanos(nanosLimite));
        var contratos = List.of(f.contrato.getId());
        ZonedDateTime inicio = JUNHO.atStartOfDay(ZONA), fim = JUNHO.plusDays(1).atStartOfDay(ZONA);
        assertEquals(visivel ? 1L : 0L, escala.contarColaboradores(contratos, inicio, fim, null, null, escopo));
        List<Long> esperados = visivel ? List.of(f.empregado.getId()) : List.of();
        assertEquals(esperados, escala.buscarIdsColaboradores(contratos, inicio, fim, null, null, escopo, 0, 5));
        assertEquals(esperados, escala.buscarIdsColaboradoresAposId(contratos, inicio, fim, null, null, escopo, 0, 5));
        assertEquals(visivel ? 1 : 0, escala.buscarFontesDoLote(List.of(f.empregado.getId()), contratos,
                inicio, fim, null, null, escopo).size());
    }


    @AfterEach void limparAutenticacao() { SecurityContextHolder.clearContext(); }

    @Test void cadastroCanonicoSemHistoricoNemFrequenciaApareceNaPaginaComoSemLancamento() {
        Fixture f = fixture(false);
        assertEquals(0L, em.createQuery("select count(h) from HistoricoVinculoContrato h where h.empregado.id = :id", Long.class)
                .setParameter("id", f.empregado.getId()).getSingleResult());
        autenticar(f.gestorA);
        var pagina = relatorios.gerarPagina(JUNHO, JUNHO, List.of(f.contrato.getId()), null, null, 0, 25);
        assertEquals(1, pagina.totalColaboradores());
        assertEquals(1, pagina.registros().size());
        var dia = pagina.registros().get(0).diario().get(JUNHO.toString());
        assertNotNull(dia);
        assertEquals("SEM_LANCAMENTO", dia.status());
        assertEquals(480, dia.minutosPrevistos());
        assertFalse(dia.lancado());
    }

    @Test void frequenciaCanonicaValidaIntegraRelatorioAbsenteismoEExportacaoEmLotes() {
        Fixture f = fixture(false);
        frequencia(f, f.alocacaoA, JUNHO, "FALTA"); em.flush();
        autenticar(f.gestorA);
        var resultado = relatorios.gerar(JUNHO, JUNHO, List.of(f.contrato.getId()), null, null);
        assertEquals(1, resultado.registros().size());
        assertEquals(1, resultado.totais().faltas());
        assertEquals(f.empregado.getId(), resultado.registros().get(0).empregadoId());
        assertEquals(1, relatorios.gerarAbsenteismo(JUNHO, JUNHO,
                List.of(f.contrato.getId()), null, null).get(0).faltas());
        List<RelatorioFrequenciaConsolidadoDTO.Linha> exportadas = new ArrayList<>();
        assertEquals(1, relatorios.processarFrequenciaEmLotes(JUNHO, JUNHO,
                List.of(f.contrato.getId()), null, null, 100, exportadas::addAll));
        assertEquals(resultado.registros(), exportadas);
    }

    @Test void canonicidadeRespeitaEscopoDoDiaEDimensoesAntesDeContar() {
        Fixture f = fixture(true);
        autenticar(f.gestorA);
        var origem = relatorios.gerarPagina(JUNHO, JULHO, List.of(f.contrato.getId()),
                f.alocacaoA.getLotacao().getId(), f.alocacaoA.getJornada().getId(), 0, 25);
        assertEquals(1, origem.totalColaboradores());
        assertEquals(Set.of(JUNHO.toString()), origem.registros().get(0).diario().keySet());
        var dimensaoDestino = relatorios.gerarPagina(JUNHO, JULHO, List.of(f.contrato.getId()),
                f.alocacaoB.getLotacao().getId(), f.alocacaoB.getJornada().getId(), 0, 25);
        assertEquals(0, dimensaoDestino.totalColaboradores());
        assertTrue(dimensaoDestino.registros().isEmpty());
        autenticar(f.gestorB);
        var destino = relatorios.gerar(JUNHO, JULHO, List.of(f.contrato.getId()), null, null);
        assertEquals(Set.of(JULHO.toString()), destino.registros().get(0).diario().keySet());
    }

    @Test void vinculoCanonicoCanceladoNaoRessurgePeloHistoricoLegado() {
        Fixture f = fixture(false); historico(f, f.alocacaoA.getLotacao());
        f.vinculo.setEstadoNegocial(EstadoNegocialVinculo.CANCELADO); em.flush();
        autenticar(f.gestorA);
        var pagina = relatorios.gerarPagina(JUNHO, JUNHO, List.of(f.contrato.getId()), null, null, 0, 25);
        assertEquals(0, pagina.totalColaboradores());
        assertTrue(pagina.registros().isEmpty());
        assertTrue(relatorios.gerar(JUNHO, JUNHO, List.of(f.contrato.getId()), null, null).registros().isEmpty());
    }

    @Test void alocacaoCanonicaCanceladaNaoRessurgePeloHistoricoLegado() {
        Fixture f = fixture(false); historico(f, f.alocacaoA.getLotacao());
        f.alocacaoA.setEstadoNegocial(EstadoNegocialAlocacao.CANCELADA); em.flush();
        autenticar(f.gestorA);
        assertEquals(0, relatorios.gerarPagina(JUNHO, JUNHO,
                List.of(f.contrato.getId()), null, null, 0, 25).totalColaboradores());
        assertTrue(relatorios.gerar(JUNHO, JUNHO, List.of(f.contrato.getId()), null, null).registros().isEmpty());
    }

    @Test void alocacaoCanonicaPrevaleceSobreDimensaoLegadaSemDuplicarPessoa() {
        Fixture f = fixture(false);
        Lotacao antiga = lotacao(f.gestaoA, "Antiga"); historico(f, antiga); em.flush();
        autenticar(f.gestorA);
        assertEquals(0, relatorios.gerarPagina(JUNHO, JUNHO,
                List.of(f.contrato.getId()), antiga.getId(), null, 0, 25).totalColaboradores());
        var pagina = relatorios.gerarPagina(JUNHO, JUNHO, List.of(f.contrato.getId()), null, null, 0, 25);
        assertEquals(1, pagina.totalColaboradores());
        assertEquals(1, pagina.registros().size());
        assertEquals(f.alocacaoA.getLotacao().getId(), pagina.registros().get(0).lotacaoId());
    }

    @Test void fonteCanonicaPreservaReducaoNoturnaSemDuplicarHora() {
        Fixture f = fixture(false);
        Frequencia frequencia = frequencia(f, f.alocacaoA, JUNHO, "PRESENCA");
        frequencia.setEntrada1("22:00"); frequencia.setSaida1("05:00"); em.flush();
        autenticar(f.gestorA);
        var resultado = relatorios.gerar(JUNHO, JUNHO, List.of(f.contrato.getId()), null, null);
        assertEquals(1, resultado.registros().size());
        var dia = resultado.registros().get(0).diario().get(JUNHO.toString());
        assertEquals(420, dia.minutosFisicos());
        assertEquals(420, dia.minutosNoturnosReais());
        assertEquals(480, dia.minutosNoturnosReduzidos());
        assertEquals(480, dia.minutosPagaveis());
    }

    @Test void filtroPorOutroContratoNaoRessuscitaLegadoDePessoaComRaizCanonica() {
        Fixture f = fixture(false);
        Contrato antigo = new Contrato(); antigo.setNumeroContrato("LEG-" + UUID.randomUUID()); antigo.setAnoContrato(2026);
        antigo.setDataInicio(f.contrato.getDataInicio()); antigo.setDataFim(f.contrato.getDataFim());
        antigo.setGestaoLocal(f.gestaoA); antigo.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        antigo.setPublicadoEm(Instant.parse("2026-01-01T00:00:00Z")); em.persist(antigo);
        versao(antigo, f.gestaoA, 1, antigo.getDataInicio());
        historico(f, f.alocacaoA.getLotacao(), antigo); em.flush();
        autenticar(f.gestorA);
        var pagina = relatorios.gerarPagina(JUNHO, JUNHO, List.of(antigo.getId()), null, null, 0, 25);
        assertEquals(0, pagina.totalColaboradores());
        assertTrue(pagina.registros().isEmpty());
    }

    @Test void fotografiaDiariaEMemoriaGravadaPrevalecemSobreJornadaAtual() {
        Fixture f = fixture(false);
        Frequencia fotografada = frequencia(f, f.alocacaoA, JUNHO, "PRESENCA");
        fotografada.setJornadaSnapshotDiaTrabalho(true);
        fotografada.setJornadaSnapshotEntrada1("22:00"); fotografada.setJornadaSnapshotSaida1("05:00");
        Frequencia memoria = frequencia(f, f.alocacaoA, JULHO, "PRESENCA");
        memoria.setMinutosFisicos(420); memoria.setMinutosNoturnosReais(420); memoria.setMinutosPagaveis(480);
        em.flush(); autenticar(f.gestorA);
        var dias = relatorios.gerar(JUNHO, JULHO, List.of(f.contrato.getId()), null, null)
                .registros().get(0).diario();
        assertEquals(420, dias.get(JUNHO.toString()).minutosPrevistos());
        assertEquals(420, dias.get(JUNHO.toString()).minutosFisicos());
        assertEquals(480, dias.get(JUNHO.toString()).minutosPagaveis());
        assertEquals(420, dias.get(JULHO.toString()).minutosFisicos());
        assertEquals(480, dias.get(JULHO.toString()).minutosPagaveis());
    }

    @Test void intervalosQueTocamConsultaMasNaoSeSobrepoemNaoContamPessoa() {
        Fixture f = fixture(false);
        f.vinculo.setDataInicio(JULHO); f.alocacaoA.setDataFim(JUNHO); em.flush();
        autenticar(f.gestorA);
        var pagina = relatorios.gerarPagina(JUNHO, JULHO, List.of(f.contrato.getId()), null, null, 0, 25);
        assertEquals(0, pagina.totalColaboradores());
        assertTrue(pagina.registros().isEmpty());
    }

    @Test void alocacaoDaOrigemAindaAbertaNaoEReclassificadaParaGestaoDeDestino() {
        Fixture f = fixture(true);
        f.alocacaoA.setDataFim(null); f.alocacaoB.setEstadoNegocial(EstadoNegocialAlocacao.CANCELADA); em.flush();
        autenticar(f.gestorB);
        assertEquals(0, relatorios.gerarPagina(JULHO, JULHO,
                List.of(f.contrato.getId()), null, null, 0, 25).totalColaboradores());
        autenticar(f.gestorA);
        var origem = relatorios.gerar(JUNHO, JULHO, List.of(f.contrato.getId()), null, null);
        assertEquals(Set.of(JUNHO.toString()), origem.registros().get(0).diario().keySet());
    }

    @Test void fotografiaDeUmDiaNaoPreencheNomeDeTodoIntervalo() {
        Fixture f = fixture(false);
        Frequencia foto = frequencia(f, f.alocacaoA, JULHO, "PRESENCA");
        foto.setEmpregadoSnapshotNome("Nome comprovado apenas em julho");
        em.flush(); autenticar(f.gestorA);
        assertEquals("Nome comprovado apenas em julho", relatorios.gerar(JULHO, JULHO,
                List.of(f.contrato.getId()), null, null).registros().get(0).nome());
        var intervalo = relatorios.gerar(JUNHO, JULHO, List.of(f.contrato.getId()), null, null);
        assertEquals(1, intervalo.registros().size());
        assertEquals(2, intervalo.registros().get(0).diario().size());
        assertNull(intervalo.registros().get(0).nome(), "Uma foto diária não documenta o outro dia sem fonte.");
        assertNull(relatorios.gerarAbsenteismo(JUNHO, JULHO,
                List.of(f.contrato.getId()), null, null).get(0).nome());
    }

    @Test void provaNominalTardiaMudaVisaoVigenteSemReescreverFotoDaFrequencia() {
        Fixture f = fixture(false);
        Frequencia foto = frequencia(f, f.alocacaoA, JULHO, "PRESENCA");
        foto.setEmpregadoSnapshotNome("Nome fotografado originalmente");
        em.flush(); autenticar(f.gestorA);
        assertEquals("Nome fotografado originalmente", relatorios.gerar(JULHO, JULHO,
                List.of(f.contrato.getId()), null, null).registros().get(0).nome());
        ProvaNomePessoaHistorico prova = new ProvaNomePessoaHistorico();
        prova.setPessoa(f.vinculo.getPessoa()); prova.setVinculo(f.vinculo); prova.setContrato(f.contrato);
        prova.setNomeComprovado("Nome documental do período");
        prova.setDataInicioEfetiva(JULHO); prova.setDataFimEfetiva(JULHO);
        prova.setReferenciaSei("SEI-SINTETICO-PROVA-NOMINAL");
        prova.setMotivo("Fixture explícita de prova recebida depois da frequência");
        prova.setAutorizacao("ATO-SINTETICO-NOMINAL");
        prova.setResponsavelId(f.gestorA.getId()); prova.setResponsavelUsername(f.gestorA.getUsername());
        prova.setRegistradoEm(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        prova.setChaveIdempotencia(UUID.randomUUID().toString());
        prova.setHashIntencao("a".repeat(64)); prova.setHashConteudo("b".repeat(64));
        em.persist(prova); em.flush();
        long frequenciaId = foto.getId(); em.clear();
        assertEquals("Nome documental do período", relatorios.gerar(JULHO, JULHO,
                List.of(f.contrato.getId()), null, null).registros().get(0).nome());
        assertEquals("Nome fotografado originalmente", em.find(Frequencia.class, frequenciaId).getEmpregadoSnapshotNome());
        assertNull(relatorios.gerar(JUNHO, JUNHO, List.of(f.contrato.getId()), null, null).registros().get(0).nome(),
                "A prova posterior só cobre o intervalo declarado.");
    }

    private Fixture fixture(boolean transferido) {
        return fixture(transferido, null, null);
    }

    private Fixture fixture(boolean transferido, Instant criadoVinculo, Instant criadaAlocacao) {
        GestaoLocal a = gestao("Origem", "RA"), b = transferido ? gestao("Destino", "RB") : a;
        if (transferido) assertNotEquals(a.getSuperintendencia().getId(), b.getSuperintendencia().getId(),
                "Origem e destino precisam manter escopos estaduais distintos.");
        Contrato c = new Contrato(); c.setNumeroContrato("CAN-" + UUID.randomUUID()); c.setAnoContrato(2026);
        c.setDataInicio(LocalDate.of(2026, 1, 1)); c.setDataFim(LocalDate.of(2026, 12, 31));
        c.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO); c.setPublicadoEm(Instant.parse("2026-01-01T00:00:00Z"));
        c.setGestaoLocal(b); em.persist(c); versao(c, a, 1, c.getDataInicio());
        if (transferido) versao(c, b, 2, JULHO);
        Pessoa p = new Pessoa(); p.setNome("Pessoa canônica de relatório"); p.setCpf("98765432100"); em.persist(p);
        VinculoContratual v = new VinculoContratual(); v.setPessoa(p); v.setContrato(c);
        v.setDataInicio(LocalDate.of(2026, 6, 1)); v.setMatricula("M-CAN");
        if (criadoVinculo != null) org.springframework.test.util.ReflectionTestUtils.setField(v, "criadoEm", criadoVinculo);
        em.persist(v);
        AlocacaoVinculo aa = alocacao(v, a, v.getDataInicio(), transferido ? JUNHO : null, criadaAlocacao);
        AlocacaoVinculo ab = transferido ? alocacao(v, b, JULHO, null, criadaAlocacao) : aa;
        // O contexto mínimo de persistência não executa InicializadorDados.
        // A fixture fornece o catálogo completo usado pela projeção, sem
        // depender de outra classe ter carregado dados no mesmo PostgreSQL.
        for (String descricao : List.of("ATIVO", "INATIVO")) {
            if (em.createQuery("select count(s) from StatusEmpregado s where upper(s.descricao) = :descricao", Long.class)
                    .setParameter("descricao", descricao).getSingleResult() == 0)
                em.persist(new StatusEmpregado(descricao));
        }
        Empregado e = projecao.projetar(v, ab);
        UsuarioAplicacao ua = usuario(a), ub = transferido ? usuario(b) : ua;
        em.flush();
        return new Fixture(a, b, c, v, aa, ab, e, ua, ub);
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
    private Lotacao lotacao(GestaoLocal g, String nome) {
        Lotacao l = new Lotacao(nome + " " + g.getNome()); l.setGestaoLocal(g); em.persist(l); return l;
    }
    private AlocacaoVinculo alocacao(VinculoContratual v, GestaoLocal g, LocalDate inicio, LocalDate fim, Instant criadaEm) {
        Jornada j = new Jornada(); j.setDescricao("Turno " + g.getNome()); j.setGestaoLocal(g);
        for (DayOfWeek dia : List.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)) {
            JornadaDiaSemana d = new JornadaDiaSemana(); d.setJornada(j); d.setDiaSemana(dia);
            d.setEntrada1("08:00"); d.setSaida1("12:00"); d.setEntrada2("13:00"); d.setSaida2("17:00");
            j.getDias().add(d);
        }
        em.persist(j);
        Cargo cargo = new Cargo("Cargo " + g.getNome(), "1"); cargo.setGestaoLocal(g); em.persist(cargo);
        AlocacaoVinculo a = new AlocacaoVinculo(); a.setVinculo(v); a.setLotacao(lotacao(g, "Lotação"));
        a.setJornada(j); a.setCargo(cargo); a.setDataInicio(inicio); a.setDataFim(fim);
        if (criadaEm != null) org.springframework.test.util.ReflectionTestUtils.setField(a, "criadoEm", criadaEm);
        em.persist(a); return a;
    }
    private UsuarioAplicacao usuario(GestaoLocal g) {
        UsuarioAplicacao u = new UsuarioAplicacao(); u.setUsername("can-rel-" + UUID.randomUUID());
        u.setPassword("hash-sintetico"); u.setEmail(u.getUsername() + "@example.test"); u.setPerfil(PerfilAcesso.GESTOR_LOCAL);
        u.setGestaoLocal(g); u.setSuperintendencia(g.getSuperintendencia()); u.setActive(true); em.persist(u); return u;
    }
    private void autenticar(UsuarioAplicacao u) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                u.getUsername(), null, List.of()));
    }
    private void versao(Contrato c, GestaoLocal g, int n, LocalDate efetiva) {
        VersaoContratoNegocial v = new VersaoContratoNegocial(); v.setContrato(c); v.setNumeroVersao(n);
        v.setDataEfetiva(efetiva); v.setDataInicio(c.getDataInicio()); v.setDataFim(c.getDataFim());
        v.setNumeroContrato(c.getNumeroContrato()); v.setAnoContrato(2026);
        v.setGestaoLocalId(g.getId()); v.setGestaoLocalNome(g.getNome());
        // Esta fixture representa contrato habilitado desde a origem datada;
        // a flag atual não é usada como prova da habilitação antiga.
        v.setHabilitacaoAdministrativa(HabilitacaoAdministrativaContrato.ATIVO); em.persist(v);
    }
    private Frequencia frequencia(Fixture f, AlocacaoVinculo a, LocalDate data, String status) {
        Frequencia r = new Frequencia(); r.setEmpregado(f.empregado); r.setPessoa(f.vinculo.getPessoa());
        r.setVinculoContratual(f.vinculo); r.setAlocacaoVinculo(a); r.setDataReferencia(data); r.setStatus(status);
        r.setContratoSnapshotId(f.contrato.getId()); r.setLotacaoSnapshotId(a.getLotacao().getId());
        r.setJornadaSnapshotId(a.getJornada().getId()); r.setCargoSnapshotId(a.getCargo().getId());
        GestaoLocal g = a.getLotacao().getGestaoLocal(); r.setGestaoLocalSnapshotId(g.getId());
        r.setSuperintendenciaSnapshotId(g.getSuperintendencia().getId()); em.persist(r); return r;
    }
    private void historico(Fixture f, Lotacao lotacao) {
        historico(f, lotacao, f.contrato);
    }
    private void historico(Fixture f, Lotacao lotacao, Contrato contrato) {
        HistoricoVinculoContrato h = new HistoricoVinculoContrato(); h.setEmpregado(f.empregado); h.setContrato(contrato);
        h.setLotacao(lotacao); h.setJornada(f.alocacaoA.getJornada()); h.setCargo(f.alocacaoA.getCargo());
        h.setDataInicio(f.vinculo.getDataInicio().atStartOfDay(ZONA));
        h.setGestaoLocalSnapshotId(f.gestaoA.getId()); h.setSuperintendenciaSnapshotId(f.gestaoA.getSuperintendencia().getId());
        h.setJornadaSnapshotMinutosDia("TUESDAY:480;WEDNESDAY:480"); em.persist(h);
    }
    private record Fixture(GestaoLocal gestaoA, GestaoLocal gestaoB, Contrato contrato, VinculoContratual vinculo,
                           AlocacaoVinculo alocacaoA, AlocacaoVinculo alocacaoB, Empregado empregado,
                           UsuarioAplicacao gestorA, UsuarioAplicacao gestorB) {}
}
