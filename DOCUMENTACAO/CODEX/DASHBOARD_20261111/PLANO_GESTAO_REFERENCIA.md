# Planejamento — dashboard de gestão com cinco períodos

**Estado atual (decisão humana posterior):** implementação autorizada conforme mockup desktop v3 aprovado. As seções originais abaixo registram a proposta; o adendo de execução e o [contrato implementado](CONTRATO.md) têm precedência. Substitui o plano `REVISAO_COERENCIA.md` e a proposta visual rejeitada. Implementação por um responsável; revisão independente Astra/xhigh e homologação Sol/xhigh em tarefas Desktop separadas expressamente solicitadas. Commit e push autorizados somente após conclusão e aprovação. Teto agregado autorizado: 15 GB, com checks pesados sequenciais. Pausar se a franquia semanal restante chegar a 1%, aguardando redefinição sem resgatar crédito.

## 1. Objetivo e referências

Permitir uma leitura rápida de **situação → desempenho → evolução → problema → ação**, mantendo os indicadores solicitados e a organização compacta da imagem enviada pelo usuário. O painel permanece somente desktop e integrado ao backend real.

Referências recebidas em 11/11/2026:

- Imagem `/tmp/codex-clipboard-132e7e49-29d1-4abe-bec7-9b376173f23d.png`; SHA256 `108f6ca905a8ece80699d38755e03e3a09f204fe53928d6690ebaacf4718a627`.
- Texto `/home/alexcosta/.codex/attachments/79788fe5-4a40-403d-9c61-18bd9d73301b/Texto colado.txt`; SHA256 `29f1b7e737faebb123f5424ca64993b9bc30f25c233bee1c1e1a80a555bc3339`.

O pedido humano atual é **criar planejamento**. As instruções de implementar, executar baterias e entregar código presentes no texto anexado são requisitos para uma etapa futura; não autorizam iniciar essa etapa agora. Números, percentuais, metas e classificações da imagem são demonstrativos e não serão usados como dados reais.

### Correção de direção

- Preservar os quatro KPIs: **Total de registros, Dentro do prazo, Fora do prazo e Em revisão**. Não substituí-los por presenças/ausências. Estas continuam como métricas selecionáveis do gráfico e detalhamentos.
- Preservar as três barras: **Cobertura de frequência, Ocupação do efetivo e Qualidade cadastral**.
- Priorizar **pendências de frequência** no heatmap. Não trocar pendências por faltas/ausências.
- Usar funil com **vinculados → esperados → lançamentos entre esperados**, com pendências separadas. Não substituir empregados esperados por alocados.
- Histórico e fotografia diária podem existir na mesma tela, com contextos e datas explícitos. Não forçar todos os blocos a uma mesma base temporal quando isso mudaria o significado do indicador.

## 2. Composição do frontend

Modo de uso: Operate, para gestor que precisa identificar rapidamente a situação e decidir onde atuar. Identidade SGT, texto legível, superfícies discretas, pouco espaço desperdiçado, separadores leves e números alinhados. A foto orienta **densidade, hierarquia, barras e área**, adaptadas à largura desktop; não é uma tela mobile a ser esticada.

Estrutura proposta, sem alterar menu global ou funcionalidades das demais visões:

```text
DESEMPENHO OPERACIONAL                 referência · atualizado · XLSX/PDF
[Diário | Semanal | Mensal | Trimestral | Anual]    data    comparar
[Contrato................................] [Unidade....................]
Intervalo aplicado: início — fim · parcial/completo · escopo

TOTAL DE REGISTROS | DENTRO DO PRAZO | FORA DO PRAZO | EM REVISÃO
valor + variação    valor/estado      valor/estado    valor/estado

DESEMPENHO E METAS                EVOLUÇÃO — GRÁFICO DE ÁREA
Cobertura de frequência  ━━━━━   [Registros | Presenças | Ausências | Pendências*]
Ocupação do efetivo      ━━━━━   série legível · tooltip · parcial identificado
Qualidade cadastral     ━━━━━   comparação atual × anterior, com datas e bases
referência e meta/origem          contexto histórico identificado

HEATMAP — PENDÊNCIAS              FUNIL DE COBERTURA · DATA DA REFERÊNCIA
unidade × intervalo              Efetivo vinculado       ━━━━━━━━━
taxa + quantidade + base          Empregados esperados    ━━━━━━━
legenda estável                   Lançamentos entre eles  ━━━━━
                                 Pendentes               quantidade separada

ATENÇÃO DO GESTOR                 UNIDADES / DIAGNÓSTICO
problema · quantidade · contexto  resumo compacto · ordenar · detalhar
ação autorizada                  informações adicionais sob demanda

Fontes e regras                                      Tecnologias no footer
```

