# Frequência — lançamento e edição configuráveis

**Decisão humana de 11/11/2026:** proposta aprovada para implementação e avaliação pela chefia. Substitui o padrão anterior de 30 datas civis compartilhado entre lançamento e edição. Recibos anteriores não homologam esta mudança.

## Contagem e configuração

- Lançar: `FREQUENCIA_JANELA_OPERACIONAL_DIAS=7`, propriedade `frequencia.janela-operacional-dias`. D é a data de início do plantão e conta como dia 1. Plantão de 10/11 pode ser lançado até o instante anterior a 17/11 00:00, em `America/Sao_Paulo`. Não são dias úteis.
- Editar: `FREQUENCIA_EDICAO_HORAS=72`, propriedade `frequencia.edicao-horas`. São 72 horas decorridas desde o primeiro lançamento: 12/11 14h → limite exclusivo 15/11 14h. Editar não renova esse prazo e a edição pode ultrapassar o prazo para primeiro lançamento.
- Valores positivos, alteráveis no `.env` do Java; reiniciar a API para aplicar configuração. Novos registros capturam ambos os prazos em campos imutáveis. Acervo sem captura não recebe classificação histórica dentro/fora; edição de registros ainda abertos usa primeiro timestamp + configuração de edição, preservando FECHADA já persistida. Ampliar configuração não reabre registros fechados.
- Primeiro lançamento, edições e fechamento automático compartilham a mesma política no backend. O React exibe `prazoEdicao` enviado pelo Java. O job persiste FECHADA após vencer a edição; a proteção já vigora na leitura/escrita antes do job.
- Turno noturno pertence à data de início. Futuro continua vedado para todos. Histórico classificado usa o primeiro timestamp e prazo capturado, sem reclassificação por retificação posterior ou mudanças do `.env`.

## Autorização e eixos preservados

- A janela é uma condição adicional a contrato/vínculo/alocação válidos na data e autorização atual sobre o escopo histórico. A projeção cadastral atual não substitui a história.
- Frequência futura é recusada inclusive para Gestor Local; ampliar N não permite antecipar presença.
- Gestor Local pode corrigir dentro/fora da janela mediante requisitos vigentes de escopo, justificativa, idempotência, versão e auditoria; a retificação tardia não tem prazo máximo rígido.
- Intervenção gerencial durante a janela bloqueia o Operacional somente no registro atingido, preserva autoria original e não antecipa o fechamento dos demais registros.
- Administrador técnico e demais perfis não recebem escrita negocial por hierarquia. Campos legados com nome de desbloqueio não constituem autorização.
- `estadoNegocial`, `validadeOperacional` e `estadoAuditoria` continuam independentes. Perda retroativa de cobertura preserva a prova e retira o fato da visão operacional válida conforme o fluxo autorizado.
- O limite é aplicado na leitura/escrita independentemente de o scheduler já ter persistido fechamento/evento. Não existe tolerância adicional pelo intervalo do job.

## Catálogos anteriores (redação histórica substituída pela decisão acima)

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
