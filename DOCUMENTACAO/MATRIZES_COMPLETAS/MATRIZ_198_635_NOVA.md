# Matriz dos 198 requisitos e 635 critérios — candidata04

> **Reconciliação vigente de09/11/2026:** este documento preserva os textos/resultados do corte histórico. A [decisão humana vigente](../../JAVA/docs/REGRA_JANELA_FREQUENCIA.md) substitui sete dias fixos por N configurável, padrão30, limite exclusivo D+N; preserva as outras permissões, IDs e namespaces. MySQL/recibos antigos não aprovam PostgreSQL ou os bytes atuais. Governança de Terceiros permanece removida; Sonar está fora desta missão. Consulte o [plano atual](../CODEX/CAMPANHA_20261109/PLANO.md) para gates/evidências atuais.

Corte: 2026-11-04T03:18:33.728846+00:00. JAR 6ab39e7f0a234f452a11233db94bbbc1d28bccefafcfaaee8cef6e1be4debb7d; selo ad30c1a3facd3235cbfc6c9e8e3b1820bf67b23f8d7133efbfaf3f01690afc79.

**Homologação parcial.** A exportação é documental e não executa produto. Os valores completos estão nas tabelas CSV e nos JSONs de rastreabilidade. Nenhum campo vazio autoriza aprovação. 33 critérios documentais, Chrome 0/24, ALO/PER, FE02 e erros do consumidor automático permanecem qualificados.

## Critérios e agrupamentos

602 critérios estão PARCIAIS no aceite integrado e 33 estão BLOQUEADOS POR DOCUMENTAÇÃO. Entre os 602 não documentais, 600 têm conjunção técnica comprovada somente nas camadas declaradas e dois mantêm predicados parciais. O total de 633 conjunções técnicas inclui 33 ainda bloqueados pela autenticidade; não representa 633 aprovações.

## GOV-003

Nenhum fato negocial publicado é apagado fisicamente; pode ser cancelado, retificado, sucedido, encerrado ou substituído por nova versão, sempre auditável.

### GOV-003:001

DELETE publicado=409 e contrato permanece

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17","M21"]. **ROP(s):** [].

- Predicado 0: DELETE do publicado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Versões e eventos do mesmo contrato são consultados após a recusa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/0); métodos/recibos/assertions no mesmo registro e no CSV.

### GOV-003:002

Retificar mantém versões original e nova

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17","M21"]. **ROP(s):** [].

- Predicado 0: ADI e REM preservam três versões com IDs distintos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Complementos preservam fotografias e hashes encadeados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Substituição preserva original e corte de conhecimento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/1); métodos/recibos/assertions no mesmo registro e no CSV.

### GOV-003:003

Cancelar preserva autor/motivo/data e retira só efeito

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17","M21"]. **ROP(s):** [].

- Predicado 0: Cancelamento contratual conserva autor, motivo, três datas e alvo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Posição retira somente delta cancelado e conserva posterior válido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Calendário/complementos mantêm raiz e cadeia e retiram efeito corrente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/2); métodos/recibos/assertions no mesmo registro e no CSV.

## GOV-004

Rascunho nunca publicado e sem fato negocial pode ser descartado fisicamente de forma transacional, incluindo dependências exclusivas de preparação.

### GOV-004:001

Rascunho vazio aceita descarte 204

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17","M21"]. **ROP(s):** [].

- Predicado 0: Descarte de rascunho sem fato publicado retorna 204 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/3); métodos/recibos/assertions no mesmo registro e no CSV.

### GOV-004:002

GET após descarte=404

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17","M21"]. **ROP(s):** [].

- Predicado 0: GET do mesmo ID após DELETE 204 retorna 404 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/4); métodos/recibos/assertions no mesmo registro e no CSV.

### GOV-004:003

Fato/vínculo/histórico impede descarte

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17","M21"]. **ROP(s):** [].

- Predicado 0: Empregado associado impede descarte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Histórico de vínculo impede descarte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Evento negocial impede descarte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Contrato e composição permanecem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/5); métodos/recibos/assertions no mesmo registro e no CSV.

### GOV-004:004

Falha intermediária não deixa descarte parcial

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17","M21"]. **ROP(s):** [].

- Predicado 0: DELETE elimina contrato e composição dentro da transação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Exceção anterior ao commit desfaz ambos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Nova transação e GET recuperam contrato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/6); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-001

Contrato é a base operacional; Colaborador/Pessoa, Vínculo Contratual e Alocação são conceitos distintos.

### TMP-001:001

Pessoa, Vínculo e Alocação são raízes relacionadas distintas; cadastro integrado preserva a identidade canônica.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Raízes Pessoa/Vínculo/Alocação distintas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Identidade canônica após cadastro integrado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Atomicidade da inclusão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/7); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-001:002

Frequência resolve Contrato/Vínculo/Alocação da data e devolve seus identificadores separadamente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Resolução por data — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Identificadores separados de Contrato/Vínculo/Alocação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/8); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-001:003

Empregado legado sem associação/fotografia não adota outra Pessoa apenas por igualdade do CPF; fotografia própria existente conserva a identidade documentada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: CPF coincidente não autoriza associação legada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Fotografia própria mantém identidade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/9); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-002

Colaborador representa a pessoa; Vínculo representa a relação temporal pessoa-contrato; Alocação representa Lotação + Turno + Cargo durante uma faixa do vínculo.

### TMP-002:001

Recontratação em dias consecutivos conserva Pessoa e cria novo Vínculo sem sobreposição.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Mesma Pessoa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Novo Vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Dias consecutivos sem sobreposição — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/10); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-002:002

Mudança de Lotação/Turno/Cargo retifica faixas da Alocação, preservando o mesmo Vínculo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Mudança temporal de composição — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Faixas da Alocação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Vínculo preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/11); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-003

Toda ocorrência relevante separa data efetiva, data/hora de registro, usuário responsável e motivo/justificativa quando aplicável.

### TMP-003:001

Fatos separam data efetiva, instante de conhecimento, autor e motivo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Data efetiva distinta do registro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Autor — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Motivo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/12); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-003:002

Alterações de calendário preservam fotografia integral, sequência, autoria e conhecimento.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Fotografia integral do calendário — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Sequência e autoria — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Corte de conhecimento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/13); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-003:003

Fontes manuais mantêm criação, retificação de retificação e cancelamento com autoria e motivo próprios.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Criação e duas retificações — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cancelamento preserva origem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Autoria e motivo próprios — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/14); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-003:004

Motivo aceito de adicional é persistido integralmente na raiz e na versão; excesso é recusado antes da gravação.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Motivo integral na raiz e versão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Limite aceito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Excesso sem gravação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/15); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-003:005

Normalização do motivo de um novo comando é coerente na validação, raiz e versão, sem reescrever fatos anteriores.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Normalização antes de validar — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Raiz e versão coerentes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: História anterior preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/16); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-004

O sistema deve reconstruir a posição histórica pela data efetiva sem usar a projeção atual como fonte canônica.

### TMP-004:001

Posição histórica contratual e de alocação usa fotografias dos fatos.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Fotografia histórica contratual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Fotografia de alocação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Ausência não suprida pelo mestre — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/17); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-004:002

Gravar ou retificar frequência sobre alocação legada não converte rótulos atuais em fotografia de contexto histórico.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Rótulos legados ausentes permanecem ausentes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Gravação/retificação não usam mestre atual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Grade necessária não é inventada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/18); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-004:003

Leitura e retificação de frequência mantêm a identidade já fotografada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Fotografia de identidade em leitura — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Retificação de status preserva nome — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: D/K não retroage correção — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/19); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-004:004

A posição em corte anterior deriva o estado do evento válido naquele corte.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Evento válido no corte anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Retificação/cancelamento não antecipados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/20); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-004:005

Retificação/cancelamento de fonte manual não apaga o fato que reconstrói o passado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Retificação preserva fonte original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cancelamento não apaga reconstrução passada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/21); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-005

A data/hora de registro informa quando o sistema tomou conhecimento; não substitui a data real do fato.

### TMP-005:001

Contrato, Vínculo e Alocação respeitam o conhecimento disponível até o instante solicitado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Contrato respeitaK — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Vínculo respeitaK — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Alocação respeitaK — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/22); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-005:002

Calendário e complemento separam data do fato e instante de registro em consultas de conhecimento.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Data do calendário/complemento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Instante de registro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Consulta em cortes distintos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/23); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-006

Reinicialização do sistema não cria, encerra ou altera fato negocial; apenas reconstrói projeções a partir da história válida.

### TMP-006:001

Reexecutar inicializadores não cria ou altera fatos negociais existentes.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Reexecução de inicializadores — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Fatos existentes intactos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/24); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-006:002

Chegada da data de fato programado altera a posição consultada sem gravar outro fato.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Data programada altera leitura — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Nenhum novo fato escrito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/25); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-007

O sistema trabalha com granularidade negocial diária para vínculo/alocação; horário serve à auditoria do registro, não para duas alocações no mesmo dia.

### TMP-007:001

Alocação possui granularidade diária, inclusive quando retificada na própria data.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Uma alocação por dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Retificação no próprioD — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/26); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-007:002

Jornada22–06 usa a alocação da data de início mesmo após mudança no dia seguinte.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Turno22–06 pertence à data inicial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Troca emD+1 não divide presença — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/27); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-008

Eventos retroativos são permitidos e devem preservar/reaplicar fatos posteriores válidos, sem mover/apagar pessoas silenciosamente.

### TMP-008:001

Documento retroativo recompõe o contrato e reaplica documento posterior válido.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Inserção retroativa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 1: Reaplicação de documento posterior válido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/28); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-008:002

Mudança retroativa e retificações sucessivas preservam as faixas originais e sucessoras válidas.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Faixa original restaurável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Sucessoras válidas preservadas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Retificações sucessivas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/29); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-008:003

Extensão através de transferência documentada recompõe destino sem recontratar e valida sua capacidade.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Reconstrução através de transferência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Mesmo Vínculo/Pessoa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Capacidade e fotografia do destino — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/30); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-008:004

Restauração de faixa e cancelamento de dispensa preservam a obrigação posterior conhecida ou recusam o conflito sem gravação parcial.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Obrigação posterior conhecida respeitada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Recusa de restauração incompatível — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Atomicidade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/31); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-009

Quando evento retroativo torna evento posterior incompatível, apresentar impacto e gerar pendência de tratamento; não apagar automaticamente.

### TMP-009:001

Replay incompatível preserva documento posterior e gera pendência identificável.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Instrumento posterior preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Pendência identificável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Tratamento causal e rollback — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/32); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-009:002

Retirada retroativa de cobertura mantém a frequência e encaminha revisão.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Frequência não apagada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cobertura inválida explícita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Encaminhamento à revisão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/33); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-009:003

Programação humana futura incompatível não é cancelada como ajuste técnico.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Programação humana futura preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Tratamento explícito antes do fato incompatível — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/34); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-010

Eventos conflitantes na mesma data efetiva exigem precedência negocial explícita; sem precedência, a conclusão é bloqueada.

### TMP-010:001

Colisão na mesma data é bloqueada até existir ordem negocial completa.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Colisão detectada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 1: Ordem completa exigida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 2: Conclusão bloqueada sem precedência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/35); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-010:002

SUCEDE explícito define a posição sem usar ordem técnica de gravação.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: SUCEDE determina ordem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: ID e instante não substituem relação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/36); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-010:003

Restaurar sucessora no novo último dia exige precedência entre os fatos.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Restauração no último dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Precedência explícita entre alocação e término — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/37); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-011

Eventos da mesma data que não colidem podem coexistir.

### TMP-011:001

Recortes distintos na mesma data podem coexistir; recortes colidentes continuam exigindo precedência.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Recortes independentes coexistem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Mesmo campo continua conflitante — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/38); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-012

Documento antigo recebido depois de documento posterior é inserido na posição temporal correta, com reaplicação dos fatos posteriores válidos e tratamento explícito de conflitos.

### TMP-012:001

Documento antigo recebido depois entra pela data efetiva e reaplica fatos válidos posteriores.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Recebimento posterior não alteraD — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 1: Deltas posteriores válidos reaplicados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/39); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-012:002

Se o documento antigo torna um posterior incompatível, o posterior é preservado e o conflito fica explícito.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Documento posterior incompatível retido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Conflito explícito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/40); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-013

Início/aditamento/remanejamento/nova alocação usam a data efetiva como primeiro dia da nova situação; desligamento usa a data como último dia do vínculo.

### TMP-013:001

Desligamento usa a data como último dia; novo vínculo no dia seguinte não sobrepõe.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Último dia inclusivo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Novo vínculo emD+1 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Sem sobreposição — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/41); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-013:002

Mudança de alocação aplica nova situação desde a data indicada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Nova alocação desdeD — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Anterior termina emD−1 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/42); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-014

Um evento não pode produzir efeito fora da vigência reconstruída do contrato, exceto quando o próprio fato, de forma válida, altera a vigência para abranger a data do efeito.

### TMP-014:001

ADI fora da vigência só produz efeito quando o próprio ato amplia validamente a cobertura.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Fato anterior/posterior fora da vigência recusado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Extensão válida no próprioADI aceita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/43); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-014:002

Prorrogação conhecida e programada permite cadastro dentro da extensão mantendo término humano.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Prorrogação conhecida permite cadastro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cobertura programada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Término humano preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/44); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-015

Prorrogação do mesmo contrato deve manter continuidade temporal; não é permitida lacuna interna de vigência.

### TMP-015:001

Prorrogação não pode criar lacuna interna na vigência do mesmo contrato.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Continuidade da vigência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Lacuna interna recusada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/45); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-015:002

Extensão contínua não apaga término humano nem recontratação já programada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Extensão não apaga término humano — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Recontratação programada preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/46); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-016

Precedência negocial é representada por relação explícita entre eventos (`SUCEDE`, `RETIFICA`, `CANCELA`, `SUBSTITUI`, `MANTÉM/COEXISTE`) e, quando existente, pela sequência formal do instrumento; conflito sem precedência definida bloqueia conclusão.

### TMP-016:001

Relação de precedência pode ser transitiva através de ato que altera outro campo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Transitividade através de outro campo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Ambiguidade não encoberta — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/47); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-016:002

Retificar predecessor conserva a sequência de sucessor válido e o original consultável.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Retificação preserva ordem causal — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Sucessor válido prevalece — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Original consultável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/48); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-016:003

Restauração coincidente com término não pode usar data de registro como precedência implícita.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Último dia não desempata por registro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Decisão de precedência auditada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/49); métodos/recibos/assertions no mesmo registro e no CSV.

## TMP-017

`FUTURO`, `VIGENTE` e `ENCERRADO` são derivados da linha temporal válida e da data corrente; rotinas técnicas apenas atualizam projeções/alertas/pendências. `CANCELADO` e `ANULADO` dependem de evento explícito.

### TMP-017:001

FUTURO/VIGENTE/ENCERRADO são posições derivadas da data e não novos estados negociados.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Estados temporais derivados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Nenhum fato adicional pela passagem do tempo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/50); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-017:002

Programação válida durante inatividade não reativa a operação silenciosamente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Eventos programados preservam história — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Inatividade operacional continua — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/51); métodos/recibos/assertions no mesmo registro e no CSV.

### TMP-017:003

Anulação explícita afeta vínculo desde seu alcance, preservando antes da data/corte.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M08","M09","M10","M12"]. **ROP(s):** [].

- Predicado 0: Alcance da anulação emD demonstrado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Faixa anterior aD preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: CorteK anterior à anulação não demonstrado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/52); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-001

Número + ano identificam unicamente o contrato em todo o sistema.

### CON-001:001

Duplicar número/ano na mesma Gestão=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Duplicação de número/ano na mesma Gestão retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/53); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-001:002

Duplicar entre Gestões=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Duplicação do par entre Gestão A e B retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/54); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-001:003

Número igual em ano distinto pode existir

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Dois POSTs com mesmo número e anos diferentes retornam 201 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: IDs distintos e anos/números conferidos na releitura JPA — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/55); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-001:004

Restrição JPA uk_contrato_numero_ano presente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Anotação JPA contém unicidade conjunta numero_contrato+ano_contrato — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Nome declarado é uk_contrato_numero_ano — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/56); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-002

Número e ano podem ser corrigidos enquanto RASCUNHO, respeitando unicidade.

### CON-002:001

PUT em rascunho altera número/ano

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: PUT de rascunho retorna 200 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Resposta contém número normalizado e ano solicitado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/57); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-002:002

Número/ano duplicado bloqueia sem persistir

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: PUT duplicado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Identidades de ambos os contratos preservadas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Identidade histórica reservada também rejeitada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/58); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-002:003

PUT publicado=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: PUT comum de contrato publicado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/59); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-003

Após publicação, número + ano são imutáveis no fluxo comum; correção excepcional é versionada/auditada e mantém o identificador antigo pesquisável.

### CON-003:001

PUT comum publicado=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: PUT comum de contrato publicado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/60); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-003:002

Sem concessão correção=403

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Correção sem concessão específica retorna 403 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Mesma intenção após concessão retorna 201 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/61); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-003:003

Correção autorizada preserva antes/depois e versões

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Correções autorizadas preservam números anterior e novo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Duas correções são relidas em JPA com conteúdo próprio — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Posições em três cortes preservam original, retroativo e posterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Projeção atual mantém a correção posterior válida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/62); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-003:004

Pesquisa pelo identificador anterior encontra contrato

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Pesquisa com número/ano anterior encontra exatamente o contrato original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Gestão externa não recebe o histórico — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/63); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-003:005

Reenvio exato não cria nova correção

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Reenvio exato retorna mesmo ID — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Lista de correções permanece com um item — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/64); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-004

Rascunho descartado que nunca foi publicado pode liberar seu número/ano para reutilização.

### CON-004:001

Descartar rascunho e recriar mesmo número/ano=201

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Recriação de número/ano descartado retorna 201 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/65); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-004:002

Novo ID difere do descartado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Novo ID difere explicitamente do ID descartado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/66); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-004:003

Publicado permanece impedido de excluir

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: DELETE do publicado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Consulta posterior de versões/eventos continua disponível — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/67); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-005

Contrato possui descrição, tipo de contratação, empresa/CNPJ/razão social/nome fantasia, vigência, situação temporal/negocial, habilitação administrativa, efetivo total, composição, aditamentos/remanejamentos, pendências e histórico.

### CON-005:001

Detalhe tem campos obrigatórios e eixos distintos

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Detalhe confere descrição, tipo, CNPJ, razão, fantasia e vigência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Estado, situação temporal e habilitação conferidos em campos separados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Total, base original, Gestão e publicação conferidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/68); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-005:002

Rotas composição/instrumentos/pendências/eventos expõem fatos do mesmo contrato

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Composições e IDs de ADI/REM do contrato conferidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Histórico contém PUBLICACAO, ADITAMENTO e REMANEJAMENTO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Endpoint de pendências responde vazio estruturado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Comprovação integral do critério — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/69); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-005:003

Consulta datada preserva empresa/tipo/vigência da data

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Empresa antes/depois do efeito conserva CNPJ, razão e fantasia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Tipo da origem/destino e fotografia após renomeação conferidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Prorrogação conserva término anterior do vínculo e reconstrói nova vigência contratual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/70); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-006

Data de início e fim de vigência são obrigatórias para contrato publicado.

### CON-006:001

Rascunho sem uma data=201

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: POST de rascunho sem início aceita 201 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: POST de rascunho sem fim aceita 201 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/71); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-006:002

Publicar sem início ou fim=400

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Ausência isolada de início causa 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Ausência isolada de fim causa 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Completar somente o campo permite publicar — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/72); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-006:003

Fim anterior ao início rejeitado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Fim anterior ao início causa 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Contrato inválido não aparece na contagem JPA — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/73); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-006:004

Publicação válida mantém datas efetivas

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Publicação válida seguida de detalhe mantém exatamente início e fim preparados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/74); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-007

Situação temporal/negocial `RASCUNHO`, `FUTURO`, `VIGENTE`, `ENCERRADO`, `CANCELADO` e `ANULADO` é separada da habilitação administrativa ativo/inativo e das pendências.

### CON-007:001

Contrato pode ser VIGENTE+INATIVO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Mesma posição datada é VIGENTE e INATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/75); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-007:002

Passagem do tempo muda FUTURO/VIGENTE/ENCERRADO sem evento novo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Função temporal classifica antes do início, início, fim e dia seguinte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: GET com relógio avançado preserva fotografia dos fatos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/76); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-007:003

