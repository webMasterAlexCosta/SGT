#!/usr/bin/env python3
"""Observe an externally prepared MySQL 8.0.45 runtime and supervise its reviewed JAR."""
from __future__ import annotations

import argparse
import configparser
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import time
from urllib.parse import parse_qs, unquote, urlsplit
import uuid
import zipfile

ROOT = Path(__file__).resolve().parent
PRIVATE = EVIDENCE = JAR = SCHEMA = None
EXPECTED_MYSQL_VERSION = "8.0.45"
EXPECTED_JAR_VERSION = "0.0.4-operacional-20261101"


def _path(value, label, directory=False):
    if not isinstance(value, str) or not Path(value).is_absolute():
        raise RuntimeError(f"Informe caminho absoluto explícito para {label}")
    path = Path(value).resolve()
    if not (path.is_dir() if directory else path.is_file()):
        raise RuntimeError(f"Caminho preparado inexistente para {label}")
    return path


def require_configuration():
    """Read explicit private configuration; never prepare directories or credentials."""
    global PRIVATE, EVIDENCE, JAR, SCHEMA
    location = os.environ.get("SGT_RUNTIME_CONFIG")
    if not location:
        raise RuntimeError("Defina SGT_RUNTIME_CONFIG para a configuração privada preparada externamente")
    try:
        config = json.loads(_path(location, "SGT_RUNTIME_CONFIG").read_text())
        PRIVATE = _path(config["private_dir"], "private_dir", directory=True)
        EVIDENCE = _path(config["evidence_dir"], "evidence_dir", directory=True)
        JAR = _path(config["java"]["jar"], "java.jar")
        SCHEMA = config["schema"]
        if not isinstance(SCHEMA, str) or not re.fullmatch(r"[A-Za-z0-9_]{1,64}", SCHEMA):
            raise RuntimeError("Informe catálogo de aplicação explícito e válido")
        for group, name in [("mysql", "client"), ("mysql", "client_options"), ("java", "executable"), ("java", "app_env")]:
            _path(config[group][name], f"{group}.{name}")
        if config["mysql"].get("mysqld"):
            _path(config["mysql"]["mysqld"], "mysql.mysqld")
        if not re.fullmatch(r"[a-fA-F0-9]{64}", str(config["java"]["sha256"])):
            raise RuntimeError("SHA-256 do JAR candidato obrigatório")
        if config["java"].get("version") != EXPECTED_JAR_VERSION:
            raise RuntimeError(f"O JAR informado deve pertencer à candidata {EXPECTED_JAR_VERSION}")
        if "java-frozen" in JAR.parts or "baseline" in JAR.parts:
            raise RuntimeError("O JAR deve pertencer à candidata, fora da árvore de referência")
        return config
    except (KeyError, TypeError, ValueError):
        raise RuntimeError("Configuração privada incompleta ou inválida; consulte o guia atual") from None


def _client_settings(config):
    options = configparser.RawConfigParser()
    try:
        options.read(config["mysql"]["client_options"])
        if options.sections() != ["client"]:
            raise ValueError()
        allowed = {"user", "password", "host", "port", "protocol", "ssl-mode", "ssl-ca", "ssl-cert", "ssl-key", "connect-timeout"}
        if set(options["client"]) - allowed:
            raise ValueError()
        settings = {key: value.strip().strip('\"\'') for key, value in options["client"].items()}
        if not settings.get("user") or settings["user"].lower() == "root":
            raise RuntimeError("Use conta de aplicação explícita; cliente root não é permitido")
        if settings.get("host") not in {"127.0.0.1", "localhost"}:
            raise RuntimeError("A conexão de teste deve informar host loopback explícito")
        port = int(settings["port"])
        if not 1 <= port <= 65535 or settings.get("protocol", "TCP").upper() != "TCP":
            raise ValueError()
        return settings
    except (configparser.Error, KeyError, ValueError):
        raise RuntimeError("Opções privadas do cliente de aplicação inválidas; não há fallback administrativo") from None