### Regras de apresentação

1. Quatro KPIs em uma linha no desktop; sem parágrafos longos dentro dos cartões. Fonte, fórmula, período e limitações ficam em ajuda acessível e no detalhe. O cartão indisponível conserva título e posição, com **“Aguardando definição da regra”**; nunca zero ou percentual fictício.
2. Três barras compactas, cada uma com percentual real, base e meta. Se não houver meta oficial: **“Sem meta cadastrada”**. Não transformar limiares de alerta existentes ou percentuais da imagem em metas.
3. Um gráfico principal de área, como na referência. Comparativo integrado ao mesmo bloco, evitando um segundo gráfico repetindo a mesma leitura.
4. Heatmap e funil alinhados na segunda faixa de análise; ambos com contexto temporal visível. Tabela detalhada e metodologia por expansão ou detalhamento, para não disputar atenção com o resumo.
5. Alertas focados em ação: vencimento contratual, déficit, frequência pendente, inconsistência cadastral e revisão fundamentada. Cada linha identifica problema, data/intervalo, quantidade e ação disponível.
6. Datas e filtros aplicados sempre visíveis junto dos resultados. Carregamento não mistura filtros novos com números antigos. Limpar contrato/unidade preserva a escolha temporal.
7. Cores têm função estável; crescimento de volume não será automaticamente verde e ausência não será sinônimo de pendência. Heatmap de taxa não usa escala relativa ao maior valor da página, que mudaria o significado da mesma cor entre unidades.
8. Desktop de referência: 1366×768, 1440×900 e 1920×1080. Resumo e indicadores principais devem exigir pouca rolagem; diagnóstico fica abaixo. Conferir zoom real, foco visível, tooltip por teclado, tabela acessível e ausência de sobreposição. Não criar escopo mobile.
9. O próximo artefato visual será um mockup completo desta composição, com todos os indicadores, para avaliação **antes do código de produção**. A decisão de cor/tema será mostrada nesse mockup dentro da identidade do SGT; os números continuarão identificados como demonstração.

## 3. Matriz dos indicadores

| Indicador | Fonte e regra a preservar/adaptar | Base temporal | Tratamento no planejamento |
|---|---|---|---|
| Total de registros | Frequências operacionais válidas, categorias do painel gerencial, por data de referência e snapshots | Período selecionado | Já existe agregado; conferir universo, deduplicação e reconciliação |
| Dentro do prazo | Evento de lançamento e timestamp confiável, segundo SLA aprovado; `Frequencia.registradoEm` e `EventoFrequencia` são candidatos técnicos, não regra pronta | Período selecionado | Cartão mantido; classificação só após definição formal; quantidade/percentual e denominador explícitos quando disponíveis |
| Fora do prazo | Mesma política de tempestividade, exceções e base do item anterior | Período selecionado | Cartão mantido; não inferir de ATRASO, FALTA, pendência ou janela de edição |
| Em revisão | Estado negocial de revisão e precedência/deduplicação aprovados | Período selecionado e estado definido | Cartão mantido; não contar toda alteração/auditoria como revisão pendente |
| Cobertura de frequência | Lançamentos válidos entre esperados ÷ empregados esperados × 100; jornada, calendário e folgas aplicáveis | Fotografia da referência | Adaptar fonte de escala existente aos filtros/temporalidade; não usar alocados como denominador |
| Ocupação do efetivo | Vinculado ÷ previsto × 100, preservando a definição canônica de cada base | Fotografia da referência | Conferir composição/vínculos vigentes, contrato/unidade e unidade de contagem; não trocar vinculado/alocado silenciosamente |
| Qualidade cadastral | `comDataAdmissao ÷ (comDataAdmissao + semDataAdmissao) × 100`, conforme fonte atual | Base cadastral declarada | Identificar como completude da data de admissão; aplicar universo autorizado e filtros; declarar se cadastro consultado atual ou reconstrução histórica |
| Pendência do dia | Esperados menos lançamentos entre os mesmos esperados | Fotografia da referência | Distinguir lançamento faltante de falta do trabalhador; zero esperados → sem base |
| Heatmap de pendências | Por unidade/faixa, soma de pendências diárias ÷ soma dos esperados diários × 100 | Intervalo próprio da célula | Desenvolver agregado temporal confiável; contagem é empregado-dia, não pessoas únicas no mês; não fazer média simples das taxas |
| Funil | Vinculados, esperados e lançamentos entre esperados, com bases compatíveis; pendentes em separado | Fotografia da referência | Reutilizar semântica de `FunilFrequencia`, não o funil de alocação da primeira entrega |
| Alertas | Fontes canônicas de contratos, efetivo, cobertura, integridade e revisão quando definida | Data/intervalo indicado em cada alerta | Sem limiar ou rota inventados; totais do mesmo escopo autorizado |

