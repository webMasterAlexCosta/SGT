# Correção de paridade das três visões — em execução

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