def _app_properties(config):
    properties = {}
    for line in Path(config["java"]["app_env"]).read_text().splitlines():
        if line.strip() and not line.lstrip().startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            properties[key.strip()] = value.strip().strip('\"\'')
    for key in properties:
        normalized = key.lower().replace("_", ".").replace("-", ".")
        if normalized.startswith(("spring.datasource.", "spring.config.", "spring.application.json")) or key.upper() in {"JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"}:
            raise RuntimeError("Configuração privada não pode substituir datasource, carregamento de configuração ou opções da JVM")
    settings = _client_settings(config)
    if not properties.get("DB_USER") or properties["DB_USER"].lower() == "root" or properties["DB_USER"] != settings["user"]:
        raise RuntimeError("DB_USER deve ser a mesma conta de aplicação do cliente privado")
    url = properties.get("DB_URL", "")
    if not url.startswith("jdbc:mysql://"):
        raise RuntimeError("DB_URL MySQL explícita obrigatória")
    parsed = urlsplit(url.removeprefix("jdbc:"))
    if parsed.username is not None or parsed.password is not None or parsed.fragment:
        raise RuntimeError("DB_URL não pode conter credenciais nem fragmentos")
    if parsed.hostname != settings["host"] or parsed.port != int(settings["port"]) or unquote(parsed.path.lstrip("/")) != SCHEMA:
        raise RuntimeError("DB_URL deve apontar para o mesmo host, porta e catálogo observado")
    parameters = {}
    for key, values in parse_qs(parsed.query, keep_blank_values=True).items():
        parameters.setdefault(key.lower(), []).extend(values)
    if {"user", "password", "username", "propertiestransform"} & parameters.keys():
        raise RuntimeError("DB_URL não pode substituir a conta de aplicação nem suas propriedades")
    if any(value.lower() != "false" for value in parameters.get("createdatabaseifnotexist", [])):
        raise RuntimeError("DB_URL não pode habilitar createDatabaseIfNotExist; catálogo deve estar preparado")
    expected = {"spring.jpa.hibernate.ddl-auto": "update", "spring.sql.init.mode": "never"}
    for key, value in expected.items():
        supplied = properties.get(key, properties.get(key.upper().replace(".", "_").replace("-", "_")))
        if supplied != value:
            raise RuntimeError(f"Configuração externa deve declarar {key}={value}")
    return properties


def _mysql_raw(config, query):
    _client_settings(config)
    args = [config["mysql"]["client"], f"--defaults-file={config['mysql']['client_options']}",
            "--protocol=TCP", f"--database={SCHEMA}", "--batch", "--skip-column-names"]
    environment = {key: value for key, value in os.environ.items() if key not in {"MYSQL_PWD", "MYSQL_HOST", "MYSQL_TCP_PORT", "MYSQL_UNIX_PORT", "MYSQL_TEST_LOGIN_FILE"}}
    # O cliente 8.0 não oferece --no-login-paths. Um login-file vazio impede
    # que .mylogin.cnf substitua a conta do defaults-file privado explícito.
    environment["MYSQL_TEST_LOGIN_FILE"] = os.devnull
    result = subprocess.run(args, input=query, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=environment)
    if result.returncode:
        raise RuntimeError(f"Consulta pelo cliente de aplicação falhou ({result.returncode}); saída privada suprimida")
    return result.stdout.strip()


