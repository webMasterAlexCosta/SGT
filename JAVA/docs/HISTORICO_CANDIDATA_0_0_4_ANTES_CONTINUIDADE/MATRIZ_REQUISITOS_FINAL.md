# Matriz de requisitos — escopo operacional derivado

**Data documental: 01/11/2026 — America/Sao_Paulo.** Os registros históricos mantêm seus carimbos UTC de 02/11/2026.

## 1. Decisão e alcance

**Todos os 198 requisitos originais permanecem obrigatórios; nenhum foi retirado ou teve sua regra alterada.** A retirada autorizada abrange funcionalidades adicionais de Governança de Terceiros, processos exclusivos e avaliação/monitoramento de risco. A extração do cadastro Fornecedor altera APIs e componentes sem retirar as regras empresariais e históricas.

A classificação desta matriz é documental: **192 preservados e seis bloqueados por fonte histórica ausente**, todos com **validação atual PENDENTE**. Os seis bloqueados também permanecem no escopo. Não se atribui aprovação aos 198 requisitos nem aos 635 critérios pelo resultado de builds, pela localização de testes ou por classificações A de uma rodada anterior.

| Categoria dos requisitos originais | Quantidade | Significado |
|---|---:|---|
| Preservados | 192 | Regra mantida; validação atual pendente. |
| Bloqueados documentais | 6 | Regra mantida; faltam fontes autênticas para completude histórica. |
| Retirados por esta decisão | 0 | As funcionalidades removidas são adicionais, fora dos 198 IDs. |
| Regras originais alteradas | 0 | Extração de cadastro não modifica os requisitos empresariais. |
| Validação atual pendente | 198 | Não é contagem adicional aos 192+6. |

## 2. Proveniência e método

- Fonte de autoridade: `baseline/backend/docs/LEVANTAMENTO_REQUISITOS.md`, versão 8.0 de 06/09/2026, exatamente 198 linhas de requisitos canônicos. SHA-256: `9051a3f9d7e99a5022bef8ebe1c956f5c0efbb4e1d64a9e5ab7b51c32b920e98`.
- Fonte de rastreabilidade histórica: `baseline/backend/docs/MATRIZ_REQUISITOS_FINAL.json`, 198 IDs e 635 critérios recebidos. SHA-256: `9bb2a528633bb058ff3a739a438c80b51eaf104775bf70b64b6b3201007816db`.
- Os conjuntos de IDs são idênticos. A matriz recebida declara 192 A e seis E; essas letras não são promovidas a resultado atual.
- Referências Java foram localizadas por classe/método na baseline. A lista completa de critérios, testes, caminhos e bloqueios fica no JSON acompanhante. As rotas foram identificadas nas anotações dos controladores, não executadas por este mapeamento.
- A finalidade de cada domínio foi conferida no texto integral: os únicos requisitos GOV são GOV-003 e GOV-004. “Processo SEI” é fonte documental, distinto de ProcessoTerceiro. Não há requisito original de onboarding/offboarding exclusivo, score manual, monitoramento de risco, tarifa, folha monetária ou fatura.

## 3. Escopo adicional e interfaces

| Item adicional | Classificação | Fronteira de preservação |
|---|---|---|
| EXTRA-MODULO — Módulo e resumo Governança de Terceiros | RETIRADO_POR_DECISAO | Retirar página/prefixo/resumo exclusivos; preservar cadastro de Fornecedor por extração e painel operacional. |
| EXTRA-PROCESSOS — Processos exclusivos ONBOARDING/OFFBOARDING de ProcessoTerceiro | RETIRADO_POR_DECISAO | Não retirar cadastro integrado Pessoa/Vínculo/Alocação, admissão, desligamento nem movimentação operacional; não confundir com SEI. |
| EXTRA-RISCO-MANUAL — Avaliação manual de risco de fornecedor, quatro dimensões e score total | RETIRADO_POR_DECISAO | Retirar dimensões trabalhista, financeira, compliance e segurança e cálculo/nível associados; conservar fatos históricos físicos. |
| EXTRA-MONITORAMENTO-RISCO — Monitoramento contínuo de risco e vencimento de avaliação | RETIRADO_POR_DECISAO | Retirar job/config/estado de monitoramento exclusivos; não remover jobs operacionais nem alertas contratuais originais por homonímia. |
| EXTRA-FORNECEDOR-INTERFACE — Cadastro de Fornecedor e seleção em contrato rascunho | ALTERADO | Extração operacional planejada mantendo identidade/CNPJ/razão social/email/ativo, permissões, senha, locks e veto à troca livre em publicado; estados de risco excluídos do DTO. Não alterar regra empresarial/histórica. |
| EXTRA-FINANCEIRO-ANTERIOR — Tarifas, folhas monetárias e faturas já retiradas antes desta missão | RETIRADO_ANTERIORMENTE_PRESERVAR_AUSENCIA | Não restaurar rotas, código ou órfãos financeiros. Apuração de minutos/horas e relatórios operacionais permanecem. |

Todos esses itens têm validação atual pendente nesta matriz. Os caminhos planejados do cadastro são `GET/POST /api/fornecedores`, `PUT /api/fornecedores/{id}` e `PUT /api/contratos/{contratoId}/fornecedor/{fornecedorId}`; fonte da extração: `MATRIZ_BACKEND.md` e `MATRIZ_REACT.md`. Não manter alias no prefixo removido.

## 4. Bloqueios documentais preservados

| ID | Fonte autêntica ainda necessária conforme a matriz recebida |
|---|---|
| CON-023 | Tipo de Contratação que pertencia à Gestão de destino e sua validade na data de transferências legadas sem vínculo de Tipo; nomes e composição/contexto de origem e destino não fotografados. |
| CAT-004 | Nomes e configurações efetivos de Gestão, Lotação, Turno/Jornada, Cargo e Tipo nos períodos/fatos em que não foram fotografados. Inclui grade semanal e município quando sua fonte histórica inexiste; município opcional comprovadamente nulo é situação distinta. |
| CAT-007 | Origem e intervalo de disponibilidade anteriores ao primeiro evento comprovado. A flag atual ou o valor ativoAnterior não informa, por si só, desde quando o catálogo era válido. |
| COL-010 | Nome efetivamente usado no período e sua associação comprovada à identidade canônica da pessoa quando inexiste fotografia nominal do fato. |
| HIS-001 | Instrumentos e fontes que comprovem a configuração contratual completa nos recortes afetados: identificação, empresa, vigência, Tipo/política de matrícula, composição/efetivo, configurações e origem dos eventos. Campos ausentes na versão e ausência de toda a versão original são ambos tratados, mas não recuperados. |
| HIS-006 | Instrumento/publicação original e seu contexto completo para os registros que não conservaram a fotografia original. Nova coluna ou uma fotografia de ADI posterior não comprova o conteúdo perdido da publicação. |

Os bloqueios são do acervo documentado na origem, não uma contagem nova de registros do banco atual. Sem inventário material não se declara quantidade de linhas afetadas. Novas colunas, dados atuais, fixtures sintéticas ou uma suíte aprovada não substituem documentos autênticos. O JSON preserva integralmente as condições de fechamento recebidas.

## 5. Rastreabilidade a reconciliar

Foram recebidas 657 referências textuais de testes; 655 foram localizadas e normalizadas para 570 métodos canônicos distintos. Duas grafias da mesma referência antiga não correspondem a um método atual.

| Requisito/critério | Situação | Tratamento documental |
|---|---|
| EMP-002 | Referência a `GovernancaTerceirosContratoTest#contratoPublicadoNaoPermiteTrocarFornecedorForaDeAditamento`. | Migrar para `FornecedorContratoTest`, preservando o oráculo de recusa em contrato publicado. É regra operacional; não apagar sua cobertura junto com o módulo. Execução nova pendente. |
| TMP-004:003; COL-010:004; COL-012:002; REL-001:004; HIS-004:002 | Referência recebida `RevisaoHistoricaAusenciaNominalHttpIT#ausenciaNominalRecusaRetroatividadeEPreservaCapturaContemporanea` não existe na baseline. | Existe `ausenciaNominalPreservaDesconhecimentoRetroativoECapturaContemporanea(boolean)`. O comportamento documentado permite ato comprovado preservando nome histórico desconhecido; requer revisão do oráculo e prova atual, não troca mecânica de nome nem transferência do PASS antigo. |
| Referências ao CRUD nacional | Os XMLs recuperados registram 409 em `ContratosCrudHttpIT.leituraEEscritaRespeitamTodosOsPerfisEEscopos`. | Ver `DIAGNOSTICO_409_INICIAL.md`: IND-08 precedeu CRUD, mas o primeiro ID causal ainda exige reprodução controlada. Não exigir 200 com cadeia inválida nem presumir todos os critérios relacionados reprovados. |

