# Mapa parcial por HTTP/TCP — preparação e execução opt-in

Roteiro integrado depois da baseline original. Os comandos abaixo descrevem a execução; os resultados de cada rodada devem ser consultados em seus recibos e no `RELATORIO_JAR_HTTP_REINICIO.md` da entrega. Execute depois da correção/regressão pertinente e da preservação dos respectivos XMLs.

## Arquivos e escopo

| Fonte inicial do draft | Arquivo permanente integrado |
|---|---|
| `PreparacaoMapaParcialTcpIT.java.draft` | `scripts/validacao/PreparacaoMapaParcialTcpIT.java` |
| `validar_mapa_transferencia_parcial_tcp.py.draft` | `scripts/validacao/validar_mapa_transferencia_parcial_tcp.py` |
| `compilar_mapa_parcial_tcp_optin.py.draft` | `scripts/validacao/compilar_mapa_parcial_tcp_optin.py` |

A fixture segue o padrão de `PreparacaoHttpTcpIT`, mas é independente: cria cenário JPA sintético, publica contrato, transfere por tríade sem `composicaoOrigemId`, cria dois feriados municipais sintéticos pelas rotas existentes e grava o documento-oráculo **antes** da perda simulada. O único DML de perda atinge a grade e o município de um mapa recém-criado. O município corrente do catálogo passa a um terceiro valor explícito, divergente da fonte. A publicação original e a versão original da transferência são relidas sem alteração em transação nova. Não prepara vínculo nem frequência no contrato sob teste.

A fixture herda somente a supressão do agendador da infraestrutura de preparação existente. Persistência, transações, autenticação/autorização das rotas e MySQL permanecem reais. Isso é preparação, não evidência TCP. O roteiro Python faz as provas, a alocação e a frequência pelo JAR externo normal, com login e CSRF reais. Não altera normas, prazos ou políticas para obter fechamento: os dias escolhidos estão no passado além da janela operacional normal da configuração existente.

O teste conserva fontes **SEI**. Nenhum PDF é obrigatório nem documento externo autêntico é alegado. Publicação e transferência já geram documentos SEI; o roteiro compara seus DTOs, eventos e evidências de integridade antes/depois/reinício.

## Oráculos do roteiro TCP

1. A fonte preexistente é relida e seu SHA-256 conferido; o mestre divergente é consultado por HTTP.
2. A primeira prova contém somente `jornadaSemanal`, `jornadaComprovada=true`, `municipioComprovado=false`.
3. Novo vínculo/alocação nasce entre as fontes no destino, com composição-base canônica e tríade de destino.
4. Uma frequência de presença é lançada pelo Gestor Local em dia anterior ao prazo normal. Deve nascer `FECHADA`, `VALIDA`, com 480 minutos físicos e município desconhecido na fotografia auditável. O roteiro não chama um fechamento antecipado nem muda configuração de prazo.
5. No segundo dia útil, sem lançamento, o relatório mostra 480 minutos previstos e `SEM_LANCAMENTO`; o feriado municipal não é aplicado enquanto o município está desconhecido.
6. Uma emissão formal congela esse resultado intermediário e seu corte de conhecimento.
7. A segunda prova referencia explicitamente a primeira e contém **somente município**; não repete a grade.
8. A frequência preexistente deve ficar `SUJEITA_REVISAO`, sem perder fechamento, cálculo, município original, horários, autoria ou identidades. Exatamente um evento `IMPACTO_TEMPORAL` é acrescentado e uma pendência é atribuída à gestão de destino.
9. O relatório do segundo dia passa a `FERIADO`, com zero minutos previstos, e sinaliza a pendência. A emissão antiga fica `SUPERADA`, mantendo conteúdo, hash e `fonteConhecidaAte`.
10. K0 não conhece prova; K1 e Kentre conhecem somente grade; K2 conhece as duas. A lista de provas liga os IDs das versões físicas ao instante de registro. Publicação, versão original da transferência, mapas consultáveis, documentos e eventos originais de vínculo/alocação permanecem idênticos.
11. Depois de parada/inicialização registrada pelo supervisor, o mesmo JAR, banco e Redis devem conservar os resultados. Reenvios de ambas as requisições originais não criam prova, pendência ou versão adicional.

