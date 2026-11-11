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

- [x] 1. Adicionar regressões em `JAVA/src/test/java/com/example/siggtrat/configuracao/VerificadorConfiguracaoProducaoTest.java`; observar falhas pertinentes. Implementar `VerificadorConfiguracaoProducao.java` e ajustar somente `application-prod.properties`. Revalidar regressões de configuração/segurança afetadas em lotes pequenos.
- [x] 2. Adicionar `PreparadorSchemaNovo.java` e testes de recusa/preparação real. Usar `PropertiesLauncher` do próprio JAR para executar a preparação sem alterar o entrypoint de negócio. Comprovar banco limitado/ownership/schema vazio e geração somente por JPA.
- [x] 2b. Adicionar `ComissionadorAdministradorInicial.java`, perfil operacional mínimo e regressões. Criar um único Administrador nominal por JPA em banco sem dados, com lock transacional e troca obrigatória de senha. Recusar repetição/contas automáticas. Ensaiar primeiro acesso, sem enviar e-mail externo. Justificativa: o seed existente de três contas de desenvolvimento não atende a instalação nova solicitada.
- [x] 3. Criar `JAVA/scripts/operacao/sgt_operacao.py` e testes negativos/positivos correspondentes; backup data-only, arquivos e material privado de recuperação, manifestação/hash; restore vazio com constraints ativas. Preparar `Start-Sgt.ps1`, template público sem credenciais e runbook Windows.
- [x] 4. Retomar somente o cluster isolado existente, provar identidade; gerar candidato preservando JAR08/assets; ensaiar configuração produtiva/TLS e recuperação em bases novas do mesmo laboratório, com os dados sintéticos anteriores apenas como fonte de leitura. Validar HTTP/Chrome e cadeias de integridade após restore.
- [x] 4b. Corrigir os avisos fixos de desenvolvimento em `Homologacao.tsx`/`HomologacaoModal.tsx`, usando o modo real de build da configuração tipada. O artefato de entrega não pode afirmar que alterações não afetam produção. Preservar avisos em HMR/desenvolvimento, credenciais de teste já ocultas e toda autorização no backend. Regredir componentes/configuração, executar typecheck/build uma vez, sincronizar assets com preservação e conferir no mesmo JAR final.
- [x] 5. Congelar fonte/JAR/recibos e obter revisão independente no Desktop conforme autorização e fluxo existente. Corrigir achados procedentes e rechecar impacto. Atualizar relatório/estado com aprovações reais e pendências externas. Commit/push somente dentro da autorização humana já aplicável à branch limpa, sem tocar main.

## Registro de execução

Planejamento iniciado após leitura do estado funcional aprovado, da matriz de comandos, do perfilprod e do laboratório. A visão de PIDs na sandbox não incluía os serviços externos; a conferência real fora da sandbox comprovou as duas unidades próprias ativas e todos os PIDs registrados vivos. Reutilizar os processos; não reiniciar ou encerrar por interpretação de um PID invisível. Antes dos testes de dados, conferir novamente versão, data directory, identificador e ownership.

Primeiros lotes: `prod67-config-red01` falhou pela ausência do novo verificador; `prod67-config-green01` passou21 verificações. `prod67-schema-red01` falhou pela ausência do preparador; `prod67-schema-green01` passou16 verificações em três classes, incluindo regressões de schema e bootstrap seguro. Picos agregados8,54GB/9,14GB; semOOM e sem deriva de fonte. Ainda não constituem prova de JPA/TLS/backup reais nem de execução Windows.

## Recuperação e lotes intermediários — 11/11

O aplicativo foi reaberto após falta de memória relatada pelo usuário; fonte/checkpoints e JAR02 preservados. O lote `prod67-jar03` não iniciou e não recebeu aceite. O monitor passou a usar a identidade atual do host, RAM+swap e PSI/reserva global; os recibos de cgroup próprios não atestam ausência de OOM em todo o host. O pedido posterior de meio termo substitui a restrição temporária de uma classe:2–4 relacionadas quando útil, sem paralelizar suítes, teto15GB e proteção12GB.

`prod68-package01`:21testes/1classe, JAR final `6d55cba894b037b6a20cca83a6d49b6f30b0e4a1c62ba95bfb434bdfbadf6944`,595arquivos estáticos;823classes anteriores e116bibliotecas byte a byte preservadas. `prod67-tls-real02`:44verificações TLS reais. `prod69-admin-native01`:schema JPA novo, um Administrador e recusa de lock/repetição sem redefinição. `prod69-first-access02`:troca obrigatória/permissões por TLS e Chrome157 com rotas profundas/recarga/logout por HTTP loopback separado; não certifica ChromeHTTPS ou Windows. Pico recente10,38GB RAM+swap; JVMs encerradas após ensaio. Tentativas de infraestrutura/fixture falhas preservadas e excluídas do aceite. O recibo público detalha o alcance.

Revisão independente atual e condições externas de liberação continuam pendentes.

## Correções após revisão operacional

A revisão independente Java, turno `01a1cb57-30a1-7df3-8241-7d5b04bb8694`, examinou afd41c69…76baa29d e apontou doisP2 no suporte Python: sobreposição com a árvore do backup e identidade do banco por endereço/porta/OID não global. Foram confirmados por contraprovas antes da correção. A fonte Java/JAR não mudou.

O suporte agora rejeita sobreposição antes de conexão/gravação/recibos, também na retomada, e lê o identificador real do cluster, comparando-o com OID normalizado. Sem identidade comprovada, recusa sem fallback/privilégio novo. O PostgreSQL real serializou OID em texto; a primeira reexecução recusou antes do backup/restore e foi preservada como falha, originando regressão própria. `prod70-review-green03`:31testes aprovados. `prod70-backup-real04`:backup+restore reais de72tabelas/7383linhas/68sequências,6uploads/5materiais,8contraprovas de sobreposição, fonte e arquivo de backup preservados; sem SQLDDL/purga/disable-triggers. Identidade lida pela role limitada sem alteração de privilégio. Colisão entre clusters distintos tem prova unitária, não se afirma ensaio em um segundo cluster físico. Pico10,47GB RAM+swap. Pacote69 foi marcado superado; falta recheque independente e pacote atualizado.

## Decisão independente final

Java/operacional aprovados no recheque do turno `01a1cb66-fb30-72c0-be4a-2997587b6ecb`, doisP2 fechados e nenhumP0–P3 restante no delta. Frontend aprovado no turno `01a1cb69-e850-7fc0-abbd-579660fd4843`, semP0–P3 no delta. Ambos examinaram o commit `9c8bf1864cb2d3e9a2dc7eeedb0be8ae0c755f05`, somente leitura, Sol/xhigh, sem executar novas suítes; fontes e recibos atuais conferidos por hashes. Ver [pareceres](PARECERES.json). As observações de pendência acima registram as fases anteriores; o pacote69 permanece superado e a entrega atual incorpora a ferramenta `c1af414d9389faff1d001836a8b6b8e97755e480f35db33d590bc33e659b336a`.

Aprovada a preparação operacional e os ensaios no laboratório, dentro do alcance descrito. Não aprovado deploy/produção/Windows nativo; completar condições externas antes da liberação. Testes de camadas diferentes não foram convertidos em uma jornada ChromeHTTPS nem em certificação Windows.
