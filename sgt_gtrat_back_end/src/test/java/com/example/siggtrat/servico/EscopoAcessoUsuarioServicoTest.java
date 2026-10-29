package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EscopoAcessoUsuarioServicoTest {

    private final EscopoAcessoUsuarioServico servico = new EscopoAcessoUsuarioServico();

    @Test
    void presidenteESuperintendenciaNaoRecebemEscopoOperacional() {
        UsuarioAplicacao presidente = usuario(ConstantesDominio.PERFIL_PRESIDENTE, null);
        UsuarioAplicacao se = usuario(ConstantesDominio.PERFIL_GESTOR_ESTADUAL, null);

        assertTrue(servico.obterEscopos(presidente).isEmpty());
        assertTrue(servico.obterEscopos(se).isEmpty());
    }

    @Test
    void gestaoLocalRecebeEscopoCompletoDaPropriaUnidade() {
        GestaoLocal gestaoLocal = gestaoLocal(10L);
        UsuarioAplicacao gestor = usuario(ConstantesDominio.PERFIL_GESTOR_LOCAL, gestaoLocal);

        assertTrue(servico.podeAcessarLotacao(gestor, 10L, 100L));
        assertTrue(servico.podeAcessarJornada(gestor, 10L, 200L));
        assertFalse(servico.podeAcessarLotacao(gestor, 11L, 100L));
    }

    @Test
    void operacionalRecebeSomenteLotacaoETurnoVinculados() {
        GestaoLocal gestaoLocal = gestaoLocal(10L);
        Lotacao lotacao = new Lotacao("Unidade Centro");
        lotacao.setId(100L);
        lotacao.setGestaoLocal(gestaoLocal);
        Jornada jornada = new Jornada();
        jornada.setId(200L);
        jornada.setDescricao("DIURNO");
        jornada.setGestaoLocal(gestaoLocal);

        UsuarioAplicacao operacional = usuario(ConstantesDominio.PERFIL_OPERACIONAL, gestaoLocal);
        operacional.setVinculosGestao(List.of(new VinculoGestaoUsuarioAplicacao(null, lotacao, jornada)));

        assertTrue(servico.podeAcessarLotacao(operacional, 10L, 100L));
        assertFalse(servico.podeAcessarLotacao(operacional, 10L, 101L));
        assertTrue(servico.podeAcessarJornada(operacional, 10L, 200L));
        assertFalse(servico.podeAcessarJornada(operacional, 10L, 201L));
        assertTrue(servico.podeAcessarJornadaLotacao(operacional, 10L, 100L, 200L));
        assertFalse(servico.podeAcessarJornadaLotacao(operacional, 10L, 100L, 201L));
    }

    private UsuarioAplicacao usuario(String role, GestaoLocal gestaoLocal) {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setPerfil(com.example.siggtrat.dominio.acesso.PerfilAcesso.valueOf((role).replace("PERFIL_", "")));
        user.setGestaoLocal(gestaoLocal);
        user.setSuperintendencia(gestaoLocal != null ? gestaoLocal.getSuperintendencia() : null);
        return user;
    }

    private GestaoLocal gestaoLocal(Long id) {
        SuperintendenciaEstadual se = new SuperintendenciaEstadual("SE/RJ", "RJ");
        se.setId(1L);
        GestaoLocal gestaoLocal = new GestaoLocal();
        gestaoLocal.setId(id);
        gestaoLocal.setNome("GTRAT/RJ");
        gestaoLocal.setCodigo("GTRAT-RJ");
        gestaoLocal.setSuperintendencia(se);
        return gestaoLocal;
    }
}
