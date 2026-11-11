package com.example.sgtoperacao;

import com.example.siggtrat.AplicacaoBackendSigGtrat;
import com.example.siggtrat.configuracao.VerificadorAmbientePostgres186;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.UtilitarioMatricula;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Comissionamento local explícito, fora do scan e do servidor HTTP do produto. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EntityScan(basePackageClasses = AplicacaoBackendSigGtrat.class)
@Import(VerificadorAmbientePostgres186.class)
public class ComissionadorAdministradorInicial {
    private static final Logger LOG = LoggerFactory.getLogger(ComissionadorAdministradorInicial.class);

    public static void main(String[] args) {
        var app = new SpringApplication(ComissionadorAdministradorInicial.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setAdditionalProfiles("sgt-admin");
        try (var context = app.run(args)) {
            Dados dados = dados(context.getEnvironment());
            EntityManager em = context.getBean(EntityManagerFactory.class).createEntityManager();
            try {
                em.getTransaction().begin();
                criarEmTransacao(em, dados, new BCryptPasswordEncoder());
                em.getTransaction().commit();
            } catch (RuntimeException error) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                String reference = UUID.randomUUID().toString();
                String point = error.getStackTrace().length == 0 ? "semStack" : error.getStackTrace()[0].toString();
                LOG.error("SGT_ADMIN_INITIAL_ABORTED referencia={} tipo={} ponto={}", reference, error.getClass().getSimpleName(), point);
                String reason = error instanceof IllegalStateException && error.getMessage() != null
                        && error.getMessage().startsWith("SGT_ADMIN_INITIAL_") ? error.getMessage() : "SGT_ADMIN_INITIAL_FAILED";
                throw new IllegalStateException(reason + "; referencia=" + reference);
            } finally { em.close(); }
            LOG.info("SGT_ADMIN_INITIAL_READY usuariosCriados=1 perfil=ADMINISTRADOR trocaObrigatoria=true");
            System.out.println("SGT_ADMIN_INITIAL_READY");
        }
    }

    @Bean static BeanFactoryPostProcessor validarAntesDaPersistencia(Environment e) {
        return factory -> {
            dados(e);
            if (!"validate".equals(e.getProperty("spring.jpa.hibernate.ddl-auto"))
                    || !"false".equals(e.getProperty("spring.jpa.generate-ddl"))
                    || !"never".equals(e.getProperty("spring.sql.init.mode"))
                    || !"none".equals(e.getProperty("spring.main.web-application-type"))
                    || !"false".equals(e.getProperty("spring.jpa.show-sql"))
                    || !"OFF".equals(e.getProperty("logging.level.org.hibernate.SQL"))
                    || !"OFF".equals(e.getProperty("logging.level.org.hibernate.orm.jdbc.bind"))) {
                throw new IllegalStateException("SGT_ADMIN_INITIAL_CONFIG_INVALID: exige validate/false/never, ausencia de HTTP e logs SQL desligados");
            }
        };
    }

    record Dados(String username, String matricula, String email, String password) {
        @Override public String toString() { return "DadosAdministradorInicial[privado]"; }
    }

