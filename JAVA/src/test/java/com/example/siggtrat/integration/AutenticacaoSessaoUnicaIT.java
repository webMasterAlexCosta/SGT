package com.example.siggtrat.integration;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAutenticacao;
import com.example.siggtrat.configuracao.propriedades.PropriedadesSegurancaLogin;
import com.example.siggtrat.configuracao.propriedades.PropriedadesTokenJwt;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dto.CredenciaisLoginDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import com.example.siggtrat.servico.*;
import com.example.siggtrat.utilitario.UtilitarioJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AutenticacaoSessaoUnicaIT extends PostgresIntegrationTestSupport {
    @Autowired UsuarioAplicacaoRepositorio usuarios;
    @Autowired TokenAtivoRepositorio tokens;
    @Autowired LotacaoRepositorio lotacoes;
    @Autowired JornadaRepositorio jornadas;
    @Autowired SuperintendenciaEstadualRepositorio superintendencias;
    @Autowired GestaoLocalRepositorio gestoes;
    @Autowired PlatformTransactionManager gerenciador;
    @Autowired JdbcTemplate jdbc;

    @Test
    void loginsConcorrentesSaoSerializadosPelaMesmaLinhaDeUsuarioNoMySql() throws Exception {
        var encoder = new BCryptPasswordEncoder(4);
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setUsername("it-sessao-" + UUID.randomUUID());
        usuario.setEmail(usuario.getUsername() + "@exemplo.invalid");
        String matricula;
        do { matricula = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000)); }
        while (usuarios.existsByMatricula(matricula));
        usuario.setMatricula(matricula);
        usuario.setPassword(encoder.encode("senha-apenas-de-teste"));
        usuario.setActive(true);
        usuario.setForceChangePassword(false);
        usuario.setPerfil(PerfilAcesso.ADMINISTRADOR);
        usuario = usuarios.saveAndFlush(usuario);
        final String matriculaTeste = matricula;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
                    new DetalhesUsuarioPersonalizadoServico(usuarios));
            provider.setPasswordEncoder(encoder);
            AutenticacaoServico servico = new AutenticacaoServico(new ProviderManager(provider), usuarios, tokens,
                    encoder, new UtilitarioJwt(new PropriedadesTokenJwt("segredo-apenas-de-teste-".repeat(4), 3600)),
                    new TentativaLoginServico(new PropriedadesSegurancaLogin(5, 60_000)),
                    lotacoes, jornadas, superintendencias, gestoes,
                    new EscopoAcessoUsuarioServico(), mock(FiltroAutenticacaoJwt.class), mock(TotpServico.class),
                    new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas."), new com.example.siggtrat.servico.RevogacaoSessaoServico(tokens, new com.example.siggtrat.configuracao.transacao.ExecutorTransacaoIdempotente(gerenciador)));
            CountDownLatch preparadas = new CountDownLatch(2);
            CountDownLatch iniciar = new CountDownLatch(1);
            Callable<Integer> login = () -> {
                preparadas.countDown();
                assertTrue(iniciar.await(5, TimeUnit.SECONDS));
                return new TransactionTemplate(gerenciador).execute(status -> servico.autenticar(
                        new CredenciaisLoginDTO(matriculaTeste, "senha-apenas-de-teste", null),
                        new MockHttpServletRequest()).getStatusCode().value());
            };
            Future<Integer> primeira = executor.submit(login);
            Future<Integer> segunda = executor.submit(login);
            assertTrue(preparadas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            assertEquals(200, primeira.get(15, TimeUnit.SECONDS));
            assertEquals(200, segunda.get(15, TimeUnit.SECONDS));
            Long quantidade = jdbc.queryForObject("select count(*) from token_ativo where username = ?",
                    Long.class, usuario.getUsername());
            assertEquals(1L, quantidade);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            tokens.deleteByUsername(usuario.getUsername());
            usuarios.deleteById(usuario.getId());
        }
    }
}
