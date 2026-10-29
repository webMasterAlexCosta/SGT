# SGT-GTRAT — Entrega final da Etapa 09 / Checkpoint C

**Data:** 09/09/2026

## Situação

**Etapa 09 concluída no recorte de frequência temporal e apta a avançar para a Etapa 10.**

A conclusão é baseada em execução real de testes, integração MySQL e auditoria do pacote; não representa homologação final do backend, que permanece prevista após as Etapas 10–12.

## Pacote final

- Arquivo: `SGT_GTRAT_BACKEND_JAVA_CHECKPOINT_C_ETAPA_09_BOOT3_JAVA17.zip`
- Tamanho: 1.272.021 bytes
- Entradas ZIP: 799
- Arquivos: 721
- Integridade ZIP: aprovada (`unzip -t`, 0 erros)
- Itens proibidos: 0
- SHA-256: `a49353068b54e56654019b6afd83d240c7a8d95ec675cf92565bf39af10eda28`

O pacote não contém `.env` real, `target`, `.git`, `node_modules`, classes/JARs compilados, logs, banco de dados, uploads, keystores/chaves privadas ou o bundle React antigo.

Apenas `.env.example` é incluído, com valores fictícios/de desenvolvimento a serem substituídos antes de produção.

## Validação da árvore limpa

A árvore limpa usada para o ZIP foi comparada com a árvore validada da Etapa 09 para `pom.xml`, fontes principais, testes, `application.properties` e scripts. O digest das duas árvores foi idêntico:

`d9094002051a072e03c8bd3a172b03e3977dacba309c6bad311b44584b027301`

Após a remoção do bundle React e dos artefatos gerados, a própria árvore limpa executou novamente:

- 517 fontes Java principais compiladas com `release 17`;
- 112 fontes Java de teste do backend compiladas com `release 17`;
- 359/359 testes JUnit aprovados;
- 0 falhas, 0 erros e 0 ignorados;
- `BUILD SUCCESS`;
- T01–T110 aprovados novamente.

## Integração MySQL

A versão final da Etapa 09 foi validada previamente no MySQL Community Server 8.4.11 real, em banco descartável e vazio:

- 359/359 testes comuns aprovados;
- 18/18 testes de integração MySQL aprovados;
- `BUILD SUCCESS`.

O caminho padrão com Testcontainers permanece disponível para execução em ambiente com Docker.

## Plataforma

- Spring Boot 3.5.16;
- Java alvo 17 (`maven.compiler.release=17`);
- nenhuma configuração ativa retornou a Spring Boot 4.

### Limitação de runtime

O ambiente de execução disponível possui JVM 21.0.11. O projeto foi compilado com `release 17`, porém o JAR de validação registra `Build-Jdk-Spec: 21`. Antes da homologação final, repetir o pipeline em JDK 17 real.

## Aderência funcional da Etapa 09

- `FRE-001` a `FRE-010`: implementados;
- `FRE-011`: parcial apenas na emissão/contagem de pendências de relatório, reservada à Etapa 11;
- `FRE-012`: parcial apenas no versionamento formal de emissões anteriores de relatório, reservado à Etapa 11;
- `FRE-013` a `FRE-019`: implementados;
- `ALO-006`: concluído no consumidor de frequência;
- `VIN-014`: repercussão em frequência concluída; repercussão de relatórios permanece para a Etapa 11.

## Próxima etapa

Prosseguir para a **Etapa 10 — Perfis, permissões granulares e minimização de dados**.

O React permanece adiado até o congelamento das APIs do backend.
