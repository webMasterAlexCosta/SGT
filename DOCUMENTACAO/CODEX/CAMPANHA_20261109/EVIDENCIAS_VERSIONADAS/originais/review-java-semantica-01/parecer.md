Identifiquei **um P2 de evidência em CON-023:001**. Examinei individualmente os 112 critérios; nenhum ficou `NÃO_EXAMINADO`.

A implementação está suportada nos 112 critérios. Os oráculos são suficientes em 111; há uma lacuna em CON-023:001. A execução atual foi conferida para **109 critérios**; três permanecem pendentes.

**P2 — CON-023:001: falta contraprova de mapa parcialmente preenchido.** Em [TransferenciaConcessaoObrigatoriaHttpIT.java:48](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/TransferenciaConcessaoObrigatoriaHttpIT.java:48), o teste remove integralmente `mapeamentos`; seu controle positivo contém uma única composição. Isso comprova mapa obrigatório, mas não discrimina a exigência de cobrir **todas** as composições operacionais. A guarda existe em [TransferenciaGestaoContratoServico.java:539](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/TransferenciaGestaoContratoServico.java:539).

Para fechar o predicado, falta um caso com duas composições operacionais e mapa válido para apenas uma: recusa específica, releitura sem alteração de contrato/eventos/versões/transferência/uso da concessão e controle positivo completando somente o mapa faltante. Não confirmei defeito na implementação dessa guarda.

O [manifesto congelado](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/review-java-semantica-01/source-manifest.json) permaneceu íntegro: **1.343 fontes e 25 documentos**, SHA-256 `e40fb4a490420e6f8aa633b64556730da1a79f183f88c558618728644c4d8418`. As seis fontes de harness excluídas foram respeitadas.

Conferi os recibos/XML sanitizados dos vínculos utilizados, incluindo hashes das fontes de produção, testes e bases comuns. O recibo novo [it-contextual-restante-07/result.json](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/it-contextual-restante-07/result.json) fecha a execução de CON-018:003: caso real executado, sem falhas, erros ou skips; recibo SHA-256 `a9e7a848cd7de21c3877ea9bdb8996c8164799ffd1a6a0036a200257cf5c27da`.

No quadro, `S/E` significa **SUPORTADO_NO_BACKEND_ATUAL / ATUAL_CONFERIDA**; `S/P`, **SUPORTADO_MAS_EXECUÇÃO_PENDENTE / PENDENTE**; `L/P`, **LACUNA no oráculo / PENDENTE**. As referências usam `sigla:linha`. Todos os IDs pertencem ao namespace `CANONICAL198::`.

Fontes de implementação:

| Sigla | Arquivo |
|---|---|
| C | [ContratoServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/ContratoServico.java) |
| M | [MotorTemporalContratoServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/MotorTemporalContratoServico.java) |
| I | [CorrecaoIdentificadorContratoServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/CorrecaoIdentificadorContratoServico.java) |
| T | [TransferenciaGestaoContratoServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/TransferenciaGestaoContratoServico.java) |
| V | [VinculoContratualServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/VinculoContratualServico.java) |
| A | [AditamentoContratualServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/AditamentoContratualServico.java) |
| R | [RemanejamentoContratualServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/RemanejamentoContratualServico.java) |
| F | [RevalidacaoFrequenciaTemporalServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/RevalidacaoFrequenciaTemporalServico.java) |
| O | [AcessoOrganizacionalServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/AcessoOrganizacionalServico.java) |
| Ent | [Contrato](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/entidade/Contrato.java) |

Fontes dos oráculos examinados:

