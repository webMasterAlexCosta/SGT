package com.example.siggtrat.configuracao;

import com.example.siggtrat.utilitario.UtilitarioMatricula;
import com.example.siggtrat.utilitario.UtilitarioHorario;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    private static final String RECURSO_TERCEIRIZADOS = "db/seed/terceirizados.tsv";
    private static final String PREFIXO_MATRICULA_TERCEIRIZADO = "TERC26-";
    private static final int QUANTIDADE_ESPERADA_TERCEIRIZADOS = 13_405;
    private static final String VALOR_CARGA_CONCLUIDA = "CONCLUIDA";
    private static final String SEED_JORNADAS_V1 = "INTERNO_SEED_JORNADAS_OPERACIONAIS_V1";
    private static final String SEED_UNIDADES_V1 = "INTERNO_SEED_UNIDADES_OPERACIONAIS_V1";
    private static final String SEED_USUARIOS_GLOBAIS_V1 = "INTERNO_SEED_USUARIOS_GLOBAIS_V1";
    private static final String SEED_GESTORES_HIERARQUIA_V1 = "INTERNO_SEED_GESTORES_HIERARQUIA_V1";
    private static final String SEED_HOMOLOGACAO_V1 = "INTERNO_SEED_HOMOLOGACAO_GESTAO_LOCAL_V1";
    private static final String SEED_TERCEIRIZADOS_V1 = "INTERNO_SEED_TERCEIRIZADOS_V1";
    private static final String SEED_CONSOLIDACAO_RJ_V1 = "INTERNO_SEED_CONSOLIDACAO_RJ_V1";

    @Value("${app.seed.executar-apenas-uma-vez:true}")
    private boolean executarSeedsApenasUmaVez;
    private static final Set<String> MARCADORES_ERRO_PLANILHA = Set.of(
            "#VALOR!", "#N/A", "#REF!", "#DIV/0!", "#NOME?"
    );
    private static final String LOTACAO_CORRIGIDA_IPIAU = "AC IPIAU";
    private static final String MUNICIPIO_CORRIGIDO_IPIAU = "IPIAU";

    private static final String UF_RIO_DE_JANEIRO = "RJ";
    private static final String MUNICIPIO_RIO_DE_JANEIRO = "RIO DE JANEIRO";
    private static final String LOTACAO_RJ_CTE_BENFICA = "CTE BENFICA";
    private static final String LOTACAO_RJ_CTCE_CIDADE_NOVA = "CTCE CIDADE NOVA";
    private static final Set<String> LOTACOES_RJ_CARGA = Set.of(
            LOTACAO_RJ_CTE_BENFICA,
            LOTACAO_RJ_CTCE_CIDADE_NOVA
    );

    private static final String CARGO_RJ_SUPERVISOR_OPERACIONAL = "SUPERVISOR OPERACIONAL";
    private static final String CARGO_RJ_AUXILIAR_OPERACIONAL = "AUXILIAR OPERACIONAL";
    private static final Set<String> CARGOS_RJ_CARGA = Set.of(
            CARGO_RJ_SUPERVISOR_OPERACIONAL,
            CARGO_RJ_AUXILIAR_OPERACIONAL
    );

    private static final List<String> EMPRESAS_CONTRATOS_RJ = List.of(
            "GTSHIP OFFSHORE COMÉRCIO E SERVIÇOS LTDA",
            "CONQUISTA VIGILANCIA E SEGURANÇA PATRIMONIAL LTDA",
            "FXX SEGURANÇA E TRANSPORTE DE VALORES LTDA.",
            "SANT´COSTA LIMPEZA E TERCEIRIZAÇÃO LTDA"
    );
    private static final Set<String> CONTRATOS_RJ_CARGA = Set.copyOf(EMPRESAS_CONTRATOS_RJ);

    private static final List<String> NOMES_FICTICIOS = List.of(
            "ALEX", "AMANDA", "ANDRE", "BEATRIZ", "BRUNO", "CAMILA", "CAIO", "CAROLINA",
            "DANIEL", "DAVI", "EDUARDA", "ELISA", "FELIPE", "FERNANDA", "GABRIEL", "HELENA",
            "IGOR", "ISABELA", "JOAO", "JULIANA", "LEONARDO", "LIVIA", "LUCAS", "MARIANA",
            "MATEUS", "NATALIA", "PAULO", "RAFAELA", "RENATO", "SABRINA", "THIAGO", "VITORIA"
    );

    private static final List<String> SOBRENOMES_FICTICIOS = List.of(
            "ALMEIDA", "BARBOSA", "CARDOSO", "CARVALHO", "CASTRO", "CORREA", "COSTA", "DUARTE",
            "FARIAS", "FERREIRA", "FREITAS", "GOMES", "LIMA", "LOPES", "MACHADO", "MARTINS",
            "MELO", "MENDES", "MONTEIRO", "MORAES", "NOGUEIRA", "OLIVEIRA", "PEREIRA", "RAMOS",
            "RIBEIRO", "ROCHA", "SANTANA", "SANTOS", "SILVA", "SOARES", "TEIXEIRA", "VIEIRA"
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

                                garantirJornada(jornadaRepositorio, gestaoLocal, definicao);

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
                        List<Jornada> jornadas = carregarJornadasIniciais(jornadaRepositorio, gestaoLocal);
                        String nomeLotacao = unidade.nomeLotacao();

                        boolean existia = lotacaoRepositorio
                                .findByNomeIgnoreCaseAndGestaoLocalId(nomeLotacao, gestaoLocal.getId())
                                .isPresent();

                        garantirLotacao(
                                lotacaoRepositorio,
                                nomeLotacao,
                                unidade.municipio(),
                                gestaoLocal,
                                jornadas
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
            matchIfMissing = true
    )
    CommandLineRunner initUsuariosGlobais(
            UsuarioAplicacaoRepositorio userRepositorio,
            PasswordEncoder passwordEncoder,
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            PlatformTransactionManager transactionManager,
            @Value("${app.seed.usuarios-globais-initial-password:123456}") String senhaInicial
    ) {
        return args -> executarCargaTransacionalUmaVez(
                configuracaoRepositorio,
                transactionManager,
                SEED_USUARIOS_GLOBAIS_V1,
                "Usuários globais de desenvolvimento",
                executarSeedsApenasUmaVez,
                () -> usuariosGlobaisCompletos(userRepositorio),
                () -> {
                    if (senhaInicial == null || senhaInicial.length() < 6 || senhaInicial.length() > 72) {
                        throw new IllegalStateException(
                                "A senha inicial dos usuários globais deve possuir entre 6 e 72 caracteres."
                        );
                    }

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
            matchIfMissing = true
    )
    CommandLineRunner initUsuariosGestoresSeEGestaoLocal(
            UsuarioAplicacaoRepositorio userRepositorio,
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            PasswordEncoder passwordEncoder,
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            PlatformTransactionManager transactionManager,
            @Value("${app.seed.gestores-initial-password:Alterar@123}") String senhaInicial
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
                    if (senhaInicial == null || senhaInicial.length() < 6 || senhaInicial.length() > 72) {
                        throw new IllegalStateException(
                                "A senha inicial dos Gestores Estaduais/Locais deve possuir entre 6 e 72 caracteres."
                        );
                    }

                    String senhaCriptografada = passwordEncoder.encode(senhaInicial);
                    int criados = 0;
                    int existentes = 0;

                    for (int indice = 0; indice < ConstantesDominio.UFS_BRASIL.size(); indice++) {
                        String uf = ConstantesDominio.UFS_BRASIL.get(indice);
                        SuperintendenciaEstadual se = exigirSuperintendencia(superintendenciaRepositorio, uf);

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

                        List<GestaoLocal> gestoesLocais = listarGestoesLocais(gestaoLocalRepositorio, se);
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

    @Bean
    @Order(40)
    @ConditionalOnProperty(
            name = "app.seed.homologacao-enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    CommandLineRunner initEscoposHomologacao(
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            JornadaRepositorio jornadaRepositorio,
            LotacaoRepositorio lotacaoRepositorio,
            ContratoRepositorio contratoRepositorio,
            EmpregadoRepositorio empregadoRepositorio,
            StatusEmpregadoRepositorio statusRepositorio,
            CargoRepositorio cargoRepositorio,
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            PlatformTransactionManager transactionManager
    ) {
        return args -> executarCargaTransacionalUmaVez(
                configuracaoRepositorio,
                transactionManager,
                SEED_HOMOLOGACAO_V1,
                "Massa isolada de homologação GTRAT/GEDIS/GETRA do RJ",
                executarSeedsApenasUmaVez,
                () -> homologacaoGestaoLocalCompleta(empregadoRepositorio),
                () -> {
                    log.warn(
                            "Carga de homologação habilitada: somente contratos de exemplo ausentes serão criados."
                    );

                    SuperintendenciaEstadual seRj = exigirSuperintendencia(superintendenciaRepositorio, "RJ");
                    StatusEmpregado statusAtivo = statusRepositorio
                            .findByDescricaoIgnoreCase(ConstantesDominio.STATUS_ATIVO)
                            .orElseThrow(() -> new IllegalStateException("O status ATIVO não foi inicializado."));
                    List<GestaoLocal> gestoesLocaisRj = listarGestoesLocais(gestaoLocalRepositorio, seRj).stream()
                            .filter(item -> SIGLAS_GESTOES_LOCAIS_INICIAIS.contains(siglaGestaoLocal(item)))
                            .sorted(Comparator.comparingInt(item ->
                                    SIGLAS_GESTOES_LOCAIS_INICIAIS.indexOf(siglaGestaoLocal(item))))
                            .toList();
                    if (gestoesLocaisRj.size() != SIGLAS_GESTOES_LOCAIS_INICIAIS.size()) {
                        throw new IllegalStateException("O RJ precisa possuir GTRAT, GEDIS e GETRA antes da homologação.");
                    }

                    for (int indice = 0; indice < gestoesLocaisRj.size(); indice++) {
                        GestaoLocal gestaoLocal = gestoesLocaisRj.get(indice);
                        String sigla = siglaGestaoLocal(gestaoLocal);
                        Cargo cargo = garantirCargoHomologacao(cargoRepositorio, gestaoLocal);
                        List<Jornada> jornadas = carregarJornadasIniciais(jornadaRepositorio, gestaoLocal);
                        Lotacao lotacao = garantirLotacao(
                                lotacaoRepositorio,
                                "BASE " + sigla + " RJ",
                                MUNICIPIO_RIO_DE_JANEIRO,
                                gestaoLocal,
                                jornadas
                        );
                        Contrato contrato = garantirContrato(
                                contratoRepositorio,
                                "RJ-" + sigla + "-2026-001",
                                "Contrato de homologação da " + sigla + "/RJ",
                                gestaoLocal,
                                10 + indice * 5
                        );
                        garantirEmpregadoHomologacao(
                                empregadoRepositorio,
                                indice,
                                sigla,
                                gestaoLocal,
                                lotacao,
                                jornadas.get(0),
                                statusAtivo,
                                cargo,
                                contrato
                        );
                    }

                    log.info("Carga de homologação concluída: contratos ausentes verificados.");
                });
    }

    @Bean
    @Order(45)
    @ConditionalOnProperty(
            name = "app.seed.terceirizados-enabled",
            havingValue = "true"
    )
    CommandLineRunner initTerceirizados(
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            JornadaRepositorio jornadaRepositorio,
            StatusEmpregadoRepositorio statusRepositorio,
            CargoRepositorio cargoRepositorio,
            LotacaoRepositorio lotacaoRepositorio,
            ContratoRepositorio contratoRepositorio,
            EmpregadoRepositorio empregadoRepositorio,
            HistoricoVinculoContratoRepositorio historicoVinculoContratoRepositorio,
            EntityManager entityManager,
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            PlatformTransactionManager transactionManager,
            @Value("${app.seed.terceirizados.batch-size:1000}") int batchSize,
            @Value("${app.seed.terceirizados.multiplier:1}") int multiplicador,
            @Value("${app.seed.terceirizados.sp-percent:55}") int percentualSp,
            @Value("${app.seed.terceirizados.rj-percent:30}") int percentualRj
    ) {
        return args -> {
            if (deveIgnorarCargaConcluida(
                    configuracaoRepositorio, SEED_TERCEIRIZADOS_V1,
                    "Carga fictícia ampliada de terceirizados", executarSeedsApenasUmaVez)) {
                return;
            }
            List<RegistroTerceirizado> registrosBase = carregarRegistrosTerceirizados();
            int tamanhoLote = Math.max(100, Math.min(batchSize, 2_000));
            PlanoCargaTerceirizados plano = criarPlanoCargaTerceirizados(
                    registrosBase,
                    multiplicador,
                    percentualSp,
                    percentualRj
            );

            executarCargaTerceirizados(
                    plano,
                    tamanhoLote,
                    superintendenciaRepositorio,
                    gestaoLocalRepositorio,
                    jornadaRepositorio,
                    statusRepositorio,
                    cargoRepositorio,
                    lotacaoRepositorio,
                    contratoRepositorio,
                    empregadoRepositorio,
                    historicoVinculoContratoRepositorio,
                    entityManager,
                    transactionManager
            );
            marcarCargaConcluida(
                    configuracaoRepositorio,
                    transactionManager,
                    SEED_TERCEIRIZADOS_V1,
                    "Carga fictícia ampliada de terceirizados",
                    executarSeedsApenasUmaVez
            );
        };
    }

    @Bean
    @Order(46)
    @ConditionalOnProperty(
            name = "app.seed.terceirizados-enabled",
            havingValue = "true"
    )
    CommandLineRunner consolidarLotacoesRioDeJaneiro(
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            JornadaRepositorio jornadaRepositorio,
            LotacaoRepositorio lotacaoRepositorio,
            EmpregadoRepositorio empregadoRepositorio,
            HistoricoVinculoContratoRepositorio historicoVinculoContratoRepositorio,
            UsuarioAplicacaoRepositorio appUserRepositorio,
            CalendarioEventoRepositorio calendarioEventoRepositorio,
            EntityManager entityManager,
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            PlatformTransactionManager transactionManager
    ) {
        return args -> executarCargaTransacionalUmaVez(
                configuracaoRepositorio,
                transactionManager,
                SEED_CONSOLIDACAO_RJ_V1,
                "Consolidação das lotações operacionais do Rio de Janeiro",
                executarSeedsApenasUmaVez,
                () -> consolidacaoRioDeJaneiroCompleta(
                        superintendenciaRepositorio, gestaoLocalRepositorio,
                        lotacaoRepositorio, empregadoRepositorio, entityManager),
                () -> {
                    SuperintendenciaEstadual seRj = exigirSuperintendencia(
                            superintendenciaRepositorio,
                            UF_RIO_DE_JANEIRO
                    );
                    GestaoLocal gestaoLocalRj = exigirGestaoLocal(gestaoLocalRepositorio, seRj);
                    List<Jornada> jornadasRj = carregarJornadasIniciais(jornadaRepositorio, gestaoLocalRj);

                    Lotacao cteBenfica = garantirLotacao(
                            lotacaoRepositorio,
                            LOTACAO_RJ_CTE_BENFICA,
                            MUNICIPIO_RIO_DE_JANEIRO,
                            gestaoLocalRj,
                            jornadasRj
                    );
                    Lotacao ctceCidadeNova = garantirLotacao(
                            lotacaoRepositorio,
                            LOTACAO_RJ_CTCE_CIDADE_NOVA,
                            MUNICIPIO_RIO_DE_JANEIRO,
                            gestaoLocalRj,
                            jornadasRj
                    );

                    entityManager.flush();

                    int alocadosBenfica = entityManager.createQuery(
                                    "UPDATE Empregado e "
                                            + "SET e.lotacao = :lotacao, e.version = e.version + 1 "
                                            + "WHERE e.gestaoLocal.id = :gestaoLocalId "
                                            + "AND MOD(e.id, 2) = 0 "
                                            + "AND e.lotacao.id NOT IN :lotacaoIds"
                            )
                            .setParameter("lotacao", cteBenfica)
                            .setParameter("lotacaoIds", List.of(cteBenfica.getId(), ctceCidadeNova.getId()))
                            .setParameter("gestaoLocalId", gestaoLocalRj.getId())
                            .executeUpdate();

                    int alocadosCidadeNova = entityManager.createQuery(
                                    "UPDATE Empregado e "
                                            + "SET e.lotacao = :lotacao, e.version = e.version + 1 "
                                            + "WHERE e.gestaoLocal.id = :gestaoLocalId "
                                            + "AND MOD(e.id, 2) = 1 "
                                            + "AND e.lotacao.id NOT IN :lotacaoIds"
                            )
                            .setParameter("lotacao", ctceCidadeNova)
                            .setParameter("lotacaoIds", List.of(cteBenfica.getId(), ctceCidadeNova.getId()))
                            .setParameter("gestaoLocalId", gestaoLocalRj.getId())
                            .executeUpdate();

                    entityManager.clear();

                    long totalEmpregadosRj = empregadoRepositorio.countByGestaoLocalId(gestaoLocalRj.getId());
                    long totalNasDuasLotacoes = entityManager.createQuery(
                                    "SELECT COUNT(e) FROM Empregado e "
                                            + "WHERE e.gestaoLocal.id = :gestaoLocalId "
                                            + "AND e.lotacao.id IN :lotacaoIds",
                                    Long.class
                            )
                            .setParameter("gestaoLocalId", gestaoLocalRj.getId())
                            .setParameter("lotacaoIds", List.of(cteBenfica.getId(), ctceCidadeNova.getId()))
                            .getSingleResult();

                    if (totalEmpregadosRj != totalNasDuasLotacoes) {
                        throw new IllegalStateException(
                                "Nem todos os empregados do RJ foram consolidados nas duas lotações. "
                                        + "Total RJ=" + totalEmpregadosRj
                                        + ", total consolidado=" + totalNasDuasLotacoes
                        );
                    }

                    int lotacoesRemovidas = 0;
                    int lotacoesPreservadasPorReferencia = 0;
                    Set<Long> lotacoesPermitidas = Set.of(cteBenfica.getId(), ctceCidadeNova.getId());

                    for (Lotacao lotacao : lotacaoRepositorio.findByGestaoLocalIdOrderByNomeAsc(gestaoLocalRj.getId())) {
                        if (lotacao.getId() == null || lotacoesPermitidas.contains(lotacao.getId())) {
                            continue;
                        }

                        boolean possuiReferencia = empregadoRepositorio.existsByLotacao_Id(lotacao.getId())
                                || historicoVinculoContratoRepositorio.existsByLotacao_Id(lotacao.getId())
                                || appUserRepositorio.existsByLotacaoId(lotacao.getId())
                                || appUserRepositorio.existsByVinculosGestao_Lotacao_Id(lotacao.getId())
                                || calendarioEventoRepositorio.existsByLotacaoId(lotacao.getId());

                        if (possuiReferencia) {
                            lotacoesPreservadasPorReferencia++;
                            log.warn(
                                    "A lotação antiga do RJ '{}' (id={}) não foi removida porque ainda possui "
                                            + "referências de histórico, usuário, escopo ou calendário.",
                                    lotacao.getNome(),
                                    lotacao.getId()
                            );
                            continue;
                        }

                        if (lotacao.getTurnos() != null) {
                            lotacao.getTurnos().clear();
                        }
                        lotacaoRepositorio.delete(lotacao);
                        lotacoesRemovidas++;
                    }
                    lotacaoRepositorio.flush();

                    log.info(
                            "Consolidação da SE/RJ concluída: {} empregado(s) direcionado(s) para {}, "
                                    + "{} para {}; total nas duas lotações={}; lotações antigas removidas={}, "
                                    + "preservadas por referência={}.",
                            alocadosBenfica,
                            LOTACAO_RJ_CTE_BENFICA,
                            alocadosCidadeNova,
                            LOTACAO_RJ_CTCE_CIDADE_NOVA,
                            totalNasDuasLotacoes,
                            lotacoesRemovidas,
                            lotacoesPreservadasPorReferencia
                    );
                });
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
     * Executa uma etapa de desenvolvimento/homologação no máximo uma vez por
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

    private static void marcarCargaConcluida(
            ConfiguracaoSistemaRepositorio configuracaoRepositorio,
            PlatformTransactionManager transactionManager,
            String chave,
            String descricao,
            boolean executarApenasUmaVez
    ) {
        if (!executarApenasUmaVez) return;
        executarEmTransacao(transactionManager,
                () -> registrarMarcadorCarga(configuracaoRepositorio, chave, descricao));
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
            List<GestaoLocal> gestoes = gestaoLocalRepositorio
                    .findBySuperintendenciaIdOrderByNomeAsc(se.get().getId());
            if (gestoes.isEmpty()) return false;
            for (GestaoLocal gestaoLocal : gestoes) {
                if (usuarioRepositorio.findByUsername(
                        nomeUsuarioGestaoLocal(siglaGestaoLocal(gestaoLocal), uf)).isEmpty()) return false;
            }
        }
        return true;
    }

    private static boolean homologacaoGestaoLocalCompleta(EmpregadoRepositorio repositorio) {
        return SIGLAS_GESTOES_LOCAIS_INICIAIS.stream()
                .allMatch(sigla -> repositorio.existsByMatricula("HOM-RJ-" + sigla));
    }

    private static boolean consolidacaoRioDeJaneiroCompleta(
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            LotacaoRepositorio lotacaoRepositorio,
            EmpregadoRepositorio empregadoRepositorio,
            EntityManager entityManager
    ) {
        Optional<SuperintendenciaEstadual> se = superintendenciaRepositorio
                .findByUfIgnoreCase(UF_RIO_DE_JANEIRO);
        if (se.isEmpty()) return false;
        Optional<GestaoLocal> gestaoLocal = gestaoLocalRepositorio
                .findBySuperintendenciaIdAndCodigoIgnoreCase(se.get().getId(), "GTRAT-RJ");
        if (gestaoLocal.isEmpty()) return false;
        Optional<Lotacao> benfica = lotacaoRepositorio.findByNomeIgnoreCaseAndGestaoLocalId(
                LOTACAO_RJ_CTE_BENFICA, gestaoLocal.get().getId());
        Optional<Lotacao> cidadeNova = lotacaoRepositorio.findByNomeIgnoreCaseAndGestaoLocalId(
                LOTACAO_RJ_CTCE_CIDADE_NOVA, gestaoLocal.get().getId());
        if (benfica.isEmpty() || cidadeNova.isEmpty()) return false;

        long total = empregadoRepositorio.countByGestaoLocalId(gestaoLocal.get().getId());
        long consolidados = entityManager.createQuery(
                        "SELECT COUNT(e) FROM Empregado e "
                                + "WHERE e.gestaoLocal.id = :gestaoLocalId "
                                + "AND e.lotacao.id IN :lotacaoIds",
                        Long.class
                )
                .setParameter("gestaoLocalId", gestaoLocal.get().getId())
                .setParameter("lotacaoIds", List.of(benfica.get().getId(), cidadeNova.get().getId()))
                .getSingleResult();
        return total == consolidados;
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
            DefinicaoJornada definicao
    ) {
        Jornada jornada = repositorio
                .findByDescricaoIgnoreCaseAndGestaoLocalId(definicao.descricao(), gestaoLocal.getId())
                .orElseGet(Jornada::new);

        jornada.setDescricao(definicao.descricao());
        jornada.setGestaoLocal(gestaoLocal);

        sincronizarDiasDaJornada(jornada, definicao);

        return repositorio.save(jornada);
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
            GestaoLocal gestaoLocal
    ) {
        List<Jornada> jornadas = new ArrayList<>();
        for (DefinicaoJornada definicao : DEFINICOES_JORNADAS) {
            jornadas.add(garantirJornada(repositorio, gestaoLocal, definicao));
        }
        return jornadas;
    }

    private static Lotacao garantirLotacao(
            LotacaoRepositorio repositorio,
            String nome,
            String municipio,
            GestaoLocal gestaoLocal,
            Collection<Jornada> jornadas
    ) {
        Lotacao lotacao = repositorio
                .findByNomeIgnoreCaseAndGestaoLocalId(nome, gestaoLocal.getId())
                .orElseGet(() -> new Lotacao(nome));

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

        return repositorio.save(lotacao);
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
            user.setForceChangePassword(false);
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
            user.setForceChangePassword(false);
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
            default -> 95;
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

    private static Contrato garantirContrato(
            ContratoRepositorio repositorio,
            String numero,
            String descricao,
            GestaoLocal gestaoLocal,
            int efetivoPrevisto
    ) {
        final int anoContrato = 2026;
        Contrato contrato = repositorio
                .findByNumeroContratoIgnoreCaseAndAnoContrato(numero, anoContrato)
                .orElseGet(() -> repositorio.findByNumeroContratoIgnoreCase(numero).orElseGet(Contrato::new));

        boolean novo = contrato.getId() == null;

        if (novo) {
            contrato.setNumeroContrato(numero);
            contrato.setAnoContrato(anoContrato);
            contrato.setDescricao(descricao);
            contrato.setTipo("HOMOLOGACAO");
            contrato.setDataInicio(LocalDate.of(2026, 1, 1));
            contrato.setDataFim(LocalDate.of(2026, 12, 31));
            contrato.setAtivo(true);
            contrato.setGestaoLocal(gestaoLocal);
            contrato.setEfetivoPrevisto(efetivoPrevisto);
        } else {
            if (contrato.getAnoContrato() == null) {
                contrato.setAnoContrato(anoContrato);
            }
            if (contrato.getDescricao() == null || contrato.getDescricao().isBlank()) {
                contrato.setDescricao(descricao);
            }
            if (contrato.getTipo() == null || contrato.getTipo().isBlank()) {
                contrato.setTipo("HOMOLOGACAO");
            }
            if (contrato.getGestaoLocal() == null) {
                contrato.setGestaoLocal(gestaoLocal);
            }
            if (contrato.getEfetivoPrevisto() == null) {
                contrato.setEfetivoPrevisto(efetivoPrevisto);
            }
        }

        return repositorio.save(contrato);
    }

    private static Cargo garantirCargoHomologacao(
            CargoRepositorio repositorio,
            GestaoLocal gestaoLocal
    ) {
        if (gestaoLocal == null || gestaoLocal.getId() == null) {
            throw new IllegalStateException("A Gestão Local deve existir antes da criação do cargo de homologação.");
        }

        Cargo cargo = repositorio
                .findByGestaoLocalIdAndNomeIgnoreCase(
                        gestaoLocal.getId(),
                        "ANALISTA OPERACIONAL HOMOLOGAÇÃO"
                )
                .orElseGet(Cargo::new);
        if (cargo.getId() == null) {
            cargo.setNome("ANALISTA OPERACIONAL HOMOLOGAÇÃO");
            cargo.setCbo("HOM-RJ-001");
            cargo.setDescricao("Cargo fictício para validar o isolamento entre Gestões Locais do RJ.");
            cargo.setGestaoLocal(gestaoLocal);
        }
        return repositorio.save(cargo);
    }

    private static void garantirEmpregadoHomologacao(
            EmpregadoRepositorio repositorio,
            int indice,
            String sigla,
            GestaoLocal gestaoLocal,
            Lotacao lotacao,
            Jornada jornada,
            StatusEmpregado status,
            Cargo cargo,
            Contrato contrato
    ) {
        String matricula = "HOM-RJ-" + sigla;
        if (repositorio.existsByMatricula(matricula)) return;

        Empregado empregado = new Empregado();
        empregado.setNome("COLABORADOR HOMOLOGAÇÃO " + sigla + " RJ");
        empregado.setMatricula(matricula);
        empregado.setCpf(gerarCpfFicticio(900_001 + indice));
        empregado.setEmail("homologacao." + sigla.toLowerCase(Locale.ROOT) + ".rj@example.invalid");
        empregado.setDataAdmissao(LocalDate.of(2026, 1, 1).plusDays(indice));
        empregado.setObservacao("Massa fictícia isolada da Gestão Local " + sigla + "/RJ.");
        empregado.setGestaoLocal(gestaoLocal);
        empregado.setLotacao(lotacao);
        empregado.setJornada(jornada);
        empregado.setStatus(status);
        empregado.setCargo(cargo);
        empregado.setContratos(new ArrayList<>(List.of(contrato)));
        repositorio.save(empregado);
    }



    private static boolean ehMarcadorErroPlanilha(String valor) {
        if (valor == null) return false;
        return MARCADORES_ERRO_PLANILHA.contains(valor.trim().toUpperCase(Locale.ROOT));
    }

    private static List<RegistroTerceirizado> carregarRegistrosTerceirizados() {
        ClassPathResource recurso = new ClassPathResource(RECURSO_TERCEIRIZADOS);
        List<RegistroTerceirizado> registros = new ArrayList<>(QUANTIDADE_ESPERADA_TERCEIRIZADOS);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                recurso.getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            String cabecalho = reader.readLine();
            if (cabecalho == null) {
                throw new IllegalStateException("O arquivo de terceirizados está vazio.");
            }

            cabecalho = cabecalho.replace("\uFEFF", "");
            if (!"uf\tcargo\tlotacao\tmunicipio\tempresa".equals(cabecalho)) {
                throw new IllegalStateException(
                        "Cabeçalho inválido em " + RECURSO_TERCEIRIZADOS + ": " + cabecalho
                );
            }

            String linha;
            int numeroLinha = 1;
            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                if (linha.isBlank()) continue;

                String[] colunas = linha.split("\\t", -1);
                if (colunas.length != 5) {
                    throw new IllegalStateException(
                            "Linha " + numeroLinha + " inválida em " + RECURSO_TERCEIRIZADOS
                                    + ": eram esperadas 5 colunas e foram encontradas " + colunas.length
                    );
                }

                RegistroTerceirizado registro = new RegistroTerceirizado(
                        colunas[0],
                        colunas[1],
                        colunas[2],
                        colunas[3],
                        colunas[4]
                );
                registros.add(normalizarLotacaoRioDeJaneiro(registro, registros.size() + 1));
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Não foi possível ler a carga de terceirizados em " + RECURSO_TERCEIRIZADOS,
                    ex
            );
        }

        if (registros.size() != QUANTIDADE_ESPERADA_TERCEIRIZADOS) {
            throw new IllegalStateException(
                    "Quantidade inesperada de terceirizados na carga: esperado "
                            + QUANTIDADE_ESPERADA_TERCEIRIZADOS + ", encontrado " + registros.size()
            );
        }

        Set<String> lotacoesRjEncontradas = registros.stream()
                .filter(registro -> UF_RIO_DE_JANEIRO.equals(registro.uf()))
                .map(RegistroTerceirizado::lotacao)
                .collect(java.util.stream.Collectors.toSet());
        if (!LOTACOES_RJ_CARGA.equals(lotacoesRjEncontradas)) {
            throw new IllegalStateException(
                    "A carga do RJ deve utilizar exclusivamente as lotações "
                            + LOTACOES_RJ_CARGA + ", mas encontrou " + lotacoesRjEncontradas
            );
        }

        Set<String> cargosRjEncontrados = registros.stream()
                .filter(registro -> UF_RIO_DE_JANEIRO.equals(registro.uf()))
                .map(RegistroTerceirizado::cargo)
                .collect(java.util.stream.Collectors.toSet());
        if (!CARGOS_RJ_CARGA.equals(cargosRjEncontrados)) {
            throw new IllegalStateException(
                    "A carga do RJ deve utilizar exclusivamente os cargos "
                            + CARGOS_RJ_CARGA + ", mas encontrou " + cargosRjEncontrados
            );
        }

        Set<String> contratosRjEncontrados = registros.stream()
                .filter(registro -> UF_RIO_DE_JANEIRO.equals(registro.uf()))
                .map(RegistroTerceirizado::empresa)
                .collect(java.util.stream.Collectors.toSet());
        if (!CONTRATOS_RJ_CARGA.equals(contratosRjEncontrados)) {
            throw new IllegalStateException(
                    "A carga do RJ deve gerar exatamente os quatro contratos "
                            + CONTRATOS_RJ_CARGA + ", mas encontrou " + contratosRjEncontrados
            );
        }

        return List.copyOf(registros);
    }

    /**
     * A carga do RJ utiliza somente duas lotações. Registros que já mencionam
     * Cidade Nova permanecem no CTCE CIDADE NOVA; os que mencionam Benfica
     * permanecem no CTE BENFICA. As demais unidades da planilha são
     * distribuídas deterministicamente entre as duas lotações para que toda
     * reinicialização produza o mesmo resultado.
     */
    private static RegistroTerceirizado normalizarLotacaoRioDeJaneiro(
            RegistroTerceirizado registro,
            int indiceCarga
    ) {
        if (!UF_RIO_DE_JANEIRO.equals(registro.uf())) {
            return registro;
        }

        String lotacaoOriginal = normalizarChave(registro.lotacao());
        String lotacaoDestino;

        if (lotacaoOriginal.contains("CIDADE NOVA")) {
            lotacaoDestino = LOTACAO_RJ_CTCE_CIDADE_NOVA;
        } else if (lotacaoOriginal.contains("BENFICA")) {
            lotacaoDestino = LOTACAO_RJ_CTE_BENFICA;
        } else {
            lotacaoDestino = Math.floorMod(indiceCarga, 2) == 0
                    ? LOTACAO_RJ_CTE_BENFICA
                    : LOTACAO_RJ_CTCE_CIDADE_NOVA;
        }

        return new RegistroTerceirizado(
                UF_RIO_DE_JANEIRO,
                normalizarCargoRioDeJaneiro(registro.cargo()),
                lotacaoDestino,
                MUNICIPIO_RIO_DE_JANEIRO,
                normalizarEmpresaContratoRioDeJaneiro(registro.empresa())
        );
    }

    private static String normalizarCargoRioDeJaneiro(String cargoOriginal) {
        String chaveCargo = normalizarChave(cargoOriginal);
        return chaveCargo.contains("SUPERV")
                ? CARGO_RJ_SUPERVISOR_OPERACIONAL
                : CARGO_RJ_AUXILIAR_OPERACIONAL;
    }

    private static String normalizarEmpresaContratoRioDeJaneiro(String empresaOriginal) {
        String chaveEmpresa = normalizarChave(empresaOriginal);

        for (String empresaContrato : EMPRESAS_CONTRATOS_RJ) {
            if (normalizarChave(empresaContrato).equals(chaveEmpresa)) {
                return empresaContrato;
            }
        }

        int indiceContrato = Math.floorMod(
                chaveEmpresa.hashCode(),
                EMPRESAS_CONTRATOS_RJ.size()
        );
        return EMPRESAS_CONTRATOS_RJ.get(indiceContrato);
    }

    private static PlanoCargaTerceirizados criarPlanoCargaTerceirizados(
            List<RegistroTerceirizado> registrosBase,
            int multiplicador,
            int percentualSp,
            int percentualRj
    ) {
        if (multiplicador < 1) {
            throw new IllegalArgumentException(
                    "O multiplicador da carga de terceirizados deve ser maior ou igual a 1."
            );
        }
        if (percentualSp < 0 || percentualSp > 100
                || percentualRj < 0 || percentualRj > 100
                || percentualSp + percentualRj > 100) {
            throw new IllegalArgumentException(
                    "Os percentuais de SP e RJ devem estar entre 0 e 100 e a soma não pode ultrapassar 100."
            );
        }

        int totalRegistros;
        try {
            totalRegistros = Math.multiplyExact(registrosBase.size(), multiplicador);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("A quantidade solicitada para a carga é muito grande.", ex);
        }

        if (totalRegistros > 999_999) {
            throw new IllegalArgumentException(
                    "A carga suporta no máximo 999.999 colaboradores para manter a numeração "
                            + "padronizada das matrículas. Total solicitado: " + totalRegistros
            );
        }

        List<RegistroTerceirizado> registrosSp = registrosBase.stream()
                .filter(registro -> "SP".equals(registro.uf()))
                .toList();
        List<RegistroTerceirizado> registrosRj = registrosBase.stream()
                .filter(registro -> "RJ".equals(registro.uf()))
                .toList();
        List<RegistroTerceirizado> registrosOutrasUfs = registrosBase.stream()
                .filter(registro -> !"SP".equals(registro.uf()) && !"RJ".equals(registro.uf()))
                .toList();

        if (percentualSp > 0 && registrosSp.isEmpty()) {
            throw new IllegalStateException("Não existem modelos de colaboradores de SP na carga base.");
        }
        if (percentualRj > 0 && registrosRj.isEmpty()) {
            throw new IllegalStateException("Não existem modelos de colaboradores do RJ na carga base.");
        }
        if (percentualSp + percentualRj < 100 && registrosOutrasUfs.isEmpty()) {
            throw new IllegalStateException("Não existem modelos de colaboradores das demais UFs na carga base.");
        }

        return new PlanoCargaTerceirizados(
                List.copyOf(registrosBase),
                List.copyOf(registrosSp),
                List.copyOf(registrosRj),
                List.copyOf(registrosOutrasUfs),
                multiplicador,
                percentualSp,
                percentualRj,
                totalRegistros
        );
    }

    private static void executarCargaTerceirizados(
            PlanoCargaTerceirizados plano,
            int batchSize,
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            JornadaRepositorio jornadaRepositorio,
            StatusEmpregadoRepositorio statusRepositorio,
            CargoRepositorio cargoRepositorio,
            LotacaoRepositorio lotacaoRepositorio,
            ContratoRepositorio contratoRepositorio,
            EmpregadoRepositorio empregadoRepositorio,
            HistoricoVinculoContratoRepositorio historicoVinculoContratoRepositorio,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        long quantidadeExistente = empregadoRepositorio
                .countByMatriculaStartingWith(PREFIXO_MATRICULA_TERCEIRIZADO);
        long menorIndice = empregadoRepositorio
                .findMenorIndiceMatriculaDaCarga(PREFIXO_MATRICULA_TERCEIRIZADO);
        long maiorIndice = empregadoRepositorio
                .findMaiorIndiceMatriculaDaCarga(PREFIXO_MATRICULA_TERCEIRIZADO);

        if (quantidadeExistente == plano.totalRegistros()
                && menorIndice == 1L
                && maiorIndice == plano.totalRegistros()) {
            log.info(
                    "Carga ampliada de terceirizados já aplicada: {} colaborador(es), multiplicador {}x.",
                    plano.totalRegistros(),
                    plano.multiplicador()
            );
            return;
        }

        if (quantidadeExistente > plano.totalRegistros() || maiorIndice > plano.totalRegistros()) {
            log.warn(
                    "Existem {} matrícula(s) da carga, com maior índice {}, mas o plano atual possui {}. "
                            + "Nenhum registro excedente será removido automaticamente.",
                    quantidadeExistente,
                    maiorIndice,
                    plano.totalRegistros()
            );
        }

        Map<String, Integer> distribuicao = calcularDistribuicaoPorUf(plano);
        log.info(
                "Iniciando carga ampliada: {} colaborador(es) no total ({}x a base), "
                        + "SP={}, RJ={}, demais UFs={}, lote transacional={}. "
                        + "Os contratos existentes serão reutilizados.",
                plano.totalRegistros(),
                plano.multiplicador(),
                distribuicao.getOrDefault("SP", 0),
                distribuicao.getOrDefault("RJ", 0),
                plano.totalRegistros()
                        - distribuicao.getOrDefault("SP", 0)
                        - distribuicao.getOrDefault("RJ", 0),
                batchSize
        );

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ContextoCargaTerceirizados contexto = Objects.requireNonNull(
                transactionTemplate.execute(status -> prepararEstruturaTerceirizados(
                        plano,
                        superintendenciaRepositorio,
                        gestaoLocalRepositorio,
                        jornadaRepositorio,
                        statusRepositorio,
                        cargoRepositorio,
                        lotacaoRepositorio,
                        contratoRepositorio
                )),
                "Não foi possível preparar a estrutura da carga de terceirizados."
        );

        long inicioCarga = System.nanoTime();
        int empregadosCriados = 0;
        int empregadosExistentes = 0;
        int historicosCriados = 0;
        int totalLotes = (int) Math.ceil(plano.totalRegistros() / (double) batchSize);
        int intervaloLog = Math.max(1, (int) Math.ceil(10_000D / batchSize));

        for (int numeroLote = 1, inicioIndice = 1;
             inicioIndice <= plano.totalRegistros();
             numeroLote++, inicioIndice += batchSize) {

            int fimIndice = Math.min(plano.totalRegistros(), inicioIndice + batchSize - 1);
            int inicioDoLote = inicioIndice;
            int fimDoLote = fimIndice;

            ResultadoLoteCarga resultado = Objects.requireNonNull(
                    transactionTemplate.execute(status -> persistirLoteCargaTerceirizados(
                            plano,
                            contexto,
                            inicioDoLote,
                            fimDoLote,
                            empregadoRepositorio,
                            historicoVinculoContratoRepositorio,
                            entityManager
                    )),
                    "A transação do lote de terceirizados não retornou resultado."
            );

            empregadosCriados += resultado.empregadosCriados();
            empregadosExistentes += resultado.empregadosExistentes();
            historicosCriados += resultado.historicosCriados();

            if (numeroLote == 1 || numeroLote == totalLotes || numeroLote % intervaloLog == 0) {
                registrarProgressoCarga(
                        fimIndice,
                        plano.totalRegistros(),
                        empregadosCriados,
                        empregadosExistentes,
                        numeroLote,
                        totalLotes,
                        inicioCarga
                );
            }
        }

        long segundos = Math.max(1L, (System.nanoTime() - inicioCarga) / 1_000_000_000L);
        log.info(
                "Carga ampliada concluída: {} colaborador(es) previsto(s), {} criado(s), "
                        + "{} já existente(s), {} histórico(s) criado(s), {} cargo(s) novo(s), "
                        + "{} lotação(ões) nova(s), {} contrato(s) novo(s), duração aproximada={}s.",
                plano.totalRegistros(),
                empregadosCriados,
                empregadosExistentes,
                historicosCriados,
                contexto.cargosCriados(),
                contexto.lotacoesCriadas(),
                contexto.contratosCriados(),
                segundos
        );
    }

    private static ContextoCargaTerceirizados prepararEstruturaTerceirizados(
            PlanoCargaTerceirizados plano,
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            JornadaRepositorio jornadaRepositorio,
            StatusEmpregadoRepositorio statusRepositorio,
            CargoRepositorio cargoRepositorio,
            LotacaoRepositorio lotacaoRepositorio,
            ContratoRepositorio contratoRepositorio
    ) {
        List<RegistroTerceirizado> registrosBase = plano.registrosBase();

        StatusEmpregado statusAtivo = statusRepositorio
                .findByDescricaoIgnoreCase(ConstantesDominio.STATUS_ATIVO)
                .orElseThrow(() -> new IllegalStateException("O status ATIVO não foi inicializado."));

        Map<String, GestaoLocal> gestoesLocaisPorUf = new HashMap<>();
        Map<Long, List<Jornada>> jornadasPorGestaoLocal = new HashMap<>();
        Set<String> ufsDaCarga = new TreeSet<>();
        for (RegistroTerceirizado registro : registrosBase) {
            ufsDaCarga.add(registro.uf());
        }

        for (String uf : ufsDaCarga) {
            SuperintendenciaEstadual se = exigirSuperintendencia(superintendenciaRepositorio, uf);
            GestaoLocal gestaoLocal = exigirGestaoLocal(gestaoLocalRepositorio, se);
            gestoesLocaisPorUf.put(uf, gestaoLocal);

            List<Jornada> jornadas = jornadaRepositorio.findByGestaoLocalIdOrderByDescricaoAsc(gestaoLocal.getId());
            if (jornadas.isEmpty()) {
                jornadas = carregarJornadasIniciais(jornadaRepositorio, gestaoLocal);
                jornadas.sort(Comparator.comparing(Jornada::getDescricao, String.CASE_INSENSITIVE_ORDER));
            }
            // O contexto é reutilizado por lotes em transações diferentes. Inicializa
            // os dias ainda nesta transação para que os snapshots da jornada não
            // dependam de uma coleção LAZY já destacada do EntityManager.
            jornadas.forEach(jornada -> {
                if (jornada.getDias() != null) jornada.getDias().size();
            });
            jornadasPorGestaoLocal.put(gestaoLocal.getId(), List.copyOf(jornadas));
        }

        Map<ChaveCargo, Cargo> cargosPorChave = new HashMap<>();
        for (Cargo cargo : cargoRepositorio.findAll()) {
            if (cargo.getGestaoLocal() != null
                    && cargo.getGestaoLocal().getId() != null
                    && cargo.getNome() != null
                    && !cargo.getNome().isBlank()) {
                cargosPorChave.putIfAbsent(
                        new ChaveCargo(
                                cargo.getGestaoLocal().getId(),
                                normalizarChave(cargo.getNome())
                        ),
                        cargo
                );
            }
        }

        Map<ChaveCargo, RegistroTerceirizado> definicoesCargo = new LinkedHashMap<>();
        for (RegistroTerceirizado registro : registrosBase) {
            GestaoLocal gestaoLocal = gestoesLocaisPorUf.get(registro.uf());
            ChaveCargo chave = new ChaveCargo(
                    gestaoLocal.getId(),
                    normalizarChave(registro.cargo())
            );
            definicoesCargo.putIfAbsent(chave, registro);
        }

        List<Cargo> novosCargos = new ArrayList<>();
        for (Map.Entry<ChaveCargo, RegistroTerceirizado> entry : definicoesCargo.entrySet()) {
            ChaveCargo chave = entry.getKey();
            RegistroTerceirizado registro = entry.getValue();
            if (cargosPorChave.containsKey(chave)) continue;

            GestaoLocal gestaoLocal = gestoesLocaisPorUf.get(registro.uf());
            Cargo cargo = new Cargo();
            cargo.setNome(registro.cargo());
            cargo.setDescricao("Cargo criado pela carga fictícia de colaboradores terceirizados.");
            cargo.setGestaoLocal(gestaoLocal);
            novosCargos.add(cargo);
            cargosPorChave.put(chave, cargo);
        }
        if (!novosCargos.isEmpty()) {
            cargoRepositorio.saveAll(novosCargos);
            cargoRepositorio.flush();
        }

        Map<ChaveLotacao, Lotacao> lotacoesPorChave = new HashMap<>();
        for (Lotacao lotacao : lotacaoRepositorio.findAll()) {
            if (lotacao.getGestaoLocal() == null || lotacao.getNome() == null || lotacao.getNome().isBlank()) {
                continue;
            }
            lotacoesPorChave.putIfAbsent(
                    new ChaveLotacao(lotacao.getGestaoLocal().getId(), normalizarChave(lotacao.getNome())),
                    lotacao
            );
        }

        Map<ChaveLotacao, RegistroTerceirizado> definicoesLotacao = new LinkedHashMap<>();
        for (RegistroTerceirizado registro : registrosBase) {
            GestaoLocal gestaoLocal = gestoesLocaisPorUf.get(registro.uf());
            ChaveLotacao chave = new ChaveLotacao(gestaoLocal.getId(), normalizarChave(registro.lotacao()));
            definicoesLotacao.putIfAbsent(chave, registro);
        }

        int lotacoesCriadas = 0;
        List<Lotacao> lotacoesParaSalvar = new ArrayList<>(definicoesLotacao.size());
        for (Map.Entry<ChaveLotacao, RegistroTerceirizado> entry : definicoesLotacao.entrySet()) {
            ChaveLotacao chave = entry.getKey();
            RegistroTerceirizado registro = entry.getValue();
            GestaoLocal gestaoLocal = gestoesLocaisPorUf.get(registro.uf());

            Lotacao lotacao = lotacoesPorChave.get(chave);
            if (lotacao == null) {
                lotacao = new Lotacao();
                lotacoesCriadas++;
            }

            lotacao.setNome(registro.lotacao());
            lotacao.setMunicipio(registro.municipio());
            lotacao.setGestaoLocal(gestaoLocal);
            associarJornadas(lotacao, jornadasPorGestaoLocal.get(gestaoLocal.getId()));

            lotacoesParaSalvar.add(lotacao);
            lotacoesPorChave.put(chave, lotacao);
        }
        if (!lotacoesParaSalvar.isEmpty()) {
            lotacaoRepositorio.saveAll(lotacoesParaSalvar);
            lotacaoRepositorio.flush();
        }

        Map<ChaveContrato, Integer> efetivoPorContrato = new HashMap<>();
        for (int indice = 1; indice <= plano.totalRegistros(); indice++) {
            RegistroTerceirizado registro = plano.registroParaIndice(indice);
            ChaveContrato chave = new ChaveContrato(registro.uf(), registro.empresa());
            efetivoPorContrato.merge(chave, 1, Integer::sum);
        }

        Map<String, Contrato> contratosPorNumero = new HashMap<>();
        for (Contrato contrato : contratoRepositorio.findAll()) {
            if (contrato.getNumeroContrato() != null && !contrato.getNumeroContrato().isBlank()) {
                contratosPorNumero.putIfAbsent(normalizarChave(contrato.getNumeroContrato()), contrato);
            }
        }

        List<ChaveContrato> chavesContratoOrdenadas = new ArrayList<>(efetivoPorContrato.keySet());
        chavesContratoOrdenadas.sort(
                Comparator.comparing(ChaveContrato::uf)
                        .thenComparing(ChaveContrato::empresa, String.CASE_INSENSITIVE_ORDER)
        );

        int contratosCriados = 0;
        Set<String> numerosGerados = new HashSet<>();
        Map<ChaveContrato, Contrato> contratosPorChave = new HashMap<>();
        List<Contrato> contratosParaSalvar = new ArrayList<>(chavesContratoOrdenadas.size());

        for (ChaveContrato chave : chavesContratoOrdenadas) {
            String numero = gerarNumeroContratoTerceirizado(chave);
            if (!numerosGerados.add(numero)) {
                throw new IllegalStateException("Colisão na geração do contrato " + numero);
            }

            Contrato contrato = contratosPorNumero.get(normalizarChave(numero));
            if (contrato == null) {
                contrato = new Contrato();
                contratosCriados++;
            }

            contrato.setNumeroContrato(numero);
            contrato.setDescricao(
                    chave.empresa() + " - SE/" + chave.uf()
            );
            contrato.setTipo("TERCEIRIZACAO");
            contrato.setDataInicio(LocalDate.of(2026, 1, 1));
            contrato.setDataFim(LocalDate.of(2026, 12, 31));
            contrato.setAtivo(true);
            contrato.setGestaoLocal(gestoesLocaisPorUf.get(chave.uf()));
            contrato.setEfetivoPrevisto(efetivoPorContrato.get(chave));

            contratosParaSalvar.add(contrato);
            contratosPorChave.put(chave, contrato);
            contratosPorNumero.put(normalizarChave(numero), contrato);
        }
        if (!contratosParaSalvar.isEmpty()) {
            contratoRepositorio.saveAll(contratosParaSalvar);
            contratoRepositorio.flush();
        }

        return new ContextoCargaTerceirizados(
                statusAtivo,
                Map.copyOf(gestoesLocaisPorUf),
                copiarMapaListas(jornadasPorGestaoLocal),
                Map.copyOf(cargosPorChave),
                Map.copyOf(lotacoesPorChave),
                Map.copyOf(contratosPorChave),
                novosCargos.size(),
                lotacoesCriadas,
                contratosCriados
        );
    }

    private static ResultadoLoteCarga persistirLoteCargaTerceirizados(
            PlanoCargaTerceirizados plano,
            ContextoCargaTerceirizados contexto,
            int inicioIndice,
            int fimIndice,
            EmpregadoRepositorio empregadoRepositorio,
            HistoricoVinculoContratoRepositorio historicoRepositorio,
            EntityManager entityManager
    ) {
        List<String> matriculasDoLote = new ArrayList<>(fimIndice - inicioIndice + 1);
        for (int indice = inicioIndice; indice <= fimIndice; indice++) {
            matriculasDoLote.add(gerarMatriculaTerceirizado(indice));
        }

        List<Empregado> empregadosExistentes = empregadoRepositorio
                .buscarPorMatriculasComContratos(matriculasDoLote);
        Map<String, Empregado> existentesPorMatricula = new HashMap<>();
        for (Empregado existente : empregadosExistentes) {
            if (existente.getMatricula() != null) {
                existentesPorMatricula.putIfAbsent(existente.getMatricula(), existente);
            }
        }

        List<Empregado> empregadosNovos = new ArrayList<>(matriculasDoLote.size());
        List<String> cpfsNovos = new ArrayList<>(matriculasDoLote.size());

        for (int indice = inicioIndice; indice <= fimIndice; indice++) {
            String matricula = gerarMatriculaTerceirizado(indice);
            if (existentesPorMatricula.containsKey(matricula)) {
                continue;
            }

            RegistroTerceirizado registro = plano.registroParaIndice(indice);
            GestaoLocal gestaoLocal = contexto.gestoesLocaisPorUf().get(registro.uf());
            List<Jornada> jornadas = contexto.jornadasPorGestaoLocal().get(gestaoLocal.getId());
            ChaveLotacao chaveLotacao = new ChaveLotacao(
                    gestaoLocal.getId(),
                    normalizarChave(registro.lotacao())
            );
            ChaveCargo chaveCargo = new ChaveCargo(
                    gestaoLocal.getId(),
                    normalizarChave(registro.cargo())
            );
            ChaveContrato chaveContrato = new ChaveContrato(registro.uf(), registro.empresa());
            Contrato contrato = contexto.contratosPorChave().get(chaveContrato);
            String cpf = gerarCpfFicticio(indice);

            Empregado empregado = new Empregado();
            empregado.setNome(gerarNomeFicticio(indice));
            empregado.setMatricula(matricula);
            empregado.setCpf(cpf);
            empregado.setEmail(
                    "colaborador.ficticio."
                            + String.format(Locale.ROOT, "%06d", indice)
                            + "@example.invalid"
            );
            empregado.setDataAdmissao(
                    LocalDate.of(2026, 1, 1).plusDays(Math.floorMod(indice * 17L, 150L))
            );
            empregado.setObservacao(
                    "Registro fictício gerado pela carga inicial ampliada. "
                            + "Nenhum nome ou CPF da planilha foi utilizado."
            );
            empregado.setJornada(jornadas.get(Math.floorMod(indice - 1, jornadas.size())));
            empregado.setStatus(contexto.statusAtivo());
            empregado.setLotacao(contexto.lotacoesPorChave().get(chaveLotacao));
            empregado.setGestaoLocal(gestaoLocal);
            empregado.setCargo(contexto.cargosPorChave().get(chaveCargo));
            empregado.setContratos(new ArrayList<>(List.of(contrato)));

            empregadosNovos.add(empregado);
            cpfsNovos.add(cpf);
        }

        if (!cpfsNovos.isEmpty()) {
            List<String> cpfsEmConflito = empregadoRepositorio.buscarCpfsExistentes(cpfsNovos);
            if (!cpfsEmConflito.isEmpty()) {
                throw new IllegalStateException(
                        "A carga encontrou CPF(s) já utilizado(s) por outro registro: "
                                + cpfsEmConflito.stream().limit(10).toList()
                                + ". A carga foi interrompida antes de persistir o lote."
                );
            }

            empregadoRepositorio.saveAll(empregadosNovos);
            empregadoRepositorio.flush();
        }

        List<Empregado> empregadosDoLote = new ArrayList<>(
                empregadosExistentes.size() + empregadosNovos.size()
        );
        empregadosDoLote.addAll(empregadosExistentes);
        empregadosDoLote.addAll(empregadosNovos);

        List<Long> empregadoIds = empregadosDoLote.stream()
                .map(Empregado::getId)
                .filter(Objects::nonNull)
                .toList();

        Set<ChaveVinculoContrato> vinculosAtivos = new HashSet<>();
        if (!empregadoIds.isEmpty()) {
            for (HistoricoVinculoContrato historico :
                    historicoRepositorio.buscarAtivosPorIdsEmpregados(empregadoIds)) {
                if (historico.getEmpregado() == null
                        || historico.getEmpregado().getId() == null
                        || historico.getContrato() == null
                        || historico.getContrato().getId() == null) {
                    continue;
                }
                vinculosAtivos.add(new ChaveVinculoContrato(
                        historico.getEmpregado().getId(),
                        historico.getContrato().getId()
                ));
            }
        }

        ZoneId zoneBR = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);
        List<HistoricoVinculoContrato> historicosNovos = new ArrayList<>(empregadosDoLote.size());
        for (Empregado empregado : empregadosDoLote) {
            if (empregado.getId() == null || empregado.getContratos() == null) {
                continue;
            }
            for (Contrato contrato : empregado.getContratos()) {
                if (contrato == null || contrato.getId() == null) {
                    continue;
                }
                ChaveVinculoContrato chave = new ChaveVinculoContrato(
                        empregado.getId(),
                        contrato.getId()
                );
                if (!vinculosAtivos.add(chave)) {
                    continue;
                }

                HistoricoVinculoContrato historico = new HistoricoVinculoContrato();
                historico.setEmpregado(empregado);
                historico.setContrato(contrato);
                historico.setLotacao(empregado.getLotacao());
                historico.setJornada(empregado.getJornada());
                historico.setCargo(empregado.getCargo());
                historico.setEmpregadoSnapshotNome(empregado.getNome());
                historico.setContratoSnapshotNumero(contrato.getNumeroContrato());
                historico.setLotacaoSnapshotNome(
                        empregado.getLotacao() != null ? empregado.getLotacao().getNome() : null
                );
                historico.setLotacaoSnapshotMunicipio(
                        empregado.getLotacao() != null ? empregado.getLotacao().getMunicipio() : null
                );
                historico.setJornadaSnapshotDescricao(
                        empregado.getJornada() != null ? empregado.getJornada().getDescricao() : null
                );
                preencherSnapshotJornada(historico, empregado.getJornada());
                historico.setCargoSnapshotNome(
                        empregado.getCargo() != null ? empregado.getCargo().getNome() : null
                );
                historico.setDataInicio(calcularInicioVinculo(empregado, contrato, zoneBR));
                historico.setObservacao(
                        "Vínculo inicial criado automaticamente pela carga fictícia ampliada."
                );
                historicosNovos.add(historico);
            }
        }

        if (!historicosNovos.isEmpty()) {
            historicoRepositorio.saveAll(historicosNovos);
            historicoRepositorio.flush();
        }

        int quantidadeEmpregados = empregadosNovos.size();
        int quantidadeHistoricos = historicosNovos.size();
        entityManager.clear();

        return new ResultadoLoteCarga(
                quantidadeEmpregados,
                empregadosExistentes.size(),
                quantidadeHistoricos
        );
    }

    private static Map<Long, List<Jornada>> copiarMapaListas(Map<Long, List<Jornada>> origem) {
        Map<Long, List<Jornada>> copia = new HashMap<>();
        origem.forEach((chave, valor) -> copia.put(chave, List.copyOf(valor)));
        return Map.copyOf(copia);
    }

    private static Map<String, Integer> calcularDistribuicaoPorUf(PlanoCargaTerceirizados plano) {
        Map<String, Integer> distribuicao = new TreeMap<>();
        for (int indice = 1; indice <= plano.totalRegistros(); indice++) {
            distribuicao.merge(plano.registroParaIndice(indice).uf(), 1, Integer::sum);
        }
        return Map.copyOf(distribuicao);
    }

    private static void registrarProgressoCarga(
            int processados,
            int total,
            int criados,
            int existentes,
            int loteAtual,
            int totalLotes,
            long inicioCarga
    ) {
        long nanosDecorridos = Math.max(1L, System.nanoTime() - inicioCarga);
        double segundosDecorridos = nanosDecorridos / 1_000_000_000D;
        double porSegundo = processados / segundosDecorridos;
        long restantes = Math.max(0L, total - processados);
        long segundosRestantes = porSegundo > 0D
                ? Math.round(restantes / porSegundo)
                : 0L;
        double percentual = total == 0 ? 100D : processados * 100D / total;

        log.info(
                "Carga de terceirizados: {}/{} ({}) — criados={}, existentes={}, lote={}/{}, "
                        + "velocidade aproximada={} registro(s)/s, previsão restante={}s.",
                processados,
                total,
                String.format(Locale.ROOT, "%.2f%%", percentual),
                criados,
                existentes,
                loteAtual,
                totalLotes,
                Math.round(porSegundo),
                segundosRestantes
        );
    }

    private static void preencherSnapshotJornada(HistoricoVinculoContrato historico, Jornada jornada) {
        if (historico == null || jornada == null || jornada.getDias() == null) {
            return;
        }

        historico.setJornadaSnapshotDiasTrabalho(jornada.getDias().stream()
                .filter(dia -> Boolean.TRUE.equals(dia.getDiaTrabalho()))
                .map(dia -> dia.getDiaSemana().name())
                .sorted()
                .collect(java.util.stream.Collectors.joining(",")));

        historico.setJornadaSnapshotMinutosDia(jornada.getDias().stream()
                .filter(dia -> Boolean.TRUE.equals(dia.getDiaTrabalho()))
                .sorted(Comparator.comparing(JornadaDiaSemana::getDiaSemana))
                .map(dia -> dia.getDiaSemana().name() + ":" + (
                        UtilitarioHorario.calcularIntervaloMinutos(
                                dia.getEntrada1(), dia.getSaida1(), "entrada 1", "saída 1"
                        )
                                + UtilitarioHorario.calcularIntervaloMinutos(
                                dia.getEntrada2(), dia.getSaida2(), "entrada 2", "saída 2"
                        )))
                .collect(java.util.stream.Collectors.joining(";")));
    }

    private static ZonedDateTime calcularInicioVinculo(
            Empregado empregado,
            Contrato contrato,
            ZoneId zoneId
    ) {
        LocalDate dataInicio = empregado.getDataAdmissao();
        if (dataInicio == null
                || (contrato.getDataInicio() != null && contrato.getDataInicio().isAfter(dataInicio))) {
            dataInicio = contrato.getDataInicio();
        }
        if (dataInicio == null) {
            dataInicio = LocalDate.now(zoneId);
        }
        return dataInicio.atStartOfDay(zoneId);
    }

    private static String gerarMatriculaTerceirizado(int indice) {
        return PREFIXO_MATRICULA_TERCEIRIZADO
                + String.format(Locale.ROOT, "%06d", indice);
    }

    private static void associarJornadas(Lotacao lotacao, Collection<Jornada> jornadas) {
        if (lotacao.getTurnos() == null) {
            lotacao.setTurnos(new ArrayList<>());
        }

        Set<Long> idsAssociados = new HashSet<>();
        for (Jornada jornada : lotacao.getTurnos()) {
            if (jornada.getId() != null) idsAssociados.add(jornada.getId());
        }

        for (Jornada jornada : jornadas) {
            if (jornada.getId() == null || idsAssociados.add(jornada.getId())) {
                lotacao.getTurnos().add(jornada);
            }
        }
    }

    private static String gerarNumeroContratoTerceirizado(ChaveContrato chave) {
        String hash = UUID.nameUUIDFromBytes(
                        (chave.uf() + "|" + chave.empresa()).getBytes(StandardCharsets.UTF_8)
                )
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);

        return "TERC-2026-" + chave.uf() + "-" + hash;
    }

    private static String gerarNomeFicticio(int indice) {
        SplittableRandom random = new SplittableRandom(20_260_805L + indice * 104_729L);
        String primeiroNome = NOMES_FICTICIOS.get(random.nextInt(NOMES_FICTICIOS.size()));
        String sobrenome1 = SOBRENOMES_FICTICIOS.get(random.nextInt(SOBRENOMES_FICTICIOS.size()));
        String sobrenome2 = SOBRENOMES_FICTICIOS.get(random.nextInt(SOBRENOMES_FICTICIOS.size()));

        return primeiroNome + " " + sobrenome1 + " " + sobrenome2
                + " FICTICIO " + String.format(Locale.ROOT, "%05d", indice);
    }

    private static String gerarCpfFicticio(int indice) {
        for (int tentativa = 0; tentativa < 100_000; tentativa++) {
            long semente = indice + tentativa * 1_000_000L;
            long baseNumerica = Math.floorMod(
                    semente * 104_729L + 314_159_265L,
                    1_000_000_000L
            );
            String base = String.format(Locale.ROOT, "%09d", baseNumerica);
            if (todosDigitosIguais(base)) continue;

            int primeiroDigito = calcularDigitoCpf(base, 10);
            String parcial = base + primeiroDigito;
            int segundoDigito = calcularDigitoCpf(parcial, 11);
            return parcial + segundoDigito;
        }

        throw new IllegalStateException("Não foi possível gerar CPF fictício para o índice " + indice);
    }

    private static int calcularDigitoCpf(String digitos, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int i = 0; i < digitos.length(); i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * peso--;
        }
        int modulo = (soma * 10) % 11;
        return modulo == 10 ? 0 : modulo;
    }

    private static boolean todosDigitosIguais(String valor) {
        for (int i = 1; i < valor.length(); i++) {
            if (valor.charAt(i) != valor.charAt(0)) return false;
        }
        return true;
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

    private record PlanoCargaTerceirizados(
            List<RegistroTerceirizado> registrosBase,
            List<RegistroTerceirizado> registrosSp,
            List<RegistroTerceirizado> registrosRj,
            List<RegistroTerceirizado> registrosOutrasUfs,
            int multiplicador,
            int percentualSp,
            int percentualRj,
            int totalRegistros
    ) {
        RegistroTerceirizado registroParaIndice(int indice) {
            if (indice < 1 || indice > totalRegistros) {
                throw new IllegalArgumentException(
                        "Índice fora do plano da carga de terceirizados: " + indice
                );
            }

            if (indice <= registrosBase.size()) {
                return registrosBase.get(indice - 1);
            }

            long indiceExtra = indice - (long) registrosBase.size() - 1L;
            int faixa = (int) Math.floorMod(misturarIndice(indiceExtra), 100L);
            List<RegistroTerceirizado> origem;

            if (faixa < percentualSp) {
                origem = registrosSp;
            } else if (faixa < percentualSp + percentualRj) {
                origem = registrosRj;
            } else {
                origem = registrosOutrasUfs;
            }

            long valorMisturado = misturarIndice(
                    indiceExtra * 104_729L + faixa * 31_337L + 20_260_805L
            );
            int posicao = (int) Math.floorMod(valorMisturado, origem.size());
            return origem.get(posicao);
        }
    }

    private record ContextoCargaTerceirizados(
            StatusEmpregado statusAtivo,
            Map<String, GestaoLocal> gestoesLocaisPorUf,
            Map<Long, List<Jornada>> jornadasPorGestaoLocal,
            Map<ChaveCargo, Cargo> cargosPorChave,
            Map<ChaveLotacao, Lotacao> lotacoesPorChave,
            Map<ChaveContrato, Contrato> contratosPorChave,
            int cargosCriados,
            int lotacoesCriadas,
            int contratosCriados
    ) {
    }

    private record ResultadoLoteCarga(
            int empregadosCriados,
            int empregadosExistentes,
            int historicosCriados
    ) {
    }

    private static long misturarIndice(long valor) {
        long resultado = valor + 0x9E3779B97F4A7C15L;
        resultado = (resultado ^ (resultado >>> 30)) * 0xBF58476D1CE4E5B9L;
        resultado = (resultado ^ (resultado >>> 27)) * 0x94D049BB133111EBL;
        return resultado ^ (resultado >>> 31);
    }

    private record RegistroTerceirizado(
            String uf,
            String cargo,
            String lotacao,
            String municipio,
            String empresa
    ) {
        private RegistroTerceirizado {
            uf = normalizarObrigatorio(uf, "UF do terceirizado").toUpperCase(Locale.ROOT);
            cargo = normalizarObrigatorio(cargo, "cargo do terceirizado").toUpperCase(Locale.ROOT);
            lotacao = normalizarObrigatorio(lotacao, "lotação do terceirizado").toUpperCase(Locale.ROOT);
            municipio = normalizarObrigatorio(municipio, "município do terceirizado").toUpperCase(Locale.ROOT);
            empresa = normalizarObrigatorio(empresa, "empresa contratada").toUpperCase(Locale.ROOT);

            if (!ConstantesDominio.UFS_BRASIL.contains(uf)) {
                throw new IllegalArgumentException("UF inválida na carga de terceirizados: " + uf);
            }
            if (cargo.length() > 100) {
                throw new IllegalArgumentException("Cargo excede 100 caracteres: " + cargo);
            }
            if (lotacao.length() > 100) {
                throw new IllegalArgumentException("Lotação excede 100 caracteres: " + lotacao);
            }
            if (municipio.length() > 100) {
                throw new IllegalArgumentException("Município excede 100 caracteres: " + municipio);
            }
        }
    }

    private record ChaveLotacao(Long gestaoLocalId, String nome) {
    }

    private record ChaveCargo(Long gestaoLocalId, String nome) {
    }

    private record ChaveVinculoContrato(Long empregadoId, Long contratoId) {
    }

    private record ChaveContrato(String uf, String empresa) {
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
