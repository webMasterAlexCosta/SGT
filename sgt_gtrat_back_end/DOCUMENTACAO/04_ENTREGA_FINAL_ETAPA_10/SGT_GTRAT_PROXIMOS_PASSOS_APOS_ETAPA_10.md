# SGT-GTRAT — Próximos passos após a Etapa 10

**Data-base:** 09/09/2026

## 1. Etapa 11 — Relatórios, indicadores, auditoria consolidada e Central de Pendências

Prioridades:

- implementar `REL-001` a `REL-006`;
- concluir `FRE-011` com aviso `sujeito a revisão` e quantidade de pendências em tela/exportação;
- concluir `FRE-012` com versionamento formal de emissões anteriores à retificação;
- concluir o reflexo restante de `VIN-014` nos relatórios;
- fechar `VIN-008` pela Central de Pendências;
- consolidar indicadores temporalmente corretos;
- distinguir emissão original, superada e retificada;
- aplicar a política de minimização da Etapa 10 a todo novo relatório e exportação.

## 2. Etapa 12 — Fechamento do Java

- carga e volume ampliados;
- concorrência e idempotência transversais;
- testes adversariais de segurança e indisponibilidade;
- regressão integral;
- matriz final dos 198 requisitos;
- execução completa em JDK 17 e MySQL 8.4;
- execução Testcontainers/Docker em ambiente com Docker;
- auditoria e ZIP Java finais.

## 3. React — somente depois

O frontend deve começar após o congelamento das APIs no fechamento Java. O primeiro trabalho será adaptar autenticação/guards para `perfil` e `autoridade`, seguido por permissões excepcionais, minimização e novos fluxos temporais.

## 4. Regras que não podem regredir

- Java 17 e Spring Boot 3.5.16;
- banco novo por versão estrutural durante o desenvolvimento;
- nenhuma reconciliação automática de dados antigos;
- um perfil principal por usuário;
- permissão excepcional não é perfil;
- Gestor Local é o escritor negocial;
- Administrador, Estado, Nacional e Presidente não ganham escrita por conveniência;
- concessão excepcional sempre tem beneficiário, escopo, validade, finalidade, autorização e auditoria;
- dado pessoal é minimizado por padrão;
- documento restrito continua protegido;
- passado vem da linha temporal, nunca da posição atual;
- fato publicado e versão anterior não são apagados.
