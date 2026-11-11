package com.example.sgtoperacao;

import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComissionadorAdministradorInicialTest {
    private MockEnvironment autorizado() {
        return new MockEnvironment().withProperty("sgt.admin.autorizado", "true")
                .withProperty("sgt.admin.username", "administrador.sintetico")
                .withProperty("sgt.admin.matricula", "9.100.999-9")
                .withProperty("sgt.admin.email", "admin@sgt.example.invalid")
                .withProperty("sgt.admin.password", "Senha-Sintetica@Inicial67")
                .withProperty("auth.password-min-length", "8");
    }

    @Test void exigeAutorizacaoEIdentidadeCompletaSemValoresNaFalha() {
        assertThrows(IllegalStateException.class, () -> ComissionadorAdministradorInicial.dados(new MockEnvironment()));
        for (String field : new String[]{"username", "matricula", "email", "password"}) {
            String value = field.equals("password") ? "CURTA" : "";
            var error = assertThrows(IllegalStateException.class,
                    () -> ComissionadorAdministradorInicial.dados(autorizado().withProperty("sgt.admin."+field, value)));
            assertTrue(error.getMessage().startsWith("SGT_ADMIN_INITIAL_CONFIG_INVALID"));
            assertFalse(error.getMessage().contains("Senha-Sintetica"));
        }
    }

    @Test void recusaEmailInvalidoEPasswordAcimaDoLimiteBcrypt() {
        assertThrows(IllegalStateException.class, () -> ComissionadorAdministradorInicial.dados(autorizado().withProperty("sgt.admin.email", "invalido")));
        assertThrows(IllegalStateException.class, () -> ComissionadorAdministradorInicial.dados(autorizado().withProperty("sgt.admin.password", "á".repeat(40))));
    }

    @Test void normalizaMatriculaEPreservaSenhaSemToStringSensivel() {
        var d=ComissionadorAdministradorInicial.dados(autorizado());
        assertEquals("91009999",d.matricula());
        assertFalse(d.toString().contains(d.password()));
    }

    @Test void criaApenasAdministradorAtivoComTrocaObrigatoriaESemEscopoFalso() {
        EntityManager em=mock(EntityManager.class);Query locks=mock(Query.class),identity=mock(Query.class),tables=mock(Query.class),count=mock(Query.class);
        EntityTransaction tx=mock(EntityTransaction.class);when(em.getTransaction()).thenReturn(tx);when(tx.isActive()).thenReturn(true);
        when(em.createNativeQuery(contains("advisory_xact_lock"))).thenReturn(locks);
        when(locks.getSingleResult()).thenReturn(Boolean.TRUE);
        when(em.createNativeQuery(contains("datdba"))).thenReturn(identity);when(identity.getSingleResult()).thenReturn(Boolean.TRUE);
        when(em.createNativeQuery(contains("pg_tables"))).thenReturn(tables);when(tables.getResultList()).thenReturn(java.util.List.of("usuarios"));
        when(em.createNativeQuery(startsWith("LOCK TABLE"))).thenReturn(mock(Query.class));
        when(em.createNativeQuery(startsWith("SELECT count(*)"))).thenReturn(count);when(count.getSingleResult()).thenReturn(0L);
        var password=new BCryptPasswordEncoder();
        var user=ComissionadorAdministradorInicial.criarEmTransacao(em,ComissionadorAdministradorInicial.dados(autorizado()),password);
        assertEquals(PerfilAcesso.ADMINISTRADOR,user.getPerfil());assertTrue(user.getActive());assertTrue(user.getForceChangePassword());
        assertNull(user.getGestaoLocal());assertNull(user.getSuperintendencia());assertNull(user.getLotacao());assertNull(user.getJornada());
        assertTrue(password.matches("Senha-Sintetica@Inicial67",user.getPassword()));assertFalse(user.getTwoFactorEnabled());
        verify(em).persist(user);verify(em).persist(any(ConfiguracaoSistema.class));verify(em).flush();
    }

    @Test void qualquerDadoAnteriorRecusaAntesDePersistirOuTrocarSenha() {
        EntityManager em=mock(EntityManager.class);Query locks=mock(Query.class),identity=mock(Query.class),tables=mock(Query.class),count=mock(Query.class);
        EntityTransaction tx=mock(EntityTransaction.class);when(em.getTransaction()).thenReturn(tx);when(tx.isActive()).thenReturn(true);
        when(em.createNativeQuery(contains("advisory_xact_lock"))).thenReturn(locks);when(locks.getSingleResult()).thenReturn(Boolean.TRUE);
        when(em.createNativeQuery(contains("datdba"))).thenReturn(identity);when(identity.getSingleResult()).thenReturn(Boolean.TRUE);
        when(em.createNativeQuery(contains("pg_tables"))).thenReturn(tables);when(tables.getResultList()).thenReturn(java.util.List.of("usuarios"));
        when(em.createNativeQuery(startsWith("LOCK TABLE"))).thenReturn(mock(Query.class));
        when(em.createNativeQuery(startsWith("SELECT count(*)"))).thenReturn(count);when(count.getSingleResult()).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> ComissionadorAdministradorInicial.criarEmTransacao(em,ComissionadorAdministradorInicial.dados(autorizado()),new BCryptPasswordEncoder()));
        verify(em,never()).persist(any());
    }

    @Test void naoCriaUsuarioSemTransacaoOuQuandoOutroComissionamentoDetemLock() {
        EntityManager em=mock(EntityManager.class);
        assertThrows(IllegalStateException.class, () -> ComissionadorAdministradorInicial.criarEmTransacao(em,ComissionadorAdministradorInicial.dados(autorizado()),new BCryptPasswordEncoder()));
        EntityTransaction tx=mock(EntityTransaction.class);when(em.getTransaction()).thenReturn(tx);when(tx.isActive()).thenReturn(true);
        Query lock=mock(Query.class);when(em.createNativeQuery(contains("advisory_xact_lock"))).thenReturn(lock);when(lock.getSingleResult()).thenReturn(Boolean.FALSE);
        assertThrows(IllegalStateException.class, () -> ComissionadorAdministradorInicial.criarEmTransacao(em,ComissionadorAdministradorInicial.dados(autorizado()),new BCryptPasswordEncoder()));
        verify(em,never()).persist(any());
    }
}
