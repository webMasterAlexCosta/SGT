# SGT-GTRAT — Aderência aos requisitos após a Etapa 09

**Data-base:** 09/09/2026  
**Referência funcional:** levantamento V8 consolidado

## 1. Matriz FRE-001–FRE-019

| Requisito | Situação | Evidência principal |
|---|---|---|
| FRE-001 | Implementado | Resolvedor usa Vínculo e Alocação temporalmente válidos na data negocial. |
| FRE-002 | Implementado | Política de sete dias corridos em `America/Sao_Paulo`, data como dia 1. |
| FRE-003 | Implementado | Borda 07/01 23:59:59 aberta e 08/01 00:00 fechada coberta por teste. |
| FRE-004 | Implementado | Alteração operacional registra evento com antes/depois, autor e instante. |
| FRE-005 | Implementado | Após fechamento, mudança comum do Operacional é bloqueada; Gestor retifica. |
| FRE-006 | Implementado | Toda escrita do Gestor exige justificativa, inclusive dentro dos sete dias. |
| FRE-007 | Implementado | Não existe prazo máximo legado para retificação do Gestor. |
| FRE-008 | Implementado | Retificação atualiza a frequência oficial; consumidores recalculam a partir de `VALIDA`; versões da frequência permanecem em eventos. |
| FRE-009 | Implementado | Vínculo/Alocação/Contrato/Aditamento retroativos revalidam frequências atingidas sem sobrescrita silenciosa. |
| FRE-010 | Implementado | Perda retroativa de cobertura mantém histórico e marca `INVALIDA_COBERTURA`, excluída dos indicadores válidos. |
| FRE-011 | **Parcial** | Estado `SUJEITA_REVISAO` existe e é preservado; aviso/quantidade de pendências na emissão de relatório pertence à Etapa 11. |
| FRE-012 | **Parcial** | Retificação concluída volta a alimentar a verdade oficial; versionamento formal da emissão anterior de relatório pertence à Etapa 11. |
| FRE-013 | Implementado | Fechamento no início do 8º dia é persistido e auditado. |
| FRE-014 | Implementado | Retificação do Gestor gera versão formal e `bloqueadaEdicaoOperacional=true`. |
| FRE-015 | Implementado | Snapshots e autorização usam Alocação histórica da data, não posição atual. |
| FRE-016 | Implementado | Mudança retroativa não transfere autoria nem reassocia o registro; passa à revisão gerencial. |
| FRE-017 | Implementado | Registro que perde cobertura permanece auditável e sai do cálculo oficial. |
| FRE-018 | Implementado | Escrita combina autorização atual com Gestão/Lotação/Jornada históricas. |
| FRE-019 | Implementado | Visibilidade histórica, validade operacional e estado de auditoria são dimensões separadas. |

### Resultado FRE

| Situação | Quantidade |
|---|---:|
| Implementado | 17 |
| Parcial | 2 |
| Pendente | 0 |

## 2. Dependências transversais

| Requisito | Situação após Etapa 09 |
|---|---|
| ALO-006 | **Implementado** — frequência consome Alocação temporal canônica. |
| VIN-014 | **Parcial** — repercussão em frequência implementada; reflexo completo em relatórios permanece para Etapa 11. |
| PER-012 | Parcial — permanece para revisão granular na Etapa 10. |
| VIN-008 | Parcial — Central de Pendências permanece para Etapa 11. |

## 3. Matriz global conservadora dos 198 requisitos

A contagem abaixo parte da matriz formal pós-Etapa 08 e promove somente itens comprovados nesta Etapa 09. Não promove requisitos de Etapas 10–12 antecipadamente.

| Grupo | Total | Implementado | Parcial | Pendente |
|---|---:|---:|---:|---:|
| GOV | 2 | 1 | 1 | 0 |
| TMP | 17 | 4 | 10 | 3 |
| CON | 25 | 15 | 8 | 2 |
| EMP | 3 | 2 | 1 | 0 |
| CAT | 9 | 0 | 4 | 5 |
| CMP | 11 | 4 | 7 | 0 |
| RED | 8 | 6 | 2 | 0 |
| ADI | 10 | 9 | 1 | 0 |
| REM | 7 | 6 | 1 | 0 |
| DOC | 10 | 10 | 0 | 0 |
| COL | 12 | 12 | 0 | 0 |
| MAT | 5 | 5 | 0 | 0 |
| VIN | 15 | 13 | 2 | 0 |
| ALO | 8 | 8 | 0 | 0 |
| DES | 6 | 6 | 0 | 0 |
| FRE | 19 | 17 | 2 | 0 |
| REL | 6 | 0 | 0 | 6 |
| AUD | 7 | 0 | 6 | 1 |
| PER | 12 | 1 | 3 | 8 |
| HIS | 6 | 3 | 3 | 0 |
| **Total** | **198** | **122** | **51** | **25** |

## 4. Conclusão

O consumidor de frequência do novo modelo temporal está implementado e validado. A Etapa 09 pode ser encerrada sem declarar como concluídas as obrigações de emissão de relatórios, Central de Pendências, minimização global ou homologação final, que continuam rastreadas nas etapas posteriores.