def _observe_mysql(config):
    settings = _client_settings(config)
    binary_version = None
    if config["mysql"].get("mysqld"):
        result = subprocess.run([config["mysql"]["mysqld"], "--version"], text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        found = re.search(r"\bVer\s+(\S+)", result.stdout + result.stderr)
        if result.returncode or not found or found.group(1) != EXPECTED_MYSQL_VERSION:
            raise RuntimeError(f"O binário mysqld informado deve ser exatamente MySQL {EXPECTED_MYSQL_VERSION}")
        binary_version = found.group(1)
    observed = _mysql_raw(config, "SELECT VERSION(), @@server_uuid, DATABASE(), @@port, CURRENT_USER();")
    values = observed.split("\t")
    if len(values) != 5:
        raise RuntimeError("A consulta do servidor não devolveu versão, UUID, catálogo, porta e usuário")
    version, server_uuid, schema, port, current_user = values
    if version != EXPECTED_MYSQL_VERSION:
        raise RuntimeError(f"Servidor observado não é MySQL {EXPECTED_MYSQL_VERSION}")
    try:
        uuid.UUID(server_uuid)
    except ValueError:
        raise RuntimeError("UUID do servidor MySQL não confirmado") from None
    if schema != SCHEMA or int(port) != int(settings["port"]) or current_user.split("@", 1)[0] != settings["user"]:
        raise RuntimeError("Identidade observada do banco diverge da configuração de aplicação")
    return {"version": version, "version_source": "SELECT VERSION()", "binary_version": binary_version,
            "server_uuid": server_uuid, "schema": schema, "host": settings["host"], "port": int(port), "current_user": current_user}


def mysql(query):
    """Only the read and transaction statements needed by status/snapshot consumers."""
    transaction = {"SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ", "START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY", "COMMIT"}
    for statement in query.split(";"):
        normalized = " ".join(statement.split()).upper()
        if not normalized:
            continue
        if normalized not in transaction and not normalized.startswith("SELECT "):
            raise RuntimeError("O supervisor admite apenas consultas de leitura e transação READ ONLY")
        if re.search(r"\bINTO\s+(OUTFILE|DUMPFILE)\b", normalized):
            raise RuntimeError("A consulta não pode gravar arquivos no servidor")
    config = require_configuration()
    _observe_mysql(config)
    return _mysql_raw(config, query)


def public_json(name, value):
    require_configuration()
    if not re.fullmatch(r"[A-Za-z0-9_-]+\.json", name):
        raise RuntimeError("Nome de recibo inválido")
    (EVIDENCE / name).write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n")


def _jar_identity(config, expected_sha256=None):
    expected = config["java"]["sha256"].lower()
    if expected_sha256 is not None and expected_sha256.lower() != expected:
        raise RuntimeError("SHA-256 informado diverge da configuração explícita do candidato")
    with JAR.open("rb") as stream:
        actual = hashlib.file_digest(stream, "sha256").hexdigest()
    if actual != expected:
        raise RuntimeError("SHA-256 do JAR fornecido diverge do candidato revisado")
    with zipfile.ZipFile(JAR) as jar:
        manifest = jar.read("META-INF/MANIFEST.MF").decode("utf-8")
    found = re.search(r"(?m)^Implementation-Version:\s*([^\r\n]+)", manifest)
    if not found or found.group(1) != config["java"]["version"]:
        raise RuntimeError("A versão do manifest do JAR diverge da candidata declarada")
    return {"jar_path": str(JAR), "jar_sha256": actual, "artifact_version": found.group(1)}


def _java_version(config):
    result = subprocess.run([config["java"]["executable"], "-version"], text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    found = re.search(r'version "(17(?:\.[^" ]+)?)"', result.stdout + result.stderr)
    if result.returncode or not found:
        raise RuntimeError("O executável Java preparado deve ser JDK 17")
    return found.group(1)


def _running_java():
    try:
        pid = int((PRIVATE / "java.pid").read_text())
        command = Path(f"/proc/{pid}/cmdline").read_bytes().decode().split("\0")
        position = command.index("-jar")
        if Path(command[position + 1]).resolve() == JAR:
            return pid
    except (OSError, ValueError, IndexError):
        pass
    return None


def _process_identity(pid, proc_root=Path("/proc")):
    """Bind a launch to a process generation, since Linux can reuse its PID."""
    try:
        fields = (proc_root / str(pid) / "stat").read_text().rsplit(")", 1)[1].split()
        start_ticks = int(fields[19])
        boot_id = (proc_root / "sys/kernel/random/boot_id").read_text().strip()
        if fields[0] == "Z" or start_ticks < 0 or not boot_id:
            raise ValueError()
        return {"start_ticks": start_ticks, "kernel_boot_id": boot_id}
    except (OSError, IndexError, ValueError):
        raise RuntimeError("Não foi possível vincular o lançamento à identidade viva do processo") from None


def status():
    config = require_configuration()
    _app_properties(config)
    observed = _observe_mysql(config)
    identity = _jar_identity(config)
    pid = _running_java()
    result = {"observation_id": str(uuid.uuid4()), "checked_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
              "mysql": observed, "java": {**identity, "runtime_version": _java_version(config), "pid": pid, "running": pid is not None},
              "schema_management": "Hibernate/JPA; ddl-auto=update; spring.sql.init.mode=never",
              "redis_observed_by_this_helper": False}
    public_json(f"runtime-status-{result['observation_id']}.json", result)
    public_json("runtime-status.json", result)
    print(json.dumps(result, indent=2), flush=True)
    return result


def serve_app(expected_sha256):
    config = require_configuration()
    properties = _app_properties(config)
    identity = _jar_identity(config, expected_sha256)
    observed = _observe_mysql(config)
    java_version = _java_version(config)
    app_dir = _path(str(PRIVATE / "app"), "private/app", directory=True)
    report_dir = _path(str(PRIVATE / "reports"), "private/reports", directory=True)
    with (PRIVATE / "heavy-java.lock").open("a") as lock, (app_dir / "java.log").open("ab") as log:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        command = [config["java"]["executable"], "-Xms128m", "-Xmx768m", "-XX:MaxMetaspaceSize=256m", "-XX:ActiveProcessorCount=2",
                   f"-Djava.io.tmpdir={report_dir}", "-Duser.timezone=America/Sao_Paulo", "-jar", str(JAR),
                   f"--env-file={config['java']['app_env']}", "--spring.jpa.hibernate.ddl-auto=update", "--spring.sql.init.mode=never",
                   "--logging.level.org.springframework.boot.availability=DEBUG"]
        environment = {key: value for key, value in os.environ.items() if key in {"PATH", "LANG", "LC_ALL", "LC_CTYPE", "TZ", "LD_LIBRARY_PATH"}}
        # Record the boundary before Popen: the new JVM can write immediately.
        log_offset = os.fstat(log.fileno()).st_size
        started_utc = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
        child = subprocess.Popen(command, cwd=app_dir, env={**environment, **properties}, stdout=log, stderr=log)
        (PRIVATE / "java.pid").write_text(str(child.pid) + "\n")
        supervisor = PRIVATE / "app-supervisor.pid"
        supervisor.write_text(str(os.getpid()) + "\n")
        stopping = False
        def request_stop(_signal, _frame):
            nonlocal stopping
            stopping = True
        signal.signal(signal.SIGTERM, request_stop)
        signal.signal(signal.SIGINT, request_stop)
        try:
            process = _process_identity(child.pid)
            public_json("jar-process.json", {**identity, "pid": child.pid, **process,
                        "runtime_version": java_version, "mysql": observed,
                        "started_utc": started_utc, "log_offset": log_offset, "readiness_confirmed": False})
            while not stopping:
                if child.poll() is not None:
                    raise RuntimeError("A JVM candidata encerrou; consulte o log privado")
                time.sleep(0.25)
        finally:
            if child.poll() is None:
                child.terminate()
            try:
                child.wait(timeout=30)
            except subprocess.TimeoutExpired:
                child.kill(); child.wait()
            supervisor.unlink(missing_ok=True)


def stop(name):
    require_configuration()
    if name != "app":
        raise RuntimeError("Este supervisor não inicia nem encerra serviços de banco/Redis")
    supervisor = PRIVATE / "app-supervisor.pid"
    if not supervisor.exists():
        return
    pid = int(supervisor.read_text())
    command = Path(f"/proc/{pid}/cmdline").read_bytes().decode().split("\0")
    if str(ROOT / "manage.py") not in command or "serve-app" not in command or _running_java() is None:
        raise RuntimeError("PID não corresponde ao supervisor e JAR candidatos configurados")
    os.kill(pid, signal.SIGTERM)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["status", "serve-app", "stop-app"])
    parser.add_argument("--expected-jar-sha256")
    args = parser.parse_args()
    if args.action == "serve-app":
        if not args.expected_jar_sha256:
            parser.error("serve-app exige --expected-jar-sha256 do candidato revisado")
        serve_app(args.expected_jar_sha256)
    elif args.action == "stop-app":
        stop("app")
    else:
        status()


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"Runtime interrompido: {type(error).__name__}: {error}", file=sys.stderr)
        raise SystemExit(1)
