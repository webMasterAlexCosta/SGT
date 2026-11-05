# Defeitos corrigidos e controles permanentes

**Evidência da versão entregue. Nenhuma aprovação de versão anterior foi importada.**

Candidata: `candidata07`. Manifesto de fontes SHA-256: `fc92555e0785d29ed9b8f2dd3c86235c7eec03992537f27b7f0bc8f5d3526f25`.

35 grupos de alterações no registro: 32 relacionados a requisitos e 3 de suporte sem RF. Um grupo pode afetar vários critérios; essa contagem não é número de requisitos. 32 grupos com RF possuem fechamento nos gates fornecidos. 1 grupos de suporte têm comprovação técnica separada, sem acrescentar RF. DEPENDENCIAS e WRAPPER conservam limites próprios e não são contados como defeitos funcionais fechados.

Cada registro identifica a reprodução, componentes, reparo e testes permanentes. `presente_na_base=null` significa que a reprodução foi feita depois de correções intermediárias e não será retroativamente atribuída à baseline.

## AUD-01

**Problema:** Pendência podia ser resolvida sem cessar a causa; resolução concorrente não revalidava a origem sob lock.

**Correção aplicada:** Releitura READ_COMMITTED sob bloqueio do Contrato; replay da causa efetiva; recorrência cria nova pendência sem reabrir prova antiga resolvida.

Requisitos diretamente relacionados: AUD-006, TMP-009.

Componentes: `PendenciaAdministrativaServico`.

Testes permanentes: `ConclusaoAud01PendenciasHttpIT`, `ConclusaoAud01RecorrenciaPendenciaHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## AUD-02

**Problema:** Motivos contratuais de desligamento eram aceitos sem ato correspondente ou em outra data.

**Correção aplicada:** Validação de categoria, documento e data do término, também nas prévias/retificações; preservação da exceção legítima após prorrogação.

Requisitos diretamente relacionados: VIN-009, VIN-010, VIN-015, DES-004, DES-005.

Componentes: `VinculoContratualServico`.

Testes permanentes: `ConclusaoAud02DesligamentoHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## AUD-03

**Problema:** Novo vínculo atravessava transferência futura sem aplicar integralmente mapas/capacidade ou antecipava projeção de destino.

**Correção aplicada:** Planejamento por todos os intervalos e mapas documentados; capacidades origem/destino e consentimento específico; projeção operacional usa apenas o intervalo atual.

Requisitos diretamente relacionados: CON-023, ALO-007, VIN-005, VIN-014.

Componentes: `AlocacaoVinculoServico`, `ProjecaoOperacionalPessoaServico`.

Testes permanentes: `ConclusaoAud03VinculosTransferenciaProgramadaHttpIT`, `VinculoAlocacaoConcorrenciaIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## AUD-04

**Problema:** Identificador publicado anterior à correção podia ser reutilizado por outro contrato.

**Correção aplicada:** Reserva permanente em entidade JPA, unicidade normalizada e vínculo ao contrato; validação da história já existente; teste de disputa em MySQL.

Requisitos diretamente relacionados: CON-001, CON-003, CON-019, CON-025.

Componentes: `ReservaIdentificadorContrato`, `IdentidadeContratualServico`, `ContratoServico`, `CorrecaoIdentificadorContratoServico`.

Testes permanentes: `ConclusaoAud04IdentidadeContratualHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## AUD-05

**Problema:** Mesmo alcance geográfico permitia reutilizar painel ou chave de cache sem conferir todas as capacidades/permissões persistidas.

**Correção aplicada:** Cinco capacidades no acesso ao cálculo persistido; perfil, Gestão, vínculos adicionais e escopo atual na chave Redis.

Requisitos diretamente relacionados: PER-004, PER-011.

Componentes: `PainelCalculoServico`, `ServicoChaveCacheRedis`.

Testes permanentes: `ConclusaoAud05PainelCapacidadesHttpIT`, `ConclusaoCacheRedisRealHttpIT`, `ChaveCacheEscopoRevogadoTest`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## AUD-06

**Problema:** Filtro/contagem/paginação da frequência precediam a autorização do escopo histórico retificado.

**Correção aplicada:** Candidatos em lotes; reconstrução de responsabilidade antes de filtrar, contar e paginar; consulta por ID/eventos/integridade e DTO seguem a mesma prova.

