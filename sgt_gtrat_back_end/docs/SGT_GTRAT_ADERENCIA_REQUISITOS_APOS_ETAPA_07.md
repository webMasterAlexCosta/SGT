# SGT-GTRAT — Aderência aos requisitos após a Etapa 07

Data-base: **09/09/2026**  
Referência: `LEVANTAMENTO_REQUISITOS_V8_REFERENCIA.md`

## 1. Resultado do recorte

A análise foi feita contra os requisitos fechados do levantamento V8, sem considerar o modelo legado de `Empregado` como prova de conclusão dos domínios temporais.

Dos 15 requisitos diretamente afetados pela Etapa 07:

- **12 estão implementados**;
- **3 estão parciais** por dependerem de consumidores ou permissões de etapas posteriores.

## 2. Matriz de aderência

| Requisito | Situação | Evidência e limite atual |
|---|---|---|
| ALO-001 | Implementado | `AlocacaoVinculo` representa faixa temporal inclusiva de Lotação + Turno + Cargo dentro do Vínculo. |
| ALO-002 | Implementado | Mudança cria faixa sucessora e fecha a anterior sem novo Vínculo ou desligamento. |
| ALO-003 | Implementado | Prévia aceita data retroativa, informa incompatibilidades e preserva faixas posteriores compatíveis e auditoria. |
| ALO-004 | Implementado | Cobertura bloqueia sobreposição; mesma data produz retificação e substitui formalmente a faixa anterior. |
| ALO-005 | Implementado | O último dia é inclusivo, a nova faixa vale na data informada e a prévia sinaliza mudança coincidente com o último dia. |
| ALO-006 | **Parcial** | O motor responde qual Alocação vale na data de início; o cálculo de frequência noturna ainda será migrado para esse motor na Etapa 09. |
| ALO-007 | Implementado | `garantirCobertura` exige exatamente uma Alocação para cada dia operacional do Vínculo, sem lacuna ou sobreposição. |
| ALO-008 | Implementado | Inativação não altera faixas; reativação revalida explicitamente a composição temporal das Alocações que voltarão a operar. |
| CON-013 | Implementado | Transferência excepcional possui data efetiva, motivo, auditoria, Gestão de origem/destino e preservação por período. |
| CON-023 | Implementado | O contexto posterior exige mapeamento explícito e autorização administrativa; a projeção só muda após o evento. |
| VIN-013 | Implementado | Inativação administrativa não encerra Vínculo nem Alocação e a reativação não cria novo Vínculo. |
| VIN-014 | **Parcial** | Retificação de início sincroniza e revalida Alocação; frequência, relatórios e pendências serão concluídos nas Etapas 09 e 11. |
| HIS-002 | Implementado | Consulta mensal expõe todos os períodos de Alocação que se sobrepõem ao mês, além dos Vínculos já entregues. |
| HIS-003 | Implementado | Transferência interna altera Alocação, preservando as datas do Vínculo e evitando falso desligamento/recontratação. |
| PER-012 | **Parcial** | Execução exige Gestor Local da origem e referência de autorização; concessão administrativa granular será implementada na Etapa 10. |

## 3. Requisitos temporais e de auditoria suportados

A Etapa 07 também amplia a evidência técnica dos requisitos transversais:

| Grupo | Evidência acrescentada |
|---|---|
| TMP | Faixas inclusivas, programação futura, fatos retroativos, retificação, cancelamento e separação entre tempo efetivo e conhecimento. |
| AUD | Eventos imutáveis com anterior/novo, responsável, datas, justificativa, evidência, autorização e cadeia SHA-256. |
| CMP | Opções de Alocação derivadas da composição temporal, sem produto cartesiano e com efetivo positivo. |
| CAT | Catálogo hoje inativo pode permanecer em fato histórico; nova Alocação atual/futura exige catálogo ativo. |
| HIS | Fotografias de nomes e identificadores impedem que alterações cadastrais reescrevam silenciosamente o passado. |

## 4. Pendências mantidas de forma explícita

Não são declarados como concluídos nesta etapa:

- ALO-006 no consumidor de frequência;
- VIN-014 em frequência, relatórios e pendências;
- PER-012 quanto à concessão administrativa granular;
- VIN-008 quanto à pendência automática de encerramento;
- documentos, anexos, SEI e classificação restrita da Etapa 08;
- frequência temporal e janela de sete dias da Etapa 09;
- minimização completa por perfil da Etapa 10;
- relatórios oficiais, indicadores e Central de Pendências da Etapa 11.

## 5. Conclusão

O núcleo funcional da Etapa 07 está implementado sem antecipar a conclusão global do backend. A linha temporal de Alocação já é canônica para Vínculos novos e para os fluxos integrados, enquanto os consumidores de frequência e relatório permanecem rastreados até suas etapas próprias.

