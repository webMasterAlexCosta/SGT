# Frequência — janela operacional vigente

**Decisão humana de 09/11/2026, aplicada à derivada atual.** Substitui a duração fixa de sete dias nos cortes anteriores; os IDs canônicos permanecem. A investigação anterior foi preservada em [REGRA_SETE_DIAS.md](REGRA_SETE_DIAS.md). Este documento define a regra, sem atribuir aprovação a testes não executados.

## Contagem e configuração

A janela tem **N dias civis configuráveis, padrão de 30**, em `America/Sao_Paulo`. D é a data de início do plantão e conta como primeiro dia. A edição operacional depende de `agora < início de D+N`; no instante exato do limite, a frequência está fechada para a via operacional. Não são dias úteis nem N períodos de24 horas desde a gravação.

Com N=30, frequência de 01/01 encerra a janela em 31/01 às 00:00. Com N=7 explicitamente configurado, encerra em08/01 às 00:00. Testar a alternativa7 não comprova o padrão de 30. Inclusão, alteração operacional ou retificação não mudam D nem reiniciam o prazo. Turno22:00–06:00 pertence ao dia de início.

A propriedade de servidor é `frequencia.janela-operacional-dias`, alimentada por `FREQUENCIA_JANELA_OPERACIONAL_DIAS`, padrão de 30 e valor positivo. `PoliticaTemporalFrequenciaServico` centraliza o cálculo e o fechamento. Essa configuração não concede permissão individual de desbloqueio retroativo. O frontend não redefine o prazo; usa `prazoEdicao` e estados efetivos da API.

## Autorização e eixos preservados

- A janela é uma condição adicional a contrato/vínculo/alocação válidos na data e autorização atual sobre o escopo histórico. A projeção cadastral atual não substitui a história.
- Frequência futura é recusada inclusive para Gestor Local; ampliar N não permite antecipar presença.
- Gestor Local pode corrigir dentro/fora da janela mediante requisitos vigentes de escopo, justificativa, idempotência, versão e auditoria; a retificação tardia não tem prazo máximo rígido.
- Intervenção gerencial durante a janela bloqueia o Operacional somente no registro atingido, preserva autoria original e não antecipa o fechamento dos demais registros.
- Administrador técnico e demais perfis não recebem escrita negocial por hierarquia. Campos legados com nome de desbloqueio não constituem autorização.
- `estadoNegocial`, `validadeOperacional` e `estadoAuditoria` continuam independentes. Perda retroativa de cobertura preserva a prova e retira o fato da visão operacional válida conforme o fluxo autorizado.
- O limite é aplicado na leitura/escrita independentemente de o scheduler já ter persistido fechamento/evento. Não existe tolerância adicional pelo intervalo do job.

## Reconciliação dos catálogos

| Identificador preservado | Redação temporal aplicável agora |
|---|---|
| `CANONICAL198::FRE-002` | Janela N configurável, padrão de 30 dias civis, D dia1, fuso São Paulo. |
| `CANONICAL198::FRE-003` | Exemplo padrão:01/01 até instante anterior a31/01 00:00; limite exclusivo. |
| `CANONICAL198::FRE-006` | Correção gerencial dentro da janela configurada exige identificação/justificativa; preserva bloqueio por registro. |
| `CANONICAL198::FRE-013` | No início de D+N há fechamento negocial; mudança posterior é retificação. |
| `MASTER::8.5::TMP-001/002/003` | Antes/no/depois do limite D+N, mantendo todas as demais condições dos oráculos. |
| `MASTER::8.5::TMP-004`, `MASTER::ANEXO_A::ROP-034` | Job verifica vencidas pela mesma política N; atraso do processamento não amplia permissão. |
| `MASTER::6::M11/M12/M13`, `MASTER::8.7::E2E-10` | Jornada noturna, janela e fechamento conforme N efetivo/padrão de 30. |

As matrizes C03/C04, CSVs e mestre de03/11 mantêm os textos/resultados do corte histórico. Este suplemento determina a substituição temporal na campanha atual, sem renomear IDs, promover PASS antigo ou converter GOV-003/004 em Governança de Terceiros.

## Evidência necessária

Validar padrão de 30 e alternativa, antes/no/depois do limite, conversão por instante de outro fuso, data futura, autorização histórica/atual, autoria, retificação/bloqueio por registro, lote/rollback e job. Unitários/MockMvc não substituem PostgreSQL/Tomcat/JAR/navegador/LAN. Atravessamento físico de meia-noite precisa de prova própria, separada de relógio controlado. Resultados e limitações ficam no [plano da campanha](../../DOCUMENTACAO/CODEX/CAMPANHA_20261109/PLANO.md).
