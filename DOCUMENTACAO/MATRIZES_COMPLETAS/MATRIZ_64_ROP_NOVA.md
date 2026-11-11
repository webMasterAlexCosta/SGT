# Matriz 64 ROP — candidata04

> **Reconciliação vigente de09/11/2026:** este documento preserva os textos/resultados do corte histórico. A [decisão humana vigente](../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md) substitui sete dias fixos por N configurável, padrão30, limite exclusivo D+N; preserva as outras permissões, IDs e namespaces. MySQL/recibos antigos não aprovam PostgreSQL ou os bytes atuais. Governança de Terceiros permanece removida; Sonar está fora desta missão. Consulte o [plano atual](../CODEX/CAMPANHA_20261109/PLANO.md) para gates/evidências atuais.

Corte documental C04: 2026-11-04T08:22:33.455770+00:00. JAR 6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d; selo ad30c1a3facd3235cbfc6c9e8e3b1820bf67b23f8d7133efbfaf3f01690afc79. Corte C03 preservado no histórico.

**Homologação parcial.** A exportação é documental e não executa produto. Os valores completos estão nas tabelas CSV e nos JSONs de rastreabilidade. Nenhum campo vazio autoriza aprovação. 33 critérios documentais, Chrome 0/24, ALO/PER, FE02 e erros do consumidor automático permanecem qualificados.

O adendo C04 acrescenta a continuação HTTP/SQL/reinício03 e o XLSX variado03, ambos revisados independentemente. A limitação antiga de séries constantes fica como histórico, superada apenas no novo recorte DTO/OOXML; UI e PDF React continuam não comprovados. O mapa de controllers é estático, com handlers e fontes, e não altera classificações funcionais. Veja [o adendo e seus limites](RASTREABILIDADE_ADENDO_FE02_C04/ADENDO_C04_FE02_E_CONTROLADORES.md).

## ROP-001 — Cadastro da empresa contratada

**Dados iniciais:** Gestor Local ativo, CNPJ sintético livre, razão social e e-mail; senha/CSRF válidos.

**Ação:** Cadastrar empresa pela API operacional e pelo formulário.

**Esperado:** Persistir identidade normalizada e ativo=true; DTO contém somente id, razão, CNPJ, e-mail e ativo; não criar risco/processo.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/0).

## ROP-002 — Edição, inativação e reativação cadastral da empresa

**Dados iniciais:** Empresa criada e contrato publicado que já a fotografou.

**Ação:** Editar razão/e-mail/ativo e consultar a versão contratual anterior.

**Esperado:** Atualizar o cadastro sem apagar a empresa nem reescrever fotografias contratuais; não regravar o monitoramento legado.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/1).

## ROP-003 — Associação da empresa ao contrato e bloqueio após publicação

**Dados iniciais:** Fornecedor existente, um rascunho e um contrato publicado.

**Ação:** Associar o fornecedor aos dois contratos pela mesma API.

**Esperado:** Rascunho aceita; publicado retorna 409 e conserva empresa, versão e eventos; divergência CNPJ/instrumento é recusada.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/2).

## ROP-004 — Rascunho, composição e publicação contratual

**Dados iniciais:** Gestor Local e catálogos próprios; instrumento, empresa e vigência explícitos.

**Ação:** Criar/editar rascunho, compor lotação+turno+cargo, gerar prévia e publicar; tentar edição livre posterior.

**Esperado:** Publicação exige empresa/vigência/fonte/soma coerentes; congela a fotografia e impede edição livre; descarte permitido apenas nas condições de rascunho.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/3).

## ROP-005 — Cargo e catálogos operacionais por gestão

**Dados iniciais:** Cargo/turno/lotação da Gestão A; catálogo alheio B e uso futuro documentado.

**Ação:** Cadastrar/editar/inativar e consultar opções na data efetiva.

**Esperado:** Normalização e escopo preservados; inativação não apaga ID/história e não invalida programação futura sem tratamento.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/4).

## ROP-006 — Fluxo empresa → contrato → pessoa/empregado → vínculo → cargo/alocação

**Dados iniciais:** Empresa e contrato publicados; pessoa CPF sintético, catálogos válidos, matrícula conforme regra do tipo/contrato.

**Ação:** Cadastrar vínculo com pessoa e alocação inicial; ler Pessoa, Vínculo, Alocação e projeção Empregado; lançar frequência.

**Esperado:** Um cadastro canônico, IDs relacionados corretamente, projeção operacional gerada pelo domínio; nenhum cadastro duplicado pelo cliente.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/5).

## ROP-007 — Cadastro inválido e integridade de pessoa/matrícula

**Dados iniciais:** CPF/nome inválidos, matrícula obrigatória ausente ou já reservada; controle válido.

**Ação:** Tentar cadastro integrado e recontratação sem substituir identidade/opcionais.

**Esperado:** Recusar sem Pessoa, projeção, Vínculo, Alocação ou reserva parcial; matrícula pertence ao vínculo e permanece historicamente reservada.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/6).

