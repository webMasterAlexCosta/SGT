# Matriz de requisitos — candidata em continuidade

Java 0.0.4 / React 1.4.4. Documento de rastreabilidade, sem homologação integral.

Os 198 requisitos, seus 635 critérios recebidos e seis bloqueios documentais foram preservados sem reescrita. GOV-003 continua exigindo a imutabilidade e rastreabilidade dos fatos publicados. Os seis itens adicionais de escopo são tratados separadamente.

## Evidências e interpretação

Resultado dos cenários identificados, sem declaração de atendimento integral do requisito. MockMvc usa aplicação/JPA/MySQL reais, mas não certifica TCP do JAR nem navegador. Vitest/jsdom não certifica persistência. Gates externos e integridade dos artefatos têm evidências próprias.

Lote Java principal: `C23_VERIFY_FINAL`, saída `0`. Lote React: `C24_REACT_COMPLETO_FINAL`, saída `0`. Contagens e comparações de fontes estão em `evidencias_continuidade` no JSON correspondente. Não são transportadas aprovações da baseline ou de relatórios anteriores.

| Situação dos requisitos | Quantidade |
|---|---:|
| BLOQUEADO_DOCUMENTAL_COM_TESTES | 6 |
| TESTADO_NOS_CENARIOS_REFERENCIADOS | 192 |

## Bloqueios documentais preservados

São condições históricas de prova; dados sintéticos e testes verdes não recuperam documentos ausentes.

- **CON-023**: `BLOQUEADO_DOCUMENTAL_COM_TESTES`. O conteúdo integral do bloqueio recebido permanece no JSON e na cópia histórica.
- **CAT-004**: `BLOQUEADO_DOCUMENTAL_COM_TESTES`. O conteúdo integral do bloqueio recebido permanece no JSON e na cópia histórica.
- **CAT-007**: `BLOQUEADO_DOCUMENTAL_COM_TESTES`. O conteúdo integral do bloqueio recebido permanece no JSON e na cópia histórica.
- **COL-010**: `BLOQUEADO_DOCUMENTAL_COM_TESTES`. O conteúdo integral do bloqueio recebido permanece no JSON e na cópia histórica.
- **HIS-001**: `BLOQUEADO_DOCUMENTAL_COM_TESTES`. O conteúdo integral do bloqueio recebido permanece no JSON e na cópia histórica.
- **HIS-006**: `BLOQUEADO_DOCUMENTAL_COM_TESTES`. O conteúdo integral do bloqueio recebido permanece no JSON e na cópia histórica.

## Referências reconciliadas

| Recebida | Atual | Motivo |
|---|---|---|
| `com.example.siggtrat.servico.GovernancaTerceirosContratoTest#contratoPublicadoNaoPermiteTrocarFornecedorForaDeAditamento` | `com.example.siggtrat.servico.FornecedorContratoTest#contratoPublicadoNaoPermiteTrocarFornecedorForaDeAditamento` | Classe renomeada após extração do cadastro compartilhado; veto ao vínculo livre de publicado preservado. |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaNominalHttpIT#ausenciaNominalRecusaRetroatividadeEPreservaCapturaContemporanea` | `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaNominalHttpIT#ausenciaNominalPreservaDesconhecimentoRetroativoECapturaContemporanea` | Referência recebida obsoleta. O método atual permite o ato documentado e conserva o desconhecimento nominal retroativo (não preenche o passado com o nome atual); recusa o corte anterior à primeira prova. Não é alegada equivalência literal com o nome antigo nem transportado seu resultado histórico. |

## Decisões adicionais de escopo

| ID | Responsabilidade | Decisão | Evidência atual |
|---|---|---|---|
| EXTRA-MODULO | Módulo e resumo Governança de Terceiros | RETIRADO_POR_DECISAO | IMPLEMENTACAO_EXCLUSIVA_AUSENTE_E_ROTAS_AUSENTES_NO_REGISTRO_REAL |
| EXTRA-PROCESSOS | Processos exclusivos ONBOARDING/OFFBOARDING de ProcessoTerceiro | RETIRADO_POR_DECISAO | IMPLEMENTACAO_EXCLUSIVA_AUSENTE_E_ROTAS_AUSENTES_NO_REGISTRO_REAL |
| EXTRA-RISCO-MANUAL | Avaliação manual de risco de fornecedor, quatro dimensões e score total | RETIRADO_POR_DECISAO | IMPLEMENTACAO_EXCLUSIVA_AUSENTE_E_ROTAS_AUSENTES_NO_REGISTRO_REAL |
| EXTRA-MONITORAMENTO-RISCO | Monitoramento contínuo de risco e vencimento de avaliação | RETIRADO_POR_DECISAO | IMPLEMENTACAO_EXCLUSIVA_AUSENTE_E_ROTAS_AUSENTES_NO_REGISTRO_REAL |
| EXTRA-FORNECEDOR-INTERFACE | Cadastro de Fornecedor e seleção em contrato rascunho | ALTERADO | ALTERADO_COM_EVIDENCIAS_FOCAIS; TCP_E_NAVEGADOR_EM_GATE_PROPRIO |
| EXTRA-FINANCEIRO-ANTERIOR | Tarifas, folhas monetárias e faturas já retiradas antes desta missão | RETIRADO_ANTERIORMENTE_PRESERVAR_AUSENCIA | PRESERVAR_AUSENCIA; EVIDENCIA_DE_ARQUITETURA_E_TESTES_CONFORME_LOTE |

## GOV-003 — PRESERVADO

Nenhum fato negocial publicado é apagado fisicamente; pode ser cancelado, retificado, sucedido, encerrado ou substituído por nova versão, sempre auditável.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| GOV-003:001 | DELETE publicado=409 e contrato permanece | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| GOV-003:002 | Retificar mantém versões original e nova | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| GOV-003:003 | Cancelar preserva autor/motivo/data e retira só efeito | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoPrecedenciaExplicitaHttpIT#substituicaoPublicadaPreservaOriginalFatoLogicoECorteDeConhecimento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga` | APROVADO_XML_CANDIDATA | 6 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#cancelamentoComPosteriorCompativelPreservaDeltasEHistoricoSemCriarPendencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#cancelarRemanejamentoFuturoDeveSinalizarOInstrumentoPosteriorQueFicaIncompativel` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#doisCancelamentosQueAtingemMesmoPosteriorReutilizamPendenciaAberta` | APROVADO_XML_CANDIDATA | 1 |

## GOV-004 — PRESERVADO

Rascunho nunca publicado e sem fato negocial pode ser descartado fisicamente de forma transacional, incluindo dependências exclusivas de preparação.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| GOV-004:001 | Rascunho vazio aceita descarte 204 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| GOV-004:002 | GET após descarte=404 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| GOV-004:003 | Fato/vínculo/histórico impede descarte | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| GOV-004:004 | Falha intermediária não deixa descarte parcial | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#rascunhoComDependenciaAuditavelNaoPodeSerDescartado` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#falhaNaTransacaoDeDescarteRestauraContratoEComposicaoEmNovaLeitura` | APROVADO_XML_CANDIDATA | 1 |

## TMP-001 — PRESERVADO

Contrato é a base operacional; Colaborador/Pessoa, Vínculo Contratual e Alocação são conceitos distintos.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-001:001 | Pessoa, Vínculo e Alocação são raízes relacionadas distintas; cadastro integrado preserva a identidade canônica. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-001:002 | Frequência resolve Contrato/Vínculo/Alocação da data e devolve seus identificadores separadamente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-001:003 | Empregado legado sem associação/fotografia não adota outra Pessoa apenas por igualdade do CPF; fotografia própria existente conserva a identidade documentada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#consultasDiariaMensalEIntervaloRetornamIdentidadeCanonicaEEstadosIndependentes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#fotografiaComprovadaMantemIdentidadeOriginalMesmoSemAssociacaoAtual` | APROVADO_XML_CANDIDATA | 2 |

## TMP-002 — PRESERVADO

Colaborador representa a pessoa; Vínculo representa a relação temporal pessoa-contrato; Alocação representa Lotação + Turno + Cargo durante uma faixa do vínculo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-002:001 | Recontratação em dias consecutivos conserva Pessoa e cria novo Vínculo sem sobreposição. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-002:002 | Mudança de Lotação/Turno/Cargo retifica faixas da Alocação, preservando o mesmo Vínculo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |

## TMP-003 — PRESERVADO

Toda ocorrência relevante separa data efetiva, data/hora de registro, usuário responsável e motivo/justificativa quando aplicável.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-003:001 | Fatos separam data efetiva, instante de conhecimento, autor e motivo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-003:002 | Alterações de calendário preservam fotografia integral, sequência, autoria e conhecimento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-003:003 | Fontes manuais mantêm criação, retificação de retificação e cancelamento com autoria e motivo próprios. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-003:004 | Motivo aceito de adicional é persistido integralmente na raiz e na versão; excesso é recusado antes da gravação. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-003:005 | Normalização do motivo de um novo comando é coerente na validação, raiz e versão, sem reescrever fatos anteriores. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga` | APROVADO_XML_CANDIDATA | 6 |
| `com.example.siggtrat.auditoria20261029.FrequenciaAdicionalJustificativaLimiteHttpIT#criacaoValidaLimiteSemTruncarMotivoNemGravarParcialmente` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.FrequenciaAdicionalJustificativaLimiteHttpIT#retificacaoValidaLimiteEPreservaOriginalQuandoMotivoExcedeMaximo` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.FrequenciaAdicionalJustificativaLimiteHttpIT#espacosExternosNaoFazemMotivoValidoDivergirEntreRaizEVersao` | APROVADO_XML_CANDIDATA | 2 |

## TMP-004 — PRESERVADO

O sistema deve reconstruir a posição histórica pela data efetiva sem usar a projeção atual como fonte canônica.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-004:001 | Posição histórica contratual e de alocação usa fotografias dos fatos. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-004:002 | Gravar ou retificar frequência sobre alocação legada não converte rótulos atuais em fotografia de contexto histórico. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-004:003 | Leitura e retificação de frequência mantêm a identidade já fotografada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-004:004 | A posição em corte anterior deriva o estado do evento válido naquele corte. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-004:005 | Retificação/cancelamento de fonte manual não apaga o fato que reconstrói o passado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa07.MotorTemporalAlocacaoEtapa07Test#fotografiaDoEventoPreservaNomesDaAlocacaoHistorica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaTotalOuCamposVaziosNaoAutorizaProjecaoAtualComoPassado` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaDaPublicacaoNaoPromoveFotografiaDeAditamentoABaseOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#fotografiaRegistradaDepoisDoCorteNaoPodeAparecerComoConhecida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#publicacaoNovaMantemValoresERotulosMesmoComMestresAlterados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#ausenciaTotalDeFotografiaNaoUsaGestaoMaterializadaComoProvaDoPassado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#corteDeConhecimentoAnteriorAFonteNaoPodeUsarResponsabilidadeConhecidaDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaDeTransferenciaSemOrigemNaoAntecipaDestinoMasProvaDestinoDesdeADataDoAto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaLegadaPosteriorProvaSomenteSeuProprioPeriodo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelAtividadesHttpIT#painelNaoExpoeNarrativaDeOcorrenciaForaDaResponsabilidadeHistoricaCorrigida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#descricaoPosteriorNaoApagaDeltaAnteriorConhecidoDepoisNemAlteraFotografias` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#ausenciaDaOrigemNaoUsaMestreENaoRetroageFonteLegadaDatada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#camposNulosNaOrigemContinuamDesconhecidosEAditamentoSoComprovaOCampoExplicito` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#fonteLegadaAceitaDeltaPosteriorMasNaoAdivinhaIncorporacaoDeDeltaAnterior` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#deltaDeInicioTambemEReaplicadoSemUsarFotografiaIntegralPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#combinacaoNovaDeAditamentoPreservaConfiguracaoPropriaDepoisDaMudancaDoMestre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#instrumentoSomenteQuantitativoNaoRedefineConfiguracaoDaCombinacaoExistente` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#novaAlocacaoRetroativaNaoTransformaGradeEMunicipioAtuaisEmConfiguracaoPassada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#instrumentoSomenteQuantitativoNaoTrocaOsTresRotulosPublicadosPelosMestresAtuais` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#instrumentoQuantitativoNaoCompletaRotulosLegadosAusentesComOsMestresAtuais` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#combinacaoRealmenteNovaDeAditamentoPreservaSeusPropriosRotulosDocumentados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFotografiasAusentesHttpIT#gravacaoNaoCompletaRotulosLegadosComMestreEPreservaProvaDaPropriaFrequencia` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#instrumentoQuantitativoPreservaAusenciaDeConfiguracaoDaComposicaoLegada` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#combinacaoNovaDeAditamentoLegadoNaoEmprestaConfiguracaoDeOutraComposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#ausenciaLegitimaDaGradeNaoEPreenchidaPelaAlteracaoAtualDoMestre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#composicaoLegadaSemFonteNaoCapturaMestreNemEmPreUpdate` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFotografiaIdentidadeHttpIT#correcaoDeIdentidadeNaoReescreveConsultaNemRetificacaoDaFrequencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFotografiaIdentidadeHttpIT#retificacaoDeStatusPreservaFotografiaAnteriorSemCongelarNovosDias` | APROVADO_XML_CANDIDATA | 7 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#transferenciaRetroativaNaoTrocaNomeComprovadoDaOrigemPeloMestreRenomeado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#atoDeMatriculaRetroativoNaoEmprestaNomeAtualAoPeriodoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#ausenciaTotalDeFotoNominalPermaneceDesconhecidaDepoisDaCorrecaoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaNominalHttpIT#ausenciaNominalPreservaDesconhecimentoRetroativoECapturaContemporanea` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#retiradaDaProrrogacaoPreservaOCorteAnteriorESoRemoveSeuDelta` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato` | APROVADO_XML_CANDIDATA | 2 |

## TMP-005 — PRESERVADO

A data/hora de registro informa quando o sistema tomou conhecimento; não substitui a data real do fato.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-005:001 | Contrato, Vínculo e Alocação respeitam o conhecimento disponível até o instante solicitado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-005:002 | Calendário e complemento separam data do fato e instante de registro em consultas de conhecimento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#consultaConhecidoAteDistingueConhecimentoDoFato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#corteDeConhecimentoPreservaVisaoHistoricaAnteriorARetificacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.MotoresTemporaisConhecimentoRegressaoTest#cancelamentoConhecidoDepoisNaoApagaAlocacaoDaVisaoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaTotalOuCamposVaziosNaoAutorizaProjecaoAtualComoPassado` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaDaPublicacaoNaoPromoveFotografiaDeAditamentoABaseOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#fotografiaRegistradaDepoisDoCorteNaoPodeAparecerComoConhecida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#publicacaoNovaMantemValoresERotulosMesmoComMestresAlterados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#ausenciaTotalDeFotografiaNaoUsaGestaoMaterializadaComoProvaDoPassado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#corteDeConhecimentoAnteriorAFonteNaoPodeUsarResponsabilidadeConhecidaDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaDeTransferenciaSemOrigemNaoAntecipaDestinoMasProvaDestinoDesdeADataDoAto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaLegadaPosteriorProvaSomenteSeuProprioPeriodo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#descricaoPosteriorNaoApagaDeltaAnteriorConhecidoDepoisNemAlteraFotografias` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#deltaExigeConhecimentoDoInstrumentoEDoEvento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#ausenciaDaOrigemNaoUsaMestreENaoRetroageFonteLegadaDatada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#retiradaDaProrrogacaoPreservaOCorteAnteriorESoRemoveSeuDelta` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ReplayVigenciaFrequenciaRelatorioHttpIT#vinculoFrequenciaERelatorioUsamProrrogacaoRecebidaDepoisDaDescricaoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento` | APROVADO_XML_CANDIDATA | 2 |

## TMP-006 — PRESERVADO

Reinicialização do sistema não cria, encerra ou altera fato negocial; apenas reconstrói projeções a partir da história válida.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-006:001 | Reexecutar inicializadores não cria ou altera fatos negociais existentes. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-006:002 | Chegada da data de fato programado altera a posição consultada sem gravar outro fato. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.TemporalidadeInicializacaoHttpIT#reexecutarInicializadoresPreservaCustomizacaoEFatosNegociaisJaRegistrados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#chegadaDaDataProgramadaAtualizaConsultaComumSemRotinaDeEscrita` | APROVADO_XML_CANDIDATA | 1 |

## TMP-007 — PRESERVADO

O sistema trabalha com granularidade negocial diária para vínculo/alocação; horário serve à auditoria do registro, não para duas alocações no mesmo dia.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-007:001 | Alocação possui granularidade diária, inclusive quando retificada na própria data. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-007:002 | Jornada22–06 usa a alocação da data de início mesmo após mudança no dia seguinte. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT#jornada22As06PermaneceNaAlocacaoDoInicioMesmoComMudancaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |

## TMP-008 — PRESERVADO

Eventos retroativos são permitidos e devem preservar/reaplicar fatos posteriores válidos, sem mover/apagar pessoas silenciosamente.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-008:001 | Documento retroativo recompõe o contrato e reaplica documento posterior válido. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-008:002 | Mudança retroativa e retificações sucessivas preservam as faixas originais e sucessoras válidas. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-008:003 | Extensão através de transferência documentada recompõe destino sem recontratar e valida sua capacidade. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-008:004 | Restauração de faixa e cancelamento de dispensa preservam a obrigação posterior conhecida ou recusam o conflito sem gravação parcial. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaIndependenteReplayVigenciaHttpIT#aditamentoPosteriorDeDescricaoNaoPodeApagarProrrogacaoAnteriorRecebidaDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#descricaoPosteriorNaoApagaDeltaAnteriorConhecidoDepoisNemAlteraFotografias` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#deltaDeInicioTambemEReaplicadoSemUsarFotografiaIntegralPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayVigenciaFrequenciaRelatorioHttpIT#vinculoFrequenciaERelatorioUsamProrrogacaoRecebidaDepoisDaDescricaoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRegrasOriginaisHttpIT#retificacaoDeRetificacaoDoInicioRecuperaAlocacaoHistoricaOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRegrasOriginaisHttpIT#retificacaoDeDesligamentoRecuperaSucessoraHistoricaAtingidaPeloLimite` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRetificacaoAposTransferenciaHttpIT#ampliarTerminoAlemDaTransferenciaUsaMapeamentoDocumentadoSemRecontratar` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRetificacaoAposTransferenciaHttpIT#extensaoMapeadaRevalidaExcedenteDoDestinoAntesDeCriarSucessora` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasFotografiaTransferenciaRestauradaHttpIT#sucessoraRestauradaUsaMunicipioEGradeDocumentadosNaTransferencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasFotografiaTransferenciaRestauradaHttpIT#mapaLegadoSemFotografiaBloqueiaReconstrucaoSemInventarGradeDoCadastroAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#cadastroPosteriorAoAgendamentoAplicaMapaNasDuasFaixasSemAnteciparProjecao` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#antecipacaoDoInicioAtravessaTransferenciaReconstroiOrigemEPreservaConhecimentoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#mapaSemFotografiaDeJornadaRecusaCadastroSemInventarDestinoENaoDeixaRegistrosParciais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasPoliticaMatriculaFuturaHttpIT#ampliarOuCancelarTerminoRevalidaObrigacaoProgramadaNaFaixaRestaurada` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PessoasPoliticaMatriculaFuturaHttpIT#cancelarDispensaProgramadaRecusaVinculoFuturoQuePerderiaPoliticaOpcional` | APROVADO_XML_CANDIDATA | 1 |

## TMP-009 — PRESERVADO

Quando evento retroativo torna evento posterior incompatível, apresentar impacto e gerar pendência de tratamento; não apagar automaticamente.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-009:001 | Replay incompatível preserva documento posterior e gera pendência identificável. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-009:002 | Retirada retroativa de cobertura mantém a frequência e encaminha revisão. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-009:003 | Programação humana futura incompatível não é cancelada como ajuste técnico. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#iniciarTratamentoNaoResolveConflitoDeCapacidadeAindaPresente` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#conflitoAbertoExigeTratamentoEContinuaPersistido` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#cancelamentoDoFatoFuturoPermiteResolverSemApagarAOrigem` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#retificacaoDoFatoPosteriorPermiteResolverComAutoriaESemApagarOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#gestorDeOutraGestaoEPerfilEstadualNaoResolvemConflito` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#origemDeConflitoAusenteNaoEquivaleACausaResolvida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#origemDeConflitoIncoerenteNaoEquivaleACausaResolvida` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#confirmacaoDisputaLockRealDoContratoAntesDeValidarReplay` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01RecorrenciaPendenciaHttpIT#recorrenciaAbreNovaPendenciaSemReescreverResolucaoAnteriorNemDuplicarAtiva` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#cancelarRemanejamentoFuturoDeveSinalizarOInstrumentoPosteriorQueFicaIncompativel` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#doisCancelamentosQueAtingemMesmoPosteriorReutilizamPendenciaAberta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#rollbackDoCancelamentoTambemDesfazPendenciaERelacaoNaMesmaTransacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#cancelamentoComPosteriorCompativelPreservaDeltasEHistoricoSemCriarPendencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRegrasOriginaisHttpIT#desligamentoFuturoNaoCancelaSucessoraProgramadaSemTratamentoExplicito` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT#cancelarRemanejamentoQueAtivouComposicaoComVinculoFuturoEhBloqueadoSemMutacao` | APROVADO_XML_CANDIDATA | 1 |

## TMP-010 — PRESERVADO

Eventos conflitantes na mesma data efetiva exigem precedência negocial explícita; sem precedência, a conclusão é bloqueada.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-010:001 | Colisão na mesma data é bloqueada até existir ordem negocial completa. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-010:002 | SUCEDE explícito define a posição sem usar ordem técnica de gravação. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-010:003 | Restaurar sucessora no novo último dia exige precedência entre os fatos. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosPrecedenciaBifurcadaHttpIT#mesmaDataNaoAdmiteDoisSucessoresConflitantesSemOrdemEntreEles` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosPrecedenciaBifurcadaHttpIT#sequenciaFormalCompletaDesambiguaSucessoresDoMesmoAto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#precedenciaExplicitaOrdenaDeltasNaMesmaDataSemUsarMaiorIdOuRegistro` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayVigenciaFrequenciaRelatorioHttpIT#doisAditamentosDeFimNaMesmaDataExigemPrecedenciaAntesDaConfirmacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#sucedeExplicitoResolveConflitoSemUsarOrdemDeGravacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRestauracaoPrecedenciaHttpIT#restaurarSucessoraNoNovoUltimoDiaExigePrecedenciaExplicita` | APROVADO_XML_CANDIDATA | 1 |

## TMP-011 — PRESERVADO

Eventos da mesma data que não colidem podem coexistir.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-011:001 | Recortes distintos na mesma data podem coexistir; recortes colidentes continuam exigindo precedência. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoPrecedenciaExplicitaHttpIT#coexistenciaMantemCamposDistintosMasNaoAutorizaDuasDescricoesConflitantes` | APROVADO_XML_CANDIDATA | 1 |

## TMP-012 — PRESERVADO

Documento antigo recebido depois de documento posterior é inserido na posição temporal correta, com reaplicação dos fatos posteriores válidos e tratamento explícito de conflitos.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-012:001 | Documento antigo recebido depois entra pela data efetiva e reaplica fatos válidos posteriores. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-012:002 | Se o documento antigo torna um posterior incompatível, o posterior é preservado e o conflito fica explícito. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaIndependenteReplayVigenciaHttpIT#aditamentoPosteriorDeDescricaoNaoPodeApagarProrrogacaoAnteriorRecebidaDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#descricaoPosteriorNaoApagaDeltaAnteriorConhecidoDepoisNemAlteraFotografias` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#deltaDeInicioTambemEReaplicadoSemUsarFotografiaIntegralPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayVigenciaFrequenciaRelatorioHttpIT#vinculoFrequenciaERelatorioUsamProrrogacaoRecebidaDepoisDaDescricaoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada` | APROVADO_XML_CANDIDATA | 1 |

## TMP-013 — PRESERVADO

Início/aditamento/remanejamento/nova alocação usam a data efetiva como primeiro dia da nova situação; desligamento usa a data como último dia do vínculo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-013:001 | Desligamento usa a data como último dia; novo vínculo no dia seguinte não sobrepõe. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-013:002 | Mudança de alocação aplica nova situação desde a data indicada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#trocaDiretaEmDiasConsecutivosNaoSobrepoePeriodosInclusivos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |

## TMP-014 — PRESERVADO

Um evento não pode produzir efeito fora da vigência reconstruída do contrato, exceto quando o próprio fato, de forma válida, altera a vigência para abranger a data do efeito.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-014:001 | ADI fora da vigência só produz efeito quando o próprio ato amplia validamente a cobertura. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-014:002 | Prorrogação conhecida e programada permite cadastro dentro da extensão mantendo término humano. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoForaDaVigenciaExigeExtensaoValidaNoProprioFato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#cadastroAposConhecerProrrogacaoProgramadaUsaLimitePlanejadoERespeitaFimHumano` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaIndependenteReplayVigenciaHttpIT#aditamentoPosteriorDeDescricaoNaoPodeApagarProrrogacaoAnteriorRecebidaDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayVigenciaFrequenciaRelatorioHttpIT#vinculoFrequenciaERelatorioUsamProrrogacaoRecebidaDepoisDaDescricaoPosterior` | APROVADO_XML_CANDIDATA | 1 |

