package com.example.siggtrat.configuracao;

import com.example.siggtrat.auditoria20261029.AuditoriaHttpSuporte;
import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Efeito persistido do bootstrap explícito; usa transações e repos reais do MySQL. */
class ConclusaoBootstrapUsuariosSeguroMySqlIT extends AuditoriaHttpSuporte {
    @Autowired private ApplicationContext contexto;

    @ParameterizedTest(name = "{0}: rejeita senha inválida [{1}]")
    @MethodSource("senhasInvalidas")
    void senhaAusentePublicaOuModeloNaoCriaContas(String runner, String senha) throws Exception {
        long antes = quantidadeUsuarios();
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> executarBootstrap(runner, senha));
        assertTrue(erro.getMessage().toLowerCase().contains("senha"));
        // A negativa deve ocorrer antes de qualquer gravação de conta ou marcador.
        em.flush(); em.clear();
        assertEquals(antes, quantidadeUsuarios());
    }

    @ParameterizedTest
    @ValueSource(strings = {"initUsuariosGlobais", "initUsuariosGestoresSeEGestaoLocal"})
    void senhaExplicitaValidaCriaHashExigeTrocaEReexecucaoPreservaConta(String runner) throws Exception {
        String senhaInicial = "Inicial-Sintetica@2026!";
        executarBootstrap(runner, senhaInicial);
        em.flush(); em.clear();
        String username = runner.equals("initUsuariosGlobais") ? "adm" : "gestorLocalGtratRJ";
        UsuarioAplicacao usuario = porUsername(username);
        Long id = usuario.getId();
        assertTrue(usuario.getForceChangePassword());
        assertTrue(passwordEncoder.matches(senhaInicial, usuario.getPassword()));
        assertNotEquals(senhaInicial, usuario.getPassword());
        long quantidade = quantidadeUsuarios();

        String hashDefinidoPeloUsuario = passwordEncoder.encode("Nova-Sintetica@2026!");
        usuario.setPassword(hashDefinidoPeloUsuario); usuario.setForceChangePassword(false); usuario.setActive(false);
        em.flush(); em.clear();
        executarBootstrap(runner, "Outra-Inicial-Sintetica@2026!");
        em.flush(); em.clear();

        UsuarioAplicacao preservado = porUsername(username);
        assertEquals(id, preservado.getId());
        assertEquals(hashDefinidoPeloUsuario, preservado.getPassword());
        assertFalse(preservado.getForceChangePassword());
        assertFalse(preservado.getActive());
        assertEquals(quantidade, quantidadeUsuarios());
    }

    @Test
    void usuariosDeDesenvolvimentoSeLimitamAGestoesIniciaisSemColidirComNovosCadastros() throws Exception {
        SuperintendenciaEstadual se = em.createQuery(
                        "select s from SuperintendenciaEstadual s where s.uf='RJ'", SuperintendenciaEstadual.class)
                .getSingleResult();
        String sufixo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        List<Long> extras = new ArrayList<>();
        for (String codigo : List.of("EXPANSAO" + sufixo + "-RJ", "APOIO" + sufixo + "-RJ",
                "GTRAT-" + sufixo + "-RJ")) {
            GestaoLocal gestao = new GestaoLocal();
            gestao.setCodigo(codigo); gestao.setNome("Gestão adicional documentada " + codigo);
            gestao.setSuperintendencia(se); em.persist(gestao); em.flush();
            extras.add(gestao.getId());
        }
        em.clear();
        for (Long id : extras) assertNotNull(em.find(GestaoLocal.class, id),
                "O cadastro organizacional aceita gestões além das três iniciais.");

        executarBootstrap("initUsuariosGestoresSeEGestaoLocal", "Inicial-Sintetica@2026!");
        em.flush(); em.clear();
        for (String sigla : List.of("Gtrat", "Gedis", "Getra")) {
            UsuarioAplicacao usuario = porUsername("gestorLocal" + sigla + "RJ");
            assertEquals(com.example.siggtrat.dominio.acesso.PerfilAcesso.GESTOR_LOCAL, usuario.getPerfil());
            assertEquals(se.getId(), usuario.getSuperintendencia().getId());
            assertEquals(sigla.toUpperCase() + "-RJ", usuario.getGestaoLocal().getCodigo(),
                    "A conta inicial pertence somente à gestão canônica exata.");
            assertTrue(usuario.getForceChangePassword());
        }
        for (Long id : extras) {
            assertNotNull(em.find(GestaoLocal.class, id), "O bootstrap não remove o cadastro adicional.");
            assertEquals(0L, em.createQuery("select count(u) from UsuarioAplicacao u where u.gestaoLocal.id=:g", Long.class)
                    .setParameter("g", id).getSingleResult(), "O seed opcional não inventa contas para gestões adicionais.");
        }
        long quantidade = quantidadeUsuarios();
        // Sem marcador prévio, a verificação de completude deve reconhecer as
        // contas iniciais existentes sem exigir senha nova ou alcançar extras.
        String chave = "INTERNO_SEED_GESTORES_HIERARQUIA_V1";
        assertNull(em.find(ConfiguracaoSistema.class, chave));
        executarBootstrap("initUsuariosGestoresSeEGestaoLocal", "", true);
        em.flush(); em.clear();
        assertNotNull(em.find(ConfiguracaoSistema.class, chave));
        assertEquals(quantidade, quantidadeUsuarios());
    }

    static Stream<Arguments> senhasInvalidas() {
        return Stream.of("initUsuariosGlobais", "initUsuariosGestoresSeEGestaoLocal")
                .flatMap(runner -> Stream.of("", "123456", "abcdefg", "Alterar@123", "SUBSTITUA_SENHA_INICIAL", "        ")
                        .map(senha -> Arguments.of(runner, senha)));
    }

    private void executarBootstrap(String nome, String senha) throws Exception {
        executarBootstrap(nome, senha, false);
    }

    private void executarBootstrap(String nome, String senha, boolean apenasUmaVez) throws Exception {
        InicializadorDados inicializador = new InicializadorDados();
        ReflectionTestUtils.setField(inicializador, "executarSeedsApenasUmaVez", apenasUmaVez);
        Method fabrica = Arrays.stream(InicializadorDados.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(nome) && m.getReturnType() == CommandLineRunner.class)
                .findFirst().orElseThrow();
        fabrica.setAccessible(true);
        Object[] argumentos = Arrays.stream(fabrica.getParameterTypes()).map(tipo -> {
            if (tipo == String.class) return senha;
            if (tipo == int.class || tipo == Integer.class) return 8;
            return contexto.getBean(tipo);
        }).toArray();
        try {
            ((CommandLineRunner) fabrica.invoke(inicializador, argumentos)).run();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception causa) throw causa;
            throw e;
        }
    }

    private long quantidadeUsuarios() {
        return em.createQuery("select count(u) from UsuarioAplicacao u", Long.class).getSingleResult();
    }

    private UsuarioAplicacao porUsername(String username) {
        return em.createQuery("select u from UsuarioAplicacao u where u.username = :username", UsuarioAplicacao.class)
                .setParameter("username", username).getSingleResult();
    }
}
