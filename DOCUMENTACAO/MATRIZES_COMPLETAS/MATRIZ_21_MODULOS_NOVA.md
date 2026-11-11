# Matriz 21 módulos — candidata04

> **Reconciliação vigente de09/11/2026:** este documento preserva os textos/resultados do corte histórico. A [decisão humana vigente](../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md) substitui sete dias fixos por N configurável, padrão30, limite exclusivo D+N; preserva as outras permissões, IDs e namespaces. MySQL/recibos antigos não aprovam PostgreSQL ou os bytes atuais. Governança de Terceiros permanece removida; Sonar está fora desta missão. Consulte o [plano atual](../CODEX/CAMPANHA_20261109/PLANO.md) para gates/evidências atuais.

Corte documental C04: 2026-11-04T08:22:33.455770+00:00. JAR 6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d; selo ad30c1a3facd3235cbfc6c9e8e3b1820bf67b23f8d7133efbfaf3f01690afc79. Corte C03 preservado no histórico.

**Homologação parcial.** A exportação é documental e não executa produto. Os valores completos estão nas tabelas CSV e nos JSONs de rastreabilidade. Nenhum campo vazio autoriza aprovação. 33 critérios documentais, Chrome 0/24, ALO/PER, FE02 e erros do consumidor automático permanecem qualificados.

O adendo C04 acrescenta a continuação HTTP/SQL/reinício03 e o XLSX variado03, ambos revisados independentemente. A limitação antiga de séries constantes fica como histórico, superada apenas no novo recorte DTO/OOXML; UI e PDF React continuam não comprovados. O mapa de controllers é estático, com handlers e fontes, e não altera classificações funcionais. Veja [o adendo e seus limites](RASTREABILIDADE_ADENDO_FE02_C04/ADENDO_C04_FE02_E_CONTROLADORES.md).

## M01 — Empresas contratadas

### M01 — Empresas contratadas / Fornecedor — ROP-001, 002, 003, 049

**Objeto:** cadastro operacional legítimo de empresa terceirizada; identidade normalizada; associação contratual em rascunho; consulta histórica e inativação sem apagar fatos.

**Teste de ponta a ponta:** entrar como Gestor Local autorizado; cadastrar CNPJ sintético e razão social/e-mail válidos, conferir resposta e GET; atualizar e reconsultar; inativar e reativar preservando ID; criar contrato rascunho e associar o cadastro; publicar e tentar nova associação, esperando **409** sem alteração de fornecedor/versão/auditoria imprópria; conferir ligação original. Em contrato anteriormente publicado, editar apenas descrição de um **outro** rascunho e confirmar que `fornecedorId` não virou `null` por efeito colateral.

**Negativos/segurança:** CNPJ inválido/duplicado e e-mail inválido; perfil sem escopo; dois usuários tentando duplicar a empresa; espera de lock com revogação de autorização antes do commit; resposta de cadastro perdida após persistência e repetição da intenção (RI03). Conferir não duplicação, eventual 409, mensagem explícita e recuperação por seleção da empresa existente, reenviando **somente** associação legítima.

**Histórico:** alterar situação/razão atuais e comparar fotografia da publicação anterior. **Proibido** recriar campos de risco, score e onboarding financeiro. A URL operacional de Fornecedor deve permanecer ativa mesmo com URLs de governança removidas.

**Status:** PARCIAL. **Páginas/rota:** /empresas-contratadas. **Controllers:** Diretos: FornecedorControlador. Compartilhados: ContratoControlador, CorrecaoCnpjContratoControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M01. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-001, ROP-002, ROP-003, ROP-049. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/0).

## M02 — Estrutura organizacional

### M02 — Estrutura organizacional e Gestão Local — ROP-032, 037, 050

**Objeto:** Superintendência, Gestão Local, escopos territorial/administrativo e autoridade temporal efetiva.

