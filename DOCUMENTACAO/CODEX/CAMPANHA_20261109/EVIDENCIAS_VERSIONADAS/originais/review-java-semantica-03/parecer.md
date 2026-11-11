Há **três achados P2**, abrangendo quatro IDs. Examinei os70 IDs; nenhum ficou sem exame de implementação e oráculo.

1. **[P2] Overflow ainda é possível no replay — CMP-003:003 e CMP-003:005.** A preparação usa `long`, mas [o replay soma em `int`](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/ComposicaoContratualTemporalServico.java:473) e [a versão contratual repete essa soma](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/AditamentoContratualServico.java:717).

   Contraexemplo estático: publicar100 em50/50; reduzir excepcionalmente para20/0 com concessão válida; registrar depois aumento para20/10, total30, conservando flag, fundamento e confirmação; incorporar entre esses atos um aumento para2.147.483.642/0. O replay posterior produz soma real2.147.483.652, mas `int` resulta em **−2.147.483.644**. A guarda de piso é dispensada pelos metadados do instrumento cujo total original30 era inferior ao piso75, conforme [o predicado de exceção](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/ComposicaoContratualTemporalServico.java:481). Cada quantidade individual permanece representável. Falta validar a soma real antes de convertê-la e tratar o posterior incompatível. **Não executei esse cenário**; os testes existentes cobrem overflow na entrada, sem essa combinação de replay.

2. **[P2] Preservação integral não está coberta pelo oráculo — CMP-006:003.** [ReplayCapacidadeDependenteHttpIT:69](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ReplayCapacidadeDependenteHttpIT.java:69) confere eventos, composições, remanejamento e `composicaoInicialId`. [CancelamentoCapacidadeContratualHttpIT:70](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/CancelamentoCapacidadeContratualHttpIT.java:70) acrescenta campos do vínculo, mas não compara o Contrato e a Pessoa integralmente. Falta um antes/depois desses registros após a negativa. Não identifiquei mutação indevida na implementação; a lacuna é de regressão.

3. **[P2] A exceção autorizada é conferida somente na resposta — RED-008:004.** [ContratosRegrasHttpIT:290](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosRegrasHttpIT.java:290) verifica201, flag, fundamento e responsável no DTO retornado. Falta releitura persistida após o commit, incluindo instrumento e uso da concessão. O oráculo atual não demonstra integralmente o predicado “persiste”.

O resultado individual é **11 S/E,55 S/P,1 L/E parcial e3 L/P**. Nos dois IDs com unitário parcial, a execução matemática não foi promovida a execução do fluxo HTTP.

Na tabela, **I/O** separa implementação e oráculo: `S` sustentado; `L` lacuna. Execução: `P` pendente neste parecer; `E7` e `E8` são os lotes atuais `it-cargo-final-07/08`; `M11` é execução matemática em `unit-cargo-final-11`. `E7*` indica casos executados com oráculo parcial.

As referências de implementação são:

| Ref. | Fonte |
|---|---|
| C | [ComposicaoContratualServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/ComposicaoContratualServico.java:167): criação, IDs, duplicidade, lock do contrato e excedentes |
| P | [ContratoServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/ContratoServico.java:404): publicação e soma real |
| A | [AditamentoContratualServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/AditamentoContratualServico.java:744): deltas, total, piso, impacto, exceção e token |
| R | [RemanejamentoContratualServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/RemanejamentoContratualServico.java:289): soma exata e redistribuição |
| T | [ComposicaoContratualTemporalServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/ComposicaoContratualTemporalServico.java:279): replay, restauração do estado e conflitos |
| H | [ImpactoProgramacaoComposicaoServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/ImpactoProgramacaoComposicaoServico.java:88): fronteiras, ocupação e dependências futuras |
| V | [AlocacaoVinculoServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/AlocacaoVinculoServico.java:448) e [VinculoContratualServico](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/VinculoContratualServico.java:846): zero, consentimento, autoria e prova |
| F | [RegraEfetivoContratual](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/regra/RegraEfetivoContratual.java:15): piso racional sem ponto flutuante |

As referências dos asserts usam estas abreviações; os números após `:` indicam a linha:

| Ref. | Teste |
|---|---|
| D | [ContratosCrudHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosCrudHttpIT.java:18) |
| K | [ContratosCriteriosComplementaresHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosCriteriosComplementaresHttpIT.java:222) |
| PC | [PessoasComposicaoConsentimentoHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/PessoasComposicaoConsentimentoHttpIT.java:29) |
| CE | [ContratosCriteriosEspecificosHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosCriteriosEspecificosHttpIT.java:227) |
| CP | [ComposicaoPublicacaoConcorrenciaHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ComposicaoPublicacaoConcorrenciaHttpIT.java:50) |
| G | [ContratosRegrasHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosRegrasHttpIT.java:207) |
| Q | [ConclusaoAditamentoRetificacaoQuantitativaHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ConclusaoAditamentoRetificacaoQuantitativaHttpIT.java:25) |
| AM | [AditamentosRemanejamentosHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/AditamentosRemanejamentosHttpIT.java:13) |
| NE | [ConclusaoEfetivoContratoHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ConclusaoEfetivoContratoHttpIT.java:20) |
| IC | [ImpactoCapacidadeLimitesHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ImpactoCapacidadeLimitesHttpIT.java:15) |
| RD | [ReplayCapacidadeDependenteHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ReplayCapacidadeDependenteHttpIT.java:14) |
| TL | [ContratosTemporalLacunasHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosTemporalLacunasHttpIT.java:19) |
| CN | [CancelamentoCapacidadeContratualHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/CancelamentoCapacidadeContratualHttpIT.java:18) |
| RC | [RetificacaoCapacidadeContratualHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/RetificacaoCapacidadeContratualHttpIT.java:13) |
| PV | [PessoasVinculosContratoHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/PessoasVinculosContratoHttpIT.java:512) |
| PR | [PessoasRegrasOriginaisHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/PessoasRegrasOriginaisHttpIT.java:269) |
| PI | [ContratosPisoReplayHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ContratosPisoReplayHttpIT.java:14) |
| U | [RegraEfetivoContratualTest](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/regra/RegraEfetivoContratualTest.java:12) |
| UM | [ConclusaoEfetivoContratualTest](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/regra/ConclusaoEfetivoContratualTest.java:12) |

Todos os IDs abaixo pertencem a `CANONICAL198`. Os predicados literais estão no [input examinado](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/review-java-semantica-03/input.json).