Anulado/cancelado dependem de evento

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: RASCUNHO/CANCELADO/ANULADO derivam de estado explícito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Cancelar/anular são comandos próprios com evento preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/77); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-007:004

Detalhe e posição temporal devem concordar na mesma data

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Detalhe e posição do contrato 2403 em 2026-11-02 contêm PUBLICADO/VIGENTE/ATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: IDs e datas da mesma entidade conferidos no recibo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/78); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-008

Publicação exige dados obrigatórios e composição consistente; rascunho pode estar incompleto.

### CON-008:001

Empresa/tipo/vigência/fonte incompletos bloqueiam

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Ausências isoladas de tipo, dados empresariais, início/fim e fonte bloqueiam — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Sem estado/versão/evento parcial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Completar só a omissão permite publicar — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/79); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-008:002

Soma divergente e duplicidade bloqueiam

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Publicação com soma 9 e total 10 retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Repetir combinação retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Corrigir soma permite publicar — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/80); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-008:003

Publicação cria evento/fotografia/base original

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Publicação grava exatamente um evento e uma versão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Base original é igual ao total publicado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: CNPJ e efetivo da fotografia original conferidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/81); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-008:004

Todos valores publicados devem estar reconstruíveis sem mestre atual

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Grade e município publicados sobrevivem à alteração dos mestres — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Nomes de lotação/turno/cargo preservados em projeção e nova alocação retroativa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Empresa e tipo datados preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Configuração sem prova permanece null — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 4: Comprovação integral do critério — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/82); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-009

O contrato pertence a uma única Gestão Local por vez; Gestor Estadual acessa a estrutura sem tornar o contrato compartilhado entre Gestões.

### CON-009:001

Gestor Local escreve só própria Gestão

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Trinta rotas de mutação recusam Gestor de outra Gestão com 404 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Gestor da própria Gestão realiza CRUD autorizado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/83); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-009:002

Gestor Estadual lê estado e não escreve

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Gestor Estadual lê detalhe/listagem do seu estado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: POST e demais mutações contratuais retornam 403 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/84); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-009:003

Gestões externas recebem 404

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Leitura por Local/Estadual/Operacional externo retorna 404 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Mutações de Gestão externa retornam 404 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/85); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-009:004

Transferência preserva titular anterior por período

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Posição anterior à transferência identifica origem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Posição desde o efeito identifica destino — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/86); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-010

Pode existir inativação administrativa durante a vigência, distinta da situação temporal do contrato. A inativação possui data efetiva, data/hora de registro, motivo e autor; a reativação é registrada como novo evento auditável.

### CON-010:001

Criar intervalo INATIVO sem mudar vigência/vínculo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Intervalo INATIVO coexistindo com VIGENTE — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Vínculo permanece aberto e único — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Resposta após inativação mantém 2026-01-01 a 2027-12-31, datas da fixture original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/87); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-010:002

Futuro não bloqueia antes da data

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Inativação futura conserva ativo antes da data — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Na data controlada, detalhe INATIVO e nova operação recusada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/88); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-010:003

Reativação preserva intervalo e vínculo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Após reativação posição anterior continua INATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Posição desde a reativação é ATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Vínculo original permanece aberto e único — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/89); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-010:004

Prévia desatualizada bloqueia confirmação

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Nova frequência invalida token anterior com 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Dependência alterada na faixa retirada também invalida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Nova prévia permite confirmar — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/90); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-011

Enquanto administrativamente inativo, eventos contratuais programados continuam produzindo seu efeito histórico, mas não liberam operação até reativação explícita.

### CON-011:001

ADI programado altera descrição durante bloqueio

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Posição após ADI programado exibe descrição nova dentro do intervalo INATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/91); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-011:002

REM programado altera distribuição

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: REM programado altera a primeira composição de 25 para 20 dentro do bloqueio — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/92); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-011:003

Habilitação continua INATIVO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Posição posterior aos instrumentos conserva habilitação INATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/93); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-011:004

Vínculo na data bloqueada=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: POST de vínculo com início no período INATIVO retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/94); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-012

Prorrogação retroativa que torna o contrato temporalmente vigente não reativa automaticamente sua operação se houver bloqueio/pendência administrativa.

### CON-012:001

Extensão válida torna situação VIGENTE

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Prorrogação retroativa torna VIGENTE o período posterior ao fim original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/95); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-012:002

Habilitação segue INATIVO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Posição do contrato prorrogado permanece INATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/96); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-012:003

Vínculo encerrado mantém término

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Vínculo encerrado mantém dataFim original, causa e estado ENCERRADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Eventos permanecem iguais e ocupação atual zero — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/97); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-012:004

Retomada depende de comando explícito

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Prorrogação não reabre vínculo encerrado nem cria ocupação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Comprovação integral do critério — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/98); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-013

Mudança de Gestão Local não ocorre no fluxo comum; transferência excepcional exige data efetiva, motivo, auditoria e preservação da Gestão responsável em cada período.

### CON-013:001

PUT comum publicado=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: PUT livre do publicado é recusado com 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/99); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-013:002

Transferência exige autorização

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Mapa válido sem concessão retorna 403 sem mutação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Após concessão específica mesma intenção aceita e registra uso — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/100); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-013:003

Antes da data Gestão origem

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Posição no dia anterior à transferência identifica Gestão origem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/101); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-013:004

Depois da data Gestão destino

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Desde o efeito, posição identifica Gestão destino e Tipo mapeado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/102); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-013:005

Nenhum cadastro histórico original é reescrito

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Renomeação posterior dos oito nomes não reescreve a transferência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Transferência retroativa usa nome comprovado da origem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/103); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-014

Contrato original pode ser cadastrado retroativamente, com fonte comprobatória, data efetiva real, registro/auditoria e prévia de impactos.

### CON-014:001

Sem prévia retroativa=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Publicação retroativa completa sem prévia retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Detalhe segue RASCUNHO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/104); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-014:002

Prévia+confirmação publica

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Prévia fornece token e data efetiva — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Confirmação com token atual publica a preparação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/105); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-014:003

Mudança da preparação invalida token

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Alterar descrição, empresa ou composição invalida token — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Alterar grade/município invalida token — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Rejeição não publica fato parcial e nova prévia autoriza — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/106); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-014:004

Evento mantém início real e registro distinto

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Evento mantém início retroativo exato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: registradoEm é posterior ao instante imediatamente anterior à publicação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Autor corresponde ao executor — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/107); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-015

Existe estado negocial `ANULADO` para contrato publicado posteriormente declarado inválido. A anulação não exclui fisicamente o contrato e deve preservar motivo, fonte comprobatória, autor e data/hora.

### CON-015:001

Sem data do ato/fonte rejeita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Anulação sem data do ato ou sem fonte retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Estado, vínculo, fim e contagem de eventos preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/108); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-015:002

Anular preserva contrato

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Após anulação contrato e eventos continuam consultáveis — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: DELETE do anulado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/109); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-015:003

Evento guarda motivo/autor/data/fonte

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Evento confere motivo, autor, data do ato, data efetiva, registro não vazio e SEI — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/110); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-015:004

Posição no alcance ANULADO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Posição desde alcance é ANULADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Dia anterior preserva validade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/111); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-016

Encerramento antecipado/rescisão é distinto de fim natural de vigência e de inativação administrativa. Ele altera a data final efetiva do contrato e exige fonte comprobatória por anexo e/ou referência SEI.

### CON-016:001

Data final efetiva reduzida

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Resposta do encerramento antecipa dataFim exatamente para dataEfetiva — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/112); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-016:002

Fonte obrigatória

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Ausência de fonte causa 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Fluxo com SEI e prévia permite encerrar — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/113); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-016:003

Vínculo aberto termina no mesmo dia com ENCERRAMENTO_ANTECIPADO_CONTRATO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Vínculo aberto termina na mesma data — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Causa relida é ENCERRAMENTO_ANTECIPADO_CONTRATO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/114); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-016:004

Dia seguinte contrato ENCERRADO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Posição no dia seguinte ao encerramento retorna ENCERRADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/115); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-016:005

Nenhuma realocação automática

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Contrato do vínculo e lotação da alocação preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Uma única faixa e um único vínculo da pessoa permanecem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/116); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-016:006

Rescisao compara termo com vigencia reconstruida por atos futuros.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: ADI futuro prorroga fim — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Rescisão dentro da prorrogação é aceita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Dia seguinte retorna ENCERRADO com fim reconstruído exato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: ADI preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/117); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-017

Contrato FUTURO pode receber aditamento/remanejamento programados, com documentação válida, coerência temporal e nenhum efeito antes do permitido.

### CON-017:001

Instrumento futuro persiste sem efeito antecipado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: ADI futuro conserva descrição original antes do efeito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: REM futuro conserva distribuição 25/75 antes do efeito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Instrumentos permanecem listados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/118); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-017:002

Na data correta aparece em posição

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Na data efetiva aparece descrição da versão 3 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Distribuição desde o efeito é 10/90 sem somar versões antigas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/119); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-017:003

Fora de vigência bloqueia salvo extensão válida no próprio ADI

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: ADI e REM fora da vigência retornam 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: ADI que prorroga validamente no próprio fato é aceito e reconstrói fim correto — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/120); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-017:004

Cancelamento anterior ao efeito preserva história

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Cancelamento antes do efeito preserva instrumento, situação e evento CANCELA — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Efeito antigo deixa de participar da posição — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/121); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-018

Anulação preserva fatos relacionados para auditoria, retira sua validade operacional e nunca realoca colaboradores automaticamente.

### CON-018:001

Vínculo e alocação continuam consultáveis

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Vínculo original continua acessível e é marcado ANULADO desde o alcance — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Comprovação integral do critério — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/122); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-018:002

Contrato da pessoa não é substituído

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Vínculo preservado continua associado ao mesmo contrato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/123); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-018:003

Frequência afetada mantém prova mas deixa cálculo válido

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Frequência FECHADA/FALTA e autor original preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cobertura torna-se inválida com revisão e bloqueio — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Emissão anterior conserva conteúdo/hash — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Cálculo atual exclui registros/minutos/faltas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 4: Controle anterior ao alcance permanece válido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/124); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-018:004

Anulação possui origem auditável

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Ato de anulação conserva motivo, autor, três datas e fonte SEI na história — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/125); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-019

Número/ano de contrato publicado e posteriormente anulado permanece reservado e pesquisável, sem reutilização.

### CON-019:001

DELETE anulado=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: DELETE de anulado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/126); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-019:002

Criar mesmo número/ano=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: POST com número/ano do anulado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/127); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-019:003

Consulta histórico preserva o anulado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Consulta de eventos preserva anulação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Correção posterior mantém os dois fatos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/128); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-020

Anulação registrada incorretamente é corrigida por novo evento auditado; não há reativação silenciosa.

### CON-020:001

Correção conserva anulação original

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: História depois da correção/reativação contém ID da anulação original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Novo evento de correção possui ID distinto — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/129); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-020:002

Relação CANCELA aparece na história

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Endpoint de precedências e JPA conferem CANCELA com alvo original e fonte CORRECAO_ANULACAO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/130); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-020:003

Detalhe e posição temporal permanecem INATIVO até reativação explícita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Detalhe e posição atuais ficam INATIVO após correção — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Reativação anterior não é reutilizada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Correção futura só bloqueia desde seu alcance — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Novo comando reativar libera operação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/131); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-020:004

Novo vínculo não opera antes da reativação

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Novo vínculo antes de reativar retorna 409 sem pessoa/faixa parcial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Mesma inclusão após reativar retorna 201 e um único vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/132); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-021

Contrato FUTURO válido cancelado antes do início assume estado CANCELADO, distinto de ANULADO e ENCERRADO.

### CON-021:001

Contrato já iniciado não admite cancelar-futuro

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Contrato já iniciado recusa cancelar-futuro com 409 e conserva PUBLICADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/133); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-021:002

Cancelamento de futuro retorna CANCELADO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Cancelamento válido do futuro retorna situação CANCELADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Estado persistido é CANCELADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/134); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-021:003

Vínculos futuros não operam

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Vínculo programado é preservado como CANCELADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Novo vínculo no contrato cancelado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/135); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-021:004

Origem e motivo permanecem

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Evento de cancelamento preserva motivo, autor, efeito e registro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Vínculo mantém contrato de origem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/136); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-021:005

Contrato FUTURO no registro não admite alcance de cancelamento após início, deixando intervalo operacional; ato documentado antes do início permanece permitido.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: FUTURO no registro com alcance após início retorna 400 sem novo evento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Alcance anterior é aceito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Quatro datas consultadas permanecem CANCELADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/137); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-022

A situação temporal/negocial, a habilitação administrativa e as pendências são eixos independentes do ciclo de vida do contrato.

### CON-022:001

VIGENTE e INATIVO coexistem

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Mesma posição é VIGENTE e INATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/138); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-022:002

Pendência não troca estado negocial

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Pendência de replay existe no contrato/origem corretos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Detalhe e posição conservam PUBLICADO e ATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/139); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-022:003

Consulta atual e temporal concordam

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Detalhe e posição para hoje concordam em estado/habilitação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Reativação histórica não muda habilitação da correção atual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/140); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-022:004

Somente evento próprio altera habilitação

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Campos estranhos em ADI não alteram estado/habilitação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Inativação/reativação próprias alteram eixo administrativo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Ato administrativo não cancela anulação por relação indevida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/141); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-023

Transferência excepcional de Gestão Local preserva contexto histórico anterior, exige validação/mapeamento explícito do contexto posterior e autorização administrativa específica para Gestor Local autorizado.

### CON-023:001

Sem autorização ou mapa completo bloqueia

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Sem concessão retorna 403 com estado preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Concessão sem mapa retorna 400 sem consumir uso — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Mesmo pedido completo passa e registra executor, data, destino e uso — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Comprovação integral do critério — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":3,"predicate":"Comprovação integral do critério","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/142); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-023:002

Tipo de contratação posterior deve ser válido na Gestão destino

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Tipo ausente/da origem é recusado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Tipo de destino explícito aparece desde o efeito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Legado sem Tipo permanece desconhecido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Ato de regularização não retroinventa dias anteriores — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 4: Tipo posterior incompatível exige tratamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 5: Comprovação integral do critério — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":5,"predicate":"Comprovação integral do critério","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/143); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-023:003

Composições e alocações mapeadas sem apagar anteriores

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Faixas anterior e posterior usam IDs corretos sem segundo vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Transferência derivada tem origem/hash — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Retificação atravessa a fronteira preservando conhecimento anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Sem fotografia original não se antecipa destino — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 4: Comprovação integral do critério — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":4,"predicate":"Comprovação integral do critério","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/144); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-023:004

Histórico de origem não muda após renomear cadastros

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Nomes fotografados não acompanham renomeação atual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Fotografia ausente permanece null — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Novo mapa captura somente destino documentado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 3: Configuração grade/município do destino é fotografada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 4: Comprovação integral do critério — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":4,"predicate":"Comprovação integral do critério","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/145); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-023:005

A política de matrícula resultante da transferência deve ser compatível com vínculos vigentes e programados atingidos; respeita override contratual false comprovado e matrícula válida já informada.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Vigente/programado sem matrícula obrigatória é recusado sem mutação/uso — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Override false documentado e matrícula informada permitem transferência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Dispensa posterior e restauração da herança são reavaliadas por período — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Regularização consome concessão só uma vez — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 4: Comprovação integral do critério — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":4,"predicate":"Comprovação integral do critério","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/146); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-023:006

Retificacao preserva transferencia anterior e reaplica ato posterior valido no corte efetivo e de conhecimento.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Retificação mantém duas transferências e marca anterior RETIFICADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Origem/destino mudam na nova data — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Corte de conhecimento antigo conserva efeito anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: ADI posterior independente é reaplicado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 4: Comprovação integral do critério — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":4,"predicate":"Comprovação integral do critério","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/147); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-023:007

Cancelamento futuro e explicito, preserva original e posteriores validos; posterior incompativel implica rollback.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Cancelamento futuro conserva original e ADI independente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Contexto retorna à origem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Tipo posterior incompatível bloqueia e mantém contagens/estado anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Comprovação integral do critério — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":3,"predicate":"Comprovação integral do critério","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/148); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-023:008

Transferencia usa vigencia reconstruida, incluindo prorrogacao e encurtamento programados.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Transferência aceita período prorrogado por ADI futuro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Rejeita dia posterior ao fim encurtado sem criar transferência/evento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Comprovação integral do critério — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":2,"predicate":"Comprovação integral do critério","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/149); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-023:009

Contexto posterior inclui todas composicoes e tentativas concorrentes nao publicam fatos conflitantes.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Composição futura da origem não mapeada bloqueia transferência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Duas tentativas concorrentes resultam em só um sucesso, uma transferência e uma cadeia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Comprovação integral do critério — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":2,"predicate":"Comprovação integral do critério","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/150); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-024

Anulação registra data do ato, data/hora de registro e data efetiva de alcance; o alcance segue o documento anulatório e não a data de digitação.

### CON-024:001

Ato posterior com alcance anterior mantém ambas as datas

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Evento conserva data efetiva DATA e data do ato DATA+3 separadamente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/151); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-024:002

Dia anterior ao alcance não fica anulado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Dia anterior ao alcance mantém PUBLICADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Vínculo limitado ao período anterior é aceito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/152); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-024:003

Dia do alcance fica ANULADO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Desde o alcance posição é ANULADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: GET corrente com relógio no alcance é ANULADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Vínculo novo desde alcance é recusado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/153); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-024:004

Registro possui Instant e autor

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: Evento contém registradoEm não vazio e ID exato do autor — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/154); métodos/recibos/assertions no mesmo registro e no CSV.

## CON-025

Contrato FUTURO publicado e posteriormente CANCELADO mantém número/ano reservado e pesquisável; somente rascunho nunca publicado pode liberar o identificador.

### CON-025:001

Criar identidade do cancelado=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: POST usando identidade do cancelado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/155); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-025:002

Excluir cancelado=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: DELETE do contrato cancelado retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/156); métodos/recibos/assertions no mesmo registro e no CSV.

### CON-025:003

História e motivo permanecem consultáveis

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M04","M16","M17"]. **ROP(s):** [].

- Predicado 0: GET de eventos retorna cancelamento com motivo, autor, efeito e registro preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/157); métodos/recibos/assertions no mesmo registro e no CSV.

## EMP-001

Contrato publicado deve possuir CNPJ, razão social e nome fantasia da empresa terceirizada.

### EMP-001:001

Omitir cada campo empresarial permite rascunho mas bloqueia publicação

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: Omissão isolada de CNPJ permite rascunho e impede publicação. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Omissão isolada de razão social permite rascunho e impede publicação. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Omissão isolada de nome fantasia permite rascunho e impede publicação. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/158); métodos/recibos/assertions no mesmo registro e no CSV.

### EMP-001:002

CNPJ inválido em tamanho rejeita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: CNPJ de tamanho inválido é rejeitado. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/159); métodos/recibos/assertions no mesmo registro e no CSV.

### EMP-001:003

Fotografia completa persiste

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: CNPJ, razão social e nome fantasia são recuperáveis após publicação. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: A fotografia original é preservada após alteração empresarial. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/160); métodos/recibos/assertions no mesmo registro e no CSV.

## EMP-002

Mudança de razão social/nome fantasia vale para a posição contratual a partir da data do evento e não reescreve outros contratos/históricos.

### EMP-002:001

Antes do evento mantém empresa original

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: Antes da data efetiva os três dados empresariais originais permanecem. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/161); métodos/recibos/assertions no mesmo registro e no CSV.

### EMP-002:002

Na data aplica novos nomes

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: Na data efetiva entram os novos nomes. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/162); métodos/recibos/assertions no mesmo registro e no CSV.

### EMP-002:003

Outro contrato não muda

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: Outro contrato conserva a empresa e o seu histórico. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/163); métodos/recibos/assertions no mesmo registro e no CSV.

### EMP-002:004

Fluxo de governança não troca fornecedor publicado livremente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: Fornecedor de contrato publicado não é trocado pelo fluxo comum de governança. — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/164); métodos/recibos/assertions no mesmo registro e no CSV.

## EMP-003

Troca real de empresa/CNPJ é fato contratual temporal; erro material de CNPJ é correção excepcional auditada, sem ser tratado como troca real.

