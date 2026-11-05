# SGT/GTRAT React — gestão operacional 1.4.4-operacional.20261101

> Execução corretiva de 2026-11-03: exigir MySQL exatamente 8.0.45 e recibos novos da candidata. Os resultados históricos abaixo mantêm a proveniência da campanha recebida; não certificam esta correção.

Esta versão deriva da baseline operacional React `1.4.3-operacional.20261101` / Java `0.0.3-operacional-20261101`. Os originais permanecem preservados. O par candidato é Java `0.0.4-operacional-20261101` e React `1.4.4-operacional.20261101`, com runtime Java/MySQL **8.0.45** e Redis a verificar conforme os recibos desta execução.

Foi retirada integralmente a Governança de Terceiros: página, abas, processos exclusivos de admissão/desligamento, avaliação manual/contínua de risco, resumo, serviço HTTP, rota e item de menu. A remoção financeira anterior permanece. URLs antigas seguem o fallback da aplicação e não montam a funcionalidade removida.

O cadastro compartilhado permanece como **Empresas contratadas**, em **Cadastros e configurações**, rota `/empresas-contratadas`. Ele conserva razão social, CNPJ, e-mail, ativação/inativação e associação a contrato em rascunho. Usa `GET/POST /api/fornecedores`, `PUT /api/fornecedores/{id}` e `PUT /api/contratos/{contratoId}/fornecedor/{fornecedorId}`, com o mesmo perfil de gestor local, confirmação de senha, CSRF e tratamento de sessão. Não contém campos, colunas nem ações de risco.

Os controles de integridade operacional do painel gerencial, os campos de empresa e os IDs históricos de fornecedor são compartilhados e permanecem. Os relatórios de execução identificam resultados e hashes do candidato. Contagens históricas não comprovam os 198 requisitos nem os 635 critérios.

## Contratos e operação

`documentacao/evolucao/CONTRATO_PROVA_CAMPO.md` descreve os DTOs históricos preservados e seus estados de prova. A disponibilidade por permissão é independente do estado factual. Zero e falso são valores; um campo desconhecido não se torna ausência comprovada por estar nulo, vazio ou omitido.

Contratos, pessoas, vínculos, cargos, alocações, frequência, ocorrências, atestados, históricos, documentos e relatórios operacionais mantêm suas implementações. A frequência futura continua recusada pelo Java. A janela operacional mantém D como dia 1 e fecha no início de D+7 em `America/Sao_Paulo`, com a correção gerencial já prevista na baseline. Não foram alteradas regras de contagem, permissões ou associações históricas.

## Instalação e build

Use Node 22.16.0/npm 10.9.2 e o `package-lock.json` desta cópia. As dependências permanecem fixadas, incluindo Vite 8.2.4 e Vitest 4.1.12; a versão da aplicação foi atualizada somente nas entradas da raiz. O procedimento vigente está em `GUIA_INSTALACAO_EXECUCAO.md`. `GUIA_REPRODUCAO.md` e `DEPENDENCIAS_ATUALIZADAS.md` preservam o acervo de preparação anterior e seus recibos históricos.

Após restaurar o cache-base e acrescentar os tarballs de `SGT_GTRAT_DEPENDENCIAS_PATCH_20261101.zip`, defina `SGT_CACHE` para esse cache. Em uma cópia nova, prepare `.env.local` a partir de `.env.example`, sem substituir configuração local existente. Todos os valores `VITE_*` são públicos. Execute sequencialmente:

```bash
npm ci --offline --ignore-scripts --no-audit --no-fund --cache "$SGT_CACHE" --userconfig=/dev/null
flock /tmp/sgt-heavy.lock env NODE_OPTIONS=--max-old-space-size=768 npm run test:preparacao
flock /tmp/sgt-heavy.lock env NODE_OPTIONS=--max-old-space-size=768 npm test -- --maxWorkers=1 --no-file-parallelism
flock /tmp/sgt-heavy.lock env NODE_OPTIONS=--max-old-space-size=768 npm run build
```

