package com.example.siggtrat.configuracao;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdministradorPadraoTest {
    private final UsuarioAplicacaoRepositorio repo=mock(UsuarioAplicacaoRepositorio.class);
    private final PasswordEncoder encoder=mock(PasswordEncoder.class);
    private CommandLineRunner inicializador(String senha) {
        var tm=mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        var em=mock(EntityManager.class);var lock=mock(Query.class);when(em.createNativeQuery(anyString())).thenReturn(lock);
        var runner=assertDoesNotThrow(()->(CommandLineRunner)Class.forName("com.example.siggtrat.configuracao.InicializadorAdministradorPadrao")
                .getConstructor(UsuarioAplicacaoRepositorio.class,PasswordEncoder.class,PlatformTransactionManager.class,EntityManager.class)
                .newInstance(repo,encoder,tm,em));
        ReflectionTestUtils.setField(runner,"senhaInicial",senha);return runner;
    }
    @Test void preservaAdministradorExistenteSemRedefinirCredenciais() throws Exception {
        var admin=new UsuarioAplicacao();admin.setUsername("admin-personalizado");admin.setPassword("hash-existente");admin.setActive(false);
        when(repo.findByPerfilOrderByUsernameAsc(PerfilAcesso.ADMINISTRADOR)).thenReturn(List.of(admin));
        inicializador("").run();assertEquals("hash-existente",admin.getPassword());assertFalse(admin.getActive());
        verifyNoInteractions(encoder);verify(repo,never()).save(any());
    }
    @Test void criaAdminPadraoComSenhaExternaETrocaObrigatoria() throws Exception {
        when(repo.findByPerfilOrderByUsernameAsc(PerfilAcesso.ADMINISTRADOR)).thenReturn(List.of());
        when(repo.findByUsername("adm")).thenReturn(Optional.empty());when(repo.findByMatricula("90000001")).thenReturn(Optional.empty());
        when(encoder.encode("Ficticia!Inicial2026")).thenReturn("hash-teste");
        inicializador("Ficticia!Inicial2026").run();
        verify(repo).save(argThat(u->u.getPerfil()==PerfilAcesso.ADMINISTRADOR && "adm".equals(u.getUsername())
                && "90000001".equals(u.getMatricula()) && "hash-teste".equals(u.getPassword())
                && Boolean.TRUE.equals(u.getForceChangePassword()) && Boolean.TRUE.equals(u.getActive())));
    }
    @Test void semAdministradorNemSenhaRecusaCriacao() {
        when(repo.findByPerfilOrderByUsernameAsc(PerfilAcesso.ADMINISTRADOR)).thenReturn(List.of());
        var r=inicializador("");assertThrows(IllegalStateException.class,()->r.run());verify(repo,never()).save(any());
    }
    @Test void naoPromoveContaQueOcupaIdentificadorPadrao() {
        when(repo.findByPerfilOrderByUsernameAsc(PerfilAcesso.ADMINISTRADOR)).thenReturn(List.of());
        when(repo.findByUsername("adm")).thenReturn(Optional.of(new UsuarioAplicacao()));
        var r=inicializador("Ficticia!Inicial2026");assertThrows(IllegalStateException.class,()->r.run());verify(repo,never()).save(any());
    }
}
