# UX do painel gerencial completo — 12/11/2026

O pedido humano foi esclarecido: melhorar a experiência do **painel analítico inteiro de cinco períodos**, já implementado, abrangendo os blocos A–H do texto de referência. A primeira iteração, limitada às três barras de metas, foi insuficiente e fica registrada apenas como histórico. Esta entrega cobre a composição completa, preservando os contratos e as decisões negociais vigentes.

## O que mudou

- Navegação entre as visões com controles maiores e estado selecionado claro; modos anteriores preservados.
- Filtros separados por período/data/comparação e recorte organizacional, com espaçamento, rótulos e foco consistentes.
- Intervalo atual, período equivalente, escopo e horário real de atualização visíveis junto ao resumo.
- Quatro KPIs com hierarquia tipográfica, total em destaque e indicação de acesso ao detalhamento. A ação não promete registros identificados a perfis que só podem ver agregados.
- Evolução como gráfico principal, com altura de 300 px, eixos mais limpos, comparação legível e tabela acessível preservada; metas na coluna complementar.
- Heatmap com células maiores, cabeçalhos legíveis, identificação fixa da unidade durante rolagem horizontal e significado das cores preservado.
- Funil diário com etapas e números destacados, separando a pendência de lançamento da falta do trabalhador.
- Diagnóstico e alertas em blocos distintos; linhas de tabelas alternadas, hover, busca e controles mais legíveis. Paginação compartilhada mantida.
- Detalhamento lateral com fundo neutro e tabelas tipográficas consistentes; Enter, Escape e restauração do foco preservados.

Não foram alterados cálculos, classificação temporal, metas propostas, regras de autorização, endpoints, exportadores, banco ou dependências. O [contrato vigente](CONTRATO.md) continua sendo a fonte de negócio; o texto anexado contextualiza a abrangência visual e não reverte decisões implementadas posteriormente.

## Evidências

Baseline de 23 arquivos do painel preservada antes da alteração, em `.homologacao/painel-ux-completo-20261112/`. Testes da iteração anterior foram reutilizados como baseline somente após conferência dos bytes. Após esta alteração passaram 875 testes React/111 arquivos, typecheck/build e 808 testes unitários Java. Um ajuste final na altura do gráfico eliminou os avisos de dimensão inicial do Recharts; o build contém a altura final e 16 testes pertinentes foram rechecados depois. Sem script de lint.

No navegador, os cinco períodos foram reconciliados com a API real na referência 11/11: diário 1, semanal 48, mensal 162, trimestral 189 e anual 189 registros. Exercitados quatro seletores de métricas, filtros contrato/unidade, limpeza, comparação ligada/desligada, 11 origens de detalhamento por teclado, célula do heatmap com denominador, três visões de diagnóstico e busca. Uma combinação de contrato/unidade incompatível respondeu 403, conservando a sessão; a limpeza recuperou a consulta. Os seletores das seis visões anteriores continuaram acessíveis, sem reivindicar nova homologação de todos os seus cálculos.

Exportações reais XLSX/PDF concluídas; XLSX conferido como ZIP íntegro e PDF pela assinatura. Não houve alteração do renderizador de exportação. Sem overflow horizontal no painel em 1280, 1440 e 1920 px e com zoom CSS de 125%; zoom nativo do navegador não foi ensaiado. Capturas completas usam a mesma largura de 1440 px de viewport (1080 px de painel), referência e valores. O viewport alto é utilizado para registrar todos os blocos sem cortar o contêiner rolável; há também capturas de 1440 × 1000 px.

JAR final: `bdf997e2cec5b5605a64f29b541ead4ea0c5aaa693eb1be67c757011f85cccc4`, ativo no laboratório na porta 58501. Os 866 bytecodes Java são idênticos aos da versão anterior; ITs de negócio não foram repetidos. Inventário estático do JAR exatamente igual ao fonte: 111 arquivos atuais e 692 anteriores preservados. Todos os 111 atuais foram conferidos por HTTP com hash; cinco rotas SPA, recusa anônima 401 e bootstrap CSRF 200 passaram. Recarga final autenticada: zero erros e avisos de console.

Build com template público explícito, backup dos assets antes da sincronização e comandos da matriz do projeto. Suítes pesadas sequenciais em cgroup de 2 GiB, serviços com teto somado de 4 GiB, swap zero e reserva de entrada de 8 GiB disponíveis; abaixo do limite agregado de 15 GB.

[Recibo sanitizado com hashes e checks](PAINEL_UX_COMPLETO_20261112.json). Capturas e logs completos permanecem no diretório local ignorado da campanha. Scripts de navegador tiveram ajustes de seletor de busca (`searchbox`) e fechamento do aviso de desenvolvimento; essas tentativas não foram tratadas como defeitos do produto.

O commit `23dd2437db64036a0287365a183ab4e077df9054` da auditoria/documentação já foi publicado. Este refinamento visual posterior permanece local. Sem nova revisão independente ou liberação de produção.
