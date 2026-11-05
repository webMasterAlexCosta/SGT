# Parecer independente final — candidata04

**Decisão global: PARCIAL / NÃO COMPROVADO.** Corte documental final C04, preparado em 2026-11-04T09:18:52.134903+00:00. Inclui as provas FE-02 encerradas, a revisão do relatório em dez seções e o estado dos arquivos após a parada normal. O [texto C03 preservado](historico_documental/C03_20261104_032454/PARECER_INDEPENDENTE_FINAL.md) conserva seu próprio corte temporal.

**Candidata:** H04/candidata04, JAR `6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d`.

**Revisor:** agente `/root/auditoria`, modelo exposto **GPT-6 Astra Pro**. O revisor foi separado dos autores das alterações e executou 31 invocações críticas próprias. Trata-se de segregação técnica dentro da equipe, não de certificação externa. Os cortes C02 e C03 e seus recibos permanecem preservados.

## Decisão de aceite

**PARCIAL / NÃO COMPROVADO para homologação integral.** A candidata tem correções rastreáveis e provas atuais em unitários, componentes, MySQL/MockMvc, HTTP real, arquivos, upgrade e recuperação de job. Não estão concluídos todos os gates do [§13 do mestre](../00_BASELINE_IDENTIDADE/PROMPT_MESTRE_SGT_CORRECAO_E_HOMOLOGACAO_8045_20261103.md). Este parecer não autoriza implantação em produção.

Os dois erros 1205/40001 do consumidor automático são reais e permanecem preservados. Os 20 predicados manuais do chunk500 passaram. Como não havia massa vencida legítima preparada nesse ensaio, ele não comprovou descumprimento do oráculo ROP-034 nem recuperação desse consumidor. A decisão parcial não significa que os erros sejam inofensivos. Se uma contraprova demonstrar defeito funcional ou falha de gate crítico, o contrato exige REPROVADO. A [análise normativa](REVISOES_CANDIDATO04/DECISAO_NORMATIVA_TIMEOUTS_E_ACEITE_FINAL04_20261104.json) fundamenta esse enquadramento.

## Identidade e fontes de prova

| Artefato | Quantidade ou identidade | SHA-256 |
|---|---|---|
| [JAR final](../06_ENTREGA_FINAL/sgt-backend-final.jar) | Spring Boot, 88 bibliotecas e 98 assets | `6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d` |
| [Fontes Java](../06_ENTREGA_FINAL/SGT_JAVA_FONTES_FINAL.zip) | 1338 membros | `0962371859363cf50b70ae04d7a27af731cb6c38a2bec96e31f727152390984e` |
| [Fontes React](../06_ENTREGA_FINAL/SGT_REACT_FONTES_FINAL.zip) | 600 membros | `ce9cf947df1e6294eeb9d91173510d179835ea1b94d5d7d61f6283673ce05447` |
| [Build React](../06_ENTREGA_FINAL/SGT_REACT_BUILD_PRODUCAO_FINAL.zip) | 98 assets | `21770ea3b2c51ece9fa320426434508d332d6bb2e7e1690b8963f3e162601905` |
| [Selo de bytes rev2](../06_ENTREGA_FINAL/SELO_FINAL_BYTES.json) | Commit `19824eb0aa93d6a4d826b757852fee15a68adb92` | `ad30c1a3facd3235cbfc6c9e8e3b1820bf67b23f8d7133efbfaf3f01690afc79` |

As provas atuais dependentes de banco citadas aqui usam **MySQL Server 8.0.45**, UUID **e3404ecd-d7dc-11f1-9b9a-1b51466e5356**, e Redis real nas campanhas pertinentes. Cada execução possui identidade de catálogo e ambiente; não se afirma que todos os cenários usam o mesmo schema ou arquivo de ambiente. Hibernate/JPA gere o schema da aplicação, com `ddl-auto=update` e `sql.init.mode=never`. Os observadores SQL externos empregados nas comparações são de leitura. UUIDs, JARs e falhas anteriores são antecedentes datados, sem transferência de resultado positivo para a candidata atual.

A [coleção pública de revisões](REVISOES_CANDIDATO04/INDICE_REVISOES_ATUAIS.md) materializa 40 documentos do corte comprovado, com hashes de origem e destino. A [reprodução de 31 invocações](REPRODUCAO_CANDIDATO04_31/PUBLICATION_MANIFEST.json) tem coleção própria e não foi duplicada. As tabelas canônicas e seus JSONs integrais estão em [01 — regras, contratos e matrizes](../01_REGRAS_CONTRATOS_MATRIZES/MATRIZ_198_635_NOVA.md).

## Evidências que sustentam o alcance comprovado