O projeto usa JavaScript/JSX com contratos documentados em JSDoc. Não há configuração de TypeScript, ESLint ou script `typecheck` no projeto recebido; o relatório não apresenta esses gates como executados. Os testes de contrato, a compilação Vite e os testes de integração verificam as fronteiras afetadas.

O `dist/` deve ser regenerado após os testes e integrado exclusivamente em `src/main/resources/static` do novo backend candidato. Até existir recibo de build desta versão, o conteúdo recebido de `dist/` é histórico. Os comandos antigos `build:java` e `sync:java` continuam bloqueados; a integração do candidato é coordenada separadamente e não substitui arquivos da baseline Java.

Para desenvolvimento, configure o proxy conforme `.env.example` e `vite.config.js`, então execute `npm run dev`. Para a aplicação integrada, use o novo JAR e o guia que acompanha a entrega. Valores `VITE_*` são públicos e não devem conter credenciais.

O [supervisor do runtime](test/real-runtime/README.md) agora usa configuração privada explícita, MySQL **8.0.45** observado por consulta, Java **17** e hash/manifest do novo JAR. Ele oferece somente `status`, `serve-app` e `stop-app`, sobre banco/Redis já preparados externamente. O supervisor original e seus guias foram preservados em `documentacao/historico-runtime-8.4.11/`, com o código antigo como texto `.py.txt`. O cenário de segurança/concorrência exige o recibo observado dessa execução antes de abrir o navegador. A validação local desses helpers não representa execução real de banco, aplicação ou navegador.

## Verificação e evidências

- `test/`: suíte permanente, incluindo as regressões anteriores e os cenários desta evolução.
- `test/remocao-governanca.test.jsx`: remoção de rotas/menus/consultas antigas e preservação do cadastro de empresa, perfil, senha, CSRF, paginação, erros e invalidação de sessão.
- `test/empregados-auxiliares-http.test.js`: conserva os três oráculos compartilhados de catálogos antes nomeados como auxiliares da governança.
- `test/e2e/empresas-contratadas.cjs`: consulta do cadastro no navegador real e verificação das URLs/controles removidos.
- `test/e2e/evolucao-governanca-real.cjs`: zero/falso comprovados e leitura histórica D/K contra o JAR real.
- `test/e2e/EVOLUCAO_GOVERNANCA_REAL.md`: pré-requisitos dos cenários sintéticos e distinção entre UI, sondagem HTTP e persistência.
- `auditoria-recebida/` e `documentacao/baseline/`: fontes e oráculos anteriores preservados, com as classificações históricas originais.
- Relatórios e manifestos externos: RED/GREEN, regressão, navegador, integração, concorrência, reinício e revisão independente do candidato efetivamente entregue.

Registro histórico recebido da campanha anterior (sem reaproveitamento como resultado atual): baseline React com **457 testes aprovados em 68 arquivos**; RED da remoção com **18 falhas esperadas e 1 caso já aprovado, em 19 testes**. A candidata passou **469/469 testes em 67 arquivos**, preparação separada **1/1** e build de produção. Java passou **500/500 unitários e clean package**. Os **98 arquivos** do dist conferem com o JAR. Integração Java/MySQL 8.0.46/Redis, causa e correção do 409, navegador e reinício permanecem pendentes. Consultar os recibos próprios; os resultados unitários não constituem homologação integral.

Os testes de frontend com adapter controlado verificam ordenação, logout, troca de identidade, validação e reenvio. Eles não substituem os testes Java/MySQL/Redis, o navegador contra o JAR nem o reinício com comparação do banco.

As seis dependências de fontes autênticas continuam separadas: CON-023, CAT-004, CAT-007, COL-010, HIS-001 e HIS-006. Dados sintéticos de teste não suprem essas fontes.
