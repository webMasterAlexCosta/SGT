package com.example.siggtrat.cache;

import com.example.siggtrat.configuracao.ServicoChaveCacheRedis;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** A chave é uma regra pura; MySQL/Redis e a revogação real são verificados nos ITs. */
class ChaveCacheEscopoRevogadoTest {
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    @Test
    void trocaDeLotacaoOuJornadaNaoReutilizaConsultaDoEscopoAnterior() {
        UsuarioAplicacao usuario = usuario();
        var chave = chave(usuario);
        String antes = chave.consulta("frequencias-dia", "2026-06-10");
        Lotacao outra = new Lotacao(); outra.setId(32L); usuario.setLotacao(outra);
        String novaLotacao = chave.consulta("frequencias-dia", "2026-06-10");
        assertNotEquals(antes, novaLotacao, "Lotação anterior não pode continuar na chave da mesma identidade");
        Jornada outraJornada = new Jornada(); outraJornada.setId(42L); usuario.setJornada(outraJornada);
        assertNotEquals(novaLotacao, chave.consulta("frequencias-dia", "2026-06-10"));
    }

    @Test
    void vinculoAdicionalRevogadoMudaChaveMesmoSemMudarPerfilOuGestao() {
        UsuarioAplicacao usuario = usuario();
        usuario.setVinculosGestao(new ArrayList<>(List.of(
                new VinculoGestaoUsuarioAplicacao(usuario, usuario.getLotacao(), usuario.getJornada()))));
        var chave = chave(usuario);
        String antes = chave.usuarioAtual();
        Jornada nova = new Jornada(); nova.setId(43L);
        usuario.getVinculosGestao().get(0).setJornada(nova);
        assertNotEquals(antes, chave.usuarioAtual(), "A mesma sessão perdeu acesso à jornada anterior");
    }

    @Test
    void perfilPersistidoFazParteDaChaveMesmoComAuthoritiesAntigas() {
        UsuarioAplicacao usuario = usuario();
        var chave = chave(usuario);
        String antes = chave.usuarioAtual();
        usuario.setPerfil(PerfilAcesso.PRESIDENTE);
        assertNotEquals(antes, chave.usuarioAtual(), "Privilégios da sessão não substituem perfil vigente no cadastro");
    }

    @Test
    void ordemDeVinculosNaoCriaEscoposDiferentes() {
        UsuarioAplicacao usuario = usuario();
        Jornada outra = new Jornada(); outra.setId(44L);
        var a = new VinculoGestaoUsuarioAplicacao(usuario, usuario.getLotacao(), usuario.getJornada());
        var b = new VinculoGestaoUsuarioAplicacao(usuario, usuario.getLotacao(), outra);
        usuario.setVinculosGestao(new ArrayList<>(List.of(a, b)));
        var chave = chave(usuario);
        String antes = chave.usuarioAtual();
        usuario.setVinculosGestao(new ArrayList<>(List.of(b, a)));
        assertEquals(antes, chave.usuarioAtual());
    }

    private UsuarioAplicacao usuario() {
        UsuarioAplicacao u = new UsuarioAplicacao(); u.setId(1L); u.setUsername("cache-sintetico");
        u.setPerfil(PerfilAcesso.OPERACIONAL); u.setActive(true);
        GestaoLocal g = new GestaoLocal(); g.setId(20L); u.setGestaoLocal(g);
        Lotacao l = new Lotacao(); l.setId(31L); l.setGestaoLocal(g); u.setLotacao(l);
        Jornada j = new Jornada(); j.setId(41L); j.setGestaoLocal(g); u.setJornada(j);
        return u;
    }

    private ServicoChaveCacheRedis chave(UsuarioAplicacao u) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                u.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_OPERACIONAL"))));
        UsuarioAplicacaoRepositorio r = mock(UsuarioAplicacaoRepositorio.class);
        when(r.findByUsername(u.getUsername())).thenReturn(Optional.of(u));
        return new ServicoChaveCacheRedis(r, org.mockito.Mockito.mock(com.example.siggtrat.servico.RevisaoFontesPainelServico.class));
    }
}
