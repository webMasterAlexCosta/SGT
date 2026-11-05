#!/usr/bin/env python3
"""Supervisor do JAR local sintético: SHA fixo, schema Hibernate update e um JVM pesado.

Usa infra.env como dados (sem executar shell); nunca imprime seus valores.
Readiness vem exclusivamente do log para permitir a fotografia antes de HTTP.
"""
import argparse
import datetime as dt
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import signal
import socket
import subprocess
import time
from configuracao_mysql import SCHEMAS_MYSQL8045

# Nomes legados específicos continuam reconhecidos; os seis catálogos desta
# candidata são explícitos. O Redis vem da infraestrutura, sem mapa implícito.
BANCOS_HTTP_AUTORIZADOS = SCHEMAS_MYSQL8045 | frozenset({
    "sgt_audit_198_http", "sgt_audit_198_http06", "sgt_audit_198_http07", "sgt_audit_198_baseline"})


def conexao_redis_http(database, env, redis_database=None):
    if database not in BANCOS_HTTP_AUTORIZADOS:
        raise SystemExit("Banco não autorizado para o roteiro HTTP sintético.")
    if env.get("REDIS_HOST") not in ("127.0.0.1", "localhost"):
        raise SystemExit("Redis do roteiro deve informar host loopback explícito.")
    port = env.get("REDIS_PORT")
    if not re.fullmatch(r"[0-9]+", str(port)) or not 0 < int(port) < 65536:
        raise SystemExit("REDIS_PORT deve ser informado explicitamente como porta TCP válida.")
    value = env.get("REDIS_DATABASE")
    if isinstance(value, bool) or not re.fullmatch(r"[0-9]+", str(value)):
        raise SystemExit("REDIS_DATABASE deve ser informado explicitamente no ambiente de infraestrutura.")
    value = int(value)
    if redis_database is not None and (isinstance(redis_database, bool)
            or not re.fullmatch(r"[0-9]+", str(redis_database)) or int(redis_database) != value):
        raise SystemExit("--redis-database deve coincidir com REDIS_DATABASE da infraestrutura.")
    return dict(redis_host=env["REDIS_HOST"], redis_port=int(port), redis_database=value)


def conferir_redis_reinicio(state, connection):
    if state and any(state.get(key) != value for key, value in connection.items()):
        raise SystemExit("Reinício exige a mesma conexão Redis registrada; preserve a cadeia anterior.")


def carregar_ambiente(path):
    env = os.environ.copy()
    # O destino Redis pertence à infraestrutura desta cadeia, nunca a defaults
    # ou variáveis herdadas de outra execução do shell chamador.
    for chave in ("REDIS_HOST", "REDIS_PORT", "REDIS_DATABASE"):
        env.pop(chave, None)
    for linha in Path(path).read_text().splitlines():
        if not linha.startswith("export ") or "=" not in linha:
            continue
        chave, valor = linha[7:].split("=", 1)
        if chave == "PATH":
            continue
        if not re.fullmatch(r"[A-Z][A-Z0-9_]*", chave):
            raise ValueError("Chave de ambiente inválida; conteúdo não executado.")
        partes = shlex.split(valor)
        if len(partes) != 1:
            raise ValueError("Export deve conter um valor shell quoted, sem expansão.")
        env[chave] = partes[0]
    return env


def sha(path):
    with Path(path).open("rb") as file:
        return hashlib.file_digest(file, "sha256").hexdigest()


def agora():
    return dt.datetime.now(dt.timezone.utc).isoformat()


def vivo(pid, jar=None):
    proc = Path("/proc") / str(pid)
    try:
        estatuto = proc.joinpath("stat").read_text().split(") ", 1)[1].split()[0]
        return proc.joinpath("comm").read_text().strip() == "java" and estatuto != "Z" and (
            jar is None or os.fsencode(jar) in proc.joinpath("cmdline").read_bytes().split(b"\0"))
    except OSError:
        return False