### Limites explícitos da API

`AlocacaoVinculoDTO` e `MapeamentoTransferenciaGestaoDTO` não expõem as colunas físicas de grade/município. O roteiro não afirma compará-las integralmente via HTTP: compara os DTOs/eventos disponíveis e observa essas propriedades na fotografia auditável da frequência e no relatório. A verificação física completa das fotografias permanece no teste JUnit/MySQL complementar da coordenação.

`posicao-temporal` produz uma fotografia composta cujo `id` é `null`; o roteiro verifica evento, identidade e gestão efetiva, sem exigir ID físico. O relatório consolidado não possui parâmetro `conhecidoAte`; os cortes K são exercitados nas rotas que o suportam, e a emissão formal conserva o relatório de Kentre.

O consolidado operacional considera somente frequências `VALIDA`, conforme as seções 5.22/5.23 e REL-001 do levantamento original. Antes da prova municipal, a frequência válida integra esse relatório com 480 minutos. Depois, a frequência `SUJEITA_REVISAO` fica fora dos minutos vigentes, e o calendário comprovado torna ambos os dias feriados; o relatório deve sinalizar exatamente uma pendência. Os 480 minutos continuam preservados na fotografia auditável e no conteúdo da emissão anterior. O ensaio exige essas duas condições separadamente. A expectativa inicial de 480 minutos no consolidado posterior foi rejeitada em revisão independente do oráculo, preservando o recibo TCP03 falho e todos os testes originais.

## Execução sequencial na candidata atual

Este roteiro é da candidata Java **0.0.4-operacional-20261101**, com MySQL Server **8.0.45**. O roteiro antigo está preservado em `docs/HISTORICO_BASELINE_0_0_3/scripts/README_MAPA_PARCIAL_TCP.md`; seus diretórios, JAR e portas não são comandos atuais. A documentação não substitui os recibos de execução.

Use a mesma cópia da candidata já compilada e validada, sem retornar a um checkout anterior. Não é necessário um prefixo de diretório específico: os caminhos devem identificar a cópia isolada real, seus XMLs, seus artefatos e uma nova pasta de evidências. Nenhum comando deste roteiro prepara MySQL/Redis, concede privilégios ou cria schema por DDL manual. A infraestrutura sintética deve estar preparada, e a única gestão de schema da aplicação continua sendo Hibernate/JPA com `update` e SQL init `never`.

### 1. Identificar fontes, ambiente e artefatos

Execute a partir do backend corrente. Os parâmetros abaixo são caminhos locais explícitos, sem credenciais em linha de comando:

```bash
export SGT_RUNTIME="$PWD"
: "${SGT_INFRA_ENV:?Arquivo privado de infraestrutura já preparado}"
: "${SGT_TCP_RUN:?Nova pasta de evidências desta execução, sem reutilizar recibos anteriores}"
: "${SGT_XML_ATUAL:?TEST-*.xml da regressão da mesma fonte, contendo java.class.path}"
: "${SGT_JAR_SHA256:?SHA-256 esperado do JAR no manifesto da candidata}"
: "${SGT_MYSQL_BIN:?Caminho do cliente mysql 8.0.45 do pacote conferido}"
export SGT_JAR="$SGT_RUNTIME/target/sig-gtrat-backend-0.0.4-operacional-20261101.jar"
export SGT_HEAVY_LOCK="${SGT_HEAVY_LOCK:-/tmp/sgt-heavy.lock}"
```

O arquivo de infraestrutura é lido como dados, sem `source`. Para estes auxiliares, cada variável usa o formato `export CHAVE='valor'`; não há expansão de shell. Ele deve declarar `JAVA_HOME`, `SGT_MAVEN_EXECUTABLE`, `INTEGRATION_MYSQL_URL`, `INTEGRATION_MYSQL_USER`, `INTEGRATION_MYSQL_PASSWORD`, `DB_USER`, `DB_PASS`, `REDIS_HOST`, `REDIS_PORT` e `REDIS_DATABASE`. Se necessários, também declara `SGT_MAVEN_REPOSITORY`, `SGT_MAVEN_SETTINGS`, `PYTHONPATH` e `LD_LIBRARY_PATH`. Os três campos Redis não são completados com variáveis herdadas do shell. A conta JDBC e a conta do capturador devem ser a conta restrita preparada para a validação. Valores de senha/token não entram nos relatórios públicos.

