package com.example.siggtrat.integration;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.auditoria20261029.AuditoriaHttpSuporte;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Serviços, autorização, JPA e transações reais, com atores persistidos e sem mocks de domínio. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VinculoAlocacaoConcorrenciaIT extends AuditoriaHttpSuporte {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired VinculoContratualServico servico;
    @Autowired VinculoContratualRepositorio vinculos;
    @Autowired EventoVinculoContratualRepositorio eventos;
    private UsuarioAplicacao ator;

    private final LocalDate hoje = RelogioSistema.hoje();
    private Fixture fixture;

    @Test
    void evidenciaExcedenteSobreviveAoCommitEAOutroPersistenceContext() {
        Fixture f = preparar(1);
        inserirVinculo(f.pessoaA, f.contratoA, f.composicaoA, hoje.minusDays(2), hoje.plusDays(30));

        VinculoContratualDTO criado = servico.cadastrar(new CadastroVinculoContratualRequisicao(
                f.pessoaB, null, f.contratoA, hoje, hoje.plusDays(20), null, f.composicaoA,
                true, "Excedente autorizado no teste", "SEI-SINTETICO-123", "ATO-SINTETICO-456"));

        new TransactionTemplate(transacoes).executeWithoutResult(status -> {
            em.clear();
            List<EventoVinculoContratual> encontrados = eventos.findByVinculoIdOrderByRegistradoEmAscIdAsc(criado.id());
            assertEquals(1, encontrados.size());
            EventoVinculoContratual salvo = encontrados.get(0);
            assertTrue(salvo.isInclusaoExcedente());
            assertEquals(1, salvo.getEfetivoPrevistoExcedente());
            assertEquals(2L, salvo.getOcupacaoAposInclusaoExcedente());
            assertEquals("Excedente autorizado no teste", salvo.getJustificativa());
            assertEquals("SEI-SINTETICO-123", salvo.getEvidencia());
            assertEquals("ATO-SINTETICO-456", salvo.getAutorizacao());
            assertEquals(64, salvo.getHashEvento().length());
        });
    }

    @Test
    void retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao() throws Exception {
        Fixture f = preparar(10);
        Long a = inserirVinculo(f.pessoaA, f.contratoA, f.composicaoA, hoje.minusDays(20), hoje.plusDays(10));
        Long b = inserirVinculo(f.pessoaA, f.contratoB, f.composicaoB, hoje.plusDays(30), hoje.plusDays(60));
        var fimSemToken = desligamento(hoje.plusDays(25), f.motivo, null);
        String tokenFim = servico.previaRetificacaoDesligamento(a, fimSemToken).token();
        var inicioSemToken = inicio(hoje.plusDays(20), null);
        String tokenInicio = servico.previaRetificacaoInicio(b, inicioSemToken).token();

        List<Integer> resultados = executarEmDisputa(
                () -> servico.retificarDesligamento(a, desligamento(hoje.plusDays(25), f.motivo, tokenFim)),
                () -> servico.retificarInicio(b, inicio(hoje.plusDays(20), tokenInicio)));

        assertEquals(List.of(200, 409), resultados.stream().sorted().toList());
        verificarPeriodosSemSobreposicao(f.pessoaA);
    }

    @Test
    void cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa() throws Exception {
        Fixture f = preparar(10);
        Long a = inserirVinculo(f.pessoaA, f.contratoA, f.composicaoA, hoje.minusDays(20), hoje.plusDays(10));
        var novo = new CadastroVinculoContratualRequisicao(f.pessoaA, null, f.contratoB,
                hoje.plusDays(11), hoje.plusDays(60), null, f.composicaoB);

        List<Integer> resultados = executarEmDisputa(
                () -> servico.cancelarDesligamentoProgramado(a,
                        new AcaoVinculoContratualRequisicao("Cancelamento documentado", "SEI-IT", "ATO-IT", true, null)),
                () -> servico.cadastrar(novo));

        assertEquals(List.of(200, 409), resultados.stream().sorted().toList());
        verificarPeriodosSemSobreposicao(f.pessoaA);
    }

    private List<Integer> executarEmDisputa(Callable<?> primeira, Callable<?> segunda) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<Integer> a = executor.submit(() -> executar(primeira, prontas, iniciar));
            Future<Integer> b = executor.submit(() -> executar(segunda, prontas, iniciar));
            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Threads de concorrência devem terminar");
        }
    }

    private int executar(Callable<?> acao, CountDownLatch prontas, CountDownLatch iniciar) throws Exception {
        prontas.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) throw new AssertionError("Disputa não iniciada");
        autenticarServico();
        try {
            acao.call();
            return 200;
        } catch (ResponseStatusException ex) {
            return ex.getStatusCode().value();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void verificarPeriodosSemSobreposicao(Long pessoaId) {
        new TransactionTemplate(transacoes).executeWithoutResult(status -> {
            em.clear();
            List<VinculoContratual> encontrados = vinculos.findByPessoaIdOrderByDataInicioAscIdAsc(pessoaId);
            for (int i = 1; i < encontrados.size(); i++) {
                LocalDate fimAnterior = encontrados.get(i - 1).getDataFim();
                assertNotNull(fimAnterior, "Vínculo aberto não pode anteceder outro vínculo da mesma Pessoa");
                assertTrue(fimAnterior.isBefore(encontrados.get(i).getDataInicio()));
            }
        });
    }

    private Fixture preparar(int capacidade) {
        Cenario cenario = criarCenarioPersistido();
        ator = cenario.localA;
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        fixture = new TransactionTemplate(transacoes).execute(status -> {
            GestaoLocal gestao = em.find(GestaoLocal.class, cenario.gestaoA.getId());
            Jornada jornada = em.find(Jornada.class, cenario.jornadaA.getId());
            Lotacao lotacao = em.find(Lotacao.class, cenario.lotacaoA.getId());
            Cargo cargo = em.find(Cargo.class, cenario.cargoA.getId());
            TipoContratacao tipo = em.find(TipoContratacao.class, cenario.tipoA.getId());
            Contrato a = contrato(gestao, tipo, "IT-A-" + sufixo, capacidade);
            Contrato b = contrato(gestao, tipo, "IT-B-" + sufixo, capacidade);
            ComposicaoContratual ca = composicaoSintetica(a, lotacao, jornada, cargo, capacidade);
            ComposicaoContratual cb = composicaoSintetica(b, lotacao, jornada, cargo, capacidade);
            Pessoa pa = pessoa("Pessoa sintética A " + sufixo);
            Pessoa pb = pessoa("Pessoa sintética B " + sufixo);
            MotivoDesligamento motivo = new MotivoDesligamento(); motivo.setGestaoLocal(gestao);
            motivo.setDescricao("Desligamento individual de teste " + sufixo);
            motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL); em.persist(motivo);
            em.flush();
            return new Fixture(a.getId(), b.getId(), ca.getId(), cb.getId(), pa.getId(), pb.getId(), motivo.getId());
        });
        assertNotNull(fixture);
        autenticarServico();
        return fixture;
    }

    private void autenticarServico() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                ator.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_GESTOR_LOCAL"))));
    }

    private Contrato contrato(GestaoLocal gestao, TipoContratacao tipo, String numero, int capacidade) {
        Contrato c = new Contrato(); c.setGestaoLocal(gestao); c.setNumeroContrato(numero); c.setAnoContrato(2026);
        c.setDataInicio(hoje.minusDays(100)); c.setDataFim(hoje.plusDays(120)); c.setEfetivoPrevisto(capacidade);
        c.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO); c.setPublicadoEm(Instant.now().minusSeconds(3600));
        c.setMatriculaVinculoObrigatoria(false); c.setTipoContratacao(tipo); c.setTipo(tipo.getDescricao());
        c.setEfetivoOriginalPublicado(capacidade);
        em.persist(c);
        // O ensaio de concorrência usa um contrato com política opcional conhecida.
        // A fotografia é persistida; o serviço real consulta a mesma prova usada
        // pela aplicação, sem recorrer ao cadastro corrente nem a um mock de JPA.
        VersaoContratoNegocial original = new VersaoContratoNegocial();
        original.setContrato(c); original.setNumeroVersao(1); original.setDataEfetiva(c.getDataInicio());
        original.setNumeroContrato(c.getNumeroContrato()); original.setAnoContrato(c.getAnoContrato());
        original.setDataInicio(c.getDataInicio()); original.setDataFim(c.getDataFim());
        original.setEfetivoPrevisto(capacidade); original.setGestaoLocalId(gestao.getId());
        original.setGestaoLocalNome(gestao.getNome());
        original.setTipoContratacaoId(tipo.getId()); original.setTipoContratacaoDescricao(tipo.getDescricao());
        original.setMatriculaTipoContratacaoObrigatoria(tipo.getMatriculaVinculoObrigatoria());
        original.setMatriculaVinculoObrigatoria(false); original.setMatriculaVinculoObrigatoriaEfetiva(false);
        original.setHabilitacaoAdministrativa(HabilitacaoAdministrativaContrato.ATIVO);
        em.persist(original);
        return c;
    }

    private ComposicaoContratual composicaoSintetica(Contrato contrato, Lotacao lotacao, Jornada jornada, Cargo cargo, int capacidade) {
        ComposicaoContratual c = new ComposicaoContratual(); c.setContrato(contrato); c.setLotacao(lotacao);
        c.setJornada(jornada); c.setCargo(cargo); c.setEfetivoPrevisto(capacidade);
        c.setDataInicioVigencia(contrato.getDataInicio()); c.setDataFimVigencia(contrato.getDataFim()); em.persist(c); return c;
    }

    private Pessoa pessoa(String nome) {
        Pessoa p = new Pessoa(); p.setNome(nome);
        p.setCpf(String.format(Locale.ROOT, "%011d", ThreadLocalRandom.current().nextLong(10_000_000_000L, 99_999_999_999L)));
        em.persist(p); return p;
    }

    private Long inserirVinculo(Long pessoaId, Long contratoId, Long composicaoId, LocalDate inicio, LocalDate fim) {
        return new TransactionTemplate(transacoes).execute(status -> {
            ComposicaoContratual composicao = em.find(ComposicaoContratual.class, composicaoId);
            VinculoContratual v = new VinculoContratual(); v.setPessoa(em.find(Pessoa.class, pessoaId));
            v.setContrato(em.find(Contrato.class, contratoId)); v.setDataInicio(inicio); v.setDataFim(fim);
            v.setMotivoDesligamento(em.find(MotivoDesligamento.class, fixture.motivo)); em.persist(v);
            AlocacaoVinculo a = new AlocacaoVinculo(); a.setVinculo(v); a.setComposicao(composicao);
            a.setLotacao(composicao.getLotacao()); a.setJornada(composicao.getJornada()); a.setCargo(composicao.getCargo());
            a.setDataInicio(inicio); a.setDataFim(fim); em.persist(a); return v.getId();
        });
    }

    private DesligamentoVinculoRequisicao desligamento(LocalDate fim, Long motivo, String token) {
        return new DesligamentoVinculoRequisicao(fim, motivo, "Retificação documentada", "SEI-IT", "ATO-IT", true, token);
    }

    private RetificacaoInicioVinculoRequisicao inicio(LocalDate inicio, String token) {
        return new RetificacaoInicioVinculoRequisicao(inicio, "Retificação documentada", "SEI-IT", "ATO-IT", true, token);
    }

    // Os IDs únicos preservam a fixture confirmada para inspeção posterior no banco
    // descartável. A limpeza DML entre suítes é feita pelo roteiro de execução.
    private record Fixture(Long contratoA, Long contratoB, Long composicaoA, Long composicaoB,
                           Long pessoaA, Long pessoaB, Long motivo) {}
}
