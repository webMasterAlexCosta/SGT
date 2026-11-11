package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dominio.vinculo.SituacaoTemporalVinculo;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Regressões observáveis das regras VIN-003, DES-005/006, CMP-005/009 e ALO-007. */
class VinculoAlocacaoRegressaoTest {
    private final LocalDate hoje = RelogioSistema.hoje();
    private final List<AlocacaoVinculo> faixas = new ArrayList<>();
    private final List<EventoAlocacaoVinculo> eventosAlocacao = new ArrayList<>();
    private final List<VinculoContratual> vinculosPersistidos = new ArrayList<>();
    private final List<Boolean> excedenteNoInsertVinculo = new ArrayList<>();
    private final List<ComposicaoContratualTemporalServico.ComposicaoProjetada> composicoesTemporais = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(100);
    private VinculoContratualRepositorio vinculos;
    private AlocacaoVinculoRepositorio alocacoes;
    private ComposicaoContratualRepositorio composicoes;
    private ContratoRepositorio contratos;
    private PessoaServico pessoas;
    private EventoVinculoContratualRepositorio eventosV;
    private AcessoOrganizacionalServico acesso;
    private ComposicaoContratualTemporalServico temporal;
    private VinculoContratualServico servicoVinculo;
    private AlocacaoVinculoServico servicoAlocacao;
    private Contrato contrato;
    private Pessoa pessoa;
    private UsuarioAplicacao usuario;
    private ComposicaoContratual origem;
    private ComposicaoContratual destino;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @BeforeEach
    void preparar() {
        vinculos = mock(VinculoContratualRepositorio.class);
        alocacoes = mock(AlocacaoVinculoRepositorio.class);
        composicoes = mock(ComposicaoContratualRepositorio.class);
        contratos = mock(ContratoRepositorio.class);
        pessoas = mock(PessoaServico.class);
        acesso = mock(AcessoOrganizacionalServico.class);
        temporal = mock(ComposicaoContratualTemporalServico.class);
        EventoAlocacaoVinculoRepositorio eventosA = mock(EventoAlocacaoVinculoRepositorio.class);
        eventosV = mock(EventoVinculoContratualRepositorio.class);
        MotorTemporalVinculoServico motorV = mock(MotorTemporalVinculoServico.class);
        servicoAlocacao = new AlocacaoVinculoServico(alocacoes, eventosA, vinculos, contratos,
                composicoes, temporal, acesso, mock(MotorTemporalAlocacaoServico.class));
        servicoVinculo = new VinculoContratualServico(vinculos, pessoas, contratos, composicoes,
                mock(ReservaMatriculaContratoRepositorio.class), alocacoes, eventosV,
                mock(MotivoDesligamentoRepositorio.class), acesso, motorV, new MinimizadorDadosPessoaisServico());
        servicoVinculo.configurarAlocacaoTemporal(servicoAlocacao);

        GestaoLocal gestao = new GestaoLocal(); gestao.setId(1L); gestao.setNome("GL"); gestao.setAtivo(true);
        contrato = new Contrato(); contrato.setId(2L); contrato.setNumeroContrato("123");
        contrato.setAnoContrato(2026); contrato.setGestaoLocal(gestao); contrato.setAtivo(true);
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setDataInicio(hoje.minusDays(60)); contrato.setDataFim(hoje.plusDays(120));
        pessoa = new Pessoa(); pessoa.setId(3L); pessoa.setNome("Maria"); pessoa.setCpf("52998224725");
        usuario = new UsuarioAplicacao(); usuario.setId(4L); usuario.setUsername("gestor"); usuario.setGestaoLocal(gestao);
        origem = composicao(10L); destino = composicao(20L);
        composicoesTemporais.add(projetada(origem, 1));
        composicoesTemporais.add(projetada(destino, 1));
        when(acesso.usuarioAtual()).thenReturn(usuario);
        when(acesso.ehGestaoLocal(usuario)).thenReturn(true);
        when(contratos.findById(2L)).thenReturn(Optional.of(contrato));
        when(contratos.buscarPorIdParaAtualizacao(2L)).thenReturn(Optional.of(contrato));
        when(pessoas.obterEntidadeParaAtualizacao(3L)).thenReturn(pessoa);
        when(composicoes.findById(10L)).thenReturn(Optional.of(origem));
        when(composicoes.findById(20L)).thenReturn(Optional.of(destino));
        when(temporal.projetarEntidades(any(), any(), any())).thenAnswer(i -> List.copyOf(composicoesTemporais));
        when(temporal.localizar(anyList(), any(), any(), any(), any())).thenAnswer(i -> {
            Long base = i.getArgument(1), lotacao = i.getArgument(2), jornada = i.getArgument(3), cargo = i.getArgument(4);
            List<ComposicaoContratualTemporalServico.ComposicaoProjetada> estado = i.getArgument(0);
            return estado.stream().filter(p -> base != null && Objects.equals(base, p.composicaoBaseId())
                    || lotacao != null && jornada != null && cargo != null && Objects.equals(lotacao, p.lotacao().getId())
                    && Objects.equals(jornada, p.jornada().getId()) && Objects.equals(cargo, p.cargo().getId())).findFirst();
        });
        when(vinculos.findById(anyLong())).thenAnswer(i -> vinculosPersistidos.stream()
                .filter(v -> Objects.equals(v.getId(), i.getArgument(0))).findFirst());
        when(vinculos.buscarPorIdParaAtualizacao(anyLong())).thenAnswer(i -> vinculos.findById(i.getArgument(0)));
        when(vinculos.buscarIdentidade(anyLong())).thenAnswer(i -> vinculos.findById(i.getArgument(0))
                .map(v -> new VinculoContratualRepositorio.IdentidadeVinculo() {
                    public Long getContratoId() { return v.getContrato().getId(); }
                    public Long getPessoaId() { return v.getPessoa().getId(); }
                }));
        when(vinculos.buscarPeriodosRegistradosParaAtualizacao(3L)).thenAnswer(i -> vinculosPersistidos.stream()
                .filter(v -> v.getEstadoNegocial() == EstadoNegocialVinculo.REGISTRADO)
                .map(v -> new VinculoContratualRepositorio.PeriodoRegistrado() {
                    public Long getId() { return v.getId(); }
                    public LocalDate getDataInicio() { return v.getDataInicio(); }
                    public LocalDate getDataFim() { return v.getDataFim(); }
                }).toList());
        when(vinculos.findByPessoaIdAndEstadoNegocialNotOrderByDataInicioAscIdAsc(eq(3L), any()))
                .thenAnswer(i -> List.copyOf(vinculosPersistidos));
        when(vinculos.save(any())).thenAnswer(i -> {
            VinculoContratual v = i.getArgument(0);
            if (v.getId() == null) v.setId(ids.incrementAndGet());
            if (!vinculosPersistidos.contains(v)) vinculosPersistidos.add(v);
            return v;
        });
        when(alocacoes.findByVinculoIdOrderByDataInicioAscIdAsc(anyLong())).thenAnswer(i -> faixas.stream()
                .filter(a -> Objects.equals(a.getVinculo().getId(), i.getArgument(0)))
                .sorted(Comparator.comparing(AlocacaoVinculo::getDataInicio)).toList());
        when(alocacoes.findByVinculoIdAndEstadoNegocialOrderByDataInicioAscIdAsc(anyLong(), any())).thenAnswer(i -> faixas.stream()
                .filter(a -> Objects.equals(a.getVinculo().getId(), i.getArgument(0)) && a.getEstadoNegocial() == i.getArgument(1))
                .sorted(Comparator.comparing(AlocacaoVinculo::getDataInicio)).toList());
        when(alocacoes.save(any())).thenAnswer(i -> {
            AlocacaoVinculo a = i.getArgument(0);
            if (a.getId() == null) ReflectionTestUtils.setField(a, "id", ids.incrementAndGet());
            if (!faixas.contains(a)) faixas.add(a);
            return a;
        });
        when(eventosA.save(any())).thenAnswer(i -> { EventoAlocacaoVinculo e = i.getArgument(0); eventosAlocacao.add(e); return e; });
        when(eventosV.save(any())).thenAnswer(i -> {
            EventoVinculoContratual evento = i.getArgument(0);
            excedenteNoInsertVinculo.add(evento.isInclusaoExcedente());
            return evento;
        });
        when(motorV.reconstruir(any(), any(), any())).thenAnswer(i -> {
            VinculoContratual v = i.getArgument(0);
            return new PosicaoVinculoTemporalDTO(v.getId(), 3L, 2L, hoje, Instant.now(), v.getDataInicio(), v.getDataFim(),
                    v.getMatricula(), v.getEstadoNegocial(), SituacaoTemporalVinculo.VIGENTE, null);
        });
    }

