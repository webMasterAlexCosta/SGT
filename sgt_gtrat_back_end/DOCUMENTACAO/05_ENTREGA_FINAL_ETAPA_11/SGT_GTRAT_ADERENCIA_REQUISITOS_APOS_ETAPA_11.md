# SGT/GTRAT — Aderência aos requisitos após a Etapa 11

**Data-base:** 10/09/2026  
**Referência funcional:** levantamento V8 consolidado

## 1. Requisitos fechados nesta etapa

| Requisito | Situação | Evidência principal |
|---|---|---|
| `VIN-008` | Implementado | Scheduler e sincronização sob consulta detectam vínculo aberto após o fim contratual, preservam sua inatividade temporal e geram pendência administrativa idempotente. |
| `VIN-014` | Implementado | Retificação versionada já revalidava frequência; agora também invalida emissões cujo período e escopo foram atingidos, sem apagar o histórico. |
| `FRE-011` | Implementado | Consultas, DTOs e exportações PDF/XLSX exibem `SUJEITO A REVISÃO` e a quantidade de pendências ativas no recorte. |
| `FRE-012` | Implementado | A visão oficial usa a verdade corrigida; a emissão anterior permanece como snapshot imutável e encadeado. |
| `REL-001` | Implementado | Relatório vigente é recalculado pela última verdade válida e emissões anteriores permanecem consultáveis. |
| `REL-002` | Implementado | Nova emissão após invalidação recebe natureza `RETIFICADA`, número de versão e data/hora. |
| `REL-003` | Implementado | Consulta por instante recupera a última emissão formal conhecida até aquela data; comparação separa histórico e vigente. |
| `REL-004` | Implementado | Prévia, emissão e exportação não simulam definitividade quando há retificação pendente. |
| `REL-005` | Implementado | Emissão registra período, geração, usuário, perfil, versão, pendências, natureza, formato, filtros, hash e escopo. |
| `REL-006` | Implementado | Retificação invalida seletivamente a consolidação atingida, recalcula a visão vigente e conserva o snapshot superado. |
| `AUD-006` | Implementado | Central identifica origem, motivo, responsável, estado e escopo; a resolução revalida a origem e o bloqueio é mínimo. |

## 2. Matriz global conservadora dos 198 requisitos

A contagem promove somente os onze itens acima. Itens de carga, concorrência transversal, segurança adversarial e homologação final não são antecipados.

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
| VIN | 15 | 15 | 0 | 0 |
| ALO | 8 | 8 | 0 | 0 |
| DES | 6 | 6 | 0 | 0 |
| FRE | 19 | 19 | 0 | 0 |
| REL | 6 | 6 | 0 | 0 |
| AUD | 7 | 1 | 6 | 0 |
| PER | 12 | 12 | 0 | 0 |
| HIS | 6 | 3 | 3 | 0 |
| **Total** | **198** | **144** | **44** | **10** |

## 3. Leitura do resultado

- VIN, FRE, REL e PER estão integralmente implementados na matriz funcional;
- a Central fecha o requisito específico `AUD-006`, mas os demais itens AUD permanecem conservadoramente parciais até a auditoria final;
- os 10 pendentes estão concentrados em temporalidade/contrato/catálogos e dependem da revisão final prevista para a Etapa 12;
- os 44 parciais não foram promovidos apenas por evidência indireta.

## 4. Conclusão

A Etapa 11 atende seu recorte completo, sem declarar o backend globalmente homologado antes dos testes nativos, adversariais e de carga da Etapa 12.
