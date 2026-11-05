package com.example.siggtrat.servico;

import com.example.siggtrat.dto.ContratoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** O replay e as provas são reais; somente a fronteira de armazenamento é isolada. */
class ContratoLeituraHistoricaLoteTest {
    private final LocalDate hoje = RelogioSistema.hoje();
    private final Instant registrado = Instant.parse("2026-01-01T00:00:00Z");
    private final List<Contrato> acervo = new ArrayList<>();
    private final List<EventoCicloVidaContrato> fatos = new ArrayList<>();
    private final List<VersaoContratoNegocial> fotografias = new ArrayList<>();
    private final List<ProvaHistoricaContrato> documentos = new ArrayList<>();
    private final List<RelacaoPrecedenciaEventoContrato> precedencias = new ArrayList<>();
    private final ContratoRepositorio contratos = mock(ContratoRepositorio.class);
    private final EventoCicloVidaContratoRepositorio eventos = mock(EventoCicloVidaContratoRepositorio.class,
            i -> ler(i, fatos, e -> e.getContrato().getId(), EventoCicloVidaContrato::getId,
                    EventoCicloVidaContrato::getRegistradoEm));
    private final RelacaoPrecedenciaEventoContratoRepositorio relacoes = mock(RelacaoPrecedenciaEventoContratoRepositorio.class,
            i -> ler(i, precedencias, r -> r.getContrato().getId(), RelacaoPrecedenciaEventoContrato::getId,
                    RelacaoPrecedenciaEventoContrato::getRegistradoEm));
    private final VersaoContratoNegocialRepositorio versoes = mock(VersaoContratoNegocialRepositorio.class,
            i -> ler(i, fotografias, v -> v.getContrato().getId(), VersaoContratoNegocial::getId,
                    VersaoContratoNegocial::getRegistradoEm));
    private final ProvaHistoricaContratoRepositorio provas = mock(ProvaHistoricaContratoRepositorio.class, this::lerProvas);
    private final AditamentoContratualRepositorio aditamentos = mock(AditamentoContratualRepositorio.class);
    private final TransferenciaGestaoContratoRepositorio transferencias = mock(TransferenciaGestaoContratoRepositorio.class);
    private final ComposicaoContratualRepositorio composicoes = mock(ComposicaoContratualRepositorio.class);
    private final RemanejamentoContratualRepositorio remanejamentos = mock(RemanejamentoContratualRepositorio.class);
    private final AcessoOrganizacionalServico acesso = mock(AcessoOrganizacionalServico.class);
    private final MotorTemporalContratoServico motor = new MotorTemporalContratoServico(eventos, relacoes, versoes, aditamentos);
    private final GestaoLocal gestao = new GestaoLocal();
    private final ContratoServico servico = prepararServico();

    @Test
    void listagemReutilizaEntradasHistoricasInclusiveAusenciasPreservandoDTOIntegral() {
        adicionarContrato(10L);
        adicionarContrato(20L);
        adicionarContrato(30L);
        List<ContratoDTO> individuais = acervo.stream().map(c -> servico.buscarPorId(c.getId())).toList();
        clearInvocations(eventos, relacoes, versoes, provas);

        var lista = servico.listarTodos();

        assertThat(lista).containsExactlyElementsOf(individuais);
        assertThat(lista).extracting(ContratoDTO::numeroContrato).containsExactly("FOTO-10", "FOTO-20", "FOTO-30");
        assertThat(lista).extracting(ContratoDTO::descricao).containsOnly("Descrição original preservada");
        assertThat(lista).extracting(ContratoDTO::efetivoPrevisto).containsOnly(12);
        assertThat(lista).extracting(ContratoDTO::ativo).containsOnly(true);
        assertThat(lista).extracting(ContratoDTO::gestaoLocalId).containsOnly(70L);
        assertThat(lista).extracting(ContratoDTO::tipoContratacaoDescricao).containsOnly("Tipo publicado");
        for (Object repositorio : List.of(eventos, relacoes, versoes, provas)) {
            long leituras = mockingDetails(repositorio).getInvocations().stream()
                    .filter(i -> i.getMethod().getName().startsWith("find")
                            || i.getMethod().getName().startsWith("buscar")
                            || i.getMethod().getName().startsWith("exists"))
                    .count();
            assertThat(leituras).as("A família %s deve carregar uma vez as entradas dos três contratos",
                    mockingDetails(repositorio).getMockCreationSettings().getTypeToMock().getSimpleName())
                    .isLessThanOrEqualTo(1);
        }
    }

