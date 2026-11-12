# Auditoria do SGT — 12/11/2026

**Concluída no alcance do repositório e do laboratório isolado.** Foram corrigidos 14 achados de produto, documentação e validação, com regressões e integração dos bytes corrigidos. Nenhum desses achados permanece aberto. Pedido: auditar o SGT e corrigir defeitos encontrados, com teto agregado de 15 GB e consideração da memória disponível. O [plano](PLANO.md) delimita execução e proteções; os limites abaixo permanecem explícitos, sem aprovação de produção ou de toda a matriz histórica.

## Checkpoint anterior

Árvore inicialmente limpa na branch `codex/sgt-progresso-seguro-20261111`. Commit de checkpoint `678758afe271cf531560e612d13bad292cab7536`, sem alteração de arquivos, enviado ao origin antes da auditoria; HEAD e upstream conferidos. As alterações da auditoria são posteriores a esse checkpoint e permanecem locais, sem novo commit/push.

## Achados e tratamento

| ID | Gravidade | Defeito demonstrado | Tratamento e evidência |
| --- | --- | --- | --- |
| AUD-01 | P2 | Preferência local inválida ou armazenamento bloqueado derrubava o aviso de desenvolvimento; falha de gravação impedia fechar. | Leitura tipada/defensiva e persistência opcional; teste inicialmente falhou e passou após a correção. |
| AUD-02 | P2 | Em viewport desktop de 780 × 493, o botão de fechar o aviso ficava fora da área alcançável. O aviso não tinha identificação de diálogo nem contenção de foco. | Limite de altura e rolagem interna, título acessível, Escape, foco visível e movimento reduzido. Clique real confirmado nesse viewport. |
| AUD-03 | P2 | Shift+Tab a partir de título com foco inicial escapava do diálogo. | Ajuste no hook compartilhado; reprodução e contraprova reais no Chromium. Regressão automatizada acrescentada. |
| AUD-04 | P2 | Cache de frequência devolvia ABERTA no instante de vencimento das 72 horas de edição, nas consultas diária, mensal e por intervalo. | Estado derivado depois do cache, sobre cópia do DTO, preservando objeto compartilhado e banco. Três controles vermelhos; nove ITs focais aprovados após a correção. |
| AUD-05 | P3 | Quatro páginas criavam `main` dentro do `main` do layout. | Contêineres locais ajustados; região principal única conferida no navegador. |
| AUD-06 | P2 documental | Índices e mapa de componentes ainda apresentavam 30 dias/janela compartilhada como regra vigente. ITs ainda associavam o prazo de edição a D+7 ou D+30. O estado atual também apresentava a campanha de 09/11 como vigente. | Reconciliação com o contrato de 11/11: lançamento em sete datas civis e edição independente em 72 horas. Histórico preservado; testes de lançamento, edição independente e materialização reconciliados com o contrato atual, com 35 testes de prazos, cache, revisão e queries aprovados na rechecagem. |
| AUD-07 | P3 | Aviso de desenvolvimento garantia que alterações não afetavam produção, mas sua condição era apenas o tipo do build React. | Texto corrigido para a finalidade do ambiente e uso de dados de teste, sem afirmar isolamento que o componente não verifica. |
| AUD-08 | P2 | Falha na leitura ou no fechamento do stream de upload deixava o arquivo parcial atual em `.staging`, embora o negócio fosse revertido. | Temporário registrado para limpeza antes da cópia. Duas contraprovas falharam antes; três ITs de I/O passaram depois, incluindo rollback físico e preservação dos arquivos anteriores. |
| AUD-09 | P2 | Exportação concluída mascarava ausência/truncamento do arquivo com o tamanho antigo; podia confirmar download incompleto ou oferecer repetição impossível. | Confere tamanho real antes de autorizar e de transferir, conta bytes efetivamente transferidos e informa indisponibilidade. Falha de rede preserva repetição com arquivo completo. Sete contraprovas falharam antes; 14 testes focais passaram depois. |
| AUD-10 | P3 | Relatório mensal exibiu nome vazio e rótulo acessível com `null` quando a prova histórica não informava nome. | Indicação explícita de nome não informado nos dois layouts, preservando a ausência legítima da prova. Não usa nome cadastral atual como substituto. |
| AUD-11 | P3 | Cinco links do rodapé ofereciam ações sem destino (`#`). | Conteúdos indisponíveis identificados como texto, preservando a ação de Créditos; conferência real encontrou cinco indicações e zero links vazios. |
| AUD-12 | P2 | Após expiração e novo login, o navegador permanecia em `/login` enquanto o React Router mostrava a página interna anterior. | Navegação da saída delegada ao roteador com substituição do histórico. Contraprova falhou antes; 53 testes de sessão/aplicação passaram depois. Ciclo real sair → entrar voltou a `/painel`. |
| AUD-13 | P2 de validação | Smoke de repositórios usava métrica inválida, tratava ausência esperada de linha como SQL quebrado e tentava invocar injeção de dependência como consulta. | Argumento válido, tratamento específico de `EmptyResultDataAccessException` e exclusão de métodos `@Autowired`. Duas verificações de queries passaram na rechecagem PostgreSQL. |
| AUD-14 | P2 de validação | O observador de lock do ensaio de queda dependia de SQL textual truncável; havia espera física, mas o gate não a identificou. Execução conjunta dos cenários também deixava jobs recuperáveis que interferiam nos seguintes. | Identificação por fila de bloqueadores e tupla/tabela em `pg_locks`; cada cenário recebe schema Hibernate e namespace Redis próprios, sem purgar dados. Os cinco critérios passaram, preservando negócio, atomicidade, autoridade e limite de 120 segundos. Atomicidade: alvo em 79.317 ms e rollback integral; o predicado textual antigo retornaria falso sobre SQL de 1.023 bytes. |

