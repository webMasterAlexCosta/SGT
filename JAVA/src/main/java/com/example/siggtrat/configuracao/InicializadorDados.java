package com.example.siggtrat.configuracao;

import com.example.siggtrat.utilitario.UtilitarioMatricula;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.DisponibilidadeCatalogoTemporalServico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.util.*;
import java.util.function.BooleanSupplier;

@Configuration
public class InicializadorDados {

    private static final Logger log = LoggerFactory.getLogger(InicializadorDados.class);

    private static final String TURNO_1A = "TURNO 1A";
    private static final String TURNO_1B = "TURNO 1B";
    private static final String TURNO_1C = "TURNO 1C";

    private static final String TURNO_2A = "TURNO 2A";
    private static final String TURNO_2B = "TURNO 2B";
    private static final String TURNO_2C = "TURNO 2C";

    private static final String TURNO_3A = "TURNO 3A";
    private static final String TURNO_3B = "TURNO 3B";
    private static final String TURNO_3C = "TURNO 3C";

    /**
     * Siglas iniciais, sem fechar o domínio: novas Gestões Locais continuam
     * podendo ser cadastradas normalmente pela administração.
     */
    private static final List<String> SIGLAS_GESTOES_LOCAIS_INICIAIS =
            List.of("GTRAT", "GEDIS", "GETRA");

    private static final String ENTRADA_SABADO = "06:00";
    private static final String SAIDA_SABADO = "10:00";
    private static final String UF_RIO_DE_JANEIRO = "RJ";

    private static final String VALOR_CARGA_CONCLUIDA = "CONCLUIDA";
    private static final String SEED_JORNADAS_V1 = "INTERNO_SEED_JORNADAS_OPERACIONAIS_V1";
    private static final String SEED_UNIDADES_V1 = "INTERNO_SEED_UNIDADES_OPERACIONAIS_V1";
    private static final String SEED_USUARIOS_GLOBAIS_V1 = "INTERNO_SEED_USUARIOS_GLOBAIS_V1";
    private static final String SEED_GESTORES_HIERARQUIA_V1 = "INTERNO_SEED_GESTORES_HIERARQUIA_V1";
    /** Valores anteriormente distribuídos como exemplos nunca são segredos de instalação. */
    private static final Set<String> SENHAS_INICIAIS_PUBLICAS = Set.of("123456", "ALTERAR@123");

    @Value("${app.seed.executar-apenas-uma-vez:true}")
    private boolean executarSeedsApenasUmaVez;
    private static final Set<String> MARCADORES_ERRO_PLANILHA = Set.of(
            "#VALOR!", "#N/A", "#REF!", "#DIV/0!", "#NOME?"
    );
    private static final List<UnidadeOperacional> UNIDADES_OPERACIONAIS =
            criarListaSemDuplicidade(List.of(
                    new UnidadeOperacional("CTE", "Brasília", "DF", "Brasília"),
                    new UnidadeOperacional("CTE", "Recife", "PE", "Recife"),
                    new UnidadeOperacional("CTE", "BENFICA", "RJ", "RIO DE JANEIRO"),
                    new UnidadeOperacional("CTE", "Guarulhos", "SP", "Guarulhos"),
                    new UnidadeOperacional("CTE", "Santo André", "SP", "Santo André"),
                    new UnidadeOperacional("CTE", "São José dos Campos", "SP", "São José dos Campos"),
                    new UnidadeOperacional("CTE", "Saúde", "SP", "São Paulo"),

                    new UnidadeOperacional("CTCE", "Maceió", "AL", "Maceió"),
                    new UnidadeOperacional("CTCE", "Manaus", "AM", "Manaus"),
                    new UnidadeOperacional("CTCE", "Salvador", "BA", "Salvador"),
                    new UnidadeOperacional("CTCE", "Fortaleza", "CE", "Fortaleza"),
                    new UnidadeOperacional("CTCE", "Vitória", "ES", "Serra"),
                    new UnidadeOperacional("CTCE", "Imperatriz", "MA", "Imperatriz"),
                    new UnidadeOperacional("CTCE", "São Luís", "MA", "São Luís"),
                    new UnidadeOperacional("CTCE", "Belo Horizonte", "MG", "Belo Horizonte"),
                    new UnidadeOperacional("CTCE", "Contagem", "MG", "Contagem"),
                    new UnidadeOperacional("CTCE", "Campo Grande", "MS", "Campo Grande"),
                    new UnidadeOperacional("CTCE", "Cuiabá", "MT", "Várzea Grande"),
                    new UnidadeOperacional("CTCE", "Belém", "PA", "Ananindeua"),
                    new UnidadeOperacional("CTCE", "Marabá", "PA", "Marabá"),
                    new UnidadeOperacional("CTCE", "João Pessoa", "PB", "João Pessoa"),
                    new UnidadeOperacional("CTCE", "Jaboatão", "PE", "Jaboatão dos Guararapes"),
                    new UnidadeOperacional("CTCE", "Teresina", "PI", "Teresina"),
                    new UnidadeOperacional("CTCE", "Curitiba", "PR", "Curitiba"),
                    new UnidadeOperacional("CTCE", "Londrina", "PR", "Londrina"),
                    new UnidadeOperacional("CTCE", "Pinhais", "PR", "Pinhais"),
                    new UnidadeOperacional("CTCE", "CIDADE NOVA", "RJ", "RIO DE JANEIRO"),
                    new UnidadeOperacional("CTCE", "Natal", "RN", "Natal"),
                    new UnidadeOperacional("CTCE", "Porto Alegre", "RS", "Porto Alegre"),
                    new UnidadeOperacional("CTCE", "Florianópolis", "SC", "São José"),
                    new UnidadeOperacional("CTCE", "Bauru", "SP", "Bauru"),
                    new UnidadeOperacional("CTCE", "Complexo Valinhos", "SP", "Valinhos"),
                    new UnidadeOperacional("CTCE", "Indaiatuba", "SP", "Indaiatuba"),
                    new UnidadeOperacional("CTCE", "Mooca", "SP", "São Paulo"),
                    new UnidadeOperacional("CTCE", "Presidente Prudente", "SP", "Presidente Prudente"),
                    new UnidadeOperacional("CTCE", "Ribeirão Preto", "SP", "Ribeirão Preto"),
                    new UnidadeOperacional("CTCE", "Santo Amaro", "SP", "São Paulo"),
                    new UnidadeOperacional("CTCE", "São José do Rio Preto", "SP", "São José do Rio Preto"),
                    new UnidadeOperacional("CTCE", "Sorocaba", "SP", "Sorocaba"),
                    new UnidadeOperacional("CTCE", "Vila Maria", "SP", "São Paulo"),

                    new UnidadeOperacional("TECA", "Brasília", "DF", "Brasília"),
                    new UnidadeOperacional("TECA", "Aeroporto RPN", "PR", "São José dos Pinhais"),
                    new UnidadeOperacional("TECA", "Guarulhos", "SP", "Guarulhos"),
                    new UnidadeOperacional("TECA", "Jaguaré", "SP", "São Paulo"),
                    new UnidadeOperacional("TECA", "Rodoanel", "SP", "Cajamar")
            ));