O Java final04 encerrou **1999 invocações — 599 UT e 1400 IT — em 449 XML**, sem falhas, erros ou skips. A reconstrução vinculou fontes, 1267 inputs arquivados, 899 classes/resources e JAR reconstruído idêntico ao 6ab. A cobertura após Failsafe foi conferida por sessões, configuração, frescor e comando. O revisor repetiu **31 invocações, UT14 + IT17**, sem compile, clean ou mudança de fontes; todas passaram, com **413 conferências semânticas**. Essa repetição sobrepõe a suíte e não amplia seu total de casos únicos.

O React atual encerrou **653 invocações em 88 suítes**, sem falhas, erros ou skips e com o multiconjunto preservado. Há 51 casos novos em relação aos 602 da baseline e 84 CASEs únicos com 98 vínculos. A cobertura global do universo de 232 itens reportados foi de 73,58% de linhas, 57,19% de ramos, 61% de funções e 69,11% de statements, sem threshold adicional. As 600 fontes e os 98 assets permaneceram idênticos; F11 React03 regenerou os 98 assets byte a byte. Nenhum total unitário homologa duas abas Chrome ou jornadas de UI.

Os 49 suplementos mantêm **48 PASS e uma falha de preparo ALO**, sem erro ou skip. A assertion que exige 201 permanece; seu recebimento de 409 impede chegar à reativação. A carga JPA real passou em um caso, no corpus declarado. A Native atual concluiu HTTP, upgrade, reinício e arquivos, com **3250 assertions e 1357 requisições da candidata**, separadas das 105 assertions e 59 requisições de preparação H02. Os 33 registros de seu controlador são registros de orquestração, não 33 testes. Oito cenários DOC antes e depois de reiniciar, 98 assets, PROR, relatório de 41 vínculos, DTO FE02, 404 autenticados/cache e negativos de Fornecedor foram auditados por predicado e identidade.

O ciclo chunk1 completou 200, cancelou após prefixo, reprocessou e retomou o mesmo job/tentativa depois de SIGKILL, sem POST adicional, com segunda inicialização observada. Chunk500 completou 200 com um start e uma JVM manual. O conteúdo por 200 ordinais coincidiu nos 19 campos da comparação. Os tempos de 149,162 s e 361,557 s são medições sob condições distintas, não benchmark causal ou SLA nacional. Os erros do automático acompanham o resultado positivo manual, sem serem apagados.

O primeiro complemento XLSX executou o writer produtivo fora da UI e confirmou os DTOs daquele ensaio em 1.940 checks de conteúdo e 4.328 conferências independentes. Sete colunas eram constantes naquele corpus. A prova discriminante posterior e seus limites atuais estão na seção de FE-02 abaixo.

## Reconciliação de 635 critérios, 198 grupos, 64 ROP, 21 módulos e 76 gates

Os 635 textos originais e os 198 agrupamentos foram preservados, sem transportar aprovações H01/H02. A [matriz635](../01_REGRAS_CONTRATOS_MATRIZES/MATRIZ_198_635_NOVA.csv) registra **602 PARCIAIS e 33 BLOQUEADOS POR DOCUMENTAÇÃO**; são **192 grupos PARCIAIS e seis grupos documentais bloqueados**. A conjunção técnica declarada em 633 critérios inclui 33 que continuam sem fonte administrativa autêntica. Entre os 602 não documentais, 600 têm conjunção técnica comprovada **nas camadas declaradas**, e dois permanecem parciais: ALO/PER. Essa coluna não é aceite global e não permite chamar 633 critérios de homologados.

Os [64 ROP](../01_REGRAS_CONTRATOS_MATRIZES/MATRIZ_64_ROP_NOVA.csv) têm 62 resultados PARCIAIS, ROP-055 NÃO EXECUTADO integralmente e ROP-059 BLOQUEADO POR AMBIENTE. Há aspectos legítimos de upgrade observados, sem extensão aos dados financeiros/de risco ausentes da baseline fresh. Os [21 módulos](../01_REGRAS_CONTRATOS_MATRIZES/MATRIZ_21_MODULOS_NOVA.csv) permanecem parciais, com camadas e predicados localizados. A [matriz transversal76](../01_REGRAS_CONTRATOS_MATRIZES/MATRIZ_GATES_TRANSVERSAIS_NOVA.csv) conserva 68 entradas individualizadas e oito posições PERF de uma faixa com parágrafo comum; não inventa oito predicados normativos independentes.

O [recibo de exportação C02](../01_REGRAS_CONTRATOS_MATRIZES/RECIBO_EXPORTACAO_ARTIFACT_C02.json) e a [releitura de 36016 conferências](REVISOES_CANDIDATO04/VERIFICACAO_EXPORTACAO_C02_INDEPENDENTE.json) confirmam os CSVs/MDs daquele corte. As 94 células longas apontam para JSON integral materializado, com pointer e hash, sem truncamento silencioso. A [matriz de alterações112](MATRIZ_RED_GREEN_FINAL.csv) contém 15 pares React, quatro Java e 93 caminhos de assets derivados, totalizando 12 fontes produtivas únicas. As seis correções documentais REV02 foram conferidas em 2912 células: esclareceram cronologia, replay de INVALIDAR e o limite da observação Redis, sem alterar fontes, oráculos ou resultados.