## TMP-015 — PRESERVADO

Prorrogação do mesmo contrato deve manter continuidade temporal; não é permitida lacuna interna de vigência.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-015:001 | Prorrogação não pode criar lacuna interna na vigência do mesmo contrato. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-015:002 | Extensão contínua não apaga término humano nem recontratação já programada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#prorrogacaoNaoPodeCriarLacunaInternaNaVigencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa` | APROVADO_XML_CANDIDATA | 1 |

## TMP-016 — PRESERVADO

Precedência negocial é representada por relação explícita entre eventos (`SUCEDE`, `RETIFICA`, `CANCELA`, `SUBSTITUI`, `MANTÉM/COEXISTE`) e, quando existente, pela sequência formal do instrumento; conflito sem precedência definida bloqueia conclusão.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-016:001 | Relação de precedência pode ser transitiva através de ato que altera outro campo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-016:002 | Retificar predecessor conserva a sequência de sucessor válido e o original consultável. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-016:003 | Restauração coincidente com término não pode usar data de registro como precedência implícita. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosPrecedenciaBifurcadaHttpIT#precedenciaTransitivaPodePassarPorInstrumentoQueAlteraOutroCampo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoPrecedenciaExplicitaHttpIT#coexistenciaMantemCamposDistintosMasNaoAutorizaDuasDescricoesConflitantes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosPrecedenciaBifurcadaHttpIT#retificarAntecessorPreservaSequenciaDoSucessorValidoNaMesmaData` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoPrecedenciaExplicitaHttpIT#substituicaoPublicadaPreservaOriginalFatoLogicoECorteDeConhecimento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRestauracaoPrecedenciaHttpIT#restaurarSucessoraNoNovoUltimoDiaExigePrecedenciaExplicita` | APROVADO_XML_CANDIDATA | 1 |

## TMP-017 — PRESERVADO

`FUTURO`, `VIGENTE` e `ENCERRADO` são derivados da linha temporal válida e da data corrente; rotinas técnicas apenas atualizam projeções/alertas/pendências. `CANCELADO` e `ANULADO` dependem de evento explícito.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| TMP-017:001 | FUTURO/VIGENTE/ENCERRADO são posições derivadas da data e não novos estados negociados. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-017:002 | Programação válida durante inatividade não reativa a operação silenciosamente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| TMP-017:003 | Anulação explícita afeta vínculo desde seu alcance, preservando antes da data/corte. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#futuroVigenteEEncerradoSaoDerivadosDaDataENaoGravadosComoEstadoNegocial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#chegadaDaDataProgramadaAtualizaConsultaComumSemRotinaDeEscrita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#aditamentoPosteriorNaoCancelaEncerramentoESeusDtosConservamMesmoLimite` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior` | APROVADO_XML_CANDIDATA | 1 |

## CON-001 — PRESERVADO

Número + ano identificam unicamente o contrato em todo o sistema.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-001:001 | Duplicar número/ano na mesma Gestão=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-001:002 | Duplicar entre Gestões=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-001:003 | Número igual em ano distinto pode existir | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-001:004 | Restrição JPA uk_contrato_numero_ano presente | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#cadastroNaoReutilizaIdentificadorAntigoEPesquisaHistoricaRespeitaEscopo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#correcoesConcorrentesParaIdentidadeLivreConfirmamUmaSoTransacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#mesmoNumeroEmAnosDiferentesIdentificaDoisContratosPersistidos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#identificacaoTemRestricaoCompostaNumeroEAno` | APROVADO_XML_CANDIDATA | 1 |

## CON-002 — PRESERVADO

Número e ano podem ser corrigidos enquanto RASCUNHO, respeitando unicidade.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-002:001 | PUT em rascunho altera número/ano | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-002:002 | Número/ano duplicado bloqueia sem persistir | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-002:003 | PUT publicado=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#identificadorDuplicadoEmPutDeRascunhoNaoAlteraNenhumContrato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#rascunhoNaoAssumeIdentificadorHistoricoDeOutroContrato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |

## CON-003 — PRESERVADO

Após publicação, número + ano são imutáveis no fluxo comum; correção excepcional é versionada/auditada e mantém o identificador antigo pesquisável.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-003:001 | PUT comum publicado=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-003:002 | Sem concessão correção=403 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-003:003 | Correção autorizada preserva antes/depois e versões | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-003:004 | Pesquisa pelo identificador anterior encontra contrato | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-003:005 | Reenvio exato não cria nova correção | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#cadastroNaoReutilizaIdentificadorAntigoEPesquisaHistoricaRespeitaEscopo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#correcaoDeIdentificadorExigePermissaoMantemPesquisaEPosicaoTemporal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa10.CorrecaoIdentificadorContratoServicoEtapa10Test#corrigeVersionaMantemHistoricoEAuditaComAutorizacaoConcedida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#correcaoExcepcionalNaoAssumeIdentificadorHistoricoDeOutroContratoNemPublicaFatosParciais` | APROVADO_XML_CANDIDATA | 1 |

## CON-004 — PRESERVADO

Rascunho descartado que nunca foi publicado pode liberar seu número/ano para reutilização.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-004:001 | Descartar rascunho e recriar mesmo número/ano=201 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-004:002 | Novo ID difere do descartado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-004:003 | Publicado permanece impedido de excluir | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#rascunhoNuncaPublicadoLiberaIdentidadeAoDescartarOuAlterar` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |

## CON-005 — PRESERVADO

Contrato possui descrição, tipo de contratação, empresa/CNPJ/razão social/nome fantasia, vigência, situação temporal/negocial, habilitação administrativa, efetivo total, composição, aditamentos/remanejamentos, pendências e histórico.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-005:001 | Detalhe tem campos obrigatórios e eixos distintos | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-005:002 | Rotas composição/instrumentos/pendências/eventos expõem fatos do mesmo contrato | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-005:003 | Consulta datada preserva empresa/tipo/vigência da data | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior` | APROVADO_XML_CANDIDATA | 1 |

## CON-006 — PRESERVADO

Data de início e fim de vigência são obrigatórias para contrato publicado.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-006:001 | Rascunho sem uma data=201 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-006:002 | Publicar sem início ou fim=400 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-006:003 | Fim anterior ao início rejeitado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-006:004 | Publicação válida mantém datas efetivas | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPublicacaoCamposObrigatoriosHttpIT#omissaoIsoladaImpedePublicacaoECompletarSomenteOCampoPermitePublicar` | APROVADO_XML_CANDIDATA | 10 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |

## CON-007 — PRESERVADO

Situação temporal/negocial `RASCUNHO`, `FUTURO`, `VIGENTE`, `ENCERRADO`, `CANCELADO` e `ANULADO` é separada da habilitação administrativa ativo/inativo e das pendências.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-007:001 | Contrato pode ser VIGENTE+INATIVO | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-007:002 | Passagem do tempo muda FUTURO/VIGENTE/ENCERRADO sem evento novo | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-007:003 | Anulado/cancelado dependem de evento | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-007:004 | Detalhe e posição temporal devem concordar na mesma data | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#habilitacaoAdministrativaEhEixoIndependenteDaSituacaoTemporal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#futuroVigenteEEncerradoSaoDerivadosDaDataENaoGravadosComoEstadoNegocial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#chegadaDaDataProgramadaAtualizaConsultaComumSemRotinaDeEscrita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#rascunhoCanceladoEAnuladoDependemDeEstadoNegocialExplicito` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#cancelarContratoFuturoMantemIdentificadorReservado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoInativaPermaneceInativaNaPosicaoTemporal` | APROVADO_XML_CANDIDATA | 1 |

## CON-008 — PRESERVADO

Publicação exige dados obrigatórios e composição consistente; rascunho pode estar incompleto.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-008:001 | Empresa/tipo/vigência/fonte incompletos bloqueiam | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-008:002 | Soma divergente e duplicidade bloqueiam | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-008:003 | Publicação cria evento/fotografia/base original | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-008:004 | Todos valores publicados devem estar reconstruíveis sem mestre atual | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#tipoContratacaoDeOutraGestaoEhBloqueadoMesmoSemGestaoNoJson` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPublicacaoCamposObrigatoriosHttpIT#omissaoIsoladaImpedePublicacaoECompletarSomenteOCampoPermitePublicar` | APROVADO_XML_CANDIDATA | 10 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ComposicaoPublicacaoConcorrenciaHttpIT#publicacaoAguardaMutacaoDaComposicaoENaoPublicaFotografiaDepoisRemovida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoPoliticaComplementarHttpIT#primeiroAditamentoNaoSubstituiFotografiaOriginalLegadaSemEventoDeOrigem` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoAlteraConfiguracoesNegociaisPreservandoFotoOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#publicacaoCongelaConfiguracaoDefinitivaERevalidaMudancaAposPrevia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#novaAlocacaoRetroativaNaoTransformaGradeEMunicipioAtuaisEmConfiguracaoPassada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#instrumentoSomenteQuantitativoNaoRedefineConfiguracaoDaCombinacaoExistente` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#instrumentoSomenteQuantitativoNaoTrocaOsTresRotulosPublicadosPelosMestresAtuais` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#combinacaoRealmenteNovaDeAditamentoPreservaSeusPropriosRotulosDocumentados` | APROVADO_XML_CANDIDATA | 1 |

## CON-009 — PRESERVADO

O contrato pertence a uma única Gestão Local por vez; Gestor Estadual acessa a estrutura sem tornar o contrato compartilhado entre Gestões.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-009:001 | Gestor Local escreve só própria Gestão | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-009:002 | Gestor Estadual lê estado e não escreve | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-009:003 | Gestões externas recebem 404 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-009:004 | Transferência preserva titular anterior por período | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosEscopoHttpIT#todasAsMutacoesContratuaisExigemGestorEContratoDaGestao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior` | APROVADO_XML_CANDIDATA | 1 |

## CON-010 — PRESERVADO

Pode existir inativação administrativa durante a vigência, distinta da situação temporal do contrato. A inativação possui data efetiva, data/hora de registro, motivo e autor; a reativação é registrada como novo evento auditável.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-010:001 | Criar intervalo INATIVO sem mudar vigência/vínculo | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-010:002 | Futuro não bloqueia antes da data | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-010:003 | Reativação preserva intervalo e vínculo | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-010:004 | Prévia desatualizada bloqueia confirmação | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#inativacaoFuturaRegistraFatoSemMudarProjecaoAtualAntesDaData` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#inativacaoProgramadaImpedeNovoInstrumentoQuandoSuaDataChega` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#previaListaImpactosERejeitaConfirmacaoQuandoNovaFrequenciaAparece` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAlcancePreviaContratoHttpIT#previaIncluiIntervaloRetiradoDoAtoEAssinaDependenciaNesseIntervalo` | APROVADO_XML_CANDIDATA | 2 |

## CON-011 — PRESERVADO

Enquanto administrativamente inativo, eventos contratuais programados continuam produzindo seu efeito histórico, mas não liberam operação até reativação explícita.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-011:001 | ADI programado altera descrição durante bloqueio | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-011:002 | REM programado altera distribuição | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-011:003 | Habilitação continua INATIVO | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-011:004 | Vínculo na data bloqueada=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao` | APROVADO_XML_CANDIDATA | 1 |

## CON-012 — PRESERVADO

Prorrogação retroativa que torna o contrato temporalmente vigente não reativa automaticamente sua operação se houver bloqueio/pendência administrativa.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-012:001 | Extensão válida torna situação VIGENTE | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-012:002 | Habilitação segue INATIVO | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-012:003 | Vínculo encerrado mantém término | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-012:004 | Retomada depende de comando explícito | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#prorrogacaoRetroativaNaoReativaContratoInativo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior` | APROVADO_XML_CANDIDATA | 1 |

## CON-013 — PRESERVADO

Mudança de Gestão Local não ocorre no fluxo comum; transferência excepcional exige data efetiva, motivo, auditoria e preservação da Gestão responsável em cada período.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-013:001 | PUT comum publicado=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-013:002 | Transferência exige autorização | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-013:003 | Antes da data Gestão origem | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-013:004 | Depois da data Gestão destino | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-013:005 | Nenhum cadastro histórico original é reescrito | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.TransferenciaConcessaoObrigatoriaHttpIT#transferenciaSemConcessaoPreservaTodaOrigemEPassaSomenteAposAutorizacaoEspecifica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#transferenciaUsaTipoMapeadoNoDestinoEPreservaTipoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#historicoDaTransferenciaNaoReescreveNomesAposRenomearCatalogosEGestoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaNovaContinuaExibindoFotografiasETipoComprovados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#transferenciaRetroativaNaoTrocaNomeComprovadoDaOrigemPeloMestreRenomeado` | APROVADO_XML_CANDIDATA | 1 |

## CON-014 — PRESERVADO

Contrato original pode ser cadastrado retroativamente, com fonte comprobatória, data efetiva real, registro/auditoria e prévia de impactos.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-014:001 | Sem prévia retroativa=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-014:002 | Prévia+confirmação publica | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-014:003 | Mudança da preparação invalida token | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-014:004 | Evento mantém início real e registro distinto | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#publicacaoRetroativaExigePreviaMesmoComFonteEComposicaoCompletas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#publicacaoRetroativaTemPreviaEAssinaDadosDePreparacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPublicacaoPreviaHttpIT#alteracaoDaPreparacaoDepoisDaPreviaExigeNovoTokenSemPublicacaoParcial` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#publicacaoCongelaConfiguracaoDefinitivaERevalidaMudancaAposPrevia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |

## CON-015 — PRESERVADO

Existe estado negocial `ANULADO` para contrato publicado posteriormente declarado inválido. A anulação não exclui fisicamente o contrato e deve preservar motivo, fonte comprobatória, autor e data/hora.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-015:001 | Sem data do ato/fonte rejeita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-015:002 | Anular preserva contrato | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-015:003 | Evento guarda motivo/autor/data/fonte | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-015:004 | Posição no alcance ANULADO | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasHttpIT#anulacaoExigePreviaAntesDeRetirarValidadeDosFatosRelacionados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior` | APROVADO_XML_CANDIDATA | 1 |

## CON-016 — PRESERVADO

Encerramento antecipado/rescisão é distinto de fim natural de vigência e de inativação administrativa. Ele altera a data final efetiva do contrato e exige fonte comprobatória por anexo e/ou referência SEI.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-016:001 | Data final efetiva reduzida | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-016:002 | Fonte obrigatória | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-016:003 | Vínculo aberto termina no mesmo dia com ENCERRAMENTO_ANTECIPADO_CONTRATO | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-016:004 | Dia seguinte contrato ENCERRADO | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-016:005 | Nenhuma realocação automática | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-016:006 | Rescisao compara termo com vigencia reconstruida por atos futuros. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#encerramentoSemCausaCadastradaFazRollbackEDepoisEncerraSemRealocacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#encerramentoAntecipadoUsaVigenciaReconstruidaProrrogadaPorAtoFuturo` | APROVADO_XML_CANDIDATA | 1 |

## CON-017 — PRESERVADO

Contrato FUTURO pode receber aditamento/remanejamento programados, com documentação válida, coerência temporal e nenhum efeito antes do permitido.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-017:001 | Instrumento futuro persiste sem efeito antecipado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-017:002 | Na data correta aparece em posição | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-017:003 | Fora de vigência bloqueia salvo extensão válida no próprio ADI | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-017:004 | Cancelamento anterior ao efeito preserva história | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoForaDaVigenciaExigeExtensaoValidaNoProprioFato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#remanejamentoForaDaVigenciaNaoRegistraFatoNemComposicao` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | APROVADO_XML_CANDIDATA | 1 |

## CON-018 — PRESERVADO

Anulação preserva fatos relacionados para auditoria, retira sua validade operacional e nunca realoca colaboradores automaticamente.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-018:001 | Vínculo e alocação continuam consultáveis | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-018:002 | Contrato da pessoa não é substituído | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-018:003 | Frequência afetada mantém prova mas deixa cálculo válido | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-018:004 | Anulação possui origem auditável | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#anulacaoFuturaPermiteVinculoLimitadoAoPeriodoAnteriorEBloqueiaDesdeAlcance` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaAnulacaoContratoPreservacaoHttpIT#anulacaoPreservaFrequenciaFechadaEProvaOriginalMasRetiraCalculoEGeraPendencia` | APROVADO_XML_CANDIDATA | 1 |

## CON-019 — PRESERVADO

Número/ano de contrato publicado e posteriormente anulado permanece reservado e pesquisável, sem reutilização.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-019:001 | DELETE anulado=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-019:002 | Criar mesmo número/ano=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-019:003 | Consulta histórico preserva o anulado | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#cadastroNaoReutilizaIdentificadorAntigoEPesquisaHistoricaRespeitaEscopo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#anulacaoECorrecaoPreservamAlcanceAnteriorSemReativacaoSilenciosa` | APROVADO_XML_CANDIDATA | 1 |

## CON-020 — PRESERVADO

Anulação registrada incorretamente é corrigida por novo evento auditado; não há reativação silenciosa.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-020:001 | Correção conserva anulação original | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-020:002 | Relação CANCELA aparece na história | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-020:003 | Detalhe e posição temporal permanecem INATIVO até reativação explícita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-020:004 | Novo vínculo não opera antes da reativação | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#anulacaoECorrecaoPreservamAlcanceAnteriorSemReativacaoSilenciosa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#correcaoCancelaAnulacaoSemApagarHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoCorrecaoAnulacaoOperacaoHttpIT#novoVinculoDepoisDeCorrigirAnulacaoExigeReativacaoSemDeixarPessoaOuFaixaParcial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#correcaoDaAnulacaoNaoReutilizaReativacaoAdministrativaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#correcaoDeAnulacaoFuturaMantemEfeitoAdministrativoNaDataDeclarada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest#correcaoDeUmaAnulacaoNaoRetiraValidadeDeOutraAnulacaoIndependente` | APROVADO_XML_CANDIDATA | 1 |

## CON-021 — PRESERVADO

Contrato FUTURO válido cancelado antes do início assume estado CANCELADO, distinto de ANULADO e ENCERRADO.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-021:001 | Contrato já iniciado não admite cancelar-futuro | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-021:002 | Cancelamento de futuro retorna CANCELADO | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-021:003 | Vínculos futuros não operam | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-021:004 | Origem e motivo permanecem | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-021:005 | Contrato FUTURO no registro não admite alcance de cancelamento após início, deixando intervalo operacional; ato documentado antes do início permanece permitido. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#cancelarContratoFuturoMantemIdentificadorReservado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#cancelamentoDeContratoFuturoPreservaEventoEAlteraEstadoExplicito` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoCancelamentoFuturoHttpIT#contratoAindaFuturoNaoAceitaCancelamentoComAlcanceDepoisDoInicio` | APROVADO_XML_CANDIDATA | 1 |

## CON-022 — PRESERVADO

A situação temporal/negocial, a habilitação administrativa e as pendências são eixos independentes do ciclo de vida do contrato.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-022:001 | VIGENTE e INATIVO coexistem | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-022:002 | Pendência não troca estado negocial | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-022:003 | Consulta atual e temporal concordam | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-022:004 | Somente evento próprio altera habilitação | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.ContratoTemporalidadeEtapa02Test#habilitacaoAdministrativaEhEixoIndependenteDaSituacaoTemporal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#pendenciaDeReplayNaoTrocaEstadoNegocialNemHabilitacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#chegadaDaDataProgramadaAtualizaConsultaComumSemRotinaDeEscrita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoNaoMudaEstadoNegocialOuHabilitacaoPorCampoEstranhoNoJson` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest#operacaoAdministrativaNaoSubstituiCorrecaoExplicitaDeAnulacao` | APROVADO_XML_CANDIDATA | 2 |

## CON-023 — BLOQUEADO

Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado.

**Validação:** BLOQUEADO_DOCUMENTAL_COM_TESTES. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-023:001 | Sem autorização ou mapa completo bloqueia | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-023:002 | Tipo de contratação posterior deve ser válido na Gestão destino | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-023:003 | Composições e alocações mapeadas sem apagar anteriores | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-023:004 | Histórico de origem não muda após renomear cadastros | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-023:005 | A política de matrícula resultante da transferência deve ser compatível com vínculos vigentes e programados atingidos; respeita override contratual false comprovado e matrícula válida já informada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-023:006 | Retificacao preserva transferencia anterior e reaplica ato posterior valido no corte efetivo e de conhecimento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-023:007 | Cancelamento futuro e explicito, preserva original e posteriores validos; posterior incompativel implica rollback. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-023:008 | Transferencia usa vigencia reconstruida, incluindo prorrogacao e encurtamento programados. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-023:009 | Contexto posterior inclui todas composicoes e tentativas concorrentes nao publicam fatos conflitantes. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.TransferenciaConcessaoObrigatoriaHttpIT#transferenciaSemConcessaoPreservaTodaOrigemEPassaSomenteAposAutorizacaoEspecifica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa07.TransferenciaGestaoContratoServicoEtapa07Test#bloqueiaTransferenciaSemMapeamentoDoContextoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#transferenciaExigeMapeamentoExplicitoDoTipoLocalDeDestino` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#transferenciaRejeitaTipoQuePertenceAGestaoDeOrigem` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#transferenciaUsaTipoMapeadoNoDestinoEPreservaTipoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaLegadaSemTipoDestinoNaoDeclaraTipoDaGestaoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#aditamentoQueDependeDoTipoLegadoExigeRegularizacaoExplicitaDoDestino` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#aditamentoPosteriorDeTipoDaOrigemExigeTratamentoAntesDaTransferencia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#transferenciaExcepcionalPreservaGestaoAnteriorEMapeiaComposicaoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRetificacaoAposTransferenciaHttpIT#ampliarTerminoAlemDaTransferenciaUsaMapeamentoDocumentadoSemRecontratar` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#cadastroPosteriorAoAgendamentoAplicaMapaNasDuasFaixasSemAnteciparProjecao` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#antecipacaoDoInicioAtravessaTransferenciaReconstroiOrigemEPreservaConhecimentoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaDeTransferenciaSemOrigemNaoAntecipaDestinoMasProvaDestinoDesdeADataDoAto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#historicoDaTransferenciaNaoReescreveNomesAposRenomearCatalogosEGestoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaLegadaNaoSubstituiFotografiasAusentesPorNomesAtuais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaNovaContinuaExibindoFotografiasETipoComprovados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosFotografiaAusenteHttpIT#novoMapaConservaProvaOuAusenciaDaOrigemECapturaDestinoDoAto` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.contrato.ComposicaoFotografiaLegadaTest#callbackDeNovoMapaPreservaOrigemDesconhecidaECapturaDestinoDocumentado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#transferenciaParaTipoObrigatorioRecusaVinculoSemMatriculaSemMudarFatos` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#overrideContratualOpcionalPermiteTransferirParaTipoObrigatorio` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#matriculaInformadaPermiteTransferenciaComHerancaObrigatoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#regularizarMatriculaAposRecusaPermiteMesmaTransferenciaSemConsumirConcessaoDuasVezes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#aditamentoPosteriorDispensandoMatriculaAntesDoInicioProgramadoEhRespeitadoNaTransferencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#restauracaoPosteriorDaHerancaObrigatoriaImpedeTransferenciaMesmoComOverrideOpcionalInicial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#retificarTransferenciaProgramadaReconstroiOrigemEReaplicaAditamentoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#cancelarTransferenciaFuturaPreservaOriginalEPosteriorSemEfeitoDeResponsabilidade` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#cancelarTransferenciaComTipoPosteriorIncompativelFazRollbackIntegral` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#transferenciaPodeUsarVigenciaProrrogadaPorAditamentoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#transferenciaForaDaVigenciaEncurtadaPorAditamentoFuturoNaoPersiste` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#transferenciaRecusaComposicaoPosteriorAindaReferenteAGestaoOrigem` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaContratoHttpIT#duasTransferenciasConflitantesConcorrentesNaoPublicamDuasCadeias` | APROVADO_XML_CANDIDATA | 1 |

## CON-024 — PRESERVADO

Anulação registra data do ato, data/hora de registro e data efetiva de alcance; o alcance segue o documento anulatório e não a data de digitação.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-024:001 | Ato posterior com alcance anterior mantém ambas as datas | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-024:002 | Dia anterior ao alcance não fica anulado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-024:003 | Dia do alcance fica ANULADO | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-024:004 | Registro possui Instant e autor | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#alcanceFuturoNaoEhAntecipadoNoDetalheENaoApagaAditamentoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#anulacaoFuturaPermiteVinculoLimitadoAoPeriodoAnteriorEBloqueiaDesdeAlcance` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoAnulacaoFuturaHttpIT#posicaoDoVinculoMostraAnulacaoDesdeAlcancePreservandoFaixaAnterior` | APROVADO_XML_CANDIDATA | 1 |

## CON-025 — PRESERVADO