    @Test
    void cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa() {
        VinculoContratual atual = vinculo(30L, hoje.minusDays(10), hoje.plusDays(2));
        faixa(atual, origem, atual.getDataInicio(), atual.getDataFim());
        vinculo(31L, hoje.plusDays(3), hoje.plusDays(30));
        LocalDate fimAnterior = atual.getDataFim();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servicoVinculo.cancelarDesligamentoProgramado(30L, acao()));

        assertEquals(409, erro.getStatusCode().value());
        assertTrue(erro.getReason().contains("sobrepos"));
        assertEquals(fimAnterior, atual.getDataFim(), "Rejeição deve anteceder qualquer alteração do vínculo");
    }

    @Test
    void retificacaoDoFimEstendeUltimaAlocacaoSemCriarLacuna() {
        VinculoContratual v = vinculo(30L, hoje.minusDays(10), hoje.plusDays(20));
        AlocacaoVinculo a = faixa(v, origem, v.getDataInicio(), hoje.plusDays(5));

        assertDoesNotThrow(() -> servicoAlocacao.sincronizarLimitesVinculo(v, usuario, "Retificação documentada"));

        assertEquals(v.getDataFim(), a.getDataFim());
        assertEquals(1, faixas.size());
        assertTrue(eventosAlocacao.stream().anyMatch(e -> Objects.equals(e.getFimAnterior(), hoje.plusDays(5))
                && Objects.equals(e.getFimNovo(), hoje.plusDays(20))));
    }

    @Test
    void prorrogacaoMantemComposicaoOriginalDisponivelParaNovoVinculo() {
        origem.setDataFimVigencia(hoje.minusDays(1));
        composicoesTemporais.set(0, projetada(origem, 1));

        VinculoContratualDTO resultado = assertDoesNotThrow(() -> servicoVinculo.cadastrar(
                new CadastroVinculoContratualRequisicao(3L, null, 2L, hoje, hoje.plusDays(10), null, 10L)));

        assertEquals(hoje, resultado.dataInicio());
        assertEquals(1, faixas.size());
    }

    @Test
    void vinculoAbertoTemAlocacaoLimitadaPelaVigenciaSemDesligamentoFicticio() {
        VinculoContratualDTO resultado = assertDoesNotThrow(() -> servicoVinculo.cadastrar(
                new CadastroVinculoContratualRequisicao(3L, null, 2L, hoje, null, null, 10L)));

        assertNull(resultado.dataFim(), "Fim natural não deve criar desligamento no cadastro");
        assertEquals(contrato.getDataFim(), faixas.get(0).getDataFim());
    }

    @Test
    void novaCombinacaoDeAditamentoPodeSerSelecionadaSemComposicaoBase() throws Exception {
        var p = projetada(destino, 2);
        composicoesTemporais.set(1, new ComposicaoContratualTemporalServico.ComposicaoProjetada(null,
                p.lotacao(), p.jornada(), p.cargo(), p.lotacaoNome(), p.jornadaDescricao(), p.cargoNome(),
                p.efetivoPrevisto(), p.dataInicio(), p.dataFim(), p.lotacaoMunicipio(), p.jornadaSemanal()));
        CadastroVinculoContratualRequisicao req = json.readValue("""
                {"pessoaId":3,"contratoId":2,"dataInicio":"%s","dataFim":"%s",
                 "lotacaoId":220,"jornadaId":120,"cargoId":320}
                """.formatted(hoje, hoje.plusDays(10)), CadastroVinculoContratualRequisicao.class);

        assertDoesNotThrow(() -> servicoVinculo.cadastrar(req));

        assertEquals(1, faixas.size());
        assertNull(faixas.get(0).getComposicao());
        assertEquals(220L, faixas.get(0).getLotacao().getId());
    }

    @Test
    void composicaoReativadaPorAditamentoUsaQuantidadeDaDataEfetiva() {
        origem.setEfetivoPrevisto(0);
        composicoesTemporais.set(0, projetada(origem, 2));

        assertDoesNotThrow(() -> servicoVinculo.cadastrar(
                new CadastroVinculoContratualRequisicao(3L, null, 2L, hoje, hoje.plusDays(10), null, 10L)));

        assertEquals(1, faixas.size());
        assertEquals(0, origem.getEfetivoPrevisto(), "Fotografia original não deve ser reescrita");
    }

    @Test
    void auditoriaImutavelDeExcedentePrecisaEstarCompletaNoPrimeiroInsert() {
        when(alocacoes.countOcupacao(eq(2L), eq(210L), eq(110L), eq(310L), eq(hoje))).thenReturn(1L);

        servicoVinculo.cadastrar(new CadastroVinculoContratualRequisicao(3L, null, 2L,
                hoje, hoje.plusDays(10), null, 10L, true, "necessidade comprovada", "SEI-123", "ato 12"));

        assertEquals(List.of(true), excedenteNoInsertVinculo,
                "Colunas updatable=false precisam conter a evidência antes do INSERT com IDENTITY");
    }

    @Test
    void alteracaoParaComposicaoLotadaExigeConfirmacaoEspecificaDeExcedente() {
        VinculoContratual v = vinculo(30L, hoje.minusDays(10), hoje.plusDays(20));
        faixa(v, origem, v.getDataInicio(), v.getDataFim());
        when(alocacoes.countOcupacao(eq(2L), eq(220L), eq(120L), eq(320L), eq(hoje))).thenReturn(1L);
        String token = servicoAlocacao.previaAlteracao(30L, alteracao(null)).token();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servicoAlocacao.alterar(30L, alteracao(token)));

        assertEquals(409, erro.getStatusCode().value());
        assertTrue(erro.getReason().toLowerCase(Locale.ROOT).contains("excedente"));
        assertEquals(1, faixas.size());
    }

    @Test
    void previaDeAlocacaoExplicitaExcedenteAntesDeConfirmar() {
        VinculoContratual v = vinculo(30L, hoje.minusDays(10), hoje.plusDays(20));
        faixa(v, origem, v.getDataInicio(), v.getDataFim());
        when(alocacoes.countOcupacao(eq(2L), eq(220L), eq(120L), eq(320L), eq(hoje))).thenReturn(1L);

        PreviaImpactoAlocacaoDTO previa = servicoAlocacao.previaAlteracao(30L, alteracao(null));

        assertTrue(previa.impactos().stream().anyMatch(i -> i.toLowerCase(Locale.ROOT).contains("excedente")));
    }

    @Test
    void perfilSemGestaoLocalNaoPodeConfirmarAlocacaoExcedente() throws Exception {
        VinculoContratual v = vinculo(30L, hoje.minusDays(10), hoje.plusDays(20));
        faixa(v, origem, v.getDataInicio(), v.getDataFim());
        when(acesso.ehGestaoLocal(usuario)).thenReturn(false);
        when(alocacoes.countOcupacao(eq(2L), eq(220L), eq(120L), eq(320L), eq(hoje))).thenReturn(1L);
        String token = servicoAlocacao.previaAlteracao(30L, alteracao(null)).token();
        AlteracaoAlocacaoVinculoRequisicao req = json.readValue("""
                {"dataEfetiva":"%s","composicaoId":20,"justificativa":"necessidade comprovada",
                 "evidencia":"SEI-123","autorizacao":"ato 12","confirmarImpacto":true,
                 "tokenPrevia":"%s","confirmarInclusaoExcedente":true}
                """.formatted(hoje, token), AlteracaoAlocacaoVinculoRequisicao.class);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servicoAlocacao.alterar(30L, req));
        assertEquals(403, erro.getStatusCode().value());
        assertEquals(1, faixas.size());
    }

    @Test
    void vinculoFuturoUsaProrrogacaoJaRegistradaSemAnteciparProjecaoDeHoje() {
        LocalDate fimOriginal = hoje.plusDays(30);
        LocalDate fimProrrogado = hoje.plusDays(90);
        LocalDate inicioVinculo = hoje.plusDays(40);
        LocalDate fimVinculo = hoje.plusDays(60);
        contrato.setDataFim(fimOriginal);
        origem.setDataFimVigencia(fimOriginal);
        var original = projetada(origem, 1);
        composicoesTemporais.set(0, new ComposicaoContratualTemporalServico.ComposicaoProjetada(
                original.composicaoBaseId(), original.lotacao(), original.jornada(), original.cargo(),
                original.lotacaoNome(), original.jornadaDescricao(), original.cargoNome(), 1,
                original.dataInicio(), fimProrrogado, original.lotacaoMunicipio(), original.jornadaSemanal()));
        MotorTemporalContratoServico motorContrato = mock(MotorTemporalContratoServico.class);
        when(motorContrato.reconstruirPosicao(eq(contrato), any(), any())).thenAnswer(i -> {
            LocalDate data = i.getArgument(1);
            LocalDate fim = data.isAfter(hoje) ? fimProrrogado : fimOriginal;
            return new PosicaoTemporalContratoDTO(2L, data, i.getArgument(2),
                    EstadoNegocialContrato.PUBLICADO, SituacaoTemporalContrato.VIGENTE,
                    HabilitacaoAdministrativaContrato.ATIVO, contrato.getDataInicio(), fim,
                    null, List.of(), List.of());
        });
        // A política opcional está documentada na fotografia inicial. O serviço
        // real a reconstrói, sem recorrer ao cadastro atual nem ao fallback legado.
        VersaoContratoNegocialDTO fotografiaOriginal = new VersaoContratoNegocialDTO(
                1L, 1, 1L, contrato.getDataInicio(), Instant.now().minusSeconds(3600),
                contrato.getNumeroContrato(), contrato.getAnoContrato(), "Contrato publicado",
                40L, "Tipo conhecido", null, "11222333000181", "Empresa", "Empresa",
                contrato.getDataInicio(), fimOriginal, 2, 1L, "GL",
                false, false, false, null, null, HabilitacaoAdministrativaContrato.ATIVO);
        when(motorContrato.listarVersoes(2L)).thenReturn(List.of(fotografiaOriginal));
        // A seleção da origem exige a publicação conhecida, além do DTO de
        // conteúdo. Esta é a mesma fotografia original já declarada acima.
        EventoCicloVidaContrato publicacao = new EventoCicloVidaContrato();
        publicacao.setContrato(contrato);
        publicacao.setTipoEvento(TipoEventoCicloVidaContrato.PUBLICACAO);
        publicacao.setDataEfetiva(contrato.getDataInicio());
        ReflectionTestUtils.setField(publicacao, "registradoEm", fotografiaOriginal.registradoEm());
        VersaoContratoNegocial fonteOriginal = new VersaoContratoNegocial();
        fonteOriginal.setContrato(contrato);
        fonteOriginal.setNumeroVersao(1);
        fonteOriginal.setEventoOrigem(publicacao);
        ReflectionTestUtils.setField(fonteOriginal, "registradoEm", fotografiaOriginal.registradoEm());
        when(motorContrato.fotografiaOriginalConhecida(eq(contrato), any())).thenReturn(Optional.of(fonteOriginal));
        when(motorContrato.paraVersaoDTO(fonteOriginal)).thenReturn(fotografiaOriginal);
        AditamentoContratualServico politicaTemporal = new AditamentoContratualServico(
                contratos, mock(AditamentoContratualRepositorio.class), mock(RemanejamentoContratualRepositorio.class),
                mock(TipoContratacaoRepositorio.class), mock(FornecedorRepositorio.class), composicoes,
                mock(LotacaoRepositorio.class), mock(JornadaRepositorio.class), mock(CargoRepositorio.class),
                acesso, motorContrato, temporal, mock(PermissaoExcepcionalServico.class));
        // O container injeta os colaboradores que a implementação declara, sem
        // exigir API nova no teste RED nem escrever a projeção atual do Contrato.
        try (var contexto = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            contexto.getBeanFactory().registerSingleton("politicaTemporalMatricula", politicaTemporal);
            contexto.registerBean(MotorTemporalContratoServico.class, () -> motorContrato);
            contexto.registerBean(TransferenciaGestaoContratoRepositorio.class,
                    () -> mock(TransferenciaGestaoContratoRepositorio.class));
            // Dependência de leitura adicionada pelo IND08; mantém a fronteira
            // unitária de repositório, sem alterar o cenário nem suas asserções.
            contexto.registerBean(VersaoContratoNegocialRepositorio.class,
                    () -> mock(VersaoContratoNegocialRepositorio.class));
            GestaoContratoTemporalServico gestaoTemporal = mock(GestaoContratoTemporalServico.class);
            when(gestaoTemporal.gestaoAtual(any())).thenAnswer(i -> i.getArgument(0, Contrato.class).getGestaoLocal());
            when(gestaoTemporal.gestaoEm(any(), any(), any())).thenAnswer(i -> i.getArgument(0, Contrato.class).getGestaoLocal());
            contexto.registerBean(GestaoContratoTemporalServico.class, () -> gestaoTemporal);
            contexto.registerBean(MinimizadorDadosPessoaisServico.class, MinimizadorDadosPessoaisServico::new);
            // Esta fixture exercita cadastro futuro; a dependência de consulta
            // operacional continua sem participar do caminho sob teste.
            var gradeLeitura = mock(com.example.siggtrat.servico.GradeFrequenciaCanonicaServico.class);
            contexto.registerBean(com.example.siggtrat.servico.GradeFrequenciaCanonicaServico.class,
                    () -> gradeLeitura);
            // O módulo de Vínculo agora exige os resolvedores históricos reais.
            // Os repositórios continuam sendo as fronteiras unitárias desta fixture;
            // nenhum valor temporal ou resultado de autorização é fabricado aqui.
            contexto.registerBean(IdentidadeHistoricaServico.class, () -> new IdentidadeHistoricaServico(
                    mock(ProvaNomePessoaHistoricoRepositorio.class), eventosV));
            contexto.registerBean(ConsultaProvaHistoricaContratoServico.class, () -> new ConsultaProvaHistoricaContratoServico(
                    mock(ProvaHistoricaContratoRepositorio.class), mock(ProvaHistoricaComposicaoRepositorio.class)));
            // Fronteira unitária exigida pela injeção @PersistenceContext do
            // serviço; os testes de integração continuam usando PostgreSQL real.
            contexto.registerBean(jakarta.persistence.EntityManagerFactory.class,
                    () -> mock(jakarta.persistence.EntityManagerFactory.class));
            var em = mock(jakarta.persistence.EntityManager.class);
            var suporteProvas = new SuporteProvaHistoricaServico(em, acesso, gestaoTemporal,
                    mock(PermissaoExcepcionalServico.class), mock(DocumentoComprobatorioServico.class));
            contexto.registerBean(MotivoDesligamentoHistoricoServico.class, () -> new MotivoDesligamentoHistoricoServico(
                    em, mock(ProvaMotivoDesligamentoHistoricoRepositorio.class), suporteProvas,
                    acesso, gestaoTemporal, new MinimizadorDadosPessoaisServico(),
                    mock(InvalidacaoCacheServico.class), mock(InvalidacaoEmissaoRelatorioServico.class)));
            contexto.refresh();
            contexto.getAutowireCapableBeanFactory().autowireBean(servicoVinculo);
            contexto.getAutowireCapableBeanFactory().autowireBean(servicoAlocacao);

            VinculoContratualDTO resultado = assertDoesNotThrow(() -> servicoVinculo.cadastrar(
                    new CadastroVinculoContratualRequisicao(3L, null, 2L,
                            inicioVinculo, fimVinculo, null, 10L)));

            assertEquals(inicioVinculo, resultado.dataInicio());
            assertEquals(fimVinculo, resultado.dataFim());
            assertEquals(fimVinculo, faixas.get(0).getDataFim());
            var eventoCapturado = org.mockito.ArgumentCaptor.forClass(EventoVinculoContratual.class);
            verify(eventosV).save(eventoCapturado.capture());
            assertEquals(pessoa.getNome(), eventoCapturado.getValue().getPessoaNomeFotografia(),
                    "O novo ato futuro fotografa a Pessoa explicitamente escolhida, pelo resolvedor real");
            assertEquals(fimOriginal, contrato.getDataFim(),
                    "Programação futura não deve sobrescrever a projeção operacional de hoje");
            verifyNoInteractions(gradeLeitura);
        }
    }

    private AcaoVinculoContratualRequisicao acao() {
        return new AcaoVinculoContratualRequisicao("cancelamento documentado", "SEI-123", "ato 12", true, null);
    }

    private AlteracaoAlocacaoVinculoRequisicao alteracao(String token) {
        return new AlteracaoAlocacaoVinculoRequisicao(hoje, 20L, null, null, null,
                "necessidade comprovada", "SEI-123", "ato 12", true, token);
    }

    private VinculoContratual vinculo(Long id, LocalDate inicio, LocalDate fim) {
        VinculoContratual v = new VinculoContratual(); v.setId(id); v.setPessoa(pessoa); v.setContrato(contrato);
        v.setDataInicio(inicio); v.setDataFim(fim); v.setEstadoNegocial(EstadoNegocialVinculo.REGISTRADO);
        vinculosPersistidos.add(v); return v;
    }

    private AlocacaoVinculo faixa(VinculoContratual v, ComposicaoContratual c, LocalDate inicio, LocalDate fim) {
        AlocacaoVinculo a = new AlocacaoVinculo(); ReflectionTestUtils.setField(a, "id", ids.incrementAndGet()); a.setVinculo(v); a.setComposicao(c);
        a.setLotacao(c.getLotacao()); a.setJornada(c.getJornada()); a.setCargo(c.getCargo());
        a.setDataInicio(inicio); a.setDataFim(fim); a.setEstadoNegocial(EstadoNegocialAlocacao.REGISTRADA);
        faixas.add(a); return a;
    }

    private ComposicaoContratual composicao(long id) {
        Jornada j = new Jornada(); j.setId(id + 100); j.setDescricao("Turno " + id); j.setAtivo(true); j.setGestaoLocal(contrato.getGestaoLocal());
        Lotacao l = new Lotacao(); l.setId(id + 200); l.setNome("Lotação " + id); l.setAtivo(true); l.setGestaoLocal(contrato.getGestaoLocal()); l.setTurnos(List.of(j));
        Cargo c = new Cargo(); c.setId(id + 300); c.setNome("Cargo " + id); c.setAtivo(true); c.setGestaoLocal(contrato.getGestaoLocal());
        ComposicaoContratual resultado = new ComposicaoContratual(); resultado.setId(id); resultado.setContrato(contrato);
        resultado.setLotacao(l); resultado.setJornada(j); resultado.setCargo(c); resultado.setEfetivoPrevisto(1);
        resultado.setDataInicioVigencia(contrato.getDataInicio()); resultado.setDataFimVigencia(contrato.getDataFim()); return resultado;
    }

    private ComposicaoContratualTemporalServico.ComposicaoProjetada projetada(ComposicaoContratual c, int efetivo) {
        return new ComposicaoContratualTemporalServico.ComposicaoProjetada(c.getId(), c.getLotacao(), c.getJornada(), c.getCargo(),
                c.getLotacao().getNome(), c.getJornada().getDescricao(), c.getCargo().getNome(), efetivo,
                c.getDataInicioVigencia(), c.getDataFimVigencia(),
                c.getLotacaoMunicipioFotografia(), c.getJornadaSemanalFotografia());
    }
}
