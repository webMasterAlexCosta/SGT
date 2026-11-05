# Relatório de regressão

**Evidência da versão entregue. Nenhuma aprovação de versão anterior foi importada.**

Candidata: `candidata07`. Manifesto de fontes SHA-256: `fc92555e0785d29ed9b8f2dd3c86235c7eec03992537f27b7f0bc8f5d3526f25`.

## Execução conclusiva da árvore atual

| Fase | Executados | Aprovados | Falhas | Erros | Ignorados |
|---|---|---|---|---|---|
| Unitários da regressão | 493 | 493 | 0 | 0 | 0 |
| Integração da regressão | 1114 | 1114 | 0 | 0 | 0 |
| Carga MySQL separada | 1 | 1 | 0 | 0 | 0 |

Foram observadas 1608 invocações distintas na regressão integral e carga. As duas rodadas executaram 2101 invocações ao todo; 493 unitários da carga repetem exatamente o conjunto da integral e não são somados novamente como distintos. Métodos parametrizados contam cada caso efetivamente presente no XML.

Regressão: exit code 0; 0 divergências log/XML; 0 arquivos de fonte alterados durante a execução.

[evidencias/rodadas/regressao_integral_06/RESUMO.json](../evidencias/rodadas/regressao_integral_06/RESUMO.json); [evidencias/rodadas/regressao_integral_06/RESULTADOS_METODOS.json](../evidencias/rodadas/regressao_integral_06/RESULTADOS_METODOS.json).

O arquivador exige XML novo, cruza contadores com o log e preserva invocação, estado e hash. JSONLs suplementares são segregados pelo prefixo existente antes da rodada. A cobertura usa um arquivo JaCoCo exclusivo por rodada.

## Verificações adicionais e escopo do build

A compilação javac e as verificações de arquitetura presentes na suíte fazem parte da regressão. A sintaxe dos roteiros Python e shell é conferida separadamente, com hashes. O POM não configura Checkstyle, PMD ou SpotBugs. O perfil PIT de mutation testing é opt-in, fora do verify, e não foi executado nesta validação; nenhum escore de mutação é declarado.

## Limites e correções do processo de prova

A baseline unitária executou 488 casos, com 486 aprovados e duas falhas. A integração inicial foi interrompida (496 casos arquivados: 470 aprovados, 23 falhas e três erros), sem aprovação integral. O histórico conserva REDs funcionais e também falhas de fixture, compilação e infraestrutura; essas categorias não são confundidas.

`regressao_integral_02` não executou testes: Maven clean não conseguiu excluir a saída anterior de cobertura. A pasta de build foi preservada por renomeação. `regressao_integral_03` executou 492 unitários, com uma falha de oráculo: um aditamento apenas quantitativo preservou nomes comprovados que o teste exigia apagar. A candidata seguinte manteve esse cenário com os nomes originais exatos e acrescentou a variante de origem realmente ausente. A produção não foi alterada por essa falha.

`regressao_integral_05` executou 1.595 casos, com 1.593 aprovações e duas falhas de preparação temporal: `PessoasRecontratacaoAposInativacaoHttpIT` e `FrequenciaFotografiaIdentidadeHttpIT` enviaram FALTA na data corrente, já sábado em America/Sao_Paulo, contra uma jornada de segunda a sexta. A regra produtiva recusou corretamente. Os cenários passaram a demonstrar datas úteis e faixas persistidas, com sete referências de calendário cada, mantendo as expectativas de identidade e fotografia. Essas alterações de testes receberam novo manifesto e nova regressão integral; a rodada reprovada foi preservada.

A primeira tentativa TCP posterior à integral foi reprovada porque o roteiro exigiu FECHADA de uma frequência da véspera, ainda dentro dos sete dias. A norma proíbe fechamento antecipado; a releitura MySQL após a parada confirmou ABERTA, INVALIDA_COBERTURA e bloqueio de edição. O roteiro corrigido estabelece e relê duas pré-condições reais, ABERTA e FECHADA por prazo, preservando conteúdo e eventos de ambas após anulação. A candidata07 altera somente esse roteiro: os 1.045 arquivos de compilação, as 776 classes e o JAR permanecem byte a byte idênticos aos exercitados na integral06/carga01. Todos os quatro roteiros TCP foram executados novamente; a tentativa parcial não confere aceite.

[evidencias/alteracoes/anulacao_tcp_01/APLICACAO_E_PRESERVACAO.json](../evidencias/alteracoes/anulacao_tcp_01/APLICACAO_E_PRESERVACAO.json); [evidencias/jar_tentativa01_reprovada/DIAGNOSTICO_FREQUENCIA_ABERTA.json](../evidencias/jar_tentativa01_reprovada/DIAGNOSTICO_FREQUENCIA_ABERTA.json)

A primeira conciliação de distribuição recusou a carga porque o empacotador procurava o nome Java em testcase@classname, onde o JUnit gravou o @DisplayName. O arquivo e testsuite@name identificavam corretamente DatabaseLoadIT. O leitor corrigido confere a suíte, o nome exato do arquivo e a invocação, mantendo todos os campos brutos, XMLs, estados e hashes originais. Controles negativos permanentes, revisão do autor e leitura cruzada independente verificam o adendo do empacotador. Nenhum Java, teste Java, JAR ou roteiro HTTP executado mudou por esse reparo.

[evidencias/controle_roteiros/identidade_carga_01/ADENDO_APROVADO.json](../evidencias/controle_roteiros/identidade_carga_01/ADENDO_APROVADO.json)

A observação concorrente de CPF precisou identificar o proprietário pelo ID da transação InnoDB, e respeitar a janela de atualização de INNODB_TRX. As primeiras falhas desse observador não demonstraram violação de unicidade: foram preservadas como falhas da prova. O cenário final exige espera real, transações diferentes, índice único, um vencedor e rollback da perdedora.

## Histórico de rodadas preservadas