Os textos de ajuda de frequência, emissão administrativa e 2FA também foram ajustados para orientar o usuário sem detalhes internos de implementação. As correções de interface passaram em 24 testes focais e a correção de navegação em 53 controles de sessão/aplicação. O gate final React passou em 111 arquivos/875 testes, mais seis verificações de preparação, typecheck e build. Não existe script de lint no projeto. O pacote Java passou em 808 testes unitários. Ensaios TCP, relatório multipágina, cancelamento com heap de 192 MiB, carga PostgreSQL, recuperação e conferência final no navegador passaram.

## Cobertura da auditoria

- Backend: autenticação, sessão, CSRF e autorização por escopo; contratos, catálogos, pessoas/vínculos/alocações e prova histórica; temporalidade de frequência, concorrência/idempotência, cache; ocorrências/anexos/documentos; relatórios e processamento assíncrono; diagnósticos e configuração operacional.
- Frontend: transporte compartilhado e cancelamento por sessão, contratos/tipos, estados assíncronos, filtros/paginação, diálogos e navegação, estrutura semântica e exportações reais. Inspeção por risco e execução dos gates existentes; não equivale à leitura integral de cada linha.
- Operação: inspeção dos utilitários de backup/restauração e comissionamento, mais testes isolados dos controles negativos. Não houve restauração de banco nem execução em produção.
- Dependências npm: consulta pública de 12/11 retornou zero avisos conhecidos para 258 dependências informadas; versões e lockfile preservados. Isso não prova ausência de vulnerabilidades desconhecidas ou nas demais camadas.
- Dependências Java: 123 coordenadas/versionamentos do JAR conferidos por metadados Maven, correspondência de hash com o cache local e BOM/manifesto do auxiliar Jarmode. Consulta pública OSV de 12/11 sem avisos para essas versões. Não é garantia contra vulnerabilidades desconhecidas; nenhum código ou dado de usuário foi enviado.

## Execuções e evidências

