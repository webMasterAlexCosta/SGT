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

## Execução sequencial, inteiramente no runtime em `/tmp`

Os três arquivos já estão em `backend/scripts/validacao` e devem integrar a fotografia da fonte que será executada. Não copiar Java para `src/test`; não criar teste desabilitado ou exclusão de suíte. Nenhum comando abaixo inicia MySQL/Redis ou cria banco/tabela. A coordenação fornece um banco sintético explicitamente autorizado e seu arquivo de ambiente privado.

O projeto de execução deve ser a cópia de fonte corrente mantida em `/tmp/sgt6e_854d965b7ee9/runs/<nome>/backend`, com `target/classes`, `target/test-classes`, JAR e XMLs produzidos pela regressão dessa mesma fonte. Não usar classes ou XMLs herdados de outra fonte. **Não executar estes passos sobre o workspace sincronizado.**

Definir os paths abaixo na sessão da coordenação. São argumentos de ambiente do comando, não conteúdo que deva ser gravado em relatório público. O par usado na cadeia final é `sgt_audit_198_http07` / Redis DB 3, explicitamente admitido pelo supervisor para uma nova base sintética vazia. Os pares das tentativas anteriores foram preservados. Outro par exige configuração explícita e compatível do supervisor e do orquestrador; fixture e JAR devem usar o mesmo banco.

```bash
export SGT_KIT=/workspace/scratch/854d965b7ee9/SGT_GTRAT_6_E_2026-10-31
export SGT_STATE=/tmp/sgt6e_854d965b7ee9
export SGT_INFRA_ENV="$SGT_STATE/sgt_audit_198_http06.env"
export SGT_TCP_RUN="$SGT_STATE/runs/mapa_parcial_tcp_01"
export SGT_MAVEN_REPOSITORY="$SGT_STATE/maven-repository"

# Informar os dois paths reais da execução corrente que a coordenação aprovou.
: "${SGT_RUNTIME:?Defina o backend compilado da fonte corrente em /tmp}"
: "${SGT_XML_ATUAL:?Defina um TEST-*.xml corrente contendo java.class.path}"
```

### 1. Compilar somente a fixture auxiliar

Este helper segue `compilar_fixture_tcp_optin.py`: extrai as dependências do XML corrente e usa as classes principais/de teste do projeto explicitamente indicado. Não executa o teste. O recibo vincula fonte, bytecode, base compilada e XML por SHA-256.

```bash
python3 "$SGT_RUNTIME/scripts/validacao/compilar_mapa_parcial_tcp_optin.py" \
  --project "$SGT_RUNTIME" \
  --xml-classpath "$SGT_XML_ATUAL" \
  --infra-env "$SGT_INFRA_ENV" \
  --saida "$SGT_TCP_RUN/preparacao/compilacao-fixture.json"
```

### 2. Executar somente a preparação opt-in

O comando abaixo lê o ambiente como dados, sem `source` e sem imprimir credenciais. Adquire a mesma trava pesada, recusa outra JVM, executa exclusivamente a fixture e arquiva seus XMLs separadamente. Não roda `clean`, não roda a suíte toda e não recompila a aplicação.

Antes de executar a preparação, a regressão corrente deve estar integralmente arquivada e ligada ao hash da fonte. O Failsafe cria o XML adicional em `target/failsafe-reports` e atualiza seu resumo; portanto **a contagem da suíte principal é a do arquivo já preservado**, não a de um consolidador executado sobre esse `target` depois da preparação. Uma regressão posterior requer o fluxo normal de fonte corrente e `clean`, que recompila apenas `src/test`.

