package com.example.siggtrat.configuracao;

import com.example.siggtrat.configuracao.propriedades.PropriedadesCors;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import com.example.siggtrat.seguranca.TratadorAcessoNegadoRest;
import com.example.siggtrat.seguranca.PontoEntradaAutenticacaoRest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class ConfiguracaoSeguranca {

    private final FiltroAutenticacaoJwt jwtFilter;
    private final FiltroLimitacaoRequisicoes rateLimitingFilter;
    private final PontoEntradaAutenticacaoRest authenticationEntryPoint;
    private final TratadorAcessoNegadoRest accessDeniedHandler;
    private final List<String> allowedOrigins;

    public ConfiguracaoSeguranca(
            FiltroAutenticacaoJwt jwtFilter,
            FiltroLimitacaoRequisicoes rateLimitingFilter,
            PontoEntradaAutenticacaoRest authenticationEntryPoint,
            TratadorAcessoNegadoRest accessDeniedHandler,
            PropriedadesCors corsProperties
    ) {
        this.jwtFilter = jwtFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.allowedOrigins = corsProperties.listarOrigensPermitidas();
    }

    @Bean
    public SecurityFilterChain cadeiaFiltrosSeguranca(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> {
                    CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
                    repositorio.setCookiePath("/");
                    CsrfTokenRequestAttributeHandler manipulador = new CsrfTokenRequestAttributeHandler();
                    manipulador.setCsrfRequestAttributeName("_csrf");
                    csrf.csrfTokenRepository(repositorio).csrfTokenRequestHandler(manipulador)
                            // Restaurar o mesmo JWT em cada requisição não é um
                            // novo login. AutenticacaoServico renova o CSRF ao
                            // autenticar, trocar a senha inicial e sair.
                            .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy());
                })

                .cors(cors ->
                        cors.configurationSource(fonteConfiguracaoCors())
                )

                .headers(headers -> headers
                        .contentTypeOptions(contentType -> {})
                        .xssProtection(xss ->
                                xss.headerValue(
                                        org.springframework.security.web.header.writers
                                                .XXssProtectionHeaderWriter
                                                .HeaderValue
                                                .ENABLED_MODE_BLOCK
                                )
                        )
                        .cacheControl(cache -> {})
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .httpStrictTransportSecurity(hsts ->
                                hsts
                                        .includeSubDomains(true)
                                        .maxAgeInSeconds(31536000)
                        )
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data:; " +
                                        "object-src 'none'; " +
                                        "base-uri 'self'; " +
                                        "connect-src 'self'"
                                )
                        )
                        .referrerPolicy(referrer ->
                                referrer.policy(
                                        ReferrerPolicyHeaderWriter
                                                .ReferrerPolicy
                                                .STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                                )
                        )
                        .permissionsPolicyHeader(permissions ->
                                permissions.policy(
                                        "camera=(), " +
                                        "microphone=(), " +
                                        "geolocation=(), " +
                                        "payment=(), " +
                                        "usb=()"
                                )
                        )
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                // Mantém o principal somente nesta requisição, inclusive no redispatch
                // ASYNC do streaming. Não cria HttpSession nem dispensa autorização.
                .securityContext(context -> context.securityContextRepository(
                        new RequestAttributeSecurityContextRepository()))

                .authorizeHttpRequests(auth -> auth

                        /*
                         * =====================================================
                         * BLOQUEIOS GERAIS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.TRACE,
                                "/**"
                        ).denyAll()

                        /*
                         * =====================================================
                         * ARQUIVOS PÚBLICOS / FRONTEND
                         * =====================================================
                         */

                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/*.ico",
                                "/*.png",
                                "/*.svg",
                                "/*.json",
                                "/*.js",
                                "/*.css"
                        ).permitAll()

                        /*
                         * =====================================================
                         * AUTENTICAÇÃO
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/autenticacao/autenticar",
                                "/api/autenticacao/cadastrar",
                                "/api/autenticacao/encerrar-sessao",
                                "/api/autenticacao/recuperar-senha/solicitar",
                                "/api/autenticacao/recuperar-senha/confirmar"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/autenticacao/csrf",
                                "/api/autenticacao/superintendencias",
                                "/api/autenticacao/gestoes-locais",
                                "/api/autenticacao/recuperar-senha/validar"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/autenticacao/alterar-senha-inicial"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/usuarios/meu-perfil"
                        ).authenticated()

                        /*
                         * =====================================================
                         * PAINEL
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/painel/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL
                        )

                        /*
                         * =====================================================
                         * RELATÓRIOS
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/relatorios/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL
                        )

                        /*
                         * =====================================================
                         * EMPRESAS CONTRATADAS
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/fornecedores/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_GESTOR_LOCAL
                        )

                        /*
                         * =====================================================
                         * FREQUÊNCIAS
                         * =====================================================
                         */

                        .requestMatchers(HttpMethod.GET, "/api/frequencias/fechamentos/**")
                        .hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL)

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/frequencias",
                                "/api/frequencias/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/frequencias/intervalo"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        .requestMatchers(
                                "/api/frequencias/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        /*
                         * =====================================================
                         * OCORRÊNCIAS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/ocorrencias/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        .requestMatchers(
                                "/api/ocorrencias/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_GESTOR_LOCAL
                        )

                        /*
                         * =====================================================
                         * PESSOAS / VÍNCULOS CANÔNICOS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/pessoas/*/dados-restritos"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/pessoas/**",
                                "/api/vinculos-contratuais/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        .requestMatchers(
                                "/api/pessoas/**",
                                "/api/vinculos-contratuais/**"
                        ).hasAuthority(ConstantesDominio.PERFIL_GESTOR_LOCAL)

                        /*
                         * =====================================================
                         * EMPREGADOS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/empregados/filtrados-frequencia"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        /*
                         * =====================================================
                         * STATUS DE EMPREGADOS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/status-empregados/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        .requestMatchers(
                                "/api/status-empregados/**"
                        ).hasAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR
                        )

                        /*
                         * =====================================================
                         * HISTÓRICO / EMPREGADOS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/historicos-empregados/**",
                                "/api/empregados/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL
                        )

                        /*
                         * =====================================================
                         * CONTRATOS
                         * =====================================================
                         *
                         * PRESIDENTE:
                         * - pode consultar todos os contratos;
                         * - pode consultar qualquer estado;
                        * - não recebe permissão para criar/editar/excluir.
                         */

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/contratos/*/documentos/*/permissoes",
                                "/api/contratos/*/documentos/*/permissoes/*/revogar"
                        ).hasAuthority(ConstantesDominio.PERFIL_ADMINISTRADOR)

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/contratos/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        /*
                         * Escrita negocial em contratos pertence ao Gestor Local.
                         */
                        .requestMatchers(
                                "/api/contratos/**"
                        ).hasAuthority(ConstantesDominio.PERFIL_GESTOR_LOCAL)

                        /*
                         * =====================================================
                         * CALENDÁRIO DE EVENTOS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/calendario-eventos/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        .requestMatchers(
                                "/api/calendario-eventos/**"
                        ).hasAuthority(ConstantesDominio.PERFIL_GESTOR_LOCAL)

                        /*
                         * =====================================================
                         * EXCEÇÕES / ADICIONAIS DE FREQUÊNCIA
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/frequencias-excecoes/**",
                                "/api/frequencias-adicionais/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        .requestMatchers(
                                "/api/frequencias-excecoes/**",
                                "/api/frequencias-adicionais/**"
                        ).hasAuthority(ConstantesDominio.PERFIL_GESTOR_LOCAL)

                        /*
                         * =====================================================
                         * USUÁRIOS
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/usuarios/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL
                        )

                        /*
                         * =====================================================
                         * GESTORES ESTADUAIS / GESTÕES LOCAIS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/superintendencias/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/gestoes-locais/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        .requestMatchers(
                                "/api/superintendencias/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR
                        )

                        .requestMatchers(
                                "/api/gestoes-locais/**"
                        ).hasAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR
                        )

                        /*
                         * =====================================================
                         * LOTAÇÕES / JORNADAS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/lotacoes/**",
                                "/api/jornadas/**",
                                "/api/jornadas/*/regras-adicionais"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        .requestMatchers(
                                "/api/lotacoes/**",
                                "/api/jornadas/**",
                                "/api/regras-adicionais/**"
                        ).hasAuthority(ConstantesDominio.PERFIL_GESTOR_LOCAL)

                        /*
                         * =====================================================
                         * CARGOS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/cargos/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL,
                                ConstantesDominio.PERFIL_OPERACIONAL
                        )

                        /*
                         * Cargo pertence ao catálogo da Gestão Local. O serviço já
                         * valida o escopo organizacional em toda operação de escrita.
                         */
                        .requestMatchers(
                                "/api/cargos/**"
                        ).hasAuthority(
                                ConstantesDominio.PERFIL_GESTOR_LOCAL
                        )

                        /*
                         * =====================================================
                         * AUDITORIA
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/registros-auditoria/**"
                        ).hasAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/permissoes-excepcionais/**"
                        ).hasAnyAuthority(
                                ConstantesDominio.PERFIL_ADMINISTRADOR,
                                ConstantesDominio.PERFIL_GESTOR_NACIONAL,
                                ConstantesDominio.PERFIL_PRESIDENTE,
                                ConstantesDominio.PERFIL_GESTOR_ESTADUAL,
                                ConstantesDominio.PERFIL_GESTOR_LOCAL
                        )

                        .requestMatchers(
                                "/api/permissoes-excepcionais/**"
                        ).hasAuthority(ConstantesDominio.PERFIL_ADMINISTRADOR)

                        /*
                         * =====================================================
                         * DEMAIS ENDPOINTS DA API
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/**"
                        ).authenticated()

                        /*
                         * =====================================================
                         * ROTAS FRONTEND
                         * =====================================================
                         */

                        .anyRequest().permitAll()
                )

                /*
                 * =========================================================
                 * FILTROS
                 * =========================================================
                 */

                .addFilterBefore(
                        rateLimitingFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource fonteConfiguracaoCors() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedMethods(
                Arrays.asList(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                Arrays.asList(
                        "Authorization",
                        "Content-Type",
                        "X-Request-Id",
                        "X-Requested-With",
                        "X-Client",
                        "X-Client-Version",
                        "X-Timezone",
                        "x-client",
                        "x-timezone",
                        "Origin",
                        "Accept",
                        "X-User-Password",
                        "X-XSRF-TOKEN"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "X-Request-Id",
                        "Content-Disposition"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public PasswordEncoder codificadorSenha() {
        return new BCryptPasswordEncoder();
    }

    /** Evita registro Servlet duplicado dos filtros que já pertencem à cadeia Security. */
    @Bean
    public FilterRegistrationBean<FiltroAutenticacaoJwt> desabilitarRegistroServletJwt(
            FiltroAutenticacaoJwt filtro) {
        FilterRegistrationBean<FiltroAutenticacaoJwt> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<FiltroLimitacaoRequisicoes> desabilitarRegistroServletLimitacao(
            FiltroLimitacaoRequisicoes filtro) {
        FilterRegistrationBean<FiltroLimitacaoRequisicoes> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public AuthenticationManager gerenciadorAutenticacao(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }
}
