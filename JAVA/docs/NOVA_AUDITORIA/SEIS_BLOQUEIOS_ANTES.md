# Seis bloqueios — avaliação anterior à implementação

A tabela registra o enunciado recebido e as provas a produzir. As referências de teste são alvos de nova execução; não são aprovações. E é a restrição documental provisória, sem encobrir lacunas técnicas que serão corrigidas.

## CAT-004

Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual.

| Critério | Texto original | Evidência existente | Testes positivos e negativos propostos | Decisão antes |
|---|---|---|---|---|
| CAT-004:001 | Novos fatos de alocação/vínculo capturam rótulos e configurações da origem efetivamente escolhida. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaRestauraConfiguracaoSemUsarCatalogosAtuaisEReaplicaAditamento, MotivosProvaContextoHistoricoHttpIT | E documental provisório; técnica em revalidação |
| CAT-004:002 | Renomear Gestão/Lotação/Turno/Cargo/Tipo posteriormente não modifica as fotografias dos fatos anteriores. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaRestauraConfiguracaoSemUsarCatalogosAtuaisEReaplicaAditamento, MotivosProvaContextoHistoricoHttpIT | E documental provisório; técnica em revalidação |
| CAT-004:003 | Uma fotografia antiga nula/vazia continua desconhecida; nenhum nome/configuração é preenchido pelo mestre atual. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaRestauraConfiguracaoSemUsarCatalogosAtuaisEReaplicaAditamento, MotivosProvaContextoHistoricoHttpIT | E documental provisório; técnica em revalidação |
| CAT-004:004 | Documentos/relatórios/exportações consomem fotografias por fato; a prova transversal depende também dos lotes contratos e frequência. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaRestauraConfiguracaoSemUsarCatalogosAtuaisEReaplicaAditamento, MotivosProvaContextoHistoricoHttpIT | E documental provisório; técnica em revalidação |

**Prova necessária:** Cenário com fonte íntegra, cenário com ausência explicitada e incorporação posterior com cortes efetivo/conhecimento, autorização e imutabilidade verificadas no MySQL.

**Condição objetiva:** Comportamento técnico íntegro comprovado em fonte congelada e documento autêntico com valores/período para cada campo legado cuja recuperação integral seja exigida.

O JSON complementar conserva código e todos os métodos recebidos por critério.

## CAT-007

Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva.

| Critério | Texto original | Evidência existente | Testes positivos e negativos propostos | Decisão antes |
|---|---|---|---|---|
| CAT-007:001 | Fato retroativo aceita item hoje inativo quando a trilha comprova atividade na data efetiva. | Fonte/testes recebidos; nova execução pendente | SegurancaCacheUriCodificadaRedIT#transicaoDeCatalogoConfirmadaDeveInvalidarCacheDaProjecaoEDaDisponibilidade | E documental provisório; técnica em revalidação |
| CAT-007:002 | Corte de conhecimento exclui transições registradas depois do instante consultado. | Fonte/testes recebidos; nova execução pendente | SegurancaCacheUriCodificadaRedIT#transicaoDeCatalogoConfirmadaDeveInvalidarCacheDaProjecaoEDaDisponibilidade | E documental provisório; técnica em revalidação |
| CAT-007:003 | Antes da primeira origem comprovada, ou sem fonte de origem, disponibilidade é desconhecida e o uso negocial dependente é recusado. | Fonte/testes recebidos; nova execução pendente | SegurancaCacheUriCodificadaRedIT#transicaoDeCatalogoConfirmadaDeveInvalidarCacheDaProjecaoEDaDisponibilidade | E documental provisório; técnica em revalidação |
| CAT-007:004 | Primeira transição concorrente produz uma única origem e rejeita estado anterior obsoleto sem gravação parcial. | Fonte/testes recebidos; nova execução pendente | SegurancaCacheUriCodificadaRedIT#transicaoDeCatalogoConfirmadaDeveInvalidarCacheDaProjecaoEDaDisponibilidade | E documental provisório; técnica em revalidação |
| CAT-007:005 | Disponibilidade retroativa é documentada por comando canônico escopado com motivo, fonte, autorização e versão esperada, sem certificar período anterior à fonte. | Fonte/testes recebidos; nova execução pendente | SegurancaCacheUriCodificadaRedIT#transicaoDeCatalogoConfirmadaDeveInvalidarCacheDaProjecaoEDaDisponibilidade | E documental provisório; técnica em revalidação |
| CAT-007:006 | A inserção retroativa respeita a ordem efetiva e conserva reativação posterior na projeção atual. | Fonte/testes recebidos; nova execução pendente | SegurancaCacheUriCodificadaRedIT#transicaoDeCatalogoConfirmadaDeveInvalidarCacheDaProjecaoEDaDisponibilidade | E documental provisório; técnica em revalidação |
| CAT-007:007 | Confirmação de ato retroativo exige prévia vigente de versão, trilha e dependências; reenvio da mesma intenção não duplica fato ou versão. | Fonte/testes recebidos; nova execução pendente | SegurancaCacheUriCodificadaRedIT#transicaoDeCatalogoConfirmadaDeveInvalidarCacheDaProjecaoEDaDisponibilidade | E documental provisório; técnica em revalidação |
| CAT-007:008 | Ato incompatível com fatos posteriores ou com programação futura é recusado atomicamente, preservando as fontes existentes. | Fonte/testes recebidos; nova execução pendente | SegurancaCacheUriCodificadaRedIT#transicaoDeCatalogoConfirmadaDeveInvalidarCacheDaProjecaoEDaDisponibilidade | E documental provisório; técnica em revalidação |

