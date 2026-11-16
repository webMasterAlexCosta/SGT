package com.example.siggtrat.configuracao;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Administrador padrão independe das cargas de demonstração. Nunca redefine conta existente. */
@Component
@Profile("!test")
@Order(35)
public class InicializadorAdministradorPadrao implements CommandLineRunner {
    private static final Logger LOG=LoggerFactory.getLogger(InicializadorAdministradorPadrao.class);
    private final UsuarioAplicacaoRepositorio usuarios;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transacao;
    private final EntityManager em;
    @Value("${APP_ADMIN_INITIAL_PASSWORD:}") private String senhaInicial;
    public InicializadorAdministradorPadrao(UsuarioAplicacaoRepositorio usuarios,PasswordEncoder encoder,
                                             PlatformTransactionManager tm,EntityManager em) {
        this.usuarios=usuarios;this.encoder=encoder;this.transacao=new TransactionTemplate(tm);this.em=em;
    }
    @Override public void run(String... args) {
        transacao.executeWithoutResult(tx->{
            em.createNativeQuery("select 1 from pg_advisory_xact_lock(73129670670002)").getSingleResult();
            if(!usuarios.findByPerfilOrderByUsernameAsc(PerfilAcesso.ADMINISTRADOR).isEmpty()) {
                LOG.info("SGT_ADMIN_PADRAO_EXISTENTE credenciaisPreservadas=true");return;
            }
            validarSenha(senhaInicial,"APP_ADMIN_INITIAL_PASSWORD");
            if(usuarios.findByUsername("adm").isPresent() || usuarios.findByMatricula("90000001").isPresent())
                throw new IllegalStateException("SGT_ADMIN_PADRAO_IDENTIFICADOR_OCUPADO: não é permitido promover uma conta existente");
            var u=new UsuarioAplicacao();u.setUsername("adm");u.setMatricula("90000001");u.setEmail("adm@example.test");
            u.setPerfil(PerfilAcesso.ADMINISTRADOR);u.setPassword(encoder.encode(senhaInicial));u.setActive(true);
            u.setForceChangePassword(true);u.setTwoFactorEnabled(false);usuarios.save(u);
            LOG.info("SGT_ADMIN_PADRAO_CRIADO perfil=ADMINISTRADOR trocaObrigatoria=true");
        });
    }
    public static void validarSenha(String senha,String propriedade) {
        if(senha==null || senha.length()<12 || senha.getBytes(StandardCharsets.UTF_8).length>72
                || Set.of("123456","ALTERAR@123").contains(senha.trim().toUpperCase(java.util.Locale.ROOT)))
            throw new IllegalStateException("SGT_SENHA_INICIAL_OBRIGATORIA: defina "+propriedade+" com pelo menos 12 caracteres");
    }
}
