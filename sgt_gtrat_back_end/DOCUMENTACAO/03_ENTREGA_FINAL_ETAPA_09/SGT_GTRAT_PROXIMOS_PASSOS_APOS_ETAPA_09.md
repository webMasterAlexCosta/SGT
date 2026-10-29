# SGT-GTRAT — Próximos passos após a Etapa 09

**Data-base:** 09/09/2026

## 1. Etapa 10 — Perfis, permissões e minimização de dados

Prioridades:

- revisar transversalmente `PER-001` a `PER-012`;
- consolidar Administrador, Gestor Nacional, Gestor Estadual, Gestor Local, Presidente e Operacional;
- garantir que Administrador técnico não seja responsável negocial automático;
- consolidar concessão/revogação de permissões excepcionais;
- aplicar minimização de dados pessoais por perfil em todos os DTOs relevantes;
- revisar escopos históricos e atuais de consulta/escrita;
- fechar `PER-012`.

## 2. Etapa 11 — Relatórios, indicadores, auditoria consolidada e Central de Pendências

Deve fechar especialmente:

- `REL-001` a `REL-006`;
- `FRE-011`: aviso `sujeito a revisão` e quantidade de pendências em visualização/exportação;
- `FRE-012`: versionamento formal de emissões de relatório anteriores à retificação;
- parcela restante de `VIN-014` em relatórios;
- `VIN-008` por meio da Central de Pendências;
- consolidação de indicadores temporalmente corretos;
- versão original/retificada de emissões administrativas.

## 3. Etapa 12 — Fechamento do Java

- carga e volume;
- concorrência ampliada;
- idempotência transversal;
- regressão final;
- testes de segurança e indisponibilidade;
- matriz final dos 198 requisitos;
- execução integral em **JDK 17 real**;
- execução `-Pintegration verify` via **Testcontainers/Docker**;
- revisão do ZIP Java final.

## 4. Regras que não devem regredir

- Spring Boot 3.5.16;
- Java 17 como alvo definitivo;
- banco novo/descartável durante desenvolvimento;
- sem rotina automática de migração/reconciliação de legado;
- fato publicado não é apagado;
- passado vem da linha temporal, nunca da posição atual;
- frequência pertence à data de início da jornada;
- retificação preserva anterior;
- frequência inválida historicamente permanece auditável, mas não entra na verdade operacional;
- React somente após congelamento das APIs do backend.
