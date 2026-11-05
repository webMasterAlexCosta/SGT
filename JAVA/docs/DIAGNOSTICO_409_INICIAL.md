# Diagnóstico inicial — HTTP 409 na listagem nacional de contratos

Data documental: **01/11/2026 — America/Sao_Paulo**, conforme a referência local do cliente. Carimbos dos recibos/logs históricos permanecem em UTC de **02/11/2026**. Modalidade: investigação estática, somente leitura das fontes recebidas. Foram escritos relatórios e um teste diagnóstico separado, não aplicado. Nenhum teste, servidor, comando SQL ou purga foi executado por este investigador; baseline e candidata não foram editadas.

## 1. Resultado e limite da conclusão

O sintoma histórico está identificado: `ContratosCrudHttpIT.leituraEEscritaRespeitamTodosOsPerfisEEscopos` espera HTTP 200 em `GET /api/contratos` com o usuário nacional e recebe HTTP 409, com a mensagem **“Cadeia documental histórica incompleta ou divergente.”**

A leitura demonstra um mecanismo suficiente para provocar o sintoma: a listagem nacional percorre todos os contratos, enquanto um teste IND-08 grava e confirma uma cadeia documental deliberadamente inválida. Isso sustenta uma hipótese forte de interferência entre testes, mas **não identifica o primeiro contrato causal da execução histórica, não prova causalidade exclusiva e não demonstra que o CRUD passa isoladamente**. Nenhum resultado GREEN é declarado.

**A exigência MySQL `8.4.*` pertence à baseline imutável, não é um bloqueio técnico ainda pendente da candidata.** Na candidata, o `@BeforeEach` de `Ind08LeituraHistoricaCampoHttpIT.exigirPersistenciaRealSemDdlManual()` já exige igualdade exata com `8.0.46`, mantendo produto MySQL, `ddl-auto=update` e `sql.init.mode=never`; essa fonte foi conferida na atualização documental. A reprodução atual continua não executada por falta da autorização específica para privilégios da conta aplicativa. Falha de pré-condição da baseline não seria reprodução do 409.

## 2. Fontes e proveniência

Raiz Java consultada: `/workspace/scratch/eb5933b67e7b/sgt_remocao/baseline/backend`. Os caminhos de fonte abaixo são relativos a essa raiz.

Relatórios históricos recebidos: `/workspace/scratch/eb5933b67e7b/sgt_remocao/relatorios_origem/`.

- `RELATORIO_TESTES.md`, seções 1, 3 e 7: 494 unitários aprovados; 370 invocações de integração, 369 aprovadas e uma falha; `verify` com saída 1. Identifica a mesma falha na baseline anterior e no candidato anterior.
- `REVISAO_INDEPENDENTE.md`, seções 5 e 7.3: confirma a mesma asserção, status e mensagem; expressamente não prova primeiro ID causal nem aprovação isolada.
- `RELATORIO_MUDANCAS.md`: conserva a ressalva da falha nacional, sem homologação integral.

Os XMLs, logs completos, recibos e `final_java_selection.json` foram disponibilizados pela coordenação durante a investigação em `/workspace/scratch/eb5933b67e7b/sgt_remocao/evidencias_origem/`. A segunda leitura recontou os XMLs e conferiu a ordem real nos logs, conforme seção 10. O relatório histórico aponta `ContratosCrudHttpIT.java:100`; a mesma chamada está presente na fonte lida. Os resultados históricos continuam separados de qualquer reprodução nova em MySQL 8.0.46.

## 3. Teste exato e ordem interna

Arquivo: `src/test/java/com/example/siggtrat/auditoria20261029/ContratosCrudHttpIT.java`.

Método: `leituraEEscritaRespeitamTodosOsPerfisEEscopos()`.

A ordem explícita dentro do método é:

1. Criar `Cenario` com contratos A e B, Gestões e superintendências distintas.
2. Ler o contrato A por Local A, Estadual A, Nacional, Presidente, Administrador e Operacional A; esperar sucesso.
3. Rejeitar detalhe por leitores de B; rejeitar criação pelos perfis sem escrita; rejeitar listagem anônima.
4. Consultar a listagem Local A, depois Estadual A; exigir A presente e B ausente.
5. Consultar a listagem Nacional (`"consulta nacional"`); exigir HTTP 200 e presença de A e B. A falha histórica ocorre aqui.
6. Somente depois seriam consultadas opções, composições e excedentes.