## 6. Invariantes do escopo preservado

- **GOV-003 integral:** manter fatos publicados, versões, relações, fotografias, documentos e auditoria. Remover código ativo não autoriza purgar dados físicos legados.
- **Empresa/Fornecedor:** conservar identidade e CNPJ, associação contratual, publicação com campos obrigatórios, alteração empresarial temporal e recusa à troca livre em contrato publicado.
- **Operação de pessoas:** cadastro integrado, admissão/início efetivo, desligamento, movimentação e vínculo/alocação temporal continuam. O fim de ProcessoTerceiro não os substitui nem elimina.
- **Frequência:** preservar cobertura na data, recusa de presença futura, janela D como dia1 e D+7 exclusivo em São Paulo, retificação gerencial, autoria original e auditoria. O detalhe está em `REGRA_SETE_DIAS.md`.
- **Painel/relatórios/documentos:** continuam sujeitos a escopo, minimização, conhecimento temporal e sinalização de pendências. Retirar resumos exclusivos de risco não autoriza retirar painel operacional.
- **Ausência financeira já decidida:** manter os controles negativos da baseline; minutos/horas operacionais não são folhas monetárias.
- **MySQL:** somente Hibernate gerencia tabelas (`update`, `sql.init.mode=never`); integrações aguardam conta e ambiente autorizados. A coordenação informa adaptação dos guards da candidata para8.0.46; a baseline continua contendo o guard8.4 descrito no diagnóstico.

## 7. Famílias de requisitos e rotas identificadas

As rotas abaixo são exemplos reais da família funcional, não uma atribuição de todos os critérios a uma única operação. O JSON contém as demais anotações localizadas por controlador.

| Domínio | Requisitos | Endpoint de exemplo na baseline |
|---|---:|---|
| GOV — Governança e integridade documental | 2 | `DELETE /api/contratos/{id}` |
| TMP — Temporalidade e reconstrução histórica | 17 | `GET /api/contratos/{id}/posicao-temporal` |
| CON — Contratos e ciclo de vida | 25 | `GET /api/contratos` |
| EMP — Identidade empresarial e alterações contratuais | 3 | `POST /api/contratos/{contratoId}/aditamentos` |
| CAT — Catálogos temporais | 9 | `GET /api/catalogos-temporais/{tipo}/{id}/disponibilidade` |
| CMP — Composição contratual | 11 | `GET /api/contratos/{contratoId}/composicoes` |
| RED — Reduções de efetivo contratual | 8 | `POST /api/contratos/{contratoId}/aditamentos` |
| ADI — Aditamentos operacionais | 10 | `POST /api/contratos/{contratoId}/aditamentos` |
| REM — Remanejamentos operacionais | 7 | `POST /api/contratos/{contratoId}/remanejamentos` |
| DOC — Documentos e permissões próprias | 10 | `GET /api/contratos/{contratoId}/documentos` |
| COL — Pessoa e identidade | 12 | `GET /api/pessoas` |
| MAT — Matrícula do vínculo | 5 | `POST /api/vinculos-contratuais/{id}/alterar-matricula` |
| VIN — Vínculo contratual | 15 | `POST /api/vinculos-contratuais` |
| ALO — Alocação temporal | 8 | `POST /api/vinculos-contratuais/{id}/alocacoes` |
| DES — Desligamento operacional | 6 | `POST /api/vinculos-contratuais/{id}/desligar` |
| FRE — Frequência e retificação | 19 | `POST /api/frequencias` |
| REL — Relatórios operacionais | 6 | `GET /api/relatorios/frequencia-consolidada` |
| AUD — Auditoria e pendências | 7 | `GET /api/pendencias` |
| PER — Perfis e autorização | 12 | `POST /api/permissoes-excepcionais` |
| HIS — História canônica e fotografias | 6 | `GET /api/contratos/{id}/posicao-temporal` |

## 8. Matriz completa dos 198 requisitos

**Legenda:** PRESERVADO mantém o requisito; BLOQUEADO também o mantém, com a limitação documental da seção4. Todas as linhas têm validação atual pendente. O teste apresentado é uma referência ilustrativa localizada; o JSON conserva todos os testes/635 critérios e as pendências de referência. Nenhuma linha “PRESERVADO” significa teste aprovado nesta missão.

### GOV — Governança e integridade documental

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| GOV-003 | Nenhum fato negocial publicado é apagado fisicamente; pode ser cancelado, retificado, sucedido, encerrado ou substituído por nova versão, sempre auditável. | PRESERVADO | `ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | PENDENTE |
| GOV-004 | Rascunho nunca publicado e sem fato negocial pode ser descartado fisicamente de forma transacional, incluindo dependências exclusivas de preparação. | PRESERVADO | `ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | PENDENTE |

### TMP — Temporalidade e reconstrução histórica

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| TMP-001 | Contrato é a base operacional; Colaborador/Pessoa, Vínculo Contratual e Alocação são conceitos distintos. | PRESERVADO | `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | PENDENTE |
| TMP-002 | Colaborador representa a pessoa; Vínculo representa a relação temporal pessoa-contrato; Alocação representa Lotação + Turno + Cargo durante uma faixa do vínculo. | PRESERVADO | `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | PENDENTE |
| TMP-003 | Toda ocorrência relevante separa data efetiva, data/hora de registro, usuário responsável e motivo/justificativa quando aplicável. | PRESERVADO | `VinculoContratualCicloVidaEtapa06Test#eventosGuardamDataEfetivaSeparadaDoConhecimento` | PENDENTE |
| TMP-004 | O sistema deve reconstruir a posição histórica pela data efetiva sem usar a projeção atual como fonte canônica. | PRESERVADO | `ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento` | PENDENTE |
| TMP-005 | A data/hora de registro informa quando o sistema tomou conhecimento; não substitui a data real do fato. | PRESERVADO | `MotorTemporalContratoEtapa04Test#consultaConhecidoAteDistingueConhecimentoDoFato` | PENDENTE |
| TMP-006 | Reinicialização do sistema não cria, encerra ou altera fato negocial; apenas reconstrói projeções a partir da história válida. | PRESERVADO | `TemporalidadeInicializacaoHttpIT#reexecutarInicializadoresPreservaCustomizacaoEFatosNegociaisJaRegistrados` | PENDENTE |
| TMP-007 | O sistema trabalha com granularidade negocial diária para vínculo/alocação; horário serve à auditoria do registro, não para duas alocações no mesmo dia. | PRESERVADO | `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | PENDENTE |
| TMP-008 | Eventos retroativos são permitidos e devem preservar/reaplicar fatos posteriores válidos, sem mover/apagar pessoas silenciosamente. | PRESERVADO | `ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal` | PENDENTE |
| TMP-009 | Quando evento retroativo torna evento posterior incompatível, apresentar impacto e gerar pendência de tratamento; não apagar automaticamente. | PRESERVADO | `ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada` | PENDENTE |
| TMP-010 | Eventos conflitantes na mesma data efetiva exigem precedência negocial explícita; sem precedência, a conclusão é bloqueada. | PRESERVADO | `ContratosPrecedenciaBifurcadaHttpIT#mesmaDataNaoAdmiteDoisSucessoresConflitantesSemOrdemEntreEles` | PENDENTE |
| TMP-011 | Eventos da mesma data que não colidem podem coexistir. | PRESERVADO | `ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao` | PENDENTE |
| TMP-012 | Documento antigo recebido depois de documento posterior é inserido na posição temporal correta, com reaplicação dos fatos posteriores válidos e tratamento explícito de conflitos. | PRESERVADO | `ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal` | PENDENTE |
| TMP-013 | Início/aditamento/remanejamento/nova alocação usam a data efetiva como primeiro dia da nova situação; desligamento usa a data como último dia do vínculo. | PRESERVADO | `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | PENDENTE |
| TMP-014 | Um evento não pode produzir efeito fora da vigência reconstruída do contrato, exceto quando o próprio fato, de forma válida, altera a vigência para abranger a data do efeito. | PRESERVADO | `ContratosRegrasHttpIT#aditamentoForaDaVigenciaExigeExtensaoValidaNoProprioFato` | PENDENTE |
| TMP-015 | Prorrogação do mesmo contrato deve manter continuidade temporal; não é permitida lacuna interna de vigência. | PRESERVADO | `ContratosRegrasHttpIT#prorrogacaoNaoPodeCriarLacunaInternaNaVigencia` | PENDENTE |
| TMP-016 | Precedência negocial é representada por relação explícita entre eventos (`SUCEDE`, `RETIFICA`, `CANCELA`, `SUBSTITUI`, `MANTÉM/COEXISTE`) e, quando existente, pela sequência formal do instrumento; conflito sem precedência definida bloqueia conclusão. | PRESERVADO | `ContratosPrecedenciaBifurcadaHttpIT#precedenciaTransitivaPodePassarPorInstrumentoQueAlteraOutroCampo` | PENDENTE |
| TMP-017 | `FUTURO`, `VIGENTE` e `ENCERRADO` são derivados da linha temporal válida e da data corrente; rotinas técnicas apenas atualizam projeções/alertas/pendências. `CANCELADO` e `ANULADO` dependem de evento explícito. | PRESERVADO | `ContratoTemporalidadeEtapa02Test#futuroVigenteEEncerradoSaoDerivadosDaDataENaoGravadosComoEstadoNegocial` | PENDENTE |