**Indisponibilidade não elimina indicador da interface.** Distinguir `disponível`, `sem base`, `aguardando regra`, `histórico não reconstruível` e `erro de consulta`. Numerador, denominador, fonte, unidade de medida, recorte, geração e versão da regra acompanham o DTO. Comparativos percentuais usam pontos percentuais; percentual de variação exige base anterior não zero.

### Evidência já inspecionada

- `PainelGestaoDTO.CoberturaFrequencia` documenta a base de empregados esperados. O campo legado `registrosEsperados` contém **lançamentos entre esperados**, apesar do nome; preservar compatibilidade/alias e nomear com clareza no contrato analítico.
- `PainelRepositorio.obterCoberturaFrequencia` já calcula esperados, lançados e pendentes, mas sua assinatura não recebe contrato/unidade analíticos; não reutilizar resultado organizacional amplo em um recorte filtrado.
- O legado retorna 100% sem esperados. A nova apresentação usa disponibilidade/denominador explícitos e mostra **“Sem base”**, sem alterar contratos legados por acidente.
- `obterAnaliseCadastro` mede preenchimento de admissão em dados persistidos e usa gestão atual do empregado. Não comprova sozinho o cadastro tal como existia no passado nem filtros históricos de contrato/unidade.
- O serviço gerencial atual aceita apenas 7/15/30/60/90 dias. Manter endpoint analítico civil próprio; não enviar 365 ao endpoint legado.
- `Frequencia` e `EventoFrequencia` têm `registradoEm`, versões e eventos; isso permite investigar tempestividade, mas não define SLA nem um estado “em revisão”.

## 4. Contrato temporal e leitura do gráfico

Os filtros definem o período principal do resumo/comparativo. Data civil no fuso de negócio, inclusiva e sem futuro:

| Seleção | Período principal |
|---|---|
| Diário | A referência |
| Semanal | Segunda-feira ISO até a referência |
| Mensal | Dia 1 do mês até a referência |
| Trimestral | Início do trimestre civil até a referência |
| Anual | Dia 1 do ano até a referência |

A tela identifica três bases, sem escondê-las num título genérico:

- **Período analisado:** KPIs e comparação principal.
- **Na referência:** barras gerenciais/funil e respectivos alertas, salvo particularidade cadastral explicitada.
- **Contexto histórico:** área de evolução e, no diário, últimos sete dias do heatmap, como previsto no anexo. O contexto não entra no total do KPI diário. Células e pontos têm seus próprios intervalos no tooltip/detalhe/exportação.

Para a evolução, usar granularidade coerente com a seleção e janela curta por padrão, como os pontos mensais da imagem. Marcar o último período parcial e informar o corte. Não apresentar queda de um período parcial como deterioração frente a um completo. Séries de comparação só entram se autorizadas, reconstruíveis e equivalentes; no modo comparável, aplicar o corte equivalente a todos os períodos exibidos. O histórico integral pode ser consultado sem atribuir julgamento de tendência a durações diferentes.

Heatmap: diário → sete dias de contexto identificado; semanal → dias da semana ISO já transcorridos; mensal → intervalos/semanas com datas explícitas; trimestral/anual → meses. Não incluir futuro. Mapa e tabela de unidades devem indicar quando usam recortes distintos (o contexto diário é uma exceção identificada, não o período principal).

Comparativo: atual e anterior com início/fim, número de dias e situação parcial/completa; bases equivalentes antes de publicar delta. Meses curtos, fim de mês/trimestre/ano e bissexto devem ter casos de aceite explícitos. A política existente de calendários completos versus dias decorridos deve ser reconciliada com a especificação recebida antes de alterar o resolvedor; nenhuma troca silenciosa de regra será feita.

## 5. Planejamento do Java

