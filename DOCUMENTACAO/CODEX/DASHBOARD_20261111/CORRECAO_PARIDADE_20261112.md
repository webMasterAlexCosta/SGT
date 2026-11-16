# Correção de paridade das três visões — retomada em validação

O usuário rejeitou a entrega `13995677` e confirmou que Resumo, Contratos e Operação precisam seguir o padrão completo de Desempenho. O refinamento anterior não cumpriu esse requisito. A descrição anterior de janelas móveis como decisão humana era uma interpretação indevida: o pedido é de cinco calendários, com Diário por padrão.

Checkpoint inicial: `139956777fef3c8f89a4c4c8abd5576172b87144`, já publicado; árvore limpa antes desta correção.

## Implementação

1. Reutilizar o resolvedor de calendários e a autorização analítica existentes. Acrescentar consulta das visões com o mesmo contrato/prestador/unidade, intervalo e comparação. Preservar os endpoints antigos para compatibilidade.
2. Aplicar os filtros também aos indicadores específicos de cadastro, contratos e operação, mantendo explícita a diferença entre fotografia na referência e fatos do período. Não transformar janela de 90 dias em trimestre por rótulo.
3. Reutilizar o acabamento de Desempenho sem alterar seus oito arquivos: cabeçalho, filtros, indicadores, gráficos, tabelas destacadas, detalhes acessíveis, estado de consulta e exportação. Organizar cada visão por sua decisão gerencial e preservar suas ferramentas específicas.
4. Validar calendários, autorização, filtros combinados, cancelamento, comparação desligada, detalhes e exportações. Conferir as três telas no navegador real e registrar antes/depois.
5. Commit/push, integração dist → static → Maven/JAR e suíte aplicável depois do commit de implementação, com comandos e evidências ligados ao artefato. Não declarar a entrega concluída antes desses passos.

## Limites

Sem alterações de runtime, credenciais ou dados operacionais. Laboratório sintético existente. Suítes pesadas sequenciais, Node 768 MiB, Maven 512 MiB, fork Java 768 MiB/metaspace 256 MiB, cgroups e reserva disponível de 8 GiB, agregado abaixo de 15 GB. Pausa humana quando a franquia semanal atingir 1% restante. Nenhuma retomada fora do turno é garantida sem mecanismo comprovado.

## Checkpoint de pausa — 12/11/2026

Consulta nativa de franquia: 99% usados, 1% restante; redefinição prevista para 16/11/2026 às 09:58:26 (America/Sao_Paulo). Auditoria pausada por instrução humana, sem crédito de reset. A tentativa de agendamento único foi rejeitada pela revisão automática por exigir autorização específica para execução futura; nenhum agendamento foi criado. A autorização foi solicitada no chat.

### Estado preservado

- `0b0d5e91`: implementação das três visões, consulta real e regressões; publicado.
- `4cc692a7`: primeira integração e correção da documentação que atribuía indevidamente o aceite ao usuário; publicado.
- JAR ativo: `JAVA/target/SGT.jar`, SHA-256 `41e4d5f2fc7186f1977789639bc1c526f3cb9043b631ebd99b1864340c30b5e2`, porta 58501, serviço próprio `sgt-sgt-api-final-20261112.service`. Não substituir por build antigo.
- Este checkpoint inclui apenas a correção de dois rótulos de comparação para “Intervalos indicados no recorte”; ela ainda NÃO foi incorporada ao JAR nem revalidada. Meses completos podem ter duração diferente.
- Os oito arquivos de Desempenho foram preservados sem alterações.
- Campanha privada/ignorada: `.homologacao/painel-paridade-20261112/`. Backups de static/target, manifestos de fonte/artefato, logs e fotos estão nessa campanha. Não publicar credenciais, ambiente ou arquivos privados.

### Evidência já observada, com limites

- React: 884 testes/114 arquivos aprovados, seis verificações preparatórias, typecheck e build; pico 781,5 MiB, swap zero (`frontend-completo.log`).
- Maven clean package: 811 testes unitários aprovados; pico 1,2 GiB, swap zero (`java-package.log`). Esses resultados antecedem os dois rótulos deste checkpoint.
- PostgreSQL focal: seis testes de analítico/paridade aprovados (`backend-focal3.log`), anterior aos últimos ajustes de contagem diária por unidade/composição vazia; revalidar fonte atual.
- Navegador real: três visões com Diário padrão e a barra completa; cinco calendários conferidos por HTTP real, totais analíticos/complementares iguais. Fotos `antes-*.png` e `depois-*-viewport.png` para resumo, contratos e operacao.
- A suíte completa de integrações, cenários JAR e recuperação NÃO foi repetida para esta correção. Não usar resultados da entrega rejeitada para aprovar o bytecode novo. Não declarar conclusão ou homologação.

### Retomada necessária

