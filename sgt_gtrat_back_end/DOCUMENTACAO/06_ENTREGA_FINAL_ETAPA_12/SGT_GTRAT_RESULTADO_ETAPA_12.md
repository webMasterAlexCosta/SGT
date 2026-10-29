# SGT/GTRAT — Resultado da Etapa 12

## Veredito

**Etapa 12 concluída. Backend Java liberado para iniciar o React.**

| Critério | Resultado |
|---|---|
| Plataforma solicitada | Java 17 + Spring Boot 3.5.16 |
| Requisitos | 198 implementados; 0 parciais; 0 pendentes |
| Regressão | 396/396 aprovada |
| MySQL nativo | 22/22 aprovada em 8.4.11 e banco novo |
| Arranque do JAR | contexto completo iniciado; `/config.json` HTTP 200 |
| Carga | 450.821 operações; 0 falhas |
| Segurança direcionada | aprovada |
| API para React | congelada em 10/09/2026 |
| Migração/compatibilidade de banco antigo | não incluída |

## Decisões definitivas

1. Java 21 não integra esta entrega.
2. Spring Boot 4 não integra esta entrega.
3. O banco de desenvolvimento/teste parte vazio; não há scripts de conversão de schema anterior.
4. Um fato publicado não é removido fisicamente.
5. Ocupação contratual é calculada por Vínculo e Alocação temporal.
6. A API descrita no contrato congelado é a base da futura aplicação React.

## Condição para mudança posterior

Correção interna que preserve o contrato pode seguir normalmente. Mudança incompatível de rota, verbo, autenticação, DTO, código de erro ou semântica temporal exige registro explícito e, quando necessário, uma nova versão de API.
