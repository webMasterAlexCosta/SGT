# Estado verificável da entrega React preparatória

## Insumos
- Original `sgt_gtrat_front_end-main.zip`, React `sig-gtrat-front@1.4.1D`.
- Dependências `node_modules-linux-node22(SGE).tar.gz` da Biblioteca, Linux x86_64.
- Backend final separado, imutável, com fonte, JAR e documentos históricos.

## Alterações efetivas
1. `package.json`: pins alinhados com o `node_modules` existente, remoção de `react-icons`, bloqueio de `sync:java` e `build:java`, scripts diagnósticos e smoke adicionados.
2. Quatro componentes de relatórios/painel: somente importações e usos de ícones Excel/PDF substituídos por equivalentes de Lucide disponível.
3. `.env` original não distribuído; `.env.example` inteiramente sanitizado com parâmetros locais fictícios, `VITE_PAGINATION_DEFAULT_SIZE=5`, sem senhas de teste.
4. Inclusão de `node_modules`, scripts auxiliares, testes locais, documentação e `dist` de demonstração.

## Verificação nesta sessão
- Node.js 22.16.0, npm 10.9.2.
- Build inicial com React original + dependências locais: **FALHOU**, `react-icons/fa` ausente.
- Build após ajuste dos quatro arquivos: **PASSOU**, `vite build`, aproximadamente 2,82s; aviso de chunk grande de face-api, não bloqueante.
- `node --test test/preparacao-local.test.mjs`: **4 aprovados, 0 falhas**. Não são testes de regra de negócio.
- `npm run diagnostico:dependencias`: indica seis devDependencies faltantes; falha esperada até instalação no Fedora.
- Tests Vitest/RTL/E2E e integração Java/MySQL/Redis: **NÃO EXECUTADOS** nesta preparação.
- Não houve alteração nem execução do Java durante esta etapa.

## Limites
O React **ainda contém divergências funcionais** em relação à API Java. São prioridade para o Codex no Fedora: transferência excepcional (prévia, token, cancelamento), provas contratuais e nome, disponibilidade temporal, fotografias históricas, consulta contratual completa, paginação e testes ausentes. Seis requisitos E seguem documentais enquanto faltarem fontes reais autênticas; o build não é homologação funcional.

## Segurança
O `.env` original foi descartado da distribuição; não utilizar credenciais reais nos exemplos. Todo `VITE_*` integra potencialmente o bundle; o build incluído foi gerado com valores fictícios. O Java congelado será distribuído separadamente sem `.env` privado, preservando o mesmo JAR verificado por SHA-256.

## Retificação do pedido — 31/10/2026

- O React fica preparado para o **Codex gerar apenas o ZIP de dependências Fedora e devolvê-lo ao ChatGPT**, e não para iniciar agora a implementação das regras de negócio.
- Versões já disponíveis reconfirmadas nos módulos instalados; `package.json` ganhou os comandos de validação, smoke Vitest e empacotamento.
- `npm run test:preparacao`: **6 aprovados / 0 falhas** (dois novos testes de pins e objetivo da entrega).
- `npm run build`: **aprovado**, Vite 8.2.2 e React local.
- `npm run dependencias:empacotar:fedora`: **bloqueado como esperado** neste ambiente por falta de `package-lock.json` real.
- Vitest e cinco dependências complementares ainda ausentes: deverão ser baixadas pelo Codex no Fedora, assim como o cache npm offline.
- O backend Java não foi tocado nem executado nesta retificação. O ZIP Java da entrega anterior é preservado byte a byte.

Ver `documentacao/RETIFICACAO_PEDIDO_DEPENDENCIAS.md`.