## ROP-008 — Admissão programada, limites inclusivos e recontratação

**Dados iniciais:** Vínculo futuro; outro vínculo com último dia conhecido; mesma pessoa em duas gestões.

**Ação:** Consultar antes/no início; tentar nova admissão no último dia e no dia seguinte.

**Esperado:** Não antecipar vínculo/projeção operacional; último dia é inclusivo; sobreposição conflita, dia seguinte pode reutilizar a mesma Pessoa.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/7).

## ROP-009 — Alocação, mudança de cargo/turno/lotação e consentimento de excedente

**Dados iniciais:** Vínculo vigente, composição válida e composição lotada; alteração em data definida.

**Ação:** Consultar prévia, confirmar mudança com consentimento quando exigido e cancelar programação futura.

**Esperado:** Fechar faixa anterior no dia anterior sem desligamento; conservar história; excedente exige aceite específico e autoria; cancelamento restaura cobertura sem lacuna.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/8).

## ROP-010 — Presença sem batidas explícitas em jornada comprovada

**Dados iniciais:** Vínculo/alocação vigentes na data, jornada fotografada e controle noturno.

**Ação:** Registrar PRESENCA sem inventar horário mestre retroativo.

**Esperado:** Apuração utiliza fotografia da jornada e mantém identidade e memória física; falta de fonte não vira descanso fictício.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/9).

## ROP-011 — Presença integral por batidas físicas

**Dados iniciais:** Dia com jornada 08–12/13–17, contrato/vínculo/alocação válidos e fonte nominal sintética comprovada.

**Ação:** Enviar PRESENCA com 08:00, 12:00, 13:00, 17:00; reler e consolidar.

**Esperado:** PRESENCA, 480 minutos físicos; vínculo/alocação/contrato corretos, relatório coerente e evento com hash.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/10).

## ROP-012 — Presença parcial por batidas físicas

**Dados iniciais:** Mesmo cenário integral, saída às 15:00; cliente ainda solicita PRESENCA.

**Ação:** Enviar 08–12/13–15 e consultar memória física/consolidado.

**Esperado:** Backend classifica PRESENCA_PARCIAL com 360 minutos; redução noturna não transforma presença fisicamente parcial em integral.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/11).

## ROP-013 — Presença futura proibida inclusive ao Gestor Local

**Dados iniciais:** Amanhã em America/Sao_Paulo; controle de Operacional e Gestor Local com contexto válido.

**Ação:** POST direto para a data futura.

**Esperado:** 400 por datas futuras; nenhuma Frequencia ou EventoFrequencia persistido, sem exceção gerencial.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/12).

## ROP-014 — Sete dias: imediatamente antes do limite

**Dados iniciais:** Data D = 01/01/2026; instante 07/01/2026 23:59:59 America/Sao_Paulo; registro não retificado por gestor.

**Ação:** Avaliar política e tentar alteração operacional antes de D + 7 às 00:00.

**Esperado:** Janela aberta; dia da frequência conta como dia 1. Autorização/contexto continuam obrigatórios.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/13).

## ROP-015 — Sete dias: instante exato do limite

**Dados iniciais:** Data D = 01/01/2026; instante 08/01/2026 00:00 America/Sao_Paulo.

**Ação:** Avaliar política exatamente em D + 7; tentar edição/criação pelo Operacional.

**Esperado:** Política FECHADA no instante exato; escrita operacional proibida; não aguardar cron para bloquear.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/14).

## ROP-016 — Sete dias: após o limite

**Dados iniciais:** D passado além de D + 7; Operacional e Gestor Local da gestão responsável.

**Ação:** Tentar criação/edição operacional e correção formal gerencial.

**Esperado:** Operacional 403; Gestor Local pode corrigir com justificativa/chave válidas, preservando história. Não há prazo máximo gerencial encontrado.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/15).

## ROP-017 — Fuso do prazo e contador visual

**Dados iniciais:** Instantes 02:59:59Z e 03:00:00Z de 08/01/2026; cliente em fusos distintos; prazoEdicao sem offset.

**Ação:** Converter instante pela política e exibir contagem na UI.

**Esperado:** Backend usa America/Sao_Paulo com conversão do mesmo instante. Contador deve informar o mesmo limite sem conceder autorização local.

**Status:** PARCIAL. **Camadas:** ["UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/16).

## ROP-018 — Plantão noturno cruza meia-noite com mudança no dia seguinte

**Dados iniciais:** Plantão 22–06, alocação A na data inicial e B no dia seguinte.

**Ação:** Lançar as batidas e consultar dia inicial/dia de saída.

**Esperado:** Frequência e janela pertencem ao início; memória noturna preservada e alocação B não recebe parcela indevida.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/17).

## ROP-019 — Correção gerencial formal dentro e depois da janela

**Dados iniciais:** Frequência original e depois alterada por Operacional; Gestor Local legítimo.

**Ação:** Corrigir com motivo, chave e contexto válidos; reler eventos/autoria.