Contrato FUTURO publicado e posteriormente CANCELADO mantém número/ano reservado e pesquisável; somente rascunho nunca publicado pode liberar o identificador.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CON-025:001 | Criar identidade do cancelado=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-025:002 | Excluir cancelado=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CON-025:003 | História e motivo permanecem consultáveis | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#cancelarContratoFuturoMantemIdentificadorReservado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar` | APROVADO_XML_CANDIDATA | 1 |

## EMP-001 — PRESERVADO

Contrato publicado deve possuir CNPJ, razão social e nome fantasia da empresa terceirizada.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| EMP-001:001 | Omitir cada campo empresarial permite rascunho mas bloqueia publicação | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| EMP-001:002 | CNPJ inválido em tamanho rejeita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| EMP-001:003 | Fotografia completa persiste | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPublicacaoCamposObrigatoriosHttpIT#omissaoIsoladaImpedePublicacaoECompletarSomenteOCampoPermitePublicar` | APROVADO_XML_CANDIDATA | 10 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#contratoRejeitaVigenciaTipoOuCnpjInvalidosSemPublicacaoParcial` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior` | APROVADO_XML_CANDIDATA | 1 |

## EMP-002 — PRESERVADO

Mudança de razão social/nome fantasia vale para a posição contratual a partir da data do evento e não reescreve outros contratos/históricos.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| EMP-002:001 | Antes do evento mantém empresa original | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| EMP-002:002 | Na data aplica novos nomes | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| EMP-002:003 | Outro contrato não muda | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| EMP-002:004 | Fluxo de governança não troca fornecedor publicado livremente | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#mudancaSomenteDosNomesComMesmoCnpjNaoAlteraOutroContratoNemVersaoOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.servico.FornecedorContratoTest#contratoPublicadoNaoPermiteTrocarFornecedorForaDeAditamento` | APROVADO_XML_CANDIDATA | 1 |

## EMP-003 — PRESERVADO

Troca real de empresa/CNPJ é fato contratual temporal; erro material de CNPJ é correção excepcional auditada, sem ser tratado como troca real.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| EMP-003:001 | Troca real mantém empresa anterior no passado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| EMP-003:002 | Correção material produz CORRECAO_CNPJ | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| EMP-003:003 | Corte conhecidoAte preserva informação anterior | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| EMP-003:004 | Reenvio exato é idempotente | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| EMP-003:005 | Fornecedor escolhido deve corresponder ao CNPJ informado | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT#trocaDeFornecedorNaoPodeConservarSnapshotsDaEmpresaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#correcaoMaterialCnpjTemFatoProprioPreservaCorteDeConhecimentoEIdempotencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT#fornecedorInformadoDeveConferirComCnpjExplicitoDoInstrumento` | APROVADO_XML_CANDIDATA | 1 |

## CAT-001 — PRESERVADO

Tipo de contratação é cadastro prévio administrado no escopo da Gestão Local, não texto livre.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CAT-001:001 | Tipo é entidade prévia e selecionada por ID, sem aceitar texto livre como substituto. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-001:002 | Somente Gestor Local ativo administra Tipos da sua Gestão; ID de outra Gestão é recusado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.contrato.ContratoServicoEtapa02Test#tipoContratacaoDeOutraGestaoEhBloqueadoMesmoSemGestaoNoJson` | APROVADO_XML_CANDIDATA | 1 |

## CAT-002 — PRESERVADO

Cargo é cadastro próprio administrado pela Gestão Local.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CAT-002:001 | Cargo possui identidade/cadastro próprio com validação de nome. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-002:002 | Escrita e unicidade nominal são delimitadas pela Gestão Local proprietária. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.servico.CargoServicoTest#criaCargoNormalizandoCamposTextuaisNoEscopoDaGestaoLocal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cargoNormalizadoDuplicadoERecorteDeOutraGestaoSaoRecusadosSemGravarComposicao` | APROVADO_XML_CANDIDATA | 1 |

## CAT-003 — PRESERVADO

Lotação, Turno, Cargo e demais catálogos usados historicamente permanecem consultáveis mesmo inativos.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CAT-003:001 | Inativar preserva a entidade e sua identidade; leitura histórica continua disponível. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-003:002 | Listagens/detalhes autorizados não excluem um catálogo somente por estar inativo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#catalogoInativadoPermaneceNaListagemAutorizadaComMesmoId` | APROVADO_XML_CANDIDATA | 5 |

## CAT-004 — BLOQUEADO

Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual.

**Validação:** BLOQUEADO_DOCUMENTAL_COM_TESTES. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CAT-004:001 | Novos fatos de alocação/vínculo capturam rótulos e configurações da origem efetivamente escolhida. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-004:002 | Renomear Gestão/Lotação/Turno/Cargo/Tipo posteriormente não modifica as fotografias dos fatos anteriores. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-004:003 | Uma fotografia antiga nula/vazia continua desconhecida; nenhum nome/configuração é preenchido pelo mestre atual. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-004:004 | Documentos/relatórios/exportações consomem fotografias por fato; a prova transversal depende também dos lotes contratos e frequência. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#alocacoesHistoricasNaoSaoRenomeadasPelosCatalogosAtuais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasFotografiaTransferenciaRestauradaHttpIT#sucessoraRestauradaUsaMunicipioEGradeDocumentadosNaTransferencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaRotuloAuditoriaIndependenteHttpIT#inclusaoFotografaRotuloComprovadoParaDataEfetiva` | APROVADO_XML_CANDIDATA | 6 |
| `com.example.siggtrat.auditoria20261029.RevisaoNomeGestaoEventoVinculoHttpIT#atoDeMatriculaPreservaNomeDaGestaoDoRecorteComFonteOriginal` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#novaAlocacaoRetroativaNaoTransformaGradeEMunicipioAtuaisEmConfiguracaoPassada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#novoAtoDeHojeOuFuturoContinuaCapturandoAConfiguracaoAtual` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#composicaoLegadaSemFonteNaoCapturaMestreNemEmPreUpdate` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#municipioOriginalLegitimamenteNuloNaoECompletadoPeloMestrePosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#publicacaoCongelaConfiguracaoDefinitivaERevalidaMudancaAposPrevia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#combinacaoNovaDeAditamentoPreservaConfiguracaoPropriaDepoisDaMudancaDoMestre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#instrumentoSomenteQuantitativoNaoRedefineConfiguracaoDaCombinacaoExistente` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#instrumentoQuantitativoPreservaAusenciaDeConfiguracaoDaComposicaoLegada` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#combinacaoNovaDeAditamentoLegadoNaoEmprestaConfiguracaoDeOutraComposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#calendarioMunicipalUsaMunicipioOriginalDaNovaAlocacaoRetroativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#instrumentoSomenteQuantitativoNaoTrocaOsTresRotulosPublicadosPelosMestresAtuais` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#instrumentoQuantitativoNaoCompletaRotulosLegadosAusentesComOsMestresAtuais` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#combinacaoRealmenteNovaDeAditamentoPreservaSeusPropriosRotulosDocumentados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaNovaContinuaExibindoFotografiasETipoComprovados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosFotografiaCatalogoHttpIT#historicoDoRemanejamentoPreservaNomeFotografadoDoCargoAposRenomearCatalogo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#transferenciaRetroativaNaoTrocaNomeComprovadoDaOrigemPeloMestreRenomeado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasFotografiasHistoricasAusentesHttpIT#listaDeAlocacoesNaoCompletaRotulosAusentesPeloCadastroAtual` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.PessoasFotografiasHistoricasAusentesHttpIT#motorSemEventosNaoInfereRotulosHistoricosDoMestre` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.PessoasFotografiaTransferenciaRestauradaHttpIT#mapaLegadoSemFotografiaBloqueiaReconstrucaoSemInventarGradeDoCadastroAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#transferenciaLegadaNaoSubstituiFotografiasAusentesPorNomesAtuais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#mapaSemFotografiaDeJornadaRecusaCadastroSemInventarDestinoENaoDeixaRegistrosParciais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#ausenciaLegitimaDaGradeNaoEPreenchidaPelaAlteracaoAtualDoMestre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosFotografiaCatalogoHttpIT#historicoDoAditamentoPreservaNomeFotografadoDoCargoAposRenomearCatalogo` | APROVADO_XML_CANDIDATA | 1 |

## CAT-005 — PRESERVADO

Não inativar Cargo/Lotação/Turno com programação futura sem antes tratar a programação.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CAT-005:001 | Inativação de Cargo/Lotação/Turno é recusada enquanto composições, alocações, instrumentos ou transferências válidas cobrem programação futura. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-005:002 | O tratamento da programação ocorre por comando de cancelamento/retificação preservando o fato original. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#transferenciaProgramadaSemPessoasImpedeInativarCatalogoDoDestino` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#catalogoDaOrigemSemUsoFuturoPodeSerInativadoAposTransferenciaEfetiva` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogoPublicacaoConcorrenteAuditoriaIndependenteHttpIT#publicacaoConfirmadaImpedeInativacaoQueAguardouCatalogo` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.CatalogoPublicacaoConcorrenteAuditoriaIndependenteHttpIT#consultaSomenteLeituraNaoAguardaEscritorDoCatalogo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaFuturaRestauraCoberturaOriginalSemExcluirHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaComAlocacaoPosteriorIncompativelFazRollbackIntegral` | APROVADO_XML_CANDIDATA | 1 |

## CAT-006 — PRESERVADO

Cadastro inativo pode ser reativado, mantendo identidade histórica e auditoria.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CAT-006:001 | Reativação explícita preserva o ID do catálogo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-006:002 | O ato grava estado anterior/novo, data efetiva, registro e responsável. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#disponibilidadeDistingueDataEfetivaDeDataDeConhecimentoEAutorizaEscopo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#reativacaoDeCargoPreservaIntervaloInativoAutoriaEPermiteComposicaoFutura` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#primeiraTransicaoAtualEReativacaoConservamIdEHistoriaSemInventarOrigem` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.auditoria20261029.CatalogoPublicacaoConcorrenteAuditoriaIndependenteHttpIT#confirmacaoReleEstadoETrilhaAposInativacaoEReativacaoConcorrentes` | APROVADO_XML_CANDIDATA | 5 |

## CAT-007 — BLOQUEADO

Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva.

**Validação:** BLOQUEADO_DOCUMENTAL_COM_TESTES. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CAT-007:001 | Fato retroativo aceita item hoje inativo quando a trilha comprova atividade na data efetiva. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-007:002 | Corte de conhecimento exclui transições registradas depois do instante consultado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-007:003 | Antes da primeira origem comprovada, ou sem fonte de origem, disponibilidade é desconhecida e o uso negocial dependente é recusado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-007:004 | Primeira transição concorrente produz uma única origem e rejeita estado anterior obsoleto sem gravação parcial. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-007:005 | Disponibilidade retroativa é documentada por comando canônico escopado com motivo, fonte, autorização e versão esperada, sem certificar período anterior à fonte. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-007:006 | A inserção retroativa respeita a ordem efetiva e conserva reativação posterior na projeção atual. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-007:007 | Confirmação de ato retroativo exige prévia vigente de versão, trilha e dependências; reenvio da mesma intenção não duplica fato ou versão. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-007:008 | Ato incompatível com fatos posteriores ou com programação futura é recusado atomicamente, preservando as fontes existentes. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT#composicaoOriginalRetroativaUsaCatalogosValidosNoPeriodo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCatalogosEmpresaHttpIT#contratoRetroativoUsaTipoValidoNaDataMesmoInativoHoje` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#disponibilidadeDistingueDataEfetivaDeDataDeConhecimentoEAutorizaEscopo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#disponibilidadeSemOrigemNaoUsaFlagAtualEAceitaFonteRetroativaExplicita` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#corteDeConhecimentoDaDisponibilidadeExcluiOrigemRegistradaDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#estadoAtualOuAnteriorSemMarcoDeOrigemNaoProvaDisponibilidadeRetroativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#disponibilidadeSemEventoPermaneceDesconhecidaAtePrimeiroFatoDocumentado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoSemProvaHttpIT#aditamentoAntesDoPrimeiroFatoSemEstadoAnteriorRetornaConflitoDeConhecimento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransferenciaLacunasHttpIT#primeiraInativacaoSemFonteNaoInventaDisponibilidadeAnterior` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#primeiraTransicaoConcorrenteTemUmaOrigemERejeitaEstadoAnteriorObsoleto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#fonteRetroativaDocumentadaPermiteVinculoPassadoComCargoHojeInativo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#primeiraTransicaoAtualEReativacaoConservamIdEHistoriaSemInventarOrigem` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#somenteGestorLocalDaPropriaGestaoPodeRegistrarOuPrepararTransicao` | APROVADO_XML_CANDIDATA | 6 |
| `com.example.siggtrat.auditoria20261029.CatalogosVersaoCanonicaAuditoriaIndependenteHttpIT#edicaoExpoeVersaoConfirmadaEPermiteTransicaoSemAdivinhacao` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#transicaoRetroativaPreservaReativacaoEfetivaPosteriorMesmoRegistradaAntes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#reenvioDaMesmaIntencaoRetornaMesmoEventoSemExigirVersaoJaConsumida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#previaObsoletaPorNovoVinculoExigeNovaAnaliseMesmoSemMudarVersaoDoCatalogo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#requisicaoIncompletaOuNaoConfirmadaNaoProduzFatos` | APROVADO_XML_CANDIDATA | 8 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#atoRetroativoNaoPodeInvalidarEscolhaPosteriorNemApagarSuaFonte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#inativacaoAtualPelaNovaRotaRespeitaProgramacaoFutura` | APROVADO_XML_CANDIDATA | 1 |

## CAT-008 — PRESERVADO

Cadastro atualmente inativo não pode ser usado em evento futuro sem reativação explícita.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CAT-008:001 | Um item atualmente inativo não autoriza programação futura. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-008:002 | Reativação explícita cria nova prova sem apagar o intervalo de inatividade. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.PublicacaoCatalogoInativadoHttpIT#publicacaoRevalidaCargoInativadoDepoisDaCriacaoDaComposicaoEmRascunho` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogoPublicacaoConcorrenteAuditoriaIndependenteHttpIT#inativacaoConfirmadaImpedePublicacaoQueAguardouCatalogo` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.CatalogoPublicacaoConcorrenteAuditoriaIndependenteHttpIT#confirmacaoReleEstadoETrilhaAposInativacaoEReativacaoConcorrentes` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.auditoria20261029.CatalogoPublicacaoConcorrenteAuditoriaIndependenteHttpIT#consultaSomenteLeituraNaoAguardaEscritorDoCatalogo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#reativacaoDeCargoPreservaIntervaloInativoAutoriaEPermiteComposicaoFutura` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTransicoesOperacionaisHttpIT#primeiraTransicaoAtualEReativacaoConservamIdEHistoriaSemInventarOrigem` | APROVADO_XML_CANDIDATA | 5 |

## CAT-009 — PRESERVADO

Causas de término do vínculo devem distinguir desligamento individual, fim natural, rescisão antecipada, anulação e outras causas cadastradas.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CAT-009:001 | Catálogo classifica desligamento individual, fim de vigência, encerramento antecipado, anulação e outras causas separadamente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CAT-009:002 | Cada término usa causa da categoria e Gestão responsável na data do fato; descrição/categoria aplicadas ficam fotografadas. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosTemporalHttpIT#motivoInativadoHojePermaneceDisponivelParaDesligamentoRealAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#desligamentoIndividualRejeitaCausaContratualSemAtoCorrespondente` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#previaDeRetificacaoRejeitaCausaContratualSemAtoCorrespondente` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#desligamentoIndividualEOuOutraCausaCadastradaContinuamValidos` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#causaContratualComAtoEDataCorretosPermiteRetificacaoExplicitamenteConfirmada` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#existenciaDoAtoNaoAutorizaUsarSuaCausaEmOutraData` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#fimAntecipadoPorRescisaoNaoPodeSerReclassificadoComoFimNatural` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#anulacaoFuturaNaoJustificaCausaAnulatoriaAntesDoAlcance` | APROVADO_XML_CANDIDATA | 1 |

## CMP-001 — PRESERVADO

Composição é combinação explícita Lotação + Turno + Cargo + efetivo previsto; não usar produto cartesiano automático.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-001:001 | Uma entrada cria exatamente uma composição | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-001:002 | IDs obrigatórios validados | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-001:003 | Composição inexistente não é opção de vínculo | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#composicaoExigeTodosIdsDoRecorteSemGerarCombinacaoImplicita` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT#composicaoInexistenteRecusaVinculoSemGravarPessoaReservaOuHistorico` | APROVADO_XML_CANDIDATA | 1 |

## CMP-002 — PRESERVADO

Não pode haver combinação duplicada no mesmo recorte temporal.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-002:001 | Criar duplicata sobreposta=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-002:002 | Lista mantém uma entrada | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-002:003 | Itens repetidos ADI/REM rejeitados | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-002:004 | Transações concorrentes serializam pelo contrato | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ComposicaoPublicacaoConcorrenciaHttpIT#publicacaoAguardaMutacaoDaComposicaoENaoPublicaFotografiaDepoisRemovida` | APROVADO_XML_CANDIDATA | 1 |

## CMP-003 — PRESERVADO

Soma do efetivo das composições deve ser igual ao efetivo total vigente do contrato.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-003:001 | Publicação soma diferente=400 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-003:002 | Novo total incompatível com deltas=400 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-003:003 | Retroativo+posterior mantém total igual à soma | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-003:004 | Remanejamento total constante | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-003:005 | Somas reais devem coincidir com total sem overflow. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#totalDoAditamentoDeveConferirComDeltasSemMutacaoParcial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.regra.RegraEfetivoContratualTest#remanejamentoExigeSomaZero` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#publicacaoRecusaSomaQueSoCoincideComTotalPorOverflow` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#aditamentoRecusaTotalResultanteQueSofreriaOverflow` | APROVADO_XML_CANDIDATA | 1 |

## CMP-004 — PRESERVADO

Previsto zero significa combinação indisponível para nova alocação, distinta de permitir excedente.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-004:001 | Zero retorna permiteNovaAlocacao=false | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-004:002 | Inclusão mesmo confirmada como excedente=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-004:003 | Histórico da composição permanece | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura` | APROVADO_XML_CANDIDATA | 1 |

## CMP-005 — PRESERVADO

Composição zerada que volta a ter efetivo retorna por nova versão temporal, preservando o período indisponível.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-005:001 | Antes do retorno capacidade zero | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-005:002 | Na data nova capacidade positiva | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-005:003 | Nova alocação permitida somente a partir do retorno | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-005:004 | Instrumentos preservados | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional` | APROVADO_XML_CANDIDATA | 1 |

## CMP-006 — PRESERVADO

Não zerar/inativar composição enquanto houver vínculos/programações futuras não tratadas; a conclusão deve ser impedida.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-006:001 | Prévia aponta programação impactada | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-006:002 | Confirmação=409 se dependência continua | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-006:003 | Contrato/instrumentos/pessoas não mudam no bloqueio | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-006:004 | Após tratamento explícito pode confirmar | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ImpactoCapacidadeLimitesHttpIT#previaDeCapacidadeContaTodosOsOcupantesELimitaAmostraDePessoas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#zeramentoNaoPodeDeixarProgramacaoFuturaSemComposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#remanejamentoNaoZeraComposicaoComVinculoProgramadoDepoisDoEfeito` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT#cancelarRemanejamentoQueAtivouComposicaoComVinculoFuturoEhBloqueadoSemMutacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#novaProgramacaoInvalidaPreviaEReducaoSoConfirmaAposCancelarDependencia` | APROVADO_XML_CANDIDATA | 1 |

## CMP-007 — PRESERVADO

Alteração que remove/reduz combinação ocupada deve identificar impactos, não mover pessoas automaticamente, exigir tratamento explícito e preservar história.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-007:001 | Impactos incluem ocupação e dependências futuras | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-007:002 | Prévia assinada muda quando dependência muda | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-007:003 | Ocupação incompatível bloqueia mesmo confirmarImpacto=true | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-007:004 | Pessoa não muda automaticamente de combinação | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ImpactoCapacidadeLimitesHttpIT#previaDeCapacidadeContaTodosOsOcupantesELimitaAmostraDePessoas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#novaProgramacaoInvalidaPreviaEReducaoSoConfirmaAposCancelarDependencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarAditamentoParaSomenteDescricaoNaoPodeRetirarCapacidadeDeVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarRemanejamentoParaOutroDestinoNaoPodeRetirarCapacidadeDeVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT#cancelarRemanejamentoQueAtivouComposicaoComVinculoFuturoEhBloqueadoSemMutacao` | APROVADO_XML_CANDIDATA | 1 |

## CMP-008 — PRESERVADO

Excedente de colaboradores acima do previsto é permitido com alerta; não é bloqueio rígido de vínculo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-008:001 | Sem confirmação inclusão excedente rejeita com aviso | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-008:002 | Com confirmação/justificativa Gestor autoriza | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-008:003 | Acima de 100% persiste | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial` | APROVADO_XML_CANDIDATA | 1 |

## CMP-009 — PRESERVADO

Nova inclusão acima do previsto é autorizada pelo Gestor Local, com alerta forte, justificativa e auditoria.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-009:001 | Perfil sem gestão não inclui | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-009:002 | Justificativa obrigatória em excesso | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-009:003 | Registro vincula autor e prova | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-009:004 | Consulta excedente recupera justificativa | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#mutacoesDePessoaVinculoEAlocacaoExigemGestorLocalAtivo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT#consentimentoDeExcedenteSemJustificativaRecusaSemGravar` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasComposicaoConsentimentoHttpIT#excedenteAutorizadoPersisteAutoriaDocumentosDataECapacidadeNoEvento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRegrasOriginaisHttpIT#restauracaoComExcedenteEmDuasCombinacoesExibeJustificativaEmCadaFaixa` | APROVADO_XML_CANDIDATA | 1 |

## CMP-010 — PRESERVADO

Não há teto técnico absoluto de excedente; sistema alerta e mantém identificação do excedente.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-010:001 | Inclusões justificadas continuam possíveis acima da capacidade | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-010:002 | Contagem mostra diferença real | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-010:003 | Zero continua vedado por regra diferente | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura` | APROVADO_XML_CANDIDATA | 1 |

## CMP-011 — PRESERVADO

Enquanto existir excedente, ele deve permanecer monitorável, exibindo no mínimo composição, efetivo previsto, quantidade vinculada, excedente, desde quando existe e justificativas relacionadas; deixa de existir quando o quantitativo volta a caber no previsto ou quando alteração contratual válida absorve o excedente.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| CMP-011:001 | Excedente 2-1 informa 1 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-011:002 | desdeQuando coincide com início do episódio | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-011:003 | Justificativa presente | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| CMP-011:004 | Aumento a 2 zera excedente sem apagar histórico | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRegrasOriginaisHttpIT#restauracaoComExcedenteEmDuasCombinacoesExibeJustificativaEmCadaFaixa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#aumentoAbsorveExcedenteSemReescreverEpisodioAnterior` | APROVADO_XML_CANDIDATA | 1 |

## RED-001 — PRESERVADO

Reduções contratuais possuem limite máximo global de 25% e esse limite é cumulativo ao longo das reduções válidas.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| RED-001:001 | 100→90→75 permitido | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-001:002 | Abaixo de75 sem exceção bloqueado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-001:003 | Replay posterior que viola piso gera conflito sem aplicar redução | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#reducaoRetroativaLegalReavaliaPisoDosAditamentosPosteriores` | APROVADO_XML_CANDIDATA | 1 |

## RED-002 — PRESERVADO

Limite de 25% é verificado sobre o total do contrato, não individualmente por composição.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| RED-002:001 | Reduzir composição25→0 em total101 para76 permitido | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-002:002 | Total ainda deve respeitar piso | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-002:003 | Ocupação deve ser tratada | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#zeramentoNaoPodeDeixarProgramacaoFuturaSemComposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |

## RED-003 — PRESERVADO

Uma composição pode reduzir mais de 25%, inclusive a zero, se a redução global do contrato respeitar o limite e os impactos forem tratados.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| RED-003:001 | Zero sem dependentes e piso respeitado é permitido | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-003:002 | Zero com futuro dependente bloqueia | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-003:003 | Não mover pessoas automaticamente | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#zeramentoNaoPodeDeixarProgramacaoFuturaSemComposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#remanejamentoNaoZeraComposicaoComVinculoProgramadoDepoisDoEfeito` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |

## RED-004 — PRESERVADO

Aumento contratual não possui limite de 25%.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| RED-004:001 | 100→140 aceito | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-004:002 | Aumento superior a25 não exige exceção de redução | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-004:003 | Soma da composição acompanha aumento | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal` | APROVADO_XML_CANDIDATA | 1 |

## RED-005 — PRESERVADO

Redução superior ao limite é bloqueada no fluxo comum; exceção só se houver regra negocial/jurídica expressamente aprovada.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| RED-005:001 | 74 para base100=409 comum | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-005:002 | Flag sozinha sem fundamento rejeita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-005:003 | Exceção completa passa com Gestão responsável | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao` | APROVADO_XML_CANDIDATA | 1 |

## RED-006 — PRESERVADO

