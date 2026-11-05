package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.SituacaoTemporalAlocacao;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dominio.vinculo.SituacaoTemporalVinculo;
import com.example.siggtrat.dto.PosicaoAlocacaoTemporalDTO;
import com.example.siggtrat.dto.PosicaoTemporalContratoDTO;
import com.example.siggtrat.dto.PosicaoVinculoTemporalDTO;
import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.EstadoNegocialContrato;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.HabilitacaoAdministrativaContrato;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.SituacaoTemporalContrato;
import com.example.siggtrat.entidade.VinculoContratual;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unidade do selecionador REAL. Não inicializa Spring/JPA/MySQL/HTTP.
 * O EntityManager entrega páginas controladas; os motores são fronteiras
 * controladas. O serviço de elegibilidade, seu keyset, o filtro externo e a
 * contagem de todos os vínculos da Pessoa NÃO são substituídos por mocks.
 *
 * O custo observado é a avaliação de candidatos no limite do EntityManager,
 * junto do resultado retornado. Nenhum cronômetro ou SLA é utilizado.
 */
class ElegibilidadeFechamentoPaginacaoH04Test {
    private static final Long CONTRATO = 10L;
    private static final Long LOTACAO = 20L;
    private static final Long JORNADA = 30L;
    private static final Long FECHAMENTO = 40L;
    private static final LocalDate DATA = LocalDate.of(2026, 10, 20);

    private EntityManager em;
    private MotorTemporalContratoServico contratos;
    private MotorTemporalVinculoServico vinculos;
    private MotorTemporalAlocacaoServico alocacoes;
    private ResolvedorContextoTemporalFrequenciaServico resolvedor;
    private ElegibilidadeFechamentoFrequenciaServico servico;

    private final TreeMap<Long, Empregado> empregados = new TreeMap<>();
    private final Map<Long, VinculoContratual> raizes = new LinkedHashMap<>();
    private final Map<Long, List<Long>> vinculosPorPessoa = new HashMap<>();
    private final Set<Long> vinculosNaoVigentes = new HashSet<>();
    private final Set<Long> contratosInativos = new HashSet<>();
    private final Map<Long, Long> lotacoesPorVinculo = new HashMap<>();
    private final Map<Long, Long> jornadasPorVinculo = new HashMap<>();
    private final Map<Long, ResponseStatusException> recusasDoResolvedor = new HashMap<>();
    private final List<Long> candidatosAvaliados = new ArrayList<>();
    private final List<Long> vinculosAvaliados = new ArrayList<>();
    private final List<PaginaLida> paginasLidas = new ArrayList<>();
    private final List<TransacaoPedida> transacoesPedidas = new ArrayList<>();
    private final List<CortePedido> cortesPedidos = new ArrayList<>();
    private LocalDate dataDaBusca = DATA;