- Baseline Java: 800 testes unitários passaram após recompilação limpa. O primeiro ensaio encontrou bytecode gerado incompatível no `target`; o diretório anterior foi preservado antes da recompilação. Não foi atribuído defeito ao fonte por esse resíduo.
- React: baseline de 109 arquivos/860 testes; gate final de 111 arquivos/875 testes, com seis verificações de preparação, typecheck e build aprovados. O build mantém o aviso de chunk facial maior que 1 MB, carregado por importação dinâmica na página de perfil; não foi tratado como erro de compilação.
- Integração inicial PostgreSQL/Redis: 36 testes em sete classes aprovados. A primeira chamada direta ao Failsafe, sem inicialização do agente de cobertura, executou zero testes e foi corrigida; não é aprovação nem defeito do produto.
- Regressão ampla: 1.544 testes em 366 classes executados. Foram 14 falhas iniciais: dez expectativas antigas de prazo (reconciliadas e rechecadas), uma falha no gerador de argumentos do smoke (corrigida/rechecada) e três verificações de artefato que exigiam o JAR ainda não empacotado naquela rodada. Os três casos passaram após o package e a conferência integral dos bytes. Nenhum caso ignorado nesses lotes. O primeiro ensaio TCP posterior parou porque faltavam os identificadores explícitos da instância Redis; os cenários anteriores ao ponto de falha não foram contados como aceite completo. O comando foi ajustado após confrontar a identidade do Redis com a unidade local proprietária; a repetição TCP passou. O teste multipágina também passou após exigir o nome explícito do banco atual e preservar ownership/role limitada/PostgreSQL 18.6, agora conferidos antes da criação da massa. O guardião histórico por prefixo permanece como padrão quando não há nome explícito.
- Operação: 31 testes isolados aprovados; não substituem uma restauração real.
- Consolidação final de integração: 378 classes, 1.597 testes, zero falhas/erros/ignorados, considerando a última execução de cada classe e os cinco métodos isolados de recuperação. **Não foi uma execução integral única sobre os bytes finais**: a regressão ampla precedeu as últimas correções de upload/exportação; áreas afetadas e casos focais foram rechecados. Os 866 bytecodes de produção e as 156 classes unitárias permanecem idênticos aos do package aprovado; as mudanças posteriores foram nos ITs e na documentação.
- Recuperação no JAR final: 200 registros com chunk 1 em 80.025 ms e chunk 500 em 103.637 ms, ambos abaixo de 120.000 ms; morte durante chunk 500 após alcançar o alvo em 79.317 ms deixou rollback integral; revogação real do criador chegou ao terminal de erro em 44.075 ms, preservando o prefixo e recusando efeitos novos; cancelamento/reprocessamento e disputa entre dois executores retomaram o mesmo job em 66.490 ms, concluindo dois registros do prefixo mais 198 novos, sem duplicação. Cada cenário teve uma JVM/schema/índice Redis próprios. A primeira tentativa do novo observador comparou a representação booleana incorreta do helper (`true` em vez de `1`); isso foi corrigido antes da repetição final dos cinco casos. Logs das tentativas permanecem preservados.
- Carga PostgreSQL: 24 workers por 15 segundos, 140.438 operações, zero erros, p95 de 11 ms, p99 de 23 ms e 28.176 escritas conferidas. É um cenário do banco, sem extrapolação para SLA da aplicação inteira ou escala nacional.
- Navegador Chromium 157.0.8103.50: cinco períodos do dashboard responderam pela API real; filtros de frequência, navegação contratual, prévia administrativa e relatório mensal consultados. No JAR final, saída redirecionou para `/login`, novo login retornou a `/painel` e recarga preservou a sessão. Uma região principal, ausência de overflow horizontal e de links `href="#"` conferidas. A recarga final autenticada apresentou zero erros/avisos de console; o 401 da sessão antiga e o cancelamento esperado na saída não são omitidos do histórico bruto.
- Exportações finais XLSX/PDF de 27 colaboradores concluíram pela API assíncrona real (criação 202, download 200). XLSX íntegro como ZIP/XML; duas páginas do PDF renderizadas e inspecionadas, sem corte ou sobreposição. PDF não marcado/tagged e fontes padrão não incorporadas; não há declaração de PDF/UA ou certificação entre visualizadores. O aviso local de cache do Fontconfig não impediu a renderização nem motivou alteração global do host.
- Sonar local consultado em leitura: nenhum projeto SGT disponível ao usuário da conexão. Não há gate nem contagem de achados Sonar atuais para declarar como aprovados.

