# Preparação de produção — plano de implementação

**Responsável:** Sol/xhigh, execução inline. Escopo/abordagem aprovados pelo pedido humano e resposta de ensaios no laboratório, banco novo e Windows10; dispensa nova aprovação genérica do mesmo escopo conforme AGENTS. Brief: [BRIEF.md](BRIEF.md).

**Arquitetura:** preservar React e contratos de negócio; validar a configuração produtiva antes dos beans de persistência; preparar banco novo por contexto JPA mínimo; oferecer suporte operacional explícito sem acesso automático a recursos reais. Não há deploy nesta etapa.

**Stack:** Java25/Boot4.1.2/PostgreSQL18.6, frontend integrado existente, Valkey9.0.7 no laboratório. Python padrão para suporte operacional já existente no projeto; execução do aplicativo Windows usa Java/PowerShell. Sem novos downloads ou alterações globais.

## Restrições e foco da revisão

- Checkpoint antes de cada lote; heavy-run e cgroup; teto humano15GB, proteção preventiva12GB RAM+swap, reserva/PSI globais. Agrupar2–4 classes/arquivos relacionados quando útil, sem repetir gates aprovados sem mudança. Maven/forks menores quando suficientes (package atual128/192MiB), pools até8, um worker; suítes e JVMs nativas sequenciais.
- Não modificar dados anteriores, .env operacional, usuários/permissões de outros serviços, schema por SQL ou os bytes preservados de `FrequenciaJanelaConfiguravelHttpIT`.
- Rejeições não podem imprimir chaves/senhas, aceitar origem HTTP, confiar em headers de proxy não configurado ou criar schema antes de validar.
- Backups devem recusar aplicação ativa/destino não vazio, detectar arquivo adulterado, preservar relações/sequências e não afetar a fonte numa falha de restauração.
- Preparação Windows e verificação Linux serão relatadas separadamente; backup no mesmo disco e ausência de aceite não equivalem a produção liberada.

## Lotes

- [ ] 1. Adicionar regressões em `JAVA/src/test/java/com/example/siggtrat/configuracao/VerificadorConfiguracaoProducaoTest.java`; observar falhas pertinentes. Implementar `VerificadorConfiguracaoProducao.java` e ajustar somente `application-prod.properties`. Revalidar regressões de configuração/segurança afetadas em lotes pequenos.
- [ ] 2. Adicionar `PreparadorSchemaNovo.java` e testes de recusa/preparação real. Usar `PropertiesLauncher` do próprio JAR para executar a preparação sem alterar o entrypoint de negócio. Comprovar banco limitado/ownership/schema vazio e geração somente por JPA.
- [ ] 2b. Adicionar `ComissionadorAdministradorInicial.java`, perfil operacional mínimo e regressões. Criar um único Administrador nominal por JPA em banco sem dados, com lock transacional e troca obrigatória de senha. Recusar repetição/contas automáticas. Ensaiar primeiro acesso, sem enviar e-mail externo. Justificativa: o seed existente de três contas de desenvolvimento não atende a instalação nova solicitada.
- [ ] 3. Criar `JAVA/scripts/operacao/sgt_operacao.py` e testes negativos/positivos correspondentes; backup data-only, arquivos e material privado de recuperação, manifestação/hash; restore vazio com constraints ativas. Preparar `Start-Sgt.ps1`, template público sem credenciais e runbook Windows.
- [ ] 4. Retomar somente o cluster isolado existente, provar identidade; gerar candidato preservando JAR08/assets; ensaiar configuração produtiva/TLS e recuperação em bases novas do mesmo laboratório, com os dados sintéticos anteriores apenas como fonte de leitura. Validar HTTP/Chrome e cadeias de integridade após restore.
- [ ] 4b. Corrigir os avisos fixos de desenvolvimento em `Homologacao.tsx`/`HomologacaoModal.tsx`, usando o modo real de build da configuração tipada. O artefato de entrega não pode afirmar que alterações não afetam produção. Preservar avisos em HMR/desenvolvimento, credenciais de teste já ocultas e toda autorização no backend. Regredir componentes/configuração, executar typecheck/build uma vez, sincronizar assets com preservação e conferir no mesmo JAR final.
- [ ] 5. Congelar fonte/JAR/recibos e obter revisão independente no Desktop conforme autorização e fluxo existente. Corrigir achados procedentes e rechecar impacto. Atualizar relatório/estado com aprovações reais e pendências externas. Commit/push somente dentro da autorização humana já aplicável à branch limpa, sem tocar main.

## Registro de execução

Planejamento iniciado após leitura do estado funcional aprovado, da matriz de comandos, do perfilprod e do laboratório. A visão de PIDs na sandbox não incluía os serviços externos; a conferência real fora da sandbox comprovou as duas unidades próprias ativas e todos os PIDs registrados vivos. Reutilizar os processos; não reiniciar ou encerrar por interpretação de um PID invisível. Antes dos testes de dados, conferir novamente versão, data directory, identificador e ownership.

Primeiros lotes: `prod67-config-red01` falhou pela ausência do novo verificador; `prod67-config-green01` passou21 verificações. `prod67-schema-red01` falhou pela ausência do preparador; `prod67-schema-green01` passou16 verificações em três classes, incluindo regressões de schema e bootstrap seguro. Picos agregados8,54GB/9,14GB; semOOM e sem deriva de fonte. Ainda não constituem prova de JPA/TLS/backup reais nem de execução Windows.

## Recuperação e lotes intermediários — 11/11

O aplicativo foi reaberto após falta de memória relatada pelo usuário; fonte/checkpoints e JAR02 preservados. O lote `prod67-jar03` não iniciou e não recebeu aceite. O monitor passou a usar a identidade atual do host, RAM+swap e PSI/reserva global; os recibos de cgroup próprios não atestam ausência de OOM em todo o host. O pedido posterior de meio termo substitui a restrição temporária de uma classe:2–4 relacionadas quando útil, sem paralelizar suítes, teto15GB e proteção12GB.

`prod68-package01`:21testes/1classe, JAR final `6d55cba894b037b6a20cca83a6d49b6f30b0e4a1c62ba95bfb434bdfbadf6944`,595arquivos estáticos;823classes anteriores e116bibliotecas byte a byte preservadas. `prod67-tls-real02`:44verificações TLS reais. `prod69-admin-native01`:schema JPA novo, um Administrador e recusa de lock/repetição sem redefinição. `prod69-first-access02`:troca obrigatória/permissões por TLS e Chrome157 com rotas profundas/recarga/logout por HTTP loopback separado; não certifica ChromeHTTPS ou Windows. Pico recente10,38GB RAM+swap; JVMs encerradas após ensaio. Tentativas de infraestrutura/fixture falhas preservadas e excluídas do aceite. O recibo público detalha o alcance.

Revisão independente atual e condições externas de liberação continuam pendentes.