### EMP-003:001

Troca real mantém empresa anterior no passado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: Troca real preserva a empresa anterior na posição passada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/165); métodos/recibos/assertions no mesmo registro e no CSV.

### EMP-003:002

Correção material produz CORRECAO_CNPJ

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: Correção material gera fato CORRECAO_CNPJ distinto de aditamento. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/166); métodos/recibos/assertions no mesmo registro e no CSV.

### EMP-003:003

Corte conhecidoAte preserva informação anterior

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: Corte conhecidoAte anterior conserva CNPJ então conhecido. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/167); métodos/recibos/assertions no mesmo registro e no CSV.

### EMP-003:004

Reenvio exato é idempotente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: Reenvio exato não duplica correção. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/168); métodos/recibos/assertions no mesmo registro e no CSV.

### EMP-003:005

Fornecedor escolhido deve corresponder ao CNPJ informado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M01","M04"]. **ROP(s):** [].

- Predicado 0: Fornecedor selecionado e CNPJ explícito devem coincidir. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/169); métodos/recibos/assertions no mesmo registro e no CSV.

## CAT-001

Tipo de contratação é cadastro prévio administrado no escopo da Gestão Local, não texto livre.

### CAT-001:001

Tipo é entidade prévia e selecionada por ID, sem aceitar texto livre como substituto.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Entidade prévia/ID demonstrados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Texto livre rejeitado no código — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Contraprova HTTP do texto livre não localizada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/170); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-001:002

Somente Gestor Local ativo administra Tipos da sua Gestão; ID de outra Gestão é recusado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Gestor Local da própria Gestão aceito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Outra Gestão404 e Nacional403 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Gestor/Gestão inativo não exercitado na rota de Tipos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/171); métodos/recibos/assertions no mesmo registro e no CSV.

## CAT-002

Cargo é cadastro próprio administrado pela Gestão Local.

### CAT-002:001

Cargo possui identidade/cadastro próprio com validação de nome.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Identidade própria de Cargo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Nome obrigatório e normalizado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/172); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-002:002

Escrita e unicidade nominal são delimitadas pela Gestão Local proprietária.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Escrita escopada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Unicidade nominal na Gestão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/173); métodos/recibos/assertions no mesmo registro e no CSV.

## CAT-003

Lotação, Turno, Cargo e demais catálogos usados historicamente permanecem consultáveis mesmo inativos.

### CAT-003:001

Inativar preserva a entidade e sua identidade; leitura histórica continua disponível.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Inativação mantémID/entidade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: História permanece acessível — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/174); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-003:002

Listagens/detalhes autorizados não excluem um catálogo somente por estar inativo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Listagem autorizada inclui inativo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Detalhe conserva identidade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/175); métodos/recibos/assertions no mesmo registro e no CSV.

## CAT-004

Consulta histórica mostra o nome/fotografia que valia no período, não reescreve o passado com o nome atual.

### CAT-004:001

Novos fatos de alocação/vínculo capturam rótulos e configurações da origem efetivamente escolhida.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Origem escolhida governa fotografia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Rótulos e configuração — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Distinção retroativo/atual/futuro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/176); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-004:002

Renomear Gestão/Lotação/Turno/Cargo/Tipo posteriormente não modifica as fotografias dos fatos anteriores.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Renomeações não alteram fotos de Gestão/Lotação/Turno/Cargo/Tipo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/177); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-004:003

Uma fotografia antiga nula/vazia continua desconhecida; nenhum nome/configuração é preenchido pelo mestre atual.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Nulo/vazio histórico permanece desconhecido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Mestre atual não preenche lacuna — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Nulo legítimo separado de ausência de prova — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/178); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-004:004

Documentos/relatórios/exportações consomem fotografias por fato; a prova transversal depende também dos lotes contratos e frequência.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Consumo sintético por relatório/frequência demonstrado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cobertura transversal dos fatos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Autenticidade documental dos contextos ausentes não suprida — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":2,"predicate":"Autenticidade documental dos contextos ausentes não suprida","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/179); métodos/recibos/assertions no mesmo registro e no CSV.

## CAT-005

Não inativar Cargo/Lotação/Turno com programação futura sem antes tratar a programação.

### CAT-005:001

Inativação de Cargo/Lotação/Turno é recusada enquanto composições, alocações, instrumentos ou transferências válidas cobrem programação futura.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Composição publicada protege catálogo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Alocação e transferência futuras protegem catálogo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Instrumentos futuros isolados não discriminados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/180); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-005:002

O tratamento da programação ocorre por comando de cancelamento/retificação preservando o fato original.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Tratamento por comando explícito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Original mantido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Dependência posterior incompatível recusada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/181); métodos/recibos/assertions no mesmo registro e no CSV.

## CAT-006

Cadastro inativo pode ser reativado, mantendo identidade histórica e auditoria.

### CAT-006:001

Reativação explícita preserva o ID do catálogo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Reativação explícita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: MesmoID — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/182); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-006:002

O ato grava estado anterior/novo, data efetiva, registro e responsável.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Estado anterior/novo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Data efetiva/registro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Responsável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/183); métodos/recibos/assertions no mesmo registro e no CSV.

## CAT-007

Cadastro hoje inativo pode ser usado em fato retroativo se estava válido na data efetiva.

### CAT-007:001

Fato retroativo aceita item hoje inativo quando a trilha comprova atividade na data efetiva.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Hoje inativo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Validade passada comprovada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Fato retroativo aceito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/184); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-007:002

Corte de conhecimento exclui transições registradas depois do instante consultado.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Fonte conhecida atéK — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Transição posterior excluída — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/185); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-007:003

Antes da primeira origem comprovada, ou sem fonte de origem, disponibilidade é desconhecida e o uso negocial dependente é recusado.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Disponibilidade desconhecida antes da origem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Flag atual/ativoAnterior insuficientes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Uso dependente recusado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/186); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-007:004

Primeira transição concorrente produz uma única origem e rejeita estado anterior obsoleto sem gravação parcial.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Uma origem sob concorrência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Estado anterior obsoleto409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Sem evento parcial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/187); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-007:005

Disponibilidade retroativa é documentada por comando canônico escopado com motivo, fonte, autorização e versão esperada, sem certificar período anterior à fonte.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Comando escopado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Motivo/evidência/autorização/versão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Sem certificar período anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/188); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-007:006

A inserção retroativa respeita a ordem efetiva e conserva reativação posterior na projeção atual.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Ordem efetiva — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Reativação posterior preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/189); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-007:007

Confirmação de ato retroativo exige prévia vigente de versão, trilha e dependências; reenvio da mesma intenção não duplica fato ou versão.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Prévia vincula versão/trilha/dependências — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Confirmação vigente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Idempotência da intenção — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/190); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-007:008

Ato incompatível com fatos posteriores ou com programação futura é recusado atomicamente, preservando as fontes existentes.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Recusa atômica sintética de ato incompatível — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Fontes posteriores/futuras preservadas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Gate documental de disponibilidade — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":2,"predicate":"Gate documental de disponibilidade","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/191); métodos/recibos/assertions no mesmo registro e no CSV.

## CAT-008

Cadastro atualmente inativo não pode ser usado em evento futuro sem reativação explícita.

### CAT-008:001

Um item atualmente inativo não autoriza programação futura.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Item inativo não autoriza futuro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Revalidação na confirmação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/192); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-008:002

Reativação explícita cria nova prova sem apagar o intervalo de inatividade.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Reativação cria nova prova — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Intervalo inativo permanece — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/193); métodos/recibos/assertions no mesmo registro e no CSV.

## CAT-009

Causas de término do vínculo devem distinguir desligamento individual, fim natural, rescisão antecipada, anulação e outras causas cadastradas.

### CAT-009:001

Catálogo classifica desligamento individual, fim de vigência, encerramento antecipado, anulação e outras causas separadamente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Cinco categorias distintas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Causa contratual exige ato correspondente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/194); métodos/recibos/assertions no mesmo registro e no CSV.

### CAT-009:002

Cada término usa causa da categoria e Gestão responsável na data do fato; descrição/categoria aplicadas ficam fotografadas.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M03","M11","M16"]. **ROP(s):** [].

- Predicado 0: Gestão competente na data do fato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: descrição e categoria aplicadas preservadas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/195); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-001

Composição é combinação explícita Lotação + Turno + Cargo + efetivo previsto; não usar produto cartesiano automático.

### CMP-001:001

Uma entrada cria exatamente uma composição

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: POST explícito cria composição — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Lista contém exatamente uma entrada após tentativa duplicada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/196); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-001:002

IDs obrigatórios validados

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Omissão isolada de lotacaoId, jornadaId ou cargoId retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Lista permanece vazia nas três invocações — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/197); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-001:003

Composição inexistente não é opção de vínculo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: ID inexistente de composição retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Contagens de pessoa/vínculo/alocação/reserva/história permanecem zero — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/198); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-002

Não pode haver combinação duplicada no mesmo recorte temporal.

### CMP-002:001

Criar duplicata sobreposta=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Repetir combinação no mesmo período retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/199); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-002:002

Lista mantém uma entrada

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Listagem após duplicação recusada possui uma entrada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/200); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-002:003

Itens repetidos ADI/REM rejeitados

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Itens repetidos em ADI e REM retornam 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Lista de instrumentos vazia e distribuição 25/75 preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/201); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-002:004

Transações concorrentes serializam pelo contrato

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: DELETE retido em transação real — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Publicação espera lock observado no MySQL — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Após remoção, publicação retorna 400 e não cria evento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/202); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-003

Soma do efetivo das composições deve ser igual ao efetivo total vigente do contrato.

### CMP-003:001

Publicação soma diferente=400

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Soma 9 com total 10 bloqueia publicação com 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/203); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-003:002

Novo total incompatível com deltas=400

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Novo total 101 com composição somando 100 retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Nenhum instrumento/evento parcial e total anterior 100 preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/204); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-003:003

Retroativo+posterior mantém total igual à soma

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Inserção retroativa reaplica delta posterior 100→120→130 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Total contratual coincide com composição — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Posterior incompatível não aplica delta parcial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/205); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-003:004

Remanejamento total constante

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: REM soma zero — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Distribuição e total 100 preservados nas versões e cancelamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/206); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-003:005

Somas reais devem coincidir com total sem overflow.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Publicação com soma matemática 6442450941 é recusada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: ADI que excederia representação é recusado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Nenhum evento/versão/instrumento parcial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/207); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-004

Previsto zero significa combinação indisponível para nova alocação, distinta de permitir excedente.

### CMP-004:001

Zero retorna permiteNovaAlocacao=false

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Composição com efetivo zero retorna permiteNovaAlocacao=false — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/208); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-004:002

Inclusão mesmo confirmada como excedente=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Zero recusa vínculo com confirmação e justificativa de excedente, com 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/209); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-004:003

Histórico da composição permanece

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Composição zerada mantém ID consultável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Nova versão positiva preserva consulta histórica com zero — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/210); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-005

Composição zerada que volta a ter efetivo retorna por nova versão temporal, preservando o período indisponível.

### CMP-005:001

Antes do retorno capacidade zero

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Depois da restauração, consulta anterior ao retorno ainda mostra zero — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/211); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-005:002

Na data nova capacidade positiva

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Consulta na data do retorno mostra capacidade 25 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/212); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-005:003

Nova alocação permitida somente a partir do retorno

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Mesma intenção de alocação é recusada no intervalo zero — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Alterar início para data do retorno permite 201 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/213); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-005:004

Instrumentos preservados

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Zeramento e retorno são registrados por ADIs separados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: História de versões é preservada nos cenários de retificação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Comprovação integral do critério — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/214); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-006

Não zerar/inativar composição enquanto houver vínculos/programações futuras não tratadas; a conclusão deve ser impedida.

### CMP-006:001

Prévia aponta programação impactada

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Prévia identifica nome da pessoa dependente de replay futuro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Volume conta 206 alocações e limita somente amostra — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/215); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-006:002

Confirmação=409 se dependência continua

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: ADI/REM e cancelamento que retirariam capacidade futura retornam 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/216); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-006:003

Contrato/instrumentos/pessoas não mudam no bloqueio

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Eventos anteriores preservados integralmente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Composições preservadas com novo corte de consulta — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Instrumento original VIGENTE e único — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Vínculo conserva composição, período e estado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/217); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-006:004

Após tratamento explícito pode confirmar

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Cancelar explicitamente programação elimina impacto — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Nova prévia seguida de confirmação registra redução e mantém vínculo CANCELADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/218); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-007

Alteração que remove/reduz combinação ocupada deve identificar impactos, não mover pessoas automaticamente, exigir tratamento explícito e preservar história.

### CMP-007:001

Impactos incluem ocupação e dependências futuras

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Prévia inclui ocupantes futuros e nome do dependente do replay — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Contagem completa permanece 206 mesmo com amostra limitada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/219); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-007:002

Prévia assinada muda quando dependência muda

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Nova programação depois da prévia muda token — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Token antigo é recusado e prévia nova informa impacto — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/220); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-007:003

Ocupação incompatível bloqueia mesmo confirmarImpacto=true

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: confirmarImpacto=true não autoriza retirar capacidade dependente por ADI, REM ou retificação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/221); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-007:004

Pessoa não muda automaticamente de combinação

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Vínculo consultado mantém composicaoInicialId após recusa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Período e estado preservados no cancelamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/222); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-008

Excedente de colaboradores acima do previsto é permitido com alerta; não é bloqueio rígido de vínculo.

### CMP-008:001

Sem confirmação inclusão excedente rejeita com aviso

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Sem consentimento excedente retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Resposta informa exigência de confirmação, justificativa, evidência e autorização — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/223); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-008:002

Com confirmação/justificativa Gestor autoriza

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Gestor confirma com justificativa e documentação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Inclusão excedente retorna 201 e evento próprio — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/224); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-008:003

Acima de 100% persiste

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Capacidade 1 admite ocupação 2 e segunda inclusão excedente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: IDs distintos e primeira ocupação excedente relida em JPA — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/225); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-009

Nova inclusão acima do previsto é autorizada pelo Gestor Local, com alerta forte, justificativa e auditoria.

### CMP-009:001

Perfil sem gestão não inclui

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Admin, Nacional, Estadual e Operacional não podem criar vínculo: 403 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Demais mutações de pessoa/vínculo/alocação também recusadas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/226); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-009:002

Justificativa obrigatória em excesso

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Confirmar sem justificativa ou com espaços retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Documentação isolada não substitui justificativa e contagens não mudam — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/227); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-009:003

Registro vincula autor e prova

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Evento e JPA conferem autor, username, data, justificativa, evidência e autorização — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Composição, capacidade, ocupação e hash do fato conferidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/228); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-009:004

Consulta excedente recupera justificativa

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Consulta de excedentes recupera justificativa original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Duas faixas restauradas recuperam prova/autorização correspondentes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/229); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-010

Não há teto técnico absoluto de excedente; sistema alerta e mantém identificação do excedente.

### CMP-010:001

Inclusões justificadas continuam possíveis acima da capacidade

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Duas inclusões justificadas sucessivas além de capacidade 1 são aceitas com IDs distintos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/230); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-010:002

Contagem mostra diferença real

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Capacidade 1 e ocupação 2 retornam excedente exatamente 1 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/231); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-010:003

Zero continua vedado por regra diferente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Consentimento de excedente não autoriza composição com capacidade zero — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/232); métodos/recibos/assertions no mesmo registro e no CSV.

## CMP-011

Enquanto existir excedente, ele deve permanecer monitorável, exibindo no mínimo composição, efetivo previsto, quantidade vinculada, excedente, desde quando existe e justificativas relacionadas; deixa de existir quando o quantitativo volta a caber no previsto ou quando alteração contratual válida absorve o excedente.

### CMP-011:001

Excedente 2-1 informa 1

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Previsto 1, vinculados 2 e excedente 1 conferidos na mesma composição — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/233); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-011:002

desdeQuando coincide com início do episódio

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: desdeQuandoExcedente coincide exatamente com início do episódio — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/234); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-011:003

Justificativa presente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Justificativa e autorização correspondente permanecem recuperáveis — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/235); métodos/recibos/assertions no mesmo registro e no CSV.

### CMP-011:004

Aumento a 2 zera excedente sem apagar histórico

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M05"]. **ROP(s):** [].

- Predicado 0: Aumento para 2 zera excedente na data posterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Consulta do episódio anterior mantém 2−1=1, início e justificativa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/236); métodos/recibos/assertions no mesmo registro e no CSV.

## RED-001

Reduções contratuais possuem limite máximo global de 25% e esse limite é cumulativo ao longo das reduções válidas.

### RED-001:001

100→90→75 permitido

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: 100→90→75 aceita dois ADIs — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Prévia conserva base 100/piso 75 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Detalhe final 75 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/237); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-001:002

Abaixo de75 sem exceção bloqueado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Redução comum para 74 retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Posição permanece 75 e só dois ADIs válidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/238); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-001:003

Replay posterior que viola piso gera conflito sem aplicar redução

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Replay que produziria 50 fica suspenso — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Total válido permanece 115 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Alerta de piso identifica posterior e pendência é criada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/239); métodos/recibos/assertions no mesmo registro e no CSV.

## RED-002

Limite de 25% é verificado sobre o total do contrato, não individualmente por composição.

### RED-002:001

Reduzir composição25→0 em total101 para76 permitido

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Composição 25→0 com total 101→76 é aceita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/240); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-002:002

Total ainda deve respeitar piso

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Depois do zeramento, tentativa de total 75 com base 101 retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/241); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-002:003

Ocupação deve ser tratada

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Dependência futura bloqueia redução — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Tratamento explícito da programação permite concluir — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/242); métodos/recibos/assertions no mesmo registro e no CSV.

## RED-003

Uma composição pode reduzir mais de 25%, inclusive a zero, se a redução global do contrato respeitar o limite e os impactos forem tratados.

### RED-003:001

Zero sem dependentes e piso respeitado é permitido

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Zero sem ocupação é aceito quando total 76 respeita piso da base 101 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/243); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-003:002

Zero com futuro dependente bloqueia

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: ADI/REM que zeram composição com futuro dependente retornam 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/244); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-003:003

Não mover pessoas automaticamente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Após recusa de replay, vínculo continua na composição C e histórico/distribuição permanecem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/245); métodos/recibos/assertions no mesmo registro e no CSV.

## RED-004

Aumento contratual não possui limite de 25%.

### RED-004:001

100→140 aceito

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Aumento 100→140 retorna 201 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/246); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-004:002

Aumento superior a25 não exige exceção de redução

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Aumento de 40% é aceito com payload comum sem concessão/flag de exceção — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/247); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-004:003

Soma da composição acompanha aumento

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Item de aumento conserva anterior 100, variação 40 e resultante 140 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Replay 120+10 confere composição e total 130 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/248); métodos/recibos/assertions no mesmo registro e no CSV.

## RED-005

Redução superior ao limite é bloqueada no fluxo comum; exceção só se houver regra negocial/jurídica expressamente aprovada.

### RED-005:001

74 para base100=409 comum

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Base 100 recusa resultado 74 com 409 no fluxo comum — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/249); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-005:002

Flag sozinha sem fundamento rejeita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Flag de exceção sem fundamento retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/250); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-005:003

Exceção completa passa com Gestão responsável

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Com fundamento, fonte, concessão e confirmação, Gestor responsável recebe 201 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/251); métodos/recibos/assertions no mesmo registro e no CSV.

## RED-006

O piso contratual é 75% do efetivo total originalmente publicado; aumentos posteriores não mudam a base, e apenas reduções válidas consomem o limite cumulativo.

### RED-006:001

100→140→75 permitido

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: 100→140→75 é aceito e posição final é 75 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/252); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-006:002

Base permanece100

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Depois do aumento, prévia informa efetivoOriginalPublicado 100 e piso 75 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/253); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-006:003

Retificar/cancelar remove efeito antigo do cálculo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Retificações 120→110→115 usam somente versão vigente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cancelar aumento exclui seu delta e suspende redução posterior incompatível — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/254); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-006:004

Replay revalida piso após retirada de aumento

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Retirar aumento revalida piso da redução posterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Flag não concedida não autoriza replay — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Total permanece 100 com pendência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/255); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-006:005

