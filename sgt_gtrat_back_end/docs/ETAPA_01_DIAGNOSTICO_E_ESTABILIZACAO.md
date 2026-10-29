# SGT/GTRAT — Etapa 01 — Diagnóstico e Estabilização do Backend Java

**Data:** 08/09/2026  
**Base recebida:** `sgt_gtrat_back_end(3).zip`  
**Requisitos de referência:** `SGT_GTRAT_HISTORICO_COMPLETO_LEVANTAMENTO_REQUISITOS_V8_FINAL`  
**Status da etapa:** **CONCLUÍDA COM VALIDAÇÃO ESTÁTICA E PREFLIGHT LOCAL**

---

## 1. Objetivo da etapa

Estabelecer uma base Java tecnicamente previsível antes da remodelagem negocial, sem antecipar alterações funcionais dos Passos 2 a 12.

Nesta etapa não foram remodelados Contrato, Pessoa, Vínculo, Alocação, Frequência ou Relatórios. A prioridade foi preservar o comportamento existente e eliminar problemas de execução/checkout que poderiam contaminar as próximas implementações.

---

## 2. Inventário confirmado da base

Foram identificados:

- **363 classes Java de produção** em `src/main/java`;
- **94 classes Java de teste** em `src/test/java`;
- **32 controllers**;
- **110 DTOs**;
- **69 services**;
- **44 repositories**;
- **41 entidades JPA**;
- testes arquiteturais já existentes, incluindo `ControllerLayerArchitectureTest`;
- infraestrutura existente de Spring Security/JWT, 2FA, Redis, auditoria, frequência, relatórios e testes.

### Estrutura de camadas

Foi feita verificação estática dos controllers.

**Resultado:** nenhum dos 32 controllers importa classes do pacote `repositorio` diretamente.

Isso confirma que a base atual já possui uma separação importante que deve ser preservada:

`Controller -> DTO/Service -> Repository`

---

## 3. Ambiente Java/Maven

### Java

O projeto declara:

```xml
<java.version>17</java.version>
```

O ambiente utilizado na análise possui:

```text
OpenJDK 17.0.11
```

Portanto a versão Java do projeto está coerente com Java 17.

### Maven

O projeto utiliza Maven Wrapper configurado para Maven 3.9.16.

O Wrapper estava presente, porém o arquivo POSIX `mvnw` apresentava dois problemas no ZIP original:

1. finais de linha `CRLF`;
2. ausência de permissão de execução.

Isso causava em Linux:

```text
./mvnw: Permission denied
```

Esses problemas foram corrigidos nesta etapa.

---

## 4. Alterações realizadas

### 4.1. Normalização do Maven Wrapper

Arquivo:

```text
mvnw
```

Alterações:

- convertido de CRLF para LF;
- permissão de execução aplicada;
- conteúdo funcional preservado.

### 4.2. Normalização dos scripts POSIX

Arquivo existente:

```text
scripts/build-with-frontend.sh
```

Alterações:

- convertido de CRLF para LF;
- permissão de execução aplicada.

Embora o React não seja objeto desta fase, o script foi apenas normalizado tecnicamente; nenhuma implementação de frontend foi realizada.

### 4.3. Inclusão de `.gitattributes`

Foi adicionado:

```text
.gitattributes
```

Objetivo:

- manter `mvnw` e `*.sh` em LF;
- manter scripts Windows em CRLF;
- reduzir regressões de checkout entre Windows e Linux;
- normalizar arquivos Java/XML/properties/yaml/Markdown.

### 4.4. Script de validação Linux/macOS

Adicionado:

```text
scripts/validar-backend.sh
```

Modos suportados:

```text
preflight
validate
test
package
completo
```

Exemplos:

```bash
./scripts/validar-backend.sh preflight
./scripts/validar-backend.sh test
./scripts/validar-backend.sh completo
```

O `preflight` valida:

- Java disponível;
- Java 17;
- presença do `pom.xml`;
- Maven Wrapper;
- permissão do Wrapper;
- ausência de CRLF em scripts POSIX.

### 4.5. Script de validação Windows

Adicionado:

```text
scripts/validar-backend.ps1
```

Exemplos:

```powershell
.\scripts\validar-backend.ps1 preflight
.\scripts\validar-backend.ps1 test
.\scripts\validar-backend.ps1 completo
```

