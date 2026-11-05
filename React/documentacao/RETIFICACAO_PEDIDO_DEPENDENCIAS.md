# Retificação da tarefa Codex — somente dependências

O usuário corrigiu a finalidade da entrega: **o Codex Fedora deve preparar a redundância/cópia completa das dependências não disponíveis no ChatGPT e devolver um ZIP para anexar à conversa e armazenar na Biblioteca**. Não é para executar a reconciliação React/Java nesta etapa.

## Alterações efetivas nesta atualização

- `package.json` já havia sido alinhado às versões dos módulos instalados no ambiente ChatGPT (React 19.2.5, Vite 8.2.2 etc.). Essa compatibilidade foi reconferida por leitura dos próprios `node_modules` e preservada, sem atualizar as bibliotecas de runtime.
- `package.json` agora contém os scripts `dependencias:verificar`, `test:vitest:dependencias` e `dependencias:empacotar:fedora`. As seis dependências ausentes continuam declaradas com versões exatas.
- `PROMPT_CODEX_FEDORA.md` agora trata **somente** de instalação, prova offline e retorno do ZIP de dependências. A missão anterior foi preservada em `documentacao/PROMPT_INTEGRACAO_FUNCIONAL_FUTURA.md`.
- Foram adicionados `vitest.dependencias.config.js` e `test/dependencias-stack.test.jsx`, para confirmar a execução real de Vitest + React Testing Library + jest-dom + jsdom no Fedora. Não são testes de negócio.
- Foi incluído `scripts/gerar-pacote-dependencias-fedora.sh`, que verifica pins, lockfile, smoke Vitest, build Vite e reinstalação **offline** isolada antes de produzir `SGT_GTRAT_DEPENDENCIAS_COMPLETAS_FEDORA_NODE22.zip` com `node_modules`, cache npm isolado, lockfile, manifesto de hashes e instruções de restauração.
- Java e JAR estão congelados em ZIP separado, sem alteração.

## O que ainda não existe nesta entrega

- Não foi criado um `package-lock.json` por ausência de acesso ao registro público npm neste ambiente.
- As seis devDependencies ausentes não foram instaladas aqui, portanto o teste Vitest novo **não pode ser declarado aprovado** nesta sessão.
- O ZIP final de dependências **ainda será gerado no Fedora pelo Codex**. Para guardá-lo na Biblioteca, o usuário deverá anexá-lo à conversa no ChatGPT, solicitando salvar em `/Dependencias/Node-e-React/SGT_GTRAT/`.
- Não foram realizados testes de integração React/Java nesta retificação.

Os resultados anteriores de build/preparação continuam registrados em `ESTADO_ENTREGA.md` e não substituem a validação do pacote completo Fedora.
