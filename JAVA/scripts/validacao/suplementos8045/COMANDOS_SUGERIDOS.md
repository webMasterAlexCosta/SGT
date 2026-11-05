# Comandos focais sugeridos — ainda não executados

O coordenador incorporará o pacote integral como suplemento opt-in em scripts/validacao/suplementos8045, fora do src/test normal. Executará após terminar a suíte pesada em andamento e preparar uma cópia isolada do backend. Sobrepor somente os 16 arquivos de src/test/java deste pacote nessa cópia; não copiar os originais, variantes ou diagnóstico. Todas as49 invocações, inclusive o negativo ALO, devem ter resultado preservado; os seletores abaixo não são uma alegação de full verde. O POM, dependências e fontes do produto permanecem os autorizados pelo coordenador.

Exigir antes: Java 17.0.20 da campanha, conforme release17 do POM; SGT_MVN e SGT_MAVEN_REPOSITORY apontando para Maven/cache já aprovados; SGT_SUPPLEMENTAL_PROJECT para a cópia isolada; SGT_SUPPLEMENTAL_EXECUTE=1; INTEGRATION_MYSQL_MODE=native; INTEGRATION_MYSQL_URL para 127.0.0.1:33075/sgt_audit_8045_red (ou schema já autorizado sob o mesmo prefixo); INTEGRATION_MYSQL_USER=sgt_campaign. SGT_MYSQL_SERVER_UUID deve vir explicitamente do ambiente privado selado da rodada e conter um UUID completo válido. INTEGRATION_MYSQL_PASSWORD e REDIS_PASSWORD devem ser injetados pelo mecanismo privado existente, sem printenv ou eco. REDIS_HOST, REDIS_PORT e REDIS_DATABASE são explícitos e devem coincidir com as propriedades efetivas. Não há criação de schema, conta ou concessão neste pacote.

O guard compara SELECT @@server_uuid com SGT_MYSQL_SERVER_UUID, sem valor padrão e sem adotar o valor observado. Exige MySQL/JDBC exatamente 8.0.45, JPA update e SQL init never. A conta observada em B230 continua estritamente sgt_campaign@127.0.0.1.

A recriação do datadir após a interrupção das18:31:41Z rompeu a continuidade física da campanha. Os recibos anteriores conservam o UUID antigo e não provam persistência/restart do servidor novo. A nova identidade deve ser confirmada por recibo nativo independente antes de executar estes suplementos; não reutilizar evidências antigas como se pertencessem ao novo datadir.

Os comandos usam os goals de compilação e Failsafe para selecionar estes IT; não executam a suíte unitária geral, não geram o JAR final e não representam clean verify. O limite de memória deve ser composto com o processo pai pelo coordenador; abaixo há apenas o teto de 768 MiB para o fork.

## SUP_A

6 invocações previstas pela leitura das anotações; confirmar pela execução e pelo XML.

```bash
flock /tmp/sgt-heavy.lock "$SGT_MVN" --offline -B -f "$SGT_SUPPLEMENTAL_PROJECT/pom.xml" \
  -Dmaven.repo.local="$SGT_MAVEN_REPOSITORY" -Pintegration \
  '-Dit.test=A244Adi004AtomicidadeHttpIT,A244Cmp005InstrumentosHttpIT,A244Con005PendenciasHttpIT,A244Con008FotografiaHttpIT,A244Con012RetomadaHttpIT,A244Con018AnulacaoHttpIT' \
  '-Dtest.jvm.args=-Xms128m -Xmx768m -XX:MaxDirectMemorySize=128m' \
  -Dfailsafe.failIfNoSpecifiedTests=true -DfailIfNoTests=true \
  test-compile failsafe:integration-test failsafe:verify
```

## SUP_B_CATALOGOS_RELATORIOS

19 invocações previstas pela leitura das anotações; confirmar pela execução e pelo XML.

```bash
flock /tmp/sgt-heavy.lock "$SGT_MVN" --offline -B -f "$SGT_SUPPLEMENTAL_PROJECT/pom.xml" \
  -Dmaven.repo.local="$SGT_MAVEN_REPOSITORY" -Pintegration \
  '-Dit.test=B230CatalogosPessoasComplementosHttpIT,B230RelatoriosComplementosHttpIT' \
  '-Dtest.jvm.args=-Xms128m -Xmx768m -XX:MaxDirectMemorySize=128m' \
  -Dfailsafe.failIfNoSpecifiedTests=true -DfailIfNoTests=true \
  test-compile failsafe:integration-test failsafe:verify
```