Requisitos diretamente relacionados: FRE-015, FRE-018, PER-010.

Componentes: `FrequenciaServico`, `FrequenciaConsultaRepositorio`, `FrequenciaDTO`.

Testes permanentes: `ConclusaoAud06FrequenciaEscopoPaginaHttpIT`, `ConclusaoCacheRedisRealHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## RH-ORIGEM

**Problema:** Ausência de fotografia original podia ser substituída por mestre atual ou primeira versão futura.

**Correção aplicada:** Origem publicada ou legado explicitamente datado e conhecido; campos não comprovados permanecem desconhecidos; retroatividade nominal exige fotografia adequada, sem inventar dados.

Requisitos diretamente relacionados: TMP-004, TMP-005, CON-023, CAT-004, CAT-007, COL-010, HIS-001, HIS-004, HIS-005, HIS-006.

Componentes: `AditamentoContratualServico`, `MotorTemporalContratoServico`, `GestaoContratoTemporalServico`, `VinculoContratualServico`, `TransferenciaGestaoContratoServico`.

Testes permanentes: `RevisaoHistoricaAusenciaVersaoHttpIT`, `RevisaoGestaoSemOrigemHttpIT`, `RevisaoHistoricaAusenciaNominalHttpIT`, `RevisaoHistoricaCatalogosIdentidadeHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## REL-INVALIDACAO

**Problema:** Alteração operacional dentro da janela não superava emissão anterior do relatório.

**Correção aplicada:** Toda alteração efetiva de frequência invalida/supera os relatórios afetados; reenvio idempotente conserva emissão e prova.

Requisitos diretamente relacionados: FRE-008, FRE-012, REL-005.

Componentes: `FrequenciaServico`.

Testes permanentes: `ConclusaoRelatoriosOperacionaisHttpIT`, `RevisaoEmissaoIdempotenciaConcorrenciaHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## PREVIA-ALCANCE

**Problema:** Prévia de retificação omitira a faixa do ato anterior; refinamento inicialmente tratava sucessão como substituição.

**Correção aplicada:** União do alcance antigo/novo em RETIFICA/SUBSTITUI/CANCELA, preservando intervalo anterior de SUCEDE e coexistência.

Requisitos diretamente relacionados: AUD-004, TMP-009, TMP-016.

Componentes: `PreviaCicloVidaContratoServico`.

Testes permanentes: `ConclusaoAlcancePreviaContratoHttpIT`, `RevisaoPreviaSucessaoAlcanceHttpIT`, `RevisaoPrecedenciaExplicitaHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## EXPORTACAO-AUTORIZACAO

**Problema:** Exportação já gerada podia ser lida com política revogada; mudança/restauração durante geração não era detectada.

**Correção aplicada:** Contexto persistido em REQUIRES_NEW/READ_COMMITTED; revalidação em status, preparação e início do streaming; versões antes/depois da geração; descarte seguro e preservação do arquivo recusado.

Requisitos diretamente relacionados: DOC-010, PER-010, PER-011, REL-005.

Componentes: `AutorizacaoExportacaoRelatorioServico`, `RelatorioExportacaoAssincronaServico`.

Testes permanentes: `RevisaoDownloadsAutorizacaoAtualHttpIT`, `RevisaoExportacaoGeracaoAutorizacaoHttpIT`, `RevisaoDownloadExpiracaoHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## PRE-001-002

**Problema:** Painel de atividades contornava minimização e responsabilidade temporal do histórico canônico.

**Correção aplicada:** Consulta canônica antes da paginação; retirada de três projeções alternativas inseguras; nome histórico desconhecido não é preenchido pelo mestre atual.

Requisitos diretamente relacionados: PER-006, PER-010, PER-011, HIS-004.

Componentes: `PainelServico`, `HistoricoEmpregadoRepositorio`.

Testes permanentes: `PreauditoriaPainelAtividadesHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## PRE-003

**Problema:** Regra segura nominal introduzida recusava antecipação legítima do início mesmo com CRIACAO documentada do próprio vínculo.

**Correção aplicada:** Exceção estrita para antecipação do mesmo fato: única fotografia de criação conhecida e não vazia; demais ausências continuam bloqueadas.

Requisitos diretamente relacionados: VIN-014, HIS-004.

