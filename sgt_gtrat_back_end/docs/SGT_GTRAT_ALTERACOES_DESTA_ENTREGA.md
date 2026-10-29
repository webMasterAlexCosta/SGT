# SGT-GTRAT — Alterações desta entrega (Etapa 10)

**Data:** 09/09/2026

## Código

1. Consolidado perfil principal único em `PerfilAcesso`.
2. Removido o modelo textual `role`/`roles` das fontes de produção.
3. Separadas consulta, auditoria, escrita negocial e administração técnica.
4. Criado ciclo versionado de permissões excepcionais, incluindo concessão, expiração, uso e revogação.
5. Integradas permissões específicas à correção de número/ano, redução excepcional e transferência de Gestão Local.
6. Implementada minimização central de dados pessoais por perfil e finalidade.
7. Bloqueados endpoints pessoais genéricos sem escopo.
8. Reforçada a combinação de autorização atual com escopo histórico da frequência.
9. Corrigidos Failsafe/Byte Buddy e import do `.env` no Spring Boot 3.5.16.
10. Adicionados 16 testes específicos da Etapa 10.

## Validação

- 375 testes Java aprovados;
- 18 testes de integração aprovados no MySQL 8.4.11;
- carga de 463.263 operações com zero erro e P95 de 2 ms;
- JAR iniciado integralmente em banco vazio;
- nenhuma referência de produção ao modelo antigo de roles.

Consulte `SGT_GTRAT_ENTREGA_FINAL_ETAPA_10.md` para o fechamento consolidado.