O script usa `mvnw.cmd`, permitindo validar o projeto sem exigir instalação global de Maven.

---

## 5. Validações executadas

### Preflight

Executado:

```text
./scripts/validar-backend.sh preflight
```

Resultado:

```text
Preflight OK: Java 17.0.11, Wrapper e scripts POSIX consistentes.
```

### POM

O `pom.xml` foi validado como XML bem-formado.

### Configuração `.env`

Foi comparado o conjunto de placeholders utilizados por `application.properties` com `.env.example`.

Resultado relevante:

- os parâmetros negociais/configuráveis principais estão representados;
- `ENV_FILE` é uma variável especial usada para localizar outro arquivo de ambiente e não precisa estar preenchida no `.env.example`;
- `REDIS_MAXMEMORY` existe no `.env.example`, mas não é consumida diretamente pelo `application.properties`, pois está relacionada à configuração operacional do Redis;
- o `.env` recebido não possui algumas variáveis de relatórios temporários, mas essas propriedades possuem valores padrão no `application.properties`.

---

## 6. Limitação da validação Maven neste ambiente

A suíte Maven completa não pôde ser executada dentro do ambiente de análise porque o Maven Wrapper precisa acessar os repositórios externos para baixar Maven/dependências e o ambiente de execução utilizado não possui resolução de rede disponível para esse download.

A falha observada foi externa ao código:

```text
curl: (6) Could not resolve host: repo.maven.apache.org
```

Portanto:

- **não foi registrado falso sucesso de `mvn test`;**
- o preflight local foi executado com sucesso;
- os scripts para executar a suíte completa estão incluídos no ZIP;
- em ambiente com acesso ao Maven Central, deve ser executado:

```powershell
.\scripts\validar-backend.ps1 completo
```

ou:

```bash
./scripts/validar-backend.sh completo
```

---

## 7. Pontos positivos encontrados e preservados

Foram preservados nesta etapa:

- organização Controller/Service/Repository;
- DTOs existentes;
- Spring Security/JWT;
- autenticação e 2FA existentes;
- auditoria existente;
- Redis e cache existentes;
- fechamento de frequência existente;
- relatórios existentes;
- testes JUnit/Mockito/ArchUnit existentes;
- perfis Maven de integração, carga e mutation testing;
- configuração de timezone `America/Sao_Paulo` existente.

Nenhuma dessas áreas foi reescrita nesta etapa.

---

## 8. Incompatibilidades funcionais identificadas para as próximas etapas

Estas incompatibilidades foram **diagnosticadas**, mas propositalmente ainda não alteradas na Etapa 1.

### 8.1. Contrato ainda possui modelo simplificado

A entidade atual possui, entre outros:

```text
numeroContrato
texto tipo
dataInicio
dataFim
ativo
efetivoPrevisto
```

Problemas frente ao V8:

- número + ano ainda não formam a identidade canônica;
- `numeroContrato` é atualmente único isoladamente;
- Tipo de Contratação ainda é texto no Contrato;
- situação temporal e habilitação administrativa ainda não estão separadas;
- não há modelo completo de RASCUNHO/FUTURO/VIGENTE/ENCERRADO/CANCELADO/ANULADO;
- composição contratual temporal ainda não representa o núcleo final;
- fotografia integral da publicação ainda precisa ser modelada.

Esses itens são prioridade do **Passo 2**.

### 8.2. Empregado ainda concentra conceitos que o V8 separa

A entidade atual ainda concentra informações como:

- matrícula;
- Lotação;
- Jornada/Turno;
- Cargo;
- Gestão Local;
- contrato atual;
- data de admissão.

O V8 determina separação entre:

```text
Pessoa
  -> Vínculo Contratual
       -> Alocação temporal
```

A matrícula deve pertencer ao Vínculo.

Essa remodelagem ocorrerá posteriormente, principalmente nos Passos 6 e 7, para não quebrar frequência e relatórios prematuramente.

### 8.3. Frequência possui configuração legada de 30 dias

Foi encontrada configuração atual:

```text
FREQUENCIA_MAX_RETROACTIVE_USER_DAYS=30
```

O V8 consolidado determina janela comum do Usuário Operacional de **7 dias corridos**, considerando `America/Sao_Paulo`.

