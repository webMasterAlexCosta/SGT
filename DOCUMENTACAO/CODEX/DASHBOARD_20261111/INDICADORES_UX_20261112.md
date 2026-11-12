# Indicadores de desempenho — refinamento de 12/11/2026

**Iteração parcial, substituída pelo [refinamento do painel completo](PAINEL_UX_COMPLETO_20261112.md).** O usuário esclareceu que o pedido abrangia todos os blocos do painel, não somente estas três barras. Este registro e seus hashes são históricos.

Pedido humano: publicar as correções/documentação da auditoria e melhorar o componente de indicadores, com destaque nas linhas e liberdade de escolha visual; apresentar imagens reais do antes e depois.

As correções da auditoria foram publicadas no commit `23dd2437db64036a0287365a183ab4e077df9054`, na branch `codex/sgt-progresso-seguro-20261111`. Os recibos da auditoria retratam seu fechamento anterior à publicação e permanecem preservados.

## Direção e alcance

Refinamento do bloco **Indicadores de desempenho e metas**, no modo Desempenho do dashboard. Mantém a identidade azul do SGT, os textos de base, a indicação de metas propostas, os valores recebidos e a abertura dos detalhes. Nenhuma alteração de cálculo, autorização, filtros, transporte, API ou banco.

A escolha visual foi uma lista com divisórias horizontais, fundo alternado discreto e destaque de hover/foco. Percentuais maiores, alinhados e com algarismos tabulares facilitam comparar as linhas. A ação “Ver base” deixa explícito que cada linha abre o detalhamento. A barra permanece limitada visualmente a 100%, enquanto o texto preserva o valor recebido; ausência de base continua distinta de zero. Não foram criados semáforos de desempenho ou novas metas de negócio.

Alterações restritas a `BlocosDesempenho.tsx` e seu `index.module.css`; removidas regras antigas que pertenciam à mesma apresentação. Sem dependências novas. A implementação existente e o [contrato do dashboard](CONTRATO.md) são a referência visual e funcional; não há PRODUCT.md/DESIGN.md no projeto.

## Verificação

- Interface real HMR com API Spring e PostgreSQL/Redis do laboratório já autorizado; mesmos dados e viewport de 1440 px no antes/depois. Verificação adicional em 1100 px, sem overflow no componente.
- Os três indicadores abriram os detalhes por Enter e fecharam por Escape. Foco visível preservado; cada linha continua sendo um único botão.
- Imagens e logs locais em `.homologacao/indicadores-ux-20261112/`, fora do Git. Capturas sem alteração artificial dos valores.
- Passaram 875 testes React em 111 arquivos, typecheck/build, sincronização dos assets e package com 808 testes Java. Não há script de lint. O aviso existente do chunk facial acima de 1 MB permanece; o componente de indicadores não acrescenta dependências nem carregamento de bibliotecas.
- Build final executado com `SGT_PUBLIC_ENV_TEMPLATE` apontando explicitamente para `React/.env.example`. Uma tentativa com configuração padrão foi preservada fora do Git; a validação foi repetida com o template público. O primeiro JAR com esse template ainda reteve 50 assets não referenciados no `target` da tentativa anterior e chegou ao smoke local; a API anterior foi restaurada, os resíduos foram retirados exclusivamente do diretório gerado e preservados, e o JAR foi reempacotado antes da entrega. Nenhum desses arquivos residuais integra o JAR final.
- O reempacotamento exclusivo de recursos usou `-DskipTests`; não é contado como nova execução dos testes. As classes de teste permaneceram byte a byte iguais às do package aprovado, assim como os 866 bytecodes de produção em relação ao JAR da auditoria. ITs de negócio não foram repetidos, pois não houve alteração Java/API.
- JAR final `2f2ebceb8fcd7dde177b8ff4555fc9da62aaca8c93dd59b270cfa1a9c0a80bb9`, ativo na porta 58501. Os 753 arquivos estáticos do JAR correspondem exatamente ao diretório fonte: 111 do dist atual e 642 anteriores preservados. Os 111 arquivos atuais foram servidos por HTTP com hash conferido; cinco rotas SPA, recusa anônima 401 e bootstrap CSRF 200 aprovados.
- Na interface do JAR final, os três detalhes abriram por Enter e fecharam por Escape; recarga autenticada, dados da captura e ausência de overflow conferidos. O contraste calculado dos textos nos fundos normal/alternado/hover foi de pelo menos 6,39:1. Capturas finais `antes.png` e `depois.png`, com a mesma largura de componente e os mesmos valores (3,7%, 90% e 88,9%).

Resultados sanitizados e hashes: [recibo desta alteração](INDICADORES_UX_20261112.json). As alterações visuais posteriores ao commit da auditoria permanecem locais, prontas para avaliação; não houve novo commit/push deste refinamento.

Esta alteração não recebe aprovação de produção ou revisão independente por associação aos pareceres anteriores. Suítes pesadas sequenciais, com cgroup de 2 GiB e serviços de até 4 GiB; memória disponível conferida antes dos comandos. Teto agregado humano de 15 GB preservado.
