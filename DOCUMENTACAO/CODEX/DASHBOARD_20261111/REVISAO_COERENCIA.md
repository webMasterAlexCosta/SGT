# Correção integral da coerência do dashboard

> **Plano não aprovado pelo usuário. Substituído pelo [planejamento baseado na imagem e no texto reenviados](PLANO_GESTAO_REFERENCIA.md).** As propostas de retirar KPIs do destaque e proibir todo contexto temporal fora do período principal não são o requisito vigente.

Solicitação humana posterior à primeira entrega: organizar por completo o componente, corrigir contradições dos períodos e dos indicadores, incluindo o Java. Sem commit ou push. Responsável único neste chat; teto agregado de 15 GB e validações pesadas sequenciais. O plano anterior é reutilizado com esta correção material.

## Diagnóstico e direção

O KPI usava o intervalo civil até a referência; o gráfico mostrava histórico de vários períodos e o mapa diário incluía seis dias anteriores. A tabela diária podia listar unidades sem movimento no dia. Estatísticas de efetivo na referência eram repetidas entre totais do período. Os três indicadores disponíveis ficavam subordinados a cartões de indicadores indisponíveis. A composição visual repetia informações e exigia interpretar janelas diferentes.

Apenas mudar rótulos manteria o esforço de interpretação. Remover o histórico e o retrato eliminaria capacidades existentes. A direção proposta mantém essas consultas em áreas próprias e faz o resumo, a evolução principal, a tabela e o mapa obedecerem ao mesmo intervalo. Não altera a classificação de presenças/ausências nem inventa metas ou prazo. Calendário civil, corte inclusivo e comparação anterior existentes são preservados, com datas explícitas.

**Situação:** planejamento apresentado para avaliação do usuário, antes de alterar código de produção. Os testes de reprodução preparados estão intencionalmente vermelhos; não são uma validação aprovada. Checkpoint do fonte, static, JAR e documentação da entrega anterior: `/tmp/sgt-dashboard-correcao-20261111`.

## Contrato temporal proposto

Data de corte inclusiva, sem futuro. Não são janelas móveis de 7/30/90/365 dias.

| Visão | Início | Fim | Evolução principal |
|---|---|---|---|
| Diário | Data escolhida | Data escolhida | Sem curva artificial de vários dias |
| Semanal | Segunda-feira da semana ISO | Data escolhida | Dias dessa semana |
| Mensal | Dia 1 do mês | Data escolhida | Dias desse mês |
| Trimestral | Primeiro dia do trimestre civil | Data escolhida | Meses do trimestre, último mês cortado na referência |
| Anual | Dia 1 do ano | Data escolhida | Meses do ano, último mês cortado na referência |

Exemplo com corte em **10/11/2026**: diário 10/11; semanal 09–10/11; mensal 01–10/11; trimestral 01/10–10/11; anual 01/01–10/11. O intervalo efetivamente aplicado aparece junto aos resultados. O comparativo anterior mantém a regra existente (mesmo número de dias decorridos no parcial, limitado ao fim anterior; calendários completos quando encerrados), sempre com suas datas e sem prometer durações idênticas entre meses/anos diferentes.

Resumo, evolução principal, tabela e mapa devem reconciliar: mesma métrica, contrato, unidade e período. A soma das partes da série é igual ao total do resumo; a paginação de unidades não altera os totais globais. No diário, mapa e unidades não podem trazer dias anteriores.

## Contrato dos indicadores