**Casos:** criar/consultar gestores e recortes autorizados; trocar Gestão Local em data efetiva e provar acessos antes/na data/depois; consultar o mesmo contrato/ocorrência por usuários de Gestão antiga e nova, considerando **responsável no dia do fato**; revogar permissão entre geração de prévia e confirmação; testar usuário externo ao recorte (403, zero efeito). Transferência excepcional deve requerer ato, fundamento, documentação, versão e trilha; sem documentação autêntica, classificar o mecanismo técnico separadamente de CON-023 documental.

**Status:** PARCIAL. **Páginas/rota:** /estrutura-organizacional. **Controllers:** Diretos: GestaoLocalControlador, SuperintendenciaControlador. Compartilhados: ContratoControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M02. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-032, ROP-037, ROP-050. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/1).

## M03 — Catálogos operacionais

### M03 — Catálogos: tipo, cargo, lotação, turno, jornada, situação e motivo — ROP-005, 029, 030

**Casos:** registrar catálogos válidos por Gestão; normalizar códigos/nomes/municípios; impedir duplicidade e combinações não ofertadas; validar inativação, reativação e validade temporal com cadastro já usado; opção inativa não deve ser selecionável para fato novo, mas deve continuar visível na fotografia histórica; alternar filtros rapidamente e impedir retorno antigo de repor opções desatualizadas; testar 401/403/409 e erros de catálogo depois de sucesso parcial do carregamento principal.

**Prova:** snapshots com data de disponibilidade e conhecimento; zero/falso/null não podem perder semântica; CAT-004 e CAT-007 permanecem bloqueados quanto à autenticidade até apresentação de fontes reais.

**Status:** PARCIAL. **Páginas/rota:** /cargos; /tipos-contratacao; /lotacoes; /turnos; /status-empregados. **Controllers:** Diretos: CargoControlador, DisponibilidadeCatalogoControlador, JornadaControlador, LotacaoControlador, MotivoDesligamentoControlador, StatusEmpregadoControlador, TipoContratacaoControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M03. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-005, ROP-029, ROP-030. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/2).

## M04 — Ciclo de vida dos contratos

### M04 — Contrato: rascunho, publicação, vigência, anulação, inativação, encerramento — ROP-004, 003, 050

**Casos:** número+ano únicos; criar rascunho com empresa, tipo, instrumento, datas, fonte e composição; editar descrição sem perder fornecedor; tentar publicar com informação faltante e comprovar recusa sem publicação parcial; publicar com **prévia real** e conferir fotografia original; tentar edição livre da publicação (recusa); executar atos permitidos de encerramento/inativação/anulação com motivo, data efetiva e trilha; testar autorização e versão concorrente antes de confirmar. Alterações em vigência e empresa devem produzir atos formais próprios, não `PUT` silencioso.

**Temporalidade:** consultar antes/na/depois da vigência e após atos retroativos, distinguindo data de efeito de data de conhecimento. Dois operadores publicando simultaneamente não podem criar duas publicações nem perder versões. Testar reinício mantendo contratos/documentos/auditoria.

**Status:** PARCIAL. **Páginas/rota:** /contratos. **Controllers:** Diretos: ContratoControlador, CorrecaoCnpjContratoControlador, CorrecaoIdentificadorContratoControlador. Compartilhados: FornecedorControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M04. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-003, ROP-004, ROP-050. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/3).

## M05 — Composição contratual

### M05 — Composição contratual e quantitativos — ROP-004, 009, 050

**Regra cardinal:** composições explícitas por **Lotação + Turno + Cargo**; a soma das quantidades deve igualar o efetivo pactuado; não gerar produto cartesiano nem duplicidade vigente.

**Massa:** publicar contrato original com quantitativo 100 e distribuições reais; reduzir para 75 (**limite ordinário permitido**) e tentar 74 (**negado na via ordinária**) mesmo após aumento intermediário para 120; verificar que o piso permanece 75% de **100 originalmente publicados**, e não de 120. Uma redução excepcional abaixo do piso só pode ocorrer com fundamento, fonte e autorização específica de Gestor Local nas condições do contrato. Provar valores anteriores/novos, arredondamentos quando aplicáveis e ausência de atualização silenciosa dos trabalhadores.

