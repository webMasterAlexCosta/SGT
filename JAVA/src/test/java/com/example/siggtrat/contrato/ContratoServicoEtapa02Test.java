package com.example.siggtrat.contrato;

import com.example.siggtrat.dto.AcaoCicloVidaContratoDTO;
import com.example.siggtrat.dto.ContratoDTO;
import com.example.siggtrat.dto.VersaoContratoNegocialDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.AcessoOrganizacionalServico;
import com.example.siggtrat.servico.ContratoServico;
import com.example.siggtrat.servico.AditamentoContratualServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.servico.IdentidadeContratualServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContratoServicoEtapa02Test {

    @Mock ContratoRepositorio contratoRepositorio;
    @Mock EmpregadoRepositorio empregadoRepositorio;
    @Mock GestaoLocalRepositorio gestaoLocalRepositorio;
    @Mock HistoricoVinculoContratoRepositorio historicoVinculoRepositorio;
    @Mock AcessoOrganizacionalServico acessoOrganizacionalServico;
    @Mock TipoContratacaoRepositorio tipoContratacaoRepositorio;
    @Mock FornecedorRepositorio fornecedorRepositorio;
    @Mock EventoCicloVidaContratoRepositorio eventoRepositorio;
    @Mock ComposicaoContratualRepositorio composicaoRepositorio;
    @Mock MotorTemporalContratoServico motorTemporal;

    private ContratoServico servico;
    private UsuarioAplicacao usuario;
    private GestaoLocal gestao;

    @BeforeEach
    void preparar() {
        SuperintendenciaEstadual se = new SuperintendenciaEstadual();
        se.setId(1L);
        se.setNome("RJ");
        se.setUf("RJ");
        se.setAtivo(true);

        gestao = new GestaoLocal();
        gestao.setId(2L);
        gestao.setNome("GTRAT/RJ");
        gestao.setCodigo("GTRAT-RJ");
        gestao.setAtivo(true);
        gestao.setSuperintendencia(se);

        usuario = new UsuarioAplicacao();
        usuario.setId(10L);
        usuario.setUsername("gestor.local");
        usuario.setGestaoLocal(gestao);

        servico = new ContratoServico(
                contratoRepositorio,
                empregadoRepositorio,
                gestaoLocalRepositorio,
                historicoVinculoRepositorio,
                acessoOrganizacionalServico,
                tipoContratacaoRepositorio,
                fornecedorRepositorio,
                eventoRepositorio,
                composicaoRepositorio,
                motorTemporal,
                new IdentidadeContratualServico(contratoRepositorio,
                        mock(CorrecaoIdentificadorContratoRepositorio.class), mock(ReservaIdentificadorContratoRepositorio.class))
        );
    }

    @Test
    void novoContratoCriadoPelaApiNasceComoRascunho() {
        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(usuario);
        when(acessoOrganizacionalServico.ehGestaoLocal(usuario)).thenReturn(true);
        when(contratoRepositorio.findByNumeroContratoIgnoreCaseAndAnoContrato("123", 2026))
                .thenReturn(Optional.empty());
        when(contratoRepositorio.save(any(Contrato.class))).thenAnswer(inv -> inv.getArgument(0));

        ContratoDTO resposta = servico.criar(dtoRascunho());

        assertEquals(EstadoNegocialContrato.RASCUNHO, resposta.estadoNegocial());
        assertEquals(SituacaoTemporalContrato.RASCUNHO, resposta.situacaoTemporal());
        assertEquals(2026, resposta.anoContrato());
        assertEquals("123", resposta.numeroContrato());

        verify(acessoOrganizacionalServico).exigirGestaoContrato(usuario);
    }

    @Test
    void tipoContratacaoDeOutraGestaoEhBloqueadoMesmoSemGestaoNoJson() {
        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(usuario);
        when(acessoOrganizacionalServico.ehGestaoLocal(usuario)).thenReturn(true);

        GestaoLocal outraGestao = new GestaoLocal();
        outraGestao.setId(99L);
        outraGestao.setNome("Outra Gestão");
        outraGestao.setAtivo(true);
        outraGestao.setSuperintendencia(gestao.getSuperintendencia());

        TipoContratacao tipoOutraGestao = new TipoContratacao();
        tipoOutraGestao.setId(50L);
        tipoOutraGestao.setGestaoLocal(outraGestao);
        tipoOutraGestao.setDescricao("Terceirização");
        tipoOutraGestao.setAtivo(true);

        when(tipoContratacaoRepositorio.findById(50L)).thenReturn(Optional.of(tipoOutraGestao));

        ContratoDTO dto = dtoRascunhoComTipo(50L, null);

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> servico.criar(dto)
        );

        assertEquals(400, erro.getStatusCode().value());
        assertTrue(erro.getReason().contains("não pertence à Gestão Local"));
        verify(contratoRepositorio, never()).save(any());
    }

    @Test
    void inativacaoFuturaRegistraFatoSemMudarProjecaoAtualAntesDaData() {
        Contrato contrato = contratoPublicado();
        LocalDate hoje = com.example.siggtrat.utilitario.RelogioSistema.hoje();
        LocalDate dataFutura = hoje.plusDays(10);

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(usuario);
        when(contratoRepositorio.buscarPorIdParaAtualizacao(20L)).thenReturn(Optional.of(contrato));
        when(motorTemporal.habilitacaoAdministrativaEm(eq(contrato), any(), any()))
                .thenReturn(HabilitacaoAdministrativaContrato.ATIVO);

        ContratoDTO resposta = servico.inativarAdministrativamente(
                20L,
                new AcaoCicloVidaContratoDTO(
                        dataFutura,
                        hoje,
                        "Bloqueio operacional programado",
                        "SEI-456"
                )
        );

        assertTrue(contrato.getAtivo(), "A projeção atual não pode ser antecipada.");
        assertTrue(resposta.ativo());

        verify(motorTemporal).registrarEvento(eq(contrato),
                eq(TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA),
                argThat(acao -> dataFutura.equals(acao.dataEfetiva())), eq(usuario),
                eq("ATIVO"), eq("INATIVO"));
        verify(contratoRepositorio, never()).save(any());
    }

    @Test
    void contratoPublicadoNaoVoltaAEdicaoLivre() {
        Contrato contrato = contratoPublicado();
        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(usuario);
        when(contratoRepositorio.buscarPorIdParaAtualizacao(20L)).thenReturn(Optional.of(contrato));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> servico.atualizar(20L, dtoRascunho())
        );

        assertEquals(409, erro.getStatusCode().value());
        verify(contratoRepositorio, never()).save(any());
    }

    @Test
    void cancelamentoDeContratoFuturoPreservaEventoEAlteraEstadoExplicito() {
        Contrato contrato = contratoPublicado();
        contrato.setDataInicio(LocalDate.now().plusDays(10));
        contrato.setDataFim(LocalDate.now().plusMonths(3));

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(usuario);
        when(contratoRepositorio.buscarPorIdParaAtualizacao(20L)).thenReturn(Optional.of(contrato));
        when(contratoRepositorio.save(any(Contrato.class))).thenAnswer(inv -> inv.getArgument(0));
        AditamentoContratualServico projecao = mock(AditamentoContratualServico.class);
        servico.configurarAditamentoTemporal(projecao);
        when(projecao.projetarVersaoContrato(eq(contrato), any(), any(), any())).thenReturn(
                new VersaoContratoNegocialDTO(null, 1, null, contrato.getDataInicio(), null,
                        contrato.getNumeroContrato(), contrato.getAnoContrato(), contrato.getDescricao(),
                        null, null, null, null, null, null, contrato.getDataInicio(), contrato.getDataFim(), 100,
                        gestao.getId(), gestao.getNome(), null, null, null, null, null, null, null));

        ContratoDTO resposta = servico.cancelarContratoFuturo(
                20L,
                new AcaoCicloVidaContratoDTO(
                        LocalDate.now(),
                        LocalDate.now(),
                        "Cancelamento formal antes do início",
                        "SEI-123"
                )
        );

        assertEquals(EstadoNegocialContrato.CANCELADO, resposta.estadoNegocial());
        assertEquals(SituacaoTemporalContrato.CANCELADO, resposta.situacaoTemporal());
        assertFalse(resposta.ativo());

        verify(motorTemporal).registrarEvento(eq(contrato),
                eq(TipoEventoCicloVidaContrato.CANCELAMENTO_CONTRATO_FUTURO),
                any(), eq(usuario), eq("PUBLICADO"), eq("CANCELADO"));
    }

    private Contrato contratoPublicado() {
        Contrato contrato = new Contrato();
        contrato.setId(20L);
        contrato.setGestaoLocal(gestao);
        contrato.setNumeroContrato("123");
        contrato.setAnoContrato(2026);
        contrato.setDescricao("Contrato teste");
        contrato.setDataInicio(LocalDate.of(2026, 1, 1));
        contrato.setDataFim(LocalDate.of(2026, 12, 31));
        contrato.setAtivo(true);
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        return contrato;
    }

    private ContratoDTO dtoRascunho() {
        return dtoRascunhoComTipo(null, 2L);
    }

    private ContratoDTO dtoRascunhoComTipo(Long tipoContratacaoId, Long gestaoLocalId) {
        return new ContratoDTO(
                null,
                "123",
                "Contrato teste",
                null,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2027, 9, 30),
                true,
                null,
                null,
                gestaoLocalId,
                null,
                null,
                null,
                null,
                100,
                null,
                null,
                null,
                2026,
                tipoContratacaoId,
                null,
                null,
                "12.345.678/0001-90",
                "EMPRESA TESTE LTDA",
                "EMPRESA TESTE",
                EstadoNegocialContrato.RASCUNHO,
                SituacaoTemporalContrato.RASCUNHO,
                HabilitacaoAdministrativaContrato.ATIVO,
                null,
                null,
                null,
                null,
                null,
                "SEI-TESTE",
                0L
        );
    }
}