## SUP_B_TEMPORAIS

6 invocações previstas pela leitura das anotações; confirmar pela execução e pelo XML.

```bash
flock /tmp/sgt-heavy.lock "$SGT_MVN" --offline -B -f "$SGT_SUPPLEMENTAL_PROJECT/pom.xml" \
  -Dmaven.repo.local="$SGT_MAVEN_REPOSITORY" -Pintegration \
  '-Dit.test=B230TemporalOperacionalComplementosHttpIT#tmp017AnulacaoMudaOMesmoDiaSomenteNoCorteDeConhecimentoDoAto+vin002PesquisaIgnoraDataDaMudancaDeAlocacaoEDataAdmissaoDivergente+vin008FimNaturalRecusaOperacaoNoDiaSeguinteMasVinculoContinuaAberto+vin013ReativacaoRetomaElegiveisMasNaoEncerradoCanceladoOuVinculoSucedido+alo008ReativacaoPreservaFaixaVencidaESubstituidaERetomaSomenteUltimaValida+fre016NovoOperacionalNaoHerdeEdicaoDaFonteMesmoDentroDosSeteDias' \
  '-Dtest.jvm.args=-Xms128m -Xmx768m -XX:MaxDirectMemorySize=128m' \
  -Dfailsafe.failIfNoSpecifiedTests=true -DfailIfNoTests=true \
  test-compile failsafe:integration-test failsafe:verify
```

## SUP_C

17 invocações previstas pela leitura das anotações; confirmar pela execução e pelo XML.

```bash
flock /tmp/sgt-heavy.lock "$SGT_MVN" --offline -B -f "$SGT_SUPPLEMENTAL_PROJECT/pom.xml" \
  -Dmaven.repo.local="$SGT_MAVEN_REPOSITORY" -Pintegration \
  '-Dit.test=CriteriosCDocumentosHttpIT,CriteriosCHistoricoHttpIT,CriteriosCOcorrenciasHttpIT' \
  '-Dtest.jvm.args=-Xms128m -Xmx768m -XX:MaxDirectMemorySize=128m' \
  -Dfailsafe.failIfNoSpecifiedTests=true -DfailIfNoTests=true \
  test-compile failsafe:integration-test failsafe:verify
```

## SUP_ALO_PREPARO_SEPARADO

1 invocação prevista pela leitura das anotações; confirmar pela execução e pelo XML.

```bash
flock /tmp/sgt-heavy.lock "$SGT_MVN" --offline -B -f "$SGT_SUPPLEMENTAL_PROJECT/pom.xml" \
  -Dmaven.repo.local="$SGT_MAVEN_REPOSITORY" -Pintegration \
  '-Dit.test=B230TemporalOperacionalComplementosHttpIT#alo008ComposicaoZeradaPorAditamentoImpedeRetomadaSemGravarReativacao' \
  '-Dtest.jvm.args=-Xms128m -Xmx768m -XX:MaxDirectMemorySize=128m' \
  -Dfailsafe.failIfNoSpecifiedTests=true -DfailIfNoTests=true \
  test-compile failsafe:integration-test failsafe:verify
```

Preservar antes da rodada seguinte: console e código de saída, comando literal sem segredos, identidade nativa, hashes dos fontes/build, XML Failsafe, requests.jsonl e recibos específicos em target/a244-criteria, target/auditoria-http/b230-complementos.jsonl e target/criteria-c. Não reutilizar diretório de saída como se fosse recibo de nova rodada. O runner do coordenador deve proteger evidências de senhas/tokens e manter todos os asserts e resultados de falha.

SUP_ALO_PREPARO_SEPARADO mantém o esperado201 no aditamento de preparo. Se ele receber409 antes da reativação, registrar PREPARO_INTERROMPIDO, com a etapa e resposta; não converter para PASS da retomada. Nenhum caso foi desabilitado na fonte: a separação só determina o recibo focal.
