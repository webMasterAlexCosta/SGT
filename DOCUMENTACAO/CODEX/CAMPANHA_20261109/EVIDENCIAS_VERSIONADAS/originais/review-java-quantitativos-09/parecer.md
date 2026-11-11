Não identifiquei novos achados P0–P3. **Os três P2 estão encerrados, no alcance dos quatro IDs e da candidata22 examinada.**

A correção valida somas e quantidades em `long` antes da conversão. O409 quantitativo escapa do tratamento que transforma conflitos negociais em pendência. Assim, o excesso rejeita a proposta preservando o estado anterior; o controle no limite exato continua201. A validação do total após todos os itens preserva redistribuições válidas independentemente da ordem. [Implementação](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/ComposicaoContratualTemporalServico.java:387).

| ID | Implementação | Oráculo | Execução atual |
|---|---|---|---|
| CMP-003:003 | Sustentada | Rejeição integral e posterior preservado | **S/E**,2/2 |
| CMP-003:005 | Sustentada | Soma independente, overflow e limite exato | **S/E**,2/2 |
| CMP-006:003 | Sustentada | Snapshots integrais e controles anteriores preservados | **S/E**,4/4 |
| RED-008:004 | Sustentada | Releitura após commit e exatamente um uso correto | **S/E**,1/1 |

Os controles estão em [ReplayQuantitativoOverflowHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ReplayQuantitativoOverflowHttpIT.java:45), [ComposicaoPreservacaoIntegralHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ComposicaoPreservacaoIntegralHttpIT.java:41) e [ReducaoExcepcionalPersistenciaHttpIT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/test/java/com/example/siggtrat/auditoria20261029/ReducaoExcepcionalPersistenciaHttpIT.java:47).

Conferi os sete casos focais,48 testes nas12 classes de regressão relacionadas e725 unitários nas146 classes esperadas, todos vinculados à compilação22. Hashes de fontes, bytecode, XMLs e artefatos conferem; sem falhas, pulos, deriva, OOM ou processos remanescentes.

O delta final corresponde ao snapshot RED e nenhuma fonte de teste mudou entre RED/GREEN. O freeze1358 fontes/28 documentos/2 scripts permanece íntegro. A primeira correção continua registrada como falha.

Revisão somente leitura. Este fechamento não promove os demais critérios, o índice Main19, JAR anterior, cobertura percentual, demais gates ou React/Chrome/LAN. A documentação usada continua sendo corpus sintético.