Piso original continua valido em toda faixa numerica admitida pelo modelo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Base 40000000 produz piso 30000000 e rejeita 28000000 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Integer.MAX_VALUE produz 1610612736 e borda inferior é rejeitada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Fórmula inteira é exata em todo domínio não negativo de int — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/256); métodos/recibos/assertions no mesmo registro e no CSV.

## RED-007

Quando o piso percentual resultar fracionário, o piso mínimo deve ser arredondado para cima para o próximo número inteiro.

### RED-007:001

Base101 exige76

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Base 101 exige e admite piso 76 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/257); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-007:002

Base100 exige75

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Base 100 tem piso 75 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/258); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-007:003

Resultado75 com base101 bloqueia

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Resultado 75 com base 101 retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/259); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-007:004

Arredondamento conserva precisao no maior inteiro admitido.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Maior int tem piso 1610612736 — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Exatamente o piso passa e piso−1 excede limite — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/260); métodos/recibos/assertions no mesmo registro e no CSV.

## RED-008

Exceção acima de 25% exige fundamento jurídico/negocial documentado, fonte comprobatória, permissão técnica excepcional para Gestor Local, prévia de impacto, justificativa e auditoria reforçada.

### RED-008:001

Falta fundamento=400

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Ausência de fundamento com flag de exceção retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/261); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-008:002

Falta concessão=403

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Fundamento sem concessão específica retorna 403 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/262); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-008:003

Sem confirmação=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Concessão com confirmarImpacto=false retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/263); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-008:004

Exceção documentada autorizada persiste

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Exceção completa retorna 201 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Resposta preserva flag, fundamento e autor — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/264); métodos/recibos/assertions no mesmo registro e no CSV.

### RED-008:005

Flag de exceção em redução normal não autoriza violação futura por replay

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Flag em redução normal sem concessão não permite que replay ultrapasse piso — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Total válido e pendência conferidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/265); métodos/recibos/assertions no mesmo registro e no CSV.

## ADI-001

Contrato pode possuir vários aditamentos sem apagar registros anteriores.

### ADI-001:001

Dois instrumentos aparecem na lista

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Listagem após aumento e redução contém dois instrumentos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/266); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-001:002

Reenvio não duplica

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Reenvio retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Lista mantém um só registro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/267); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-001:003

Retificação versão2 mantém versão1

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Versão 2/3 não elimina versão 1 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: IDs, conteúdo e situações da cadeia são conferidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/268); métodos/recibos/assertions no mesmo registro e no CSV.

## ADI-002

Aditamento possui identificação, datas/documento, data efetiva, justificativa, responsável e fonte comprobatória.

### ADI-002:001

Campos obrigatórios ausentes rejeitam

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Ausência de identificação, data do documento, data efetiva ou justificativa retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Nenhum instrumento/evento parcial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/269); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-002:002

Sem fonte=400

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Ausência de SEI/anexo bloqueia prévia com 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Reposição da fonte permite prosseguir — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/270); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-002:003

Autor/registro são servidor

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Sem informar autor/registro no payload, lista retorna executor e username corretos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Instant do registro dentro da janela do teste — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/271); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-002:004

Assinatura opcional preservada quando informada

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: dataAssinatura informada na primeira versão preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Ausência explícita nas versões seguintes permanece null — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/272); métodos/recibos/assertions no mesmo registro e no CSV.

## ADI-003

Aditamento quantitativo informa combinação afetada, quantidade anterior, variação e quantidade resultante.

### ADI-003:001

Aumento100→140 registra anterior100,variação40,resultante140

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Histórico do aumento confere anterior 100, variação 40 e resultante 140 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/273); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-003:002

Histórico mantém esses valores após novos fatos

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Após redução posterior a 120, fotografia original continua 100/+40/140 e mesmo ID — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/274); métodos/recibos/assertions no mesmo registro e no CSV.

## ADI-004

Aditamento pode aumentar ou reduzir, observadas as regras de redução e impacto.

### ADI-004:001

Aumento e redução válidos aceitos

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Aumentos e reduções válidos retornam 201 inclusive zeramento de composição sem dependentes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/275); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-004:002

Abaixo piso comum=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Resultado abaixo do piso nas bases 100 e 101 retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/276); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-004:003

Redução ocupada/futuro órfão=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Redução que deixaria programação órfã retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Prévia identifica a pessoa afetada no replay — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/277); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-004:004

Falha deixa toda transação intacta

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Recusa conserva eventos, composição projetada, instrumento vigente e composição do vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Comprovação integral do critério — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/278); métodos/recibos/assertions no mesmo registro e no CSV.

## ADI-005

Aditamento retroativo é permitido e sua data efetiva define quando passa a valer na reconstrução histórica.

### ADI-005:001

Fato antigo digitado depois produz efeito no dia certo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: ADI digitado depois produz 120 desde sua data anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Dia anterior continua 100 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Alterar catálogo atual não impede combinação histórica publicada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/279); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-005:002

Posterior válido continua aplicado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Delta posterior válido de +10 continua aplicado sobre 120, resultando em 130 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/280); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-005:003

Nova frequência após prévia invalida token

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Nova frequência persistida após prévia faz confirmação retroativa retornar 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/281); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-005:004

Versões anteriores preservadas

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Cadeia de três versões preserva conteúdos anteriores — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Fotografia legada anterior ao primeiro ADI permanece — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Cortes de conhecimento retornam 120/110/115 sem somar versões — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/282); métodos/recibos/assertions no mesmo registro e no CSV.

## ADI-006

Aditamento futuro pode ser cancelado antes de produzir efeito; não há exclusão física, e cancelamento tem motivo/autor/data.

### ADI-006:001

Cancelar futuro remove efeito

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Cancelamento futuro faz posição retornar descrição original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/283); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-006:002

Cancelar efetivo rejeita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: ADI já efetivo retorna 409 ao cancelar como programação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Mesmo ID continua VIGENTE — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/284); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-006:003

Original e cancelamento auditáveis

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Instrumento cancelado mantém ID — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Evento conserva motivo, autor, datas e registro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: CANCELA aponta exatamente o evento original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/285); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-006:004

Dependente futuro incompatível bloqueia cancelamento

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Cancelar ADI que sustenta vínculo futuro retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Instrumento, eventos, composição e vínculo preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/286); métodos/recibos/assertions no mesmo registro e no CSV.

## ADI-007

Retificação preserva original e possui data efetiva explícita, valor correto pretendido e variação/diferença aplicada.

### ADI-007:001

Versão2 com RETIFICA preserva versão1

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: RETIFICA cria novo ID e versão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Conteúdo original permanece como RETIFICADO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Valor quantitativo original não é reescrito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/287); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-007:002

Retificação subsequente conserva cadeia

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Retificar novamente conserva três IDs/versões e duas relações — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Valores e autoria de cada versão preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/288); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-007:003

Capacidade omitida na retificação é revalidada

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Retificar para somente descrição não pode remover capacidade necessária ao futuro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: 409 e cadeia/eventos preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/289); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-007:004

Resultado temporal usa versão vigente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Data anterior mantém base — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Data efetiva aplica só última versão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Cortes anteriores reapresentam versões conhecidas naquela época — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/290); métodos/recibos/assertions no mesmo registro e no CSV.

## ADI-008

Se retificação torna eventos posteriores incompatíveis, mostrar impacto e gerar pendência; posteriores válidos são preservados.

### ADI-008:001

Instrumento posterior continua na lista

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Instrumento posterior permanece com mesmos IDs, itens e situação mesmo ao ficar incompatível — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/291); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-008:002

Incompatibilidade gera pendência ligada ao evento

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Uma pendência ABERTA é ligada ao contrato e evento posterior exatos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/292); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-008:003

Não aplica delta incompatível parcialmente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Nenhum delta do REM incompatível é aplicado parcialmente: 20/75 e total 95 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Redução posterior violadora de piso também não produz total inválido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/293); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-008:004

Com dependente operacional impede operação

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Com vínculo futuro dependente, confirmação do antecedente incompatível retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/294); métodos/recibos/assertions no mesmo registro e no CSV.

## ADI-009

Dois aditamentos conflitantes na mesma data precisam de sequência negocial explícita; não usar ordem de gravação.

### ADI-009:001

Sem precedência colisão=409

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Duas descrições no mesmo dia sem relação explícita causam 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/295); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-009:002

SUCEDE válido decide valor

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Adicionar SUCEDE permite gravação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Posição usa descrição C e preserva razão social independente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/296); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-009:003

Sucessores bifurcados sem relação entre si bloqueiam

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: A→B e A→C conflitantes rejeitam C sem mutação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: A→B→C passa e aplica C — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/297); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-009:004

Recortes distintos coexistem

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: Descrição e razão social distintas coexistem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: MANTEM_COEXISTE não autoriza duas descrições conflitantes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Recusa preserva eventos e versões — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/298); métodos/recibos/assertions no mesmo registro e no CSV.

## ADI-010

Aditamento não altera diretamente publicação, inativação, reativação, encerramento, cancelamento ou anulação; esses estados possuem eventos/operações próprios.

### ADI-010:001

Campos estado/ativo no JSON não mudam estado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: JSON com estadoNegocial=ANULADO/ativo=false no ADI só altera descrição — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Contrato permanece PUBLICADO/ATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/299); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-010:002

RETIFICA/SUBSTITUI/CANCELA apontando evento de ciclo é recusado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: ADI com CANCELA/RETIFICA/SUBSTITUI de inativação é recusado — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Evento alvo continua VIGENTE e não há precedência nova — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/300); métodos/recibos/assertions no mesmo registro e no CSV.

### ADI-010:003

SUCEDE misto válido pode ordenar sem invalidar estado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M06"]. **ROP(s):** [].

- Predicado 0: SUCEDE entre ADI e inativação é aceito — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Inativação permanece VIGENTE — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Uma relação de ordem é criada — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/301); métodos/recibos/assertions no mesmo registro e no CSV.

## REM-001

Remanejamento é fato próprio, identificado, datado, justificado e auditado.

### REM-001:001

Registro contém identificação/data/justificativa/autor

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Lista confere data, justificativa, autor e registro das três versões — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Identificação REM-TRES-VERSOES preservada nas três — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/302); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-001:002

Itens guardam anterior/variação/resultante

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Item original confere anterior 25, variação −5 e resultante 20 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Lista das três versões conserva pares 25/75 e resultados 20/80,15/85,10/90 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/303); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-001:003

História usa tipo REMANEJAMENTO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: História contém tipo REMANEJAMENTO e eventoId positivo do instrumento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/304); métodos/recibos/assertions no mesmo registro e no CSV.

## REM-002

Remanejamento mantém o efetivo total: soma das variações efetivamente aplicadas deve ser zero.

### REM-002:001

-5/+5 preserva100

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: REM −5/+5 tem soma zero — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Resposta arquivada conserva resultados 20+80=100 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Versão vigente posterior também soma 100 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/305); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-002:002

-5/+4 rejeita400

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Prévia informa soma −1 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Confirmação de −5/+4 retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/306); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-002:003

Replay inválido não aplica metade da redistribuição

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Redistribuição incompatível é suspensa inteira — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Ambas composições e total preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Dependente operacional causa recusa sem mudança — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/307); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-002:004

Soma zero deve ser matematica, nao resultado do descarte de bits altos.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Soma matemática 4294967296 é recusada sem instrumento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Pares grandes que realmente zeram são aceitos pela regra — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Soma usa long — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/308); métodos/recibos/assertions no mesmo registro e no CSV.

## REM-003

Remanejamento só redistribui entre combinações válidas já existentes na data; criação de nova combinação é aditamento.

### REM-003:001

Composição inexistente=400

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Composição inexistente na redistribuição retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/309); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-003:002

Vigência fora do contrato rejeita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: REM antes do início ou após fim retorna 400 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Nenhum fato e duas composições originais preservadas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/310); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-003:003

Chave repetida rejeita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Repetição da mesma chave de composição nos itens retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/311); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-003:004

Nenhuma nova composição criada pelo REM

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Três versões de REM mantêm exatamente duas composições originais — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Rejeições por data/duplicidade não criam combinação nova — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/312); métodos/recibos/assertions no mesmo registro e no CSV.

## REM-004

Remanejamento pode ser retificado/versionado, preservando original e cadeia completa.

### REM-004:001

Versão2 mantém original RETIFICADO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Versão 1 e 2 permanecem RETIFICADO quando versão 3 é vigente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: IDs são distintos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/313); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-004:002

Repetir retificação conserva cadeia

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Segunda retificação conserva três registros e duas relações — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Quantitativos não acumulam versões substituídas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/314); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-004:003

Trocar destino não pode retirar capacidade de futuro ocupado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Trocar destino B por C retirando capacidade de vínculo futuro retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Evento e instrumento originais preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/315); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-004:004

Não invalidar fato de outro domínio

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: REM com CANCELA/RETIFICA/SUBSTITUI de inativação é recusado — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Alvo permanece VIGENTE — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/316); métodos/recibos/assertions no mesmo registro e no CSV.

## REM-005

Anexo de remanejamento é opcional; pode haver referência SEI para rastreabilidade.

### REM-005:001

REM sem anexo e sem SEI aceita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Primeira versão sem anexo e sem SEI retorna 201 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/317); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-005:002

Fonte informada permanece vinculada

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: SEI das versões permanece na lista — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Documento PUBLICADO ligado ao fato correto é relido em JPA e GET sem arquivo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/318); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-005:003

Ausência de fonte não causa erro

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: REM sem fonte opcional tem prévia 200, registro 201 e cadeia posterior acessível — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/319); métodos/recibos/assertions no mesmo registro e no CSV.

## REM-006

Eventos futuros de remanejamento podem permanecer historicamente efetivos mesmo durante inativação administrativa, sem liberar operação.

### REM-006:001

Composição histórica redistribuída

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: REM já programado redistribui capacidade durante intervalo INATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/320); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-006:002

Habilitação segue INATIVO

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Habilitação depois do REM continua INATIVO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/321); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-006:003

Nova operação no bloqueio rejeita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Na data do bloqueio, novo REM retorna 409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Inclusão de vínculo em período inativo também é recusada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/322); métodos/recibos/assertions no mesmo registro e no CSV.

## REM-007

Remanejamento futuro pode ser cancelado antes do efeito, preservando original, cancelamento, motivo, usuário, data/hora e fonte comprobatória quando aplicável; o cancelado não produz efeito.

### REM-007:001

Original futuro cancelável

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: REM futuro admite cancelamento 200 antes de produzir efeito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/323); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-007:002

Efetivo já iniciado rejeita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: REM já efetivo retorna 409 e permanece VIGENTE — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/324); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-007:003

Posição futura não aplica cancelado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Posição remove delta cancelado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Posterior válido aplica só seu próprio delta — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Sem posterior, distribuição original 25/75 é restaurada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/325); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-007:004

Com futuro dependente incompatível cancelação bloqueia

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Cancelamento que deixaria futuro dependente sem capacidade retorna 409 e conserva o vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/326); métodos/recibos/assertions no mesmo registro e no CSV.

### REM-007:005

Histórico conserva original+cancelamento

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M07"]. **ROP(s):** [].

- Predicado 0: Original e cancelamento permanecem na história com alvo, motivo, autor, datas e fonte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Posterior conserva fotografia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Rollback abortado desfaz evento/relação/pendência e nova tentativa confirma uma vez — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/327); métodos/recibos/assertions no mesmo registro e no CSV.

## DOC-001

Contrato original publicado deve ter pelo menos uma fonte comprobatória: anexo, número/processo SEI ou ambos.

### DOC-001:001

Publicação exige SEI, anexo ou ambos

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Publicação sem SEI e sem anexo é recusada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Publicação aceita somente anexo ou somente SEI. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/328); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-001:002

Rejeitar fonte vazia

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Fonte vazia bloqueia a publicação e não publica parcialmente. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/329); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-001:003

Anexo suficiente sem SEI e SEI suficiente sem arquivo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Anexo basta sem SEI. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: SEI basta sem arquivo. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/330); métodos/recibos/assertions no mesmo registro e no CSV.

## DOC-002

Aditamento deve ter pelo menos uma fonte comprobatória: anexo e/ou SEI.

### DOC-002:001

Aditamento deve ter fonte documental válida

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Aditamento com anexo sem SEI é aceito e publica a fonte vinculada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Aditamento com SEI é aceito e prévia sem fonte é recusada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: O comando de gravação sem qualquer fonte é recusado por ausência de fonte. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/331); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-002:002

Fonte acompanha fato e identidade da versão

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Fonte acompanha o fato e a identidade do instrumento criado. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/332); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-002:003

Sem fonte recusar gravação

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Prévia sem fonte é recusada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Gravação sem fonte é recusada sem criar fato. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/333); métodos/recibos/assertions no mesmo registro e no CSV.

## DOC-003

Remanejamento pode registrar SEI e mantém anexo opcional.

### DOC-003:001

Remanejamento aceita ausência de arquivo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Remanejamento sem arquivo é aceito. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/334); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-003:002

SEI opcional permanece vinculado ao fato

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: SEI opcional fica vinculado ao remanejamento correto. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/335); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-003:003

Cadeia de versões preservada

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Retificação/cancelamento conserva versões anteriores do remanejamento. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/336); métodos/recibos/assertions no mesmo registro e no CSV.

## DOC-004

Um mesmo processo SEI pode referenciar vários contratos; SEI não é identificador único do contrato.

### DOC-004:001

SEI não tem unicidade de contrato

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: SEI pode se repetir entre contratos. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/337); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-004:002

Dois contratos com mesma referência são publicados com identidades próprias

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Dois contratos publicados com o mesmo SEI mantêm identidades próprias. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/338); métodos/recibos/assertions no mesmo registro e no CSV.

## DOC-005

Número SEI incorreto em fato publicado é corrigido por nova versão/auditoria, preservando a referência anterior.

### DOC-005:001

Nova referência SEI cria versão

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Retificar SEI gera nova versão vinculada à anterior. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/339); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-005:002

Referência anterior permanece consultável

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Referência SEI anterior segue consultável. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/340); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-005:003

Motivo e autor são registrados

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Retificação registra motivo e autor atribuíveis. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/341); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-005:004

Reenvio com intenção divergente não muda a cadeia

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Reuso da chave para documento alvo diferente é rejeitado. — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: A cadeia completa permanece exatamente igual após a intenção divergente. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/342); métodos/recibos/assertions no mesmo registro e no CSV.

## DOC-006

Contrato publicado inicialmente apenas com SEI pode receber anexo posteriormente, sem substituir silenciosamente o SEI.

### DOC-006:001

Anexo pode chegar após publicação com SEI

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Contrato publicado somente com SEI aceita anexo posterior. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/343); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-006:002

A nova versão preserva referência SEI e vínculo anterior

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Nova versão preserva SEI e elo anterior. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/344); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-006:003

Download confere checksum do arquivo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Download entrega bytes esperados e mantém checksum documental. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Arquivo adulterado é bloqueado pelo controle de checksum. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/345); métodos/recibos/assertions no mesmo registro e no CSV.

## DOC-007

Arquivo associado a fato publicado e enviado errado não é apagado fisicamente; é marcado inválido/substituído e permanece auditável.

### DOC-007:001

Documento de fato publicado não é excluído fisicamente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Exclusão física de documento publicado é recusada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: A invalidação conserva o arquivo físico associado ao fato publicado. — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: ["H_UNIT_DOC revalidated by exact source bytes and full helper read; repository mocks/maps + physical file, no MySQL persistence claim.","H_UNIT_DOC: serviço real/repositórios controlados/arquivo físico; não persistênciaMySQL."].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/346); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-007:002

Invalidação/substituição preserva metadados e bytes

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Substituição/invalidação mantém os metadados e histórico. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cancelamento de ocorrência conserva bytes e metadados após commit. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Bytes anteriores são preservados após invalidação/substituição documental. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/347); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-007:003

Cancelar ocorrência preserva fonte e permite auditoria autorizada

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Cancelamento lógico conserva fonte e bytes. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Auditoria continua sujeita à autorização histórica. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/348); métodos/recibos/assertions no mesmo registro e no CSV.

## DOC-008

Em rascunho nunca publicado, anexos/dependências exclusivas podem ser removidos junto com o descarte, salvo valor auditável externo.

### DOC-008:001

Rascunho nunca publicado admite descarte transacional

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Rascunho exclusivo nunca publicado permite descarte transacional. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/349); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-008:002

Preservar dependência que possui valor documental externo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Dependência em rascunho com valor documental externo é preservada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/350); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-008:003

