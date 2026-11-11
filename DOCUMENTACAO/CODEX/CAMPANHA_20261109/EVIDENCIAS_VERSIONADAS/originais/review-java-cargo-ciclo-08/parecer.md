Não identifiquei novos achados P0–P3 na correção produtiva. **O P2 de ordem/ciclos está encerrado no alcance da fonte19, dos oráculos e das execuções examinadas.**

- **Ciclo de dois atores:** RED confirmou `40P01` e duas arestas físicas. No GREEN, Cargo retorna409 com `55P03`, o Admin confirma200 e o rollback preserva integralmente o negócio. O [NOWAIT restrito a Cargo](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/AcessoOrganizacionalServico.java:62) mantém a revalidação de perfil, escopo e atividade; o comportamento padrão permanece bloqueante.
- **Ciclo transitivo:** RED confirmou três arestas e `40P01`. Com a [ordem atual do PUT](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/JAVA/src/main/java/com/example/siggtrat/servico/CargoServico.java:96), o Admin confirma e é relido antes de liberar C; A/C recebem403, sem alterações negociais ou deadlock observado.
- **CAT-002:002:** permanece suportado. Todos os escritores produtivos de nome passam pelo namespace antes da validação e escrita. As três disputas continuam200+409, com uma identidade, caixa preservada e perdedora intacta.
- **Execuções atuais:**18/18 testes focais e725/725 unitários em146 classes conferem com a compilação19, XMLs e bytecode. Sem deriva, pulos, OOM ou processos remanescentes.

O freeze1354 fontes/9 documentos/4 scripts permanece íntegro.

Há uma ressalva de evidência: [current-method-trace.json](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/current-method-trace.json) ainda corresponde ao Main15. Excluí seus vínculos do aceite atual; precisa ser regenerado antes de uso como índice da fonte19. O script atualizado e o snapshot `JAVA_CHECKS` conferem no alcance declarado.

Revisão somente leitura. O contador de deadlocks é complementar e não comprova ausência universal. JAR `ae4`, empacotamento, demais gates e homologação global/UI/LAN permanecem fora deste fechamento.