### CON — Contratos e ciclo de vida

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| CON-001 | Número + ano identificam unicamente o contrato em todo o sistema. | PRESERVADO | `ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria` | PENDENTE |
| CON-002 | Número e ano podem ser corrigidos enquanto RASCUNHO, respeitando unicidade. | PRESERVADO | `ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | PENDENTE |
| CON-003 | Após publicação, número + ano são imutáveis no fluxo comum; correção excepcional é versionada/auditada e mantém o identificador antigo pesquisável. | PRESERVADO | `ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | PENDENTE |
| CON-004 | Rascunho descartado que nunca foi publicado pode liberar seu número/ano para reutilização. | PRESERVADO | `ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | PENDENTE |
| CON-005 | Contrato possui descrição, tipo de contratação, empresa/CNPJ/razão social/nome fantasia, vigência, situação temporal/negocial, habilitação administrativa, efetivo total, composição, aditamentos/remanejamentos, pendências e histórico. | PRESERVADO | `ContratosCompletudeHttpIT#consultaCompletaExpoeFotografiaEstadosComposicoesInstrumentosPendenciasEHistorico` | PENDENTE |
| CON-006 | Data de início e fim de vigência são obrigatórias para contrato publicado. | PRESERVADO | `ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria` | PENDENTE |
| CON-007 | Situação temporal/negocial `RASCUNHO`, `FUTURO`, `VIGENTE`, `ENCERRADO`, `CANCELADO` e `ANULADO` é separada da habilitação administrativa ativo/inativo e das pendências. | PRESERVADO | `ContratoTemporalidadeEtapa02Test#habilitacaoAdministrativaEhEixoIndependenteDaSituacaoTemporal` | PENDENTE |
| CON-008 | Publicação exige dados obrigatórios e composição consistente; rascunho pode estar incompleto. | PRESERVADO | `ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria` | PENDENTE |
| CON-009 | O contrato pertence a uma única Gestão Local por vez; Gestor Estadual acessa a estrutura sem tornar o contrato compartilhado entre Gestões. | PRESERVADO | `ContratosEscopoHttpIT#todasAsMutacoesContratuaisExigemGestorEContratoDaGestao` | PENDENTE |
| CON-010 | Pode existir inativação administrativa durante a vigência, distinta da situação temporal do contrato. A inativação possui data efetiva, data/hora de registro, motivo e autor; a reativação é registrada como novo evento auditável. | PRESERVADO | `CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo` | PENDENTE |
| CON-011 | Enquanto administrativamente inativo, eventos contratuais programados continuam produzindo seu efeito histórico, mas não liberam operação até reativação explícita. | PRESERVADO | `ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao` | PENDENTE |
| CON-012 | Prorrogação retroativa que torna o contrato temporalmente vigente não reativa automaticamente sua operação se houver bloqueio/pendência administrativa. | PRESERVADO | `ContratosRegrasHttpIT#prorrogacaoRetroativaNaoReativaContratoInativo` | PENDENTE |
| CON-013 | Mudança de Gestão Local não ocorre no fluxo comum; transferência excepcional exige data efetiva, motivo, auditoria e preservação da Gestão responsável em cada período. | PRESERVADO | `ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | PENDENTE |
| CON-014 | Contrato original pode ser cadastrado retroativamente, com fonte comprobatória, data efetiva real, registro/auditoria e prévia de impactos. | PRESERVADO | `ContratosTemporalLacunasHttpIT#publicacaoRetroativaExigePreviaMesmoComFonteEComposicaoCompletas` | PENDENTE |
| CON-015 | Existe estado negocial `ANULADO` para contrato publicado posteriormente declarado inválido. A anulação não exclui fisicamente o contrato e deve preservar motivo, fonte comprobatória, autor e data/hora. | PRESERVADO | `ContratosCriteriosComplementaresHttpIT#cicloDeVidaExigeDataDoAtoEFonteSemAlterarContratoOuVinculo` | PENDENTE |
| CON-016 | Encerramento antecipado/rescisão é distinto de fim natural de vigência e de inativação administrativa. Ele altera a data final efetiva do contrato e exige fonte comprobatória por anexo e/ou referência SEI. | PRESERVADO | `CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria` | PENDENTE |
| CON-017 | Contrato FUTURO pode receber aditamento/remanejamento programados, com documentação válida, coerência temporal e nenhum efeito antes do permitido. | PRESERVADO | `ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional` | PENDENTE |
| CON-018 | Anulação preserva fatos relacionados para auditoria, retira sua validade operacional e nunca realoca colaboradores automaticamente. | PRESERVADO | `ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados` | PENDENTE |
| CON-019 | Número/ano de contrato publicado e posteriormente anulado permanece reservado e pesquisável, sem reutilização. | PRESERVADO | `ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados` | PENDENTE |
| CON-020 | Anulação registrada incorretamente é corrigida por novo evento auditado; não há reativação silenciosa. | PRESERVADO | `CicloVidaContratosHttpIT#anulacaoECorrecaoPreservamAlcanceAnteriorSemReativacaoSilenciosa` | PENDENTE |
| CON-021 | Contrato FUTURO válido cancelado antes do início assume estado CANCELADO, distinto de ANULADO e ENCERRADO. | PRESERVADO | `ContratosCriteriosComplementaresHttpIT#cancelarContratoFuturoCancelaProgramacaoPreservaMotivoEReservaSemApagar` | PENDENTE |
| CON-022 | A situação temporal/negocial, a habilitação administrativa e as pendências são eixos independentes do ciclo de vida do contrato. | PRESERVADO | `CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo` | PENDENTE |
| CON-023 | Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado. | BLOQUEADO | `TransferenciaConcessaoObrigatoriaHttpIT#transferenciaSemConcessaoPreservaTodaOrigemEPassaSomenteAposAutorizacaoEspecifica` | PENDENTE |
| CON-024 | Anulação registra data do ato, data/hora de registro e data efetiva de alcance; o alcance segue o documento anulatório e não a data de digitação. | PRESERVADO | `ContratosRegrasHttpIT#anulacaoReservaIdentificadorEPreservaDatasAutorEFatosRelacionados` | PENDENTE |
| CON-025 | Contrato FUTURO publicado e posteriormente CANCELADO mantém número/ano reservado e pesquisável; somente rascunho nunca publicado pode liberar o identificador. | PRESERVADO | `CicloVidaContratosHttpIT#cancelarContratoFuturoMantemIdentificadorReservado` | PENDENTE |