    @BeforeEach
    void prepararFronteiras() {
        em = mock(EntityManager.class);
        contratos = mock(MotorTemporalContratoServico.class);
        vinculos = mock(MotorTemporalVinculoServico.class);
        alocacoes = mock(MotorTemporalAlocacaoServico.class);
        resolvedor = mock(ResolvedorContextoTemporalFrequenciaServico.class);
        PlatformTransactionManager transacoes = mock(PlatformTransactionManager.class);
        when(transacoes.getTransaction(any(TransactionDefinition.class))).thenAnswer(inv -> {
            TransactionDefinition definicao = inv.getArgument(0);
            transacoesPedidas.add(new TransacaoPedida(definicao.getPropagationBehavior(), definicao.isReadOnly()));
            return new SimpleTransactionStatus();
        });
        servico = new ElegibilidadeFechamentoFrequenciaServico(contratos, vinculos, alocacoes,
                resolvedor, mock(ProjecaoOperacionalPessoaServico.class), transacoes,
                mock(BloqueioEscritaFrequenciaServico.class));
        ReflectionTestUtils.setField(servico, "em", em);

        when(em.createQuery(anyString(), eq(Long.class))).thenAnswer(inv -> consulta(inv.getArgument(0)));
        when(em.find(eq(Empregado.class), any())).thenAnswer(inv -> {
            Long id = inv.getArgument(1);
            candidatosAvaliados.add(id);
            return empregados.get(id);
        });
        when(em.find(eq(VinculoContratual.class), any())).thenAnswer(inv -> raizes.get((Long) inv.getArgument(1)));
        when(vinculos.reconstruir(any(VinculoContratual.class), any(LocalDate.class), any(Instant.class)))
                .thenAnswer(inv -> {
                    VinculoContratual vinculo = inv.getArgument(0);
                    LocalDate data = inv.getArgument(1);
                    Instant conhecimento = inv.getArgument(2);
                    cortesPedidos.add(new CortePedido("vinculo", vinculo.getId(), data, conhecimento));
                    vinculosAvaliados.add(vinculo.getId());
                    return posicaoVinculo(vinculo, data, conhecimento);
                });
        when(contratos.reconstruirPosicao(any(Contrato.class), any(LocalDate.class), any(Instant.class)))
                .thenAnswer(inv -> {
                    Contrato contrato = inv.getArgument(0);
                    LocalDate data = inv.getArgument(1);
                    Instant conhecimento = inv.getArgument(2);
                    cortesPedidos.add(new CortePedido("contrato", contrato.getId(), data, conhecimento));
                    return posicaoContrato(contrato, data, conhecimento);
                });
        when(alocacoes.reconstruir(any(VinculoContratual.class), any(LocalDate.class), any(Instant.class)))
                .thenAnswer(inv -> {
                    VinculoContratual vinculo = inv.getArgument(0);
                    LocalDate data = inv.getArgument(1);
                    Instant conhecimento = inv.getArgument(2);
                    cortesPedidos.add(new CortePedido("alocacao", vinculo.getId(), data, conhecimento));
                    return posicaoAlocacao(vinculo, data, conhecimento);
                });
        when(resolvedor.resolverPorVinculo(anyLong(), any(LocalDate.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            LocalDate data = inv.getArgument(1);
            ResponseStatusException recusa = recusasDoResolvedor.get(id);
            if (recusa != null) throw recusa;
            VinculoContratual vinculo = raizes.get(id);
            Instant conhecimento = Instant.parse("2026-11-03T12:00:00Z");
            AlocacaoVinculo alocacao = new AlocacaoVinculo();
            alocacao.setVinculo(vinculo);
            GestaoLocal gestao = new GestaoLocal();
            gestao.setId(60L);
            return new ResolvedorContextoTemporalFrequenciaServico.Contexto(vinculo.getPessoa(), vinculo,
                    alocacao, posicaoContrato(vinculo.getContrato(), data, conhecimento),
                    posicaoVinculo(vinculo, data, conhecimento), posicaoAlocacao(vinculo, data, conhecimento),
                    gestao, new ConsultaProvaHistoricaContratoServico.Configuracao(
                        "Lotacao sintetica", "Turno sintetico", "Cargo sintetico", "Cidade sintetica", "{}", true));
        });
    }

    @Test
    void chunkUmNaoAvaliaCaudaAposPrimeiroElegivel() {
        cadastrarEmpregados(200);

        List<Long> resultado = buscar(0, 1);

        assertEquals(List.of(1L), resultado);
        assertEquals(List.of(1L), candidatosAvaliados,
                "Um bloco preenchido não deve reconstruir os outros 199 empregados da página.");
    }

    @Test
    void duzentosChunksUnitariosNaoReconstroemRepetidamenteACauda() {
        cadastrarEmpregados(200);
        List<Long> recebidos = new ArrayList<>();
        long cursor = 0;
        for (int bloco = 0; bloco < 200; bloco++) {
            List<Long> ids = buscar(cursor, 1);
            assertEquals(List.of((long) bloco + 1), ids);
            recebidos.addAll(ids);
            cursor = ids.get(0);
        }
        assertTrue(buscar(cursor, 1).isEmpty());

        assertEquals(intervalo(1, 200), recebidos);
        assertEquals(200, candidatosAvaliados.size(),
                "200 IDs em 200 blocos devem avaliar 200 candidatos; a cauda integral produz 20.100.");
        assertEquals(intervalo(1, 200), candidatosAvaliados);
    }

    @Test
    void ineligiveisNaoEncurtamOBlocoNemSaltamOProximoKeyset() {
        cadastrarEmpregados(8);
        for (long id : List.of(1L, 3L, 4L, 6L, 7L)) tornarNaoVigente(id);

        List<Long> primeiro = buscar(0, 2);
        List<Long> segundo = buscar(primeiro.get(primeiro.size() - 1), 2);

        assertEquals(List.of(2L, 5L), primeiro);
        assertEquals(List.of(8L), segundo);
        assertTrue(buscar(8, 2).isEmpty());
    }

    @Test
    void paginaInteiraSemElegivelNaoEncerraBuscaComPaginaValidaPosterior() {
        cadastrarEmpregados(401);
        for (long id = 1; id <= 400; id++) if (id != 201) tornarNaoVigente(id);

        assertEquals(List.of(201L, 401L), buscar(0, 2));
        assertEquals(List.of(0L, 200L, 400L), paginasLidas.stream().map(PaginaLida::apos).toList());
        assertEquals(List.of(200, 200, 1), paginasLidas.stream().map(p -> p.ids().size()).toList());
    }

    @Test
    void limite500CruzaPaginasEInterrompeSomenteOCemElegiveisRestante() {
        cadastrarEmpregados(601);

        List<Long> primeiro = buscar(0, 500);

        assertEquals(intervalo(1, 500), primeiro);
        assertEquals(intervalo(1, 500), candidatosAvaliados,
                "Depois de 200 + 200, bastam 100 elegíveis; os outros IDs continuam para o próximo bloco.");
        candidatosAvaliados.clear();
        assertEquals(intervalo(501, 601), buscar(500, 500));
        assertEquals(intervalo(501, 601), candidatosAvaliados);
    }

    @Test
    void contagemUsadaPeloTotalPrevistoPermaneceCompletaAlemDeDuzentos() {
        cadastrarEmpregados(505);

        long total = servico.contarPendentes(FECHAMENTO, DATA, LOTACAO, JORNADA, List.of(CONTRATO));

        assertEquals(505, total);
        assertEquals(intervalo(1, 505), candidatosAvaliados);
    }

    @Test
    void mesmaPessoaComDoisVinculosElegiveisContinuaConflitoMesmoComChunkUm() {
        cadastrarEmpregados(1);
        adicionarVinculo(1, 2, CONTRATO);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> buscar(0, 1));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertTrue(erro.getReason().contains("Mais de um Vínculo"));
        assertEquals(List.of(11L, 12L), vinculosAvaliados,
                "O limite externo não autoriza parar no primeiro vínculo da mesma Pessoa.");
    }