**Esperado:** Nova versão, RETIFICADA_GESTOR e bloqueio operacional; autor original/fotografias anteriores preservados; dentro da janela não fechar antecipadamente.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/18).

## ROP-020 — Bloqueio gerencial restrito ao registro corrigido

**Dados iniciais:** Dois registros distintos ainda ABERTA e editáveis; gestor corrige somente A.

**Ação:** Operacional tenta modificar A e B.

**Esperado:** A retorna 403; B permanece editável se prazo/contexto permitem.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/19).

## ROP-021 — Justificativa gerencial, chave e entrada inválida

**Dados iniciais:** Gestor Local; motivo ausente/curto, chave inválida; controle válido.

**Ação:** Enviar retificação e fechamento com payload inválido.

**Esperado:** Recusar sem versão/evento parcial; motivo formal e chave válida obrigatórios; atestado sem justificativa no fechamento é recusado.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/20).

## ROP-022 — Ausência/FALTA, sem lançamento e absenteísmo

**Dados iniciais:** Pessoa elegível com carga prevista; um dia FALTA e outro sem registro.

**Ação:** Consultar diária/mensal/intervalo e absenteísmo.

**Esperado:** FALTA e SEM_LANCAMENTO permanecem distintos; população, minutos previstos e totais concordam sem duplicação por alocação.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/21).

## ROP-023 — Atestado e ocorrência com anexo sobre presença existente

**Dados iniciais:** Presença física de 480 min já registrada; atestado sintético 1 dia com anexo classificado.

**Ação:** Criar ocorrência, consultar/baixar anexo e cancelar ocorrência autorizada.

**Esperado:** 201 na criação; classificar ATESTADO sem perder batidas; cancelamento restaura classificação cabível, conserva ocorrência cancelada/anexo e três eventos da frequência.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/22).

## ROP-024 — Ocorrência inválida e período que atravessa gestões

**Dados iniciais:** Data futura, dias inválidos, anexo/classificação incompatível, ocorrência indivisível 2 dias atravessando responsabilidade.

**Ação:** Tentar criação pelos perfis/gestões pertinentes e controle autorizado.

**Esperado:** Recusar período/futuro/escopo inválidos; autorizar todos os dias; não produzir fato, arquivo ou histórico parcial.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/23).

## ROP-025 — Vínculo explícito correto, homônimos e projeção de outra pessoa

**Dados iniciais:** Duas Pessoas/homônimos; IDs distintos; vínculo selecionado e empregado de origem divergente.

**Ação:** Selecionar vínculo manualmente e enviar origem incompatível em controle negativo.

**Esperado:** Não pesquisar identidade por nome/CPF textual; backend 409 se empregado diverge da projeção do vínculo; UI mantém seleção explícita e dados após recusa.

**Status:** PARCIAL. **Camadas:** ["REACT_UNIT_JSDOM"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/24).

## ROP-026 — Vínculo inexistente ou ID ausente

**Dados iniciais:** ID de vínculo não persistido; payload sem vínculo canônico; nenhuma frequência anterior.

**Ação:** POST/fetch de posição com ID inexistente/ausente e controle válido.

**Esperado:** Payload público sem vínculo 400; vínculo não encontrado 400; não escolher outro vínculo nem criar associação automática.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/25).

## ROP-027 — Vínculo fora da data efetiva, contrato inativo ou vínculo cancelado

**Dados iniciais:** Pessoa com vínculo programado, encerrado/cancelado ou contrato historicamente inativo; controle vigente.

**Ação:** Registrar frequência em data sem cobertura.

**Esperado:** 409 por ausência de vínculo operacional na data, inclusive com cadastro atual ativo; não antecipar admissão.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/26).

## ROP-028 — Alocação inexistente, cancelada ou incompatível com vínculo/data

**Dados iniciais:** Vínculo vigente sem alocação comprovada, faixa cancelada ou alocação de outro vínculo.

**Ação:** Tentar frequência e consultar opções para data histórica.

**Esperado:** 409 sem alocação Lotação+Turno+Cargo válida; não usar projeção atual nem associar faixa alheia.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/27).

## ROP-029 — Mudança cadastral de nome sem reescrever fotografias

**Dados iniciais:** Frequência/ato publicado antes da correção de identidade.

**Ação:** Corrigir identidade e retificar frequência; consultar cortes anteriores/posteriores.

**Esperado:** Identidade atual separada da histórica; autor/fotografia anterior preservados; desconhecimento não preenchido com nome atual.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/28).

## ROP-030 — Mudança de cargo/turno/município não altera apuração histórica

**Dados iniciais:** Alocação fotografada com grade de 480 min e município original; mestre alterado depois.

**Ação:** Consultar dia sem lançamento, lançar retroativo e consolidar antes/depois.

**Esperado:** Carga/feriado/rótulos da data seguem fonte comprovada; não usar configuração mestre posterior como prova retroativa.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/29).

## ROP-031 — Desligamento retroativo preserva presença, ocorrência e documento

