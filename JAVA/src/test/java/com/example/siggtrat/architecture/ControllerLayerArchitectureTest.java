package com.example.siggtrat.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Protege a fronteira HTTP da aplicação.
 * Controllers podem orquestrar DTOs e services, mas nunca acessar persistência
 * ou entidades JPA diretamente.
 */
@AnalyzeClasses(
        packages = "com.example.siggtrat",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ControllerLayerArchitectureTest {

    @ArchTest
    static final ArchRule controllers_nao_acessam_repositories = noClasses()
            .that().resideInAPackage("..controlador..")
            .should().dependOnClassesThat().resideInAPackage("..repositorio..")
            .because("o fluxo HTTP deve seguir Controller -> DTO -> Service -> Repository");

    @ArchTest
    static final ArchRule controllers_nao_expoem_entidades_jpa = noClasses()
            .that().resideInAPackage("..controlador..")
            .should().dependOnClassesThat().resideInAPackage("..entidade..")
            .because("controllers devem trocar DTOs com a camada de serviço, não entidades JPA");
    @ArchTest
    static final ArchRule dtos_nao_dependem_de_entidades_jpa = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .because("DTOs podem compartilhar enums de valor, mas nunca entidades JPA");

    @ArchTest
    static final ArchRule dtos_nao_dependem_de_repositories = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().resideInAPackage("..repositorio..")
            .because("DTOs devem ser contratos de dados puros; acesso a persistencia pertence ao servico");

    @ArchTest
    static final ArchRule controllers_nao_acessam_infraestrutura_de_dados = noClasses()
            .that().resideInAPackage("..controlador..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.data.redis..",
                    "org.springframework.data.repository..",
                    "org.springframework.jdbc..",
                    "org.springframework.cache.."
            )
            .because("acesso a cache, Redis e dados deve ficar na camada de serviço");

}