## Impedimentos e riscos que acompanham os artefatos

- Chrome atual bloqueou antes de CDP/DOM: **0/24 jornadas**. RI03 real entre abas, feedback ROP-064, interação, acessibilidade, navegação e exportação pela UI não recebem PASS por unitários ou HTTP.
- **33 critérios de seis grupos** dependem de autenticidade administrativa; dados sintéticos não resolvem essa exigência.
- **ALO-008:002** não alcançou a reativação. **PER-010:004** não encerrou o ramo conceder. **FE-02** passou no novo confronto discriminante entre DTO e OOXML; os gráficos, o PDF React e a jornada de download continuam pendentes.
- Houve **dois erros SQL 1205/40001 do scheduler automático** sob contenção; recuperação de vencidas reais não está comprovada. Cluster arbitrário e desempenho nacional equivalente não foram testados.
- A limpeza global de staging continua sem comprovação. A reaparição histórica da classe Native às 00:49 não teve causa demonstrada; a geração posterior em FE-02 foi identificada e a higiene final confirmou 727 arquivos normais. Nenhuma dessas leituras retroage sobre testes já encerrados.
- Os alertas residuais de Spring WebMVC 6.2.19 foram preservados, sem exploração SGT demonstrada e sem certificado de ausência de vulnerabilidades.

Os detalhes constam de [limites](LIMITES_E_BLOQUEIOS.md), [segurança](PARECER_SEGURANCA.md), [revisão do código](PARECER_REVISAO_CODIGO.md) e [resultados adversariais](RESULTADOS_REPRODUCAO_ADVERSARIAL.md). O alcance comprovado não equivale a “100%”, “zero erro global” ou homologação integral.

## Independência, publicação e trilha

A revisão foi conduzida por agente distinto dos implementadores. Incluiu leitura dos contratos normativos e canônicos, exame estático, contestação de oráculos, leitura dos recibos atuais e reprodução das 31 invocações. Os 40 documentos da [coleção pública](REVISOES_CANDIDATO04/INDICE_REVISOES_ATUAIS.md) são idênticos aos originais delimitados; seu manifesto tem SHA-256 `478a50cdea22cab22e29a8caed4f9c966f888361fe76b8e6c74e496133c51dab`. A coleção31 não foi duplicada. O acervo04 conserva erros, preparos falhos e antecedentes com identificação temporal.

As cópias públicas examinadas foram conferidas para supressão de credenciais, caminhos privados e snapshots integrais, preservando assertions e binários autorizados. O relatório do coordenador e os recibos de releitura de seus pacotes registram a persistência da entrega. Este parecer fixa a opinião independente de aceite parcial. Evidências posteriores exigem adendo datado e não alteram retroativamente os resultados aqui descritos.

## Histórico preservado: corte C03

O corte C03 substituiu 26 pendências genéricas por quatro resultados de infraestrutura delimitados e 22 vínculos parciais específicos. A leitura Java ligou 27 métodos e 37 invocações já presentes no F12 a 17 gates e acrescentou a observação da revogação concorrente no repositório a SEC-011. Eram vínculos documentais de execuções concluídas, com camada, transação e limites próprios. Os 635 textos, os 198 grupos e suas classificações permaneceram preservados. A exportação C03 teve 35.401 conferências; a releitura independente, 74.857. Consulte o [recibo C03](../01_REGRAS_CONTRATOS_MATRIZES/RECIBO_EXPORTACAO_ARTIFACT_C03.json) e o [índice do adendo C03](REVISOES_CANDIDATO04/ADENDO_C03/INDICE_ADENDO_C03.md).

Naquele momento, a tentativa `FE02_HTTP_VARIADO_01` estava interrompida sem terminal. O último registro HTTP preservado, às 02:59:08 UTC, deixava 164 requisições, 275 asserções, 72 respostas de cadastro e nenhum lote de frequência confirmado. A causa da perda do supervisor ficou indeterminada. As qualificações posteriores verificaram o estado efetivo sem inventar respostas POST e sem transformar essa tentativa em PASS.

O limite de ROP-055 permanece: o fixture H02 observado não continha as tabelas e linhas legadas de riscos e processos exigidas pelo roteiro. A igualdade das 72 tabelas presentes não comprova a preservação das ausentes. Não houve DDL manual ou reintrodução de mapeamentos para fabricar essa prova.

## Evidência atual de FE-02 e alcance do fechamento

