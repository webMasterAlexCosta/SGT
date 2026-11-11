# SGT/GTRAT React — gestão operacional 1.4.4-operacional.20261101

> **Entrada de módulo. Projeto em desenvolvimento, sem homologação integral de produção.** Consulte o [estado atual](../DOCUMENTACAO/ESTADO_ATUAL.md) e o README da raiz. Trechos de preparação/entrega abaixo identificam suas campanhas e não concedem conclusão ou produção.

> Toolchain atualizada por pedido humano para Node26.13.0/npm12.2.2, Java/release25 e Boot4.1.2. Consulte `../DOCUMENTACAO/CODEX/TOOLCHAIN.md` e o recibo de migração correspondente. Os números, hashes e aprovações de campanhas anteriores abaixo permanecem históricos e não aprovam estes novos bytes.

A etapa atual migra os componentes para React TypeScript e os utilitários para `.ts`, com contratos Java, estados/props explícitos e `strict=true`. A autenticação permanece por cookies e CSRF. O procedimento vigente está em [FRONTEND_TYPESCRIPT.md](documentacao/FRONTEND_TYPESCRIPT.md). Os relatórios históricos preservados neste repositório não certificam a execução atual.

A integração PostgreSQL/Redis, o JAR e as jornadas no navegador pertencem à frente de integração. Esta frente compila somente `React/dist` e confere a sincronização em modo de leitura.

Foi retirada integralmente a Governança de Terceiros: página, abas, processos exclusivos de admissão/desligamento, avaliação manual/contínua de risco, resumo, serviço HTTP, rota e item de menu. A remoção financeira anterior permanece. URLs antigas seguem o fallback da aplicação e não montam a funcionalidade removida.

O cadastro compartilhado permanece como **Empresas contratadas**, em **Cadastros e configurações**, rota `/empresas-contratadas`. Ele conserva razão social, CNPJ, e-mail, ativação/inativação e associação a contrato em rascunho. Usa `GET/POST /api/fornecedores`, `PUT /api/fornecedores/{id}` e `PUT /api/contratos/{contratoId}/fornecedor/{fornecedorId}`, com o mesmo perfil de gestor local, confirmação de senha, CSRF e tratamento de sessão. Não contém campos, colunas nem ações de risco.

Os controles de integridade operacional do painel gerencial, os campos de empresa e os IDs históricos de fornecedor são compartilhados e permanecem. Os relatórios de execução identificam resultados e hashes do candidato. Contagens históricas não comprovam os 198 requisitos nem os 635 critérios.

## Contratos e operação

`documentacao/evolucao/CONTRATO_PROVA_CAMPO.md` descreve os DTOs históricos preservados e seus estados de prova. A disponibilidade por permissão é independente do estado factual. Zero e falso são valores; um campo desconhecido não se torna ausência comprovada por estar nulo, vazio ou omitido.

Contratos, pessoas, vínculos, cargos, alocações, frequência, ocorrências, atestados, históricos, documentos e relatórios operacionais mantêm suas implementações. A frequência futura continua recusada pelo Java. A janela operacional mantém D como dia 1 e fecha no início de D+7 em `America/Sao_Paulo`, com a correção gerencial já prevista na baseline. Não foram alteradas regras de contagem, permissões ou associações históricas.

## Instalação e build

Use Node 26.13.0 e npm 12.2.2 com o lockfile. A toolchain escolhida usa Node 26.13.0; os pins existentes, incluindo React 19.2.5 e Vite 8.2.4, foram preservados. TypeScript 5.9.3 e os tipos de React/React DOM são as dependências adicionadas.

`.env.example` contém valores públicos sanitizados; preserve sua configuração privada existente. Valores `VITE_*` integram o bundle e não devem conter credenciais. O build de laboratório usa essas configurações públicas e `VITE_API_URL=/api`, aplicadas somente ao processo.

```bash
npm ci --ignore-scripts --no-audit --no-fund
npm run test:preparacao
npm run typecheck
npm run build
npm run sync:java:check
```

A regressão Vitest é executada em lotes sequenciais de cinco ou dez arquivos no ambiente compartilhado. Os recibos registram cada comando, horário, saída, testes e XML; um lote não iniciado por exclusão mútua não conta como teste ou skip. A compilação verifica `strict`, `noImplicitAny` e `strictNullChecks`, sem supressões globais.

`npm run dev` preserva HMR em `127.0.0.1:5173`, com proxy `/api` e `/config.json` para `127.0.0.1:5000`. `npm run sync:java:check` apenas confere o dist e o destino. `sync:java` e `build:java` executam a cópia explicitamente; a frente frontend não os executa. A integração final no static/JAR cabe ao orquestrador.

Os guias, supervisores e recibos antigos que exigem MySQL pertencem ao acervo histórico. Eles não são os comandos de execução desta etapa PostgreSQL e não devem ser usados para interpretar a validação atual.

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

Os testes de frontend com adapter controlado verificam ordenação, logout, troca de identidade, validação e reenvio. Eles não substituem os testes Java/PostgreSQL/Redis, o navegador contra o JAR nem o reinício com comparação do banco.

As seis dependências de fontes autênticas continuam separadas: CON-023, CAT-004, CAT-007, COL-010, HIS-001 e HIS-006. Dados sintéticos de teste não suprem essas fontes.