| Rodada | Início UTC | Casos | Passaram | Falhas | Erros | Ignorados | Exit | Uso |
|---|---|---|---|---|---|---|---|---|
| carga_final_01 | 2026-10-31T04:13:54.590959+00:00 | 494 | 494 | 0 | 0 | 0 | 0 | Atual |
| focal_calendario_revisao06 | 2026-10-31T03:42:59.191467+00:00 | 15 | 15 | 0 | 0 | 0 | 0 | Histórico; sem aceite da versão final |
| focal_catalogos_cpf_01 | 2026-10-31T02:11:34.105418+00:00 | 44 | 44 | 0 | 0 | 0 | 0 | Histórico; sem aceite da versão final |
| focal_configuracao_01 | 2026-10-31T00:40:09.168909+00:00 | 516 | 516 | 0 | 0 | 0 | 0 | Histórico; sem aceite da versão final |
| focal_controles_finais_01 | 2026-10-31T01:41:53.208144+00:00 | 26 | 26 | 0 | 0 | 0 | 0 | Histórico; sem aceite da versão final |
| focal_corrigido_01 | 2026-10-30T23:02:46.192184+00:00 | 165 | 114 | 51 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| focal_corrigido_02 | 2026-10-31T00:28:16.060569+00:00 | 745 | 742 | 3 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| focal_cpf_01 | 2026-10-31T01:50:26.581847+00:00 | 2 | 1 | 1 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| focal_cpf_02 | 2026-10-31T01:59:26.447669+00:00 | 2 | 1 | 1 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| focal_fim_natural_01 | 2026-10-31T01:03:37.995193+00:00 | 44 | 42 | 2 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| focal_lacunas_01 | 2026-10-31T00:55:11.241566+00:00 | 8 | 7 | 1 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| focal_pendencia_fim_01 | 2026-10-31T01:14:38.011184+00:00 | 41 | 41 | 0 | 0 | 0 | 0 | Histórico; sem aceite da versão final |
| focal_resolucao_fim_01 | 2026-10-31T01:29:34.002786+00:00 | 16 | 16 | 0 | 0 | 0 | 0 | Histórico; sem aceite da versão final |
| focal_revisao_integral_05 | 2026-10-31T03:01:55.958241+00:00 | 69 | 69 | 0 | 0 | 0 | 0 | Histórico; sem aceite da versão final |
| novas_reproducoes_01 | 2026-10-30T22:55:06.358952+00:00 | 8 | 2 | 6 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_02 | 2026-10-30T23:17:16.733984+00:00 | 4 | 1 | 3 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_03 | 2026-10-31T00:00:54.106641+00:00 | 0 | 0 | 0 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_04 | 2026-10-31T00:01:42.864044+00:00 | 11 | 5 | 6 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_05 | 2026-10-31T00:20:14.733418+00:00 | 3 | 2 | 1 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_06 | 2026-10-31T00:47:36.344073+00:00 | 8 | 1 | 7 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_07 | 2026-10-31T00:50:48.261661+00:00 | 10 | 9 | 1 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_08 | 2026-10-31T01:01:05.269970+00:00 | 9 | 8 | 1 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_09 | 2026-10-31T01:08:33.494109+00:00 | 21 | 19 | 2 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_10 | 2026-10-31T01:11:10.355429+00:00 | 3 | 0 | 3 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_11 | 2026-10-31T01:20:32.321619+00:00 | 2 | 0 | 2 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_12 | 2026-10-31T01:27:02.424964+00:00 | 2 | 0 | 2 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_13 | 2026-10-31T01:36:44.070283+00:00 | 5 | 4 | 1 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_14 | 2026-10-31T02:06:42.036391+00:00 | 7 | 2 | 5 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| novos_red_15 | 2026-10-31T02:57:38.939236+00:00 | 1 | 0 | 0 | 1 | 0 | 1 | Histórico; sem aceite da versão final |
| preparacao_http_final06 | 2026-10-31T04:18:47.029389+00:00 | 1 | 1 | 0 | 0 | 0 | 0 | Histórico; sem aceite da versão final |
| preparacao_http_final07 | 2026-10-31T04:32:12.040536+00:00 | 1 | 1 | 0 | 0 | 0 | 0 | Histórico; sem aceite da versão final |
| regressao_integral_01 | 2026-10-30T23:23:50.682096+00:00 | 1502 | 1237 | 76 | 189 | 0 | 1 | Histórico; sem aceite da versão final |
| regressao_integral_02 | 2026-10-31T02:15:11.317432+00:00 | 0 | 0 | 0 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| regressao_integral_03 | 2026-10-31T02:17:27.936296+00:00 | 492 | 491 | 1 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| regressao_integral_04 | 2026-10-31T02:22:44.582195+00:00 | 1593 | 1583 | 9 | 1 | 0 | 1 | Histórico; sem aceite da versão final |
| regressao_integral_05 | 2026-10-31T03:06:36.334890+00:00 | 1595 | 1593 | 2 | 0 | 0 | 1 | Histórico; sem aceite da versão final |
| regressao_integral_06 | 2026-10-31T03:46:08.289409+00:00 | 1607 | 1607 | 0 | 0 | 0 | 0 | Atual |
| unitarios_corrigidos_02 | 2026-10-30T22:51:55.915697+00:00 | 492 | 492 | 0 | 0 | 0 | 0 | Histórico; sem aceite da versão final |
| unitarios_corrigidos_03 | 2026-10-30T23:21:50.115295+00:00 | 492 | 491 | 0 | 1 | 0 | 1 | Histórico; sem aceite da versão final |

## Resultados por classe da regressão final