O piso contratual é 75% do efetivo total originalmente publicado; aumentos posteriores não mudam a base, e apenas reduções válidas consomem o limite cumulativo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| RED-006:001 | 100→140→75 permitido | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-006:002 | Base permanece100 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-006:003 | Retificar/cancelar remove efeito antigo do cálculo | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-006:004 | Replay revalida piso após retirada de aumento | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-006:005 | Piso original continua valido em toda faixa numerica admitida pelo modelo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.regra.RegraEfetivoContratualTest#contrato100TemPiso75MesmoDepoisDeAumento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#cancelarAumentoNaoPermiteQueReducaoPosteriorReaplicadaUltrapassePisoOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#indicacaoDeExcecaoEmReducaoNormalNaoCriaAutorizacaoExcepcionalParaReplay` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#pisoGlobalMantemArredondamentoNoMaiorEfetivoRepresentavel` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#reducaoDeEfetivoGrandeContinuaSujeitaAoPisoOriginal` | APROVADO_XML_CANDIDATA | 1 |

## RED-007 — PRESERVADO

Quando o piso percentual resultar fracionário, o piso mínimo deve ser arredondado para cima para o próximo número inteiro.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| RED-007:001 | Base101 exige76 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-007:002 | Base100 exige75 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-007:003 | Resultado75 com base101 bloqueia | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-007:004 | Arredondamento conserva precisao no maior inteiro admitido. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.regra.RegraEfetivoContratualTest#contrato101ArredondaPisoPara76` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.regra.RegraEfetivoContratualTest#contrato100TemPiso75MesmoDepoisDeAumento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#pisoGlobalMantemArredondamentoNoMaiorEfetivoRepresentavel` | APROVADO_XML_CANDIDATA | 1 |

## RED-008 — PRESERVADO

Exceção acima de 25% exige fundamento jurídico/negocial documentado, fonte comprobatória, permissão técnica excepcional para Gestor Local, prévia de impacto, justificativa e auditoria reforçada.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| RED-008:001 | Falta fundamento=400 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-008:002 | Falta concessão=403 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-008:003 | Sem confirmação=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-008:004 | Exceção documentada autorizada persiste | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| RED-008:005 | Flag de exceção em redução normal não autoriza violação futura por replay | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#indicacaoDeExcecaoEmReducaoNormalNaoCriaAutorizacaoExcepcionalParaReplay` | APROVADO_XML_CANDIDATA | 1 |

## ADI-001 — PRESERVADO

Contrato pode possuir vários aditamentos sem apagar registros anteriores.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ADI-001:001 | Dois instrumentos aparecem na lista | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-001:002 | Reenvio não duplica | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-001:003 | Retificação versão2 mantém versão1 | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional` | APROVADO_XML_CANDIDATA | 1 |

## ADI-002 — PRESERVADO

Aditamento possui identificação, datas/documento, data efetiva, justificativa, responsável e fonte comprobatória.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ADI-002:001 | Campos obrigatórios ausentes rejeitam | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-002:002 | Sem fonte=400 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-002:003 | Autor/registro são servidor | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-002:004 | Assinatura opcional preservada quando informada | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#aditamentoExigeCadaCampoBasicoSemRegistrarInstrumento` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional` | APROVADO_XML_CANDIDATA | 1 |

## ADI-003 — PRESERVADO

Aditamento quantitativo informa combinação afetada, quantidade anterior, variação e quantidade resultante.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ADI-003:001 | Aumento100→140 registra anterior100,variação40,resultante140 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-003:002 | Histórico mantém esses valores após novos fatos | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados` | APROVADO_XML_CANDIDATA | 1 |

## ADI-004 — PRESERVADO

Aditamento pode aumentar ou reduzir, observadas as regras de redução e impacto.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ADI-004:001 | Aumento e redução válidos aceitos | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-004:002 | Abaixo piso comum=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-004:003 | Redução ocupada/futuro órfão=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-004:004 | Falha deixa toda transação intacta | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#zeramentoNaoPodeDeixarProgramacaoFuturaSemComposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarAditamentoParaSomenteDescricaoNaoPodeRetirarCapacidadeDeVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |

## ADI-005 — PRESERVADO

Aditamento retroativo é permitido e sua data efetiva define quando passa a valer na reconstrução histórica.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ADI-005:001 | Fato antigo digitado depois produz efeito no dia certo | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-005:002 | Posterior válido continua aplicado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-005:003 | Nova frequência após prévia invalida token | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-005:004 | Versões anteriores preservadas | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoRetroativoRespeitaAssociacaoPublicadaAposRemocaoNoCatalogoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosTemporalLacunasHttpIT#novaFrequenciaInvalidaTokenDeAditamentoRetroativo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLegadoPoliticaComplementarHttpIT#primeiroAditamentoNaoSubstituiFotografiaOriginalLegadaSemEventoDeOrigem` | APROVADO_XML_CANDIDATA | 1 |

## ADI-006 — PRESERVADO

Aditamento futuro pode ser cancelado antes de produzir efeito; não há exclusão física, e cancelamento tem motivo/autor/data.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ADI-006:001 | Cancelar futuro remove efeito | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-006:002 | Cancelar efetivo rejeita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-006:003 | Original e cancelamento auditáveis | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-006:004 | Dependente futuro incompatível bloqueia cancelamento | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoJaEfetivoNaoPodeSerCanceladoComoProgramacao` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT#cancelarAditamentoQueReativouComposicaoComVinculoFuturoEhBloqueadoSemMutacao` | APROVADO_XML_CANDIDATA | 1 |

## ADI-007 — PRESERVADO

Retificação preserva original e possui data efetiva explícita, valor correto pretendido e variação/diferença aplicada.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ADI-007:001 | Versão2 com RETIFICA preserva versão1 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-007:002 | Retificação subsequente conserva cadeia | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-007:003 | Capacidade omitida na retificação é revalidada | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-007:004 | Resultado temporal usa versão vigente | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAditamentoRetificacaoQuantitativaHttpIT#retificacoesQuantitativasPreservamValoresOriginaisECortesDeConhecimentoSemSomarVersoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarAditamentoParaSomenteDescricaoNaoPodeRetirarCapacidadeDeVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |

## ADI-008 — PRESERVADO

Se retificação torna eventos posteriores incompatíveis, mostrar impacto e gerar pendência; posteriores válidos são preservados.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ADI-008:001 | Instrumento posterior continua na lista | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-008:002 | Incompatibilidade gera pendência ligada ao evento | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-008:003 | Não aplica delta incompatível parcialmente | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-008:004 | Com dependente operacional impede operação | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAditamentoRetificacaoQuantitativaHttpIT#retificarAntecedentePreservaPosteriorIncompativelESuspendeTodaSuaRedistribuicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosPisoReplayHttpIT#reducaoRetroativaLegalReavaliaPisoDosAditamentosPosteriores` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |

## ADI-009 — PRESERVADO

Dois aditamentos conflitantes na mesma data precisam de sequência negocial explícita; não usar ordem de gravação.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ADI-009:001 | Sem precedência colisão=409 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-009:002 | SUCEDE válido decide valor | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-009:003 | Sucessores bifurcados sem relação entre si bloqueiam | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-009:004 | Recortes distintos coexistem | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosPrecedenciaBifurcadaHttpIT#mesmaDataNaoAdmiteDoisSucessoresConflitantesSemOrdemEntreEles` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoPrecedenciaExplicitaHttpIT#coexistenciaMantemCamposDistintosMasNaoAutorizaDuasDescricoesConflitantes` | APROVADO_XML_CANDIDATA | 1 |

## ADI-010 — PRESERVADO

Aditamento não altera diretamente publicação, inativação, reativação, encerramento, cancelamento ou anulação; esses estados possuem eventos/operações próprios.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ADI-010:001 | Campos estado/ativo no JSON não mudam estado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-010:002 | RETIFICA/SUBSTITUI/CANCELA apontando evento de ciclo é recusado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ADI-010:003 | SUCEDE misto válido pode ordenar sem invalidar estado | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#aditamentoNaoMudaEstadoNegocialOuHabilitacaoPorCampoEstranhoNoJson` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida` | APROVADO_XML_CANDIDATA | 6 |
| `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#precedenciaDeOrdemEntreTiposDiferentesContinuaPermitida` | APROVADO_XML_CANDIDATA | 1 |

## REM-001 — PRESERVADO

Remanejamento é fato próprio, identificado, datado, justificado e auditado.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REM-001:001 | Registro contém identificação/data/justificativa/autor | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-001:002 | Itens guardam anterior/variação/resultante | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-001:003 | História usa tipo REMANEJAMENTO | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico` | APROVADO_XML_CANDIDATA | 1 |

## REM-002 — PRESERVADO

Remanejamento mantém o efetivo total: soma das variações efetivamente aplicadas deve ser zero.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REM-002:001 | -5/+5 preserva100 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-002:002 | -5/+4 rejeita400 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-002:003 | Replay inválido não aplica metade da redistribuição | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-002:004 | Soma zero deve ser matematica, nao resultado do descarte de bits altos. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.regra.RegraEfetivoContratualTest#remanejamentoExigeSomaZero` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoRecusaNovaCombinacaoOuSomaNaoZero` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayCapacidadeDependenteHttpIT#remanejamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAditamentoRetificacaoQuantitativaHttpIT#retificarAntecedentePreservaPosteriorIncompativelESuspendeTodaSuaRedistribuicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#variacoesQueSomamDoisElevadoA32NaoSaoTratadasComoRedistribuicaoNeutra` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.regra.ConclusaoEfetivoContratualTest#variacoesGrandesRealmenteCompensadasPreservamTotal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoEfetivoContratoHttpIT#remanejamentoRecusaVariacoesCujaSomaRealNaoEhZero` | APROVADO_XML_CANDIDATA | 1 |

## REM-003 — PRESERVADO

Remanejamento só redistribui entre combinações válidas já existentes na data; criação de nova combinação é aditamento.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REM-003:001 | Composição inexistente=400 | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-003:002 | Vigência fora do contrato rejeita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-003:003 | Chave repetida rejeita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-003:004 | Nenhuma nova composição criada pelo REM | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoRecusaNovaCombinacaoOuSomaNaoZero` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#remanejamentoForaDaVigenciaNaoRegistraFatoNemComposicao` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoRejeitaCombinacaoRepetidaSemPersistirFato` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria` | APROVADO_XML_CANDIDATA | 1 |

## REM-004 — PRESERVADO

Remanejamento pode ser retificado/versionado, preservando original e cadeia completa.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REM-004:001 | Versão2 mantém original RETIFICADO | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-004:002 | Repetir retificação conserva cadeia | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-004:003 | Trocar destino não pode retirar capacidade de futuro ocupado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-004:004 | Não invalidar fato de outro domínio | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RetificacaoCapacidadeContratualHttpIT#retificarRemanejamentoParaOutroDestinoNaoPodeRetirarCapacidadeDeVinculoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida` | APROVADO_XML_CANDIDATA | 6 |

## REM-005 — PRESERVADO

Anexo de remanejamento é opcional; pode haver referência SEI para rastreabilidade.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REM-005:001 | REM sem anexo e sem SEI aceita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-005:002 | Fonte informada permanece vinculada | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-005:003 | Ausência de fonte não causa erro | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.DocumentosInstrumentosPersistenciaHttpIT#remanejamentoComSeiOpcionalPreservaReferenciaEFatoSemExigirArquivo` | APROVADO_XML_CANDIDATA | 1 |

## REM-006 — PRESERVADO

Eventos futuros de remanejamento podem permanecer historicamente efetivos mesmo durante inativação administrativa, sem liberar operação.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REM-006:001 | Composição histórica redistribuída | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-006:002 | Habilitação segue INATIVO | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-006:003 | Nova operação no bloqueio rejeita | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#inativacaoProgramadaImpedeNovoInstrumentoQuandoSuaDataChega` | APROVADO_XML_CANDIDATA | 2 |

## REM-007 — PRESERVADO

Remanejamento futuro pode ser cancelado antes do efeito, preservando original, cancelamento, motivo, usuário, data/hora e fonte comprobatória quando aplicável; o cancelado não produz efeito.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REM-007:001 | Original futuro cancelável | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-007:002 | Efetivo já iniciado rejeita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-007:003 | Posição futura não aplica cancelado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-007:004 | Com futuro dependente incompatível cancelação bloqueia | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REM-007:005 | Histórico conserva original+cancelamento | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#cancelarRemanejamentoFuturoDeveSinalizarOInstrumentoPosteriorQueFicaIncompativel` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#cancelamentoComPosteriorCompativelPreservaDeltasEHistoricoSemCriarPendencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosEspecificosHttpIT#instrumentoJaEfetivoNaoPodeSerCanceladoComoProgramacao` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ContratosCriteriosComplementaresHttpIT#cancelamentoDoInstrumentoPreservaAutorMotivoDatasAlvoEDistribuicaoAnterior` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.CancelamentoCapacidadeContratualHttpIT#cancelarRemanejamentoQueAtivouComposicaoComVinculoFuturoEhBloqueadoSemMutacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#doisCancelamentosQueAtingemMesmoPosteriorReutilizamPendenciaAberta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#rollbackDoCancelamentoTambemDesfazPendenciaERelacaoNaMesmaTransacao` | APROVADO_XML_CANDIDATA | 1 |

## DOC-001 — PRESERVADO

Contrato original publicado deve ter pelo menos uma fonte comprobatória: anexo, número/processo SEI ou ambos.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DOC-001:001 | Publicação exige SEI, anexo ou ambos | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-001:002 | Rejeitar fonte vazia | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-001:003 | Anexo suficiente sem SEI e SEI suficiente sem arquivo | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.DocumentosContratuaisHttpIT#documentoRascunhoExclusivoPodeSerExcluidoEPublicacaoAceitaSomenteAnexo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado` | APROVADO_XML_CANDIDATA | 1 |

## DOC-002 — PRESERVADO

Aditamento deve ter pelo menos uma fonte comprobatória: anexo e/ou SEI.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DOC-002:001 | Aditamento deve ter fonte documental válida | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-002:002 | Fonte acompanha fato e identidade da versão | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-002:003 | Sem fonte recusar gravação | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.DocumentosInstrumentosPersistenciaHttpIT#aditamentoSemSeiPublicaAnexoPreparadoEVinculaFonteAoInstrumentoCorreto` | APROVADO_XML_CANDIDATA | 1 |

## DOC-003 — PRESERVADO

Remanejamento pode registrar SEI e mantém anexo opcional.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DOC-003:001 | Remanejamento aceita ausência de arquivo | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-003:002 | SEI opcional permanece vinculado ao fato | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-003:003 | Cadeia de versões preservada | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.DocumentosInstrumentosPersistenciaHttpIT#remanejamentoComSeiOpcionalPreservaReferenciaEFatoSemExigirArquivo` | APROVADO_XML_CANDIDATA | 1 |

## DOC-004 — PRESERVADO

Um mesmo processo SEI pode referenciar vários contratos; SEI não é identificador único do contrato.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DOC-004:001 | SEI não tem unicidade de contrato | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-004:002 | Dois contratos com mesma referência são publicados com identidades próprias | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#seiCompartilhadoNaoViraIdentificadorUnicoDeContrato` | APROVADO_XML_CANDIDATA | 1 |

## DOC-005 — PRESERVADO

Número SEI incorreto em fato publicado é corrigido por nova versão/auditoria, preservando a referência anterior.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DOC-005:001 | Nova referência SEI cria versão | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-005:002 | Referência anterior permanece consultável | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-005:003 | Motivo e autor são registrados | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-005:004 | Reenvio com intenção divergente não muda a cadeia | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa08.DocumentoComprobatorioServicoEtapa08Test#idempotenciaDaRetificacaoComparaDocumentoAlvo` | APROVADO_XML_CANDIDATA | 1 |

## DOC-006 — PRESERVADO

Contrato publicado inicialmente apenas com SEI pode receber anexo posteriormente, sem substituir silenciosamente o SEI.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DOC-006:001 | Anexo pode chegar após publicação com SEI | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-006:002 | A nova versão preserva referência SEI e vínculo anterior | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-006:003 | Download confere checksum do arquivo | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa08.DocumentoComprobatorioServicoEtapa08Test#downloadComArquivoAdulteradoEhBloqueadoPorChecksum` | APROVADO_XML_CANDIDATA | 1 |

## DOC-007 — PRESERVADO

Arquivo associado a fato publicado e enviado errado não é apagado fisicamente; é marcado inválido/substituído e permanece auditável.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DOC-007:001 | Documento de fato publicado não é excluído fisicamente | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-007:002 | Invalidação/substituição preserva metadados e bytes | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-007:003 | Cancelar ocorrência preserva fonte e permite auditoria autorizada | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaProvaPreservadaHttpIT#retiradaDaOcorrenciaPreservaFatoEArquivoJaReferenciadosPelaCadeiaEEmissao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#anexoNegocialRevalidaContextoEfetivoAteDepoisDoCancelamento` | APROVADO_XML_CANDIDATA | 1 |

## DOC-008 — PRESERVADO

Em rascunho nunca publicado, anexos/dependências exclusivas podem ser removidos junto com o descarte, salvo valor auditável externo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DOC-008:001 | Rascunho nunca publicado admite descarte transacional | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-008:002 | Preservar dependência que possui valor documental externo | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-008:003 | Exclusão física só após commit | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT#descarteFisicoAcompanhaConfirmacaoDaTransacaoSemApagarArquivoEmRollback` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.DocumentosRascunhoCommitRollbackHttpIT#referenciaExternaNoRascunhoImpedeDescarteEPreservaMetadadosEArquivo` | APROVADO_XML_CANDIDATA | 1 |

## DOC-009 — PRESERVADO

Documentos anexados possuem classificação de acesso própria, distinguindo no mínimo documento negocial e documento pessoal/restrito.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DOC-009:001 | Cada anexo possui classificação própria | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-009:002 | NEGOCIAL e PESSOAL_RESTRITO têm políticas diferentes | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-009:003 | Metadados também respeitam classificação | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-009:004 | Classificação explícita por anexo; legado sem classificação tratado como pessoal/restrito; hash SHA-256 dos novos bytes; metadados pessoais omitidos de consultas minimizadas. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.DocumentosContratuaisHttpIT#uploadValidaClassificacaoFormatoESigiloPorEscopo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#classificacaoInvalidaOuQuantidadeIncompativelNaoPublicaArquivoNemFato` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#anexoNegocialRevalidaContextoEfetivoAteDepoisDoCancelamento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#anexoLegadoSemClasseOuHashMantemRestricaoENaoRecebeCertificacaoRetroativa` | APROVADO_XML_CANDIDATA | 1 |

## DOC-010 — PRESERVADO

Documento Pessoal/Restrito possui acesso por necessidade e permissão específica conforme perfil; Presidente/Operacional não acessam por padrão, Administrador técnico somente em suporte excepcional autorizado, e todo acesso é auditado.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DOC-010:001 | Conteúdo restrito exige necessidade e perfil/concessão | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-010:002 | Operacional não lê documento pessoal | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-010:003 | Acesso permitido é auditado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-010:004 | Revogação/expiração aplica-se a cada leitura | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DOC-010:005 | Download revalida todos os dias do contexto do fato, perfil, concessão vigente e finalidade; registra uso auditável. Revogação não é contornada por cache. Operacional não recebe documento pessoal; Presidente somente mediante permissão explícita. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.DocumentosContratuaisHttpIT#documentoRestritoExigeFinalidadePermissaoEAuditaAcessoComRevogacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#administracaoAcessaDocumentoPessoalApenasDuranteSuporteExcepcionalAuditado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#presidenciaAcessaDocumentoPessoalSomenteComConcessaoEspecificaAuditada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoDownloadsAutorizacaoAtualHttpIT#downloadRestritoRevalidaRevogacaoSemAlterarBytesOuAuditoriaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoDownloadExpiracaoHttpIT#concessaoValidaDeixaDeAutorizarBytesDepoisDoPrazoSemRevogacaoOuAlteracaoDaProva` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#ocorrenciaIndivisivelDeDoisDiasExigeEscopoEmTodoOPeriodo` | APROVADO_XML_CANDIDATA | 1 |

## COL-001 — PRESERVADO

Nome e CPF são obrigatórios.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-001:001 | Cadastro integrado sem nome/CPF é recusado sem persistência parcial. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-001:002 | Nome não vazio é normalizado e preservado na identidade canônica. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#nomeECpfInvalidosRecusamCadastroSemPessoaProjecaoVinculoAlocacaoOuReservaParcial` | APROVADO_XML_CANDIDATA | 10 |

## COL-002 — PRESERVADO

RG, CTPS, NIT/PIS, data de nascimento e e-mail são opcionais.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-002:001 | RG/CTPS/NIT-PIS/data de nascimento/e-mail podem ser omitidos. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-002:002 | Quando informados, os opcionais são persistidos no cadastro de Pessoa e expostos conforme perfil. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa06.PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#todosOpcionaisInformadosPossuemValoresExatosPersistidosERecontratacaoNaoOsApaga` | APROVADO_XML_CANDIDATA | 1 |

## COL-003 — PRESERVADO

Endereço é mantido e opcional.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-003:001 | Ausência de endereço é aceita. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-003:002 | Endereço informado é mantido no cadastro; remoção exige ação explícita. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#todosOpcionaisInformadosPossuemValoresExatosPersistidosERecontratacaoNaoOsApaga` | APROVADO_XML_CANDIDATA | 1 |

## COL-004 — PRESERVADO

Dados bancários foram removidos do cadastro deste fluxo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-004:001 | Cadastro canônico de Pessoa e seus contratos de entrada/saída não incluem dados bancários. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cadastroPermanenteNaoPossuiDadosBancariosNemMatricula` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | APROVADO_XML_CANDIDATA | 1 |

## COL-005 — PRESERVADO

CPF é válido, único globalmente, persistido sem pontuação e mascarado apenas na interface.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-005:001 | CPF valida tamanho e dígitos verificadores e rejeita sequências repetidas. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-005:002 | CPF é normalizado sem pontuação antes da persistência. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-005:003 | Unicidade global é garantida inclusive em cadastros concorrentes no MySQL. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cpfValidoEhNormalizadoSemPontuacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cpfInvalidoEhBloqueado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#nomeECpfInvalidosRecusamCadastroSemPessoaProjecaoVinculoAlocacaoOuReservaParcial` | APROVADO_XML_CANDIDATA | 10 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalCorrecaoCpfControlesHttpIT#cpfNaoUniformeComDigitoAdulteradoRecusaCorrecaoEPreservaIdentidadeETrilha` | APROVADO_XML_CANDIDATA | 1 |

## COL-006 — PRESERVADO

CPF não é editável no fluxo comum. Correção excepcional de erro material comprovado é executada pelo Gestor Local e deve registrar motivo, evidência, CPF anterior, CPF novo, responsável, autorização, data/hora e impactos encontrados, além de validar o novo CPF.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-006:001 | Atualização comum de opcionais não permite alterar CPF. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-006:002 | Correção excepcional exige Gestor Local autorizado no escopo, motivo, evidência, autorização e impactos declarados. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-006:003 | Novo CPF é validado; ato preserva anterior/novo/responsável/data de registro sem fundir identidade. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-006:004 | CPF anterior deixa de ser identificador ativo, podendo ser usado por titular distinto após validação sem apropriar a história da Pessoa original. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#correcaoExcepcionalDeIdentidadeExigeDeclaracaoDosImpactosEncontrados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasCriteriosComplementaresHttpIT#correcaoCpfDocumentadaEColisaoPreservamAsDuasIdentidades` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalCorrecaoCpfControlesHttpIT#cpfNaoUniformeComDigitoAdulteradoRecusaCorrecaoEPreservaIdentidadeETrilha` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalCorrecaoCpfControlesHttpIT#correcoesDePessoasDistintasDisputamMesmoCpfComEsperaRealERollbackDaPerdedora` | APROVADO_XML_CANDIDATA | 1 |

## COL-007 — PRESERVADO

Se o CPF corrigido já pertence a outra pessoa, bloquear correção automática e tratar duplicidade/identidade excepcionalmente; nunca fundir silenciosamente.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-007:001 | Correção para CPF ativo de outra Pessoa é recusada atomicamente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-007:002 | Projeção legada coincidente por CPF não é adotada sem associação canônica explícita. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasCriteriosComplementaresHttpIT#correcaoCpfDocumentadaEColisaoPreservamAsDuasIdentidades` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalCorrecaoCpfControlesHttpIT#correcoesDePessoasDistintasDisputamMesmoCpfComEsperaRealERollbackDaPerdedora` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasIdentidadeLegadaHttpIT#cpfCorrigidoNaoAutorizaAdotarProjecaoLegadaSemAssociacao` | APROVADO_XML_CANDIDATA | 1 |

## COL-009 — PRESERVADO

Nome não é editável no fluxo normal; erro material ou mudança legal pode ser corrigido excepcionalmente e auditado, sem criar nova pessoa.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-009:001 | Nome não pode ser alterado pelo fluxo comum de opcionais. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-009:002 | Correção excepcional auditada mantém o mesmo Pessoa.id e fotografa anterior/novo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#fluxoComumDeOpcionaisNaoAlteraIdentidadeCanonica` | APROVADO_XML_CANDIDATA | 1 |