**Excedente:** compor 2 postos, alocar 3 vínculos/presenças parciais e verificar visualização de **150%** onde o indicador representa excedente; texto/DTO devem conservar 150% mesmo quando a barra visual se limita a 100%. Excedente exige aviso, justificativa e auditoria, mas **não inventar teto proibitivo** se não existe na regra.

**Status:** PARCIAL. **Páginas/rota:** /contratos — aba Composição. **Controllers:** Diretos: ComposicaoContratualControlador. Compartilhados: AditamentoContratualControlador, VinculoContratualControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M05. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-004, ROP-009, ROP-050. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/4).

## M06 — Aditamentos

### M06 — Aditamentos e prorrogação comum — ROP-004, 050; defeito PROR01

**Fluxo completo:** contrato publicado com data final conhecida → abrir aditamento normal → preencher identificação, justificativa, data efetiva, data documental/assinatura, **`novaDataFim`** de prorrogação, fonte e componentes exigidos → obter prévia → alterar dado depois da prévia para testar invalidação → obter nova prévia → confirmar com token atual e usuário autorizado → GET do contrato e da versão histórica → reiniciar JAR e reconferir estado. O formulário deve mostrar controle acessível de nova data final e o payload deve preservar a diferença entre `dataEfetiva` e `novaDataFim`.

**Negativos:** data final anterior à permitida/incompatível com vigência, campos ausentes, datas em fuso civil distinto, confirmação repetida, dupla confirmação concorrente, autorização revogada entre prévia/confirmar, documento ausente quando obrigatório, versão 409 e conexão perdida após commit. O erro deve manter formulário preenchido, informar conflito e não enviar ato duplicado. **Não confundir prorrogação com mera troca de data de ciclo de vida.**

**Status:** PARCIAL. **Páginas/rota:** /contratos — aba Aditamentos. **Controllers:** Diretos: AditamentoContratualControlador. Compartilhados: ContratoControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M06. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-004, ROP-050. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/5).

## M07 — Remanejamentos

### M07 — Remanejamento de composições — ROP-009, 050

**Regra:** redistribuir quantidade entre composições **existentes**, com variações cuja soma seja zero, sem alterar o efetivo global. Testar remanejamento válido, origem insuficiente, destino inexistente, duplicação de faixa, data fora da vigência, sobreposição temporal, assinatura documental e autorização. Reconstruir a alocação anterior; não movimentar empregados automaticamente como efeito implícito do remanejamento; vigência e histórico devem refletir datas efetivas.

**Status:** PARCIAL. **Páginas/rota:** /contratos — aba Remanejamentos. **Controllers:** Diretos: RemanejamentoContratualControlador. Compartilhados: ComposicaoContratualControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M07. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-009, ROP-050. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/6).

## M08 — Pessoas e empregados

### M08 — Pessoas e Empregados — ROP-006, 007, 025, 029

**Regra:** Pessoa é identidade permanente; Empregado é projeção operacional; Vínculo é relação temporal separada. Testar CPF sintético válido, unicidade, nome atual, alteração histórica, homônimos, nome social quando previsto, dados inconsistentes e validações. Selecionar o vínculo por identificação inequívoca em frequência e ocorrências: IDs, matrícula e contrato, não somente nome. Alterar nome atual não altera a fotografia jurídica do fato pretérito. Testar permissões para consulta de dados pessoais e minimização do DTO. COL-010 documental exige fonte autorizada de nomes anteriores.

**Status:** PARCIAL. **Páginas/rota:** /pessoas; /empregados redireciona. **Controllers:** Diretos: EmpregadoControlador, PessoaControlador. Compartilhados: ProvaNomePessoaHistoricoControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M08. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-006, ROP-007, ROP-025, ROP-029. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/7).

## M09 — Vínculos contratuais

### M09 — Vínculos contratuais — ROP-006, 007, 008, 025–028, 031, 048