Componentes: `VinculoContratualServico`.

Testes permanentes: `PreauditoriaAntecipacaoInicioVinculoHttpIT`, `RevisaoHistoricaAusenciaNominalHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## PRE-004

**Problema:** Narrativas da Central de Pendências expunham dados pessoais aos perfis minimizados.

**Correção aplicada:** Minimização de motivo e observação na projeção DTO, preservando o conteúdo original no banco.

Requisitos diretamente relacionados: PER-006, PER-011, AUD-006.

Componentes: `PendenciaAdministrativaServico`.

Testes permanentes: `PreauditoriaPendenciaNarrativaHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## PRE-005

**Problema:** Painel persistido podia permanecer atual após retificação ou publicar resultado antigo de worker concorrente.

**Correção aplicada:** Geração técnica global JPA na transação da fonte/lote; geração na chave Redis e payload; comparação no consumidor e conclusão; token rejeita worker obsoleto.

Requisitos diretamente relacionados: FRE-008, FRE-012, TMP-008.

Componentes: `RevisaoFontesPainel`, `RevisaoFontesPainelServico`, `RevisaoFontesPainelRepositorio`, `AspectoInvalidacaoCacheMutacaoApi`, `PainelCalculoEstadoServico`, `PainelCalculoServico`, `FrequenciaServico`, `FechamentoAutomaticoFrequenciaNegocialServico`.