    @Bean
    @Order(10)
    CommandLineRunner initStatus(
            StatusEmpregadoRepositorio repositorio,
            PlatformTransactionManager transactionManager
    ) {
        return args -> executarEmTransacao(transactionManager, () -> {
            for (String descricao : List.of(
                    ConstantesDominio.STATUS_ATIVO,
                    ConstantesDominio.STATUS_INATIVO,
                    ConstantesDominio.STATUS_FERIAS,
                    ConstantesDominio.STATUS_AFASTADO_INSS
            )) {
                if (repositorio.findByDescricaoIgnoreCase(descricao).isEmpty()) {
                    repositorio.save(new StatusEmpregado(descricao));
                }
            }
        });
    }

    @Bean
    @Order(20)
    CommandLineRunner initConfiguracoesSistema(
            ConfiguracaoSistemaRepositorio repositorio,
            PlatformTransactionManager transactionManager
    ) {
        return args -> executarEmTransacao(transactionManager, () -> {
            garantirConfiguracao(
                    repositorio,
                    "HORAS_DIA_TRABALHO",
                    "8",
                    "Quantidade de horas que compõem 1 dia de trabalho padrão"
            );
            garantirConfiguracao(
                    repositorio,
                    "HORA_INICIO_NOTURNO",
                    "22:00",
                    "Horário em que se inicia a contagem do adicional noturno"
            );
            garantirConfiguracao(
                    repositorio,
                    "HORA_FIM_NOTURNO",
                    "05:00",
                    "Horário em que termina o período de adicional noturno"
            );
            garantirConfiguracao(
                    repositorio,
                    "MINUTOS_HORA_NOTURNA",
                    "52.5",
                    "Minutos que equivalem a 1 hora contábil no período noturno"
            );
        });
    }

    @Bean
    @Order(30)
    CommandLineRunner initEstruturaOrganizacional(
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            PlatformTransactionManager transactionManager
    ) {
        return args -> executarEmTransacao(transactionManager, () -> {
            int sesCriadas = 0;
            int gestoesLocaisCriadas = 0;

            for (String uf : ConstantesDominio.UFS_BRASIL) {
                SuperintendenciaEstadual se = superintendenciaRepositorio.findByUfIgnoreCase(uf)
                        .orElseGet(() -> new SuperintendenciaEstadual("SE/" + uf, uf));

                boolean novaSe = se.getId() == null;
                if (novaSe) {
                    se.setNome("SE/" + uf);
                    se.setUf(uf);
                    se.setAtivo(true);
                    sesCriadas++;
                } else {
                    if (se.getNome() == null || se.getNome().isBlank()) {
                        se.setNome("SE/" + uf);
                    }
                    if (se.getUf() == null || se.getUf().isBlank()) {
                        se.setUf(uf);
                    }
                }
                se = superintendenciaRepositorio.save(se);

                for (String sigla : SIGLAS_GESTOES_LOCAIS_INICIAIS) {
                    String codigo = sigla + "-" + uf;
                    Optional<GestaoLocal> existente = gestaoLocalRepositorio
                            .findBySuperintendenciaIdAndCodigoIgnoreCase(se.getId(), codigo);
                    if (existente.isPresent()) continue;

                    GestaoLocal gestaoLocal = new GestaoLocal();
                    gestaoLocal.setNome(sigla + "/" + uf);
                    gestaoLocal.setCodigo(codigo);
                    gestaoLocal.setSuperintendencia(se);
                    gestaoLocal.setAtivo(true);
                    gestaoLocalRepositorio.save(gestaoLocal);
                    gestoesLocaisCriadas++;
                }
            }

            log.info(
                    "Estrutura organizacional concluída: {} Gestão(ões) Estadual(is) criada(s) e {} Gestão(ões) Local(is) criada(s).",
                    sesCriadas,
                    gestoesLocaisCriadas
            );
        });
    }