### EMP — Identidade empresarial e alterações contratuais

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| EMP-001 | Contrato publicado deve possuir CNPJ, razão social e nome fantasia da empresa terceirizada. | PRESERVADO | `ContratosRegrasHttpIT#identificadorEhGlobalEVigenciaCompletaEhObrigatoria` | PENDENTE |
| EMP-002 | Mudança de razão social/nome fantasia vale para a posição contratual a partir da data do evento e não reescreve outros contratos/históricos. | PRESERVADO | `GovernancaTerceirosContratoTest#contratoPublicadoNaoPermiteTrocarFornecedorForaDeAditamento (migrar)` | PENDENTE |
| EMP-003 | Troca real de empresa/CNPJ é fato contratual temporal; erro material de CNPJ é correção excepcional auditada, sem ser tratado como troca real. | PRESERVADO | `ContratosRegrasHttpIT#empresaAlteradaTemporalmenteNaoReescreveOutroContratoNemFotografiaAnterior` | PENDENTE |

### CAT — Catálogos temporais

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| CAT-001 | Tipo de contratação é cadastro prévio administrado no escopo da Gestão Local, não texto livre. | PRESERVADO | `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao` | PENDENTE |
| CAT-002 | Cargo é cadastro próprio administrado pela Gestão Local. | PRESERVADO | `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao` | PENDENTE |
| CAT-003 | Lotação, Turno, Cargo e demais catálogos usados historicamente permanecem consultáveis mesmo inativos. | PRESERVADO | `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao` | PENDENTE |
| CAT-004 | Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual. | BLOQUEADO | `PessoasLacunasHttpIT#alocacoesHistoricasNaoSaoRenomeadasPelosCatalogosAtuais` | PENDENTE |
| CAT-005 | Não inativar Cargo/Lotação/Turno com programação futura sem antes tratar a programação. | PRESERVADO | `CatalogosTemporalHttpIT#programacaoFuturaImpedeInativacaoDoCatalogoAntesDeTrataLa` | PENDENTE |
| CAT-006 | Cadastro inativo pode ser reativado, mantendo identidade histórica e auditoria. | PRESERVADO | `CatalogosContratoHttpIT#cicloDeCatalogoPreservaIdentidadeHistoricaEIsolaGestao` | PENDENTE |
| CAT-007 | Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva. | BLOQUEADO | `CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura` | PENDENTE |
| CAT-008 | Cadastro atualmente inativo não pode ser usado em evento futuro sem reativação explícita. | PRESERVADO | `CatalogosTemporalHttpIT#catalogoInativoPermiteFatoNoPeriodoValidoMasBloqueiaProgramacaoFutura` | PENDENTE |
| CAT-009 | Causas de término do vínculo devem distinguir desligamento individual, fim natural, rescisão antecipada, anulação e outras causas cadastradas. | PRESERVADO | `CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria` | PENDENTE |

### CMP — Composição contratual

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| CMP-001 | Composição é combinação explícita Lotação + Turno + Cargo + efetivo previsto; não usar produto cartesiano automático. | PRESERVADO | `ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | PENDENTE |
| CMP-002 | Não pode haver combinação duplicada no mesmo recorte temporal. | PRESERVADO | `ContratosCrudHttpIT#rascunhoIncompletoPodeSerEditadoCompostoEDescartadoLiberandoIdentificador` | PENDENTE |
| CMP-003 | Soma do efetivo das composições deve ser igual ao efetivo total vigente do contrato. | PRESERVADO | `ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | PENDENTE |
| CMP-004 | Previsto zero significa combinação indisponível para nova alocação, distinta de permitir excedente. | PRESERVADO | `AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada` | PENDENTE |
| CMP-005 | Composição zerada que volta a ter efetivo retorna por nova versão temporal, preservando o período indisponível. | PRESERVADO | `ContratosRegrasHttpIT#composicaoZeradaBloqueiaNovaAlocacaoEApenasNovaVersaoARestaura` | PENDENTE |
| CMP-006 | Não zerar/inativar composição enquanto houver vínculos/programações futuras não tratadas; a conclusão deve ser impedida. | PRESERVADO | `ReplayCapacidadeDependenteHttpIT#aditamentoAnteriorNaoPodeInvalidarRemanejamentoQueSustentaVinculoFuturo` | PENDENTE |
| CMP-007 | Alteração que remove/reduz combinação ocupada deve identificar impactos, não mover pessoas automaticamente, exigir tratamento explícito e preservar história. | PRESERVADO | `ImpactoCapacidadeLimitesHttpIT#previaDeCapacidadeContaTodosOsOcupantesELimitaAmostraDePessoas` | PENDENTE |
| CMP-008 | Excedente de colaboradores acima do previsto é permitido com alerta; não é bloqueio rígido de vínculo. | PRESERVADO | `PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial` | PENDENTE |
| CMP-009 | Nova inclusão acima do previsto é autorizada pelo Gestor Local, com alerta forte, justificativa e auditoria. | PRESERVADO | `PessoasVinculosContratoHttpIT#mutacoesDePessoaVinculoEAlocacaoExigemGestorLocalAtivo` | PENDENTE |
| CMP-010 | Não há teto técnico absoluto de excedente; sistema alerta e mantém identificação do excedente. | PRESERVADO | `PessoasVinculosContratoHttpIT#inclusaoExcedenteExigeConsentimentoERegistraAutorizacaoSemTetoArtificial` | PENDENTE |
| CMP-011 | Enquanto existir excedente, ele deve permanecer monitorável, exibindo no mínimo composição, efetivo previsto, quantidade vinculada, excedente, desde quando existe e justificativas relacionadas; deixa de existir quando o quantitativo volta a caber no previsto ou quando alteração contratual válida absorve o excedente. | PRESERVADO | `ContratosRegrasHttpIT#excedenteEhMonitoradoComJustificativaEAumentoPosteriorOAbsorve` | PENDENTE |

### RED — Reduções de efetivo contratual

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| RED-001 | Reduções contratuais possuem limite máximo global de 25% e esse limite é cumulativo ao longo das reduções válidas. | PRESERVADO | `ContratosCriteriosComplementaresHttpIT#reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados` | PENDENTE |
| RED-002 | Limite de 25% é verificado sobre o total do contrato, não individualmente por composição. | PRESERVADO | `AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada` | PENDENTE |
| RED-003 | Uma composição pode reduzir mais de 25%, inclusive a zero, se a redução global do contrato respeitar o limite e os impactos forem tratados. | PRESERVADO | `AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada` | PENDENTE |
| RED-004 | Aumento contratual não possui limite de 25%. | PRESERVADO | `AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | PENDENTE |
| RED-005 | Redução superior ao limite é bloqueada no fluxo comum; exceção só se houver regra negocial/jurídica expressamente aprovada. | PRESERVADO | `AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | PENDENTE |
| RED-006 | O piso contratual é 75% do efetivo total originalmente publicado; aumentos posteriores não mudam a base, e apenas reduções válidas consomem o limite cumulativo. | PRESERVADO | `AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | PENDENTE |
| RED-007 | Quando o piso percentual resultar fracionário, o piso mínimo deve ser arredondado para cima para o próximo número inteiro. | PRESERVADO | `AditamentosRemanejamentosHttpIT#reducaoOriginal101Exige76MesmoComComposicaoZerada` | PENDENTE |
| RED-008 | Exceção acima de 25% exige fundamento jurídico/negocial documentado, fonte comprobatória, permissão técnica excepcional para Gestor Local, prévia de impacto, justificativa e auditoria reforçada. | PRESERVADO | `ContratosRegrasHttpIT#reducaoExcepcionalExigeFundamentoFonteConcessaoEConfirmacao` | PENDENTE |