Testes permanentes: `PreauditoriaPainelRetificacaoHttpIT`, `PreauditoriaPainelRetificacaoConcorrenteHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## PRE-006

**Problema:** Pendência histórica exigia simultaneamente Gestão responsável no dia e Gestão atual do contrato.

**Correção aplicada:** Bloqueio/releitura da origem e autorização pela responsabilidade efetiva da frequência, mantendo a negativa para o destino posterior.

Requisitos diretamente relacionados: AUD-006, PER-010.

Componentes: `PendenciaAdministrativaServico`.

Testes permanentes: `PreauditoriaPendenciaHistoricaAposTransferenciaHttpIT`.

Reprodução: Ver evidência específica.

Fechamento final: VERIFICADO nos gates da fonte atual.



## TRV-001-RETRY-EMISSAO

**Problema:** Retry de emissão concorrente reutilizava sessão abortada por transação externa introduzida no aspecto de invalidação.

**Correção aplicada:** Retirada da transação externa do controlador de relatório; revisão/invalidação participa da transação de cada tentativa do serviço.

Requisitos diretamente relacionados: AUD-005.

Componentes: `RelatorioAdministrativoControlador`, `RelatorioAdministrativoServico`.

Testes permanentes: `RevisaoEmissaoIdempotenciaConcorrenciaHttpIT`.

Reprodução: evidencias/rodadas/regressao_integral_01.

Fechamento final: VERIFICADO nos gates da fonte atual.



## TRV-002-REPLAY-VIGENCIA

**Problema:** Foto integral posterior de descrição podia apagar prorrogação anterior recebida depois nos consumidores operacionais.

**Correção aplicada:** Motor aplica deltas explícitos conhecidos de vigência em ordem válida, conserva encerramento próprio e alinha DTO com posição usada por vínculo/frequência/relatório.

Requisitos diretamente relacionados: TMP-004, TMP-005, TMP-008, TMP-012, TMP-014, TMP-017, VIN-005.

Componentes: `MotorTemporalContratoServico`, `AditamentoContratualRepositorio`, `VersaoContratoNegocialDTO`, `ContratoServico`.

Testes permanentes: `AuditoriaIndependenteReplayVigenciaHttpIT`, `MotorTemporalVigenciaReplayIT`, `ReplayVigenciaFrequenciaRelatorioHttpIT`.

Reprodução: evidencias/rodadas/novos_red_04.

Fechamento final: VERIFICADO nos gates da fonte atual.



## REM-CANCELAMENTO-REPLAY

**Problema:** Cancelamento de REM não sinalizava instrumento posterior incompatível, inclusive dentro de prorrogação futura.

**Correção aplicada:** Diagnóstico completo após cancelamento, no mesmo commit; pendência canônica deduplicada por evento; horizonte LocalDate.MAX.

Requisitos diretamente relacionados: REM-007, TMP-009, TMP-012.

Componentes: `RemanejamentoContratualServico`.

Testes permanentes: `AuditoriaCancelamentoRemanejamentoReplayHttpIT`.

Reprodução: evidencias/rodadas/novos_red_04.

Fechamento final: VERIFICADO nos gates da fonte atual.



## CAT-CONCORRENCIA-PUBLICACAO

**Problema:** Leitura de dependências antes de lock permitia inativar catálogo após publicação concorrente de uso futuro.

**Correção aplicada:** Locks compartilhados nas escolhas e exclusivos antes de alteração administrativa, releitura do estado, ordem de locks sem aquisição tardia invertida e isolamento READ_COMMITTED.

Requisitos diretamente relacionados: CAT-005, CAT-008.

Componentes: `DisponibilidadeCatalogoTemporalServico`, `ProgramacaoFuturaCatalogoServico`, `TransicaoDisponibilidadeCatalogoServico`, `CargoServico`, `LotacaoServico`, `JornadaServico`, `TipoContratacaoServico`, `MotivoDesligamentoServico`.

Testes permanentes: `CatalogoPublicacaoConcorrenteAuditoriaIndependenteHttpIT`.

Reprodução: evidencias/rodadas/novos_red_04.

Fechamento final: VERIFICADO nos gates da fonte atual.



## CAT007-CONTINUIDADE-FAIXA

**Problema:** Validação da continuidade de uma alocação retroativa tratava o fim futuro como nova escolha de catálogo.

**Correção aplicada:** Disponibilidade na data da decisão e no primeiro dia realmente acrescido; composição/capacidade permanecem verificadas em todas as fronteiras.

Requisitos diretamente relacionados: CAT-007, CAT-008.

Componentes: `AlocacaoVinculoServico`.

Testes permanentes: `CatalogosTemporalHttpIT`, `ConclusaoAud03VinculosTransferenciaProgramadaHttpIT`.

Reprodução: evidencias/rodadas/regressao_integral_01.

Fechamento final: VERIFICADO nos gates da fonte atual.



## ROTULOS-ALOCACAO-RETROATIVA

**Problema:** Nova alocação retroativa usava nomes do catálogo renomeado depois da data efetiva, apesar de fonte publicada anterior.

**Correção aplicada:** Rótulos são copiados da composição projetada para a data; lacunas não recebem fallback do mestre; atos contemporâneos e mapas próprios preservados.

Requisitos diretamente relacionados: CAT-004, HIS-004, TMP-004.

Componentes: `AlocacaoVinculo`, `AlocacaoVinculoServico`.

Testes permanentes: `AlocacaoRetroativaRotuloAuditoriaIndependenteHttpIT`.

Reprodução: evidencias/rodadas/novos_red_04.

Fechamento final: VERIFICADO nos gates da fonte atual.



## NOME-GESTAO-RETROATIVA

**Problema:** Evento retroativo de vínculo fotografava nome atual da Gestão apesar da fonte contratual antiga.

**Correção aplicada:** Projeção contratual datada fornece nome quando a identidade coincide; ausência permanece null; registro contemporâneo captura cadastro atual.

Requisitos diretamente relacionados: HIS-004, TMP-004, CAT-004.

Componentes: `VinculoContratualServico`.

Testes permanentes: `RevisaoNomeGestaoEventoVinculoHttpIT`.

Reprodução: evidencias/rodadas/novos_red_04.

Fechamento final: VERIFICADO nos gates da fonte atual.



## REL-CONHECIMENTO-SUPERACAO

**Problema:** Consulta conhecida até corte anterior à superação retornava metadados atuais SUPERADA.

**Correção aplicada:** DTO histórico reflete o estado conhecido no corte e oculta metadados de superação posteriores sem mutar entidade, snapshot ou hash.

Requisitos diretamente relacionados: REL-003, TMP-005, AUD-003.

Componentes: `RelatorioAdministrativoServico`.

Testes permanentes: `ConclusaoRelatoriosConhecimentoHttpIT`.

Reprodução: evidencias/rodadas/regressao_integral_01.

Fechamento final: VERIFICADO nos gates da fonte atual.



## DEPENDENCIAS-AVISOS-20261031

**Problema:** Consulta OSV encontrou21 alertas nas versões empacotadas; aplicabilidade é separada da presença da biblioteca.

**Correção aplicada:** Patch de Tomcat10.1.59/Jackson2.21.7/Netty4.1.137.Final/Log4j2.25.5. Spring6.2.19 permanece com2 avisos condicionais de SSE/XSLT, cuja ausência de capacidades é guardada e deve constar da auditoria.

Requisitos diretamente relacionados: Suporte técnico; sem atribuição automática de RF.

Componentes: `pom.xml`.

Testes permanentes: `SuperficieRenderizacaoSeguraHttpIT`.

Reprodução: evidencias/dependencias_osv/candidata01_rede.

Fechamento final: PENDENTE; conferir os limites específicos, sem atribuir conclusão.



## WRAPPER-WINDOWS-AUSENTE

**Problema:** mvnw.cmd referencia um script PowerShell ausente no checkpoint.

**Correção aplicada:** Script incluído, com download TLS e validação SHA256 da distribuição Maven. O CMD já tinha CRLF correto.

Requisitos diretamente relacionados: Suporte técnico; sem atribuição automática de RF.

Componentes: `.mvn/wrapper/mvnw.ps1`.

Testes permanentes: .

Reprodução: Ver evidência específica.

Fechamento final: PENDENTE; conferir os limites específicos, sem atribuir conclusão.



## CONFIGURACAO-RETROATIVA

**Problema:** Nova alocação retroativa capturava grade semanal e município atuais, alterando cálculos históricos; confirmação da publicação não assinava toda a configuração a congelar.

**Correção aplicada:** Configuração nullable em composição e instrumentos; projeção histórica completa; ausência preservada sem backfill; atos quantitativos conservam configuração; prévias assinam configuração relevante.

Requisitos diretamente relacionados: CAT-004, HIS-004, TMP-004, TMP-005, FRE-001, REL-001, AUD-004.

Componentes: `ComposicaoContratual`, `ComposicaoContratualTemporalServico`, `AlocacaoVinculoServico`, `AlocacaoVinculo`, `AditamentoContratualServico`, `RemanejamentoContratualServico`, `AssinaturaImpactoVinculoServico`, `PreviaCicloVidaContratoServico`.

Testes permanentes: `AlocacaoRetroativaConfiguracaoAuditoriaIndependenteHttpIT`, `InstrumentosConfiguracaoHistoricaHttpIT`.

Reprodução: evidencias/rodadas/novos_red_05.

Fechamento final: VERIFICADO nos gates da fonte atual.



## ROTULOS-INSTRUMENTOS-QUANTITATIVOS

**Problema:** Deltas quantitativos ADI/REM trocavam rótulos comprovados ou ausentes por nomes atuais dos mestres na projeção e na alocação retroativa.

**Correção aplicada:** Combinação existente conserva seus rótulos, inclusive null. Combinação criada por ADI usa fotografia própria. Itens publicados preservados.

Requisitos diretamente relacionados: CON-008, HIS-001, CAT-004, HIS-004, TMP-004, HIS-005.

Componentes: `ComposicaoContratualTemporalServico`.

Testes permanentes: `InstrumentosQuantitativosRotulosAuditoriaIndependenteHttpIT`.

Reprodução: evidencias/rodadas/novos_red_06.

Fechamento final: VERIFICADO nos gates da fonte atual.



## METADADOS-EMISSAO-MINIMIZACAO

**Problema:** Justificativa livre de retificação vazava em motivoSuperacao das emissões para leitores institucionais, nas consultas, comparações e listagens.

**Correção aplicada:** Mapper de metadados recebe leitor autorizado e aplica política restrito também ao motivo; contexto local conserva acesso, entidade/blob/hash não são alterados.

Requisitos diretamente relacionados: PER-006, PER-011, REL-003.

Componentes: `RelatorioAdministrativoServico`.

Testes permanentes: `AuditoriaFinalMetadadosEmissaoMinimizacaoHttpIT`.

Reprodução: evidencias/rodadas/novos_red_06.

Fechamento final: VERIFICADO nos gates da fonte atual.



## FIM-NATURAL-PRORROGADO

**Problema:** Tratamento de fim natural usava data materializada antiga e encerrava vínculo quando prorrogação programada já estava efetiva.

**Correção aplicada:** Lock de Contrato, leitura temporal canônica no conhecimento atual e fim efetivo; causa incompatível recusada; preservação do término legítimo prorrogado e reenvio sem duplicação.

Requisitos diretamente relacionados: VIN-008, VIN-009, VIN-010, HIS-005.

Componentes: `VinculoContratualServico`.

Testes permanentes: `AuditoriaFinalFimNaturalProrrogacaoHttpIT`.

Reprodução: evidencias/rodadas/novos_red_08.

Fechamento final: VERIFICADO nos gates da fonte atual.



## PENDENCIA-FIM-CANONICO

**Problema:** Pendência natural prematura, com data/Gestão incorretas após prorrogação e ausente após redução futura do fim, por usar exclusivamente a projeção materializada.

**Correção aplicada:** Seleção conservadora também por versões e ADI em lotes200/keyset; replay canônico após lock/refresh com READ_COMMITTED; fim documentado governa chave, data e responsabilidade.

Requisitos diretamente relacionados: VIN-008, VIN-009, HIS-005, AUD-006.

Componentes: `PendenciaAdministrativaServico`, `VinculoContratualRepositorio`.

Testes permanentes: `AuditoriaFinalPendenciaFimNaturalProrrogacaoHttpIT`.

Reprodução: evidencias/rodadas/novos_red_09 e novos_red_10.

Fechamento final: VERIFICADO nos gates da fonte atual.



## RESOLUCAO-FIM-SUPERADO

**Problema:** Tarefa antiga de fim natural permanecia irresolúvel mesmo quando a prorrogação comprovada havia superado sua causa; comando exigia desligamento artificial de vínculo ainda vigente.

**Correção aplicada:** Confirmação humana usa fim canônico conhecido sob lock/READ_COMMITTED; preserva origem, autoria e fatos negociais, e não resolve tarefa do novo fim ainda não tratado.

Requisitos diretamente relacionados: AUD-006.

Componentes: `PendenciaAdministrativaServico`.

Testes permanentes: `AuditoriaFinalResolucaoPendenciaFimSuperadoHttpIT`.

Reprodução: evidencias/rodadas/novos_red_12 (RED11 identificou espera ausente na fixture de auditoria assíncrona, não a falha funcional).

Fechamento final: VERIFICADO nos gates da fonte atual.



## RECORRENCIA-FIM-NATURAL

**Problema:** Ocorrência de fim natural já resolvida impedia criar outra tarefa quando atos válidos faziam a mesma causa/data voltar, pois só era consultada existência da chave histórica.

**Correção aplicada:** Deduplicação de tarefa ativa por vínculo/contrato/data e nova identidade para recorrência; resolução antiga permanece intacta. Produtores serializados pelo lock do contrato.

Requisitos diretamente relacionados: AUD-006, VIN-008.

Componentes: `PendenciaAdministrativaServico`.

Testes permanentes: `AuditoriaFinalPendenciaFimControlesHttpIT`.

Reprodução: evidencias/rodadas/novos_red_13.

Fechamento final: VERIFICADO nos gates da fonte atual.

Condição de existência da chave já estava no serviço, mas a reprodução foi executada após o reparo da resolução. Não atribui resultado dinâmico à baseline.

## CATALOGOS-VERSAO-CANONICA

**Problema:** Lotação/Jornada não expunham version no GET canônico; PUT de Cargo/Tipo devolvia versão anterior ao flush, impedindo usar de modo confiável versaoEsperada obtida pelo cliente.

**Correção aplicada:** Version incluída nos DTOs de Lotação e Jornada. Serviços de atualização dos quatro catálogos fazem flush dentro da transação antes de mapear a resposta, devolvendo a versão efetivamente confirmada.

Requisitos diretamente relacionados: CAT-007.

Componentes: `JornadaDTO`, `LotacaoDTO`, `JornadaServico`, `LotacaoServico`, `CargoServico`, `TipoContratacaoServico`.

Testes permanentes: `CatalogosVersaoCanonicaAuditoriaIndependenteHttpIT`.

Reprodução: evidencias/rodadas/novos_red_14 (quatro falhas funcionais de DTO entre cinco catálogos; falha do observador CPF é separada).

Fechamento final: VERIFICADO nos gates da fonte atual.

Causa no checkpoint confirmada por leitura e hashes dos DTOs e cauda save→mapeamento; a nova classe não foi executada retroativamente no checkpoint. O teste usa hoje; efeito sobre a rota retroativa decorre do guard de versão compartilhado, não de execução retroativa. E documental de CAT-007 permanece.

## CENTRAL-PRESIDENCIA-CONSULTA

**Problema:** A guarda do controlador da Central de Pendências omitia Presidência também nos GETs, impedindo a consulta nacional permitida pela norma apesar de o serviço já aplicar escopo e minimização.

**Correção aplicada:** Guarda específica nos dois GETs inclui Presidência; comandos mantêm a guarda restrita e exigência de Gestor Local no serviço. Teste permanente verifica perfil persistido, lista minimizada, resumo exato, dois POSTs403 e conservação de estado/autores/datas/versão/narrativa em transações posteriores.

Requisitos diretamente relacionados: PER-004.

Componentes: `PendenciaAdministrativaControlador`.

Testes permanentes: `PreauditoriaPendenciaNarrativaHttpIT`.

Reprodução: evidencias/rodadas/regressao_integral_04: PreauditoriaPendenciaNarrativaHttpIT, parâmetro PRESIDENTE com403; SELECT real de perfil em evidencias/infraestrutura/DIAGNOSTICO_PERFIL_CENTRAL_INTEGRAL04.json..

Fechamento final: VERIFICADO nos gates da fonte atual.

A omissão foi lida nas árvores original/checkpoint/candidata04. O403 reproduzido pertence à integral04; não se atribui execução retroativa à baseline. A falha foi restrição indevida de consulta, não vazamento de dados.

## BOOTSTRAP-CONTAS-GESTOES-INICIAIS

**Problema:** O seed opcional de gestores percorria todas as Gestões cadastradas e atribuía a mesma matrícula95+UF às siglas adicionais, causando colisão; o truncamento de códigos por prefixo também podia alcançar a conta inicial de outra Gestão.

**Correção aplicada:** Execução e completude do seed de usuários selecionam somente os códigos iniciais exatos GTRAT-UF/GEDIS-UF/GETRA-UF dentro de cada SE. Domínio de cadastro continua aberto; contas adicionais são administradas por seu fluxo próprio. Fallback95 removido; outros seeds não tiveram o escopo alterado. Teste cria duas siglas extras e um prefixo GTRAT adicional, exige contas apenas das três Gestões canônicas e reexecução/completude sem substituir credenciais ou estado.

Requisitos diretamente relacionados: Suporte técnico; sem atribuição automática de RF.

Componentes: `InicializadorDados`.

Testes permanentes: `ConclusaoBootstrapUsuariosSeguroMySqlIT`, `InicializadorDadosPrimeiroAcessoRegressaoTest`.

Reprodução: evidencias/rodadas/regressao_integral_04: colisão95000001; novos_red_15: reprodução independente em MySQL vazio, colisão95000019, um ERROR e nenhuma alteração de fontes durante a rodada..

Fechamento final: SUPORTE VERIFICADO na regressão atual e revisto independentemente, sem atribuição a RF.

Defeito de suporte no provisionamento opcional de desenvolvimento, desabilitado por padrão. Não foi inventada associação a RF. Causa lida na base original; a reprodução nova foi executada na árvore corrigida antes deste reparo. As precondições/resultados JPA do teste usam flush/clear dentro da transação externa, sem alegar commit independente.

## Falhas do processo de prova

Além dos reparos Java, foram corrigidos os scripts de distribuição para exigir quatro provas TCP distintas (dois roteiros × duas fases), com hashes de roteiro/utilitário, e para não marcar como concluída uma prova parcial. O teste unitário de fotografia quantitativa e o observador de CPF tiveram seus próprios diagnósticos preservados. O capturador MySQL também foi corrigido: HEX aplicado diretamente a DECIMAL confundia 1.01 e 1.02. O formato V2 usa CAST AS BINARY antes de HEX, com controle real permanente de decimais, doubles, binários, texto e temporalidade; o gate exige o hash atual do capturador e compara também regras FK/CHECK. A publicação dos digests passou a ser confrontada com a projeção exata das fotos privadas, com testes negativos que alteram conteúdo e refazem hashes. Esses roteiros não integram as classes do JAR e têm controles próprios. As alterações de testes Java foram congeladas em novo manifesto e receberam nova regressão integral. Esses ajustes não são contados automaticamente como novos defeitos funcionais dos 198 requisitos.
