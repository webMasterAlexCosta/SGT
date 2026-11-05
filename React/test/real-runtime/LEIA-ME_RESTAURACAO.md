# Preparar a infraestrutura externa da candidata

> Requisito operacional vigente em 2026-11-03: MySQL exatamente 8.0.45. As referências de versões anteriores preservadas em documentação histórica e seus recibos não autorizam outra versão nesta execução.

Siga o [README atual](README.md) e o [guia de instalação](../../GUIA_INSTALACAO_EXECUCAO.md). O runtime atual requer MySQL **8.0.45**, Java **17**, Redis já preparado e o JAR candidato `0.0.4-operacional-20261101` verificado por hash e manifest.

O procedimento anterior foi preservado byte a byte em [LEIA-ME_RESTAURACAO.original.md](../../documentacao/historico-runtime-8.4.11/LEIA-ME_RESTAURACAO.original.md). Seus comandos de inicialização, versões e caminhos congelados pertencem ao histórico.

O supervisor atual aceita somente `status`, `serve-app` e `stop-app`, usa caminhos privados explicitamente configurados por `SGT_RUNTIME_CONFIG` e nunca provisiona banco/contas ou concede privilégios. Copie os helpers atuais para o ambiente sintético preparado, mantenha os arquivos privados fora da entrega e aponte `SGT_TEST_RUNTIME` para esse diretório. Os clientes HTTP conservam o layout `SGT_TEST_RUNTIME/private`.

As fixtures, contas, sessões e comandos de negócio devem ser preparados e executados com autorização, em sequência. A entrega não distribui datadir, senhas ou cookies. Recibos de outra instalação não fornecem IDs, estado, prontidão ou resultados para a candidata atual. A adaptação do supervisor teve validação local de preparação; não constitui execução real do ambiente.