O teste **não filtra a listagem nacional pelos IDs da própria fixture**. A asserção verifica inclusão dos seus contratos, mas a consulta global também alcança dados confirmados por outros testes e por rodadas anteriores no mesmo banco.

Não foi encontrada definição de `@TestMethodOrder`, `@TestClassOrder`, propriedades JUnit de ordenação ou `runOrder` no `pom.xml`/`src/test`. O POM fixa `forkCount=1`, `reuseForks=true`, paralelismo JUnit desativado e cache de contexto de tamanho 2. Isso serializa o trabalho, mas não determina por si a ordem das classes. Os logs recuperados comprovam IND-08 antes do CRUD nas duas rodadas; a seção 10 registra as linhas e posições. A lista `-Dit.test` é alfabética e difere da ordem efetiva, portanto não deve ser usada como substituta do log.

## 4. Caminho da consulta até a guarda histórica

| Etapa | Fonte e método | Comportamento observado no código |
|---|---|---|
| Entrada | `controlador/ContratoControlador.java`, `listarTodos()` | `GET /api/contratos` delega ao serviço. |
| Escopo | `servico/ContratoServico.java`, `listarTodos()` e `contratosNoEscopoAtual()` | Perfis globais/Presidente usam `ContratoRepositorio.findAllByOrderByNumeroContratoAsc()`. |
| Reconstrução anterior ao DTO | `ContratoServico.habilitacoesAdministrativasAtuais()` → `habilitacaoAdministrativaEm()` | Percorre a lista e reconstrói a habilitação atual de cada contrato antes de iniciar o `stream().map(paraDTO)`. |
| Origem histórica | `servico/MotorTemporalContratoServico.java`, `habilitacaoAdministrativaEm()` → `fotografiaOriginalConhecida()` | Busca a fotografia de publicação conhecida, incluindo prova histórica incorporada. |
| Validação | `servico/ConsultaProvaHistoricaContratoServico.java`, `original()` → `vigente()` | Valida cada cadeia de prova da origem de publicação. |
| Conversão, se a etapa anterior passar | `ContratoServico.paraDTO()` | Também projeta a versão contratual atual e consulta motores temporais, com novos caminhos para as mesmas provas. |

**Precisão em relação ao relatório anterior:** a recusa não precisa esperar a conversão de DTO. A reconstrução de habilitações já oferece um caminho direto para a guarda. Sem stack trace/ID da execução histórica, não se pode afirmar em qual desses caminhos ocorreu a primeira recusa daquela rodada.

`ConsultaProvaHistoricaContratoServico.vigente(evento, conhecido)` lê as provas por evento, com `registradoEm <= conhecido`, ordenadas por `numeroRevisao`. Exige:

- primeira revisão igual a 1 e sem antecessora;
- cada revisão seguinte igual à anterior mais 1;
- `provaAnterior.id` igual ao ID da linha anterior.

Qualquer violação lança a mensagem exata com HTTP 409. Uma cadeia vazia devolve `Optional.empty()`; portanto mera ausência total de prova incorporada não produz, por si só, essa mensagem. O repositório usa `PrecisaoTemporal.limiteInclusivoMicros()` para o corte de conhecimento, o que também precisa ser preservado na reprodução.

Fontes: `repositorio/ProvaHistoricaContratoRepositorio.java`; `utilitario/PrecisaoTemporal.java`; `ConsultaProvaHistoricaContratoServico.java:61–80`; `ContratoServico.java:174–193,246–266,1105–1115,1367–1400`; `MotorTemporalContratoServico.java:425–442,610–624`.

## 5. Fixture inválida conhecida e duração dos dados

Arquivo: `src/test/java/com/example/siggtrat/auditoria20261029/Ind08LeituraHistoricaCampoHttpIT.java`.

Método: `leituraDeCadeiaIncompletaRecusaInterpretacaoEmVezDeCertificarUltimaLinha()`.

Sequência do método e auxiliares:

1. `origem(false)` chama `criarCenarioPersistido()` e cria outro contrato por `ContratosHttpBase.publicado(...)`, com rascunho, composição, prévia da publicação retroativa e publicação via camada MVC.
2. Recupera a fotografia original e seu evento de publicação.
3. Concede `INCORPORACAO_PROVA_HISTORICA` e incorpora uma fonte sintética por prévia + comando canônico. A incorporação normal cria a primeira prova com revisão 1.
4. Executa, dentro de `emTransacao`, o JPQL `update ProvaHistoricaContrato p set p.numeroRevisao=3 where p.id=:id`.
5. Exige HTTP 409 na consulta `/{id}/provas-historicas` e em `/{id}/versoes`.

