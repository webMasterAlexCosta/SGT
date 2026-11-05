# Runtime real isolado do SGT/GTRAT

Este diretório contém somente infraestrutura externa e clientes HTTP de validação. O JAR e toda a árvore `work/java-frozen` são lidos sem alteração. Os dados nativos e segredos ficam no destino local do link ignorado `private`, fora da sincronização do workspace.

## Componentes e limites efetivos

| Componente | Configuração |
|---|---|
| MySQL Community | 8.4.11, `127.0.0.1:13317`, buffer pool 128 MiB, até 20 conexões |
| Redis real | 8.2.11, `127.0.0.1:26389`, database 7, `maxmemory 64mb`, sem persistência de cache |
| JDK fornecido | Temurin 21.0.12.1+1 |
| JAR congelado | `work/java-frozen/bin/sig-gtrat-backend-0.0.1-SNAPSHOT.jar` |
| SHA-256 do JAR | `08d483d6e25105c62372efd4e0fd5f61a62ed9dee029745e0e35bd9d8f0ba63d` |
| HTTP Java | `http://127.0.0.1:18080` |
| JVM | heap máximo 768 MiB, metaspace 256 MiB, 2 processadores ativos |
| Hikari | 8 conexões máximas, 1 ociosa mínima |
| Schema descartável | `sgt_integration_20261101` |

O serviço Redis foi compilado da distribuição oficial, cuja soma SHA-256 foi confrontada com `redis/redis-hashes`. A compilação usou `make -j2 MALLOC=libc BUILD_TLS=no OPTIMIZATION=-O2`. O MySQL, o JDK e `libaio.so.1` vieram exclusivamente das dependências fornecidas em `sources/Projetos/SGT/dependencia`. As bibliotecas adicionais do cliente MySQL foram extraídas de pacotes oficiais Ubuntu dentro deste runtime, sem instalação global. URLs e hashes ficam nos recibos `evidence/infra/`.

## Schema e dados

`manage.py init` executa a inicialização nativa do MySQL e configura uma conta sintética com privilégios limitados ao catálogo de teste, além da observabilidade necessária. Não executa DDL de tabelas da aplicação. O catálogo lógico é criado pela opção JDBC `createDatabaseIfNotExist=true` no primeiro acesso do JAR. Hibernate/JPA cria e atualiza as tabelas com as únicas opções permitidas:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.sql.init.mode=never
```

O recibo `mysql-before-application.json` comprova catálogo ausente e zero tabelas antes do JAR. Após a primeira inicialização havia 77 tabelas criadas pelo Hibernate. Nenhum H2, mock de persistência, Flyway, Liquibase ou script SQL de schema foi empregado.

As três contas globais iniciais foram criadas pelo mecanismo nativo e explícito `APP_SEED_USUARIOS_GLOBAIS_ENABLED=true`, com senha inicial aleatória. As demais contas, gestões, catálogos, contratos em rascunho, composições e concessão excepcional foram criados exclusivamente por requisições HTTP reais. Senhas, cookies e tokens não integram os recibos públicos.

## Operação no workspace preparado

Execute os comandos a partir de `sgt/`. Neste executor, comandos que abrem conexões ou sockets locais precisam da aprovação normal de execução fora do sandbox.

```bash
# Uma vez, somente para um datadir vazio:
python runtime/manage.py init

# Sessão persistente 1: serviços reais.
python runtime/manage.py serve

# Sessão persistente 2: exato JAR congelado, após baseline persistida.
python runtime/manage.py serve-app \
  --expected-jar-sha256 08d483d6e25105c62372efd4e0fd5f61a62ed9dee029745e0e35bd9d8f0ba63d

# Consulta sanitizada; nenhuma credencial é exibida.
python runtime/manage.py status
```

`init` recusa datadir não vazio. `serve-app` recusa SHA-256 divergente e utiliza trava exclusiva `heavy-java.lock`. Uma suíte pesada deve ser executada por vez, coordenando a mesma trava ou parando o JAR previamente. As portas ficam restritas a loopback. Os supervisores devem permanecer ativos durante os testes.

## HTTP e fixture de negócio

```bash
# CSRF, login inicial, troca obrigatória da senha e sessão autenticada.
python runtime/http-smoke.py

