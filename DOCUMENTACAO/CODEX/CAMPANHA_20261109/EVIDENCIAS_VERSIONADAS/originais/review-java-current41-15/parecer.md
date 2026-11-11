**Parecer15: não identifiquei defeito produtivo novo nos dois arquivos Main alterados. Aprovo a compatibilidade delimitada das provas anteriores. O fechamento do gate técnico backend permanece PENDENTE pelas quatro lacunas de prova abaixo.**

Encontrei dois achados P3 de rastreabilidade/documentação:

::code-comment{title="[P3] Manifesto inclui arquivo ainda mutável" body="coverage-proof.json registra execution-state.json com hash 4c9fc3d4ac76…, mas o arquivo preservado está FINISHED e tem hash ce6a9c46c8c2…. O pipeline sela esse controle antes de run_unit.py:396 finalizá-lo. Exclua controles voláteis do conjunto imutável ou finalize a selagem após o runner. As outras 1.645 entradas conferem; classIDs, união dos probes e contadores XML/CSV também conferem. A divergência afeta a integridade declarada do manifesto." file="/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/coverage_pipeline.py" start=198 priority=3}

::code-comment{title="[P3] Escopo descreve o PDF anterior como atual" body="O texto atribui ao controle atual 41 registros na primeira página e apenas resumo/legenda na segunda. RELATORIO_REAL.json agora aponta para o cenário atual, cuja renderização conferida tem 28+13 registros e cabeçalho repetido. Atualize a descrição ou identifique explicitamente o recibo histórico, preservando a distinção entre as duas provas." file="/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/DOCUMENTACAO/CODEX/CAMPANHA_20261109/ESCOPO_HOMOLOGACAO.md" start=7 priority=3}

As lacunas backend exigem estas provas específicas:

| Predicado | Ação/oráculo ainda sem prova suficiente |
|---|---|
| **JAVA-API — autorização atual no novo cancelamento** | Com executor retido, alterar/revogar a autoridade do proprietário e chamar **POST `/cancelamento`**: recusa sem transição para `CANCELADO` nem efeitos. O novo IT cobre terceiro, proprietário e repetição; as regressões de revogação exercitam consulta/download/geração. [IT:66](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/RelatorioExportacaoCancelamentoHttpIT.java:66). |
| **SEC-011 — logout concorrente com idle/revogação** | Exercitar as entradas HTTP concorrentes, comprovar sobreposição e verificar revogação do alvo, preservação de sessão-controle legítima e ausência de erro otimista/5xx. As duas revogações PostgreSQL diretas mantêm sua prova própria. [IT:30](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/integration/TokenRevogacaoConcorrenciaH02IT.java:30). |
| **SEC-012 — traversal de arquivos** | Preservar cenário TCP com caminhos manipulados/codificados, recusa sem bytes protegidos ou consumo indevido e controle autorizado. IDs de outro escopo já têm negativas; não encontrei o caso discriminante de traversal nos recibos apresentados. |
| **RST-004 — preservação integral no reinício** | No mesmo corpus isolado/JAR, comparar snapshots canônicos de linhas, arquivos e jobs antes/depois da parada graciosa, identificando mudanças legítimas. O ensaio atual compara catálogo físico e releituras específicas; a recuperação de job usa outro cenário. Esse conjunto ainda não comprova o snapshot integral exigido. [ensaio:208](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/JarHttpRealIT.java:208). |

O incremento comprovou, nos respectivos recortes:

- Cancelamento cooperativo, repetição, disputa com conclusão, limpeza tardia, download409 e controle normal; RED preservado e GREEN22IT+2unitários.
- Concorrências SEC-007/CON-001/003/005/007, falha física de arquivo CON-009 e retificação documental CON-010.
- Entradas negativas de frequência e parcelas temporais examinadas, sem efeitos persistidos.
- N30 no serviço e no scheduler do JAR, com alvo e testemunha posterior. A prova física usa cron2s de laboratório.
- JAR atual com146HTTP/45janelas, quatro métodos de jobs e recuperação em fases: mesmo job/tentativa2,200 efeitos,65.891ms. `fullJUnitExecuted=false` permanece correto.
- PDF41 com **28+13**, cabeçalho repetido e sem cortes visíveis nas três imagens; XLSX41 e controleB1 vinculados. :codex-file-citation{path="/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/it-relatorio-contextual-03/scenario-evidence/cenario-16388928455590633931/relatorio-a-41.pdf" purpose="source"}

A compatibilidade aprovada conserva os estágios e limites das revisões anteriores. Os739 unitários/1565ITs são resultados dos próprios bytes catalogados; **1 classe unitária e14ITs** têm Main35 exato. Os casos antigos não se tornam reexecuções atuais nem aprovação automática dos161IDs. A cobertura estrutural confere em90,42%linhas/65,52%ramos, sem atribuir probes às JVMs filhas.

Reconferi o congelamento sem divergências:1374fontes/recursos,26documentos,28scripts,26harnesses e1753classes. Manifesto `072f876d…051c4`, input `fb49cb36…f8f`; JAR `dcb3a384…26f7d`, com823classes/190assets preservados/116bibliotecas.

Revisão somente de leitura, sem executar testes/builds/servidores. Permanecem separados React/Chrome/LAN/outro computador, writer FE02/ARQ-003, meia-noite física, cadência padrão, migração de acervo, autenticidade institucional, corpus nacional e PDF/UA. Nenhuma homologação integrada ou global concedida.