| Sigla | Teste |
|---|---|
| D | [ContratosCrudHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosCrudHttpIT.java) |
| E | [ContratosCriteriosEspecificosHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosCriteriosEspecificosHttpIT.java) |
| K | [ContratosCriteriosComplementaresHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosCriteriosComplementaresHttpIT.java) |
| G | [ContratosRegrasHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosRegrasHttpIT.java) |
| Q | [ContratosCompletudeHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosCompletudeHttpIT.java) |
| L | [CicloVidaContratosHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/CicloVidaContratosHttpIT.java) |
| LF | [ContratosLacunasFuncionaisHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosLacunasFuncionaisHttpIT.java) |
| SC | [ContratosEscopoHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosEscopoHttpIT.java) |
| U | [ContratoTemporalidadeEtapa02Test](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/contrato/ContratoTemporalidadeEtapa02Test.java) |
| OP | [ConclusaoPublicacaoCamposObrigatoriosHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ConclusaoPublicacaoCamposObrigatoriosHttpIT.java) |
| PP | [ConclusaoPublicacaoPreviaHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ConclusaoPublicacaoPreviaHttpIT.java) |
| TL | [ContratosTemporalLacunasHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosTemporalLacunasHttpIT.java) |
| AF | [ContratoAnulacaoFuturaHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratoAnulacaoFuturaHttpIT.java) |
| AC | [ConclusaoCorrecaoAnulacaoOperacaoHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ConclusaoCorrecaoAnulacaoOperacaoHttpIT.java) |
| CF | [ConclusaoCancelamentoFuturoHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ConclusaoCancelamentoFuturoHttpIT.java) |
| CT | [ConclusaoTransferenciaContratoHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ConclusaoTransferenciaContratoHttpIT.java) |
| CAT | [CatalogosTransferenciaLacunasHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/CatalogosTransferenciaLacunasHttpIT.java) |
| FOTO | [ContratoCatalogoFotografiaHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratoCatalogoFotografiaHttpIT.java) |
| VT | [ConclusaoAud03VinculosTransferenciaProgramadaHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ConclusaoAud03VinculosTransferenciaProgramadaHttpIT.java) |
| TC | [TransferenciaConcessaoObrigatoriaHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/TransferenciaConcessaoObrigatoriaHttpIT.java) |
| TM | [TransferenciaPoliticaMatriculaHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/TransferenciaPoliticaMatriculaHttpIT.java) |
| AN | [FrequenciaAnulacaoContratoPreservacaoHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/FrequenciaAnulacaoContratoPreservacaoHttpIT.java) |
| ID | [ConclusaoAud04IdentidadeContratualHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ConclusaoAud04IdentidadeContratualHttpIT.java) |

