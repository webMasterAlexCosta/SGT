# Contratos preservados na versão operacional

A origem é a entrega CANDIDATO_02 de 01/11/2026. A tabela abaixo descreve a evolução
histórica já existente nessa baseline e preservada, sem nova regra, na retirada financeira.
A especificação operacional mantida está em `CONTRATO_PROVA_CAMPO.md`.

## Leituras históricas

| Superfície | Antes | Nesta derivação |
|---|---|---|
| Posição contratual em D/K | Escalares e fotografia; campos nulos ou valores residuais não explicavam a prova individual | Escalares preservados e `provaCampos` no resultado e na versão de leitura |
| Acervo de versões | Fotografia persistida sem estado individual de evidência | Wrapper de leitura com prova por campo; fotografia derivada interpretada no D/K da própria materialização |
| Composições | Consulta por data; escalares/flags parciais | Corte `conhecidoAte` opcional e metadata por campo, município e grade histórica |
| Provas incorporadas | Cadeia de revisão e fotografia acumulada | Máscara própria explícita, fragmentos declarados e proveniência individual da fotografia acumulada |
| Valor preenchido | Consumidor podia tratá-lo como evidência por si só | O consumidor consulta o estado de prova antes de apresentar o escalar como fato |
| Zero e falso | Podiam ser confundidos com vazio ou desconhecido | `VALOR_COMPROVADO` conserva zero e falso quando a fonte os sustenta |
| Nulo, vazio ou omissão | Ambiguidade entre ausência e lacuna documental | Ausência só é certificada por fonte/máscara/ato que a declare; de outro modo permanece `NAO_COMPROVADO` |
| Informação minimizada | Poderia aparentar ausência factual | `visibilidade=RESTRITA`, estado factual nulo, sem proveniência restrita |
| Fotografia sem vínculo de origem | Ordinal/preenchimento podia receber prova indevida | Escalares são conservados, mas o ordinal não certifica a origem; eventos explícitos ainda podem provar seus próprios campos |
| Projeção | Risco de aparentar registro físico | ID físico continua nulo quando não existe linha correspondente |

Os estados factuais são `VALOR_COMPROVADO`, `AUSENCIA_COMPROVADA` e `NAO_COMPROVADO`. Visibilidade é independente. Origens referenciam somente registros reais permitidos: fotografia, prova, evento, composição, item de aditamento ou mapeamento. Não são divulgados arquivos, dados pessoais ou metadados documentais restritos por meio dessa estrutura.

O DTO de fotografia usado na escrita e no hash de intenção foi preservado. Os wrappers novos são contratos de leitura. Prova incorporada posteriormente não aparece em K anterior; nem o cadastro atual nem uma fotografia futura completam uma lacuna antiga. O acervo anterior não é regravado.

Os consumidores React afetados incluem o módulo Histórico, o modal de detalhes de contrato, os cartões de composição e os seletores de aditamento, remanejamento e transferência. A edição de RASCUNHO continua apresentando dados atuais de preparação com essa identificação. Exportadores e relatórios que usam DTOs próprios mantêm esses contratos, sem metadata fabricada pelo cliente.

## Remoção financeira desta entrega

Tarifas e seus comandos de encerramento/substituição, prévias/recibos, folhas de horas
monetárias e faturas foram retirados da implementação Java e React. As rotas exclusivas
não são registradas. O resumo compartilhado mantém somente fornecedores,
processos abertos e riscos vencidos. Fornecedores, processos de admissão/desligamento
e avaliação composta de risco conservam regras e controles existentes.

Não houve mudança da política de frequência: proibição de futuro, D como primeiro dia,
janela até D+6 23:59:59 no fuso America/Sao_Paulo e fechamento no início de D+7, com as
exceções justificadas de Gestor Local já existentes. O vínculo histórico não é substituído
pelo vínculo atual.

## Persistência e limites

Permanecem `spring.jpa.hibernate.ddl-auto=update` e `spring.sql.init.mode=never`.
As tabelas financeiras e os dados/auditorias antigos são preservados; nenhum DDL manual,
purga ou framework de migração foi introduzido. Os resultados efetivos desta derivação
estão no relatório de testes. Relatórios e matrizes herdados não representam nova aprovação.

As dependências autênticas de CON-023, CAT-004, CAT-007, COL-010, HIS-001 e HIS-006
continuam abertas onde faltam fontes reais. A resposta de desconhecimento não recupera
o documento ausente. Nenhuma contagem de testes fecha esses requisitos.