Os logs brutos e os dados sintéticos ficam em `.homologacao/auditoria-20261112/`, ignorado pelo Git. Nenhuma credencial, cookie ou dotenv operacional integra o relatório versionável. Os [recibos sanitizados](EVIDENCIAS.json) vinculam fonte, classes, assets e JAR.

### Artefato final e execução real

- JAR SHA-256: `963677cb995456c40b3f61a2865d8dfe1b8be93e8f8be9b2ae11861b4a6789d4`.
- Manifesto de fontes SHA-256: `f83c65f437c081ad5be2c8399207d34d312de8443abfce85c29bb2e036619251` (`fontes-candidato-03.json` no diretório local da auditoria).
- 866 classes e 111 arquivos do dist conferidos byte a byte no JAR; 595 assets anteriores preservados. Backup do static e target anterior mantidos localmente.
- API do laboratório reiniciada com a cópia imutável desse JAR, na porta já usada, 58501. `/`, `/painel`, `/contratos`, `/relatorio-mensal` e `/historico` retornaram o índice correto; todos os 111 assets servidos por HTTP têm o hash do dist. API anônima de contratos respondeu 401 e bootstrap CSRF respondeu 200.
- A API anterior teve comando/artefato preservados. PostgreSQL, Redis e Vite existentes foram mantidos. Não restou suíte pesada em execução no fechamento.

## Recursos e limites

PostgreSQL e Redis isolados já existentes, com ownership da role limitada confirmado. Base de IT separada da base da interface. Cgroups dos serviços existentes somam 4 GiB de teto. Suítes comuns usam 2 GiB; o lote de JARs usa 3 GiB para incluir até duas JVMs filhas de 192 MiB, mantendo no máximo 7 GiB de limites agregados (7.516.192.768 bytes, abaixo de 15 GB) e swap zero. O maior pico registrado para uma suíte foi 2.250.080.256 bytes, cerca de 2,10 GiB. Suítes pesadas sequenciais pela fila global; reserva de entrada de 8 GiB disponíveis, reavaliada antes de cada lote. Maven 512 MiB, fork Java 768 MiB/metaspace 256 MiB, Node 768 MiB, um fork/worker e pool de oito conexões.

A franquia semanal foi acompanhada pela ferramenta nativa do host. Última leitura no fechamento: 92% usados, 8% restantes. O limiar humano de pausa com 1% restante não foi atingido; nenhum crédito de redefinição foi usado e não foi necessário agendar retomada.

Nenhum resultado histórico homologa fonte alterada. A auditoria não autoriza produção, implantação, migração de acervo, autenticidade institucional, carga nacional, Windows nativo, LAN externa nem todos os critérios históricos por associação de classe. Não houve revisão independente: o pedido não autorizou criar outra tarefa de revisão. Os ensaios históricos presos a JAR/hash e preparadores de fixtures excluídos são discriminados abaixo.

### Preparação dos ensaios adversariais