O método é um controle negativo de integridade. **O contrato é publicado pelo fluxo de aplicação; a invalidade documental é introduzida posteriormente por DML da fixture.** Não há evidência nessa leitura de que a publicação canônica tenha criado a lacuna.

A classe declara `@Transactional(propagation = Propagation.NOT_SUPPORTED)`. `AuditoriaHttpSuporte.emTransacao()` usa `TransactionTemplate`; sem transação externa, essas operações confirmam os dados. `criarCenarioPersistido()` é exatamente `emTransacao(this::criarCenario)`. O único `@AfterEach` compartilhado limpa o `SecurityContextHolder`; não desfaz os dados. Não foi encontrada restauração da revisão no teste negativo.

Em contraste, `ContratosCrudHttpIT` herda `@Transactional` de `AuditoriaHttpSuporte` e não a substitui. Sua própria fixture participa dessa transação de teste, mas ela pode ler linhas previamente confirmadas por outras classes no mesmo schema. `ddl-auto=update` não limpa dados entre execuções.

`AuditoriaHttpSuporte.criarCenario()` gera dois contratos `A-<sufixo>` e `B-<sufixo>`, com versões originais sintéticas número 1, sem `ProvaHistoricaContrato`. `ContratosHttpBase.publicado()` gera identificador `HTTP-<sufixo>`. Os sufixos UUID e a ordenação por número impedem inferir o primeiro ID causal apenas pelo nome de classe.

Na busca focal por `ProvaHistoricaContrato`, `numeroRevisao`, `setNumeroRevisao` e `provaAnterior` nos testes, esse foi o mutador explícito de revisão inválida localizado. Isso **não é prova de exclusividade**: exige-se inventário do banco no momento da falha e observação da cadeia que efetivamente chega à guarda.

## 6. Gravação canônica e razão para não afrouxar a validação

`ProvaHistoricaContratoServico.incorporar()` obtém contexto com bloqueio de contrato, valida a prova anterior declarada contra `consulta.vigente()`, confere a versão anterior e o token da prévia, registra a permissão, cria a fotografia e grava uma nova prova com `anterior == null ? 1 : anterior.numeroRevisao + 1` e instante em microssegundos. O reenvio idempotente depende da mesma intenção.

`entidade/ProvaHistoricaContrato.java` contém unicidade por evento/revisão, vínculo único da antecessora, vínculo único da versão gerada e campos `updatable=false`. Esses mecanismos não impedem o JPQL deliberado da fixture nem substituem a validação da sequência completa. A leitura estática não demonstra ausência de todos os defeitos possíveis de concorrência, corte temporal ou persistência; apenas identifica proteções presentes.

GOV-003 e a disciplina de fotografias exigem preservar os fatos publicados. Não é correção admissível limpar o banco compartilhado, apagar a prova inválida para esconder o resultado, silenciar a exceção, aceitar revisão 3 como primeira revisão ou substituir lacunas pelo cadastro atual. Também não se deve tratar um retorno parcial silencioso da lista como atendimento automático a CON-009; seria alteração de contrato funcional a avaliar separadamente.

## 7. Versão do MySQL e comando histórico

O comando histórico documentado corresponde a:

```bash
mvn -B -ntp -T1 -s <settings-local> \
  -Dmaven.repo.local=<cache-maven-local> \
  '-Dtest.jvm.args=-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2' \
  -o -Pintegration -Dit.test=<86-classes-de-final_java_selection.json> verify
```

A expansão integral foi recuperada no campo `command` de `evidencias_origem/work/evidencias/remocao-java-integracao-final/recibo.json`. Seu seletor tem exatamente 86 classes e coincide, inclusive na ordem textual, com `evidencias_origem/evidence/final_java_selection.json:selected`. Todos os seletores têm XML e não há XML adicional nessa comparação. O comando literal usa Maven 3.9.16 por `bash`, em `/workspace/scratch/7b86d83c3e7e/sgt_finance_removal/validation/runtime/bin/apache-maven-3.9.16/bin/mvn`, settings em `.../validation/infra/maven-settings.xml` e cache em `.../work/reproducao_offline/m2/repository`. Esses são caminhos históricos; os equivalentes desta missão devem ser registrados no recibo novo. O JSON original preserva o comando completo, sem inventar expansão.