    @Bean
    @Order(32)
    @ConditionalOnProperty(
            name = "app.seed.jornadas-operacionais-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    CommandLineRunner initJornadasOperacionais(
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            JornadaRepositorio jornadaRepositorio,
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            DisponibilidadeCatalogoTemporalServico disponibilidade,
            PlatformTransactionManager transactionManager
    ) {
        return args -> executarCargaTransacionalUmaVez(
                configuracaoRepositorio,
                transactionManager,
                SEED_JORNADAS_V1,
                "Jornadas operacionais padrão",
                executarSeedsApenasUmaVez,
                () -> jornadasOperacionaisCompletas(
                        superintendenciaRepositorio, gestaoLocalRepositorio, jornadaRepositorio),
                () -> {
                    int criadas = 0;
                    int existentes = 0;

                    for (String uf : ConstantesDominio.UFS_BRASIL) {
                        SuperintendenciaEstadual se = exigirSuperintendencia(superintendenciaRepositorio, uf);
                        for (GestaoLocal gestaoLocal : listarGestoesLocais(gestaoLocalRepositorio, se)) {
                            for (DefinicaoJornada definicao : DEFINICOES_JORNADAS) {
                                boolean existia = jornadaRepositorio
                                        .findByDescricaoIgnoreCaseAndGestaoLocalId(definicao.descricao(), gestaoLocal.getId())
                                        .isPresent();

                                garantirJornada(jornadaRepositorio, gestaoLocal, definicao, disponibilidade);

                                if (existia) {
                                    existentes++;
                                } else {
                                    criadas++;
                                }
                            }
                        }
                    }

                    log.info(
                            "Carga de jornadas concluída: {} criada(s) e {} já existente(s).",
                            criadas,
                            existentes
                    );
                });
    }

    @Bean
    @Order(35)
    @ConditionalOnProperty(
            name = "app.seed.unidades-operacionais-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    CommandLineRunner initUnidadesOperacionais(
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            LotacaoRepositorio lotacaoRepositorio,
            JornadaRepositorio jornadaRepositorio,
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            DisponibilidadeCatalogoTemporalServico disponibilidade,
            PlatformTransactionManager transactionManager
    ) {
        return args -> executarCargaTransacionalUmaVez(
                configuracaoRepositorio,
                transactionManager,
                SEED_UNIDADES_V1,
                "Lotações operacionais de desenvolvimento",
                executarSeedsApenasUmaVez,
                () -> unidadesOperacionaisCompletas(
                        superintendenciaRepositorio, gestaoLocalRepositorio, lotacaoRepositorio),
                () -> {
                    int criadas = 0;
                    int existentes = 0;

                    for (UnidadeOperacional unidade : UNIDADES_OPERACIONAIS) {
                        SuperintendenciaEstadual se = exigirSuperintendencia(superintendenciaRepositorio, unidade.uf());
                        GestaoLocal gestaoLocal = exigirGestaoLocal(gestaoLocalRepositorio, se);
                        List<Jornada> jornadas = carregarJornadasIniciais(jornadaRepositorio, gestaoLocal, disponibilidade);
                        String nomeLotacao = unidade.nomeLotacao();

                        boolean existia = lotacaoRepositorio
                                .findByNomeIgnoreCaseAndGestaoLocalId(nomeLotacao, gestaoLocal.getId())
                                .isPresent();

                        garantirLotacao(
                                lotacaoRepositorio,
                                nomeLotacao,
                                unidade.municipio(),
                                gestaoLocal,
                                jornadas,
                                disponibilidade
                        );

                        if (existia) {
                            existentes++;
                        } else {
                            criadas++;
                        }
                    }

                    log.info(
                            "Carga de unidades operacionais concluída: {} unidade(s) cadastrada(s), "
                                    + "sendo {} criada(s) e {} já existente(s).",
                            UNIDADES_OPERACIONAIS.size(),
                            criadas,
                            existentes
                    );
                });
    }

    @Bean
    @Order(36)
    @ConditionalOnProperty(
            name = "app.seed.usuarios-globais-enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    CommandLineRunner initUsuariosGlobais(
            UsuarioAplicacaoRepositorio userRepositorio,
            PasswordEncoder passwordEncoder,
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            PlatformTransactionManager transactionManager,
            @Value("${app.seed.usuarios-globais-initial-password:}") String senhaInicial,
            @Value("${auth.password-min-length:8}") int tamanhoMinimoSenha
    ) {
        return args -> executarCargaTransacionalUmaVez(
                configuracaoRepositorio,
                transactionManager,
                SEED_USUARIOS_GLOBAIS_V1,
                "Usuários globais de desenvolvimento",
                executarSeedsApenasUmaVez,
                () -> usuariosGlobaisCompletos(userRepositorio),
                () -> {
                    validarSenhaInicial(senhaInicial, tamanhoMinimoSenha,
                            "APP_SEED_USUARIOS_GLOBAIS_INITIAL_PASSWORD");

                    String senhaCriptografada = passwordEncoder.encode(senhaInicial);
                    boolean adminExistia = userRepositorio.findByUsername("adm").isPresent();
                    boolean gestorNacionalExistia = userRepositorio.findByUsername("gestorNacional").isPresent();
                    boolean presidenteExistia = userRepositorio.findByUsername("presidente").isPresent();

                    garantirUsuarioGlobal(
                            userRepositorio,
                            "adm",
                            "90000001",
                            "adm@correios.com.br",
                            PerfilAcesso.ADMINISTRADOR,
                            senhaCriptografada
                    );
                    garantirUsuarioGlobal(
                            userRepositorio,
                            "gestorNacional",
                            "90000002",
                            "gestor.nacional@correios.com.br",
                            PerfilAcesso.GESTOR_NACIONAL,
                            senhaCriptografada
                    );
                    garantirUsuarioGlobal(
                            userRepositorio,
                            "presidente",
                            "90000003",
                            "presidente@correios.com.br",
                            PerfilAcesso.PRESIDENTE,
                            senhaCriptografada
                    );

                    int criados = (adminExistia ? 0 : 1) + (gestorNacionalExistia ? 0 : 1)
                            + (presidenteExistia ? 0 : 1);
                    log.info(
                            "Carga de usuários globais concluída: {} criado(s) e {} já existente(s).",
                            criados,
                            3 - criados
                    );
                });
    }

