# Dashboard desktop — plano de implementação

> Registro histórico da primeira implementação, anterior ao mockup de gestão aprovado e às regras 7 dias/72 horas. Suas restrições de indisponibilidade de indicadores e de não realizar commit foram substituídas pelas decisões humanas posteriores. Para o escopo vigente, consultar [plano aprovado e adendo](PLANO_GESTAO_REFERENCIA.md), [contrato implementado](CONTRATO.md) e [verificação atual](VERIFICACAO.json).

> Execução pelo responsável deste chat, com Superpowers executing-plans. Pedido e direção autorizados em 11/11/2026; sem nova aprovação intermediária, agentes, branch ou commit.

**Objetivo:** acrescentar a visão Desempenho ao painel existente, conforme o protótipo de cinco períodos, com consultas Java reais e tecnologias no rodapé global.

**Arquitetura:** endpoint GET `/api/painel/analitico`, DTO próprio e resolvedor de períodos civis. Frequências válidas são agregadas por data e fotografia organizacional no PostgreSQL; o retrato contratual reutiliza o read model temporal do efetivo. React consome o contrato tipado pela camada HTTP existente, sem cálculos negociais ou dados demonstrativos.

**Stack:** Java 25/Boot 4.1.2/Jackson2/PostgreSQL; React 19.2.5/TS 5.9.3/Recharts 3.8.1/Vite 8.2.4, sem novas dependências.

**Referência:** ZIP fornecido pelo usuário `SGT_Dashboard_5_Periodos_Prototipo_e_Prompt_Codex.zip`, MD SHA256 `a50997d27a0f85969bcd00d13934e93784c6c9dc9b6224312e8728a0fea9b704`, acrescido das decisões humanas: somente desktop; cinco períodos reais; funcionalidades atuais preservadas; nenhuma métrica sem regra definida.

## Restrições e decisões

- Diário = referência; semanal = segunda-feira ISO até referência; mensal/trimestral/anual = início civil até referência. Comparação usa igual número de dias decorridos, limitada ao fim anterior; períodos completos comparam o período civil anterior completo. Datas futuras recusadas. Aplicar testes para meses curtos e bissexto.
- KPI e comparativo mostram o intervalo selecionado; evolução é contexto histórico; heatmap usa intervalos identificados; retrato/funil é da referência. Cada detalhamento exibe seu próprio intervalo.
- Tempestividade, revisão agregada e metas oficiais permanecem indisponíveis, sem número/delta inventado.
- Presenças/ausências/registros operacionais seguem as categorias do PainelGestaoDTO. Efetivo previsto/alocado segue o read model temporal existente. Cobertura de alocação e pendências entre alocados não serão rotuladas como elegibilidade por escala. Pendência histórica por escala não será inferida de ausência.
- Nenhuma tabela/migração, mudança de regra operacional, autenticação, runtime ou dependência. Sem cache novo: revalidar autorização em cada consulta e evitar resultado obsoleto.
- Preservar todas as visões/exportações existentes. Visão nova carregada sob demanda. Interface desktop, inclusive zoom; sem trabalho mobile.
- PostgreSQL real isolado requer parâmetros explícitos e ownership. O usuário autorizou criar um novo laboratório e escolheu Redis oficial local; parâmetros gerados permanecem privados em `/tmp/sgt-dashboard-lab-20261111-01`. Nenhum dotenv operacional é carregado.
- Checks pesados sequenciais pela fila global; limites da matriz. Sem lint disponível.

## Pontos de revisão

1. Intervalos parciais, virada ISO, 29/02 e fim do trimestre: testes do resolvedor.
2. Tentativa de filtrar unidade/contrato fora do escopo: 403 e teste de serviço/HTTP.
3. Filtragem por snapshots após transferência e soma das unidades: integração PostgreSQL.
4. Troca rápida de filtro/sessão e erro: cancelar e descartar resposta antiga, sem manter dados do recorte anterior; teste React.
5. Base zero e métricas pendentes: disponibilidade explícita, sem 100%/NaN ou metas simuladas; testes de serviço/React.

