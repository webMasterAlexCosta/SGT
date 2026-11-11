package com.example.siggtrat.frequencia;

import com.example.siggtrat.configuracao.propriedades.PropriedadesFechamentoFrequencia;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.RequisicaoCriacaoFechamentoFrequencia;
import com.example.siggtrat.dto.FechamentoFrequenciaDTO;
import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.evento.EventoFechamentoFrequenciaSolicitado;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.FechamentoFrequenciaServico;
import com.example.siggtrat.servico.ElegibilidadeFechamentoFrequenciaServico;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import com.example.siggtrat.servico.AcessoOrganizacionalServico;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FechamentoFrequenciaServicoTest {

    @Mock private FechamentoFrequenciaRepositorio repositorio;
    @Mock private FechamentoFrequenciaItemRepositorio itemRepositorio;
    @Mock private EmpregadoRepositorio empregadoRepositorio;
    @Mock private LotacaoRepositorio lotacaoRepositorio;
    @Mock private JornadaRepositorio jornadaRepositorio;
    @Mock private ContratoRepositorio contratoRepositorio;
    @Mock private AcessoOrganizacionalServico acessoOrganizacionalServico;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ElegibilidadeFechamentoFrequenciaServico elegibilidade;
    @Mock private GestaoContratoTemporalServico gestaoContratoTemporal;

    private FechamentoFrequenciaServico servico;
    private UsuarioAplicacao user;
    private Lotacao lotacao;
    private Jornada jornada;

    @BeforeEach
    void setUp() {
        var propriedades = new PropriedadesFechamentoFrequencia(500, 2, 1, 1, 10, 10_000, 120);
        var politicaTemporal = new PoliticaTemporalFrequenciaServico(7);
        servico = new FechamentoFrequenciaServico(
                repositorio, itemRepositorio, empregadoRepositorio, lotacaoRepositorio, jornadaRepositorio,
                contratoRepositorio, acessoOrganizacionalServico, eventPublisher, propriedades, politicaTemporal, elegibilidade,
                gestaoContratoTemporal);

        user = new UsuarioAplicacao();
        user.setId(99L);
        GestaoLocal gestaoLocal = new GestaoLocal();
        gestaoLocal.setId(7L);
        lotacao = new Lotacao();
        lotacao.setId(10L);
        lotacao.setNome("CTE BENFICA");
        lotacao.setGestaoLocal(gestaoLocal);
        jornada = new Jornada();
        jornada.setId(20L);
        jornada.setDescricao("TURNO A");
        jornada.setGestaoLocal(gestaoLocal);

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(user);
        when(acessoOrganizacionalServico.ehAdministrador(user)).thenReturn(true);
        when(acessoOrganizacionalServico.podeLancarFrequenciaDiaria(user, lotacao, jornada)).thenReturn(true);
        when(lotacaoRepositorio.findById(10L)).thenReturn(Optional.of(lotacao));
        when(jornadaRepositorio.findById(20L)).thenReturn(Optional.of(jornada));
        when(jornadaRepositorio.findByLotacoes_Id(10L)).thenReturn(List.of(jornada));
        when(repositorio.buscarPorChaveEscopoParaAtualizacao(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void lotacaoInexistenteRetorna404() {
        when(lotacaoRepositorio.findById(10L)).thenReturn(Optional.empty());
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criarOuReiniciar(request(List.of())));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    @Test
    void cliqueDuplicadoEmTrabalhoAtivoEhIdempotente() {
        FechamentoFrequencia existente = fechamento(FechamentoFrequenciaStatus.PROCESSANDO);
        when(repositorio.buscarPorChaveEscopoParaAtualizacao(anyString())).thenReturn(Optional.of(existente));

        var dto = servico.criarOuReiniciar(request(List.of()));

        assertEquals(existente.getId(), dto.id());
        verify(repositorio, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void excessoDeAlteracoesRetorna413() {
        List<FrequenciaDTO> alteracoes = new ArrayList<>();
        alteracoes.add(dto(1L, "FALTA", null));
        alteracoes.add(dto(2L, "FALTA", null));
        alteracoes.add(dto(3L, "FALTA", null));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criarOuReiniciar(request(alteracoes)));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, erro.getStatusCode());
    }

    @Test
    void alteracaoDuplicadaParaMesmoEmpregadoEhRejeitada() {
        List<FrequenciaDTO> alteracoes = List.of(dto(1L, "FALTA", null), dto(1L, "ATESTADO", "ok"));
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criarOuReiniciar(request(alteracoes)));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        assertTrue(erro.getReason().contains("mais de uma alteração"));
    }

    @Test
    void atestadoSemJustificativaEhRejeitado() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criarOuReiniciar(request(List.of(dto(1L, "ATESTADO", "  ")))));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        assertTrue(erro.getReason().contains("Atestado exige justificativa"));
    }

    @Test
    void statusDesconhecidoEhRejeitado() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criarOuReiniciar(request(List.of(dto(1L, "TESTE", null)))));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void escopoSemElegiveisEhRejeitadoRapidamente() {
        when(elegibilidade.existe(any(), anyLong(), anyLong(), anyList())).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.criarOuReiniciar(request(List.of())));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        assertTrue(erro.getReason().contains("Nenhum colaborador elegível"));
    }

    @Test
    void criaFechamentoEPublicaEventoDepoisDePersistir() {
        when(elegibilidade.existe(any(), anyLong(), anyLong(), anyList())).thenReturn(true);
        when(elegibilidade.contarExplicitos(anyCollection(), any(), anyLong(), anyLong(), anyList())).thenReturn(1L);
        Empregado empregado = new Empregado();
        empregado.setId(1L);
        empregado.setNome("ANA TESTE");
        when(empregadoRepositorio.getReferenceById(1L)).thenReturn(empregado);
        when(repositorio.saveAndFlush(any())).thenAnswer(invocation -> {
            FechamentoFrequencia f = invocation.getArgument(0);
            f.setId(55L);
            return f;
        });

        var resposta = servico.criarOuReiniciar(request(List.of(dto(1L, " falta ", null))));

        assertEquals(55L, resposta.id());
        assertEquals("PENDENTE", resposta.status());
        assertEquals(1, resposta.totalAlteracoes());
        ArgumentCaptor<List<FechamentoFrequenciaItem>> itens = ArgumentCaptor.forClass(List.class);
        verify(itemRepositorio).saveAll(itens.capture());
        assertEquals("FALTA", itens.getValue().get(0).getStatus());
        verify(eventPublisher).publishEvent(new EventoFechamentoFrequenciaSolicitado(55L));
    }

    @Test
    void usuarioOperacionalComVinculoPodeRealizarFrequencia() {
        user.setPerfil(com.example.siggtrat.dominio.acesso.PerfilAcesso.valueOf((ConstantesDominio.PERFIL_OPERACIONAL).replace("PERFIL_", "")));
        when(acessoOrganizacionalServico.ehAdministrador(user)).thenReturn(false);
        when(acessoOrganizacionalServico.ehGestaoLocal(user)).thenReturn(false);
        when(acessoOrganizacionalServico.ehOperacional(user)).thenReturn(true);
        when(acessoOrganizacionalServico.podeLancarFrequenciaDiaria(user, lotacao, jornada)).thenReturn(true);
        when(elegibilidade.existe(any(), anyLong(), anyLong(), anyList())).thenReturn(true);
        when(elegibilidade.contarExplicitos(anyCollection(), any(), anyLong(), anyLong(), anyList())).thenReturn(1L);
        when(repositorio.saveAndFlush(any())).thenAnswer(invocation -> {
            FechamentoFrequencia fechamento = invocation.getArgument(0);
            fechamento.setId(88L);
            return fechamento;
        });

        FechamentoFrequenciaDTO resposta = servico.criarOuReiniciar(request(List.of()));

        assertEquals(88L, resposta.id());
        assertEquals("PENDENTE", resposta.status());
        verify(acessoOrganizacionalServico).podeLancarFrequenciaDiaria(user, lotacao, jornada);
    }

    @Test
    void fechamentoConcluidoSemPendenciasNemAlteracoesEhRetornadoSemNovoJob() {
        FechamentoFrequencia existente = fechamento(FechamentoFrequenciaStatus.CONCLUIDO);
        when(repositorio.buscarPorChaveEscopoParaAtualizacao(anyString())).thenReturn(Optional.of(existente));
        when(elegibilidade.contarSemFrequencia(any(), anyLong(), anyLong(), anyList())).thenReturn(0L);

        var resposta = servico.criarOuReiniciar(request(List.of()));

        assertEquals(existente.getId(), resposta.id());
        verify(repositorio, never()).saveAndFlush(any());
    }

    private RequisicaoCriacaoFechamentoFrequencia request(List<FrequenciaDTO> alteracoes) {
        LocalDate dataValida = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1);
        return new RequisicaoCriacaoFechamentoFrequencia(dataValida, 10L, 20L, List.of(), alteracoes);
    }

    private FrequenciaDTO dto(Long empregadoId, String status, String observacao) {
        FrequenciaDTO dto = new FrequenciaDTO();
        dto.setEmpregado(new FrequenciaDTO.EmpregadoResumoDTO(empregadoId, "Empregado " + empregadoId));
        dto.setStatus(status);
        dto.setObservacao(observacao);
        return dto;
    }

    private FechamentoFrequencia fechamento(FechamentoFrequenciaStatus status) {
        FechamentoFrequencia f = new FechamentoFrequencia();
        f.setId(77L);
        f.setStatus(status);
        f.setDataReferencia(LocalDate.of(2026, 8, 6));
        f.setLotacao(lotacao);
        f.setJornada(jornada);
        f.setUsuarioCriador(user);
        f.setContratoIds("");
        f.setMensagem("ok");
        return f;
    }
}
