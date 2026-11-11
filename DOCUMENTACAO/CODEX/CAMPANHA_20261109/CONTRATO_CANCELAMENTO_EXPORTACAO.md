# Cancelamento de exportação assíncrona

Complemento do ARQ-007 do mestre vigente, no laboratório desta campanha. Implementação em validação; este contrato não concede aprovação independente ou homologação.

`POST /api/relatorios/exportacoes/{id}/cancelamento` usa a sessão, CSRF e proteção existentes. Somente o proprietário com autorização atual para a exportação pode executar o comando. Outro usuário recebe403; recurso inexistente recebe404. Não há poder adicional para Administrador cancelar tarefas de terceiros.

Uma tarefa `NA_FILA` ou `PROCESSANDO` passa para `CANCELADO`, com HTTP200 e o mesmo `ExportacaoRelatorioStatusDTO`. Repetir o comando nessa tarefa retorna o terminal sem reiniciar trabalho ou duplicar a transição. `concluido`, `falhou` e `podeBaixar` ficam falsos. Consulta e transferência mantêm a verificação de proprietário e autorização atual; download da tarefa cancelada recebe409.

O cancelamento é cooperativo: o executor confere o estado antes de iniciar, a cada lote e antes de publicar o arquivo. A transição de conclusão disputa o mesmo monitor da transição de cancelamento. Um arquivo que terminar depois do cancelamento é removido pelos mecanismos de armazenamento existentes e não pode republicar `CONCLUIDO`. Não há interrupção de threads alheias nem alteração de schema, executor ou dependências.

Tarefas já concluídas, em download, baixadas ou falhas recebem409 ao tentar cancelar; o arquivo válido e seu estado são preservados. A interrupção de uma transferência já concluída continua no contrato anterior: permite nova tentativa de baixar o arquivo íntegro, sem repetir sua geração.

Na futura etapa React, `CANCELADO` deve ser reconhecido como terminal, encerrar o polling e preservar a identidade da operação/sessão. O botão de cancelamento aplica-se às etapas em fila/processamento; respostas403/409 seguem o tratamento específico do fluxo. A integração frontend permanece pendente até seu próprio gate.

Provas planejadas/registradas: RED HTTP por rota ausente e publicação tardia; unitários de cancelamento na fila/progresso; API real com gerador XLSX, executor e arquivo, proprietário versus terceiro, repetição, limpeza tardia e controle normal. Resultados e hashes ficam no checkpoint e nos recibos específicos; preparação ou teste reprovado não é aceite.
