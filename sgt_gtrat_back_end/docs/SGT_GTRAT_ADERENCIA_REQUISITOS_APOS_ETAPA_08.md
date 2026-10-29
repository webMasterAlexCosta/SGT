# SGT-GTRAT — Aderência aos requisitos após a Etapa 08

Data-base: **09/09/2026**  
Referência: `LEVANTAMENTO_REQUISITOS_V8_REFERENCIA.md`

## 1. Resultado do recorte

Os 10 requisitos do grupo Documentos e SEI estão implementados no backend.

| Situação | Quantidade |
|---|---:|
| Implementado | 10 |
| Parcial | 0 |
| Não implementado no recorte | 0 |

## 2. Matriz DOC-001–DOC-010

| Requisito | Situação | Evidência principal |
|---|---|---|
| DOC-001 | Implementado | Publicação do Contrato valida fonte preparada ou SEI e publica a fonte no agregado canônico. |
| DOC-002 | Implementado | Aditamento exige fonte por anexo, SEI ou ambos antes da persistência negocial. |
| DOC-003 | Implementado | Remanejamento materializa SEI ou anexo preparado quando informado, sem tornar a fonte obrigatória. |
| DOC-004 | Implementado | A referência SEI possui índice de consulta, mas não restrição de unicidade; grupos e Contratos permanecem independentes. |
| DOC-005 | Implementado | `retificarSei` cria nova versão, relaciona a anterior e registra evento com SEI anterior/novo. |
| DOC-006 | Implementado | `substituir` mantém o SEI e registra `ANEXO_ACRESCENTADO` quando a versão anterior era somente SEI. |
| DOC-007 | Implementado | Documento publicado pode ser substituído ou invalidado; exclusão física é bloqueada fora de rascunho. |
| DOC-008 | Implementado | Rascunho exclusivo sem SEI pode ser descartado; referência SEI ou publicação bloqueiam exclusão física. |
| DOC-009 | Implementado | Cada documento registra classificação própria `NEGOCIAL` ou `PESSOAL_RESTRITO`. |
| DOC-010 | Implementado | Acesso restrito exige finalidade e autorização conforme perfil; Operacional é negado e todo acesso autorizado é auditado. |

## 3. Requisitos transversais reforçados

| Requisito | Situação após a Etapa 08 |
|---|---|
| AUD-001 | Documento e evento registram responsável, tempos, motivo e antes/depois. |
| AUD-002 | Retificação preserva original, nova versão, motivo, autor e datas. |
| AUD-003 | Data efetiva e instante de registro permanecem distintos. |
| AUD-005 | Chave de idempotência impede duplicidade acidental; materialização automática usa SHA-256. |
| AUD-007 | Grupo lógico e vínculo com versão anterior permitem retificação sucessiva sem limite artificial. |
| PER-009 | Documentos negociais seguem o escopo de consulta do Contrato; documento pessoal permanece oculto sem necessidade/permissão. |
| PER-011 | Presidente não recebe conteúdo restrito automaticamente e o acesso excepcional deixa trilha. |

`PER-009` e `PER-011` ainda serão reavaliados transversalmente na Etapa 10, junto aos perfis Gestor Nacional/Estadual e à minimização de todos os DTOs do sistema. Isso não reduz a conclusão de `DOC-010` no módulo documental.

## 4. Requisitos ainda fora deste checkpoint

Permanecem explicitamente reservados:

- `FRE-001` a `FRE-019`: Etapa 09;
- fechamento de `ALO-006` no consumidor de frequência: Etapa 09;
- repercussões completas de `VIN-014` em frequência e relatórios: Etapas 09 e 11;
- revisão global de `PER-001` a `PER-012`: Etapa 10;
- relatórios oficiais, auditoria consolidada e Central de Pendências: Etapa 11;
- carga, concorrência e validação final: Etapa 12.

## 5. Conclusão

O domínio documental está aderente ao recorte V8 e não depende de exclusão ou reescrita silenciosa. O histórico publicado, o acesso restrito e a fonte de cada fato permanecem reproduzíveis e auditáveis.
