# Persistência e integração em MySQL real

**Evidência da versão entregue. Nenhuma aprovação de versão anterior foi importada.**

Candidata: `candidata07`. Manifesto de fontes SHA-256: `fc92555e0785d29ed9b8f2dd3c86235c7eec03992537f27b7f0bc8f5d3526f25`.

MySQL 8.4.11 local por TCP, Redis 8.4.8 real e transações Spring/JPA. A aplicação usa `spring.jpa.hibernate.ddl-auto=update` e `spring.sql.init.mode=never`; nenhum Flyway, Liquibase, H2 ou DDL manual de tabelas foi usado. O catálogo lógico descartável foi provisionado vazio e suas tabelas foram criadas pelo Hibernate.

Os testes de integração usam HTTP no contexto Spring e conexões reais MySQL. Os controles de commit/rollback que declaram transações independentes relêem o estado em nova transação; outros cenários herdam uma transação de teste e usam flush/clear, comprovando releitura nesse contexto, sem equivaler a commit externo ou reinício. O alcance está descrito por cenário nos oráculos da auditoria. Relógios, schedulers e sincronizações são controlados quando necessário, sem substituir o banco por mocks. O JAR recebe prova adicional por HTTP/TCP em processo separado.

| Fase | Casos | Aprovados | Falhas | Erros | Ignorados |
|---|---|---|---|---|---|
| Integração | 1114 | 1114 | 0 | 0 | 0 |
| Carga | 1 | 1 | 0 | 0 | 0 |

As fixtures são sintéticas. Após a regressão e a carga, a preparação do JAR remove somente esses registros por DML, conserva o schema e prepara usuários de teste. A preparação tem relatório próprio e não é contabilizada como requisito funcional.

## Persistência após reinício

72 tabelas, 525 linhas e 138 chaves estrangeiras examinadas. Órfãos após reinício: 0. Preservação integral: True.

A comparação usa SELECTs e HEX(CAST(coluna AS BINARY)), sem comprimento fixo, diferenciando NULL, vazio e NUL sem converter decimais em inteiros. Compara todos os registros e os atributos coletados de colunas, tabelas-base, componentes de índices, regras de FKs e CHECKs antes/depois. Isso representa valores SQL e bytes de colunas binárias, não os bytes físicos das páginas InnoDB. As fotografias brutas privadas não acompanham a distribuição. As consultas são feitas por tabela sem requisições de negócio concorrentes; não constituem um snapshot transacional único de todas as tabelas. O inventário não abrange views, triggers, rotinas, permissões ou particionamento. A distribuição inclui as duas projeções com a estrutura coletada e os hashes/contagens de todas as linhas por tabela. Somente os valores brutos foram omitidos para não distribuir hashes de senhas e sessões. A publicação revalidou as quatro provas TCP e os dados privados; o consumidor confrontou a projeção exata novamente, distinguindo tipos JSON mesmo se hashes públicos forem refeitos.

O controle permanente do capturador atual passou em 25 casos e 7 distinções obrigatórias por SELECTs no MySQL real. Esse controle é da metodologia de prova, sem somar casos ou requisitos Java.

[evidencias/controle_roteiros/captura_decimal_01/CONTROLE_FINAL_V2.json](../evidencias/controle_roteiros/captura_decimal_01/CONTROLE_FINAL_V2.json)

[evidencias/jar/PROVA_HTTP_REINICIO.json](../evidencias/jar/PROVA_HTTP_REINICIO.json); [evidencias/jar/PUBLICACAO_PERSISTENCIA.json](../evidencias/jar/PUBLICACAO_PERSISTENCIA.json)

[evidencias/jar/PERSISTENCIA_ANTES_REINICIO.json](../evidencias/jar/PERSISTENCIA_ANTES_REINICIO.json)
[evidencias/jar/PERSISTENCIA_DEPOIS_REINICIO.json](../evidencias/jar/PERSISTENCIA_DEPOIS_REINICIO.json)

## Classes de integração executadas

| Classe | Casos | Aprovados | Falhas | Erros | Ignorados |
|---|---|---|---|---|---|
| Integração MySQL: TokenAtivoRepositorio | 3 | 3 | 0 | 0 | 0 |
| Integração MySQL: chamadas diretas de banco ainda existentes em services | 3 | 3 | 0 | 0 | 0 |
| Integração MySQL: contrato do schema Hibernate | 2 | 2 | 0 | 0 | 0 |
| Integração MySQL: disponibilidade e pool de conexões | 3 | 3 | 0 | 0 | 0 |
| Integração MySQL: fechamento de persistência da Etapa 12 | 3 | 3 | 0 | 0 | 0 |
| Integração MySQL: integridade transacional e concorrência | 5 | 5 | 0 | 0 | 0 |
| Integração MySQL: persistência da Etapa 11 | 1 | 1 | 0 | 0 | 0 |
| Integração MySQL: smoke de todas as queries de repositorio | 2 | 2 | 0 | 0 | 0 |
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
| VinculoConhecimentoSemOrigemHttpIT | 3 | 3 | 0 | 0 | 0 |
| ConclusaoBootstrapCatalogosMySqlIT | 3 | 3 | 0 | 0 | 0 |
| ConclusaoBootstrapUsuariosSeguroMySqlIT | 15 | 15 | 0 | 0 | 0 |
| AutenticacaoSessaoUnicaIT | 1 | 1 | 0 | 0 | 0 |
| MotorTemporalVigenciaReplayIT | 13 | 13 | 0 | 0 | 0 |
| PainelEfetivoTemporalIT | 27 | 27 | 0 | 0 | 0 |
| RelatorioFrequenciaCanonicaIT | 11 | 11 | 0 | 0 | 0 |
| RelatorioFrequenciaEscopoHistoricoIT | 16 | 16 | 0 | 0 | 0 |
| VinculoAlocacaoConcorrenciaIT | 3 | 3 | 0 | 0 | 0 |