    @Test
    void cadeiaDocumentalIncompletaContinuaRecusadaNaListaELeituraIndividual() {
        Contrato contrato = adicionarContrato(10L);
        VersaoContratoNegocial original = fotografias.get(0);
        var gerada1 = novaVersao(original, 2, 2201L, registrado.plusSeconds(1));
        var gerada3 = novaVersao(original, 3, 2203L, registrado.plusSeconds(3));
        var primeira = prova(contrato, 1, null, original, gerada1, 3001L, registrado.plusSeconds(1));
        documentos.add(primeira);
        documentos.add(prova(contrato, 3, primeira, gerada1, gerada3, 3003L, registrado.plusSeconds(3)));

        assertConflito(() -> servico.buscarPorId(10L));
        assertConflito(servico::listarTodos);
    }

    @Test
    void ausenciaDeOrigemNaoHerdaDadosMateriaisDaEntidade() {
        adicionarContrato(10L);
        fotografias.clear();

        var resultado = servico.listarTodos().get(0);

        assertThat(resultado.numeroContrato()).isNull();
        assertThat(resultado.descricao()).isNull();
        assertThat(resultado.dataInicio()).isNull();
        assertThat(resultado.gestaoLocalId()).isNull();
        assertThat(resultado.habilitacaoAdministrativa()).isNull();
        assertThat(resultado.ativo()).isFalse();
    }

    @Test
    void novaInvocacaoReleFontesEInativacaoConhecidaSemVazarContextoAnterior() {
        Contrato contrato = adicionarContrato(10L);
        assertThat(servico.listarTodos().get(0).ativo()).isTrue();
        var inativacao = evento(contrato, 900L, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, hoje);
        inativacao.setChaveNegocial("HABILITACAO_ADMINISTRATIVA");
        fatos.add(inativacao);

        assertThat(servico.listarTodos().get(0).ativo()).isFalse();
        assertThat(servico.buscarPorId(10L).ativo()).isFalse();
    }

    @Test
    void loteResolveInvalidacaoConhecidaAntesDeCortarDataEfetivaEConservaEventosIgnorados() {
        Contrato contrato = adicionarContrato(10L);
        var anterior = evento(contrato, 910L, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, hoje.minusDays(5));
        var substituto = evento(contrato, 920L, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, hoje.plusDays(5));
        anterior.setChaveNegocial("HABILITACAO_ADMINISTRATIVA");
        substituto.setChaveNegocial("HABILITACAO_ADMINISTRATIVA");
        substituto.setFatoLogicoId(anterior.getFatoLogicoId());
        Instant conhecimento = registrado.plusSeconds(20);
        ReflectionTestUtils.setField(anterior, "registradoEm", registrado.plusSeconds(10));
        ReflectionTestUtils.setField(substituto, "registradoEm", conhecimento);
        fatos.add(anterior); fatos.add(substituto);
        var relacao = new RelacaoPrecedenciaEventoContrato();
        relacao.setContrato(contrato); relacao.setEventoFonte(substituto); relacao.setEventoAlvo(anterior);
        relacao.setTipoRelacao(TipoRelacaoPrecedenciaNegocial.RETIFICA);
        ReflectionTestUtils.setField(relacao, "id", 500L);
        ReflectionTestUtils.setField(relacao, "registradoEm", conhecimento);
        precedencias.add(relacao);
        var entradas = motor.carregarLeitura(List.of(10L));

        assertThat(motor.eventosValidosOrdenados(10L, hoje, conhecimento.minusNanos(1), entradas))
                .extracting(EventoCicloVidaContrato::getId).containsExactly(1010L, 910L);
        assertThat(motor.eventosValidosOrdenados(10L, hoje, conhecimento.plusNanos(999), entradas))
                .extracting(EventoCicloVidaContrato::getId).containsExactly(1010L);
        assertThat(motor.eventosValidosOrdenados(10L, hoje.plusDays(6), conhecimento, entradas))
                .extracting(EventoCicloVidaContrato::getId).containsExactly(1010L, 920L);
        assertThat(motor.eventosValidosOrdenados(10L, hoje, conhecimento, entradas))
                .containsExactlyElementsOf(motor.eventosValidosOrdenados(10L, hoje, conhecimento));
        assertThat(motor.habilitacaoAdministrativaEm(contrato, hoje, conhecimento.minusNanos(1), Set.of(910L), entradas))
                .isEqualTo(HabilitacaoAdministrativaContrato.ATIVO);
        assertThat(motor.habilitacaoAdministrativaEm(contrato, hoje, conhecimento.minusNanos(1), Set.of(), entradas))
                .isEqualTo(HabilitacaoAdministrativaContrato.INATIVO);
    }

