package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesUsuarioAplicacao;
import com.example.siggtrat.configuracao.propriedades.PropriedadesAutenticacao;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.RedefinicaoSenhaUsuarioAplicacaoDTO;
import com.example.siggtrat.dto.ResumoUsuarioAplicacaoDTO;
import com.example.siggtrat.dto.AtivacaoUsuarioAplicacaoDTO;
import com.example.siggtrat.dto.AtualizacaoUsuarioAplicacaoDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoGestaoUsuarioAplicacao;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.repositorio.GestaoLocalRepositorio;
import com.example.siggtrat.repositorio.JornadaRepositorio;
import com.example.siggtrat.repositorio.LotacaoRepositorio;
import com.example.siggtrat.repositorio.SuperintendenciaEstadualRepositorio;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioAplicacaoServiceSecurityTest {

    @Mock UsuarioAplicacaoRepositorio userRepositorio;
    @Mock LotacaoRepositorio lotacaoRepositorio;
    @Mock JornadaRepositorio jornadaRepositorio;
    @Mock SuperintendenciaEstadualRepositorio superintendenciaRepositorio;
    @Mock GestaoLocalRepositorio gestaoLocalRepositorio;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EscopoAcessoUsuarioServico userAccessScopeServico;
    @Mock AcessoOrganizacionalServico acessoOrganizacionalServico;
    @Mock TokenAtivoRepositorio tokenAtivoRepositorio;
    @Mock FiltroAutenticacaoJwt jwtAuthenticationFilter;
    @Mock GestaoAcessoHierarquicoServico gestaoAcessoHierarquicoServico;

    private UsuarioAplicacaoServico servico;

    @BeforeEach
    void setUp() {
        servico = new UsuarioAplicacaoServico(
                userRepositorio,
                lotacaoRepositorio,
                jornadaRepositorio,
                superintendenciaRepositorio,
                gestaoLocalRepositorio,
                passwordEncoder,
                userAccessScopeServico,
                acessoOrganizacionalServico,
                tokenAtivoRepositorio,
                jwtAuthenticationFilter,
                gestaoAcessoHierarquicoServico,
                new PropriedadesUsuarioAplicacao(24, 7),
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas.")
        );
    }

    @Test
    void superintendenciaListaSomenteUsuariosGestaoLocalDaPropriaSe() {
        SuperintendenciaEstadual se = se(10L, "RJ");
        UsuarioAplicacao gestorSe = usuario(1L, "se_rj", ConstantesDominio.PERFIL_GESTOR_ESTADUAL, se, null);
        GestaoLocal gestaoLocal = gestaoLocal(20L, "GTRAT/RJ", se);
        UsuarioAplicacao usuarioGestaoLocal = usuario(2L, "gestaoLocal_rj", ConstantesDominio.PERFIL_GESTOR_LOCAL, se, gestaoLocal);
        UsuarioAplicacao operacional = usuario(3L, "operacional_rj", ConstantesDominio.PERFIL_OPERACIONAL, se, gestaoLocal);

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(gestorSe);
        when(acessoOrganizacionalServico.ehGestorEstadual(gestorSe)).thenReturn(true);
        when(acessoOrganizacionalServico.superintendenciaEfetiva(gestorSe)).thenReturn(se);
        // Mesmo que uma implementação de repositório devolvesse dado indevido,
        // a defesa adicional no serviço elimina o usuário operacional.
        when(userRepositorio.findByPerfilAndSuperintendenciaIdOrderByUsernameAsc(
                com.example.siggtrat.dominio.acesso.PerfilAcesso.GESTOR_LOCAL, se.getId()))
                .thenReturn(List.of(usuarioGestaoLocal, operacional));
        when(acessoOrganizacionalServico.superintendenciaEfetiva(usuarioGestaoLocal)).thenReturn(se);
        when(acessoOrganizacionalServico.superintendenciaEfetiva(operacional)).thenReturn(se);
        when(acessoOrganizacionalServico.ehGestaoLocal(usuarioGestaoLocal)).thenReturn(true);
        when(acessoOrganizacionalServico.ehGestaoLocal(operacional)).thenReturn(false);
        when(acessoOrganizacionalServico.podeAcessarSuperintendencia(gestorSe, se.getId())).thenReturn(true);

        List<ResumoUsuarioAplicacaoDTO> resultado = servico.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals("gestaoLocal_rj", resultado.get(0).username());
        verify(userRepositorio, never()).findAllByOrderByUsernameAsc();
        verify(userRepositorio, never()).findByPerfilAndGestaoLocalIdOrderByUsernameAsc(any(), any());
    }

    @Test
    void gestaoLocalRedefineSenhaDoOperacionalEDerrubaSessoes() {
        SuperintendenciaEstadual se = se(10L, "RJ");
        GestaoLocal gestaoLocal = gestaoLocal(20L, "GTRAT/RJ", se);
        UsuarioAplicacao gestorGestaoLocal = usuario(1L, "gestaoLocal_rj", ConstantesDominio.PERFIL_GESTOR_LOCAL, se, gestaoLocal);
        UsuarioAplicacao operacional = usuario(3L, "bfct3", ConstantesDominio.PERFIL_OPERACIONAL, se, gestaoLocal);
        operacional.setPassword("hash-antigo");
        operacional.setFailedAttempts(4);
        operacional.setLockoutEnd(LocalDateTime.now().plusMinutes(10));

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(gestorGestaoLocal);
        when(userRepositorio.findById(operacional.getId())).thenReturn(Optional.of(operacional));
        when(acessoOrganizacionalServico.ehGestaoLocal(gestorGestaoLocal)).thenReturn(true);
        when(acessoOrganizacionalServico.ehOperacional(operacional)).thenReturn(true);
        when(acessoOrganizacionalServico.podeAcessarGestaoLocal(gestorGestaoLocal, gestaoLocal)).thenReturn(true);
        when(passwordEncoder.encode("senha-nova")).thenReturn("hash-novo");
        when(userRepositorio.save(operacional)).thenReturn(operacional);

        servico.redefinirSenha(operacional.getId(), new RedefinicaoSenhaUsuarioAplicacaoDTO("senha-nova"));

        assertEquals("hash-novo", operacional.getPassword());
        assertTrue(operacional.getForceChangePassword());
        assertEquals(0, operacional.getFailedAttempts());
        assertNull(operacional.getLockoutEnd());
        verify(tokenAtivoRepositorio).deleteByUsername("bfct3");
        verify(jwtAuthenticationFilter).removerSessoesUsuarioDoCache("bfct3");
    }

    @Test
    void superintendenciaNaoPodeRedefinirSenhaDeOperacional() {
        SuperintendenciaEstadual se = se(10L, "RJ");
        GestaoLocal gestaoLocal = gestaoLocal(20L, "GTRAT/RJ", se);
        UsuarioAplicacao gestorSe = usuario(1L, "se_rj", ConstantesDominio.PERFIL_GESTOR_ESTADUAL, se, null);
        UsuarioAplicacao operacional = usuario(3L, "bfct3", ConstantesDominio.PERFIL_OPERACIONAL, se, gestaoLocal);

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(gestorSe);
        when(userRepositorio.findById(operacional.getId())).thenReturn(Optional.of(operacional));
        when(acessoOrganizacionalServico.ehGestorEstadual(gestorSe)).thenReturn(true);
        when(acessoOrganizacionalServico.superintendenciaEfetiva(operacional)).thenReturn(se);
        when(acessoOrganizacionalServico.ehGestaoLocal(operacional)).thenReturn(false);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> servico.redefinirSenha(operacional.getId(),
                        new RedefinicaoSenhaUsuarioAplicacaoDTO("senha-nova")));
        verify(passwordEncoder, never()).encode(any());
        verify(tokenAtivoRepositorio, never()).deleteByUsername(any());
    }


    @Test
    void superintendenciaPodeRedefinirSenhaSomenteDoUsuarioGestaoLocalDaPropriaSe() {
        SuperintendenciaEstadual se = se(10L, "RJ");
        GestaoLocal gestaoLocal = gestaoLocal(20L, "GTRAT/RJ", se);
        UsuarioAplicacao gestorSe = usuario(1L, "se_rj", ConstantesDominio.PERFIL_GESTOR_ESTADUAL, se, null);
        UsuarioAplicacao usuarioGestaoLocal = usuario(2L, "gestaoLocal_rj", ConstantesDominio.PERFIL_GESTOR_LOCAL, se, gestaoLocal);
        usuarioGestaoLocal.setPassword("hash-antigo");

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(gestorSe);
        when(userRepositorio.findById(usuarioGestaoLocal.getId())).thenReturn(Optional.of(usuarioGestaoLocal));
        when(acessoOrganizacionalServico.ehGestorEstadual(gestorSe)).thenReturn(true);
        when(acessoOrganizacionalServico.superintendenciaEfetiva(usuarioGestaoLocal)).thenReturn(se);
        when(acessoOrganizacionalServico.ehGestaoLocal(usuarioGestaoLocal)).thenReturn(true);
        when(acessoOrganizacionalServico.podeAcessarSuperintendencia(gestorSe, se.getId())).thenReturn(true);
        when(passwordEncoder.encode("senha-gestaoLocal")).thenReturn("hash-gestaoLocal");
        when(userRepositorio.save(usuarioGestaoLocal)).thenReturn(usuarioGestaoLocal);

        servico.redefinirSenha(usuarioGestaoLocal.getId(), new RedefinicaoSenhaUsuarioAplicacaoDTO("senha-gestaoLocal"));

        assertEquals("hash-gestaoLocal", usuarioGestaoLocal.getPassword());
        verify(tokenAtivoRepositorio).deleteByUsername("gestaoLocal_rj");
        verify(jwtAuthenticationFilter).removerSessoesUsuarioDoCache("gestaoLocal_rj");
    }

    @Test
    void gestaoLocalNaoPodeAtivarOperacionalDeOutraGestaoLocal() {
        SuperintendenciaEstadual se = se(10L, "RJ");
        GestaoLocal gestaoLocalGestora = gestaoLocal(20L, "GTRAT/RJ-1", se);
        GestaoLocal outraGestaoLocal = gestaoLocal(21L, "GTRAT/RJ-2", se);
        UsuarioAplicacao gestor = usuario(1L, "gestaoLocal_rj_1", ConstantesDominio.PERFIL_GESTOR_LOCAL, se, gestaoLocalGestora);
        UsuarioAplicacao operacional = usuario(3L, "operacional_rj_2", ConstantesDominio.PERFIL_OPERACIONAL, se, outraGestaoLocal);
        operacional.setActive(false);

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(gestor);
        when(userRepositorio.findById(operacional.getId())).thenReturn(Optional.of(operacional));
        when(acessoOrganizacionalServico.ehGestaoLocal(gestor)).thenReturn(true);
        when(acessoOrganizacionalServico.ehOperacional(operacional)).thenReturn(true);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> servico.ativarUsuario(operacional.getId()));
        verify(userRepositorio, never()).save(any());
        verify(tokenAtivoRepositorio, never()).deleteByUsername(any());
    }

    @Test
    void administradorNaoPodeAprovarSolicitacaoOperacionalEmNomeDaGestaoLocal() {
        SuperintendenciaEstadual se = se(10L, "RJ");
        GestaoLocal gestaoLocal = gestaoLocal(20L, "GTRAT/RJ", se);
        UsuarioAplicacao administrador = usuario(1L, "adm", ConstantesDominio.PERFIL_ADMINISTRADOR, null, null);
        UsuarioAplicacao operacional = usuario(3L, "pendente_rj", ConstantesDominio.PERFIL_OPERACIONAL, se, gestaoLocal);
        operacional.setActive(false);

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(administrador);
        when(userRepositorio.findById(operacional.getId())).thenReturn(Optional.of(operacional));
        when(acessoOrganizacionalServico.ehOperacional(operacional)).thenReturn(true);
        when(acessoOrganizacionalServico.ehGestaoLocal(administrador)).thenReturn(false);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> servico.ativarUsuario(operacional.getId()));
        verify(userRepositorio, never()).save(any());
    }

    @Test
    void ativacaoLimpaBloqueioEInvalidaTokensAnteriores() {
        SuperintendenciaEstadual se = se(10L, "RJ");
        GestaoLocal gestaoLocal = gestaoLocal(20L, "GTRAT/RJ", se);
        UsuarioAplicacao gestor = usuario(1L, "gestaoLocal_rj", ConstantesDominio.PERFIL_GESTOR_LOCAL, se, gestaoLocal);
        UsuarioAplicacao operacional = usuario(3L, "bfct3", ConstantesDominio.PERFIL_OPERACIONAL, se, gestaoLocal);
        operacional.setActive(false);
        operacional.setFailedAttempts(5);
        operacional.setLockoutEnd(LocalDateTime.now().plusHours(1));

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(gestor);
        when(userRepositorio.findById(operacional.getId())).thenReturn(Optional.of(operacional));
        when(acessoOrganizacionalServico.ehGestaoLocal(gestor)).thenReturn(true);
        when(acessoOrganizacionalServico.ehOperacional(operacional)).thenReturn(true);
        when(gestaoAcessoHierarquicoServico.ativarComSenhaTemporaria(operacional))
                .thenAnswer(ignored -> {
                    operacional.setActive(true);
                    operacional.setPassword("hash-temporario");
                    operacional.setForceChangePassword(true);
                    operacional.setFailedAttempts(0);
                    operacional.setLockoutEnd(null);
                    return new AtivacaoUsuarioAplicacaoDTO(
                            "Usuário ativado.", operacional.getUsername(), "Temp@2026Segura", true);
                });

        var resposta = servico.ativarUsuario(operacional.getId());

        assertTrue(operacional.getActive());
        assertEquals("hash-temporario", operacional.getPassword());
        assertTrue(operacional.getForceChangePassword());
        assertEquals(0, operacional.getFailedAttempts());
        assertNull(operacional.getLockoutEnd());
        assertEquals("Temp@2026Segura", resposta.temporaryPassword());
        assertTrue(resposta.forceChangeOnNextLogin());
        verify(gestaoAcessoHierarquicoServico).ativarComSenhaTemporaria(operacional);
    }

    @Test
    void ativacaoNaoPodeRedefinirSenhaDeUsuarioQueJaEstaAtivo() {
        UsuarioAplicacao gestor = usuario(1L, "admin", ConstantesDominio.PERFIL_ADMINISTRADOR, null, null);
        UsuarioAplicacao ativo = usuario(2L, "usuario_ativo", ConstantesDominio.PERFIL_GESTOR_LOCAL, null, null);
        ativo.setActive(true);

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(gestor);
        when(userRepositorio.findById(ativo.getId())).thenReturn(Optional.of(ativo));

        assertThrows(IllegalArgumentException.class, () -> servico.ativarUsuario(ativo.getId()));
        verify(gestaoAcessoHierarquicoServico, never()).ativarComSenhaTemporaria(any());
        verify(userRepositorio, never()).save(any());
    }

    @Test
    void edicaoEscopoOperacionalPreservaVinculosPersistidosEImpedeConflitoUnique() throws Exception {
        UsuarioAplicacao operacional = new UsuarioAplicacao();
        operacional.setId(58L);

        Lotacao lotacao = new Lotacao();
        lotacao.setId(3L);
        Jornada turno1A = new Jornada();
        turno1A.setId(163L);
        Jornada turno1B = new Jornada();
        turno1B.setId(164L);
        Jornada turno1C = new Jornada();
        turno1C.setId(165L);

        VinculoGestaoUsuarioAplicacao existente1A = new VinculoGestaoUsuarioAplicacao(operacional, lotacao, turno1A);
        existente1A.setId(501L);
        VinculoGestaoUsuarioAplicacao existente1B = new VinculoGestaoUsuarioAplicacao(operacional, lotacao, turno1B);
        existente1B.setId(502L);
        operacional.adicionarVinculoGestao(existente1A);
        operacional.adicionarVinculoGestao(existente1B);

        List<VinculoGestaoUsuarioAplicacao> enviadosPelaTela = List.of(
                new VinculoGestaoUsuarioAplicacao(null, lotacao, turno1A),
                new VinculoGestaoUsuarioAplicacao(null, lotacao, turno1B),
                new VinculoGestaoUsuarioAplicacao(null, lotacao, turno1C)
        );

        Method merge = UsuarioAplicacaoServico.class.getDeclaredMethod(
                "aplicarEscoposGestao", UsuarioAplicacao.class, List.class);
        merge.setAccessible(true);
        merge.invoke(servico, operacional, enviadosPelaTela);

        assertEquals(3, operacional.getVinculosGestao().size());
        assertTrue(operacional.getVinculosGestao().contains(existente1A),
                "O vínculo 3:163 já persistido deve ser reaproveitado, não reinserido.");
        assertTrue(operacional.getVinculosGestao().contains(existente1B),
                "O vínculo 3:164 já persistido deve ser reaproveitado, não reinserido.");
        assertEquals(1, operacional.getVinculosGestao().stream()
                .filter(v -> v.getId() == null)
                .count(), "Somente o vínculo realmente novo deve gerar INSERT.");
        assertEquals(3L, operacional.getLotacao().getId());
        assertEquals(163L, operacional.getJornada().getId());
    }

    @Test
    void desativacaoEncerraSessoesDoOperacional() {
        SuperintendenciaEstadual se = se(10L, "RJ");
        GestaoLocal gestaoLocal = gestaoLocal(20L, "GTRAT/RJ", se);
        UsuarioAplicacao gestor = usuario(1L, "gestaoLocal_rj", ConstantesDominio.PERFIL_GESTOR_LOCAL, se, gestaoLocal);
        UsuarioAplicacao operacional = usuario(3L, "bfct3", ConstantesDominio.PERFIL_OPERACIONAL, se, gestaoLocal);

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(gestor);
        when(userRepositorio.findById(operacional.getId())).thenReturn(Optional.of(operacional));
        when(acessoOrganizacionalServico.ehGestaoLocal(gestor)).thenReturn(true);
        when(acessoOrganizacionalServico.ehOperacional(operacional)).thenReturn(true);
        when(acessoOrganizacionalServico.podeAcessarGestaoLocal(gestor, gestaoLocal)).thenReturn(true);
        doAnswer(ignored -> {
            operacional.setActive(false);
            return null;
        }).when(gestaoAcessoHierarquicoServico).inativarUsuario(operacional);

        servico.excluirUsuario(operacional.getId());

        assertFalse(operacional.getActive());
        verify(gestaoAcessoHierarquicoServico).inativarUsuario(operacional);
    }

    @Test
    void inativacaoIndividualDeGestorLocalNaoInativaTodaAUnidade() {
        SuperintendenciaEstadual se = se(10L, "RJ");
        GestaoLocal unidadeLocal = gestaoLocal(20L, "GTRAT/RJ", se);
        UsuarioAplicacao gestorSe = usuario(
                1L, "se_rj", ConstantesDominio.PERFIL_GESTOR_ESTADUAL, se, null);
        UsuarioAplicacao gestorLocal = usuario(
                2L, "gestor_local_rj", ConstantesDominio.PERFIL_GESTOR_LOCAL, se, unidadeLocal);
        AtualizacaoUsuarioAplicacaoDTO atualizacao = new AtualizacaoUsuarioAplicacaoDTO(
                null,
                false,
                null,
                com.example.siggtrat.dominio.acesso.PerfilAcesso.GESTOR_LOCAL,
                null,
                null,
                null,
                List.of(),
                se.getId(),
                unidadeLocal.getId()
        );

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(gestorSe);
        when(userRepositorio.findById(gestorLocal.getId())).thenReturn(Optional.of(gestorLocal));
        when(acessoOrganizacionalServico.ehGestorEstadual(gestorSe)).thenReturn(true);
        when(acessoOrganizacionalServico.ehGestaoLocal(gestorLocal)).thenReturn(true);
        when(acessoOrganizacionalServico.superintendenciaEfetiva(gestorLocal)).thenReturn(se);
        when(acessoOrganizacionalServico.podeAcessarSuperintendencia(gestorSe, se.getId()))
                .thenReturn(true);
        when(gestaoLocalRepositorio.findById(unidadeLocal.getId())).thenReturn(Optional.of(unidadeLocal));
        when(userRepositorio.save(gestorLocal)).thenReturn(gestorLocal);
        doAnswer(ignored -> {
            gestorLocal.setActive(false);
            return null;
        }).when(gestaoAcessoHierarquicoServico).inativarUsuario(gestorLocal);

        servico.atualizar(gestorLocal.getId(), atualizacao);

        assertFalse(gestorLocal.getActive());
        verify(gestaoAcessoHierarquicoServico).inativarUsuario(gestorLocal);
        verify(gestaoAcessoHierarquicoServico, never()).inativarGestaoLocal(unidadeLocal.getId());
    }

    private static SuperintendenciaEstadual se(Long id, String uf) {
        SuperintendenciaEstadual se = new SuperintendenciaEstadual("SE/" + uf, uf);
        se.setId(id);
        return se;
    }

    private static GestaoLocal gestaoLocal(Long id, String nome, SuperintendenciaEstadual se) {
        GestaoLocal gestaoLocal = new GestaoLocal();
        gestaoLocal.setId(id);
        gestaoLocal.setNome(nome);
        gestaoLocal.setCodigo("C" + id);
        gestaoLocal.setSuperintendencia(se);
        return gestaoLocal;
    }

    private static UsuarioAplicacao usuario(Long id, String username, String role,
                                   SuperintendenciaEstadual se, GestaoLocal gestaoLocal) {
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@correios.com.br");
        user.setPerfil(com.example.siggtrat.dominio.acesso.PerfilAcesso.valueOf((role).replace("PERFIL_", "")));
        user.setActive(true);
        user.setForceChangePassword(false);
        user.setSuperintendencia(se);
        user.setGestaoLocal(gestaoLocal);
        return user;
    }
}
