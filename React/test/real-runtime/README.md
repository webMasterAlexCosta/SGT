# Histórico retirado do fluxo atual: runtime MySQL 8.0.45

> **Procedimento histórico; não executar como preparação da campanha atual.** O alvo atual é **PostgreSQL 18.6 (`server_version_num=180006`) com pgJDBC 42.7.14**. Os comandos e exemplos abaixo pertencem à campanha MySQL e não validam esse alvo. `manage.py`, `snapshot.py`, `runtime-observado.cjs` e o cenário manual que importa esse gate continuam específicos de MySQL; não foram portados.
>
> O pacote externo `50_BROWSER_JAR_V2_PREPARACAO` prepara outro caminho explícito para JAR integrado/appv2. Ele **não é chamado por estes scripts nem os substitui automaticamente**. Seu gate de recibo, suas pré-condições e a identidade do ambiente precisam de revisão e observação próprias do coordenador. Não usar IDs, recibos ou instruções MySQL para preencher evidência PostgreSQL.
>
> Os sete casos Node de `test/e2e/verificar-runtime-observado.cjs` são regressão histórica do validador MySQL com recibos sintéticos. Não conectam ao banco e não constituem prova PostgreSQL. A inclusão explícita deles em um runner externo não muda esse alcance.

## Conteúdo preservado da campanha MySQL

> Requisito histórico da campanha de 2026-11-03: MySQL exatamente 8.0.45. Os guardas atuais recusam 8.0.46 e versões com sufixos; execução real exige SELECT VERSION(), identidade do servidor e JAR desta rodada. Testes unitários dos guardas usam respostas controladas e não constituem observação de servidor.

Na campanha histórica, o `manage.py` observa MySQL **8.0.45** e supervisiona exclusivamente o JAR candidato **0.0.4-operacional-20261101**, com Java **17**. O banco, o catálogo, a conta de aplicação e o Redis devem estar preparados externamente, com autorização. O helper não inicia MySQL/Redis, não inicializa datadir, não cria contas ou catálogo, não concede privilégios e não usa cliente administrativo. Suas únicas ações de linha de comando são `status`, `serve-app` e `stop-app`.

## Configuração privada explícita

Defina `SGT_RUNTIME_CONFIG` como o caminho absoluto de um JSON preparado externamente. Todos os diretórios e arquivos referenciados devem existir. O exemplo abaixo descreve os campos, sem fornecer credenciais nem defaults administrativos:

```json
{
  "private_dir": "/caminho/runtime/private",
  "evidence_dir": "/caminho/evidence/infra",
  "schema": "catalogo_sintetico_preparado",
  "mysql": {
    "client": "/caminho/mysql-8.0.45/bin/mysql",
    "client_options": "/caminho/runtime/private/mysql-app.cnf",
    "mysqld": "/caminho/mysql-8.0.45/bin/mysqld"
  },
  "java": {
    "executable": "/caminho/jdk-17/bin/java",
    "jar": "/caminho/candidata/backend/bin/candidato.jar",
    "sha256": "SHA256_REAL_DO_JAR_CANDIDATO_REVISADO",
    "version": "0.0.4-operacional-20261101",
    "app_env": "/caminho/runtime/private/app/app.env"
  }
}
```

O campo `mysql.mysqld` é opcional. Quando presente, seu `--version` deve informar 8.0.45. A versão do servidor é sempre consultada por `SELECT VERSION()` com a conta de aplicação e deve ser exatamente `8.0.45`, independentemente da presença do binário local.

O arquivo de opções MySQL deve conter apenas `[client]`, com `user`, `password`, `host` loopback, `port` e protocolo TCP explícitos, além de opções TLS se necessárias. O usuário deve ser a conta de aplicação autorizada; `root` é recusado. Os privilégios são preparados e revisados fora deste helper. Não há fallback para login administrativo ou arquivo de login pessoal.

O `app.env` deve conter a configuração completa do backend candidato, inclusive segredos locais de MySQL/Redis, sem copiar credenciais históricas. `DB_USER` deve coincidir com o cliente MySQL. `DB_URL` deve apontar para o mesmo host, porta e catálogo, sem credenciais no URL e com `createDatabaseIfNotExist` ausente ou `false`. Declare `spring.jpa.hibernate.ddl-auto=update` e `spring.sql.init.mode=never` no arquivo; a JVM também recebe esses valores explicitamente. Os seeds devem permanecer desabilitados no ambiente já preparado, exceto em um procedimento externo separado e autorizado. O supervisor não gera nem ativa seeds.

Overrides diretos de datasource, importação de configuração e opções injetadas da JVM são recusados. O processo Java recebe a configuração fornecida e um conjunto reduzido de variáveis de sistema; não herda configurações `SPRING_*`, `DB_*` ou opções de JVM do shell. O arquivo de configuração e as credenciais devem ter acesso restrito e permanecer fora da entrega.

Para os clientes HTTP legados, use `private_dir` igual a `SGT_TEST_RUNTIME/private`: eles continuam lendo as fixtures e contas sintéticas desse caminho. Prepare também `private/app`, `private/reports` e `evidence_dir`. Os helpers não oferecem defaults de credenciais.

## Origem explícita dos clientes HTTP

Copie os helpers Python atuais, incluindo `configuracao_http.py`, para o runtime privado. `api_client.py` e `http-smoke.py` mantêm `private` relativo ao diretório do próprio script. Antes de executar qualquer consumidor desses clientes, defina `SGT_BACKEND_URL` com a origem do JAR já preparado e observado:

```bash
export SGT_BACKEND_URL=http://127.0.0.1:18080
# Para o cenário de upgrade isolado em outra porta, use em sua sessão própria:
# export SGT_BACKEND_URL=http://127.0.0.1:18081
```