### ADI — Aditamentos operacionais

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| ADI-001 | Contrato pode possuir vários aditamentos sem apagar registros anteriores. | PRESERVADO | `AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | PENDENTE |
| ADI-002 | Aditamento possui identificação, datas/documento, data efetiva, justificativa, responsável e fonte comprobatória. | PRESERVADO | `ContratosCriteriosComplementaresHttpIT#aditamentoExigeCadaCampoBasicoSemRegistrarInstrumento` | PENDENTE |
| ADI-003 | Aditamento quantitativo informa combinação afetada, quantidade anterior, variação e quantidade resultante. | PRESERVADO | `ContratosCriteriosComplementaresHttpIT#reducoesSucessivasRespeitamBaseOriginalEValoresDoAumentoPermanecemFotografados` | PENDENTE |
| ADI-004 | Aditamento pode aumentar ou reduzir, observadas as regras de redução e impacto. | PRESERVADO | `AditamentosRemanejamentosHttpIT#aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado` | PENDENTE |
| ADI-005 | Aditamento retroativo é permitido e sua data efetiva define quando passa a valer na reconstrução histórica. | PRESERVADO | `ContratosRegrasHttpIT#documentoRetroativoReaplicaFatosPosterioresSemDivergirTotal` | PENDENTE |
| ADI-006 | Aditamento futuro pode ser cancelado antes de produzir efeito; não há exclusão física, e cancelamento tem motivo/autor/data. | PRESERVADO | `AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes` | PENDENTE |
| ADI-007 | Retificação preserva original e possui data efetiva explícita, valor correto pretendido e variação/diferença aplicada. | PRESERVADO | `AditamentosRemanejamentosHttpIT#aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes` | PENDENTE |
| ADI-008 | Se retificação torna eventos posteriores incompatíveis, mostrar impacto e gerar pendência; posteriores válidos são preservados. | PRESERVADO | `ContratosRegrasHttpIT#retroativoIncompativelPreservaPosteriorEAbrePendenciaIdentificada` | PENDENTE |
| ADI-009 | Dois aditamentos conflitantes na mesma data precisam de sequência negocial explícita; não usar ordem de gravação. | PRESERVADO | `ContratosRegrasHttpIT#mesmaDataPermiteRecortesDistintosEExigePrecedenciaParaColisao` | PENDENTE |
| ADI-010 | Aditamento não altera diretamente publicação, inativação, reativação, encerramento, cancelamento ou anulação; esses estados possuem eventos/operações próprios. | PRESERVADO | `ContratosRegrasHttpIT#aditamentoNaoMudaEstadoNegocialOuHabilitacaoPorCampoEstranhoNoJson` | PENDENTE |

### REM — Remanejamentos operacionais

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| REM-001 | Remanejamento é fato próprio, identificado, datado, justificado e auditado. | PRESERVADO | `ContratosCriteriosEspecificosHttpIT#retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria` | PENDENTE |
| REM-002 | Remanejamento mantém o efetivo total: soma das variações efetivamente aplicadas deve ser zero. | PRESERVADO | `AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | PENDENTE |
| REM-003 | Remanejamento só redistribui entre combinações válidas já existentes na data; criação de nova combinação é aditamento. | PRESERVADO | `AditamentosRemanejamentosHttpIT#remanejamentoRecusaNovaCombinacaoOuSomaNaoZero` | PENDENTE |
| REM-004 | Remanejamento pode ser retificado/versionado, preservando original e cadeia completa. | PRESERVADO | `AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | PENDENTE |
| REM-005 | Anexo de remanejamento é opcional; pode haver referência SEI para rastreabilidade. | PRESERVADO | `AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | PENDENTE |
| REM-006 | Eventos futuros de remanejamento podem permanecer historicamente efetivos mesmo durante inativação administrativa, sem liberar operação. | PRESERVADO | `ContratosRegrasHttpIT#eventosProgramadosProduzemHistoriaDuranteInatividadeSemReativarOperacao` | PENDENTE |
| REM-007 | Remanejamento futuro pode ser cancelado antes do efeito, preservando original, cancelamento, motivo, usuário, data/hora e fonte comprobatória quando aplicável; o cancelado não produz efeito. | PRESERVADO | `AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | PENDENTE |

### DOC — Documentos e permissões próprias

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| DOC-001 | Contrato original publicado deve ter pelo menos uma fonte comprobatória: anexo, número/processo SEI ou ambos. | PRESERVADO | `ContratosCrudHttpIT#publicacaoExigeEmpresaVigenciaFonteESomaEImpedeEdicaoLivre` | PENDENTE |
| DOC-002 | Aditamento deve ter pelo menos uma fonte comprobatória: anexo e/ou SEI. | PRESERVADO | `AditamentosRemanejamentosHttpIT#aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade` | PENDENTE |
| DOC-003 | Remanejamento pode registrar SEI e mantém anexo opcional. | PRESERVADO | `AditamentosRemanejamentosHttpIT#remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo` | PENDENTE |
| DOC-004 | Um mesmo processo SEI pode referenciar vários contratos; SEI não é identificador único do contrato. | PRESERVADO | `ContratosRegrasHttpIT#seiCompartilhadoNaoViraIdentificadorUnicoDeContrato` | PENDENTE |
| DOC-005 | Número SEI incorreto em fato publicado é corrigido por nova versão/auditoria, preservando a referência anterior. | PRESERVADO | `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado` | PENDENTE |
| DOC-006 | Contrato publicado inicialmente apenas com SEI pode receber anexo posteriormente, sem substituir silenciosamente o SEI. | PRESERVADO | `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado` | PENDENTE |
| DOC-007 | Arquivo associado a fato publicado e enviado errado não é apagado fisicamente; é marcado inválido/substituído e permanece auditável. | PRESERVADO | `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado` | PENDENTE |
| DOC-008 | Em rascunho nunca publicado, anexos/dependências exclusivas podem ser removidos junto com o descarte, salvo valor auditável externo. | PRESERVADO | `DocumentosRascunhoCommitRollbackHttpIT#descarteFisicoAcompanhaConfirmacaoDaTransacaoSemApagarArquivoEmRollback` | PENDENTE |
| DOC-009 | Documentos anexados possuem classificação de acesso própria, distinguindo no mínimo documento negocial e documento pessoal/restrito. | PRESERVADO | `DocumentosContratuaisHttpIT#uploadValidaClassificacaoFormatoESigiloPorEscopo` | PENDENTE |
| DOC-010 | Documento Pessoal/Restrito possui acesso por necessidade e permissão específica conforme perfil; Presidente/Operacional não acessam por padrão, Administrador técnico somente em suporte excepcional autorizado, e todo acesso é auditado. | PRESERVADO | `DocumentosContratuaisHttpIT#documentoRestritoExigeFinalidadePermissaoEAuditaAcessoComRevogacao` | PENDENTE |

