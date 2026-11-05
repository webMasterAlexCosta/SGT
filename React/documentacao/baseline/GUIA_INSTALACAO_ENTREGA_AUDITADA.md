# Instalação e execução — SGT/GTRAT React

Este guia acompanha a cópia React integrada em 2026-11-01. O frontend é executado e publicado separadamente do Java congelado. Há três verificações distintas: testes React sem serviços reais, build do frontend e E2E com Java/MySQL/Redis/browser. Os comandos de uma não substituem a evidência das demais.

## 1. Pacotes e caminhos de referência

Na Biblioteca, as fontes recebidas estão sob `/Projetos/SGT/`. Os nomes abaixo foram conferidos no inventário da rodada:

| Caminho relativo a `/Projetos/SGT/` | Uso |
|---|---|
| `SGT_GTRAT_REACT_CONGELADO.zip` | Baseline recebido, preservado para comparação. Para executar as correções, use a cópia React final entregue com este guia. |
| `SGT_GTRAT_JAVA_CONGELADO_DOCUMENTACAO.zip` | JAR, backend autoritativo em `backend/` e documentação. `entrada/backend_recebido` é acervo anterior, não a fonte atual dos contratos. |
| `dependencia/Proprias/SGT_GTRAT_DEPENDENCIAS_COMPLETAS_FEDORA_NODE22.zip` | Lockfile autêntico, módulos públicos, cache npm e scripts de restauração offline. |
| `dependencia/Proprias/SGT_REACT_COMPLETO_COM_CHROME_20261101T024306Z.zip` | Pacote companheiro recebido, que inclui o Chrome for Testing e documentação própria. Preserve os manifests e confira seu layout ao extrair. |
| `dependencia/Proprias/SGT_COMPLEMENTO_RUNTIME_REDIS_PLAYWRIGHT_20261101.zip` | Somente as dependências adicionais desta rodada: Redis com fonte, Playwright/Core e bibliotecas Ubuntu do cliente MySQL; sem duplicar os pacotes recebidos. |
| `dependencia/Copiadas_SGE/Node-e-React/node-v22.16.0-linux-x64.tar.xz` | Node 22.16.0 Linux x86_64. |
| `dependencia/Copiadas_SGE/Java-e-Spring/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz` | JDK 21 fornecido para o JAR. |
| `dependencia/Copiadas_SGE/MySQL/mysql-8.4.11-linux-glibc2.28-x86_64-minimal.tar.xz` | MySQL 8.4.11 real. |
| `dependencia/Copiadas_SGE/Outras/libaio.so.1` | Biblioteca nativa fornecida para o MySQL. |

Esses caminhos identificam os arquivos recebidos; não são URLs de download nem comandos para a Biblioteca. Baixe os pacotes para a máquina de execução e confira os hashes/manifests entregues. Não substitua a cópia final pelo ZIP de baseline ao seguir este guia.

O conjunto offline foi preparado para Linux x86_64 com glibc e Node 22. O README do pacote de dependências documenta a prova no Fedora 44. Outras arquiteturas e Windows/macOS não são cobertas por essa prova; exigem dependências nativas próprias.

## 2. Preparar Node e restaurar as dependências offline

Defina os caminhos conforme o diretório em que extraiu os pacotes. Os exemplos seguintes usam variáveis específicas do projeto e não sobrescrevem variáveis de sistema:

```bash
export SGT_BASE="/caminho/SGT"
export SGT_FRONTEND="$SGT_BASE/frontend"
export SGT_ARQUIVOS="$SGT_BASE/pacotes"
export SGT_DEPS="$SGT_BASE/dependencias/SGT_GTRAT_DEPENDENCIAS_COMPLETAS_FEDORA_NODE22"
export SGT_NODE="$SGT_BASE/runtime/node-v22.16.0-linux-x64"
mkdir -p "$SGT_BASE/runtime" "$SGT_BASE/dependencias"

tar -xJf "$SGT_ARQUIVOS/node-v22.16.0-linux-x64.tar.xz" -C "$SGT_BASE/runtime"
export PATH="$SGT_NODE/bin:$PATH"
node --version
npm --version

unzip "$SGT_ARQUIVOS/SGT_GTRAT_DEPENDENCIAS_COMPLETAS_FEDORA_NODE22.zip" -d "$SGT_BASE/dependencias"
```

