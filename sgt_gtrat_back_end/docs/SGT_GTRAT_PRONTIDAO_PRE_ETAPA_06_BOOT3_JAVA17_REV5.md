# SGT/GTRAT — Revisão 5 de prontidão antes da Etapa 06

**Data:** 08/09/2026  
**Base:** Revisão 4 — Spring Boot 3.5.16 + Java 17  
**Objetivo:** utilizar o novo repositório Maven offline fornecido para fechar empacotamento, repetir a suíte de testes e verificar o perfil de integração MySQL até o limite da infraestrutura disponível.

## 1. Resultado executivo

O `repositoryNovo.zip` fornecido contém as dependências que faltavam no cache anterior. Com ele foi possível executar Maven 3.9.16 totalmente em modo offline e concluir o empacotamento Spring Boot.

### Resultado principal

- Spring Boot **3.5.16**;
- Maven **3.9.16**;
- linguagem/alvo do compilador: **Java 17 (`release 17`)**;
- **419** fontes Java de produção compilados;
- **101** fontes Java de teste compilados;
- **71** classes de teste executáveis cobertas;
- **292 testes** executados;
- **0 falhas**;
- **0 erros**;
- **0 ignorados**;
- `mvn clean package -DskipTests`: **BUILD SUCCESS**;
- JAR Spring Boot executável gerado com sucesso.

O bloqueio anterior do `maven-jar-plugin`/`spring-boot:repackage` foi eliminado pelo novo cache Maven.

## 2. Empacotamento Maven offline

Comando executado:

```bash
mvn -o -B -ntp -Dmaven.repo.local=<repositoryNovo> -DskipTests clean package
```

Resultado:

```text
Compiling 419 source files with javac [debug parameters release 17]
Compiling 101 source files with javac [debug parameters release 17]
Building jar: target/sig-gtrat-backend-0.0.1-SNAPSHOT.jar
spring-boot:3.5.16:repackage
BUILD SUCCESS
```

O artefato gerado possui aproximadamente **75 MB** e foi validado com `unzip -t`, sem erros de integridade.

Manifesto principal:

```text
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: com.example.siggtrat.AplicacaoBackendSigGtrat
Spring-Boot-Version: 3.5.16
Spring-Boot-Classes: BOOT-INF/classes/
Spring-Boot-Lib: BOOT-INF/lib/
```

Foram encontradas **91 bibliotecas** em `BOOT-INF/lib`, incluindo Spring Boot, Spring Security, Hibernate e MySQL Connector.

## 3. Compatibilidade Java 17

O ambiente disponível executa Maven sobre JDK 21.0.11, porém o projeto está configurado com `release 17`.

Foi verificado diretamente o bytecode produzido:

```text
AplicacaoBackendSigGtrat.class -> major version 61
ControllerLayerArchitectureTest.class -> major version 61
```

`major version 61` corresponde a **Java 17**.

O campo `Build-Jdk-Spec: 21` do manifesto indica apenas qual JDK executou o processo de build neste ambiente; ele não altera o bytecode compilado com `release 17`.

Ainda é recomendável executar o mesmo JAR em uma JVM 17 real no ambiente de desenvolvimento/homologação para fechar a comprovação de runtime exatamente na versão escolhida.

## 4. Suíte de testes Maven

A ferramenta de execução desta sessão encerra comandos longos antes de uma execução monolítica terminar. Por isso a suíte foi executada em lotes usando o mesmo Maven, o mesmo classpath e o mesmo provider Surefire/JUnit Platform.

Resultado consolidado dos relatórios Surefire:

```text
71 classes de teste
292 testes
Failures: 0
Errors: 0
Skipped: 0
```

O `FechamentoFrequenciaProcessadorTest` está fisicamente em uma pasta `frequencia`, mas declara `package com.example.siggtrat.servico`; ele foi chamado pelo FQCN real e executou **7/7** testes com sucesso, fechando o total de 292.

As regressões e verificações já aprovadas na Revisão 4 permanecem sem alteração de código de produção nesta Revisão 5.

## 5. Integração MySQL/Testcontainers

Existem sete classes `*IT` no perfil Maven `integration`:

- `DatabaseAvailabilityIT`;
- `DatabaseLoadIT`;
- `DatabaseSchemaContractIT`;
- `DirectDatabaseServiceSmokeIT`;
- `RepositoryQuerySmokeIT`;
- `TokenAtivoRepositorioIT`;
- `TransactionIntegrityIT`.

Foi iniciada explicitamente a integração `DatabaseAvailabilityIT` com Testcontainers 1.21.4.

O framework iniciou corretamente, mas abortou ao procurar Docker:

```text
Could not find a valid Docker environment
NoSuchFileException (/var/run/docker.sock)
```

O ambiente não possui executável Docker nem daemon/socket Docker. Portanto os erros observados no `*IT` são de **infraestrutura indisponível**, antes de MySQL, Hibernate ou repositories serem exercitados.

Não foi usado H2 como substituto, porque isso não comprovaria o comportamento específico exigido para MySQL.

No ambiente com Docker, o gate indicado continua sendo:

```bash
mvn -Pintegration verify
```

## 6. Situação dos bloqueadores anteriores

Continuam corrigidos e cobertos pelos testes/regressões das Revisões anteriores:

- R01 — cadeia de retificação/substituição;
- R02 — conflito Aditamento × Remanejamento;
- R03 — reaplicação de remanejamento após retroatividade;
- R04 — cancelamento de contrato futuro;
- R05 — composições futuras escondidas na publicação;
- R06 — reconciliação/migração automática de legado no startup;
- R07 — incompatibilidade arquitetural DTO/ArchUnit;
- R09 — vigência reconstruída para remanejamento;
- governança de fornecedor de contrato publicado;
- Cargo por Gestão Local e ativo/inativo;
- inativação de Cargo/Lotação/Turno em vez de exclusão física;
- Motivo de Desligamento como catálogo preparatório para a Etapa 06;
- Java 17 como versão definitiva;
- Spring Boot 3.5.16 como base atual.

## 7. Decisão de prontidão

### Build e testes de aplicação

**APROVADOS.**

O backend compila, os testes unitários/arquiteturais passam e o JAR Spring Boot é empacotado com sucesso usando somente o repositório Maven offline fornecido.

### Integração MySQL

**PENDENTE APENAS POR INFRAESTRUTURA.**

O Testcontainers foi efetivamente acionado e a única causa observada foi ausência de Docker no ambiente. Não há evidência nova de defeito de código nesse gate porque a inicialização do banco nem chegou a ocorrer.

### Liberação da Etapa 06

A base está **liberada para iniciar o desenvolvimento da Etapa 06 — Pessoa, Vínculo, Matrícula e Desligamento**.

Essa liberação significa que os bloqueadores conhecidos das Etapas 01–05 foram corrigidos, a suíte Maven está verde e o empacotamento está aprovado. A execução `-Pintegration verify` em Docker/MySQL continua obrigatória antes de considerar a validação de persistência/homologação encerrada, mas não é mais tratada como bloqueador para começar a implementação da Etapa 06.

Nenhum código funcional da Etapa 06 foi iniciado nesta Revisão 5.