```bash
python3 - "$SGT_RUNTIME" "$SGT_INFRA_ENV" "$SGT_TCP_RUN" "$SGT_STATE/heavy-java.lock" "$SGT_MAVEN_REPOSITORY" <<'PY'
from pathlib import Path
import datetime as dt
import fcntl
import json
import os
import shutil
import subprocess
import sys

project, env_path, run, lock_path, repository = map(Path, sys.argv[1:])
project = project.resolve()
if not str(project).startswith('/tmp/sgt6e_854d965b7ee9/runs/'):
    raise SystemExit('A preparação deve executar na cópia isolada em /tmp')
sys.path.insert(0, str(project / 'scripts/validacao'))
from gerenciar_jar_tcp import carregar_ambiente, sha

env = carregar_ambiente(env_path)
env['SGT_MAVEN_REPOSITORY'] = str(repository)
if not env.get('INTEGRATION_MYSQL_URL'):
    raise SystemExit('Ambiente precisa declarar explicitamente o MySQL sintético')
lock = lock_path.open('a')
fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
for proc in Path('/proc').iterdir():
    if not proc.name.isdigit():
        continue
    try:
        if proc.joinpath('comm').read_text().strip() == 'java':
            raise SystemExit('Outra JVM está ativa; não paralelizar')
    except OSError:
        pass

output = run / 'preparacao'
private = run / 'privado'
output.mkdir(parents=True, exist_ok=True)
private.mkdir(parents=True, exist_ok=True)
private.chmod(0o700)
fixture = private / 'fixture_mapa_parcial_tcp.json'
log = output / 'maven-fixture.log'
if fixture.exists() or log.exists():
    raise SystemExit('Não sobrescrever uma preparação anterior ou interrompida')
command = [env['SGT_MAVEN_EXECUTABLE'], '-Pintegration', '-DargLine=',
           '-DskipTests=false', '-DskipITs=false',
           '-Dit.test=PreparacaoMapaParcialTcpIT', '-DfailIfNoTests=true',
           '-Dfixture.mapa-parcial.saida=' + str(fixture),
           'failsafe:integration-test', 'failsafe:verify']
started = dt.datetime.now(dt.timezone.utc).isoformat()
fd = os.open(log, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, 'w') as stream:
    result = subprocess.run(command, cwd=project, env=env, stdin=subprocess.DEVNULL,
                            stdout=stream, stderr=subprocess.STDOUT)
reports = project / 'target/failsafe-reports'
names = ['TEST-com.example.siggtrat.auditoria20261029.PreparacaoMapaParcialTcpIT.xml',
         'com.example.siggtrat.auditoria20261029.PreparacaoMapaParcialTcpIT.txt',
         'failsafe-summary.xml']
copied = {}
for name in names:
    src = reports / name
    if src.is_file():
        dst = output / name
        shutil.copy2(src, dst)
        copied[name] = sha(dst)
receipt = dict(inicio_utc=started, fim_utc=dt.datetime.now(dt.timezone.utc).isoformat(),
               project=str(project), comando_sem_credenciais=command,
               exit_code=result.returncode, xmls=copied,
               fixture_privada_gravada=fixture.is_file(),
               preparacao_auxiliar=True, integra_contagem_suite_principal=False)
with (output / 'execucao-fixture.json').open('x') as stream:
    json.dump(receipt, stream, ensure_ascii=False, indent=2)
print(json.dumps(dict(exit_code=result.returncode, fixture_gravada=fixture.is_file())))
raise SystemExit(result.returncode)
PY
```

Antes do JAR, conferir que o XML da fixture registra um teste, zero falhas, zero erros e zero ignorados; que o recibo de compilação corresponde à fonte integrada; e que a fixture privada e sua fonte preexistente existem. O helper de preparação não publica os arquivos privados.

### 3. Iniciar o JAR pela coordenação e executar a fase inicial

Usar o supervisor existente com os mesmos paths de runtime e saída em `/tmp`. A porta MySQL desta infraestrutura é **13307**, diferente do default do supervisor; declarar o argumento. O par abaixo é `sgt_audit_198_http06` / Redis 10. O supervisor verifica SHA do JAR, trava pesada, existência de outra JVM e prontidão pelo log.

