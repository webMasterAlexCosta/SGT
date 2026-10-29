# SGT-GTRAT — Resultado final da Etapa 08

Data: **09/09/2026**

## Resultado

A Etapa 08 — Documentos, anexos e SEI foi concluída mantendo:

- Spring Boot **3.5.16**;
- Java **17**;
- banco novo, vazio e exclusivo por versão;
- backend como prioridade, sem iniciar nova implementação React.

## Evidências

| Verificação | Resultado |
|---|---|
| Requisitos DOC-001–DOC-010 | 10 de 10 implementados |
| Suíte JUnit | 342 aprovados; 0 falhas; 0 erros; 0 ignorados |
| Testes específicos da Etapa 08 | 15 aprovados |
| Checkpoints | T01–T90 aprovados |
| Fontes principais | 505 arquivos Java, `release 17` |
| Fontes de teste | 111 arquivos Java |
| JAR | Executável Spring Boot gerado |
| Manifesto | `Build-Jdk-Spec: 17`; `Spring-Boot-Version: 3.5.16` |

## ZIP entregue

Arquivo: `SGT_GTRAT_BACKEND_JAVA_ETAPA_08_BOOT3_JAVA17_REV1.zip`

- tamanho: **2.631.310 bytes**;
- entradas: **860**;
- teste de integridade do ZIP: aprovado;
- itens proibidos encontrados: **0**;
- SHA-256: `ced9f766bb0c1984d8d6d1cb1c44b2a4a8fa94439718d502f41e98c575b8c36d`.

O pacote não contém `.env`, credenciais, chaves, keystores, `target`, `.git`, `node_modules`, uploads, caches, classes compiladas ou logs.

## Limitação

O perfil MySQL/Testcontainers não foi executado porque o Docker não está disponível neste ambiente. Antes da homologação, executar em máquina com Docker:

```bash
./mvnw -B -ntp -Pintegration verify
```

Usar banco descartável, vazio e exclusivo desta versão.

## Próxima fase

Prosseguir para a **Etapa 09 — Frequência integrada ao novo modelo temporal**, conforme `SGT_GTRAT_PROXIMOS_PASSOS_APOS_ETAPA_08.md`. O React permanece adiado até a consolidação do backend.
