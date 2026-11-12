package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AcessoOrganizacionalServicoTest {

    private final EscopoAcessoUsuarioServico scopeServico = new EscopoAcessoUsuarioServico();
    private final AcessoOrganizacionalServico servico =
            new AcessoOrganizacionalServico(mock(UsuarioAplicacaoRepositorio.class), scopeServico);


    @Test
    void administradorTecnicoNaoExecutaFrequenciaNegocialPorPadrao() {
        UsuarioAplicacao administrador = usuario(ConstantesDominio.PERFIL_ADMINISTRADOR, null, null);

        assertFalse(servico.podeUsarFrequenciaDiaria(administrador));
        assertTrue(servico.podeConsultarFrequencia(administrador));
    }

    @Test
    void presidenteConsultaContratosNacionaisSemReceberAcessoOperacional() {
        UsuarioAplicacao presidente = usuario(ConstantesDominio.PERFIL_PRESIDENTE, null, null);
        Contrato contrato = contrato(gestaoLocal(10L, superintendencia(1L, "RJ")));

        assertFalse(servico.ehGlobal(presidente));
        assertTrue(servico.podeVisualizarTodasSuperintendencias(presidente));
        assertTrue(servico.podeVisualizarPainelGestao(presidente));
        assertFalse(servico.podeAcessarGestaoLocal(presidente, contrato.getGestaoLocal()));
        assertTrue(servico.podeAcessarContrato(presidente, contrato));
        assertTrue(servico.podeConsultarContratos(presidente));
        assertFalse(servico.podeGerenciarContratos(presidente));
        assertTrue(servico.podeAcessarRelatoriosGestao(presidente));
        assertFalse(servico.podeUsarFrequenciaDiaria(presidente));
        assertFalse(servico.podeConsultarFrequencia(presidente));
        assertFalse(servico.podeConsultarEmpregados(presidente));
    }

    @Test
    void superintendenciaEnxergaSomenteGestoesLocaisDoProprioEstado() {
        SuperintendenciaEstadual rj = superintendencia(1L, "RJ");
        SuperintendenciaEstadual sp = superintendencia(2L, "SP");
        UsuarioAplicacao usuarioRj = usuario(ConstantesDominio.PERFIL_GESTOR_ESTADUAL, rj, null);

        assertTrue(servico.podeAcessarContrato(usuarioRj, contrato(gestaoLocal(10L, rj))));
        assertFalse(servico.podeAcessarContrato(usuarioRj, contrato(gestaoLocal(20L, sp))));
        assertFalse(servico.podeGerenciarContratos(usuarioRj));
        assertTrue(servico.podeAcessarRelatoriosGestao(usuarioRj));
        assertFalse(servico.podeUsarFrequenciaDiaria(usuarioRj));
        assertTrue(servico.podeConsultarFrequencia(usuarioRj));
        assertTrue(servico.podeConsultarEmpregados(usuarioRj));
        assertFalse(servico.podeGerenciarEmpregados(usuarioRj));

        Empregado empregadoRj = new Empregado();
        empregadoRj.setGestaoLocal(gestaoLocal(11L, rj));
        Empregado empregadoSp = new Empregado();
        empregadoSp.setGestaoLocal(gestaoLocal(21L, sp));
        assertTrue(servico.podeAcessarEmpregado(usuarioRj, empregadoRj));
        assertFalse(servico.podeAcessarEmpregado(usuarioRj, empregadoSp));
    }

    @Test
    void gestaoLocalGerenciaSomenteOsPropriosContratosEFrequencias() {
        SuperintendenciaEstadual rj = superintendencia(1L, "RJ");
        SuperintendenciaEstadual sp = superintendencia(2L, "SP");
        GestaoLocal gestaoLocalA = gestaoLocal(10L, rj);
        GestaoLocal gestaoLocalB = gestaoLocal(11L, sp);
        UsuarioAplicacao usuarioGestaoLocal = usuario(ConstantesDominio.PERFIL_GESTOR_LOCAL, rj, gestaoLocalA);

        assertTrue(servico.podeAcessarContrato(usuarioGestaoLocal, contrato(gestaoLocalA)));
        assertFalse(servico.podeAcessarContrato(usuarioGestaoLocal, contrato(gestaoLocalB)));
        assertTrue(servico.podeGerenciarContratos(usuarioGestaoLocal));
        assertTrue(servico.podeAcessarRelatoriosGestao(usuarioGestaoLocal));
        assertTrue(servico.podeUsarFrequenciaDiaria(usuarioGestaoLocal));
    }


    @Test
    void gestaoLocalNaoAcessaEmpregadoDeOutraGestaoLocalMesmoComIdConhecido() {
        SuperintendenciaEstadual rj = superintendencia(1L, "RJ");
        GestaoLocal gestaoLocalA = gestaoLocal(10L, rj);
        GestaoLocal gestaoLocalB = gestaoLocal(11L, rj);
        UsuarioAplicacao usuarioGestaoLocal = usuario(ConstantesDominio.PERFIL_GESTOR_LOCAL, rj, gestaoLocalA);
        Empregado empregadoDaOutraGestaoLocal = new Empregado();
        empregadoDaOutraGestaoLocal.setGestaoLocal(gestaoLocalB);

        assertFalse(servico.podeAcessarEmpregado(usuarioGestaoLocal, empregadoDaOutraGestaoLocal));
    }

    @Test
    void usuarioOperacionalUsaDadosAuxiliaresDaFrequenciaSemAcessarRelatorios() {
        SuperintendenciaEstadual rj = superintendencia(1L, "RJ");
        GestaoLocal gestaoLocalRj = gestaoLocal(10L, rj);
        UsuarioAplicacao operacional = usuario(ConstantesDominio.PERFIL_OPERACIONAL, rj, gestaoLocalRj);

        assertTrue(servico.podeConsultarContratos(operacional));
        assertFalse(servico.podeAcessarRelatoriosGestao(operacional));
        assertFalse(servico.podeGerenciarContratos(operacional));
        assertTrue(servico.podeUsarFrequenciaDiaria(operacional));
        assertTrue(servico.podeConsultarFrequencia(operacional));
    }

    @Test
    void usuarioOperacionalPodeLancarFrequenciaSomenteNoParVinculado() {
        SuperintendenciaEstadual rj = superintendencia(1L, "RJ");
        GestaoLocal gestaoLocalRj = gestaoLocal(10L, rj);
        Lotacao lotacao = new Lotacao();
        lotacao.setId(100L);
        lotacao.setGestaoLocal(gestaoLocalRj);
        Jornada jornadaPermitida = new Jornada();
        jornadaPermitida.setId(200L);
        jornadaPermitida.setGestaoLocal(gestaoLocalRj);
        Jornada jornadaNaoPermitida = new Jornada();
        jornadaNaoPermitida.setId(201L);
        jornadaNaoPermitida.setGestaoLocal(gestaoLocalRj);

        UsuarioAplicacao operacional = usuario(ConstantesDominio.PERFIL_OPERACIONAL, rj, gestaoLocalRj);
        operacional.setVinculosGestao(List.of(
                new VinculoGestaoUsuarioAplicacao(operacional, lotacao, jornadaPermitida)
        ));

        Contrato contrato = contrato(gestaoLocalRj);

        assertTrue(servico.podeLancarFrequenciaDiaria(operacional, lotacao, jornadaPermitida));
        assertFalse(servico.podeLancarFrequenciaDiaria(operacional, lotacao, jornadaNaoPermitida));
        assertTrue(servico.podeAcessarAlocacaoRelatorio(operacional, contrato, lotacao, jornadaPermitida));
        assertFalse(servico.podeAcessarAlocacaoRelatorio(operacional, contrato, lotacao, jornadaNaoPermitida));
    }

    private UsuarioAplicacao usuario(String role, SuperintendenciaEstadual se, GestaoLocal gestaoLocal) {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setPerfil(com.example.siggtrat.dominio.acesso.PerfilAcesso.valueOf((role).replace("PERFIL_", "")));
        user.setSuperintendencia(se);
        user.setGestaoLocal(gestaoLocal);
        return user;
    }

    @Test
    void recargaAposLockRecusaVinculoRevogadoMesmoComPerfilERaizIguais() {
        var em = mock(jakarta.persistence.EntityManager.class);
        org.springframework.test.util.ReflectionTestUtils.setField(servico,"entityManager",em);
        var se=superintendencia(1L,"RJ");var gestao=gestaoLocal(10L,se);
        var antes=usuario(ConstantesDominio.PERFIL_OPERACIONAL,se,gestao);antes.setId(7L);antes.setActive(true);
        var lotacao=new Lotacao();lotacao.setId(100L);
        var jornada=new Jornada();jornada.setId(200L);
        antes.setVinculosGestao(List.of(new VinculoGestaoUsuarioAplicacao(antes,lotacao,jornada)));
        var depois=usuario(ConstantesDominio.PERFIL_OPERACIONAL,se,gestao);depois.setId(7L);depois.setActive(true);
        org.mockito.Mockito.when(em.find(UsuarioAplicacao.class,7L,jakarta.persistence.LockModeType.PESSIMISTIC_READ)).thenReturn(depois);
        var erro=org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->servico.recarregarAposEspera(antes));
        org.junit.jupiter.api.Assertions.assertEquals(403,erro.getStatusCode().value());
        org.mockito.Mockito.verify(em).detach(antes);
    }

    private SuperintendenciaEstadual superintendencia(Long id, String uf) {
        SuperintendenciaEstadual se = new SuperintendenciaEstadual("SE/" + uf, uf);
        se.setId(id);
        return se;
    }

    private GestaoLocal gestaoLocal(Long id, SuperintendenciaEstadual se) {
        GestaoLocal gestaoLocal = new GestaoLocal();
        gestaoLocal.setId(id);
        gestaoLocal.setNome("GestaoLocal " + id);
        gestaoLocal.setCodigo("CP" + id);
        gestaoLocal.setSuperintendencia(se);
        return gestaoLocal;
    }

    private Contrato contrato(GestaoLocal gestaoLocal) {
        Contrato contrato = new Contrato();
        contrato.setGestaoLocal(gestaoLocal);
        return contrato;
    }
}