Referência da entrega: Node **22.16.0**, npm **10.9.2**. `package.json` exige Node `>=22.16.0 <23`. Use extração que preserve symlinks e permissões Unix. Caso o ZIP esteja dentro de uma subpasta adicional, ajuste `SGT_DEPS` para a pasta que efetivamente contém `restaurar-fedora.sh`.

Após extrair a cópia React final em `SGT_FRONTEND`:

```bash
cd "$SGT_DEPS"
node validar-redundancia.mjs integridade .
bash restaurar-fedora.sh "$SGT_FRONTEND"

cd "$SGT_FRONTEND"
npm run dependencias:verificar
```

O restaurador confere compatibilidade dos manifests, usa `npm ci --offline` em uma pasta vazia e move os módulos depois da instalação bem-sucedida. Ele preserva um checkpoint se já existem módulos no destino e recusa lockfile divergente. Não regenere o lockfile para contornar a verificação.

Para reproduzir somente uma instalação npm isolada, use uma pasta nova com os `package.json` e `package-lock.json` correspondentes e o cache do pacote:

```bash
npm ci --offline --include=dev --include=optional --no-audit --no-fund \
  --cache "$SGT_DEPS/npm-cache" --userconfig=/dev/null
```

Não execute esse comando sobre módulos em uso sem checkpoint. Os módulos existentes no workspace da rodada podem ser um symlink de restauração local; esse caminho absoluto não deve ser transportado para outra máquina. O artefato reproduzível é o código com seu lockfile mais o pacote offline verificado.

## 3. Executar testes sem `.env` privado

Os testes React precisam das dependências, mas não precisam do Java, banco, Redis, browser externo nem credenciais. `vitest.config.js` lê as chaves `VITE_*` de `.env.example`, usando o parser nativo do Node 22. `envDir:false` impede a descoberta implícita de `.env`, `.env.local` e arquivos de modo pelo runner.

```bash
cd "$SGT_FRONTEND"
npm run test:preparacao

NODE_OPTIONS=--max-old-space-size=768 flock /tmp/sgt-vitest.lock \
  npm test -- --maxWorkers=1 --fileParallelism=false
```

`npm run test:preparacao` executa o arquivo legado que usa `node:test`. Ele continua fora da descoberta do Vitest. A configuração Vitest inclui um worker e arquivos sequenciais por padrão; as opções explícitas acima deixam o limite visível na execução.

Para a regressão específica de configuração, inclusive numa extração sem `.env`:

```bash
NODE_OPTIONS=--max-old-space-size=768 flock /tmp/sgt-vitest.lock \
  npm test -- test/configuracao-sem-env.test.js --maxWorkers=1 --fileParallelism=false
```

Essa regressão importa o `CONFIG` usado pelo aplicativo, verifica os valores sanitizados e confirma que produção continua recusando variáveis obrigatórias ausentes. Uma rodada adicional registrada nas evidências introduziu `.env`/`.env.local` e uma variável externa com valores sentinela conflitantes; o runner continuou usando o exemplo sanitizado.

O smoke antigo de dependências também está disponível em `npm run test:vitest:dependencias`. Ele valida infraestrutura de testes, não os requisitos de negócio.

Cobertura é uma verificação separada, com maior custo. Execute-a sem outra suite pesada em paralelo:

```bash
NODE_OPTIONS=--max-old-space-size=768 flock /tmp/sgt-vitest.lock \
  npm run test:coverage -- --maxWorkers=1 --fileParallelism=false
```

Os testes unitários/de componentes podem simular respostas HTTP. Seus resultados não demonstram concessões reais, persistência, completude documental ou autorização efetiva do backend. Consulte os relatórios da rodada para saber quais casos receberam E2E real.

## 4. Configurar e compilar o aplicativo

O mecanismo de `.env.example` descrito acima pertence **exclusivamente ao Vitest**. `vite.config.js`, `src/core/config.js` e as validações de produção não ganham valores padrão a partir dele. Antes de executar o aplicativo, configure explicitamente os valores públicos exigidos:

```bash
cd "$SGT_FRONTEND"
test -f .env || cp .env.example .env
chmod 600 .env
```

Edite `.env` para o ambiente correto. `VITE_*` entra no JavaScript entregue ao navegador: não inclua senhas, tokens, chaves privadas ou dados reais nesse prefixo. Mantenha `VITE_SHOW_TEST_CREDENTIALS=false`. Os placeholders institucionais `CONFIGURAR`, `INSTITUICAO_CONFIGURAR` e o CNPJ de exemplo precisam ser substituídos pelos valores públicos aprovados para a publicação.

Um exemplo de conexão ao runtime isolado desta rodada é:

```dotenv
VITE_API_URL=/api
DEV_PROXY_TARGET=http://127.0.0.1:18080
DEV_SERVER_HOST=127.0.0.1
DEV_SERVER_PORT=5173
DEV_HTTPS_ENABLED=false
```

Esse trecho complementa as demais variáveis obrigatórias do exemplo; não constitui um `.env` completo. A porta 18080 é a porta do runtime real preparado nesta rodada. `.env.example` usa 5000 como endereço ilustrativo: ajuste o destino antes de tentar autenticar.

Para validar os valores sem exibi-los no terminal:

```bash
node --input-type=module <<'NODE'
import { readFileSync } from 'node:fs';
import { parseEnv } from 'node:util';
import { criarConfiguracaoFrontend } from './src/core/configuracaoFrontend.js';
criarConfiguracaoFrontend(parseEnv(readFileSync('.env', 'utf8')), { producao: true });
console.log('Configuração pública validada.');
NODE
```

Build:

```bash
NODE_OPTIONS=--max-old-space-size=768 flock /tmp/sgt-vitest.lock npm run build
```

O resultado fica em `dist/`. O build pode compilar mesmo quando a configuração da aplicação falharia ao ser inicializada pelo navegador; por isso valide o ambiente e faça o teste de abertura. Avisos de tamanho de chunk devem ser registrados como avisos, sem apresentá-los como falha de integração nem ignorar erros reais de compilação.

`npm run dev` inicia o servidor de desenvolvimento. `npm run preview` serve o build para inspeção local; não é a implantação de produção. A publicação de `dist/` deve usar um servidor frontend independente com fallback da SPA e encaminhamento real de `/api` e `/config.json` para o Java.

**Não execute `sync:java` ou `build:java`.** Esses scripts retornam erro de bloqueio por projeto. Não copie arquivos para os recursos estáticos do backend.

## 5. Proxy, sessão, CSRF, Origin e TLS

No desenvolvimento, o navegador usa `VITE_API_URL=/api` na mesma origem do frontend. O proxy encaminha `/api` e `/config.json` ao destino configurado. A configuração preserva o cabeçalho **Origin** do navegador e mantém a validação do certificado quando o destino usa HTTPS (`secure:true`). `changeOrigin:true` ajusta o destino HTTP; não deve ser usado para remover o Origin original.

O cliente Axios utiliza `withCredentials:true`. Ele consulta `/api/autenticacao/csrf`, utiliza o `headerName` e o token recebidos nas mutações e renova o contexto quando muda a sessão. As confirmações de senha usam o cabeçalho previsto pelo contrato existente. Não grave esses valores em arquivos públicos nem os injete como constantes de frontend.

No backend, a lista `CORS_ALLOWED_ORIGINS` deve incluir a origem exata usada no browser, com protocolo, host e porta. `localhost` e `127.0.0.1` são origens distintas. No runtime da rodada, os endereços de desenvolvimento 5173 e de inspeção 4173 foram configurados explicitamente. Se alterar host/protocolo/porta, atualize a configuração externa autorizada do runtime; não apague Origin nem desative CSRF para fazer o fluxo passar.

Para servir o frontend local com HTTPS, a configuração existente exige:

```dotenv
DEV_HTTPS_ENABLED=true
DEV_HTTPS_PFX_PATH=/caminho/privado/certificado.p12
DEV_HTTPS_PFX_PASSPHRASE=VALOR_PRIVADO_CONFIGURADO_LOCALMENTE
```