### COL — Pessoa e identidade

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| COL-001 | Nome e CPF são obrigatórios. | PRESERVADO | `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | PENDENTE |
| COL-002 | RG, CTPS, NIT/PIS, data de nascimento e e-mail são opcionais. | PRESERVADO | `PessoaServicoEtapa06Test#cadastroAceitaCamposOpcionaisOmitidos` | PENDENTE |
| COL-003 | Endereço é mantido e opcional. | PRESERVADO | `PessoasVinculosContratoHttpIT#cadastroIntegradoPreservaOpcionaisELimpezaExigeIntencaoExplicita` | PENDENTE |
| COL-004 | Dados bancários foram removidos do cadastro deste fluxo. | PRESERVADO | `PessoaVinculoEtapa06Test#cadastroPermanenteNaoPossuiDadosBancariosNemMatricula` | PENDENTE |
| COL-005 | CPF é válido, único globalmente, persistido sem pontuação e mascarado apenas na interface. | PRESERVADO | `PessoaVinculoEtapa06Test#cpfValidoEhNormalizadoSemPontuacao` | PENDENTE |
| COL-006 | CPF não é editável no fluxo comum. Correção excepcional de erro material comprovado é executada pelo Gestor Local e deve registrar motivo, evidência, CPF anterior, CPF novo, responsável, autorização, data/hora e impactos encontrados, além de validar o novo CPF. | PRESERVADO | `PessoasLacunasHttpIT#corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior` | PENDENTE |
| COL-007 | Se o CPF corrigido já pertence a outra pessoa, bloquear correção automática e tratar duplicidade/identidade excepcionalmente; nunca fundir silenciosamente. | PRESERVADO | `PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao` | PENDENTE |
| COL-009 | Nome não é editável no fluxo normal; erro material ou mudança legal pode ser corrigido excepcionalmente e auditado, sem criar nova pessoa. | PRESERVADO | `PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao` | PENDENTE |
| COL-010 | Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época. | BLOQUEADO | `PessoasVinculosContratoHttpIT#correcaoExcepcionalMantemPessoaEAuditoriaSemReescreverCriacao` | PENDENTE |
| COL-011 | Na recontratação, campos opcionais omitidos significam manter dados atuais; limpeza exige ação explícita. | PRESERVADO | `PessoaServicoEtapa06Test#omissaoNaAtualizacaoMantemValorELimpezaExigeIndicador` | PENDENTE |
| COL-012 | Dados pessoais atuais e fotografia histórica são separados: cadastro mostra valor atual, histórico negocial usa fotografia registrada na época. | PRESERVADO | `CatalogosPessoasConclusaoHttpIT#posicaoTemporalSeparaCadastroAtualDaFotografiaDoEvento` | PENDENTE |
| COL-013 | Pessoa é única por CPF; recontratação reutiliza a mesma pessoa e cria novo vínculo, não nova pessoa. | PRESERVADO | `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | PENDENTE |

### MAT — Matrícula do vínculo

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| MAT-001 | Matrícula pertence ao Vínculo Contratual, não ao cadastro permanente da Pessoa. | PRESERVADO | `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento` | PENDENTE |
| MAT-002 | Matrícula é opcional no vínculo e única dentro do contrato. | PRESERVADO | `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | PENDENTE |
| MAT-003 | Matrícula permanece reservada historicamente no contrato mesmo após desligamento, alteração ou programação cancelada. | PRESERVADO | `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento` | PENDENTE |
| MAT-004 | Nem a própria pessoa reutiliza matrícula antiga em recontratação; novo vínculo recebe nova matrícula. | PRESERVADO | `PessoasVinculosContratoHttpIT#matriculaPertenceAoVinculoEFicaReservadaAposAlteracaoEDesligamento` | PENDENTE |
| MAT-005 | Matrícula é opcional por padrão, mas Tipo de Contratação/Contrato pode torná-la obrigatória; quando obrigatória, vínculo não pode ser ativado/publicado sem matrícula válida. | PRESERVADO | `PessoaVinculoEtapa06Test#obrigatoriedadeDaMatriculaPodeSerHerdadaDoTipo` | PENDENTE |

### VIN — Vínculo contratual

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| VIN-001 | Pessoa + primeiro vínculo + alocação são cadastrados em operação integrada/transacional. | PRESERVADO | `PessoasAtomicidadeHttpIT#falhaDepoisDeCriarPessoaDesfazCadastroInteiroENovaTentativaConfirmaTudo` | PENDENTE |
| VIN-002 | Antiga data de admissão é substituída por início efetivo do vínculo. | PRESERVADO | `PessoasLacunasHttpIT#pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto` | PENDENTE |
| VIN-003 | Não pode haver sobreposição de vínculos da mesma pessoa, globalmente, inclusive entre contratos/Gestões diferentes. | PRESERVADO | `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | PENDENTE |
| VIN-004 | Troca direta entre contratos é permitida em dias consecutivos: A termina num dia e B começa no seguinte, sem dia de intervalo. | PRESERVADO | `PessoasVinculosContratoHttpIT#vinculosNaoSobrepoemGlobalmenteERecontratacaoReutilizaPessoaNoDiaSeguinte` | PENDENTE |
| VIN-005 | Vínculo futuro pode ser cadastrado como PROGRAMADO e não opera antes da data efetiva. | PRESERVADO | `MotorTemporalVinculoEtapa06Test#vinculoFuturoPermaneceProgramadoAteInicioEfetivo` | PENDENTE |
| VIN-006 | Programação futura pode ser cancelada antes de começar sem ser tratada como desligamento de vínculo efetivamente ocorrido. | PRESERVADO | `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia` | PENDENTE |
| VIN-007 | Programação futura cancelada/retificada preserva cadeia completa original → cancelamento/retificação → programação vigente. | PRESERVADO | `PessoasVinculosContratoHttpIT#tipoPodeExigirMatriculaEProgramacaoCanceladaPreservaReservaECadeia` | PENDENTE |
| VIN-008 | Ao fim natural da vigência, vínculos abertos ficam operacionalmente inativos a partir do dia seguinte e geram pendência administrativa. | PRESERVADO | `MotorTemporalVinculoEtapa06Test#fimDoContratoInativaOperacionalmenteVinculoAbertoNoDiaSeguinte` | PENDENTE |
| VIN-009 | Ao tratar a pendência de fim de vigência, a data final do vínculo é a data final do contrato, salvo desligamento real anterior. | PRESERVADO | `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior` | PENDENTE |
| VIN-010 | Fim natural do contrato usa motivo próprio de catálogo, como 'Fim da vigência do contrato', administrado pelo Gestor Local. | PRESERVADO | `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior` | PENDENTE |
| VIN-011 | Data de registro administrativo pode ser posterior, mas data efetiva do encerramento não ultrapassa o fim da vigência. | PRESERVADO | `PessoasVinculosContratoHttpIT#fimNaturalEncerraNaVigenciaComMotivoProprioSemReescreverDesligamentoAnterior` | PENDENTE |
| VIN-012 | Prorrogação retroativa posterior não reabre automaticamente vínculos já encerrados no fim antigo; exige impacto e eventual retificação explícita. | PRESERVADO | `ContratosCompletudeHttpIT#prorrogacaoTardiaNaoReabreVinculoEncerradoNoFimNaturalAnterior` | PENDENTE |
| VIN-013 | Inativação administrativa não encerra vínculos; reativação dentro da mesma vigência permite retomada dos vínculos ainda válidos sem novo vínculo. | PRESERVADO | `CicloVidaContratosHttpIT#inativacaoEReativacaoSaoEventosSeparadosSemNovoVinculo` | PENDENTE |
| VIN-014 | Data inicial do vínculo pode ser retificada por versão auditada, justificativa e prévia de impacto, revalidando sobreposição, alocação, frequência, matrícula, relatórios e eventos posteriores. | PRESERVADO | `PessoasVinculosContratoHttpIT#retificacaoDeInicioConfirmaPreviaExataEAjustaCoberturaSemRecontratar` | PENDENTE |
| VIN-015 | Encerramento antecipado/rescisão encerra vínculos abertos na mesma data efetiva, salvo desligamento anterior, usando causa própria. | PRESERVADO | `CicloVidaContratosHttpIT#encerramentoComPreviaTerminaVinculoNaDataEfetivaComCausaPropria` | PENDENTE |