## COL-010 — BLOQUEADO

Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época.

**Validação:** BLOQUEADO_DOCUMENTAL_COM_TESTES. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-010:001 | Cadastro atual e projeção operacional mostram o nome corrigido. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-010:002 | Novos fatos preservam o nome fotografado e a correção atual não muda eventos/documentos anteriores. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-010:003 | Consulta temporal separa nomeAtual de nomeFotografia e aplica os cortes efetivo e de conhecimento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-010:004 | Registro antigo sem fotografia nominal permanece desconhecido; não se alega recuperação de dado ausente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasFotografiasHistoricasAusentesHttpIT#historicoContratualNaoCompletaNomeAusenteAposCorrecaoDaPessoa` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#atoDeMatriculaRetroativoNaoEmprestaNomeAtualAoPeriodoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#ausenciaTotalDeFotoNominalPermaneceDesconhecidaDepoisDaCorrecaoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaNominalHttpIT#ausenciaNominalPreservaDesconhecimentoRetroativoECapturaContemporanea` | APROVADO_XML_CANDIDATA | 2 |

## COL-011 — PRESERVADO

Na recontratação, campos opcionais omitidos significam manter dados atuais; limpeza exige ação explícita.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-011:001 | Recontratação com pessoaId preserva campos opcionais omitidos. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-011:002 | Limpeza usa indicadores explícitos, mantendo os demais opcionais. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa06.PessoaServicoEtapa06Test#omissaoNaAtualizacaoMantemValorELimpezaExigeIndicador` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#todosOpcionaisInformadosPossuemValoresExatosPersistidosERecontratacaoNaoOsApaga` | APROVADO_XML_CANDIDATA | 1 |

## COL-012 — PRESERVADO

Dados pessoais atuais e fotografia histórica são separados: cadastro mostra valor atual, histórico negocial usa fotografia registrada na época.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-012:001 | Dados atuais pertencem à Pessoa canônica; histórico usa dados fotografados no fato. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-012:002 | Ausência de fotografia é distinta de valor atual e nunca é completada implicitamente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#ausenciaTotalDeFotoNominalPermaneceDesconhecidaDepoisDaCorrecaoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaNominalHttpIT#ausenciaNominalPreservaDesconhecimentoRetroativoECapturaContemporanea` | APROVADO_XML_CANDIDATA | 2 |

## COL-013 — PRESERVADO

Pessoa é única por CPF; recontratação reutiliza a mesma pessoa e cria novo vínculo, não nova pessoa.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| COL-013:001 | Uma Pessoa é identificada globalmente por CPF atual e ID estável. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| COL-013:002 | Recontratação reutiliza essa Pessoa e cria outro Vínculo, sem duplicar identidade ou projeção. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.EmpregadoContextoCanonicoControlesHttpIT#recontratacaoMantemIdentidadeEHistoricoSemAnteciparContextoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#recontratacaoComPessoaIdPreservaOpcionaisOmitidosENovoVinculo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#todosOpcionaisInformadosPossuemValoresExatosPersistidosERecontratacaoNaoOsApaga` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRecontratacaoAposInativacaoHttpIT#recontratacaoDepoisDeInativarProjecaoEncerradaVoltaAConsultaGradeEFrequenciaSemDuplicarIdentidade` | APROVADO_XML_CANDIDATA | 7 |

## MAT-001 — PRESERVADO

Matrícula pertence ao Vínculo Contratual, não ao cadastro permanente da Pessoa.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| MAT-001:001 | Matrícula reside no Vínculo e nas reservas por Contrato, não no cadastro permanente da Pessoa. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| MAT-001:002 | Consulta temporal aplica cada alteração de matrícula na data efetiva. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#cadastroPermanenteNaoPossuiDadosBancariosNemMatricula` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#alteracaoDeMatriculaSoValeAPartirDaDataEfetiva` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#atoDeMatriculaRetroativoNaoEmprestaNomeAtualAoPeriodoAnterior` | APROVADO_XML_CANDIDATA | 1 |

## MAT-002 — PRESERVADO

Matrícula é opcional no vínculo e única dentro do contrato.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| MAT-002:001 | Matrícula é opcional na ausência de política obrigatória. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| MAT-002:002 | Valor normalizado não pode ser atribuído a outro vínculo do mesmo Contrato. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#matriculaEhNormalizadaNoDominioDoVinculo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasCriteriosComplementaresHttpIT#matriculaNormalizadaConflitaSomenteDentroDoMesmoContrato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasMatriculaConcorrenciaHttpIT#cpfsDistintosDisputamMesmaMatriculaSemGravacaoParcialDaTentativaPerdedora` | APROVADO_XML_CANDIDATA | 1 |

## MAT-003 — PRESERVADO

Matrícula permanece reservada historicamente no contrato mesmo após desligamento, alteração ou programação cancelada.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| MAT-003:001 | Desligamento mantém a reserva histórica. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| MAT-003:002 | Alteração mantém também a reserva anterior. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| MAT-003:003 | Cancelamento de programação não libera a matrícula. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia` | APROVADO_XML_CANDIDATA | 1 |

## MAT-004 — PRESERVADO

Nem a própria pessoa reutiliza matrícula antiga em recontratação; novo vínculo recebe nova matrícula.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| MAT-004:001 | Novo vínculo da mesma Pessoa não pode reutilizar matrícula já reservada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| MAT-004:002 | Matrícula diferente permite recontratação válida. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia` | APROVADO_XML_CANDIDATA | 1 |

## MAT-005 — PRESERVADO

Matrícula é opcional por padrão, mas Tipo de Contratação/Contrato pode torná-la obrigatória; quando obrigatória, vínculo não pode ser ativado/publicado sem matrícula válida.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| MAT-005:001 | Política padrão é opcional; contrato pode fixar regra ou herdar a do Tipo fotografado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| MAT-005:002 | Inclusão/ativação e toda faixa atingida por política obrigatória exigem matrícula válida. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| MAT-005:003 | Mudança futura/transferência revalida vínculos existentes e programados nas fronteiras pertinentes. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#obrigatoriedadeDaMatriculaPodeSerHerdadaDoTipo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#contratoPodeSobrescreverRegraDeMatriculaDoTipo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasCriteriosTemporaisComplementaresHttpIT#retificarInicioRevalidaMatriculaNaPoliticaDaNovaData` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasPoliticaMatriculaFuturaHttpIT#cadastroSemMatriculaRecusaObrigacaoJaProgramadaDentroDoPeriodo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasPoliticaMatriculaFuturaHttpIT#cadastroRespeitaFimAnteriorOuMatriculaInformadaSemBloqueioGenerico` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#transferenciaParaTipoObrigatorioRecusaVinculoSemMatriculaSemMudarFatos` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#overrideContratualOpcionalPermiteTransferirParaTipoObrigatorio` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#matriculaInformadaPermiteTransferenciaComHerancaObrigatoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.TransferenciaPoliticaMatriculaHttpIT#aditamentoPosteriorDispensandoMatriculaAntesDoInicioProgramadoEhRespeitadoNaTransferencia` | APROVADO_XML_CANDIDATA | 1 |

## VIN-001 — PRESERVADO

Pessoa + primeiro vínculo + alocação são cadastrados em operação integrada/transacional.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-001:001 | Pessoa nova, primeiro Vínculo e Alocação inicial são gravados na mesma transação. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-001:002 | Falha posterior reverte todos os componentes e permite nova tentativa íntegra. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#cadastrosConcorrentesComMesmoCpfConfirmamSomenteUmaPessoa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#nomeECpfInvalidosRecusamCadastroSemPessoaProjecaoVinculoAlocacaoOuReservaParcial` | APROVADO_XML_CANDIDATA | 10 |

## VIN-002 — PRESERVADO

Antiga data de admissão é substituída por início efetivo do vínculo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-002:001 | O fato de início usa dataInicio efetiva do Vínculo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-002:002 | Pesquisa de início não usa mudança de alocação nem dataAdmissao da projeção como novo vínculo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasCriteriosComplementaresHttpIT#pesquisaDeInicioUsaMesEfetivoEmVezDoRegistro` | APROVADO_XML_CANDIDATA | 1 |

## VIN-003 — PRESERVADO

Não pode haver sobreposição de vínculos da mesma pessoa, globalmente, inclusive entre contratos/Gestões diferentes.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-003:001 | Sobreposição é verificada globalmente por Pessoa, inclusive contratos/Gestões distintos. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-003:002 | Locks e revalidação impedem sobreposição em cadastro/retificações concorrentes. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#vinculosConcorrentesDaMesmaPessoaNaoSobrepoemEntreContratos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest#cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#inicioNoMesmoUltimoDiaConflitaMasDiaSeguintePermiteRecontratacaoEntreGestoes` | APROVADO_XML_CANDIDATA | 1 |

## VIN-004 — PRESERVADO

Troca direta entre contratos é permitida em dias consecutivos: A termina num dia e B começa no seguinte, sem dia de intervalo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-004:001 | Fim inclusivo em D permite vínculo seguinte em D+1. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-004:002 | Fim em D não permite outro vínculo iniciar em D. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#inicioNoMesmoUltimoDiaConflitaMasDiaSeguintePermiteRecontratacaoEntreGestoes` | APROVADO_XML_CANDIDATA | 1 |

## VIN-005 — PRESERVADO

Vínculo futuro pode ser cadastrado como PROGRAMADO e não opera antes da data efetiva.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-005:001 | Vínculo com início futuro retorna PROGRAMADO antes do início. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-005:002 | Programação não torna o colaborador operacionalmente ativo antes da data efetiva. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#vinculoFuturoPermaneceProgramadoAteInicioEfetivo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.EmpregadoContextoCanonicoControlesHttpIT#recontratacaoMantemIdentidadeEHistoricoSemAnteciparContextoFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPessoasLacunasEspecificasHttpIT#programacaoNaoAtivaProjecaoNemPermiteFrequenciaAntesDoInicio` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#antecipacaoDoInicioAtravessaTransferenciaReconstroiOrigemEPreservaConhecimentoAnterior` | APROVADO_XML_CANDIDATA | 1 |

## VIN-006 — PRESERVADO

Programação futura pode ser cancelada antes de começar sem ser tratada como desligamento de vínculo efetivamente ocorrido.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-006:001 | Programação pode ser cancelada antes do início. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-006:002 | Cancelamento não grava desligamento individual de relação nunca efetivada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.VinculoContratualServicoEtapa06Test#cancelamentoDeProgramacaoNaoViraDesligamento` | APROVADO_XML_CANDIDATA | 1 |

## VIN-007 — PRESERVADO

Programação futura cancelada/retificada preserva cadeia completa original → cancelamento/retificação → programação vigente.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-007:001 | Cancelamento/retificação preservam evento original e relacionamento de precedência. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-007:002 | Corte de conhecimento anterior recupera a programação então conhecida. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#retificacaoDeInicioConfirmaPreviaExataEAjustaCoberturaSemRecontratar` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#corteDeConhecimentoPreservaVisaoHistoricaAnteriorARetificacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPessoasLacunasEspecificasHttpIT#cortesAnterioresAoCancelamentoERetificacaoRecuperamCadaProgramacaoConhecida` | APROVADO_XML_CANDIDATA | 1 |

## VIN-008 — PRESERVADO

Ao fim natural da vigência, vínculos abertos ficam operacionalmente inativos a partir do dia seguinte e geram pendência administrativa.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-008:001 | Vínculo aberto perde operação no dia seguinte ao fim natural do Contrato. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-008:002 | Pendência administrativa identifica o vínculo ainda aberto sem encerrá-lo por rotina técnica. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa06.MotorTemporalVinculoEtapa06Test#fimDoContratoInativaOperacionalmenteVinculoAbertoNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa11.PendenciaAdministrativaServicoEtapa11Test#fimNaturalCriaPendenciaAdministrativaPorVinculoAberto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PendenciaEscopoEfetivoHttpIT#fimNaturalAposTransferenciaAtribuiPendenciaAoDestinoEConservaDataReal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPendenciaFimNaturalProrrogacaoHttpIT#consultaNaoCriaPendenciaNoFimAntigoQuandoProrrogacaoJaVigora` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPendenciaFimNaturalProrrogacaoHttpIT#consultaDepoisDoNovoFimGeraUmaPendenciaNaGestaoEfetivaDoNovoFim` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPendenciaFimNaturalProrrogacaoHttpIT#fimReduzidoProgramadoGeraPendenciaAntesDoFimMaterializadoAntigo` | APROVADO_XML_CANDIDATA | 1 |

## VIN-009 — PRESERVADO

Ao tratar a pendência de fim de vigência, a data final do vínculo é a data final do contrato, salvo desligamento real anterior.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-009:001 | Tratamento da pendência usa o fim efetivo do Contrato. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-009:002 | Desligamento real anterior é preservado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#fimAntecipadoPorRescisaoNaoPodeSerReclassificadoComoFimNatural` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#causaContratualComAtoEDataCorretosPermiteRetificacaoExplicitamenteConfirmada` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalFimNaturalProrrogacaoHttpIT#fimNaturalNaoPodeUsarFimMaterializadoQuandoProrrogacaoProgramadaJaProduzEfeito` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalFimNaturalProrrogacaoHttpIT#fimNaturalDepoisDaProrrogacaoUsaNovoFimESuaRepeticaoNaoDuplicaEventos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLimitesHistoricosCriteriosHttpIT#fimNaturalPreservaDataMotivoEFaixasDoDesligamentoIndividualAnterior` | APROVADO_XML_CANDIDATA | 1 |

## VIN-010 — PRESERVADO

Fim natural do contrato usa motivo próprio de catálogo, como 'Fim da vigência do contrato', administrado pelo Gestor Local.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-010:001 | Fim natural usa categoria FIM_VIGENCIA_CONTRATO do catálogo da Gestão competente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-010:002 | Gestor Local administra o motivo, e o ato preserva descrição/categoria aplicadas. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PessoasLimitesHistoricosCriteriosHttpIT#fimNaturalPreservaDataMotivoEFaixasDoDesligamentoIndividualAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#fimAntecipadoPorRescisaoNaoPodeSerReclassificadoComoFimNatural` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#causaContratualComAtoEDataCorretosPermiteRetificacaoExplicitamenteConfirmada` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalFimNaturalProrrogacaoHttpIT#fimNaturalDepoisDaProrrogacaoUsaNovoFimESuaRepeticaoNaoDuplicaEventos` | APROVADO_XML_CANDIDATA | 1 |

## VIN-011 — PRESERVADO

Data de registro administrativo pode ser posterior, mas data efetiva do encerramento não ultrapassa o fim da vigência.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-011:001 | Data efetiva de encerramento natural não ultrapassa fim da vigência. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-011:002 | Registro administrativo posterior conserva Instant próprio e responsável. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLimitesHistoricosCriteriosHttpIT#fimNaturalPreservaDataMotivoEFaixasDoDesligamentoIndividualAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#fimAntecipadoPorRescisaoNaoPodeSerReclassificadoComoFimNatural` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#causaContratualComAtoEDataCorretosPermiteRetificacaoExplicitamenteConfirmada` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalFimNaturalProrrogacaoHttpIT#fimNaturalDepoisDaProrrogacaoUsaNovoFimESuaRepeticaoNaoDuplicaEventos` | APROVADO_XML_CANDIDATA | 1 |

## VIN-012 — PRESERVADO

Prorrogação retroativa posterior não reabre automaticamente vínculos já encerrados no fim antigo; exige impacto e eventual retificação explícita.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-012:001 | Prorrogação posterior não reabre vínculo já encerrado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-012:002 | Reabertura/retificação requer comando explícito com impacto e revalidações. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#prorrogacaoTardiaPreservaFimNaturalComprovadoEAdmiteRetificacaoExplicitaDaNovaFaixa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#prorrogacaoNaoConverteDesligamentoIndividualAnteriorEmFimNatural` | APROVADO_XML_CANDIDATA | 1 |

## VIN-013 — PRESERVADO

Inativação administrativa não encerra vínculos; reativação dentro da mesma vigência permite retomada dos vínculos ainda válidos sem novo vínculo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-013:001 | Inativação administrativa não altera término do Vínculo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-013:002 | Reativação dentro da mesma vigência retoma somente vínculos temporalmente válidos, sem criar outro vínculo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasProjecaoHabilitacaoHttpIT#opcionalNaPessoaAposInativacaoEReativacaoMantemMesmaLinhaCanonica` | APROVADO_XML_CANDIDATA | 1 |

## VIN-014 — PRESERVADO

Data inicial do vínculo pode ser retificada por versão auditada, justificativa e prévia de impacto, revalidando sobreposição, alocação, frequência, matrícula, relatórios e eventos posteriores.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-014:001 | Retificação de início exige justificativa/evidência/autorização e prévia confirmada válida. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-014:002 | Revalida sobreposição global, matrícula e alocação inclusive eventos posteriores. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-014:003 | Frequências e relatórios atingidos são reavaliados, com pendências e invalidação quando cabíveis. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-014:004 | Evento original e versões posteriores são preservados e reconstruíveis por conhecimento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#retificacaoDeInicioConfirmaPreviaExataEAjustaCoberturaSemRecontratar` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#novaFrequenciaAtingidaInvalidaPreviaDeRetificacaoDoInicio` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasAtomicidadeHttpIT#frequenciaConfirmadaEnquantoRetificacaoAguardaLockInvalidaPrevia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#corpoDoComandoNaoPodeMudarEntrePreviaEConfirmacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasCriteriosTemporaisComplementaresHttpIT#retificarInicioRevalidaMatriculaNaPoliticaDaNovaData` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasPoliticaMatriculaFuturaHttpIT#cadastroSemMatriculaRecusaObrigacaoJaProgramadaDentroDoPeriodo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasPoliticaMatriculaFuturaHttpIT#cadastroRespeitaFimAnteriorOuMatriculaInformadaSemBloqueioGenerico` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PessoasLimitesHistoricosCriteriosHttpIT#adiarInicioAlemDeFaixaFuturaExigeTratarProgramacaoAntesDaRetificacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#antecipacaoDoInicioAtravessaTransferenciaReconstroiOrigemEPreservaConhecimentoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#antecipacaoExigeConsentimentoQuandoSomenteAOrigemTemExcedente` | APROVADO_XML_CANDIDATA | 1 |

## VIN-015 — PRESERVADO

Encerramento antecipado/rescisão encerra vínculos abertos na mesma data efetiva, salvo desligamento anterior, usando causa própria.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| VIN-015:001 | Encerramento antecipado encerra vínculos abertos na mesma data efetiva. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| VIN-015:002 | Término individual anterior é preservado; causa específica é fotografada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLimitesHistoricosCriteriosHttpIT#rescisaoPreservaDataMotivoEFaixasDoDesligamentoIndividualAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#desligamentoIndividualAnteriorARescisaoConservaSuaCausaEAdmiteRetificacaoIndividual` | APROVADO_XML_CANDIDATA | 1 |

## ALO-001 — PRESERVADO

Alocação é faixa temporal Lotação + Turno + Cargo dentro do vínculo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ALO-001:001 | Cada Alocação relaciona Vínculo, Lotação, Turno e Cargo em faixa temporal. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ALO-001:002 | Somente combinação presente na composição válida no recorte é selecionável. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#alocacoesHistoricasNaoSaoRenomeadasPelosCatalogosAtuais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasComposicaoFuturaHttpIT#previaDeMudancaDeAlocacaoApontaZeramentoDuranteFaixaProposta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento` | APROVADO_XML_CANDIDATA | 1 |

## ALO-002 — PRESERVADO

Mudança de Lotação/Turno/Cargo cria novo período de alocação; não é desligamento/recontratação.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ALO-002:001 | Mudança de Lotação/Turno/Cargo cria faixa sucessora e encerra anterior em D-1. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ALO-002:002 | A Pessoa e o Vínculo permanecem os mesmos; não é novo desligamento/recontratação. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#pesquisaDesligadosNaoRetornaVinculoAindaAberto` | APROVADO_XML_CANDIDATA | 1 |

## ALO-003 — PRESERVADO

Alteração retroativa de alocação é permitida, preservando fatos posteriores válidos e apontando incompatibilidades.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ALO-003:001 | Mudança retroativa altera apenas o trecho alcançado e preserva eventos posteriores válidos. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ALO-003:002 | Incompatibilidades posteriores são apresentadas e impedem confirmação sem tratamento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PessoasComposicaoFuturaHttpIT#previaDeMudancaDeAlocacaoApontaZeramentoDuranteFaixaProposta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaComAlocacaoPosteriorIncompativelFazRollbackIntegral` | APROVADO_XML_CANDIDATA | 1 |

## ALO-004 — PRESERVADO

Existe no máximo uma alocação efetiva por colaborador por dia; segunda informação do mesmo dia retifica a anterior.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ALO-004:001 | Há no máximo uma faixa REGISTRADA por Pessoa/dia. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ALO-004:002 | Nova informação no mesmo início substitui/retifica a faixa anterior preservada, sem sobreposição. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#mesmaDataRetificaFaixaEmVezDeCriarDuasAlocacoesEfetivas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLimitesHistoricosCriteriosHttpIT#duasRetificacoesDeAlocacaoNoMesmoDiaPreservamTresVersoesEUmaFaixaEfetiva` | APROVADO_XML_CANDIDATA | 1 |

## ALO-005 — PRESERVADO

Mudança de alocação e desligamento na mesma data só podem coexistir se a precedência tornar o último dia inequívoco; caso contrário, corrigir/retificar.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ALO-005:001 | Mudança e desligamento no mesmo dia exigem precedência explícita do último dia. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ALO-005:002 | A regra vale nas duas ordens de lançamento e também nas retificações. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoNoUltimoDiaExigePrecedenciaExplicita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT#desligamentoRegistradoDepoisDaAlocacaoNoMesmoDiaExigePrecedencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT#retificacaoDeDesligamentoParaInicioDeAlocacaoExigePrecedencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLimitesHistoricosCriteriosHttpIT#sucessoraCriadaDoMapaNoNovoUltimoDiaExigePrecedenciaAntesDePersistir` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRestauracaoPrecedenciaHttpIT#restaurarSucessoraNoNovoUltimoDiaExigePrecedenciaExplicita` | APROVADO_XML_CANDIDATA | 1 |

## ALO-006 — PRESERVADO

Jornada que cruza meia-noite pertence à data de início e usa a alocação válida nessa data.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ALO-006:001 | Jornada que atravessa meia-noite pertence à data de início. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ALO-006:002 | A composição/alocação usada é a válida nessa data, sem deslocar o fato ao dia seguinte. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#jornadaNoturnaMantemDataNegocialDeInicioComoChave` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT#jornada22As06PermaneceNaAlocacaoDoInicioMesmoComMudancaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |

## ALO-007 — PRESERVADO

Todo dia de vínculo operacional efetivo deve possuir exatamente uma alocação Lotação + Turno + Cargo válida.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ALO-007:001 | Toda faixa operacional de Vínculo fica coberta sem lacunas por exatamente uma Alocação válida. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ALO-007:002 | Mudanças de início/fim, prorrogação e programação posterior revalidam cobertura/composição nas fronteiras. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ALO-007:003 | Reprojeção após retificar/cancelar transferência preserva decisões independentes válidas e rejeita incompatíveis atomicamente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasComposicaoFuturaHttpIT#vinculoAbertoNaoPodeAtravessarZeramentoFuturoRegistradoAntesDoCadastro` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasComposicaoFuturaHttpIT#vinculoQueTerminaAntesDoZeramentoFuturoMantemCadastroValido` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoProgramadaEstendeSucessoraSemAnteciparContratoEAceitaMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#cadastroAposConhecerProrrogacaoProgramadaUsaLimitePlanejadoERespeitaFimHumano` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest#retificacaoDoFimEstendeUltimaAlocacaoSemCriarLacuna` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#faixaSemCoberturaNaDataEfetivaEhBloqueada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasComposicaoFuturaHttpIT#previaDeMudancaDeAlocacaoApontaZeramentoDuranteFaixaProposta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasProrrogacaoPlanejadaNovaAuditoriaHttpIT#prorrogacaoPreservaTerminoHumanoERecontratacaoJaProgramadaDaMesmaPessoa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#cadastroPosteriorAoAgendamentoAplicaMapaNasDuasFaixasSemAnteciparProjecao` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#capacidadeDoDestinoExigeConsentimentoEspecificoERollbackAntesDaInclusao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLimitesHistoricosCriteriosHttpIT#adiarInicioAlemDeFaixaFuturaExigeTratarProgramacaoAntesDaRetificacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#antecipacaoDoInicioAtravessaTransferenciaReconstroiOrigemEPreservaConhecimentoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#antecipacaoExigeConsentimentoQuandoSomenteAOrigemTemExcedente` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaFuturaRestauraCoberturaOriginalSemExcluirHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaComAlocacaoPosteriorIncompativelFazRollbackIntegral` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud03VinculosTransferenciaProgramadaHttpIT#mapaSemFotografiaDeJornadaRecusaCadastroSemInventarDestinoENaoDeixaRegistrosParciais` | APROVADO_XML_CANDIDATA | 1 |

## ALO-008 — PRESERVADO

