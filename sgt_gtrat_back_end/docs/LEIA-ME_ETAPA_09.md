# SGT-GTRAT — Entrega da Etapa 09

**Data:** 09/09/2026  
**Checkpoint:** C — Alocação temporal + Documentos/SEI + Frequência temporal  
**Plataforma do projeto:** Spring Boot 3.5.16 + Java 17 (`maven.compiler.release=17`)

## Resultado

A Etapa 09 integrou a frequência ao modelo temporal canônico de Pessoa, Vínculo Contratual, Alocação e Contrato.

Principais garantias:

- frequência resolve Pessoa, Vínculo e Alocação válidos na data negocial;
- Contrato e Gestão Local são reconstruídos historicamente para a data da jornada;
- a posição atual do `Empregado` não reescreve a frequência passada;
- jornada noturna pertence à data de início;
- Operacional possui janela de sete dias corridos em `America/Sao_Paulo`, contando a própria data como dia 1;
- no início do 8º dia a frequência é `FECHADA` negocialmente;
- qualquer escrita do Gestor Local é retificação formal, com justificativa e idempotência;
- após intervenção do Gestor, o registro não retorna à edição do Operacional;
- fatos retroativos preservam o lançamento original e podem marcá-lo `SUJEITA_REVISAO` ou `INVALIDA_COBERTURA`;
- frequências fora da verdade operacional oficial permanecem auditáveis, mas não entram nos cálculos/painéis oficiais;
- eventos de frequência formam cadeia SHA-256;
- autorização de escrita combina permissão atual do usuário com o escopo histórico do registro;
- Administrador técnico não assume escrita negocial de frequência por padrão.

## Validações executadas

- 517 fontes Java principais compiladas com `release 17`;
- 112 fontes Java de teste compiladas com `release 17`;
- 359 testes JUnit/Surefire: **359 aprovados, 0 falhas, 0 erros, 0 ignorados**;
- 16 testes específicos da Etapa 09: **16 aprovados**;
- 18 testes de integração em MySQL 8.4.11 real: **18 aprovados, 0 falhas, 0 erros, 0 ignorados**;
- checkpoints T01–T110: **aprovados**;
- JAR Spring Boot executável gerado.

## Observação sobre JDK

O ambiente disponível nesta execução possui JVM **21.0.11**. O `pom.xml` fixa Java 17 e Maven compilou fontes e testes com `release 17`, comprovando o alvo de bytecode/configuração Java 17. Como o empacotamento ocorreu na JVM 21 disponível, o manifesto do JAR registra `Build-Jdk-Spec: 21`.

Antes da homologação final, executar também todo o pipeline em um **JDK 17 real** para obter evidência de runtime e empacotamento integralmente em JDK 17.

## MySQL

O MySQL 8.4.11 enviado foi executado diretamente, sem Docker, usando banco descartável `sgt_etapa09`. O perfil de integração foi adaptado para aceitar MySQL externo somente quando `INTEGRATION_MYSQL_URL` é informada. Sem essa variável, o caminho padrão continua sendo Testcontainers.

A execução nativa encontrou e permitiu corrigir uma query inválida do `CalendarioEventoRepositorio` e falhas do próprio gerador de smoke test. A execução final ficou verde.

## Aderência

- `FRE-001` a `FRE-010`: implementados;
- `FRE-011`: parcial — a frequência sinaliza revisão, mas aviso/quantidade de pendências na emissão de relatório pertence à Etapa 11;
- `FRE-012`: parcial — a verdade oficial da frequência é atualizada após retificação, mas versionamento formal de emissão anterior de relatório pertence à Etapa 11;
- `FRE-013` a `FRE-019`: implementados;
- `ALO-006`: concluído no consumidor de frequência;
- `VIN-014`: repercussão em frequência implementada; parcela de relatórios permanece para a Etapa 11.

## Próxima etapa

Prosseguir para a **Etapa 10 — Perfis, permissões granulares e minimização de dados**.

O React continua adiado até o congelamento das APIs do backend final.

## Limpeza do checkpoint backend

O ZIP de fontes desta Etapa 09 não inclui o bundle React antigo que estava em `src/main/resources/static`. Esses 85 artefatos compilados pertencem ao frontend anterior, contêm metadados tecnológicos obsoletos e não participaram da validação funcional do backend. O diretório será novamente preenchido quando o React for retomado após o congelamento das APIs. A configuração backend preparada para servir recursos estáticos permanece.

