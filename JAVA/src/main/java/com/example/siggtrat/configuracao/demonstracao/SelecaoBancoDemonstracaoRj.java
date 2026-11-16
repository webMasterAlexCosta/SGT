package com.example.siggtrat.configuracao.demonstracao;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Seleciona o banco de demonstração depois do .env e antes de criar o DataSource. */
public final class SelecaoBancoDemonstracaoRj implements EnvironmentPostProcessor, Ordered {
    @Override public int getOrder() { return ConfigDataEnvironmentPostProcessor.ORDER + 1; }
    @Override public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String flag=env.getProperty("APP_DEMO_RJ_ENABLED", "false").trim();
        if (!Set.of("true","false").contains(flag.toLowerCase(Locale.ROOT))) throw erro("APP_DEMO_RJ_ENABLED");
        if (!Boolean.parseBoolean(flag)) return;
        if (Arrays.stream(env.getActiveProfiles()).anyMatch(p->p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production")))
            throw erro("perfil-produtivo");
        String base=env.getRequiredProperty("spring.datasource.url");
        String url=obrigatorio(env,"APP_DEMO_RJ_DB_URL"),user=obrigatorio(env,"APP_DEMO_RJ_DB_USER"),password=obrigatorio(env,"APP_DEMO_RJ_DB_PASSWORD");
        URI demo=parse(url),normal=parse(base);
        if (!Set.of("localhost","127.0.0.1","[::1]","::1").contains(demo.getHost())
                || !demo.getPath().endsWith("_demo_rj")) throw erro("APP_DEMO_RJ_DB_URL");
        if (identidade(demo).equals(identidade(normal))) throw erro("bancos-devem-ser-distintos");
        if (Set.of("postgres","root","rdsadmin").contains(user.toLowerCase(Locale.ROOT))) throw erro("APP_DEMO_RJ_DB_USER");
        int redis;
        try { redis=Integer.parseInt(env.getProperty("APP_DEMO_RJ_REDIS_DATABASE","15")); }
        catch(NumberFormatException ex){throw erro("APP_DEMO_RJ_REDIS_DATABASE");}
        int normalRedis=env.getProperty("spring.data.redis.database",Integer.class,0);
        if(redis<0 || redis>15 || redis==normalRedis) throw erro("bancos-redis-devem-ser-distintos");
        Map<String,Object> selected=new LinkedHashMap<>();
        selected.put("app.demo-rj.enabled","true");selected.put("spring.datasource.url",url);
        selected.put("spring.datasource.username",user);selected.put("spring.datasource.password",password);
        selected.put("spring.datasource.driver-class-name","org.postgresql.Driver");
        selected.put("spring.data.redis.database",redis);
        // Não executar cargas opcionais globais no corpus isolado.
        for(String seed:List.of("usuarios-globais","gestores-se-gestao-local","jornadas-operacionais","unidades-operacionais"))
            selected.put("app.seed."+seed+"-enabled","false");
        env.getPropertySources().addFirst(new MapPropertySource("sgt-demonstracao-rj",selected));
    }
    private static String obrigatorio(ConfigurableEnvironment env,String key) {
        String value=env.getProperty(key,"");if(value.isBlank())throw erro(key);return value;
    }
    private static URI parse(String url) {
        try {
            if(!url.startsWith("jdbc:postgresql://"))throw erro("url-postgresql");
            URI uri=URI.create(url.substring(5));
            if(uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null
                    || uri.getPath()==null || !uri.getPath().matches("/[A-Za-z0-9_]+"))throw erro("url-postgresql");
            if(uri.getRawQuery()!=null)for(String param:uri.getRawQuery().split("&")) {
                String key=URLDecoder.decode(param.split("=",2)[0],StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                if(key.contains("password") || Set.of("user","passfile","service","servicefile","currentschema").contains(key))throw erro("url-postgresql");
            }
            return uri;
        }catch(IllegalArgumentException ex){throw erro("url-postgresql");}
    }
    private static String identidade(URI uri) {
        String host=uri.getHost().toLowerCase(Locale.ROOT);
        if(Set.of("localhost","127.0.0.1","[::1]","::1").contains(host))host="loopback";
        return host+":"+(uri.getPort()<0?5432:uri.getPort())+uri.getPath();
    }
    private static IllegalStateException erro(String campo){return new IllegalStateException("SGT_DEMO_RJ_CONFIG_INVALID: "+campo);}
}
