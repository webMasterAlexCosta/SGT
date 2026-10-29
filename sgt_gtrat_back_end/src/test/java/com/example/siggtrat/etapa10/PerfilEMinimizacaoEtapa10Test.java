package com.example.siggtrat.etapa10;

import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.servico.AcessoOrganizacionalServico;
import com.example.siggtrat.servico.EscopoAcessoUsuarioServico;
import com.example.siggtrat.servico.MinimizadorDadosPessoaisServico;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PerfilEMinimizacaoEtapa10Test {

    private final AcessoOrganizacionalServico acesso = new AcessoOrganizacionalServico(
            mock(UsuarioAplicacaoRepositorio.class), new EscopoAcessoUsuarioServico());
    private final MinimizadorDadosPessoaisServico minimizador = new MinimizadorDadosPessoaisServico();

    @Test
    void possuiSeisPerfisCanonicosComAutoridadesUnicas() {
        assertArrayEquals(new PerfilAcesso[]{
                PerfilAcesso.ADMINISTRADOR,
                PerfilAcesso.GESTOR_NACIONAL,
                PerfilAcesso.GESTOR_ESTADUAL,
                PerfilAcesso.GESTOR_LOCAL,
                PerfilAcesso.PRESIDENTE,
                PerfilAcesso.OPERACIONAL
        }, PerfilAcesso.values());
        assertEquals(6, Arrays.stream(PerfilAcesso.values())
                .map(PerfilAcesso::autoridade).distinct().count());
        assertTrue(Arrays.stream(PerfilAcesso.values())
                .allMatch(perfil -> perfil.autoridade().equals("PERFIL_" + perfil.name())));
        assertTrue(Arrays.stream(PerfilAcesso.values())
                .noneMatch(perfil -> perfil.autoridade().startsWith("ROLE_")));
    }

    @Test
    void usuarioPersisteUmUnicoPerfilTipadoSemCampoDePapeis() throws Exception {
        Field perfil = UsuarioAplicacao.class.getDeclaredField("perfil");
        assertEquals(PerfilAcesso.class, perfil.getType());
        assertNotNull(perfil.getAnnotation(Enumerated.class));
        assertEquals("perfil", perfil.getAnnotation(Column.class).name());
        assertThrows(NoSuchFieldException.class, () -> UsuarioAplicacao.class.getDeclaredField("roles"));
    }

    @Test
    void somenteGestorLocalPossuiEscritaNegocialPadrao() {
        UsuarioAplicacao local = usuario(PerfilAcesso.GESTOR_LOCAL, gestao(11L, 1L));

        assertTrue(acesso.podeGerenciarContratos(local));
        assertTrue(acesso.podeGerenciarEmpregados(local));
        assertTrue(acesso.podeGerenciarCalendario(local));

        for (PerfilAcesso perfil : new PerfilAcesso[]{PerfilAcesso.ADMINISTRADOR,
                PerfilAcesso.GESTOR_NACIONAL, PerfilAcesso.GESTOR_ESTADUAL,
                PerfilAcesso.PRESIDENTE, PerfilAcesso.OPERACIONAL}) {
            UsuarioAplicacao usuario = usuario(perfil, gestao(11L, 1L));
            assertFalse(acesso.podeGerenciarContratos(usuario), perfil.name());
            assertFalse(acesso.podeGerenciarEmpregados(usuario), perfil.name());
            assertFalse(acesso.podeGerenciarCalendario(usuario), perfil.name());
        }
    }

    @Test
    void gestorNacionalAuditaOPaisSemHerdarEscrita() {
        UsuarioAplicacao nacional = usuario(PerfilAcesso.GESTOR_NACIONAL, null);
        Contrato contrato = contrato(99L, gestao(22L, 2L));

        assertTrue(acesso.ehGlobal(nacional));
        assertTrue(acesso.podeAcessarContrato(nacional, contrato));
        assertTrue(acesso.podeConsultarEmpregados(nacional));
        assertTrue(acesso.podeAcessarRelatoriosGestao(nacional));
        assertFalse(acesso.podeGerenciarContratos(nacional));
        assertFalse(acesso.podeUsarFrequenciaDiaria(nacional));
    }

    @Test
    void presidenteTemConsultaNacionalInstitucionalSemDadosOperacionais() {
        UsuarioAplicacao presidente = usuario(PerfilAcesso.PRESIDENTE, null);
        Contrato contrato = contrato(99L, gestao(22L, 2L));

        assertTrue(acesso.podeAcessarContrato(presidente, contrato));
        assertTrue(acesso.podeVisualizarPainelGestao(presidente));
        assertFalse(acesso.podeConsultarEmpregados(presidente));
        assertFalse(acesso.podeConsultarFrequencia(presidente));
        assertTrue(acesso.podeAcessarRelatoriosGestao(presidente));
        assertFalse(acesso.ehGlobal(presidente));
    }

    @Test
    void gestorEstadualConsultaSomenteSeuEstadoESemEscrita() {
        GestaoLocal alvoMesmoEstado = gestao(31L, 3L);
        GestaoLocal alvoOutroEstado = gestao(41L, 4L);
        UsuarioAplicacao estadual = usuario(PerfilAcesso.GESTOR_ESTADUAL, null);
        estadual.setSuperintendencia(alvoMesmoEstado.getSuperintendencia());

        assertTrue(acesso.podeAcessarContrato(estadual, contrato(1L, alvoMesmoEstado)));
        assertFalse(acesso.podeAcessarContrato(estadual, contrato(2L, alvoOutroEstado)));
        assertFalse(acesso.podeGerenciarContratos(estadual));
        assertFalse(acesso.podeUsarFrequenciaDiaria(estadual));
    }

    @Test
    void aplicaMinimizacaoDeterministicaPorPerfil() {
        String cpf = "12345678909";

        NivelExposicaoDadosPessoais completo = minimizador.nivelPadrao(
                usuario(PerfilAcesso.GESTOR_LOCAL, gestao(1L, 1L)));
        assertEquals(cpf, minimizador.cpf(cpf, completo));
        assertEquals("dado sensível", minimizador.restrito("dado sensível", completo));

        NivelExposicaoDadosPessoais operacional = minimizador.nivelPadrao(
                usuario(PerfilAcesso.OPERACIONAL, gestao(1L, 1L)));
        assertEquals("***.***.***-09", minimizador.cpf(cpf, operacional));
        assertEquals("MAT123", minimizador.matricula("MAT123", operacional));
        assertNull(minimizador.restrito("dado sensível", operacional));

        for (PerfilAcesso perfil : new PerfilAcesso[]{PerfilAcesso.ADMINISTRADOR,
                PerfilAcesso.GESTOR_NACIONAL, PerfilAcesso.GESTOR_ESTADUAL}) {
            NivelExposicaoDadosPessoais auditoria = minimizador.nivelPadrao(usuario(perfil, null));
            assertEquals("***.***.***-09", minimizador.cpf(cpf, auditoria));
            assertEquals("****23", minimizador.matricula("MAT123", auditoria));
            assertNull(minimizador.restrito("dado sensível", auditoria));
        }

        NivelExposicaoDadosPessoais institucional = minimizador.nivelPadrao(
                usuario(PerfilAcesso.PRESIDENTE, null));
        assertNull(minimizador.nome("Pessoa", institucional));
        assertNull(minimizador.cpf(cpf, institucional));
        assertNull(minimizador.matricula("MAT123", institucional));
    }

    private UsuarioAplicacao usuario(PerfilAcesso perfil, GestaoLocal gestao) {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setPerfil(perfil);
        usuario.setGestaoLocal(gestao);
        if (gestao != null) usuario.setSuperintendencia(gestao.getSuperintendencia());
        return usuario;
    }

    private Contrato contrato(Long id, GestaoLocal gestao) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setGestaoLocal(gestao);
        return contrato;
    }

    private GestaoLocal gestao(Long id, Long superintendenciaId) {
        SuperintendenciaEstadual superintendencia = new SuperintendenciaEstadual();
        superintendencia.setId(superintendenciaId);
        superintendencia.setAtivo(true);
        GestaoLocal gestao = new GestaoLocal();
        gestao.setId(id);
        gestao.setAtivo(true);
        gestao.setSuperintendencia(superintendencia);
        return gestao;
    }
}