| ID | I/O | Execução | Implementação; asserts |
|---|---|---|---|
| CMP-001:001 | S/S | P | C; D:35–43 |
| CMP-001:002 | S/S | P | C; K:222 |
| CMP-001:003 | S/S | P | C/V; PC:29 |
| CMP-002:001 | S/S | P | C; D:38 |
| CMP-002:002 | S/S | P | C; D:43 |
| CMP-002:003 | S/S | P | A/R; CE:227 |
| CMP-002:004 | S/S | P | C/P; CP:50 |
| CMP-003:001 | S/S | P | P; D:52 |
| CMP-003:002 | S/S | P | A; K:275 |
| CMP-003:003 | L/L | P | A/T; G:297, Q:76; achado1 |
| CMP-003:004 | S/S | P; M11 parcial | R/T; AM:89, CE:152, U:24 |
| CMP-003:005 | L/L | P | P/A/T; NE:20/47; achado1 |
| CMP-004:001 | S/S | P | C/T; AM:41 |
| CMP-004:002 | S/S | P | V; G:207 |
| CMP-004:003 | S/S | P | C/T; G:207, AM:41 |
| CMP-005:001 | S/S | P | T; G:207 |
| CMP-005:002 | S/S | P | T; G:207 |
| CMP-005:003 | S/S | P | T/V; G:245–248 |
| CMP-005:004 | S/S | P | A/T; G:214–244, AM:64, CE:110 |
| CMP-006:001 | S/S | E7 | A/H; RD:14/19; complementar IC:15 |
| CMP-006:002 | S/S | E7 | A/R/H; RD:55–67; AM:168, TL:19 |
| CMP-006:003 | S/L | E7* | A/R/H; RD:69, CN:70; achado2 |
| CMP-006:004 | S/S | P | A/H; K:287 |
| CMP-007:001 | S/S | E7 | A/R/H; RD:14/19 |
| CMP-007:002 | S/S | P | A; K:287 |
| CMP-007:003 | S/S | E7/E8 | A/R/H; RD:56, RC:13/39 |
| CMP-007:004 | S/S | E7 | A/R/H; RD:76; complementar CN:79 |
| CMP-008:001 | S/S | P | V; PV:553 |
| CMP-008:002 | S/S | P | V; PV:559, PC:62 |
| CMP-008:003 | S/S | P | V; PV:576–583, PC:62 |
| CMP-009:001 | S/S | P | V; PV:512 |
| CMP-009:002 | S/S | P | V; PC:43 |
| CMP-009:003 | S/S | P | V; PC:62 |
| CMP-009:004 | S/S | P | C/V; G:252, PR:269/468 |
| CMP-010:001 | S/S | P | V; PV:553 |
| CMP-010:002 | S/S | P | C/V; G:252 |
| CMP-010:003 | S/S | P | V; G:217–223 |
| CMP-011:001 | S/S | P | C; G:262–265 |
| CMP-011:002 | S/S | P | C/T/V; G:266 |
| CMP-011:003 | S/S | P | C/V; G:267, PR:468 |
| CMP-011:004 | S/S | P | C/A/T; K:311 |
| RED-001:001 | S/S | P | A/F; K:329 |
| RED-001:002 | S/S | P | A/F; AM:13 |
| RED-001:003 | S/S | P | A/T/F; PI:39 |
| RED-002:001 | S/S | P | A/F; AM:41 |
| RED-002:002 | S/S | P | A/F; AM:56–60 |
| RED-002:003 | S/S | E7 | A/H; RD:14; AM:168, K:287 |
| RED-003:001 | S/S | P | A/F; AM:41 |
| RED-003:002 | S/S | P | A/R/H; AM:168, TL:19 |
| RED-003:003 | S/S | E7 | A/R/H; RD:14/19 |
| RED-004:001 | S/S | P | A; AM:13 |
| RED-004:002 | S/S | P | A/F; AM:16–28 |
| RED-004:003 | S/S | P | A/T; AM:13, K:329, G:297 |
| RED-005:001 | S/S | P | A/F; AM:30–37 |
| RED-005:002 | S/S | P | A; G:277 |
| RED-005:003 | S/S | P | A; G:286–293 |
| RED-006:001 | S/S | P | A/F; AM:13 |
| RED-006:002 | S/S | P; M11 parcial | A/F; AM:25–28, U:12 |
| RED-006:003 | S/S | P | A/T; Q:25, PI:14 |
| RED-006:004 | S/S | P | A/T/F; PI:14/88 |
| RED-006:005 | S/S | M11 | F; UM:12 e oráculo BigInteger; NE:31 pendente |
| RED-007:001 | S/S | M11 | F; U:19; AM:41 pendente |
| RED-007:002 | S/S | M11 | F; U:12 |
| RED-007:003 | S/S | P | A/F; AM:56–60 |
| RED-007:004 | S/S | M11 | F; UM:12 |
| RED-008:001 | S/S | P | A; G:283 |
| RED-008:002 | S/S | P | A; G:285 |
| RED-008:003 | S/S | P | A; G:287–288 |
| RED-008:004 | S/L | P | A; G:290–293; achado3 |
| RED-008:005 | S/S | P | A/T; PI:88 |

Conferi fonte, compilação19, hashes dos recibos/XMLs e ausência de falhas, pulos, deriva e OOM nos vínculos atuais usados acima. As execuções antigas serviram como localizadores, sem promoção ao Main atual.

O ajuste de JaCoCo está correto no alcance examinado: flags conferidas no descriptor do plugin0.8.15; arquivo exclusivo por lote; `append=false`; ausência em sucesso não compilação resulta em código83. A primeira coleta confere com fonte/bytecode e contém **uma sessão, um header e134.030 bytes**, com tempos dentro do lote. O caminho83 foi conferido estaticamente; nenhum percentual de cobertura foi concedido.

O freeze1354 fontes/28 documentos/2 scripts e o bytecode conferem. O índice nominal regenerado corresponde ao Main19. Revisão somente leitura; autenticidade documental externa, JAR novo, demais gates, UI/LAN e homologação global permanecem fora deste aceite.
