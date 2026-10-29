# SGT-GTRAT — Entrega final da Etapa 10

**Data-base:** 09/09/2026  
**Status:** concluída e validada

## Síntese executiva

A Etapa 10 conclui integralmente `PER-001` a `PER-012`. O backend agora trabalha com um perfil canônico único por usuário, separa administração técnica de escrita negocial, limita cada consulta ao escopo correto, minimiza dados pessoais e fornece um ciclo completo para permissões excepcionais.

Foram introduzidos:

- `PerfilAcesso` com seis perfis;
- `TipoPermissaoExcepcional` e entidades de concessão/uso;
- concessão, revogação, expiração, escopo e auditoria de exceções;
- correção versionada e idempotente de número/ano de contrato;
- gates de permissão para redução excepcional e transferência de Gestão Local;
- minimização central aplicada a pessoas, empregados, vínculos, frequência e relatório de terceirizados;
- acesso restrito com finalidade e trilha de uso;
- matriz HTTP e defesa em profundidade nos serviços;
- contrato `.env` inicializável no Spring Boot 3.5.16;
- Failsafe apto a executar a suíte real de MySQL.

## Evidências principais

| Validação | Resultado |
|---|---|
| regressão Java | 375 testes; 0 falhas; 0 erros; 0 ignorados |
| testes novos da Etapa 10 | 16 |
| integração MySQL 8.4.11 | 18 testes; todos aprovados |
| carga | 463.263 operações; 30.855,56 ops/s; P95 2 ms; 0 erros |
| integridade concorrente | 93.001 escritas = 93.001 incrementos persistidos |
| inicialização do JAR | schema criado em base vazia; Tomcat e HTTP ativos |
| modelo antigo de roles em produção | nenhuma ocorrência |

## Aderência

| Grupo | Implementado | Parcial | Pendente |
|---|---:|---:|---:|
| PER (12) | 12 | 0 | 0 |
| total global (198) | 133 | 48 | 17 |

O total global é conservador: requisitos de relatórios, Central de Pendências e fechamento final continuam nas Etapas 11 e 12.

## Decisões de integração

- A API usa `perfil` e `autoridade`; `role`/`roles` foram removidos.
- A escrita negocial comum é exclusiva de `GESTOR_LOCAL` no próprio escopo.
- O Administrador concede/revoga exceções, mas não se torna responsável negocial automático.
- A permissão excepcional é temporária, específica, revogável e auditável.
- O Presidente recebe apenas consulta institucional; dados pessoais comuns não são expostos.
- O banco da Etapa 10 deve ser novo. Não há migração automática do modelo anterior.

## Próximo checkpoint

A próxima etapa é a Etapa 11: relatórios, indicadores, auditoria consolidada e Central de Pendências. O React continua posterior ao fechamento do backend Java.

Consulte também:

- `LEIA-ME_ETAPA_10.md`;
- `SGT_GTRAT_RESULTADO_ETAPA_10.md`;
- `SGT_GTRAT_ADERENCIA_REQUISITOS_APOS_ETAPA_10.md`;
- `SGT_GTRAT_EVIDENCIAS_TESTES_ETAPA_10.md`;
- `SGT_GTRAT_API_PERFIS_PERMISSOES_ETAPA_10.md`;
- `SGT_GTRAT_PROXIMOS_PASSOS_APOS_ETAPA_10.md`.