**Dados iniciais:** Frequência e ocorrência com anexo nos dias dentro/depois do fim efetivo.

**Ação:** Desligar/retificar fim e recalcular relatórios.

**Esperado:** Último dia incluído; fatos posteriores preservados para auditoria, mas deixam cálculo oficial válido; pendência/revisão explícita, sem purga.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/30).

## ROP-032 — Transferência de gestão e acesso histórico corrigido

**Dados iniciais:** Contrato transferido com vínculo/frequência antigos; origem A, destino B e cortes de conhecimento.

**Ação:** Listar frequência, filtrar/paginar e consultar evento após transferência/retificação.

**Esperado:** Responsabilidade efetiva autoriza o recorte correto sem reescrever fotografia; origem/destino não capturam período alheio; total paginado só inclui autorizado.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/31).

## ROP-033 — Fonte histórica tardia e desconhecimento preservado

**Dados iniciais:** Publicação/composição com lacuna documental e prova posterior autorizada.

**Ação:** Incorporar prova por prévia/permissão e consultar antes/depois do conhecimento.

**Esperado:** Completar só campos comprovados da mesma origem; não reescrever original nem certificar cadeia incompleta; seis bloqueios documentais da matriz 198 não se tornam resolvidos por existir código.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/32).

## ROP-034 — Fechamento automático no oitavo dia

**Dados iniciais:** Frequências ABERTA vencidas, fontes/eventos originais intactos e lotes limitados.

**Ação:** Executar fechamento e repetir; consultar estado/eventos.

**Esperado:** FECHADA e evento encadeado uma vez; fonte/autoria preservadas; repetição não duplica; autorização já recusa prazo vencido antes do cron.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/33).

## ROP-035 — Fechamento assíncrono preserva comando e contexto

**Dados iniciais:** Escopo elegível; alterações explícitas ou comando coletivo com motivo/chave.

**Ação:** POST fechamento, acompanhar GET até estado terminal, ler frequências/eventos.

**Esperado:** 202, processamento após commit; CONCLUIDO, contadores corretos, motivo/chave/autoria do gestor preservados; usa alocação histórica.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado.","Dois1205 no consumidor automático durante500; recuperação de vencidas reais não comprovada.","Desempenho nacional e causalidade dos tempos não demonstrados."].

**Provas diretas:** [{"binding_id":"CLOSURE1-CONTROLE200","review":{"path":"review_notes/VERIFICACAO_CLOSURE1_FINAL04_20261104.json","sha256":"63932e54dc888716d750bac2d42b011f077eaf476541e8ac415f0c8257d45207","role":"AUDITORIA_NOVA_DOS_ARQUIVOS; não é execução adicional"},"scope":"Controle200 real na variante1;14UTseleção discriminantes emF12/reprodução; variante500 atual200 e comparação19campos por ordinal também auditadas.","status":"PASS NA CAMADA"},{"binding_id":"CLOSURE500-CONTROLE200","review":{"path":"review_notes/VERIFICACAO_CLOSURE500_FINAL04_02_20261104.json","sha256":"6fe4562bd78e75bd4d889a3f6d93df9ba0d89a6379f27a9d0abe785817f097d3","status":"PASS20 MANUAL; 2ERROS1205 DO CONSUMIDOR AUTOMÁTICO PRESERVADOS"},"status":"PASS NA CAMADA","scope":"20oráculos do controle manual;2timeouts do consumidor automático preservados, sem prova de vencidas após retry."},{"binding_id":"COMPARADOR-CLOSURE1-500","review":{"path":"review_notes/VERIFICACAO_COMPARADOR_CLOSURE_FINAL04_02_20261104.json","sha256":"34f3206172bcd3aa948d5fa9b66ada25d3f0fca62d8dbc62ff65864392440292","status":"PASS SEMÂNTICA200×19; NÃO BENCHMARK CAUSAL"},"status":"PASS NA CAMADA","scope":"200×19campos de semântica final iguais; tempos não isolam causalidade."}].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/34).

## ROP-036 — Cancelamento, reprocessamento e recuperação de fechamento

**Dados iniciais:** Job pendente/processando/erro/concluído e heartbeat vencido.

**Ação:** Cancelar/reprocessar estados permitidos, recuperar pendências e reiniciar processo.

**Esperado:** Estado terminal/conteúdo/contadores coerentes; concluído não reprocessa; recuperação não duplica fatos e conserva MySQL como fonte.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado.","Cancel/crash/revogação/segunda inicialização realizados na variante1; não reexecutados na500.","Sem dois executores ativos ou cluster."].

**Provas diretas:** [{"binding_id":"CLOSURE1-CANCEL-CRASH","review":{"path":"review_notes/VERIFICACAO_CLOSURE1_FINAL04_20261104.json","sha256":"63932e54dc888716d750bac2d42b011f077eaf476541e8ac415f0c8257d45207","role":"AUDITORIA_NOVA_DOS_ARQUIVOS; não é execução adicional"},"scope":"Cancelamento1→2prefixo/reatentativa198;queda4→200 mesma tentativa1/revogação/lease/segunda init;1JVM própria, semcluster","status":"PASS NA CAMADA"}].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/35).