    @Bean
    @Order(37)
    @ConditionalOnProperty(
            name = "app.seed.gestores-se-gestao-local-enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    CommandLineRunner initUsuariosGestoresSeEGestaoLocal(
            UsuarioAplicacaoRepositorio userRepositorio,
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            PasswordEncoder passwordEncoder,
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            PlatformTransactionManager transactionManager,
            @Value("${app.seed.gestores-initial-password:}") String senhaInicial,
            @Value("${auth.password-min-length:8}") int tamanhoMinimoSenha
    ) {
        return args -> executarCargaTransacionalUmaVez(
                configuracaoRepositorio,
                transactionManager,
                SEED_GESTORES_HIERARQUIA_V1,
                "Gestores Estaduais e Locais de desenvolvimento",
                executarSeedsApenasUmaVez,
                () -> gestoresHierarquiaCompletos(
                        userRepositorio, superintendenciaRepositorio, gestaoLocalRepositorio),
                () -> {
                    validarSenhaInicial(senhaInicial, tamanhoMinimoSenha,
                            "APP_SEED_GESTORES_INITIAL_PASSWORD");

                    String senhaCriptografada = passwordEncoder.encode(senhaInicial);
                    int criados = 0;
                    int existentes = 0;

                    for (int indice = 0; indice < ConstantesDominio.UFS_BRASIL.size(); indice++) {
                        String uf = ConstantesDominio.UFS_BRASIL.get(indice);
                        SuperintendenciaEstadual se = exigirSuperintendencia(superintendenciaRepositorio, uf);
                        List<GestaoLocal> gestoesLocais = listarGestoesIniciaisParaUsuarios(gestaoLocalRepositorio, se);
                        if (gestoesLocais.size() != SIGLAS_GESTOES_LOCAIS_INICIAIS.size()) {
                            throw new IllegalStateException("As Gestões Locais iniciais de SE/" + uf
                                    + " precisam estar completas antes do seed de usuários.");
                        }

                        // CORREÇÃO: Utilizando logins distintos para não haver colisão.
                        String loginSe = nomeUsuarioGestorEstadual(uf);
                        String matriculaSe = String.format("91%06d", indice + 1);
                        boolean seExistia = userRepositorio.findByMatricula(matriculaSe).isPresent()
                                || userRepositorio.findByUsername(loginSe).isPresent();
                        garantirUsuarioGestor(
                                userRepositorio,
                                loginSe,
                                matriculaSe,
                                "se." + uf.toLowerCase(Locale.ROOT) + "@correios.com.br",
                                PerfilAcesso.GESTOR_ESTADUAL,
                                se,
                                null,
                                senhaCriptografada
                        );
                        if (seExistia) existentes++; else criados++;

                        for (int indiceLocal = 0; indiceLocal < gestoesLocais.size(); indiceLocal++) {
                            GestaoLocal gestaoLocal = gestoesLocais.get(indiceLocal);
                            String sigla = siglaGestaoLocal(gestaoLocal);
                            String loginGestaoLocal = nomeUsuarioGestaoLocal(sigla, uf);
                            String matriculaGestaoLocal = matriculaGestorLocal(sigla, indice);
                            boolean gestaoLocalExistia = userRepositorio.findByMatricula(matriculaGestaoLocal).isPresent()
                                    || userRepositorio.findByUsername(loginGestaoLocal).isPresent();
                            garantirUsuarioGestor(
                                    userRepositorio,
                                    loginGestaoLocal,
                                    matriculaGestaoLocal,
                                    "gestor.local." + sigla.toLowerCase(Locale.ROOT) + "."
                                            + uf.toLowerCase(Locale.ROOT) + "@correios.com.br",
                                    PerfilAcesso.GESTOR_LOCAL,
                                    se,
                                    gestaoLocal,
                                    senhaCriptografada
                            );
                            if (gestaoLocalExistia) existentes++; else criados++;
                        }
                    }

                    log.info(
                            "Carga de Gestores Estaduais/Locais concluída: {} criado(s) e {} já existente(s).",
                            criados,
                            existentes
                    );
                });
    }

    private static void validarSenhaInicial(String senha, int tamanhoMinimo, String variavel) {
        if (tamanhoMinimo < 1 || tamanhoMinimo > 72) {
            throw new IllegalStateException("O tamanho mínimo de senha configurado deve estar entre 1 e 72 caracteres.");
        }
        String modelo = senha == null ? "" : senha.trim().toUpperCase(Locale.ROOT);
        if (senha == null || senha.isBlank() || senha.length() < tamanhoMinimo
                || senha.getBytes(StandardCharsets.UTF_8).length > 72
                || SENHAS_INICIAIS_PUBLICAS.contains(modelo) || modelo.startsWith("SUBSTITUA_")) {
            throw new IllegalStateException("Defina uma senha inicial exclusiva em " + variavel
                    + ", com pelo menos " + tamanhoMinimo
                    + " caracteres e no máximo 72 bytes UTF-8. Valores públicos ou de modelo não são aceitos.");
        }
    }