**Fluxos:** admitir Pessoa em contrato válido com matrícula e data; programar admissão futura e impedir atos prematuros; desligar, cancelar desligamento e recontratar preservando histórico; tentar matrícula duplicada e associação a contrato inativo; duas inclusões simultâneas de uma Pessoa sob regras de reserva global; consulta de vínculo fora da validade; recontratação deve criar vínculo distinto sem duplicar Pessoa. Testar fronteiras inclusivas/exclusivas definidas no contrato, autorização, cadastros inativos e idempotência de POST/PUT. Não perder ocorrências/atestado/frequência do vínculo desligado retroativamente.

**Status:** PARCIAL. **Páginas/rota:** /pessoas — fluxos de vínculo. **Controllers:** Diretos: VinculoContratualControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M09. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-006, ROP-007, ROP-008, ROP-025, ROP-026, ROP-027, ROP-028, ROP-031, ROP-048. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/8).

## M10 — Alocações e movimentações

### M10 — Alocações e movimentações temporais — ROP-009, 027–032

**Fluxos:** mover trabalhador entre Lotação, Turno e Cargo em data de efeito, fechar faixa anterior sem lacuna/sobreposição indevida; garantir composição existente e pertencente ao contrato; verificar pessoa/contrato/cargo corretos para cada data; cancelar movimento e reconstruir estado sem adulterar histórico. Testar turno noturno, mudanças do dia seguinte e permissões por Gestão vigentes no dia. Operação com alocação futura, cancelada ou estranha ao vínculo deve ser recusada. Excedente deve ser auditado, sem fabricação de proibição genérica.

**Status:** PARCIAL. **Páginas/rota:** /pessoas — fluxos de alocação. **Controllers:** Diretos: VinculoContratualControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M10. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-009, ROP-027, ROP-028, ROP-029, ROP-030, ROP-031, ROP-032. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/9).

## M11 — Calendário e jornadas

### M11 — Calendário administrativo e jornada — ROP-005, 017, 018, 030

**Casos:** validar jornadas semanais, feriados, eventos, escalas, vigência e hora local; fevereiro bissexto 2024 com 29/02; ano comum de comparação 2023; turno noturno que começa em D e termina em D+1; determinar pertencimento do lançamento ao **início da jornada**. Mudança de turno/cargo/município no dia seguinte não pode reclassificar os fatos de D. Datas sem fonte de escala não recebem presença inventada; calendário gerado para relatório deve coincidir com fonte e período.

**Status:** PARCIAL. **Páginas/rota:** /calendario-folgas; /turnos. **Controllers:** Diretos: CalendarioEventoControlador, JornadaControlador, RegraAdicionalControlador. Compartilhados: RelatorioControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M11. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-005, ROP-017, ROP-018, ROP-030. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/10).

## M12 — Frequência

### M12 — Frequência: presença integral, parcial, ausência e limites — ROP-010–022, 025–028, 043–045

**Positivos:** batidas explícitas e presença sem batidas quando comprovada por jornada válida; presenças parciais e integrais; cálculo de minutos, jornada, apuração noturna, faltas/absenteísmo, totalização por trabalhador/contrato/Gestão e auditoria. Identificar inequivocamente Pessoa + Vínculo + Alocação + data do início da jornada. Separar **FALTA**, ausência de lançamento, ausência justificada e ausência de prova.

**Bordas:** data futura sempre recusada **pela API inclusive Gestor Local**; registro de D permitido apenas até instante estritamente anterior a `D+7 00:00 America/Sao_Paulo`; exatamente às `D+7 00:00` recusar e após também; em D+8, observar fechamento conforme fluxo. Testar relógio controlado e processo real em ensaios separados. No turno noturno, mudança de alocação na madrugada do dia seguinte não altera o fato originado em D.

**Correção gerencial:** Gestor Local corrige com fundamento, versão e auditoria; durante janela, o bloqueio operacional recai **sobre o registro corrigido**, não todos os outros; depois da janela somente atos excepcionais autorizados. Testar idempotência com/sem chave, requisições simultâneas, contrato inativado entre validação e commit, executor revogado durante lock e rollback de lote com item futuro/inválido. Zero escrita parcial sem semântica contratual prevista.