A variável é obrigatória: não há fallback para `18080`, para `SGT_FRONTEND_URL` ou para o supervisor. Aceita somente origem HTTP(S), sem usuário/senha, caminho de API, query ou fragmento. Uma barra final é normalizada. Configuração ausente ou inválida interrompe o helper antes de ler segredos, abrir sessão ou enviar requisições. A versão, a identidade do banco e a prontidão da aplicação continuam precisando dos recibos próprios; uma URL configurada não comprova nenhum desses gates.

O contrato se aplica a `create-fixture.py`, `prepare_sixth_version.py`, `pos-auditoria-*-http.py` e ao preparador de evolução histórica que importam `api_client.py`. Use runtime, contas, cookies e fixtures separados para candidata e upgrade. Não copie IDs ou sessões de um servidor para outro. `SGT_FRONTEND_URL` continua identificando a origem acessada pelo navegador; no cenário de JAR integrado, ela deve coincidir com o backend preparado e registrado no estado da fixture.

As regressões locais de origem usam openers controlados, sem HTTP ou navegador real:

```bash
python3 -m unittest discover -s test/real-runtime -p 'test_http*origin.py' -v
```

Execute esse comando na raiz React das fontes. Os casos conferem origem diferente de `18080`, falha sem configuração, rejeição de URL com credenciais e manutenção de CSRF, confirmação, troca inicial de senha e recusas por status. Não são evidência de integração com o JAR.

## Observação, aplicação e recibo

Depois da preparação externa, use os caminhos absolutos dos helpers:

```bash
export SGT_TEST_RUNTIME="/caminho/runtime"
export SGT_RUNTIME_CONFIG="/caminho/runtime/private/runtime.json"
python3 "$SGT_TEST_RUNTIME/manage.py" status
python3 "$SGT_TEST_RUNTIME/manage.py" serve-app --expected-jar-sha256 "$SGT_JAR_SHA256"
```

O hash passado deve coincidir com o JSON e com o arquivo real. O manifest deve informar `0.0.4-operacional-20261101`. JARs na árvore `baseline` ou `java-frozen` são recusados; renomear o JAR antigo tampouco satisfaz a versão do manifest. `serve-app` verifica configuração, identidade do banco, Java 17 e identidade do JAR antes de criar a JVM. Ele escreve o log e os PIDs privados e mantém o processo supervisionado. `stop-app` encerra somente o supervisor identificado e sua JVM candidata.

Em outra sessão, após observar `ReadinessState changed to ACCEPTING_TRAFFIC` do PID atual, execute novamente `status`. O recibo registra hora e ID de observação, resultado de `SELECT VERSION()`, UUID do servidor, catálogo, porta, usuário efetivo, hash/versão do JAR e presença do processo configurado. São gravados `runtime-status-<id>.json` e `runtime-status.json` no `evidence_dir`. O status de um processo existente não prova prontidão HTTP nem funcionamento do Redis; essas verificações exigem seus próprios recibos.

Antes do cenário `seguranca-concorrencia.cjs`, defina `SGT_RUNTIME_RECEIPT` com o caminho absoluto do recibo identificado por ID dessa execução. O cenário recusa recibo ausente, versão divergente, fonte diferente de `SELECT VERSION()`, identidade incompleta, hash/versão de JAR incompatível ou processo não observado em execução. A checagem ocorre antes de abrir o navegador. O relatório referencia a observação e seu hash; não atribui uma versão ao banco a partir de um rótulo fixo.

## Snapshots, reinício e clientes HTTP

`snapshot.py` conserva snapshots de leitura com metadados e hashes agregados de todas as tabelas visíveis à conta de aplicação. `mysql()` admite as consultas e a transação `READ ONLY` utilizadas por esse consumidor. A conta precisa dos acessos de leitura pertinentes, revisados externamente; o helper não concede privilégios. `snapshot.py compare` compara arquivos já existentes sem conectar ao banco.

`restart-check.py` conserva o hash candidato, encerra a JVM supervisionada, captura o estado, reinicia o mesmo JAR e exige um novo marcador `ACCEPTING_TRAFFIC` após os runners, associado ao novo PID. Só então captura e compara o estado antes de retomar HTTP. Clientes HTTP e cenários de negócio existentes permanecem separados e sequenciais; leia suas pré-condições antes de acioná-los. `http-smoke.py` ainda pressupõe a conta sintética de bootstrap e os arquivos privados do cenário: não é um provisionador de MySQL nem deve ser executado para descobrir credenciais.

## Dependências e limites desta alteração

O cliente MySQL da preparação atual precisou das bibliotecas Ubuntu `libncurses5` e `libtinfo5`, versão `6.3-2ubuntu0.3`, extraídas localmente. O coordenador registrou os hashes e o `mysql --version` em `evidencias/mysql_cliente_dependencias.json`. A resolução foi isolada por `LD_LIBRARY_PATH`, incluindo `runtime/mysql-client-libs/lib/x86_64-linux-gnu` e `runtime/libs`, sem instalação no sistema. Use os caminhos reais da preparação; esse recibo do cliente não substitui a consulta de versão do servidor.

Registro histórico recebido: na alteração anterior do supervisor foram executadas somente verificações locais de sintaxe e de preparação com respostas controladas. Não foi iniciado banco, Redis, JVM ou navegador, nem realizada conexão real por este helper. Os testes de preparação não são prova do runtime MySQL 8.0.46, de prontidão, concorrência ou persistência.

O supervisor e os guias originais estão preservados em `../../documentacao/historico-runtime-8.4.11/`; `manage.py.txt` é somente texto histórico. `MANIFESTO_ORIGEM.json` deste diretório conserva os hashes da origem histórica, não os hashes dos helpers atuais. A integridade da candidata é registrada pelo manifesto final da entrega.
