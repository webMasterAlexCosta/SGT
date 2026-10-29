# SGT-GTRAT — Resultado final da Etapa 09

**Data:** 09/09/2026

## Decisão

**ETAPA 09 CONCLUÍDA NO RECORTE DE FREQUÊNCIA TEMPORAL E APTA A AVANÇAR PARA A ETAPA 10.**

Esta decisão não equivale à homologação final do backend. Permanecem as etapas 10, 11 e 12, além da execução final em JDK 17 real e do perfil Testcontainers em ambiente com Docker.

## Resultado técnico

| Verificação | Resultado |
|---|---|
| Spring Boot | 3.5.16 |
| Java alvo | 17 (`release 17`) |
| Fontes principais | 517 |
| Fontes de teste | 112 |
| Suíte JUnit | 359/359 aprovados |
| Testes específicos Etapa 09 | 16/16 aprovados |
| Integração MySQL 8.4.11 | 18/18 aprovados |
| Checkpoints | T01–T110 aprovados |
| JAR | Executável Spring Boot gerado |

## Resultado funcional

A frequência deixou de usar posição atual/legada como fonte para Contrato, Lotação, Turno e Cargo e passou a resolver o contexto canônico na data da jornada:

`Pessoa → Vínculo Contratual → Alocação → Contrato/posição temporal → Gestão histórica`

Foram implementados fechamento no 8º dia, retificação formal do Gestor, auditoria versionada, idempotência por intenção, revalidação retroativa, separação entre validade operacional e visibilidade histórica e exclusão de registros inválidos dos consumidores oficiais.

## Aderência FRE

| Situação | Quantidade |
|---|---:|
| Implementado integralmente nesta etapa | 17 |
| Parcial por dependência explícita da Etapa 11 | 2 |
| Não implementado no recorte de frequência | 0 |

Os parciais são `FRE-011` e `FRE-012`, exclusivamente no trecho de emissão/versionamento de relatórios.

## Banco real

Foi usado MySQL Community Server 8.4.11 em banco descartável e vazio. A execução final do perfil de integração terminou `BUILD SUCCESS`, com 18 testes de integração aprovados após a suíte comum de 359 testes.

## Limitação honesta de ambiente

A JVM disponível é Java 21.0.11. Maven compilou com `release 17`, porém o JAR empacotado registra `Build-Jdk-Spec: 21`. Isso deve ser repetido em JDK 17 real antes da homologação para eliminar a última limitação de evidência de runtime.

## Limpeza do ZIP backend

O pacote de fontes final exclui o `dist`/bundle React antigo de `src/main/resources/static` (85 arquivos). O frontend antigo não faz parte do Checkpoint C e será reconstruído na fase React; isso evita distribuir JavaScript compilado com metadados tecnológicos obsoletos junto ao backend atualizado.


## Pacote final materializado

- `SGT_GTRAT_BACKEND_JAVA_CHECKPOINT_C_ETAPA_09_BOOT3_JAVA17.zip`
- integridade ZIP: aprovada;
- 799 entradas / 721 arquivos;
- itens proibidos encontrados: 0;
- SHA-256: `a49353068b54e56654019b6afd83d240c7a8d95ec675cf92565bf39af10eda28`.