Nesta continuidade, o MySQL observado usa `127.0.0.1:33076`. Os seis catálogos autorizados são `sgt8045_baseline`, `sgt8045_candidata`, `sgt8045_upgrade`, `sgt8045_http`, `sgt8045_409_red` e `sgt8045_regressao`. O alvo do exemplo HTTP é **sgt8045_http**, sem sobrescrever o estado dos demais. Redis usa `127.0.0.1:26376`; a preparação corrente separa DB7 para integrações, DB8 para HTTP e DB9 para upgrade. Esses valores são configuração explícita, não um mapa de defaults do supervisor. O argumento `--redis-database`, quando fornecido, deve coincidir com o arquivo de infraestrutura.

Confira os bytes e a versão antes da primeira inicialização:

```bash
python3 - "$SGT_JAR" "$SGT_JAR_SHA256" <<'PY_IDENTIDADE'
import hashlib, re, sys, zipfile
from pathlib import Path
jar = Path(sys.argv[1])
assert hashlib.file_digest(jar.open('rb'), 'sha256').hexdigest() == sys.argv[2]
with zipfile.ZipFile(jar) as archive:
    manifest = archive.read('META-INF/MANIFEST.MF').decode()
assert re.search(r'^Implementation-Version: 0\.0\.4-operacional-20261101\r?$', manifest, re.M)
print('Identidade do JAR conferida; nenhuma inicialização executada.')
PY_IDENTIDADE
```

### 2. Compilar somente a fixture auxiliar

O helper usa o classpath do XML corrente e classes principais/de teste da mesma fonte. Não executa o teste, não copia código para `src/test` e não altera o POM. A regressão principal e seus XMLs devem estar arquivados antes de introduzir a classe opt-in em `target/test-classes`.

```bash
python3 "$SGT_RUNTIME/scripts/validacao/compilar_mapa_parcial_tcp_optin.py" \
  --project "$SGT_RUNTIME" --xml-classpath "$SGT_XML_ATUAL" \
  --infra-env "$SGT_INFRA_ENV" \
  --saida "$SGT_TCP_RUN/preparacao/compilacao-fixture.json"
```

O recibo vincula fonte, bytecode, classe base e XML por hash. Não usar XML/classes herdados de outra candidata para preencher um recibo ausente.

### 3. Executar a preparação opt-in e preservar os XMLs

Somente a fixture é executada. Ela exige produto MySQL e versão exata **8.0.45**, tanto por metadados JDBC quanto por `SELECT VERSION()` na mesma conexão, além de `ddl-auto=update`/SQL init `never`. Mantém a perda sintética delimitada ao mapa recém-criado, a preservação da fonte anterior e todos os oráculos negociais descritos acima.