```bash
python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" iniciar \
  --project "$SGT_RUNTIME" \
  --jar "$SGT_RUNTIME/target/sig-gtrat-backend-0.0.1-SNAPSHOT.jar" \
  --infra-env "$SGT_INFRA_ENV" \
  --saida "$SGT_TCP_RUN/jar" --trava "$SGT_STATE/heavy-java.lock" \
  --database sgt_audit_198_http06 --redis-database 10 \
  --mysql-port 13307 --port 18086 --fase mapa-primeira

python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" pronto \
  --saida "$SGT_TCP_RUN/jar"
```

Executar a próxima chamada somente depois de `pronto` confirmar a inicialização. Se ainda não estiver pronto, repetir apenas a consulta de prontidão, acompanhando o log existente.

```bash
python3 "$SGT_RUNTIME/scripts/validacao/validar_mapa_transferencia_parcial_tcp.py" \
  --base http://127.0.0.1:18086 \
  --fixture "$SGT_TCP_RUN/privado/fixture_mapa_parcial_tcp.json" \
  --estado "$SGT_TCP_RUN/privado/estado_mapa_parcial_tcp.json" \
  --saida "$SGT_TCP_RUN/recibos" \
  --processo-jar "$SGT_TCP_RUN/jar/PROCESSO.json" --fase criar
```

### 4. Encerrar, reiniciar os mesmos bytes e conferir persistência

```bash
python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" encerrar \
  --saida "$SGT_TCP_RUN/jar"

python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" iniciar \
  --project "$SGT_RUNTIME" \
  --jar "$SGT_RUNTIME/target/sig-gtrat-backend-0.0.1-SNAPSHOT.jar" \
  --infra-env "$SGT_INFRA_ENV" \
  --saida "$SGT_TCP_RUN/jar" --trava "$SGT_STATE/heavy-java.lock" \
  --database sgt_audit_198_http06 --redis-database 10 \
  --mysql-port 13307 --port 18086 --fase mapa-reinicio

python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" pronto \
  --saida "$SGT_TCP_RUN/jar"
```

Depois da prontidão:

```bash
python3 "$SGT_RUNTIME/scripts/validacao/validar_mapa_transferencia_parcial_tcp.py" \
  --base http://127.0.0.1:18086 \
  --fixture "$SGT_TCP_RUN/privado/fixture_mapa_parcial_tcp.json" \
  --estado "$SGT_TCP_RUN/privado/estado_mapa_parcial_tcp.json" \
  --saida "$SGT_TCP_RUN/recibos" \
  --processo-jar "$SGT_TCP_RUN/jar/PROCESSO.json" --fase reinicio

python3 "$SGT_RUNTIME/scripts/validacao/gerenciar_jar_tcp.py" encerrar \
  --saida "$SGT_TCP_RUN/jar"
```

## Preservação e publicação das evidências

Os recibos `recibos/criar.json` e `recibos/reinicio.json` somente indicam sucesso quando a fase conclui e todas as asserções passam. Recibo já existente não é sobrescrito. Falha deixa o estado parcial preservado; usar outra preparação/nome se for necessária uma nova criação, mantendo os recibos RED.

`privado/` contém senha sintética da fixture e tokens de prévia necessários aos reenvios. O estado é gravado com modo0600 e não pode ficar dentro da pasta pública de recibos. Não incluir esse diretório, arquivos `.env` ou logs brutos de preparação/JAR em pacote público sem análise e sanitização. `Cliente` mascara senha/token nos recibos HTTP. A fixture substitui apenas o helper de exemplos de `ContratosHttpBase` para evitar tokens sem máscara no arquivo adicional `contratos-exemplos.jsonl`; mantém o registro mascarado de `AuditoriaHttpSuporte`.

Copiar recibos e manifestos para o workspace somente após os processos correspondentes terem terminado, conforme a política de isolamento vigente. A integração destes scripts não comprova execução, atendimento integral dos requisitos ou homologação; esses resultados dependem dos recibos efetivos da fonte corrente.