**Status:** PARCIAL. **Páginas/rota:** /frequencia. **Controllers:** Diretos: FrequenciaControlador. Compartilhados: EmpregadoControlador, VinculoContratualControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M12. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-010, ROP-011, ROP-012, ROP-013, ROP-014, ROP-015, ROP-016, ROP-017, ROP-018, ROP-019, ROP-020, ROP-021, ROP-022, ROP-025, ROP-026, ROP-027, ROP-028, ROP-043, ROP-044, ROP-045. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/11).

## M13 — Ajustes e fechamento de frequência

### M13 — Ajustes, exceções e fechamento assíncrono de frequência — ROP-019–021, 034–036, 043–045, 054

**Fluxo:** preparar massa de **200 vínculos elegíveis** e outra configuração com chunk produtivo **500**. Criar job único, acompanhar estados do mesmo ID, progresso, quantidade concluída, erros, latência, cancelamento e reprocessamento. Reproduzir a falha anterior (não terminou em 120s com chunk 1) e determinar se a causa é desempenho, espera do driver, elegibilidade repetida, lock, scheduler ou problema real de execução; não apenas aumentar timeout até ficar verde.

**Concorrência/recuperação:** cancelar após prefixo **N>0** realmente persistido, comparar registros antes/depois, reprocessar sem duplicar efeitos, introduzir falha controlada **da JVM própria** depois de prefixo confirmado, reiniciar exatamente os mesmos bytes/configuração e observar progressão automática do **mesmo job/tentativa** em UI, API e MySQL. Testar idempotência da segunda inicialização e evitar duplo executor. Demonstrar **200/200 e estado terminal**, ou classificar como falha/não execução e manter evidências parciais honestas. A prova H02 antiga de 47/200 (48 no corte) não encerra este item.

**Regras complementares:** D+8, estados de vínculo/contrato, documentos, cancelamento de operação, retorno de erro/retry, auditoria, progresso de jobs e reabertura após reinício. Dados confirmados não podem ser apagados por limpeza de teste.

**Status:** PARCIAL. **Páginas/rota:** /frequencia-ajustes. **Controllers:** Diretos: FechamentoFrequenciaControlador, FrequenciaAdicionalControlador, FrequenciaExcecaoControlador. Compartilhados: FrequenciaControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M13. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-019, ROP-020, ROP-021, ROP-034, ROP-035, ROP-036, ROP-043, ROP-044, ROP-045, ROP-054. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem. Dois1205 no consumidor automático sob bloco500; não confundir20oráculos manuais com zero erro global..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/12).

## M14 — Ocorrências, justificativas e atestados

### M14 — Ocorrências, atestados, justificativas e anexos — ROP-022–024, 031, 046

**Casos:** criar ocorrência em data com vínculo/alocação/Gestão válidos; anexar arquivo sintético, baixar e comparar SHA; atestado de vários dias com intervalo correto, inclusive atravessando gestão; ocorrência sobre presença já existente deve preservar presença/versões e aplicar somente efeito permitido; tentativa de fato no futuro ou por usuário sem escopo deve ser recusada; validar extensão/MIME/tamanho e acesso a anexos restritos. Conflito concorrente deve ser idempotente e fazer rollback **de fato e arquivo**, não deixar órfãos em `.staging`/repositório final. Revalidar autoridade temporal do usuário no dia do fato (DIF-02 histórico), não apenas Gestão/lotação atuais.

**Status:** PARCIAL. **Páginas/rota:** /ocorrencias. **Controllers:** Diretos: OcorrenciaControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M14. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-022, ROP-023, ROP-024, ROP-031, ROP-046. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/13).

## M15 — Pendências administrativas

### M15 — Pendências administrativas e regularização — ROP-037, 054; AUD-006

