O predicado comum de recursos PERF pendente da rodada04 está **suportado no corpus local do lote10**. Identifiquei **um novo P2 no runner**, relativo à restauração dos logs após falha de preparação.

**P2 — configuração de logging pode permanecer ativa se o setup falhar.** [run_unit.py:91](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/run_unit.py:91) ativa `log_min_duration_statement=0`, mas o `try/finally` começa somente na [linha185](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/run_unit.py:185). Falhas intermediárias — por exemplo, divergência de fontes na linha125, falha de checkpoint ou de `Popen` — escapam sem executar o RESET da linha232. Isso deixa logging integral habilitado naquele banco isolado.

A correção deve abranger o intervalo desde a alteração da configuração, com evidência de uma falha controlada anterior ao Maven que ainda restaure o valor. Constatação estática; não injetei falhas.

As métricas conferidas diretamente nos JSONs sanitizados correspondem ao resumo público:

| Evidência | Resultado conferido |
|---|---|
| Janelas HTTP | 45, cinco rotas, três frias e seis quentes por rota |
| Contador SQL | 1.981 execuções reconhecidas nas janelas |
| GC | Duas coletas; 261 ms acumulados |
| Amostras durante requisições | 81 |
| Hikari | Máximo observado: uma conexão ativa; nenhuma aguardando |
| Tomcat | Máximo observado: uma thread ocupada |
| PostgreSQL | Máximo observado por backend: 168 locks concedidos; nenhum aguardando/bloqueado nas amostras |
| Cache | MISS/HIT nas três rotas elegíveis; BYPASS em frequência/exportação |

A coleta usa JMX local no filho próprio e sessões PostgreSQL selecionadas por banco e `ApplicationName` exclusivo. O contador exporta contagens, tempos e verbos permitidos. Não abri logs SQL brutos.

Os lotes [10 instrumentado](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/jar-http-contextual-10/result.json) e [11 padrão](/home/alexcosta/Documentos/Desenvolvimento/ect/GTRAT/SGT/.homologacao/20261109-sgt-final/jar-http-contextual-11/result.json) têm, cada um, 1/1 teste, 146 respostas com status esperado e zero falhas, erros ou skips. Fontes, XMLs e bytecode registrado correspondem à compilação09. O modo padrão registra `NAO_SOLICITADO` nas 45 amostras e não recebe aceite de recursos.

O lote10 comprova **RESET e SHOW −1 no caminho executado**. Isso sustenta sua restauração efetiva, sem cobrir o P2 de setup acima.

O congelamento permaneceu íntegro: 1.350 fontes, seis documentos e quatro scripts; produção e JAR `ae4be259…ba27` preservados. Reutilizo os quatro fechamentos focais da rodada04.

Limites: um usuário, concorrência1 e corpus sintético local; instrumentação interfere na medição; CPU/GC/SQL podem incluir trabalho concorrente do próprio JAR. Filas, locks e esperas são amostrados. Não há conclusão sobre SLA, escala nacional, UI/LAN ou gate global.