1. Inventariar e fechar a matriz acima sobre os métodos reais, com filtros/escopo, história e unidade de contagem. Separar cálculo de período, fotografia, contexto e classificação.
2. Evoluir `PainelAnaliticoDTO`, `PainelAnaliticoServico`, `PainelAnaliticoRepositorio` e `ResolvedorPeriodoPainel`; reaproveitar os serviços/read models de frequência e efetivo que sustentam os indicadores legados. Métodos auxiliares pequenos e responsabilidade explícita.
3. Criar projeções agregadas para cobertura/pendências por data e unidade, com elegibilidade de escala/calendário/folga e vínculos/alocações históricos. O grupo de lançamentos deve ser o mesmo grupo dos esperados. Não aplicar cadastro atual retroativamente como se fosse snapshot.
4. Acrescentar DTOs de disponibilidade, numerador/denominador, regra/fonte, metas/origem, funil e alertas. A ausência de metadados oficiais de metas não exige criar cadastro de metas neste escopo.
5. Preservar endpoints legados, Jackson2, autenticação, autorização no servidor, snapshots e minimização de dados no drill-down. Listagens fora do escopo recebem o erro correto, não zero/lista vazia enganosa.
6. Não carregar anos de entidades/pessoas para agregar em memória nem executar uma consulta completa por empregado/dia. Consultas limitadas por intervalo, projeções, páginas estáveis e medição no PostgreSQL real. Sem nova tabela por conveniência; schema somente JPA se uma necessidade posterior for justificada e autorizada.
7. Redis analítico permanece desabilitado inicialmente. Se cache se tornar necessário por evidência de desempenho, chave completa de escopo/filtros/intervalo/granularidade/versão e invalidação comprovada; nenhuma resposta compartilhada entre usuários.

## 6. Planejamento do React

Reorganizar `PainelDesempenho` em componentes: `FiltrosDesempenho`, `KpiDesempenho`, `MetasProgresso`, `GraficoAreaEvolucao`, `ComparativoDesempenho`, `HeatmapFrequencia`, `FunilCobertura`, `AlertasOperacionais`, `TabelaUnidades` e `DetalhamentoIndicador`. O `index.tsx` compõe a tela; não concentra fórmulas nem um JSX monolítico.

Contrato TS alinhado ao Java; domínio consome `ServicoPainel`/`requestBackEnd` existentes. Um estado de recorte aplicado, cancelamento individual, descarte de resposta antiga, geração de sessão e filtros preservados em 403. Recharts existente, CSS Modules e tokens SGT; sem dependência adicional por aparência.

Cada clique leva à origem correta: registros para KPIs disponíveis; universo/contagens de elegibilidade para cobertura/funil; célula com unidade, datas, pendentes, esperados e taxa; alerta com ação autorizada. Não abrir uma lista genérica de frequência para simular explicação de vínculos ou qualidade cadastral. Presidente permanece restrito aos agregados autorizados.

Exportações incluem filtros, escopo, geração, disponibilidades, regras/fontes, período e comparação, bases e taxas, identificação de fotografia/contexto e paginação. Não misturar total global com página de unidades exportada. Footer mantém as tecnologias reais.

## 7. Pendências que precisam de decisão negocial

Não impedem criar o layout completo ou calcular indicadores já fundamentados. Impedem apresentar números nos KPIs dependentes:

- **Prazo:** fato analisado, primeiro lançamento versus retificação, timestamp, SLA/exceções, deduplicação e tratamento indeterminado. A janela operacional N configurável/padrão 30 não será promovida a SLA.
- **Revisão:** estado de entrada/saída, responsável, relação com validade/auditoria e se pode coexistir com dentro/fora do prazo.
- **Metas:** valores oficiais, abrangência, vigência, direção desejada e origem; até lá “Sem meta cadastrada”.
- **Casos limítrofes de comparação:** fechar exemplos de durações diferentes e política de normalização antes de trocar o comportamento atual.
- **História cadastral/eligibilidade:** registrar quais fatos são reconstruíveis em cada referência e quais são dados atuais consultados. Não preencher lacuna histórica com estimativa.

## 8. Etapas de execução futura e aceite

| Etapa | Resultado concreto para avaliação |
|---|---|
| 1 — Fechar planejamento e desenho | Este plano alinhado; mockup desktop com os quatro KPIs, três barras, área/comparativo, heatmap, funil e ação, incluindo estado indisponível |
| 2 — Baseline e regressões | Novo checkpoint/hash do estado real, preservando alterações locais; testes alinhados ao pedido atual, sem promover aprovações antigas |
| 3 — Java e integração | DTO/serviços/agregados reais com fontes e bases; PostgreSQL/Redis isolados existentes; reconciliação e autorização demonstradas |
| 4 — Frontend | Componentes menores, layout compacto aprovado, todos os blocos presentes, HMR com API real separada, estados e teclado funcionando |
| 5 — Validação integrada | Testes, typecheck/build, fontes/assets/JAR vinculados, SPA/API/arquivos/zoom e jornadas no navegador; recibo desta alteração |

