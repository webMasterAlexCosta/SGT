# Fechamento: diagnóstico e comprovação em andamento

O candidato Java atual concluiu os controles reais de 200 vínculos com chunk 1 em **81.317 ms** e chunk 500 em **102.114 ms**, abaixo dos 120.000 ms desde o POST. Revogação durante espera e rollback após morte no chunk 500 também passaram. Recuperação em fases passou, sujeita à revisão da equivalência. Esses resultados não aprovam sozinhos o backend; regressão, relatórios, demais integrações e revisão independente permanecem pendentes.

JAR atual: `8c3a2edcb08d4286db99ae093b3461951cf8b32bd3339a8d1ee2e58e84d8d1de`. Recibos: `it-job-c1-02`, `it-job-c500-06`, `it-job-revogacao-01`, `it-job-atomicidade-04` e recuperação `it-job-preparacao-02` + `it-job-segunda-fase-02`. Cenários, estados HTTP e snapshots persistidos estão preservados no acervo privado, com hashes. [Índice sanitizado](FECHAMENTO_REAL.json); [ambiente e corpus](AMBIENTE_E_CORPUS.json). A tabela abaixo preserva a investigação anterior e seus próprios bytes.

## Falhas e causa observada

| Tentativa | Condição e resultado |
|---|---|
| it-job-c1-01 |200/chunk 1, JAR anterior:85.537 ms, passou. Essa prova conserva os seus próprios bytes e configuração.|
| it-job-c500-01 |Fixture com lease 15 s, abaixo do padrão 300 s: reenfileirou enquanto preparava, não aprovou.|
| it-job-c500-02 |Lease corrigido para 300 s; excedeu 120 s na preparação. Amostras mostraram CPU em reconstrução temporal e AUTO flush do Hibernate.|
| it-job-c500-03 |Contexto da preparação limitado; alcançou totalPrevisto 200, mas o bloco ainda excedeu 120 s.|
| it-job-c500-04 |Amostras localizaram AUTO flush/cascade/dirty checking em salvarOuAtualizarInterno, dentro do bloco de frequências.|
| it-job-c500-05 |Preparação e processamento com contextos limitados;200/200, CONCLUIDO,104.399 ms, sem profiling, pico agregado 7.449.124.864 bytes, nenhum filho restante.|
| it-job-recuperacao-01 |Dois recuperadores carregaram a mesma versão na seleção de travados. Lock PESSIMISTIC_WRITE e ordem por ID foram acrescentados na mesma transação.|
| it-job-recuperacao-02 |OOM no cgroup próprio de 2,5 GB, com Maven/contexto Spring preparador e dois JARs simultâneos. Interrupção preservada, sem aprovação.|
| it-job-segunda-fase-01 |Maven/preparador já encerrados: pico próprio 1,107 GB, sem OOM. Dois logins do mesmo usuário revogavam a primeira sessão e causaram 401. Fixture passou a compartilhar uma sessão HTTP real entre B/C.|
| it-job-preparacao-02 + it-job-segunda-fase-02 |Preparação auxiliar encerrada; B/C reais disputaram o mesmo job e retomaram automaticamente em 68.205 ms. 200 frequências/eventos únicos, prefixo e tentativa preservados, terminal estável com ambos vivos por mais 3 s. Pico próprio da fase2 1,192 GB/agregado 6,226 GB, sem OOM ou filhos restantes.|

Nos recortes diagnósticos, o worker consumia CPU e o PostgreSQL não indicava espera física por lock. As amostras identificaram verificações repetidas do conjunto de entidades gerenciadas. O primeiro ajuste removeu esse crescimento na preparação; a medição seguinte revelou o mesmo custo no processamento.

## Alteração e invariantes

`ElegibilidadeFechamentoFrequenciaServico` sincroniza/desanexa as entidades após adquirir os locks e a cada 25 projeções avaliadas. `FrequenciaServico` mantém todos os locks do chunk antes de processar contextos internos de 25 empregados. Recarrega e revalida o criador após as esperas, preserva reconstrução temporal, idempotência, autoria, eventos e validações.

O chunk e o volume não foram reduzidos. Flush sincroniza alterações dentro da transação; não confirma uma página. Cursor, contadores e frequências continuam confirmados juntos no fim do chunk. Não foi adotado flush mode COMMIT, que alteraria a visibilidade das consultas, nem removida validação para acelerar.

Fundamentação: orientação de [batching do Hibernate 7.4.13](https://github.com/hibernate/hibernate-orm/blob/7.4.13/documentation/src/main/asciidoc/userguide/chapters/batch/Batching.adoc), [semântica de AUTO flush](https://github.com/hibernate/hibernate-orm/blob/7.4.13/documentation/src/main/asciidoc/userguide/chapters/flushing/Flushing.adoc) e [locks transacionais do PostgreSQL 18](https://www.postgresql.org/docs/18/explicit-locking.html). A comprovação adversarial adicional bloqueia a frequência de posição 100 e mata somente a JVM filha: deve preservar a fonte original e rejeitar qualquer confirmação parcial das 99 anteriores, seus eventos e auditoria.

No JAR atual, `it-job-atomicidade-04` reconheceu esse alvo em 80.162 ms e confirmou rollback integral após morte/EOF: fonte original, eventos, histórico, auditoria e contadores intactos. A fixture libera sua barreira artificial depois de matar A, antes de aguardar a desconexão do PostgreSQL, sem alterar `client_connection_check_interval=0` do servidor. A captura de consultas do laboratório foi ampliada de 1 kB para 64 kB para reconhecer o SELECT longo; cluster, versão, roles, dados e limite de memória foram revalidados/preservados.

A prova em fases não afirma execução aprovada do método JUnit completo. O preparador gera `PREPARATION_ONLY_NOT_ACCEPTANCE`; após seu encerramento, o observador Java sem Spring/Maven confere identidade/ownership, banco/job/tentativa/snapshot canônico e hashes de fontes/classes/JAR/handoff antes de iniciar B+C. Resultado conjunto `PASS_IN_PHASES`, com `full_junit_executed=false`; a equivalência será submetida à revisão independente. Não houve redução de volume, executores ou critérios.

## Ambiente e limites

PostgreSQL 18.6/Valkey 9.0.7 nativos isolados, role PostgreSQL limitada e ownership conferidos. Máquina AMD Ryzen 7 4800H, MemTotal reportado 32,971 GB; JVMs usam ActiveProcessorCount=2, sem quota de CPU imposta por cgroup. Fork 256 MiB/metaspace 256 MiB; JAR 192 MiB/metaspace 192 MiB, pool 2. Maven 512 MiB. Cgroup de validação 2,5 GB, swap 0, guarda operacional agregada 8 GB e reserva mínima 2 GB; teto humano 15 GB decimais.

O corpus deste controle tem 2 contratos, 201 pessoas/vínculos/alocações, 200 frequências e 200 eventos. O job seleciona200 vínculos; o vínculo restante pertence ao controle de outro escopo. É um único job, com polling HTTP em loopback e sem outra suíte pesada simultânea. Esses números delimitam a prova; não constituem aprovação de carga nacional, navegador, cliente LAN externo ou produção.

Os demais critérios devem apontar resultados compatíveis com os bytes finais. O teste adversarial não exige terminal porque interrompe intencionalmente o bloco; ele complementa o controle200/200. O teste com dois executores precisa de disputa comprovada e ambas as JVMs saudáveis, além da ausência de duplicações. Qualquer falha mantém o gate pendente.