## ROP-037 — Perfis e escopos de leitura/escrita operacional

**Dados iniciais:** Seis perfis canônicos; Gestões A/B e estados diferentes; usuário inativo/sem autorização.

**Ação:** Ler contratos/pessoas/frequência e tentar mutações por cada perfil.

**Esperado:** Gestor Local escreve no próprio escopo; Operacional apenas capacidades de frequência; leitores nacionais/estaduais/presidenciais não herdam escrita; dados minimizados; fornecedor exclusivo Local.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/36).

## ROP-038 — Documento contratual: upload, SEI, versão e invalidação

**Dados iniciais:** Documento SEI/anexo sintético, tipos/classificações válidos e inválidos; rascunho/publicado.

**Ação:** Criar, anexar, retificar SEI, substituir/invalidar e consultar cadeia.

**Esperado:** Nova versão preserva anterior; valida tipo/MIME/assinatura/sigilo; publicado não é apagado; rascunho tem regras próprias.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/37).

## ROP-039 — Download restrito, finalidade, revogação e expiração

**Dados iniciais:** Arquivo restrito íntegro, concessão válida/revogada/expirada, perfil consultivo e controle Local.

**Ação:** Baixar antes/depois da revogação/expiração e adulteração sintética.

**Esperado:** Revalidar autorização atual antes de entregar bytes; registrar finalidade/acesso; recusa preserva arquivo e prova anterior; checksum adulterado bloqueia.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/38).

## ROP-040 — Relatório, emissão histórica e invalidação de fonte alterada

**Dados iniciais:** Frequência elegível, emissão original e posterior retificação/alteração operacional.

**Ação:** Emitir, mudar fonte e consultar emissão/corte de conhecimento/comparação.

**Esperado:** Snapshot/hash/autor/período originais preservados; emissão anterior superada quando fonte muda; reenvio idêntico não invalida indevidamente.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/39).

## ROP-041 — Exportação XLSX/PDF síncrona e assíncrona

**Dados iniciais:** Relatório com dados significativos e duas gestões; exportação pronta sob concessão.

**Ação:** Exportar todos os dados filtrados; baixar pronto após alteração de autorização.

**Esperado:** Arquivo válido e conteúdo/quantidade/escopo corretos; exportação usa recorte completo e revalida autorização antes dos bytes.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/40).

## ROP-042 — Paginação preserva totais, cauda e filtros

**Dados iniciais:** Seis empresas; 31 colaboradores; mais de 5 documentos e opções/cargos em páginas posteriores.

**Ação:** Navegar primeira/intermediária/última página, filtrar e exportar.

**Esperado:** Cinco linhas visuais sem truncar universo; totais autorizados oficiais; nenhuma duplicação/perda de IDs; exportação não limitada à página visual.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","REACT_UNIT_JSDOM"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/41).

## ROP-043 — Idempotência de frequência com/sem chave

**Dados iniciais:** Mesma intenção operacional/gerencial, mesma chave e depois conteúdo diferente.

**Ação:** Reenviar sequencialmente e verificar versões/eventos; alterar intenção.

**Esperado:** Mesmo reenvio não duplica fato/versão/autor; mesma chave com intenção diferente 409 sem mutação; nova alteração legítima é versionada.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/42).

## ROP-044 — Frequência concorrente, inativação de contrato e executor revogado

**Dados iniciais:** Duas transações/threads; barreira SQL real; contexto/autorização mudam durante espera.

**Ação:** Enviar reenvios simultâneos e disputar frequência com inativação/revogação.

**Esperado:** Um fato idempotente; não confirmar frequência válida com contexto velho nem executor revogado após lock.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/43).

## ROP-045 — Rollback de lote com item inválido/futuro

**Dados iniciais:** Lote com primeiro item válido e segundo inválido/futuro.

**Ação:** POST/lote e reler frequências/eventos/histórico após falha.

**Esperado:** Nenhum item confirmado nem auditoria de sucesso parcial; 400 na presença futura; controle lote válido cria ambos.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/44).

## ROP-046 — Ocorrência: concorrência idempotente e rollback de arquivo/fato

**Dados iniciais:** Mesma ocorrência/anexo/chave em duas transações; falha induzida antes do commit.

**Ação:** Criar simultaneamente; em cenário separado reverter criação.

**Esperado:** Uma única fonte confirmada; rollback não deixa ocorrência/contextos/arquivo/histórico assinado parcial.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/45).

## ROP-047 — Documento: rollback e concorrência da cadeia

**Dados iniciais:** Documento rascunho com arquivo e duas concessões concorrentes.

**Ação:** Descartar com rollback e depois commit; disputar concessões.

**Esperado:** Arquivo somente descartado após commit autorizado; rollback conserva; cadeia do documento não bifurca.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/46).

## ROP-048 — Vínculos concorrentes e reserva global da Pessoa