Inativação administrativa não encerra Alocação; na reativação, a mesma Alocação volta a operar se ainda válida, não sucedida e com composição válida.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| ALO-008:001 | Inativação administrativa conserva a faixa de Alocação. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| ALO-008:002 | Retomada exige faixa ainda válida, não sucedida e composição válida; não cria nova faixa só por reativar. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa07.AlocacaoVinculoServicoEtapa07Test#reativacaoBloqueiaAlocacaoCujaComposicaoNaoEhMaisValida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasProjecaoHabilitacaoHttpIT#opcionalNaPessoaAposInativacaoEReativacaoMantemMesmaLinhaCanonica` | APROVADO_XML_CANDIDATA | 1 |

## DES-001 — PRESERVADO

Desligamento pertence ao Vínculo Contratual, não é campo fixo do Contrato.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DES-001:001 | Término pertence ao Vínculo específico, sem alterar término do Contrato como substituto. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo` | APROVADO_XML_CANDIDATA | 1 |

## DES-002 — PRESERVADO

Data de desligamento é o último dia em que o vínculo ainda existe; inatividade começa no dia seguinte.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DES-002:001 | Data de desligamento é inclusiva para o Vínculo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DES-002:002 | Inatividade começa em D+1; último dia não é excluído da cobertura. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa06.PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoNoUltimoDiaExigePrecedenciaExplicita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasComposicaoFuturaHttpIT#vinculoQueTerminaAntesDoZeramentoFuturoMantemCadastroValido` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoPessoasCriteriosFronteirasHttpIT#inicioNoMesmoUltimoDiaConflitaMasDiaSeguintePermiteRecontratacaoEntreGestoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLimitesHistoricosCriteriosHttpIT#sucessoraCriadaDoMapaNoNovoUltimoDiaExigePrecedenciaAntesDePersistir` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasRestauracaoPrecedenciaHttpIT#restaurarSucessoraNoNovoUltimoDiaExigePrecedenciaExplicita` | APROVADO_XML_CANDIDATA | 1 |

## DES-003 — PRESERVADO

Desligamento pode ser informado retroativamente; data real do fato prevalece, registro posterior fica auditado.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DES-003:001 | Desligamento retroativo usa a data real do fato. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DES-003:002 | Registro posterior é auditado e não reescreve fatos existentes silenciosamente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa06.VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.DesligamentoFrequenciaProvaPreservadaHttpIT#desligamentoRetroativoPreservaFrequenciaOcorrenciaEAnexoEExcluiCalculoSomenteAposUltimoDia` | APROVADO_XML_CANDIDATA | 1 |

## DES-004 — PRESERVADO

Desligamento possui motivo de catálogo, responsável e data/hora de registro.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DES-004:001 | Desligamento exige causa válida de catálogo e Gestão responsável na data. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DES-004:002 | Responsável e registro são preservados; renomeação posterior não altera a causa aplicada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#previaDeDesligamentoRejeitaMotivoDeOutraGestaoAntesDaConfirmacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasTransferenciaGestaoEfetivaHttpIT#destinoAtualizaPessoaEUsaCatalogoEFotografiaDaDataRealDoDesligamento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#desligamentoIndividualRejeitaCausaContratualSemAtoCorrespondente` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#previaDeRetificacaoRejeitaCausaContratualSemAtoCorrespondente` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#desligamentoIndividualEOuOutraCausaCadastradaContinuamValidos` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#causaContratualComAtoEDataCorretosPermiteRetificacaoExplicitamenteConfirmada` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#existenciaDoAtoNaoAutorizaUsarSuaCausaEmOutraData` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#fimAntecipadoPorRescisaoNaoPodeSerReclassificadoComoFimNatural` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#anulacaoFuturaNaoJustificaCausaAnulatoriaAntesDoAlcance` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPessoasLacunasEspecificasHttpIT#renomearCausaPreservaResponsavelInstanteEFotografiaDoDesligamento` | APROVADO_XML_CANDIDATA | 1 |

## DES-005 — PRESERVADO

Reprogramação/retificação de desligamento preserva original e verifica toda a linha temporal para evitar sobreposição.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DES-005:001 | Retificação mantém original e novo evento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DES-005:002 | Revalida toda a linha temporal quanto a sobreposição, alocações posteriores e capacidade/excedente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DES-005:003 | Confirmação usa prévia ainda válida; mudança concorrente rejeita o comando. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasDesligamentoExcedenteFuturoHttpIT#retificarDesligamentoQueAmpliaFaixaExigeConsentimentoParaOcupanteFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasValidacoesAdicionaisHttpIT#alteracaoDoMotivoDepoisDaPreviaExigeNovaConfirmacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#retificacoesConcorrentesEmContratosDiferentesNaoCriamSobreposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#desligamentoIndividualAnteriorARescisaoConservaSuaCausaEAdmiteRetificacaoIndividual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud02DesligamentoHttpIT#confirmacaoRevalidaAtoAnulatorioRetiradoDepoisDaPrevia` | APROVADO_XML_CANDIDATA | 1 |

## DES-006 — PRESERVADO

Desligamento futuro/programado pode ser cancelado/retificado, preservando toda a cadeia.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| DES-006:001 | Desligamento futuro pode ser cancelado ou retificado por comando próprio. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| DES-006:002 | Cadeia original é preservada e período restaurado é revalidado antes de confirmar. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#desligamentoFuturoPodeSerCanceladoSemApagarOrigem` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.VinculoAlocacaoRegressaoTest#cancelarDesligamentoNaoPodeSobreporOutroVinculoDaPessoa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasDesligamentoExcedenteFuturoHttpIT#cancelarDesligamentoQueAmpliaFaixaExigeConsentimentoParaOcupanteFuturo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.VinculoAlocacaoConcorrenciaIT#cancelamentoDoDesligamentoContraNovoCadastroMantemExclusividadeDaPessoa` | APROVADO_XML_CANDIDATA | 1 |

## FRE-001 — PRESERVADO

Frequência resolve vínculo e alocação válidos na data negocial.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-001:001 | Lançamento resolve vínculo/alocação da data e expõe seus IDs e estados independentes. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-001:002 | Transferência/ADI e jornada noturna respeitam Gestão e alocação efetivas do início da jornada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-001:003 | Complementos novos exigem cobertura e autorização em todos os dias do período. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-001:004 | Fechamento de contrato transferido resolve o contrato pelo dia solicitado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-001:005 | CPF isolado de raiz legada não comprova contexto de criação. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#consultasDiariaMensalEIntervaloRetornamIdentidadeCanonicaEEstadosIndependentes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#resolvedorBloqueiaContratoHistoricamenteInativo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayVigenciaFrequenciaRelatorioHttpIT#vinculoFrequenciaERelatorioUsamProrrogacaoRecebidaDepoisDaDescricaoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaGestaoTransferidaAditamentoHttpIT#frequenciaDepoisDoAditamentoUsaGestaoDoFatoSemRestaurarFotografiaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT#jornada22As06PermaneceNaAlocacaoDoInicioMesmoComMudancaNoDiaSeguinte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#cadastroExigeVinculoEAlocacaoValidosNaDataDoComplemento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#excecaoDeVariosDiasExigeAutorizacaoEmTodosOsDiasAtingidos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#gestaoHistoricaPodeCadastrarComplementoMesmoDepoisDaTransferencia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#gestaoAtualNaoPodeCadastrarComplementoEmDiaDaGestaoAnterior` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#fechamentoDaGestaoHistoricaAceitaContratoTransferidoEWorkerPreservaContexto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#comandoDaGestaoAtualNaoAceitaContratoEmDataHistoricamenteAlheia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado` | APROVADO_XML_CANDIDATA | 2 |

## FRE-002 — PRESERVADO

Usuário Operacional pode editar por 7 dias corridos, contando a data da frequência como dia 1, em America/Sao_Paulo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-002:001 | A data da jornada é dia1; a janela termina às23:59:59 de D+6 em America/Sao_Paulo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-002:002 | A fronteira usa o fuso São Paulo e a autorização HTTP recusa Operacional fora da janela. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#politicaConverteInstanteParaSaoPauloSemOffsetFixo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve` | APROVADO_XML_CANDIDATA | 1 |

## FRE-003 — PRESERVADO

Exemplo: frequência de 01/01 editável até 07/01 23:59:59 e bloqueada para o Operacional a partir de 08/01 00:00.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-003:001 | A data da jornada é dia1; a janela termina às23:59:59 de D+6 em America/Sao_Paulo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-003:002 | A fronteira usa o fuso São Paulo e a autorização HTTP recusa Operacional fora da janela. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#politicaConverteInstanteParaSaoPauloSemOffsetFixo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve` | APROVADO_XML_CANDIDATA | 1 |

## FRE-004 — PRESERVADO

Alterações do Operacional dentro da janela permanecem auditadas, com antes/depois, usuário e data/hora.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-004:001 | Edição operacional dentro da janela avança versão e mantém antes/depois, autor e registro. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-004:002 | Cadeia de auditoria da frequência verifica hashes e detecta conteúdo incompatível. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT#alteracaoOperacionalNaJanelaPreservaAntesDepoisEAutorOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#auditoriaGeraHashEValidaCadeia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaEvidenciasIntegridadeHttpIT#eventoComRepresentacaoLegadaPermaneceConsultavelSemSerDeclaradoIntegroOuReassinado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaEvidenciasIntegridadeHttpIT#evidenciaIntegraExibeComparacaoEReutilizaEscopoHistoricoDaFrequencia` | APROVADO_XML_CANDIDATA | 1 |

## FRE-005 — PRESERVADO

Depois da janela, Gestor Local corrige por retificação versionada; não sobrescreve silenciosamente.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-005:001 | Após a janela, Operacional é recusado e Gestor corrige por ato versionado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-005:002 | Retificação de memória manual preserva a memória anterior no evento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaHistoricoIndicadoresAuditoriaHttpIT#retificacaoManualDeNoiteParaDiaPreservaAMemoriaAnteriorNaCadeia` | APROVADO_XML_CANDIDATA | 1 |

## FRE-006 — PRESERVADO

Gestor Local pode corrigir antes do fim dos 7 dias, com identificação e justificativa.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-006:001 | Gestor pode retificar antes do7º dia, com versão formal e bloqueio apenas daquele registro. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-006:002 | Retificação gerencial sem justificativa é rejeitada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#parametrosInvalidosNaoSaoAceitosComoConsultasOuLancamentosValidos` | APROVADO_XML_CANDIDATA | 1 |

## FRE-007 — PRESERVADO

Gestor Local não possui prazo máximo rígido para retificação tardia; toda retificação tardia exige justificativa/auditoria.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-007:001 | Gestor pode retificar jornada antiga sem prazo máximo rígido e com justificativa. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | APROVADO_XML_CANDIDATA | 1 |

## FRE-008 — PRESERVADO

Retificação recalcula visão oficial atual de relatórios mensais, absenteísmo e indicadores; versões anteriores permanecem auditáveis.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-008:001 | Retificação altera o resultado oficial atual sem reescrever emissão anterior. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-008:002 | Memória manual de horas é aplicada de forma consistente em consulta, consolidação, emissão e arquivo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-008:003 | Reconciliação automática preserva fontes manuais e recalcula apenas RT derivado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-008:004 | Mudança de calendário invalida resultados dependentes, inclusive dias SEM_LANCAMENTO. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoRelatoriosOperacionaisHttpIT#alteracaoOperacionalDentroDaJanelaSuperaEmissaoEPreservaSnapshot` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoRelatoriosOperacionaisHttpIT#reenvioOperacionalIdenticoNaoSuperaEmissaoNemDuplicaFato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelNovoDuranteRetificacaoHttpIT#primeiroCalculoCriadoAntesDoCommitDaFonteNaoPublicaResultadoAntigoComoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaMemoriaManualAuditoriaHttpIT#memoriaManualPersistidaPrevaleceSobreJornadaEmConsultaEmissaoEArquivo` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#recalculoDaFrequenciaNaoRemoveNemSobrescreveAdicionalManualPublicado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#adicionalDerivadoContinuaReconciliavelComProvaNaFrequenciaDeOrigem` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificarDataELotacaoInvalidaOsDoisRecortesSemAtingirTerceiroDia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual` | APROVADO_XML_CANDIDATA | 1 |

## FRE-009 — PRESERVADO

Alteração retroativa deve identificar frequências abertas e fechadas atingidas; fechada não é alterada silenciosamente, exige retificação autorizada.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-009:001 | Impacto retroativo em vínculo/alocação identifica frequência atingida e mantém seu conteúdo original. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-009:002 | Retirar/mover fonte que sustenta FOLGA fechada exige revisão sem alterar silenciosamente a jornada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-009:003 | Concorrência entre inativação e gravação não deixa frequência válida sob contexto obsoleto. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#contextoMudadoNaoReassociaFrequenciaAutomaticamente` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFonteRetiradaRevisaoHttpIT#retirarFonteDeFolgaFechadaPreservaFrequenciaESinalizaSomenteRecorteAfetado` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.FrequenciaConcorrenciaContratoHttpIT#inativacaoNaoPodeConfirmarEDeixarFrequenciaConcorrenteValidaPorContextoAntigo` | APROVADO_XML_CANDIDATA | 2 |

## FRE-010 — PRESERVADO

Frequências lançadas depois da data real de um desligamento retroativo permanecem visíveis/auditáveis e sinalizadas, mas não devem ser misturadas ao efetivo operacional válido/indicadores.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-010:001 | Frequência após desligamento retroativo é preservada e retirada da população operacional válida. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-010:002 | Consolidado sem faixa operacional mantém aviso, sem misturar a frequência inválida aos indicadores. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaHistoricoIndicadoresAuditoriaHttpIT#consolidadoSemFaixaOperacionalMantemAvisoDaFrequenciaInvalidada` | APROVADO_XML_CANDIDATA | 1 |

## FRE-011 — PRESERVADO

Enquanto retificações pendem, relatórios podem ser consultados/exportados com aviso 'sujeito a revisão' e quantidade de pendências.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-011:001 | Relatório com retificação pendente expõe aviso e contagem no recorte correto. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-011:002 | Exportação XLSX mostra o aviso no próprio documento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-011:003 | Presidência e comparação institucional recebem aviso real limitado ao recorte publicado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-011:004 | Retirada de fonte de FOLGA gera aviso e pendência apenas para o dia afetado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-011:005 | Pendência e aviso acompanham o responsável efetivo após transferência retroativa, inclusive quando a revisão já estava aberta. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaHistoricoIndicadoresAuditoriaHttpIT#consolidadoSemFaixaOperacionalMantemAvisoDaFrequenciaInvalidada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa11.ExportacaoRelatorioAvisoEtapa11Test#xlsxExibeAvisoEQuantidadeDePendenciasNoProprioArquivo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#presidenciaRecebeAvisoDeRevisaoComContagemRealDoRecorte` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFonteRetiradaRevisaoHttpIT#retirarFonteDeFolgaFechadaPreservaFrequenciaESinalizaSomenteRecorteAfetado` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRetroativaEntregaPendenciaEAvisoAoResponsavelSemReescreverFonte` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRealinhaTodasAsRevisoesAbertasDeVersoesAnterioresSemAlterarSuaHistoria` | APROVADO_XML_CANDIDATA | 1 |

## FRE-012 — PRESERVADO

Quando retificação é concluída, a visão oficial atual usa a verdade corrigida e a emissão anterior permanece versionada.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-012:001 | Após corrigir, verdade atual e nova emissão usam o resultado retificado preservando a original. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-012:002 | Pendência só é resolvida depois da correção da origem. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao` | APROVADO_XML_CANDIDATA | 1 |

## FRE-013 — PRESERVADO

No 8º dia, a frequência passa automaticamente a FECHADA negocialmente; qualquer mudança posterior é retificação.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-013:001 | No início do8º dia, a política de edição já considera frequência fechada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-013:002 | Cache não conserva ABERTA depois da fronteira e rotina técnica grava fechamento auditado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CacheEscopoTemporalHttpIT#cacheInternoNaoConservaEstadoAbertoDepoisDoInicioDoOitavoDia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#fechamentoAutomaticoPersisteEstadoEEvento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoFechamentoNegocialMysqlHttpIT#oitavoDiaPersisteFechamentoComEventoEncadeadoSemAlterarFonteNemDuplicarNoReprocessamento` | APROVADO_XML_CANDIDATA | 1 |

## FRE-014 — PRESERVADO

Alteração do Gestor Local gera versão formal e bloqueia nova edição daquele registro pelo Operacional.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-014:001 | Retificação gerencial bloqueia nova edição operacional daquele registro. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-014:002 | Nova retificação gerencial acrescenta versão mesmo se uma anterior já existe. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | APROVADO_XML_CANDIDATA | 1 |

## FRE-015 — PRESERVADO

A frequência diária pertence ao escopo da alocação histórica do colaborador na data da frequência, e não à alocação atual do colaborador.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-015:001 | Filtros de intervalo limitam cada dia à Gestão efetiva da alocação. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-015:002 | Alteração da projeção atual de contrato não apaga horas do período histórico. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-015:003 | Complementos e filtros usam a responsabilidade histórica; origem continua podendo manter o fato. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-015:004 | Perda de cobertura não restaura uma Gestão superada nem apaga acesso histórico comprovado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-015:005 | Fechamento por contrato explícito usa Gestão do dia, não a atual. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#intervaloMistoNaoEntregaJulhoAGestaoDeOrigem` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#gestaoDeDestinoRecebeSomenteDiasPosterioresATransferencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#consultaUsaResponsabilidadeRetroativaSemReescreverFotografia` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#paginaIntermediariaETotalContemSomenteLinhasAutorizadas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#filtrosOrganizacionaisSaoAplicadosAResponsabilidadeCorrigida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#consultaPorIdNaoConcedeAcessoAoEscopoAnteriorDaFotografia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaRelatoriosLacunasHttpIT#horasTrabalhadasContinuamVisiveisQuandoProjecaoAtualMudouDeContrato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#consultaEFiltrosPreservamGestaoHistoricaDepoisDeTransferencia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#gestaoHistoricaPodeManterComplementoDepoisDaTransferencia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#gestaoAtualNaoCapturaNemExcluiComplementoDeOutraGestao` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#perdaDeCoberturaOperacionalNaoApagaContextoComprovadoParaConsulta` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoAposPerdaCoberturaHttpIT#anulacaoNaoRestauraGestaoDaFotografiaSuperadaPorTransferenciaRetroativa` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#fechamentoDaGestaoHistoricaAceitaContratoTransferidoEWorkerPreservaContexto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#comandoDaGestaoAtualNaoAceitaContratoEmDataHistoricamenteAlheia` | APROVADO_XML_CANDIDATA | 1 |

## FRE-016 — PRESERVADO

Se retificação retroativa muda a alocação histórica, outro Operacional não assume silenciosamente o registro; eventual correção passa ao Gestor Local e preserva o autor original.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-016:001 | Mudança retroativa de alocação não reassocia a frequência nem transfere edição ao novo Operacional. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-016:002 | Correção autorizada preserva autor original e antes/depois auditáveis. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#contextoMudadoNaoReassociaFrequenciaAutomaticamente` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaTemporalBordasHttpIT#alteracaoOperacionalNaJanelaPreservaAntesDepoisEAutorOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao` | APROVADO_XML_CANDIDATA | 1 |

## FRE-017 — PRESERVADO

Qualquer fato retroativo que retire cobertura válida preserva a frequência para auditoria, mas a retira do cálculo operacional oficial.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-017:001 | Retirada de cobertura conserva a frequência como prova e marca validade operacional inválida. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-017:002 | Recuperar cobertura não regulariza silenciosamente a frequência pendente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-017:003 | Transações concorrentes não confirmam cobertura inválida como válida. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-017:004 | Anulação contratual desde a jornada preserva frequência fechada e emissão original, retirando a frequência do cálculo vigente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-017:005 | Inativação documentada retira do cálculo somente os dias abrangidos pelo alcance, mantendo as fontes históricas. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#impactoRetroativoSemCoberturaPreservaRegistroEMarcaInvalido` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.frequencia.FrequenciaEtapa09Test#reativacaoNaoRestauraValidadeSilenciosamente` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaConcorrenciaContratoHttpIT#inativacaoNaoPodeConfirmarEDeixarFrequenciaConcorrenteValidaPorContextoAntigo` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaAnulacaoContratoPreservacaoHttpIT#anulacaoPreservaFrequenciaFechadaEProvaOriginalMasRetiraCalculoEGeraPendencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#inativacaoDocumentadaPreservaPeriodoAnteriorEExcluiSomenteAlcance` | APROVADO_XML_CANDIDATA | 1 |

## FRE-018 — PRESERVADO

Permissão de edição combina autorização atual do usuário com o escopo histórico do registro.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-018:001 | Edição exige perfil atual autorizado e escopo histórico próprio. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-018:002 | Revogação atual de permissão excepcional vale em consulta e eventos. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-018:003 | Complementos autorizam origem antes de destino e todos os dias de uma exceção. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-018:004 | Sem identidade comprovada, o CPF atual de outra Pessoa não concede consulta nem escrita; prova própria existente mantém acesso legítimo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-018:005 | Comando de fechamento usa autorização presente sobre Gestão histórica. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaPermissaoHistoricaHttpIT#concessaoDaGestaoAtualNaoAmpliaLeituraPessoalDaGestaoHistorica` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#consultaUsaResponsabilidadeRetroativaSemReescreverFotografia` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#paginaIntermediariaETotalContemSomenteLinhasAutorizadas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#filtrosOrganizacionaisSaoAplicadosAResponsabilidadeCorrigida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#consultaPorIdNaoConcedeAcessoAoEscopoAnteriorDaFotografia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaConsultaMinimizacaoHttpIT#permissaoExcepcionalEscopadaRegistraUsoERevogacaoValeEmConsultaEEventos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#gestaoAtualNaoCapturaNemExcluiComplementoDeOutraGestao` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#excecaoDeVariosDiasExigeAutorizacaoEmTodosOsDiasAtingidos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#gestaoHistoricaPodeManterComplementoDepoisDaTransferencia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#fonteLegadaSemProvaNaoMudaDeGestaoPorCoincidenciaAtualDeCpf` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosIdentidadeLegadaHttpIT#fotografiaComprovadaMantemIdentidadeOriginalMesmoSemAssociacaoAtual` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#fechamentoDaGestaoHistoricaAceitaContratoTransferidoEWorkerPreservaContexto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FechamentoContratoTransferidoHttpIT#comandoDaGestaoAtualNaoAceitaContratoEmDataHistoricamenteAlheia` | APROVADO_XML_CANDIDATA | 1 |

## FRE-019 — PRESERVADO

Frequência separa visibilidade histórica, validade para cálculo operacional e estado de auditoria.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| FRE-019:001 | Visibilidade, estado negocial, validade e auditoria são campos distintos. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-019:002 | Registro inválido permanece auditável sem integrar totais operacionais. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-019:003 | Fonte com cobertura retirada continua acessível pelo escopo comprovado, realinhado às transferências válidas. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| FRE-019:004 | Preservar leitura de fatos passados não libera o bloqueio operacional corrente nem fabrica sua data inicial. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.FrequenciaContratosHttpIT#consultasDiariaMensalEIntervaloRetornamIdentidadeCanonicaEEstadosIndependentes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaHistoricoIndicadoresAuditoriaHttpIT#consolidadoSemFaixaOperacionalMantemAvisoDaFrequenciaInvalidada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoTemporalHttpIT#perdaDeCoberturaOperacionalNaoApagaContextoComprovadoParaConsulta` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosEscopoAposPerdaCoberturaHttpIT#anulacaoNaoRestauraGestaoDaFotografiaSuperadaPorTransferenciaRetroativa` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#flagAtualSemDataNaoTransportaPreservacaoHistoricaParaDiaCorrenteOuFuturo` | APROVADO_XML_CANDIDATA | 1 |

## REL-001 — PRESERVADO