### ALO — Alocação temporal

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| ALO-001 | Alocação é faixa temporal Lotação + Turno + Cargo dentro do vínculo. | PRESERVADO | `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | PENDENTE |
| ALO-002 | Mudança de Lotação/Turno/Cargo cria novo período de alocação; não é desligamento/recontratação. | PRESERVADO | `AlocacaoVinculoServicoEtapa07Test#mudancaFechaFaixaAnteriorNoDiaAnteriorESemDesligamento` | PENDENTE |
| ALO-003 | Alteração retroativa de alocação é permitida, preservando fatos posteriores válidos e apontando incompatibilidades. | PRESERVADO | `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | PENDENTE |
| ALO-004 | Existe no máximo uma alocação efetiva por colaborador por dia; segunda informação do mesmo dia retifica a anterior. | PRESERVADO | `AlocacaoVinculoServicoEtapa07Test#mesmaDataRetificaFaixaEmVezDeCriarDuasAlocacoesEfetivas` | PENDENTE |
| ALO-005 | Mudança de alocação e desligamento na mesma data só podem coexistir se a precedência tornar o último dia inequívoco; caso contrário, corrigir/retificar. | PRESERVADO | `PessoasVinculosContratoHttpIT#alocacaoNoUltimoDiaExigePrecedenciaExplicita` | PENDENTE |
| ALO-006 | Jornada que cruza meia-noite pertence à data de início e usa a alocação válida nessa data. | PRESERVADO | `FrequenciaEtapa09Test#jornadaNoturnaMantemDataNegocialDeInicioComoChave` | PENDENTE |
| ALO-007 | Todo dia de vínculo operacional efetivo deve possuir exatamente uma alocação Lotação + Turno + Cargo válida. | PRESERVADO | `PessoasComposicaoFuturaHttpIT#vinculoAbertoNaoPodeAtravessarZeramentoFuturoRegistradoAntesDoCadastro` | PENDENTE |
| ALO-008 | Inativação administrativa não encerra Alocação; na reativação, a mesma Alocação volta a operar se ainda válida, não sucedida e com composição válida. | PRESERVADO | `AlocacaoVinculoServicoEtapa07Test#reativacaoBloqueiaAlocacaoCujaComposicaoNaoEhMaisValida` | PENDENTE |

### DES — Desligamento operacional

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| DES-001 | Desligamento pertence ao Vínculo Contratual, não é campo fixo do Contrato. | PRESERVADO | `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | PENDENTE |
| DES-002 | Data de desligamento é o último dia em que o vínculo ainda existe; inatividade começa no dia seguinte. | PRESERVADO | `PessoaVinculoEtapa06Test#fimDoVinculoEhInclusivo` | PENDENTE |
| DES-003 | Desligamento pode ser informado retroativamente; data real do fato prevalece, registro posterior fica auditado. | PRESERVADO | `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | PENDENTE |
| DES-004 | Desligamento possui motivo de catálogo, responsável e data/hora de registro. | PRESERVADO | `PessoasLacunasHttpIT#motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo` | PENDENTE |
| DES-005 | Reprogramação/retificação de desligamento preserva original e verifica toda a linha temporal para evitar sobreposição. | PRESERVADO | `PessoasVinculosContratoHttpIT#desligamentoRetroativoIncluiUltimoDiaERetificacaoPreservaEventos` | PENDENTE |
| DES-006 | Desligamento futuro/programado pode ser cancelado/retificado, preservando toda a cadeia. | PRESERVADO | `PessoasVinculosContratoHttpIT#desligamentoFuturoPodeSerCanceladoSemApagarOrigem` | PENDENTE |

### FRE — Frequência e retificação

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| FRE-001 | Frequência resolve vínculo e alocação válidos na data negocial. | PRESERVADO | `FrequenciaContratosHttpIT#consultasDiariaMensalEIntervaloRetornamIdentidadeCanonicaEEstadosIndependentes` | PENDENTE |
| FRE-002 | Usuário Operacional pode editar por 7 dias corridos, contando a data da frequência como dia 1, em America/Sao_Paulo. | PRESERVADO | `FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia` | PENDENTE |
| FRE-003 | Exemplo: frequência de 01/01 editável até 07/01 23:59:59 e bloqueada para o Operacional a partir de 08/01 00:00. | PRESERVADO | `FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia` | PENDENTE |
| FRE-004 | Alterações do Operacional dentro da janela permanecem auditadas, com antes/depois, usuário e data/hora. | PRESERVADO | `FrequenciaContratosHttpIT#perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado` | PENDENTE |
| FRE-005 | Depois da janela, Gestor Local corrige por retificação versionada; não sobrescreve silenciosamente. | PRESERVADO | `FrequenciaContratosHttpIT#operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve` | PENDENTE |
| FRE-006 | Gestor Local pode corrigir antes do fim dos 7 dias, com identificação e justificativa. | PRESERVADO | `FrequenciaContratosHttpIT#perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado` | PENDENTE |
| FRE-007 | Gestor Local não possui prazo máximo rígido para retificação tardia; toda retificação tardia exige justificativa/auditoria. | PRESERVADO | `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | PENDENTE |
| FRE-008 | Retificação recalcula visão oficial atual de relatórios mensais, absenteísmo e indicadores; versões anteriores permanecem auditáveis. | PRESERVADO | `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | PENDENTE |
| FRE-009 | Alteração retroativa deve identificar frequências abertas e fechadas atingidas; fechada não é alterada silenciosamente, exige retificação autorizada. | PRESERVADO | `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa` | PENDENTE |
| FRE-010 | Frequências lançadas depois da data real de um desligamento retroativo permanecem visíveis/auditáveis e sinalizadas, mas não devem ser misturadas ao efetivo operacional válido/indicadores. | PRESERVADO | `PessoasVinculosContratoHttpIT#retificacaoQueRetiraCoberturaPreservaFrequenciaEGeraPendenciaAdministrativa` | PENDENTE |
| FRE-011 | Enquanto retificações pendem, relatórios podem ser consultados/exportados com aviso 'sujeito a revisão' e quantidade de pendências. | PRESERVADO | `RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao` | PENDENTE |
| FRE-012 | Quando retificação é concluída, a visão oficial atual usa a verdade corrigida e a emissão anterior permanece versionada. | PRESERVADO | `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | PENDENTE |
| FRE-013 | No 8º dia, a frequência passa automaticamente a FECHADA negocialmente; qualquer mudança posterior é retificação. | PRESERVADO | `FrequenciaEtapa09Test#janelaContaDataComoDiaUmEFechaNoInicioDoOitavoDia` | PENDENTE |
| FRE-014 | Alteração do Gestor Local gera versão formal e bloqueia nova edição daquele registro pelo Operacional. | PRESERVADO | `FrequenciaContratosHttpIT#perfilOperacionalEditaNaJanelaEGestorBloqueiaSomenteRegistroRetificado` | PENDENTE |
| FRE-015 | A frequência diária pertence ao escopo da alocação histórica do colaborador na data da frequência, e não à alocação atual do colaborador. | PRESERVADO | `RelatorioFrequenciaEscopoHistoricoIT#intervaloMistoNaoEntregaJulhoAGestaoDeOrigem` | PENDENTE |
| FRE-016 | Se retificação retroativa muda a alocação histórica, outro Operacional não assume silenciosamente o registro; eventual correção passa ao Gestor Local e preserva o autor original. | PRESERVADO | `FrequenciaEtapa09Test#contextoMudadoNaoReassociaFrequenciaAutomaticamente` | PENDENTE |
| FRE-017 | Qualquer fato retroativo que retire cobertura válida preserva a frequência para auditoria, mas a retira do cálculo operacional oficial. | PRESERVADO | `FrequenciaEtapa09Test#impactoRetroativoSemCoberturaPreservaRegistroEMarcaInvalido` | PENDENTE |
| FRE-018 | Permissão de edição combina autorização atual do usuário com o escopo histórico do registro. | PRESERVADO | `FrequenciaContratosHttpIT#operacionalNaoCriaRegistroFechadoEGestaoAlheiaNaoEscreve` | PENDENTE |
| FRE-019 | Frequência separa visibilidade histórica, validade para cálculo operacional e estado de auditoria. | PRESERVADO | `FrequenciaContratosHttpIT#consultasDiariaMensalEIntervaloRetornamIdentidadeCanonicaEEstadosIndependentes` | PENDENTE |