# Criação idempotente por IDs preservados, somente via API real.
python runtime/create-fixture.py
```

Não executar os comandos de login enquanto o navegador estiver usando a mesma conta: o backend substitui a sessão anterior. A fixture usa um administrador próprio para não disputar a sessão do administrador principal. Depois de entregue a fixture, os scripts não são executados novamente sem coordenação.

O arquivo privado `private/http-credentials.json` contém o administrador principal. `private/e2e-fixture.json` contém os campos `accounts.fixture_admin`, `accounts.gestor_local` e `accounts.gestor_destino`, os IDs e as datas. O Gestor Local de origem é o usuário apropriado para escrita negocial; o Administrador concedeu a ele `INCORPORACAO_PROVA_HISTORICA` no contrato 1 pela API legítima.

Ao concluir a preparação inicial, os contratos 1 e 2 estão em `RASCUNHO`, com composição de efetivo 2 cada. Os cenários de navegador publicam o contrato 1, registram pessoa/vínculo, documentos, provas, transferência futura, retificação, cancelamento e correção material. A base depois da homologação já contém esses efeitos; não execute novamente cenários de criação presumindo o estado inicial. A publicação e a confirmação das provas são feitas pela interface nos testes correspondentes. A API não cria pessoa/vínculo em contrato não publicado; por isso `pending_vinculo_request` contém o payload pronto para o próximo passo após a publicação. Todas as descrições e referências da fixture estão explicitamente rotuladas como sintéticas.

## Snapshot somente leitura

```bash
python runtime/snapshot.py capture antes-validacao
python runtime/snapshot.py capture depois-validacao
python runtime/snapshot.py compare \
  evidence/infra/snapshots/antes-validacao.json \
  evidence/infra/snapshots/depois-validacao.json
```

Cada captura usa `REPEATABLE READ` e `START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY`. As linhas são convertidas em SHA-256 no próprio MySQL. O arquivo resultante contém somente contagens, hashes agregados de cada tabela e fingerprints dos metadados. Não exporta dados brutos nem SQL de criação de tabelas. Nenhuma tabela é excluída da comparação. Alterações legítimas de sessões, auditorias ou tarefas de fundo aparecem explicitamente como diferenças, sem serem ocultadas.

## Reinício controlado para prova de persistência

Somente quando o navegador não estiver no meio de um fluxo:

```bash
python runtime/restart-check.py \
  --label e2e-persistencia \
  --expected-jar-sha256 08d483d6e25105c62372efd4e0fd5f61a62ed9dee029745e0e35bd9d8f0ba63d
```

O helper encerra graciosamente apenas o supervisor Java identificado, aguarda a saída da JVM, captura a fotografia anterior e inicia o mesmo JAR. A prontidão exige o marcador `ReadinessState changed to ACCEPTING_TRAFFIC` no trecho recém-adicionado do log, associado ao novo PID Java ainda ativo. A opção externa de logging `--logging.level.org.springframework.boot.availability=DEBUG` torna esse evento observável. O log `Started` isoladamente não basta: no Spring Boot empacotado ele precede os runners. A ordem foi conferida no bytecode do JAR; o marcador de readiness vem depois dos runners e dos listeners síncronos de `ApplicationReadyEvent`.

Em seguida o helper captura e compara a fotografia posterior, **sem realizar nenhuma requisição HTTP**, e grava `evidence/infra/<label>-restart-comparison.json`. A coordenação deve manter o navegador e outros clientes HTTP pausados até esse recibo estar concluído. MySQL e Redis continuam ativos. O helper permanece em execução para manter o JAR disponível. Ele recusa sobrescrever fotografias existentes: escolha um novo rótulo por rodada.

A rodada estrita `e2e-persistencia-ready` foi efetivamente executada em 2026-11-01: mesmo SHA do JAR, mesmas 77 tabelas, metadados e dados, zero exclusões e zero HTTP antes da comparação. Depois foram aprovadas 28 asserções de consultas históricas e reenvios idempotentes. A primeira rodada `e2e-persistencia` é preservada como comparação anterior ao readiness completo; a conclusão final usa a rodada estrita. A regressão do marcador está em `test_restart_readiness.py` (2 RED funcionais e 2 controles, depois 4 GREEN).

```bash
python runtime/manage.py stop-app
python runtime/manage.py stop-services
```

Os comandos de parada verificam a identidade do supervisor antes de enviar sinal. Os dados MySQL são preservados. Não copiar datadir ativo, conteúdo de `private`, arquivos `.env` gerados, tokens ou cookies para entregas públicas.
