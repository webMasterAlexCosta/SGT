# SGT — arquivo e recuperação

Antes da limpeza foi criado o commit de segurança `d8c415f46f0942df1dc00b8ced1e55debf4792da` na branch main. Ele preserva o estado rastreado anterior à reorganização, inclusive os gerados que não pertencem à fonte.

`JAVA/target/` continha classes, cobertura, relatórios e requisições de testes. A cópia local de segurança está em `.homologacao/organizacao-20261107/target-checkpoint.zip`, com manifesto de hashes. Um relatório com token de laboratório preexistente foi preservado separadamente, como arquivo privado 0600; não integra o ZIP nem a documentação pública. O índice local está na mesma pasta privada. Não extrair isso em produção nem publicar o acervo privado.

Para recuperar um arquivo rastreado anterior sem alterar o projeto, use `git show d8c415f4:CAMINHO > /tmp/copia-de-consulta` com o caminho desejado. A restauração da árvore gerada exige um diretório de laboratório isolado, comparação de hashes e observância das proteções do cenário. O histórico Git não foi reescrito.

As cópias Markdown idênticas consolidadas e seus destinos constam em [ORGANIZACAO_20261107.json](ORGANIZACAO_20261107.json). Baselines e evidências únicas permanecem preservados. Conteúdo histórico não é instrução para repetir MySQL ou aprovar o código atual.

Antes de reproduzir cortes antigos, leia [AGENTS Java anterior](../CODEX/AGENTS-JAVA-anterior-migracao-20261106.md) e as proteções específicas da campanha. Elas valem para seus artefatos; não reativam MySQL, Java17/Boot3.5.16 ou a missão de 198/198 no código atual.
