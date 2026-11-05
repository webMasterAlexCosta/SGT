package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.ContratoOpcaoDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ContratoControladorOpcoesTest {

    @Mock
    private ContratoRepositorio contratoRepositorio;
    @Mock
    private EmpregadoRepositorio empregadoRepositorio;
    @Mock
    private GestaoLocalRepositorio gestaoLocalRepositorio;
    @Mock
    private HistoricoVinculoContratoRepositorio historicoVinculoRepositorio;
    @Mock
    private AcessoOrganizacionalServico acessoOrganizacionalServico;

    @Test
    void opcoesTrazemTotalCanonicoDeVinculosSemDependerDaPaginaDeColaboradores() {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        Contrato primeiro = contrato(10L, "TERC-10", "Primeiro contrato");
        Contrato segundo = contrato(20L, "TERC-20", "Segundo contrato");

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(usuario);
        when(acessoOrganizacionalServico.ehGlobal(usuario)).thenReturn(true);
        when(contratoRepositorio.findAllByOrderByNumeroContratoAsc())
                .thenReturn(List.of(primeiro, segundo));
        VinculoContratualServico vinculos = mock(VinculoContratualServico.class);
        when(vinculos.contarOperacionaisPorContrato(anyCollection(), any()))
                .thenReturn(Map.of(10L, 144L));

        ContratoServico servico = new ContratoServico(
                contratoRepositorio,
                empregadoRepositorio,
                gestaoLocalRepositorio,
                historicoVinculoRepositorio,
                acessoOrganizacionalServico,
                mock(TipoContratacaoRepositorio.class),
                mock(FornecedorRepositorio.class),
                mock(EventoCicloVidaContratoRepositorio.class),
                mock(ComposicaoContratualRepositorio.class),
                mock(MotorTemporalContratoServico.class),
                mock(IdentidadeContratualServico.class)
        );
        servico.configurarVinculosCanonicos(vinculos);

        List<ContratoOpcaoDTO> opcoes = servico.obterOpcoes();

        assertEquals(2, opcoes.size());
        assertEquals(144L, opcoes.get(0).quantidadeColaboradores());
        assertEquals(0L, opcoes.get(1).quantidadeColaboradores());
        verify(acessoOrganizacionalServico).exigirConsultaContrato(usuario);
    }

    @Test
    void presidenteRecebeOpcoesDeContratosDeTodoOPais() {
        UsuarioAplicacao presidente = new UsuarioAplicacao();
        Contrato rj = contrato(10L, "RJ-10", "Contrato RJ");
        Contrato sp = contrato(20L, "SP-20", "Contrato SP");

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(presidente);
        when(acessoOrganizacionalServico.ehGlobal(presidente)).thenReturn(false);
        when(acessoOrganizacionalServico.ehPresidente(presidente)).thenReturn(true);
        when(contratoRepositorio.findAllByOrderByNumeroContratoAsc()).thenReturn(List.of(rj, sp));
        ContratoServico servico = new ContratoServico(
                contratoRepositorio,
                empregadoRepositorio,
                gestaoLocalRepositorio,
                historicoVinculoRepositorio,
                acessoOrganizacionalServico,
                mock(TipoContratacaoRepositorio.class),
                mock(FornecedorRepositorio.class),
                mock(EventoCicloVidaContratoRepositorio.class),
                mock(ComposicaoContratualRepositorio.class),
                mock(MotorTemporalContratoServico.class),
                mock(IdentidadeContratualServico.class)
        );

        List<ContratoOpcaoDTO> opcoes = servico.obterOpcoes();

        assertEquals(2, opcoes.size());
        verify(contratoRepositorio).findAllByOrderByNumeroContratoAsc();
        verify(acessoOrganizacionalServico).exigirConsultaContrato(presidente);
    }

    private static Contrato contrato(Long id, String numero, String descricao) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setNumeroContrato(numero);
        contrato.setDescricao(descricao);
        contrato.setAtivo(true);
        return contrato;
    }
}