- **Registros operacionais:** frequências válidas das sete categorias já usadas pelo painel. **Presenças:** PRESENCA/PRESENCA_PARCIAL/ATRASO/TR. **Ausências:** FALTA/ATESTADO/SUSPENSAO. Presenças + ausências reconciliam com registros. Sem reclassificar ATRASO como atraso de lançamento ou inventar tempestividade.
- **Efetivo:** previsto, alocado, lançamentos e pendências são da data de corte e da fonte temporal canônica. Não somar esses estoques em vários dias nem compará-los diretamente aos registros acumulados do mês. Unidades de medida e denominadores explícitos; zero de base é “sem base”. Conferir o universo de pessoas/alocações versus registros antes de apresentar qualquer razão.
- **Comparação:** valor atual, anterior, diferença e percentual com base; zero anterior não produz percentual fictício. Variação de volume não recebe julgamento automático de melhora/piora.
- **Metas, dentro/fora do prazo, revisão, pendências históricas por escala e qualidade no recorte:** continuar indisponíveis enquanto faltar regra/agregado validado. Permanecem documentados, fora do resumo principal.
- **Sem dados e erro:** zero é resultado de consulta bem-sucedida; erro ou acesso negado não se transformam em zero. Filtros e sessão preservados conforme o fluxo de autenticação atual.

## Organização

- Um cabeçalho, um conjunto de filtros e um intervalo aplicado. A data de corte é compartilhada com a página pai; limpar contrato/unidade preserva o período escolhido.
- Registros, presenças e ausências como resumo principal, com base anterior e diferença quando solicitadas. Todos os detalhes preservam data/escopo/filtros.
- Evolução principal somente dentro do intervalo: dias no semanal/mensal, meses no trimestral/anual. Diário não recebe uma curva artificial. Java fornece os agregados, sem somar registros no navegador.
- Unidades do período em tabela ou mapa, com paginação única. Mapa diário usa somente a referência. Unidades com apenas categorias não operacionais não compõem essa listagem.
- Histórico de períodos, situação do efetivo na referência e fontes/indisponibilidades em áreas identificadas e recolhíveis. As taxas diárias exibem seus denominadores, sem metas fictícias ou interpretação de funil.
- Hierarquia e espaçamento contidos, tipografia legível, controles nomeados e foco/estados preservados. Identidade SGT mantida, desktop apenas.
- Exportações distinguem resultados do período, retrato diário e histórico contextual.

## Execução e aceite

- [x] Diagnóstico e testes de reprodução: mapa diário retornou 7 datas em vez de 1; integração PostgreSQL confirmou unidade fora do recorte operacional e ausência de série restrita ao período; UI confirmou ausência dos cartões de presenças/ausências e limpeza incompatível com o período escolhido. Produção não alterada.
- [ ] Java: `ResolvedorPeriodoPainel` fornece faixas da série principal dentro do intervalo e corrige o mapa diário. `PainelAnaliticoDTO`/`PainelAnaliticoServico` fornecem série do período explicitamente separada da série histórica, conservando o contrato histórico. `PainelAnaliticoRepositorio` pagina/lista unidades operacionais dentro do período; totais e filtros autorizados usam o mesmo universo.
- [ ] React: contrato TS alinhado ao DTO. Decompor `PainelDesempenho/index.tsx` em filtros, resumo, gráficos, unidades e retrato; `useDesempenho` mantém cancelamento e limpeza previsível; data compartilhada com a página pai. Exportações identificam as três bases temporais e incluem a série principal.
- [ ] HMR com API real separada, conferir visual e os cinco períodos, erros/estado vazio, recorte, detalhamento e exportações.
- [ ] Regressão completa, PostgreSQL isolado, typecheck/build, checkpoint/sync/package e jornada no JAR final. Recibo próprio desta revisão, sem promover os hashes anteriores.

Aceite também cobre virada de semana/ano, fevereiro e bissexto, período completo/parcial, comparação desligada/base zero, filtros rápidos/respostas antigas, troca de sessão, 403, páginas vazias e origem do detalhamento. Testes leves/focais durante a correção; suíte completa e build no checkpoint, sem build por edição. PostgreSQL/Redis do laboratório existente, sem novos serviços ou dependências por conveniência.

Revisão própria neste chat; sem tarefas, agentes, revisão independente, branch, commit, push ou implantação.