**Dados iniciais:** Mesma Pessoa, dois contratos; retificações ou cancelamento de desligamento contra nova admissão.

**Ação:** Executar transações simultâneas e reler períodos.

**Esperado:** Não existir sobreposição global; exclusividade preservada após commit e em novo persistence context.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/47).

## ROP-049 — Fornecedor: autorização corrente após espera de lock

**Dados iniciais:** Gestor inicialmente autorizado; lock de contrato/fornecedor/CNPJ e revogação durante espera.

**Ação:** Associar/editar/criar, revogar antes de liberar a transação bloqueadora.

**Esperado:** Revalidar executor; recusa sem alterações indevidas, inclusive após rollback do reservador de CNPJ.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/48).

## ROP-050 — GOV-003: auditoria geral e publicação após commit

**Dados iniciais:** Operação bem-sucedida e operação revertida; Administrador para consulta geral.

**Ação:** Alterar domínio preservado, verificar auditoria após commit/rollback e ler API geral.

**Esperado:** Somente fato confirmado gera sucesso; eventos, autor, data e histórico permanecem consultáveis; remoção do módulo não elimina auditoria compartilhada.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/49).

## ROP-051 — APIs antigas realmente removidas com controle autenticado

**Dados iniciais:** Gestor Local válido, sessão e CSRF funcionais; GET /api/fornecedores retorna 200 como controle.

**Ação:** Invocar as dez rotas de resumo, fornecedores, associação, processos, status e riscos sob o prefixo antigo.

**Esperado:** 404 com ausência de handler; 401/403 não demonstram remoção. As rotas operacionais continuam disponíveis antes e depois.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/50).

## ROP-052 — UI removida, menu e nova página de empresas

**Dados iniciais:** Aplicação React, Gestor Local e perfis sem acesso; URLs antigas com query e subrota.

**Ação:** Abrir rotas antigas e menu; acessar cadastro de empresas, pesquisar a sexta empresa e enviar cadastro.

**Esperado:** Não renderizar governança, risco ou processo nem chamar endpoints removidos; o cadastro de empresas usa as novas rotas, confirmação de senha, CSRF e paginação.

**Status:** PARCIAL. **Camadas:** ["REACT_UNIT_JSDOM"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/51).

## ROP-053 — Classes, entidades, DTOs e jobs exclusivos de risco retirados

**Dados iniciais:** Classpath compilado e contexto Spring/metamodelo previstos para a candidata.

**Ação:** Inspecionar classes e DTOs; no runtime autorizado, verificar beans e agendamentos.

**Esperado:** ProcessoTerceiro, AvaliacaoRiscoFornecedor e RiscoContinuoServico ausentes; APIs não expõem escores, processos ou monitoramento de fornecedor. O núcleo operacional permanece.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/52).

## ROP-054 — Jobs operacionais de frequência, pendências, painel e auditoria preservados

**Dados iniciais:** Jobs e filas/pendências sintéticas; somente o risco exclusivo de fornecedor deve ser retirado.

**Ação:** Inicializar aplicação e observar agendamentos, execução após commit e recuperação; consultar resultados.

**Esperado:** Fechamento automático e recuperação, pendências de vigência/revisão, painel e auditoria continuam ativos. Monitoramento operacional permanece.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/53).

## ROP-055 — Upgrade com dados legados preservados sem purga nem DDL manual

**Dados iniciais:** Schema sintético criado pela baseline com Hibernate/update; fornecedores, riscos, processos, históricos e fotografias sintéticos.

**Ação:** Capturar contagens e hashes, iniciar candidata no mesmo catálogo e comparar após operações cadastrais.

**Esperado:** Nenhuma perda de linhas, tabelas ou histórico; classes exclusivas deixam de ser ativas; dados compartilhados permanecem. UPDATE cadastral não regrava monitoramento legado.

**Status:** NÃO EXECUTADO. **Camadas:** [].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["O fixture H02 observado não contém as tabelas e linhas legadas de riscos/processos e financeiro exigidas pelo roteiro. A comparação integral das 72 tabelas presentes comprova preservação desse universo, mas não a preservação material das estruturas/linhas ausentes. Não foi preparada massa legada legítima equivalente, não se reintroduziram mapeamentos removidos e não se executou DDL manual para fabricar essa situação. O roteiro permanece NÃO EXECUTADO integralmente; as provas parciais de upgrade têm seus próprios vínculos.","Chrome atual bloqueado antes de CDP/DOM; nenhuma jornada de UI aprovada."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/54).

## ROP-056 — Ausência financeira previamente decidida permanece

**Dados iniciais:** Baseline já sem tarifas, folhas monetárias e faturas; frequência e cadastro de fornecedor operacionais existentes.

**Ação:** Verificar metamodelo e handlers; chamar rotas financeiras retiradas com sessão autorizada.

**Esperado:** Sem restauração de órfãos financeiros; 404 nas rotas retiradas; frequência e cadastro operacional continuam funcionando.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/55).

