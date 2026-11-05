"""Identidade explícita para ferramentas de validação; nunca provisiona banco ou contas."""
import os
import re

SCHEMAS_MYSQL8045 = frozenset({
    "sgt8045_baseline", "sgt8045_candidata", "sgt8045_upgrade", "sgt8045_http",
    "sgt8045_409_red", "sgt8045_regressao"})


def conexao_mysql():
    usuario = os.environ.get("MYSQL_USER")
    senha = os.environ.get("MYSQL_PWD")
    if not usuario or not usuario.strip() or senha is None or not senha.strip():
        raise ValueError("Configuração MySQL: informe MYSQL_USER e MYSQL_PWD explicitamente; não há fallback administrativo.")
    usuario = usuario.strip()
    if usuario.lower() in {"root", "mysql.sys", "mysql.session", "mysql.infoschema"}:
        raise ValueError("Configuração MySQL: use uma conta própria restrita ao schema sintético.")
    host = os.environ.get("MYSQL_HOST", "127.0.0.1")
    if host not in {"127.0.0.1", "localhost"}:
        raise ValueError("Configuração MySQL: somente servidor local de validação.")
    try:
        porta = int(os.environ.get("MYSQL_TCP_PORT", "3306"))
    except ValueError:
        raise ValueError("Configuração MySQL: MYSQL_TCP_PORT deve ser uma porta TCP válida.") from None
    if not 0 < porta < 65536:
        raise ValueError("Configuração MySQL: MYSQL_TCP_PORT deve ser uma porta TCP válida.")
    return {"host": host, "port": porta, "user": usuario, "password": senha}


def argumentos_cliente(configuracao):
    # As credenciais explícitas não podem ser substituídas por defaults pessoais.
    # A senha permanece em MYSQL_PWD, nunca na linha de comando/publicação.
    return [os.environ.get("MYSQL_BIN", "mysql"), "--no-defaults",
            "--protocol=TCP", "--host=" + configuracao["host"],
            "--port=" + str(configuracao["port"]), "--user=" + configuracao["user"],
            "--batch", "--raw", "--skip-column-names", "--default-character-set=utf8mb4"]


def ambiente_cliente():
    # MySQL 8.0 lê .mylogin.cnf mesmo com --no-defaults; --no-login-paths
    # só existe a partir de 8.2. O caminho nulo é explícito e não altera
    # arquivos pessoais nem o ambiente do processo chamador.
    return {**os.environ, "MYSQL_TEST_LOGIN_FILE": os.devnull}


def schema_sintetico(database):
    return bool(re.fullmatch(r"sgt_(impl|audit)_[A-Za-z0-9_]+", database)) or database in SCHEMAS_MYSQL8045