Exclusão física só após commit

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Arquivo não é apagado antes do commit. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Rollback conserva metadado e bytes. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/351); métodos/recibos/assertions no mesmo registro e no CSV.

## DOC-009

Documentos anexados possuem classificação de acesso própria, distinguindo no mínimo documento negocial e documento pessoal/restrito.

### DOC-009:001

Cada anexo possui classificação própria

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Classificação documental é explícita e validada por anexo. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/352); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-009:002

NEGOCIAL e PESSOAL_RESTRITO têm políticas diferentes

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: NEGOCIAL e PESSOAL_RESTRITO aplicam políticas distintas. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/353); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-009:003

Metadados também respeitam classificação

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Consulta minimizada não revela metadados de anexo pessoal. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/354); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-009:004

Classificação explícita por anexo; legado sem classificação tratado como pessoal/restrito; hash SHA-256 dos novos bytes; metadados pessoais omitidos de consultas minimizadas.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Classificação explícita por anexo é validada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Legado sem classificação recebe tratamento pessoal/restrito. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Novos bytes possuem SHA-256 correto. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["The response SHA predicate requires a source-qualified fresh POST receipt with matching run timestamp; XML PASS alone or appended prior entries are insufficient."].
- Predicado 3: Metadados pessoais são omitidos em consulta minimizada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/355); métodos/recibos/assertions no mesmo registro e no CSV.

## DOC-010

Documento Pessoal/Restrito possui acesso por necessidade e permissão específica conforme perfil; Presidente/Operacional não acessam por padrão, Administrador técnico somente em suporte excepcional autorizado, e todo acesso é auditado.

### DOC-010:001

Conteúdo restrito exige necessidade e perfil/concessão

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Documento pessoal exige perfil competente ou concessão específica e necessidade declarada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Administrador técnico depende de suporte excepcional autorizado. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/356); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-010:002

Operacional não lê documento pessoal

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Operacional não recebe documento pessoal mesmo no escopo correto. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/357); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-010:003

Acesso permitido é auditado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Acesso autorizado gera trilha atribuível. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Negativa posterior não modifica o registro autorizado anterior. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/358); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-010:004

Revogação/expiração aplica-se a cada leitura

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Revogação é aplicada na leitura seguinte pela mesma sessão/URL. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Expiração é aplicada sem editar/revogar a concessão. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/359); métodos/recibos/assertions no mesmo registro e no CSV.

### DOC-010:005

Download revalida todos os dias do contexto do fato, perfil, concessão vigente e finalidade; registra uso auditável. Revogação não é contornada por cache. Operacional não recebe documento pessoal; Presidente somente mediante permissão explícita.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M17"]. **ROP(s):** [].

- Predicado 0: Download exige escopo em todos os dias do contexto indivisível. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Perfil e concessão vigente são aplicados e usos são auditáveis. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Operacional não recebe pessoal e Presidente exige permissão explícita. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Revogação não reutiliza resposta anterior na mesma sessão/URL. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 4: Finalidade é revalidada especificamente em cada download. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/360); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-001

Nome e CPF são obrigatórios.

### COL-001:001

Cadastro integrado sem nome/CPF é recusado sem persistência parcial.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Nome/CPF obrigatórios — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Recusa de ausente/nulo/branco — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Sem persistência parcial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/361); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-001:002

Nome não vazio é normalizado e preservado na identidade canônica.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Nome não vazio — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Normalização — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Identidade canônica preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/362); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-002

RG, CTPS, NIT/PIS, data de nascimento e e-mail são opcionais.

### COL-002:001

RG/CTPS/NIT-PIS/data de nascimento/e-mail podem ser omitidos.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: RG/CTPS/NIT-PIS/nascimento/email opcionais — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/363); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-002:002

Quando informados, os opcionais são persistidos no cadastro de Pessoa e expostos conforme perfil.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Valores exatos persistidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Exposição segundo perfil — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/364); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-003

Endereço é mantido e opcional.

### COL-003:001

Ausência de endereço é aceita.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Endereço omitido aceito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/365); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-003:002

Endereço informado é mantido no cadastro; remoção exige ação explícita.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Endereço informado persistido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Omissão preserva — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Remoção explícita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/366); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-004

Dados bancários foram removidos do cadastro deste fluxo.

### COL-004:001

Cadastro canônico de Pessoa e seus contratos de entrada/saída não incluem dados bancários.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Pessoa sem dados bancários — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: DTOs de entrada e saída sem esses campos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/367); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-005

CPF é válido, único globalmente, persistido sem pontuação e mascarado apenas na interface.

### COL-005:001

CPF valida tamanho e dígitos verificadores e rejeita sequências repetidas.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Comprimento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Dígitos verificadores — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Repetição uniforme inválida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/368); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-005:002

CPF é normalizado sem pontuação antes da persistência.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Normalização sem pontuação antes da persistência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/369); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-005:003

Unicidade global é garantida inclusive em cadastros concorrentes no MySQL.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Unicidade global — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Cadastros concorrentes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Uma identidade confirmada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/370); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-006

CPF não é editável no fluxo comum. Correção excepcional de erro material comprovado é executada pelo Gestor Local e deve registrar motivo, evidência, CPF anterior, CPF novo, responsável, autorização, data/hora e impactos encontrados, além de validar o novo CPF.

### COL-006:001

Atualização comum de opcionais não permite alterar CPF.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Atualização comum não mudaCPF — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/371); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-006:002

Correção excepcional exige Gestor Local autorizado no escopo, motivo, evidência, autorização e impactos declarados.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Gestor Local/escopo demonstrados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Campos do ato persistidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Obrigatoriedade individual de motivo/evidência/autorização não totalmente contrastada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/372); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-006:003

Novo CPF é validado; ato preserva anterior/novo/responsável/data de registro sem fundir identidade.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: CPF novo validado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Valores anterior/novo e responsável/registro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Sem fusão de identidade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/373); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-006:004

CPF anterior deixa de ser identificador ativo, podendo ser usado por titular distinto após validação sem apropriar a história da Pessoa original.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: CPF antigo liberado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Titular distinto recebe outraPessoa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: História da origem não apropriada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/374); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-007

Se o CPF corrigido já pertence a outra pessoa, bloquear correção automática e tratar duplicidade/identidade excepcionalmente; nunca fundir silenciosamente.

### COL-007:001

Correção para CPF ativo de outra Pessoa é recusada atomicamente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: CPF ativo alheio409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Recusa atômica — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Identidades distintas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/375); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-007:002

Projeção legada coincidente por CPF não é adotada sem associação canônica explícita.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: CPF igual não autoriza adotarEmpregado legado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: História preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/376); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-009

Nome não é editável no fluxo normal; erro material ou mudança legal pode ser corrigido excepcionalmente e auditado, sem criar nova pessoa.

### COL-009:001

Nome não pode ser alterado pelo fluxo comum de opcionais.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Fluxo comum não altera nome — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/377); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-009:002

Correção excepcional auditada mantém o mesmo Pessoa.id e fotografa anterior/novo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Correção excepcional auditada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: MesmoPessoa.id — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Fotografia anterior/novo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/378); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-010

Cadastro atual mostra nome atual; históricos/documentos preservam fotografia do nome da época.

### COL-010:001

Cadastro atual e projeção operacional mostram o nome corrigido.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Cadastro atual mostra nome corrigido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Projeção operacional acompanha — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/379); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-010:002

Novos fatos preservam o nome fotografado e a correção atual não muda eventos/documentos anteriores.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Novos eventos fotografam nome — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Correção não reescreve eventos/histórico — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Documento previamente emitido não confrontado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/380); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-010:003

Consulta temporal separa nomeAtual de nomeFotografia e aplica os cortes efetivo e de conhecimento.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: NomeAtual separado deNomeFotografia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: CortesD/K — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/381); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-010:004

Registro antigo sem fotografia nominal permanece desconhecido; não se alega recuperação de dado ausente.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Fotografia nominal ausente fica desconhecida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Não recuperar dado inexistente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Fonte autêntica permanece pendente — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":2,"predicate":"Fonte autêntica permanece pendente","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/382); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-011

Na recontratação, campos opcionais omitidos significam manter dados atuais; limpeza exige ação explícita.

### COL-011:001

Recontratação com pessoaId preserva campos opcionais omitidos.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Recontratação porPessoaId — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Opcionais omitidos preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/383); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-011:002

Limpeza usa indicadores explícitos, mantendo os demais opcionais.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Limpeza por indicador — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Demais valores mantidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/384); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-012

Dados pessoais atuais e fotografia histórica são separados: cadastro mostra valor atual, histórico negocial usa fotografia registrada na época.

### COL-012:001

Dados atuais pertencem à Pessoa canônica; histórico usa dados fotografados no fato.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Cadastro atual pertence àPessoa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Histórico deriva da fonte do fato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/385); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-012:002

Ausência de fotografia é distinta de valor atual e nunca é completada implicitamente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Ausência distinta de valor atual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Sem preenchimento implícito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/386); métodos/recibos/assertions no mesmo registro e no CSV.

## COL-013

Pessoa é única por CPF; recontratação reutiliza a mesma pessoa e cria novo vínculo, não nova pessoa.

### COL-013:001

Uma Pessoa é identificada globalmente por CPF atual e ID estável.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Pessoa global porCPF atual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: ID estável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/387); métodos/recibos/assertions no mesmo registro e no CSV.

### COL-013:002

Recontratação reutiliza essa Pessoa e cria outro Vínculo, sem duplicar identidade ou projeção.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M08","M09","M16"]. **ROP(s):** [].

- Predicado 0: Recontratação reutilizaPessoa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: NovoVínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Sem duplicar projeção — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/388); métodos/recibos/assertions no mesmo registro e no CSV.

## MAT-001

Matrícula pertence ao Vínculo Contratual, não ao cadastro permanente da Pessoa.

### MAT-001:001

Matrícula reside no Vínculo e nas reservas por Contrato, não no cadastro permanente da Pessoa.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Matrícula noVínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Reserva vinculada aoContrato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Ausente daPessoa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/389); métodos/recibos/assertions no mesmo registro e no CSV.

### MAT-001:002

Consulta temporal aplica cada alteração de matrícula na data efetiva.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Alteração aplica na data efetiva — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Consulta anterior mantém valor antigo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/390); métodos/recibos/assertions no mesmo registro e no CSV.

## MAT-002

Matrícula é opcional no vínculo e única dentro do contrato.

### MAT-002:001

Matrícula é opcional na ausência de política obrigatória.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Matrícula opcional sem política obrigatória — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/391); métodos/recibos/assertions no mesmo registro e no CSV.

### MAT-002:002

Valor normalizado não pode ser atribuído a outro vínculo do mesmo Contrato.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Normalização — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Unicidade porContrato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: OutroContrato permitido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/392); métodos/recibos/assertions no mesmo registro e no CSV.

## MAT-003

Matrícula permanece reservada historicamente no contrato mesmo após desligamento, alteração ou programação cancelada.

### MAT-003:001

Desligamento mantém a reserva histórica.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Reserva sobrevive ao desligamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/393); métodos/recibos/assertions no mesmo registro e no CSV.

### MAT-003:002

Alteração mantém também a reserva anterior.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Alteração mantém reserva anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Reserva nova também permanente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/394); métodos/recibos/assertions no mesmo registro e no CSV.

### MAT-003:003

Cancelamento de programação não libera a matrícula.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Cancelamento de programação não libera matrícula — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/395); métodos/recibos/assertions no mesmo registro e no CSV.

## MAT-004

Nem a própria pessoa reutiliza matrícula antiga em recontratação; novo vínculo recebe nova matrícula.

### MAT-004:001

Novo vínculo da mesma Pessoa não pode reutilizar matrícula já reservada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: MesmaPessoa em novoVínculo não reutiliza reserva — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/396); métodos/recibos/assertions no mesmo registro e no CSV.

### MAT-004:002

Matrícula diferente permite recontratação válida.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Matrícula distinta permite nova contratação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/397); métodos/recibos/assertions no mesmo registro e no CSV.

## MAT-005

Matrícula é opcional por padrão, mas Tipo de Contratação/Contrato pode torná-la obrigatória; quando obrigatória, vínculo não pode ser ativado/publicado sem matrícula válida.

### MAT-005:001

Política padrão é opcional; contrato pode fixar regra ou herdar a do Tipo fotografado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Padrão opcional — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Override contratual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Herança doTipo fotografado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/398); métodos/recibos/assertions no mesmo registro e no CSV.

### MAT-005:002

Inclusão/ativação e toda faixa atingida por política obrigatória exigem matrícula válida.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Inclusão exige matrícula sob política — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Todas as faixas atingidas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Recusa atômica e controles positivos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/399); métodos/recibos/assertions no mesmo registro e no CSV.

### MAT-005:003

Mudança futura/transferência revalida vínculos existentes e programados nas fronteiras pertinentes.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Mudança futura — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Transferência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Vínculos existentes/programados nas fronteiras — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/400); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-001

Pessoa + primeiro vínculo + alocação são cadastrados em operação integrada/transacional.

### VIN-001:001

Pessoa nova, primeiro Vínculo e Alocação inicial são gravados na mesma transação.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Pessoa nova — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: primeiro vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: alocação inicial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: mesma transação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/401); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-001:002

Falha posterior reverte todos os componentes e permite nova tentativa íntegra.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Falha posterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: rollback de todos os componentes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: repetição íntegra — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/402); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-002

Antiga data de admissão é substituída por início efetivo do vínculo.

### VIN-002:001

O fato de início usa dataInicio efetiva do Vínculo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Início efetivo do vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: independência do instante de cadastro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/403); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-002:002

Pesquisa de início não usa mudança de alocação nem dataAdmissao da projeção como novo vínculo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Mudança de alocação não é novo vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: dataAdmissao da projeção não governa pesquisa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/404); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-003

Não pode haver sobreposição de vínculos da mesma pessoa, globalmente, inclusive entre contratos/Gestões diferentes.

### VIN-003:001

Sobreposição é verificada globalmente por Pessoa, inclusive contratos/Gestões distintos.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Unicidade global por Pessoa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: contratos e Gestões diferentes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: limites inclusivos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/405); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-003:002

Locks e revalidação impedem sobreposição em cadastro/retificações concorrentes.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Concorrência de cadastro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: retificação e cancelamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: revalidação global sob locks — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/406); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-004

Troca direta entre contratos é permitida em dias consecutivos: A termina num dia e B começa no seguinte, sem dia de intervalo.

### VIN-004:001

Fim inclusivo em D permite vínculo seguinte em D+1.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Último dia inclusivo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: contratação consecutiva em D+1 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/407); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-004:002

Fim em D não permite outro vínculo iniciar em D.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Último dia ocupado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: outro início em D é rejeitado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/408); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-005

Vínculo futuro pode ser cadastrado como PROGRAMADO e não opera antes da data efetiva.

### VIN-005:001

Vínculo com início futuro retorna PROGRAMADO antes do início.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Início futuro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: estado PROGRAMADO antes do início — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/409); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-005:002

Programação não torna o colaborador operacionalmente ativo antes da data efetiva.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Programação não ativa projeção antes de D — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: operação só a partir do início — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/410); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-006

Programação futura pode ser cancelada antes de começar sem ser tratada como desligamento de vínculo efetivamente ocorrido.

### VIN-006:001

Programação pode ser cancelada antes do início.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Cancelamento por comando próprio — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: antes do início — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/411); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-006:002

Cancelamento não grava desligamento individual de relação nunca efetivada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Relação nunca efetivada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: ausência de causa e desligamento individual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/412); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-007

Programação futura cancelada/retificada preserva cadeia completa original → cancelamento/retificação → programação vigente.

### VIN-007:001

Cancelamento/retificação preservam evento original e relacionamento de precedência.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Evento original preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: cancelamento e retificação acrescentam relação de precedência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/413); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-007:002

Corte de conhecimento anterior recupera a programação então conhecida.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Corte de conhecimento anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: programação antes da correção — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/414); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-008

Ao fim natural da vigência, vínculos abertos ficam operacionalmente inativos a partir do dia seguinte e geram pendência administrativa.

### VIN-008:001

Vínculo aberto perde operação no dia seguinte ao fim natural do Contrato.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Vínculo ainda aberto — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: perda operacional no primeiro dia após fim contratual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/415); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-008:002

Pendência administrativa identifica o vínculo ainda aberto sem encerrá-lo por rotina técnica.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Pendência do vínculo aberto — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: fim temporal efetivo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: nenhuma terminação técnica — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/416); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-009

Ao tratar a pendência de fim de vigência, a data final do vínculo é a data final do contrato, salvo desligamento real anterior.

### VIN-009:001

Tratamento da pendência usa o fim efetivo do Contrato.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Tratamento administrativo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: fim efetivo após prorrogação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: recusa prematura — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/417); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-009:002

Desligamento real anterior é preservado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Desligamento real anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: fim e causa conservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/418); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-010

Fim natural do contrato usa motivo próprio de catálogo, como 'Fim da vigência do contrato', administrado pelo Gestor Local.

### VIN-010:001

Fim natural usa categoria FIM_VIGENCIA_CONTRATO do catálogo da Gestão competente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Categoria FIM_VIGENCIA_CONTRATO — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Gestão competente na data — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/419); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-010:002

Gestor Local administra o motivo, e o ato preserva descrição/categoria aplicadas.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Gestor Local administra catálogo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: descrição e categoria aplicadas são fotografadas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/420); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-011

Data de registro administrativo pode ser posterior, mas data efetiva do encerramento não ultrapassa o fim da vigência.

### VIN-011:001

Data efetiva de encerramento natural não ultrapassa fim da vigência.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Fim efetivo natural — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: limite da vigência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/421); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-011:002

Registro administrativo posterior conserva Instant próprio e responsável.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Registro administrativo posterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Instant próprio — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: responsável preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/422); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-012

Prorrogação retroativa posterior não reabre automaticamente vínculos já encerrados no fim antigo; exige impacto e eventual retificação explícita.

### VIN-012:001

Prorrogação posterior não reabre vínculo já encerrado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Prorrogação posterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: vínculo encerrado não reabre — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/423); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-012:002

Reabertura/retificação requer comando explícito com impacto e revalidações.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Retificação explícita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: prévia de impacto — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: revalidações — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/424); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-013

Inativação administrativa não encerra vínculos; reativação dentro da mesma vigência permite retomada dos vínculos ainda válidos sem novo vínculo.

### VIN-013:001

Inativação administrativa não altera término do Vínculo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Inativação administrativa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: término do vínculo preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/425); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-013:002

Reativação dentro da mesma vigência retoma somente vínculos temporalmente válidos, sem criar outro vínculo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Retomar só vínculo temporalmente válido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: mesma vigência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: não criar vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/426); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-014

Data inicial do vínculo pode ser retificada por versão auditada, justificativa e prévia de impacto, revalidando sobreposição, alocação, frequência, matrícula, relatórios e eventos posteriores.

### VIN-014:001

Retificação de início exige justificativa/evidência/autorização e prévia confirmada válida.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Justificativa obrigatória — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: evidência obrigatória — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: autorização obrigatória — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: prévia confirmada válida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/427); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-014:002

Revalida sobreposição global, matrícula e alocação inclusive eventos posteriores.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Sobreposição global — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: política de matrícula no período — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: faixas e programações posteriores — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/428); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-014:003

Frequências e relatórios atingidos são reavaliados, com pendências e invalidação quando cabíveis.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Frequências atingidas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: relatórios atingidos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: pendências e invalidação aplicáveis — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/429); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-014:004

Evento original e versões posteriores são preservados e reconstruíveis por conhecimento.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Original preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: versões posteriores — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: reconstrução por conhecimento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/430); métodos/recibos/assertions no mesmo registro e no CSV.

## VIN-015

Encerramento antecipado/rescisão encerra vínculos abertos na mesma data efetiva, salvo desligamento anterior, usando causa própria.

### VIN-015:001

Encerramento antecipado encerra vínculos abertos na mesma data efetiva.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Encerramento antecipado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: vínculos abertos terminam na data do ato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/431); métodos/recibos/assertions no mesmo registro e no CSV.

### VIN-015:002

Término individual anterior é preservado; causa específica é fotografada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09"]. **ROP(s):** [].

- Predicado 0: Término individual prévio — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: causa específica da rescisão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/432); métodos/recibos/assertions no mesmo registro e no CSV.