| Classe | Casos | Passaram | Falhas | Erros | Ignorados |
|---|---|---|---|---|---|
| ControllerLayerArchitectureTest | 5 | 5 | 0 | 0 | 0 |
| Integração MySQL: TokenAtivoRepositorio | 3 | 3 | 0 | 0 | 0 |
| Integração MySQL: chamadas diretas de banco ainda existentes em services | 3 | 3 | 0 | 0 | 0 |
| Integração MySQL: contrato do schema Hibernate | 2 | 2 | 0 | 0 | 0 |
| Integração MySQL: disponibilidade e pool de conexões | 3 | 3 | 0 | 0 | 0 |
| Integração MySQL: fechamento de persistência da Etapa 12 | 3 | 3 | 0 | 0 | 0 |
| Integração MySQL: integridade transacional e concorrência | 5 | 5 | 0 | 0 | 0 |
| Integração MySQL: persistência da Etapa 11 | 1 | 1 | 0 | 0 | 0 |
| Integração MySQL: smoke de todas as queries de repositorio | 2 | 2 | 0 | 0 | 0 |
| EmpregadoMatriculaOptionalConfigurationTest | 1 | 1 | 0 | 0 | 0 |
| HibernateSchemaConfigurationTest | 1 | 1 | 0 | 0 | 0 |
| AditamentosRemanejamentosHttpIT | 7 | 7 | 0 | 0 | 0 |
| AdministracaoContratoHttpIT | 7 | 7 | 0 | 0 | 0 |
| AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT | 9 | 9 | 0 | 0 | 0 |
| AlocacaoRetroativaRotuloAuditoriaIndependenteHttpIT | 6 | 6 | 0 | 0 | 0 |
| AuditoriaCancelamentoRemanejamentoReplayHttpIT | 5 | 5 | 0 | 0 | 0 |
| AuditoriaDadosRestritosNovaAuditoriaHttpIT | 4 | 4 | 0 | 0 | 0 |
| AuditoriaFinalCorrecaoCpfControlesHttpIT | 2 | 2 | 0 | 0 | 0 |
| AuditoriaFinalFimNaturalProrrogacaoHttpIT | 2 | 2 | 0 | 0 | 0 |
| AuditoriaFinalMetadadosEmissaoMinimizacaoHttpIT | 4 | 4 | 0 | 0 | 0 |
| AuditoriaFinalPendenciaFimControlesHttpIT | 2 | 2 | 0 | 0 | 0 |
| AuditoriaFinalPendenciaFimNaturalProrrogacaoHttpIT | 3 | 3 | 0 | 0 | 0 |
| AuditoriaFinalPessoasLacunasEspecificasHttpIT | 3 | 3 | 0 | 0 | 0 |
| AuditoriaFinalResolucaoPendenciaFimSuperadoHttpIT | 2 | 2 | 0 | 0 | 0 |
| AuditoriaIndependenteReplayVigenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| AuditoriaInstrumentosConcorrentesHttpIT | 3 | 3 | 0 | 0 | 0 |
| AuditoriaTransversalHttpIT | 1 | 1 | 0 | 0 | 0 |
| AutenticacaoPerfilContratoHttpIT | 8 | 8 | 0 | 0 | 0 |
| AutenticacaoTodasRotasHttpIT | 1 | 1 | 0 | 0 | 0 |
| CacheEscopoTemporalHttpIT | 3 | 3 | 0 | 0 | 0 |
| CalendarioSemContratoConcorrenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| CalendarioVersionamentoFontesHttpIT | 4 | 4 | 0 | 0 | 0 |
| CancelamentoCapacidadeContratualHttpIT | 3 | 3 | 0 | 0 | 0 |
| CatalogoPublicacaoConcorrenteAuditoriaIndependenteHttpIT | 12 | 12 | 0 | 0 | 0 |
| CatalogosContratoHttpIT | 8 | 8 | 0 | 0 | 0 |
| CatalogosPessoasConclusaoHttpIT | 7 | 7 | 0 | 0 | 0 |
| CatalogosTemporalHttpIT | 7 | 7 | 0 | 0 | 0 |
| CatalogosTransferenciaLacunasHttpIT | 11 | 11 | 0 | 0 | 0 |
| CatalogosTransicoesOperacionaisHttpIT | 25 | 25 | 0 | 0 | 0 |
| CatalogosVersaoCanonicaAuditoriaIndependenteHttpIT | 5 | 5 | 0 | 0 | 0 |
| CicloVidaContratosHttpIT | 5 | 5 | 0 | 0 | 0 |
| ComposicaoFotografiaRascunhoHttpIT | 1 | 1 | 0 | 0 | 0 |
| ComposicaoPublicacaoConcorrenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| ConclusaoAditamentoRetificacaoQuantitativaHttpIT | 2 | 2 | 0 | 0 | 0 |
| ConclusaoAlcancePreviaContratoHttpIT | 2 | 2 | 0 | 0 | 0 |
| ConclusaoApiCanonicaHttpIT | 4 | 4 | 0 | 0 | 0 |
| ConclusaoAud01PendenciasHttpIT | 10 | 10 | 0 | 0 | 0 |
| ConclusaoAud01RecorrenciaPendenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| ConclusaoAud02DesligamentoHttpIT | 19 | 19 | 0 | 0 | 0 |
| ConclusaoAud03VinculosTransferenciaProgramadaHttpIT | 7 | 7 | 0 | 0 | 0 |
| ConclusaoAud04IdentidadeContratualHttpIT | 8 | 8 | 0 | 0 | 0 |
| ConclusaoAud05PainelCapacidadesHttpIT | 7 | 7 | 0 | 0 | 0 |
| ConclusaoAud06FrequenciaEscopoPaginaHttpIT | 7 | 7 | 0 | 0 | 0 |
| ConclusaoCacheRedisRealHttpIT | 3 | 3 | 0 | 0 | 0 |
| ConclusaoCancelamentoFuturoHttpIT | 1 | 1 | 0 | 0 | 0 |
| ConclusaoCorrecaoAnulacaoOperacaoHttpIT | 1 | 1 | 0 | 0 | 0 |
| ConclusaoEfetivoContratoHttpIT | 4 | 4 | 0 | 0 | 0 |
| ConclusaoFotografiaContratoHttpIT | 4 | 4 | 0 | 0 | 0 |
| ConclusaoFrequenciaIdempotenciaOperacionalHttpIT | 7 | 7 | 0 | 0 | 0 |
| ConclusaoHabilitacaoSemOrigemHttpIT | 5 | 5 | 0 | 0 | 0 |
| ConclusaoPessoasCriteriosFronteirasHttpIT | 17 | 17 | 0 | 0 | 0 |
| ConclusaoPublicacaoCamposObrigatoriosHttpIT | 10 | 10 | 0 | 0 | 0 |
| ConclusaoPublicacaoPreviaHttpIT | 3 | 3 | 0 | 0 | 0 |
| ConclusaoRelatoriosConhecimentoHttpIT | 2 | 2 | 0 | 0 | 0 |
| ConclusaoRelatoriosOperacionaisHttpIT | 2 | 2 | 0 | 0 | 0 |
| ConclusaoRetificacaoAdministrativaHttpIT | 2 | 2 | 0 | 0 | 0 |
| ConclusaoTransferenciaAlocacaoHttpIT | 5 | 5 | 0 | 0 | 0 |
| ConclusaoTransferenciaContratoHttpIT | 8 | 8 | 0 | 0 | 0 |
| ConclusaoTransferenciaPreviaHttpIT | 5 | 5 | 0 | 0 | 0 |
| ContratoAnulacaoFuturaHttpIT | 4 | 4 | 0 | 0 | 0 |
| ContratoCatalogoFotografiaHttpIT | 1 | 1 | 0 | 0 | 0 |
| ContratoConfiguracaoLegadaHttpIT | 6 | 6 | 0 | 0 | 0 |
| ContratosCatalogosEmpresaHttpIT | 5 | 5 | 0 | 0 | 0 |
| ContratosCompletudeHttpIT | 3 | 3 | 0 | 0 | 0 |
| ContratosCriteriosComplementaresHttpIT | 28 | 28 | 0 | 0 | 0 |
| ContratosCriteriosEspecificosHttpIT | 11 | 11 | 0 | 0 | 0 |
| ContratosCrudHttpIT | 5 | 5 | 0 | 0 | 0 |
| ContratosEscopoHttpIT | 2 | 2 | 0 | 0 | 0 |
| ContratosFotografiaAusenteHttpIT | 7 | 7 | 0 | 0 | 0 |
| ContratosLacunasFuncionaisHttpIT | 9 | 9 | 0 | 0 | 0 |
| ContratosLacunasHttpIT | 7 | 7 | 0 | 0 | 0 |
| ContratosLegadoPoliticaComplementarHttpIT | 2 | 2 | 0 | 0 | 0 |
| ContratosLegadoSemProvaHttpIT | 8 | 8 | 0 | 0 | 0 |
| ContratosOpcoesTemporalHttpIT | 1 | 1 | 0 | 0 | 0 |
| ContratosPisoReplayHttpIT | 4 | 4 | 0 | 0 | 0 |
| ContratosPrecedenciaBifurcadaHttpIT | 5 | 5 | 0 | 0 | 0 |
| ContratosRegrasHttpIT | 16 | 16 | 0 | 0 | 0 |
| ContratosTemporalLacunasHttpIT | 7 | 7 | 0 | 0 | 0 |
| DesligamentoFrequenciaProvaPreservadaHttpIT | 1 | 1 | 0 | 0 | 0 |
| DocumentoLegadoIntegridadeHttpIT | 3 | 3 | 0 | 0 | 0 |
| DocumentosCadeiaConcorrenciaNovaAuditoriaHttpIT | 1 | 1 | 0 | 0 | 0 |
| DocumentosContratuaisHttpIT | 4 | 4 | 0 | 0 | 0 |
| DocumentosInstrumentosPersistenciaHttpIT | 2 | 2 | 0 | 0 | 0 |
| DocumentosPermissoesTransferenciaGestaoHttpIT | 4 | 4 | 0 | 0 | 0 |
| DocumentosRascunhoCommitRollbackHttpIT | 3 | 3 | 0 | 0 | 0 |
| EmissaoRetificacaoConcorrenciaAuditoriaHttpIT | 2 | 2 | 0 | 0 | 0 |
| EmissaoSemFrequenciaInvalidacaoHttpIT | 2 | 2 | 0 | 0 | 0 |
| EmissaoVinculoTransferidoInvalidacaoHttpIT | 1 | 1 | 0 | 0 | 0 |
| EmpregadoConsultaFiltradaEscalaHttpIT | 2 | 2 | 0 | 0 | 0 |
| EmpregadoContextoCanonicoControlesHttpIT | 4 | 4 | 0 | 0 | 0 |
| EmpregadoHistoricoEscopoAtualHttpIT | 2 | 2 | 0 | 0 | 0 |
| EmpregadoTransferenciaGestaoEfetivaHttpIT | 6 | 6 | 0 | 0 | 0 |
| FechamentoContratoTransferidoHttpIT | 2 | 2 | 0 | 0 | 0 |
| FechamentosTransacoesHttpIT | 7 | 7 | 0 | 0 | 0 |
| FrequenciaAdicionalJustificativaLimiteHttpIT | 10 | 10 | 0 | 0 | 0 |
| FrequenciaAnulacaoContratoPreservacaoHttpIT | 1 | 1 | 0 | 0 | 0 |
| FrequenciaAuditoriaMinimizacaoHttpIT | 9 | 9 | 0 | 0 | 0 |
| FrequenciaComplementosEscopoAposPerdaCoberturaHttpIT | 2 | 2 | 0 | 0 | 0 |
| FrequenciaComplementosEscopoTemporalHttpIT | 17 | 17 | 0 | 0 | 0 |
| FrequenciaComplementosHistoricoHttpIT | 6 | 6 | 0 | 0 | 0 |
| FrequenciaComplementosHttpIT | 6 | 6 | 0 | 0 | 0 |
| FrequenciaComplementosIdentidadeLegadaHttpIT | 6 | 6 | 0 | 0 | 0 |
| FrequenciaComplementosPresidenciaHttpIT | 2 | 2 | 0 | 0 | 0 |
| FrequenciaConcorrenciaContratoHttpIT | 2 | 2 | 0 | 0 | 0 |
| FrequenciaConsultaMinimizacaoHttpIT | 10 | 10 | 0 | 0 | 0 |
| FrequenciaContratosHttpIT | 6 | 6 | 0 | 0 | 0 |
| FrequenciaEvidenciasIntegridadeHttpIT | 2 | 2 | 0 | 0 | 0 |
| FrequenciaFonteRetiradaRevisaoHttpIT | 4 | 4 | 0 | 0 | 0 |
| FrequenciaFontesMotivoObrigatorioHttpIT | 6 | 6 | 0 | 0 | 0 |
| FrequenciaFotografiaIdentidadeHttpIT | 8 | 8 | 0 | 0 | 0 |
| FrequenciaFotografiaLegadaHttpIT | 2 | 2 | 0 | 0 | 0 |
| FrequenciaFotografiasAusentesHttpIT | 5 | 5 | 0 | 0 | 0 |
| FrequenciaGestaoTransferidaAditamentoHttpIT | 2 | 2 | 0 | 0 | 0 |
| FrequenciaHistoricoEscopoTransferidoHttpIT | 1 | 1 | 0 | 0 | 0 |
| FrequenciaHistoricoIndicadoresAuditoriaHttpIT | 4 | 4 | 0 | 0 | 0 |
| FrequenciaMemoriaManualAuditoriaHttpIT | 5 | 5 | 0 | 0 | 0 |
| FrequenciaNomeLegadoDesconhecidoHttpIT | 6 | 6 | 0 | 0 | 0 |
| FrequenciaPermissaoHistoricaHttpIT | 3 | 3 | 0 | 0 | 0 |
| FrequenciaRelatoriosLacunasHttpIT | 16 | 16 | 0 | 0 | 0 |
| FrequenciaTemporalBordasHttpIT | 2 | 2 | 0 | 0 | 0 |
| GovernancaContratualHttpIT | 11 | 11 | 0 | 0 | 0 |
| GradeFrequenciaRotulosHistoricosHttpIT | 2 | 2 | 0 | 0 | 0 |
| HistoricoAssinaturaLegadaMistaHttpIT | 1 | 1 | 0 | 0 | 0 |
| HistoricoOcorrenciaContextoAtualHttpIT | 1 | 1 | 0 | 0 | 0 |
| HistoricosEmpregadosContratoHttpIT | 3 | 3 | 0 | 0 | 0 |
| IdempotenciaFontesFrequenciaHttpIT | 3 | 3 | 0 | 0 | 0 |
| ImpactoCapacidadeLimitesHttpIT | 1 | 1 | 0 | 0 | 0 |
| InstrumentosConfiguracaoHistoricaHttpIT | 7 | 7 | 0 | 0 | 0 |
| InstrumentosFotografiaCatalogoHttpIT | 2 | 2 | 0 | 0 | 0 |
| InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT | 5 | 5 | 0 | 0 | 0 |
| MotoresTemporaisConhecimentoRegressaoTest | 5 | 5 | 0 | 0 | 0 |
| OcorrenciaAutorizacaoTemporalHttpIT | 17 | 17 | 0 | 0 | 0 |
| OcorrenciaEscopoAposPerdaCoberturaHttpIT | 1 | 1 | 0 | 0 | 0 |
| OcorrenciaIdentidadeLegadaHttpIT | 1 | 1 | 0 | 0 | 0 |
| OcorrenciaNomeLegadoHttpIT | 2 | 2 | 0 | 0 | 0 |
| OcorrenciaPrivacidadeHistoricaHttpIT | 6 | 6 | 0 | 0 | 0 |
| OcorrenciaProvaPreservadaHttpIT | 1 | 1 | 0 | 0 | 0 |
| OcorrenciasPainelAssincronoHttpIT | 3 | 3 | 0 | 0 | 0 |
| PaineisHttpIT | 3 | 3 | 0 | 0 | 0 |
| PendenciaEscopoEfetivoHttpIT | 1 | 1 | 0 | 0 | 0 |
| PendenciaFrequenciaResponsabilidadeAtualHttpIT | 3 | 3 | 0 | 0 | 0 |
| PermissoesMinimizacaoHttpIT | 16 | 16 | 0 | 0 | 0 |
| PessoasAssinaturaImpactoVolumeHttpIT | 1 | 1 | 0 | 0 | 0 |
| PessoasAtomicidadeHttpIT | 5 | 5 | 0 | 0 | 0 |
| PessoasComposicaoConsentimentoHttpIT | 3 | 3 | 0 | 0 | 0 |
| PessoasComposicaoFuturaHttpIT | 3 | 3 | 0 | 0 | 0 |
| PessoasContinuidadeLegadoNovaAuditoriaHttpIT | 3 | 3 | 0 | 0 | 0 |
| PessoasCriteriosComplementaresHttpIT | 4 | 4 | 0 | 0 | 0 |
| PessoasCriteriosTemporaisComplementaresHttpIT | 1 | 1 | 0 | 0 | 0 |
| PessoasDesligamentoExcedenteFuturoHttpIT | 2 | 2 | 0 | 0 | 0 |
| PessoasEscopoOperacionalHttpIT | 21 | 21 | 0 | 0 | 0 |
| PessoasExcedenteNarrativasHttpIT | 1 | 1 | 0 | 0 | 0 |
| PessoasFotografiaTransferenciaRestauradaHttpIT | 4 | 4 | 0 | 0 | 0 |
| PessoasFotografiasHistoricasAusentesHttpIT | 18 | 18 | 0 | 0 | 0 |
| PessoasHistoricoMinimizacaoHttpIT | 4 | 4 | 0 | 0 | 0 |
| PessoasIdentidadeLegadaHttpIT | 1 | 1 | 0 | 0 | 0 |
| PessoasLacunasHttpIT | 8 | 8 | 0 | 0 | 0 |
| PessoasLimitesHistoricosCriteriosHttpIT | 5 | 5 | 0 | 0 | 0 |
| PessoasMatriculaConcorrenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| PessoasMinimizacaoEventosHttpIT | 6 | 6 | 0 | 0 | 0 |
| PessoasNarrativasMinimizacaoHttpIT | 3 | 3 | 0 | 0 | 0 |
| PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT | 4 | 4 | 0 | 0 | 0 |
| PessoasPoliticaMatriculaFuturaHttpIT | 8 | 8 | 0 | 0 | 0 |
| PessoasProgramacaoCanceladaHttpIT | 1 | 1 | 0 | 0 | 0 |
| PessoasProjecaoHabilitacaoHttpIT | 1 | 1 | 0 | 0 | 0 |
| PessoasProjecaoTransferenciaHttpIT | 3 | 3 | 0 | 0 | 0 |
| PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT | 3 | 3 | 0 | 0 | 0 |
| PessoasRecontratacaoAposInativacaoHttpIT | 7 | 7 | 0 | 0 | 0 |
| PessoasRegrasOriginaisHttpIT | 8 | 8 | 0 | 0 | 0 |
| PessoasRestauracaoPrecedenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| PessoasRetificacaoAposTransferenciaHttpIT | 2 | 2 | 0 | 0 | 0 |
| PessoasTransferenciaGestaoEfetivaHttpIT | 3 | 3 | 0 | 0 | 0 |
| PessoasTransferenciaReativacaoTemporalHttpIT | 1 | 1 | 0 | 0 | 0 |
| PessoasValidacoesAdicionaisHttpIT | 5 | 5 | 0 | 0 | 0 |
| PessoasVinculosContratoHttpIT | 16 | 16 | 0 | 0 | 0 |
| PreauditoriaAntecipacaoInicioVinculoHttpIT | 2 | 2 | 0 | 0 | 0 |
| PreauditoriaPainelAtividadesHttpIT | 3 | 3 | 0 | 0 | 0 |
| PreauditoriaPainelNovoDuranteRetificacaoHttpIT | 1 | 1 | 0 | 0 | 0 |
| PreauditoriaPainelRetificacaoConcorrenteHttpIT | 1 | 1 | 0 | 0 | 0 |
| PreauditoriaPainelRetificacaoHttpIT | 1 | 1 | 0 | 0 | 0 |
| PreauditoriaPendenciaHistoricaAposTransferenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| PreauditoriaPendenciaNarrativaHttpIT | 3 | 3 | 0 | 0 | 0 |
| PreviaCicloVidaLimitesHttpIT | 1 | 1 | 0 | 0 | 0 |
| PublicacaoCatalogoInativadoHttpIT | 1 | 1 | 0 | 0 | 0 |
| RelatorioComparacaoEscopoHttpIT | 7 | 7 | 0 | 0 | 0 |
| RelatorioNomeLegadoDesconhecidoHttpIT | 4 | 4 | 0 | 0 | 0 |
| RelatorioRotulosLegadosHttpIT | 2 | 2 | 0 | 0 | 0 |
| RelatoriosAdministrativosPendenciasHttpIT | 4 | 4 | 0 | 0 | 0 |
| RelatoriosConsultasHttpIT | 4 | 4 | 0 | 0 | 0 |
| RelatoriosExportacoesHttpIT | 8 | 8 | 0 | 0 | 0 |
| RelatoriosMinimizacaoExportacoesHttpIT | 8 | 8 | 0 | 0 | 0 |
| RelatoriosMinimizacaoHttpIT | 11 | 11 | 0 | 0 | 0 |
| ReplayCapacidadeAumentoHttpIT | 1 | 1 | 0 | 0 | 0 |
| ReplayCapacidadeDependenteHttpIT | 2 | 2 | 0 | 0 | 0 |
| ReplayVigenciaFrequenciaRelatorioHttpIT | 2 | 2 | 0 | 0 | 0 |
| RetificacaoCapacidadeContratualHttpIT | 2 | 2 | 0 | 0 | 0 |
| RetificacaoVigenciaDependenteHttpIT | 1 | 1 | 0 | 0 | 0 |
| RevisaoDownloadExpiracaoHttpIT | 1 | 1 | 0 | 0 | 0 |
| RevisaoDownloadsAutorizacaoAtualHttpIT | 3 | 3 | 0 | 0 | 0 |
| RevisaoEmissaoIdempotenciaConcorrenciaHttpIT | 1 | 1 | 0 | 0 | 0 |
| RevisaoExportacaoGeracaoAutorizacaoHttpIT | 2 | 2 | 0 | 0 | 0 |
| RevisaoFechamentoNegocialMysqlHttpIT | 1 | 1 | 0 | 0 | 0 |
| RevisaoGestaoSemOrigemHttpIT | 4 | 4 | 0 | 0 | 0 |
| RevisaoHistoricaAusenciaNominalHttpIT | 2 | 2 | 0 | 0 | 0 |
| RevisaoHistoricaAusenciaVersaoHttpIT | 5 | 5 | 0 | 0 | 0 |
| RevisaoHistoricaCatalogosIdentidadeHttpIT | 7 | 7 | 0 | 0 | 0 |
| RevisaoNomeGestaoEventoVinculoHttpIT | 2 | 2 | 0 | 0 | 0 |
| RevisaoPrecedenciaExplicitaHttpIT | 2 | 2 | 0 | 0 | 0 |
| RevisaoPreviaSucessaoAlcanceHttpIT | 1 | 1 | 0 | 0 | 0 |
| SegurancaTodasRotasNovaAuditoriaHttpIT | 5 | 5 | 0 | 0 | 0 |
| SuperficieRenderizacaoSeguraHttpIT | 1 | 1 | 0 | 0 | 0 |
| TemporalidadeInicializacaoHttpIT | 1 | 1 | 0 | 0 | 0 |
| TransferenciaConcessaoObrigatoriaHttpIT | 1 | 1 | 0 | 0 | 0 |
| TransferenciaGestaoFuturaAcessoHttpIT | 6 | 6 | 0 | 0 | 0 |
| TransferenciaPoliticaMatriculaHttpIT | 9 | 9 | 0 | 0 | 0 |
| VinculoAlocacaoRegressaoTest | 11 | 11 | 0 | 0 | 0 |
| VinculoConhecimentoSemOrigemHttpIT | 3 | 3 | 0 | 0 | 0 |
| CacheDocumentalSegurancaRegressaoTest | 8 | 8 | 0 | 0 | 0 |
| CacheFrequenciaMinimizacaoTest | 6 | 6 | 0 | 0 | 0 |
| ChaveCacheEscopoRevogadoTest | 4 | 4 | 0 | 0 | 0 |
| RedisQueryCacheArchitectureTest | 5 | 5 | 0 | 0 | 0 |
| ConclusaoBootstrapCatalogosMySqlIT | 3 | 3 | 0 | 0 | 0 |
| ConclusaoBootstrapUsuariosSeguroMySqlIT | 15 | 15 | 0 | 0 | 0 |
| ConclusaoBootstrapUsuariosSeguroTest | 5 | 5 | 0 | 0 | 0 |
| DataInitializerSeedTest | 4 | 4 | 0 | 0 | 0 |
| EmailGmailConfiguracaoTest | 1 | 1 | 0 | 0 | 0 |
| EnvironmentContractTest | 2 | 2 | 0 | 0 | 0 |
| InicializadorDadosPrimeiroAcessoRegressaoTest | 6 | 6 | 0 | 0 | 0 |
| ComposicaoContratualEtapa03Test | 2 | 2 | 0 | 0 | 0 |
| ComposicaoFotografiaLegadaTest | 6 | 6 | 0 | 0 | 0 |
| ContratoHttpReactTest | 11 | 11 | 0 | 0 | 0 |
| ContratoServicoEtapa02Test | 5 | 5 | 0 | 0 | 0 |
| ContratoTemporalidadeEtapa02Test | 4 | 4 | 0 | 0 | 0 |
| MotorPrecedenciaCicloVidaTest | 3 | 3 | 0 | 0 | 0 |
| MotorTemporalContratoEtapa04Test | 5 | 5 | 0 | 0 | 0 |
| MotorTemporalContratoLacunasTest | 9 | 9 | 0 | 0 | 0 |
| AutenticacaoControladorInitialPasswordTest | 4 | 4 | 0 | 0 | 0 |
| AutenticacaoPrimeiroAcessoRegressaoTest | 1 | 1 | 0 | 0 | 0 |
| CalendarioEventoControladorTest | 4 | 4 | 0 | 0 | 0 |
| CargoControladorTest | 3 | 3 | 0 | 0 | 0 |
| ContratoControladorOpcoesTest | 2 | 2 | 0 | 0 | 0 |
| JornadaControladorTurnoNoturnoTest | 3 | 3 | 0 | 0 | 0 |
| PerfilControladorPasswordTest | 4 | 4 | 0 | 0 | 0 |
| SuperintendenciaControladorTest | 3 | 3 | 0 | 0 | 0 |
| EntityIntegrityValidationTest | 7 | 7 | 0 | 0 | 0 |
| FrequenciaStatusTest | 3 | 3 | 0 | 0 | 0 |
| TokenAtivoTest | 1 | 1 | 0 | 0 | 0 |
| MotorTemporalVinculoEtapa06Test | 4 | 4 | 0 | 0 | 0 |
| PessoaServicoEtapa06Test | 3 | 3 | 0 | 0 | 0 |
| PessoaVinculoEtapa06Test | 6 | 6 | 0 | 0 | 0 |
| VinculoContratualCicloVidaEtapa06Test | 7 | 7 | 0 | 0 | 0 |
| VinculoContratualServicoEtapa06Test | 3 | 3 | 0 | 0 | 0 |
| AlocacaoVinculoServicoEtapa07Test | 7 | 7 | 0 | 0 | 0 |
| ComposicaoTransferenciaGestaoEtapa07Test | 2 | 2 | 0 | 0 | 0 |
| MotorTemporalAlocacaoEtapa07Test | 3 | 3 | 0 | 0 | 0 |
| TransferenciaGestaoContratoServicoEtapa07Test | 2 | 2 | 0 | 0 | 0 |
| DocumentoComprobatorioServicoEtapa08Test | 23 | 23 | 0 | 0 | 0 |
| CorrecaoIdentificadorContratoServicoEtapa10Test | 6 | 6 | 0 | 0 | 0 |
| PerfilEMinimizacaoEtapa10Test | 7 | 7 | 0 | 0 | 0 |
| PermissaoExcepcionalServicoEtapa10Test | 6 | 6 | 0 | 0 | 0 |
| ExportacaoRelatorioAvisoEtapa11Test | 1 | 1 | 0 | 0 | 0 |
| InvalidacaoEmissaoRelatorioServicoEtapa11Test | 1 | 1 | 0 | 0 | 0 |
| PendenciaAdministrativaServicoEtapa11Test | 5 | 5 | 0 | 0 | 0 |
| RelatorioAdministrativoConcorrenciaTest | 1 | 1 | 0 | 0 | 0 |
| RelatorioAdministrativoServicoEtapa11Test | 4 | 4 | 0 | 0 | 0 |
| SincronizadorPendenciasFrequenciaTest | 2 | 2 | 0 | 0 | 0 |
| FechamentoAutomaticoLotesTest | 1 | 1 | 0 | 0 | 0 |
| FechamentoFrequenciaControladorTest | 4 | 4 | 0 | 0 | 0 |
| FechamentoFrequenciaEstadoServicoTest | 11 | 11 | 0 | 0 | 0 |
| FechamentoFrequenciaPropertiesTest | 2 | 2 | 0 | 0 | 0 |
| FechamentoFrequenciaServicoStaticTest | 8 | 8 | 0 | 0 | 0 |
| FechamentoFrequenciaServicoTest | 10 | 10 | 0 | 0 | 0 |
| FrequenciaEtapa09Test | 16 | 16 | 0 | 0 | 0 |
| FrequenciaLargeScaleArchitectureTest | 8 | 8 | 0 | 0 | 0 |
| ProjecaoOperacionalPessoaServicoTest | 2 | 2 | 0 | 0 | 0 |
| RequisicaoCriacaoFechamentoFrequenciaValidationTest | 4 | 4 | 0 | 0 | 0 |
| RevalidacaoFrequenciaLotesTest | 1 | 1 | 0 | 0 | 0 |
| AutenticacaoSessaoUnicaIT | 1 | 1 | 0 | 0 | 0 |
| MotorTemporalVigenciaReplayIT | 13 | 13 | 0 | 0 | 0 |
| PainelEfetivoTemporalIT | 27 | 27 | 0 | 0 | 0 |
| RelatorioFrequenciaCanonicaIT | 11 | 11 | 0 | 0 | 0 |
| RelatorioFrequenciaEscopoHistoricoIT | 16 | 16 | 0 | 0 | 0 |
| VinculoAlocacaoConcorrenciaIT | 3 | 3 | 0 | 0 | 0 |
| PainelEscalaGrandeArchitectureTest | 9 | 9 | 0 | 0 | 0 |
| PainelPeriodosRequisitosTest | 6 | 6 | 0 | 0 | 0 |
| AssinaturaPreviaNegocialTest | 2 | 2 | 0 | 0 | 0 |
| ConclusaoEfetivoContratualTest | 3 | 3 | 0 | 0 | 0 |
| RegraDisponibilidadeCatalogoTest | 4 | 4 | 0 | 0 | 0 |
| RegraEfetivoContratualTest | 3 | 3 | 0 | 0 | 0 |
| RelatorioMensalAbsenteismoArchitectureTest | 5 | 5 | 0 | 0 | 0 |
| PainelRepositorioSqlConsistenciaTest | 2 | 2 | 0 | 0 | 0 |
| AutenticacaoRotacaoCsrfRegressaoTest | 4 | 4 | 0 | 0 | 0 |
| CsrfJwtCadeiaRegressaoTest | 4 | 4 | 0 | 0 | 0 |
| CsrfSpaContractTest | 1 | 1 | 0 | 0 | 0 |
| ExigirSenhaInterceptorTest | 5 | 5 | 0 | 0 | 0 |
| JwtAuthenticationFilterInactivityTest | 4 | 4 | 0 | 0 | 0 |
| LogoutRevocationContractTest | 2 | 2 | 0 | 0 | 0 |
| SegurancaAdversarialEtapa12Test | 3 | 3 | 0 | 0 | 0 |
| SessaoJwtSegurancaRegressaoTest | 11 | 11 | 0 | 0 | 0 |
| AcessoOrganizacionalServicoTest | 7 | 7 | 0 | 0 | 0 |
| ArmazenamentoTemporarioRelatorioServicoTest | 3 | 3 | 0 | 0 | 0 |
| AuditoriaCommitRegressaoTest | 7 | 7 | 0 | 0 | 0 |
| CalculadoraJornadaNoturnaServicoTest | 2 | 2 | 0 | 0 | 0 |
| CalculoFrequenciaServicoNoturnoTest | 5 | 5 | 0 | 0 | 0 |
| CargoServicoTest | 6 | 6 | 0 | 0 | 0 |
| CodigoQrServicoTest | 2 | 2 | 0 | 0 | 0 |
| DetalhesUsuarioPersonalizadoServicoMatriculaTest | 3 | 3 | 0 | 0 | 0 |
| EscopoAcessoUsuarioServicoTest | 3 | 3 | 0 | 0 | 0 |
| FechamentoFrequenciaProcessadorTest | 7 | 7 | 0 | 0 | 0 |
| FilaRegistroAuditoriaEncerramentoTest | 4 | 4 | 0 | 0 | 0 |
| FotografiaCatalogoComposicaoTest | 1 | 1 | 0 | 0 | 0 |
| GeradorSenhaTemporariaServicoTest | 1 | 1 | 0 | 0 | 0 |
| GestaoAcessoHierarquicoServicoTest | 5 | 5 | 0 | 0 | 0 |
| GestaoContratoTemporalLegadoTest | 4 | 4 | 0 | 0 | 0 |
| GestaoLocalServicoPresidenteTest | 2 | 2 | 0 | 0 | 0 |
| GovernancaTerceirosContratoTest | 2 | 2 | 0 | 0 | 0 |
| InvalidacaoEmissaoTemporalTest | 4 | 4 | 0 | 0 | 0 |
| PessoaAuditoriaTransacaoRegressaoTest | 1 | 1 | 0 | 0 | 0 |
| ProgramacaoFuturaCatalogoServicoTest | 4 | 4 | 0 | 0 | 0 |
| RecuperacaoSenhaServicoTest | 2 | 2 | 0 | 0 | 0 |
| RelatorioTerceirizadosServicoPaginationTest | 4 | 4 | 0 | 0 | 0 |
| SessaoUnicaConcorrenciaRegressaoTest | 4 | 4 | 0 | 0 | 0 |
| StatusEmpregadoServicoTest | 5 | 5 | 0 | 0 | 0 |
| SuperintendenciaServicoTest | 9 | 9 | 0 | 0 | 0 |
| TentativaLoginServicoTest | 1 | 1 | 0 | 0 | 0 |
| TotpServicoTest | 2 | 2 | 0 | 0 | 0 |
| UsuarioAplicacaoServiceSecurityTest | 11 | 11 | 0 | 0 | 0 |
| ContratoFiltroUtilsTest | 2 | 2 | 0 | 0 | 0 |
| HorarioUtilsTest | 3 | 3 | 0 | 0 | 0 |
| MatriculaUtilsTest | 2 | 2 | 0 | 0 | 0 |
| PaginacaoUtilsTest | 2 | 2 | 0 | 0 | 0 |
| UtilitarioJwtUnicidadeTest | 1 | 1 | 0 | 0 | 0 |
