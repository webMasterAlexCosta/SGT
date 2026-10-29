# SGT/GTRAT — Correção pré-Etapa 06 — Spring Boot 3 / Java 17 — Revisão 3

**Data:** 08/09/2026  
**Base:** `SGT_GTRAT_BACKEND_JAVA_ETAPA_05_CORRIGIDA_JAVA17_REV2.zip`  
**Objetivo:** adequar permanentemente o backend para Spring Boot 3 + Java 17, executar o máximo possível de validação antes da Etapa 06 e manter as correções acumuladas das Etapas 01–05.

## 1. Decisões técnicas consolidadas

- Java definitivo do backend: **Java 17**.
- Spring Boot definitivo desta linha: **Spring Boot 3.5.16**.
- Build: **Maven**.
- Maven usado na validação: **Apache Maven 3.9.16**.
- Banco de desenvolvimento é descartável entre versões; não são mantidas rotinas automáticas de migração/reconciliação de banco antigo.
- O histórico negocial criado dentro da versão atual continua obrigatório conforme o levantamento V8.

## 2. Alterações para Spring Boot 3.5.16

### `pom.xml`

- parent alterado de Spring Boot `4.1.0` para `3.5.16`;
- `spring-boot-starter-aspectj` substituído por `spring-boot-starter-aop`;
- Spring Retry passou a usar a versão gerenciada pelo Spring Boot 3.5;
- Testcontainers alterado para os módulos gerenciados pela linha 1.x do Spring Boot 3.5:
  - `org.testcontainers:mysql`;
  - `org.testcontainers:junit-jupiter`;
- Java permanece em `<java.version>17</java.version>`;
- Maven Compiler permanece com `release=17`.

### Testes de integração

- `EntityScan` alterado de pacote específico do Boot 4 para:
  - `org.springframework.boot.autoconfigure.domain.EntityScan`;
- `MySQLContainer` alterado para o pacote compatível com Testcontainers 1.21.x:
  - `org.testcontainers.containers.MySQLContainer`;
- tipo atualizado para `MySQLContainer<?>`.

## 3. Compatibilidade oficial utilizada

Spring Boot 3.5.16 exige Java 17 ou superior e oferece suporte explícito a Maven 3.6.3 ou superior. A linha 3.5.16 gerencia Testcontainers 1.21.4, incluindo os módulos `mysql` e `junit-jupiter`.

Referências oficiais consultadas:

- Spring Boot 3.5.16 System Requirements: `https://docs.spring.io/spring-boot/3.5/system-requirements.html`
- Spring Boot 3.5.16 Managed Dependency Coordinates: `https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html`
- Spring Boot 3.5 EntityScan API: `https://docs.spring.io/spring-boot/3.5/api/java/org/springframework/boot/autoconfigure/domain/EntityScan.html`

## 4. Validações executadas com sucesso

### Checkpoint estático Boot 3 / Java 17

**42/42 verificações aprovadas.**

Incluem:

- Spring Boot 3.5.16 configurado;
- starter AOP compatível com Boot 3;
- Testcontainers 1.x configurado;
- imports Boot 3 corrigidos;
- Java 17 no backend e módulo de performance;
- ausência das rotinas automáticas de legado removidas;
- correções R01–R09 preservadas;
- proteção de fornecedor publicado;
- Cargo/Lotação/Turno com inativação;
- Cargo vinculado à Gestão Local;
- massas negociais antigas desativadas por padrão;
- ausência de `List.getFirst()/getLast()` incompatível com Java 17.

### Regressões executáveis

Os harnesses são compilados explicitamente com `javac --release 17`.

**28/28 cenários aprovados:**

- T01–T16: motor temporal, retificações, conflitos, retroatividade, redução e cancelamento;
- T17–T21: vigência reconstruída e publicação integral de composições;
- T22–T24: Motivo de Desligamento;
- T25–T28: Cargo/Lotação/Gestão Local.

### Validação estrutural

- 419 arquivos Java analisados;
- 49 entidades com tabela explícita;
- nomenclatura aprovada;
- `pom.xml` válido como XML;
- `performance-tests/pom.xml` válido como XML.

## 5. Maven — tentativa real

Foi usado o Maven binário fornecido:

```text
Apache Maven 3.9.16
```

Foram executados:

```bash
mvn -B -ntp clean test
mvn -B -ntp -DskipTests package
```

Ambos param **antes da compilação do projeto**, ao tentar resolver:

```text
org.springframework.boot:spring-boot-starter-parent:pom:3.5.16
```

Motivo registrado pelo Maven:

```text
Unknown host repo.maven.apache.org
Temporary failure in name resolution
```

Também foi testada conexão direta aos endereços atuais do Maven Central e a saída HTTPS do ambiente permanece bloqueada. Portanto a falha não é de código, Spring Boot 3 ou Maven: o ambiente não consegue baixar dependências externas e não possui esse parent no cache local.

Os logs estão em:

- `docs/evidencias_pre_etapa06_rev3/maven_clean_test_boot3.log`;
- `docs/evidencias_pre_etapa06_rev3/maven_package_boot3.log`.

## 6. Limitação de JVM do ambiente

O projeto está configurado para Java 17 e os harnesses são compilados com `--release 17`.

O runtime disponível neste ambiente, porém, é JDK 21.0.11. Não existe uma instalação JDK 17 local para executar a JVM dos testes Maven. Quando as dependências estiverem disponíveis, a homologação final ideal deve executar a suíte com JDK 17 real.

## 7. Situação dos bloqueios pré-Etapa 06

As correções funcionais previamente identificadas permanecem implementadas e suas regressões direcionadas estão aprovadas:

- R01 — cadeia de retificação/substituição;
- R02 — conflito Aditamento × Remanejamento;
- R03 — remanejamento após retroatividade;
- R04 — contrato futuro cancelado;
- R05 — composição futura escondida na publicação;
- R06 — inicialização/reconciliação automática de legado;
- R07 — regra arquitetural DTO/ArchUnit;
- R09 — vigência reconstruída do remanejamento;
- proteção de fornecedor publicado;
- cadastros Cargo/Lotação/Turno;
- Motivo de Desligamento;
- Java 17;
- Spring Boot 3.5.16.

## 8. Decisão de prontidão

### Funcionalidade direcionada das Etapas 01–05

**APROVADA nas verificações disponíveis neste ambiente.**

### Build Spring completo

**AINDA NÃO CERTIFICADO**, exclusivamente porque o Maven Central está inacessível e o cache local não contém as dependências Spring Boot 3.5.16.

Não há, nesta revisão, novo bloqueador funcional conhecido no núcleo Contrato → Composição → Motor Temporal → Aditamento/Remanejamento.

### Gate restante antes da Etapa 06

Executar em ambiente com JDK 17 e dependências acessíveis:

```bash
mvn -B -ntp clean test
mvn -B -ntp -DskipTests package
```

Se ambos concluírem sem falhas, a base estará tecnicamente liberada para iniciar a Etapa 06 — Pessoa, Vínculo, Matrícula e Desligamento.

## 9. Arquivos de evidência

A revisão inclui em `docs/evidencias_pre_etapa06_rev3/`:

- validações finais Boot 3 / Java 17;
- checkpoint estático;
- logs da tentativa Maven `clean test`;
- logs da tentativa Maven `package`;
- códigos de saída das tentativas Maven.

**Esta Revisão 3 substitui as Revisões 1 e 2 como checkpoint técnico atual anterior à Etapa 06.**