## ALO-001

Alocação é faixa temporal Lotação + Turno + Cargo dentro do vínculo.

### ALO-001:001

Cada Alocação relaciona Vínculo, Lotação, Turno e Cargo em faixa temporal.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Lotação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: Turno/Jornada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 3: Cargo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 4: faixa temporal — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/433); métodos/recibos/assertions no mesmo registro e no CSV.

### ALO-001:002

Somente combinação presente na composição válida no recorte é selecionável.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Combinação contratual válida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: recorte da faixa proposta — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/434); métodos/recibos/assertions no mesmo registro e no CSV.

## ALO-002

Mudança de Lotação/Turno/Cargo cria novo período de alocação; não é desligamento/recontratação.

### ALO-002:001

Mudança de Lotação/Turno/Cargo cria faixa sucessora e encerra anterior em D-1.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Mudança de dimensões — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: sucessora em D — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: anterior termina em D-1 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/435); métodos/recibos/assertions no mesmo registro e no CSV.

### ALO-002:002

A Pessoa e o Vínculo permanecem os mesmos; não é novo desligamento/recontratação.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Mesma Pessoa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: mesmo Vínculo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: ausência de nova contratação ou desligamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/436); métodos/recibos/assertions no mesmo registro e no CSV.

## ALO-003

Alteração retroativa de alocação é permitida, preservando fatos posteriores válidos e apontando incompatibilidades.

### ALO-003:001

Mudança retroativa altera apenas o trecho alcançado e preserva eventos posteriores válidos.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Trecho retroativo alcançado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: decisão posterior independente preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/437); métodos/recibos/assertions no mesmo registro e no CSV.

### ALO-003:002

Incompatibilidades posteriores são apresentadas e impedem confirmação sem tratamento.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Incompatibilidade posterior apresentada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: confirmação impedida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/438); métodos/recibos/assertions no mesmo registro e no CSV.

## ALO-004

Existe no máximo uma alocação efetiva por colaborador por dia; segunda informação do mesmo dia retifica a anterior.

### ALO-004:001

Há no máximo uma faixa REGISTRADA por Pessoa/dia.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: No máximo uma REGISTRADA por Pessoa/dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: versões não operacionais preservadas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/439); métodos/recibos/assertions no mesmo registro e no CSV.

### ALO-004:002

Nova informação no mesmo início substitui/retifica a faixa anterior preservada, sem sobreposição.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Mesmo início — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: retificação/substituição — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: preservação e ausência de sobreposição — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/440); métodos/recibos/assertions no mesmo registro e no CSV.

## ALO-005

Mudança de alocação e desligamento na mesma data só podem coexistir se a precedência tornar o último dia inequívoco; caso contrário, corrigir/retificar.

### ALO-005:001

Mudança e desligamento no mesmo dia exigem precedência explícita do último dia.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Alocação e término em D — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: precedência explícita do último dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/441); métodos/recibos/assertions no mesmo registro e no CSV.

### ALO-005:002

A regra vale nas duas ordens de lançamento e também nas retificações.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Duas ordens de lançamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: retificações — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: sucessora ainda inexistente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/442); métodos/recibos/assertions no mesmo registro e no CSV.

## ALO-006

Jornada que cruza meia-noite pertence à data de início e usa a alocação válida nessa data.

### ALO-006:001

Jornada que atravessa meia-noite pertence à data de início.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Jornada atravessa meia-noite — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: fato pertence ao dia inicial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/443); métodos/recibos/assertions no mesmo registro e no CSV.

### ALO-006:002

A composição/alocação usada é a válida nessa data, sem deslocar o fato ao dia seguinte.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Composição e alocação de D — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: não deslocar fato para D+1 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/444); métodos/recibos/assertions no mesmo registro e no CSV.

## ALO-007

Todo dia de vínculo operacional efetivo deve possuir exatamente uma alocação Lotação + Turno + Cargo válida.

### ALO-007:001

Toda faixa operacional de Vínculo fica coberta sem lacunas por exatamente uma Alocação válida.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Cobertura completa da operação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: uma faixa válida por dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: sem lacunas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/445); métodos/recibos/assertions no mesmo registro e no CSV.

### ALO-007:002

Mudanças de início/fim, prorrogação e programação posterior revalidam cobertura/composição nas fronteiras.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Início/fim — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: prorrogação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: programação posterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 3: fronteiras de composição/capacidade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/446); métodos/recibos/assertions no mesmo registro e no CSV.

### ALO-007:003

Reprojeção após retificar/cancelar transferência preserva decisões independentes válidas e rejeita incompatíveis atomicamente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Reprojeção na retificação/cancelamento de transferência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: decisões independentes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: recusa atômica — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/447); métodos/recibos/assertions no mesmo registro e no CSV.

## ALO-008

Inativação administrativa não encerra Alocação; na reativação, a mesma Alocação volta a operar se ainda válida, não sucedida e com composição válida.

### ALO-008:001

Inativação administrativa conserva a faixa de Alocação.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Inativação administrativa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: faixa de alocação conservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/448); métodos/recibos/assertions no mesmo registro e no CSV.

### ALO-008:002

Retomada exige faixa ainda válida, não sucedida e composição válida; não cria nova faixa só por reativar.

**Status:** PARCIAL. **Técnico:** PARCIAL.

**Módulo(s):** ["M10"]. **ROP(s):** [].

- Predicado 0: Faixa ainda válida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: não sucedida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: composição válida — **PARCIAL**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: ["The occupied zero-capacity preparation never reached reactivation. Positive composition paths cannot substitute that negative target; no invalid state was fabricated.","New opted-in negative test really FAILed at ADI setup (201 expected,409 observed due valid occupancy); reactivation target not reached. Positive reactivation and unit control do not close this legitimate-negative requirement.","Suplemento49 atual falha legitimamente no preparoADI409 esperado201, antes de reativar. Contraprova negativa não alcançada; análise causal de alcançabilidade preservada, sem fabricar estado."].
- Predicado 3: reativação não cria faixa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"index":2,"predicate":"composição válida","status":"PARCIAL","limits":["The occupied zero-capacity preparation never reached reactivation. Positive composition paths cannot substitute that negative target; no invalid state was fabricated.","New opted-in negative test really FAILed at ADI setup (201 expected,409 observed due valid occupancy); reactivation target not reached. Positive reactivation and unit control do not close this legitimate-negative requirement.","Suplemento49 atual falha legitimamente no preparoADI409 esperado201, antes de reativar. Contraprova negativa não alcançada; análise causal de alcançabilidade preservada, sem fabricar estado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/449); métodos/recibos/assertions no mesmo registro e no CSV.

## DES-001

Desligamento pertence ao Vínculo Contratual, não é campo fixo do Contrato.

### DES-001:001

Término pertence ao Vínculo específico, sem alterar término do Contrato como substituto.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Término do Vínculo específico — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Contrato não serve de substituto — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/450); métodos/recibos/assertions no mesmo registro e no CSV.

## DES-002

Data de desligamento é o último dia em que o vínculo ainda existe; inatividade começa no dia seguinte.

### DES-002:001

Data de desligamento é inclusiva para o Vínculo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Data inclusiva de desligamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/451); métodos/recibos/assertions no mesmo registro e no CSV.

### DES-002:002

Inatividade começa em D+1; último dia não é excluído da cobertura.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Inatividade em D+1 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: cobertura do último dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/452); métodos/recibos/assertions no mesmo registro e no CSV.

## DES-003

Desligamento pode ser informado retroativamente; data real do fato prevalece, registro posterior fica auditado.

### DES-003:001

Desligamento retroativo usa a data real do fato.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Fato retroativo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: data real distinta do registro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/453); métodos/recibos/assertions no mesmo registro e no CSV.

### DES-003:002

Registro posterior é auditado e não reescreve fatos existentes silenciosamente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Registro posterior auditado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: fatos existentes preservados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/454); métodos/recibos/assertions no mesmo registro e no CSV.

## DES-004

Desligamento possui motivo de catálogo, responsável e data/hora de registro.

### DES-004:001

Desligamento exige causa válida de catálogo e Gestão responsável na data.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Causa de catálogo válida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: categoria compatível — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Gestão na data — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/455); métodos/recibos/assertions no mesmo registro e no CSV.

### DES-004:002

Responsável e registro são preservados; renomeação posterior não altera a causa aplicada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Responsável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: instante — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: fotografia da causa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: renomeação posterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/456); métodos/recibos/assertions no mesmo registro e no CSV.

## DES-005

Reprogramação/retificação de desligamento preserva original e verifica toda a linha temporal para evitar sobreposição.

### DES-005:001

Retificação mantém original e novo evento.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Original e novo evento na retificação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/457); métodos/recibos/assertions no mesmo registro e no CSV.

### DES-005:002

Revalida toda a linha temporal quanto a sobreposição, alocações posteriores e capacidade/excedente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Sobreposição global — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: faixas posteriores — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: capacidade e excedente de toda extensão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/458); métodos/recibos/assertions no mesmo registro e no CSV.

### DES-005:003

Confirmação usa prévia ainda válida; mudança concorrente rejeita o comando.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Prévia ainda válida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: mudança interveniente rejeita confirmação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/459); métodos/recibos/assertions no mesmo registro e no CSV.

## DES-006

Desligamento futuro/programado pode ser cancelado/retificado, preservando toda a cadeia.

### DES-006:001

Desligamento futuro pode ser cancelado ou retificado por comando próprio.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Desligamento futuro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: comandos de cancelamento e retificação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/460); métodos/recibos/assertions no mesmo registro e no CSV.

### DES-006:002

Cadeia original é preservada e período restaurado é revalidado antes de confirmar.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M09","M10"]. **ROP(s):** [].

- Predicado 0: Cadeia original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: restauração do período — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: validação antes de confirmar — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/461); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-001

Frequência resolve vínculo e alocação válidos na data negocial.

### FRE-001:001

Lançamento resolve vínculo/alocação da data e expõe seus IDs e estados independentes.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Resolver por D — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: IDs separados de Contrato/Vínculo/Alocação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: estados independentes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/462); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-001:002

Transferência/ADI e jornada noturna respeitam Gestão e alocação efetivas do início da jornada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Transferência e ADI — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Gestão do fato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: jornada atravessa meia-noite — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/463); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-001:003

Complementos novos exigem cobertura e autorização em todos os dias do período.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Cobertura na data — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: autorização em todos os dias do complemento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/464); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-001:004

Fechamento de contrato transferido resolve o contrato pelo dia solicitado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Contrato transferido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: fechamento pelo dia solicitado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: worker mantém contexto — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/465); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-001:005

CPF isolado de raiz legada não comprova contexto de criação.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Raiz legada sem associação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: CPF isolado não comprova contexto de criação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/466); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-002

Usuário Operacional pode editar por 7 dias corridos, contando a data da frequência como dia 1, em America/Sao_Paulo.

### FRE-002:001

A data da jornada é dia1; a janela termina às23:59:59 de D+6 em America/Sao_Paulo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: D é dia 1 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: dias corridos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: fim de D+6 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 3: America/Sao_Paulo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/467); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-002:002

A fronteira usa o fuso São Paulo e a autorização HTTP recusa Operacional fora da janela.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Conversão para São Paulo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: autorização HTTP na fronteira — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/468); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-003

Exemplo: frequência de 01/01 editável até 07/01 23:59:59 e bloqueada para o Operacional a partir de 08/01 00:00.

### FRE-003:001

A data da jornada é dia1; a janela termina às23:59:59 de D+6 em America/Sao_Paulo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Exemplo 01/01 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: editável até 07/01 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: fechamento em 08/01 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/469); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-003:002

A fronteira usa o fuso São Paulo e a autorização HTTP recusa Operacional fora da janela.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Fuso da fronteira — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: recusa efetiva do Operacional — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/470); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-004

Alterações do Operacional dentro da janela permanecem auditadas, com antes/depois, usuário e data/hora.

### FRE-004:001

Edição operacional dentro da janela avança versão e mantém antes/depois, autor e registro.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Versão da edição operacional — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: antes/depois — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: ator — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: data e instante — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/471); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-004:002

Cadeia de auditoria da frequência verifica hashes e detecta conteúdo incompatível.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Hashes recalculados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: cadeia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: conteúdo não verificável detectado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/472); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-005

Depois da janela, Gestor Local corrige por retificação versionada; não sobrescreve silenciosamente.

### FRE-005:001

Após a janela, Operacional é recusado e Gestor corrige por ato versionado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Operacional depois da janela — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Gestor versiona correção tardia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/473); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-005:002

Retificação de memória manual preserva a memória anterior no evento.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Memória manual anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: retificação noturna/diurna — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: fotografia diferente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/474); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-006

Gestor Local pode corrigir antes do fim dos 7 dias, com identificação e justificativa.

### FRE-006:001

Gestor pode retificar antes do7º dia, com versão formal e bloqueio apenas daquele registro.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Gestor antes do sétimo dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: ato formal — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: bloqueio somente do alvo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/475); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-006:002

Retificação gerencial sem justificativa é rejeitada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Retificação gerencial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: justificativa obrigatória — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/476); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-007

Gestor Local não possui prazo máximo rígido para retificação tardia; toda retificação tardia exige justificativa/auditoria.

### FRE-007:001

Gestor pode retificar jornada antiga sem prazo máximo rígido e com justificativa.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Jornada antiga — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: sem prazo máximo rígido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: justificativa auditada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/477); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-008

Retificação recalcula visão oficial atual de relatórios mensais, absenteísmo e indicadores; versões anteriores permanecem auditáveis.

### FRE-008:001

Retificação altera o resultado oficial atual sem reescrever emissão anterior.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Resultado oficial atual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: emissão anterior imutável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: corrida de publicação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/478); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-008:002

Memória manual de horas é aplicada de forma consistente em consulta, consolidação, emissão e arquivo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Memória manual persistida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: consulta — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: consolidação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: emissão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 4: arquivo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/479); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-008:003

Reconciliação automática preserva fontes manuais e recalcula apenas RT derivado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Fonte manual preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: somente adicional RT derivado reconciliado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/480); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-008:004

Mudança de calendário invalida resultados dependentes, inclusive dias SEM_LANCAMENTO.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Calendário retroativo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: dias sem lançamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: recortes antigo/novo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: controle externo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/481); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-009

Alteração retroativa deve identificar frequências abertas e fechadas atingidas; fechada não é alterada silenciosamente, exige retificação autorizada.

### FRE-009:001

Impacto retroativo em vínculo/alocação identifica frequência atingida e mantém seu conteúdo original.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Impacto em vínculo/alocação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: identificação do atingido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: conteúdo original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/482); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-009:002

Retirar/mover fonte que sustenta FOLGA fechada exige revisão sem alterar silenciosamente a jornada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: FOLGA fechada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: remoção/movimento de fonte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: revisão sem reescrita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/483); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-009:003

Concorrência entre inativação e gravação não deixa frequência válida sob contexto obsoleto.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Disputa inativação/gravação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: frequência não confirma válida em contexto vencido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/484); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-010

Frequências lançadas depois da data real de um desligamento retroativo permanecem visíveis/auditáveis e sinalizadas, mas não devem ser misturadas ao efetivo operacional válido/indicadores.

### FRE-010:001

Frequência após desligamento retroativo é preservada e retirada da população operacional válida.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Desligamento retroativo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: fonte posterior preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: exclusão do cálculo válido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/485); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-010:002

Consolidado sem faixa operacional mantém aviso, sem misturar a frequência inválida aos indicadores.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Sem faixa operacional — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: aviso de revisão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: inválida fora dos indicadores — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/486); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-011

Enquanto retificações pendem, relatórios podem ser consultados/exportados com aviso 'sujeito a revisão' e quantidade de pendências.

### FRE-011:001

Relatório com retificação pendente expõe aviso e contagem no recorte correto.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Aviso — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: quantidade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: recorte correto — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/487); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-011:002

Exportação XLSX mostra o aviso no próprio documento.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Exportação XLSX — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: aviso textual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: quantidade no próprio arquivo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/488); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-011:003

Presidência e comparação institucional recebem aviso real limitado ao recorte publicado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Presidência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: comparação institucional — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: contagem restrita à emissão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/489); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-011:004

Retirada de fonte de FOLGA gera aviso e pendência apenas para o dia afetado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Retirada de fonte de FOLGA — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: aviso/pendência só no dia atingido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/490); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-011:005

Pendência e aviso acompanham o responsável efetivo após transferência retroativa, inclusive quando a revisão já estava aberta.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Responsável efetivo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: transferência retroativa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: revisão já aberta — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: história preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/491); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-012

Quando retificação é concluída, a visão oficial atual usa a verdade corrigida e a emissão anterior permanece versionada.

### FRE-012:001

Após corrigir, verdade atual e nova emissão usam o resultado retificado preservando a original.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Correção oficial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: nova emissão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: preservação da original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/492); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-012:002

Pendência só é resolvida depois da correção da origem.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Não resolver pendência antes da origem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: resolver depois da retificação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/493); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-013

No 8º dia, a frequência passa automaticamente a FECHADA negocialmente; qualquer mudança posterior é retificação.

### FRE-013:001

No início do8º dia, a política de edição já considera frequência fechada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Primeiro instante do oitavo dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: política já considera fechada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/494); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-013:002

Cache não conserva ABERTA depois da fronteira e rotina técnica grava fechamento auditado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Cache não mantém ABERTA — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: rotina persiste fechamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: evento e idempotência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/495); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-014

Alteração do Gestor Local gera versão formal e bloqueia nova edição daquele registro pelo Operacional.

### FRE-014:001

Retificação gerencial bloqueia nova edição operacional daquele registro.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Retificação gerencial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: recusa posterior do Operacional — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/496); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-014:002

Nova retificação gerencial acrescenta versão mesmo se uma anterior já existe.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Nova retificação do Gestor — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: nova versão após anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/497); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-015

A frequência diária pertence ao escopo da alocação histórica do colaborador na data da frequência, e não à alocação atual do colaborador.

### FRE-015:001

Filtros de intervalo limitam cada dia à Gestão efetiva da alocação.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Intervalo por dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 1: Gestão efetiva — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 2: paginação e total após escopo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/498); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-015:002

Alteração da projeção atual de contrato não apaga horas do período histórico.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Projeção atual em outro contrato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: horas históricas mantidas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/499); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-015:003

Complementos e filtros usam a responsabilidade histórica; origem continua podendo manter o fato.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Complementos/filtros históricos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: manutenção pela origem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/500); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-015:004

Perda de cobertura não restaura uma Gestão superada nem apaga acesso histórico comprovado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Cobertura retirada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Gestão antiga superada não retorna — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: leitura comprovada preservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/501); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-015:005

Fechamento por contrato explícito usa Gestão do dia, não a atual.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Fechamento com contrato explícito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Gestão do dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/502); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-016

Se retificação retroativa muda a alocação histórica, outro Operacional não assume silenciosamente o registro; eventual correção passa ao Gestor Local e preserva o autor original.

### FRE-016:001

Mudança retroativa de alocação não reassocia a frequência nem transfere edição ao novo Operacional.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Retificação de alocação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: não reassociar fonte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: não transferir edição ao novo Operacional — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/503); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-016:002

Correção autorizada preserva autor original e antes/depois auditáveis.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Correção autorizada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: autor original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: snapshots antes/depois — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/504); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-017

Qualquer fato retroativo que retire cobertura válida preserva a frequência para auditoria, mas a retira do cálculo operacional oficial.

### FRE-017:001

Retirada de cobertura conserva a frequência como prova e marca validade operacional inválida.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Cobertura retirada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: prova conservada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 2: validade inválida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/505); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-017:002

Recuperar cobertura não regulariza silenciosamente a frequência pendente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Cobertura recuperada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: pendência não regularizada silenciosamente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/506); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-017:003

Transações concorrentes não confirmam cobertura inválida como válida.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Concorrência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: nenhuma frequência válida sob cobertura inválida confirmada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/507); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-017:004

Anulação contratual desde a jornada preserva frequência fechada e emissão original, retirando a frequência do cálculo vigente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Anulação desde D — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: frequência fechada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: emissão original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: cálculo vigente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/508); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-017:005

Inativação documentada retira do cálculo somente os dias abrangidos pelo alcance, mantendo as fontes históricas.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Inativação datada — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 1: exclusão só do alcance — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 2: conservação de fontes — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/509); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-018

Permissão de edição combina autorização atual do usuário com o escopo histórico do registro.

