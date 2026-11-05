package com.example.siggtrat.seguranca;

import com.example.siggtrat.configuracao.propriedades.PropriedadesSegurancaJwt;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.servico.DetalhesUsuarioPersonalizadoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.example.siggtrat.utilitario.UtilitarioJwt;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessaoJwtSegurancaRegressaoTest {
    private final TokenAtivoRepositorio tokens = mock(TokenAtivoRepositorio.class);
    private final UsuarioAplicacaoRepositorio usuarios = mock(UsuarioAplicacaoRepositorio.class);
    private final UtilitarioJwt jwt = mock(UtilitarioJwt.class);
    private final UsuarioAplicacao usuario = new UsuarioAplicacao();
    private final FiltroAutenticacaoJwt filtro = new FiltroAutenticacaoJwt(jwt,
            new DetalhesUsuarioPersonalizadoServico(usuarios), tokens,
            new PropriedadesSegurancaJwt(30, 10, 60_000, 60_000,
                    new PropriedadesSegurancaJwt.Limpeza("0 0 * * * *", 100)));

    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest
    @CsvSource({"GET,/api/contratos", "POST,/api/frequencias", "POST,/api/perfil/2fa/configurar"})
    void senhaTemporariaNaoAcessaOperacoesDaApi(String metodo, String rota) throws Exception {
        prepararUsuario(true);
        AtomicBoolean executou = new AtomicBoolean();
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(requisicao(metodo, rota), resposta, (req, res) -> executou.set(true));

        assertEquals(403, resposta.getStatus());
        assertTrue(resposta.getContentAsString().contains("PASSWORD_CHANGE_REQUIRED"));
        assertFalse(executou.get(), "A API de negócio não deve executar antes da troca inicial.");
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/autenticacao/csrf", "GET,/api/autenticacao/sessao",
            "GET,/api/usuarios/meu-perfil", "POST,/api/autenticacao/alterar-senha-inicial",
            "POST,/api/autenticacao/encerrar-sessao"})
    void senhaTemporariaPreservaRotasNecessariasParaPrimeiroAcesso(String metodo, String rota) throws Exception {
        prepararUsuario(true);
        AtomicBoolean executou = new AtomicBoolean();
        filtro.doFilter(requisicao(metodo, rota), new MockHttpServletResponse(), (req, res) -> executou.set(true));
        assertTrue(executou.get());
    }

    @Test
    void guardaDePrimeiroAcessoFuncionaComContextPath() throws Exception {
        prepararUsuario(true);
        MockHttpServletRequest requisicao = requisicao("GET", "/sgt/api/contratos");
        requisicao.setContextPath("/sgt");
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        filtro.doFilter(requisicao, resposta, (req, res) -> fail("Negócio não pode executar."));
        assertEquals(403, resposta.getStatus());
    }

    @Test
    void sessaoRevogadaEmOutraInstanciaNaoEAutorizadaPeloCacheLocal() throws Exception {
        prepararUsuario(false);
        AtomicBoolean autenticado = new AtomicBoolean();
        filtro.doFilter(requisicao("GET", "/api/contratos"), new MockHttpServletResponse(),
                (req, res) -> autenticado.set(SecurityContextHolder.getContext().getAuthentication() != null));
        assertTrue(autenticado.get());

        SecurityContextHolder.clearContext();
        when(tokens.buscarPorTokenBruto("jwt-valido")).thenReturn(Optional.empty());
        MockHttpServletRequest requisicaoRevogada = requisicao("POST", "/api/frequencias");
        filtro.doFilter(requisicaoRevogada, new MockHttpServletResponse(),
                (req, res) -> autenticado.set(SecurityContextHolder.getContext().getAuthentication() != null));

        assertFalse(autenticado.get(), "Token removido no banco não pode sobreviver no cache local.");
        assertEquals("SESSION_REPLACED", requisicaoRevogada.getAttribute("AUTH_ERROR_CODE"));
    }

    @Test
    void sessaoExcluidaEntreLeituraETouchNaoExecutaMutacao() throws Exception {
        TokenAtivo token = prepararUsuario(false);
        token.setLastSeenAt(RelogioSistema.agora().minusSeconds(20));
        AtomicBoolean autenticado = new AtomicBoolean();
        MockHttpServletRequest request = requisicao("POST", "/api/frequencias");
        // O repositório retorna sua resposta padrão para o UPDATE, representando
        // zero linhas quando o token já foi excluído por outra transação.
        filtro.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> autenticado.set(SecurityContextHolder.getContext().getAuthentication() != null));
        assertFalse(autenticado.get());
        assertEquals("SESSION_REPLACED", request.getAttribute("AUTH_ERROR_CODE"));
    }

    private TokenAtivo prepararUsuario(boolean trocaObrigatoria) {
        usuario.setId(7L);
        usuario.setUsername("usuario.teste");
        usuario.setPassword("hash");
        usuario.setActive(true);
        usuario.setPerfil(PerfilAcesso.ADMINISTRADOR);
        usuario.setForceChangePassword(trocaObrigatoria);
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        when(jwt.validarToken("jwt-valido")).thenReturn(true);
        when(jwt.extrairIdUsuario("jwt-valido")).thenReturn(7L);
        TokenAtivo token = TokenAtivo.deTokenBruto("jwt-valido", usuario.getUsername(),
                RelogioSistema.agora(), RelogioSistema.agora().plusHours(1));
        token.setId(1L);
        token.setLastSeenAt(RelogioSistema.agora());
        token.setDeviceHash(UtilitarioHashSeguranca.sha256Hexadecimal("dispositivo-teste"));
        when(tokens.buscarPorTokenBruto("jwt-valido")).thenReturn(Optional.of(token));
        return token;
    }

    private MockHttpServletRequest requisicao(String metodo, String rota) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, rota);
        request.setCookies(new Cookie(ConstantesDominio.COOKIE_AUTH, "jwt-valido"),
                new Cookie(ConstantesDominio.COOKIE_DEVICE, "dispositivo-teste"));
        return request;
    }
}