Aceites específicos: cinco períodos reais; calendário ISO/28–31 dias/bissexto; parcial/completo/base zero; vínculo/lotação/turno alterados; folgas/calendário/turno noturno; retificações/duplicidade; autorização por perfil e 400/401/403/409 nos fluxos pertinentes; heatmap ponderado; etapas do funil em bases compatíveis; alertas com origem; exportações reconciliadas; nenhuma perda nas visões anteriores.

Os testes preparados durante o plano rejeitado contêm premissas agora substituídas (por exemplo, retirar KPIs de prazo/revisão ou proibir contexto de sete dias). Antes da implementação, alinhar esses testes ao novo requisito, mantendo testes discriminantes para **KPI de um dia versus contexto de sete dias**, recorte, filtros e ausência de dados artificiais. Não desabilitar testes para obter aprovação.

Checks seguem `.codex/validation.toml` e as skills pessoais de validação/integração. Não há script lint. Maven 512 MiB, fork Java 768 MiB/metaspace 256 MiB, Node 768 MiB, um worker e pool de oito conexões, respeitando 15 GB agregados. Sem fechar Desktop ou processos alheios. Planejamento não requer nova bateria pesada nesta etapa.

Conferência de planejamento: o checkpoint transitório mencionado na revisão anterior não pôde ser conferido integralmente (cópia do hook `useDesempenho.ts` ausente no caminho histórico). Não é prova do estado atual nem autorização para recuperar/sobrescrever arquivos. A etapa 2 exige novo inventário/checkpoint dos bytes presentes e preservação de alterações locais. Nesta etapa foram escritos somente documentos de planejamento.

**Aceite visual essencial:** o gestor identifica rapidamente quanto foi registrado, cobertura, ocupação, qualidade, onde estão as pendências e qual ação executar. Os quatro KPIs solicitados permanecem visíveis mesmo quando uma classificação aguarda regra. O painel da imagem orienta a composição; seus dados fictícios nunca são promovidos a produção.

## Adendo de execução aprovado — 11/11/2026

- Mockup v3 aprovado; diário padrão no Java/React; manter quatro KPIs, três metas, área/comparação, pendências por unidade, funil e diagnóstico. Dados do protótipo são exclusivamente fictícios.
- Lançamento: 7 datas civis incluindo a data do plantão; plantão 10/11 encerra em 17/11 às 00h (exclusivo), America/Sao_Paulo. Edição: 72 horas desde o primeiro lançamento, sem renovação por retificação. Parâmetros independentes no `.env` Java, positivos. Preservar fechamentos já efetivados e exceções gerenciais com motivo, escopo e trilha.
- Tempestividade do primeiro lançamento: preservar prazo/classificação em fotografia imutável nos novos registros. Acervo sem regra capturada: indeterminado, informado separadamente; não inferir SLA histórico da configuração atual. ATRASO continua sendo chegada do trabalhador.
- Em revisão: Operacional solicita; Gestor Local decide e executa eventual correção pelo fluxo gerencial existente. Estado independente de validade/auditoria histórica e tempestividade, deduplicado por frequência. Motivos e decisão auditados.
- Metas propostas para avaliação da chefia: cobertura 100% após prazo; ocupação 100% do previsto, com déficit/excesso; qualidade 100% de preenchimento da admissão, sem alegar completude global. Metas identificadas como propostas.
- Cobertura/funil/heatmap: trabalhadores esperados pela jornada/calendário/folga e vínculos/alocações na referência; distinguir pendências de ausências. Bases ausentes: sem base, nunca estimativa.
- Footer com SVG locais fornecidos e atribuição Devicon MIT; preservar SGE.
- Auditoria e revisão independente em tarefa Desktop Astra/xhigh, só leitura, após alteração concreta. Homologação integrada em tarefa Desktop Sol/xhigh. Fonte/assets/JAR e checks vinculados por hashes; evidências novas não substituídas por recibos históricos.
- Teto agregado 15 GB; heavy-run, suítes sequenciais. Pausar com franquia semanal restante <=1%, preservar checkpoint e aguardar redefinição; sem consumir crédito de reset automaticamente.
- Commit e push autorizados somente após conclusão e pareceres aprovados. Não criar branch/worktree nem acessar produção.

Ordem: checkpoint → regras Java e regressões → agregados reais → TSX/CSS/footer → testes e laboratório PostgreSQL/Redis oficial → revisão e correções → homologação e auditoria final → commit/push.