def salvar(path, state):
    path.write_text(json.dumps(state, ensure_ascii=False, indent=2) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("acao", choices=("iniciar", "pronto", "encerrar", "estado"))
    parser.add_argument("--saida", type=Path, required=True)
    parser.add_argument("--project", type=Path)
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--infra-env", type=Path)
    parser.add_argument("--trava", type=Path)
    parser.add_argument("--fase", default="primeira")
    parser.add_argument("--database")
    parser.add_argument("--redis-database", type=int)
    parser.add_argument("--port", type=int, default=18086)
    parser.add_argument("--mysql-port", type=int)
    args = parser.parse_args()
    args.saida.mkdir(parents=True, exist_ok=True)
    arquivo = args.saida / "PROCESSO.json"
    state = json.loads(arquivo.read_text()) if arquivo.exists() else {}
    if args.acao == "estado":
        print(json.dumps(dict(state, em_execucao=vivo(state.get("pid", 0), state.get("jar"))), ensure_ascii=False)); return
    if args.acao == "pronto":
        ready = vivo(state.get("pid", 0), state.get("jar")) and "ReadinessState changed to ACCEPTING_TRAFFIC" in Path(state["log"]).read_text(errors="replace")
        if ready and not any(e["acao"] == "pronto" and e["pid"] == state["pid"] for e in state["eventos"]):
            state["eventos"].append(dict(acao="pronto", utc=agora(), pid=state["pid"], jar_sha256=state["sha256"],
                                          database=state.get("database"), redis_database=state.get("redis_database")))
            salvar(arquivo, state)
        print(json.dumps(dict(pronto=bool(ready), pid=state.get("pid"), fase=state.get("fase"))))
        raise SystemExit(0 if ready else 3)
    if args.acao == "encerrar":
        pid = state.get("pid", 0)
        if vivo(pid):
            if not vivo(pid, state.get("jar")):
                raise SystemExit("PID pertence a outro Java; nenhuma ação realizada.")
            os.kill(pid, signal.SIGTERM)
            limite = time.monotonic() + 40
            while vivo(pid) and time.monotonic() < limite:
                time.sleep(0.2)
            if vivo(pid):
                raise SystemExit("JAR ainda encerrando; diagnóstico necessário, nenhum SIGKILL aplicado.")
        state.setdefault("eventos", []).append(dict(acao="encerrado", utc=agora(), pid=pid, jar_sha256=state.get("sha256"),
                                                    database=state.get("database"), redis_database=state.get("redis_database")))
        salvar(arquivo, state); print("JAR encerrado; PID e horário registrados."); return
    if not all((args.project, args.infra_env, args.trava)):
        parser.error("iniciar exige --project, --infra-env e --trava")
    if args.database not in BANCOS_HTTP_AUTORIZADOS:
        raise SystemExit("Banco não autorizado para o roteiro HTTP sintético.")
    if args.mysql_port is None or not 0 < args.mysql_port < 65536:
        raise SystemExit("iniciar exige --mysql-port explícita para a instância sintética preparada.")
    if not re.fullmatch(r"[A-Za-z0-9_-]+", args.fase):
        raise SystemExit("Nome de fase inválido.")
    project = args.project.resolve()
    jars = [args.jar.resolve()] if args.jar else sorted((project / "target").glob("*.jar"))
    if len(jars) != 1:
        raise SystemExit("Indique exatamente um JAR executável.")
    jar = jars[0].resolve(); digest = sha(jar)
    if state and (state.get("sha256") != digest or state.get("database") != args.database or state.get("base_http") != f"http://127.0.0.1:{args.port}"):
        raise SystemExit("Reinício exige os mesmos bytes do JAR, banco e porta HTTP. Preserve a prova anterior.")
    if state and (state.get("mysql_host") != "127.0.0.1" or state.get("mysql_port") != args.mysql_port):
        raise SystemExit("Reinício exige a mesma conexão MySQL registrada; preserve a cadeia anterior.")
    if state and state["eventos"][-1]["acao"] != "encerrado":
        raise SystemExit("É obrigatório registrar o encerramento antes do reinício.")
    env = carregar_ambiente(args.infra_env)
    redis = conexao_redis_http(args.database, env, args.redis_database)
    conferir_redis_reinicio(state, redis)
    env["REDIS_DATABASE"] = str(redis["redis_database"])
    lock = args.trava.open("a")
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    for proc in Path("/proc").iterdir():
        if proc.name.isdigit() and vivo(proc.name):
            raise SystemExit("Existe outra JVM. Aguarde a suíte pesada terminar.")
    with socket.socket() as check:
        check.bind(("127.0.0.1", args.port))
    jdbc = (f"jdbc:mysql://127.0.0.1:{args.mysql_port}/{args.database}"
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8")
    env.update(DB_URL=jdbc, SERVER_PORT=str(args.port), SERVER_ADDRESS="127.0.0.1", DB_MAX_POOL_SIZE="8", DB_MIN_IDLE="2",
               JPA_DDL_AUTO="update", JPA_OPEN_IN_VIEW="false", CACHE_TYPE="redis", REDIS_HTTP_GET_ENABLED="true",
               APP_SEED_USUARIOS_GLOBAIS_ENABLED="false", APP_SEED_GESTORES_SE_GESTAO_LOCAL_ENABLED="false",
               GMAIL_EMAIL_ENABLED="false", TZ="America/Sao_Paulo")
    java = Path(env["JAVA_HOME"]) / "bin/java"
    command = [str(java), "-Xmx768m", "-XX:MaxMetaspaceSize=256m", "-Duser.timezone=America/Sao_Paulo", "-jar", str(jar),
               "--env-file=./src/test/resources/auditoria-endpoints.env", "--spring.jpa.hibernate.ddl-auto=update",
               "--spring.sql.init.mode=never", "--spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
               "--spring.jpa.open-in-view=false", "--spring.main.banner-mode=off", "--app.email.enabled=false",
               "--spring.data.redis.database=" + str(redis["redis_database"]),
               "--app.documentos.diretorio=target/auditoria-http/documentos", "--logging.level.root=WARN",
               "--logging.level.com.example.siggtrat=INFO", "--logging.level.org.springframework.boot.availability=DEBUG"]
    logpath = (args.saida / (args.fase + ".log")).resolve()
    if logpath.exists():
        raise SystemExit("Não sobrescrever o log de uma fase anterior.")
    fd = os.open(logpath, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as log:
        process = subprocess.Popen(command, cwd=project, env=env, stdout=log, stderr=subprocess.STDOUT,
                                   stdin=subprocess.DEVNULL, start_new_session=True, pass_fds=(lock.fileno(),))
    state.update(jar=str(jar), sha256=digest, pid=process.pid, base_http=f"http://127.0.0.1:{args.port}", fase=args.fase,
                 database=args.database, mysql_host="127.0.0.1", mysql_port=args.mysql_port, project=str(project), log=str(logpath),
                 **redis,
                 argumentos_sem_credenciais=command[1:], heap_maximo_mib=768, metaspace_maximo_mib=256,
                 limite_cgroup=Path("/sys/fs/cgroup/memory.max").read_text().strip(),
                 prontidao="ApplicationAvailabilityBean ACCEPTING_TRAFFIC no log; nenhuma requisição HTTP de readiness")
    state.setdefault("eventos", []).append(dict(acao="iniciado", utc=agora(), pid=process.pid, jar_sha256=digest, fase=args.fase,
                                                database=args.database, redis_database=redis["redis_database"]))
    salvar(arquivo, state)
    print(json.dumps(dict(pid=process.pid, fase=args.fase, jar_sha256=digest, base_http=state["base_http"])))


if __name__ == "__main__":
    main()