    @Test
    void lotePreservaVersoesBrutasESelecionaProvaPeloMesmoLimiteInclusivoDeMicros() {
        Contrato contrato = adicionarContrato(10L);
        var original = fotografias.get(0);
        Instant limite = registrado.plusSeconds(10);
        var primeiraFoto = novaVersao(original, 2, 2201L, limite);
        var segundaFoto = novaVersao(original, 3, 2202L, limite.plusNanos(1000));
        var primeira = prova(contrato, 1, null, original, primeiraFoto, 3001L, limite);
        documentos.add(primeira);
        documentos.add(prova(contrato, 2, primeira, primeiraFoto, segundaFoto, 3002L, limite.plusNanos(1000)));
        var entradas = motor.carregarLeitura(List.of(10L));

        assertThat(motor.listarVersoes(10L, entradas)).hasSize(3);
        assertThat(motor.fotografiaOriginalConhecida(contrato, limite.minusNanos(1), entradas)).contains(original);
        assertThat(motor.fotografiaOriginalConhecida(contrato, limite.plusNanos(999), entradas)).contains(primeiraFoto);
        assertThat(motor.fotografiaOriginalConhecida(contrato, limite.plusNanos(1000), entradas)).contains(segundaFoto);
        assertThat(motor.fotografiaOriginalConhecida(contrato, limite.plusNanos(999), entradas))
                .isEqualTo(motor.fotografiaOriginalConhecida(contrato, limite.plusNanos(999)));
    }

    @Test
    void loteMantemRecusaDeDuasOrigensDocumentaisDePublicacao() {
        Contrato contrato = adicionarContrato(10L);
        var original = fotografias.get(0);
        var outraOrigem = evento(contrato, 930L, TipoEventoCicloVidaContrato.PUBLICACAO, hoje.minusDays(19));
        fatos.add(outraOrigem);
        var outraFoto = novaVersao(original, 2, 2202L, registrado.plusSeconds(1));
        outraFoto.setEventoOrigem(outraOrigem);
        documentos.add(prova(contrato, 1, null, null, original, 3001L, registrado.plusSeconds(1)));
        documentos.add(prova(contrato, 1, null, null, outraFoto, 3002L, registrado.plusSeconds(1)));
        var entradas = motor.carregarLeitura(List.of(10L));

        assertConflito(() -> motor.fotografiaOriginalConhecida(contrato, registrado.plusSeconds(2)));
        assertConflito(() -> motor.fotografiaOriginalConhecida(contrato, registrado.plusSeconds(2), entradas));
    }