```bash
python3 - "$SGT_RUNTIME" "$SGT_INFRA_ENV" "$SGT_TCP_RUN" "$SGT_HEAVY_LOCK" <<'PY_FIXTURE'
from pathlib import Path
import datetime as dt, fcntl, json, os, shutil, subprocess, sys
project, env_path, run, lock_path = map(Path, sys.argv[1:])
project = project.resolve()
sys.path.insert(0, str(project / 'scripts/validacao'))
from gerenciar_jar_tcp import carregar_ambiente, sha, vivo
env = carregar_ambiente(env_path)
for key in ('INTEGRATION_MYSQL_URL', 'INTEGRATION_MYSQL_USER', 'INTEGRATION_MYSQL_PASSWORD', 'SGT_MAVEN_EXECUTABLE'):
    if not env.get(key):
        raise SystemExit('Configuração explícita ausente: ' + key)
env.setdefault('MAVEN_OPTS', '-Xmx512m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2')
with lock_path.open('a') as lock:
    fcntl.flock(lock, fcntl.LOCK_EX)
    if any(proc.name.isdigit() and vivo(proc.name) for proc in Path('/proc').iterdir()):
        raise SystemExit('Outra JVM está ativa; preservar a execução e aguardar a janela sequencial')
    output, private = run / 'preparacao', run / 'privado'
    output.mkdir(parents=True, exist_ok=True)
    private.mkdir(parents=True, exist_ok=True, mode=0o700)
    private.chmod(0o700)
    fixture, log = private / 'fixture_mapa_parcial_tcp.json', output / 'maven-fixture.log'
    if fixture.exists() or log.exists():
        raise SystemExit('Não sobrescrever preparação anterior/interrompida')
    command = [env['SGT_MAVEN_EXECUTABLE'], '-B', '-ntp', '-T1']
    if env.get('SGT_MAVEN_REPOSITORY'):
        command.append('-Dmaven.repo.local=' + env['SGT_MAVEN_REPOSITORY'])
    if env.get('SGT_MAVEN_SETTINGS'):
        command += ['-s', env['SGT_MAVEN_SETTINGS']]
    command += ['-Pintegration', '-DargLine=', '-DskipTests=false', '-DskipITs=false',
                '-Dtest.jvm.args=-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2',
                '-Dit.test=PreparacaoMapaParcialTcpIT', '-DfailIfNoTests=true',
                '-Dfixture.mapa-parcial.saida=' + str(fixture), 'failsafe:integration-test', 'failsafe:verify']
    start = dt.datetime.now(dt.timezone.utc).isoformat()
    with log.open('x') as stream:
        os.chmod(log, 0o600)
        result = subprocess.run(command, cwd=project, env=env, stdin=subprocess.DEVNULL,
                                stdout=stream, stderr=subprocess.STDOUT)
    names = ['TEST-com.example.siggtrat.auditoria20261029.PreparacaoMapaParcialTcpIT.xml',
             'com.example.siggtrat.auditoria20261029.PreparacaoMapaParcialTcpIT.txt', 'failsafe-summary.xml']
    copied = {}
    for name in names:
        src = project / 'target/failsafe-reports' / name
        if src.is_file():
            dst = output / name
            if dst.exists():
                raise SystemExit('Não sobrescrever XML arquivado')
            shutil.copy2(src, dst)
            copied[name] = sha(dst)
    receipt = dict(inicio_utc=start, fim_utc=dt.datetime.now(dt.timezone.utc).isoformat(),
                   project=str(project), comando_sem_credenciais=command, exit_code=result.returncode,
                   xmls=copied, fixture_privada_gravada=fixture.is_file(),
                   preparacao_auxiliar=True, integra_contagem_suite_principal=False)
    with (output / 'execucao-fixture.json').open('x') as stream:
        json.dump(receipt, stream, ensure_ascii=False, indent=2)
    print(json.dumps(dict(exit_code=result.returncode, fixture_gravada=fixture.is_file())))
    raise SystemExit(result.returncode)
PY_FIXTURE
```

Conferir separadamente o resultado real da fixture: um teste, zero falhas/erros/ignorados, recibos e fonte preexistente disponíveis. O Failsafe atualiza seu resumo no `target`; isso não altera a contagem arquivada da regressão principal. Um `clean` posterior remove a classe opt-in compilada e a preparação deve ser recompilada, sem promover seus resultados antigos a aprovação dos bytes novos.

### 4. Iniciar o mesmo JAR e executar a cadeia HTTP

```bash
python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" iniciar \
  --project "$SGT_RUNTIME" --jar "$SGT_JAR" --infra-env "$SGT_INFRA_ENV" \
  --saida "$SGT_TCP_RUN/jar" --trava "$SGT_HEAVY_LOCK" \
  --database sgt8045_http --redis-database 8 --mysql-port 33076 --port 18086 --fase mapa-primeira

python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" pronto --saida "$SGT_TCP_RUN/jar"
```

Aguardar `pronto=true`, acompanhando o log, sem interpretar uma tentativa ainda indisponível como aprovação. A prontidão é verificada pelo log do Spring, sem uma requisição que altere o banco antes de uma fotografia.