Relatório oficial atual reflete a última verdade válida/retificada; auditoria preserva versões anteriores.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REL-001:001 | Relatório oficial atual reflete a última retificação e conserva a emissão anterior. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-001:002 | Comparação institucional de emissão local conserva o recorte publicado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-001:003 | Mudança de calendário ou fonte de FOLGA sinaliza a verdade atual dependente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-001:004 | Consolidado e absenteísmo usam nome historicamente comprovado, com ausência explícita quando não há fotografia. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-001:005 | Rótulos de Lotação, Turno e Cargo do relatório derivam de fotografia comprovada da Alocação ou Frequência. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-001:006 | Flag corrente sem data de inativação não exclui apuração histórica comprovada, com ou sem versões contratuais. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-001:007 | Apuração continua respeitando inatividade comprovada por evento temporal ou fotografia administrativa inicial. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-001:008 | Intervalo misto é dividido na fronteira de consulta corrente sem registrar um ato de inativação presumido. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoRelatoriosOperacionaisHttpIT#alteracaoOperacionalDentroDaJanelaSuperaEmissaoEPreservaSnapshot` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoRelatoriosOperacionaisHttpIT#reenvioOperacionalIdenticoNaoSuperaEmissaoNemDuplicaFato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelRetificacaoHttpIT#retificacaoDeFrequenciaNaoDeixaPayloadAntigoDoPainelMarcadoComoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelRetificacaoConcorrenteHttpIT#resultadoCalculadoAntesDaRetificacaoNaoPodeSerPublicadoComoAtualDepoisDoCommit` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ReplayVigenciaFrequenciaRelatorioHttpIT#vinculoFrequenciaERelatorioUsamProrrogacaoRecebidaDepoisDaDescricaoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelNovoDuranteRetificacaoHttpIT#primeiroCalculoCriadoAntesDoCommitDaFonteNaoPublicaResultadoAntigoComoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificarDataELotacaoInvalidaOsDoisRecortesSemAtingirTerceiroDia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFonteRetiradaRevisaoHttpIT#retirarFonteDeFolgaFechadaPreservaFrequenciaESinalizaSomenteRecorteAfetado` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.RelatorioNomeLegadoDesconhecidoHttpIT#relatorioEPlanilhaNaoSubstituemNomeHistoricoAusentePeloCadastroAtual` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RelatorioNomeLegadoDesconhecidoHttpIT#relatorioEPlanilhaPreservamNomeComprovadoNaFrequenciaAposCorrecaoAtual` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#transferenciaRetroativaNaoTrocaNomeComprovadoDaOrigemPeloMestreRenomeado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#atoDeMatriculaRetroativoNaoEmprestaNomeAtualAoPeriodoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#ausenciaTotalDeFotoNominalPermaneceDesconhecidaDepoisDaCorrecaoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaNominalHttpIT#ausenciaNominalPreservaDesconhecimentoRetroativoECapturaContemporanea` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RelatorioRotulosLegadosHttpIT#relatorioPreservaRotulosComprovadosEExplicitaAusenciaLegada` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#contratoInativoHojeContinuaNoRelatorioHistoricoSemFiltroDeContrato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#flagAtualSemDataPreservaFaixaHistoricaMesmoSemVersoes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#inativacaoDocumentadaPreservaPeriodoAnteriorEExcluiSomenteAlcance` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#fotografiaAdministrativaInativaPermaneceBloqueadaMesmoComFlagAtualAtiva` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.RelatorioFrequenciaEscopoHistoricoIT#flagAtualSemDataNaoTransportaPreservacaoHistoricaParaDiaCorrenteOuFuturo` | APROVADO_XML_CANDIDATA | 1 |

## REL-002 — PRESERVADO

Nova emissão após retificação retroativa deve ser identificada como versão retificada, com data/hora de geração.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REL-002:001 | Nova emissão após invalidação tem versão maior e natureza RETIFICADA. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-002:002 | Fontes temporais sem lançamento e calendário também provocam emissão retificada quando alteram resultado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa11.RelatorioAdministrativoServicoEtapa11Test#novaEmissaoAposInvalidacaoDaFonteEhRetificadaEPreservaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.EmissaoSemFrequenciaInvalidacaoHttpIT#fonteTemporalSemFrequenciaSuperaEmissaoAfetadaEPreservaPeriodoAnterior` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual` | APROVADO_XML_CANDIDATA | 1 |

## REL-003 — PRESERVADO

Auditoria deve permitir consultar a verdade efetiva atual e o que o sistema conhecia até determinada data de registro.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REL-003:001 | Consulta de emissão e posições distingue conhecimento anterior da verdade atual. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-003:002 | Fontes de calendário/complementos mantêm cadeia consultável no instante de conhecimento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.MotorTemporalContratoEtapa04Test#consultaConhecidoAteDistingueConhecimentoDoFato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.MotoresTemporaisConhecimentoRegressaoTest#reducaoPosteriorDoContratoNaoEncerraVinculoNoConhecimentoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaTotalOuCamposVaziosNaoAutorizaProjecaoAtualComoPassado` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaDaPublicacaoNaoPromoveFotografiaDeAditamentoABaseOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#fotografiaRegistradaDepoisDoCorteNaoPodeAparecerComoConhecida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#publicacaoNovaMantemValoresERotulosMesmoComMestresAlterados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#deltaExigeConhecimentoDoInstrumentoEDoEvento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ReplayVigenciaFrequenciaRelatorioHttpIT#vinculoFrequenciaERelatorioUsamProrrogacaoRecebidaDepoisDaDescricaoPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoRelatoriosConhecimentoHttpIT#corteAnteriorNaoExpoeSuperacaoConhecidaSomenteDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoRelatoriosConhecimentoHttpIT#cortePosteriorMostraSuperacaoJaConhecidaSemReescreverPersistencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato` | APROVADO_XML_CANDIDATA | 2 |

## REL-004 — PRESERVADO

Relatório com pendências de retificação pode ser visualizado/exportado, mas não deve parecer definitivo; marcar sujeito a revisão.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REL-004:001 | Relatório pendente indica revisão e quantidade, inclusive quando não há linhas operacionais. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-004:002 | Aviso acompanha o arquivo exportado e o recorte institucional autorizado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-004:003 | O aviso do relatório segue o escopo responsável pela revisão, sem alterar a fotografia original da fonte. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaHistoricoIndicadoresAuditoriaHttpIT#consolidadoSemFaixaOperacionalMantemAvisoDaFrequenciaInvalidada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa11.ExportacaoRelatorioAvisoEtapa11Test#xlsxExibeAvisoEQuantidadeDePendenciasNoProprioArquivo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#presidenciaRecebeAvisoDeRevisaoComContagemRealDoRecorte` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRetroativaEntregaPendenciaEAvisoAoResponsavelSemReescreverFonte` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRealinhaTodasAsRevisoesAbertasDeVersoesAnterioresSemAlterarSuaHistoria` | APROVADO_XML_CANDIDATA | 1 |

## REL-005 — PRESERVADO

Uma emissão administrativa de relatório deve registrar período, data/hora de geração, usuário responsável, versão, indicação de pendências existentes e identificação de emissão original ou retificada.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REL-005:001 | Emissão registra período, geração, usuário/perfil, versão, natureza, pendências e hash. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-005:002 | Reenvio de mesma intenção preserva emissão e intenção diferente na mesma chave é conflito. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-005:003 | Concorrência idempotente só retorna após transação válida de recuperação. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.etapa11.RelatorioAdministrativoServicoEtapa11Test#emissaoOriginalPreservaSnapshotHashAutorPeriodoEPendencias` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissoesRejeitamPerfilEscritorIndevidoIntencaoDivergenteEFiltrosInvalidos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoEmissaoIdempotenciaConcorrenciaHttpIT#mesmaChaveConcorrenteConfirmaUmaEmissaoEDevolveMesmaProvaAosDoisClientes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa11.RelatorioAdministrativoConcorrenciaTest#repeticaoConcorrenteSoRetornaDepoisDeNovaTransacaoValida` | APROVADO_XML_CANDIDATA | 1 |

## REL-006 — PRESERVADO

Retificação que altera cálculo invalida a consolidação anterior para a visão vigente, recalcula resultados dependentes e preserva a versão histórica.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| REL-006:001 | Alteração temporal invalida exatamente intervalos/escopos atingidos e preserva conteúdo anterior. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-006:002 | Cálculo afetado por calendário e retirada de fonte de FOLGA supera emissão vigente sem apagar prova. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| REL-006:003 | Comparação da consolidação vigente mantém o escopo da emissão original. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.servico.InvalidacaoEmissaoTemporalTest#retificacaoAtingeAlocacaoAnteriorENovaEPreservaOutroRecorteEConteudo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.EmissaoSemFrequenciaInvalidacaoHttpIT#fonteTemporalSemFrequenciaSuperaEmissaoAfetadaEPreservaPeriodoAnterior` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoRelatoriosOperacionaisHttpIT#alteracaoOperacionalDentroDaJanelaSuperaEmissaoEPreservaSnapshot` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoRelatoriosOperacionaisHttpIT#reenvioOperacionalIdenticoNaoSuperaEmissaoNemDuplicaFato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelRetificacaoHttpIT#retificacaoDeFrequenciaNaoDeixaPayloadAntigoDoPainelMarcadoComoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelRetificacaoConcorrenteHttpIT#resultadoCalculadoAntesDaRetificacaoNaoPodeSerPublicadoComoAtualDepoisDoCommit` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelNovoDuranteRetificacaoHttpIT#primeiroCalculoCriadoAntesDoCommitDaFonteNaoPublicaResultadoAntigoComoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificarDataELotacaoInvalidaOsDoisRecortesSemAtingirTerceiroDia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFonteRetiradaRevisaoHttpIT#retirarFonteDeFolgaFechadaPreservaFrequenciaESinalizaSomenteRecorteAfetado` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#leitorNacionalNaoAmpliaEmissaoLocalSemFiltroDeContratos` | APROVADO_XML_CANDIDATA | 4 |

## AUD-001 — PRESERVADO

Auditoria registra autor, data/hora de ação, data efetiva, motivo/justificativa e antes/depois quando aplicável.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| AUD-001:001 | Registrar autor, data de registro, data efetiva, motivo e antes/depois | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-001:002 | Escopo auditado corresponde ao fato | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-001:003 | Cadeia protege os metadados de autorização | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-001:004 | Ocorrências registram autor/instante/contexto efetivo; histórico novo v2 autentica Gestão/estado/data; histórico de ocorrência v3 também autentica o vínculo explícito à fonte. Calendário e fontes manuais preservam versões com autoria, registro, conteúdo e cadeia de hashes. Assinaturas legadas não são reescritas. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-001:005 | Atualização dos dados opcionais da Pessoa pelo fluxo canônico registra valores anteriores/novos, autor, instante, data efetiva e motivo de operação; rollback não confirma os dados nem publica ação bem-sucedida. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga` | APROVADO_XML_CANDIDATA | 6 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#cancelamentoComPosteriorCompativelPreservaDeltasEHistoricoSemCriarPendencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#cancelarRemanejamentoFuturoDeveSinalizarOInstrumentoPosteriorQueFicaIncompativel` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalResolucaoPendenciaFimSuperadoHttpIT#tarefaLegadaSemCausaAtualPodeSerResolvidaSemEncerrarVinculoVigente` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalResolucaoPendenciaFimSuperadoHttpIT#resolverTarefaAntigaPreservaTarefaDoNovoFimAindaNaoTratado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.HistoricoOcorrenciaContextoAtualHttpIT#historicoVinculadoSegueResponsabilidadeDaOcorrenciaSemReescreverProvaOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#assinaturaDetectaAlteracaoDoEscopoQueControlaAConsultaHistorica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.HistoricoAssinaturaLegadaMistaHttpIT#cadeiaMistaPreservaHashOriginalEAutenticaIdentidadeDaOcorrenciaNova` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaEvidenciasIntegridadeHttpIT#eventoComRepresentacaoLegadaPermaneceConsultavelSemSerDeclaradoIntegroOuReassinado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaEvidenciasIntegridadeHttpIT#evidenciaIntegraExibeComparacaoEReutilizaEscopoHistoricoDaFrequencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoApiCanonicaHttpIT#atualizacaoCanonicaRegistraAntesDepoisAutorDataEfetivaESomenteAposCommit` | APROVADO_XML_CANDIDATA | 1 |

## AUD-002 — PRESERVADO

Retificação preserva fato original, nova versão, motivo, autor, data de registro e data efetiva.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| AUD-002:001 | Retificação preserva original e nova versão | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-002:002 | Motivo/autor/data efetiva/registro são explícitos | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-002:003 | Alteração de fonte de frequência não apaga sua versão original | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-002:004 | Retificações de calendário/exceção/adicional acrescentam fotografias imutáveis; cancelamentos lógicos preservam raiz e versões. Reconciliar frequência não remove fonte manual publicada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.FrequenciaFontesMotivoObrigatorioHttpIT#retificarOuCancelarFontePublicadaExigeMotivoNovoSemReutilizarNarrativaAntiga` | APROVADO_XML_CANDIDATA | 6 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#recalculoDaFrequenciaNaoRemoveNemSobrescreveAdicionalManualPublicado` | APROVADO_XML_CANDIDATA | 1 |

## AUD-003 — PRESERVADO

Histórico deve distinguir quando o fato valeu de quando o sistema tomou conhecimento.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| AUD-003:001 | Data efetiva e registro são distintas | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-003:002 | Consulta conhecidoAte respeita conhecimento disponível | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-003:003 | Auditoria mantém as versões superadas | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-003:004 | Situação de evento contratual em conhecidoAte é calculada pelas relações conhecidas no corte. Cadeias de calendário e complementos distinguem data do fato e instante registrado, com consulta histórica por conhecidoAte. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaTotalOuCamposVaziosNaoAutorizaProjecaoAtualComoPassado` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaDaPublicacaoNaoPromoveFotografiaDeAditamentoABaseOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#fotografiaRegistradaDepoisDoCorteNaoPodeAparecerComoConhecida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#publicacaoNovaMantemValoresERotulosMesmoComMestresAlterados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#descricaoPosteriorNaoApagaDeltaAnteriorConhecidoDepoisNemAlteraFotografias` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#deltaExigeConhecimentoDoInstrumentoEDoEvento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.integration.MotorTemporalVigenciaReplayIT#retiradaDaProrrogacaoPreservaOCorteAnteriorESoRemoveSeuDelta` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoRelatoriosConhecimentoHttpIT#corteAnteriorNaoExpoeSuperacaoConhecidaSomenteDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoRelatoriosConhecimentoHttpIT#cortePosteriorMostraSuperacaoJaConhecidaSemReescreverPersistencia` | APROVADO_XML_CANDIDATA | 1 |

## AUD-004 — PRESERVADO

Prévia de impacto deve ser revalidada na confirmação; se o impacto mudou, recalcular e exigir nova confirmação.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| AUD-004:001 | Prévia assina estado/impactos completos | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-004:002 | Confirmação relê sob lock | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-004:003 | Dependência fora da amostra invalida token | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-004:004 | Nova confirmação é exigida | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PreviaCicloVidaLimitesHttpIT#previaLimitaDetalhesMasContaTudoEAssinaDependenciaForaDaAmostra` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CicloVidaContratosHttpIT#previaListaImpactosERejeitaConfirmacaoQuandoNovaFrequenciaAparece` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#novaFrequenciaAtingidaInvalidaPreviaDeRetificacaoDoInicio` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaConcorrenciaContratoHttpIT#inativacaoNaoPodeConfirmarEDeixarFrequenciaConcorrenteValidaPorContextoAntigo` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#confirmacaoDisputaLockRealDoContratoAntesDeValidarReplay` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#publicacaoCongelaConfiguracaoDefinitivaERevalidaMudancaAposPrevia` | APROVADO_XML_CANDIDATA | 2 |

## AUD-005 — PRESERVADO

Operações negociais devem impedir duplicidade acidental por clique repetido, reenvio ou falha de rede.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| AUD-005:001 | Reenvio de mesma intenção não cria fato duplicado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-005:002 | Mesma chave com intenção diferente conflita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-005:003 | Autorização é revalidada antes de devolver resposta idempotente | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-005:004 | Ocorrência identifica intenção normalizada e arquivos sob bloqueio de contexto, chave explícita ou digest determinístico. Mesmo reenvio retorna mesmo fato; intenção divergente409; duas transações concorrentes confirmam uma única fonte. PUT idêntico de calendário/complemento não acrescenta versão. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-005:005 | Reenvio operacional de frequência (sequencial ou simultâneo, com/sem chave) preserva versão e cadeia; mesma chave com intenção diferente retorna409; textos com separadores têm fronteiras inequívocas no hash. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#reenvioComMesmaChaveNaoDuplicaFatoArquivoHistoricoNemFrequencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#reenviosRealmenteConcorrentesConfirmamUmaUnicaFonteNoMysql` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioSemContratoConcorrenciaHttpIT#cadastrosConcorrentesSemContratoNaoPublicamDuasFontesIguais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoEmissaoIdempotenciaConcorrenciaHttpIT#mesmaChaveConcorrenteConfirmaUmaEmissaoEDevolveMesmaProvaAosDoisClientes` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#rollbackDaCriacaoReverteFonteContextosArquivoEHistoricoAssinado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.IdempotenciaFontesFrequenciaHttpIT#reenvioDePutIdenticoNaoDuplicaVersaoMasNovaAlteracaoPermaneceDocumentada` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoFrequenciaIdempotenciaOperacionalHttpIT#reenvioOperacionalPreservaVersaoCadeiaEAutorComOuSemChave` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoFrequenciaIdempotenciaOperacionalHttpIT#mesmaChaveOperacionalComIntencaoDiferenteConflitaSemMutacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoFrequenciaIdempotenciaOperacionalHttpIT#reenviosOperacionaisConcorrentesConfirmamUmUnicoFato` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoFrequenciaIdempotenciaOperacionalHttpIT#novaAlteracaoSemChaveContinuaVersionadaEAuditavel` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoFrequenciaIdempotenciaOperacionalHttpIT#camposTextuaisComSeparadorNaoColidemNaIntencaoGerencial` | APROVADO_XML_CANDIDATA | 1 |

## AUD-006 — PRESERVADO

Central de pendências deve identificar origem, motivo, responsável, estado e escopo afetado; cada pendência bloqueia somente o necessário.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| AUD-006:001 | Central identifica origem, motivo, datas, responsável e estado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-006:002 | Pendência bloqueia só recorte atingido | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-006:003 | Responsabilidade usa Gestão efetiva, inclusive após transferência | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-006:004 | Pendência de fim natural/conflito usa Gestão efetiva no dia do efeito. Aviso de relatório respeita o recorte congelado da emissão, inclusive consulta institucional nacional. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-006:005 | Pendência de frequência acompanha a responsabilidade efetiva por Gestão/Lotação/Turno após transferência retroativa, inclusive revisão já aberta, sem reescrever fotografia, autoria ou versão original da fonte. Relatório do responsável avisa revisão e deixa o aviso após correção e resolução comprovadas. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-006:006 | Todas as pendências ainda ativas da mesma frequência, inclusive versões anteriores em tratamento, acompanham a responsabilidade efetiva após transferência; IDs, chaves, motivo, surgimento, autoria de início e fotografia da fonte permanecem. Correção humana resolve as revisões da origem e retira o aviso. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#iniciarTratamentoNaoResolveConflitoDeCapacidadeAindaPresente` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#conflitoAbertoExigeTratamentoEContinuaPersistido` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#cancelamentoDoFatoFuturoPermiteResolverSemApagarAOrigem` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#retificacaoDoFatoPosteriorPermiteResolverComAutoriaESemApagarOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#gestorDeOutraGestaoEPerfilEstadualNaoResolvemConflito` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#origemDeConflitoAusenteNaoEquivaleACausaResolvida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#origemDeConflitoIncoerenteNaoEquivaleACausaResolvida` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01PendenciasHttpIT#confirmacaoDisputaLockRealDoContratoAntesDeValidarReplay` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPendenciaHistoricaAposTransferenciaHttpIT#responsavelPeloDiaHistoricoPodeIniciarPendenciaAposTransferenciaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPendenciaFimNaturalProrrogacaoHttpIT#consultaNaoCriaPendenciaNoFimAntigoQuandoProrrogacaoJaVigora` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPendenciaFimNaturalProrrogacaoHttpIT#consultaDepoisDoNovoFimGeraUmaPendenciaNaGestaoEfetivaDoNovoFim` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPendenciaFimNaturalProrrogacaoHttpIT#fimReduzidoProgramadoGeraPendenciaAntesDoFimMaterializadoAntigo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalResolucaoPendenciaFimSuperadoHttpIT#tarefaLegadaSemCausaAtualPodeSerResolvidaSemEncerrarVinculoVigente` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalResolucaoPendenciaFimSuperadoHttpIT#resolverTarefaAntigaPreservaTarefaDoNovoFimAindaNaoTratado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPendenciaFimControlesHttpIT#ausenciaDeFimCanonicoNaoEquivaleACausaResolvidaMesmoComGestaoComprovada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalPendenciaFimControlesHttpIT#causaNaturalQueVoltaPorAtosValidosGeraNovaOcorrenciaSemReabrirResolucaoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PendenciaEscopoEfetivoHttpIT#fimNaturalAposTransferenciaAtribuiPendenciaAoDestinoEConservaDataReal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RelatorioComparacaoEscopoHttpIT#comparacaoDeEmissaoLocalContaSomentePendenciasDoRecortePublicado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRetroativaEntregaPendenciaEAvisoAoResponsavelSemReescreverFonte` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PendenciaFrequenciaResponsabilidadeAtualHttpIT#transferenciaRealinhaTodasAsRevisoesAbertasDeVersoesAnterioresSemAlterarSuaHistoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud01RecorrenciaPendenciaHttpIT#recorrenciaAbreNovaPendenciaSemReescreverResolucaoAnteriorNemDuplicarAtiva` | APROVADO_XML_CANDIDATA | 1 |

## AUD-007 — PRESERVADO

Retificação de retificação é permitida sem limite artificial; preservar cadeia completa e identificar a versão vigente em cada ponto temporal.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| AUD-007:001 | Retificação de retificação sem teto artificial | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-007:002 | Cadeia completa permanece consultável | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-007:003 | Versão vigente e datas são inequívocas | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| AUD-007:004 | Retificação de retificação preserva versões sucessivas também nas fontes calendário/exceção/adicional; conhecidoAte seleciona apenas conhecimento existente no corte. Correção de uma anulação não invalida outra decisão independente. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CalendarioVersionamentoFontesHttpIT#retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaComplementosHistoricoHttpIT#retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.contrato.MotorTemporalContratoLacunasTest#corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.contrato.MotorPrecedenciaCicloVidaTest#correcaoDeUmaAnulacaoNaoRetiraValidadeDeOutraAnulacaoIndependente` | APROVADO_XML_CANDIDATA | 1 |

## PER-001 — PRESERVADO

Gestor Local é o principal perfil de escrita negocial do módulo no escopo de sua Gestão.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-001:001 | Gestor Local escreve somente no próprio escopo | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-001:002 | Rotas canônicas e legadas aplicam a mesma fronteira | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-001:003 | Ocorrência respeita Gestão efetiva da data do fato | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-001:004 | Criação e cancelamento exigem Gestor Local na Gestão efetiva de todos os dias do fato; gravação e histórico não dependem dos campos atuais materializados. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-001:005 | Uma ocorrência legada sem associação canônica nem fotografia vinculada não pode receber a identidade, o contrato ou a Gestão de outra Pessoa por coincidência atual de CPF. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoApiCanonicaHttpIT#rotasDeEscritaCompatibilidadeECrudDescontinuadoNaoSaoMapeadas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#ocorrenciaIndivisivelDeDoisDiasExigeEscopoEmTodoOPeriodo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaIdentidadeLegadaHttpIT#ocorrenciaSemProvaDeIdentidadeNaoAdotaPessoaAtualPeloCpfReutilizado` | APROVADO_XML_CANDIDATA | 1 |

## PER-002 — PRESERVADO

Gestor Estadual consulta/audita as Gestões Locais de seu estado e não executa aditamento/remanejamento/encerramento/retificação negocial.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-002:001 | Estadual consulta somente o próprio estado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-002:002 | Não executa atos negociais | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-002:003 | Limite territorial segue o fato histórico consultado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-002:004 | Consulta de ocorrência usa estado da Gestão reconstruída para a data efetiva; não libera escrita ao Gestor Estadual. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaEscopoAposPerdaCoberturaHttpIT#anulacaoNaoDevolveOcorrenciaNemAnexoAGestaoSuperadaPorTransferenciaRetroativa` | APROVADO_XML_CANDIDATA | 1 |

## PER-003 — PRESERVADO

Gestor Nacional atua em consulta/auditoria nacional neste módulo, sem escrita negocial.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-003:001 | Gestor Nacional consulta/audita nacionalmente | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-003:002 | Escrita negocial continua exclusiva ao Gestor Local | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-003:003 | Concessão pessoal não concede escrita | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel` | APROVADO_XML_CANDIDATA | 1 |

## PER-004 — PRESERVADO

Presidente atua somente em consulta nacional neste módulo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-004:001 | Presidente consulta nacional sem escrita | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-004:002 | Contratos/pessoas/vínculos institucionais minimizados | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-004:003 | Documento negocial permitido no escopo nacional | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-004:004 | Presidência consulta documentos negociais e DTOs institucionais minimizados das ocorrências em âmbito nacional; GET não libera comandos de criação/cancelamento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPendenciaNarrativaHttpIT#observacaoPessoalDeTratamentoNaoAmpliaExposicaoNaCentral` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal` | APROVADO_XML_CANDIDATA | 6 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud05PainelCapacidadesHttpIT#calculoPersistidoComUmaCapacidadeDivergenteNaoPodeSerLido` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud05PainelCapacidadesHttpIT#mesmosAlcanceECapacidadesPermitemCompartilhamentoDoAgregado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud05PainelCapacidadesHttpIT#mudancaParaPresidenciaRevogaLeituraDeCalculoDetalhadoMesmoComMesmaSessao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado` | APROVADO_XML_CANDIDATA | 1 |

## PER-005 — PRESERVADO