    private ContratoServico prepararServico() {
        gestao.setId(70L);
        gestao.setNome("Gestão histórica");
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        when(acesso.usuarioAtual()).thenReturn(usuario);
        when(acesso.ehGlobal(usuario)).thenReturn(true);
        when(contratos.findAllByOrderByNumeroContratoAsc()).thenAnswer(i -> List.copyOf(acervo));
        when(contratos.findById(anyLong())).thenAnswer(i -> acervo.stream()
                .filter(c -> c.getId().equals(i.getArgument(0))).findFirst());
        var provaServico = new ConsultaProvaHistoricaContratoServico(provas,
                mock(ProvaHistoricaComposicaoRepositorio.class), mock(ProvaHistoricaAditamentoComposicaoRepositorio.class));
        provaServico.configurarLeitura(versoes, transferencias);
        motor.configurarProvasHistoricas(provaServico);
        var composicao = new ComposicaoContratualTemporalServico(composicoes, aditamentos, remanejamentos, motor);
        composicao.configurarProvasHistoricas(provaServico);
        var aditamento = new AditamentoContratualServico(contratos, aditamentos, remanejamentos,
                mock(TipoContratacaoRepositorio.class), mock(FornecedorRepositorio.class), composicoes,
                mock(LotacaoRepositorio.class), mock(JornadaRepositorio.class), mock(CargoRepositorio.class),
                acesso, motor, composicao, mock(PermissaoExcepcionalServico.class));
        aditamento.configurarTransferencias(transferencias);
        var gestoes = mock(GestaoLocalRepositorio.class);
        when(gestoes.findById(70L)).thenReturn(Optional.of(gestao));
        var resultado = new ContratoServico(contratos, mock(EmpregadoRepositorio.class), gestoes,
                mock(HistoricoVinculoContratoRepositorio.class), acesso, mock(TipoContratacaoRepositorio.class),
                mock(FornecedorRepositorio.class), eventos, composicoes, motor, mock(IdentidadeContratualServico.class));
        resultado.configurarAditamentoTemporal(aditamento);
        resultado.configurarGestaoContratoTemporal(new GestaoContratoTemporalServico(motor, transferencias, gestoes));
        return resultado;
    }