**Casos:** abrir pendência por fato/recorte, listar somente escopo permitido, regularizar com fonte e histórico, provar bloqueio apenas no conjunto alcançado pela pendência; efetuar **escrita positiva fora do recorte atingido** com autorização válida (`AUD-006:002`), e testar replay conflitante após mudança temporal de Gestão (`AUD-006:004`). Cancelar pendência não deve apagar evidência pretérita nem liberar autorizações alheias. Não declarar testada a escrita positiva apenas porque GET funcionou.

**Status:** PARCIAL. **Páginas/rota:** /pendencias. **Controllers:** Diretos: PendenciaAdministrativaControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M15. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-006, ROP-037, ROP-054. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/14).

## M16 — Histórico e provas temporais

### M16 — Histórico, provas temporais, nomes e catálogos — ROP-029–033, 038–040

**Regra:** separar **quando o fato produziu efeito** de **quando a administração tomou conhecimento**. Testar fotografia de publicação original, alteração posterior de CNPJ/empresa, nome, cargo/lotação/turno, validade de cadastro, transferências de Gestão e documentos de publicação; consultas por data de efeito e corte de conhecimento. `null`, valor não comprovado e ausência factual são coisas distintas; zero e `false` são valores legítimos. Uma fonte tardia não pode ser retroprojetada como conhecida antes de seu registro.

**Provas:** prévia/confirmar, idempotência, permissão atual versus histórico, reconstrução anterior à retificação, ordem dos atos e cadeia documental. Não retornar 200 fabricado para prova inconsistente. Testar com e sem fonte autêntica; mecanismo técnico pode passar enquanto HIS-001/HIS-006, CAT-004/CAT-007, COL-010/CON-023 continuam documentalmente bloqueados.

**Status:** PARCIAL. **Páginas/rota:** /historico. **Controllers:** Diretos: CorrecaoCnpjContratoControlador, CorrecaoIdentificadorContratoControlador, DisponibilidadeCatalogoControlador, HistoricoEmpregadoControlador, ProvaHistoricaContratoControlador, ProvaMotivoHistoricoControlador, ProvaNomePessoaHistoricoControlador. Compartilhados: CalendarioEventoControlador, ComposicaoContratualControlador, ContratoControlador, DocumentoComprobatorioControlador, EmpregadoControlador, FrequenciaAdicionalControlador, FrequenciaExcecaoControlador, PessoaControlador, RelatorioAdministrativoControlador, VinculoContratualControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M16. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-029, ROP-030, ROP-031, ROP-032, ROP-033, ROP-038, ROP-039, ROP-040. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/15).

## M17 — Documentos e SEI

### M17 — Documentos, SEI, upload/download, versões — ROP-038, 039, 047; defeito DOC-DTO

**Fluxo:** anexar arquivo sintético com checksum, classificação de acesso e referência SEI quando contratualmente exigida; versionar/retificar; confirmar/documentar; reler via GET e replay; verificar conteúdo byte a byte. Em prova de primeira resposta, `version` técnica deve obedecer contrato explícito com GET/replay/JPA depois de commit (H02 retornava 0 e depois 1); não confundir `version` de trava técnica com versão negocial do documento.

**Negativos:** revogar permissão atual e tentar link de download antigo; sessão expirada durante download; arquivos inválidos; uploads interrompidos; dois retificadores simultâneos; replay 409; transação falha antes/depois de persistir anexo e limpeza de órfãos **somente na massa isolada**, sem tocar arquivos de usuários. Prever política de retenção e não apagar documentos históricos. FE-01: atravessar o ramo de `await import(...)` do PDF inline em carregamento frio e mudança de sessão, provando descartes antes/depois do import; não tomar exportação assíncrona como substituta.

**Status:** PARCIAL. **Páginas/rota:** Fluxos de documentos em /contratos e /historico. **Controllers:** Diretos: DocumentoComprobatorioControlador. Compartilhados: OcorrenciaControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M17. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-038, ROP-039, ROP-047. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/16).

## M18 — Painel gerencial e indicadores

### M18 — Painel, indicadores, efetivo e análises — ROP-057, 061, 064