    static Dados dados(Environment e) {
        Set<String> invalidas = new LinkedHashSet<>();
        if (!"true".equals(e.getProperty("sgt.admin.autorizado"))) invalidas.add("sgt.admin.autorizado");
        String username = e.getProperty("sgt.admin.username", "").trim();
        String matricula = null;
        try { matricula = UtilitarioMatricula.normalizar(e.getProperty("sgt.admin.matricula", "")); }
        catch (IllegalArgumentException error) { invalidas.add("sgt.admin.matricula"); }
        if (matricula == null) invalidas.add("sgt.admin.matricula");
        String email = e.getProperty("sgt.admin.email", "").trim();
        try {
            var address = new InternetAddress(email, true);address.validate();
            if (!email.contains("@") || !email.equals(address.getAddress()) || address.getPersonal() != null) invalidas.add("sgt.admin.email");
        } catch (AddressException error) { invalidas.add("sgt.admin.email"); }
        if (username.isBlank() || username.length() > 120 || username.chars().anyMatch(Character::isISOControl)) invalidas.add("sgt.admin.username");
        String password = e.getProperty("sgt.admin.password", "");
        int min = e.getProperty("auth.password-min-length", Integer.class, 8);
        String model = password.trim().toUpperCase(Locale.ROOT);
        if (min < 1 || min > 72 || password.isBlank() || password.length() < min
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || Set.of("123456", "ALTERAR@123").contains(model) || model.startsWith("SUBSTITUA_")) invalidas.add("sgt.admin.password");
        if (!invalidas.isEmpty()) throw new IllegalStateException("SGT_ADMIN_INITIAL_CONFIG_INVALID: " + String.join(", ", invalidas));
        return new Dados(username, matricula, email, password);
    }

    static UsuarioAplicacao criarEmTransacao(EntityManager em, Dados d, PasswordEncoder encoder) {
        if (em.getTransaction() == null || !em.getTransaction().isActive()) {
            throw new IllegalStateException("SGT_ADMIN_INITIAL_TRANSACTION_REQUIRED");
        }
        Object locked = em.createNativeQuery("SELECT pg_try_advisory_xact_lock(73129670670001)").getSingleResult();
        if (!Boolean.TRUE.equals(locked)) throw new IllegalStateException("SGT_ADMIN_INITIAL_ALREADY_RUNNING");
        Object owned = em.createNativeQuery("""
                SELECT d.datdba=r.oid AND NOT EXISTS(
                  SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE n.nspname='public' AND c.relkind IN ('r','p','S','v','m','f') AND c.relowner<>r.oid)
                  FROM pg_database d JOIN pg_roles r ON r.rolname=current_user WHERE d.datname=current_database()
                """).getSingleResult();
        if (!Boolean.TRUE.equals(owned)) throw new IllegalStateException("SGT_ADMIN_INITIAL_OWNERSHIP_REQUIRED");
        var tables = em.createNativeQuery("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename").getResultList();
        if (tables.isEmpty()) throw new IllegalStateException("SGT_ADMIN_INITIAL_JPA_SCHEMA_REQUIRED");
        var qualified = new java.util.ArrayList<String>();
        for (Object value : tables) {
            String table = String.valueOf(value);
            if (!table.matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalStateException("SGT_ADMIN_INITIAL_TABLE_REFUSED");
            qualified.add("public.\"" + table + "\"");
        }
        // O lock de transação coordena os comissionadores; locks de relação
        // também impedem INSERT/UPDATE de outro processo entre a checagem de
        // vazio e a criação. Não altera schema, dados ou privilégios existentes.
        em.createNativeQuery("LOCK TABLE " + String.join(",", qualified) + " IN SHARE ROW EXCLUSIVE MODE").executeUpdate();
        for (Object value : tables) {
            String table = String.valueOf(value);
            Number count = (Number) em.createNativeQuery("SELECT count(*) FROM public.\"" + table + "\"").getSingleResult();
            if (count.longValue() != 0) throw new IllegalStateException("SGT_ADMIN_INITIAL_NONEMPTY_DATABASE_REFUSED");
        }
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setUsername(d.username());user.setMatricula(d.matricula());user.setEmail(d.email());
        user.setPassword(encoder.encode(d.password()));user.setPerfil(PerfilAcesso.ADMINISTRADOR);
        user.setActive(true);user.setForceChangePassword(true);user.setTwoFactorEnabled(false);
        em.persist(user);
        em.persist(new ConfiguracaoSistema("bootstrap.admin.inicial.v1", "CRIADO_TROCA_OBRIGATORIA", "Comissionamento explicito de um Administrador inicial; sem contas de desenvolvimento"));
        em.flush();
        return user;
    }
}
