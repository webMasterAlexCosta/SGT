package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAutenticacao;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dto.CredenciaisLoginDTO;
import com.example.siggtrat.dto.RenovacaoSessaoDTO;
import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import com.example.siggtrat.utilitario.UtilitarioJwt;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessaoUnicaConcorrenciaRegressaoTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sessaoAtivaRenovaPreservandoSegundoFatorPorCookieOuBearer(boolean bearer) {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setId(7L);
        usuario.setUsername("gestor.teste");
        usuario.setPassword("hash");
        usuario.setActive(true);
        usuario.setTwoFactorEnabled(true);
        UsuarioAplicacaoRepositorio usuarios = mock(UsuarioAplicacaoRepositorio.class);
        when(usuarios.buscarPorUsernameParaAtualizacao(usuario.getUsername())).thenReturn(Optional.of(usuario));
        TokenAtivoRepositorio tokens = mock(TokenAtivoRepositorio.class);
        TokenAtivo anterior = new TokenAtivo();
        anterior.setUsername(usuario.getUsername());
        when(tokens.buscarPorTokenBruto("jwt-anterior")).thenReturn(Optional.of(anterior));
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.matches("senha-teste", "hash")).thenReturn(true);
        UtilitarioJwt jwt = mock(UtilitarioJwt.class);
        when(jwt.validarToken("jwt-anterior")).thenReturn(true);
        when(jwt.segundoFatorVerificado("jwt-anterior")).thenReturn(true);
        when(jwt.gerarToken(7L, true)).thenReturn("jwt-novo");
        when(jwt.extrairExpiracao("jwt-novo")).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        AutenticacaoServico servico = new AutenticacaoServico(mock(AuthenticationManager.class), usuarios, tokens,
                encoder, jwt, mock(TentativaLoginServico.class), mock(LotacaoRepositorio.class),
                mock(JornadaRepositorio.class), mock(SuperintendenciaEstadualRepositorio.class),
                mock(GestaoLocalRepositorio.class), mock(EscopoAcessoUsuarioServico.class),
                mock(FiltroAutenticacaoJwt.class), mock(TotpServico.class),
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas."), mock(com.example.siggtrat.servico.RevogacaoSessaoServico.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (bearer) request.addHeader("Authorization", "Bearer jwt-anterior");
        else request.setCookies(new Cookie(ConstantesDominio.COOKIE_AUTH, "jwt-anterior"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.getUsername(), null, List.of()));
        try {
            var resposta = servico.renovarSessao(new RenovacaoSessaoDTO("senha-teste"), request);
            assertEquals(200, resposta.getStatusCode().value());
            verify(jwt).gerarToken(7L, true);
            verify(tokens).deleteByUsername(usuario.getUsername());
            verify(tokens).save(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void renovacaoObsoletaNaoCriaNovaSessaoAposRevogacao() {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setId(7L);
        usuario.setUsername("gestor.teste");
        usuario.setPassword("hash");
        usuario.setActive(true);
        UsuarioAplicacaoRepositorio usuarios = mock(UsuarioAplicacaoRepositorio.class, invocation -> {
            String nome = invocation.getMethod().getName();
            if (nome.equals("findByUsername") || nome.equals("buscarPorUsernameParaAtualizacao")) {
                return Optional.of(usuario);
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        TokenAtivoRepositorio tokens = mock(TokenAtivoRepositorio.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.matches("senha-teste", "hash")).thenReturn(true);
        UtilitarioJwt jwt = mock(UtilitarioJwt.class);
        when(jwt.validarToken("jwt-revogado")).thenReturn(true);
        when(jwt.gerarToken(7L, false)).thenReturn("jwt-novo");
        when(jwt.extrairExpiracao("jwt-novo")).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        AutenticacaoServico servico = new AutenticacaoServico(mock(AuthenticationManager.class), usuarios, tokens,
                encoder, jwt, mock(TentativaLoginServico.class), mock(LotacaoRepositorio.class),
                mock(JornadaRepositorio.class), mock(SuperintendenciaEstadualRepositorio.class),
                mock(GestaoLocalRepositorio.class), mock(EscopoAcessoUsuarioServico.class),
                mock(FiltroAutenticacaoJwt.class), mock(TotpServico.class),
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas."), mock(com.example.siggtrat.servico.RevogacaoSessaoServico.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(ConstantesDominio.COOKIE_AUTH, "jwt-revogado"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.getUsername(), null, List.of()));
        try {
            var resposta = servico.renovarSessao(new RenovacaoSessaoDTO("senha-teste"), request);
            assertEquals(401, resposta.getStatusCode().value());
            verify(tokens, never()).save(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void duasAutenticacoesConcorrentesDeixamUmaUnicaSessaoAtiva() throws Exception {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setId(7L);
        usuario.setUsername("gestor.teste");
        usuario.setMatricula("12345678");
        usuario.setPassword("hash");
        usuario.setActive(true);
        usuario.setPerfil(PerfilAcesso.ADMINISTRADOR);
        usuario.setForceChangePassword(false);
        ReentrantLock bloqueioDaLinha = new ReentrantLock();
        UsuarioAplicacaoRepositorio usuarios = mock(UsuarioAplicacaoRepositorio.class, invocation -> {
            String metodo = invocation.getMethod().getName();
            if (metodo.equals("buscarPorMatriculaParaAtualizacao")) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                bloqueioDaLinha.lock();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) { bloqueioDaLinha.unlock(); }
                });
                return Optional.of(usuario);
            }
            if (metodo.equals("findByMatricula")) return Optional.of(usuario);
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        var sessoes = new ConcurrentHashMap<Long, TokenAtivo>();
        AtomicLong sequencia = new AtomicLong();
        CountDownLatch ambasExcluiram = new CountDownLatch(2);
        TokenAtivoRepositorio tokens = mock(TokenAtivoRepositorio.class);
        doAnswer(invocation -> {
            sessoes.clear();
            if (!bloqueioDaLinha.isHeldByCurrentThread()) {
                ambasExcluiram.countDown();
                assertTrue(ambasExcluiram.await(5, TimeUnit.SECONDS));
            }
            return null;
        }).when(tokens).deleteByUsername(usuario.getUsername());
        when(tokens.save(any(TokenAtivo.class))).thenAnswer(invocation -> {
            TokenAtivo token = invocation.getArgument(0);
            token.setId(sequencia.incrementAndGet());
            sessoes.put(token.getId(), token);
            return token;
        });
        AuthenticationManager autenticador = mock(AuthenticationManager.class);
        when(autenticador.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(
                usuario.getUsername(), null, List.of(new SimpleGrantedAuthority("PERFIL_ADMINISTRADOR"))));
        UtilitarioJwt jwt = mock(UtilitarioJwt.class);
        AtomicLong emissao = new AtomicLong();
        when(jwt.gerarToken(anyLong(), anyBoolean())).thenAnswer(invocation -> "jwt-" + emissao.incrementAndGet());
        when(jwt.extrairExpiracao(anyString())).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        AutenticacaoServico servico = new AutenticacaoServico(autenticador, usuarios, tokens,
                mock(PasswordEncoder.class), jwt, mock(TentativaLoginServico.class), mock(LotacaoRepositorio.class),
                mock(JornadaRepositorio.class), mock(SuperintendenciaEstadualRepositorio.class),
                mock(GestaoLocalRepositorio.class), mock(EscopoAcessoUsuarioServico.class),
                mock(FiltroAutenticacaoJwt.class), mock(TotpServico.class),
                new PropriedadesAutenticacao(6, 31_536_000, "Credenciais inválidas."), mock(com.example.siggtrat.servico.RevogacaoSessaoServico.class));
        ProxyFactory fabrica = new ProxyFactory(servico);
        fabrica.setProxyTargetClass(true);
        fabrica.addAdvice(new TransactionInterceptor(new TransacaoDeTeste(), new AnnotationTransactionAttributeSource()));
        AutenticacaoServico proxy = (AutenticacaoServico) fabrica.getProxy();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> login = () -> proxy.autenticar(
                    new CredenciaisLoginDTO("12345678", "senha-teste", null),
                    new MockHttpServletRequest()).getStatusCode().value();
            Future<Integer> primeira = executor.submit(login);
            Future<Integer> segunda = executor.submit(login);
            assertEquals(200, primeira.get(10, TimeUnit.SECONDS));
            assertEquals(200, segunda.get(10, TimeUnit.SECONDS));
            assertEquals(1, sessoes.size(), "Nova autenticação deve substituir a sessão anterior atomicamente.");
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static final class TransacaoDeTeste extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
