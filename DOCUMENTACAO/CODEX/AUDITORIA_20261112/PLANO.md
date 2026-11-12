# Auditoria e correções do SGT — 12/11/2026

Pedido humano: auditar o SGT completo e corrigir defeitos à medida que forem demonstrados, respeitando teto agregado de 15 GB e memória disponível. Commit/push prévios concluídos: `678758afe271cf531560e612d13bad292cab7536`, branch `codex/sgt-progresso-seguro-20261111`, árvore inicial limpa. O commit é um checkpoint sem alteração de arquivos.

Execução pelo responsável deste chat, no projeto/host atuais. Não criar tarefas, agentes, branches ou worktrees. Preservar versões, dados, áreas removidas e decisões vigentes; não acessar produção. Requisitos e precedência: [índice documental](../../README.md), [estado atual](../../ESTADO_ATUAL.md), [toolchain](../TOOLCHAIN.md), [matriz de comandos](../../../.codex/validation.toml) e contratos referenciados nesses documentos. Auditoria e correção não equivalem a homologação integral ou aprovação de produção.

## Plano de execução

- [x] Conferir Git, projeto, instruções e runtimes; registrar e publicar checkpoint anterior à auditoria.
- [x] Conferir serviços existentes e limites. Reutilizar o [laboratório sintético](../DASHBOARD_20261111/LABORATORIO.md); confirmar ownership antes de ITs.
- [x] Inventariar módulos, pontos de entrada, testes e cobertura; executar gates existentes para a fonte atual.
- [x] Auditar backend: contratos HTTP, autenticação/autorização/CSRF, temporalidade, transações/concorrência, arquivos/documentos, jobs, relatórios e operação.
- [x] Auditar frontend: transporte compartilhado, cancelamento/sessão, contratos/tipos, formulários, acessibilidade, estado assíncrono, relatórios e interface no navegador.
- [x] Para cada defeito: registrar regra, evidência e impacto; reproduzir antes de corrigir; acrescentar regressão pertinente; executar check focal.
- [x] Executar integrações pertinentes em PostgreSQL/Redis reais isolados, gates finais e integração React → static → JAR se houver alterações de produto.
- [x] Registrar resultados, hashes, achados resolvidos/remanescentes e limites reais; conferir diff e estado final. Ver [relatório](RELATORIO.md) e [evidências](EVIDENCIAS.json).

## Recursos e evidência

Fila global `~/.codex/tools/heavy-run.py`; suítes pesadas sequenciais. Maven 512 MiB; fork Java 768 MiB/metaspace 256 MiB; Node 768 MiB; um worker/fork; pool de testes 8. Cgroup de suíte de 2 GiB, ampliado para 3 GiB no lote TCP que inclui JVMs filhas; sem swap; checar disponibilidade antes de cada suíte, com reserva de entrada de 8 GiB. Serviços existentes têm tetos somados de 4 GiB (PostgreSQL 768 MiB, Redis 256 MiB, API 2 GiB, Vite 1 GiB), abaixo do teto agregado autorizado com a suíte. Reavaliar limites se houver pressão de memória; não parar processos alheios.

Logs brutos e parâmetros sintéticos permanecem em `.homologacao/`, ignorado pelo Git. Relatório versionável contém somente evidências sanitizadas e vinculação a bytes. Gate Sonar existente será consultado somente em leitura e tratado conforme data/escopo da análise. Ausência de lint, checks bloqueados, testes ignorados e lacunas de cobertura serão explícitos.
