# Histórico do supervisor MySQL 8.4.11

Este diretório preserva as fontes originais da baseline para consulta. Não é procedimento da candidata. O supervisor anterior está em `manage.py.txt`, como texto sem entrada executável. Sua cópia é byte a byte da baseline, SHA-256 `1b8d15fa505a8f84cd72880a44a43d6c17620ddd0bd47bb94b6d7531923f64fe`.

`README.original.md`, `LEIA-ME_RESTAURACAO.original.md` e `MANIFESTO_ORIGEM.original.json` também conservam os bytes recebidos. As versões, caminhos de JAR congelado, comandos de inicialização e resultados que descrevem pertencem à rodada anterior. O manifesto registra a origem desses arquivos; não atesta a integridade dos helpers atuais.

O supervisor vigente está em `../../test/real-runtime/manage.py`; o procedimento atual está em `../../test/real-runtime/README.md` e `../../GUIA_INSTALACAO_EXECUCAO.md`. Ele aceita somente serviços preparados externamente, MySQL **8.0.46**, Java **17** e o JAR candidato `0.0.4-operacional-20261101`. Não disponibiliza comandos de provisionamento.