1. Conferir franquia, memória disponível, Git e processos próprios. Preservar serviços/dados alheios. Usar heavy-run.py, orçamento agregado abaixo de 15 GB e validações pesadas sequenciais.
2. Revisar `PainelRepositorio.unidadesRecortadas`: `alertasContratuais` foi preenchido com zero sem cálculo; esclarecer/corrigir sem apresentar valor inventado. Conferir sem base de cobertura, contagem diária e contrato com composição sem empregado, com regressões pertinentes.
3. Recuperar/concluir QA dos filtros reais (contrato/prestador/unidade), comparação desligada/ligada, detalhes, teclado/foco, exportações e larguras nas três visões. A última verificação automatizada de filtros não teve seu resultado consolidado antes da pausa. O explorador avançado de efetivo mantém filtros e períodos próprios explicitamente identificados; verificar que isso não prejudica a paridade solicitada.
4. Commit/push de implementação, nova integração dist → static → wrapper clean package → JAR, preservando backups e novos recibos. Não rerodar `integrar.py` sem adaptar rótulos/manifestos: ele contém a expectativa do artefato anterior e não deve sobrescrever evidências.
5. Repetir a suíte completa aplicável após o commit da implementação, sobre os mesmos bytes: frontend, Java unitário, integrações PostgreSQL/Redis isoladas, helpers e cenários JAR/recuperação. Inventariar novamente (novo PainelVisoesPostgresIT); usar schema isolado criado por Hibernate, sem DDL manual. Cenário JarHttpRealIT exige Redis DB0 exclusivo próprio; preservar o DB0 da UI. Helpers anteriores em `.homologacao/painel-visoes-login-20261112/` contêm hashes/caminhos antigos e precisam revisão antes do uso.
6. Registrar resultados, limites e hashes atuais; atualizar fotos finais das três visões; commit/push da documentação/artefatos públicos, Git limpo e entrega com antes/depois. Não tratar os checkpoints acima como aceite final.

## Retomada autorizada — 16/11/2026

Agendamento único explicitamente autorizado e acionado às 10h05 (Brasília), após confirmação de redefinição e 100% da franquia disponível. Agendamento removido depois do acionamento. Git encontrado limpo em `1fac8fac`; serviços próprios desligados, PostgreSQL18.6 e Redis do laboratório restaurados sem purga. Role limitada/ownership de `sgt_dashboard_lab` novamente conferidos.

O teste novo reproduziu alerta contratual indevidamente zerado. Corrigido o contador por unidade e filtro: contratos ativos vencidos, sem término ou sem efetivo cadastrado contam uma única vez. A UI explicita essa natureza cadastral. Cobertura sem esperados ou com grade incompleta retorna null; a UI/exportação apresenta indisponibilidade, e a média não inclui unidades sem base. Ausência diária e composição sem empregado possuem regressões próprias. Nenhum arquivo de Desempenho foi alterado.

Checkpoint focal: oito testes PostgreSQL e nove React aprovados, typecheck aprovado. A primeira fixture de composição vaga violava a regra de empregado ativo com contrato; corrigida a preparação sintética, sem relaxar a regra ou as assertivas. Logs em `.homologacao/painel-paridade-20261116/`. Suíte completa, integração final e QA das três telas ainda em execução.

Host observado: Node26.13.1/npm12.2.2, Java25.0.5 e wrapper Maven3.9.16. Node26.13.0 continua fixado no manifesto; não foi encontrado entre os executáveis locais conferidos. Não houve migração de manifests/lockfiles nem alteração de instalações globais. Registrar a divergência como limite de ambiente; não alegar revalidação da patch26.13.0 a partir de testes26.13.1.


## Checkpoint antes da correção de desempenho — 16/11/2026

As três telas foram verificadas no JAR real: cinco calendários, filtros, comparação, detalhes por teclado, exportações e larguras 1024/1440/1920. Capturas antes/depois preservadas. React: 886 testes/114 arquivos, seis verificações preparatórias, typecheck/build. Java: 811 unitários; matriz ordinária: 1.589 casos em 373 classes sem falhas/erros/skips; Python: 137. Correspondência do JAR, HTTP real/reinício, relatório multipágina, cancelamento e carga PostgreSQL aprovados.

A recuperação de atomicidade passou, mas o controle de 200 registros com chunk500 excedeu 120 segundos, em duas execuções. Os outros três cenários de recuperação ainda não foram executados. Não declarar a suíte completa aprovada. A reprodução preservou o teste e coletou threads/GC: leituras temporais repetidas, sem OOM/swap e sem bloqueadores no snapshot PostgreSQL. A criação da presença padrão e sua gravação resolvem o mesmo contexto duas vezes sob os mesmos locks; a correção será restrita a esse reaproveitamento local, com autorização, locks e transação preservados.

Este checkpoint versiona os assets do JAR `a98e82ec1bc44f43bd07776a5e29c24aed5b7e9fac66f982f6cc3a36c94afef6` antes de alterar o serviço de frequência. O código do painel e as oito fontes de Desempenho permanecem preservados. [Recibo parcial e falha explícita](PARIDADE_CHECKPOINT_20261116.json). Evidências privadas em `.homologacao/painel-paridade-20261116/`, incluindo a reprodução diagnóstica; não sobrescrever.

### Primeiro ajuste de fechamento e falha seguinte

Checkpoint `b39215d2` enviado antes da alteração. `FrequenciaServico` passou a reutilizar o contexto temporal somente entre a criação da presença implícita e a gravação do mesmo item, dentro da mesma transação e sob os locks canônicos. Alterações explícitas e escrita individual mantêm resolução própria; autorização, locks e commit do bloco preservados. Os 811 unitários passaram.

JAR focal `3784a922b9ff588b23aaf7c368f38fab998bc62a560c84e4493daf1c6847e8d6`: os 200 registros concluíram em 115.448 ms, dentro dos 120 s. O teste completo ainda falhou: o segundo POST idêntico excedeu o timeout HTTP de 15 s. O caminho de fechamento concluído conta pendentes reconstruindo o histórico de todos os vínculos antes de excluir quem já possui frequência. A próxima correção antecipará somente essa exclusão definitiva, mantendo as demais validações e a rechecagem posterior. Evidência integral preservada em `.homologacao/fechamento-regressao-20261116/privado/controle-200-evidencias/`; não declarar a suíte aprovada.