    private static final List<DefinicaoJornada> DEFINICOES_JORNADAS = List.of(
            new DefinicaoJornada(
                    TURNO_1A,
                    "06:00",
                    "10:00",
                    "11:00",
                    "15:00"
            ),
            new DefinicaoJornada(
                    TURNO_1B,
                    "06:20",
                    "10:20",
                    "11:20",
                    "15:20"
            ),
            new DefinicaoJornada(
                    TURNO_1C,
                    "06:40",
                    "11:40",
                    "12:40",
                    "15:40"
            ),
            new DefinicaoJornada(
                    TURNO_2A,
                    "06:00",
                    "10:00",
                    "11:00",
                    "15:00"
            ),
            new DefinicaoJornada(
                    TURNO_2B,
                    "06:20",
                    "10:20",
                    "11:20",
                    "15:20"
            ),
            new DefinicaoJornada(
                    TURNO_2C,
                    "06:40",
                    "11:40",
                    "12:40",
                    "15:40"
            ),
            new DefinicaoJornada(
                    TURNO_3A,
                    "22:00",
                    "02:00",
                    "03:00",
                    "06:00"
            ),
            new DefinicaoJornada(
                    TURNO_3B,
                    "22:00",
                    "02:00",
                    "03:00",
                    "06:00"
            ),
            new DefinicaoJornada(
                    TURNO_3C,
                    "22:00",
                    "02:00",
                    "03:00",
                    "06:00"
            )
    );