O corpus discriminante tem **122 vínculos**: 72 da tentativa original, preservados e qualificados pela leitura 04; o item 73 do prefixo (vínculo 75, pessoa 75), originado no POST real com resposta 201 da tentativa 02 e depois qualificado separadamente; e 49 cadastros novos da continuação 03. As duas identidades auxiliares da fixture explicam por que a fotografia tinha 124 pessoas e vínculos. Não houve repetição daqueles 73 cadastros. A tentativa original interrompida, a tentativa 02 falha e as qualificações instrumentais que falharam mantêm seus resultados próprios.

A [continuação HTTP/SQL e reinício](REVISOES_CANDIDATO04/ADENDO_FINAL_FE02_C04/CONTINUACAO03_PARECER_ACEITO.json) conferiu **106 chamadas e 328 asserções**: 95/263 na continuação e 11/65 após reinício. Os 78 POSTs de negócio correspondem aos 49 vínculos novos e 29 lotes; os dois logins são contados à parte. Foram conferidos separadamente 1.392 registros de frequência, 1.392 eventos, 1.392 históricos e 1.392 registros de auditoria posteriores ao commit. As 29 fontes de capacidade já existiam. A revisão independente encerrou 17.422 verificações; as duas JVMs próprias tiveram parada normal.

A [reabertura independente dos dois XLSX](REVISOES_CANDIDATO04/ADENDO_FINAL_FE02_C04/WRITER03_PARECER_OOXML_REAL.json) confirmou os DTOs de 15 datas atuais e 14 anteriores. As dez métricas diárias variam nas 29 datas observadas. Em cada planilha, B–K contêm 15 valores atuais distintos por coluna; L tem 14 valores numéricos anteriores e o texto “—” no dia 29/02. A comparação retorna à data civil de 01/03 depois do dia bissexto. Cada XLSX tem sete abas, 360 células tipadas e série diária de 15 × 12. Os 26.739 checks do executor e as 23.706 conferências independentes não são casos Vitest adicionais.

O exportador e o writer produtivos consumiram DTOs HTTP reais em contexto Node/Vite instrumentado para capturar o Blob. O ensaio comprovou valores, tipos, formatos e igualdade das células após reinício. Não demonstrou a tela, o gráfico, o download em Chrome ou o PDF React. O PDF backend com 41 registros pertence a outro cenário. `watch:null` foi solicitado; a desativação do watcher não foi demonstrada.

## Deliberação sobre os documentos e os artefatos atuais

A [revisão das matrizes C04](REVISOES_CANDIDATO04/ADENDO_FINAL_FE02_C04/MATRIZES_C04_PARECER.json) observou 60 células documentais alteradas: 42 associações estáticas de controllers nas duas cópias da matriz de módulos e 18 campos de evidência ou limite de FE-02. Os demais campos, a redação normativa e as classificações permaneceram iguais. Os 21 módulos e 62 dos 64 ROP continuam parciais; ROP-055 não foi executado no legado exigido, e ROP-059 foi bloqueado pelo ambiente. As 635 linhas de critérios conservam 602 resultados parciais e 33 bloqueios documentais. Um rótulo histórico de ARQ-003 ainda menciona colunas constantes; a ressalva do relatório esclarece que esse limite foi superado **neste corpus DTO/OOXML**, sem aprovar o gate integral.

O [parecer documental do relatório final](REVISOES_CANDIDATO04/ADENDO_FINAL_FE02_C04/RELATORIO_FINAL_REV03_PARECER.json) examinou a versão `88da9aa9bb9d02f1a183da64e3657407627ab6904ed6dc52ebfee413a062e488`, as notas, o README e o guia. Foram verificadas as lacunas literais dos 21 módulos e 64 roteiros e os contadores das matrizes. A decisão desse conjunto é consistente com o §13: a evidência atual não demonstrou divergência funcional crítica nos cenários aprovados, e os impedimentos restantes são materiais. **PARCIAL / NÃO COMPROVADO** continua sendo o veredito integral.

A [higiene posterior](REVISOES_CANDIDATO04/ADENDO_FINAL_FE02_C04/HIGIENE04_PARECER.json) confirmou 724 classes e três recursos normais no diretório de testes, zero fixture de preparação e 899 arquivos principais iguais aos do JAR. A classe extra foi arquivada e relida antes da remoção. Isso demonstra o estado posterior, sem explicar a reaparição histórica observada às 00:49. A base histórica React foi aceita somente após classificação dos achados de sua varredura. O [índice das revisões de fechamento](REVISOES_CANDIDATO04/ADENDO_FINAL_FE02_C04/INDICE_ADENDO_FINAL.md) apresenta as provas atuais e os primeiros resultados falhos preservados. A auditoria lateral do ZIP final será vinculada aos bytes do arquivo efetivamente montado; não faz parte do resultado antecipado deste parecer.
