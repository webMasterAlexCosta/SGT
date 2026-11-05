package com.example.siggtrat.etapa06;

import com.example.siggtrat.dominio.vinculo.*;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VinculoContratualServicoEtapa06Test {
    private VinculoContratualRepositorio vinculos;
    private ReservaMatriculaContratoRepositorio reservas;
    private EventoVinculoContratualRepositorio eventos;
    private AlocacaoVinculoRepositorio alocacoes;
    private AcessoOrganizacionalServico acesso;
    private MotorTemporalVinculoServico motor;
    private VinculoContratualServico servico;
    private UsuarioAplicacao usuario;

    @BeforeEach void preparar() {
        vinculos = mock(VinculoContratualRepositorio.class); reservas = mock(ReservaMatriculaContratoRepositorio.class);
        eventos = mock(EventoVinculoContratualRepositorio.class); alocacoes = mock(AlocacaoVinculoRepositorio.class);
        acesso = mock(AcessoOrganizacionalServico.class); motor = mock(MotorTemporalVinculoServico.class);
        ContratoRepositorio contratos = mock(ContratoRepositorio.class);
        servico = new VinculoContratualServico(vinculos, mock(PessoaServico.class), contratos,
                mock(ComposicaoContratualRepositorio.class), reservas, alocacoes, eventos,
                mock(MotivoDesligamentoRepositorio.class), acesso, motor,
                new MinimizadorDadosPessoaisServico());
        usuario = new UsuarioAplicacao(); usuario.setId(1L); usuario.setUsername("gestor");
        when(acesso.usuarioAtual()).thenReturn(usuario); doNothing().when(acesso).exigirGestaoEmpregado(any());
        doNothing().when(acesso).exigirAcessoContrato(any(), any());
        when(eventos.findTopByVinculoIdOrderByRegistradoEmDescIdDesc(anyLong())).thenReturn(Optional.empty());
        when(eventos.save(any())).thenAnswer(i -> i.getArgument(0));
        when(vinculos.save(any())).thenAnswer(i -> i.getArgument(0));
        when(alocacoes.findByVinculoIdOrderByDataInicioAscIdAsc(anyLong())).thenReturn(List.of());
        when(vinculos.buscarPorIdParaAtualizacao(anyLong())).thenAnswer(i -> vinculos.findById(i.getArgument(0)));
        when(vinculos.buscarIdentidade(anyLong())).thenAnswer(i -> vinculos.findById(i.getArgument(0))
                .map(v -> new VinculoContratualRepositorio.IdentidadeVinculo() {
                    public Long getContratoId() { return v.getContrato().getId(); }
                    public Long getPessoaId() { return v.getPessoa().getId(); }
                }));
        when(contratos.buscarPorIdParaAtualizacao(2L)).thenAnswer(i -> vinculos.findById(3L).map(VinculoContratual::getContrato));
    }

    @Test void origemCanonicaEhUsadaQuandoContratoPossuiVinculoNovo() {
        when(vinculos.existsByContratoId(10L)).thenReturn(true);
        assertTrue(servico.possuiVinculosCanonicos(10L));
    }

    @Test void alteracaoFuturaDeMatriculaEhBloqueada() {
        VinculoContratual v = vinculo(); when(vinculos.findById(3L)).thenReturn(Optional.of(v));
        var req = new AlteracaoMatriculaVinculoRequisicao(RelogioSistema.hoje().plusDays(1), "M2", "erro", "doc", "aut");
        assertThrows(ResponseStatusException.class, () -> servico.alterarMatricula(3L, req));
        verify(reservas, never()).saveAndFlush(any());
    }

    @Test void cancelamentoDeProgramacaoNaoViraDesligamento() {
        VinculoContratual v = vinculo(); v.setDataInicio(RelogioSistema.hoje().plusDays(10));
        when(vinculos.findById(3L)).thenReturn(Optional.of(v));
        when(motor.reconstruir(any(), any(), any())).thenReturn(new PosicaoVinculoTemporalDTO(
                3L, 1L, 2L, RelogioSistema.hoje(), Instant.now(), v.getDataInicio(), null, "M1",
                EstadoNegocialVinculo.CANCELADO, SituacaoTemporalVinculo.CANCELADO, null));
        VinculoContratualDTO dto = servico.cancelarProgramacao(3L,
                new AcaoVinculoContratualRequisicao("cancelar", "doc", "aut", true, null));
        assertEquals(EstadoNegocialVinculo.CANCELADO, dto.estadoNegocial());
        assertNull(v.getMotivoDesligamento());
        verify(eventos).save(argThat(e -> e.getTipoEvento() == TipoEventoVinculoContratual.CANCELAMENTO_PROGRAMACAO));
    }

    private VinculoContratual vinculo() {
        Pessoa p = new Pessoa(); p.setId(1L); p.setNome("Maria"); p.setCpf("52998224725");
        GestaoLocal g = new GestaoLocal(); g.setId(7L); g.setNome("GL");
        Contrato c = new Contrato(); c.setId(2L); c.setNumeroContrato("1"); c.setAnoContrato(2026); c.setGestaoLocal(g);
        c.setDataInicio(LocalDate.of(2026, 1, 1)); c.setDataFim(LocalDate.of(2027, 12, 31)); c.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        VinculoContratual v = new VinculoContratual(); v.setId(3L); v.setPessoa(p); v.setContrato(c);
        v.setDataInicio(LocalDate.of(2026, 1, 1)); v.setMatricula("M1"); v.setEstadoNegocial(EstadoNegocialVinculo.REGISTRADO); return v;
    }
}