No laboratório, `track_activity_query_size=1kB` foi confirmado em leitura. O texto de `pg_stat_activity.query` é limitado por essa configuração; a cadeia de bloqueio deve ser conferida por `pg_blocking_pids`, e não inferida apenas de um trecho de SQL ([estatísticas PostgreSQL 18](https://www.postgresql.org/docs/18/monitoring-stats.html), [locks PostgreSQL 18](https://www.postgresql.org/docs/18/view-pg-locks.html)). A rechecagem registra também se o predicado textual anterior reconheceria o mesmo bloqueio.

Schemas de teste são gerados exclusivamente pelo Hibernate com `ddl-auto=update`, namespace explícito e ownership da mesma role limitada; nenhum DDL manual, limpeza de banco ou aumento de privilégios foi usado. O suporte foi conferido na documentação e no bytecode do Hibernate instalado ([configuração Hibernate 7.4.13](https://github.com/hibernate/hibernate-orm/blob/7.4.13/documentation/src/main/asciidoc/introduction/Configuration.adoc)). As JVMs do JAR recebem o mesmo schema da preparação; Redis usa um índice vazio por cenário, preservando o índice da interface e os dados anteriores.

### Ensaios fora desta execução

- `AnexoTraversalTcpJarIT`, `ReinicioIntegralCorpusJarIT` e `SessaoLogoutIdleConcorrenteJarIT` dependem do suporte com JAR/hash histórico fixo. Não tiveram seus guardiões alterados para aceitar a candidata por associação.
- `FrequenciaTimerTrintaDiasJarIT` depende do artefato, configuração de 30 dias e preparação/backup daquela campanha. Não representa o padrão atual de lançamento em sete datas civis e edição em 72 horas.
- `PreparacaoFrontendRealIT`, `PrepararPainelAnaliticoLaboratorioIT` e o método `prepararFixtureOptInParaRecuperacaoEmSegundaFase` são preparadores opt-in, sem aceite funcional por si sós; não foram acionados como testes de aprovação.

As verificações atuais de autorização, arquivos, sessão, janela temporal e reinício foram executadas nos respectivos ITs de fonte e no `JarHttpRealIT` da candidata. Isso não transforma os cenários históricos excluídos em novos passes.

### Comandos e repetição

Comandos pesados executados pela fila `~/.codex/tools/heavy-run.py`, dentro dos cgroups registrados; não executar simultaneamente.

| Cwd | Conferência |
| --- | --- |
| `React/` | `npm run test:preparacao`; `npm run test`; `npm run build` (inclui `npm run typecheck`) |
| `React/` | `npm run sync:java:check`, depois `npm run sync:java`, com backup prévio dos assets |
| `JAVA/` | `bash ./mvnw -B -ntp -T1 -Dmaven.compiler.useIncrementalCompilation=false package` |
| `JAVA/` | Lotes nominados: `bash ./mvnw -B -ntp -T1 -Pintegration -DskipTests=false -DskipITs=false -Dit.test=CLASSES jacoco:prepare-agent failsafe:integration-test failsafe:verify` |
| `JAVA/` | `RelatorioCancelamentoAutoridadeAtualHttpIT` acrescenta `-Dtest.jvm.args="-Xmx192m -XX:MaxMetaspaceSize=256m"` |
| `JAVA/` | `DatabaseLoadIT` usa o perfil `db-load`, com os limites padrão do teste preservados |
| `JAVA/scripts/operacao/` | `python3 -m unittest -q test_sgt_operacao.py` |

Os ITs exigem `INTEGRATION_POSTGRES_URL`, `INTEGRATION_POSTGRES_USER` e `INTEGRATION_POSTGRES_PASSWORD` explícitos do laboratório; cenários de cache também exigem Redis real. `JarHttpRealIT` exige PID/run-id da instância Redis conferidos. `RelatorioJarMultipaginaIT` aceita `SGT_INTEGRATION_DATABASE_EXPECTED` explícito para o laboratório atual, mantendo as verificações de versão, loopback, ownership e privilégios.

Cada um dos cinco critérios de `FechamentoJarRecuperacaoIT` é selecionado individualmente por `-Dit.test=FechamentoJarRecuperacaoIT#METODO`, em uma JVM de teste nova. Usar `SGT_INTEGRATION_SCHEMA=sgt_auditoria_nome_unico`, URL JDBC com `currentSchema` exatamente igual e um `REDIS_DATABASE` vazio exclusivo daquele cenário. O Hibernate cria o schema com `update`; o teste confere ownership e recusa jobs recuperáveis anteriores. Os limites de 200 registros/120 segundos, heap de 192 MiB dos filhos e máximo de dois executores são mantidos. Não limpar banco ou Redis para forçar aprovação; preservar a evidência anterior e usar outro contexto isolado autorizado.

O relatório bruto de cada lote, XML e recibos de recursos permanecem no diretório local ignorado da auditoria. Preparação de massa, cenários deliberadamente excluídos e execuções interrompidas não contam como aprovação.