A configuração não foi alterada agora para evitar introduzir parcialmente a nova regra sem o motor temporal correspondente.

A correção será realizada no **Passo 9** junto com os testes de fechamento e retificação.

---

# 9. Próxima etapa — Passo 2

## Remodelagem do domínio Contrato

**Esforço:** MUITO ALTO

O objetivo do Passo 2 será criar a fundação contratual temporal sem quebrar prematuramente os módulos dependentes.

## 9.1. Identificação do Contrato

Implementar:

- número;
- ano;
- unicidade `numero + ano`;
- rascunho;
- reserva do identificador após publicação;
- liberação somente quando rascunho nunca publicado for descartado;
- estrutura para correção excepcional versionada.

## 9.2. Tipo de Contratação

Retirar progressivamente o uso de texto livre no Contrato e implementar relacionamento com cadastro próprio de Tipo de Contratação.

## 9.3. Empresa contratada

Preparar o Contrato para preservar:

- CNPJ;
- razão social;
- nome fantasia;
- empresa/fornecedor correspondente;
- fotografia histórica publicada.

Mudança real de empresa e correção material de CNPJ não serão tratadas como a mesma operação.

## 9.4. Ciclo de vida

Separar situação temporal/negocial de habilitação administrativa.

Situações previstas:

```text
RASCUNHO
FUTURO
VIGENTE
ENCERRADO
CANCELADO
ANULADO
```

Habilitação administrativa será eixo separado.

## 9.5. Situação temporal derivada

Preparar o domínio para que:

- antes da vigência -> FUTURO;
- dentro da vigência -> VIGENTE;
- depois do fim -> ENCERRADO.

A passagem do tempo não deverá criar fatos negociais artificialmente.

## 9.6. Publicação

Implementar fundação para publicação com:

- validação de obrigatórios;
- identificação única;
- fonte comprobatória;
- fotografia integral original;
- preservação histórica.

A validação completa da soma das composições será consolidada juntamente com o Passo 3.

## 9.7. Inativação administrativa

Preparar separação entre:

```text
situação temporal do Contrato
```

e:

```text
habilitação administrativa
```

Inativação não deverá representar automaticamente encerramento do Contrato.

## 9.8. Estratégia de compatibilidade

Como muitos módulos atuais dependem diretamente da entidade `Contrato`, o Passo 2 será implementado de forma controlada.

Prioridades:

1. evitar remoção destrutiva imediata de campos legados usados por frequência/relatórios;
2. introduzir o novo modelo contratual de forma compatível;
3. adaptar services e DTOs do domínio de Contrato;
4. criar testes para as novas regras;
5. somente remover dependências antigas quando os consumidores tiverem sido migrados.

Isso reduz o risco de repetir a quebra generalizada ocorrida na versão anterior do projeto.

---

# 10. Critério para concluir o Passo 2

O Passo 2 somente será considerado encerrado quando o backend possuir testes cobrindo, no mínimo:

- unicidade número + ano;
- rascunho;
- publicação;
- contrato futuro;
- contrato vigente;
- contrato encerrado por derivação temporal;
- cancelamento de contrato futuro;
- distinção inicial de anulação;
- habilitação administrativa separada;
- validação de vigência;
- correção excepcional estruturada/auditável;
- preservação da compatibilidade necessária para os módulos ainda não migrados.

Ao término será produzido:

```text
ZIP JAVA — ETAPA 02
MD — IMPLEMENTADO NA ETAPA 02 + PLANO DA ETAPA 03
```

---

# 11. Estado geral após esta entrega

```text
ETAPA 01 - CONCLUÍDA
ETAPA 02 - PRÓXIMA
ETAPA 03 - PENDENTE
ETAPA 04 - PENDENTE
ETAPA 05 - PENDENTE
ETAPA 06 - PENDENTE
ETAPA 07 - PENDENTE
ETAPA 08 - PENDENTE
ETAPA 09 - PENDENTE
ETAPA 10 - PENDENTE
ETAPA 11 - PENDENTE
ETAPA 12 - PENDENTE
```

**Checkpoint A** será produzido ao concluir os Passos 1, 2 e 3. Mesmo assim, conforme solicitado, haverá um ZIP individual ao final de cada etapa.