### FRE-018:001

Edição exige perfil atual autorizado e escopo histórico próprio.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Autorização atual de perfil — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: escopo histórico da escrita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/510); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-018:002

Revogação atual de permissão excepcional vale em consulta e eventos.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Permissão excepcional vigente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: uso auditado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: revogação em consulta/eventos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/511); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-018:003

Complementos autorizam origem antes de destino e todos os dias de uma exceção.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Autorizar origem antes de mover ao destino — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: exceção indivisível em todos os dias — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/512); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-018:004

Sem identidade comprovada, o CPF atual de outra Pessoa não concede consulta nem escrita; prova própria existente mantém acesso legítimo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: CPF coincidente sem identidade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: negação leitura/escrita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: prova própria conserva acesso — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/513); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-018:005

Comando de fechamento usa autorização presente sobre Gestão histórica.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Fechamento — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: autorização presente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Gestão histórica — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/514); métodos/recibos/assertions no mesmo registro e no CSV.

## FRE-019

Frequência separa visibilidade histórica, validade para cálculo operacional e estado de auditoria.

### FRE-019:001

Visibilidade, estado negocial, validade e auditoria são campos distintos.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Visibilidade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: estado negocial — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: validade operacional — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: auditoria distintos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/515); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-019:002

Registro inválido permanece auditável sem integrar totais operacionais.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Inválida permanece auditável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: fora dos totais operacionais — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/516); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-019:003

Fonte com cobertura retirada continua acessível pelo escopo comprovado, realinhado às transferências válidas.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Fonte sem cobertura — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: escopo comprovado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: transferências válidas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/517); métodos/recibos/assertions no mesmo registro e no CSV.

### FRE-019:004

Preservar leitura de fatos passados não libera o bloqueio operacional corrente nem fabrica sua data inicial.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M11","M12","M13","M14"]. **ROP(s):** [].

- Predicado 0: Leitura histórica preservada — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 1: bloqueio atual/futuro — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 2: não inventar data de inativação — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/518); métodos/recibos/assertions no mesmo registro e no CSV.

## REL-001

Relatório oficial atual reflete a última verdade válida/retificada; auditoria preserva versões anteriores.

### REL-001:001

Relatório oficial atual reflete a última retificação e conserva a emissão anterior.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Resultado atual usa a retificação confirmada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Conteúdo da emissão anterior permanece consultável e inalterado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/519); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-001:002

Comparação institucional de emissão local conserva o recorte publicado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Leitor nacional/Presidente conserva o universo local publicado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Pendências da comparação respeitam o mesmo recorte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/520); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-001:003

Mudança de calendário ou fonte de FOLGA sinaliza a verdade atual dependente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Alteração/cancelamento de calendário muda a previsão e supera a emissão atingida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Retirada de fonte de FOLGA sinaliza revisão sem transformar a frequência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/521); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-001:004

Consolidado e absenteísmo usam nome historicamente comprovado, com ausência explícita quando não há fotografia.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Consolidado e absenteísmo preservam o nome comprovado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Ausência de fotografia fica explícita e não usa cadastro atual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Exportação mantém a mesma política nominal — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/522); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-001:005

Rótulos de Lotação, Turno e Cargo do relatório derivam de fotografia comprovada da Alocação ou Frequência.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Lotação/Turno/Cargo vêm de fotografia de Alocação ou Frequência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Ausência dos rótulos permanece null sem eliminar a linha — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/523); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-001:006

Flag corrente sem data de inativação não exclui apuração histórica comprovada, com ou sem versões contratuais.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Flag corrente inativa sem data preserva relatório histórico com versões — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 1: A mesma regra vale sem versões contratuais — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/524); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-001:007

Apuração continua respeitando inatividade comprovada por evento temporal ou fotografia administrativa inicial.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Evento temporal de inativação exclui somente seu alcance — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 1: Fotografia administrativa inicialmente inativa prevalece sobre flag atual ativa — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/525); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-001:008

Intervalo misto é dividido na fronteira de consulta corrente sem registrar um ato de inativação presumido.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Intervalo misto conserva dias históricos e bloqueia corrente/futuro sem prova — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 1: Consulta não inventa evento de inativação — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/526); métodos/recibos/assertions no mesmo registro e no CSV.

## REL-002

Nova emissão após retificação retroativa deve ser identificada como versão retificada, com data/hora de geração.

### REL-002:001

Nova emissão após invalidação tem versão maior e natureza RETIFICADA.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Após invalidação, nova emissão tem versão maior e natureza RETIFICADA — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: A emissão original continua identificada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/527); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-002:002

Fontes temporais sem lançamento e calendário também provocam emissão retificada quando alteram resultado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Fonte temporal sem frequência provoca retificação quando muda resultado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Calendário sem frequência também provoca emissão retificada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/528); métodos/recibos/assertions no mesmo registro e no CSV.

## REL-003

Auditoria deve permitir consultar a verdade efetiva atual e o que o sistema conhecia até determinada data de registro.

### REL-003:001

Consulta de emissão e posições distingue conhecimento anterior da verdade atual.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Consulta de emissão distingue conhecimento anterior/atual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Posições respeitam conhecimento dos eventos/relações — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/529); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-003:002

Fontes de calendário/complementos mantêm cadeia consultável no instante de conhecimento.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Calendário conserva cadeia e seleciona conhecidoAte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Complementos conservam versões e seleção no corte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/530); métodos/recibos/assertions no mesmo registro e no CSV.

## REL-004

Relatório com pendências de retificação pode ser visualizado/exportado, mas não deve parecer definitivo; marcar sujeito a revisão.

### REL-004:001

Relatório pendente indica revisão e quantidade, inclusive quando não há linhas operacionais.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Relatório pendente indica revisão e quantidade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Aviso persiste quando não há linhas operacionais — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/531); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-004:002

Aviso acompanha o arquivo exportado e o recorte institucional autorizado.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Arquivo exportado contém aviso e quantidade — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Aviso segue recorte autorizado inclusive leitura institucional — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/532); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-004:003

O aviso do relatório segue o escopo responsável pela revisão, sem alterar a fotografia original da fonte.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Responsável efetivo recebe aviso de revisão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Fotografia/autoria originais permanecem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Correção e resolução retiram aviso — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/533); métodos/recibos/assertions no mesmo registro e no CSV.

## REL-005

Uma emissão administrativa de relatório deve registrar período, data/hora de geração, usuário responsável, versão, indicação de pendências existentes e identificação de emissão original ou retificada.

### REL-005:001

Emissão registra período, geração, usuário/perfil, versão, natureza, pendências e hash.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Período, geração e autoria/perfil são registrados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Versão, natureza, pendências e hash constam da emissão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/534); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-005:002

Reenvio de mesma intenção preserva emissão e intenção diferente na mesma chave é conflito.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Reenvio da mesma intenção preserva a emissão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Mesma chave com filtros diferentes conflita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/535); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-005:003

Concorrência idempotente só retorna após transação válida de recuperação.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Concorrência real confirma uma emissão e retorna prova persistida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Recuperação após rollback abre e confirma nova transação — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/536); métodos/recibos/assertions no mesmo registro e no CSV.

## REL-006

Retificação que altera cálculo invalida a consolidação anterior para a visão vigente, recalcula resultados dependentes e preserva a versão histórica.

### REL-006:001

Alteração temporal invalida exatamente intervalos/escopos atingidos e preserva conteúdo anterior.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Invalidação alcança datas/escopos antigo e novo e poupa controles — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Conteúdo anterior permanece preservado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Consolidação dependente não publica cálculo anterior como atual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/537); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-006:002

Cálculo afetado por calendário e retirada de fonte de FOLGA supera emissão vigente sem apagar prova.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Calendário/retirada de fonte de FOLGA supera emissão vigente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Prova original permanece e recorte alheio é poupado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/538); métodos/recibos/assertions no mesmo registro e no CSV.

### REL-006:003

Comparação da consolidação vigente mantém o escopo da emissão original.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M18","M19"]. **ROP(s):** [].

- Predicado 0: Comparação mantém o escopo da emissão original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Mudança fora do escopo não altera resultado e a prova não é regravada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/539); métodos/recibos/assertions no mesmo registro e no CSV.

## AUD-001

Auditoria registra autor, data/hora de ação, data efetiva, motivo/justificativa e antes/depois quando aplicável.

### AUD-001:001

Registrar autor, data de registro, data efetiva, motivo e antes/depois

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Autor, instante de registro, data efetiva e motivo explícitos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Antes/depois quando há retificação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/540); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-001:002

Escopo auditado corresponde ao fato

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Escopo do registro corresponde ao contexto do fato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Consulta revalida responsabilidade temporal sem reescrever prova — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/541); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-001:003

Cadeia protege os metadados de autorização

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Alterar metadados que controlam autorização rompe integridade — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Legado não é reassinado para aparentar proteção inexistente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/542); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-001:004

Ocorrências registram autor/instante/contexto efetivo; histórico novo v2 autentica Gestão/estado/data; histórico de ocorrência v3 também autentica o vínculo explícito à fonte. Calendário e fontes manuais preservam versões com autoria, registro, conteúdo e cadeia de hashes. Assinaturas legadas não são reescritas.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Ocorrência registra contexto efetivo e vínculo explícito autenticado v3 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Assinatura legada permanece sem reescrita — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Calendário/complementos preservam autoria, registro e fotografias sucessivas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: v2 autentica isoladamente Gestão, estado e data efetiva; todos os campos declarados discriminados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/543); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-001:005

Atualização dos dados opcionais da Pessoa pelo fluxo canônico registra valores anteriores/novos, autor, instante, data efetiva e motivo de operação; rollback não confirma os dados nem publica ação bem-sucedida.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Atualização canônica opcional registra antes/depois, autor, datas e motivo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Rollback não confirma dados nem ação bem-sucedida — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/544); métodos/recibos/assertions no mesmo registro e no CSV.

## AUD-002

Retificação preserva fato original, nova versão, motivo, autor, data de registro e data efetiva.

### AUD-002:001

Retificação preserva original e nova versão

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Original permanece e nova versão é acrescentada — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/545); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-002:002

Motivo/autor/data efetiva/registro são explícitos

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Motivo, autor, data efetiva e registro são explícitos — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/546); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-002:003

Alteração de fonte de frequência não apaga sua versão original

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Retificar fonte mantém versão original — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Reconciliar frequência não remove adicional manual publicado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/547); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-002:004

Retificações de calendário/exceção/adicional acrescentam fotografias imutáveis; cancelamentos lógicos preservam raiz e versões. Reconciliar frequência não remove fonte manual publicada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Calendário/exceção/adicional acrescentam fotografias — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cancelamento conserva raiz e versões — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Reconciliação conserva fonte manual — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/548); métodos/recibos/assertions no mesmo registro e no CSV.

## AUD-003

Histórico deve distinguir quando o fato valeu de quando o sistema tomou conhecimento.

### AUD-003:001

Data efetiva e registro são distintas

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Data efetiva é distinta do instante de registro — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/549); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-003:002

Consulta conhecidoAte respeita conhecimento disponível

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: conhecidoAte seleciona apenas fontes conhecidas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/550); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-003:003

Auditoria mantém as versões superadas

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Versões superadas permanecem consultáveis — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/551); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-003:004

Situação de evento contratual em conhecidoAte é calculada pelas relações conhecidas no corte. Cadeias de calendário e complementos distinguem data do fato e instante registrado, com consulta histórica por conhecidoAte.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Situação contratual depende das relações conhecidas no corte — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].
- Predicado 1: Calendário e complementos distinguem data do fato/registro e cortes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/552); métodos/recibos/assertions no mesmo registro e no CSV.

## AUD-004

Prévia de impacto deve ser revalidada na confirmação; se o impacto mudou, recalcular e exigir nova confirmação.

### AUD-004:001

Prévia assina estado/impactos completos

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Prévia conta impactos completos e indica truncamento da amostra — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Token incorpora dependência fora da amostra — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/553); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-004:002

Confirmação relê sob lock

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Confirmação relê estado/impacto concorrente antes de confirmar — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Confirmação disputa lock contratual real e revalida causa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/554); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-004:003

Dependência fora da amostra invalida token

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Dependência fora da amostra invalida token — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/555); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-004:004

Nova confirmação é exigida

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Prévia obsoleta não confirma — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Nova prévia/confirmação é exigida para prosseguir — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/556); métodos/recibos/assertions no mesmo registro e no CSV.

## AUD-005

Operações negociais devem impedir duplicidade acidental por clique repetido, reenvio ou falha de rede.

### AUD-005:001

Reenvio de mesma intenção não cria fato duplicado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Reenvio não cria fato duplicado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/557); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-005:002

Mesma chave com intenção diferente conflita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Mesma chave com intenção diferente retorna409 sem mutação — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/558); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-005:003

Autorização é revalidada antes de devolver resposta idempotente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Autorização é checada antes de devolver resultado idempotente — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/559); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-005:004

Ocorrência identifica intenção normalizada e arquivos sob bloqueio de contexto, chave explícita ou digest determinístico. Mesmo reenvio retorna mesmo fato; intenção divergente409; duas transações concorrentes confirmam uma única fonte. PUT idêntico de calendário/complemento não acrescenta versão.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Mesmo reenvio de ocorrência retorna fato e divergência409 — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Concorrência confirma uma fonte/contexto/arquivo e rollback desfaz — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: PUT idêntico calendário/complemento não acrescenta versão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Normalização de intenção/arquivos e caminho sem chave por digest são discriminados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/560); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-005:005

Reenvio operacional de frequência (sequencial ou simultâneo, com/sem chave) preserva versão e cadeia; mesma chave com intenção diferente retorna409; textos com separadores têm fronteiras inequívocas no hash.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Reenvio operacional sequencial com/sem chave preserva versão/cadeia/autor — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Reenvio simultâneo com/sem chave confirma um fato — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Divergência409 e fronteiras textuais inequívocas — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Alteração nova sem chave continua auditável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/561); métodos/recibos/assertions no mesmo registro e no CSV.

## AUD-006

Central de pendências deve identificar origem, motivo, responsável, estado e escopo afetado; cada pendência bloqueia somente o necessário.

### AUD-006:001

Central identifica origem, motivo, datas, responsável e estado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Origem, motivo, datas, responsável e estado são identificados — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/562); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-006:002

Pendência bloqueia só recorte atingido

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Pendência e tratamento respeitam escopo autorizado — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Controle de frequência fora do recorte permanece válido — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Bloqueio negocial limita-se ao necessário, com escrita positiva fora do recorte — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/563); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-006:003

Responsabilidade usa Gestão efetiva, inclusive após transferência

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Responsabilidade usa Gestão efetiva do efeito após transferência — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Transferência posterior não tira tarefa do responsável pelo dia anterior — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/564); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-006:004

Pendência de fim natural/conflito usa Gestão efetiva no dia do efeito. Aviso de relatório respeita o recorte congelado da emissão, inclusive consulta institucional nacional.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Fim natural usa Gestão efetiva do dia do efeito — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Aviso institucional usa recorte congelado da emissão — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Conflito de replay após mudança de Gestão usa o responsável do seu dia — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/565); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-006:005

Pendência de frequência acompanha a responsabilidade efetiva por Gestão/Lotação/Turno após transferência retroativa, inclusive revisão já aberta, sem reescrever fotografia, autoria ou versão original da fonte. Relatório do responsável avisa revisão e deixa o aviso após correção e resolução comprovadas.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Transferência retroativa realinha Gestão/Lotação/Turno inclusive revisão aberta — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Fotografia/autoria/versão original permanecem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Relatório do responsável avisa e deixa de avisar após resolução — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/566); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-006:006

Todas as pendências ainda ativas da mesma frequência, inclusive versões anteriores em tratamento, acompanham a responsabilidade efetiva após transferência; IDs, chaves, motivo, surgimento, autoria de início e fotografia da fonte permanecem. Correção humana resolve as revisões da origem e retira o aviso.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Todas revisões ainda ativas inclusive antiga em tratamento acompanham contexto efetivo — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: IDs/chaves/motivos/surgimento/início/fotografia permanecem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Correção humana resolve todas e retira aviso — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/567); métodos/recibos/assertions no mesmo registro e no CSV.

## AUD-007

Retificação de retificação é permitida sem limite artificial; preservar cadeia completa e identificar a versão vigente em cada ponto temporal.

### AUD-007:001

Retificação de retificação sem teto artificial

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Retificação de retificação é aceita em cadeia extensa — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/568); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-007:002

Cadeia completa permanece consultável

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Cadeia completa continua consultável — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/569); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-007:003

Versão vigente e datas são inequívocas

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Versão vigente e datas não se confundem — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/570); métodos/recibos/assertions no mesmo registro e no CSV.

### AUD-007:004

Retificação de retificação preserva versões sucessivas também nas fontes calendário/exceção/adicional; conhecidoAte seleciona apenas conhecimento existente no corte. Correção de uma anulação não invalida outra decisão independente.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M15","M16","M21"]. **ROP(s):** [].

- Predicado 0: Fontes de calendário/complementos preservam retificações sucessivas e cortes — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Corrigir uma anulação não retira outra decisão independente — **PASS NA CAMADA**. Camadas: ["UNIT_OR_STRUCTURAL_TEST"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/571); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-001

Gestor Local é o principal perfil de escrita negocial do módulo no escopo de sua Gestão.

### PER-001:001

Gestor Local escreve somente no próprio escopo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Gestor Local realiza ato negocial no próprio escopo. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: O mesmo perfil em Gestão alheia é recusado nas mutações contratuais e pessoais. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/572); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-001:002

Rotas canônicas e legadas aplicam a mesma fronteira

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Rotas canônicas aplicam a fronteira de perfil e Gestão. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Rotas legadas removidas não deixam manipuladores alternativos de escrita. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/573); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-001:003

Ocorrência respeita Gestão efetiva da data do fato

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: A Gestão usada para criar, consultar e cancelar corresponde à data efetiva do fato. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: A projeção atual e a fotografia original não substituem a linha temporal corrigida. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/574); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-001:004

Criação e cancelamento exigem Gestor Local na Gestão efetiva de todos os dias do fato; gravação e histórico não dependem dos campos atuais materializados.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Criação/cancelamento por Local são autorizados na Gestão efetiva, inclusive após correção retroativa. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Ocorrência de dois dias exige cobertura de todos os contextos de Lotação/Turno para leitura/download. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Criação e cancelamento por Local em ocorrência que atravessa duas Gestões exigem autorização em todos os dias. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/575); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-001:005

Uma ocorrência legada sem associação canônica nem fotografia vinculada não pode receber a identidade, o contrato ou a Gestão de outra Pessoa por coincidência atual de CPF.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Ocorrência legada sem associação/fotografia não é reassociada por coincidência posterior de CPF. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/576); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-002

Gestor Estadual consulta/audita as Gestões Locais de seu estado e não executa aditamento/remanejamento/encerramento/retificação negocial.

### PER-002:001

Estadual consulta somente o próprio estado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Estadual consulta contrato da Gestão de seu estado. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Estado diverso recebe recusa e lista estadual não inclui contrato alheio. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/577); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-002:002

Não executa atos negociais

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Estadual não adita, remaneja, encerra nem retifica negócio. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: A proibição também alcança mutações de Pessoa/vínculo/alocação. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/578); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-002:003

Limite territorial segue o fato histórico consultado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: A fronteira estadual acompanha a Gestão reconstruída na data histórica. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: A fotografia original e o efeito posterior de anulação não autorizam estado antigo. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/579); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-002:004

Consulta de ocorrência usa estado da Gestão reconstruída para a data efetiva; não libera escrita ao Gestor Estadual.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Consulta estadual de ocorrência usa estado da Gestão efetiva reconstruída. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: O Gestor Estadual é recusado especificamente em criação e cancelamento de ocorrência. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/580); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-003

Gestor Nacional atua em consulta/auditoria nacional neste módulo, sem escrita negocial.

### PER-003:001

Gestor Nacional consulta/audita nacionalmente

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Nacional consulta contratos das duas Gestões/estados. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/581); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-003:002

Escrita negocial continua exclusiva ao Gestor Local

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Nacional não recebe escrita contratual nem pessoal/canônica. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Local autorizado mantém controle positivo de escrita. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/582); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-003:003

Concessão pessoal não concede escrita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Concessão para conteúdo pessoal não concede cancelamento de ocorrência. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/583); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-004

Presidente atua somente em consulta nacional neste módulo.

