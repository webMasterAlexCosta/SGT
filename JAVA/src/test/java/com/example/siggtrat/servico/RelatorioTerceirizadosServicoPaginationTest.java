package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.repositorio.RelatorioTerceirizadosRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioTerceirizadosServicoPaginationTest {

    @Mock
    RelatorioTerceirizadosRepositorio repositorio;

    @Mock
    AcessoOrganizacionalServico acessoOrganizacionalServico;

    private RelatorioTerceirizadosServico servico;
    private UsuarioAplicacao usuario;

    @BeforeEach
    void setup() {
        servico = new RelatorioTerceirizadosServico(repositorio, acessoOrganizacionalServico,
                new MinimizadorDadosPessoaisServico());
        usuario = mock(UsuarioAplicacao.class);
        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(usuario);
        when(acessoOrganizacionalServico.podeAcessarRelatoriosGestao(usuario)).thenReturn(true);
    }

    @Test
    void deveUsarCemComoTamanhoPadrao() {
        when(acessoOrganizacionalServico.ehAdministrador(usuario)).thenReturn(true);

        servico.consultar(null, null, null, false, null, 0, null);

        verify(repositorio).consultar(
                any(LocalDate.class),
                eq("Brasil — auditoria técnica"),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(100));
    }

    @Test
    void devePermitirPaginasGrandesAteCincoMil() {
        when(acessoOrganizacionalServico.ehAdministrador(usuario)).thenReturn(true);

        servico.consultar(null, null, null, false, null, 0, 2000);

        verify(repositorio).consultar(
                any(LocalDate.class),
                eq("Brasil — auditoria técnica"),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(2000));
    }

    @Test
    void deveLimitarUmaRequisicaoAteCincoMil() {
        when(acessoOrganizacionalServico.ehAdministrador(usuario)).thenReturn(true);

        servico.consultar(null, null, null, false, null, 0, 50000);

        verify(repositorio).consultar(
                any(LocalDate.class),
                eq("Brasil — auditoria técnica"),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(5000));
    }

    @Test
    void deveRestringirGestorEstadualAPropriaUf() {
        SuperintendenciaEstadual se = new SuperintendenciaEstadual();
        se.setId(7L);
        se.setUf("RJ");
        se.setNome("SE/RJ");
        when(acessoOrganizacionalServico.ehGestorEstadual(usuario)).thenReturn(true);
        when(acessoOrganizacionalServico.superintendenciaEfetiva(usuario)).thenReturn(se);

        servico.consultar(null, null, null, false, null, 0, 100);

        verify(repositorio).consultar(
                any(LocalDate.class), eq("SE/RJ"), eq(7L),
                isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(100));
    }
}