    private Contrato adicionarContrato(long id) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setNumeroContrato("MATERIALIZADO-" + id);
        contrato.setDescricao("Descrição atual não deve substituir a fonte");
        contrato.setDataInicio(hoje.minusDays(20));
        contrato.setDataFim(hoje.plusDays(20));
        contrato.setEfetivoPrevisto(99);
        contrato.setAtivo(false);
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setPublicadoEm(registrado);
        contrato.setGestaoLocal(gestao);
        var publicacao = evento(contrato, id + 1000, TipoEventoCicloVidaContrato.PUBLICACAO, hoje.minusDays(20));
        VersaoContratoNegocial foto = new VersaoContratoNegocial();
        foto.setContrato(contrato);
        foto.setEventoOrigem(publicacao);
        foto.setNumeroVersao(1);
        foto.setDataEfetiva(hoje.minusDays(20));
        foto.definirConhecimentoDocumental(registrado);
        foto.setNumeroContrato("FOTO-" + id);
        foto.setAnoContrato(2026);
        foto.setDescricao("Descrição original preservada");
        foto.setTipoContratacaoId(80L);
        foto.setTipoContratacaoDescricao("Tipo publicado");
        foto.setDataInicio(hoje.minusDays(20));
        foto.setDataFim(hoje.plusDays(20));
        foto.setEfetivoPrevisto(12);
        foto.setGestaoLocalId(70L);
        foto.setGestaoLocalNome("Gestão histórica");
        foto.setHabilitacaoAdministrativa(HabilitacaoAdministrativaContrato.ATIVO);
        ReflectionTestUtils.setField(foto, "id", id + 2000);
        acervo.add(contrato); fatos.add(publicacao); fotografias.add(foto);
        return contrato;
    }

    private EventoCicloVidaContrato evento(Contrato contrato, long id, TipoEventoCicloVidaContrato tipo, LocalDate data) {
        var evento = new EventoCicloVidaContrato();
        evento.setContrato(contrato); evento.setTipoEvento(tipo); evento.setDataEfetiva(data);
        evento.setFatoLogicoId("FATO-" + id);
        ReflectionTestUtils.setField(evento, "id", id);
        ReflectionTestUtils.setField(evento, "registradoEm", registrado);
        return evento;
    }

    private VersaoContratoNegocial novaVersao(VersaoContratoNegocial base, int numero, long id, Instant conhecido) {
        var foto = new VersaoContratoNegocial();
        foto.setContrato(base.getContrato()); foto.setEventoOrigem(base.getEventoOrigem()); foto.setNumeroVersao(numero);
        foto.setDataEfetiva(base.getDataEfetiva()); foto.setDataInicio(base.getDataInicio()); foto.setDataFim(base.getDataFim());
        foto.setNumeroContrato(base.getNumeroContrato()); foto.setDescricao("Prova documental " + numero);
        foto.setHabilitacaoAdministrativa(base.getHabilitacaoAdministrativa());
        foto.definirConhecimentoDocumental(conhecido); ReflectionTestUtils.setField(foto, "id", id);
        fotografias.add(foto);
        return foto;
    }

    private ProvaHistoricaContrato prova(Contrato contrato, int revisao, ProvaHistoricaContrato anterior,
                                          VersaoContratoNegocial versaoAnterior, VersaoContratoNegocial gerada,
                                          long id, Instant conhecido) {
        var resultado = new ProvaHistoricaContrato(contrato, gerada.getEventoOrigem(), anterior, versaoAnterior, gerada,
                null, null, revisao, conhecido, hoje, 1L, "SEI", "Prova sintética",
                "AUT", "PROVA-" + id, "HASH", null, null, "descricao", false);
        ReflectionTestUtils.setField(resultado, "id", id);
        return resultado;
    }

    private Object lerProvas(InvocationOnMock chamada) throws Throwable {
        String metodo = chamada.getMethod().getName();
        if (metodo.equals("findByVersaoGeradaId")) return documentos.stream()
                .filter(p -> p.getVersaoGerada().getId().equals(chamada.getArgument(0))).findFirst();
        if (metodo.contains("EventoOrigem")) return ler(chamada, documentos,
                p -> p.getEventoOrigem().getId(), ProvaHistoricaContrato::getId, ProvaHistoricaContrato::getRegistradoEm);
        return ler(chamada, documentos, p -> p.getContrato().getId(),
                ProvaHistoricaContrato::getId, ProvaHistoricaContrato::getRegistradoEm);
    }

    private static <T> Object ler(InvocationOnMock chamada, List<T> dados, Function<T, Long> contrato,
                                   Function<T, Long> id, Function<T, Instant> registro) throws Throwable {
        String metodo = chamada.getMethod().getName();
        if (metodo.equals("findById")) return dados.stream().filter(v -> id.apply(v).equals(chamada.getArgument(0))).findFirst();
        if (metodo.startsWith("find") || metodo.startsWith("buscar") || metodo.startsWith("exists")) {
            Object argumento = chamada.getArgument(0);
            Collection<?> ids = argumento instanceof Collection<?> colecao ? colecao : List.of(argumento);
            Instant limite = Arrays.stream(chamada.getArguments()).filter(Instant.class::isInstance)
                    .map(Instant.class::cast).map(PrecisaoTemporal::limiteInclusivoMicros).findFirst().orElse(null);
            List<T> encontrados = dados.stream().filter(v -> ids.contains(contrato.apply(v)))
                    .filter(v -> limite == null || registro.apply(v) != null && !registro.apply(v).isAfter(limite)).toList();
            return metodo.startsWith("exists") ? !encontrados.isEmpty() : encontrados;
        }
        return RETURNS_DEFAULTS.answer(chamada);
    }

    private static void assertConflito(org.assertj.core.api.ThrowableAssert.ThrowingCallable chamada) {
        assertThatThrownBy(chamada).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }
}
