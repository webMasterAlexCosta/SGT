**Quatro lacunas P2 estão encerradas no alcance focal. PERF-001–008 permanece parcialmente pendente.** Não identifiquei novo defeito produtivo neste incremento.

**[P2 mantido parcialmente] Completar a caracterização de recursos dos fluxos HTTP.** [CaracterizacaoHttpJar.java:62](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/CaracterizacaoHttpJar.java:62) agora registra latência, payload, cache, CPU e RSS de caminhos reais. Isso resolve a limitação anterior da carga restrita ao banco. Entretanto, não localizei medições vinculadas de **GC, filas/pool, esperas/locks e contadores de consultas**, exigidas pelo [mestre:518](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/DOCUMENTACAO/MATRIZES_COMPLETAS/PROMPT_MESTRE_SGT_CORRECAO_E_HOMOLOGACAO_8045_20261103.md:518). Falta coletar ou vincular essas observações ao cenário atual para encerrar o predicado comum. As 45 amostras permanecem válidas como caracterização local descritiva, com concorrência 1; não exigem SLA nem corpus nacional.

| Lacuna anterior | Resultado desta revisão |
|---|---|
| **ROP-025** | **Encerrada.** Dois homônimos com identidades distintas; Vínculo A + Empregado B recebe 409, preservando snapshots e eventos. Controles A/A e B/B passam na mesma data. [Asserções:276](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/JarHttpRealIT.java:276). |
| **RST-002** | **Encerrada no alcance crítico examinado.** Expectativas independentes verificam PKs, FKs com colunas/destinos, unicidades, índices e nulidade; a captura inclui definições e validade. [Asserções:59](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/CatalogoPostgresAssercoes.java:59). |
| **RST-004/008** | **Encerrada a lacuna apontada.** Catálogo lógico idêntico após reinício; DTO integral e células estáveis do XLSX comparados; PDF/XLSX reais novamente gerados. A normalização de “Gerado em” é restrita e valida o instante. [Reinício:197](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/JarHttpRealIT.java:197). |
| **TMP-012/ARQ-003** | **Encerrada a parte DTO/backend apontada.** Datas e valores discriminantes 15/14, ausência do comparativo de 29/02, fontes preservadas e sequência comparação false/true/false. [Asserções:66](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/PainelBissextoDtoRealHttpIT.java:66). |
| **PERF-001–008** | **Parcialmente encerrada**, com a pendência de instrumentação descrita acima. |

Conferi hashes e XMLs sanitizados dos três ensaios focais: JAR HTTP, bissexto e relatório, todos 1/1, sem falhas, skips, deriva ou processos remanescentes nos recibos. O aproveitamento do bissexto continua válido: as diferenças posteriores de fonte/bytecode estão nos outros harnesses; seu teste e dependências pertinentes permaneceram iguais.

Os **1.349 arquivos de fonte, 25 documentos e bytecode atual** conferem com o manifesto `2638b14b…`. Produção e JAR `ae4be259…` permanecem inalterados. As imagens atuais conservam os 41 registros legíveis na página 1 e total/legenda na página 2, sem comprovar continuação da tabela.

Mantêm-se as conclusões anteriores nos respectivos alcances. Regressão restante, matriz completa, writer React, navegador e LAN continuam pendentes. Não alterei arquivos nem executei testes/builds/aplicações.

:codex-file-citation{path="/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/pdf-visual-contextual-02/cairo-a-1.png" purpose="source"}
:codex-file-citation{path="/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/pdf-visual-contextual-02/cairo-a-2.png" purpose="source"}