**Casos:** efetivo contratado versus alocado, indicador 150%, taxa de presença/absenteísmo com fonte/denominador corretos, status nulos/zeros, filtros por período, Gestão e contrato; alternar rapidamente A→B, reter/liberar resposta de A e provar que gráfico/título/tabela/exportação continuam B; invalidar resultado quando retificação altera fonte. Filtros de indicadores historicamente corrigidos não podem regredir. Testar erro em uma subconsulta com outras concluídas e impedir dados antigos sob cabeçalho novo.

**Status:** PARCIAL. **Páginas/rota:** /painel. **Controllers:** Diretos: PainelCalculoControlador, PainelControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M18. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-057, ROP-061, ROP-064. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** Jornadas de UI permanecem bloqueadas; os critérios documentais e os predicados particulares continuam qualificados nas matrizes 635/64. A continuação03 e o writer03 comprovaram valores diários discriminantes, DTO/SQL/reinício e OOXML real. Pendem gráficos, interação e download no Chrome, PDF React da massa discriminante e demais predicados integrados do módulo. Não se atribui PASS às tentativas01/02 nem ao módulo por contagem de verificações..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/17).

## M19 — Relatórios e exportações

### M19 — Relatórios e exportações PDF/XLSX síncronas/assíncronas — ROP-040–042, 057, 061

**Casos:** download real em Chrome sob diferentes perfis/períodos/contratos; PDF de **41 registros em 2 páginas** como controle de paginação de relatório; outro caso com uma pessoa; tabela UI de 5 registros por página e sexto acessível; exportação de período com 25+1 quando API real utilizar essa paginação. Verificar magic bytes, headers, content-type, tamanho, autorizações, contagens, conteúdo textual, colunas, valores, paginação, identidade de arquivo e hash. PDF/XLSX gerado deve ter período e identidade da **resposta atual**, nunca arquivo de filtro anterior.

**FE-02:** calendário bissexto 2024 × 2023: 15 datas de fevereiro de 2024 contra 14 datas correspondentes de 2023 no recorte anterior; 29/02 **sem inventar valor comparativo**. Além de shape, usar valores **diferentes por dia** no DTO real e validar cada posição/índice na planilha e PDF; massa com todos zeros NÃO prova ausência de deslocamento. Importação inline FE-01 permanece caso separado. Inspecionar legibilidade em zoom padrão, quebra de página, repetição de cabeçalhos, truncamento e acessibilidade PDF/UA no alcance disponível; não declarar WCAG/PDF-UA integral sem auditoria.

**Status:** PARCIAL. **Páginas/rota:** /relatorios; /relatorio-mensal; /relatorio-absenteismo; /relatorios-administrativos. **Controllers:** Diretos: RelatorioAdministrativoControlador, RelatorioControlador. Compartilhados: PainelControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M19. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-040, ROP-041, ROP-042, ROP-057, ROP-061. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** Jornadas de UI permanecem bloqueadas; os critérios documentais e os predicados particulares continuam qualificados nas matrizes 635/64. A continuação03 e o writer03 comprovaram valores diários discriminantes, DTO/SQL/reinício e OOXML real. Pendem gráficos, interação e download no Chrome, PDF React da massa discriminante e demais predicados integrados do módulo. Não se atribui PASS às tentativas01/02 nem ao módulo por contagem de verificações..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/18).

## M20 — Usuários, perfis e segurança

### M20 — Usuários, perfis, autenticação, sessão, CSRF e 2FA — ROP-037, 039, 049, 064; RI03

**Matriz de perfis:** Gestor Local, Operacional, consulta e demais perfis reais encontrados; verificar endpoint por endpoint com usuário legítimo, sem login, com sessão expirada, escopo alheio, permissão revogada e CSRF ausente/incorreto; distinguir **401**, **403**, **409**, **404** de acordo com contrato. Não confiar no React como barreira de autorização.

