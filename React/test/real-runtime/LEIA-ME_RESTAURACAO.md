# Histórico de preparação da candidata MySQL

> **Retirado das instruções da campanha atual.** O alvo atual é PostgreSQL **18.6** (`server_version_num=180006`) e pgJDBC **42.7.14**. Não executar os comandos MySQL deste diretório para esse alvo. Consulte o [aviso de alcance no README](README.md): o harness externo de preparação appv2 não substitui estes helpers por código, e recibos/testes MySQL não comprovam PostgreSQL.

## Conteúdo histórico preservado

> Requisito histórico da campanha de 2026-11-03: MySQL exatamente 8.0.45. As referências de versões anteriores preservadas em documentação histórica e seus recibos não autorizam outra versão nesta execução.

Na campanha histórica, o procedimento seguia o [README MySQL](README.md) e o [guia de instalação](../../GUIA_INSTALACAO_EXECUCAO.md). Aquele runtime requer MySQL **8.0.45**, Java **17**, Redis já preparado e o JAR candidato `0.0.4-operacional-20261101` verificado por hash e manifest.

O procedimento anterior foi preservado byte a byte em [LEIA-ME_RESTAURACAO.original.md](../../documentacao/historico-runtime-8.4.11/LEIA-ME_RESTAURACAO.original.md). Seus comandos de inicialização, versões e caminhos congelados pertencem ao histórico.

O supervisor MySQL aceita somente `status`, `serve-app` e `stop-app`, usa caminhos privados explicitamente configurados por `SGT_RUNTIME_CONFIG` e nunca provisiona banco/contas ou concede privilégios. Naquela campanha, os helpers eram copiados para o ambiente sintético preparado, mantenha os arquivos privados fora da entrega e aponte `SGT_TEST_RUNTIME` para esse diretório. Os clientes HTTP conservam o layout `SGT_TEST_RUNTIME/private`.

As fixtures, contas, sessões e comandos de negócio devem ser preparados e executados com autorização, em sequência. A entrega não distribui datadir, senhas ou cookies. Recibos de outra instalação não fornecem IDs, estado, prontidão ou resultados para a candidata atual. A adaptação do supervisor teve validação local de preparação; não constitui execução real do ambiente.