O certificado e a senha não integram o pacote final. Se o Java usa HTTPS com uma CA privada, disponibilize a CA confiável ao Node por configuração externa, por exemplo `NODE_EXTRA_CA_CERTS`, em vez de desabilitar a validação TLS. O HTTP em loopback mostrado neste guia corresponde ao runtime sintético isolado, não à configuração recomendada para tráfego real externo.

Para diagnosticar recusas, diferencie: sessão ausente/expirada, token CSRF, origem não permitida, perfil/escopo e regra negocial. Um 403 de concessão ou contexto histórico não é corrigido alterando o proxy. Refaça login somente quando o fluxo e a coordenação de sessão permitirem.

## 6. Java congelado e runtime real

O E2E utiliza o arquivo original:

```text
bin/sig-gtrat-backend-0.0.1-SNAPSHOT.jar
SHA256 08d483d6e25105c62372efd4e0fd5f61a62ed9dee029745e0e35bd9d8f0ba63d
```

Confira o hash antes de iniciar. Nenhuma etapa deste guia recompila ou substitui o JAR. A árvore de análise `backend/` permanece somente leitura.

Os helpers externos efetivamente utilizados acompanham `test/real-runtime/`. O arquivo `LEIA-ME_RESTAURACAO.md` dessa pasta descreve como copiá-los para `SGT_BASE/runtime/` e restaurar as dependências. Não execute os helpers diretamente da pasta de testes sem montar o layout documentado.

Configuração do runtime preparado na rodada, documentada em `test/real-runtime/README.md`:

| Componente | Valor da rodada |
|---|---|
| Java | Temurin 21.0.12.1+1; heap máximo 768 MiB; metaspace 256 MiB; dois processadores ativos. |
| JAR HTTP | `http://127.0.0.1:18080`. |
| MySQL | 8.4.11 real, loopback 13317; buffer pool 128 MiB; até 20 conexões. |
| Hikari | Pool máximo 8, mínimo ocioso 1. |
| Redis | 8.2.11 real, loopback 26389; database 7; limite 64 MiB. |
| Schema de teste | `sgt_integration_20261101`, sintético e separado. |

O pacote React sozinho não instala nem inicializa esses serviços. O MySQL depende de bibliotecas nativas compatíveis, incluindo a `libaio` fornecida; Redis e o browser também exigem seu runtime e bibliotecas. Preserve os recibos de origem e os manifests dos componentes distribuídos.

As tabelas da aplicação são geridas exclusivamente pelo Hibernate/JPA:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.sql.init.mode=never
```

Não use DDL manual, Flyway, Liquibase ou H2 para substituir a validação com MySQL real. As configurações de banco, Redis, segredos, uploads e relatórios pertencem ao runtime externo privado, nunca ao bundle React.

Se estiver usando exatamente o runtime preparado e entregue com `manage.py`, execute os comandos a partir da raiz que contém `runtime/` e `work/java-frozen/`:

```bash
# Somente na preparação inicial de um datadir vazio, sob coordenação do responsável:
python runtime/manage.py init

# Mantenha cada supervisor ativo em sua própria sessão:
python runtime/manage.py serve
python runtime/manage.py serve-app \
  --expected-jar-sha256 08d483d6e25105c62372efd4e0fd5f61a62ed9dee029745e0e35bd9d8f0ba63d