    @Test
    void contagemTotalDetectaConflitoEmPessoaDepoisDaPrimeiraPagina() {
        cadastrarEmpregados(201);
        adicionarVinculo(201, 2, CONTRATO);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.contarPendentes(FECHAMENTO, DATA, LOTACAO, JORNADA, List.of(CONTRATO)));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertTrue(erro.getReason().contains("Mais de um Vínculo"));
        assertTrue(vinculosAvaliados.containsAll(List.of(2011L, 2012L)));
    }

    @Test
    void buscaPosteriorRevalidaElegibilidadeSemReusarDecisaoDaCauda() {
        cadastrarEmpregados(3);
        assertEquals(List.of(1L), buscar(0, 1));
        tornarNaoVigente(2);
        candidatosAvaliados.clear();

        assertEquals(List.of(3L), buscar(1, 1));
        assertEquals(List.of(2L, 3L), candidatosAvaliados);
    }

    @Test
    void conflitoNovoNaCaudaApareceQuandoAPessoaChegaAoBloco() {
        cadastrarEmpregados(2);
        assertEquals(List.of(1L), buscar(0, 1));
        adicionarVinculo(2, 2, CONTRATO);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> buscar(1, 1));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertTrue(erro.getReason().contains("Mais de um Vínculo"));
        // O primeiro resultado é apenas uma seleção: este UT não declara commit.
    }

    @Test
    void contratoLotacaoETurnoContinuamFiltrosMesmoComCandidatosBrutosPresentes() {
        cadastrarEmpregados(5);
        raizes.get(11L).getContrato().setId(999L);
        lotacoesPorVinculo.put(21L, 999L);
        jornadasPorVinculo.put(31L, 999L);
        raizes.get(41L).getContrato().setId(998L);
        contratosInativos.add(998L);

        assertEquals(List.of(5L), servico.buscarPendentesApos(FECHAMENTO, DATA, LOTACAO, JORNADA,
                List.of(CONTRATO, 998L), 0, 1));
        assertTrue(buscar(5, 1).isEmpty());
    }

    @Test
    void recusaDoResolvedorHistoricoPropagaSemVirarCandidatoIgnorado() {
        cadastrarEmpregados(2);
        ResponseStatusException recusa = new ResponseStatusException(HttpStatus.CONFLICT,
                "Fonte histórica não comprovada no corte informado.");
        recusasDoResolvedor.put(11L, recusa);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> buscar(0, 1));

        assertSame(recusa, erro);
        assertEquals(List.of(1L), candidatosAvaliados);
    }

    @Test
    void novaBuscaEncaminhaDataEConhecimentoAosMotoresSemCongelarCorteAnterior() {
        cadastrarEmpregados(1);
        assertEquals(List.of(1L), buscar(0, 1));
        cortesPedidos.clear();
        dataDaBusca = DATA.minusDays(15);

        assertEquals(List.of(1L), buscar(0, 1));

        assertEquals(List.of("vinculo", "contrato", "alocacao"),
                cortesPedidos.stream().map(CortePedido::motor).toList());
        assertTrue(cortesPedidos.stream().allMatch(c -> dataDaBusca.equals(c.data())));
        assertTrue(cortesPedidos.stream().allMatch(c -> c.conhecimento() != null));
        assertEquals(1, cortesPedidos.stream().map(CortePedido::conhecimento).distinct().count(),
                "As três fronteiras diretas de contextoElegivel recebem o mesmo corte capturado nessa avaliação.");
    }

    @Test
    void paginasDaBuscaPedemTransacaoNovaSomenteLeitura() {
        cadastrarEmpregados(201);
        for (long id = 1; id <= 200; id++) tornarNaoVigente(id);

        assertEquals(List.of(201L), buscar(0, 1));

        assertEquals(2, transacoesPedidas.size());
        assertTrue(transacoesPedidas.stream().allMatch(t -> t.somenteLeitura()
                && t.propagacao() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    private List<Long> buscar(long apos, int tamanho) {
        return servico.buscarPendentesApos(FECHAMENTO, dataDaBusca, LOTACAO, JORNADA,
                List.of(CONTRATO), apos, tamanho);
    }

    private void cadastrarEmpregados(int quantidade) {
        for (long id = 1; id <= quantidade; id++) {
            Pessoa pessoa = new Pessoa();
            pessoa.setId(10_000L + id);
            Empregado empregado = new Empregado();
            empregado.setId(id);
            empregado.setPessoaCanonica(pessoa);
            empregados.put(id, empregado);
            adicionarVinculo(id, 1, CONTRATO);
        }
    }

    private void adicionarVinculo(long empregadoId, int ordinal, Long contratoId) {
        Pessoa pessoa = empregados.get(empregadoId).getPessoaCanonica();
        Contrato contrato = new Contrato();
        contrato.setId(contratoId);
        VinculoContratual vinculo = new VinculoContratual();
        vinculo.setId(empregadoId * 10 + ordinal);
        vinculo.setPessoa(pessoa);
        vinculo.setContrato(contrato);
        vinculo.setDataInicio(DATA.minusYears(1));
        raizes.put(vinculo.getId(), vinculo);
        vinculosPorPessoa.computeIfAbsent(pessoa.getId(), ignorada -> new ArrayList<>()).add(vinculo.getId());
    }

    private void tornarNaoVigente(long empregadoId) {
        vinculosNaoVigentes.add(empregadoId * 10 + 1);
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<Long> consulta(String jpql) {
        boolean paginaEmpregados = jpql.contains("from Empregado e");
        boolean vinculosPessoa = jpql.contains("from VinculoContratual v") && jpql.contains("v.pessoa.id=:p");
        assertTrue(paginaEmpregados || vinculosPessoa, "Fronteira de consulta inesperada: " + jpql);
        Map<String, Object> parametros = new HashMap<>();
        AtomicInteger maximo = new AtomicInteger(Integer.MAX_VALUE);
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(query.setParameter(anyString(), any())).thenAnswer(inv -> {
            parametros.put(inv.getArgument(0), inv.getArgument(1));
            return query;
        });
        when(query.setMaxResults(anyInt())).thenAnswer(inv -> {
            maximo.set(inv.getArgument(0));
            return query;
        });
        when(query.getResultList()).thenAnswer(inv -> {
            if (vinculosPessoa) {
                Long pessoaId = (Long) parametros.get("p");
                return List.copyOf(vinculosPorPessoa.getOrDefault(pessoaId, List.of()));
            }
            assertEquals(LOTACAO, parametros.get("lotacao"));
            assertEquals(JORNADA, parametros.get("jornada"));
            assertEquals(dataDaBusca, parametros.get("data"));
            assertEquals(FECHAMENTO, parametros.get("fechamento"));
            assertTrue(maximo.get() > 0 && maximo.get() <= 200,
                    "A fronteira deve manter páginas brutas limitadas, sem carregar o universo inteiro.");
            long apos = ((Number) parametros.get("apos")).longValue();
            List<Long> ids = empregados.tailMap(apos, false).keySet().stream().limit(maximo.get()).toList();
            paginasLidas.add(new PaginaLida(apos, ids));
            return ids;
        });
        return query;
    }

    private PosicaoVinculoTemporalDTO posicaoVinculo(VinculoContratual vinculo, LocalDate data, Instant conhecimento) {
        boolean vigente = !vinculosNaoVigentes.contains(vinculo.getId());
        return new PosicaoVinculoTemporalDTO(vinculo.getId(), vinculo.getPessoa().getId(),
                vinculo.getContrato().getId(), data, conhecimento, DATA.minusYears(1), null, null,
                EstadoNegocialVinculo.REGISTRADO,
                vigente ? SituacaoTemporalVinculo.VIGENTE : SituacaoTemporalVinculo.ENCERRADO, null);
    }

    private PosicaoTemporalContratoDTO posicaoContrato(Contrato contrato, LocalDate data, Instant conhecimento) {
        return new PosicaoTemporalContratoDTO(contrato.getId(), data, conhecimento,
                EstadoNegocialContrato.PUBLICADO, SituacaoTemporalContrato.VIGENTE,
                contratosInativos.contains(contrato.getId()) ? HabilitacaoAdministrativaContrato.INATIVO
                        : HabilitacaoAdministrativaContrato.ATIVO,
                DATA.minusYears(1), DATA.plusYears(1), null, List.of(), List.of());
    }

    private PosicaoAlocacaoTemporalDTO posicaoAlocacao(VinculoContratual vinculo, LocalDate data, Instant conhecimento) {
        return new PosicaoAlocacaoTemporalDTO(vinculo.getId(), data, conhecimento,
                SituacaoTemporalAlocacao.VIGENTE, vinculo.getId() + 100_000L, null,
                lotacoesPorVinculo.getOrDefault(vinculo.getId(), LOTACAO), "Lotacao sintetica",
                jornadasPorVinculo.getOrDefault(vinculo.getId(), JORNADA), "Turno sintetico",
                50L, "Cargo sintetico", DATA.minusYears(1), null, null);
    }

    private static List<Long> intervalo(long primeiro, long ultimo) {
        return LongStream.rangeClosed(primeiro, ultimo).boxed().toList();
    }

    private record PaginaLida(long apos, List<Long> ids) {}
    private record TransacaoPedida(int propagacao, boolean somenteLeitura) {}
    private record CortePedido(String motor, Long id, LocalDate data, Instant conhecimento) {}
}
