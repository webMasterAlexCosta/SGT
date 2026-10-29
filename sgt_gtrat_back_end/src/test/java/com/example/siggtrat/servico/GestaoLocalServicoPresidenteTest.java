package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dto.GestaoLocalDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.repositorio.GestaoLocalRepositorio;
import com.example.siggtrat.repositorio.SuperintendenciaEstadualRepositorio;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GestaoLocalServicoPresidenteTest {

    @Test
    void presidentePodeListarGestoesLocaisDaEstruturaNacional() {
        GestaoLocalRepositorio repositorio = mock(GestaoLocalRepositorio.class);
        SuperintendenciaEstadualRepositorio superintendenciaRepositorio = mock(SuperintendenciaEstadualRepositorio.class);
        AcessoOrganizacionalServico acessoOrganizacionalServico = mock(AcessoOrganizacionalServico.class);
        GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico = mock(GestaoAcessoHierarquicoServico.class);
        GestaoLocalServico servico = new GestaoLocalServico(
                repositorio,
                superintendenciaRepositorio,
                acessoOrganizacionalServico,
                gestaoAcessoHierarquicoServico
        );

        UsuarioAplicacao presidente = new UsuarioAplicacao();
        presidente.setPerfil(PerfilAcesso.PRESIDENTE);

        SuperintendenciaEstadual rj = new SuperintendenciaEstadual("SE/RJ", "RJ");
        rj.setId(1L);
        GestaoLocal benfica = new GestaoLocal();
        benfica.setId(10L);
        benfica.setNome("CTE Benfica");
        benfica.setCodigo("BENFICA");
        benfica.setAtivo(true);
        benfica.setSuperintendencia(rj);

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(presidente);
        when(acessoOrganizacionalServico.podeVisualizarTodasSuperintendencias(presidente)).thenReturn(true);
        when(repositorio.findAllByOrderByNomeAsc()).thenReturn(List.of(benfica));

        List<GestaoLocalDTO> resultado = servico.listar(null);

        assertEquals(1, resultado.size());
        assertEquals("CTE Benfica", resultado.get(0).nome());
        assertEquals("RJ", resultado.get(0).uf());
        verify(repositorio).findAllByOrderByNomeAsc();
    }

    @Test
    void presidentePodeFiltrarGestoesLocaisPorGestorEstadual() {
        GestaoLocalRepositorio repositorio = mock(GestaoLocalRepositorio.class);
        SuperintendenciaEstadualRepositorio superintendenciaRepositorio = mock(SuperintendenciaEstadualRepositorio.class);
        AcessoOrganizacionalServico acessoOrganizacionalServico = mock(AcessoOrganizacionalServico.class);
        GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico = mock(GestaoAcessoHierarquicoServico.class);
        GestaoLocalServico servico = new GestaoLocalServico(
                repositorio,
                superintendenciaRepositorio,
                acessoOrganizacionalServico,
                gestaoAcessoHierarquicoServico
        );

        UsuarioAplicacao presidente = new UsuarioAplicacao();
        presidente.setPerfil(PerfilAcesso.PRESIDENTE);
        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(presidente);
        when(acessoOrganizacionalServico.podeVisualizarTodasSuperintendencias(presidente)).thenReturn(true);
        when(repositorio.findBySuperintendenciaIdOrderByNomeAsc(1L)).thenReturn(List.of());

        servico.listar(1L);

        verify(repositorio).findBySuperintendenciaIdOrderByNomeAsc(1L);
        verify(repositorio, never()).findAllByOrderByNomeAsc();
    }
}