## ROP-057 — Painel e indicadores operacionais após retificação

**Dados iniciais:** Cálculo assíncrono no escopo A e frequência que será corrigida.

**Ação:** Calcular, consultar por outro escopo, reutilizar cache e retificar a fonte.

**Esperado:** Resultado concluído com dados do escopo correto e recusa ao escopo alheio; não apresentar cache antigo como atual depois da retificação.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","REACT_EXPORT_FUNCTION_REAL_DTO_OOXML"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado.","FE02: o recorte HTTP/SQL/reinício e o XLSX variado foram comprovados; permanecem sem prova integrada os gráficos, a UI, o download Chrome e o PDF React com esses valores diários.","O contexto do exportador foi construído fora da UI; a desativação efetiva do watcher não foi demonstrada. A prova não encerra os demais filtros, respostas obsoletas, denominadores e predicados integrais deste roteiro."].

**Provas diretas:** [{"binding_id":"FE02-XLSX-FUNCAO-PRODUTIVA-DTO-REAL","review":{"path":"review_notes/VERIFICACAO_FE02_XLSX_FINAL04_20261104.json","sha256":"36c19697860394f082449951f6bccabc62b838ffd1af57c16d0789cbcc97a364","status":"PASS ARQUIVOS/ALINHAMENTO; FE02 PARCIAL POR COLUNAS CONSTANTES E UI/PDF AUSENTES"},"status":"PASS NA CAMADA","scope":"Dois XLSX reais,7folhas,15×12 células diárias; valores/tipos/formatos confrontados com DTOcreate/restart,29/02 sem comparativo e01/03 alinhado civilmente.","limit":"Contexto construído foraUI; nenhuma nova chamadaHTTP/Chrome/PDF. B/I/K/L discriminantes; C/D/E/F/G/H/J constantes. Watcher não comprovado desligado."},{"binding_id":"FE02-XLSX-VARIADO-CONTINUACAO03-DTO-REAL","kind":"REAL_HTTP_SQL_RESTART_AND_PRODUCTIVE_REACT_XLSX_WITH_DISCRIMINATING_DAILY_VALUES","status":"PASS NAS CAMADAS HTTP/SQL/REINICIO E FUNCAO PRODUTIVA XLSX; UI/PDF NAO COMPROVADOS","actual_HTTP_run_id":"FE02_HTTP_VARIADO_03","actual_writer_run_id":"FE02_XLSX_VARIADO_CONTINUACAO03_01","scope":"106 chamadas HTTP/328 asserções; 78 POSTs negociais e dois logins separados; corpus de 122 vínculos, 1.392 frequências/eventos/históricos/auditorias por universo. Duas JVMs com parada normal e fontes preservadas após reinício. Dez métricas com 29 valores distintos (15 atuais/14 anteriores); dois XLSX reais, sete abas/360 células cada, série diária 15×12. B–K: 15 valores distintos; L: 14 e texto — em 29/02, com 01/03 alinhado a 01/03/2023. Valores, tipos e formatos idênticos após reinício; somente docProps/core.xml difere.","limit":"Contexto do exportador construído fora da UI; a função produtiva, o writer OOXML e os DTOs reais não foram simulados. Sem jornada Chrome, renderização de gráficos, download por navegador ou PDF React nesta prova. watch:null foi solicitado, mas sua aplicação não foi comprovada. Excedente contratual permanece zero e não é coluna diária B–L. Tentativas anteriores interrompidas/falhas conservam seus resultados; não há aprovação integral do módulo ou ROP.","rop_ids":["ROP-057"],"gate_keys":["8.5::TMP-012","8.8::ARQ-003"],"criterion_predicates":[],"normative_scope":"M18/M19 FE02 e §7.8.2 no alcance DTO/OOXML; não aprova o PDF nem todos os predicados do ROP-057.","reviews":[{"path":"review_notes/CONTINUACAO_FE02_03_TERMINAL_20261104/VERIFICACAO_INDEPENDENTE_TERMINAL_CONTINUACAO03_REV02.json","sha256":"c968eec4f85d03603d0bb5b6512985b3f366bf21e037494db3541845ddd5f3b1","status":"PASS_ACTUAL_CONTINUATION03_HTTP_SQL_RESTART_NO_WRITER"},{"path":"review_notes/WRITER_FE02_CONTINUACAO03_TERMINAL_20261104/VERIFICACAO_INDEPENDENTE_WRITER03_OOXML_REAL.json","sha256":"474ca959358cd7a1e32f62f72bc6a754ef8ea44042ab5d476206e6932957f0b5","status":"PASS_ACTUAL_WRITER03_OOXML_DTO_REAL_NO_UI"}],"source_facts":{"path":"frontend_evidence/59_DOCUMENTACAO_FINAL_FE02/FATOS_FECHADOS_02/FATOS_FE02_FECHADOS.json","sha256":"b4d08f0c297b2260b701c1105fcaedbd8a1302f8aae7713ab73db096d350eac0"},"distinct_counts_not_summed":{"HTTP_assertions":328,"source_readback":12917,"independent_HTTP_SQL":17422,"producer_writer":26739,"independent_writer":23706,"new_Vitest_tests":0,"new_Chrome_journeys":0},"historical_evidence_preserved":true}].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/56).