    private static void executarEmTransacao(
            PlatformTransactionManager transactionManager,
            Runnable acao
    ) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> acao.run());
    }

    /**
     * Executa uma etapa de inicialização no máximo uma vez por
     * banco. O marcador somente é persistido na mesma transação após o sucesso.
     * Bancos criados antes desta proteção são reconhecidos pela verificação de
     * conteúdo e recebem o marcador sem regravar a massa já existente.
     */
    private static void executarCargaTransacionalUmaVez(
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            PlatformTransactionManager transactionManager,
            String chave,
            String descricao,
            boolean executarApenasUmaVez,
            BooleanSupplier cargaJaPresente,
            Runnable carga
    ) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (deveIgnorarCargaConcluida(
                    configuracaoRepositorio, chave, descricao, executarApenasUmaVez)) {
                return;
            }
            if (executarApenasUmaVez && cargaJaPresente != null && cargaJaPresente.getAsBoolean()) {
                registrarMarcadorCarga(configuracaoRepositorio, chave, descricao);
                log.info("{} já estava presente; marcador de conclusão criado sem reprocessamento.", descricao);
                return;
            }

            carga.run();
            if (executarApenasUmaVez) {
                registrarMarcadorCarga(configuracaoRepositorio, chave, descricao);
            }
        });
    }

    private static boolean deveIgnorarCargaConcluida(
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            String chave,
            String descricao,
            boolean executarApenasUmaVez
    ) {
        if (!executarApenasUmaVez || !configuracaoRepositorio.existsById(chave)) return false;
        log.info("{} já foi concluída neste banco; etapa ignorada.", descricao);
        return true;
    }

    private static void registrarMarcadorCarga(
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            String chave,
            String descricao
    ) {
        configuracaoRepositorio.save(new ConfiguracaoSistema(
                chave,
                VALOR_CARGA_CONCLUIDA,
                descricao + " — controle interno; não excluir em ambiente já populado."
        ));
    }

    private static boolean jornadasOperacionaisCompletas(
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            JornadaRepositorio jornadaRepositorio
    ) {
        for (String uf : ConstantesDominio.UFS_BRASIL) {
            Optional<SuperintendenciaEstadual> se = superintendenciaRepositorio.findByUfIgnoreCase(uf);
            if (se.isEmpty()) return false;
            List<GestaoLocal> gestoes = gestaoLocalRepositorio
                    .findBySuperintendenciaIdOrderByNomeAsc(se.get().getId());
            if (gestoes.isEmpty()) return false;
            for (GestaoLocal gestaoLocal : gestoes) {
                for (DefinicaoJornada definicao : DEFINICOES_JORNADAS) {
                    if (jornadaRepositorio.findByDescricaoIgnoreCaseAndGestaoLocalId(
                            definicao.descricao(), gestaoLocal.getId()).isEmpty()) return false;
                }
            }
        }
        return true;
    }

    private static boolean unidadesOperacionaisCompletas(
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            LotacaoRepositorio lotacaoRepositorio
    ) {
        for (UnidadeOperacional unidade : UNIDADES_OPERACIONAIS) {
            Optional<SuperintendenciaEstadual> se = superintendenciaRepositorio
                    .findByUfIgnoreCase(unidade.uf());
            if (se.isEmpty()) return false;
            Optional<GestaoLocal> gestaoLocal = gestaoLocalRepositorio
                    .findBySuperintendenciaIdAndCodigoIgnoreCase(
                            se.get().getId(), "GTRAT-" + unidade.uf().toUpperCase(Locale.ROOT));
            if (gestaoLocal.isEmpty() || lotacaoRepositorio
                    .findByNomeIgnoreCaseAndGestaoLocalId(
                            unidade.nomeLotacao(), gestaoLocal.get().getId()).isEmpty()) return false;
        }
        return true;
    }

    private static boolean usuariosGlobaisCompletos(UsuarioAplicacaoRepositorio repositorio) {
        return repositorio.findByUsername("adm").isPresent()
                && repositorio.findByUsername("gestorNacional").isPresent()
                && repositorio.findByUsername("presidente").isPresent();
    }

    private static boolean gestoresHierarquiaCompletos(
            UsuarioAplicacaoRepositorio usuarioRepositorio,
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio
    ) {
        for (String uf : ConstantesDominio.UFS_BRASIL) {
            Optional<SuperintendenciaEstadual> se = superintendenciaRepositorio.findByUfIgnoreCase(uf);
            if (se.isEmpty() || usuarioRepositorio
                    .findByUsername(nomeUsuarioGestorEstadual(uf)).isEmpty()) return false;
            List<GestaoLocal> gestoes = listarGestoesIniciaisParaUsuarios(gestaoLocalRepositorio, se.get());
            if (gestoes.size() != SIGLAS_GESTOES_LOCAIS_INICIAIS.size()) return false;
            for (GestaoLocal gestaoLocal : gestoes) {
                if (usuarioRepositorio.findByUsername(
                        nomeUsuarioGestaoLocal(siglaGestaoLocal(gestaoLocal), uf)).isEmpty()) return false;
            }
        }
        return true;
    }

    private static void garantirConfiguracao(
            ConfiguracaoSistemaRepositorio repositorio,
            String chave,
            String valor,
            String descricao
    ) {
        if (repositorio.findById(chave).isEmpty()) {
            repositorio.save(new ConfiguracaoSistema(chave, valor, descricao));
        }
    }

    private static Jornada garantirJornada(
            JornadaRepositorio repositorio,
            GestaoLocal gestaoLocal,
            DefinicaoJornada definicao,
            DisponibilidadeCatalogoTemporalServico disponibilidade
    ) {
        Optional<Jornada> existente = repositorio
                .findByDescricaoIgnoreCaseAndGestaoLocalId(definicao.descricao(), gestaoLocal.getId());
        // A inicialização cria itens ausentes; alterações de uma Jornada existente
        // pertencem ao fluxo negocial e não podem ser aplicadas ao reiniciar o JAR.
        if (existente.isPresent()) return existente.get();

        Jornada jornada = new Jornada();
        jornada.setDescricao(definicao.descricao());
        jornada.setGestaoLocal(gestaoLocal);

        sincronizarDiasDaJornada(jornada, definicao);

        Jornada criada = repositorio.save(jornada);
        disponibilidade.registrar(TipoCatalogoTemporal.JORNADA, criada.getId(), true,
                "Criação inicial da Jornada operacional", responsavelInicializacao());
        return criada;
    }

    private static void sincronizarDiasDaJornada(
            Jornada jornada,
            DefinicaoJornada definicao
    ) {
        Map<DayOfWeek, JornadaDiaSemana> diasPorSemana = new EnumMap<>(DayOfWeek.class);

        for (JornadaDiaSemana configuracao : jornada.getDias()) {
            if (configuracao.getDiaSemana() != null) {
                diasPorSemana.putIfAbsent(configuracao.getDiaSemana(), configuracao);
            }
        }

        jornada.getDias().removeIf(configuracao ->
                configuracao.getDiaSemana() == null
                        || diasPorSemana.get(configuracao.getDiaSemana()) != configuracao
        );

        for (DayOfWeek dia : DayOfWeek.values()) {
            JornadaDiaSemana configuracao = diasPorSemana.get(dia);

            if (configuracao == null) {
                configuracao = new JornadaDiaSemana();
                configuracao.setJornada(jornada);
                configuracao.setDiaSemana(dia);
                jornada.getDias().add(configuracao);
            }

            if (dia == DayOfWeek.SATURDAY) {
                configurarSabado(configuracao);
            } else if (dia == DayOfWeek.SUNDAY) {
                configurarFolga(configuracao);
            } else {
                configurarDiaUtil(configuracao, definicao);
            }
        }
    }

    private static void configurarDiaUtil(
            JornadaDiaSemana configuracao,
            DefinicaoJornada definicao
    ) {
        configuracao.setDiaTrabalho(true);
        configuracao.setEntrada1(definicao.entrada1());
        configuracao.setSaida1(definicao.saida1());
        configuracao.setEntrada2(definicao.entrada2());
        configuracao.setSaida2(definicao.saida2());
    }

    private static void configurarSabado(JornadaDiaSemana configuracao) {
        configuracao.setDiaTrabalho(true);
        configuracao.setEntrada1(ENTRADA_SABADO);
        configuracao.setSaida1(SAIDA_SABADO);
        configuracao.setEntrada2(null);
        configuracao.setSaida2(null);
    }

    private static void configurarFolga(JornadaDiaSemana configuracao) {
        configuracao.setDiaTrabalho(false);
        configuracao.setEntrada1(null);
        configuracao.setSaida1(null);
        configuracao.setEntrada2(null);
        configuracao.setSaida2(null);
    }

    private static List<Jornada> carregarJornadasIniciais(
            JornadaRepositorio repositorio,
            GestaoLocal gestaoLocal,
            DisponibilidadeCatalogoTemporalServico disponibilidade
    ) {
        List<Jornada> jornadas = new ArrayList<>();
        for (DefinicaoJornada definicao : DEFINICOES_JORNADAS) {
            jornadas.add(garantirJornada(repositorio, gestaoLocal, definicao, disponibilidade));
        }
        return jornadas;
    }

    private static Lotacao garantirLotacao(
            LotacaoRepositorio repositorio,
            String nome,
            String municipio,
            GestaoLocal gestaoLocal,
            Collection<Jornada> jornadas,
            DisponibilidadeCatalogoTemporalServico disponibilidade
    ) {
        Optional<Lotacao> existente = repositorio.findByNomeIgnoreCaseAndGestaoLocalId(nome, gestaoLocal.getId());
        if (existente.isPresent()) return existente.get();

        Lotacao lotacao = new Lotacao(nome);
        lotacao.setNome(nome);
        lotacao.setMunicipio(municipio);
        lotacao.setGestaoLocal(gestaoLocal);

        if (lotacao.getTurnos() == null) {
            lotacao.setTurnos(new ArrayList<>());
        }

        for (Jornada jornada : jornadas) {
            boolean jaAssociada = lotacao.getTurnos().stream()
                    .anyMatch(item -> Objects.equals(item.getId(), jornada.getId()));

            if (!jaAssociada) {
                lotacao.getTurnos().add(jornada);
            }
        }

        Lotacao criada = repositorio.save(lotacao);
        disponibilidade.registrar(TipoCatalogoTemporal.LOTACAO, criada.getId(), true,
                "Criação inicial da Lotação operacional", responsavelInicializacao());
        return criada;
    }

    private static UsuarioAplicacao responsavelInicializacao() {
        UsuarioAplicacao responsavel = new UsuarioAplicacao();
        responsavel.setUsername("SISTEMA_INICIALIZACAO");
        return responsavel;
    }

    private static SuperintendenciaEstadual exigirSuperintendencia(
            SuperintendenciaEstadualRepositorio repositorio,
            String uf
    ) {
        return repositorio.findByUfIgnoreCase(uf)
                .orElseThrow(() -> new IllegalStateException(
                        "SE/" + uf + " não foi inicializada."
                ));
    }

    private static GestaoLocal exigirGestaoLocal(
            GestaoLocalRepositorio repositorio,
            SuperintendenciaEstadual se
    ) {
        String codigoPadrao = "GTRAT-" + se.getUf().trim().toUpperCase(Locale.ROOT);

        return repositorio
                .findBySuperintendenciaIdAndCodigoIgnoreCase(se.getId(), codigoPadrao)
                .or(() -> repositorio.findFirstBySuperintendenciaIdOrderByIdAsc(se.getId()))
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhuma Gestão Local de " + se.getNome() + " foi inicializada."
                ));
    }

    private static List<GestaoLocal> listarGestoesLocais(
            GestaoLocalRepositorio repositorio,
            SuperintendenciaEstadual se
    ) {
        List<GestaoLocal> gestoesLocais = repositorio.findBySuperintendenciaIdOrderByNomeAsc(se.getId());
        if (gestoesLocais.isEmpty()) {
            throw new IllegalStateException("Nenhuma Gestão Local de " + se.getNome() + " foi inicializada.");
        }
        return gestoesLocais;
    }

    /**
     * Contas opcionais de desenvolvimento pertencem somente às três Gestões
     * criadas pela estrutura inicial. O cadastro organizacional permanece
     * aberto; uma Gestão adicional não autoriza criar uma conta automática,
     * reutilizar matrícula ou alcançar a conta de outra Gestão pela sigla.
     */
    private static List<GestaoLocal> listarGestoesIniciaisParaUsuarios(
            GestaoLocalRepositorio repositorio, SuperintendenciaEstadual se
    ) {
        String uf = se.getUf().trim().toUpperCase(Locale.ROOT);
        List<GestaoLocal> iniciais = new ArrayList<>();
        for (String sigla : SIGLAS_GESTOES_LOCAIS_INICIAIS) {
            repositorio.findBySuperintendenciaIdAndCodigoIgnoreCase(se.getId(), sigla + "-" + uf)
                    .ifPresent(iniciais::add);
        }
        return iniciais;
    }

    private static UsuarioAplicacao garantirUsuarioGlobal(
            UsuarioAplicacaoRepositorio repositorio,
            String username,
            String matricula,
            String email,
            PerfilAcesso perfil,
            String senhaCriptografada
    ) {
        String matriculaNormalizada = UtilitarioMatricula.normalizar(matricula);
        UsuarioAplicacao user = repositorio.findByMatricula(matriculaNormalizada)
                .or(() -> repositorio.findByUsername(username))
                .orElseGet(UsuarioAplicacao::new);
        boolean novo = user.getId() == null;

        user.setUsername(username);

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.setEmail(email);
        }
        if (user.getMatricula() == null || user.getMatricula().isBlank()) {
            user.setMatricula(matriculaNormalizada);
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(senhaCriptografada);
        }

        if (novo) {
            user.setPerfil(perfil);
            user.setSuperintendencia(null);
            user.setGestaoLocal(null);
            user.setLotacao(null);
            user.setJornada(null);
            user.setVinculosGestao(new ArrayList<>());
            user.setActive(true);
            user.setForceChangePassword(true);
            user.setFailedAttempts(0);
            user.setLockoutEnd(null);
        } else if (user.getPerfil() == null) {
            user.setPerfil(perfil);
        }

        return repositorio.save(user);
    }

    private static UsuarioAplicacao garantirUsuarioGestor(
            UsuarioAplicacaoRepositorio repositorio,
            String username,
            String matricula,
            String email,
            PerfilAcesso perfil,
            SuperintendenciaEstadual se,
            GestaoLocal gestaoLocal,
            String senhaCriptografada
    ) {
        UsuarioAplicacao user = repositorio.findByUsername(username).orElseGet(UsuarioAplicacao::new);
        boolean novo = user.getId() == null;

        user.setUsername(username);

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.setEmail(email);
        }
        if (user.getMatricula() == null || user.getMatricula().isBlank()) {
            user.setMatricula(UtilitarioMatricula.normalizar(matricula));
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(senhaCriptografada);
        }

        if (novo) {
            user.setPerfil(perfil);
            user.setSuperintendencia(se);
            user.setGestaoLocal(gestaoLocal);
            user.setLotacao(null);
            user.setJornada(null);
            user.setVinculosGestao(new ArrayList<>());
            user.setActive(true);
            user.setForceChangePassword(true);
            user.setFailedAttempts(0);
            user.setLockoutEnd(null);
        } else {
            if (user.getPerfil() == null) {
                user.setPerfil(perfil);
            }
            if (user.getSuperintendencia() == null) {
                user.setSuperintendencia(se);
            }
            if (user.getGestaoLocal() == null && gestaoLocal != null) {
                user.setGestaoLocal(gestaoLocal);
            }
        }

        return repositorio.save(user);
    }

    private static String nomeUsuarioGestorEstadual(String uf) {
        String sigla = uf.trim().toUpperCase(Locale.ROOT);
        return UF_RIO_DE_JANEIRO.equals(sigla)
                ? "gestorEstadual"
                : "gestorEstadual" + sigla;
    }

    private static String nomeUsuarioGestaoLocal(String siglaGestaoLocal, String uf) {
        return "gestorLocal" + capitalizarIdentificador(siglaGestaoLocal) + uf.trim().toUpperCase(Locale.ROOT);
    }

    private static String matriculaGestorLocal(String siglaGestaoLocal, int indiceUf) {
        int prefixo = switch (siglaGestaoLocal.toUpperCase(Locale.ROOT)) {
            case "GTRAT" -> 92;
            case "GEDIS" -> 93;
            case "GETRA" -> 94;
            default -> throw new IllegalArgumentException("O seed de usuários exige uma Gestão Local inicial.");
        };
        return String.format(Locale.ROOT, "%02d%06d", prefixo, indiceUf + 1);
    }

    private static String siglaGestaoLocal(GestaoLocal gestaoLocal) {
        String codigo = gestaoLocal.getCodigo() == null ? "LOCAL" : gestaoLocal.getCodigo().trim();
        int separador = codigo.indexOf('-');
        return (separador > 0 ? codigo.substring(0, separador) : codigo).toUpperCase(Locale.ROOT);
    }

    private static String capitalizarIdentificador(String valor) {
        String limpo = valor == null ? "Local" : valor.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return limpo.isEmpty() ? "Local" : Character.toUpperCase(limpo.charAt(0)) + limpo.substring(1);
    }

    private static boolean ehMarcadorErroPlanilha(String valor) {
        if (valor == null) return false;
        return MARCADORES_ERRO_PLANILHA.contains(valor.trim().toUpperCase(Locale.ROOT));
    }

    private static List<UnidadeOperacional> criarListaSemDuplicidade(
            List<UnidadeOperacional> unidades
    ) {
        Map<String, UnidadeOperacional> unicas = new LinkedHashMap<>();

        for (UnidadeOperacional unidade : unidades) {
            String chave = unidade.chaveUnica();
            UnidadeOperacional anterior = unicas.putIfAbsent(chave, unidade);

            if (anterior != null) {
                throw new IllegalStateException(
                        "Unidade operacional repetida na carga: " + unidade.nomeLotacao()
                                + " / " + unidade.uf()
                );
            }
        }

        return List.copyOf(unicas.values());
    }

    private record DefinicaoJornada(
            String descricao,
            String entrada1,
            String saida1,
            String entrada2,
            String saida2
    ) {
        private DefinicaoJornada {
            descricao = normalizarObrigatorio(descricao, "descrição da jornada");
            entrada1 = normalizarObrigatorio(entrada1, "entrada 1");
            saida1 = normalizarObrigatorio(saida1, "saída 1");

            if ((entrada2 == null) != (saida2 == null)) {
                throw new IllegalArgumentException(
                        "Entrada 2 e saída 2 devem ser informadas juntas para " + descricao
                );
            }
        }
    }

    private record UnidadeOperacional(
            String tipo,
            String nome,
            String uf,
            String municipio
    ) {
        private UnidadeOperacional {
            tipo = normalizarObrigatorio(tipo, "tipo").toUpperCase(Locale.ROOT);
            nome = normalizarObrigatorio(nome, "nome");
            uf = normalizarObrigatorio(uf, "UF").toUpperCase(Locale.ROOT);
            municipio = normalizarObrigatorio(municipio, "município");

            if (!Set.of("CTCE", "CTE", "TECA").contains(tipo)) {
                throw new IllegalArgumentException(
                        "Tipo de unidade operacional não suportado: " + tipo
                );
            }
            if (uf.length() != 2) {
                throw new IllegalArgumentException(
                        "UF inválida para a unidade " + nome + ": " + uf
                );
            }
        }

        String nomeLotacao() {
            return tipo + " " + nome;
        }

        String chaveUnica() {
            return normalizarChave(tipo) + "|"
                    + normalizarChave(uf) + "|"
                    + normalizarChave(nome);
        }
    }

    private static String normalizarObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "O campo " + campo + " é obrigatório na carga inicial."
            );
        }

        String normalizado = valor.trim().replaceAll("\\s+", " ");
        if (ehMarcadorErroPlanilha(normalizado)) {
            throw new IllegalArgumentException(
                    "O campo " + campo + " contém um erro de planilha inválido: " + normalizado
            );
        }
        return normalizado;
    }

    private static String normalizarChave(String valor) {
        String normalizado = Normalizer.normalize(
                normalizarObrigatorio(valor, "chave"),
                Normalizer.Form.NFD
        );

        return normalizado
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }
}
