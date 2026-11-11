# ALO-008:002 — alcance da contraprova de retomada

O caso negativo pede contrato inativo, vínculo REGISTRADO, alocação REGISTRADA contendo D e composição temporal ausente, incompatível ou com efetivo não positivo em D. A análise estática delimitada aos escritores canônicos abaixo não encontrou uma sequência legítima que prepare essa combinação partindo de uma base nova coerente. Isso não demonstra impossibilidade em legado, corrupção, escrita direta ou outra versão, nem concede PASS HTTP à negativa.

O [mestre §7.6](../../MATRIZES_COMPLETAS/PROMPT_MESTRE_SGT_CORRECAO_E_HOMOLOGACAO_8045_20261103.md) permite criar sequência válida pela API ou declarar precisamente por que é inalcançável, e veda fabricar inconsistência. A delimitação abaixo aplica essa alternativa à preparação negativa; não declara todo o critério inaplicável.

| Escritor examinado | Guarda que impede preparar a condição |
|---|---|
| CRUD de composição | Exige contrato RASCUNHO, sob lock. Não altera composição publicada. `ComposicaoContratualServico.java:168–198,292–299`. |
| Registrar/retificar aditamento ou remanejamento | Simula resultado, reaplica posteriores e recusa redução abaixo da reserva registrada, inclusive quando o contrato está administrativamente inativo. `ImpactoProgramacaoComposicaoServico.java:39–48,88–121`; `AditamentoContratualServico.java:838–852,1015–1020`; `RemanejamentoContratualServico.java:148–151,303–311`. |
| Cancelar instrumento que sustentava capacidade | Retira evento, reaplica posteriores e valida ocupação antes de persistir. `AditamentoContratualServico.java:351–363`; `RemanejamentoContratualServico.java:228–231`. |
| Criar vínculo ou criar/mudar/retificar alocação | Exige combinação positiva durante toda a faixa, incluindo fronteiras de eventos futuros. `VinculoContratualServico.java:310–334`; `AlocacaoVinculoServico.java:538–598,1407–1458`. |
| Restaurar, estender ou cancelar término/desligamento/alocação futura | A restauração revalida a faixa antes de persistir. Terminar uma faixa para liberar capacidade retira D dela; restaurar passa novamente pela guarda. `AlocacaoVinculoServico.java:618–634,685–716,909–936,1300–1316`; `VinculoContratualServico.java:401–431,538–562,579–592`. |
| Transferir Gestão ou retificar/cancelar transferência | Exige mapa e composição operacional; reaplica decisões e garante cobertura durante cada faixa. `TransferenciaGestaoContratoServico.java:191–201,318–320`; `AlocacaoVinculoServico.java:997–1080,1426–1458`. |
| Prova histórica complementar e catálogo | Prova referencia origens preservadas, sem nova quantidade ou troca de IDs; total não pode contradizer a composição comprovada. Inativar catálogo atual não apaga composição histórica nem significa efetivo zero. `ProvaHistoricaContratoRequisicao.java:16–18`; `ProvaHistoricaContratoServico.java:289–295,355–378`. |

Todos os caminhos citados ficam em `JAVA/src/main/java/com/example/siggtrat/{servico,dto}`. Na própria retomada, `ContratoServico.java:626–639` chama `AlocacaoVinculoServico.validarRetomadaOperacional` antes de registrar o evento. A seleção abrange apenas faixas REGISTRADAS e vínculos REGISTRADOS que contêm D; a resolução recusa composição ausente ou não positiva (`AlocacaoVinculoServico.java:198–208,1264–1297`). Inativação preserva faixas e reservas. Encerrar/cancelar/substituir pode liberar capacidade, mas a retomada não restaura essas faixas automaticamente.

Controles existentes a vincular aos recibos atuais, sem inferir aprovação apenas por sua presença:

- `AlocacaoRetomadaInvariantesApiHttpIT#reservaDurantePausaRecusaZeramentoAceitaReducaoCompativelERetomaMesmaFaixa`: preparo por API, recusa409 na retirada de capacidade durante pausa, redução compatível e retomada válida com mesmos IDs/intervalos/eventos do vínculo e faixa. É um ensaio de barreira e controle positivo, não a negativa original na retomada.
- `PessoasProjecaoHabilitacaoHttpIT#retomadaPreservaEncerradoCanceladoEFaixasVencidaESubstituida`: distinção entre faixas selecionáveis e encerradas/canceladas/vencidas/substituídas.
- `PessoasComposicaoFuturaHttpIT`, `CancelamentoCapacidadeContratualHttpIT`, `RetificacaoCapacidadeContratualHttpIT` e `ConclusaoTransferenciaAlocacaoHttpIT`: guardas de cobertura/capacidade, inclusive efeitos futuros e rollback.
- `AlocacaoVinculoServicoEtapa07Test#reativacaoBloqueiaAlocacaoCujaComposicaoNaoEhMaisValida` usa simulação unitária. `CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo` injeta inconsistência JPA declarada. São contraprovas da guarda; não demonstram preparo canônico do estado.

Limitações: não foi localizada contraprova de capacidade zero para cada operação de restauração; parte da exclusão decorre da guarda compartilhada lida. A análise documental de apoio não executou checks nem substitui revisão independente. O root deve vincular os resultados do candidato e apresentar este alcance ao revisor Desktop antes de decidir o aceite técnico. Até essa avaliação, o critério conserva aprovação pendente.