### REL — Relatórios operacionais

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| REL-001 | Relatório oficial atual reflete a última verdade válida/retificada; auditoria preserva versões anteriores. | PRESERVADO | `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | PENDENTE |
| REL-002 | Nova emissão após retificação retroativa deve ser identificada como versão retificada, com data/hora de geração. | PRESERVADO | `RelatorioAdministrativoServicoEtapa11Test#novaEmissaoAposInvalidacaoDaFonteEhRetificadaEPreservaAnterior` | PENDENTE |
| REL-003 | Auditoria deve permitir consultar a verdade efetiva atual e o que o sistema conhecia até determinada data de registro. | PRESERVADO | `RelatoriosAdministrativosPendenciasHttpIT#emissaoPreservaSnapshotConhecimentoAutoriaERetificacaoOficial` | PENDENTE |
| REL-004 | Relatório com pendências de retificação pode ser visualizado/exportado, mas não deve parecer definitivo; marcar sujeito a revisão. | PRESERVADO | `RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao` | PENDENTE |
| REL-005 | Uma emissão administrativa de relatório deve registrar período, data/hora de geração, usuário responsável, versão, indicação de pendências existentes e identificação de emissão original ou retificada. | PRESERVADO | `RelatorioAdministrativoServicoEtapa11Test#emissaoOriginalPreservaSnapshotHashAutorPeriodoEPendencias` | PENDENTE |
| REL-006 | Retificação que altera cálculo invalida a consolidação anterior para a visão vigente, recalcula resultados dependentes e preserva a versão histórica. | PRESERVADO | `InvalidacaoEmissaoTemporalTest#retificacaoAtingeAlocacaoAnteriorENovaEPreservaOutroRecorteEConteudo` | PENDENTE |

### AUD — Auditoria e pendências

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| AUD-001 | Auditoria registra autor, data/hora de ação, data efetiva, motivo/justificativa e antes/depois quando aplicável. | PRESERVADO | `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | PENDENTE |
| AUD-002 | Retificação preserva fato original, nova versão, motivo, autor, data de registro e data efetiva. | PRESERVADO | `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | PENDENTE |
| AUD-003 | Histórico deve distinguir quando o fato valeu de quando o sistema tomou conhecimento. | PRESERVADO | `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | PENDENTE |
| AUD-004 | Prévia de impacto deve ser revalidada na confirmação; se o impacto mudou, recalcular e exigir nova confirmação. | PRESERVADO | `PreviaCicloVidaLimitesHttpIT#previaLimitaDetalhesMasContaTudoEAssinaDependenciaForaDaAmostra` | PENDENTE |
| AUD-005 | Operações negociais devem impedir duplicidade acidental por clique repetido, reenvio ou falha de rede. | PRESERVADO | `OcorrenciaAutorizacaoTemporalHttpIT#reenvioComMesmaChaveNaoDuplicaFatoArquivoHistoricoNemFrequencia` | PENDENTE |
| AUD-006 | Central de pendências deve identificar origem, motivo, responsável, estado e escopo afetado; cada pendência bloqueia somente o necessário. | PRESERVADO | `RelatoriosAdministrativosPendenciasHttpIT#pendenciaMantemOrigemEscopoAvisoETemResolucaoSomenteDepoisDaRetificacao` | PENDENTE |
| AUD-007 | Retificação de retificação é permitida sem limite artificial; preservar cadeia completa e identificar a versão vigente em cada ponto temporal. | PRESERVADO | `AuditoriaTransversalHttpIT#retificacaoDeRetificacaoPreserva25VersoesAutoriaMotivoEDatas` | PENDENTE |

### PER — Perfis e autorização

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| PER-001 | Gestor Local é o principal perfil de escrita negocial do módulo no escopo de sua Gestão. | PRESERVADO | `PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais` | PENDENTE |
| PER-002 | Gestor Estadual consulta/audita as Gestões Locais de seu estado e não executa aditamento/remanejamento/encerramento/retificação negocial. | PRESERVADO | `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos` | PENDENTE |
| PER-003 | Gestor Nacional atua em consulta/auditoria nacional neste módulo, sem escrita negocial. | PRESERVADO | `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos` | PENDENTE |
| PER-004 | Presidente atua somente em consulta nacional neste módulo. | PRESERVADO | `ContratosCrudHttpIT#leituraEEscritaRespeitamTodosOsPerfisEEscopos` | PENDENTE |
| PER-005 | Administrador técnico não deve ser confundido com responsável negocial; intervenção excepcional exige permissão explícita e auditoria forte. | PRESERVADO | `PermissoesMinimizacaoHttpIT#concessaoInvalidaNaoAmpliaPrivilegios` | PENDENTE |
| PER-006 | Gestor Estadual visualiza apenas os dados pessoais necessários à auditoria. | PRESERVADO | `PermissoesMinimizacaoHttpIT#auditoresRecebemCpfMascaradoEOpcionalRestritoAusente` | PENDENTE |
| PER-007 | Correção excepcional de CPF é autorizada pelo Gestor Local. | PRESERVADO | `PermissoesMinimizacaoHttpIT#somenteGestorLocalResponsavelEscreveDadosPessoais` | PENDENTE |
| PER-008 | Correção excepcional de número/ano exige permissão específica concedida pela administração técnica; o ato negocial permanece com o Gestor Local autorizado. | PRESERVADO | `DocumentosPermissoesTransferenciaGestaoHttpIT#gestorDestinoComConcessaoExistenteCorrigeIdentificadorEPreservaAliasUsoEAuditoria` | PENDENTE |
| PER-009 | Gestor Estadual, Gestor Nacional e Presidente podem consultar documentos negociais dentro de seus respectivos escopos de consulta/auditoria. Dados pessoais desnecessários à auditoria permanecem protegidos. | PRESERVADO | `DocumentosContratuaisHttpIT#seiPodeSerRetificadoReceberAnexoESerInvalidadoComHistoricoPreservado` | PENDENTE |
| PER-010 | Permissão histórica de frequência usa autorização atual do usuário combinada com o escopo histórico do registro. | PRESERVADO | `OcorrenciaAutorizacaoTemporalHttpIT#consultaEListaUsamLotacaoETurnoEfetivosMesmoComProjecaoDefasada` | PENDENTE |
| PER-011 | Gestor Nacional e Presidente seguem minimização de dados pessoais: somente dados necessários à auditoria nacional/consulta institucional; conteúdo pessoal/restrito adicional depende de permissão específica. | PRESERVADO | `PermissoesMinimizacaoHttpIT#presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal` | PENDENTE |
| PER-012 | Transferência excepcional de Gestão Local exige autorização administrativa específica e execução por Gestor Local autorizado; Gestor Estadual não recebe escrita negocial. | PRESERVADO | `DocumentosPermissoesTransferenciaGestaoHttpIT#concessaoNegocialDepoisDaTransferenciaExigeResponsavelEfetivo` | PENDENTE |

### HIS — História canônica e fotografias

| ID | Regra original | Classificação | Teste identificado (exemplo) | Validação atual |
|---|---|---|---|---|
| HIS-001 | Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos. | BLOQUEADO | `ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento` | PENDENTE |
| HIS-002 | Consulta histórica do colaborador deve mostrar todos os vínculos e períodos de alocação, inclusive mudanças dentro do mês. | PRESERVADO | `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | PENDENTE |
| HIS-003 | Pesquisa por início/fim de vínculo não pode confundir transferência interna de alocação com desligamento/recontratação. | PRESERVADO | `PessoasVinculosContratoHttpIT#alocacaoRetroativaERetificacaoNoMesmoDiaPreservamMudancaPosterior` | PENDENTE |
| HIS-004 | Cadastros mestre atuais não reescrevem fotografia histórica do contrato/vínculo. | PRESERVADO | `ContratoCatalogoFotografiaHttpIT#renomearTipoAposAditamentoPreservaFotografiaHistoricaEInstrumento` | PENDENTE |
| HIS-005 | História é a fonte canônica; projeções atuais são conveniência técnica. | PRESERVADO | `VinculoConhecimentoSemOrigemHttpIT#primeiroEventoConhecidoProvaEstadoAnteriorSemHerdarLimiteModificadoDepois` | PENDENTE |
| HIS-006 | A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores. | BLOQUEADO | `ComposicaoFotografiaRascunhoHttpIT#editarCargoNoRascunhoPublicaFotografiaDoCargoEfetivamenteEscolhido` | PENDENTE |

## 9. Limite de aceite

A matriz é uma derivação de escopo e um mapa de validação. Não encerra os seis bloqueios documentais, não substitui reprodução do409 nem prova integração/banco/navegador. Para atribuir resultado atual, a coordenação deve vincular teste executado, XML/log, hash da candidata, ambiente real e oráculo do critério correspondente. Uma aprovação de código também não reconstrói automaticamente acervo histórico ausente.