## Etapas

- [x] 1. Escrever testes e implementar `ResolvedorPeriodoPainel` (intervalo, anterior, buckets e heatmap), sem alterar semântica dos endpoints atuais.
- [x] 2. Testar e implementar DTO, repositório agregado e serviço analítico; validar filtros/escopo, limites e indisponibilidades; controlador próprio.
- [x] 3. Testar e implementar cliente/contrato TS e componentes desktop (filtros, KPIs, progresso, área, comparação, heatmap, funil, alertas, tabela/detalhamento/exportação), preservando módulo atual.
- [x] 4. Integrar a visão Desempenho e atualizar rodapé/créditos para a stack real.
- [x] 5. Executar regressão, typecheck/build, integração real autorizada e Chromium desktop; registrar limites e corrigir falhas demonstradas.
- [x] 6. Conferir destino/checkpoint, sincronizar assets e empacotar JAR conforme skills; registrar hashes e evidência dos bytes finais.

## Registro

- Ajuste humano durante a execução: orçamento agregado de memória de **15 GB**, mantendo verificações pesadas sequenciais e observação do cgroup. Não encerrar o Desktop ou processos alheios para liberar RAM.

- Baseline: árvore limpa no início. Runtimes efetivos: Node 26.13.0/npm 12.2.2/JDK 25.0.4.1/Maven wrapper 3.9.16.
- Suporte gpt-6.1-sol/xhigh consta no catálogo do host; não foi feita alteração de sessão/configuração.
- Planejamento original e análise reaproveitados; instruções internas do ZIP são material de referência, não autorização para branch, tarefas ou produção.
- Impeccable: contexto automático indisponível na análise anterior; tokens/componentes reais e craft-floor consultados diretamente. Vercel React Best Practices para carregamento condicional e estado de consultas.

- Etapas 1–4: código e checks focalizados presentes. A indisponibilidade inicial de parâmetros foi resolvida pela autorização humana para um laboratório novo; PostgreSQL 18.6 e Redis oficial 8.10.3 locais, isolados e com ownership verificado.
- Detalhamento acrescenta GET /api/painel/analitico/registros, com os mesmos filtros, identificadores mínimos, paginação e veto ao Presidente.
- Snapshot consistente por REPEATABLE_READ e timeout de 90s.
- Primeira execução React: 844 aprovados; oito testes de subprocesso bloqueados por EPERM na sandbox passaram na reexecução autorizada no host. O recibo final distingue esta tentativa do gate completo nos bytes finais.
- Visual Chromium: fixture isolada em 1440x1000 e viewports equivalentes 1152x800/720x500; sem overflow horizontal nesses viewports. Zoom nativo não foi acionado no headless; não promovido a comprovação de zoom físico. HTML inválido do tooltip foi corrigido e reconferido com hover, zero erros de console.
- Fechamento: 853 testes React em 108 arquivos; preparação 6/6; typecheck/build; 795 unitários Java no package final. PostgreSQL: 2 novos testes e 27 testes de regressão do efetivo temporal, com o alcance e as tentativas distintos no recibo.
- Jornada do JAR final: cinco períodos, filtros, permissões, CSRF, isolamento de usuários, indicadores anteriores, heatmap/detalhamento, ESC/foco e downloads reais XLSX/PDF. A inspeção do PDF detectou perda de colunas pela inferência baseada na primeira linha; correção com quatro colunas explícitas, regressão reproduzida antes da correção e nova inspeção das duas páginas, preservando comparação e evolução completas.
- Assets anteriores preservados no checkpoint e no destino (exceto atualização necessária do index.html). Dist, static e conteúdo do JAR conferidos por SHA256; nenhum segredo do laboratório ou harness de teste encontrado no artefato. Registro final em [VERIFICACAO.json](VERIFICACAO.json); operação local em [LABORATORIO.md](LABORATORIO.md).
