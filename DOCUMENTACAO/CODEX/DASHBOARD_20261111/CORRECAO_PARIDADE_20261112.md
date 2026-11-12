# Correção de paridade das três visões — pausada por franquia

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