`AuditoriaHttpSuporte` aceita banco externo por `INTEGRATION_MYSQL_URL/USER/PASSWORD` ou propriedades `integration.mysql.*`; sem URL, inicializa container `mysql:8.4`. Define `spring.jpa.hibernate.ddl-auto=update` e `hibernate.hbm2ddl.halt_on_error=true`. O POM fixa fuso do fork em `America/Sao_Paulo`; `application-test.properties` usa `spring.sql.init.mode=never`.

Na **baseline**, `Ind08LeituraHistoricaCampoHttpIT.exigirPersistenciaRealSemDdlManual()` exige produto MySQL, versão começando com `8.4.`, `ddl-auto=update` e `sql.init.mode=never`. A cópia original da baseline, se executada sem adaptação em MySQL 8.0.46, falharia antes do corpo. O CRUD não possui esse guard específico. Na **candidata**, a adaptação já foi realizada: `getDatabaseProductVersion()).isEqualTo("8.0.46")`, com as demais restrições preservadas. Portanto, não resta adaptar esse guard para a reprodução candidata; resta obter o acesso autorizado e executar o cenário.

**Distinção entre árvores:** os caminhos e o mecanismo histórico deste diagnóstico referem-se à baseline, preservada. Na atualização da matriz operacional, foi conferida por leitura a candidata em `/workspace/scratch/eb5933b67e7b/sgt_remocao/candidata/backend/src/test/java/com/example/siggtrat/auditoria20261029/Ind08LeituraHistoricaCampoHttpIT.java`, método `exigirPersistenciaRealSemDdlManual`, com igualdade exata `8.0.46`. Essa adaptação de fonte não é execução da integração e não altera a evidência histórica de MySQL 8.4.11.

## 8. Plano mínimo de reprodução controlada

Executar serialmente, em cópia de execução, preservando a baseline e a candidata. Usar instância/schema sintético dedicado provisionado pela coordenação, sem limpeza por DML/DDL e sem tocar dados do usuário. Hibernate é o único gestor das tabelas. Registrar MySQL real 8.0.46, hash da fonte, configuração efetiva, estado inicial dos dados, relógio/fuso e comando exato, sem credenciais.

O seletor exato do teste observado é:

```text
com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos
```

Modelo de comando focal, com settings/cache privados já preparados pela coordenação:

```bash
MAVEN_OPTS='-Xmx512m' mvn -B -ntp -T1 -o -Pintegration \
  '-Dtest.jvm.args=-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2' \
  '-Dit.test=com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos' \
  test-compile failsafe:integration-test failsafe:verify
```

Esse comando é proposta diagnóstica, **não executada**. Os goals diretos evitam repetir toda a suíte unitária entre ensaios; não substituem o `verify` integral do gate final. Conservar os XMLs/logs de cada comando antes de reutilizar `target`, junto de `target/auditoria-http/requests.jsonl` e identificação da rodada.

| Etapa | Execução | Evidência que resolve |
|---|---|---|
| A | Método CRUD isolado em banco dedicado inicialmente vazio, preparado por Hibernate. | Se falhar aqui, há causa independente de sequência IND-08. Observar primeiro contrato/evento/prova e o caminho de chamada. |
| B | Usando a fonte candidata com guard de versão já adaptado e rastreado, executar somente `Ind08LeituraHistoricaCampoHttpIT#leituraDeCadeiaIncompletaRecusaInterpretacaoEmVezDeCertificarUltimaLinha` no mesmo banco. | Confirmar ambos os 409 intencionais e identificar os IDs gravados; comprovar em nova transação a revisão 3 e antecessora nula. |
| C | Repetir exatamente o método CRUD, em novo comando, no mesmo banco de B. | Ordem garantida entre processos, sem depender da ordenação do runner. Comparar o primeiro ID recusado com B. |
| D | Em outro ambiente dedicado inicialmente vazio, executar a classe CRUD inteira e posteriormente repetir o par B→C. | Separar efeito interno da classe, estado do schema e interferência entre classes. Somente ampliar se A–C deixarem risco concreto. |
| E | Depois da causa confirmada e eventual correção, rodar a seleção operacional e a regressão necessária com recibos novos. | Não converter o focal em aprovação integral. |

