**Parecer17: APROVADO para estender o gate técnico backend16 ao Main36, exclusivamente no escopo sintético delimitado.** Não identifiquei achados P0–P3 neste incremento.

- **Mensagens e contrato:** confirmei que somente os dois textos mudaram. Agora descrevem corretamente o marcador legado, sem prometer liberação de lançamentos. Rotas, status, DTO, persistência e autorização permanecem iguais. [Serviço:156](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/UsuarioAplicacaoServico.java:156) e [controlador:46](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/controlador/UsuarioAplicacaoControlador.java:46).
- **Embalagem:** o confronto integral dos JARs confirmou diferenças somente nessas duas classes e no POM embutido. As outras **821 classes, 190 assets e 116 bibliotecas** são idênticas. O candidato não contém `*.orig`; a fonte de segurança foi preservada. [Exclusão:288](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/pom.xml:288).
- **Realocação do teste:** conteúdo e hash idênticos ao parecer16, agora no diretório correspondente ao package declarado. Nenhuma asserção foi alterada.
- **Verificação:** conferi os XMLs dos **22 casos em três classes**, sem falhas, erros ou skips, vinculados às fontes/classes atuais. A [prova HTTP](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/api-legacy-marker-main36-02/http-proof.json) comprova seis checks em oito chamadas no mesmo JAR, incluindo concessão, revogação e restauração do marcador. Tentativas anteriores falhas permanecem excluídas.

O congelamento final confere, sem divergências: manifesto `f5182656…53cbf0c`, input `6fe924b8…ed37dc` e JAR:

`3c5cd0a6d616737efb3c85b8922294fadf53cfe231022edd1d9503242b7c87c9`

Revisão somente leitura, sem executar testes, builds ou servidores. Mantêm-se as limitações dos pareceres15/16: **sem aprovação de React, Chrome/24 critérios, LAN/outro computador ou homologação integrada/global**. Os percentuais anteriores de cobertura pertencem ao Main35; não constituem cobertura global do Main36.
