**Parecer16: gate técnico backend APROVADO no escopo sintético delimitado.** As quatro lacunas do parecer15 foram comprovadas e os dois P3 corrigidos. Não identifiquei novos achados P0–P3 neste incremento.

| Predicado | Resultado |
|---|---|
| **JAVA-API — cancelamento e autoridade atual** | **Fechado.** Proprietário rebaixado, com novo login legítimo, recebe403; tarefa, fatos e arquivo permanecem iguais enquanto o gerador está retido. Autoridade restaurada permite cancelar, limpar o arquivo tardio e executar nova exportação. [Teste:51](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/RelatorioCancelamentoAutoridadeAtualHttpIT.java:51). |
| **SEC-011 — logout/idle concorrentes** | **Fechado.** Espera física de61,08s, dois backends bloqueados no DELETE do alvo e pedidos simultaneamente pendentes. Logout200, idle401, alvo revogado e sessão de outro usuário preservada. [Teste:17](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/SessaoLogoutIdleConcorrenteJarIT.java:17). |
| **SEC-012 — traversal TCP** | **Fechado nos seis vetores declarados.** Request-target enviado por socket; todos retornaram400, sem conteúdo protegido, alteração dos arquivos ou consumo da concessão. Controles canônicos entregaram os bytes antes/depois. A recusa não é atribuída a handler não alcançado. [Teste:20](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/AnexoTraversalTcpJarIT.java:20). |
| **RST-004 — reinício integral do corpus** | **Fechado.** Mesmo JAR/PG/Valkey, novos PIDs Java e comparação integral:72tabelas,196linhas, catálogo/sequências e dois arquivos, sem exclusões nem login entre snapshots. Job, emissão, documento e nova exportação funcionaram depois. [Teste:17](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ReinicioIntegralCorpusJarIT.java:17). |

Os dois **P3 estão fechados**:

- Cobertura: controles mutáveis explicitamente excluídos; os1.648 hashes finais conferem. Também conferem244entradas, classIDs atuais, união dos probes e contadores XML/CSV.
- Documento de escopo: descreve corretamente o PDF atual28+13 e distingue o controle histórico41+0. [Texto:7](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/DOCUMENTACAO/CODEX/CAMPANHA_20261109/ESCOPO_HOMOLOGACAO.md:7).

Mantive a compatibilidade delimitada aprovada no15. Os três lotes43 conservam seu helper43; o reinício44 usa o acréscimo de C1 no filho e preparador128MiB. Conferi que esse delta não altera oráculos, corpus ou prazos. Tentativas falhas/preventivas permanecem excluídas.

O congelamento confere integralmente:1379fontes/recursos e1763classes; todos os1374arquivos/1753classes anteriores permanecem idênticos. Manifesto `b58bf897…a00f3bf`, input `5a3d2bd2…ec5583d` e JAR `dcb3a384…26f7d` verificados.

Revisão somente de leitura, sem executar testes/builds/servidores. A aprovação permite prosseguir à etapa React; **não concede homologação integrada/global**. Permanecem separados Chrome, writerFE02, LAN/outro computador, cadência padrão de idle/scheduler, meia-noite física, restauro/reboot, migração de acervo, autenticidade institucional, corpus nacional e PDF/UA.