**Sessão entre abas:** login A, iniciar fluxo assíncrono e reter resposta HTTP verdadeira; logout/login B em outra aba, confirmar cookies e `userId`, liberar A; **nenhum POST/PUT/DELETE da intenção A** depois de B; nenhum dado privado de A pode reaparecer; formulário parcial seguro e recuperação sem duplicidade. Testar variantes POST→PUT, PUT final já confirmado, perda de resposta após commit, resultado tardio de GET, múltiplos logouts/logins, abas em foco/desfoco, `storage` bloqueado, clear storage, evento focus, remount e corrida com 2FA. Backend deve permanecer protegendo a operação mesmo que UI tenha bug.

**2FA:** habilitar/desabilitar, recuperação de estado após novo login, recuperação de 401, rejeição de requisições antigas e limite razoável de polling; H01 realizou 710 GETs em 5s e H02 corrigiu o comportamento principal. Reexecutar HTTP real. Não expor segredo ou QR nas capturas. Sessão idle deve efetivamente encerrar UI e revogar token sem exceção de exclusão otimista; teste real de tempo e reconciliação de token.

**Status:** PARCIAL. **Páginas/rota:** /gestao-usuarios; /perfil; /permissoes-excepcionais. **Controllers:** Diretos: AutenticacaoControlador, PerfilControlador, PermissaoExcepcionalControlador, UsuarioAplicacaoControlador. Compartilhados: DocumentoComprobatorioControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M20. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-037, ROP-039, ROP-049, ROP-064. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/19).

## M21 — Auditoria e rastreabilidade

### M21 — Auditoria, causalidade e governança de alterações — ROP-050, 055, 060, 062–064

**Casos:** para publicar, retificar, corrigir, transferir, vincular, remanejar, anular, baixar documento restrito quando auditável e realizar exceção, verificar autor, perfil, data/hora, data efetiva, motivação, fonte, versão anterior/atual e sequência de eventos. Falha 401/403/409, cancelamento de confirmação ou resposta antiga não deve produzir auditoria de ato inexistente. O log deve ser consistente após rollback, cancelamento, reprocessamento, upgrade e reinício. Revisão de escopo administrativa não libera escrita em outra Gestão sem autorização temporal. ROP-064 foi **FALHOU na H02**: testar expressamente que nenhuma recusa, confirmação cancelada ou resposta tardia aparente sucesso ao usuário ou em banco.

**Status:** PARCIAL. **Páginas/rota:** /logs. **Controllers:** Diretos: RegistroAuditoriaControlador. Compartilhados: CalendarioEventoControlador, ContratoControlador, DocumentoComprobatorioControlador, FrequenciaAdicionalControlador, FrequenciaControlador, FrequenciaExcecaoControlador, HistoricoEmpregadoControlador, PermissaoExcepcionalControlador, VinculoContratualControlador. Handlers, endpoint_ids, fontes/SHA e limites: deliverables/01_REGRAS_CONTRATOS_MATRIZES/RASTREABILIDADE_ADENDO_FE02_C04/MAPA_CONTROLADORES_21_MODULOS.json#/modules/M21. Vínculo estático; não afirma cobertura funcional exaustiva..

**ROP normativos:** ROP-050, ROP-055, ROP-060, ROP-062, ROP-063, ROP-064. Associação de escopo não significa aprovação integral.

**UI:** NÃO EXECUTADO — Chrome0/24. **DB:** Somente métodos/predicadosJPA eHTTP explicitamente vinculados nosROPs/critérios; não todo omódulo.

**Pendências:** JornadasUI bloqueadas; autênticos33 e predicados particulares permanecem qualificados na635/64. Nenhum módulo é aprovado por nome/contagem. O fixture H02 observado não contém as tabelas e linhas legadas de riscos/processos e financeiro exigidas pelo roteiro. A comparação integral das 72 tabelas presentes comprova preservação desse universo, mas não a preservação material das estruturas/linhas ausentes. Não foi preparada massa legada legítima equivalente, não se reintroduziram mapeamentos removidos e não se executou DDL manual para fabricar essa situação. O roteiro permanece NÃO EXECUTADO integralmente; as provas parciais de upgrade têm seus próprios vínculos..

**API e vínculos integrais:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_21_MODULOS_NOVA_DADOS.json#/modules/20).