```bash
python3 "$SGT_RUNTIME/scripts/validacao/validar_mapa_transferencia_parcial_tcp.py" \
  --base http://127.0.0.1:18086 --fixture "$SGT_TCP_RUN/privado/fixture_mapa_parcial_tcp.json" \
  --estado "$SGT_TCP_RUN/privado/estado_mapa_parcial_tcp.json" --saida "$SGT_TCP_RUN/recibos" \
  --processo-jar "$SGT_TCP_RUN/jar/PROCESSO.json" --fase criar
```

### 5. Fotografar, reiniciar e reler antes de novas requisições

```bash
python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" encerrar --saida "$SGT_TCP_RUN/jar"
python3 "$SGT_RUNTIME/scripts/validacao/fotografar_persistencia_jar.py" \
  --saida "$SGT_TCP_RUN/jar" --privado "$SGT_TCP_RUN/privado/persistencia" \
  --infra-env "$SGT_INFRA_ENV" --mysql-bin "$SGT_MYSQL_BIN" --fase antes_reinicio

python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" iniciar \
  --project "$SGT_RUNTIME" --jar "$SGT_JAR" --infra-env "$SGT_INFRA_ENV" \
  --saida "$SGT_TCP_RUN/jar" --trava "$SGT_HEAVY_LOCK" \
  --database sgt8045_http --redis-database 8 --mysql-port 33076 --port 18086 --fase mapa-reinicio
python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" pronto --saida "$SGT_TCP_RUN/jar"
python3 "$SGT_RUNTIME/scripts/validacao/fotografar_persistencia_jar.py" \
  --saida "$SGT_TCP_RUN/jar" --privado "$SGT_TCP_RUN/privado/persistencia" \
  --infra-env "$SGT_INFRA_ENV" --mysql-bin "$SGT_MYSQL_BIN" --fase depois_reinicio
```

O supervisor recusa mudança de bytes do JAR, catálogo, endpoint MySQL, porta HTTP ou identidade Redis no reinício. O capturador cruza o recibo do processo com a infraestrutura e compara schema, índices, FKs e valores persistidos. Qualquer diferença legítima de inicialização/job precisa de diagnóstico; não deve ser ocultada, apagada ou reclassificada como aprovação automática.

Somente após a comparação pré-HTTP, reexecutar as consultas e reenvios idempotentes:

```bash
python3 "$SGT_RUNTIME/scripts/validacao/validar_mapa_transferencia_parcial_tcp.py" \
  --base http://127.0.0.1:18086 --fixture "$SGT_TCP_RUN/privado/fixture_mapa_parcial_tcp.json" \
  --estado "$SGT_TCP_RUN/privado/estado_mapa_parcial_tcp.json" --saida "$SGT_TCP_RUN/recibos" \
  --processo-jar "$SGT_TCP_RUN/jar/PROCESSO.json" --fase reinicio
python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" encerrar --saida "$SGT_TCP_RUN/jar"
```

## Preservação e publicação das evidências

Os recibos `criar.json` e `reinicio.json` somente representam sucesso quando a fase termina e todas as asserções passam. Uma falha conserva o estado parcial; usar nova pasta de execução se outra preparação for necessária. Não sobrescrever recibos, snapshots, fontes sintéticas anteriores ou logs para apagar o RED.

`privado/` contém senha sintética e tokens de prévia; conservar modo0700/0600 e separar dos relatórios publicados. Não incluir esse diretório, arquivos `.env` ou logs brutos sem sanitização no pacote público. A fixture evita a cópia extra de exemplos que gravava token sem máscara e mantém o registro mascarado de `AuditoriaHttpSuporte`.

O restaurador aceita somente destino vazio e INSERTs de backup com SHA conferido. A opção antiga de apagar sementes foi removida: contagens iguais não identificam o conteúdo, e nenhum dado existente é excluído para preparar este roteiro. Conservar a baseline e escolher a área de validação definida pela coordenação.

A integração destes auxiliares, sua compilação e os testes de configuração não comprovam o resultado negocial da cadeia. Preparação Java, HTTP TCP, snapshots e reinício têm gates e recibos separados. Na ausência de qualquer um deles, indicar o item como pendente ou bloqueado, sem completar a cadeia com resultados de outra fonte.
