package com.example.siggtrat.configuracao;

import com.example.siggtrat.seguranca.InterceptadorExigirSenha;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracaoExigenciaSenha implements WebMvcConfigurer {

    private final InterceptadorExigirSenha exigirSenhaInterceptor;

    public ConfiguracaoExigenciaSenha(InterceptadorExigirSenha interceptor) {
        this.exigirSenhaInterceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(exigirSenhaInterceptor);
    }
}