| ID | Implementação | Oráculo efetivamente examinado | Estado |
|---|---|---|---|
| GOV-003:001 | C:518 | D:52 — DELETE publicado 409, preservação | S/E |
| GOV-003:002 | A:315; R:196 | E:110,152 — três versões, IDs e metadados preservados | S/E |
| GOV-003:003 | A:340; R:215 | K:357 — CANCELA preserva autor, motivo, datas e alvo | S/E |
| GOV-004:001 | C:518 | D:18 — descarte 204 de rascunho | S/E |
| GOV-004:002 | C:518 | D:18 — GET após descarte 404 | S/E |
| GOV-004:003 | C:518 | E:48 — empregado, histórico e evento impedem descarte | S/E |
| GOV-004:004 | C:518 | E:86 — falha após exclusão restaura contrato e composição | S/E |
| CON-001:001 | C:1205; Ent:12 | G:64 — duplicidade na mesma Gestão 409 | S/E |
| CON-001:002 | C:1205; Ent:12 | G:64 — duplicidade entre Gestões 409 | S/E |
| CON-001:003 | C:1205; Ent:12 | K:34 — número igual, anos distintos, dois registros | S/E |
| CON-001:004 | Ent:12 | U:72 — nome e colunas da restrição JPA | S/E |
| CON-002:001 | C:313 | D:18 — PUT altera número/ano do rascunho | S/E |
| CON-002:002 | C:313,1205 | E:29 — recusa e releitura dos dois contratos inalterados | S/E |
| CON-002:003 | C:313 | D:52 — PUT publicado 409 | S/E |
| CON-003:001 | C:313 | D:52 — edição comum publicada bloqueada | S/E |
| CON-003:002 | I:60 | D:127 — correção sem concessão 403 | S/E |
| CON-003:003 | I:60; M:329 | D:129; ID:153 — antes/depois, histórico e cortes efetivo/conhecimento | S/E |
| CON-003:004 | I:60 | D:135 — pesquisa pela identidade anterior | S/E |
| CON-003:005 | I:60 | D:132 — mesmo ID e uma única correção | S/E |
| CON-004:001 | C:518,1205 | D:18; ID:133 — identidade nunca publicada liberada | S/E |
| CON-004:002 | C:285,518 | D:18; ID:133 — recriação com outro ID | S/E |
| CON-004:003 | C:518 | D:52 — publicado continua inexcluível | S/E |
| CON-005:001 | C:1385; M:577 | Q:89 — campos e três eixos conferidos | S/E |
| CON-005:002 | C:929 | Q:129 — dois contratos discriminam todas as rotas relacionadas | S/E |
| CON-005:003 | M:577 | G:152; CAT:89; Q:19 — empresa, tipo e vigência datados | S/E |
| CON-006:001 | C:285,1205 | OP:42 — ausência isolada de início/fim aceita no rascunho | S/E |
| CON-006:002 | C:479 | OP:56 — publicação recusa cada data ausente, sem fatos parciais | S/E |
| CON-006:003 | C:1205 | K:50 — fim anterior ao início rejeitado | S/E |
| CON-006:004 | C:348; M:286 | Q:89 — datas publicadas conservadas | S/E |
| CON-007:001 | M:577,728 | U:57; L:37 — VIGENTE e INATIVO coexistem | S/E |
| CON-007:002 | M:577 | U:21; TL:51 — passagem do tempo sem escrita de fatos | S/E |
| CON-007:003 | C:773,819; M:728 | U:38; L:140; G:81 — cancelamento/anulação explícitos | S/E |
| CON-007:004 | C:1385; M:577 | K:162; LF:106 — detalhe e posição concordantes | S/E |
| CON-008:001 | C:479 | OP:56 — omissões isoladas e controles positivos | S/E |
| CON-008:002 | C:404 | D:18,52 — duplicidade e soma divergente recusadas | S/E |
| CON-008:003 | C:348; M:286 | OP:82; D:78 — evento, fotografia, autoria e base original | S/E |
| CON-008:004 | M:286,577 | FOTO:15,122 — valores completos preservados após mudar mestres | S/E |
| CON-009:001 | O:278; C:285 | SC:30; D:18 — mutações próprias permitidas, externas recusadas | S/E |
| CON-009:002 | O:278 | D:89; SC:30 — Estadual lê seu escopo e não escreve | S/E |
| CON-009:003 | O:278 | D:89; SC:30 — escopo externo 404 | S/E |
| CON-009:004 | T:119; M:577 | D:142 — origem antes, destino após a data | S/E |
| CON-010:001 | C:583 | L:37 — intervalo sem mudar vigência ou vínculo | S/E |
| CON-010:002 | C:583; M:728 | LF:145 — efeito futuro não antecipado; bloqueio ao chegar a data | S/E |
| CON-010:003 | C:626 | L:37 — reativação explícita, mesmo vínculo e intervalo preservado | S/E |
| CON-010:004 | C:583 | L:17 — nova frequência invalida prévia anterior | S/E |
| CON-011:001 | M:577 | G:345 — descrição do ADI programado durante inatividade | S/E |
| CON-011:002 | M:577 | G:345 — distribuição do REM programado durante inatividade | S/E |
| CON-011:003 | M:728 | G:345 — habilitação permanece INATIVO | S/E |
| CON-011:004 | C:583; M:728 | G:345 — vínculo no intervalo bloqueado 409 | S/E |
| CON-012:001 | A:1099; M:577 | G:392; Q:19 — extensão torna a posição VIGENTE | S/E |
| CON-012:002 | M:728 | G:392 — extensão conserva INATIVO | S/E |
| CON-012:003 | V:657 | Q:19 — término e causa do vínculo anterior preservados | S/E |
| CON-012:004 | C:626; V:657 | Q:19 — retomada exige comando e não reabre vínculo antigo | S/E |
| CON-013:001 | C:313 | D:52 — PUT comum publicado 409 | S/E |
| CON-013:002 | T:329 | TC:36,60 — sem concessão 403; mesma intenção autorizada passa | S/P |
| CON-013:003 | T:174; M:577 | D:142 — Gestão origem antes do alcance | S/E |
| CON-013:004 | T:119; M:577 | D:142; CAT:89 — Gestão e tipo de destino após alcance | S/E |
| CON-013:005 | T:467; M:286 | CAT:105 — nomes históricos preservados após renomeações | S/E |
| CON-014:001 | C:348 | TL:79 — publicação retroativa sem prévia 409 | S/E |
| CON-014:002 | C:348 | PP:70,75 — nova prévia e confirmação publicam | S/E |
| CON-014:003 | C:348 | PP:40,52 — descrição, empresa e composição invalidam token | S/E |
| CON-014:004 | C:348; M:151 | PP:82 — início efetivo original e registro posterior distintos | S/E |
| CON-015:001 | C:819 | K:79 — data do ato/fonte obrigatórias, sem alteração parcial | S/E |
| CON-015:002 | C:819 | G:81; AF:18 — contrato e atos posteriores preservados | S/E |
| CON-015:003 | M:151 | G:81 — motivo, autor, datas e fonte conferidos | S/E |
| CON-015:004 | M:728 | AF:18,73 — posição ANULADO desde o alcance | S/E |
| CON-016:001 | C:690 | L:75 — termo final reduzido à data efetiva | S/E |
| CON-016:002 | C:690 | K:79 — falta de fonte recusa encerramento | S/E |
| CON-016:003 | V:634,763 | L:75 — término no mesmo dia e causa específica | S/E |
| CON-016:004 | C:690; M:577 | L:75 — dia seguinte ENCERRADO | S/E |
| CON-016:005 | V:763 | K:119 — mesmos vínculo/alocação, sem realocação automática | S/E |
| CON-016:006 | C:690 | CT:166 — comparação com vigência prorrogada por ato futuro | S/E |
| CON-017:001 | M:577 | E:110,152 — instrumentos persistidos sem antecipar efeito | S/E |
| CON-017:002 | M:577 | E:110,152 — versões e distribuição corretas na data efetiva | S/E |
| CON-017:003 | A:1099; R:245 | G:27; K:242 — limites da vigência e extensão válida no ADI | S/E |
| CON-017:004 | A:340; R:215 | K:357 — cancelamento futuro preserva original e metadados | S/E |
| CON-018:001 | C:819; V:634 | G:81; AF:73 — vínculo e alocação continuam identificáveis | S/E |
| CON-018:002 | V:634,763 | G:81 — pessoa/vínculo conservam o contrato original | S/E |
| CON-018:003 | C:819; F:149 | AN:28 — frequência/prova preservadas, cálculo válido retirado | S/E |
| CON-018:004 | M:151 | G:81 — origem, fonte e autoria da anulação | S/E |
| CON-019:001 | C:518 | G:81 — DELETE anulado 409 | S/E |
| CON-019:002 | C:1205 | G:81 — identidade do anulado continua reservada | S/E |
| CON-019:003 | M:577,728 | G:81; L:98 — anulação permanece no histórico | S/E |
| CON-020:001 | C:879 | L:98; AC:24 — evento original preservado integralmente | S/E |
| CON-020:002 | C:879 | AC:24 — relação CANCELA aponta para a anulação original | S/E |
| CON-020:003 | C:879; M:728 | LF:106; AF:53; AC:24 — detalhe/posição seguem INATIVO | S/E |
| CON-020:004 | C:626,879 | AC:24 — vínculo recusado sem resíduos; reativação permite inclusão | S/E |
| CON-021:001 | C:773 | K:93 — contrato iniciado não aceita cancelar-futuro | S/E |
| CON-021:002 | C:773; M:728 | K:93; L:140 — cancelamento retorna CANCELADO | S/E |
| CON-021:003 | V:645 | K:93 — programação cancelada não opera | S/E |
| CON-021:004 | C:773; M:151 | K:93 — motivo, autor, datas e origem preservados | S/E |
| CON-021:005 | C:773 | CF:18 — alcance após início recusado; alcance anterior permitido | S/E |
| CON-022:001 | M:577,728 | U:57; L:37 — situação e habilitação independentes | S/E |
| CON-022:002 | M:577,728 | K:162 — pendência não troca estado negocial | S/E |
| CON-022:003 | C:1385; M:577 | K:162; LF:106 — consulta atual/temporal concordante | S/E |
| CON-022:004 | C:583,626; M:728 | G:52; L:37 — JSON/instrumento não substituem evento administrativo | S/E |
| CON-023:001 | T:329,539 | TC:36,48 — concessão e mapa ausente; falta mapa não vazio incompleto | **L/P** |
| CON-023:002 | T:437 | CAT:69,79,89 — tipo ausente/origem recusado; destino válido aceito | S/E |
| CON-023:003 | T:119,197 | VT:36,117 — duas faixas, mesmo vínculo e conhecimento anterior preservados | S/E |
| CON-023:004 | T:467 | CAT:105 — nomes de origem/destino permanecem fotografados | S/E |
| CON-023:005 | T:158; A:890 | TM:30,42,58,74,96,115,131 — vigentes/programados, override e matrícula | S/P |
| CON-023:006 | T:337; M:577 | CT:31 — retificação preserva original e posterior nos dois cortes | S/E |
| CON-023:007 | T:268 | CT:65,92 — cancelamento explícito; posterior incompatível faz rollback | S/E |
| CON-023:008 | T:349 | CT:115,128 — prorrogação permite e encurtamento bloqueia | S/E |
| CON-023:009 | T:370; C:929 | CT:142,219 — composição posterior e duas transferências concorrentes | S/E |
| CON-024:001 | C:819; M:151 | G:81 — data do ato posterior e alcance anterior distintos | S/E |
| CON-024:002 | M:728 | AF:18,38 — dia anterior permanece coberto | S/E |
| CON-024:003 | M:728 | AF:18,38,73 — dia do alcance ANULADO | S/E |
| CON-024:004 | M:151 | G:81 — Instant de registro e autor preservados | S/E |
| CON-025:001 | C:1205 | L:140 — identidade do cancelado não pode ser reutilizada | S/E |
| CON-025:002 | C:518 | K:93 — DELETE cancelado 409 | S/E |
| CON-025:003 | C:773; M:151 | K:93 — história, motivo e autoria consultáveis | S/E |

O alcance é **backend dos 112 IDs**. As execuções examinadas de integração usam MockMvc, serviços/JPA e PostgreSQL real; não comprovam TCP, Chrome, LAN ou UI. Autenticidade documental permanece `CONDICIONAL_DOCUMENTAL/CORPUS` quando exigida pelo fato confrontado; fontes sintéticas não a comprovam. Este parecer não concede gate global, homologação dos 21 módulos ou aprovação dos 635 critérios.
