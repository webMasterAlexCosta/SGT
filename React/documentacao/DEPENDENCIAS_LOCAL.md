# Dependências React — estado conferido e tarefa Fedora

As versões de execução já foram ajustadas **no `package.json` distribuído** para corresponder aos módulos instalados no ambiente ChatGPT. A compilação Vite foi aprovada antes da atualização das instruções.

| Pacote | Versão local mantida |
|---|---|
| `react` e `react-dom` | 19.2.5 |
| `vite` | 8.2.2 |
| `@vitejs/plugin-react` | 6.1.1 |
| `axios` | 1.20.0 |
| `react-router-dom` | 7.18.3 |
| `lucide-react` | 1.11.0 |
| `recharts` | 3.8.1 |
| `@vladmandic/face-api` | 1.7.15 |
| `jspdf` | 4.2.1 |
| `jspdf-autotable` | 5.0.7 |
| `react-toastify` | 11.0.5 |

O pacote original de `node_modules` veio da Biblioteca (`node_modules-linux-node22(SGE).tar.gz`). Há seis devDependencies declaradas e ainda não instaladas nesta entrega:

| Falta instalar no Fedora | Versão solicitada |
|---|---|
| `@vitejs/plugin-basic-ssl` | 2.3.0 |
| `@testing-library/jest-dom` | 6.9.1 |
| `@testing-library/react` | 16.3.3 |
| `@vitest/coverage-v8` | 4.1.11 |
| `jsdom` | 27.4.0 |
| `vitest` | 4.1.11 |

O `react-icons` ausente foi substituído por ícones equivalentes `lucide-react` nos quatro componentes de Excel/PDF do pacote anterior.

**Ação exigida do Codex:** instalar essas seis dependências e transitivas no Fedora, gerar lockfile genuíno, comprovar o Vitest/RTL/jsdom com `npm run test:vitest:dependencias`, verificar `npm ci --offline` com cache dedicado e devolver **um ZIP somente de dependências** usando `npm run dependencias:empacotar:fedora`. O usuário enviará esse ZIP ao ChatGPT para registro na pasta `/Dependencias/Node-e-React/SGT_GTRAT/`.

O ChatGPT **não instalou** essas seis dependências nesta sessão, nem afirma que os testes Vitest foram executados. Esse é o trabalho do Codex. Para a implementação React/Java posterior, consultar `documentacao/PROMPT_INTEGRACAO_FUNCIONAL_FUTURA.md` sem executá-la agora.