**Prova necessária:** Cenário com fonte íntegra, cenário com ausência explicitada e incorporação posterior com cortes efetivo/conhecimento, autorização e imutabilidade verificadas no MySQL.

**Condição objetiva:** Comportamento técnico íntegro comprovado em fonte congelada e documento autêntico com valores/período para cada campo legado cuja recuperação integral seja exigida.

O JSON complementar conserva código e todos os métodos recebidos por critério.

## COL-010

Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época.

| Critério | Texto original | Evidência existente | Testes positivos e negativos propostos | Decisão antes |
|---|---|---|---|---|
| COL-010:001 | Cadastro atual e projeção operacional mostram o nome corrigido. | Fonte/testes recebidos; nova execução pendente | NovaAuditoriaFotografiasTardiasPessoasHttpIT, PessoasProvaNomeHistoricoHttpIT, TransversaisIdentidadeRetroativaRedIT, TransversaisRelatorioFonteFuturaRedIT | E documental provisório; técnica em revalidação |
| COL-010:002 | Novos fatos preservam o nome fotografado e a correção atual não muda eventos/documentos anteriores. | Fonte/testes recebidos; nova execução pendente | NovaAuditoriaFotografiasTardiasPessoasHttpIT, PessoasProvaNomeHistoricoHttpIT, TransversaisIdentidadeRetroativaRedIT, TransversaisRelatorioFonteFuturaRedIT | E documental provisório; técnica em revalidação |
| COL-010:003 | Consulta temporal separa nomeAtual de nomeFotografia e aplica os cortes efetivo e de conhecimento. | Fonte/testes recebidos; nova execução pendente | NovaAuditoriaFotografiasTardiasPessoasHttpIT, PessoasProvaNomeHistoricoHttpIT, TransversaisIdentidadeRetroativaRedIT, TransversaisRelatorioFonteFuturaRedIT | E documental provisório; técnica em revalidação |
| COL-010:004 | Registro antigo sem fotografia nominal permanece desconhecido; não se alega recuperação de dado ausente. | Fonte/testes recebidos; nova execução pendente | NovaAuditoriaFotografiasTardiasPessoasHttpIT, PessoasProvaNomeHistoricoHttpIT, TransversaisIdentidadeRetroativaRedIT, TransversaisRelatorioFonteFuturaRedIT | E documental provisório; técnica em revalidação |

**Prova necessária:** Cenário com fonte íntegra, cenário com ausência explicitada e incorporação posterior com cortes efetivo/conhecimento, autorização e imutabilidade verificadas no MySQL.

**Condição objetiva:** Comportamento técnico íntegro comprovado em fonte congelada e documento autêntico com valores/período para cada campo legado cuja recuperação integral seja exigida.

O JSON complementar conserva código e todos os métodos recebidos por critério.

## CON-023

Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado.

