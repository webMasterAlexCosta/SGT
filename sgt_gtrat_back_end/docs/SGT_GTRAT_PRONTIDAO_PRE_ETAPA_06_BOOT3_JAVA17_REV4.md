# SGT/GTRAT — Revisão 4 de prontidão antes da Etapa 06

**Data:** 08/09/2026  
**Base:** Revisão 3 — Spring Boot 3.5.16 + Java 17  
**Objetivo:** fechar a validação Maven possível antes de iniciar Pessoa/Vínculo/Matrícula/Desligamento.

## 1. Resultado executivo

O `repository.zip` fornecido foi utilizado como repositório Maven local em modo totalmente offline. Com ele foi possível compilar o backend real e executar a suíte Maven completa.

### Resultado principal

- Spring Boot **3.5.16**;
- Maven **3.9.16**;
- compilação de produção com `release 17`;
- **419 fontes Java de produção compilados**;
- **101 fontes Java de teste compilados**;
- suíte Maven completa: **292 testes**;
- **0 falhas**;
- **0 erros**;
- **0 ignorados**;
- resultado Maven: **BUILD SUCCESS**.

Com isso, a incompatibilidade arquitetural e as regressões que bloqueavam a Etapa 06 deixaram de ser apenas provas isoladas: a suíte real do projeto também está verde.

## 2. Falhas encontradas pela suíte real e corrigidas

A primeira execução completa revelou dois testes quebrados por fragilidade temporal/textual. O código de produção correspondente estava correto.

### 2.1 `FechamentoFrequenciaServicoTest`

O helper do teste utilizava a data absoluta `06/08/2026`. Com a passagem do tempo, essa data ultrapassou a janela configurada e o teste passou a receber `400 BAD_REQUEST` por prazo expirado.

Correção:

- o teste agora calcula uma data válida relativa a `America/Sao_Paulo`;
- utiliza o dia anterior ao dia corrente;
- a regra funcional de frequência **não foi antecipada** nesta revisão.

A especificação V8 prevê a janela definitiva do Operacional em 7 dias; essa adequação funcional continua planejada para a Etapa 09, onde toda a frequência será revalidada contra o novo Vínculo/Alocação temporal.

### 2.2 `RelatorioMensalAbsenteismoArchitectureTest`

O teste extraía o bloco `CALENDÁRIO DE EVENTOS` da configuração de segurança usando uma sequência exata de espaços e quebras de linha. A configuração continuava contendo os perfis esperados, mas a alteração de formatação fazia `indexOf()` retornar `-1` e provocava `StringIndexOutOfBoundsException`.

Correção:

- a busca passou a usar os marcadores semânticos dos blocos de configuração;
- são validados explicitamente início e fim antes do `substring`;
- continuam sendo exigidos no bloco de leitura de calendário: `ROLE_ADMIN`, `ROLE_GESTOR_LOCAL`, `ROLE_USER` e `ROLE_SUPERINTENDENCIA`.

Após as correções, os dois testes passaram: **17 testes executados, 0 falhas, 0 erros** no bloco de confirmação.

## 3. Suíte Maven completa

Comando executado em modo offline:

```bash
mvn -o -B -ntp -Dmaven.repo.local=<repository-fornecido> test
```

Resultado final:

```text
Tests run: 292, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Tempo da execução consolidada registrada: aproximadamente 31 segundos, sem contar preparação inicial do cache.

Também foi feita uma tentativa de `clean test`. Ela recompilou os fontes e gerou relatórios Surefire para os **292 testes, todos aprovados**, porém o processo da ferramenta foi encerrado pelo limite externo de execução antes de imprimir o rodapé final do Maven. Por isso a evidência formal de `BUILD SUCCESS` usada nesta revisão é a execução completa de `mvn test`, enquanto a execução `clean` confirma novamente a recompilação e os relatórios dos testes.

## 4. Regressões direcionadas e checkpoint

Continuam aprovados:

- **42/42** verificações do checkpoint Spring Boot 3/Java 17;
- **T01–T16**: motor temporal, retificações, precedência, remanejamento, redução, hash e cancelamento futuro;
- **T17–T21**: vigência reconstruída e consistência integral de publicação/composições;
- **T22–T24**: Motivo de Desligamento;
- **T25–T28**: Cargo, Gestão Local e Lotação;
- nomenclatura: **419 arquivos Java**, **49 entidades**, aprovado.

Nenhuma correção da Revisão 4 alterou o código de produção da Revisão 3; as alterações adicionais foram exclusivamente nos dois testes frágeis identificados pela execução Maven real.

## 5. Java 17

O projeto permanece configurado permanentemente para Java 17.

O Maven compilou produção e testes com:

```text
release 17
```

O ambiente de execução disponível possui JVM 21.0.11, portanto os testes Surefire rodaram nessa JVM apesar de o bytecode/API estar limitado a Java 17 pelo compilador. Foi tentada obtenção de uma JVM 17 local, mas o ambiente não permitiu o download binário externo.

Assim, a evidência atual comprova compatibilidade de compilação Java 17 e execução da suíte em JVM 21. A execução da mesma suíte especificamente numa JVM 17 continua recomendada no ambiente de desenvolvimento/homologação.

## 6. `clean package`

O `repository.zip` trouxe todas as dependências necessárias para compilar e testar, mas não contém todas as dependências transitivas dos plugins de empacotamento.

`mvn -DskipTests clean package` chegou até:

- compilação dos 419 fontes de produção com release 17;
- compilação dos 101 fontes de teste com release 17;
- início do `maven-jar-plugin`.

O empacotamento parou porque o cache offline não possui:

- `org.apache.maven.shared:file-management:3.1.0`;
- `org.apache.maven:maven-archiver:3.6.2`;
- `org.codehaus.plexus:plexus-archiver:4.9.2`.

A tentativa direta do `spring-boot:repackage` também mostrou que ainda faltam no cache:

- `org.springframework.boot:spring-boot-buildpack-platform:3.5.16`;
- `org.springframework.boot:spring-boot-loader-tools:3.5.16`;
- `org.apache.maven.plugins:maven-shade-plugin:3.6.0`.

Isso é uma limitação do conteúdo do repositório offline enviado, e não um erro de compilação do backend.

Para completar esse cache em uma máquina com internet, o comando recomendado é:

```powershell
mvn -U -B -ntp -Dmaven.repo.local=C:\temp\sgt-maven-offline -DskipTests clean package
```

Depois basta compactar novamente `C:\temp\sgt-maven-offline`.

## 7. Integração MySQL/Testcontainers

Os testes `*IT` são opt-in e pertencem ao perfil Maven `integration`. Eles usam Testcontainers com MySQL 8.4.

O ambiente atual não possui Docker, portanto essa suíte não foi executada. Ela deliberadamente **não foi substituída por H2**, porque isso não validaria as particularidades reais de MySQL/JPA exigidas pelo projeto.

No ambiente com Docker:

```powershell
mvn -Pintegration verify
```

continua sendo a validação indicada.

## 8. Decisão de prontidão

### Bloqueadores funcionais conhecidos das Etapas 01–05

**Nenhum bloqueador funcional conhecido permanece nos itens revisados.**

Os defeitos R01–R09 e os achados adicionais de governança/catálogos permanecem corrigidos e cobertos pelas regressões direcionadas. A suíte Maven real, incluindo ArchUnit/JUnit/Mockito, está verde com **292/292 testes**.

### Pendências de infraestrutura de validação

Restam duas comprovações ambientais, não correções funcionais conhecidas:

1. executar `clean package` com o cache completo dos plugins Maven;
2. executar `-Pintegration verify` num ambiente com Docker/MySQL.

### Recomendação

A base está **funcionalmente pronta para servir de ponto de partida da Etapa 06**. Se for adotado o gate mais estrito possível, pode-se completar primeiro as duas validações ambientais acima; elas não apontam atualmente defeito conhecido no código.

A Etapa 06 continua sendo: **Pessoa, Vínculo Contratual, Matrícula e Desligamento**. Nenhum código dessa etapa foi iniciado nesta Revisão 4.
