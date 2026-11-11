**P3 encerrado. Nenhum novo achado P0–P3 neste recorte.**

A correção em [OcorrenciaServico.java:157](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/OcorrenciaServico.java:157) preserva a `IOException` como causa, mantendo a mensagem existente. Não acrescenta logging bruto nem altera regras de negócio.

Conferi fontes, classes, recibos e XMLs preservados, sem executar suítes:

- **RED:** 2 falhas por causa ausente entre 3 testes; controle de preservação aprovado.
- **GREEN:** **10/10 aprovados em 3 classes**, vinculados à compilação 25. Os três testes novos usam filesystem temporário real e verificam falha na raiz, falha no staging e inicialização repetida preservando conteúdo.
- **Delta 24 → 25:** somente uma linha produtiva alterada e um teste acrescentado. As outras **1.358 fontes e 1.721 classes anteriores** permanecem idênticas. A diferença no bytecode normalizado de `OcorrenciaServico` limita-se a `inicializar`. Nenhuma divergência de hashes, deriva, OOM ou processo remanescente nos recibos conferidos.

É tecnicamente válido reutilizar as provas anteriores **já examinadas**, nos respectivos caminhos e limites, como **COMPATÍVEIS APÓS DELTA DE DIAGNÓSTICO**. Elas continuam sendo execuções dos bytes anteriores; não devem ser registradas como executadas na compilação 25. Critérios pendentes ou parciais mantêm essa classificação. O novo caminho de falha tem comprovação própria no GREEN.

Evidências: [manifesto conferido](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/review-java-diagnostico-10/source-manifest.json), [vínculo do delta](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/review-java-diagnostico-10/delta-binding.json) e [recibo GREEN](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/upload-cause-green-01/result.json).

Este parecer não comprova partida real do Spring/JAR sob falha de filesystem, não promove o JAR antigo a artefato novo e não aprova os 124 ITs restantes, frontend ou gates globais.