Na falha, observar via diagnóstico de teste ou debugger, sem expor dados pessoais: `contratoId`, `numeroContrato`, `eventoOrigemId`, `provaId`, `numeroRevisao`, `provaAnteriorId`, `registradoEm`, corte de conhecimento, posição na lista e stack trace. Ler novamente em transação independente. Se o erro ocorrer antes do DTO, capturar a iteração de `habilitacoesAdministrativasAtuais`. Inventariar todas as cadeias inválidas, porque identificar uma não exclui outras.

## 9. Decisões ainda abertas

- CRUD isolado passa em MySQL 8.0.46? Não executado.
- A cadeia da fixture IND-08 é a primeira causal da rodada histórica? Não identificado.
- A ordem histórica colocou IND-08 antes do CRUD? Sim, comprovado pelos dois logs recuperados. O banco também poderia conter cadeias anteriores: o histórico documenta reutilização de schema; a leitura dos logs não exclui essa contribuição.
- Existe outra origem de cadeia inválida ou problema de corte/persistência? Não excluído.
- Nenhuma mudança do contrato de listagem é proposta: não se exige HTTP 200 enquanto uma cadeia inválida permanece visível, nem se relaxa a guarda histórica. A regressão a demonstrar é a fuga de uma corrupção sintética entre testes.

A investigação permanece na fase de caracterização. Aguarda-se a reprodução controlada para qualquer correção de código ou conclusão causal.

## 10. Complemento — recibos e logs históricos recuperados e conferidos

Raiz dos caminhos desta seção: `/workspace/scratch/eb5933b67e7b/sgt_remocao/evidencias_origem/`.

### Recontagem independente dos XMLs

| Rodada | Grupo | Arquivos XML | Casos | Falhas | Erros | Ignorados | Exit do recibo |
|---|---|---:|---:|---:|---:|---:|---:|
| `remocao-baseline-java-repeticao-schema` | Surefire | 112 | 494 | 0 | 0 | 0 | 1 |
| Mesma | Failsafe | 84 | 390 | 1 | 0 | 0 | 1 |
| `remocao-java-integracao-final` | Surefire | 112 | 494 | 0 | 0 | 0 | 1 |
| Mesma | Failsafe | 86 | 370 | 1 | 0 | 0 | 1 |

As somas vieram dos atributos `tests`, `failures`, `errors` e `skipped` de `work/evidencias/<rodada>/xml/{surefire-reports,failsafe-reports}/TEST-*.xml`, sem somar reexecuções como casos novos. Nos dois recibos `mysql_real=true` e `source_unchanged=true`; isso é declaração vinculada àquela execução, não verificação de MySQL 8.0.46 atual.

| Rodada | Início UTC | Fim UTC | SHA-256 de manifesto declarado |
|---|---|---|---|
| Baseline repetida | 2026-11-02 00:18:06 | 2026-11-02 00:33:00 | `a846fb74193318670f70ee1d6e69e82fa70df0a93d09ff10691634c95aa3070f` |
| Final | 2026-11-02 01:02:41 | 2026-11-02 01:17:14 | `3c84eea5d98b5bd9c824f8c11768ee76afd7a4ed4a7520139436b5c2e965280a` |

### Ordem efetiva relevante

No `execucao.log` da baseline, IND-08 inicia na linha 4646 e é a 55ª classe IT registrada; CRUD inicia na linha 5723 e é a 67ª. No final, IND-08 inicia na linha 4770 e é a 56ª IT; CRUD inicia na linha 5977 e é a 69ª. A contagem considera as linhas `Running` de integração, incluindo as classes com nome de exibição “Integração MySQL”.

A sequência entre as duas classes é a mesma nos dois logs, com uma adição no final:

1. `Ind08LeituraHistoricaCampoHttpIT`.
2. `ProvaHistoricaComposicaoAditamentoHttpIT`.
3. `PessoasVinculosContratoHttpIT`.
4. `ProvaHistoricaTransferenciaConvergenteRotulosHttpIT`.
5. `EmissaoRetificacaoConcorrenciaAuditoriaHttpIT`.
6. `Ind08AcervoDerivadoMascaraHttpIT`.
7. `ProvaHistoricaContratoHttpIT`.
8. `RelatoriosConsultasHttpIT`.
9. `ProvaHistoricaTransferenciaRevisaoParcialHttpIT`.
10. `RevisaoDownloadExpiracaoHttpIT`.
11. `RevisaoDownloadsAutorizacaoAtualHttpIT`.
12. `ContratosOpcoesTemporalHttpIT`.
13. Apenas na rodada final: `PreservacaoFrequenciaSemFinanceiroHttpIT`.
14. `ContratosCrudHttpIT`.

