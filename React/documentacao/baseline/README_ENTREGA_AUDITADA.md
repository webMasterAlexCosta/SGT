# SGT/GTRAT React — integração com o Java congelado

Esta cópia contém a interface React, os ajustes de integração com os contratos Java existentes e os testes de regressão da rodada de 2026-11-01. O backend e o JAR permanecem congelados. As matrizes, os relatórios e as evidências entregues com o projeto distinguem verificação estática, testes React com transporte simulado e E2E real; a presença de um componente ou um build bem-sucedido não homologa os 198 requisitos.

**Comece pelo [GUIA_INSTALACAO_EXECUCAO.md](GUIA_INSTALACAO_EXECUCAO.md).** Ele descreve a restauração offline, a configuração do aplicativo, os testes, o proxy e os pré-requisitos do ambiente real.

## Ambiente e dependências

- Linux x86_64 com glibc, Node **22.16.0** ou superior da série 22, npm **10.9.2** de referência.
- Dependências e transitivas reproduzíveis pelo `package-lock.json` recebido e pelo pacote `SGT_GTRAT_DEPENDENCIAS_COMPLETAS_FEDORA_NODE22.zip`.
- Para restaurar offline, use `restaurar-fedora.sh` do pacote de dependências. O script verifica compatibilidade, instala em uma pasta temporária e preserva um checkpoint dos módulos existentes.
- Não copie para a entrega um symlink `node_modules` que aponte para o workspace de outra máquina. Restaure os módulos no destino a partir do pacote público de dependências.

## Comandos principais

Execute na raiz do frontend, com Node 22 no `PATH` e as dependências restauradas:

```bash
npm run dependencias:verificar
npm run test:preparacao
NODE_OPTIONS=--max-old-space-size=768 flock /tmp/sgt-vitest.lock npm test -- --maxWorkers=1 --fileParallelism=false
```

`npm test` usa somente os valores `VITE_*` sanitizados de `.env.example`. Não requer `.env`, credenciais, Java, banco, Redis ou sessão autenticada. O Vitest ignora `.env` e `.env.local`; os testes que precisam de outro valor devem configurá-lo explicitamente no próprio cenário.

Para executar ou compilar o aplicativo, prepare seu ambiente conforme o guia. `.env.example` **não é carregado automaticamente pelo build nem pelo aplicativo em produção**:

```bash
test -f .env || cp .env.example .env
# Ajuste as configurações públicas e o destino do proxy antes de usar o aplicativo.
NODE_OPTIONS=--max-old-space-size=768 flock /tmp/sgt-vitest.lock npm run build
npm run dev
```

O build gera `dist/`, que deve ser servido por um frontend independente. `build:java` e `sync:java` estão bloqueados de propósito. Não copie o `dist` para o backend e não recompile/substitua o JAR.

## Organização

| Arquivo/diretório | Finalidade |
|---|---|
| `src/` | Interface e serviços consumidores dos contratos existentes. |
| `.env.example` | Exemplo público sanitizado; fonte de ambiente da suíte Vitest. |
| `vite.config.js` | Build e servidor de desenvolvimento; proxy de `/api` e `/config.json`, preservando Origin e validando TLS do destino. |
| `vitest.config.js` | Suíte React com um worker e arquivos sequenciais. |
| `test/` | Regressões unitárias/de componentes; `preparacao-local.test.mjs` roda pelo Node. |
| `test/e2e/` | Cenários que dependem do runtime real preparado, contas sintéticas e browser externo. Não são executados por `npm test`. |
| `GUIA_INSTALACAO_EXECUCAO.md` | Procedimentos atuais de execução e limites da reprodução. |

## Histórico preservado da preparação Fedora

A etapa anterior tratava exclusivamente de completar seis dependências de desenvolvimento ausentes e devolver um pacote offline. Ela não representa o escopo atual desta cópia. Permanecem úteis e preservados:

- `PROMPT_CODEX_FEDORA.md`: instrução histórica de geração do pacote de dependências. Seu conteúdo não foi alterado; um teste legado verifica esse documento.
- `scripts/gerar-pacote-dependencias-fedora.sh`: empacotador das dependências, cache, lock e manifesto.
- `vitest.dependencias.config.js` e `test/dependencias-stack.test.jsx`: smoke restrito à infraestrutura Vitest/RTL/jsdom.
- `documentacao/PROMPT_INTEGRACAO_FUNCIONAL_FUTURA.md`: registro do planejamento anterior de integração.
- `scripts/preparar-fedora.sh`: helper da etapa antiga; para a execução atual, siga o guia acima.

Os avisos históricos de “Vitest ausente”, “lockfile ainda não gerado” ou “não integrar nesta etapa” descrevem a entrega anterior. A restauração offline e a implementação atual têm evidências próprias. Conserve os pins existentes; não faça upgrades oportunistas para reproduzir esta entrega.