# Consulta de estado sanitizada:
python runtime/manage.py status
```

`init` recusa datadir não vazio; não repita para “limpar” um ambiente em uso. `serve-app` verifica o hash e usa a trava exclusiva do Java. Não execute uma suite pesada de backend em paralelo com esse processo. Consulte `runtime/README.md` para criação autorizada da fixture, snapshots somente leitura e reinício controlado; esses procedimentos não fazem parte de `npm test`.

## 7. E2E real: pré-requisitos e execução coordenada

Os arquivos `test/e2e/*.cjs` dependem de um ambiente sintético já preparado: Java/MySQL/Redis ativos, frontend apontado ao Java, contas próprias, concessões válidas, dados e estado inicial compatíveis com o cenário. Eles podem realizar operações de negócio reais nessa base de teste. Não execute os mesmos cenários contra produção ou contra uma base que não corresponda à fixture.

O helper utiliza Playwright/Core 1.62.1 e Chrome for Testing 156.0.8078.80. Playwright não foi acrescentado às dependências do aplicativo; seus módulos são preservados no complemento de runtime. Um executável de browser isolado, por si só, não fornece a biblioteca de automação.

Exemplo de variáveis, sem valores de credenciais:

```bash
export SGT_TEST_RUNTIME="/caminho/SGT/runtime"
export SGT_FRONTEND_URL="http://127.0.0.1:5173"
export SGT_E2E_OUTPUT="/caminho/SGT/evidence/browser"
export PLAYWRIGHT_MODULE="/caminho/runtime-automacao/node_modules/playwright"
export CHROMIUM_EXECUTABLE="$SGT_TEST_RUNTIME/chromium/chrome-linux64/chrome"
```

As contas e os IDs são lidos de `runtime/private/e2e-fixture.json`, e os estados autenticados do browser ficam sob `runtime/private/`. Esses arquivos são locais, privados e não acompanham o frontend distribuído. Um novo consumidor precisa preparar legitimamente sua própria fixture; não há login padrão embutido.

Somente após conferir o estado inicial e coordenar a sessão, execute o cenário apropriado, por exemplo:

```bash
NODE_OPTIONS=--max-old-space-size=768 flock /tmp/sgt-e2e.lock \
  node test/e2e/contrato-publicacao.cjs
```

O cenário de publicação exige contrato sintético no estado esperado. Não se presume que repetir o script sobre um contrato já publicado seja uma nova prova válida. Novos logins da mesma conta podem invalidar a sessão anterior; não execute scripts de login HTTP em paralelo com o browser dessa conta.

Para relatar resultado, preserve recibos sanitizados de HTTP, asserções, capturas e hashes. Não publique cookies, tokens, senhas, storageState, arquivos de ambiente ou dados pessoais. Uma prova histórica sintética testa a integração técnica; não soluciona os impedimentos documentais reais registrados na matriz.

## 8. O que acompanhar na entrega final

O pacote React deve conter o código atual, `package.json`, lockfile autêntico, `.env.example`, configurações, scripts e testes. `.env` privado, runtime privado, cache do browser e dependências apontadas por symlink absoluto ficam fora dele.

A restauração completa também utiliza o pacote público de dependências. Os relatórios e evidências informam separadamente baseline, testes RED/GREEN, build, HTTP/E2E real e limites documentais. Este guia descreve como reproduzir cada camada; a aprovação efetiva de uma camada depende do recibo de sua execução.

Na candidata de integração desta rodada passaram 182 testes Vitest em 33 arquivos, o teste legado, o build de produção e 15 cenários reais com 185 asserções. O reinício estrito do JAR comparou todas as 77 tabelas depois de `ReadinessState.ACCEPTING_TRAFFIC` e antes de qualquer HTTP. Esses números descrevem o alcance da evidência; não representam homologação integral dos 198 requisitos ou dos 635 critérios. Consulte as matrizes finais e o parecer independente para as classificações específicas.

A candidata final da fase05 acrescenta a correção dos auxiliares da governança, carga integral de cargos e pesquisa/paginação de cinco nas seis tabelas dessa tela. A regressão final passou com **192 testes em 35 arquivos**, mais o teste legado; cobertura de linhas **62,05%** sobre 156 arquivos instrumentados. O novo cenário de leitura real da governança passou com 15 asserções, levando o conjunto a **16 cenários/200 asserções**. As execuções anteriores não foram recontadas como novos critérios nem repetidas ficticiamente sobre o build final.

O build final também foi gerado com `dist/` inicialmente ausente: 97 arquivos, todos comparados por SHA-256 às respostas do servidor de preview. O diretório anterior foi preservado porque continha assets de builds anteriores. Para empacotar em outro ambiente, use uma pasta de saída limpa ou preserve/renomeie a anterior antes da compilação; não copie arquivos antigos de outra versão para o `dist/` final. Essa orientação se refere somente ao frontend.
