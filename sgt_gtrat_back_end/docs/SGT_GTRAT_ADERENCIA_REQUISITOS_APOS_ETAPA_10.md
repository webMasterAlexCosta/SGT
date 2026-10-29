# SGT-GTRAT — Aderência aos requisitos após a Etapa 10

**Data-base:** 09/09/2026  
**Referência funcional:** levantamento V8 consolidado

## 1. Matriz PER-001–PER-012

| Requisito | Situação | Evidência principal |
|---|---|---|
| `PER-001` | Implementado | Gestor Local é o único perfil de escrita negocial local; HTTP e serviços validam a Gestão do contrato. |
| `PER-002` | Implementado | Gestor Estadual consulta/audita apenas o próprio estado e é excluído de aditamento, remanejamento, encerramento e retificação negocial. |
| `PER-003` | Implementado | Gestor Nacional possui leitura/auditoria nacional e relatórios permitidos, sem escrita negocial. |
| `PER-004` | Implementado | Presidente possui consulta institucional nacional de contratos/documentos, sem escrita e sem acesso normal aos módulos pessoais. |
| `PER-005` | Implementado | Administrador técnico não é escritor negocial; exceções possuem tipo, escopo, validade, autorização, revogação e trilha imutável de uso. |
| `PER-006` | Implementado | Gestor Estadual recebe DTOs com CPF/matrícula mascarados e sem endereço, telefone, banco ou evidência pessoal desnecessária. |
| `PER-007` | Implementado | Correção de CPF permanece ato do Gestor Local no próprio escopo, com histórico de correção preservado. |
| `PER-008` | Implementado | Correção de número/ano exige `CORRECAO_IDENTIFICADOR_CONTRATO`, autorização coincidente, documento, idempotência, evento e antes/depois. |
| `PER-009` | Implementado | Estado, Nacional e Presidente consultam documentos negociais no respectivo escopo; classificação restrita da Etapa 08 permanece aplicada. |
| `PER-010` | Implementado | Frequência combina o perfil/escopo atual do usuário com Gestão, lotação e jornada históricas do registro. |
| `PER-011` | Implementado | Nacional e Presidente recebem minimização; dados adicionais exigem `ACESSO_DADOS_PESSOAIS_RESTRITOS` com finalidade e auditoria. |
| `PER-012` | Implementado | Transferência exige `TRANSFERENCIA_GESTAO_LOCAL`, autorização específica coincidente e execução pelo Gestor Local responsável; Estado não escreve. |

### Resultado PER

| Situação | Quantidade |
|---|---:|
| Implementado | 12 |
| Parcial | 0 |
| Pendente | 0 |

## 2. Matriz global conservadora dos 198 requisitos

A contagem promove somente os onze itens PER que ainda estavam parciais ou pendentes após a Etapa 09. Nenhum requisito de relatório, Central de Pendências ou homologação final foi antecipado.

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
| PER | 12 | 12 | 0 | 0 |
| HIS | 6 | 3 | 3 | 0 |
| **Total** | **198** | **133** | **48** | **17** |

## 3. Itens deliberadamente não promovidos

- `FRE-011` e `FRE-012`: aviso/contagem de pendências e versionamento formal de emissões pertencem à Etapa 11;
- `VIN-008`: Central de Pendências pertence à Etapa 11;
- parcela restante de `VIN-014`: reflexo completo em relatórios pertence à Etapa 11;
- `REL-001` a `REL-006`: fechamento da Etapa 11;
- carga ampliada, segurança adversarial e matriz final: Etapa 12.

## 4. Conclusão

O grupo PER está integralmente implementado. A contagem global permanece conservadora e rastreável, sem declarar itens de etapas futuras como concluídos.