| Critério | Texto original | Evidência existente | Testes positivos e negativos propostos | Decisão antes |
|---|---|---|---|---|
| CON-023:001 | Sem autorização ou mapa completo bloqueia | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaDaTransferenciaRestauraTipoEMapaSomenteDepoisDoConhecimento | E documental provisório; técnica em revalidação |
| CON-023:002 | Tipo de contratação posterior deve ser válido na Gestão destino | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaDaTransferenciaRestauraTipoEMapaSomenteDepoisDoConhecimento | E documental provisório; técnica em revalidação |
| CON-023:003 | Composições e alocações mapeadas sem apagar anteriores | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaDaTransferenciaRestauraTipoEMapaSomenteDepoisDoConhecimento | E documental provisório; técnica em revalidação |
| CON-023:004 | Histórico de origem não muda após renomear cadastros | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaDaTransferenciaRestauraTipoEMapaSomenteDepoisDoConhecimento | E documental provisório; técnica em revalidação |
| CON-023:005 | A política de matrícula resultante da transferência deve ser compatível com vínculos vigentes e programados atingidos; respeita override contratual false comprovado e matrícula válida já informada. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaDaTransferenciaRestauraTipoEMapaSomenteDepoisDoConhecimento | E documental provisório; técnica em revalidação |
| CON-023:006 | Retificacao preserva transferencia anterior e reaplica ato posterior valido no corte efetivo e de conhecimento. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaDaTransferenciaRestauraTipoEMapaSomenteDepoisDoConhecimento | E documental provisório; técnica em revalidação |
| CON-023:007 | Cancelamento futuro e explicito, preserva original e posteriores validos; posterior incompativel implica rollback. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaDaTransferenciaRestauraTipoEMapaSomenteDepoisDoConhecimento | E documental provisório; técnica em revalidação |
| CON-023:008 | Transferencia usa vigencia reconstruida, incluindo prorrogacao e encurtamento programados. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaDaTransferenciaRestauraTipoEMapaSomenteDepoisDoConhecimento | E documental provisório; técnica em revalidação |
| CON-023:009 | Contexto posterior inclui todas composicoes e tentativas concorrentes nao publicam fatos conflitantes. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT#fonteTardiaDaTransferenciaRestauraTipoEMapaSomenteDepoisDoConhecimento | E documental provisório; técnica em revalidação |

**Prova necessária:** Cenário com fonte íntegra, cenário com ausência explicitada e incorporação posterior com cortes efetivo/conhecimento, autorização e imutabilidade verificadas no MySQL.

**Condição objetiva:** Comportamento técnico íntegro comprovado em fonte congelada e documento autêntico com valores/período para cada campo legado cuja recuperação integral seja exigida.

O JSON complementar conserva código e todos os métodos recebidos por critério.

## HIS-001

Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos.

| Critério | Texto original | Evidência existente | Testes positivos e negativos propostos | Decisão antes |
|---|---|---|---|---|
| HIS-001:001 | Posição contratual apresenta empresa, tipo, composição, efetivo e origem de eventos da data. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT | E documental provisório; técnica em revalidação |
| HIS-001:002 | Novas publicações/aditamentos fotografam política de matrícula e redução de jornada. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT | E documental provisório; técnica em revalidação |
| HIS-001:003 | Consulta legada sem configuração original explicita desconhecimento e não adota o mestre atual. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT | E documental provisório; técnica em revalidação |
| HIS-001:004 | Habilitacao original ausente permanece desconhecida; ato posterior define somente seu periodo comprovado. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT | E documental provisório; técnica em revalidação |

**Prova necessária:** Cenário com fonte íntegra, cenário com ausência explicitada e incorporação posterior com cortes efetivo/conhecimento, autorização e imutabilidade verificadas no MySQL.

**Condição objetiva:** Comportamento técnico íntegro comprovado em fonte congelada e documento autêntico com valores/período para cada campo legado cuja recuperação integral seja exigida.

O JSON complementar conserva código e todos os métodos recebidos por critério.

## HIS-006

A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores.

| Critério | Texto original | Evidência existente | Testes positivos e negativos propostos | Decisão antes |
|---|---|---|---|---|
| HIS-006:001 | Publicação fotografa o cargo efetivamente selecionado no rascunho e a base quantitativa original. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT | E documental provisório; técnica em revalidação |
| HIS-006:002 | Novas fotografias preservam todas as configurações operacionais contratuais e ADI conserva a original. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT | E documental provisório; técnica em revalidação |
| HIS-006:003 | Legado incompleto não recebe configuração atual retroativamente como se fosse a publicação original. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT | E documental provisório; técnica em revalidação |
| HIS-006:004 | Habilitacao original ausente permanece desconhecida; ato posterior define somente seu periodo comprovado. | Fonte/testes recebidos; nova execução pendente | ProvaHistoricaContratoHttpIT | E documental provisório; técnica em revalidação |

**Prova necessária:** Cenário com fonte íntegra, cenário com ausência explicitada e incorporação posterior com cortes efetivo/conhecimento, autorização e imutabilidade verificadas no MySQL.

**Condição objetiva:** Comportamento técnico íntegro comprovado em fonte congelada e documento autêntico com valores/período para cada campo legado cuja recuperação integral seja exigida.

O JSON complementar conserva código e todos os métodos recebidos por critério.