Nos XMLs de IND-08, o primeiro `testcase` é `leituraDeCadeiaIncompletaRecusaInterpretacaoEmVezDeCertificarUltimaLinha`, aprovado em ambas as rodadas. Os seis casos dessa classe passam. Nos XMLs de CRUD, o segundo `testcase` é `leituraEEscritaRespeitamTodosOsPerfisEEscopos`, a única falha. Os outros quatro passam. Assim, o controle negativo que grava revisão 3 não foi ignorado e precedeu a falha nacional em ambas as execuções.

### Requisição e limite de identificação

O dump MockMvc registra `GET /api/contratos`, sem parâmetros e sem corpo, handler `ContratoControlador`, exceção resolvida `ResponseStatusException` e HTTP 409. A resposta da baseline é datada `2026-11-02T00:30:18.592792631Z` (log linha 6408); a final, `2026-11-02T01:14:38.899481318Z` (linha 6662). As asserções e seus stacks estão nas linhas 6449–6458 e 6703–6712, respectivamente. Os stacks são da asserção de teste e não expõem o ID interno da guarda.

As respostas 200 local/estadual imediatamente anteriores mostram o contrato saudável de cada fixture: ID 914 na baseline e ID 1787 no final. **Eles não são identificados como o contrato causal do 409.** O corpo de erro nacional não traz contrato, evento ou prova. O `testcase` negativo IND-08 não tem `system-out`/`system-err` que revele esses IDs. Não foram localizados `requests.jsonl`/`contratos-exemplos.jsonl` no material recuperado que permitam completar esse elo. Portanto, ordem e coexistência potencial estão comprovadas; primeiro ID causal e exclusividade continuam pendentes.

Hashes dos logs efetivamente lidos:

- Baseline: `c080ffe828241d615aef685cff890ead7312ccd23221a69c3f6644c2768d5617`.
- Final: `6fc941e23b3969f85c0f35798b404b2476df0b9aeb02e599fe8b4ea225d4bcfb`.

`evidence/BASELINE_EXECUTADA_E_BLOQUEIOS.md`, também recuperado, documenta execução anterior com falha no bootstrap Hibernate e repetição no mesmo schema. Esse histórico aumenta a necessidade de distinguir dados da execução em curso de dados já existentes, sem autorizar limpeza dos fatos.

## 11. Artefato de caracterização preparado, não aplicado nem executado

Arquivo separado: `/workspace/scratch/eb5933b67e7b/sgt_remocao/diagnostico/DiagnosticoContaminacaoContratosNacionalIT.java`.

O teste proposto exige MySQL exatamente 8.0.46 real (`isEqualTo("8.0.46")`, guard ajustado por revisão documental), `ddl-auto=update`, `sql.init.mode=never` e ambiente inicialmente sem contratos. Cria publicação e prova válidas pelo fluxo de domínio, confirma 200 nacional, altera somente a revisão da própria prova sintética de 1 para 3 e confirma 409 individual e global. Registra IDs de contrato, evento, prova e primeira recusa da reconstrução de habilitação.

Um `finally` restaura exclusivamente esse campo sintético de 3 para 1, restrito a ID da prova, contrato, evento e antecessora nula. Após leitura em nova transação, exige 200 nacional novamente. Nenhum contrato, prova, fotografia ou evento é apagado. Essa restauração desfaz a corrupção artificial introduzida pelo próprio diagnóstico e não é retificação de um fato negocial real. A versão entregue não exige sucesso enquanto a cadeia está inválida.

O resultado esperado da caracterização é **200 → 409 → 200**, não uma correção da regra de negócio. O teste ainda não foi compilado nem executado, não deve ser contado como evidência de aprovação e não foi adicionado à candidata. O RED de isolamento deve executar o método negativo IND-08 real e depois um observador/CRUD em banco dedicado, registrando que a corrupção do teste anterior vazou para o seguinte. A correção desse isolamento só deve ocorrer depois dessa reprodução, preservando o 409 legítimo das consultas históricas.