### PER-004:001

Presidente consulta nacional sem escrita

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Presidência consulta dados institucionais em Gestão diversa. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Perfil não executa os comandos negociais contratuais e de pendência exercitados. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/584); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-004:002

Contratos/pessoas/vínculos institucionais minimizados

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Contratos, Pessoas e vínculos preservam identidade institucional útil. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: CPF/nome/RG/e-mail não são incluídos na consulta institucional padrão. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/585); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-004:003

Documento negocial permitido no escopo nacional

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Presidente recebe conteúdo de documento negocial contratual e de ocorrência. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/586); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-004:004

Presidência consulta documentos negociais e DTOs institucionais minimizados das ocorrências em âmbito nacional; GET não libera comandos de criação/cancelamento.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Presidente consulta documento negocial e lista/detalhe minimizados de ocorrência. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: GET/concessão não libera cancelamento da ocorrência. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: GET não libera criação de ocorrência por Presidente. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/587); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-005

Administrador técnico não deve ser confundido com responsável negocial; intervenção excepcional exige permissão explícita e auditoria forte.

### PER-005:001

Administração concede permissões técnicas

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Administrador concede permissão técnica específica válida. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Local não concede; escopo/prazo inválidos são recusados. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/588); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-005:002

Não ganha escrita negocial comum

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Administrador técnico não executa negócio ordinário. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/589); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-005:003

Acesso funcional pessoal exige concessão e uso auditado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Conteúdo pessoal ao Administrador depende de intervenção concedida. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Uso identifica beneficiário, recurso e tipo; revogação remove acesso. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/590); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-005:004

Administrador técnico permanece sem escrita negocial; documento pessoal exige intervenção excepcional vigente, finalidade e trilha de uso.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Administrador permanece sem escrita negocial. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Documento pessoal exige concessão de intervenção e produz trilha de uso. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Finalidade é revalidada isoladamente no download com concessão ativa. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/591); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-006

Gestor Estadual visualiza apenas os dados pessoais necessários à auditoria.

### PER-006:001

Estadual recebe somente dados necessários

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Estadual recebe dados gerenciais úteis com proteção de campos pessoais e narrativas. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: A minimização preserva a fonte do fato. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/592); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-006:002

CPF mascarado e opcionais ausentes

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: CPF aparece mascarado para Estadual. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Dados opcionais RG/e-mail ficam ausentes/nulos. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/593); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-006:003

Narrativas livres de ocorrência não escapam na lista

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Narrativas pessoais de ocorrência são removidas no detalhe estadual, ativo e cancelado. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Listagem nacional remove narrativa pessoal e anexo restrito. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Listagem de ocorrência pelo Estadual não vaza narrativa. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/594); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-006:004

Lista/detalhe de ocorrência e narrativas livres de Pessoa, vínculo, alocação e excedente passam pela política central de minimização; fontes persistidas não são alteradas.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Detalhe de ocorrência estadual minimiza sem destruir prova, inclusive cancelada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Pessoa/vínculo/alocação/excedente têm minimização multi-perfil e fonte preservada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Listagem estadual de ocorrência passa pela mesma fronteira sem vazamento. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/595); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-007

Correção excepcional de CPF é autorizada pelo Gestor Local.

### PER-007:001

Correção CPF é ato do Gestor Local

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Gestor Local efetua a correção excepcional de CPF na própria Pessoa. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: A rota de correção de identidade recusa perfis não negociais exercitados. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/596); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-007:002

Novo CPF validado e único

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: CPF novo respeita validação de dígitos e não aceita sequência inválida. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: CPF já atribuído a outra Pessoa é recusado sem alterar ambas. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/597); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-007:003

Motivo/evidência/autorização/valores anterior e novo preservados

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Motivo, evidência, autorização e impactos ficam preservados no evento. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Valores anterior/novo, autor e instante ficam ligados à correção. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/598); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-008

Correção excepcional de número/ano exige permissão específica concedida pela administração técnica; o ato negocial permanece com o Gestor Local autorizado.

### PER-008:001

Correção de identificador exige concessão específica ativa

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Mesma correção válida sem concessão específica é recusada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Concessão específica válida permite o comando e não transfere execução ao Administrador. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/599); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-008:002

Administração concede, Gestor Local executa

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Administração concede e Gestor Local executa. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Após transferência, beneficiário deve pertencer à Gestão efetiva. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Concedente não recebe escrita negocial por ser administrador. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/600); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-008:003

Uso auditado e alias antigo reservado/pesquisável

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Uso específico é auditado com executor correto. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Alias antigo continua pesquisável e reservado a seu contrato. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/601); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-009

Gestor Estadual, Gestor Nacional e Presidente podem consultar documentos negociais dentro de seus respectivos escopos de consulta/auditoria. Dados pessoais desnecessários à auditoria permanecem protegidos.

### PER-009:001

Perfis de auditoria consultam documentos negociais no seu escopo

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Estadual, Nacional e Presidente consultam documento negocial em escopo permitido. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Presidente recebe bytes negociais e DTO minimizado de ocorrência. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Documento de outro estado é recusado especificamente ao Estadual. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/602); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-009:002

Conteúdo pessoal não é liberado pelo simples acesso ao evento

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Consulta do evento não concede conteúdo pessoal. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: DTO/lista omite narrativa e anexo restrito enquanto download é recusado. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/603); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-009:003

Documento restrito exige concessão

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Documento pessoal/restrito exige concessão para leitor institucional. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Concessão permite leitura e sua revogação volta a recusá-la. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/604); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-009:004

Documento negocial de ocorrência tem consulta autorizada pelo contexto efetivo; Presidente recebe conteúdo negocial nacional com dados pessoais minimizados no DTO. Classificação pessoal continua exigindo concessão.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Documento negocial de ocorrência revalida contexto efetivo, inclusive após cancelamento. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Presidente recebe bytes negociais nacionais com DTO minimizado. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Classificação pessoal exige concessão. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/605); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-010

Permissão histórica de frequência usa autorização atual do usuário combinada com o escopo histórico do registro.

### PER-010:001

Autorização atual sobre contexto histórico do registro

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Leitura usa contexto histórico do registro, combinado com autorização corrente. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Filtros e paginação são calculados após o recorte temporal autorizado. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/606); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-010:002

Mudança de Lotação/Turno não transfere acesso a fatos de outra data

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Movimento interno posterior não transfere acesso ao fato antigo. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Ocorrência de vários dias exige cobertura de todos os contextos. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/607); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-010:003

Revogação atual remove acesso

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Alteração/revogação atual retira acesso inclusive com cache previamente utilizado. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Revogação/expiração de concessão bloqueia novos bytes sem destruir prova. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Arquivo pronto/handle já preparado não contorna mudança de perfil/Gestão. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/608); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-010:004

DIF-02: uma política temporal compartilhada resolve Lotação/Turno/Gestão do fato, cobre detalhe, lista, filtros antes de paginação, total, download, criação, cancelamento e concessão. Contextos de ocorrência são fotografados; correções retroativas revalidam a linha temporal; ausência de prova bloqueia acesso.

**Status:** PARCIAL. **Técnico:** PARCIAL.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Detalhe/lista/filtros antes de página/total e contexto fotografado têm fronteira temporal. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Download/criação/cancelamento são revalidados após correções retroativas. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Ausência de prova retorna escopo desconhecido, sem recorrer à projeção atual. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Todos os consumidores, especialmente concessão, bloqueiam acesso sem prova e compartilham a política em todo o contexto multi-dia. — **PARCIAL**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["Literal §7.6 consumer conceder remains unproved as a contextual absence-of-origin refusal. The generic grant endpoint has no occurrenceId/date and returned201; subsequent use403 and all-five-consumer denials are proved, but do not demonstrate contextual refusal at granting. Not classified as a product vulnerability.","Concessão genérica201 e recusas posteriores dos consumidores não demonstram recusa contextual da própria concessão sem origem."].

**Parte não comprovada:** [{"index":3,"predicate":"Todos os consumidores, especialmente concessão, bloqueiam acesso sem prova e compartilham a política em todo o contexto multi-dia.","status":"PARCIAL","limits":["Literal §7.6 consumer conceder remains unproved as a contextual absence-of-origin refusal. The generic grant endpoint has no occurrenceId/date and returned201; subsequent use403 and all-five-consumer denials are proved, but do not demonstrate contextual refusal at granting. Not classified as a product vulnerability.","Concessão genérica201 e recusas posteriores dos consumidores não demonstram recusa contextual da própria concessão sem origem."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/609); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-010:005

Uma ocorrência legada sem associação canônica nem fotografia vinculada não pode receber a identidade, o contrato ou a Gestão de outra Pessoa por coincidência atual de CPF.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Coincidência atual de CPF não reassocia ocorrência legada sem prova. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/610); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-011

Gestor Nacional e Presidente seguem minimização de dados pessoais: somente dados necessários à auditoria nacional/consulta institucional; conteúdo pessoal/restrito adicional depende de permissão específica.

### PER-011:001

Nacional e Presidente recebem conteúdo minimizado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Nacional recebe pessoal mascarado/narrativas reduzidas. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Presidente recebe dados institucionais sem nome/CPF/opcionais. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Cache distingue capacidades Nacional/Presidente. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/611); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-011:002

Conteúdo adicional requer concessão específica

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Conteúdo adicional exige concessão delimitada, não apenas o perfil. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Concessão de um contrato não abre outro. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/612); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-011:003

A concessão é revalidada e usada com auditoria

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Concessão é revalidada por leitura após revogação e vencimento. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Uso autorizado gera trilha ligada a usuário, permissão, recurso, finalidade e instante. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/613); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-011:004

Nacional e Presidente recebem narrativas minimizadas e documentos pessoais somente com finalidade e concessão explícita vigente; a Presidência não recebe nome na consulta institucional padrão. Mesma política aplicada às narrativas de identidade/vínculo/alocação/excedentes.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Nacional/Presidente recebem narrativas reduzidas, Presidente sem nome padrão. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Documentos pessoais requerem concessão explícita e atualmente válida. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Download revalida finalidade ausente/divergente isoladamente com concessão válida. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/614); métodos/recibos/assertions no mesmo registro e no CSV.

## PER-012

Transferência excepcional de Gestão Local exige autorização administrativa específica e execução por Gestor Local autorizado; Gestor Estadual não recebe escrita negocial.

### PER-012:001

Transferência de Gestão exige autorização administrativa específica

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Transferência com intenção válida sem autorização específica é recusada sem efeitos. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Autorização específica administrativa permite a mesma intenção válida. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/615); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-012:002

Ator é Gestor Local autorizado

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Executor do ato é Local autorizado; autor e uso são persistidos. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Demais perfis e Local de outra Gestão não executam a transferência. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/616); métodos/recibos/assertions no mesmo registro e no CSV.

### PER-012:003

Estadual não passa a executar negócio

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M20","M17"]. **ROP(s):** [].

- Predicado 0: Estadual não ganha transferência nem demais escritas negociais. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/617); métodos/recibos/assertions no mesmo registro e no CSV.

## HIS-001

Consulta histórica do contrato deve responder como estava sua configuração completa em uma data: vigência, empresa, tipo, composição, efetivo, aditamentos/remanejamentos efetivos e origem dos eventos.

### HIS-001:001

Posição contratual apresenta empresa, tipo, composição, efetivo e origem de eventos da data.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Empresa e Tipo são os da versão efetiva consultada, não os cadastros posteriores. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Composição/efetivo são reconstruídos pelos deltas e rótulos de sua fonte. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Origem, data efetiva e autoria dos eventos permanecem rastreáveis. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Acervo administrativo autêntico comprova a configuração original no recorte real. — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":3,"predicate":"Acervo administrativo autêntico comprova a configuração original no recorte real.","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/618); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-001:002

Novas publicações/aditamentos fotografam política de matrícula e redução de jornada.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Publicação nova fotografa política de matrícula e dia/horas reduzidos. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Mudança do mestre posterior não redefine a política publicada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Aditamento documenta nova política/redução conservando os valores originais. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Acervo administrativo autêntico comprova a configuração original no recorte real. — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":3,"predicate":"Acervo administrativo autêntico comprova a configuração original no recorte real.","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/619); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-001:003

Consulta legada sem configuração original explicita desconhecimento e não adota o mestre atual.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Ausência integral da versão e campos vazios/nulos não são preenchidos pelo contrato/mestres atuais. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Configurações ausentes da composição e de novos itens legados continuam desconhecidas. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Política de matrícula/redução desconhecida é explícita e não autoriza inferência do mestre. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Foto posterior não vira origem anterior nem conhecimento anterior à sua incorporação. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 4: Acervo administrativo autêntico comprova a configuração original no recorte real. — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":4,"predicate":"Acervo administrativo autêntico comprova a configuração original no recorte real.","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/620); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-001:004

Habilitacao original ausente permanece desconhecida; ato posterior define somente seu periodo comprovado.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Comportamento técnico mantém habilitação original ausente como desconhecida, para projeção atualtrue/false. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Primeiro ato posterior só define habilitação desde sua própria data efetiva. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Autenticidade/origem documental da habilitação ausente no acervo real. — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":2,"predicate":"Autenticidade/origem documental da habilitação ausente no acervo real.","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/621); métodos/recibos/assertions no mesmo registro e no CSV.

## HIS-002

Consulta histórica do colaborador deve mostrar todos os vínculos e períodos de alocação, inclusive mudanças dentro do mês.

### HIS-002:001

História retorna todos os vínculos autorizados da Pessoa e todas as faixas de Alocação no mês.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: História da mesma Pessoa retorna os dois vínculos de contratos distintos autorizados ao Nacional. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Faixas do mês incluem origem, faixa substituída, retificação e mudança posterior com identidades e datas preservadas. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Duas alocações no mês não duplicam a população de Pessoas. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/622); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-002:002

Nome histórico usa fotografia do fato, separado do cadastro atual.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Nome histórico vem da foto do evento e tem fonte identificada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Nome atual permanece separado e a correção não altera foto/hash do evento. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/623); métodos/recibos/assertions no mesmo registro e no CSV.

## HIS-003

Pesquisa por início/fim de vínculo não pode confundir transferência interna de alocação com desligamento/recontratação.

### HIS-003:001

Filtros por início/fim selecionam datas de Vínculo.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Filtro de início usa data efetiva do vínculo e não mês do registro. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Filtro de término exclui vínculo ainda aberto. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Filtro de término inclui o vínculo encerrado pela data efetiva correta, com contraste de data do registro/alocação. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/624); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-003:002

Mudança interna de Alocação não aparece como novo vínculo ou desligamento.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Mudança interna retroativa e sua retificação conservam início/fim do mesmo vínculo. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Pesquisa por início/fim após essa mudança não o apresenta como contratação/desligamento na data da Alocação. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/625); métodos/recibos/assertions no mesmo registro e no CSV.

## HIS-004

Cadastros mestre atuais não reescrevem fotografia histórica do contrato/vínculo.

### HIS-004:001

Alterar cadastro atual de Pessoa/Cargo/Lotação/Turno/Tipo/Gestão não modifica fotografias anteriores.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Mudança de Pessoa preserva nome histórico e fonte. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cargo/Lotação/Turno renomeados não reescrevem composição, alocação ou instrumento anteriores. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Tipo e Gestão posteriores não substituem os fotografados. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Grade/município/valores operacionais históricos sobrevivem à mudança de mestres. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/626); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-004:002

Fotografias ausentes não são completadas pelos mestres atuais.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Foto nominal ausente continua desconhecida após correção de cadastro. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Rótulos de Cargo/Lotação/Turno ausentes não são preenchidos pelos mestres. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Grade/município ausentes não são completados e ausência legítima de município é preservada. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Versão ausente/parcial não herda identificação/empresa/Tipo/Gestão/configuração atual. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/627); métodos/recibos/assertions no mesmo registro e no CSV.

## HIS-005

História é a fonte canônica; projeções atuais são conveniência técnica.

### HIS-005:001

Motores históricos usam a cadeia de eventos conhecida no corte.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Corte anterior à origem não usa vínculo da projeção corrente. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Primeiro evento conhecido preserva os limites anteriores sem herdar alteração recebida depois de K. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Reprocessamento temporal de Alocação conserva a cadeia conhecida antes da correção. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/628); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-005:002

Projeção Empregado não sobrepõe Vínculo/Alocação canônicos nem ressuscita programação cancelada.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Projeção de Empregado não redefine Vínculo/Alocação canônicos. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL","SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 1: Vínculo canônico cancelado não ressuscita por história legada. — **PASS NA CAMADA**. Camadas: ["SPRING_SERVICE_OR_JPA_INTEGRATION"]. Limites: [].
- Predicado 2: Corte desconhecido não expõe a projeção atual como vínculo histórico. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/629); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-005:003

Eventos novos de reprocessamento preservam a visão anterior ao seu registro.

**Status:** PARCIAL. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Retificação acrescenta reprocessamento sem alterar a visão anterior a seu registro. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Cancelamento futuro restaura cobertura corrente e mantém destino na visão anterior. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Alocação posterior independente e vínculo único são preservados. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].

**Parte não comprovada:** [{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/630); métodos/recibos/assertions no mesmo registro e no CSV.

## HIS-006

A publicação preserva fotografia integral da versão original: identificação, empresa, vigência, tipo, composição, efetivo e demais dados negociais, base para eventos posteriores.

### HIS-006:001

Publicação fotografa o cargo efetivamente selecionado no rascunho e a base quantitativa original.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Publicação congela ID/nome do Cargo efetivamente escolhido por último no rascunho. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Base quantitativa original não muda quando o contrato aumenta e depois reduz. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Acervo administrativo autêntico comprova a configuração original no recorte real. — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":2,"predicate":"Acervo administrativo autêntico comprova a configuração original no recorte real.","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/631); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-006:002

Novas fotografias preservam todas as configurações operacionais contratuais e ADI conserva a original.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Publicação captura configuração operacional final e revalida sua mudança depois da prévia. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Política de matrícula, dia reduzido, grade/município e dados negociais são preservados contra mestres atuais. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: ADI altera o que documentou, conserva original e não redefine combinação existente por mero delta quantitativo. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Acervo administrativo autêntico comprova a configuração original no recorte real. — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":3,"predicate":"Acervo administrativo autêntico comprova a configuração original no recorte real.","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/632); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-006:003

Legado incompleto não recebe configuração atual retroativamente como se fosse a publicação original.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Ausência da publicação integral não permite promovê-la a partir da projeção atual. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Versão posterior/ADI só comprova seu período e não vira base original. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Configurações ausentes em versão/composição/item legado continuam desconhecidas. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 3: Acervo administrativo autêntico comprova a configuração original no recorte real. — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":3,"predicate":"Acervo administrativo autêntico comprova a configuração original no recorte real.","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/633); métodos/recibos/assertions no mesmo registro e no CSV.

### HIS-006:004

Habilitacao original ausente permanece desconhecida; ato posterior define somente seu periodo comprovado.

**Status:** BLOQUEADO POR DOCUMENTAÇÃO. **Técnico:** CONJUNÇÃO TÉCNICA COMPROVADA NAS CAMADAS DECLARADAS.

**Módulo(s):** ["M16"]. **ROP(s):** [].

- Predicado 0: Comportamento técnico mantém habilitação original ausente como desconhecida, para projeção atualtrue/false. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 1: Primeiro ato posterior só define habilitação desde sua própria data efetiva. — **PASS NA CAMADA**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: [].
- Predicado 2: Autenticidade/origem documental da habilitação ausente no acervo real. — **BLOQUEADO POR DOCUMENTAÇÃO**. Camadas: ["MOCKMVC_IN_PROCESS_REAL_SERVICES_JPA_MYSQL"]. Limites: ["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."].

**Parte não comprovada:** [{"index":2,"predicate":"Autenticidade/origem documental da habilitação ausente no acervo real.","status":"BLOQUEADO POR DOCUMENTAÇÃO","limits":["External authentic documentary proof remains absent; synthetic mechanisms do not discharge this predicate.","Fonte externa autêntica ausente; fonte sintética não substitui este predicado."]},{"gate":"Chrome","status":"BLOQUEADO AMBIENTE","executed_journeys":0}].

**Rastreabilidade:** [conteúdo integral](RASTREABILIDADE_CANDIDATO04_C03/MATRIZ_198_635_NOVA_DADOS.json#/criteria/634); métodos/recibos/assertions no mesmo registro e no CSV.

