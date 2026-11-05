package com.example.siggtrat.etapa07;

import com.example.siggtrat.dominio.alocacao.*;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AlocacaoVinculoServicoEtapa07Test {
    private AlocacaoVinculoRepositorio alocacoes;
    private EventoAlocacaoVinculoRepositorio eventos;
    private VinculoContratualRepositorio vinculos;
    private ComposicaoContratualRepositorio composicoes;
    private ComposicaoContratualTemporalServico temporal;
    private AlocacaoVinculoServico servico;
    private final List<AlocacaoVinculo> linhas = new ArrayList<>();
    private final List<EventoAlocacaoVinculo> eventosSalvos = new ArrayList<>();
    private final List<ComposicaoContratual> composicoesDoCenario = new ArrayList<>();
    private VinculoContratual vinculo;
    private UsuarioAplicacao usuario;

    @BeforeEach
    void preparar() {
        alocacoes = mock(AlocacaoVinculoRepositorio.class);
        eventos = mock(EventoAlocacaoVinculoRepositorio.class);
        vinculos = mock(VinculoContratualRepositorio.class);
        composicoes = mock(ComposicaoContratualRepositorio.class);
        temporal = mock(ComposicaoContratualTemporalServico.class);
        AcessoOrganizacionalServico acesso = mock(AcessoOrganizacionalServico.class);
        usuario = new UsuarioAplicacao(); usuario.setId(99L); usuario.setUsername("gestor-local");
        when(acesso.usuarioAtual()).thenReturn(usuario);
        doNothing().when(acesso).exigirGestaoEmpregado(any());
        doNothing().when(acesso).exigirAcessoContrato(any(), any());
        MotorTemporalAlocacaoServico motor = mock(MotorTemporalAlocacaoServico.class);
        ContratoRepositorio contratos = mock(ContratoRepositorio.class);
        servico = new AlocacaoVinculoServico(alocacoes, eventos, vinculos, contratos,
                composicoes, temporal, acesso, motor);

        vinculo = vinculo(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        when(vinculos.findById(7L)).thenReturn(Optional.of(vinculo));
        when(vinculos.buscarPorIdParaAtualizacao(7L)).thenAnswer(i -> Optional.of(vinculo));
        when(vinculos.buscarIdentidade(7L)).thenAnswer(i -> Optional.of(new VinculoContratualRepositorio.IdentidadeVinculo() {
            public Long getContratoId() { return vinculo.getContrato().getId(); }
            public Long getPessoaId() { return vinculo.getPessoa().getId(); }
        }));
        when(contratos.buscarPorIdParaAtualizacao(5L)).thenAnswer(i -> Optional.of(vinculo.getContrato()));
        when(alocacoes.findByVinculoIdAndEstadoNegocialOrderByDataInicioAscIdAsc(eq(7L), eq(EstadoNegocialAlocacao.REGISTRADA)))
                .thenAnswer(inv -> linhas.stream().filter(a -> a.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA)
                        .sorted(Comparator.comparing(AlocacaoVinculo::getDataInicio)).toList());
        when(alocacoes.findByVinculoIdOrderByDataInicioAscIdAsc(7L)).thenAnswer(inv -> List.copyOf(linhas));
        when(alocacoes.save(any(AlocacaoVinculo.class))).thenAnswer(inv -> {
            AlocacaoVinculo valor = inv.getArgument(0);
            if (!linhas.contains(valor)) linhas.add(valor);
            return valor;
        });
        when(eventos.findTopByVinculoIdOrderByRegistradoEmDescIdDesc(7L)).thenReturn(Optional.empty());
        when(eventos.save(any(EventoAlocacaoVinculo.class))).thenAnswer(inv -> {
            EventoAlocacaoVinculo valor = inv.getArgument(0); eventosSalvos.add(valor); return valor;
        });
        // A cobertura valida também as faixas preservadas: o cenário deve conter
        // as composições de origem e destino, não apenas a última solicitada.
        when(temporal.projetarEntidades(any(), any(), any())).thenAnswer(inv -> {
            Contrato contrato = inv.getArgument(0);
            LocalDate data = inv.getArgument(1);
            return composicoesDoCenario.stream()
                    .filter(c -> Objects.equals(c.getContrato().getId(), contrato.getId()))
                    .filter(c -> !data.isBefore(c.getDataInicioVigencia()))
                    .filter(c -> c.getDataFimVigencia() == null || !data.isAfter(c.getDataFimVigencia()))
                    .map(this::projecao).toList();
        });
        when(temporal.localizar(anyList(), any(), any(), any(), any())).thenCallRealMethod();
        when(composicoes.findById(anyLong())).thenAnswer(inv -> composicoesDoCenario.stream()
                .filter(c -> Objects.equals(c.getId(), inv.getArgument(0))).findFirst());
    }

    @Test
    void mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento() {
        ComposicaoContratual origem = composicao(1L, "A", "T1", "Cargo A");
        ComposicaoContratual destino = composicao(2L, "B", "T2", "Cargo B");
        linhas.add(faixa(origem, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)));

        AlteracaoAlocacaoVinculoRequisicao base = requisicao(LocalDate.of(2026, 6, 10), 2L, null);
        PreviaImpactoAlocacaoDTO previa = servico.previaAlteracao(7L, base);
        AlocacaoVinculoDTO resultado = servico.alterar(7L, requisicao(LocalDate.of(2026, 6, 10), 2L, previa.token()));

        assertEquals(LocalDate.of(2026, 6, 10), resultado.dataInicio());
        assertEquals(LocalDate.of(2026, 6, 9), linhas.get(0).getDataFim());
        assertEquals(EstadoNegocialAlocacao.REGISTRADA, linhas.get(0).getEstadoNegocial());
        assertEquals(EstadoNegocialVinculo.REGISTRADO, vinculo.getEstadoNegocial());
        assertEquals(2, linhas.size());
        assertTrue(eventosSalvos.stream().anyMatch(e -> e.getTipoEvento() == TipoEventoAlocacaoVinculo.ALTERACAO));
    }

    @Test
    void mesmaDataRetificaFaixaEmVezDeCriarDuasAlocacoesEfetivas() {
        ComposicaoContratual primeira = composicao(2L, "B", "T2", "Cargo B");
        ComposicaoContratual corrigida = composicao(3L, "C", "T3", "Cargo C");
        linhas.add(faixa(composicao(1L, "A", "T1", "Cargo A"), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 9)));
        AlocacaoVinculo alvo = faixa(primeira, LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 30)); linhas.add(alvo);
        when(alocacoes.findById(22L)).thenReturn(Optional.of(alvo));

        AlteracaoAlocacaoVinculoRequisicao base = requisicao(LocalDate.of(2026, 6, 10), 3L, null);
        String token = servico.previaAlteracao(7L, base).token();
        AlocacaoVinculoDTO resultado = servico.retificar(7L, 22L, requisicao(LocalDate.of(2026, 6, 10), 3L, token));

        assertEquals(EstadoNegocialAlocacao.SUBSTITUIDA, alvo.getEstadoNegocial());
        assertEquals(LocalDate.of(2026, 6, 10), resultado.dataInicio());
        assertEquals(2, linhas.stream().filter(a -> a.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA).count());
        assertTrue(eventosSalvos.stream().anyMatch(e -> e.getTipoEvento() == TipoEventoAlocacaoVinculo.RETIFICACAO));
    }

    @Test
    void cancelamentoDeProgramacaoRestauraFaixaAnteriorSemLacuna() {
        LocalDate hoje = RelogioSistema.hoje();
        vinculo = vinculo(hoje.minusDays(5), hoje.plusDays(30));
        when(vinculos.findById(7L)).thenReturn(Optional.of(vinculo));
        ComposicaoContratual origem = composicao(1L, "A", "T1", "Cargo A");
        ComposicaoContratual futura = composicao(2L, "B", "T2", "Cargo B");
        LocalDate inicioFuturo = hoje.plusDays(10);
        AlocacaoVinculo anterior = faixa(origem, vinculo.getDataInicio(), inicioFuturo.minusDays(1));
        AlocacaoVinculo programada = faixa(futura, inicioFuturo, vinculo.getDataFim());
        linhas.add(anterior); linhas.add(programada);
        when(alocacoes.findById(44L)).thenReturn(Optional.of(programada));

        AlocacaoVinculoDTO cancelada = servico.cancelarProgramacao(7L, 44L,
                new AcaoVinculoContratualRequisicao("cancelar", "SEI", "ato", true, null));

        assertEquals(EstadoNegocialAlocacao.CANCELADA, cancelada.estadoNegocial());
        assertEquals(vinculo.getDataFim(), anterior.getDataFim());
        assertEquals(1, linhas.stream().filter(a -> a.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA).count());
        assertTrue(eventosSalvos.stream().anyMatch(e -> e.getTipoEvento() == TipoEventoAlocacaoVinculo.CANCELAMENTO_PROGRAMACAO));
    }

    @Test
    void faixaSemCoberturaNaDataEfetivaEhBloqueada() {
        ComposicaoContratual origem = composicao(1L, "A", "T1", "Cargo A");
        ComposicaoContratual destino = composicao(2L, "B", "T2", "Cargo B");
        linhas.add(faixa(origem, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5)));

        AlteracaoAlocacaoVinculoRequisicao base = requisicao(LocalDate.of(2026, 6, 10), 2L, null);
        String token = servico.previaAlteracao(7L, base).token();
        assertThrows(ResponseStatusException.class,
                () -> servico.alterar(7L, requisicao(LocalDate.of(2026, 6, 10), 2L, token)));
    }

    @Test
    void reativacaoBloqueiaAlocacaoCujaComposicaoNaoEhMaisValida() {
        ComposicaoContratual origem = composicao(1L, "A", "T1", "Cargo A");
        AlocacaoVinculo alocacao = faixa(origem, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        linhas.add(alocacao);
        when(alocacoes.findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(5L)).thenReturn(List.of(alocacao));
        when(temporal.localizar(anyList(), any(), any(), any(), any())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> servico.validarRetomadaOperacional(vinculo.getContrato(), LocalDate.of(2026, 6, 15)));
    }

    @Test
    void capacidadeNaDataEfetivaExplicitaInclusaoExcedente() {
        ComposicaoContratual composicao = composicao(8L, "A", "T1", "Cargo A");
        var p = projecao(composicao);
        doReturn(List.of(p)).when(temporal).projetarEntidades(any(), eq(LocalDate.of(2026, 6, 15)), any());
        when(alocacoes.countOcupacao(eq(5L), anyLong(), anyLong(), anyLong(), eq(LocalDate.of(2026, 6, 15))))
                .thenReturn(1L);

        var capacidade = servico.capacidadeInclusao(vinculo.getContrato(), composicao, LocalDate.of(2026, 6, 15));

        assertEquals(1, capacidade.efetivoPrevisto());
        assertEquals(2, capacidade.ocupacaoAposInclusao());
        assertEquals(1, capacidade.excedenteAposInclusao());
        assertTrue(capacidade.excede());
    }

    @Test
    void ultimoDiaExigePrecedenciaExplicita() {
        ComposicaoContratual origem = composicao(1L, "A", "T1", "Cargo A");
        ComposicaoContratual destino = composicao(2L, "B", "T2", "Cargo B");
        linhas.add(faixa(origem, vinculo.getDataInicio(), vinculo.getDataFim()));
        AlteracaoAlocacaoVinculoRequisicao base = requisicao(vinculo.getDataFim(), 2L, null);
        String token = servico.previaAlteracao(7L, base).token();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.alterar(7L, requisicao(vinculo.getDataFim(), 2L, token)));
        assertTrue(erro.getReason().contains("confirmarPrecedenciaUltimoDia"));

        AlteracaoAlocacaoVinculoRequisicao confirmada = new AlteracaoAlocacaoVinculoRequisicao(
                vinculo.getDataFim(), 2L, null, null, null, "movimentação", "SEI-123",
                "ato administrativo", true, token, true);
        servico.alterar(7L, confirmada);
        assertTrue(eventosSalvos.stream().anyMatch(e -> e.getJustificativa() != null
                && e.getJustificativa().startsWith("[PRECEDENCIA:")));
    }

    private AlteracaoAlocacaoVinculoRequisicao requisicao(LocalDate data, Long composicaoId, String token) {
        return new AlteracaoAlocacaoVinculoRequisicao(data, composicaoId, null, null, null,
                "movimentação", "SEI-123", "ato administrativo", true, token);
    }

    private VinculoContratual vinculo(LocalDate inicio, LocalDate fim) {
        GestaoLocal gestao = gestao(1L, "GL");
        Contrato contrato = new Contrato(); contrato.setId(5L); contrato.setGestaoLocal(gestao);
        contrato.setDataInicio(inicio); contrato.setDataFim(fim); contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setAtivo(true);
        Pessoa pessoa = new Pessoa(); pessoa.setId(4L); pessoa.setNome("Maria"); pessoa.setCpf("52998224725");
        VinculoContratual valor = new VinculoContratual(); valor.setId(7L); valor.setPessoa(pessoa); valor.setContrato(contrato);
        valor.setDataInicio(inicio); valor.setDataFim(fim); valor.setEstadoNegocial(EstadoNegocialVinculo.REGISTRADO); return valor;
    }

    private ComposicaoContratual composicao(Long id, String lotacaoNome, String turno, String cargoNome) {
        GestaoLocal gestao = vinculo.getContrato().getGestaoLocal();
        Jornada jornada = new Jornada(); jornada.setId(id + 100); jornada.setDescricao(turno); jornada.setGestaoLocal(gestao); jornada.setAtivo(true);
        Lotacao lotacao = new Lotacao(); lotacao.setId(id + 200); lotacao.setNome(lotacaoNome); lotacao.setGestaoLocal(gestao); lotacao.setAtivo(true); lotacao.setTurnos(List.of(jornada));
        Cargo cargo = new Cargo(); cargo.setId(id + 300); cargo.setNome(cargoNome); cargo.setGestaoLocal(gestao); cargo.setAtivo(true);
        ComposicaoContratual c = new ComposicaoContratual(); c.setId(id); c.setContrato(vinculo.getContrato()); c.setLotacao(lotacao);
        c.setJornada(jornada); c.setCargo(cargo); c.setEfetivoPrevisto(1); c.setDataInicioVigencia(vinculo.getDataInicio());
        c.setDataFimVigencia(vinculo.getDataFim()); composicoesDoCenario.add(c); return c;
    }

    private AlocacaoVinculo faixa(ComposicaoContratual composicao, LocalDate inicio, LocalDate fim) {
        AlocacaoVinculo a = new AlocacaoVinculo(); a.setVinculo(vinculo); a.setComposicao(composicao); a.setLotacao(composicao.getLotacao());
        a.setJornada(composicao.getJornada()); a.setCargo(composicao.getCargo()); a.setDataInicio(inicio); a.setDataFim(fim);
        a.setEstadoNegocial(EstadoNegocialAlocacao.REGISTRADA); return a;
    }

    private ComposicaoContratualTemporalServico.ComposicaoProjetada projecao(ComposicaoContratual c) {
        return new ComposicaoContratualTemporalServico.ComposicaoProjetada(c.getId(), c.getLotacao(), c.getJornada(), c.getCargo(),
                c.getLotacao().getNome(), c.getJornada().getDescricao(), c.getCargo().getNome(),
                c.getEfetivoPrevisto(), c.getDataInicioVigencia(), c.getDataFimVigencia(),
                c.getLotacaoMunicipioFotografia(), c.getJornadaSemanalFotografia());
    }

    private GestaoLocal gestao(Long id, String nome) {
        GestaoLocal g = new GestaoLocal(); g.setId(id); g.setNome(nome); g.setAtivo(true); return g;
    }
}
