#!/usr/bin/env python3
"""Captura SELECT-only coordenada com PID/JAR; fotografa o pós-boot antes de HTTP.

JSON bruto contém dados de sessão e fica exclusivamente em --privado (0700).
O resumo público contém contagens/hashes/comparações, nunca linhas ou segredos.
"""
import argparse
import datetime as dt
import json
import os
from pathlib import Path
import subprocess
import sys

from gerenciar_jar_tcp import BANCOS_HTTP_AUTORIZADOS, carregar_ambiente, conexao_redis_http, sha, vivo


def exigir(condicao, mensagem):
    if not condicao:
        raise RuntimeError(mensagem)


def conferir_conexao(processo, env):
    database = processo.get("database")
    exigir(database in BANCOS_HTTP_AUTORIZADOS, "Snapshot limitado às bases sintéticas HTTP autorizadas")
    exigir(type(processo.get("mysql_port")) is int and 0 < processo["mysql_port"] < 65536,
           "Porta MySQL local ausente ou inválida")
    exigir(processo.get("mysql_host") == "127.0.0.1", "Snapshot exige conexão MySQL local explícita")
    redis = conexao_redis_http(database, env, processo.get("redis_database"))
    exigir(type(processo.get("redis_database")) is int and type(processo.get("redis_port")) is int,
           "Snapshot exige identidade Redis completa no recibo do processo")
    exigir(all(processo.get(key) == value for key, value in redis.items()),
           "Identidade Redis diverge entre o processo e a infraestrutura; preserve a cadeia anterior")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--saida", type=Path, required=True)
    parser.add_argument("--privado", type=Path, required=True)
    parser.add_argument("--infra-env", type=Path, required=True)
    parser.add_argument("--mysql-bin", type=Path)
    parser.add_argument("--fase", choices=("antes_reinicio", "depois_reinicio"), required=True)
    args = parser.parse_args()
    caminho_processo = args.saida / "PROCESSO.json"
    processo = json.loads(caminho_processo.read_text())
    hash_processo = sha(caminho_processo)
    fases = processo["eventos"]
    ligado = args.fase == "depois_reinicio"
    exigir(sha(processo["jar"]) == processo["sha256"], "Bytes do JAR divergiram antes da fotografia")
    exigir(vivo(processo["pid"], processo["jar"]) == ligado, "Estado real do PID diverge da fase de fotografia")
    env = carregar_ambiente(args.infra_env)
    conferir_conexao(processo, env)
    if ligado:
        exigir(sum(e["acao"] == "iniciado" for e in fases) >= 2, "Foto pós-reinício exige segunda inicialização registrada")
        exigir(fases[-1]["acao"] == "pronto" and fases[-1]["pid"] == processo["pid"], "Foto pós-boot exige readiness sem HTTP")
        artefatos_http_reinicio = [args.saida / grupo / "reinicio.json"
                                   for grupo in ("basico", "auditorias", "provas_historicas", "ocorrencias")]
        # A quarta família usa --saida ocorrencias/reinicio e fase fixa
        # idempotencia-automatica; seu recibo não se chama reinicio.json.
        artefatos_http_reinicio.append(args.saida / "ocorrencias" / "reinicio" / "idempotencia-automatica.json")
        exigir(not any(arquivo.exists() for arquivo in artefatos_http_reinicio),
               "A fase HTTP do reinício já começou: preservar artefatos e não alegar snapshot pré-HTTP")
    else:
        exigir(fases[-1]["acao"] == "encerrado", "Antes da foto é obrigatório encerrar o JAR e registrar o término")
    args.privado.mkdir(parents=True, exist_ok=True, mode=0o700); args.privado.chmod(0o700)
    raw = args.privado / (args.fase + ".json")
    resumo = args.saida / ("PERSISTENCIA_" + args.fase + ".json")
    exigir(not raw.exists() and not resumo.exists(), "Não sobrescrever fotografias anteriores")
    env.update(MYSQL_HOST="127.0.0.1", MYSQL_TCP_PORT=str(processo["mysql_port"]),
               MYSQL_USER=env["DB_USER"], MYSQL_PWD=env["DB_PASS"])
    if args.mysql_bin:
        env["MYSQL_BIN"] = str(args.mysql_bin.resolve())
    capture = Path(__file__).resolve().with_name("capturar_schema_mysql.py")
    command = [sys.executable, str(capture), "--database", processo["database"], "--saida", str(raw)]
    if ligado:
        command += ["--comparar", str(args.privado / "antes_reinicio.json")]
    inicio = dt.datetime.now(dt.timezone.utc).isoformat()
    old_umask = os.umask(0o077)
    try:
        subprocess.run(command, env=env, check=True)
    finally:
        os.umask(old_umask)
    fim = dt.datetime.now(dt.timezone.utc).isoformat()
    raw.chmod(0o600)
    exigir(sha(caminho_processo) == hash_processo, "Supervisor mudou durante a fotografia; conservar para diagnóstico")
    exigir(vivo(processo["pid"], processo["jar"]) == ligado, "PID mudou de estado durante fotografia")
    dados = json.loads(raw.read_text())
    exigir(dados["database"] == processo["database"], "Captura respondeu outra base")
    exigir(str(dados["configuracao"]["mysql"]) == "8.0.45", "Persistência exige MySQL 8.0.45 real")
    exigir(dados["configuracao"]["foreign_key_checks"] == 1, "Verificação de FKs deve permanecer habilitada")
    exigir(dados["configuracao"]["innodb_strict_mode"] == 1, "InnoDB strict mode deve permanecer habilitado")
    coordenacao = dict(fase=args.fase, database=processo["database"], pid=processo["pid"], jar_sha256=processo["sha256"],
                      base_http=processo["base_http"], processo_sha256=hash_processo, eventos_observados=fases,
                      captura_inicio_utc=inicio, captura_fim_utc=fim, capturador_sha256=sha(capture))
    for key in ("mysql_host", "mysql_port", "redis_host", "redis_port", "redis_database"):
        if key in processo:
            coordenacao[key] = processo[key]
    dados["coordenacao"] = coordenacao
    raw.write_text(json.dumps(dados, ensure_ascii=False, indent=2) + "\n")
    publicos = {k: dados[k] for k in ("database", "formato_valores", "configuracao", "resumo_tabelas", "orfaos")}
    publicos.update(coordenacao=coordenacao, bruto_sha256=sha(raw), total_tabelas=len(dados["tabelas"]),
                    total_linhas=sum(x["linhas"] for x in dados["resumo_tabelas"].values()),
                    indices_sha256=__import__("hashlib").sha256(json.dumps(dados["indices"], sort_keys=True).encode()).hexdigest(),
                    fks_sha256=__import__("hashlib").sha256(json.dumps(dados["fks"], sort_keys=True).encode()).hexdigest())
    if "comparacao" in dados:
        publicos["comparacao"] = dados["comparacao"]
        antes = json.loads((args.privado / "antes_reinicio.json").read_text())
        for chave in ("schema", "tabelas", "indices", "fks", "restricoes_check", "resumo_tabelas"):
            publicos[chave + "_igual"] = dados[chave] == antes[chave]
        publicos["integridade_apos_reinicio"] = all(publicos[chave + "_igual"] for chave in
            ("schema", "tabelas", "indices", "fks", "restricoes_check", "resumo_tabelas"))
    publicos["sem_orfaos"] = all(x["orfaos"] == 0 for x in dados["orfaos"])
    resumo.write_text(json.dumps(publicos, ensure_ascii=False, indent=2) + "\n")
    exigir(publicos["sem_orfaos"], "Snapshot contém órfãos: evidência preservada, investigar")
    if ligado:
        exigir(publicos["integridade_apos_reinicio"], "Valores/schema/índices/restrições mudaram no boot: investigar antes de aprovar")
    print(json.dumps(dict(fase=args.fase, resumo=str(resumo), bruto_sha256=publicos["bruto_sha256"], sucesso=True)))


if __name__ == "__main__":
    main()