## ROP-058 — HTTP do JAR empacotado com bundle novo

**Dados iniciais:** Candidata com artefatos identificados; conta restrita **nova**, sintética, provisionada em servidor **MySQL 8.0.45**, conexão JDBC observada e recibo desta nova campanha. Não reutilizar a conta/teste legado 8.0.46 da matriz C25.

**Ação:** Iniciar o JAR com perfil real; autenticar, obter CSRF, confirmar senha e invocar rotas operacionais e retiradas por TCP.

**Esperado:** Aplicação inicializa com Hibernate/update e bundle correto, sem classes/jobs exclusivos; respostas reais atendem aos contratos operacionais.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/57).

## ROP-059 — Navegador real: fluxo operacional completo e rotas retiradas

**Dados iniciais:** JAR, MySQL e React integrados; Gestor Local, Operacional e perfis de controle negativo.

**Ação:** Navegar empresa→contrato→pessoa/vínculo/alocação→frequência→ocorrência→relatório/exportação; verificar menus e rotas retirados.

**Esperado:** Concluir o fluxo com identidade canônica, autorização, erros visíveis, histórico e arquivo correto; nenhuma chamada a recurso retirado.

**Status:** BLOQUEADO POR AMBIENTE. **Camadas:** [].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/58).

## ROP-060 — Reinício conserva fatos, histórico e jobs

**Dados iniciais:** JAR com dados operacionais e legados sintéticos, job pendente e snapshots/hashes capturados.

**Ação:** Encerrar e reiniciar a aplicação; consultar os mesmos IDs, arquivos e jobs.

**Esperado:** Dados, documentos e cadeias persistem; pendências são recuperáveis; nenhum score, processo ou job retirado é reativado; nenhum seed destrutivo.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/59).

## ROP-061 — Listagem nacional: falha histórica existente

**Dados iniciais:** Contratos de vários testes no mesmo schema histórico; IND08 executado antes do CRUD nas duas rodadas anteriores.

**Ação:** GET /api/contratos com perfil nacional no método CRUD real.

**Esperado:** Se todas as cadeias visíveis são válidas, consulta 200; se houver cadeia inválida, 409 legítimo. Não impor 200 contra a validação.

**Status:** PARCIAL. **Camadas:** ["HTTP_TCP_MYSQL_JVM","MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** PARCIAL — predicados explícitos indexados. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/60).

## ROP-062 — Caracterização causal com restauração da única revisão sintética

**Dados iniciais:** Catálogo sintético sem outro escritor; contrato com cadeia válida na revisão 1; servidor **MySQL exatamente 8.0.45**. Não usar a versão histórica 8.0.46 referida na matriz antiga.

**Ação:** Confirmar 200; alterar somente a prova própria de 1 para 3; exigir 409 individual/nacional; restaurar somente essa revisão em finally e confirmar 200.

**Esperado:** 200→409→200; restauração restringida por IDs de contrato, evento e prova e predecessor nulo; não purgar nem relaxar a validação.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/61).

## ROP-063 — Isolamento entre IND08 negativo real e observador/CRUD

**Dados iniciais:** Schema autorizado e controle inicial válido; método negativo IND08 real; nenhuma purga intermediária.

**Ação:** Executar o IND08 real seguido de observador/CRUD; comparar estado antes e depois do único dado sintético alterado.

**Esperado:** Teste negativo individual continua retornando 409 durante a corrupção; ao encerrar, deve ter restaurado seu próprio dado sem deixar invalidade para o próximo teste.

**Status:** PARCIAL. **Camadas:** ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/62).

## ROP-064 — Recusas, confirmação cancelada e respostas antigas não simulam sucesso

**Dados iniciais:** Formulário preenchido; respostas 409/403 ou erro; senha cancelada; logout ou troca de data com requisição pendente.

**Ação:** Enviar ou consultar, cancelar confirmação, trocar contexto e resolver a resposta antiga.

**Esperado:** Preservar intenção e formulário; mostrar recusa real; não apresentar lista vazia ou sucesso indevido; não enviar confirmação vencida nem repopular outra sessão.

**Status:** PARCIAL. **Camadas:** ["REACT_UNIT_JSDOM"].

**HTTP:** NÃO VINCULADO NESTE ROP. **Chrome:** BLOQUEADO AMBIENTE —0/24jornadas atuais.

**Pendências:** ["Aceite integrado do roteiro requer suas camadas/predicados não demonstrados; nenhum resultado geral decorre do número de casos.","Chrome atual bloqueado."].

**Provas diretas:** [].

**Registro integral:** [conteúdo integral](RASTREABILIDADE_ADENDO_FE02_C04/MATRIZ_64_ROP_NOVA_DADOS.json#/rops/63).

