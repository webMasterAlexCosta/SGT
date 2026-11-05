package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Regressão da família COUNT que crescia uma vez por contrato na primeira leitura. */
class ContratoContagemOperacionalLoteTest {
    private final ContratoRepositorio contratos = mock(ContratoRepositorio.class);
    private final VinculoContratualRepositorio vinculos = mock(VinculoContratualRepositorio.class);
    private final AcessoOrganizacionalServico acesso = mock(AcessoOrganizacionalServico.class);
    private final UsuarioAplicacao usuario = new UsuarioAplicacao();
    private final ContratoServico servico = novoServico();

    @Test
    void listagemNacionalConservaTotaisSaldoEOrdemSemUmCountPorContrato() {
        prepararNacional();

        var resposta = servico.listarTodos();

        assertThat(resposta).extracting(d -> d.id()).containsExactly(10L, 20L, 30L);
        assertThat(resposta).extracting(d -> d.efetivoAtual()).containsExactly(4L, 0L, 2L);
        assertThat(resposta).extracting(d -> d.saldoEfetivo()).containsExactly(6L, 10L, 8L);
        assertThat(resposta).extracting(d -> d.percentualOcupacao()).containsExactly(40.0, 0.0, 20.0);
        verificarConsultaUnica(Set.of(10L, 20L, 30L));
    }

    @Test
    void opcoesConservamZerosEOrdemSemUmCountPorContrato() {
        prepararNacional();

        var resposta = servico.obterOpcoes();

        assertThat(resposta).extracting(d -> d.id()).containsExactly(10L, 20L, 30L);
        assertThat(resposta).extracting(d -> d.quantidadeColaboradores()).containsExactly(4L, 0L, 2L);
        verificarConsultaUnica(Set.of(10L, 20L, 30L));
    }

    @Test
    void contagemEmLoteRecebeSomenteContratosDoEscopoAutorizado() {
        prepararContagens();
        GestaoLocal gestao = new GestaoLocal();
        gestao.setId(77L);
        usuario.setGestaoLocal(gestao);
        when(contratos.findByGestaoLocalIdOrderByNumeroContratoAsc(77L))
                .thenReturn(List.of(contrato(30L)));

        var resposta = servico.listarTodos();

        assertThat(resposta).extracting(d -> d.id()).containsExactly(30L);
        assertThat(resposta.get(0).efetivoAtual()).isEqualTo(2L);
        verificarConsultaUnica(Set.of(30L));
    }

    @Test
    void escopoVazioNaoConsultaVinculosNemEmiteInVazio() {
        when(acesso.usuarioAtual()).thenReturn(usuario);

        assertThat(servico.listarTodos()).isEmpty();
        assertThat(servico.obterOpcoes()).isEmpty();
        verifyNoInteractions(vinculos);
    }

    private void prepararNacional() {
        prepararContagens();
        when(acesso.ehGlobal(usuario)).thenReturn(true);
        when(contratos.findAllByOrderByNumeroContratoAsc())
                .thenReturn(List.of(contrato(10L), contrato(20L), contrato(30L)));
    }

    private void prepararContagens() {
        when(acesso.usuarioAtual()).thenReturn(usuario);
        when(vinculos.countOperacionaisNoContrato(anyLong(), any()))
                .thenAnswer(i -> total(i.getArgument(0)));
        when(vinculos.contarOperacionaisPorContrato(anyCollection(), any())).thenAnswer(i -> {
            Collection<Long> ids = i.getArgument(0);
            return ids.stream().filter(id -> total(id) > 0)
                    .map(id -> new Object[]{id, total(id)}).toList();
        });
    }

    private void verificarConsultaUnica(Set<Long> ids) {
        long consultas = mockingDetails(vinculos).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("countOperacionaisNoContrato")
                        || i.getMethod().getName().equals("contarOperacionaisPorContrato"))
                .count();
        assertThat(consultas).as("A primeira leitura deve contar o conjunto autorizado em uma consulta")
                .isEqualTo(1);
        verify(vinculos).contarOperacionaisPorContrato(
                argThat(selecionados -> Set.copyOf(selecionados).equals(ids)), eq(RelogioSistema.hoje()));
        verify(vinculos, never()).countOperacionaisNoContrato(anyLong(), any());
    }

    private static long total(Long id) {
        return id.equals(10L) ? 4L : id.equals(30L) ? 2L : 0L;
    }

    private static Contrato contrato(long id) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setNumeroContrato("CONTRATO-" + id);
        contrato.setEfetivoPrevisto(10);
        contrato.setEstadoNegocial(EstadoNegocialContrato.RASCUNHO);
        return contrato;
    }

    private ContratoServico novoServico() {
        var motor = new MotorTemporalContratoServico(mock(EventoCicloVidaContratoRepositorio.class),
                mock(RelacaoPrecedenciaEventoContratoRepositorio.class),
                mock(VersaoContratoNegocialRepositorio.class), mock(AditamentoContratualRepositorio.class));
        var resultado = new ContratoServico(contratos, mock(EmpregadoRepositorio.class),
                mock(GestaoLocalRepositorio.class), mock(HistoricoVinculoContratoRepositorio.class), acesso,
                mock(TipoContratacaoRepositorio.class), mock(FornecedorRepositorio.class),
                mock(EventoCicloVidaContratoRepositorio.class), mock(ComposicaoContratualRepositorio.class),
                motor, mock(IdentidadeContratualServico.class));
        resultado.configurarVinculosCanonicos(new VinculoContratualServico(vinculos,
                mock(PessoaServico.class), contratos, mock(ComposicaoContratualRepositorio.class),
                mock(ReservaMatriculaContratoRepositorio.class), mock(AlocacaoVinculoRepositorio.class),
                mock(EventoVinculoContratualRepositorio.class), mock(MotivoDesligamentoRepositorio.class),
                acesso, mock(MotorTemporalVinculoServico.class), mock(MinimizadorDadosPessoaisServico.class)));
        return resultado;
    }
}