Administrador técnico não deve ser confundido com responsável negocial; intervenção excepcional exige permissão explícita e auditoria forte.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-005:001 | Administração concede permissões técnicas | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-005:002 | Não ganha escrita negocial comum | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-005:003 | Acesso funcional pessoal exige concessão e uso auditado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-005:004 | Administrador técnico permanece sem escrita negocial; documento pessoal exige intervenção excepcional vigente, finalidade e trilha de uso. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#concessaoInvalidaNaoAmpliaPrivilegios` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#permissaoPessoalTemEscopoValidadeUsoAuditadoERevogacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#administracaoAcessaDocumentoPessoalApenasDuranteSuporteExcepcionalAuditado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaDadosRestritosNovaAuditoriaHttpIT#auditoriaTecnicaNaoContornaMinimizacaoDosDadosPessoais` | APROVADO_XML_CANDIDATA | 1 |

## PER-006 — PRESERVADO

Gestor Estadual visualiza apenas os dados pessoais necessários à auditoria.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-006:001 | Estadual recebe somente dados necessários | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-006:002 | CPF mascarado e opcionais ausentes | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-006:003 | Narrativas livres de ocorrência não escapam na lista | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-006:004 | Lista/detalhe de ocorrência e narrativas livres de Pessoa, vínculo, alocação e excedente passam pela política central de minimização; fontes persistidas não são alteradas. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#auditoresRecebemCpfMascaradoEOpcionalRestritoAusente` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelAtividadesHttpIT#painelNaoContornaMinimizacaoDoHistoricoGeradoPorEdicaoRealDePessoa` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPendenciaNarrativaHttpIT#observacaoPessoalDeTratamentoNaoAmpliaExposicaoNaCentral` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalMetadadosEmissaoMinimizacaoHttpIT#motivoPessoalDaRetificacaoNaoVazaNosMetadadosDaEmissao` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasNarrativasMinimizacaoHttpIT#motivoEImpactosDaCorrecaoPessoalSaoMinimizadosSemAlterarFonte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasNarrativasMinimizacaoHttpIT#justificativaDoEventoDeVinculoEMinimizadaSemAlterarFonte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasNarrativasMinimizacaoHttpIT#justificativaDoEventoDeAlocacaoEMinimizadaSemAlterarFonte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasExcedenteNarrativasHttpIT#excedentesMinimizamNarrativasDeVinculoEAlocacaoPreservandoQuantitativos` | APROVADO_XML_CANDIDATA | 1 |

## PER-007 — PRESERVADO

Correção excepcional de CPF é autorizada pelo Gestor Local.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-007:001 | Correção CPF é ato do Gestor Local | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-007:002 | Novo CPF validado e único | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-007:003 | Motivo/evidência/autorização/valores anterior e novo preservados | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasCriteriosComplementaresHttpIT#correcaoCpfDocumentadaEColisaoPreservamAsDuasIdentidades` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalCorrecaoCpfControlesHttpIT#cpfNaoUniformeComDigitoAdulteradoRecusaCorrecaoEPreservaIdentidadeETrilha` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalCorrecaoCpfControlesHttpIT#correcoesDePessoasDistintasDisputamMesmoCpfComEsperaRealERollbackDaPerdedora` | APROVADO_XML_CANDIDATA | 1 |

## PER-008 — PRESERVADO

Correção excepcional de número/ano exige permissão específica concedida pela administração técnica; o ato negocial permanece com o Gestor Local autorizado.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-008:001 | Correção de identificador exige concessão específica ativa | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-008:002 | Administração concede, Gestor Local executa | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-008:003 | Uso auditado e alias antigo reservado/pesquisável | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.DocumentosPermissoesTransferenciaGestaoHttpIT#gestorDestinoComConcessaoExistenteCorrigeIdentificadorEPreservaAliasUsoEAuditoria` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#concessaoInvalidaNaoAmpliaPrivilegios` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#cadastroNaoReutilizaIdentificadorAntigoEPesquisaHistoricaRespeitaEscopo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#rascunhoNaoAssumeIdentificadorHistoricoDeOutroContrato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#correcaoExcepcionalNaoAssumeIdentificadorHistoricoDeOutroContratoNemPublicaFatosParciais` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#doisCadastrosConcorrentesRejeitamIdentificadorHistoricoSemCriarContrato` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#correcoesConcorrentesParaIdentidadeLivreConfirmamUmaSoTransacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#rascunhoNuncaPublicadoLiberaIdentidadeAoDescartarOuAlterar` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud04IdentidadeContratualHttpIT#identificadorHistoricoPodeVoltarAoMesmoContratoComNovaCorrecaoAuditada` | APROVADO_XML_CANDIDATA | 1 |

## PER-009 — PRESERVADO

Gestor Estadual, Gestor Nacional e Presidente podem consultar documentos negociais dentro de seus respectivos escopos de consulta/auditoria. Dados pessoais desnecessários à auditoria permanecem protegidos.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-009:001 | Perfis de auditoria consultam documentos negociais no seu escopo | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-009:002 | Conteúdo pessoal não é liberado pelo simples acesso ao evento | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-009:003 | Documento restrito exige concessão | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-009:004 | Documento negocial de ocorrência tem consulta autorizada pelo contexto efetivo; Presidente recebe conteúdo negocial nacional com dados pessoais minimizados no DTO. Classificação pessoal continua exigindo concessão. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.DocumentosContratuaisHttpIT#documentoRestritoExigeFinalidadePermissaoEAuditaAcessoComRevogacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#presidenciaAcessaDocumentoPessoalSomenteComConcessaoEspecificaAuditada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#anexoNegocialRevalidaContextoEfetivoAteDepoisDoCancelamento` | APROVADO_XML_CANDIDATA | 1 |

## PER-010 — PRESERVADO

Permissão histórica de frequência usa autorização atual do usuário combinada com o escopo histórico do registro.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-010:001 | Autorização atual sobre contexto histórico do registro | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-010:002 | Mudança de Lotação/Turno não transfere acesso a fatos de outra data | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-010:003 | Revogação atual remove acesso | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-010:004 | DIF-02: uma política temporal compartilhada resolve Lotação/Turno/Gestão do fato, cobre detalhe, lista, filtros antes de paginação, total, download, criação, cancelamento e concessão. Contextos de ocorrência são fotografados; correções retroativas revalidam a linha temporal; ausência de prova bloqueia acesso. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-010:005 | Uma ocorrência legada sem associação canônica nem fotografia vinculada não pode receber a identidade, o contrato ou a Gestão de outra Pessoa por coincidência atual de CPF. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#consultaEListaUsamLotacaoETurnoEfetivosMesmoComProjecaoDefasada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.HistoricoOcorrenciaContextoAtualHttpIT#historicoVinculadoSegueResponsabilidadeDaOcorrenciaSemReescreverProvaOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaEscopoAposPerdaCoberturaHttpIT#anulacaoNaoDevolveOcorrenciaNemAnexoAGestaoSuperadaPorTransferenciaRetroativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#ausenciaTotalDeFotografiaNaoUsaGestaoMaterializadaComoProvaDoPassado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#corteDeConhecimentoAnteriorAFonteNaoPodeUsarResponsabilidadeConhecidaDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaDeTransferenciaSemOrigemNaoAntecipaDestinoMasProvaDestinoDesdeADataDoAto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaLegadaPosteriorProvaSomenteSeuProprioPeriodo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#consultaUsaResponsabilidadeRetroativaSemReescreverFotografia` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#paginaIntermediariaETotalContemSomenteLinhasAutorizadas` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#filtrosOrganizacionaisSaoAplicadosAResponsabilidadeCorrigida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud06FrequenciaEscopoPaginaHttpIT#consultaPorIdNaoConcedeAcessoAoEscopoAnteriorDaFotografia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelAtividadesHttpIT#painelNaoExpoeNarrativaDeOcorrenciaForaDaResponsabilidadeHistoricaCorrigida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPendenciaHistoricaAposTransferenciaHttpIT#responsavelPeloDiaHistoricoPodeIniciarPendenciaAposTransferenciaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#fatoAnteriorConservaEscopoAntigoMesmoDepoisDeAtualizarAProjecao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#ocorrenciaIndivisivelDeDoisDiasExigeEscopoEmTodoOPeriodo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoDownloadsAutorizacaoAtualHttpIT#downloadRestritoRevalidaRevogacaoSemAlterarBytesOuAuditoriaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoDownloadsAutorizacaoAtualHttpIT#exportacaoProntaRevalidaAutorizacaoAtualSemConsumirArquivoRecusado` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoExportacaoGeracaoAutorizacaoHttpIT#mudarERestaurarAutorizacaoDuranteGeracaoNaoPublicaArquivoDePoliticaIncerta` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoCacheRedisRealHttpIT#painelEfetivoTemMissHitSemCompartilharDetalhamentoComPresidencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoCacheRedisRealHttpIT#paginaEmRedisNaoSobreviveARevogacaoDaLotacaoDoOperacional` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoCacheRedisRealHttpIT#transferenciaRetroativaInvalidaPaginasPositivaENegativaNoRedis` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaIdentidadeLegadaHttpIT#ocorrenciaSemProvaDeIdentidadeNaoAdotaPessoaAtualPeloCpfReutilizado` | APROVADO_XML_CANDIDATA | 1 |

## PER-011 — PRESERVADO

Gestor Nacional e Presidente seguem minimização de dados pessoais: somente dados necessários à auditoria nacional/consulta institucional; conteúdo pessoal/restrito adicional depende de permissão específica.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-011:001 | Nacional e Presidente recebem conteúdo minimizado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-011:002 | Conteúdo adicional requer concessão específica | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-011:003 | A concessão é revalidada e usada com auditoria | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-011:004 | Nacional e Presidente recebem narrativas minimizadas e documentos pessoais somente com finalidade e concessão explícita vigente; a Presidência não recebe nome na consulta institucional padrão. Mesma política aplicada às narrativas de identidade/vínculo/alocação/excedentes. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal` | APROVADO_XML_CANDIDATA | 6 |
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#auditoresRecebemCpfMascaradoEOpcionalRestritoAusente` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoDownloadsAutorizacaoAtualHttpIT#exportacaoProntaRevalidaAutorizacaoAtualSemConsumirArquivoRecusado` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoExportacaoGeracaoAutorizacaoHttpIT#mudarERestaurarAutorizacaoDuranteGeracaoNaoPublicaArquivoDePoliticaIncerta` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud05PainelCapacidadesHttpIT#calculoPersistidoComUmaCapacidadeDivergenteNaoPodeSerLido` | APROVADO_XML_CANDIDATA | 5 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud05PainelCapacidadesHttpIT#mesmosAlcanceECapacidadesPermitemCompartilhamentoDoAgregado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoAud05PainelCapacidadesHttpIT#mudancaParaPresidenciaRevogaLeituraDeCalculoDetalhadoMesmoComMesmaSessao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPainelAtividadesHttpIT#painelNaoContornaMinimizacaoDoHistoricoGeradoPorEdicaoRealDePessoa` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.PreauditoriaPendenciaNarrativaHttpIT#observacaoPessoalDeTratamentoNaoAmpliaExposicaoNaCentral` | APROVADO_XML_CANDIDATA | 3 |
| `com.example.siggtrat.auditoria20261029.AuditoriaFinalMetadadosEmissaoMinimizacaoHttpIT#motivoPessoalDaRetificacaoNaoVazaNosMetadadosDaEmissao` | APROVADO_XML_CANDIDATA | 4 |
| `com.example.siggtrat.auditoria20261029.PermissoesMinimizacaoHttpIT#permissaoPessoalTemEscopoValidadeUsoAuditadoERevogacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#presidenciaAcessaDocumentoPessoalSomenteComConcessaoEspecificaAuditada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoDownloadsAutorizacaoAtualHttpIT#downloadRestritoRevalidaRevogacaoSemAlterarBytesOuAuditoriaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoDownloadExpiracaoHttpIT#concessaoValidaDeixaDeAutorizarBytesDepoisDoPrazoSemRevogacaoOuAlteracaoDaProva` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasNarrativasMinimizacaoHttpIT#motivoEImpactosDaCorrecaoPessoalSaoMinimizadosSemAlterarFonte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasNarrativasMinimizacaoHttpIT#justificativaDoEventoDeVinculoEMinimizadaSemAlterarFonte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasNarrativasMinimizacaoHttpIT#justificativaDoEventoDeAlocacaoEMinimizadaSemAlterarFonte` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasExcedenteNarrativasHttpIT#excedentesMinimizamNarrativasDeVinculoEAlocacaoPreservandoQuantitativos` | APROVADO_XML_CANDIDATA | 1 |

## PER-012 — PRESERVADO

Transferência excepcional de Gestão Local exige autorização administrativa específica e execução por Gestor Local autorizado; Gestor Estadual não recebe escrita negocial.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| PER-012:001 | Transferência de Gestão exige autorização administrativa específica | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-012:002 | Ator é Gestor Local autorizado | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| PER-012:003 | Estadual não passa a executar negócio | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.DocumentosPermissoesTransferenciaGestaoHttpIT#concessaoNegocialDepoisDaTransferenciaExigeResponsavelEfetivo` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.TransferenciaConcessaoObrigatoriaHttpIT#transferenciaSemConcessaoPreservaTodaOrigemEPassaSomenteAposAutorizacaoEspecifica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.OcorrenciaAutorizacaoTemporalHttpIT#transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos` | APROVADO_XML_CANDIDATA | 1 |

## HIS-001 — BLOQUEADO

Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos.

**Validação:** BLOQUEADO_DOCUMENTAL_COM_TESTES. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| HIS-001:001 | Posição contratual apresenta empresa, tipo, composição, efetivo e origem de eventos da data. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-001:002 | Novas publicações/aditamentos fotografam política de matrícula e redução de jornada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-001:003 | Consulta legada sem configuração original explicita desconhecimento e não adota o mestre atual. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-001:004 | Habilitacao original ausente permanece desconhecida; ato posterior define somente seu periodo comprovado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#instrumentoSomenteQuantitativoNaoTrocaOsTresRotulosPublicadosPelosMestresAtuais` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#combinacaoRealmenteNovaDeAditamentoPreservaSeusPropriosRotulosDocumentados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#cancelarRemanejamentoFuturoDeveSinalizarOInstrumentoPosteriorQueFicaIncompativel` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AuditoriaCancelamentoRemanejamentoReplayHttpIT#cancelamentoComPosteriorCompativelPreservaDeltasEHistoricoSemCriarPendencia` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#alterarTipoMestreNaoDispensaMatriculaExigidaNaPublicacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoAlteraConfiguracoesNegociaisPreservandoFotoOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#legadoSemPoliticaComprovadaExigeTratarMatriculaAntesDeCriarVinculoSemEla` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaTotalOuCamposVaziosNaoAutorizaProjecaoAtualComoPassado` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaDaPublicacaoNaoPromoveFotografiaDeAditamentoABaseOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#fotografiaRegistradaDepoisDoCorteNaoPodeAparecerComoConhecida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#ausenciaTotalDeFotografiaNaoUsaGestaoMaterializadaComoProvaDoPassado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#corteDeConhecimentoAnteriorAFonteNaoPodeUsarResponsabilidadeConhecidaDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaDeTransferenciaSemOrigemNaoAntecipaDestinoMasProvaDestinoDesdeADataDoAto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaLegadaPosteriorProvaSomenteSeuProprioPeriodo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#instrumentoQuantitativoNaoCompletaRotulosLegadosAusentesComOsMestresAtuais` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#instrumentoQuantitativoPreservaAusenciaDeConfiguracaoDaComposicaoLegada` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#combinacaoNovaDeAditamentoLegadoNaoEmprestaConfiguracaoDeOutraComposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#composicaoLegadaSemFonteNaoCapturaMestreNemEmPreUpdate` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#habilitacaoNaoFotografadaPermaneceDesconhecidaMesmoComProjecaoAtual` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#primeiroAtoComprovadoDefineHabilitacaoSomenteDesdeSuaData` | APROVADO_XML_CANDIDATA | 2 |

## HIS-002 — PRESERVADO

Consulta histórica do colaborador deve mostrar todos os vínculos e períodos de alocação, inclusive mudanças dentro do mês.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| HIS-002:001 | História retorna todos os vínculos autorizados da Pessoa e todas as faixas de Alocação no mês. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-002:002 | Nome histórico usa fotografia do fato, separado do cadastro atual. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.FrequenciaHistoricoIndicadoresAuditoriaHttpIT#duasAlocacoesDoMesmoColaboradorNoMesNaoDuplicamPopulacaoDoResumo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento` | APROVADO_XML_CANDIDATA | 1 |

## HIS-003 — PRESERVADO

Pesquisa por início/fim de vínculo não pode confundir transferência interna de alocação com desligamento/recontratação.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| HIS-003:001 | Filtros por início/fim selecionam datas de Vínculo. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-003:002 | Mudança interna de Alocação não aparece como novo vínculo ou desligamento. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#pesquisaDesligadosNaoRetornaVinculoAindaAberto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasCriteriosComplementaresHttpIT#pesquisaDeInicioUsaMesEfetivoEmVezDoRegistro` | APROVADO_XML_CANDIDATA | 1 |

## HIS-004 — PRESERVADO

Cadastros mestre atuais não reescrevem fotografia histórica do contrato/vínculo.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| HIS-004:001 | Alterar cadastro atual de Pessoa/Cargo/Lotação/Turno/Tipo/Gestão não modifica fotografias anteriores. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-004:002 | Fotografias ausentes não são completadas pelos mestres atuais. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.HistoricosEmpregadosContratoHttpIT#historicoContratualLegadoNaoMudaQuandoContratoEGestaoSaoRenomeados` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.etapa07.MotorTemporalAlocacaoEtapa07Test#fotografiaDoEventoPreservaNomesDaAlocacaoHistorica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.CatalogosPessoasConclusaoHttpIT#posicaoSemEventoIdentitarioNaoUsaNomeAtualComoFonteHistorica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#transferenciaRetroativaNaoTrocaNomeComprovadoDaOrigemPeloMestreRenomeado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#atoDeMatriculaRetroativoNaoEmprestaNomeAtualAoPeriodoAnterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaRotuloAuditoriaIndependenteHttpIT#inclusaoFotografaRotuloComprovadoParaDataEfetiva` | APROVADO_XML_CANDIDATA | 6 |
| `com.example.siggtrat.auditoria20261029.RevisaoNomeGestaoEventoVinculoHttpIT#atoDeMatriculaPreservaNomeDaGestaoDoRecorteComFonteOriginal` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#novaAlocacaoRetroativaNaoTransformaGradeEMunicipioAtuaisEmConfiguracaoPassada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#publicacaoCongelaConfiguracaoDefinitivaERevalidaMudancaAposPrevia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#combinacaoNovaDeAditamentoPreservaConfiguracaoPropriaDepoisDaMudancaDoMestre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#instrumentoSomenteQuantitativoNaoRedefineConfiguracaoDaCombinacaoExistente` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#calendarioMunicipalUsaMunicipioOriginalDaNovaAlocacaoRetroativa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#instrumentoSomenteQuantitativoNaoTrocaOsTresRotulosPublicadosPelosMestresAtuais` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaCatalogosIdentidadeHttpIT#ausenciaTotalDeFotoNominalPermaneceDesconhecidaDepoisDaCorrecaoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaNominalHttpIT#ausenciaNominalPreservaDesconhecimentoRetroativoECapturaContemporanea` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#ausenciaLegitimaDaGradeNaoEPreenchidaPelaAlteracaoAtualDoMestre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#composicaoLegadaSemFonteNaoCapturaMestreNemEmPreUpdate` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#municipioOriginalLegitimamenteNuloNaoECompletadoPeloMestrePosterior` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#instrumentoQuantitativoPreservaAusenciaDeConfiguracaoDaComposicaoLegada` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#combinacaoNovaDeAditamentoLegadoNaoEmprestaConfiguracaoDeOutraComposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT#instrumentoQuantitativoNaoCompletaRotulosLegadosAusentesComOsMestresAtuais` | APROVADO_XML_CANDIDATA | 2 |

## HIS-005 — PRESERVADO

História é a fonte canônica; projeções atuais são conveniência técnica.

**Validação:** TESTADO_NOS_CENARIOS_REFERENCIADOS. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| HIS-005:001 | Motores históricos usam a cadeia de eventos conhecida no corte. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-005:002 | Projeção Empregado não sobrepõe Vínculo/Alocação canônicos nem ressuscita programação cancelada. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-005:003 | Eventos novos de reprocessamento preservam a visão anterior ao seu registro. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.VinculoConhecimentoSemOrigemHttpIT#primeiroEventoConhecidoProvaEstadoAnteriorSemHerdarLimiteModificadoDepois` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.VinculoConhecimentoSemOrigemHttpIT#corteAnteriorAPrimeiraFonteNaoCertificaProjecaoAtualComoEstadoHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasLacunasHttpIT#corteAnteriorAoCadastroNaoRevelaVinculoDaProjecaoAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.RelatorioFrequenciaCanonicaIT#alocacaoCanonicaPrevaleceSobreDimensaoLegadaSemDuplicarPessoa` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.integration.RelatorioFrequenciaCanonicaIT#vinculoCanonicoCanceladoNaoRessurgePeloHistoricoLegado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.PessoasProjecaoHabilitacaoHttpIT#opcionalNaPessoaAposInativacaoEReativacaoMantemMesmaLinhaCanonica` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#retificacaoDaTransferenciaPreservaAlocacaoIndependentePosteriorEVisaoAnterior` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoTransferenciaAlocacaoHttpIT#cancelarTransferenciaFuturaRestauraCoberturaOriginalSemExcluirHistorico` | APROVADO_XML_CANDIDATA | 1 |

## HIS-006 — BLOQUEADO

A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores.

**Validação:** BLOQUEADO_DOCUMENTAL_COM_TESTES. Atendimento integral não declarado.

| Critério recebido | Texto preservado | Evidência dos métodos referenciados |
|---|---|---|
| HIS-006:001 | Publicação fotografa o cargo efetivamente selecionado no rascunho e a base quantitativa original. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-006:002 | Novas fotografias preservam todas as configurações operacionais contratuais e ADI conserva a original. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-006:003 | Legado incompleto não recebe configuração atual retroativamente como se fosse a publicação original. | TESTADO_NOS_CENARIOS_REFERENCIADOS |
| HIS-006:004 | Habilitacao original ausente permanece desconhecida; ato posterior define somente seu periodo comprovado. | TESTADO_NOS_CENARIOS_REFERENCIADOS |

| Método atual | Resultado atribuível | Casos XML |
|---|---|---:|
| `com.example.siggtrat.auditoria20261029.ComposicaoFotografiaRascunhoHttpIT#editarCargoNoRascunhoPublicaFotografiaDoCargoEfetivamenteEscolhido` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#publicacaoFotografaMatriculaEDiaReduzidoSemDependerDoCadastroAtual` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#aditamentoAlteraConfiguracoesNegociaisPreservandoFotoOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratosLacunasFuncionaisHttpIT#alterarTipoMestreNaoDispensaMatriculaExigidaNaPublicacao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#publicacaoCongelaConfiguracaoDefinitivaERevalidaMudancaAposPrevia` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#novaAlocacaoRetroativaNaoTransformaGradeEMunicipioAtuaisEmConfiguracaoPassada` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#combinacaoNovaDeAditamentoPreservaConfiguracaoPropriaDepoisDaMudancaDoMestre` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#instrumentoSomenteQuantitativoNaoRedefineConfiguracaoDaCombinacaoExistente` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#posicaoLegadaNaoApresentaConfiguracaoAtualComoConhecimentoHistorico` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ContratoConfiguracaoLegadaHttpIT#legadoSemPoliticaComprovadaExigeTratarMatriculaAntesDeCriarVinculoSemEla` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaTotalOuCamposVaziosNaoAutorizaProjecaoAtualComoPassado` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#ausenciaDaPublicacaoNaoPromoveFotografiaDeAditamentoABaseOriginal` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoHistoricaAusenciaVersaoHttpIT#fotografiaRegistradaDepoisDoCorteNaoPodeAparecerComoConhecida` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#ausenciaTotalDeFotografiaNaoUsaGestaoMaterializadaComoProvaDoPassado` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#corteDeConhecimentoAnteriorAFonteNaoPodeUsarResponsabilidadeConhecidaDepois` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaDeTransferenciaSemOrigemNaoAntecipaDestinoMasProvaDestinoDesdeADataDoAto` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.RevisaoGestaoSemOrigemHttpIT#fotografiaLegadaPosteriorProvaSomenteSeuProprioPeriodo` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT#composicaoLegadaSemFonteNaoCapturaMestreNemEmPreUpdate` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#instrumentoQuantitativoPreservaAusenciaDeConfiguracaoDaComposicaoLegada` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.InstrumentosConfiguracaoHistoricaHttpIT#combinacaoNovaDeAditamentoLegadoNaoEmprestaConfiguracaoDeOutraComposicao` | APROVADO_XML_CANDIDATA | 1 |
| `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#habilitacaoNaoFotografadaPermaneceDesconhecidaMesmoComProjecaoAtual` | APROVADO_XML_CANDIDATA | 2 |
| `com.example.siggtrat.auditoria20261029.ConclusaoFotografiaContratoHttpIT#primeiroAtoComprovadoDefineHabilitacaoSomenteDesdeSuaData` | APROVADO_XML_CANDIDATA | 2 |
